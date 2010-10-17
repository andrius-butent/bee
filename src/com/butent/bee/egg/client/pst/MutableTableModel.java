package com.butent.bee.egg.client.pst;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * A mutable version of the {@link TableModel} that supports inserting and
 * removing rows and setting cell data.
 * 
 * @param <RowType> the data type of the row values
 */
public abstract class MutableTableModel<RowType> extends TableModel<RowType> implements
    HasRowInsertionHandlers, HasRowRemovalHandlers, HasRowValueChangeHandlers<RowType> {
  public HandlerRegistration addRowInsertionHandler(RowInsertionHandler handler) {
    return addHandler(RowInsertionEvent.getType(), handler);
  }

  public HandlerRegistration addRowRemovalHandler(RowRemovalHandler handler) {
    return addHandler(RowRemovalEvent.getType(), handler);
  }

  public HandlerRegistration addRowValueChangeHandler(RowValueChangeHandler<RowType> handler) {
    return addHandler(RowValueChangeEvent.getType(), handler);
  }

  /**
   * Insert a row and increment the row count by one.
   * 
   * @param beforeRow the row index of the new row
   * TODO (jlabanca): should this require a row value?
   */
  public void insertRow(int beforeRow) {
    if (onRowInserted(beforeRow)) {
      // Fire listeners
      fireEvent(new RowInsertionEvent(beforeRow));

      // Increment the row count
      int numRows = getRowCount();
      if (numRows != UNKNOWN_ROW_COUNT) {
        setRowCount(numRows + 1);
      }
    }
  }

  /**
   * Remove a row and decrement the row count by one.
   * 
   * @param row the row index of the removed row
   */
  public void removeRow(int row) {
    if (onRowRemoved(row)) {
      // Fire listeners
      fireEvent(new RowRemovalEvent(row));

      // Decrement the row count
      int numRows = getRowCount();
      if (numRows != UNKNOWN_ROW_COUNT) {
        setRowCount(numRows - 1);
      }
    }
  }

  /**
   * Set a new row value.
   * 
   * @param row the row index
   * @param rowValue the new row value at this row
   */
  public void setRowValue(int row, RowType rowValue) {
    if (onSetRowValue(row, rowValue)) {
      // Fire the listeners
      fireEvent(new RowValueChangeEvent<RowType>(row, rowValue));

      // Update the row count
      int numRows = getRowCount();
      if (numRows != UNKNOWN_ROW_COUNT && row >= numRows) {
        setRowCount(row + 1);
      }
    }
  }

  /**
   * Event fired when a row is inserted. Returning true will increment the row
   * count by one.
   * 
   * @param beforeRow the row index of the new row
   * @return true if the action is successful
   */
  protected abstract boolean onRowInserted(int beforeRow);

  /**
   * Event fired when a row is removed. Returning true will decrement the row
   * count by one.
   * 
   * @param row the row index of the removed row
   * @return true if the action is successful
   */
  protected abstract boolean onRowRemoved(int row);

  /**
   * Event fired when the local data changes. Returning true will ensure that
   * the row count is at least as one greater than the row index.
   * 
   * @param row the row index
   * @param rowValue the new row value at this row
   * @return true if the action is successful
   */
  protected abstract boolean onSetRowValue(int row, RowType rowValue);
}
