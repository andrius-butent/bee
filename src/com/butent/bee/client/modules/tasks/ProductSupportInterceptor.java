package com.butent.bee.client.modules.tasks;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

abstract class ProductSupportInterceptor extends AbstractFormInterceptor {

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.inList(editableWidget.getColumnId(), COL_TASK_TYPE, COL_REQUEST_TYPE)) {
      ((DataSelector) widget).addSelectorHandler(event -> setProductStyle());
    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    setProductStyle();
  }

  public boolean maybeNotifyEmptyProduct(Consumer<String> notifier) {
    if (isProductRequired(getActiveRow())
        && Data.isNull(getViewName(), getActiveRow(), COL_PRODUCT)) {

      notifier.accept(BeeUtils.joinWords(Localized.dictionary().crmTaskProduct(),
          Localized.dictionary().valueRequired()));

      return true;
    }
    return false;
  }

  private boolean isProductRequired(IsRow row) {
    return BeeUtils.unbox(row.getBoolean(getDataIndex(COL_PRODUCT_REQUIRED)));
  }

  private void setProductStyle() {
    Widget product = getFormView().getWidgetByName(COL_PRODUCT);

    if (product != null) {
      product.setStyleName(StyleUtils.NAME_REQUIRED, isProductRequired(getActiveRow()));
    }
  }
}
