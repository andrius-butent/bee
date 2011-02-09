package com.butent.bee.client.data;

import com.google.gwt.core.client.JsArrayString;

import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.shared.ArraySequence;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.StringMatrix;
import com.butent.bee.shared.data.StringRow;

public class JsData<ColType extends IsColumn> extends StringMatrix<ColType> {

  public JsData(JsArrayString data, ColType... columns) {
    this(data, 0, columns);
  }

  public JsData(JsArrayString data, String... columnLabels) {
    this(data, 0, columnLabels);
  }
  
  public JsData(JsArrayString data, int start, ColType... columns) {
    super(columns);
    initData(data, start, columns.length);
  }

  public JsData(JsArrayString data, int start, String... columnLabels) {
    super(columnLabels);
    initData(data, start, columnLabels.length);
  }
  
  private void initData(JsArrayString data, int start, int rowSize) {
    Assert.isPositive(rowSize);
    int rc = (data.length() - start) / rowSize;

    setRows(new ArraySequence<StringRow>(new StringRow[rc]));
    for (int i = 0; i < rc; i++) {
      getRows().set(i, new StringRow(new JsStringSequence(JsUtils.slice(data, 
          start + i * rowSize, start + (i + 1) * rowSize))));
    }
  }
}
