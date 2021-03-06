package com.butent.bee.server.i18n;

import com.butent.bee.server.Config;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.ibm.icu.text.RuleBasedNumberFormat;

import java.io.File;
import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.TreeMap;

public final class I18nUtils {

  public static final char LOCALE_SEPARATOR = '_';

  private static BeeLogger logger = LogUtils.getLogger(I18nUtils.class);

  /**
   * Compares two locales and returns the result of the comparison.
   */
  public static class LocaleComparator implements Comparator<Locale> {
    @Override
    public int compare(Locale o1, Locale o2) {
      if (o1 == o2) {
        return BeeConst.COMPARE_EQUAL;
      }
      if (o1 == null) {
        return BeeConst.COMPARE_LESS;
      }
      if (o2 == null) {
        return BeeConst.COMPARE_MORE;
      }
      return BeeUtils.compareNullsFirst(o1.toString(), o2.toString());
    }
  }

  public static File getDictionaryDir() {
    return new File(Config.CONFIG_DIR, "dictionaries");
  }

  public static File getDictionaryFile(SupportedLocale supportedLocale) {
    if (supportedLocale == null) {
      logger.severe("getDictionaryFile: locale is null");
      return null;
    } else {
      return new File(getDictionaryDir(),
          FileNameUtils.defaultExtension(supportedLocale.getDictionaryFileName(),
              FileUtils.EXT_PROPERTIES));
    }
  }

  public static List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> lst = new ArrayList<>();

    Map<Locale, String> locales = new TreeMap<>(new LocaleComparator());
    for (Locale lc : Locale.getAvailableLocales()) {
      locales.put(lc, "Loc");
    }

    String sep = BeeConst.STRING_COMMA;
    for (Locale lc : BreakIterator.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "BrIt"));
    }
    for (Locale lc : Collator.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "Coll"));
    }
    for (Locale lc : DateFormat.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "DtF"));
    }
    for (Locale lc : DateFormatSymbols.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "DtFSymb"));
    }
    for (Locale lc : NumberFormat.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "NumF"));
    }
    for (Locale lc : Calendar.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "Cal"));
    }

    int i = 0;
    for (Map.Entry<Locale, String> entry : locales.entrySet()) {
      Locale lc = entry.getKey();
      String av = entry.getValue();
      lst.add(new ExtendedProperty(
          BeeUtils.join(BeeUtils.space(3), BeeUtils.progress(++i, locales.size()), lc.toString()),
          BeeUtils.join(" | ", lc.getDisplayName(), lc.getDisplayName(lc),
              (BeeUtils.count(av, BeeConst.CHAR_COMMA) == 7) ? null : av),
          BeeUtils.join(" ; ",
              BeeUtils.join(" | ", lc.getDisplayLanguage(), lc.getDisplayLanguage(lc)),
              BeeUtils.join(" | ", lc.getDisplayCountry(), lc.getDisplayCountry(lc)),
              BeeUtils.join(" | ", lc.getDisplayVariant(), lc.getDisplayVariant(lc)),
              getIso3Language(lc), getIso3Country(lc))));
    }
    return lst;
  }

  public static List<Property> getInfo() {
    List<Property> lst = PropertyUtils.createProperties("Default Locale", Locale.getDefault());

    Locale[] locales = Locale.getAvailableLocales();
    int len = ArrayUtils.length(locales);
    PropertyUtils.addProperty(lst, "Available Locales", len);
    if (len > 0) {
      Arrays.sort(locales, new LocaleComparator());
      Locale lc;
      for (int i = 0; i < len; i++) {
        lc = locales[i];
        lst.add(new Property(BeeUtils.join(BeeUtils.space(3), lc.toString(),
            BeeUtils.progress(i + 1, len)),
            BeeUtils.join(" | ", lc.getDisplayName(), lc.getDisplayName(lc))));
      }
    }

    String[] languages = Locale.getISOLanguages();
    len = ArrayUtils.length(languages);
    PropertyUtils.addProperty(lst, "Languages", len);
    if (len > 0) {
      Arrays.sort(languages);
      lst.addAll(PropertyUtils.createProperties("language", languages));
    }

    String[] countries = Locale.getISOCountries();
    len = ArrayUtils.length(countries);
    PropertyUtils.addProperty(lst, "Countries", len);
    if (len > 0) {
      Arrays.sort(countries);
      lst.addAll(PropertyUtils.createProperties("country", countries));
    }
    return lst;
  }

  public static String getIso3Country(Locale locale) {
    if (locale == null) {
      return BeeConst.STRING_EMPTY;
    }
    String country;
    try {
      country = locale.getISO3Country();
    } catch (MissingResourceException ex) {
      country = BeeConst.STRING_EMPTY;
    }
    return country;
  }

  public static String getIso3Language(Locale locale) {
    if (locale == null) {
      return BeeConst.STRING_EMPTY;
    }
    String lang;
    try {
      lang = locale.getISO3Language();
    } catch (MissingResourceException ex) {
      lang = BeeConst.STRING_EMPTY;
    }
    return lang;
  }

  public static String getNumberInWords(Number number, Locale locale) {
    return new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT).format(number);
  }

  public static String getTotalInWords(Double amount, String currencyName, String minorName,
      String locale) {
    if (!BeeUtils.isPositive(amount)) {
      return null;
    }
    long number = BeeUtils.toLong(Math.floor(amount));
    int fraction = BeeUtils.round((amount - number) * 100);

    return BeeUtils.joinWords(getNumberInWords(number, toLocale(locale)), currencyName, fraction,
        minorName);
  }

  public static Map<String, String> readProperties(SupportedLocale supportedLocale) {
    Map<String, String> dictionary = new HashMap<>();

    File file = getDictionaryFile(supportedLocale);
    if (file != null) {
      Properties properties = FileUtils.readProperties(file);

      for (String name : properties.stringPropertyNames()) {
        dictionary.put(name, properties.getProperty(name));
      }
    }

    return dictionary;
  }

  public static Locale toLocale(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    for (Locale lc : Locale.getAvailableLocales()) {
      if (BeeUtils.same(lc.toString(), s)) {
        return lc;
      }
    }
    return Locale.getDefault();
  }

  public static String toString(Locale locale) {
    if (locale == null) {
      return BeeConst.NULL;
    } else if (locale.equals(Locale.ROOT)) {
      return "ROOT";
    } else {
      return locale.toString();
    }
  }

  private I18nUtils() {
  }
}
