package com.butent.bee.server.modules.tasks;

import com.butent.bee.client.output.ReportDateItem;
import com.butent.bee.client.output.ReportTextItem;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.composite.Relations;
import com.butent.bee.server.Config;
import com.butent.bee.server.communication.ChatBean;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SearchBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.AdministrationModuleBean;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.modules.classifiers.ClassifiersModuleBean;
import com.butent.bee.server.modules.classifiers.TimerBuilder;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.ExtendedUsageQueryProvider;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
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
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.html.builder.elements.Td;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.CronExpression;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.ScheduleDateRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.WorkdayTransition;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Singleton
@Lock(LockType.READ)
@LocalBean
public class TasksModuleBean extends TimerBuilder implements BeeModule {

  private static final String COL_DELAYED_HOURS = "DelayedHours";
  private static BeeLogger logger = LogUtils.getLogger(TasksModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  DataEditorBean deb;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  NewsBean news;

  @EJB
  ParamHolderBean prm;
  @EJB
  MailModuleBean mail;
  @EJB
  AdministrationModuleBean adm;
  @EJB
  ClassifiersModuleBean cls;
  @EJB
  SearchBean src;
  @EJB
  ChatBean chat;
  @EJB
  UiServiceBean usb;

  @Resource
  EJBContext ctx;

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    if (!usr.isMenuVisible(MENU_TASKS)) {
      return result;
    }

    Filter queryTaskSearch = src.buildSearchFilter(VIEW_TASKS, Sets.newHashSet(COL_ID, COL_SUMMARY,
        COL_DESCRIPTION, ALS_COMPANY_NAME, ALS_EXECUTOR_FIRST_NAME, ALS_EXECUTOR_LAST_NAME), query);

    Filter queryTaskEventSearch =
        src.buildSearchFilter(VIEW_TASK_EVENTS, Collections.singleton(COL_COMMENT), query);

    Long userId = usr.getCurrentUserId();

    Filter taskFilter =
        Filter.or(Filter.and(queryTaskSearch, Filter.isNull(COL_PRIVATE_TASK)), Filter.and(
            queryTaskSearch, Filter.notNull(COL_PRIVATE_TASK), Filter.or(Filter.equals(COL_OWNER,
                userId), Filter.equals(COL_EXECUTOR, userId), Filter.in(COL_TASK_ID,
                VIEW_TASK_USERS, COL_TASK, Filter
                    .equals(AdministrationConstants.COL_USER, userId)))));

    result.addAll(qs.getSearchResults(VIEW_TASKS, taskFilter));

    Filter taskEventFilter =
        Filter.or(Filter.and(queryTaskEventSearch, Filter.in(COL_TASK, VIEW_TASKS, COL_TASK_ID,
            Filter.isNull(COL_PRIVATE_TASK))), Filter.and(
            queryTaskEventSearch, Filter.in(COL_TASK, VIEW_TASKS, COL_TASK_ID, Filter.and(Filter
                .notNull(COL_PRIVATE_TASK), Filter.or(Filter.equals(COL_OWNER,
                userId), Filter.equals(COL_EXECUTOR, userId), Filter.in(COL_TASK_ID,
                VIEW_TASK_USERS, COL_TASK, Filter
                    .equals(AdministrationConstants.COL_USER, userId)))))));

    result.addAll(qs.getSearchResults(VIEW_TASK_EVENTS, taskEventFilter));

    List<SearchResult> rtSr = qs.getSearchResults(VIEW_RECURRING_TASKS,
        Filter.anyContains(Sets.newHashSet(COL_SUMMARY, COL_DESCRIPTION, ALS_COMPANY_NAME), query));
    result.addAll(rtSr);

    List<SearchResult> toSr = qs.getSearchResults(VIEW_TASK_ORDERS,
        Filter.anyContains(Sets.newHashSet(COL_SUMMARY, ALS_COMPANY_NAME,
            TransportConstants.COL_ORDER_NO), query));
    result.addAll(toSr);

    List<SearchResult> tfSr = qs.getSearchResults(VIEW_TASK_FILES,
        Filter.anyContains(Sets.newHashSet(AdministrationConstants.COL_FILE_CAPTION,
            AdministrationConstants.ALS_FILE_NAME), query));
    result.addAll(tfSr);

    return result;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;
    Map<String, String> reqMap = new HashMap<>();

    if (!BeeUtils.isEmpty(reqInfo.getParams())) {
      reqMap.putAll(reqInfo.getParams());
    }
    if (!BeeUtils.isEmpty(reqInfo.getHeaders())) {
      reqMap.putAll(reqInfo.getHeaders());
    }
    if (!BeeUtils.isEmpty(reqInfo.getVars())) {
      reqMap.putAll(reqInfo.getVars());
    }
    if (BeeUtils.isPrefix(svc, CRM_TASK_PREFIX)) {
      response = doTaskEvent(BeeUtils.removePrefix(svc, CRM_TASK_PREFIX), reqMap);

    } else if (BeeUtils.same(svc, SVC_ACCESS_TASK)) {
      response = accessTask(BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_TASK_ID)));

    } else if (BeeUtils.same(svc, SVC_GET_TASK_DATA)) {
      response = getTaskData(reqInfo);

    } else if (BeeUtils.same(svc, SVC_EXTEND_TASK)) {
      response = extendTask(reqMap);

    } else if (BeeUtils.same(svc, SVC_GET_CHANGED_TASKS)) {
      response = getChangedTasks();

    } else if (BeeUtils.same(svc, SVC_TASKS_REPORTS_COMPANY_TIMES)) {
      response = getCompanyTimesReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_TASKS_REPORTS_TYPE_HOURS)) {
      response = getTypeHoursReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_TASKS_REPORTS_USERS_HOURS)) {
      response = getUsersHoursReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_REQUEST_FILES)) {
      response = getRequestFiles(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_REQUEST)));

    } else if (BeeUtils.same(svc, SVC_FINISH_REQUEST_WITH_TASK)) {
      response = finishRequestWithTask(reqInfo);

    } else if (BeeUtils.same(svc, SVC_RT_GET_SCHEDULING_DATA)) {
      response = getSchedulingData(reqInfo);

    } else if (BeeUtils.same(svc, SVC_RT_SPAWN)) {
      response = spawnTasks(reqInfo);

    } else if (BeeUtils.same(svc, SVC_RT_COPY)) {
      response = copyRecurringTask(reqInfo);

    } else if (BeeUtils.same(svc, SVC_RT_SCHEDULE)) {
      response = scheduleTasks(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CONFIRM_TASKS)) {
      response = confirmTasks(reqInfo);

    } else if (BeeUtils.same(svc, SVC_TASK_REPORT)) {
        response = getReportData(reqInfo);

    } else if (BeeUtils.same(svc, SVC_TASK_REPORT + 2)) {
        response = getReportData2(reqInfo);

    } else if (BeeUtils.same(svc, SVC_COPY_TASK_ORDER)) {
      BeeRowSet taskData = BeeRowSet.restore(reqInfo.getParameter(VAR_TASK_DATA));
      response = createTasks(taskData, taskData.getRow(0), usr.getCurrentUserId());

    } else {
      String msg = BeeUtils.joinWords("CRM service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();
    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createTimeOfDay(module, PRM_END_OF_WORK_DAY),
        BeeParameter.createTimeOfDay(module, PRM_START_OF_WORK_DAY),
        BeeParameter.createRelation(module, PRM_DEFAULT_DBA_TEMPLATE,
            DocumentConstants.VIEW_DOCUMENT_TEMPLATES,
            DocumentConstants.COL_DOCUMENT_TEMPLATE_NAME),
        BeeParameter.createRelation(module, PRM_DEFAULT_DBA_DOCUMENT_TYPE,
            DocumentConstants.VIEW_DOCUMENT_TYPES, DocumentConstants.COL_DOCUMENT_TYPE_NAME),
        BeeParameter.createBoolean(module, PRM_CREATE_PRIVATE_TASK_FIRST, true, null));

    return params;
  }

  @Override
  public Module getModule() {
    return Module.TASKS;
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
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_TASKS, VIEW_RELATED_TASKS) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();
          Set<Long> taskIds = new HashSet<>();
          Long id;

          if (rowSet.getNumberOfRows() < 100) {
            for (BeeRow row : rowSet.getRows()) {
              switch (event.getTargetName()) {
                case VIEW_TASKS:
                  id = row.getId();
                  break;
                case VIEW_RELATED_TASKS:
                  id = row.getLong(rowSet.getColumnIndex(COL_TASK));
                  break;
                default:
                  id = null;
              }

              if (DataUtils.isId(id)) {
                taskIds.add(id);
              }
            }
          }

          Long userId = usr.getCurrentUserId();

          SqlSelect tuQuery = new SqlSelect().addFrom(TBL_TASK_USERS)
              .addFields(TBL_TASK_USERS, COL_TASK, COL_LAST_ACCESS, COL_STAR);

          IsCondition uwh = SqlUtils.equals(TBL_TASK_USERS, COL_USER, userId);

          if (taskIds.isEmpty()) {
            tuQuery.setWhere(uwh);
          } else {
            tuQuery.setWhere(SqlUtils.and(uwh,
                SqlUtils.inList(TBL_TASK_USERS, COL_TASK, taskIds)));
          }

          SimpleRowSet tuData = qs.getData(tuQuery);
          int taskIndex = tuData.getColumnIndex(COL_TASK);
          int accessIndex = tuData.getColumnIndex(COL_LAST_ACCESS);
          int starIndex = tuData.getColumnIndex(COL_STAR);

          for (SimpleRow tuRow : tuData) {
            long taskId = tuRow.getLong(taskIndex);
            BeeRow row = event.isTarget(VIEW_RELATED_TASKS)
                ? rowSet.findRow(Filter.equals(COL_TASK, taskId)) : rowSet.getRowById(taskId);

            if (row != null) {
              row.setProperty(PROP_USER, userId, BeeConst.STRING_PLUS);

              row.setProperty(PROP_LAST_ACCESS, userId, tuRow.getValue(accessIndex));
              row.setProperty(PROP_STAR, userId, tuRow.getValue(starIndex));
            }
          }

          SqlSelect teQuery = new SqlSelect().addFrom(TBL_TASK_EVENTS)
              .addFields(TBL_TASK_EVENTS, COL_TASK)
              .addMax(TBL_TASK_EVENTS, COL_PUBLISH_TIME)
              .addGroup(TBL_TASK_EVENTS, COL_TASK);

          if (!taskIds.isEmpty()) {
            teQuery.setWhere(SqlUtils.inList(TBL_TASK_EVENTS, COL_TASK, taskIds));
          }

          SimpleRowSet teData = qs.getData(teQuery);
          taskIndex = teData.getColumnIndex(COL_TASK);
          int publishIndex = teData.getColumnIndex(COL_PUBLISH_TIME);

          for (SimpleRow teRow : teData) {
            if (teRow.getValue(publishIndex) != null) {
              long taskId = teRow.getLong(taskIndex);
              BeeRow row = event.isTarget(VIEW_RELATED_TASKS)
                  ? rowSet.findRow(Filter.equals(COL_TASK, taskId)) : rowSet.getRowById(taskId);

              if (row != null) {
                row.setProperty(PROP_LAST_PUBLISH, teRow.getValue(publishIndex));
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillTasksTimeData(ViewQueryEvent event) {
        if (event.isAfter(VIEW_TASKS) && event.hasData()) {
          BeeRowSet taskRows = event.getRowset();

          int idxActualDuration = DataUtils.getColumnIndex(COL_ACTUAL_DURATION,
              taskRows.getColumns(), false);
          int idxActualExpenses = DataUtils.getColumnIndex(COL_ACTUAL_EXPENSES,
              taskRows.getColumns(), false);

          List<Long> rowIds = taskRows.getRowIds();

          SimpleRowSet timesData = getTaskActualTimesAndExpenses(rowIds);

          if (timesData.isEmpty()) {
            return;
          }

          Map<String, String> times =
              Codec.deserializeLinkedHashMap(timesData.getValue(0, COL_ACTUAL_DURATION));
          Map<String, String> expenses =
              Codec.deserializeLinkedHashMap(timesData.getValue(0, COL_ACTUAL_EXPENSES));

          for (BeeRow row : taskRows) {
            if (row == null) {
              continue;
            }

            if (!BeeUtils.isNegative(idxActualDuration)) {
              row.setValue(idxActualDuration, times.get(BeeUtils.toString(row.getId())));
            }

            if (!BeeUtils.isNegative(idxActualExpenses)) {
              row.setValue(idxActualExpenses, expenses.get(BeeUtils.toString(row.getId())));
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateTimers(DataEvent.ViewModifyEvent event) {
        if (event.isAfter(TBL_USERS)) {
          if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_USER_BLOCK_FROM)
                || DataUtils.contains(ev.getColumns(), COL_USER_BLOCK_UNTIL)) {
              createOrUpdateTimers(TIMER_REMIND_TASKS_SUMMARY, TBL_USERS, ev.getRow().getId());
            }
          } else if (event instanceof DataEvent.ViewDeleteEvent) {
            for (long id : ((DataEvent.ViewDeleteEvent) event).getIds()) {
              createOrUpdateTimers(TIMER_REMIND_TASKS_SUMMARY, TBL_USERS, id);
            }
          }
        }

        if (event.isAfter(TBL_USER_SETTINGS)) {
          if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;

            if (DataUtils.contains(ev.getColumns(), COL_TASKS_MAILING_TIME)) {
              createOrUpdateTimers(TIMER_REMIND_TASKS_SUMMARY, TBL_USER_SETTINGS,
                  ev.getRow().getId());
            }
          } else if (event instanceof DataEvent.ViewInsertEvent) {
            createOrUpdateTimers(TIMER_REMIND_TASKS_SUMMARY, TBL_USER_SETTINGS,
                    ((DataEvent.ViewInsertEvent) event).getRow().getId());
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateRemindersTimers(DataEvent.ViewModifyEvent event) {
        if (event.isAfter(TBL_REMINDER_TYPES)) {
          if (event instanceof DataEvent.ViewDeleteEvent) {
            createOrUpdateTimers(TIMER_REMIND_USER_TASKS, null, null);
          } else if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_REMINDER_DATA_FIELD)
                || DataUtils.contains(ev.getColumns(), COL_REMINDER_DATA_INDICATOR)
                || DataUtils.contains(ev.getColumns(), COL_REMINDER_HOURS)
                || DataUtils.contains(ev.getColumns(), COL_REMINDER_MINUTES)) {
              createOrUpdateTimers(TIMER_REMIND_USER_TASKS, TBL_REMINDER_TYPES,
                  ev.getRow().getId());
            }
          }
        }
        if (event.isAfter(VIEW_USER_REMINDERS)) {
          if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_USER_REMINDER_TIME)
                || DataUtils.contains(ev.getColumns(), COL_USER_REMINDER_TYPE)
                || DataUtils.contains(ev.getColumns(), COL_USER_REMINDER_ACTIVE)) {
              createOrUpdateTimers(TIMER_REMIND_USER_TASKS, VIEW_USER_REMINDERS,
                  ev.getRow().getId());
            }
          } else if (event instanceof DataEvent.ViewInsertEvent) {
            createOrUpdateTimers(TIMER_REMIND_USER_TASKS, VIEW_USER_REMINDERS,
                ((DataEvent.ViewInsertEvent) event).getRow().getId());
          }
        } else if (event.isAfter(TBL_TASKS)) {
          if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_START_TIME)
                || DataUtils.contains(ev.getColumns(), COL_FINISH_TIME)
                || DataUtils.contains(ev.getColumns(), COL_STATUS)) {
              createOrUpdateTimers(TIMER_REMIND_USER_TASKS, TBL_TASKS, ev.getRow().getId());
            }
          }
        }
      }
    });

    TaskUsageQueryProvider usageQueryProvider = new TaskUsageQueryProvider();

    news.registerUsageQueryProvider(Feed.TASKS_ALL, usageQueryProvider);
    news.registerUsageQueryProvider(Feed.TASKS_ASSIGNED, usageQueryProvider);
    news.registerUsageQueryProvider(Feed.TASKS_DELEGATED, usageQueryProvider);
    news.registerUsageQueryProvider(Feed.TASKS_OBSERVED, usageQueryProvider);
    news.registerUsageQueryProvider(Feed.REQUESTS_ASSIGNED, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.equals(TBL_REQUESTS, COL_REQUEST_MANAGER,
            userId));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_REQUESTS, news.joinUsage(TBL_REQUESTS));
      }
    });

    HeadlineProducer headlineProducer = (feed, userId, rowSet, row, isNew, constants, dtfInfo) -> {
      String caption = DataUtils.getString(rowSet, row, COL_SUMMARY);
      if (BeeUtils.isEmpty(caption)) {
        caption = BeeUtils.bracket(row.getId());
      }

      List<String> subtitles = new ArrayList<>();

      DateTime finish = DataUtils.getDateTime(rowSet, row, COL_FINISH_TIME);
      if (finish != null) {
        subtitles.add(Formatter.renderDateTime(dtfInfo, finish));
      }

      TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class,
          DataUtils.getInteger(rowSet, row, COL_STATUS));
      if (status != null) {
        subtitles.add(status.getCaption(constants));
      }

      if (feed != Feed.TASKS_ASSIGNED) {
        subtitles.add(BeeUtils.joinWords(
            DataUtils.getString(rowSet, row, ALS_EXECUTOR_FIRST_NAME),
            DataUtils.getString(rowSet, row, ALS_EXECUTOR_LAST_NAME)));
      }

      return Headline.create(row.getId(), caption, subtitles, isNew);
    };

    news.registerHeadlineProducer(Feed.TASKS_ALL, headlineProducer);
    news.registerHeadlineProducer(Feed.TASKS_ASSIGNED, headlineProducer);
    news.registerHeadlineProducer(Feed.TASKS_DELEGATED, headlineProducer);
    news.registerHeadlineProducer(Feed.TASKS_OBSERVED, headlineProducer);

    BeeView.registerConditionProvider(FILTER_TASKS_NEW, new BeeView.ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        Long userId = usr.getCurrentUserId();
        if (userId == null) {
          return null;
        }

        return SqlUtils.in(TBL_TASKS, COL_TASK_ID, TBL_TASK_USERS, COL_TASK,
            SqlUtils.and(SqlUtils.equals(TBL_TASK_USERS, COL_USER, userId),
                SqlUtils.isNull(TBL_TASK_USERS, COL_LAST_ACCESS)));
      }
    });

    BeeView.registerConditionProvider(FILTER_TASKS_UPDATED, new BeeView.ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        Long userId = usr.getCurrentUserId();
        if (userId == null) {
          return null;
        }

        SqlSelect query = new SqlSelect().setDistinctMode(true)
            .addFields(TBL_TASK_EVENTS, COL_TASK)
            .addFrom(TBL_TASK_EVENTS)
            .addFromInner(TBL_TASK_USERS,
                SqlUtils.join(TBL_TASK_EVENTS, COL_TASK, TBL_TASK_USERS, COL_TASK))
            .setWhere(SqlUtils.and(
                SqlUtils.notEqual(TBL_TASK_EVENTS, COL_PUBLISHER, userId),
                SqlUtils.equals(TBL_TASK_USERS, COL_USER, userId),
                SqlUtils.joinMore(TBL_TASK_EVENTS, COL_PUBLISH_TIME,
                    TBL_TASK_USERS, COL_LAST_ACCESS)));

        return SqlUtils.in(TBL_TASKS, COL_TASK_ID, query);
      }
    });

    buildTimers(TIMER_REMIND_TASKS_SUMMARY, TIMER_REMIND_USER_TASKS);
  }

  @Override
  public void onTimeout(String timerInfo) {
    if (BeeUtils.isPrefix(timerInfo, TIMER_REMIND_TASKS_SUMMARY)) {
      Long userId = BeeUtils.toLong(BeeUtils.removePrefix(timerInfo, TIMER_REMIND_TASKS_SUMMARY));

      if (DataUtils.isId(userId)
          && usr.isActive(userId)
          && DataUtils.isId(mail.getSenderAccountId(TIMER_REMIND_TASKS_SUMMARY))
          && !TimeUtils.isWeekend(TimeUtils.today())) {
        sendTasksSummaryReminder(userId);
      }
    } else if (BeeUtils.isPrefix(timerInfo, TIMER_REMIND_USER_TASKS)) {

      Long reminderId = BeeUtils.toLong(timerInfo.substring(TIMER_REMIND_USER_TASKS.length()));
      if (reminderId != null) {
        SimpleRow reminderRow = qs.getRow(new SqlSelect()
            .addFields(VIEW_USER_REMINDERS, COL_USER_REMINDER_USER, COL_USER_REMINDER_TYPE)
            .addFields(TBL_TASKS, COL_TASK_ID, COL_SUMMARY)
            .addFrom(VIEW_USER_REMINDERS)
            .addFromInner(TBL_TASKS, sys.joinTables(TBL_TASKS, VIEW_USER_REMINDERS,
                COL_USER_REMINDER_OBJECT))
            .setWhere(SqlUtils.and(SqlUtils.equals(VIEW_USER_REMINDERS,
                sys.getIdName(VIEW_USER_REMINDERS), reminderId))));

        if (reminderRow == null) {
          return;
        }

        Long recipientId = reminderRow.getLong(COL_USER_REMINDER_USER);
        Long taskId = reminderRow.getLong(COL_TASK_ID);

        if (DataUtils.isId(recipientId) && DataUtils.isId(taskId)) {
          Long senderAccountId = mail.getSenderAccountId(TIMER_REMIND_USER_TASKS);

          ResponseObject response = mailNewTask(senderAccountId, taskId, true, true,
              recipientId, usr.getDictionary(recipientId).crmReminderMailSubject());

          if (response.hasErrors() || response.hasWarnings()) {
            logger.warning("user remind tasks canceled");
          }

          String headerCaption = mail.styleMailHeader(
              BeeUtils.joinWords(usr.getDictionary(recipientId).crmTask(), taskId,
                  reminderRow.getValue(COL_SUMMARY)));
          Map<String, String> linkData = Maps.newHashMap();
          linkData.put(VIEW_TASKS, BeeUtils.toString(taskId));
          chat.putMessage(headerCaption, recipientId, linkData);
        }

        Long reminderTypeId = reminderRow.getLong(COL_USER_REMINDER_TYPE);
        if (DataUtils.isId(reminderTypeId)) {
          qs.updateData(new SqlUpdate(VIEW_USER_REMINDERS)
              .addConstant(COL_USER_REMINDER_TIMEOUT, true)
              .setWhere(sys.idEquals(VIEW_USER_REMINDERS, reminderId)));
        } else {
          qs.updateData(new SqlUpdate(VIEW_USER_REMINDERS)
              .addConstant(COL_USER_REMINDER_ACTIVE, null)
              .addConstant(COL_USER_REMINDER_TIMEOUT, true)
              .setWhere(sys.idEquals(VIEW_USER_REMINDERS, reminderId)));
        }
      }
    }
  }

  @Override
  protected List<Timer> createTimers(String timerIdentifier, IsCondition wh) {
    List<Timer> timersList = new ArrayList<>();
    Value currentTime = Value.getValue(System.currentTimeMillis());

    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_TASKS_SUMMARY)) {
      SimpleRowSet data = qs.getData(new SqlSelect()
          .addFields(TBL_USER_SETTINGS, COL_USER, COL_TASKS_MAILING_TIME)
          .addFrom(TBL_USER_SETTINGS)
          .addFromInner(TBL_USERS, sys.joinTables(TBL_USERS, TBL_USER_SETTINGS, COL_USER))

          .setWhere(SqlUtils.and(wh,
              SqlUtils.notNull(TBL_USER_SETTINGS, COL_TASKS_MAILING_TIME),
              SqlUtils.or(
                  SqlUtils.and(
                      SqlUtils.isNull(TBL_USERS, COL_USER_BLOCK_FROM),
                      SqlUtils.isNull(TBL_USERS, COL_USER_BLOCK_UNTIL)
                  ),
                  SqlUtils.and(
                      SqlUtils.notNull(TBL_USERS, COL_USER_BLOCK_FROM),
                      SqlUtils.more(TBL_USERS, COL_USER_BLOCK_FROM, currentTime)
                  ),
                  SqlUtils.and(
                      SqlUtils.notNull(TBL_USERS, COL_USER_BLOCK_UNTIL),
                      SqlUtils.less(TBL_USERS, COL_USER_BLOCK_UNTIL, currentTime)
                  )
              )
          ))
      );

      for (SimpleRowSet.SimpleRow row : data) {
        Long timerId = row.getLong(COL_USER);
        DateTime timerTime = null;
        DateTime reminderTime = TimeUtils.toDateTimeOrNull(
            TimeUtils.parseTime(row.getValue(COL_TASKS_MAILING_TIME)));

        if (reminderTime == null) {
          continue;
        }

        if (reminderTime != null) {
          int hour = reminderTime.getUtcHour();
          int minute = reminderTime.getUtcMinute();

          timerTime = new DateTime(System.currentTimeMillis());
          timerTime.setHour(hour);
          timerTime.setMinute(minute);
          timerTime.setSecond(0);
          timerTime.setMillis(0);
          if (timerTime.getTime() < System.currentTimeMillis()) {
            timerTime.setDom(timerTime.getDom() + 1);
          }
        }

        if (timerTime != null && timerId != null) {
          Timer timer = getTimerService().createIntervalTimer(timerTime.getJava(),
              TimeUtils.MILLIS_PER_DAY, new TimerConfig(timerIdentifier + timerId, false));

          logger.info("Created timer:", timerTime, timer.getInfo());

          if (timer != null) {
            timersList.add(timer);
          }
        }
      }

    } else if (BeeUtils.same(timerIdentifier, TIMER_REMIND_USER_TASKS)) {
      SimpleRowSet data = qs.getData(new SqlSelect()
          .addFields(VIEW_USER_REMINDERS, sys.getIdName(VIEW_USER_REMINDERS),
              COL_USER_REMINDER_OBJECT, COL_USER_REMINDER_USER, COL_USER_REMINDER_TIME)
          .addFields(TBL_TASKS, COL_START_TIME, COL_FINISH_TIME)
          .addFields(VIEW_REMINDER_TYPES, COL_REMINDER_DATA_FIELD, COL_REMINDER_DATA_INDICATOR,
              COL_REMINDER_HOURS, COL_REMINDER_MINUTES)
          .addFrom(VIEW_USER_REMINDERS)
          .addFromInner(TBL_TASKS, sys.joinTables(TBL_TASKS,
              VIEW_USER_REMINDERS, COL_USER_REMINDER_OBJECT))
          .addFromInner(TBL_USERS, sys.joinTables(TBL_USERS, VIEW_USER_REMINDERS, COL_USER))
          .addFromLeft(VIEW_REMINDER_TYPES,
              sys.joinTables(VIEW_REMINDER_TYPES, VIEW_USER_REMINDERS, COL_USER_REMINDER_TYPE))

          .setWhere(SqlUtils.and(wh,
              SqlUtils.equals(VIEW_USER_REMINDERS,
                  COL_USER_REMINDER_OBJECT_MODULE, getModule().ordinal()),
              SqlUtils.equals(VIEW_USER_REMINDERS, COL_USER_REMINDER_ACTIVE, true),
              getTaskStatusConditionForReminders(),
              SqlUtils.or(
                  SqlUtils.and(
                      SqlUtils.isNull(TBL_USERS, COL_USER_BLOCK_FROM),
                      SqlUtils.isNull(TBL_USERS, COL_USER_BLOCK_UNTIL)),
                  SqlUtils.and(
                      SqlUtils.notNull(TBL_USERS, COL_USER_BLOCK_FROM),
                      SqlUtils.more(TBL_USERS, COL_USER_BLOCK_FROM, currentTime)),
                  SqlUtils.and(
                      SqlUtils.notNull(TBL_USERS, COL_USER_BLOCK_UNTIL),
                      SqlUtils.less(TBL_USERS, COL_USER_BLOCK_UNTIL, currentTime))))));

      for (SimpleRow row : data) {

        Long taskId = row.getLong(COL_USER_REMINDER_OBJECT);
        Long userId = row.getLong(COL_USER_REMINDER_USER);

        Integer reminderDateFieldId = row.getInt(COL_REMINDER_DATA_FIELD);
        DateTime reminderTime = row.getDateTime(COL_USER_REMINDER_TIME);

        if (reminderTime == null && reminderDateFieldId != null) {
          DateTime time = null;
          if (BeeUtils.same(reminderDateFieldId.toString(),
              BeeUtils.toString(ReminderDateField.START_DATE.ordinal()))) {
            time = row.getDateTime(COL_START_TIME);
          } else if (BeeUtils.same(reminderDateFieldId.toString(),
              BeeUtils.toString(ReminderDateField.END_DATE.ordinal()))) {
            time = row.getDateTime(COL_FINISH_TIME);
          }

          Integer dataIndicator = row.getInt(COL_REMINDER_DATA_INDICATOR);
          Integer dataHours = row.getInt(COL_REMINDER_HOURS);
          Integer dataMinutes = row.getInt(COL_REMINDER_MINUTES);
          reminderTime = cls.calculateReminderTime(time.getTime(), dataIndicator,
              dataHours, dataMinutes);
        }
        if (reminderTime == null) {
          continue;
        }

        Long reminderId = row.getLong(sys.getIdName(VIEW_USER_REMINDERS));
        if (reminderTime != null && taskId != null && userId != null && reminderId != null) {
          if (reminderTime.getTime() > System.currentTimeMillis()) {
            Timer timer = timerService.createSingleActionTimer(reminderTime.getJava(),
                new TimerConfig(TIMER_REMIND_USER_TASKS + reminderId, false));

            logger.info("Created timer:", reminderTime, TIMER_REMIND_USER_TASKS + reminderId);
            if (timer != null) {
              timersList.add(timer);
            }

            qs.updateData(new SqlUpdate(VIEW_USER_REMINDERS)
                .addConstant(COL_USER_REMINDER_TIMEOUT, null)
                .setWhere(sys.idEquals(VIEW_USER_REMINDERS, reminderId)));

          } else {
            qs.updateData(new SqlUpdate(VIEW_USER_REMINDERS)
                .addConstant(COL_USER_REMINDER_TIMEOUT, true)
                .setWhere(sys.idEquals(VIEW_USER_REMINDERS, reminderId)));
          }
        }
      }
    }
    return timersList;
  }

  private IsCondition getTaskStatusConditionForReminders() {
    return SqlUtils.not(SqlUtils.inList(TBL_TASKS, COL_STATUS,
            Sets.newHashSet(TaskStatus.NOT_SCHEDULED.ordinal(), TaskStatus.APPROVED.ordinal())));
  }

  @Override
  protected Pair<IsCondition, List<String>> getConditionAndTimerIdForUpdate(String timerIdentifier,
                                                                String viewName, Long relationId) {
    IsCondition wh = null;

    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_TASKS_SUMMARY)) {
      Long userId = null;

      if (BeeUtils.same(viewName, TBL_USER_SETTINGS)) {
        SimpleRowSet data = qs.getData(new SqlSelect()
            .addFields(TBL_USER_SETTINGS, COL_USER)
            .addFrom(TBL_USER_SETTINGS)
            .setWhere(sys.idEquals(TBL_USER_SETTINGS, relationId)));
        if (data.getRows() != null && data.getRows().size() > 0) {
          userId = data.getRow(0).getLong(COL_USER);
        }
      } else if (BeeUtils.same(viewName, TBL_USERS)) {
        userId = relationId;
      }

      if (userId != null) {
        wh = SqlUtils.equals(TBL_USER_SETTINGS, COL_USER, userId);
      }

      List<String> timerIdentifiersIds = new ArrayList<String>();
      timerIdentifiersIds.add(timerIdentifier + userId);
      return Pair.of(wh, timerIdentifiersIds);

    } else if (BeeUtils.same(timerIdentifier, TIMER_REMIND_USER_TASKS)) {
      if (BeeUtils.same(viewName, TBL_REMINDER_TYPES)) {
        Long typeId = relationId;
        if (typeId != null) {
          wh = SqlUtils.equals(VIEW_USER_REMINDERS, COL_USER_REMINDER_TYPE, typeId);

          return Pair.of(wh, generateReminderTimersIdsListForUpdate(timerIdentifier, typeId, null));
        }

      } else if (BeeUtils.same(viewName, VIEW_USER_REMINDERS)) {
        Long reminderId = relationId;
        wh = SqlUtils.equals(VIEW_USER_REMINDERS,
            sys.getIdName(VIEW_USER_REMINDERS), reminderId);
        List<String> timerIdentifiersIds = new ArrayList<String>();
        timerIdentifiersIds.add(timerIdentifier + reminderId);
        return Pair.of(wh, timerIdentifiersIds);

      } else if (BeeUtils.same(viewName, TBL_TASKS)) {
        Long taskId = relationId;
        wh = SqlUtils.equals(VIEW_USER_REMINDERS, COL_USER_REMINDER_OBJECT, taskId);

        return Pair.of(wh, generateReminderTimersIdsListForUpdate(timerIdentifier, null, taskId));
      }
    }
    return null;
  }

  private List<String> generateReminderTimersIdsListForUpdate(String timerIdentifier,
      Long reminderTypeId, Long taskId) {

    List<String> timersIds = new ArrayList<>();

    SimpleRowSet data = null;

    if (reminderTypeId != null) {
      data = qs.getData(new SqlSelect()
          .addFields(VIEW_USER_REMINDERS, sys.getIdName(VIEW_USER_REMINDERS))
          .addFrom(VIEW_USER_REMINDERS)
          .addFromInner(TBL_TASKS, sys.joinTables(TBL_TASKS,
              VIEW_USER_REMINDERS, COL_USER_REMINDER_OBJECT))
          .setWhere(SqlUtils.and(SqlUtils.equals(VIEW_USER_REMINDERS,
              COL_USER_REMINDER_TYPE, reminderTypeId),
              SqlUtils.equals(VIEW_USER_REMINDERS, COL_USER_REMINDER_ACTIVE, true),
              getTaskStatusConditionForReminders())));

    } else if (taskId != null) {
      data = qs.getData(new SqlSelect()
          .addFields(VIEW_USER_REMINDERS, sys.getIdName(VIEW_USER_REMINDERS))
          .addFrom(VIEW_USER_REMINDERS)
          .setWhere(SqlUtils.and(SqlUtils.equals(VIEW_USER_REMINDERS,
              COL_USER_REMINDER_OBJECT, taskId),
              SqlUtils.equals(VIEW_USER_REMINDERS, COL_USER_REMINDER_ACTIVE, true))));
    }

    if (data != null) {
      for (SimpleRow row : data) {
        timersIds.add(timerIdentifier + row.getLong(sys.getIdName(VIEW_USER_REMINDERS)));
      }
    }
    return timersIds;
  }

  public SimpleRowSet getTaskActualTimesAndExpenses(List<Long> ids) {
    SimpleRowSet result = new SimpleRowSet(new String[] {
        COL_ACTUAL_DURATION, COL_ACTUAL_EXPENSES
    });

    Filter idFilter = Filter.any(COL_TASK, ids);
    Filter durationFilter = Filter.notNull(COL_DURATION);

    BeeRowSet taskEvents =
        qs.getViewData(VIEW_TASK_DURATIONS, Filter.and(idFilter, durationFilter),
            null, Lists.newArrayList(COL_TASK, COL_DURATION, ProjectConstants.COL_RATE));

    if (taskEvents.isEmpty()) {
      return result;
    }
    Double defaultRate = prm.getDouble(ProjectConstants.PRM_PROJECT_COMMON_RATE);

    Map<Long, Long> times = new HashMap<>();
    Map<Long, Double> expenses = new HashMap<>();

    int idxEventDuration =
        DataUtils.getColumnIndex(COL_DURATION, taskEvents.getColumns(), false);
    int idxRate =
        DataUtils.getColumnIndex(ProjectConstants.COL_RATE, taskEvents.getColumns(), false);
    int idxId = DataUtils.getColumnIndex(COL_TASK, taskEvents.getColumns(), false);

    if (BeeUtils.isNegative(idxEventDuration) || BeeUtils.isNegative(idxId)) {
      return result;
    }

    for (IsRow row : taskEvents) {
      Long id = row.getLong(idxId);
      Long newTimeMls = TimeUtils.parseTime(row.getString(idxEventDuration));

      if (Objects.isNull(newTimeMls)) {
        continue;
      }

      Long currentTime = times.get(id);

      if (currentTime == null) {
        currentTime = Long.valueOf(0);
      }

      currentTime += newTimeMls;

      times.put(id, currentTime);

      if (BeeUtils.isNegative(idxRate)) {
        continue;
      }

      Double rate = row.getDouble(idxRate);
      double currentTimeInHrs = Double.valueOf(newTimeMls)
          / Double.valueOf(TimeUtils.MILLIS_PER_HOUR);
      Double currentExpense = expenses.get(id);

      if (!BeeUtils.isPositive(rate)) {
        if (BeeUtils.isDouble(defaultRate)) {
          rate = defaultRate;
        } else {
          rate = Double.valueOf(BeeConst.DOUBLE_ZERO);
        }
      }

      if (currentExpense == null) {
        currentExpense = Double.valueOf(BeeConst.DOUBLE_ZERO);
      }

      currentExpense += rate * currentTimeInHrs;
      expenses.put(id, currentExpense);

    }

    result.addRow(new String[] {
        Codec.beeSerialize(times),
        Codec.beeSerialize(expenses)
    });

    return result;

  }

  public ResponseObject accessTask(Long taskId) {
    if (!DataUtils.isId(taskId)) {
      return ResponseObject.parameterNotFound(SVC_ACCESS_TASK, VAR_TASK_ID);
    }
    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(SVC_ACCESS_TASK, "user id not available");
    }
    IsCondition where = SqlUtils.equals(TBL_TASK_USERS, COL_TASK, taskId, COL_USER, userId);

    if (qs.sqlExists(TBL_TASK_USERS, where)) {
      return registerTaskVisit(taskId, userId, System.currentTimeMillis());
    } else {
      if (!usr.isAdministrator()) {
        logger.warning("task", taskId, "access by unauthorized user", userId);
      }
      return ResponseObject.emptyResponse();
    }
  }

  private void addTaskProperties(BeeRow row, Collection<Long> observers, Long eventId,
      Collection<String> propNames, boolean addRelations) {
    long taskId = row.getId();

    if (propNames.contains(PROP_OBSERVERS) && !BeeUtils.isEmpty(observers)) {
      row.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(observers));
    }


    if (addRelations) {
      Collection<RowChildren> relations = getRelations(COL_TASK, row.getId());

      for (RowChildren relation : relations) {
        String relView = sys.getDataInfo(TBL_RELATIONS).getRelation(relation.getChildColumn());
        row.setProperty(Relations.PFX_RELATED + relView, relation.getChildrenIds());
      }
    }

    if (propNames.contains(PROP_FILES)) {
      List<FileInfo> files = getTaskFiles(taskId);
      if (!files.isEmpty()) {
        row.setProperty(PROP_FILES, Codec.beeSerialize(files));
      }
    }

    if (propNames.contains(PROP_EVENTS)) {
      BeeRowSet events =
          qs.getViewData(VIEW_TASK_EVENTS, Filter.equals(COL_TASK, taskId), new Order(
              COL_PUBLISH_TIME, !propNames.contains(PROP_DESCENDING)));
      if (!DataUtils.isEmpty(events)) {
        row.setProperty(PROP_EVENTS, events.serialize());
      }
    }

    if (eventId != null) {
      row.setProperty(PROP_LAST_EVENT_ID, BeeUtils.toString(eventId));
    }
  }

  @Schedule(hour = "5", persistent = false)
  @Deprecated
  private void checkTaskStatus() {
    if (!Config.isInitialized()) {
      return;
    }
    logger.info("check task status timeout");

    int count = maybeUpdateTaskStatus();
    logger.info("check task status updated", count, "tasks");
  }

  private ResponseObject commitTaskData(BeeRowSet data, Collection<Long> oldUsers,
     String updatedRelations, Long eventId) {

    ResponseObject response;
    BeeRow row = data.getRow(0);

    List<Long> newUsers;
    if (!BeeUtils.isEmpty(oldUsers)) {
      newUsers = TaskUtils.getTaskUsers(row, data.getColumns());
      if (!BeeUtils.sameElements(oldUsers, newUsers)) {
        updateTaskUsers(row.getId(), oldUsers, newUsers);
      }
    } else {
      newUsers = null;
    }

    if (!BeeUtils.isEmpty(updatedRelations)) {
      usb.updateRelatedValues(data.getViewName(), row.getId(), updatedRelations);
    }

    Set<String> propNames = NameUtils.toSet(DEFAULT_TASK_PROPERTIES);
    if (row.hasPropertyValue(PROP_DESCENDING)) {
      propNames.add(PROP_DESCENDING);
    }

    Map<Integer, String> shadow = row.getShadow();
    if (shadow != null && !shadow.isEmpty()) {
      List<BeeColumn> columns = new ArrayList<>();
      List<String> oldValues = new ArrayList<>();
      List<String> newValues = new ArrayList<>();

      for (Map.Entry<Integer, String> entry : shadow.entrySet()) {
        columns.add(data.getColumn(entry.getKey()));

        oldValues.add(entry.getValue());
        newValues.add(row.getString(entry.getKey()));
      }

      BeeRow updRow = new BeeRow(row.getId(), row.getVersion(), oldValues);
      for (int i = 0; i < columns.size(); i++) {
        updRow.preliminaryUpdate(i, newValues.get(i));
      }

      BeeRowSet updated = new BeeRowSet(data.getViewName(), columns);
      updated.addRow(updRow);

      response = deb.commitRow(updated);
      if (!response.hasErrors() && response.hasResponse(BeeRow.class)) {
        BeeRow respRow = (BeeRow) response.getResponse();
        if (newUsers == null) {
          newUsers = getTaskUsers(respRow.getId());
        }
        columns = sys.getView(data.getViewName()).getRowSetColumns();
        newUsers.remove(DataUtils.getLong(columns, respRow, COL_OWNER));
        newUsers.remove(DataUtils.getLong(columns, respRow, COL_EXECUTOR));

        addTaskProperties(respRow, newUsers, eventId, propNames, false);
      }

    } else {
      response = getTaskData(row.getId(), eventId, propNames, false);
    }

    return response;
  }

  private ResponseObject copyRecurringTask(RequestInfo reqInfo) {
    Long rtId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_RT_ID));
    if (!DataUtils.isId(rtId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_ID);
    }

    BeeRowSet rowSet = qs.getViewData(VIEW_RECURRING_TASKS, Filter.compareId(rtId));
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_RECURRING_TASKS, rtId,
          "not available");
    }

    BeeRow row = rowSet.getRow(0);

    DataUtils.setValue(rowSet, row, COL_RT_SCHEDULE_FROM,
        Integer.toString(TimeUtils.today().getDays() + 1));
    DataUtils.setValue(rowSet, row, COL_RT_SCHEDULE_UNTIL, null);

    BeeRowSet insert = DataUtils.createRowSetForInsert(rowSet.getViewName(), rowSet.getColumns(),
        row);
    ResponseObject response = deb.commitRow(insert);

    if (!response.hasErrors() && response.hasResponse(BeeRow.class)) {
      long newId = ((BeeRow) response.getResponse()).getId();

      qs.copyData(TBL_RT_EXECUTORS, COL_RTEX_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_EXECUTOR_GROUPS, COL_RTEXGR_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_OBSERVERS, COL_RTOB_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_OBSERVER_GROUPS, COL_RTOBGR_RECURRING_TASK, rtId, newId);

      qs.copyData(TBL_RT_DATES, COL_RTD_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_FILES, COL_RTD_RECURRING_TASK, rtId, newId);

      qs.copyData(TBL_RELATIONS, COL_RECURRING_TASK, rtId, newId);
    }

    return response;
  }

  private ResponseObject confirmTasks(RequestInfo reqInfo) {
    ResponseObject response = null;

    String taskIdListData = reqInfo.getParameter(VAR_TASK_DATA);
    if (BeeUtils.isEmpty(taskIdListData)) {
      String msg = BeeUtils.joinWords("Task data not received:", SVC_CONFIRM_TASKS);
      logger.warning(msg);
      response = ResponseObject.error(msg);
      return response;
    }

    List<Long> taskIdList = Codec.deserializeIdList(taskIdListData);
    DataInfo info = sys.getDataInfo(VIEW_TASKS);
    response = ResponseObject.emptyResponse();
    long user = BeeUtils.unbox(usr.getCurrentUserId());
    long now = System.currentTimeMillis();

    /* Don't trust client only */
    BeeRowSet oldTaskData = qs.getViewData(VIEW_TASKS, Filter.idIn(taskIdList));
    if (!TaskUtils.canConfirmTasks(info, oldTaskData.getRows(), user, response)) {
      return response;
    }

    String comment = reqInfo.getParameter(VAR_TASK_COMMENT);
    String notes = reqInfo.getParameter(VAR_TASK_NOTES);

    String eventNote;
    if (BeeUtils.isEmpty(notes)) {
      eventNote = null;
    } else {
      eventNote = BeeUtils.buildLines(Codec.beeDeserializeCollection(notes));
    }

    DateTime approved = new DateTime();

    if (reqInfo.hasParameter(VAR_TASK_APPROVED_TIME)) {
      String strTime = reqInfo.getParameter(VAR_TASK_APPROVED_TIME);

      if (!BeeUtils.isEmpty(strTime)) {
        approved = DateTime.restore(strTime);
      }
    }

    for (Long taskId : taskIdList) {
      response = registerTaskEvent(BeeUtils.unbox(taskId), user, TaskEvent.APPROVE, comment,
          eventNote, null, null, null, now);

      if (response.hasErrors()) {
        logger.severe("Confirmation failed");
        ctx.setRollbackOnly();
        return response;
      }
    }

    SqlUpdate update = new SqlUpdate(TBL_TASKS)
        .addConstant(COL_STATUS, TaskStatus.APPROVED.ordinal())
        .addConstant(COL_APPROVED, approved)
        .setWhere(SqlUtils.inList(TBL_TASKS, sys.getIdName(TBL_TASKS), taskIdList));
    response = qs.updateDataWithResponse(update);

    if (response.hasErrors()) {
      logger.severe("Confirmation failed");
      ctx.setRollbackOnly();
      return response;
    }

    return response;
  }

  private ResponseObject createNotScheduledTask(BeeRowSet data, BeeRow row, long owner) {
    ResponseObject response = null;

    Map<String, String> properties = BeeUtils.isEmpty(row.getProperties()) ? new HashMap<>() : row
        .getProperties();

    List<Long> observers = DataUtils.parseIdList(properties.get(PROP_OBSERVERS));

    Long[] observerMembers =
        adm.getUserGroupMembers(properties.get(PROP_OBSERVER_GROUPS)).getLongColumn(COL_UG_USER);
    if (!ArrayUtils.isEmpty(observerMembers)) {
      for (Long member : observerMembers) {
        if (!observers.contains(member)) {
          observers.add(member);
        }
      }
    }

    List<Long> tasks = new ArrayList<>();
    BeeRow newRow = DataUtils.cloneRow(row);

    TaskStatus status = TaskStatus.NOT_SCHEDULED;
    newRow.setValue(data.getColumnIndex(COL_STATUS), status.ordinal());

    BeeRowSet rowSet = new BeeRowSet(data.getViewName(), data.getColumns());
    rowSet.addRow(newRow);

    response = deb.commitRow(rowSet, RowInfo.class);
    if (response.hasErrors()) {
      return response;
    }

    long taskId = ((RowInfo) response.getResponse()).getId();

    response = registerTaskEvent(taskId, owner, TaskEvent.CREATE_NOT_SCHEDULED, System
        .currentTimeMillis());
    if (!response.hasErrors()) {
      createTaskUser(taskId, owner, System.currentTimeMillis());
    }

    if (!response.hasErrors()) {
      for (long obsId : observers) {

        response = createTaskUser(taskId, obsId, null);
        if (response.hasErrors()) {
          break;
        }

      }
    }

    if (!response.hasErrors() && !BeeUtils.isEmpty(properties.get(VAR_TASK_RELATIONS))) {
      response = usb.updateRelatedValues(data.getViewName(), taskId,
          properties.get(VAR_TASK_RELATIONS));
    }

    if (!response.hasErrors()) {
      tasks.add(taskId);
    }

    if (response.hasErrors()) {
      return response;
    }

    response = ResponseObject.response(DataUtils.buildIdList(tasks));

    return response;
  }

  private ResponseObject createTasks(BeeRowSet data, BeeRow row, long owner) {
    ResponseObject response = null;

    Map<String, String> properties = row.getProperties();

    List<Long> executors = DataUtils.parseIdList(properties.get(PROP_EXECUTORS));
    List<Long> observers = DataUtils.parseIdList(properties.get(PROP_OBSERVERS));

    Long[] executorMembers =
        adm.getUserGroupMembers(properties.get(PROP_EXECUTOR_GROUPS)).getLongColumn(COL_UG_USER);
    if (!ArrayUtils.isEmpty(executorMembers)) {
      for (Long member : executorMembers) {
        if (!executors.contains(member) && !observers.contains(member)) {
          executors.add(member);
        }
      }
    }

    Long[] observerMembers =
        adm.getUserGroupMembers(properties.get(PROP_OBSERVER_GROUPS)).getLongColumn(COL_UG_USER);
    if (!ArrayUtils.isEmpty(observerMembers)) {
      for (Long member : observerMembers) {
        if (!observers.contains(member)) {
          observers.add(member);
        }
      }
    }

    List<Long> tasks = new ArrayList<>();

    for (long executor : executors) {
      BeeRow newRow = DataUtils.cloneRow(row);
      newRow.setValue(data.getColumnIndex(COL_EXECUTOR), executor);
      TaskStatus status = (executor == owner) ? TaskStatus.VISITED : TaskStatus.NOT_VISITED;

      newRow.setValue(data.getColumnIndex(COL_STATUS), status.ordinal());

      BeeRowSet rowSet = new BeeRowSet(data.getViewName(), data.getColumns());
      rowSet.addRow(newRow);

      response = deb.commitRow(rowSet, RowInfo.class);
      if (response.hasErrors()) {
        break;
      }

      long taskId = ((RowInfo) response.getResponse()).getId();

      response = registerTaskEvent(taskId, owner, TaskEvent.CREATE, System.currentTimeMillis());
      if (!response.hasErrors()) {
        createTaskUser(taskId, owner, System.currentTimeMillis());
      }

      if (!response.hasErrors() && executor != owner) {
        response = createTaskUser(taskId, executor, null);
      }

      if (!response.hasErrors()) {
        for (long obsId : observers) {
          if (obsId != owner && obsId != executor) {
            response = createTaskUser(taskId, obsId, null);
            if (response.hasErrors()) {
              break;
            }
          }
        }
      }

      if (!response.hasErrors() && !BeeUtils.isEmpty(properties.get(VAR_TASK_RELATIONS))) {
        response = usb.updateRelatedValues(data.getViewName(), taskId,
            properties.get(VAR_TASK_RELATIONS));
      }
      if (!response.hasErrors()) {
        tasks.add(taskId);
      }

      if (response.hasErrors()) {
        break;
      }
    }

    if (response == null) {
      response = ResponseObject.emptyResponse();
    } else if (!response.hasErrors() && !tasks.isEmpty()) {
      response = ResponseObject.response(DataUtils.buildIdList(tasks));
    }

    return response;
  }

  public ResponseObject createTaskUser(long taskId, long userId, Long millis) {
    SqlInsert si = new SqlInsert(TBL_TASK_USERS)
        .addConstant(COL_TASK, taskId)
        .addConstant(COL_USER, userId);

    if (millis != null) {
      si.addConstant(COL_LAST_ACCESS, millis);
    }

    return qs.insertDataWithResponse(si);
  }

  public ResponseObject doTaskEvent(String svc, Map<String, String> reqInfo) {
    ResponseObject response = null;

    TaskEvent event = EnumUtils.getEnumByName(TaskEvent.class, svc);
    if (event == null) {
      String msg = BeeUtils.joinWords("Task service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
      return response;
    }

    String dataParam = reqInfo.get(VAR_TASK_DATA);
    if (BeeUtils.isEmpty(dataParam)) {
      String msg = BeeUtils.joinWords("Task data not received:", svc, event);
      logger.warning(msg);
      response = ResponseObject.error(msg);
      return response;
    }

    BeeRowSet taskData = BeeRowSet.restore(dataParam);
    BeeRow taskRow = taskData.getRow(0);

    long taskId = taskRow.getId();

    long currentUser = usr.getCurrentUserId();
    long now = System.currentTimeMillis();

    String eventNote;

    String notes = reqInfo.get(VAR_TASK_NOTES);
    if (BeeUtils.isEmpty(notes)) {
      eventNote = null;
    } else {
      eventNote = BeeUtils.buildLines(Codec.beeDeserializeCollection(notes));
    }
    String updatedRelations = reqInfo.get(VAR_TASK_RELATIONS);

    switch (event) {
      case CREATE_NOT_SCHEDULED:
        response = createNotScheduledTask(taskData, taskRow, currentUser);

        Set<Long> notSheduledTask = DataUtils.parseIdSet(response.getResponseAsString());

        response.setResponse(qs.getViewData(taskData.getViewName(), Filter.idIn(notSheduledTask)));
        break;
      case CREATE:
        response = createTasks(taskData, taskRow, currentUser);

        if (!response.hasErrors() && response.hasResponse(String.class)) {
          Set<Long> createdTasks = DataUtils.parseIdSet(response.getResponseAsString());
          Long senderAccountId = mail.getSenderAccountId("create task:");

          if (senderAccountId != null) {
            boolean pref = BeeConst.isTrue(taskRow.getProperty(PROP_MAIL));

            for (Long id : createdTasks) {
              ResponseObject mailResponse = mailNewTask(senderAccountId, id, pref, false);
              response.addMessagesFrom(mailResponse);
            }
          }

          response.setResponse(qs.getViewData(taskData.getViewName(), Filter.idIn(createdTasks)));
        }
        break;

      case VISIT:
        if (reqInfo.containsKey(VAR_TASK_VISITED)) {
          response = registerTaskEvent(taskId, currentUser, event, now);

        } else if (reqInfo.containsKey(VAR_TASK_VISITED_STATE)) {
          response =
              updateTaskData(reqInfo, taskData, taskRow, event, updatedRelations, currentUser,
                  eventNote, now);
          break;
        }
        if (response == null || !response.hasErrors()) {
          response = commitTaskData(taskData, null, updatedRelations, null);
        }
        break;

      case FORWARD:
      case EXTEND:
      case EDIT:
      case COMMENT:
      case SUSPEND:
      case CANCEL:
      case COMPLETE:
      case APPROVE:
      case REFRESH:
      case RENEW:
      case ACTIVATE:
      case OUT_OF_OBSERVERS:
      case CREATE_SCHEDULED:

        response = updateTaskData(reqInfo, taskData, taskRow, event, updatedRelations, currentUser,
            eventNote, now);

        break;
    }

    if (response.hasErrors()) {
      ctx.setRollbackOnly();
    }
    return response;
  }

  private ResponseObject extendTask(Map<String, String> reqInfo) {
    Long taskId = BeeUtils.toLongOrNull(reqInfo.get(VAR_TASK_ID));
    if (!DataUtils.isId(taskId)) {
      return ResponseObject.parameterNotFound(SVC_EXTEND_TASK, VAR_TASK_ID);
    }

    Long startMillis = BeeUtils.toLongOrNull(reqInfo.get(COL_START_TIME));
    Long endMillis = BeeUtils.toLongOrNull(reqInfo.get(COL_FINISH_TIME));

    if (!BeeUtils.isPositive(startMillis) && !BeeUtils.isPositive(endMillis)) {
      return ResponseObject.error(SVC_EXTEND_TASK, COL_START_TIME, "or", COL_FINISH_TIME,
          "not specified");
    }

    String eventNote;
    String notes = reqInfo.get(VAR_TASK_NOTES);
    if (BeeUtils.isEmpty(notes)) {
      eventNote = null;
    } else {
      eventNote = BeeUtils.buildLines(Codec.beeDeserializeCollection(notes));
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(SVC_EXTEND_TASK, "user id not available");
    }

    BeeRowSet data = qs.getViewData(VIEW_TASKS, Filter.compareId(taskId));
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(SVC_EXTEND_TASK, "task", taskId, "not found");
    }

    BeeRow row = data.getRow(0);

    if (BeeUtils.isPositive(startMillis)) {
      row.preliminaryUpdate(data.getColumnIndex(COL_START_TIME), startMillis.toString());
    }

    Long oldEnd = null;
    if (BeeUtils.isPositive(endMillis)) {
      int index = data.getColumnIndex(COL_FINISH_TIME);
      Long value = row.getLong(index);

      if (value != null && !value.equals(endMillis)) {
        oldEnd = value;
      }

      row.preliminaryUpdate(index, endMillis.toString());
    }

    long now = System.currentTimeMillis();
    Long eventId = null;

    ResponseObject response = registerTaskEvent(taskId, userId, TaskEvent.EXTEND,
        reqInfo, eventNote, oldEnd, now);
    if (response.hasResponse(Long.class)) {
      eventId = (Long) response.getResponse();
    }
    if (!response.hasErrors()) {
      response = registerTaskVisit(taskId, userId, now);
    }

    if (!response.hasErrors()) {
      response = commitTaskData(data, null, null, eventId);
    }

    return response;
  }

  private ResponseObject finishRequestWithTask(RequestInfo reqInfo) {
    String comment = reqInfo.getParameter(VAR_TASK_COMMENT);
    Long type = reqInfo.getParameterLong(VAR_TASK_DURATION_TYPE);
    String time = reqInfo.getParameter(VAR_TASK_DURATION_TIME);

    if (BeeUtils.isEmpty(comment)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_TASK_COMMENT);
    }
    if (!DataUtils.isId(type)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_TASK_DURATION_TYPE);
    }
    if (BeeUtils.isEmpty(time)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_TASK_DURATION_TIME);
    }

    Map<String, String> reqMap = new HashMap<>();

    if (!BeeUtils.isEmpty(reqInfo.getParams())) {
      reqMap.putAll(reqInfo.getParams());
    }
    if (!BeeUtils.isEmpty(reqInfo.getVars())) {
      reqMap.putAll(reqInfo.getVars());
    }
    ResponseObject response = doTaskEvent(TaskEvent.CREATE.name(), reqMap);

    if (!response.hasErrors()) {
      BeeRowSet rowSet = (BeeRowSet) response.getResponse();
      rowSet.getRow(0).preliminaryUpdate(DataUtils.getColumnIndex(COL_STATUS, rowSet.getColumns()),
          BeeUtils.toString(TaskStatus.APPROVED.ordinal()));

      reqMap.put(VAR_TASK_DATA, Codec.beeSerialize(rowSet));
      response = doTaskEvent(TaskEvent.APPROVE.name(), reqMap);
    }

    return response;
  }

  private ResponseObject getChangedTasks() {
    IsCondition uwh = SqlUtils.equals(TBL_TASK_USERS, COL_USER, usr.getCurrentUserId());

    SqlSelect tNewQuery = new SqlSelect().setDistinctMode(true)
        .addFrom(TBL_TASK_USERS)
        .addFields(TBL_TASK_USERS, COL_TASK)
        .setWhere(SqlUtils.and(uwh, SqlUtils.isNull(TBL_TASK_USERS, COL_LAST_ACCESS)));

    Long[] newTasks = qs.getLongColumn(tNewQuery);
    logger.debug("new tasks", newTasks);

    String idName = sys.getIdName(TBL_TASKS);

    SqlSelect inner = new SqlSelect()
        .addFrom(TBL_TASKS)
        .addFromInner(TBL_TASK_USERS, sys.joinTables(TBL_TASKS, TBL_TASK_USERS, COL_TASK))
        .addFromInner(TBL_TASK_EVENTS, sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
        .addFields(TBL_TASKS, idName)
        .addMax(TBL_TASK_USERS, COL_LAST_ACCESS)
        .addMax(TBL_TASK_EVENTS, COL_PUBLISH_TIME)
        .addGroup(TBL_TASKS, idName)
        .setWhere(SqlUtils.and(uwh, SqlUtils.notNull(TBL_TASK_USERS, COL_LAST_ACCESS)));

    String alias = "tupd_" + SqlUtils.uniqueName();

    SqlSelect tUpdQuery = new SqlSelect().setDistinctMode(true)
        .addFrom(inner, alias)
        .addFields(alias, idName)
        .setWhere(SqlUtils.more(alias, COL_PUBLISH_TIME, SqlUtils.field(alias, COL_LAST_ACCESS)));

    Long[] updTasks = qs.getLongColumn(tUpdQuery);
    logger.debug("upd tasks", updTasks);

    List<Long> result = new ArrayList<>();

    long cntNew = (newTasks == null) ? 0 : newTasks.length;
    result.add(cntNew);
    if (cntNew > 0) {
      result.addAll(Lists.newArrayList(newTasks));
    }

    if (updTasks != null && updTasks.length > 0) {
      result.addAll(Lists.newArrayList(updTasks));
    }

    return ResponseObject.response(Joiner.on(BeeConst.CHAR_COMMA).join(result));
  }

  private ResponseObject getCompanyTimesReport(RequestInfo reqInfo) {
    Dictionary constants = usr.getDictionary();
    boolean hideZeroTimes = false;

    SqlSelect companiesListQuery =
        new SqlSelect()
            .addFields(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES))
            .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
            .addField(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME, ALS_COMPANY_TYPE)
            .addFrom(TBL_COMPANIES)
            .addFromLeft(TBL_COMPANY_TYPES,
                sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE))
            .setWhere(SqlUtils.sqlTrue())
            .addOrder(TBL_COMPANIES, COL_COMPANY_NAME);

    if (reqInfo.hasParameter(VAR_TASK_COMPANY)) {
      companiesListQuery.setWhere(SqlUtils.and(companiesListQuery.getWhere(), SqlUtils.inList(
          TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), DataUtils
              .parseIdList(reqInfo.getParameter(VAR_TASK_COMPANY)))));
    }

    if (reqInfo.hasParameter(VAR_TASK_DURATION_HIDE_ZEROS)) {
      hideZeroTimes = true;
    }

    SimpleRowSet companiesListSet = qs.getData(companiesListQuery);
    SimpleRowSet result = new SimpleRowSet(new String[] {COL_COMPANY_NAME, COL_DURATION});
    long totalTimeMls = 0;

    result.addRow(new String[] {constants.client(), constants.crmSpentTime()});

    /* Register times in tasks without company */
    companiesListSet.addRow(new String[] {null, "—", null});

    for (int i = 0; i < companiesListSet.getNumberOfRows(); i++) {
      String compFullName =
          companiesListSet.getValue(i, COL_COMPANY_NAME)
              + (!BeeUtils.isEmpty(companiesListSet.getValue(i, ALS_COMPANY_TYPE))
              ? ", " + companiesListSet.getValue(i, ALS_COMPANY_TYPE) : "");

      SqlSelect companyTimesQuery = new SqlSelect()
          .addFields(TBL_EVENT_DURATIONS, COL_DURATION)
          .addFrom(TBL_TASK_EVENTS)
          .addFromRight(TBL_EVENT_DURATIONS,
              sys.joinTables(TBL_EVENT_DURATIONS, TBL_TASK_EVENTS, COL_EVENT_DURATION))
          .addFromLeft(TBL_DURATION_TYPES,
              sys.joinTables(TBL_DURATION_TYPES, TBL_EVENT_DURATIONS, COL_DURATION_TYPE))
          .addFromLeft(TBL_TASKS,
              sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
          .addFromLeft(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_TASK_EVENTS, COL_PUBLISHER))
          .setWhere(
              SqlUtils.equals(TBL_COMPANIES, sys
                  .getIdName(TBL_COMPANIES), companiesListSet.getValue(i, sys
                  .getIdName(TBL_COMPANIES))));

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_FROM)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_FROM))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils
              .moreEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_FROM))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_TO)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_TO))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils
              .lessEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_TO))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_PUBLISHER)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_PUBLISHER))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils.inList(
              TBL_USERS, sys.getIdName(TBL_USERS), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_PUBLISHER)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_TYPE)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_TYPE))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils.inList(
              TBL_DURATION_TYPES, sys.getIdName(TBL_DURATION_TYPES), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_DURATION_TYPE)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_PROJECT)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_PROJECT))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils.inList(
              TBL_TASKS, ProjectConstants.COL_PROJECT, DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_PROJECT)))));
        }
      }

      SimpleRowSet companyTimes = qs.getData(companyTimesQuery);
      long dTimeMls = 0;

      for (int j = 0; j < companyTimes.getNumberOfRows(); j++) {
        Long timeMls = TimeUtils.parseTime(companyTimes.getValue(j, companyTimes
            .getColumnIndex(COL_DURATION)));
        dTimeMls += timeMls;
      }

      totalTimeMls += dTimeMls;

      if (!(hideZeroTimes && dTimeMls <= 0)) {
        result.addRow(new String[] {compFullName, TimeUtils.renderTime(dTimeMls, false)});
      }
    }

    result.addRow(new String[] {
        constants.totalOf() + ":", TimeUtils.renderTime(totalTimeMls, false)});

    ResponseObject resp = ResponseObject.response(result);
    return resp;
  }

  private Set<Long> getRecurringTaskExecutors(long rtId) {
    Set<Long> result = new HashSet<>();

    Long[] users = qs.getLongColumn(new SqlSelect()
        .addFields(TBL_RT_EXECUTORS, COL_RTEX_USER)
        .addFrom(TBL_RT_EXECUTORS)
        .setWhere(SqlUtils.equals(TBL_RT_EXECUTORS, COL_RTEX_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        if (usr.isActive(user)) {
          result.add(user);
        }
      }
    }

    users = qs.getLongColumn(new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_USER_GROUPS, COL_UG_USER)
        .addFrom(TBL_RT_EXECUTOR_GROUPS)
        .addFromInner(TBL_USER_GROUPS,
            SqlUtils.join(TBL_RT_EXECUTOR_GROUPS, COL_RTEXGR_GROUP,
                TBL_USER_GROUPS, COL_UG_GROUP))
        .setWhere(SqlUtils.equals(TBL_RT_EXECUTOR_GROUPS, COL_RTEXGR_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        if (usr.isActive(user)) {
          result.add(user);
        }
      }
    }

    return result;
  }

  private SimpleRowSet getRecurringTaskFileData(long rtId) {
    return qs.getData(new SqlSelect()
        .addFields(TBL_RT_FILES, COL_FILE, COL_FILE_CAPTION)
        .addFrom(TBL_RT_FILES)
        .setWhere(SqlUtils.equals(TBL_RT_FILES, COL_RTF_RECURRING_TASK, rtId)));
  }

  private Set<Long> getRecurringTaskObservers(long rtId) {
    Set<Long> result = new HashSet<>();

    Long[] users = qs.getLongColumn(new SqlSelect()
        .addFields(TBL_RT_OBSERVERS, COL_RTOB_USER)
        .addFrom(TBL_RT_OBSERVERS)
        .setWhere(SqlUtils.equals(TBL_RT_OBSERVERS, COL_RTOB_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        if (usr.isActive(user)) {
          result.add(user);
        }
      }
    }

    users = qs.getLongColumn(new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_USER_GROUPS, COL_UG_USER)
        .addFrom(TBL_RT_OBSERVER_GROUPS)
        .addFromInner(TBL_USER_GROUPS,
            SqlUtils.join(TBL_RT_OBSERVER_GROUPS, COL_RTOBGR_GROUP,
                TBL_USER_GROUPS, COL_UG_GROUP))
        .setWhere(SqlUtils.equals(TBL_RT_OBSERVER_GROUPS, COL_RTOBGR_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        if (usr.isActive(user)) {
          result.add(user);
        }
      }
    }

    return result;
  }

  private Collection<RowChildren> getRelations(String relTaskColumn, long taskId) {
   Collection<RowChildren> relations = new ArrayList<>();
   DataInfo relationsView = sys.getDataInfo(TBL_RELATIONS);


    for (BeeColumn relation : relationsView.getColumns()) {
      if (relation.isForeign() && !relation.isEditable()) {
        continue;
      }


      Long[] ids = qs.getRelatedValues(TBL_RELATIONS, relTaskColumn, taskId,
          relation.getId());

      if (!ArrayUtils.isEmpty(ids)) {
        relations.add(RowChildren.create(TBL_RELATIONS, relTaskColumn, null, relation.getId(),
            DataUtils.buildIdList(ids)));
      }
    }
    return relations;
  }

  private ResponseObject getRequestFiles(Long requestId) {
    Assert.state(DataUtils.isId(requestId));

    SimpleRowSet data =
        qs.getData(new SqlSelect()
            .addFields(TBL_REQUEST_FILES, COL_FILE, COL_CAPTION)
            .addFields(TBL_FILES, COL_FILE_HASH, COL_FILE_NAME, COL_FILE_SIZE, COL_FILE_TYPE)
            .addFrom(TBL_REQUEST_FILES)
            .addFromInner(TBL_FILES,
                sys.joinTables(TBL_FILES, TBL_REQUEST_FILES, COL_FILE))
            .setWhere(SqlUtils.equals(TBL_REQUEST_FILES, COL_REQUEST, requestId)));

    List<FileInfo> files = new ArrayList<>();

    for (SimpleRow file : data) {
      FileInfo sf = new FileInfo(file.getLong(COL_FILE),
          file.getValue(COL_FILE_HASH),
          file.getValue(COL_FILE_NAME),
          file.getLong(COL_FILE_SIZE),
          file.getValue(COL_FILE_TYPE));

      sf.setCaption(file.getValue(COL_CAPTION));
      sf.setIcon(ExtensionIcons.getIcon(sf.getName()));
      files.add(sf);
    }
    return ResponseObject.response(files);
  }

  private ResponseObject getReportData(RequestInfo reqInfo) {

    String ownerPerson = SqlUtils.uniqueName();
    String executorPerson = SqlUtils.uniqueName();

    SqlSelect select = new SqlSelect();
    select.addField(TaskConstants.TBL_TASKS, sys.getIdName(TaskConstants.TBL_TASKS),
        TaskConstants.COL_TASK);
    select.addFields(TaskConstants.TBL_TASKS, COL_SUMMARY, COL_STATUS,
        COL_PRIORITY, COL_START_TIME, COL_FINISH_TIME, COL_EXPECTED_DURATION);
    select.addField(VIEW_TASK_PRODUCTS, COL_PRODUCT_NAME, ALS_TASK_PRODUCT_NAME);
    select.addField(VIEW_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME);
    select.addField(VIEW_TASK_TYPES, COL_TASK_TYPE_NAME, ALS_TASK_TYPE_NAME);
    select.addField(ProjectConstants.VIEW_PROJECTS,
        ProjectConstants.COL_PROJECT_NAME, ProjectConstants.ALS_PROJECT_NAME);
    select.addField(ProjectConstants.VIEW_PROJECT_STAGES,
        ProjectConstants.COL_STAGE_NAME, ProjectConstants.ALS_STAGE_NAME);

    select.addField(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME, ALS_DURATION_TYPE_NAME);
    select.addFields(TBL_EVENT_DURATIONS, COL_DURATION_DATE, COL_DURATION);

    addExprForUserFirstLastNames(select, ownerPerson, TaskConstants.COL_OWNER);

    addExprForUserFirstLastNames(select, executorPerson, TaskConstants.COL_EXECUTOR);

    addExprForUserFirstLastNames(select, TBL_PERSONS, COL_USER);

    select.addFrom(TaskConstants.TBL_TASKS);

    addJoinsForUserFirstLastNames(select, ownerPerson, TBL_TASKS, COL_OWNER);

    addJoinsForUserFirstLastNames(select, executorPerson, TBL_TASKS, COL_EXECUTOR);

    select.addFromLeft(VIEW_TASK_PRODUCTS, sys.joinTables(
        VIEW_TASK_PRODUCTS, TBL_TASKS,
        COL_PRODUCT));

    select.addFromLeft(VIEW_COMPANIES, sys.joinTables(
        VIEW_COMPANIES, TBL_TASKS,
        COL_COMPANY));

    select.addFromLeft(VIEW_TASK_TYPES, sys.joinTables(
        VIEW_TASK_TYPES, TBL_TASKS,
        COL_TASK_TYPE));

    select.addFromLeft(ProjectConstants.VIEW_PROJECTS, sys.joinTables(
        ProjectConstants.VIEW_PROJECTS, TBL_TASKS,
        ProjectConstants.COL_PROJECT));

    select.addFromLeft(ProjectConstants.VIEW_PROJECT_STAGES, sys.joinTables(
        ProjectConstants.VIEW_PROJECT_STAGES, TBL_TASKS,
        ProjectConstants.COL_PROJECT_STAGE));

    select.addFromLeft(VIEW_TASK_EVENTS, sys.joinTables(
        TBL_TASKS, VIEW_TASK_EVENTS,
        COL_TASK));

    select.addFromLeft(TBL_EVENT_DURATIONS, sys.joinTables(
        TBL_EVENT_DURATIONS, VIEW_TASK_EVENTS,
        COL_EVENT_DURATION));

    select.addFromLeft(TBL_DURATION_TYPES, sys.joinTables(
        TBL_DURATION_TYPES, TBL_EVENT_DURATIONS,
        COL_DURATION_TYPE));

    select.addFromLeft(VIEW_TASK_USERS, SqlUtils.and(sys.joinTables(
        TBL_TASKS, VIEW_TASK_USERS,
        COL_TASK),
        SqlUtils.notEqual(VIEW_TASK_USERS, COL_USER,  SqlUtils.field(TBL_TASKS, COL_EXECUTOR)),
        SqlUtils.notEqual(VIEW_TASK_USERS, COL_USER,  SqlUtils.field(TBL_TASKS, COL_OWNER))));

    addJoinsForUserFirstLastNames(select, TBL_PERSONS, VIEW_TASK_USERS, COL_USER);

    ReportInfo report = ReportInfo.restore(reqInfo.getParameter(Service.VAR_DATA));
    HasConditions clause = SqlUtils.and();
    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(TBL_TASKS,
        sys.getIdName(TBL_TASKS)), SqlConstants.SqlDataType.STRING, 20, 0), COL_TASK));
    clause.add(report.getCondition(TBL_TASKS, COL_SUMMARY));
    clause.add(report.getCondition(TBL_TASKS, COL_PRIORITY));
    clause.add(report.getCondition(TBL_TASKS, COL_STATUS));
    clause.add(report.getCondition(SqlUtils.field(VIEW_TASK_TYPES, COL_TASK_TYPE_NAME),
        ALS_TASK_TYPE_NAME));
    clause.add(report.getCondition(SqlUtils.field(VIEW_TASK_PRODUCTS, COL_PRODUCT_NAME),
        ALS_TASK_PRODUCT_NAME));
    clause.add(report.getCondition(SqlUtils.field(VIEW_COMPANIES, COL_COMPANY_NAME),
        ALS_COMPANY_NAME));
    clause.add(report.getCondition(TBL_TASKS, COL_START_TIME));
    clause.add(report.getCondition(TBL_TASKS, COL_FINISH_TIME));
    clause.add(report.getCondition(TBL_TASKS, COL_EXPECTED_DURATION));
    clause.add(report.getCondition(SqlUtils.field(ProjectConstants.VIEW_PROJECTS,
        ProjectConstants.COL_PROJECT_NAME), ProjectConstants.ALS_PROJECT_NAME));
    clause.add(report.getCondition(SqlUtils.field(ProjectConstants.VIEW_PROJECT_STAGES,
        ProjectConstants.COL_STAGE_NAME), ProjectConstants.ALS_STAGE_NAME));
    clause.add(report.getCondition(SqlUtils.field(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME),
        ALS_DURATION_TYPE_NAME));
    clause.add(report.getCondition(SqlUtils.field(TBL_EVENT_DURATIONS, COL_DURATION_DATE),
        COL_DURATION));
    clause.add(report.getCondition(SqlUtils.concat(
        SqlUtils.field(ownerPerson, COL_FIRST_NAME), "' '",
        SqlUtils.nvl(SqlUtils.field(ownerPerson, COL_LAST_NAME), "''")), COL_OWNER));
    clause.add(report.getCondition(SqlUtils.concat(
        SqlUtils.field(executorPerson, COL_FIRST_NAME), "' '",
        SqlUtils.nvl(SqlUtils.field(executorPerson, COL_LAST_NAME), "''")), COL_EXECUTOR));
    clause.add(report.getCondition(SqlUtils.concat(
        SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
        SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")), COL_USER));
    select.setWhere(clause);

    String tmp = qs.sqlCreateTemp(select);
    select = new SqlSelect().addFrom(tmp);

    for (String column : qs.getData(new SqlSelect()
        .addAllFields(tmp)
        .addFrom(tmp)
        .setWhere(SqlUtils.sqlFalse())).getColumnNames()) {

      if (report.requiresField(column)) {
        select.addFields(tmp, column);
      }
    }
    SimpleRowSet data = qs.getData(select);
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(data);
  }

  private ResponseObject getReportData2(RequestInfo reqInfo) {
    String ownerPerson = SqlUtils.uniqueName();
    String supplier = SqlUtils.uniqueName();
    String decorationSupplier = SqlUtils.uniqueName();

    SqlSelect select = new SqlSelect();
    select.addField(VIEW_TASKS, sys.getIdName(VIEW_TASKS), COL_TASK_ID);
    select.addFields(VIEW_TASKS, COL_START_TIME, COL_FINISH_TIME, COL_SUMMARY, COL_STATUS);
    select.addField(VIEW_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME);
    select.addField(supplier, COL_COMPANY_NAME, "Supplier");
    select.addField(decorationSupplier, COL_COMPANY_NAME, "DecorationSupplier");
    select.addFields(VIEW_TASK_ORDERS, TransportConstants.COL_ORDER_NO, "InvoiceSeries", "InvoiceNumber",
      "InvoiceDate", "TransportationNote", "PrintFormat", "ProductFormat", "PaperEnvelope", "PaperEnvelopeNote",
      "NumberOfRuns", "ArcNumber", "Price", "DecorationPrice", "PageNumber", "From", "To", "Series", "Color1",
      "Color2","InvoiceSupplier");
    select.addFields(VIEW_TASK_ORDER_ITEMS, COL_CURRENCY_RATE_QUANTITY, COL_DESCRIPTION);
    select.addField(VIEW_TASK_ORDER_ITEMS, COL_ITEM_PRICE, COL_ITEM + COL_ITEM_PRICE);
    select.addField(VIEW_ITEMS, COL_ITEM_NAME, ALS_ITEM_NAME);
    addExprForUserFirstLastNames(select, ownerPerson, TaskConstants.COL_OWNER);
    select.addFrom(VIEW_TASKS);

    addJoinsForUserFirstLastNames(select, ownerPerson, TBL_TASKS, COL_OWNER);

    select.addFromLeft(VIEW_TASK_ORDERS, sys.joinTables(VIEW_TASK_ORDERS, TBL_TASKS, COL_TASK_ORDER));
    select.addFromLeft(VIEW_TASK_ORDER_ITEMS, sys.joinTables(TBL_TASKS, VIEW_TASK_ORDER_ITEMS, COL_TASK));
    select.addFromLeft(VIEW_ITEMS, sys.joinTables(VIEW_ITEMS, VIEW_TASK_ORDER_ITEMS, COL_ITEM));
    select.addFromLeft(VIEW_COMPANIES, sys.joinTables(VIEW_COMPANIES, TBL_TASKS, COL_COMPANY));
    select.addFromLeft(VIEW_COMPANIES, supplier, sys.joinTables(VIEW_COMPANIES, supplier,
      VIEW_TASK_ORDERS, "Supplier"));
    select.addFromLeft(VIEW_COMPANIES, decorationSupplier, sys.joinTables(VIEW_COMPANIES, decorationSupplier,
      VIEW_TASK_ORDERS, "DecorationSupplier"));

    ReportInfo report = ReportInfo.restore(reqInfo.getParameter(Service.VAR_DATA));
    HasConditions clause = SqlUtils.and();
    clause.add(SqlUtils.notNull(VIEW_TASKS, COL_TASK_ORDER));
    clause.add(report.getCondition(TBL_TASKS, COL_START_TIME));
    clause.add(report.getCondition(TBL_TASKS, COL_FINISH_TIME));
    clause.add(report.getCondition(TBL_TASKS, COL_SUMMARY));
    clause.add(report.getCondition(TBL_TASKS, COL_STATUS));
    clause.add(report.getCondition(SqlUtils.field(VIEW_COMPANIES, COL_COMPANY_NAME), ALS_COMPANY_NAME));
    clause.add(report.getCondition(SqlUtils.field(supplier, COL_COMPANY_NAME), "Supplier"));
    clause.add(report.getCondition(SqlUtils.field(decorationSupplier, COL_COMPANY_NAME), "DecorationSupplier"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "InvoiceSupplier"));
    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(TBL_TASKS,
      sys.getIdName(TBL_TASKS)), SqlConstants.SqlDataType.STRING, 20, 0), COL_TASK_ID));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, TransportConstants.COL_ORDER_NO));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "InvoiceSeries"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "InvoiceNumber"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "InvoiceDate"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "TransportationNote"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "PrintFormat"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "ProductFormat"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "PaperEnvelope"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "PaperEnvelopeNote"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "NumberOfRuns"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "ArcNumber"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "Price"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "DecorationPrice"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "PageNumber"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "From"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "To"));
    clause.add(report.getCondition(VIEW_TASK_ORDERS, "Series"));
    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(VIEW_TASK_ORDERS,
      "Color1"), SqlConstants.SqlDataType.STRING, 20, 0), "Color1"));
    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(VIEW_TASK_ORDERS,
      "Color2"), SqlConstants.SqlDataType.STRING, 20, 0), "Color2"));
    clause.add(report.getCondition(SqlUtils.concat(SqlUtils.field(ownerPerson, COL_FIRST_NAME), "' '",
      SqlUtils.nvl(SqlUtils.field(ownerPerson, COL_LAST_NAME), "''")), COL_OWNER));
    clause.add(report.getCondition(VIEW_TASK_ORDER_ITEMS, COL_CURRENCY_RATE_QUANTITY));
    clause.add(report.getCondition(SqlUtils.field(VIEW_TASK_ORDER_ITEMS, COL_ITEM_PRICE),
      COL_ITEM + COL_ITEM_PRICE));
    clause.add(report.getCondition(SqlUtils.field(VIEW_ITEMS, COL_ITEM_NAME), ALS_ITEM_NAME));
    select.setWhere(clause);

    String tmp = qs.sqlCreateTemp(select);
    select = new SqlSelect().addFrom(tmp);

    for (String column : qs.getData(new SqlSelect()
      .addAllFields(tmp)
      .addFrom(tmp)
      .setWhere(SqlUtils.sqlFalse())).getColumnNames()) {

      if (report.requiresField(column)) {
        select.addFields(tmp, column);
      }
    }
    SimpleRowSet data = qs.getData(select);
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(data);
  }

  private void addExprForUserFirstLastNames(SqlSelect select, String table, String col) {
    select.addExpr(SqlUtils.concat(SqlUtils.nvl(SqlUtils.field(table, COL_FIRST_NAME),
        SqlUtils.constant(BeeConst.STRING_EMPTY)),
        SqlUtils.constant(BeeConst.STRING_SPACE),
        SqlUtils.nvl(SqlUtils.field(table, COL_LAST_NAME),
            SqlUtils.constant(BeeConst.STRING_EMPTY))), col);
  }

  private void addJoinsForUserFirstLastNames(SqlSelect select, String personTblAls,
      String table, String col) {
    String userTblAls = SqlUtils.uniqueName();
    String companyPersonTblAls = SqlUtils.uniqueName();

    select.addFromLeft(AdministrationConstants.TBL_USERS, userTblAls, sys.joinTables(
        AdministrationConstants.TBL_USERS, userTblAls, table,
        col));

    select.addFromLeft(TBL_COMPANY_PERSONS, companyPersonTblAls, sys.joinTables(
        TBL_COMPANY_PERSONS, companyPersonTblAls, userTblAls, COL_COMPANY_PERSON));

    select.addFromLeft(TBL_PERSONS, personTblAls, sys.joinTables(
        TBL_PERSONS, personTblAls, companyPersonTblAls, COL_PERSON));
  }

  private ResponseObject getSchedulingData(RequestInfo reqInfo) {
    Long rtId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_RT_ID));
    if (!DataUtils.isId(rtId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_ID);
    }

    Map<String, String> data = new HashMap<>();

    Set<Long> executors = getRecurringTaskExecutors(rtId);
    if (!executors.isEmpty()) {
      BeeRowSet users = qs.getViewData(VIEW_USERS, Filter.idIn(executors));
      if (!DataUtils.isEmpty(users)) {
        data.put(users.getViewName(), users.serialize());
      }
    }

    BeeRowSet rtDates = qs.getViewData(VIEW_RT_DATES, Filter.equals(COL_RTD_RECURRING_TASK, rtId));
    if (!DataUtils.isEmpty(rtDates)) {
      data.put(rtDates.getViewName(), rtDates.serialize());
    }

    BeeRowSet tasks = qs.getViewData(VIEW_TASKS, Filter.equals(COL_RECURRING_TASK, rtId),
        Order.ascending(ALS_EXECUTOR_LAST_NAME, ALS_EXECUTOR_FIRST_NAME));
    if (!DataUtils.isEmpty(tasks)) {
      data.put(tasks.getViewName(), tasks.serialize());
    }

    if (data.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(data);
    }
  }

  private Set<JustDate> getSpawnedDates(long rtId, DateRange range) {
    Set<JustDate> dates = new HashSet<>();

    DateTime[] startTimes = qs.getDateTimeColumn(new SqlSelect().setDistinctMode(true)
        .addFields(TBL_TASKS, COL_START_TIME)
        .addFrom(TBL_TASKS)
        .setWhere(SqlUtils.equals(TBL_TASKS, COL_RECURRING_TASK, rtId)));

    if (startTimes != null && startTimes.length > 0) {
      for (DateTime startTime : startTimes) {
        JustDate date = JustDate.get(startTime);
        if (date == null) {
          continue;
        }

        if (range != null && !range.contains(date)) {
          continue;
        }

        dates.add(date);
      }
    }

    return dates;
  }

  private ResponseObject getTaskData(long taskId, Long eventId, Collection<String> propNames,
                                     boolean addRelations) {

    BeeRowSet rowSet = qs.getViewData(VIEW_TASKS, Filter.compareId(taskId));
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(SVC_GET_TASK_DATA, "task id: " + taskId + " not found");
    }
    List<Long> observers = getTaskUsers(taskId);
    observers.remove(rowSet.getLong(0, COL_OWNER));
    observers.remove(rowSet.getLong(0, COL_EXECUTOR));

    BeeRow data = rowSet.getRow(0);

    addTaskProperties(data, observers, eventId, propNames, addRelations);

    return ResponseObject.response(data);
  }

  private ResponseObject getTaskData(RequestInfo reqInfo) {
    long taskId = BeeUtils.toLong(reqInfo.getParameter(VAR_TASK_ID));
    if (!DataUtils.isId(taskId)) {
      String msg = BeeUtils.joinWords(reqInfo.getService(), "task id not received");
      logger.warning(msg);
      return ResponseObject.error(msg);
    }

    Set<String> propNames = new HashSet<>();

    String propList = reqInfo.getParameter(VAR_TASK_PROPERTIES);
    if (!BeeUtils.isEmpty(propList)) {
      propNames.addAll(NameUtils.toSet(propList));
    }

    return getTaskData(taskId, null, propNames, !BeeUtils.isEmpty(VAR_COPY_RELATIONS));
  }

  private List<FileInfo> getTaskFiles(long taskId) {
    List<FileInfo> result = new ArrayList<>();

    BeeRowSet rowSet = qs.getViewData(VIEW_TASK_FILES, Filter.equals(COL_TASK, taskId));
    if (rowSet == null || rowSet.isEmpty()) {
      return result;
    }

    for (BeeRow row : rowSet.getRows()) {
      FileInfo sf = new FileInfo(DataUtils.getLong(rowSet, row, COL_FILE),
          DataUtils.getString(rowSet, row, COL_FILE_HASH),
          DataUtils.getString(rowSet, row, ALS_FILE_NAME),
          DataUtils.getLong(rowSet, row, ALS_FILE_SIZE),
          DataUtils.getString(rowSet, row, ALS_FILE_TYPE));

      Long teId = DataUtils.getLong(rowSet, row, COL_TASK_EVENT);
      if (teId != null) {
        sf.setRelatedId(teId);
      }

      String caption = DataUtils.getString(rowSet, row, COL_CAPTION);
      if (!BeeUtils.isEmpty(caption)) {
        sf.setCaption(caption);
      }

      sf.setIcon(ExtensionIcons.getIcon(sf.getName()));
      result.add(sf);
    }

    return result;
  }

  private List<Long> getTaskUsers(long taskId) {
    if (!DataUtils.isId(taskId)) {
      return new ArrayList<>();
    }

    SqlSelect query = new SqlSelect()
        .addFrom(TBL_TASK_USERS)
        .addFields(TBL_TASK_USERS, COL_USER)
        .setWhere(SqlUtils.equals(TBL_TASK_USERS, COL_TASK, taskId))
        .addOrder(TBL_TASK_USERS, sys.getIdName(TBL_TASK_USERS));

    return Lists.newArrayList(qs.getLongColumn(query));
  }

  private ResponseObject getTypeHoursReport(RequestInfo reqInfo) {
    Dictionary constants = usr.getDictionary();
    SqlSelect durationTypes = new SqlSelect()
        .addFrom(TBL_DURATION_TYPES)
        .addFields(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME)
        .setWhere(SqlUtils.sqlTrue())
        .addOrder(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME);

    boolean hideTimeZeros = false;

    if (reqInfo.hasParameter(VAR_TASK_DURATION_TYPE)) {
      durationTypes.setWhere(SqlUtils.and(durationTypes.getWhere(), SqlUtils.inList(
          TBL_DURATION_TYPES, sys.getIdName(TBL_DURATION_TYPES), DataUtils.parseIdList(reqInfo
              .getParameter(VAR_TASK_DURATION_TYPE)))));
    }

    if (reqInfo.hasParameter(VAR_TASK_DURATION_HIDE_ZEROS)) {
      hideTimeZeros = true;
    }

    SimpleRowSet dTypesList = qs.getData(durationTypes);
    SimpleRowSet result = new SimpleRowSet(new String[] {COL_DURATION_TYPE_NAME, COL_DURATION});

    result.addRow(new String[] {constants.crmDurationType(), constants.crmSpentTime()});
    Assert.notNull(dTypesList);

    long totalTimeMls = 0;

    for (int i = 0; i < dTypesList.getNumberOfRows(); i++) {
      String dType = dTypesList.getValue(i, dTypesList.getColumnIndex(COL_DURATION_TYPE_NAME));

      SqlSelect dTypeTime = new SqlSelect()
          .addFrom(TBL_TASK_EVENTS)
          .addFromRight(TBL_EVENT_DURATIONS,
              sys.joinTables(TBL_EVENT_DURATIONS, TBL_TASK_EVENTS, COL_EVENT_DURATION))
          .addFromLeft(TBL_DURATION_TYPES,
              sys.joinTables(TBL_DURATION_TYPES, TBL_EVENT_DURATIONS, COL_DURATION_TYPE))
          .addFromLeft(TBL_TASKS,
              sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
          .addFromLeft(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_TASK_EVENTS, COL_PUBLISHER))
          .addFields(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME)
          .addFields(TBL_EVENT_DURATIONS, COL_DURATION)
          .setWhere(SqlUtils.equals(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME, dType));

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_FROM)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_FROM))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils
              .moreEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_FROM))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_TO)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_TO))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils
              .lessEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_TO))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_COMPANY)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_COMPANY))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils.inList(
              TBL_COMPANIES, sys.getIdName(TBL_COMPANIES),
              DataUtils.parseIdList(reqInfo.getParameter(VAR_TASK_COMPANY)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_PUBLISHER)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_PUBLISHER))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils.inList(
              TBL_USERS, sys.getIdName(TBL_USERS), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_PUBLISHER)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_PROJECT)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_PROJECT))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils.inList(
              TBL_TASKS, ProjectConstants.COL_PROJECT, DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_PROJECT)))));
        }
      }

      SimpleRowSet dTypeTimes = qs.getData(dTypeTime);
      Assert.notNull(dTypeTimes);

      long dTimeMls = 0;

      for (int j = 0; j < dTypeTimes.getNumberOfRows(); j++) {
        Long timeMls = TimeUtils.parseTime(dTypeTimes.getValue(j, COL_DURATION));
        dTimeMls += timeMls;
      }

      totalTimeMls += dTimeMls;

      if (!(hideTimeZeros && dTimeMls <= 0)) {
        result.addRow(new String[] {dType, TimeUtils.renderTime(dTimeMls, false)});
      }
    }

    result.addRow(new String[] {
        constants.totalOf() + ":", TimeUtils.renderTime(totalTimeMls, false)});

    ResponseObject resp = ResponseObject.response(result);
    return resp;
  }

  private ResponseObject getUsersHoursReport(RequestInfo reqInfo) {
    Dictionary constants = usr.getDictionary();
    SqlSelect userListQuery =
        new SqlSelect()
            .addFields(TBL_USERS, sys.getIdName(TBL_USERS))
            .addFields(TBL_PERSONS,
                COL_FIRST_NAME,
                COL_LAST_NAME)
            .addFrom(TBL_USERS)
            .addFromLeft(
                TBL_COMPANY_PERSONS,
                sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS,
                    COL_COMPANY_PERSON))
            .addFromLeft(
                TBL_PERSONS,
                sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS,
                    COL_PERSON)).setWhere(SqlUtils.sqlTrue())
            .setWhere(SqlUtils.sqlTrue())
            .addOrder(TBL_PERSONS, COL_FIRST_NAME,
                COL_LAST_NAME);

    boolean hideTimeZeros = false;

    if (reqInfo.hasParameter(VAR_TASK_PUBLISHER)) {
      userListQuery.setWhere(SqlUtils.and(userListQuery.getWhere(), SqlUtils.inList(
          TBL_USERS, sys.getIdName(TBL_USERS), DataUtils
              .parseIdList(reqInfo.getParameter(VAR_TASK_PUBLISHER)))));
    }

    if (reqInfo.hasParameter(VAR_TASK_DURATION_HIDE_ZEROS)) {
      hideTimeZeros = true;
    }

    SimpleRowSet usersListSet = qs.getData(userListQuery);
    SimpleRowSet result = new SimpleRowSet(new String[] {COL_USER, COL_DURATION});
    long totalTimeMls = 0;

    result.addRow(new String[] {
        constants.executorFullName(), constants.crmSpentTime()});

    for (int i = 0; i < usersListSet.getNumberOfRows(); i++) {
      String userFullName =
          (!BeeUtils.isEmpty(usersListSet.getValue(i, COL_FIRST_NAME))
              ? usersListSet.getValue(i, COL_FIRST_NAME) : "") + " "
              + (!BeeUtils.isEmpty(usersListSet.getValue(i, COL_LAST_NAME))
              ? usersListSet.getValue(i, COL_LAST_NAME) : "");

      userFullName = BeeUtils.isEmpty(userFullName) ? "—" : userFullName;

      SqlSelect userTimesQuery = new SqlSelect()
          .addFields(TBL_EVENT_DURATIONS, COL_DURATION)
          .addFrom(TBL_TASK_EVENTS)
          .addFromRight(TBL_EVENT_DURATIONS,
              sys.joinTables(TBL_EVENT_DURATIONS, TBL_TASK_EVENTS, COL_EVENT_DURATION))
          .addFromLeft(TBL_DURATION_TYPES,
              sys.joinTables(TBL_DURATION_TYPES, TBL_EVENT_DURATIONS, COL_DURATION_TYPE))
          .addFromLeft(TBL_TASKS,
              sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
          .addFromLeft(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_TASK_EVENTS, COL_PUBLISHER))
          .setWhere(
              SqlUtils.equals(TBL_USERS, sys
                  .getIdName(TBL_USERS), usersListSet.getValue(i, sys
                  .getIdName(TBL_USERS))));

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_FROM)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_FROM))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils
              .moreEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_FROM))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_TO)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_TO))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils
              .lessEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_TO))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_COMPANY)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_COMPANY))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils.inList(
              TBL_COMPANIES, sys.getIdName(TBL_COMPANIES),
              DataUtils.parseIdList(reqInfo.getParameter(VAR_TASK_COMPANY)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_TYPE)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_TYPE))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils.inList(
              TBL_DURATION_TYPES, sys.getIdName(TBL_DURATION_TYPES), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_DURATION_TYPE)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_PROJECT)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_PROJECT))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils.inList(
              TBL_TASKS, ProjectConstants.COL_PROJECT, DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_PROJECT)))));
        }
      }

      SimpleRowSet companyTimes = qs.getData(userTimesQuery);
      long dTimeMls = 0;

      for (int j = 0; j < companyTimes.getNumberOfRows(); j++) {
        Long timeMls = TimeUtils.parseTime(companyTimes.getValue(j, companyTimes
            .getColumnIndex(COL_DURATION)));
        dTimeMls += timeMls;
      }

      totalTimeMls += dTimeMls;

      if (!(hideTimeZeros && dTimeMls <= 0)) {
        result.addRow(new String[] {userFullName, TimeUtils.renderTime(dTimeMls, false)});
      }
    }

    result.addRow(new String[] {
        constants.totalOf() + ":", TimeUtils.renderTime(totalTimeMls, false)});

    ResponseObject resp = ResponseObject.response(result);
    return resp;
  }

  private ResponseObject mailNewTask(Long senderAccountId, long taskId,
      boolean ownerPreference, boolean automatic) {
    return mailNewTask(senderAccountId, taskId, ownerPreference, automatic, null, null);
  }

  private ResponseObject mailNewTask(Long senderAccountId, long taskId,
      boolean ownerPreference, boolean automatic,
      Long recipientId, String mailSubject) {

    ResponseObject response = ResponseObject.emptyResponse();
    String label = "mail new task";

    SqlSelect query =
        new SqlSelect()
            .addFields(TBL_TASKS, COL_SUMMARY, COL_DESCRIPTION, COL_OWNER,
                COL_EXECUTOR, COL_START_TIME, COL_FINISH_TIME)
            .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
            .addField(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME, ALS_COMPANY_TYPE_NAME)
            .addFields(VIEW_TASK_ORDERS, "OrderNo")
            .addFrom(TBL_TASKS)
            .addFromLeft(VIEW_TASK_ORDERS,
                sys.joinTables(VIEW_TASK_ORDERS, TBL_TASKS, COL_TASK_ORDER))
            .addFromLeft(TBL_COMPANIES,
                sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
            .addFromLeft(TBL_COMPANY_TYPES,
                sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE));

    HasConditions where = SqlUtils.and(SqlUtils.equals(TBL_TASKS, COL_TASK_ID, taskId));

    if (!ownerPreference) {
      query.addFromInner(TBL_USER_SETTINGS,
          SqlUtils.join(TBL_USER_SETTINGS, COL_USER, TBL_TASKS, COL_EXECUTOR));
      where.add(SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_ASSIGNED_TASKS));
    }

    if (!automatic) {
      where.add(SqlUtils.notEqual(TBL_TASKS, COL_OWNER, SqlUtils.field(TBL_TASKS, COL_EXECUTOR)));
    }

    query.setWhere(where);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return response;
    }

    SimpleRow row = data.getRow(0);

    Long executor = row.getLong(COL_EXECUTOR);

    Long recipientUser = recipientId;
    if (recipientUser == null) {
      recipientUser = executor;
    }

    String recipientEmail = usr.getUserEmail(recipientUser, false);

    if (BeeUtils.isEmpty(recipientEmail)) {
      logger.warning(label, taskId, "recipient user", recipientUser, "email not available");
      return response;
    }

    Dictionary constants = usr.getDictionary(recipientUser);
    if (constants == null) {
      logger.warning(label, taskId, "recipient user", recipientUser, "localization not available");
      return response;
    }

    Document document =
        taskToHtml(taskId, row.getDateTime(COL_START_TIME),
            row.getDateTime(COL_FINISH_TIME), row.getValue(COL_SUMMARY),
            row.getValue(COL_DESCRIPTION), BeeUtils.joinItems(row.getValue(ALS_COMPANY_NAME), row
                .getValue(ALS_COMPANY_TYPE_NAME)), row.getLong(COL_OWNER),
            executor, constants, usr.getDateTimeFormatInfo(recipientUser));
    String content = document.buildLines();

    logger.info(label, taskId, "mail to", recipientUser, recipientEmail);

    String headerCaption = BeeUtils.joinWords("Užsakymo Nr.", row.getValue("OrderNo"),
      "(ID: " + taskId + ")", row.getValue(COL_SUMMARY));

    ResponseObject mailResponse = mail.sendStyledMail(senderAccountId, recipientEmail,
        mailSubject == null ? constants.crmMailTaskSubject() : mailSubject, content, headerCaption);

    if (mailResponse.hasErrors()) {
      response.addWarning("Send mail failed");
    }
    return response;
  }

  private void mailScheduledTasks(Set<Long> tasks) {
    if (BeeUtils.isEmpty(tasks)) {
      return;
    }

    Long senderAccountId = mail.getSenderAccountId("scheduled tasks:");
    if (senderAccountId == null) {
      return;
    }

    HasConditions where = SqlUtils.and(
        SqlUtils.inList(TBL_TASKS, COL_TASK_ID, tasks),
        SqlUtils.or(
            SqlUtils.notNull(TBL_RECURRING_TASKS, COL_RT_COPY_BY_MAIL),
            SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_ASSIGNED_TASKS)));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TASKS, COL_TASK_ID)
        .addFrom(TBL_TASKS)
        .addFromLeft(TBL_RECURRING_TASKS,
            sys.joinTables(TBL_RECURRING_TASKS, TBL_TASKS, COL_RECURRING_TASK))
        .addFromLeft(TBL_USER_SETTINGS,
            SqlUtils.join(TBL_USER_SETTINGS, COL_USER, TBL_TASKS, COL_EXECUTOR))
        .setWhere(where);

    Long[] ids = qs.getLongColumn(query);
    if (ids == null || ids.length == 0) {
      return;
    }

    for (Long id : ids) {
      ResponseObject response = mailNewTask(senderAccountId, id, true, true);
      if (response.hasErrors() || response.hasWarnings()) {
        logger.warning("mail scheduled tasks canceled");
        break;
      }
    }
  }

  @Deprecated
  private int maybeUpdateTaskStatus() {
    int updated = 0;

    IsCondition where = SqlUtils.and(
        SqlUtils.equals(TBL_TASKS, COL_STATUS, TaskStatus.SCHEDULED.ordinal()),
        SqlUtils.less(TBL_TASKS, COL_START_TIME, TimeUtils.startOfDay(1)));

    SqlUpdate activate = new SqlUpdate(TBL_TASKS)
        .addConstant(COL_STATUS, TaskStatus.ACTIVE.ordinal())
        .setWhere(SqlUtils.and(where, SqlUtils.equals(TBL_TASKS, COL_EXECUTOR,
            SqlUtils.field(TBL_TASKS, COL_OWNER))));

    int activated = qs.updateData(activate);
    if (activated > 0) {
      updated += activated;
    }

    SqlUpdate pending = new SqlUpdate(TBL_TASKS)
        .addConstant(COL_STATUS, TaskStatus.NOT_VISITED.ordinal())
        .setWhere(SqlUtils.and(where, SqlUtils.notEqual(TBL_TASKS, COL_EXECUTOR,
            SqlUtils.field(TBL_TASKS, COL_OWNER))));

    int notVisited = qs.updateData(pending);
    if (notVisited > 0) {
      updated += notVisited;
    }

    return updated;
  }

  @Schedule(hour = "4", persistent = false)
  private void recurringTaskSchedulingTimeout() {
    if (!Config.isInitialized()) {
      return;
    }
    logger.info("recurring task scheduling timeout ");

    Set<Long> tasks = scheduleRecurringTasks(DateRange.day(TimeUtils.today()));
    logger.info("recurring task scheduler created", tasks.size(), "tasks");

    if (!tasks.isEmpty()) {
      mailScheduledTasks(tasks);
    }
  }

  private ResponseObject registerTaskDuration(long durationType, Map<String, String> reqInfo) {
    Long date = BeeUtils.toLongOrNull(reqInfo.get(VAR_TASK_DURATION_DATE));
    if (date == null) {
      return ResponseObject.error("task duration date not specified");
    }

    String time = reqInfo.get(VAR_TASK_DURATION_TIME);
    if (BeeUtils.isEmpty(time)) {
      return ResponseObject.error("task duration time not specified");
    }

    return qs.insertDataWithResponse(new SqlInsert(TBL_EVENT_DURATIONS)
        .addConstant(COL_DURATION_TYPE, durationType)
        .addConstant(COL_DURATION_DATE, date)
        .addConstant(COL_DURATION, time));
  }

  private ResponseObject registerTaskEvent(long taskId, long userId, TaskEvent event, long millis) {
    return registerTaskEvent(taskId, userId, event, null, null, null, null, null, millis);
  }

  private ResponseObject registerTaskEvent(long taskId, long userId, TaskEvent event,
      Map<String, String> reqInfo, String note, Long finishTime, long millis) {
    String comment = reqInfo.get(VAR_TASK_COMMENT);
    String eventData = reqInfo.get(COL_EVENT_DATA);

    Long durationId = null;
    Long durationType = BeeUtils.toLongOrNull(reqInfo.get(VAR_TASK_DURATION_TYPE));

    if (DataUtils.isId(durationType)) {
      ResponseObject response = registerTaskDuration(durationType, reqInfo);
      if (response.hasErrors()) {
        return response;
      } else if (response.hasResponse(Long.class)) {
        durationId = (Long) response.getResponse();
      }
    }

    return registerTaskEvent(taskId, userId, event, comment, note, eventData, finishTime,
        durationId, millis);
  }

  private ResponseObject registerTaskEvent(long taskId, long userId, TaskEvent event,
      String comment, String note, String eventData, Long finishTime, Long durationId,
      long millis) {

    SqlInsert si = new SqlInsert(TBL_TASK_EVENTS)
        .addConstant(COL_TASK, taskId)
        .addConstant(COL_PUBLISHER, userId)
        .addConstant(COL_PUBLISH_TIME, millis)
        .addConstant(TaskConstants.COL_EVENT, event.ordinal());

    if (!BeeUtils.isEmpty(comment)) {
      si.addConstant(COL_COMMENT, comment);
    }
    if (!BeeUtils.isEmpty(note)) {
      si.addConstant(COL_EVENT_NOTE, note);
    }

    if (!BeeUtils.isEmpty(eventData)) {
      si.addConstant(COL_EVENT_DATA, eventData);
    }

    if (BeeUtils.isPositive(finishTime)) {
      si.addConstant(COL_FINISH_TIME, finishTime);
    }

    if (DataUtils.isId(durationId)) {
      si.addConstant(COL_EVENT_DURATION, durationId);
    }

    return qs.insertDataWithResponse(si);
  }

  private ResponseObject registerTaskVisit(long taskId, long userId, long millis) {
    IsCondition where = SqlUtils.equals(TBL_TASK_USERS, COL_TASK, taskId, COL_USER, userId);

    return qs.updateDataWithResponse(new SqlUpdate(TBL_TASK_USERS)
        .addConstant(COL_LAST_ACCESS, millis)
        .setWhere(where));
  }

  private Set<Long> scheduleRecurringTasks(DateRange defRange) {
    Set<Long> result = new HashSet<>();

    String label = "scheduling tasks";

    JustDate defStart = defRange.getMinDate();
    JustDate defEnd = defRange.getMaxDate();

    Filter filter = Filter.and(
        Filter.or(Filter.isNull(COL_RT_SCHEDULE_DAYS),
            Filter.isPositive(COL_RT_SCHEDULE_DAYS)),
        Filter.or(Filter.isNull(COL_RT_SCHEDULE_UNTIL),
            Filter.isMoreEqual(COL_RT_SCHEDULE_UNTIL, new DateValue(defStart))));

    BeeRowSet rtData = qs.getViewData(VIEW_RECURRING_TASKS, filter);
    if (DataUtils.isEmpty(rtData)) {
      logger.info(label, defRange, "no active recurring tasks found");
      return result;
    }

    List<BeeColumn> taskColumns = sys.getView(VIEW_TASKS).getRowSetColumns();
    if (BeeUtils.isEmpty(taskColumns)) {
      logger.severe(label, defRange, "task columns not available");
      return result;
    }

    for (BeeRow rtRow : rtData.getRows()) {
      long rtId = rtRow.getId();

      Set<Long> executors = getRecurringTaskExecutors(rtId);
      if (executors.isEmpty()) {
        logger.debug(label, rtId, "has no executors");
        continue;
      }

      JustDate from = DataUtils.getDate(rtData, rtRow, COL_RT_SCHEDULE_FROM);
      if (from == null) {
        logger.warning(label, rtId, COL_RT_SCHEDULE_FROM, "is null");
        continue;
      }

      JustDate until = DataUtils.getDate(rtData, rtRow, COL_RT_SCHEDULE_UNTIL);
      if (until != null && TimeUtils.isLess(until, from)) {
        logger.warning(label, rtId, "invalid range", COL_RT_SCHEDULE_FROM, from,
            COL_RT_SCHEDULE_UNTIL, until);
        continue;
      }

      Integer days = DataUtils.getInteger(rtData, rtRow, COL_RT_SCHEDULE_DAYS);
      if (BeeUtils.isNegative(days)) {
        logger.debug(label, rtId, COL_RT_SCHEDULE_DAYS, days, "not scheduled");
        continue;
      }

      JustDate min = TimeUtils.max(defStart, from);
      JustDate max;

      if (until == null) {
        max = BeeUtils.isPositive(days) ? TimeUtils.nextDay(defEnd, days) : defEnd;
      } else if (BeeUtils.isPositive(days)) {
        max = TimeUtils.min(TimeUtils.nextDay(defEnd, days), until);
      } else {
        max = TimeUtils.min(defEnd, until);
      }

      if (TimeUtils.isLess(max, min)) {
        logger.debug(label, rtId, "min", min, "max", max, "out of range", defRange);
        continue;
      }

      CronExpression.Builder builder = new CronExpression.Builder(from, until)
          .id(BeeUtils.toString(rtId))
          .dayOfMonth(DataUtils.getString(rtData, rtRow, COL_RT_DAY_OF_MONTH))
          .month(DataUtils.getString(rtData, rtRow, COL_RT_MONTH))
          .dayOfWeek(DataUtils.getString(rtData, rtRow, COL_RT_DAY_OF_WEEK))
          .year(DataUtils.getString(rtData, rtRow, COL_RT_YEAR))
          .workdayTransition(EnumUtils.getEnumByIndex(WorkdayTransition.class,
              DataUtils.getInteger(rtData, rtRow, COL_RT_WORKDAY_TRANSITION)));

      BeeRowSet rtDates = qs.getViewData(VIEW_RT_DATES,
          Filter.equals(COL_RTD_RECURRING_TASK, rtId));

      if (!DataUtils.isEmpty(rtDates)) {
        List<ScheduleDateRange> scheduleDateRanges = TaskUtils.getScheduleDateRanges(rtDates);

        for (ScheduleDateRange sdr : scheduleDateRanges) {
          builder.rangeMode(sdr.getRange(), sdr.getMode());
        }
      }

      CronExpression cron = builder.build();

      List<JustDate> cronDates = cron.getDates(min, max);
      if (cronDates.isEmpty()) {
        logger.debug(label, rtId, "no cron dates in", min, max);
        continue;
      }

      Set<JustDate> spawnedDates = getSpawnedDates(rtId, DateRange.closed(min, max));
      if (!spawnedDates.isEmpty()) {
        cronDates.removeAll(spawnedDates);
        if (cronDates.isEmpty()) {
          logger.debug(label, rtId, "all dates already spawned", spawnedDates);
          continue;
        }
      }

      Set<Long> observers = getRecurringTaskObservers(rtId);

      Collection<RowChildren> relations = getRelations(COL_RECURRING_TASK, rtId);
      SimpleRowSet fileData = getRecurringTaskFileData(rtId);

      for (JustDate date : cronDates) {
        Set<Long> tasks = spawnTasks(rtData.getColumns(), rtRow, date,
            executors, observers, relations, fileData, taskColumns);

        if (BeeUtils.isEmpty(tasks)) {
          logger.severe(label, rtId, date, "no tasks created");
          return result;
        }

        result.addAll(tasks);
        logger.info("recurring task", rtId, "scheduled", date, tasks);
      }
    }

    return result;
  }

  private ResponseObject scheduleTasks(RequestInfo reqInfo) {
    Integer from = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_RT_SCHEDULE_FROM));
    if (!BeeUtils.isPositive(from)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_RT_SCHEDULE_FROM);
    }

    Integer until = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_RT_SCHEDULE_UNTIL));
    if (BeeUtils.isPositive(until) && from > until) {
      return ResponseObject.error(reqInfo.getService(), "invalid range", from, until);
    }

    DateRange range;
    if (until == null) {
      range = DateRange.day(new JustDate(from));
    } else {
      range = DateRange.closed(new JustDate(from), new JustDate(until));
    }

    Set<Long> tasks = scheduleRecurringTasks(range);
    if (!tasks.isEmpty()) {
      mailScheduledTasks(tasks);
    }

    return ResponseObject.response(tasks.size());
  }

  private int sendTaskReminders(long timeRemaining) {
    int count = 0;
    String label = "task reminders:";

    Long accountId = mail.getSenderAccountId(label);

    if (!DataUtils.isId(accountId)) {
      return count;
    }
    Set<Integer> statusValues = Sets.newHashSet(TaskStatus.NOT_VISITED.ordinal(),
        TaskStatus.VISITED.ordinal(), TaskStatus.ACTIVE.ordinal(),
        TaskStatus.SUSPENDED.ordinal());

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TASKS, COL_TASK_ID, COL_SUMMARY, COL_DESCRIPTION, COL_OWNER,
            COL_EXECUTOR, COL_START_TIME, COL_FINISH_TIME, COL_REMINDER_TIME, COL_REMINDER_SENT)
        .addFields(TBL_REMINDER_TYPES, COL_REMINDER_HOURS,
            COL_REMINDER_MINUTES)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addField(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME, ALS_COMPANY_TYPE_NAME)
        .addFrom(TBL_TASKS)
        .addFromLeft(TBL_COMPANIES,
            sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
        .addFromLeft(TBL_COMPANY_TYPES,
            sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE))
        .addFromInner(TBL_REMINDER_TYPES,
            sys.joinTables(TBL_REMINDER_TYPES, TBL_TASKS, COL_REMINDER))
        .setWhere(SqlUtils.and(
            SqlUtils.inList(TBL_TASKS, COL_STATUS, statusValues),
            SqlUtils.more(TBL_TASKS, COL_FINISH_TIME, System.currentTimeMillis()),
            SqlUtils.equals(TBL_REMINDER_TYPES,
                COL_REMINDER_METHOD,
                ReminderMethod.EMAIL.ordinal())))
        .addOrder(TBL_TASKS, COL_TASK_ID);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return count;
    }

    for (SimpleRow row : data) {
      long reminderMillis;

      DateTime reminderTime = row.getDateTime(COL_REMINDER_TIME);
      if (reminderTime == null) {
        reminderMillis = row.getLong(COL_FINISH_TIME);

        Integer hours = row.getInt(COL_REMINDER_HOURS);
        Integer minutes = row.getInt(COL_REMINDER_MINUTES);

        if (BeeUtils.isPositive(hours) || BeeUtils.isPositive(minutes)) {
          if (BeeUtils.isPositive(hours)) {
            reminderMillis -= hours * TimeUtils.MILLIS_PER_HOUR;
          }
          if (BeeUtils.isPositive(minutes)) {
            reminderMillis -= minutes * TimeUtils.MILLIS_PER_MINUTE;
          }
        } else {
          reminderMillis -= TimeUtils.MILLIS_PER_DAY;
        }

      } else {
        reminderMillis = reminderTime.getTime();
      }

      boolean ok = System.currentTimeMillis() + timeRemaining / 2 > reminderMillis;
      if (ok) {
        Long sent = row.getLong(COL_REMINDER_SENT);
        if (sent != null) {
          ok = reminderMillis > sent + timeRemaining;
        }
      }
      if (!ok) {
        continue;
      }

      Long taskId = row.getLong(COL_TASK_ID);
      Long executor = row.getLong(COL_EXECUTOR);
      if (!DataUtils.isId(taskId) || !DataUtils.isId(executor)) {
        continue;
      }

      String recipientEmail = usr.getUserEmail(executor, false);
      if (BeeUtils.isEmpty(recipientEmail)) {
        logger.warning(label, "task", taskId, "executor", executor, "email not available");
        continue;
      }

      Dictionary constants = usr.getDictionary(executor);
      if (constants == null) {
        logger.warning(label, "task", taskId, "executor", executor, "localization not available");
        continue;
      }

      Document document =
          taskToHtml(taskId, row.getDateTime(COL_START_TIME),
              row.getDateTime(COL_FINISH_TIME), row.getValue(COL_SUMMARY),
              row.getValue(COL_DESCRIPTION), BeeUtils.joinItems(row.getValue(ALS_COMPANY_NAME), row
                  .getValue(ALS_COMPANY_TYPE_NAME)), row.getLong(COL_OWNER),
              executor, constants, usr.getDateTimeFormatInfo(executor));
      String content = document.buildLines();
      String headerCaption = BeeUtils.joinWords(constants.crmTask(), taskId,
          row.getValue(COL_SUMMARY));
      logger.info(label, taskId, "mail to", executor, recipientEmail);

      ResponseObject mailResponse = mail.sendStyledMail(accountId, recipientEmail,
          constants.crmReminderMailSubject(), content, headerCaption);

      if (mailResponse.hasErrors()) {
        logger.severe(label, "mail error - canceled");
        break;
      }

      count++;

      ResponseObject updateResponse = qs.updateDataWithResponse(new SqlUpdate(TBL_TASKS)
          .addConstant(COL_REMINDER_SENT, System.currentTimeMillis())
          .setWhere(SqlUtils.equals(TBL_TASKS, COL_TASK_ID, taskId)));

      if (updateResponse.hasErrors()) {
        updateResponse.log(logger);
        logger.severe(label, "update error - canceled");
        break;
      }
    }

    return count;
  }

  private Set<Long> spawnTasks(List<BeeColumn> rtColumns, BeeRow rtRow, JustDate date,
      Set<Long> executors, Set<Long> observers, Collection<RowChildren> relations,
      SimpleRowSet fileData, List<BeeColumn> taskColumns) {

    long rtId = rtRow.getId();

    List<BeeColumn> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    columns.add(DataUtils.getColumn(COL_SUMMARY, taskColumns));
    values.add(DataUtils.getString(rtColumns, rtRow, COL_SUMMARY));

    String description = DataUtils.getString(rtColumns, rtRow, COL_DESCRIPTION);
    if (!BeeUtils.isEmpty(description)) {
      columns.add(DataUtils.getColumn(COL_DESCRIPTION, taskColumns));
      values.add(description.trim());
    }

    Long type = DataUtils.getLong(rtColumns, rtRow, COL_TASK_TYPE);
    if (DataUtils.isId(type)) {
      columns.add(DataUtils.getColumn(COL_TASK_TYPE, taskColumns));
      values.add(type.toString());
    }

    columns.add(DataUtils.getColumn(COL_PRIORITY, taskColumns));
    values.add(DataUtils.getString(rtColumns, rtRow, COL_PRIORITY));

    Long startAt = TimeUtils.parseTime(DataUtils.getString(rtColumns, rtRow, COL_RT_START_AT));
    DateTime startTime = TimeUtils.combine(date, startAt);

    columns.add(DataUtils.getColumn(COL_START_TIME, taskColumns));
    values.add(BeeUtils.toString(startTime.getTime()));

    Integer durationDays = DataUtils.getInteger(rtColumns, rtRow, COL_RT_DURATION_DAYS);
    Long durationMillis = TimeUtils.parseTime(DataUtils.getString(rtColumns, rtRow,
        COL_RT_DURATION_TIME));

    if (!BeeUtils.isPositive(durationDays) && !BeeUtils.isPositive(durationMillis)) {
      durationDays = 1;
    }

    long finishMillis = startTime.getTime();
    if (BeeUtils.isPositive(durationDays)) {
      finishMillis += durationDays * TimeUtils.MILLIS_PER_DAY;
    }
    if (BeeUtils.isPositive(durationMillis)) {
      finishMillis += durationMillis;
    }

    columns.add(DataUtils.getColumn(COL_FINISH_TIME, taskColumns));
    values.add(BeeUtils.toString(finishMillis));

    Long owner = DataUtils.getLong(rtColumns, rtRow, COL_OWNER);
    columns.add(DataUtils.getColumn(COL_OWNER, taskColumns));
    values.add(owner.toString());

    columns.add(DataUtils.getColumn(COL_EXECUTOR, taskColumns));
    values.add(null);

    Long company = DataUtils.getLong(rtColumns, rtRow, COL_COMPANY);
    if (DataUtils.isId(company)) {
      columns.add(DataUtils.getColumn(COL_COMPANY, taskColumns));
      values.add(company.toString());
    }

    Long contact = DataUtils.getLong(rtColumns, rtRow, COL_CONTACT);
    if (DataUtils.isId(contact)) {
      columns.add(DataUtils.getColumn(COL_CONTACT, taskColumns));
      values.add(contact.toString());
    }

    Long project = DataUtils.getLong(rtColumns, rtRow, ProjectConstants.COL_PROJECT);
    if (DataUtils.isId(project)) {
      columns.add(DataUtils.getColumn(ProjectConstants.COL_PROJECT, taskColumns));
      values.add(project.toString());
    }

    Long reminder = DataUtils.getLong(rtColumns, rtRow, COL_RT_REMINDER);
    if (DataUtils.isId(reminder)) {
      columns.add(DataUtils.getColumn(COL_REMINDER, taskColumns));
      values.add(reminder.toString());
    }

    Integer remindBefore = DataUtils.getInteger(rtColumns, rtRow, COL_RT_REMIND_BEFORE);
    Long remindAt = TimeUtils.parseTime(DataUtils.getString(rtColumns, rtRow,
        COL_RT_REMIND_AT));

    if (BeeUtils.isPositive(remindBefore) || BeeUtils.isPositive(remindAt)) {
      long reminderMillis = finishMillis;

      if (BeeUtils.isPositive(remindBefore)) {
        reminderMillis -= remindBefore * TimeUtils.MILLIS_PER_DAY;
      }
      if (BeeUtils.isPositive(remindAt)) {
        reminderMillis = TimeUtils.combine(new DateTime(reminderMillis), remindAt).getTime();
        if (reminderMillis >= finishMillis) {
          reminderMillis -= TimeUtils.MILLIS_PER_DAY;
        }
      }

      columns.add(DataUtils.getColumn(COL_REMINDER_TIME, taskColumns));
      values.add(BeeUtils.toString(reminderMillis));
    }

    columns.add(DataUtils.getColumn(COL_STATUS, taskColumns));
    values.add(null);

    columns.add(DataUtils.getColumn(COL_RECURRING_TASK, taskColumns));
    values.add(BeeUtils.toString(rtId));

    Boolean privateTask = DataUtils.getBoolean(rtColumns, rtRow, COL_PRIVATE_TASK);
    if (BeeUtils.isTrue(privateTask)) {
      columns.add(DataUtils.getColumn(COL_PRIVATE_TASK, taskColumns));
      values.add(BeeUtils.toString(privateTask));
    }

    BeeRowSet taskData = new BeeRowSet(VIEW_TASKS, columns);
    BeeRow taskRow = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);

    taskRow.setProperty(PROP_EXECUTORS, DataUtils.buildIdList(executors));
    if (!observers.isEmpty()) {
      taskRow.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(observers));
    }

    if (!relations.isEmpty()) {
      taskRow.setProperty(VAR_TASK_RELATIONS, Codec.beeSerialize(relations));
    }

    ResponseObject response = createTasks(taskData, taskRow, owner);
    if (response.hasErrors() || !response.hasResponse()) {
      response.log(logger);

      Set<Long> result = new HashSet<>();
      return result;
    }

    Set<Long> tasks = DataUtils.parseIdSet(response.getResponseAsString());

    if (!DataUtils.isEmpty(fileData)) {
      for (SimpleRow fileRow : fileData) {
        for (Long taskId : tasks) {
          SqlInsert si = new SqlInsert(TBL_TASK_FILES)
              .addConstant(COL_TASK, taskId)
              .addConstant(COL_FILE, fileRow.getLong(COL_FILE))
              .addConstant(COL_CAPTION, fileRow.getLong(COL_FILE_CAPTION));

          qs.insertData(si);
        }
      }
    }

    return tasks;
  }

  private ResponseObject spawnTasks(RequestInfo reqInfo) {
    Long rtId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_RT_ID));
    if (!DataUtils.isId(rtId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_ID);
    }

    Integer dayNumber = BeeUtils.toIntOrNull(reqInfo.getParameter(VAR_RT_DAY));
    if (!BeeUtils.isPositive(dayNumber)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_DAY);
    }

    Long executor = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_EXECUTOR));

    BeeRowSet rtData = qs.getViewData(VIEW_RECURRING_TASKS, Filter.compareId(rtId));
    if (DataUtils.isEmpty(rtData)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_RECURRING_TASKS, rtId,
          "not available");
    }

    Set<Long> executors;
    if (DataUtils.isId(executor)) {
      executors = Sets.newHashSet(executor);
    } else {
      executors = getRecurringTaskExecutors(rtId);
      if (executors.isEmpty()) {
        return ResponseObject.error(reqInfo.getService(), rtId, "executors not available");
      }
    }

    Set<Long> observers = getRecurringTaskObservers(rtId);

    Collection<RowChildren> relations = getRelations(COL_RECURRING_TASK, rtId);
    SimpleRowSet fileData = getRecurringTaskFileData(rtId);

    List<BeeColumn> taskColumns = sys.getView(VIEW_TASKS).getRowSetColumns();

    Set<Long> tasks = spawnTasks(rtData.getColumns(), rtData.getRow(0), new JustDate(dayNumber),
        executors, observers, relations, fileData, taskColumns);
    if (tasks.isEmpty()) {
      return ResponseObject.emptyResponse();
    }

    mailScheduledTasks(tasks);

    BeeRowSet result = qs.getViewData(VIEW_TASKS, Filter.idIn(tasks));
    return ResponseObject.response(result);
  }

  @Schedule(minute = "0,30", hour = "*", persistent = false)
  private void taskReminderTimeout(Timer timer) {
    if (!Config.isInitialized()) {
      return;
    }
    long timeRemaining = timer.getTimeRemaining();
    logger.debug("task reminder timeout, time remainining", timeRemaining);

    int count = sendTaskReminders(timeRemaining);
    logger.debug("sent", count, "task reminders");
  }

  private Document taskToHtml(long taskId, DateTime startTime, DateTime finishTime,
      String summary, String description, String company, Long owner, Long executor,
      Dictionary constants, DateTimeFormatInfo dateTimeFormatInfo) {

    Document doc = new Document();

    doc.getHead().append(meta().encodingDeclarationUtf8());

    Div panel = div();
    doc.getBody().append(panel);

    Tbody fields = tbody().append(
        tr().append(
            td().text(constants.crmStartDate()),
            td().text(Formatter.renderDateTime(dateTimeFormatInfo, startTime))),
        tr().append(
            td().text(constants.crmFinishDate()),
            td().text(Formatter.renderDateTime(dateTimeFormatInfo, finishTime))),
        tr().append(
            td().text(constants.crmTaskSubject()), td().text(BeeUtils.trim(summary))));

    if (!BeeUtils.isEmpty(description)) {
      fields.append(tr().append(
          td().verticalAlign(VerticalAlign.TOP).text(constants.crmTaskDescription()),
          td().whiteSpace(WhiteSpace.PRE_LINE).text(BeeUtils.trim(description))));
    }

    if (!BeeUtils.isEmpty(company)) {
      fields.append(tr().append(
          td().text(constants.company()),
          td().text(company)));
    }

    if (owner != null) {
      fields.append(tr().append(
          td().text(constants.crmTaskManager()),
          td().text(usr.getUserSign(owner))));
    }

    List<Long> taskUsers = getTaskUsers(taskId);
    taskUsers.remove(owner);
    taskUsers.remove(executor);

    if (!taskUsers.isEmpty()) {
      for (int i = 0; i < taskUsers.size(); i++) {
        Td td = td();
        if (i == 0) {
          td.text(constants.crmTaskObservers());
        }

        fields.append(tr().append(td, td().text(usr.getUserSign(taskUsers.get(i)))));
      }
    }

    List<Element> cells = fields.queryTag(Tags.TD);
    for (Element cell : cells) {
      if (cell.index() == 0) {
        cell.setPaddingRight(1, CssUnit.EM);
        cell.setFontWeight(FontWeight.BOLDER);
      }
    }

    panel.append(table().append(fields));

    return doc;
  }

  private ResponseObject updateTaskData(Map<String, String> reqInfo, BeeRowSet taskData,
      BeeRow taskRow, TaskEvent event, String updatedRelations, Long currentUser,
      String eventNote, long now) {

    long taskId = taskRow.getId();
    Long finishTime = BeeUtils.toLongOrNull(reqInfo.get(VAR_TASK_FINISH_TIME));
    Long eventId = null;
    TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class, taskRow.getInteger(
        DataUtils.getColumnIndex(COL_STATUS, taskData.getColumns())));

    ResponseObject response = null;

    if (status != TaskStatus.NOT_SCHEDULED) {
      response = registerTaskEvent(taskId, currentUser, event, reqInfo, eventNote, finishTime, now);
    } else {
      response = ResponseObject.emptyResponse();
    }

    if (response.hasResponse(Long.class)) {
      eventId = (Long) response.getResponse();
    }
    if (!response.hasErrors()) {
      response = registerTaskVisit(taskId, currentUser, now);
    }

    if (!response.hasErrors() || status == TaskStatus.NOT_SCHEDULED) {
      response = commitTaskData(taskData, DataUtils.parseIdSet(reqInfo.get(VAR_TASK_USERS)),
          updatedRelations, eventId);
    }

    if (!response.hasErrors() && event == TaskEvent.FORWARD
        && !Objects.equals(currentUser, DataUtils.getLong(taskData, taskRow, COL_EXECUTOR))) {
      Long senderAccountId = mail.getSenderAccountId("forward task:");

      if (senderAccountId != null) {
        ResponseObject mailResponse = mailNewTask(senderAccountId, taskId, true, false);
        response.addMessagesFrom(mailResponse);
      }
    }

    return response;
  }

  private void updateTaskUsers(long taskId, Collection<Long> oldUsers, Collection<Long> newUsers) {
    List<Long> insert = new ArrayList<>(newUsers);
    insert.removeAll(oldUsers);

    List<Long> delete = new ArrayList<>(oldUsers);
    delete.removeAll(newUsers);

    for (Long user : insert) {
      createTaskUser(taskId, user, null);
    }

    String tblName = TBL_TASK_USERS;
    for (Long user : delete) {
      IsCondition condition = SqlUtils.equals(tblName, COL_TASK, taskId, COL_USER, user);
      qs.updateData(new SqlDelete(tblName).setWhere(condition));
    }
  }

  private void sendTasksReminder(Long reminderId) {
    if (!DataUtils.isId(reminderId)) {
      return;
    }

    Long accountId = mail.getSenderAccountId(TIMER_REMIND_USER_TASKS);
    if (!DataUtils.isId(accountId)) {
      return;
    }

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(VIEW_USER_REMINDERS, COL_USER_REMINDER_USER)
        .addFields(TBL_TASKS, COL_TASK_ID, COL_SUMMARY, COL_DESCRIPTION, COL_OWNER,
            COL_EXECUTOR, COL_START_TIME, COL_FINISH_TIME)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addField(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME, ALS_COMPANY_TYPE_NAME)
        .addFrom(VIEW_USER_REMINDERS)
        .addFromInner(TBL_TASKS, sys.joinTables(TBL_TASKS, VIEW_USER_REMINDERS,
            COL_USER_REMINDER_OBJECT))
        .addFromLeft(TBL_COMPANIES,
            sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
        .addFromLeft(TBL_COMPANY_TYPES,
            sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE))
        .setWhere(SqlUtils.and(
            SqlUtils.equals(VIEW_USER_REMINDERS, sys.getIdName(VIEW_USER_REMINDERS), reminderId))));

    if (DataUtils.isEmpty(data)) {
      return;
    }

    SimpleRow row = data.getRow(0);
    Long recipientId = row.getLong(COL_USER_REMINDER_USER);
    Long executor = row.getLong(COL_EXECUTOR);
    if (recipientId != null) {
      String recipientEmail = usr.getUserEmail(recipientId, false);
      if (BeeUtils.isEmpty(recipientEmail)) {
        logger.warning(TIMER_REMIND_USER_TASKS, "task reminder id",
            reminderId, "user ", recipientId, "email not available");
        return;
      }

      Dictionary constants = usr.getDictionary(executor);
      if (constants == null) {
        logger.warning(TIMER_REMIND_USER_TASKS, "task reminder id",
            reminderId, "executor", executor, "localization not available");
        return;
      }

      Document document =
          taskToHtml(row.getLong(COL_TASK_ID), row.getDateTime(COL_START_TIME),
              row.getDateTime(COL_FINISH_TIME), row.getValue(COL_SUMMARY),
              row.getValue(COL_DESCRIPTION), BeeUtils.joinItems(row.getValue(ALS_COMPANY_NAME), row
                  .getValue(ALS_COMPANY_TYPE_NAME)), row.getLong(COL_OWNER),
              row.getLong(COL_EXECUTOR), constants, usr.getDateTimeFormatInfo(executor));
      String content = document.buildLines();

      logger.info(TIMER_REMIND_USER_TASKS, "task reminder id",
          reminderId, "mail to", recipientId, recipientEmail);

      ResponseObject mailResponse = mail.sendMail(accountId, recipientEmail,
          constants.crmReminderMailSubject(), content);

      if (mailResponse.hasErrors()) {
        logger.severe(TIMER_REMIND_USER_TASKS, "mail error - canceled");
      }
    }
  }

  private void sendTasksSummaryReminder(Long userID) {
    if (!DataUtils.isId(userID)) {
      return;
    }

    SqlSelect query =
        new SqlSelect()
            .addFields(TBL_TASKS, COL_TASK_ID, COL_SUMMARY, COL_STATUS)
            .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
            .addFields(TBL_TASKS, COL_START_TIME, COL_FINISH_TIME)
            .addEmptyField(COL_DELAYED_HOURS, SqlConstants.SqlDataType.LONG, 0, 0, false)
            .addFrom(TBL_TASKS)
            .addFromLeft(TBL_COMPANIES,
                sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
            .setWhere(
                SqlUtils.and(SqlUtils.equals(TBL_TASKS, COL_EXECUTOR, userID),
                    SqlUtils.notEqual(TBL_TASKS, COL_STATUS, TaskConstants.TaskStatus.COMPLETED),
                    SqlUtils.or(
                        SqlUtils.equals(TBL_TASKS, COL_STATUS, TaskConstants.TaskStatus.ACTIVE),
                        SqlUtils.equals(TBL_TASKS, COL_STATUS, TaskConstants.TaskStatus.VISITED)),
                    SqlUtils.notNull(TBL_TASKS, COL_START_TIME),
                    SqlUtils.notNull(TBL_TASKS, COL_FINISH_TIME)))
            .addOrder(TBL_TASKS, COL_SUMMARY, COL_TASK_ID);

    SimpleRowSet data = qs.getData(query);

    if (data.getNumberOfRows() > 0) {
      createMail(userID, data);
    }
  }

  public static Map<String, String> getReminderDataLabels(Dictionary dic) {
    Map<String, String> labels = new HashMap<>();

    labels.put(COL_TASK_ID, dic.captionId());
    labels.put(COL_SUMMARY, dic.crmTaskSubject());
    labels.put(COL_COMPANY_NAME, dic.calClient());
    labels.put(COL_START_TIME, dic.crmStartDate());
    labels.put(COL_FINISH_TIME, dic.crmFinishDate());
    labels.put(COL_DELAYED_HOURS, dic.crmTaskLabelDelayedHours());

    return labels;
  }

  public static Map<String, ValueType> getReminderDataTypes() {
    Map<String, ValueType> dataTypes = new HashMap<>();

    dataTypes.put(COL_TASK_ID, ValueType.LONG);
    dataTypes.put(COL_SUMMARY, ValueType.TEXT);
    dataTypes.put(COL_COMPANY_NAME, ValueType.TEXT);
    dataTypes.put(COL_START_TIME, ValueType.DATE_TIME);
    dataTypes.put(COL_FINISH_TIME, ValueType.DATE_TIME);
    dataTypes.put(COL_DELAYED_HOURS, ValueType.LONG);

    return dataTypes;
  }

  private void createMail(Long userId, SimpleRowSet tasks) {
    Long accountId = mail.getSenderAccountId(TIMER_REMIND_TASKS_SUMMARY);
    String to = usr.getUserEmail(userId, false);

    if (!DataUtils.isId(accountId) && BeeUtils.isEmpty(to)) {
      return;
    }

    DateTimeFormatInfo dateTimeFormatInfo = usr.getDateTimeFormatInfo();
    String dateNow = DateTimeFormat.of(PredefinedFormat.DATE_SHORT, dateTimeFormatInfo)
        .format(TimeUtils.today());
    String subject =
        BeeUtils.joinWords(usr.getDictionary(userId).crmMailTasksSummarySubject(), dateNow);

    int scheduledCount = 0;
    int activeCount = 0;
    int lateCount = 0;
    DateTime now = new DateTime();
    long nowTime = now.getTime();

    String[] columns = tasks.getColumnNames();
    SimpleRowSet lateTasks = new SimpleRowSet(columns);

    for (SimpleRowSet.SimpleRow row : tasks) {
      TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class,
          row.getInt(tasks.getColumnIndex(COL_STATUS)));
      if (status != null) {
        if (status == TaskStatus.VISITED) {
          scheduledCount++;
        } else if (status == TaskStatus.ACTIVE) {
          activeCount++;
        }
        long finishTime = BeeUtils.unbox(row.getLong(tasks.getColumnIndex(COL_FINISH_TIME)));
        if ((finishTime - nowTime) < 0) {
          lateCount++;
          long diff = (nowTime - finishTime) / TimeUtils.MILLIS_PER_HOUR;
          row.setValue(COL_DELAYED_HOURS, BeeUtils.toString(diff));
          lateTasks.addRow(row.getValues());
        }
      }
    }
    Dictionary dic = usr.getDictionary(userId);
    String reminderText = dic.crmMailTasksSummaryText();
    reminderText += BeeUtils.joinWords("<br />", dic.crmTaskLabelScheduled(), scheduledCount);
    reminderText += BeeUtils.joinWords("<br />", dic.crmTaskStatusActive(), activeCount);
    reminderText += BeeUtils.joinWords("<br />", dic.crmTaskLabelLate(), lateCount);

    Map<String, String> labels = getReminderDataLabels(dic);
    Map<String, ValueType> format = getReminderDataTypes();

    List<String> excludedColumns = new ArrayList<>();
    excludedColumns.add(COL_STATUS);
    Document doc = cls.createRemindTemplate(lateTasks, labels, format, null,
        excludedColumns, BeeConst.STRING_EMPTY, reminderText, userId);

    mail.sendMail(accountId, to, subject, doc.buildLines());
    logger.info(TIMER_REMIND_COMPANY_ACTIONS, "mail send, user id", userId,
        ",reminded tasks count", tasks.getRows().size());

  }
}
