package com.butent.bee.shared.modules.payroll;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class PayrollConstants {

  public enum ObjectStatus implements HasLocalizedCaption {
    INACTIVE {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.objectStatusInactive();
      }
    },
    ACTIVE {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.objectStatusActive();
      }
    }
  }

  public enum WorkScheduleKind implements HasLocalizedCaption {
    PLANNED {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.workSchedulePlannedShort();
      }

      @Override
      public String getClearDataQuestion(Dictionary dictionary) {
        return dictionary.clearWorkScheduleQuestion();
      }

      @Override
      public String getStorageKeyPrefix() {
        return "WorkSchedule";
      }

      @Override
      public String getTccColumnName() {
        return COL_TC_WS_PLANNED;
      }

      @Override
      public boolean isExtensionEnabled() {
        return true;
      }

      @Override
      public boolean isSubstitutionEnabled() {
        return false;
      }
    },

    ACTUAL {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.workScheduleActualShort();
      }

      @Override
      public String getClearDataQuestion(Dictionary dictionary) {
        return dictionary.clearTimeSheetQuestion();
      }

      @Override
      public String getStorageKeyPrefix() {
        return "TimeSheet";
      }

      @Override
      public String getTccColumnName() {
        return COL_TC_WS_ACTUAL;
      }

      @Override
      public boolean isExtensionEnabled() {
        return false;
      }

      @Override
      public boolean isSubstitutionEnabled() {
        return true;
      }
    };

    public abstract String getClearDataQuestion(Dictionary dictionary);

    public abstract String getStorageKeyPrefix();

    public String getStyleSuffix() {
      return name().toLowerCase();
    }

    public abstract String getTccColumnName();

    public abstract boolean isExtensionEnabled();

    public abstract boolean isSubstitutionEnabled();
  }

  public static final String SVC_GET_SCHEDULE_OVERLAP = "getScheduleOverlap";
  public static final String SVC_GET_SCHEDULED_MONTHS = "getScheduledMonths";
  public static final String SVC_GET_EARNINGS = "getEarnings";

  public static final String TBL_EMPLOYEES = "Employees";
  public static final String TBL_LOCATIONS = "Locations";
  public static final String TBL_EMPLOYEE_OBJECTS = "EmployeeObjects";

  public static final String TBL_WORK_SCHEDULE = "WorkSchedule";

  public static final String TBL_OBJECT_SALARY_FUND = "ObjectSalaryFund";

  public static final String TBL_TIME_CARD_CODES = "TimeCardCodes";
  public static final String TBL_TIME_RANGES = "TimeRanges";

  public static final String VIEW_EMPLOYEES = "Employees";
  public static final String VIEW_LOCATIONS = "Locations";
  public static final String VIEW_EMPLOYEE_OBJECTS = "EmployeeObjects";

  public static final String VIEW_WORK_SCHEDULE = "WorkSchedule";

  public static final String VIEW_TIME_CARD_CHANGES = "TimeCardChanges";

  public static final String VIEW_TIME_CARD_CODES = "TimeCardCodes";
  public static final String VIEW_TIME_RANGES = "TimeRanges";

  public static final String COL_TAB_NUMBER = "TabNumber";
  public static final String COL_SALARY = "Salary";
  public static final String COL_PART_TIME = "PartTime";

  public static final String COL_LOCATION_ADDRESS = "Address";
  public static final String COL_LOCATION_NAME = "LocationName";
  public static final String COL_LOCATION_MANAGER = "Manager";
  public static final String COL_LOCATION_STATUS = "Status";

  public static final String COL_EMPLOYEE = "Employee";
  public static final String COL_PAYROLL_OBJECT = "Object";

  public static final String COL_EMPLOYEE_OBJECT_FROM = "DateFrom";
  public static final String COL_EMPLOYEE_OBJECT_UNTIL = "DateUntil";
  public static final String COL_EMPLOYEE_OBJECT_FUND = "Fund";
  public static final String COL_EMPLOYEE_OBJECT_NOTE = "Note";

  public static final String COL_SUBSTITUTE_FOR = "SubstituteFor";
  public static final String COL_WAGE = "Wage";

  public static final String COL_TIME_CARD_CHANGES_FROM = "DateFrom";
  public static final String COL_TIME_CARD_CHANGES_UNTIL = "DateUntil";

  public static final String COL_DATE_OF_EMPLOYMENT = "DateOfEmployment";
  public static final String COL_DATE_OF_DISMISSAL = "DateOfDismissal";
  public static final String COL_DEPARTMENT = "Department";

  public static final String COL_WORK_SCHEDULE_KIND = "Kind";
  public static final String COL_WORK_SCHEDULE_DATE = "Date";
  public static final String COL_WORK_SCHEDULE_FROM = "TimeFrom";
  public static final String COL_WORK_SCHEDULE_UNTIL = "TimeUntil";
  public static final String COL_WORK_SCHEDULE_DURATION = "Duration";
  public static final String COL_WORK_SCHEDULE_NOTE = "Note";

  public static final String COL_TIME_RANGE_CODE = "TimeRangeCode";
  public static final String COL_TIME_CARD_CODE = "TimeCardCode";

  public static final String COL_TC_CODE = "TcCode";
  public static final String COL_TC_NAME = "TcName";
  public static final String COL_TC_WS_PLANNED = "WsPlanned";
  public static final String COL_TC_WS_ACTUAL = "WsActual";
  public static final String COL_TC_DESCRIPTION = "Description";

  public static final String COL_TR_CODE = "TrCode";
  public static final String COL_TR_NAME = "TrName";
  public static final String COL_TR_FROM = "TimeFrom";
  public static final String COL_TR_UNTIL = "TimeUntil";
  public static final String COL_TR_DURATION = "Duration";
  public static final String COL_TR_DESCRIPTION = "Description";

  public static final String COL_OSF_YEAR_FROM = "YearFrom";
  public static final String COL_OSF_MONTH_FROM = "MonthFrom";
  public static final String COL_OSF_YEAR_UNTIL = "YearUntil";
  public static final String COL_OSF_MONTH_UNTIL = "MonthUntil";

  public static final String COL_OSF_AMOUNT = "Amount";

  public static final String ALS_DEPARTMENT_NAME = "DepartmentName";

  public static final String ALS_LOCATION_CITY_NAME = "CityName";
  public static final String ALS_LOCATION_MANAGER_FIRST_NAME = "ManagerFirstName";
  public static final String ALS_LOCATION_MANAGER_LAST_NAME = "ManagerLastName";
  public static final String ALS_LOCATION_MANAGER_MOBILE = "ManagerMobile";

  public static final String ALS_TC_BACKGROUND = "TcBackground";
  public static final String ALS_TC_FOREGROUND = "TcForeground";

  public static final String ALS_TR_BACKGROUND = "TrBackground";
  public static final String ALS_TR_FOREGROUND = "TrForeground";

  public static final String ALS_TR_FROM = "TrTimeFrom";
  public static final String ALS_TR_UNTIL = "TrTimeUntil";
  public static final String ALS_TR_DURATION = "TrDuration";

  public static final String ALS_TR_USAGE = "TrUsage";

  public static final String GRID_TIME_CARD_CHANGES = "TimeCardChanges";

  public static final String GRID_TIME_CARD_CODES = "TimeCardCodes";
  public static final String GRID_TIME_RANGES = "TimeRanges";
  public static final String GRID_WORK_SCHEDULE_DAY = "WorkScheduleDay";

  public static final String FORM_LOCATION = "Location";
  public static final String FORM_EMPLOYEE = "Employee";
  public static final String FORM_WORK_SCHEDULE = "WorkSchedule";
  public static final String FORM_TIME_SHEET = "TimeSheet";
  public static final String FORM_WORK_SCHEDULE_EDITOR = "WorkScheduleEditor";
  public static final String FORM_NEW_SUBSTITUTION = "NewSubstitution";

  public static void register() {
    EnumUtils.register(ObjectStatus.class);
    EnumUtils.register(WorkScheduleKind.class);
  }

  private PayrollConstants() {
  }
}
