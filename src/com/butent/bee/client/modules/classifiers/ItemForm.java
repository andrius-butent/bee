package com.butent.bee.client.modules.classifiers;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;

import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.modules.trade.TradeConstants;
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

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

class ItemForm extends AbstractFormInterceptor {

	@Override
	public void afterCreateWidget(String name, IdentifiableWidget widget, FormFactory.WidgetDescriptionCallback callback) {

		if (BeeUtils.same(name, TBL_ADDITION_STOCKS) && widget instanceof ChildGrid) {
			((ChildGrid) widget).setGridInterceptor(getGridInterceptor());

		} else if (BeeUtils.same(name, TBL_WRITE_OFF_STOCKS) && widget instanceof ChildGrid) {
			((ChildGrid) widget).setGridInterceptor(getGridInterceptor());
		}

		super.afterCreateWidget(name, widget, callback);
	}

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

    if (DataUtils.hasId(row) && isService) {
    	getStocksInWarehouse();
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

  private void getStocksInWarehouse() {

	  final FormView form = getFormView();
	  final Widget widget = form.getWidgetByName(WIDGET_STOCKS_IN_WAREHOUSE, false);

	  if (widget != null && getActiveRow() != null && DataUtils.isId(getActiveRowId())) {
	  	ParameterList params = ClassifierKeeper.createArgs(SVC_GET_STOCKS_IN_WAREHOUSE);
	  	params.addDataItem(COL_ITEM, getActiveRow().getId());

	    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
		    @Override
		    public void onResponse(ResponseObject response) {
			    if (response.hasErrors()) {
			    	return;
			    }

				((InputNumber) widget).setValue(response.getResponseAsString());
		    }
	    });
	  }
  }

  private HasWidgets getStockByWarehousePanel() {
    Widget panel = getWidgetByName("StockByWarehouse");

    if (panel instanceof HasWidgets) {
      return (HasWidgets) panel;
    } else {
      return null;
    }
  }

  private GridInterceptor getGridInterceptor(){
	return new AbstractGridInterceptor() {

		@Override
		public void afterUpdateRow(IsRow result) {
			getStocksInWarehouse();
			super.afterUpdateRow(result);
		}

		@Override
		public boolean onRowCountChange(GridView gridView, RowCountChangeEvent event) {
			getStocksInWarehouse();
			return super.onRowCountChange(gridView, event);
		}

		@Override
		public void afterInsertRow(IsRow result) {
			getGridPresenter().refresh(true, true);

			if (BeeUtils.same(getGridView().getGridName(), TBL_ADDITION_STOCKS)) {
				Double price = Data.getDouble(TBL_ADDITION_STOCKS, result, COL_ITEM_PRICE);

				if (price != null) {
					Queries.updateCellAndFire(VIEW_ITEMS, ItemForm.this.getActiveRowId(),
						ItemForm.this.getActiveRow().getVersion(), COL_ITEM_COST,null, BeeUtils.toString(price));
				}
			}

			super.afterInsertRow(result);
		}
	};
  }
}