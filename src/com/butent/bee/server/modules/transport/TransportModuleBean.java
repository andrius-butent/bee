package com.butent.bee.server.modules.transport;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.concurrency.ConcurrencyBean;
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
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.news.ExtendedUsageQueryProvider;
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
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
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
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.ModificationMessage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletResponse;

@Stateless
@LocalBean
public class TransportModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(TransportModuleBean.class);

  @EJB
  DataEditorBean deb;
  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  ParamHolderBean prm;
  @EJB
  TradeModuleBean trd;
  @EJB
  NewsBean news;
  @EJB
  TransportReportsBean rep;
  @EJB
  MailModuleBean mail;
  @EJB
  ConcurrencyBean cb;

  private static IsExpression getAssessmentTurnoverExpression(SqlSelect query, String source,
      String defDateSource, String defDateAlias, Long currency, boolean woVat) {

    IsExpression amountExpr = SqlUtils.field(source, COL_AMOUNT);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(source, amountExpr);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(source, amountExpr);
    }
    if (DataUtils.isId(currency)) {
      return ExchangeUtils.exchangeFieldTo(query, amountExpr, SqlUtils.field(source, COL_CURRENCY),
          SqlUtils.nvl(SqlUtils.field(source, COL_DATE),
              SqlUtils.field(defDateSource, defDateAlias)),
          SqlUtils.constant(currency));

    } else {
      return ExchangeUtils.exchangeField(query, amountExpr, SqlUtils.field(source, COL_CURRENCY),
          SqlUtils.nvl(SqlUtils.field(source, COL_DATE),
              SqlUtils.field(defDateSource, defDateAlias)));
    }
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    List<SearchResult> vehiclesResult = qs.getSearchResults(VIEW_VEHICLES,
        Filter.anyContains(Sets.newHashSet(COL_NUMBER, COL_PARENT_MODEL_NAME, COL_MODEL_NAME,
            COL_OWNER_NAME), query));

    List<SearchResult> orderCargoResult = qs.getSearchResults(VIEW_ORDER_CARGO,
        Filter.anyContains(Sets.newHashSet(COL_CARGO_DESCRIPTION,
            COL_NUMBER, ALS_CARGO_CMR_NUMBER, COL_CARGO_NOTES, COL_CARGO_DIRECTIONS,
            ALS_LOADING_NUMBER, ALS_LOADING_CONTACT, ALS_LOADING_COMPANY, ALS_LOADING_ADDRESS,
            ALS_LOADING_POST_INDEX, ALS_LOADING_CITY_NAME, ALS_LOADING_COUNTRY_NAME,
            ALS_LOADING_COUNTRY_CODE, ALS_UNLOADING_NUMBER, ALS_UNLOADING_CONTACT,
            ALS_UNLOADING_COMPANY, ALS_UNLOADING_ADDRESS, ALS_UNLOADING_POST_INDEX,
            ALS_UNLOADING_CITY_NAME, ALS_UNLOADING_COUNTRY_NAME, ALS_UNLOADING_COUNTRY_CODE),
            query));

    result.addAll(vehiclesResult);
    result.addAll(orderCargoResult);

    if (usr.isModuleVisible(ModuleAndSub.of(Module.TRANSPORT, SubModule.LOGISTICS))) {
      result.addAll(qs.getSearchResults(VIEW_ASSESSMENTS,
          Filter.compareId(BeeUtils.toLong(query))));
    }

    return result;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (BeeUtils.same(svc, SVC_GET_BEFORE)) {
      response = getTripBeforeData(BeeUtils.toLong(reqInfo.getParameter(COL_VEHICLE)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DATE)));

    } else if (BeeUtils.same(svc, SVC_GET_UNASSIGNED_CARGOS)) {
      response = getUnassignedCargos(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_ROUTE)) {
      String tmp = rep.getTripRoutes(new SqlSelect()
          .addExpr(SqlUtils.constant(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP))), COL_TRIP));

      response = ResponseObject.response(qs.getValue(new SqlSelect()
          .addFields(tmp, COL_TRIP_ROUTE)
          .addFrom(tmp)));

      qs.sqlDropTemp(tmp);

    } else if (BeeUtils.same(svc, SVC_GENERATE_DAILY_COSTS)) {
      response = generateDailyCosts(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP)));

    } else if (BeeUtils.same(svc, SVC_GENERATE_ROUTE)) {
      response = generateTripRoute(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP)));

    } else if (BeeUtils.same(svc, SVC_GET_PROFIT)) {
      if (reqInfo.hasParameter(COL_TRIP)) {
        response = rep.getTripProfit(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP)));

      } else if (reqInfo.hasParameter(COL_CARGO)) {
        Long cargoId = BeeUtils.toLong(reqInfo.getParameter(COL_CARGO));

        response = rep.getCargoProfit(new SqlSelect().addConstant(cargoId, COL_CARGO));

      } else if (reqInfo.hasParameter(COL_ORDER)) {
        Long orderId = BeeUtils.toLong(reqInfo.getParameter(COL_ORDER));
        String cargo = VIEW_ORDER_CARGO;

        response = rep.getCargoProfit(new SqlSelect()
            .addField(cargo, sys.getIdName(cargo), COL_CARGO)
            .addFrom(cargo)
            .setWhere(SqlUtils.equals(cargo, COL_ORDER, orderId)));

      } else {
        response = ResponseObject.error("Profit of WHAT?");
      }

    } else if (BeeUtils.same(svc, SVC_GET_FX_DATA)) {
      response = getFxData();

    } else if (BeeUtils.same(svc, SVC_GET_SS_DATA)) {
      response = getVehicleTbData(svc, Filter.in(COL_VEHICLE_ID, VIEW_TRIPS, COL_VEHICLE),
          VehicleType.TRUCK, COL_SS_THEME);

    } else if (BeeUtils.same(svc, SVC_GET_DTB_DATA)) {
      response = getDtbData();

    } else if (BeeUtils.same(svc, SVC_GET_TRUCK_TB_DATA)) {
      response = getVehicleTbData(svc, Filter.notNull(COL_IS_TRUCK), VehicleType.TRUCK,
          COL_TRUCK_THEME);

    } else if (BeeUtils.same(svc, SVC_GET_TRAILER_TB_DATA)) {
      response = getVehicleTbData(svc, Filter.notNull(COL_IS_TRAILER), VehicleType.TRAILER,
          COL_TRAILER_THEME);

    } else if (BeeUtils.same(svc, SVC_GET_COLORS)) {
      response = getColors(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CARGO_USAGE)) {
      response = getCargoUsage(reqInfo.getParameter("ViewName"),
          Codec.beeDeserializeCollection(reqInfo.getParameter("IdList")));

    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_TOTALS)) {
      response = getAssessmentTotals(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ASSESSMENT)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY)),
          BeeUtils.toBoolean(reqInfo.getParameter("isPrimary")));

    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_QUANTITY_REPORT)) {
      response = getAssessmentQuantityReport(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_TURNOVER_REPORT)) {
      response = getAssessmentTurnoverReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CREATE_INVOICE_ITEMS)) {
      Long saleId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SALE));
      Long purchaseId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_PURCHASE));
      Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
      Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(Service.VAR_DATA));
      Long item = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ITEM));
      Double creditAmount = BeeUtils.toDoubleOrNull(reqInfo.getParameter(COL_TRADE_AMOUNT));

      if (DataUtils.isId(saleId)) {
        response = createInvoiceItems(saleId, currency, ids, item);
      } else if (BeeUtils.isPositive(creditAmount)) {
        response = createCreditInvoiceItems(purchaseId, currency, ids, item, creditAmount);

      } else if (BeeUtils.same(reqInfo.getParameter(Service.VAR_TABLE), TBL_TRIP_COSTS)) {
        response = createTripInvoiceItems(purchaseId, currency, ids, item);
      } else {
        response = createPurchaseInvoiceItems(purchaseId, currency, ids, item);
      }
    } else if (BeeUtils.same(svc, SVC_SEND_MESSAGE)) {
      response = sendMessage(reqInfo.getParameter(COL_DESCRIPTION),
          Codec.beeDeserializeCollection(reqInfo.getParameter(COL_MOBILE)));

    } else if (BeeUtils.same(svc, SVC_GET_CREDIT_INFO)) {
      response = getCreditInfo(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CARGO_TOTAL)) {
      response = getCargoTotal(BeeUtils.toLong(reqInfo.getParameter(COL_CARGO)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY)));

    } else if (BeeUtils.same(svc, SVC_TRIP_PROFIT_REPORT)) {
      response = rep.getTripProfitReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_VEHICLE_BUSY_DATES)) {
      response = getVehicleBusyDates(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_VEHICLE)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRAILER)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_FROM)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_TO)));

    } else if (BeeUtils.same(svc, SVC_GET_DRIVER_BUSY_DATES)) {
      response = getDriverBusyDates(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DRIVER)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_FROM)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_TO)));

    } else if (BeeUtils.same(svc, SVC_CREATE_USER)) {
      response = createUser(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_TRIP_INFO)) {
      response = rep.getTripInfo(reqInfo);

    } else {
      String msg = BeeUtils.joinWords("Transport service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Lists.newArrayList(
        BeeParameter.createBoolean(module, PRM_EXCLUDE_VAT),
        BeeParameter.createText(module, PRM_TRIP_PREFIX, true, null),
        BeeParameter.createRelation(module, PRM_INVOICE_PREFIX, true, TBL_SALES_SERIES,
            COL_SERIES_NAME),
        BeeParameter.createMap(module, PRM_MESSAGE_TEMPLATE, true, null),
        BeeParameter.createText(module, "SmsServiceAddress"),
        BeeParameter.createText(module, "SmsUserName"),
        BeeParameter.createText(module, "SmsPassword"),
        BeeParameter.createText(module, "SmsServiceId"),
        BeeParameter.createText(module, "SmsDisplayText"),
        BeeParameter.createRelation(module, PRM_SELF_SERVICE_ROLE, TBL_ROLES, COL_ROLE_NAME),
        BeeParameter.createRelation(module, PRM_CARGO_TYPE, true, TBL_CARGO_TYPES,
            COL_CARGO_TYPE_NAME),
        BeeParameter.createRelation(module, PRM_CARGO_SERVICE, TBL_SERVICES, COL_SERVICE_NAME),
        BeeParameter.createBoolean(module, PRM_BIND_EXPENSES_TO_INCOMES, false, true));
  }

  @Override
  public Module getModule() {
    return Module.TRANSPORT;
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
      public void calcAssessmentAmounts(ViewQueryEvent event) {
        if (event.isAfter(VIEW_CHILD_ASSESSMENTS) && event.hasData()) {
          for (String tbl : new String[] {TBL_CARGO_INCOMES, TBL_CARGO_EXPENSES}) {
            SqlSelect query = new SqlSelect()
                .addField(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS), COL_ASSESSMENT)
                .addFrom(TBL_ASSESSMENTS)
                .addFromInner(TBL_ORDER_CARGO,
                    sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO))
                .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
                .addFromInner(tbl, SqlUtils.joinUsing(TBL_ASSESSMENTS, tbl, COL_CARGO))
                .setWhere(sys.idInList(TBL_ASSESSMENTS, event.getRowset().getRowIds()))
                .addGroup(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS));

            IsExpression amountExpr = SqlUtils.field(tbl, COL_AMOUNT);

            if (BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT))) {
              amountExpr = TradeModuleBean.getWithoutVatExpression(tbl, amountExpr);
            } else {
              amountExpr = TradeModuleBean.getTotalExpression(tbl, amountExpr);
            }
            IsExpression xpr = ExchangeUtils.exchangeFieldTo(query, amountExpr,
                SqlUtils.field(tbl, COL_CURRENCY),
                SqlUtils.nvl(SqlUtils.field(tbl, COL_DATE), SqlUtils.field(TBL_ORDERS, COL_DATE)),
                SqlUtils.field(TBL_ORDER_CARGO, COL_CURRENCY));

            SimpleRowSet rs = qs.getData(query.addSum(xpr, VAR_TOTAL));

            for (BeeRow row : event.getRowset().getRows()) {
              row.setProperty((BeeUtils.same(tbl, TBL_CARGO_INCOMES)) ? VAR_INCOME : VAR_EXPENSE,
                  rs.getValueByKey(COL_ASSESSMENT, BeeUtils.toString(row.getId()), VAR_TOTAL));
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillCargoIncomes(ViewQueryEvent event) {
        if (event.isAfter(VIEW_ORDER_CARGO) && event.hasData()) {
          SimpleRowSet rs = qs.getData(rep.getCargoIncomeQuery(event.getQuery()
                  .resetFields().resetOrder().resetGroup()
                  .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
                  .addGroup(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO)), null,
              BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT))));

          for (BeeRow row : event.getRowset().getRows()) {
            String cargoId = BeeUtils.toString(row.getId());
            String cargoIncome = rs.getValueByKey(COL_CARGO, cargoId, "CargoIncome");
            String servicesIncome = rs.getValueByKey(COL_CARGO, cargoId, "ServicesIncome");

            row.setProperty(VAR_INCOME, BeeUtils.toString(BeeUtils.toDouble(cargoIncome)
                + BeeUtils.toDouble(servicesIncome)));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillFuelConsumptions(ViewQueryEvent event) {
        if (event.isAfter(TBL_TRIP_ROUTES) && event.hasData()) {
          BeeRowSet rowset = event.getRowset();
          int colIndex = DataUtils.getColumnIndex("Consumption", rowset.getColumns(), false);

          if (BeeConst.isUndef(colIndex)) {
            return;
          }
          SimpleRowSet rs = qs.getData(rep.getFuelConsumptionsQuery(event.getQuery()
              .resetFields().resetOrder().resetGroup()
              .addFields(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES))
              .addGroup(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES)), true));

          for (BeeRow row : rowset.getRows()) {
            row.setValue(colIndex, rs.getValueByKey(sys.getIdName(TBL_TRIP_ROUTES),
                BeeUtils.toString(row.getId()), "Quantity"));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillTripCargoIncomes(ViewQueryEvent event) {
        if (event.isAfter(VIEW_TRIP_CARGO) && event.hasData()) {
          BeeRowSet rowset = event.getRowset();
          int cargoIndex = rowset.getColumnIndex(COL_CARGO);

          if (BeeConst.isUndef(cargoIndex)) {
            return;
          }
          String crs = rep.getTripIncomes(event.getQuery().resetFields().resetOrder().resetGroup()
                  .addFields(VIEW_CARGO_TRIPS, COL_TRIP).addGroup(VIEW_CARGO_TRIPS, COL_TRIP),
              null, BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));

          SimpleRowSet rs = qs.getData(new SqlSelect().addAllFields(crs).addFrom(crs));
          qs.sqlDropTemp(crs);

          for (BeeRow row : rowset.getRows()) {
            row.setProperty(VAR_INCOME, rs.getValueByKey(COL_CARGO, row.getString(cargoIndex),
                "TripIncome"));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void getFileIcons(ViewQueryEvent event) {
        if (event.isAfter(VIEW_SHIPMENT_REQUEST_FILES)) {
          ExtensionIcons.setIcons(event.getRowset(), ALS_FILE_NAME, PROP_ICON);
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillTripNumber(ViewInsertEvent event) {
        if (event.isBefore(VIEW_TRIPS, VIEW_EXPEDITION_TRIPS)
            && BeeConst.isUndef(DataUtils.getColumnIndex(COL_TRIP_NO, event.getColumns()))) {

          String prefix = prm.getText(PRM_TRIP_PREFIX);

          if (!BeeUtils.isEmpty(prefix)) {
            event.addValue(new BeeColumn(COL_TRIP_NO),
                Value.getValue(qs.getNextNumber(TBL_TRIPS, COL_TRIP_NO, prefix, null)));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void informShipmentRequestCustomer(DataEvent.ViewUpdateEvent event) {
        if (event.isAfter(VIEW_SHIPMENT_REQUESTS)) {
          int col = DataUtils.getColumnIndex(COL_QUERY_STATUS, event.getColumns());

          if (!BeeConst.isUndef(col)) {
            Integer status = event.getRow().getInteger(col);
            TextConstant constant = null;

            if (ShipmentRequestStatus.CONFIRMED.is(status)) {
              constant = TextConstant.REQUEST_CONFIRMED_MAIL_CONTENT;
            } else if (ShipmentRequestStatus.LOST.is(status)) {
              constant = TextConstant.REQUEST_LOST_MAIL_CONTENT;
            }
            if (Objects.isNull(constant)) {
              return;
            }
            BeeRowSet info = qs.getViewData(VIEW_SHIPMENT_REQUESTS,
                Filter.compareId(event.getRow().getId()));

            String email = BeeUtils.notEmpty(info.getString(0, COL_PERSON + COL_EMAIL),
                info.getString(0, COL_QUERY_CUSTOMER_EMAIL));

            if (BeeUtils.isEmpty(email)) {
              return;
            }
            Long accountId = prm.getRelation(MailConstants.PRM_DEFAULT_ACCOUNT);

            if (!DataUtils.isId(accountId)) {
              logger.warning("Default mail account not found");
              return;
            }

            BeeRowSet data = qs.getViewData(VIEW_TEXT_CONSTANTS,
                Filter.equals(COL_TEXT_CONSTANT, status));

            String localizedContent = Localized.column(COL_TEXT_CONTENT,
                EnumUtils.getEnumByIndex(SupportedLocale.class, info.getInteger(0, COL_USER_LOCALE))
                    .getLanguage());
            String text;

            if (DataUtils.isEmpty(data)) {
              text = constant.getDefaultContent();
            } else if (BeeConst.isUndef(DataUtils.getColumnIndex(localizedContent,
                data.getColumns()))) {
              text = data.getString(0, COL_TEXT_CONTENT);
            } else {
              text = BeeUtils.notEmpty(data.getString(0, localizedContent),
                  data.getString(0, COL_TEXT_CONTENT));
            }
            cb.asynchronousCall(() -> mail.sendMail(accountId, email, null, text));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateAssessmentRelations(ViewInsertEvent event) {
        String tbl = sys.getViewSource(event.getTargetName());

        if (BeeUtils.inList(tbl, TBL_ASSESSMENTS, TBL_ASSESSMENT_FORWARDERS) && event.isAfter()) {
          String fld;
          String tblFrom;
          String joinFrom;

          if (BeeUtils.same(tbl, TBL_ASSESSMENTS)) {
            fld = COL_ORDER;
            tblFrom = TBL_ORDER_CARGO;
            joinFrom = COL_CARGO;
          } else {
            fld = COL_TRIP;
            tblFrom = TBL_CARGO_TRIPS;
            joinFrom = COL_CARGO_TRIP;
          }
          IsCondition clause = sys.idEquals(tbl, event.getRow().getId());

          qs.updateData(new SqlUpdate(tbl)
              .addExpression(fld, new SqlSelect()
                  .addFields(tblFrom, fld)
                  .addFrom(tbl)
                  .addFromInner(tblFrom, sys.joinTables(tblFrom, tbl, joinFrom))
                  .setWhere(clause))
              .setWhere(clause));
        }
      }
    });

    HeadlineProducer assessmentsHeadlineProducer =
        (feed, userId, rowSet, row, isNew, constants) -> {
          String caption = "";
          String pid = DataUtils.getString(rowSet, row, COL_ASSESSMENT);

          if (!BeeUtils.isEmpty(pid)) {
            caption = BeeUtils.joinWords(caption, constants.captionPid() + BeeConst.STRING_COLON,
                pid);
          }

          String id = BeeUtils.toString(row.getId());

          caption = BeeUtils.joinWords(caption, constants.captionId() + BeeConst.STRING_COLON, id);

          AssessmentStatus status =
              EnumUtils.getEnumByIndex(AssessmentStatus.class,
                  DataUtils.getInteger(rowSet, row, COL_STATUS));

          if (status != null) {
            caption = BeeUtils.joinWords(caption, status.getCaption(constants));
          }

          String notes = DataUtils.getString(rowSet, row, ALS_ORDER_NOTES);
          String customer = DataUtils.getString(rowSet, row, TransportConstants.ALS_CUSTOMER_NAME);

          caption = BeeUtils.joinWords(caption, notes, customer);

          return Headline.create(row.getId(), caption, isNew);
        };

    news.registerUsageQueryProvider(Feed.ORDER_CARGO, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.notNull(TBL_ORDER_CARGO, COL_ORDER));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_ORDER_CARGO, news.joinUsage(TBL_ORDER_CARGO));
      }
    });

    news.registerUsageQueryProvider(Feed.TRANSPORTATION_ORDERS_MY,
        new ExtendedUsageQueryProvider() {
          @Override
          protected List<IsCondition> getConditions(long userId) {
            return NewsHelper.buildConditions(SqlUtils.equals(TBL_ORDERS, COL_ORDER_MANAGER,
                userId));
          }

          @Override
          protected List<Pair<String, IsCondition>> getJoins() {
            return NewsHelper.buildJoin(TBL_ORDERS, news.joinUsage(TBL_ORDERS));
          }
        });

    news.registerUsageQueryProvider(Feed.TRIPS, new ExtendedUsageQueryProvider() {

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_TRIPS, news.joinUsage(TBL_TRIPS));
      }

      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION));
      }
    });

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_UNREGISTERED_MY,
        new ShipmentRequestsUsageQueryProvider());

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_MY,
        new ShipmentRequestsUsageQueryProvider());

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_UNREGISTERED_ALL,
        new ExtendedUsageQueryProvider() {
          @Override
          protected List<IsCondition> getConditions(long userId) {
            return NewsHelper.buildConditions(SqlUtils.isNull(TBL_SHIPMENT_REQUESTS,
                COL_COMPANY_PERSON));
          }

          @Override
          protected List<Pair<String, IsCondition>> getJoins() {
            return NewsHelper.buildJoin(TBL_SHIPMENT_REQUESTS,
                news.joinUsage(TBL_SHIPMENT_REQUESTS));
          }
        });

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_ALL, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.notNull(TBL_SHIPMENT_REQUESTS,
            COL_COMPANY_PERSON));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_SHIPMENT_REQUESTS, news.joinUsage(TBL_SHIPMENT_REQUESTS));
      }
    });

    news.registerHeadlineProducer(Feed.ASSESSMENT_REQUESTS_ALL, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_REQUESTS_ALL,
        new AssesmentRequestsUsageQueryProvider(false));

    news.registerHeadlineProducer(Feed.ASSESSMENT_REQUESTS_MY, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_REQUESTS_MY,
        new AssesmentRequestsUsageQueryProvider(true));

    news.registerHeadlineProducer(Feed.ASSESSMENT_ORDERS_ALL, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_ORDERS_ALL,
        new AssesmentRequestsUsageQueryProvider(false, true));

    news.registerHeadlineProducer(Feed.ASSESSMENT_ORDERS_MY, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_ORDERS_MY,
        new AssesmentRequestsUsageQueryProvider(true, true));

    news.registerUsageQueryProvider(Feed.CARGO_SALES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_CARGO_INCOMES, COL_SALE));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_CARGO_INCOMES, news.joinUsage(TBL_CARGO_INCOMES));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_CREDIT_SALES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_CARGO_INCOMES, COL_PURCHASE),
            SqlUtils.isNull(TBL_SALES, COL_SALE_PROFORMA),
            SqlUtils.notNull(TBL_CARGO_INCOMES, COL_SALE));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_CARGO_INCOMES, news.joinUsage(TBL_CARGO_INCOMES),
            TBL_SALES, sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_PURCHASES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_CARGO_EXPENSES, COL_PURCHASE));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_CARGO_EXPENSES, news.joinUsage(TBL_CARGO_EXPENSES));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_SALES, COL_SALE_PROFORMA));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_SALES, news.joinUsage(TBL_SALES),
            TBL_CARGO_INCOMES, sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_PROFORMA_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.notNull(TBL_SALES, COL_SALE_PROFORMA));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_SALES, news.joinUsage(TBL_SALES),
            TBL_CARGO_INCOMES, sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_CREDIT_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return null;
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_PURCHASES, news.joinUsage(TBL_PURCHASES),
            TBL_CARGO_INCOMES, sys.joinTables(TBL_PURCHASES, TBL_CARGO_INCOMES, COL_PURCHASE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_PURCHASE_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return null;
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_PURCHASES, news.joinUsage(TBL_PURCHASES),
            TBL_CARGO_EXPENSES, sys.joinTables(TBL_PURCHASES, TBL_CARGO_EXPENSES, COL_PURCHASE));
      }
    });

    news.registerUsageQueryProvider(Feed.ASSESSMENT_TRANSPORTATIONS, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return new SqlSelect()
            .addFields(TBL_TRIP_USAGE, COL_TRIP)
            .addMax(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_ACCESS)
            .addFrom(TBL_TRIP_USAGE)
            .addFromInner(TBL_TRIPS, SqlUtils.join(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
                TBL_TRIP_USAGE, COL_TRIP))
            .addFromLeft(
                TBL_ASSESSMENT_FORWARDERS,
                SqlUtils.join(TBL_ASSESSMENT_FORWARDERS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(
                TBL_CARGO_TRIPS,
                SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(TBL_ASSESSMENTS,
                SqlUtils.join(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
            .setWhere(SqlUtils.and(
                SqlUtils.isNull(TBL_ASSESSMENT_FORWARDERS, COL_TRIP),
                SqlUtils.notNull(TBL_ASSESSMENTS, COL_CARGO),
                SqlUtils.equals(TBL_TRIP_USAGE, NewsConstants.COL_UF_USER, userId),
                SqlUtils.notNull(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_ACCESS)))
            .addGroup(TBL_TRIP_USAGE, COL_TRIP);
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return new SqlSelect()
            .addFields(TBL_TRIP_USAGE, COL_TRIP)
            .addMax(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(TBL_TRIP_USAGE)
            .addFromInner(TBL_TRIPS, SqlUtils.join(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
                TBL_TRIP_USAGE, COL_TRIP))
            .addFromLeft(
                TBL_ASSESSMENT_FORWARDERS,
                SqlUtils.join(TBL_ASSESSMENT_FORWARDERS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(
                TBL_CARGO_TRIPS,
                SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(TBL_ASSESSMENTS,
                SqlUtils.join(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
            .setWhere(SqlUtils.and(
                SqlUtils.isNull(TBL_ASSESSMENT_FORWARDERS, COL_TRIP),
                SqlUtils.notNull(TBL_ASSESSMENTS, COL_CARGO),
                SqlUtils.notEqual(TBL_TRIP_USAGE, NewsConstants.COL_UF_USER, userId),
                SqlUtils.more(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_UPDATE,
                    NewsHelper.getStartTime(startDate))))
            .addGroup(TBL_TRIP_USAGE, COL_TRIP);
      }
    });
  }

  @Schedule(persistent = false)
  private void checkRequestStatus() {
    DateTime date = TimeUtils.startOfDay(1);

    SqlSelect query = new SqlSelect()
        .addField(TBL_SHIPMENT_REQUESTS, sys.getIdName(TBL_SHIPMENT_REQUESTS), "id")
        .addField(TBL_SHIPMENT_REQUESTS, sys.getVersionName(TBL_SHIPMENT_REQUESTS), "version")
        .addFields(TBL_SHIPMENT_REQUESTS, COL_QUERY_STATUS)
        .addFrom(TBL_SHIPMENT_REQUESTS)
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_SHIPMENT_REQUESTS, COL_CARGO))
        .setWhere(SqlUtils.and(SqlUtils.not(SqlUtils.inList(TBL_SHIPMENT_REQUESTS, COL_QUERY_STATUS,
            ShipmentRequestStatus.CONFIRMED, ShipmentRequestStatus.LOST)),
            SqlUtils.less(TBL_CARGO_PLACES, COL_PLACE_DATE, date)));

    SimpleRowSet expired = qs.getData(query.copyOf()
        .addFromInner(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_HANDLING, TBL_ORDER_CARGO, COL_CARGO_HANDLING))
        .addFromInner(TBL_CARGO_PLACES,
            sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .setUnionAllMode(false)
        .addUnion(query.copyOf()
            .addFromInner(TBL_CARGO_HANDLING,
                sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_HANDLING, COL_CARGO))
            .addFromInner(TBL_CARGO_PLACES,
                sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_LOADING_PLACE))));

    if (!DataUtils.isEmpty(expired)) {
      BeeColumn col = DataUtils.getColumn(COL_QUERY_STATUS,
          sys.getView(VIEW_SHIPMENT_REQUESTS).getRowSetColumns());

      for (SimpleRow row : expired) {
        BeeRowSet rs = DataUtils.getUpdated(VIEW_SHIPMENT_REQUESTS, row.getLong("id"),
            row.getLong("version"), col, row.getValue(COL_QUERY_STATUS),
            BeeUtils.toString(ShipmentRequestStatus.LOST.ordinal()));

        deb.commitRow(rs, RowInfo.class);
      }
      DataChangeEvent.fireRefresh(
          (event, locality) -> Endpoint.sendToAll(new ModificationMessage(event)),
          VIEW_SHIPMENT_REQUESTS);
    }
  }

  private ResponseObject createCreditInvoiceItems(Long purchaseId, Long currency, Set<Long> idList,
      Long mainItem, Double amount) {

    if (!DataUtils.isId(purchaseId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!BeeUtils.isPositive(amount)) {
      return ResponseObject.error("Wrong amount");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error("Empty ID list");
    }
    IsCondition wh = sys.idInList(TBL_CARGO_INCOMES, idList);

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_ORDERS, COL_ORDER_NO)
        .addFields(TBL_SALES_SERIES, COL_SERIES_NAME)
        .addFields(TBL_SALES, COL_TRADE_INVOICE_NO)
        .addFields(TBL_CARGO_INCOMES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addFrom(TBL_CARGO_INCOMES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addFromInner(TBL_SALES,
            sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE))
        .addFromLeft(TBL_SALES_SERIES,
            sys.joinTables(TBL_SALES_SERIES, TBL_SALES, COL_TRADE_SALE_SERIES))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .setWhere(SqlUtils.and(wh, SqlUtils.positive(TBL_CARGO_INCOMES, COL_AMOUNT)))
        .addGroup(TBL_ORDERS, COL_ORDER_NO)
        .addGroup(TBL_SALES_SERIES, COL_SERIES_NAME)
        .addGroup(TBL_SALES, COL_TRADE_INVOICE_NO)
        .addGroup(TBL_CARGO_INCOMES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC);

    if (DataUtils.isId(mainItem)) {
      ss.addConstant(mainItem, COL_ITEM);
    } else {
      ss.addFields(TBL_SERVICES, COL_ITEM)
          .addGroup(TBL_SERVICES, COL_ITEM);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT),
        SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY),
        SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
            SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE)), SqlUtils.constant(currency));

    SimpleRowSet rs = qs.getData(ss.addSum(xpr, COL_AMOUNT));
    ResponseObject response = new ResponseObject();

    double totalAmount = 0;

    for (Double n : rs.getDoubleColumn(COL_AMOUNT)) {
      totalAmount += BeeUtils.unbox(n);
    }
    for (SimpleRow row : rs) {
      String xml = XmlUtils.createString("CreditInfo",
          COL_ORDER_NO, row.getValue(COL_ORDER_NO),
          COL_TRADE_INVOICE_NO, BeeUtils.joinWords(row.getValue(COL_SERIES_NAME),
              row.getValue(COL_TRADE_INVOICE_NO)));

      SqlInsert insert = new SqlInsert(TBL_PURCHASE_ITEMS)
          .addConstant(COL_PURCHASE, purchaseId)
          .addConstant(COL_ITEM, row.getLong(COL_ITEM))
          .addConstant(COL_TRADE_ITEM_QUANTITY, 1)
          .addConstant(COL_TRADE_ITEM_PRICE,
              BeeUtils.round(amount * row.getDouble(COL_AMOUNT) / totalAmount, 2))
          .addConstant(COL_TRADE_VAT_PLUS, row.getBoolean(COL_TRADE_VAT_PLUS))
          .addConstant(COL_TRADE_VAT, row.getDouble(COL_TRADE_VAT))
          .addConstant(COL_TRADE_VAT_PERC, row.getBoolean(COL_TRADE_VAT_PERC))
          .addConstant(COL_TRADE_ITEM_NOTE, xml);

      qs.insertData(insert);
    }
    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_CARGO_INCOMES)
        .addConstant(COL_PURCHASE, purchaseId)
        .setWhere(wh)));
  }

  private ResponseObject createInvoiceItems(Long saleId, Long currency, Set<Long> idList,
      Long mainItem) {
    if (!DataUtils.isId(saleId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error("Empty ID list");
    }
    IsCondition wh = sys.idInList(TBL_CARGO_INCOMES, idList);

    String loadPlace = SqlUtils.uniqueName();
    String unloadPlace = SqlUtils.uniqueName();
    String loadCountry = SqlUtils.uniqueName();
    String unloadCountry = SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_ORDERS, COL_ORDER_NO, COL_ORDER_NOTES)
        .addFields(TBL_ORDER_CARGO, COL_NUMBER)
        .addFields(TBL_CARGO_INCOMES,
            COL_CARGO, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addField(loadPlace, COL_DATE, ALS_LOADING_DATE)
        .addField(unloadPlace, COL_DATE, ALS_UNLOADING_DATE)
        .addField(loadCountry, COL_COUNTRY_CODE, ALS_LOADING_COUNTRY_CODE)
        .addField(loadCountry, COL_COUNTRY_NAME, ALS_LOADING_COUNTRY_NAME)
        .addField(unloadCountry, COL_COUNTRY_CODE, ALS_UNLOADING_COUNTRY_CODE)
        .addField(unloadCountry, COL_COUNTRY_NAME, ALS_UNLOADING_COUNTRY_NAME)
        .addField(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS), COL_ASSESSMENT)
        .addField(DocumentConstants.TBL_DOCUMENTS, DocumentConstants.COL_DOCUMENT_NUMBER,
            ALS_CARGO_CMR_NUMBER)
        .addFrom(TBL_CARGO_INCOMES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_HANDLING, TBL_ORDER_CARGO, COL_CARGO_HANDLING))
        .addFromLeft(TBL_CARGO_PLACES, loadPlace,
            sys.joinTables(TBL_CARGO_PLACES, loadPlace, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .addFromLeft(TBL_COUNTRIES, loadCountry,
            sys.joinTables(TBL_COUNTRIES, loadCountry, loadPlace, COL_COUNTRY))
        .addFromLeft(TBL_CARGO_PLACES, unloadPlace,
            sys.joinTables(TBL_CARGO_PLACES, unloadPlace, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFromLeft(TBL_COUNTRIES, unloadCountry,
            sys.joinTables(TBL_COUNTRIES, unloadCountry, unloadPlace, COL_COUNTRY))
        .addFromLeft(TBL_ASSESSMENTS,
            sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO))
        .addFromLeft(DocumentConstants.TBL_DOCUMENTS,
            sys.joinTables(DocumentConstants.TBL_DOCUMENTS, TBL_ORDER_CARGO, COL_CARGO_CMR))
        .setWhere(wh);

    if (DataUtils.isId(mainItem)) {
      ss.addConstant(mainItem, COL_ITEM)
          .addConstant(true, COL_TRANSPORTATION);
    } else {
      ss.addFields(TBL_SERVICES, COL_ITEM, COL_TRANSPORTATION);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT),
        SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY),
        SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
            SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE)), SqlUtils.constant(currency));

    SimpleRowSet rs = qs.getData(ss.addExpr(xpr, COL_AMOUNT));
    ResponseObject response = new ResponseObject();

    Multimap<Long, String> drivers = HashMultimap.create();
    Multimap<Long, String> vehicles = HashMultimap.create();

    String vehicle = SqlUtils.uniqueName();
    String trailer = SqlUtils.uniqueName();

    IsCondition clause = SqlUtils.and(SqlUtils.inList(TBL_CARGO_TRIPS, COL_CARGO,
        Sets.newHashSet(rs.getLongColumn(COL_CARGO))));

    SimpleRowSet tripData = qs.getData(new SqlSelect()
        .addFields(TBL_CARGO_TRIPS, COL_CARGO)
        .addField(vehicle, COL_VEHICLE_NUMBER, vehicle)
        .addField(trailer, COL_VEHICLE_NUMBER, trailer)
        .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_CARGO_TRIPS, COL_TRIP))
        .addFromLeft(TBL_VEHICLES, vehicle,
            sys.joinTables(TBL_VEHICLES, vehicle, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLES, trailer,
            sys.joinTables(TBL_VEHICLES, trailer, TBL_TRIPS, COL_TRAILER))
        .addFromLeft(TBL_TRIP_DRIVERS, sys.joinTables(TBL_TRIPS, TBL_TRIP_DRIVERS, COL_TRIP))
        .addFromLeft(TBL_DRIVERS, sys.joinTables(TBL_DRIVERS, TBL_TRIP_DRIVERS, COL_DRIVER))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_DRIVERS, COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .setWhere(SqlUtils.and(clause, SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION))));

    for (SimpleRow trip : tripData) {
      Long cargo = trip.getLong(COL_CARGO);
      String txt = BeeUtils.joinWords(trip.getValue(COL_FIRST_NAME), trip.getValue(COL_LAST_NAME));

      if (!BeeUtils.isEmpty(txt)) {
        drivers.put(cargo, txt);
      }
      txt = BeeUtils.join("/", trip.getValue(vehicle), trip.getValue(trailer));

      if (!BeeUtils.isEmpty(txt)) {
        vehicles.put(cargo, txt);
      }
    }
    tripData = qs.getData(new SqlSelect()
        .addFields(TBL_CARGO_TRIPS, COL_CARGO)
        .addFields(TBL_TRIPS, COL_FORWARDER_VEHICLE, COL_FORWARDER_DRIVER)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_CARGO_TRIPS, COL_TRIP))
        .setWhere(SqlUtils.and(clause, SqlUtils.notNull(TBL_TRIPS, COL_EXPEDITION))));

    for (SimpleRow trip : tripData) {
      Long cargo = trip.getLong(COL_CARGO);
      String txt = trip.getValue(COL_FORWARDER_DRIVER);

      if (!BeeUtils.isEmpty(txt)) {
        drivers.put(cargo, txt);
      }
      txt = trip.getValue(COL_FORWARDER_VEHICLE);

      if (!BeeUtils.isEmpty(txt)) {
        vehicles.put(cargo, txt);
      }
    }
    String[] tableFields = new String[] {
        COL_ITEM, COL_TRADE_VAT_PLUS, COL_TRADE_VAT,
        COL_TRADE_VAT_PERC};

    String[] group = DataUtils.isId(mainItem) ? tableFields : rs.getColumnNames();
    Map<String, Multimap<String, String>> map = new HashMap<>();
    Map<String, Double> amounts = new HashMap<>();

    for (SimpleRow row : rs) {
      String key = "";

      for (String fld : group) {
        if (!fld.equals(COL_AMOUNT)) {
          key += fld + row.getValue(fld);
        }
      }
      if (!map.containsKey(key)) {
        map.put(key, TreeMultimap.create());
      }
      Multimap<String, String> valueMap = map.get(key);

      for (String fld : tableFields) {
        String value = row.getValue(fld);

        if (!BeeUtils.isEmpty(value)) {
          valueMap.put(fld, value);
        }
      }
      for (String fld : new String[] {
          COL_ORDER_NO, COL_ASSESSMENT, ALS_CARGO_CMR_NUMBER, COL_NUMBER}) {
        String value = row.getValue(fld);

        if (!BeeUtils.isEmpty(value)) {
          valueMap.put(fld, value);
        }
      }
      if (BeeUtils.unbox(row.getBoolean(COL_TRANSPORTATION))) {
        String value = BeeUtils.join("\n", row.getValue(COL_ORDER_NOTES),
            BeeUtils.join("-", row.getValue(ALS_LOADING_COUNTRY_CODE)
                    + " (" + row.getValue(ALS_LOADING_COUNTRY_NAME) + ")",
                row.getValue(ALS_UNLOADING_COUNTRY_CODE)
                    + " (" + row.getValue(ALS_UNLOADING_COUNTRY_NAME) + ")"));

        if (!BeeUtils.isEmpty(value)) {
          valueMap.put(COL_ORDER_NOTES, value);
        }
        for (String fld : new String[] {ALS_LOADING_DATE, ALS_UNLOADING_DATE}) {
          DateTime time = row.getDateTime(fld);

          if (time != null) {
            valueMap.put(fld, time.getDate().toString());
          }
        }
        Long cargo = row.getLong(COL_CARGO);

        if (drivers.containsKey(cargo)) {
          valueMap.putAll(COL_DRIVER, drivers.get(cargo));
        }
        if (vehicles.containsKey(cargo)) {
          valueMap.putAll(COL_VEHICLE, vehicles.get(cargo));
        }
      }
      amounts.put(key, BeeUtils.unbox(amounts.get(key))
          + BeeUtils.unbox(row.getDouble(COL_AMOUNT)));
    }
    for (Entry<String, Multimap<String, String>> entry : map.entrySet()) {
      Multimap<String, String> values = entry.getValue();

      SqlInsert insert = new SqlInsert(TBL_SALE_ITEMS)
          .addConstant(COL_SALE, saleId)
          .addConstant(COL_TRADE_ITEM_QUANTITY, 1)
          .addConstant(COL_TRADE_ITEM_PRICE, BeeUtils.round(amounts.get(entry.getKey()), 2));

      List<String> nodes = new ArrayList<>();

      for (String fld : values.keySet()) {
        if (ArrayUtils.contains(tableFields, fld)) {
          insert.addConstant(fld, BeeUtils.peek(values.get(fld)));
        } else {
          nodes.add(fld);
          nodes.add(BeeUtils.joinItems(values.get(fld)));
        }
      }
      qs.insertData(insert.addConstant(COL_TRADE_ITEM_NOTE,
          XmlUtils.createString("CargoInfo", nodes.toArray(new String[0]))));
    }
    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_CARGO_INCOMES)
        .addConstant(COL_SALE, saleId)
        .setWhere(wh)));
  }

  private ResponseObject createPurchaseInvoiceItems(Long purchaseId, Long currency,
      Set<Long> idList, Long mainItem) {

    if (!DataUtils.isId(purchaseId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error("Empty ID list");
    }
    IsCondition wh = sys.idInList(TBL_CARGO_EXPENSES, idList);

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_CARGO_EXPENSES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addFrom(TBL_CARGO_EXPENSES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_EXPENSES, COL_SERVICE))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_EXPENSES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .setWhere(SqlUtils.and(wh, SqlUtils.positive(TBL_CARGO_EXPENSES, COL_AMOUNT)))
        .addGroup(TBL_CARGO_EXPENSES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC);

    if (DataUtils.isId(mainItem)) {
      ss.addConstant(mainItem, COL_ITEM);
    } else {
      ss.addFields(TBL_SERVICES, COL_ITEM)
          .addGroup(TBL_SERVICES, COL_ITEM);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_CARGO_EXPENSES, COL_AMOUNT),
        SqlUtils.field(TBL_CARGO_EXPENSES, COL_CURRENCY),
        SqlUtils.nvl(SqlUtils.field(TBL_CARGO_EXPENSES, COL_DATE),
            SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE)), SqlUtils.constant(currency));

    SimpleRowSet rs = qs.getData(ss.addSum(xpr, COL_AMOUNT));
    ResponseObject response = new ResponseObject();

    for (SimpleRow row : rs) {
      SqlInsert insert = new SqlInsert(TBL_PURCHASE_ITEMS)
          .addConstant(COL_PURCHASE, purchaseId)
          .addConstant(COL_ITEM, row.getLong(COL_ITEM))
          .addConstant(COL_TRADE_ITEM_QUANTITY, 1)
          .addConstant(COL_TRADE_ITEM_PRICE, BeeUtils.round(row.getDouble(COL_AMOUNT), 2))
          .addConstant(COL_TRADE_VAT_PLUS, row.getBoolean(COL_TRADE_VAT_PLUS))
          .addConstant(COL_TRADE_VAT, row.getDouble(COL_TRADE_VAT))
          .addConstant(COL_TRADE_VAT_PERC, row.getBoolean(COL_TRADE_VAT_PERC));

      qs.insertData(insert);
    }
    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_CARGO_EXPENSES)
        .addConstant(COL_PURCHASE, purchaseId)
        .setWhere(wh)));
  }

  private ResponseObject createTripInvoiceItems(Long purchaseId, Long currency, Set<Long> idList,
      Long mainItem) {

    IsCondition wh = sys.idInList(TBL_TRIP_COSTS, idList);

    SqlSelect ss = new SqlSelect()
        .addConstant(purchaseId, COL_PURCHASE)
        .addFields(TBL_TRIP_COSTS, COL_TRADE_VAT_PLUS, COL_TRADE_VAT_PERC)
        .addField(TBL_TRIP_COSTS, COL_COSTS_NOTE, COL_TRADE_ITEM_NOTE)
        .addFrom(TBL_TRIP_COSTS)
        .setWhere(wh)
        .addGroup(TBL_TRIP_COSTS, COL_TRADE_VAT_PLUS, COL_COSTS_VAT, COL_TRADE_VAT_PERC,
            COL_COSTS_NOTE);

    if (DataUtils.isId(mainItem)) {
      IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
          TradeModuleBean.getAmountExpression(TBL_TRIP_COSTS),
          SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_CURRENCY),
          SqlUtils.field(TBL_TRIP_COSTS, COL_DATE), SqlUtils.constant(currency));

      ss.addConstant(mainItem, COL_ITEM)
          .addConstant(1, COL_TRADE_ITEM_QUANTITY)
          .addSum(xpr, COL_TRADE_ITEM_PRICE);
    } else {
      IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
          SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_PRICE),
          SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_CURRENCY),
          SqlUtils.field(TBL_TRIP_COSTS, COL_DATE), SqlUtils.constant(currency));

      ss.addFields(TBL_TRIP_COSTS, COL_ITEM)
          .addSum(TBL_TRIP_COSTS, COL_COSTS_QUANTITY, COL_TRADE_ITEM_QUANTITY)
          .addMax(xpr, COL_TRADE_ITEM_PRICE)
          .addGroup(TBL_TRIP_COSTS, COL_ITEM, COL_COSTS_PRICE);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_TRIP_COSTS, COL_TRADE_VAT),
        SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_CURRENCY),
        SqlUtils.field(TBL_TRIP_COSTS, COL_DATE), SqlUtils.constant(currency));

    ss.addMax(SqlUtils.sqlIf(SqlUtils.isNull(TBL_TRIP_COSTS, COL_TRADE_VAT_PERC), xpr,
        SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_VAT)), COL_TRADE_VAT);

    qs.loadData(TBL_PURCHASE_ITEMS, ss);

    ResponseObject response = new ResponseObject();

    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_TRIP_COSTS)
        .addConstant(COL_PURCHASE, purchaseId)
        .setWhere(wh)));
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
      return ResponseObject.warning(usr.getDictionary()
          .valueExists(BeeUtils.joinWords(usr.getDictionary().user(), login)));
    }
    Long role = prm.getRelation(PRM_SELF_SERVICE_ROLE);

    if (!DataUtils.isId(role)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, PRM_SELF_SERVICE_ROLE);
    }
    ResponseObject resp = ResponseObject.info(usr.getDictionary().newUser(), login);
    String email;

    try {
      email = new InternetAddress(reqInfo.getParameter(COL_EMAIL), true).getAddress();
    } catch (AddressException e) {
      return ResponseObject.error(e);
    }
    Long accountId = prm.getRelation(MailConstants.PRM_DEFAULT_ACCOUNT);

    if (!DataUtils.isId(accountId)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, MailConstants.PRM_DEFAULT_ACCOUNT);
    }
    Long user = qs.insertData(new SqlInsert(TBL_USERS)
        .addConstant(COL_LOGIN, login)
        .addConstant(COL_PASSWORD, Codec.encodePassword(password))
        .addConstant(COL_COMPANY_PERSON, reqInfo.getParameter(COL_COMPANY_PERSON))
        .addConstant(COL_USER_INTERFACE, UserInterface.SELF_SERVICE));

    qs.insertData(new SqlInsert(TBL_USER_ROLES)
        .addConstant(COL_USER, user)
        .addConstant(COL_ROLE, role));

    TextConstant constant = TextConstant.REGISTRATION_MAIL_CONTENT;

    BeeRowSet data = qs.getViewData(VIEW_TEXT_CONSTANTS,
        Filter.equals(COL_TEXT_CONSTANT, constant));

    String localizedContent = Localized.column(COL_TEXT_CONTENT,
        EnumUtils.getEnumByIndex(SupportedLocale.class, reqInfo.getParameterInt(COL_USER_LOCALE))
            .getLanguage());
    String text;

    if (DataUtils.isEmpty(data)) {
      text = constant.getDefaultContent();
    } else if (BeeConst.isUndef(DataUtils.getColumnIndex(localizedContent, data.getColumns()))) {
      text = data.getString(0, COL_TEXT_CONTENT);
    } else {
      text = BeeUtils.notEmpty(data.getString(0, localizedContent),
          data.getString(0, COL_TEXT_CONTENT));
    }
    if (!BeeUtils.isEmpty(text)) {
      cb.asynchronousCall(() -> mail.sendMail(accountId, email, null,
          text.replace("{login}", login).replace("{password}", password)));
    }
    return resp;
  }

  private ResponseObject generateDailyCosts(long tripId) {
    Long mainCountry = prm.getRelation(PRM_COUNTRY);

    SimpleRowSet rs = qs.getData(new SqlSelect().setDistinctMode(true)
        .addFields(TBL_COUNTRY_NORMS, COL_COUNTRY, COL_DAILY_COSTS_ITEM)
        .addFields(TBL_COUNTRY_DAILY_COSTS, COL_AMOUNT, COL_CURRENCY)
        .addExpr(SqlUtils.sqlIf(SqlUtils.or(
            SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM),
            SqlUtils.joinLess(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM,
                TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE)),
            SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE),
            SqlUtils.field(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM)), COL_ROUTE_DEPARTURE_DATE)
        .addExpr(SqlUtils.sqlIf(
            SqlUtils.or(SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO),
                SqlUtils.joinMore(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO,
                    TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE)),
            SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE),
            SqlUtils.field(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO)), COL_ROUTE_ARRIVAL_DATE)
        .addFrom(TBL_TRIP_ROUTES)
        .addFromInner(TBL_COUNTRY_NORMS,
            SqlUtils.compare(SqlUtils.sqlIf(SqlUtils.equals(TBL_TRIP_ROUTES,
                COL_ROUTE_ARRIVAL_COUNTRY, mainCountry),
                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_COUNTRY),
                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_COUNTRY)), Operator.EQ,
                SqlUtils.field(TBL_COUNTRY_NORMS, COL_COUNTRY)))
        .addFromInner(TBL_COUNTRY_DAILY_COSTS, SqlUtils.and(
            sys.joinTables(TBL_COUNTRY_NORMS, TBL_COUNTRY_DAILY_COSTS, COL_COUNTRY_NORM),
            SqlUtils.notNull(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE),
            SqlUtils.joinMoreEqual(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE, TBL_TRIP_ROUTES,
                COL_ROUTE_DEPARTURE_DATE),
            SqlUtils.or(SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM),
                SqlUtils.joinLess(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM,
                    TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE)),
            SqlUtils.or(SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO),
                SqlUtils.joinMore(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO,
                    TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE))))
        .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId))
        .addOrderDesc(null, COL_ROUTE_DEPARTURE_DATE, COL_ROUTE_ARRIVAL_DATE));

    qs.updateData(new SqlDelete(TBL_TRIP_COSTS)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TRIP_COSTS, COL_TRIP, tripId),
            SqlUtils.in(TBL_TRIP_COSTS, COL_COSTS_ITEM, TBL_COUNTRY_NORMS, COL_DAILY_COSTS_ITEM))));

    Map<String, Map<String, String>> map = new HashMap<>();
    DateTime lastArrivalTime = null;
    String lastKey = null;

    for (SimpleRow row : rs) {
      String key;

      if (TimeUtils.sameDate(row.getDateTime(COL_ROUTE_ARRIVAL_DATE), lastArrivalTime)) {
        key = lastKey;
      } else {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(COL_COSTS_COUNTRY, row.getValue(COL_COUNTRY));
        values.put(COL_COSTS_ITEM, row.getValue(COL_DAILY_COSTS_ITEM));
        values.put(COL_COSTS_PRICE, row.getValue(COL_AMOUNT));
        values.put(COL_COSTS_CURRENCY, row.getValue(COL_CURRENCY));

        key = Codec.md5(BeeUtils.joinItems(values.values()));

        if (!map.containsKey(key)) {
          map.put(key, values);
        }
        lastArrivalTime = row.getDateTime(COL_ROUTE_ARRIVAL_DATE);
        lastKey = key;
      }
      Map<String, String> values = map.get(key);

      values.put(COL_COSTS_QUANTITY,
          BeeUtils.toString(BeeUtils.toInt(values.get(COL_COSTS_QUANTITY))
              + Math.max(TimeUtils.dayDiff(row.getDateTime(COL_ROUTE_DEPARTURE_DATE),
              row.getDateTime(COL_ROUTE_ARRIVAL_DATE)), 1)));
    }
    if (!BeeUtils.isEmpty(lastKey)
        && BeeUtils.isPositive(TimeUtils.dayDiff(rs.getDateTime(rs.getNumberOfRows() - 1,
        COL_ROUTE_DEPARTURE_DATE), rs.getDateTime(rs.getNumberOfRows() - 1,
        COL_ROUTE_ARRIVAL_DATE)))) {

      Map<String, String> values = map.get(lastKey);
      values.put(COL_COSTS_QUANTITY,
          BeeUtils.toString(BeeUtils.toInt(values.get(COL_COSTS_QUANTITY)) + 1));
    }
    DateTime date = qs.getDateTime(new SqlSelect()
        .addFields(TBL_TRIPS, COL_TRIP_DATE)
        .addFrom(TBL_TRIPS)
        .setWhere(sys.idEquals(TBL_TRIPS, tripId)));

    Long payment = ArrayUtils.getQuietly(qs.getLongColumn(new SqlSelect()
        .addFields(TBL_PAYMENT_TYPES, sys.getIdName(TBL_PAYMENT_TYPES))
        .addFrom(TBL_PAYMENT_TYPES)
        .setWhere(SqlUtils.notNull(TBL_PAYMENT_TYPES, COL_PAYMENT_CASH))), 0);

    Long[] drivers = qs.getLongColumn(new SqlSelect()
        .addFields(TBL_TRIP_DRIVERS, sys.getIdName(TBL_TRIP_DRIVERS))
        .addFrom(TBL_TRIP_DRIVERS)
        .setWhere(SqlUtils.equals(TBL_TRIP_DRIVERS, COL_TRIP, tripId)));

    for (Long driver : drivers) {
      for (Map<String, String> values : map.values()) {
        SqlInsert insert = new SqlInsert(TBL_TRIP_COSTS)
            .addConstant(COL_TRIP, tripId)
            .addConstant(COL_COSTS_DATE, date)
            .addConstant(COL_DRIVER, driver)
            .addNotNull(COL_PAYMENT_TYPE, payment);

        for (Entry<String, String> entry : values.entrySet()) {
          if (Objects.equals(entry.getKey(), COL_COSTS_QUANTITY)
              && BeeUtils.toInt(entry.getValue()) == 0) {
            entry.setValue("1");
          }
          insert.addConstant(entry.getKey(), entry.getValue());
        }
        qs.insertData(insert);
      }
    }
    return ResponseObject.info(Localized.dictionary().createdRows(drivers.length * map.size()));
  }

  private ResponseObject generateTripRoute(long tripId) {
    List<Long> ids = qs.getLongList(new SqlSelect().setDistinctMode(true)
        .addFields(TBL_CARGO_HANDLING, COL_CARGO_TRIP)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_HANDLING, COL_CARGO_TRIP))
        .setWhere(SqlUtils.equals(TBL_CARGO_TRIPS, COL_TRIP, tripId)));

    SqlSelect query = new SqlSelect()
        .addField(TBL_CARGO_TRIPS, sys.getIdName(TBL_CARGO_TRIPS), COL_ROUTE_CARGO)
        .addFields(TBL_CARGO_HANDLING, COL_CARGO_WEIGHT, COL_LOADED_KILOMETERS,
            COL_EMPTY_KILOMETERS)
        .addFields(TBL_CARGO_PLACES, COL_PLACE_DATE, COL_PLACE_COUNTRY, COL_PLACE_CITY)
        .addFields(TBL_ORDER_CARGO, COL_CARGO_PARTIAL)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_CARGO_TRIPS, COL_TRIP, tripId),
            SqlUtils.notNull(TBL_CARGO_PLACES, COL_PLACE_DATE)));

    SqlSelect union = query.copyOf()
        .addConstant(0, VAR_UNLOADING)
        .addFromInner(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_HANDLING, COL_CARGO_TRIP))
        .addFromLeft(TBL_CARGO_PLACES,
            sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_LOADING_PLACE))

        .addUnion(query.copyOf()
            .addConstant(1, VAR_UNLOADING)
            .addFromInner(TBL_CARGO_HANDLING,
                sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_HANDLING, COL_CARGO_TRIP))
            .addFromLeft(TBL_CARGO_PLACES,
                sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE)));

    if (!BeeUtils.isEmpty(ids)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.not(sys.idInList(TBL_CARGO_TRIPS, ids))));
    }
    union.addUnion(query.copyOf()
        .addConstant(0, VAR_UNLOADING)
        .addFromInner(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_HANDLING, TBL_ORDER_CARGO, COL_CARGO_HANDLING))
        .addFromLeft(TBL_CARGO_PLACES,
            sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_LOADING_PLACE))

        .addUnion(query.copyOf()
            .addConstant(1, VAR_UNLOADING)
            .addFromInner(TBL_CARGO_HANDLING,
                sys.joinTables(TBL_CARGO_HANDLING, TBL_ORDER_CARGO, COL_CARGO_HANDLING))
            .addFromLeft(TBL_CARGO_PLACES,
                sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE)))

        .addUnion(query.copyOf()
            .addConstant(0, VAR_UNLOADING)
            .addFromInner(TBL_CARGO_HANDLING,
                sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_HANDLING, COL_CARGO))
            .addFromLeft(TBL_CARGO_PLACES,
                sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_LOADING_PLACE)))

        .addUnion(query.copyOf()
            .addConstant(1, VAR_UNLOADING)
            .addFromInner(TBL_CARGO_HANDLING,
                sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_HANDLING, COL_CARGO))
            .addFromLeft(TBL_CARGO_PLACES,
                sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE)))

        .addOrder(null, COL_PLACE_DATE, VAR_UNLOADING));

    Long currentCargo = null;
    double currentWeight = 0;
    DateTime currentDate = null;
    Long currentCountry = null;
    Long currentCity = null;

    Multimap<Long, Map<String, Object>> data = LinkedListMultimap.create();
    Map<Long, Pair<Integer, Integer>> stack = new HashMap<>();
    Set<Long> nonPartials = new HashSet<>();

    for (SimpleRow row : qs.getData(union)) {
      Long orderCargo = row.getLong(COL_ROUTE_CARGO);
      boolean loaded = stack.containsKey(orderCargo);
      boolean unloading = BeeUtils.unbox(row.getBoolean(VAR_UNLOADING));

      if (unloading && !loaded) {
        continue;
      }
      double weight = BeeUtils.unbox(row.getDouble(COL_CARGO_WEIGHT));
      DateTime date = row.getDateTime(COL_PLACE_DATE);
      Long country = row.getLong(COL_PLACE_COUNTRY);
      Long city = row.getLong(COL_PLACE_CITY);

      if (!BeeUtils.isEmpty(stack) && TimeUtils.isMore(date, currentDate)) {
        Map<String, Object> rec = new HashMap<>();
        data.put(unloading || !BeeUtils.isPositive(currentWeight) ? orderCargo : currentCargo, rec);

        rec.put(COL_ROUTE_DEPARTURE_DATE, currentDate);
        rec.put(COL_ROUTE_DEPARTURE_COUNTRY, currentCountry);
        rec.put(COL_ROUTE_DEPARTURE_CITY, currentCity);
        rec.put(COL_ROUTE_WEIGHT, currentWeight);
        rec.put(COL_ROUTE_ARRIVAL_DATE, date);
        rec.put(COL_ROUTE_ARRIVAL_COUNTRY, country);
        rec.put(COL_ROUTE_ARRIVAL_CITY, city);
      }
      currentDate = date;
      currentCountry = country;
      currentCity = city;

      if (unloading) {
        Pair<Integer, Integer> pair = stack.get(orderCargo);

        int emptyKm = BeeUtils.unbox(row.getInt(COL_EMPTY_KILOMETERS));
        pair.setA(pair.getA() + BeeUtils.unbox(row.getInt(COL_LOADED_KILOMETERS)) + emptyKm);
        pair.setB(pair.getB() + emptyKm);

        currentWeight -= weight;
      } else {
        if (!loaded) {
          if (!BeeUtils.unbox(row.getBoolean(COL_CARGO_PARTIAL))) {
            nonPartials.add(orderCargo);
          }
          stack.put(orderCargo, Pair.of(0, 0));
        }
        currentCargo = orderCargo;
        currentWeight += weight;
      }
    }
    if (currentWeight > 0) {
      Map<String, Object> rec = new HashMap<>();
      data.put(currentCargo, rec);

      rec.put(COL_ROUTE_DEPARTURE_DATE, currentDate);
      rec.put(COL_ROUTE_DEPARTURE_COUNTRY, currentCountry);
      rec.put(COL_ROUTE_DEPARTURE_CITY, currentCity);
      rec.put(COL_ROUTE_WEIGHT, currentWeight);
    }
    for (Long orderCargo : stack.keySet()) {
      if (data.containsKey(orderCargo)) {
        Collection<Map<String, Object>> recs = data.get(orderCargo);
        Map<String, Object> info = new HashMap<>();

        info.put(COL_ROUTE_KILOMETERS, stack.get(orderCargo).getA().doubleValue() / recs.size());
        info.put(COL_EMPTY_KILOMETERS, stack.get(orderCargo).getB().doubleValue() / recs.size());
        info.put(COL_ROUTE_CARGO, nonPartials.isEmpty() || nonPartials.contains(orderCargo)
            ? orderCargo : nonPartials.iterator().next());

        for (Map<String, Object> rec : recs) {
          rec.putAll(info);
        }
      }
    }
    if (data.isEmpty()) {
      return ResponseObject.warning(Localized.dictionary().noData());
    }
    qs.updateData(new SqlDelete(TBL_TRIP_ROUTES)
        .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId)));

    for (Map<String, Object> rec : data.values()) {
      SqlInsert insert = new SqlInsert(TBL_TRIP_ROUTES)
          .addConstant(COL_TRIP, tripId);

      for (Entry<String, Object> entry : rec.entrySet()) {
        insert.addConstant(entry.getKey(), entry.getValue());

        if (Objects.equals(entry.getKey(), COL_ROUTE_DEPARTURE_DATE)) {
          insert.addConstant(COL_ROUTE_SEASON,
              BeeUtils.betweenInclusive(((DateTime) entry.getValue()).getMonth(), 4, 10)
                  ? FuelSeason.SUMMER.ordinal() : FuelSeason.WINTER.ordinal());
        }
      }
      qs.insertData(insert);
    }
    return ResponseObject.emptyResponse();
  }

  private ResponseObject getAssessmentQuantityReport(RequestInfo reqInfo) {
    Long startDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_FROM));
    Long endDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_TO));

    Set<Long> departments = DataUtils.parseIdSet(reqInfo.getParameter(AR_DEPARTMENT));
    Set<Long> managers = DataUtils.parseIdSet(reqInfo.getParameter(AR_MANAGER));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    HasConditions where = SqlUtils.and(SqlUtils.notNull(TBL_ASSESSMENTS, COL_ASSESSMENT_STATUS));

    if (startDate != null) {
      where.add(SqlUtils.moreEqual(TBL_ORDERS, COL_ORDER_DATE, startDate));
    }
    if (endDate != null) {
      where.add(SqlUtils.less(TBL_ORDERS, COL_ORDER_DATE, endDate));
    }

    if (!departments.isEmpty()) {
      where.add(SqlUtils.inList(TBL_ASSESSMENTS, COL_DEPARTMENT, departments));
    }
    if (!managers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_USERS, COL_COMPANY_PERSON, managers));
    }

    SqlSelect query = new SqlSelect();
    query.addFields(TBL_ASSESSMENTS, COL_ASSESSMENT_STATUS, COL_ASSESSMENT);

    if (groupBy.contains(BeeConst.MONTH)) {
      query.addFields(TBL_ORDERS, COL_ORDER_DATE);
      query.addEmptyNumeric(BeeConst.YEAR, 4, 0);
      query.addEmptyNumeric(BeeConst.MONTH, 2, 0);
    }

    if (groupBy.contains(AR_DEPARTMENT)) {
      query.addFields(TBL_ASSESSMENTS, COL_DEPARTMENT);
    }
    if (groupBy.contains(AR_MANAGER)) {
      query.addFields(TBL_USERS, COL_COMPANY_PERSON);
    }

    query.addFrom(TBL_ASSESSMENTS);
    if (!managers.isEmpty() || groupBy.contains(AR_MANAGER) || groupBy.contains(BeeConst.MONTH)
        || startDate != null || endDate != null) {

      query.addFromInner(TBL_ORDER_CARGO,
          sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO));
      query.addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER));

      if (!managers.isEmpty() || groupBy.contains(AR_MANAGER)) {
        query.addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_ORDER_MANAGER));
      }
    }

    query.setWhere(where);

    String tmp = qs.sqlCreateTemp(query);

    long count;
    if (groupBy.contains(BeeConst.MONTH)) {
      count = qs.setYearMonth(tmp, COL_ORDER_DATE, BeeConst.YEAR, BeeConst.MONTH);
    } else {
      count = qs.sqlCount(tmp, null);
    }

    if (count <= 0) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    query = new SqlSelect();
    query.addFrom(tmp);

    for (String by : groupBy) {
      switch (by) {
        case BeeConst.MONTH:
          query.addFields(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addOrder(tmp, BeeConst.YEAR, BeeConst.MONTH);
          break;

        case AR_DEPARTMENT:
          query.addFields(tmp, COL_DEPARTMENT);
          query.addFields(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);

          query.addFromLeft(TBL_DEPARTMENTS,
              SqlUtils.join(TBL_DEPARTMENTS, sys.getIdName(TBL_DEPARTMENTS), tmp, COL_DEPARTMENT));

          query.addGroup(tmp, COL_DEPARTMENT);
          query.addGroup(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          query.addOrder(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          break;

        case AR_MANAGER:
          query.addFields(tmp, COL_COMPANY_PERSON);
          query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addFromLeft(TBL_COMPANY_PERSONS,
              SqlUtils.join(TBL_COMPANY_PERSONS, sys.getIdName(TBL_COMPANY_PERSONS),
                  tmp, COL_COMPANY_PERSON));
          query.addFromLeft(TBL_PERSONS,
              sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

          query.addGroup(tmp, COL_COMPANY_PERSON);
          query.addGroup(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addOrder(TBL_PERSONS, COL_LAST_NAME, COL_FIRST_NAME);
          query.addOrder(tmp, COL_COMPANY_PERSON);
          break;
      }
    }

    query.addCount(AR_RECEIVED);

    IsExpression xpr = SqlUtils.field(tmp, COL_ASSESSMENT_STATUS);

    query.addSum(SqlUtils.sqlCase(xpr, AssessmentStatus.ANSWERED.ordinal(), 1, 0), AR_ANSWERED);
    query.addSum(SqlUtils.sqlCase(xpr, AssessmentStatus.LOST.ordinal(), 1, 0), AR_LOST);
    query.addSum(SqlUtils.sqlCase(xpr, AssessmentStatus.APPROVED.ordinal(), 1, 0), AR_APPROVED);

    query.addSum(SqlUtils.sqlIf(SqlUtils.isNull(tmp, COL_ASSESSMENT), 0, 1), AR_SECONDARY);

    SimpleRowSet result = qs.getData(query);
    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(result)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private ResponseObject getAssessmentTotals(Long assessmentId, Long currency, boolean isPrimary) {
    Assert.state(DataUtils.isId(assessmentId));

    SqlSelect query = null;

    for (String tbl : new String[] {TBL_CARGO_INCOMES, TBL_CARGO_EXPENSES}) {
      for (int i = 0; i < 2; i++) {
        if (i > 0 && !isPrimary) {
          break;
        }
        SqlSelect ss = new SqlSelect()
            .addConstant(tbl + (i > 0 ? VAR_TOTAL : ""), COL_SERVICE)
            .addFrom(tbl)
            .addFromInner(TBL_ASSESSMENTS, SqlUtils.joinUsing(tbl, TBL_ASSESSMENTS, COL_CARGO))
            .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, tbl, COL_CARGO))
            .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER));

        if (i > 0) {
          ss.setWhere(SqlUtils.equals(TBL_ASSESSMENTS, COL_ASSESSMENT, assessmentId));
        } else {
          ss.setWhere(sys.idEquals(TBL_ASSESSMENTS, assessmentId));
        }
        IsExpression amountExpr = SqlUtils.field(tbl, COL_AMOUNT);

        if (BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT))) {
          amountExpr = TradeModuleBean.getWithoutVatExpression(tbl, amountExpr);
        } else {
          amountExpr = TradeModuleBean.getTotalExpression(tbl, amountExpr);
        }
        IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr,
            SqlUtils.field(tbl, COL_CURRENCY),
            SqlUtils.nvl(SqlUtils.field(tbl, COL_DATE), SqlUtils.field(TBL_ORDERS, COL_DATE)),
            SqlUtils.constant(currency));

        ss.addSum(xpr, COL_AMOUNT);

        if (query == null) {
          query = ss;
        } else {
          query.setUnionAllMode(true).addUnion(ss);
        }
      }
    }
    return ResponseObject.response(qs.getData(query));
  }

  private ResponseObject getAssessmentTurnoverReport(RequestInfo reqInfo) {
    Long startDate = reqInfo.getParameterLong(Service.VAR_FROM);
    Long endDate = reqInfo.getParameterLong(Service.VAR_TO);

    Long currency = reqInfo.getParameterLong(COL_CURRENCY);

    Set<Long> departments = DataUtils.parseIdSet(reqInfo.getParameter(AR_DEPARTMENT));
    Set<Long> managers = DataUtils.parseIdSet(reqInfo.getParameter(AR_MANAGER));
    Set<Long> customers = DataUtils.parseIdSet(reqInfo.getParameter(AR_CUSTOMER));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    HasConditions where = SqlUtils.and(
        SqlUtils.equals(TBL_ORDERS, COL_STATUS, OrderStatus.COMPLETED.ordinal()),
        SqlUtils.in(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_INCOMES, COL_CARGO,
            SqlUtils.notNull(TBL_CARGO_INCOMES, COL_SALE)),
        SqlUtils.not(SqlUtils.in(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_INCOMES, COL_CARGO,
            SqlUtils.isNull(TBL_CARGO_INCOMES, COL_SALE))));

    if (startDate != null || endDate != null) {
      if (startDate != null) {
        where.add(SqlUtils.moreEqual(TBL_CARGO_PLACES, COL_PLACE_DATE, startDate));
      }
      if (endDate != null) {
        where.add(SqlUtils.less(TBL_CARGO_PLACES, COL_PLACE_DATE, endDate));
      }
    } else {
      where.add(SqlUtils.notNull(TBL_CARGO_PLACES, COL_PLACE_DATE));
    }

    if (!departments.isEmpty()) {
      where.add(SqlUtils.inList(TBL_ASSESSMENTS, COL_DEPARTMENT, departments));
    }
    if (!managers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_USERS, COL_COMPANY_PERSON, managers));
    }
    if (!customers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_ORDERS, COL_CUSTOMER, customers));
    }

    SqlSelect query = new SqlSelect();
    query.addFields(TBL_ASSESSMENTS, COL_ASSESSMENT, COL_CARGO);

    String orderDateAlias = "OrdDt" + SqlUtils.uniqueName();
    query.addField(TBL_ORDERS, COL_ORDER_DATE, orderDateAlias);

    if (groupBy.contains(BeeConst.MONTH)) {
      query.addFields(TBL_CARGO_PLACES, COL_PLACE_DATE);
      query.addEmptyNumeric(BeeConst.YEAR, 4, 0);
      query.addEmptyNumeric(BeeConst.MONTH, 2, 0);
    }

    if (groupBy.contains(AR_DEPARTMENT)) {
      query.addFields(TBL_ASSESSMENTS, COL_DEPARTMENT);
    }
    if (groupBy.contains(AR_MANAGER)) {
      query.addFields(TBL_USERS, COL_COMPANY_PERSON);
    }
    if (groupBy.contains(AR_CUSTOMER)) {
      query.addFields(TBL_ORDERS, COL_CUSTOMER);
    }

    query.addFrom(TBL_ASSESSMENTS)
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO))
        .addFromLeft(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_HANDLING, TBL_ORDER_CARGO, COL_CARGO_HANDLING))
        .addFromInner(TBL_CARGO_PLACES,
            sys.joinTables(TBL_CARGO_PLACES, COL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER));

    if (!managers.isEmpty() || groupBy.contains(AR_MANAGER)) {
      query.addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_ORDER_MANAGER));
    }

    query.setWhere(where);

    String tmp = qs.sqlCreateTemp(query);

    long count;
    if (groupBy.contains(BeeConst.MONTH)) {
      count = qs.setYearMonth(tmp, COL_PLACE_DATE, BeeConst.YEAR, BeeConst.MONTH);
    } else {
      count = qs.sqlCount(tmp, null);
    }

    if (count <= 0) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    query = new SqlSelect();
    query.addFrom(tmp);

    for (String by : groupBy) {
      switch (by) {
        case BeeConst.MONTH:
          query.addFields(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addOrder(tmp, BeeConst.YEAR, BeeConst.MONTH);
          break;

        case AR_DEPARTMENT:
          query.addFields(tmp, COL_DEPARTMENT);
          query.addFields(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);

          query.addFromLeft(TBL_DEPARTMENTS,
              SqlUtils.join(TBL_DEPARTMENTS, sys.getIdName(TBL_DEPARTMENTS), tmp, COL_DEPARTMENT));

          query.addGroup(tmp, COL_DEPARTMENT);
          query.addGroup(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          query.addOrder(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          break;

        case AR_MANAGER:
          query.addFields(tmp, COL_COMPANY_PERSON);
          query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addFromLeft(TBL_COMPANY_PERSONS,
              SqlUtils.join(TBL_COMPANY_PERSONS, sys.getIdName(TBL_COMPANY_PERSONS),
                  tmp, COL_COMPANY_PERSON));
          query.addFromLeft(TBL_PERSONS,
              sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

          query.addGroup(tmp, COL_COMPANY_PERSON);
          query.addGroup(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addOrder(TBL_PERSONS, COL_LAST_NAME, COL_FIRST_NAME);
          query.addOrder(tmp, COL_COMPANY_PERSON);
          break;

        case AR_CUSTOMER:
          query.addFields(tmp, COL_CUSTOMER);
          query.addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME);

          query.addFromLeft(TBL_COMPANIES,
              SqlUtils.join(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), tmp, COL_CUSTOMER));

          query.addGroup(tmp, COL_CUSTOMER);
          query.addGroup(TBL_COMPANIES, COL_COMPANY_NAME);
          query.addOrder(TBL_COMPANIES, COL_COMPANY_NAME);
          break;
      }
    }

    query.addCount(AR_RECEIVED);

    if (!DataUtils.isId(currency)) {
      currency = prm.getRelation(PRM_CURRENCY);
    }

    SqlSelect subIncome = new SqlSelect()
        .addFields(tmp, COL_CARGO)
        .addFrom(tmp)
        .addFromInner(TBL_CARGO_INCOMES,
            SqlUtils.join(TBL_CARGO_INCOMES, COL_CARGO, tmp, COL_CARGO))
        .addGroup(tmp, COL_CARGO);

    IsExpression incomeXpr = getAssessmentTurnoverExpression(subIncome, TBL_CARGO_INCOMES,
        tmp, orderDateAlias, currency, BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));
    subIncome.addSum(incomeXpr, AR_INCOME);

    SqlSelect subExpense = new SqlSelect()
        .addFields(tmp, COL_CARGO)
        .addFrom(tmp)
        .addFromInner(TBL_CARGO_EXPENSES,
            SqlUtils.join(TBL_CARGO_EXPENSES, COL_CARGO, tmp, COL_CARGO))
        .addGroup(tmp, COL_CARGO);

    IsExpression expenseXpr = getAssessmentTurnoverExpression(subExpense, TBL_CARGO_EXPENSES,
        tmp, orderDateAlias, currency, BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));
    subExpense.addSum(expenseXpr, AR_EXPENSE);

    String incomeAlias = "Inc" + SqlUtils.uniqueName();
    String expenseAlias = "Exp" + SqlUtils.uniqueName();

    query.addFromLeft(subIncome, incomeAlias,
        SqlUtils.join(incomeAlias, COL_CARGO, tmp, COL_CARGO));
    query.addFromLeft(subExpense, expenseAlias,
        SqlUtils.join(expenseAlias, COL_CARGO, tmp, COL_CARGO));

    query.addSum(incomeAlias, AR_INCOME);
    query.addSum(expenseAlias, AR_EXPENSE);

    IsCondition condition = SqlUtils.isNull(tmp, COL_ASSESSMENT);
    query.addSum(SqlUtils.sqlIf(condition, 0, 1), AR_SECONDARY);

    query.addSum(SqlUtils.sqlIf(condition, 0, SqlUtils.field(incomeAlias, AR_INCOME)),
        AR_SECONDARY_INCOME);
    query.addSum(SqlUtils.sqlIf(condition, 0, SqlUtils.field(expenseAlias, AR_EXPENSE)),
        AR_SECONDARY_EXPENSE);

    SimpleRowSet result = qs.getData(query);
    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(result)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private ResponseObject getCargoTotal(long cargoId, Long currency) {
    String val = null;
    SimpleRow row = qs.getRow(rep.getCargoIncomeQuery(new SqlSelect()
            .addConstant(cargoId, COL_CARGO), currency,
        BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT))));

    if (row != null) {
      val = BeeUtils.round(row.getValue("CargoIncome"), 2);
    }
    return ResponseObject.response(BeeUtils.notEmpty(val, "0.00"));
  }

  private ResponseObject getCargoUsage(String viewName, String[] ids) {
    String source = sys.getViewSource(viewName);
    IsExpression ref;
    SqlSelect ss = new SqlSelect().addFrom(TBL_CARGO_TRIPS);

    if (BeeUtils.same(source, TBL_TRIPS)) {
      ref = SqlUtils.field(TBL_CARGO_TRIPS, COL_TRIP);

    } else if (BeeUtils.same(source, TBL_ORDER_CARGO)) {
      ref = SqlUtils.field(TBL_CARGO_TRIPS, COL_CARGO);

    } else if (BeeUtils.same(source, TBL_ORDERS)) {
      ss.addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO));
      ref = SqlUtils.field(TBL_ORDER_CARGO, COL_ORDER);

    } else {
      return ResponseObject.error("Table not supported:", source);
    }
    int cnt = qs.sqlCount(ss.setWhere(SqlUtils.inList(ref, (Object[]) ids)));

    return ResponseObject.response(cnt);
  }

  private ResponseObject getColors(RequestInfo reqInfo) {
    Long theme;
    if (reqInfo.hasParameter(Service.VAR_ID)) {
      theme = BeeUtils.toLong(reqInfo.getParameter(Service.VAR_ID));
    } else {
      theme = null;
    }

    return ResponseObject.response(getThemeColors(theme));
  }

  @SuppressWarnings("unchecked")
  private ResponseObject getCreditInfo(RequestInfo reqInfo) {
    Long company = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_COMPANY));

    if (!DataUtils.isId(company)) {
      Long incomeId = BeeUtils.toLongOrNull(reqInfo.getParameter(TBL_CARGO_INCOMES));

      if (!DataUtils.isId(incomeId)) {
        return ResponseObject.emptyResponse();
      }
      SimpleRow row = qs.getRow(new SqlSelect()
          .addFields(TBL_ORDERS, COL_CUSTOMER, COL_PAYER)
          .addFrom(TBL_CARGO_INCOMES)
          .addFromInner(TBL_ORDER_CARGO,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
          .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
          .setWhere(sys.idEquals(TBL_CARGO_INCOMES, incomeId)));

      if (row != null) {
        company = BeeUtils.nvl(row.getLong(COL_PAYER), row.getLong(COL_CUSTOMER));
      }
    }
    Map<String, Object> resp = new HashMap<>();

    if (DataUtils.isId(company)) {
      ResponseObject response = trd.getCreditInfo(company);

      if (response.hasErrors()) {
        return response;
      }
      resp.putAll((Map<? extends String, ?>) response.getResponse());
      Long curr = (Long) resp.get(COL_COMPANY_LIMIT_CURRENCY);

      SqlSelect query = new SqlSelect()
          .addFrom(TBL_ORDERS)
          .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
          .addFromInner(TBL_CARGO_INCOMES,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
          .setWhere(SqlUtils.and(SqlUtils.or(SqlUtils.equals(TBL_ORDERS, COL_PAYER, company),
              SqlUtils.and(SqlUtils.isNull(TBL_ORDERS, COL_PAYER),
                  SqlUtils.equals(TBL_ORDERS, COL_CUSTOMER, company))),
              SqlUtils.isNull(TBL_CARGO_INCOMES, COL_SALE)));

      IsExpression cargoIncome;
      IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
          SqlUtils.field(TBL_ORDERS, COL_DATE));

      if (DataUtils.isId(curr)) {
        cargoIncome = ExchangeUtils.exchangeFieldTo(query,
            TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT)),
            SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr, SqlUtils.constant(curr));
      } else {
        cargoIncome = ExchangeUtils.exchangeField(query,
            TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT)),
            SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr);
      }
      resp.put(VAR_INCOME, BeeUtils.round(qs.getValue(query.addSum(cargoIncome, VAR_INCOME)), 2));
    }
    return ResponseObject.response(resp);
  }

  private ResponseObject getDriverBusyDates(Long driver, Long from, Long to) {
    SqlSelect query = new SqlSelect()
        .addField(TBL_TRIPS, COL_TRIP_NO, COL_NOTES)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_FROM),
            SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE)), COL_ABSENCE_FROM)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_TO),
            SqlUtils.field(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)), COL_ABSENCE_TO)
        .addFrom(TBL_TRIPS)
        .addFromInner(TBL_TRIP_DRIVERS,
            SqlUtils.and(sys.joinTables(TBL_TRIPS, TBL_TRIP_DRIVERS, COL_TRIP),
                SqlUtils.equals(TBL_TRIP_DRIVERS, COL_DRIVER, driver)))
        .setWhere(SqlUtils.or(
            SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.isNull(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)),
            SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.more(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE, from)),
            SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.more(TBL_TRIPS, COL_TRIP_DATE_TO, from))))
        .addOrder(null, COL_ABSENCE_FROM, COL_ABSENCE_TO);

    if (Objects.nonNull(to)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.or(SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
              SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE, to)),
              SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
                  SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE_FROM, to)))));
    }
    query.addUnion(new SqlSelect()
        .addExpr(SqlUtils.concat(SqlUtils.field(TBL_ABSENCE_TYPES, COL_ABSENCE_NAME), "' '",
            SqlUtils.nvl(SqlUtils.field(TBL_DRIVER_ABSENCE, COL_ABSENCE_NOTES), "''")), COL_NOTES)
        .addFields(TBL_DRIVER_ABSENCE, COL_ABSENCE_FROM, COL_ABSENCE_TO)
        .addFrom(TBL_DRIVER_ABSENCE)
        .addFromInner(TBL_ABSENCE_TYPES,
            sys.joinTables(TBL_ABSENCE_TYPES, TBL_DRIVER_ABSENCE, COL_ABSENCE))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_DRIVER_ABSENCE, COL_DRIVER, driver),
            SqlUtils.more(TBL_DRIVER_ABSENCE, COL_ABSENCE_TO, from),
            Objects.nonNull(to) ? SqlUtils.less(TBL_DRIVER_ABSENCE, COL_ABSENCE_FROM, to) : null)));

    List<String> messages = new ArrayList<>();
    Dictionary loc = usr.getDictionary();

    for (SimpleRow row : qs.getData(query)) {
      messages.add(BeeUtils.joinWords(loc.dateFromShort(), row.getDate(COL_ABSENCE_FROM),
          Objects.isNull(row.getDate(COL_ABSENCE_TO))
              ? null : loc.dateToShort() + " " + row.getDate(COL_ABSENCE_TO),
          row.getValue(COL_NOTES)));
    }

    return ResponseObject.response(messages);
  }

  private Multimap<Long, Long> getDriverGroups(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_DRIVER_GROUPS, COL_DRIVER, COL_GROUP)
        .addFrom(TBL_DRIVER_GROUPS)
        .setWhere(condition);

    SimpleRowSet data = qs.getData(query);

    Multimap<Long, Long> result = HashMultimap.create();
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_DRIVER), row.getLong(COL_GROUP));
      }
    }

    return result;
  }

  private ResponseObject getDtbData() {
    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    List<Color> colors = getThemeColors(null);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet transportGroups = qs.getViewData(VIEW_TRANSPORT_GROUPS);
    if (!DataUtils.isEmpty(transportGroups)) {
      settings.setTableProperty(PROP_TRANSPORT_GROUPS, transportGroups.serialize());
    }

    BeeRowSet cargoTypes = qs.getViewData(VIEW_CARGO_TYPES);
    if (!DataUtils.isEmpty(cargoTypes)) {
      settings.setTableProperty(PROP_CARGO_TYPES, cargoTypes.serialize());
    }

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    BeeRowSet cities = qs.getViewData(VIEW_CITIES, Filter.any(COL_COUNTRY, countries.getRowIds()));
    settings.setTableProperty(PROP_CITIES, cities.serialize());

    BeeRowSet drivers = qs.getViewData(VIEW_DRIVERS);
    if (DataUtils.isEmpty(drivers)) {
      logger.warning(SVC_GET_DTB_DATA, "drivers not available");
      return ResponseObject.response(settings);
    }

    List<Long> driverIds = DataUtils.getRowIds(drivers);

    IsCondition groupWhere = SqlUtils.inList(TBL_DRIVER_GROUPS, COL_DRIVER, driverIds);
    Multimap<Long, Long> driverGroups = getDriverGroups(groupWhere);
    if (!driverGroups.isEmpty()) {
      for (BeeRow row : drivers) {
        if (driverGroups.containsKey(row.getId())) {
          row.setProperty(PROP_DRIVER_GROUPS,
              DataUtils.buildIdList(driverGroups.get(row.getId())));
        }
      }
    }

    settings.setTableProperty(PROP_DRIVERS, drivers.serialize());

    BeeRowSet absence = qs.getViewData(VIEW_DRIVER_ABSENCE,
        Filter.and(Filter.any(COL_DRIVER, driverIds), Filter.isMoreEqual(COL_ABSENCE_FROM,
            Value.getValue(TimeUtils.startOfYear()))));
    if (!DataUtils.isEmpty(absence)) {
      settings.setTableProperty(PROP_ABSENCE, absence.serialize());
    }

    IsCondition tripDriverWhere = SqlUtils.inList(TBL_TRIP_DRIVERS, COL_DRIVER, driverIds);

    SimpleRowSet tripDrivers = getTripDrivers(tripDriverWhere);
    if (DataUtils.isEmpty(tripDrivers)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_TRIP_DRIVERS, tripDrivers.serialize());

    IsCondition tripWhere = SqlUtils.in(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
        TBL_TRIP_DRIVERS, COL_TRIP, tripDriverWhere);

    SqlSelect tripQuery = getTripQuery(tripWhere);
    tripQuery.addOrder(TBL_TRIPS, COL_TRIP_DATE);

    SimpleRowSet trips = qs.getData(tripQuery);
    if (DataUtils.isEmpty(trips)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_TRIPS, trips.serialize());

    SqlSelect freightQuery = getFreightQuery(tripWhere);

    SimpleRowSet freights = qs.getData(freightQuery);
    if (DataUtils.isEmpty(freights)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_FREIGHTS, freights.serialize());

    SqlSelect cargoHandlingQuery = getFreightHandlingQuery(tripWhere);

    SimpleRowSet cargoHandling = qs.getData(cargoHandlingQuery);
    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }

    return ResponseObject.response(settings);
  }

  private SqlSelect getFreightHandlingQuery(IsCondition tripWhere) {
    String loadAlias = "load_" + SqlUtils.uniqueName();
    String unlAlias = "unl_" + SqlUtils.uniqueName();

    IsCondition handlingWhere = SqlUtils.or(SqlUtils.notNull(loadAlias, COL_PLACE_DATE),
        SqlUtils.notNull(unlAlias, COL_PLACE_DATE));

    return new SqlSelect()
        .addFrom(TBL_TRIPS)
        .addFromInner(TBL_CARGO_TRIPS, sys.joinTables(TBL_TRIPS, TBL_CARGO_TRIPS, COL_TRIP))
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .addFromInner(TBL_CARGO_HANDLING,
            SqlUtils.or(SqlUtils.and(SqlUtils.notNull(TBL_CARGO_TRIPS, COL_CARGO_HANDLING),
                sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_HANDLING, COL_CARGO_TRIP),
                SqlUtils.joinNotEqual(TBL_CARGO_TRIPS, COL_CARGO_HANDLING, TBL_CARGO_HANDLING,
                    sys.getIdName(TBL_CARGO_HANDLING))),
                SqlUtils.and(SqlUtils.isNull(TBL_CARGO_TRIPS, COL_CARGO_HANDLING),
                    sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_HANDLING, COL_CARGO))))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            sys.joinTables(TBL_CARGO_PLACES, loadAlias, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            sys.joinTables(TBL_CARGO_PLACES, unlAlias, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFields(TBL_CARGO_TRIPS, COL_CARGO)
        .addFields(TBL_CARGO_HANDLING, COL_CARGO_HANDLING_NOTES)
        .addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE))
        .addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY))
        .addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER))
        .addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE))
        .addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY))
        .addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER))
        .setWhere(SqlUtils.and(tripWhere, handlingWhere))
        .addOrder(TBL_CARGO_TRIPS, COL_CARGO);
  }

  private SqlSelect getFreightQuery(IsCondition where) {
    String loadAlias = "load_" + SqlUtils.uniqueName();
    String unlAlias = "unl_" + SqlUtils.uniqueName();

    String defLoadAlias = "defl_" + SqlUtils.uniqueName();
    String defUnlAlias = "defu_" + SqlUtils.uniqueName();
    String defHandling = SqlUtils.uniqueName();

    return new SqlSelect()
        .addFrom(TBL_TRIPS)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, COL_TRIP_ID))
        .addFromLeft(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_HANDLING, TBL_CARGO_TRIPS, COL_CARGO_HANDLING))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            sys.joinTables(TBL_CARGO_PLACES, loadAlias, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            sys.joinTables(TBL_CARGO_PLACES, unlAlias, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .addFromLeft(TBL_CARGO_HANDLING, defHandling,
            sys.joinTables(TBL_CARGO_HANDLING, defHandling, TBL_ORDER_CARGO, COL_CARGO_HANDLING))
        .addFromLeft(TBL_CARGO_PLACES, defLoadAlias,
            sys.joinTables(TBL_CARGO_PLACES, defLoadAlias, defHandling, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, defUnlAlias,
            sys.joinTables(TBL_CARGO_PLACES, defUnlAlias, defHandling, COL_UNLOADING_PLACE))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
        .addFields(TBL_TRIPS, COL_TRIP_ID, COL_VEHICLE)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_CARGO_TRIPS, COL_TRAILER),
            SqlUtils.field(TBL_TRIPS, COL_TRAILER)), COL_TRAILER)
        .addFields(TBL_CARGO_TRIPS, COL_CARGO, COL_CARGO_TRIP_ID)
        .addField(TBL_CARGO_TRIPS, sys.getVersionName(TBL_CARGO_TRIPS), ALS_CARGO_TRIP_VERSION)
        .addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE))
        .addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY))
        .addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER))
        .addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE))
        .addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY))
        .addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER))
        .addFields(TBL_ORDER_CARGO, COL_ORDER, COL_CARGO_TYPE, COL_CARGO_DESCRIPTION,
            COL_CARGO_NOTES)
        .addField(defLoadAlias, COL_PLACE_DATE, defaultLoadingColumnAlias(COL_PLACE_DATE))
        .addField(defLoadAlias, COL_PLACE_COUNTRY, defaultLoadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(defLoadAlias, COL_PLACE_ADDRESS, defaultLoadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(defLoadAlias, COL_PLACE_POST_INDEX,
            defaultLoadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(defLoadAlias, COL_PLACE_CITY, defaultLoadingColumnAlias(COL_PLACE_CITY))
        .addField(defLoadAlias, COL_PLACE_NUMBER, defaultLoadingColumnAlias(COL_PLACE_NUMBER))
        .addField(defUnlAlias, COL_PLACE_DATE, defaultUnloadingColumnAlias(COL_PLACE_DATE))
        .addField(defUnlAlias, COL_PLACE_COUNTRY, defaultUnloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(defUnlAlias, COL_PLACE_ADDRESS, defaultUnloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(defUnlAlias, COL_PLACE_POST_INDEX,
            defaultUnloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(defUnlAlias, COL_PLACE_CITY, defaultUnloadingColumnAlias(COL_PLACE_CITY))
        .addField(defUnlAlias, COL_PLACE_NUMBER, defaultUnloadingColumnAlias(COL_PLACE_NUMBER))
        .addFields(TBL_ORDERS, COL_ORDER_NO, COL_CUSTOMER, COL_ORDER_MANAGER, COL_STATUS)
        .addField(TBL_ORDERS, COL_ORDER_DATE, ALS_ORDER_DATE)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER_NAME)
        .setWhere(where)
        .addOrder(TBL_TRIPS, COL_TRIP_ID);
  }

  private ResponseObject getFxData() {
    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    Long theme = settings.getLong(0, settings.getColumnIndex(COL_FX_THEME));
    List<Color> colors = getThemeColors(theme);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet cargoTypes = qs.getViewData(VIEW_CARGO_TYPES);
    if (!DataUtils.isEmpty(cargoTypes)) {
      settings.setTableProperty(PROP_CARGO_TYPES, cargoTypes.serialize());
    }

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    BeeRowSet cities = qs.getViewData(VIEW_CITIES, Filter.any(COL_COUNTRY, countries.getRowIds()));
    settings.setTableProperty(PROP_CITIES, cities.serialize());

    String loadAlias = "load_" + SqlUtils.uniqueName();
    String unlAlias = "unl_" + SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect()
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
        .addFromLeft(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_CARGO_HANDLING, TBL_ORDER_CARGO, COL_CARGO_HANDLING))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            sys.joinTables(TBL_CARGO_PLACES, loadAlias, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            sys.joinTables(TBL_CARGO_PLACES, unlAlias, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFromLeft(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_CARGO, TBL_ORDER_CARGO, COL_CARGO_ID));

    query.addFields(TBL_ORDERS, COL_STATUS, COL_ORDER_DATE, COL_ORDER_NO, COL_CUSTOMER,
        COL_ORDER_MANAGER);
    query.addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER_NAME);

    query.addFields(TBL_ORDER_CARGO, COL_ORDER, COL_CARGO_ID, COL_CARGO_TYPE,
        COL_CARGO_DESCRIPTION, COL_CARGO_NOTES);

    query.addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE));
    query.addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY));
    query.addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS));
    query.addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX));
    query.addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY));
    query.addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER));

    query.addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE));
    query.addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY));
    query.addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS));
    query.addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX));
    query.addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY));
    query.addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER));

    Set<Integer> statuses = Sets.newHashSet(OrderStatus.REQUEST.ordinal(),
        OrderStatus.ACTIVE.ordinal());
    IsCondition cargoWhere = SqlUtils.and(SqlUtils.inList(TBL_ORDERS, COL_STATUS, statuses),
        SqlUtils.isNull(TBL_CARGO_TRIPS, COL_CARGO));

    query.setWhere(cargoWhere);

    query.addOrder(TBL_COMPANIES, COL_COMPANY_NAME);
    query.addOrder(TBL_ORDERS, COL_ORDER_DATE, COL_ORDER_NO);
    query.addOrder(loadAlias, COL_PLACE_DATE);
    query.addOrder(unlAlias, COL_PLACE_DATE);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_ORDER_CARGO, data.serialize());

    IsCondition cargoHandlingWhere = SqlUtils.or(SqlUtils.notNull(loadAlias, COL_PLACE_DATE),
        SqlUtils.notNull(unlAlias, COL_PLACE_DATE));

    SqlSelect cargoHandlingQuery = new SqlSelect()
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_CARGO, TBL_ORDER_CARGO, COL_CARGO_ID))
        .addFromInner(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_HANDLING, COL_CARGO))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            sys.joinTables(TBL_CARGO_PLACES, loadAlias, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            sys.joinTables(TBL_CARGO_PLACES, unlAlias, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFields(TBL_CARGO_HANDLING, COL_CARGO, COL_CARGO_HANDLING_NOTES)
        .addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE))
        .addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY))
        .addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER))
        .addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE))
        .addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY))
        .addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER))
        .setWhere(SqlUtils.and(cargoWhere, cargoHandlingWhere))
        .addOrder(TBL_CARGO_HANDLING, COL_CARGO);

    SimpleRowSet cargoHandling = qs.getData(cargoHandlingQuery);
    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }

    return ResponseObject.response(settings);
  }

  private BeeRowSet getSettings() {
    long userId = usr.getCurrentUserId();
    Filter filter = Filter.equals(COL_USER, userId);

    BeeRowSet rowSet = qs.getViewData(VIEW_TRANSPORT_SETTINGS, filter);
    if (!DataUtils.isEmpty(rowSet)) {
      return rowSet;
    }

    SqlInsert sqlInsert = new SqlInsert(TBL_TRANSPORT_SETTINGS)
        .addConstant(COL_USER, userId);

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return null;
    }

    return qs.getViewData(VIEW_TRANSPORT_SETTINGS, filter);
  }

  private List<Color> getThemeColors(Long theme) {
    List<Color> result = new ArrayList<>();

    BeeRowSet rowSet;
    if (theme != null) {
      rowSet = qs.getViewData(VIEW_THEME_COLORS, Filter.equals(COL_THEME, theme));
    } else {
      rowSet = null;
    }

    if (DataUtils.isEmpty(rowSet)) {
      rowSet = qs.getViewData(VIEW_COLORS);
      if (DataUtils.isEmpty(rowSet)) {
        return result;
      }
    }

    int bgIndex = rowSet.getColumnIndex(COL_BACKGROUND);
    int fgIndex = rowSet.getColumnIndex(COL_FOREGROUND);

    for (BeeRow row : rowSet.getRows()) {
      String bg = row.getString(bgIndex);
      String fg = row.getString(fgIndex);

      if (!BeeUtils.isEmpty(bg)) {
        result.add(new Color(row.getId(), bg.trim(), BeeUtils.trim(fg)));
      }
    }
    return result;
  }

  private ResponseObject getTripBeforeData(long vehicle, Long date) {
    Pair<String, String> pair = Pair.empty();

    if (Objects.nonNull(date)) {
      SimpleRow row = qs.getRow(new SqlSelect()
          .addField(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP)
          .addFields(TBL_TRIPS, COL_SPEEDOMETER_BEFORE, COL_SPEEDOMETER_AFTER, COL_FUEL_BEFORE,
              COL_FUEL_AFTER)
          .addFields(TBL_VEHICLES, COL_SPEEDOMETER)
          .addFrom(TBL_TRIPS)
          .addFromInner(TBL_VEHICLES,
              SqlUtils.and(sys.joinTables(TBL_VEHICLES, TBL_TRIPS, COL_VEHICLE),
                  sys.idEquals(TBL_VEHICLES, vehicle)))
          .setWhere(SqlUtils.less(TBL_TRIPS, COL_DATE, date))
          .addOrderDesc(TBL_TRIPS, COL_DATE)
          .setLimit(1));

      if (Objects.nonNull(row)) {
        Long tripId = row.getLong(COL_TRIP);
        Double speedometer = row.getDouble(COL_SPEEDOMETER_AFTER);
        Double fuel = row.getDouble(COL_FUEL_AFTER);

        if (Objects.isNull(speedometer)) {
          Double km = qs.getDouble(new SqlSelect()
              .addSum(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS)
              .addFrom(TBL_TRIP_ROUTES)
              .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId)));

          speedometer = BeeUtils.unbox(row.getDouble(COL_SPEEDOMETER_BEFORE)) + BeeUtils.unbox(km);
          Integer scale = row.getInt(COL_SPEEDOMETER);

          if (BeeUtils.isPositive(scale) && scale < speedometer) {
            speedometer -= scale;
          }
        }
        if (Objects.isNull(fuel)) {
          Double fill = qs.getDouble(new SqlSelect()
              .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
              .addFrom(TBL_TRIP_FUEL_COSTS)
              .setWhere(SqlUtils.equals(TBL_TRIP_FUEL_COSTS, COL_TRIP, tripId)));

          SimpleRow cons = qs.getRow(rep.getFuelConsumptionsQuery(new SqlSelect()
                  .addFields(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES))
                  .addFrom(TBL_TRIP_ROUTES)
                  .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId)),
              false));

          Double consume = Objects.isNull(cons) ? null : cons.getDouble(COL_COSTS_QUANTITY);

          Double addit = qs.getDouble(new SqlSelect()
              .addSum(TBL_TRIP_FUEL_CONSUMPTIONS, COL_COSTS_QUANTITY)
              .addFrom(TBL_TRIP_FUEL_CONSUMPTIONS)
              .setWhere(SqlUtils.equals(TBL_TRIP_FUEL_CONSUMPTIONS, COL_TRIP, tripId)));

          fuel = BeeUtils.unbox(row.getDouble(COL_FUEL_BEFORE)) + BeeUtils.unbox(fill)
              - BeeUtils.unbox(consume) - BeeUtils.unbox(addit);
        }
        pair.setA(BeeUtils.toString(speedometer));
        pair.setB(BeeUtils.toString(fuel));
      }
    }
    return ResponseObject.response(pair);
  }

  private SimpleRowSet getTripDrivers(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFrom(TBL_TRIP_DRIVERS)
        .addFromLeft(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_TRIP_DRIVERS, COL_TRIP))
        .addFromLeft(TBL_DRIVERS, sys.joinTables(TBL_DRIVERS, TBL_TRIP_DRIVERS, COL_DRIVER))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_DRIVERS, COL_DRIVER_PERSON))
        .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

    query.addFields(TBL_TRIP_DRIVERS, COL_TRIP, COL_DRIVER,
        COL_TRIP_DRIVER_FROM, COL_TRIP_DRIVER_TO, COL_TRIP_DRIVER_NOTE);
    query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

    if (condition != null) {
      query.setWhere(condition);
    }

    return qs.getData(query);
  }

  private SqlSelect getTripQuery(IsCondition where) {
    String truckJoinAlias = "truck_" + SqlUtils.uniqueName();
    String trailerJoinAlias = "trail_" + SqlUtils.uniqueName();

    return new SqlSelect().addFrom(TBL_TRIPS)
        .addFromLeft(TBL_VEHICLES, truckJoinAlias,
            SqlUtils.join(truckJoinAlias, COL_VEHICLE_ID, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLES, trailerJoinAlias,
            SqlUtils.join(trailerJoinAlias, COL_VEHICLE_ID, TBL_TRIPS, COL_TRAILER))
        .addFields(TBL_TRIPS, COL_TRIP_ID, COL_TRIP_NO, COL_VEHICLE, COL_TRAILER,
            COL_TRIP_DATE, COL_TRIP_PLANNED_END_DATE, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO,
            COL_TRIP_STATUS, COL_TRIP_NOTES)
        .addField(TBL_TRIPS, sys.getVersionName(TBL_TRIPS), ALS_TRIP_VERSION)
        .addField(truckJoinAlias, COL_NUMBER, ALS_VEHICLE_NUMBER)
        .addField(trailerJoinAlias, COL_NUMBER, ALS_TRAILER_NUMBER)
        .setWhere(where);
  }

  private ResponseObject getUnassignedCargos(RequestInfo reqInfo) {
    long orderId = BeeUtils.toLong(reqInfo.getParameter(COL_ORDER));

    SqlSelect query = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_CARGO_TRIPS, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDER_CARGO, COL_ORDER, orderId),
            SqlUtils.isNull(TBL_CARGO_TRIPS, COL_CARGO)));

    return ResponseObject.response(qs.getColumn(query));
  }

  private ResponseObject getVehicleBusyDates(Long vehicle, Long trailer, Long from, Long to) {
    SqlSelect select = null;

    for (Pair<String, Long> pair : Arrays.asList(Pair.of(COL_VEHICLE, vehicle),
        Pair.of(COL_TRAILER, trailer))) {

      if (DataUtils.isId(pair.getB())) {
        SqlSelect query = new SqlSelect()
            .addFields(TBL_VEHICLES, COL_VEHICLE_NUMBER)
            .addField(TBL_TRIPS, COL_TRIP_NO, COL_NOTES)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_FROM),
                SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE)), COL_ABSENCE_FROM)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.field(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)), COL_ABSENCE_TO)
            .addFrom(TBL_TRIPS)
            .addFromInner(TBL_VEHICLES,
                SqlUtils.and(sys.joinTables(TBL_VEHICLES, TBL_TRIPS, pair.getA()),
                    sys.idEquals(TBL_VEHICLES, pair.getB())))
            .setWhere(SqlUtils.or(
                SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.isNull(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)),
                SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.more(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE, from)),
                SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.more(TBL_TRIPS, COL_TRIP_DATE_TO, from))));

        if (Objects.nonNull(to)) {
          query.setWhere(SqlUtils.and(query.getWhere(),
              SqlUtils.or(SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
                  SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE, to)),
                  SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
                      SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE_FROM, to)))));
        }
        if (Objects.isNull(select)) {
          select = query.setUnionAllMode(false);
        } else {
          select.addUnion(query);
        }
        select.addUnion(new SqlSelect()
            .addFields(TBL_VEHICLES, COL_VEHICLE_NUMBER)
            .addExpr(SqlUtils
                .concat(SqlUtils.field(TBL_VEHICLE_SERVICE_TYPES, COL_VEHICLE_SERVICE_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_NOTES),
                        "''")), COL_NOTES)
            .addField(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE, COL_ABSENCE_FROM)
            .addField(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE_TO, COL_ABSENCE_TO)
            .addFrom(TBL_VEHICLE_SERVICES)
            .addFromInner(TBL_VEHICLES,
                SqlUtils.and(sys.joinTables(TBL_VEHICLES, TBL_VEHICLE_SERVICES, COL_VEHICLE),
                    sys.idEquals(TBL_VEHICLES, pair.getB())))
            .addFromInner(TBL_VEHICLE_SERVICE_TYPES,
                sys.joinTables(TBL_VEHICLE_SERVICE_TYPES, TBL_VEHICLE_SERVICES,
                    COL_VEHICLE_SERVICE_TYPE))
            .setWhere(SqlUtils.and(
                SqlUtils.or(SqlUtils.isNull(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE_TO),
                    SqlUtils.more(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE_TO, from)),
                Objects.nonNull(to)
                    ? SqlUtils.less(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE, to) : null)));
      }
    }
    List<String> messages = new ArrayList<>();
    Dictionary loc = usr.getDictionary();

    for (SimpleRow row : qs.getData(select
        .addOrder(null, COL_VEHICLE_NUMBER, COL_ABSENCE_FROM, COL_ABSENCE_TO))) {

      messages.add(BeeUtils.joinWords(row.getValue(COL_VEHICLE_NUMBER),
          loc.dateFromShort(), row.getDate(COL_ABSENCE_FROM),
          Objects.isNull(row.getDate(COL_ABSENCE_TO))
              ? null : loc.dateToShort() + " " + row.getDate(COL_ABSENCE_TO),
          row.getValue(COL_NOTES)));
    }
    return ResponseObject.response(messages);
  }

  private Multimap<Long, Long> getVehicleGroups(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_VEHICLE_GROUPS, COL_VEHICLE, COL_GROUP)
        .addFrom(TBL_VEHICLE_GROUPS)
        .setWhere(condition);

    SimpleRowSet data = qs.getData(query);

    Multimap<Long, Long> result = HashMultimap.create();
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_VEHICLE), row.getLong(COL_GROUP));
      }
    }

    return result;
  }

  private Map<Long, Long> getVehicleManagers(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFrom(TBL_VEHICLE_GROUPS)
        .addFromInner(TBL_TRANSPORT_GROUPS,
            sys.joinTables(TBL_TRANSPORT_GROUPS, TBL_VEHICLE_GROUPS, COL_GROUP))
        .addFields(TBL_VEHICLE_GROUPS, COL_VEHICLE)
        .addMax(TBL_TRANSPORT_GROUPS, COL_GROUP_MANAGER)
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_TRANSPORT_GROUPS, COL_GROUP_MANAGER),
            condition))
        .addGroup(TBL_VEHICLE_GROUPS, COL_VEHICLE)
        .setHaving(SqlUtils.equals(SqlUtils.aggregate(SqlFunction.COUNT_DISTINCT,
            SqlUtils.field(TBL_TRANSPORT_GROUPS, COL_GROUP_MANAGER)), 1));

    SimpleRowSet data = qs.getData(query);

    Map<Long, Long> result = new HashMap<>();
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_VEHICLE), row.getLong(COL_GROUP_MANAGER));
      }
    }

    return result;
  }

  private SimpleRowSet getVehicleServices(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFrom(TBL_VEHICLE_SERVICES)
        .addFromLeft(TBL_VEHICLES,
            sys.joinTables(TBL_VEHICLES, TBL_VEHICLE_SERVICES, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLE_SERVICE_TYPES,
            sys.joinTables(TBL_VEHICLE_SERVICE_TYPES, TBL_VEHICLE_SERVICES,
                COL_VEHICLE_SERVICE_TYPE));

    query.addFields(TBL_VEHICLE_SERVICES, COL_VEHICLE, COL_VEHICLE_SERVICE_DATE,
        COL_VEHICLE_SERVICE_DATE_TO, COL_VEHICLE_SERVICE_NOTES);
    query.addFields(TBL_VEHICLES, COL_NUMBER);
    query.addFields(TBL_VEHICLE_SERVICE_TYPES, COL_VEHICLE_SERVICE_NAME);

    query.addOrder(TBL_VEHICLE_SERVICES, COL_VEHICLE, COL_VEHICLE_SERVICE_DATE);

    if (condition != null) {
      query.setWhere(condition);
    }

    return qs.getData(query);
  }

  private ResponseObject getVehicleTbData(String svc, Filter vehicleFilter,
      VehicleType vehicleType, String themeColumnName) {

    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    Long theme = settings.getLong(0, settings.getColumnIndex(themeColumnName));
    List<Color> colors = getThemeColors(theme);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet transportGroups = qs.getViewData(VIEW_TRANSPORT_GROUPS);
    if (!DataUtils.isEmpty(transportGroups)) {
      settings.setTableProperty(PROP_TRANSPORT_GROUPS, transportGroups.serialize());
    }

    BeeRowSet cargoTypes = qs.getViewData(VIEW_CARGO_TYPES);
    if (!DataUtils.isEmpty(cargoTypes)) {
      settings.setTableProperty(PROP_CARGO_TYPES, cargoTypes.serialize());
    }

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    BeeRowSet cities = qs.getViewData(VIEW_CITIES, Filter.any(COL_COUNTRY, countries.getRowIds()));
    settings.setTableProperty(PROP_CITIES, cities.serialize());

    Order vehicleOrder = new Order(COL_NUMBER, true);
    BeeRowSet vehicles = qs.getViewData(VIEW_VEHICLES, vehicleFilter, vehicleOrder);
    if (DataUtils.isEmpty(vehicles)) {
      logger.warning(svc, "vehicles not available");
      return ResponseObject.response(settings);
    }

    List<Long> vehicleIds = DataUtils.getRowIds(vehicles);
    IsCondition groupWhere = SqlUtils.inList(TBL_VEHICLE_GROUPS, COL_VEHICLE, vehicleIds);

    Multimap<Long, Long> vehicleGroups = getVehicleGroups(groupWhere);
    if (!vehicleGroups.isEmpty()) {
      for (BeeRow row : vehicles) {
        if (vehicleGroups.containsKey(row.getId())) {
          row.setProperty(PROP_VEHICLE_GROUPS,
              DataUtils.buildIdList(vehicleGroups.get(row.getId())));
        }
      }
    }

    Map<Long, Long> vehicleManagers = getVehicleManagers(groupWhere);
    if (!BeeUtils.isEmpty(vehicleManagers)) {
      for (BeeRow row : vehicles) {
        Long manager = vehicleManagers.get(row.getId());
        if (manager != null) {
          row.setProperty(PROP_VEHICLE_MANAGER, BeeUtils.toString(manager));
        }
      }
    }

    settings.setTableProperty(PROP_VEHICLES, vehicles.serialize());

    SimpleRowSet vehicleServices = getVehicleServices(SqlUtils.inList(TBL_VEHICLE_SERVICES,
        COL_VEHICLE, vehicleIds));
    if (!DataUtils.isEmpty(vehicleServices)) {
      settings.setTableProperty(PROP_VEHICLE_SERVICES, vehicleServices.serialize());
    }

    IsCondition tripWhere = SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION),
        SqlUtils.inList(TBL_TRIPS, vehicleType.getTripVehicleIdColumnName(), vehicleIds));

    SqlSelect tripQuery = getTripQuery(tripWhere);
    tripQuery.addOrder(TBL_TRIPS, vehicleType.getTripVehicleIdColumnName(), COL_TRIP_DATE);

    SimpleRowSet trips = qs.getData(tripQuery);
    if (DataUtils.isEmpty(trips)) {
      logger.warning(svc, "trips not available");
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_TRIPS, trips.serialize());

    SimpleRowSet drivers = getTripDrivers(tripWhere);
    if (!DataUtils.isEmpty(drivers)) {
      settings.setTableProperty(PROP_TRIP_DRIVERS, drivers.serialize());
    }

    SqlSelect freightQuery = getFreightQuery(tripWhere);

    SimpleRowSet freights = qs.getData(freightQuery);
    if (DataUtils.isEmpty(freights)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_FREIGHTS, freights.serialize());

    SqlSelect cargoHandlingQuery = getFreightHandlingQuery(tripWhere);

    SimpleRowSet cargoHandling = qs.getData(cargoHandlingQuery);
    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }

    return ResponseObject.response(settings);
  }

  private ResponseObject sendMessage(String message, String[] recipients) {
    String address = prm.getText("SmsServiceAddress");

    if (BeeUtils.isEmpty(address)) {
      return ResponseObject.error("SmsServiceAddress is empty");
    }
    StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")
        .append("<sms-send>")
        .append("<authentication>")
        .append(XmlUtils.tag("username", prm.getText("SmsUserName")))
        .append(XmlUtils.tag("password", prm.getText("SmsPassword")))
        .append(XmlUtils.tag("serviceId", prm.getText("SmsServiceId")))
        .append("</authentication>")
        .append("<originator>")
        .append(XmlUtils.tag("source", prm.getText("SmsDisplayText")))
        .append("</originator>")
        .append("<sms-messages>");

    for (String phone : recipients) {
      xml.append("<sms>")
          .append(XmlUtils.tag("destination", phone))
          .append(XmlUtils.tag("msg", message))
          .append(XmlUtils.tag("dr", true))
          .append(XmlUtils.tag("id", 0))
          .append(XmlUtils.tag("sendTime", new DateTime().toString()))
          .append("</sms>");
    }
    xml.append("</sms-messages>")
        .append("</sms-send>");

    ResponseObject response = ResponseObject.info(Localized.dictionary().messageSent());
    BufferedWriter wr = null;
    BufferedReader in = null;
    HttpURLConnection conn = null;

    try {
      conn = (HttpURLConnection) new URL(address).openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);

      wr = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(),
          BeeConst.CHARSET_UTF8));
      wr.write(xml.toString());
      wr.close();

      if (conn.getResponseCode() != HttpServletResponse.SC_OK) {
        response = ResponseObject.error(Localized.dictionary().error(), conn.getResponseCode(),
            conn.getResponseMessage());
      } else {
        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String input = in.readLine();

        while (input != null) {
          sb.append(input);
          input = in.readLine();
        }
        in.close();
        input = sb.toString();
        String status = XmlUtils.getText(input, "status");

        if (!BeeUtils.same(status, "OK")) {
          response = ResponseObject.error(input);
        }
      }
    } catch (IOException e) {
      try {
        if (wr != null) {
          wr.close();
        }
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {
        logger.error(ex);
      }
      response = ResponseObject.error(e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return response;
  }
}
