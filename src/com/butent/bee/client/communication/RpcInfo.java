package com.butent.bee.client.communication;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.Response;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.utils.BeeDuration;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contains all relevant RPC related information, both request and response, and methods for
 * operating with that information.
 */

public class RpcInfo {
  private static int COUNTER = 0;

  public static int MAX_DATA_LEN = 1024;

  public static final String COL_ID = "Id";
  public static final String COL_SERVICE = "Service";
  public static final String COL_STAGE = "Stage";
  public static final String COL_METHOD = "Method";

  public static final String COL_STATE = "State";

  public static final String COL_START = "Start";
  public static final String COL_END = "End";
  public static final String COL_COMPLETED = "Completed";
  public static final String COL_TIMEOUT = "Timeout";
  public static final String COL_EXPIRES = "Expires";

  public static final String COL_REQ_PARAMS = "Request Params";
  public static final String COL_REQ_TYPE = "Request Type";
  public static final String COL_REQ_DATA = "Request Data";
  public static final String COL_REQ_ROWS = "Request Rows";
  public static final String COL_REQ_COLS = "Request Columns";
  public static final String COL_REQ_SIZE = "Request Size";

  public static final String COL_RESP_TYPE = "Response Type";
  public static final String COL_RESP_DATA = "Response Data";
  public static final String COL_RESP_ROWS = "Response Rows";
  public static final String COL_RESP_COLS = "Response Columns";
  public static final String COL_RESP_SIZE = "Response Size";

  public static final String COL_RESP_MSG_CNT = "Response Msg Cnt";
  public static final String COL_RESP_MESSAGES = "Response Messages";

  public static final String COL_RESP_PART_CNT = "Response Part Cnt";
  public static final String COL_RESP_PART_SIZES = "Response Part Sizes";

  public static final String COL_RESP_INFO = "Response Info";

  public static final String COL_ERR_MSG = "Error Msg";

  public static final String COL_USR_DATA = "User Data";

  private int id;
  private String service = null;
  private String stage = null;
  private RequestBuilder.Method method = RequestBuilder.GET;

  private final Set<State> states = EnumSet.noneOf(State.class);
  private final BeeDuration duration;

  private RequestBuilder reqBuilder = null;
  private Request request = null;
  private ParameterList reqParams = null;

  private String reqData = null;
  private ContentType reqType = null;
  private int reqRows = BeeConst.SIZE_UNKNOWN;
  private int reqCols = BeeConst.SIZE_UNKNOWN;
  private int reqSize = BeeConst.SIZE_UNKNOWN;

  private Response response = null;
  private Collection<ExtendedProperty> respInfo = null;

  private ContentType respType = null;
  private String respData = null;
  private int respRows = BeeConst.SIZE_UNKNOWN;
  private int respCols = BeeConst.SIZE_UNKNOWN;
  private int respSize = BeeConst.SIZE_UNKNOWN;

  private int respMsgCnt = BeeConst.SIZE_UNKNOWN;
  private Collection<ResponseMessage> respMessages = null;
  private int respPartCnt = BeeConst.SIZE_UNKNOWN;
  private int[] respPartSize = null;

  private String errMsg = null;

  private Map<String, String> userData = null;
  private ResponseCallback respCallback;

  public RpcInfo(RequestBuilder.Method method, String service,
      ParameterList params, ContentType ctp, String data, ResponseCallback callback) {
    this.id = ++COUNTER;
    this.duration = new BeeDuration();

    this.method = method;
    this.service = service;
    this.reqParams = params;
    this.reqType = ctp;
    this.reqData = data;
    this.respCallback = callback;
  }
 
  public void addState(State state) {
    if (state != null) {
      getStates().add(state);
    }
  }

  public void addUserData(Object... obj) {
    Assert.notNull(obj);
    Assert.parameterCount(obj.length, 2);
    Assert.isEven(obj.length);

    if (userData == null) {
      userData = new HashMap<String, String>();
    }

    for (int i = 0; i < obj.length; i += 2) {
      if (!(obj[i] instanceof String)) {
        BeeKeeper.getLog().warning("parameter", i, "not a string");
        continue;
      }
      userData.put((String) obj[i], BeeUtils.transformNoTrim(obj[i + 1]));
    }
  }
  
  public boolean cancel() {
    boolean wasPending = false;

    if (getRequest() != null) {
      wasPending = getRequest().isPending();
      getRequest().cancel();
    }
    addState(State.CANCELED);
    
    return wasPending;
  }

  public int end(ContentType ctp, String data, int size, int rows,
      int cols, int msgCnt, Collection<ResponseMessage> messages, int partCnt, int[] partSizes) {
    int r = done();
    setState(State.CLOSED);

    setRespType(ctp);
    setRespData(data);

    if (size != BeeConst.SIZE_UNKNOWN) {
      setRespSize(size);
    }
    if (rows != BeeConst.SIZE_UNKNOWN) {
      setRespRows(rows);
    }
    if (cols != BeeConst.SIZE_UNKNOWN) {
      setRespCols(cols);
    }

    if (msgCnt != BeeConst.SIZE_UNKNOWN) {
      setRespMsgCnt(msgCnt);
    }
    if (messages != null) {
      setRespMessages(messages);
    }

    if (partCnt != BeeConst.SIZE_UNKNOWN) {
      setRespPartCnt(partCnt);
    }
    if (partSizes != null) {
      setRespPartSize(partSizes);
    }
    return r;
  }

  public void endError(Exception ex) {
    endError(ex.toString());
  }

  public void endError(String msg) {
    done();
    setState(State.ERROR);
    setErrMsg(msg);
  }

  public boolean filterStates(Collection<State> filter) {
    if (filter == null) {
      return true;
    }
    if (getStates().isEmpty()) {
      return false;
    }
    return BeeUtils.containsAny(getStates(), filter);
  }

  public String getCompletedTime() {
    return duration.getCompletedTime();
  }

  public String getEndTime() {
    return duration.getEndTime();
  }

  public String getErrMsg() {
    return errMsg;
  }

  public String getExpireTime() {
    return duration.getExpireTime();
  }

  public int getId() {
    return id;
  }

  public RequestBuilder.Method getMethod() {
    return method;
  }

  public String getMethodString() {
    if (getMethod() == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return getMethod().toString();
    }
  }

  public String getParameter(String name) {
    Assert.notEmpty(name);

    if (getReqParams() == null) {
      return null;
    } else {
      return getReqParams().getParameter(name);
    }
  }

  public RequestBuilder getReqBuilder() {
    return reqBuilder;
  }

  public int getReqCols() {
    return reqCols;
  }

  public String getReqData() {
    return reqData;
  }

  public ParameterList getReqParams() {
    return reqParams;
  }

  public int getReqRows() {
    return reqRows;
  }

  public int getReqSize() {
    return reqSize;
  }

  public ContentType getReqType() {
    return reqType;
  }

  public Request getRequest() {
    return request;
  }

  public ResponseCallback getRespCallback() {
    return respCallback;
  }

  public int getRespCols() {
    return respCols;
  }

  public String getRespData() {
    return respData;
  }

  public Collection<ExtendedProperty> getRespInfo() {
    return respInfo;
  }

  public String getRespInfoString() {
    return BeeUtils.transformCollection(getRespInfo(), BeeConst.DEFAULT_ROW_SEPARATOR);
  }

  public Collection<ResponseMessage> getRespMessages() {
    return respMessages;
  }

  public int getRespMsgCnt() {
    return respMsgCnt;
  }

  public Response getResponse() {
    return response;
  }

  public int getRespPartCnt() {
    return respPartCnt;
  }

  public int[] getRespPartSize() {
    return respPartSize;
  }

  public int getRespRows() {
    return respRows;
  }

  public int getRespSize() {
    return respSize;
  }

  public ContentType getRespType() {
    return respType;
  }

  public String getService() {
    return service;
  }

  public String getSizeString(int z) {
    if (z != BeeConst.SIZE_UNKNOWN) {
      return BeeUtils.toString(z);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public String getStage() {
    return stage;
  }

  public String getStartTime() {
    return duration.getStartTime();
  }

  public Set<State> getStates() {
    return states;
  }

  public String getStateString() {
    if (getStates().isEmpty()) {
      return BeeConst.STRING_EMPTY;
    }

    StringBuilder sb = new StringBuilder();
    for (State state : getStates()) {
      if (sb.length() > 0) {
        sb.append(BeeConst.CHAR_SPACE);
      }
      sb.append(state.name().toLowerCase());
    }
    return sb.toString();
  }

  public int getTimeout() {
    return duration.getTimeout();
  }

  public String getTimeoutString() {
    return duration.getTimeoutAsTime();
  }

  public Map<String, String> getUserData() {
    return userData;
  }
  
  public boolean isCanceled() {
    return getStates().contains(State.CANCELED);
  }

  public void setErrMsg(String errMsg) {
    this.errMsg = errMsg;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setMethod(RequestBuilder.Method method) {
    this.method = method;
  }

  public void setReqBuilder(RequestBuilder reqBuilder) {
    this.reqBuilder = reqBuilder;
  }

  public void setReqCols(int reqCols) {
    this.reqCols = reqCols;
  }

  public void setReqData(String reqData) {
    this.reqData = reqData;
  }

  public void setReqParams(ParameterList reqParams) {
    this.reqParams = reqParams;
  }

  public void setReqRows(int reqRows) {
    this.reqRows = reqRows;
  }

  public void setReqSize(int reqSize) {
    this.reqSize = reqSize;
  }

  public void setReqType(ContentType reqType) {
    this.reqType = reqType;
  }

  public void setRequest(Request request) {
    this.request = request;
  }

  public void setRespCallback(ResponseCallback respCallback) {
    this.respCallback = respCallback;
  }

  public void setRespCols(int respCols) {
    this.respCols = respCols;
  }

  public void setRespData(String respData) {
    this.respData = BeeUtils.clip(respData, MAX_DATA_LEN);
  }

  public void setRespInfo(Collection<ExtendedProperty> respInfo) {
    this.respInfo = respInfo;
  }

  public void setRespMessages(Collection<ResponseMessage> respMessages) {
    this.respMessages = respMessages;
  }

  public void setRespMsgCnt(int respMsgCnt) {
    this.respMsgCnt = respMsgCnt;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  public void setRespPartCnt(int respPartCnt) {
    this.respPartCnt = respPartCnt;
  }

  public void setRespPartSize(int[] respPartSize) {
    this.respPartSize = respPartSize;
  }

  public void setRespRows(int respRows) {
    this.respRows = respRows;
  }

  public void setRespSize(int respSize) {
    this.respSize = respSize;
  }

  public void setRespType(ContentType respType) {
    this.respType = respType;
  }

  public void setService(String service) {
    this.service = service;
  }

  public void setStage(String stage) {
    this.stage = stage;
  }

  public void setState(State state) {
    Assert.notNull(state);
    getStates().clear();
    getStates().add(state);
  }

  public void setTimeout(int timeout) {
    duration.setTimeout(timeout);
  }

  public void setUserData(Map<String, String> userData) {
    this.userData = userData;
  }

  private int done() {
    return duration.finish();
  }
}
