package com.butent.bee.egg.client.layout;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.egg.client.dom.DomUtils;
import com.butent.bee.egg.client.event.HasAfterAddHandler;
import com.butent.bee.egg.client.event.HasBeforeAddHandler;
import com.butent.bee.egg.shared.HasId;

public class BeeStack extends StackLayoutPanel implements HasId {

  public BeeStack(Unit unit) {
    super(unit);
    createId();
  }
  
  public void createId() {
    DomUtils.createId(this, "stack");
  }

  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public void insert(Widget child, Widget header, double headerSize,
      int beforeIndex) {
    Widget cw = child;
    if (cw instanceof HasBeforeAddHandler) {
      cw = ((HasBeforeAddHandler) cw).onBeforeAdd(this);
    }
    Widget hw = header;
    if (hw instanceof HasBeforeAddHandler) {
      hw = ((HasBeforeAddHandler) hw).onBeforeAdd(this);
    }
    
    super.insert(cw, hw, headerSize, beforeIndex);
    
    if (cw instanceof HasAfterAddHandler) {
      ((HasAfterAddHandler) cw).onAfterAdd(this);
    }
    if (hw instanceof HasAfterAddHandler) {
      ((HasAfterAddHandler) hw).onAfterAdd(this);
    }
  }

  public void setId(String id) {
    DomUtils.setId(this, id);
  }

}
