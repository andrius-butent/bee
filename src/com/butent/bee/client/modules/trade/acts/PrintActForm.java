package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActTimeUnit;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PrintActForm extends AbstractFormInterceptor {

  private static final String ITEMS_WIDGET_NAME = "TradeActItems";
  private static final String SERVICES_WIDGET_NAME = "TradeActServices";
  private static final String FORM_PRINT_TA_NO_STOCK = "PrintTASaleNoStock";

  final Map<String, String> tableHeaders = new HashMap<>();
  Map<String, Widget> companies = new HashMap<>();
  List<Widget> totals = new ArrayList<>();
  List<Widget> totalsOf = new ArrayList<>();
  Consumer<Double> totConsumer;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (BeeUtils.inListSame(name, COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER, COL_SALE_PAYER,
        ClassifierConstants.COL_COMPANY)) {
      companies.put(name, widget.asWidget());

    } else if (BeeUtils.startsSame(name, "TotalInWords")) {
      totals.add(widget.asWidget());
    } else if (BeeUtils.startsSame(name, "TotalOf")) {
      totalsOf.add(widget.asWidget());
    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    for (String name : companies.keySet()) {
      Long id = form.getLongValue(name);

      if (!DataUtils.isId(id) && !BeeUtils.same(name, COL_SALE_PAYER)) {
        id = BeeKeeper.getUser().getUserData().getCompany();
      }
      ClassifierUtils.getCompanyInfo(id, companies.get(name));
    }

    totConsumer = new Consumer<Double>() {
      static final int MAX_COUNT = 4;
      double totalOf;
      int count;

      @Override
      public void accept(Double input) {
        totalOf += input;
        count++;

        if (count >= MAX_COUNT) {
          renderTotalOf(totalOf);
        }

      }
    };

    renderItems(ITEMS_WIDGET_NAME);
    renderItems(SERVICES_WIDGET_NAME);

    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new PrintActForm();
  }

  public PrintActForm() {
    tableHeaders.put("Article", "Artikulas");
    tableHeaders.put("Quantity", "Kiekis");
    tableHeaders.put("Unit", "Mato vnt.");
    tableHeaders.put("TimeUnit", "Laiko vnt.");
    tableHeaders.put("ReturnedQty", "Grąžinta");
    tableHeaders.put("RemainingQty", "Liko");
    tableHeaders.put("DateFrom", "Data nuo");
    tableHeaders.put("DateTo", "Data iki");
    tableHeaders.put("Weight", "Svoris");
    tableHeaders.put("Area", "Plotas");
    tableHeaders.put("Tariff", "Tarifas");
    tableHeaders.put("Price", "Kaina");
    tableHeaders.put("Discount", "Nuol.");
    tableHeaders.put("Amount", "Suma be PVM");
    tableHeaders.put("Vat", "PVM");
    tableHeaders.put("AmountTotal", "Suma");
    tableHeaders.put("TradeActServicesAmountTotal", "Suma už min. term.");
    tableHeaders.put("Name", "Nuomojama įranga");
    tableHeaders.put("TradeActServicesName", "Teikiamos paslaugos/prekės");
  }

  private List<String> getVisibleColumns() {
    final String[] columnList = new String[] {
        COL_ITEM_ARTICLE, COL_ITEM_NAME, COL_TA_SERVICE_FROM, COL_TA_SERVICE_TO,
        COL_TRADE_ITEM_QUANTITY, COL_UNIT, COL_TRADE_TIME_UNIT, COL_TA_RETURNED_QTY,
        "RemainingQty", COL_TRADE_WEIGHT, COL_ITEM_AREA, COL_TA_SERVICE_TARIFF,
        COL_TRADE_ITEM_PRICE, COL_TRADE_DISCOUNT, "Amount", COL_TRADE_VAT, "AmountTotal"};
    final String formName = getFormView().getFormName();

    List<String> res = new ArrayList<>();

    for (String col : columnList) {
      if (BeeUtils.same(col, "RemainingQty") && BeeUtils.same(formName, FORM_PRINT_TA_NO_STOCK)) {
        continue;
      }

      res.add(col);
    }

    return res;
  }

  private void renderItems(final String typeTable) {
    final Widget items = getFormView().getWidgetByName(typeTable);

    if (items == null) {
      return;
    }
    items.getElement().setInnerHTML(null);

    ParameterList args = TradeKeeper.createArgs(SVC_ITEMS_INFO);
    args.addDataItem("view_name", getViewName());
    args.addDataItem("id", getActiveRowId());
    args.addDataItem("TypeTable", typeTable);

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (response.hasErrors()) {
          return;
        }
        SimpleRowSet rs = SimpleRowSet.restore(response.getResponseAsString());

        if (rs.isEmpty()) {
          return;
        }
        Table<Long, String, String> data = TreeBasedTable.create();

        for (SimpleRowSet.SimpleRow row : rs) {
          Long id = row.getLong(typeTable);

          for (String col : rs.getColumnNames()) {
            String value = row.getValue(col);

            if (Objects.equals(col, COL_TA_RETURNED_QTY)) {
              BigDecimal remaining = row.getDecimal(COL_TRADE_ITEM_QUANTITY)
                  .subtract(BeeUtils.nvl(row.getDecimal(COL_TA_RETURNED_QTY), BigDecimal.ZERO));

              if (remaining.compareTo(BigDecimal.ZERO) != 0) {
                data.put(id, "RemainingQty", remaining.toPlainString());
              }
            }
            if (!BeeUtils.isEmpty(value)) {
              switch (col) {
                case COL_TA_SERVICE_FROM:
                case COL_TA_SERVICE_TO:
                  value = new JustDate(BeeUtils.toLong(value)).toString();
                  break;

                case COL_TRADE_TIME_UNIT:
                  value = EnumUtils.getCaption(TradeActTimeUnit.class, BeeUtils.toIntOrNull(value));
                  break;

                case COL_TRADE_ITEM_NOTE:
                  if (BeeUtils.same(typeTable, ITEMS_WIDGET_NAME)) {
                    data.put(id, COL_ITEM_NAME, BeeUtils.join("/", row.getValue(COL_ITEM_NAME),
                        row.getValue(COL_TRADE_ITEM_NOTE)));
                  }
                  break;

                case COL_TA_SERVICE_MIN:
                  String daysPerWeek = row.getValue(COL_TA_SERVICE_DAYS);
                  data.put(id, COL_ITEM_NAME, BeeUtils.join("/", row.getValue(COL_ITEM_NAME),
                      row.getValue(COL_TRADE_ITEM_NOTE),
                      BeeUtils.joinWords("Minimalus nuomos terminas", value,
                          EnumUtils.getCaption(TradeActTimeUnit.class,
                              row.getInt(COL_TRADE_TIME_UNIT))),
                      BeeUtils.isEmpty(daysPerWeek) ? null : daysPerWeek + "d.per Sav."));
                  break;

              }
              data.put(id, col, value);
            }
          }
          double qty = BeeUtils.toDouble(data.get(id, COL_TRADE_ITEM_QUANTITY))
              - BeeUtils.toDouble(data.get(id, COL_TA_RETURNED_QTY));
          double prc = BeeUtils.toDouble(data.get(id, COL_TRADE_ITEM_PRICE));
          double sum = qty * prc;

          double disc = BeeUtils.toDouble(data.get(id, COL_TRADE_DISCOUNT));
          double vat = BeeUtils.toDouble(data.get(id, COL_TRADE_VAT));
          boolean vatInPercents = BeeUtils.toBoolean(data.get(id, COL_TRADE_VAT_PERC));

          double dscSum = sum / 100 * disc;
          sum -= dscSum;

          if (BeeUtils.toBoolean(data.get(id, COL_TRADE_VAT_PLUS))) {
            if (vatInPercents) {
              vat = sum / 100 * vat;
            }
          } else {
            if (vatInPercents) {
              vat = sum - sum / (1 + vat / 100);
            }
            sum -= vat;
          }
          sum = BeeUtils.round(sum, 2);

          for (String col : new String[] {COL_ITEM_WEIGHT, COL_ITEM_AREA}) {
            if (data.contains(id, col)) {
              data.put(id, col,
                  BeeUtils.toString(BeeUtils.round(BeeUtils.toDouble(data.get(id, col)) * qty, 5)));
            }
          }
          if (disc > 0) {
            data.put(id, COL_TRADE_DISCOUNT,
                BeeUtils.removeTrailingZeros(BeeUtils.toString(disc)) + "%");
          }
          if (vat > 0) {
            data.put(id, COL_TRADE_VAT, BeeUtils.toString(BeeUtils.round(vat, 2)));
          }
          data.put(id, COL_TRADE_ITEM_PRICE, BeeUtils.removeTrailingZeros(BeeUtils
              .toString(BeeUtils.round((sum + dscSum) / (qty != 0 ? qty : 1d), 5))));
          data.put(id, "Amount", BeeUtils.toString(BeeUtils.round(sum, 2)));

          if (BeeUtils.same(typeTable, SERVICES_WIDGET_NAME)) {
            if (BeeUtils.isDouble(BeeUtils.toDouble(data.get(id, COL_TA_SERVICE_MIN)))) {
              double mint = BeeUtils.toDouble(data.get(id, COL_TA_SERVICE_MIN));
              sum = mint * prc;
            }
            data.put(id, "AmountTotal", BeeUtils.toString(BeeUtils.round(sum, 2)));
          } else {
            data.put(id, "AmountTotal", BeeUtils.toString(BeeUtils.round(sum + vat, 2)));
          }
        }
        HtmlTable table = new HtmlTable(TradeUtils.STYLE_ITEMS_TABLE);
        int c = 0;

        Set<String> calc = new HashSet<>();
        calc.add(COL_TRADE_ITEM_QUANTITY);
        calc.add(COL_TA_RETURNED_QTY);
        calc.add("RemainingQty");
        calc.add(COL_TRADE_WEIGHT);
        calc.add(COL_ITEM_AREA);
        calc.add("Amount");
        calc.add(COL_TRADE_VAT);
        calc.add("AmountTotal");

        for (String col : getVisibleColumns()) {

          if (!data.containsColumn(col)) {
            continue;
          }
          table.setText(0, c, BeeUtils.nvl(tableHeaders.get(typeTable + col), tableHeaders.get(
              col)), TradeUtils.STYLE_ITEMS + col);
          int r = 1;
          BigDecimal sum = BigDecimal.ZERO;

          for (Long id : data.rowKeySet()) {
            String value = data.get(id, col);

            if (calc.contains(col)) {
              sum = sum.add(BeeUtils.nvl(BeeUtils.toDecimalOrNull(value), BigDecimal.ZERO));
              value = BeeUtils.removeTrailingZeros(value);
            }

            table.setText(r++, c, value, TradeUtils.STYLE_ITEMS + col);
          }
          String value = null;

          if (sum.compareTo(BigDecimal.ZERO) != 0) {
            value = BeeUtils.removeTrailingZeros(sum.toPlainString());
          }
          if ("Amount".equals(col) || COL_TRADE_VAT.equals(col)) {
            totConsumer.accept(sum.doubleValue());
          }

          table.setText(r, c, value, TradeUtils.STYLE_ITEMS + col);
          c++;
        }
        table.setText(table.getRowCount() - 1, 0, Localized.getConstants().totalOf());

        for (int i = 0; i < table.getRowCount(); i++) {
          table.getRowFormatter().addStyleName(i, i == 0 ? TradeUtils.STYLE_ITEMS_HEADER
              : (i < (table.getRowCount() - 1)
                  ? TradeUtils.STYLE_ITEMS_DATA : TradeUtils.STYLE_ITEMS_FOOTER));
        }
        items.getElement().setInnerHTML(table.getElement().getString());
      }
    });
  }

  private void renderTotalOf(double tot) {

    for (Widget total : totals) {
      total.getElement().setInnerText("");
      TradeUtils.getTotalInWords(BeeUtils.round(tot, 2),
          getFormView().getLongValue(COL_TRADE_CURRENCY), total);
    }

    for (Widget total : totalsOf) {
      total.getElement().setInnerText("Iš viso: " + BeeUtils.round(tot, 2));
    }
  }

}
