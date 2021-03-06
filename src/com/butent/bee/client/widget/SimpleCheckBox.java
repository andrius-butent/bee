package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Implements a checkbox user interface component without label.
 */

public class SimpleCheckBox extends FocusWidget implements BooleanWidget {

  private boolean valueChangeHandlerInitialized;

  public SimpleCheckBox() {
    setElement(Document.get().createCheckInputElement());
    init();
  }

  public SimpleCheckBox(boolean value) {
    this();
    setValue(value);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
    if (!valueChangeHandlerInitialized) {
      ensureDomEventHandlers();
      valueChangeHandlerInitialized = true;
    }
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "bool";
  }

  @Override
  public Boolean getValue() {
    return getInputElement().isChecked();
  }

  @Override
  public boolean isChecked() {
    return getValue();
  }

  @Override
  public void setChecked(boolean checked) {
    setValue(checked);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setValue(Boolean value) {
    setValue(value, false);
  }

  @Override
  public void setValue(Boolean value, boolean fireEvents) {
    boolean b = BeeUtils.unbox(value);
    boolean oldValue = BeeUtils.unbox(getValue());

    getInputElement().setChecked(b);

    if (fireEvents && (b != oldValue)) {
      ValueChangeEvent.fire(this, b);
    }
  }

  protected void ensureDomEventHandlers() {
    addClickHandler(event -> ValueChangeEvent.fire(SimpleCheckBox.this, getValue()));
  }

  private InputElement getInputElement() {
    return InputElement.as(getElement());
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
    setStyleName(BeeConst.CSS_CLASS_PREFIX + "SimpleCheckBox");

    EventUtils.preventClickDebouncer(this);
  }
}
