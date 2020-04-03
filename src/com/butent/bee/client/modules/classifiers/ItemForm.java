package com.butent.bee.client.modules.classifiers;

import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.presenter.GridFormPresenter;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ItemForm extends AbstractFormInterceptor {

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    int index = form.getDataIndex(ClassifierConstants.COL_ITEM_IS_SERVICE);
    boolean isService = row != null && !row.isNull(index);

    String caption;

    if (DataUtils.isNewRow(row)) {
      Widget categoryWidget = form.getWidgetByName("Categories");

      if (categoryWidget instanceof MultiSelector) {
        List<Long> categories = new ArrayList<>();
        ItemsGrid gridHandler = getItemGridHandler(form);

        if (gridHandler != null && gridHandler.getSelectedCategory() != null) {
          categories.add(gridHandler.getSelectedCategory().getId());
        }

        ((MultiSelector) categoryWidget).setIds(categories);
      }

      caption = isService
          ? Localized.dictionary().newStocks() : Localized.dictionary().newItem();

    } else {
      caption = isService
          ? Localized.dictionary().stocks() : Localized.dictionary().item();

    }

    if (form.getViewPresenter() != null && form.getViewPresenter().getHeader() != null) {
      form.getViewPresenter().getHeader().setCaption(caption);
    }

    if (!isService && DataUtils.hasId(row)
        && BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.TRADE))) {

      HasWidgets panel = getStockByWarehousePanel();

      if (panel != null) {
        panel.clear();
        long id = row.getId();

        TradeKeeper.getItemStockByWarehouse(id, list -> {
          if (!BeeUtils.isEmpty(list) && Objects.equals(getActiveRowId(), id)) {
            Widget widget = TradeUtils.renderItemStockByWarehouse(id, list);

            if (widget != null) {
              panel.clear();
              panel.add(widget);
            }
          }
        });
      }
    }

    super.afterRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new ItemForm();
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    ItemsGrid gridHandler = getItemGridHandler(form);

    if (gridHandler != null && gridHandler.showServices()) {
      newRow.setValue(form.getDataIndex(ClassifierConstants.COL_ITEM_IS_SERVICE), 1);
    }
  }

  private static ItemsGrid getItemGridHandler(FormView form) {
    if (form.getViewPresenter() instanceof GridFormPresenter) {
      GridInterceptor gic = ((GridFormPresenter) form.getViewPresenter()).getGridInterceptor();

      if (gic instanceof ItemsGrid) {
        return (ItemsGrid) gic;
      }
    }
    return null;
  }

  private HasWidgets getStockByWarehousePanel() {
    Widget panel = getWidgetByName("StockByWarehouse");

    if (panel instanceof HasWidgets) {
      return (HasWidgets) panel;
    } else {
      return null;
    }
  }
}