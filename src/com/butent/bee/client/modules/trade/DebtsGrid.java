package com.butent.bee.client.modules.trade;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

public class DebtsGrid extends AbstractGridInterceptor {

  private static final LocalizableConstants localizedConstants = Localized.getConstants();

  @Override
  public AbstractFilterSupplier getFilterSupplier(String columnName,
      ColumnDescription columnDescription) {

    if (BeeUtils.same(columnName, TradeConstants.COL_SALE_LASTEST_PAYMENT)) {
      BeeColumn filterColumn = Data.getColumn(TradeConstants.VIEW_DEBTS, columnName);

      return new NoPaymentPeriodFilterSuppler(getViewName(), filterColumn,
          localizedConstants.trdNoPaymentPeriod(), columnDescription.getFilterOptions());
    } else {
      return super.getFilterSupplier(columnName, columnDescription);
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new DebtsGrid();
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    IsRow activeRow = event.getRowValue();

    if (activeRow == null) {
      return;
    }

    if (TradeConstants.PROP_AVERAGE_OVERDUE.equals(event.getColumnId())) {
      GridFactory.openGrid(TradeConstants.GRID_SALES,
          GridFactory.getGridInterceptor(TradeConstants.GRID_SALES),
          /*
           * GridOptions.forFilter(Filter.compareWithValue(TradeConstants.COL_TRADE_CUSTOMER,
           * Operator.EQ, Value.getValue(activeRow.getId())))
           */null, PresenterCallback.SHOW_IN_NEW_TAB);
    }

  }

}
