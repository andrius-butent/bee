package com.butent.bee.client.grid.cell;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.shared.utils.BeeUtils;

public class SelectionCell extends AbstractCell<Boolean> {

  private static final InputElement INPUT;

  static {
    INPUT = Document.get().createCheckInputElement();
    INPUT.addClassName("bee-SelectionCell");
  }

  public SelectionCell() {
    super();
  }

  @Override
  public void onBrowserEvent(CellContext context, Element parent, Boolean value,
      NativeEvent event) {
  }

  @Override
  public void render(CellContext context, Boolean value, SafeHtmlBuilder sb) {
    boolean b = BeeUtils.unbox(value);

    INPUT.setChecked(b);
    INPUT.setDefaultChecked(b);

    sb.append(SafeHtmlUtils.fromTrustedString(INPUT.getString()));
  }

  public void update(Element cellElement, boolean value) {
    if (cellElement == null) {
      return;
    }
    DomUtils.setCheckValue(cellElement, value);
  }
}
