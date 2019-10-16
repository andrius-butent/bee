package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.trade.acts.TradeActKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.css.values.Overflow;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.google.gwt.i18n.client.NumberFormat;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

public class TaskOrderOtherItems extends AbstractGridInterceptor {

  private static final String STYLE_PREFIX = "be-task-" + "picker-";
  private static final String STYLE_ITEM_PANEL = STYLE_PREFIX + "item-panel";
  private static final String STYLE_HEADER_CELL_SUFFIX = "label";
  private static final String STYLE_ITEM_TABLE = STYLE_PREFIX + "item-table";
  private static final String STYLE_HEADER_ROW = STYLE_PREFIX + "header";
  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_PRICE_PREFIX = STYLE_PREFIX + "price-";
  private static final String STYLE_GROUP_PREFIX = STYLE_PREFIX + "group-";
  private static final String STYLE_NAME_PREFIX = STYLE_PREFIX + "name-";
  private static final String STYLE_QTY_PREFIX = STYLE_PREFIX + "qty-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";
  private static final String STYLE_CELL_SUFFIX = "cell";
  private static final String STYLE_ITEM_ROW = STYLE_PREFIX + "item";

  private final Flow panel = new Flow(STYLE_ITEM_PANEL);
  private final Flow container = new Flow(STYLE_CONTAINER);


  @Override
  public void afterCreatePresenter(GridPresenter presenter) {

    presenter.getHeader().clearCommandPanel();
    FaLabel showItems = new FaLabel(FontAwesome.LIST_OL);
    showItems.addClickHandler(event -> renderServices());
    presenter.getHeader().addCommandItem(showItems);

    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return new TaskOrderOtherItems();
  }

  private void renderServices() {

    FormView order = ViewHelper.getForm(getGridView());
    long taskId = order.getActiveRowId();

    Queries.getRowSet("TaskOrderOtherItems", null, Filter.equals(COL_TASK, taskId), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.isEmpty()) {
          return;
        }

        panel.clear();
        Dictionary d = Localized.dictionary();
        HtmlTable table = new HtmlTable(STYLE_ITEM_TABLE);

        int r = 0;
        int c = 0;

        table.setText(r, c++, d.service(), STYLE_NAME_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, d.description(), STYLE_GROUP_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, d.quantity(), STYLE_QTY_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, d.price(), STYLE_PRICE_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, "Suma", STYLE_PRICE_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, d.supplier(), STYLE_NAME_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, "S/F numeris", STYLE_GROUP_PREFIX + STYLE_HEADER_CELL_SUFFIX);

        table.getRowFormatter().addStyleName(r, STYLE_HEADER_ROW);

        NumberFormat nf = Format.getDecimalFormat(4, 4);
        r++;

        for (BeeRow row : result) {
          c = 0;

          table.setText(r, c++, row.getString(result.getColumnIndex("ItemName")), STYLE_NAME_PREFIX + STYLE_CELL_SUFFIX);
          table.setText(r, c++, row.getString(result.getColumnIndex(COL_DESCRIPTION)), STYLE_GROUP_PREFIX + STYLE_CELL_SUFFIX);
          table.setText(r, c++, row.getString(result.getColumnIndex("Quantity")), STYLE_QTY_PREFIX + STYLE_CELL_SUFFIX);
          table.setText(r, c++, nf.format(row.getDouble(result.getColumnIndex(COL_ITEM_PRICE))), STYLE_PRICE_PREFIX + STYLE_CELL_SUFFIX);
          table.setText(r, c++, nf.format(row.getDouble(result.getColumnIndex("Quantity")) * row.getDouble(result.getColumnIndex(COL_ITEM_PRICE))), STYLE_PRICE_PREFIX + STYLE_CELL_SUFFIX);
          table.setText(r, c++, row.getString(result.getColumnIndex("SupplierName")), STYLE_NAME_PREFIX + STYLE_CELL_SUFFIX);
          table.setText(r, c++, row.getString(result.getColumnIndex("Number")), STYLE_GROUP_PREFIX + STYLE_CELL_SUFFIX);

          table.getRowFormatter().addStyleName(r, STYLE_ITEM_ROW);
          r++;
        }

        StyleUtils.setOverflow(panel, StyleUtils.ScrollBars.VERTICAL, Overflow.AUTO);
        openDialog(table);
      }
    });
  }

  private void openDialog(HtmlTable table) {
    final DialogBox dialog = DialogBox.withoutCloseBox(Localized.dictionary().services(), STYLE_DIALOG);
    panel.add(table);
    container.add(panel);
    dialog.setWidget(container);

    FaLabel close = new FaLabel(FontAwesome.CLOSE);
    close.addStyleName(STYLE_CLOSE);

    close.addClickHandler(clickEvent -> dialog.close());
    dialog.addAction(Action.CLOSE, close);
    dialog.showOnTop(getGridPresenter().getMainView().getElement());
  }
}
