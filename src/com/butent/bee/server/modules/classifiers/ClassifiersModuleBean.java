package com.butent.bee.server.modules.classifiers;

import com.butent.bee.shared.data.*;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.ALS_COMPANY_TYPE_NAME;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.COL_ORDER;
import static com.butent.bee.shared.modules.orders.OrdersConstants.TBL_ORDERS;
import static com.butent.bee.shared.modules.projects.ProjectConstants.COL_DATES_START_DATE;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.COL_SERVICE_ITEM;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.COL_MODEL;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.TableModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SearchBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.AdministrationModuleBean;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.elements.Table;
import com.butent.bee.shared.html.builder.elements.Td;
import com.butent.bee.shared.html.builder.elements.Th;
import com.butent.bee.shared.html.builder.elements.Tr;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants.AppointmentStatus;
import com.butent.bee.shared.modules.calendar.CalendarHelper;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.classifiers.PriceInfo;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.LogMessage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TimerService;
import javax.imageio.ImageIO;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@Stateless
@LocalBean
public class ClassifiersModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(ClassifiersModuleBean.class);

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
  AdministrationModuleBean adm;
  @EJB
  ParamHolderBean prm;

  @EJB
  SearchBean src;

  @Resource
  SessionContext ctx;
  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> search = new ArrayList<>();

    if (usr.isModuleVisible(ModuleAndSub.of(getModule(), SubModule.CONTACTS))) {
      List<SearchResult> companiesSr = qs.getSearchResults(VIEW_COMPANIES, src.buildSearchFilter(
          VIEW_COMPANIES, Sets.newHashSet(COL_COMPANY_NAME, COL_COMPANY_CODE, COL_PHONE,
              COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(companiesSr);

      List<SearchResult> personsSr = qs.getSearchResults(VIEW_PERSONS,
          src.buildSearchFilter(VIEW_PERSONS, Sets.newHashSet(COL_FIRST_NAME, COL_LAST_NAME,
              COL_PHONE, COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(personsSr);

      List<SearchResult> companiesAndPersons = qs.getSearchResults(VIEW_COMPANY_PERSONS,
          src.buildSearchFilter(VIEW_COMPANY_PERSONS, Sets.newHashSet(ALS_COMPANY_NAME,
              ALS_COMPANY_TYPE_NAME, COL_FIRST_NAME, COL_LAST_NAME, COL_DEPARTMENT_NAME,
              ALS_POSITION_NAME, COL_PHONE, COL_MOBILE, COL_FAX, COL_EMAIL, COL_ADDRESS,
              COL_POST_INDEX, COL_WEBSITE, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(companiesAndPersons);
    }

    if (usr.isModuleVisible(ModuleAndSub.of(Module.TRADE))) {
      List<SearchResult> itemsSr =
          qs.getSearchResults(VIEW_ITEMS, Filter.anyContains(Sets.newHashSet(COL_ITEM_NAME,
              COL_ITEM_ARTICLE, COL_ITEM_BARCODE), query));
      search.addAll(itemsSr);
    }
    return search;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (BeeUtils.same(svc, SVC_COMPANY_INFO)) {
      response = getCompanyInfo(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_COMPANY)),
          reqInfo.getParameter(VAR_LOCALE));

    } else if (BeeUtils.same(svc, SVC_CREATE_COMPANY)) {
      Map<String, String> info = new HashMap<>();

      for (Map<String, String> map : Arrays.asList(reqInfo.getParams(), reqInfo.getHeaders(),
          reqInfo.getVars())) {

        if (!BeeUtils.isEmpty(map)) {
          info.putAll(map);
        }
      }
      response = createCompany(info);

    } else if (BeeUtils.same(svc, SVC_CREATE_COMPANY_PERSON)) {
      Map<String, String> info = new HashMap<>();

      for (Map<String, String> map : Arrays.asList(reqInfo.getParams(), reqInfo.getHeaders(),
          reqInfo.getVars())) {

        if (!BeeUtils.isEmpty(map)) {
          info.putAll(map);
        }
      }
      response = createCompanyPerson(info);

    } else if (BeeUtils.same(svc, SVC_GET_COMPANY_TYPE_REPORT)) {
      response = getCompanyTypeReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GENERATE_QR_CODE)) {
      try {
        response = generateQrCode(reqInfo);
      } catch (WriterException | IOException e) {
        response = ResponseObject.error(e);
        e.printStackTrace();
      }

    } else if (BeeUtils.same(svc, SVC_GET_PRICE_AND_DISCOUNT)) {
      response = getPriceAndDiscount(reqInfo);

    } else if (BeeUtils.same(svc, SVC_FILTER_ORDERS)) {
      response = filterOrders(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_RESERVATION)) {
      response = getReservation(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_STOCKS_IN_WAREHOUSE)) {
		response = getStocks(reqInfo);

    } else {
      String msg = BeeUtils.joinWords("Commons service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    if (Objects.isNull(response) || response.hasErrors()) {
      ctx.setRollbackOnly();
    }
    return response;
  }

	@Override
  public Collection<BeeParameter> getDefaultParameters() {
    return Collections.singletonList(BeeParameter.createMap(getModule().getName(),
        PRM_RECORD_DEPENDENCY, false, ImmutableMap.of(TBL_DOCUMENTS, COL_DOCUMENT_CATEGORY,
            VIEW_RELATED_DOCUMENTS, COL_DOCUMENT_CATEGORY)));
  }

  @Override
  public Module getModule() {
    return Module.CLASSIFIERS;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void setPersonCompanies(ViewQueryEvent event) {
        if (event.isAfter(VIEW_PERSONS) && event.hasData()) {
          SqlSelect query = new SqlSelect()
              .addFields(TBL_COMPANY_PERSONS, COL_PERSON, COL_COMPANY)
              .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
              .addFrom(TBL_COMPANY_PERSONS)
              .addFromInner(TBL_COMPANIES,
                  sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS, COL_COMPANY))
              .addOrder(TBL_COMPANY_PERSONS, COL_PERSON)
              .addOrder(TBL_COMPANIES, COL_COMPANY_NAME);

          if (event.getRowset().getNumberOfRows() < 100) {
            query.setWhere(SqlUtils.inList(TBL_COMPANY_PERSONS, COL_PERSON,
                event.getRowset().getRowIds()));
          }

          SimpleRowSet data = qs.getData(query);
          if (!DataUtils.isEmpty(data)) {
            Multimap<Long, Long> companyIds = ArrayListMultimap.create();
            Multimap<Long, String> companyNames = ArrayListMultimap.create();

            for (SimpleRow row : data) {
              Long person = row.getLong(COL_PERSON);

              companyIds.put(person, row.getLong(COL_COMPANY));
              companyNames.put(person, row.getValue(COL_COMPANY_NAME));
            }

            for (BeeRow row : event.getRowset()) {
              if (companyIds.containsKey(row.getId())) {
                row.setProperty(PROP_COMPANY_IDS,
                    DataUtils.buildIdList(companyIds.get(row.getId())));
                row.setProperty(PROP_COMPANY_NAMES,
                    BeeUtils.joinItems(companyNames.get(row.getId())));
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void storeEmail(TableModifyEvent event) {
        if (event.isBefore(TBL_EMAILS)
            && (event.getQuery() instanceof SqlInsert || event.getQuery() instanceof SqlUpdate)) {

          IsQuery query = event.getQuery();
          Object expr = null;

          if (query instanceof SqlInsert) {
            if (!((SqlInsert) query).isMultipleInsert()
                && ((SqlInsert) query).hasField(COL_EMAIL_ADDRESS)) {

              expr = ((SqlInsert) query).getValue(COL_EMAIL_ADDRESS).getValue();
            }
          } else if (((SqlUpdate) query).hasField(COL_EMAIL_ADDRESS)) {
            expr = ((SqlUpdate) query).getValue(COL_EMAIL_ADDRESS);

            if (expr instanceof IsExpression) {
              expr = ((IsExpression) expr).getValue();
            }
          }
          if (expr instanceof Value) {
            String email = BeeUtils.normalize(((Value) expr).getString());

            try {
              new InternetAddress(email, true).validate();

              if (query instanceof SqlInsert) {
                ((SqlInsert) query).updExpression(COL_EMAIL_ADDRESS, SqlUtils.constant(email));
              } else {
                ((SqlUpdate) query).updExpression(COL_EMAIL_ADDRESS, SqlUtils.constant(email));
              }
            } catch (AddressException ex) {
              event.addErrorMessage(BeeUtils.joinWords("Wrong address ", email, ": ",
                  ex.getMessage()));
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setItemRemainders(ViewQueryEvent event) {
        if (event.isAfter(VIEW_ITEMS) && usr.isModuleVisible(ModuleAndSub.of(Module.ORDERS))
            && DataUtils.containsNull(event.getRowset(), COL_ITEM_IS_SERVICE)) {

          Map<Long, IsRow> indexedRows = new HashMap<>();
          BeeRowSet rowSet = event.getRowset();
          List<Long> ids = rowSet.getRowIds();

          Map<Long, Double> resRemainders =
              getRemainders(ids, TBL_ORDER_ITEMS, COL_RESERVED_REMAINDER);
          Map<Long, Double> wrhRemainders =
              getRemainders(ids, VIEW_ITEM_REMAINDERS, COL_WAREHOUSE_REMAINDER);

          for (BeeRow row : rowSet.getRows()) {
            Long id = row.getId();

            if (DataUtils.isId(id)) {
              indexedRows.put(id, row);
              row.setProperty(COL_RESERVED_REMAINDER, resRemainders.get(id));
              row.setProperty(PROP_WAREHOUSE_REMAINDER, wrhRemainders.get(id));
            }
          }
          if (!indexedRows.isEmpty()) {
            BeeView view = sys.getView(VIEW_ITEM_REMAINDERS);
            SqlSelect query = view.getQuery(usr.getCurrentUserId());

            query.setWhere(SqlUtils.and(query.getWhere(), SqlUtils.inList(view.getSourceAlias(),
                COL_ITEM, indexedRows.keySet())));

            for (SimpleRow row : qs.getData(query)) {
              IsRow r = indexedRows.get(row.getLong(COL_ITEM));

              if (r != null) {
                r.setProperty(COL_WAREHOUSE_REMAINDER + row.getValue(ALS_WAREHOUSE_CODE),
                    row.getValue(COL_WAREHOUSE_REMAINDER));
              }
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.COMPANIES_MY, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        String usageTable = NewsConstants.getUsageTable(TBL_COMPANIES);
        String urc = news.getUsageRelationColumn(TBL_COMPANIES);

        List<Pair<String, IsCondition>> joins = NewsHelper.buildJoin(TBL_COMPANY_USERS,
            SqlUtils.join(TBL_COMPANY_USERS, COL_COMPANY_USER_COMPANY, usageTable, urc));

        return NewsHelper.getAccessQuery(usageTable, urc, joins, getUserCompanyCondition(userId),
            userId);
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        String usageTable = feed.getUsageTable();

        List<Pair<String, IsCondition>> joins = NewsHelper.buildJoin(usageTable,
            sys.joinTables(TBL_COMPANY_USERS, usageTable, relationColumn));

        return NewsHelper.getUpdatesQuery(TBL_COMPANY_USERS, COL_COMPANY_USER_COMPANY, usageTable,
            joins, getUserCompanyCondition(userId), userId, startDate);
      }

      private List<IsCondition> getUserCompanyCondition(long userId) {
        return Lists.newArrayList(SqlUtils.equals(TBL_COMPANY_USERS, COL_COMPANY_USER_USER,
            userId));
      }
    });

    BeeView.registerConditionProvider(FILTER_COMPANY_CREATION_AND_TYPE,
        (view, args) -> {
          Long start = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, 0));
          Long end = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, 1));

          String type = BeeUtils.getQuietly(args, 2);

          String idName = view.getSourceIdName();

          HasConditions conditions = SqlUtils.and();

          if (start != null || end != null) {
            String auditSource = sys.getAuditSource(view.getSourceName());

            IsExpression created = SqlUtils.aggregate(SqlFunction.MIN,
                SqlUtils.field(auditSource, AUDIT_FLD_TIME));

            SqlSelect auditQuery = new SqlSelect()
                .addFields(auditSource, AUDIT_FLD_ID)
                .addExpr(created, AUDIT_FLD_TIME)
                .addFrom(auditSource)
                .addGroup(auditSource, AUDIT_FLD_ID);

            HasConditions having = SqlUtils.and();
            if (start != null) {
              having.add(SqlUtils.moreEqual(created, start));
            }
            if (end != null) {
              having.add(SqlUtils.less(created, end));
            }
            auditQuery.setHaving(having);

            String auditAlias = "audit" + SqlUtils.uniqueName();

            SqlSelect subQuery = new SqlSelect()
                .addFields(auditAlias, AUDIT_FLD_ID)
                .addFrom(auditQuery, auditAlias);

            conditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));
          }

          if (!BeeUtils.isEmpty(type)) {
            if (type.equals(BeeConst.STRING_ZERO)) {
              conditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName,
                  TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY)));

            } else {
              Set<Long> types = DataUtils.parseIdSet(type);
              if (!types.isEmpty()) {
                conditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                    TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY,
                    SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE, types)));
              }
            }
          }

          return conditions;
        });

    BeeView.registerConditionProvider(FILTER_COMPANY_USAGE, (view, args) -> {
      if (view == null || BeeUtils.size(args) < 2) {
        return null;
      }

      HasConditions conditions = SqlUtils.and();

      String idName = view.getSourceIdName();

      String relation = null;
      Operator operator = null;
      Integer count = null;

      Long start = null;
      Long end = null;

      for (int i = 0; i < args.size() - 1; i += 2) {
        String key = args.get(i);
        String value = args.get(i + 1);

        if (BeeUtils.isEmpty(key) || BeeUtils.isEmpty(value)) {
          continue;
        }

        switch (key) {
          case Service.VAR_VIEW_NAME:
            relation = value;
            break;
          case Service.VAR_OPERATOR:
            operator = Operator.getOperator(value);
            break;
          case Service.VAR_VALUE:
            count = BeeUtils.toIntOrNull(value);
            break;

          case Service.VAR_FROM:
            start = BeeUtils.toLongOrNull(value);
            break;
          case Service.VAR_TO:
            end = BeeUtils.toLongOrNull(value);
            break;

          case COL_COMPANY_TYPE:
          case COL_COMPANY_GROUP:
          case COL_COMPANY_PRIORITY:
          case COL_COMPANY_RELATION_TYPE_STATE:
          case COL_COMPANY_FINANCIAL_STATE:
          case COL_COMPANY_SIZE:
          case COL_COMPANY_INFORMATION_SOURCE:
            conditions.add(SqlUtils.inList(TBL_COMPANIES, key, DataUtils.parseIdSet(value)));
            break;

          case COL_RELATION_TYPE:
            conditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY,
                SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE, key,
                    DataUtils.parseIdSet(value))));
            break;

          case COL_ACTIVITY:
            conditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                TBL_COMPANY_ACTIVITY_STORE, COL_COMPANY,
                SqlUtils.inList(TBL_COMPANY_ACTIVITY_STORE, key, DataUtils.parseIdSet(value))));
            break;

          case COL_COUNTRY:
          case COL_CITY:
            conditions.add(SqlUtils.inList(view.getColumnSource(key), key,
                DataUtils.parseIdSet(value)));
            break;
        }
      }

      if (sys.isView(relation) && operator != null) {
        BeeView relView = sys.getView(relation);

        String source = relView.getSourceName();
        String sourceField = null;
        String dtField = null;

        String relationsField = null;

        switch (relation) {
          case VIEW_SALES:
            sourceField = COL_TRADE_CUSTOMER;
            dtField = COL_TRADE_DATE;
            break;

          case VIEW_PURCHASES:
            sourceField = COL_TRADE_SUPPLIER;
            dtField = COL_TRADE_DATE;
            break;

          case DocumentConstants.VIEW_DOCUMENTS:
            dtField = DocumentConstants.COL_DOCUMENT_DATE;
            relationsField = DocumentConstants.COL_DOCUMENT;
            break;

          case TaskConstants.VIEW_TASKS:
            sourceField = COL_COMPANY;
            dtField = TaskConstants.COL_FINISH_TIME;
            relationsField = TaskConstants.COL_TASK;
            break;

          case TaskConstants.VIEW_RECURRING_TASKS:
            sourceField = COL_COMPANY;
            dtField = TaskConstants.COL_RT_SCHEDULE_FROM;
            relationsField = TaskConstants.COL_RECURRING_TASK;
            break;

          case CalendarConstants.VIEW_APPOINTMENTS:
            sourceField = COL_COMPANY;
            dtField = CalendarConstants.COL_START_DATE_TIME;
            break;

          case ServiceConstants.VIEW_SERVICE_OBJECTS:
            sourceField = ServiceConstants.COL_SERVICE_CUSTOMER;
            dtField = relView.getSourceVersionName();
            break;

          case TransportConstants.VIEW_ORDERS:
            sourceField = TransportConstants.COL_CUSTOMER;
            dtField = TransportConstants.COL_ORDER_DATE;
            break;

          case TransportConstants.VIEW_VEHICLES:
            sourceField = TransportConstants.COL_OWNER;
            dtField = relView.getSourceVersionName();
            break;

          case VIEW_COMPANY_CONTACTS:
          case VIEW_COMPANY_DEPARTMENTS:
          case VIEW_COMPANY_PERSONS:
          case VIEW_COMPANY_USERS:
          case VIEW_COMPANY_RELATION_TYPE_STORE:
          case VIEW_COMPANY_ACTIVITY_STORE:
            sourceField = COL_COMPANY;
            dtField = relView.getSourceVersionName();
            break;
        }

        if (BeeUtils.allEmpty(sourceField, relationsField)) {
          logger.severe("filter", FILTER_COMPANY_USAGE, "relation", relation, "not supported");

        } else if (!BeeUtils.isPositive(count) && operator == Operator.LT) {
          conditions.add(SqlUtils.sqlFalse());

        } else if (BeeUtils.isPositive(count) || operator != Operator.GE) {
          if (!BeeUtils.isPositive(count)) {
            count = 0;
          }

          HasConditions dtWhere;
          if (!BeeUtils.isEmpty(dtField) && (start != null || end != null)) {
            dtWhere = SqlUtils.and();
            if (start != null) {
              dtWhere.add(SqlUtils.moreEqual(source, dtField, start));
            }
            if (end != null) {
              dtWhere.add(SqlUtils.less(source, dtField, end));
            }
          } else {
            dtWhere = null;
          }

          HasConditions sourceWhere = BeeUtils.isEmpty(sourceField)
              ? null : SqlUtils.and(SqlUtils.notNull(source, sourceField), dtWhere);
          HasConditions relationsWhere = BeeUtils.isEmpty(relationsField)
              ? null : SqlUtils.and(SqlUtils.notNull(TBL_RELATIONS, COL_COMPANY), dtWhere);

          HasConditions relConditions = SqlUtils.or();

          SqlSelect subQuery;

          if (operator == Operator.LT && count > 0
              || operator == Operator.LE
              || operator == Operator.EQ && count == 0) {

            if (BeeUtils.isEmpty(relationsField)) {
              relConditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName,
                  source, sourceField, sourceWhere)));

            } else if (BeeUtils.isEmpty(sourceField)) {
              subQuery = new SqlSelect().setDistinctMode(true)
                  .addFields(TBL_RELATIONS, COL_COMPANY)
                  .addFrom(TBL_RELATIONS)
                  .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                  .setWhere(relationsWhere);

              relConditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName, subQuery)));

            } else {
              subQuery = new SqlSelect()
                  .addField(source, sourceField, COL_COMPANY)
                  .addFrom(source)
                  .setWhere(sourceWhere);

              subQuery.addUnion(new SqlSelect()
                  .addFields(TBL_RELATIONS, COL_COMPANY)
                  .addFrom(TBL_RELATIONS)
                  .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                  .setWhere(relationsWhere));

              relConditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName, subQuery)));
            }
          }

          if (operator == Operator.GE && count == 1
              || operator == Operator.GT && count == 0) {

            if (BeeUtils.isEmpty(relationsField)) {
              relConditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                  source, sourceField, sourceWhere));

            } else if (BeeUtils.isEmpty(sourceField)) {
              subQuery = new SqlSelect().setDistinctMode(true)
                  .addFields(TBL_RELATIONS, COL_COMPANY)
                  .addFrom(TBL_RELATIONS)
                  .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                  .setWhere(relationsWhere);

              relConditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));

            } else {
              subQuery = new SqlSelect()
                  .addField(source, sourceField, COL_COMPANY)
                  .addFrom(source)
                  .setWhere(sourceWhere);

              subQuery.addUnion(new SqlSelect()
                  .addFields(TBL_RELATIONS, COL_COMPANY)
                  .addFrom(TBL_RELATIONS)
                  .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                  .setWhere(relationsWhere));

              relConditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));
            }
          }

          if (operator == Operator.LT && count > 1
              || operator == Operator.LE && count > 0
              || operator == Operator.EQ && count > 0
              || operator == Operator.GE && count > 1
              || operator == Operator.GT && count > 0) {

            subQuery = new SqlSelect();

            if (BeeUtils.isEmpty(relationsField)) {
              subQuery
                  .addFields(source, sourceField)
                  .addFrom(source)
                  .setWhere(sourceWhere)
                  .addGroup(source, sourceField);

            } else if (BeeUtils.isEmpty(sourceField)) {
              subQuery
                  .addFields(TBL_RELATIONS, COL_COMPANY)
                  .addFrom(TBL_RELATIONS)
                  .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                  .setWhere(relationsWhere)
                  .addGroup(TBL_RELATIONS, COL_COMPANY);

            } else {
              SqlSelect unionQuery = new SqlSelect()
                  .addField(source, sourceField, COL_COMPANY)
                  .addFrom(source)
                  .setWhere(sourceWhere);

              unionQuery.setUnionAllMode(true).addUnion(new SqlSelect()
                  .addFields(TBL_RELATIONS, COL_COMPANY)
                  .addFrom(TBL_RELATIONS)
                  .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                  .setWhere(relationsWhere));

              String unionAlias = "Un" + SqlUtils.uniqueName();

              subQuery
                  .addFields(unionAlias, COL_COMPANY)
                  .addFrom(unionQuery, unionAlias)
                  .addGroup(unionAlias, COL_COMPANY);
            }

            IsExpression xpr = SqlUtils.aggregate(SqlFunction.COUNT, null);
            subQuery.setHaving(SqlUtils.compare(xpr, operator, SqlUtils.constant(count)));

            relConditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));
          }

          if (!relConditions.isEmpty()) {
            conditions.add(relConditions);
          }
        }
      }

      return conditions;
    });
  }

  public Document createRemindTemplate(SimpleRowSet data, Map<String, String> labels,
      Map<String, ValueType> format, Map<String, String> enumKeys,
      String subject, String reminderText, Long userId) {
    return createRemindTemplate(data, labels, format, enumKeys, null, subject, reminderText,
        userId);
  }

  public Document createRemindTemplate(SimpleRowSet data, Map<String, String> labels,
      Map<String, ValueType> format, Map<String, String> enumKeys,
      List<String> excludedColumns, String subject,
      String reminderText, Long userId) {
    Assert.notNull(data);
    Document doc = new Document();
    doc.getHead().append(meta().encodingDeclarationUtf8(), title().text(subject));

    Table table = table();
    Tr trHead = tr();

    for (int i = 0; i < data.getNumberOfColumns(); i++) {
      if (BeeUtils.contains(excludedColumns, data.getColumnName(i))) {
        continue;
      }

      String label = BeeUtils.containsKey(labels, data.getColumnName(i))
          ? labels.get(data.getColumnName(i)) : data.getColumnName(i);

      Th th = th().text(label);
      th.setBorderWidth("1px");
      th.setBorderStyle(BorderStyle.SOLID);
      th.setBorderColor("black");
      trHead.append(th);
    }

    table.append(trHead);

    Range<Long> maybeTime = Range.closed(
        TimeUtils.startOfYear(TimeUtils.today(), -10).getTime(),
        TimeUtils.startOfYear(TimeUtils.today(), 100).getTime());

    DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo();

    for (SimpleRow row : data) {
      Tr tr = tr();

      for (int i = 0; i < data.getNumberOfColumns(); i++) {
        if (BeeUtils.contains(excludedColumns, data.getColumnName(i))) {
          continue;
        }

        if (BeeUtils.isEmpty(row.getValue(i))) {
          Td td = td();
          tr.append(td);
          td.setBorderWidth("1px");
          td.setBorderStyle(BorderStyle.SOLID);
          td.setBorderColor("black");
          continue;
        }

        ValueType type = format.get(data.getColumnName(i)) == null
            ? ValueType.TEXT : format.get(data.getColumnName(i));
        String value = BeeUtils.nvl(row.getValue(i), BeeConst.STRING_EMPTY);

        switch (type) {
          case LONG:
            Long x = row.getLong(i);
            if (x != null && maybeTime.contains(x)) {
              type = ValueType.DATE_TIME;
              value = Formatter.renderDateTime(dtfInfo, x);
            }
            break;
          case INTEGER:
            if (BeeUtils.containsKey(enumKeys, data.getColumnName(i))) {
              value = EnumUtils.getLocalizedCaption(enumKeys.get(data.getColumnName(i)),
                  row.getInt(i), usr.getDictionary(userId));
            }
            break;
          case DATE_TIME:
            value = Formatter.renderDateTime(dtfInfo, row.getDateTime(i));
            break;
          default:
            value = BeeUtils.nvl(row.getValue(i), BeeConst.STRING_EMPTY);
        }

        Td td = td();
        tr.append(td);
        td.text(value);
        td.setBorderWidth("1px");
        td.setBorderStyle(BorderStyle.SOLID);
        td.setBorderColor("black");

        if (ValueType.isNumeric(type) || ValueType.TEXT == type
            && CharMatcher.digit().matchesAnyOf(value) && BeeUtils.isDouble(value)) {
          td.setTextAlign(TextAlign.RIGHT);
        }
      }
      table.append(tr);
    }

    table.setBorderWidth("1px;");
    table.setBorderStyle(BorderStyle.SOLID);
    table.setBorderSpacing("0px;");

    doc.getBody().append(p().text(reminderText));
    doc.getBody().append(table);

    return doc;
  }

  private void createActionRemindMail(Long userId, SimpleRowSet appointments) {
    Long accountId = mail.getSenderAccountId(TIMER_REMIND_COMPANY_ACTIONS);
    String to = usr.getUserEmail(userId, false);

    if (!DataUtils.isId(accountId) && BeeUtils.isEmpty(to)) {
      return;
    }

    Dictionary dic = usr.getDictionary();
    String subject = dic.calMailPlannedActionSubject();
    String reminderText = dic.calMailPlannedActionText();

    String appointmentIdName = sys.getIdName(CalendarConstants.TBL_APPOINTMENTS);

    Map<String, String> labels =
        CalendarHelper.getAppointmentReminderDataLabels(dic, appointmentIdName);
    Map<String, ValueType> format =
        CalendarHelper.getAppointmentReminderDataTypes(appointmentIdName);
    Map<String, String> enumKeys = CalendarHelper.getAppointmentReminderDataEnumKeys();
    Document doc = createRemindTemplate(appointments, labels, format, enumKeys,
        BeeConst.STRING_EMPTY, reminderText, userId);

    mail.sendMail(accountId, to, subject, doc.buildLines());
    setRemindedAppointments(Lists.newArrayList(appointments.getLongColumn(appointmentIdName)));
    logger.info(TIMER_REMIND_COMPANY_ACTIONS, "mail send, user id", userId,
        ", company action count", appointments.getRows().size());
  }

  private ResponseObject createCompany(Map<String, String> companyInfo) {
    String companyName = companyInfo.get(COL_COMPANY_NAME);
    String companyCode = companyInfo.get(COL_COMPANY_CODE);
    HasConditions clause = SqlUtils.or();

    if (!BeeUtils.isEmpty(companyCode)) {
      clause.add(SqlUtils.equals(TBL_COMPANIES, COL_COMPANY_CODE, companyCode));
    }
    if (!BeeUtils.isEmpty(companyName)) {
      clause.add(SqlUtils.same(TBL_COMPANIES, COL_COMPANY_NAME, companyName));
    }
    if (clause.isEmpty()) {
      return ResponseObject.parameterNotFound(SVC_CREATE_COMPANY, COL_COMPANY_NAME);
    }
    Long company = qs.getLong(new SqlSelect()
        .addFields(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES))
        .addFrom(TBL_COMPANIES)
        .setWhere(clause));

    if (DataUtils.isId(company)) {
      return ResponseObject.response(company);
    }
    return createEntity(VIEW_COMPANIES, companyInfo);
  }

  private ResponseObject createCompanyPerson(Map<String, String> personInfo) {
    String company = personInfo.get(COL_COMPANY);

    if (!DataUtils.isId(company)) {
      ResponseObject response = createCompany(Collections.singletonMap(COL_COMPANY_NAME, company));

      if (response.hasErrors()) {
        return response;
      }
      personInfo.put(COL_COMPANY, BeeUtils.toString(response.getResponseAsLong()));
    }
    String firstName = personInfo.get(COL_FIRST_NAME);
    String lastName = personInfo.get(COL_LAST_NAME);
    HasConditions clause = SqlUtils.and();

    if (!BeeUtils.isEmpty(firstName)) {
      clause.add(SqlUtils.same(TBL_PERSONS, COL_FIRST_NAME, firstName));

      if (!BeeUtils.isEmpty(lastName)) {
        clause.add(SqlUtils.same(TBL_PERSONS, COL_LAST_NAME, lastName));
      } else {
        clause.add(SqlUtils.isNull(TBL_PERSONS, COL_LAST_NAME));
      }
    }
    if (clause.isEmpty()) {
      return ResponseObject.parameterNotFound(SVC_CREATE_COMPANY_PERSON, COL_FIRST_NAME);
    }
    Long person = qs.getLong(new SqlSelect()
        .addFields(TBL_COMPANY_PERSONS, sys.getIdName(TBL_COMPANY_PERSONS))
        .addFrom(TBL_COMPANY_PERSONS)
        .addFromInner(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .setWhere(clause.add(SqlUtils.equals(TBL_COMPANY_PERSONS, COL_COMPANY, company))));

    if (DataUtils.isId(person)) {
      return ResponseObject.response(person);
    }
    personInfo.put(COL_PERSON, BeeUtils.toString(qs.insertData(new SqlInsert(TBL_PERSONS)
        .addConstant(COL_FIRST_NAME, firstName)
        .addNotEmpty(COL_LAST_NAME, lastName))));

    return createEntity(VIEW_COMPANY_PERSONS, personInfo);
  }

  private ResponseObject createEntity(String viewName, Map<String, String> viewInfo) {
    BeeView view = sys.getView(viewName);
    List<BeeColumn> columns = view.getRowSetColumns();
    List<String> values = new ArrayList<>();

    Long country = null;
    Pair<String, String> city = null;

    for (BeeColumn column : columns) {
      String name = column.getId();
      String value = column.isEditable() ? viewInfo.get(name) : null;

      if (Objects.nonNull(value)) {
        String relation = view.getColumnRelation(name);

        if (!BeeUtils.isEmpty(relation) && !DataUtils.isId(value)) {
          if (Objects.equals(relation, TBL_CITIES)) {
            city = Pair.of(name, value);
          } else {
            String relCol = sys.getTable(relation).getFields().stream()
                .filter(BeeTable.BeeField::isUnique)
                .filter(BeeTable.BeeField::isNotNull)
                .map(BeeTable.BeeField::getName)
                .findFirst().orElse(null);

            if (BeeUtils.isEmpty(relCol)) {
              return ResponseObject.error("Unknown relation", relation, "field for column", name,
                  "and value", value);
            } else {
              Long id = qs.getLong(new SqlSelect()
                  .addFields(relation, sys.getIdName(relation))
                  .addFrom(relation)
                  .setWhere(SqlUtils.same(relation, relCol, value)));

              if (!DataUtils.isId(id)) {
                id = qs.insertData(new SqlInsert(relation)
                    .addConstant(relCol, value));
              }
              value = BeeUtils.toString(id);
            }
          }
        }
        if (Objects.equals(relation, TBL_COUNTRIES)) {
          country = BeeUtils.toLongOrNull(value);
        }
      }
      values.add(value);
    }
    if (Objects.nonNull(city)) {
      Long id = qs.getLong(new SqlSelect()
          .addFields(TBL_CITIES, sys.getIdName(TBL_CITIES))
          .addFrom(TBL_CITIES)
          .setWhere(SqlUtils.and(SqlUtils.same(TBL_CITIES, COL_CITY_NAME, city.getB()),
              SqlUtils.equals(TBL_CITIES, COL_COUNTRY, country))));

      if (!DataUtils.isId(id)) {
        id = qs.insertData(new SqlInsert(TBL_CITIES)
            .addConstant(COL_CITY_NAME, city.getB())
            .addNotNull(COL_COUNTRY, country));
      }
      values.set(DataUtils.getColumnIndex(city.getA(), columns), BeeUtils.toString(id));
    }
    BeeRowSet rowSet = DataUtils.createRowSetForInsert(view.getName(), columns, values);
    ResponseObject response = deb.commitRow(rowSet, RowInfo.class);

    if (response.hasErrors()) {
      return response;
    }
    return ResponseObject.response(((RowInfo) response.getResponse()).getId());
  }

  private ResponseObject getCompanyInfo(Long companyId, String locale) {
    if (!DataUtils.isId(companyId)) {
      return ResponseObject.error("Wrong company ID");
    }
    SimpleRow row = qs.getRow(new SqlSelect()
        .addFields(TBL_COMPANIES, COL_COMPANY_NAME, COL_COMPANY_CODE, COL_COMPANY_VAT_CODE)
        .addFields(TBL_CONTACTS, COL_ADDRESS, COL_POST_INDEX, COL_PHONE, COL_MOBILE, COL_FAX)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addField(TBL_CITIES, COL_CITY_NAME, COL_CITY)
        .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, COL_COUNTRY)
        .addFields(TBL_COMPANY_BANK_ACCOUNTS, COL_BANK_ACCOUNT)
        .addField(TBL_BANKS, COL_BANK_NAME, COL_BANK)
        .addFields(TBL_BANKS, COL_BANK_CODE, COL_SWIFT_CODE)
        .addFrom(TBL_COMPANIES)
        .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
        .addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
        .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CONTACTS, COL_CITY))
        .addFromLeft(TBL_COUNTRIES, sys.joinTables(TBL_COUNTRIES, TBL_CONTACTS, COL_COUNTRY))
        .addFromLeft(TBL_COMPANY_BANK_ACCOUNTS,
            sys.joinTables(TBL_COMPANY_BANK_ACCOUNTS, TBL_COMPANIES, COL_DEFAULT_BANK_ACCOUNT))
        .addFromLeft(TBL_BANKS, sys.joinTables(TBL_BANKS, TBL_COMPANY_BANK_ACCOUNTS, COL_BANK))
        .setWhere(sys.idEquals(TBL_COMPANIES, companyId)));

    Dictionary constants = Localizations.getDictionary(SupportedLocale.parse(locale));

    Map<String, String> translations = new HashMap<>();
    translations.put(COL_COMPANY_NAME, constants.company());
    translations.put(COL_COMPANY_CODE, constants.companyCode());
    translations.put(COL_COMPANY_VAT_CODE, constants.companyVATCode());
    translations.put(COL_ADDRESS, constants.address());
    translations.put(COL_POST_INDEX, constants.postIndex());
    translations.put(COL_PHONE, constants.phone());
    translations.put(COL_MOBILE, constants.mobile());
    translations.put(COL_FAX, constants.fax());
    translations.put(COL_EMAIL_ADDRESS, constants.email());
    translations.put(COL_CITY, constants.city());
    translations.put(COL_COUNTRY, constants.country());
    translations.put(COL_BANK, constants.bank());
    translations.put(COL_BANK_CODE, constants.printBankCode());
    translations.put(COL_SWIFT_CODE, constants.printBankSwift());
    translations.put(COL_BANK_ACCOUNT, constants.printBankAccount());

    Map<String, Pair<String, String>> info = new HashMap<>();

    for (String col : translations.keySet()) {
      info.put(col, Pair.of(translations.get(col), row.getValue(col)));
    }
    return ResponseObject.response(info);
  }

  private ResponseObject getCompanyTypeReport(RequestInfo reqInfo) {
    Long startDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_FROM));
    Long endDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_TO));

    Set<Long> filterTypes = DataUtils.parseIdSet(reqInfo.getParameter(COL_RELATION_TYPE));

    BeeTable table = sys.getTable(TBL_COMPANIES);
    if (!table.isAuditable()) {
      return ResponseObject.warning(TBL_COMPANIES, "is not auditable");
    }

    String auditSource = sys.getAuditSource(table.getName());

    String idName = table.getIdName();

    IsExpression minTime = SqlUtils.aggregate(SqlFunction.MIN,
        SqlUtils.field(auditSource, AUDIT_FLD_TIME));

    SqlSelect companyQuery = new SqlSelect()
        .addFields(TBL_COMPANIES, idName)
        .addExpr(minTime, AUDIT_FLD_TIME)
        .addEmptyNumeric(BeeConst.YEAR, 4, 0)
        .addEmptyNumeric(BeeConst.MONTH, 2, 0)
        .addFrom(TBL_COMPANIES)
        .addFromInner(auditSource,
            SqlUtils.join(auditSource, AUDIT_FLD_ID, TBL_COMPANIES, idName));

    if (!filterTypes.isEmpty()) {
      companyQuery.addFromInner(TBL_COMPANY_RELATION_TYPE_STORE,
          sys.joinTables(TBL_COMPANIES, TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY));
      companyQuery.setWhere(SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE,
          COL_RELATION_TYPE, filterTypes));
    }

    companyQuery.addGroup(TBL_COMPANIES, idName);

    if (startDate != null || endDate != null) {
      HasConditions having = SqlUtils.and();

      if (startDate != null) {
        having.add(SqlUtils.moreEqual(minTime, startDate));
      }
      if (endDate != null) {
        having.add(SqlUtils.less(minTime, endDate));
      }

      companyQuery.setHaving(having);
    }

    String tmp = qs.sqlCreateTemp(companyQuery);

    long count = qs.setYearMonth(tmp, AUDIT_FLD_TIME, BeeConst.YEAR, BeeConst.MONTH);
    if (count <= 0) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    String countName = "Cnt" + SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect()
        .addFields(tmp, BeeConst.YEAR, BeeConst.MONTH)
        .addFields(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE)
        .addFields(TBL_COMPANY_RELATION_TYPES, COL_RELATION_TYPE_NAME)
        .addCount(countName)
        .addFrom(tmp)
        .addFromLeft(TBL_COMPANY_RELATION_TYPE_STORE,
            SqlUtils.join(TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY, tmp, idName))
        .addFromLeft(TBL_COMPANY_RELATION_TYPES,
            sys.joinTables(TBL_COMPANY_RELATION_TYPES, TBL_COMPANY_RELATION_TYPE_STORE,
                COL_RELATION_TYPE))
        .addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH)
        .addGroup(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE)
        .addGroup(TBL_COMPANY_RELATION_TYPES, COL_RELATION_TYPE_NAME);

    if (filterTypes.isEmpty()) {
      query.setWhere(SqlUtils.notNull(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE));
    } else {
      query.setWhere(SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE,
          filterTypes));
    }

    SimpleRowSet result = qs.getData(query);

    if (filterTypes.isEmpty()) {
      query.setWhere(SqlUtils.isNull(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE));

      SimpleRowSet other = qs.getData(query);
      if (!DataUtils.isEmpty(other)) {
        result.append(other);
      }

      SqlSelect totalQuery = new SqlSelect()
          .addFields(tmp, BeeConst.YEAR, BeeConst.MONTH)
          .addCount(countName)
          .addFrom(tmp)
          .addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH);

      SimpleRowSet totals = qs.getData(totalQuery);

      if (!DataUtils.isEmpty(totals)) {
        for (SimpleRow totalRow : totals) {
          SimpleRow row = result.addEmptyRow();

          row.setValue(BeeConst.YEAR, totalRow.getValue(BeeConst.YEAR));
          row.setValue(BeeConst.MONTH, totalRow.getValue(BeeConst.MONTH));

          row.setValue(COL_RELATION_TYPE, BeeConst.STRING_NUMBER_SIGN);

          row.setValue(countName, totalRow.getValue(countName));
        }
      }
    }

    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(result)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private void explain(Object... messages) {
    Long userId = usr.getCurrentUserId();
    String text = ArrayUtils.joinWords(messages);

    if (DataUtils.isId(userId) && !BeeUtils.isEmpty(text)) {
      Endpoint.sendToUser(usr.getCurrentUserId(), LogMessage.debug(text));
    }
  }

  private ResponseObject getPriceAndDiscount(RequestInfo reqInfo) {
    Long company = reqInfo.getParameterLong(COL_DISCOUNT_COMPANY);
    if (company == null) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_DISCOUNT_COMPANY);
    }

    Long item = reqInfo.getParameterLong(COL_DISCOUNT_ITEM);
    if (!DataUtils.isId(item)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_DISCOUNT_ITEM);
    }

    Long operation = reqInfo.getParameterLong(COL_DISCOUNT_OPERATION);
    Long warehouse = reqInfo.getParameterLong(COL_DISCOUNT_WAREHOUSE);

    Long time = reqInfo.getParameterLong(Service.VAR_TIME);
    if (!BeeUtils.isPositive(time)) {
      time = System.currentTimeMillis();
    }

    Double qty = reqInfo.getParameterDouble(Service.VAR_QTY);
    Long unit = reqInfo.getParameterLong(COL_DISCOUNT_UNIT);

    Long currency = reqInfo.getParameterLong(COL_DISCOUNT_CURRENCY);
    if (!DataUtils.isId(currency)) {
      currency = prm.getRelation(PRM_CURRENCY);
    }

    ItemPrice defPriceName = EnumUtils.getEnumByIndex(ItemPrice.class,
        reqInfo.getParameterInt(COL_DISCOUNT_PRICE_NAME));
    if (defPriceName == null) {
      defPriceName = ItemPrice.SALE;
    }

    Set<String> requiredColumns = NameUtils.toSet(reqInfo.getParameter(Service.VAR_REQUIRED));

    int explain = BeeUtils.unbox(reqInfo.getParameterInt(Service.VAR_EXPLAIN));
    if (explain > 0) {
      explain(SVC_GET_PRICE_AND_DISCOUNT,
          BeeUtils.joinOptions(COL_DISCOUNT_COMPANY, company, COL_DISCOUNT_ITEM, item,
              COL_DISCOUNT_OPERATION, operation, COL_DISCOUNT_WAREHOUSE, warehouse,
              Service.VAR_TIME, time, Service.VAR_QTY, qty, COL_DISCOUNT_UNIT, unit,
              COL_DISCOUNT_CURRENCY, currency, COL_DISCOUNT_PRICE_NAME, defPriceName,
              Service.VAR_REQUIRED, requiredColumns));
    }

    List<Long> companyParents = getDiscountParents(company);
    if (explain > 0) {
      explain(COL_DISCOUNT_PARENT, companyParents.size(),
          companyParents.isEmpty() ? BeeConst.STRING_EMPTY : companyParents.toString());
    }

    Map<Long, Long> categories = getItemCategories(item);
    if (explain > 0) {
      explain(TBL_ITEM_CATEGORIES, categories.size(),
          categories.isEmpty() ? BeeConst.STRING_EMPTY : categories.keySet().toString());
    }

    EnumMap<ItemPrice, Double> itemPrices = getItemPrices(item, currency, time);
    if (explain > 0) {
      explain(NameUtils.getClassName(ItemPrice.class), itemPrices.size(),
          itemPrices.isEmpty() ? BeeConst.STRING_EMPTY : itemPrices.toString());
    }

    Double defPrice = itemPrices.get(defPriceName);

    HasConditions discountWhere = SqlUtils.and();

    HasConditions companyWhere = SqlUtils.or();
    if (!requiredColumns.contains(COL_DISCOUNT_COMPANY) || !DataUtils.isId(company)) {
      companyWhere.add(SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_COMPANY));
    }
    if (DataUtils.isId(company)) {
      companyWhere.add(SqlUtils.equals(TBL_DISCOUNTS, COL_DISCOUNT_COMPANY, company));
      if (!BeeUtils.isEmpty(companyParents)) {
        companyWhere.add(SqlUtils.inList(TBL_DISCOUNTS, COL_DISCOUNT_COMPANY, companyParents));
      }
    }

    if (!companyWhere.isEmpty()) {
      discountWhere.add(companyWhere);
    }

    IsCondition itemWhere = SqlUtils.equals(TBL_DISCOUNTS, COL_DISCOUNT_ITEM, item);
    if (requiredColumns.contains(COL_DISCOUNT_ITEM)) {
      discountWhere.add(itemWhere);

    } else {
      HasConditions categoryWhere = SqlUtils.or();
      if (!requiredColumns.contains(COL_DISCOUNT_CATEGORY) || BeeUtils.isEmpty(categories)) {
        categoryWhere.add(SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_CATEGORY));
      }
      if (!BeeUtils.isEmpty(categories)) {
        categoryWhere.add(SqlUtils.inList(TBL_DISCOUNTS, COL_DISCOUNT_CATEGORY,
            categories.keySet()));
      }

      discountWhere.add(SqlUtils.or(itemWhere,
          SqlUtils.and(SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_ITEM), categoryWhere)));
    }

    HasConditions operationWhere = SqlUtils.or();
    if (!requiredColumns.contains(COL_DISCOUNT_OPERATION) || !DataUtils.isId(operation)) {
      operationWhere.add(SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_OPERATION));
    }
    if (DataUtils.isId(operation)) {
      operationWhere.add(SqlUtils.equals(TBL_DISCOUNTS, COL_DISCOUNT_OPERATION, operation));
    }

    if (!operationWhere.isEmpty()) {
      discountWhere.add(operationWhere);
    }

    HasConditions warehouseWhere = SqlUtils.or();
    if (!requiredColumns.contains(COL_DISCOUNT_WAREHOUSE) || !DataUtils.isId(warehouse)) {
      warehouseWhere.add(SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_WAREHOUSE));
    }
    if (DataUtils.isId(warehouse)) {
      warehouseWhere.add(SqlUtils.equals(TBL_DISCOUNTS, COL_DISCOUNT_WAREHOUSE, warehouse));
    }

    if (!warehouseWhere.isEmpty()) {
      discountWhere.add(warehouseWhere);
    }

    HasConditions timeWhere = SqlUtils.and(
        SqlUtils.or(
            SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_DATE_FROM),
            SqlUtils.lessEqual(TBL_DISCOUNTS, COL_DISCOUNT_DATE_FROM, time)),
        SqlUtils.or(
            SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_DATE_TO),
            SqlUtils.more(TBL_DISCOUNTS, COL_DISCOUNT_DATE_TO, time)));

    discountWhere.add(timeWhere);

    HasConditions qtyWhere;
    if (qty == null) {
      qtyWhere = SqlUtils.or(SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_FROM),
          SqlUtils.nonPositive(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_FROM));
    } else {
      qtyWhere = SqlUtils.and(
          SqlUtils.or(
              SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_FROM),
              SqlUtils.lessEqual(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_FROM, qty)),
          SqlUtils.or(
              SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_TO),
              SqlUtils.more(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_TO, qty)));

      if (DataUtils.isId(unit)) {
        qtyWhere.add(
            SqlUtils.or(
                SqlUtils.and(
                    SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_FROM),
                    SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_QUANTITY_TO)),
                SqlUtils.isNull(TBL_DISCOUNTS, COL_DISCOUNT_UNIT),
                SqlUtils.equals(TBL_DISCOUNTS, COL_DISCOUNT_UNIT, unit)));
      }
    }

    discountWhere.add(qtyWhere);

    discountWhere.add(SqlUtils.or(
        SqlUtils.notNull(TBL_DISCOUNTS, COL_DISCOUNT_PRICE_NAME),
        SqlUtils.notNull(TBL_DISCOUNTS, COL_DISCOUNT_PERCENT),
        SqlUtils.positive(TBL_DISCOUNTS, COL_DISCOUNT_PRICE)));

    SqlSelect discountQuery = new SqlSelect()
        .addFields(TBL_DISCOUNTS, COL_DISCOUNT_COMPANY, COL_DISCOUNT_CATEGORY, COL_DISCOUNT_ITEM,
            COL_DISCOUNT_PRICE_NAME, COL_DISCOUNT_PERCENT,
            COL_DISCOUNT_PRICE, COL_DISCOUNT_CURRENCY)
        .addFrom(TBL_DISCOUNTS)
        .setWhere(discountWhere);

    IsCondition carDiscountsWhere = getCarDiscountCondition(reqInfo);

    if (Objects.nonNull(carDiscountsWhere)) {
      discountWhere.add(carDiscountsWhere);
      discountQuery.addFromLeft(TBL_CAR_DISCOUNTS,
          sys.joinTables(TBL_CAR_DISCOUNTS, TBL_DISCOUNTS, COL_CAR_DISCOUNT));
    } else {
      discountWhere.add(SqlUtils.isNull(TBL_DISCOUNTS, COL_CAR_DISCOUNT));
    }

    if (explain > 1) {
      explain(discountQuery);
    }

    SimpleRowSet discountData = qs.getData(discountQuery);
    if (explain > 0) {
      explain(TBL_DISCOUNTS, (discountData == null) ? 0 : discountData.getNumberOfRows());
    }

    List<PriceInfo> discounts = new ArrayList<>();

    if (!DataUtils.isEmpty(discountData)) {
      for (SimpleRow row : discountData) {
        PriceInfo pi = PriceInfo.fromDiscount(row);
        discounts.add(pi);

        if (explain > 0) {
          explain(pi);
        }
      }
    }

    if (DataUtils.isId(company) && requiredColumns.isEmpty()) {
      String companyIdName = sys.getIdName(TBL_COMPANIES);

      HasConditions cw = SqlUtils.or(SqlUtils.equals(TBL_COMPANIES, companyIdName, company));
      if (!BeeUtils.isEmpty(companyParents)) {
        cw.add(SqlUtils.inList(TBL_COMPANIES, companyIdName, companyParents));
      }

      SqlSelect companyQuery = new SqlSelect()
          .addFields(TBL_COMPANIES, companyIdName,
              COL_COMPANY_PRICE_NAME, COL_COMPANY_DISCOUNT_PERCENT)
          .addFrom(TBL_COMPANIES)
          .setWhere(SqlUtils.and(cw,
              SqlUtils.or(
                  SqlUtils.notNull(TBL_COMPANIES, COL_COMPANY_PRICE_NAME),
                  SqlUtils.notNull(TBL_COMPANIES, COL_COMPANY_DISCOUNT_PERCENT))));

      if (explain > 1) {
        explain(companyQuery);
      }

      SimpleRowSet companyData = qs.getData(companyQuery);
      if (explain > 0) {
        explain(TBL_COMPANIES, (companyData == null) ? 0 : companyData.getNumberOfRows());
      }

      if (!DataUtils.isEmpty(companyData)) {
        for (SimpleRow row : companyData) {
          PriceInfo pi = PriceInfo.fromCompany(row.getLong(companyIdName), row);
          discounts.add(pi);

          if (explain > 0) {
            explain(pi);
          }
        }
      }
    }

    Pair<Double, Double> result;

    if (discounts.isEmpty()) {
      if (explain > 0) {
        explain(BeeConst.NO, TBL_DISCOUNTS);
      }
      result = Pair.empty();

    } else {
      double toRate = getRate(currency, time);
      for (PriceInfo pi : discounts) {
        if (pi.hasPrice() && !Objects.equals(currency, pi.getCurrency())) {
          if (explain > 0) {
            explain("exchange", pi);
          }

          double fromRate = getRate(pi.getCurrency(), time);
          pi.setPrice(Localized.normalizeMoney(pi.getPrice() * fromRate / toRate));
          pi.setCurrency(currency);

          if (explain > 0) {
            explain(BeeUtils.joinOptions("fromRate", fromRate, "toRate", toRate,
                COL_DISCOUNT_PRICE, pi.getPrice(), COL_DISCOUNT_CURRENCY, pi.getCurrency()));
          }
        }
      }

      for (PriceInfo pi : discounts) {
        if (!pi.hasPrice() && pi.getPriceName() != null
            && itemPrices.containsKey(pi.getPriceName())) {

          if (explain > 0) {
            explain("set price", pi);
          }

          pi.setPrice(itemPrices.get(pi.getPriceName()));
          pi.setCurrency(currency);

          if (explain > 0) {
            explain(BeeUtils.joinOptions(COL_DISCOUNT_PRICE_NAME, pi.getPriceName(),
                COL_DISCOUNT_PRICE, pi.getPrice(), COL_DISCOUNT_CURRENCY, pi.getCurrency()));
          }
        }
      }

      result = getPriceAndDiscount(discounts, company, companyParents, categories, explain);
    }

    if (!BeeUtils.isPositive(result.getA()) && BeeUtils.isPositive(defPrice)
        && requiredColumns.isEmpty()) {

      result.setA(defPrice);
      if (explain > 0) {
        explain(COL_DISCOUNT_PRICE_NAME, "default", defPriceName, result.getA());
      }
    }

    if (explain > 0) {
      if (result.isNull()) {
        explain(SVC_GET_PRICE_AND_DISCOUNT, "result is null");
      } else {
        explain(SVC_GET_PRICE_AND_DISCOUNT, "result:",
            COL_DISCOUNT_PRICE, result.getA(), COL_DISCOUNT_PERCENT, result.getB());
      }
    }

    if (!BeeUtils.isPositive(result.getA()) && !BeeUtils.isDouble(result.getB())) {
      if (explain > 0) {
        explain(SVC_GET_PRICE_AND_DISCOUNT, "response is empty");
      }
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private Pair<Double, Double> getPriceAndDiscount(List<PriceInfo> discounts,
      Long company, List<Long> companyParents, Map<Long, Long> categories, int explain) {

    if (discounts.size() == 1) {
      PriceInfo pi = discounts.get(0);
      if (explain > 0) {
        explain("found 1 discount:", pi);
      }
      return Pair.of(pi.getPrice(), pi.getDiscountPercent());
    }

    List<Long> companies = new ArrayList<>();
    if (DataUtils.isId(company)) {
      companies.add(company);
    }
    if (!BeeUtils.isEmpty(companyParents)) {
      companies.addAll(companyParents);
    }

    if (explain > 0) {
      explain(TBL_COMPANIES, companies);
    }

    List<PriceInfo> input = new ArrayList<>(discounts);
    input.sort((o1, o2) -> {
      boolean x1 = o1.hasPrice() && o1.hasPercent();
      boolean x2 = o2.hasPrice() && o2.hasPercent();

      if (x1 && x2) {
        return BeeUtils.compareNullsLast(
            BeeUtils.minusPercent(o1.getPrice(), o1.getDiscountPercent()),
            BeeUtils.minusPercent(o2.getPrice(), o2.getDiscountPercent()));

      } else if (x1 || x2) {
        return Boolean.compare(x2, x1);

      } else if (o1.hasPrice() || o2.hasPrice()) {
        return BeeUtils.compareNullsLast(o1.getPrice(), o2.getPrice());

      } else if (o1.hasPercent() || o2.hasPercent()) {
        return BeeUtils.compareNullsLast(o2.getDiscountPercent(), o1.getDiscountPercent());

      } else {
        return BeeConst.COMPARE_EQUAL;
      }
    });

    if (explain > 0) {
      explain("ordered input", BeeUtils.bracket(input.size()));
      input.forEach(this::explain);
    }

    Pair<Double, Double> result = Pair.empty();

    for (Long co : companies) {
      for (PriceInfo pi : input) {
        if (co.equals(pi.getCompany()) && DataUtils.isId(pi.getItem())) {
          acceptDiscount(result, pi, (explain > 0) ? "company + item" : null);

          if (result.noNulls()) {
            return result;
          }
        }
      }
    }

    Set<Long> discountCategories = new HashSet<>();
    List<List<Long>> categoryBranches = new ArrayList<>();

    if (!BeeUtils.isEmpty(categories)) {
      for (PriceInfo pi : input) {
        Long cat = pi.getCategory();
        if (DataUtils.isId(cat) && categories.containsKey(cat) && !DataUtils.isId(pi.getItem())) {
          discountCategories.add(cat);
        }
      }
    }

    if (!discountCategories.isEmpty()) {
      Map<Long, Integer> categoryLevels = new HashMap<>();
      ListMultimap<Long, Long> roots = ArrayListMultimap.create();

      for (Long category : discountCategories) {
        int level = 0;
        Long root = category;

        Long parent = categories.get(category);
        while (parent != null) {
          level++;
          root = parent;

          parent = categories.get(parent);
        }

        categoryLevels.put(category, level);
        roots.put(root, category);
      }

      for (Long root : roots.keySet()) {
        List<Long> branch = new ArrayList<>(roots.get(root));

        if (branch.size() > 1) {
          branch.sort((o1, o2) ->
              BeeUtils.compareNullsLast(categoryLevels.get(o2), categoryLevels.get(o1)));
        }

        categoryBranches.add(branch);
      }

      if (explain > 0) {
        explain("category branches", categoryBranches);
      }
    }

    List<Pair<Double, Double>> candidates = new ArrayList<>();
    Pair<Double, Double> pp;

    for (List<Long> branch : categoryBranches) {
      pp = getBranchPriceAndDiscount(input, branch, companies, explain);
      if (!pp.isNull()) {
        candidates.add(pp);

        if (explain > 0) {
          explain("candidate company + category", branch, format(pp));
        }
      }
    }

    pp = getCompanyLevelPriceAndDiscount(input, companies, explain);
    if (!pp.isNull()) {
      candidates.add(pp);

      if (explain > 0) {
        explain("candidate company", format(pp));
      }
    }

    pp = getItemPriceAndDiscount(input, explain);
    if (!pp.isNull()) {
      candidates.add(pp);

      if (explain > 0) {
        explain("candidate item", format(pp));
      }
    }

    for (List<Long> branch : categoryBranches) {
      pp = getBranchPriceAndDiscount(input, branch, explain);
      if (!pp.isNull()) {
        candidates.add(pp);

        if (explain > 0) {
          explain("candidate category", branch, format(pp));
        }
      }
    }

    if (!candidates.isEmpty()) {
      if (result.getA() != null) {
        OptionalDouble maxPercent = candidates
            .stream()
            .filter(e -> e.getA() == null && e.getB() != null)
            .mapToDouble(Pair::getB)
            .max();

        if (maxPercent.isPresent()) {
          result.setB(maxPercent.getAsDouble());

          if (explain > 0) {
            explain("max candidate percent", result.getB());
          }
        }

      } else if (result.getB() != null) {
        OptionalDouble minPrice = candidates
            .stream()
            .filter(e -> e.getA() != null && e.getB() == null)
            .mapToDouble(Pair::getA)
            .min();

        if (minPrice.isPresent()) {
          result.setB(minPrice.getAsDouble());

          if (explain > 0) {
            explain("min candidate price", result.getB());
          }
        }

      } else if (candidates.stream().anyMatch(e -> e.getA() != null)) {
        Optional<Pair<Double, Double>> best = candidates
            .stream()
            .filter(e -> e.getA() != null)
            .min((e1, e2) -> BeeUtils.compareNullsLast(BeeUtils.minusPercent(e1.getA(), e1.getB()),
                BeeUtils.minusPercent(e2.getA(), e2.getB())));

        if (best.isPresent()) {
          result.setA(best.get().getA());
          result.setB(best.get().getB());

          if (explain > 0) {
            explain("best candidate", format(result));
          }
        }

      } else {
        OptionalDouble maxPercent = candidates
            .stream()
            .filter(e -> e.getB() != null)
            .mapToDouble(Pair::getB)
            .max();

        if (maxPercent.isPresent()) {
          result.setB(maxPercent.getAsDouble());

          if (explain > 0) {
            explain("max candidate percent", result.getB());
          }
        }
      }
    }

    return result;
  }

  private ResponseObject filterOrders(RequestInfo reqInfo) {
    String[] orders = Codec.beeDeserializeCollection(reqInfo.getParameter(TBL_ORDERS));
    String[] repairs = Codec.beeDeserializeCollection(reqInfo
        .getParameter(COL_SERVICE_MAINTENANCE));
    Long itemId = reqInfo.getParameterLong(COL_ITEM);

    if (orders.length == 0 && repairs.length == 0) {
      return ResponseObject.parameterNotFound(SVC_FILTER_ORDERS, TBL_ORDERS);
    }
    if (!DataUtils.isId(itemId)) {
      return ResponseObject.parameterNotFound(SVC_FILTER_ORDERS, COL_ITEM);
    }

    Map<Pair<String, String>, Pair<Double, Double>> remainderMap = new HashMap<>();

    List<Pair<String, String>> objects = new ArrayList<>();

    if (orders != null) {
      Arrays.asList(orders).forEach(orderId -> {
        if (DataUtils.isId(orderId)) {
          objects.add(Pair.of(COL_ORDER, orderId));
        }
      });
    }
    if (repairs != null) {
      Arrays.asList(repairs).forEach(repairId -> {
        if (DataUtils.isId(repairId)) {
          objects.add(Pair.of(COL_SERVICE_MAINTENANCE, repairId));
        }
      });
    }

    for (Pair<String, String> object : objects) {
      SqlSelect slcOrderItems = new SqlSelect()
          .addField(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS), COL_ORDER_ITEM)
          .addFields(TBL_ORDER_ITEMS, COL_RESERVED_REMAINDER)
          .addFrom(TBL_ORDER_ITEMS);

      if (BeeUtils.same(object.getA(), COL_ORDER)) {
        slcOrderItems.setWhere(SqlUtils.and(SqlUtils
            .equals(TBL_ORDER_ITEMS, COL_ORDER, object.getB(), COL_ITEM, itemId)));

      } else if (BeeUtils.same(object.getA(), COL_SERVICE_MAINTENANCE)) {
        slcOrderItems.setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ITEM, itemId),
            SqlUtils.in(TBL_ORDER_ITEMS, COL_SERVICE_ITEM,
                TBL_SERVICE_ITEMS, sys.getIdName(TBL_SERVICE_ITEMS),
                SqlUtils.equals(TBL_SERVICE_ITEMS, COL_SERVICE_MAINTENANCE, object.getB()))
        ));
      }

      SimpleRowSet orderItems = qs.getData(slcOrderItems);
      double resQty = BeeConst.DOUBLE_ZERO;

      for (SimpleRow orderItem : orderItems) {
        resQty += BeeUtils.unbox(orderItem.getDouble(COL_RESERVED_REMAINDER));
      }

      SqlSelect slcInvoices = new SqlSelect()
          .addFields(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY)
          .addFrom(VIEW_ORDER_CHILD_INVOICES)
          .addFromLeft(TBL_SALE_ITEMS, sys.joinTables(TBL_SALE_ITEMS, VIEW_ORDER_CHILD_INVOICES,
              COL_SALE_ITEM))
          .addFromLeft(TBL_SALES, sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE))
          .setWhere(SqlUtils.and(SqlUtils.inList(VIEW_ORDER_CHILD_INVOICES, COL_ORDER_ITEM,
              (Object[]) orderItems.getLongColumn(COL_ORDER_ITEM)),
              SqlUtils.isNull(TBL_SALES, COL_TRADE_EXPORTED)));

      double invoiceQty = BeeConst.DOUBLE_ZERO;
      for (SimpleRow invoice : qs.getData(slcInvoices)) {
        invoiceQty += BeeUtils.unbox(invoice.getDouble(COL_TRADE_ITEM_QUANTITY));
      }

      remainderMap.put(object, Pair.of(resQty, invoiceQty));
    }

    return ResponseObject.response(remainderMap);
  }

  private static String format(Pair<Double, Double> pp) {
    return BeeUtils.joinOptions(COL_DISCOUNT_PRICE, pp.getA(), COL_DISCOUNT_PERCENT, pp.getB());
  }

  private static IsCondition getCompanyActionsFilter(Long user, int remindBefore) {
    JustDate today = new JustDate();
    long nowInHours = new DateTime().getTime() / TimeUtils.MILLIS_PER_HOUR;

    IsCondition hasCompany = SqlUtils.notNull(CalendarConstants.TBL_APPOINTMENTS, COL_COMPANY);

    IsCondition isCompleted = SqlUtils.inList(CalendarConstants.TBL_APPOINTMENTS,
        CalendarConstants.COL_STATUS, AppointmentStatus.COMPLETED.ordinal(),
        AppointmentStatus.CONFIRMED.ordinal());

    IsCondition isActive = SqlUtils.not(isCompleted);

    IsCondition notResult = SqlUtils.isNull(CalendarConstants.TBL_APPOINTMENTS,
        CalendarConstants.COL_ACTION_RESULT);
    IsCondition notRemind = SqlUtils.isNull(CalendarConstants.TBL_APPOINTMENTS,
        CalendarConstants.COL_ACTION_REMINDED);
    IsCondition validStart = SqlUtils.more(CalendarConstants.TBL_APPOINTMENTS,
        CalendarConstants.COL_START_DATE_TIME,
        today);
    IsCondition isOwner = SqlUtils.equals(CalendarConstants.TBL_APPOINTMENTS,
        CalendarConstants.COL_CREATOR, user);

    IsExpression remindStartHrs = SqlUtils.divide(
        SqlUtils.field(CalendarConstants.TBL_APPOINTMENTS,
            CalendarConstants.COL_START_DATE_TIME), TimeUtils.MILLIS_PER_HOUR);

    IsCondition canRemind = SqlUtils.moreEqual(SqlUtils.minus(remindStartHrs, nowInHours),
        remindBefore);

    return SqlUtils.and(hasCompany, isActive, notResult, notRemind, validStart, canRemind, isOwner);
  }

  private Pair<Double, Double> getCompanyLevelPriceAndDiscount(List<PriceInfo> discounts,
      List<Long> companies, int explain) {

    Pair<Double, Double> result = Pair.empty();

    for (Long co : companies) {
      for (PriceInfo pi : discounts) {
        if (co.equals(pi.getCompany()) && !DataUtils.isId(pi.getItem())
            && !DataUtils.isId(pi.getCategory())) {

          acceptDiscount(result, pi, (explain > 0) ? "company" : null);
          if (result.noNulls()) {
            return result;
          }
        }
      }
    }

    return result;
  }

  private Pair<Double, Double> getItemPriceAndDiscount(List<PriceInfo> discounts, int explain) {
    Pair<Double, Double> result = Pair.empty();

    for (PriceInfo pi : discounts) {
      if (!DataUtils.isId(pi.getCompany()) && DataUtils.isId(pi.getItem())) {
        acceptDiscount(result, pi, (explain > 0) ? "item" : null);
        if (result.noNulls()) {
          return result;
        }
      }
    }

    return result;
  }

  private Pair<Double, Double> getBranchPriceAndDiscount(List<PriceInfo> discounts,
      List<Long> branch, int explain) {

    Pair<Double, Double> result = Pair.empty();

    for (Long cat : branch) {
      for (PriceInfo pi : discounts) {
        if (!DataUtils.isId(pi.getCompany()) && cat.equals(pi.getCategory())
            && !DataUtils.isId(pi.getItem())) {

          acceptDiscount(result, pi, (explain > 0) ? "category" : null);
          if (result.noNulls()) {
            return result;
          }
        }
      }
    }

    return result;
  }

  private Pair<Double, Double> getBranchPriceAndDiscount(List<PriceInfo> discounts,
      List<Long> branch, List<Long> companies, int explain) {

    Pair<Double, Double> result = Pair.empty();

    for (Long cat : branch) {
      for (Long co : companies) {
        for (PriceInfo pi : discounts) {
          if (co.equals(pi.getCompany()) && cat.equals(pi.getCategory())
              && !DataUtils.isId(pi.getItem())) {

            acceptDiscount(result, pi, (explain > 0) ? "company + category" : null);
            if (result.noNulls()) {
              return result;
            }
          }
        }
      }
    }

    return result;
  }

  private void acceptDiscount(Pair<Double, Double> priceAndPercent, PriceInfo priceInfo,
      String source) {

    boolean updatePrice = priceAndPercent.getA() == null && priceInfo.hasPrice();
    if (updatePrice) {
      priceAndPercent.setA(priceInfo.getPrice());
      if (source != null) {
        explain(source, COL_DISCOUNT_PRICE, priceAndPercent.getA(), "from", priceInfo);
      }
    }

    if ((priceAndPercent.getB() == null || updatePrice) && priceInfo.hasPercent()) {
      priceAndPercent.setB(priceInfo.getDiscountPercent());
      if (source != null) {
        explain(source, COL_DISCOUNT_PERCENT, priceAndPercent.getB(), "from", priceInfo);
      }
    }
  }

  private IsCondition getCarDiscountCondition(RequestInfo reqInfo) {
    Long model = reqInfo.getParameterLong(COL_MODEL);
    JustDate prodDate = TimeUtils.toDateOrNull(reqInfo.getParameterInt(COL_PRODUCTION_DATE));
    Long job = reqInfo.getParameterLong(COL_JOB);

    if (!BeeUtils.anyNotNull(model, prodDate, job)) {
      return null;
    }
    if (BeeUtils.isPositive(reqInfo.getParameterInt(Service.VAR_EXPLAIN))) {
      explain(SVC_GET_PRICE_AND_DISCOUNT, TBL_CAR_DISCOUNTS,
          BeeUtils.joinOptions(COL_MODEL, model, COL_PRODUCTION_DATE, prodDate, COL_JOB, job));
    }
    HasConditions carDiscountWhere = SqlUtils.and();

    Map<String, Long> crit = new HashMap<>();
    crit.put(COL_MODEL, model);
    crit.put(COL_JOB, job);

    crit.forEach((fld, id) -> {
      IsCondition clause = SqlUtils.isNull(TBL_CAR_DISCOUNTS, fld);

      if (DataUtils.isId(id)) {
        clause = SqlUtils.or(clause, SqlUtils.equals(TBL_CAR_DISCOUNTS, fld, id));
      }
      carDiscountWhere.add(clause);
    });
    IsCondition clause = SqlUtils.isNull(TBL_CAR_DISCOUNTS, COL_PRODUCED_FROM);

    if (Objects.nonNull(prodDate)) {
      clause = SqlUtils.or(clause,
          SqlUtils.lessEqual(TBL_CAR_DISCOUNTS, COL_PRODUCED_FROM, prodDate));
    }
    carDiscountWhere.add(clause);

    clause = SqlUtils.isNull(TBL_CAR_DISCOUNTS, COL_PRODUCED_TO);

    if (Objects.nonNull(prodDate)) {
      clause = SqlUtils.or(clause,
          SqlUtils.more(TBL_CAR_DISCOUNTS, COL_PRODUCED_TO, prodDate));
    }
    carDiscountWhere.add(clause);

    return carDiscountWhere;
  }

  private List<Long> getDiscountParents(Long company) {
    List<Long> result = new ArrayList<>();

    Long child = company;
    String idName = sys.getIdName(TBL_COMPANIES);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_COMPANIES, COL_DISCOUNT_PARENT)
        .addFrom(TBL_COMPANIES);

    while (DataUtils.isId(child)) {
      query.setWhere(SqlUtils.equals(TBL_COMPANIES, idName, child));
      Long parent = qs.getLong(query);

      if (DataUtils.isId(parent) && !Objects.equals(company, parent) && !result.contains(parent)) {
        result.add(parent);
        child = parent;
      } else {
        child = null;
        break;
      }
    }

    return result;
  }

  private Map<Long, Long> getItemCategories(long item) {
    Map<Long, Long> result = new HashMap<>();

    Set<Long> categories = new HashSet<>();

    SqlSelect itemQuery = new SqlSelect()
        .addFields(TBL_ITEMS, COL_ITEM_TYPE, COL_ITEM_GROUP)
        .addFrom(TBL_ITEMS)
        .setWhere(sys.idEquals(TBL_ITEMS, item));

    SimpleRowSet itemData = qs.getData(itemQuery);
    if (!DataUtils.isEmpty(itemData)) {
      Long type = itemData.getLong(0, COL_ITEM_TYPE);
      if (DataUtils.isId(type)) {
        categories.add(type);
      }

      Long group = itemData.getLong(0, COL_ITEM_GROUP);
      if (DataUtils.isId(group)) {
        categories.add(group);
      }
    }

    SqlSelect icQuery = new SqlSelect()
        .addFields(TBL_ITEM_CATEGORIES, COL_CATEGORY)
        .addFrom(TBL_ITEM_CATEGORIES)
        .setWhere(SqlUtils.equals(TBL_ITEM_CATEGORIES, COL_ITEM, item));

    categories.addAll(qs.getLongSet(icQuery));

    if (!categories.isEmpty()) {
      Map<Long, Long> parents = new HashMap<>();

      String idName = sys.getIdName(TBL_ITEM_CATEGORY_TREE);
      SqlSelect treeQuery = new SqlSelect()
          .addFields(TBL_ITEM_CATEGORY_TREE, idName, COL_CATEGORY_PARENT)
          .addFrom(TBL_ITEM_CATEGORY_TREE)
          .setWhere(SqlUtils.notNull(TBL_ITEM_CATEGORY_TREE, COL_CATEGORY_PARENT));

      SimpleRowSet treeData = qs.getData(treeQuery);
      if (!DataUtils.isEmpty(treeData)) {
        for (SimpleRow row : treeData) {
          parents.put(row.getLong(idName), row.getLong(COL_CATEGORY_PARENT));
        }
      }

      for (Long category : categories) {
        Long c = category;

        while (c != null && !result.containsKey(c)) {
          Long p = parents.get(c);
          result.put(c, p);

          c = p;
        }
      }
    }

    return result;
  }

  private EnumMap<ItemPrice, Double> getItemPrices(long item, Long currency, long time) {
    EnumMap<ItemPrice, Double> prices = new EnumMap<>(ItemPrice.class);

    SqlSelect query = new SqlSelect();
    for (ItemPrice ip : ItemPrice.values()) {
      query.addFields(TBL_ITEMS, ip.getPriceColumn(), ip.getCurrencyColumn());
    }
    query.addFrom(TBL_ITEMS).setWhere(sys.idEquals(TBL_ITEMS, item));

    SimpleRow row = qs.getRow(query);
    if (row != null) {
      double toRate = getRate(currency, time);

      for (ItemPrice ip : ItemPrice.values()) {
        Double price = row.getDouble(ip.getPriceColumn());

        if (BeeUtils.isPositive(price)) {
          Long c = row.getLong(ip.getCurrencyColumn());
          if (!Objects.equals(currency, c)) {
            double fromRate = getRate(c, time);
            price = Localized.normalizeMoney(price * fromRate / toRate);
          }

          if (BeeUtils.isPositive(price)) {
            prices.put(ip, price);
          }
        }
      }
    }

    return prices;
  }

  private double getRate(Long currency, long time) {
    return DataUtils.isId(currency) ? adm.getRate(currency, time) : BeeConst.DOUBLE_ONE;
  }

  private Map<Long, Integer> getRemindActionsUserSettings() {
    Map<Long, Integer> userSettings = Maps.newHashMap();
    Filter timeIsSet = Filter.isPositive(COL_REMIND_ACTION_BEFORE);

    BeeRowSet rows = qs.getViewData(VIEW_USER_SETTINGS, timeIsSet);

    for (IsRow row : rows) {
      Long userId = row.getLong(rows.getColumnIndex(COL_USER));
      Integer parameter = row.getInteger(rows.getColumnIndex(COL_REMIND_ACTION_BEFORE));
      if (!usr.isBlocked(usr.getUserName(userId))
          && !BeeUtils.isEmpty(usr.getUserEmail(userId, false))) {
        userSettings.put(userId, parameter);
      }
    }

    return userSettings;
  }

  private Map<Long, Double> getRemainders(List<Long> ids, String source, String field) {

    Map<Long, Double> remainders = new HashMap<>();
    Map<Long, Double> invoices = new HashMap<>();

    if (!BeeUtils.isEmpty(ids)) {
      SqlSelect select = new SqlSelect()
          .addFields(source, COL_ITEM)
          .addSum(source, field)
          .addFrom(source)
          .setWhere(SqlUtils.inList(source, COL_ITEM, ids))
          .addGroup(source, COL_ITEM);

      if (Objects.equals(source, TBL_ORDER_ITEMS)) {
        SqlSelect slcInvoices =
            new SqlSelect()
                .addFields(TBL_SALE_ITEMS, COL_ITEM)
                .addSum(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY)
                .addFrom(TBL_SALE_ITEMS)
                .addFromLeft(TBL_SALES, sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE))
                .setWhere(
                    SqlUtils.and(SqlUtils.inList(TBL_SALE_ITEMS, COL_ITEM, ids), SqlUtils.isNull(
                        TBL_SALES, COL_TRADE_EXPORTED)))
                .addGroup(TBL_SALE_ITEMS, COL_ITEM);

        for (SimpleRow row : qs.getData(slcInvoices)) {
          invoices.put(row.getLong(COL_ITEM), BeeUtils
              .unbox(row.getDouble(COL_TRADE_ITEM_QUANTITY)));
        }

        for (SimpleRow row : qs.getData(select)) {
          if (invoices.containsKey(row.getLong(COL_ITEM))) {
            remainders.put(row.getLong(COL_ITEM), BeeUtils.unbox(row.getDouble(field))
                + BeeUtils.unbox(invoices.get(row.getLong(COL_ITEM))));
          } else {
            remainders.put(row.getLong(COL_ITEM), BeeUtils.unbox(row.getDouble(field)));
          }
        }
      } else {
        for (SimpleRow row : qs.getData(select)) {
          remainders.put(row.getLong(COL_ITEM), BeeUtils.unbox(row.getDouble(field)));
        }
      }
    }

    return remainders;
  }

  private ResponseObject getStocks(RequestInfo reqInfo) {

  	Long itemId = reqInfo.getParameterLong(COL_ITEM);

  	if (!DataUtils.isId(itemId)) {
  		return ResponseObject.parameterNotFound(SVC_GET_STOCKS_IN_WAREHOUSE, COL_ITEM);
  	}

  	SqlSelect selectAddedStocks = new SqlSelect()
  		.addSum(TBL_ADDITION_STOCKS, COL_TRADE_ITEM_QUANTITY)
  		.addFrom(TBL_ADDITION_STOCKS)
		.setWhere(SqlUtils.equals(TBL_ADDITION_STOCKS, COL_ITEM, itemId));

	  SqlSelect selectWriteOffStocks = new SqlSelect()
		  .addSum(TBL_WRITE_OFF_STOCKS, COL_TRADE_ITEM_QUANTITY)
		  .addFrom(TBL_WRITE_OFF_STOCKS)
		  .setWhere(SqlUtils.equals(TBL_WRITE_OFF_STOCKS, COL_ITEM, itemId));

  	SimpleRowSet addedStocks = qs.getData(selectAddedStocks);
  	SimpleRowSet writeOffStocks = qs.getData(selectWriteOffStocks);

  	Integer addedStocksSum = addedStocks.getInt(0, 0);
  	Integer writeOffStocksSum = writeOffStocks.getInt(0, 0);

  	Integer result = (addedStocksSum == null ? 0 : addedStocksSum) - (writeOffStocksSum == null ? 0 : writeOffStocksSum);

  	return ResponseObject.response(result);
  }

  private ResponseObject getReservation(RequestInfo reqInfo) {
    Arrays.asList(COL_DATES_START_DATE,
        ALS_COMPANY_NAME, ALS_MANAGER_FIRST_NAME, ALS_MANAGER_LAST_NAME, ALS_WAREHOUSE_CODE);

    Long itemId = reqInfo.getParameterLong(COL_ITEM);
    if (!DataUtils.isId(itemId)) {
      return ResponseObject.parameterNotFound(SVC_GET_RESERVATION, COL_ITEM);
    }

    SqlSelect objectsSql = new SqlSelect()
        .addField(TBL_ORDERS, sys.getIdName(TBL_ORDERS), COL_ORDER)
        .addEmptyLong(COL_SERVICE_MAINTENANCE)
        .addFields(TBL_ORDERS, COL_DATES_START_DATE)
        .addField(VIEW_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addFields(VIEW_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addField(VIEW_WAREHOUSES, COL_WAREHOUSE_CODE, ALS_WAREHOUSE_CODE)
        .addFrom(TBL_ORDERS)
        .addFromLeft(VIEW_COMPANIES, sys.joinTables(VIEW_COMPANIES, TBL_ORDERS, COL_COMPANY))
        .addFromLeft(VIEW_COMPANY_PERSONS,
            sys.joinTables(VIEW_COMPANY_PERSONS, TBL_ORDERS, COL_TRADE_MANAGER))
        .addFromLeft(VIEW_PERSONS, sys.joinTables(VIEW_PERSONS, VIEW_COMPANY_PERSONS, COL_PERSON))
        .addFromLeft(VIEW_WAREHOUSES, sys.joinTables(VIEW_WAREHOUSES, TBL_ORDERS, COL_WAREHOUSE))
        .setWhere(SqlUtils.and(SqlUtils.in(TBL_ORDERS, sys.getIdName(TBL_ORDERS),
            VIEW_ORDER_ITEMS, COL_ORDER, SqlUtils.equals(VIEW_ORDER_ITEMS, COL_ITEM, itemId)),
            SqlUtils.equals(TBL_ORDERS, COL_ORDERS_STATUS, OrdersStatus.APPROVED)));

    objectsSql.addUnion(new SqlSelect()
        .addEmptyLong(COL_ORDER)
        .addField(TBL_SERVICE_MAINTENANCE,
            sys.getIdName(TBL_SERVICE_MAINTENANCE), COL_SERVICE_MAINTENANCE)
        .addField(TBL_SERVICE_MAINTENANCE, COL_TRADE_DATE, COL_DATES_START_DATE)
        .addField(VIEW_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addFields(VIEW_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addField(VIEW_WAREHOUSES, COL_WAREHOUSE_CODE, ALS_WAREHOUSE_CODE)
        .addFrom(TBL_SERVICE_MAINTENANCE)
        .addFromLeft(VIEW_COMPANIES,
            sys.joinTables(VIEW_COMPANIES, TBL_SERVICE_MAINTENANCE, COL_COMPANY))
        .addFromLeft(VIEW_USERS,
            sys.joinTables(VIEW_USERS, TBL_SERVICE_MAINTENANCE, COL_REPAIRER))
        .addFromLeft(VIEW_COMPANY_PERSONS,
            sys.joinTables(VIEW_COMPANY_PERSONS, VIEW_USERS, COL_COMPANY_PERSON))
        .addFromLeft(VIEW_PERSONS, sys.joinTables(VIEW_PERSONS, VIEW_COMPANY_PERSONS, COL_PERSON))
        .addFromLeft(VIEW_WAREHOUSES,
            sys.joinTables(VIEW_WAREHOUSES, TBL_SERVICE_MAINTENANCE, COL_WAREHOUSE))
        .setWhere(SqlUtils.and(SqlUtils.isNull(TBL_SERVICE_MAINTENANCE, COL_ENDING_DATE),
            SqlUtils.in(TBL_SERVICE_MAINTENANCE, sys.getIdName(TBL_SERVICE_MAINTENANCE),
                TBL_SERVICE_ITEMS, COL_SERVICE_MAINTENANCE,
                SqlUtils.in(TBL_SERVICE_ITEMS, sys.getIdName(TBL_SERVICE_ITEMS), VIEW_ORDER_ITEMS,
                    COL_SERVICE_ITEM, SqlUtils.equals(VIEW_ORDER_ITEMS, COL_ITEM, itemId)))))
    );

    SimpleRowSet rqs = qs.getData(objectsSql);

    return ResponseObject.response(rqs);
  }

  @Schedule(hour = "*/1", persistent = false)
  private void notifyCompanyActions() {
    logger.debug("Timer", TIMER_REMIND_COMPANY_ACTIONS, "started");

    if (!DataUtils.isId(mail.getSenderAccountId(TIMER_REMIND_COMPANY_ACTIONS))) {
      return;
    }

    Map<Long, Integer> userSettings = getRemindActionsUserSettings();

    for (Long user : userSettings.keySet()) {
      logger.debug("try to remind user", user);
      sendCompanyActionsReminder(user, userSettings.get(user));
    }

    logger.debug("Timer", TIMER_REMIND_COMPANY_ACTIONS, "ended");
  }

  private void sendCompanyActionsReminder(Long user, Integer remindBefore) {
    if (!DataUtils.isId(user) || !BeeUtils.isPositive(remindBefore)) {
      return;
    }

    IsCondition filter = getCompanyActionsFilter(user, remindBefore);

    SqlSelect appointmentsQuery = new SqlSelect()
        .addFields(CalendarConstants.TBL_APPOINTMENTS,
            sys.getIdName(CalendarConstants.TBL_APPOINTMENTS),
            CalendarConstants.COL_CREATED, CalendarConstants.COL_SUMMARY,
            CalendarConstants.COL_STATUS,
            CalendarConstants.COL_START_DATE_TIME, CalendarConstants.COL_END_DATE_TIME)
        .addField(CalendarConstants.TBL_APPOINTMENT_TYPES,
            CalendarConstants.COL_APPOINTMENT_TYPE_NAME,
            CalendarConstants.ALS_APPOINTMENT_TYPE_NAME)
        .addExpr(SqlUtils.concat(
            SqlUtils.nvl(SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME), "''"), "' '",
            SqlUtils.nvl(SqlUtils.field(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME), "''")),
            ALS_COMPANY_NAME)
        .addExpr(SqlUtils.concat(
            SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "''"), "' '",
            SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")),
            ALS_CONTACT_PERSON)
        .addFrom(CalendarConstants.TBL_APPOINTMENTS)
        .addFromLeft(CalendarConstants.TBL_APPOINTMENT_TYPES,
            sys.joinTables(CalendarConstants.TBL_APPOINTMENT_TYPES,
                CalendarConstants.TBL_APPOINTMENTS,
                CalendarConstants.COL_APPOINTMENT_TYPE))
        .addFromLeft(TBL_COMPANIES,
            sys.joinTables(TBL_COMPANIES, CalendarConstants.TBL_APPOINTMENTS, COL_COMPANY))
        .addFromLeft(TBL_COMPANY_TYPES,
            sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, CalendarConstants.TBL_APPOINTMENTS,
                COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .setWhere(filter)
        .addOrder(CalendarConstants.TBL_APPOINTMENTS, CalendarConstants.COL_START_DATE_TIME);

    SimpleRowSet appointments = qs.getData(appointmentsQuery);

    if (appointments.getNumberOfRows() > 0) {
      createActionRemindMail(user, appointments);
    } else {
      logger.info("no actions remind for user", user);
    }
  }

  public void setRemindedAppointments(List<Long> appointments) {
    if (BeeUtils.isEmpty(appointments)) {
      return;
    }

    SqlUpdate update = new SqlUpdate(CalendarConstants.TBL_APPOINTMENTS);
    update.addConstant(CalendarConstants.COL_ACTION_REMINDED, Boolean.TRUE);
    update.setWhere(SqlUtils.inList(CalendarConstants.TBL_APPOINTMENTS,
        sys.getIdName(CalendarConstants.TBL_APPOINTMENTS), appointments));
    qs.updateData(update);
  }

  public ResponseObject generateQrCode(RequestInfo reqInfo) throws WriterException, IOException {
    final String qrBase64;

    String qrCodeText = "";
    Map<String, String> values = new HashMap<>();
    String[] qrValues = null;
    String[] keys = null;
    String mobile = reqInfo.getParameter(COL_MOBILE);
    String email = reqInfo.getParameter(COL_EMAIL);
    String address = reqInfo.getParameter(COL_ADDRESS);
    String type = reqInfo.getParameter(QR_TYPE);

    if (type.equals(QR_COMPANY)) {
      qrValues = new String[] {"MECARD:N:", "TEL:", "EMAIL:", "ADR:"};
      keys = new String[] {"name", "mobile", "email", "address"};
      String companyName = reqInfo.getParameter(COL_COMPANY_NAME);
      values.put("name", companyName);
      values.put("mobile", mobile);
      values.put("email", email);
      values.put("address", address);
    } else if (type.equals(QR_PERSON)) {
      qrValues = new String[] {"MECARD:N:", "TEL:", "EMAIL:", "ADR:"};
      keys = new String[] {"userName", "mobile", "email", "address"};
      String userName = reqInfo.getParameter(COL_FIRST_NAME);
      String userLastName = reqInfo.getParameter(COL_LAST_NAME);
      if (!BeeUtils.isEmpty(userLastName)) {
        values.put("userName", userLastName + "," + userName);
      }
      values.put("userName", userName);
      values.put("mobile", mobile);
      values.put("email", email);
      values.put("address", address);
    }

    if (!values.isEmpty()) {
      for (int i = 0; i < values.size(); i++) {
        if (values.get(keys[i]) != null) {
          qrCodeText += qrValues[i] + values.get(keys[i]) + ";";
        }
      }
    }

    qrBase64 = qrCodeGenerator(qrCodeText);
    return ResponseObject.response(qrBase64);
  }

  public String qrCodeGenerator(String qrCodeText) throws WriterException, IOException {
    final String qrBase64;
    int size = 500;
    String fileType = "png";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    hintMap.put(EncodeHintType.CHARACTER_SET, "utf-8");

    ByteMatrix byteMatrix =
        qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
    int matrixWidth = byteMatrix.getWidth();
    BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
    image.createGraphics();

    Graphics2D graphics = (Graphics2D) image.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, matrixWidth, matrixWidth);
    graphics.setColor(Color.BLACK);

    for (int i = 0; i < matrixWidth; i++) {
      for (int j = 0; j < matrixWidth; j++) {
        if (byteMatrix.get(i, j) != -1) {
          graphics.fillRect(i, j, 1, 1);
        }
      }
    }

    ImageIO.write(image, fileType, baos);
    baos.flush();
    byte[] imageInByte = baos.toByteArray();
    baos.close();
    qrBase64 = Codec.toBase64(imageInByte);
    return qrBase64;
  }

  public DateTime calculateReminderTime(Long time, Integer dataIndicator,
      Integer hours, Integer minutes) {

    DateTime reminderTime = new DateTime();
    reminderTime.setTime(time);

    if (time != null && dataIndicator != null) {
      if (BeeUtils.same(dataIndicator.toString(),
          BeeUtils.toString(ReminderDateIndicator.AFTER.ordinal()))) {
        if (hours != null) {
          TimeUtils.addHour(reminderTime, hours);
        }
        if (minutes != null) {
          TimeUtils.addMinute(reminderTime, minutes);
        }
      }
      if (BeeUtils.same(dataIndicator.toString(),
          BeeUtils.toString(ReminderDateIndicator.BEFORE.ordinal()))) {
        if (hours != null) {
          TimeUtils.addHour(reminderTime, hours * -1);
        }
        if (minutes != null) {
          TimeUtils.addMinute(reminderTime, minutes * -1);
        }
      }
    }
    return reminderTime;
  }
}
