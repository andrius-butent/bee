package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_DEPARTMENT;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.ALS_COMPANY_TYPE_NAME;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.COL_COMMENT;
import static com.butent.bee.shared.modules.tasks.TaskConstants.COL_EVENT_NOTE;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.modules.classifiers.CompanyTypeReport;
import com.butent.bee.client.modules.classifiers.CompanyUsageReport;
import com.butent.bee.client.modules.trade.acts.TradeActItemsByCompanyReport;
import com.butent.bee.client.modules.trade.acts.TradeActServicesReport;
import com.butent.bee.client.modules.trade.acts.TradeActStockReport;
import com.butent.bee.client.modules.trade.acts.TradeActTransferReport;
import com.butent.bee.client.modules.trade.reports.TradeMovementOfGoodsReport;
import com.butent.bee.client.modules.trade.reports.TradeStockReport;
import com.butent.bee.client.modules.transport.AssessmentQuantityReport;
import com.butent.bee.client.modules.transport.AssessmentTurnoverReport;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.interceptor.ExtendedReportInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.finance.FinanceConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectPriority;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.report.DateTimeFunction;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum Report implements HasWidgetSupplier {
  COMPANY_TYPES(ModuleAndSub.of(Module.CLASSIFIERS), "CompanyTypes", "CompanyRelationTypeReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new CompanyTypeReport();
    }
  },
  COMPANY_USAGE(ModuleAndSub.of(Module.CLASSIFIERS), "CompanyUsage", "CompanyUsageReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new CompanyUsageReport();
    }
  },
  COMPANY_SOURCE(ModuleAndSub.of(Module.CLASSIFIERS), SVC_COMPANY_SOURCE_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();

      return Arrays.asList(
          new ReportTextItem(COL_COMPANY_NAME, loc.name()),
          new ReportTextItem(COL_COMPANY_TYPE, loc.companyStatus()),
          new ReportTextItem(COL_COMPANY_CODE, loc.code()),
          new ReportTextItem(COL_COMPANY_INFORMATION_SOURCE, loc.informationSource()),
          new ReportTextItem(COL_COMPANY_SIZE, loc.companySize()),
          new ReportTextItem(COL_COMPANY_TURNOVER, loc.trdTurnover()),
          new ReportTextItem(COL_COMPANY_GROUP, loc.clientGroup()),
          new ReportTextItem(COL_COMPANY_RELATION_TYPE_STATE, loc.companyRelationState()));
    }

    @Override
    public String getReportCaption() {
      return Localized.dictionary().contactReportCompanySource();
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();
      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      for (String item : new String[]{
          COL_COMPANY_TYPE,
          COL_COMPANY_CODE,
          COL_COMPANY_INFORMATION_SOURCE,
          COL_COMPANY_SIZE,
          COL_COMPANY_TURNOVER,
          COL_COMPANY_GROUP,
          COL_COMPANY_RELATION_TYPE_STATE}) {
        report.addColItem(items.get(item));
      }

      report.addRowItem(items.get(COL_COMPANY_NAME));

      return Collections.singletonList(report);
    }
  },

  ASSESSMENT_QUANTITY(ModuleAndSub.of(Module.TRANSPORT), "AssessmentQuantity",
      "AssessmentQuantityReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new AssessmentQuantityReport();
    }
  },
  ASSESSMENT_TURNOVER(ModuleAndSub.of(Module.TRANSPORT), "AssessmentTurnover",
      "AssessmentTurnoverReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new AssessmentTurnoverReport();
    }
  },

  TRADE_STOCK(ModuleAndSub.of(Module.TRADE), "TradeStock", "TradeStockReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeStockReport();
    }
  },
  TRADE_MOVEMENT_OF_GOODS(ModuleAndSub.of(Module.TRADE), "TradeMovementOfGoods",
      "TradeMovementOfGoodsReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeMovementOfGoodsReport();
    }
  },

  TRADE_ACT_ITEMS_BY_COMPANY(ModuleAndSub.of(Module.TRADE, SubModule.ACTS),
      "TradeActItemsByCompany", "TradeActItemsByCompanyReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActItemsByCompanyReport();
    }
  },
  TRADE_ACT_STOCK(ModuleAndSub.of(Module.TRADE, SubModule.ACTS), "TradeActStock",
      "TradeActStockReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActStockReport();
    }
  },
  TRADE_ACT_SERVICES(ModuleAndSub.of(Module.TRADE, SubModule.ACTS), "TradeActServices",
      "TradeActServicesReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActServicesReport();
    }
  },
  TRADE_ACT_TRANSFER(ModuleAndSub.of(Module.TRADE, SubModule.ACTS), "TradeActTransfer",
      "TradeActTransferReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActTransferReport();
    }
  },

  TRANSPORT_TRIP_PROFIT(ModuleAndSub.of(Module.TRANSPORT), SVC_TRIP_PROFIT_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();
      String plan = BeeUtils.parenthesize(loc.plan());

      return Arrays.asList(
          new ReportTextItem(COL_TRIP, Data.getColumnLabel(TBL_TRIP_COSTS, COL_TRIP)),
          new ReportTextItem(COL_TRIP_NO, Data.getColumnLabel(TBL_TRIPS, COL_TRIP_NO)),
          new ReportDateTimeItem(COL_TRIP_DATE, loc.date()),
          new ReportDateItem(COL_TRIP_DATE_FROM, loc.dateFrom()),
          new ReportDateItem(COL_TRIP_DATE_TO, loc.dateTo()),
          new ReportTextItem(COL_VEHICLE, Data.getColumnLabel(TBL_TRIPS, COL_VEHICLE)),
          new ReportTextItem(COL_TRAILER, Data.getColumnLabel(TBL_TRIPS, COL_TRAILER)),
          new ReportTextItem(COL_TRIP_ROUTE, loc.route()),
          new ReportEnumItem(COL_TRIP_STATUS, Data.getColumnLabel(TBL_TRIPS, COL_TRIP_STATUS),
              TripStatus.class),
          new ReportTextItem(ALS_TRIP_MANAGER, loc.tripManager()),
          new ReportTextItem(COL_MAIN_DRIVER, loc.trdDriver()),

          new ReportTextItem(COL_ORDER_NO, loc.orderNumber()),
          new ReportDateTimeItem(TransportConstants.COL_ORDER + COL_ORDER_DATE, loc.orderDate()),
          new ReportTextItem(COL_CUSTOMER, loc.customer()),
          new ReportTextItem(COL_ORDER_MANAGER, loc.manager()),
          new ReportTextItem(COL_CARGO, loc.cargo()),
          new ReportBooleanItem(COL_CARGO_PARTIAL, loc.partial()),

          new ReportNumericItem(COL_ROUTE_KILOMETERS, loc.kilometers()),
          new ReportNumericItem("TripIncome", loc.incomes()).setPrecision(2),
          new ReportNumericItem("CargoIncome", loc.trCargoIncomes()).setPrecision(2),
          new ReportNumericItem("FuelCosts", loc.trFuelCosts()).setPrecision(2),
          new ReportNumericItem("DailyCosts", loc.trDailyCosts()).setPrecision(2),
          new ReportNumericItem("RoadCosts", loc.trRoadCosts()).setPrecision(2),
          new ReportNumericItem("OtherCosts", loc.trOtherCosts()).setPrecision(2),
          new ReportNumericItem("ConstantCosts", loc.trConstantCosts()).setPrecision(2),
          new ReportNumericItem("CargoCosts", loc.trCargoCosts()).setPrecision(2),

          new ReportNumericItem("Planned" + COL_ROUTE_KILOMETERS,
              BeeUtils.joinWords(loc.kilometers(), plan)),
          new ReportNumericItem("PlannedFuelCosts", BeeUtils.joinWords(loc.trFuelCosts(), plan))
              .setPrecision(2),
          new ReportNumericItem("PlannedDailyCosts", BeeUtils.joinWords(loc.trDailyCosts(), plan))
              .setPrecision(2),
          new ReportNumericItem("PlannedRoadCosts", BeeUtils.joinWords(loc.trRoadCosts(), plan))
              .setPrecision(2),
          new ReportEnumItem(TransportConstants.ALS_ORDER_STATUS,
              Data.getColumnLabel(TBL_ORDERS, TransportConstants.COL_STATUS), OrderStatus.class));
    }

    @Override
    public String getReportCaption() {
      return Localized.dictionary().trReportProfitability();
    }

    @Override
    public LinkedHashMap<String, Editor> getReportParams() {
      LinkedHashMap<String, Editor> params = new LinkedHashMap<>();
      params.put(COL_CURRENCY, getCurrencyEditor());
      params.put(COL_TRADE_VAT, getWoVatEditor());
      return params;
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();

      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      Collection<ReportInfo> reports = new ArrayList<>();
      reports.add(getTripProfit(items));
      reports.add(getOrderProfit(items));
      reports.add(getCustomerProfitability(items));

      return reports;
    }

    private ReportInfo getCustomerProfitability(Map<String, ReportItem> items) {
      ReportInfo report = new ReportInfo(Localized.dictionary().trReportCustomerProfit());

      Stream.of(ALS_ORDER_DATE, COL_ORDER_NO, COL_TRIP_MANAGER, COL_TRIP_ROUTE)
          .forEach(item -> report.addRowItem(items.get(item)));

      report.setRowGrouping(items.get(COL_CUSTOMER));
      report.addColItem(items.get(COL_ROUTE_KILOMETERS));
      createProfit(report, items, false);
      report.getFilterItems().add(items.get(COL_TRIP_STATUS)
          .setFilter(BeeUtils.toString(TripStatus.COMPLETED.ordinal())));

      return report;
    }

    private ReportInfo getOrderProfit(Map<String, ReportItem> items) {
      ReportInfo report = new ReportInfo(Localized.dictionary().trReportOrderProfit());
      Stream.of(ALS_ORDER_DATE, COL_TRIP_STATUS, COL_CUSTOMER, COL_TRIP_MANAGER, COL_TRIP_ROUTE)
          .forEach(item -> report.addRowItem(items.get(item)));

      report.setRowGrouping(items.get(COL_ORDER_NO));
      Stream.of(COL_TRIP_NO, ALS_TRIP_MANAGER, COL_ROUTE_KILOMETERS)
          .forEach(item -> report.addColItem(items.get(item)));

      createProfit(report, items, false);
      report.getFilterItems().add(items.get(COL_TRIP_STATUS)
          .setFilter(BeeUtils.toString(TripStatus.COMPLETED.ordinal())));

      return report;
    }

    private ReportInfo getTripProfit(Map<String, ReportItem> items) {
      ReportInfo report = new ReportInfo(Localized.dictionary().trReportTripProfit());
      Stream.of(COL_TRIP_NO, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_CUSTOMER, COL_MAIN_DRIVER,
          COL_TRAILER)
          .forEach(item -> report.addRowItem(items.get(item)));

      report.setRowGrouping(items.get(COL_VEHICLE));
      report.addColItem(items.get(COL_ROUTE_KILOMETERS));
      createProfit(report, items, true);
      report.getFilterItems().add(items.get(COL_TRIP_STATUS)
          .setFilter(BeeUtils.toString(TripStatus.COMPLETED.ordinal())));

      return report;
    }

    private void createProfit(ReportInfo report, Map<String, ReportItem> items, boolean expand) {
      ReportFormulaItem incomes = (ReportFormulaItem) new ReportFormulaItem(
          Localized.dictionary().incomes()).setPrecision(2);
      ReportFormulaItem costs = (ReportFormulaItem) new ReportFormulaItem(
          Localized.dictionary().expenses()).setPrecision(2);
      ReportFormulaItem profit = (ReportFormulaItem) new ReportFormulaItem(
          Localized.dictionary().profit()).setPrecision(2);

      Stream.of("TripIncome", "CargoIncome")
          .forEach(item -> {
            if (expand) {
              report.addColItem(items.get(item));
              profit.plus(new ReportResultItem(items.get(item)));
            } else {
              incomes.plus(items.get(item));
            }
          });
      Stream.of("FuelCosts", "DailyCosts", "RoadCosts", "OtherCosts", "ConstantCosts", "CargoCosts")
          .forEach(item -> {
            if (expand) {
              report.addColItem(items.get(item));
              profit.minus(new ReportResultItem(items.get(item)));
            } else {
              costs.plus(items.get(item));
            }
          });
      if (!expand) {
        report.addColItem(incomes);
        report.addColItem(costs);
        profit.plus(new ReportResultItem(incomes)).minus(new ReportResultItem(costs));
      }
      report.addColItem(profit);
    }
  },

  TRANSPORT_FUEL_USAGE(ModuleAndSub.of(Module.TRANSPORT), SVC_FUEL_USAGE_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();

      return Arrays.asList(
          new ReportTextItem(COL_TRIP, Data.getColumnLabel(TBL_TRIP_COSTS, COL_TRIP)),
          new ReportTextItem(COL_TRIP_NO, Data.getColumnLabel(TBL_TRIPS, COL_TRIP_NO)),
          new ReportDateTimeItem(COL_TRIP_DATE, loc.date()),
          new ReportDateItem(COL_TRIP_DATE_FROM, loc.dateFrom()),
          new ReportDateItem(COL_TRIP_DATE_TO, loc.dateTo()),
          new ReportTextItem(COL_VEHICLE, Data.getColumnLabel(TBL_TRIPS, COL_VEHICLE)),
          new ReportTextItem(COL_TRAILER, Data.getColumnLabel(TBL_TRIPS, COL_TRAILER)),
          new ReportTextItem(COL_DRIVER, loc.vehicleDriver()),
          new ReportEnumItem(COL_TRIP_STATUS, Data.getColumnLabel(TBL_TRIPS, COL_TRIP_STATUS),
              TripStatus.class),
          new ReportTextItem(COL_CARGO, loc.cargo()),
          new ReportTextItem(COL_TRIP_ROUTE, loc.route()),

          new ReportNumericItem(COL_ROUTE_KILOMETERS, loc.kilometers()).setPrecision(1),
          new ReportNumericItem(COL_EMPTY_KILOMETERS, loc.trEmptyKilometers()),
          new ReportNumericItem(COL_ROUTE_WEIGHT, loc.trWeightInTons()).setPrecision(2),
          new ReportNumericItem(COL_ROUTE_CONSUMPTION, loc.trFuelConsumptions()).setPrecision(2),
          new ReportNumericItem("Norm" + COL_ROUTE_CONSUMPTION,
              BeeUtils.joinWords(loc.trFuelConsumptions(), BeeUtils.parenthesize(loc.plan())))
              .setPrecision(2),
          new ReportNumericItem(COL_COSTS_PRICE, loc.price()).setPrecision(2));
    }

    @Override
    public String getReportCaption() {
      return Localized.dictionary().trReportFuelUsage();
    }

    @Override
    public LinkedHashMap<String, Editor> getReportParams() {
      LinkedHashMap<String, Editor> params = new LinkedHashMap<>();
      params.put(COL_CURRENCY, getCurrencyEditor());
      params.put(COL_TRADE_VAT, getWoVatEditor());
      return params;
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();

      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      report.setRowGrouping(items.get(COL_DRIVER));

      for (String item : new String[] {
          COL_TRIP_NO, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_VEHICLE}) {
        report.addRowItem(items.get(item));
      }
      for (String item : new String[] {
          COL_ROUTE_KILOMETERS, COL_ROUTE_WEIGHT, COL_ROUTE_CONSUMPTION,
          "Norm" + COL_ROUTE_CONSUMPTION}) {
        report.addColItem(items.get(item));
      }
      for (int i = 0; i < report.getColItems().size(); i++) {
        report.setGroupSummary(i, false);
        report.setColSummary(i, false);
      }
      report.addColItem(new ReportFormulaItem(Localized.dictionary().debt() + " / -"
          + Localized.dictionary().overpayment())
          .plus(new ReportFormulaItem(null)
              .plus(items.get("Norm" + COL_ROUTE_CONSUMPTION))
              .minus(items.get(COL_ROUTE_CONSUMPTION)).setPrecision(2))
          .multiply(items.get(COL_COSTS_PRICE)).setPrecision(2));

      return Collections.singletonList(report);
    }
  },

  TRANSPORT_TRIP_COSTS_REPORT(ModuleAndSub.of(Module.TRANSPORT), SVC_TRIP_COSTS_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();

      return Arrays.asList(
          new ReportTextItem(COL_TRIP_NO, loc.trTripNo()),
          new ReportDateItem(COL_DATE_FROM, loc.trTripDateFrom()),
          new ReportDateItem(COL_DATE_TO, loc.trTripDateTo()),
          new ReportTextItem(ALS_VEHICLE_NUMBER, loc.trTruck()),
          new ReportTextItem(ALS_TRAILER_NUMBER, loc.trailer()),
          new ReportEnumItem(TaskConstants.COL_STATUS, loc.status(), TripStatus.class),
          new ReportTextItem(TradeConstants.COL_TRADE_MANAGER, loc.manager()),
          new ReportTextItem(COL_MAIN_DRIVER,
              BeeUtils.joinWords(loc.mailDefault(), loc.trdDriver())),
          new ReportNumericItem(COL_SPEEDOMETER_BEFORE, loc.trSpeedometerFrom()),
          new ReportNumericItem(COL_SPEEDOMETER_AFTER, loc.trSpeedometerAfter()),
          new ReportNumericItem(COL_FUEL_BEFORE, loc.trFuelBalanceBefore()).setPrecision(3),
          new ReportNumericItem(COL_FUEL_AFTER, loc.trFuelBalanceAfter()).setPrecision(3),
          new ReportTextItem(COL_NOTES, loc.trTripNotes()),
          new ReportTextItem(COL_ITEM, loc.productService()),
          new ReportNumericItem(TradeConstants.COL_TRADE_ITEM_QUANTITY,
              loc.quantity()).setPrecision(2),
          new ReportNumericItem(COL_ITEM_PRICE, loc.price()).setPrecision(2),
          new ReportNumericItem(VAR_TOTAL, loc.printDocumentAmount())
              .setPrecision(2),
          new ReportTextItem(COL_NUMBER, loc.number()),
          new ReportTextItem(TradeConstants.COL_TRADE_SUPPLIER, loc.supplier()),
          new ReportTextItem(ALS_COUNTRY_NAME, loc.country()),
          new ReportTextItem(COL_NOTE, loc.note()),
          new ReportTextItem(COL_PAYMENT_NAME, loc.paymentType()),
          new ReportTextItem(COL_DRIVER, loc.trdDriver()),
          new ReportTextItem(COL_CURRENCY, loc.currency()));
    }

    @Override
    public String getReportCaption() {
      return Localized.dictionary().trImportCosts();
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();
      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      for (String item : new String[] {
          COL_DATE_FROM,
          COL_DATE_TO,
          COL_SPEEDOMETER_BEFORE,
          COL_SPEEDOMETER_AFTER,
          COL_FUEL_BEFORE,
          COL_FUEL_AFTER,
          COL_ITEM,
          COL_NUMBER,
          TradeConstants.COL_TRADE_SUPPLIER,
          ALS_COUNTRY_NAME,
          COL_NOTE,
          COL_PAYMENT_NAME,
          COL_DRIVER,
          COL_ITEM_PRICE
      }) {
        report.addRowItem(items.get(item));
      }

      for (String item : new String[] {
          TradeConstants.COL_TRADE_ITEM_QUANTITY, VAR_TOTAL, COL_CURRENCY
      }) {
        report.addColItem(items.get(item));
      }
      return Collections.singletonList(report);
    }

    @Override
    public LinkedHashMap<String, Editor> getReportParams() {
      LinkedHashMap<String, Editor> params = new LinkedHashMap<>();
      params.put(COL_CURRENCY, getCurrencyEditor());
      params.put(COL_TRADE_VAT, getWoVatEditor());
      return params;
    }
  },

  PROJECT_REPORT(ModuleAndSub.of(Module.PROJECTS), ProjectConstants.SVC_PROJECT_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();

      return Arrays.asList(
          new ReportTextItem(ProjectConstants.COL_PROJECT_NAME, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_NAME)),
          new ReportTextItem(ClassifierConstants.ALS_COMPANY_NAME, loc.client()),
          new ReportTextItem(ProjectConstants.ALS_STAGE_NAME, loc.prjStage()),
          new ReportTextItem(ProjectConstants.COL_PROJECT_OWNER, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_OWNER)),
          new ReportTextItem(ProjectConstants.COL_PROJECT, loc.project()),
          new ReportEnumItem(ProjectConstants.COL_PROJECT_STATUS, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_STATUS),
              ProjectStatus.class),
          new ReportTextItem(ProjectConstants.COL_PROJECT_TYPE, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_TYPE)),
          new ReportEnumItem(ProjectConstants.COL_PROJECT_PRIORITY, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_PRIORITY),
              ProjectPriority.class),
          new ReportTextItem(ProjectConstants.ALS_TERM, loc.prjTerm()),

          /* calc */
          new ReportNumericItem(COL_ACTUAL_DURATION,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  Data.getColumnLabel(VIEW_TASKS, COL_ACTUAL_DURATION),
                  loc.unitHourShort())).setPrecision(2),

          new ReportNumericItem(COL_EXPECTED_DURATION,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  Data.getColumnLabel(VIEW_TASKS,
                      COL_EXPECTED_DURATION), loc.unitHourShort())).setPrecision(2),
          new ReportNumericItem(COL_EXPECTED_EXPENSES, loc.crmTaskExpectedExpenses())
              .setPrecision(2),

          new ReportNumericItem(COL_ACTUAL_EXPENSES,
              Data.getColumnLabel(VIEW_TASKS, COL_ACTUAL_EXPENSES))
              .setPrecision(2),

          new ReportNumericItem(COL_TASK, loc.crmTask()),
          new ReportNumericItem(ProjectConstants.ALS_PROFIT, loc.profit()).setPrecision(2),

          new ReportEnumItem(ProjectConstants.ALS_TASK_STATUS,
              BeeUtils.joinWords(Data.getColumnLabel(VIEW_TASKS,
                  TaskConstants.COL_STATUS), BeeUtils.parenthesize(loc.crmTasks())),
              TaskStatus.class)
      );
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();

      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      for (String item : new String[] {
          ProjectConstants.COL_PROJECT_NAME,
          ProjectConstants.COL_PROJECT_OWNER,
          ProjectConstants.COL_PROJECT_STATUS,
          ProjectConstants.ALS_TERM
      }) {
        report.addRowItem(items.get(item));
      }
      report.setRowGrouping(items.get(ClassifierConstants.ALS_COMPANY_NAME));

      for (String item : new String[] {
          COL_EXPECTED_DURATION,
          COL_ACTUAL_DURATION,
          COL_EXPECTED_EXPENSES,
          COL_ACTUAL_EXPENSES,
          ProjectConstants.ALS_PROFIT
      }) {
        report.addColItem(items.get(item));
      }
      report.setColGrouping(items.get(ProjectConstants.ALS_TASK_STATUS));
      return Collections.singletonList(report);
    }
  },

  TASK_REPORT(ModuleAndSub.of(Module.TASKS), SVC_TASK_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();

      return Arrays.asList(
          new ReportTextItem(COL_TASK, loc.crmTask()),
          new ReportTextItem(COL_SUMMARY, Data.getColumnLabel(VIEW_TASKS, COL_SUMMARY)),
          new ReportEnumItem(COL_PRIORITY,
              Data.getColumnLabel(VIEW_TASKS, COL_PRIORITY), TaskPriority.class),
          new ReportDateTimeItem(COL_START_TIME, Data.getColumnLabel(VIEW_TASKS, COL_START_TIME)),
          new ReportDateTimeItem(COL_FINISH_TIME, Data.getColumnLabel(VIEW_TASKS, COL_FINISH_TIME)),
          new ReportTimeDurationItem(COL_EXPECTED_DURATION,
              Data.getColumnLabel(VIEW_TASKS, COL_EXPECTED_DURATION)),
          new ReportNumericItem(COL_EXPECTED_EXPENSES,
              Data.getColumnLabel(VIEW_TASKS, COL_EXPECTED_EXPENSES)),
          new ReportEnumItem(TaskConstants.COL_STATUS,
              Data.getColumnLabel(VIEW_TASKS, TaskConstants.COL_STATUS), TaskStatus.class),
          new ReportTextItem(ALS_TASK_TYPE_NAME, Data.getColumnLabel(VIEW_TASKS, COL_TASK_TYPE)),
          new ReportTextItem(TaskConstants.COL_OWNER,
              Data.getColumnLabel(VIEW_TASKS, TaskConstants.COL_OWNER)),
          new ReportTextItem(TaskConstants.COL_OWNER + COL_DEPARTMENT,
              Data.getColumnLabel(VIEW_TASKS, TaskConstants.COL_OWNER)
                  + BeeUtils.parenthesize(loc.department())),
          new ReportTextItem(COL_EXECUTOR, Data.getColumnLabel(VIEW_TASKS, COL_EXECUTOR)),
          new ReportTextItem(COL_EXECUTOR + COL_DEPARTMENT, Data.getColumnLabel(VIEW_TASKS,
              COL_EXECUTOR) + BeeUtils.parenthesize(loc.department())),
          new ReportTextItem(ALS_COMPANY_NAME, Data.getColumnLabel(VIEW_TASKS, COL_COMPANY)),
          new ReportTextItem(ALS_TASK_PRODUCT_NAME, Data.getColumnLabel(VIEW_TASKS, COL_PRODUCT)),
          new ReportTextItem(ProjectConstants.ALS_PROJECT_NAME,
              Data.getColumnLabel(VIEW_TASKS, ProjectConstants.COL_PROJECT)),
          new ReportTextItem(ProjectConstants.ALS_STAGE_NAME,
              Data.getColumnLabel(VIEW_TASKS, ProjectConstants.COL_PROJECT_STAGE)),
          new ReportEnumItem(TaskConstants.COL_EVENT,
              Data.getColumnLabel(VIEW_TASK_EVENTS, TaskConstants.COL_EVENT), TaskEvent.class),
          new ReportTextItem(COL_PUBLISHER, loc.crmTaskPublisher()),
          new ReportDateTimeItem(TaskConstants.COL_PUBLISH_TIME, Data.getColumnLabel(VIEW_TASK_EVENTS,
                  TaskConstants.COL_PUBLISH_TIME)),
          new ReportTextItem(COL_PUBLISHER + COL_DEPARTMENT,
              loc.crmTaskPublisher() + BeeUtils.parenthesize(loc.department())),
          new ReportTextItem(COL_EVENT_NOTE, Data.getColumnLabel(VIEW_TASK_EVENTS, COL_EVENT_NOTE)),
          new ReportTextItem(COL_COMMENT, Data.getColumnLabel(VIEW_TASK_EVENTS, COL_COMMENT)),
          new ReportTextItem(ALS_DURATION_TYPE_NAME,
              Data.getColumnLabel(TBL_EVENT_DURATIONS, COL_DURATION_TYPE)),
          new ReportDateTimeItem(COL_DURATION_DATE,
              Data.getColumnLabel(TBL_EVENT_DURATIONS, COL_DURATION_DATE)),
          new ReportTimeDurationItem(COL_DURATION,
              Data.getColumnLabel(TBL_EVENT_DURATIONS, COL_DURATION)),
          // Verslo Aljansas TID 25505
          new ReportNumericItem(COL_VA_MILEAGE, loc.mileage()).setPrecision(VAR_VA_MILEAGE_PREC),
          new ReportDateTimeItem(COL_COMPLETED, Data.getColumnLabel(VIEW_TASKS, COL_COMPLETED)),
          new ReportDateTimeItem(COL_APPROVED, Data.getColumnLabel(VIEW_TASKS, COL_APPROVED))
      );
    }

    @Override
    public String getReportCaption() {
      return Localized.dictionary().crmTaskReports();
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();
      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo tasks = new ReportInfo(Localized.dictionary().crmTasks());

      Stream.of(COL_TASK, COL_SUMMARY, COL_PRIORITY, COL_START_TIME, COL_FINISH_TIME,
          COL_EXPECTED_DURATION, COL_EXPECTED_EXPENSES, TaskConstants.COL_STATUS,
          ALS_TASK_TYPE_NAME, TaskConstants.COL_OWNER, COL_EXECUTOR, COL_EXECUTOR + COL_DEPARTMENT,
          ALS_COMPANY_NAME, ALS_TASK_PRODUCT_NAME, ProjectConstants.ALS_PROJECT_NAME,
          ProjectConstants.ALS_STAGE_NAME)
          .forEach(item -> tasks.addRowItem(items.get(item)));

      tasks.setColGrouping(items.get(TaskConstants.COL_EVENT));
      int idx = tasks.addColItem(items.get(TaskConstants.COL_EVENT));
      tasks.setFunction(idx, ReportFunction.COUNT);
      tasks.setRowSummary(idx, true);
      tasks.setColSummary(idx, true);

      ReportInfo taskDurations = new ReportInfo(Localized.dictionary().crmTaskDurations());

      idx = taskDurations.addRowItem(items.get(COL_TASK));
      taskDurations.getRowItems().get(idx).setRelation(tasks.getCaption());

      Stream.of(COL_SUMMARY, TaskConstants.COL_STATUS, COL_START_TIME, COL_FINISH_TIME,
          COL_EXPECTED_DURATION, COL_EXPECTED_EXPENSES)
          .forEach(item -> taskDurations.addRowItem(items.get(item)));

      taskDurations.setRowGrouping(items.get(COL_PUBLISHER));
      taskDurations.addColItem(items.get(COL_DURATION));
      taskDurations.setColGrouping(items.get(ALS_DURATION_TYPE_NAME));

      ReportInfo hoursByExecutors = new ReportInfo(Localized.dictionary().hoursByExecutors());
      hoursByExecutors.addRowItem(items.get(COL_PUBLISHER));
      hoursByExecutors.addColItem(items.get(COL_DURATION));
      hoursByExecutors.setRowGrouping(items.get(COL_PUBLISHER + COL_DEPARTMENT));
      hoursByExecutors.setColGrouping(items.get(ALS_DURATION_TYPE_NAME));
      hoursByExecutors.getColItems().forEach(item -> item.setRelation(taskDurations.getCaption()));

      ReportInfo hoursByCompanies = new ReportInfo(Localized.dictionary().hoursByCompanies());
      hoursByCompanies.addRowItem(items.get(ALS_COMPANY_NAME));
      hoursByCompanies.addColItem(items.get(COL_DURATION));

      ReportInfo hoursByTypes = new ReportInfo(Localized.dictionary().hoursByTypes());
      hoursByTypes.addRowItem(items.get(ALS_DURATION_TYPE_NAME));
      hoursByTypes.addColItem(items.get(COL_DURATION));

      return Arrays.asList(tasks, taskDurations, hoursByExecutors, hoursByTypes, hoursByCompanies);
    }
  },

  SERVICE_PAYROLL_REPORT(ModuleAndSub.of(Module.SERVICE), SVC_SERVICE_PAYROLL_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      return Arrays.asList(
          new ReportTextItem(COL_SERVICE_MAINTENANCE,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_SERVICE_MAINTENANCE)),
          new ReportTextItem(COL_REPAIRER,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_REPAIRER)),
          new ReportDateItem(COL_DATE,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_DATE)),
          new ReportDateTimeItem(COL_PAYROLL_DATE,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_PAYROLL_DATE)),
          new ReportNumericItem(COL_PAYROLL_BASIC_AMOUNT,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_PAYROLL_BASIC_AMOUNT))
              .setPrecision(2),
          new ReportNumericItem(COL_PAYROLL_TARIFF,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_PAYROLL_TARIFF)).setPrecision(2),
          new ReportNumericItem(COL_PAYROLL_SALARY,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_PAYROLL_SALARY)).setPrecision(2),
          new ReportTextItem(ALS_CURRENCY_NAME,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_CURRENCY)),
          new ReportBooleanItem(COL_PAYROLL_CONFIRMED,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_PAYROLL_CONFIRMED)),
          new ReportDateTimeItem(COL_PAYROLL_CONFIRMATION_DATE,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_PAYROLL_CONFIRMATION_DATE))
              .setFormat(DateTimeFunction.DATE),
          new ReportTextItem(COL_PAYROLL_CONFIRMED + COL_USER,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_PAYROLL_CONFIRMED + COL_USER)),
          new ReportTextItem(COL_NOTES,
              Data.getColumnLabel(TBL_MAINTENANCE_PAYROLL, COL_NOTES))
      );
    }

    @Override
    public String getReportCaption() {
      return Localized.dictionary().svcPayrollReport();
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();

      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());
      report.addRowItem(items.get(COL_SERVICE_MAINTENANCE));

      Stream.of(COL_REPAIRER, COL_DATE, COL_PAYROLL_DATE, COL_PAYROLL_BASIC_AMOUNT,
          COL_PAYROLL_TARIFF, COL_PAYROLL_SALARY, ALS_CURRENCY_NAME, COL_PAYROLL_CONFIRMED,
          COL_PAYROLL_CONFIRMATION_DATE, COL_PAYROLL_CONFIRMED + COL_USER, COL_NOTES,

        // Verslo Aljansas TID 25505
        COL_VA_MILEAGE
        )
          .forEach(item -> report.addColItem(items.get(item)));
      return Collections.singletonList(report);
    }
  },

  PAYROLL_FUND_REPORT(ModuleAndSub.of(Module.PAYROLL), SVC_PAYROLL_FUND_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary dict = Localized.dictionary();
      return Arrays.asList(
          new ReportDateItem(ALS_REPORT_TIME_PERIOD, dict.period()),

          new ReportTextItem(COL_OSF_AMOUNT,
              Data.getColumnLabel(TBL_OBJECT_SALARY_FUND, COL_OSF_AMOUNT)),

          new ReportNumericItem(TradeConstants.COL_TRADE_PAID, dict.trdPaid()).setPrecision(2),

          new ReportTextItem(COL_CURRENCY, dict.currency()),

          new ReportTextItem(TradeConstants.COL_TRADE_DEBT, dict.trdItemStock()),

          new ReportTextItem(COL_LOCATION_NAME, dict.objectLocation()),

          new ReportEnumItem(COL_LOCATION_STATUS,
              Data.getColumnLabel(VIEW_LOCATIONS, COL_LOCATION_STATUS), ObjectStatus.class),

          new ReportTextItem(COL_COMPANY_CODE, dict.companyCode()),

          new ReportTextItem(COL_COMPANY_NAME, dict.companyName()),

          new ReportTextItem(ALS_COMPANY_TYPE_NAME, dict.companyStatusName()),

          new ReportTextItem(COL_FIRST_NAME, dict.firstName()),

          new ReportTextItem(COL_LAST_NAME, dict.lastName()),

          new ReportTextItem(ALS_LOCATION_MANAGER_FIRST_NAME, BeeUtils.joinWords(dict.manager(),
              dict.firstName())),

          new ReportTextItem(ALS_LOCATION_MANAGER_LAST_NAME, BeeUtils.joinWords(dict.manager(),
              dict.lastName())),

          new ReportTextItem(FinanceConstants.ALS_EMPLOYEE_FIRST_NAME,
              BeeUtils.joinWords(dict.employee(), dict.firstName())),

          new ReportTextItem(FinanceConstants.ALS_EMPLOYEE_LAST_NAME,
              BeeUtils.joinWords(dict.employee(), dict.lastName()))
      );
    }

    @Override
    public LinkedHashMap<String, Editor> getReportParams() {
      LinkedHashMap<String, Editor> params = new LinkedHashMap<>();
      Editor currencyEditor = getCurrencyEditor();
      if (currencyEditor instanceof UnboundSelector) {
        ((UnboundSelector) currencyEditor).setValue(Global.getParameterRelation(PRM_CURRENCY),
          true);
      }
      params.put(COL_CURRENCY, currencyEditor);
      return params;
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Dictionary dict = Localized.dictionary();
      ReportInfo report = new ReportInfo(getReportCaption());

      Map<String, ReportItem> items = new HashMap<>();

      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }

      ReportExpressionItem period = new ReportExpressionItem(dict.period());
      period.append(null, new ReportDateItem(ALS_REPORT_TIME_PERIOD,
          dict.year()).setFormat(DateTimeFunction.YEAR));
      period.append(null, new ReportDateItem(ALS_REPORT_TIME_PERIOD,
          dict.month()).setFormat(DateTimeFunction.MONTH));

      report.addRowItem(period);
      report.addRowItem(items.get(COL_LOCATION_NAME));
      report.addRowItem(items.get(COL_OSF_AMOUNT));
      report.addRowItem(items.get(COL_CURRENCY));

      report.addColItem(items.get(TradeConstants.COL_TRADE_PAID));
      report.addRowItem(items.get(TradeConstants.COL_TRADE_DEBT));

      report.getFilterItems().add(new ReportDateItem(COL_DATE_FROM, dict.dateFrom()).setFilter(
          BeeUtils.toString(new YearMonth(new JustDate()).previousMonth().getDate().getDays())
      ));

      report.getFilterItems().add(new ReportDateItem(COL_DATE_TO, dict.dateTo()).setFilter(
          BeeUtils.toString(new YearMonth(new JustDate()).getDate().getDays())
      ));
      return Collections.singletonList(report);
    }

    @Override
    public String getReportCaption() {
      return Localized.dictionary().payrollFundReport();
    }
  };

  private static BeeLogger logger = LogUtils.getLogger(Report.class);

  private final ModuleAndSub moduleAndSub;
  private final String reportName;
  private final String formName;

  Report(ModuleAndSub module, String reportName) {
    this(module, reportName, "ExtendedReport");
  }

  Report(ModuleAndSub moduleAndSub, String reportName, String formName) {
    this.moduleAndSub = Assert.notNull(moduleAndSub);
    this.reportName = Assert.notEmpty(reportName);
    this.formName = formName;
  }

  public String getFormName() {
    return formName;
  }

  public List<ReportItem> getItems() {
    return new ArrayList<>();
  }

  public ModuleAndSub getModuleAndSub() {
    return moduleAndSub;
  }

  public String getReportCaption() {
    return getReportName();
  }

  public String getReportName() {
    return reportName;
  }

  public LinkedHashMap<String, Editor> getReportParams() {
    return new LinkedHashMap<>();
  }

  public Collection<ReportInfo> getReports() {
    return new LinkedHashSet<>();
  }

  @Override
  public String getSupplierKey() {
    return ViewFactory.SupplierKind.REPORT.getKey(reportName);
  }

  public void open() {
    FormFactory.openForm(formName, getInterceptor());
  }

  public void open(ReportParameters parameters) {
    ReportInterceptor interceptor = getInterceptor();
    interceptor.setInitialParameters(parameters);

    FormFactory.openForm(formName, interceptor);
  }

  public static void open(String reportName) {
    Report report = parse(reportName);

    if (report != null) {
      report.open();
    }
  }

  public static void open(String reportName, final ViewCallback callback) {
    Assert.notNull(callback);

    Report report = parse(reportName);

    if (report != null) {
      FormFactory.getFormDescription(report.getFormName(),
          result -> FormFactory.openForm(result, report.getInterceptor(),
              ViewFactory.getPresenterCallback(callback)));
    }
  }

  public static Report parse(String input) {
    for (Report report : values()) {
      if (BeeUtils.same(report.reportName, input)) {
        return report;
      }
    }
    logger.severe("report not recognized:", input);
    return null;
  }

  public void showModal(ReportParameters parameters) {
    ReportInterceptor interceptor = getInterceptor();
    interceptor.setInitialParameters(parameters);

    FormFactory.getFormDescription(getFormName(),
        description -> FormFactory.openForm(description, interceptor, presenter -> {
          Widget form = presenter.getMainView().asWidget();
          StyleUtils.setWidth(form, BeeKeeper.getScreen().getWidth() * 0.7, CssUnit.PX);
          StyleUtils.setHeight(form, BeeKeeper.getScreen().getHeight() * 0.9, CssUnit.PX);

          Popup popup = new Popup(Popup.OutsideClick.CLOSE);
          popup.setHideOnEscape(true);
          popup.setWidget(form);
          popup.center();
        }));
  }

  protected ReportInterceptor getInterceptor() {
    return new ExtendedReportInterceptor(this);
  }

  private static Editor getCurrencyEditor() {
    Relation relation = Relation.create(TBL_CURRENCIES,
        Collections.singletonList(COL_CURRENCY_NAME));
    relation.disableNewRow();
    relation.disableEdit();
    Editor currency = UnboundSelector.create(relation);
    DomUtils.setPlaceholder(currency.asWidget(), Localized.dictionary().currencyShort());
    StyleUtils.setWidth(currency.asWidget(), 80);
    return currency;
  }

  private static Editor getWoVatEditor() {
    return new InputBoolean(Localized.dictionary().trdAmountWoVat());
  }
}
