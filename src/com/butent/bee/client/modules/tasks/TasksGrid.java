package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.client.composite.Relations.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.modules.projects.ProjectsKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridFormKind;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

class TasksGrid extends AbstractGridInterceptor implements RowUpdateEvent.Handler {

  private static final String NAME_MODE = "Mode";
  private static final String NAME_SLACK = "Slack";
  private static final String NAME_STAR = "Star";

  private static final int DEFAULT_STAR_COUNT = 3;

  static BiConsumer<GridOptions, PresenterCallback> getFeedFilterHandler(Feed feed) {
    final TaskType type = TaskType.getByFeed(feed);
    Assert.notNull(type);

    return (gridOptions, callback) -> {
      String cap = BeeUtils.notEmpty(gridOptions.getCaption(), type.getCaption());
      GridFactory.openGrid(GRID_TASKS, new TasksGrid(type, cap), gridOptions, callback);
    };
  }

  private final TaskType type;
  private final String caption;

  private final Long userId;
  private final Collection<HandlerRegistration> eventRegistry = new ArrayList<>();

  protected TasksGrid(TaskType type, String caption) {
    this.type = type;
    this.caption = caption;

    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public boolean afterCreateColumn(String columnId, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnId, NAME_MODE) && column instanceof HasCellRenderer) {
      ((HasCellRenderer) column).setRenderer(new ModeRenderer());

    } else if (BeeUtils.same(columnId, NAME_SLACK) && column instanceof HasCellRenderer) {
      ((HasCellRenderer) column).setRenderer(new TaskSlackRenderer(dataColumns));

    } else if (BeeUtils.inListSame(columnId, COL_FINISH_TIME, COL_EXECUTOR)) {
      editableColumn.addCellValidationHandler(ValidationHelper.DO_NOT_VALIDATE);
    }

    return true;
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();

    if (type.equals(TaskType.ALL) || type.equals(TaskType.DELEGATED)) {
      FaLabel confirmTask = new FaLabel(FontAwesome.CHECK_SQUARE_O);
      confirmTask.setTitle(Localized.dictionary().crmTaskConfirm());
      confirmTask.addClickHandler(arg0 -> confirmTasksClick());

      presenter.getHeader().addCommandItem(confirmTask);
    }

    if (BeeKeeper.getUser().canCreateData(ProjectConstants.VIEW_PROJECTS)
        && !presenter.getGridView().isChild()) {
      FaLabel createProject = new FaLabel(FontAwesome.ROCKET);
      createProject.setTitle(Localized.dictionary().prjCreateFromTasks());
      createProject.addClickHandler(arg0 -> createProjectClick());

      presenter.getHeader().addCommandItem(createProject);
    }

    presenter.getHeader().addCommandItem(CustomTasksGrid.getOrderAction());
  }

  @Override
  public boolean beforeAction(Action action, final GridPresenter presenter) {
    if (action == Action.COPY) {
      if (presenter.getMainView().isEnabled() && presenter.getActiveRow() != null) {
        String title = presenter.getActiveRow().getString(getDataIndex(COL_SUMMARY));
        List<String> msg = Lists.newArrayList(Localized.dictionary().crmTaskCopyQuestion());

        List<String> options = Lists.newArrayList(Localized.dictionary().crmNewTask(),
            Localized.dictionary().crmNewRecurringTask(), Localized.dictionary().cancel());
        int defValue = options.size() - 1;

        Global.messageBox(title, Icon.QUESTION, msg, options, defValue, value -> {
          if (value < 0 || value > 1 || presenter.getActiveRow() == null) {
            return;
          }

          ParameterList params = TasksKeeper.createArgs(SVC_GET_TASK_DATA);
          params.addQueryItem(VAR_TASK_ID, getTaskId(presenter.getActiveRow()));
          params.addQueryItem(VAR_TASK_PROPERTIES, PROP_OBSERVERS);
          params.addQueryItem(VAR_COPY_RELATIONS, BeeConst.STRING_ASTERISK);

          BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              if (Queries.checkRowResponse(SVC_GET_TASK_DATA, VIEW_TASKS, response)) {
                BeeRow original = BeeRow.restore(response.getResponseAsString());

                switch (value) {
                  case 0:
                    copyTask(original);
                    break;
                  case 1:
                    copyAsRecurringTask(original);
                    break;
                }
              }
            }
          });
        });
      }

      return false;

    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {

    if (type == TaskType.ASSIGNED && columnDescription.is(COL_EXECUTOR)
        || type == TaskType.DELEGATED && columnDescription.is(COL_OWNER)) {

      if (columnDescription.getVisible() == null
          && !GridSettings.hasVisibleColumns(gridView.getGridKey())) {
        ColumnDescription copy = columnDescription.copy();
        copy.setVisible(false);

        return copy;
      }
    }

    return columnDescription;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {
    Provider provider = presenter.getDataProvider();
    Long owner = activeRow.getLong(provider.getColumnIndex(COL_OWNER));

    if (Objects.equals(owner, userId)) {
      return GridInterceptor.DeleteMode.SINGLE;
    } else {
      presenter.getGridView().notifyWarning(
          BeeUtils.joinWords(Localized.dictionary().crmTask(), getTaskId(activeRow)),
          Localized.dictionary().crmTaskDeleteCanManager());
      return GridInterceptor.DeleteMode.CANCEL;
    }
  }

  @Override
  public List<String> getDeleteRowMessage(IsRow row) {
    String m1 = BeeUtils.joinWords(Localized.dictionary().crmTask(), getTaskId(row));
    String m2 = Localized.dictionary().crmTaskDeleteQuestion();

    return Lists.newArrayList(m1, m2);
  }

  @Override
  public AbstractFilterSupplier getFilterSupplier(String columnName,
      ColumnDescription columnDescription) {
    if (BeeUtils.same(columnName, NAME_SLACK)) {
      return new SlackFilterSupplier(columnDescription.getFilterOptions());

    } else if (BeeUtils.same(columnName, NAME_STAR)) {
      return new StarFilterSupplier(columnDescription.getFilterOptions());

    } else if (BeeUtils.same(columnName, NAME_MODE)) {
      return new ModeFilterSupplier(columnDescription.getFilterOptions());

    } else {
      return super.getFilterSupplier(columnName, columnDescription);
    }
  }

  @Override
  public ColumnHeader getHeader(String columnName, String headerCaption) {
    if (PROP_STAR.equals(columnName)) {
      return new ColumnHeader(columnName, Stars.getDefaultHeader(), BeeConst.STRING_ASTERISK);
    } else {
      return super.getHeader(columnName, headerCaption);
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new TasksGrid(type, caption);
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {

    Filter f1 = type.getFilter(new LongValue(userId));
    Filter f2 = Filter.or(Filter.and(f1, Filter.isNull(COL_PRIVATE_TASK)), Filter.and(f1, Filter
        .notNull(COL_PRIVATE_TASK), Filter.or(Filter.equals(COL_OWNER, userId), Filter
        .equals(COL_EXECUTOR, userId), Filter.in("TaskID", VIEW_TASK_USERS, COL_TASK, Filter
        .equals(AdministrationConstants.COL_USER, userId)))));

    gridDescription.setFilter(Filter.or(f1, f2));

    return true;
  }

  @Override
  public void onEditStart(final EditStartEvent event) {
    if (!maybeEditStar(event)) {
      IsRow row = event.getRowValue();

      TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class, row.getInteger(getDataIndex(
          COL_STATUS)));

      if (Objects.equals(TaskStatus.NOT_SCHEDULED, status) && BeeKeeper.getUser().canCreateData(
          getViewName())) {
        event.consume();

        RowEditor.openForm(FORM_NEW_TASK, Data.getDataInfo(getViewName()),
            Filter.compareId(row.getId()), Opener.MODAL);
      } else if (Objects.equals(TaskStatus.NOT_SCHEDULED, status)) {
        event.consume();
        getGridView().notifySevere(Localized.dictionary().actionCanNotBeExecuted(), BeeUtils
            .bracket(Localized.dictionary().createNewRow()));
      } else if (DataUtils.isId(row.getLong(Data.getColumnIndex(VIEW_TASKS, COL_TASK_ORDER)))) {
        event.consume();
        RowEditor.openForm(FORM_TASK_ORDER, Data.getDataInfo(VIEW_TASK_ORDERS),
            Filter.compareId(row.getId()), Opener.NEW_TAB);
      } else if (BeeKeeper.getUser().openInNewTab()) {
        event.consume();
        FormView form = getGridView().getForm(GridFormKind.EDIT);
        String editForm =
          form == null ? Data.getDataInfo(getViewName()).getEditForm() : form.getFormName();

        RowEditor.openForm(editForm, Data.getDataInfo(getViewName()), row, Opener.NEW_TAB, null,
          null);
      }
    }
  }

  @Override
  public void onLoad(GridView gridView) {
    super.onLoad(gridView);

    if (!BeeUtils.isEmpty(eventRegistry)) {
      EventUtils.clearRegistry(eventRegistry);
    }

    eventRegistry.add(BeeKeeper.getBus().registerRowUpdateHandler(this, false));
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    if (TaskType.NOT_SCHEDULED.equals(type)) {
      newRow.setValue(gridView.getDataIndex(COL_STATUS), TaskStatus.NOT_SCHEDULED.ordinal());
    }
    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (!event.hasView(TaskConstants.VIEW_TASKS)
        && !event.hasView(TaskConstants.VIEW_RELATED_TASKS)) {
      return;
    }
    IsRow row = event.getRow();
    IsRow oldRow = null;

    if (getGridView() != null && getGridView().getGrid() != null) {
      oldRow = getGridView().getGrid().getRowById(row.getId());
    }

    boolean isNew = false;
    boolean changed = false;

    /* Fast switch list type and show new records */
    switch (type) {
      case ALL:
      case RELATED:
        Long id = row.getId();
        Long oldId = oldRow != null ? oldRow.getId() : null;

        isNew = !Objects.equals(id, oldId);
        changed = !Objects.equals(id, oldId) && oldRow != null;
        break;
      case ASSIGNED:
        Long executor = Data.getLong(VIEW_TASKS, row, COL_EXECUTOR);
        Long oldExecutor = oldRow != null ? Data.getLong(VIEW_TASKS, oldRow, COL_EXECUTOR) : null;

        isNew = !Objects.equals(executor, oldExecutor) && Objects.equals(executor, userId);
        changed = !Objects.equals(oldExecutor, userId) && oldRow != null;

        break;
      case DELEGATED:
      case NOT_SCHEDULED:
        Long owner = Data.getLong(VIEW_TASKS, row, COL_OWNER);
        Long oldOwner = oldRow != null ? Data.getLong(VIEW_TASKS, oldRow, COL_OWNER) : null;

        isNew = !Objects.equals(owner, oldOwner) && Objects.equals(owner, userId);
        changed = !Objects.equals(oldOwner, userId) && oldRow != null;

        break;
      case OBSERVED:
        List<Long> observers = DataUtils.parseIdList(row.getProperty(PROP_OBSERVERS));
        List<Long> oldObservers = oldRow != null
            ? DataUtils.parseIdList(oldRow.getProperty(PROP_OBSERVERS)) : null;

        isNew = !Objects.equals(observers, oldObservers) && observers != null
            && observers.contains(userId);
        changed = oldObservers != null && !oldObservers.contains(userId) && oldRow != null;
        break;
    }

    if (getGridPresenter() != null) {
    /* check if cell not contains in current user filter */
      if (!(changed || isNew) && DataUtils.idEquals(oldRow, row.getId())
          && getGridPresenter().getDataProvider() != null
          && getGridPresenter().getDataProvider().getFilter() != null) {

        Filter filter = getGridPresenter().getDataProvider().getFilter();
        changed = !filter.isMatch(Data.getColumns(VIEW_TASKS), row);
      }

      if (isNew || changed) {
        getGridPresenter().handleAction(Action.REFRESH);
      }
    }
  }

  @Override
  public void onUnload(GridView gridView) {
    super.onUnload(gridView);
    EventUtils.clearRegistry(eventRegistry);
  }

  protected void afterCopyAsRecurringTask() {
  }

  protected void afterCopyTask() {
  }

  protected Long getTaskId(IsRow row) {
    return (row == null) ? null : row.getId();
  }

  protected boolean maybeEditStar(final EditStartEvent event) {
    if (event != null && PROP_STAR.equals(event.getColumnId()) && event.getRowValue() != null
        && event.getRowValue().hasPropertyValue(PROP_USER, userId)) {

      final CellSource source = CellSource.forProperty(PROP_STAR, userId, ValueType.INTEGER);

      EditorAssistant.editStarCell(DEFAULT_STAR_COUNT, event, source,
          parameter -> updateStar(event, source, parameter));

      return true;
    } else {
      return false;
    }
  }

  private void confirmTasksClick() {
    final GridView gridView = getGridPresenter().getGridView();
    CompoundFilter filter = CompoundFilter.or();

    for (RowInfo row : gridView.getSelectedRows(SelectedRows.ALL)) {
      filter.add(Filter.compareId(row.getId()));
    }

    if (filter.isEmpty()) {
      IsRow selectedRow = gridView.getActiveRow();
      if (selectedRow == null) {
        gridView.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      } else {
        confirmTask(gridView, selectedRow);
      }

    } else {
      confirmTasks(gridView, filter);
    }
  }

  private void createProjectClick() {
    if (userId == null) {
      Assert.untouchable();
    }

    final GridView gridView = getGridPresenter().getGridView();
    int idxTaskProject = gridView.getDataIndex(ProjectConstants.COL_PROJECT);

    int idxTaskOwner = gridView.getDataIndex(COL_OWNER);

    final IsRow selectedRow = gridView.getActiveRow();

    if (selectedRow == null) {
      gridView.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }

    if (!BeeUtils.isEmpty(selectedRow.getString(idxTaskProject))) {
      gridView.notifyWarning(Localized.dictionary().taskAssignedToProject(selectedRow.getId(),
          selectedRow.getLong(idxTaskProject)));
      return;
    }

    if (!userId.equals(selectedRow.getLong(idxTaskOwner))) {
      gridView
          .notifyWarning(Localized.dictionary().projectCanCreateTaskOwner(selectedRow.getId()));
      return;
    }

    FormFactory.createFormView(ProjectConstants.FORM_NEW_PROJECT_FROM_TASK, null, null, false,
        getNewProjectFormInterceptor(selectedRow),
        (formDescription, form) -> {
          if (form != null) {
            form.start(null);
            Global.showModalWidget(form.getCaption(), form.asWidget());
          }
        });
  }

  private void createSimpleProject(final IsRow selectedRow) {

    DataInfo prjDataInfo = Data.getDataInfo(ProjectConstants.VIEW_PROJECTS);
    DataInfo taskDataInfo = Data.getDataInfo(VIEW_TASKS);

    int idxPrjCompany = prjDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxPrjCompanyName = prjDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxPrjCompanyTypeName =
        prjDataInfo.getColumnIndex(ProjectConstants.ALS_COMPANY_TYPE_NAME);
    int idxPrjContact = prjDataInfo.getColumnIndex(ClassifierConstants.COL_CONTACT);
    int idxPrjContactPerson = prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_PERSON);
    int idxPrjContactFirstName =
        prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_FIRST_NAME);
    int idxPrjContactLastName =
        prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_LAST_NAME);
    int idxPrjContactCompanyName =
        prjDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_COMPANY_NAME);

    int idxPrjOwner = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_OWNER);
    int idxPrjOwnerFirstName = prjDataInfo.getColumnIndex(ProjectConstants.ALS_OWNER_FIRST_NAME);
    int idxPrjOwnerLastName = prjDataInfo.getColumnIndex(ProjectConstants.ALS_OWNER_LAST_NAME);
    int idxPrjDescrition = prjDataInfo.getColumnIndex(ProjectConstants.COL_DESCRIPTION);
    int idxPrjStartDate = prjDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_START_DATE);

    int idxTaskCompany = taskDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxTaskCompanyName = taskDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxTaskCompanyTypeName = taskDataInfo.getColumnIndex(
        ProjectConstants.ALS_COMPANY_TYPE_NAME);
    int idxTaskContact = taskDataInfo.getColumnIndex(ClassifierConstants.COL_CONTACT);
    int idxTaskContactPerson = taskDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_PERSON);
    int idxTaskContactFirstName = taskDataInfo.getColumnIndex(
        ClassifierConstants.ALS_CONTACT_FIRST_NAME);
    int idxTaskContactLastName = taskDataInfo.getColumnIndex(
        ClassifierConstants.ALS_CONTACT_LAST_NAME);
    int idxTaskContactCompanyName =
        taskDataInfo.getColumnIndex(ClassifierConstants.ALS_CONTACT_COMPANY_NAME);
    int idxTaskOwnerFirstName = taskDataInfo.getColumnIndex(ALS_OWNER_FIRST_NAME);
    int idxTaskOwnerLastName = taskDataInfo.getColumnIndex(ALS_OWNER_LAST_NAME);
    int idxTaskDescription = taskDataInfo.getColumnIndex(COL_DESCRIPTION);
    int idxTaskOwner = taskDataInfo.getColumnIndex(COL_OWNER);

    BeeRow prjRow = RowFactory.createEmptyRow(prjDataInfo, true);
    prjRow.setValue(idxPrjCompany, selectedRow.getValue(idxTaskCompany));
    prjRow.setValue(idxPrjCompanyName, selectedRow.getValue(idxTaskCompanyName));
    prjRow.setValue(idxPrjCompanyTypeName, selectedRow.getValue(idxTaskCompanyTypeName));
    prjRow.setValue(idxPrjContact, selectedRow.getValue(idxTaskContact));
    prjRow.setValue(idxPrjContactPerson, selectedRow.getValue(idxTaskContactPerson));
    prjRow.setValue(idxPrjContactFirstName, selectedRow.getValue(idxTaskContactFirstName));
    prjRow.setValue(idxPrjContactLastName, selectedRow.getValue(idxTaskContactLastName));
    prjRow.setValue(idxPrjContactCompanyName, selectedRow.getValue(idxTaskContactCompanyName));
    prjRow.setValue(idxPrjOwner, selectedRow.getValue(idxTaskOwner));
    prjRow.setValue(idxPrjOwnerFirstName, selectedRow.getValue(idxTaskOwnerFirstName));
    prjRow.setValue(idxPrjOwnerLastName, selectedRow.getValue(idxTaskOwnerLastName));
    prjRow.setValue(idxPrjDescrition, selectedRow.getValue(idxTaskDescription));
    prjRow.setValue(idxPrjStartDate, new JustDate());

    RowFactory.createRow(prjDataInfo, prjRow, Modality.ENABLED, new RowCallback() {

      @Override
      public void onSuccess(final BeeRow projectRow) {
        final List<Long> observers = Lists.newArrayList();
        // Temporary disabled create project users
        // DataUtils.parseIdList(selectedRow.getProperty(TaskConstants.PROP_OBSERVERS));

        if (!BeeUtils.isEmpty(observers)) {
          addProjectUsers(observers, projectRow);
        }

        Queries.update(VIEW_TASKS, selectedRow.getId(), ProjectConstants.COL_PROJECT, Value
            .getValue(projectRow.getId()), new IntCallback() {

          @Override
          public void onSuccess(Integer result) {
            if (getGridView() != null) {
              getGridView().notifyInfo(
                  Localized.dictionary().newProjectCreated(projectRow.getId()));
            }
          }
        });
      }
    });
  }

  public void createProjectFromTemplate(final IsRow selectedRow, final IsRow templateRow) {
    List<String> copyCols = Lists.newArrayList(ClassifierConstants.COL_COMPANY,
        ClassifierConstants.ALS_COMPANY_NAME, ProjectConstants.ALS_COMPANY_TYPE_NAME);

    for (String col : copyCols) {

      Data.setValue(ProjectConstants.VIEW_PROJECT_TEMPLATES, templateRow, col,
          selectedRow.getString(Data.getColumnIndex(VIEW_TASKS, col)));
    }

    ProjectsKeeper.createProjectFromTemplate(templateRow, new RowCallback() {
      @Override
      public void onSuccess(final BeeRow projectRow) {
        Queries.update(VIEW_TASKS, selectedRow.getId(), ProjectConstants.COL_PROJECT, Value
            .getValue(projectRow.getId()), new IntCallback() {

          @Override
          public void onSuccess(Integer result) {
            if (getGridView() != null) {
              getGridView().notifyInfo(Localized.dictionary()
                  .newProjectCreated(projectRow.getId()));
            }
          }
        });
      }
    });
  }

  private void confirmTask(final GridView gridView, final IsRow row) {
    Assert.notNull(row);

    final List<BeeRow> taskRow = Lists.newArrayList(DataUtils.cloneRow(row));
    final DataInfo info = Data.getDataInfo(gridView.getViewName());
    final ResponseObject messages = ResponseObject.emptyResponse();
    long user = BeeUtils.unbox(userId);

    if (!TaskUtils.canConfirmTasks(info, taskRow, user, messages)) {
      if (messages.hasWarnings()) {
        gridView.notifyWarning(messages.getWarnings());
      }
      return;
    }

    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskConfirmation());

    final String did =
        dialog.addDateTime(Localized.dictionary().crmTaskConfirmDate(), true, TimeUtils
            .nowMinutes());
    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.dictionary().crmTaskConfirm(), () -> {
      DateTime approved = dialog.getDateTime(did);
      if (approved == null) {
        gridView.notifySevere(Localized.dictionary().crmEnterConfirmDate());
        return;
      }

      List<String> notes = new ArrayList<>();

      BeeRow newRow = DataUtils.cloneRow(row);
      newRow.setValue(info.getColumnIndex(COL_STATUS), TaskStatus.APPROVED.ordinal());
      newRow.setValue(info.getColumnIndex(COL_APPROVED), approved);

      notes.add(TaskUtils.getUpdateNote(Localized.getLabel(info.getColumn(COL_STATUS)),
          DataUtils.render(info, row, info.getColumn(COL_STATUS),
              info.getColumnIndex(COL_STATUS),
              Format.getDateRenderer(), Format.getDateTimeRenderer()),
          DataUtils.render(info, newRow, info.getColumn(COL_STATUS),
              info.getColumnIndex(COL_STATUS),
              Format.getDateRenderer(), Format.getDateTimeRenderer())));

      notes.add(TaskUtils.getInsertNote(Localized.getLabel(info.getColumn(COL_APPROVED)),
          DataUtils.render(info, newRow, info.getColumn(COL_APPROVED),
              info.getColumnIndex(COL_APPROVED),
              Format.getDateRenderer(), Format.getDateTimeRenderer())));

      ParameterList params = TasksKeeper.createArgs(SVC_CONFIRM_TASKS);
      params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(Lists.newArrayList(row.getId())));
      params.addDataItem(VAR_TASK_APPROVED_TIME, approved.serialize());

      String comment = dialog.getComment(cid);
      if (!BeeUtils.isEmpty(comment)) {
        params.addDataItem(VAR_TASK_COMMENT, comment);
      }

      if (!notes.isEmpty()) {
        params.addDataItem(VAR_TASK_NOTES, Codec.beeSerialize(notes));
      }

      sendRequest(params, gridView);
      dialog.close();
    });

    dialog.display();
  }

  private void confirmTasks(final GridView gridView, final CompoundFilter filter) {
    Queries.getRowSet(gridView.getViewName(), null, filter, new RowSetCallback() {
      @Override
      public void onSuccess(final BeeRowSet result) {
        final DataInfo info = Data.getDataInfo(gridView.getViewName());
        final ResponseObject messages = ResponseObject.emptyResponse();
        long user = BeeUtils.unbox(userId);

        if (!TaskUtils.canConfirmTasks(info, result.getRows(), user, messages)) {
          if (messages.hasWarnings()) {
            gridView.notifyWarning(messages.getWarnings());
          }
          return;
        }

        Global.confirm(Localized.dictionary().crmTasksConfirmQuestion(),
            () -> {
              DateTime approved = new DateTime();

              ParameterList params = TasksKeeper.createArgs(SVC_CONFIRM_TASKS);
              params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(result.getRowIds()));
              params.addDataItem(VAR_TASK_APPROVED_TIME, approved.serialize());

              sendRequest(params, gridView);
            });
      }
    });
  }

  private void copyAsRecurringTask(BeeRow oldRow) {
    DataInfo sourceInfo = Data.getDataInfo(VIEW_TASKS);
    if (sourceInfo == null) {
      return;
    }
    DataInfo targetInfo = Data.getDataInfo(VIEW_RECURRING_TASKS);
    if (targetInfo == null) {
      return;
    }

    BeeRow newRow = RowFactory.createEmptyRow(targetInfo, true);

    Set<String> colNames = Sets.newHashSet(COL_PRIORITY, COL_SUMMARY, COL_DESCRIPTION);
    for (String colName : colNames) {
      String value = oldRow.getString(sourceInfo.getColumnIndex(colName));
      if (!BeeUtils.isEmpty(value)) {
        newRow.setValue(targetInfo.getColumnIndex(colName), value);
      }
    }

    DateTime startTime = oldRow.getDateTime(sourceInfo.getColumnIndex(COL_START_TIME));
    DateTime finishTime = oldRow.getDateTime(sourceInfo.getColumnIndex(COL_FINISH_TIME));

    if (startTime != null) {
      int minutes = TimeUtils.minutesSinceDayStarted(startTime);
      if (minutes > 0) {
        newRow.setValue(targetInfo.getColumnIndex(COL_RT_START_AT),
            TimeUtils.renderMinutes(minutes, true));
      }
    }

    String duration = oldRow.getString(sourceInfo.getColumnIndex(COL_EXPECTED_DURATION));
    if (!BeeUtils.isEmpty(duration)) {
      newRow.clearCell(targetInfo.getColumnIndex(COL_RT_DURATION_DAYS));
      newRow.setValue(targetInfo.getColumnIndex(COL_RT_DURATION_TIME), duration);

    } else if (startTime != null && finishTime != null) {
      long durationMillis = finishTime.getTime() - startTime.getTime();
      long days = durationMillis / TimeUtils.MILLIS_PER_DAY;
      long time = durationMillis % TimeUtils.MILLIS_PER_DAY;

      if (durationMillis > 0 && (days > 0 || time > TimeUtils.MILLIS_PER_MINUTE)) {
        if (days > 0) {
          newRow.setValue(targetInfo.getColumnIndex(COL_RT_DURATION_DAYS), days);
        } else {
          newRow.clearCell(targetInfo.getColumnIndex(COL_RT_DURATION_DAYS));
        }

        if (time > TimeUtils.MILLIS_PER_MINUTE) {
          newRow.setValue(targetInfo.getColumnIndex(COL_RT_DURATION_TIME),
              TimeUtils.renderMinutes(BeeUtils.toInt(time / TimeUtils.MILLIS_PER_MINUTE), true));
        }
      }
    }

    colNames = Sets.newHashSet(ClassifierConstants.COL_COMPANY, ClassifierConstants.COL_CONTACT,
        COL_REMINDER, COL_TASK_TYPE);
    for (String colName : colNames) {
      RelationUtils.copyWithDescendants(sourceInfo, colName, oldRow, targetInfo, colName, newRow);
    }

    Long owner = BeeKeeper.getUser().getUserId();
    Long executor = oldRow.getLong(sourceInfo.getColumnIndex(COL_EXECUTOR));

    if (executor != null) {
      newRow.setProperty(PROP_EXECUTORS, executor.toString());
    }

    String observers = oldRow.getProperty(PROP_OBSERVERS);
    if (!BeeUtils.isEmpty(observers)) {
      List<Long> users = DataUtils.parseIdList(observers);

      if (users.contains(owner)) {
        users.remove(owner);
      }
      if (users.contains(executor)) {
        users.remove(executor);
      }

      if (!users.isEmpty()) {
        newRow.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(users));
      }
    }
    for (String propViewName : TaskHelper.getRelatedViews()) {
      String propValue = oldRow.getProperty(PFX_RELATED + propViewName);
      if (!BeeUtils.isEmpty(propValue)) {
        newRow.setProperty(PFX_RELATED + propViewName, propValue);
      }
    }

    RowFactory.createRow(targetInfo, newRow, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        afterCopyAsRecurringTask();
      }
    });
  }

  private void copyTask(BeeRow oldRow) {
    DataInfo dataInfo = Data.getDataInfo(VIEW_TASKS);
    if (dataInfo == null) {
      return;
    }

    BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);

    Set<String> colNames = Sets.newHashSet(COL_PRIORITY, COL_SUMMARY, COL_DESCRIPTION,
        COL_EXPECTED_DURATION, COL_PRODUCT);
    for (String colName : colNames) {
      int index = dataInfo.getColumnIndex(colName);
      String value = oldRow.getString(index);

      if (!BeeUtils.isEmpty(value)) {
        newRow.setValue(index, value);
      }
    }

    colNames = Sets.newHashSet(ClassifierConstants.COL_COMPANY, ClassifierConstants.COL_CONTACT,
        COL_REMINDER, COL_TASK_TYPE);
    for (String colName : colNames) {
      RelationUtils.copyWithDescendants(dataInfo, colName, oldRow, dataInfo, colName, newRow);
    }

    Long owner = BeeKeeper.getUser().getUserId();
    Long executor = oldRow.getLong(dataInfo.getColumnIndex(COL_EXECUTOR));

    if (executor != null) {
      newRow.setProperty(PROP_EXECUTORS, executor.toString());
    }

    String observers = oldRow.getProperty(PROP_OBSERVERS);
    if (!BeeUtils.isEmpty(observers)) {
      List<Long> users = DataUtils.parseIdList(observers);

      if (users.contains(owner)) {
        users.remove(owner);
      }
      if (users.contains(executor)) {
        users.remove(executor);
      }

      if (!users.isEmpty()) {
        newRow.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(users));
      }
    }

    for (String propViewName : TaskHelper.getRelatedViews()) {
      String propValue = oldRow.getProperty(PFX_RELATED + propViewName);
      if (!BeeUtils.isEmpty(propValue)) {
        newRow.setProperty(PFX_RELATED + propViewName, propValue);
      }
    }

    if (DataUtils.isId(Data.getLong(VIEW_TASKS, oldRow, COL_TASK_ORDER))) {
      for (BeeColumn column : Data.getColumns(VIEW_TASK_ORDER_COLUMNS)) {
        int index = dataInfo.getColumnIndex(column.getId());
        String value = oldRow.getString(index);

        if (!BeeUtils.isEmpty(value) && !Objects.equals(column.getId(),
            TransportConstants.COL_ORDER_NO)) {
          newRow.setValue(index, value);
        }
      }

      newRow.setProperty(PROP_EXECUTORS, executor);

      for (String column : new String[]{COL_START_TIME, COL_FINISH_TIME}) {
        int index = Data.getColumnIndex(VIEW_TASKS, column);

        if (DataUtils.isId(Data.getLong(VIEW_TASKS, oldRow, COL_TASK_ORDER))
          && Objects.equals(column, COL_START_TIME)) {
          newRow.setValue(index, TimeUtils.nowMinutes());
        } else {
          newRow.setValue(index, oldRow.getDateTime(index));
        }
      }

      ParameterList params = TasksKeeper.createArgs(SVC_COPY_TASK_ORDER);

      List<BeeColumn> cols = Data.getColumns(VIEW_TASKS);

      BeeRowSet rowSet = DataUtils.createRowSetForInsert(VIEW_TASKS, cols,
        newRow, Sets.newHashSet(COL_EXECUTOR, COL_STATUS), true);
      rowSet.addRow(newRow);

      params.addDataItem(VAR_TASK_DATA, rowSet.serialize());

      BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (!response.hasErrors() && !response.isEmpty()) {
            Long newTaskID = response.getResponseAsLong();

            if (!DataUtils.isId(newTaskID)) {
              return;
            }

            Queries.getRowSet(VIEW_TASK_ORDER_ITEMS, null, Filter.equals(COL_TASK, oldRow.getId()), new RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                if (!DataUtils.isEmpty(result)) {

                  BeeRowSet insertRows = DataUtils.createRowSetForInsert(result);
                  insertRows.forEach(row -> Data.setValue(VIEW_TASK_ORDER_ITEMS, row, COL_TASK, newTaskID));

                  Queries.insertRows(insertRows, new RpcCallback<RowInfoList>() {
                    @Override
                    public void onSuccess(RowInfoList rowInfo) {
                      RowEditor.openForm(FORM_TASK_ORDER, Data.getDataInfo(VIEW_TASKS), Filter.compareId(newTaskID),
                        Opener.MODAL, null);
                      getGridPresenter().handleAction(Action.REFRESH);
                    }
                  });
                } else {
                  getGridPresenter().handleAction(Action.REFRESH);
                }
              }
            });
          }
        }
      });
    } else {
      RowFactory.createRow(dataInfo, newRow, Modality.ENABLED, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          afterCopyTask();
        }
      });
    }
  }

  private AbstractFormInterceptor getNewProjectFormInterceptor(final IsRow selectedRow) {
    return new AbstractFormInterceptor() {
      private static final String NAME_PROJECT_TEMPLATE = "ProjectTemplate";
      private static final String NAME_CREATE = "Create";
      private static final String NAME_CANCEL = "Cancel";

      private UnboundSelector templateSelector;
      private Button createButton;
      private Button cancelButton;

      @Override
      public void afterCreateWidget(String name, IdentifiableWidget widget,
          FormFactory.WidgetDescriptionCallback callback) {

        switch (name) {
          case NAME_PROJECT_TEMPLATE:
            templateSelector = widget instanceof UnboundSelector ? (UnboundSelector) widget : null;
            break;
          case NAME_CREATE:
            createButton = widget instanceof Button ? (Button) widget : null;

            if (createButton == null) {
              break;
            }

            createButton.addClickHandler(event -> {
              Popup.getActivePopup().close();

              if (templateSelector == null) {
                createSimpleProject(selectedRow);
              } else if (BeeUtils.isEmpty(templateSelector.getValue())) {
                createSimpleProject(selectedRow);
              } else {
                createProjectFromTemplate(selectedRow, templateSelector.getRelatedRow());
              }
            });
            break;
          case NAME_CANCEL:
            cancelButton = widget instanceof Button ? (Button) widget : null;

            if (cancelButton == null) {
              break;
            }

            cancelButton.addClickHandler(event -> {
              if (templateSelector != null) {
                templateSelector.setValue(null, true);
              }
              Popup.getActivePopup().close();
            });
            break;
          default:
            super.afterCreateWidget(name, widget, callback);
        }

      }

      @Override
      public FormInterceptor getInstance() {
        return this;
      }
    };
  }

  private void sendRequest(final ParameterList params, final GridView gridView) {
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasMessages()) {
          gridView.notifySevere(BeeUtils.joinItems(response.getMessages()));
          return;
        }

        gridView.notifyInfo(Localized.dictionary().crmTaskStatusApproved());
        getGridPresenter().refresh(true, false);
      }
    });
  }

  private void updateStar(final EditStartEvent event, final CellSource source,
      final Integer value) {

    final Long taskId = getTaskId(event.getRowValue());
    if (!DataUtils.isId(taskId)) {
      return;
    }

    Filter filter = Filter.and(Filter.equals(COL_TASK, taskId),
        Filter.equals(AdministrationConstants.COL_USER, userId));

    Queries.update(VIEW_TASK_USERS, filter, COL_STAR, new IntegerValue(value),
        new Queries.IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            CellUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), event.getRowValue().getId(),
                event.getRowValue().getVersion(), source,
                (value == null) ? null : BeeUtils.toString(value));
          }
        });
  }

  private static void addProjectUsers(final List<Long> userIds, final BeeRow projectRow) {
    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_USERS, Lists
        .newArrayList(AdministrationConstants.COL_USER),
        Filter.equals(ProjectConstants.COL_PROJECT, projectRow.getId()), new RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet projectUsers) {
            for (IsRow row : projectUsers) {
              userIds.remove(row.getLong(projectUsers
                  .getColumnIndex(AdministrationConstants.COL_USER)));
            }

            for (Long user : userIds) {
              Queries.insert(ProjectConstants.VIEW_PROJECT_USERS, Data.getColumns(
                  ProjectConstants.VIEW_PROJECT_USERS, Lists.newArrayList(
                      ProjectConstants.COL_PROJECT, AdministrationConstants.COL_USER)),
                  Lists.newArrayList(BeeUtils.toString(projectRow.getId()), BeeUtils
                      .toString(user)));
            }
          }
        });
  }
}
