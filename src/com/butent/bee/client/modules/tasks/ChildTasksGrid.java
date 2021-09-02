package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.modules.projects.ProjectsHelper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

import static com.butent.bee.shared.modules.tasks.TaskConstants.COL_TASK_COMPANY;
import static com.butent.bee.shared.modules.tasks.TaskConstants.VIEW_TASKS;

class ChildTasksGrid extends TasksGrid {

  private static final List<String> COPY_COLUMNS = Lists.newArrayList(TaskConstants.COL_SUMMARY,
      TaskConstants.COL_DESCRIPTION,
      TaskConstants.COL_PRIORITY, TaskConstants.COL_TASK_TYPE, TaskConstants.ALS_TASK_TYPE_NAME,
      TaskConstants.COL_EXPECTED_DURATION, ClassifierConstants.COL_COMPANY,
      ClassifierConstants.ALS_COMPANY_NAME, ProjectConstants.ALS_COMPANY_TYPE_NAME,
      ClassifierConstants.COL_CONTACT, ClassifierConstants.ALS_CONTACT_FIRST_NAME,
      ClassifierConstants.ALS_CONTACT_LAST_NAME, ClassifierConstants.ALS_CONTACT_COMPANY_NAME,
      ClassifierConstants.ALS_CONTACT_COMPANY_TYPE_NAME, TaskConstants.ALS_REMINDER_NAME);

  ChildTasksGrid() {
    super(TaskType.RELATED, null);
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    if (copy) {
      return true;
    }

    presenter.getGridView().ensureRelId(new IdCallback() {
      @Override
      public void onSuccess(Long relId) {
        DataInfo childTaskDataInfo = Data.getDataInfo(presenter.getViewName());

        BeeRow childTaskRow = RowFactory.createEmptyRow(childTaskDataInfo, true);
        String relColumn = presenter.getGridView().getRelColumn();

        FormView parentForm = ViewHelper.getForm(presenter.getMainView());
        if (parentForm != null) {
          DataInfo parentFormDataInfo = Data.getDataInfo(parentForm.getViewName());
          IsRow parentFormRow = parentForm.getActiveRow();

          RelationUtils.updateRow(childTaskDataInfo, relColumn, childTaskRow,
              parentFormDataInfo, parentFormRow, true);

          if (BeeUtils.same(parentForm.getViewName(), ProjectConstants.VIEW_PROJECTS)) {
            fillProjectData(childTaskDataInfo, childTaskRow, parentFormDataInfo, parentFormRow);
          }

          if (BeeUtils.same(parentForm.getViewName(), ProjectConstants.VIEW_PROJECT_STAGES)) {
            fillProjectStageData(childTaskDataInfo, childTaskRow, parentForm);
            fillProjectData(childTaskDataInfo, childTaskRow, parentFormDataInfo, parentFormRow);
          }
        }

        RowFactory.createRow(childTaskDataInfo, childTaskRow, Modality.ENABLED, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            presenter.handleAction(Action.REFRESH);
          }
        });
      }
    });

    return false;
  }

  @Override
  public void beforeRender(final GridView gridView, RenderingEvent event) {

    for (IsRow row : getGridView().getRowData()) {
      if (!BeeUtils.isEmpty(row.getProperty(ProjectConstants.PROP_TEMPLATE))) {
        getGridView().getRowData().remove(row);
      }
    }

    FormView form = ViewHelper.getForm(gridView.asWidget());

    if (form == null) {
      return;
    }

    IsRow formRow = form.getActiveRow();

    if (formRow == null) {
      return;
    }

    setEnabled(form, formRow);

    String prop = formRow.getProperty(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY);

    if (BeeUtils.isEmpty(prop)) {
      return;
    }

    BeeRowSet templates = BeeRowSet.restore(prop);
    DataInfo viewTasks = Data.getDataInfo(TaskConstants.VIEW_TASKS);

    for (IsRow templRow : templates) {

      BeeRow row = RowFactory.createEmptyRow(viewTasks, true);

      for (String col : COPY_COLUMNS) {
        row.setValue(viewTasks.getColumnIndex(col),
            templRow.getValue(templates.getColumnIndex(col)));
      }

      row.setValue(viewTasks.getColumnIndex(TaskConstants.COL_STATUS),
          TaskConstants.TaskStatus.NOT_VISITED.ordinal());

      row.setProperty(ProjectConstants.PROP_TEMPLATE, BeeUtils.toString(templRow.getId()));

      gridView.getGrid().getRowData().add(0, row);
    }
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (!BeeUtils.isEmpty(event.getRowValue().getProperty(ProjectConstants.PROP_TEMPLATE))) {
      event.consume();

      IsRow templRow = event.getRowValue();
      final Long templateId =
          BeeUtils.toLong(templRow.getProperty(ProjectConstants.PROP_TEMPLATE));

      if (!DataUtils.isId(templateId)) {
        return;
      }

      final DataInfo viewTasks = Data.getDataInfo(TaskConstants.VIEW_TASKS);
      final BeeRow row = RowFactory.createEmptyRow(viewTasks, true);

      for (String col : COPY_COLUMNS) {
        row.setValue(viewTasks.getColumnIndex(col),
            templRow.getValue(viewTasks.getColumnIndex(col)));
      }

      if (getGridView() != null) {

        getGridView().ensureRelId(new IdCallback() {
          @Override
          public void onSuccess(Long result) {
            FormView parentForm = ViewHelper.getForm(getGridView().asWidget());
            if (parentForm != null) {

              if (parentForm.getActiveRow() != null) {

                RelationUtils.updateRow(viewTasks, getGridView().getRelColumn(), row,
                    Data.getDataInfo(parentForm.getViewName()), parentForm.getActiveRow(), true);

                switch (parentForm.getViewName()) {
                  case ProjectConstants.VIEW_PROJECT_STAGES:
                    fillProjectStageData(viewTasks, row, parentForm);
                    fillProjectData(viewTasks, row, Data.getDataInfo(parentForm.getViewName()),
                        parentForm.getActiveRow());
                    break;
                  case ProjectConstants.VIEW_PROJECTS:
                    fillProjectData(viewTasks, row, Data.getDataInfo(parentForm.getViewName()),
                        parentForm.getActiveRow());
                    break;
                  default:
                    break;
                }
              }
            }

            RowFactory.createRow(viewTasks, row, Modality.ENABLED, new RowCallback() {
              @Override
              public void onSuccess(BeeRow createdTask) {
                Queries.deleteRow(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY, templateId,
                    new Queries.IntCallback() {
                      @Override
                      public void onSuccess(Integer templateTask) {
                        getGridPresenter().handleAction(Action.REFRESH);
                      }
                    });
              }
            });
          }
        });
      }

      return;
    }

    super.onEditStart(event);
  }

  @Override
  public GridInterceptor getInstance() {
    return new ChildTasksGrid();
  }

  private static void fillProjectData(DataInfo taskData, IsRow taskRow, DataInfo parentFormData,
      IsRow parentRowData) {
    if (taskData == null && taskRow == null && parentFormData == null && parentRowData == null) {
      return;
    }

    /* Fill company info */
    int idxTaskCompany = taskData.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxProjectCompany = parentFormData.getColumnIndex(ClassifierConstants.COL_COMPANY);

    if (BeeUtils.isNegative(idxTaskCompany) && BeeUtils.isNegative(idxProjectCompany)) {
      return;
    }

    taskRow.setValue(idxTaskCompany, parentRowData.getValue(idxProjectCompany));

    int idxTaskCompanyName = taskData.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxProjectCompanyName = parentFormData.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);

    if (!BeeUtils.isNegative(idxTaskCompanyName) && !BeeUtils.isNegative(idxProjectCompanyName)) {
      taskRow.setValue(idxTaskCompanyName, parentRowData.getValue(idxProjectCompanyName));
    }
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter,
      IsRow activeRow, Collection<RowInfo> selectedRows, DeleteMode defMode) {

    if (!BeeUtils.isEmpty(activeRow.getProperty(ProjectConstants.PROP_TEMPLATE))) {
      return DeleteMode.SINGLE;
    }

    return super.getDeleteMode(presenter, activeRow, selectedRows, defMode);
  }

  @Override
  public DeleteMode beforeDeleteRow(final GridPresenter presenter, IsRow row) {
    if (!BeeUtils.isEmpty(row.getProperty(ProjectConstants.PROP_TEMPLATE))) {

      final Long templateId = BeeUtils.toLong(row.getProperty(ProjectConstants.PROP_TEMPLATE));

      if (!DataUtils.isId(templateId)) {
        return DeleteMode.CANCEL;
      }

      Queries.deleteRow(ProjectConstants.VIEW_PROJECT_TEMPLATE_TASK_COPY, templateId,
          new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getViewName());
              presenter.handleAction(Action.REFRESH);
            }
          });
      return DeleteMode.CANCEL;
    }

    return super.beforeDeleteRow(presenter, row);
  }

  private static void fillProjectStageData(DataInfo taskData, IsRow taskRow, FormView parentForm) {

    IsRow prjRow = ViewHelper.getParentRow(parentForm.asWidget(), ProjectConstants.VIEW_PROJECTS);

    if (prjRow != null) {
      RelationUtils.updateRow(taskData, ProjectConstants.COL_PROJECT, taskRow,
              Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), prjRow, true);

      Data.setValue(VIEW_TASKS, taskRow, COL_TASK_COMPANY, Data.getLong(
              ProjectConstants.VIEW_PROJECTS, prjRow, ProjectConstants.COL_COMAPNY));
      RelationUtils.updateRow(taskData, COL_TASK_COMPANY, taskRow,
              Data.getDataInfo(ProjectConstants.VIEW_PROJECTS), prjRow, false);
    }
  }

  private void setEnabled(FormView form, IsRow formRow) {
    if (form == null && formRow == null) {
      return;
    }

    if (!BeeUtils.inListSame(form.getViewName(), ProjectConstants.VIEW_PROJECTS,
        ProjectConstants.VIEW_PROJECT_STAGES)) {
      return;
    }

    if (DataUtils.isNewRow(formRow)) {
      getGridPresenter().getMainView().setEnabled(true);
      return;
    }

    getGridPresenter().getMainView().setEnabled(ProjectsHelper.isProjectOwner(form, formRow)
        || ProjectsHelper.isProjectUser(form, formRow));

  }

}
