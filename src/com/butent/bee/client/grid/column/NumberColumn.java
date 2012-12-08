package com.butent.bee.client.grid.column;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.grid.cell.NumberCell;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;

/**
 * Manages value and number format of a number type column.
 */

public abstract class NumberColumn<C extends Number> extends DataColumn<C> implements
    HasNumberFormat {

  public NumberColumn(Cell<C> cell, CellSource cellSource) {
    super(cell, cellSource);
    setHorizontalAlignment(ALIGN_RIGHT);
  }

  public NumberColumn(NumberFormat format, CellSource cellSource) {
    this(new NumberCell<C>(format), cellSource);
  }

  @Override
  public NumberFormat getNumberFormat() {
    if (getCell() instanceof HasNumberFormat) {
      return ((HasNumberFormat) getCell()).getNumberFormat();
    }
    return null;
  }

  @Override
  public C getValue(IsRow row) {
    if (row == null) {
      return null;
    }
    return getNumber(row);
  }

  @Override
  public void setNumberFormat(NumberFormat format) {
    if (getCell() instanceof HasNumberFormat) {
      ((HasNumberFormat) getCell()).setNumberFormat(format);
    }
  }

  protected abstract C getNumber(IsRow row);
}
