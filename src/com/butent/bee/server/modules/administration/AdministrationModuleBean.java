package com.butent.bee.server.modules.administration;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.concurrency.ConcurrencyBean.HasTimerService;
import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.TableModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.I18nUtils;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.ibm.icu.text.RuleBasedNumberFormat;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import lt.lb.webservices.exchangerates.ExchangeRatesWS;

@Stateless
@LocalBean
public class AdministrationModuleBean implements BeeModule, HasTimerService {

  private static BeeLogger logger = LogUtils.getLogger(AdministrationModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  DataEditorBean deb;
  @EJB
  QueryServiceBean qs;
  @EJB
  ParamHolderBean prm;
  @EJB
  ImportBean imp;
  @EJB
  ConcurrencyBean cb;

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> usersSr = qs.getSearchResults(VIEW_USERS,
        Filter.anyContains(Sets.newHashSet(COL_LOGIN, COL_FIRST_NAME, COL_LAST_NAME), query));
    return usersSr;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.isPrefix(svc, PARAMETERS_PREFIX)) {
      response = prm.doService(svc, reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_HISTORY)) {
      response = getHistory(reqInfo.getParameter(VAR_HISTORY_VIEW),
          DataUtils.parseIdSet(reqInfo.getParameter(VAR_HISTORY_IDS)));

    } else if (BeeUtils.same(svc, SVC_UPDATE_EXCHANGE_RATES)) {
      response = updateExchangeRates(reqInfo.getParameter(VAR_DATE_LOW),
          reqInfo.getParameter(VAR_DATE_HIGH));

    } else if (BeeUtils.same(svc, SVC_GET_LIST_OF_CURRENCIES)) {
      response = getListOfCurrencies();
    } else if (BeeUtils.same(svc, SVC_GET_CURRENT_EXCHANGE_RATE)) {
      response = getCurrentExchangeRate(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_EXCHANGE_RATE)) {
      response = getExchangeRate(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_EXCHANGE_RATES_FOR_CURRENCY)) {
      response = getExchangeRatesForCurrency(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CREATE_USER)) {
      response = createUser(reqInfo);
    } else if (BeeUtils.same(svc, SVC_BLOCK_HOST)) {
      response = blockHost(reqInfo);

    } else if (BeeUtils.same(svc, SVC_COPY_RIGHTS)) {
      response = copyRights(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ROLE)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_BASE_ROLE)));

    } else if (BeeUtils.same(svc, SVC_NUMBER_TO_WORDS)) {
      response = getNumberInWords(BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_AMOUNT)),
          reqInfo.getParameter(VAR_LOCALE));

    } else if (BeeUtils.same(svc, SVC_DO_IMPORT)) {
      response = imp.doImport(reqInfo);

    } else {
      String msg = BeeUtils.joinWords("Commons service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public void ejbTimeout(Timer timer) {
    if (cb.isParameterTimer(timer, PRM_REFRESH_CURRENCY_HOURS)) {
      refreshCurrencyRates();
    }
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createMap(module, PRM_SERVER_PROPERTIES),
        BeeParameter.createRelation(module, PRM_COMPANY, TBL_COMPANIES, COL_COMPANY_NAME),
        BeeParameter.createRelation(module, PRM_COUNTRY, TBL_COUNTRIES, COL_COUNTRY_NAME),
        BeeParameter.createRelation(module, PRM_CURRENCY, TBL_CURRENCIES, COL_CURRENCY_NAME),
        BeeParameter.createNumber(module, PRM_VAT_PERCENT, false, 21),
        BeeParameter.createText(module, PRM_REFRESH_CURRENCY_HOURS),
        BeeParameter.createText(module, PRM_ERP_ADDRESS),
        BeeParameter.createText(module, PRM_ERP_LOGIN),
        BeeParameter.createText(module, PRM_ERP_PASSWORD),
        BeeParameter.createText(module, PRM_URL));

    params.addAll(getSqlEngineParameters());
    return params;
  }

  @Override
  public Module getModule() {
    return Module.ADMINISTRATION;
  }

  public double getRate(long currency, long time) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_CURRENCY_RATES,
            COL_CURRENCY_RATE_DATE, COL_CURRENCY_RATE_QUANTITY, COL_CURRENCY_RATE)
        .addFrom(TBL_CURRENCY_RATES)
        .setWhere(SqlUtils.and(
            SqlUtils.equals(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_CURRENCY, currency),
            SqlUtils.lessEqual(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE, time)))
        .addOrderDesc(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE)
        .setLimit(1);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return BeeConst.DOUBLE_ONE;

    } else {
      double rate = data.getDouble(0, COL_CURRENCY_RATE);

      Integer quantity = data.getInt(0, COL_CURRENCY_RATE_QUANTITY);
      if (quantity != null && quantity > 1) {
        rate /= quantity;
      }

      return rate;
    }
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
    cb.createCalendarTimer(this.getClass(), PRM_REFRESH_CURRENCY_HOURS);

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void normalizeFileReference(TableModifyEvent event) {
        if (event.isBefore()) {
          if (event.getQuery() instanceof SqlInsert) {
            sys.normalizeFileReference((SqlInsert) event.getQuery());

          } else if (event.getQuery() instanceof SqlUpdate) {
            sys.normalizeFileReference((SqlUpdate) event.getQuery());
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void orderDepartments(ViewQueryEvent event) {
        if (event.isAfter(VIEW_DEPARTMENTS) && event.hasData()) {
          String idName = sys.getIdName(TBL_DEPARTMENTS);

          SqlSelect query = new SqlSelect()
              .addFields(TBL_DEPARTMENTS, idName, COL_DEPARTMENT_NAME, COL_DEPARTMENT_PARENT)
              .addFrom(TBL_DEPARTMENTS);

          SimpleRowSet data = qs.getData(query);
          if (DataUtils.isEmpty(data)) {
            return;
          }

          Map<Long, Long> parents = new HashMap<>();
          Map<Long, String> names = new HashMap<>();

          for (SimpleRow row : data) {
            Long id = row.getLong(idName);

            Long parent = row.getLong(COL_DEPARTMENT_PARENT);
            if (DataUtils.isId(parent) && !Objects.equals(id, parent)) {
              parents.put(id, parent);
            }

            names.put(id, BeeUtils.trim(row.getValue(COL_DEPARTMENT_NAME)));
          }

          BeeRowSet rowSet = event.getRowset();
          final int nameIndex = rowSet.getColumnIndex(COL_DEPARTMENT_NAME);

          String fullName;

          for (BeeRow row : rowSet) {
            if (parents.containsKey(row.getId())) {
              List<Long> branch = new ArrayList<>();
              branch.add(row.getId());

              Long parent = parents.get(row.getId());
              while (parent != null && !branch.contains(parent)) {
                branch.add(parent);
                parent = parents.get(parent);
              }

              StringBuilder sb = new StringBuilder();
              for (int i = branch.size() - 1; i >= 0; i--) {
                sb.append(names.get(branch.get(i)));
                if (i > 0) {
                  sb.append(DEPARTMENT_NAME_SEPARATOR);
                }
              }

              fullName = sb.toString();

            } else {
              fullName = row.getString(nameIndex);
            }

            row.setProperty(PROP_DEPARTMENT_FULL_NAME, fullName);
          }

          if (rowSet.getNumberOfRows() > 1) {
            final Collator collator = Collator.getInstance(usr.getLocale());
            collator.setStrength(Collator.IDENTICAL);

            Collections.sort(rowSet.getRows(), new Comparator<BeeRow>() {
              @Override
              public int compare(BeeRow row1, BeeRow row2) {
                String name1 = row1.getProperty(PROP_DEPARTMENT_FULL_NAME);
                if (BeeUtils.isEmpty(name1)) {
                  name1 = row1.getString(nameIndex);
                }

                String name2 = row2.getProperty(PROP_DEPARTMENT_FULL_NAME);
                if (BeeUtils.isEmpty(name2)) {
                  name2 = row2.getString(nameIndex);
                }

                int result = collator.compare(BeeUtils.normalize(name1), BeeUtils.normalize(name2));
                if (result == BeeConst.COMPARE_EQUAL) {
                  result = Long.compare(row1.getId(), row2.getId());
                }

                return result;
              }
            });
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void refreshIpFilterCache(TableModifyEvent event) {
        if (event.isAfter(TBL_IP_FILTERS)) {
          usr.initIpFilters();
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void refreshUsersCache(TableModifyEvent event) {
        if (event.isAfter(TBL_USERS, TBL_ROLES, TBL_USER_ROLES)) {
          usr.initUsers();
          Endpoint.updateUserData(usr.getAllUserData());
        }
      }
    });
  }

  public Double maybeExchange(Long from, Long to, Double v, DateTime dt) {
    if (from == null || to == null || Objects.equals(from, to) || !BeeUtils.nonZero(v)) {
      return v;

    } else {
      long time = (dt == null) ? System.currentTimeMillis() : dt.getTime();
      return v * getRate(from, time) / getRate(to, time);
    }
  }

  private ResponseObject blockHost(RequestInfo reqInfo) {
    String host = BeeUtils.trim(reqInfo.getParameter(COL_IP_FILTER_HOST));
    if (BeeUtils.isEmpty(host)) {
      return ResponseObject.parameterNotFound(SVC_BLOCK_HOST, COL_IP_FILTER_HOST);
    }

    if (qs.sqlExists(TBL_IP_FILTERS,
        SqlUtils.and(SqlUtils.equals(TBL_IP_FILTERS, COL_IP_FILTER_HOST, host),
            SqlUtils.isNull(TBL_IP_FILTERS, COL_IP_FILTER_BLOCK_AFTER),
            SqlUtils.isNull(TBL_IP_FILTERS, COL_IP_FILTER_BLOCK_BEFORE)))) {
      return ResponseObject.response(host);
    }

    SqlInsert insert = new SqlInsert(TBL_IP_FILTERS).addConstant(COL_IP_FILTER_HOST, host);
    ResponseObject response = qs.insertDataWithResponse(insert);

    if (response.hasErrors()) {
      return response;
    } else {
      return ResponseObject.response(host);
    }
  }

  private ResponseObject createUser(RequestInfo reqInfo) {
    String login = reqInfo.getParameter(COL_LOGIN);
    if (BeeUtils.isEmpty(login)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, COL_LOGIN);
    }

    String password = reqInfo.getParameter(COL_PASSWORD);
    if (BeeUtils.isEmpty(password)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, COL_PASSWORD);
    }

    if (usr.isUser(login)) {
      return ResponseObject.warning(usr.getLocalizableMesssages()
          .valueExists(BeeUtils.joinWords(usr.getLocalizableConstants().user(), login)));
    }

    String email = reqInfo.getParameter(COL_EMAIL);
    if (!BeeUtils.isEmpty(email) && qs.sqlExists(TBL_EMAILS, COL_EMAIL_ADDRESS, email)) {
      return ResponseObject.warning(usr.getLocalizableMesssages()
          .valueExists(BeeUtils.joinWords(usr.getLocalizableConstants().email(), email)));
    }

    UserInterface userInterface = EnumUtils.getEnumByIndex(UserInterface.class,
        BeeUtils.toIntOrNull(reqInfo.getParameter(COL_USER_INTERFACE)));

    String companyName = BeeUtils.notEmpty(reqInfo.getParameter(ALS_COMPANY_NAME), login);
    String companyCode = reqInfo.getParameter(ALS_COMPANY_CODE);
    String vatCode = reqInfo.getParameter(COL_COMPANY_VAT_CODE);
    String exchangeCode = reqInfo.getParameter(COL_COMPANY_EXCHANGE_CODE);

    String firstName = BeeUtils.notEmpty(reqInfo.getParameter(COL_FIRST_NAME), login);
    String lastName = reqInfo.getParameter(COL_LAST_NAME);

    String positionName = reqInfo.getParameter(COL_POSITION);

    String address = reqInfo.getParameter(COL_ADDRESS);
    String postIndex = reqInfo.getParameter(COL_POST_INDEX);

    String cityName = reqInfo.getParameter(COL_CITY);
    String countryName = reqInfo.getParameter(COL_COUNTRY);

    String phone = reqInfo.getParameter(COL_PHONE);
    String mobile = reqInfo.getParameter(COL_MOBILE);
    String fax = reqInfo.getParameter(COL_FAX);

    ResponseObject response;

    Long company = qs.getId(TBL_COMPANIES, COL_COMPANY_NAME, companyName);
    if (company == null && !BeeUtils.isEmpty(companyCode)) {
      company = qs.getId(TBL_COMPANIES, COL_COMPANY_CODE, companyCode);
    }

    if (company == null) {
      SqlInsert insCompany = new SqlInsert(TBL_COMPANIES)
          .addConstant(COL_COMPANY_NAME, companyName)
          .addNotEmpty(COL_COMPANY_CODE, companyCode)
          .addNotEmpty(COL_COMPANY_VAT_CODE, vatCode)
          .addNotEmpty(COL_COMPANY_EXCHANGE_CODE, exchangeCode);

      response = qs.insertDataWithResponse(insCompany);
      if (response.hasErrors()) {
        return response;
      }
      company = response.getResponseAsLong();
    }

    SqlInsert insPerson = new SqlInsert(TBL_PERSONS)
        .addConstant(COL_FIRST_NAME, firstName)
        .addNotEmpty(COL_LAST_NAME, lastName);

    response = qs.insertDataWithResponse(insPerson);
    if (response.hasErrors()) {
      return response;
    }
    Long person = response.getResponseAsLong();

    List<BeeColumn> cpColumns = sys.getView(VIEW_COMPANY_PERSONS).getRowSetColumns();
    BeeRow cpRow = DataUtils.createEmptyRow(cpColumns.size());

    cpRow.setValue(DataUtils.getColumnIndex(COL_COMPANY, cpColumns), company);
    cpRow.setValue(DataUtils.getColumnIndex(COL_PERSON, cpColumns), person);

    if (!BeeUtils.isEmpty(email)) {
      response = qs.insertDataWithResponse(new SqlInsert(TBL_EMAILS)
          .addConstant(COL_EMAIL_ADDRESS, email));

      if (response.hasErrors()) {
        return response;
      }
      cpRow.setValue(DataUtils.getColumnIndex(ALS_EMAIL_ID, cpColumns),
          response.getResponseAsLong());
    }

    if (!BeeUtils.isEmpty(positionName)) {
      Long position = qs.getId(TBL_POSITIONS, COL_POSITION_NAME, positionName);

      if (position == null) {
        SqlInsert insPosition = new SqlInsert(TBL_POSITIONS)
            .addConstant(COL_POSITION_NAME, positionName);

        response = qs.insertDataWithResponse(insPosition);
        if (response.hasErrors()) {
          return response;
        }
        position = response.getResponseAsLong();
      }

      if (position != null) {
        cpRow.setValue(DataUtils.getColumnIndex(COL_POSITION, cpColumns), position);
      }
    }
    Long country = null;

    if (!BeeUtils.isEmpty(countryName)) {
      country = qs.getId(TBL_COUNTRIES, COL_COUNTRY_NAME, countryName);

      if (!DataUtils.isId(country)) {
        country = qs.insertData(new SqlInsert(TBL_COUNTRIES)
            .addConstant(COL_COUNTRY_NAME, countryName));
      }
      cpRow.setValue(DataUtils.getColumnIndex(COL_COUNTRY, cpColumns), country);
    }

    if (!BeeUtils.isEmpty(cityName)) {
      Long city;

      if (DataUtils.isId(country)) {
        city = qs.getId(TBL_CITIES, COL_CITY_NAME, cityName, COL_COUNTRY, country);

        if (!DataUtils.isId(city)) {
          city = qs.insertData(new SqlInsert(TBL_CITIES)
              .addConstant(COL_CITY_NAME, cityName)
              .addConstant(COL_COUNTRY, country));
        }
      } else {
        city = qs.getId(TBL_CITIES, COL_CITY_NAME, cityName);
      }
      if (DataUtils.isId(city)) {
        cpRow.setValue(DataUtils.getColumnIndex(COL_CITY, cpColumns), city);
      }
    }

    if (!BeeUtils.isEmpty(address)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_ADDRESS, cpColumns), address);
    }
    if (!BeeUtils.isEmpty(postIndex)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_POST_INDEX, cpColumns), postIndex);
    }

    if (!BeeUtils.isEmpty(phone)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_PHONE, cpColumns), phone);
    }
    if (!BeeUtils.isEmpty(mobile)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_MOBILE, cpColumns), mobile);
    }
    if (!BeeUtils.isEmpty(fax)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_FAX, cpColumns), fax);
    }

    BeeRowSet cpRowSet = DataUtils.createRowSetForInsert(VIEW_COMPANY_PERSONS, cpColumns, cpRow);
    response = deb.commitRow(cpRowSet);
    if (response.hasErrors()) {
      return response;
    }

    Long companyPerson = ((BeeRow) response.getResponse()).getId();

    SqlInsert insUser = new SqlInsert(TBL_USERS)
        .addConstant(COL_LOGIN, login)
        .addConstant(COL_PASSWORD, password)
        .addConstant(COL_COMPANY_PERSON, companyPerson);
    if (userInterface != null) {
      insUser.addConstant(COL_USER_INTERFACE, userInterface.ordinal());
    }

    return qs.insertDataWithResponse(insUser);
  }

  private ResponseObject copyRights(Long role, Long baseRole) {
    Assert.state(DataUtils.isId(role));
    Assert.state(DataUtils.isId(baseRole));

    for (SimpleRow row : qs.getData(new SqlSelect()
        .addFields(TBL_RIGHTS, COL_OBJECT, COL_STATE)
        .addFrom(TBL_RIGHTS)
        .setWhere(SqlUtils.equals(TBL_RIGHTS, COL_ROLE, baseRole)))) {

      qs.insertData(new SqlInsert(TBL_RIGHTS)
          .addConstant(COL_ROLE, role)
          .addConstant(COL_OBJECT, row.getLong(COL_OBJECT))
          .addConstant(COL_STATE, row.getInt(COL_STATE)));
    }
    usr.initRights();
    return ResponseObject.emptyResponse();
  }

  private ResponseObject getCurrentExchangeRate(RequestInfo reqInfo) {
    String type = reqInfo.getParameter(Service.VAR_TYPE);
    String currency = reqInfo.getParameter(COL_CURRENCY_NAME);

    String address = getExchangeRatesRemoteAddress();

    return ExchangeRatesWS.getCurrentExchangeRates(address, type, currency);
  }

  private ResponseObject getExchangeRate(RequestInfo reqInfo) {
    String type = reqInfo.getParameter(Service.VAR_TYPE);

    String currency = reqInfo.getParameter(COL_CURRENCY_NAME);
    JustDate date = TimeUtils.parseDate(reqInfo.getParameter(COL_CURRENCY_RATE_DATE));

    String address = getExchangeRatesRemoteAddress();

    return ExchangeRatesWS.getExchangeRate(address, type, currency, date);
  }

  private ResponseObject getExchangeRatesForCurrency(RequestInfo reqInfo) {
    String type = reqInfo.getParameter(Service.VAR_TYPE);

    String currency = reqInfo.getParameter(COL_CURRENCY_NAME);

    JustDate dateLow = TimeUtils.parseDate(reqInfo.getParameter(VAR_DATE_LOW));
    JustDate dateHigh = TimeUtils.parseDate(reqInfo.getParameter(VAR_DATE_HIGH));

    String address = getExchangeRatesRemoteAddress();

    return ExchangeRatesWS.getExchangeRatesForCurrency(address, type, currency, dateLow, dateHigh);
  }

  private String getExchangeRatesRemoteAddress() {
    if (prm.hasParameter(PRM_WS_LB_EXCHANGE_RATES_ADDRESS)) {
      return prm.getText(PRM_WS_LB_EXCHANGE_RATES_ADDRESS);
    } else {
      return null;
    }
  }

  private ResponseObject getHistory(String viewName, Collection<Long> idList) {
    LocalizableConstants loc = usr.getLocalizableConstants();

    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error(loc.selectAtLeastOneRow());
    }
    BeeView view = sys.getView(viewName);

    SqlSelect query = view.getQuery(usr.getCurrentUserId(), Filter.idIn(idList))
        .resetFields().resetOrder();

    Multimap<String, ViewColumn> columnMap = HashMultimap.create();
    Map<String, Pair<String, String>> idMap = new HashMap<>();

    for (ViewColumn col : view.getViewColumns()) {
      if (!col.isHidden() && !col.isReadOnly()
          && (col.getLevel() == 0 || BeeUtils.unbox(col.getEditable()))) {

        String als = view.getColumnSource(col.getName());
        columnMap.put(als, col);

        if (!idMap.containsKey(als)) {
          String parent = col.getParent();

          if (!BeeUtils.isEmpty(parent)) {
            String src = view.getColumnSource(parent);
            String fld = view.getColumnField(parent);
            query.addField(src, fld, parent);

            if (!BeeUtils.isEmpty(query.getGroupBy())) {
              query.addGroup(src, fld);
            }
          } else {
            parent = view.getSourceIdName();
            query.addFields(view.getSourceAlias(), parent);
          }
          idMap.put(als, Pair.of(col.getTable(), parent));
        }
      }
    }
    SimpleRowSet ids = qs.getData(query);
    query = null;

    for (String als : columnMap.keySet()) {
      BeeTable table = sys.getTable(idMap.get(als).getA());

      Set<Long> auditIds = Sets.newHashSet(ids.getLongColumn(idMap.get(als).getB()));
      auditIds.remove(null);

      if (BeeUtils.isEmpty(auditIds) || !table.isAuditable()) {
        continue;
      }
      String src = sys.getAuditSource(table.getName());
      SqlSelect subq = new SqlSelect();

      List<String> fields = new ArrayList<>();
      List<Object> pairs = new ArrayList<>();

      for (ViewColumn col : columnMap.get(als)) {
        fields.add(col.getField());

        if (!BeeUtils.same(col.getField(), col.getName())) {
          pairs.add(col.getField());
          pairs.add(col.getName());
        }
      }
      if (!BeeUtils.isEmpty(pairs)) {
        pairs.add(SqlUtils.field(src, AUDIT_FLD_FIELD));

        subq.addExpr(SqlUtils.sqlCase(SqlUtils.field(src, AUDIT_FLD_FIELD),
            pairs.toArray()), AUDIT_FLD_FIELD);
      } else {
        subq.addFields(src, AUDIT_FLD_FIELD);
      }
      subq.addFields(src, AUDIT_FLD_TIME, AUDIT_FLD_TX, AUDIT_FLD_MODE, AUDIT_FLD_ID,
          AUDIT_FLD_VALUE)
          .addConstant(table.getName(), COL_OBJECT)
          .addField(TBL_USERS, COL_LOGIN, COL_USER)
          .addFrom(src)
          .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, src, AUDIT_FLD_USER))
          .setWhere(SqlUtils.and(SqlUtils.inList(src, AUDIT_FLD_ID, auditIds),
              SqlUtils.or(SqlUtils.inList(src, AUDIT_FLD_FIELD, fields),
                  SqlUtils.isNull(src, AUDIT_FLD_FIELD))));

      if (query == null) {
        query = subq.addOrder(src, AUDIT_FLD_TIME, AUDIT_FLD_ID).addOrder(null, COL_OBJECT);
      } else {
        query.setUnionAllMode(true).addUnion(subq);
      }
    }
    BeeRowSet rs = new BeeRowSet(HISTORY_COLUMNS);

    if (query != null) {
      int fldIdx = rs.getColumnIndex(AUDIT_FLD_FIELD);
      int valIdx = rs.getColumnIndex(AUDIT_FLD_VALUE);
      int relIdx = rs.getColumnIndex(COL_RELATION);
      Map<String, String> dict = usr.getLocalizableDictionary();

      for (SimpleRow row : qs.getData(query)) {
        String[] values = new String[rs.getNumberOfColumns()];
        String fld = row.getValue(AUDIT_FLD_FIELD);

        for (int i = 0; i < values.length; i++) {
          String value;

          if (i == relIdx) {
            value = BeeUtils.isEmpty(fld) ? null : view.getColumnRelation(fld);
          } else {
            value = row.getValue(rs.getColumnId(i));
          }
          if (value != null) {
            if (i == fldIdx) {
              value = BeeUtils.notEmpty(Localized.maybeTranslate(view.getColumnLabel(fld), dict),
                  value);

            } else if (i == valIdx) {
              switch (view.getColumnType(fld)) {
                case BOOLEAN:
                  value = BeeUtils.toBoolean(value) ? loc.yes() : loc.no();
                  break;
                case DATE:
                  value = TimeUtils.toDateTimeOrNull(BeeUtils.toLong(value)).toDateString();
                  break;
                case DATETIME:
                  value = TimeUtils.toDateTimeOrNull(BeeUtils.toLong(value)).toCompactString();
                  break;
                case DECIMAL:
                  String enumKey = view.getColumnEnumKey(fld);

                  if (!BeeUtils.isEmpty(enumKey)) {
                    value = BeeUtils.notEmpty(EnumUtils.getLocalizedCaption(enumKey,
                        BeeUtils.toInt(value), loc), value);
                  }
                  break;
                default:
                  break;
              }
            }
          }
          values[i] = value;
        }
        rs.addRow(new BeeRow(0L, values));
      }
    }
    return ResponseObject.response(rs);
  }

  private ResponseObject getListOfCurrencies() {
    String address = getExchangeRatesRemoteAddress();
    return ExchangeRatesWS.getListOfCurrencies(address);
  }

  private ResponseObject getNumberInWords(Long number, String locale) {
    Assert.notNull(number);

    Locale loc = I18nUtils.toLocale(locale);

    if (loc == null) {
      loc = usr.getLocale();
    }
    return ResponseObject.response(new RuleBasedNumberFormat(loc, RuleBasedNumberFormat.SPELLOUT)
        .format(number));
  }

  private Collection<? extends BeeParameter> getSqlEngineParameters() {
    List<BeeParameter> params = new ArrayList<>();

    for (SqlEngine engine : SqlEngine.values()) {
      Map<String, String> value = null;

      switch (engine) {
        case GENERIC:
        case MSSQL:
        case ORACLE:
          break;
        case POSTGRESQL:
          value = ImmutableMap
              .of(".+duplicate key value violates unique constraint.+(\\(.+=.+\\)).+",
                  "Tokia reikšmė jau egzistuoja: $1",
                  ".+violates foreign key constraint.+from table \"(.+)\"\\.",
                  "Įrašas naudojamas lentelėje \"$1\"");
          break;
      }
      params.add(BeeParameter.createMap(getModule().getName(),
          BeeUtils.join(BeeConst.STRING_EMPTY, PRM_SQL_MESSAGES, engine), false, value));
    }
    return params;
  }

  private void refreshCurrencyRates() {
    long historyId = sys.eventStart(PRM_REFRESH_CURRENCY_HOURS);

    String daysOfToday = BeeUtils.toString(TimeUtils.today().getDays());
    ResponseObject response = updateExchangeRates(daysOfToday, daysOfToday);

    sys.eventEnd(historyId, response.getMessages().toArray());
  }

  private ResponseObject updateExchangeRates(String low, String high) {
    if (!BeeUtils.isPositiveInt(low)) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_EXCHANGE_RATES, VAR_DATE_LOW);
    }
    JustDate dateLow = new JustDate(BeeUtils.toInt(low));

    if (!BeeUtils.isPositiveInt(high)) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_EXCHANGE_RATES, VAR_DATE_HIGH);
    }
    JustDate dateHigh = new JustDate(BeeUtils.toInt(high));

    if (TimeUtils.isMore(dateLow, dateHigh)) {
      return ResponseObject.error(usr.getLocalizableConstants().invalidRange(), dateLow, dateHigh);
    }

    String currencyIdName = sys.getIdName(TBL_CURRENCIES);

    SqlSelect currencyQuery = new SqlSelect()
        .addFields(TBL_CURRENCIES, currencyIdName, COL_CURRENCY_NAME)
        .addFrom(TBL_CURRENCIES)
        .setWhere(SqlUtils.notNull(TBL_CURRENCIES, COL_CURRENCY_UPDATE_TAG))
        .addOrder(TBL_CURRENCIES, COL_CURRENCY_NAME);

    SimpleRowSet currencies = qs.getData(currencyQuery);
    if (DataUtils.isEmpty(currencies)) {
      return ResponseObject
          .warning(usr.getLocalizableConstants().updateExchangeRatesNoCurrencies());
    }

    String address = getExchangeRatesRemoteAddress();

    ResponseObject response = ResponseObject.emptyResponse();

    for (SimpleRow currencyRow : currencies) {
      Long currencyId = currencyRow.getLong(currencyIdName);
      String currencyName = BeeUtils.trim(currencyRow.getValue(COL_CURRENCY_NAME));

      ResponseObject currencyResponse = ExchangeRatesWS.getExchangeRatesForCurrency(address, null,
          currencyName, dateLow, dateHigh);

      if (currencyResponse.hasErrors()) {
        response.addErrorsFrom(currencyResponse);
        break;
      }

      SimpleRowSet rates;
      if (currencyResponse.hasResponse(SimpleRowSet.class)) {
        rates = (SimpleRowSet) currencyResponse.getResponse();
      } else {
        rates = null;
      }

      if (DataUtils.isEmpty(rates)) {
        response.addInfo(currencyName, usr.getLocalizableConstants().noData());
        continue;
      }

      String value = rates.getValue(0, ExchangeRatesWS.COL_DT);
      JustDate min = TimeUtils.parseDate(value);
      if (min == null) {
        response.addWarning(currencyName, usr.getLocalizableConstants().invalidDate(), value);
        continue;
      }

      JustDate max = JustDate.copyOf(min);

      if (rates.getNumberOfRows() > 1) {
        for (int i = 1; i < rates.getNumberOfRows(); i++) {
          JustDate date = TimeUtils.parseDate(rates.getValue(i, ExchangeRatesWS.COL_DT));
          if (date != null) {
            min = TimeUtils.min(min, date);
            max = TimeUtils.max(max, date);
          }
        }
      }

      SqlDelete delete = new SqlDelete(TBL_CURRENCY_RATES).setWhere(SqlUtils.and(
          SqlUtils.equals(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_CURRENCY, currencyId),
          SqlUtils.moreEqual(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE,
              min.getDateTime().getTime()),
          SqlUtils.less(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE,
              TimeUtils.nextDay(max).getDateTime().getTime())));

      ResponseObject deleteResponse = qs.updateDataWithResponse(delete);
      if (deleteResponse.hasErrors()) {
        response.addErrorsFrom(deleteResponse);
        break;
      }

      int deleteCount = (int) deleteResponse.getResponse();
      int insertCount = 0;

      for (SimpleRow rateRow : rates) {
        DateTime date = TimeUtils.parseDateTime(rateRow.getValue(ExchangeRatesWS.COL_DT));

        Double factor = rateRow.getDouble(ExchangeRatesWS.COL_AMT_2);

        Double amt = rateRow.getDouble(ExchangeRatesWS.COL_AMT_1);
        if (BeeUtils.isPositive(amt) && !Objects.equals(amt, BeeConst.DOUBLE_ONE)) {
          factor /= amt;
        }

        if (date != null && BeeUtils.isPositive(factor)) {
          SqlInsert insert = new SqlInsert(TBL_CURRENCY_RATES)
              .addConstant(COL_CURRENCY_RATE_CURRENCY, currencyId)
              .addConstant(COL_CURRENCY_RATE_DATE, date.getTime())
              .addConstant(COL_CURRENCY_RATE_QUANTITY, 1)
              .addConstant(COL_CURRENCY_RATE, 1 / factor);

          ResponseObject insertResponse = qs.insertDataWithResponse(insert);
          if (insertResponse.hasErrors()) {
            response.addErrorsFrom(insertResponse);
            break;
          }

          insertCount++;
        }

        if (response.hasErrors()) {
          break;
        }
      }

      if (response.hasErrors()) {
        break;
      }

      String delMsg = (deleteCount > 0) ? BeeUtils.toString(-deleteCount) : null;
      String insMsg = BeeConst.STRING_PLUS + insertCount;

      response.addInfo(currencyName, delMsg, insMsg);
    }

    return response;
  }
}
