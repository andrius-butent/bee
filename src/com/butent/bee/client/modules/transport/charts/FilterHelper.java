package com.butent.bee.client.modules.transport.charts;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.modules.transport.charts.ChartData.Type;
import com.butent.bee.client.modules.transport.charts.Filterable.FilterType;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class FilterHelper {

  interface DialogCallback {

    void onClear();

    void onDataTypesChange(Set<ChartData.Type> types);

    void onFilter();

    void onSelectionChange(HasWidgets dataContainer);
  }

  static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-chart-filter-";
  static final String STYLE_DATA_PREFIX = STYLE_PREFIX + "data-";

  private static final BeeLogger logger = LogUtils.getLogger(FilterHelper.class);

  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_CONTENT = STYLE_PREFIX + "content";

  private static final String STYLE_DATA_WRAPPER = STYLE_DATA_PREFIX + "wrapper";
  private static final String STYLE_DATA_CONTAINER = STYLE_DATA_PREFIX + "container";

  private static final String STYLE_COMMAND_GROUP = STYLE_PREFIX + "commandGroup";
  private static final String STYLE_COMMAND_CLEAR = STYLE_PREFIX + "commandClear";
  private static final String STYLE_COMMAND_CONFIGURE = STYLE_PREFIX + "commandConfigure";
  private static final String STYLE_COMMAND_FILTER = STYLE_PREFIX + "commandFilter";

  private static final String STYLE_CONFIGURE_DIALOG = STYLE_PREFIX + "configure-dialog";
  private static final String STYLE_CONFIGURE_PANEL = STYLE_PREFIX + "configure-panel";
  private static final String STYLE_CONFIGURE_ITEM = STYLE_PREFIX + "configure-item";

  private static final int DATA_SPLITTER_WIDTH = 3;
  private static final int DATA_PANEL_MIN_WIDTH = 100;
  private static final int DATA_PANEL_MAX_WIDTH = 250;
  private static final int DATA_PANEL_MIN_HEIGHT = 200;
  private static final int DATA_PANEL_MAX_HEIGHT = 400;

  private static final int COMMAND_GROUP_HEIGHT = 32;

  private static final double DIALOG_MAX_WIDTH_FACTOR = 0.8;
  private static final double DIALOG_MAX_HEIGHT_FACTOR = 0.8;

  static void enableData(List<ChartData> allData, List<ChartData> tentativeData,
      HasWidgets dataContainer) {

    ChartData.Type typeOfSingleDataHavingSelection = null;
    for (ChartData data : allData) {
      if (data.hasSelection()) {
        if (typeOfSingleDataHavingSelection == null) {
          typeOfSingleDataHavingSelection = data.getType();
        } else {
          typeOfSingleDataHavingSelection = null;
          break;
        }
      }
    }

    FilterDataWidget dataWidget = null;

    for (ChartData data : allData) {
      ChartData.Type type = data.getType();

      if (dataContainer != null) {
        dataWidget = getDataWidget(dataContainer, type);
      }

      ChartData tentative = getDataByType(tentativeData, data.getType());

      int size = data.getItems().size();
      boolean changed = false;

      for (int i = 0; i < size; i++) {
        ChartData.Item item = data.getItems().get(i);

        boolean itemEnabled;
        if (typeOfSingleDataHavingSelection != null && typeOfSingleDataHavingSelection == type) {
          itemEnabled = true;
        } else if (tentative == null || tentative.isEmpty()) {
          itemEnabled = false;
        } else {
          itemEnabled = tentative.contains(item.getName());
        }

        boolean selected = item.isSelected();

        if (data.setItemEnabled(item, itemEnabled) && dataWidget != null) {
          if (itemEnabled) {
            dataWidget.addItem(item, i);
          } else {
            dataWidget.removeItem(i, selected);
          }

          changed = true;
        }
      }

      if (changed && dataWidget != null) {
        dataWidget.refresh();
      }
    }
  }

  static void enableDataTypes(Collection<ChartData> data, Set<ChartData.Type> types) {
    if (BeeUtils.isEmpty(data)) {
      return;
    }

    if (BeeUtils.isEmpty(types)) {
      for (ChartData cd : data) {
        if (cd != null) {
          cd.setEnabled(true);
        }
      }

    } else {
      boolean found = false;
      for (ChartData cd : data) {
        if (cd != null && types.contains(cd.getType())) {
          found = true;
          break;
        }
      }

      for (ChartData cd : data) {
        if (cd != null) {
          if (found) {
            cd.setEnabled(types.contains(cd.getType()));
          } else {
            cd.setEnabled(true);
          }
        }
      }
    }
  }

  static ChartData getDataByType(Collection<ChartData> data, ChartData.Type type) {
    if (data != null && type != null) {
      for (ChartData cd : data) {
        if (cd != null && cd.getType() == type) {
          return cd;
        }
      }
    }
    return null;
  }

  static FilterDataWidget getDataWidget(HasWidgets container, ChartData.Type type) {
    for (Widget widget : container) {
      if (widget instanceof FilterDataWidget && ((FilterDataWidget) widget).hasType(type)) {
        return (FilterDataWidget) widget;
      }
    }
    return null;
  }

  static <T extends Filterable & HasDateRange> List<HasDateRange> getPersistentItems(
      Collection<T> items) {

    List<HasDateRange> result = new ArrayList<>();
    if (items == null) {
      return result;
    }

    for (T item : items) {
      if (item != null && item.matched(FilterType.PERSISTENT)) {
        result.add(item);
      }
    }
    return result;
  }

  static List<ChartData> getSelectedData(Collection<ChartData> data) {
    List<ChartData> result = new ArrayList<>();

    if (data != null) {
      for (ChartData input : data) {
        if (input != null && input.hasSelection()) {
          ChartData selected = new ChartData(input.getType());
          selected.getItems().addAll(input.getSelectedItems());

          result.add(selected);
        }
      }
    }

    return result;
  }

  static boolean matches(ChartData data, JustDate date) {
    if (data == null) {
      return true;
    } else {
      return data.contains(date);
    }
  }

  static boolean matches(ChartData data, Long id) {
    if (data == null) {
      return true;
    } else if (id == null) {
      return false;
    } else {
      return data.contains(id);
    }
  }

  static boolean matches(ChartData data, String name) {
    if (data == null) {
      return true;
    } else if (BeeUtils.isEmpty(name)) {
      return false;
    } else {
      return data.contains(name);
    }
  }

  static boolean matchesAny(ChartData data, Collection<Long> ids) {
    if (data == null) {
      return true;
    } else {
      return data.containsAny(ids);
    }
  }

  static List<ChartData> notEmptyData(Collection<ChartData> data) {
    List<ChartData> result = new ArrayList<>();

    if (data != null) {
      for (ChartData cd : data) {
        if (cd != null && !cd.isEmpty()) {
          result.add(cd);
        }
      }
    }

    return result;
  }

  static void openDialog(final List<ChartData> filterData, final DialogCallback callback) {
    int dataCounter = 0;

    for (ChartData data : filterData) {
      if (data.isEnabled() && !data.isEmpty()) {
        dataCounter++;
      }
    }

    if (dataCounter <= 0) {
      BeeKeeper.getScreen().notifyWarning(Localized.getConstants().tooLittleData());
      return;
    }

    int dialogMaxWidth = BeeUtils.round(BeeKeeper.getScreen().getWidth()
        * DIALOG_MAX_WIDTH_FACTOR);
    int dialogMaxHeight = BeeUtils.round(BeeKeeper.getScreen().getHeight()
        * DIALOG_MAX_HEIGHT_FACTOR);

    int dataPanelWidth = (dialogMaxWidth - DATA_SPLITTER_WIDTH * (dataCounter - 1)) / dataCounter;
    int dataPanelHeight = dialogMaxHeight - DialogBox.HEADER_HEIGHT - COMMAND_GROUP_HEIGHT
        - DomUtils.getScrollBarHeight();

    if (dialogMaxWidth < DATA_PANEL_MIN_WIDTH || dataPanelHeight < DATA_PANEL_MIN_HEIGHT) {
      logger.warning("get a real computer", BeeKeeper.getScreen().getWidth(),
          BeeKeeper.getScreen().getHeight(), dataPanelWidth, dataPanelHeight);
      return;
    }

    dataPanelWidth = BeeUtils.clamp(dataPanelWidth, DATA_PANEL_MIN_WIDTH, DATA_PANEL_MAX_WIDTH);
    dataPanelHeight = BeeUtils.clamp(dataPanelHeight, DATA_PANEL_MIN_HEIGHT, DATA_PANEL_MAX_HEIGHT);

    int dataContainerWidth = dataPanelWidth * dataCounter + DATA_SPLITTER_WIDTH * (dataCounter - 1);
    int dataContainerHeight = dataPanelHeight;

    int dataWrapperWidth = Math.min(dataContainerWidth, dialogMaxWidth);
    int dataWrapperHeight = dataContainerHeight + DomUtils.getScrollBarHeight();

    int contentWidth = dataWrapperWidth;
    int contentHeight = dataWrapperHeight + COMMAND_GROUP_HEIGHT;

    final DialogBox dialog = DialogBox.create(Localized.getConstants().filter(), STYLE_DIALOG);

    final Split dataContainer = new Split(DATA_SPLITTER_WIDTH);
    dataContainer.addStyleName(STYLE_DATA_CONTAINER);
    StyleUtils.setSize(dataContainer, dataContainerWidth, dataContainerHeight);

    SelectionHandler<ChartData.Type> selectionHandler = new SelectionHandler<ChartData.Type>() {
      @Override
      public void onSelection(SelectionEvent<ChartData.Type> event) {
        callback.onSelectionChange(dataContainer);
      }
    };

    int dataIndex = 0;
    for (ChartData data : filterData) {
      if (data.isEnabled() && !data.isEmpty()) {
        data.saveState();

        FilterDataWidget dataWidget = new FilterDataWidget(data);
        dataWidget.addSelectionHandler(selectionHandler);

        dataIndex++;
        if (dataIndex < dataCounter) {
          dataContainer.addWest(dataWidget, dataPanelWidth, DATA_SPLITTER_WIDTH);
        } else {
          dataContainer.add(dataWidget);
        }
      }
    }

    Flow commands = new Flow();
    commands.addStyleName(STYLE_COMMAND_GROUP);

    Button filter = new Button(Localized.getConstants().doFilter(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialog.close();
        callback.onFilter();
      }
    });
    filter.addStyleName(STYLE_COMMAND_FILTER);
    commands.add(filter);

    Button clear = new Button(Localized.getConstants().clear(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        for (Widget widget : dataContainer) {
          if (widget instanceof FilterDataWidget) {
            ((FilterDataWidget) widget).reset(true);
          }
        }
        callback.onClear();
      }
    });
    clear.addStyleName(STYLE_COMMAND_CLEAR);
    commands.add(clear);

    Button configure = new Button(Localized.getConstants().actionConfigure(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        configureDataTypes(filterData, EventUtils.getEventTargetElement(event),
            new Callback<Set<ChartData.Type>>() {
              @Override
              public void onSuccess(Set<Type> result) {
                dialog.setAnimationEnabled(false);
                dialog.close();
                callback.onDataTypesChange(result);
              }
            });
      }
    });
    configure.addStyleName(STYLE_COMMAND_CONFIGURE);
    commands.add(configure);

    dialog.addCloseHandler(new CloseEvent.Handler() {
      @Override
      public void onClose(CloseEvent event) {
        if (event.userCaused()) {
          for (ChartData data : filterData) {
            data.restoreState();
          }
        }
      }
    });

    Simple dataWrapper = new Simple(dataContainer);
    dataWrapper.addStyleName(STYLE_DATA_WRAPPER);
    StyleUtils.setSize(dataWrapper, dataWrapperWidth, dataWrapperHeight);

    Flow content = new Flow();
    content.addStyleName(STYLE_CONTENT);
    StyleUtils.setSize(content, contentWidth, contentHeight);

    content.add(dataWrapper);
    content.add(commands);

    dialog.setWidget(content);

    dialog.setHideOnEscape(true);
    dialog.setAnimationEnabled(true);

    dialog.center();
    filter.setFocus(true);
  }

  static boolean persistFilter(Collection<? extends Filterable> items) {
    boolean filtered = false;

    if (items != null) {
      for (Filterable item : items) {
        if (item != null && !item.persistFilter()) {
          filtered = true;
        }
      }
    }
    return filtered;
  }

  static void resetFilter(Collection<? extends Filterable> items, FilterType filterType) {
    if (items != null && filterType != null) {
      for (Filterable item : items) {
        if (item != null) {
          item.setMatch(filterType, true);
        }
      }
    }
  }

  private static void configureDataTypes(List<ChartData> filterData, Element target,
      final Callback<Set<ChartData.Type>> callback) {

    final Set<ChartData.Type> oldTypes = EnumSet.noneOf(ChartData.Type.class);
    final Set<ChartData.Type> newTypes = EnumSet.noneOf(ChartData.Type.class);

    Flow panel = new Flow(STYLE_CONFIGURE_PANEL);

    for (ChartData chartData : filterData) {
      ChartData.Type type = chartData.getType();
      boolean enabled = chartData.isEnabled();

      CheckBox item = new CheckBox(type.getCaption());
      DomUtils.setDataIndex(item.getElement(), type.ordinal());
      item.setChecked(enabled);

      item.addStyleName(STYLE_CONFIGURE_ITEM);

      item.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof CheckBox) {
            CheckBox source = (CheckBox) event.getSource();
            ChartData.Type tp = EnumUtils.getEnumByIndex(ChartData.Type.class,
                DomUtils.getDataIndexInt(source.getElement()));

            if (tp != null) {
              if (source.isChecked()) {
                newTypes.add(tp);
              } else {
                newTypes.remove(tp);
              }
            }
          }
        }
      });

      panel.add(item);

      if (enabled) {
        oldTypes.add(type);
      }
    }

    if (!oldTypes.isEmpty()) {
      newTypes.addAll(oldTypes);
    }

    Global.inputWidget(Localized.getConstants().actionConfigure(), panel,
        new InputCallback() {
          @Override
          public void onSuccess() {
            if (!oldTypes.equals(newTypes)) {
              callback.onSuccess(newTypes);
            }
          }
        }, STYLE_CONFIGURE_DIALOG, target);
  }

  private FilterHelper() {
  }
}
