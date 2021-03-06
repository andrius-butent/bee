package com.butent.bee.client.rights;

import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsObjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class WidgetRightsHandler extends MultiRoleForm {

  @Override
  public FormInterceptor getInstance() {
    return new WidgetRightsHandler();
  }

  @Override
  protected RightsObjectType getObjectType() {
    return RightsObjectType.WIDGET;
  }

  @Override
  protected void initObjects(Consumer<List<RightsObject>> consumer) {
    List<RightsObject> result = new ArrayList<>();

    for (RegulatedWidget widget : RegulatedWidget.values()) {
      ModuleAndSub ms = widget.getModuleAndSub();

      if (ms == null || ms.isEnabled()) {
        result.add(new RightsObject(widget.getName(), widget.getCaption(), ms));
      }
    }

    consumer.accept(result);
  }
}
