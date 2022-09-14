package com.butent.bee.server.modules.calendar;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_SERVICE_EVENT;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_TYPE;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.TBL_TASK_TYPES;

import com.butent.bee.server.communication.ChatBean;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewDeleteEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEvent.ViewUpdateEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.classifiers.ClassifiersModuleBean;
import com.butent.bee.server.modules.classifiers.TimerBuilder;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.ui.UiServiceBean;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.calendar.CalendarHelper;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.modules.calendar.CalendarTask;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Singleton
@Lock(LockType.READ)
public class CalendarModuleBean extends TimerBuilder implements BeeModule {

  private static final Filter VALID_APPOINTMENT = Filter.and(Filter.notNull(COL_START_DATE_TIME),
      Filter.compareWithColumn(COL_START_DATE_TIME, Operator.LT, COL_END_DATE_TIME));

  private static BeeLogger logger = LogUtils.getLogger(CalendarModuleBean.class);

  private static String formatMinutes(int minutes) {
    return BeeUtils.toString(minutes / TimeUtils.MINUTES_PER_HOUR) + TimeUtils.TIME_FIELD_SEPARATOR
        + TimeUtils.padTwo(minutes % TimeUtils.MINUTES_PER_HOUR);
  }

  private static void formatTimeColumns(BeeRowSet rowSet, int colFrom, int colTo) {
    for (BeeRow row : rowSet.getRows()) {
      for (int c = colFrom; c <= colTo; c++) {
        Integer value = row.getInteger(c);
        if (value != null) {
          row.setValue(c, formatMinutes(value));
        }
      }
    }
  }

  private static Filter getAttendeeFilter(List<Long> attendeeTypes, List<Long> attendees) {
    if (!BeeUtils.isEmpty(attendeeTypes) || !BeeUtils.isEmpty(attendees)) {
      return Filter.or(Filter.any(COL_ATTENDEE_TYPE, attendeeTypes),
          Filter.idIn(attendees));
    } else if (!BeeUtils.isEmpty(attendeeTypes)) {
      return Filter.any(COL_ATTENDEE_TYPE, attendeeTypes);
    } else if (!BeeUtils.isEmpty(attendees)) {
      return Filter.idIn(attendees);
    } else {
      return null;
    }
  }

  private static Range<DateTime> getDateRange(JustDate lower, JustDate upper) {
    if (lower != null && upper != null) {
      return Range.closedOpen(lower.getDateTime(), upper.getDateTime());
    } else if (lower != null) {
      return Range.atLeast(lower.getDateTime());
    } else if (upper != null) {
      return Range.lessThan(upper.getDateTime());
    } else {
      return Range.all();
    }
  }

  private static Range<Integer> getHourRange(Integer lower, Integer upper) {
    if (BeeUtils.isPositive(lower) && BeeUtils.isPositive(upper)) {
      return Range.closedOpen(lower, upper);
    } else if (BeeUtils.isPositive(lower)) {
      return Range.atLeast(lower);
    } else if (BeeUtils.isPositive(upper)) {
      return Range.lessThan(upper);
    } else {
      return Range.all();
    }
  }

  private static Map<Integer, Integer> splitByHour(DateTime start, DateTime end,
      Range<DateTime> dateRange, Range<Integer> hourRange) {
    Map<Integer, Integer> result = new HashMap<>();
    if (start == null || end == null) {
      return result;
    }

    DateTime dt = DateTime.copyOf(start);
    while (TimeUtils.isLess(dt, end)) {
      int hour = dt.getHour();
      DateTime next = TimeUtils.nextHour(dt, 0);

      int value = BeeUtils.unbox(result.get(hour));

      if (TimeUtils.isLess(next, end)) {
        if (dateRange.contains(dt) && hourRange.contains(hour)) {
          result.put(hour, TimeUtils.minuteDiff(dt, next) + value);
        }
        dt.setTime(next.getTime());

      } else {
        if (dateRange.contains(dt) && hourRange.contains(hour)) {
          result.put(hour, TimeUtils.minuteDiff(dt, end) + value);
        }
        break;
      }
    }
    return result;
  }

  private static Map<YearMonth, Integer> splitByYearMonth(DateTime start, DateTime end,
      Range<DateTime> range) {
    Map<YearMonth, Integer> result = new HashMap<>();
    if (start == null || end == null) {
      return result;
    }

    if (TimeUtils.sameMonth(start, end) && range.contains(start) && range.contains(end)) {
      result.put(new YearMonth(start), TimeUtils.minuteDiff(start, end));
      return result;
    }

    DateTime dt = DateTime.copyOf(start);
    while (TimeUtils.isLess(dt, end)) {
      YearMonth ym = new YearMonth(dt);
      DateTime next = TimeUtils.nextDay(dt).getDateTime();

      int value = BeeUtils.unbox(result.get(ym));

      if (TimeUtils.isLess(next, end)) {
        if (range.contains(dt)) {
          result.put(ym, TimeUtils.minuteDiff(dt, next) + value);
        }
        dt.setTime(next.getTime());
      } else {
        if (range.contains(dt)) {
          result.put(ym, TimeUtils.minuteDiff(dt, end) + value);
        }
        break;
      }
    }
    return result;
  }

  private static void totalColumns(BeeRowSet rowSet, int colFrom, int colTo, int colTotal) {
    for (BeeRow row : rowSet.getRows()) {
      int sum = 0;
      for (int c = colFrom; c <= colTo; c++) {
        Integer value = row.getInteger(c);
        if (value != null) {
          sum += value;
        }
      }
      row.setValue(colTotal, sum);
    }
  }

  private static void totalRows(BeeRowSet rowSet, int colFrom, int colTo, long rowId,
      String caption, int colCaption) {

    Integer[] totals = new Integer[colTo - colFrom + 1];
    for (int i = 0; i < totals.length; i++) {
      totals[i] = 0;
    }

    for (BeeRow row : rowSet.getRows()) {
      for (int c = colFrom; c <= colTo; c++) {
        Integer value = row.getInteger(c);
        if (value != null) {
          totals[c - colFrom] += value;
        }
      }
    }

    String[] arr = new String[rowSet.getNumberOfColumns()];
    if (!BeeUtils.isEmpty(caption)) {
      arr[colCaption] = caption;
    }
    for (int c = colFrom; c <= colTo; c++) {
      arr[c] = BeeUtils.toString(totals[c - colFrom]);
    }

    rowSet.addRow(rowId, arr);
  }

  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  DataEditorBean deb;
  @EJB
  MailModuleBean mail;
  @EJB
  ParamHolderBean prm;
  @EJB
  ClassifiersModuleBean cmb;
  @EJB
  ChatBean chat;
  @EJB
  UiServiceBean usb;

  @EJB
  NewsBean news;

  @Resource
  TimerService timerService;

  @Override
  protected List<Timer> createTimers(String timerIdentifier, IsCondition wh) {
    List<Timer> timersList = new ArrayList<>();

    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_CALENDAR_EVENTS)) {
      String reminderIdName = sys.getIdName(TBL_APPOINTMENT_REMINDERS);

      SimpleRowSet data = qs.getData(new SqlSelect()
          .addFields(TBL_APPOINTMENTS, COL_START_DATE_TIME)
          .addFields(TBL_APPOINTMENT_REMINDERS, reminderIdName, COL_APPOINTMENT,
              COL_HOURS, COL_MINUTES, COL_SCHEDULED)
          .addField(TBL_REMINDER_TYPES, COL_HOURS, COL_ALIAS_DEF_HOURS)
          .addField(TBL_REMINDER_TYPES, COL_MINUTES, COL_ALIAS_DEL_MINUTES)
          .addFrom(TBL_APPOINTMENTS)
          .addFromInner(TBL_APPOINTMENT_REMINDERS,
              sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT))
          .addFromInner(TBL_REMINDER_TYPES,
              sys.joinTables(TBL_REMINDER_TYPES, TBL_APPOINTMENT_REMINDERS,
                  COL_REMINDER_TYPE))
          .setWhere(SqlUtils.and(wh,
              SqlUtils.more(TBL_APPOINTMENTS, COL_START_DATE_TIME, System.currentTimeMillis()),
              SqlUtils.notEqual(TBL_APPOINTMENTS, COL_STATUS,
                  AppointmentStatus.CANCELED.ordinal()))));

      for (SimpleRow row : data) {
        Long start = row.getLong(COL_SCHEDULED);

        if (start == null) {
          long offset = BeeUtils.unbox(row.getInt(COL_HOURS)) * TimeUtils.MILLIS_PER_HOUR
              + BeeUtils.unbox(row.getInt(COL_MINUTES)) * TimeUtils.MILLIS_PER_MINUTE;

          if (offset == 0) {
            offset = BeeUtils.unbox(row.getInt(COL_ALIAS_DEF_HOURS)) * TimeUtils.MILLIS_PER_HOUR
                + BeeUtils.unbox(row.getInt(COL_ALIAS_DEL_MINUTES))
                * TimeUtils.MILLIS_PER_MINUTE;
          }
          if (offset != 0) {
            start = BeeUtils.unbox(row.getLong(COL_START_DATE_TIME)) - offset;
          }
        }
        if (start != null) {
          DateTime time = TimeUtils.toDateTimeOrNull(start);
          long from = BeeUtils.unbox(prm.getTime(PRM_REMINDER_TIME_FROM));
          long until = BeeUtils.unbox(prm.getTime(PRM_REMINDER_TIME_UNTIL));

          if (from < until) {
            int current = TimeUtils.minutesSinceDayStarted(time) * TimeUtils.MILLIS_PER_MINUTE;

            if (current < from || current > until) {
              time =
                  new DateTime((current < from) ? TimeUtils.previousDay(time) : time.getDate());
              time.setTime(time.getTime() + until);
            }
          }
          if (time.getTime() > System.currentTimeMillis()) {
            Timer timer = timerService.createSingleActionTimer(time.getJava(),
                new TimerConfig(TIMER_REMIND_CALENDAR_EVENTS + row.getLong(reminderIdName), false));

            if (timer != null) {
              timersList.add(timer);
            }
          }
        }
      }
    }
    return timersList;
  }

  @Override
  protected Pair<IsCondition, List<String>> getConditionAndTimerIdForUpdate(String timerIdentifier,
      String viewName, Long relationId) {
    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_CALENDAR_EVENTS)) {
      IsCondition wh;
      String idName = viewName;
      Long id = relationId;
      String reminderIdName = sys.getIdName(TBL_APPOINTMENT_REMINDERS);

      if (BeeUtils.same(idName, TBL_APPOINTMENTS)) {
        wh = SqlUtils.equals(TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT, id);

        SimpleRowSet data = qs.getData(new SqlSelect()
            .addFields(TBL_APPOINTMENT_REMINDERS, reminderIdName)
            .addFrom(TBL_APPOINTMENT_REMINDERS)
            .addFromInner(TBL_APPOINTMENTS,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT))
            .setWhere(SqlUtils.and(wh,
                SqlUtils.more(TBL_APPOINTMENTS,
                    COL_START_DATE_TIME, System.currentTimeMillis()),
                SqlUtils.notEqual(TBL_APPOINTMENTS, COL_STATUS,
                    AppointmentStatus.CANCELED.ordinal()))));

        List<String> timerIdentifiersIds = new ArrayList<String>();
        if (data != null) {
          for (SimpleRow row : data) {
            timerIdentifiersIds.add(
                timerIdentifier + row.getLong(data.getColumnIndex(reminderIdName)));
          }
        }

        return Pair.of(wh, timerIdentifiersIds);

      } else if (BeeUtils.same(idName, TBL_APPOINTMENT_REMINDERS)) {
        wh = SqlUtils.equals(TBL_APPOINTMENT_REMINDERS, reminderIdName, id);

        List<String> timerIdentifiersIds = new ArrayList<String>();
        timerIdentifiersIds.add(timerIdentifier + id);
        return Pair.of(wh, timerIdentifiersIds);
      }
    }
    return null;
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> results = new ArrayList<>();

    Filter filter = Filter.or(
        Filter.anyContains(Sets.newHashSet(COL_SUMMARY, COL_DESCRIPTION,
            COL_APPOINTMENT_LOCATION, ALS_COMPANY_NAME, COL_VEHICLE_NUMBER), query),
        Filter.anyItemContains(COL_STATUS, AppointmentStatus.class, query));

    List<BeeRow> appointments = getAppointments(filter,
        new Order(COL_START_DATE_TIME, false), true);
    if (!BeeUtils.isEmpty(appointments)) {
      for (BeeRow row : appointments) {
        results.add(new SearchResult(VIEW_APPOINTMENTS, row));
      }
    }

    return results;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.same(svc, SVC_GET_USER_CALENDAR)) {
      response = getUserCalendar(reqInfo);
    } else if (BeeUtils.same(svc, SVC_CREATE_APPOINTMENT)) {
      response = createAppointment(reqInfo);
    } else if (BeeUtils.same(svc, SVC_UPDATE_APPOINTMENT)) {
      response = updateAppointment(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_CALENDAR_ITEMS)) {
      response = getCalendarItems(reqInfo);
    } else if (BeeUtils.same(svc, SVC_SAVE_ACTIVE_VIEW)) {
      response = saveActiveView(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_OVERLAPPING_APPOINTMENTS)) {
      response = getOverlappingAppointments(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_REPORT_OPTIONS)) {
      response = getReportOptions(reqInfo);
    } else if (BeeUtils.same(svc, SVC_DO_REPORT)) {
      response = doReport(reqInfo);

    } else {
      String msg = BeeUtils.joinWords("Calendar service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Lists.newArrayList(
        BeeParameter.createTimeOfDay(module, PRM_REMINDER_TIME_FROM, false,
            TimeUtils.parseTime("8:00")),
        BeeParameter.createTimeOfDay(module, PRM_REMINDER_TIME_UNTIL, false,
            TimeUtils.parseTime("18:00")));
  }

  @Override
  public Module getModule() {
    return Module.CALENDAR;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  @Override
  public void init() {
    buildTimers(TIMER_REMIND_CALENDAR_EVENTS);

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_APPOINTMENTS) && event.hasData()) {
          setAppopintmentProperties(event.getRowset());
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateTimers(ViewModifyEvent event) {
        if (event.isAfter(TBL_REMINDER_TYPES)) {
          if (event instanceof ViewDeleteEvent
              || event instanceof ViewUpdateEvent
              && (DataUtils.contains(((ViewUpdateEvent) event).getColumns(), COL_HOURS)
              || DataUtils.contains(((ViewUpdateEvent) event).getColumns(), COL_MINUTES))) {

            createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS, null, null);
          }

        } else if (event.isAfter(TBL_APPOINTMENTS)) {
          if (event instanceof ViewDeleteEvent) {
            for (long id : ((ViewDeleteEvent) event).getIds()) {
              createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS, TBL_APPOINTMENTS, id);
            }
          } else if (event instanceof ViewUpdateEvent) {
            ViewUpdateEvent ev = (ViewUpdateEvent) event;

            if (DataUtils.contains(ev.getColumns(), COL_STATUS)
                || DataUtils.contains(ev.getColumns(), COL_START_DATE_TIME)) {

              createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS,
                  TBL_APPOINTMENTS, ev.getRow().getId());
            }
          }

        } else if (event.isAfter(TBL_APPOINTMENT_REMINDERS)) {
          if (event instanceof ViewDeleteEvent) {
            for (long id : ((ViewDeleteEvent) event).getIds()) {
              createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS, TBL_APPOINTMENT_REMINDERS, id);
            }
          } else if (event instanceof ViewUpdateEvent) {
            ViewUpdateEvent ev = (ViewUpdateEvent) event;

            if (DataUtils.contains(ev.getColumns(), COL_REMINDER_TYPE)
                || DataUtils.contains(ev.getColumns(), COL_HOURS)
                || DataUtils.contains(ev.getColumns(), COL_MINUTES)
                || DataUtils.contains(ev.getColumns(), COL_SCHEDULED)) {

              createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS,
                  TBL_APPOINTMENT_REMINDERS, ev.getRow().getId());
            }
          } else if (event instanceof ViewInsertEvent) {
            createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS,
                TBL_APPOINTMENT_REMINDERS, ((ViewInsertEvent) event).getRow().getId());
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.APPOINTMENTS_MY, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        Long companyPerson = usr.getCompanyPerson(userId);
        if (companyPerson == null) {
          return null;
        }

        String idColumn = sys.getIdName(TBL_APPOINTMENTS);
        String usageTable = NewsConstants.getUsageTable(TBL_APPOINTMENTS);

        return new SqlSelect()
            .addFields(TBL_APPOINTMENTS, idColumn)
            .addMax(usageTable, NewsConstants.COL_USAGE_ACCESS)
            .addFrom(TBL_APPOINTMENTS)
            .addFromInner(usageTable, news.joinUsage(TBL_APPOINTMENTS))
            .addFromLeft(TBL_APPOINTMENT_OWNERS,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT))
            .addFromLeft(TBL_APPOINTMENT_ATTENDEES,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT))
            .addFromLeft(TBL_ATTENDEES,
                sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE))
            .setWhere(
                SqlUtils.and(SqlUtils.equals(usageTable, NewsConstants.COL_USAGE_USER, userId),
                    SqlUtils.or(
                        SqlUtils.equals(TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT_OWNER, userId),
                        SqlUtils.equals(TBL_ATTENDEES, COL_COMPANY_PERSON, companyPerson))))
            .addGroup(TBL_APPOINTMENTS, idColumn);
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        Long companyPerson = usr.getCompanyPerson(userId);
        if (companyPerson == null) {
          return null;
        }

        String idColumn = sys.getIdName(TBL_APPOINTMENTS);
        String usageTable = NewsConstants.getUsageTable(TBL_APPOINTMENTS);

        SqlSelect subquery = new SqlSelect()
            .addFields(TBL_APPOINTMENTS, idColumn)
            .addFields(usageTable, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(TBL_APPOINTMENTS)
            .addFromInner(usageTable, news.joinUsage(TBL_APPOINTMENTS))
            .addFromLeft(TBL_APPOINTMENT_OWNERS,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT))
            .addFromLeft(TBL_APPOINTMENT_ATTENDEES,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT))
            .addFromLeft(TBL_ATTENDEES,
                sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE))
            .setWhere(SqlUtils.and(NewsHelper.getUpdatesCondition(usageTable, userId, startDate),
                SqlUtils.or(
                    SqlUtils.equals(TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT_OWNER, userId),
                    SqlUtils.equals(TBL_ATTENDEES, COL_COMPANY_PERSON, companyPerson))));

        usageTable = NewsConstants.getUsageTable(TBL_APPOINTMENT_ATTENDEES);

        subquery.addUnion(new SqlSelect()
            .addFields(TBL_APPOINTMENTS, idColumn)
            .addFields(usageTable, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(TBL_APPOINTMENTS)
            .addFromInner(TBL_APPOINTMENT_ATTENDEES,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT))
            .addFromInner(TBL_ATTENDEES,
                sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE))
            .addFromInner(usageTable, news.joinUsage(TBL_APPOINTMENT_ATTENDEES))
            .setWhere(SqlUtils.and(NewsHelper.getUpdatesCondition(usageTable, userId, startDate),
                SqlUtils.equals(TBL_ATTENDEES, COL_COMPANY_PERSON, companyPerson))));

        String alias = SqlUtils.uniqueName();

        return new SqlSelect()
            .addFields(alias, idColumn)
            .addMax(alias, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(subquery, alias)
            .addGroup(alias, idColumn);
      }
    });

    HeadlineProducer headlineProducer = (feed, userId, rowSet, row, isNew, constants, dtfInfo) -> {
      String caption = DataUtils.getString(rowSet, row, COL_SUMMARY);
      if (BeeUtils.isEmpty(caption)) {
        caption = BeeUtils.bracket(row.getId());
      }

      List<String> subtitles = new ArrayList<>();

      DateTime start = DataUtils.getDateTime(rowSet, row, COL_START_DATE_TIME);
      DateTime end = DataUtils.getDateTime(rowSet, row, COL_END_DATE_TIME);

      if ((start != null || end != null) && dtfInfo != null) {
        String period = BeeUtils.joinWords(
            (start == null) ? null : Formatter.renderDateTime(dtfInfo, start),
            TimeUtils.PERIOD_SEPARATOR,
            (end == null) ? null : Formatter.renderDateTime(dtfInfo, end));

        subtitles.add(period);
      }

      AppointmentStatus status = EnumUtils.getEnumByIndex(AppointmentStatus.class,
          DataUtils.getInteger(rowSet, row, COL_STATUS));
      if (status != null) {
        subtitles.add(status.getCaption(constants));
      }

      return Headline.create(row.getId(), caption, subtitles, isNew);
    };

    news.registerHeadlineProducer(Feed.APPOINTMENTS_MY, headlineProducer);
    news.registerHeadlineProducer(Feed.APPOINTMENTS_ALL, headlineProducer);
  }

  private boolean checkTable(String name) {
    return sys.isTable(name) && sys.getTable(name).isActive();
  }

  private ResponseObject createAppointment(RequestInfo reqInfo) {
    BeeRowSet rowSet = BeeRowSet.restore(reqInfo.getContent());
    if (rowSet.isEmpty()) {
      return ResponseObject.error(SVC_CREATE_APPOINTMENT, ": rowSet is empty");
    }

    ResponseObject response = deb.commitRow(rowSet);
    if (response.hasErrors()) {
      return response;
    }

    long appId = ((BeeRow) response.getResponse()).getId();

    for (Map.Entry<String, String> entry : APPOINTMENT_CHILDREN.entrySet()) {
      String childTable = entry.getKey();

      List<Long> children = DataUtils.parseIdList(rowSet.getTableProperty(childTable));
      if (!children.isEmpty()) {
        for (long child : children) {
          long childId = qs.insertData(new SqlInsert(childTable).addConstant(COL_APPOINTMENT, appId)
              .addConstant(entry.getValue(), child));

          if (BeeUtils.same(TBL_APPOINTMENT_REMINDERS, childTable)) {
            createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS, TBL_APPOINTMENT_REMINDERS, childId);
          }
        }
      }
    }

    return response;
  }

  private ResponseObject doReport(RequestInfo reqInfo) {
    Integer paramRep = BeeUtils.toIntOrNull(reqInfo.getParameter(PARAM_REPORT));
    if (!EnumUtils.isOrdinal(Report.class, paramRep)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_REPORT);
    }

    BeeRowSet rowSet = BeeRowSet.restore(reqInfo.getContent());
    if (rowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), ": options rowSet is empty");
    }

    BeeRow row = rowSet.getRow(0);

    JustDate lowerDate = DataUtils.getDate(rowSet, row, COL_LOWER_DATE);
    JustDate upperDate = DataUtils.getDate(rowSet, row, COL_UPPER_DATE);

    Integer lowerHour = DataUtils.getInteger(rowSet, row, COL_LOWER_HOUR);
    Integer upperHour = DataUtils.getInteger(rowSet, row, COL_UPPER_HOUR);

    String atpList = DataUtils.getString(rowSet, row, COL_ATTENDEE_TYPES);
    String attList = DataUtils.getString(rowSet, row, COL_ATTENDEES);

    SqlUpdate update = new SqlUpdate(TBL_REPORT_OPTIONS)
        .addConstant(COL_CAPTION, DataUtils.getString(rowSet, row, COL_CAPTION))
        .addConstant(COL_LOWER_DATE, lowerDate)
        .addConstant(COL_UPPER_DATE, upperDate)
        .addConstant(COL_LOWER_HOUR, lowerHour)
        .addConstant(COL_UPPER_HOUR, upperHour)
        .addConstant(COL_ATTENDEE_TYPES, atpList)
        .addConstant(COL_ATTENDEES, attList)
        .setWhere(sys.idEquals(TBL_REPORT_OPTIONS, row.getId()));

    ResponseObject response = qs.updateDataWithResponse(update);
    if (response.hasErrors()) {
      return response;
    }

    List<Long> attendeeTypes = DataUtils.parseIdList(atpList);
    List<Long> attendees = DataUtils.parseIdList(attList);

    Report report = Report.values()[paramRep];
    BeeRowSet result;

    switch (report) {
      case BUSY_HOURS:
        result = getAttendeesByHour(lowerDate, upperDate, lowerHour, upperHour,
            attendeeTypes, attendees, false);
        break;
      case BUSY_MONTHS:
        result = getAttendeesByMonth(lowerDate, upperDate, attendeeTypes, attendees, false);
        break;
      case CANCEL_HOURS:
        result = getAttendeesByHour(lowerDate, upperDate, lowerHour, upperHour,
            attendeeTypes, attendees, true);
        break;
      case CANCEL_MONTHS:
        result = getAttendeesByMonth(lowerDate, upperDate, attendeeTypes, attendees, true);
        break;
      default:
        Assert.untouchable();
        result = null;
    }

    if (result == null) {
      return ResponseObject.response(BeeConst.STRING_EMPTY);
    } else {
      return ResponseObject.response(result);
    }
  }

  private SimpleRowSet getAppointmentAttendeeEmails(Long appointmentId) {
    if (!DataUtils.isId(appointmentId)) {
      return null;
    }
    SqlSelect select = new SqlSelect()
        .addFields(TBL_EMAILS, COL_EMAIL)
        .addFields(TBL_USERS, sys.getIdName(TBL_USERS))
        .addFrom(TBL_APPOINTMENT_ATTENDEES)
        .addFromLeft(TBL_ATTENDEES,
            sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE))
        .addFromInner(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_ATTENDEES, COL_COMPANY_PERSON))
        .addFromInner(TBL_CONTACTS,
            sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
        .addFromInner(TBL_EMAILS,
            sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
        .addFromInner(TBL_USERS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
        .setWhere(SqlUtils.equals(TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT, appointmentId));

    return qs.getData(select);
  }

  private SimpleRow getAppointmentRemindData(Long reminderId) {
    if (!DataUtils.isId(reminderId)) {
      return null;
    }
    IsCondition wh = sys.idEquals(TBL_APPOINTMENT_REMINDERS, reminderId);
    String ownerCompanyPerson = SqlUtils.uniqueName();
    return qs.getRow(new SqlSelect()
        .addFields(TBL_APPOINTMENTS, sys.getIdName(TBL_APPOINTMENTS), COL_CREATED, COL_SUMMARY,
            COL_STATUS, COL_DESCRIPTION, COL_START_DATE_TIME, COL_END_DATE_TIME)
        .addField(TBL_APPOINTMENT_TYPES, COL_APPOINTMENT_TYPE_NAME, ALS_APPOINTMENT_TYPE_NAME)
        .addExpr(SqlUtils.concat(
            SqlUtils.nvl(SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME), "''"), "' '",
            SqlUtils.nvl(SqlUtils.field(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME), "''")),
            ALS_COMPANY_NAME)
        .addExpr(SqlUtils.concat(
            SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "''"), "' '",
            SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")),
            ALS_CONTACT_PERSON)
        .addField(TBL_EMAILS, COL_EMAIL_ADDRESS, ALS_OWNER_EMAIL)
        .addFields(TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT, COL_MESSAGE, COL_REMINDER_TYPE)
        .addFields(TBL_REMINDER_TYPES, COL_REMINDER_METHOD, COL_REMINDER_TEMPLATE,
            COL_REMINDER_TEMPLATE_CAPTION)
        .addFields(TBL_USERS, sys.getIdName(TBL_USERS))
        .addFrom(TBL_APPOINTMENTS)
        .addFromLeft(TBL_APPOINTMENT_TYPES,
            sys.joinTables(TBL_APPOINTMENT_TYPES, TBL_APPOINTMENTS, COL_APPOINTMENT_TYPE))
        .addFromInner(TBL_APPOINTMENT_REMINDERS,
            sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT))
        .addFromInner(TBL_REMINDER_TYPES,
            sys.joinTables(TBL_REMINDER_TYPES, TBL_APPOINTMENT_REMINDERS,
                COL_REMINDER_TYPE))
        .addFromLeft(TBL_COMPANIES,
            sys.joinTables(TBL_COMPANIES, CalendarConstants.TBL_APPOINTMENTS, COL_COMPANY))
        .addFromLeft(TBL_COMPANY_TYPES,
            sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, CalendarConstants.TBL_APPOINTMENTS,
                COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .addFromLeft(TBL_USERS,
            sys.joinTables(TBL_USERS, TBL_APPOINTMENTS, COL_CREATOR))
        .addFromLeft(TBL_COMPANY_PERSONS, ownerCompanyPerson,
            sys.joinTables(TBL_COMPANY_PERSONS, ownerCompanyPerson, TBL_USERS,
                COL_COMPANY_PERSON))
        .addFromLeft(TBL_CONTACTS,
            sys.joinTables(TBL_CONTACTS, ownerCompanyPerson, COL_CONTACT))
        .addFromLeft(TBL_EMAILS,
            sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
        .setWhere(SqlUtils.and(wh,
            SqlUtils.more(TBL_APPOINTMENTS, COL_START_DATE_TIME,
                System.currentTimeMillis() - TimeUtils.MILLIS_PER_MINUTE),
            SqlUtils.notEqual(TBL_APPOINTMENTS, COL_STATUS,
                AppointmentStatus.CANCELED.ordinal()))));
  }

  private List<BeeRow> getAppointments(Filter filter, Order order, boolean checkVisibility) {
    List<BeeRow> result = new ArrayList<>();

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return result;
    }

    Filter queryFilter;

    if (checkVisibility) {
      Filter visible = Filter.or().add(
          Filter.equals(COL_CREATOR, userId),
          Filter.in(sys.getIdName(TBL_APPOINTMENTS), VIEW_APPOINTMENT_OWNERS, COL_APPOINTMENT,
              Filter.equals(COL_APPOINTMENT_OWNER, userId)),
          Filter.isNull(COL_VISIBILITY),
          Filter.isNotEqual(COL_VISIBILITY, IntegerValue.of(CalendarVisibility.PRIVATE)));

      queryFilter = Filter.and(filter, visible);

    } else {
      queryFilter = filter;
    }

    BeeRowSet appointments = qs.getViewData(VIEW_APPOINTMENTS, queryFilter, order);
    if (!DataUtils.isEmpty(appointments)) {
      result.addAll(appointments.getRows());
    }

    return result;
  }

  private SimpleRowSet getAttendeePeriods(JustDate lowerDate, JustDate upperDate,
      List<Long> attendeeTypes, List<Long> attendees, boolean canceled) {

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_APPOINTMENTS, COL_START_DATE_TIME, COL_END_DATE_TIME)
        .addFields(TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE)
        .addFrom(TBL_APPOINTMENTS)
        .addFromInner(TBL_APPOINTMENT_ATTENDEES,
            sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT));

    int statusCanceled = AppointmentStatus.CANCELED.ordinal();
    IsCondition statusCondition = canceled
        ? SqlUtils.equals(TBL_APPOINTMENTS, COL_STATUS, statusCanceled)
        : SqlUtils.notEqual(TBL_APPOINTMENTS, COL_STATUS, statusCanceled);

    HasConditions where = SqlUtils.and(statusCondition);

    if (!BeeUtils.isEmpty(attendeeTypes) || !BeeUtils.isEmpty(attendees)) {
      HasConditions attCondition = SqlUtils.or();

      if (!BeeUtils.isEmpty(attendeeTypes)) {
        ss.addFromInner(TBL_ATTENDEES,
            sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE));

        for (Long atpId : attendeeTypes) {
          attCondition.add(SqlUtils.equals(TBL_ATTENDEES, COL_ATTENDEE_TYPE, atpId));
        }
      }

      if (!BeeUtils.isEmpty(attendees)) {
        for (Long attId : attendees) {
          attCondition.add(SqlUtils.equals(TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE, attId));
        }
      }

      where.add(attCondition);
    }

    if (lowerDate != null) {
      where.add(SqlUtils.more(TBL_APPOINTMENTS, COL_END_DATE_TIME, lowerDate));
    }
    if (upperDate != null) {
      where.add(SqlUtils.less(TBL_APPOINTMENTS, COL_START_DATE_TIME, upperDate));
    }

    ss.setWhere(where);

    return qs.getData(ss);
  }

  private BeeRowSet getAttendeesByHour(JustDate lowerDate, JustDate upperDate,
      Integer lowerHour, Integer upperHour, List<Long> attendeeTypes, List<Long> attendees,
      boolean canceled) {

    SimpleRowSet data =
        getAttendeePeriods(lowerDate, upperDate, attendeeTypes, attendees, canceled);
    if (data == null || data.getNumberOfRows() <= 0) {
      return null;
    }

    int startIndex = data.getColumnIndex(COL_START_DATE_TIME);
    int endIndex = data.getColumnIndex(COL_END_DATE_TIME);
    int attIndex = data.getColumnIndex(COL_ATTENDEE);

    Table<Long, Integer, Integer> table = HashBasedTable.create();

    Range<DateTime> dateRange = getDateRange(lowerDate, upperDate);
    Range<Integer> hourRange = getHourRange(lowerHour, upperHour);

    for (int r = 0; r < data.getNumberOfRows(); r++) {
      DateTime start = data.getDateTime(r, startIndex);
      DateTime end = data.getDateTime(r, endIndex);

      Long attendee = data.getLong(r, attIndex);
      Map<Integer, Integer> map = splitByHour(start, end, dateRange, hourRange);

      for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
        int hour = entry.getKey();
        int minutes = entry.getValue();

        Integer value = table.get(attendee, hour);
        if (value == null) {
          table.put(attendee, hour, minutes);
        } else {
          table.put(attendee, hour, value + minutes);
        }
      }
    }
    if (table.isEmpty()) {
      return null;
    }

    List<Integer> hours = new ArrayList<>(table.columnKeySet());
    Collections.sort(hours);

    BeeRowSet attRowSet = qs.getViewData(VIEW_ATTENDEES,
        getAttendeeFilter(attendeeTypes, attendees));
    if (attRowSet.isEmpty()) {
      return null;
    }

    BeeRowSet result = new BeeRowSet(new BeeColumn(ValueType.TEXT, ALS_ATTENDEE_TYPE_NAME),
        new BeeColumn(ValueType.TEXT, COL_ATTENDEE_NAME));
    for (Integer hour : hours) {
      result.addColumn(ValueType.TEXT, hour.toString());
    }

    if (hours.size() > 1) {
      result.addColumn(ValueType.TEXT, usr.getDictionary().calTotal());
    }
    int columnCount = result.getNumberOfColumns();

    for (BeeRow attRow : attRowSet.getRows()) {
      Map<Integer, Integer> tableRow = table.row(attRow.getId());
      if (tableRow == null || tableRow.isEmpty()) {
        continue;
      }

      String[] values = new String[result.getNumberOfColumns()];
      values[0] = DataUtils.getString(attRowSet, attRow, ALS_ATTENDEE_TYPE_NAME);
      values[1] = DataUtils.getString(attRowSet, attRow, COL_ATTENDEE_NAME);

      for (int c = 0; c < hours.size(); c++) {
        Integer minutes = tableRow.get(hours.get(c));
        values[c + 2] = (minutes == null) ? null : minutes.toString();
      }
      result.addRow(attRow.getId(), values);
    }

    if (hours.size() > 1) {
      totalColumns(result, 2, columnCount - 2, columnCount - 1);
    }
    if (result.getNumberOfRows() > 1) {
      totalRows(result, 2, columnCount - 1, 0, usr.getDictionary().totalOf() + ":", 0);
    }
    formatTimeColumns(result, 2, columnCount - 1);

    return result;
  }

  private BeeRowSet getAttendeesByMonth(JustDate lowerDate, JustDate upperDate,
      List<Long> attendeeTypes, List<Long> attendees, boolean canceled) {

    SimpleRowSet data =
        getAttendeePeriods(lowerDate, upperDate, attendeeTypes, attendees, canceled);
    if (data == null || data.getNumberOfRows() <= 0) {
      return null;
    }

    int startIndex = data.getColumnIndex(COL_START_DATE_TIME);
    int endIndex = data.getColumnIndex(COL_END_DATE_TIME);
    int attIndex = data.getColumnIndex(COL_ATTENDEE);

    Table<Long, YearMonth, Integer> table = HashBasedTable.create();

    Range<DateTime> range = getDateRange(lowerDate, upperDate);

    for (int r = 0; r < data.getNumberOfRows(); r++) {
      DateTime start = data.getDateTime(r, startIndex);
      DateTime end = data.getDateTime(r, endIndex);

      Long attendee = data.getLong(r, attIndex);

      for (Map.Entry<YearMonth, Integer> entry : splitByYearMonth(start, end, range).entrySet()) {
        YearMonth ym = entry.getKey();
        int minutes = entry.getValue();

        Integer value = table.get(attendee, ym);
        if (value == null) {
          table.put(attendee, ym, minutes);
        } else {
          table.put(attendee, ym, value + minutes);
        }
      }
    }
    if (table.isEmpty()) {
      return null;
    }

    List<YearMonth> months = new ArrayList<>(table.columnKeySet());
    Collections.sort(months);

    BeeRowSet attRowSet = qs.getViewData(VIEW_ATTENDEES,
        getAttendeeFilter(attendeeTypes, attendees));
    if (attRowSet.isEmpty()) {
      return null;
    }

    BeeRowSet result = new BeeRowSet(new BeeColumn(ValueType.TEXT, ALS_ATTENDEE_TYPE_NAME),
        new BeeColumn(ValueType.TEXT, COL_ATTENDEE_NAME));
    for (YearMonth ym : months) {
      result.addColumn(ValueType.TEXT, ym.toString());
    }

    if (months.size() > 1) {
      result.addColumn(ValueType.TEXT, usr.getDictionary().calTotal());
    }
    int columnCount = result.getNumberOfColumns();

    for (BeeRow attRow : attRowSet.getRows()) {
      Map<YearMonth, Integer> tableRow = table.row(attRow.getId());
      if (tableRow == null || tableRow.isEmpty()) {
        continue;
      }

      String[] values = new String[columnCount];
      values[0] = DataUtils.getString(attRowSet, attRow, ALS_ATTENDEE_TYPE_NAME);
      values[1] = DataUtils.getString(attRowSet, attRow, COL_ATTENDEE_NAME);

      for (int c = 0; c < months.size(); c++) {
        Integer minutes = tableRow.get(months.get(c));
        values[c + 2] = (minutes == null) ? null : minutes.toString();
      }
      result.addRow(attRow.getId(), values);
    }

    if (months.size() > 1) {
      totalColumns(result, 2, columnCount - 2, columnCount - 1);
    }
    if (result.getNumberOfRows() > 1) {
      totalRows(result, 2, columnCount - 1, 0, usr.getDictionary().totalOf() + ":", 0);
    }
    formatTimeColumns(result, 2, columnCount - 1);

    return result;
  }

  private BeeRowSet getCalendarAttendees(long userId, long calendarId) {
    Filter calFilter = Filter.equals(COL_CALENDAR, calendarId);

    BeeRowSet calAttTypes = qs.getViewData(VIEW_CAL_ATTENDEE_TYPES, calFilter);
    BeeRowSet calAttendees = qs.getViewData(VIEW_CALENDAR_ATTENDEES, calFilter);

    CompoundFilter attFilter = Filter.or();
    if (!calAttTypes.isEmpty()) {
      attFilter.add(Filter.any(COL_ATTENDEE_TYPE,
          DataUtils.getDistinct(calAttTypes, COL_ATTENDEE_TYPE)));
    }
    if (!calAttendees.isEmpty()) {
      attFilter.add(Filter.idIn(DataUtils.getDistinct(calAttendees, COL_ATTENDEE)));
    }

    if (attFilter.isEmpty()) {
      Long cp = usr.getCompanyPerson(userId);

      if (cp != null) {
        Filter cpFilter = Filter.equals(COL_COMPANY_PERSON, cp);
        BeeRowSet attendees = qs.getViewData(VIEW_ATTENDEES, cpFilter);
        if (!attendees.isEmpty()) {
          return attendees;
        }
      }
    }

    return qs.getViewData(VIEW_ATTENDEES, attFilter);
  }

  private ResponseObject getCalendarItems(RequestInfo reqInfo) {
    long calendarId = BeeUtils.toLong(reqInfo.getParameter(PARAM_CALENDAR_ID));
    if (!DataUtils.isId(calendarId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_CALENDAR_ID);
    }

    Filter calFilter = Filter.equals(COL_CALENDAR, calendarId);
    BeeRowSet calAppTypes = qs.getViewData(VIEW_CAL_APPOINTMENT_TYPES, calFilter);

    CompoundFilter appFilter = Filter.and();
    appFilter.add(VALID_APPOINTMENT);
    appFilter.add(Filter.isNotEqual(COL_STATUS, IntegerValue.of(AppointmentStatus.CANCELED)));

    Long startTime = BeeUtils.toLongOrNull(reqInfo.getParameter(PARAM_START_TIME));
    Long endTime = BeeUtils.toLongOrNull(reqInfo.getParameter(PARAM_END_TIME));

    if (startTime != null) {
      appFilter.add(Filter.isMore(COL_END_DATE_TIME, new LongValue(startTime)));
    }
    if (endTime != null) {
      appFilter.add(Filter.isLess(COL_START_DATE_TIME, new LongValue(endTime)));
    }

    if (!calAppTypes.isEmpty()) {
      appFilter.add(Filter.any(COL_APPOINTMENT_TYPE,
          DataUtils.getDistinct(calAppTypes, COL_APPOINTMENT_TYPE)));
    }
    Boolean isService = BeeUtils.unbox(qs.getBooleanById(VIEW_CALENDARS, calendarId,
        COL_CALENDAR_IS_SERVICE));

    if (isService) {
      appFilter.add(Filter.notNull(COL_SERVICE_EVENT));
    } else {
      appFilter.add(Filter.isNull(COL_SERVICE_EVENT));
    }
    long millis = System.currentTimeMillis();

    List<BeeRow> appointments = getAppointments(appFilter, null, false);
    if (!appointments.isEmpty()) {
      prepareAppointments(appointments);
    }
    long appDuration = System.currentTimeMillis() - millis;

    List<CalendarTask> tasks = isService ? Collections.emptyList()
        : getCalendarTasks(calendarId, startTime, endTime);
    long taskDuration = System.currentTimeMillis() - millis - appDuration;

    DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo();
    logger.info(reqInfo.getService(), calendarId,
        (startTime == null) ? BeeConst.STRING_EMPTY : Formatter.renderDateTime(dtfInfo, startTime),
        (endTime == null) ? BeeConst.STRING_EMPTY : Formatter.renderDateTime(dtfInfo, endTime),
        BeeConst.STRING_EQ, appointments.size(), BeeConst.STRING_PLUS, tasks.size(),
        BeeConst.STRING_LEFT_BRACKET, appDuration, BeeConst.STRING_PLUS, taskDuration,
        BeeConst.STRING_EQ, appDuration + taskDuration, BeeConst.STRING_RIGHT_BRACKET);

    Map<String, Object> result = new HashMap<>();
    if (!appointments.isEmpty()) {
      result.put(ItemType.APPOINTMENT.name(), appointments);
    }
    if (!tasks.isEmpty()) {
      result.put(ItemType.TASK.name(), tasks);
    }

    if (result.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private void prepareAppointments(Collection<BeeRow> appointments) {
    Map<Long, String> personAttendees = new HashMap<>();

    BeeRowSet attendees = qs.getViewData(VIEW_ATTENDEES, Filter.notNull(COL_COMPANY_PERSON));

    if (!DataUtils.isEmpty(attendees)) {
      int cpIndex = attendees.getColumnIndex(COL_COMPANY_PERSON);

      for (BeeRow row : attendees) {
        Long cp = row.getLong(cpIndex);
        if (DataUtils.isId(cp) && !personAttendees.containsKey(cp)) {
          personAttendees.put(cp, BeeUtils.toString(row.getId()));
        }
      }
    }

    if (!personAttendees.isEmpty()) {
      int cpIndex = sys.getView(VIEW_APPOINTMENTS).getRowSetIndex(ALS_CREATOR_COMPANY_PERSON);

      for (BeeRow row : appointments) {
        if (!row.isNull(cpIndex) && BeeUtils.isEmpty(row.getProperty(TBL_APPOINTMENT_ATTENDEES))) {
          String att = personAttendees.get(row.getLong(cpIndex));
          if (!BeeUtils.isEmpty(att)) {
            row.setProperty(TBL_APPOINTMENT_ATTENDEES, att);
          }
        }
      }
    }
  }

  private List<CalendarTask> getCalendarTasks(long calendarId, Long startMillis, Long endMillis) {
    List<CalendarTask> tasks = new ArrayList<>();

    BeeRowSet calRowSet = qs.getViewData(VIEW_CALENDARS, Filter.compareId(calendarId));
    if (DataUtils.isEmpty(calRowSet)) {
      logger.warning("calendar", calendarId, "not found");
      return tasks;
    }

    BeeRow calendar = calRowSet.getRow(0);
    Long calendarOwner = DataUtils.getLong(calRowSet, calendar, COL_CALENDAR_OWNER);

    boolean assigned;
    boolean delegated;
    boolean observed;

    if (DataUtils.isId(calendarOwner)) {
      assigned = BeeUtils.isTrue(DataUtils.getBoolean(calRowSet, calendar, COL_ASSIGNED_TASKS));
      delegated = BeeUtils.isTrue(DataUtils.getBoolean(calRowSet, calendar, COL_DELEGATED_TASKS));
      observed = BeeUtils.isTrue(DataUtils.getBoolean(calRowSet, calendar, COL_OBSERVED_TASKS));
    } else {
      assigned = false;
      delegated = false;
      observed = false;
    }

    Set<Long> executors = new HashSet<>();
    Map<Long, ColorAndStyle> executorAppearance = new HashMap<>();

    SimpleRowSet exGroupData = qs.getData(new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_USER_GROUPS, COL_UG_USER)
        .addFields(TBL_CAL_EXECUTOR_GROUPS, COL_BACKGROUND, COL_FOREGROUND, COL_STYLE)
        .addFrom(TBL_CAL_EXECUTOR_GROUPS)
        .addFromInner(TBL_USER_GROUPS,
            SqlUtils.join(TBL_CAL_EXECUTOR_GROUPS, COL_EXECUTOR_GROUP,
                TBL_USER_GROUPS, COL_UG_GROUP))
        .setWhere(SqlUtils.equals(TBL_CAL_EXECUTOR_GROUPS, COL_CALENDAR, calendarId)));

    if (!DataUtils.isEmpty(exGroupData)) {
      for (SimpleRow exRow : exGroupData) {
        Long user = exRow.getLong(COL_UG_USER);
        executors.add(user);

        ColorAndStyle cs = ColorAndStyle.maybeCreate(exRow.getValue(COL_BACKGROUND),
            exRow.getValue(COL_FOREGROUND), exRow.getLong(COL_STYLE));
        if (cs != null) {
          if (executorAppearance.containsKey(user)) {
            cs.merge(executorAppearance.get(user), false);
          }
          executorAppearance.put(user, cs);
        }
      }
    }

    SimpleRowSet executorData = qs.getData(new SqlSelect()
        .addFields(TBL_CALENDAR_EXECUTORS, COL_EXECUTOR_USER,
            COL_BACKGROUND, COL_FOREGROUND, COL_STYLE)
        .addFrom(TBL_CALENDAR_EXECUTORS)
        .setWhere(SqlUtils.equals(TBL_CALENDAR_EXECUTORS, COL_CALENDAR, calendarId)));

    if (!DataUtils.isEmpty(executorData)) {
      for (SimpleRow exRow : executorData) {
        Long user = exRow.getLong(COL_EXECUTOR_USER);
        executors.add(user);

        ColorAndStyle cs = ColorAndStyle.maybeCreate(exRow.getValue(COL_BACKGROUND),
            exRow.getValue(COL_FOREGROUND), exRow.getLong(COL_STYLE));
        if (cs != null) {
          if (executorAppearance.containsKey(user)) {
            cs.merge(executorAppearance.get(user), false);
          }
          executorAppearance.put(user, cs);
        }
      }
    }

    if (!assigned && !delegated && !observed && executors.isEmpty()) {
      return tasks;
    }

    EnumMap<TaskType, ColorAndStyle> typeAppearance = new EnumMap<>(TaskType.class);

    String source = TaskConstants.TBL_TASKS;
    String idName = sys.getIdName(source);

    HasConditions where = SqlUtils.and();
    where.add(SqlUtils.notEqual(source, TaskConstants.COL_STATUS, TaskStatus.CANCELED.ordinal()));

    if (startMillis != null) {
      where.add(SqlUtils.more(source, TaskConstants.COL_FINISH_TIME, startMillis));
    }
    if (endMillis != null) {
      where.add(SqlUtils.less(source, TaskConstants.COL_START_TIME, endMillis));
    }

    HasConditions match = SqlUtils.or();
    if (assigned) {
      match.add(SqlUtils.equals(source, TaskConstants.COL_EXECUTOR, calendarOwner));
      if (executors.contains(calendarOwner)) {
        executors.remove(calendarOwner);
      }

/*      ColorAndStyle cs = ColorAndStyle.maybeCreate(
          DataUtils.getString(calRowSet, calendar, COL_ASSIGNED_TASKS_BACKGROUND),
          DataUtils.getString(calRowSet, calendar, COL_ASSIGNED_TASKS_FOREGROUND),
          DataUtils.getLong(calRowSet, calendar, COL_ASSIGNED_TASKS_STYLE));
      if (cs != null) {
        typeAppearance.put(TaskType.ASSIGNED, cs);
      }*/
    }

    if (delegated) {
      if (executors.contains(calendarOwner)) {
        match.add(SqlUtils.equals(source, TaskConstants.COL_OWNER, calendarOwner));
      } else {
        match.add(SqlUtils.and(
            SqlUtils.equals(source, TaskConstants.COL_OWNER, calendarOwner),
            SqlUtils.notEqual(source, TaskConstants.COL_EXECUTOR, calendarOwner)));
      }

/*      ColorAndStyle cs = ColorAndStyle.maybeCreate(
          DataUtils.getString(calRowSet, calendar, COL_DELEGATED_TASKS_BACKGROUND),
          DataUtils.getString(calRowSet, calendar, COL_DELEGATED_TASKS_FOREGROUND),
          DataUtils.getLong(calRowSet, calendar, COL_DELEGATED_TASKS_STYLE));
      if (cs != null) {
        typeAppearance.put(TaskType.DELEGATED, cs);
      }*/
    }

    if (observed) {
      match.add(SqlUtils.and(
          SqlUtils.notEqual(source, TaskConstants.COL_OWNER, calendarOwner),
          SqlUtils.notEqual(source, TaskConstants.COL_EXECUTOR, calendarOwner),
          SqlUtils.in(source, idName, TaskConstants.TBL_TASK_USERS, TaskConstants.COL_TASK,
              SqlUtils
                  .equals(TaskConstants.TBL_TASK_USERS, COL_USER, calendarOwner))));

/*      ColorAndStyle cs = ColorAndStyle.maybeCreate(
          DataUtils.getString(calRowSet, calendar, COL_OBSERVED_TASKS_BACKGROUND),
          DataUtils.getString(calRowSet, calendar, COL_OBSERVED_TASKS_FOREGROUND),
          DataUtils.getLong(calRowSet, calendar, COL_OBSERVED_TASKS_STYLE));
      if (cs != null) {
        typeAppearance.put(TaskType.OBSERVED, cs);
      }*/
    }

    if (!executors.isEmpty()) {
      match.add(SqlUtils.inList(source, TaskConstants.COL_EXECUTOR, executors));
    }

    where.add(match);

    SqlSelect query = new SqlSelect()
        .addFields(source, idName, TaskConstants.COL_START_TIME, TaskConstants.COL_FINISH_TIME,
            TaskConstants.COL_SUMMARY, TaskConstants.COL_DESCRIPTION,
            TaskConstants.COL_PRIORITY, TaskConstants.COL_STATUS,
            TaskConstants.COL_OWNER, TaskConstants.COL_EXECUTOR)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addFields(TBL_TASK_TYPES, COL_BACKGROUND, COL_FOREGROUND)
        .addFrom(source)
        .addFromLeft(TBL_TASK_TYPES, sys.joinTables(TBL_TASK_TYPES, source, COL_TYPE))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, source, COL_COMPANY))
        .setWhere(where)
        .addOrder(source, TaskConstants.COL_START_TIME, TaskConstants.COL_FINISH_TIME, idName);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return tasks;
    }

    for (SimpleRow row : data) {
      Long id = row.getLong(idName);
      if (!DataUtils.isId(id)) {
        continue;
      }

      Long owner = row.getLong(TaskConstants.COL_OWNER);
      Long executor = row.getLong(TaskConstants.COL_EXECUTOR);

      Set<Long> observers = new HashSet<>();

      Long[] taskUsers = qs.getLongColumn(new SqlSelect()
          .addFields(TaskConstants.TBL_TASK_USERS, COL_USER)
          .addFrom(TaskConstants.TBL_TASK_USERS)
          .setWhere(SqlUtils.and(
              SqlUtils.equals(TaskConstants.TBL_TASK_USERS, TaskConstants.COL_TASK, id),
              SqlUtils.notEqual(TaskConstants.TBL_TASK_USERS, COL_USER, owner),
              SqlUtils.notEqual(TaskConstants.TBL_TASK_USERS, COL_USER,
                  executor))));

      if (taskUsers != null && taskUsers.length > 0) {
        for (Long user : taskUsers) {
          observers.add(user);
        }
      }

      TaskType type;
      if (!DataUtils.isId(calendarOwner)) {
        type = TaskType.ALL;
      } else if (Objects.equals(executor, calendarOwner)) {
        type = TaskType.ASSIGNED;
      } else if (Objects.equals(owner, calendarOwner)) {
        type = TaskType.DELEGATED;
      } else if (observers.contains(calendarOwner)) {
        type = TaskType.OBSERVED;
      } else {
        type = TaskType.ALL;
      }

      CalendarTask task = new CalendarTask(type, id, row);

      if (!observers.isEmpty()) {
        task.setObservers(observers);
      }

      ColorAndStyle cs;
      if (executor != null && executorAppearance.containsKey(executor)) {
        cs = executorAppearance.get(executor);
      } else {
        cs = ColorAndStyle.maybeCreate(row.getValue(COL_BACKGROUND), row.getValue(COL_FOREGROUND), null);
      }

      if (typeAppearance.containsKey(type)) {
        if (cs == null) {
          cs = typeAppearance.get(type);
        } else {
          cs = cs.copy();
          cs.merge(typeAppearance.get(type), false);
        }
      }

      if (cs != null) {
        task.setBackground(cs.getBackground());
        task.setForeground(cs.getForeground());
        task.setStyle(cs.getStyle());
      }

      tasks.add(task);
    }

    return tasks;
  }

  private ResponseObject getOverlappingAppointments(RequestInfo reqInfo) {
    String appId = reqInfo.getParameter(PARAM_APPOINTMENT_ID);

    String start = reqInfo.getParameter(PARAM_APPOINTMENT_START);
    if (!BeeUtils.isLong(start)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_APPOINTMENT_START);
    }
    String end = reqInfo.getParameter(PARAM_APPOINTMENT_END);
    if (!BeeUtils.isLong(end)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_APPOINTMENT_END);
    }

    List<Long> resources = DataUtils.parseIdList(reqInfo.getParameter(PARAM_ATTENDEES));
    if (resources.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_ATTENDEES);
    }

    CompoundFilter filter = Filter.and();
    filter.add(Filter.isNotEqual(COL_STATUS, IntegerValue.of(AppointmentStatus.CANCELED)));

    if (BeeUtils.isLong(appId)) {
      filter.add(Filter.compareId(Operator.NE, BeeUtils.toLong(appId)));
    }

    filter.add(Filter.isMore(COL_END_DATE_TIME, new LongValue(BeeUtils.toLong(start))));
    filter.add(Filter.isLess(COL_START_DATE_TIME, new LongValue(BeeUtils.toLong(end))));

    filter.add(Filter.in(sys.getIdName(TBL_APPOINTMENTS), VIEW_APPOINTMENT_ATTENDEES,
        COL_APPOINTMENT, Filter.any(COL_ATTENDEE, resources)));

    BeeRowSet appointments = qs.getViewData(VIEW_APPOINTMENTS, filter);
    return ResponseObject.response(appointments);
  }

  private ResponseObject getReportOptions(RequestInfo reqInfo) {
    Integer report = BeeUtils.toIntOrNull(reqInfo.getParameter(PARAM_REPORT));
    if (!EnumUtils.isOrdinal(Report.class, report)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_REPORT);
    }

    long userId = usr.getCurrentUserId();

    Filter filter = Filter.and(Filter.equals(COL_USER, userId),
        Filter.isEqual(COL_REPORT, new IntegerValue(report)));

    BeeRowSet rowSet = qs.getViewData(VIEW_REPORT_OPTIONS, filter);
    if (rowSet.getNumberOfRows() == 1) {
      return ResponseObject.response(rowSet.getRow(0));
    }

    SqlInsert sqlInsert = new SqlInsert(TBL_REPORT_OPTIONS).addConstant(COL_USER, userId)
        .addConstant(COL_REPORT, report);

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return response;
    }

    BeeRowSet result = qs.getViewData(VIEW_REPORT_OPTIONS, filter);
    if (result.getNumberOfRows() != 1) {
      return ResponseObject.error(reqInfo.getService(), COL_USER, userId, COL_REPORT, report,
          "report options not created");
    }
    return ResponseObject.response(result.getRow(0));
  }

  private BeeRowSet getUserCalAttendees(long ucId, long userId, long calendarId, boolean isNew) {
    BeeRowSet attendees = qs.getViewData(VIEW_ATTENDEES);
    if (DataUtils.isEmpty(attendees)) {
      logger.warning("attendees not available, user", userId);
      return null;
    }

    List<Long> attIds = attendees.getRowIds();

    Filter filter = Filter.and(Filter.equals(COL_USER_CALENDAR, ucId),
        Filter.any(COL_ATTENDEE, attIds));

    if (!isNew) {
      BeeRowSet ucAttendees = qs.getViewData(VIEW_USER_CAL_ATTENDEES, filter);
      if (!ucAttendees.isEmpty()) {
        return ucAttendees;
      }
    }

    BeeRowSet calendarAttendees = getCalendarAttendees(userId, calendarId);
    if (DataUtils.isEmpty(calendarAttendees)) {
      logger.warning("calendar attendees not available, calendar", calendarId, "user", userId);
      return null;
    }

    int ord = 0;
    for (BeeRow row : calendarAttendees.getRows()) {
      SqlInsert sqlInsert = new SqlInsert(TBL_USER_CAL_ATTENDEES)
          .addConstant(COL_USER_CALENDAR, ucId)
          .addConstant(COL_ATTENDEE, row.getId())
          .addConstant(COL_ENABLED, true)
          .addConstant(COL_ORDINAL, ord++);

      qs.insertDataWithResponse(sqlInsert);
    }

    return qs.getViewData(VIEW_USER_CAL_ATTENDEES, filter);
  }

  private ResponseObject getUserCalendar(RequestInfo reqInfo) {
    if (!checkTable(TBL_USER_CALENDARS)) {
      return ResponseObject.error("table not active:", TBL_USER_CALENDARS);
    }

    long calendarId = BeeUtils.toLong(reqInfo.getParameter(PARAM_CALENDAR_ID));
    if (!DataUtils.isId(calendarId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_CALENDAR_ID);
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(reqInfo.getService(), "current user not available");
    }

    Filter filter = Filter.and(Filter.equals(COL_CALENDAR, calendarId),
        Filter.equals(COL_USER, userId));

    BeeRowSet ucRowSet = qs.getViewData(VIEW_USER_CALENDARS, filter);
    if (!ucRowSet.isEmpty()) {
      BeeRow row = ucRowSet.getRow(0);
      BeeRowSet ucAttendees = getUserCalAttendees(row.getId(), userId, calendarId, false);
      if (!DataUtils.isEmpty(ucAttendees)) {
        row.setProperty(TBL_USER_CAL_ATTENDEES, ucAttendees.serialize());
      }

      return ResponseObject.response(ucRowSet);
    }

    BeeRowSet calRowSet = qs.getViewData(VIEW_CALENDARS, Filter.compareId(calendarId));
    if (calRowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), PARAM_CALENDAR_ID, calendarId,
          "calendar not found");
    }

    CalendarSettings settings = CalendarSettings.create(calRowSet.getRow(0),
        calRowSet.getColumns());

    SqlInsert sqlInsert = new SqlInsert(TBL_USER_CALENDARS)
        .addConstant(COL_CALENDAR, calendarId)
        .addConstant(COL_USER, userId)
        .addConstant(COL_PIXELS_PER_INTERVAL, settings.getPixelsPerInterval())
        .addConstant(COL_INTERVALS_PER_HOUR, settings.getIntervalsPerHour())
        .addConstant(COL_WORKING_HOUR_START, settings.getWorkingHourStart())
        .addConstant(COL_WORKING_HOUR_END, settings.getWorkingHourEnd())
        .addConstant(COL_SCROLL_TO_HOUR, settings.getScrollToHour())
        .addConstant(COL_DEFAULT_DISPLAYED_DAYS, settings.getDefaultDisplayedDays());

    if (settings.getTimeBlockClickNumber() != null) {
      sqlInsert.addConstant(COL_TIME_BLOCK_CLICK_NUMBER,
          settings.getTimeBlockClickNumber().ordinal());
    }

    if (settings.getMultidayLayout() != null) {
      sqlInsert.addConstant(COL_MULTIDAY_LAYOUT, settings.getMultidayLayout().ordinal());
    }
    if (settings.getMultidayTaskLayout() != null) {
      sqlInsert.addConstant(COL_MULTIDAY_TASK_LAYOUT, settings.getMultidayTaskLayout().ordinal());
    }

    for (ViewType view : ViewType.values()) {
      if (settings.isVisible(view)) {
        sqlInsert.addConstant(view.getColumnId(), true);
      }
    }

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return response;
    }

    BeeRowSet result = qs.getViewData(VIEW_USER_CALENDARS, filter);
    if (result.isEmpty()) {
      return ResponseObject.error(SVC_GET_USER_CALENDAR, PARAM_CALENDAR_ID, calendarId,
          "user calendar not created");
    }

    BeeRow row = result.getRow(0);
    BeeRowSet ucAttendees = getUserCalAttendees(row.getId(), userId, calendarId, true);
    if (!DataUtils.isEmpty(ucAttendees)) {
      row.setProperty(TBL_USER_CAL_ATTENDEES, ucAttendees.serialize());
    }

    return ResponseObject.response(result);
  }

  @Override
  public void onTimeout(String timerInfo) {
    if (BeeUtils.isPrefix(timerInfo, TIMER_REMIND_CALENDAR_EVENTS)) {
      Long reminderId = BeeUtils.toLong(
          BeeUtils.removePrefix(timerInfo, TIMER_REMIND_CALENDAR_EVENTS));
      logger.debug("Fired timer:", reminderId);

      if (DataUtils.isId(reminderId)) {
        generateAppointmentEmails(reminderId);
      }
    }
  }

  private void generateAppointmentEmails(Long reminderId) {
    SimpleRow data = getAppointmentRemindData(reminderId);
    IsCondition wh = sys.idEquals(TBL_APPOINTMENT_REMINDERS, reminderId);
    if (data != null) {
      Dictionary dic = usr.getDictionary();
      String error = null;
      String idName = sys.getIdName(TBL_APPOINTMENTS);
      String subject = data.getValue(COL_REMINDER_TEMPLATE_CAPTION);
      String template = BeeUtils.notEmpty(data.getValue(COL_MESSAGE),
          data.getValue(COL_REMINDER_TEMPLATE));

      if (BeeUtils.isEmpty(subject)) {
        error = "No reminder caption specified";
      }
      if (BeeUtils.isEmpty(error) && BeeUtils.isEmpty(template)) {
        error = "No reminder message specified";
      }

      SimpleRowSet recipientsRowSet =
          getAppointmentAttendeeEmails(data.getLong(sys.getIdName(TBL_APPOINTMENTS)));

      if (BeeUtils.isEmpty(error)) {
        ReminderMethod method = EnumUtils.getEnumByIndex(ReminderMethod.class,
            data.getInt(COL_REMINDER_METHOD));

        if (method == ReminderMethod.EMAIL) {
          String email = data.getValue(ALS_OWNER_EMAIL);

          Set<String> emails = new HashSet<>();
          if (recipientsRowSet != null) {
            Collections.addAll(emails, recipientsRowSet.getColumn(COL_EMAIL));
          }
          if (!BeeUtils.isEmpty(email)) {
            emails.add(email);
          }

          if (BeeUtils.isEmpty(emails)) {
            error = "No recipients e-mail found";
          }

          List<String> exclCols = Lists.newArrayList(ALS_OWNER_EMAIL, COL_APPOINTMENT, COL_MESSAGE,
              COL_REMINDER_METHOD, COL_REMINDER_TEMPLATE, COL_REMINDER_TEMPLATE_CAPTION,
              sys.getIdName(TBL_USERS), COL_REMINDER_TYPE);
          Map<String, String> labels = CalendarHelper.getAppointmentReminderDataLabels(dic, idName);
          Map<String, ValueType> format = CalendarHelper.getAppointmentReminderDataTypes(idName);
          Map<String, String> enumKeys = CalendarHelper.getAppointmentReminderDataEnumKeys();

          Document content = cmb.createRemindTemplate(data.getRowSet(),
              labels, format, enumKeys, exclCols, BeeConst.STRING_EMPTY,
              template, usr.getCurrentUserId());

          String contentLines = content.buildLines();

          for (String recipient : emails) {
            error = sendAppointmentReminderEmail(recipient, subject, contentLines);

            if (!BeeUtils.isEmpty(error)) {
              break;
            }
          }
        } else {
          error = "Unsupported reminder method: " + method;
        }
      }

      if (DataUtils.isId(data.getValue(COL_REMINDER_TYPE))) {
        Set<String> userIds = new HashSet<>();
        if (recipientsRowSet != null) {
          Collections.addAll(userIds, recipientsRowSet.getColumn(sys.getIdName(TBL_USERS)));
        }
        userIds.add(data.getValue(sys.getIdName(TBL_USERS)));

        for (String user : userIds) {
          Long userId = BeeUtils.toLong(user);
          Map<String, String> linkData = new HashMap<>();
          linkData.put(VIEW_APPOINTMENTS, data.getValue(sys.getIdName(TBL_APPOINTMENTS)));

          if (DataUtils.isId(userId)) {
            Dictionary dictionary = usr.getDictionary(userId);
            DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo(userId);

            chat.putMessage(
                BeeUtils.joinWords(dictionary.event(),
                    BeeConst.STRING_QUOT + data.getValue(COL_SUMMARY) + BeeConst.STRING_QUOT
                        + BeeConst.STRING_POINT,
                    dictionary.scheduledStartingTime() + BeeConst.STRING_COLON,
                    Formatter.renderDateTime(dtfInfo, data.getDateTime(COL_START_DATE_TIME))
                        + BeeConst.STRING_POINT),
                userId,
                linkData);
          }
        }
      }

      if (!BeeUtils.isEmpty(error)) {
        logger.severe(error);
      }
      qs.updateData(new SqlUpdate(TBL_APPOINTMENT_REMINDERS)
          .addConstant(COL_SENT, System.currentTimeMillis())
          .addConstant(COL_ERROR, error)
          .setWhere(wh));
    }
  }

  private ResponseObject saveActiveView(RequestInfo reqInfo) {
    Long rowId = BeeUtils.toLongOrNull(reqInfo.getParameter(PARAM_USER_CALENDAR_ID));
    if (!DataUtils.isId(rowId)) {
      return ResponseObject.parameterNotFound(SVC_SAVE_ACTIVE_VIEW, PARAM_USER_CALENDAR_ID);
    }

    Integer activeView = BeeUtils.toIntOrNull(reqInfo.getParameter(PARAM_ACTIVE_VIEW));
    if (!EnumUtils.isOrdinal(ViewType.class, activeView)) {
      return ResponseObject.parameterNotFound(SVC_SAVE_ACTIVE_VIEW, PARAM_ACTIVE_VIEW);
    }

    SqlUpdate update = new SqlUpdate(TBL_USER_CALENDARS).addConstant(COL_ACTIVE_VIEW, activeView)
        .setWhere(sys.idEquals(TBL_USER_CALENDARS, rowId));

    return qs.updateDataWithResponse(update);
  }

  private String sendAppointmentReminderEmail(String email, String subject, String message) {
    String error = BeeConst.STRING_EMPTY;

    Long account = mail.getSenderAccountId("CalendarRemainders");

    if (!DataUtils.isId(account)) {
      error = "No default account specified";
    } else if (BeeUtils.isEmpty(email)) {
      error = "No recipient email address specified";
    } else {
      ResponseObject response = mail.sendMail(account, email, subject, message);

      if (response.hasErrors()) {
        error = ArrayUtils.toString(response.getErrors());
      }
    }

    return error;
  }

  private void setAppopintmentProperties(BeeRowSet rowSet) {
    List<Long> appIds = rowSet.getRowIds();

    for (Map.Entry<String, String> entry : APPOINTMENT_CHILDREN.entrySet()) {
      String source = entry.getKey();
      String field = entry.getValue();

      SqlSelect query = new SqlSelect()
          .addFields(source, COL_APPOINTMENT, field)
          .addFrom(source)
          .setWhere(SqlUtils.inList(source, COL_APPOINTMENT, appIds))
          .addOrder(source, COL_APPOINTMENT, field);

      SimpleRowSet data = qs.getData(query);

      if (!DataUtils.isEmpty(data)) {
        Multimap<Long, Long> children = ArrayListMultimap.create();
        for (SimpleRow childRow : data) {
          children.put(childRow.getLong(0), childRow.getLong(1));
        }

        for (BeeRow row : rowSet) {
          if (children.containsKey(row.getId())) {
            row.setProperty(source, DataUtils.buildIdList(children.get(row.getId())));
          }
        }
      }
    }
  }

  private ResponseObject updateAppointment(RequestInfo reqInfo) {
    BeeRowSet newRowSet = BeeRowSet.restore(reqInfo.getContent());
    if (newRowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), ": rowSet is empty");
    }

    long appId = newRowSet.getRow(0).getId();
    if (!DataUtils.isId(appId)) {
      return ResponseObject.error(reqInfo.getService(), ": invalid row id", appId);
    }

    String viewName = VIEW_APPOINTMENTS;

    Collection<RowChildren> children = new ArrayList<>();
    final String propChildren = "_row_children";

    BeeRowSet oldRowSet = qs.getViewData(viewName, Filter.compareId(appId));
    if (oldRowSet == null || oldRowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), ": old row not found", appId);
    }

    BeeRow oldRow = oldRowSet.getRow(0);

    if (newRowSet.getRow(0).hasPropertyValue(propChildren)) {
      String serialized = newRowSet.getRow(0).getProperty(propChildren);

      String[] arr = Codec.beeDeserializeCollection(serialized);
      if (!ArrayUtils.isEmpty(arr)) {
        for (String s : arr) {
          children.add(RowChildren.restore(s));
        }
      }
    }

    BeeRowSet updated = DataUtils.getUpdated(viewName, oldRowSet.getColumns(), oldRow,
        newRowSet.getRow(0), children);

    ResponseObject response;
    if (updated == null) {
      if (!children.isEmpty()) {
        usb.updateRelatedValues(viewName, appId, Codec.beeSerialize(children));
      }

      response = ResponseObject.response(oldRow);
    } else {
      response = deb.commitRow(updated);
      if (response.hasErrors()) {
        return response;
      }
    }

    boolean childrenChanged = false;
    boolean updateReminders = false;

    for (Map.Entry<String, String> entry : APPOINTMENT_CHILDREN.entrySet()) {
      String table = entry.getKey();
      String column = entry.getValue();

      List<Long> oldValues = DataUtils.parseIdList(oldRow.getProperty(table));
      List<Long> newValues = DataUtils.parseIdList(newRowSet.getTableProperty(table));

      if (TBL_APPOINTMENT_REMINDERS.equals(table)) {
        updateReminders = !oldValues.isEmpty() || !newValues.isEmpty();
      }

      if (!BeeUtils.sameElements(oldValues, newValues)
          && updateChildren(table, COL_APPOINTMENT, appId, column, oldValues, newValues)) {
        childrenChanged = true;
      }
    }

    if (childrenChanged) {
      news.maybeRecordUpdate(VIEW_APPOINTMENTS, appId);
    }
    if (updateReminders) {
      createOrUpdateTimers(TIMER_REMIND_CALENDAR_EVENTS, TBL_APPOINTMENTS, appId);
    }

    return response;
  }

  private boolean updateChildren(String tblName, String parentRelation, long parentId,
      String columnId, List<Long> oldValues, List<Long> newValues) {

    List<Long> insert = new ArrayList<>(newValues);
    insert.removeAll(oldValues);

    List<Long> delete = new ArrayList<>(oldValues);
    delete.removeAll(newValues);

    if (insert.isEmpty() && delete.isEmpty()) {
      return false;

    } else {
      for (Long value : insert) {
        qs.insertData(new SqlInsert(tblName).addConstant(parentRelation, parentId)
            .addConstant(columnId, value));
      }
      for (Long value : delete) {
        IsCondition condition = SqlUtils.equals(tblName, parentRelation, parentId, columnId, value);
        qs.updateData(new SqlDelete(tblName).setWhere(condition));
      }

      return true;
    }
  }
}
