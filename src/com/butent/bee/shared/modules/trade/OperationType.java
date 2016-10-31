package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.finance.TradeAccounts;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum OperationType implements HasLocalizedCaption {
  PURCHASE(false, true, true) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypePurchase();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getCostAccount();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatReceivable();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }
  },

  SALE(true, false, false) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeSale();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getSalesRevenue();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatPayable();
    }
  },

  TRANSFER(true, true, false) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeTransfer();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return null;
    }
  },

  WRITE_OFF(true, false, false) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeWriteOff();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return null;
    }
  },

  POS(true, false, false) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypePointOfSale();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getSalesRevenue();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatPayable();
    }
  },

  CUSTOMER_RETURN(false, true, true) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeCustomerReturn();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getSalesRevenue();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatPayable();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }
  },

  RETURN_TO_SUPPLIER(true, false, false) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeReturnToSupplier();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getCostAccount();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatReceivable();
    }
  };

  private final boolean consumesStock;
  private final boolean producesStock;

  private final boolean providesCost;

  OperationType(boolean consumesStock, boolean producesStock, boolean providesCost) {
    this.consumesStock = consumesStock;
    this.producesStock = producesStock;

    this.providesCost = providesCost;
  }

  public boolean consumesStock() {
    return consumesStock;
  }

  public boolean producesStock() {
    return producesStock;
  }

  public boolean providesCost() {
    return providesCost;
  }

  public abstract Long getAmountDebit(TradeAccounts tradeAccounts);

  public abstract Long getAmountCredit(TradeAccounts tradeAccounts);

  public abstract Long getVatDebit(TradeAccounts tradeAccounts);

  public abstract Long getVatCredit(TradeAccounts tradeAccounts);
}
