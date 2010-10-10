package com.butent.bee.egg.client.menu;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.egg.client.dom.DomUtils;
import com.butent.bee.egg.shared.HasId;

public class BeeMenuItem extends UIObject implements HasId {
  public static enum ITEM_TYPE {
    LABEL, BUTTON, RADIO, HTML, OPTION, LI, DT, DD
  }

  public static ITEM_TYPE defaultType = ITEM_TYPE.LABEL;

  private static final String STYLENAME_DEFAULT = "bee-MenuItem";
  private static final String STYLENAME_SELECTED = "selected";

  private MenuCommand command;
  private BeeMenuBar parentMenu, subMenu;

  private ITEM_TYPE itemType;

  public BeeMenuItem(BeeMenuBar parent, String text, BeeMenuBar subMenu) {
    init(parent, text, getDefaultType(parent));
    setSubMenu(subMenu);
  }

  public BeeMenuItem(BeeMenuBar parent, String text, ITEM_TYPE type,
      BeeMenuBar subMenu) {
    init(parent, text, type);
    setSubMenu(subMenu);
  }

  public BeeMenuItem(BeeMenuBar parent, String text, ITEM_TYPE type,
      MenuCommand cmd) {
    init(parent, text, type);
    setCommand(cmd);
  }

  public BeeMenuItem(BeeMenuBar parent, String text, MenuCommand cmd) {
    init(parent, text, getDefaultType(parent));
    setCommand(cmd);
  }

  public void createId() {
    DomUtils.createId(this, "menuitem");
  }

  public MenuCommand getCommand() {
    return command;
  }

  public String getId() {
    return DomUtils.getId(this);
  }

  public ITEM_TYPE getItemType() {
    return itemType;
  }

  public BeeMenuBar getParentMenu() {
    return parentMenu;
  }

  public BeeMenuBar getSubMenu() {
    return subMenu;
  }

  public void setCommand(MenuCommand cmd) {
    command = cmd;
  }

  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setItemType(ITEM_TYPE it) {
    this.itemType = it;
  }

  public void setParentMenu(BeeMenuBar parentMenu) {
    this.parentMenu = parentMenu;
  }

  public void setSelected(boolean selected) {
    if (selected) {
      addStyleDependentName(STYLENAME_SELECTED);
    } else {
      removeStyleDependentName(STYLENAME_SELECTED);
    }

    switch (getItemType()) {
      case RADIO:
        DomUtils.setCheckValue(getElement(), selected);
        break;
      case OPTION:
        DomUtils.setSelected(getElement(), selected);
        break;
      default:
    }
  }

  public void setSubMenu(BeeMenuBar subMenu) {
    this.subMenu = subMenu;
    if (this.parentMenu != null) {
      this.parentMenu.updateSubmenuIcon(this);
    }

    if (subMenu != null) {
      subMenu.getElement().setTabIndex(-1);
    }
  }

  private ITEM_TYPE getDefaultType(BeeMenuBar parent) {
    ITEM_TYPE w = null;

    if (parent != null) {
      w = parent.getDefaultItemType();
    }
    if (w == null) {
      w = defaultType;
    }

    return w;
  }

  private void init(BeeMenuBar parent, String text, ITEM_TYPE type) {
    Element elem;

    switch (type) {
      case HTML:
        elem = DomUtils.createHtml(text).cast();
        break;
      case BUTTON:
        elem = DomUtils.createButton(text).cast();
        break;
      case RADIO:
        elem = DomUtils.createRadio(parent.getName(), text).cast();
        break;
      case OPTION:
        elem = DomUtils.createOption(text).cast();
        break;
      case LI:
        elem = DomUtils.createListItem(text).cast();
        break;
      case DT:
        elem = DomUtils.createDefinitionItem(true, text).cast();
        break;
      case DD:
        elem = DomUtils.createDefinitionItem(false, text).cast();
        break;
      default:
        elem = DomUtils.createLabel(text).cast();
    }

    setElement(elem);

    setStyleName(STYLENAME_DEFAULT);
    addStyleDependentName(type.toString().toLowerCase());

    createId();

    setParentMenu(parent);
    setItemType(type);
  }

}
