package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.ALS_COMPANY_TYPE_NAME;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.Popup;
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
import com.butent.bee.client.view.form.interceptor.ExtendedReportInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
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
import com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.report.DateTimeFunction;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
          new ReportNumericItem(TaskConstants.COL_ACTUAL_DURATION,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  Data.getColumnLabel(TaskConstants.VIEW_TASKS, TaskConstants.COL_ACTUAL_DURATION),
                  loc.unitHourShort())).setPrecision(2),

          new ReportNumericItem(TaskConstants.COL_EXPECTED_DURATION,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  Data.getColumnLabel(TaskConstants.VIEW_TASKS,
                      TaskConstants.COL_EXPECTED_DURATION), loc.unitHourShort())).setPrecision(2),
          new ReportNumericItem(TaskConstants.COL_EXPECTED_EXPENSES, loc.crmTaskExpectedExpenses())
              .setPrecision(2),

          new ReportNumericItem(TaskConstants.COL_ACTUAL_EXPENSES,
              Data.getColumnLabel(TaskConstants.VIEW_TASKS, TaskConstants.COL_ACTUAL_EXPENSES))
              .setPrecision(2),

          new ReportNumericItem(TaskConstants.COL_TASK, loc.crmTask()),
          new ReportNumericItem(ProjectConstants.ALS_PROFIT, loc.profit()).setPrecision(2),

          new ReportEnumItem(ProjectConstants.ALS_TASK_STATUS,
              BeeUtils.joinWords(Data.getColumnLabel(TaskConstants.VIEW_TASKS,
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
          TaskConstants.COL_EXPECTED_DURATION,
          TaskConstants.COL_ACTUAL_DURATION,
          TaskConstants.COL_EXPECTED_EXPENSES,
          TaskConstants.COL_ACTUAL_EXPENSES,
          ProjectConstants.ALS_PROFIT
      }) {
        report.addColItem(items.get(item));
      }
      report.setColGrouping(items.get(ProjectConstants.ALS_TASK_STATUS));
      return Collections.singletonList(report);
    }
  },

  TASK_REPORT(ModuleAndSub.of(Module.TASKS), TaskConstants.SVC_TASK_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();
      return Arrays.asList(
          new ReportTextItem(TaskConstants.COL_TASK, loc.crmTask()),
          new ReportTextItem(TaskConstants.COL_SUMMARY, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_SUMMARY)),
          new ReportTextItem(TaskConstants.COL_OWNER, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_OWNER)),
          new ReportTextItem(TaskConstants.COL_EXECUTOR, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_EXECUTOR)),
          new ReportTextItem(COL_USER, loc.crmTaskObservers()),
          new ReportEnumItem(TaskConstants.COL_PRIORITY, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_PRIORITY),
              TaskConstants.TaskPriority.class),
          new ReportEnumItem(TaskConstants.COL_STATUS, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_STATUS),
              TaskStatus.class),
          new ReportTextItem(TaskConstants.ALS_TASK_TYPE_NAME, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_TASK_TYPE)),
          new ReportTextItem(TaskConstants.ALS_TASK_PRODUCT_NAME, loc.crmTaskProduct()),
          new ReportTextItem(ALS_COMPANY_NAME, loc.company()),
          new ReportDateTimeItem(TaskConstants.COL_START_TIME, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_START_TIME)),
          new ReportDateTimeItem(TaskConstants.COL_FINISH_TIME, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_FINISH_TIME)),
          new ReportTextItem(TaskConstants.COL_EXPECTED_DURATION, Data.getColumnLabel(
              TaskConstants.VIEW_TASKS, TaskConstants.COL_EXPECTED_DURATION)),
          new ReportTextItem(ProjectConstants.ALS_PROJECT_NAME, loc.project()),
          new ReportTextItem(ProjectConstants.ALS_STAGE_NAME, loc.prjStage()),

          new ReportTextItem(ALS_DURATION_TYPE_NAME, loc.crmDurationType()),
          new ReportDateTimeItem(COL_DURATION_DATE,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  loc.crmSpentTime(), loc.unitDaysShort())),
          new ReportTimeDurationItem(COL_DURATION,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  Data.getColumnLabel(TBL_EVENT_DURATIONS,
                      COL_DURATION), loc.unitHourShort()))

      );
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();
      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      report.addRowItem(items.get(TaskConstants.COL_TASK));

      for (String item : new String[] {
          TaskConstants.COL_TASK,
          TaskConstants.COL_SUMMARY,
          TaskConstants.COL_OWNER,
          TaskConstants.COL_EXECUTOR,
          COL_USER,
          TaskConstants.COL_PRIORITY,
          TaskConstants.COL_STATUS,
          TaskConstants.ALS_TASK_TYPE_NAME,
          TaskConstants.ALS_TASK_PRODUCT_NAME,
          ALS_COMPANY_NAME,
          TaskConstants.COL_START_TIME,
          TaskConstants.COL_FINISH_TIME,
          TaskConstants.COL_EXPECTED_DURATION,
          ProjectConstants.ALS_PROJECT_NAME,
          ProjectConstants.ALS_STAGE_NAME,
          ALS_DURATION_TYPE_NAME,
          COL_DURATION_DATE,
          COL_DURATION

      }) {
        report.addColItem(items.get(item));
      }
      return Collections.singletonList(report);
    }

  },

  TASK_REPORT2(ModuleAndSub.of(Module.TASKS), TaskConstants.SVC_TASK_REPORT + 2) {
    @Override
    public List<ReportItem> getItems() {
      Dictionary loc = Localized.dictionary();
      return Arrays.asList(
        new ReportTextItem(COL_ORDER_NO, loc.order()),
        new ReportTextItem(TaskConstants.COL_TASK_ID, loc.captionId()),
        new ReportEnumItem(TaskConstants.COL_STATUS, loc.crmTaskStatus(), TaskStatus.class),
        new ReportDateTimeItem(TaskConstants.COL_START_TIME, Data.getColumnLabel(
          TaskConstants.VIEW_TASKS, TaskConstants.COL_START_TIME)),
        new ReportDateTimeItem(TaskConstants.COL_FINISH_TIME, Data.getColumnLabel(
          TaskConstants.VIEW_TASKS, TaskConstants.COL_FINISH_TIME)),
        new ReportTextItem("InvoiceSupplier", "Sąskaitą išrašė"),
        new ReportTextItem(TaskConstants.COL_OWNER, Data.getColumnLabel(
          TaskConstants.VIEW_TASKS, TaskConstants.COL_OWNER)),
        new ReportTextItem(ALS_COMPANY_NAME, loc.company()),
        new ReportTextItem(TaskConstants.COL_SUMMARY, "Darbo aprašymas"),
        new ReportTextItem("Supplier", "SPAUDA Tiekėjas"),
        new ReportNumericItem("Price", "SPAUDA Tiekėjo kaina").setPrecision(4),
        new ReportTextItem("DecorationSupplier", "APDAILA Tiekėjas"),
        new ReportNumericItem("DecorationPrice", "APDAILA Tiekėjo kaina").setPrecision(4),
        new ReportTextItem("ProductFormat", "Gaminio formatas"),
        new ReportNumericItem("PageNumber", "Psl. skaičius"),
        new ReportTextItem("PrintFormat", "Spaudos formatas"),
        new ReportNumericItem("NumberOfRuns", "Prabėgimų sk."),
        new ReportNumericItem("ArcNumber", "Skaičius lanke"),
        new ReportTextItem("PaperEnvelope", "Žaliava"),
        new ReportTextItem("PaperEnvelopeNote", "Žaliava pastaba"),
        new ReportTextItem("InvoiceSeries", "Sąskaitos serija"),
        new ReportTextItem("InvoiceNumber", "Sąskaitos numeris"),
        new ReportDateItem("InvoiceDate", "Sąskaitos data"),
        new ReportTextItem("TransportationNote", "Transp. pastaba"),
        new ReportTextItem("From", "Numeracija (Nr. nuo)"),
        new ReportTextItem("To", "Numeracija (Nr. iki)"),
        new ReportTextItem("Series", "Numeracija (Serija)"),
        new ReportNumericItem("Color1", "Spalvingumas1"),
        new ReportNumericItem("Color2", "Spalvingumas2"),
        new ReportTextItem(ALS_ITEM_NAME, "Produktas"),
        new ReportNumericItem(COL_CURRENCY_RATE_QUANTITY, "Kiekis"),
        new ReportNumericItem(COL_ITEM + COL_ITEM_PRICE, "Vieneto kaina").setPrecision(4)
      );
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();
      for (ReportItem item : getItems()) {
        items.put(item.getExpression(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      report.addRowItem(items.get(ALS_ITEM_NAME));

      for (String item : new String[]{
        COL_ORDER_NO,
        COL_TASK_ID,
        TaskConstants.COL_STATUS,
        COL_START_TIME,
        COL_FINISH_TIME,
        "InvoiceSupplier",
        TaskConstants.COL_OWNER,
        ALS_COMPANY_NAME,
        COL_SUMMARY,
        "Supplier",
        "Price",
        "DecorationSupplier",
        "DecorationPrice",
        "ProductFormat",
        "PageNumber",
        "PrintFormat",
        "NumberOfRuns",
        "ArcNumber",
        "PaperEnvelope",
        "PaperEnvelopeNote",
        "InvoiceSeries",
        "InvoiceNumber",
        "InvoiceDate",
        "TransportationNote",
        "From",
        "To",
        "Series",
        "Color1",
        "Color2",
        ALS_ITEM_NAME,
        COL_CURRENCY_RATE_QUANTITY,
        COL_ITEM + COL_ITEM_PRICE

      }) {
        report.addColItem(items.get(item));
      }
      return Collections.singletonList(report);
    }

    @Override
    public String getReportCaption() {
      return"Užsakymų ataskaita";
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
          COL_PAYROLL_CONFIRMATION_DATE, COL_PAYROLL_CONFIRMED + COL_USER, COL_NOTES)
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

        new ReportTextItem(COL_LOCATION_NAME, dict.object()),

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

  public void showModal(ReportInfo reportInfo) {
    ReportInterceptor interceptor = getInterceptor();
    interceptor.setInitialParameters(new ReportParameters(Collections.singletonMap(COL_RS_REPORT,
        reportInfo.serialize())));

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
}
