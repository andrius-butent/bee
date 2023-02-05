package com.butent.bee.shared.modules.orders;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class OrdersConstants {
  public enum OrdersStatus implements HasLocalizedCaption {
    APPROVED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.ordApproved();
      }
    },
    CANCELED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.ordCanceled();
      }
    },
    PREPARED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.ordPrepared();
      }
    },
    SENT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.ordSent();
      }
    },
    FINISH {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.prjStatusApproved();
      }
    };

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }
  }

  public static void register() {
    EnumUtils.register(OrdersStatus.class);
  }

  public static final String SVC_GET_TEMPLATE_ITEMS = "GetTemplateItems";
  public static final String SVC_CREATE_INVOICE_ITEMS = "CreateInvoiceItems";
  public static final String SVC_FILL_RESERVED_REMAINDERS = "FillReservedRemainders";
  public static final String SVC_GET_ERP_STOCKS = "GetERPStocks";
  public static final String SVC_GET_CREDIT_INFO = "GetCreditInfo";

  public static final String TBL_ORDER_ITEMS = "OrderItems";
  public static final String TBL_ORDERS = "Orders";
  public static final String TBL_ORDER_SERIES = "OrderSeries";

  public static final String VIEW_ORDERS = "Orders";
  public static final String VIEW_ORDER_CHILD_INVOICES = "OrderChildInvoices";
  public static final String VIEW_ORDER_ITEMS = "OrderItems";
  public static final String VIEW_ORDERS_TEMPLATES = "OrdersTemplates";
  public static final String VIEW_ORDER_TMPL_ITEMS = "OrderTmplItems";
  public static final String VIEW_ORDER_SALES = "OrderSales";

  public static final String FORM_NEW_ORDER_INVOICE = "NewOrderInvoice";

  public static final String GRID_COMPANY_ORDERS = "CompanyOrders";
  public static final String GRID_OFFERS = "Offers";
  public static final String GRID_ORDERS_INVOICES = "OrdersInvoices";

  public static final String ALS_COMPANY_EMAIL = "CompanyEmail";
  public static final String ALS_CONTACT_EMAIL = "ContactEmail";
  public static final String ALS_MANAGER_FIRST_NAME = "ManagerFirstName";
  public static final String ALS_MANAGER_LAST_NAME = "ManagerLastName";
  public static final String ALS_RESERVATIONS = "Reservations";
  public static final String ALS_CUSTOMER_EMAIL = "CustomerEmail";
  public static final String ALS_PAYER_EMAIL = "PayerEmail";

  public static final String COL_END_DATE = "EndDate";
  public static final String COL_ORDER = "Order";
  public static final String COL_ORDERS_STATUS = "Status";
  public static final String COL_RESERVED_REMAINDER = "ResRemainder";
  public static final String COL_TEMPLATE = "Template";
  public static final String COL_SOURCE = "Source";
  public static final String COL_UNPACKING = "Unpacking";
  public static final String COL_ORDER_ITEM = "OrderItem";
  public static final String COL_COMPLETED_QTY = "CompletedQty";
  public static final String COL_INVISIBLE_DISCOUNT = "InvisibleDiscount";
  public static final String COL_SALE_ITEM = "SaleItem";
  public static final String COL_ORDER_TRADE_OPERATION = "TradeOperation";
  public static final String COL_TRADE_OPERATION_NAME = "OperationName";
  public static final String COL_PAPER_ENVELOPE = "PaperEnvelope";
  public static final String COL_MATERIAL_TYPE = "MaterialType";

  public static final String PRP_FREE_REMAINDER = "FreeRemainder";
  public static final String PRP_COMPLETED_INVOICES = "CompletedInvoices";
  public static final String PRP_AMOUNT_WO_VAT = "AmountWoVAT";
  public static final String PRM_MANAGER_WAREHOUSE = "ManagerWarehouse";

  public static final String PRM_UPDATE_ITEMS_PRICES = "UpdateItemsPrices";
  public static final String PRM_IMPORT_ERP_ITEMS_TIME = "ImportERPItemsTime";
  public static final String PRM_IMPORT_ERP_STOCKS_TIME = "ImportERPStocksTime";
  public static final String PRM_EXPORT_ERP_RESERVATIONS_TIME = "ExportERPReservationsTime";
  public static final String PRM_DEFAULT_SALE_OPERATION = "DefaultSaleOperation";
  public static final String PRM_CLEAR_RESERVATIONS_TIME = "ClearReservationsTime";
  public static final String PRM_MANAGER_DISCOUNT = "ManagerDiscount";
  public static final String PRM_CHECK_DEBT = "CheckDebt";
  public static final String PRM_NOTIFY_ABOUT_DEBTS = "NotifyAboutDebts";

  public static final String STYLE_ITEM_PRICE_PICKER_DISABLED = BeeConst.CSS_CLASS_PREFIX
      + "ItemPricePicker-disabled";

  private OrdersConstants() {
  }
}
