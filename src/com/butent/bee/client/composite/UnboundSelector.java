package com.butent.bee.client.composite;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HandlesRendering;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Launchable;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public final class UnboundSelector extends DataSelector implements HandlesRendering, Launchable {

  public static UnboundSelector create(Relation relation) {
    Assert.notNull(relation);
    return create(relation, relation.getChoiceColumns());
  }

  public static UnboundSelector create(Relation relation, AbstractCellRenderer renderer) {
    Assert.notNull(relation);
    Assert.notNull(renderer);

    UnboundSelector selector = new UnboundSelector(relation);
    selector.setRenderer(renderer);

    return selector;
  }

  public static UnboundSelector create(Relation relation, List<String> renderColumns) {
    Assert.notNull(relation);
    return create(relation, RendererFactory.createRenderer(relation.getViewName(), renderColumns));
  }

  public static UnboundSelector create(String viewName, List<String> columns) {
    Assert.notEmpty(viewName);
    Assert.notEmpty(columns);
    return create(Relation.create(viewName, columns));
  }

  private AbstractCellRenderer renderer;
  private String renderedValue;

  private boolean handledByForm;

  private UnboundSelector(Relation relation) {
    super(relation, true);
  }

  @Override
  public void clearValue() {
    super.clearValue();
    setRenderedValue(null);
  }

  @Override
  public String getIdPrefix() {
    return "unbound";
  }

  public String getRenderedValue() {
    return renderedValue;
  }

  @Override
  public AbstractCellRenderer getRenderer() {
    return renderer;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.UNBOUND_SELECTOR;
  }

  @Override
  public void launch() {
    setHandledByForm(true);
  }

  @Override
  public void render(IsRow row) {
    if (getRenderer() != null) {
      if (row == null) {
        setRenderedValue(null);
      } else {
        setRenderedValue(getRenderer().render(row));
      }
    }
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
    updateDisplay(getRenderedValue());
  }

  @Override
  public void setRenderer(AbstractCellRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public void setSelection(BeeRow row, Value value, boolean fire) {
    if (value == null) {
      render(row);
    } else {
      setRenderedValue(value.getString());
    }

    super.setSelection(row, value, fire);
  }

  public void setValue(Long id, final boolean fire) {
    if (DataUtils.isId(id)) {
      Queries.getRow(getOracle().getViewName(), id, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          setSelection(result, null, fire);
          updateDisplay(getRenderedValue());
        }
      });

    } else {
      setSelection(null, null, fire);
      updateDisplay(getRenderedValue());
    }
  }

  @Override
  public void setValue(String value) {
    super.setValue(value);

    if (getValueType() == ValueType.TEXT) {
      setRenderedValue(value);
      updateDisplay(value);
    }
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return validate(getNormalizedValue(), checkForNull);
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    List<String> messages = new ArrayList<>();
    messages.addAll(super.validate(normalizedValue, checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    if (BeeUtils.isEmpty(normalizedValue) && checkForNull && !isNullable()) {
      if (!BeeUtils.isEmpty(getRelationLabel())) {
        messages.add(getRelationLabel());
      }
      messages.add(Localized.dictionary().valueRequired());
    }
    return messages;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (!isHandledByForm()) {
      addFocusHandler(event -> setEditing(true));
    }

    addBlurHandler(event -> {
      if (isStrict()) {
        updateDisplay(getRenderedValue());
      }
      if (!isHandledByForm()) {
        setEditing(false);
      }
    });

    addEditStopHandler(event -> {
      if (getRenderer() != null) {
        if (getRelatedRow() != null || isStrict()) {
          if (event.isChanged()) {
            render(getRelatedRow());
          }
          updateDisplay(getRenderedValue());
        }

        if (!isHandledByForm()) {
          UiHelper.moveFocus(getParent(), getElement(), true);
        }
      }
    });
  }

  private boolean isHandledByForm() {
    return handledByForm;
  }

  private void setHandledByForm(boolean handledByForm) {
    this.handledByForm = handledByForm;
  }

  private void setRenderedValue(String renderedValue) {
    this.renderedValue = renderedValue;
  }
}
