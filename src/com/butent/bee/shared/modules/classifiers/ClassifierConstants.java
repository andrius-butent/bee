package com.butent.bee.shared.modules.classifiers;

import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public final class ClassifierConstants {

  public static final String SVC_CREATE_COMPANY = "create_company";
  public static final String SVC_CREATE_COMPANY_PERSON = "create_company_person";
  public static final String SVC_COMPANY_INFO = "GetCompanyInfo";
  public static final String SVC_GENERATE_QR_CODE = "GenerateQrCode";
  public static final String SVC_GENERATE_QR_CODE_PERSON = "GenerateQrCodePerson";
  public static final String SVC_GET_COMPANY_TYPE_REPORT = "get_company_type_report";
  public static final String SVC_GET_PRICE_AND_DISCOUNT = "GetPriceAndDiscount";
  public static final String SVC_FILTER_ORDERS = "FilterOrders";
  public static final String SVC_GET_RESERVATION = "GetReservation";
  public static final String SVC_GET_STOCKS_IN_WAREHOUSE = "GetStocksInWarehouse";

  public static final String TIMER_REMIND_COMPANY_ACTIONS = "timer_remind_company_actions";
  public static final String TIMER_REMIND_TASKS_SUMMARY = "timer_remind_tasks_summary_";
  public static final String TIMER_REMIND_CALENDAR_EVENTS = "timer_remind_calendar_events";
  public static final String TIMER_REMIND_USER_TASKS = "timer_remind_user_tasks";
  public static final String TIMER_REMIND_DOCUMENT_END = "timer_remind_document_end_";
  public static final String TIMER_REMIND_PROJECT_DATES = "timer_remind_project_dates";

  public static final String TBL_ITEMS = "Items";
  public static final String TBL_UNITS = "Units";

  public static final String TBL_ITEM_CATEGORY_TREE = "CategoryTree";
  public static final String TBL_ITEM_CATEGORIES = "ItemCategories";
  public static final String TBL_ITEM_MANUFACTURERS = "ItemManufacturers";

  public static final String TBL_CONTACTS = "Contacts";
  public static final String TBL_EMAILS = "Emails";

  public static final String TBL_COMPANY_PERSONS = "CompanyPersons";
  public static final String TBL_COMPANY_USERS = "CompanyUsers";
  public static final String TBL_COMPANIES = "Companies";
  public static final String TBL_COMPANY_TYPES = "CompanyTypes";
  public static final String TBL_COMPANY_CONTACTS = "CompanyContacts";
  public static final String TBL_COMPANY_OBJECTS = "CompanyObjects";

  public static final String TBL_COMPANY_RELATION_TYPES = "CompanyRelationTypes";
  public static final String TBL_COMPANY_RELATION_TYPE_STORE = "CompRelTypeStore";

  public static final String TBL_COMPANY_ACTIVITY_STORE = "CompActStore";

  public static final String TBL_PERSONS = "Persons";
  public static final String TBL_POSITIONS = "Positions";

  public static final String TBL_CITIES = "Cities";
  public static final String TBL_COUNTRIES = "Countries";

  public static final String TBL_BANKS = "Banks";
  public static final String TBL_COMPANY_BANK_ACCOUNTS = "CompanyBankAccounts";

  public static final String TBL_BRANCHES = "Branches";
  public static final String TBL_WAREHOUSES = "Warehouses";

  public static final String TBL_HOLIDAYS = "Holidays";

  public static final String TBL_PAYMENT_TYPES = "PaymentTypes";

  public static final String TBL_DISCOUNTS = "Discounts";

  public static final String TBL_CHART_OF_ACCOUNTS = "ChartOfAccounts";

  public static final String TBL_LOSS_REASONS = "LossReasons";

  public static final String TBL_VEHICLE_TYPES = "VehicleTypes";
  public static final String TBL_VEHICLE_BRANDS = "VehicleBrands";
  public static final String TBL_VEHICLE_MODELS = "VehicleModels";

  public static final String TBL_ADDITION_STOCKS = "AdditionStocks";
  public static final String TBL_WRITE_OFF_STOCKS = "WriteOffStocks";

  public static final String FORM_COMPANY = "Company";
  public static final String FORM_COMPANY_ACTION = "CompanyAction";
  public static final String FORM_PERSON = "Person";
  public static final String FORM_NEW_COMPANY = "NewCompany";
  public static final String FORM_COMPANY_PERSON = "CompanyPerson";

  public static final String VIEW_COMPANIES = "Companies";
  public static final String VIEW_COMPANY_PERSONS = "CompanyPersons";
  public static final String VIEW_PERSONS = "Persons";
  public static final String VIEW_POSITIONS = "Positions";

  public static final String VIEW_ITEMS = "Items";
  public static final String VIEW_ITEM_CATEGORY_TREE = "CategoryTree";
  public static final String VIEW_ITEM_CATEGORIES = "ItemCategories";
  public static final String VIEW_ITEM_REMAINDERS = "ItemRemainders";
  public static final String VIEW_ITEM_SUPPLIERS = "ItemSuppliers";
  public static final String VIEW_ITEM_MANUFACTURERS = "ItemManufacturers";

  public static final String VIEW_COUNTRIES = "Countries";
  public static final String VIEW_CITIES = "Cities";
  public static final String VIEW_BRANCHES = "Branches";
  public static final String VIEW_WAREHOUSES = "Warehouses";

  public static final String VIEW_COMPANY_TYPES = "CompanyTypes";
  public static final String VIEW_COMPANY_GROUPS = "CompanyGroups";
  public static final String VIEW_COMPANY_PRIORITIES = "CompanyPriorities";
  public static final String VIEW_COMPANY_RELATION_TYPES = "CompanyRelationTypes";
  public static final String VIEW_COMPANY_RELATION_TYPE_STORE = "CompRelTypeStore";
  public static final String VIEW_RELATION_TYPE_STATES = "RelationTypeStates";
  public static final String VIEW_FINANCIAL_STATES = "FinancialStates";
  public static final String VIEW_COMPANY_SIZES = "CompanySizes";
  public static final String VIEW_INFORMATION_SOURCES = "InformationSources";
  public static final String VIEW_COMPANY_ACTIVITIES = "CompanyActivities";
  public static final String VIEW_COMPANY_ACTIVITY_STORE = "CompActStore";
  public static final String VIEW_COMPANY_BANK_ACCOUNTS = "CompanyBankAccounts";

  public static final String VIEW_COMPANY_CONTACTS = "CompanyContacts";
  public static final String VIEW_COMPANY_DEPARTMENTS = "CompanyDepartments";
  public static final String VIEW_COMPANY_USERS = "CompanyUsers";
  public static final String VIEW_COMPANY_OBJECTS = "CompanyObjects";

  public static final String VIEW_HOLIDAYS = "Holidays";

  public static final String VIEW_CHART_OF_ACCOUNTS = "ChartOfAccounts";
  public static final String VIEW_JOURNALS = "Journals";

  public static final String GRID_PERSONS = "Persons";
  public static final String GRID_COMPANIES = "Companies";
  public static final String GRID_COMPANY_BANK_ACCOUNTS = "CompanyBankAccounts";
  public static final String GRID_COMPANY_USERS = "CompanyUsers";

  public static final String GRID_ITEMS = "Items";

  public static final String GRID_CHART_OF_ACCOUNTS = "ChartOfAccounts";
  public static final String GRID_JOURNALS = "Journals";

  public static final String COL_COMPANY = "Company";
  public static final String COL_COMPANY_NAME = "Name";
  public static final String COL_COMPANY_CODE = "Code";
  public static final String COL_COMPANY_CONTACT = "CompanyContact";
  public static final String COL_COMPANY_VAT_CODE = "VATCode";
  public static final String COL_COMPANY_PERSON = "CompanyPerson";
  public static final String COL_COMPANY_TYPE = "CompanyType";
  public static final String COL_COMPANY_GROUP = "CompanyGroup";
  public static final String COL_COMPANY_PRIORITY = "CompanyPriority";
  public static final String COL_COMPANY_EXCHANGE_CODE = "ExchangeCode";
  public static final String COL_COMPANY_CREDIT_LIMIT = "CreditLimit";
  public static final String COL_COMPANY_LIMIT_CURRENCY = "LimitCurrency";
  public static final String COL_COMPANY_CREDIT_DAYS = "CreditDays";
  public static final String COL_COMPANY_SUPPLIER_DAYS = "SupplierCreditDays";
  public static final String COL_COMPANY_RELATION_TYPE_STATE = "CompanyRelationTypeState";
  public static final String COL_COMPANY_FINANCIAL_STATE = "FinancialState";
  public static final String COL_COMPANY_SIZE = "CompanySize";
  public static final String COL_COMPANY_INFORMATION_SOURCE = "InformationSource";
  public static final String COL_COMPANY_PRICE_NAME = "PriceName";
  public static final String COL_COMPANY_DISCOUNT_PERCENT = "DiscountPercent";
  public static final String COL_COMPANY_TOLERATED_DAYS = "ToleratedDays";
  public static final String COL_COMPANY_TYPE_PERSON = "TypePerson";

  public static final String COL_PERSON = "Person";
  public static final String COL_FIRST_NAME = "FirstName";
  public static final String COL_LAST_NAME = "LastName";

  public static final String COL_PHOTO = "PhotoFile";

  public static final String COL_DATE_OF_BIRTH = "DateOfBirth";

  public static final String COL_DATE_FROM = "DateFrom";
  public static final String COL_DATE_TO = "DateTo";

  public static final String COL_CATEGORY = "Category";
  public static final String COL_CATEGORY_NAME = "Name";
  public static final String COL_CATEGORY_PARENT = "Parent";

  public static final String COL_ITEM = "Item";
  public static final String COL_ITEM_NAME = "Name";
  public static final String COL_ITEM_NAME_2 = "Name2";
  public static final String COL_ITEM_NAME_3 = "Name3";
  public static final String COL_ITEM_ARTICLE = "Article";
  public static final String COL_ITEM_ARTICLE_2 = "Article2";
  public static final String COL_ITEM_ATTRIBUTE = "Attribute";
  public static final String COL_ITEM_BARCODE = "Barcode";
  public static final String COL_ITEM_IS_SERVICE = "IsService";
  public static final String COL_ITEM_EXTERNAL_CODE = "ExternalCode";
  public static final String COL_ITEM_PRICE = "Price";
  public static final String COL_ITEM_CURRENCY = "Currency";
  public static final String COL_ITEM_COST = "Cost";
  public static final String COL_ITEM_COST_CURRENCY = "CostCurrency";
  public static final String COL_ITEM_PACKAGE_UNITS = "PackageUnits";
  public static final String COL_ITEM_PRICE_1 = "Price1";
  public static final String COL_ITEM_CURRENCY_1 = "Currency1";
  public static final String COL_ITEM_PRICE_2 = "Price2";
  public static final String COL_ITEM_CURRENCY_2 = "Currency2";
  public static final String COL_ITEM_PRICE_3 = "Price3";
  public static final String COL_ITEM_CURRENCY_3 = "Currency3";
  public static final String COL_ITEM_PRICE_4 = "Price4";
  public static final String COL_ITEM_CURRENCY_4 = "Currency4";
  public static final String COL_ITEM_PRICE_5 = "Price5";
  public static final String COL_ITEM_CURRENCY_5 = "Currency5";
  public static final String COL_ITEM_PRICE_6 = "Price6";
  public static final String COL_ITEM_CURRENCY_6 = "Currency6";
  public static final String COL_ITEM_PRICE_7 = "Price7";
  public static final String COL_ITEM_CURRENCY_7 = "Currency7";
  public static final String COL_ITEM_PRICE_8 = "Price8";
  public static final String COL_ITEM_CURRENCY_8 = "Currency8";
  public static final String COL_ITEM_PRICE_9 = "Price9";
  public static final String COL_ITEM_CURRENCY_9 = "Currency9";
  public static final String COL_ITEM_PRICE_10 = "Price10";
  public static final String COL_ITEM_CURRENCY_10 = "Currency10";
  public static final String COL_ITEM_TYPE = "Type";
  public static final String COL_ITEM_GROUP = "Group";
  public static final String COL_ITEM_WEIGHT = "Weight";
  public static final String COL_ITEM_AREA = "Area";
  public static final String COL_ITEM_ORDINAL = "Ordinal";
  public static final String COL_ITEM_DPW = "DaysPerWeek";
  public static final String COL_ITEM_MIN_TERM = "MinTerm";
  public static final String COL_ITEM_LINK = "Link";
  public static final String COL_ITEM_NOT_MANUFACTURED = "NotManufactured";
  public static final String COL_ITEM_REMAINDER_ID = "Id";
  public static final String COL_ITEM_VAT = "Vat";
  public static final String COL_ITEM_VAT_PERCENT = "VatPercent";
  public static final String COL_ITEM_DEFAULT_QUANTITY = "DefaultQuantity";
  public static final String COL_ITEM_BRUTTO = "Brutto";
  public static final String COL_ITEM_NETTO = "Netto";
  public static final String COL_ITEM_VOLUME = "Volume";
  public static final String COL_ITEM_FACTOR = "Factor";
  public static final String COL_ITEM_COUNTRY_OF_ORIGIN = "CountryOfOrigin";
  public static final String COL_ITEM_ADDITIONAL_UNIT = "AdditionalUnit";
  public static final String COL_ITEM_KPN_CODE = "KPNCode";
  public static final String COL_ITEM_DESCRIPTION = "Description";

  public static final String COL_ITEM_MANUFACTURER = "Manufacturer";

  public static final String COL_UNIT = "Unit";
  public static final String COL_UNIT_NAME = "Name";
  public static final String COL_TIME_UNIT = "TimeUnit";
  public static final String COL_BASE_UNIT = "BaseUnit";
  public static final String COL_UNIT_FACTOR = "Factor";

  public static final String COL_CONTACT = "Contact";
  public static final String COL_PHONE = "Phone";
  public static final String COL_MOBILE = "Mobile";
  public static final String COL_FAX = "Fax";
  public static final String COL_EMAIL = "Email";
  public static final String COL_EMAIL_ADDRESS = "Email";
  public static final String COL_ADDRESS = "Address";
  public static final String COL_POST_INDEX = "PostIndex";
  public static final String COL_WEBSITE = "Website";
  public static final String COL_NOTES = "Notes";
  public static final String COL_REMIND_EMAIL = "RemindEmail";
  public static final String COL_REMIND_ACTION_BEFORE = "CompanyActionRemindBefore";
  public static final String COL_TASKS_MAILING_TIME = "TasksMailingTime";
  public static final String COL_EMAIL_INVOICES = "EmailInvoices";

  public static final String COL_CITY = "City";
  public static final String COL_CITY_NAME = "Name";
  public static final String COL_COUNTRY = "Country";
  public static final String COL_COUNTRY_NAME = "Name";
  public static final String COL_COUNTRY_CODE = "Code";

  public static final String COL_COMPANY_USER_COMPANY = "Company";
  public static final String COL_COMPANY_USER_USER = "User";
  public static final String COL_COMPANY_USER_RESPONSIBILITY = "Responsibility";
  public static final String COL_POSITION = "Position";
  public static final String COL_POSITION_NAME = "Name";

  public static final String COL_BRANCH_NAME = "Name";
  public static final String COL_BRANCH_CODE = "Code";
  public static final String COL_BRANCH_PRIMARY_WAREHOUSE = "PrimaryWarehouse";

  public static final String COL_DEFAULT_BANK_ACCOUNT = "DefaultBankAccount";
  public static final String COL_DEFAULT_COMPANY_USER = "DefaultCompanyUser";

  public static final String COL_WAREHOUSE = "Warehouse";
  public static final String COL_WAREHOUSE_CODE = "Code";
  public static final String COL_WAREHOUSE_NAME = "Name";
  public static final String COL_WAREHOUSE_SUPPLIER_CODE = "SupplierCode";
  public static final String COL_WAREHOUSE_BRANCH = "Branch";
  public static final String COL_WAREHOUSE_REMAINDER = "Remainder";

  public static final String COL_RELATION_TYPE = "RelationType";
  public static final String COL_RELATION_TYPE_NAME = "Name";

  public static final String COL_ACTIVITY = "Activity";

  public static final String COL_COMPANY_TYPE_NAME = "Name";

  public static final String COL_COMPANY_OBJECT_NAME = "ObjectName";
  public static final String COL_COMPANY_OBJECT_ADDRESS = "ObjectAddress";

  public static final String COL_BANK = "Bank";
  public static final String COL_BANK_NAME = "Name";
  public static final String COL_BANK_CODE = "BankCode";
  public static final String COL_SWIFT_CODE = "SWIFTCode";

  public static final String COL_BANK_ACCOUNT = "BankAccount";

  public static final String COL_HOLY_COUNTRY = "Country";
  public static final String COL_HOLY_DAY = "Date";

  public static final String COL_PAYMENT_TYPE = "PaymentType";
  public static final String COL_PAYMENT_NAME = "PaymentName";
  public static final String COL_PAYMENT_CASH = "PaymentCash";

  public static final String COL_DISCOUNT_PARENT = "DiscountParent";
  public static final String COL_DISCOUNT_COMPANY = "Company";
  public static final String COL_DISCOUNT_CATEGORY = "Category";
  public static final String COL_DISCOUNT_ITEM = "Item";
  public static final String COL_DISCOUNT_OPERATION = "Operation";
  public static final String COL_DISCOUNT_WAREHOUSE = "Warehouse";
  public static final String COL_DISCOUNT_DATE_FROM = "DateFrom";
  public static final String COL_DISCOUNT_DATE_TO = "DateTo";
  public static final String COL_DISCOUNT_QUANTITY_FROM = "QuantityFrom";
  public static final String COL_DISCOUNT_QUANTITY_TO = "QuantityTo";
  public static final String COL_DISCOUNT_UNIT = "Unit";
  public static final String COL_DISCOUNT_PRICE_NAME = "PriceName";
  public static final String COL_DISCOUNT_PERCENT = "Percent";
  public static final String COL_DISCOUNT_PRICE = "Price";
  public static final String COL_DISCOUNT_CURRENCY = "Currency";

  public static final String COL_ACCOUNT_CODE = "AccountCode";
  public static final String COL_ACCOUNT_NAME = "AccountName";
  public static final String COL_JOURNAL_CODE = "JournalCode";

  public static final String COL_LOSS_REASON_NAME = "ReasonName";
  public static final String COL_LOSS_REASON_TEMPLATE = "Template";
  public static final String COL_LOSS_REASON = "LossReason";
  public static final String COL_LOSS_NOTES = "LossNotes";

  public static final String COL_VEHICLE_BRAND = "Brand";
  public static final String COL_VEHICLE_BRAND_NAME = "BrandName";
  public static final String COL_VEHICLE_MODEL_NAME = "Name";
  public static final String COL_VEHICLE_TYPE_NAME = "Name";

  public static final String WIDGET_STOCKS_IN_WAREHOUSE = "StocksInWarehouse";

  public static final String ALS_COMPANY_NAME = "CompanyName";
  public static final String ALS_COMPANY_CODE = "CompanyCode";
  public static final String ALS_COMPANY_TYPE = "CompanyType";
  public static final String ALS_COMPANY_TYPE_NAME = "TypeName";

  public static final String ALS_CONTACT_FIRST_NAME = "ContactFirstName";
  public static final String ALS_CONTACT_LAST_NAME = "ContactLastName";
  public static final String ALS_CONTACT_PERSON = "ContactPerson";
  public static final String ALS_CONTACT_COMPANY_NAME = "ContactCompanyName";
  public static final String ALS_CONTACT_COMPANY_TYPE_NAME = "ContactCompanyTypeName";

  public static final String ALS_CITY_NAME = "CityName";
  public static final String ALS_COUNTRY_NAME = "CountryName";

  public static final String ALS_POSITION_NAME = "PositionName";
  public static final String ALS_PRIMARY_POSITION = "PrimaryPosition";
  public static final String ALS_PRIMARY_POSITION_NAME = "PrimaryPositionName";

  public static final String ALS_EMAIL_ID = "EmailId";

  public static final String ALS_CATEGORY_PARENT_NAME = "ParentName";

  public static final String ALS_BANK_NAME = "BankName";

  public static final String ALS_ITEM_NAME = "ItemName";
  public static final String ALS_ITEM_VAT = "ItemVat";
  public static final String ALS_ITEM_VAT_PERCENT = "ItemVatPercent";

  public static final String ALS_UNIT_NAME = "UnitName";

  public static final String ALS_WAREHOUSE_CODE = "WarehouseCode";

  public static final String ALS_PARENT_TYPE_NAME = "ParentTypeName";
  public static final String ALS_ITEM_TYPE_NAME = "ItemTypeName";
  public static final String ALS_PARENT_GROUP_NAME = "ParentGroupName";
  public static final String ALS_ITEM_GROUP_NAME = "ItemGroupName";

  public static final String ALS_ITEM_TYPES = "ItemTypes";
  public static final String ALS_ITEM_GROUPS = "ItemGroups";

  public static final String ALS_ITEM_SUPPLIER_NAME = "SupplierName";
  public static final String ALS_ITEM_MANUFACTURER_NAME = "ManufacturerName";

  public static final String PROP_COMPANY_NAMES = "CompanyNames";
  public static final String PROP_COMPANY_IDS = "CompanyIds";
  public static final String PROP_WAREHOUSE_REMAINDER = "WarehouseRemainder";

  public static final String FILTER_COMPANY_CREATION_AND_TYPE = "company_creation_and_type";
  public static final String FILTER_COMPANY_USAGE = "company_usage";

  public static final String QR_FLOW_PANEL = "qrFlowPanel";
  public static final String QR_TYPE = "qrType";
  public static final String QR_COMPANY = "qrCompany";
  public static final String QR_PERSON = "qrPerson";

  public static final long DEFAULT_REMIND_ACTIONS_TIMER_TIMEOUT = TimeUtils.MILLIS_PER_HOUR;
  public static final long DEFAULT_REMIND_TASKS_TIMER_TIMEOUT = TimeUtils.MILLIS_PER_MINUTE;

  public static void register() {
    EnumUtils.register(ItemPrice.class);
  }

  private ClassifierConstants() {
  }
}
