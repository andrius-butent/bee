package com.butent.bee.shared.modules.administration;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.modules.ParameterType;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.time.ScheduleDateMode;
import com.butent.bee.shared.time.WorkdayTransition;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

public final class AdministrationConstants {

  public static final List<BeeColumn> HISTORY_COLUMNS = Lists.newArrayList(
      new BeeColumn(ValueType.DATE_TIME, AdministrationConstants.AUDIT_FLD_TIME, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.COL_USER, true),
      new BeeColumn(ValueType.LONG, AdministrationConstants.AUDIT_FLD_TX, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.AUDIT_FLD_MODE, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.COL_OBJECT, false),
      new BeeColumn(ValueType.LONG, AdministrationConstants.AUDIT_FLD_ID, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.AUDIT_FLD_FIELD, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.AUDIT_FLD_VALUE, true),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.COL_RELATION, true));

  public enum ReminderMethod implements HasCaption {
    EMAIL, SMS;

    @Override
    public String getCaption() {
      return this.name().toLowerCase();
    }
  }

  public enum ReminderDateField implements HasLocalizedCaption {
    START_DATE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.startingDate();
      }
    },

    END_DATE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.endingDate();
      }
    }
  }

  public enum ReminderDateIndicator implements HasLocalizedCaption {
    BEFORE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.before();
      }
    },

    AFTER {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.after();
      }
    }
  }

  public enum UserGroupVisibility implements HasCaption {
    PRIVATE(Localized.dictionary().userGroupPrivate()),
    PUBLIC(Localized.dictionary().userGroupPublic());

    private final String caption;

    UserGroupVisibility(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public static void register() {
    EnumUtils.register(Feed.class);
    EnumUtils.register(ParameterType.class);
    EnumUtils.register(ReminderMethod.class);
    EnumUtils.register(RightsObjectType.class);
    EnumUtils.register(RightsState.class);
    EnumUtils.register(SupportedLocale.class);
    EnumUtils.register(UserGroupVisibility.class);
    EnumUtils.register(UserInterface.class);

    EnumUtils.register(ScheduleDateMode.class);
    EnumUtils.register(WorkdayTransition.class);

    EnumUtils.register(ImportType.class);
    EnumUtils.register(Module.class);
    EnumUtils.register(SysObject.class);

    EnumUtils.register(ReminderDateField.class);
    EnumUtils.register(ReminderDateIndicator.class);
  }

  public static final String FILE_URL = "file";
  public static final String FILE_COMMIT = "CommitFile";

  public static final String PARAMETERS_PREFIX = "parameters_";

  public static final String SVC_GET_RELATION_PARAMETER = PARAMETERS_PREFIX + "relation_parameter";
  public static final String SVC_SET_PARAMETER = PARAMETERS_PREFIX + "set";

  public static final String SVC_DO_IMPORT = "DoImport";

  public static final String SVC_GET_CONFIG_DIFF = "GetConfigDiff";
  public static final String SVC_GET_CONFIG_OBJECT = "GetConfigObject";
  public static final String SVC_GET_CONFIG_OBJECTS = "GetConfigObjects";

  public static final String SVC_TOTAL_TO_WORDS = "GetTotalInWords";

  public static final String SVC_GET_DICTIONARY = "get_dictionary";
  public static final String SVC_DICTIONARY_DATABASE_TO_PROPERTIES =
      "dictionary_database_to_properties";

  public static final String SVC_INIT_DIMENSION_NAMES = "init_dimension_names";

  public static final String VAR_AMOUNT = Service.RPC_VAR_PREFIX + "amount";
  public static final String VAR_LOCALE = Service.RPC_VAR_PREFIX + "locale";

  public static final String VAR_PARAMETERS_MODULE = Service.RPC_VAR_PREFIX + "module";
  public static final String VAR_PARAMETER_DEFAULT = "DefaultMode";

  public static final String SVC_GET_HISTORY = "get_history";
  public static final String VAR_HISTORY_VIEW = Service.RPC_VAR_PREFIX + "history_view";
  public static final String VAR_HISTORY_IDS = Service.RPC_VAR_PREFIX + "history_ids";

  public static final String VAR_DATE_LOW = Service.RPC_VAR_PREFIX + "date_low";
  public static final String VAR_DATE_HIGH = Service.RPC_VAR_PREFIX + "date_high";

  public static final String VAR_BASE_ROLE = Service.RPC_VAR_PREFIX + "base_role";

  public static final String SVC_GET_CURRENT_EXCHANGE_RATE = "get_current_exchange_rate";
  public static final String SVC_GET_EXCHANGE_RATE = "get_exchange_rate";
  public static final String SVC_GET_LIST_OF_CURRENCIES = "get_list_of_currencies";
  public static final String SVC_GET_EXCHANGE_RATES_FOR_CURRENCY =
      "get_exchange_rates_for_currency";

  public static final String SVC_UPDATE_EXCHANGE_RATES = "update_exchange_rates";

  public static final String SVC_BLOCK_HOST = "block_host";
  public static final String SVC_CREATE_USER = "create_user";
  public static final String SVC_COPY_RIGHTS = "copy_rights";
  public static final String SVC_CREATE_DATA_IMPORT_TEMPLATES = "create_data_import_templates";

  public static final String SVC_DO_SUBSTITUTION = "do_substitution";

  public static final String VAR_IMPORT_TEST = "Test";
  public static final String VAR_IMPORT_FILE = "File";
  public static final String VAR_IMPORT_SHEET = "Sheet";
  public static final String VAR_IMPORT_START_ROW = "StartRow";
  public static final String VAR_IMPORT_END_ROW = "EndRow";
  public static final String VAR_IMPORT_DATE_FORMAT = "DateFormat";

  public static final String VAR_IMPORT_LOGIN = "Login";
  public static final String VAR_IMPORT_PASSWORD = "Password";

  public static final String AUDIT_SUFFIX = "AUDIT";
  public static final String AUDIT_USER = "bee.user";
  public static final String AUDIT_FLD_TIME = "Time";
  public static final String AUDIT_FLD_USER = "UserId";
  public static final String AUDIT_FLD_TX = "TransactionId";
  public static final String AUDIT_FLD_MODE = "Mode";
  public static final String AUDIT_FLD_ID = "RecordId";
  public static final String AUDIT_FLD_FIELD = "Field";
  public static final String AUDIT_FLD_VALUE = "Value";

  public static final String TBL_PARAMETERS = "Parameters";
  public static final String TBL_USER_PARAMETERS = "UserParameters";

  public static final String TBL_USERS = "Users";
  public static final String TBL_USER_SETTINGS = "UserSettings";
  public static final String TBL_ROLES = "Roles";
  public static final String TBL_USER_ROLES = "UserRoles";
  public static final String TBL_OBJECTS = "Objects";
  public static final String TBL_RIGHTS = "Rights";
  public static final String TBL_USER_HISTORY = "UserHistory";
  public static final String TBL_USER_GROUPS = "UserGroups";

  public static final String TBL_FILES = "Files";
  public static final String TBL_FILE_PARTS = "FileParts";

  public static final String TBL_REMINDER_TYPES = "ReminderTypes";

  public static final String TBL_RELATIONS = "Relations";

  public static final String TBL_IP_FILTERS = "IpFilters";

  public static final String TBL_CURRENCIES = "Currencies";
  public static final String TBL_CURRENCY_RATES = "CurrencyRates";

  public static final String TBL_DEPARTMENTS = "Departments";
  public static final String TBL_DEPARTMENT_EMPLOYEES = "DepartmentEmployees";

  public static final String TBL_AUTOCOMPLETE = "Autocomplete";

  public static final String TBL_IMPORT_OPTIONS = "ImportOptions";
  public static final String TBL_IMPORT_PROPERTIES = "ImportProperties";
  public static final String TBL_IMPORT_MAPPINGS = "ImportMappings";
  public static final String TBL_IMPORT_CONDITIONS = "ImportConditions";

  public static final String TBL_STAGES = "Stages";
  public static final String TBL_STAGE_CONDITIONS = "StageConditions";
  public static final String TBL_STAGE_ACTIONS = "StageActions";
  public static final String TBL_STAGE_TRIGGERS = "StageTriggers";

  public static final String TBL_SUBSTITUTIONS = "Substitutions";
  public static final String TBL_SUBSTITUTION_OBJECTS = "SubstitutionObjects";
  public static final String TBL_SUBSTITUTION_REASONS = "SubstitutionReasons";

  public static final String TBL_CUSTOM_CONFIG = "CustomConfig";

  public static final String TBL_EVENT_HISTORY = "EventHistory";

  public static final String TBL_DICTIONARY = "Dictionary";

  public static final String VIEW_USERS = "Users";
  public static final String VIEW_USER_SETTINGS = "UserSettings";
  public static final String VIEW_USER_GROUP_SETTINGS = "UserGroupSettings";
  public static final String VIEW_USER_GROUP_MEMBERS = "UserGroupMembers";

  public static final String VIEW_REMINDER_TYPES = "ReminderTypes";

  public static final String VIEW_COLORS = "Colors";
  public static final String VIEW_THEMES = "Themes";
  public static final String VIEW_THEME_COLORS = "ThemeColors";

  public static final String VIEW_CURRENCIES = "Currencies";
  public static final String VIEW_CURRENCY_RATES = "CurrencyRates";

  public static final String VIEW_RELATIONS = "Relations";

  public static final String VIEW_IP_FILTERS = "IpFilters";

  public static final String VIEW_AUTOCOMPLETE = "Autocomplete";

  public static final String VIEW_ROLES = "Roles";
  public static final String VIEW_RIGHTS = "Rights";
  public static final String VIEW_USER_ROLES = "UserRoles";

  public static final String VIEW_DEPARTMENTS = "Departments";
  public static final String VIEW_DEPARTMENT_EMPLOYEES = "DepartmentEmployees";
  public static final String VIEW_DEPARTMENT_POSITIONS = "DepartmentPositions";

  public static final String VIEW_FILTERS = "Filters";

  public static final String VIEW_FAVORITES = "Favorites";
  public static final String VIEW_WORKSPACES = "Workspaces";
  public static final String VIEW_REPORT_SETTINGS = "ReportSettings";

  public static final String VIEW_UI_THEMES = "UiThemes";

  public static final String VIEW_USER_REMINDERS = "UserReminders";

  public static final String GRID_HISTORY = "History";
  public static final String GRID_USER_GROUP_MEMBERS = "UserGroupMembers";

  public static final String GRID_ROLE_USERS = "RoleUsers";

  public static final String GRID_COLORS = "Colors";
  public static final String GRID_THEMES = "Themes";
  public static final String GRID_THEME_COLORS = "ThemeColors";

  public static final String GRID_DICTIONARY = "Dictionary";

  public static final String COL_PARAMETER = "Parameter";
  public static final String COL_PARAMETER_NAME = "Name";
  public static final String COL_PARAMETER_VALUE = "Value";

  public static final String COL_RELATION = "Relation";

  public static final String COL_LOGIN = "Login";
  public static final String COL_PASSWORD = "Password";
  public static final String COL_USER_LOCALE = "Locale";
  public static final String COL_USER_INTERFACE = "Interface";
  public static final String COL_USER_DATE_FORMAT = "DateFormat";
  public static final String COL_USER_EULA_AGREEMENT = "EulaAgreement";
  public static final String COL_USER_BLOCK_FROM = "BlockAfter";
  public static final String COL_USER_BLOCK_UNTIL = "BlockBefore";
  public static final String COL_REMOTE_HOST = "Host";
  public static final String COL_USER_AGENT = "Agent";
  public static final String COL_LOGGED_IN = "LoggedIn";
  public static final String COL_LOGGED_OUT = "LoggedOut";

  public static final String COL_ROLE_NAME = "Name";
  public static final String COL_USER = "User";
  public static final String COL_ROLE = "Role";

  public static final String COL_OBJECT_TYPE = "Type";
  public static final String COL_OBJECT = "Object";
  public static final String COL_OBJECT_NAME = "Name";
  public static final String COL_STATE = "State";
  public static final String COL_OBJECT_ID = "ObjectID";

  public static final String COL_FILE = "File";
  public static final String COL_FILE_NAME = "Name";
  public static final String COL_FILE_HASH = "Hash";
  public static final String COL_FILE_REPO = "Repository";
  public static final String COL_FILE_SIZE = "Size";
  public static final String COL_FILE_TYPE = "Type";
  public static final String COL_FILE_PART = "Part";

  public static final String COL_FILE_CAPTION = "Caption";

  public static final String COL_REMINDER_NAME = "Name";
  public static final String COL_REMINDER_METHOD = "Method";
  public static final String COL_REMINDER_HOURS = "Hours";
  public static final String COL_REMINDER_MINUTES = "Minutes";
  public static final String COL_REMINDER_TEMPLATE_CAPTION = "Caption";
  public static final String COL_REMINDER_TEMPLATE = "Template";
  public static final String COL_REMINDER_MODULE = "Module";
  public static final String COL_REMINDER_DATA_FIELD = "ReminderDateField";
  public static final String COL_REMINDER_DATA_INDICATOR = "ReminderDateIndicator";

  public static final String COL_COLOR = "Color";
  public static final String COL_COLOR_NAME = "Name";
  public static final String COL_DEFAULT_COLOR = "DefaultColor";
  public static final String COL_BACKGROUND = "Background";
  public static final String COL_FOREGROUND = "Foreground";

  public static final String COL_THEME = "Theme";

  public static final String COL_FAVORITE_USER = "User";

  public static final String COL_FILTER_USER = "User";
  public static final String COL_FILTER_KEY = "Key";
  public static final String COL_FILTER_ORDINAL = "Ordinal";

  public static final String COL_CURRENCY = "Currency";
  public static final String COL_CURRENCY_NAME = "Name";
  public static final String COL_CURRENCY_MINOR_NAME = "MinorName";
  public static final String COL_CURRENCY_UPDATE_TAG = "UpdateTag";

  public static final String COL_CURRENCY_RATE_CURRENCY = "Currency";
  public static final String COL_CURRENCY_RATE_DATE = "Date";
  public static final String COL_CURRENCY_RATE_QUANTITY = "Quantity";
  public static final String COL_CURRENCY_RATE = "Rate";

  public static final String COL_IP_FILTER_HOST = "BlockHost";
  public static final String COL_IP_FILTER_BLOCK_AFTER = "BlockAfter";
  public static final String COL_IP_FILTER_BLOCK_BEFORE = "BlockBefore";

  public static final String COL_AUTOCOMPLETE_USER = "User";
  public static final String COL_AUTOCOMPLETE_KEY = "Key";
  public static final String COL_AUTOCOMPLETE_VALUE = "Value";

  public static final String COL_USER_GROUP_SETTINGS_NAME = "Name";
  public static final String COL_USER_GROUP_SETTINGS_OWNER = "Owner";
  public static final String COL_USER_GROUP_SETTINGS_VISIBILITY = "Visibility";

  public static final String COL_UG_GROUP = "Group";
  public static final String COL_UG_USER = "User";

  public static final String COL_DEPARTMENT = "Department";
  public static final String COL_DEPARTMENT_NAME = "DepartmentName";
  public static final String COL_DEPARTMENT_PARENT = "Parent";
  public static final String COL_DEPARTMENT_HEAD = "DepartmentHead";
  public static final String COL_DEPARTMENT_POSITION_NUMBER_OF_EMPLOYEES = "NumberOfEmployees";

  public static final String COL_RS_REPORT = "Report";
  public static final String COL_RS_USER = "User";
  public static final String COL_RS_CAPTION = "Caption";
  public static final String COL_RS_PARAMETERS = "Parameters";

  public static final String COL_OPEN_IN_NEW_TAB = "OpenInNewTab";
  public static final String COL_WORKSPACE_CONTINUE = "WorkspaceContinue";
  public static final String COL_SHOW_NEW_MESSAGES_NOTIFIER = "ShowNewMessagesNotifier";
  public static final String COL_ASSISTANT = "Assistant";
  public static final String COL_LAST_WORKSPACE = "LastWorkspace";

  public static final String COL_CLICK_SENSITIVITY_MILLIS = "ClickSensitivityMillis";
  public static final String COL_CLICK_SENSITIVITY_DISTANCE = "ClickSensitivityDistance";
  public static final String COL_ACTION_SENSITIVITY_MILLIS = "ActionSensitivityMillis";

  public static final String COL_NEWS_REFRESH_INTERVAL_SECONDS = "NewsRefreshIntervalSeconds";
  public static final String COL_LOADING_STATE_DELAY_MILLIS = "LoadingStateDelayMillis";

  public static final String COL_SHOW_GRID_FILTER_COMMAND = "showGridFilterCommand";

  public static final String COL_GRID_EDIT_WINDOW = "GridEditWindow";
  public static final String COL_GRID_NEW_ROW_WINDOW = "GridNewRowWindow";
  public static final String COL_CHILD_EDIT_WINDOW = "ChildEditWindow";
  public static final String COL_CHILD_NEW_ROW_WINDOW = "ChildNewRowWindow";
  public static final String COL_RELATION_EDIT_WINDOW = "RelationEditWindow";
  public static final String COL_RELATION_NEW_ROW_WINDOW = "RelationNewRowWindow";
  public static final String COL_OTHER_EDIT_WINDOWS = "OtherEditWindows";
  public static final String COL_OTHER_NEW_ROW_WINDOWS = "OtherNewRowWindows";
  public static final String COL_NEW_MAIL_MESSAGE_WINDOW = "NewMailMessageWindow";

  public static final String COL_UI_THEME = "UiTheme";
  public static final String COL_USER_STYLE = "Style";

  public static final String COL_IMPORT_OPTION = "Option";
  public static final String COL_IMPORT_TYPE = "Type";
  public static final String COL_IMPORT_DATA = "Data";
  public static final String COL_IMPORT_DESCRIPTION = "Description";
  public static final String COL_IMPORT_PROPERTY = "Property";
  public static final String COL_IMPORT_VALUE = "Value";
  public static final String COL_IMPORT_RELATION_OPTION = "RelationOption";
  public static final String COL_IMPORT_MAPPING = "Mapping";

  public static final String COL_STAGE = "Stage";
  public static final String COL_STAGE_VIEW = "ViewName";
  public static final String COL_STAGE_NAME = "StageName";
  public static final String COL_STAGE_CONFIRM = "StageConfirm";

  public static final String COL_STAGE_FIELD = "Field";
  public static final String COL_STAGE_OPERATOR = "Operator";
  public static final String COL_STAGE_VALUE = "Value";
  public static final String COL_STAGE_FROM = "StageFrom";
  public static final String COL_STAGE_ACTION = "Action";
  public static final String COL_STAGE_TRIGGER = "Trigger";

  public static final String COL_CONFIG_MODULE = "ObjectModule";
  public static final String COL_CONFIG_TYPE = "ObjectType";
  public static final String COL_CONFIG_OBJECT = "ObjectName";
  public static final String COL_CONFIG_DATA = "ObjectData";

  public static final String COL_DEFAULT_VAT = "DefaultVat";

  public static final String COL_MENU_HIDE = "MenuHide";
  public static final String COL_COMMENTS_LAYOUT = "CommentsLayout";

  public static final String COL_EVENT = "Event";
  public static final String COL_EVENT_STARTED = "Started";
  public static final String COL_EVENT_ENDED = "Ended";
  public static final String COL_EVENT_RESULT = "Result";

  public static final String COL_SUBSTITUTE = "Substitute";
  public static final String COL_SUBSTITUTE_FROM = "SubstituteFrom";
  public static final String COL_SUBSTITUTION = "Substitution";
  public static final String COL_SUBSTITUTION_REASON = "SubstitutionReason";
  public static final String COL_SUBSTITUTION_REASON_NAME = "SubstitutionReasonName";
  public static final String COL_SUBSTITUTION_EXECUTED = "Executed";

  public static final String COL_DICTIONARY_KEY = "Key";

  public static final String COL_USER_REMINDER_OBJECT = "Object";
  public static final String COL_USER_REMINDER_OBJECT_MODULE = "ObjectModule";
  public static final String COL_USER_REMINDER_USER = "User";
  public static final String COL_USER_REMINDER_TYPE = "ReminderType";
  public static final String COL_USER_REMINDER_ACTIVE = "Active";
  public static final String COL_USER_REMINDER_TIMEOUT = "Timeout";
  public static final String COL_USER_REMINDER_TIME = "ReminderTime";

  public static final String ALS_FILE_NAME = "FileName";
  public static final String ALS_FILE_SIZE = "FileSize";
  public static final String ALS_FILE_TYPE = "FileType";

  public static final String ALS_COLOR_NAME = "ColorName";
  public static final String ALS_DEFAULT_COLOR_NAME = "DefaultColorName";
  public static final String ALS_DEFAULT_BACKGROUND = "DefaultBackground";
  public static final String ALS_DEFAULT_FOREGROUND = "DefaultForeground";

  public static final String ALS_OBJECT_TYPE = "ObjectType";
  public static final String ALS_OBJECT_NAME = "ObjectName";

  public static final String ALS_CURRENCY_NAME = "CurrencyName";

  public static final String ALS_ROLE_NAME = "RoleName";

  public static final String ALS_DEPARTMENT_PARENT_NAME = "ParentName";

  public static final String FORM_USER = "User";
  public static final String FORM_USER_SETTINGS = "UserSettings";
  public static final String FORM_DEPARTMENT = "Department";
  public static final String FORM_COMPANY_STRUCTURE = "CompanyStructure";
  public static final String FORM_NEW_ROLE = "NewRole";

  public static final String FORM_IMPORT_MAPPINGS = "ImportOptionMappings";
  public static final String FORM_IMPORT_OPTION = "ImportOption";

  public static final String FORM_DEPENDENCY_RIGHTS = "DependencyRights";
  public static final String FORM_STAGES = "StageEditor";

  public static final String FORM_SUBSTITUTION = "Substitution";

  public static final String PRM_SQL_MESSAGES = "SQLMessages";

  public static final String PRM_SERVER_PROPERTIES = "ServerProperties";
  public static final String PRM_COMPANY = "CompanyName";
  public static final String PRM_COUNTRY = "Country";
  public static final String PRM_CURRENCY = "MainCurrency";
  public static final String PRM_VAT_PERCENT = "VATPercent";
  public static final String PRM_REFRESH_CURRENCY_HOURS = "CurrencyRefreshHours";

  public static final String PRM_ERP_ADDRESS = "ERPAddress";
  public static final String PRM_ERP_LOGIN = "ERPLogin";
  public static final String PRM_ERP_PASSWORD = "ERPPassword";

  public static final String PRM_TASK_MAIL_OBSERVERS_GROUP = "TaskMailObserversGroup";

  public static final String PRM_RECORD_DEPENDENCY = "RecordDependency";

  public static final String PRM_WS_LB_EXCHANGE_RATES_ADDRESS = "WSLBExchangeRates";

  public static final String PRM_URL = "Url";

  public static final String PROP_ICON = "Icon";

  public static final String PROP_DEPARTMENT_FULL_NAME = "FullName";
  public static final char DEPARTMENT_NAME_SEPARATOR = '\n';

  private AdministrationConstants() {
  }
}
