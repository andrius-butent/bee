package com.butent.bee.client.modules.tasks;

import com.butent.bee.shared.time.JustDate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.modules.projects.ProjectsHelper;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class TaskBuilder extends ProductSupportInterceptor {

  private static final String NAME_START_DATE = "Start_Date";
  private static final String NAME_START_TIME = "Start_Time";
  private static final String NAME_END_DATE = "End_Date";
  private static final String NAME_END_TIME = "End_Time";
  private static final String NAME_EXPECTED_DURATION = "ExpectedDuration";
  private static final String NAME_USER_GROUP_SETTINGS = "UserGroupSettings";
  private static final String NAME_EXECUTORS_LABEL = "ExecutorsLabel";
  private static final String NAME_END_DATE_LABEL = "EndDateLabel";
  private static final String NAME_EXECUTORS = "Executors";
  private static final String NAME_OBSERVERS = "Observers";
  private static final String NAME_OBSERVER_GROUPS = "ObserverGroups";

  private static final String NAME_REMINDER_DATE = "Reminder_Date";
  private static final String NAME_REMINDER_TIME = "Reminder_Time";
  private static final String NAME_LABEL_SUFFIX = "Label";

  private static final String NAME_FILES = "Files";

  private HasCheckedness mailToggle;
  private InputTime expectedDurationInput;
  private Label endDateInputLabel;
  private Label executorsLabel;

  private MultiSelector executors;
  private MultiSelector executorGroups;
  private MultiSelector observers;
  private MultiSelector observerGroups;

  private final Map<Long, FileInfo> filesToUpload = new HashMap<>();
  private Long executor;
  private boolean taskIdsCallback;

  String startTime;

  TaskBuilder() {
    super();
  }

  TaskBuilder(Map<Long, FileInfo> files, Long executor, boolean taskIdsCallback) {
    this();

    if (files != null) {
      filesToUpload.putAll(files);
    }

    this.executor = executor;
    this.taskIdsCallback = taskIdsCallback;
  }

  @Override
  public void afterCreateEditableWidget(final EditableWidget editableWidget,
      IdentifiableWidget widget) {

    if (BeeUtils.inList(editableWidget.getWidgetName(), NAME_EXPECTED_DURATION,
        NAME_USER_GROUP_SETTINGS)) {

      editableWidget.addCellValidationHandler(new CellValidateEvent.Handler() {
        @Override
        public Boolean validateCell(CellValidateEvent event) {
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
        }
      });
    }

    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof FileCollector) {
      ((FileCollector) widget).bindDnd(getFormView());
      if (!filesToUpload.isEmpty()) {
        ((FileCollector) widget).addFiles(filesToUpload.values());
      }

    } else if (BeeUtils.same(name, PROP_MAIL) && (widget instanceof HasCheckedness)) {
      mailToggle = (HasCheckedness) widget;

    } else if (BeeUtils.same(name, NAME_END_DATE_LABEL) && (widget instanceof Label)) {
      endDateInputLabel = (Label) widget;

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

      userGroups.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (!BeeUtils.isEmpty(userGroups.getDisplayValue()) && executorsLabel != null) {
            executorsLabel.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          } else if (BeeUtils.isEmpty(userGroups.getDisplayValue()) && endDateInputLabel != null) {
            executorsLabel.removeStyleName(StyleUtils.NAME_HAS_DEFAULTS);
          }
        }
      });
    } else if (BeeUtils.same(NAME_EXECUTORS, name) && (widget instanceof MultiSelector)) {
      executors = (MultiSelector) widget;
    } else if (BeeUtils.same(NAME_OBSERVERS, name) && (widget instanceof MultiSelector)) {
      observers = (MultiSelector) widget;
    } else if (BeeUtils.same(NAME_OBSERVER_GROUPS, name) && (widget instanceof MultiSelector)) {
      observerGroups = (MultiSelector) widget;
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
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
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    if (executor != null) {
      newRow.setProperty(PROP_EXECUTORS, DataUtils.buildIdList(executor));
    }

    TaskStatus status = EnumUtils
        .getEnumByIndex(TaskStatus.class, newRow.getInteger(form.getDataIndex(COL_STATUS)));

    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);
    long projectId = BeeUtils.unbox(newRow.getLong(idxProject));

    DateTime start = newRow.getDateTime(getDataIndex(COL_START_TIME));
    DateTime end = newRow.getDateTime(getDataIndex(COL_FINISH_TIME));

    if (DataUtils.isId(projectId)) {

      JustDate projectStartDate = newRow.getDate(Data.getColumnIndex(VIEW_TASKS, ProjectConstants.COL_PROJECT_START_DATE));
      JustDate projectEndDate = newRow.getDate(Data.getColumnIndex(VIEW_TASKS, ProjectConstants.COL_PROJECT_END_DATE));


      if (Objects.nonNull(projectStartDate)) {
        start = projectStartDate.getDateTime();
      }

      if (Objects.nonNull(projectEndDate)) {
        end = projectEndDate.getDateTime();
      }

    } else if (start == null && !TaskStatus.NOT_SCHEDULED.equals(status)) {
      start = TimeUtils.nowHours(1);
    }

    Widget widget = getFormView().getWidgetByName(NAME_START_DATE);
    if (widget instanceof InputDate) {
      ((InputDate) widget).setDate(start);
    }

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

      Global.getParameter(TaskConstants.PRM_END_OF_WORK_DAY, new Consumer<String>() {
        @Override
        public void accept(String input) {

          Widget w = getFormView().getWidgetByName(NAME_END_TIME);
          if (w instanceof InputTime) {
            ((InputTime) w).setValue(input);
          }
        }
      });

      Global.getParameter(TaskConstants.PRM_START_OF_WORK_DAY, new Consumer<String>() {
        @Override
        public void accept(String input) {
          startTime = input;
        }
      });
    }
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();

    IsRow activeRow = getFormView().getActiveRow();
    DateTime start = getStart();
    DateTime end = getEnd(start, Data.getString(VIEW_TASKS, activeRow, COL_EXPECTED_DURATION));

    boolean noExecutors = BeeUtils.allEmpty(activeRow.getProperty(PROP_EXECUTORS),
        activeRow.getProperty(PROP_EXECUTOR_GROUPS));

    if (start == null && start == end && noExecutors) {
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
    if (start == null && start == end && noExecutors) {
      updateRow(event, null, null, null, null);
    } else {
      createTasks(activeRow, event.getCallback(), event);
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    Long owner = row.getLong(form.getDataIndex(COL_OWNER));

    form.setEnabled(Objects.equals(owner, BeeKeeper.getUser().getUserId()));

    Widget fileWidget = form.getWidgetByName(PROP_FILES);
    if (fileWidget instanceof FileGroup) {
      ((FileGroup) fileWidget).clear();
    }

    List<FileInfo> files = TaskUtils.getFiles(row);
    if (!files.isEmpty()) {
      if (fileWidget instanceof FileGroup) {
        for (FileInfo file : files) {
          if (file.getRelatedId() == null) {
            ((FileGroup) fileWidget).addFile(file);
          }
        }
      }
    }

    return true;
  }

  private static MultiSelector getMultiSelector(FormView form, String source) {
    Widget widget = form.getWidgetBySource(source);
    return (widget instanceof MultiSelector) ? (MultiSelector) widget : null;
  }

  private static BeeRow getResponseRow(String caption, ResponseObject ro, RowCallback callback) {
    if (!Queries.checkResponse(caption, BeeConst.UNDEF, VIEW_TASKS, ro, BeeRow.class, null)) {
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

  private static void setProjectStagesFilter(FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);
    int idxProjectUser = form.getDataIndex(ProjectConstants.ALS_FILTERED_PROJECT_USER);

    if (BeeConst.isUndef(idxProjectOwner) || BeeConst.isUndef(idxProject) || BeeConst.isUndef(
        idxProjectUser)) {
      return;
    }

    Widget wProjectStage = form.getWidgetBySource(ProjectConstants.COL_PROJECT_STAGE);

    if (wProjectStage instanceof DataSelector) {
      ((DataSelector) wProjectStage).setEnabled(false);
    } else {
      return;
    }

    long projectId = BeeUtils.unbox(row.getLong(idxProject));

    if (DataUtils.isId(projectId)) {
      setVisibleProjectData(form, true);
    } else {
      setVisibleProjectData(form, false);
    }

    if (!ProjectsHelper.isProjectOwner(form, row) || !ProjectsHelper.isProjectUser(form, row)) {
      return;
    }

    ((DataSelector) wProjectStage).getOracle().setAdditionalFilter(
        Filter.equals(ProjectConstants.COL_PROJECT, projectId), true);
    ((DataSelector) wProjectStage).setEnabled(true);

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

  private static void validateReminderTime(DateTime reminderTime, DateTime end,
      RowCallback callback) {
    if (reminderTime != null) {
      DateTime now = TimeUtils.nowMinutes();
      if (TimeUtils.isLess(reminderTime, now)) {
        callback.onFailure(BeeUtils.joinWords(
            Localized.dictionary().crmReminderTimeMustBeGreaterThan(), now));
        return;
      }

      if (TimeUtils.isMeq(reminderTime, end)) {
        callback.onFailure(BeeUtils.joinWords(
            Localized.dictionary().crmReminderTimeMustBeLessThan(), end));
        return;
      }
    }
  }

  private void clearValue(String widgetName) {
    Widget widget = getFormView().getWidgetByName(widgetName);
    if (widget instanceof Editor) {
      ((Editor) widget).clearValue();
    }
  }

  private BeeRow createNewRow(IsRow activeRow, DateTime start, DateTime end, RowCallback callback) {
    String summary = activeRow.getString(Data.getColumnIndex(VIEW_TASKS, COL_SUMMARY)).trim();
    BeeRow newRow = DataUtils.cloneRow(activeRow);

    if (start != null) {
      Data.setValue(VIEW_TASKS, newRow, COL_START_TIME, start);
    }

    if (end != null) {
      Data.setValue(VIEW_TASKS, newRow, COL_FINISH_TIME, end);
    }
    Data.setValue(VIEW_TASKS, newRow, COL_SUMMARY, summary);

    DateTime reminderTime = getReminderTime(end);
    validateReminderTime(reminderTime, end, callback);

    if (reminderTime != null) {
      Data.setValue(VIEW_TASKS, newRow, COL_REMINDER_TIME, reminderTime);
    }

    if (mailToggle != null && mailToggle.isChecked()) {
      newRow.setProperty(PROP_MAIL, BooleanValue.S_TRUE);
    }

    return newRow;
  }

  private void createFiles(final List<Long> tasks) {
    Widget widget = getFormView().getWidgetByName(NAME_FILES);

    if (widget instanceof FileCollector && !((FileCollector) widget).isEmpty()) {
      List<FileInfo> files = Lists.newArrayList(((FileCollector) widget).getFiles());

      final List<BeeColumn> columns = Data.getColumns(VIEW_TASK_FILES,
          Lists.newArrayList(COL_TASK, AdministrationConstants.COL_FILE, COL_CAPTION));

      for (final FileInfo fileInfo : files) {
        FileUtils.uploadFile(fileInfo, new Callback<Long>() {
          @Override
          public void onSuccess(Long result) {
            for (long taskId : tasks) {
              List<String> values = Lists.newArrayList(BeeUtils.toString(taskId),
                  BeeUtils.toString(result), fileInfo.getCaption());
              Queries.insert(VIEW_TASK_FILES, columns, values);
            }
          }
        });
      }

      ((FileCollector) widget).clear();
    }
  }

  private void createNotScheduledTask(RowCallback callback) {
    IsRow activeRow = getFormView().getActiveRow();

    BeeRow newRow = createNewRow(activeRow, null, null, callback);
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

  private ParameterList createParams(TaskEvent event, BeeRow newRow, String comment) {
    FormView form = getFormView();
    String viewName = form.getViewName();

    IsRow oldRow = form.getOldRow();

    BeeRowSet updated = DataUtils.getUpdated(viewName, form.getDataColumns(), oldRow, newRow,
        form.getChildrenForUpdate());

    if (!DataUtils.isEmpty(updated)) {
      BeeRow updRow = updated.getRow(0);

      for (int i = 0; i < updated.getNumberOfColumns(); i++) {
        int index = form.getDataIndex(updated.getColumnId(i));

        newRow.setValue(index, oldRow.getString(index));
        newRow.preliminaryUpdate(index, updRow.getString(i));
      }
    }

    BeeRowSet rowSet = new BeeRowSet(viewName, form.getDataColumns());
    rowSet.addRow(newRow);

    ParameterList params = TasksKeeper.createTaskRequestParameters(event);
    params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));
    params.addDataItem(VAR_TASK_USERS, DataUtils.buildIdList(TaskUtils.getTaskUsers(oldRow, form
        .getDataColumns())));

    if (!BeeUtils.isEmpty(comment)) {
      params.addDataItem(VAR_TASK_COMMENT, comment);
    }

    List<String> notes = TaskUtils.getUpdateNotes(Data.getDataInfo(viewName), oldRow, newRow);

    if (form.isEnabled() || !TaskUtils.getUpdatedRelations(oldRow, newRow).isEmpty()) {
      if (!TaskUtils.sameObservers(oldRow, newRow)) {
        String oldObservers = oldRow.getProperty(PROP_OBSERVERS);
        String newObservers = newRow.getProperty(PROP_OBSERVERS);

        MultiSelector selector = getMultiSelector(form, PROP_OBSERVERS);

        Set<Long> removed = DataUtils.getIdSetDifference(oldObservers, newObservers);
        for (long id : removed) {
          String label = selector.getRowLabel(id);
          if (!BeeUtils.isEmpty(label)) {
            notes.add(TaskUtils.getDeleteNote(Localized.dictionary().crmTaskObservers(), label));
          }
        }

        Set<Long> added = DataUtils.getIdSetDifference(newObservers, oldObservers);
        for (long id : added) {
          String label = selector.getRowLabel(id);
          if (!BeeUtils.isEmpty(label)) {
            notes.add(TaskUtils.getInsertNote(Localized.dictionary().crmTaskObservers(), label));
          }
        }
      }

      List<String> updatedRelations = TaskUtils.getUpdatedRelations(oldRow, newRow);
      if (!updatedRelations.isEmpty()) {
        params.addDataItem(VAR_TASK_RELATIONS, NameUtils.join(updatedRelations));

        for (String relation : updatedRelations) {
          String caption = Data.getViewCaption(relation);
          MultiSelector selector = getMultiSelector(form, relation);
          if (selector == null) {
            continue;
          }

          String oldValue = oldRow.getProperty(relation);
          String newValue = newRow.getProperty(relation);

          Set<Long> removed = DataUtils.getIdSetDifference(oldValue, newValue);
          for (long id : removed) {
            String label = selector.getRowLabel(id);
            if (!BeeUtils.isEmpty(label)) {
              notes.add(TaskUtils.getDeleteNote(caption, label));
            }
          }

          Set<Long> added = DataUtils.getIdSetDifference(newValue, oldValue);
          for (long id : added) {
            String label = selector.getRowLabel(id);
            if (!BeeUtils.isEmpty(label)) {
              notes.add(TaskUtils.getInsertNote(caption, label));
            }
          }
        }
      }
    }

    if (!notes.isEmpty()) {
      params.addDataItem(VAR_TASK_NOTES, Codec.beeSerialize(notes));
    }

    return params;
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
          widget.setValue(startTime);
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

    BeeRow newRow = createNewRow(activeRow, start, end, callback);
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

  private DateTime getReminderTime(DateTime end) {
    HasDateValue datePart = getDate(NAME_REMINDER_DATE);
    Long timePart = getMillis(NAME_REMINDER_TIME);

    if (datePart == null && timePart == null) {
      return null;
    } else if (datePart == null) {
      return TimeUtils.combine(end, timePart);
    } else {
      return TimeUtils.combine(datePart, timePart);
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

      clearValue(NAME_START_DATE);
      clearValue(NAME_START_TIME);
      clearValue(NAME_END_DATE);
      clearValue(NAME_END_TIME);
      clearValue(NAME_REMINDER_DATE);
      clearValue(NAME_REMINDER_TIME);

      createFiles(tasks.getRowIds());

      if (!taskIdsCallback) {
        callback.onSuccess(null);
      } else {
        BeeRow row = new BeeRow(0, new String[] {DataUtils.buildIdList(tasks)});
        callback.onSuccess(row);
      }

      String message = Localized.dictionary().crmCreatedNewTasks(tasks.getNumberOfRows());
      BeeKeeper.getScreen().notifyInfo(message);

      if (!taskIdsCallback) {
        callback.onSuccess(tasks.getRow(0));
      }

      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TASKS);

    } else {
      callback.onFailure("Unknown response");
    }
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

    if (executors != null) {
      executors.setEnabled(false);
    }

    if (executorGroups != null) {
      executorGroups.setEnabled(false);
      executorGroups.getElement().addClassName(StyleUtils.NAME_DISABLED);
    }

    if (observers != null) {
      observers.setEnabled(false);
    }

    if (observerGroups != null) {
      observerGroups.setEnabled(false);
      observerGroups.getElement().addClassName(StyleUtils.NAME_DISABLED);
    }

    Queries.getRowSet(AdministrationConstants.VIEW_USERS, null, new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          if (executors != null) {
            executors.getOracle().setAdditionalFilter(Filter.compareId(projectOwner), true);
            executors.setEnabled(true);
            return;
          }
        }

        List<Long> userIds = Lists.newArrayList(projectOwner);

        for (IsRow userRow : result) {
          long projectUser = BeeUtils.unbox(userRow.getId());

          if (DataUtils.isId(projectUser)) {
            userIds.add(projectUser);
          }
        }

        if (executors != null) {
          executors.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          executors.setEnabled(true);
        }

        if (observers != null) {
          observers.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          observers.setEnabled(true);
        }
      }
    });
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

  private void updateRow(final SaveChangesEvent event, Long newExecutor, DateTime start,
      DateTime end, Callback<BeeRow> callback) {

    final IsRow oldRow = event.getOldRow();
    IsRow newRow = event.getNewRow();

    if (oldRow == null || newRow == null) {
      return;
    }

    if (event.isEmpty() && TaskUtils.sameObservers(oldRow, newRow)
        && TaskUtils.getUpdatedRelations(oldRow, newRow).isEmpty()
        && !DataUtils.isId(newExecutor)) {
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

    ParameterList params = createParams(TaskEvent.EDIT, updateRow, null);
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        BeeRow data = getResponseRow(TaskEvent.EDIT.getCaption(), response, event.getCallback());

        if (data != null) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);
          if (callback != null) {
            callback.onSuccess(data);
          }

          if (TaskUtils.hasRelations(oldRow) || TaskUtils.hasRelations(data)) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
          }
        }
      }
    });

    event.consume();
    event.getCallback().onCancel();
  }

}
