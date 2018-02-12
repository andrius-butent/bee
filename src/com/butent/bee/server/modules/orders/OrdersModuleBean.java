package com.butent.bee.server.modules.orders;

import com.butent.bee.server.modules.transport.ShipmentRequestsWorker;
import com.butent.bee.server.rest.RestResponse;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.css.values.Rest;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.time.JustDate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.projects.ProjectConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.concurrency.ConcurrencyBean.HasTimerService;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.orders.OrdersConstants;
import com.butent.bee.shared.modules.orders.OrdersConstants.*;
import com.butent.bee.shared.modules.trade.Totalizer;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.webservice.ButentWS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.net.ssl.HttpsURLConnection;

@Stateless
@LocalBean
public class OrdersModuleBean implements BeeModule, HasTimerService {

  private static BeeLogger logger = LogUtils.getLogger(OrdersModuleBean.class);

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  ParamHolderBean prm;
  @EJB
  ConcurrencyBean cb;
  @EJB
  TradeModuleBean trd;
  @EJB
  DataEditorBean deb;

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    return null;
  }

  @Override
  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response;

    String svc = BeeUtils.trim(service);

    switch (svc) {
      case SVC_GET_ITEMS_FOR_SELECTION:
        response = getItemsForSelection(reqInfo);
        break;

      case SVC_GET_TMPL_ITEMS_FOR_SELECTION:
        response = getTmplItemsForSelection(reqInfo);
        break;

      case SVC_GET_TEMPLATE_ITEMS:
        response = getTemplateItems(reqInfo);
        break;

      case OrdersConstants.SVC_CREATE_INVOICE_ITEMS:
        response = createInvoiceItems(reqInfo);
        break;

      case SVC_FILL_RESERVED_REMAINDERS:
        response = fillReservedRemainders(reqInfo);
        break;

      case SVC_GET_ERP_STOCKS:
        Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(Service.VAR_DATA));
        getERPStocks(ids);
        response = ResponseObject.emptyResponse();
        break;

      case SVC_GET_CREDIT_INFO:
        response = getCreditInfo(reqInfo);
        break;

      case SVC_FILTER_COMPONENTS:
        response = filterComponents(reqInfo);
        break;

      case SVC_CHECK_FOR_COMPLECTS:
        response = checkForComplects(reqInfo);
        break;

      case SVC_UPDATE_ORDER_ITEMS:
        response = updateOrderItems(reqInfo);
        break;

      case SVC_GET_COMPLECT_FREE_REMAINDERS:
        response = getComplectFreeRemainders(reqInfo);
        break;

      case SVC_INSERT_COMPLECTS:
        response = insertComplects(reqInfo);
        break;

      case SVC_GET_ALL_ORDER_ITEMS:
        response = getAllOrderItems(reqInfo);
        break;

      case SVC_JOIN_INVOICES:
        response = joinInvoices(reqInfo);
        break;

      case SVC_ORDER_REPORTS:
        response = getReportData(reqInfo);
        break;
      case SVC_GET_SUPPLIER_TERM_VALUES:
        response = getSupplierTermValues(reqInfo);
        break;

      default:
        String msg = BeeUtils.joinWords("service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  @Override
  public void ejbTimeout(Timer timer) {
    if (cb.isParameterTimer(timer, PRM_CLEAR_RESERVATIONS_TIME)) {
      clearReservations();
    }
    if (cb.isParameterTimer(timer, PRM_IMPORT_ERP_ITEMS_TIME)) {
      getERPItems();
    }
    if (cb.isParameterTimer(timer, PRM_IMPORT_ERP_STOCKS_TIME)) {
      getERPStocks(null);
    }
    if (cb.isParameterTimer(timer, PRM_EXPORT_ERP_RESERVATIONS_TIME)) {
      exportReservations();
    }
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createBoolean(module, PRM_UPDATE_ITEMS_PRICES),
        BeeParameter.createNumber(module, PRM_CLEAR_RESERVATIONS_TIME),
        BeeParameter.createNumber(module, PRM_IMPORT_ERP_ITEMS_TIME),
        BeeParameter.createNumber(module, PRM_IMPORT_ERP_STOCKS_TIME),
        BeeParameter.createNumber(module, PRM_EXPORT_ERP_RESERVATIONS_TIME),
        BeeParameter.createRelation(module, PRM_DEFAULT_SALE_OPERATION, false,
            VIEW_TRADE_OPERATIONS, COL_OPERATION_NAME),
        BeeParameter.createNumber(module, PRM_MANAGER_DISCOUNT),
        BeeParameter.createRelation(module, PRM_MANAGER_WAREHOUSE, true, VIEW_WAREHOUSES,
            COL_WAREHOUSE_CODE),
        BeeParameter.createBoolean(module, PRM_CHECK_DEBT),
        BeeParameter.createBoolean(module, PRM_NOTIFY_ABOUT_DEBTS),
        BeeParameter.createRelation(module, PRM_SUPPLIER_TERM_COMPANY, false, VIEW_COMPANIES,
                COL_COMPANY_NAME, COL_COMPANY_CODE),
        BeeParameter.createText(module, PRM_SUPPLIER_TERM_ADDRESS));

    return params;
  }

  @Override
  public Module getModule() {
    return Module.ORDERS;
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
    cb.createIntervalTimer(this.getClass(), PRM_CLEAR_RESERVATIONS_TIME);
    cb.createIntervalTimer(this.getClass(), PRM_IMPORT_ERP_ITEMS_TIME);
    cb.createIntervalTimer(this.getClass(), PRM_IMPORT_ERP_STOCKS_TIME);
    cb.createIntervalTimer(this.getClass(), PRM_EXPORT_ERP_RESERVATIONS_TIME);

    sys.registerDataEventHandler(new DataEventHandler() {

      @Subscribe
      @AllowConcurrentEvents
      public void setOrderData(ViewQueryEvent event) {
        if ((event.isAfter(VIEW_ORDER_ITEMS) || event.isAfter(VIEW_ORDER_TMPL_ITEMS)
            || event.isAfter(VIEW_ORDER_SALES) || event.isAfter(VIEW_ORDER_SALES_FILTERED))
            && event.hasData()) {

          BeeRowSet rowSet = event.getRowset();
          List<BeeColumn> cols = rowSet.getColumns();

          BeeRow row = rowSet.getRow(0);
          String viewName = event.getTargetName();

          String source;
          String column;

          if (Objects.equals(VIEW_ORDER_TMPL_ITEMS, event.getTargetName())) {
            source = VIEW_ORDER_TMPL_ITEMS;
            column = COL_TEMPLATE;
          } else {
            source = VIEW_ORDER_ITEMS;
            column = COL_ORDER;
          }
          Long target = row.getLong(DataUtils.getColumnIndex(column, cols));

          SqlSelect select = new SqlSelect()
              .addFields(source, COL_TRADE_ITEM_PARENT)
              .addFrom(source)
              .setWhere(SqlUtils.equals(source, column, target));

          Set<Long> parentIds = qs.getLongSet(select);
          Multimap<Long, BeeRow> itemMap = HashMultimap.create();

          BeeRowSet set = new BeeRowSet();
          set.addRows(rowSet.getRows());

          if (BeeUtils.inList(viewName, VIEW_ORDER_ITEMS, VIEW_ORDER_SALES_FILTERED)) {
            BeeRowSet components =  qs.getViewData(viewName, Filter.equals(column, target), null,
                DataUtils.getColumnNames(cols));

            if (!components.isEmpty()) {
              set.addRows(components.getRows());
            }
          }

          for (BeeRow item : set) {
            if (parentIds.contains(item.getId())) {
              item.setProperty(PROP_ITEM_COMPONENT, 1);
              itemMap.put(null, item);
            } else if (item.getLong(DataUtils.getColumnIndex(COL_TA_PARENT, cols)) != null) {
              item.setProperty(PROP_ITEM_COMPONENT, 0);
              itemMap.put(item.getLong(DataUtils.getColumnIndex(COL_TA_PARENT, cols)), item);
            } else {
              item.setProperty(PROP_ITEM_COMPONENT, 0);
              itemMap.put(item.getId(), item);
            }
          }

          if (BeeUtils.inListSame(viewName, VIEW_ORDER_ITEMS,
              VIEW_ORDER_SALES_FILTERED)) {
            setOrderFreeRemainders(itemMap, cols, event.getTargetName());
          }

          if (Objects.equals(viewName, VIEW_ORDER_SALES_FILTERED)) {
            filterOrderSales(rowSet, cols);
          }

          if (BeeUtils.inList(viewName, VIEW_ORDER_ITEMS, VIEW_ORDER_TMPL_ITEMS)) {
            fillOrderDiscounts(itemMap, cols);
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillOrderNumber(DataEvent.ViewModifyEvent event) {
        if (event.isBefore()
            && Objects.equals(sys.getViewSource(event.getTargetName()), TBL_ORDERS)) {
          List<BeeColumn> cols;
          IsRow row;
          Long series = null;

          if (event instanceof ViewInsertEvent) {
            cols = ((ViewInsertEvent) event).getColumns();
            row = ((ViewInsertEvent) event).getRow();
          } else if (event instanceof DataEvent.ViewUpdateEvent) {
            cols = ((DataEvent.ViewUpdateEvent) event).getColumns();
            row = ((DataEvent.ViewUpdateEvent) event).getRow();
          } else {
            return;
          }

          int seriesIdx = DataUtils.getColumnIndex(COL_TA_SERIES, cols);

          if (!BeeConst.isUndef(seriesIdx)) {
            series = row.getLong(seriesIdx);
          }
          if (DataUtils.isId(series)) {
            int numberIdx = DataUtils.getColumnIndex(COL_TA_NUMBER, cols);

            if (BeeConst.isUndef(numberIdx)) {
              cols.add(new BeeColumn(COL_TA_NUMBER));
              row.addValue(null);
              numberIdx = row.getNumberOfCells() - 1;

            } else if (!BeeUtils.isEmpty(row.getString(numberIdx))) {
              return;
            }
            row.setValue(numberIdx, qs.getNextNumber(TBL_ORDERS, COL_TA_NUMBER, null, null));
          }
        }
      }
    });
  }

  private ResponseObject getItems(BeeRowSet items, Long warehouse) {
    Map<Long, Double> freeRemainders = getFreeRemainders(items.getRowIds(), null, warehouse);
    Map<Long, Double> wrhRemainders = getWarehouseReminders(items.getRowIds(), warehouse);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
        .addFrom(TBL_WAREHOUSES)
        .setWhere(SqlUtils.equals(TBL_WAREHOUSES, sys.getIdName(TBL_WAREHOUSES), warehouse));

    String code = qs.getValue(query);
    Integer defaultVAT = prm.getInteger(PRM_VAT_PERCENT);

    BeeView remView = sys.getView(VIEW_ITEM_REMAINDERS);
    items.addColumn(ValueType.NUMBER, COL_TRADE_SUPPLIER);
    items.addColumn(ValueType.NUMBER, COL_UNPACKING);
    items.addColumn(ValueType.DATE, COL_DATE_TO);
    items.addColumn(ValueType.NUMBER, COL_DEFAULT_VAT);
    items.addColumn(remView.getBeeColumn(ALS_WAREHOUSE_CODE));
    items.addColumn(remView.getBeeColumn(COL_WAREHOUSE_REMAINDER));
    items.addColumn(ValueType.NUMBER, PRP_FREE_REMAINDER);
    items.addColumn(ValueType.NUMBER, COL_RESERVED_REMAINDER);

    for (BeeRow row : items) {
      Long itemId = row.getId();

      SqlSelect suppliersQry = new SqlSelect()
          .addFields(VIEW_ITEM_SUPPLIERS, COL_TRADE_SUPPLIER, COL_UNPACKING, COL_DATE_TO)
          .addFrom(VIEW_ITEM_SUPPLIERS)
          .setWhere(SqlUtils.equals(VIEW_ITEM_SUPPLIERS, COL_ITEM, itemId));

      SimpleRowSet suppliers = qs.getData(suppliersQry);

      if (suppliers.getNumberOfRows() == 1) {
        row.setValue(row.getNumberOfCells() - 8, suppliers.getLong(0, COL_TRADE_SUPPLIER));
        row.setValue(row.getNumberOfCells() - 7, suppliers.getDouble(0, COL_UNPACKING));
        row.setValue(row.getNumberOfCells() - 6, suppliers.getDate(0, COL_DATE_TO));
      }

      Double free = freeRemainders.get(itemId);
      double wrhReminder = BeeConst.DOUBLE_ZERO;

      if (wrhRemainders.size() > 0) {
        wrhReminder = BeeUtils.unbox(wrhRemainders.get(itemId));
      }
      row.setValue(row.getNumberOfCells() - 5, defaultVAT);
      row.setValue(row.getNumberOfCells() - 4, code);
      row.setValue(row.getNumberOfCells() - 3, wrhReminder);
      row.setValue(row.getNumberOfCells() - 2, free);
      row.setValue(row.getNumberOfCells() - 1, wrhReminder - free);
    }

    return ResponseObject.response(items);
  }

  private ResponseObject getItemsForSelection(RequestInfo reqInfo) {

    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);
    Long warehouse = reqInfo.getParameterLong(COL_WAREHOUSE);
    boolean remChecked = reqInfo.hasParameter(COL_WAREHOUSE_REMAINDER);

    CompoundFilter filter = Filter.and();
    filter.add(Filter.isNull(COL_ITEM_IS_SERVICE));

    if (!BeeUtils.isEmpty(where)) {
      filter.add(Filter.restore(where));
    }

    if (warehouse != null && !remChecked) {
      filter.add(Filter.in(sys.getIdName(TBL_ITEMS), VIEW_ITEM_REMAINDERS, COL_ITEM, Filter.and(
          Filter.equals(COL_WAREHOUSE, warehouse), Filter.notNull(COL_WAREHOUSE_REMAINDER))));
    }

    BeeRowSet items = qs.getViewData(VIEW_ITEMS, filter);

    if (DataUtils.isEmpty(items)) {
      logger.debug(reqInfo.getService(), "no items found", filter);
      return ResponseObject.emptyResponse();
    }

    return getItems(items, warehouse);
  }

  private ResponseObject getTmplItems(BeeRowSet items) {
    items.addColumn(ValueType.NUMBER, COL_DEFAULT_VAT);

    for (BeeRow row : items) {
      row.setValue(row.getNumberOfCells() - 1, prm.getInteger(PRM_VAT_PERCENT));
    }

    return ResponseObject.response(items);
  }

  private ResponseObject getTmplItemsForSelection(RequestInfo reqInfo) {

    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);

    CompoundFilter filter = Filter.and();
    filter.add(Filter.isNull(COL_ITEM_IS_SERVICE));

    if (!BeeUtils.isEmpty(where)) {
      filter.add(Filter.restore(where));
    }

    BeeRowSet items = qs.getViewData(VIEW_ITEMS, filter);

    if (DataUtils.isEmpty(items)) {
      logger.debug(reqInfo.getService(), "no items found", filter);
      return ResponseObject.emptyResponse();
    }

    return getTmplItems(items);
  }

  private ResponseObject getReportData(RequestInfo reqInfo) {
    ReportInfo report = ReportInfo.restore(reqInfo.getParameter(Service.VAR_DATA));

    HasConditions clause = SqlUtils.and();

    clause.add(report.getCondition(OrdersConstants.TBL_ORDERS, COL_ORDERS_STATUS));
    clause.add(report.getCondition(TBL_ORDER_TYPES, TransportConstants.COL_TYPE_NAME));
    clause.add(report.getCondition(OrdersConstants.TBL_ORDERS, COL_START_DATE));
    clause.add(report.getCondition(OrdersConstants.TBL_ORDERS, COL_END_DATE));
    clause.add(report.getCondition(TBL_COMPANIES, COL_COMPANY_NAME));
    clause.add(report.getCondition(TBL_WAREHOUSES, ClassifierConstants.COL_WAREHOUSE_CODE));
    clause.add(report.getCondition(TBL_ORDER_SERIES, COL_SERIES_NAME));
    clause.add(report.getCondition(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME),
        "' '", SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")), COL_TRADE_MANAGER));
    clause.add(report.getCondition(TBL_ITEMS, COL_ITEM_NAME));
    clause.add(report.getCondition(TBL_ITEMS, COL_ITEM_ARTICLE));
    clause.add(report.getCondition(TBL_ITEMS, COL_ITEM_BARCODE));
    clause.add(report.getCondition(TBL_UNITS, COL_UNIT_NAME));
    clause.add(report.getCondition(ALS_ITEM_TYPES, COL_CATEGORY_NAME));
    clause.add(report.getCondition(ALS_ITEM_GROUPS, COL_CATEGORY_NAME));
    clause.add(report.getCondition("SupplierCompany", COL_COMPANY_NAME));
    clause.add(report.getCondition(TBL_ORDER_ITEMS, COL_SUPPLIER_TERM));
    clause.add(report.getCondition(TBL_ORDER_ITEMS, COL_TRADE_ITEM_NOTE));
    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(TBL_ORDER_ITEMS,
        sys.getIdName(TBL_ORDER_ITEMS)), SqlConstants.SqlDataType.STRING, 20, 0), COL_ORDER_ITEM));

    SqlSelect select = new SqlSelect()
        .addFields(TBL_ORDERS, COL_ORDERS_STATUS, COL_START_DATE, COL_END_DATE, COL_TRADE_NUMBER)
        .addFields(TBL_ORDER_ITEMS, COL_DISCOUNT, COL_ITEM_PRICE, COL_TRADE_ITEM_QUANTITY,
            COL_TRADE_ITEM_NOTE, COL_SUPPLIER_TERM)
        .addField(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS), COL_ORDER_ITEM)
        .addField("SupplierCompany", COL_COMPANY_NAME, COL_TRADE_SUPPLIER)
        .addFields(TBL_ITEMS, COL_ITEM_NAME, COL_ITEM_ARTICLE, COL_ITEM_BARCODE)
        .addField(ALS_ITEM_TYPES, COL_CATEGORY_NAME, ALS_ITEM_TYPE_NAME)
        .addField(ALS_ITEM_GROUPS, COL_CATEGORY_NAME, ALS_ITEM_GROUP_NAME)
        .addField(TBL_UNITS, COL_UNIT_NAME, ALS_UNIT_NAME)
        .addFields(TBL_ORDER_TYPES, TransportConstants.COL_TYPE_NAME)
        .addFields(TBL_ORDER_SERIES, COL_SERIES_NAME)
        .addFields(TBL_WAREHOUSES, ClassifierConstants.COL_WAREHOUSE_CODE)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME),
            SqlUtils.constant(BeeConst.STRING_SPACE),
            SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME),
                SqlUtils.constant(BeeConst.STRING_EMPTY))), TradeConstants.COL_TRADE_MANAGER)
        .addFields(TBL_ORDER_ITEMS, COL_ORDER)
        .addFrom(TBL_ORDER_ITEMS)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER))
        .addFromLeft(TBL_ORDER_TYPES, sys.joinTables(TBL_ORDER_TYPES, TBL_ORDERS, COL_TYPE))
        .addFromLeft(TBL_ORDER_SERIES, sys.joinTables(TBL_ORDER_SERIES, TBL_ORDERS, COL_SERIES))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_COMPANY))
        .addFromLeft(TBL_COMPANIES, "SupplierCompany", sys.joinTables(TBL_COMPANIES,
            "SupplierCompany", TBL_ORDER_ITEMS, COL_TRADE_SUPPLIER))
        .addFromLeft(TBL_WAREHOUSES, sys.joinTables(TBL_WAREHOUSES, TBL_ORDERS, COL_WAREHOUSE))
        .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_TRADE_MANAGER))
        .addFromLeft(TBL_COMPANY_PERSONS, sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS,
            COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_ORDER_ITEMS, COL_ITEM))
        .addFromLeft(TBL_UNITS, sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT))
        .addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES, sys.joinTables(TBL_ITEM_CATEGORY_TREE,
            ALS_ITEM_TYPES, TBL_ITEMS, COL_TYPE))
        .addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS,
            sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS, TBL_ITEMS, COL_ITEM_GROUP))
        .setWhere(SqlUtils.and(clause, SqlUtils.isNull(TBL_ORDER_ITEMS, COL_TRADE_ITEM_PARENT)));

    return ResponseObject.response(qs.getData(select));
  }

  private ResponseObject getSupplierTermValues(RequestInfo reqInfo) {
    Multimap<String, String> orderItems = Codec.deserializeMultiMap(reqInfo.getParameter(VAR_ID_LIST));

    if (orderItems.isEmpty()) {
      return ResponseObject.error("Supplier list is empty");
    }

    StringBuilder xml = new StringBuilder("<SalesPricesRequest>")
            .append(XmlUtils.tag("MemberID", 12238))
            .append(XmlUtils.tag("MemberSerial", 65839434))
            .append(XmlUtils.tag("ServiceID", ""));
            for (String item : orderItems.values()) {
              xml.append("<Item>")
                      .append(XmlUtils.tag("ID", "10"))
                      .append(XmlUtils.tag("ProductID", item))
                      .append(XmlUtils.tag("Quantity", ""))
                      .append(XmlUtils.tag("Date", ""))
                      .append(XmlUtils.tag("PlantID", 0200))
                      .append(XmlUtils.tag("Price", ""))
                      .append("</Item>");
            }
            xml.append("</SalesPricesRequest>");

    String address = prm.getText(PRM_SUPPLIER_TERM_ADDRESS);
    if (BeeUtils.isEmpty(address)) {
      return ResponseObject.error("Empty WEB SERVICE address");
    }

    HttpsURLConnection connection = null;

    try {
      URL url = new URL(address);
      connection = (HttpsURLConnection) url.openConnection();

      connection.setRequestMethod("POST");
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setConnectTimeout( 20000 );
      connection.setReadTimeout( 20000 );
      connection.setUseCaches (false);
      connection.setDefaultUseCaches (false);
      connection.setRequestProperty ( "Content-Type", "text/xml" );

      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
      writer.write(String.valueOf(xml));
      writer.flush();
      writer.close();

      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String inputLine;
      StringBuilder response = new StringBuilder();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();

      SimpleRowSet rowSet = ButentWS.xmlToSimpleRowSet(response.toString(), "ProductID", "Date");
      Map<String, String> terms = new HashMap<>();

      if (!rowSet.isEmpty()) {
        for (SimpleRow row : rowSet) {
          String itemId = row.getValue("ProductID");

          JustDate date = TimeUtils.parseDate(row.getValue("Date"), DateOrdering.YMD);

          if (date.getYear() == 9999) {
            continue;
          }

          if (!BeeUtils.isEmpty(itemId) && date != null) {

            for (Entry<String, String> entry : orderItems.entries()) {
              if (Objects.equals(entry.getValue(), itemId)) {
                terms.put(entry.getKey(), BeeUtils.toString(date.getDays()));
              }
            }
          }
        }

        return ResponseObject.response(terms);
      }
    } catch (IOException e) {
      return ResponseObject.error("Error connecting via WEB SERVICE");
    } catch (BeeException e) {
      return ResponseObject.error("Can not convert data from WEB SERVICE");
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject createInvoiceItems(RequestInfo reqInfo) {
    Long saleId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SALE));
    Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
    Map<String, String> map =
        Codec.deserializeLinkedHashMap(reqInfo.getParameter(Service.VAR_DATA));
    Map<Long, Double> idsQty = new HashMap<>();

    for (Entry<String, String> entry : map.entrySet()) {
      idsQty.put(Long.valueOf(entry.getKey()), Double.valueOf(entry.getValue()));
    }

    if (!DataUtils.isId(saleId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(idsQty)) {
      return ResponseObject.error("Empty ID list");
    }

    IsCondition where = sys.idInList(TBL_ORDER_ITEMS, idsQty.keySet());

    SqlSelect query = new SqlSelect();
    query.addFields(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS), COL_ORDER, COL_TRADE_VAT_PLUS,
        TradeConstants.COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_INCOME_ITEM, COL_RESERVED_REMAINDER,
        COL_TRADE_DISCOUNT, COL_TRADE_ITEM_QUANTITY)
        .addFields(TBL_ITEMS, COL_ITEM_ARTICLE)
        .addFrom(TBL_ORDER_ITEMS)
        .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_ORDER_ITEMS, COL_ITEM))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER))
        .setWhere(where);

    IsExpression vatExch =
        ExchangeUtils.exchangeFieldTo(query, SqlUtils.field(TBL_ORDER_ITEMS, COL_TRADE_VAT),
            SqlUtils.field(TBL_ORDER_ITEMS, COL_TRADE_CURRENCY), SqlUtils.field(TBL_ORDERS,
                COL_DATES_START_DATE), SqlUtils.constant(currency));

    String vatAlias = "Vat_" + SqlUtils.uniqueName();

    String priceAlias = "Price_" + SqlUtils.uniqueName();
    IsExpression priceExch =
        ExchangeUtils.exchangeFieldTo(query, SqlUtils.field(TBL_ORDER_ITEMS, COL_TRADE_ITEM_PRICE),
            SqlUtils.field(TBL_ORDER_ITEMS, COL_TRADE_CURRENCY), SqlUtils.field(TBL_ORDERS,
                COL_DATES_START_DATE), SqlUtils.constant(currency));

    query.addExpr(priceExch, priceAlias)
        .addExpr(vatExch, vatAlias)
        .addOrder(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS));

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(TBL_ORDER_ITEMS, idsQty, "not found");
    }

    ResponseObject response = new ResponseObject();

    for (SimpleRow row : data) {
      Long item = row.getLong(COL_INCOME_ITEM);
      String article = row.getValue(COL_ITEM_ARTICLE);

      SqlInsert insert = new SqlInsert(TBL_SALE_ITEMS)
          .addConstant(COL_SALE, saleId)
          .addConstant(COL_ITEM_ARTICLE, article)
          .addConstant(COL_ITEM, item);

      Boolean vatPerc = row.getBoolean(COL_TRADE_VAT_PERC);
      Double vat;
      if (BeeUtils.isTrue(vatPerc)) {
        insert.addConstant(COL_TRADE_VAT_PERC, vatPerc);
        vat = row.getDouble(COL_TRADE_VAT);
      } else {
        vat = row.getDouble(vatAlias);
      }

      if (BeeUtils.nonZero(vat)) {
        insert.addConstant(COL_TRADE_VAT, vat);
      }

      Boolean vatPlus = row.getBoolean(COL_TRADE_VAT_PLUS);

      if (BeeUtils.isTrue(vatPlus)) {
        insert.addConstant(COL_TRADE_VAT_PLUS, vatPlus);
      }

      double saleQuantity = BeeUtils.unbox(idsQty.get(row.getLong(sys.getIdName(TBL_ORDER_ITEMS))));
      double price = BeeUtils.unbox(row.getDouble(priceAlias));
      double discount = BeeUtils.unbox(row.getDouble(COL_TRADE_DISCOUNT));
      if (discount > 0) {
        insert.addConstant(COL_TRADE_DISCOUNT, discount);
      }

      insert.addConstant(COL_TRADE_ITEM_QUANTITY, saleQuantity);

      if (price > 0) {
        insert.addConstant(COL_TRADE_ITEM_PRICE, price);
      }

      ResponseObject insResponse = qs.insertDataWithResponse(insert);
      if (insResponse.hasErrors()) {
        response.addMessagesFrom(insResponse);
        break;
      } else {
        SqlInsert si = new SqlInsert(VIEW_ORDER_CHILD_INVOICES)
            .addConstant(COL_SALE_ITEM, insResponse.getResponseAsLong())
            .addConstant(COL_ORDER_ITEM, row.getLong(sys.getIdName(TBL_ORDER_ITEMS)))
            .addConstant("OrderSale", saleId);

        qs.insertData(si);

        SqlUpdate update = new SqlUpdate(TBL_ORDER_ITEMS)
            .addConstant(COL_RESERVED_REMAINDER, BeeConst.DOUBLE_ZERO)
            .setWhere(sys.idEquals(TBL_ORDER_ITEMS, row.getLong(sys.getIdName(TBL_ORDER_ITEMS))));

        qs.updateData(update);
      }
    }
    return response;
  }

  private ResponseObject autoReservation() {

    SqlSelect slc = new SqlSelect()
        .addFields(TBL_ORDER_ITEMS, COL_ITEM)
        .addFields(TBL_ORDERS, COL_WAREHOUSE)
        .addFrom(TBL_ORDER_ITEMS)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER))
        .setWhere(SqlUtils.equals(TBL_ORDERS, COL_ORDERS_STATUS, OrdersStatus.APPROVED.ordinal()))
        .addGroup(TBL_ORDER_ITEMS, COL_ITEM)
        .addGroup(TBL_ORDERS, COL_WAREHOUSE);

    for (SimpleRow r : qs.getData(slc)) {
      Long itemId = r.getLong(COL_ITEM);
      Long warehouse = r.getLong(COL_WAREHOUSE);

      double freeRemainder = BeeUtils.unbox(getFreeRemainders(Collections.singletonList(itemId),
          null, warehouse).get(itemId));

      if (freeRemainder == 0) {
        continue;
      }

      SqlSelect select = new SqlSelect()
          .addFields(TBL_ORDER_ITEMS, COL_RESERVED_REMAINDER,
              COL_TRADE_ITEM_QUANTITY, COL_ORDER, sys.getIdName(TBL_ORDER_ITEMS))
          .addFields(TBL_ORDERS, COL_DATES_START_DATE)
          .addFrom(TBL_ORDER_ITEMS)
          .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER))
          .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ITEM, itemId),
              SqlUtils.equals(TBL_ORDERS, COL_ORDERS_STATUS, OrdersStatus.APPROVED.ordinal()),
              SqlUtils.equals(TBL_ORDERS, COL_WAREHOUSE, warehouse)))
          .addOrder(TBL_ORDERS, COL_DATES_START_DATE);

      for (SimpleRow row : qs.getData(select)) {
        double quantity = BeeUtils.unbox(row.getDouble(COL_TRADE_ITEM_QUANTITY));
        double reserved = BeeUtils.unbox(row.getDouble(COL_RESERVED_REMAINDER));

        Map<Long, Double> completedMap = getCompletedInvoices(row.getLong(COL_ORDER));
        double completed =
            BeeUtils.unbox(completedMap.get(row.getLong(sys.getIdName(TBL_ORDER_ITEMS))));
        double uncompleted = quantity - completed;
        double value;

        if (uncompleted != 0 && reserved != uncompleted && freeRemainder > 0) {
          if (reserved + freeRemainder <= uncompleted) {
            value = reserved + freeRemainder;
          } else {
            value = uncompleted;
          }
          freeRemainder -= value - reserved;

          if (value > 0) {
            SqlUpdate update = new SqlUpdate(TBL_ORDER_ITEMS)
                .addConstant(COL_RESERVED_REMAINDER, value).setWhere(sys.idEquals(TBL_ORDER_ITEMS,
                    row.getLong(sys.getIdName(TBL_ORDER_ITEMS))));

            qs.updateData(update);
          }
        }
      }
    }

    return ResponseObject.emptyResponse();
  }

  private static boolean areRowsEquals(BeeRow row1, BeeRow row2, List<BeeColumn> cols) {
    List<String> colNames = Lists.newArrayList(COL_TA_ITEM, COL_TRADE_ITEM_PRICE,
        COL_TRADE_DISCOUNT, COL_INVISIBLE_DISCOUNT, COL_TRADE_VAT, COL_TRADE_VAT_PERC,
        COL_TRADE_VAT_PLUS, COL_TRADE_SUPPLIER, COL_UNPACKING);

    for (String col : colNames) {
      if (!Objects.equals(row1.getString(DataUtils.getColumnIndex(col, cols)),
          row2.getString(DataUtils.getColumnIndex(col, cols)))) {
        return false;
      }
    }

    return true;
  }

  private ResponseObject checkForComplects(RequestInfo reqInfo) {
    Long saleId = reqInfo.getParameterLong(COL_SALE);
    if (!DataUtils.isId(saleId)) {
      return ResponseObject.emptyResponse();
    }

    SqlSelect slcSaleItems = new SqlSelect()
        .addFields(VIEW_ORDER_CHILD_INVOICES, COL_SALE_ITEM)
        .addField(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY, "SaleItemQuantity")
        .addFields(TBL_ORDER_ITEMS, COL_TRADE_ITEM_PARENT, COL_TRADE_ITEM_QUANTITY)
        .addFrom(VIEW_ORDER_CHILD_INVOICES)
        .addFromLeft(TBL_SALE_ITEMS, sys.joinTables(TBL_SALE_ITEMS, VIEW_ORDER_CHILD_INVOICES,
            COL_SALE_ITEM))
        .addFromLeft(TBL_ORDER_ITEMS, sys.joinTables(TBL_ORDER_ITEMS, VIEW_ORDER_CHILD_INVOICES,
            COL_ORDER_ITEM))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_SALE_ITEMS, COL_SALE, saleId), SqlUtils.notNull(
            TBL_ORDER_ITEMS, COL_TRADE_ITEM_PARENT)));

    SimpleRowSet saleItemsSet = qs.getData(slcSaleItems);
    BeeRowSet saleItems = qs.getViewData(VIEW_SALE_ITEMS, Filter.equals(COL_SALE, saleId));

    Multimap<Long, BeeRow> itemMap = HashMultimap.create();
    for (BeeRow row : saleItems) {
      itemMap.put(row.getId(), row);
    }

    fillOrderDiscounts(itemMap, saleItems.getColumns());

    if (saleItemsSet.getNumberOfRows() == 0) {
      return ResponseObject.response(saleItems);
    }

    Long[] parentsColumn = saleItemsSet.getLongColumn(COL_TRADE_ITEM_PARENT);

    Set<Long> complectIds = new HashSet<>();
    Collections.addAll(complectIds, parentsColumn);

    BeeRowSet complects = qs.getViewData(VIEW_ORDER_ITEMS, Filter.idIn(complectIds));
    List<BeeColumn> columns = saleItems.getColumns();
    List<BeeColumn> complectColumns = complects.getColumns();

    int complectQtyIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, complectColumns);

    for (BeeRow complect : complects) {
      BeeRow row = DataUtils.createEmptyRow(saleItems.getNumberOfColumns());
      Double complectQty = complect.getDouble(complectQtyIdx);
      double componentQty = 0;
      double saleItemQty = 0;
      Set<Long> removeIds = new HashSet<>();

      for (SimpleRow sr : saleItemsSet) {
        if (Objects.equals(sr.getLong(COL_TRADE_ITEM_PARENT), complect.getId())) {
          componentQty = BeeUtils.unbox(sr.getDouble(COL_TRADE_ITEM_QUANTITY));
          saleItemQty = BeeUtils.unbox(sr.getDouble("SaleItemQuantity"));
          removeIds.add(sr.getLong(COL_SALE_ITEM));
        }
      }

      row.setValue(DataUtils.getColumnIndex(ALS_ITEM_NAME, columns),
          complect.getValue(DataUtils.getColumnIndex(ALS_ITEM_NAME, complectColumns)));
      row.setValue(DataUtils.getColumnIndex(COL_ITEM_ARTICLE, columns),
          complect.getValue(DataUtils.getColumnIndex(COL_ITEM_ARTICLE, complectColumns)));
      row.setValue(DataUtils.getColumnIndex(ALS_UNIT_NAME, columns),
          complect.getValue(DataUtils.getColumnIndex(ALS_UNIT_NAME, complectColumns)));
      row.setValue(DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, columns), saleItemQty
          / (componentQty / complectQty));
      row.setValue(DataUtils.getColumnIndex(COL_ITEM_PRICE, columns),
          complect.getValue(DataUtils.getColumnIndex(COL_ITEM_PRICE, complectColumns)));
      row.setValue(DataUtils.getColumnIndex(COL_ITEM_LINK, columns),
          complect.getValue(DataUtils.getColumnIndex(COL_ITEM_LINK, complectColumns)));

      Double vat = complect.getDouble(DataUtils.getColumnIndex(COL_ITEM_VAT, complectColumns));
      if (BeeUtils.isDouble(vat)) {
        vat = vat * (saleItemQty / (componentQty / complectQty)) / complectQty;
        row.setValue(DataUtils.getColumnIndex(COL_ITEM_VAT, columns), vat);
      }

      double discount = 0.0;
      for (BeeRow saleItem : DataUtils.filterRows(saleItems.getRows(), removeIds)) {
        discount += BeeUtils.unbox(saleItem.getPropertyDouble(PRP_DISCOUNT_VALUE));
        saleItems.removeRowById(saleItem.getId());
      }
      row.setProperty(PRP_DISCOUNT_VALUE, discount);

      saleItems.addRow(row);
    }

    return ResponseObject.response(saleItems);
  }

  private void clearReservations() {

    Double hours = prm.getDouble(PRM_CLEAR_RESERVATIONS_TIME);

    SqlSelect select = new SqlSelect()
        .addFields(TBL_ORDERS, COL_DATES_START_DATE, sys.getIdName(TBL_ORDERS))
        .addFrom(TBL_ORDERS)
        .addFromLeft(TBL_ORDER_ITEMS,
            sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDERS, COL_ORDERS_STATUS,
            OrdersStatus.APPROVED.ordinal()), SqlUtils.positive(TBL_ORDER_ITEMS,
            COL_RESERVED_REMAINDER)));

    SimpleRowSet rowSet = qs.getData(select);

    for (SimpleRow row : rowSet) {
      DateTime orderTime = row.getDateTime(COL_DATES_START_DATE);

      if (TimeUtils.nowMillis().getTime() > orderTime.getTime() + hours
          * TimeUtils.MILLIS_PER_HOUR) {

        SqlUpdate update =
            new SqlUpdate(TBL_ORDER_ITEMS)
                .addConstant(COL_RESERVED_REMAINDER, null)
                .setWhere(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ORDER, row.getLong(sys
                    .getIdName(TBL_ORDERS))));

        qs.updateData(update);
      }
    }
  }

  private void exportReservations() {
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    SqlSelect select =
        new SqlSelect()
            .addFields(ALS_RESERVATIONS, COL_ITEM_EXTERNAL_CODE, COL_WAREHOUSE_CODE)
            .addSum(ALS_RESERVATIONS, COL_RESERVED_REMAINDER, ALS_TOTAL_AMOUNT)
            .addGroup(ALS_RESERVATIONS, COL_ITEM_EXTERNAL_CODE)
            .addGroup(ALS_RESERVATIONS, COL_WAREHOUSE_CODE)
            .addFrom(
                new SqlSelect()
                    .setUnionAllMode(true)
                    .addFields(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)
                    .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
                    .addFields(TBL_ORDER_ITEMS, COL_RESERVED_REMAINDER)
                    .addFrom(TBL_ORDER_ITEMS)
                    .addFromLeft(TBL_ITEMS,
                        sys.joinTables(TBL_ITEMS, TBL_ORDER_ITEMS, COL_ITEM))
                    .addFromLeft(TBL_ORDERS,
                        sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER))
                    .addFromLeft(TBL_WAREHOUSES,
                        sys.joinTables(TBL_WAREHOUSES, TBL_ORDERS, COL_WAREHOUSE))
                    .setWhere(SqlUtils.equals(TBL_ORDERS, COL_ORDERS_STATUS,
                        OrdersStatus.APPROVED.ordinal())).addUnion(
                    new SqlSelect()
                        .addFields(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)
                        .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
                        .addField(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY,
                            COL_RESERVED_REMAINDER)
                        .addFrom(VIEW_ORDER_CHILD_INVOICES)
                        .addFromLeft(TBL_ORDER_ITEMS, sys.joinTables(TBL_ORDER_ITEMS,
                            VIEW_ORDER_CHILD_INVOICES, COL_ORDER_ITEM))
                        .addFromLeft(TBL_ORDERS,
                            sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER))
                        .addFromLeft(TBL_WAREHOUSES,
                            sys.joinTables(TBL_WAREHOUSES, TBL_ORDERS, COL_WAREHOUSE))
                        .addFromLeft(TBL_SALE_ITEMS, sys.joinTables(TBL_SALE_ITEMS,
                            VIEW_ORDER_CHILD_INVOICES, COL_SALE_ITEM))
                        .addFromLeft(TBL_SALES, sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE))
                        .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_SALE_ITEMS, COL_ITEM))
                        .setWhere(SqlUtils.isNull(TBL_SALES, COL_TRADE_EXPORTED))),
                ALS_RESERVATIONS);

    SimpleRowSet rs = qs.getData(select);

    if (rs.getNumberOfRows() > 0) {
      try {
        ButentWS.connect(remoteAddress, remoteLogin, remotePassword).importItemReservation(rs);
      } catch (BeeException e) {
        logger.error(e);
        sys.eventEnd(sys.eventStart(PRM_EXPORT_ERP_RESERVATIONS_TIME), "ERROR", e.getMessage());
      }
    }
  }

  private ResponseObject getCreditInfo(RequestInfo reqInfo) {
    Long orderId = BeeUtils.toLongOrNull(reqInfo.getParameter(VIEW_ORDERS));

    if (!DataUtils.isId(orderId)) {
      return ResponseObject.emptyResponse();
    }

    SqlSelect select = new SqlSelect()
        .addFields(VIEW_ORDERS, COL_COMPANY)
        .addFrom(VIEW_ORDERS)
        .setWhere(SqlUtils.equals(VIEW_ORDERS, sys.getIdName(VIEW_ORDERS), orderId));

    Long companyId = qs.getLong(select);

    if (DataUtils.isId(companyId)) {
      ResponseObject response = trd.getCreditInfo(companyId);

      if (!response.hasErrors()) {
        return response;
      }
    }
    return ResponseObject.emptyResponse();
  }

  private void getERPItems() {
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);
    SimpleRowSet rs;

    try {
      rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword).getGoods("e");

    } catch (BeeException e) {
      logger.error(e);
      sys.eventEnd(sys.eventStart(PRM_IMPORT_ERP_ITEMS_TIME), "ERROR", e.getMessage());
      return;
    }

    if (rs.getNumberOfColumns() > 0) {

      List<String> externalCodes = new ArrayList<>();

      externalCodes.addAll(Arrays.asList(qs.getColumn(new SqlSelect()
          .addFields(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)
          .addFrom(TBL_ITEMS))));

      Map<String, Long> currencies = new HashMap<>();

      for (SimpleRow row : qs.getData(new SqlSelect()
          .addFields(TBL_CURRENCIES, COL_CURRENCY_NAME)
          .addField(TBL_CURRENCIES, sys.getIdName(TBL_CURRENCIES), COL_CURRENCY)
          .addFrom(TBL_CURRENCIES))) {

        currencies.put(row.getValue(COL_CURRENCY_NAME), row.getLong(COL_CURRENCY));
      }

      Map<String, Long> typesGroups = new HashMap<>();

      for (SimpleRow row : qs.getData(new SqlSelect()
          .addFields(TBL_ITEM_CATEGORY_TREE, COL_CATEGORY_NAME)
          .addField(TBL_ITEM_CATEGORY_TREE, sys.getIdName(TBL_ITEM_CATEGORY_TREE), COL_CATEGORY)
          .addFrom(TBL_ITEM_CATEGORY_TREE))) {

        typesGroups.put(row.getValue(COL_CATEGORY_NAME), row.getLong(COL_CATEGORY));
      }

      List<String> articles = new ArrayList<>();
      articles.addAll(Arrays.asList(qs.getColumn(new SqlSelect()
          .addFields(TBL_ITEMS, COL_ITEM_ARTICLE)
          .addFrom(TBL_ITEMS))));

      Map<String, Long> units = new HashMap<>();

      for (SimpleRow row : qs.getData(new SqlSelect()
          .addFields(TBL_UNITS, COL_UNIT_NAME)
          .addField(TBL_UNITS, sys.getIdName(TBL_UNITS), COL_UNIT)
          .addFrom(TBL_UNITS))) {

        units.put(row.getValue(COL_UNIT_NAME), row.getLong(COL_UNIT));
      }

      boolean updatePrc = BeeUtils.unbox(prm.getBoolean(PRM_UPDATE_ITEMS_PRICES));

      for (SimpleRow row : rs) {

        String type = row.getValue("TIPAS");
        String group = row.getValue("GRUPE");
        String article = row.getValue("ARTIKULAS");
        String unit = row.getValue("MATO_VIEN");
        String exCode = row.getValue("PREKE");

        Map<String, String> currenciesMap = new HashMap<>();
        currenciesMap.put("PARD_VAL", row.getValue("PARD_VAL"));
        currenciesMap.put("SAV_VAL", row.getValue("SAV_VAL"));
        currenciesMap.put("VAL_1", row.getValue("VAL_1"));
        currenciesMap.put("VAL_2", row.getValue("VAL_2"));
        currenciesMap.put("VAL_3", row.getValue("VAL_3"));
        currenciesMap.put("VAL_4", row.getValue("VAL_4"));
        currenciesMap.put("VAL_5", row.getValue("VAL_5"));
        currenciesMap.put("VAL_6", row.getValue("VAL_6"));
        currenciesMap.put("VAL_7", row.getValue("VAL_7"));
        currenciesMap.put("VAL_8", row.getValue("VAL_8"));
        currenciesMap.put("VAL_9", row.getValue("VAL_9"));
        currenciesMap.put("VAL_10", row.getValue("VAL_10"));

        if (!articles.contains(article) && !externalCodes.contains(exCode)) {

          if (!typesGroups.containsKey(type)) {
            typesGroups.put(type, qs.insertData(new
                SqlInsert(TBL_ITEM_CATEGORY_TREE).addConstant(COL_CATEGORY_NAME, type)));
          }

          if (!typesGroups.containsKey(group)) {
            typesGroups.put(group, qs.insertData(new
                SqlInsert(TBL_ITEM_CATEGORY_TREE).addConstant(COL_CATEGORY_NAME, group)));
          }

          if (!units.containsKey(unit)) {
            units.put(unit, qs.insertData(new SqlInsert(TBL_UNITS)
                .addConstant(COL_UNIT_NAME, unit)));
          }

          for (String value : currenciesMap.values()) {
            if (!currencies.containsKey(value) && !BeeUtils.isEmpty(value)) {
              currencies.put(value, qs.insertData(new SqlInsert(TBL_CURRENCIES)
                  .addConstant(COL_CURRENCY_NAME, value)));
            }
          }

          ResponseObject response = qs.insertDataWithResponse(new SqlInsert(TBL_ITEMS)
              .addConstant(COL_ITEM_NAME, row.getValue("PAVAD"))
              .addConstant(COL_ITEM_EXTERNAL_CODE, exCode)
              .addConstant(COL_UNIT, units.get(unit))
              .addNotEmpty(COL_ITEM_ARTICLE, article)
              .addConstant(COL_ITEM_PRICE, row.getDouble("PARD_KAINA"))
              .addConstant(COL_ITEM_COST, row.getDouble("SAVIKAINA"))
              .addConstant(COL_ITEM_PRICE_1, row.getDouble("KAINA_1"))
              .addConstant(COL_ITEM_PRICE_2, row.getDouble("KAINA_2"))
              .addConstant(COL_ITEM_PRICE_3, row.getDouble("KAINA_3"))
              .addConstant(COL_ITEM_PRICE_4, row.getDouble("KAINA_4"))
              .addConstant(COL_ITEM_PRICE_5, row.getDouble("KAINA_5"))
              .addConstant(COL_ITEM_PRICE_6, row.getDouble("KAINA_6"))
              .addConstant(COL_ITEM_PRICE_7, row.getDouble("KAINA_7"))
              .addConstant(COL_ITEM_PRICE_8, row.getDouble("KAINA_8"))
              .addConstant(COL_ITEM_PRICE_9, row.getDouble("KAINA_9"))
              .addConstant(COL_ITEM_PRICE_10, row.getDouble("KAINA_10"))
              .addConstant(COL_ITEM_GROUP, typesGroups.get(group))
              .addConstant(COL_ITEM_TYPE, typesGroups.get(type))
              .addConstant(COL_ITEM_CURRENCY, currencies.get(currenciesMap.get("PARD_VAL")))
              .addConstant(COL_ITEM_COST_CURRENCY, currencies.get(currenciesMap.get("SAV_VAL")))
              .addConstant(COL_ITEM_CURRENCY_1, currencies.get(currenciesMap.get("VAL_1")))
              .addConstant(COL_ITEM_CURRENCY_2, currencies.get(currenciesMap.get("VAL_2")))
              .addConstant(COL_ITEM_CURRENCY_3, currencies.get(currenciesMap.get("VAL_3")))
              .addConstant(COL_ITEM_CURRENCY_4, currencies.get(currenciesMap.get("VAL_4")))
              .addConstant(COL_ITEM_CURRENCY_5, currencies.get(currenciesMap.get("VAL_5")))
              .addConstant(COL_ITEM_CURRENCY_6, currencies.get(currenciesMap.get("VAL_6")))
              .addConstant(COL_ITEM_CURRENCY_7, currencies.get(currenciesMap.get("VAL_7")))
              .addConstant(COL_ITEM_CURRENCY_8, currencies.get(currenciesMap.get("VAL_8")))
              .addConstant(COL_ITEM_CURRENCY_9, currencies.get(currenciesMap.get("VAL_9")))
              .addConstant(COL_ITEM_CURRENCY_10, currencies.get(currenciesMap.get("VAL_10")))
              .addConstant(COL_TRADE_VAT, true)
              .addConstant(COL_TRADE_VAT_PERC, prm.getInteger(PRM_VAT_PERCENT)));

          if (!response.hasErrors()) {
            externalCodes.add(exCode);
            articles.add(article);
          }
        } else if (updatePrc) {
          SqlUpdate update = new SqlUpdate(TBL_ITEMS)
              .addConstant(COL_ITEM_PRICE, row.getDouble("PARD_KAINA"))
              .addConstant(COL_ITEM_COST, row.getDouble("SAVIKAINA"))
              .addConstant(COL_ITEM_PRICE_1, row.getDouble("KAINA_1"))
              .addConstant(COL_ITEM_PRICE_2, row.getDouble("KAINA_2"))
              .addConstant(COL_ITEM_PRICE_3, row.getDouble("KAINA_3"))
              .addConstant(COL_ITEM_PRICE_4, row.getDouble("KAINA_4"))
              .addConstant(COL_ITEM_PRICE_5, row.getDouble("KAINA_5"))
              .addConstant(COL_ITEM_PRICE_6, row.getDouble("KAINA_6"))
              .addConstant(COL_ITEM_PRICE_7, row.getDouble("KAINA_7"))
              .addConstant(COL_ITEM_PRICE_8, row.getDouble("KAINA_8"))
              .addConstant(COL_ITEM_PRICE_9, row.getDouble("KAINA_9"))
              .addConstant(COL_ITEM_PRICE_10, row.getDouble("KAINA_10"))
              .addConstant(COL_ITEM_CURRENCY, currencies.get(currenciesMap.get("PARD_VAL")))
              .addConstant(COL_ITEM_COST_CURRENCY, currencies.get(currenciesMap.get("SAV_VAL")))
              .addConstant(COL_ITEM_CURRENCY_1, currencies.get(currenciesMap.get("VAL_1")))
              .addConstant(COL_ITEM_CURRENCY_2, currencies.get(currenciesMap.get("VAL_2")))
              .addConstant(COL_ITEM_CURRENCY_3, currencies.get(currenciesMap.get("VAL_3")))
              .addConstant(COL_ITEM_CURRENCY_4, currencies.get(currenciesMap.get("VAL_4")))
              .addConstant(COL_ITEM_CURRENCY_5, currencies.get(currenciesMap.get("VAL_5")))
              .addConstant(COL_ITEM_CURRENCY_6, currencies.get(currenciesMap.get("VAL_6")))
              .addConstant(COL_ITEM_CURRENCY_7, currencies.get(currenciesMap.get("VAL_7")))
              .addConstant(COL_ITEM_CURRENCY_8, currencies.get(currenciesMap.get("VAL_8")))
              .addConstant(COL_ITEM_CURRENCY_9, currencies.get(currenciesMap.get("VAL_9")))
              .addConstant(COL_ITEM_CURRENCY_10, currencies.get(currenciesMap.get("VAL_10")))
              .setWhere(SqlUtils.equals(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE, exCode));

          qs.updateData(update);
        }
      }
    }
  }

  private void getERPStocks(Set<Long> ids) {
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);
    SimpleRowSet rs = null;
    SqlSelect select = null;
    SimpleRowSet srs = null;

    if (!BeeUtils.isEmpty(ids)) {
      select = new SqlSelect()
          .setDistinctMode(true)
          .addFields(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)
          .addField(TBL_ITEMS, sys.getIdName(TBL_ITEMS), COL_ITEM)
          .addFrom(TBL_SALES)
          .addFromInner(TBL_SALE_ITEMS, sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE))
          .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_SALE_ITEMS, COL_ITEM))
          .setWhere(sys.idInList(TBL_SALES, ids));
    }

    try {

      if (!BeeUtils.isEmpty(ids)) {
        srs = qs.getData(select);
        String[] codeList = srs.getColumn(COL_ITEM_EXTERNAL_CODE);
        for (String code : codeList) {
          if (rs == null) {
            rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword).getStocks(code);
          } else {
            rs.append(ButentWS.connect(remoteAddress, remoteLogin, remotePassword).getStocks(code));
          }
        }
      } else {
        rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword).getStocks("");
      }

    } catch (BeeException e) {
      logger.error(e);
      sys.eventEnd(sys.eventStart(PRM_IMPORT_ERP_STOCKS_TIME), "ERROR", e.getMessage());
      return;
    }

    if (rs.getNumberOfRows() > 0) {
      Map<String, Long> externalCodes = new HashMap<>();

      for (SimpleRow row : qs.getData(new SqlSelect()
          .addFields(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)
          .addField(TBL_ITEMS, sys.getIdName(TBL_ITEMS), COL_ITEM)
          .addFrom(TBL_ITEMS))) {

        externalCodes.put(row.getValue(COL_ITEM_EXTERNAL_CODE), row.getLong(COL_ITEM));
      }

      Map<String, Long> warehouses = new HashMap<>();

      for (SimpleRow row : qs.getData(new SqlSelect()
          .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
          .addField(TBL_WAREHOUSES, sys.getIdName(TBL_WAREHOUSES), COL_WAREHOUSE)
          .addFrom(TBL_WAREHOUSES))) {

        warehouses.put(row.getValue(COL_WAREHOUSE_CODE), row.getLong(COL_WAREHOUSE));
      }

      String tmp = SqlUtils.temporaryName();
      qs.updateData(new SqlCreate(tmp)
          .addLong(COL_ITEM, true)
          .addLong(COL_WAREHOUSE, true)
          .addDecimal(COL_WAREHOUSE_REMAINDER, 12, 3, false)
          .addLong(COL_ITEM_REMAINDER_ID, false));

      SqlInsert insert = new SqlInsert(tmp)
          .addFields(COL_ITEM, COL_WAREHOUSE, COL_WAREHOUSE_REMAINDER);
      int tot = 0;

      for (SimpleRow row : rs) {
        String exCode = row.getValue("PREKE");
        String warehouse = row.getValue("SANDELIS");
        String stock = row.getValue("LIKUTIS");

        if (externalCodes.containsKey(exCode) && warehouses.containsKey(warehouse)) {
          insert.addValues(externalCodes.get(exCode), warehouses.get(warehouse), stock);

          if (++tot % 1e4 == 0) {
            qs.insertData(insert);
            insert.resetValues();
          }
        }
      }

      if (tot % 1e4 > 0) {
        if (!insert.isEmpty()) {
          qs.insertData(insert);
        }
      }

      SqlUpdate updateTmp = new SqlUpdate(tmp)
          .addExpression(COL_ITEM_REMAINDER_ID,
              SqlUtils.field(VIEW_ITEM_REMAINDERS, sys.getIdName(VIEW_ITEM_REMAINDERS)))
          .setFrom(VIEW_ITEM_REMAINDERS, SqlUtils.joinUsing(VIEW_ITEM_REMAINDERS, tmp, COL_ITEM,
              COL_WAREHOUSE));

      qs.updateData(updateTmp);

      SqlUpdate updateRem =
          new SqlUpdate(VIEW_ITEM_REMAINDERS)
              .addExpression(COL_WAREHOUSE_REMAINDER,
                  SqlUtils.field(tmp, COL_WAREHOUSE_REMAINDER))
              .setFrom(tmp, sys.joinTables(VIEW_ITEM_REMAINDERS, tmp, COL_ITEM_REMAINDER_ID))
              .setWhere(SqlUtils.or(SqlUtils.notEqual(VIEW_ITEM_REMAINDERS, COL_WAREHOUSE_REMAINDER,
                  SqlUtils.field(tmp, COL_WAREHOUSE_REMAINDER)), SqlUtils.isNull(
                  VIEW_ITEM_REMAINDERS, COL_WAREHOUSE_REMAINDER)));

      qs.updateData(updateRem);

      SqlUpdate updRem = new SqlUpdate(VIEW_ITEM_REMAINDERS)
          .addConstant(COL_WAREHOUSE_REMAINDER, null);

      IsCondition whereCondition;
      if (BeeUtils.isEmpty(ids)) {
        whereCondition =
            SqlUtils.not(SqlUtils.in(VIEW_ITEM_REMAINDERS, sys.getIdName(VIEW_ITEM_REMAINDERS), new
                SqlSelect().addFields(tmp, COL_ITEM_REMAINDER_ID)
                .addFrom(tmp)));
      } else {
        whereCondition =
            SqlUtils.and(SqlUtils.not(SqlUtils.in(VIEW_ITEM_REMAINDERS, sys
                    .getIdName(VIEW_ITEM_REMAINDERS), new SqlSelect().addFields(tmp,
                COL_ITEM_REMAINDER_ID).addFrom(tmp))),
                SqlUtils.inList(VIEW_ITEM_REMAINDERS, COL_ITEM,
                    Lists.newArrayList(srs.getLongColumn(COL_ITEM))));
      }
      updRem.setWhere(whereCondition);
      qs.updateData(updRem);

      qs.loadData(VIEW_ITEM_REMAINDERS, new SqlSelect().setLimit(10000).addFields(
          tmp, COL_ITEM, COL_WAREHOUSE, COL_WAREHOUSE_REMAINDER)
          .addFrom(tmp).setWhere(SqlUtils.isNull(tmp,
              COL_ITEM_REMAINDER_ID)).addOrder(tmp, COL_ITEM, COL_WAREHOUSE));

      qs.sqlDropTemp(tmp);

    }

    if (BeeUtils.isEmpty(ids)) {
      autoReservation();
    }
  }

  private Set<Long> getOrderItems(Long targetId, String source, String column) {
    if (DataUtils.isId(targetId)) {
      return qs.getLongSet(new SqlSelect()
          .addFields(source, COL_ITEM)
          .addFrom(source)
          .setWhere(SqlUtils.equals(source, column, targetId)));
    } else {
      return BeeConst.EMPTY_IMMUTABLE_LONG_SET;
    }
  }

  private ResponseObject getTemplateItems(RequestInfo reqInfo) {
    Long templateId = reqInfo.getParameterLong(COL_TEMPLATE);
    if (!DataUtils.isId(templateId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TEMPLATE);
    }

    Long orderId = reqInfo.getParameterLong(COL_ORDER);

    List<BeeRowSet> result = new ArrayList<>();

    Set<Long> ordItems = getOrderItems(orderId, TBL_ORDER_ITEMS, COL_ORDER);
    Filter filter = getTemplateChildrenFilter(templateId, ordItems);

    BeeRowSet templateItems = qs.getViewData(VIEW_ORDER_TMPL_ITEMS, filter);
    if (!DataUtils.isEmpty(templateItems)) {
      result.add(templateItems);
    }

    if (result.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result).setSize(result.size());
    }
  }

  private static Filter getTemplateChildrenFilter(Long templateId, Collection<Long> excludeItems) {
    if (BeeUtils.isEmpty(excludeItems)) {
      return Filter.equals(COL_TEMPLATE, templateId);
    } else {
      return Filter.and(Filter.equals(COL_TEMPLATE, templateId),
          Filter.exclude(COL_ITEM, excludeItems));
    }
  }

  private ResponseObject getComplectFreeRemainders(RequestInfo reqInfo) {
    BeeRowSet data = BeeRowSet.restore(reqInfo.getParameter(Service.VAR_DATA));
    BeeRowSet complects = BeeRowSet.restore(reqInfo.getParameter(COL_ITEM_COMPLECT));

    if (complects.isEmpty()) {
      return ResponseObject.parameterNotFound(SVC_GET_COMPLECT_FREE_REMAINDERS, COL_ITEM_COMPLECT);
    }

    int itemIdx = DataUtils.getColumnIndex(COL_ITEM, complects.getColumns());
    int qtyIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, complects.getColumns());
    int complQtyIdx = DataUtils.getColumnIndex(COL_COMPLETED_QTY, complects.getColumns());
    int parentIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_PARENT, complects.getColumns());
    int itemNameIdx = DataUtils.getColumnIndex(ALS_ITEM_NAME, complects.getColumns());
    int resIdx = DataUtils.getColumnIndex(COL_RESERVED_REMAINDER, complects.getColumns());

    Long order = complects.getLong(0, COL_ORDER);
    Set<Long> itemIds = new HashSet<>();

    BeeRowSet components = qs.getViewData(VIEW_ORDER_SALES, Filter.and(Filter.equals(COL_ORDER,
        order), Filter.any(COL_TRADE_ITEM_PARENT, complects.getRowIds())));

    for (BeeRow row : components) {
      itemIds.add(row.getLong(itemIdx));
    }

    Map<Long, Double> remainders = getFreeRemainders(itemIds, order, null);

    for (BeeRow row : data) {
      Long item = row.getLong(itemIdx);

      if (remainders.containsKey(item)) {
        Double freeRemainder = remainders.get(item);
        remainders.put(item, freeRemainder - row.getDouble(complQtyIdx)
            + BeeUtils.unbox(row.getDouble(resIdx)));
      }
    }

    BeeRowSet result = new BeeRowSet(VIEW_ORDER_SALES, complects.getColumns());

    for (BeeRow complect : complects) {
      Double complectQty = complect.getDouble(qtyIdx);
      Double completedQty = complect.getDouble(complQtyIdx);
      BeeRowSet tmp = new BeeRowSet(complects.getColumns());

      for (BeeRow component : components) {
        if (Objects.equals(complect.getId(), component.getLong(parentIdx))) {
          tmp.addRow(component);
        }
      }

      for (BeeRow row : tmp) {
        Double componentQty = row.getDouble(qtyIdx) / complectQty * completedQty;
        double remQty = BeeUtils.unbox(remainders.get(row.getLong(itemIdx)));
        double resRemainder = BeeUtils.unbox(row.getDouble(resIdx));

        if (remQty + resRemainder >= componentQty) {
          row.setValue(complQtyIdx, componentQty);
          result.addRow(row);

          if (componentQty > resRemainder) {
            remainders.put(row.getLong(itemIdx), remQty - componentQty + resRemainder);
          }
        } else {
          return ResponseObject.error(Localized.dictionary().ordEmptyFreeRemainder() + ": "
              + complect.getString(itemNameIdx) + " -> " + row.getString(itemNameIdx));
        }
      }
    }
    result.addRows(data.getRows());

    return ResponseObject.response(result);
  }

  private Map<Long, Double> getCompletedInvoices(Long order) {
    Map<Long, Double> complInvoices = new HashMap<>();

    SqlSelect select = new SqlSelect()
        .addSum(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFields(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS))
        .addFrom(VIEW_ORDER_CHILD_INVOICES)
        .addFromInner(TBL_ORDER_ITEMS, sys.joinTables(TBL_ORDER_ITEMS,
            VIEW_ORDER_CHILD_INVOICES, COL_ORDER_ITEM))
        .addFromInner(TBL_SALE_ITEMS,
            sys.joinTables(TBL_SALE_ITEMS, VIEW_ORDER_CHILD_INVOICES, COL_SALE_ITEM))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ORDER, order),
            SqlUtils.joinUsing(TBL_ORDER_ITEMS, TBL_SALE_ITEMS, COL_ITEM)))
        .addGroup(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS));

    SimpleRowSet rs = qs.getData(select);

    if (rs.getNumberOfRows() > 0) {
      for (SimpleRow row : rs) {
        complInvoices.put(row.getLong(sys.getIdName(TBL_ORDER_ITEMS)),
            row.getDouble(COL_TRADE_ITEM_QUANTITY));
      }
    }
    return complInvoices;
  }

  private ResponseObject getAllOrderItems(RequestInfo reqInfo) {
    Long order = reqInfo.getParameterLong(COL_ORDER);

    if (!DataUtils.isId(order)) {
      return ResponseObject.parameterNotFound(SVC_GET_ALL_ORDER_ITEMS, COL_ORDER);
    }

    BeeRowSet data = qs.getViewData(VIEW_ORDER_ITEMS, Filter.equals(COL_ORDER, order));
    Set<Long> parentIds = data.getDistinctLongs(DataUtils.getColumnIndex(COL_TRADE_ITEM_PARENT,
        data.getColumns()));
    parentIds.remove(null);

    for (Long id : parentIds) {
      data.removeRowById(id);
    }

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    }

    List<BeeColumn> cols = DataUtils.getColumns(data.getColumns(), Arrays.asList(ALS_ITEM_NAME,
        COL_ITEM_ARTICLE, COL_TRADE_ITEM_QUANTITY));
    BeeColumn lackCol = new BeeColumn(ValueType.NUMBER, Localized.dictionary().ordLack(), "Lack");
    cols.add(lackCol);

    BeeRowSet result = new BeeRowSet(cols);

    int itemNameIdx = DataUtils.getColumnIndex(ALS_ITEM_NAME, data.getColumns());
    int qtyIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, data.getColumns());
    int reservedIdx = DataUtils.getColumnIndex(COL_RESERVED_REMAINDER, data.getColumns());
    int articleIdx = DataUtils.getColumnIndex(COL_ITEM_ARTICLE, data.getColumns());

    List<Long> ids = DataUtils.getDistinct(data, COL_ITEM);

    for (Long id : ids) {
      List<BeeRow> rows =  DataUtils.filterRows(data, COL_ITEM, id);
      Double qty = BeeConst.DOUBLE_ZERO;
      Double reserved = BeeConst.DOUBLE_ZERO;
      Double free = rows.get(0).getPropertyDouble(PRP_FREE_REMAINDER);
      Double completed = BeeConst.DOUBLE_ZERO;

      for (BeeRow r : rows) {
        qty += r.getDouble(qtyIdx);
        reserved += BeeUtils.unbox(r.getDouble(reservedIdx));
        completed += r.getPropertyDouble(PRP_COMPLETED_INVOICES);
      }

      BeeRow row = result.addEmptyRow();
      Double lack = qty - completed - free - reserved;
      row.setId(id);
      row.setValue(0, rows.get(0).getString(itemNameIdx));
      row.setValue(1, rows.get(0).getString(articleIdx));
      row.setValue(2, qty);

      if (lack > 0) {
        row.setValue(3, lack);
      } else {
        row.setValue(3, 0);
      }
    }

    return ResponseObject.response(result);
  }

  private Map<Long, Double> getAllRemainders(Collection<Long> ids) {
    Map<Long, Double> reminders = new HashMap<>();
    Map<Long, Double> resRemainders = new HashMap<>();
    Map<Long, Double> invoices = new HashMap<>();
    Map<Long, Double> wrhRemainders = getWarehouseReminders(ids, null);

    if (!BeeUtils.isEmpty(ids)) {
      SqlSelect selectReminders = new SqlSelect()
          .addFields(TBL_ORDER_ITEMS, COL_ITEM)
          .addSum(TBL_ORDER_ITEMS, COL_RESERVED_REMAINDER)
          .addFrom(TBL_ORDER_ITEMS)
          .setWhere(SqlUtils.inList(TBL_ORDER_ITEMS, COL_ITEM, ids))
          .addGroup(TBL_ORDER_ITEMS, COL_ITEM);

      SqlSelect slcInvoices = new SqlSelect()
          .addFields(TBL_SALE_ITEMS, COL_ITEM)
          .addSum(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY)
          .addFrom(TBL_SALE_ITEMS)
          .addFromLeft(TBL_SALES, sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE))
          .setWhere(SqlUtils.and(SqlUtils.inList(TBL_SALE_ITEMS, COL_ITEM, ids), SqlUtils.isNull(
              TBL_SALES, COL_TRADE_EXPORTED)))
          .addGroup(TBL_SALE_ITEMS, COL_ITEM);

      for (SimpleRow row : qs.getData(slcInvoices)) {
        invoices.put(row.getLong(COL_ITEM), BeeUtils.unbox(row.getDouble(COL_TRADE_ITEM_QUANTITY)));
      }

      for (SimpleRow row : qs.getData(selectReminders)) {
        resRemainders.put(row.getLong(COL_ITEM),
            BeeUtils.unbox(row.getDouble(COL_RESERVED_REMAINDER)));
      }

      for (Long itemId : ids) {
        double wrhRemainder = BeeUtils.unbox(wrhRemainders.get(itemId));
        double remainder = BeeUtils.unbox(resRemainders.get(itemId));
        double invoice = BeeUtils.unbox(invoices.get(itemId));

        reminders.put(itemId, wrhRemainder - remainder - invoice);
      }
    }

    return reminders;
  }

  private Map<Long, Double> getFreeRemainders(Collection<Long> itemIds, Long order, Long whId) {
    Long warehouseId;

    if (whId == null) {
      SqlSelect query = new SqlSelect()
          .addFields(TBL_ORDERS, COL_WAREHOUSE)
          .addFrom(TBL_ORDERS)
          .setWhere(SqlUtils.equals(TBL_ORDERS, sys.getIdName(TBL_ORDERS), order));

      warehouseId = qs.getLong(query);
    } else {
      warehouseId = whId;
    }

    Map<Long, Double> totRemainders = new HashMap<>();

    if (warehouseId == null) {
      return getAllRemainders(itemIds);
    }

    for (Long itemId : itemIds) {
      SqlSelect qry = new SqlSelect()
          .addSum(TBL_ORDER_ITEMS, COL_RESERVED_REMAINDER)
          .addFrom(TBL_ORDERS)
          .addFromLeft(TBL_ORDER_ITEMS,
              SqlUtils.join(TBL_ORDER_ITEMS, COL_ORDER, TBL_ORDERS, sys.getIdName(TBL_ORDERS)))
          .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDERS, COL_WAREHOUSE, warehouseId),
              SqlUtils.equals(TBL_ORDERS, COL_ORDERS_STATUS, OrdersStatus.APPROVED.ordinal()),
              SqlUtils.equals(TBL_ORDER_ITEMS, COL_ITEM, itemId)))
          .addGroup(TBL_ORDER_ITEMS, COL_ITEM);

      Double totRes = qs.getDouble(qry);

      if (totRes == null) {
        totRes = BeeConst.DOUBLE_ZERO;
      }

      SqlSelect invoiceQry = new SqlSelect()
          .addSum(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY)
          .addFrom(VIEW_ORDER_CHILD_INVOICES)
          .addFromLeft(TBL_SALE_ITEMS, sys.joinTables(TBL_SALE_ITEMS, VIEW_ORDER_CHILD_INVOICES,
              COL_SALE_ITEM))
          .addFromLeft(TBL_SALES, sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE))
          .setWhere(SqlUtils.and(SqlUtils.equals(TBL_SALES, COL_TRADE_WAREHOUSE_FROM, warehouseId),
              SqlUtils.equals(TBL_SALE_ITEMS, COL_ITEM, itemId), SqlUtils.isNull(TBL_SALES,
                  COL_TRADE_EXPORTED)))
          .addGroup(TBL_SALE_ITEMS, COL_ITEM);

      Double totInvc = qs.getDouble(invoiceQry);

      if (totInvc == null) {
        totInvc = BeeConst.DOUBLE_ZERO;
      }

      SqlSelect q = new SqlSelect()
          .addFields(VIEW_ITEM_REMAINDERS, COL_WAREHOUSE_REMAINDER)
          .addFrom(VIEW_ITEM_REMAINDERS)
          .setWhere(SqlUtils.and(SqlUtils.equals(VIEW_ITEM_REMAINDERS, COL_ITEM, itemId),
              SqlUtils.equals(VIEW_ITEM_REMAINDERS, COL_WAREHOUSE, warehouseId), SqlUtils.notNull(
                  VIEW_ITEM_REMAINDERS, COL_WAREHOUSE_REMAINDER)));

      if (BeeUtils.isDouble(qs.getDouble(q))) {
        Double rem = qs.getDouble(q);
        totRemainders.put(itemId, rem - totRes - totInvc);
      } else {
        totRemainders.put(itemId, BeeConst.DOUBLE_ZERO);
      }
    }

    return totRemainders;
  }

  private Map<Long, Double> getWarehouseReminders(Collection<Long> ids, Long warehouse) {
    Map<Long, Double> result = new HashMap<>();

    SqlSelect selectWrhReminders = new SqlSelect();

    if (DataUtils.isId(warehouse)) {
      selectWrhReminders
          .addFields(VIEW_ITEM_REMAINDERS, COL_ITEM, COL_WAREHOUSE_REMAINDER)
          .addFrom(VIEW_ITEM_REMAINDERS)
          .setWhere(SqlUtils.and(SqlUtils.inList(VIEW_ITEM_REMAINDERS, COL_ITEM, ids),
              SqlUtils.equals(VIEW_ITEM_REMAINDERS, COL_WAREHOUSE, warehouse)));
    } else {
      selectWrhReminders
          .addFields(VIEW_ITEM_REMAINDERS, COL_ITEM)
          .addSum(VIEW_ITEM_REMAINDERS, COL_WAREHOUSE_REMAINDER)
          .addFrom(VIEW_ITEM_REMAINDERS)
          .setWhere(SqlUtils.inList(VIEW_ITEM_REMAINDERS, COL_ITEM, ids))
          .addGroup(VIEW_ITEM_REMAINDERS, COL_ITEM);
    }

    for (SimpleRow row : qs.getData(selectWrhReminders)) {
      result.put(row.getLong(COL_ITEM), BeeUtils.unbox(row.getDouble(COL_WAREHOUSE_REMAINDER)));
    }

    return result;
  }

  private ResponseObject insertComplects(RequestInfo reqInfo) {
    String[] list = Codec.beeDeserializeCollection(reqInfo.getParameter(Service.VAR_DATA));
    ResponseObject response = ResponseObject.emptyResponse();

    for (String arr : list) {
      BeeRowSet rowSet = BeeRowSet.restore(arr);
      if (rowSet != null) {
        response = insertComplect(rowSet);
      }
    }

    return response;
  }

  private ResponseObject insertComplect(BeeRowSet rowSet) {
    int parentIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_PARENT, rowSet.getColumns());
    ResponseObject response = deb.commitRow(rowSet, 0, BeeRow.class);
    long complectId = ((BeeRow) response.getResponse()).getId();

    if (!DataUtils.isId(complectId)) {
      return response;
    }

    for (int i = 1; i < rowSet.getNumberOfRows(); i++) {
      rowSet.getRow(i).setValue(parentIdx, complectId);
      response = deb.commitRow(rowSet, i, BeeRow.class);
    }

    return response;
  }

  private ResponseObject insertRows(BeeRowSet rowSet) {
    ResponseObject response = ResponseObject.emptyResponse();

    for (int i = 0; i < rowSet.getNumberOfRows(); i++) {
      response = deb.commitRow(rowSet, i, BeeRow.class);
    }

    return response;
  }

  private ResponseObject joinInvoices(RequestInfo reqInfo) {

    List<Long> ids = Codec.deserializeIdList(reqInfo.getParameter(Service.VAR_DATA));
    Long invoiceId = reqInfo.getParameterLong(COL_SALE);

    ResponseObject response;

    SqlUpdate update = new SqlUpdate(TBL_SALE_ITEMS)
        .addConstant(COL_SALE, invoiceId)
        .setWhere(SqlUtils.inList(TBL_SALE_ITEMS, COL_SALE, ids));
    qs.updateDataWithResponse(update);

    update = new SqlUpdate(VIEW_ORDER_CHILD_INVOICES)
        .addConstant("OrderSale", invoiceId)
        .setWhere(SqlUtils.inList(VIEW_ORDER_CHILD_INVOICES, "OrderSale", ids));
    qs.updateDataWithResponse(update);

    SqlDelete delete = new SqlDelete(TBL_SALES)
        .setWhere(sys.idInList(TBL_SALES, ids));

    response = qs.updateDataWithResponse(delete);

    return response;
  }

  private static BeeRowSet prepareOrderItems(BeeRowSet data, BeeRowSet orderItems) {
    int qtyIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, data.getColumns());
    int parentIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_PARENT, data.getColumns());
    BeeRowSet update = new BeeRowSet(VIEW_ORDER_ITEMS, data.getColumns());

    for (BeeRow row : data) {
      boolean equals = false;

      for (BeeRow orderItem : orderItems) {
        Long parent = orderItem.getLong(parentIdx);
        if (areRowsEquals(row, orderItem, data.getColumns()) && !BeeUtils.isPositive(parent)) {
          orderItem.setValue(qtyIdx, orderItem.getDouble(qtyIdx) + row.getDouble(qtyIdx));
          update.addRow(orderItem);
          equals = true;
          break;
        }
      }

      if (!equals) {
        update.addRow(row);
      }
    }

    return update;
  }

  private static void fillOrderDiscounts(Multimap<Long, BeeRow> itemMap, List<BeeColumn> cols) {
    Totalizer total = new Totalizer(cols);

    if (itemMap.isEmpty()) {
      return;
    }

    for (BeeRow row : itemMap.values()) {
      Double discount = 0.0;

      if (!BeeUtils.isPositive(row.getPropertyInteger(ClassifierConstants.PROP_ITEM_COMPONENT))) {
        discount = BeeUtils.unbox(total.getDiscount(row));
      } else {
        Long complectId = row.getId();

        for (BeeRow component : itemMap.get(complectId)) {
          discount += BeeUtils.unbox(total.getDiscount(component));
        }
      }

      if (BeeUtils.isPositive(discount)) {
        row.setProperty(PRP_DISCOUNT_VALUE, discount);
      }
    }
  }

  private ResponseObject filterComponents(RequestInfo reqInfo) {
    Map<String, String> complects = Codec.deserializeHashMap(reqInfo.getParameter(
        COL_ITEM_COMPLECT));
    Long warehouse = reqInfo.getParameterLong(COL_WAREHOUSE);
    Long order = reqInfo.getParameterLong(COL_ORDER);
    String source = reqInfo.getParameter(COL_SOURCE);

    if (!DataUtils.isId(order)) {
      return ResponseObject.parameterNotFound(SVC_ADD_COMPLECTS_TO_ORDER, COL_ORDER);
    }

    BeeRowSet items = new BeeRowSet(VIEW_ITEMS, sys.getView(VIEW_ITEMS).getRowSetColumns());
    BeeRowSet tmp;

    for (String id : complects.keySet()) {

      tmp = qs.getViewData(VIEW_ITEMS, Filter.in(sys.getIdName(TBL_ITEMS),
          VIEW_ITEM_COMPONENTS, COL_ITEM_COMPONENT, Filter.equals(COL_ITEM, id)));

      for (BeeRow row : tmp) {
        Double itemQuantity = qs.getDouble(new SqlSelect()
            .addFields(VIEW_ITEM_COMPONENTS, COL_TRADE_ITEM_QUANTITY)
            .addFrom(VIEW_ITEM_COMPONENTS)
            .setWhere(SqlUtils.equals(VIEW_ITEM_COMPONENTS, COL_ITEM, id, COL_ITEM_COMPONENT,
                row.getId())));

        row.setProperty(PRP_QUANTITY, itemQuantity * BeeUtils.toDouble(complects.get(id)));
        row.setProperty(COL_TRADE_ITEM_PARENT, id);

        items.addRow(row);
      }
    }

    if (Objects.equals(source, COL_TEMPLATE)) {
      return getTmplItems(items);
    } else {
      return getItems(items, warehouse);
    }
  }

  private static void filterOrderSales(BeeRowSet rowSet, List<BeeColumn> cols) {
    Map<Long, Pair<Double, Double>> quantities = new HashMap<>();
    List<Long> removeIds = new ArrayList<>();
    if (rowSet.isEmpty()) {
      return;
    }

    int parentIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_PARENT, cols);
    int completedIdx = DataUtils.getColumnIndex(PRP_COMPLETED_INVOICES, cols);
    int qtyIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, cols);

    for (BeeRow row : rowSet) {
      if (DataUtils.isId(row.getLong(parentIdx))) {
        if (quantities.containsKey(row.getLong(parentIdx))) {
          quantities.put(row.getLong(parentIdx), Pair.of(quantities.get(row
              .getLong(parentIdx)).getA() + row.getDouble(qtyIdx), quantities.get(row
              .getLong(parentIdx)).getB() + BeeUtils.nvl(row.getDouble(completedIdx),
              BeeConst.DOUBLE_ZERO)));
        } else {
          quantities.put(row.getLong(parentIdx), Pair.of(row.getDouble(qtyIdx),
              BeeUtils.nvl(row.getDouble(completedIdx), BeeConst.DOUBLE_ZERO)));
        }
      }
    }

    for (BeeRow row : rowSet) {
      if (BeeUtils.isPositive(row.getPropertyInteger(PROP_ITEM_COMPONENT))) {
        if (quantities.containsKey(row.getId())) {
          Pair<Double, Double> pair = quantities.get(row.getId());
          if (Objects.equals(pair.getA(), pair.getB())) {
            removeIds.add(row.getId());
          }
        }
      } else if (Objects.equals(row.getDouble(qtyIdx), row.getDouble(completedIdx))
          || BeeUtils.isPositive(row.getLong(parentIdx))) {
        removeIds.add(row.getId());
      }
    }

    for (Long id : removeIds) {
      rowSet.removeRowById(id);
    }
  }

  private ResponseObject fillReservedRemainders(RequestInfo reqInfo) {
    Long orderId = reqInfo.getParameterLong(COL_ORDER);
    Long warehouseId = reqInfo.getParameterLong(COL_WAREHOUSE);

    if (!DataUtils.isId(orderId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_ORDER);
    }
    if (!DataUtils.isId(warehouseId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_WAREHOUSE);
    }

    SqlSelect itemsQry = new SqlSelect()
        .addField(VIEW_ORDER_ITEMS, sys.getIdName(VIEW_ORDER_ITEMS), COL_ORDER_ITEM)
        .addFields(VIEW_ORDER_ITEMS, COL_ITEM, COL_TRADE_ITEM_QUANTITY)
        .addFrom(VIEW_ORDER_ITEMS)
        .setWhere(SqlUtils.equals(VIEW_ORDER_ITEMS, COL_ORDER, orderId));

    SimpleRowSet srs = qs.getData(itemsQry);
    Map<Long, Double> rem =
        getFreeRemainders(Arrays.asList(srs.getLongColumn(COL_ITEM)), null, warehouseId);

    for (SimpleRow sr : srs) {
      Long item = sr.getLong(COL_ITEM);
      Double resRemainder;
      Double qty = sr.getDouble(COL_TRADE_ITEM_QUANTITY);
      Double free = rem.get(item);

      if (qty <= free) {
        resRemainder = qty;
        rem.put(item, free - qty);
      } else {
        resRemainder = free;
        rem.put(item, BeeConst.DOUBLE_ZERO);
      }

      SqlUpdate update = new SqlUpdate(VIEW_ORDER_ITEMS)
          .addConstant(COL_RESERVED_REMAINDER, resRemainder)
          .setWhere(sys.idEquals(VIEW_ORDER_ITEMS, sr.getLong(COL_ORDER_ITEM)));

      qs.updateData(update);
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject updateOrderItems(RequestInfo reqInfo) {
    Long order = reqInfo.getParameterLong(COL_ORDER);
    BeeRowSet data = BeeRowSet.restore(reqInfo.getParameter(Service.VAR_DATA));
    BeeRowSet complects = null;

    if (reqInfo.hasParameter(COL_ITEM_COMPLECT)) {
      complects = BeeRowSet.restore(reqInfo.getParameter(COL_ITEM_COMPLECT));
    }

    if (!DataUtils.isId(order)) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_ORDER_ITEMS, COL_ORDER);
    }
    if (data.getNumberOfRows() == 0) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_ORDER_ITEMS, Service.VAR_DATA);
    }

    List<String> colNames = Lists.newArrayList(COL_ORDER, COL_TA_ITEM, COL_TRADE_ITEM_QUANTITY,
        COL_TRADE_ITEM_PRICE, COL_TRADE_DISCOUNT, COL_INVISIBLE_DISCOUNT, COL_RESERVED_REMAINDER,
        COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_TRADE_VAT_PLUS, COL_TRADE_SUPPLIER, COL_UNPACKING,
        COL_TRADE_ITEM_PARENT);

    ResponseObject response = null;
    Filter filter = Filter.equals(COL_ORDER, order);
    BeeRowSet allOrderItems = qs.getViewData(VIEW_ORDER_ITEMS, filter, null, colNames);

    int qtyIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, data.getColumns());
    int parentIdx = DataUtils.getColumnIndex(COL_TRADE_ITEM_PARENT, data.getColumns());
    int itemIdx = DataUtils.getColumnIndex(COL_ITEM, data.getColumns());

    Map<Long, List<BeeRow>> complectsMap = new HashMap<>();
    BeeRowSet orderItems = new BeeRowSet(data.getColumns());

    Set<Long> parentIds = allOrderItems.getDistinctLongs(DataUtils.getColumnIndex(
        COL_TRADE_ITEM_PARENT, allOrderItems.getColumns()));
    parentIds.remove(null);

    for (BeeRow item : allOrderItems) {
      if (parentIds.contains(item.getId())) {
        List<BeeRow> complectList = new ArrayList<>();
        complectList.add(item);
        complectList.addAll(DataUtils.filterRows(allOrderItems, COL_TA_PARENT, item.getId()));
        complectsMap.put(item.getLong(itemIdx), complectList);
      } else if (!BeeUtils.isPositive(item.getLong(parentIdx))) {
        orderItems.addRow(item);
      }
    }

    if (complects != null) {
      BeeRowSet items = new BeeRowSet(data.getColumns());
      BeeRowSet allComponents = new BeeRowSet(data.getColumns());

      for (BeeRow row : data) {
        if (row.getLong(parentIdx) == null) {
          items.addRow(row);
        } else {
          allComponents.addRow(row);
        }
      }

      for (BeeRow complect : complects) {
        Long complectItemId = complect.getLong(itemIdx);
        Map<Long, Double> quantities = new HashMap<>();
        BeeRowSet components = new BeeRowSet(VIEW_ORDER_ITEMS, data.getColumns());
        components.addRow(complect);
        components.addRows(DataUtils.filterRows(allComponents, COL_TA_PARENT,
            complect.getLong(itemIdx)));

        if (complectsMap.containsKey(complectItemId)) {
          boolean equals = false;
          for (BeeRow component : components) {
            equals = false;

            for (BeeRow orderItem : complectsMap.get(complectItemId)) {
              if (areRowsEquals(component, orderItem, data.getColumns())) {
                quantities.put(orderItem.getId(), orderItem.getDouble(qtyIdx)
                    + component.getDouble(qtyIdx));
                equals = true;
                break;
              }
            }

            if (!equals) {
              response = insertComplect(components);
              break;
            }
          }

          if (equals) {
            BeeRowSet update = new BeeRowSet(VIEW_ORDER_ITEMS, data.getColumns());

            for (BeeRow orderItem : complectsMap.get(complectItemId)) {
              orderItem.setValue(qtyIdx, quantities.get(orderItem.getId()));
              update.addRow(orderItem);
            }
            response = insertRows(update);
          }
        } else {
          response = insertComplect(components);
        }
      }

      if (items.getNumberOfRows() > 0) {
        response = insertRows(prepareOrderItems(items, orderItems));
      }

    } else {
      response = insertRows(prepareOrderItems(data, orderItems));
    }

    return response;
  }

  private void setOrderFreeRemainders(Multimap<Long, BeeRow> rowMap, List<BeeColumn> cols,
      String viewName) {

    if (BeeUtils.isPositive(rowMap.size())) {

      List<BeeRow> allRows = new ArrayList<>();
      allRows.addAll(rowMap.values());

      int itemIndex = DataUtils.getColumnIndex(COL_ITEM, cols);
      int qtyIndex = DataUtils.getColumnIndex(COL_TRADE_ITEM_QUANTITY, cols);
      int resIndex = DataUtils.getColumnIndex(COL_RESERVED_REMAINDER, cols);
      int ordIndex = DataUtils.getColumnIndex(COL_ORDER, cols);
      List<Long> itemIds = DataUtils.getDistinct(allRows, itemIndex);

      Long order = allRows.get(0).getLong(ordIndex);
      BeeRowSet complects = new BeeRowSet();

      Map<Long, Double> freeRemainders = getFreeRemainders(itemIds, order, null);
      Map<Long, Double> compInvoices = getCompletedInvoices(order);

      Totalizer totalizer = new Totalizer(cols);

      for (BeeRow row : allRows) {
        row.setProperty(PRP_FREE_REMAINDER, BeeUtils.toString(freeRemainders.get(row
            .getLong(itemIndex))));

        if (!Objects.equals(VIEW_ORDER_SALES_FILTERED, viewName)) {
          Long key = row.getId();
          if (BeeUtils.isPositive(compInvoices.get(key))) {
            row.setProperty(PRP_COMPLETED_INVOICES, compInvoices.get(key));
          } else {
            row.setProperty(PRP_COMPLETED_INVOICES, BeeConst.DOUBLE_ZERO);
          }
        }

        double total = BeeUtils.unbox(totalizer.getTotal(row));
        double vat = BeeUtils.unbox(totalizer.getVat(row));

        row.setProperty(PRP_AMOUNT_WO_VAT, total - vat);

        if (Objects.equals(VIEW_ORDER_SALES_FILTERED, viewName)) {
          row.setProperty(PRP_TOTAL_PRICE, total / row.getDouble(qtyIndex));
        }

        if (BeeUtils.isPositive(row.getPropertyInteger(PROP_ITEM_COMPONENT))) {
          complects.addRow(row);
        }
      }

      if (!complects.isEmpty()) {
        for (BeeRow complect : complects) {
          List<BeeRow> components = new ArrayList<>();
          components.addAll(rowMap.get(complect.getId()));

          if (!components.isEmpty()) {
            BeeRow row = components.get(0);

            complect.setProperty(PRP_COMPLETED_INVOICES,
                BeeUtils.unbox(compInvoices.get(row.getId())) / (row.getDouble(qtyIndex)
                    / complect.getDouble(qtyIndex)));

            for (BeeRow component : components) {
              double qty = component.getDouble(qtyIndex);
              double free = BeeUtils.unbox(component.getPropertyDouble(PRP_FREE_REMAINDER));
              double resRemainder = BeeUtils.unbox(component.getDouble(resIndex));
              double completed =
                  BeeUtils.unbox(component.getPropertyDouble(PRP_COMPLETED_INVOICES));

              if (qty - resRemainder - completed > free) {
                complect.setProperty(PRP_EMPTY_REMAINDER, 1);
                break;
              }
            }
          }
        }
      }
    }
  }
}