package com.butent.bee.shared.modules.classifiers;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum ItemPrice implements HasLocalizedCaption {
  SALE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.salePrice();
    }

    @Override
    public String getCurrencyColumn() {
      return ClassifierConstants.COL_ITEM_CURRENCY;
    }

    @Override
    public String getPriceColumn() {
      return ClassifierConstants.COL_ITEM_PRICE;
    }
  },

  COST {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.cost();
    }

    @Override
    public String getCurrencyColumn() {
      return ClassifierConstants.COL_ITEM_COST_CURRENCY;
    }

    @Override
    public String getPriceColumn() {
      return ClassifierConstants.COL_ITEM_COST;
    }
  },

  PRICE_1 {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.price1();
    }

    @Override
    public String getCurrencyColumn() {
      return ClassifierConstants.COL_ITEM_CURRENCY_1;
    }

    @Override
    public String getPriceColumn() {
      return ClassifierConstants.COL_ITEM_PRICE_1;
    }
  },

  PRICE_2 {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.price2();
    }

    @Override
    public String getCurrencyColumn() {
      return ClassifierConstants.COL_ITEM_CURRENCY_2;
    }

    @Override
    public String getPriceColumn() {
      return ClassifierConstants.COL_ITEM_PRICE_2;
    }
  },

  PRICE_3 {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.price3();
    }

    @Override
    public String getCurrencyColumn() {
      return ClassifierConstants.COL_ITEM_CURRENCY_3;
    }

    @Override
    public String getPriceColumn() {
      return ClassifierConstants.COL_ITEM_PRICE_3;
    }
  };

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public abstract String getCurrencyColumn();

  public String getCurrencyNameAlias() {
    return getCurrencyColumn() + "Name";
  }

  public abstract String getPriceColumn();
}
