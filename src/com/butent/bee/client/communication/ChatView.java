package com.butent.bee.client.communication;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.communication.ChatConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.Chat;
import com.butent.bee.shared.communication.ChatItem;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.ChatMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class ChatView extends Flow implements Presenter, View, Printable,
    VisibilityChangeEvent.Handler, HasWidgetSupplier {

  private static final class MessageWidget extends Flow {

    private final long millis;
    private final Label timeLabel;

    private MessageWidget(ChatItem message, boolean addPhoto) {
      super(STYLE_MESSAGE_WRAPPER);

      this.millis = message.getTime();

      this.timeLabel = new Label();
      timeLabel.addStyleName(STYLE_MESSAGE_TIME);
      ChatUtils.updateTime(timeLabel, millis);

      add(timeLabel);

      if (addPhoto) {
        Image photo = Global.getUsers().getPhoto(message.getUserId());

        if (photo == null) {
          CustomDiv signature = new CustomDiv(STYLE_MESSAGE_SIGNATURE);
          signature.setText(Global.getUsers().getSignature(message.getUserId()));
          add(signature);

        } else {
          photo.addStyleName(STYLE_MESSAGE_PHOTO);
          add(photo);
        }
      }

      Label body = new Label(message.getText());
      body.addStyleName(STYLE_MESSAGE_BODY);
      add(body);
    }

    private boolean refresh() {
      if (ChatUtils.needsRefresh(millis)) {
        timeLabel.setText(ChatUtils.elapsed(millis));
        return true;
      } else {
        return false;
      }
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(ChatView.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Chat-";
  private static final String STYLE_MESSAGE_WRAPPER = STYLE_PREFIX + "message";

  private static final String STYLE_MESSAGE_PREFIX = STYLE_PREFIX + "message-";
  private static final String STYLE_MESSAGE_INCOMING = STYLE_MESSAGE_PREFIX + "incoming";
  private static final String STYLE_MESSAGE_OUTGOING = STYLE_MESSAGE_PREFIX + "outgoing";

  private static final String STYLE_MESSAGE_PHOTO = STYLE_MESSAGE_PREFIX + "photo";
  private static final String STYLE_MESSAGE_SIGNATURE = STYLE_MESSAGE_PREFIX + "signature";
  private static final String STYLE_MESSAGE_BODY = STYLE_MESSAGE_PREFIX + "body";
  private static final String STYLE_MESSAGE_TIME = STYLE_MESSAGE_PREFIX + "time";

  private static final String STYLE_AUTO_SCROLL_PREFIX = STYLE_PREFIX + "autoScroll-";
  private static final String STYLE_AUTO_SCROLL_ON = STYLE_AUTO_SCROLL_PREFIX + "on";
  private static final String STYLE_AUTO_SCROLL_OFF = STYLE_AUTO_SCROLL_PREFIX + "off";

  private static final String AUTO_SCROLL_LABEL = "Auto Scroll";
  private static final String AUTO_SCROLL_ON = "On";
  private static final String AUTO_SCROLL_OFF = "Off";

  private static final int TIMER_PERIOD = 5000;

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.VIEW);

  private final long chatId;
  private final List<Long> otherUsers;

  private final HeaderView headerView;
  private final Flow messagePanel;
  private final InputArea inputArea;
  private final Flow onlinePanel;

  private final Timer timer;

  private boolean enabled = true;
  private boolean autoScroll = true;

  private final List<HandlerRegistration> registry = new ArrayList<>();

  public ChatView(Chat chat) {
    super(STYLE_PREFIX + "view");
    addStyleName(UiOption.getStyleName(uiOptions));

    this.chatId = chat.getId();
    this.otherUsers = ChatUtils.getOtherUsers(chat.getUsers());

    String caption;
    if (!BeeUtils.isEmpty(chat.getName())) {
      caption = chat.getName();
    } else if (otherUsers.size() == 1) {
      caption = Global.getUsers().getSignature(otherUsers.get(0));
    } else {
      caption = ChatUtils.getFirstNames(otherUsers);
    }

    this.headerView = new HeaderImpl();
    headerView.create(caption, false, true, null, uiOptions,
        EnumSet.of(Action.CLOSE), Action.NO_ACTIONS, Action.NO_ACTIONS);
    headerView.setViewPresenter(this);

    // EnumSet.of(Action.PRINT, Action.CONFIGURE, Action.CLOSE)
    // headerView.addCommandItem(createAutoScrollToggle(autoScroll));
    add(headerView);

    this.messagePanel = new Flow(STYLE_PREFIX + "messages");
    StyleUtils.setTop(messagePanel, headerView.getHeight());
    add(messagePanel);

    this.inputArea = new InputArea();
    inputArea.addStyleName(STYLE_PREFIX + "input");
    inputArea.setMaxLength(TextMessage.MAX_LENGTH);

    inputArea.addKeyDownHandler(event -> {
      if (UiHelper.isSave(event.getNativeEvent()) && compose()) {
        event.preventDefault();
        event.stopPropagation();

        inputArea.clearValue();
      }
    });

    FaLabel submit = new FaLabel(FontAwesome.REPLY_ALL);
    submit.addStyleName(STYLE_PREFIX + "submit");

    submit.addClickHandler(event -> {
      if (compose()) {
        inputArea.clearValue();
      }
    });

    this.onlinePanel = new Flow(STYLE_PREFIX + "online");

    Flow footer = new Flow(STYLE_PREFIX + "footer");
    footer.add(inputArea);
    footer.add(submit);
    footer.add(onlinePanel);

    add(footer);

    if (!chat.getMessages().isEmpty()) {
      for (ChatItem message : chat.getMessages()) {
        addMessage(message, false);
      }
      updateHeader(chat.getMaxTime());
    }

    updateOnlinePanel(otherUsers);

    this.timer = new Timer() {
      @Override
      public void run() {
        onTimer();
      }
    };
    timer.scheduleRepeating(TIMER_PERIOD);
  }

  public void addMessage(ChatItem message, boolean update) {
    if (message != null && message.isValid()) {
      boolean incoming = !BeeKeeper.getUser().is(message.getUserId());
      boolean addPhoto = incoming && otherUsers.size() > 1;

      MessageWidget messageWidget = new MessageWidget(message, addPhoto);
      messageWidget.addStyleName(incoming ? STYLE_MESSAGE_INCOMING : STYLE_MESSAGE_OUTGOING);

      messagePanel.add(messageWidget);

      if (update) {
        updateHeader(message.getTime());

        if (incoming) {
          maybeScroll(true);
        } else {
          DomUtils.scrollToBottom(messagePanel);
        }
      }
    }
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addReadyHandler(
      ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public String getCaption() {
    return headerView.getCaption();
  }

  public long getChatId() {
    return chatId;
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return headerView;
  }

  @Override
  public String getIdPrefix() {
    return "chat";
  }

  @Override
  public View getMainView() {
    return this;
  }

  @Override
  public Element getPrintElement() {
    return messagePanel.getElement();
  }

  @Override
  public String getSupplierKey() {
    return ViewFactory.SupplierKind.CHAT.getKey(BeeUtils.toString(chatId));
  }

  @Override
  public String getViewKey() {
    return getSupplierKey();
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case CONFIGURE:
        Global.getChatManager().configure(getChatId());
        break;

      case PRINT:
        Printer.print(this);
        break;

      case CANCEL:
      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public void onChatUpdate(Chat chat) {
    Assert.notNull(chat);

    headerView.setCaption(chat.getName());
    updateOnlinePanel(chat.getUsers());

    BeeKeeper.getScreen().onWidgetChange(this);
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (messagePanel.getId().equals(source.getId())) {
      ElementSize.copyScroll(source, target);
      target.setClassName(BeeConst.STRING_EMPTY);
    }
    return true;
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      maybeScroll(false);
    }
  }

  @Override
  public boolean reactsTo(Action action) {
    return EnumUtils.in(action, Action.CONFIGURE, Action.PRINT, Action.CANCEL, Action.CLOSE);
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    EventUtils.clearRegistry(registry);
    registry.add(VisibilityChangeEvent.register(this));

    maybeScroll(false);

    ReadyEvent.fire(this);
  }

  @Override
  protected void onUnload() {
    timer.cancel();
    EventUtils.clearRegistry(registry);

    super.onUnload();

    Global.getChatManager().leaveChat(getChatId());
  }

  private boolean autoScroll() {
    return autoScroll;
  }

  private boolean compose() {
    String text = BeeUtils.trim(inputArea.getValue());
    if (BeeUtils.isEmpty(text)) {
      return false;
    }

    if (!Endpoint.isOpen()) {
      logger.warning("cannot send message");
      return false;
    }

    ChatItem item = new ChatItem(BeeKeeper.getUser().getUserId(), text);
    ChatMessage chatMessage = new ChatMessage(chatId, item);

    Global.getChatManager().addMessage(chatMessage);

    ParameterList params = BeeKeeper.getRpc().createParameters(Service.SEND_CHAT_MESSAGE);
    params.addDataItem(COL_CHAT_MESSAGE, chatMessage.encode());

    String from = Endpoint.getSessionId();
    if (!BeeUtils.isEmpty(from)) {
      params.addDataItem(Service.VAR_FROM, from);
    }

    BeeKeeper.getRpc().makeRequest(params);

    return true;
  }

  private IdentifiableWidget createAutoScrollToggle(boolean on) {
    Flow container = new Flow(STYLE_AUTO_SCROLL_PREFIX + "container");

    InlineLabel label = new InlineLabel(AUTO_SCROLL_LABEL);
    label.addStyleName(STYLE_AUTO_SCROLL_PREFIX + "label");
    container.add(label);

    final Button toggle = new Button(on ? AUTO_SCROLL_ON : AUTO_SCROLL_OFF);
    toggle.addStyleName(STYLE_AUTO_SCROLL_PREFIX + "toggle");
    toggle.addStyleName(on ? STYLE_AUTO_SCROLL_ON : AUTO_SCROLL_OFF);

    toggle.addClickHandler(event -> {
      setAutoScroll(!autoScroll());

      toggle.setHtml(autoScroll() ? AUTO_SCROLL_ON : AUTO_SCROLL_OFF);
      toggle.setStyleName(STYLE_AUTO_SCROLL_ON, autoScroll());
      toggle.setStyleName(STYLE_AUTO_SCROLL_OFF, !autoScroll());

      maybeScroll(false);
    });

    container.add(toggle);

    return container;
  }

  private void maybeScroll(boolean checkVisibility) {
    if (autoScroll() && messagePanel.getWidgetCount() > 1
        && (!checkVisibility || DomUtils.isVisible(this))) {
      DomUtils.scrollToBottom(messagePanel);
    }
  }

  private void onTimer() {
    int count = messagePanel.getWidgetCount();

    if (count > 0) {
      for (int i = count - 1; i >= 0; i--) {
        Widget widget = messagePanel.getWidget(i);

        if (widget instanceof MessageWidget) {
          boolean updated = ((MessageWidget) widget).refresh();
          if (!updated) {
            break;
          }
        }
      }

      Long maxTime = Global.getChatManager().getMaxTime(chatId);
      if (maxTime != null) {
        updateHeader(maxTime);
      }
    }
  }

  private void setAutoScroll(boolean autoScroll) {
    this.autoScroll = autoScroll;
  }

  private void updateHeader(long maxTime) {
    // List<String> list = new ArrayList<>();
    //
    // if (!messagePanel.isEmpty()) {
    // list.add(BeeUtils.bracket(messagePanel.getWidgetCount()));
    // }
    // if (maxTime > 0) {
    // list.add(ChatUtils.elapsed(maxTime));
    // }
    //
    // headerView.setMessage(BeeUtils.join(BeeConst.STRING_SPACE, list));
    // if (maxTime > 0) {
    // headerView.setMessageTitle(TimeUtils.renderDateTime(maxTime));
    // }
  }

  private void updateOnlinePanel(Collection<Long> users) {
    if (!onlinePanel.isEmpty()) {
      onlinePanel.clear();
    }

    for (Long userId : users) {
      UserData userData = Global.getUsers().getUserData(userId);
      if (userData != null) {
        CustomDiv label = new CustomDiv(STYLE_PREFIX + "userLabel");
        label.setText(userData.getFirstName());
        label.setTitle(userData.getUserSign());

        onlinePanel.add(label);
      }

      Presence presence = Global.getUsers().getUserPresence(userId);
      if (presence != null) {
        FaLabel icon = new FaLabel(presence.getIcon(), presence.getStyleName());
        icon.addStyleName(STYLE_PREFIX + "userPresence");
        icon.setTitle(presence.getCaption());

        onlinePanel.add(icon);
      }
    }
  }
}