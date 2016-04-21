package com.butent.bee.client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Consumer;

public final class Opener {

  public static final Opener MODAL = new Opener(true, null);

  public static final Opener NEW_TAB = new Opener(PresenterCallback.SHOW_IN_NEW_TAB);
  private static final Opener UPDATE = new Opener(PresenterCallback.SHOW_IN_ACTIVE_PANEL);

  public static Opener modal(Consumer<FormView> onOpen) {
    if (onOpen == null) {
      return MODAL;
    } else {
      return new Opener(true, null, null, onOpen);
    }
  }

  public static Opener modeless() {
    if (BeeKeeper.getUser().openInNewTab()) {
      return NEW_TAB;
    } else {
      return UPDATE;
    }
  }

  public static Opener newTab(Consumer<FormView> onOpen) {
    if (onOpen == null) {
      return NEW_TAB;
    } else {
      return new Opener(false, null, NEW_TAB.getPresenterCallback(), onOpen);
    }
  }

  public static Opener relativeTo(Element element) {
    return new Opener(true, element);
  }

  public static Opener relativeTo(UIObject obj) {
    if (obj == null) {
      return MODAL;
    } else {
      return relativeTo(obj.getElement());
    }
  }

  public static Opener with(PresenterCallback callback) {
    Assert.notNull(callback);
    return new Opener(callback);
  }

  private final boolean modal;
  private final Element target;

  private final PresenterCallback presenterCallback;
  private final Consumer<FormView> onOpen;

  private Opener(boolean modal, Element target) {
    this(modal, target, null, null);
  }

  private Opener(boolean modal, Element target, PresenterCallback presenterCallback,
      Consumer<FormView> onOpen) {

    this.modal = modal;
    this.target = target;
    this.presenterCallback = presenterCallback;
    this.onOpen = onOpen;
  }

  private Opener(PresenterCallback presenterCallback) {
    this(false, null, presenterCallback, null);
  }

  public Consumer<FormView> getOnOpen() {
    return onOpen;
  }

  public PresenterCallback getPresenterCallback() {
    return presenterCallback;
  }

  public Element getTarget() {
    return target;
  }

  public boolean isModal() {
    return modal;
  }
}
