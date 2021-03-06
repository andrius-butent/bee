package com.butent.bee.client.view;

import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.data.HasActiveRow;
import com.butent.bee.client.data.HasDataRows;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;

import java.util.List;
import java.util.Map;

public interface DataView extends View, NotificationListener, HasViewName, HasActiveRow,
    HasDataRows {

  HandlerRegistration addCellValidationHandler(String columnId, CellValidateEvent.Handler handler);

  void finishNewRow(IsRow row);

  boolean isFlushable();

  List<BeeColumn> getDataColumns();

  default int getDataIndex(String source) {
    return DataUtils.getColumnIndex(source, getDataColumns());
  }

  Map<String, String> getProperties();

  String getProperty(String key);

  void refresh(boolean refreshChildren, boolean focus);

  int refreshBySource(String source);

  void startNewRow(boolean copy);
}
