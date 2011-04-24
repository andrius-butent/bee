package com.butent.bee.client.view;

import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.data.BeeColumn;

import java.util.List;

public interface GridContainerView extends View {
  void create(String caption, List<BeeColumn> dataColumns, int rowCount);
  
  GridView getContent();
}
