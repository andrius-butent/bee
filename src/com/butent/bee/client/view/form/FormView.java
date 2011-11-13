package com.butent.bee.client.view.form;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.data.HasDataTable;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory.FormCallback;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.add.HasAddEndHandlers;
import com.butent.bee.client.view.add.HasAddStartHandlers;
import com.butent.bee.client.view.add.HasReadyForInsertHandlers;
import com.butent.bee.client.view.edit.HasReadyForUpdateHandlers;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfo;

import java.util.List;

/**
 * Contains necessary methods for form implementing classes.
 */

public interface FormView extends DataView, HasDataTable,
    HasAddStartHandlers, HasAddEndHandlers, HasReadyForInsertHandlers, HasReadyForUpdateHandlers {

  void applyOptions(String options);

  void create(FormDescription formDescription, List<BeeColumn> dataColumns, FormCallback callback);

  RowInfo getActiveRowInfo();
  
  String getCaption();

  List<BeeColumn> getDataColumns();

  int getDataIndex(String source);

  HasDataTable getDisplay();

  FormCallback getFormCallback();

  IsRow getRow();

  JavaScriptObject getRowJso();

  Widget getWidgetBySource(String source);

  boolean isRowEditable(boolean warn);

  void prepareForInsert();

  void refreshCellContent(String columnSource);
  
  void setRow(IsRow row);

  void showChildren(boolean show);

  void start(Integer rowCount);

  void updateCell(String columnId, String newValue);

  void updateRow(IsRow row);
}