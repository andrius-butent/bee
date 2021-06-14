package com.butent.bee.server.modules.documents;

import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.communication.ChatBean;
import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.classifiers.TimerBuilder;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.modules.tasks.TasksModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Singleton
@Lock(LockType.READ)
@LocalBean
public class DocumentsModuleBean extends TimerBuilder implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(DocumentsModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  DataEditorBean deb;
  @EJB
  NewsBean news;
  @EJB
  MailModuleBean mail;
  @EJB
  ChatBean chat;
  @EJB
  ParamHolderBean prm;
  @EJB
  TasksModuleBean tmb;

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    result.addAll(qs.getSearchResults(VIEW_DOCUMENTS,
        Filter.anyContains(Sets.newHashSet(COL_DOCUMENT_NUMBER, COL_REGISTRATION_NUMBER,
            COL_DOCUMENT_NAME, ALS_CATEGORY_NAME, ALS_TYPE_NAME,
            ALS_PLACE_NAME, ALS_STATUS_NAME, ALS_DOCUMENT_COMPANY_NAME), query)));

    result.addAll(qs.getSearchResults(VIEW_DOCUMENT_FILES,
        Filter.anyContains(Sets.newHashSet(AdministrationConstants.ALS_FILE_NAME,
            AdministrationConstants.COL_FILE_CAPTION, COL_FILE_DESCRIPTION, COL_FILE_COMMENT),
            query)));

    return result;

  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (BeeUtils.same(svc, SVC_COPY_DOCUMENT_DATA)) {
      response = copyDocumentData(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DOCUMENT_DATA)));

    } else if (BeeUtils.same(svc, SVC_SET_CATEGORY_STATE)) {
      response = setCategoryState(BeeUtils.toLongOrNull(reqInfo.getParameter("id")),
          BeeUtils.toLongOrNull(reqInfo.getParameter(AdministrationConstants.COL_ROLE)),
          EnumUtils.getEnumByIndex(RightsState.class,
              reqInfo.getParameter(AdministrationConstants.COL_STATE)),
          Codec.unpack(reqInfo.getParameter("on")));

    } else {
      String msg = BeeUtils.joinWords(getModule().getName(), "service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Arrays.asList(BeeParameter.createBoolean(module, PRM_PRINT_AS_PDF, true, null),
        BeeParameter.createText(module, PRM_PRINT_SIZE, true, "A4 portrait"),
        BeeParameter.createRelation(module, PRM_PRINT_HEADER, true, TBL_EDITOR_TEMPLATES,
            COL_EDITOR_TEMPLATE_NAME),
        BeeParameter.createRelation(module, PRM_PRINT_FOOTER, true, TBL_EDITOR_TEMPLATES,
            COL_EDITOR_TEMPLATE_NAME),
        BeeParameter.createText(module, PRM_PRINT_MARGINS, true, "2em 1em"));
  }

  @Override
  public Module getModule() {
    return Module.DOCUMENTS;
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
      public void fillDocumentNumber(ViewInsertEvent event) {
        if (event.isBefore(TBL_DOCUMENTS)) {
          List<BeeColumn> cols = event.getColumns();

          if (DataUtils.contains(cols, COL_DOCUMENT_NUMBER)
              || !DataUtils.contains(cols, COL_DOCUMENT_CATEGORY)) {
            return;
          }
          IsRow row = event.getRow();
          HasConditions or = SqlUtils.or(SqlUtils.isNull(TBL_TREE_PREFIXES, COL_DOCUMENT_TYPE));

          SqlSelect query = new SqlSelect()
              .addFields(TBL_TREE_PREFIXES, COL_NUMBER_PREFIX)
              .addFrom(TBL_TREE_PREFIXES)
              .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TREE_PREFIXES, COL_DOCUMENT_CATEGORY,
                  row.getLong(DataUtils.getColumnIndex(COL_DOCUMENT_CATEGORY, cols))), or))
              .addOrder(TBL_TREE_PREFIXES, COL_DOCUMENT_TYPE);

          Long type;
          int typeIdx = DataUtils.getColumnIndex(COL_DOCUMENT_TYPE, cols);

          if (!BeeConst.isUndef(typeIdx)) {
            type = row.getLong(typeIdx);

            if (DataUtils.isId(type)) {
              or.add(SqlUtils.equals(TBL_TREE_PREFIXES, COL_DOCUMENT_TYPE, type));
            }
          }
          SimpleRowSet rs = qs.getData(query);
          String prefix = DataUtils.isEmpty(rs) ? null : rs.getValue(0, COL_NUMBER_PREFIX);

          if (!BeeUtils.isEmpty(prefix)) {
            JustDate date = TimeUtils.today();
            cols.add(new BeeColumn(COL_DOCUMENT_NUMBER));
            row.addValue(Value.getValue(qs.getNextNumber(event.getTargetName(),
                COL_DOCUMENT_NUMBER,
                prefix.replace("{year}", TimeUtils.yearToString(date.getYear()))
                    .replace("{month}", TimeUtils.monthToString(date.getMonth()))
                    .replace("{day}", TimeUtils.dayOfMonthToString(date.getDom())), null)));
          }
        }
      }

      /**
       * Fills sent/received document numbers.
       *
       * Data modify handler checks document sent/received date field changes. If sent/received date
       * was modified then sent/received number field will be changed. New sent/received number
       * value is latest numbers of documents max value. Changes of sent/received number not
       * applying when sent/received date field is empty or sent/received number has own input
       * value.
       *
       * @param event listener of Data modify handler
       */
      @Subscribe
      @AllowConcurrentEvents
      public void fillDocumentSentReceivedNumber(DataEvent.ViewModifyEvent event) {
        if (event.isBefore(TBL_DOCUMENTS)) {
          final IsRow row;
          final List<BeeColumn> columns;

          if (event instanceof ViewInsertEvent) {
            row = ((ViewInsertEvent) event).getRow();
            columns = ((ViewInsertEvent) event).getColumns();

          } else if (event instanceof DataEvent.ViewUpdateEvent) {
            row = ((DataEvent.ViewUpdateEvent) event).getRow();
            columns = ((DataEvent.ViewUpdateEvent) event).getColumns();

          } else {
            return;
          }
          for (Pair<String, String> pair : Arrays.asList(Pair.of(COL_DOCUMENT_SENT_NUMBER,
              COL_DOCUMENT_SENT), Pair.of(COL_DOCUMENT_RECEIVED_NUMBER, COL_DOCUMENT_RECEIVED))) {

            String number = pair.getA();
            String date = pair.getB();

            if (!DataUtils.contains(columns, number) && DataUtils.contains(columns, date)) {
              int idxDate = DataUtils.getColumnIndex(date, columns);

              if (!BeeUtils.isEmpty(row.getString(idxDate))) {
                /** Write values in derived references of instances */
                columns.add(sys.getView(TBL_DOCUMENTS).getBeeColumn(number));
                row.addValue(new TextValue(qs.getNextNumber(event.getTargetName(), number, null,
                    null)));
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_DOCUMENT_TEMPLATES)) {
          Map<Long, IsRow> indexedRows = new HashMap<>();
          BeeRowSet rowSet = event.getRowset();
          int idx = rowSet.getColumnIndex(COL_DOCUMENT_DATA);

          for (BeeRow row : rowSet.getRows()) {
            Long id = row.getLong(idx);

            if (DataUtils.isId(id)) {
              indexedRows.put(id, row);
            }
          }
          if (!indexedRows.isEmpty()) {
            BeeView view = sys.getView(VIEW_DATA_CRITERIA);
            SqlSelect query = view.getQuery(usr.getCurrentUserId());

            query.setWhere(SqlUtils.and(query.getWhere(),
                SqlUtils.isNull(view.getSourceAlias(), COL_CRITERIA_GROUP_NAME),
                SqlUtils.inList(view.getSourceAlias(), COL_DOCUMENT_DATA, indexedRows.keySet())));

            for (SimpleRow row : qs.getData(query)) {
              IsRow r = indexedRows.get(row.getLong(COL_DOCUMENT_DATA));

              if (r != null) {
                r.setProperty(COL_CRITERION_NAME + row.getValue(COL_CRITERION_NAME),
                    row.getValue(COL_CRITERION_VALUE));
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setRightsProperties(ViewQueryEvent event) {
        if (event.isAfter(TBL_DOCUMENT_TREE)
            && usr.isWidgetVisible(RegulatedWidget.DOCUMENT_TREE)) {

          String tableName = event.getTargetName();
          String idName = sys.getIdName(tableName);

          SqlSelect query = event.getQuery().resetFields().addFields(tableName, idName);

          BeeTable table = sys.getTable(tableName);
          Map<RightsState, String> states = new LinkedHashMap<>();
          boolean stateExists = false;
          Map<String, Long> roles = new TreeMap<>();
          roles.put("", 0L);

          for (Long role : usr.getRoles()) {
            roles.put(usr.getRoleName(role), role);
          }
          for (RightsState state : table.getStates()) {
            states.put(state, table.joinState(query, tableName, state));

            if (!BeeUtils.isEmpty(states.get(state))) {
              for (Long role : roles.values()) {
                IsExpression xpr = SqlUtils.sqlIf(table.checkState(states.get(state), state, role),
                    true, false);

                if (!BeeUtils.isEmpty(query.getGroupBy())) {
                  query.addMax(xpr, state.name() + role);
                } else {
                  query.addExpr(xpr, state.name() + role);
                }
              }
              stateExists = true;
            }
          }
          SimpleRowSet rs = null;

          if (stateExists) {
            rs = qs.getData(query);
          }
          for (BeeRow row : event.getRowset()) {
            for (RightsState state : states.keySet()) {
              for (Long role : roles.values()) {
                String value;

                if (!BeeUtils.isEmpty(states.get(state))) {
                  value = rs.getValueByKey(idName, BeeUtils.toString(row.getId()),
                      state.name() + role);
                } else {
                  value = Codec.pack(state.isChecked());
                }
                row.setProperty(BeeUtils.join("_", role, state.ordinal()), value);
              }
            }
          }
          event.getRowset().setTableProperty(AdministrationConstants.TBL_ROLES,
              Codec.beeSerialize(roles));
          event.getRowset().setTableProperty(AdministrationConstants.TBL_RIGHTS,
              Codec.beeSerialize(states.keySet()));
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateTimers(DataEvent.ViewModifyEvent event) {
        if (event.isAfter(TBL_DOCUMENTS)) {
          if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_DOCUMENT_EXPIRES)) {
              createUpdateDocumentReminder(ev.getRow().getId(), ev.getRow().getDateTime(
                  DataUtils.getColumnIndex(COL_DOCUMENT_EXPIRES, ev.getColumns())), true);

              createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, TBL_DOCUMENTS, ev.getRow().getId());
            }
          } else if (event instanceof DataEvent.ViewDeleteEvent) {
            for (long id : ((DataEvent.ViewDeleteEvent) event).getIds()) {
              deleteDocumentReminder(id);

              createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, TBL_DOCUMENTS, id);
            }
          } else if (event instanceof DataEvent.ViewInsertEvent) {
            DataEvent.ViewInsertEvent ev = (DataEvent.ViewInsertEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_DOCUMENT_EXPIRES)) {
              createUpdateDocumentReminder(ev.getRow().getId(), ev.getRow().getDateTime(
                  DataUtils.getColumnIndex(COL_DOCUMENT_EXPIRES, ev.getColumns())), false);

              createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, TBL_DOCUMENTS,
                  ((DataEvent.ViewInsertEvent) event).getRow().getId());
            }
          }
        } else if (event.isAfter(VIEW_DOCUMENT_REMINDERS)) {
          if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_DOCUMENT_REMINDER_DATE)
                || DataUtils.contains(ev.getColumns(), COL_DOCUMENT_REMINDER_ACTIVE)) {
              createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, VIEW_DOCUMENT_REMINDERS,
                  ev.getRow().getId());
            }
          } else if (event instanceof DataEvent.ViewInsertEvent) {
            createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, VIEW_DOCUMENT_REMINDERS,
                ((DataEvent.ViewInsertEvent) event).getRow().getId());
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.DOCUMENTS, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return visibility(NewsHelper.getAccessQuery(feed.getUsageTable(), relationColumn, null,
            null, userId));
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return visibility(NewsHelper.getUpdatesQuery(feed.getUsageTable(), relationColumn, null,
            null, userId, startDate));
      }

      private SqlSelect visibility(SqlSelect query) {
        if (!Invocation.locateRemoteBean(UserServiceBean.class).isAdministrator()) {
          SystemBean sysBean = Invocation.locateRemoteBean(SystemBean.class);

          query.addFromInner(TBL_DOCUMENTS,
              Invocation.locateRemoteBean(NewsBean.class).joinUsage(TBL_DOCUMENTS))
              .addFromInner(TBL_DOCUMENT_TREE,
                  sysBean.joinTables(TBL_DOCUMENT_TREE, TBL_DOCUMENTS, COL_DOCUMENT_CATEGORY));

          sysBean.filterVisibleState(query, TBL_DOCUMENT_TREE, null);
        }
        return query;
      }
    });

    buildTimers(TIMER_REMIND_DOCUMENT_END);

  }

  private DateTime calculateTimerTime(boolean userDate, DateTime dateTime) {
    Integer percentExpired = prm.getInteger(TaskConstants.PRM_SUMMARY_EXPIRED_TASK_PERCENT);
    DateTime timerTime = null;

    if (dateTime != null) {
      if (userDate) {
        timerTime = dateTime;
      } else if (BeeUtils.isPositive(percentExpired)) {
        long now = System.currentTimeMillis();
        long expDate = dateTime.getTime();

        if ((expDate - now) > TimeUtils.MILLIS_PER_DAY) {
          long diff = (now - expDate) * percentExpired / 100 / TimeUtils.MILLIS_PER_DAY;
          int days = BeeUtils.ceil(diff) < 0 ? Math.abs(BeeUtils.ceil(diff)) : 0;

          if (days > 0) {
            TimeUtils.addDay(dateTime, -days);
            long startHours = BeeUtils.unbox(prm.getTime(TaskConstants.PRM_START_OF_WORK_DAY));
            timerTime = new DateTime(dateTime.getDate().getTime() + startHours);
          }
        }
      } else if (TimeUtils.isMeq(TimeUtils.goMonth(dateTime, -1),
          new DateTime(System.currentTimeMillis()))) {
        timerTime = TimeUtils.goMonth(dateTime, -1);
      } else {
        TimeUtils.addDay(dateTime, -DOCUMENT_EXPIRATION_MIN_DAYS);
        timerTime = dateTime;
      }
    }

    if (timerTime != null && timerTime.getTime() < System.currentTimeMillis()) {
      timerTime = null;
    }

    return timerTime;
  }

  private void createUpdateDocumentReminder(long documentId, DateTime expDate, boolean update) {
    SqlSelect select = new SqlSelect()
        .addFields(VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT_REMINDER_USER_DATE)
        .addFrom(VIEW_DOCUMENT_REMINDERS)
        .setWhere(SqlUtils.and(SqlUtils.equals(VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT, documentId),
            SqlUtils.equals(VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT_REMINDER_ACTIVE, true)));

    SimpleRowSet data = qs.getData(select);

    boolean emptyReminder = data.isEmpty();
    boolean userDate = BeeUtils.unbox(qs.getBoolean(select));

    DateTime timerTime = calculateTimerTime(userDate, expDate);

    if (!userDate) {
      if (timerTime != null) {
        if (update && !emptyReminder) {
          SqlUpdate updateSql = new SqlUpdate(VIEW_DOCUMENT_REMINDERS)
              .addConstant(COL_DOCUMENT_REMINDER_DATE, timerTime)
              .setWhere(SqlUtils.equals(VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT, documentId));

          qs.updateData(updateSql);
        } else {
          SqlInsert insert = new SqlInsert(VIEW_DOCUMENT_REMINDERS)
              .addConstant(COL_DOCUMENT_REMINDER_USER, usr.getCurrentUserId())
              .addConstant(COL_DOCUMENT, documentId)
              .addConstant(COL_DOCUMENT_REMINDER_DATE, timerTime)
              .addConstant(COL_DOCUMENT_REMINDER_ACTIVE, 1);

          qs.insertData(insert);
        }
      } else if (update) {
        deleteDocumentReminder(documentId);
      }
    }
  }

  private void deleteDocumentReminder(long documentId) {
    SqlDelete delete = new SqlDelete(VIEW_DOCUMENT_REMINDERS)
        .setWhere(SqlUtils.equals(VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT, documentId));

    qs.updateData(delete);
  }

  @Override
  public void onTimeout(String timerInfo) {
    if (BeeUtils.isPrefix(timerInfo, TIMER_REMIND_DOCUMENT_END)) {
      logger.info("expired document reminder timeout", timerInfo);
      Long documentId =
          BeeUtils.toLong(BeeUtils.removePrefix(timerInfo, TIMER_REMIND_DOCUMENT_END));

      if (DataUtils.isId(documentId)
          && DataUtils.isId(mail.getSenderAccountId(TIMER_REMIND_DOCUMENT_END))) {

        SqlSelect select = new SqlSelect()
            .addFields(VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT_REMINDER_ISTASK,
                COL_DOCUMENT_REMINDER_USER, COL_DOCUMENT_REMINDER_EXECUTORS,
                COL_DOCUMENT_REMINDER_TASK_TEMPLATE, COL_DOCUMENT_REMINDER_DATE, COL_DOCUMENT)
            .addFields(TBL_DOCUMENTS, COL_DOCUMENT_COMPANY, COL_DOCUMENT_NAME,
                DocumentConstants.COL_DESCRIPTION, COL_DOCUMENT_EXPIRES)
            .addField(TBL_DOCUMENTS, COL_USER, COL_DOCUMENT + COL_USER)
            .addFrom(VIEW_DOCUMENT_REMINDERS)
            .addFromLeft(TBL_DOCUMENTS, sys.joinTables(TBL_DOCUMENTS, VIEW_DOCUMENT_REMINDERS,
                COL_DOCUMENT))
            .setWhere(SqlUtils.and(SqlUtils.equals(VIEW_DOCUMENT_REMINDERS,
                COL_DOCUMENT, documentId), SqlUtils.equals(VIEW_DOCUMENT_REMINDERS,
                COL_DOCUMENT_REMINDER_ACTIVE, true)));

        SimpleRowSet data = qs.getData(select);

        if (!data.isEmpty()) {
          boolean isTask = BeeUtils.unbox(data.getRow(0).getBoolean(COL_DOCUMENT_REMINDER_ISTASK));

          if (isTask) {
            createReminderTasks(data.getRow(0));
          } else {
            Long reminderUser = data.getRow(0).getLong(COL_DOCUMENT_REMINDER_USER);
            sendExpiredDocumentsReminders(documentId, reminderUser);
          }

          deleteDocumentReminder(documentId);
        }
      }
    }
  }

  private void createReminderTasks(SimpleRow row) {
    Set<Long> allExecutors = new HashSet<>();

    List<BeeColumn> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    List<BeeColumn> taskColumns = sys.getView(VIEW_TASKS).getRowSetColumns();

    Long templateId = row.getLong(COL_DOCUMENT_REMINDER_TASK_TEMPLATE);
    List<Long> executors = Codec.deserializeIdList(row.getValue(COL_DOCUMENT_REMINDER_EXECUTORS));

    if (DataUtils.isId(templateId)) {
      JustDate now = TimeUtils.today();

      Filter filter = Filter.and(Filter.or(Filter.isNull(COL_START_TIME),
          Filter.compareWithValue(COL_START_TIME, Operator.LE, new DateValue(now))),
          Filter.or(Filter.isNull(COL_FINISH_TIME), Filter.compareWithValue(COL_FINISH_TIME,
              Operator.GE, new DateValue(now))));

      List<String> requiredColumns = Arrays.asList(COL_TASK_TYPE, COL_EXPECTED_DURATION,
          COL_EXPECTED_EXPENSES, COL_CURRENCY, COL_COMPANY, COL_CONTACT, COL_PRODUCT,
          COL_PRIVATE_TASK, COL_END_RESULT);

      BeeRowSet ttData = qs.getViewData(VIEW_TASK_TEMPLATES,  Filter.and(filter,
          Filter.compareId(templateId)), null, requiredColumns);

      if (DataUtils.isEmpty(ttData)) {
        return;
      }

      SqlSelect selextExecutors = new SqlSelect()
          .addFields(VIEW_TT_EXECUTORS, COL_USER)
          .addFrom(VIEW_TT_EXECUTORS)
          .setWhere(SqlUtils.equals(VIEW_TT_EXECUTORS, COL_DOCUMENT_REMINDER_TASK_TEMPLATE,
              templateId));

      Set<Long> ttExecutors = qs.getLongSet(selextExecutors);

      allExecutors.addAll(ttExecutors);

      for (int i = 0; i < requiredColumns.size(); i++) {
        String colName = requiredColumns.get(i);
        String value = ttData.getRow(0).getString(i);
        int index = DataUtils.getColumnIndex(colName, taskColumns);

        if (!BeeUtils.isEmpty(value) && index >= 0) {
          columns.add(DataUtils.getColumn(colName, taskColumns));
          values.add(value);
        }
      }

      if (!BeeUtils.isEmpty(row.getValue(COL_DOCUMENT_COMPANY))) {
        int index = columns.indexOf(DataUtils.getColumn(COL_COMPANY, columns));

        if (index > 0) {
          values.set(index, row.getValue(COL_DOCUMENT_COMPANY));
        } else {
          columns.add(DataUtils.getColumn(COL_COMPANY, taskColumns));
          values.add(row.getValue(COL_DOCUMENT_COMPANY));
        }
      }
    } else {
      columns.add(DataUtils.getColumn(COL_COMPANY, taskColumns));
      values.add(row.getValue(COL_DOCUMENT_COMPANY));
    }

    allExecutors.addAll(executors);

    columns.add(DataUtils.getColumn(COL_SUMMARY, taskColumns));
    values.add(null);

    columns.add(DataUtils.getColumn(DocumentConstants.COL_DESCRIPTION, taskColumns));
    values.add(BeeUtils.join(". ", row.getValue(COL_DOCUMENT_NAME),
        row.getValue(DocumentConstants.COL_DESCRIPTION)));

    columns.add(DataUtils.getColumn(COL_EXECUTOR, taskColumns));
    values.add(null);

    columns.add(DataUtils.getColumn(COL_OWNER, taskColumns));
    values.add(null);

    columns.add(DataUtils.getColumn(COL_STATUS, taskColumns));
    values.add(null);

    columns.add(DataUtils.getColumn(COL_START_TIME, taskColumns));
    values.add(row.getValue(COL_DOCUMENT_REMINDER_DATE));

    columns.add(DataUtils.getColumn(COL_FINISH_TIME, taskColumns));
    DateTime expires = row.getDateTime(COL_DOCUMENT_EXPIRES);

    if (expires == null || TimeUtils.isMeq(row.getDateTime(COL_DOCUMENT_REMINDER_DATE), expires)) {
      DateTime finishDate = row.getDateTime(COL_DOCUMENT_REMINDER_DATE);
      TimeUtils.addDay(finishDate, 30);
      values.add(BeeUtils.toString(finishDate.getTime()));
    } else {
      values.add(BeeUtils.toString(expires.getTime()));
    }

    SqlSelect selectHeads = new SqlSelect()
        .addField(TBL_USERS, sys.getIdName(TBL_USERS), COL_EXECUTOR)
        .addField("Head" + TBL_USERS, sys.getIdName(TBL_USERS), COL_DEPARTMENT_HEAD)
        .addFrom(TBL_USERS)
        .addFromLeft(TBL_DEPARTMENT_EMPLOYEES,
            SqlUtils.joinUsing(TBL_DEPARTMENT_EMPLOYEES, TBL_USERS, COL_COMPANY_PERSON))
        .addFromLeft(TBL_DEPARTMENTS,
            sys.joinTables(TBL_DEPARTMENTS, TBL_DEPARTMENT_EMPLOYEES, COL_DEPARTMENT))
        .addFromLeft(TBL_DEPARTMENT_EMPLOYEES, "Head" + TBL_DEPARTMENT_EMPLOYEES,
            sys.joinTables(TBL_DEPARTMENT_EMPLOYEES, "Head" + TBL_DEPARTMENT_EMPLOYEES,
                TBL_DEPARTMENTS, COL_DEPARTMENT_HEAD))
        .addFromLeft(TBL_USERS, "Head" + TBL_USERS, SqlUtils.join("Head" + TBL_USERS,
            COL_COMPANY_PERSON, "Head" + TBL_DEPARTMENT_EMPLOYEES, COL_COMPANY_PERSON))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_DEPARTMENT_EMPLOYEES, COL_DEPARTMENT),
            SqlUtils.joinNotEqual(TBL_DEPARTMENT_EMPLOYEES, sys.getIdName(TBL_DEPARTMENT_EMPLOYEES),
                TBL_DEPARTMENTS, COL_DEPARTMENT_HEAD), sys.idInList(TBL_USERS, allExecutors)));

    Long autoTaskUser = prm.getRelation(PRM_AUTO_TASK_USER);
    Map<Long, Long> executorOwners = new HashMap<>();
    Map<Long, Long> validExecutors = new HashMap<>();

    for (SimpleRow simpleRow : qs.getData(selectHeads)) {
      Long executor = simpleRow.getLong(COL_EXECUTOR);
      Long departmentHead = simpleRow.getLong(COL_DEPARTMENT_HEAD);

      executorOwners.put(executor, departmentHead);
    }

    for (Long executor : allExecutors) {

      Long departmentHead = executorOwners.getOrDefault(executor, autoTaskUser);

      if (executor != null && usr.isActive(executor)) {
        if (departmentHead != null && usr.isActive(departmentHead)) {
          validExecutors.put(executor, departmentHead);
        } else if (autoTaskUser != null && usr.isActive(autoTaskUser)) {
          validExecutors.put(executor, autoTaskUser);
        }
      } else {
        if (departmentHead != null && usr.isActive(departmentHead)) {
          validExecutors.put(departmentHead, departmentHead);
        } else if (autoTaskUser != null && usr.isActive(autoTaskUser)) {
          validExecutors.put(autoTaskUser, autoTaskUser);
        }
      }
    }

    BeeRowSet taskData = new BeeRowSet(VIEW_TASKS, columns);
    BeeRow taskRow = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);

    for (Long executor : validExecutors.keySet()) {
      taskRow.setValue(taskData.getColumnIndex(COL_OWNER), validExecutors.get(executor));

      Dictionary dic = usr.getDictionary(executor);
      taskRow.setValue(taskData.getColumnIndex(COL_SUMMARY),
          dic.documentExpireReminderMailSubject());

      taskRow.setProperty(PROP_EXECUTORS, executor);

      ResponseObject response = tmb.createTasks(taskData, taskRow,  validExecutors.get(executor));

      if (!response.hasErrors() && response.hasResponse(String.class)) {
        List<Long> createdTasks = DataUtils.parseIdList(response.getResponseAsString());

        insertFiles(VIEW_TT_FILES, COL_TASK_TEMPLATE, templateId, createdTasks.get(0));

        createRelationAndComment(createdTasks.get(0), row.getLong(COL_DOCUMENT), dic,
            validExecutors.get(executor), row.getLong(COL_DOCUMENT + COL_USER), templateId);
      }
    }
  }

  private void createRelationAndComment(Long taskId, Long documentId, Dictionary dic,
      Long owner, Long user, Long templateId) {

    if (DataUtils.isId(taskId) && DataUtils.isId(documentId)) {
      SqlInsert insertDocRel = new SqlInsert(TBL_RELATIONS)
          .addConstant(COL_TASK, taskId)
          .addConstant(COL_DOCUMENT, documentId);

      if (DataUtils.isId(templateId)) {
        qs.insertData(new SqlInsert(TBL_RELATIONS)
            .addConstant(COL_TASK_TEMPLATE, templateId)
            .addConstant(COL_DOCUMENT, documentId));
      }

      SqlInsert insertComment = new SqlInsert(TBL_TASK_EVENTS)
          .addConstant(COL_TASK, taskId)
          .addConstant(COL_PUBLISHER, owner)
          .addConstant(COL_PUBLISH_TIME, TimeUtils.nowMillis())
          .addConstant(COL_COMMENT, dic.crmAutoCreatedTask())
          .addConstant(TaskConstants.COL_EVENT, TaskEvent.EDIT);

      if (DataUtils.isId(user)) {
        String name = dic.creator() + ": " + usr.getUserSign(user);
        insertComment.addConstant(COL_EVENT_NOTE, name);
      }

      qs.insertData(insertDocRel);
      qs.insertData(insertComment);

      insertFiles(VIEW_DOCUMENT_FILES, COL_DOCUMENT, documentId, taskId);
    }
  }

  private void insertFiles(String tableName, String column, Long columnId, Long taskId) {
    if (DataUtils.isId(columnId) && DataUtils.isId(taskId)) {

      SqlSelect selectFiles = new SqlSelect()
          .addFields(tableName, COL_FILE, COL_CAPTION)
          .addFrom(tableName)
          .setWhere(SqlUtils.equals(tableName, column, columnId));

      SimpleRowSet files = qs.getData(selectFiles);

      if (!files.isEmpty()) {
        for (SimpleRow file : files) {

          SqlInsert insertFiles = new SqlInsert(VIEW_TASK_FILES)
              .addConstant(COL_TASK, taskId)
              .addConstant(COL_FILE, file.getLong(COL_FILE))
              .addConstant(COL_CAPTION, file.getValue(COL_CAPTION));

          qs.insertData(insertFiles);
        }
      }
    }
  }

  @Override
  protected List<Timer> createTimers(String timerIdentifier, IsCondition wh) {
    List<Timer> timersList = new ArrayList<>();

    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_DOCUMENT_END)) {
      SimpleRowSet data = qs.getData(new SqlSelect()
          .addFields(VIEW_DOCUMENT_REMINDERS, sys.getIdName(VIEW_DOCUMENT_REMINDERS),
              COL_DOCUMENT_REMINDER_USER, COL_DOCUMENT_REMINDER_DATE, COL_DOCUMENT_REMINDER_ISTASK,
              COL_DOCUMENT, COL_DOCUMENT_REMINDER_USER_DATE)
          .addFields(TBL_DOCUMENTS, COL_DOCUMENT_EXPIRES)
          .addFrom(VIEW_DOCUMENT_REMINDERS)
          .addFromLeft(TBL_DOCUMENTS, sys.joinTables(TBL_DOCUMENTS,
              VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT))
          .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, VIEW_DOCUMENT_REMINDERS,
              COL_DOCUMENT_REMINDER_USER))
          .setWhere(SqlUtils.and(wh,
              SqlUtils.equals(VIEW_DOCUMENT_REMINDERS, COL_DOCUMENT_REMINDER_ACTIVE, true))));

      for (SimpleRowSet.SimpleRow row : data) {
        Long timerId = row.getLong(COL_DOCUMENT);

        DateTime expDate = TimeUtils.toDateTimeOrNull(row.getValue(COL_DOCUMENT_REMINDER_DATE));
        boolean userDate = BeeUtils.unbox(row.getBoolean(COL_DOCUMENT_REMINDER_USER_DATE));

        DateTime timerTime = calculateTimerTime(userDate, expDate);

        if (timerTime == null) {
          continue;
        }

        if (timerTime.getTime() > System.currentTimeMillis()) {
          Timer timer = getTimerService().createSingleActionTimer(timerTime.getJava(),
              new TimerConfig(timerIdentifier + timerId, false));

          logger.info("Created timer:", timerTime, timer.getInfo());

          if (timer != null) {
            timersList.add(timer);
          }
        }
      }
    }

    return timersList;
  }

  @Override
  protected Pair<IsCondition, List<String>> getConditionAndTimerIdForUpdate(String timerIdentifier,
      String viewName, Long relationId) {
    IsCondition wh = null;

    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_DOCUMENT_END)) {
      if (BeeUtils.same(viewName, TBL_DOCUMENTS)) {
        wh = SqlUtils.equals(TBL_DOCUMENTS, sys.getIdName(TBL_DOCUMENTS), relationId);
        List<String> timerIdentifiersIds = new ArrayList<>();
        timerIdentifiersIds.add(timerIdentifier + relationId);
        return Pair.of(wh, timerIdentifiersIds);
      } else if (BeeUtils.same(viewName, VIEW_DOCUMENT_REMINDERS)) {
        Long reminderId = relationId;
        wh = SqlUtils.equals(VIEW_DOCUMENT_REMINDERS, sys.getIdName(VIEW_DOCUMENT_REMINDERS),
            reminderId);
        List<String> timerIdentifiersIds = new ArrayList<String>();
        timerIdentifiersIds.add(timerIdentifier + reminderId);
        return Pair.of(wh, timerIdentifiersIds);
      }
    }

    return null;
  }

  private void sendExpiredDocumentsReminders(Long documentId, Long reminderUser) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_DOCUMENTS, sys.getIdName(TBL_DOCUMENTS), COL_DOCUMENT_USER,
            COL_DOCUMENT_DATE, COL_DOCUMENT_EXPIRES,
            COL_DOCUMENT_NAME, COL_DOCUMENT_NUMBER, COL_REGISTRATION_NUMBER)
        .addField(ClassifierConstants.TBL_COMPANIES,
            ClassifierConstants.COL_COMPANY_NAME, ALS_DOCUMENT_COMPANY_NAME)
        .addField(ClassifierConstants.TBL_COMPANY_TYPES,
            ClassifierConstants.COL_COMPANY_TYPE_NAME, ALS_TYPE_NAME)
        .addFrom(TBL_DOCUMENTS)
        .addFromLeft(ClassifierConstants.TBL_COMPANIES,
            sys.joinTables(ClassifierConstants.TBL_COMPANIES, TBL_DOCUMENTS, COL_DOCUMENT_COMPANY))
        .addFromLeft(ClassifierConstants.TBL_COMPANY_TYPES,
            sys.joinTables(ClassifierConstants.TBL_COMPANY_TYPES, ClassifierConstants.TBL_COMPANIES,
                ClassifierConstants.COL_COMPANY_TYPE))
        .setWhere(sys.idEquals(TBL_DOCUMENTS, documentId));

    Long senderAccountId = mail.getSenderAccountId(TIMER_REMIND_DOCUMENT_END);

    SimpleRowSet data = qs.getData(query);
    if (data.getNumberOfRows() > 0 && DataUtils.isId(senderAccountId)) {
      formAndSendExpiredDocument(data.getRow(0), senderAccountId, reminderUser);
    }
  }

  private void formAndSendExpiredDocument(SimpleRow dRow, Long senderAccountId,
      Long reminderUser) {

    Long userId = dRow.getLong(COL_DOCUMENT_USER);

    for (Long user : new Long[]{userId, reminderUser}) {
      Document doc = new Document();

      Dictionary dic = usr.getDictionary(user);
      DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo(user);

      doc.getHead().append(meta().encodingDeclarationUtf8());

      Div panel = div();
      doc.getBody().append(panel);

      Tbody fields = tbody().append(
          tr().append(
              td().text(dic.documentName()),
              td().text(dRow.getValue(COL_DOCUMENT_NAME))),
          tr().append(
              td().text(dic.company()),
              td().text(BeeUtils.joinWords(dRow.getValue(ALS_DOCUMENT_COMPANY_NAME),
                  dRow.getValue(ALS_TYPE_NAME)))),
          tr().append(
              td().text(dic.documentDate()),
              td().text(Formatter.renderDateTime(dtfInfo, dRow.getDateTime(COL_DOCUMENT_DATE)))),
          tr().append(
              td().text(dic.documentExpires()),
              td().text(Formatter.renderDateTime(dtfInfo, dRow.getDateTime(COL_DOCUMENT_EXPIRES)))),
          tr().append(
              td().text(dic.documentNumber()),
              td().text(dRow.getValue(COL_DOCUMENT_NUMBER))),
          tr().append(
              td().text(dic.documentRegistrationNumberShort()),
              td().text(dRow.getValue(COL_REGISTRATION_NUMBER))));

      List<Element> cells = fields.queryTag(Tags.TD);
      for (Element cell : cells) {
        if (cell.index() == 0) {
          cell.setPaddingRight(1, CssUnit.EM);
          cell.setFontWeight(FontWeight.BOLDER);
        }
      }

      panel.append(table().append(fields));

      String content = doc.buildLines();
      String headerCaption = BeeUtils.joinWords(dic.document(),
          dRow.getValue(COL_DOCUMENT_NAME));

      String recipientEmail;
      if (user != null && usr.isActive(user)) {
        recipientEmail = usr.getUserEmail(user, false);
      } else {
        recipientEmail = mail.getSenderAccountEmail(senderAccountId);
      }

      ResponseObject mailResponse = mail.sendStyledMail(senderAccountId, recipientEmail,
          dic.documentExpireReminderMailSubject(), content, headerCaption, null);

      if (mailResponse.hasErrors()) {
        logger.severe(TIMER_REMIND_DOCUMENT_END, "mail error - canceled");
      }

      Map<String, String> linkData = new HashMap<>();
      linkData.put(VIEW_DOCUMENTS, dRow.getValue(sys.getIdName(TBL_DOCUMENTS)));
      chat.putMessage(mail.styleMailHeader(headerCaption), user, linkData);
    }
  }

  private ResponseObject copyDocumentData(Long data) {
    Assert.state(DataUtils.isId(data));

    Long dataId = qs.insertData(new SqlInsert(TBL_DOCUMENT_DATA)
        .addConstant(COL_DOCUMENT_CONTENT, qs.getValue(new SqlSelect()
            .addFields(TBL_DOCUMENT_DATA, COL_DOCUMENT_CONTENT)
            .addFrom(TBL_DOCUMENT_DATA)
            .setWhere(sys.idEquals(TBL_DOCUMENT_DATA, data)))));

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addField(TBL_CRITERIA_GROUPS, sys.getIdName(TBL_CRITERIA_GROUPS), COL_CRITERIA_GROUP)
        .addField(TBL_CRITERIA_GROUPS, COL_CRITERIA_ORDINAL,
            COL_CRITERIA_GROUP + COL_CRITERIA_ORDINAL)
        .addFields(TBL_CRITERIA_GROUPS, COL_CRITERIA_GROUP_NAME)
        .addFields(TBL_CRITERIA, COL_CRITERIA_ORDINAL, COL_CRITERION_NAME, COL_CRITERION_VALUE)
        .addFrom(TBL_CRITERIA_GROUPS)
        .addFromLeft(TBL_CRITERIA,
            sys.joinTables(TBL_CRITERIA_GROUPS, TBL_CRITERIA, COL_CRITERIA_GROUP))
        .setWhere(SqlUtils.equals(TBL_CRITERIA_GROUPS, COL_DOCUMENT_DATA, data)));

    Map<Long, Long> groups = new HashMap<>();

    for (SimpleRow row : rs) {
      long groupId = row.getLong(COL_CRITERIA_GROUP);
      String criterion = row.getValue(COL_CRITERION_NAME);

      if (!groups.containsKey(groupId)) {
        groups.put(groupId, qs.insertData(new SqlInsert(TBL_CRITERIA_GROUPS)
            .addConstant(COL_DOCUMENT_DATA, dataId)
            .addConstant(COL_CRITERIA_ORDINAL,
                row.getValue(COL_CRITERIA_GROUP + COL_CRITERIA_ORDINAL))
            .addConstant(COL_CRITERIA_GROUP_NAME, row.getValue(COL_CRITERIA_GROUP_NAME))));
      }
      if (!BeeUtils.isEmpty(criterion)) {
        qs.insertData(new SqlInsert(TBL_CRITERIA)
            .addConstant(COL_CRITERIA_GROUP, groups.get(groupId))
            .addConstant(COL_CRITERIA_ORDINAL, row.getValue(COL_CRITERIA_ORDINAL))
            .addConstant(COL_CRITERION_NAME, criterion)
            .addConstant(COL_CRITERION_VALUE, row.getValue(COL_CRITERION_VALUE)));
      }
    }
    return ResponseObject.response(dataId);
  }

  private ResponseObject setCategoryState(Long id, Long roleId, RightsState state, boolean on) {
    Assert.noNulls(id, roleId, state);

    deb.setState(TBL_DOCUMENT_TREE, state, id, roleId, on);
    return ResponseObject.emptyResponse();
  }
}
