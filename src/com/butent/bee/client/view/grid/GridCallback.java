package com.butent.bee.client.view.grid;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.grid.AbstractColumn;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.WidgetCallback;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GridCallback extends WidgetCallback {

  void afterCreate(CellGrid grid);

  boolean afterCreateColumn(String columnId, AbstractColumn<?> column, ColumnHeader header,
      ColumnFooter footer);

  void afterCreateColumns(CellGrid grid);
  
  boolean beforeAddRow(GridPresenter presenter);

  void beforeCreate(List<BeeColumn> dataColumns, int rowCount, GridDescription gridDescription,
      boolean hasSearch);
  
  boolean beforeCreateColumn(String columnId, List<BeeColumn> dataColumns,
      ColumnDescription columnDescription);

  void beforeCreateColumns(List<BeeColumn> dataColumns, List<ColumnDescription> columnDescriptions);
  
  int beforeDeleteRow(GridPresenter presenter, IsRow row);

  int beforeDeleteRows(GridPresenter presenter, IsRow activeRow, Collection<RowInfo> selectedRows);

  void beforeRefresh(GridPresenter presenter);

  void beforeRequery(GridPresenter presenter);

  String getCaption();
  
  Map<String, Filter> getInitialFilters();
  
  GridCallback getInstance();
  
  boolean onClose(GridPresenter presenter);
  
  boolean onLoad(GridDescription gridDescription);

  boolean onLoadExtWidget(Element root);
  
  void onShow(GridPresenter presenter);

  boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow);
}
