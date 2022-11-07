package com.butent.bee.server.modules.mail;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.microsoft.aad.msal4j.*;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

public class MailAccount {

  private static final long CONNECTION_TIMEOUT = TimeUtils.MILLIS_PER_MINUTE;
  private static final long TIMEOUT = TimeUtils.MILLIS_PER_MINUTE * 10L;
  private static final String OUTLOOK_365 = "office365.com";

  static final class MailStore {
    private final ArrayBlockingQueue<Object> running;
    Store store;
    long lastActivity = System.currentTimeMillis();

    private MailStore(int maxSessions) {
      int size = BeeUtils.positive(maxSessions, 15);
      running = new ArrayBlockingQueue<>(size, true);

      for (int i = 0; i < size; i++) {
        running.add(new Object());
      }
    }

    public Store get() throws MessagingException {
      if (Objects.isNull(store)) {
        throw new MessagingException("Store was not initialized");
      }
      return store;
    }

    private void enter() throws MessagingException {
      try {
        logger.debug("Waiting for store...");

        if (!running.offer(new Object(), TIMEOUT, TimeUnit.MILLISECONDS)) {
          throw new MessagingException("Store wait timeout");
        }
        logger.debug("Entering store...");
        lastActivity = System.currentTimeMillis();

      } catch (InterruptedException e) {
        throw new MessagingException("Failure waiting for store", e);
      }
    }

    private boolean expired() {
      return BeeUtils.isMore(System.currentTimeMillis() - lastActivity, TimeUtils.MILLIS_PER_HOUR);
    }

    private boolean leave() {
      logger.debug("Leaving store...");
      lastActivity = System.currentTimeMillis();
      running.poll();
      return running.size() == 0;
    }

    private void set(Store st) {
      this.store = st;
      running.clear();
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(MailAccount.class);

  private static final Map<Long, MailStore> stores = new HashMap<>();
  private static final ReentrantLock storesLock = new ReentrantLock();

  private static boolean checkNewFolderName(Folder newFolder, String name)
      throws MessagingException {
    if (name.indexOf(newFolder.getSeparator()) >= 0) {
      throw new MessagingException("Invalid folder name: " + name);
    }
    logger.debug("Checking, if folder exists:", name);

    if (newFolder.exists()) {
      throw new MessagingException("Folder with new name already exists: " + name);
    }
    return true;
  }

  private static void fillTree(MailFolder parent, Multimap<Long, SimpleRow> folders) {
    for (SimpleRow row : folders.get(parent.getId())) {
      MailFolder folder = new MailFolder(row.getLong(COL_FOLDER), row.getValue(COL_FOLDER_NAME),
          row.getLong(COL_FOLDER_UID));

      folder.setModSeq(row.getLong(COL_FOLDER_MODSEQ));
      folder.setUnread(BeeUtils.unbox(row.getInt(COL_MESSAGE)));

      fillTree(folder, folders);
      parent.addSubFolder(folder);
    }
  }

  private static List<Message> getMessageReferences(Folder remoteSource, Long uidValidity,
      long[] uids) throws MessagingException {

    logger.debug("Checking folder", remoteSource.getName(), "UIDValidity with", uidValidity);

    if (!Objects.equals(((UIDFolder) remoteSource).getUIDValidity(), uidValidity)) {
      throw new FolderOutOfSyncException("Folder out of sync: " + remoteSource.getName());
    }
    logger.debug("Opening folder:", remoteSource.getName());
    remoteSource.open(Folder.READ_WRITE);

    logger.debug("Getting messages from folder", remoteSource.getName(), "by UIDs:", uids);
    Message[] msgs = ((UIDFolder) remoteSource).getMessagesByUID(uids);

    for (Message message : msgs) {
      if (message == null) {
        throw new FolderOutOfSyncException("Not all messages where returned by given UIDs");
      }
    }
    return Lists.newArrayList(msgs);
  }

  private final Protocol storeProtocol;
  private final String storeHost;
  private final Integer storePort;
  private final String storeLogin;
  private final String storePassword;
  private final boolean storeSSL;
  private final Map<String, String> storeProperties = new LinkedHashMap<>();

  private final Protocol transportProtocol = Protocol.SMTP;
  private final String transportHost;
  private final Integer transportPort;
  private final String transportLogin;
  private final String transportPassword;
  private final boolean transportSSL;
  private final Map<String, String> transportProperties = new LinkedHashMap<>();

  private final AccountInfo accountInfo;
  private final Collection<Long> accountUsers = new HashSet<>();

  private final OutlookConfigs outlookConfigs;

  MailAccount(SimpleRow data, OutlookConfigs outlookConfigs) {
    Assert.notNull(data);

    storeProtocol = EnumUtils.getEnumByName(Protocol.class, data.getValue(COL_STORE_TYPE));
    storeHost = data.getValue(COL_STORE_SERVER);
    storePort = data.getInt(COL_STORE_SPORT);
    storeLogin = BeeUtils.notEmpty(data.getValue(COL_STORE_LOGIN),
        data.getValue(ClassifierConstants.COL_EMAIL_ADDRESS));
    storePassword = BeeUtils.isEmpty(data.getValue(COL_STORE_PASSWORD))
        ? null : Codec.decodeBase64(data.getValue(COL_STORE_PASSWORD));
    storeSSL = BeeUtils.isTrue(data.getBoolean(COL_STORE_SSL));
    Codec.deserializeLinkedHashMap(data.getValue(COL_STORE_PROPERTIES))
        .forEach((p, v) -> storeProperties.put(BeeUtils.normalize(p), v));

    transportHost = data.getValue(COL_TRANSPORT_SERVER);
    transportPort = data.getInt(COL_TRANSPORT_PORT);
    transportLogin = BeeUtils.notEmpty(data.getValue(COL_TRANSPORT_LOGIN),
        data.getValue(ClassifierConstants.COL_EMAIL_ADDRESS));
    transportPassword = BeeUtils.isEmpty(data.getValue(COL_TRANSPORT_PASSWORD))
        ? null : Codec.decodeBase64(data.getValue(COL_TRANSPORT_PASSWORD));
    transportSSL = BeeUtils.isTrue(data.getBoolean(COL_TRANSPORT_SSL));
    Codec.deserializeLinkedHashMap(data.getValue(COL_TRANSPORT_PROPERTIES))
        .forEach((p, v) -> transportProperties.put(BeeUtils.normalize(p), v));

    accountInfo = new AccountInfo(data);
    this.outlookConfigs = outlookConfigs;
  }

  public Long getAccountId() {
    return accountInfo.getAccountId();
  }

  public String getAddress() {
    return accountInfo.getAddress();
  }

  public String getFolderCaption(Long folderId) {
    return accountInfo.getFolderCaption(folderId);
  }

  public Long getSignatureId() {
    return accountInfo.getSignatureId();
  }

  public String getStoreHost() {
    return storeHost;
  }

  public String getStoreLogin() {
    return storeLogin;
  }

  public String getStorePassword() {
    return storePassword;
  }

  public int getStorePort() {
    return BeeUtils.isPositive(storePort) ? storePort : -1;
  }

  public Map<String, String> getStoreProperties() {
    return storeProperties;
  }

  public Protocol getStoreProtocol() {
    return storeProtocol;
  }

  public String getTransportErrorMessage() {
    String err = null;

    if (BeeUtils.isEmpty(err)) {
      if (transportProtocol == null) {
        err = "Unknown transport protocol";

      } else if (BeeUtils.isEmpty(transportHost)) {
        err = "Unknown transport host";
      }
    }
    return err;
  }

  public String getTransportHost() {
    return transportHost;
  }

  public String getTransportLogin() {
    return transportLogin;
  }

  public String getTransportPassword() {
    return transportPassword;
  }

  public Integer getTransportPort() {
    return BeeUtils.isPositive(transportPort) ? transportPort : -1;
  }

  public Map<String, String> getTransportProperties() {
    return transportProperties;
  }

  public Protocol getTransportProtocol() {
    return transportProtocol;
  }

  public Long getUserId() {
    return accountInfo.getUserId();
  }

  public OutlookConfigs getOutlookConfigs() {
    return outlookConfigs;
  }

  public Collection<Long> getUsers() {
    return accountUsers;
  }

  public boolean isStoredRemotedly(MailFolder folder) {
    Assert.notNull(folder);
    return (getStoreProtocol() == Protocol.IMAP) && folder.isConnected();
  }

  public boolean isStoreSSL() {
    return storeSSL;
  }

  public boolean isTransportSSL() {
    return transportSSL;
  }

  public boolean isValidTransportAccount() {
    return BeeUtils.isEmpty(getTransportErrorMessage());
  }

  boolean addMessageToRemoteFolder(MimeMessage message, MailFolder localFolder)
      throws MessagingException {

    if (!isStoredRemotedly(localFolder)) {
      return false;
    }
    MailStore store = null;

    try {
      store = connect();
      Folder folder = getRemoteFolder(store.get(), localFolder);
      folder.appendMessages(new MimeMessage[] {message});

    } finally {
      disconnect(store);
    }
    return true;
  }

  MailStore connect() throws MessagingException {
    storesLock.lock();
    MailStore mailStore = stores.get(getAccountId());

    if (mailStore != null) {
      if (mailStore.expired()) {
        logger.debug("Removing expired store...");
        stores.remove(getAccountId());
      } else {
        storesLock.unlock();
        mailStore.enter();
        return mailStore;
      }
    }
    logger.debug("Opening store...");

    mailStore = new MailStore(BeeUtils.toInt(getStoreProperties().get("maxsessions")));
    stores.put(getAccountId(), mailStore);
    storesLock.unlock();

    try {

      String password = getStorePassword();
      String protocol = getStoreProtocol().name().toLowerCase();
      Properties props = new Properties();
      String pfx = "mail." + protocol + ".";

      if (!BeeUtils.isEmpty(getStoreHost()) && getStoreHost().contains(OUTLOOK_365)) {

        IAuthenticationResult token = acquireOutlook365Token();

        if (Objects.nonNull(token) && !BeeUtils.isEmpty(token.accessToken())) {
          password = token.accessToken();
          props.put("mail.imap.auth.mechanisms", "XOAUTH2");
        }
      }

      if (Objects.equals(getStoreProtocol(), Protocol.IMAP)) {
        props.put(pfx + "partialfetch", "false");
        props.put(pfx + "compress.enable", "true");
        props.put(pfx + "connectionpoolsize", "10");
      }
      props.put("mail.mime.address.strict", "false");
      props.put(pfx + "connectiontimeout", BeeUtils.toString(CONNECTION_TIMEOUT));
      props.put(pfx + "timeout", BeeUtils.toString(TIMEOUT));

      if (isStoreSSL()) {
        props.put(pfx + "ssl.enable", "true");
      }
      for (Entry<String, String> prop : getStoreProperties().entrySet()) {
        String key = prop.getKey();
        props.put(BeeUtils.isPrefix(key, "mail.") ? key : pfx + key, prop.getValue());
      }
      Store store = Session.getInstance(props, null).getStore(protocol);
      store.connect(getStoreHost(), getStorePort(), getStoreLogin(), password);
      mailStore.set(store);

    } catch (Exception ex) {
      storesLock.lock();
      stores.remove(getAccountId());
      storesLock.unlock();
      mailStore.set(null);
      throw new ConnectionFailureException(ex.getMessage());
    }
    mailStore.enter();
    return mailStore;
  }

  Transport connectToTransport() throws MessagingException {
    if (!isValidTransportAccount()) {
      throw new MessagingException(getTransportErrorMessage());
    }
    logger.debug("Connecting to transport...");

    String protocol = getTransportProtocol().name().toLowerCase();
    Properties props = new Properties();
    String pfx = "mail." + protocol + ".";

    props.put(pfx + "connectiontimeout", BeeUtils.toString(CONNECTION_TIMEOUT));
    props.put(pfx + "timeout", BeeUtils.toString(TIMEOUT));

    if (!BeeUtils.isEmpty(getTransportPassword())) {
      props.put(pfx + "auth", "true");

      if (isTransportSSL()) {
        props.put(pfx + "ssl.enable", "true");
      } else {
        props.put(pfx + "starttls.enable", "true");
      }
    }
    for (Entry<String, String> prop : getTransportProperties().entrySet()) {
      String key = prop.getKey();
      props.put(BeeUtils.isPrefix(key, "mail.") ? key : pfx + key, prop.getValue());
    }
    Session session = Session.getInstance(props, null);
    Transport transport = session.getTransport(protocol);
    transport.connect(getTransportHost(), getTransportPort(), getTransportLogin(),
        getTransportPassword());
    return transport;
  }

  private IAuthenticationResult acquireOutlook365Token() {

    try {

      IClientCredential credential = ClientCredentialFactory.createFromSecret(getOutlookConfigs().getSecretKey());
      ConfidentialClientApplication cca =
              ConfidentialClientApplication
                      .builder(getOutlookConfigs().getClientId(), credential)
                      .authority("https://login.microsoftonline.com/" + getOutlookConfigs().getTenantId())
                      .build();

      Set<String> scopes = new HashSet<>();
      scopes.add(getOutlookConfigs().getScope());

      ClientCredentialParameters parameters =
              ClientCredentialParameters
                      .builder(scopes)
                      .build();

      return cca.acquireToken(parameters).join();

    } catch (MalformedURLException e) {
      logger.debug(e.getMessage());
      return null;
    }
  }

  boolean createRemoteFolder(MailFolder parent, String name) throws MessagingException {
    boolean ok = true;

    if (!isStoredRemotedly(parent)) {
      return ok;
    }
    MailStore store = null;

    try {
      store = connect();
      Folder folder = getRemoteFolder(store.get(), parent);
      Folder newFolder = folder.getFolder(name);

      if (checkNewFolderName(newFolder, name)) {
        logger.debug("Creating folder:", newFolder.getFullName());
        ok = newFolder.create(Folder.HOLDS_MESSAGES);
      }
    } finally {
      disconnect(store);
    }
    return ok;
  }

  void disconnect(MailStore mailStore) {
    if (mailStore != null) {
      storesLock.lock();
      boolean close = mailStore.leave();

      if (close && Objects.equals(mailStore, stores.get(getAccountId()))) {
        stores.remove(getAccountId());
      }
      storesLock.unlock();

      if (close) {
        logger.debug("Closing store...");
        try {
          mailStore.get().close();
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
    }
  }

  boolean dropRemoteFolder(MailFolder source) throws MessagingException {
    boolean ok = true;

    if (!isStoredRemotedly(source)) {
      return ok;
    }
    MailStore store = null;

    try {
      store = connect();
      Folder folder = getRemoteFolder(store.get(), source);

      logger.debug("Removing folder:", folder.getName());
      ok = folder.delete(true) || !folder.exists();

    } finally {
      disconnect(store);
    }
    return ok;
  }

  MailFolder findFolder(Long folderId) {
    return accountInfo.findFolder(folderId);
  }

  Folder getRemoteFolder(Store remoteStore, MailFolder localFolder) throws MessagingException {
    Assert.noNulls(remoteStore, localFolder);
    Folder remote;

    if (localFolder.getParent() == null) {
      logger.debug("Looking for remote", localFolder.getName(), "folder");
      remote = remoteStore.getDefaultFolder();
    } else {
      String path = localFolder.getPath(remoteStore.getDefaultFolder().getSeparator());
      logger.debug("Looking for remote", path, "folder");
      remote = remoteStore.getFolder(path);

      if (!remote.exists()) {
        remote = getRemoteFolder2(remoteStore, localFolder);
      }
    }
    return remote;
  }

  Folder getRemoteFolder2(Store remoteStore, MailFolder localFolder) throws MessagingException {
    if (localFolder.getParent() == null) {
      logger.debug("Looking for remote", localFolder.getName(), "folder");
      return remoteStore.getDefaultFolder();
    }
    Folder remoteParent = getRemoteFolder2(remoteStore, localFolder.getParent());

    String name = localFolder.getName();
    logger.debug("Looking for remote folder", name, "in", localFolder.getParent().getName());

    Folder remote = remoteParent.getFolder(name);

    if (!remote.exists()) {
      if (isInbox(localFolder) || !isSystemFolder(localFolder)
          || !createRemoteFolder(localFolder.getParent(), name)) {

        throw new MessagingException(BeeUtils.joinWords("Remote folder", name, "in",
            localFolder.getParent().getName(), "does not exist"));
      }
    }
    return remote;
  }

  MailFolder getDraftsFolder() {
    return findFolder(getSystemFolder(SystemFolder.Drafts));
  }

  MailFolder getInboxFolder() {
    return findFolder(getSystemFolder(SystemFolder.Inbox));
  }

  MailFolder getSentFolder() {
    return findFolder(getSystemFolder(SystemFolder.Sent));
  }

  public Long getSystemFolder(SystemFolder sysFolder) {
    return accountInfo.getSystemFolder(sysFolder);
  }

  MailFolder getTrashFolder() {
    return findFolder(getSystemFolder(SystemFolder.Trash));
  }

  MailFolder getRootFolder() {
    return accountInfo.getRootFolder();
  }

  static boolean holdsMessages(Folder remoteFolder) throws MessagingException {
    return (remoteFolder.getType() & Folder.HOLDS_MESSAGES) != 0;
  }

  boolean isInbox(MailFolder folder) {
    return accountInfo.isInboxFolder(folder.getId());
  }

  boolean isRoot(MailFolder folder) {
    return folder == getRootFolder();
  }

  boolean isSystemFolder(MailFolder folder) {
    return accountInfo.isSystemFolder(folder.getId());
  }

  boolean processMessages(long[] uids, MailFolder source, MailFolder target, boolean move)
      throws MessagingException {

    if (!isStoredRemotedly(source)) {
      return false;
    }
    boolean isTarget = target != null && target.isConnected();

    if (!move && !isTarget || ArrayUtils.length(uids) == 0) {
      return true;
    }
    MailStore store = null;
    Folder remoteSource = null;

    try {
      store = connect();
      remoteSource = getRemoteFolder(store.get(), source);
      List<Message> messages = getMessageReferences(remoteSource, source.getUidValidity(), uids);

      if (isTarget) {
        Folder remoteTarget = getRemoteFolder(store.get(), target);

        if (!holdsMessages(remoteTarget)) {
          throw new MessagingException(BeeUtils.joinWords("Folder",
              BeeUtils.bracket(target.getName()), "cannot hold messages"));
        }
        logger.debug("Copying messages to folder:", target.getName());
        remoteSource.copyMessages(messages.toArray(new Message[0]), remoteTarget);
      }
      if (move) {
        messages.removeIf(Message::isExpunged);

        if (!BeeUtils.isEmpty(messages)) {
          logger.debug("Deleting selected messages from folder:", remoteSource.getName());
          remoteSource.setFlags(messages.toArray(new Message[0]), new Flags(Flag.DELETED), true);
        } else {
          logger.debug("All messages already expunged, no delete required");
        }
      }
      logger.debug("Closing folder:", remoteSource.getName());
      remoteSource.close(move);

    } finally {
      if (remoteSource != null && remoteSource.isOpen()) {
        try {
          remoteSource.close(false);
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
      disconnect(store);
    }
    return true;
  }

  boolean renameRemoteFolder(MailFolder source, String name) throws MessagingException {
    boolean ok = true;

    if (!isStoredRemotedly(source)) {
      return ok;
    }
    MailStore store = null;

    try {
      store = connect();
      Folder folder = getRemoteFolder(store.get(), source);
      Folder newFolder = folder.getParent().getFolder(name);

      checkNewFolderName(newFolder, name);

      logger.debug("Renamng folder:", folder.getName(), "->", name);
      ok = folder.renameTo(newFolder);

    } finally {
      disconnect(store);
    }
    return ok;
  }

  boolean setFlag(MailFolder source, long[] uids, MessageFlag messageFlag, boolean on)
      throws MessagingException {

    if (!isStoredRemotedly(source)) {
      return false;
    }
    MailStore store = null;
    Folder folder = null;

    try {
      store = connect();
      folder = getRemoteFolder(store.get(), source);
      List<Message> messages = getMessageReferences(folder, source.getUidValidity(), uids);

      Flag flag = MailEnvelope.getFlag(messageFlag);
      Flags flags = null;

      if (flag != null) {
        flags = new Flags(flag);
      } else if (folder.getPermanentFlags().contains(Flag.USER)) {
        flags = new Flags(messageFlag.name());
      } else {
        logger.warning("Remote folder", folder.getName(), "does not support user defined flags");
      }
      if (flags != null) {
        logger.debug(on ? "Setting" : "Clearing", "flag", messageFlag, " for selected messages");
        folder.setFlags(messages.toArray(new Message[0]), flags, on);
      }
    } finally {
      if (folder != null && folder.isOpen()) {
        try {
          logger.debug("Closing folder:", folder.getName());
          folder.close(false);
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
      disconnect(store);
    }
    return true;
  }

  void setFolders(Multimap<Long, SimpleRow> folders) {
    getRootFolder().getSubFolders().clear();
    fillTree(getRootFolder(), folders);
  }

  void setSystemFolder(SystemFolder sysFolder, Long folderId) {
    accountInfo.setSystemFolder(sysFolder, folderId);
  }

  void setUsers(Long... users) {
    accountUsers.clear();
    accountUsers.add(getUserId());

    if (!ArrayUtils.isEmpty(users)) {
      Collections.addAll(accountUsers, users);
    }
  }
}
