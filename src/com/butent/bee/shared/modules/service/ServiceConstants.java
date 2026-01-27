package com.butent.bee.shared.modules.service;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class ServiceConstants {

  public enum ServiceCompanyKind implements HasLocalizedCaption {
    CUSTOMER {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.customer();
      }
    },
    CONTRACTOR {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.svcContractor();
      }
    };

    public static final ServiceCompanyKind DETAULT = CUSTOMER;
  }

  public static final String PRM_CLIENT_CHANGING_SETTING = "ClientChangingSetting";
  public static final String PRM_DEFAULT_MAINTENANCE_TYPE = "DefaultMaintenanceType";
  public static final String PRM_DEFAULT_WARRANTY_TYPE = "DefaultWarrantyType";
  public static final String PRM_FILTER_ALL_DEVICES = "FilterAllDevices";
  public static final String PRM_ITEM_ARTICLE_SOURCE_COLUMN = "ItemArticleSourceColumn";
  public static final String PRM_ITEM_NOTE_SOURCE_COLUMNS = "ItemNoteSourceColumns";
  public static final String PRM_URGENT_RATE = "UrgentRate";
  public static final String PRM_SERVICE_MANAGER_WAREHOUSE = "ServiceManagerWarehouse";
  public static final String PRM_SMS_REQUEST_SERVICE_ADDRESS = "SmsRequestServiceAddress";
  public static final String PRM_SMS_REQUEST_SERVICE_UID = "SmsRequestServiceUid";
  public static final String PRM_SMS_REQUEST_SERVICE_APIKEY = "SmsRequestServiceApiKey";
  public static final String PRM_SMS_REQUEST_SERVICE_FROM = "SmsRequestServiceFrom";
  public static final String PRM_EXTERNAL_MAINTENANCE_URL = "ExternalMaintenanceUrl";
  public static final String PRM_SMS_REQUEST_CONTACT_INFO_FROM = "SmsRequestContactInfoFrom";
  public static final String PRM_ROLE = "Role";

  public static final String SVC_CREATE_INVOICE_ITEMS = "CreateInvoiceItems";
  public static final String SVC_CREATE_DEFECT_ITEMS = "CreateDefectItems";
  public static final String SVC_GET_CALENDAR_DATA = "getServiceCalendarData";
  public static final String SVC_COPY_DOCUMENT_CRITERIA = "CopyDocumentCriteria";
  public static final String SVC_UPDATE_SERVICE_MAINTENANCE_OBJECT
          = "update_service_maintenance_object";
  public static final String SVC_INFORM_CUSTOMER = "inform_customer";
  public static final String SVC_GET_MAINTENANCE_NEW_ROW_VALUES = "getMaintenanceNewRowValues";
  public static final String SVC_CREATE_RESERVATION_INVOICE_ITEMS = "CreateReservationInvoiceItems";
  public static final String SVC_GET_ITEMS_INFO = "getItemsInfo";
  public static final String SVC_GET_REPAIRER_TARIFF = "getRepairerTariff";

  public static final String SVC_SERVICE_PAYROLL_REPORT = "ServicePayrollReport";

  public static final String TBL_EQUIPMENT = "Equipment";
  public static final String TBL_SERVICE_TREE = "ServiceTree";
  public static final String TBL_SERVICE_OBJECTS = "ServiceObjects";
  public static final String TBL_MAINTENANCE = "Maintenance";
  public static final String TBL_MAINTENANCE_COMMENTS = "MaintenanceComments";
  public static final String TBL_MAINTENANCE_PAYROLL = "MaintenancePayroll";
  public static final String TBL_MAINTENANCE_TARIFFS = "MaintenanceTariffs";
  public static final String TBL_MAINTENANCE_TYPES = "MaintenanceTypes";
  public static final String TBL_SERVICE_DATES = "ServiceDates";
  public static final String TBL_SERVICE_ITEMS = "ServiceItems";
  public static final String TBL_SERVICE_SETTINGS = "ServiceSettings";
  public static final String TBL_SERVICE_DEFECT_ITEMS = "ServiceDefectItems";
  public static final String TBL_SERVICE_CRITERIA_GROUPS = "ServiceCritGroups";
  public static final String TBL_SERVICE_CRITERIA = "ServiceCriteria";
  public static final String TBL_SERVICE_MAINTENANCE = "ServiceMaintenance";
  public static final String TBL_STATE_PROCESS = "StateProcess";
  public static final String TBL_WARRANTY_TYPES = "WarrantyTypes";

  public static final String VIEW_SERVICE_OBJECTS = "ServiceObjects";

  public static final String VIEW_SERVICE_CRITERIA_GROUPS = "ServiceCritGroups";
  public static final String VIEW_SERVICE_CRITERIA = "ServiceCriteria";

  public static final String VIEW_SERVICE_DISTINCT_CRITERIA = "ServiceDistinctCriteria";
  public static final String VIEW_SERVICE_DISTINCT_VALUES = "ServiceDistinctCritValues";

  public static final String VIEW_SERVICE_OBJECT_CRITERIA = "ServiceObjectCriteria";

  public static final String VIEW_SERVICE_FILES = "ServiceFiles";
  public static final String VIEW_SERVICE_DATES = "ServiceDates";

  public static final String VIEW_MAINTENANCE = "Maintenance";
  public static final String VIEW_MAINTENANCE_STATES = "MaintenanceStates";
  public static final String VIEW_MAINTENANCE_TYPES = "MaintenanceTypes";
  public static final String VIEW_SERVICE_INVOICES = "ServiceInvoices";
  public static final String VIEW_SERVICE_DEFECTS = "ServiceDefects";
  public static final String VIEW_SERVICE_DEFECT_ITEMS = "ServiceDefectItems";

  public static final String VIEW_SERVICE_SALES = "ServiceSales";
  public static final String VIEW_SERVICE_SETTINGS = "ServiceSettings";

  public static final String COL_ADDRESS_REQUIRED = "AddressRequired";
  public static final String COL_ARTICLE_NO = "ArticleNo";
  public static final String COL_CREATOR = "Creator";
  public static final String COL_COMMENT = "Comment";
  public static final String COL_CUSTOMER_SENT = "CustomerSent";
  public static final String COL_DAYS_ACTIVE = "DaysActive";
  public static final String COL_ENDING_DATE = "EndingDate";
  public static final String COL_EVENT_NOTE = "EventNote";
  public static final String COL_EQUIPMENT = "Equipment";
  public static final String COL_EQUIPMENT_NAME = "Name";
  public static final String COL_INITIAL = "Initial";
  public static final String COL_IS_ITEMS_REQUIRED = "IsItemsRequired";
  public static final String COL_FINITE = "Finite";
  public static final String COL_TYPE = "Type";
  public static final String COL_MAINTENANCE_NUMBER = "MaintenanceNumber";
  public static final String COL_MAINTENANCE_STATE = "MaintenanceState";
  public static final String COL_MESSAGE = "Message";
  public static final String COL_NOTIFY_CUSTOMER = "NotifyCustomer";
  public static final String COL_PROHIBIT_EDIT = "ProhibitEdit";
  public static final String COL_REPAIRER = "Repairer";
  public static final String COL_SHOW_CUSTOMER = "ShowCustomer";
  public static final String COL_SEND_EMAIL = "SentEmail";
  public static final String COL_SEND_SMS = "SentSms";
  public static final String COL_SERVICE_CATEGORY = "Category";
  public static final String COL_SERVICE_ADDRESS = "Address";
  public static final String COL_SERVICE_CUSTOMER = "Customer";
  public static final String COL_SERVICE_CONTRACTOR = "Contractor";

  public static final String COL_PAYROLL_BASIC_AMOUNT = "BasicAmount";
  public static final String COL_PAYROLL_CONFIRMED = "Confirmed";
  public static final String COL_PAYROLL_CONFIRMED_USER = "ConfirmedUser";
  public static final String COL_PAYROLL_CONFIRMATION_DATE = "ConfirmationDate";
  public static final String COL_PAYROLL_DATE = "PayrollDate";
  public static final String COL_PAYROLL_SALARY = "Salary";
  public static final String COL_PAYROLL_TARIFF = "Tariff";

  public static final String COL_SERVICE_CRITERIA_GROUP = "Group";
  public static final String COL_SERVICE_CRITERIA_GROUP_NAME = "Name";
  public static final String COL_SERVICE_CRITERION_NAME = "Criterion";
  public static final String COL_SERVICE_CRITERION_VALUE = "Value";
  public static final String COL_SERVICE_CRITERIA_ORDINAL = "Ordinal";

  public static final String COL_SERVICE_OBJECT = "ServiceObject";

  public static final String COL_SERVICE_CATEGORY_NAME = "Name";
  public static final String COL_STATE_COMMENT = "StateComment";
  public static final String COL_STATE_NAME = "Name";

  public static final String COL_MANUFACTURER = "ServiceManufacturer";
  public static final String COL_MAINTENANCE_DATE = "Date";
  public static final String COL_MAINTENANCE_ITEM = "Item";
  public static final String COL_MAINTENANCE_INVOICE = "Invoice";
  public static final String COL_MAINTENANCE_DEFECT = "Defect";
  public static final String COL_MAINTENANCE_DESCRIPTION = "Description";
  public static final String COL_MAINTENANCE_NOTES = "Notes";
  public static final String COL_MAINTENANCE_TYPE = "MaintenanceType";
  public static final String COL_MAINTENANCE_URGENT = "Urgent";
  public static final String COL_MODEL = "Model";
  public static final String COL_PUBLISH_TIME = "PublishTime";
  public static final String COL_SERIAL_NO = "SerialNo";
  public static final String COL_SERVICE_MAINTENANCE = "ServiceMaintenance";
  public static final String COL_SERVICE_MAINTENANCE_ID = "ServiceMaintenanceID";
  public static final String COL_SERVICE_ITEM = "ServiceItem";
  public static final String COL_SERVICE_DATE_FROM = "DateFrom";
  public static final String COL_SERVICE_DATE_UNTIL = "DateUntil";
  public static final String COL_SERVICE_DATE_COLOR = "Color";
  public static final String COL_SERVICE_DATE_NOTE = "Note";

  public static final String COL_SERVICE_CALENDAR_TASK_TYPES = "CalendarTaskTypes";
  public static final String COL_SERVICE_CALENDAR_MIN_DATE = "CalendarMinDate";
  public static final String COL_SERVICE_CALENDAR_MAX_DATE = "CalendarMaxDate";

  public static final String COL_DEFECT_SUPPLIER = "Supplier";
  public static final String COL_DEFECT_MANAGER = "Manager";

  public static final String COL_DEFECT = "Defect";
  public static final String COL_DEFECT_ITEM = "Item";
  public static final String COL_DEFECT_NOTE = "Note";

  public static final String COL_TYPE_NAME = "Name";
  public static final String COL_TERM = "Term";

  public static final String COL_WARRANTY_MAINTENANCE = "WarrantyMaintenance";
  public static final String COL_WARRANTY = "Warranty";
  public static final String COL_WARRANTY_DURATION = "Duration";
  public static final String COL_WARRANTY_NAME = "Name";
  public static final String COL_WARRANTY_TYPE = "WarrantyType";
  public static final String COL_WARRANTY_VALID_TO = "WarrantyValidTo";

  public static final String ALS_COMPANY_TYPE_NAME = "CompanyTypeName";
  public static final String ALS_CONTACT_ADDRESS = "ContactAddress";
  public static final String ALS_CONTACT_EMAIL = "ContactEmail";
  public static final String ALS_CONTACT_PHONE = "ContactPhone";

  public static final String ALS_CUSTOMER_TYPE_NAME = "CustomerTypeName";
  public static final String ALS_DEPARTMENT_NAME = "DepartmentName";
  public static final String ALS_SERVICE_CATEGORY_NAME = "CategoryName";
  public static final String ALS_SERVICE_CUSTOMER_NAME = "CustomerName";
  public static final String ALS_SERVICE_CONTRACTOR_NAME = "ContractorName";

  public static final String ALS_STATE_NAME = "StateName";
  public static final String ALS_STATE_TIME = "StateTime";

  public static final String ALS_MAINTENANCE_ITEM_NAME = "ItemName";
  public static final String ALS_MAINTENANCE_TYPE_NAME = "TypeName";

  public static final String ALS_MANUFACTURER_NAME = "ManufacturerName";

  public static final String ALS_DEFECT_SUPPLIER_NAME = "SupplierName";
  public static final String ALS_DEFECT_ITEM_NAME = "ItemName";
  public static final String ALS_DEFECT_UNIT_NAME = "UnitName";

  public static final String ALS_PUBLISHER_FIRST_NAME = "PublisherFirstName";
  public static final String ALS_PUBLISHER_LAST_NAME = "PublisherLastName";

  public static final String ALS_WARRANTY_TYPE_NAME = "WarrantyTypeName";

  public static final String FORM_MAINTENANCE_COMMENT = "MaintenanceComment";
  public static final String FORM_MAINTENANCE_STATE_COMMENT = "MaintenanceStateComment";

  public static final String GRID_OBJECT_INVOICES = "ObjectInvoices";
  public static final String GRID_OBJECT_DEFECTS = "ObjectDefects";
  public static final String GRID_SERVICE_INVOICES = "ServiceInvoices";
  public static final String GRID_SERVICE_MAINTENANCE = "ServiceMaintenance";
  public static final String GRID_SERVICE_MAINTENANCE_INVOICES = "ServiceMaintenanceInvoices";

  public static final String PROP_MAIN_ITEM = "MainItem";
  public static final String PROP_CRITERIA = "Criteria";
  public static final String PROP_SERVICE_MAINTENANCE_LATE = "Late";

  public static void register() {
    EnumUtils.register(ServiceCompanyKind.class);
  }

  private ServiceConstants() {
  }
}
