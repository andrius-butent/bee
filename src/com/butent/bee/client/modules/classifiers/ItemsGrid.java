package com.butent.bee.client.modules.classifiers;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.COL_SERVICE_MAINTENANCE;
import static com.butent.bee.shared.modules.service.ServiceConstants.TBL_SERVICE_MAINTENANCE;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.orders.OrderForm;
import com.butent.bee.client.modules.service.ServiceMaintenanceForm;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.TreeGridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ItemsGrid extends TreeGridInterceptor {

  private static final String STYLE_RES_ORDER_PREFIX = "bee-reserved-orders-";
  private static final String STYLE_RES_REPAIR_PREFIX = "bee-reserved-repairs-";
  private static final String STYLE_RES_ORDER_TABLE = STYLE_RES_ORDER_PREFIX + "table";
  private static final String STYLE_RES_ORDER_HEADER = STYLE_RES_ORDER_PREFIX + "header";
  private static final String STYLE_RES_ORDER_INFO = STYLE_RES_ORDER_PREFIX + "info";
  private static final String STYLE_LABEL_SUFFIX = "label";
  private static final String STYLE_CELL_SUFFIX = "cell";

  private static final String STYLE_RES_ORDER_ID_PREFIX = STYLE_RES_ORDER_PREFIX + "id-";
  private static final String STYLE_RES_REPAIR_ID_PREFIX = STYLE_RES_REPAIR_PREFIX + "id-";
  private static final String STYLE_RES_ORDER_DATE_PREFIX = STYLE_RES_ORDER_PREFIX + "date-";
  private static final String STYLE_RES_ORDER_COMPANY_PREFIX = STYLE_RES_ORDER_PREFIX + "company-";
  private static final String STYLE_RES_ORDER_MANAGER_PREFIX = STYLE_RES_ORDER_PREFIX
      + "manager-";
  private static final String STYLE_RES_ORDER_WAREHOUSE_PREFIX = STYLE_RES_ORDER_PREFIX
      + "warehouse-";
  private static final String STYLE_RES_ORDER_REMAINDER_PREFIX = STYLE_RES_ORDER_PREFIX
      + "remainderQty-";
  private static final String STYLE_RES_ORDER_INVOICE_QTY_PREFIX = STYLE_RES_ORDER_PREFIX
      + "invoiceQty-";

  static String getSupplierKey(boolean services) {
    return BeeUtils.join(BeeConst.STRING_UNDER, GRID_ITEMS, services ? "services" : "goods");
  }

  private final boolean services;

  private Button stockCommand;

  ItemsGrid(boolean services) {
    this.services = services;
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (!showServices() && BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.TRADE))
        && BeeKeeper.getUser().isDataVisible(TradeConstants.VIEW_TRADE_STOCK)) {

      stockCommand = new Button(Localized.dictionary().trdItemStock(), event -> showStock());
      StyleUtils.hideDisplay(stockCommand);

      presenter.getHeader().addCommandItem(stockCommand);
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {

    String id = columnDescription.getId();

    if (showServices()) {
      switch (id) {
        case COL_ITEM_WEIGHT:
        case COL_ITEM_AREA:
        case COL_RESERVED_REMAINDER:
        case PRP_FREE_REMAINDER:
        case COL_WAREHOUSE_REMAINDER:
        case TradeConstants.PROP_STOCK:
          return null;
      }

    } else {
      switch (id) {
        case COL_TIME_UNIT:
        case COL_ITEM_DPW:
        case COL_ITEM_MIN_TERM:
          return null;
      }
    }

    return super.beforeCreateColumn(gridView, columnDescription);
  }

  @Override
  public String getCaption() {
    if (showServices()) {
      return Localized.dictionary().stocks();
    } else {
      return Localized.dictionary().goods();
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new ItemsGrid(services);
  }

  @Override
  public List<String> getParentLabels() {
    if (getSelectedTreeItem() == null || getTreeView() == null) {
      return super.getParentLabels();
    } else {
      return getTreeView().getPathLabels(getSelectedTreeItem().getId(), COL_CATEGORY_NAME);
    }
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    gridDescription.setCaption(null);

    Filter filter;
    if (showServices()) {
      filter = Filter.notNull(COL_ITEM_IS_SERVICE);
    } else {
      filter = Filter.isNull(COL_ITEM_IS_SERVICE);
    }

    gridDescription.setFilter(filter);
    return true;
  }

  @Override
  public void onEditStart(EditStartEvent event) {

    if (BeeUtils.same(event.getColumnId(), COL_RESERVED_REMAINDER)) {
      event.consume();

      IsRow row = event.getRowValue();
      ParameterList reservationParams = ClassifierKeeper.createArgs(SVC_GET_RESERVATION);
      reservationParams.addDataItem(COL_ITEM, row.getId());

      BeeKeeper.getRpc().makePostRequest(reservationParams, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject reservationResponse) {
          if (!reservationResponse.hasErrors()) {
            SimpleRowSet rowSet = SimpleRowSet.restore(reservationResponse.getResponseAsString());
            if (rowSet.getNumberOfRows() > 0) {
              ParameterList params = ClassifierKeeper.createArgs(SVC_FILTER_ORDERS);
              params.addDataItem(TBL_ORDERS, Codec.beeSerialize(rowSet.getLongColumn(COL_ORDER)));
              params.addDataItem(COL_SERVICE_MAINTENANCE,
                  Codec.beeSerialize(rowSet.getLongColumn(COL_SERVICE_MAINTENANCE)));
              params.addDataItem(COL_ITEM, row.getId());

              BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  if (!response.hasErrors()) {
                    Map<Pair<String, String>, Pair<String, String>> remainders = new HashMap<>();
                    for (Map.Entry<String, String> entry : Codec.deserializeHashMap(response
                        .getResponseAsString()).entrySet()) {
                      remainders.put(Pair.restore(entry.getKey()), Pair.restore(entry.getValue()));
                    }

                    Global.showModalWidget(renderOrders(rowSet, remainders));
                  }
                }
              });
            }
          }
        }
      });
    } else {
      super.onEditStart(event);
    }

  }

  @Override
  public void onActiveRowChange(ActiveRowChangeEvent event) {
    if (stockCommand != null) {
      StyleUtils.setVisible(stockCommand, DataUtils.hasId(event.getRowValue()));
    }

    super.onActiveRowChange(event);
  }

  public boolean showServices() {
    return services;
  }

  @Override
  protected Filter getFilter(Long category) {
    if (category == null) {
      return null;
    } else {
      return Filter.or(Filter.equals(COL_ITEM_TYPE, category),
          Filter.equals(COL_ITEM_GROUP, category),
          Filter.in(Data.getIdColumn(VIEW_ITEMS),
              VIEW_ITEM_CATEGORIES, COL_ITEM, Filter.equals(COL_CATEGORY, category)));
    }
  }

  IsRow getSelectedCategory() {
    return getSelectedTreeItem();
  }

  private static HtmlTable renderOrders(SimpleRowSet rowSet,
      Map<Pair<String, String>, Pair<String, String>> remainderMap) {

    HtmlTable table = new HtmlTable(STYLE_RES_ORDER_TABLE);
    Dictionary lc = Localized.dictionary();

    int row = 0;
    int col = 0;
    table.setText(row, col++, lc.order(), STYLE_RES_ORDER_ID_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(row, col++, lc.svcRepair(), STYLE_RES_REPAIR_ID_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(row, col++, lc.orderDate(), STYLE_RES_ORDER_DATE_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(row, col++, lc.company(), STYLE_RES_ORDER_COMPANY_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(row, col++, lc.manager(), STYLE_RES_ORDER_MANAGER_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(row, col++, lc.warehouse(), STYLE_RES_ORDER_WAREHOUSE_PREFIX
        + STYLE_LABEL_SUFFIX);
    table.setText(row, col++, lc.ordResQty(), STYLE_RES_ORDER_REMAINDER_PREFIX
        + STYLE_LABEL_SUFFIX);
    table.setText(row, col, lc.ordInvoiceQty(), STYLE_RES_ORDER_INVOICE_QTY_PREFIX
        + STYLE_LABEL_SUFFIX);
    table.getRowFormatter().addStyleName(row, STYLE_RES_ORDER_HEADER);

    for (SimpleRow dataRow : rowSet) {
      col = 0;
      row++;
      String order = dataRow.getValue(COL_ORDER);
      String repair = dataRow.getValue(COL_SERVICE_MAINTENANCE);
      table.setText(row, col++, order, STYLE_RES_ORDER_ID_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(row, col++, repair, STYLE_RES_REPAIR_ID_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(row, col++, Format.renderDate(dataRow.getDateTime(
          ProjectConstants.COL_DATES_START_DATE).getDate()),
          STYLE_RES_ORDER_DATE_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(row, col++, dataRow.getValue(ALS_COMPANY_NAME),
          STYLE_RES_ORDER_COMPANY_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(row, col++, BeeUtils.join(" ", dataRow.getValue(COL_FIRST_NAME),
          dataRow.getValue(COL_LAST_NAME)), STYLE_RES_ORDER_MANAGER_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(row, col++, dataRow.getValue(rowSet.getColumnIndex(ALS_WAREHOUSE_CODE)),
          STYLE_RES_ORDER_WAREHOUSE_PREFIX + STYLE_CELL_SUFFIX);

      Pair<String, String> remainderKey = DataUtils.isId(order)
          ? Pair.of(COL_ORDER, order) : Pair.of(COL_SERVICE_MAINTENANCE, repair);

      table.setText(row, col++, remainderMap.get(remainderKey).getA(),
          STYLE_RES_ORDER_REMAINDER_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(row, col, remainderMap.get(remainderKey).getB(),
          STYLE_RES_ORDER_INVOICE_QTY_PREFIX + STYLE_CELL_SUFFIX);
      table.getRowFormatter().addStyleName(row, STYLE_RES_ORDER_INFO);
    }

    table.addClickHandler(event -> {
      Element target = EventUtils.getEventTargetElement(event);
      TableCellElement cell = DomUtils.getParentCell(target, true);

      if (cell != null) {
        long id = Long.valueOf(cell.getInnerText());
        if (DataUtils.isId(id)) {
          if (cell.hasClassName(STYLE_RES_ORDER_ID_PREFIX + STYLE_CELL_SUFFIX)) {
            RowEditor.openForm(COL_ORDER, Data.getDataInfo(VIEW_ORDERS), Filter.compareId(id),
                Opener.MODAL, null, new OrderForm());

          } else if (cell.hasClassName(STYLE_RES_REPAIR_ID_PREFIX + STYLE_CELL_SUFFIX)) {
            RowEditor.openForm(COL_SERVICE_MAINTENANCE, Data.getDataInfo(TBL_SERVICE_MAINTENANCE),
                Filter.compareId(id), Opener.MODAL, null, new ServiceMaintenanceForm());
          }
        }
      }
    });

    return table;
  }

  private void showStock() {
    final long id = getActiveRowId();

    if (DataUtils.isId(id)) {
      TradeKeeper.getItemStockByWarehouse(id, list -> {
        if (BeeUtils.isEmpty(list)) {
          getGridView().notifyInfo(Localized.dictionary().noData());

        } else if (Objects.equals(getActiveRowId(), id)) {
          String caption = BeeUtils.joinWords(id,
              getStringValue(COL_ITEM_NAME), getStringValue(COL_ITEM_ARTICLE));

          Widget widget = TradeUtils.renderItemStockByWarehouse(id, list);

          if (widget != null) {
            Global.showModalWidget(caption, widget, stockCommand.getElement());
          }
        }
      });
    }
  }
}
