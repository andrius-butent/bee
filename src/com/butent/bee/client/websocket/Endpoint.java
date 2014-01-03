package com.butent.bee.client.websocket;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.Settings;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.websocket.WsUtils;
import com.butent.bee.shared.websocket.messages.Message;
import com.butent.bee.shared.websocket.messages.ProgressMessage;

import java.util.List;
import java.util.Map;

import elemental.js.events.JsEvent;
import elemental.events.MessageEvent;
import elemental.events.CloseEvent;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.client.Browser;
import elemental.html.WebSocket;

public final class Endpoint {

  private enum ReadyState {
    CONNECTING(0),
    OPEN(1),
    CLOSING(2),
    CLOSED(3);

    private static ReadyState getByValue(int value) {
      for (ReadyState state : values()) {
        if (state.value == value) {
          return state;
        }
      }
      return null;
    }

    private final int value;

    private ReadyState(int value) {
      this.value = value;
    }
  }

  private static final int PROGRESS_ACTIVATION_TIMEOUT = 2000;

  private static BeeLogger logger = LogUtils.getLogger(Endpoint.class);

  private static Boolean enabled;

  private static WebSocket socket;
  private static String sessionId;

  private static MessageDispatcher dispatcher = new MessageDispatcher();

  private static Map<String, Consumer<String>> progressQueue = Maps.newHashMap();

  public static void cancelPropgress(String progressId) {
    Assert.notEmpty(progressId);

    dequeuePropgress(progressId);
    send(ProgressMessage.cancel(progressId));
  }

  public static void close() {
    if (socket != null && !isClosed()) {
      socket.close();
    }
  }

  public static void dequeuePropgress(String progressId) {
    if (progressQueue.containsKey(progressId)) {
      progressQueue.remove(progressId);
    }
  }

  public static void enqueuePropgress(final String progressId, Consumer<String> consumer) {
    Assert.notEmpty(progressId);
    Assert.notNull(consumer);

    if (!isEnabled()) {
      consumer.accept(null);
      return;
    }

    progressQueue.put(progressId, consumer);
    send(ProgressMessage.open(progressId));

    Timer timer = new Timer() {
      @Override
      public void run() {
        if (!progressQueue.isEmpty()) {
          Consumer<String> starter = progressQueue.remove(progressId);
          if (starter != null) {
            starter.accept(null);
          }
        }
      }
    };

    timer.schedule(PROGRESS_ACTIVATION_TIMEOUT);
  }

  public static List<Property> getInfo() {
    List<Property> info = Lists.newArrayList();

    if (socket == null) {
      PropertyUtils.addProperties(info,
          "Enabled", (enabled == null) ? BeeConst.NULL : Boolean.toString(enabled),
          WebSocket.class.getSimpleName(), BeeConst.NULL);

    } else {
      PropertyUtils.addProperties(info,
          "Session Id", sessionId,
          "Binary Type", socket.getBinaryType(),
          "Buffered Amount", socket.getBufferedAmount(),
          "Extensions", socket.getExtensions(),
          "Protocol", socket.getProtocol(),
          "Ready State", getReadyState(),
          "Url", socket.getUrl());
    }

    info.add(new Property("Progress Queue", BeeUtils.bracket(progressQueue.size())));
    if (!progressQueue.isEmpty()) {
      int i = 0;
      for (String key : progressQueue.keySet()) {
        info.add(new Property(BeeUtils.joinWords("Progress", i++), key));
      }
    }

    return info;
  }

  public static String getSessionId() {
    return sessionId;
  }

  public static boolean isClosed() {
    return socket != null && socket.getReadyState() == ReadyState.CLOSED.value;
  }

  public static boolean isEnabled() {
    if (enabled == null) {
      enabled = Features.supportsWebSockets() && !BeeConst.isOff(Settings.getWebSocketUrl());
    }
    return enabled;
  }

  public static boolean isOpen() {
    return socket != null && socket.getReadyState() == ReadyState.OPEN.value;
  }

  public static void open(Long userId) {
    if (isEnabled() && (socket == null || isClosed()) && userId != null) {

      try {
        socket = Browser.getWindow().newWebSocket(url(userId));
      } catch (JavaScriptException ex) {
        socket = null;
        logger.severe(ex, "cannot open websocket");
      }

      if (socket != null) {
        socket.setOnopen(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            onOpen();
          }
        });

        socket.setOnclose(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            onClose((CloseEvent) evt);
          }
        });

        socket.setOnerror(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            onError((JsEvent) evt);
          }
        });

        socket.setOnmessage(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            onMessage((MessageEvent) evt);
          }
        });
      }
    }
  }

  public static void send(Message message) {
    if (!isOpen()) {
      if (isEnabled()) {
        logger.warning("ws is not open");
      }

    } else if (message == null) {
      WsUtils.onEmptyMessage(message);

    } else {
      String data = message.encode();
      socket.send(data);

      logger.info("->", message.getType().name().toLowerCase(), data.length());
    }
  }

  public static void setSessionId(String sessionId) {
    Endpoint.sessionId = sessionId;
  }

  static boolean startPropgress(String progressId) {
    if (!progressQueue.isEmpty()) {
      Consumer<String> starter = progressQueue.remove(progressId);
      if (starter != null) {
        starter.accept(progressId);
        return true;
      }
    }

    return false;
  }

  private static String getReadyState() {
    if (socket == null) {
      return null;

    } else {
      int value = socket.getReadyState();
      ReadyState readyState = ReadyState.getByValue(value);

      return (readyState == null) ? Integer.toString(value) : readyState.name().toLowerCase();
    }
  }

  private static void onClose(CloseEvent event) {
    if (socket != null) {
      setSessionId(null);

      String eventInfo = (event == null) ? null
          : BeeUtils.joinOptions("code", Integer.toString(event.getCode()),
              "reason", event.getReason());
      logger.info("close", socket.getUrl(), getReadyState(), eventInfo);
    }
  }

  private static void onError(JsEvent event) {
    logger.severe("ws error event", JsUtils.toString(event));
  }

  private static void onMessage(MessageEvent event) {
    Object data = event.getData();

    if (data instanceof String) {
      Message message = Message.decode((String) data);
      if (message != null) {
        logger.info("<-", message.getType().name().toLowerCase(), ((String) data).length());
        dispatcher.dispatch(message);
      }

    } else if (data == null) {
      logger.warning("ws received data is null");
    } else {
      logger.warning("ws unknown received data type", NameUtils.getName(data));
    }
  }

  private static void onOpen() {
    if (socket != null) {
      logger.info(socket.getUrl(), getReadyState());
    }
  }

  private static String url(long userId) {
    String url = Settings.getWebSocketUrl();
    if (BeeUtils.isEmpty(url)) {
      String href = GWT.getHostPageBaseURL();
      url = "ws" + Paths.ensureEndSeparator(href.substring(4)) + "ws/";
    }
    return Paths.ensureEndSeparator(url) + Long.toString(userId);
  }

  private Endpoint() {
  }
}
