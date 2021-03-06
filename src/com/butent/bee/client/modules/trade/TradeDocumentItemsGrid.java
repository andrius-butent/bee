package com.butent.bee.client.modules.trade;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.ModalGrid;
import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.event.logical.DataReceivedEvent;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasRowValue;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.DecimalValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TradeDocumentItemsGrid extends AbstractGridInterceptor {

  private enum SumColumn {
    AMOUNT {
      @Override
      String getName() {
        return "Amount";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemAmount(row.getId());
      }
    },

    DISCOUNT {
      @Override
      String getName() {
        return "DiscountAmount";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemDiscount(row.getId());
      }
    },

    PRICE_WITHOUT_VAT {
      @Override
      String getName() {
        return "PriceWithoutVat";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getPriceWithoutVat(row.getId());
      }
    },

    AMOUNT_WITHOUT_VAT {
      @Override
      String getName() {
        return "AmountWithoutVat";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO
            : (tds.getItemTotal(row.getId()) - tds.getItemVat(row.getId()));
      }
    },

    VAT {
      @Override
      String getName() {
        return "VatAmount";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemVat(row.getId());
      }
    },

    PRICE_WITH_VAT {
      @Override
      String getName() {
        return "PriceWithVat";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getPriceWithVat(row.getId());
      }
    },

    TOTAL {
      @Override
      String getName() {
        return "Total";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemTotal(row.getId());
      }
    };

    abstract String getName();

    abstract double getValue(IsRow row, TradeDocumentSums tds);
  }

  private final class SumRenderer extends AbstractCellRenderer implements HasRowValue {

    private final SumColumn sumColumn;

    private SumRenderer(SumColumn sumColumn) {
      super(null);
      this.sumColumn = sumColumn;
    }

    @Override
    public boolean dependsOnSource(String source) {
      return BeeUtils.inList(source, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
          COL_TRADE_DOCUMENT_ITEM_DISCOUNT, COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
          COL_TRADE_DOCUMENT_ITEM_VAT, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT);
    }

    @Override
    public Value getRowValue(IsRow row) {
      return DecimalValue.of(evaluate(row));
    }

    @Override
    public String render(IsRow row) {
      double x = evaluate(row);
      return (x == BeeConst.DOUBLE_ZERO) ? null : BeeUtils.toString(x);
    }

    private double evaluate(IsRow row) {
      if (row == null || tdsSupplier == null) {
        return BeeConst.DOUBLE_ZERO;
      } else {
        return sumColumn.getValue(row, tdsSupplier.get());
      }
    }
  }

  private static final String STYLE_SHOW_ITEM_STOCK_COMMAND =
      TradeKeeper.STYLE_PREFIX + "show-item-stock";
  private static final String STYLE_SHOW_RELATED_DOCUMENTS_COMMAND =
      TradeKeeper.STYLE_PREFIX + "show-related-documents";
  private static final String STYLE_PRICE_CALCULATION_COMMAND =
      TradeKeeper.STYLE_PREFIX + "price-calculation";
  private static final String STYLE_RETURN_COMMAND =
      TradeKeeper.STYLE_PREFIX + "return-command";

  private static final int COMMAND_DURATION = TimeUtils.MILLIS_PER_SECOND * 10;

  private Supplier<TradeDocumentSums> tdsSupplier;
  private Consumer<Boolean> tdsListener;

  TradeDocumentItemsGrid() {
  }

  void setTdsSupplier(Supplier<TradeDocumentSums> tdsSupplier) {
    this.tdsSupplier = tdsSupplier;
  }

  void setTdsListener(Consumer<Boolean> tdsListener) {
    this.tdsListener = tdsListener;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeDocumentItemsGrid();
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getHeader() != null) {
      Button stockCommand = new Button(Localized.dictionary().trdItemStock(),
          event -> showItemStock(EventUtils.getEventTargetElement(event)));
      stockCommand.addStyleName(STYLE_SHOW_ITEM_STOCK_COMMAND);
      stockCommand.setEnabled(false);

      presenter.getHeader().addCommandItem(stockCommand, COMMAND_DURATION);

      Button relatedDocumentsCommand = new Button(Localized.dictionary().trdRelatedDocuments(),
          event -> getRelatedDocuments());
      relatedDocumentsCommand.addStyleName(STYLE_SHOW_RELATED_DOCUMENTS_COMMAND);
      relatedDocumentsCommand.setEnabled(false);

      presenter.getHeader().addCommandItem(relatedDocumentsCommand, COMMAND_DURATION);

      if (BeeKeeper.getUser().canEditData(getViewName())) {
        Button priceCommand = new Button(Localized.dictionary().recalculateTradeItemPriceCaption(),
            event -> recalculatePrice());
        priceCommand.addStyleName(STYLE_PRICE_CALCULATION_COMMAND);

        presenter.getHeader().addCommandItem(priceCommand);
      }

      if (BeeKeeper.getUser().canCreateData(getViewName())) {
        Button returnCommand = new Button(Localized.dictionary().trdCommandReturn(),
            event -> onReturn(getGridView()));
        returnCommand.addStyleName(STYLE_RETURN_COMMAND);
        returnCommand.setEnabled(false);

        presenter.getHeader().addCommandItem(returnCommand, COMMAND_DURATION);
      }

      refreshCommands();
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    presenter.getGridView().ensureRelId(docId -> {
      final IsRow parentRow = getParentRow(presenter.getGridView());

      if (checkParentOnAdd(parentRow)) {
        TradeUtils.getDocumentVatPercent(parentRow, vatPercent ->
            openPicker(parentRow, vatPercent));
      }
    });

    return false;
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {
    IsRow parentRow = ViewHelper.getFormRow(gridView);

    if (parentRow != null) {
      boolean changed = false;
      CellGrid grid = getGridView().getGrid();

      boolean visible = TradeUtils.getDocumentDiscountMode(parentRow) != null;

      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, visible);
      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT, visible);

      visible = TradeUtils.getDocumentVatMode(parentRow) != null;

      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_VAT, visible);
      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT, visible);

      if (changed) {
        event.setDataChanged();
      }
    }

    super.beforeRender(gridView, event);
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {

    for (SumColumn sc : SumColumn.values()) {
      if (BeeUtils.same(columnName, sc.getName())) {
        return new SumRenderer(sc);
      }
    }

    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  @Override
  public void onActiveRowChange(ActiveRowChangeEvent event) {
    HeaderView header = getGridPresenter().getHeader();

    if (header != null) {
      IsRow row = event.getRowValue();
      boolean isService = row != null && row.isTrue(getDataIndex(COL_ITEM_IS_SERVICE));

      boolean enable = row != null && !isService
          && DataUtils.isId(row.getLong(getDataIndex(COL_ITEM)));

      header.enableCommandByStyleName(STYLE_SHOW_ITEM_STOCK_COMMAND, enable);

      if (enable) {
        TradeDocumentPhase phase = TradeUtils.getDocumentPhase(getParentRow(getGridView()));
        enable = phase != null && phase.modifyStock() && DataUtils.hasId(row);
      }

      header.enableCommandByStyleName(STYLE_SHOW_RELATED_DOCUMENTS_COMMAND, enable);
    }

    super.onActiveRowChange(event);
  }

  @Override
  public void onDataReceived(DataReceivedEvent event) {
    if (tdsSupplier != null && event != null) {
      if (getGridPresenter() != null && getGridPresenter().getUserFilter() == null) {
        tdsSupplier.get().clearItems();
      }

      if (!BeeUtils.isEmpty(event.getRows())) {
        int qtyIndex = getQuantityIndex();
        int priceIndex = getPriceIndex();

        int discountIndex = getDiscountIndex();
        int dipIndex = getDiscountIsPercentIndex();

        int vatIndex = getVatIndex();
        int vipIndex = getVatIsPercentIndex();

        tdsSupplier.get().addItems(event.getRows(), qtyIndex, priceIndex,
            discountIndex, dipIndex, vatIndex, vipIndex);
      }

      fireTdsChange(event.isInsert());
    }

    super.onDataReceived(event);
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (getHeaderView() != null) {
      refreshCommands();
    }

    super.onParentRow(event);
  }

  @Override
  public boolean previewCellUpdate(CellUpdateEvent event) {
    if (event.hasColumn() && tdsSupplier != null
        && tdsSupplier.get().containsItem(event.getRowId())) {

      long id = event.getRowId();
      String value = event.getValue();

      boolean fire = false;

      switch (event.getSourceName()) {
        case COL_TRADE_ITEM_QUANTITY:
          fire = tdsSupplier.get().updateQuantity(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_ITEM_PRICE:
          fire = tdsSupplier.get().updatePrice(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_DISCOUNT:
          fire = tdsSupplier.get().updateDiscount(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT:
          fire = tdsSupplier.get().updateDiscountIsPercent(id, BeeUtils.toBooleanOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_VAT:
          fire = tdsSupplier.get().updateVat(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT:
          fire = tdsSupplier.get().updateVatIsPercent(id, BeeUtils.toBooleanOrNull(value));
          break;
      }

      if (fire) {
        fireTdsChange(true);
      }
    }

    return super.previewCellUpdate(event);
  }

  @Override
  public boolean previewMultiDelete(MultiDeleteEvent event) {
    if (tdsSupplier != null) {
      boolean fire = false;

      for (long id : event.getRowIds()) {
        if (tdsSupplier.get().containsItem(id)) {
          tdsSupplier.get().deleteItem(id);
          fire = true;
        }
      }

      if (fire) {
        fireTdsChange(true);
      }
    }

    return super.previewMultiDelete(event);
  }

  @Override
  public boolean previewRowDelete(RowDeleteEvent event) {
    if (tdsSupplier != null && tdsSupplier.get().containsItem(event.getRowId())) {
      tdsSupplier.get().deleteItem(event.getRowId());
      fireTdsChange(true);
    }

    return super.previewRowDelete(event);
  }

  @Override
  public boolean previewRowUpdate(RowUpdateEvent event) {
    if (tdsSupplier != null && tdsSupplier.get().containsItem(event.getRowId())) {
      IsRow row = event.getRow();
      long id = row.getId();

      boolean fire = false;

      fire |= tdsSupplier.get().updateQuantity(id, row.getDouble(getQuantityIndex()));
      fire |= tdsSupplier.get().updatePrice(id, row.getDouble(getPriceIndex()));

      fire |= tdsSupplier.get().updateDiscount(id, row.getDouble(getDiscountIndex()));
      fire |= tdsSupplier.get().updateDiscountIsPercent(id,
          row.getBoolean(getDiscountIsPercentIndex()));

      fire |= tdsSupplier.get().updateVat(id, row.getDouble(getVatIndex()));
      fire |= tdsSupplier.get().updateVatIsPercent(id,
          row.getBoolean(getVatIsPercentIndex()));

      if (fire) {
        fireTdsChange(true);
      }
    }

    return super.previewRowUpdate(event);
  }

  void refreshCommands() {
    TradeUtils.configureCostCalculation(getGridView());
    checkReturnCommand(getGridView());
  }

  private void fireTdsChange(boolean update) {
    if (tdsListener != null) {
      tdsListener.accept(update);
    }
  }

  private int getQuantityIndex() {
    return getDataIndex(COL_TRADE_ITEM_QUANTITY);
  }

  private int getPriceIndex() {
    return getDataIndex(COL_TRADE_ITEM_PRICE);
  }

  private int getDiscountIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT);
  }

  private int getDiscountIsPercentIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT);
  }

  private int getVatIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_VAT);
  }

  private int getVatIsPercentIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT);
  }

  private static IsRow getParentRow(GridView gridView) {
    if (gridView == null) {
      return null;
    } else {
      return ViewHelper.getParentRow(gridView.asWidget(), VIEW_TRADE_DOCUMENTS);
    }
  }

  private static void getStock(Collection<Long> itemIds, Long warehouse,
      Multimap<String, String> options, final Consumer<Multimap<Long, IsRow>> consumer) {

    CompoundFilter filter = CompoundFilter.and();
    filter.add(Filter.equals(COL_STOCK_WAREHOUSE, warehouse),
        Filter.any(COL_ITEM, itemIds),
        Filter.isPositive(COL_STOCK_QUANTITY));

    if (options != null && options.containsKey(COL_WAREHOUSE_CONSIGNMENT)) {
      filter.add(Filter.custom(FILTER_STOCK_CONSIGNOR, options));
    }

    Order order = Order.ascending(ALS_STOCK_PRIMARY_DATE, COL_TRADE_DATE);

    Queries.getRowSet(VIEW_TRADE_STOCK, null, filter, order, rowSet -> {
      Multimap<Long, IsRow> stock = ArrayListMultimap.create();

      if (!DataUtils.isEmpty(rowSet)) {
        int itemIndex = rowSet.getColumnIndex(COL_ITEM);

        for (IsRow row : rowSet) {
          stock.put(row.getLong(itemIndex), row);
        }
      }

      consumer.accept(stock);
    });
  }

  private static boolean isStockRequired(IsRow documentRow) {
    if (documentRow == null) {
      return false;
    }

    TradeDocumentPhase phase = TradeUtils.getDocumentPhase(documentRow);
    OperationType operationType = TradeUtils.getDocumentOperationType(documentRow);

    if (phase == null || operationType == null) {
      return false;
    } else {
      return phase.modifyStock() && operationType.consumesStock();
    }
  }

  private void recalculatePrice() {
    final List<? extends IsRow> rows = getGridView().getRowData();
    if (BeeUtils.isEmpty(rows)) {
      getGridView().notifyWarning(Localized.dictionary().noData());
      return;
    }

    String caption = Localized.dictionary().recalculateTradeItemPriceCaption();

    if (rows.size() <= 1) {
      Global.confirm(caption, () -> recalculatePrice(rows));

    } else {
      final IsRow activeRow = getGridView().getActiveRow();

      if (activeRow == null) {
        Global.confirm(caption, Icon.QUESTION, Collections.singletonList(
            Localized.dictionary().recalculateTradeItemPriceForAllItems()),
            () -> recalculatePrice(rows));

      } else {
        List<String> options = new ArrayList<>();
        options.add(BeeUtils.joinWords(activeRow.getString(getDataIndex(ALS_ITEM_NAME)),
            activeRow.getString(getDataIndex(COL_TRADE_ITEM_ARTICLE))));
        options.add(Localized.dictionary().recalculateTradeItemPriceForAllItems());

        Global.choiceWithCancel(caption, null, options, choice -> {
          switch (choice) {
            case 0:
              recalculatePrice(Collections.singletonList(activeRow));
              break;
            case 1:
              recalculatePrice(rows);
              break;
          }
        });
      }
    }
  }

  private void recalculatePrice(List<? extends IsRow> rows) {
    if (BeeUtils.isEmpty(rows)) {
      return;
    }

    IsRow parentRow = getParentRow(getGridView());
    if (parentRow == null) {
      return;
    }

    DateTime date = TradeUtils.getDocumentDate(parentRow);
    Long currency = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_CURRENCY);
    if (date == null || currency == null) {
      return;
    }

    final Latch latch = new Latch(rows.size());
    final Holder<Integer> counter = Holder.of(0);

    if (TradeUtils.documentPriceIsParentCost(parentRow)) {
      startCommand(STYLE_PRICE_CALCULATION_COMMAND, TimeUtils.MILLIS_PER_MINUTE);

      for (IsRow row : rows) {
        setPriceToParentCost(row, date, currency,
            changed -> afterRecalculatePrice(changed, latch, counter));
      }

      return;
    }

    OperationType operationType = TradeUtils.getDocumentOperationType(parentRow);
    if (operationType == null) {
      return;
    }

    Long company = TradeUtils.getCompanyForPriceCalculation(parentRow, operationType);
    if (company == null) {
      getGridView().notifyWarning("company not specified");
      return;
    }

    Map<String, String> options = TradeUtils.getDocumentPriceCalculationOptions(parentRow,
        date, currency, operationType, company, null);
    if (BeeUtils.isEmpty(options)) {
      return;
    }

    Long documentWarehouse = TradeUtils.getWarehouseForPriceCalculation(parentRow, operationType);

    TradeDiscountMode discountMode = TradeUtils.getDocumentDiscountMode(parentRow);

    startCommand(STYLE_PRICE_CALCULATION_COMMAND, TimeUtils.MILLIS_PER_MINUTE);

    for (IsRow row : rows) {
      Double quantity = row.getDouble(getDataIndex(COL_TRADE_ITEM_QUANTITY));
      options.put(Service.VAR_QTY, BeeUtils.toStringOrNull(quantity));
      options.put(COL_DISCOUNT_UNIT, row.getString(getDataIndex(COL_UNIT)));

      Long warehouse = row.getLong(getDataIndex(operationType.consumesStock()
          ? COL_TRADE_ITEM_WAREHOUSE_FROM : COL_TRADE_ITEM_WAREHOUSE_TO));
      if (warehouse == null) {
        warehouse = documentWarehouse;
      }
      options.put(COL_DISCOUNT_WAREHOUSE, BeeUtils.toStringOrNull(warehouse));

      getPriceAndDiscount(row, options, discountMode,
          changed -> afterRecalculatePrice(changed, latch, counter));
    }
  }

  private void getPriceAndDiscount(final IsRow row, Map<String, String> options,
      final TradeDiscountMode discountMode, final Consumer<Boolean> callback) {

    Long item = row.getLong(getDataIndex(COL_ITEM));

    final int priceIndex = getDataIndex(COL_TRADE_ITEM_PRICE);
    final int discountIndex = getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT);
    final int discountIsPercentIndex = getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT);

    Double oldPrice = row.getDouble(priceIndex);
    Double oldDiscount = row.getDouble(discountIndex);
    boolean oldDiscountIsPercent = row.isTrue(discountIsPercentIndex);

    ClassifierKeeper.getPriceAndDiscount(item, options, (price, discount) -> {
      Double newPrice = oldPrice;
      Double newDiscount = oldDiscount;
      boolean newDiscountIsPercent = oldDiscountIsPercent;

      if (BeeUtils.isDouble(price)) {
        if (BeeUtils.nonZero(price) && BeeUtils.nonZero(discount) && discountMode == null) {
          newPrice = TradeUtils.roundPrice(BeeUtils.minusPercent(price, discount));
          newDiscount = null;
          newDiscountIsPercent = false;
        } else {
          newPrice = price;
        }
      }

      if (BeeUtils.isDouble(discount) && discountMode != null) {
        if (BeeUtils.isZero(discount)) {
          newDiscount = null;
          newDiscountIsPercent = false;
        } else {
          newDiscount = discount;
          newDiscountIsPercent = true;
        }
      }

      boolean priceChanged = !Objects.equals(oldPrice, newPrice);
      boolean discountChanged = !Objects.equals(oldDiscount, newDiscount);
      boolean discountIsPercentChanged = oldDiscountIsPercent ^ newDiscountIsPercent;

      if (priceChanged || discountChanged || discountIsPercentChanged) {
        final List<BeeColumn> columns = new ArrayList<>();
        List<String> oldValues = new ArrayList<>();
        List<String> newValues = new ArrayList<>();

        if (priceChanged) {
          columns.add(getDataColumns().get(priceIndex));
          oldValues.add(BeeUtils.toStringOrNull(oldPrice));
          newValues.add(BeeUtils.toStringOrNull(newPrice));
        }

        if (discountChanged) {
          columns.add(getDataColumns().get(discountIndex));
          oldValues.add(BeeUtils.toStringOrNull(oldDiscount));
          newValues.add(BeeUtils.toStringOrNull(newDiscount));
        }

        if (discountIsPercentChanged) {
          columns.add(getDataColumns().get(discountIsPercentIndex));
          oldValues.add(BooleanValue.pack(oldDiscountIsPercent));
          newValues.add(BooleanValue.pack(newDiscountIsPercent));
        }

        Queries.update(getViewName(), row.getId(), row.getVersion(), columns, oldValues, newValues,
            null, result -> {
              RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), result);
              getGridView().getGrid().addUpdatedSources(row.getId(),
                  DataUtils.getColumnNames(columns));

              callback.accept(true);
            });

      } else {
        callback.accept(false);
      }
    });
  }

  private void setPriceToParentCost(IsRow row, DateTime date, Long currency,
      Consumer<Boolean> callback) {

    Double cost = row.getDouble(getDataIndex(ALS_PARENT_COST));
    Long costCurrency = row.getLong(getDataIndex(ALS_PARENT_COST_CURRENCY));

    if (BeeUtils.isDouble(cost) && costCurrency != null) {
      Double oldPrice = row.getDouble(getDataIndex(COL_TRADE_ITEM_PRICE));

      Double newPrice;
      if (Objects.equals(currency, costCurrency)) {
        newPrice = cost;
      } else {
        newPrice = Data.round(getViewName(), COL_TRADE_ITEM_PRICE,
            Money.exchange(costCurrency, currency, cost, date));
      }

      if (Objects.equals(oldPrice, newPrice)) {
        callback.accept(false);

      } else {
        Queries.updateCellAndFire(getViewName(), row.getId(), row.getVersion(),
            COL_TRADE_ITEM_PRICE, BeeUtils.toStringOrNull(oldPrice),
            BeeUtils.toStringOrNull(newPrice));

        getGridView().getGrid().addUpdatedSources(row.getId(),
            Collections.singleton(COL_TRADE_ITEM_PRICE));
        callback.accept(true);
      }

    } else {
      callback.accept(false);
    }
  }

  private void afterRecalculatePrice(boolean changed, Latch latch, Holder<Integer> counter) {
    if (changed) {
      counter.set(counter.get() + 1);
    }

    latch.decrement();
    if (latch.isOpen()) {
      getGridView().notifyInfo(
          Localized.dictionary().recalculateTradeItemPriceNotification(counter.get()));

      endCommand(STYLE_PRICE_CALCULATION_COMMAND, true);
    }
  }

  private boolean checkParentOnAdd(IsRow parentRow) {
    OperationType operationType = TradeUtils.getDocumentOperationType(parentRow);
    if (operationType == null || TradeUtils.getDocumentPhase(parentRow) == null) {
      return false;
    }

    if (operationType.consumesStock()) {
      if (!TradeUtils.isDocumentValueId(parentRow, COL_TRADE_WAREHOUSE_FROM)) {
        fieldRequired(Localized.dictionary().trdWarehouseFrom());
        return false;
      }

      if (TradeUtils.isDocumentValueTrue(parentRow, ALS_WAREHOUSE_FROM_CONSIGNMENT)) {
        if (operationType.producesStock()) {
          if (!TradeUtils.isDocumentValueId(parentRow, COL_TRADE_SUPPLIER)) {
            fieldRequired(Localized.dictionary().trdSupplier());
            return false;
          }

        } else if (!TradeUtils.isDocumentValueId(parentRow, COL_TRADE_SUPPLIER)
            && !TradeUtils.isDocumentValueId(parentRow, COL_TRADE_CUSTOMER)) {

          getGridView().notifyWarning(Localized.dictionary().trdEnterSupplierOrCustomer());
          return false;
        }
      }
    }

    return true;
  }

  private void addItems(IsRow parentRow, Collection<BeeRow> selectedItems, TradeDocumentSums tds,
      Multimap<String, String> options) {

    if (isStockRequired(parentRow)) {
      Long warehouse = Data.getLong(VIEW_TRADE_DOCUMENTS, parentRow, COL_TRADE_WAREHOUSE_FROM);

      if (DataUtils.isId(warehouse)) {
        getStock(tds.getItemIds(), warehouse, options, stock ->
            addItems(parentRow, selectedItems, tds, true, stock));
      }

    } else {
      addItems(parentRow, selectedItems, tds, false, null);
    }
  }

  private void addItems(IsRow parentRow, Collection<BeeRow> selectedItems, TradeDocumentSums tds,
      boolean stockRequired, Multimap<Long, IsRow> stock) {

    DateTime date = TradeUtils.getDocumentDate(parentRow);
    Long currency = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_CURRENCY);

    int serviceIndex = Data.getColumnIndex(VIEW_ITEM_SELECTION, COL_ITEM_IS_SERVICE);
    int itemArticleIndex = Data.getColumnIndex(VIEW_ITEM_SELECTION, COL_ITEM_ARTICLE);

    int stockArticleIndex = Data.getColumnIndex(VIEW_TRADE_STOCK, COL_TRADE_ITEM_ARTICLE);
    int stockQuantityIndex = Data.getColumnIndex(VIEW_TRADE_STOCK, COL_STOCK_QUANTITY);
    int costIndex = Data.getColumnIndex(VIEW_TRADE_STOCK, COL_TRADE_ITEM_COST);
    int costCurrencyIndex = Data.getColumnIndex(VIEW_TRADE_STOCK, COL_TRADE_ITEM_COST_CURRENCY);
    int parentIndex = Data.getColumnIndex(VIEW_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM);

    boolean priceIsParentCost = TradeUtils.documentPriceIsParentCost(parentRow);

    List<BeeColumn> columns = DataUtils.getColumns(getDataColumns(),
        Arrays.asList(COL_TRADE_DOCUMENT, COL_ITEM, COL_TRADE_ITEM_ARTICLE,
            COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
            COL_TRADE_DOCUMENT_ITEM_DISCOUNT, COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
            COL_TRADE_DOCUMENT_ITEM_VAT, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT,
            COL_TRADE_ITEM_PARENT));

    BeeRowSet rowSet = new BeeRowSet(getViewName(), columns);

    for (BeeRow row : selectedItems) {
      long id = row.getId();

      double quantity = tds.getQuantity(id);

      Double price = tds.getPrice(id);
      if (BeeUtils.isZero(price)) {
        price = null;
      }

      Pair<Double, Boolean> discountInfo =
          TradeUtils.normalizeDiscountOrVatInfo(tds.getDiscountInfo(id));
      Pair<Double, Boolean> vatInfo = TradeUtils.normalizeDiscountOrVatInfo(tds.getVatInfo(id));

      if (stockRequired && !row.isTrue(serviceIndex)) {
        if (stock != null && stock.containsKey(id)) {
          for (IsRow stockRow : stock.get(id)) {
            Double stockQuantity = stockRow.getDouble(stockQuantityIndex);

            if (BeeUtils.isPositive(stockQuantity)) {
              double qty = Math.min(quantity, stockQuantity);

              if (priceIsParentCost) {
                Double cost = stockRow.getDouble(costIndex);
                Long costCurrency = stockRow.getLong(costCurrencyIndex);

                if (BeeUtils.nonZero(cost) && costCurrency != null) {
                  if (Objects.equals(currency, costCurrency)) {
                    price = cost;
                  } else {
                    price = Data.round(getViewName(), COL_TRADE_ITEM_PRICE,
                        Money.exchange(costCurrency, currency, cost, date));
                  }

                } else {
                  price = null;
                }
              }

              rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
                  Queries.asList(parentRow.getId(), id, row.getString(stockArticleIndex),
                      qty, price, discountInfo.getA(), discountInfo.getB(),
                      vatInfo.getA(), vatInfo.getB(), stockRow.getLong(parentIndex)));

              quantity -= qty;
              if (!BeeUtils.isPositive(quantity)) {
                break;
              }
            }
          }
        }

      } else {
        rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
            Queries.asList(parentRow.getId(), id, row.getString(itemArticleIndex),
                quantity, price, discountInfo.getA(), discountInfo.getB(),
                vatInfo.getA(), vatInfo.getB(), null));
      }
    }

    Queries.insertRows(rowSet, result -> {
      if (result != null && result.size() == rowSet.getNumberOfRows()) {
        for (int i = 0; i < result.size(); i++) {
          BeeRow row = rowSet.getRow(i);
          RowInfo rowInfo = result.get(i);

          row.setId(rowInfo.getId());
          row.setVersion(rowInfo.getVersion());
        }

        tdsSupplier.get().addItems(rowSet);
        fireTdsChange(true);
      }

      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getViewName(), parentRow.getId());
    });
  }

  private void openPicker(IsRow parentRow, Double defaultVatPercent) {
    TradeItemPicker picker = new TradeItemPicker(parentRow, defaultVatPercent);

    Multimap<String, String> options = ArrayListMultimap.create();

    BiConsumer<Collection<BeeRow>, TradeDocumentSums> selectionConsumer =
        (selectedItems, tds) -> addItems(parentRow, selectedItems, tds, options);

    if (TradeUtils.isDocumentValueTrue(parentRow, ALS_WAREHOUSE_FROM_CONSIGNMENT)
        && isStockRequired(parentRow)) {

      options.put(COL_WAREHOUSE_CONSIGNMENT, Codec.pack(true));

      Long warehouse = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_WAREHOUSE_FROM);
      if (DataUtils.isId(warehouse)) {
        options.put(COL_STOCK_WAREHOUSE, BeeUtils.toString(warehouse));
      }

      OperationType operationType = TradeUtils.getDocumentOperationType(parentRow);
      options.put(COL_OPERATION_TYPE, Codec.pack(operationType));

      Long supplier = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_SUPPLIER);
      if (DataUtils.isId(supplier)) {
        options.put(COL_TRADE_SUPPLIER, BeeUtils.toString(supplier));
      }

      Long customer = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_CUSTOMER);
      if (DataUtils.isId(customer)) {
        options.put(COL_TRADE_CUSTOMER, BeeUtils.toString(customer));
      }

      Filter filter;
      if (operationType.producesStock()) {
        filter = Filter.custom(FILTER_ITEM_HAS_STOCK, options);
      } else {
        filter = Filter.or(Filter.notNull(COL_ITEM_IS_SERVICE),
            Filter.custom(FILTER_ITEM_HAS_STOCK, options));
      }

      picker.setParentFilter(filter);

      StringList labels = StringList.of(TradeUtils.getDocumentString(parentRow, ALS_SUPPLIER_NAME),
          TradeUtils.getDocumentString(parentRow, ALS_CUSTOMER_NAME));

      picker.open(BeeUtils.joinWords(labels), !operationType.producesStock(), selectionConsumer);

    } else {
      picker.open(selectionConsumer);
    }
  }

  private void showItemStock(final Element target) {
    final Long item = getLongValue(COL_ITEM);

    if (DataUtils.isId(item)) {
      TradeKeeper.getItemStockByWarehouse(item, list -> {
        if (BeeUtils.isEmpty(list)) {
          getGridView().notifyInfo(Localized.dictionary().noData());

        } else if (Objects.equals(getLongValue(COL_ITEM), item)) {
          String caption = BeeUtils.joinWords(item,
              getStringValue(ALS_ITEM_NAME), getStringValue(COL_TRADE_ITEM_ARTICLE));

          Widget widget = TradeUtils.renderItemStockByWarehouse(item, list);

          if (widget != null) {
            Global.showModalWidget(caption, widget, target);
          }
        }

        endCommand(STYLE_SHOW_ITEM_STOCK_COMMAND);
      });

    } else {
      endCommand(STYLE_SHOW_ITEM_STOCK_COMMAND);
    }
  }

  private void getRelatedDocuments() {
    if (DataUtils.hasId(getActiveRow())) {
      final long id = getActiveRowId();
      Long parent = getLongValue(COL_TRADE_ITEM_PARENT);

      ParameterList parameters = TradeKeeper.createArgs(SVC_GET_RELATED_TRADE_ITEMS);
      parameters.addQueryItem(Service.VAR_ID, id);

      if (DataUtils.isId(parent)) {
        parameters.addQueryItem(COL_TRADE_ITEM_PARENT, parent);
      }

      BeeKeeper.getRpc().makeRequest(parameters, response -> {
        if (Objects.equals(id, getActiveRowId())) {
          if (response.hasResponse()) {
            BeeRowSet rowSet = BeeRowSet.restore(response.getResponseAsString());
            showRelatedDocuments(rowSet);

          } else if (getGridView().isInteractive()) {
            getGridView().notifyInfo(Localized.dictionary().noData());
          }
        }

        endCommand(STYLE_SHOW_RELATED_DOCUMENTS_COMMAND);
      });

    } else {
      endCommand(STYLE_SHOW_RELATED_DOCUMENTS_COMMAND);
    }
  }

  private void showRelatedDocuments(final BeeRowSet rowSet) {
    String caption = BeeUtils.joinItems(Localized.dictionary().trdRelatedDocuments(),
        getActiveRowId(), getStringValue(ALS_ITEM_NAME), getStringValue(COL_TRADE_ITEM_ARTICLE));

    int height = BeeUtils.resize(rowSet.getNumberOfRows(), 1, 12, 20, 80);

    GridInterceptor interceptor = new AbstractGridInterceptor() {
      @Override
      public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
        return rowSet;
      }
    };

    GridFactory.openGrid(GRID_TRADE_RELATED_ITEMS, interceptor,
        GridFactory.GridOptions.forCaption(caption),
        ModalGrid.opener(75, CssUnit.PCT, height, CssUnit.PCT, false));
  }

  private void checkReturnCommand(GridView gridView) {
    HeaderView header = getHeaderView();

    if (gridView != null && header != null) {
      OperationType operationType = TradeUtils.getDocumentOperationType(getParentRow(gridView));
      boolean enable = operationType != null && operationType.isReturn()
          && !gridView.isReadOnly() && BeeKeeper.getUser().canCreateData(getViewName());

      header.enableCommandByStyleName(STYLE_RETURN_COMMAND, enable);
    }
  }

  private void onReturn(GridView gridView) {
    gridView.ensureRelId(docId -> {
      IsRow parentRow = getParentRow(gridView);
      OperationType operationType = TradeUtils.getDocumentOperationType(parentRow);

      if (checkParentOnAdd(parentRow) && operationType != null && operationType.isReturn()) {
        if (operationType.consumesStock()) {
          onReturnToSupplier(parentRow);
        } else {
          onCustomerReturn(parentRow);
        }

      } else {
        endReturnCommand();
      }
    });
  }

  private void endReturnCommand() {
    endCommand(STYLE_RETURN_COMMAND, true);
  }

  private void onReturnToSupplier(IsRow documentRow) {
    Long supplier = TradeUtils.getDocumentRelation(documentRow, COL_TRADE_CUSTOMER);

    if (!DataUtils.isId(supplier)) {
      FormView form = ViewHelper.getForm(getGridView());
      if (form != null) {
        String label = Data.getColumnLabel(form.getViewName(), COL_TRADE_CUSTOMER);
        form.notifyWarning(Localized.dictionary().fieldRequired(label));
      }

      endReturnCommand();
      return;
    }

    DateTime date = TradeUtils.getDocumentDate(documentRow);

    String n1 = TradeUtils.getDocumentString(documentRow, COL_TRADE_DOCUMENT_NUMBER_1);
    String n2 = TradeUtils.getDocumentString(documentRow, COL_TRADE_DOCUMENT_NUMBER_2);

    Long warehouse = TradeUtils.getDocumentRelation(documentRow, COL_TRADE_WAREHOUSE_FROM);

    String supplierName = TradeUtils.getDocumentString(documentRow, ALS_CUSTOMER_NAME);
    String warehouseCode = TradeUtils.getDocumentString(documentRow, ALS_WAREHOUSE_FROM_CODE);

    String caption = BeeUtils.joinWords(supplierName, n1, n2, Format.renderDate(date),
        warehouseCode);

    Multimap<String, String> options = ArrayListMultimap.create();
    options.put(COL_TRADE_SUPPLIER, BeeUtils.toString(supplier));

    if (date != null) {
      options.put(COL_TRADE_DATE, TimeUtils.startOfNextDay(date).serialize());
    }

    if (!BeeUtils.isEmpty(n1)) {
      options.put(COL_TRADE_NUMBER, n1);
    }
    if (!BeeUtils.isEmpty(n2) && !BeeUtils.equalsTrim(n1, n2)) {
      options.put(COL_TRADE_NUMBER, n2);
    }

    if (DataUtils.isId(warehouse)) {
      options.put(COL_STOCK_WAREHOUSE, BeeUtils.toString(warehouse));
    }

    Filter filter = Filter.custom(FILTER_ITEM_HAS_STOCK, options);
    long docId = documentRow.getId();

    Queries.hasAnyRows(VIEW_ITEMS, filter, has -> {
      if (getGridView().isInteractive() && Objects.equals(getGridView().getRelId(), docId)) {
        if (has) {
          TradeUtils.getDocumentVatPercent(documentRow, vatPercent -> {
            TradeItemPicker picker = new TradeItemPicker(documentRow, vatPercent);
            picker.setParentFilter(filter);

            picker.open(caption, false,
                (selectedItems, tds) -> addItems(documentRow, selectedItems, tds, options));
            endReturnCommand();
          });

        } else {
          getGridView().notifyInfo(Localized.dictionary().trdTypeReturnToSupplier(), caption,
              Localized.dictionary().nothingFound());
          endReturnCommand();
        }
      }
    });
  }

  private void onCustomerReturn(IsRow documentRow) {
    Long customer = TradeUtils.getDocumentRelation(documentRow, COL_TRADE_SUPPLIER);

    if (!DataUtils.isId(customer)) {
      FormView form = ViewHelper.getForm(getGridView());
      if (form != null) {
        String label = Data.getColumnLabel(form.getViewName(), COL_TRADE_SUPPLIER);
        form.notifyWarning(Localized.dictionary().fieldRequired(label));
      }

      endReturnCommand();
      return;
    }

    long docId = documentRow.getId();

    DateTime date = TradeUtils.getDocumentDate(documentRow);

    String n1 = TradeUtils.getDocumentString(documentRow, COL_TRADE_DOCUMENT_NUMBER_1);
    String n2 = TradeUtils.getDocumentString(documentRow, COL_TRADE_DOCUMENT_NUMBER_2);

    String customerName = TradeUtils.getDocumentString(documentRow, ALS_SUPPLIER_NAME);
    String caption = BeeUtils.joinWords(customerName, n1, n2, Format.renderDate(date));

    Filter filter = TradeUtils.getFilterForCustomerReturn(customer, date, n1, n2);

    Queries.getRowCount(VIEW_TRADE_ITEMS_FOR_RETURN, filter, rowCount -> {
      if (getGridView().isInteractive() && Objects.equals(getGridView().getRelId(), docId)) {
        if (BeeUtils.isPositive(rowCount)) {
          openCustomerReturns(docId, caption, filter, rowCount);

        } else {
          getGridView().notifyInfo(Localized.dictionary().trdItemsForReturn(), caption,
              Localized.dictionary().nothingFound());
          endReturnCommand();
        }
      }
    });
  }

  private void openCustomerReturns(long docId, String caption, Filter filter, int rowCount) {
    GridInterceptor interceptor =
        new TradeItemsForReturnGrid(selection -> acceptCustomerReturns(docId, selection));

    PresenterCallback opener = presenter -> {
      double height = BeeUtils.rescale(rowCount, 1, 12, 20, 80);
      Dimensions dimensions = new Dimensions(75.0, CssUnit.PCT, height, CssUnit.PCT);

      ModalGrid modalGrid = new ModalGrid(presenter, dimensions, false);
      modalGrid.center();

      endReturnCommand();
    };

    GridFactory.openGrid(GRID_TRADE_ITEMS_FOR_RETURN, interceptor,
        GridFactory.GridOptions.forCaptionAndFilter(caption, filter), opener);
  }

  private void acceptCustomerReturns(long docId, Map<Long, Double> selection) {
    if (!BeeUtils.isEmpty(selection)) {
      Queries.getRowSet(VIEW_TRADE_MOVEMENT, null, Filter.idIn(selection.keySet()), rowSet -> {
        if (!DataUtils.isEmpty(rowSet) && getGridView().isInteractive()) {
          IsRow documentRow = getParentRow(getGridView());

          if (DataUtils.idEquals(documentRow, docId)) {
            int index = rowSet.getColumnIndex(COL_TRADE_ITEM_QUANTITY);
            rowSet.forEach(row -> row.setValue(index, selection.get(row.getId())));

            addCustomerReturns(documentRow, rowSet);
          }
        }
      });
    }
  }

  private void addCustomerReturns(IsRow documentRow, BeeRowSet data) {
    DateTime date = TradeUtils.getDocumentDate(documentRow);
    Long documentCurrency = TradeUtils.getDocumentRelation(documentRow, COL_TRADE_CURRENCY);

    int currencyIndex = data.getColumnIndex(COL_TRADE_CURRENCY);

    int itemIndex = data.getColumnIndex(COL_ITEM);
    int articleIndex = data.getColumnIndex(COL_ITEM_ARTICLE);
    int primaryArticleIndex = data.getColumnIndex(ALS_PRIMARY_ARTICLE);

    int qtyIndex = data.getColumnIndex(COL_TRADE_ITEM_QUANTITY);
    int priceIndex = data.getColumnIndex(COL_TRADE_ITEM_PRICE);

    int discountIndex = data.getColumnIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT);
    int dipIndex = data.getColumnIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT);

    int vatIndex = data.getColumnIndex(COL_TRADE_DOCUMENT_ITEM_VAT);
    int vipIndex = data.getColumnIndex(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT);

    List<BeeColumn> columns = DataUtils.getColumns(getDataColumns(),
        Arrays.asList(COL_TRADE_DOCUMENT, COL_ITEM, COL_TRADE_ITEM_ARTICLE,
            COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
            COL_TRADE_DOCUMENT_ITEM_DISCOUNT, COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
            COL_TRADE_DOCUMENT_ITEM_VAT, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT));

    BeeRowSet rowSet = new BeeRowSet(getViewName(), columns);

    for (BeeRow row : data) {
      String article = BeeUtils.notEmpty(row.getString(articleIndex),
          row.getString(primaryArticleIndex));

      Double price = row.getDouble(priceIndex);
      Long currency = row.getLong(currencyIndex);

      if (BeeUtils.isPositive(price)) {
        if (!Objects.equals(currency, documentCurrency)) {
          price = Localized.normalizeMoney(
              Money.maybeExchange(currency, documentCurrency, price, date));
        }
      } else {
        price = null;
      }

      rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
          Queries.asList(documentRow.getId(),
              row.getLong(itemIndex), article,
              row.getDouble(qtyIndex), price,
              row.getDouble(discountIndex), row.getBoolean(dipIndex),
              row.getDouble(vatIndex), row.getBoolean(vipIndex)));
    }

    Queries.insertRows(rowSet, result -> {
      if (result != null && result.size() == rowSet.getNumberOfRows()) {
        Map<Long, Long> returns = new HashMap<>();

        for (int i = 0; i < result.size(); i++) {
          BeeRow row = rowSet.getRow(i);
          RowInfo rowInfo = result.get(i);

          row.setId(rowInfo.getId());
          row.setVersion(rowInfo.getVersion());

          returns.put(rowInfo.getId(), data.getRow(i).getId());
        }

        tdsSupplier.get().addItems(rowSet);
        fireTdsChange(true);

        ParameterList parameters = TradeKeeper.createArgs(SVC_SAVE_CUSTOMER_RETURNS);
        parameters.addDataItem(TBL_TRADE_ITEM_RETURNS, Codec.beeSerialize(returns));

        BeeKeeper.getRpc().makeRequest(parameters);
      }

      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getViewName(), documentRow.getId());
    });
  }

  private void fieldRequired(String label) {
    getGridView().notifyWarning(Localized.dictionary().fieldRequired(label));
  }
}
