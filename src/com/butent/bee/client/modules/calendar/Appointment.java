package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarConstants.AppointmentStatus;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ItemType;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Map;

public class Appointment extends CalendarItem {

  private static final int BACKGROUND_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      CommonsConstants.COL_BACKGROUND);
  private static final int COLOR_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      CommonsConstants.COL_COLOR);
  private static final int COMPANY_NAME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      ALS_COMPANY_NAME);
  private static final int CREATOR_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS, COL_CREATOR);
  private static final int DESCRIPTION_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_DESCRIPTION);
  private static final int END_DATE_TIME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_END_DATE_TIME);
  private static final int FOREGROUND_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      CommonsConstants.COL_FOREGROUND);
  private static final int START_DATE_TIME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_START_DATE_TIME);
  private static final int STYLE_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS, COL_STYLE);
  private static final int SUMMARY_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS, COL_SUMMARY);
  private static final int APPOINTMENT_TYPE_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_APPOINTMENT_TYPE);
  private static final int VEHICLE_MODEL_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_VEHICLE_MODEL);
  private static final int VEHICLE_NUMBER_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_VEHICLE_NUMBER);
  private static final int VEHICLE_PARENT_MODEL_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_VEHICLE_PARENT_MODEL);

  private static final String SIMPLE_HEADER_TEMPLATE;
  private static final String SIMPLE_BODY_TEMPLATE;

  private static final String MULTI_HEADER_TEMPLATE;
  private static final String MULTI_BODY_TEMPLATE;

  private static final String COMPACT_TEMPLATE;
  private static final String TITLE_TEMPLATE;

  private static final String STRING_TEMPLATE;

  private static final String KEY_RESOURCES = "Resources";
  private static final String KEY_OWNERS = "Owners";
  private static final String KEY_PROPERTIES = "Properties";
  private static final String KEY_REMINDERS = "Reminders";

  static {
    SIMPLE_HEADER_TEMPLATE = wrap(COL_SUMMARY);
    SIMPLE_BODY_TEMPLATE = BeeUtils.buildLines(wrap(ALS_COMPANY_NAME),
        BeeUtils.joinWords(wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL)),
        wrap(COL_VEHICLE_NUMBER), wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES),
        wrap(KEY_OWNERS), wrap(COL_DESCRIPTION), wrap(KEY_REMINDERS));

    MULTI_HEADER_TEMPLATE = BeeUtils.joinWords(wrap(KEY_PERIOD), wrap(COL_SUMMARY));
    MULTI_BODY_TEMPLATE = BeeUtils.joinWords(wrap(ALS_COMPANY_NAME),
        wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL), wrap(COL_VEHICLE_NUMBER),
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(KEY_OWNERS));

    COMPACT_TEMPLATE = BeeUtils.joinWords(wrap(COL_SUMMARY),
        wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL), wrap(COL_VEHICLE_NUMBER));

    TITLE_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_STATUS),
        BeeConst.STRING_EMPTY, wrap(ALS_COMPANY_NAME),
        BeeUtils.joinWords(wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL),
            wrap(COL_VEHICLE_NUMBER)), BeeConst.STRING_EMPTY,
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(KEY_OWNERS), BeeConst.STRING_EMPTY,
        wrap(COL_DESCRIPTION), BeeConst.STRING_EMPTY, wrap(KEY_REMINDERS));

    STRING_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_STATUS),
        wrap(ALS_COMPANY_NAME),
        BeeUtils.joinWords(wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL),
            wrap(COL_VEHICLE_NUMBER)), wrap(COL_SUMMARY),
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(KEY_OWNERS), wrap(COL_DESCRIPTION),
        wrap(KEY_REMINDERS));
  }

  private final BeeRow row;

  private final List<Long> attendees = Lists.newArrayList();
  private final List<Long> owners = Lists.newArrayList();
  private final List<Long> properties = Lists.newArrayList();
  private final List<Long> reminders = Lists.newArrayList();

  private final Long separatedAttendee;

  public Appointment(BeeRow row) {
    this(row, null);
  }

  public Appointment(BeeRow row, Long separatedAttendee) {
    this.row = row;
    this.separatedAttendee = separatedAttendee;

    String attList = row.getProperty(TBL_APPOINTMENT_ATTENDEES);
    if (!BeeUtils.isEmpty(attList)) {
      attendees.addAll(DataUtils.parseIdList(attList));
    }

    String ownerList = row.getProperty(TBL_APPOINTMENT_OWNERS);
    if (!BeeUtils.isEmpty(ownerList)) {
      owners.addAll(DataUtils.parseIdList(ownerList));
    }

    String propList = row.getProperty(TBL_APPOINTMENT_PROPS);
    if (!BeeUtils.isEmpty(propList)) {
      properties.addAll(DataUtils.parseIdList(propList));
    }

    String remindList = row.getProperty(TBL_APPOINTMENT_REMINDERS);
    if (!BeeUtils.isEmpty(remindList)) {
      reminders.addAll(DataUtils.parseIdList(remindList));
    }
  }

  public List<Long> getAttendees() {
    return attendees;
  }

  @Override
  public String getBackground() {
    return row.getString(BACKGROUND_INDEX);
  }

  public Long getColor() {
    return row.getLong(COLOR_INDEX);
  }

  @Override
  public String getCompactTemplate() {
    return COMPACT_TEMPLATE;
  }

  @Override
  public String getCompanyName() {
    return row.getString(COMPANY_NAME_INDEX);
  }

  public Long getCreator() {
    return row.getLong(CREATOR_INDEX);
  }

  @Override
  public String getDescription() {
    return row.getString(DESCRIPTION_INDEX);
  }

  @Override
  public DateTime getEnd() {
    return row.getDateTime(END_DATE_TIME_INDEX);
  }

  @Override
  public long getEndMillis() {
    return BeeUtils.unbox(row.getLong(END_DATE_TIME_INDEX));
  }

  @Override
  public String getForeground() {
    return row.getString(FOREGROUND_INDEX);
  }

  @Override
  public long getId() {
    return row.getId();
  }

  @Override
  public ItemType getItemType() {
    return ItemType.APPOINTMENT;
  }

  @Override
  public String getMultiBodyTemplate() {
    return MULTI_BODY_TEMPLATE;
  }

  @Override
  public String getMultiHeaderTemplate() {
    return MULTI_HEADER_TEMPLATE;
  }

  public List<Long> getOwners() {
    return owners;
  }

  public List<Long> getProperties() {
    return properties;
  }

  public List<Long> getReminders() {
    return reminders;
  }

  public BeeRow getRow() {
    return row;
  }

  @Override
  public Long getSeparatedAttendee() {
    return separatedAttendee;
  }

  @Override
  public String getSimpleBodyTemplate() {
    return SIMPLE_BODY_TEMPLATE;
  }

  @Override
  public String getSimpleHeaderTemplate() {
    return SIMPLE_HEADER_TEMPLATE;
  }

  @Override
  public DateTime getStart() {
    return row.getDateTime(START_DATE_TIME_INDEX);
  }

  @Override
  public long getStartMillis() {
    return BeeUtils.unbox(row.getLong(START_DATE_TIME_INDEX));
  }

  @Override
  public String getStringTemplate() {
    return STRING_TEMPLATE;
  }

  @Override
  public Long getStyle() {
    return row.getLong(STYLE_INDEX);
  }

  @Override
  public Map<String, String> getSubstitutes(long calendarId, Map<Long, UserData> users) {
    Map<String, String> result = Maps.newHashMap();

    List<BeeColumn> columns = CalendarKeeper.getAppointmentViewColumns();

    for (int i = 0; i < columns.size(); i++) {
      String key = columns.get(i).getId();
      String value = row.getString(i);

      if (key.equals(COL_STATUS) && BeeUtils.isInt(value)) {
        value = EnumUtils.getCaption(AppointmentStatus.class, BeeUtils.toInt(value));
      } else if (value != null && ValueType.DATE_TIME.equals(columns.get(i).getType())) {
        value = CalendarUtils.renderDateTime(row.getDateTime(i));
      }

      result.put(wrap(key), BeeUtils.trim(value));
    }

    String attNames = BeeConst.STRING_EMPTY;
    String ownerNames = BeeConst.STRING_EMPTY;
    String propNames = BeeConst.STRING_EMPTY;
    String remindNames = BeeConst.STRING_EMPTY;

    if (!getAttendees().isEmpty()) {
      for (Long id : getAttendees()) {
        attNames = BeeUtils.join(CHILD_SEPARATOR, attNames,
            CalendarKeeper.getAttendeeCaption(calendarId, id));
      }
    }

    if (!getOwners().isEmpty()) {
      for (Long id : getOwners()) {
        if (users.containsKey(id)) {
          ownerNames = BeeUtils.join(CHILD_SEPARATOR, ownerNames, users.get(id).getUserSign());
        }
      }
    }

    if (!getProperties().isEmpty()) {
      for (Long id : getProperties()) {
        propNames = BeeUtils.join(CHILD_SEPARATOR, propNames, CalendarKeeper.getPropertyName(id));
      }
    }

    if (!getReminders().isEmpty()) {
      for (Long id : getReminders()) {
        remindNames = BeeUtils.join(CHILD_SEPARATOR, remindNames,
            CalendarKeeper.getReminderTypeName(id));
      }
    }

    result.put(wrap(KEY_RESOURCES), attNames);
    result.put(wrap(KEY_OWNERS), ownerNames);
    result.put(wrap(KEY_PROPERTIES), propNames);
    result.put(wrap(KEY_REMINDERS), remindNames);

    result.put(wrap(KEY_PERIOD), CalendarUtils.renderPeriod(getStart(), getEnd()));

    return result;
  }

  @Override
  public String getSummary() {
    return row.getString(SUMMARY_INDEX);
  }

  @Override
  public String getTitleTemplate() {
    return TITLE_TEMPLATE;
  }

  public Long getType() {
    return row.getLong(APPOINTMENT_TYPE_INDEX);
  }

  public String getVehicleModel() {
    return row.getString(VEHICLE_MODEL_INDEX);
  }

  public String getVehicleNumber() {
    return row.getString(VEHICLE_NUMBER_INDEX);
  }

  public String getVehicleParentModel() {
    return row.getString(VEHICLE_PARENT_MODEL_INDEX);
  }

  public void setEnd(DateTime end) {
    row.setValue(END_DATE_TIME_INDEX, end);
  }

  public void setStart(DateTime start) {
    row.setValue(START_DATE_TIME_INDEX, start);
  }

  public void updateAttendees(List<Long> ids) {
    attendees.clear();
    if (!BeeUtils.isEmpty(ids)) {
      attendees.addAll(ids);
    }
    row.setProperty(TBL_APPOINTMENT_ATTENDEES, DataUtils.buildIdList(ids));
  }
}
