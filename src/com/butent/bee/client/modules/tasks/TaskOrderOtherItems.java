package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.grid.HtmlTable;
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
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

public class TaskOrderOtherItems extends AbstractGridInterceptor {

  private static final String STYLE_PREFIX = TradeActKeeper.STYLE_PREFIX + "picker-";
  private static final String STYLE_ITEM_PANEL = STYLE_PREFIX + "item-panel";
  private static final String STYLE_HEADER_CELL_SUFFIX = "label";
  private static final String STYLE_ITEM_TABLE = STYLE_PREFIX + "item-table";

  private static final String STYLE_PRICE_PREFIX = STYLE_PREFIX + "price-";
  private static final String STYLE_PRICE_HEADER_CELL = STYLE_PRICE_PREFIX
    + STYLE_HEADER_CELL_SUFFIX;

  private static final String STYLE_TYPE_PREFIX = STYLE_PREFIX + "type-";
  private static final String STYLE_GROUP_PREFIX = STYLE_PREFIX + "group-";
  private static final String STYLE_NAME_PREFIX = STYLE_PREFIX + "name-";
  private static final String STYLE_ARTICLE_PREFIX = STYLE_PREFIX + "article-";

  private final Flow panel = new Flow(STYLE_ITEM_PANEL);

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {

    presenter.getHeader().clearCommandPanel();
    FaLabel showItems = new FaLabel(FontAwesome.LIST_OL);
    showItems.setTitle(Localized.dictionary().taRecalculatePrices());
    showItems.addClickHandler(event -> render());
    presenter.getHeader().addCommandItem(showItems);

    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return new TaskOrderOtherItems();
  }

  private void render() {

    FormView order = ViewHelper.getForm(getGridView());
    long taskId = order.getActiveRowId();

    Queries.getRowSet("TaskOrderOtherItems", null, Filter.equals(COL_TASK, taskId), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.isEmpty()) {
          return;
        }

        Dictionary d = Localized.dictionary();
        HtmlTable table = new HtmlTable(STYLE_ITEM_TABLE);

        int r = 0;
        int c = 0;

        String pfx;

        table.setText(r, c++, d.service(), STYLE_TYPE_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, d.description(), STYLE_GROUP_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, d.quantity(), STYLE_NAME_PREFIX + STYLE_HEADER_CELL_SUFFIX);
        table.setText(r, c++, d.price(), STYLE_ARTICLE_PREFIX + STYLE_HEADER_CELL_SUFFIX);

        //StyleUtils.setSize(container, 800, 600);
        StyleUtils.setOverflow(panel, StyleUtils.ScrollBars.VERTICAL, Overflow.AUTO);
        panel.add(table);

        Global.showModalWidget(panel);
      }
    });
  }
}
