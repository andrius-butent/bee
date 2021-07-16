package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.SimpleEditorHandler;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TaskDialog extends CustomTaskDialog {

  private static final String STYLE_DIALOG = CRM_STYLE_PREFIX + "taskDialog";
  private static final String STYLE_CELL = "Cell";

  private Label durationType;

  TaskDialog(String caption) {
    super(caption, STYLE_DIALOG);
    addDefaultCloseBox();

    HtmlTable container = new HtmlTable();
    container.addStyleName(STYLE_DIALOG + "-container");

    setWidget(container);
  }

  void addAction(String caption, final ScheduledCommand command) {
    String styleName = STYLE_DIALOG + "-action";

    Button button = new Button(caption, command);
    button.addStyleName(styleName);

    FaLabel faSave = new FaLabel(FontAwesome.SAVE);
    faSave.addClickHandler(arg0 -> command.execute());

    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    UiHelper.initActionWidget(Action.SAVE, faSave);

    insertAction(BeeConst.INT_TRUE, faSave);

    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.CENTER);

    table.getCellFormatter().setColSpan(row, col, 2);
  }

  String addComment(boolean required) {
    String styleName = STYLE_DIALOG + "-commentLabel";
    Label label = new Label(Localized.dictionary().crmTaskComment());
    label.addStyleName(styleName);
    if (required) {
      label.addStyleName(StyleUtils.NAME_REQUIRED);
    }

    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    InputArea input = new InputArea();
    styleName = STYLE_DIALOG + "-commentArea";
    input.addStyleName(styleName);

    table.setWidget(row, col, input);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    table.getCellFormatter().setColSpan(row, col, 2);

    return input.getId();
  }

  String addDateTime(String caption, boolean required, DateTime def) {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-dateLabel";
    Label label = new Label(caption);
    label.addStyleName(styleName);
    if (required) {
      label.addStyleName(StyleUtils.NAME_REQUIRED);
    }

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    styleName = STYLE_DIALOG + "-dateInput";
    InputDateTime input = new InputDateTime();
    input.addStyleName(styleName);

    if (def != null) {
      input.setDateTime(def);
    }

    SimpleEditorHandler.observe(caption, input);

    table.setWidget(row, col, input);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return input.getId();
  }

  Map<String, String> addDuration() {
    Map<String, String> result = new HashMap<>();

    result.put(COL_DURATION, addTime(Localized.dictionary().crmSpentTime(), false));
    result.put(COL_DURATION_TYPE, addSelector(Localized.dictionary().crmDurationType(),
        VIEW_DURATION_TYPES, Lists.newArrayList(COL_DURATION_TYPE_NAME), false, null, null, null));

    return result;
  }

  String addFileCollector() {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-filesLabel";
    Label label = new Label(Localized.dictionary().files());
    label.addStyleName(styleName);

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    styleName = STYLE_DIALOG + "-fileCollector";
    // FileCollector collector = new FileCollector(new Image(Global.getImages().attachment()));
    IdentifiableWidget identifiableWidget = new Button(Localized.dictionary().chooseFiles());
    FileCollector collector = new FileCollector(identifiableWidget);
    collector.addStyleName(styleName);

    table.setWidget(row, col, collector);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    Widget panel = getWidget();
    if (panel instanceof DndTarget) {
      collector.bindDnd((DndTarget) panel);
    }

    return collector.getId();
  }

  String addCheckBox(boolean checked) {
    HtmlTable table = getContainer();
    int row = table.getRowCount() - 1;
    int col = 2;

    String styleName = STYLE_DIALOG + "-observerCheckbox";
    CheckBox chkBx = new CheckBox(Localized.dictionary().crmTaskAddSenderToObservers());
    chkBx.setChecked(checked);
    chkBx.addStyleName(styleName);

    table.setWidget(row, col, chkBx);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return chkBx.getId();
  }

  String addTaskReturnCheckBox() {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 2;

    CheckBox chkBx = new CheckBox(Localized.dictionary().taskReturnedTask());
    chkBx.setChecked(false);

    table.setWidget(row, col, chkBx);

    return chkBx.getId();
  }

  Map<String, String> addEndResult(Map<String, MultiSelector> widgets, List<String> endResults,
      String fileId) {
    if (BeeUtils.isEmpty(endResults)) {
      return null;
    }

    Map<String, String> multiIds = new HashMap<>();

    HtmlTable table = getContainer();
    int row = table.getRowCount();
    String styleName = STYLE_DIALOG + "-resultLabel";

    if (endResults.contains(VIEW_TASK_FILES)) {
      Element fileLabel = Selectors.getElementByClassName(getContent(),
          STYLE_DIALOG + "-filesLabel");

      if (fileLabel != null) {
        fileLabel.addClassName(StyleUtils.NAME_REQUIRED);
        multiIds.put(VIEW_TASK_FILES, fileId);
      }
    }

    for (String viewName : widgets.keySet()) {
      if (endResults.contains(viewName)) {
        Label label = new Label(Data.getViewCaption(viewName));
        label.addStyleName(styleName);
        label.addStyleName(StyleUtils.NAME_REQUIRED);

        table.setWidget(row, 0, label);
        table.getCellFormatter().addStyleName(row, 0, styleName + STYLE_CELL);

        MultiSelector multi = widgets.get(viewName);
        List<Long> ids = multi.getIds();

        Relation relation = Relation.create(viewName, multi.getChoiceColumns());

        MultiSelector newMulti = MultiSelector.autonomous(relation, multi.getRenderer());
        StyleUtils.setWidth(newMulti, 100, CssUnit.PCT);

        if (!BeeUtils.isEmpty(ids)) {
          newMulti.setIds(ids);
          newMulti.setValues(multi.getValues());
          newMulti.setChoices(multi.getChoices());
        }

        table.setWidget(row, 1, newMulti);
        multiIds.put(viewName, newMulti.getId());
        row++;
      }
    }

    return multiIds;
  }

  String addSelector(String caption, String relView, List<String> relColumns,
      boolean required, Collection<Long> exclusions, Collection<Long> filter, String valueSource) {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-selectorLabel";
    durationType = new Label(caption);
    durationType.addStyleName(styleName);
    if (required) {
      durationType.addStyleName(StyleUtils.NAME_REQUIRED);
    }

    table.setWidget(row, col, durationType);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    Relation relation = Relation.create(relView, relColumns);

    if (!BeeUtils.isEmpty(valueSource)) {
      relation.setValueSource(valueSource);
    }

    styleName = STYLE_DIALOG + "-selectorInput";
    UnboundSelector selector = UnboundSelector.create(relation);
    selector.addStyleName(styleName);

    if (!BeeUtils.isEmpty(exclusions)) {
      selector.getOracle().setExclusions(exclusions);
    }

    if (!BeeUtils.isEmpty(filter)) {
      selector.getOracle().setAdditionalFilter(Filter.idIn(filter), true);
    } else {
      selector.getOracle().setAdditionalFilter(null, true);
    }

    table.setWidget(row, col, selector);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return selector.getId();
  }

  String addTime(String caption, boolean required) {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-timeLabel";
    Label label = new Label(caption);
    label.addStyleName(styleName);

    if (required) {
      label.addStyleName(StyleUtils.NAME_REQUIRED);
    }

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    styleName = STYLE_DIALOG + "-timeInput";
    InputTime input = new InputTime();
    input.addStyleName(styleName);

    input.addEditStopHandler(event -> durationType.setStyleName(StyleUtils.NAME_REQUIRED, true));

    input.addValueChangeHandler(
        event1 -> durationType.setStyleName(StyleUtils.NAME_REQUIRED, !BeeUtils.isEmpty(
            event1.getValue())));

    SimpleEditorHandler.observe(caption, input);

    table.setWidget(row, col, input);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return input.getId();
  }

  void display() {
    focusOnOpen(getContent());
    center();
  }

  void display(String focusId) {
    focusOnOpen(getChild(focusId));
    center();
  }

  String getComment(String id) {
    Widget child = getChild(id);
    if (child instanceof InputArea) {
      return ((InputArea) child).getValue();
    } else {
      return null;
    }
  }

  DateTime getDateTime(String id) {
    Widget child = getChild(id);
    if (child instanceof InputDateTime) {
      return ((InputDateTime) child).getDateTime();
    } else {
      return null;
    }
  }

  InputDateTime getInputDateTime(String id) {
    Widget child = getChild(id);
    if (child instanceof InputDateTime) {
      return (InputDateTime) child;
    } else {
      return null;
    }
  }

  List<FileInfo> getFiles(String id) {
    Widget child = getChild(id);
    if (child instanceof FileCollector) {
      return ((FileCollector) child).getFiles();
    } else {
      return new ArrayList<>();
    }
  }

  MultiSelector getRelation(String id) {
    Widget child = getChild(id);
    if (child instanceof MultiSelector) {
      return (MultiSelector) child;
    } else {
      return null;
    }
  }

  DataSelector getSelector(String id) {
    Widget child = getChild(id);
    if (child instanceof DataSelector) {
      return (DataSelector) child;
    } else {
      return null;
    }
  }

  String getTime(String id) {
    Widget child = getChild(id);
    if (child instanceof InputTime) {
      return ((InputTime) child).getNormalizedValue();
    } else {
      return null;
    }
  }

  boolean isChecked(String id) {
    Widget child = getChild(id);
    if (child instanceof CheckBox) {
      return ((CheckBox) child).isChecked();
    } else {
      return false;
    }
  }

  Widget getChild(String id) {
    return DomUtils.getChildQuietly(getContent(), id);
  }

  /**
   * Verslo Aljansas TID 25505.
   */
  @Override
  protected HtmlTable getContainer() {
    return (HtmlTable) getContent();
  }
}
