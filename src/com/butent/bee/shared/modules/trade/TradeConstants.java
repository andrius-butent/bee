package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.List;

public final class TradeConstants {

  public static void register() {
    EnumUtils.register(OperationType.class);
    EnumUtils.register(TradeDocumentPhase.class);

    EnumUtils.register(TradeVatMode.class);
    EnumUtils.register(TradeDiscountMode.class);

    EnumUtils.register(TradeCostBasis.class);
  }

  public static final String PRM_ERP_REFRESH_INTERVAL = "ERPRefreshIntervalInMinutes";
  public static final String PRM_OVERDUE_INVOICES = "OverdueInvoices";
  public static final String PRM_MULTIPLIER = "IdMultiplier";

  public static final String SVC_ITEMS_INFO = "ItemsInfo";
  public static final String SVC_CREDIT_INFO = "CreditInfo";
  public static final String SVC_GET_DOCUMENT_DATA = "getTradeDocumentData";
  public static final String SVC_SEND_TO_ERP = "SendToERP";
  public static final String SVC_REMIND_DEBTS_EMAIL = "RemindDebtsEmail";

  public static final String SVC_GET_DOCUMENT_TYPE_CAPTION_AND_FILTER =
      "getTradeDocumentTypeCaptionAndFilter";

  public static final String SVC_DOCUMENT_PHASE_TRANSITION = "TradeDocumentPhaseTransition";
  public static final String SVC_REBUILD_STOCK = "RebuildStock";
  public static final String SVC_CALCULATE_COST = "CalculateCost";

  public static final String SVC_GET_STOCK = "GetStock";
  public static final String SVC_GET_ITEM_STOCK_BY_WAREHOUSE = "GetItemStockByWarehouse";
  public static final String SVC_GET_RESERVATIONS_INFO = "GetReservationsInfo";
  public static final String SVC_CREATE_DOCUMENT = "CreateDocument";
  public static final String SVC_GET_RELATED_TRADE_ITEMS = "GetRelatedTradeItems";

  public static final String SVC_TRADE_STOCK_REPORT = "GetTradeStockReport";
  public static final String SVC_TRADE_MOVEMENT_OF_GOODS_REPORT = "GetTradeMovementOfGoodsReport";

  public static final String SVC_SUBMIT_PAYMENT = "SubmitPayment";
  public static final String SVC_DISCHARGE_DEBT = "DischargeDebt";
  public static final String SVC_DISCHARGE_PREPAYMENT = "DischargePrepayment";

  public static final String TBL_PURCHASES = "Purchases";
  public static final String TBL_PURCHASE_USAGE = "PurchaseUsage";
  public static final String TBL_PURCHASE_ITEMS = "PurchaseItems";
  public static final String TBL_SALES = "Sales";
  public static final String TBL_SALES_SERIES = "SaleSeries";
  public static final String TBL_SALE_ITEMS = "SaleItems";

  public static final String TBL_TRADE_OPERATIONS = "TradeOperations";
  public static final String TBL_TRADE_SERIES = "TradeSeries";
  public static final String TBL_SERIES_MANAGERS = "SeriesManagers";
  public static final String TBL_TRADE_STATUSES = "TradeStatuses";

  public static final String TBL_TRADE_DOCUMENTS = "TradeDocuments";
  public static final String TBL_TRADE_DOCUMENT_ITEMS = "TradeDocumentItems";
  public static final String TBL_TRADE_ITEM_COST = "TradeItemCost";
  public static final String TBL_TRADE_STOCK = "TradeStock";

  public static final String TBL_TRADE_TYPE_OPERATIONS = "TradeTypeOperations";
  public static final String TBL_TRADE_TYPE_STATUSES = "TradeTypeStatuses";
  public static final String TBL_TRADE_TYPE_TAGS = "TradeTypeTags";

  public static final String TBL_EXPENDITURE_TYPES = "ExpenditureTypes";
  public static final String TBL_TRADE_EXPENDITURES = "TradeExpenditures";

  public static final String TBL_TRADE_PAYMENTS = "TradePayments";
  public static final String TBL_TRADE_PAYMENT_TERMS = "TradePaymentTerms";

  public static final String VAR_VIEW_NAME = "view_name";
  public static final String VAR_ID_LIST = "IdList";
  public static final String VAR_SUBJECT = "Subject";
  public static final String VAR_HEADER = "Header";
  public static final String VAR_FOOTER = "Footer";

  public static final String VIEW_DEBTS = "Debts";
  public static final String VIEW_DEBT_REPORTS = "DebtReports";
  public static final String VIEW_DEBT_REMINDER_TEMPLATE = "DebtReminderTemplates";

  public static final String VIEW_PURCHASES = "Purchases";
  public static final String VIEW_PURCHASE_ITEMS = "PurchaseItems";
  public static final String VIEW_SALES = "Sales";
  public static final String VIEW_SALE_ITEMS = "SaleItems";
  public static final String VIEW_SALE_FILES = "SaleFiles";

  public static final String VIEW_TRADE_NOTES = "TradeNotes";
  public static final String VIEW_TRADE_OPERATIONS = "TradeOperations";
  public static final String VIEW_TRADE_SERIES = "TradeSeries";
  public static final String VIEW_TRADE_STATUSES = "TradeStatuses";
  public static final String VIEW_TRADE_TAGS = "TradeTags";

  public static final String VIEW_SERIES_MANAGERS = "SeriesManagers";

  public static final String VIEW_TRADE_DOCUMENTS = "TradeDocuments";
  public static final String VIEW_TRADE_DOCUMENT_ITEMS = "TradeDocumentItems";
  public static final String VIEW_TRADE_ITEM_COST = "TradeItemCost";
  public static final String VIEW_TRADE_PAYMENTS = "TradePayments";
  public static final String VIEW_TRADE_STOCK = "TradeStock";

  public static final String VIEW_TRADE_DOCUMENT_TYPES = "TradeDocumentTypes";
  public static final String VIEW_TRADE_DOCUMENT_TAGS = "TradeDocumentTags";

  public static final String VIEW_EXPENDITURE_TYPES = "ExpenditureTypes";
  public static final String VIEW_TRADE_EXPENDITURES = "TradeExpenditures";

  public static final String VIEW_TRADE_MOVEMENT = "TradeMovement";
  public static final String VIEW_ITEM_SELECTION = "ItemSelection";

  public static final String COL_PURCHASE = "Purchase";

  public static final String COL_SALE = "Sale";
  public static final String COL_SALE_PROFORMA = "Proforma";
  public static final String COL_SALE_PAYER = "Payer";

  public static final String COL_TRADE_DATE = "Date";
  public static final String COL_TRADE_SERIES = "Series";
  public static final String COL_TRADE_NUMBER = "Number";
  public static final String COL_TRADE_OPERATION = "Operation";
  public static final String COL_TRADE_AMOUNT = "Amount";
  public static final String COL_TRADE_CURRENCY = "Currency";
  public static final String COL_TRADE_WAREHOUSE_FROM = "WarehouseFrom";
  public static final String COL_TRADE_WAREHOUSE_TO = "WarehouseTo";
  public static final String COL_TRADE_SUPPLIER = "Supplier";
  public static final String COL_TRADE_CUSTOMER = "Customer";
  public static final String COL_TRADE_PAYER = "Payer";
  public static final String COL_TRADE_MANAGER = "Manager";
  public static final String COL_TRADE_VEHICLE = "Vehicle";
  public static final String COL_TRADE_TERM = "Term";
  public static final String COL_TRADE_NOTES = "Notes";

  public static final String COL_TRADE_INVOICE_PREFIX = "InvoicePrefix";
  public static final String COL_TRADE_SALE_SERIES = "SaleSeries";
  public static final String COL_TRADE_INVOICE_NO = "InvoiceNo";
  public static final String COL_TRADE_PAYMENT_TIME = "PaymentTime";
  public static final String COL_TRADE_PAID = "Paid";
  public static final String COL_TRADE_EXPORTED = "Exported";
  public static final String COL_TRADE_KIND = "Kind";
  public static final String COL_TRADE_DEBT = "Debt";
  public static final String COL_TRADE_DEBT_COUNT = "DebtCount";
  public static final String COL_TRADE_JOIN = "Join";

  public static final String COL_TRADE_BOL_SERIES = "BoLSeries";
  public static final String COL_TRADE_BOL_NUMBER = "BoLNumber";
  public static final String COL_TRADE_BOL_ISSUE_DATE = "BoLIssueDate";
  public static final String COL_TRADE_BOL_DEPARTURE_DATE = "BoLDepartureDate";
  public static final String COL_TRADE_BOL_UNLOADING_DATE = "BoLUnloadingDate";
  public static final String COL_TRADE_BOL_LOADING = "BoLLoading";
  public static final String COL_TRADE_BOL_UNLOADING = "BoLUnloading";
  public static final String COL_TRADE_BOL_DRIVER_TAB_NO = "BoLDriverTabNo";
  public static final String COL_TRADE_BOL_VEHICLE_NUMBER = "BoLVehicleNumber";
  public static final String COL_TRADE_BOL_DRIVER = "BoLDriver";
  public static final String COL_TRADE_BOL_CARRIER = "BoLCarrier";

  public static final String COL_TRADE_VAT_PLUS = "VatPlus";
  public static final String COL_TRADE_VAT = "Vat";
  public static final String COL_TRADE_VAT_PERC = "VatPercent";

  public static final String COL_TRADE_DISCOUNT = "Discount";
  public static final String COL_TRADE_DISCOUNT_PERC = "DiscountPercent";

  public static final String COL_TRADE_ITEM_ORDINAL = "Ordinal";
  public static final String COL_TRADE_ITEM_ARTICLE = "Article";
  public static final String COL_TRADE_ITEM_QUANTITY = "Quantity";
  public static final String COL_TRADE_ITEM_PRICE = "Price";
  public static final String COL_TRADE_ITEM_NOTE = "Note";
  public static final String COL_TRADE_ITEM_PARENT = "Parent";
  public static final String COL_TRADE_ITEM_WAREHOUSE_FROM = "ItemWarehouseFrom";
  public static final String COL_TRADE_ITEM_WAREHOUSE_TO = "ItemWarehouseTo";
  public static final String COL_TRADE_ITEM_EMPLOYEE = "Employee";
  public static final String COL_TRADE_ITEM_VEHICLE = "ItemVehicle";

  public static final String COL_SERIES = "Series";
  public static final String COL_SERIES_NAME = "SeriesName";
  public static final String COL_SERIES_NUMBER_PREFIX = "NumberPrefix";
  public static final String COL_SERIES_NUMBER_LENGTH = "NumberLength";

  public static final String COL_SERIES_MANAGER = "Manager";
  public static final String COL_SERIES_DEFAULT = "IsDefault";

  public static final String COL_OPERATION_NAME = "OperationName";
  public static final String COL_OPERATION_TYPE = "OperationType";
  public static final String COL_OPERATION_KIND = "Kind";
  public static final String COL_OPERATION_PRICE = "Price";
  public static final String COL_OPERATION_VAT_MODE = "OperationVatMode";
  public static final String COL_OPERATION_VAT_PERCENT = "VatPercent";
  public static final String COL_OPERATION_DISCOUNT_MODE = "OperationDiscountMode";
  public static final String COL_OPERATION_WAREHOUSE_FROM = "WarehouseFrom";
  public static final String COL_OPERATION_WAREHOUSE_TO = "WarehouseTo";
  public static final String COL_OPERATION_DEFAULT = "IsDefault";
  public static final String COL_OPERATION_CASH_REGISTER_NO = "CashRegisterNo";
  public static final String COL_OPERATION_ORDINAL = "Ordinal";

  public static final String COL_STATUS_NAME = "StatusName";
  public static final String COL_STATUS_ACTIVE = "StatusActive";
  public static final String COL_TEMPLATE_SUBJECT = "Subject";
  public static final String COL_TEMPLATE_FIRST_PARAGRAPH = "FirstParagraph";
  public static final String COL_TEMPLATE_LAST_PARAGRAPH = "LastParagraph";

  public static final String COL_TRADE_DOCUMENT_PHASE = "Phase";
  public static final String COL_TRADE_DOCUMENT_OWNER = "Owner";
  public static final String COL_TRADE_DOCUMENT_STATUS = "Status";
  public static final String COL_TRADE_DOCUMENT_NUMBER_1 = "Number1";
  public static final String COL_TRADE_DOCUMENT_NUMBER_2 = "Number2";
  public static final String COL_TRADE_DOCUMENT_RECEIVED_DATE = "ReceivedDate";

  public static final String COL_TRADE_DOCUMENT_DISCOUNT = "DocumentDiscount";
  public static final String COL_TRADE_DOCUMENT_PRICE_NAME = "PriceName";
  public static final String COL_TRADE_DOCUMENT_VAT_MODE = "DocumentVatMode";
  public static final String COL_TRADE_DOCUMENT_DISCOUNT_MODE = "DocumentDiscountMode";

  public static final String COL_TRADE_DOCUMENT = "TradeDocument";
  public static final String COL_TRADE_DOCUMENT_ITEM = "TradeDocumentItem";

  public static final String COL_TRADE_DOCUMENT_ITEM_DISCOUNT = "Discount";
  public static final String COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT = "DiscountIsPercent";
  public static final String COL_TRADE_DOCUMENT_ITEM_VAT = "Vat";
  public static final String COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT = "VatIsPercent";

  public static final String COL_DOCUMENT_TYPE_NAME = "DocumentTypeName";
  public static final String COL_DOCUMENT_TYPE = "DocumentType";

  public static final String COL_TRADE_TAG = "TradeTag";
  public static final String COL_TAG_NAME = "TagName";

  public static final String COL_PRIMARY_DOCUMENT_ITEM = "PrimaryDocumentItem";
  public static final String COL_STOCK_QUANTITY = "Quantity";
  public static final String COL_STOCK_WAREHOUSE = "Warehouse";

  public static final String COL_TRADE_PAYMENT_DATE = "PaymentDate";
  public static final String COL_TRADE_PAYMENT_AMOUNT = "PaymentAmount";
  public static final String COL_TRADE_PAYMENT_ACCOUNT = "PaymentAccount";
  public static final String COL_TRADE_PAYMENT_TYPE = "PaymentType";
  public static final String COL_TRADE_PAYMENT_SERIES = "PaymentSeries";
  public static final String COL_TRADE_PAYMENT_NUMBER = "PaymentNumber";
  public static final String COL_TRADE_PREPAYMENT_PARENT = "PrepaymentParent";

  public static final String COL_TRADE_PAYMENT_TYPE_ACCOUNT = "PaymentTypeAccount";

  public static final String COL_TRADE_PAYMENT_TERM_DATE = "PaymentDate";
  public static final String COL_TRADE_PAYMENT_TERM_AMOUNT = "PaymentAmount";

  public static final String COL_EXPENDITURE_TYPE_NAME = "ExpenditureTypeName";
  public static final String COL_EXPENDITURE_TYPE_DEBIT = "Debit";
  public static final String COL_EXPENDITURE_TYPE_CREDIT = "Credit";
  public static final String COL_EXPENDITURE_TYPE_COST_BASIS = "CostBasis";
  public static final String COL_EXPENDITURE_TYPE_SUPPLIER = "Supplier";
  public static final String COL_EXPENDITURE_TYPE_OPERATION = "Operation";
  public static final String COL_EXPENDITURE_TYPE_WAREHOUSE = "Warehouse";
  public static final String COL_EXPENDITURE_TYPE_ITEM = "Item";

  public static final String COL_EXPENDITURE_TYPE = "ExpenditureType";
  public static final String COL_EXPENDITURE_DATE = "Date";
  public static final String COL_EXPENDITURE_AMOUNT = "Amount";
  public static final String COL_EXPENDITURE_CURRENCY = "Currency";
  public static final String COL_EXPENDITURE_VAT = "Vat";
  public static final String COL_EXPENDITURE_VAT_IS_PERCENT = "VatIsPercent";
  public static final String COL_EXPENDITURE_SERIES = "Series";
  public static final String COL_EXPENDITURE_NUMBER = "Number";
  public static final String COL_EXPENDITURE_SUPPLIER = "Supplier";
  public static final String COL_EXPENDITURE_GENERATED_DOCUMENT = "GeneratedDocument";

  public static final String COL_TRADE_ITEM_COST = "Cost";
  public static final String COL_TRADE_ITEM_COST_CURRENCY = "Currency";

  public static final String ALS_TRADE_BOL_SERIES = "BoLSeriesName";
  public static final String ALS_TRADE_BOL_DRIVER_TAB_NO = "DriverTabNo";
  public static final String ALS_TRADE_BOL_DRIVER_EMPLOYEES = "DriverEmployees";

  public static final String ALS_CUSTOMER_NAME = "CustomerName";
  public static final String ALS_PAYER_NAME = "PayerName";
  public static final String ALS_SUPPLIER_NAME = "SupplierName";

  public static final String ALS_OPERATION_BACKGROUND = "OperationBackground";
  public static final String ALS_OPERATION_FOREGROUND = "OperationForeground";
  public static final String ALS_STATUS_BACKGROUND = "StatusBackground";
  public static final String ALS_STATUS_FOREGROUND = "StatusForeground";

  public static final String ALS_EXPENDITURE_TYPE_SUPPLIER = "TypeSupplier";

  public static final String ALS_WAREHOUSE_FROM_CODE = "WarehouseFromCode";
  public static final String ALS_WAREHOUSE_FROM_NAME = "WarehouseFromName";
  public static final String ALS_WAREHOUSE_TO_CODE = "WarehouseToCode";
  public static final String ALS_WAREHOUSE_TO_NAME = "WarehouseToName";

  public static final String ALS_STOCK_PRIMARY_DATE = "PrimaryDate";

  public static final String ALS_COST_CURRENCY = "CostCurrency";
  public static final String ALS_PARENT_COST = "ParentCost";
  public static final String ALS_PARENT_COST_CURRENCY = "ParentCostCurrency";

  public static final String PROP_REMIND_EMAIL = "RemindEmail";

  public static final String PROP_STOCK = "Stock";
  public static final String PROP_RESERVED = "Reserved";
  public static final String PROP_WAREHOUSES = "Warehouses";
  public static final String PROP_COST = "Cost";
  public static final String PROP_LEVEL = "Level";

  public static final String PROP_TD_AMOUNT = "TdAmount";
  public static final String PROP_TD_DISCOUNT = "TdDiscount";
  public static final String PROP_TD_WITHOUT_VAT = "TdWithoutVat";
  public static final String PROP_TD_VAT = "TdVat";
  public static final String PROP_TD_TOTAL = "TdTotal";

  public static final String PROP_TD_PAID = "TdPaid";
  public static final String PROP_TD_DEBT = "TdDebt";

  public static final String VAR_TOTAL = "Total";
  public static final String VAR_DEBT = "Debt";
  public static final String VAR_OVERDUE = "Overdue";
  public static final String VAR_DOCUMENT = "Document";
  public static final String VAR_ITEMS = "Items";
  public static final String VAR_RESERVATIONS = "Reservations";
  public static final String VAR_PAYMENTS = "Payments";
  public static final String VAR_PREPAYMENT = "Prepayment";
  public static final String VAR_KIND = "Kind";

  public static final String GRID_TRADE_OPERATIONS = "TradeOperations";
  public static final String GRID_TRADE_STATUSES = "TradeStatuses";
  public static final String GRID_TRADE_TAGS = "TradeTags";

  public static final String GRID_SERIES_MANAGERS = "SeriesManagers";
  public static final String GRID_DEBTS = "Debts";
  public static final String GRID_DEBT_REPORTS = "DebtReports";
  public static final String GRID_SALES = "Sales";

  public static final String GRID_TRADE_DOCUMENTS = "TradeDocuments";
  public static final String GRID_TRADE_DOCUMENT_ITEMS = "TradeDocumentItems";
  public static final String GRID_TRADE_PAYMENTS = "TradePayments";
  public static final String GRID_TRADE_PAYMENT_TERMS = "TradePaymentTerms";
  public static final String GRID_TRADE_DOCUMENT_FILES = "TradeDocumentFiles";

  public static final String GRID_TRADE_STOCK = "TradeStock";

  public static final String GRID_EXPENDITURE_TYPES = "ExpenditureTypes";
  public static final String GRID_TRADE_EXPENDITURES = "TradeExpenditures";

  public static final String GRID_ITEM_MOVEMENT = "ItemMovement";
  public static final String GRID_TRADE_RELATED_ITEMS = "TradeRelatedItems";

  public static final String GRID_TRADE_PAYABLES = "TradePayables";
  public static final String GRID_TRADE_RECEIVABLES = "TradeReceivables";

  public static final String FORM_SALES_INVOICE = "SalesInvoice";
  public static final String FORM_DEBT_REPORT_TEMPLATE = "DebtReportTemplate";

  public static final String FORM_TRADE_DOCUMENT = "TradeDocument";

  public static final String FORM_PAYMENT_SUPPLIERS = "PaymentSuppliers";
  public static final String FORM_PAYMENT_CUSTOMERS = "PaymentCustomers";

  public static final String PRM_PROTECT_TRADE_DOCUMENTS_BEFORE = "ProtectTradeDocumentsBefore";

  public static final String FILTER_ITEM_HAS_STOCK = "item_has_stock";
  public static final String FILTER_USER_TRADE_SERIES = "user_trade_series";
  public static final String FILTER_HAS_TRADE_DEBT = "has_trade_debt";

  public static final int MAX_STOCK_DEPTH = 1_000;
  public static final int DEFAULT_SERIES_NUMBER_LENGTH = 6;

  public static final String PREFIX_START_STOCK = "Start_";
  public static final String PREFIX_MOVEMENT_IN = "In_";
  public static final String PREFIX_MOVEMENT_OUT = "Out_";
  public static final String PREFIX_END_STOCK = "End_";

  public static final String EMPTY_VALUE_SUFFIX = "_0";

  public static final String RP_DATE = "Date";
  public static final String RP_START_DATE = "StartDate";
  public static final String RP_END_DATE = "EndDate";

  public static final String RP_SHOW_QUANTITY = "Quantity";
  public static final String RP_SHOW_AMOUNT = "Amount";

  public static final String RP_ITEM_PRICE = "Price";
  public static final String RP_CURRENCY = "Currency";

  public static final String RP_RECEIVED_FROM = "ReceivedFrom";
  public static final String RP_RECEIVED_TO = "ReceivedTo";

  public static final String RP_ITEM_FILTER = "ItemFilter";

  public static final String RP_SUMMARY = "Summary";
  public static final String RP_STOCK_COLUMNS = "StockColumns";
  public static final String RP_MOVEMENT_COLUMNS = "MovementColumns";

  public static final String RP_WAREHOUSES = "Warehouses";
  public static final String RP_SUPPLIERS = "Suppliers";
  public static final String RP_MANUFACTURERS = "Manufacturers";
  public static final String RP_DOCUMENTS = "Documents";
  public static final String RP_ITEM_TYPES = "ItemTypes";
  public static final String RP_ITEM_GROUPS = "ItemGroups";
  public static final String RP_ITEM_CATEGORIES = "ItemCategories";
  public static final String RP_ITEMS = "Items";

  public static final String RP_ROW_GROUPS = "RowGroups";
  public static final String RP_ROW_GROUP_VALUE_COLUMNS = "RowGroupValueColums";
  public static final String RP_ROW_GROUP_LABEL_COLUMNS = "RowGroupLabelColums";

  public static final String RP_STOCK_COLUMN_GROUPS = "StockColumnGroups";
  public static final String RP_STOCK_START_COLUMN_LABELS = "StockStartColumnLabels";
  public static final String RP_STOCK_END_COLUMN_LABELS = "StockEndColumnLabels";
  public static final String RP_STOCK_START_COLUMN_VALUES = "StockStartColumnValues";
  public static final String RP_STOCK_END_COLUMN_VALUES = "StockEndColumnValues";

  public static final String RP_MOVEMENT_COLUMN_GROUPS = "MovementColumnGroups";
  public static final String RP_MOVEMENT_IN_COLUMNS = "MovementInColumns";
  public static final String RP_MOVEMENT_OUT_COLUMNS = "MovementOutColumns";

  public static final String RP_QUANTITY_COLUMNS = "QuantityColumns";
  public static final String RP_AMOUNT_COLUMNS = "AmountColumns";
  public static final String RP_PRICE_COLUMN = "PriceColumn";

  public static String keyStockWarehouse(String warehouseCode) {
    return PROP_STOCK + BeeUtils.trim(warehouseCode);
  }

  public static String keyReservedWarehouse(String warehouseCode) {
    return PROP_RESERVED + BeeUtils.trim(warehouseCode);
  }

  public static String keyCostWarehouse(String warehouseCode) {
    return PROP_COST + BeeUtils.trim(warehouseCode);
  }

  public static String reportGroupName(int index) {
    return "Group" + BeeUtils.toString(index);
  }

  public static List<String> reportGroupNames(int count) {
    List<String> names = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      names.add(reportGroupName(i));
    }
    return names;
  }

  private TradeConstants() {
  }
}
