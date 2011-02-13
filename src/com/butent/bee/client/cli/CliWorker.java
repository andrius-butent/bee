package com.butent.bee.client.cli;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.media.client.Audio;
import com.google.gwt.media.client.Video;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.ajaxloader.AjaxKeyRepository;
import com.butent.bee.client.ajaxloader.AjaxLoader;
import com.butent.bee.client.ajaxloader.ClientLocation;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.RpcList;
import com.butent.bee.client.composite.SliderBar;
import com.butent.bee.client.data.JsData;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.FlexTable;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.TilePanel;
import com.butent.bee.client.tree.BeeTree;
import com.butent.bee.client.utils.Browser;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.Meter;
import com.butent.bee.client.widget.Progress;
import com.butent.bee.client.widget.Svg;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeDate;
import com.butent.bee.shared.BeeService;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CliWorker {

  public static void clearLog() {
    BeeKeeper.getLog().clear();
  }

  public static void digest(String v) {
    String src = null;
    int p = v.indexOf(BeeConst.CHAR_SPACE);

    if (p > 0) {
      String z = v.substring(p + 1).trim();

      if (BeeUtils.isDigit(z)) {
        int x = BeeUtils.toInt(z);
        if (BeeUtils.betweenInclusive(x, 1, BeeUtils.exp10(6))) {
          src = BeeUtils.randomString(x, x, BeeConst.CHAR_SPACE, '\u0800');
        } else {
          src = z;
        }
      } else if (z.length() > 0) {
        src = z;
      }
    }

    if (src == null) {
      src = BeeUtils.randomString(10, 20, BeeConst.CHAR_SPACE, '\u0400');
    }

    if (src.length() > 100) {
      BeeKeeper.getLog().info("Source length", src.length());
    } else {
      BeeKeeper.getLog().info(Codec.escapeUnicode(src));
    }

    BeeKeeper.getLog().info("js", JsUtils.md5(src));
    BeeKeeper.getLog().info("js fast", JsUtils.md5fast(src));
    BeeKeeper.getLog().info(BeeConst.CLIENT, Codec.md5(src));

    BeeKeeper.getRpc().makePostRequest(BeeService.SERVICE_GET_DIGEST, ContentType.BINARY, src);
  }

  public static void doAjaxKeys(String[] arr) {
    if (BeeUtils.length(arr) == 3) {
      String loc = arr[1];
      String key = arr[2];

      if (Global.confirm("add api key", loc, key)) {
        AjaxKeyRepository.putKey(loc, key);
      }
    }

    Map<String, String> keyMap = AjaxKeyRepository.getKeys();
    if (BeeUtils.isEmpty(keyMap)) {
      BeeKeeper.getLog().warning("api key repository is empty");
      return;
    }

    List<Property> lst = new ArrayList<Property>();
    for (Map.Entry<String, String> entry : keyMap.entrySet()) {
      lst.add(new Property(entry.getKey(), entry.getValue()));
    }

    BeeKeeper.getUi().showGrid(lst, "Location", "Api Key");
  }

  public static void doLog(String[] arr) {
    if (BeeUtils.length(arr) > 1) {
      String z = arr[1];

      if (BeeUtils.inList(z, BeeConst.STRING_ZERO, BeeConst.STRING_MINUS)) {
        BeeKeeper.getLog().hide();
      } else if (BeeUtils.isDigit(z)) {
        BeeKeeper.getLog().resize(BeeUtils.toInt(z));
      } else if (BeeUtils.startsSame(z, "clear")) {
        BeeKeeper.getLog().clear();
      } else {
        BeeKeeper.getLog().show();
        BeeKeeper.getLog().info((Object[]) arr);
        BeeKeeper.getLog().addSeparator();
      }

      return;
    }

    Level[] levels = new Level[]{Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO,
        Level.WARNING, Level.SEVERE};
    for (Level lvl : levels) {
      BeeKeeper.getLog().log(lvl, lvl.getName().toLowerCase());
    }
    BeeKeeper.getLog().addSeparator();
  }

  public static void doMenu(String[] arr) {
    if (BeeUtils.length(arr) > 1) {
      ParameterList params = BeeKeeper.getRpc().createParameters(BeeService.SERVICE_GET_MENU);
      params.addPositionalHeader(arr[1]);
      BeeKeeper.getRpc().makeGetRequest(params);
    } else {
      BeeKeeper.getMenu().showMenu();
    }
  }

  public static void doScreen(String[] arr) {
    Split screen = BeeKeeper.getUi().getScreenPanel();
    Assert.notNull(screen);

    String p1 = ArrayUtils.getQuietly(arr, 0);
    String p2 = ArrayUtils.getQuietly(arr, 1);

    if (BeeUtils.same(p1, "screen")) {
      BeeKeeper.getUi().showGrid(screen.getInfo());
      return;
    }

    Direction dir = DomUtils.getDirection(p1);
    if (dir == null) {
      Global.sayHuh(p1, p2);
      return;
    }

    if (BeeUtils.isEmpty(p2)) {
      BeeKeeper.getUi().showGrid(screen.getDirectionInfo(dir));
      return;
    }

    double size = BeeUtils.toDouble(p2);
    if (Double.isNaN(size)) {
      Global.showError(p1, p2, "NaN");
      return;
    }

    screen.setDirectionSize(dir, size);
  }

  public static void eval(String v, String[] arr) {
    String xpr = v.substring(arr[0].length()).trim();

    if (BeeUtils.isEmpty(xpr)) {
      Global.sayHuh(v);
    } else {
      Global.showDialog(xpr, JsUtils.evalToString(xpr));
    }
  }

  public static void getCharsets() {
    ParameterList params = BeeKeeper.getRpc().createParameters(BeeService.SERVICE_GET_RESOURCE);
    params.addPositionalHeader("cs");

    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void getFs() {
    ParameterList params = BeeKeeper.getRpc().createParameters(BeeService.SERVICE_GET_RESOURCE);
    params.addPositionalHeader("fs");

    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void getKeys(String[] arr) {
    int parCnt = BeeUtils.length(arr) - 1;
    if (parCnt <= 0) {
      Global.showError("getKeys", "table not specified");
      return;
    }

    ParameterList params = BeeKeeper.getRpc().createParameters(
        BeeUtils.same(arr[0], "pk") ? BeeService.SERVICE_DB_PRIMARY : BeeService.SERVICE_DB_KEYS);
    for (int i = 0; i < parCnt; i++) {
      params.addPositionalHeader(arr[i + 1]);
    }
    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void getResource(String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      Global.sayHuh(BeeUtils.transform(arr));
      return;
    }

    ParameterList params = BeeKeeper.getRpc().createParameters(BeeService.SERVICE_GET_RESOURCE);
    params.addPositionalHeader(arr);

    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void playAudio(String[] arr) {
    String src = ArrayUtils.getQuietly(arr, 1);
    if (BeeUtils.isEmpty(src)) {
      BeeKeeper.getLog().warning("source not specified");
      return;
    }

    Audio widget = Audio.createIfSupported();
    if (widget == null) {
      BeeKeeper.getLog().severe("audio not supported");
      return;
    }

    widget.getAudioElement().setSrc(src);
    widget.getAudioElement().setControls(true);

    BeeKeeper.getUi().updateActivePanel(widget, true);
  }

  public static void playVideo(String[] arr) {
    if (!Video.isSupported()) {
      BeeKeeper.getLog().severe("video not supported");
      return;
    }

    String src = ArrayUtils.getQuietly(arr, 1);
    if (BeeUtils.isEmpty(src)) {
      src = "http://people.opera.com/shwetankd/webm/sunflower.webm";
    }

    Video widget = new Video(src);
    widget.getVideoElement().setControls(true);

    BeeKeeper.getUi().updateActivePanel(widget, true);
  }

  public static void showBrowser(String[] arr) {
    boolean wnd = false;
    boolean loc = false;
    boolean nav = false;
    boolean scr = false;

    for (int i = 1; i < BeeUtils.length(arr); i++) {
      switch (arr[i].toLowerCase().charAt(0)) {
        case 'w':
          wnd = true;
          break;
        case 'l':
          loc = true;
          break;
        case 'n':
          nav = true;
          break;
        case 's':
          scr = true;
          break;
      }
    }

    if (!wnd && !loc && !nav && !scr) {
      wnd = loc = nav = scr = true;
    }

    List<ExtendedProperty> info = new ArrayList<ExtendedProperty>();

    if (wnd) {
      PropertyUtils.appendChildrenToExtended(info, "Window", Browser.getWindowInfo());
    }
    if (loc) {
      PropertyUtils.appendChildrenToExtended(info, "Location", Browser.getLocationInfo());
    }
    if (nav) {
      PropertyUtils.appendChildrenToExtended(info, "Navigator", Browser.getNavigatorInfo());
    }
    if (scr) {
      PropertyUtils.appendChildrenToExtended(info, "Screen", Browser.getScreenInfo());
    }

    BeeKeeper.getUi().showGrid(info);
  }

  public static void showClientLocation() {
    AjaxLoader.load(new Runnable() {
      public void run() {
        ClientLocation location = AjaxLoader.getClientLocation();

        BeeKeeper.getUi().showGrid(
            PropertyUtils.createProperties("City", location.getCity(),
                "Country", location.getCountry(), "Country Code", location.getCountryCode(),
                "Latitude", location.getLatitude(), "Longitude", location.getLongitude(),
                "Region", location.getRegion()));
      }
    });
  }

  public static void showDate(String[] arr) {
    int len = BeeUtils.length(arr);
    BeeDate date;

    if (len == 2 && BeeUtils.isDigit(arr[1])) {
      date = new BeeDate(arr[1]);

    } else if (len >= 3) {
      int[] fields = new int[7];
      for (int i = 0; i < fields.length; i++) {
        if (i < len - 1) {
          fields[i] = BeeUtils.toInt(arr[i + 1]);
        } else {
          fields[i] = 0;
        }
      }
      date = new BeeDate(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5],
          fields[6]);

    } else {
      date = new BeeDate();
    }

    List<Property> lst = PropertyUtils.createProperties("Time", date.getTime(),
        "Year", date.getYear(), "Month", date.getMonth(), "Dom", date.getDom(),
        "Dow", date.getDow(), "Doy", date.getDoy(), "Hour", date.getHour(),
        "Minute", date.getMinute(), "Second", date.getSecond(), "Millis", date.getMillis(),
        "Log", date.toLog(), "String", date.toString(),
        "Date", new Date(date.getTime()).toString(), "Tz Offset", Global.getTzo());

    BeeKeeper.getUi().showGrid(lst);
  }

  public static void showDateFormat() {
    int r = DateTimeFormat.PredefinedFormat.values().length;
    String[][] data = new String[r][2];

    int i = 0;
    for (DateTimeFormat.PredefinedFormat dtf : DateTimeFormat.PredefinedFormat.values()) {
      data[i][0] = dtf.toString();
      data[i][1] = DateTimeFormat.getFormat(dtf).format(new Date());
      i++;
    }

    BeeKeeper.getUi().showGrid(data, "Format", "Value");
  }

  public static void showDnd() {
    if (!EventUtils.supportsDnd()) {
      BeeKeeper.getLog().warning("dnd not supported");
      return;
    }

    List<Property> lst = EventUtils.showDnd();
    if (BeeUtils.isEmpty(lst)) {
      Global.showDialog("dnd mappings empty");
    } else if (lst.size() <= 30) {
      Global.modalGrid(BeeUtils.concat(1, "Dnd", BeeUtils.bracket(lst.size())), lst);
    } else {
      BeeKeeper.getUi().showGrid(lst);
    }
  }

  public static void showElement(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      Global.sayHuh(v);
      return;
    }

    JavaScriptObject obj = DOM.getElementById(arr[1]);
    if (obj == null) {
      Global.showError(arr[1], "element id not found");
      return;
    }

    String patt = ArrayUtils.getQuietly(arr, 2);
    JsArrayString prp = JsUtils.getProperties(obj, patt);

    if (JsUtils.isEmpty(prp)) {
      Global.showError(v, "properties not found");
      return;
    }

    JsData<?> table = (JsData<?>) DataUtils.createTable(prp, "property", "type", "value");
    table.sort(0);

    if (table.getNumberOfRows() <= 20) {
      Global.modalGrid(v, table);
    } else {
      BeeKeeper.getUi().showGrid(table);
    }
  }

  public static void showFunctions(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      Global.sayHuh(v);
      return;
    }

    JavaScriptObject obj = JsUtils.eval(arr[1]);
    if (obj == null) {
      Global.showError(arr[1], "not a js object");
      return;
    }

    String patt = ArrayUtils.getQuietly(arr, 2);
    JsArrayString fnc = JsUtils.getFunctions(obj, patt);

    if (JsUtils.isEmpty(fnc)) {
      Global.showError(v, "functions not found");
      return;
    }
    if (fnc.length() <= 5) {
      Global.showDialog(v, fnc.join());
      return;
    }

    JsData<?> table = (JsData<?>) DataUtils.createTable(fnc, "function");
    table.sort(0);

    if (BeeUtils.same(arr[0], "f") && table.getNumberOfRows() <= 20) {
      Global.modalGrid(v, table);
    } else {
      BeeKeeper.getUi().showGrid(table);
    }
  }

  public static void showGeo() {
    BeeLabel widget = new BeeLabel("Looking for location...");
    getGeo(widget.getElement());
    BeeKeeper.getUi().updateActivePanel(widget);
  }

  public static void showGwt() {
    Global.modalGrid("GWT",
        PropertyUtils.createProperties("Host Page Base URL", GWT.getHostPageBaseURL(),
            "Module Base URL", GWT.getModuleBaseURL(),
            "Module Name", GWT.getModuleName(),
            "Permutation Strong Name", GWT.getPermutationStrongName(),
            "Uncaught Exception Handler", GWT.getUncaughtExceptionHandler(),
            "Unique Thread Id", GWT.getUniqueThreadId(),
            "Version", GWT.getVersion(),
            "Is Client", GWT.isClient(),
            "Is Prod Mode", GWT.isProdMode(),
            "Is Script", GWT.isScript()));
  }

  public static void showInput() {
    FlexTable table = new FlexTable();
    table.setCellSpacing(3);

    String[] types = new String[]{
        "search", "tel", "url", "email", "datetime", "date", "month", "week", "time",
        "datetime-local", "number", "range", "color"};
    TextBox widget;

    int row = 0;
    for (String type : types) {
      table.setWidget(row, 0, new BeeLabel(type));

      if (Features.supportsInputType(type)) {
        widget = new TextBox();
        widget.getElement().setAttribute(DomUtils.ATTRIBUTE_TYPE, type);

        if (type.equals("search")) {
          if (Features.supportsAttributePlaceholder()) {
            widget.getElement().setAttribute("placeholder", "Search...");
            widget.getElement().setAttribute("results", "0");
          }
        } else if (type.equals("number")) {
          widget.getElement().setAttribute("min", "0");
          widget.getElement().setAttribute("max", "20");
          widget.getElement().setAttribute("step", "2");
          widget.getElement().setAttribute("value", "4");
        } else if (type.equals("range")) {
          widget.getElement().setAttribute("min", "0");
          widget.getElement().setAttribute("max", "50");
          widget.getElement().setAttribute("step", "5");
          widget.getElement().setAttribute("value", "30");
        }

        table.setWidget(row, 1, widget);
      } else {
        table.setWidget(row, 1, new BeeLabel("not supported"));
      }

      row++;
    }

    BeeKeeper.getUi().updateActivePanel(table, true);
  }

  public static void showMeter(String[] arr) {
    if (!Features.supportsElementMeter()) {
      Global.showError("meter element not supported");
      return;
    }

    double min = 0;
    double max = 100;
    double value = 30;
    double low = 20;
    double high = 80;
    double optimum = 50;

    String pars = "<>vlho";
    int p = -1, j;
    String s, z;
    char c;

    for (int i = 1; i < arr.length; i++) {
      s = arr[i].toLowerCase();
      c = s.charAt(0);
      j = pars.indexOf(c);

      if (j >= 0) {
        z = s.substring(1);
        p = j;
      } else {
        z = s;
        p++;
      }

      switch (p) {
        case 0:
          min = BeeUtils.toDouble(z);
          break;
        case 1:
          max = BeeUtils.toDouble(z);
          break;
        case 2:
          value = BeeUtils.toDouble(z);
          break;
        case 3:
          low = BeeUtils.toDouble(z);
          break;
        case 4:
          high = BeeUtils.toDouble(z);
          break;
        case 5:
          optimum = BeeUtils.toDouble(z);
          break;
      }
    }

    if (max <= min) {
      max = min + 100;
    }
    value = BeeUtils.limit(value, min, max);
    low = BeeUtils.limit(low, min, max);
    high = BeeUtils.limit(high, low, max);
    optimum = BeeUtils.limit(optimum, min, max);

    FlexTable table = new FlexTable();
    table.setCellPadding(3);
    table.setCellSpacing(3);
    table.setBorderWidth(1);

    int r = 0;
    table.setHTML(r, 0, "min");
    table.setHTML(r, 1, BeeUtils.toString(min));
    r++;
    table.setHTML(r, 0, "max");
    table.setHTML(r, 1, BeeUtils.toString(max));
    r++;
    table.setHTML(r, 0, "low");
    table.setHTML(r, 1, BeeUtils.toString(low));
    r++;
    table.setHTML(r, 0, "high");
    table.setHTML(r, 1, BeeUtils.toString(high));
    r++;
    table.setHTML(r, 0, "optimum");
    table.setHTML(r, 1, BeeUtils.toString(optimum));

    r++;
    table.setHTML(r, 0, BeeUtils.toString(value));
    table.setWidget(r, 1, new Meter(min, max, value, low, high, optimum));

    for (double i = min; i <= max; i += (max - min) / 10) {
      r++;
      table.setHTML(r, 0, BeeUtils.toString(i));
      table.setWidget(r, 1, new Meter(min, max, i, low, high, optimum));
    }
    BeeKeeper.getUi().updateActivePanel(table, true);
  }

  public static void showProgress(String[] arr) {
    if (!Features.supportsElementProgress()) {
      Global.showError("progress element not supported");
      return;
    }

    final int steps;
    int millis = 250;

    double max = 100;
    double value = 0;

    String s = ArrayUtils.getQuietly(arr, 1);
    if (BeeUtils.isDigit(s)) {
      steps = BeeUtils.toInt(s);
    } else {
      steps = 100;
    }
    s = ArrayUtils.getQuietly(arr, 2);
    if (BeeUtils.isDigit(s)) {
      millis = BeeUtils.toInt(s);
    }
    s = ArrayUtils.getQuietly(arr, 3);
    if (BeeUtils.isDigit(s)) {
      max = BeeUtils.toDouble(s);
    }

    Absolute panel = new Absolute();
    panel.add(new BeeLabel("indeterminate"), 10, 8);
    panel.add(new Progress(), 120, 10);

    panel.add(new BeeLabel(BeeUtils.addName("steps", steps)), 10, 36);
    panel.add(new BeeLabel(BeeUtils.addName("millis", millis)), 10, 53);
    panel.add(new BeeLabel(BeeUtils.addName("max", max)), 10, 70);

    final Progress prg = new Progress(max, value);
    panel.add(prg, 120, 40);
    final BeeLabel lbl = new BeeLabel();
    panel.add(lbl, 160, 70);

    Timer timer = new Timer() {
      @Override
      public void run() {
        double v = prg.getValue();
        double x = prg.getMax();

        v += x / steps;
        if (v > x) {
          v = 0;
        }

        prg.setValue(v);
        lbl.setText(BeeUtils.concat(3, BeeUtils.round(v, 1), BeeUtils.round(prg.getPosition(), 3)));
      }
    };
    timer.scheduleRepeating(millis);

    BeeKeeper.getUi().updateActivePanel(panel, true);
  }

  public static void showProperties(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      Global.sayHuh(v);
      return;
    }

    JavaScriptObject obj = JsUtils.eval(arr[1]);
    if (obj == null) {
      Global.showError(arr[1], "not a js object");
      return;
    }

    String patt = ArrayUtils.getQuietly(arr, 2);
    JsArrayString prp = JsUtils.getProperties(obj, patt);

    if (JsUtils.isEmpty(prp)) {
      Global.showError(v, "properties not found");
      return;
    }

    JsData<?> table = (JsData<?>) DataUtils.createTable(prp, "property", "type", "value");
    table.sort(0);

    if (BeeUtils.same(arr[0], "p") && table.getNumberOfRows() <= 20) {
      Global.modalGrid(v, table);
    } else {
      BeeKeeper.getUi().showGrid(table);
    }
  }

  public static void showRpc() {
    if (BeeKeeper.getRpc().getRpcList().isEmpty()) {
      Global.showDialog("RpcList empty");
    } else {
      BeeKeeper.getUi().showGrid(BeeKeeper.getRpc().getRpcList().getDefaultInfo(),
          RpcList.DEFAULT_INFO_COLUMNS);
    }
  }

  public static void showSlider(String[] arr) {
    double value = 0;
    double min = 0;
    double max = 100;
    double step = 1;
    int labels = 5;
    int ticks = 10;

    String pars = "v<>slt";
    int p = -1, j;
    String s, z;
    char c;

    for (int i = 1; i < arr.length; i++) {
      s = arr[i].toLowerCase();
      c = s.charAt(0);
      j = pars.indexOf(c);

      if (j >= 0) {
        z = s.substring(1);
        p = j;
      } else {
        z = s;
        p++;
      }

      switch (p) {
        case 0:
          value = BeeUtils.toDouble(z);
          break;
        case 1:
          min = BeeUtils.toDouble(z);
          break;
        case 2:
          max = BeeUtils.toDouble(z);
          break;
        case 3:
          step = BeeUtils.toDouble(z);
          break;
        case 4:
          labels = BeeUtils.toInt(z);
          break;
        case 5:
          ticks = BeeUtils.toInt(z);
          break;
      }
    }

    if (max <= min) {
      max = min + 100;
    }
    if (step <= 0 || step > (max - min) / 2) {
      step = (max - min) / 10;
    }
    if (value < min) {
      value = min;
    }
    if (value > max) {
      value = max;
    }

    BeeKeeper.getUi().updateActivePanel(new SliderBar(value, min, max, step, labels, ticks));
  }

  public static void showStack() {
    BeeKeeper.getLog().stack();
    BeeKeeper.getLog().addSeparator();
  }

  public static void showSupport() {
    BeeKeeper.getUi().showGrid(Features.getInfo());
  }

  public static void showSvg(String[] arr) {
    if (!Features.supportsSvgInline()) {
      BeeKeeper.getLog().severe("svg not supported");
      return;
    }

    Svg widget = new Svg();
    Flow panel = new Flow();
    panel.add(widget);
    BeeKeeper.getUi().updateActivePanel(panel);

    if (ArrayUtils.length(arr) <= 1) {
      sampleSvg(widget.getElement());
    }
  }

  public static void showTiles() {
    Widget tiles = BeeKeeper.getUi().getScreenPanel().getCenter();
    if (!(tiles instanceof TilePanel)) {
      Global.showDialog("no tiles vailable");
    }

    BeeTree tree = new BeeTree();
    tree.addItem(((TilePanel) tiles).getTree(null, true));

    Global.inform(tree);
  }

  public static void showVars(String[] arr) {
    if (BeeUtils.length(arr) > 1) {
      Global.showVars(ArrayUtils.copyOfRange(arr, 1, arr.length));
    } else {
      Global.showVars();
    }
  }

  public static void storage(String[] arr) {
    int parCnt = ArrayUtils.length(arr) - 1;
    int len = BeeKeeper.getStorage().length();

    if (parCnt <= 1 && len <= 0) {
      Global.inform("Storage empty");
      return;
    }

    if (parCnt <= 0) {
      BeeKeeper.getUi().showGrid(BeeKeeper.getStorage().getAll());
      return;
    }

    String key = arr[1];

    if (parCnt == 1) {
      if (key.equals(BeeConst.STRING_MINUS)) {
        BeeKeeper.getStorage().clear();
        Global.inform(BeeUtils.concat(1, len, "items cleared"));
      } else {
        String z = BeeKeeper.getStorage().getItem(key);
        if (z == null) {
          Global.showError(key, "key not found");
        } else {
          Global.inform(key, z);
        }
      }
      return;
    }

    String value = ArrayUtils.join(arr, 1, 2);

    if (key.equals(BeeConst.STRING_MINUS)) {
      BeeKeeper.getStorage().removeItem(value);
      Global.inform(value, "removed");
    } else {
      BeeKeeper.getStorage().setItem(key, value);
      Global.inform("Storage", BeeUtils.addName(key, value));
    }
  }

  public static void style(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      NodeList<Element> nodes = Document.get().getElementsByTagName("style");
      if (nodes == null || nodes.getLength() <= 0) {
        Global.showDialog("styles not available");
        return;
      }
      int stCnt = nodes.getLength();

      List<ExtendedProperty> lst = new ArrayList<ExtendedProperty>();
      PropertyUtils.addExtended(lst, "Styles", "count", stCnt);

      for (int i = 0; i < stCnt; i++) {
        String ref = "$doc.getElementsByTagName('style').item(" + i + ").sheet.rules";
        int len = JsUtils.evalToInt(ref + ".length");
        PropertyUtils.addExtended(lst, "Style " + BeeUtils.progress(i + 1, stCnt), "rules", len);

        for (int j = 0; j < len; j++) {
          JavaScriptObject obj = JsUtils.eval(ref + "[" + j + "]");
          if (obj == null) {
            PropertyUtils.addExtended(lst, "Rule", BeeUtils.progress(j + 1, len), "not available");
            break;
          }

          JsArrayString prp = JsUtils.getProperties(obj, null);
          for (int k = 0; k < prp.length() - 2; k += 3) {
            PropertyUtils.addExtended(lst,
                BeeUtils.concat(1, "Rule", BeeUtils.progress(j + 1, len), prp.get(k * 3)),
                prp.get(k * 3 + 1), prp.get(k * 3 + 2));
          }
        }
      }

      BeeKeeper.getUi().showGrid(lst);
      return;
    }

    if (!v.contains("{")) {
      Element elem = Document.get().getElementById(arr[1]);
      if (elem == null) {
        Global.showDialog("element id", arr[1], "not found");
        return;
      }

      List<Property> lst = DomUtils.getStyleInfo(elem.getStyle());
      if (BeeUtils.isEmpty(lst)) {
        Global.showDialog("element id", arr[1], "has no style");
      } else {
        BeeKeeper.getUi().showGrid(lst);
      }
      return;
    }

    boolean start = false;
    boolean end = false;
    boolean immediate = false;

    StringBuilder sb = new StringBuilder();
    boolean tg = false;

    for (int i = 1; i < arr.length; i++) {
      if (!tg) {
        if (arr[i].contains("{")) {
          tg = true;
        } else {
          if (BeeUtils.same(arr[i], "start")) {
            start = true;
            continue;
          }
          if (BeeUtils.same(arr[i], "end")) {
            end = true;
            continue;
          }
          if (arr[i].equals("+")) {
            immediate = true;
            continue;
          }
        }
      }
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(arr[i]);
    }

    String st = sb.toString();
    if (!st.contains("{") || !st.contains("}")) {
      Global.showError("Nah pop no style, a strictly roots", v, st);
      return;
    }

    BeeKeeper.getLog().info(st);
    BeeKeeper.getLog().addSeparator();

    if (start) {
      StyleInjector.injectAtStart(st, immediate);
    } else if (end) {
      StyleInjector.injectAtEnd(st, immediate);
    } else {
      StyleInjector.inject(st, immediate);
    }
  }

  public static void unicode(String[] arr) {
    StringBuilder sb = new StringBuilder();
    int len = BeeUtils.length(arr);

    if (len < 2 || len == 2 && BeeUtils.isDigit(ArrayUtils.getQuietly(arr, 1))) {
      int n = (len < 2) ? 10 : BeeUtils.toInt(arr[1]);
      for (int i = 0; i < n; i++) {
        sb.append((char) BeeUtils.randomInt(Character.MIN_VALUE, Character.MAX_VALUE + 1));
      }

    } else {
      for (int i = 1; i < len; i++) {
        String s = arr[i];

        if (s.length() > 1 && BeeUtils.inListIgnoreCase(s.substring(0, 1), "u", "x")
            && BeeUtils.isHexString(s.substring(1))) {
          sb.append(BeeUtils.fromHex(s.substring(1)));
        } else if (s.length() > 2 && BeeUtils.startsSame(s, "0x")
            && BeeUtils.isHexString(s.substring(2))) {
          sb.append(BeeUtils.fromHex(s.substring(2)));

        } else if (BeeUtils.isDigit(s)) {
          int n = BeeUtils.toInt(s);
          if (n > 0 && n < Character.MAX_VALUE && sb.length() > 0) {
            for (int j = 0; j < n; j++) {
              sb.append((char) (sb.charAt(sb.length() - 1) + 1));
            }
          } else {
            sb.append((char) n);
          }

        } else {
          sb.append(s);
        }
      }
    }

    String s = sb.toString();
    byte[] bytes = Codec.toBytes(s);

    int id = BeeKeeper.getRpc().invoke("stringInfo", ContentType.BINARY, s);
    BeeKeeper.getRpc().addUserData(id, "length", s.length(), "data", s,
        "adler32", Codec.adler32(bytes), "crc16", Codec.crc16(bytes),
        "crc32", Codec.crc32(bytes), "crc32d", Codec.crc32Direct(bytes));
  }

  public static void whereAmI() {
    BeeKeeper.getLog().info(BeeConst.whereAmI());
    BeeKeeper.getRpc().makeGetRequest(BeeService.SERVICE_WHERE_AM_I);
  }

  private static native void getGeo(Element element) /*-{
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(showPosition);
    } else {
      element.innerHTML = "no geolocation support";
    }

    function showPosition(position) {
      var lat = position.coords.latitude;
      var lng = position.coords.longitude;
      element.innerHTML = "Lat = " + lat + ", Lng = " + lng;
    }
  }-*/;

  private static native void sampleCanvas(Element el) /*-{
    var ctx = el.getContext("2d");

    for ( var i = 0; i < 6; i++) {
      for ( var j = 0; j < 6; j++) {
        ctx.fillStyle = 'rgb(' + Math.floor(255 - 42.5 * i) + ', ' + Math.floor(255 - 42.5 * j)
            + ', 0)';
        ctx.fillRect(j * 25, i * 25, 25, 25);
      }
    }
  }-*/;

  private static void sampleSvg(Element el) {
    el.setInnerHTML("<circle cx=\"100\" cy=\"75\" r=\"50\" fill=\"blue\" stroke=\"firebrick\" "
        + "stroke-width=\"3\"></circle><text x=\"60\" y=\"155\">Hello Svg</text>");
  }
}
