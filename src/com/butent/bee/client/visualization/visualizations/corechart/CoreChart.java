package com.butent.bee.client.visualization.visualizations.corechart;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;

import com.butent.bee.client.visualization.AbstractDataTable;
import com.butent.bee.client.visualization.Selectable;
import com.butent.bee.client.visualization.Selection;
import com.butent.bee.client.visualization.events.Handler;
import com.butent.bee.client.visualization.events.OnMouseOutHandler;
import com.butent.bee.client.visualization.events.OnMouseOverHandler;
import com.butent.bee.client.visualization.events.ReadyHandler;
import com.butent.bee.client.visualization.events.SelectHandler;
import com.butent.bee.client.visualization.visualizations.Visualization;

/**
 * Serves as an abstract class for main visualization types and contains necessary methods for their
 * implementation.
 */

public abstract class CoreChart extends Visualization<Options> implements Selectable {

  /**
   * Contains a list of possible visualization types.
   */

  public enum Type {
    AREA, LINE, SCATTER, BARS, COLUMNS, PIE, NONE
  }

  public static final String PACKAGE = "corechart";

  public static Options createOptions() {
    return JavaScriptObject.createObject().cast();
  }

  public CoreChart() {
    super();
  }

  public CoreChart(AbstractDataTable data, Options options) {
    super(data, options);
  }

  public final void addOnMouseOutHandler(OnMouseOutHandler handler) {
    Handler.addHandler(this, "onmouseout", handler);
  }

  public final void addOnMouseOverHandler(OnMouseOverHandler handler) {
    Handler.addHandler(this, "onmouseover", handler);
  }

  public final void addReadyHandler(ReadyHandler handler) {
    Handler.addHandler(this, "ready", handler);
  }

  @Override
  public final void addSelectHandler(SelectHandler handler) {
    Selection.addSelectHandler(this, handler);
  }

  @Override
  public final JsArray<Selection> getSelections() {
    return Selection.getSelections(this);
  }

  @Override
  public final void setSelections(JsArray<Selection> sel) {
    Selection.setSelections(this, sel);
  }

  @Override
  protected native JavaScriptObject createJso(Element parent) /*-{
    return new $wnd.google.visualization.CoreChart(parent);
  }-*/;
}
