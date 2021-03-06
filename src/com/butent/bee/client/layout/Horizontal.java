package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains a panel that lays all of its widgets out in a single horizontal row.
 */

public class Horizontal extends CellVector {

  private final Element tableRow;

  public Horizontal() {
    super();
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "Horizontal");

    tableRow = DOM.createTR();
    DOM.appendChild(getBody(), tableRow);
  }

  public Horizontal(String styleName) {
    this();
    if (!BeeUtils.isEmpty(styleName)) {
      addStyleName(styleName);
    }
  }

  @Override
  public void add(Widget w) {
    Element td = createDefaultCell();
    DOM.appendChild(tableRow, td);
    add(w, td);
  }

  @Override
  public String getIdPrefix() {
    return "hor";
  }

  @Override
  public void insert(Widget w, int beforeIndex) {
    checkIndexBoundsForInsertion(beforeIndex);

    Element td = createDefaultCell();
    DOM.insertChild(tableRow, td, beforeIndex);
    insert(w, td, beforeIndex, false);
  }

  @Override
  public boolean remove(Widget w) {
    Element td = w.getElement().getParentElement();
    boolean removed = super.remove(w);
    if (removed) {
      tableRow.removeChild(td);
    }
    return removed;
  }
}
