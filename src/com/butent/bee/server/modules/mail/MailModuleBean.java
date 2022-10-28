package com.butent.bee.server.modules.mail;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.concurrency.ConcurrencyBean.AsynchronousRunnable;
import com.butent.bee.server.concurrency.ConcurrencyBean.HasTimerService;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.QueryServiceBean.ViewDataProvider;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.HtmlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.ColumnValueFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.websocket.messages.MailMessage;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.ResyncData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.ReadOnlyFolderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.MediaType;

@Stateless
@LocalBean
public class MailModuleBean implements BeeModule, HasTimerService {

  private static final BeeLogger logger = LogUtils.getLogger(MailModuleBean.class);
  private static final int CHUNK = 1000;

  @EJB
  MailStorageBean mail;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  FileStorageBean fs;
  @EJB
  ParamHolderBean prm;
  @EJB
  NewsBean news;
  @EJB
  ConcurrencyBean cb;

  @Resource
  SessionContext ctx;
  @Resource
  TimerService timerService;

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void checkMail(boolean async, MailAccount account, MailFolder localFolder, boolean force) {
    Assert.noNulls(account, localFolder);

    if (async) {
      cb.asynchronousCall(new AsynchronousRunnable() {
        @Override
        public String getId() {
          if (DataUtils.isId(localFolder.getId())) {
            return BeeUtils.join("-", "CheckMailFolder", localFolder.getId());
          } else {
            return BeeUtils.join("-", "CheckMailAccount", account.getAccountId());
          }
        }

        @Override
        public void run() {
          Invocation.locateRemoteBean(MailModuleBean.class)
              .checkMail(false, account, localFolder, force);
        }
      });
      return;
    }
    if (localFolder.isConnected()) {
      MailAccount.MailStore store = null;
      int c = 0;
      int f = 0;
      String error = null;
      boolean connectError = false;
      String progressId = DataUtils.isId(usr.getCurrentUserId())
          ? Endpoint.createProgress(usr.getCurrentUserId(),
          account.getFolderCaption(localFolder.getId())) : null;

      try {
        store = account.connect();

        if (account.isRoot(localFolder)) {
          f += syncFolders(account, store.get());

          Holder<Consumer<MailFolder>> holder = Holder.absent();
          holder.set(mailFolder -> {
            mailFolder.getSubFolders().forEach(folder -> checkMail(true, account, folder, force));
            mailFolder.getSubFolders().forEach(folder -> holder.get().accept(folder));
          });
          holder.get().accept(localFolder);
        } else {
          c += checkFolder(account, account.getRemoteFolder(store.get(), localFolder),
              localFolder, force, progressId);
        }
      } catch (Throwable e) {
        error = BeeUtils.joinWords(account.getStoreProtocol(), account.getStoreHost(),
            account.getStoreLogin(), localFolder.getName());

        if (e instanceof ConnectionFailureException) {
          if (BeeUtils.isEmpty(progressId)
              && (account.isRoot(localFolder) || account.isInbox(localFolder))
              && (System.currentTimeMillis() - BeeUtils.unbox(qs.getLongById(TBL_ACCOUNTS,
              account.getAccountId(), COL_ACCOUNT_LAST_CONNECT))) > TimeUtils.MILLIS_PER_DAY) {

            mail.updateAccount(account.getAccountId(), COL_ACCOUNT_SYNC_MODE,
                SyncMode.SYNC_NOTHING);
          }
          logger.warning(error, e.getMessage());
          connectError = true;
        } else {
          if (e instanceof BeeRuntimeException || e.getCause() instanceof BeeRuntimeException) {
            logger.severe(error);
          } else {
            logger.error(e, error);
          }
          error = ArrayUtils.joinWords(ResponseObject.error(e, account.getStoreProtocol())
              .getErrors());
        }
      } finally {
        if (!BeeUtils.isEmpty(progressId)) {
          Endpoint.closeProgress(progressId);
        }
        account.disconnect(store);
      }
      if (!connectError && account.isInbox(localFolder)) {
        mail.updateAccount(account.getAccountId(), COL_ACCOUNT_LAST_CONNECT,
            System.currentTimeMillis());
      }
      if (!BeeUtils.isEmpty(error) || c > 0 || f > 0) {
        MailMessage mailMessage = new MailMessage(localFolder.getId());
        mailMessage.setMessagesUpdated(c > 0);
        mailMessage.setFoldersUpdated(f > 0);
        mailMessage.setError(error);
        Endpoint.sendToUsers(account.getUsers(), mailMessage, null);
      }
    }
  }

  public int countUnread() {
    long userId = usr.getCurrentUserId();

    return qs.sqlCount(new SqlSelect()
        .addFrom(TBL_PLACES)
        .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
        .addFromInner(TBL_ACCOUNTS, sys.joinTables(TBL_ACCOUNTS, TBL_FOLDERS, COL_ACCOUNT))
        .addFromLeft(TBL_ACCOUNT_USERS,
            SqlUtils.and(sys.joinTables(TBL_ACCOUNTS, TBL_ACCOUNT_USERS, COL_ACCOUNT),
                SqlUtils.equals(TBL_ACCOUNT_USERS, COL_USER, userId)))
        .setWhere(SqlUtils.and(SqlUtils.or(SqlUtils.equals(TBL_ACCOUNTS, COL_USER, userId),
            SqlUtils.equals(TBL_ACCOUNT_USERS, COL_USER, userId)),
            SqlUtils.equals(SqlUtils.bitAnd(SqlUtils.nvl(SqlUtils.field(TBL_PLACES,
                COL_FLAGS), 0), MessageFlag.SEEN.getMask()), 0))));
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    return null;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    try {
      if (BeeUtils.same(svc, SVC_GET_ACCOUNTS)) {
        long userId = reqInfo.getParameterLong(COL_USER);

        response = ResponseObject.response(qs.getData(new SqlSelect()
            .addField(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS), COL_ACCOUNT)
            .addFields(TBL_ACCOUNTS, MailConstants.COL_ADDRESS, COL_USER, COL_ACCOUNT_DESCRIPTION,
                COL_ACCOUNT_DEFAULT, COL_SIGNATURE, COL_ACCOUNT_PRIVATE,
                SystemFolder.Inbox.name() + COL_FOLDER,
                SystemFolder.Drafts.name() + COL_FOLDER,
                SystemFolder.Sent.name() + COL_FOLDER,
                SystemFolder.Trash.name() + COL_FOLDER)
            .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
            .addFrom(TBL_ACCOUNTS)
            .addFromInner(TBL_EMAILS,
                sys.joinTables(TBL_EMAILS, TBL_ACCOUNTS, MailConstants.COL_ADDRESS))
            .addFromLeft(TBL_ACCOUNT_USERS,
                SqlUtils.and(sys.joinTables(TBL_ACCOUNTS, TBL_ACCOUNT_USERS, COL_ACCOUNT),
                    SqlUtils.equals(TBL_ACCOUNT_USERS, COL_USER, userId)))
            .setWhere(SqlUtils.or(SqlUtils.equals(TBL_ACCOUNTS, COL_USER, userId),
                SqlUtils.equals(TBL_ACCOUNT_USERS, COL_USER, userId)))
            .addOrder(TBL_ACCOUNTS, COL_ACCOUNT_DESCRIPTION)));

      } else if (BeeUtils.same(svc, SVC_GET_MESSAGE)) {
        response = getMessage(reqInfo.getParameterLong(COL_MESSAGE),
            reqInfo.getParameterLong(COL_PLACE));

      } else if (BeeUtils.same(svc, SVC_FLAG_MESSAGE)) {
        setMessageFlag(reqInfo.getParameterLong(COL_PLACE),
            EnumUtils.getEnumByName(MessageFlag.class, reqInfo.getParameter(COL_FLAGS)),
            reqInfo.getParameterBoolean("on"));

        response = ResponseObject.emptyResponse();

      } else if (BeeUtils.same(svc, SVC_COPY_MESSAGES)) {
        MailAccount account = mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT));
        Long targetId = reqInfo.getParameterLong(COL_FOLDER);
        MailFolder target = account.findFolder(targetId);

        if (target == null) {
          response = ResponseObject.error("Folder does not exist: ID =", targetId);
        } else {
          response = ResponseObject.response(copyMessages(account,
              DataUtils.parseIdList(reqInfo.getParameter(COL_PLACE)), target,
              reqInfo.getParameterBoolean("move")));
        }
      } else if (BeeUtils.same(svc, SVC_REMOVE_MESSAGES)) {
        response = ResponseObject.response(removeMessages(
            mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT)),
            DataUtils.parseIdList(reqInfo.getParameter(COL_PLACE))));

      } else if (BeeUtils.same(svc, SVC_GET_FOLDERS)) {
        MailAccount account = mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT), true);
        Map<String, Object> map = new HashMap<>();
        map.put(COL_FOLDER, account.getRootFolder());

        for (SystemFolder sysFolder : EnumSet.complementOf(EnumSet.of(SystemFolder.Inbox))) {
          map.put(sysFolder.name(), account.getSystemFolder(sysFolder));
        }
        response = ResponseObject.response(map);

      } else if (BeeUtils.same(svc, SVC_CREATE_FOLDER)) {
        MailAccount account = mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT));
        Long folderId = reqInfo.getParameterLong(COL_FOLDER);
        MailFolder parent;

        if (DataUtils.isId(folderId)) {
          parent = account.findFolder(folderId);
        } else {
          parent = account.getRootFolder();
        }
        if (parent == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          String name = reqInfo.getParameter(COL_FOLDER_NAME);
          boolean ok = account.createRemoteFolder(parent, name);

          if (!ok && account.isRoot(parent)) {
            parent = account.getInboxFolder();
            ok = account.createRemoteFolder(parent, name);
          }
          if (ok) {
            mail.createFolder(account, parent, name);
            response = ResponseObject.info("Folder created:", name);
          } else {
            response = ResponseObject.error("Cannot create folder:", name);
          }
        }
      } else if (BeeUtils.same(svc, SVC_DISCONNECT_FOLDER)) {
        MailAccount account = mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT));
        Long folderId = reqInfo.getParameterLong(COL_FOLDER);
        MailFolder folder = account.findFolder(folderId);

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          disconnectFolder(account, folder);
          response = ResponseObject.info("Folder disconnected: " + folder.getName());
        }
      } else if (BeeUtils.same(svc, SVC_RENAME_FOLDER)) {
        MailAccount account = mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT));
        Long folderId = reqInfo.getParameterLong(COL_FOLDER);
        MailFolder folder = account.findFolder(folderId);

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          String name = reqInfo.getParameter(COL_FOLDER_NAME);

          if (account.renameRemoteFolder(folder, name)) {
            mail.renameFolder(folder, name);
            response = ResponseObject.info("Folder renamed: " + folder.getName() + "->" + name);
          } else {
            response = ResponseObject.error("Cannot rename folder", folder.getName(), "to", name);
          }
        }
      } else if (BeeUtils.same(svc, SVC_DROP_FOLDER)) {
        MailAccount account = mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT));
        Long folderId = reqInfo.getParameterLong(COL_FOLDER);
        MailFolder folder = account.findFolder(folderId);

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          if (account.dropRemoteFolder(folder)) {
            mail.dropFolder(folder);
            response = ResponseObject.info("Folder dropped: " + folder.getName());
          } else {
            response = ResponseObject.error("Cannot drop folder", folder.getName());
          }
        }
      } else if (BeeUtils.same(svc, SVC_CHECK_MAIL)) {
        MailAccount account = mail.getAccount(reqInfo.getParameterLong(COL_ACCOUNT));
        Long folderId = reqInfo.getParameterLong(COL_FOLDER);
        MailFolder folder = DataUtils.isId(folderId)
            ? account.findFolder(folderId) : account.getRootFolder();

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          ctx.getBusinessObject(this.getClass()).checkMail(false, account, folder,
              reqInfo.getParameterBoolean(Service.VAR_CHECK));
          response = ResponseObject.emptyResponse();
        }
      } else if (BeeUtils.same(svc, SVC_SEND_MAIL)) {
        response = new ResponseObject();
        boolean save = reqInfo.getParameterBoolean("Save");
        Long relatedId = reqInfo.getParameterLong(AdministrationConstants.COL_RELATION);
        Long accountId = reqInfo.getParameterLong(COL_ACCOUNT);
        String[] to = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.TO.name()));
        String[] cc = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.CC.name()));
        String[] bcc = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.BCC.name()));
        String subject = reqInfo.getParameter(COL_SUBJECT);
        String content = reqInfo.getParameter(COL_CONTENT);
        String inReplyTo = null;

        if (DataUtils.isId(relatedId)) {
          SimpleRow row = qs.getRow(new SqlSelect()
              .addField(TBL_FOLDERS, sys.getIdName(TBL_FOLDERS), COL_FOLDER)
              .addFields(TBL_FOLDERS, COL_ACCOUNT)
              .addFields(TBL_MESSAGES, COL_UNIQUE_ID, COL_IN_REPLY_TO)
              .addFrom(TBL_PLACES)
              .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
              .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(sys.idEquals(TBL_PLACES, relatedId)));

          MailAccount account = mail.getAccount(row.getLong(COL_ACCOUNT));

          if (Objects.equals(row.getLong(COL_FOLDER), account.getDraftsFolder().getId())) {
            removeMessages(account, Collections.singletonList(relatedId));
            inReplyTo = row.getValue(COL_IN_REPLY_TO);
            Long[] places = null;

            if (!BeeUtils.isEmpty(inReplyTo)) {
              places =
                  qs.getLongColumn(new SqlSelect()
                      .addFields(TBL_PLACES, sys.getIdName(TBL_PLACES))
                      .addFrom(TBL_PLACES)
                      .addFromInner(TBL_FOLDERS,
                          sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
                      .addFromInner(TBL_MESSAGES,
                          sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
                      .setWhere(
                          SqlUtils.and(SqlUtils.equals(TBL_MESSAGES, COL_UNIQUE_ID, inReplyTo),
                              SqlUtils.equals(TBL_FOLDERS, COL_ACCOUNT, account.getAccountId()))));
            }
            relatedId = ArrayUtils.getQuietly(places, 0);
          } else {
            inReplyTo = row.getValue(COL_UNIQUE_ID);
          }
        }
        Map<Long, String> attachments = new LinkedHashMap<>();

        for (Entry<String, String> entry : Codec
            .deserializeLinkedHashMap(reqInfo.getParameter(TBL_ATTACHMENTS)).entrySet()) {
          attachments.put(BeeUtils.toLong(entry.getKey()), entry.getValue());
        }
        MailAccount account = mail.getAccount(accountId);
        MimeMessage message = null;

        if (!save) {
          try {
            message = sendMail(account, to, cc, bcc, subject, content, attachments, inReplyTo);
            response.setResponse(storeMessage(account, message, account.getSentFolder()));
            response.addInfo(usr.getDictionary().mailMessageSent());

          } catch (MessagingException e) {
            save = true;
            logger.error(e, account.getTransportProtocol(), account.getTransportHost(),
                account.getTransportLogin());
            response.addError(e, account.getTransportProtocol());
          }
        }
        if (save) {
          if (Objects.isNull(message)) {
            message = buildMessage(account, to, cc, bcc, subject, content, attachments, inReplyTo);
          }
          response.setResponse(storeMessage(account, message, account.getDraftsFolder()));
          response.addInfo(usr.getDictionary().mailMessageIsSavedInDraft());

        } else if (DataUtils.isId(relatedId)) {
          setMessageFlag(relatedId, MessageFlag.ANSWERED, true);
        }
      } else if (BeeUtils.same(svc, SVC_STRIP_HTML)) {
        response = ResponseObject
            .response(HtmlUtils.stripHtml(reqInfo.getParameter(COL_HTML_CONTENT)));

      } else if (BeeUtils.same(svc, SVC_GET_RAW_CONTENT)) {
        response = getRawContent(reqInfo.getParameterLong(COL_MESSAGE));

      } else if (BeeUtils.same(svc, SVC_GET_UNREAD_COUNT)) {
        response = ResponseObject.response(countUnread());

      } else if (BeeUtils.same(svc, SVC_GET_NEWSLETTER_CONTACTS)) {
        response = getNewsletterContacts(reqInfo);

      } else {
        String msg = BeeUtils.joinWords("Mail service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
      }
    } catch (MessagingException e) {
      ctx.setRollbackOnly();
      logger.error(e);
      response = ResponseObject.error(e);
    }
    return response;
  }

  @Override
  public void ejbTimeout(Timer timer) {
    if (ConcurrencyBean.isParameterTimer(timer, PRM_MAIL_CHECK_INTERVAL)) {
      checkMail();
    }
    if (ConcurrencyBean.isParameterTimer(timer, PRM_SEND_NEWSLETTERS_INTERVAL)) {
      sendNewsletter();
    }
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Lists.newArrayList(
        BeeParameter.createBoolean(module, PRM_SIGNATURE_POSITION, true, true),
        BeeParameter.createRelation(module, PRM_DEFAULT_ACCOUNT, TBL_ACCOUNTS, COL_ACCOUNT_DESCRIPTION),
        BeeParameter.createNumber(module, PRM_MAIL_CHECK_INTERVAL),
        BeeParameter.createNumber(module, PRM_SEND_NEWSLETTERS_COUNT, false, null),
        BeeParameter.createNumber(module, PRM_SEND_NEWSLETTERS_INTERVAL, false, null),
        BeeParameter.createText(module, PRM_REMINDERS_MAIL_TEMPLATE, false, DEFAULT_REMINDERS_MAIL_TEMPLATE_VALUE),
        BeeParameter.createText(module, PRM_OUTLOOK_365_CLIENT_ID, false, null),
        BeeParameter.createText(module, PRM_OUTLOOK_365_TENANT_ID, false, null),
        BeeParameter.createText(module, PRM_OUTLOOK_365_SECRET_KEY, false, null),
        BeeParameter.createText(module, PRM_OUTLOOK_365_SCOPE, false, null));
  }

  @Override
  public Module getModule() {
    return Module.MAIL;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  public Long getSenderAccountId(String logLabel) {
    Long account = prm.getRelation(PRM_DEFAULT_ACCOUNT);

    if (!DataUtils.isId(account)) {
      logger.info(logLabel, "sender account not specified", BeeUtils.bracket(PRM_DEFAULT_ACCOUNT));
    }
    return account;
  }

  public String getSenderAccountEmail(Long accountId) {
    SimpleRow row = qs.getRow(new SqlSelect()
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFrom(TBL_ACCOUNTS)
        .addFromInner(TBL_EMAILS,
            sys.joinTables(TBL_EMAILS, TBL_ACCOUNTS, MailConstants.COL_ADDRESS))
        .setWhere(sys.idEquals(TBL_ACCOUNTS, accountId)));

    return row.getValue(COL_EMAIL_ADDRESS);
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  @Override
  public void init() {
    System.setProperty("mail.mime.charset", BeeConst.CHARSET_UTF8);
    System.setProperty("mail.mime.decodetext.strict", "false");
    System.setProperty("mail.mime.decodefilename", "true");
    System.setProperty("mail.mime.parameters.strict", "false");
    System.setProperty("mail.mime.base64.ignoreerrors", "true");
    System.setProperty("mail.mime.ignoreunknownencoding", "true");
    System.setProperty("mail.mime.uudecode.ignoreerrors", "true");
    System.setProperty("mail.mime.uudecode.ignoremissingbeginend", "true");
    System.setProperty("mail.mime.ignoremultipartencoding", "false");
    System.setProperty("mail.mime.allowencodedmessages", "true");

    cb.createIntervalTimer(this.getClass(), PRM_MAIL_CHECK_INTERVAL);
    cb.createIntervalTimer(this.getClass(), PRM_SEND_NEWSLETTERS_INTERVAL);

    sys.registerDataEventHandler(new DataEventHandler() {

      @Subscribe
      @AllowConcurrentEvents
      public void initAccount(ViewInsertEvent event) {
        if (event.isAfter(TBL_ACCOUNTS)) {
          mail.initAccount(event.getRow().getId());
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void getRecipients(ViewQueryEvent event) {
        if (event.isBefore(TBL_PLACES)) {
          // TODO Removed order by "PlaceID" for optimization purposes
          List<String[]> order = event.getQuery().getOrderBy();

          if (BeeUtils.size(order) > 1) {
            order.remove(order.size() - 1);
          }
        } else if (event.isAfter(TBL_PLACES)) {
          BeeRowSet rowSet = event.getRowset();
          int idx = DataUtils.getColumnIndex(COL_MESSAGE, rowSet.getColumns(), false);

          if (BeeConst.isUndef(idx)) {
            return;
          }
          Set<Long> messages = rowSet.getDistinctLongs(idx);

          if (!BeeUtils.isEmpty(messages)) {
            SimpleRowSet result = qs.getData(new SqlSelect()
                .addFields(TBL_RECIPIENTS, COL_MESSAGE, COL_ADDRESS_TYPE)
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addFields(TBL_ADDRESSBOOK, COL_EMAIL_LABEL)
                .addFrom(TBL_RECIPIENTS)
                .addFromInner(TBL_EMAILS,
                    sys.joinTables(TBL_EMAILS, TBL_RECIPIENTS, MailConstants.COL_ADDRESS))
                .addFromLeft(TBL_ADDRESSBOOK,
                    SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                        SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())))
                .setWhere(SqlUtils.inList(TBL_RECIPIENTS, COL_MESSAGE, messages))
                .addOrderDesc(TBL_RECIPIENTS, COL_ADDRESS_TYPE)
                .addOrder(TBL_RECIPIENTS, sys.getIdName(TBL_RECIPIENTS)));

            Map<String, String[]> recipients = new HashMap<>();

            for (SimpleRow row : result) {
              String msg = row.getValue(COL_MESSAGE);
              String email = row.getValue(COL_EMAIL_ADDRESS);
              String label = row.getValue(COL_EMAIL_LABEL);

              if (recipients.containsKey(msg)) {
                String[] info = recipients.get(msg);
                info[2] = BeeUtils.toString(BeeUtils.toLong(info[2]) + 1);
              } else {
                recipients.put(msg, new String[] {email, label, "1"});
              }
            }
            for (BeeRow row : rowSet) {
              String[] info = recipients.get(row.getString(idx));

              if (info != null) {
                row.setProperty(COL_EMAIL_ADDRESS, info[0]);
                row.setProperty(COL_EMAIL_LABEL, info[1]);
                row.setProperty(MailConstants.COL_ADDRESS, info[2]);
              }
            }
            String relations = AdministrationConstants.TBL_RELATIONS;
            String relation = AdministrationConstants.COL_RELATION;

            result = qs.getData(new SqlSelect()
                .addFields(relations, COL_MESSAGE)
                .addCount(relation)
                .addFrom(relations)
                .setWhere(SqlUtils.and(SqlUtils.inList(relations, COL_MESSAGE, messages),
                    SqlUtils.isNull(relations, COL_COMPANY)))
                .addGroup(relations, COL_MESSAGE));

            for (BeeRow row : rowSet) {
              row.setProperty(relation,
                  result.getValueByKey(COL_MESSAGE, row.getString(idx), relation));
            }
          }
        }
      }
    });

    BeeView.registerConditionProvider(TBL_PLACES, (view, args) -> {
      Map<String, String> params = Codec.deserializeLinkedHashMap(BeeUtils.peek(args));
      Long folderId = BeeUtils.toLong(params.get(COL_FOLDER));
      Long accountId = BeeUtils.toLong(params.get(COL_ACCOUNT));
      String sender = params.get(COL_SENDER);
      String recipient = params.get(MailConstants.COL_ADDRESS);
      String subject = params.get(COL_SUBJECT);
      DateTime dateFrom = TimeUtils.toDateTimeOrNull(params.get(Service.VAR_FROM));
      DateTime dateTo = TimeUtils.toDateTimeOrNull(params.get(Service.VAR_TO));
      String content = params.get(COL_CONTENT);

      SqlSelect query = new SqlSelect().setDistinctMode(true)
          .addFields(TBL_PLACES, sys.getIdName(TBL_PLACES))
          .addFrom(TBL_PLACES)
          .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE));

      HasConditions clause = SqlUtils.and();
      HasConditions subClause = SqlUtils.and();

      Function<String, IsExpression> xpr = col ->
          SqlUtils.field(view.getColumnSource(col), view.getColumnField(col));

      if (DataUtils.isId(folderId)) {
        clause.add(SqlUtils.equals(xpr.apply(COL_FOLDER), folderId));
        subClause.add(SqlUtils.equals(TBL_PLACES, COL_FOLDER, folderId));
      } else {
        clause.add(SqlUtils.equals(xpr.apply(COL_ACCOUNT), accountId));
        subClause.add(SqlUtils.equals(TBL_FOLDERS, COL_ACCOUNT, accountId));
        query.addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER));
      }
      if (Objects.nonNull(dateFrom)) {
        clause.add(SqlUtils.moreEqual(xpr.apply(COL_DATE), dateFrom));
        subClause.add(SqlUtils.moreEqual(TBL_MESSAGES, COL_DATE, dateFrom));
      }
      if (Objects.nonNull(dateTo)) {
        clause.add(SqlUtils.lessEqual(xpr.apply(COL_DATE), dateTo));
        subClause.add(SqlUtils.lessEqual(TBL_MESSAGES, COL_DATE, dateTo));
      }
      if (BeeUtils.toBoolean(params.get(MessageFlag.SEEN.name()))) {
        clause.add(SqlUtils.equals(SqlUtils.bitAnd(SqlUtils.nvl(xpr.apply(COL_FLAGS), 0),
            MessageFlag.SEEN.getMask()), 0));
        subClause.add(SqlUtils.equals(SqlUtils.bitAnd(SqlUtils.nvl(SqlUtils.field(TBL_PLACES,
            COL_FLAGS), 0), MessageFlag.SEEN.getMask()), 0));
      }
      if (BeeUtils.toBoolean(params.get(MessageFlag.FLAGGED.name()))) {
        clause.add(SqlUtils.positive(SqlUtils.bitAnd(xpr.apply(COL_FLAGS),
            MessageFlag.FLAGGED.getMask())));
        subClause.add(SqlUtils.positive(SqlUtils.bitAnd(TBL_PLACES, COL_FLAGS,
            MessageFlag.FLAGGED.getMask())));
      }
      if (BeeUtils.toBoolean(params.get(TBL_ATTACHMENTS))) {
        clause.add(SqlUtils.positive(xpr.apply(COL_ATTACHMENT_COUNT)));
        subClause.add(SqlUtils.positive(TBL_MESSAGES, COL_ATTACHMENT_COUNT));
      }
      if (!BeeUtils.isEmpty(subject)) {
        clause.add(SqlUtils.contains(xpr.apply(COL_SUBJECT), subject));
        subClause.add(SqlUtils.contains(TBL_MESSAGES, COL_SUBJECT, subject));
      }
      if (BeeUtils.anyNotEmpty(sender, content)) {
        query.addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_MESSAGES, COL_SENDER))
            .addFromLeft(TBL_ADDRESSBOOK,
                SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                    SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())));

        if (!BeeUtils.isEmpty(sender)) {
          clause.add(SqlUtils.or(SqlUtils.contains(xpr.apply(COL_SENDER + COL_EMAIL_ADDRESS),
              sender), SqlUtils.contains(xpr.apply(COL_SENDER + COL_EMAIL_LABEL), sender)));
          subClause.add(SqlUtils.or(SqlUtils.contains(TBL_EMAILS, COL_EMAIL_ADDRESS, sender),
              SqlUtils.contains(TBL_ADDRESSBOOK, COL_EMAIL_LABEL, sender)));
        }
      }
      if (BeeUtils.anyNotEmpty(recipient, content)) {
        String alsMails = SqlUtils.uniqueName();
        String alsBook = SqlUtils.uniqueName();

        query.addFromLeft(TBL_RECIPIENTS,
            sys.joinTables(TBL_MESSAGES, TBL_RECIPIENTS, COL_MESSAGE))
            .addFromLeft(TBL_EMAILS, alsMails,
                sys.joinTables(TBL_EMAILS, alsMails, TBL_RECIPIENTS, MailConstants.COL_ADDRESS))
            .addFromLeft(TBL_ADDRESSBOOK, alsBook,
                SqlUtils.and(sys.joinTables(TBL_EMAILS, alsMails, alsBook, COL_EMAIL),
                    SqlUtils.equals(alsBook, COL_USER, usr.getCurrentUserId())));

        if (!BeeUtils.isEmpty(recipient)) {
          subClause.add(SqlUtils.or(SqlUtils.contains(alsMails, COL_EMAIL_ADDRESS, recipient),
              SqlUtils.contains(alsBook, COL_EMAIL_LABEL, recipient)));
        }
        if (!BeeUtils.isEmpty(content)) {
          query.addFromLeft(TBL_PARTS, sys.joinTables(TBL_MESSAGES, TBL_PARTS, COL_MESSAGE));
          HasConditions cl = SqlUtils.or();

          if (BeeUtils.isEmpty(subject)) {
            cl.add(SqlUtils.contains(TBL_MESSAGES, COL_SUBJECT, content));
          }
          if (BeeUtils.isEmpty(sender)) {
            cl.add(SqlUtils.contains(TBL_EMAILS, COL_EMAIL_ADDRESS, content))
                .add(SqlUtils.contains(TBL_ADDRESSBOOK, COL_EMAIL_LABEL, content));
          }
          if (BeeUtils.isEmpty(recipient)) {
            cl.add(SqlUtils.contains(alsMails, COL_EMAIL_ADDRESS, content))
                .add(SqlUtils.contains(alsBook, COL_EMAIL_LABEL, content));
          }
          subClause.add(cl.add(SqlUtils.fullText(TBL_PARTS, COL_CONTENT + "FTS", content)));
        }
        clause.add(SqlUtils.in(TBL_PLACES, sys.getIdName(TBL_PLACES), query.setWhere(subClause)));
      }
      return clause;
    });

    QueryServiceBean.registerViewDataProvider(VIEW_USER_EMAILS, new ViewDataProvider() {
      @Override
      public BeeRowSet getViewData(BeeView view, SqlSelect query, Filter filter) {
        return qs.getViewData(getQuery(filter)
            .setLimit(query.getLimit())
            .setOffset(query.getOffset()), sys.getView(view.getName()), false);
      }

      @Override
      public int getViewSize(BeeView view, SqlSelect query, Filter filter) {
        return qs.sqlCount(getQuery(filter));
      }

      private Set<String> getFilterValues(Filter filter) {
        Set<String> values = new HashSet<>();

        if (filter instanceof CompoundFilter) {
          for (Filter subFilter : ((CompoundFilter) filter).getSubFilters()) {
            values.addAll(getFilterValues(subFilter));
          }
        } else if (filter instanceof ColumnValueFilter) {
          ((ColumnValueFilter) filter).getValue().forEach(v -> values.add(v.getString()));
        }
        return values;
      }

      private SqlSelect getQuery(Filter filter) {
        Set<String> values = getFilterValues(filter);
        HasConditions whAddressbook = SqlUtils.and();
        HasConditions whCompanies = SqlUtils.and();
        HasConditions whCompanyContacts = SqlUtils.and();
        HasConditions whPersons = SqlUtils.and();
        HasConditions whCompanyPersons = SqlUtils.and();
        IsExpression emailFld = SqlUtils.field(TBL_EMAILS, COL_EMAIL_ADDRESS);

        values.forEach(v -> {
          whAddressbook.add(SqlUtils.containsAny(v, emailFld,
              SqlUtils.field(TBL_ADDRESSBOOK, COL_ADDRESSBOOK_LABEL)));
          whCompanies.add(SqlUtils.containsAny(v, emailFld,
              SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME)));
          whCompanyContacts.add(SqlUtils.containsAny(v, emailFld,
              SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME),
              SqlUtils.field(TBL_CONTACTS, COL_NOTES)));
          whPersons.add(SqlUtils.containsAny(v, emailFld,
              SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME),
              SqlUtils.field(TBL_PERSONS, COL_LAST_NAME)));
          whCompanyPersons.add(SqlUtils.containsAny(v, emailFld,
              SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME),
              SqlUtils.field(TBL_PERSONS, COL_LAST_NAME),
              SqlUtils.field(TBL_POSITIONS, COL_POSITION_NAME)));
        });
        return new SqlSelect().setUnionAllMode(true)
            .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
            .addFields(TBL_ADDRESSBOOK, COL_ADDRESSBOOK_LABEL)
            .addFrom(TBL_EMAILS)
            .addFromInner(TBL_ADDRESSBOOK,
                SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                    SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())))
            .setWhere(whAddressbook)
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_COMPANIES,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
                .setWhere(whCompanies))
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addExpr(SqlUtils.concat(SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_CONTACTS, COL_NOTES), "''")),
                    COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_COMPANY_CONTACTS,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANY_CONTACTS, COL_CONTACT))
                .addFromInner(TBL_COMPANIES,
                    sys.joinTables(TBL_COMPANIES, TBL_COMPANY_CONTACTS, COL_COMPANY))
                .setWhere(whCompanyContacts))
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")),
                    COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_PERSONS,
                    sys.joinTables(TBL_CONTACTS, TBL_PERSONS, COL_CONTACT))
                .setWhere(whPersons))
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''"), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_POSITIONS, COL_POSITION_NAME), "''")),
                    COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_COMPANY_PERSONS,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
                .addFromInner(TBL_PERSONS,
                    sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
                .addFromLeft(TBL_POSITIONS,
                    sys.joinTables(TBL_POSITIONS, TBL_COMPANY_PERSONS, COL_POSITION))
                .setWhere(whCompanyPersons))
            .addOrder(null, COL_EMAIL_ADDRESS);
      }
    });

    news.registerUsageQueryProvider(Feed.MAIL, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        return new SqlSelect()
            .addFields(TBL_PLACES, sys.getIdName(TBL_PLACES))
            .addEmptyDateTime(COL_DATE)
            .addFrom(TBL_PLACES)
            .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
            .addFromInner(TBL_ACCOUNTS, sys.joinTables(TBL_ACCOUNTS, TBL_FOLDERS, COL_ACCOUNT))
            .addFromLeft(TBL_ACCOUNT_USERS,
                SqlUtils.and(sys.joinTables(TBL_ACCOUNTS, TBL_ACCOUNT_USERS, COL_ACCOUNT),
                    SqlUtils.equals(TBL_ACCOUNT_USERS, COL_USER, userId)))
            .setWhere(SqlUtils.and(SqlUtils.or(SqlUtils.equals(TBL_ACCOUNTS, COL_USER, userId),
                SqlUtils.equals(TBL_ACCOUNT_USERS, COL_USER, userId)),
                SqlUtils.equals(SqlUtils.bitAnd(SqlUtils.nvl(SqlUtils.field(TBL_PLACES,
                    COL_FLAGS), 0), MessageFlag.SEEN.getMask()), 0)));
      }

      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        return new SqlSelect().addConstant(0, "dummy").setWhere(SqlUtils.sqlFalse());
      }
    });
  }

  public ResponseObject sendMail(Long accountId, String to, String subject, String content) {
    return sendMail(accountId, new String[] {to}, null, null, subject, content, null, false);
  }

  public ResponseObject sendMail(Long accountId, String[] to, String[] cc, String[] bcc,
      String subject, String content, Map<Long, String> attachments, boolean storeInSentFolder) {

    MailAccount account = mail.getAccount(accountId);
    MimeMessage message;

    try {
      message = sendMail(account, to, cc, bcc, subject, content, attachments, null);
    } catch (MessagingException ex) {
      logger.error(ex, account.getTransportProtocol(), account.getTransportHost(),
          account.getTransportLogin());
      return ResponseObject.error(ex, account.getTransportProtocol());
    }
    if (storeInSentFolder) {
      try {
        return ResponseObject.response(storeMessage(account, message, account.getSentFolder()));
      } catch (MessagingException ex) {
        logger.error(ex, account.getStoreProtocol(), account.getStoreHost(),
            account.getStoreLogin(), account.getSentFolder().getName());
        return ResponseObject.error(ex, account.getStoreProtocol());
      }
    }
    return ResponseObject.emptyResponse();
  }

  public ResponseObject sendStyledMail(Long accountId, String to, String subject, String content,
      String headerContent, String[] taskObservers) {

    String emailContent = BeeUtils.join("", styleMailHeader(headerContent), content);
    return sendMail(accountId, new String[] {to}, taskObservers, null, subject, emailContent, null, false);
  }

  @Asynchronous
  public void setMessageFlag(Long placeId, MessageFlag flag, boolean on) throws MessagingException {
    SimpleRow row = qs.getRow(new SqlSelect()
        .addFields(TBL_PLACES, COL_FOLDER, COL_FLAGS, COL_MESSAGE_UID)
        .addFields(TBL_FOLDERS, COL_ACCOUNT)
        .addFrom(TBL_PLACES)
        .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
        .setWhere(sys.idEquals(TBL_PLACES, placeId)));

    if (Objects.isNull(row)) {
      return;
    }
    int oldValue = BeeUtils.unbox(row.getInt(COL_FLAGS));
    int value;

    if (on) {
      value = flag.set(oldValue);
    } else {
      value = flag.clear(oldValue);
    }
    if (value == oldValue) {
      return;
    }
    MailAccount account = mail.getAccount(row.getLong(COL_ACCOUNT));
    MailFolder folder = account.findFolder(row.getLong(COL_FOLDER));

    try {
      account.setFlag(folder, new long[] {BeeUtils.unbox(row.getLong(COL_MESSAGE_UID))}, flag, on);
    } catch (FolderOutOfSyncException e) {
      checkMail(account, folder);
      return;
    }
    mail.setFlags(value, sys.idEquals(TBL_PLACES, placeId));

    MailMessage mailMessage = new MailMessage(folder.getId());
    mailMessage.setFlag(flag);
    Endpoint.sendToUsers(account.getUsers(), mailMessage, null);
  }

  public String styleMailHeader(String headerContent) {
    return styleMailHeader(headerContent, null);
  }

  public String styleMailHeader(String headerContent, String headerColor) {
    String styledHeader;
    String headerTemplate = prm.getText(PRM_REMINDERS_MAIL_TEMPLATE);

    if (!BeeUtils.isEmpty(headerTemplate) && !BeeUtils.isEmpty(headerContent)
        && headerTemplate.contains(REMINDERS_MAIL_TEMPLATE_HEADER_TEXT)) {

      if (headerTemplate.contains(REMINDERS_MAIL_TEMPLATE_HEADER_COLOR)) {
        if (BeeUtils.isEmpty(headerColor)) {
          headerTemplate = headerTemplate.replace(
              REMINDERS_MAIL_TEMPLATE_HEADER_COLOR, REMINDERS_MAIL_DEFAULT_COLOR);
        } else {
          headerTemplate = headerTemplate.replace(
              REMINDERS_MAIL_TEMPLATE_HEADER_COLOR, headerColor);
        }

      }

      styledHeader = headerTemplate.replace(REMINDERS_MAIL_TEMPLATE_HEADER_TEXT, headerContent);

    } else {
      styledHeader = headerContent;
    }

    return styledHeader;
  }

  private void applyRules(Message message, long placeId, MailAccount account,
      MailFolder folder, SimpleRowSet rules) throws MessagingException {

    MailEnvelope envelope = new MailEnvelope(message);
    String sender = envelope.getSender() != null ? envelope.getSender().getAddress() : null;

    for (SimpleRow row : rules) {
      RuleCondition condition = EnumUtils.getEnumByIndex(RuleCondition.class,
          row.getInt(COL_RULE_CONDITION));

      boolean ok = false;
      Set<String> expr = new HashSet<>();

      switch (condition) {
        case ALL:
          ok = true;
          break;

        case RECIPIENTS:
          for (InternetAddress address : envelope.getRecipients().values()) {
            expr.add(address.getAddress());
          }
          break;

        case SENDER:
          expr.add(sender);
          break;

        case SUBJECT:
          expr.add(envelope.getSubject());
          break;
      }
      if (!ok) {
        String value = row.getValue(COL_RULE_CONDITION_OPTIONS);

        for (String x : expr) {
          ok = BeeUtils.containsSame(x, value);

          if (ok) {
            break;
          }
        }
      }
      if (!ok) {
        continue;
      }
      RuleAction action = EnumUtils.getEnumByIndex(RuleAction.class, row.getInt(COL_RULE_ACTION));

      String log = BeeUtils.joinWords(Localized.dictionary().mailRule() + ":",
          condition.getCaption(), row.getValue(COL_RULE_CONDITION_OPTIONS), action.getCaption());

      switch (action) {
        case COPY:
        case MOVE:
        case DELETE:
          MailFolder folderTo = null;

          if (EnumSet.of(RuleAction.COPY, RuleAction.MOVE).contains(action)) {
            folderTo = account.findFolder(row.getLong(COL_RULE_ACTION_OPTIONS));

            if (Objects.isNull(folderTo)) {
              logger.severe(log, ": Destination folder not found",
                  row.getLong(COL_RULE_ACTION_OPTIONS));
              continue;
            }
          }
          if (Objects.nonNull(folderTo)) {
            log += " " + BeeUtils.join("/", folderTo.getParent().getName(), folderTo.getName());
          }
          logger.debug(log);

          boolean move = EnumSet.of(RuleAction.MOVE, RuleAction.DELETE).contains(action);
          processMessages(account, folder, folderTo, Collections.singleton(placeId), move);

          if (move) {
            return;
          }
          break;

        case FLAG:
        case READ:
          logger.debug(log);

          setMessageFlag(placeId, action == RuleAction.FLAG
              ? MessageFlag.FLAGGED : MessageFlag.SEEN, true);
          break;

        case FORWARD:
          logger.debug(log, row.getValue(COL_RULE_ACTION_OPTIONS));

          Map<Long, String> attachments = new LinkedHashMap<>();

          SimpleRowSet rs = qs.getData(new SqlSelect()
              .addFields(TBL_ATTACHMENTS, AdministrationConstants.COL_FILE, COL_ATTACHMENT_NAME)
              .addFrom(TBL_ATTACHMENTS)
              .addFromInner(TBL_MESSAGES,
                  sys.joinTables(TBL_MESSAGES, TBL_ATTACHMENTS, COL_MESSAGE))
              .addFromInner(TBL_PLACES,
                  sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(sys.idEquals(TBL_PLACES, placeId)));

          for (SimpleRow attach : rs) {
            attachments.put(attach.getLong(AdministrationConstants.COL_FILE),
                attach.getValue(COL_ATTACHMENT_NAME));
          }
          rs = qs.getData(new SqlSelect()
              .addFields(TBL_PARTS, COL_CONTENT, COL_HTML_CONTENT)
              .addFrom(TBL_PARTS)
              .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PARTS, COL_MESSAGE))
              .addFromInner(TBL_PLACES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(sys.idEquals(TBL_PLACES, placeId)));

          Dictionary loc = Localized.dictionary();

          String content = BeeUtils.join("<br>", "---------- "
                  + loc.mailForwardedMessage() + " ----------",
              loc.mailFrom() + ": " + sender,
              loc.date() + ": " + envelope.getDate(),
              loc.mailSubject() + ": " + envelope.getSubject(),
              loc.mailTo() + ": " + account.getAddress());

          for (SimpleRow part : rs) {
            String text = BeeUtils.notEmpty(part.getValue(COL_HTML_CONTENT),
                part.getValue(COL_CONTENT));

            if (!BeeUtils.isEmpty(text)) {
              content += "<br><br>" + text;
            }
          }
          sendMail(account, new String[] {row.getValue(COL_RULE_ACTION_OPTIONS)}, null, null,
              envelope.getSubject(), content, attachments, null);
          break;

        case REPLY:
          if (!BeeUtils.isEmpty(sender)) {
            SimpleRow info = qs.getRow(new SqlSelect()
                .addField(TBL_ADDRESSBOOK, sys.getIdName(TBL_ADDRESSBOOK), TBL_ADDRESSBOOK)
                .addFields(TBL_ADDRESSBOOK, COL_ADDRESSBOOK_AUTOREPLY)
                .addFrom(TBL_PLACES)
                .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
                .addFromInner(TBL_ADDRESSBOOK,
                    SqlUtils.and(SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, account.getUserId()),
                        SqlUtils.join(TBL_MESSAGES, COL_SENDER, TBL_ADDRESSBOOK, COL_EMAIL)))
                .setWhere(sys.idEquals(TBL_PLACES, placeId)));

            if (Objects.nonNull(info) && !TimeUtils.sameDate(TimeUtils.today(),
                info.getDateTime(COL_ADDRESSBOOK_AUTOREPLY))) {
              logger.debug(log);

              content = row.getValue(COL_RULE_ACTION_OPTIONS).replace("\n", "<br>");
              Long signatureId = account.getSignatureId();

              if (DataUtils.isId(signatureId)) {
                content = BeeUtils.join(SIGNATURE_SEPARATOR, content,
                    qs.getValue(new SqlSelect()
                        .addFields(TBL_SIGNATURES, COL_SIGNATURE_CONTENT)
                        .addFrom(TBL_SIGNATURES)
                        .setWhere(sys.idEquals(TBL_SIGNATURES, signatureId))));
              }
              sendMail(account, new String[] {sender}, null, null,
                  BeeUtils.joinWords(Localized.dictionary().mailReplayPrefix(),
                      envelope.getSubject()), content, null, null);

              mail.setAutoReply(info.getLong(TBL_ADDRESSBOOK));
            }
          }
          break;
      }
    }
  }

  private MimeMessage buildMessage(MailAccount account, String[] to, String[] cc, String[] bcc,
      String subject, String content, Map<Long, String> attachments, String inReplyTo)
      throws MessagingException {

    MimeMessage message = new MimeMessage((Session) null);
    Map<RecipientType, String[]> recs = new HashMap<>();
    recs.put(RecipientType.TO, to);
    recs.put(RecipientType.CC, cc);
    recs.put(RecipientType.BCC, bcc);

    for (RecipientType type : recs.keySet()) {
      String[] emails = recs.get(type);

      if (!ArrayUtils.isEmpty(emails)) {
        List<Address> recipients = new ArrayList<>();

        for (String email : emails) {
          try {
            recipients.add(new InternetAddress(email, true));
          } catch (AddressException e) {
            logger.error(e);
          }
        }
        if (!BeeUtils.isEmpty(recipients)) {
          message.setRecipients(type, recipients.toArray(new Address[0]));
        }
      }
    }
    Address sender = new InternetAddress(account.getAddress(), true);
    message.setSender(sender);
    message.setFrom(sender);
    message.setSentDate(TimeUtils.toJava(new DateTime()));
    message.setSubject(subject, BeeConst.CHARSET_UTF8);

    MimeMultipart multi = null;

    if (!BeeUtils.isEmpty(attachments)) {
      multi = new MimeMultipart();

      for (Entry<Long, String> entry : attachments.entrySet()) {
        MimeBodyPart p;

        try {
          FileInfo fileInfo = fs.getFile(entry.getKey());

          p = new MimeBodyPart();
          p.attachFile(fileInfo.getFile(), fileInfo.getType(), null);
          p.setFileName(BeeUtils.notEmpty(entry.getValue(), fileInfo.getName()));

        } catch (IOException ex) {
          logger.error(ex);
          p = null;
        }
        if (p != null) {
          multi.addBodyPart(p);
        }
      }
    }
    if (HtmlUtils.hasHtml(content)) {
      MimeMultipart alternative = new MimeMultipart("alternative");

      MimeBodyPart p = new MimeBodyPart();
      p.setText(HtmlUtils.stripHtml(content), BeeConst.CHARSET_UTF8);
      alternative.addBodyPart(p);

      MimeMultipart related = new MimeMultipart("related");

      Map<String, String> relatedFiles = HtmlUtils.getFileReferences(content);
      String parsedContent = content;

      for (String hash : relatedFiles.keySet()) {
        try {
          FileInfo fileInfo = fs.getFile(fs.getId(hash));

          p = new MimeBodyPart();
          String cid = BeeUtils.randomString(10);

          try {
            p.attachFile(fileInfo.getFile(), fileInfo.getType(), null);
            p.addHeader("Content-ID", "<" + cid + ">");
            p.setFileName(fileInfo.getName());
          } catch (IOException ex) {
            logger.error(ex);
            p = null;
          }
          if (p != null) {
            parsedContent = parsedContent.replace(relatedFiles.get(hash), "cid:" + cid);
            related.addBodyPart(p);
          }
        } catch (IOException e) {
          logger.error(e);
        }
      }
      p = new MimeBodyPart();
      p.setText(parsedContent, BeeConst.CHARSET_UTF8, "html");

      if (related.getCount() > 0) {
        related.addBodyPart(p, 0);
        p = new MimeBodyPart();
        p.setContent(related);
      }
      alternative.addBodyPart(p);

      if (multi != null && multi.getCount() > 0) {
        p = new MimeBodyPart();
        p.setContent(alternative);
        multi.addBodyPart(p, 0);
      } else {
        multi = alternative;
      }
    } else if (multi != null && multi.getCount() > 0) {
      MimeBodyPart p = new MimeBodyPart();
      p.setText(content, BeeConst.CHARSET_UTF8);
      multi.addBodyPart(p, 0);
    }
    if (multi != null && multi.getCount() > 0) {
      message.setContent(multi);
    } else {
      message.setText(content, BeeConst.CHARSET_UTF8);
    }
    if (!BeeUtils.isEmpty(inReplyTo)) {
      message.addHeader(COL_IN_REPLY_TO, inReplyTo);
    }
    message.saveChanges();
    return new MimeMessage(message);
  }

  private int checkFolder(MailAccount account, Folder remoteFolder, MailFolder localFolder,
      boolean syncAll, String progressId) throws MessagingException {

    Assert.noNulls(remoteFolder, localFolder);

    int c = 0;
    SimpleRowSet rules = null;

    if (localFolder.isConnected() && MailAccount.holdsMessages(remoteFolder)) {
      boolean hasUid = remoteFolder instanceof UIDFolder;

      if (hasUid && !DataUtils.isId(localFolder.getUidValidity())) {
        try {
          remoteFolder.open(Folder.READ_WRITE);
        } catch (ReadOnlyFolderException e) {
          // Courier-IMAP server bug workaround
        } finally {
          if (remoteFolder.isOpen()) {
            try {
              remoteFolder.close(false);
            } catch (MessagingException e) {
              logger.warning(e);
            }
          }
        }
      }
      mail.validateFolder(localFolder, hasUid ? ((UIDFolder) remoteFolder).getUIDValidity() : null);

      try {
        int first = 1;
        int count;

        if (hasUid) {
          Pair<Integer, Integer> pair = syncFolder(account, localFolder, remoteFolder, progressId,
              syncAll);

          c += pair.getB();
          count = remoteFolder.getMessageCount();

          if (BeeUtils.isPositive(pair.getA())) {
            first = pair.getA() + 1;
            count = count - first + 1;
          }
        } else {
          remoteFolder.open(Folder.READ_ONLY);
          count = remoteFolder.getMessageCount();
        }
        if (!BeeUtils.isPositive(count)) {
          return c;
        }
        double l = 0;
        boolean stop = false;
        int start;
        int end = remoteFolder.getMessageCount();

        do {
          start = Math.max(end - CHUNK + 1, first);
          Message[] newMessages = remoteFolder.getMessages(start, end);
          end = start - 1;

          if (hasUid) {
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.FLAGS);
            fp.add(MailConstants.COL_IN_REPLY_TO);
            fp.add(UIDFolder.FetchProfileItem.UID);
            remoteFolder.fetch(newMessages, fp);
          }
          for (int i = newMessages.length - 1; i >= 0; i--) {
            if (!BeeUtils.isEmpty(progressId)) {
              if (!Endpoint.updateProgress(progressId, l / count)) {
                throw new ProgressInterruptedException(c);
              }
              l++;
            }
            Message message = newMessages[i];
            Long placeId = mail.storeMail(account, message, localFolder.getId(),
                hasUid ? ((UIDFolder) remoteFolder).getUID(message) : null).getB();

            if (DataUtils.isId(placeId)) {
              if (account.isInbox(localFolder)) {
                if (rules == null) {
                  rules = getRules(account.getAccountId());
                }
                if (!rules.isEmpty()) {
                  applyRules(message, placeId, account, localFolder, rules);
                }
              }
              c++;
            } else if (!syncAll) {
              stop = true; // POP3 resync
              break;
            }
          }
        } while (!stop && start > first);
      } catch (ProgressInterruptedException e) {
        c += e.getProcessed();
      } finally {
        if (remoteFolder.isOpen()) {
          try {
            remoteFolder.close(false);
          } catch (MessagingException e) {
            logger.warning(e);
          }
        }
      }
    }
    return c;
  }

  private void checkMail() {
    long now = System.currentTimeMillis();
    IsExpression blockFrom = SqlUtils.field(AdministrationConstants.TBL_USERS,
        AdministrationConstants.COL_USER_BLOCK_FROM);
    IsExpression blockUntil = SqlUtils.field(AdministrationConstants.TBL_USERS,
        AdministrationConstants.COL_USER_BLOCK_UNTIL);

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addField(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS), COL_ACCOUNT)
        .addFields(TBL_ACCOUNTS, COL_ACCOUNT_SYNC_MODE)
        .addFrom(TBL_ACCOUNTS)
        .addFromInner(AdministrationConstants.TBL_USERS,
            SqlUtils.and(sys.joinTables(AdministrationConstants.TBL_USERS, TBL_ACCOUNTS, COL_USER),
                SqlUtils.or(SqlUtils.and(SqlUtils.isNull(blockFrom), SqlUtils.isNull(blockUntil)),
                    SqlUtils.and(SqlUtils.notNull(blockFrom), SqlUtils.more(blockFrom, now)),
                    SqlUtils.and(SqlUtils.notNull(blockUntil), SqlUtils.less(blockUntil, now)))))
        .setWhere(SqlUtils.or(SqlUtils.isNull(TBL_ACCOUNTS, COL_ACCOUNT_SYNC_MODE),
            SqlUtils.notEqual(TBL_ACCOUNTS, COL_ACCOUNT_SYNC_MODE, SyncMode.SYNC_NOTHING))));

    for (SimpleRow row : rs) {
      MailAccount account = mail.getAccount(row.getLong(COL_ACCOUNT));

      checkMail(account, Objects.equals(row.getInt(COL_ACCOUNT_SYNC_MODE),
          SyncMode.SYNC_ALL.ordinal()) ? account.getRootFolder() : account.getInboxFolder());
    }
  }

  private void checkMail(MailAccount account, MailFolder localFolder) {
    checkMail(true, account, localFolder, false);
  }

  private int copyMessages(MailAccount account, List<Long> places, MailFolder target,
      boolean move) throws MessagingException {

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_FOLDER)
        .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
        .addFrom(TBL_PLACES)
        .setWhere(sys.idInList(TBL_PLACES, places)));

    int c = 0;
    Multimap<Long, Long> folders = HashMultimap.create();

    for (SimpleRow row : data) {
      folders.put(row.getLong(COL_FOLDER), row.getLong(COL_PLACE));
    }
    for (Long folderId : folders.keySet()) {
      MailFolder source = account.findFolder(folderId);

      if (!move || source != target) {
        c += processMessages(account, source, target, folders.get(folderId), move);
      }
    }
    return c;
  }

  private void disconnectFolder(MailAccount account, MailFolder folder) throws MessagingException {
    for (MailFolder subFolder : folder.getSubFolders()) {
      disconnectFolder(account, subFolder);
    }
    account.dropRemoteFolder(folder);
    mail.disconnectFolder(folder);
  }

  private ResponseObject getMessage(Long messageId, Long placeId) {
    Assert.isTrue(BeeUtils.anyNotNull(messageId, placeId));

    Map<String, SimpleRowSet> packet = new HashMap<>();
    String sent = SystemFolder.Sent.name();
    String drafts = SystemFolder.Drafts.name();

    SqlSelect query = new SqlSelect()
        .addField(TBL_MESSAGES, sys.getIdName(TBL_MESSAGES), COL_MESSAGE)
        .addFields(TBL_MESSAGES, COL_DATE, COL_SUBJECT, COL_UNIQUE_ID, COL_IN_REPLY_TO)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFields(TBL_ADDRESSBOOK, COL_EMAIL_LABEL)
        .addFrom(TBL_MESSAGES)
        .addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_MESSAGES, COL_SENDER))
        .addFromLeft(TBL_ADDRESSBOOK,
            SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())));

    if (DataUtils.isId(placeId)) {
      query.addFields(TBL_PLACES, COL_FLAGS, COL_FOLDER)
          .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
          .addExpr(SqlUtils.expression(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
              SqlUtils.field(TBL_ACCOUNTS, sent + COL_FOLDER))), sent)
          .addExpr(SqlUtils.expression(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
              SqlUtils.field(TBL_ACCOUNTS, drafts + COL_FOLDER))), drafts)
          .addFields(TBL_ACCOUNTS, COL_USER)
          .addFields(TBL_FOLDERS, COL_ACCOUNT)
          .addFromInner(TBL_PLACES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
          .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
          .addFromInner(TBL_ACCOUNTS, sys.joinTables(TBL_ACCOUNTS, TBL_FOLDERS, COL_ACCOUNT))
          .setWhere(sys.idEquals(TBL_PLACES, placeId));
    } else {
      query.addEmptyLong(COL_PLACE)
          .addEmptyLong(COL_FOLDER)
          .addEmptyBoolean(sent)
          .addEmptyBoolean(drafts)
          .setWhere(sys.idEquals(TBL_MESSAGES, messageId));
    }
    SimpleRow msg = qs.getRow(query);

    if (Objects.isNull(msg)) {
      return ResponseObject.error(usr.getDictionary().nothingFound());
    }
    packet.put(TBL_MESSAGES, msg.getRowSet());

    if (DataUtils.isId(placeId)) {
      IsCondition wh = SqlUtils.equals(TBL_MESSAGES, COL_IN_REPLY_TO, msg.getValue(COL_UNIQUE_ID));

      if (!BeeUtils.isEmpty(msg.getValue(COL_IN_REPLY_TO))) {
        wh = SqlUtils.or(wh,
            SqlUtils.equals(TBL_MESSAGES, COL_UNIQUE_ID, msg.getValue(COL_IN_REPLY_TO)));
      }
      packet.put(COL_IN_REPLY_TO, qs.getData(new SqlSelect()
          .addFields(TBL_MESSAGES, COL_DATE, COL_SUBJECT, COL_IN_REPLY_TO)
          .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
          .addFrom(TBL_PLACES)
          .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
          .addFromInner(TBL_FOLDERS,
              SqlUtils.and(sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER),
                  SqlUtils.equals(TBL_FOLDERS, COL_ACCOUNT, msg.getLong(COL_ACCOUNT))))
          .setWhere(wh)
          .addOrder(TBL_MESSAGES, COL_DATE)));
    }
    Long message = msg.getLong(COL_MESSAGE);
    IsCondition wh = SqlUtils.equals(TBL_RECIPIENTS, COL_MESSAGE, message);

    if (!DataUtils.isId(placeId)
        || !Objects.equals(msg.getLong(COL_USER), usr.getCurrentUserId())) {
      wh = SqlUtils.and(wh,
          SqlUtils.notEqual(TBL_RECIPIENTS, COL_ADDRESS_TYPE, AddressType.BCC.name()));
    }
    packet.put(TBL_RECIPIENTS, qs.getData(new SqlSelect()
        .addFields(TBL_RECIPIENTS, COL_ADDRESS_TYPE)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFields(TBL_ADDRESSBOOK, COL_EMAIL_LABEL)
        .addFrom(TBL_RECIPIENTS)
        .addFromInner(TBL_EMAILS,
            sys.joinTables(TBL_EMAILS, TBL_RECIPIENTS, MailConstants.COL_ADDRESS))
        .addFromLeft(TBL_ADDRESSBOOK,
            SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())))
        .setWhere(wh)
        .addOrderDesc(TBL_RECIPIENTS, COL_ADDRESS_TYPE)
        .addOrder(TBL_RECIPIENTS, sys.getIdName(TBL_RECIPIENTS))));

    String[] cols = new String[] {COL_CONTENT, COL_HTML_CONTENT};

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_PARTS, cols)
        .addFrom(TBL_PARTS)
        .setWhere(SqlUtils.equals(TBL_PARTS, COL_MESSAGE, message))
        .addOrder(TBL_PARTS, sys.getIdName(TBL_PARTS)));

    for (SimpleRow row : rs) {
      row.setValue(COL_HTML_CONTENT, HtmlUtils.cleanHtml(row.getValue(COL_HTML_CONTENT)));
    }
    packet.put(TBL_PARTS, rs);

    packet.put(TBL_ATTACHMENTS, qs.getData(new SqlSelect()
        .addFields(TBL_ATTACHMENTS, AdministrationConstants.COL_FILE, COL_ATTACHMENT_NAME)
        .addFields(AdministrationConstants.TBL_FILES, AdministrationConstants.COL_FILE_HASH,
            AdministrationConstants.COL_FILE_NAME, AdministrationConstants.COL_FILE_SIZE,
            AdministrationConstants.COL_FILE_TYPE)
        .addFrom(TBL_ATTACHMENTS)
        .addFromInner(AdministrationConstants.TBL_FILES,
            sys.joinTables(AdministrationConstants.TBL_FILES, TBL_ATTACHMENTS,
                AdministrationConstants.COL_FILE))
        .setWhere(SqlUtils.equals(TBL_ATTACHMENTS, COL_MESSAGE, message))
        .addOrder(TBL_ATTACHMENTS, sys.getIdName(TBL_ATTACHMENTS))));

    if (DataUtils.isId(placeId) && !MessageFlag.SEEN.isSet(msg.getInt(COL_FLAGS))) {
      try {
        ctx.getBusinessObject(this.getClass()).setMessageFlag(placeId, MessageFlag.SEEN, true);
      } catch (MessagingException e) {
        logger.error(e);
      }
    }
    return ResponseObject.response(packet);
  }

  private ResponseObject getNewsletterContacts(RequestInfo reqInfo) {
    List<Long> ids = Codec.deserializeIdList(reqInfo.getParameter(Service.VAR_DATA));
    String column = reqInfo.getParameter(Service.VAR_COLUMN);
    String newsletterId = reqInfo.getParameter(COL_NEWSLETTER);
    Set<Long> emailList = new HashSet<>();
    String source = "";
    SqlSelect contactsQuery;

    if (BeeUtils.isEmpty(ids) || !BeeUtils.isLong(newsletterId)) {
      return ResponseObject.error("Wrong newsletter ID or Data is empty");
    }

    if (!BeeUtils.isEmpty(column)) {

      switch (column) {
        case COL_COMPANY:
          source = TBL_COMPANIES;
          break;

        case COL_COMPANY_PERSON:
          source = TBL_COMPANY_PERSONS;
          break;

        case COL_COMPANY_CONTACT:
          source = TBL_COMPANY_CONTACTS;
          break;

        case COL_PERSON:
          source = TBL_PERSONS;
          break;
      }

      contactsQuery = new SqlSelect()
          .addFields(TBL_CONTACTS, COL_EMAIL)
          .addFrom(source)
          .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, source, COL_CONTACT))
          .setWhere(sys.idInList(source, ids));

    } else {
      contactsQuery =
          new SqlSelect()
              .addFields(TBL_CONTACTS, COL_EMAIL)
              .addFrom(VIEW_RCPS_GROUPS_CONTACTS)
              .addFromInner(TBL_COMPANIES,
                  sys.joinTables(TBL_COMPANIES, VIEW_RCPS_GROUPS_CONTACTS, COL_COMPANY))
              .addFromLeft(TBL_CONTACTS,
                  sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
              .setWhere(
                  SqlUtils.and(SqlUtils
                      .inList(VIEW_RCPS_GROUPS_CONTACTS, COL_RECIPIENTS_GROUP, ids), SqlUtils
                      .notNull(TBL_CONTACTS, COL_EMAIL)))
              .addUnion(
                  new SqlSelect()
                      .addFields(TBL_CONTACTS, COL_EMAIL)
                      .addFrom(VIEW_RCPS_GROUPS_CONTACTS)
                      .addFromInner(TBL_COMPANY_PERSONS,
                          sys.joinTables(TBL_COMPANY_PERSONS, VIEW_RCPS_GROUPS_CONTACTS,
                              COL_COMPANY_PERSON))
                      .addFromLeft(TBL_CONTACTS,
                          sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
                      .setWhere(
                          SqlUtils
                              .and(SqlUtils.inList(VIEW_RCPS_GROUPS_CONTACTS, COL_RECIPIENTS_GROUP,
                                  ids), SqlUtils.notNull(TBL_CONTACTS, COL_EMAIL))))
              .addUnion(
                  new SqlSelect()
                      .addFields(TBL_CONTACTS, COL_EMAIL)
                      .addFrom(VIEW_RCPS_GROUPS_CONTACTS)
                      .addFromInner(TBL_COMPANY_CONTACTS,
                          sys.joinTables(TBL_COMPANY_CONTACTS, VIEW_RCPS_GROUPS_CONTACTS,
                              COL_COMPANY_CONTACT))
                      .addFromLeft(TBL_CONTACTS,
                          sys.joinTables(TBL_CONTACTS, TBL_COMPANY_CONTACTS, COL_CONTACT))
                      .setWhere(
                          SqlUtils
                              .and(SqlUtils.inList(VIEW_RCPS_GROUPS_CONTACTS, COL_RECIPIENTS_GROUP,
                                  ids), SqlUtils.notNull(TBL_CONTACTS, COL_EMAIL))))
              .addUnion(
                  new SqlSelect()
                      .addFields(TBL_CONTACTS, COL_EMAIL)
                      .addFrom(VIEW_RCPS_GROUPS_CONTACTS)
                      .addFromInner(TBL_PERSONS,
                          sys.joinTables(TBL_PERSONS, VIEW_RCPS_GROUPS_CONTACTS,
                              COL_PERSON))
                      .addFromLeft(TBL_CONTACTS,
                          sys.joinTables(TBL_CONTACTS, TBL_PERSONS, COL_CONTACT))
                      .setWhere(
                          SqlUtils
                              .and(SqlUtils.inList(VIEW_RCPS_GROUPS_CONTACTS,
                                  COL_RECIPIENTS_GROUP,
                                  ids), SqlUtils.notNull(TBL_CONTACTS, COL_EMAIL))));
    }

    emailList.addAll(qs.getLongSet(contactsQuery));

    SqlSelect selectOldEmails = new SqlSelect()
        .addFields(VIEW_NEWSLETTER_CONTACTS, COL_EMAIL)
        .addFrom(VIEW_NEWSLETTER_CONTACTS)
        .setWhere(SqlUtils.equals(VIEW_NEWSLETTER_CONTACTS, COL_NEWSLETTER, newsletterId));

    if (emailList.size() > 0) {
      Set<Long> oldEmails = qs.getLongSet(selectOldEmails);
      emailList.removeAll(oldEmails);
      for (Long email : emailList) {
        SqlInsert si = new SqlInsert(VIEW_NEWSLETTER_CONTACTS)
            .addConstant(COL_NEWSLETTER, newsletterId)
            .addConstant(COL_EMAIL, email);
        qs.insertDataWithResponse(si);
      }
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject getRawContent(Long messageId) {
    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_MESSAGES, COL_RAW_CONTENT)
        .addFields(TBL_FOLDERS, COL_ACCOUNT)
        .addFields(TBL_PLACES, COL_FOLDER, COL_MESSAGE_UID)
        .addFrom(TBL_MESSAGES)
        .addFromLeft(TBL_PLACES, SqlUtils.and(sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE),
            SqlUtils.notNull(TBL_PLACES, COL_MESSAGE_UID)))
        .addFromLeft(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
        .setWhere(sys.idEquals(TBL_MESSAGES, messageId)));

    FileInfo raw = null;
    Long rawId = Arrays.stream(rs.getLongColumn(COL_RAW_CONTENT))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);

    if (DataUtils.isId(rawId)) {
      try {
        raw = fs.getFile(rawId);
      } catch (IOException e) {
        logger.error(e);
      }
    } else {
      for (SimpleRow row : rs) {
        if (Objects.isNull(row.getLong(COL_MESSAGE_UID))) {
          continue;
        }
        MailAccount account = mail.getAccount(row.getLong(COL_ACCOUNT));
        MailAccount.MailStore store = null;

        try {
          store = account.connect();
          MailFolder localFolder = account.findFolder(row.getLong(COL_FOLDER));
          Folder remoteFolder = account.getRemoteFolder(store.get(), localFolder);

          remoteFolder.open(Folder.READ_ONLY);
          Message msg = ((UIDFolder) remoteFolder).getMessageByUID(row.getLong(COL_MESSAGE_UID));

          ByteArrayOutputStream os = new ByteArrayOutputStream();
          msg.writeTo(os);
          os.close();
          remoteFolder.close(false);

          raw = fs.storeFile(new ByteArrayInputStream(os.toByteArray()),
              "mail@" + SqlUtils.uniqueName(), MediaType.TEXT_PLAIN);
          break;
        } catch (Throwable e) {
          logger.error(e);
        } finally {
          account.disconnect(store);
        }
      }
    }
    if (Objects.nonNull(raw)) {
      return ResponseObject.response(raw);
    }
    return ResponseObject.error(usr.getDictionary().noData());
  }

  private SimpleRowSet getRules(Long accountId) {
    return qs.getData(new SqlSelect()
        .addFields(TBL_RULES, COL_RULE_CONDITION, COL_RULE_CONDITION_OPTIONS,
            COL_RULE_ACTION, COL_RULE_ACTION_OPTIONS)
        .addExpr(SqlUtils.sqlCase(SqlUtils.field(TBL_RULES, COL_RULE_ACTION),
            RuleAction.MOVE, 1, RuleAction.DELETE, 2, 0), COL_ITEM_ORDINAL)
        .addFrom(TBL_RULES)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_RULES, COL_ACCOUNT, accountId),
            SqlUtils.notNull(TBL_RULES, COL_RULE_ACTIVE)))
        .addOrder(null, COL_ITEM_ORDINAL));
  }

  private int processMessages(MailAccount account, MailFolder source, MailFolder target,
      Collection<Long> places, boolean move) throws MessagingException {

    Assert.notNull(source);
    Assert.notEmpty(places);

    IsCondition wh = sys.idInList(TBL_PLACES, places);
    boolean checkMail = false;

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_MESSAGE, COL_FLAGS, COL_MESSAGE_UID)
        .addFrom(TBL_PLACES)
        .setWhere(wh));

    List<Long> lst = new ArrayList<>();

    for (SimpleRow row : data) {
      Long id = row.getLong(COL_MESSAGE_UID);

      if (id != null) {
        lst.add(id);
      }
    }
    long[] uids = new long[lst.size()];

    for (int i = 0; i < lst.size(); i++) {
      uids[i] = lst.get(i);
    }
    if (target != null) {
      if (account.isStoredRemotedly(target)) {
        try {
          checkMail = account.processMessages(uids, source, target, move);
        } catch (FolderOutOfSyncException e) {
          checkMail(account, source);
          return 0;
        }
        if (checkMail) {
          uids = new long[0];
        } else {
          SimpleRowSet contents = qs.getData(new SqlSelect()
              .addFields(TBL_MESSAGES, COL_RAW_CONTENT)
              .addFields(TBL_PLACES, COL_FLAGS)
              .addFrom(TBL_MESSAGES)
              .addFromInner(TBL_PLACES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(SqlUtils.and(wh, SqlUtils.notNull(TBL_MESSAGES, COL_RAW_CONTENT))));

          for (SimpleRow content : contents) {
            try (InputStream is = new FileInputStream(fs.getFile(content.getLong(COL_RAW_CONTENT))
                .getFile())) {
              MimeMessage message = new MimeMessage(null, is);
              Flags on = new Flags();
              Flags off = new Flags();

              for (MessageFlag messageFlag : MessageFlag.values()) {
                Flags flags = messageFlag.isSet(content.getInt(COL_FLAGS)) ? on : off;
                Flag flag = MailEnvelope.getFlag(messageFlag);

                if (flag != null) {
                  flags.add(flag);
                } else {
                  flags.add(messageFlag.name());
                }
              }
              message.setFlags(on, true);
              message.setFlags(off, false);
              storeMail(account, message, target);

            } catch (IOException e) {
              throw new MessagingException(e.getMessage());
            }
          }
          if (contents.getNumberOfRows() < data.getNumberOfRows()) {
            throw new MessagingException(
                "Some messages can't be transfered from disconnected folder");
          }
        }
      } else {
        Map<Long, Integer> messages = new HashMap<>();

        for (SimpleRow row : data) {
          messages.put(row.getLong(COL_MESSAGE), row.getInt(COL_FLAGS));
        }
        mail.attachMessages(target.getId(), messages);

        MailMessage mailMessage = new MailMessage(target.getId());
        mailMessage.setMessagesUpdated(true);
        Endpoint.sendToUsers(account.getUsers(), mailMessage, null);

        for (Long msgId : qs.getLongColumn(new SqlSelect().setDistinctMode(true)
            .addFields(TBL_PLACES, COL_MESSAGE)
            .addFrom(TBL_PLACES)
            .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
            .setWhere(SqlUtils.and(wh, SqlUtils.isNull(TBL_MESSAGES, COL_RAW_CONTENT))))) {

          ResponseObject resp = getRawContent(msgId);

          if (resp.hasResponse(FileInfo.class)) {
            mail.updateMessage(msgId, ImmutableMap.of(COL_RAW_CONTENT,
                ((FileInfo) resp.getResponse()).getId()));
          }
        }
      }
    }
    if (move) {
      try {
        account.processMessages(uids, source, null, true);
      } catch (FolderOutOfSyncException e) {
        checkMail(account, source);
        return 0;
      }
      mail.detachMessages(wh);

      MailMessage mailMessage = new MailMessage(source.getId());
      mailMessage.setMessagesUpdated(true);
      Endpoint.sendToUsers(account.getUsers(), mailMessage, null);
    }
    if (checkMail) {
      checkMail(account, target);
    }
    return data.getNumberOfRows();
  }

  private int removeMessages(MailAccount account, List<Long> places)
      throws MessagingException {

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_FOLDER)
        .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
        .addFrom(TBL_PLACES)
        .setWhere(sys.idInList(TBL_PLACES, places)));

    int c = 0;
    Multimap<Long, Long> folders = HashMultimap.create();

    for (SimpleRow row : data) {
      folders.put(row.getLong(COL_FOLDER), row.getLong(COL_PLACE));
    }
    for (Long folderId : folders.keySet()) {
      MailFolder source = account.findFolder(folderId);
      MailFolder target = account.getTrashFolder();

      if (source == account.getDraftsFolder() || source == target) {
        target = null;
      }
      c += processMessages(account, source, target, folders.get(folderId), true);
    }
    return c;
  }

  private MimeMessage sendMail(MailAccount account, String[] to, String[] cc, String[] bcc,
      String subject, String content, Map<Long, String> attachments, String inReplyTo)
      throws MessagingException {

    Transport transport = null;
    MimeMessage message;

    try {
      message = buildMessage(account, to, cc, bcc, subject, content, attachments, inReplyTo);
      Address[] recipients = message.getAllRecipients();

      if (recipients == null || recipients.length == 0) {
        throw new MessagingException("No recipients");
      }
      transport = account.connectToTransport();
      transport.sendMessage(message, recipients);
    } finally {
      if (transport != null) {
        try {
          transport.close();
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
    }
    return message;
  }

  private void sendNewsletter() {
    Integer count = prm.getInteger(PRM_SEND_NEWSLETTERS_COUNT);
    Long accountId = getSenderAccountId(PRM_DEFAULT_ACCOUNT);

    if (BeeUtils.unbox(count) > 0 && DataUtils.isId(accountId)
        && BeeUtils.isNonNegative(prm.getInteger(PRM_SEND_NEWSLETTERS_INTERVAL))) {
      SqlSelect newsQry = new SqlSelect()
          .addAllFields(VIEW_NEWSLETTERS)
          .addFrom(VIEW_NEWSLETTERS)
          .setWhere(SqlUtils.notNull(VIEW_NEWSLETTERS, "Active"));

      SimpleRowSet newsInfo = qs.getData(newsQry);
      if (newsInfo.getNumberOfRows() > 0) {
        for (SimpleRow sr : newsInfo) {
          SqlSelect contacts = new SqlSelect()
              .addFields(TBL_EMAILS, COL_EMAIL)
              .addField(VIEW_NEWSLETTER_CONTACTS, COL_EMAIL, "EmailId")
              .addFrom(VIEW_NEWSLETTER_CONTACTS)
              .addFromLeft(TBL_EMAILS,
                  sys.joinTables(TBL_EMAILS, VIEW_NEWSLETTER_CONTACTS, COL_EMAIL))
              .setWhere(
                  SqlUtils.and(SqlUtils.equals(VIEW_NEWSLETTER_CONTACTS, COL_NEWSLETTER,
                      sr.getValue(sys.getIdName(VIEW_NEWSLETTERS))), SqlUtils.isNull(
                      VIEW_NEWSLETTER_CONTACTS, COL_DATE))).setLimit(count);

          SimpleRowSet emailSet = qs.getData(contacts);

          if (emailSet.getNumberOfRows() > 0) {
            SqlSelect query = new SqlSelect()
                .addFields(VIEW_NEWSLETTER_FILES, AdministrationConstants.COL_FILE,
                    CalendarConstants.COL_CAPTION)
                .addFrom(VIEW_NEWSLETTER_FILES)
                .setWhere(SqlUtils.equals(VIEW_NEWSLETTER_FILES, COL_NEWSLETTER, sr.getValue(sys
                    .getIdName(VIEW_NEWSLETTERS))));

            Map<Long, String> attachments = new HashMap<>();
            SimpleRowSet attachList = qs.getData(query);

            if (attachList.getNumberOfRows() > 0) {
              for (SimpleRow attach : attachList) {
                attachments.put(attach.getLong(AdministrationConstants.COL_FILE), attach
                    .getValue(CalendarConstants.COL_CAPTION));
              }
            }
            boolean visibleCopies = BeeUtils.toBoolean(sr.getValue(COL_NEWSLETTER_VISIBLE_COPIES));
            if (visibleCopies) {
              sendMail(accountId, null, emailSet.getColumn(COL_EMAIL), null,
                  sr.getValue(COL_SUBJECT),
                  sr.getValue(COL_CONTENT), attachments, true);
            } else {
              sendMail(accountId, null, null, emailSet.getColumn(COL_EMAIL),
                  sr.getValue(COL_SUBJECT),
                  sr.getValue(COL_CONTENT), attachments, true);
            }
            for (Long email : emailSet.getLongColumn("EmailId")) {
              qs.updateData(new SqlUpdate(VIEW_NEWSLETTER_CONTACTS)
                  .addConstant(COL_DATE, TimeUtils.nowMillis())
                  .setWhere(SqlUtils.and(SqlUtils.equals(VIEW_NEWSLETTER_CONTACTS, COL_EMAIL,
                      email), SqlUtils.equals(VIEW_NEWSLETTER_CONTACTS, COL_NEWSLETTER,
                      sr.getValue(sys.getIdName(VIEW_NEWSLETTERS))))));
            }
          } else {
            qs.updateData(new SqlUpdate(VIEW_NEWSLETTERS)
                .addConstant("Active", null)
                .setWhere(SqlUtils.equals(VIEW_NEWSLETTERS, sys.getIdName(VIEW_NEWSLETTERS),
                    sr.getValue(sys.getIdName(VIEW_NEWSLETTERS)))));
          }
        }
      }
    }
  }

  private Long storeMail(MailAccount account, MimeMessage message, MailFolder folder)
      throws MessagingException {

    Long messageId;

    if (account.addMessageToRemoteFolder(message, folder)) {
      messageId = mail.storeMail(account, message, folder.getId(), BeeConst.LONG_UNDEF).getA();
      checkMail(account, folder);
    } else {
      messageId = mail.storeMail(account, message, folder.getId(), null).getA();

      MailMessage mailMessage = new MailMessage(folder.getId());
      mailMessage.setMessagesUpdated(true);
      Endpoint.sendToUsers(account.getUsers(), mailMessage, null);
    }
    return messageId;
  }

  private Long storeMessage(MailAccount account, MimeMessage message, MailFolder folder)
      throws MessagingException {
    message.setFlag(Flag.SEEN, true);
    return storeMail(account, message, folder);
  }

  private Pair<Integer, Integer> syncFolder(MailAccount account, MailFolder localFolder,
      Folder remoteFolder, String progressId, boolean syncAll)
      throws MessagingException, ProgressInterruptedException {

    Assert.noNulls(localFolder, remoteFolder);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_PLACES, COL_FLAGS, COL_MESSAGE_UID)
        .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
        .addFrom(TBL_PLACES)
        .setWhere(SqlUtils.equals(TBL_PLACES, COL_FOLDER, localFolder.getId()))
        .addOrderDesc(TBL_PLACES, COL_MESSAGE_UID)
        .setLimit(CHUNK);

    SimpleRowSet data = qs.getData(query);
    int size = data.getNumberOfRows();
    long modseq = BeeUtils.unbox(localFolder.getModSeq());

    int start = 0;
    int lastNo = 0;
    int cnt = 0;

    if (remoteFolder instanceof IMAPFolder
        && ((IMAPStore) remoteFolder.getStore()).hasCapability("CONDSTORE")) {

      IMAPFolder imapFolder = (IMAPFolder) remoteFolder;
      imapFolder.open(Folder.READ_ONLY, ResyncData.CONDSTORE);

      if (!syncAll && size > 1 && modseq > 0) {
        long uidLast = BeeUtils.unbox(data.getLong(0, COL_MESSAGE_UID));
        Message[] msgs = imapFolder.getMessagesByUID(new long[] {
            BeeUtils.unbox(data.getLong(size - 1, COL_MESSAGE_UID)), uidLast});

        if (BeeUtils.allNotNull(msgs[0], msgs[1])
            && msgs[1].getMessageNumber() - msgs[0].getMessageNumber() + 1 == size) {
          lastNo = msgs[1].getMessageNumber();

          for (Message msg : imapFolder.getMessagesByUIDChangedSince(1, uidLast, modseq)) {
            Integer flags = MailEnvelope.getFlagMask(msg);

            cnt += mail.setFlags(flags, SqlUtils.and(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
                localFolder.getId(), COL_MESSAGE_UID, imapFolder.getUID(msg)),
                SqlUtils.or(Objects.isNull(flags) ? null : SqlUtils.isNull(TBL_PLACES, COL_FLAGS),
                    SqlUtils.notEqual(TBL_PLACES, COL_FLAGS, flags))));
          }
        }
      }
      modseq = imapFolder.getHighestModSeq();
    } else {
      remoteFolder.open(Folder.READ_ONLY);
    }
    long uidLast = 0;

    if (lastNo == 0) {
      while (size > 0) {
        uidLast = BeeUtils.unbox(data.getLong(size - 1, COL_MESSAGE_UID));

        Message[] msgs = ((UIDFolder) remoteFolder).getMessagesByUID(uidLast,
            BeeUtils.unbox(data.getLong(0, COL_MESSAGE_UID)));

        if (!ArrayUtils.isEmpty(msgs)) {
          start = msgs[0].getMessageNumber();
          lastNo = msgs[msgs.length - 1].getMessageNumber();

          FetchProfile fp = new FetchProfile();
          fp.add(FetchProfile.Item.FLAGS);
          remoteFolder.fetch(msgs, fp);
        }
        try {
          cnt += syncMessages(data, msgs, account, localFolder, remoteFolder, progressId);
        } catch (ProgressInterruptedException e) {
          throw new ProgressInterruptedException(cnt + e.getProcessed());
        }
        if (lastNo > 0) {
          break;
        }
        data = qs.getData(query.setOffset(query.getOffset() + query.getLimit()));
        size = data.getNumberOfRows();
      }
    }
    if (syncAll && start > 0) {
      HasConditions clause = SqlUtils.and();

      query.resetOrder().setOffset(0).setLimit(0)
          .setWhere(SqlUtils.and(query.getWhere(), clause));

      int end = start - 1;

      while (end > 0) {
        start = Math.max(end - CHUNK + 1, 1);
        Message[] msgs = remoteFolder.getMessages(start, end);
        end = start - 1;

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.FLAGS);
        fp.add(UIDFolder.FetchProfileItem.UID);
        remoteFolder.fetch(msgs, fp);

        long uidTo = ((UIDFolder) remoteFolder).getUID(msgs[msgs.length - 1]);

        if (uidLast > uidTo + 1) {
          cnt += mail.detachMessages(SqlUtils.and(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
              localFolder.getId()), SqlUtils.more(TBL_PLACES, COL_MESSAGE_UID, uidTo),
              SqlUtils.less(TBL_PLACES, COL_MESSAGE_UID, uidLast)));
        }
        uidLast = ((UIDFolder) remoteFolder).getUID(msgs[0]);
        clause.clear();
        clause.add(SqlUtils.moreEqual(TBL_PLACES, COL_MESSAGE_UID, uidLast),
            SqlUtils.lessEqual(TBL_PLACES, COL_MESSAGE_UID, uidTo));

        try {
          cnt += syncMessages(qs.getData(query), msgs, account, localFolder, remoteFolder,
              progressId);
        } catch (ProgressInterruptedException e) {
          throw new ProgressInterruptedException(cnt + e.getProcessed());
        }
      }
      if (uidLast > 1) {
        cnt += mail.detachMessages(SqlUtils.and(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
            localFolder.getId()), SqlUtils.less(TBL_PLACES, COL_MESSAGE_UID, uidLast)));
      }
    }
    if (!Objects.equals(modseq, BeeUtils.unbox(localFolder.getModSeq()))) {
      mail.updateFolder(localFolder, localFolder.getUidValidity(), modseq);
    }
    return Pair.of(lastNo, cnt);
  }

  private int syncFolders(MailAccount account, Store mailStore) throws MessagingException {
    MailFolder localRoot = account.getRootFolder();
    Folder remoteRoot = account.getRemoteFolder(mailStore, localRoot);
    Multimap<String, Folder> remotes = HashMultimap.create();

    for (Folder remote : remoteRoot.list("*")) {
      if (remote instanceof IMAPFolder
          && ArrayUtils.contains(((IMAPFolder) remote).getAttributes(), "\\NoInferiors")
          && !remote.exists()) {
        continue;
      }
      remotes.put(remote.getParent().getFullName(), remote);
    }
    return syncNewFolders(account, remotes, remoteRoot.getName(), localRoot)
        + syncOldFolders(account, remotes, remoteRoot.getName(), localRoot);
  }

  private int syncMessages(SimpleRowSet data, Message[] messages, MailAccount account,
      MailFolder localFolder, Folder remoteFolder, String progressId)
      throws MessagingException, ProgressInterruptedException {
    int cnt = 0;
    SimpleRowSet rules = null;
    Set<Long> syncedMsgs = new HashSet<>();
    double l = messages.length;

    for (int i = messages.length - 1; i >= 0; i--) {
      if (!BeeUtils.isEmpty(progressId)) {
        if (!Endpoint.updateProgress(progressId, l / messages.length)) {
          throw new ProgressInterruptedException(cnt);
        }
        l--;
      }
      Message message = messages[i];
      long uid = ((UIDFolder) remoteFolder).getUID(message);
      SimpleRow row = data.getRowByKey(COL_MESSAGE_UID, BeeUtils.toString(uid));

      if (row != null) {
        Integer flags = MailEnvelope.getFlagMask(message);
        Long placeId = row.getLong(COL_PLACE);

        if (BeeUtils.unbox(row.getInt(COL_FLAGS)) != BeeUtils.unbox(flags)) {
          cnt += mail.setFlags(flags, sys.idEquals(TBL_PLACES, placeId));
        }
        syncedMsgs.add(placeId);
      } else {
        try {
          FetchProfile fp = new FetchProfile();
          fp.add(FetchProfile.Item.ENVELOPE);
          fp.add(MailConstants.COL_IN_REPLY_TO);
          remoteFolder.fetch(new Message[] {message}, fp);

          Long placeId = mail.storeMail(account, message, localFolder.getId(), uid).getB();

          if (DataUtils.isId(placeId)) {
            if (account.isInbox(localFolder)) {
              if (rules == null) {
                rules = getRules(account.getAccountId());
              }
              if (!rules.isEmpty()) {
                applyRules(message, placeId, account, localFolder, rules);
              }
            }
            cnt++;
          }
        } catch (MessagingException e) {
          logger.error(e);
        }
      }
    }
    List<Long> deletedMsgs = Arrays.stream(data.getLongColumn(COL_PLACE))
        .filter(id -> !syncedMsgs.contains(id))
        .collect(Collectors.toList());

    if (!deletedMsgs.isEmpty()) {
      cnt += mail.detachMessages(sys.idInList(TBL_PLACES, deletedMsgs));
    }
    return cnt;
  }

  private int syncNewFolders(MailAccount account, Multimap<String, Folder> remotes,
      String remoteParentName, MailFolder localParent) throws MessagingException {

    Holder<Integer> c = Holder.of(0);

    for (Folder remoteFolder : remotes.get(remoteParentName)) {
      MailFolder localFolder = localParent.getSubFolders().stream()
          .filter(local -> Objects.equals(local.getName(), remoteFolder.getName()))
          .findFirst().orElse(null);

      if (Objects.isNull(localFolder)) {
        localFolder = mail.createFolder(account, localParent, remoteFolder.getName());

        if (remoteFolder instanceof IMAPFolder) {
          String[] attributes = ((IMAPFolder) remoteFolder).getAttributes();

          for (SystemFolder sysFolder : EnumSet.complementOf(EnumSet.of(SystemFolder.Inbox))) {
            if (ArrayUtils.contains(attributes, "\\" + sysFolder.getFolderName())) {
              mail.updateAccount(account.getAccountId(), sysFolder.name() + COL_FOLDER,
                  localFolder.getId());
              account.setSystemFolder(sysFolder, localFolder.getId());
            }
          }
        }
        c.set(c.get() + 1);
      }
      c.set(c.get() + syncNewFolders(account, remotes, remoteFolder.getFullName(), localFolder));
    }
    return c.get();
  }

  private int syncOldFolders(MailAccount account, Multimap<String, Folder> remotes,
      String remoteParentName, MailFolder localParent) {

    Holder<Integer> c = Holder.of(0);

    localParent.getSubFolders().removeIf(localFolder -> {
      Folder remoteFolder = remotes.get(remoteParentName).stream()
          .filter(remote -> Objects.equals(remote.getName(), localFolder.getName()))
          .findFirst().orElse(null);

      if (Objects.isNull(remoteFolder)) {
        if (localFolder.isConnected() && !account.isSystemFolder(localFolder)) {
          mail.dropFolder(localFolder);
          c.set(c.get() + 1);
          return true;
        }
      } else {
        c.set(c.get() + syncOldFolders(account, remotes, remoteFolder.getFullName(), localFolder));
      }
      return false;
    });
    return c.get();
  }
}
