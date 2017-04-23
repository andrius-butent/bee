package com.butent.bee.client.modules.trade.reports;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.HasClickHandlers;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.modules.trade.TradeReportGroup;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TradeStockReport extends ReportInterceptor {

  private static BeeLogger logger = LogUtils.getLogger(TradeStockReport.class);

  private static final List<String> SELECTOR_NAMES = Arrays.asList(
      RP_WAREHOUSES, RP_SUPPLIERS, RP_MANUFACTURERS, RP_DOCUMENTS,
      RP_ITEM_TYPES, RP_ITEM_GROUPS, RP_ITEM_CATEGORIES, RP_ITEMS);

  private static final List<String> GROUP_NAMES = reportGroupNames(5);

  private static final String STYLE_PREFIX = TradeKeeper.STYLE_PREFIX + "report-stock-";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_BODY = STYLE_PREFIX + "body";
  private static final String STYLE_FOOTER = STYLE_PREFIX + "footer";

  private static final String STYLE_COLUMN_EMPTY_LABEL = STYLE_PREFIX + "column-empty-label";
  private static final String STYLE_COLUMN_LABEL = STYLE_PREFIX + "column-label";

  private static final String STYLE_PRICE = STYLE_PREFIX + "price";
  private static final String STYLE_QUANTITY = STYLE_PREFIX + "qty";
  private static final String STYLE_AMOUNT = STYLE_PREFIX + "amount";

  private static final String STYLE_QUANTITY_ROW = STYLE_PREFIX + "qty-row";
  private static final String STYLE_AMOUNT_ROW = STYLE_PREFIX + "amount-row";

  private static final String STYLE_ROW_TOTAL = STYLE_PREFIX + "row-total";
  private static final String STYLE_TOTAL = STYLE_PREFIX + "total";

  private static final String STYLE_EDITABLE = STYLE_PREFIX + "editable";

  private static final String KEY_GROUP = "group";
  private static final String KEY_ID = "id";

  public TradeStockReport() {
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      loadDateTime(parameters, RP_DATE, form);

      loadBoolean(parameters, RP_SHOW_QUANTITY, form);
      loadBoolean(parameters, RP_SHOW_AMOUNT, form);

      loadListByIndex(parameters, RP_ITEM_PRICE, form);
      loadId(parameters, RP_CURRENCY, form);

      loadMulti(parameters, SELECTOR_NAMES, form);

      loadDateTime(parameters, RP_RECEIVED_FROM, form);
      loadDateTime(parameters, RP_RECEIVED_TO, form);

      loadText(parameters, RP_ITEM_FILTER, form);

      loadGroupByValue(parameters, GROUP_NAMES, form);
      loadBoolean(parameters, RP_SUMMARY, form);

      loadListByValue(parameters, RP_COLUMNS, form);
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(RP_DATE, RP_RECEIVED_FROM, RP_RECEIVED_TO);
    storeBooleanValues(RP_SHOW_QUANTITY, RP_SHOW_AMOUNT, RP_SUMMARY);

    storeSelectedIndex(RP_ITEM_PRICE, 0);
    storeSelectedValue(RP_COLUMNS, 1);

    storeEditorValues(RP_CURRENCY, RP_ITEM_FILTER);
    storeEditorValues(SELECTOR_NAMES);

    storeGroupByValue(GROUP_NAMES);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeStockReport();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (RP_DOCUMENTS.equals(name) && widget instanceof DataSelector) {
      ((DataSelector) widget).setAdditionalFilter(getDocumentSelectorFilter());

    } else if (NAME_DATA_CONTAINER.equals(name) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(event -> {
        Element target = EventUtils.getEventTargetElement(event);
        TableCellElement cell = DomUtils.getParentCell(target, true);

        if (cell != null) {
          onCellClick(cell);
        }
      });
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  protected void clearFilter() {
    clearEditors(RP_DATE, RP_RECEIVED_FROM, RP_RECEIVED_TO, RP_ITEM_FILTER);
    clearEditors(SELECTOR_NAMES);
  }

  @Override
  protected void doReport() {
    ReportParameters reportParameters = getReportParameters();

    if (validateParameters(reportParameters)) {
      ParameterList parameters = TradeKeeper.createArgs(SVC_TRADE_STOCK_REPORT);
      parameters.addDataItem(Service.VAR_REPORT_PARAMETERS, reportParameters.serialize());

      BeeKeeper.getRpc().makeRequest(parameters, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasMessages()) {
            response.notify(getFormView());
          }

          if (response.hasResponse()) {
            Map<String, String> data = Codec.deserializeHashMap(response.getResponseAsString());
            render(data);

          } else {
            getFormView().notifyWarning(Localized.dictionary().nothingFound());
          }
        }
      });
    }
  }

  @Override
  protected String getBookmarkLabel() {
    List<String> labels = StringList.uniqueCaseSensitive();

    labels.addAll(getCaptions(getDateTime(RP_DATE),
        getBoolean(RP_SHOW_QUANTITY), getBoolean(RP_SHOW_AMOUNT),
        getItemPrice(), getSelectorLabel(RP_CURRENCY)));

    SELECTOR_NAMES.forEach(name -> labels.add(getSelectorLabel(name)));

    DateTime rFrom = getDateTime(RP_RECEIVED_FROM);
    DateTime rTo = getDateTime(RP_RECEIVED_TO);

    if (rFrom != null || rTo != null) {
      labels.add(BeeUtils.joinWords(Localized.dictionary().received(),
          Format.renderPeriod(rFrom, rTo)));
    }

    labels.add(getEditorValue(RP_ITEM_FILTER));

    GROUP_NAMES.stream()
        .filter(name -> BeeUtils.isPositive(getSelectedIndex(name)))
        .forEach(name -> labels.add(getSelectedItemText(name)));

    if (getBoolean(RP_SUMMARY)) {
      labels.add(Localized.dictionary().summary());
    }

    if (BeeUtils.isPositive(getSelectedIndex(RP_COLUMNS))) {
      labels.add(BeeUtils.joinWords(Localized.dictionary().columns(),
          getSelectedItemText(RP_COLUMNS)));
    }

    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return Report.TRADE_STOCK;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, RP_DATE, RP_RECEIVED_FROM, RP_RECEIVED_TO);
    addBooleanValues(parameters, RP_SHOW_QUANTITY, RP_SHOW_AMOUNT, RP_SUMMARY);

    addSelectedIndex(parameters, RP_ITEM_PRICE, 0);
    addSelectedValue(parameters, RP_COLUMNS, 1);

    addEditorValues(parameters, RP_CURRENCY, RP_ITEM_FILTER);
    addEditorValues(parameters, SELECTOR_NAMES);

    addGroupByValue(parameters, GROUP_NAMES);

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    return checkRange(getDateTime(RP_RECEIVED_FROM), getDateTime(RP_RECEIVED_TO))
        && checkFilter(ClassifierConstants.VIEW_ITEMS, getEditorValue(RP_ITEM_FILTER));
  }

  private static List<String> getCaptions(DateTime date, boolean qty, boolean amount,
      ItemPrice itemPrice, String currencyName) {

    List<String> captions = new ArrayList<>();

    captions.add(Localized.dictionary().trdReportStock());
    if (date != null) {
      captions.add(Format.renderDateLong(date));
    }

    if (qty && !amount) {
      captions.add(Localized.dictionary().quantity());

    } else if (itemPrice != null || !BeeUtils.isEmpty(currencyName)) {
      String priceName = (itemPrice == null) ? null : itemPrice.getCaption();
      captions.add(BeeUtils.joinItems(priceName, currencyName));
    }

    return captions;
  }

  private static Filter getDocumentSelectorFilter() {
    EnumSet<TradeDocumentPhase> phases = EnumSet.noneOf(TradeDocumentPhase.class);
    phases.addAll(Arrays.stream(TradeDocumentPhase.values())
        .filter(TradeDocumentPhase::modifyStock)
        .collect(Collectors.toSet()));

    EnumSet<OperationType> operationTypes = EnumSet.noneOf(OperationType.class);
    operationTypes.addAll(Arrays.stream(OperationType.values())
        .filter(OperationType::producesStock)
        .collect(Collectors.toSet()));

    return Filter.and(Filter.any(COL_TRADE_DOCUMENT_PHASE, phases),
        Filter.any(COL_OPERATION_TYPE, operationTypes));
  }

  private ItemPrice getItemPrice() {
    return EnumUtils.getEnumByIndex(ItemPrice.class, getSelectedIndex(RP_ITEM_PRICE));
  }

  private void render(Map<String, String> data) {
    SimpleRowSet rowSet = SimpleRowSet.restore(data.get(Service.VAR_DATA));
    if (DataUtils.isEmpty(rowSet)) {
      String message = Localized.dictionary().keyNotFound(Service.VAR_DATA);

      logger.severe(message);
      getFormView().notifySevere(message);
      return;
    }

    ReportParameters parameters = ReportParameters.restore(data.get(Service.VAR_REPORT_PARAMETERS));

    List<TradeReportGroup> rowGroups = EnumUtils.parseIndexList(TradeReportGroup.class,
        data.get(RP_ROW_GROUPS));

    List<String> rowGroupLabelColumns = new ArrayList<>();
    List<String> rowGroupValueColumns = new ArrayList<>();

    if (!rowGroups.isEmpty()) {
      rowGroupLabelColumns.addAll(NameUtils.toList(data.get(RP_ROW_GROUP_LABEL_COLUMNS)));
      rowGroupValueColumns.addAll(NameUtils.toList(data.get(RP_ROW_GROUP_VALUE_COLUMNS)));
    }

    TradeReportGroup columnGroup = EnumUtils.getEnumByIndex(TradeReportGroup.class,
        data.get(RP_COLUMN_GROUPS));

    List<String> columnGroupLabels = new ArrayList<>();
    List<String> columnGroupValues = new ArrayList<>();

    if (columnGroup != null) {
      columnGroupLabels.addAll(Codec.deserializeList(data.get(RP_COLUMN_GROUP_LABELS)));
      columnGroupValues.addAll(Codec.deserializeList(data.get(RP_COLUMN_GROUP_VALUES)));
    }

    List<String> quantityColumns = NameUtils.toList(data.get(RP_QUANTITY_COLUMNS));
    List<String> amountColumns = NameUtils.toList(data.get(RP_AMOUNT_COLUMNS));

    boolean hasQuantity = !quantityColumns.isEmpty();
    boolean hasAmount = !amountColumns.isEmpty();

    String priceColumn = data.get(RP_PRICE_COLUMN);
    boolean hasPrice = !BeeUtils.isEmpty(priceColumn);

    boolean hasEmptyColumnGroupValue = columnGroup != null
        && (quantityColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX))
        || amountColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX)));

    boolean needsRowTotals = columnGroup != null
        && (quantityColumns.size() > 1 || amountColumns.size() > 1);

    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    if (!container.isEmpty()) {
      container.clear();
    }

    Map<String, Double> totals = new HashMap<>();
    quantityColumns.forEach(column -> totals.put(column, BeeConst.DOUBLE_ZERO));
    amountColumns.forEach(column -> totals.put(column, BeeConst.DOUBLE_ZERO));

    double totalQuantity = BeeConst.DOUBLE_ZERO;
    double totalAmount = BeeConst.DOUBLE_ZERO;

    double rowQuantity;
    double rowAmount;

    Map<String, Integer> columnIndexes = new HashMap<>();
    int rowTotalColumnIndex = BeeConst.UNDEF;

    HtmlTable table = new HtmlTable(STYLE_TABLE);
    int r = 0;
    int c = 0;

    if (!rowGroups.isEmpty()) {
      for (TradeReportGroup group : rowGroups) {
        table.setText(r, c++, group.getCaption(), STYLE_PREFIX + group.getStyleSuffix());
      }
    }

    if (hasPrice) {
      ItemPrice itemPrice = parameters.getEnum(RP_ITEM_PRICE, ItemPrice.class);
      String text = (itemPrice == null) ? Localized.dictionary().cost() : itemPrice.getCaption();

      table.setText(r, c++, text, STYLE_PRICE);
    }

    if (columnGroup == null) {
      if (hasQuantity) {
        columnIndexes.put(quantityColumns.get(0), c);
        table.setText(r, c, Localized.dictionary().quantity(), STYLE_QUANTITY);
        c++;
      }

      if (hasAmount) {
        columnIndexes.put(amountColumns.get(0), c);
        table.setText(r, c, Localized.dictionary().amount(), STYLE_AMOUNT);
      }

    } else {
      if (hasQuantity) {
        for (int i = 0; i < quantityColumns.size(); i++) {
          columnIndexes.put(quantityColumns.get(i), c + i);
        }
      }

      if (hasAmount) {
        for (int i = 0; i < amountColumns.size(); i++) {
          columnIndexes.put(amountColumns.get(i), c + i);
        }
      }

      if (hasEmptyColumnGroupValue) {
        table.setText(r, c++, BeeUtils.bracket(columnGroup.getCaption()), STYLE_COLUMN_EMPTY_LABEL);
      }

      for (int i = 0; i < columnGroupLabels.size(); i++) {
        table.setText(r, c, TradeUtils.formatGroupLabel(columnGroup, columnGroupLabels.get(i)),
            STYLE_COLUMN_LABEL);

        if (columnGroup.isEditable() && BeeUtils.isIndex(columnGroupValues, i)) {
          TableCellElement cell = table.getCellFormatter().getElement(r, c);
          String value = columnGroupValues.get(i);

          maybeMakeEditable(cell, columnGroup, value);
        }

        c++;
      }

      if (needsRowTotals) {
        table.setText(r, c, Localized.dictionary().total(), STYLE_ROW_TOTAL);
        rowTotalColumnIndex = c;
      }
    }

    table.getRowFormatter().addStyleName(r, STYLE_HEADER);
    r++;

    for (SimpleRowSet.SimpleRow row : rowSet) {
      c = 0;

      if (!rowGroups.isEmpty()) {
        for (int i = 0; i < rowGroups.size(); i++) {
          TradeReportGroup group = rowGroups.get(i);

          String column = BeeUtils.getQuietly(rowGroupLabelColumns, i);
          String label = (column == null) ? null : row.getValue(column);

          table.setText(r, c, TradeUtils.formatGroupLabel(group, label),
              STYLE_PREFIX + group.getStyleSuffix());

          if (!BeeUtils.isEmpty(label) && group.isEditable()
              && BeeUtils.isIndex(rowGroupValueColumns, i)) {

            TableCellElement cell = table.getCellFormatter().getElement(r, c);
            String value = row.getValue(rowGroupValueColumns.get(i));

            maybeMakeEditable(cell, group, value);
          }

          c++;
        }
      }

      if (hasPrice) {
        table.setText(r, c++, TradeUtils.formatCost(row.getDouble(priceColumn)), STYLE_PRICE);
      }

      rowQuantity = BeeConst.DOUBLE_ZERO;
      rowAmount = BeeConst.DOUBLE_ZERO;

      if (columnGroup == null) {
        if (hasQuantity) {
          table.setText(r, c++, TradeUtils.formatQuantity(row.getDouble(quantityColumns.get(0))),
              STYLE_QUANTITY);
        }
        if (hasAmount) {
          table.setText(r, c, TradeUtils.formatAmount(row.getDouble(amountColumns.get(0))),
              STYLE_AMOUNT);
        }

      } else if (hasQuantity && hasAmount) {
        for (int i = 0; i < quantityColumns.size(); i++) {
          Double qty = row.getDouble(quantityColumns.get(i));
          table.setText(r, c + i, TradeUtils.formatQuantity(qty), STYLE_QUANTITY);

          rowQuantity += BeeUtils.unbox(qty);
        }

        if (needsRowTotals) {
          table.setText(r, c + quantityColumns.size(), TradeUtils.formatQuantity(rowQuantity),
              STYLE_QUANTITY, STYLE_ROW_TOTAL);

          totalQuantity += rowQuantity;
        }

        table.getRowFormatter().addStyleName(r, STYLE_QUANTITY_ROW);
        table.getRowFormatter().addStyleName(r, STYLE_BODY);
        r++;

        for (int i = 0; i < amountColumns.size(); i++) {
          Double amount = row.getDouble(amountColumns.get(i));
          table.setText(r, c + i, TradeUtils.formatAmount(amount), STYLE_AMOUNT);

          rowAmount += BeeUtils.unbox(amount);
        }

        if (needsRowTotals) {
          table.setText(r, c + amountColumns.size(), TradeUtils.formatAmount(rowAmount),
              STYLE_AMOUNT, STYLE_ROW_TOTAL);

          totalAmount += rowAmount;
        }

        table.getRowFormatter().addStyleName(r, STYLE_AMOUNT_ROW);

      } else if (hasQuantity) {
        for (String column : quantityColumns) {
          Double qty = row.getDouble(column);
          table.setText(r, c++, TradeUtils.formatQuantity(qty), STYLE_QUANTITY);

          rowQuantity += BeeUtils.unbox(qty);
        }

        if (needsRowTotals) {
          table.setText(r, c, TradeUtils.formatQuantity(rowQuantity),
              STYLE_QUANTITY, STYLE_ROW_TOTAL);

          totalQuantity += rowQuantity;
        }

      } else if (hasAmount) {
        for (String column : amountColumns) {
          Double amount = row.getDouble(column);
          table.setText(r, c++, TradeUtils.formatAmount(amount), STYLE_AMOUNT);

          rowAmount += BeeUtils.unbox(amount);
        }

        if (needsRowTotals) {
          table.setText(r, c, TradeUtils.formatAmount(rowAmount), STYLE_AMOUNT, STYLE_ROW_TOTAL);
          totalAmount += rowAmount;
        }
      }

      table.getRowFormatter().addStyleName(r, STYLE_BODY);
      r++;

      if (hasQuantity) {
        quantityColumns.forEach(column -> {
          Double value = row.getDouble(column);
          if (BeeUtils.nonZero(value)) {
            totals.merge(column, value, Double::sum);
          }
        });
      }

      if (hasAmount) {
        amountColumns.forEach(column -> {
          Double value = row.getDouble(column);
          if (BeeUtils.nonZero(value)) {
            totals.merge(column, value, Double::sum);
          }
        });
      }
    }

    if (rowSet.getNumberOfRows() > 1) {
      int minIndex = columnIndexes.values().stream().mapToInt(i -> i).min().getAsInt();
      if (minIndex > 0) {
        table.setText(r, minIndex - 1, Localized.dictionary().totalOf(), STYLE_TOTAL);
      }

      if (hasQuantity) {
        for (String column : quantityColumns) {
          table.setText(r, columnIndexes.get(column), TradeUtils.formatQuantity(totals.get(column)),
              STYLE_QUANTITY);
        }

        if (needsRowTotals) {
          table.setText(r, rowTotalColumnIndex, TradeUtils.formatQuantity(totalQuantity),
              STYLE_QUANTITY, STYLE_ROW_TOTAL);
        }
      }

      if (columnGroup != null && hasQuantity && hasAmount) {
        table.getRowFormatter().addStyleName(r, STYLE_QUANTITY_ROW);
        table.getRowFormatter().addStyleName(r, STYLE_FOOTER);
        r++;
      }

      if (hasAmount) {
        for (String column : amountColumns) {
          table.setText(r, columnIndexes.get(column), TradeUtils.formatAmount(totals.get(column)),
              STYLE_AMOUNT);
        }

        if (needsRowTotals) {
          table.setText(r, rowTotalColumnIndex, TradeUtils.formatAmount(totalAmount),
              STYLE_AMOUNT, STYLE_ROW_TOTAL);
        }

        if (columnGroup != null && hasQuantity) {
          table.getRowFormatter().addStyleName(r, STYLE_AMOUNT_ROW);
        }
      }

      table.getRowFormatter().addStyleName(r, STYLE_FOOTER);
    }

    container.add(table);
  }

  private static void maybeMakeEditable(Element cell, TradeReportGroup group, String value) {
    if (DataUtils.isId(value)) {
      DomUtils.setDataProperty(cell, KEY_GROUP, group.ordinal());
      DomUtils.setDataProperty(cell, KEY_ID, value);

      cell.addClassName(STYLE_EDITABLE);

    } else if (!BeeUtils.isEmpty(value)) {
      cell.setTitle(value);
    }
  }

  private static void onCellClick(Element cell) {
    if (cell.hasClassName(STYLE_EDITABLE)) {
      TradeReportGroup group = EnumUtils.getEnumByIndex(TradeReportGroup.class,
          DomUtils.getDataProperty(cell, KEY_GROUP));

      Long id = DomUtils.getDataPropertyLong(cell, KEY_ID);

      if (group != null && DataUtils.isId(id)) {
        RowEditor.open(group.editViewName(), id, Opener.MODAL);
      }
    }
  }
}
