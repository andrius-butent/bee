package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.FileGroup.Column;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.MutationEvent;
import com.butent.bee.client.event.logical.MutationEvent.Handler;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
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
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskEvent;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

  private static final String STYLE_EXTENSION = CRM_STYLE_PREFIX + "taskExtension";
  private static final String NAME_OBSERVERS = "Observers";
  private static final String NAME_PRIVATE_TASK = "PrivateTask";
  private Map<String, Pair<Long, String>> dbaParameters = Maps.newConcurrentMap();

  private static final String NAME_TASK_TREE = "TaskTree";
  private static final String NAME_ORDER = "TaskEventsOrder";

  private static final String DEFAULT_PHOTO_IMAGE = "images/defaultUser.png";

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
      Map<String, String> data = Codec.deserializeMap(value);

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
        url.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent arg0) {
            RowEditor.open(VIEW_TASKS, extTaskId, Opener.NEW_TAB);
          }
        });

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

  private static MultiSelector getMultiSelector(FormView form, String source) {
    Widget widget = form.getWidgetBySource(source);
    return (widget instanceof MultiSelector) ? (MultiSelector) widget : null;
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

  private static String getStorageKey(String name) {

    switch (name) {
      case NAME_TASK_TREE:
        return BeeUtils.join(BeeConst.STRING_MINUS, name, BeeKeeper.getUser().getUserId(),
            UiConstants.ATTR_SIZE);

      case NAME_ORDER:
        return BeeUtils.join(BeeConst.STRING_MINUS, name, BeeKeeper.getUser().getUserId());
    }
    return name;
  }

  private static String getTaskUsers(FormView form, IsRow row) {
    return DataUtils.buildIdList(TaskUtils.getTaskUsers(row, form.getDataColumns()));
  }

  private static boolean readBoolean(String name) {
    String key = getStorageKey(name);
    return BeeKeeper.getStorage().hasItem(key);
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
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TASKS);
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
    String photoUrl;

    Long photo = row.getLong(DataUtils.getColumnIndex(COL_PHOTO, columns));
    if (!DataUtils.isId(photo)) {
      photoUrl = DEFAULT_PHOTO_IMAGE;
    } else {
      photoUrl = PhotoRenderer.getUrl(photo);
    }

    Image image = new Image(photoUrl);
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
      row1.add(createEventCell(COL_PUBLISH_TIME,
          Format.getDefaultDateTimeFormat().format(publishTime)));
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
        createTask.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent arg0) {
            doCreate(row.getId(), taskRow);
          }
        });

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

      fileGroup.setDocCreator(new Consumer<FileInfo>() {

        @Override
        public void accept(FileInfo fileInfo) {
          createDocument(fileInfo, taskRow);
        }
      });
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
        row4.add(createEventCell(COL_DURATION_DATE, durDate.toCompactString()));
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
        && !readBoolean(NAME_ORDER)) {
      final Widget last = panel.getWidget(panel.getWidgetCount() - 1);
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          DomUtils.scrollIntoView(last.getElement());
        }
      });
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
        Label label = new Label(extensions.get(i).toCompactString());
        label.addStyleName(STYLE_EXTENSION);
        panel.add(label);
      }
    }
  }

  private final long userId;
  private MultiSelector observers;
  private List<Long> projectUsers;

  Split split;
  com.google.gwt.xml.client.Element west;
  IdentifiableWidget taskWidget;
  IdentifiableWidget taskEventsWidget;
  private boolean isDefaultLayout;

  TaskEditor() {
    super();
    this.isDefaultLayout = BeeKeeper.getUser().getCommentsLayout();
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.SAVE && maybeNotifyEmptyProduct(msg -> getFormView().notifySevere(msg))) {
      return false;
    }
    return true;
  }

  @Override
  public boolean beforeCreateWidget(String name, com.google.gwt.xml.client.Element description) {

    if (BeeUtils.same(name, "Split")) {
      Integer size = BeeKeeper.getStorage().getInteger(getStorageKey(NAME_TASK_TREE));

      if (BeeUtils.isPositive(size)) {

        west = XmlUtils.getFirstChildElement(description, Direction.WEST.name().toLowerCase());

        if (west != null) {
          west.setAttribute(UiConstants.ATTR_SIZE, size.toString());
        }
      }
    }
    return super.beforeCreateWidget(name, description);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, NAME_OBSERVERS) && widget instanceof MultiSelector) {
      observers = (MultiSelector) widget;
    } else if (BeeUtils.same(name, "TabbedPages") && widget instanceof TabbedPages) {
      taskWidget = widget;

    } else if (BeeUtils.same(name, "TaskEvents") && widget instanceof Flow) {
      taskEventsWidget = widget;

    } else if (BeeUtils.same(name, "Split") && widget instanceof Split) {
      split = (Split) widget;
      split.addMutationHandler(new Handler() {

        @Override
        public void onMutation(MutationEvent event) {
          int size = split.getDirectionSize(Direction.WEST);

          String key = getStorageKey(NAME_TASK_TREE);
          if (size > 0 && !BeeUtils.isEmpty(key)) {
            BeeKeeper.getStorage().set(key, size);
          }
        }
      });
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

    final FaLabel createDocument = new FaLabel(FontAwesome.FILE_O);
    createDocument.setTitle(Localized.dictionary().documentNew());
    createDocument.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        createDocument.setEnabled(false);
        ensureDefaultDBAParameters(createDocument, row);
      }
    });

    if (BeeKeeper.getUser().canCreateData(DocumentConstants.VIEW_DOCUMENTS)) {
      header.addCommandItem(createDocument);
    }

    for (final TaskEvent event : TaskEvent.values()) {

      if (!event.canExecute(getExecutor(), getOwner(), getObservers(), EnumUtils.getEnumByIndex(
          TaskStatus.class, status))) {
        continue;
      }
      String label = event.getCommandLabel();
      FontAwesome icon = event.getCommandIcon();

      IdentifiableWidget button = icon != null ? new FaLabel(icon) : new Button(label);

      ((HasClickHandlers) button).addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent e) {
          doEvent(event);
        }
      });

      if (button instanceof FaLabel) {
        ((FaLabel) button).setTitle(label);
      }

      if (!BeeUtils.isEmpty(label)) {
        header.addCommandItem(button);
      }
    }

    setProjectStagesFilter(form, row);
    setProjectUsersFilter(form, row);

    if (isExecutor()) {
      setEnabledRelations();
    }

    header.addCommandItem(setMenuLabel());
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (isExecutor()) {
      setEnabledRelations();
    }
    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TaskEditor();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return row != null && (BeeKeeper.getUser().is(row.getLong(getDataIndex(COL_OWNER)))
        || BeeKeeper.getUser().is(row.getLong(getDataIndex(COL_EXECUTOR))));
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
        && TaskUtils.getUpdatedRelations(oldRow, newRow).isEmpty()) {
      return;
    }

    ParameterList params = createParams(TaskEvent.EDIT, null);

    sendRequest(params, new RpcCallback<ResponseObject>() {
      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(TaskEvent.EDIT.getCaption(), result, this);

        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), ProjectConstants.VIEW_PROJECT_STAGES);
        if (data != null) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);

          if (TaskUtils.hasRelations(oldRow) || TaskUtils.hasRelations(data)) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
          }
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

    form.setEnabled(Objects.equals(owner, userId));

    TaskStatus newStatus = oldStatus;

    if (TaskStatus.NOT_VISITED.equals(oldStatus) && Objects.equals(executor, userId)) {
      newStatus = TaskStatus.VISITED;
    }

    BeeRow visitedRow = DataUtils.cloneRow(row);
    if (newStatus != null) {
      visitedRow.preliminaryUpdate(form.getDataIndex(COL_STATUS),
          BeeUtils.toString(newStatus.ordinal()));
    }

    if (readBoolean(NAME_ORDER)) {
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
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(TaskEvent.VISIT.getCaption(), result, this);
        if (data == null) {
          return;
        }

        RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);
        if (TaskUtils.hasRelations(data)) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
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
            ((FileGroup) fileWidget).setDocCreator(new Consumer<FileInfo>() {

              @Override
              public void accept(FileInfo fileInfo) {
                createDocument(fileInfo, row);
              }
            });
          }
        }

        if (!getEvents(data).isEmpty()) {
          showEventsAndDuration(form, data, getEvents(data), files, lastAccess);
        }

        form.updateRow(data, true);
      }
    });
    return false;
  }

  private static void setProjectStagesFilter(FormView form, IsRow row) {
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

    Widget wProjectStage = form.getWidgetBySource(ProjectConstants.COL_PROJECT_STAGE);

    if (wProjectStage instanceof DataSelector) {
      ((DataSelector) wProjectStage).setEnabled(false);
    } else {
      return;
    }

    /*
     * if (BeeConst.isUndef(idxTaskState)) { return; }
     */

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

    /*
     * if (TaskStatus.SCHEDULED.ordinal() != state) { return; }
     */

    if (ProjectStatus.APPROVED.ordinal() == projectStatus) {
      return;
    }

    ((DataSelector) wProjectStage).getOracle().setAdditionalFilter(
        Filter.equals(ProjectConstants.COL_PROJECT, projectId), true);
    ((DataSelector) wProjectStage).setEnabled(true);

  }

  public void createDocument(final FileInfo fileInfo, final IsRow row) {
    createDocument(fileInfo, row, false);
  }

  public void createDocument(final FileInfo fileInfo, final IsRow row,
      final boolean enableTemplates) {

    final DataInfo dataInfo = Data.getDataInfo(DocumentConstants.VIEW_DOCUMENTS);
    final BeeRow docRow = RowFactory.createEmptyRow(dataInfo, true);
    final boolean ensureEnableTemplate = enableTemplates && dbaParameters != null;

    if (docRow != null) {

      int idxCompanyName = Data.getColumnIndex(VIEW_TASKS, ALS_COMPANY_NAME);
      int idxCompany = Data.getColumnIndex(VIEW_TASKS, COL_TASK_COMPANY);

      if (!BeeConst.isUndef(idxCompanyName) && !BeeConst.isUndef(idxCompany)) {
        String companyName = row.getString(idxCompanyName);

        if (!BeeUtils.isEmpty(companyName)) {
          docRow.setValue(dataInfo
              .getColumnIndex(DocumentConstants.ALS_DOCUMENT_COMPANY_NAME),
              companyName);
          docRow.setValue(dataInfo
              .getColumnIndex(DocumentConstants.COL_DOCUMENT_COMPANY), row
              .getLong(idxCompany));
          docRow.setValue(dataInfo
              .getColumnIndex(DocumentConstants.COL_DOCUMENT_DATE), new DateTime(new JustDate()));
        }

        FileCollector.pushFiles(Lists.newArrayList(fileInfo));

        if (ensureEnableTemplate && dbaParameters.containsKey(PRM_DEFAULT_DBA_DOCUMENT_TYPE)) {
          Pair<Long, String> defaultDBAType = dbaParameters.get(PRM_DEFAULT_DBA_DOCUMENT_TYPE);
          docRow.setValue(dataInfo.getColumnIndex(DocumentConstants.COL_DOCUMENT_TYPE),
              defaultDBAType.getA());
          docRow.setValue(dataInfo.getColumnIndex(DocumentConstants.ALS_TYPE_NAME),
              defaultDBAType.getB());
        }

        if (ensureEnableTemplate && dbaParameters.containsKey(PRM_DEFAULT_DBA_TEMPLATE)) {
          Pair<Long, String> defaultDBATemplate = dbaParameters.get(PRM_DEFAULT_DBA_TEMPLATE);
          docRow.setProperty(PRM_DEFAULT_DBA_TEMPLATE,
              BeeUtils.toString(defaultDBATemplate.getA()));
        }

        RowFactory.createRow(dataInfo, docRow, Modality.ENABLED, new RowCallback() {

          @Override
          public void onSuccess(final BeeRow br) {
            RowInsertEvent.fire(BeeKeeper.getBus(), DocumentConstants.VIEW_DOCUMENTS, br, null);
            MultiSelector sel = getMultiSelector(getFormView(), PROP_DOCUMENTS);

            if (sel != null) {
              List<MultiSelector.Choice> val = sel.getChoices();
              val.add(new MultiSelector.Choice(br.getId()));
              sel.setChoices(val);

            }

            BeeRow newRow = getNewRow();
            List<Long> idList = DataUtils.parseIdList(newRow.getProperty(PROP_DOCUMENTS));
            idList.add(br.getId());

            newRow.setProperty(PROP_DOCUMENTS, DataUtils.buildIdList(idList));

            ParameterList prm = createParams(TaskEvent.EDIT, newRow,
                TaskUtils.getInsertNote(Localized.dictionary().document(),
                    BeeUtils.joinWords(
                        br.getString(Data.getColumnIndex(DocumentConstants.VIEW_DOCUMENTS,
                            DocumentConstants.COL_DOCUMENT_NAME)),
                        br.getString(Data.getColumnIndex(DocumentConstants.VIEW_DOCUMENTS,
                            DocumentConstants.COL_REGISTRATION_NUMBER)),
                        br.getDateTime(Data.getColumnIndex(DocumentConstants.VIEW_DOCUMENTS,
                            DocumentConstants.COL_DOCUMENT_DATE))
                        )));
            sendRequest(prm, TaskEvent.EDIT);
          }
        });
      }
    }
  }

  private static BeeRowSet getEvents(IsRow row) {
    if (!BeeUtils.isEmpty(row.getProperty(PROP_EVENTS))) {
      return BeeRowSet.restore(row.getProperty(PROP_EVENTS));
    }

    return Data.createRowSet(VIEW_TASK_EVENTS);
  }

  private void createCellValidationHandler(FormView form, IsRow row) {

    if (form == null || row == null) {
      return;
    }

    form.addCellValidationHandler(NAME_PRIVATE_TASK, new CellValidateEvent.Handler() {

      @Override
      public Boolean validateCell(CellValidateEvent event) {
        if (isOwner()) {
          return true;
        }
        return false;
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

    if (readBoolean(NAME_ORDER)) {
      newRow.setProperty(PROP_DESCENDING, BeeConst.INT_TRUE);
    } else {
      newRow.removeProperty(PROP_DESCENDING);
    }

    BeeRowSet rowSet = new BeeRowSet(viewName, form.getDataColumns());
    rowSet.addRow(newRow);

    ParameterList params = TasksKeeper.createTaskRequestParameters(event);
    params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));
    params.addDataItem(VAR_TASK_USERS, getTaskUsers(form, oldRow));

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

  private void createRelations(final IsRow taskRow, final Long eventId,
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
            data = Codec.deserializeMap(result);
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

    List<Long> relIds = DataUtils.parseIdList(taskRow.getProperty(PROP_TASKS));
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

  private void doApprove() {
    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskConfirmation());

    final String did = dialog.addDateTime(Localized.dictionary().crmTaskConfirmDate(), true,
        TimeUtils.nowMinutes());
    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.dictionary().crmTaskConfirm(), new ScheduledCommand() {
      @Override
      public void execute() {

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
      }
    });

    dialog.display();
    dialog.focusOnOpen(DomUtils.getWidget(cid));
  }

  private void doCancel() {
    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskCancellation());

    final String cid = dialog.addComment(true);

    dialog.addAction(Localized.dictionary().crmTaskCancel(), new ScheduledCommand() {
      @Override
      public void execute() {

        String comment = dialog.getComment(cid);
        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.dictionary().crmEnterComment());
          return;
        }

        ParameterList params = createParams(TaskEvent.CANCEL, getNewRow(TaskStatus.CANCELED),
            comment);

        sendRequest(params, TaskEvent.CANCEL);
        dialog.close();
      }
    });

    dialog.display();
  }

  private void doComment() {
    final TaskDialog dialog =
        new TaskDialog(Localized.dictionary().crmTaskCommentTimeRegistration());

    final String cid = dialog.addComment(false);
    final String fid = dialog.addFileCollector();
    Map<String, String> durIds = setDurations(dialog);

    dialog.addAction(Localized.dictionary().actionSave(), new ScheduledCommand() {
      @Override
      public void execute() {

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

    dialog.addAction(Localized.dictionary().crmActionFinish(), new ScheduledCommand() {
      @Override
      public void execute() {

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
    int idxPriority = newTaskInfo.getColumnIndex(COL_PRIORITY);
    int idxType = newTaskInfo.getColumnIndex(COL_TASK_TYPE);
    int idxTypeName = newTaskInfo.getColumnIndex(ALS_TASK_TYPE_NAME);
    int idxCompany = newTaskInfo.getColumnIndex(COL_COMPANY);
    int idxCompanyName = newTaskInfo.getColumnIndex(ALS_COMPANY_NAME);
    int idxCompanyType = newTaskInfo.getColumnIndex(ALS_COMPANY_TYPE_NAME);
    int idxContact = newTaskInfo.getColumnIndex(COL_CONTACT);
    int idxContactFirstName = newTaskInfo.getColumnIndex(TaskConstants.ALS_CONTACT_FIRST_NAME);
    int idxContactLastName = newTaskInfo.getColumnIndex(TaskConstants.ALS_CONTACT_LAST_NAME);

    int idxProject = newTaskInfo.getColumnIndex(ProjectConstants.COL_PROJECT);
    int idxProjectStage = newTaskInfo.getColumnIndex(ProjectConstants.COL_PROJECT_STAGE);

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

    newTaskRow.setValue(idxPriority, taskRow
        .getInteger(idxPriority));

    newTaskRow.setValue(idxType, taskRow
        .getLong(idxType));

    newTaskRow.setValue(idxTypeName, taskRow
        .getString(idxTypeName));

    newTaskRow.setValue(idxCompany, taskRow
        .getLong(idxCompany));

    newTaskRow.setValue(idxCompanyName, taskRow
        .getString(idxCompanyName));

    newTaskRow.setValue(idxCompanyType, taskRow
        .getLong(idxCompanyType));

    newTaskRow.setValue(idxContact, taskRow
        .getLong(idxContact));

    newTaskRow.setValue(idxContactFirstName, taskRow
        .getString(idxContactFirstName));

    newTaskRow.setValue(idxContactLastName, taskRow
        .getString(idxContactLastName));

    newTaskRow.setValue(idxProject, taskRow
        .getLong(idxProject));

    newTaskRow.setValue(idxProjectStage, taskRow
        .getLong(idxProjectStage));

    Map<Long, FileInfo> files = Maps.newLinkedHashMap();

    for (FileInfo file : TaskUtils.getFiles(taskRow)) {
      files.put(file.getId(), file);
    }

    RowFactory.createRow(newTaskInfo.getNewRowForm(), newTaskInfo.getNewRowCaption(), newTaskInfo,
        newTaskRow, Modality.ENABLED, null, new TaskBuilder(files, null, true), null,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            createRelations(taskRow, eventId, result.getString(0));
          }
        });
  }

  private void doEvent(TaskEvent event) {

    if (!event.canExecute(getExecutor(), getOwner(), getObservers(), EnumUtils.getEnumByIndex(
        TaskStatus.class, getStatus()))) {
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

    final String cid = dialog.addComment(false);

    dialog.addAction(Localized.dictionary().crmTaskChangeTerm(), new ScheduledCommand() {
      @Override
      public void execute() {

        DateTime newStart = getDateTime(COL_START_TIME);
        DateTime oldEnd = getDateTime(COL_FINISH_TIME);

        DateTime newEnd = dialog.getDateTime(endId);

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
                  + " " + now.toCompactString()));
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
      }
    });

    dialog.display(endId);
  }

  private void doForward() {
    final Long oldUser = getExecutor();
    Set<Long> exclusions = new HashSet<>();
    //Set<Long> filter = new HashSet<>();
    if (oldUser != null) {
      exclusions.add(oldUser);
    }

/*    if (!BeeUtils.isEmpty(getProjectUsers())) {
      filter.addAll(getProjectUsers());
    }*/

    final TaskDialog dialog = new TaskDialog(Localized.dictionary().crmTaskForwarding());

    final String sid =
        dialog.addSelector(Localized.dictionary().crmTaskExecutor(), VIEW_USERS,
            Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME),
            true, exclusions, null, null);
    final String obs = dialog.addCheckBox(true);

    final String cid = dialog.addComment(true);
    final String fid = dialog.addFileCollector();

    dialog.addAction(Localized.dictionary().crmActionForward(), new ScheduledCommand() {
      @Override
      public void execute() {

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
        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.dictionary().crmEnterComment());
          return;
        }

        BeeRow newRow = getNewRow();
        RelationUtils.updateRow(Data.getDataInfo(VIEW_TASKS), COL_EXECUTOR, newRow,
            Data.getDataInfo(VIEW_USERS), selector.getRelatedRow(), true);

        TaskStatus newStatus = TaskStatus.NOT_VISITED;

        /** Forward task itself */
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
      }
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

    dialog.addAction(Localized.dictionary().crmTaskReturnExecution(), new ScheduledCommand() {
      @Override
      public void execute() {

        TaskStatus newStatus = isExecutor() ? TaskStatus.VISITED : TaskStatus.NOT_VISITED;

        BeeRow newRow = getNewRow(newStatus);
        newRow.clearCell(getFormView().getDataIndex(COL_COMPLETED));
        newRow.clearCell(getFormView().getDataIndex(COL_APPROVED));

        ParameterList params = createParams(TaskEvent.RENEW, newRow, dialog.getComment(cid));

        sendRequest(params, TaskEvent.RENEW);
        dialog.close();
      }
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

    dialog.addAction(Localized.dictionary().crmActionSuspend(), new ScheduledCommand() {
      @Override
      public void execute() {

        String comment = dialog.getComment(cid);
        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.dictionary().crmEnterComment());
          return;
        }

        ParameterList params = createParams(TaskEvent.SUSPEND, getNewRow(TaskStatus.SUSPENDED),
            comment);

        sendRequest(params, TaskEvent.SUSPEND);
        dialog.close();
      }
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

  private static FaLabel getOrderLabelInfo() {
    FaLabel label =
        readBoolean(NAME_ORDER) ? new FaLabel(FontAwesome.SORT_NUMERIC_ASC) : new FaLabel(
            FontAwesome.SORT_NUMERIC_DESC);
    label.setTitle(readBoolean(NAME_ORDER) ? Localized.dictionary().crmTaskCommentsAsc()
        : Localized.dictionary().crmTaskCommentsDesc());

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
              widget.setEnabled(true);
            }
          }
        };

    Global.getRelationParameter(PRM_DEFAULT_DBA_TEMPLATE, new BiConsumer<Long, String>() {
      @Override
      public void accept(Long id, String name) {
        paramHolder.accept(PRM_DEFAULT_DBA_TEMPLATE, Pair.of(id, name));
      }
    });

    Global.getRelationParameter(PRM_DEFAULT_DBA_DOCUMENT_TYPE, new BiConsumer<Long, String>() {
      @Override
      public void accept(Long id, String name) {
        paramHolder.accept(PRM_DEFAULT_DBA_DOCUMENT_TYPE, Pair.of(id, name));
      }
    });
  }

  private DateTime getDateTime(String colName) {
    return getFormView().getActiveRow().getDateTime(getFormView().getDataIndex(colName));
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

  private List<Long> getObservers() {
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

  // private boolean isObserver() {
  // return getObservers().contains(userId);
  // }

  private boolean isOwner() {
    return Objects.equals(userId, getOwner());
  }

  private void onResponse(BeeRow data) {
    RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, data);

    FormView form = getFormView();
    Long lastAccess = BeeUtils.toLongOrNull(data.getProperty(PROP_LAST_ACCESS,
        BeeKeeper.getUser().getUserId()));

    if (TaskUtils.hasRelations(form.getOldRow()) || TaskUtils.hasRelations(data)) {
      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_TASKS);
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
    params.addDataItem(VAR_TASK_RELATIONS, BeeConst.STRING_ASTERISK);

    String properties =
        BeeUtils.join(BeeConst.STRING_COMMA, PROP_OBSERVERS, PROP_FILES, PROP_EVENTS);

    if (readBoolean(NAME_ORDER)) {
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

  private void sendFiles(final List<FileInfo> files, final long taskId, final long teId) {

    final Holder<Integer> counter = Holder.of(0);

    final List<BeeColumn> columns = Data.getColumns(VIEW_TASK_FILES,
        Lists.newArrayList(COL_TASK, COL_TASK_EVENT, COL_FILE, COL_CAPTION));

    for (final FileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, new Callback<Long>() {
        @Override
        public void onSuccess(Long result) {
          List<String> values = Lists.newArrayList(BeeUtils.toString(taskId),
              BeeUtils.toString(teId), BeeUtils.toString(result), fileInfo.getCaption());

          Queries.insert(VIEW_TASK_FILES, columns, values, null, new RowCallback() {
            @Override
            public void onSuccess(BeeRow row) {
              counter.set(counter.get() + 1);
              if (counter.get() == files.size()) {
                requeryEvents(taskId);
              }
            }
          });
        }
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
    final String durId = dialog.addTime(Localized.dictionary().crmSpentTime());
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
    if (isDefaultLayout) {
      split.remove(taskWidget);
      split.remove(taskEventsWidget);
      split.addNorth(taskWidget, 575);
      split.updateCenter(taskEventsWidget);

    } else {
      Integer size = BeeKeeper.getStorage().getInteger(getStorageKey(NAME_TASK_TREE));
      split.remove(taskWidget);
      split.remove(taskEventsWidget);
      split.addWest(taskWidget, size == null ? 650 : size);
      StyleUtils.autoHeight(taskWidget.getElement());
      split.updateCenter(taskEventsWidget);
    }
  }

  private FaLabel setMenuLabel() {
    FaLabel menu = new FaLabel(FontAwesome.NAVICON);
    menu.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        final HtmlTable tb = new HtmlTable(BeeConst.CSS_CLASS_PREFIX + "GridMenu-table");
        FaLabel commentLbl = getCommentsLabelInfo();
        FaLabel orderLbl = getOrderLabelInfo();

        tb.setWidget(0, 0, commentLbl);
        tb.setText(0, 1, commentLbl.getTitle());
        tb.setWidget(1, 0, orderLbl);
        tb.setText(1, 1, orderLbl.getTitle());

        tb.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent ev) {
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
                if (readBoolean(NAME_ORDER)) {
                  BeeKeeper.getStorage().remove(getStorageKey(NAME_ORDER));
                  getActiveRow().removeProperty(PROP_DESCENDING);
                } else {
                  BeeKeeper.getStorage().set(getStorageKey(NAME_ORDER), true);
                  getActiveRow().setProperty(PROP_DESCENDING, BeeConst.INT_TRUE);
                }

                UiHelper.closeDialog(tb);
                doEvent(TaskEvent.REFRESH);
                break;

              default:
            }
          }
        });

        Popup popup = new Popup(OutsideClick.CLOSE, BeeConst.CSS_CLASS_PREFIX + "GridMenu-popup");
        popup.setWidget(tb);
        popup.setHideOnEscape(true);
        popup.showRelativeTo(menu.getElement());
      }
    });

    return menu;
  }

  private void setProjectUsers(List<Long> projectUsers) {
    this.projectUsers = projectUsers;
  }

  private void setProjectUsersFilter(final FormView form, IsRow row) {
    int idxProjectOwner = form.getDataIndex(ALS_PROJECT_OWNER);
    int idxProject = form.getDataIndex(ProjectConstants.COL_PROJECT);

    setProjectUsers(null);

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

    if (observers != null) {
      observers.setEnabled(false);
    }

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

        if (observers != null) {
          observers.getOracle().setAdditionalFilter(Filter.idIn(userIds), true);
          observers.setEnabled(true);
        }

        setProjectUsers(userIds);
      }
    });
  }

  private void setEnabledRelations() {
    for (String relation : TaskUtils.TASK_RELATIONS) {
      MultiSelector selector = getMultiSelector(getFormView(), relation);
      if (selector != null) {
        selector.setEnabled(true);
      }
    }
  }
}
