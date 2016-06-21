package com.butent.bee.shared.i18n;

import com.google.common.base.Splitter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Localized {

  private static final BeeLogger logger = LogUtils.getLogger(Localized.class);

  private static final char L10N_PREFIX = '=';
  private static final char L10N_SEPARATOR = '+';

  public static final Splitter L10N_SPLITTER = Splitter.on(L10N_SEPARATOR);

  private static final Map<String, String> glossary = new HashMap<>();

  private static final Dictionary dictionary = key -> BeeUtils.nvl(glossary.get(key), key);

  public static String column(String colName, String locale) {
    return BeeUtils.join(BeeConst.STRING_UNDER, Assert.notEmpty(colName), Assert.notEmpty(locale));
  }

  public static Dictionary dictionary() {
    return dictionary;
  }

  public static Map<String, String> getGlossary() {
    return glossary;
  }

  public static String getLabel(IsColumn column) {
    if (column == null) {
      logger.severe(NameUtils.getClassName(Localized.class), "getLabel: column is null");
      return null;
    } else {
      return maybeTranslate(column.getLabel());
    }
  }

  public static List<String> getLabels(List<? extends IsColumn> columns) {
    List<String> labels = new ArrayList<>();
    for (IsColumn column : columns) {
      labels.add(getLabel(column));
    }
    return labels;
  }

  public static List<String> maybeTranslate(List<String> items) {
    return maybeTranslate(items, glossary);
  }

  public static List<String> maybeTranslate(List<String> items, Map<String, String> data) {
    List<String> result = new ArrayList<>();

    if (items != null) {
      for (String item : items) {
        result.add(maybeTranslate(item, data));
      }
    }

    return result;
  }

  public static String maybeTranslate(String text) {
    return maybeTranslate(text, glossary);
  }

  public static String maybeTranslate(String text, Map<String, String> data) {
    if (text == null || text.length() < 3 || text.charAt(0) != L10N_PREFIX) {
      return text;
    }

    String localized;

    if (text.indexOf(L10N_SEPARATOR) > 0) {
      StringBuilder sb = new StringBuilder();

      for (String s : L10N_SPLITTER.split(text)) {
        sb.append(maybeTranslate(s, data));
      }

      localized = sb.toString();

    } else {
      localized = translate(text.substring(1), data);
    }

    if (localized == null) {
      logger.warning("cannot localize:", text);
      return text;
    } else {
      return localized;
    }
  }

  public static synchronized void setGlossary(Map<String, String> glossary) {
    if (BeeUtils.isEmpty(glossary)) {
      logger.severe("glossary is empty");

    } else {
      if (!Localized.glossary.isEmpty()) {
        Localized.glossary.clear();
      }

      Localized.glossary.putAll(glossary);
      logger.info(NameUtils.getClassName(Localized.class), "glossary",
          dictionary().languageTag(), glossary.size());
    }
  }

  public static String translate(String key) {
    return translate(key, glossary);
  }

  static String format(String message, Map<String, Object> parameters) {
    if (BeeUtils.isEmpty(message)) {
      return BeeConst.STRING_EMPTY;

    } else if (BeeUtils.isEmpty(parameters)) {
      return message;

    } else {
      String s = message;

      for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
        String value = (parameter.getValue() == null)
            ? BeeConst.NULL : parameter.getValue().toString();

        s = BeeUtils.replace(s, parameter.getKey(), value);
      }

      return s;
    }
  }

  private static String translate(String key, Map<String, String> data) {
    return (data == null) ? null : data.get(key);
  }

  private Localized() {
  }
}
