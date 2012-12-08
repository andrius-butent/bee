package com.butent.bee.client.dialog;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.dialog.Popup.Modality;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.widget.BeeButton;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.Html;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.IsTable;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

/**
 * Implements a message box user interface component, sending text messages to the user.
 */

public class MessageBoxes {

  private static final BeeLogger logger = LogUtils.getLogger(MessageBoxes.class);

  private static final String STYLE_CHOICE_DIALOG = "bee-ChoiceDialog";
  private static final String STYLE_CHOICE_PANEL = "bee-ChoicePanel";
  private static final String STYLE_CHOICE_PROMPT = "bee-ChoicePrompt";
  private static final String STYLE_CHOICE_CONTAINER = "bee-ChoiceContainer";
  private static final String STYLE_CHOICE_GROUP = "bee-ChoiceGroup-";
  private static final String STYLE_CHOICE_DEFAULT = "bee-ChoiceDefault";
  private static final String STYLE_CHOICE_CANCEL = "bee-ChoiceCancel";

  private static final String STYLE_CONFIRM_CONTAINER = "bee-ConfirmContainer";
  private static final String STYLE_CONFIRM_MESSAGE = "bee-ConfirmMessage";

  private static final String STYLE_DECISION_DIALOG = "bee-DecisionDialog";
  private static final String STYLE_DECISION_PANEL = "bee-DecisionPanel";
  private static final String STYLE_DECISION_ICON = "bee-DecisionIcon";
  private static final String STYLE_DECISION_MESSAGE = "bee-DecisionMessage";
  private static final String STYLE_DECISION_GROUP = "bee-DecisionGroup";
  private static final String STYLE_DECISION_OPTION = "bee-DecisionOption";

  private static final String STYLE_TABLE_CONTAINER = "bee-ModalTableContainer";
  private static final String STYLE_TABLE = "bee-ModalTable";

  private static final String STYLE_STAR_PICKER = "bee-StarPicker";
  private static final String STYLE_STAR_CLUSTER = "bee-StarCluster-";

  private static final String STYLE_ERROR_DIALOG = "bee-ErrorDialog";
  private static final String STYLE_ERROR_PANEL = "bee-ErrorPanel";
  private static final String STYLE_ERROR_ICON = "bee-ErrorIcon";
  private static final String STYLE_ERROR_MESSAGE = "bee-ErrorMessage";
  private static final String STYLE_ERROR_CLOSE = "bee-ErrorClose";

  private static final String CELL_STYLE_SUFFIX = "-cell";

  private static final int CHOICE_MAX_HORIZONTAL_ITEMS = 10;
  private static final int CHOICE_MAX_HORIZONTAL_CHARS = 100;

  public void alert(String... lines) {
    Assert.notNull(lines);
    Assert.parameterCount(lines.length, 1);
    Window.alert(BeeUtils.buildLines(lines));
  }

  public void choice(String caption, String prompt, List<String> options,
      final ChoiceCallback callback, final int defaultValue, final int timeout,
      String cancelHtml, WidgetInitializer initializer) {
    Assert.notEmpty(options);
    Assert.notNull(callback);

    final Holder<State> state = Holder.of(State.OPEN);

    final DialogBox dialog = DialogBox.create(caption, STYLE_CHOICE_DIALOG);
    UiHelper.initialize(dialog, initializer, DialogConstants.WIDGET_DIALOG);

    final Timer timer = (timeout > 0) ? new DialogTimer(dialog, state) : null;

    Panel panel = new Flow();
    panel.addStyleName(STYLE_CHOICE_PANEL);

    if (!BeeUtils.isEmpty(prompt)) {
      BeeLabel label = new BeeLabel(prompt.trim());
      label.addStyleName(STYLE_CHOICE_PROMPT);

      UiHelper.add(panel, label, initializer, DialogConstants.WIDGET_PROMPT);
    }

    int size = options.size();

    boolean vertical = size > CHOICE_MAX_HORIZONTAL_ITEMS;
    if (!vertical && size > 1) {
      int len = 0;
      for (String option : options) {
        len += option.trim().length();
      }
      vertical = len > CHOICE_MAX_HORIZONTAL_CHARS;
    }

    TabBar group = new TabBar(STYLE_CHOICE_GROUP,
        vertical ? Orientation.VERTICAL : Orientation.HORIZONTAL);

    for (int i = 0; i < size; i++) {
      Widget widget = UiHelper.initialize(new Html(options.get(i)), initializer,
          DialogConstants.WIDGET_COMMAND_ITEM);
      group.addItem(widget);
    }

    final Holder<Integer> cancelIndex = Holder.absent();

    if (!BeeUtils.isEmpty(cancelHtml)) {
      Widget widget = UiHelper.initialize(new Html(cancelHtml), initializer,
          DialogConstants.WIDGET_CANCEL);
      if (widget != null) {
        widget.addStyleName(STYLE_CHOICE_CANCEL);
        group.addItem(widget);
        cancelIndex.set(group.getItemCount() - 1);
      }
    }

    Flow container = new Flow();
    container.addStyleName(STYLE_CHOICE_CONTAINER);
    container.addStyleName(STYLE_CHOICE_CONTAINER + BeeConst.STRING_MINUS
        + (vertical ? StyleUtils.NAME_VERTICAL : StyleUtils.NAME_HORIZONTAL));

    UiHelper.add(container, group, initializer, DialogConstants.WIDGET_COMMAND_GROUP);
    panel.add(container);

    final Holder<Integer> selectedIndex = Holder.absent();

    group.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        selectedIndex.set(event.getSelectedItem());
        dialog.hide();
      }
    });

    dialog.setHideOnEscape(true);

    dialog.addCloseHandler(new CloseHandler<Popup>() {
      public void onClose(CloseEvent<Popup> event) {
        if (timer != null) {
          timer.cancel();
        }

        if (selectedIndex.isNotNull() && !cancelIndex.contains(selectedIndex.get())) {
          callback.onSuccess(selectedIndex.get());
        } else if (State.EXPIRED.equals(state.get())) {
          callback.onTimeout();
        } else {
          callback.onCancel();
        }
      }
    });

    UiHelper.setWidget(dialog, panel, initializer, DialogConstants.WIDGET_PANEL);

    dialog.setAnimationEnabled(true);
    dialog.center();

    int focusIndex;

    if (group.isIndex(defaultValue)) {
      group.getTabWidget(defaultValue).addStyleName(STYLE_CHOICE_DEFAULT);
      group.selectTab(defaultValue, false);
      focusIndex = defaultValue;
    } else if (cancelIndex.isNotNull()) {
      focusIndex = cancelIndex.get();
    } else {
      focusIndex = 0;
    }

    group.focusTab(focusIndex);

    if (timer != null) {
      timer.schedule(timeout);
    }
  }

  public boolean close(Widget source) {
    boolean ok = false;

    if (source != null) {
      Popup p = DomUtils.getParentPopup(source);
      if (p != null) {
        p.hide();
        ok = true;
      }
    }
    return ok;
  }

  public void confirm(String message, ConfirmationCallback callback) {
    confirm(null, message, callback);
  }

  public void confirm(String caption, String message, ConfirmationCallback callback) {
    confirm(caption, message, callback, null, null);
  }

  public void confirm(String caption, String message, ConfirmationCallback callback,
      String dialogStyle, String messageStyle) {
    Assert.notEmpty(message);
    confirm(caption, Lists.newArrayList(message), callback, dialogStyle, messageStyle);
  }

  public void confirm(String caption, List<String> messages, final ConfirmationCallback callback,
      String dialogStyle, String messageStyle) {
    Assert.notEmpty(messages);
    Assert.notNull(callback);

    final Popup panel;
    if (BeeUtils.isEmpty(caption)) {
      panel = new Popup(OutsideClick.IGNORE, Modality.MODAL);
    } else {
      panel = DialogBox.create(caption);
    }

    HtmlTable content = new HtmlTable();
    content.addStyleName(STYLE_CONFIRM_CONTAINER);

    int row = 0;
    for (String message : messages) {
      if (message != null) {
        BeeLabel label = new BeeLabel(message);
        label.addStyleName(BeeUtils.notEmpty(messageStyle, STYLE_CONFIRM_MESSAGE));

        content.setWidget(row++, 1, label);
      }
    }

    BeeImage ok = new BeeImage(Global.getImages().ok(), new Command() {
      @Override
      public void execute() {
        panel.hide();
        callback.onConfirm();
      }
    });
    content.setWidget(row, 0, ok);

    BeeImage cancel = new BeeImage(Global.getImages().cancel(), new Command() {
      @Override
      public void execute() {
        panel.hide();
        callback.onCancel();
      }
    });
    content.setWidget(row, 2, cancel);

    if (!BeeUtils.isEmpty(dialogStyle)) {
      panel.addStyleName(dialogStyle);
    }

    panel.setWidget(content);
    panel.setAnimationEnabled(true);
    panel.center();

    panel.setHideOnEscape(true);
    panel.setOnEscape(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        callback.onCancel();
      }
    });

    panel.setHideOnSave(true);
    panel.setOnSave(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        callback.onConfirm();
      }
    });

    DomUtils.makeFocusable(panel);
    DomUtils.setFocus(panel, true);
  }

  public void decide(String caption, List<String> messages, DecisionCallback callback,
      int defaultValue) {
    decide(caption, messages, callback, defaultValue, null, null);
  }

  public void decide(String caption, List<String> messages, final DecisionCallback callback,
      int defaultValue, String dialogStyle, String messageStyle) {
    Assert.notEmpty(messages);
    Assert.notNull(callback);

    final Popup popup;
    final String styleName = BeeUtils.notEmpty(dialogStyle, STYLE_DECISION_DIALOG);
    if (BeeUtils.isEmpty(caption)) {
      popup = new Popup(OutsideClick.IGNORE, Modality.MODAL, styleName);
    } else {
      popup = DialogBox.create(caption, styleName);
    }

    HtmlTable panel = new HtmlTable();
    panel.addStyleName(STYLE_DECISION_PANEL);

    setCell(panel, 0, 0, new BeeImage(Global.getImages().question()), STYLE_DECISION_ICON);

    int row = 0;
    for (String message : messages) {
      if (message != null) {
        setCell(panel, row++, 1, new BeeLabel(message),
            BeeUtils.notEmpty(messageStyle, STYLE_DECISION_MESSAGE));
      }
    }

    Horizontal group = new Horizontal();

    final BeeButton yes = new BeeButton(Global.CONSTANTS.yes(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        popup.hide();
        callback.onConfirm();
      }
    });
    group.add(yes);

    final BeeButton no = new BeeButton(Global.CONSTANTS.no(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        popup.hide();
        callback.onDeny();
      }
    });
    group.add(no);

    final BeeButton cancel = new BeeButton(Global.CONSTANTS.cancel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        popup.hide();
        callback.onCancel();
      }
    });
    group.add(cancel);

    for (Widget widget : group) {
      widget.addStyleName(STYLE_DECISION_OPTION);
    }

    setCell(panel, row, 1, group, STYLE_DECISION_GROUP);

    popup.setWidget(panel);
    popup.setAnimationEnabled(true);
    popup.center();

    popup.setHideOnEscape(true);
    popup.setOnEscape(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        callback.onCancel();
      }
    });

    if (defaultValue >= 0 && defaultValue < group.getWidgetCount()) {
      UiHelper.focus(group.getWidget(defaultValue));

      yes.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (event.isRightArrow() || event.isDownArrow()) {
            no.setFocus(true);
          } else if (event.isLeftArrow() || event.isUpArrow()) {
            cancel.setFocus(true);
          }
        }
      });

      no.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (event.isRightArrow() || event.isDownArrow()) {
            cancel.setFocus(true);
          } else if (event.isLeftArrow() || event.isUpArrow()) {
            yes.setFocus(true);
          }
        }
      });

      cancel.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (event.isRightArrow() || event.isDownArrow()) {
            yes.setFocus(true);
          } else if (event.isLeftArrow() || event.isUpArrow()) {
            no.setFocus(true);
          }
        }
      });
    }
  }

  public boolean nativeConfirm(String... lines) {
    Assert.notNull(lines);
    Assert.parameterCount(lines.length, 1);
    return Window.confirm(BeeUtils.buildLines(lines));
  }

  public void pickStar(Integer defaultValue, Element target, final ChoiceCallback callback) {
    Assert.notNull(callback);

    final Popup popup = new Popup(OutsideClick.CLOSE, Modality.MODAL, STYLE_STAR_PICKER);

    TabBar cluster = new TabBar(STYLE_STAR_CLUSTER, Orientation.HORIZONTAL);

    int size = Stars.count();
    for (int i = 0; i < size; i++) {
      BeeImage image = new BeeImage(Stars.get(i));
      cluster.addItem(image);
    }

    final Holder<Integer> selectedIndex = Holder.absent();

    cluster.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        selectedIndex.set(event.getSelectedItem());
        popup.hide();
      }
    });

    popup.setHideOnEscape(true);

    popup.addCloseHandler(new CloseHandler<Popup>() {
      public void onClose(CloseEvent<Popup> event) {
        if (selectedIndex.isNotNull()) {
          callback.onSuccess(selectedIndex.get());
        } else {
          callback.onCancel();
        }
      }
    });

    popup.setWidget(cluster);

    popup.setAnimationEnabled(true);

    if (target == null) {
      popup.center();
    } else {
      popup.showRelativeTo(target);
    }

    int focusIndex;

    if (defaultValue != null && cluster.isIndex(defaultValue)) {
      cluster.selectTab(defaultValue, false);
      focusIndex = defaultValue;
    } else {
      focusIndex = 0;
    }

    cluster.focusTab(focusIndex);
  }

  public void showError(String caption, List<String> messages, String dialogStyle,
      String closeHtml) {

    Popup popup;
    String styleName = BeeUtils.notEmpty(dialogStyle, STYLE_ERROR_DIALOG);
    if (BeeUtils.isEmpty(caption)) {
      popup = new Popup(OutsideClick.CLOSE, Modality.MODAL, styleName);
    } else {
      popup = DialogBox.create(caption, styleName);
    }

    HtmlTable panel = new HtmlTable();
    panel.addStyleName(STYLE_ERROR_PANEL);

    setCell(panel, 0, 0, new BeeImage(Global.getImages().error()), STYLE_ERROR_ICON);

    int row = 0;
    if (BeeUtils.isEmpty(messages)) {
      setCell(panel, row++, 1, new Html("where is my errors"), STYLE_ERROR_MESSAGE);
    } else {
      for (String message : messages) {
        if (message != null) {
          setCell(panel, row++, 1, new Html(message), STYLE_ERROR_MESSAGE);
        }
      }
    }

    CloseButton close = new CloseButton(BeeUtils.notEmpty(closeHtml, Global.CONSTANTS.ok()));
    setCell(panel, row, 1, close, STYLE_ERROR_CLOSE);

    popup.setHideOnEscape(true);

    popup.setWidget(panel);
    popup.setAnimationEnabled(true);

    popup.center();
    close.setFocus(true);
  }

  public void showInfo(String... messages) {
    CloseButton b = new CloseButton(DialogConstants.OK);
    Popup box = createPopup(b, messages);

    box.center();
    b.setFocus(true);
  }

  public void showTable(String caption, IsTable<?, ?> table) {
    Assert.notNull(table);

    int c = table.getNumberOfColumns();
    Assert.isPositive(c);

    int r = table.getNumberOfRows();
    if (r <= 0) {
      logger.warning(caption, "data table empty");
      return;
    }

    HtmlTable grid = new HtmlTable();
    grid.addStyleName(STYLE_TABLE);
    int index = 0;

    if (!BeeUtils.isEmpty(caption)) {
      grid.setHTML(index, 0, caption.trim());
      grid.alignCenter(index, 0);
      if (c > 1) {
        grid.getCellFormatter().setColSpan(index, 0, c);
      }
      index++;
    }

    for (int j = 0; j < c; j++) {
      grid.setHTML(index, j, table.getColumnLabel(j));
      grid.alignCenter(index, j);
    }
    index++;

    for (int i = 0; i < r; i++) {
      for (int j = 0; j < c; j++) {
        String value = table.getString(i, j);
        if (!BeeUtils.isEmpty(value)) {
          grid.setHTML(index, j, value);
          if (ValueType.isNumeric(table.getColumnType(j))) {
            grid.alignRight(index, j);
          }
        }
      }
      grid.getRowFormatter().setStyleName(index,
          (index % 2 == 0) ? CellGrid.STYLE_EVEN_ROW : CellGrid.STYLE_ODD_ROW);
      index++;
    }

    CloseButton close = new CloseButton(DialogConstants.OK);
    grid.setWidget(index, 0, close);
    grid.alignCenter(index, 0);
    if (c > 1) {
      grid.getCellFormatter().setColSpan(index, 0, c);
    }

    Simple container = new Simple(grid);
    container.addStyleName(STYLE_TABLE_CONTAINER);

    Popup box = new Popup(OutsideClick.CLOSE, Modality.MODAL);
    box.setAnimationEnabled(true);

    box.setWidget(container);

    box.center();
    close.setFocus(true);
  }

  public void showWidget(Widget widget) {
    Assert.notNull(widget);

    Popup box = new Popup(OutsideClick.CLOSE, Modality.MODAL);
    box.setAnimationEnabled(true);

    box.setWidget(widget);
    box.center();
  }

  private Popup createPopup(Widget bottom, String... messages) {
    Assert.notNull(messages);

    Vertical vp = new Vertical();
    for (String s : messages) {
      vp.add(new BeeLabel(s));
    }

    if (bottom != null) {
      vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      vp.add(bottom);
    }

    Popup popup = new Popup(OutsideClick.CLOSE, Modality.MODAL);
    popup.setAnimationEnabled(true);

    popup.setWidget(vp);
    return popup;
  }

  private void setCell(HtmlTable table, int row, int col, Widget widget, String styleName) {
    widget.addStyleName(styleName);
    table.setWidget(row, col, widget, styleName + CELL_STYLE_SUFFIX);
  }
}
