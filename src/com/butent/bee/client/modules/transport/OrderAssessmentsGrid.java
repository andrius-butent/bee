package com.butent.bee.client.modules.transport;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.view.grid.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.GridInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.ui.GridDescription;

public class OrderAssessmentsGrid extends AbstractGridInterceptor {

  private final Long userId;

  public OrderAssessmentsGrid() {
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public GridInterceptor getInstance() {
    return new OrderAssessmentsGrid();
  }

  @Override
  public String getRowCaption(IsRow row, boolean edit) {
    if (edit) {
      Long parent = row.getLong(DataUtils.getColumnIndex("Assessor",
          getGridPresenter().getDataColumns()));
      return DataUtils.isId(parent) ? "Vertinimas" : "Užklausimas";
    } else {
      return super.getRowCaption(row, edit);
    }
  }

  @Override
  public boolean onLoad(GridDescription gridDescription) {
    gridDescription.setFilter(ComparisonFilter.isEqual("Manager", new LongValue(userId)));
    return true;
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
    newRow.setValue(DataUtils.getColumnIndex("Manager", gridView.getDataColumns()), userId);
    return true;
  }
}
