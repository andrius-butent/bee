package com.butent.bee.client.communication;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.ResponseData;
import com.butent.bee.client.utils.BeeDuration;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.DateTime;
import com.butent.bee.shared.BeeResource;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.LogUtils;

import java.util.logging.Level;

public class BeeCallback implements RequestCallback {

  @Override
  public void onError(Request req, Throwable ex) {
    String msg = (ex instanceof RequestTimeoutException) ? "request timeout" : "request failure";
    BeeKeeper.getLog().severe(msg, ex);
  }

  @Override
  public void onResponseReceived(Request req, Response resp) {
    BeeDuration dur = new BeeDuration("response");

    int statusCode = resp.getStatusCode();
    boolean debug = Global.isDebug();

    int id = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_QID));
    RpcInfo info = BeeKeeper.getRpc().getRpcInfo(id);
    String svc = (info == null) ? BeeConst.STRING_EMPTY : info.getService();

    String msg;

    if (info == null) {
      BeeKeeper.getLog().warning("Rpc info not available");
    }
    if (BeeUtils.isEmpty(svc)) {
      BeeKeeper.getLog().warning("Rpc service",
          BeeUtils.bracket(Service.RPC_VAR_SVC), "not available");
    }

    if (statusCode != Response.SC_OK) {
      msg = BeeUtils.concat(1, BeeUtils.addName(Service.RPC_VAR_QID, id),
          BeeUtils.addName(Service.RPC_VAR_SVC, svc));
      if (!BeeUtils.isEmpty(msg)) {
        BeeKeeper.getLog().severe(msg);
      }

      msg = BeeUtils.concat(1, BeeUtils.bracket(statusCode), resp.getStatusText());
      BeeKeeper.getLog().severe("response status", msg);

      if (info != null) {
        info.endError(msg);
      }
      finalizeResponse();
      return;
    } else {
      String sid = resp.getHeader(Service.RPC_VAR_SID);
      String usr = resp.getHeader(Service.VAR_USER_SIGN);
      BeeKeeper.getUser().setSessionId(sid);

      if (BeeUtils.isEmpty(sid) || !BeeUtils.isEmpty(usr)) {
        if (!BeeUtils.isEmpty(usr)) {
          usr = Codec.decodeBase64(usr);
        }
        BeeKeeper.getUser().setUserSign(usr);
        BeeKeeper.getUi().updateSignature();
      }
    }

    ContentType ctp = CommUtils.getContentType(resp.getHeader(Service.RPC_VAR_CTP));

    String txt = CommUtils.getContent(ctp, resp.getText());
    int len = txt.length();

    int cnt = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_CNT));
    int cc = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_COLS));
    int mc = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_MSG_CNT));
    int pc = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_PART_CNT));

    if (debug) {
      BeeKeeper.getLog().finish(dur, BeeUtils.addName(Service.RPC_VAR_QID, id),
          BeeUtils.addName(Service.RPC_VAR_SVC, svc));

      BeeKeeper.getLog().info(BeeUtils.addName(Service.RPC_VAR_CTP, ctp),
          BeeUtils.addName("len", len), BeeUtils.addName(Service.RPC_VAR_CNT, cnt));
      BeeKeeper.getLog().info(BeeUtils.addName(Service.RPC_VAR_COLS, cc),
          BeeUtils.addName(Service.RPC_VAR_MSG_CNT, mc),
          BeeUtils.addName(Service.RPC_VAR_PART_CNT, pc));
    } else {
      BeeKeeper.getLog().info("response", id, svc, ctp, cnt, cc, mc, pc, len);
    }

    String hSep = resp.getHeader(Service.RPC_VAR_SEP);
    String sep;

    if (BeeUtils.isHexString(hSep)) {
      sep = new String(BeeUtils.fromHex(hSep));
      BeeKeeper.getLog().warning("response separator", BeeUtils.bracket(hSep));
    } else {
      sep = Character.toString(CommUtils.DEFAULT_INFORMATION_SEPARATOR);
      if (!BeeUtils.isEmpty(hSep)) {
        BeeKeeper.getLog().severe("wrong response separator", BeeUtils.bracket(hSep));
      }
    }

    if (debug) {
      Header[] headers = resp.getHeaders();
      for (int i = 0; i < headers.length; i++) {
        if (!BeeUtils.isEmpty(headers[i])) {
          BeeKeeper.getLog().info("Header", i + 1, headers[i].getName(), headers[i].getValue());
        }
      }
      if (info != null) {
        info.setRespInfo(RpcUtils.responseInfo(resp));
      }
    }

    ResponseMessage[] messages = null;
    if (mc > 0) {
      messages = new ResponseMessage[mc];
      for (int i = 0; i < mc; i++) {
        messages[i] = new ResponseMessage(resp.getHeader(CommUtils.rpcMessageName(i)), true);
      }
      dispatchMessages(mc, messages);
    }

    int[] partSizes = null;
    if (pc > 0) {
      partSizes = new int[pc];
      for (int i = 0; i < pc; i++) {
        partSizes[i] = BeeUtils.toInt(resp.getHeader(CommUtils.rpcPartName(i)));
      }
    }

    if (info != null) {
      info.end(ctp, txt, len, cnt, cc, mc, messages, pc, partSizes);
    }

    if (len == 0) {
      if (mc == 0) {
        BeeKeeper.getLog().warning("response empty");
      }

    } else if (Service.isInvocation(svc)) {
      dispatchInvocation(svc, info, txt, mc, messages, cc, cnt, sep);

    } else if (pc > 0) {
      dispatchParts(svc, pc, partSizes, txt);

    } else if (CommUtils.isResource(ctp)) {
      dispatchResource(txt);

    } else if (txt.indexOf(sep) < 0) {
      BeeKeeper.getLog().info("text", txt);

    } else {

      JsArrayString arr = splitResponse(txt, sep, cnt);

      ResponseCallback callback = null;
      if (info != null) {
        callback = info.getRespCallback();
      }

      if (callback != null) {
        callback.onResponse(arr);
      } else {
        dispatchResponse(svc, cc, arr);
      }
    }

    BeeKeeper.getLog().finish(dur);
    finalizeResponse();
  }

  private void dispatchInvocation(String svc, RpcInfo info, String txt, int mc,
      ResponseMessage[] messages, int cc, int cnt, String sep) {
    if (info == null) {
      BeeKeeper.getLog().severe("rpc info not available");
      return;
    }

    String method = info.getParameter(Service.RPC_VAR_METH);
    if (BeeUtils.isEmpty(method)) {
      BeeKeeper.getLog().severe("rpc parameter [method] not found");
      return;
    }

    if (BeeUtils.same(method, "stringInfo")) {
      ResponseHandler.unicodeTest(info, txt, mc, messages);
    } else if (cnt > 0) {
      JsArrayString arr = splitResponse(txt, sep, cnt);
      dispatchResponse(svc, cc, arr);
    } else if (mc <= 0) {
      BeeKeeper.getLog().warning("unknown invocation method", method);
    }
  }

  private void dispatchMessages(int mc, ResponseMessage[] messages) {
    for (int i = 0; i < mc; i++) {
      Level level = messages[i].getLevel();
      if (LogUtils.isOff(level)) {
        continue;
      }

      DateTime date = messages[i].getDate();
      String msg;
      if (date == null) {
        msg = messages[i].getMessage();
      } else {
        msg = BeeUtils.concat(1, date.toTimeString(), messages[i].getMessage());
      }

      if (level == null) {
        BeeKeeper.getLog().info(msg);
      } else {
        BeeKeeper.getLog().log(level, msg);
      }
    }
  }

  private void dispatchParts(String svc, int pc, int[] sizes, String content) {
    if (BeeUtils.same(svc, Service.GET_XML_INFO)) {
      ResponseHandler.showXmlInfo(pc, sizes, content);
    } else {
      BeeKeeper.getLog().warning("unknown multipart response", svc);
    }
  }

  private void dispatchResource(String src) {
    BeeKeeper.getUi().showResource(new BeeResource(src));
  }

  private void dispatchResponse(String svc, int cc, JsArrayString arr) {
    if (BeeUtils.same(svc, Service.LOAD_MENU)) {
      BeeKeeper.getMenu().loadCallBack(arr);

    } else if (cc > 0) {
      BeeColumn[] columns = new BeeColumn[cc];
      for (int i = 0; i < cc; i++) {
        columns[i] = BeeColumn.restore(arr.get(i));
      }

      ResponseData table = new ResponseData(arr, columns);
      BeeKeeper.getUi().showGrid(table);

    } else {
      for (int i = 0; i < arr.length(); i++) {
        if (!BeeUtils.isEmpty(arr.get(i))) {
          BeeKeeper.getLog().info(arr.get(i));
        }
      }

      if (BeeUtils.same(svc, Service.WHERE_AM_I)) {
        BeeKeeper.getLog().info(BeeConst.whereAmI());
      }
    }
  }

  private void finalizeResponse() {
    BeeKeeper.getLog().addSeparator();
  }

  private JsArrayString splitResponse(String txt, String sep, int cnt) {
    JsArrayString arr = JsUtils.split(txt, sep);
    if (cnt > 0 && arr.length() > cnt) {
      arr.setLength(cnt);
    }
    return arr;
  }
}
