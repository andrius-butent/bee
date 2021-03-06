package com.butent.bee.shared.modules.documents;

public final class DocumentConstants {

  public static final String SVC_COPY_DOCUMENT_DATA = "copy_document_data";
  public static final String SVC_SET_CATEGORY_STATE = "set_category_state";

  public static final String PRM_PRINT_AS_PDF = "PrintAsPDF";
  public static final String PRM_PRINT_SIZE = "PrintPageSize";
  public static final String PRM_PRINT_HEADER = "PrintPageHeader";
  public static final String PRM_PRINT_FOOTER = "PrintPageFooter";
  public static final String PRM_PRINT_MARGINS = "PrintPageMargins";

  public static final String TBL_DOCUMENT_TREE = "DocumentTree";
  public static final String TBL_TREE_PREFIXES = "TreePrefixes";
  public static final String TBL_DOCUMENTS = "Documents";
  public static final String TBL_DOCUMENT_DATA = "DocumentData";
  public static final String TBL_CRITERIA_GROUPS = "CriteriaGroups";
  public static final String TBL_CRITERIA = "Criteria";

  public static final String TBL_EDITOR_TEMPLATES = "EditorTemplates";

  public static final String VIEW_DOCUMENTS = "Documents";
  public static final String VIEW_DOCUMENT_FILES = "DocumentFiles";
  public static final String VIEW_RELATED_DOCUMENTS = "RelatedDocuments";

  public static final String VIEW_DOCUMENT_TEMPLATES = "DocumentTemplates";
  public static final String VIEW_DATA_CRITERIA = "DataCriteria";
  public static final String VIEW_DOCUMENT_ITEMS = "DocumentItems";
  public static final String VIEW_DOCUMENT_DATA = "DocumentData";
  public static final String VIEW_CRITERIA_GROUPS = "CriteriaGroups";
  public static final String VIEW_CRITERIA = "Criteria";
  public static final String VIEW_DOCUMENT_TYPES = "DocumentTypes";
  public static final String VIEW_DOCUMENT_REMINDERS = "DocumentReminders";

  public static final String COL_CATEGORY_NAME = "Name";

  public static final String COL_DOCUMENT = "Document";
  public static final String COL_DOCUMENT_NAME = "Name";
  public static final String COL_DOCUMENT_DATE = "DocumentDate";
  public static final String COL_DOCUMENT_COUNT = "DocumentCount";
  public static final String COL_DOCUMENT_COMPANY = "Company";
  public static final String COL_DOCUMENT_CATEGORY = "Category";
  public static final String COL_DOCUMENT_TYPE = "Type";
  public static final String COL_DOCUMENT_PLACE = "Place";
  public static final String COL_DOCUMENT_RECEIVED = "Received";
  public static final String COL_DOCUMENT_SENT = "Sent";
  public static final String COL_DOCUMENT_SENT_NUMBER = "SentNumber";
  public static final String COL_DOCUMENT_RECEIVED_NUMBER = "ReceivedNumber";
  public static final String COL_DOCUMENT_STATUS = "Status";
  public static final String COL_DOCUMENT_EXPIRES = "Expires";
  public static final String COL_DOCUMENT_USER = "User";

  public static final String COL_DOCUMENT_REMINDER_ISTASK = "IsTask";
  public static final String COL_DOCUMENT_REMINDER_USER = "User";
  public static final String COL_DOCUMENT_REMINDER_TASK_TEMPLATE = "TaskTemplate";
  public static final String COL_DOCUMENT_REMINDER_EXECUTORS = "Executors";
  public static final String COL_DOCUMENT_REMINDER_DATE = "ReminderDate";
  public static final String COL_DOCUMENT_REMINDER_ACTIVE = "Active";
  public static final String COL_DOCUMENT_REMINDER_USER_DATE = "UserDate";

  public static final String COL_DOCUMENT_TEMPLATE = "Template";
  public static final String COL_DOCUMENT_TEMPLATE_NAME = "Name";
  public static final String COL_DOCUMENT_DATA = "Data";
  public static final String COL_DOCUMENT_CONTENT = "Content";
  public static final String COL_CRITERIA_GROUP = "Group";
  public static final String COL_CRITERIA_GROUP_NAME = "Name";
  public static final String COL_CRITERION_NAME = "Criterion";
  public static final String COL_CRITERION_VALUE = "Value";
  public static final String COL_CRITERIA_ORDINAL = "Ordinal";

  public static final String COL_EDITOR_TEMPLATE_ORIGIN = "Origin";
  public static final String COL_EDITOR_TEMPLATE_NAME = "Name";
  public static final String COL_EDITOR_TEMPLATE_CONTENT = "Content";

  public static final String COL_DOCUMENT_NUMBER = "Number";
  public static final String COL_NUMBER_PREFIX = "NumberPrefix";
  public static final String COL_REGISTRATION_NUMBER = "RegistrationNumber";
  public static final String COL_DESCRIPTION = "Description";

  public static final String COL_FILE_DATE = "FileDate";
  public static final String COL_FILE_VERSION = "FileVersion";
  public static final String COL_FILE_CAPTION = "Caption";
  public static final String COL_FILE_DESCRIPTION = "Description";
  public static final String COL_FILE_COMMENT = "Comment";
  public static final String COL_FILE_OWNER_FIRST_NAME = "OwnerFirstName";
  public static final String COL_FILE_OWNER_LAST_NAME = "OwnerLastName";
  public static final String COL_DOCUMENT_TYPE_NAME = "Name";

  public static final String ALS_CATEGORY_NAME = "CategoryName";
  public static final String ALS_TYPE_NAME = "TypeName";
  public static final String ALS_PLACE_NAME = "PlaceName";
  public static final String ALS_STATUS_NAME = "StatusName";
  public static final String ALS_DOCUMENT_COMPANY_NAME = "CompanyName";

  public static final String FORM_DOCUMENT = "Document";

  public static final int DOCUMENT_EXPIRATION_MIN_DAYS = 5;

  private DocumentConstants() {
  }
}
