package com.butent.bee.egg.client.widget;

import com.butent.bee.egg.client.utils.BeeDom;
import com.butent.bee.egg.shared.HasId;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.user.cellview.client.CellList;

public class BeeCellList<T> extends CellList<T> implements HasId {

  public BeeCellList(Cell<T> cell,
      com.google.gwt.user.cellview.client.CellList.Resources resources) {
    super(cell, resources);
    createId();
  }

  public BeeCellList(Cell<T> cell) {
    super(cell);
    createId();
  }

  public String getId() {
    return BeeDom.getId(this);
  }

  public void setId(String id) {
    BeeDom.setId(this, id);
  }

  public void createId() {
    BeeDom.createId(this, "celllist");
  }

}
