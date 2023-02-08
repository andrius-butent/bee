package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.client.modules.tasks.TaskHelper.isMaterialTypeRequired;
import static com.butent.bee.client.modules.tasks.TaskHelper.isTaskOrder;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.PLEVELE;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.composite.Relations;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.modules.projects.ProjectsHelper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class TaskBuilder extends ProductSupportInterceptor {

  private static final String NAME_START_DATE = "Start_Date";
  private static final String NAME_START_TIME = "Start_Time";
  private static final String NAME_START_TIME_LABEL = "StartTimeLabel";
  private static final String NAME_END_DATE = "End_Date";
  private static final String NAME_END_TIME = "End_Time";

  private static final String NAME_EXPECTED_DURATION = "ExpectedDuration";
  private static final String NAME_USER_GROUP_SETTINGS = "UserGroupSettings";
  private static final String NAME_EXECUTORS_LABEL = "ExecutorsLabel";
  private static final String NAME_END_DATE_LABEL = "EndDateLabel";
  private static final String NAME_EXECUTORS = "Executors";

  private static final String NAME_OBSERVERS = "Observers";
  private static final String NAME_OBSERVER_GROUPS = "ObserverGroups";
  private static final String NAME_NOT_SCHEDULED_TASK = "NotScheduledTask";
  private static final String NAME_LABEL_SUFFIX = "Label";

  private final List<String> pleveleType = Arrays.asList("3551 su oro kanalais (Auto)", "3551 be oro kanalų (Auto)",
          "3951 su oro kanalais (Wrap)", "3951 be oro kanalų (Wrap)", "kita");
  private final List<String> tentasType = Arrays.asList("440 laminuotas", "450 lietas", "270 mešas", "450 block out",
          "450 šviesdėžinis", "kitas");

  private final String[] widgets = {
      NAME_START_DATE, NAME_START_TIME, NAME_END_DATE, NAME_END_TIME,
      NAME_EXPECTED_DURATION, NAME_EXECUTORS, NAME_USER_GROUP_SETTINGS, COL_PRIVATE_TASK,
      PROP_MAIL, NAME_OBSERVERS, NAME_OBSERVER_GROUPS};

  private final List<String> orderColumns = Arrays.asList("Price", "PressSeries", "PressNumber",
      "PressDate", "DecorationPrice", "Date", "PriceSeries", "Number");

  private InputBoolean mailToggle;
  private InputTime expectedDurationInput;
  private Label endDateInputLabel;
  private Label startDateInputLabel;
  private Label executorsLabel;

  private MultiSelector executors;
  private MultiSelector executorGroups;
  private MultiSelector observers;
  private MultiSelector observerGroups;
  private DataSelector projectSelector;
  private DataSelector stagesSelector;

  private InputBoolean notScheduledTask;
  private Relations relations;
  private FileCollector fileCollector;
  private boolean taskIdsCallback;

  private ListBox materialTypeList;
  private Label materialTypeLabel;
  private ListBox paperEnvelope;

  TaskBuilder() {
    super();
  }

  TaskBuilder(boolean taskIdsCallback) {
    this();
    this.taskIdsCallback = taskIdsCallback;
  }

  @Override
  public void afterCreateEditableWidget(final EditableWidget editableWidget,
      IdentifiableWidget widget) {

    if (BeeUtils.inList(editableWidget.getWidgetName(), NAME_EXPECTED_DURATION,
        NAME_USER_GROUP_SETTINGS)) {

      editableWidget.addCellValidationHandler(event -> {
        if (event.isCellValidation() && event.isPreValidation()
            && BeeUtils.isEmpty(event.getNewValue())) {

          if (BeeUtils.same(editableWidget.getWidgetName(), NAME_EXPECTED_DURATION)) {
            endDateInputLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.same(editableWidget.getWidgetName(), NAME_USER_GROUP_SETTINGS)) {
            executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }

        } else if (event.isCellValidation() && event.isPreValidation()
            && !BeeUtils.isEmpty(event.getNewValue())) {
          if (BeeUtils.same(editableWidget.getWidgetName(), NAME_EXPECTED_DURATION)) {
            endDateInputLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.same(editableWidget.getWidgetName(), NAME_USER_GROUP_SETTINGS)) {
            executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }
        return true;
      });
    } else if (BeeUtils.same(editableWidget.getColumnId(), ProjectConstants.COL_PROJECT)
        && widget instanceof DataSelector) {
      projectSelector = (DataSelector) widget;
      projectSelector.addSelectorHandler(
          event -> {
            FormView form = ViewHelper.getForm(event.getSelector());

            if (!BeeUtils.same(form.getViewName(), VIEW_TASKS) && !form.isEnabled()) {
              return;
            }
            setProjectStagesFilter(form, form.getActiveRow(), event);
            setProjectUsersFilter(form, form.getActiveRow());
          });
    } else if (BeeUtils.same(editableWidget.getColumnId(), ProjectConstants.COL_PROJECT_STAGE)
        && widget instanceof DataSelector) {
      stagesSelector = (DataSelector) widget;
    } else if (BeeUtils.containsSame(orderColumns, editableWidget.getColumnId())) {

      if (widget instanceof InputDate) {
        ((InputDate) widget).addEditStopHandler(event -> {
          if (event.isChanged()) {
            getActiveRow().setValue(Data.getColumnIndex(VIEW_TASK_ORDERS,
                editableWidget.getColumnId()), ((InputDate) event.getSource()).getDate());
            getFormView().refreshBySource(editableWidget.getColumnId());
          }
        });
      } else {
        ((InputText) widget).addValueChangeHandler(valueChangeEvent -> {
          getFormView().refreshBySource(editableWidget.getColumnId());
        });
      }
    }
    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof FileCollector) {
      fileCollector = (FileCollector) widget;
      fileCollector.bindDnd(getFormView());
      fileCollector.addSelectionHandler(selectionEvent -> {
        FormView form = getFormView();
        IsRow row = form == null ? getActiveRow() : form.getActiveRow();

        if (row == null) {
          return;
        }
        row.setProperty(PROP_FILES, Codec.beeSerialize(fileCollector.getFiles()));
        form.refreshBySource(PROP_FILES);
      });

    } else if (BeeUtils.same(name, PROP_MAIL) && (widget instanceof InputBoolean)) {
      mailToggle = (InputBoolean) widget;

    } else if (BeeUtils.same(name, NAME_END_DATE_LABEL) && (widget instanceof Label)) {
      endDateInputLabel = (Label) widget;

    } else if (BeeUtils.same(name, NAME_START_TIME_LABEL) && (widget instanceof Label)) {
      startDateInputLabel = (Label) widget;

    } else if (BeeUtils.same(name, NAME_EXPECTED_DURATION) && (widget instanceof InputTime)) {
      expectedDurationInput = (InputTime) widget;

      expectedDurationInput.addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          if (!BeeUtils.isEmpty(expectedDurationInput.getText()) && endDateInputLabel != null) {
            endDateInputLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(event.getValue()) && endDateInputLabel != null) {
            endDateInputLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }
      });

    } else if (BeeUtils.same(NAME_EXECUTORS_LABEL, name) && (widget instanceof Label)) {
      executorsLabel = (Label) widget;

    } else if (BeeUtils.same(NAME_USER_GROUP_SETTINGS, name) && (widget instanceof MultiSelector)) {
      final MultiSelector userGroups = (MultiSelector) widget;
      executorGroups = userGroups;

      userGroups.addEditChangeHandler(new EditChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          if (!BeeUtils.isEmpty(event.getValue()) && executorsLabel != null) {
            executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(event.getValue()) && endDateInputLabel != null) {
            executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }

        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (!BeeUtils.isEmpty(userGroups.getDisplayValue()) && executorsLabel != null) {
            executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(userGroups.getDisplayValue()) && endDateInputLabel != null) {
            executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }
      });

      userGroups.addKeyDownHandler(event -> {
        if (!BeeUtils.isEmpty(userGroups.getDisplayValue()) && executorsLabel != null) {
          executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
        } else if (BeeUtils.isEmpty(userGroups.getDisplayValue()) && endDateInputLabel != null) {
          executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
        }
      });
    } else if (BeeUtils.same(NAME_EXECUTORS, name) && (widget instanceof MultiSelector)) {
      executors = (MultiSelector) widget;
    } else if (BeeUtils.same(NAME_OBSERVERS, name) && (widget instanceof MultiSelector)) {
      observers = (MultiSelector) widget;
    } else if (BeeUtils.same(NAME_OBSERVER_GROUPS, name) && (widget instanceof MultiSelector)) {
      observerGroups = (MultiSelector) widget;
    } else if (BeeUtils.same(name, NAME_NOT_SCHEDULED_TASK)) {
      notScheduledTask = (InputBoolean) widget;

      notScheduledTask.addValueChangeHandler(valueChangeEvent -> {
        styleRequiredFields(!BeeUtils.toBoolean(valueChangeEvent.getValue()));

        if (!BeeUtils.toBoolean(valueChangeEvent.getValue())) {
          executors.setStyleName(StyleUtils.NAME_DISABLED, false);
          observers.setStyleName(StyleUtils.NAME_DISABLED, false);
          executorGroups.setStyleName(StyleUtils.NAME_DISABLED, false);
          observerGroups.setStyleName(StyleUtils.NAME_DISABLED, false);

          if (DataUtils.isId(getActiveRow().getLong(getDataIndex(ProjectConstants.COL_PROJECT)))) {
            executorGroups.setStyleName(StyleUtils.NAME_DISABLED, true);
            observerGroups.setStyleName(StyleUtils.NAME_DISABLED, true);
            executorGroups.setEnabled(false);
            observerGroups.setEnabled(false);
            executors.setEnabled(true);
            observers.setEnabled(true);
          }
        }
      });
    } else if (BeeUtils.same(name,
        AdministrationConstants.TBL_RELATIONS) && widget instanceof Relations) {
      relations = (Relations) widget;
    } else if (BeeUtils.same(name, VIEW_TASK_ORDER_ITEMS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
          IsRow activeRow = getFormView().getActiveRow();

          if (!DataUtils.hasId(activeRow)) {
            Widget widget;
            String executors = activeRow.getProperty(PROP_EXECUTORS);

            if (BeeUtils.isEmpty(executors) || DataUtils.parseIdList(executors).size() != 1) {
              getFormView().notifySevere(Localized.dictionary().crmEnterOneExecutor());

              widget = getFormView().getWidgetByName(NAME_EXECUTORS);
              ((MultiSelector) widget).setFocus(true);

              return false;
            }

            DateTime start = getStart();
            DateTime end = getEnd(start, Data.getString(VIEW_TASKS, activeRow,
                COL_EXPECTED_DURATION));

            if (start == null) {
              getFormView().notifySevere(Localized.dictionary().fieldRequired(
                  Localized.dictionary().crmStartDate()));

              widget = getFormView().getWidgetByName(NAME_START_DATE);
              ((InputDate) widget).setFocus(true);
              return false;
            }

            if (end == null) {
              getFormView().notifySevere(Localized.dictionary().fieldRequired(
                  Localized.dictionary().crmFinishDate()));

              widget = getFormView().getWidgetByName(NAME_END_DATE);
              ((InputDate) widget).setFocus(true);
              return false;
            }

            if (maybeNotifyEmptyProduct(msg -> getFormView().notifySevere(msg))) {

              widget = getFormView().getWidgetBySource(COL_PRODUCT);
              ((DataSelector) widget).setFocus(true);
              return false;
            }
          }

          return true;
        }
      });
    } else if (BeeUtils.same(name, COL_MATERIAL_TYPE) && widget instanceof ListBox) {
      materialTypeList = (ListBox) widget;
    } else if (BeeUtils.same(name, NAME_MATERIAL_TYPE_LABEL) && widget instanceof Label) {
      materialTypeLabel = (Label) widget;
    } else if (BeeUtils.same(name, COL_PAPER_ENVELOPE) && widget instanceof ListBox) {
      paperEnvelope = (ListBox) widget;
      paperEnvelope.addChangeHandler(changeEvent -> {
        setMaterialTypeRequired(paperEnvelope.getValue());
        setMaterialTypes(paperEnvelope.getValue());
      });
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    Integer status = getActiveRow().getInteger(getDataIndex(COL_STATUS));
    if (Objects.equals(TaskStatus.NOT_SCHEDULED.ordinal(), status)) {
      notScheduledTask.setChecked(true);
    }

    styleRequiredFields(!notScheduledTask.isChecked());
    setProjectStagesFilter(form, row);
    setProjectUsersFilter(form, row);
  }

  @Override
  public void beforeStateChange(State state, boolean modal) {
    if (state == State.OPEN && mailToggle != null && mailToggle.isChecked()) {
      mailToggle.setChecked(false);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new TaskBuilder();
  }

  @Override
  public boolean isWidgetEditable(EditableWidget editableWidget, IsRow row) {
    if (editableWidget != null) {
      boolean value = BeeUtils.unbox(Boolean.valueOf(notScheduledTask.getValue()));

      for (String widget : widgets) {
        String name = BeeUtils.nvl(editableWidget.getColumnId(), editableWidget.getWidgetName());

        if (!BeeUtils.isEmpty(name) && Objects.equals(name, widget)) {
          return !value;
        }
      }
    }
    return super.isWidgetEditable(editableWidget, row);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    TaskStatus status = EnumUtils
        .getEnumByIndex(TaskStatus.class, newRow.getInteger(form.getDataIndex(COL_STATUS)));

    DateTime start = newRow.getDateTime(getDataIndex(COL_START_TIME));
    if (start == null && !TaskStatus.NOT_SCHEDULED.equals(status)) {
      start = TimeUtils.nowHours(1);
    }

    Widget widget = getFormView().getWidgetByName(NAME_START_DATE);
    if (widget instanceof InputDate) {
      ((InputDate) widget).setDate(start);
    }

    DateTime end = newRow.getDateTime(getDataIndex(COL_FINISH_TIME));
    DateTime endTime = newRow.getDateTime(getDataIndex(COL_FINISH_TIME));

    if (end != null) {
      widget = getFormView().getWidgetByName(NAME_END_DATE);
      if (widget instanceof InputDate) {
        ((InputDate) widget).setDate(end);
      }
      widget = getFormView().getWidgetByName(NAME_END_TIME);
      if (widget instanceof InputTime) {
        ((InputTime) widget).setTime(endTime);
      }
    }

    if (!TaskStatus.NOT_SCHEDULED.equals(status)) {
      Widget w = getFormView().getWidgetByName(NAME_END_TIME);
      if (w instanceof InputTime) {
        ((InputTime) w).setMillis(Global.getParameterTime(TaskConstants.PRM_END_OF_WORK_DAY));
      }
      newRow.setValue(form.getDataIndex(COL_PRIVATE_TASK),
          Global.getParameterBoolean(TaskConstants.PRM_CREATE_PRIVATE_TASK_FIRST));
      form.refreshBySource(COL_PRIVATE_TASK);
    }

    if (relations != null) {
      relations.requery(newRow, newRow.getId());
    }
    showFiles(newRow);
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    String oldProperty = oldRow.hasPropertyValue(PROP_FILES)
      ? oldRow.getProperty(PROP_FILES)
      : BeeConst.STRING_EMPTY;

    String newProperty = newRow.hasPropertyValue(PROP_FILES)
      ? newRow.getProperty(PROP_FILES)
      : BeeConst.STRING_EMPTY;

    if (!BeeUtils.same(oldProperty, newProperty)) {
      String msg = DataUtils.isNewRow(newRow) ? Localized.dictionary().newValues()
        : Localized.dictionary().changedValues();
      messages.add((!BeeUtils.isEmpty(msg) ? msg : BeeConst.STRING_EMPTY)
        + BeeConst.STRING_SPACE + Localized.dictionary().files());
    }
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();

    IsRow activeRow = getFormView().getActiveRow();
    DateTime start = getStart();
    DateTime end = getEnd(start, Data.getString(VIEW_TASKS, activeRow, COL_EXPECTED_DURATION));

    boolean notSheduledTask = notScheduledTask.isChecked();

    boolean noExecutors = BeeUtils.allEmpty(activeRow.getProperty(PROP_EXECUTORS),
        activeRow.getProperty(PROP_EXECUTOR_GROUPS));

    if (isMaterialTypeRequired(getStringValue(COL_PAPER_ENVELOPE)) && BeeUtils.isEmpty(getStringValue(COL_MATERIAL_TYPE))) {
      getFormView().notifySevere(Localized.dictionary().fieldRequired(Localized.dictionary().type()));
      return;
    }

    if (start == null && start == end && noExecutors && notSheduledTask) {
      if (maybeNotifyEmptyProduct(msg -> event.getCallback().onFailure(msg))) {
        return;
      }
      createNotScheduledTask(event.getCallback());
    } else {
      createTasks(activeRow, event.getCallback(), null);
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, final SaveChangesEvent event) {
    event.consume();
    final IsRow activeRow = getFormView().getActiveRow();
    DateTime start = getStart();
    DateTime end = getEnd(start, Data.getString(VIEW_TASKS, activeRow, COL_EXPECTED_DURATION));

    boolean noExecutors = BeeUtils.allEmpty(activeRow.getProperty(PROP_EXECUTORS),
        activeRow.getProperty(PROP_EXECUTOR_GROUPS));

    if (maybeNotifyEmptyProduct(msg -> event.getCallback().onFailure(msg))) {
      return;
    }

    if (start == null && start == end && noExecutors && notScheduledTask != null
        && notScheduledTask.isChecked()) {
      updateRow(event, null, null, null, null);
    } else {
      createTasks(activeRow, event.getCallback(), event);
    }
  }

  @Override
  public void onSetActiveRow(IsRow row) {
    if (relations != null && !BeeUtils.isEmpty(relations.getParentId())) {
      relations.setParentId(null);
    }
    super.onSetActiveRow(row);
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    Long owner = row.getLong(form.getDataIndex(COL_OWNER));
    form.setEnabled(Objects.equals(owner, BeeKeeper.getUser().getUserId()));

    if (fileCollector != null) {
      fileCollector.clear();
    }
    ParameterList params = TasksKeeper.createArgs(SVC_GET_TASK_DATA);
    params.addDataItem(VAR_TASK_ID, row.getId());
    params.addDataItem(VAR_TASK_PROPERTIES, DEFAULT_TASK_PROPERTIES);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!response.hasErrors()) {
          BeeRow data = BeeRow.restore((String) response.getResponse());
          if (data != null) {
            if (relations != null) {
              relations.requery(data, data.getId());
            }
            showFiles(data);
            form.updateRow(data, true);

            if (focusCommand != null) {
              focusCommand.execute();
            }
          }
        }
      }
    });
    return false;
  }


  private static BeeRow getResponseRow(String caption, ResponseObject ro, RowCallback callback) {
    if (!Queries.checkResponse(caption, BeeConst.UNDEF, VIEW_TASKS, ro, BeeRow.class, null)
        || ro.isEmpty()) {
      return null;
    }

    BeeRow row = BeeRow.restore((String) ro.getResponse());
    if (row == null && callback != null) {
      callback.onFailure(caption, VIEW_TASKS, "cannot restore row");
    }
    return row;
  }

  private static void getUserGroupMembers(String groupList, Callback<Set<Long>> callback) {
    final Set<Long> users = new HashSet<>();

    Set<Long> groups = DataUtils.parseIdSet(groupList);
    if (groups.isEmpty()) {
      callback.onSuccess(users);
      return;
    }

    Value time = Value.getValue(System.currentTimeMillis());
    Filter blockedUser = Filter.or(Filter.and(Filter.isLessEqual(
        AdministrationConstants.COL_USER_BLOCK_FROM, time), Filter.isMore(
        AdministrationConstants.COL_USER_BLOCK_UNTIL, time)), Filter.isLessEqual(
        AdministrationConstants.COL_USER_BLOCK_FROM, time), Filter.isMore(
        AdministrationConstants.COL_USER_BLOCK_UNTIL, time));

    Queries.getRowSet(AdministrationConstants.VIEW_USER_GROUP_MEMBERS, Lists.newArrayList(
        AdministrationConstants.COL_UG_USER), Filter.and(Filter.any(
        AdministrationConstants.COL_UG_GROUP,
        groups), Filter.isNot(blockedUser)), new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        for (int i = 0; i < result.getNumberOfRows(); i++) {
          Long member = result.getLong(i, AdministrationConstants.COL_UG_USER);

          if (DataUtils.isId(member)) {
            users.add(member);
          }
        }
        callback.onSuccess(users);
      }
    });
  }

  private static void setVisibleProjectData(FormView form, boolean visible) {
    Widget widget = form.getWidgetBySource(ProjectConstants.COL_PROJECT);
    if (widget != null) {
      widget.setVisible(visible);
    }

    widget = form.getWidgetBySource(ProjectConstants.COL_PROJECT_STAGE);

    if (widget != null) {
      widget.setVisible(visible);
    }

    widget = form.getWidgetByName(ProjectConstants.COL_PROJECT + NAME_LABEL_SUFFIX);

    if (widget != null) {
      widget.setVisible(visible);
    }

    widget = form.getWidgetByName(ProjectConstants.COL_PROJECT_STAGE + NAME_LABEL_SUFFIX);

    if (widget != null) {
      widget.setVisible(visible);
    }
  }

  private void clearValue(String widgetName) {
    Widget widget = getFormView().getWidgetByName(widgetName);
    if (widget instanceof Editor) {
      ((Editor) widget).clearValue();
    }
  }

  private BeeRow createNewRow(IsRow activeRow, DateTime start, DateTime end) {
    String summary = activeRow.getString(Data.getColumnIndex(VIEW_TASKS, COL_SUMMARY)).trim();
    BeeRow newRow = DataUtils.cloneRow(activeRow);

    if (start != null) {
      Data.setValue(VIEW_TASKS, newRow, COL_START_TIME, start);
    }

    if (end != null) {
      Data.setValue(VIEW_TASKS, newRow, COL_FINISH_TIME, end);
    }
    Data.setValue(VIEW_TASKS, newRow, COL_SUMMARY, summary);

    if (mailToggle != null && mailToggle.isChecked()) {
      newRow.setProperty(PROP_MAIL, BooleanValue.S_TRUE);
    }

    if (relations != null && !relations.getRowChildren(true).isEmpty()) {
      Collection<RowChildren> relatedData = new ArrayList<>();

      relations.getRowChildren(true).forEach(relation ->
          relatedData.add(RowChildren.create(relation.getRepository(), relation.getParentColumn(),
              null, relation.getChildColumn(), relation.getChildrenIds()))
      );
      newRow.setProperty(VAR_TASK_RELATIONS, Codec.beeSerialize(relatedData));
    }

    return newRow;
  }

  private void createNotScheduledTask(RowCallback callback) {
    IsRow activeRow = getFormView().getActiveRow();

    BeeRow newRow = createNewRow(activeRow, null, null);
    BeeRowSet rowSet = DataUtils.createRowSetForInsert(VIEW_TASKS, getFormView().getDataColumns(),
        newRow, Sets.newHashSet(COL_STATUS), true);

    ParameterList args = TasksKeeper.createTaskRequestParameters(TaskEvent.CREATE_NOT_SCHEDULED);
    args.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        TaskBuilder.this.onResponse(callback, response);
      }
    });

  }

  private void createTasks(IsRow activeRow, RowCallback callback,
      SaveChangesEvent notScheduledTaskUdateEvent) {
    DateTime start = getStart();

    if (start == null) {
      callback.onFailure(Localized.dictionary().crmEnterStartDate());
      return;
    }

    DateTime nowTime = TimeUtils.nowMillis();
    InputTime widget = (InputTime) getFormView().getWidgetByName(NAME_START_TIME);
    if (widget != null) {
      String time = widget.getValue();

      if (BeeUtils.isEmpty(time)) {
        if (nowTime.getDate().getDays() < start.getDate().getDays()) {
          widget.setMillis(Global.getParameterTime(TaskConstants.PRM_START_OF_WORK_DAY));
        } else {
          widget.setTime(TimeUtils.nowMillis());
        }
      }
    }

    start = getStart();

    DateTime end = getEnd(start, Data.getString(VIEW_TASKS, activeRow, COL_EXPECTED_DURATION));
    if (end == null) {
      callback.onFailure(Localized.dictionary().crmEnterFinishDateOrEstimatedTime());
      return;
    }

    if (TimeUtils.isLeq(end, start)) {
      callback.onFailure(Localized.dictionary().crmFinishTimeMustBeGreaterThanStart());
      return;
    }

    if (Data.isNull(VIEW_TASKS, activeRow, COL_SUMMARY)) {
      callback.onFailure(Localized.dictionary().crmEnterSubject());
      return;
    }

    if (BeeUtils.allEmpty(activeRow.getProperty(PROP_EXECUTORS),
        activeRow.getProperty(PROP_EXECUTOR_GROUPS))) {
      callback.onFailure(Localized.dictionary().crmSelectExecutor());
      return;
    }

    if (notScheduledTaskUdateEvent != null) {
      startNotScheduledTask(activeRow, start, end, notScheduledTaskUdateEvent);
      return;
    }

    if (maybeNotifyEmptyProduct(callback::onFailure)) {
      return;
    }

    BeeRow newRow = createNewRow(activeRow, start, end);
    BeeRowSet rowSet = DataUtils.createRowSetForInsert(VIEW_TASKS, getFormView().getDataColumns(),
        newRow, Sets.newHashSet(COL_EXECUTOR, COL_STATUS), true);

    ParameterList args = TasksKeeper.createTaskRequestParameters(TaskEvent.CREATE);
    args.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        TaskBuilder.this.onResponse(callback, response);
      }
    });
  }

  private HasDateValue getDate(String widgetName) {
    Widget widget = getFormView().getWidgetByName(widgetName);
    if (widget instanceof InputDate) {
      return ((InputDate) widget).getDate();
    } else {
      return null;
    }
  }

  private DateTime getEnd(DateTime start, String duration) {
    HasDateValue datePart = getDate(NAME_END_DATE);
    if (datePart != null) {
      return TimeUtils.combine(datePart, getMillis(NAME_END_TIME));
    }

    if (start != null && !BeeUtils.isEmpty(duration)) {
      Long millis = TimeUtils.parseTime(duration);
      if (BeeUtils.isPositive(millis)) {
        return new DateTime(start.getTime() + millis);
      }
    }
    return null;
  }

  private Long getMillis(String widgetName) {
    Widget widget = getFormView().getWidgetByName(widgetName);
    if (widget instanceof InputTime) {
      return ((InputTime) widget).getMillis();
    } else {
      return null;
    }
  }

  private DateTime getStart() {
    HasDateValue datePart = getDate(NAME_START_DATE);
    if (datePart == null) {
      return null;
    } else {
      return TimeUtils.combine(datePart, getMillis(NAME_START_TIME));
    }
  }

  private void onResponse(RowCallback callback, ResponseObject response) {
    Assert.notNull(response);

    if (response.hasWarnings()) {
      BeeKeeper.getScreen().notifyWarning(response.getWarnings());
    }

    if (response.hasErrors()) {
      callback.onFailure(response.getErrors());

    } else if (!response.hasResponse()) {
      callback.onFailure("No tasks created");

    } else if (response.hasResponse(BeeRowSet.class)) {
      BeeRowSet tasks = BeeRowSet.restore(response.getResponseAsString());
      if (tasks.isEmpty()) {
        callback.onFailure("No tasks created");
        return;
      }

/*      clearValue(NAME_START_DATE);
      clearValue(NAME_START_TIME);
      clearValue(NAME_END_DATE);
      clearValue(NAME_END_TIME);*/

      updateAndCreateFiles(tasks.getRowIds());

      if (taskIdsCallback) {
        BeeRow row = new BeeRow(0, new String[] {DataUtils.buildIdList(tasks)});
        callback.onSuccess(row);
      }

      String message = Localized.dictionary().crmCreatedNewTasks(tasks.getNumberOfRows());
      BeeKeeper.getScreen().notifyInfo(message);

      tasks.getRow(0).setProperty(PROP_EXECUTORS, getActiveRow().getProperty(PROP_EXECUTORS));
      if (!taskIdsCallback && tasks.getNumberOfRows() == 1) {
        callback.onSuccess(tasks.getRow(0));
      } else {
        callback.onSuccess(null);
      }

      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TASKS);

    } else {
      callback.onFailure("Unknown response");
    }
  }

  private void setProjectStagesFilter(FormView form, IsRow row) {
    setProjectStagesFilter(form, row, null);
  }

  private void setProjectStagesFilter(FormView form, IsRow row, SelectorEvent event) {
    if (event != null && DataUtils.isId(event.getValue())) {
      if (!Data.equals(form.getViewName(), row, ProjectConstants.COL_PROJECT, event.getValue())) {
        if (row.getProperties() != null) {
          row.getProperties().remove(PROP_EXECUTORS);
          row.getProperties().remove(PROP_OBSERVERS);
        }

        Data.clearCell(form.getViewName(), row, ProjectConstants.COL_PROJECT_STAGE);
        RelationUtils.clearRelatedValues(Data.getDataInfo(form.getViewName()),
            ProjectConstants.COL_PROJECT_STAGE, row);

        form.refreshBySource(PROP_EXECUTORS);
        form.refreshBySource(PROP_OBSERVERS);
        form.refreshBySource(ProjectConstants.COL_PROJECT_STAGE);
      }

      TaskHelper.setSelectorFilter(stagesSelector,
          Filter.equals(ProjectConstants.COL_PROJECT, event.getValue()));
      return;
    }

    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);
    int idxProjectUser = form.getDataIndex(ProjectConstants.ALS_FILTERED_PROJECT_USER);

    if (BeeConst.isUndef(idxProjectOwner) || BeeConst.isUndef(idxProject) || BeeConst.isUndef(
        idxProjectUser)) {
      return;
    }

    TaskHelper.setWidgetEnabled(projectSelector, false);
    TaskHelper.setWidgetEnabled(stagesSelector, false);

    long projectId = BeeUtils.unbox(row.getLong(idxProject));

    setVisibleProjectData(form, DataUtils.isId(projectId));

    if (!ProjectsHelper.isProjectOwner(form, row) || !ProjectsHelper.isProjectUser(form, row)) {
      return;
    }

    TaskHelper.setSelectorFilter(projectSelector, Filter.and(
        Filter.equals(ProjectConstants.COL_PROJECT_OWNER, BeeKeeper.getUser().getUserId()),
        Filter.or(
            Filter.equals(ProjectConstants.COL_PROJECT_STATUS, ProjectStatus.ACTIVE.ordinal()),
            Filter.equals(ProjectConstants.COL_PROJECT_STATUS, ProjectStatus.SCHEDULED.ordinal())
        )
    ));

    TaskHelper.setSelectorFilter(stagesSelector,
        Filter.equals(ProjectConstants.COL_PROJECT, projectId));

    TaskHelper.setWidgetEnabled(projectSelector, ProjectsHelper.isProjectOwner(form, row));
    TaskHelper.setWidgetEnabled(stagesSelector, true);
  }

  private void setProjectUsersFilter(final FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);

    if (BeeConst.isUndef(idxProjectOwner)) {
      return;
    }

    if (BeeConst.isUndef(idxProject)) {
      return;
    }

    final long projectOwner = BeeUtils.unbox(row.getLong(idxProjectOwner));
    long projectId = BeeUtils.unbox(row.getLong(idxProject));

    if (!DataUtils.isId(projectId)) {
      return;
    }
    TaskHelper.setWidgetEnabled(executors, false);
    TaskHelper.setWidgetEnabled(executorGroups, false);
    TaskHelper.setWidgetEnabled(observers, false);
    TaskHelper.setWidgetEnabled(observerGroups, false);

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_USERS, Lists
        .newArrayList(AdministrationConstants.COL_USER), Filter.isEqual(
        ProjectConstants.COL_PROJECT, Value.getValue(projectId)), new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          if (executors != null) {
            executors.getOracle().setAdditionalFilter(Filter.compareId(projectOwner), true);
            TaskHelper.setWidgetEnabled(executors, notScheduledTask != null
                && !notScheduledTask.isChecked());
            return;
          }
        }

        List<Long> userIds = Lists.newArrayList(projectOwner);
        int idxUser = result.getColumnIndex(AdministrationConstants.COL_USER);

        if (BeeConst.isUndef(idxUser)) {
          Assert.untouchable();
          return;
        }

        for (IsRow userRow : result) {
          long projectUser = BeeUtils.unbox(userRow.getLong(idxUser));

          if (DataUtils.isId(projectUser)) {
            userIds.add(projectUser);
          }
        }

        if (executors != null) {
          executors.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          TaskHelper.setWidgetEnabled(executors, notScheduledTask != null
              && !notScheduledTask.isChecked());
        }

        if (observers != null) {
          observers.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          TaskHelper.setWidgetEnabled(observers, notScheduledTask != null
              && !notScheduledTask.isChecked());
        }
      }
    });
  }

  private void showFiles(IsRow row) {
    if (fileCollector == null) {
      return;
    }
    List<FileInfo> files = TaskUtils.getFiles(row);
    fileCollector.clear();
    fileCollector.addFiles(files);
  }

  private void startNotScheduledTask(IsRow activeRow, final DateTime start, final DateTime end,
      final SaveChangesEvent event) {
    if (!BeeUtils.allEmpty(activeRow.getProperty(PROP_EXECUTORS),
        activeRow.getProperty(PROP_EXECUTOR_GROUPS))) {
      final List<Long> executorList = DataUtils.parseIdList(activeRow.getProperty(
          PROP_EXECUTORS));
      final List<Long> observersList = DataUtils.parseIdList(activeRow.getProperty(
          PROP_OBSERVERS));

      getUserGroupMembers(activeRow.getProperty(PROP_EXECUTOR_GROUPS), new Callback<Set<Long>>() {

        @Override
        public void onSuccess(Set<Long> executorMembers) {
          if (!executorMembers.isEmpty()) {
            for (Long member : executorMembers) {
              if (!executorList.contains(member) && !observersList.contains(member)) {
                executorMembers.add(member);
              }
            }
          }

          final Long newExecutor = executorList.get(0);

          updateRow(event, newExecutor, start, end, new Callback<BeeRow>() {

            @Override
            public void onSuccess(BeeRow result) {
              executorList.remove(newExecutor);

              String message = Localized.dictionary().crmTaskReturningForExecution();
              BeeKeeper.getScreen().notifyInfo(BeeUtils.joinWords(message, result.getId()));

              if (executorList.isEmpty()) {
                event.getCallback().onSuccess(result);
                return;
              }
              activeRow.setProperty(PROP_EXECUTORS, DataUtils.buildIdList(executorList));
              activeRow.setProperty(PROP_EXECUTOR_GROUPS, DataUtils.buildIdList(
                  (BeeRowSet) null));

              createTasks(activeRow, event.getCallback(), null);

            }
          });
        }
      });
    } else {
      createTasks(activeRow, event.getCallback(), null);
    }
  }

  private void styleRequiredFields(boolean value) {
    FormView form = getFormView();

    executorsLabel.setStyleName(StyleUtils.NAME_REQUIRED, value);
    startDateInputLabel.setStyleName(StyleUtils.NAME_REQUIRED, value);
    endDateInputLabel.setStyleName(StyleUtils.NAME_REQUIRED, value);

    InputDate startInputDate = (InputDate) form.getWidgetByName(NAME_START_DATE);
    startInputDate.setEnabled(value);

    InputTime startInputTime = (InputTime) form.getWidgetByName(NAME_START_TIME);
    startInputTime.setEnabled(value);

    InputDate endInputDate = (InputDate) form.getWidgetByName(NAME_END_DATE);
    endInputDate.setEnabled(value);

    InputTime endInputTime = (InputTime) form.getWidgetByName(NAME_END_TIME);
    endInputTime.setEnabled(value);

    expectedDurationInput.setEnabled(value);

    executors.setEnabled(value);
    executorGroups.setEnabled(value);
    observers.setEnabled(value);
    observerGroups.setEnabled(value);

    InputBoolean privateTask = (InputBoolean) form.getWidgetBySource(COL_PRIVATE_TASK);
    privateTask.setEnabled(value);

    mailToggle.setEnabled(value);

    if (!value) {
      startInputDate.clearValue();
      startInputTime.clearValue();
      endInputDate.clearValue();
      endInputTime.clearValue();
      expectedDurationInput.clearValue();
      getActiveRow().clearCell(getDataIndex(COL_EXPECTED_DURATION));

      getActiveRow().removeProperty(PROP_EXECUTORS);
      getActiveRow().removeProperty(PROP_EXECUTOR_GROUPS);
      getActiveRow().removeProperty(PROP_OBSERVERS);
      getActiveRow().removeProperty(PROP_OBSERVER_GROUPS);

      executors.clearValue();
      executorGroups.clearValue();
      observers.clearValue();
      observerGroups.clearValue();

      mailToggle.setChecked(false);
      privateTask.clearValue();
      getActiveRow().clearCell(getDataIndex(COL_PRIVATE_TASK));
    }
  }

  private void updateAndCreateFiles(final List<Long> tasks) {
    if (fileCollector == null) {
      return;
    }
    List<FileInfo> files = new ArrayList<>();
    BeeUtils.addAllNotNull(files, fileCollector.getFiles());
    Set<Long> uploadedFiles = new HashSet<>();

    files.forEach(file -> {
        if (DataUtils.isId(file.getId())) {
          uploadedFiles.add(file.getId());
        }
      }
    );
    Filter deletionFilter = Filter.any(COL_TASK, tasks);

    if (!BeeUtils.isEmpty(uploadedFiles)) {
      deletionFilter = Filter.and(deletionFilter,
        Filter.isNot(Filter.any(AdministrationConstants.COL_FILE, uploadedFiles)));
    }
    Queries.delete(VIEW_TASK_FILES, deletionFilter, new Queries.IntCallback() {
      @Override
      public void onSuccess(Integer result) {
        files.forEach(file ->
          FileUtils.uploadFile(file, info ->
            tasks.forEach(task -> {
              Long fileId = info.getId();
              List<String> values = Lists.newArrayList(BeeUtils.toString(task),
                BeeUtils.toString(fileId), file.getCaption());

              Queries.update(VIEW_TASK_FILES, Filter.and(
                Filter.equals(COL_TASK, task),
                Filter.equals(AdministrationConstants.COL_FILE, fileId)), COL_CAPTION,
                file.getCaption(), new Queries.IntCallback() {
                  @Override
                  public void onSuccess(Integer result) {
                    if (BeeUtils.isPositive(result)) {
                      return;
                    }
                    List<BeeColumn> columns = Data.getColumns(VIEW_TASK_FILES,
                      Lists.newArrayList(COL_TASK, AdministrationConstants.COL_FILE, COL_CAPTION));
                    Queries.insert(VIEW_TASK_FILES, columns, values);
                  }
                });
            })
          )
        );
      }
    });
  }

  private void updateRow(final SaveChangesEvent event, Long newExecutor, DateTime start,
      DateTime end, Callback<BeeRow> callback) {

    final IsRow oldRow = event.getOldRow();
    IsRow newRow = event.getNewRow();

    if (oldRow == null || newRow == null) {
      return;
    }
    String oldFiles = oldRow.hasPropertyValue(PROP_FILES)
      ? oldRow.getProperty(PROP_FILES)
      : BeeConst.STRING_EMPTY;

    String newFiles = newRow.hasPropertyValue(PROP_FILES)
      ? newRow.getProperty(PROP_FILES)
      : BeeConst.STRING_EMPTY;

    if (event.isEmpty() && TaskUtils.sameObservers(oldRow, newRow)
        && (relations != null && BeeUtils.isEmpty(relations.getChildrenForUpdate()))
        && !DataUtils.isId(newExecutor) && BeeUtils.same(oldFiles, newFiles)) {
      event.setConsumed(false);
      return;
    }

    BeeRow updateRow = DataUtils.cloneRow(getFormView()
        .getActiveRow());

    if (DataUtils.isId(newExecutor)) {
      Data.setValue(VIEW_TASKS, updateRow, COL_EXECUTOR, newExecutor);

      TaskStatus newStatus = TaskStatus.NOT_VISITED;

      /** create task for self */
      if (Objects.equals(newExecutor, BeeKeeper.getUser().getUserId())) {
        newStatus = TaskStatus.VISITED;
      }

      updateRow.setValue(getDataIndex(COL_STATUS), newStatus.ordinal());
    }

    if (start != null) {
      Data.setValue(VIEW_TASKS, updateRow, COL_START_TIME, start);
    }

    if (end != null) {
      Data.setValue(VIEW_TASKS, updateRow, COL_FINISH_TIME, end);
    }

    Collection<RowChildren> updatedRelations = relations == null ? null
        : relations.getChildrenForUpdate();

    ParameterList params = TaskHelper.createTaskParams(getFormView(), TaskEvent.CREATE_SCHEDULED,
        updateRow, updatedRelations, null);
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        BeeRow data = getResponseRow(TaskEvent.CREATE_SCHEDULED.getCaption(), response,
            event.getCallback());

        if (data != null) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);
          if (callback != null) {
            callback.onSuccess(data);
          }

          if (relations != null && !BeeUtils.isEmpty(relations.getChildrenForUpdate())) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
            relations.requery(data, data.getId());
          }
          updateAndCreateFiles(Lists.newArrayList(data.getId()));
        }
      }
    });
    event.consume();
    event.getCallback().onCancel();
  }

  private void setMaterialTypeRequired(String material) {
    boolean isPleveleOrTentas = Objects.equals(material, TENTAS) || Objects.equals(material, PLEVELE);
    materialTypeLabel.setStyleName(StyleUtils.NAME_REQUIRED, isPleveleOrTentas);
  }

  private void setMaterialTypes(String material) {
    if (Objects.equals(TENTAS, material)) {
      materialTypeList.setItems(tentasType);
    } else if (Objects.equals(PLEVELE, material)) {
      materialTypeList.setItems(pleveleType);
    } else {
      materialTypeList.clear();
    }
  }
}
