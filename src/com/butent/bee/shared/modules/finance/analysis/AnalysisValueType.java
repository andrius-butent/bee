package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum AnalysisValueType implements HasLocalizedCaption {

  ACTUAL('a', false) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValueActualShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValueActual();
    }
  },

  BUDGET('b', true) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValueBudgetShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValueBudget();
    }
  },

  DIFFERENCE('d', true) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValueDifferenceShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValueDifference();
    }
  },

  PERCENTAGE('p', true, 1) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValuePercentageShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValuePercentage();
    }
  };

  public static AnalysisValueType parse(char c) {
    for (AnalysisValueType av : values()) {
      if (av.code == c) {
        return av;
      }
    }
    return null;
  }

  public static AnalysisValueType parse(String s) {
    if (!BeeUtils.isEmpty(s) && s.trim().length() == 1) {
      return parse(s.trim().charAt(0));
    } else {
      return null;
    }
  }

  private final char code;
  private final boolean needsBudget;
  private final int defaultScale;

  AnalysisValueType(char code, boolean needsBudget) {
    this(code, needsBudget, BeeConst.UNDEF);
  }

  AnalysisValueType(char code, boolean needsBudget, int defaultScale) {
    this.code = code;
    this.needsBudget = needsBudget;
    this.defaultScale = defaultScale;
  }

  public abstract String getAbbreviation();

  public char getCode() {
    return code;
  }

  public int getDefaultScale() {
    return defaultScale;
  }

  public boolean hasScale() {
    return !BeeConst.isUndef(defaultScale);
  }

  public boolean needsBudget() {
    return needsBudget;
  }
}
