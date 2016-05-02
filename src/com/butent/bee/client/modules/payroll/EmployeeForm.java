package com.butent.bee.client.modules.payroll;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.layout.SummaryProxy;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.payroll.PayrollConstants.WorkScheduleKind;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;

class EmployeeForm extends AbstractFormInterceptor {

  private static void refreshSchedule(FormView form, IsRow row, String widgetName,
      WorkScheduleKind kind) {

    Widget container = form.getWidgetByName(widgetName, false);
    if (container instanceof SummaryProxy) {
      SummaryProxy panel = (SummaryProxy) container;

      if (DataUtils.hasId(row)) {
        EmployeeSchedule widget = new EmployeeSchedule(row.getId(), kind);
        panel.setWidget(widget);

        widget.refresh();

      } else if (!panel.isEmpty()) {
        panel.clear();
      }
    }
  }

  EmployeeForm() {
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.PAYROLL))) {
      refreshSchedule(form, row, "WorkSchedule", WorkScheduleKind.PLANNED);
      refreshSchedule(form, row, "TimeSheet", WorkScheduleKind.ACTUAL);
    }

    super.afterRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new EmployeeForm();
  }
}
