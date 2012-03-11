package com.butent.bee.client.widget;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextArea;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.HasInputHandlers;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.ui.HandlesAfterSave;
import com.butent.bee.client.ui.HasTextDimensions;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.shared.BeeResource;
import com.butent.bee.shared.State;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Implements a text area that allows multiple lines of text to be entered.
 */

public class InputArea extends TextArea implements Editor, HandlesAfterSave, HasTextDimensions,
    HasInputHandlers {

  private BeeResource resource = null;

  private String digest = null;

  private boolean nullable = true;

  private boolean editing = false;

  private boolean editorInitialized = false;

  public InputArea() {
    super();
    init();
  }

  public InputArea(BeeResource resource) {
    this();
    this.resource = resource;

    setValue(resource.getContent());
    if (resource.isReadOnly()) {
      setReadOnly(true);
    }
  }

  public InputArea(Element element) {
    super(element);
    init();
  }

  public HandlerRegistration addEditStopHandler(EditStopEvent.Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }

  public HandlerRegistration addInputHandler(InputHandler handler) {
    return Binder.addInputHandler(this, handler);
  }

  public String getDigest() {
    return digest;
  }

  public String getId() {
    return DomUtils.getId(this);
  }

  public String getIdPrefix() {
    return "area";
  }

  public String getNormalizedValue() {
    String v = getValue();
    if (BeeUtils.isEmpty(v) && isNullable()) {
      return null;
    } else {
      return BeeUtils.trimRight(v);
    }
  }

  public BeeResource getResource() {
    return resource;
  }

  public boolean handlesKey(int keyCode) {
    return !BeeUtils.inList(keyCode, KeyCodes.KEY_ESCAPE, KeyCodes.KEY_TAB);
  }

  public boolean isEditing() {
    return editing;
  }

  public boolean isNullable() {
    return nullable;
  }

  public boolean isValueChanged() {
    String v = getValue();
    String d = getDigest();

    if (BeeUtils.isEmpty(v)) {
      return !BeeUtils.isEmpty(d);
    } else if (BeeUtils.isEmpty(d)) {
      return true;
    } else {
      return !d.equals(JsUtils.md5(v));
    }
  }

  public void onAfterSave(String opt) {
    if (BeeUtils.isEmpty(opt)) {
      updateDigest();
    } else {
      setDigest(opt);
    }
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (isEditing() && UiHelper.isSave(event)) {
      event.preventDefault();
      fireEvent(new EditStopEvent(State.CHANGED));
      return;
    }
    super.onBrowserEvent(event);
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public void setResource(BeeResource resource) {
    this.resource = resource;
  }

  @Override
  public void setValue(String value) {
    super.setValue(value);
    updateDigest(getValue());
  }

  public void startEdit(String oldValue, char charCode, EditorAction onEntry) {
    if (!isEditorInitialized()) {
      initEditor();
      setEditorInitialized(true);
    }

    EditorAction action = (onEntry == null) ? EditorAction.ADD_LAST : onEntry;
    UiHelper.doEditorAction(this, oldValue, charCode, action);
  }

  public String updateDigest() {
    return updateDigest(getValue());
  }

  public String updateDigest(String value) {
    if (BeeUtils.isEmpty(value)) {
      setDigest(null);
    } else {
      setDigest(JsUtils.md5(value));
    }
    return getDigest();
  }

  public String validate() {
    return null;
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
    setStyleName("bee-InputArea");
  }

  private void initEditor() {
    UiHelper.registerSave(this);
  }

  private boolean isEditorInitialized() {
    return editorInitialized;
  }

  private void setEditorInitialized(boolean editorInitialized) {
    this.editorInitialized = editorInitialized;
  }
}
