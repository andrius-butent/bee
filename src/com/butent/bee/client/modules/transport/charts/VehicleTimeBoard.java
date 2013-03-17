package com.butent.bee.client.modules.transport.charts;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.transport.charts.CargoEvent.Type;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Procedure;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class VehicleTimeBoard extends ChartBase {

  private static final BeeLogger logger = LogUtils.getLogger(VehicleTimeBoard.class);

  private static final String STYLE_PREFIX = "bee-tr-vtb-";

  private static final String STYLE_VEHICLE_PREFIX = STYLE_PREFIX + "Vehicle-";
  private static final String STYLE_VEHICLE_ROW_SEPARATOR = STYLE_VEHICLE_PREFIX + "row-sep";

  private static final String STYLE_NUMBER_PREFIX = STYLE_PREFIX + "Number-";
  private static final String STYLE_NUMBER_PANEL = STYLE_NUMBER_PREFIX + "panel";
  private static final String STYLE_NUMBER_LABEL = STYLE_NUMBER_PREFIX + "label";
  private static final String STYLE_NUMBER_OVERLAP = STYLE_NUMBER_PREFIX + "overlap";

  private static final String STYLE_INFO_PREFIX = STYLE_PREFIX + "Info-";
  private static final String STYLE_INFO_PANEL = STYLE_INFO_PREFIX + "panel";
  private static final String STYLE_INFO_LABEL = STYLE_INFO_PREFIX + "label";
  private static final String STYLE_INFO_OVERLAP = STYLE_INFO_PREFIX + "overlap";

  private static final String STYLE_VEHICLE_DRAG = STYLE_VEHICLE_PREFIX + "drag";
  private static final String STYLE_VEHICLE_OVER = STYLE_VEHICLE_PREFIX + "over";
  
  private static final String STYLE_TRIP_PREFIX = STYLE_PREFIX + "Trip-";
  private static final String STYLE_TRIP_PANEL = STYLE_TRIP_PREFIX + "panel";
  private static final String STYLE_TRIP_VOID = STYLE_TRIP_PREFIX + "void";

  private static final String STYLE_TRIP_DRAG = STYLE_TRIP_PREFIX + "drag";
  private static final String STYLE_TRIP_OVER = STYLE_TRIP_PREFIX + "over";
  
  private static final String STYLE_FREIGHT_PREFIX = STYLE_PREFIX + "Freight-";
  private static final String STYLE_FREIGHT_PANEL = STYLE_FREIGHT_PREFIX + "panel";

  private static final String STYLE_FREIGHT_DRAG = STYLE_FREIGHT_PREFIX + "drag";
  private static final String STYLE_FREIGHT_OVER = STYLE_FREIGHT_PREFIX + "over";
  
  private static final String STYLE_DAY_PREFIX = STYLE_PREFIX + "Day-";
  private static final String STYLE_DAY_PANEL = STYLE_DAY_PREFIX + "panel";
  private static final String STYLE_DAY_WIDGET = STYLE_DAY_PREFIX + "widget";
  private static final String STYLE_DAY_EMPTY = STYLE_DAY_PREFIX + "empty";
  private static final String STYLE_DAY_FLAG = STYLE_DAY_PREFIX + "flag";
  private static final String STYLE_DAY_LABEL = STYLE_DAY_PREFIX + "label";

  private static final String STYLE_SERVICE_PREFIX = STYLE_PREFIX + "Service-";
  private static final String STYLE_SERVICE_PANEL = STYLE_SERVICE_PREFIX + "panel";
  private static final String STYLE_SERVICE_LABEL = STYLE_SERVICE_PREFIX + "label";

  private static final String STYLE_INACTIVE = STYLE_PREFIX + "Inactive";
  private static final String STYLE_OVERLAP = STYLE_PREFIX + "Overlap";
  
  private static final Set<String> VEHICLE_ACCEPTS_DROP_TYPES = 
      ImmutableSet.of(DATA_TYPE_TRIP, DATA_TYPE_FREIGHT, DATA_TYPE_ORDER_CARGO);
  private static final Set<String> TRIP_ACCEPTS_DROP_TYPES = 
      ImmutableSet.of(DATA_TYPE_TRUCK, DATA_TYPE_TRAILER, DATA_TYPE_FREIGHT, DATA_TYPE_ORDER_CARGO,
          DATA_TYPE_DRIVER);
  private static final Set<String> FREIGHT_ACCEPTS_DROP_TYPES = 
      ImmutableSet.of(DATA_TYPE_FREIGHT, DATA_TYPE_ORDER_CARGO);

  private final List<Vehicle> vehicles = Lists.newArrayList();

  private final Multimap<Long, Trip> trips = ArrayListMultimap.create();
  private final Multimap<Long, String> drivers = HashMultimap.create();

  private final Multimap<Long, Freight> freights = ArrayListMultimap.create();
  private final Multimap<Long, CargoHandling> handling = ArrayListMultimap.create();

  private final Multimap<Long, VehicleService> services = ArrayListMultimap.create();

  private int numberWidth = BeeConst.UNDEF;
  private int infoWidth = BeeConst.UNDEF;

  private boolean separateCargo = false;
  private boolean showCountryFlags = false;
  private boolean showPlaceInfo = false;

  private final Set<String> numberPanels = Sets.newHashSet();
  private final Set<String> infoPanels = Sets.newHashSet();
  
  private final List<Integer> vehicleIndexesByRow = Lists.newArrayList();
  
  private final VehicleType vehicleType;

  protected VehicleTimeBoard() {
    super();

    addStyleName(STYLE_PREFIX + "View");

    setRelevantDataViews(VIEW_VEHICLES, VIEW_TRIPS, VIEW_ORDER_CARGO, VIEW_CARGO_HANDLING,
        VIEW_CARGO_TRIPS, VIEW_TRIP_CARGO, VIEW_TRIP_DRIVERS, VIEW_VEHICLE_SERVICES,
        CommonsConstants.VIEW_COLORS, CommonsConstants.VIEW_THEME_COLORS);
    
    this.vehicleType = getDataType().equals(DATA_TYPE_TRUCK) 
        ? VehicleType.TRUCK : VehicleType.TRAILER;
  }

  @Override
  public void handleAction(Action action) {
    if (Action.ADD.equals(action)) {
      RowFactory.createRow(VIEW_VEHICLES);
    } else {
      super.handleAction(action);
    }
  }
  
  @Override
  protected Collection<? extends HasDateRange> getChartItems() {
    return separateCargo() ? freights.values() : trips.values();
  }

  protected abstract String getDataType();

  protected abstract String getDayWidthColumnName();

  @Override
  protected Set<Action> getEnabledActions() {
    return EnumSet.of(Action.REFRESH, Action.ADD, Action.CONFIGURE, Action.FILTER);
  }

  protected abstract String getInfoWidthColumnName();

  protected abstract String getItemOpacityColumnName();

  protected abstract String getNumberWidthColumnName();

  protected abstract String getSeparateCargoColumnName();

  protected abstract String getShowCountryFlagsColumnName();

  protected abstract String getShowPlaceInfoColumnName();

  protected abstract String getTripVehicleIdColumnName();

  protected abstract String getTripVehicleNumberColumnName();

  @Override
  protected void initData(BeeRowSet rowSet) {
    vehicles.clear();
    trips.clear();
    drivers.clear();
    freights.clear();
    handling.clear();
    services.clear();

    if (rowSet == null) {
      updateMaxRange();
      return;
    }

    String serialized = rowSet.getTableProperty(PROP_VEHICLES);
    if (!BeeUtils.isEmpty(serialized)) {
      BeeRowSet brs = BeeRowSet.restore(serialized);
      for (BeeRow row : brs.getRows()) {
        vehicles.add(new Vehicle(row));
      }
    }

    serialized = rowSet.getTableProperty(PROP_DRIVERS);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      for (SimpleRow row : srs) {
        drivers.put(row.getLong(COL_TRIP),
            BeeUtils.joinWords(row.getValue(CommonsConstants.COL_FIRST_NAME),
                row.getValue(CommonsConstants.COL_LAST_NAME)));
      }
    }

    serialized = rowSet.getTableProperty(PROP_CARGO_HANDLING);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      for (SimpleRow row : srs) {
        handling.put(row.getLong(COL_CARGO), new CargoHandling(row));
      }
    }

    serialized = rowSet.getTableProperty(PROP_FREIGHTS);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);

      for (SimpleRow row : srs) {
        JustDate minLoad = null;
        JustDate maxUnload = null;
        Long cargoId = row.getLong(COL_CARGO);

        if (handling.containsKey(cargoId)) {
          for (CargoHandling ch : handling.get(cargoId)) {
            minLoad = BeeUtils.min(minLoad, ch.getLoadingDate());
            maxUnload = BeeUtils.max(maxUnload, ch.getUnloadingDate());
          }
        }

        freights.put(row.getLong(COL_TRIP_ID), new Freight(row, minLoad, maxUnload));
      }
    }

    serialized = rowSet.getTableProperty(PROP_TRIPS);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      int index = srs.getColumnIndex(getTripVehicleIdColumnName());

      for (SimpleRow row : srs) {
        Long tripId = row.getLong(COL_TRIP_ID);

        String drv = drivers.containsKey(tripId)
            ? BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, drivers.get(tripId)) : null;
        int cargoCount = 0;

        if (freights.containsKey(tripId)) {
          JustDate maxDate = null;
          for (Freight freight : freights.get(tripId)) {
            maxDate = BeeUtils.max(maxDate, freight.getMaxDate());
            cargoCount++;
          }

          Trip trip = new Trip(row, maxDate, drv, cargoCount);
          trips.put(row.getLong(index), trip);

          for (Freight freight : freights.get(tripId)) {
            freight.adjustRange(trip.getRange());
            freight.setTripTitle(trip.getTitle());
          }

        } else {
          trips.put(row.getLong(index), new Trip(row, null, drv, cargoCount));
        }
      }
    }

    serialized = rowSet.getTableProperty(PROP_VEHICLE_SERVICES);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      for (SimpleRow row : srs) {
        VehicleService service = new VehicleService(row);
        services.put(service.getVehicleId(), service);
      }
    }

    setSeparateCargo(ChartHelper.getBoolean(getSettings(), getSeparateCargoColumnName()));
    updateMaxRange();

    logger.debug(getCaption(), vehicles.size(), trips.size(), drivers.size(), freights.size(),
        handling.size(), services.size());
  }

  @Override
  protected Collection<? extends HasDateRange> initItems(SimpleRowSet data) {
    return null;
  }

  @Override
  protected void onDoubleClickChart(int row, JustDate date) {
    if (BeeUtils.isIndex(vehicleIndexesByRow, row) && TimeUtils.isMeq(date, TimeUtils.today())) {
      DataInfo dataInfo = Data.getDataInfo(VIEW_TRIPS);
      BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);

      if (TimeUtils.isMore(date, TimeUtils.today())) {
        newRow.setValue(dataInfo.getColumnIndex(COL_TRIP_DATE), date.getDateTime());
      }
      
      Vehicle vehicle = vehicles.get(vehicleIndexesByRow.get(row));

      newRow.setValue(dataInfo.getColumnIndex(getTripVehicleIdColumnName()), vehicle.getId());
      newRow.setValue(dataInfo.getColumnIndex(getTripVehicleNumberColumnName()),
          vehicle.getNumber());

      RowFactory.createRow(dataInfo, newRow);
    }
  }
  
  @Override
  protected void prepareChart(Size canvasSize) {
    setNumberWidth(ChartHelper.getPixels(getSettings(), getNumberWidthColumnName(), 80,
        ChartHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));
    setInfoWidth(ChartHelper.getPixels(getSettings(), getInfoWidthColumnName(), 120,
        ChartHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));

    setChartLeft(getNumberWidth() + getInfoWidth());
    setChartWidth(canvasSize.getWidth() - getChartLeft() - getChartRight());

    setDayColumnWidth(ChartHelper.getPixels(getSettings(), getDayWidthColumnName(), 20,
        1, getChartWidth()));

    boolean sc = ChartHelper.getBoolean(getSettings(), getSeparateCargoColumnName());
    if (separateCargo() != sc) {
      setSeparateCargo(sc);
      updateMaxRange();
    }

    setShowCountryFlags(ChartHelper.getBoolean(getSettings(), getShowCountryFlagsColumnName()));
    setShowPlaceInfo(ChartHelper.getBoolean(getSettings(), getShowPlaceInfoColumnName()));
  }

  @Override
  protected void renderContent(ComplexPanel panel) {
    numberPanels.clear();
    infoPanels.clear();
    
    vehicleIndexesByRow.clear();
       
    List<ChartRowLayout> vehicleLayout = doLayout();

    int rc = 0;
    for (ChartRowLayout layout : vehicleLayout) {
      rc += layout.size();
    }

    initContent(panel, rc);
    if (vehicleLayout.isEmpty()) {
      return;
    }

    int calendarWidth = getCalendarWidth();

    Double opacity = ChartHelper.getOpacity(getSettings(), getItemOpacityColumnName());

    Edges margins = new Edges();
    margins.setBottom(ChartHelper.ROW_SEPARATOR_HEIGHT);

    Widget offWidget;
    Widget itemWidget;
    Widget overlapWidget;

    int rowIndex = 0;
    for (ChartRowLayout layout : vehicleLayout) {
      
      int vehicleIndex = layout.getDataIndex();

      int size = layout.size();
      int lastRow = rowIndex + size - 1;

      int top = rowIndex * getRowHeight();

      if (rowIndex > 0) {
        ChartHelper.addRowSeparator(panel, STYLE_VEHICLE_ROW_SEPARATOR, top, 0,
            getChartLeft() + calendarWidth);
      }

      Vehicle vehicle = vehicles.get(vehicleIndex);
      Assert.notNull(vehicle, "vehicle not found");

      boolean hasOverlap = layout.hasOverlap();

      IdentifiableWidget numberWidget = createNumberWidget(vehicle, hasOverlap);
      addNumberWidget(panel, numberWidget.asWidget(), rowIndex, lastRow);
      numberPanels.add(numberWidget.getId());

      IdentifiableWidget infoWidget = createInfoWidget(vehicle, hasOverlap);
      addInfoWidget(panel, infoWidget.asWidget(), rowIndex, lastRow);
      infoPanels.add(infoWidget.getId());

      for (int i = 1; i < size; i++) {
        ChartHelper.addRowSeparator(panel, top + getRowHeight() * i, getChartLeft(), calendarWidth);
      }

      for (HasDateRange item : layout.getInactivity()) {
        if (item instanceof VehicleService) {
          offWidget = ((VehicleService) item).createWidget(this, STYLE_SERVICE_PANEL,
              STYLE_SERVICE_LABEL);
        } else {
          offWidget = new CustomDiv(STYLE_INACTIVE);
          UiHelper.maybeSetTitle(offWidget, vehicle.getInactivityTitle(item.getRange()));
        }

        Rectangle rectangle = getRectangle(item.getRange(), rowIndex, lastRow);
        ChartHelper.apply(offWidget, rectangle, margins);

        panel.add(offWidget);
      }

      for (int i = 0; i < layout.getRows().size(); i++) {
        for (HasDateRange item : layout.getRows().get(i)) {

          if (item instanceof Trip) {
            itemWidget = createTripWidget((Trip) item);
          } else if (item instanceof Freight) {
            itemWidget = createFreightWidget((Freight) item);
          } else {
            itemWidget = null;
          }

          if (itemWidget != null) {
            Rectangle rectangle = getRectangle(item.getRange(), rowIndex + i);
            ChartHelper.apply(itemWidget, rectangle, margins);
            if (opacity != null) {
              StyleUtils.setOpacity(itemWidget, opacity);
            }

            panel.add(itemWidget);
          }

          if (hasOverlap) {
            Set<Range<JustDate>> overlap = layout.getOverlap(item.getRange());

            for (Range<JustDate> over : overlap) {
              overlapWidget = new CustomDiv(STYLE_OVERLAP);

              Rectangle rectangle = getRectangle(over, rowIndex + i);
              ChartHelper.apply(overlapWidget, rectangle, margins);

              panel.add(overlapWidget);
            }
          }
        }
      }
      
      for (int i = 0; i < size; i++) {
        vehicleIndexesByRow.add(vehicleIndex);
      }

      rowIndex += size;
    }
  }

  @Override
  protected void renderMovers(ComplexPanel panel, int height) {
    Mover numberMover = ChartHelper.createHorizontalMover();
    StyleUtils.setLeft(numberMover, getNumberWidth() - ChartHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(numberMover, height);

    numberMover.addMoveHandler(new MoveEvent.Handler() {
      @Override
      public void onMove(MoveEvent event) {
        onNumberResize(event);
      }
    });

    panel.add(numberMover);

    Mover infoMover = ChartHelper.createHorizontalMover();
    StyleUtils.setLeft(infoMover, getChartLeft() - ChartHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(infoMover, height);

    infoMover.addMoveHandler(new MoveEvent.Handler() {
      @Override
      public void onMove(MoveEvent event) {
        onInfoResize(event);
      }
    });

    panel.add(infoMover);
  }

  private void addInfoWidget(HasWidgets panel, Widget widget, int firstRow, int lastRow) {
    Rectangle rectangle = ChartHelper.getRectangle(getNumberWidth(), getInfoWidth(),
        firstRow, lastRow, getRowHeight());

    Edges margins = new Edges();
    margins.setRight(ChartHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(ChartHelper.ROW_SEPARATOR_HEIGHT);

    ChartHelper.apply(widget, rectangle, margins);
    panel.add(widget);
  }

  private void addNumberWidget(HasWidgets panel, Widget widget, int firstRow, int lastRow) {
    Rectangle rectangle = ChartHelper.getRectangle(0, getNumberWidth(), firstRow, lastRow,
        getRowHeight());

    Edges margins = new Edges();
    margins.setRight(ChartHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(ChartHelper.ROW_SEPARATOR_HEIGHT);

    ChartHelper.apply(widget, rectangle, margins);
    panel.add(widget);
  }

  private Widget createDayPanel(Multimap<Long, CargoEvent> dayEvents, String tripTitle) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_DAY_PANEL);

    Set<Long> countryIds = dayEvents.keySet();
    Size size = ChartHelper.splitRectangle(getDayColumnWidth(), getRowHeight(), countryIds.size());

    if (size != null) {
      for (Long countryId : countryIds) {
        Widget widget = createDayWidget(countryId, dayEvents.get(countryId), tripTitle);
        StyleUtils.setSize(widget, size.getWidth(), size.getHeight());

        panel.add(widget);
      }
    }

    return panel;
  }

  private Widget createDayWidget(Long countryId, Collection<CargoEvent> events, String tripTitle) {
    Flow widget = new Flow();
    widget.addStyleName(STYLE_DAY_WIDGET);

    String flag = showCountryFlags() ? getCountryFlag(countryId) : null;

    if (!BeeUtils.isEmpty(flag)) {
      widget.addStyleName(STYLE_DAY_FLAG);
      StyleUtils.setBackgroundImage(widget, flag);
    }

    if (!BeeUtils.isEmpty(events)) {
      if (showPlaceInfo()) {
        List<String> info = Lists.newArrayList();

        if (BeeUtils.isEmpty(flag) && DataUtils.isId(countryId)) {
          String countryLabel = getCountryLabel(countryId);
          if (!BeeUtils.isEmpty(countryLabel)) {
            info.add(countryLabel);
          }
        }

        for (CargoEvent event : events) {
          String place = event.getPlace();
          if (!BeeUtils.isEmpty(place) && !BeeUtils.containsSame(info, place)) {
            info.add(place);
          }

          String terminal = event.getTerminal();
          if (!BeeUtils.isEmpty(terminal) && BeeUtils.containsSame(info, terminal)) {
            info.add(terminal);
          }
        }

        if (!info.isEmpty()) {
          CustomDiv label = new CustomDiv(STYLE_DAY_LABEL);
          label.setText(BeeUtils.join(BeeConst.STRING_SPACE, info));

          widget.add(label);
        }
      }

      List<String> title = Lists.newArrayList();

      Multimap<Freight, CargoEvent> eventsByFreight = LinkedListMultimap.create();
      for (CargoEvent event : events) {
        eventsByFreight.put(event.getFreight(), event);
      }

      for (Freight freight : eventsByFreight.keySet()) {
        EnumSet<CargoEvent.Type> freightEventTypes = EnumSet.noneOf(CargoEvent.Type.class);
        Map<CargoHandling, EnumSet<CargoEvent.Type>> handlingEvents = Maps.newHashMap();

        for (CargoEvent event : eventsByFreight.get(freight)) {
          CargoEvent.Type eventType = event.isLoading()
              ? CargoEvent.Type.LOADING : CargoEvent.Type.UNLOADING;

          if (event.isFreightEvent()) {
            freightEventTypes.add(eventType);
          } else if (handlingEvents.containsKey(event.getCargoHandling())) {
            handlingEvents.get(event.getCargoHandling()).add(eventType);
          } else {
            handlingEvents.put(event.getCargoHandling(), EnumSet.of(eventType));
          }
        }

        String freightLoading = freightEventTypes.contains(CargoEvent.Type.LOADING)
            ? getLoadingInfo(freight) : null;
        String freightUnloading = freightEventTypes.contains(CargoEvent.Type.UNLOADING)
            ? getUnloadingInfo(freight) : null;

        if (!title.isEmpty()) {
          title.add(BeeConst.STRING_NBSP);
        }
        title.add(freight.getTitle(freightLoading, freightUnloading, false));

        if (!handlingEvents.isEmpty()) {
          title.add(BeeConst.STRING_NBSP);

          for (Map.Entry<CargoHandling, EnumSet<Type>> entry : handlingEvents.entrySet()) {
            String chLoading = entry.getValue().contains(CargoEvent.Type.LOADING)
                ? getLoadingInfo(entry.getKey()) : null;
            String chUnloading = entry.getValue().contains(CargoEvent.Type.UNLOADING)
                ? getUnloadingInfo(entry.getKey()) : null;

            title.add(entry.getKey().getTitle(chLoading, chUnloading));
          }
        }
      }
      
      if (!BeeUtils.isEmpty(tripTitle)) {
        title.add(BeeConst.STRING_NBSP);
        title.add(tripTitle);
      }
      
      if (!title.isEmpty()) {
        widget.setTitle(BeeUtils.join(BeeConst.STRING_EOL, title));
      }
    }

    if (widget.isEmpty() && BeeUtils.isEmpty(flag)) {
      widget.addStyleName(STYLE_DAY_EMPTY);
    }

    return widget;
  }

  private Widget createFreightWidget(Freight freight) {
    final Flow panel = new Flow();
    panel.addStyleName(STYLE_FREIGHT_PANEL);
    setItemWidgetColor(freight, panel);

    panel.setTitle(freight.getTitle(getLoadingInfo(freight), getUnloadingInfo(freight), true));

    final Long cargoId = freight.getCargoId();

    panel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openDataRow(event, VIEW_ORDER_CARGO, cargoId);
      }
    });

    DndHelper.makeSource(panel, DATA_TYPE_FREIGHT, cargoId, null, freight, STYLE_FREIGHT_DRAG,
        true);
    
    DndHelper.makeTarget(panel, FREIGHT_ACCEPTS_DROP_TYPES, STYLE_FREIGHT_OVER,
        new Predicate<Long>() {
          @Override
          public boolean apply(Long input) {
            return mayDropOnFreight(cargoId);
          }
        }, new Procedure<Long>() {
          @Override
          public void call(Long parameter) {
            panel.removeStyleName(STYLE_FREIGHT_OVER);
            dropOnFreight(cargoId);
          }
        });
    
    Range<JustDate> freightRange =
        ChartHelper.normalizedIntersection(freight.getRange(), getVisibleRange());
    if (freightRange == null) {
      return panel;
    }

    Multimap<JustDate, CargoEvent> freightLayout = splitFreightByDate(freight, freightRange);
    if (freightLayout.isEmpty()) {
      return panel;
    }

    for (JustDate date : freightLayout.keySet()) {
      Multimap<Long, CargoEvent> dayLayout = splitByCountry(freightLayout.get(date));
      if (!dayLayout.isEmpty()) {
        Widget dayWidget = createDayPanel(dayLayout, freight.getTripTitle());

        StyleUtils.setLeft(dayWidget, getRelativeLeft(freightRange, date));
        StyleUtils.setWidth(dayWidget, getDayColumnWidth());

        panel.add(dayWidget);
      }
    }

    return panel;
  }

  private IdentifiableWidget createInfoWidget(Vehicle vehicle, boolean hasOverlap) {
    Simple panel = new Simple();
    panel.addStyleName(STYLE_INFO_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_INFO_OVERLAP);
    }

    final Long vehicleId = vehicle.getId();

    BeeLabel label = new BeeLabel(vehicle.getInfo());
    label.addStyleName(STYLE_INFO_LABEL);

    UiHelper.maybeSetTitle(label, vehicle.getTitle());

    label.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openDataRow(event, VIEW_VEHICLES, vehicleId);
      }
    });

    panel.add(label);

    return panel;
  }

  private IdentifiableWidget createNumberWidget(final Vehicle vehicle, boolean hasOverlap) {
    final Simple panel = new Simple();
    panel.addStyleName(STYLE_NUMBER_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_NUMBER_OVERLAP);
    }

    final Long vehicleId = vehicle.getId();

    DndDiv label = new DndDiv(STYLE_NUMBER_LABEL);
    label.setText(vehicle.getNumber());

    UiHelper.maybeSetTitle(label, vehicle.getTitle());

    label.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openDataRow(event, VIEW_VEHICLES, vehicleId);
      }
    });

    DndHelper.makeSource(label, getDataType(), vehicleId, null, vehicle.getNumber(),
        STYLE_VEHICLE_DRAG, true);
    
    panel.add(label);

    DndHelper.makeTarget(panel, VEHICLE_ACCEPTS_DROP_TYPES, STYLE_VEHICLE_OVER,
        new Predicate<Long>() {
          @Override
          public boolean apply(Long input) {
            return mayDropOnVehicle(vehicleId);
          }
        }, new Procedure<Long>() {
          @Override
          public void call(Long parameter) {
            panel.removeStyleName(STYLE_VEHICLE_OVER);
            dropOnVehicle(vehicle);
          }
        });

    return panel;
  }
  
  private Widget createTripWidget(Trip trip) {
    final Flow panel = new Flow();
    panel.addStyleName(STYLE_TRIP_PANEL);
    setItemWidgetColor(trip, panel);

    panel.setTitle(trip.getTitle());

    final Long tripId = trip.getTripId();

    panel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openDataRow(event, VIEW_TRIPS, tripId);
      }
    });

    DndHelper.makeSource(panel, DATA_TYPE_TRIP, tripId, null, trip, STYLE_TRIP_DRAG, true);
    
    DndHelper.makeTarget(panel, TRIP_ACCEPTS_DROP_TYPES, STYLE_TRIP_OVER,
        new Predicate<Long>() {
          @Override
          public boolean apply(Long input) {
            return mayDropOnTrip(tripId);
          }
        }, new Procedure<Long>() {
          @Override
          public void call(Long parameter) {
            panel.removeStyleName(STYLE_TRIP_OVER);
            dropOnTrip(tripId);
          }
        });
    
    Range<JustDate> tripRange =
        ChartHelper.normalizedIntersection(trip.getRange(), getVisibleRange());
    if (tripRange == null) {
      return panel;
    }

    List<Range<JustDate>> voidRanges;

    if (freights.containsKey(tripId)) {
      Multimap<JustDate, CargoEvent> tripLayout = splitTripByDate(tripId, tripRange);
      Set<JustDate> eventDates = tripLayout.keySet();

      for (JustDate date : eventDates) {
        Multimap<Long, CargoEvent> dayLayout = splitByCountry(tripLayout.get(date));
        if (!dayLayout.isEmpty()) {
          Widget dayWidget = createDayPanel(dayLayout, trip.getTitle());

          StyleUtils.setLeft(dayWidget, getRelativeLeft(tripRange, date));
          StyleUtils.setWidth(dayWidget, getDayColumnWidth());

          panel.add(dayWidget);
        }
      }

      voidRanges = getVoidRanges(tripRange, eventDates, freights.get(tripId));

    } else {
      voidRanges = Lists.newArrayList();
      voidRanges.add(tripRange);
    }

    for (Range<JustDate> voidRange : voidRanges) {
      Widget voidWidget = new CustomDiv(STYLE_TRIP_VOID);

      StyleUtils.setLeft(voidWidget, getRelativeLeft(tripRange, voidRange.lowerEndpoint()));
      StyleUtils.setWidth(voidWidget, ChartHelper.getSize(voidRange) * getDayColumnWidth());

      panel.add(voidWidget);
    }

    return panel;
  }

  private List<ChartRowLayout> doLayout() {
    List<ChartRowLayout> result = Lists.newArrayList();
    Range<JustDate> range = getVisibleRange();

    for (int vehicleIndex = 0; vehicleIndex < vehicles.size(); vehicleIndex++) {
      Vehicle vehicle = vehicles.get(vehicleIndex);

      if (ChartHelper.isActive(vehicle, range)) {
        ChartRowLayout layout = new ChartRowLayout(vehicleIndex);

        if (trips.containsKey(vehicle.getId())) {
          if (separateCargo()) {
            Collection<Trip> vehicleTrips = trips.get(vehicle.getId());

            for (Trip trip : vehicleTrips) {
              if (freights.containsKey(trip.getTripId())) {
                layout.addItems(ChartHelper.getActiveItems(freights.get(trip.getTripId()), range),
                    range, ChartRowLayout.FREIGHT_BLENDER);
              }
            }

          } else {
            layout.addItems(ChartHelper.getActiveItems(trips.get(vehicle.getId()), range), range);
          }
        }

        layout.addInactivity(ChartHelper.getInactivity(vehicle, range), range);
        if (services.containsKey(vehicle.getId())) {
          layout.addInactivity(ChartHelper.getActiveItems(services.get(vehicle.getId()), range),
              range);
        }

        result.add(layout);
      }
    }
    return result;
  }
  
  private void dropOnFreight(Long cargoId) {
  }

  private void dropOnTrip(Long tripId) {
  }
  
  private void dropOnVehicle(Vehicle vehicle) {
    if (DndHelper.isDataType(DATA_TYPE_TRIP) && DndHelper.getData() instanceof Trip) {
      ((Trip) DndHelper.getData()).dropOnVehicle(vehicleType, vehicle);

    } else if (DndHelper.isDataType(DATA_TYPE_FREIGHT) && DndHelper.getData() instanceof Freight) {

    } else if (DndHelper.isDataType(DATA_TYPE_ORDER_CARGO) 
        && DndHelper.getData() instanceof OrderCargo) {
    }
  }

  private int getInfoWidth() {
    return infoWidth;
  }

  private int getNumberWidth() {
    return numberWidth;
  }
  
  private int getRelativeLeft(Range<JustDate> parent, JustDate date) {
    return TimeUtils.dayDiff(parent.lowerEndpoint(), date) * getDayColumnWidth();
  }

  private List<Range<JustDate>> getVoidRanges(Range<JustDate> tripRange,
      Set<JustDate> eventDates, Collection<Freight> tripFreights) {

    List<Range<JustDate>> result = Lists.newArrayList();
    int tripDays = ChartHelper.getSize(tripRange);

    Set<JustDate> usedDates = Sets.newHashSet();

    if (!BeeUtils.isEmpty(eventDates)) {
      if (eventDates.size() >= tripDays) {
        return result;
      }
      usedDates.addAll(eventDates);
    }

    if (!BeeUtils.isEmpty(tripFreights)) {
      for (Freight freight : tripFreights) {
        if (ChartHelper.isActive(freight, tripRange)) {
          Range<JustDate> freightRange =
              ChartHelper.normalizedIntersection(freight.getRange(), tripRange);
          if (freightRange == null) {
            continue;
          }

          int freightDays = ChartHelper.getSize(freightRange);
          if (freightDays >= tripDays) {
            return result;
          }

          for (int i = 0; i < freightDays; i++) {
            usedDates.add(TimeUtils.nextDay(freightRange.lowerEndpoint(), i));
          }

          if (usedDates.size() >= tripDays) {
            return result;
          }
        }
      }
    }

    if (BeeUtils.isEmpty(usedDates)) {
      result.add(tripRange);
      return result;
    }

    List<JustDate> dates = Lists.newArrayList(usedDates);
    Collections.sort(dates);

    for (int i = 0; i < dates.size(); i++) {
      JustDate date = dates.get(i);

      if (i == 0 && TimeUtils.isMore(date, tripRange.lowerEndpoint())) {
        result.add(Range.closed(tripRange.lowerEndpoint(), TimeUtils.previousDay(date)));
      }

      if (i > 0 && TimeUtils.dayDiff(dates.get(i - 1), date) > 1) {
        result.add(Range.closed(TimeUtils.nextDay(dates.get(i - 1)), TimeUtils.previousDay(date)));
      }

      if (i == dates.size() - 1 && TimeUtils.isLess(date, tripRange.upperEndpoint())) {
        result.add(Range.closed(TimeUtils.nextDay(date), tripRange.upperEndpoint()));
      }
    }

    return result;
  }

  private boolean mayDropOnFreight(Long cargoId) {
    return true;
  }

  private boolean mayDropOnTrip(Long tripId) {
    return true;
  }

  private boolean mayDropOnVehicle(Long vehicleId) {
    if (DndHelper.isDataType(DATA_TYPE_TRIP) && DndHelper.getData() instanceof Trip) {
      return !Objects.equal(vehicleId, ((Trip) DndHelper.getData()).getVehicleId(vehicleType));

    } else if (DndHelper.isDataType(DATA_TYPE_FREIGHT) && DndHelper.getData() instanceof Freight) {
      return !Objects.equal(vehicleId, ((Freight) DndHelper.getData()).getVehicleId(vehicleType));

    } else if (DndHelper.isDataType(DATA_TYPE_ORDER_CARGO) 
        && DndHelper.getData() instanceof OrderCargo) {
      return true;
      
    } else {
      return false;
    }
  }

  private void onInfoResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int maxLeft = getNumberWidth() + 300;
    if (getChartWidth() > 0) {
      maxLeft = Math.min(maxLeft, getChartLeft() + getChartWidth() / 2);
    }

    int newLeft = BeeUtils.clamp(oldLeft + delta, getNumberWidth() + 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      int infoPx = newLeft - getNumberWidth() + ChartHelper.DEFAULT_MOVER_WIDTH;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : infoPanels) {
          StyleUtils.setWidth(id, infoPx - ChartHelper.DEFAULT_MOVER_WIDTH);
        }
      }

      if (event.isFinished() && updateSetting(getInfoWidthColumnName(), infoPx)) {
        setInfoWidth(infoPx);
        render(false);
      }
    }
  }

  private void onNumberResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int newLeft = BeeUtils.clamp(oldLeft + delta, 1,
        getChartLeft() - ChartHelper.DEFAULT_MOVER_WIDTH * 2 - 1);

    if (newLeft != oldLeft || event.isFinished()) {
      int numberPx = newLeft + ChartHelper.DEFAULT_MOVER_WIDTH;
      int infoPx = getChartLeft() - numberPx;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : numberPanels) {
          StyleUtils.setWidth(id, numberPx - ChartHelper.DEFAULT_MOVER_WIDTH);
        }

        for (String id : infoPanels) {
          Element element = Document.get().getElementById(id);
          if (element != null) {
            StyleUtils.setLeft(element, numberPx);
            StyleUtils.setWidth(element, infoPx - ChartHelper.DEFAULT_MOVER_WIDTH);
          }
        }
      }

      if (event.isFinished() && updateSettings(getNumberWidthColumnName(), numberPx,
          getInfoWidthColumnName(), infoPx)) {
        setNumberWidth(numberPx);
        setInfoWidth(infoPx);
      }
    }
  }

  private boolean separateCargo() {
    return separateCargo;
  }

  private void setInfoWidth(int infoWidth) {
    this.infoWidth = infoWidth;
  }

  private void setNumberWidth(int numberWidth) {
    this.numberWidth = numberWidth;
  }

  private void setSeparateCargo(boolean separateCargo) {
    this.separateCargo = separateCargo;
  }

  private void setShowCountryFlags(boolean showCountryFlags) {
    this.showCountryFlags = showCountryFlags;
  }

  private void setShowPlaceInfo(boolean showPlaceInfo) {
    this.showPlaceInfo = showPlaceInfo;
  }

  private boolean showCountryFlags() {
    return showCountryFlags;
  }

  private boolean showPlaceInfo() {
    return showPlaceInfo;
  }

  private Multimap<Long, CargoEvent> splitByCountry(Collection<CargoEvent> events) {
    Multimap<Long, CargoEvent> result = LinkedListMultimap.create();
    if (BeeUtils.isEmpty(events)) {
      return result;
    }

    for (CargoEvent event : events) {
      if (event.isLoading() && event.isFreightEvent()) {
        result.put(event.getCountryId(), event);
      }
    }

    for (CargoEvent event : events) {
      if (event.isLoading() && event.isHandlingEvent()) {
        result.put(event.getCountryId(), event);
      }
    }
    for (CargoEvent event : events) {
      if (event.isUnloading() && event.isHandlingEvent()) {
        result.put(event.getCountryId(), event);
      }
    }

    for (CargoEvent event : events) {
      if (event.isUnloading() && event.isFreightEvent()) {
        result.put(event.getCountryId(), event);
      }
    }

    return result;
  }

  private Multimap<JustDate, CargoEvent> splitFreightByDate(Freight freight, Range<JustDate> range) {
    Multimap<JustDate, CargoEvent> result = ArrayListMultimap.create();
    if (freight == null || range == null || range.isEmpty()) {
      return result;
    }

    if (freight.getLoadingDate() != null && range.contains(freight.getLoadingDate())) {
      result.put(freight.getLoadingDate(), new CargoEvent(freight, null, true));
    }

    if (freight.getUnloadingDate() != null && range.contains(freight.getUnloadingDate())) {
      result.put(freight.getUnloadingDate(), new CargoEvent(freight, null, false));
    }

    if (handling.containsKey(freight.getCargoId())) {
      for (CargoHandling ch : handling.get(freight.getCargoId())) {
        if (ch.getLoadingDate() != null && range.contains(ch.getLoadingDate())) {
          result.put(ch.getLoadingDate(), new CargoEvent(freight, ch, true));
        }

        if (ch.getUnloadingDate() != null && range.contains(ch.getUnloadingDate())) {
          result.put(ch.getUnloadingDate(), new CargoEvent(freight, ch, false));
        }
      }
    }

    return result;
  }

  private Multimap<JustDate, CargoEvent> splitTripByDate(Long tripId, Range<JustDate> range) {
    Multimap<JustDate, CargoEvent> result = ArrayListMultimap.create();
    if (tripId == null || range == null || range.isEmpty() || !freights.containsKey(tripId)) {
      return result;
    }

    Collection<Freight> tripCargos = freights.get(tripId);
    for (Freight freight : tripCargos) {
      result.putAll(splitFreightByDate(freight, range));
    }

    return result;
  }
}
