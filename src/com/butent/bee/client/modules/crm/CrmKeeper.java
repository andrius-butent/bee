package com.butent.bee.client.modules.crm;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.crm.CrmConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.MenuManager;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.crm.CrmConstants.TaskEvent;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public final class CrmKeeper {

  private static final String COMPANY_TIMES_REPORT = "companytimes";
  private static final String TYPE_HOURS_REPORT = "typehours";
  private static final String USERS_HOURS_REPORT = "usershours";

  private static class RowTransformHandler implements RowTransformEvent.Handler {

    private final List<String> taskColumns = Lists.newArrayList(COL_SUMMARY, COL_COMPANY_NAME,
        COL_EXECUTOR_FIRST_NAME, COL_EXECUTOR_LAST_NAME, COL_FINISH_TIME, COL_STATUS);

    private DataInfo taskViewInfo;

    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_TASKS)) {
        event.setResult(DataUtils.join(getTaskViewInfo(), event.getRow(), taskColumns,
            BeeConst.STRING_SPACE));
      }
    }

    private DataInfo getTaskViewInfo() {
      if (this.taskViewInfo == null) {
        this.taskViewInfo = Data.getDataInfo(VIEW_TASKS);
      }
      return this.taskViewInfo;
    }
  }

  public static void register() {
    FormFactory.registerFormInterceptor(FORM_NEW_TASK, new TaskBuilder());
    FormFactory.registerFormInterceptor(FORM_TASK, new TaskEditor());

    FormFactory.registerFormInterceptor(FORM_NEW_REQUEST, new RequestBuilder(null));
    FormFactory.registerFormInterceptor(FORM_REQUEST, new RequestEditor());

    GridFactory.registerGridInterceptor(GRID_REQUESTS, new RequestsGridInterceptor());

    BeeKeeper.getMenu().registerMenuCallback("task_list", new MenuManager.MenuCallback() {
      @Override
      public void onSelection(String parameters) {
        TaskList.open(parameters);
      }
    });

    BeeKeeper.getMenu().registerMenuCallback("task_reports", new MenuManager.MenuCallback() {
      @Override
      public void onSelection(String parameters) {
        if (BeeUtils.startsSame(parameters, COMPANY_TIMES_REPORT)) {
          FormFactory.openForm(FORM_TASKS_REPORT, new TasksReportsInterceptor(
              TasksReportsInterceptor.ReportType.COMPANY_TIMES));
        } else if (BeeUtils.startsSame(parameters, TYPE_HOURS_REPORT)) {
          FormFactory.openForm(FORM_TASKS_REPORT, new TasksReportsInterceptor(
              TasksReportsInterceptor.ReportType.TYPE_HOURS));
        } else if (BeeUtils.startsSame(parameters, USERS_HOURS_REPORT)) {
          FormFactory.openForm(FORM_TASKS_REPORT, new TasksReportsInterceptor(
              TasksReportsInterceptor.ReportType.USERS_HOURS));
        } else {
          Global.showError("Service type '" + parameters + "' not found");
        }
      }
    });

    SelectorEvent.register(new TaskSelectorHandler());

    DocumentHandler.register();

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler(), false);
  }

  static ParameterList createArgs(String method) {
    ParameterList args = BeeKeeper.getRpc().createParameters(CRM_MODULE);
    args.addQueryItem(CRM_METHOD, method);
    return args;
  }

  static ParameterList createTaskRequestParameters(TaskEvent event) {
    return createArgs(CRM_TASK_PREFIX + event.name());
  }

  private CrmKeeper() {
  }
}
