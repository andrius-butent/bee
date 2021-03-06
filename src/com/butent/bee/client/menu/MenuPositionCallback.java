package com.butent.bee.client.menu;

import com.google.gwt.dom.client.Element;

import com.butent.bee.client.dialog.Popup.PositionCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Waits for menu position changes and sets them.
 */

public class MenuPositionCallback implements PositionCallback {

  private final Element parent;
  private final Element item;
  private final MenuPopup popup;
  private final boolean vertical;

  public MenuPositionCallback(Element parent, Element item, MenuPopup popup,
      boolean vertical) {
    this.parent = parent;
    this.item = item;
    this.popup = popup;
    this.vertical = vertical;
  }

  @Override
  public void setPosition(int offsetWidth, int offsetHeight) {
    Assert.notNull(parent);
    Assert.notNull(popup);

    int x;
    int y;

    if (vertical) {
      x = parent.getAbsoluteLeft() + parent.getOffsetWidth() + 5;
      y = item.getAbsoluteTop();
      if (y <= 0) {
        y = parent.getAbsoluteTop();
      }
    } else {
      x = item.getAbsoluteLeft();
      if (x <= 0) {
        x = parent.getAbsoluteLeft();
      }
      y = item.getAbsoluteBottom() + 5;
      if (y <= 0) {
        y = parent.getAbsoluteBottom();
      }
    }

    x = BeeUtils.fitStart(x, offsetWidth, DomUtils.getClientWidth());
    y = BeeUtils.fitStart(y, offsetHeight, DomUtils.getClientHeight());

    popup.setPopupPosition(x, y);
  }
}
