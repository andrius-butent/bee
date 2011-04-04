package com.butent.bee.client.logging;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.layout.Flow;

public class LogArea extends Flow {

  public LogArea() {
    super();
    setStyleName("bee-LogArea");
    sinkEvents(Event.ONCLICK);
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (EventUtils.hasModifierKey(event) && getWidgetCount() > 0) {
      Widget target = DomUtils.getWidget(this, Element.as(event.getEventTarget()));
      EventUtils.eatEvent(event);

      if (target == null || equals(target) || getWidgetCount() <= 1) {
        clear();
      } else {
        Widget child;
        while (getWidgetCount() > 0) {
          child = getWidget(0);
          if (target.equals(child)) {
            break;
          }
          remove(child);
        }
      }
      return;
    }
    super.onBrowserEvent(event);
  }
}
