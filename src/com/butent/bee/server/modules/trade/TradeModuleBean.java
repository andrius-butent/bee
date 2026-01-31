package com.butent.bee.server.modules.trade;

import com.google.common.base.CharMatcher;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.finance.FinanceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.InsertOrUpdate;
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
import com.butent.bee.server.modules.administration.AdministrationModuleBean;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.finance.FinanceModuleBean;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.Triplet;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.WordWrap;
import com.butent.bee.shared.data.AbstractRow;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.elements.Caption;
import com.butent.bee.shared.html.builder.elements.Pre;
import com.butent.bee.shared.html.builder.elements.Table;
import com.butent.bee.shared.html.builder.elements.Td;
import com.butent.bee.shared.html.builder.elements.Th;
import com.butent.bee.shared.html.builder.elements.Tr;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.Menu;
import com.butent.bee.shared.menu.MenuItem;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.modules.finance.TradeAccounts;
import com.butent.bee.shared.modules.orders.OrdersConstants;
import com.butent.bee.shared.modules.payroll.PayrollConstants;
import com.butent.bee.shared.modules.trade.DebtKind;
import com.butent.bee.shared.modules.trade.ItemQuantities;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeCostBasis;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocument;
import com.butent.bee.shared.modules.trade.TradeDocumentData;
import com.butent.bee.shared.modules.trade.TradeDocumentItem;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.webservice.ButentWS;
import com.butent.webservice.WSDocument;
import com.butent.webservice.WSDocument.WSDocumentItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Stateless
@LocalBean
public class TradeModuleBean implements BeeModule, ConcurrencyBean.HasTimerService {

  private static BeeLogger logger = LogUtils.getLogger(TradeModuleBean.class);

  private static final Map<ModuleAndSub, StockReservationsProvider> stockReservationsProviders =
      new HashMap<>();

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  ParamHolderBean prm;
  @EJB
  ConcurrencyBean cb;
  @EJB
  TradeReportsBean rep;
  @EJB
  TradeActBean act;
  @EJB
  MailModuleBean mail;
  @EJB
  DataEditorBean deb;
  @EJB
  AdministrationModuleBean adm;
  @EJB
  FinanceModuleBean fin;

  @Resource
  TimerService timerService;

  public static String buildMessage(Stopwatch stopwatch, Object... args) {
    List<Object> words = Lists.newArrayList(args);
    words.add(BeeUtils.bracket(stopwatch.toString()));

    return BeeUtils.joinWords(words);
  }

  public static Long decodeId(String trade, Long id) {
    Assert.notEmpty(trade);
    Long normalizedId;

    switch (trade) {
      case TBL_PURCHASES:
        normalizedId = id / 2;
        break;

      case TBL_SALES:
        normalizedId = (id - 1) / 2;
        break;

      default:
        throw new BeeRuntimeException("View source not supported: " + trade);
    }
    return normalizedId;
  }

  public static void refreshStock() {
    Endpoint.refreshViews(VIEW_TRADE_STOCK);
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    Filter documentFilter;
    if (DataUtils.isId(query)) {
      documentFilter = Filter.or(Filter.compareId(BeeUtils.toLong(query)),
          Filter.equals(COL_TRADE_NUMBER, query));
    } else {
      documentFilter = Filter.equals(COL_TRADE_NUMBER, query);
    }

    result.addAll(qs.getSearchResults(VIEW_TRADE_DOCUMENTS, documentFilter));

    Set<String> columns = Sets.newHashSet(COL_TRADE_NUMBER, COL_TRADE_INVOICE_NO,
        ALS_CUSTOMER_NAME);
    result.addAll(qs.getSearchResults(VIEW_SALES, Filter.anyContains(columns, query)));

    if (usr.isModuleVisible(ModuleAndSub.of(getModule(), SubModule.ACTS))) {
      List<SearchResult> actSr = act.doSearch(query);
      if (!BeeUtils.isEmpty(actSr)) {
        result.addAll(actSr);
      }
    }
    return result;
  }

  @Override
  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response;

    String svc = BeeUtils.trim(service);

    switch (svc) {
      case SVC_ITEMS_INFO:
        response = getItemsInfo(reqInfo.getParameter("view_name"),
            BeeUtils.toLongOrNull(reqInfo.getParameter("id")),
            reqInfo.getParameter(COL_CURRENCY));
        break;

      case SVC_CREDIT_INFO:
        response = getCreditInfo(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_COMPANY)));
        break;

      case SVC_GET_DOCUMENT_DATA:
        response = getTradeDocumentData(reqInfo);
        break;

      case SVC_SEND_TO_ERP:
        response = sendToERP(reqInfo.getParameter(VAR_VIEW_NAME),
            DataUtils.parseIdSet(reqInfo.getParameter(VAR_ID_LIST)));
        break;

      case SVC_REMIND_DEBTS_EMAIL:
        response = sendDebtsRemindEmail(reqInfo);
        break;

      case SVC_GET_DOCUMENT_TYPE_CAPTION_AND_FILTER:
        response = getDocumentTypeCaptionAndFilter(reqInfo);
        break;

      case SVC_DOCUMENT_PHASE_TRANSITION:
        response = tryPhaseTransition(reqInfo);
        break;

      case SVC_REBUILD_STOCK:
        response = rebuildStock();
        break;

      case SVC_CALCULATE_COST:
        response = calculateCost(reqInfo);
        break;

      case SVC_GET_STOCK:
        Multimap<Long, ItemQuantities> stock =
            getStock(reqInfo.getParameterLong(COL_STOCK_WAREHOUSE),
                DataUtils.parseIdSet(reqInfo.getParameter(VAR_ITEMS)),
                reqInfo.getParameterBoolean(VAR_RESERVATIONS));

        response = stock.isEmpty()
            ? ResponseObject.emptyResponse() : ResponseObject.response(stock);
        break;

      case SVC_GET_ITEM_STOCK_BY_WAREHOUSE:
        response = getItemStockByWarehouse(reqInfo);
        break;

      case SVC_GET_RESERVATIONS_INFO:
        response = ResponseObject.response(getReservationsInfo(
            reqInfo.getParameterLong(COL_STOCK_WAREHOUSE),
            reqInfo.getParameterLong(COL_ITEM),
            reqInfo.getParameterDateTime(COL_DATE_TO)));
        break;

      case SVC_CREATE_DOCUMENT:
        response = createDocument(TradeDocument.restore(reqInfo.getParameter(VAR_DOCUMENT)));
        break;

      case SVC_GET_RELATED_TRADE_ITEMS:
        response = getRelatedTradeItems(reqInfo);
        break;

      case SVC_TRADE_STOCK_REPORT:
      case SVC_TRADE_MOVEMENT_OF_GOODS_REPORT:
        response = rep.doService(svc, reqInfo);
        break;

      case SVC_SUBMIT_PAYMENT:
        response = submitPayment(reqInfo);
        break;

      case SVC_DISCHARGE_DEBT:
        response = dischargeDebt(reqInfo);
        break;

      case SVC_DISCHARGE_PREPAYMENT:
        response = dischargePrepayment(reqInfo);
        break;

      default:
        if (reqInfo.getSubModule() == SubModule.ACTS) {
          response = act.doService(svc, reqInfo);

        } else {
          String msg = BeeUtils.joinWords("Trade service not recognized:", svc);
          logger.warning(msg);
          response = ResponseObject.error(msg);
        }
    }

    return response;
  }

  @Override
  public void ejbTimeout(Timer timer) {
    if (ConcurrencyBean.isParameterTimer(timer, PRM_ERP_REFRESH_INTERVAL)) {
      importERPPayments();
    }
  }

  public static String encodeId(String trade, Long id) {
    Assert.notEmpty(trade);
    Long normalizedId;

    switch (trade) {
      case TBL_PURCHASES:
        normalizedId = id * 2;
        break;

      case TBL_SALES:
        normalizedId = id * 2 + 1;
        break;

      default:
        throw new BeeRuntimeException("View source not supported: " + trade);
    }
    return BeeUtils.toString(normalizedId);
  }

  public static IsExpression getAmountExpression(String tblName) {
    return SqlUtils.multiply(SqlUtils.field(tblName, COL_TRADE_ITEM_QUANTITY),
        SqlUtils.field(tblName, COL_TRADE_ITEM_PRICE));
  }

  /**
   * Returns map based collection of company and e-mail address'es list. This collection formed from
   * data source table of Contacts where has relation of Companies table. It must also be not empty
   * the RemindEmail field in Contacts data source table.
   * <p>
   * This collection using send reports or documents over e-mail liked with company Id.
   *
   * @param companyIds List of company Id's using to filter company email of contacts.
   * @return collection of company contact e-mails where key of map is companyId and value of map is
   * collection of email address.
   * @throws BeeRuntimeException throws if collection {@code companyIds} is empty or null.
   */
  public Multimap<Long, String> getCompaniesRemindEmailAddresses(List<Long> companyIds) {

    Assert.notEmpty(companyIds);

    Multimap<Long, String> emails = HashMultimap.create();

    SqlSelect select = new SqlSelect()
        .addField(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), COL_COMPANY)
        .addField(TBL_EMAILS, COL_EMAIL_ADDRESS, COL_EMAIL_ADDRESS)
        .addFrom(TBL_COMPANIES)
        .addFromInner(TBL_COMPANY_CONTACTS, sys.joinTables(TBL_COMPANIES, TBL_COMPANY_CONTACTS,
            COL_COMPANY))
        .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, TBL_COMPANY_CONTACTS, COL_CONTACT))
        .addFromInner(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
        .setWhere(SqlUtils.and(SqlUtils.inList(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES),
            companyIds), SqlUtils.notNull(TBL_COMPANY_CONTACTS, COL_REMIND_EMAIL)))
        .setDistinctMode(true);

    SimpleRowSet companiesEmails = qs.getData(select);

    for (String[] row : companiesEmails.getRows()) {
      Long companyId = BeeUtils.toLong(row[companiesEmails.getColumnIndex(COL_COMPANY)]);
      emails.put(companyId, row[companiesEmails.getColumnIndex(COL_EMAIL)]);
    }

    return emails;
  }

  public ResponseObject getCreditInfo(Long companyId) {
    if (!DataUtils.isId(companyId)) {
      return ResponseObject.emptyResponse();
    }
    SimpleRow company = qs.getRow(new SqlSelect()
        .addFields(TBL_COMPANIES, COL_COMPANY_NAME, COL_COMPANY_CREDIT_LIMIT,
            COL_COMPANY_LIMIT_CURRENCY, COL_COMPANY_CREDIT_DAYS)
        .addField(TBL_CURRENCIES, COL_CURRENCY_NAME, COL_CURRENCY)
        .addFrom(TBL_COMPANIES)
        .addFromLeft(TBL_CURRENCIES,
            sys.joinTables(TBL_CURRENCIES, TBL_COMPANIES, COL_COMPANY_LIMIT_CURRENCY))
        .setWhere(sys.idEquals(TBL_COMPANIES, companyId)));

    Map<String, Object> resp = new HashMap<>();

    if (company != null) {
      double limit = BeeUtils.unbox(company.getDouble(COL_COMPANY_CREDIT_LIMIT));
      Long curr = company.getLong(COL_COMPANY_LIMIT_CURRENCY);
      int days = BeeUtils.unbox(company.getInt(COL_COMPANY_CREDIT_DAYS));

      SqlSelect query = new SqlSelect()
          .addFields(TBL_SALES, COL_TRADE_DATE, COL_TRADE_TERM)
          .addFrom(TBL_SALES)
          .setWhere(SqlUtils.and(SqlUtils.or(SqlUtils.equals(TBL_SALES, COL_SALE_PAYER, companyId),
              SqlUtils.and(SqlUtils.isNull(TBL_SALES, COL_SALE_PAYER),
                  SqlUtils.equals(TBL_SALES, COL_TRADE_CUSTOMER, companyId))),
              SqlUtils.less(SqlUtils.nvl(SqlUtils.field(TBL_SALES, COL_TRADE_PAID), 0),
                  SqlUtils.nvl(SqlUtils.field(TBL_SALES, COL_TRADE_AMOUNT), 0))));

      if (DataUtils.isId(curr)) {
        query.addExpr(ExchangeUtils.exchangeFieldTo(query, TBL_SALES, COL_TRADE_AMOUNT,
            COL_TRADE_CURRENCY, COL_TRADE_DATE, curr), COL_TRADE_AMOUNT)
            .addExpr(ExchangeUtils.exchangeFieldTo(query, TBL_SALES, COL_TRADE_PAID,
                COL_TRADE_CURRENCY, COL_TRADE_PAYMENT_TIME, curr), COL_TRADE_PAID);
      } else {
        query.addExpr(ExchangeUtils.exchangeField(query, TBL_SALES, COL_TRADE_AMOUNT,
            COL_TRADE_CURRENCY, COL_TRADE_DATE), COL_TRADE_AMOUNT)
            .addExpr(ExchangeUtils.exchangeField(query, TBL_SALES, COL_TRADE_PAID,
                COL_TRADE_CURRENCY, COL_TRADE_PAYMENT_TIME), COL_TRADE_PAID);
      }
      double debt = 0.0;
      double overdue = 0.0;

      for (SimpleRow row : qs.getData(query)) {
        double xxx = BeeUtils.unbox(row.getDouble(COL_TRADE_AMOUNT))
            - BeeUtils.unbox(row.getDouble(COL_TRADE_PAID));

        int dayDiff = TimeUtils.dayDiff(BeeUtils.nvl(row.getDateTime(COL_TRADE_TERM),
            TimeUtils.nextDay(row.getDateTime(COL_TRADE_DATE), days)), TimeUtils.nowMinutes());

        if (dayDiff > 0) {
          overdue += xxx;
        }
        debt += xxx;
      }
      resp.put(COL_COMPANY_NAME, company.getValue(COL_COMPANY_NAME));
      resp.put(COL_COMPANY_CREDIT_LIMIT, limit);
      resp.put(COL_COMPANY_LIMIT_CURRENCY, curr);
      resp.put(COL_CURRENCY, company.getValue(COL_CURRENCY));
      resp.put(COL_COMPANY_CREDIT_DAYS, days);
      resp.put(VAR_DEBT, BeeUtils.round(debt, 2));
      resp.put(VAR_OVERDUE, BeeUtils.round(overdue, 2));
    }
    return ResponseObject.response(resp);
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    Collection<BeeParameter> params = new ArrayList<>();
    params.add(BeeParameter.createDate(module, PRM_PROTECT_TRADE_DOCUMENTS_BEFORE, true, null));

    params.addAll(act.getDefaultParameters());

    return params;
  }

  public static IsExpression getDiscount(String tblName) {
    return getDiscount(tblName, getAmountExpression(tblName));
  }

  public static IsExpression getDiscount(String tblName, IsExpression amount) {
    IsExpression discount = SqlUtils.field(tblName, COL_TRADE_DISCOUNT);

    return SqlUtils.sqlIf(SqlUtils.isNull(tblName, COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT),
        discount, SqlUtils.multiply(SqlUtils.divide(amount, 100), discount));
  }

  @Override
  public Module getModule() {
    return Module.TRADE;
  }

  public static Multimap<Long, ItemQuantities> getReservations(Long warehouse,
      Collection<Long> items) {
    Multimap<Long, ItemQuantities> reservations = HashMultimap.create();

    stockReservationsProviders.values().forEach(provider -> {
      Multimap<Long, ItemQuantities> result = provider.getStockReservations(warehouse, items);

      if (Objects.nonNull(result)) {
        result.forEach((item, itemQuantities) -> {
          Optional<ItemQuantities> existing = reservations.get(item).stream()
              .filter(itemQuantities::equals).findAny();

          if (existing.isPresent()) {
            itemQuantities.getReservedMap().forEach(existing.get()::addReserved);
          } else {
            reservations.put(item, itemQuantities);
          }
        });
      }
    });
    return reservations;
  }

  public static Map<ModuleAndSub, Map<String, Double>> getReservationsInfo(Long warehouse,
      Long item, DateTime dateTo) {
    Map<ModuleAndSub, Map<String, Double>> reservationsInfo = new LinkedHashMap<>();

    stockReservationsProviders.forEach((moduleAndSub, stockReservationsProvider) -> {
      Map<String, Double> info = stockReservationsProvider.getItemReservationsInfo(warehouse, item,
          dateTo);

      if (!BeeUtils.isEmpty(info)) {
        reservationsInfo.put(moduleAndSub, info);
      }
    });
    return reservationsInfo;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  public Multimap<Long, ItemQuantities> getStock(Long warehouse, Collection<Long> items,
      boolean includeReservations) {
    Multimap<Long, ItemQuantities> result = ArrayListMultimap.create();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, COL_TRADE_ITEM_ARTICLE)
        .addSum(TBL_TRADE_STOCK, COL_STOCK_QUANTITY)
        .addFrom(TBL_TRADE_STOCK)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM))
        .addGroup(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, COL_TRADE_ITEM_ARTICLE)
        .addOrder(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, COL_TRADE_ITEM_ARTICLE);

    HasConditions where = SqlUtils.and(SqlUtils.positive(TBL_TRADE_STOCK, COL_STOCK_QUANTITY));

    if (DataUtils.isId(warehouse)) {
      where.add(SqlUtils.equals(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE, warehouse));
    }
    if (!BeeUtils.isEmpty(items)) {
      where.add(SqlUtils.inList(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, items));
    }
    query.setWhere(where);

    SimpleRowSet data = qs.getData(query);
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_ITEM), ItemQuantities.stock(row.getValue(COL_TRADE_ITEM_ARTICLE),
            row.getDouble(COL_STOCK_QUANTITY)));
      }
    }
    if (includeReservations) {
      getReservations(warehouse, items).forEach((item, itemQuantities) -> {
        Optional<ItemQuantities> existing = result.get(item).stream()
            .filter(itemQuantities::equals).findAny();

        if (existing.isPresent()) {
          itemQuantities.getReservedMap().forEach(existing.get()::addReserved);
        } else {
          result.put(item, itemQuantities);
        }
      });
    }
    return result;
  }

  public ResponseObject createDocument(TradeDocument document) {
    if (document == null) {
      return ResponseObject.error(SVC_CREATE_DOCUMENT, "document is null");
    }
    if (!document.isValid()) {
      return ResponseObject.error(SVC_CREATE_DOCUMENT, "document is not valid");
    }
    List<TradeDocumentItem> tradeDocumentItems = document.getItems().stream()
        .filter(item -> item != null && item.isValid())
        .collect(Collectors.toList());

    if (BeeUtils.isEmpty(tradeDocumentItems)) {
      return ResponseObject.error(SVC_CREATE_DOCUMENT, "no valid items found");
    }

    OperationType operationType = getOperationTypeByOperation(document.getOperation());
    if (operationType == null) {
      return ResponseObject.error(SVC_CREATE_DOCUMENT,
          COL_TRADE_OPERATION, document.getOperation(), COL_OPERATION_TYPE, "is null");
    }

    TradeDocumentPhase phase = document.getPhase();

    if (operationType.producesStock() && phase.modifyStock()
        && !DataUtils.isId(document.getWarehouseTo())) {
      return ResponseObject.error(SVC_CREATE_DOCUMENT, COL_TRADE_WAREHOUSE_TO, "is null");
    }

    Multimap<Integer, Pair<Long, Double>> parents = ArrayListMultimap.create();

    if (operationType.consumesStock() && phase.modifyStock()) {
      Map<Long, Double> usedParents = new HashMap<>();

      for (int i = 0; i < tradeDocumentItems.size(); i++) {
        TradeDocumentItem tradeDocumentItem = tradeDocumentItems.get(i);
        Long item = tradeDocumentItem.getItem();

        if (isStockItem(item)) {
          double quantity = tradeDocumentItem.getQuantity();
          Long warehouse = BeeUtils.nvl(tradeDocumentItem.getItemWarehouseFrom(),
              document.getWarehouseFrom());

          Map<Long, Double> parentQuantities = getParentQuantities(item, warehouse, usedParents);
          double totalStock = parentQuantities.values().stream().mapToDouble(d -> d).sum();

          if (!parentQuantities.isEmpty() && totalStock >= quantity) {
            for (Map.Entry<Long, Double> entry : parentQuantities.entrySet()) {
              long parent = entry.getKey();
              double stock = entry.getValue();

              double q = Math.min(quantity, stock);

              parents.put(i, Pair.of(parent, q));
              usedParents.merge(parent, q, Double::sum);

              quantity -= q;

              if (!BeeUtils.isPositive(quantity)) {
                break;
              }
            }

          } else {
            return ResponseObject.error("stock", totalStock,
                "item", item, tradeDocumentItem.getArticle(),
                "quantity", tradeDocumentItem.getQuantity(),
                "warehouse", warehouse);
          }
        }
      }
    }

    SqlInsert insertDocument = new SqlInsert(TBL_TRADE_DOCUMENTS).addAll(document.getValues());

    ResponseObject insertDocumentResponse = qs.insertDataWithResponse(insertDocument);
    if (insertDocumentResponse.hasErrors()) {
      return insertDocumentResponse;
    }

    Long docId = insertDocumentResponse.getResponseAsLong();

    ResponseObject extraDimensionsResponse =
        maybeCreateExtraDimensions(TBL_TRADE_DOCUMENTS, docId, document.getDimensionValues());
    if (extraDimensionsResponse.hasErrors()) {
      return extraDimensionsResponse;
    }

    ResponseObject tradeAccountsResponse =
        maybeCreateTradeAccounts(TBL_TRADE_DOCUMENTS, docId, document.getTradeAccountValues());
    if (tradeAccountsResponse.hasErrors()) {
      return tradeAccountsResponse;
    }

    for (int i = 0; i < tradeDocumentItems.size(); i++) {
      TradeDocumentItem tradeDocumentItem = tradeDocumentItems.get(i);

      Map<String, Value> values = tradeDocumentItem.getValues();
      values.put(COL_TRADE_DOCUMENT, new LongValue(docId));

      if (parents.containsKey(i)) {
        for (Pair<Long, Double> p : parents.get(i)) {
          long parent = p.getA();
          double quantity = p.getB();

          values.put(COL_TRADE_ITEM_QUANTITY, new NumberValue(quantity));
          values.put(COL_TRADE_ITEM_PARENT, new LongValue(parent));

          SqlInsert insertItem = new SqlInsert(TBL_TRADE_DOCUMENT_ITEMS).addAll(values);

          ResponseObject insertItemResponse = qs.insertDataWithResponse(insertItem);
          if (insertItemResponse.hasErrors()) {
            return insertItemResponse;
          }

          SqlUpdate stockUpdate = new SqlUpdate(TBL_TRADE_STOCK)
              .addExpression(COL_STOCK_QUANTITY,
                  SqlUtils.minus(SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_QUANTITY), quantity))
              .setWhere(SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, parent));

          ResponseObject stockUpdateResponse = qs.updateDataWithResponse(stockUpdate);
          if (stockUpdateResponse.hasErrors()) {
            return stockUpdateResponse;
          }

          Long itemId = insertItemResponse.getResponseAsLong();

          extraDimensionsResponse = maybeCreateExtraDimensions(TBL_TRADE_DOCUMENT_ITEMS, itemId,
              tradeDocumentItem.getDimensionValues());
          if (extraDimensionsResponse.hasErrors()) {
            return extraDimensionsResponse;
          }

          tradeAccountsResponse = maybeCreateTradeAccounts(TBL_TRADE_DOCUMENT_ITEMS, itemId,
              tradeDocumentItem.getTradeAccountValues());
          if (tradeAccountsResponse.hasErrors()) {
            return tradeAccountsResponse;
          }
        }

      } else {
        SqlInsert insertItem = new SqlInsert(TBL_TRADE_DOCUMENT_ITEMS).addAll(values);

        ResponseObject insertItemResponse = qs.insertDataWithResponse(insertItem);
        if (insertItemResponse.hasErrors()) {
          return insertItemResponse;
        }

        Long itemId = insertItemResponse.getResponseAsLong();

        extraDimensionsResponse = maybeCreateExtraDimensions(TBL_TRADE_DOCUMENT_ITEMS, itemId,
            tradeDocumentItem.getDimensionValues());
        if (extraDimensionsResponse.hasErrors()) {
          return extraDimensionsResponse;
        }

        tradeAccountsResponse = maybeCreateTradeAccounts(TBL_TRADE_DOCUMENT_ITEMS, itemId,
            tradeDocumentItem.getTradeAccountValues());
        if (tradeAccountsResponse.hasErrors()) {
          return tradeAccountsResponse;
        }
      }
    }

    if (operationType.producesStock() && phase.modifyStock()) {
      ResponseObject insertStockResponse =
          insertStock(SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT, docId),
              document.getWarehouseTo());
      if (insertStockResponse.hasErrors()) {
        return insertStockResponse;
      }
    }

    if (operationType.providesCost() && phase.modifyStock()) {
      ResponseObject calculateCostResponse =
          calculateCost(docId, document.getDate(), document.getCurrency(),
              document.getDocumentVatMode(), document.getDocumentDiscountMode(),
              document.getDocumentDiscount());
      if (calculateCostResponse.hasErrors()) {
        return calculateCostResponse;
      }
    }

    if (!parents.isEmpty()) {
      refreshStock();
    }

    return ResponseObject.response(docId);
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  public static IsExpression getTotal(String tblName) {
    return getTotal(tblName, getAmountExpression(tblName));
  }

  public static IsExpression getTotal(String tblName, IsExpression amount) {
    return SqlUtils.plus(SqlUtils.minus(amount, SqlUtils.nvl(getDiscount(tblName), 0)),
        SqlUtils.sqlIf(SqlUtils.equals(SqlUtils.name(COL_TRADE_DOCUMENT_VAT_MODE),
            TradeVatMode.PLUS), SqlUtils.nvl(getVat(tblName, amount), 0), 0));
  }

  public static IsExpression getTotalExpression(String tblName) {
    return getTotalExpression(tblName, getAmountExpression(tblName));
  }

  public static IsExpression getTotalExpression(String tblName, IsExpression amount) {
    return SqlUtils.plus(amount,
        SqlUtils.sqlIf(SqlUtils.isNull(tblName, COL_TRADE_VAT_PLUS), 0,
            getVatExpression(tblName, amount)));
  }

  public static IsExpression getVat(String tblName) {
    return getVat(tblName, getAmountExpression(tblName));
  }

  public static IsExpression getVat(String tblName, IsExpression amount) {
    IsExpression total = SqlUtils.minus(amount, SqlUtils.nvl(getDiscount(tblName), 0));
    IsExpression vat = SqlUtils.field(tblName, COL_TRADE_VAT);

    return SqlUtils.sqlCase(null,
        SqlUtils.isNull(SqlUtils.name(COL_TRADE_DOCUMENT_VAT_MODE)), null,
        SqlUtils.isNull(tblName, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT), vat,
        SqlUtils.equals(SqlUtils.name(COL_TRADE_DOCUMENT_VAT_MODE), TradeVatMode.PLUS),
        SqlUtils.multiply(SqlUtils.divide(total, 100), vat),
        SqlUtils.multiply(SqlUtils.divide(total, SqlUtils.plus(100, vat)), vat));
  }

  public static IsExpression getVatExpression(String tblName) {
    return getVatExpression(tblName, getAmountExpression(tblName));
  }

  public static IsExpression getVatExpression(String tblName, IsExpression amount) {
    return SqlUtils.sqlCase(null,
        SqlUtils.isNull(tblName, COL_TRADE_VAT), 0,
        SqlUtils.isNull(tblName, COL_TRADE_VAT_PERC), SqlUtils.field(tblName, COL_TRADE_VAT),
        SqlUtils.notNull(tblName, COL_TRADE_VAT_PLUS), SqlUtils.multiply(SqlUtils
            .divide(amount, 100), SqlUtils.field(tblName, COL_TRADE_VAT)),
        SqlUtils.multiply(
            SqlUtils.divide(amount, SqlUtils.plus(100, SqlUtils.field(tblName, COL_TRADE_VAT))),
            SqlUtils.field(tblName, COL_TRADE_VAT)));
  }

  public static IsExpression getWithoutVatExpression(String tblName) {
    return getWithoutVatExpression(tblName, getAmountExpression(tblName));
  }

  public static IsExpression getWithoutVatExpression(String tblName, IsExpression amount) {
    return SqlUtils.minus(amount,
        SqlUtils.sqlIf(SqlUtils.notNull(tblName, COL_TRADE_VAT_PLUS), 0,
            getVatExpression(tblName, amount)));
  }

  @Override
  public void init() {
    cb.createIntervalTimer(this.getClass(), PRM_ERP_REFRESH_INTERVAL);

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void fillInvoiceNumber(ViewModifyEvent event) {
        if (event.isBefore()
            && Objects.equals(sys.getViewSource(event.getTargetName()), TBL_SALES)) {
          List<BeeColumn> cols;
          IsRow row;
          String prefix = null;

          if (event instanceof ViewInsertEvent) {
            cols = ((ViewInsertEvent) event).getColumns();
            row = ((ViewInsertEvent) event).getRow();
          } else if (event instanceof ViewUpdateEvent) {
            cols = ((ViewUpdateEvent) event).getColumns();
            row = ((ViewUpdateEvent) event).getRow();
          } else {
            return;
          }
          int prefixIdx = DataUtils.getColumnIndex(COL_TRADE_SALE_SERIES, cols);

          if (!BeeConst.isUndef(prefixIdx)) {
            prefix = row.getString(prefixIdx);
          }
          if (!BeeUtils.isEmpty(prefix)) {
            int numberIdx = DataUtils.getColumnIndex(COL_TRADE_INVOICE_NO, cols);

            if (BeeConst.isUndef(numberIdx)) {
              cols.add(new BeeColumn(COL_TRADE_INVOICE_NO));
              row.addValue(null);
              numberIdx = row.getNumberOfCells() - 1;

            } else if (!BeeUtils.isEmpty(row.getString(numberIdx))) {
              return;
            }
            row.setValue(numberIdx, qs.getNextNumber(TBL_SALES, COL_TRADE_INVOICE_NO, prefix,
                COL_TRADE_SALE_SERIES));
          }
        }
      }

      @AllowConcurrentEvents
      @Subscribe
      public void fillDebtReportsProperties(ViewQueryEvent event) {
        if (event.isBefore()) {
          return;
        }

        if (BeeUtils.same(event.getTargetName(), VIEW_DEBT_REPORTS)) {
          BeeRowSet gridRowSet = event.getRowset();
          DataInfo viewData = sys.getDataInfo(VIEW_DEBT_REPORTS);

          if (gridRowSet.isEmpty()) {
            return;
          }

          List<Long> companiesId = Lists.newArrayList();

          for (IsRow gridRow : gridRowSet) {
            Long companyId = gridRow.getLong(viewData.getColumnIndex(COL_COMPANY));
            if (DataUtils.isId(companyId)) {
              companiesId.add(companyId);
            }
          }

          if (BeeUtils.isEmpty(companiesId)) {
            return;
          }

          Multimap<Long, String> reminderEmails = getCompaniesRemindEmailAddresses(companiesId);

          for (IsRow gridRow : gridRowSet) {
            Long companyId = gridRow.getLong(viewData.getColumnIndex(COL_COMPANY));

            Collection<String> emailsData = reminderEmails.get(companyId);

            if (BeeUtils.isEmpty(emailsData)) {
              continue;
            }

            String emails = BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, emailsData);

            if (!BeeUtils.isEmpty(emails)) {
              gridRow.setProperty(PROP_REMIND_EMAIL, emails);
            }
          }

        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void modifyTradeStock(ViewModifyEvent event) {
        List<BeeColumn> columns;
        BeeRow row;

        if (event.isTarget(VIEW_TRADE_DOCUMENT_ITEMS)) {
          if (event instanceof ViewInsertEvent) {
            columns = ((ViewInsertEvent) event).getColumns();
            row = ((ViewInsertEvent) event).getRow();

            int docIndex = DataUtils.getColumnIndex(COL_TRADE_DOCUMENT, columns);
            int itemIndex = DataUtils.getColumnIndex(COL_ITEM, columns);
            int qtyIndex = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, columns);
            int wrhToIndex = DataUtils.getColumnIndex(COL_TRADE_ITEM_WAREHOUSE_TO, columns);
            int parentIndex = DataUtils.getColumnIndex(COL_TRADE_ITEM_PARENT, columns);

            Long docId = DataUtils.getLongQuietly(row, docIndex);
            Long item = DataUtils.getLongQuietly(row, itemIndex);
            Double quantity = DataUtils.getDoubleQuietly(row, qtyIndex);
            Long warehouseTo = DataUtils.getLongQuietly(row, wrhToIndex);
            Long parent = DataUtils.getLongQuietly(row, parentIndex);

            if (DataUtils.isId(docId) && isStockItem(item) && modifyDocumentStock(docId)) {
              if (event.isBefore()) {
                String message = verifyTradeItemInsert(docId, quantity, warehouseTo, parent);
                if (!BeeUtils.isEmpty(message)) {
                  event.addErrorMessage(message);
                }

              } else if (event.isAfter()) {
                event.addErrors(afterTradeItemInsert(docId, quantity, warehouseTo, parent,
                    row.getId()));
              }
            }

          } else if (event instanceof ViewUpdateEvent) {
            if (event.isBefore()) {
              columns = ((ViewUpdateEvent) event).getColumns();
              row = ((ViewUpdateEvent) event).getRow();

              if (row != null) {
                int index = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, columns);
                if (!BeeConst.isUndef(index)) {
                  event.addErrors(onTradeItemQuantityUpdate(row.getId(), row.getDouble(index)));
                }

                if (!event.hasErrors()) {
                  index = DataUtils.getColumnIndex(COL_TRADE_ITEM_WAREHOUSE_TO, columns);
                  if (!BeeConst.isUndef(index)) {
                    event.addErrors(onTradeItemWarehouseReceiverUpdate(row.getId(),
                        row.getLong(index)));
                  }
                }
              }
            }

          } else if (event instanceof ViewDeleteEvent) {
            Set<Long> ids = ((ViewDeleteEvent) event).getIds();

            if (!BeeUtils.isEmpty(ids)) {
              if (event.isBefore()) {
                ResponseObject responseObject = onDeleteTradeItems(ids);

                if (responseObject != null) {
                  event.addErrors(responseObject);
                  event.setUserObject(responseObject.getResponse());
                }

              } else if (event.isAfter()) {
                if (Action.REFRESH.equals(event.getUserObject())) {
                  refreshStock();
                }
              }
            }
          }

        } else if (event.isTarget(VIEW_TRADE_DOCUMENTS)) {
          if (event instanceof ViewUpdateEvent) {
            columns = ((ViewUpdateEvent) event).getColumns();
            row = ((ViewUpdateEvent) event).getRow();

            int index = DataUtils.getColumnIndex(COL_TRADE_WAREHOUSE_TO, columns);
            if (row != null && !BeeConst.isUndef(index)) {
              Long warehouse = row.getLong(index);

              if (event.isBefore()) {
                Set<Long> itemIds = getTradeDocumentStockItemsWithoutWarehouseReceiver(row.getId());

                if (!BeeUtils.isEmpty(itemIds)) {
                  if (DataUtils.isId(warehouse)) {
                    event.setUserObject(DataUtils.buildIdList(itemIds));
                  } else {
                    event.addErrorMessage("warehouse-receiver required for items");
                    event.addErrorMessage(DataUtils.buildIdList(itemIds));
                  }
                }

              } else if (event.isAfter() && DataUtils.isId(warehouse)
                  && (event.getUserObject() instanceof String)) {

                Set<Long> itemIds = DataUtils.parseIdSet((String) event.getUserObject());
                if (!BeeUtils.isEmpty(itemIds)) {
                  SqlUpdate update = new SqlUpdate(TBL_TRADE_STOCK)
                      .addConstant(COL_STOCK_WAREHOUSE, warehouse)
                      .setWhere(SqlUtils.inList(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, itemIds));

                  ResponseObject response = qs.updateDataWithResponse(update);
                  if (response.hasErrors()) {
                    event.addErrors(response);
                  } else {
                    refreshStock();
                  }
                }
              }
            }

          } else if (event instanceof ViewDeleteEvent) {
            Set<Long> ids = ((ViewDeleteEvent) event).getIds();

            if (!BeeUtils.isEmpty(ids)) {
              if (event.isBefore()) {
                ResponseObject responseObject = onDeleteTradeDocuments(ids);

                if (responseObject != null) {
                  event.addErrors(responseObject);
                  event.setUserObject(responseObject.getResponse());
                }

              } else if (event.isAfter()) {
                if (Action.REFRESH.equals(event.getUserObject())) {
                  refreshStock();
                }
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setItemStock(ViewQueryEvent event) {
        if (event.isAfter(VIEW_ITEMS, VIEW_ITEM_SELECTION)
            && usr.isDataVisible(VIEW_TRADE_STOCK)
            && DataUtils.containsNull(event.getRowset(), COL_ITEM_IS_SERVICE)) {

          BeeRowSet rowSet = event.getRowset();
          Multimap<Long, Triplet<Long, String, Double>> stock =
              getStockByWarehouse(rowSet.getRowIds());

          if (!stock.isEmpty()) {
            for (BeeRow row : rowSet) {
              if (stock.containsKey(row.getId())) {
                stock.get(row.getId()).forEach(value ->
                    row.setProperty(keyStockWarehouse(value.getB()), value.getC()));
              }
            }

            if (event.isTarget(VIEW_ITEM_SELECTION)) {
              Map<Long, String> warehouses = new HashMap<>();
              stock.values().forEach(value -> warehouses.put(value.getA(), value.getB()));

              rowSet.setTableProperty(PROP_WAREHOUSES, Codec.beeSerialize(warehouses));

              int serviceTagIndex = rowSet.getColumnIndex(COL_ITEM_IS_SERVICE);

              Set<Long> items = rowSet.getRows().stream()
                  .filter(row -> row.isNull(serviceTagIndex))
                  .map(AbstractRow::getId)
                  .collect(Collectors.toSet());

              warehouses.forEach((warehouseId, warehouseCode) -> {
                Multimap<Long, ItemQuantities> reservations = getReservations(warehouseId, items);
                Map<Long, Double> averageCost = getAverageCost(warehouseId, items);

                if (!reservations.isEmpty() || !averageCost.isEmpty()) {
                  int costScale = sys.getFieldScale(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST);

                  for (BeeRow row : rowSet) {
                    long id = row.getId();

                    if (reservations.containsKey(id)) {
                      double reserved = reservations.get(id).stream()
                          .mapToDouble(ItemQuantities::getReserved).sum();

                      if (reserved > 0) {
                        row.setProperty(keyReservedWarehouse(warehouseCode), reserved);
                      }
                    }

                    Double cost = averageCost.get(id);
                    if (BeeUtils.isDouble(cost)) {
                      row.setProperty(keyCostWarehouse(warehouseCode),
                          BeeUtils.toString(cost, costScale));
                    }
                  }
                }
              });
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillDocumentNumber(ViewModifyEvent event) {
        if (event instanceof InsertOrUpdate && event.isBefore(VIEW_TRADE_DOCUMENTS)) {
          List<BeeColumn> columns = ((InsertOrUpdate) event).getColumns();
          IsRow row = ((InsertOrUpdate) event).getRow();

          int seriesIndex = DataUtils.getColumnIndex(COL_TRADE_SERIES, columns);
          int numberIndex = DataUtils.getColumnIndex(COL_TRADE_NUMBER, columns);

          String series = DataUtils.getStringQuietly(row, seriesIndex);
          String number = DataUtils.getStringQuietly(row, numberIndex);

          if (!BeeUtils.isEmpty(series) && BeeUtils.isEmpty(number)) {
            SqlSelect query = new SqlSelect()
                .addFields(TBL_TRADE_SERIES,
                    COL_SERIES_NUMBER_PREFIX, COL_SERIES_NUMBER_LENGTH)
                .addFrom(TBL_TRADE_SERIES)
                .setWhere(SqlUtils.equals(TBL_TRADE_SERIES, COL_SERIES_NAME, series));

            SimpleRowSet seriesData = qs.getData(query);

            if (!DataUtils.isEmpty(seriesData)) {
              int dateIndex = DataUtils.getColumnIndex(COL_TRADE_DATE, columns);
              DateTime date = DataUtils.getDateTimeQuietly(row, dateIndex);

              number = getNextDocumentNumber(row.getId(), date, series,
                  seriesData.getValue(0, COL_SERIES_NUMBER_PREFIX),
                  seriesData.getInt(0, COL_SERIES_NUMBER_LENGTH));

              if (!BeeUtils.isEmpty(number)) {
                if (row.isIndex(numberIndex)) {
                  row.setValue(numberIndex, number);
                } else {
                  BeeColumn col = sys.getView(VIEW_TRADE_DOCUMENTS).getBeeColumn(COL_TRADE_NUMBER);
                  ((InsertOrUpdate) event).addValue(col, new TextValue(number));
                }
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setTradeDocumentSums(ViewQueryEvent event) {
        if (event.isAfter(VIEW_TRADE_DOCUMENTS) && !DataUtils.isEmpty(event.getRowset())) {
          BeeRowSet docData = event.getRowset();

          int operationTypeIndex = docData.getColumnIndex(COL_OPERATION_TYPE);

          for (int index = 0; index < docData.getNumberOfRows(); index++) {
            BeeRow docRow = docData.getRow(index);
            long docId = docRow.getId();

            BeeRowSet itemData = qs.getViewData(VIEW_TRADE_DOCUMENT_ITEMS,
                Filter.equals(COL_TRADE_DOCUMENT, docId), null, TradeDocumentSums.ITEM_COLUMNS);
            BeeRowSet paymentData = qs.getViewData(VIEW_TRADE_PAYMENTS,
                Filter.equals(COL_TRADE_DOCUMENT, docId), null, TradeDocumentSums.PAYMENT_COLUMNS);

            TradeDocumentSums tds = TradeDocumentSums.of(docData, index, itemData, paymentData);

            if (tds != null) {
              double vat = tds.getVat();
              double total = tds.getTotal();
              double paid = tds.getPaid();

              docRow.setNonZero(PROP_TD_AMOUNT, tds.getAmount());
              docRow.setNonZero(PROP_TD_DISCOUNT, tds.getDiscount());
              docRow.setNonZero(PROP_TD_WITHOUT_VAT, total - vat);
              docRow.setNonZero(PROP_TD_VAT, vat);
              docRow.setNonZero(PROP_TD_TOTAL, total);

              boolean ok = BeeUtils.isPositive(paid);
              if (!ok) {
                OperationType opType = docRow.getEnum(operationTypeIndex, OperationType.class);
                ok = opType != null && opType.hasDebt();
              }

              if (ok) {
                docRow.setNonZero(PROP_TD_PAID, paid);
                docRow.setNonZero(PROP_TD_DEBT, total - paid);
              }
            }
          }
        }
      }
    });

    MenuService.TRADE_DOCUMENTS.setTransformer(input -> {
      List<Menu> result = new ArrayList<>();

      if (input instanceof MenuItem) {
        BeeRowSet data = qs.getViewData(VIEW_TRADE_DOCUMENT_TYPES);

        if (!DataUtils.isEmpty(data)) {
          String language = usr.getLanguage();

          for (BeeRow row : data) {
            long id = row.getId();

            MenuItem item = (MenuItem) input.copy();

            item.setName(BeeUtils.join(BeeConst.STRING_UNDER, input.getName(), id));
            item.setLabel(DataUtils.getTranslation(data, row, COL_DOCUMENT_TYPE_NAME, language));

            item.setParameters(BeeUtils.toString(id));

            result.add(item);
          }
        }
      }

      return result;
    });

    BeeView.registerConditionProvider(FILTER_ITEM_HAS_STOCK, (view, args) -> {
      Set<Long> warehouses = new HashSet<>();

      if (!BeeUtils.isEmpty(args)) {
        for (String arg : args) {
          Long warehouse = BeeUtils.toLongOrNull(arg);
          if (DataUtils.isId(warehouse)) {
            warehouses.add(warehouse);
          }
        }
      }

      SqlSelect query = new SqlSelect().setDistinctMode(true)
          .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM)
          .addFrom(TBL_TRADE_STOCK)
          .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
              TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM));

      IsCondition qtyWhere = SqlUtils.positive(TBL_TRADE_STOCK, COL_STOCK_QUANTITY);

      if (warehouses.isEmpty()) {
        query.setWhere(qtyWhere);
      } else {
        query.setWhere(SqlUtils.and(qtyWhere,
            SqlUtils.inList(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE, warehouses)));
      }

      return SqlUtils.in(view.getSourceAlias(), view.getSourceIdName(), query);
    });

    BeeView.registerConditionProvider(FILTER_USER_TRADE_SERIES, (view, args) -> {
      Long manager;

      if (BeeUtils.isEmpty(args)) {
        manager = usr.getCurrentUserId();
      } else {
        manager = args.stream()
            .map(BeeUtils::toLongOrNull)
            .filter(DataUtils::isId)
            .findFirst().orElse(null);
      }

      String tblName = view.getSourceAlias();

      if (DataUtils.isId(manager) && TBL_TRADE_SERIES.equals(tblName)) {
        String idName = view.getSourceIdName();

        SqlSelect query = new SqlSelect().setDistinctMode(true)
            .addFields(tblName, idName)
            .addFrom(tblName)
            .addFromLeft(TBL_SERIES_MANAGERS,
                SqlUtils.join(tblName, idName, TBL_SERIES_MANAGERS, COL_SERIES))
            .setWhere(SqlUtils.or(
                SqlUtils.equals(TBL_SERIES_MANAGERS, COL_SERIES_MANAGER, manager),
                SqlUtils.isNull(TBL_SERIES_MANAGERS, COL_SERIES_MANAGER)));

        Set<Long> series = qs.getLongSet(query);

        if (BeeUtils.isEmpty(series)) {
          return SqlUtils.sqlFalse();
        } else {
          return SqlUtils.inList(tblName, idName, series);
        }

      } else {
        return SqlUtils.sqlTrue();
      }
    });

    BeeView.registerConditionProvider(FILTER_HAS_TRADE_DEBT, (view, args) -> {
      int index = 0;
      DebtKind debtKind = Codec.unpack(DebtKind.class, BeeUtils.getQuietly(args, index++));

      if (debtKind != null) {
        Long company = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, index++));
        Long currency = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, index++));

        Long dateTo = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, index++));
        Long termTo = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, index));

        String source = TBL_TRADE_DOCUMENTS;
        String idName = sys.getIdName(source);

        HasConditions where = SqlUtils.and();
        where.add(SqlUtils.inList(source, COL_TRADE_OPERATION, getOperationsByDebtKind(debtKind)));

        if (DataUtils.isId(company)) {
          where.add(
              SqlUtils.or(
                  SqlUtils.equals(source, COL_TRADE_PAYER, company),
                  SqlUtils.and(
                      SqlUtils.isNull(source, COL_TRADE_PAYER),
                      SqlUtils.equals(source, debtKind.tradeDocumentCompanyColumn(), company))));
        }

        if (DataUtils.isId(currency)) {
          where.add(SqlUtils.equals(source, COL_TRADE_CURRENCY, currency));
        }

        if (dateTo != null) {
          where.add(SqlUtils.less(source, COL_TRADE_DATE, dateTo));
        }

        if (termTo != null) {
          where.add(
              SqlUtils.or(
                  SqlUtils.less(source, COL_TRADE_TERM, termTo),
                  SqlUtils.in(source, idName, TBL_TRADE_PAYMENT_TERMS, COL_TRADE_DOCUMENT,
                      SqlUtils.less(TBL_TRADE_PAYMENT_TERMS, COL_TRADE_PAYMENT_TERM_DATE,
                          termTo))));
        }

        SqlSelect query = new SqlSelect()
            .addFields(source, idName)
            .addFields(source, TradeDocumentSums.DOCUMENT_COLUMNS)
            .addFrom(source)
            .setWhere(where);

        SimpleRowSet docData = qs.getData(query);

        if (!DataUtils.isEmpty(docData)) {
          Set<Long> docIds = new HashSet<>();

          for (SimpleRow docRow : docData) {
            long docId = docRow.getLong(idName);

            TradeVatMode vatMode = docRow.getEnum(COL_TRADE_DOCUMENT_VAT_MODE, TradeVatMode.class);
            TradeDiscountMode discountMode = docRow.getEnum(COL_TRADE_DOCUMENT_DISCOUNT_MODE,
                TradeDiscountMode.class);
            Double docDiscount = docRow.getDouble(COL_TRADE_DOCUMENT_DISCOUNT);

            BeeRowSet itemData = qs.getViewData(VIEW_TRADE_DOCUMENT_ITEMS,
                Filter.equals(COL_TRADE_DOCUMENT, docId), null, TradeDocumentSums.ITEM_COLUMNS);
            BeeRowSet paymentData = qs.getViewData(VIEW_TRADE_PAYMENTS,
                Filter.equals(COL_TRADE_DOCUMENT, docId), null, TradeDocumentSums.PAYMENT_COLUMNS);

            TradeDocumentSums tds = new TradeDocumentSums(vatMode, discountMode, docDiscount);

            if (!DataUtils.isEmpty(itemData)) {
              tds.addItems(itemData);
            }
            if (!DataUtils.isEmpty(paymentData)) {
              tds.addPayments(paymentData);
            }

            if (BeeUtils.nonZero(tds.getDebt())) {
              docIds.add(docId);
            }
          }

          if (!docIds.isEmpty()) {
            return SqlUtils.inList(source, idName, docIds);
          }
        }
      }

      return SqlUtils.sqlFalse();
    });

    registerStockReservationsProvider(ModuleAndSub.of(getModule()),
        new StockReservationsProvider() {
          @Override
          public Map<String, Double> getItemReservationsInfo(Long warehouse, Long item,
              DateTime dateTo) {
            return getTradeReservationsInfo(warehouse, item, dateTo);
          }

          @Override
          public Multimap<Long, ItemQuantities> getStockReservations(Long warehouse,
              Collection<Long> items) {
            return getTradeReservations(warehouse, items);
          }
        });

    act.init();
  }

  public static void registerStockReservationsProvider(ModuleAndSub moduleAndSub,
      StockReservationsProvider provider) {
    stockReservationsProviders.put(Assert.notNull(moduleAndSub), Assert.notNull(provider));
  }

  private ResponseObject getDocumentTypeCaptionAndFilter(RequestInfo reqInfo) {
    Long typeId = reqInfo.getParameterLong(COL_DOCUMENT_TYPE);
    if (!DataUtils.isId(typeId)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_DOCUMENT_TYPE);
    }

    BeeRowSet typeData = qs.getViewData(VIEW_TRADE_DOCUMENT_TYPES, Filter.compareId(typeId));
    if (DataUtils.isEmpty(typeData)) {
      return ResponseObject.error(reqInfo.getLabel(), typeId, "not found");
    }

    BeeRow typeRow = typeData.getRow(0);

    String caption = DataUtils.getTranslation(typeData, typeRow, COL_DOCUMENT_TYPE_NAME,
        usr.getLanguage());
    CompoundFilter filter = Filter.and();

    CompoundFilter statusFilter = Filter.or();

    for (TradeDocumentPhase phase : TradeDocumentPhase.values()) {
      Boolean v = DataUtils.getBoolean(typeData, typeRow, phase.getDocumentTypeColumnName());
      if (BeeUtils.isTrue(v)) {
        statusFilter.add(Filter.equals(COL_TRADE_DOCUMENT_PHASE, phase));
      }
    }

    if (!statusFilter.isEmpty()) {
      filter.add(statusFilter);
    }

    Set<Long> operations = qs.getLongSet(new SqlSelect()
        .addFields(TBL_TRADE_TYPE_OPERATIONS, COL_TRADE_OPERATION)
        .addFrom(TBL_TRADE_TYPE_OPERATIONS)
        .setWhere(SqlUtils.equals(TBL_TRADE_TYPE_OPERATIONS, COL_DOCUMENT_TYPE, typeId)));

    if (!BeeUtils.isEmpty(operations)) {
      filter.add(Filter.any(COL_TRADE_OPERATION, operations));
    }

    Set<Long> statuses = qs.getLongSet(new SqlSelect()
        .addFields(TBL_TRADE_TYPE_STATUSES, COL_TRADE_DOCUMENT_STATUS)
        .addFrom(TBL_TRADE_TYPE_STATUSES)
        .setWhere(SqlUtils.equals(TBL_TRADE_TYPE_STATUSES, COL_DOCUMENT_TYPE, typeId)));

    if (!BeeUtils.isEmpty(statuses)) {
      filter.add(Filter.any(COL_TRADE_DOCUMENT_STATUS, statuses));
    }

    Set<Long> tags = qs.getLongSet(new SqlSelect()
        .addFields(TBL_TRADE_TYPE_TAGS, COL_TRADE_TAG)
        .addFrom(TBL_TRADE_TYPE_TAGS)
        .setWhere(SqlUtils.equals(TBL_TRADE_TYPE_TAGS, COL_DOCUMENT_TYPE, typeId)));

    if (!BeeUtils.isEmpty(tags)) {
      filter.add(Filter.in(sys.getIdName(VIEW_TRADE_DOCUMENTS), VIEW_TRADE_DOCUMENT_TAGS,
          COL_TRADE_DOCUMENT, Filter.any(COL_TRADE_TAG, tags)));
    }

    if (filter.isEmpty()) {
      filter = null;
    }

    return ResponseObject.response(Pair.of(caption, filter));
  }

  private ResponseObject getItemsInfo(String viewName, Long id, String currencyTo) {
    if (!sys.isView(viewName)) {
      return ResponseObject.error("Wrong view name");
    }
    if (!DataUtils.isId(id)) {
      return ResponseObject.error("Wrong document ID");
    }
    String trade = sys.getView(viewName).getSourceName();
    String tradeItems;
    String itemsRelation;

    if (BeeUtils.same(trade, TBL_SALES)) {
      tradeItems = TBL_SALE_ITEMS;
      itemsRelation = COL_SALE;
    } else if (BeeUtils.same(trade, TBL_PURCHASES)) {
      tradeItems = TBL_PURCHASE_ITEMS;
      itemsRelation = COL_PURCHASE;
    } else {
      return ResponseObject.error("View source not supported:", trade);
    }
    SqlSelect query = new SqlSelect()
        .addFields(TBL_ITEMS,
            COL_ITEM_NAME, COL_ITEM_NAME + "2", COL_ITEM_NAME + "3", COL_ITEM_BARCODE)
        .addField(TBL_UNITS, COL_UNIT_NAME, COL_UNIT)
        .addFields(tradeItems, COL_ITEM_ARTICLE, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
            COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_TRADE_ITEM_NOTE)
        .addField(TBL_CURRENCIES, COL_CURRENCY_NAME, COL_CURRENCY)
        .addFrom(tradeItems)
        .addFromInner(trade, sys.joinTables(trade, tradeItems, itemsRelation))
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, tradeItems, COL_ITEM))
        .addFromInner(TBL_UNITS, sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT))
        .addFromInner(TBL_CURRENCIES, sys.joinTables(TBL_CURRENCIES, trade, COL_CURRENCY))
        .setWhere(SqlUtils.equals(tradeItems, itemsRelation, id))
        .addOrder(tradeItems, COL_TRADE_ITEM_ORDINAL, sys.getIdName(tradeItems));

    if (!BeeUtils.isEmpty(currencyTo)) {
      String currAlias = SqlUtils.uniqueName();

      IsExpression xpr = ExchangeUtils.exchangeFieldTo(query.addFromLeft(TBL_CURRENCIES, currAlias,
          SqlUtils.equals(currAlias, COL_CURRENCY_NAME, currencyTo)),
          SqlUtils.constant(1),
          SqlUtils.field(trade, COL_CURRENCY),
          SqlUtils.field(trade, COL_TRADE_DATE),
          SqlUtils.field(currAlias, sys.getIdName(TBL_CURRENCIES)));

      query.addExpr(xpr, COL_CURRENCY_RATE)
          .addField(currAlias, COL_CURRENCY_NAME, COL_CURRENCY_RATE + COL_CURRENCY);
    }
    return ResponseObject.response(qs.getData(query));
  }

  private static Integer getOverdueInDays(BeeRowSet rs, IsRow row) {
    Integer overdue = null;

    if (rs == null || row == null) {
      return overdue;
    }

    int idxDate = rs.getColumnIndex(COL_TRADE_DATE);
    int idxTerm = rs.getColumnIndex(COL_TRADE_TERM);

    if (idxDate < 0) {
      return overdue;
    }

    if (idxTerm < 0) {
      idxTerm = idxDate;
    }

    int start;
    int end = (new JustDate()).getDays();

    if (row.getDate(idxTerm) != null) {
      start = row.getDate(idxTerm).getDays();
    } else if (row.getDateTime(idxDate) != null) {
      start = row.getDateTime(idxDate).getDate().getDays();
    } else {
      return overdue;
    }

    overdue = end - start;

    return overdue;
  }

  private ResponseObject getTradeDocumentData(RequestInfo reqInfo) {
    Long docId = reqInfo.getParameterLong(Service.VAR_ID);
    if (!DataUtils.isId(docId)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), Service.VAR_ID);
    }

    String itemViewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(itemViewName)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), Service.VAR_VIEW_NAME);
    }

    String itemRelation = reqInfo.getParameter(Service.VAR_COLUMN);
    if (BeeUtils.isEmpty(itemRelation)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), Service.VAR_COLUMN);
    }

    Set<Long> companyIds = DataUtils.parseIdSet(reqInfo.getParameter(VIEW_COMPANIES));

    BeeRowSet companies;
    BeeRowSet bankAccounts;

    if (companyIds.isEmpty()) {
      companies = null;
      bankAccounts = null;

    } else {
      companies = qs.getViewData(VIEW_COMPANIES, Filter.idIn(companyIds));
      bankAccounts = qs.getViewData(VIEW_COMPANY_BANK_ACCOUNTS,
          Filter.any(COL_COMPANY, companyIds));
    }

    BeeRowSet items = qs.getViewData(itemViewName, Filter.equals(itemRelation, docId));

    TradeDocumentData tdd = new TradeDocumentData(companies, bankAccounts, items, null, null);
    return ResponseObject.response(tdd);
  }

  private Document renderCompanyDebtMail(String subject, String p1,
      String p2, Long companyId) {
    Document doc = new Document();
    doc.getHead().append(meta().encodingDeclarationUtf8(), title().text(subject));
    doc.getBody().setColor("black");

    Pre pre = pre().text(p1);
    pre.setWordWrap(WordWrap.BREAK_WORD);
    pre.setFontFamily("sans-serif");
    pre.setFontSize(11, CssUnit.PT);
    String first = pre.build();

    pre = pre().text(p2);
    pre.setWordWrap(WordWrap.BREAK_WORD);
    pre.setFontFamily("sans-serif");
    pre.setFontSize(11, CssUnit.PT);
    String last = pre.build();

    Filter filter = Filter.and(
        Filter.or(Filter.and(
            Filter.isEqual(COL_TRADE_CUSTOMER, Value.getValue(companyId)), Filter.isNull(
                COL_SALE_PAYER)),
            Filter.isEqual(COL_SALE_PAYER, Value.getValue(companyId))),
        Filter.compareWithValue(COL_TRADE_DEBT, Operator.GT, Value.getValue(0)));

    if (BeeUtils.unbox(prm.getBoolean(PRM_OVERDUE_INVOICES))) {
      filter =
          Filter.and(filter, Filter.compareWithValue(COL_TRADE_TERM, Operator.LT, Value
              .getValue(TimeUtils.nowMillis())));
    }

    BeeRowSet rs = qs.getViewData(VIEW_DEBTS, filter, null,
        Lists.newArrayList(COL_TRADE_INVOICE_NO, COL_TRADE_DATE,
            COL_TRADE_TERM, COL_TRADE_AMOUNT, COL_TRADE_DEBT, ALS_CURRENCY_NAME,
            ALS_CUSTOMER_NAME, ALS_PAYER_NAME, COL_SERIES_NAME));

    if (rs.isEmpty()) {
      return null;
    }

    Map<String, Object> creditInfo = Maps.newHashMap();
    ResponseObject resp = getCreditInfo(companyId);
    Double debt = null;

    if (resp.getResponse() instanceof Map) {
      creditInfo = resp.getResponse(creditInfo, logger);
      if (creditInfo.get(VAR_DEBT) instanceof Double) {
        debt = (Double) creditInfo.get(VAR_DEBT);
      }
    }

    int ignoreLast = 3;

    Table table = table();
    Caption caption = caption()
        .text(BeeUtils.nvl(rs.getRows().get(0).getString(rs.getColumnIndex(ALS_CUSTOMER_NAME)), rs
            .getRows().get(0).getString(rs.getColumnIndex(ALS_PAYER_NAME))));

    caption.setTextAlign(TextAlign.LEFT);
    table.append(caption);
    Tr trHead = tr();

    for (int i = 0; i < rs.getNumberOfColumns() - ignoreLast; i++) {
      String label = Localized.maybeTranslate(rs.getColumnLabel(i), usr.getGlossary());

      if (BeeUtils.same(rs.getColumnId(i), COL_TRADE_INVOICE_NO)) {
        label = usr.getDictionary().trdInvoice();
      }

      if (BeeUtils.same(rs.getColumnId(i), COL_TRADE_AMOUNT)) {
        label = usr.getDictionary().trdAmount();
      }
      Th th = th().text(label);
      th.setBorderWidth("1px");
      th.setBorderStyle(BorderStyle.SOLID);
      th.setBorderColor("black");
      trHead.append(th);
    }

    Th th = th().text(usr.getDictionary().trdOverdueInDays());
    th.setBorderWidth("1px");
    th.setBorderStyle(BorderStyle.SOLID);
    th.setBorderColor("black");
    trHead.insert(rs.getColumnIndex(COL_TRADE_TERM) + 1, th);

    table.append(trHead);

    Range<Long> maybeTime = Range.closed(
        TimeUtils.startOfYear(TimeUtils.today(), -10).getTime(),
        TimeUtils.startOfYear(TimeUtils.today(), 100).getTime());

    DateTimeFormatInfo dateTimeFormatInfo = usr.getDateTimeFormatInfo();

    for (IsRow row : rs) {
      Tr tr = tr();

      for (int i = 0; i < rs.getNumberOfColumns() - ignoreLast; i++) {
        if (row.isNull(i)) {
          tr.append(td());
          continue;
        }

        ValueType type = rs.getColumnType(i);
        String value = DataUtils.render(rs.getColumn(i), row, i,
            Formatter.getDateRenderer(dateTimeFormatInfo),
            Formatter.getDateTimeRenderer(dateTimeFormatInfo));

        if (type == ValueType.LONG) {
          Long x = row.getLong(i);
          if (x != null && maybeTime.contains(x)) {
            type = ValueType.DATE_TIME;
            value = Formatter.renderDate(dateTimeFormatInfo, new JustDate(x));
          }
        }

        if (type == ValueType.DATE_TIME) {
          value = Formatter.renderDate(dateTimeFormatInfo, new JustDate(row.getLong(i)));
        }

        if (BeeUtils.same(rs.getColumnId(i), COL_TRADE_INVOICE_NO)) {
          type = ValueType.TEXT;

          int idxInvoicePref = rs.getColumnIndex(COL_SERIES_NAME);

          if (idxInvoicePref > -1
              && !BeeUtils.isEmpty(row.getString(idxInvoicePref))) {
            value = BeeUtils.joinWords(row.getString(idxInvoicePref),
                row.getString(i));
          }
        }

        Td td = td();
        tr.append(td);
        td.text(value);
        td.setPadding("0 5px 0 5px");

        if (ValueType.isNumeric(type) || ValueType.TEXT == type
            && CharMatcher.digit().matchesAnyOf(value) && BeeUtils.isDouble(value)) {
          if (!BeeUtils.same(rs.getColumnId(i), COL_TRADE_INVOICE_NO)) {
            td.setTextAlign(TextAlign.RIGHT);
          }
        }

      }

      Integer overdue = getOverdueInDays(rs, row);
      Td td = td();
      tr.insert(rs.getColumnIndex(COL_TRADE_TERM) + 1, td);
      td.text(overdue == null ? BeeConst.STRING_EMPTY : BeeUtils.toString(overdue));
      td.setPadding("0 5px 0 5px");
      td.setTextAlign(TextAlign.RIGHT);

      table.append(tr);
    }

    Tr footer = tr();
    for (int i = 0; i < rs.getNumberOfColumns() - ignoreLast - 3; i++) {
      footer.append(td());
    }
    footer.append(td());

    footer.append(td().append(b().text(usr.getDictionary().total())));
    footer.append(td().text(BeeUtils.notEmpty(BeeUtils.toString(debt), BeeConst.STRING_EMPTY)));
    table.append(footer);
    footer.append(td());

    table.setBorderWidth("1px;");
    table.setBorderStyle(BorderStyle.NONE);
    table.setBorderSpacing("0px;");
    table.setFontFamily("sans-serif");
    table.setFontSize(10, CssUnit.PT);

    doc.getBody().append(p().text(first));
    doc.getBody().append(table);
    doc.getBody().append(p().text(last));
    return doc;
  }

  private ResponseObject sendDebtsRemindEmail(RequestInfo req) {
    Long senderMailAccountId = mail.getSenderAccountId(SVC_REMIND_DEBTS_EMAIL);
    ResponseObject resp = ResponseObject.emptyResponse();

    if (!DataUtils.isId(senderMailAccountId)) {
      return ResponseObject.error(usr.getDictionary().mailAccountNotFound());
    }

    String subject = req.getParameter(VAR_SUBJECT);
    String p1 = req.getParameter(VAR_HEADER);
    String p2 = req.getParameter(VAR_FOOTER);
    List<Long> ids = DataUtils.parseIdList(req.getParameter(VAR_ID_LIST));

    Multimap<Long, String> emails = getCompaniesRemindEmailAddresses(ids);
    Map<Long, String> errorMails = Maps.newHashMap();
    Set<Long> sentEmailCompanyIds = Sets.newHashSet();

    for (Long companyId : emails.keySet()) {
      if (BeeUtils.isEmpty(emails.get(companyId))) {
        errorMails.put(companyId, usr.getDictionary().mailRecipientAddressNotFound());
        continue;
      }

      Document mailDocument = renderCompanyDebtMail(subject, p1, p2, companyId);

      if (mailDocument == null) {
        errorMails.put(companyId, usr.getDictionary().noData());
        continue;
      }

      try {
        ResponseObject response = mail.sendMail(senderMailAccountId, ArrayUtils.toArray(
            Lists.newArrayList(emails.get(companyId))), null, null, subject, mailDocument
            .buildLines(), null, true);

        if (response.hasWarnings()) {
          resp.addWarning((Object) response.getWarnings());
        }
        sentEmailCompanyIds.add(companyId);
      } catch (BeeRuntimeException ex) {
        logger.error(ex);
        errorMails.put(companyId, ex.getMessage());
      }
    }

    String message = BeeUtils.joinWords(usr.getDictionary().mailMessageSentCount(),
        sentEmailCompanyIds.size(), br().build());

    if (!BeeUtils.isEmpty(errorMails.keySet())) {
      message = BeeUtils.joinWords(message, usr.getDictionary().errors(), br().build());

      Filter filter = Filter.idIn(errorMails.keySet());
      BeeRowSet rs =
          qs.getViewData(VIEW_COMPANIES, filter, null, Lists.newArrayList(COL_COMPANY_NAME));
      int i = 0;
      for (Long id : errorMails.keySet()) {

        message = BeeUtils.joinWords(message,
            rs.getRowById(id).getString(rs.getColumnIndex(COL_COMPANY_NAME)), errorMails.get(id),
            br().build());

        if (i > 5) {
          message = BeeUtils.joinWords(message, br().build(),
              BeeUtils.bracket(BeeConst.ELLIPSIS), errorMails.keySet().size() - i);
          break;
        }
        i++;
      }
    }

    resp.setResponse(message);

    return resp;
  }

  private void importERPPayments() {
    int multiplier = prm.getInteger(PRM_MULTIPLIER);
    long historyId = sys.eventStart(PRM_ERP_REFRESH_INTERVAL);
    int c = 0;

    for (String table : new String[] {TBL_SALES, TBL_PURCHASES}) {
      String idName = sys.getIdName(table);

      SimpleRowSet debts = qs.getData(new SqlSelect()
          .addFields(table, idName, COL_TRADE_PAID)
          .addFrom(table)
          .setWhere(SqlUtils.and(SqlUtils.notNull(table, COL_TRADE_EXPORTED),
              SqlUtils.or(SqlUtils.isNull(table, COL_TRADE_PAID),
                  SqlUtils.less(table, COL_TRADE_PAID, SqlUtils.field(table, COL_TRADE_AMOUNT))))));

      if (!debts.isEmpty()) {
        StringBuilder ids = new StringBuilder();

        for (Long id : debts.getLongColumn(idName)) {
          if (ids.length() > 0) {
            ids.append(",");
          }
          ids.append("'").append(TradeModuleBean.encodeId(table, id * multiplier)).append("'");
        }
        String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
        String remoteLogin = prm.getText(PRM_ERP_LOGIN);
        String remotePassword = prm.getText(PRM_ERP_PASSWORD);

        try {
          SimpleRowSet payments = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
              .getSQLData("SELECT extern_id AS id,"
                      + " CASE WHEN oper_apm IS NULL THEN data ELSE apm_data END AS data,"
                      + " CASE WHEN oper_apm IS NULL THEN viso ELSE apm_suma END AS suma"
                      + " FROM apyvarta"
                      + " INNER JOIN operac ON apyvarta.operacija = operac.operacija"
                      + " AND extern_id IN(" + ids.toString() + ")",
                  "id", "data", "suma");

          for (SimpleRow payment : payments) {
              Long id = TradeModuleBean.decodeId(table, payment.getLong("id")) / multiplier;
              Double paid = payment.getDouble("suma");
            if (!Objects.equals(paid, BeeUtils.toDoubleOrNull(debts.getValueByKey(idName,
                BeeUtils.toString(id), COL_TRADE_PAID)))) {

              c += qs.updateData(new SqlUpdate(table)
                  .addConstant(COL_TRADE_PAID, paid)
                  .addConstant(COL_TRADE_PAYMENT_TIME,
                      TimeUtils.parseDateTime(payment.getValue("data"), DateOrdering.YMD))
                  .setWhere(SqlUtils.equals(table, idName, id)));
            }
          }
        } catch (BeeException e) {
          logger.error(e);
          sys.eventError(historyId, e);
          return;
        }
      }
    }
    sys.eventEnd(historyId, "OK", BeeUtils.joinWords("Updated", c, "records"));
  }

  private ResponseObject sendToERP(String viewName, Set<Long> ids) {
    if (!sys.isView(viewName)) {
      return ResponseObject.error("Wrong view name");
    }
    String trade = sys.getView(viewName).getSourceName();
    String tradeItems;
    String itemsRelation;

    SqlSelect query = new SqlSelect()
        .addFields(trade, COL_TRADE_DATE, COL_TRADE_INVOICE_NO,
            COL_TRADE_NUMBER, COL_TRADE_TERM, COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER)
        .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_NAME)
        .addField(TBL_CURRENCIES, COL_CURRENCY_NAME, COL_CURRENCY)
        .addField(COL_TRADE_WAREHOUSE_FROM, COL_WAREHOUSE_CODE, COL_TRADE_WAREHOUSE_FROM)
        .addFields(PayrollConstants.TBL_EMPLOYEES, PayrollConstants.COL_TAB_NUMBER)
        .addFrom(trade)
        .addFromLeft(TBL_TRADE_OPERATIONS,
            sys.joinTables(TBL_TRADE_OPERATIONS, trade, COL_TRADE_OPERATION))
        .addFromLeft(TBL_CURRENCIES, sys.joinTables(TBL_CURRENCIES, trade, COL_CURRENCY))
        .addFromLeft(TBL_WAREHOUSES, COL_TRADE_WAREHOUSE_FROM,
            sys.joinTables(TBL_WAREHOUSES, COL_TRADE_WAREHOUSE_FROM, trade,
                COL_TRADE_WAREHOUSE_FROM))
        .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, trade, COL_TRADE_MANAGER))
        .addFromLeft(PayrollConstants.TBL_EMPLOYEES,
            SqlUtils.joinUsing(TBL_USERS, PayrollConstants.TBL_EMPLOYEES, COL_COMPANY_PERSON))
        .setWhere(sys.idInList(trade, ids));

    switch (trade) {
      case TBL_SALES:
        tradeItems = TBL_SALE_ITEMS;
        itemsRelation = COL_SALE;

        query.addField(TBL_SALES_SERIES, COL_SERIES_NAME, COL_TRADE_INVOICE_PREFIX)
            .addFields(trade, COL_SALE_PAYER, COL_TRADE_BOL_NUMBER, COL_TRADE_BOL_LOADING,
                COL_TRADE_BOL_UNLOADING, COL_TRADE_BOL_VEHICLE_NUMBER, COL_TRADE_BOL_DRIVER,
                COL_TRADE_BOL_CARRIER, COL_TRADE_BOL_ISSUE_DATE, COL_TRADE_BOL_DEPARTURE_DATE,
                COL_TRADE_BOL_UNLOADING_DATE)
            .addField(OrdersConstants.TBL_ORDER_SERIES, COL_SERIES_NAME, ALS_TRADE_BOL_SERIES)
            .addField(ALS_TRADE_BOL_DRIVER_EMPLOYEES, PayrollConstants.COL_TAB_NUMBER,
                ALS_TRADE_BOL_DRIVER_TAB_NO)
            .addFromLeft(TBL_SALES_SERIES,
                sys.joinTables(TBL_SALES_SERIES, trade, COL_TRADE_SALE_SERIES))
            .addFromLeft(OrdersConstants.TBL_ORDER_SERIES,
                sys.joinTables(OrdersConstants.TBL_ORDER_SERIES, trade, COL_TRADE_BOL_SERIES))
            .addFromLeft(PayrollConstants.TBL_EMPLOYEES, ALS_TRADE_BOL_DRIVER_EMPLOYEES,
                SqlUtils.join(ALS_TRADE_BOL_DRIVER_EMPLOYEES,
                    sys.getIdName(PayrollConstants.TBL_EMPLOYEES), trade,
                    COL_TRADE_BOL_DRIVER_TAB_NO));

        break;

      case TBL_PURCHASES:
        tradeItems = TBL_PURCHASE_ITEMS;
        itemsRelation = COL_PURCHASE;

        query.addFields(trade, COL_TRADE_INVOICE_PREFIX)
            .addField(COL_TRADE_WAREHOUSE_TO, COL_WAREHOUSE_CODE, COL_TRADE_WAREHOUSE_TO)
            .addFromLeft(TBL_WAREHOUSES, COL_TRADE_WAREHOUSE_TO,
                sys.joinTables(TBL_WAREHOUSES, COL_TRADE_WAREHOUSE_TO, trade,
                    COL_TRADE_WAREHOUSE_TO));
        break;

      default:
        return ResponseObject.error("View source not supported:", trade);
    }
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    SimpleRowSet invoices = qs.getData(query.addField(trade, sys.getIdName(trade), itemsRelation));

    Map<Long, String> companies = new HashMap<>();
    ResponseObject response = ResponseObject.emptyResponse();

    for (SimpleRow invoice : invoices) {
      for (String col : new String[] {
          COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER, COL_SALE_PAYER,
          COL_TRADE_BOL_CARRIER}) {
        Long id = invoices.hasColumn(col) ? invoice.getLong(col) : null;

        if (DataUtils.isId(id) && !companies.containsKey(id)) {
          SimpleRow data = qs.getRow(new SqlSelect()
              .addFields(TBL_COMPANIES, COL_COMPANY_NAME, COL_COMPANY_CODE, COL_COMPANY_VAT_CODE)
              .addField(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME, COL_COMPANY_TYPE)
              .addFields(TBL_CONTACTS, COL_ADDRESS, COL_POST_INDEX)
              .addField(TBL_CITIES, COL_CITY_NAME, COL_CITY)
              .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, COL_COUNTRY)
              .addFrom(TBL_COMPANIES)
              .addFromLeft(TBL_COMPANY_TYPES,
                  sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE))
              .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
              .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CONTACTS, COL_CITY))
              .addFromLeft(TBL_COUNTRIES, sys.joinTables(TBL_COUNTRIES, TBL_CONTACTS, COL_COUNTRY))
              .setWhere(sys.idEquals(TBL_COMPANIES, id)));

          try {
            String company = BeeUtils.joinItems(data.getValue(COL_COMPANY_NAME),
                data.getValue(COL_COMPANY_TYPE));

            company = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
                .importClient(BeeUtils.toString(id), company, data.getValue(COL_COMPANY_CODE),
                    data.getValue(COL_COMPANY_VAT_CODE), data.getValue(COL_ADDRESS),
                    data.getValue(COL_POST_INDEX), data.getValue(COL_CITY),
                    data.getValue(COL_COUNTRY));

            companies.put(id, company);

          } catch (BeeException e) {
            response.addError(e);
          }
        }
      }
      if (response.hasErrors()) {
        break;
      }
      String warehouse;
      String client;

      if (invoices.hasColumn(COL_TRADE_WAREHOUSE_TO)) {
        warehouse = invoice.getValue(COL_TRADE_WAREHOUSE_TO);
        client = companies.get(invoice.getLong(COL_TRADE_SUPPLIER));
      } else {
        warehouse = invoice.getValue(COL_TRADE_WAREHOUSE_FROM);
        client = companies.get(invoice.getLong(COL_TRADE_CUSTOMER));
      }
      int multiplier = prm.getInteger(PRM_MULTIPLIER);
      WSDocument doc = new WSDocument(encodeId(trade, invoice.getLong(itemsRelation) * multiplier),
          invoice.getDateTime(COL_TRADE_DATE), invoice.getValue(COL_OPERATION_NAME), client,
          warehouse);

      if (invoices.hasColumn(COL_SALE_PAYER)) {
        doc.setPayer(companies.get(invoice.getLong(COL_SALE_PAYER)));
      }
      doc.setNumber(invoice.getValue(COL_TRADE_NUMBER));
      doc.setInvoice(invoice.getValue(COL_TRADE_INVOICE_PREFIX),
          invoice.getValue(COL_TRADE_INVOICE_NO));
      doc.setSupplier(companies.get(invoice.getLong(COL_TRADE_SUPPLIER)));
      doc.setCustomer(companies.get(invoice.getLong(COL_TRADE_CUSTOMER)));
      doc.setTerm(invoice.getDate(COL_TRADE_TERM));
      doc.setCurrency(invoice.getValue(COL_CURRENCY));
      doc.setManager(invoice.getValue(PayrollConstants.COL_TAB_NUMBER));

      if (Objects.equals(trade, TBL_SALES)) {
        doc.setBolNumber(invoice.getValue(COL_TRADE_BOL_NUMBER));
        doc.setBolSeries(invoice.getValue(ALS_TRADE_BOL_SERIES));
        doc.setBolLoadingPlace(invoice.getValue(COL_TRADE_BOL_LOADING));
        doc.setBolUnloadingPlace(invoice.getValue(COL_TRADE_BOL_UNLOADING));
        doc.setBolVehicleNumber(invoice.getValue(COL_TRADE_BOL_VEHICLE_NUMBER));
        doc.setBolDriver(invoice.getValue(COL_TRADE_BOL_DRIVER));
        doc.setBolDriverTabNo(invoice.getValue(ALS_TRADE_BOL_DRIVER_TAB_NO));
        doc.setBolCarrier(companies.get(invoice.getLong(COL_TRADE_BOL_CARRIER)));

        DateTimeFormatInfo dtfInfo = SupportedLocale.LT.getDateTimeFormatInfo();

        DateTime dt;
        if (!BeeUtils.isEmpty(invoice.getValue(COL_TRADE_BOL_ISSUE_DATE))) {
          dt = new DateTime(BeeUtils.toLong(invoice.getValue(COL_TRADE_BOL_ISSUE_DATE)));
          doc.setBolIssueDate(Formatter.renderDateTime(dtfInfo, dt));
        }

        if (!BeeUtils.isEmpty(invoice.getValue(COL_TRADE_BOL_DEPARTURE_DATE))) {
          dt = new DateTime(BeeUtils.toLong(invoice.getValue(COL_TRADE_BOL_DEPARTURE_DATE)));
          doc.setBolDepartureDate(Formatter.renderDateTime(dtfInfo, dt));
        }

        if (!BeeUtils.isEmpty(invoice.getValue(COL_TRADE_BOL_UNLOADING_DATE))) {
          dt = new DateTime(BeeUtils.toLong(invoice.getValue(COL_TRADE_BOL_UNLOADING_DATE)));
          doc.setBolUnloadingDate(Formatter.renderDateTime(dtfInfo, dt));
        }
      }

      SimpleRowSet items = qs.getData(new SqlSelect()
          .addFields(TBL_ITEMS, COL_ITEM_NAME, COL_ITEM_EXTERNAL_CODE)
          .addFields(tradeItems, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE, COL_TRADE_VAT_PLUS,
              COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_TRADE_ITEM_ARTICLE, COL_TRADE_ITEM_NOTE,
              COL_TRADE_DISCOUNT)
          .addFrom(tradeItems)
          .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, tradeItems, COL_ITEM))
          .setWhere(SqlUtils.equals(tradeItems, itemsRelation, invoice.getLong(itemsRelation))));

      for (SimpleRow item : items) {
        if (BeeUtils.isEmpty(item.getValue(COL_ITEM_EXTERNAL_CODE))) {
          response.addError("Item", BeeUtils.bracket(item.getValue(COL_ITEM_NAME)),
              "does not have ERP code");
          break;
        }
        WSDocumentItem wsItem = doc.addItem(item.getValue(COL_ITEM_EXTERNAL_CODE),
            item.getValue(COL_TRADE_ITEM_QUANTITY));

        wsItem.setPrice(item.getValue(COL_TRADE_ITEM_PRICE));
        wsItem.setDiscount(item.getValue(COL_TRADE_DISCOUNT), true);
        wsItem.setVat(item.getValue(COL_TRADE_VAT), item.getBoolean(COL_TRADE_VAT_PERC),
            item.getBoolean(COL_TRADE_VAT_PLUS));
        wsItem.setArticle(item.getValue(COL_TRADE_ITEM_ARTICLE));
        wsItem.setNote(item.getValue(COL_TRADE_ITEM_NOTE));
      }
      if (response.hasErrors()) {
        break;
      }
      try {
        ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
            .importDoc(doc);
      } catch (BeeException e) {
        response.addError(e);
        break;
      }
      qs.updateData(new SqlUpdate(trade)
          .addConstant(COL_TRADE_EXPORTED, System.currentTimeMillis())
          .setWhere(sys.idEquals(trade, invoice.getLong(itemsRelation))));
    }
    if (response.hasErrors()) {
      response.log(logger);
    }
    return response;
  }

  private ResponseObject tryPhaseTransition(RequestInfo reqInfo) {
    BeeRowSet newRowSet = BeeRowSet.restore(reqInfo.getContent());
    if (DataUtils.isEmpty(newRowSet)) {
      return ResponseObject.error(reqInfo.getLabel(), "content not available");
    }

    BeeRow newRow = newRowSet.getRow(0);
    long docId = newRow.getId();

    BeeRowSet oldRowSet = qs.getViewData(VIEW_TRADE_DOCUMENTS, Filter.compareId(docId));
    if (DataUtils.isEmpty(oldRowSet)) {
      return ResponseObject.error(reqInfo.getLabel(), "row", docId, "not found");
    }

    BeeRow oldRow = oldRowSet.getRow(0);

    TradeDocumentPhase oldPhase = oldRow.getEnum(
        oldRowSet.getColumnIndex(COL_TRADE_DOCUMENT_PHASE), TradeDocumentPhase.class);
    TradeDocumentPhase newPhase = newRow.getEnum(
        newRowSet.getColumnIndex(COL_TRADE_DOCUMENT_PHASE), TradeDocumentPhase.class);

    if (newPhase == null) {
      return ResponseObject.error(reqInfo.getLabel(), docId, "new phase not specified");
    }
    if (newPhase == oldPhase) {
      return ResponseObject.warning(reqInfo.getLabel(), docId, "phase not changed");
    }

    OperationType operationType = newRow.getEnum(newRowSet.getColumnIndex(COL_OPERATION_TYPE),
        OperationType.class);

    boolean oldStock = oldPhase != null && oldPhase.modifyStock();
    boolean newStock = newPhase.modifyStock();

    if (oldStock != newStock) {
      Long warehouseFrom = newRow.getLong(newRowSet.getColumnIndex(COL_TRADE_WAREHOUSE_FROM));
      Long warehouseTo = newRow.getLong(newRowSet.getColumnIndex(COL_TRADE_WAREHOUSE_TO));

      Collection<String> errorMessages = verifyPhaseTransition(docId, operationType,
          warehouseFrom, warehouseTo, newStock);

      ResponseObject response;

      if (!BeeUtils.isEmpty(errorMessages)) {
        response = ResponseObject.error(reqInfo.getLabel(), docId);
        errorMessages.forEach(response::addError);
        return response;
      }

      response = doPhaseTransition(docId, operationType, warehouseFrom, warehouseTo, newStock);
      if (response != null && response.hasErrors()) {
        return response;
      }
    }

    int statusIndex = newRowSet.getColumnIndex(COL_TRADE_DOCUMENT_STATUS);
    Long status = newRow.getLong(statusIndex);

    if (DataUtils.isId(status) && !BeeUtils.isTrue(qs.getBooleanById(TBL_TRADE_STATUSES, status,
        newPhase.getStatusColumnName()))) {

      newRow.clearCell(statusIndex);
    }

    ResponseObject result =
        commitRow(oldRowSet.getViewName(), oldRowSet.getColumns(), oldRow, newRow);

    if (!result.hasErrors() && newStock && operationType != null && operationType.providesCost()) {
      ResponseObject response = calculateCost(docId,
          newRow.getDateTime(newRowSet.getColumnIndex(COL_TRADE_DATE)),
          newRow.getLong(newRowSet.getColumnIndex(COL_TRADE_CURRENCY)),
          newRow.getEnum(newRowSet.getColumnIndex(COL_TRADE_DOCUMENT_VAT_MODE),
              TradeVatMode.class),
          newRow.getEnum(newRowSet.getColumnIndex(COL_TRADE_DOCUMENT_DISCOUNT_MODE),
              TradeDiscountMode.class),
          newRow.getDouble(newRowSet.getColumnIndex(COL_TRADE_DOCUMENT_DISCOUNT)));

      if (response != null && response.hasMessages()) {
        List<ResponseMessage> messages = new ArrayList<>();

        for (ResponseMessage rm : response.getMessages()) {
          if (rm != null) {
            messages.add(new ResponseMessage(rm.getDate(),
                rm.getLevel() == LogLevel.ERROR ? LogLevel.WARNING : rm.getLevel(),
                rm.getMessage()));
          }
        }

        result.addMessages(messages);
      }
    }

    return result;
  }

  private ResponseObject doPhaseTransition(long docId, OperationType operationType,
      Long warehouseFrom, Long warehouseTo, boolean toStock) {

    IsCondition itemCondition = SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT,
        docId);

    ResponseObject response;

    if (operationType.consumesStock() && operationType.producesStock()) {
      if (toStock) {
        response = adoptItems(itemCondition, warehouseFrom);
        if (!response.hasErrors()) {
          response = insertStock(itemCondition, warehouseTo);
        }

      } else {
        response = leaveParents(itemCondition);
        if (!response.hasErrors()) {
          response = deleteStock(itemCondition);
        }
      }

    } else if (operationType.producesStock()) {
      if (toStock) {
        response = insertStock(itemCondition, warehouseTo);
      } else {
        response = deleteStock(itemCondition);
      }

    } else if (operationType.consumesStock()) {
      if (toStock) {
        response = adoptItems(itemCondition, warehouseFrom);
      } else {
        response = leaveParents(itemCondition);
      }

    } else {
      response = ResponseObject.error("phase transition: operation type", operationType,
          "does not modify stock");
    }

    return response;
  }

  private ResponseObject adoptItems(IsCondition itemCondition, Long documentWarehouseFrom) {
    int count = 0;

    String idName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

    SqlSelect itemQuery = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, idName, COL_ITEM, COL_TRADE_ITEM_WAREHOUSE_FROM,
            COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM))
        .setWhere(SqlUtils.and(itemCondition, SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE)))
        .addOrder(TBL_TRADE_DOCUMENT_ITEMS, idName);

    SimpleRowSet itemData = qs.getData(itemQuery);

    if (!DataUtils.isEmpty(itemData)) {
      Map<Long, Double> usedParents = new HashMap<>();
      ResponseObject response;

      for (SimpleRow itemRow : itemData) {
        Long id = itemRow.getLong(idName);
        Long item = itemRow.getLong(COL_ITEM);

        Long warehouseFrom = itemRow.getLong(COL_TRADE_ITEM_WAREHOUSE_FROM);
        if (!DataUtils.isId(warehouseFrom)) {
          warehouseFrom = documentWarehouseFrom;
        }

        Double qty = itemRow.getDouble(COL_TRADE_ITEM_QUANTITY);

        Long parent = null;
        Map<Long, Double> parentQuantities = getParentQuantities(item, warehouseFrom, usedParents);

        if (!BeeUtils.isEmpty(parentQuantities)) {
          for (Map.Entry<Long, Double> entry : parentQuantities.entrySet()) {
            if (BeeUtils.isMeq(entry.getValue(), qty)) {
              parent = entry.getKey();
              break;
            }
          }
        }

        if (!DataUtils.isId(parent)) {
          return ResponseObject.error("parent not found for id", id, "item", item, "qty", qty,
              "warehouse", warehouseFrom);
        }

        if (BeeUtils.nonZero(qty)) {
          SqlUpdate stockUpdate = new SqlUpdate(TBL_TRADE_STOCK)
              .addExpression(COL_STOCK_QUANTITY,
                  SqlUtils.minus(SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_QUANTITY), qty))
              .setWhere(SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, parent));

          response = qs.updateDataWithResponse(stockUpdate);
          if (response.hasErrors()) {
            return response;
          }
        }

        SqlUpdate itemUpdate = new SqlUpdate(TBL_TRADE_DOCUMENT_ITEMS)
            .addConstant(COL_TRADE_ITEM_PARENT, parent)
            .setWhere(SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, idName, id));

        response = qs.updateDataWithResponse(itemUpdate);
        if (response.hasErrors()) {
          return response;
        }

        count++;
      }
    }

    return ResponseObject.response(count);
  }

  private Map<Long, Double> getParentQuantities(Long item, Long warehouse,
      Map<Long, Double> exclude) {

    Map<Long, Double> result = new LinkedHashMap<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, COL_STOCK_QUANTITY)
        .addFrom(TBL_TRADE_STOCK)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_STOCK, COL_PRIMARY_DOCUMENT_ITEM))
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addOrder(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE)
        .addOrder(TBL_TRADE_STOCK, COL_PRIMARY_DOCUMENT_ITEM);

    HasConditions where = SqlUtils.and(
        SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, item),
        SqlUtils.positive(TBL_TRADE_STOCK, COL_STOCK_QUANTITY));

    if (DataUtils.isId(warehouse)) {
      where.add(SqlUtils.equals(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE, warehouse));
    }

    query.setWhere(where);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        Long parent = row.getLong(COL_TRADE_DOCUMENT_ITEM);
        Double quantity = row.getDouble(COL_STOCK_QUANTITY);

        if (!BeeUtils.isEmpty(exclude) && exclude.containsKey(parent)) {
          quantity -= exclude.get(parent);
        }

        if (BeeUtils.isPositive(quantity)) {
          result.put(parent, quantity);
        }
      }
    }

    return result;
  }

  private ResponseObject leaveParents(IsCondition itemCondition) {
    int count = 0;

    String idName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, idName, COL_TRADE_ITEM_PARENT, COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .setWhere(SqlUtils.and(itemCondition,
            SqlUtils.notNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT)));

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      ResponseObject response;

      for (SimpleRow row : data) {
        Long id = row.getLong(idName);
        Long parent = row.getLong(COL_TRADE_ITEM_PARENT);

        Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);

        if (BeeUtils.nonZero(qty)) {
          SqlUpdate stockUpdate = new SqlUpdate(TBL_TRADE_STOCK)
              .addExpression(COL_STOCK_QUANTITY,
                  SqlUtils.plus(SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_QUANTITY), qty))
              .setWhere(SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, parent));

          response = qs.updateDataWithResponse(stockUpdate);
          if (response.hasErrors()) {
            return response;
          }
        }

        SqlUpdate itemUpdate = new SqlUpdate(TBL_TRADE_DOCUMENT_ITEMS)
            .addConstant(COL_TRADE_ITEM_PARENT, null)
            .setWhere(SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, idName, id));

        response = qs.updateDataWithResponse(itemUpdate);
        if (response.hasErrors()) {
          return response;
        }

        count++;
      }
    }

    return ResponseObject.response(count);
  }

  private ResponseObject deleteStock(IsCondition itemCondition) {
    SqlDelete delete = new SqlDelete(TBL_TRADE_STOCK)
        .setWhere(SqlUtils.in(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM,
            TBL_TRADE_DOCUMENT_ITEMS, sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS), itemCondition));

    return qs.updateDataWithResponse(delete);
  }

  private ResponseObject insertStock(IsCondition itemCondition, Long warehouseTo) {
    int count = 0;

    String idName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, idName, COL_TRADE_ITEM_PARENT,
            COL_TRADE_ITEM_WAREHOUSE_TO, COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM))
        .setWhere(SqlUtils.and(itemCondition, SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE)));

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        Long id = row.getLong(idName);

        Long parent = row.getLong(COL_TRADE_ITEM_PARENT);
        Long primary = DataUtils.isId(parent) ? getPrimary(parent) : id;

        Long warehouse = BeeUtils.nvl(row.getLong(COL_TRADE_ITEM_WAREHOUSE_TO), warehouseTo);
        Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);

        SqlInsert insert = new SqlInsert(TBL_TRADE_STOCK)
            .addConstant(COL_PRIMARY_DOCUMENT_ITEM, primary)
            .addConstant(COL_TRADE_DOCUMENT_ITEM, id)
            .addConstant(COL_STOCK_WAREHOUSE, warehouse)
            .addConstant(COL_STOCK_QUANTITY, qty);

        ResponseObject response = qs.insertDataWithResponse(insert);
        if (response.hasErrors()) {
          return response;
        }

        count++;
      }
    }

    return ResponseObject.response(count);
  }

  private Long getPrimary(long parent) {
    return qs.getLong(TBL_TRADE_STOCK, COL_PRIMARY_DOCUMENT_ITEM, COL_TRADE_DOCUMENT_ITEM, parent);
  }

  private Collection<String> verifyPhaseTransition(long docId, OperationType operationType,
      Long warehouseFrom, Long warehouseTo, boolean toStock) {

    List<String> messages = new ArrayList<>();

    IsCondition itemCondition = SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT,
        docId);
    if (!qs.sqlExists(TBL_TRADE_DOCUMENT_ITEMS, itemCondition)) {
      return messages;
    }

    if (operationType == null) {
      messages.add("operation type not specified");
      return messages;
    }
    if (toStock && operationType.producesStock() && !DataUtils.isId(warehouseTo)) {
      messages.add("warehouse-receiver not specified");
      return messages;
    }

    if (operationType.producesStock() && !toStock && hasChildren(itemCondition)) {
      messages.add("document has children");
    }
    if (operationType.consumesStock() && toStock) {
      messages.addAll(verifyStock(itemCondition, warehouseFrom));
    }

    return messages;
  }

  private List<String> verifyStock(IsCondition itemCondition, Long documentWarehouseFrom) {
    List<String> messages = new ArrayList<>();

    String idName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

    SqlSelect itemQuery = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, idName, COL_ITEM, COL_TRADE_ITEM_WAREHOUSE_FROM,
            COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM))
        .setWhere(SqlUtils.and(itemCondition, SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE)))
        .addOrder(TBL_TRADE_DOCUMENT_ITEMS, idName);

    SimpleRowSet itemData = qs.getData(itemQuery);

    if (!DataUtils.isEmpty(itemData)) {
      Map<Long, Double> usedParents = new HashMap<>();

      for (SimpleRow itemRow : itemData) {
        Long id = itemRow.getLong(idName);
        Long item = itemRow.getLong(COL_ITEM);

        Long warehouseFrom = itemRow.getLong(COL_TRADE_ITEM_WAREHOUSE_FROM);
        if (!DataUtils.isId(warehouseFrom)) {
          warehouseFrom = documentWarehouseFrom;
        }

        Double qty = itemRow.getDouble(COL_TRADE_ITEM_QUANTITY);

        Long parent = null;
        Map<Long, Double> parentQuantities = getParentQuantities(item, warehouseFrom, usedParents);

        if (!BeeUtils.isEmpty(parentQuantities)) {
          for (Map.Entry<Long, Double> entry : parentQuantities.entrySet()) {
            if (BeeUtils.isMeq(entry.getValue(), qty)) {
              parent = entry.getKey();
              usedParents.merge(parent, qty, Double::sum);
              break;
            }
          }
        }

        if (!DataUtils.isId(parent)) {
          messages.add(BeeUtils.joinWords("parent not found for id", id, "item", item,
              "qty", qty, "warehouse", warehouseFrom));
        }
      }
    }

    return messages;
  }

  private boolean hasChildren(IsCondition itemCondition) {
    String alias = SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect()
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, alias,
            SqlUtils.join(alias, COL_TRADE_ITEM_PARENT,
                TBL_TRADE_DOCUMENT_ITEMS, sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS)))
        .setWhere(itemCondition);

    return qs.sqlExists(query);
  }

  private ResponseObject commitRow(String viewName, List<BeeColumn> columns, BeeRow oldRow,
      BeeRow newRow) {

    BeeRowSet updated = DataUtils.getUpdated(viewName, columns, oldRow, newRow, null);

    if (DataUtils.isEmpty(updated)) {
      return ResponseObject.response(newRow);
    } else {
      return deb.commitRow(updated);
    }
  }

  private void fireStockUpdate(IsCondition where, String fieldName) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_STOCK,
            sys.getIdName(TBL_TRADE_STOCK), sys.getVersionName(TBL_TRADE_STOCK), fieldName)
        .addFrom(TBL_TRADE_STOCK)
        .setWhere(where);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      CellSource source = sys.getCellSource(VIEW_TRADE_STOCK, fieldName);

      for (SimpleRow row : data) {
        CellUpdateEvent.fire(Endpoint.getModificationShooter(), VIEW_TRADE_STOCK,
            row.getLong(0), row.getLong(1), source, row.getValue(fieldName));
      }
    }
  }

  private boolean isStockItem(Long item) {
    if (DataUtils.isId(item)) {
      return !BeeUtils.isTrue(qs.getBooleanById(TBL_ITEMS, item, COL_ITEM_IS_SERVICE));
    } else {
      return false;
    }
  }

  private boolean modifyDocumentStock(long docId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENTS, COL_TRADE_DOCUMENT_PHASE)
        .addFrom(TBL_TRADE_DOCUMENTS)
        .setWhere(sys.idEquals(TBL_TRADE_DOCUMENTS, docId));

    TradeDocumentPhase phase = qs.getEnum(query, TradeDocumentPhase.class);
    return phase != null && phase.modifyStock();
  }

  private boolean modifyItemStock(long itemId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENTS, COL_TRADE_DOCUMENT_PHASE)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM))
        .setWhere(SqlUtils.and(sys.idEquals(TBL_TRADE_DOCUMENT_ITEMS, itemId),
            SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE)));

    TradeDocumentPhase phase = qs.getEnum(query, TradeDocumentPhase.class);
    return phase != null && phase.modifyStock();
  }

  private ResponseObject onTradeItemQuantityUpdate(long itemId, Double newQty) {
    if (modifyItemStock(itemId)) {
      if (!BeeUtils.isPositive(newQty)) {
        return ResponseObject.error("invalid quantity", newQty);
      }

      String idName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

      Double oldQty = qs.getDouble(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY,
          idName, itemId);

      if (BeeUtils.isDouble(oldQty) && !newQty.equals(oldQty)) {
        IsCondition itemWhere = SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, itemId);
        IsCondition parentWhere = null;

        boolean updateItemStock = qs.sqlExists(TBL_TRADE_STOCK, itemWhere);
        boolean updateParentStock = false;

        Double itemOldStock = null;
        Double itemNewStock = null;
        Double parentOldStock = null;
        Double parentNewStock = null;

        if (updateItemStock) {
          itemOldStock = qs.getDouble(TBL_TRADE_STOCK, COL_STOCK_QUANTITY, itemWhere);
          itemNewStock = BeeUtils.unbox(itemOldStock) - oldQty + newQty;

          if (BeeUtils.isNegative(itemNewStock)) {
            return ResponseObject.error("item stock", itemOldStock, "-", oldQty, "+", newQty,
                "=", itemNewStock);
          }
        }

        Long parent = qs.getLong(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT,
            idName, itemId);

        if (DataUtils.isId(parent)) {
          parentWhere = SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, parent);
          updateParentStock = qs.sqlExists(TBL_TRADE_STOCK, parentWhere);

          if (updateParentStock) {
            parentOldStock = qs.getDouble(TBL_TRADE_STOCK, COL_STOCK_QUANTITY, parentWhere);
            parentNewStock = BeeUtils.unbox(parentOldStock) + oldQty - newQty;

            if (BeeUtils.isNegative(parentNewStock)) {
              return ResponseObject.error("parent stock", parentOldStock, "+", oldQty, "-", newQty,
                  "=", parentNewStock);
            }
          }
        }

        if (updateItemStock) {
          SqlUpdate update = new SqlUpdate(TBL_TRADE_STOCK)
              .addConstant(COL_STOCK_QUANTITY, itemNewStock)
              .setWhere(itemWhere);

          ResponseObject response = qs.updateDataWithResponse(update);
          if (response.hasErrors()) {
            return response;
          }
        }

        if (updateParentStock) {
          SqlUpdate update = new SqlUpdate(TBL_TRADE_STOCK)
              .addConstant(COL_STOCK_QUANTITY, parentNewStock)
              .setWhere(parentWhere);

          ResponseObject response = qs.updateDataWithResponse(update);
          if (response.hasErrors()) {
            return response;
          }
        }

        if (updateItemStock) {
          if (BeeUtils.isPositive(itemOldStock) && BeeUtils.isPositive(itemNewStock)) {
            fireStockUpdate(itemWhere, COL_STOCK_QUANTITY);
          } else {
            refreshStock();
          }
        }

        if (updateParentStock) {
          if (BeeUtils.isPositive(parentOldStock) && BeeUtils.isPositive(parentNewStock)) {
            fireStockUpdate(parentWhere, COL_STOCK_QUANTITY);
          } else {
            refreshStock();
          }
        }
      }
    }

    return ResponseObject.emptyResponse();
  }

  private OperationType getOperationTypeByTradeDocument(long docId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE)
        .addFrom(TBL_TRADE_DOCUMENTS)
        .addFromInner(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .setWhere(sys.idEquals(TBL_TRADE_DOCUMENTS, docId));

    return qs.getEnum(query, OperationType.class);
  }

  private OperationType getOperationTypeByTradeItem(long itemId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromInner(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .setWhere(sys.idEquals(TBL_TRADE_DOCUMENT_ITEMS, itemId));

    return qs.getEnum(query, OperationType.class);
  }

  private OperationType getOperationTypeByOperation(long operation) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE)
        .addFrom(TBL_TRADE_OPERATIONS)
        .setWhere(sys.idEquals(TBL_TRADE_OPERATIONS, operation));

    return qs.getEnum(query, OperationType.class);
  }

  private Collection<Long> getOperationsByDebtKind(DebtKind debtKind) {
    Collection<OperationType> operationTypes = Arrays.stream(OperationType.values())
        .filter(type -> type.getDebtKind() == debtKind)
        .collect(Collectors.toSet());

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_OPERATIONS, sys.getIdName(TBL_TRADE_OPERATIONS))
        .addFrom(TBL_TRADE_OPERATIONS)
        .setWhere(SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE, operationTypes));

    return qs.getLongSet(query);
  }

  private String getDocumentFieldByTradeItem(long itemId, String fieldName) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENTS, fieldName)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .setWhere(sys.idEquals(TBL_TRADE_DOCUMENT_ITEMS, itemId));

    return qs.getValue(query);
  }

  private ResponseObject onTradeItemWarehouseReceiverUpdate(long itemId, Long newValue) {
    OperationType operationType = getOperationTypeByTradeItem(itemId);

    if (operationType != null && operationType.producesStock() && modifyItemStock(itemId)) {
      String idName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

      Long oldValue = qs.getLong(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_WAREHOUSE_TO,
          idName, itemId);

      Long warehouse;
      if (DataUtils.isId(newValue)) {
        warehouse = newValue;
      } else {
        warehouse = BeeUtils.toLongOrNull(getDocumentFieldByTradeItem(itemId,
            COL_TRADE_WAREHOUSE_TO));
      }

      if (!Objects.equals(oldValue, warehouse)) {
        if (!DataUtils.isId(warehouse)) {
          return ResponseObject.error("warehouse required");
        }

        SqlUpdate update = new SqlUpdate(TBL_TRADE_STOCK)
            .addConstant(COL_STOCK_WAREHOUSE, warehouse)
            .setWhere(SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, itemId));

        ResponseObject response = qs.updateDataWithResponse(update);
        if (response.hasErrors()) {
          return response;
        }

        if (BeeUtils.isPositive(response.getResponseAsInt())) {
          Endpoint.refreshRows(qs.getViewData(VIEW_TRADE_STOCK,
              Filter.equals(COL_TRADE_DOCUMENT_ITEM, itemId)));
        }
      }
    }

    return ResponseObject.emptyResponse();
  }

  private Long getWarehouseReceiver(long docId, Long itemWarehouseTo) {
    if (DataUtils.isId(itemWarehouseTo)) {
      return itemWarehouseTo;
    } else {
      return qs.getLongById(TBL_TRADE_DOCUMENTS, docId, COL_TRADE_WAREHOUSE_TO);
    }
  }

  private String verifyTradeItemInsert(long docId, Double quantity, Long warehouseTo, Long parent) {
    if (!BeeUtils.isDouble(quantity)) {
      return "item quantity not available";
    }

    OperationType operationType = getOperationTypeByTradeDocument(docId);
    if (operationType == null) {
      return "operation type not available";
    }

    if (operationType.consumesStock()) {
      if (!DataUtils.isId(parent)) {
        return "item parent not available";
      }

      Double stock = qs.getDouble(TBL_TRADE_STOCK, COL_STOCK_QUANTITY,
          COL_TRADE_DOCUMENT_ITEM, parent);

      if (!BeeUtils.isDouble(stock)) {
        return "parent stock not found";
      }
      if (stock < quantity) {
        return BeeUtils.joinWords("parent", parent, "stock", stock, "quantity", quantity);
      }
    }

    if (operationType.producesStock()) {
      if (BeeUtils.isNegative(quantity)) {
        return "quantity must be >= 0";
      }

      Long warehouseReceiver = getWarehouseReceiver(docId, warehouseTo);
      if (!DataUtils.isId(warehouseReceiver)) {
        return "warehouse-receiver not specified";
      }
    }

    return null;
  }

  private ResponseObject afterTradeItemInsert(long docId, Double quantity, Long warehouseTo,
      Long parent, long itemId) {

    OperationType operationType = getOperationTypeByTradeDocument(docId);

    if (operationType != null && operationType.consumesStock() && BeeUtils.nonZero(quantity)
        && DataUtils.isId(parent)) {

      IsCondition where = SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, parent);
      Double stock = qs.getDouble(TBL_TRADE_STOCK, COL_STOCK_QUANTITY, where);

      if (BeeUtils.isDouble(stock)) {
        SqlUpdate update = new SqlUpdate(TBL_TRADE_STOCK)
            .addConstant(COL_STOCK_QUANTITY, stock - quantity)
            .setWhere(where);

        ResponseObject response = qs.updateDataWithResponse(update);
        if (response.hasErrors()) {
          return response;
        }

        fireStockUpdate(where, COL_STOCK_QUANTITY);
      }
    }

    if (operationType != null && operationType.producesStock()) {
      Long primary = DataUtils.isId(parent) ? getPrimary(parent) : itemId;
      Long warehouse = getWarehouseReceiver(docId, warehouseTo);

      SqlInsert insert = new SqlInsert(TBL_TRADE_STOCK)
          .addConstant(COL_PRIMARY_DOCUMENT_ITEM, primary)
          .addConstant(COL_TRADE_DOCUMENT_ITEM, itemId)
          .addConstant(COL_STOCK_WAREHOUSE, warehouse)
          .addConstant(COL_STOCK_QUANTITY, quantity);

      ResponseObject response = qs.insertDataWithResponse(insert);
      if (response.hasErrors()) {
        return response;
      }

      refreshStock();
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject onDeleteTradeItems(Collection<Long> itemIds) {
    boolean refresh = false;

    for (Long itemId : itemIds) {
      if (DataUtils.isId(itemId)) {
        if (qs.sqlExists(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT, itemId)) {
          return ResponseObject.error("item", itemId, "has children");
        }

        SqlSelect query = new SqlSelect()
            .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PARENT)
            .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
            .setWhere(SqlUtils.and(sys.idEquals(TBL_TRADE_DOCUMENT_ITEMS, itemId),
                SqlUtils.notNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT)));

        SimpleRow row = qs.getRow(query);
        if (row != null) {
          Long parent = row.getLong(COL_TRADE_ITEM_PARENT);
          Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);

          if (DataUtils.isId(parent) && BeeUtils.nonZero(qty)) {
            SqlUpdate stockUpdate = new SqlUpdate(TBL_TRADE_STOCK)
                .addExpression(COL_STOCK_QUANTITY,
                    SqlUtils.plus(SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_QUANTITY), qty))
                .setWhere(SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, parent));

            ResponseObject response = qs.updateDataWithResponse(stockUpdate);
            if (response.hasErrors()) {
              return response;
            }

            refresh = true;
          }
        }
      }
    }

    if (!refresh) {
      refresh = qs.sqlExists(TBL_TRADE_STOCK,
          SqlUtils.inList(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, itemIds));
    }

    return refresh ? ResponseObject.response(Action.REFRESH) : ResponseObject.emptyResponse();
  }

  private ResponseObject onDeleteTradeDocuments(Collection<Long> docIds) {
    boolean refresh = false;

    for (Long docId : docIds) {
      if (DataUtils.isId(docId)) {
        if (hasChildren(SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT, docId))) {
          return ResponseObject.error("document", docId, "has children");
        }

        SqlSelect query = new SqlSelect()
            .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PARENT)
            .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
            .setWhere(SqlUtils.and(
                SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT, docId),
                SqlUtils.notNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT)));

        SimpleRowSet data = qs.getData(query);

        if (!DataUtils.isEmpty(data)) {
          for (SimpleRow row : data) {
            Long parent = row.getLong(COL_TRADE_ITEM_PARENT);
            Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);

            if (DataUtils.isId(parent) && BeeUtils.nonZero(qty)) {
              SqlUpdate stockUpdate = new SqlUpdate(TBL_TRADE_STOCK)
                  .addExpression(COL_STOCK_QUANTITY,
                      SqlUtils.plus(SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_QUANTITY), qty))
                  .setWhere(SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, parent));

              ResponseObject response = qs.updateDataWithResponse(stockUpdate);
              if (response.hasErrors()) {
                return response;
              }

              refresh = true;
            }
          }
        }
      }
    }

    if (!refresh) {
      SqlSelect query = new SqlSelect()
          .addFrom(TBL_TRADE_STOCK)
          .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
              TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM))
          .setWhere(SqlUtils.inList(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT, docIds));

      refresh = qs.sqlExists(query);
    }

    return refresh ? ResponseObject.response(Action.REFRESH) : ResponseObject.emptyResponse();
  }

  private Set<Long> getTradeDocumentStockItemsWithoutWarehouseReceiver(long docId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM)
        .addFrom(TBL_TRADE_STOCK)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM))
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM))
        .setWhere(SqlUtils.and(
            SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT, docId),
            SqlUtils.isNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_WAREHOUSE_TO),
            SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE)));

    return qs.getLongSet(query);
  }

  private ResponseObject rebuildStock() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    ResponseObject response = ResponseObject.emptyResponse();

    Set<Integer> rootTypes = new HashSet<>();
    Set<Integer> producerTypes = new HashSet<>();
    Set<Integer> consumerTypes = new HashSet<>();

    for (OperationType type : OperationType.values()) {
      if (type.producesStock()) {
        producerTypes.add(type.ordinal());

        if (!type.consumesStock()) {
          rootTypes.add(type.ordinal());
        }
      }

      if (type.consumesStock()) {
        consumerTypes.add(type.ordinal());
      }
    }

    IsCondition rootCondition = SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE,
        rootTypes);
    IsCondition producerCondition = SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE,
        producerTypes);
    IsCondition consumerCondition = SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE,
        consumerTypes);

    Set<Integer> stockPhases = new HashSet<>();

    for (TradeDocumentPhase phase : TradeDocumentPhase.values()) {
      if (phase.modifyStock()) {
        stockPhases.add(phase.ordinal());
      }
    }

    IsCondition phaseCondition = SqlUtils.inList(TBL_TRADE_DOCUMENTS, COL_TRADE_DOCUMENT_PHASE,
        stockPhases);

    IsCondition warehouseToCondition = SqlUtils.or(
        SqlUtils.notNull(TBL_TRADE_DOCUMENTS, COL_TRADE_WAREHOUSE_TO),
        SqlUtils.notNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_WAREHOUSE_TO));

    IsCondition itemCondition = SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE);

    SqlSelect rootQuery = createStockProducerQuery(null,
        SqlUtils.and(rootCondition, phaseCondition, warehouseToCondition, itemCondition,
            SqlUtils.isNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT)));

    String parents = qs.sqlCreateTemp(rootQuery);
    int count = qs.sqlCount(parents, null);

    if (count <= 0) {
      qs.sqlDropTemp(parents);
      response.addWarning("stock root items not found");

      if (!qs.isEmpty(TBL_TRADE_STOCK)) {
        response.addWarning(BeeUtils.joinWords(TBL_TRADE_STOCK, "is not empty"));
      }

      response.log(logger);
      return response;
    }

    List<String> stockFields = Lists.newArrayList(COL_PRIMARY_DOCUMENT_ITEM,
        COL_TRADE_DOCUMENT_ITEM, COL_STOCK_WAREHOUSE, COL_STOCK_QUANTITY);

    SqlSelect stockQuery = new SqlSelect().addFields(parents, stockFields).addFrom(parents);
    String stock = qs.sqlCreateTemp(stockQuery);

    String message = buildMessage(stopwatch, "depth", 0, "count", count);
    logger.info(message);
    response.addInfo(message);

    for (int depth = 1; depth < MAX_STOCK_DEPTH; depth++) {
      TimeUtils.restart(stopwatch);

      qs.sqlIndex(parents, COL_TRADE_DOCUMENT_ITEM);

      SqlSelect childQuery = createStockProducerQuery(parents,
          SqlUtils.and(producerCondition, phaseCondition, warehouseToCondition, consumerCondition));

      String children = qs.sqlCreateTemp(childQuery);
      qs.sqlDropTemp(parents);

      count = qs.sqlCount(children, null);
      if (count <= 0) {
        qs.sqlDropTemp(children);
        break;
      }

      SqlSelect source = new SqlSelect().addFields(children, stockFields).addFrom(children);
      SqlInsert append = new SqlInsert(stock).addFields(stockFields).setDataSource(source);

      ResponseObject insResponse = qs.insertDataWithResponse(append);
      if (insResponse.hasErrors()) {
        qs.sqlDropTemp(children);
        qs.sqlDropTemp(stock);

        return insResponse;
      }

      message = buildMessage(stopwatch, "depth", depth, "count", count);
      logger.info(message);
      response.addInfo(message);

      parents = children;
    }

    TimeUtils.restart(stopwatch);

    qs.sqlIndex(stock, COL_TRADE_DOCUMENT_ITEM);

    SqlSelect consumerQuery = new SqlSelect()
        .addFields(stock, COL_TRADE_DOCUMENT_ITEM)
        .addSum(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(stock)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, SqlUtils.join(stock, COL_TRADE_DOCUMENT_ITEM,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT))
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromInner(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .setWhere(SqlUtils.and(consumerCondition, phaseCondition))
        .addGroup(stock, COL_TRADE_DOCUMENT_ITEM);

    String consumers = qs.sqlCreateTemp(consumerQuery);

    if (qs.isEmpty(consumers)) {
      qs.sqlDropTemp(consumers);

    } else {
      qs.sqlIndex(consumers, COL_TRADE_DOCUMENT_ITEM);

      SqlUpdate update = new SqlUpdate(stock)
          .setFrom(consumers, SqlUtils.join(consumers, COL_TRADE_DOCUMENT_ITEM,
              stock, COL_TRADE_DOCUMENT_ITEM))
          .addExpression(COL_STOCK_QUANTITY,
              SqlUtils.minus(SqlUtils.field(stock, COL_STOCK_QUANTITY),
                  SqlUtils.field(consumers, COL_TRADE_ITEM_QUANTITY)));

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      qs.sqlDropTemp(consumers);

      if (updResponse.hasErrors()) {
        qs.sqlDropTemp(stock);
        return updResponse;
      }

      message = buildMessage(stopwatch, "consumers", updResponse.getResponse());
      logger.info(message);
      response.addInfo(message);
    }

    boolean updated = false;

    TimeUtils.restart(stopwatch);

    IsCondition diffCondition = SqlUtils.and(
        SqlUtils.compare(SqlUtils.field(stock, COL_TRADE_DOCUMENT_ITEM),
            Operator.EQ, SqlUtils.field(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM)),
        SqlUtils.or(
            SqlUtils.compare(SqlUtils.field(stock, COL_PRIMARY_DOCUMENT_ITEM),
                Operator.NE, SqlUtils.field(TBL_TRADE_STOCK, COL_PRIMARY_DOCUMENT_ITEM)),
            SqlUtils.compare(SqlUtils.field(stock, COL_STOCK_WAREHOUSE),
                Operator.NE, SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE)),
            SqlUtils.compare(SqlUtils.field(stock, COL_STOCK_QUANTITY),
                Operator.NE, SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_QUANTITY))));

    SqlSelect diffQuery = new SqlSelect()
        .addFields(stock, stockFields)
        .addFrom(stock)
        .addFromInner(TBL_TRADE_STOCK, diffCondition);

    SimpleRowSet diffData = qs.getData(diffQuery);

    if (!DataUtils.isEmpty(diffData)) {
      for (SimpleRow row : diffData) {
        Long tdItem = row.getLong(COL_TRADE_DOCUMENT_ITEM);

        Long primary = row.getLong(COL_PRIMARY_DOCUMENT_ITEM);
        Long warehouse = row.getLong(COL_STOCK_WAREHOUSE);

        Double quantity = row.getDouble(COL_STOCK_QUANTITY);
        if (BeeUtils.isNegative(quantity)) {
          message = BeeUtils.joinWords(COL_TRADE_DOCUMENT_ITEM, tdItem,
              COL_STOCK_QUANTITY, quantity);

          logger.warning(message);
          response.addWarning(message);

          quantity = BeeConst.DOUBLE_ZERO;
        }

        SqlUpdate update = new SqlUpdate(TBL_TRADE_STOCK)
            .addConstant(COL_PRIMARY_DOCUMENT_ITEM, primary)
            .addConstant(COL_STOCK_WAREHOUSE, warehouse)
            .addConstant(COL_STOCK_QUANTITY, quantity)
            .setWhere(SqlUtils.equals(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, tdItem));

        ResponseObject updResponse = qs.updateDataWithResponse(update);
        if (updResponse.hasErrors()) {
          qs.sqlDropTemp(stock);
          return updResponse;
        }
      }

      message = buildMessage(stopwatch, "updated", diffData.getNumberOfRows());
      logger.info(message);
      response.addInfo(message);

      updated = true;
    }

    TimeUtils.restart(stopwatch);

    SqlSelect oldQuery = new SqlSelect()
        .addFields(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM)
        .addFrom(TBL_TRADE_STOCK)
        .setWhere(SqlUtils.not(SqlUtils.in(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM,
            stock, COL_TRADE_DOCUMENT_ITEM)));

    Set<Long> oldItems = qs.getLongSet(oldQuery);

    if (!BeeUtils.isEmpty(oldItems)) {
      SqlDelete delete = new SqlDelete(TBL_TRADE_STOCK)
          .setWhere(SqlUtils.inList(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM, oldItems));

      ResponseObject delResponse = qs.updateDataWithResponse(delete);
      if (delResponse.hasErrors()) {
        qs.sqlDropTemp(stock);
        return delResponse;
      }

      message = buildMessage(stopwatch, "deleted", delResponse.getResponse());
      logger.info(message);
      response.addInfo(message);

      updated = true;
    }

    TimeUtils.restart(stopwatch);

    SqlSelect newQuery = new SqlSelect()
        .addFields(stock, stockFields)
        .addFrom(stock)
        .setWhere(SqlUtils.not(SqlUtils.in(stock, COL_TRADE_DOCUMENT_ITEM,
            TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM)));

    SimpleRowSet newData = qs.getData(newQuery);

    if (!DataUtils.isEmpty(newData)) {
      for (SimpleRow row : newData) {
        Long primary = row.getLong(COL_PRIMARY_DOCUMENT_ITEM);
        Long tdItem = row.getLong(COL_TRADE_DOCUMENT_ITEM);

        Long warehouse = row.getLong(COL_STOCK_WAREHOUSE);
        Double quantity = row.getDouble(COL_STOCK_QUANTITY);

        if (BeeUtils.isNegative(quantity)) {
          message = BeeUtils.joinWords(COL_TRADE_DOCUMENT_ITEM, tdItem,
              COL_STOCK_QUANTITY, quantity);

          logger.warning(message);
          response.addWarning(message);

          quantity = BeeConst.DOUBLE_ZERO;
        }

        SqlInsert insert = new SqlInsert(TBL_TRADE_STOCK)
            .addConstant(COL_PRIMARY_DOCUMENT_ITEM, primary)
            .addConstant(COL_TRADE_DOCUMENT_ITEM, tdItem)
            .addConstant(COL_STOCK_WAREHOUSE, warehouse)
            .addConstant(COL_STOCK_QUANTITY, quantity);

        ResponseObject insResponse = qs.insertDataWithResponse(insert);
        if (insResponse.hasErrors()) {
          qs.sqlDropTemp(stock);
          return insResponse;
        }
      }

      message = buildMessage(stopwatch, "inserted", newData.getNumberOfRows());
      logger.info(message);
      response.addInfo(message);

      updated = true;
    }

    qs.sqlDropTemp(stock);

    if (updated) {
      refreshStock();

    } else {
      message = "trade stock is up to date";

      logger.info(message);
      response.addInfo(message);
    }

    return response;
  }

  private SqlSelect createStockProducerQuery(String parents, IsCondition condition) {
    IsExpression warehouseExpression = SqlUtils.nvl(
        SqlUtils.field(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_WAREHOUSE_TO),
        SqlUtils.field(TBL_TRADE_DOCUMENTS, COL_TRADE_WAREHOUSE_TO));

    String tdItemIdName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

    boolean root = BeeUtils.isEmpty(parents) || TBL_TRADE_DOCUMENT_ITEMS.equals(parents);

    SqlSelect query = new SqlSelect();
    if (root) {
      query.addField(TBL_TRADE_DOCUMENT_ITEMS, tdItemIdName, COL_PRIMARY_DOCUMENT_ITEM);
    } else {
      query.addFields(parents, COL_PRIMARY_DOCUMENT_ITEM);
    }

    query.addField(TBL_TRADE_DOCUMENT_ITEMS, tdItemIdName, COL_TRADE_DOCUMENT_ITEM)
        .addExpr(warehouseExpression, COL_STOCK_WAREHOUSE)
        .addField(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY, COL_STOCK_QUANTITY)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromInner(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM));

    if (!root) {
      query.addFromInner(parents, SqlUtils.join(parents, COL_TRADE_DOCUMENT_ITEM,
          TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT));
    }

    query.setWhere(condition);

    return query;
  }

  private ResponseObject calculateCost(RequestInfo reqInfo) {
    Long id = reqInfo.getParameterLong(COL_TRADE_DOCUMENT);
    if (!DataUtils.isId(id)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_TRADE_DOCUMENT);
    }

    DateTime date = reqInfo.getParameterDateTime(COL_TRADE_DATE);
    if (date == null) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_TRADE_DATE);
    }

    Long currency = reqInfo.getParameterLong(COL_TRADE_CURRENCY);
    if (!DataUtils.isId(currency)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_TRADE_CURRENCY);
    }

    TradeVatMode vatMode = reqInfo.getParameterEnum(COL_TRADE_DOCUMENT_VAT_MODE,
        TradeVatMode.class);
    TradeDiscountMode discountMode = reqInfo.getParameterEnum(COL_TRADE_DOCUMENT_DISCOUNT_MODE,
        TradeDiscountMode.class);

    Double discount = reqInfo.getParameterDouble(COL_TRADE_DOCUMENT_DISCOUNT);

    return calculateCost(id, date, currency, vatMode, discountMode, discount);
  }

  private ResponseObject calculateCost(long docId, DateTime docDate, Long docCurrency,
      TradeVatMode docVatMode, TradeDiscountMode docDiscountMode, Double docDiscount) {

    Long costCurrency = prm.getRelation(PRM_CURRENCY);
    if (!DataUtils.isId(costCurrency)) {
      return ResponseObject.error(PRM_CURRENCY, Objects.toString(costCurrency));
    }

    String docItemIdName = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);

    SqlSelect itemQuery = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, docItemIdName,
            COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
            COL_TRADE_DOCUMENT_ITEM_DISCOUNT, COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
            COL_TRADE_DOCUMENT_ITEM_VAT, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT)
        .addFields(TBL_ITEMS, COL_ITEM_WEIGHT)
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM))
        .setWhere(SqlUtils.and(
            SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT, docId),
            SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE),
            SqlUtils.positive(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY),
            SqlUtils.positive(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PRICE)));

    SimpleRowSet docItems = qs.getData(itemQuery);

    TradeDocumentSums tdSums = new TradeDocumentSums(docVatMode, docDiscountMode, docDiscount);
    tdSums.disableRounding();

    Map<Long, Double> costs = new HashMap<>();

    Map<Long, Double> itemQuantities = new HashMap<>();
    Map<Long, Double> itemWeights = new HashMap<>();

    if (!DataUtils.isEmpty(docItems)) {
      for (SimpleRow item : docItems) {
        Long itemId = item.getLong(docItemIdName);
        Double quantity = item.getDouble(COL_TRADE_ITEM_QUANTITY);

        tdSums.add(itemId, quantity, item.getDouble(COL_TRADE_ITEM_PRICE),
            item.getDouble(COL_TRADE_DOCUMENT_ITEM_DISCOUNT),
            item.getBoolean(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT),
            item.getDouble(COL_TRADE_DOCUMENT_ITEM_VAT),
            item.getBoolean(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT));

        itemQuantities.put(itemId, quantity);

        Double weight = item.getDouble(COL_ITEM_WEIGHT);
        if (BeeUtils.nonZero(weight)) {
          itemWeights.put(itemId, weight * quantity);
        }
      }

      for (SimpleRow item : docItems) {
        Long itemId = item.getLong(docItemIdName);

        double vat = tdSums.getItemVat(itemId);
        double discount = tdSums.getItemDiscount(itemId);

        Double amount;
        if ((BeeUtils.isZero(vat) || docVatMode != TradeVatMode.INCLUSIVE)
            && BeeUtils.isZero(discount)) {

          amount = item.getDouble(COL_TRADE_ITEM_QUANTITY) * item.getDouble(COL_TRADE_ITEM_PRICE);

        } else {
          amount = tdSums.getItemTotal(itemId) - vat;
        }

        Double cost = adm.maybeExchange(docCurrency, costCurrency, amount, docDate);
        costs.put(itemId, cost);
      }
    }

    double totalItemCost = BeeUtils.sum(costs.values());
    double totalExpenditure = BeeConst.DOUBLE_ZERO;
    double totalCost = totalItemCost;

    logger.info(docId, COL_TRADE_ITEM_COST, totalItemCost);

    SqlSelect expenditureQuery = new SqlSelect()
        .addFields(TBL_TRADE_EXPENDITURES, COL_EXPENDITURE_DATE, COL_EXPENDITURE_AMOUNT,
            COL_EXPENDITURE_CURRENCY, COL_EXPENDITURE_VAT, COL_EXPENDITURE_VAT_IS_PERCENT)
        .addFields(TBL_EXPENDITURE_TYPES, COL_EXPENDITURE_TYPE_COST_BASIS)
        .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_VAT_MODE)
        .addFrom(TBL_TRADE_EXPENDITURES)
        .addFromLeft(TBL_EXPENDITURE_TYPES, sys.joinTables(TBL_EXPENDITURE_TYPES,
            TBL_TRADE_EXPENDITURES, COL_EXPENDITURE_TYPE))
        .addFromLeft(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_EXPENDITURE_TYPES, COL_EXPENDITURE_TYPE_OPERATION))
        .setWhere(SqlUtils.and(
            SqlUtils.equals(TBL_TRADE_EXPENDITURES, COL_TRADE_DOCUMENT, docId),
            SqlUtils.notNull(TBL_TRADE_EXPENDITURES, COL_EXPENDITURE_DATE),
            SqlUtils.nonZero(TBL_TRADE_EXPENDITURES, COL_EXPENDITURE_AMOUNT),
            SqlUtils.notNull(TBL_TRADE_EXPENDITURES, COL_EXPENDITURE_CURRENCY),
            SqlUtils.notNull(TBL_EXPENDITURE_TYPES, COL_EXPENDITURE_TYPE_COST_BASIS)));

    SimpleRowSet expenditures = qs.getData(expenditureQuery);

    if (!DataUtils.isEmpty(expenditures)) {
      EnumMap<TradeCostBasis, Double> expenditureByCostBasis = new EnumMap<>(TradeCostBasis.class);

      for (SimpleRow expenditure : expenditures) {
        TradeCostBasis costBasis = expenditure.getEnum(COL_EXPENDITURE_TYPE_COST_BASIS,
            TradeCostBasis.class);

        double amount = Localized.normalizeMoney(expenditure.getDouble(COL_EXPENDITURE_AMOUNT));

        TradeVatMode vatMode = expenditure.getEnum(COL_OPERATION_VAT_MODE, TradeVatMode.class);

        if (TradeVatMode.INCLUSIVE == vatMode) {
          Double vat = expenditure.getDouble(COL_EXPENDITURE_VAT);

          if (BeeUtils.nonZero(vat)) {
            if (BeeUtils.isTrue(expenditure.getBoolean(COL_EXPENDITURE_VAT_IS_PERCENT))) {
              amount -= Localized.normalizeMoney(vatMode.computePercent(amount, vat));
            } else {
              amount -= Localized.normalizeMoney(vat);
            }
          }
        }

        amount = Localized.normalizeMoney(adm.maybeExchange(
            expenditure.getLong(COL_EXPENDITURE_CURRENCY), costCurrency,
            amount, expenditure.getDateTime(COL_EXPENDITURE_DATE)));

        if (BeeUtils.nonZero(amount) && costBasis != null) {
          expenditureByCostBasis.merge(costBasis, amount, Double::sum);
        }
      }

      Map<Long, Double> costIncrement = new HashMap<>();

      expenditureByCostBasis.forEach((costBasis, amount) -> {
        double total;

        switch (costBasis) {
          case AMOUNT:
            total = BeeUtils.sum(costs.values());
            if (BeeUtils.isPositive(total)) {
              costs.forEach((itemId, cost) ->
                  costIncrement.merge(itemId, cost * amount / total, Double::sum));
            }
            break;

          case QUANTITY:
            total = BeeUtils.sum(itemQuantities.values());
            if (BeeUtils.isPositive(total)) {
              itemQuantities.forEach((itemId, quantity) ->
                  costIncrement.merge(itemId, quantity * amount / total, Double::sum));
            }
            break;

          case WEIGHT:
            total = BeeUtils.sum(itemWeights.values());
            if (BeeUtils.isPositive(total)) {
              itemWeights.forEach((itemId, weight) ->
                  costIncrement.merge(itemId, weight * amount / total, Double::sum));
            }
            break;
        }
      });

      if (!costIncrement.isEmpty()) {
        totalExpenditure = BeeUtils.sum(costIncrement.values());
        logger.info(docId, TBL_TRADE_EXPENDITURES, totalExpenditure);

        costIncrement.forEach((itemId, increment) ->
            costs.merge(itemId, increment, Double::sum));

        totalCost = BeeUtils.sum(costs.values());
        logger.info(docId, COL_TRADE_ITEM_COST, Localized.dictionary().total(), totalCost);
      }
    }

    itemQuantities.forEach((itemId, quantity) ->
        costs.computeIfPresent(itemId, (k, v) -> v / quantity));

    costs.replaceAll((k, v) -> Math.max(v, BeeConst.DOUBLE_ZERO));

    int scale = sys.getFieldScale(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST);
    costs.replaceAll((k, v) -> BeeUtils.round(v, scale));

    ResponseObject saveResponse = saveCost(docId, costs, costCurrency);
    if (saveResponse.hasErrors()) {
      return saveResponse;
    }

    if (saveResponse.getSize() > 0) {
      Map<Long, Double> updatedCosts = new HashMap<>();
      Set<Long> changedItems = DataUtils.parseIdSet(saveResponse.getResponseAsString());

      for (Long itemId : changedItems) {
        updatedCosts.put(itemId, costs.get(itemId));
      }

      if (!updatedCosts.isEmpty()) {
        refreshCost(VIEW_TRADE_DOCUMENT_ITEMS, sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS),
            COL_TRADE_ITEM_COST, updatedCosts);
        refreshCost(VIEW_TRADE_STOCK, COL_PRIMARY_DOCUMENT_ITEM,
            COL_TRADE_ITEM_COST, updatedCosts);
      }
    }

    int dec = 5;
    String result = BeeUtils.isZero(totalExpenditure)
        ? BeeUtils.toString(totalCost, dec)
        : BeeUtils.joinWords(BeeUtils.toString(totalItemCost, dec), BeeConst.STRING_PLUS,
        BeeUtils.toString(totalExpenditure, dec), BeeConst.STRING_EQ,
        BeeUtils.toString(totalCost, dec));

    return ResponseObject.response(result);
  }

  private void refreshCost(String viewName, String keyColumn, String valueColumn,
      Map<Long, Double> costs) {

    boolean keyIsId = sys.getIdName(sys.getViewSource(viewName)).equals(keyColumn);

    Filter filter = keyIsId ? Filter.idIn(costs.keySet()) : Filter.any(keyColumn, costs.keySet());
    BeeRowSet rowSet = qs.getViewData(viewName, filter);

    if (!DataUtils.isEmpty(rowSet)) {
      int keyIndex = keyIsId ? DataUtils.ID_INDEX : rowSet.getColumnIndex(keyColumn);
      int valueIndex = rowSet.getColumnIndex(valueColumn);

      CellSource cellSource = CellSource.forColumn(rowSet.getColumn(valueIndex), valueIndex);

      for (BeeRow row : rowSet) {
        Long key = keyIsId ? row.getId() : row.getLong(keyIndex);
        Double value = costs.get(key);

        CellUpdateEvent.fire(Endpoint.getModificationShooter(), viewName,
            row.getId(), row.getVersion(), cellSource, BeeUtils.toStringOrNull(value));
      }
    }
  }

  private ResponseObject saveCost(long docId, Map<Long, Double> costs, long currency) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ITEM_COST, COL_TRADE_DOCUMENT_ITEM,
            COL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST_CURRENCY)
        .addFrom(TBL_TRADE_ITEM_COST)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_ITEM_COST, COL_TRADE_DOCUMENT_ITEM))
        .setWhere(SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT, docId));

    SimpleRowSet oldCosts = qs.getData(query);

    Set<Long> delete = new HashSet<>();
    Map<Long, Double> update = new HashMap<>();
    Map<Long, Double> insert = new HashMap<>();

    if (DataUtils.isEmpty(oldCosts)) {
      insert.putAll(costs);

    } else {
      Set<Long> oldIds = new HashSet<>();

      for (SimpleRow row : oldCosts) {
        Long itemId = row.getLong(COL_TRADE_DOCUMENT_ITEM);

        Double oldCost = row.getDouble(COL_TRADE_ITEM_COST);
        Long oldCurrency = row.getLong(COL_TRADE_ITEM_COST_CURRENCY);

        oldIds.add(itemId);

        if (costs.containsKey(itemId)) {
          Double cost = costs.get(itemId);
          if (!Objects.equals(cost, oldCost) || !Objects.equals(currency, oldCurrency)) {
            update.put(itemId, cost);
          }

        } else {
          delete.add(itemId);
        }
      }

      costs.forEach((k, v) -> {
        if (!oldIds.contains(k)) {
          insert.put(k, v);
        }
      });
    }

    Set<Long> changedItems = new HashSet<>();

    if (!delete.isEmpty()) {
      SqlDelete sd = new SqlDelete(TBL_TRADE_ITEM_COST)
          .setWhere(SqlUtils.inList(TBL_TRADE_ITEM_COST, COL_TRADE_DOCUMENT_ITEM, delete));

      ResponseObject response = qs.updateDataWithResponse(sd);
      if (response.hasErrors()) {
        return response;
      }

      changedItems.addAll(delete);
    }

    if (!update.isEmpty()) {
      for (Map.Entry<Long, Double> entry : update.entrySet()) {
        SqlUpdate su = new SqlUpdate(TBL_TRADE_ITEM_COST)
            .addConstant(COL_TRADE_ITEM_COST, entry.getValue())
            .addConstant(COL_TRADE_ITEM_COST_CURRENCY, currency)
            .setWhere(SqlUtils.equals(TBL_TRADE_ITEM_COST, COL_TRADE_DOCUMENT_ITEM,
                entry.getKey()));

        ResponseObject response = qs.updateDataWithResponse(su);
        if (response.hasErrors()) {
          return response;
        }
      }

      changedItems.addAll(update.keySet());
    }

    if (!insert.isEmpty()) {
      for (Map.Entry<Long, Double> entry : insert.entrySet()) {
        SqlInsert si = new SqlInsert(TBL_TRADE_ITEM_COST)
            .addConstant(COL_TRADE_DOCUMENT_ITEM, entry.getKey())
            .addConstant(COL_TRADE_ITEM_COST, entry.getValue())
            .addConstant(COL_TRADE_ITEM_COST_CURRENCY, currency);

        ResponseObject response = qs.insertDataWithResponse(si);
        if (response.hasErrors()) {
          return response;
        }
      }

      changedItems.addAll(insert.keySet());
    }

    return ResponseObject.response(DataUtils.buildIdList(changedItems))
        .setSize(changedItems.size());
  }

  private ResponseObject maybeCreateExtraDimensions(String tbl, long id, Map<String, ?> values) {
    if (!BeeUtils.isEmpty(values)) {
      SqlInsert insert = new SqlInsert(Dimensions.TBL_EXTRA_DIMENSIONS).addAll(values);

      ResponseObject insertResponse = qs.insertDataWithResponse(insert);
      if (insertResponse.hasErrors()) {
        return insertResponse;
      }

      SqlUpdate update = new SqlUpdate(tbl)
          .addConstant(Dimensions.COL_EXTRA_DIMENSIONS, insertResponse.getResponseAsLong())
          .setWhere(sys.idEquals(tbl, id));

      return qs.updateDataWithResponse(update);

    } else {
      return ResponseObject.emptyResponse();
    }
  }

  private ResponseObject maybeCreateTradeAccounts(String tbl, long id, Map<String, ?> values) {
    if (!BeeUtils.isEmpty(values)) {
      SqlInsert insert = new SqlInsert(TradeAccounts.TBL_TRADE_ACCOUNTS).addAll(values);

      ResponseObject insertResponse = qs.insertDataWithResponse(insert);
      if (insertResponse.hasErrors()) {
        return insertResponse;
      }

      SqlUpdate update = new SqlUpdate(tbl)
          .addConstant(TradeAccounts.COL_TRADE_ACCOUNTS, insertResponse.getResponseAsLong())
          .setWhere(sys.idEquals(tbl, id));

      return qs.updateDataWithResponse(update);

    } else {
      return ResponseObject.emptyResponse();
    }
  }

  private Multimap<Long, Triplet<Long, String, Double>> getStockByWarehouse(
      Collection<Long> items) {
    Multimap<Long, Triplet<Long, String, Double>> result = ArrayListMultimap.create();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM)
        .addFields(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE)
        .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
        .addSum(TBL_TRADE_STOCK, COL_STOCK_QUANTITY)
        .addFrom(TBL_TRADE_STOCK)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM))
        .addFromInner(TBL_WAREHOUSES, sys.joinTables(TBL_WAREHOUSES,
            TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE))
        .setWhere(SqlUtils.and(SqlUtils.inList(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, items),
            SqlUtils.nonZero(TBL_TRADE_STOCK, COL_STOCK_QUANTITY)))
        .addGroup(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM)
        .addGroup(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE)
        .addGroup(TBL_WAREHOUSES, COL_WAREHOUSE_CODE);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        Double quantity = row.getDouble(COL_STOCK_QUANTITY);

        if (BeeUtils.nonZero(quantity)) {
          result.put(row.getLong(COL_ITEM), Triplet.of(row.getLong(COL_STOCK_WAREHOUSE),
              row.getValue(COL_WAREHOUSE_CODE), quantity));
        }
      }
    }

    return result;
  }

  private void addTradeReservationsFrom(SqlSelect query) {
    query.addFrom(TBL_TRADE_DOCUMENT_ITEMS)
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromInner(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM));
  }

  private static IsCondition getTradeReservationsCondition(Long warehouse, Collection<Long> items,
      DateTime dateTo) {

    Set<OperationType> operationTypes = new HashSet<>();
    for (OperationType ot : OperationType.values()) {
      if (ot.consumesStock()) {
        operationTypes.add(ot);
      }
    }

    HasConditions where = SqlUtils.and(
        SqlUtils.equals(TBL_TRADE_DOCUMENTS, COL_TRADE_DOCUMENT_PHASE, TradeDocumentPhase.ORDER),
        SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE, operationTypes),
        SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE),
        SqlUtils.positive(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY));

    if (DataUtils.isId(warehouse)) {
      where.add(SqlUtils.or(
          SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_WAREHOUSE_FROM, warehouse),
          SqlUtils.and(
              SqlUtils.isNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_WAREHOUSE_FROM),
              SqlUtils.equals(TBL_TRADE_DOCUMENTS, COL_TRADE_WAREHOUSE_FROM, warehouse))));
    }

    if (!BeeUtils.isEmpty(items)) {
      where.add(SqlUtils.inList(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, items));
    }

    if (dateTo != null) {
      where.add(SqlUtils.less(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE, dateTo));
    }

    return where;
  }

  private Multimap<Long, ItemQuantities> getTradeReservations(Long warehouse,
      Collection<Long> items) {

    Multimap<Long, ItemQuantities> reservations = ArrayListMultimap.create();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE)
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, COL_TRADE_ITEM_ARTICLE,
            COL_TRADE_ITEM_QUANTITY);

    addTradeReservationsFrom(query);
    query.setWhere(getTradeReservationsCondition(warehouse, items, null));

    query.addOrder(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, COL_TRADE_ITEM_ARTICLE)
        .addOrder(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      Long lastItem = null;
      ItemQuantities itemQuantities = null;

      for (SimpleRow row : data) {
        Long item = row.getLong(COL_ITEM);

        String article = row.getValue(COL_TRADE_ITEM_ARTICLE);
        article = BeeUtils.isEmpty(article) ? null : BeeUtils.trim(article);

        if (!Objects.equals(item, lastItem)) {
          lastItem = item;
          itemQuantities = null;
        }

        if (itemQuantities == null || !Objects.equals(itemQuantities.getArticle(), article)) {
          itemQuantities = new ItemQuantities(article);
          reservations.put(item, itemQuantities);
        }

        itemQuantities.addReserved(row.getDateTime(COL_TRADE_DATE),
            row.getDouble(COL_TRADE_ITEM_QUANTITY));
      }
    }

    return reservations;
  }

  private Map<String, Double> getTradeReservationsInfo(Long warehouse, Long item, DateTime dateTo) {
    Map<String, Double> info = new LinkedHashMap<>();
    if (!DataUtils.isId(item)) {
      return info;
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE, COL_TRADE_NUMBER)
        .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY);

    addTradeReservationsFrom(query);
    query.addFromLeft(TBL_COMPANIES,
        sys.joinTables(TBL_COMPANIES, TBL_TRADE_DOCUMENTS, COL_TRADE_CUSTOMER));

    query.setWhere(getTradeReservationsCondition(warehouse, Collections.singleton(item), dateTo));

    query.addOrder(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE, COL_TRADE_NUMBER);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo();

      data.forEach(row -> {
        String key = BeeUtils.joinItems(
            Formatter.renderDateTime(dtfInfo, row.getDateTime(COL_TRADE_DATE)),
            row.getValue(COL_TRADE_NUMBER),
            row.getValue(COL_COMPANY_NAME));

        info.merge(key, row.getDouble(COL_TRADE_ITEM_QUANTITY), Double::sum);
      });
    }

    return info;
  }

  private Map<Long, Double> getAverageCost(Long warehouse, Collection<Long> items) {
    Map<Long, Double> result = new HashMap<>();

    String colAmount = SqlUtils.uniqueName(COL_TRADE_ITEM_COST);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM)
        .addFields(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST_CURRENCY)
        .addSum(TBL_TRADE_STOCK, COL_STOCK_QUANTITY)
        .addSum(SqlUtils.multiply(
            SqlUtils.field(TBL_TRADE_STOCK, COL_STOCK_QUANTITY),
            SqlUtils.field(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST)), colAmount)
        .addFrom(TBL_TRADE_STOCK)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_STOCK, COL_PRIMARY_DOCUMENT_ITEM))
        .addFromInner(TBL_TRADE_ITEM_COST, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_ITEM_COST, COL_TRADE_DOCUMENT_ITEM))
        .addGroup(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM)
        .addGroup(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST_CURRENCY)
        .addOrder(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM);

    HasConditions where = SqlUtils.and(SqlUtils.nonZero(TBL_TRADE_STOCK, COL_STOCK_QUANTITY),
        SqlUtils.notNull(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST_CURRENCY));

    if (DataUtils.isId(warehouse)) {
      where.add(SqlUtils.equals(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE, warehouse));
    }

    if (!BeeUtils.isEmpty(items)) {
      where.add(SqlUtils.inList(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, items));
    }

    SimpleRowSet data = qs.getData(query.setWhere(where));

    if (!DataUtils.isEmpty(data)) {
      Long currencyTo = prm.getRelation(PRM_CURRENCY);
      DateTime date = TimeUtils.nowMinutes();

      Long lastItem = null;
      double totalQuantity = BeeConst.DOUBLE_ZERO;
      double totalAmount = BeeConst.DOUBLE_ZERO;

      for (SimpleRow row : data) {
        Long item = row.getLong(COL_ITEM);

        Double quantity = row.getDouble(COL_STOCK_QUANTITY);
        Double amount = adm.maybeExchange(row.getLong(COL_TRADE_ITEM_COST_CURRENCY), currencyTo,
            row.getDouble(colAmount), date);

        if (!Objects.equals(lastItem, item)) {
          if (lastItem != null && BeeUtils.nonZero(totalQuantity)) {
            result.put(lastItem, totalAmount / totalQuantity);
          }

          lastItem = item;
          totalQuantity = BeeConst.DOUBLE_ZERO;
          totalAmount = BeeConst.DOUBLE_ZERO;
        }

        totalQuantity += BeeUtils.unbox(quantity);
        totalAmount += BeeUtils.unbox(amount);
      }

      if (lastItem != null && BeeUtils.nonZero(totalQuantity)) {
        result.put(lastItem, totalAmount / totalQuantity);
      }
    }

    return result;
  }

  private String getNextDocumentNumber(long id, DateTime date, String series, String prefix,
      Integer length) {

    String tbl = TBL_TRADE_DOCUMENTS;

    String pfx = parseNumberPrefix(id, date, prefix);
    int len = BeeUtils.positive(length, DEFAULT_SERIES_NUMBER_LENGTH);

    HasConditions where = SqlUtils.and();
    where.add(SqlUtils.equals(tbl, COL_TRADE_SERIES, series));

    if (DataUtils.isId(id)) {
      where.add(SqlUtils.notEqual(tbl, sys.getIdName(tbl), id));
    }

    if (!BeeUtils.isEmpty(pfx)) {
      where.add(SqlUtils.startsWith(tbl, COL_TRADE_NUMBER, pfx));
    }

    IsExpression lengthExpression = SqlUtils.length(tbl, COL_TRADE_NUMBER);

    SqlSelect maxLengthQuery = new SqlSelect()
        .addMax(lengthExpression, SqlUtils.temporaryName("len"))
        .addFrom(tbl)
        .setWhere(where);

    int maxLength = BeeUtils.unbox(qs.getInt(maxLengthQuery));
    int pfxLength = BeeUtils.length(pfx);

    int max = 0;

    if (maxLength > pfxLength) {
      for (int i = pfxLength + 1; i <= maxLength; i++) {
        IsExpression charExpression = SqlUtils.substring(tbl, COL_TRADE_NUMBER, i, 1);

        where.add(SqlUtils.moreEqual(charExpression, BeeConst.STRING_ZERO));
        where.add(SqlUtils.lessEqual(charExpression, BeeUtils.toString(BeeConst.CHAR_NINE)));

        SqlSelect maxQuery = new SqlSelect()
            .addMax(tbl, COL_TRADE_NUMBER)
            .addFrom(tbl)
            .setWhere(SqlUtils.and(where, SqlUtils.equals(lengthExpression, i)));

        String maxNumber = qs.getValue(maxQuery);

        if (BeeUtils.length(maxNumber) == i) {
          max = Math.max(max, BeeUtils.toInt(BeeUtils.right(maxNumber, i - pfxLength)));
        }
      }
    }

    return BeeUtils.trim(pfx)
        + BeeUtils.padLeft(BeeUtils.toString(max + 1), len, BeeConst.CHAR_ZERO);
  }

  private String parseNumberPrefix(long id, DateTime date, String prefix) {
    if (BeeUtils.isEmpty(prefix)) {
      return null;
    }

    String result = prefix.trim();

    if (prefix.contains(BeeConst.STRING_LEFT_BRACE)
        && prefix.contains(BeeConst.STRING_RIGHT_BRACE)) {

      DateTime dt;

      if (date == null && DataUtils.isId(id)) {
        dt = qs.getDateTimeById(TBL_TRADE_DOCUMENTS, id, COL_TRADE_DATE);
      } else {
        dt = DateTime.copyOf(date);
      }

      if (dt != null) {
        String y = TimeUtils.yearToString(dt.getYear());
        String y2 = BeeUtils.right(y, 2);
        String m = TimeUtils.monthToString(dt.getMonth());
        String d = TimeUtils.dayOfMonthToString(dt.getDom());

        for (String s : new String[] {"y", "y4", "yyyy"}) {
          result = BeeUtils.replaceSame(result, BeeUtils.embrace(s), y);
        }
        for (String s : new String[] {"yy", "y2"}) {
          result = BeeUtils.replaceSame(result, BeeUtils.embrace(s), y2);
        }

        for (String s : new String[] {"m", "mm", "m2"}) {
          result = BeeUtils.replaceSame(result, BeeUtils.embrace(s), m);
        }
        for (String s : new String[] {"d", "dd", "d2"}) {
          result = BeeUtils.replaceSame(result, BeeUtils.embrace(s), d);
        }
      }
    }

    return result;
  }

  private ResponseObject getItemStockByWarehouse(RequestInfo reqInfo) {
    Long item = reqInfo.getParameterLong(COL_ITEM);
    if (!DataUtils.isId(item)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_ITEM);
    }

    List<Triplet<String, Double, Double>> result = new ArrayList<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE)
        .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
        .addSum(TBL_TRADE_STOCK, COL_STOCK_QUANTITY)
        .addFrom(TBL_TRADE_STOCK)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM))
        .addFromInner(TBL_WAREHOUSES, sys.joinTables(TBL_WAREHOUSES,
            TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TRADE_DOCUMENT_ITEMS, COL_ITEM, item),
            SqlUtils.nonZero(TBL_TRADE_STOCK, COL_STOCK_QUANTITY)))
        .addGroup(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE)
        .addGroup(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
        .addOrder(TBL_WAREHOUSES, COL_WAREHOUSE_CODE);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      Collection<Long> items = Collections.singleton(item);

      for (SimpleRow row : data) {
        Double stock = row.getDouble(COL_STOCK_QUANTITY);

        if (BeeUtils.nonZero(stock)) {
          Multimap<Long, ItemQuantities> reservations =
              getReservations(row.getLong(COL_STOCK_WAREHOUSE), items);

          Double reserved;
          if (reservations.containsKey(item)) {
            reserved = reservations.get(item).stream()
                .mapToDouble(ItemQuantities::getReserved).sum();
          } else {
            reserved = null;
          }

          result.add(Triplet.of(row.getValue(COL_WAREHOUSE_CODE), stock, reserved));
        }
      }
    }

    if (result.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.responseWithSize(result);
    }
  }

  private ResponseObject getRelatedTradeItems(RequestInfo reqInfo) {
    Long id = reqInfo.getParameterLong(Service.VAR_ID);
    if (!DataUtils.isId(id)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), Service.VAR_ID);
    }

    Long parent = reqInfo.getParameterLong(COL_TRADE_ITEM_PARENT);

    Multimap<Integer, Long> idsByLevel = HashMultimap.create();
    int level = 0;

    while (DataUtils.isId(parent)) {
      level--;
      idsByLevel.put(level, parent);

      parent = qs.getLongById(TBL_TRADE_DOCUMENT_ITEMS, parent, COL_TRADE_ITEM_PARENT);

      if (parent == null || idsByLevel.containsValue(parent) || level < -MAX_STOCK_DEPTH) {
        break;
      }
    }

    level = 0;

    Set<Long> parents = new HashSet<>();
    parents.add(id);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS))
        .addFrom(TBL_TRADE_DOCUMENT_ITEMS);

    while (!parents.isEmpty()) {
      query.setWhere(SqlUtils.inList(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT, parents));
      Set<Long> children = qs.getLongSet(query);

      children.removeAll(idsByLevel.values());
      if (children.isEmpty() || level > MAX_STOCK_DEPTH) {
        break;
      }

      level++;
      idsByLevel.putAll(level, children);

      parents.clear();
      parents.addAll(children);
    }

    if (idsByLevel.isEmpty()) {
      return ResponseObject.emptyResponse();
    }

    BeeRowSet result = null;

    List<Integer> levels = new ArrayList<>(idsByLevel.keySet());
    levels.sort(null);

    for (int key : levels) {
      BeeRowSet rowSet = qs.getViewData(VIEW_TRADE_MOVEMENT, Filter.idIn(idsByLevel.get(key)));

      if (!DataUtils.isEmpty(rowSet)) {
        rowSet.forEach(row -> row.setProperty(PROP_LEVEL, key));

        if (result == null) {
          result = rowSet;
        } else {
          result.addRows(rowSet.getRows());
        }
      }
    }

    if (DataUtils.isEmpty(result)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private ResponseObject submitPayment(RequestInfo reqInfo) {
    Long time = reqInfo.getParameterLong(COL_TRADE_PAYMENT_DATE);
    if (time == null) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_TRADE_PAYMENT_DATE);
    }

    Long account = reqInfo.getParameterLong(COL_TRADE_PAYMENT_ACCOUNT);
    Long paymentType = reqInfo.getParameterLong(COL_TRADE_PAYMENT_TYPE);

    String series = reqInfo.getParameter(COL_TRADE_PAYMENT_SERIES);
    String number = reqInfo.getParameter(COL_TRADE_PAYMENT_NUMBER);

    if (!BeeUtils.isEmpty(series) && BeeUtils.isEmpty(number)) {
      number = qs.getNextNumber(TBL_TRADE_PAYMENTS, COL_TRADE_PAYMENT_NUMBER,
          series, COL_TRADE_PAYMENT_SERIES);
    }

    Set<Long> docIds = new HashSet<>();

    ResponseObject response = ResponseObject.emptyResponse();

    boolean finEnabled = Module.FINANCE.isEnabled();

    if (reqInfo.hasParameter(VAR_PAYMENTS)) {
      Map<String, String> payments = Codec.deserializeHashMap(reqInfo.getParameter(VAR_PAYMENTS));

      if (!payments.isEmpty()) {
        for (Map.Entry<String, String> entry : payments.entrySet()) {
          Long docId = BeeUtils.toLongOrNull(entry.getKey());
          double amount = Localized.normalizeMoney(BeeUtils.toDoubleOrNull(entry.getValue()));

          if (DataUtils.isId(docId) && BeeUtils.isPositive(amount)) {
            SqlInsert insert = new SqlInsert(TBL_TRADE_PAYMENTS)
                .addConstant(COL_TRADE_DOCUMENT, docId)
                .addConstant(COL_TRADE_PAYMENT_DATE, time)
                .addConstant(COL_TRADE_PAYMENT_AMOUNT, amount)
                .addNotNull(COL_TRADE_PAYMENT_ACCOUNT, account)
                .addNotNull(COL_TRADE_PAYMENT_TYPE, paymentType)
                .addNotEmpty(COL_TRADE_PAYMENT_SERIES, series)
                .addNotEmpty(COL_TRADE_PAYMENT_NUMBER, number);

            ResponseObject insertResponse = qs.insertDataWithResponse(insert);
            if (insertResponse.hasErrors()) {
              return insertResponse;
            }

            docIds.add(docId);
          }
        }
      }
    }

    if (finEnabled && reqInfo.hasParameter(VAR_PREPAYMENT)) {
      double prepayment = Localized.normalizeMoney(reqInfo.getParameterDouble(VAR_PREPAYMENT));

      if (BeeUtils.isPositive(prepayment)) {
        DebtKind debtKind = reqInfo.getParameterEnum(VAR_KIND, DebtKind.class);

        Long payer = reqInfo.getParameterLong(COL_TRADE_PAYER);
        Long currency = reqInfo.getParameterLong(COL_TRADE_CURRENCY);

        if (!DataUtils.isId(account) && DataUtils.isId(paymentType)) {
          account = qs.getLongById(TBL_PAYMENT_TYPES, paymentType, COL_TRADE_PAYMENT_TYPE_ACCOUNT);
        }

        List<String> messages = new ArrayList<>();

        if (debtKind == null) {
          messages.add(Localized.dictionary().parameterNotFound(VAR_KIND));
        }

        if (!DataUtils.isId(payer)) {
          messages.add(Localized.dictionary().parameterNotFound(COL_TRADE_PAYER));
        }
        if (!DataUtils.isId(currency)) {
          messages.add(Localized.dictionary().parameterNotFound(COL_TRADE_CURRENCY));
        }

        if (!DataUtils.isId(currency)) {
          messages.add("payment account not available");
        }

        if (messages.isEmpty()) {
          ResponseObject finResponse = fin.addPrepayment(debtKind.getPrepaymentKind(),
              new DateTime(time), payer, account, series, number, prepayment, currency);
          if (finResponse.hasErrors()) {
            return finResponse;
          }

          response.addMessagesFrom(finResponse);

        } else {
          response.addWarning(reqInfo.getLabel(), "cannot build prepayment");
          messages.forEach(response::addWarning);
        }
      }
    }

    if (!docIds.isEmpty()) {
      Endpoint.refreshRows(qs.getViewData(VIEW_TRADE_DOCUMENTS, Filter.idIn(docIds)));
      Endpoint.refreshChildren(VIEW_TRADE_PAYMENTS, docIds);
    }

    return response;
  }

  private ResponseObject dischargeDebt(RequestInfo reqInfo) {
    Long time = reqInfo.getParameterLong(COL_TRADE_PAYMENT_DATE);
    if (time == null) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_TRADE_PAYMENT_DATE);
    }

    List<Triplet<Long, Long, Double>> discharges =
        deserializeDischarges(reqInfo.getParameter(VAR_PAYMENTS));
    if (discharges.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), VAR_PAYMENTS);
    }

    String series = reqInfo.getParameter(COL_TRADE_PAYMENT_SERIES);
    String number = reqInfo.getParameter(COL_TRADE_PAYMENT_NUMBER);

    Dictionary dictionary = usr.getDictionary();

    Long account = fin.getDefaultAccount(COL_DISCHARGE_ACCOUNT);
    if (!DataUtils.isId(account)) {
      return ResponseObject.error(dictionary.finDefaultAccounts(),
          dictionary.fieldRequired(dictionary.finDischargeAccount()));
    }

    if (!BeeUtils.isEmpty(series) && BeeUtils.isEmpty(number)) {
      number = qs.getNextNumber(TBL_TRADE_PAYMENTS, COL_TRADE_PAYMENT_NUMBER,
          series, COL_TRADE_PAYMENT_SERIES);
    }

    Set<Long> docIds = new HashSet<>();

    for (Triplet<Long, Long, Double> discharge : discharges) {
      double amount = discharge.getC();

      for (long docId : new long[] {discharge.getA(), discharge.getB()}) {
        SqlInsert insert = new SqlInsert(TBL_TRADE_PAYMENTS)
            .addConstant(COL_TRADE_DOCUMENT, docId)
            .addConstant(COL_TRADE_PAYMENT_DATE, time)
            .addConstant(COL_TRADE_PAYMENT_AMOUNT, amount)
            .addConstant(COL_TRADE_PAYMENT_ACCOUNT, account)
            .addNotEmpty(COL_TRADE_PAYMENT_SERIES, series)
            .addNotEmpty(COL_TRADE_PAYMENT_NUMBER, number);

        ResponseObject insertResponse = qs.insertDataWithResponse(insert);
        if (insertResponse.hasErrors()) {
          return insertResponse;
        }

        docIds.add(docId);
      }
    }

    if (!docIds.isEmpty()) {
      Endpoint.refreshRows(qs.getViewData(VIEW_TRADE_DOCUMENTS, Filter.idIn(docIds)));
      Endpoint.refreshChildren(VIEW_TRADE_PAYMENTS, docIds);
    }

    return ResponseObject.response(docIds.size());
  }

  private ResponseObject dischargePrepayment(RequestInfo reqInfo) {
    PrepaymentKind prepaymentKind = reqInfo.getParameterEnum(VAR_KIND, PrepaymentKind.class);
    if (prepaymentKind == null) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), VAR_KIND);
    }

    Long time = reqInfo.getParameterLong(COL_TRADE_PAYMENT_DATE);
    if (time == null) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), COL_TRADE_PAYMENT_DATE);
    }

    List<Triplet<Long, Long, Double>> discharges =
        deserializeDischarges(reqInfo.getParameter(VAR_PREPAYMENT));
    if (discharges.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), VAR_PREPAYMENT);
    }

    Dictionary dictionary = usr.getDictionary();

    BeeRowSet config = qs.getViewData(VIEW_FINANCE_CONFIGURATION);
    if (DataUtils.isEmpty(config)) {
      return ResponseObject.error(dictionary.dataNotAvailable(dictionary.finDefaultAccounts()));
    }

    TradeAccounts tradeAccounts = TradeAccounts.createAvailable(config, config.getRow(0));
    Long debtAccount = prepaymentKind.getDebtKInd().getTradeAccount(tradeAccounts);
    if (!DataUtils.isId(debtAccount)) {
      return ResponseObject.error(prepaymentKind.getDebtKInd().getCaption(dictionary),
          "default account not available");
    }

    Set<Long> finIds = discharges.stream().map(Triplet::getA).collect(Collectors.toSet());
    Filter filter = Filter.and(Filter.idIn(finIds),
        Filter.equals(COL_FIN_PREPAYMENT_KIND, prepaymentKind));

    BeeRowSet finData = qs.getViewData(VIEW_FINANCIAL_RECORDS, filter);
    if (DataUtils.isEmpty(finData)) {
      return ResponseObject.error(dictionary.dataNotAvailable(VIEW_FINANCIAL_RECORDS));
    }

    int prepaymentAccountIndex =
        finData.getColumnIndex(prepaymentKind.getPrepaymentAccountColumn());

    int seriesIndex = finData.getColumnIndex(prepaymentKind.getSeriesColumn());
    int numberIndex = finData.getColumnIndex(prepaymentKind.getDocumentColumn());

    int journalIndex = finData.getColumnIndex(COL_FIN_JOURNAL);
    int companyIndex = finData.getColumnIndex(COL_FIN_COMPANY);
    int currencyIndex = finData.getColumnIndex(COL_FIN_CURRENCY);

    ResponseObject response = ResponseObject.emptyResponse();

    Set<Long> docIds = new HashSet<>();
    int count = 0;

    for (Triplet<Long, Long, Double> discharge : discharges) {
      long finId = discharge.getA();
      long docId = discharge.getB();

      double amount = discharge.getC();

      BeeRow finRow = finData.getRowById(finId);
      if (finRow == null) {
        response.addWarning(VIEW_FINANCIAL_RECORDS, dictionary.keyNotFound(finId));

      } else {
        Long prepaymentAccount = finRow.getLong(prepaymentAccountIndex);
        if (Objects.equals(debtAccount, prepaymentAccount)) {
          response.addWarning(VIEW_FINANCIAL_RECORDS, finId,
              "debt account equals prepayment account");

        } else {
          SqlInsert insertPayment = new SqlInsert(TBL_TRADE_PAYMENTS)
              .addConstant(COL_TRADE_DOCUMENT, docId)
              .addConstant(COL_TRADE_PAYMENT_DATE, time)
              .addConstant(COL_TRADE_PAYMENT_AMOUNT, amount)
              .addConstant(COL_TRADE_PAYMENT_ACCOUNT, prepaymentAccount)
              .addNotEmpty(COL_TRADE_PAYMENT_SERIES, finRow.getString(seriesIndex))
              .addNotEmpty(COL_TRADE_PAYMENT_NUMBER, finRow.getString(numberIndex))
              .addConstant(COL_TRADE_PREPAYMENT_PARENT, finId);

          ResponseObject paymentResponse = qs.insertDataWithResponse(insertPayment);
          if (paymentResponse.hasErrors()) {
            return paymentResponse;
          }

          Long paymentId = paymentResponse.getResponseAsLong();

          SqlInsert insertFin = new SqlInsert(TBL_FINANCIAL_RECORDS)
              .addNotNull(COL_FIN_JOURNAL, finRow.getLong(journalIndex))
              .addConstant(COL_FIN_DATE, time)
              .addNotNull(COL_FIN_COMPANY, finRow.getLong(companyIndex))
              .addConstant(prepaymentKind.getOppositeAccountColumn(), prepaymentAccount)
              .addConstant(prepaymentKind.getPrepaymentAccountColumn(), debtAccount)
              .addConstant(COL_FIN_AMOUNT, amount)
              .addConstant(COL_FIN_CURRENCY, finRow.getLong(currencyIndex))
              .addConstant(COL_FIN_TRADE_DOCUMENT, docId)
              .addConstant(COL_FIN_TRADE_PAYMENT, paymentId)
              .addConstant(COL_FIN_PREPAYMENT_PARENT, finId);

          ResponseObject finResponse = qs.insertDataWithResponse(insertFin);
          if (finResponse.hasErrors()) {
            return finResponse;
          }

          docIds.add(docId);
          count++;
        }
      }
    }

    if (!docIds.isEmpty()) {
      Endpoint.refreshRows(qs.getViewData(VIEW_TRADE_DOCUMENTS, Filter.idIn(docIds)));
      Endpoint.refreshChildren(VIEW_TRADE_PAYMENTS, docIds);
    }

    response.setResponse(count);
    return response;
  }

  private static List<Triplet<Long, Long, Double>> deserializeDischarges(String input) {
    List<Triplet<Long, Long, Double>> discharges = new ArrayList<>();

    for (String s : Codec.deserializeList(input)) {
      Triplet<String, String, String> triplet = Triplet.restore(s);

      if (triplet != null) {
        Long a = BeeUtils.toLongOrNull(triplet.getA());
        Long b = BeeUtils.toLongOrNull(triplet.getB());
        Double c = BeeUtils.toDoubleOrNull(triplet.getC());

        if (DataUtils.isId(a) && DataUtils.isId(b) && BeeUtils.isPositive(c)) {
          discharges.add(Triplet.of(a, b, c));
        }
      }
    }

    return discharges;
  }
}