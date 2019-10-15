package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.client.composite.Relations.PFX_RELATED;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.FileGroup.Column;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dialog.ReminderDialog;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.CellKind;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.grid.TableKind;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.composite.Relations;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.AbstractSlackRenderer;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class TaskEditor extends ProductSupportInterceptor {

  private static final String STYLE_EVENT = CRM_STYLE_PREFIX + "taskEvent-";
  private static final String STYLE_EVENT_ROW = STYLE_EVENT + "row";
  private static final String STYLE_EVENT_ROW_NEW = STYLE_EVENT_ROW + "-new";
  private static final String STYLE_EVENT_COL = STYLE_EVENT + "col-";
  private static final String STYLE_EVENT_COL_ROW = STYLE_EVENT_COL + "row";

  private static final String STYLE_EVENT_FLEX = STYLE_EVENT + "flex";
  private static final String STYLE_EVENT_CREATE_TASK = STYLE_EVENT + "createTask";
  private static final String STYLE_EVENT_FILES = STYLE_EVENT + "files";
  private static final String STYLE_DURATION = CRM_STYLE_PREFIX + "taskDuration-";
  private static final String STYLE_DURATION_CELL = "Cell";

  private static final String STYLE_PHOTO = "Photo";
  private static final String STYLE_TASK = CRM_STYLE_PREFIX + "task-";
  private static final String STYLE_TASK_LATE_KIND = STYLE_TASK + "lateKind-";
  private static final String STYLE_EXTENSION = CRM_STYLE_PREFIX + "taskExtension";
  private static final String NAME_OBSERVERS = "Observers";

  private static final String NAME_PRIVATE_TASK = "PrivateTask";
  private static final String NAME_LATE_INDICATOR = "LateIndicator";

  private final long userId;

  private IdentifiableWidget taskWidget;
  private IdentifiableWidget taskEventsWidget;

  private Split split;

  private MultiSelector observersSelector;
  private DataSelector ownerSelector;
  private DataSelector stagesSelector;

  private TextLabel lateIndicator;
  private Relations relations;

  private List<Long> projectUsers;
  private Map<String, Pair<Long, String>> dbaParameters = Maps.newConcurrentMap();
  private Long prmEndOfWorkDay;

  private boolean isDefaultLayout;

  private static void addDurationCell(HtmlTable display, int row, int col, String value,
      String style) {
    Widget widget = new CustomDiv(STYLE_DURATION + style);
    if (!BeeUtils.isEmpty(value)) {
      widget.getElement().setInnerText(value);
    }

    display.setWidget(row, col, widget, STYLE_DURATION + style + STYLE_DURATION_CELL);
  }

  private static Widget createEventCell(String colName, String value) {
    return createEventCell(colName, value, false);
  }

  private static Widget createEventCell(String colName, String value, boolean serializable) {
    Widget widget = new Flow(STYLE_EVENT + colName);
    if (!BeeUtils.isEmpty(value) && !serializable) {
      widget.getElement().setInnerText(value);
    } else if (!BeeUtils.isEmpty(value) && serializable) {
      Map<String, String> data = Codec.deserializeLinkedHashMap(value);

      if (data == null) {
        return widget;
      }

      if (!data.containsKey(BeeUtils.toString(TaskEvent.CREATE.ordinal()))) {
        return widget;
      }

      List<Long> extTasks =
          DataUtils.parseIdList(data.get(BeeUtils.toString(TaskEvent.CREATE.ordinal())));

      widget.getElement().setInnerHTML(Localized.dictionary().crmTasksDelegatedTasks());

      for (final Long extTaskId : extTasks) {
        InternalLink url = new InternalLink(BeeUtils.toString(extTaskId));
        url.addClickHandler(arg0 -> RowEditor.open(VIEW_TASKS, extTaskId, Opener.NEW_TAB));

        ((Flow) widget).add(url);
      }

    }
    return widget;
  }

  private static List<FileInfo> filterEventFiles(List<FileInfo> input, long teId) {
    if (input.isEmpty()) {
      return input;
    }
    List<FileInfo> result = new ArrayList<>();

    for (FileInfo file : input) {
      Long id = file.getRelatedId();
      if (id != null && id == teId) {
        result.add(file);
      }
    }
    return result;
  }

  private static BeeRow getResponseRow(String caption, ResponseObject ro, RpcCallback<?> callback) {
    if (!Queries.checkResponse(caption, BeeConst.UNDEF, VIEW_TASKS, ro, BeeRow.class, callback)) {
      return null;
    }

    BeeRow row = BeeRow.restore((String) ro.getResponse());
    if (row == null && callback != null) {
      callback.onFailure(caption, VIEW_TASKS, "cannot restore row");
    }
    return row;
  }

  private static String renderDuration(long millis) {
    return TimeUtils.renderTime(millis, false);
  }

  private static void sendRequest(ParameterList params,
      final RpcCallback<ResponseObject> callback) {

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          if (callback != null) {
            callback.onFailure(response.getErrors());
          }
        } else {
          if (callback != null) {
            callback.onSuccess(response);

            if (response.hasResponse(BeeRow.class)) {
              BeeRow row = BeeRow.restore((String) response.getResponse());

              if (row != null) {
                RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, row);
              }
            }
          }
        }
      }
    });
  }

  private static boolean setDurationParams(TaskDialog dialog, Map<String, String> ids,
      ParameterList params) {
    String time = dialog.getTime(ids.get(COL_DURATION));

    if (!BeeUtils.isEmpty(time)) {
      BeeRow durType = dialog.getSelector(ids.get(COL_DURATION_TYPE)).getRelatedRow();
      Long type = durType.getLong(Data.getColumnIndex(VIEW_TASK_DURATION_TYPES, COL_DURATION_TYPE));
      if (!DataUtils.isId(type)) {
        showError(Localized.dictionary().crmEnterDurationType());
        return false;
      }

      DateTime date = dialog.getDateTime(ids.get(COL_DURATION_DATE));
      if (date == null) {
        date = TimeUtils.nowMinutes();
      }

      params.addDataItem(VAR_TASK_DURATION_DATE, date.serialize());
      params.addDataItem(VAR_TASK_DURATION_TIME, time);
      params.addDataItem(VAR_TASK_DURATION_TYPE, type);
    }
    return true;
  }

  private static void showDurations(FormView form, Table<String, String, Long> durations) {
    Widget widget = form.getWidgetByName(VIEW_TASK_DURATIONS);
    if (!(widget instanceof Flow)) {
      return;
    }

    Flow panel = (Flow) widget;
    panel.clear();

    if (durations.isEmpty()) {
      return;
    }

    Set<String> rows = durations.rowKeySet();
    Set<String> columns = durations.columnKeySet();

    HtmlTable display = new HtmlTable();
    display.addStyleName(STYLE_DURATION + "display");

    int r = 0;
    int c = 0;

    addDurationCell(display, r, c++, Localized.dictionary().crmSpentTime(), "caption");
    for (String column : columns) {
      addDurationCell(display, r, c++, column, "colLabel");
    }
    r++;

    long totMillis = 0;
    for (String row : rows) {
      c = 0;
      addDurationCell(display, r, c++, row, "rowLabel");

      long rowMillis = 0;
      for (String column : columns) {
        Long millis = durations.get(row, column);

        if (BeeUtils.isPositive(millis)) {
          addDurationCell(display, r, c, renderDuration(millis), "value");

          rowMillis += millis;
          totMillis += millis;
        }
        c++;
      }

      if (columns.size() > 1) {
        addDurationCell(display, r, c, renderDuration(rowMillis), "rowTotal");
      }
      r++;
    }

    if (rows.size() > 1) {
      c = 1;

      for (String column : columns) {
        Collection<Long> values = durations.column(column).values();

        long colMillis = 0;
        for (Long value : values) {
          colMillis += BeeUtils.unbox(value);
        }
        addDurationCell(display, r, c++, renderDuration(colMillis), "rowTotal");
      }

      if (columns.size() > 1) {
        addDurationCell(display, r, c, renderDuration(totMillis), "colTotal");
      }
    }

    panel.add(display);
  }

  private static void showError(String message) {
    Global.showError(Localized.dictionary().error(), Collections.singletonList(message));
  }

  private void showEvent(Flow panel, final BeeRow row, List<BeeColumn> columns,
      List<FileInfo> files, Table<String, String, Long> durations, Long lastAccess,
      final IsRow taskRow) {

    Flow container = new Flow();
    container.addStyleName(STYLE_EVENT_ROW);

    Flow body = new Flow();

    Flow colPhoto = new Flow();
    colPhoto.addStyleName(STYLE_EVENT_COL + STYLE_PHOTO);

    Image image = new Image(PhotoRenderer.getPhotoUrl(DataUtils.getString(columns, row,
        COL_PHOTO)));
    image.addStyleName(STYLE_EVENT + STYLE_PHOTO);

    colPhoto.add(image);
    container.add(colPhoto);

    Flow row1 = new Flow();
    row1.addStyleName(STYLE_EVENT_COL_ROW);
    row1.addStyleName(STYLE_EVENT_FLEX);

    String publisher = BeeUtils.joinWords(
        row.getString(DataUtils.getColumnIndex(ALS_PUBLISHER_FIRST_NAME, columns)),
        row.getString(DataUtils.getColumnIndex(ALS_PUBLISHER_LAST_NAME, columns)));
    if (!BeeUtils.isEmpty(publisher)) {
      row1.add(createEventCell(COL_PUBLISHER, publisher));
    }

    DateTime publishTime = row.getDateTime(DataUtils.getColumnIndex(COL_PUBLISH_TIME, columns));
    if (publishTime != null) {
      row1.add(createEventCell(COL_PUBLISH_TIME, Format.renderDateTime(publishTime)));
    }

    if (lastAccess != null && publishTime != null) {
      if (BeeUtils.unbox(lastAccess) < publishTime.getTime()) {
        container.addStyleName(STYLE_EVENT_ROW_NEW);
      } else {
        container.removeStyleName(STYLE_EVENT_ROW_NEW);
      }
    } else {
      container.removeStyleName(STYLE_EVENT_ROW_NEW);
    }

    Integer ev = row.getInteger(DataUtils.getColumnIndex(TaskConstants.COL_EVENT, columns));
    TaskEvent event = EnumUtils.getEnumByIndex(TaskEvent.class, ev);
    if (event != null) {
      row1.add(createEventCell(TaskConstants.COL_EVENT, event.getCaption()));
    }

    body.add(row1);

    Flow row2 = new Flow();
    row2.addStyleName(STYLE_EVENT_COL_ROW);

    String note = row.getString(DataUtils.getColumnIndex(COL_EVENT_NOTE, columns));
    if (!BeeUtils.isEmpty(note)) {
      row2.add(createEventCell(COL_EVENT_NOTE, note));
    }

    String eventData = row.getString(DataUtils.getColumnIndex(COL_EVENT_DATA, columns));

    if (!BeeUtils.isEmpty(eventData)) {
      row2.add(createEventCell(COL_EVENT_NOTE, eventData, true));
    }

    String comment = row.getString(DataUtils.getColumnIndex(COL_COMMENT, columns));
    if (!BeeUtils.isEmpty(comment)) {
      row2.add(createEventCell(COL_COMMENT, comment));
      int idxOwner = Data.getColumnIndex(VIEW_TASKS, COL_OWNER);
      int idxExecutor = Data.getColumnIndex(VIEW_TASKS, COL_EXECUTOR);
      if (event == TaskEvent.COMMENT
          && (Objects.equals(taskRow.getLong(idxOwner), userId) || Objects.equals(taskRow
          .getLong(idxExecutor), userId))) {
        FaLabel createTask = new FaLabel(TaskEvent.CREATE.getCommandIcon());
        createTask.addStyleName(STYLE_EVENT_CREATE_TASK);
        createTask.setTitle(TaskEvent.CREATE.getCommandLabel());
        createTask.addClickHandler(arg0 -> doCreate(row.getId(), taskRow));

        row1.add(createTask);
      }
    }

    body.add(row2);

    if (!files.isEmpty()) {
      Flow row3 = new Flow();
      row3.addStyleName(STYLE_EVENT_COL_ROW);

      Simple fileContainer = new Simple();
      fileContainer.addStyleName(STYLE_EVENT_FILES);

      FileGroup fileGroup = new FileGroup(Lists.newArrayList(Column.ICON, Column.NAME, Column.SIZE,
          Column.CREATEDOC));
      fileGroup.addFiles(files);

      fileGroup.setDocCreator(fileInfo -> createDocument(fileInfo, taskRow));
      fileContainer.setWidget(fileGroup);
      row3.add(fileContainer);
      body.add(row3);
    }

    String duration = row.getString(DataUtils.getColumnIndex(COL_DURATION, columns));
    if (!BeeUtils.isEmpty(duration)) {

      Flow row4 = new Flow();
      row4.addStyleName(STYLE_EVENT_COL_ROW);
      row4.addStyleName(STYLE_EVENT_FLEX);

      row4.add(createEventCell(COL_DURATION, BeeUtils.joinWords(Localized.dictionary()
          .crmSpentTime(), duration)));

      String durType = row.getString(DataUtils.getColumnIndex(COL_DURATION_TYPE, columns));
      if (!BeeUtils.isEmpty(durType)) {
        row4.add(createEventCell(COL_DURATION_TYPE, durType));
      }

      DateTime durDate = row.getDateTime(DataUtils.getColumnIndex(COL_DURATION_DATE, columns));
      if (durDate != null) {
        row4.add(createEventCell(COL_DURATION_DATE, Format.renderDateTime(durDate)));
      }

      body.add(row4);

      Long millis = TimeUtils.parseTime(duration);
      if (BeeUtils.isPositive(millis)
          && !BeeUtils.isEmpty(publisher) && !BeeUtils.isEmpty(durType)) {
        Long value =
            durations.get(publisher, durType);
        durations.put(publisher, durType, millis
            + BeeUtils.unbox(value));
      }
    }
    container.add(body);
    panel.add(container);
  }

  private void showEventsAndDuration(FormView form, IsRow taskRow, BeeRowSet rowSet,
      List<FileInfo> files, Long lastAccess) {

    Widget widget = form.getWidgetByName(VIEW_TASK_EVENTS);
    if (!(widget instanceof Flow) || DataUtils.isEmpty(rowSet)) {
      return;
    }

    Flow panel = (Flow) widget;
    panel.clear();

    Table<String, String, Long> durations = TreeBasedTable.create();

    for (BeeRow row : rowSet.getRows()) {
      showEvent(panel, row, rowSet.getColumns(), filterEventFiles(files, row.getId()), durations,
          lastAccess, taskRow);
    }

    showExtensions(form, rowSet);
    showDurations(form, durations);

    if (panel.getWidgetCount() > 1 && DomUtils.isVisible(form.getElement())
        && !TaskHelper.getBooleanValueFromStorage(TaskHelper.NAME_ORDER)) {
      final Widget last = panel.getWidget(panel.getWidgetCount() - 1);
      Scheduler.get().scheduleDeferred(() -> DomUtils.scrollIntoView(last.getElement()));
    }
  }

  private static void showExtensions(FormView form, BeeRowSet rowSet) {
    if (DataUtils.isEmpty(rowSet)) {
      return;
    }
    int index = rowSet.getColumnIndex(COL_FINISH_TIME);
    if (BeeConst.isUndef(index)) {
      return;
    }

    Widget widget = form.getWidgetByName("TaskExtensions");
    if (!(widget instanceof HasWidgets)) {
      return;
    }

    HasWidgets panel = (HasWidgets) widget;
    panel.clear();

    List<DateTime> extensions = new ArrayList<>();

    for (BeeRow row : rowSet.getRows()) {
      DateTime dt = row.getDateTime(index);
      if (dt != null) {
        extensions.add(dt);
      }
    }

    if (!extensions.isEmpty()) {
      for (int i = extensions.size() - 1; i >= 0; i--) {
        Label label = new Label(Format.renderDateTime(extensions.get(i)));
        label.addStyleName(STYLE_EXTENSION);
        panel.add(label);
      }
    }
  }

  TaskEditor() {
    super();
    this.isDefaultLayout = BeeKeeper.getUser().getCommentsLayout();
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    super.afterCreateEditableWidget(editableWidget, widget);

    if (BeeUtils.same(editableWidget.getColumnId(), COL_OWNER) && widget instanceof DataSelector) {
      ownerSelector = (DataSelector) widget;
      ownerSelector.addSelectorHandler(this::changeTaskOwner);
    } else if (BeeUtils.same(editableWidget.getColumnId(), ProjectConstants.COL_PROJECT_STAGE)
        && widget instanceof DataSelector) {
      stagesSelector = (DataSelector) widget;
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, NAME_OBSERVERS) && widget instanceof MultiSelector) {
      observersSelector = (MultiSelector) widget;
    } else if (BeeUtils.same(name, "TabbedPages") && widget instanceof TabbedPages) {
      taskWidget = widget;

    } else if (BeeUtils.same(name, "TaskEvents") && widget instanceof Flow) {
      taskEventsWidget = widget;

    } else if (BeeUtils.same(name, "Split") && widget instanceof Split) {
      split = (Split) widget;
      split.addMutationHandler(event -> {
        int size = split.getDirectionSize(Direction.WEST);

        String key = TaskHelper.getStorageKey(TaskHelper.NAME_TASK_TREE);
        if (size > 0 && !BeeUtils.isEmpty(key)) {
          BeeKeeper.getStorage().set(key, size);
        }
      });
    } else if (BeeUtils.same(name, NAME_LATE_INDICATOR) && widget instanceof TextLabel) {
      lateIndicator = (TextLabel) widget;
    }

    if (BeeUtils.same(name,
        AdministrationConstants.TBL_RELATIONS) && widget instanceof Relations) {
      relations = (Relations) widget;
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    Widget area = form.getWidgetBySource(COL_DESCRIPTION);

    if (area != null && area instanceof InputArea) {
      StyleUtils.setHeight(area, 60);
      int scroll = area.getElement().getScrollHeight();

      if (scroll > 60) {
        StyleUtils.setHeight(area, scroll + 2);
      }
    }

    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (row == null) {
      return;
    }

    setCommentsLayout();

    Integer status = row.getInteger(form.getDataIndex(COL_STATUS));

    if ((isExecutor() || isOwner())
        && !Objects.equals(TaskStatus.NOT_SCHEDULED.ordinal(), getStatus())
        && !Objects.equals(TaskStatus.APPROVED.ordinal(), getStatus())) {

      final ReminderDialog reminderDialog = new ReminderDialog(Module.TASKS, getActiveRow().getId(),
          userId);

      reminderDialog.getReminderLabel().addClickHandler(event -> {

        Map<Integer, DateTime> datesByField = new HashMap<>();
        datesByField.put(ReminderDateField.START_DATE.ordinal(), getDateTime(COL_START_TIME));
        datesByField.put(ReminderDateField.END_DATE.ordinal(), getDateTime(COL_FINISH_TIME));

        reminderDialog.showDialog(datesByField);
      });
      header.addCommandItem(reminderDialog.getReminderLabel());
    }

    final FaLabel createDocument = new FaLabel(FontAwesome.FILE_O);
    createDocument.setTitle(Localized.dictionary().documentNew());
    createDocument.addClickHandler(event -> {
      TaskHelper.setWidgetEnabled(createDocument, false);
      ensureDefaultDBAParameters(createDocument, row);
    });

    if (BeeKeeper.getUser().canCreateData(DocumentConstants.VIEW_DOCUMENTS)) {
      header.addCommandItem(createDocument);
    }

    for (final TaskEvent event : TaskEvent.values()) {

      if (!event.canExecute(getExecutor(), getOwner(), getObserversSelector(),
          EnumUtils.getEnumByIndex(TaskStatus.class, status))) {
        continue;
      }
      String label = event.getCommandLabel();
      FontAwesome icon = event.getCommandIcon();

      IdentifiableWidget button = icon != null ? new FaLabel(icon) : new Button(label);

      ((HasClickHandlers) button).addClickHandler(e -> doEvent(event));

      if (button instanceof FaLabel) {
        ((FaLabel) button).setTitle(label);
      }

      if (!BeeUtils.isEmpty(label)) {
        header.addCommandItem(button);
      }
    }

    setProjectStagesFilter(form, row);
    setProjectUsersFilter(form, row);

    TaskHelper.setWidgetEnabled(relations, isExecutor() || isOwner());

    header.addCommandItem(createMenuLabel());

    TaskSlackRenderer renderer = new TaskSlackRenderer(form.getDataColumns());
    Pair<AbstractSlackRenderer.SlackKind, Long> data = renderer.getMinutes(row);
    setLateIndicatorHtml(data);
    setTaskStatusStyle(header, form, row, data);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.SAVE) {
      return !maybeNotifyEmptyProduct(msg -> getFormView().notifySevere(msg));
    }

    return super.beforeAction(action, presenter);
  }

  @Override
  public boolean beforeCreateWidget(String name, com.google.gwt.xml.client.Element description) {

    if (BeeUtils.same(name, "Split")) {
      Integer size = BeeKeeper.getStorage().getInteger(
          TaskHelper.getStorageKey(TaskHelper.NAME_TASK_TREE));

      if (BeeUtils.isPositive(size)) {

        com.google.gwt.xml.client.Element west = XmlUtils.getFirstChildElement(description,
            Direction.WEST.name().toLowerCase());

        if (west != null) {
          west.setAttribute(UiConstants.ATTR_SIZE, size.toString());
        }
      }
    }
    return super.beforeCreateWidget(name, description);
  }

  private static void setTaskStatusStyle(HeaderView header, FormView form, IsRow row,
      Pair<AbstractSlackRenderer.SlackKind, Long> slackData) {
    for (TaskStatus taskStatusStyle : TaskStatus.values()) {
      if (taskStatusStyle.getStyleName(true) != null) {
        header.removeStyleName(taskStatusStyle.getStyleName(true));
      }
      if (taskStatusStyle.getStyleName(false) != null) {
        header.removeStyleName(taskStatusStyle.getStyleName(false));
      }
    }

    TaskStatus taskStatus = EnumUtils.getEnumByIndex(TaskStatus.class,
        row.getInteger(form.getDataIndex(COL_STATUS)));
    String styleName;
    if (slackData != null && slackData.getA() != null) {
      styleName = taskStatus.getStyleName(slackData.getA().equals(
          AbstractSlackRenderer.SlackKind.LATE));
    } else {
      styleName = taskStatus.getStyleName(false);
    }

    if (!BeeUtils.isEmpty(styleName)) {
      header.addStyleName(TASK_STATUS_STYLE);
      header.addStyleName(styleName);
    }

  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    TaskHelper.setWidgetEnabled(relations, isExecutor() || isOwner());
    setLateIndicatorHtml(null);
    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TaskEditor();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return row != null
        && (BeeKeeper.getUser().is(row.getLong(getDataIndex(COL_OWNER)))
        || BeeKeeper.getUser().is(row.getLong(getDataIndex(COL_EXECUTOR)))
        || BeeKeeper.getUser().is(row.getLong(getDataIndex(ALS_PROJECT_OWNER)))
        || BeeKeeper.getUser().isAdministrator());
  }

  @Override
  public void onSetActiveRow(IsRow row) {

    boolean privateTask =
        BeeUtils.unbox(row.getBoolean(Data.getColumnIndex(TaskConstants.VIEW_TASKS,
            COL_PRIVATE_TASK)));

    if (privateTask) {
      Filter filter =
          Filter.and(Filter.notNull(COL_PRIVATE_TASK), Filter.or(Filter.equals(COL_OWNER,
              userId), Filter.equals(COL_EXECUTOR, userId), Filter.in("TaskID",
              VIEW_TASK_USERS, COL_TASK, Filter.equals(AdministrationConstants.COL_USER,
                  userId))));
      Queries.getRowSet(TaskConstants.VIEW_TASKS, null, filter, new RowSetCallback() {

        @Override
        public void onSuccess(BeeRowSet result) {
          if (!result.getRowIds().contains(row.getId())) {
            getFormView().getViewPresenter().handleAction(Action.CLOSE);
            getFormView().notifySevere(Localized.dictionary().crmTaskPrivate());
          }
        }
      });
    } else {
      super.onSetActiveRow(row);
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    final IsRow oldRow = event.getOldRow();
    IsRow newRow = event.getNewRow();

    if (oldRow == null || newRow == null) {
      return;
    }

    if (event.isEmpty() && TaskUtils.sameObservers(oldRow, newRow)
        && (relations != null && BeeUtils.isEmpty(relations.getChildrenForUpdate()))) {
      return;
    }

    ParameterList params = createParams(TaskEvent.EDIT, null);

    sendRequest(params, new RpcCallback<ResponseObject>() {
      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(TaskEvent.EDIT.getCaption(), result, this);
        if (data != null) {
          TasksKeeper.fireRelatedDataRefresh(data, relations);
        }
      }
    });

    event.consume();
    event.getCallback().onCancel();
  }

  @Override
  public boolean onStartEdit(final FormView form, final IsRow row, ScheduledCommand focusCommand) {

    final Long lastAccess = BeeUtils.toLongOrNull(row.getProperty(PROP_LAST_ACCESS,
        BeeKeeper.getUser().getUserId()));

    Long owner = row.getLong(form.getDataIndex(COL_OWNER));
    Long executor = row.getLong(form.getDataIndex(COL_EXECUTOR));

    createCellValidationHandler(form, row);

    TaskStatus oldStatus = EnumUtils.getEnumByIndex(TaskStatus.class,
        row.getInteger(form.getDataIndex(COL_STATUS)));

    TaskHelper.setWidgetEnabled(form, Objects.equals(owner, userId)
        || Objects.equals(executor, userId));

    TaskStatus newStatus = oldStatus;

    if (TaskStatus.NOT_VISITED.equals(oldStatus) && Objects.equals(executor, userId)) {
      newStatus = TaskStatus.VISITED;
    }

    BeeRow visitedRow = DataUtils.cloneRow(row);
    if (newStatus != null) {
      visitedRow.preliminaryUpdate(form.getDataIndex(COL_STATUS),
          BeeUtils.toString(newStatus.ordinal()));
    }

    if (TaskHelper.getBooleanValueFromStorage(TaskHelper.NAME_ORDER)) {
      visitedRow.setProperty(PROP_DESCENDING, BeeConst.INT_TRUE);
    } else {
      visitedRow.removeProperty(PROP_DESCENDING);
    }

    BeeRowSet rowSet = new BeeRowSet(form.getViewName(), form.getDataColumns());
    rowSet.addRow(visitedRow);

    ParameterList params = TasksKeeper.createTaskRequestParameters(TaskEvent.VISIT);

    if (newStatus == TaskStatus.VISITED && oldStatus != newStatus) {
      params.addQueryItem(VAR_TASK_VISITED, 1);
    }

    params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));

    sendRequest(params, new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        form.updateRow(row, true);
        form.notifySevere(reason);
        if (focusCommand != null) {
          focusCommand.execute();
        }
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(TaskEvent.VISIT.getCaption(), result, this);
        if (data == null) {
          return;
        }

        RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);
        if (relations != null && !BeeUtils.isEmpty(relations.getChildrenForUpdate())) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
        }

        if (relations != null) {
          relations.requery(data, data.getId());
        }

        Widget fileWidget = form.getWidgetByName(PROP_FILES);
        if (fileWidget instanceof FileGroup) {
          ((FileGroup) fileWidget).clear();
        }

        List<FileInfo> files = TaskUtils.getFiles(data);
        if (!files.isEmpty()) {
          if (fileWidget instanceof FileGroup) {
            for (FileInfo file : files) {
              if (file.getRelatedId() == null) {
                ((FileGroup) fileWidget).addFile(file);
              }
            }
            ((FileGroup) fileWidget).setDocCreator(fileInfo -> createDocument(fileInfo, row));
          }
        }

        if (!getEvents(data).isEmpty()) {
          showEventsAndDuration(form, data, getEvents(data), files, lastAccess);
        }

        form.updateRow(data, true);
        if (focusCommand != null) {
          focusCommand.execute();
        }
      }
    });
    setPrmEndOfWorkDay(Global.getParameterTime(PRM_END_OF_WORK_DAY));

    return false;
  }

  @Override
  public boolean showReadOnly(boolean readOnly) {
    return false;
  }

  public void createDocument(final FileInfo fileInfo, final IsRow row) {
    createDocument(fileInfo, row, false);
  }

  public void createDocument(final FileInfo fileInfo, final IsRow taskRow,
      final boolean enableTemplates) {
    final DataInfo documentViewInfo = Data.getDataInfo(DocumentConstants.VIEW_DOCUMENTS);
    final DataInfo taskViewInfo = Data.getDataInfo(VIEW_TASKS);
    final BeeRow docRow = RowFactory.createEmptyRow(documentViewInfo, true);
    final boolean ensureEnableTemplate = enableTemplates && dbaParameters != null;

    if (docRow != null) {
      int idxCompany = Data.getColumnIndex(VIEW_TASKS, COL_TASK_COMPANY);

      if (!BeeConst.isUndef(idxCompany)) {
        docRow.setValue(documentViewInfo
          .getColumnIndex(DocumentConstants.COL_DOCUMENT_COMPANY), taskRow
          .getLong(idxCompany));
        RelationUtils.updateRow(documentViewInfo, DocumentConstants.COL_DOCUMENT_COMPANY, docRow,
          taskViewInfo, taskRow, false);
      }
      docRow.setValue(documentViewInfo
        .getColumnIndex(DocumentConstants.COL_DOCUMENT_DATE), new DateTime(new JustDate()));
      FileCollector.pushFiles(Lists.newArrayList(fileInfo));

      if (ensureEnableTemplate && dbaParameters.containsKey(PRM_DEFAULT_DBA_DOCUMENT_TYPE)) {
        Pair<Long, String> defaultDBAType = dbaParameters.get(PRM_DEFAULT_DBA_DOCUMENT_TYPE);
        docRow.setValue(documentViewInfo.getColumnIndex(DocumentConstants.COL_DOCUMENT_TYPE),
          defaultDBAType.getA());
        docRow.setValue(documentViewInfo.getColumnIndex(DocumentConstants.ALS_TYPE_NAME),
          defaultDBAType.getB());
      }
      Set<Long> relDocTasks = new HashSet<>();

      if (docRow.hasPropertyValue(PFX_RELATED + VIEW_TASKS)) {
        relDocTasks = DataUtils.parseIdSet(docRow.getProperty(PFX_RELATED + VIEW_TASKS));
      }
      relDocTasks.add(taskRow.getId());
      docRow.setProperty(PFX_RELATED + VIEW_TASKS, DataUtils.buildIdList(relDocTasks));

      if (ensureEnableTemplate && dbaParameters.containsKey(PRM_DEFAULT_DBA_TEMPLATE)) {
        Pair<Long, String> defaultDBATemplate = dbaParameters.get(PRM_DEFAULT_DBA_TEMPLATE);
        docRow.setProperty(PRM_DEFAULT_DBA_TEMPLATE,
          BeeUtils.toString(defaultDBATemplate.getA()));
      }
      RowFactory.createRow(documentViewInfo, docRow, Modality.ENABLED, new RowCallback() {

        @Override
        public void onSuccess(final BeeRow createdDocument) {
          Collection<RowChildren> relatedRows = relations != null
            ? relations.getRowChildren(true) : new ArrayList<>();
          Collection<RowChildren> updatedRelations = relations != null
            ? relations.getChildrenForUpdate() : new ArrayList<>();
          RowChildren relDocuments = null;
          RowInsertEvent.fire(BeeKeeper.getBus(), DocumentConstants.VIEW_DOCUMENTS,
            createdDocument, null);

          for (RowChildren rel : relatedRows) {
            relDocuments = BeeUtils.same(rel.getChildColumn(), DocumentConstants.COL_DOCUMENT)
              ? rel : null;

            if (relDocuments != null) {
              break;
            }
          }
          if (relDocuments != null) {
            for (RowChildren rel : updatedRelations) {
              if (BeeUtils.same(rel.getChildColumn(), DocumentConstants.COL_DOCUMENT)) {
                updatedRelations.remove(rel);
                break;
              }
            }
          }
          Set<Long> relDocumentIds = relDocuments != null
            ? DataUtils.parseIdSet(relDocuments.getChildrenIds())
            : new HashSet<>();
          AbstractCellRenderer render =
            RendererFactory.createRenderer(DocumentConstants.VIEW_DOCUMENTS,
              Data.getRelation(DocumentConstants.VIEW_DOCUMENTS).getOriginalRenderColumns());
          relDocumentIds.add(createdDocument.getId());
          updatedRelations.add(RowChildren.create(TBL_RELATIONS, COL_TASK, null,
            DocumentConstants.COL_DOCUMENT, DataUtils.buildIdList(relDocumentIds)));

          ParameterList prm = createParams(TaskEvent.EDIT, getNewRow(), updatedRelations,
            TaskUtils.getInsertNote(Localized.dictionary().document(),
              render.render(createdDocument)));
          sendRequest(prm, TaskEvent.EDIT);
        }
      });
    }
  }

  @Override
  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    if (!Data.isNull(VIEW_TASKS, getActiveRow(), COL_TASK_ORDER)) {
      Queries.getRowSet(VIEW_TASK_ORDER_ITEMS, null, Filter.equals(COL_TASK, getActiveRowId()),
          new RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet result) {
              if (result.getNumberOfRows() > 0) {
                dataConsumer.accept(new BeeRowSet[] {result});
              } else {
                TaskEditor.super.getReportData(dataConsumer);
              }
            }
          });
    }
  }

  private static void addObserver(FormView form, IsRow row, Long userId) {
    Set<Long> observers = DataUtils.parseIdSet(row.getProperty(PROP_OBSERVERS));

    observers.add(userId);
    row.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(observers));
    form.refreshBySource(PROP_OBSERVERS);
  }

  private static BeeRowSet getEvents(IsRow row) {
    if (!BeeUtils.isEmpty(row.getProperty(PROP_EVENTS))) {
      return BeeRowSet.restore(row.getProperty(PROP_EVENTS));
    }

    return Data.createRowSet(VIEW_TASK_EVENTS);
  }

  private UnboundSelector getProjectSelector() {
    Relation relation = Relation.create();
    relation.setViewName(ProjectConstants.VIEW_PROJECTS);
    relation.disableNewRow();
    relation.disableEdit();
    relation.setChoiceColumns(Lists.newArrayList("ID", ProjectConstants.COL_PROJECT_NAME,
        ALS_COMPANY_NAME));
    relation.setSearchableColumns(Lists.newArrayList("ID", ProjectConstants.COL_PROJECT_NAME,
        ALS_COMPANY_NAME));

    Filter filter = Filter.and(Filter.notEquals(COL_STATUS, ProjectStatus.APPROVED),
        Filter.in(Data.getIdColumn(ProjectConstants.VIEW_PROJECTS),
            ProjectConstants.VIEW_PROJECT_USERS, ProjectConstants.COL_PROJECT,
            Filter.equals(COL_USER, getOwner())),
        Filter.in(Data.getIdColumn(ProjectConstants.VIEW_PROJECTS),
            ProjectConstants.VIEW_PROJECT_USERS, ProjectConstants.COL_PROJECT,
            Filter.equals(COL_USER, getExecutor())));

    relation.setFilter(filter);

    UnboundSelector selector = UnboundSelector.create(relation,
        Lists.newArrayList("ID", ProjectConstants.COL_PROJECT_NAME));

    selector.setWidth("100%");

    return selector;
  }

  private static UnboundSelector getProjectStageSelector() {
    Relation relation = Relation.create();
    relation.setViewName(ProjectConstants.VIEW_PROJECT_STAGES);
    relation.disableNewRow();
    relation.disableEdit();
    relation.setChoiceColumns(Lists.newArrayList(ProjectConstants.COL_STAGE_NAME,
        ProjectConstants.COL_STAGE_START_DATE, ProjectConstants.COL_STAGE_END_DATE));
    relation.setSearchableColumns(Lists.newArrayList(ProjectConstants.COL_STAGE_NAME,
        ProjectConstants.COL_STAGE_START_DATE, ProjectConstants.COL_STAGE_END_DATE));

    UnboundSelector selector = UnboundSelector.create(relation,
        Lists.newArrayList(ProjectConstants.COL_STAGE_NAME));

    selector.setWidth("100%");

    return selector;
  }

  private static FaLabel getOrderLabelInfo() {
    boolean order = TaskHelper.getBooleanValueFromStorage(TaskHelper.NAME_ORDER);
    FaLabel label = order
        ? new FaLabel(FontAwesome.SORT_NUMERIC_ASC)
        : new FaLabel(FontAwesome.SORT_NUMERIC_DESC);

    label.setTitle(order ? Localized.dictionary().crmTaskCommentsAsc()
        : Localized.dictionary().crmTaskCommentsDesc());

    return label;
  }

  private static void resetField(FormView form, IsRow oldRow, IsRow newRow, String column) {
    DataInfo tasksView = Data.getDataInfo(VIEW_TASKS);
    int idxColumn = form.getDataIndex(column);

    if (BeeConst.isUndef(idxColumn)) {
      return;
    }

    newRow.setValue(idxColumn, oldRow.getValue(idxColumn));

    for (ViewColumn col : tasksView.getDescendants(column, true)) {
      int idxCol = form.getDataIndex(col.getName());

      if (BeeConst.isUndef(idxCol)) {
        continue;
      }

      newRow.setValue(idxCol, oldRow.getValue(idxCol));
    }

    form.refreshBySource(column);
  }

  private void createCellValidationHandler(FormView form, IsRow row) {

    if (form == null || row == null) {
      return;
    }

    form.addCellValidationHandler(NAME_PRIVATE_TASK, event -> {
      boolean canModify = isOwner() || isExecutor();
      IsRow oldRow = form.getOldRow();
      int idxOwner = form.getDataIndex(COL_OWNER);

      if (oldRow != null && !BeeConst.isUndef(idxOwner)) {
        canModify = canModify || Objects.equals(userId, oldRow.getLong(idxOwner));
      }
      return canModify;
    });
  }

  private void createTaskLinks(final IsRow taskRow, final Long eventId,
      final String taskIds) {
    final Consumer<Boolean> consumer = new Consumer<Boolean>() {
      private int consumeCount = 2;

      @Override
      public void accept(Boolean input) {
        consumeCount--;

        if (consumeCount == 0) {
          Map<String, String> data = Maps.newLinkedHashMap();
          data.put(BeeUtils.toString(TaskEvent.CREATE.ordinal()), taskIds);

          ParameterList params =
              createParams(TaskEvent.EDIT, BeeConst.STRING_EMPTY);

          params.addDataItem(COL_EVENT_DATA, Codec.beeSerialize(data));

          sendRequest(params, TaskEvent.EDIT);
        }
      }
    };

    if (DataUtils.isId(eventId)) {
      Queries.getValue(VIEW_TASK_EVENTS, eventId, COL_EVENT_DATA, new RpcCallback<String>() {

        @Override
        public void onSuccess(String result) {
          Map<String, String> data;

          if (BeeUtils.isEmpty(result)) {
            data = Maps.newLinkedHashMap();
          } else {
            data = Codec.deserializeLinkedHashMap(result);
          }

          if (data.containsKey(BeeUtils.toString(TaskEvent.CREATE.ordinal()))) {
            List<Long> lTaskIds =
                DataUtils.parseIdList(data.get(BeeUtils.toString(TaskEvent.CREATE.ordinal())));
            lTaskIds.addAll(DataUtils.parseIdList(taskIds));

            data.put(BeeUtils.toString(TaskEvent.CREATE.ordinal()), DataUtils
                .buildIdList(lTaskIds));
          } else {
            data.put(BeeUtils.toString(TaskEvent.CREATE.ordinal()), taskIds);
          }

          Queries.update(VIEW_TASK_EVENTS, eventId, COL_EVENT_DATA, Value.getValue(Codec
                  .beeSerialize(data)),
              new IntCallback() {

                @Override
                public void onSuccess(Integer updateCount) {
                  consumer.accept(null);
                }
              });

        }
      });
    } else {
      consumer.accept(null);
    }

    List<Long> relIds = DataUtils.parseIdList(taskRow.getProperty(PFX_RELATED + VIEW_TASKS));
    relIds.addAll(DataUtils.parseIdList(taskIds));

    Queries.updateChildren(VIEW_TASKS, taskRow.getId(), Lists.newArrayList(RowChildren
            .create(TBL_RELATIONS, COL_TASK, null, COL_TASK, DataUtils.buildIdList(relIds))),
        new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            consumer.accept(null);
          }
        });

  }

  private ParameterList createParams(TaskEvent event, String comment) {
    return createParams(event, getNewRow(), comment);
  }

  private ParameterList createParams(TaskEvent event, BeeRow row, String comment) {

    Collection<RowChildren> updatedRelations =
        relations == null ? null : relations.getChildrenForUpdate();
    return TaskHelper.createTaskParams(getFormView(), event, row, updatedRelations, comment);
  }

  private ParameterList createParams(TaskEvent event, BeeRow row, Collection<RowChildren>
      updatedRelations, String comment) {
    return TaskHelper.createTaskParams(getFormView(), event, row, updatedRelations, comment);
  }

  private void doApprove() {
    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskConfirmation());

    final String did = dialog.addDateTime(Localized.dictionary().crmTaskConfirmDate(), true,
        TimeUtils.nowMinutes());
    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.dictionary().crmTaskConfirm(), () -> {

      DateTime approved = dialog.getDateTime(did);
      if (approved == null) {
        showError(Localized.dictionary().crmEnterConfirmDate());
        return;
      }

      BeeRow newRow = getNewRow(TaskStatus.APPROVED);
      newRow.setValue(getFormView().getDataIndex(COL_APPROVED), approved);

      ParameterList params = createParams(TaskEvent.APPROVE, newRow, dialog.getComment(cid));

      sendRequest(params, TaskEvent.APPROVE);
      dialog.close();
    });

    dialog.display();
    dialog.focusOnOpen(DomUtils.getWidget(cid));
  }

  private void doCancel() {
    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskCancellation());

    final String cid = dialog.addComment(true);

    dialog.addAction(Localized.dictionary().crmTaskCancel(), () -> {

      String comment = dialog.getComment(cid);
      if (BeeUtils.isEmpty(comment)) {
        showError(Localized.dictionary().crmEnterComment());
        return;
      }

      ParameterList params = createParams(TaskEvent.CANCEL, getNewRow(TaskStatus.CANCELED),
          comment);

      sendRequest(params, TaskEvent.CANCEL);
      dialog.close();
    });

    dialog.display();
  }

  private void doComment() {
    final TaskDialog dialog =
        new TaskDialog(Localized.dictionary().crmTaskCommentTimeRegistration());

    final String cid = dialog.addComment(false);
    final String fid = dialog.addFileCollector();
    Map<String, String> durIds = setDurations(dialog);

    dialog.addAction(Localized.dictionary().actionSave(), () -> {

      String comment = dialog.getComment(cid);
      String time = dialog.getTime(durIds.get(COL_DURATION));
      Long type = dialog.getSelector(durIds.get(COL_DURATION_TYPE)).getRelatedId();

      if (BeeUtils.allEmpty(comment, time)) {
        showError(Localized.dictionary().crmEnterCommentOrDuration());
        return;
      }

      if (!BeeUtils.isEmpty(time) && !DataUtils.isId(type)) {
        showError(Localized.dictionary().crmEnterDurationType());
        return;
      }

      ParameterList params = createParams(TaskEvent.COMMENT, comment);

      if (setDurationParams(dialog, durIds, params)) {
        sendRequest(params, TaskEvent.COMMENT, dialog.getFiles(fid));
        dialog.close();
      }
    });

    dialog.display();
  }

  private void doComplete() {
    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskFinishing());

    final String cid = dialog.addComment(false);
    final String fid = dialog.addFileCollector();

    final Map<String, String> durIds = setDurations(dialog);
    final String dd =
        dialog.addDateTime(Localized.dictionary().crmTaskFinishDate(), true, TimeUtils
            .nowMinutes());
    durIds.put(COL_DURATION_DATE, dd);

    dialog.addAction(Localized.dictionary().crmActionFinish(), () -> {

      DateTime completed = dialog.getDateTime(dd);
      String time = dialog.getTime(durIds.get(COL_DURATION));
      Long type = dialog.getSelector(durIds.get(COL_DURATION_TYPE)).getRelatedId();

      if (completed == null) {
        showError(Localized.dictionary().crmEnterCompleteDate());
        return;
      }

      if (!BeeUtils.isEmpty(time) && !DataUtils.isId(type)) {
        showError(Localized.dictionary().crmEnterDurationType());
        return;
      }

      String comment = dialog.getComment(cid);

      TaskStatus status;
      TaskEvent event;

      if (isOwner() && isExecutor()) {
        status = TaskStatus.APPROVED;
        event = TaskEvent.APPROVE;
      } else {
        status = TaskStatus.COMPLETED;
        event = TaskEvent.COMPLETE;
      }
      BeeRow newRow = getNewRow(status);
      newRow.setValue(getFormView().getDataIndex(COL_COMPLETED), completed);

      ParameterList params = createParams(event, newRow, comment);

      if (setDurationParams(dialog, durIds, params)) {
        sendRequest(params, event, dialog.getFiles(fid));
        dialog.close();
      }
    });

    dialog.display();
    dialog.focusOnOpen(DomUtils.getWidget(cid));
  }

  private void doCreate(final Long eventId, final IsRow taskRow) {
    DataInfo newTaskInfo = Data.getDataInfo(VIEW_TASKS);
    BeeRow newTaskRow = RowFactory.createEmptyRow(newTaskInfo, true);

    int idxSummary = newTaskInfo.getColumnIndex(COL_SUMMARY);
    int idxDescription = newTaskInfo.getColumnIndex(COL_DESCRIPTION);

    newTaskRow.setValue(idxSummary, Data.clamp(VIEW_TASKS, COL_SUMMARY, BeeUtils.joinWords(taskRow
        .getString(idxSummary), BeeUtils
        .parenthesize(taskRow.getId()))));

    String description = taskRow.getString(idxDescription);

    BeeRowSet events = getEvents(taskRow);

    if (!events.isEmpty() && DataUtils.isId(eventId)) {
      IsRow event = events.getRowById(eventId);

      if (event != null
          && Objects.equals(TaskEvent.COMMENT.ordinal(), event.getInteger(events
          .getColumnIndex(TaskConstants.COL_EVENT)))) {
        description = BeeUtils.join(BeeConst.STRING_EOL
                + BeeUtils.replicate(BeeConst.CHAR_MINUS, BeeConst.MAX_SCALE)
                + BeeConst.STRING_EOL, description,
            BeeUtils.joinWords(event.getDateTime(events.getColumnIndex(COL_PUBLISH_TIME)),
                BeeUtils.nvl(event.getString(events.getColumnIndex(ALS_PUBLISHER_FIRST_NAME)),
                    BeeConst.STRING_EMPTY),
                BeeUtils.nvl(event.getString(events.getColumnIndex(ALS_PUBLISHER_LAST_NAME)),
                    BeeConst.STRING_EMPTY) + BeeConst.STRING_COLON,
                event.getString(events.getColumnIndex(COL_COMMENT))));
      }
    } else if (!events.isEmpty()) {
      for (IsRow event : events) {
        if (event != null
            && Objects.equals(TaskEvent.COMMENT.ordinal(), event.getInteger(events
            .getColumnIndex(TaskConstants.COL_EVENT)))) {
          description = BeeUtils.join(BeeConst.STRING_EOL
                  + BeeUtils.replicate(BeeConst.CHAR_MINUS, BeeConst.MAX_SCALE)
                  + BeeConst.STRING_EOL, description,
              BeeUtils.joinWords(event.getDateTime(events.getColumnIndex(COL_PUBLISH_TIME)),
                  BeeUtils.nvl(event.getString(events.getColumnIndex(ALS_PUBLISHER_FIRST_NAME)),
                      BeeConst.STRING_EMPTY),
                  BeeUtils.nvl(event.getString(events.getColumnIndex(ALS_PUBLISHER_LAST_NAME)),
                      BeeConst.STRING_EMPTY) + BeeConst.STRING_COLON,
                  event.getString(events.getColumnIndex(COL_COMMENT))));
        }
      }
    }

    newTaskRow.setValue(idxDescription, Data.clamp(VIEW_TASKS, COL_DESCRIPTION, description));

    Arrays.asList(COL_PRIORITY, COL_TASK_TYPE, COL_COMPANY, COL_CONTACT,
        ProjectConstants.COL_PROJECT, ProjectConstants.COL_PROJECT_STAGE)
        .forEach((String column) -> {

          if (!newTaskInfo.containsColumn(column)) {
            return;
          }
          Data.setValue(VIEW_TASKS, newTaskRow, column,
              Data.getString(VIEW_TASKS, taskRow, column));

          if (newTaskInfo.hasRelation(column)) {
            RelationUtils.updateRow(Data.getDataInfo(VIEW_TASKS), column, newTaskRow,
                Data.getDataInfo(VIEW_TASKS), taskRow, false);
          }
        });

    if (taskRow.hasPropertyValue(PROP_FILES)) {
      newTaskRow.setProperty(PROP_FILES, taskRow.getProperty(PROP_FILES));
    }
    RowFactory.createRow(newTaskInfo.getNewRowForm(), newTaskInfo.getNewRowCaption(), newTaskInfo,
        newTaskRow, Modality.ENABLED, null, new TaskBuilder(true), null,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            if (result == null || result.getNumberOfCells() < 1) {
              return;
            }
            createTaskLinks(taskRow, eventId, result.getString(0));
          }
        });
  }

  private void doEvent(TaskEvent event) {

    if (!event.canExecute(getExecutor(), getOwner(), getObserversSelector(),
        EnumUtils.getEnumByIndex(TaskStatus.class, getStatus()))) {
      showError(Localized.dictionary().actionNotAllowed());
      return;
    }

    switch (event) {
      case COMMENT:
        doComment();
        break;

      case FORWARD:
        doForward();
        break;

      case EXTEND:
        doExtend();
        break;

      case SUSPEND:
        doSuspend();
        break;

      case CANCEL:
        doCancel();
        break;

      case COMPLETE:
        doComplete();
        break;

      case APPROVE:
        doApprove();
        break;

      case RENEW:
        doRenew();
        break;

      case CREATE:
        doCreate(null, getActiveRow());
        break;
      case OUT_OF_OBSERVERS:
        doOut();
        break;
      case REFRESH:
        onStartEdit(getFormView(), getActiveRow(), null);
        break;
      case ACTIVATE:
        doExecute();
        break;
      case VISIT:
        doVisit();
        break;
      case CREATE_NOT_SCHEDULED:
      case CREATE_SCHEDULED:
      case EDIT:
        Assert.untouchable();
    }
  }

  private void doExecute() {
    TaskStatus newStatus = TaskStatus.ACTIVE;

    BeeRow newRow = getNewRow(newStatus);
    ParameterList params = createParams(TaskEvent.ACTIVATE, newRow, BeeConst.STRING_EMPTY);

    sendRequest(params, TaskEvent.ACTIVATE);
  }

  private void doExtend() {
    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskTermChange());

    final String endId = dialog.addDateTime(Localized.dictionary().crmFinishDate(), true, null);

    if (dialog.getChild(endId) instanceof InputDateTime) {
      InputDateTime input = (InputDateTime) dialog.getChild(endId);
      JustDate minTime = new JustDate();
      input.setMinDate(minTime);

      minTime.increment();
      DateTime extendTime = new DateTime(minTime.getDateTime().getTime());

      if (BeeUtils.isPositive(getPrmEndOfWorkDay())) {
        extendTime = new DateTime(new JustDate().getDateTime().getTime() + getPrmEndOfWorkDay());
      }

      input.setDateTime(extendTime);
    }

    final String cid = dialog.addComment(false);

    InputDateTime newEndInput = dialog.getInputDateTime(endId);
    newEndInput.addEditStopHandler(event -> {
      if (event.isChanged()) {
        Long input = Global.getParameterTime(PRM_END_OF_WORK_DAY);

        if (Objects.nonNull(input)) {
          DateTime dateTime = TimeUtils.toDateTimeOrNull(input);

          if (dateTime != null) {
            int hour = dateTime.getUtcHour();
            int minute = dateTime.getUtcMinute();
            DateTime value = newEndInput.getDateTime();
            value.setHour(hour);
            value.setMinute(minute);
            newEndInput.setDateTime(value);
          }
        }
      }
    });

    dialog.addAction(Localized.dictionary().crmTaskChangeTerm(), () -> {

      DateTime newStart = getDateTime(COL_START_TIME);
      DateTime oldEnd = getDateTime(COL_FINISH_TIME);

      DateTime newEnd = newEndInput.getDateTime();

      if (newEnd == null) {
        showError(Localized.dictionary().crmEnterFinishDate());
        return;
      }

      if (Objects.equals(newEnd, oldEnd)) {
        showError(Localized.dictionary().crmTermNotChanged());
        return;
      }

      if (TimeUtils.isLeq(newEnd, newStart)) {
        showError(Localized.dictionary().crmFinishDateMustBeGreaterThanStart());
        return;
      }

      DateTime now = TimeUtils.nowMinutes();
      if (TimeUtils.isLess(newEnd, TimeUtils.nowMinutes())) {
        Global.showError("Time travel not supported",
            Collections.singletonList(Localized.dictionary().crmFinishDateMustBeGreaterThan()
                + " " + Format.renderDateTime(now)));
        return;
      }

      BeeRow newRow = getNewRow();
      if (newStart != null) {
        newRow.setValue(getFormView().getDataIndex(COL_START_TIME), newStart);
      }
      if (!Objects.equals(newEnd, oldEnd)) {
        newRow.setValue(getFormView().getDataIndex(COL_FINISH_TIME), newEnd);
      }

      ParameterList params = createParams(TaskEvent.EXTEND, newRow, dialog.getComment(cid));
      if (oldEnd != null && !Objects.equals(newEnd, oldEnd)) {
        params.addDataItem(VAR_TASK_FINISH_TIME, oldEnd.getTime());
      }

      sendRequest(params, TaskEvent.EXTEND);
      dialog.close();
    });

    dialog.display(endId);
  }

  private void doForward() {
    final Long oldUser = getExecutor();
    Set<Long> exclusions = new HashSet<>();
    Set<Long> filter = new HashSet<>();
    if (oldUser != null) {
      exclusions.add(oldUser);
    }

    if (!BeeUtils.isEmpty(getProjectUsers())) {
      filter.addAll(getProjectUsers());
    }

    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskForwarding());

    final String sid =
        dialog.addSelector(Localized.dictionary().crmTaskExecutor(), VIEW_USERS,
            Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME),
            true, exclusions, filter, null);
    final String obs = dialog.addCheckBox(true);

    final String cid = dialog.addComment(false);
    final String fid = dialog.addFileCollector();

    dialog.addAction(Localized.dictionary().crmActionForward(), () -> {

      DataSelector selector = dialog.getSelector(sid);

      Long newUser = selector.getRelatedId();
      if (newUser == null) {
        showError(Localized.dictionary().crmEnterExecutor());
        return;
      }
      if (Objects.equals(newUser, oldUser)) {
        showError(Localized.dictionary().crmSelectedSameExecutor());
        return;
      }

      String comment = dialog.getComment(cid);

      BeeRow newRow = getNewRow();
      RelationUtils.updateRow(Data.getDataInfo(VIEW_TASKS), COL_EXECUTOR, newRow,
          Data.getDataInfo(VIEW_USERS), selector.getRelatedRow(), true);

      TaskStatus newStatus = TaskStatus.NOT_VISITED;

      /* Forward task itself */
      if (Objects.equals(newUser, userId)) {
        newStatus = TaskStatus.VISITED;
      }

      newRow.setValue(getDataIndex(COL_STATUS), newStatus.ordinal());

      if (dialog.isChecked(obs)) {
        List<Long> obsUsers = DataUtils.parseIdList(newRow.getProperty(PROP_OBSERVERS));
        if (!obsUsers.contains(oldUser)) {
          obsUsers.add(oldUser);
          newRow.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(obsUsers));
        }
      }

      ParameterList params = createParams(TaskEvent.FORWARD, newRow, comment);
      sendRequest(params, TaskEvent.FORWARD, dialog.getFiles(fid));
      dialog.close();
    });

    dialog.display();
  }

  private void doOut() {
    BeeRow row = getNewRow();
    List<Long> obsIds = DataUtils.parseIdList(row.getProperty(PROP_OBSERVERS));

    obsIds.remove(userId);

    row.setProperty(PROP_OBSERVERS,
        DataUtils.buildIdList(obsIds));

    ParameterList params =
        createParams(TaskEvent.OUT_OF_OBSERVERS, row, BeeConst.STRING_EMPTY);
    sendRequest(params, TaskEvent.OUT_OF_OBSERVERS);
  }

  private void doRenew() {
    final TaskDialog dialog =
        new TaskDialog(Localized.dictionary().crmTaskReturningForExecution());

    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.dictionary().crmTaskReturnExecution(), () -> {

      TaskStatus newStatus = isExecutor() ? TaskStatus.VISITED : TaskStatus.NOT_VISITED;

      BeeRow newRow = getNewRow(newStatus);
      newRow.clearCell(getFormView().getDataIndex(COL_COMPLETED));
      newRow.clearCell(getFormView().getDataIndex(COL_APPROVED));

      ParameterList params = createParams(TaskEvent.RENEW, newRow, dialog.getComment(cid));

      sendRequest(params, TaskEvent.RENEW);
      dialog.close();
    });

    dialog.display();
  }

  private void doVisit() {
    TaskStatus newStatus = TaskStatus.VISITED;

    BeeRow newRow = getNewRow(newStatus);
    ParameterList params = createParams(TaskEvent.VISIT, newRow, BeeConst.STRING_EMPTY);
    params.addDataItem(VAR_TASK_VISITED_STATE, 1);

    sendRequest(params, TaskEvent.VISIT);
  }

  private void doSuspend() {
    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskSuspension());

    final String cid = dialog.addComment(true);

    dialog.addAction(Localized.dictionary().crmActionSuspend(), () -> {

      String comment = dialog.getComment(cid);
      if (BeeUtils.isEmpty(comment)) {
        showError(Localized.dictionary().crmEnterComment());
        return;
      }

      ParameterList params = createParams(TaskEvent.SUSPEND, getNewRow(TaskStatus.SUSPENDED),
          comment);

      sendRequest(params, TaskEvent.SUSPEND);
      dialog.close();
    });

    dialog.display();
  }

  private FaLabel getCommentsLabelInfo() {
    FaLabel label =
        isDefaultLayout ? new FaLabel(FontAwesome.COLUMNS) : new FaLabel(FontAwesome.LIST_ALT);
    label.addStyleName(BeeConst.CSS_CLASS_PREFIX + "crm-commentLayout-label");
    label.setTitle(isDefaultLayout ? Localized.dictionary().crmTaskShowCommentsRight() : Localized
        .dictionary().crmTaskShowCommentsBelow());

    return label;
  }

  private void ensureDefaultDBAParameters(final HasEnabled widget, final IsRow row) {
    if (row == null && dbaParameters == null) {
      return;
    }

    dbaParameters.clear();

    final BiConsumer<String, Pair<Long, String>> paramHolder =
        new BiConsumer<String, Pair<Long, String>>() {
          static final int MAX_PARAM_COUNT = 2;
          int added;

          @Override
          public void accept(String prm, Pair<Long, String> value) {
            if (!value.isNull()) {
              dbaParameters.put(prm, value);
            }
            ensureAllParameters();
          }

          private void ensureAllParameters() {
            added++;
            if (added >= MAX_PARAM_COUNT) {
              createDocument(null, row, true);
              TaskHelper.setWidgetEnabled(widget, true);
            }
          }
        };

    Global.getParameterRelation(PRM_DEFAULT_DBA_TEMPLATE, (id, name)
        -> paramHolder.accept(PRM_DEFAULT_DBA_TEMPLATE, Pair.of(id, name)));

    Global.getParameterRelation(PRM_DEFAULT_DBA_DOCUMENT_TYPE, (id, name)
        -> paramHolder.accept(PRM_DEFAULT_DBA_DOCUMENT_TYPE, Pair.of(id, name)));
  }

  private DateTime getDateTime(String colName) {
    return getFormView().getActiveRow().getDateTime(getFormView().getDataIndex(colName));
  }

  private Long getPrmEndOfWorkDay() {
    return prmEndOfWorkDay;
  }

  private Long getExecutor() {
    return getLong(COL_EXECUTOR);
  }

  private Long getLong(String colName) {
    return getFormView().getActiveRow().getLong(getFormView().getDataIndex(colName));
  }

  private BeeRow getNewRow() {
    return DataUtils.cloneRow(getFormView().getActiveRow());
  }

  private BeeRow getNewRow(TaskStatus status) {
    BeeRow row = getNewRow();
    row.setValue(getFormView().getDataIndex(COL_STATUS), status.ordinal());
    return row;
  }

  private List<Long> getObserversSelector() {
    return DataUtils.parseIdList(getFormView().getActiveRow().getProperty(PROP_OBSERVERS));
  }

  private Long getOwner() {
    return getLong(COL_OWNER);
  }

  private List<Long> getProjectUsers() {
    return projectUsers;
  }

  private Integer getStatus() {
    return getFormView().getActiveRow().getInteger(getFormView().getDataIndex(COL_STATUS));
  }

  private boolean isExecutor() {
    return Objects.equals(userId, getExecutor());
  }

  private boolean isOwner() {
    return Objects.equals(userId, getOwner());
  }

  private void onResponse(BeeRow data) {
    RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);

    FormView form = getFormView();
    Long lastAccess = BeeUtils.toLongOrNull(data.getProperty(PROP_LAST_ACCESS,
        BeeKeeper.getUser().getUserId()));

    if (relations != null && !BeeUtils.isEmpty(relations.getChildrenForUpdate())) {
      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
      relations.requery(data, data.getId());
    }

    if (!getEvents(data).isEmpty()) {
      showEventsAndDuration(form, form.getActiveRow(), getEvents(data), TaskUtils.getFiles(data),
          lastAccess);
    }

    form.updateRow(data, true);
  }

  private void requeryEvents(final long taskId) {
    ParameterList params = TasksKeeper.createArgs(SVC_GET_TASK_DATA);
    params.addDataItem(VAR_TASK_ID, taskId);

    String properties = DEFAULT_TASK_PROPERTIES;
    if (TaskHelper.getBooleanValueFromStorage(TaskHelper.NAME_ORDER)) {
      properties = BeeUtils.join(BeeConst.STRING_COMMA, properties, PROP_DESCENDING);
    }
    params.addDataItem(VAR_TASK_PROPERTIES, properties);

    RpcCallback<ResponseObject> callback = new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        getFormView().notifySevere(reason);
      }

      @Override
      public void onSuccess(ResponseObject result) {
        if (getFormView().getActiveRow().getId() != taskId) {
          return;
        }

        BeeRow data = getResponseRow(SVC_GET_TASK_DATA, result, this);
        if (data != null) {
          onResponse(data);
        }
      }
    };

    sendRequest(params, callback);
  }

  private static HtmlTable renderProjectChoiceTable(UnboundSelector prjSelector,
      UnboundSelector prjStageSelector) {

    HtmlTable table = new HtmlTable();
    table.setKind(TableKind.CONTROLS);
    table.setColumnCellKind(0, CellKind.LABEL);
    table.setColumnCellStyles(1, "width:300px");

    table.setText(0, 0, Localized.dictionary().project(), StyleUtils.NAME_REQUIRED);
    table.setWidget(0, 1, prjSelector);
    table.setText(1, 0, Localized.dictionary().prjStage());
    table.setWidget(1, 1, prjStageSelector);

    return table;
  }

  private void sendFiles(final List<FileInfo> files, final long taskId, final long teId) {

    final Holder<Integer> counter = Holder.of(0);

    final List<BeeColumn> columns = Data.getColumns(VIEW_TASK_FILES,
        Lists.newArrayList(COL_TASK, COL_TASK_EVENT, COL_FILE, COL_CAPTION));

    for (final FileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, result -> {
        List<String> values = Lists.newArrayList(BeeUtils.toString(taskId),
            BeeUtils.toString(teId), BeeUtils.toString(result.getId()), result.getCaption());

        Queries.insert(VIEW_TASK_FILES, columns, values, null, new RowCallback() {
          @Override
          public void onSuccess(BeeRow row) {
            counter.set(counter.get() + 1);
            if (counter.get() == files.size()) {
              requeryEvents(taskId);
            }
          }
        });
      });
    }
  }

  private void sendRequest(ParameterList params, TaskEvent event) {
    sendRequest(params, event, null);
  }

  private void sendRequest(ParameterList params, final TaskEvent event,
      final List<FileInfo> files) {

    RpcCallback<ResponseObject> callback = new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        getFormView().notifySevere(reason);
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(event.getCaption(), result, this);
        if (data != null) {
          if (result.hasWarnings()) {
            BeeKeeper.getScreen().notifyWarning(result.getWarnings());
          }

          onResponse(data);

          if (!BeeUtils.isEmpty(files)) {
            Long teId = BeeUtils.toLongOrNull(data.getProperty(PROP_LAST_EVENT_ID));
            if (DataUtils.isId(teId)) {
              sendFiles(files, data.getId(), teId);
            }
          }
        }
      }
    };

    sendRequest(params, callback);
  }

  private Map<String, String> setDurations(TaskDialog dialog) {
    final String durId = dialog.addTime(Localized.dictionary().crmSpentTime(), false);
    String durTypeId = dialog.addSelector(Localized.dictionary().crmDurationType(),
        VIEW_TASK_DURATION_TYPES, Lists.newArrayList(ALS_DURATION_TYPE_NAME), false, null, null,
        COL_DURATION_TYPE);

    Filter filter = Filter.equals(COL_TASK_TYPE,
        getActiveRow().getLong(Data.getColumnIndex(VIEW_TASKS, COL_TASK_TYPE)));

    dialog.getSelector(durTypeId).getOracle().setAdditionalFilter(filter, true);

    final Map<String, String> durIds = new HashMap<>();
    durIds.put(COL_DURATION, durId);
    durIds.put(COL_DURATION_TYPE, durTypeId);

    return durIds;
  }

  private void setCommentsLayout() {
    /*if (isDefaultLayout) {
      if (taskWidget != null) {
        int height = BeeUtils.max(getFormView().getWidgetByName("TaskContainer").getElement()
          .getScrollHeight(), 660);

        split.addNorth(taskWidget, height + 52);
        StyleUtils.autoWidth(taskWidget.getElement());
        split.updateCenter(taskEventsWidget);
      }
    } else {
      Integer size = BeeKeeper.getStorage().getInteger(
          TaskHelper.getStorageKey(TaskHelper.NAME_TASK_TREE));
      split.addWest(taskWidget, size == null ? 660 : size);
      StyleUtils.autoHeight(taskWidget.getElement());
      split.updateCenter(taskEventsWidget);
    }*/
  }

  private void setPrmEndOfWorkDay(Long prmEndOfWorkDay) {
    this.prmEndOfWorkDay = prmEndOfWorkDay;
  }

  private void setLateIndicatorHtml(Pair<AbstractSlackRenderer.SlackKind, Long> data) {
    if (lateIndicator == null) {
      return;
    }

    String text = BeeConst.STRING_EMPTY;

    if (data != null) {
      if (!data.bEquals(0L)) {
        text = BeeUtils.parenthesize(AbstractSlackRenderer.getFormatedTimeLabel(data.getB()));
      }

      setLateIndicatorStyle(data.getA());
    } else {
      setLateIndicatorStyle(null);
    }

    lateIndicator.setHtml(text + BeeConst.HTML_NBSP);
  }

  private void setLateIndicatorStyle(AbstractSlackRenderer.SlackKind kind) {
    if (lateIndicator == null) {
      return;
    }

    for (AbstractSlackRenderer.SlackKind k : AbstractSlackRenderer.SlackKind.values()) {
      lateIndicator.removeStyleName(STYLE_TASK_LATE_KIND + k.toString().toLowerCase());
    }

    if (kind != null) {
      lateIndicator.addStyleName(STYLE_TASK_LATE_KIND + kind.toString().toLowerCase());
    }
  }

  private FaLabel createMenuLabel() {
    FaLabel menu = new FaLabel(FontAwesome.NAVICON);
    menu.addClickHandler(arg0 -> {
      final HtmlTable tb = new HtmlTable(BeeConst.CSS_CLASS_PREFIX + "GridMenu-table");
      FaLabel commentLbl = getCommentsLabelInfo();
      FaLabel orderLbl = getOrderLabelInfo();

      tb.setWidget(0, 0, commentLbl);
      tb.setText(0, 1, commentLbl.getTitle());
      tb.setWidget(1, 0, orderLbl);
      tb.setText(1, 1, orderLbl.getTitle());

      int projectIdx = Data.getColumnIndex(VIEW_TASKS, ProjectConstants.COL_PROJECT);
      int prjStatusIdx = Data.getColumnIndex(VIEW_TASKS, ALS_PROJECT_STATUS);
      int idxProjectOwner = getFormView().getDataIndex(ALS_PROJECT_OWNER);

      final long projectOwner = BeeUtils.unbox(getActiveRow().getLong(idxProjectOwner));
      final long projectId = BeeUtils.unbox(getActiveRow().getLong(projectIdx));
      Integer prjStatus = getActiveRow().getInteger(prjStatusIdx);

      String caption = null;
      FaLabel label = null;

      if (projectId > 0 && !Objects.equals(prjStatus, ProjectStatus.APPROVED.ordinal())
          && Objects.equals(projectOwner, userId)) {
        label = new FaLabel(FontAwesome.OUTDENT);
        caption = Localized.dictionary().crmTaskRemoveFromProject();
      } else if (projectId == 0 && isOwner()) {
        label = new FaLabel(FontAwesome.INDENT);
        caption = Localized.dictionary().crmTaskAddToProject();
      }

      if (label != null && caption != null) {
        tb.setWidget(2, 0, label);
        tb.setText(2, 1, caption);
      }

      tb.addClickHandler(ev -> {
        Element targetElement = EventUtils.getEventTargetElement(ev);
        TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);
        int index = rowElement.getRowIndex();

        switch (index) {
          case 0:
            BeeKeeper.getUser().setCommentsLayout(!isDefaultLayout);
            isDefaultLayout = !isDefaultLayout;
            UiHelper.closeDialog(tb);
            setCommentsLayout();
            break;

          case 1:
            if (TaskHelper.getBooleanValueFromStorage(TaskHelper.NAME_ORDER)) {
              BeeKeeper.getStorage().remove(TaskHelper.getStorageKey(TaskHelper.NAME_ORDER));
              getActiveRow().removeProperty(PROP_DESCENDING);
            } else {
              BeeKeeper.getStorage().set(
                  TaskHelper.getStorageKey(TaskHelper.NAME_ORDER), true);
              getActiveRow().setProperty(PROP_DESCENDING, BeeConst.INT_TRUE);
            }

            UiHelper.closeDialog(tb);
            doEvent(TaskEvent.REFRESH);
            break;

          case 2:
            UiHelper.closeDialog(tb);

            if (projectId > 0) {

              Global.confirmRemove(null, Localized.dictionary().crmTaskAskRemoveFromProject(),
                  () -> updateProjectInfo(BeeConst.STRING_EMPTY, BeeConst.STRING_EMPTY));
            } else {
              UnboundSelector prjSelector = getProjectSelector();
              UnboundSelector prjStageSelector = getProjectStageSelector();
              prjStageSelector.addSelectorHandler(event -> {
                if (event.isOpened()) {
                  Filter filter = Filter.equals(ProjectConstants.COL_PROJECT,
                      prjSelector.getRelatedId());
                  event.getSelector().setAdditionalFilter(filter);
                }
              });

              Global.inputWidget(Localized.dictionary().crmTaskAddToProject(),
                  renderProjectChoiceTable(prjSelector, prjStageSelector), new InputCallback() {
                    @Override
                    public void onSuccess() {

                      String prjStageId = prjStageSelector.getRelatedId() == null
                          ? BeeConst.STRING_EMPTY : prjStageSelector.getRelatedId().toString();

                      updateProjectInfo(prjSelector.getRelatedId().toString(), prjStageId);
                    }

                    @Override
                    public String getErrorMessage() {
                      if (BeeUtils.isEmpty(prjSelector.getValue())) {
                        return Localized.dictionary()
                            .fieldRequired(Localized.dictionary().project());
                      }
                      return InputCallback.super.getErrorMessage();
                    }
                  });
            }
            break;
          default:
        }
      });

      Popup popup = new Popup(OutsideClick.CLOSE, BeeConst.CSS_CLASS_PREFIX + "GridMenu-popup");
      popup.setWidget(tb);
      popup.setHideOnEscape(true);
      popup.showRelativeTo(menu.getElement());
    });

    return menu;
  }

  private void setProjectStagesFilter(FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);
    /* int idxTaskState = form.getDataIndex(COL_STATUS); */
    int idxProjectStatus = form.getDataIndex(ALS_PROJECT_STATUS);

    if (BeeConst.isUndef(idxProjectOwner)) {
      return;
    }

    if (BeeConst.isUndef(idxProject)) {
      return;
    }
    TaskHelper.setWidgetEnabled(stagesSelector, false);

    if (BeeConst.isUndef(idxProjectStatus)) {
      return;
    }

    long currentUser = BeeUtils.unbox(BeeKeeper.getUser().getUserId());
    long projectOwner = BeeUtils.unbox(row.getLong(idxProjectOwner));
    long projectId = BeeUtils.unbox(row.getLong(idxProject));
    /* int state = BeeUtils.unbox(row.getInteger(idxTaskState)); */
    int projectStatus = BeeUtils.unbox(row.getInteger(idxProjectStatus));

    if (currentUser != projectOwner) {
      return;
    }

    if (ProjectStatus.APPROVED.ordinal() == projectStatus) {
      return;
    }
    TaskHelper.setSelectorFilter(stagesSelector, Filter.equals(ProjectConstants.COL_PROJECT,
        projectId));
    TaskHelper.setWidgetEnabled(stagesSelector, true);
  }

  private void setProjectUsers(List<Long> projectUsers) {
    this.projectUsers = projectUsers;
  }

  private void setProjectUsersFilter(final FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);
    int idxTaskStatus = form.getDataIndex(COL_STATUS);
    setProjectUsers(null);

    if (BeeConst.isUndef(idxProjectOwner) || BeeConst.isUndef(idxProject)
        || BeeConst.isUndef(idxTaskStatus)) {
      return;
    }

    final long projectOwner = BeeUtils.unbox(row.getLong(idxProjectOwner));
    long projectId = BeeUtils.unbox(row.getLong(idxProject));
    int taskStatus = BeeUtils.unbox(row.getInteger(idxTaskStatus));
    boolean validStatus = !TaskStatus.in(taskStatus, TaskStatus.APPROVED, TaskStatus.CANCELED,
        TaskStatus.COMPLETED, TaskStatus.SUSPENDED);

    boolean canChangeOwner = (isOwner() || BeeKeeper.getUser().isAdministrator())
        && validStatus;

    TaskHelper.setWidgetEnabled(ownerSelector, canChangeOwner);
    TaskHelper.setWidgetEnabled(observersSelector, isOwner());

    if (!DataUtils.isId(projectId)) {
      TaskHelper.setSelectorFilter(ownerSelector, null);
      TaskHelper.setSelectorFilter(observersSelector, null);
      return;
    }
    TaskHelper.setWidgetEnabled(observersSelector, isOwner()
        || (projectOwner == userId && validStatus));
    TaskHelper.setWidgetEnabled(ownerSelector, canChangeOwner
        || (projectOwner == userId && validStatus));

    Queries.getRowSet(ProjectConstants.VIEW_PROJECT_USERS, Lists
        .newArrayList(COL_USER), Filter.isEqual(
        ProjectConstants.COL_PROJECT, Value.getValue(projectId)), new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        List<Long> userIds = Lists.newArrayList(projectOwner);
        int idxUser = result.getColumnIndex(COL_USER);

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
        Filter projectTeamFilter = Filter.idIn(userIds);

        TaskHelper.setSelectorFilter(observersSelector, projectTeamFilter);
        TaskHelper.setSelectorFilter(ownerSelector, projectTeamFilter);
        setProjectUsers(userIds);
      }
    });
  }

  private void updateProjectInfo(String projectId, String stageId) {
    BeeRow newRow = getNewRow();

    newRow.setValue(getFormView().getDataIndex(ProjectConstants.COL_PROJECT), projectId);
    newRow.setValue(getFormView().getDataIndex(ProjectConstants.COL_PROJECT_STAGE), stageId);

    ParameterList params = createParams(TaskEvent.EDIT, newRow, null);
    sendRequest(params, TaskEvent.EDIT);
  }

  private void changeTaskOwner(SelectorEvent event) {
    if (!event.isChanged() || event.getRelatedRow() == null) {
      return;
    }

    FormView form = getFormView();

    if (form == null) {
      return;
    }

    IsRow row = form.getActiveRow();
    IsRow oldRow = form.getOldRow();

    if (row == null || DataUtils.isNewRow(form.getActiveRow())) {
      return;
    }

    Long newOwner = event.getValue();
    Boolean taskPrivate = row.getBoolean(form.getDataIndex(COL_PRIVATE_TASK));
    UserData userData = Global.getUsers().getUserData(newOwner);

    if (userData == null && oldRow != null) {
      resetField(form, oldRow, row, COL_OWNER);
      return;
    }

    if (!userData.canEditData(VIEW_TASKS)) {
      form.notifySevere(Localized.dictionary().crmTaskOwnerCanNotBe(userData.getLogin()));

      if (oldRow != null) {
        resetField(form, oldRow, row, COL_OWNER);
      }
    } else if (userData.canEditData(VIEW_TASKS) && BeeUtils.isTrue(taskPrivate) && oldRow != null) {
      Long oldOwner = oldRow.getLong(form.getDataIndex(COL_OWNER));
      addObserver(form, row, oldOwner);
    }

    form.refresh();
  }
}