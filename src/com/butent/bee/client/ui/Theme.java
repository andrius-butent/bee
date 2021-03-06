package com.butent.bee.client.ui;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.json.client.JSONObject;

import com.butent.bee.client.Settings;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.utils.JsonUtils;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Theme {

  private static BeeLogger logger = LogUtils.getLogger(Theme.class);

  private static final Splitter INT_SPLITTER =
      Splitter.on(CharMatcher.inRange(BeeConst.CHAR_ZERO, BeeConst.CHAR_NINE).negate())
          .omitEmptyStrings().trimResults();

  private static JSONObject values;

  public static void load(String serialized) {
    if (BeeUtils.isEmpty(serialized)) {
      setValues(Settings.getTheme());
    } else {
      BeeRowSet rowSet = BeeRowSet.restore(serialized);
      if (!DataUtils.isEmpty(rowSet)) {
        setValues(JsonUtils.toJson(rowSet.getColumns(), rowSet.getRow(0)));
      }
    }

    List<String> rules = getRules();

    String css = getCss();
    if (!BeeUtils.isEmpty(css)) {
      rules.add(css);
    }

    if (!rules.isEmpty()) {
      StyleElement element = Document.get().createStyleElement();
      DomUtils.createId(element, "theme-");

      element.setInnerText(BeeUtils.buildLines(rules));
      DomUtils.getHead().appendChild(element);
    }
  }

  public static int getWorkspaceMarginRight() {
    return getInteger("WorkspaceMarginRight");
  }

  public static int getViewHeaderHeight() {
    return getHeight("ViewHeaderHeight");
  }

  public static int getChildViewHeaderHeight() {
    return getHeight("ChildViewHeaderHeight");
  }

  public static boolean hasViewActionCreateNew() {
    return getBoolean("ViewActionCreateNew");
  }

  public static boolean hasGridActionCreateNew() {
    return getBoolean("GridActionCreateNew");
  }

  public static boolean hasChildActionCreateNew() {
    return getBoolean("ChildActionCreateNew");
  }

  public static boolean hasActionSaveLarge() {
    return getBoolean("ActionSaveLarge");
  }

  public static int getGridHeaderRowHeight() {
    return getHeight("GridHeaderRowHeight");
  }

  public static int getGridBodyRowHeight() {
    return getHeight("GridBodyRowHeight");
  }

  public static int getGridFooterRowHeight() {
    return getHeight("GridFooterRowHeight");
  }

  public static int getGridMarginLeft() {
    return getInteger("GridMarginLeft");
  }

  public static String getGridHeaderFont() {
    return getString("GridHeaderFont");
  }

  public static String getGridBodyFont() {
    return getString("GridBodyFont");
  }

  public static String getGridFooterFont() {
    return getString("GridFooterFont");
  }

  public static int getChildGridHeaderRowHeight() {
    return getHeight("ChildGridHeaderRowHeight");
  }

  public static int getChildGridBodyRowHeight() {
    return getHeight("ChildGridBodyRowHeight");
  }

  public static int getChildGridFooterRowHeight() {
    return getHeight("ChildGridFooterRowHeight");
  }

  public static int getChildGridMarginLeft() {
    return getInteger("ChildGridMarginLeft");
  }

  public static String getChildGridHeaderFont() {
    return getString("ChildGridHeaderFont");
  }

  public static String getChildGridBodyFont() {
    return getString("ChildGridBodyFont");
  }

  public static String getChildGridFooterFont() {
    return getString("ChildGridFooterFont");
  }

  public static int getApplianceHeaderHeight() {
    return getInteger("ApplianceHeaderHeight");
  }

  public static int getSubMenuLineHeight() {
    return getInteger("SubMenuLineHeight");
  }

  public static int getWorkspaceTabHeight() {
    return getInteger("WorkspaceTabHeight");
  }

  public static int getInputLineHeight() {
    return getInteger("InputLineHeight");
  }

  public static String getInputPadding() {
    return getString("InputPadding");
  }

  public static String getListSize1Padding() {
    return getString("ListSize1Padding");
  }

  public static JSONObject getValues() {
    return values;
  }

  private static String getCss() {
    return getString("Css");
  }

  private static boolean getBoolean(String key) {
    return BeeUtils.isTrue(JsonUtils.getBoolean(values, key));
  }

  private static int getHeight(String key) {
    String value = JsonUtils.getString(values, key);

    if (BeeUtils.isEmpty(value)) {
      return BeeConst.UNDEF;

    } else if (BeeUtils.isPositiveInt(value)) {
      return BeeUtils.toInt(value);

    } else {
      List<Integer> ints = parseInts(value);

      if (ints.size() == 4) {
        Collections.sort(ints);
        return BeeUtils.resize(DomUtils.getClientHeight(),
            ints.get(2), ints.get(3), ints.get(0), ints.get(1));

      } else {
        logger.warning(key, "cannot parse", value);
        return BeeConst.UNDEF;
      }
    }
  }

  private static int getInteger(String key) {
    Integer value = JsonUtils.getInteger(values, key);
    return (value == null) ? BeeConst.UNDEF : value;
  }

  private static List<String> getRules() {
    List<String> rules = new ArrayList<>();

    int px = getInputLineHeight();
    if (px > 0) {
      String inp = Selectors.buildSelectors(
          Selectors.classSelector(StyleUtils.NAME_TEXT_BOX),
          Selectors.classSelector(ListBox.STYLE_NAME),
          Selectors.classSelector(DataSelector.STYLE_EDITABLE_CONTAINER),
          Selectors.classSelector(MultiSelector.STYLE_CONTAINER));
      rules.add(StyleUtils.buildRule(inp, StyleUtils.buildLineHeight(px)));
    }

    String padding = getInputPadding();
    if (!BeeUtils.isEmpty(padding)) {
      String inp = Selectors.buildSelectors(
          Selectors.classSelector(StyleUtils.NAME_TEXT_BOX),
          Selectors.classSelector(InputArea.STYLE_NAME));
      rules.add(StyleUtils.buildRule(inp, StyleUtils.buildPadding(padding)));
    }

    padding = getListSize1Padding();
    if (!BeeUtils.isEmpty(padding)) {
      String ls1 = Selectors.conjunction(Selectors.classSelector(ListBox.STYLE_NAME),
          Selectors.attributeEquals(Attributes.SIZE, 1));
      rules.add(StyleUtils.buildRule(ls1, StyleUtils.buildPadding(padding)));
    }

    return rules;
  }

  private static String getString(String key) {
    return JsonUtils.getString(values, key);
  }

  public static List<Integer> parseInts(String input) {
    List<Integer> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(input)) {
      for (String field : INT_SPLITTER.split(input)) {
        if (BeeUtils.isInt(field)) {
          result.add(BeeUtils.toInt(field));
        }
      }
    }
    return result;
  }

  private static void setValues(JSONObject values) {
    Theme.values = values;
  }

  private Theme() {
  }
}
