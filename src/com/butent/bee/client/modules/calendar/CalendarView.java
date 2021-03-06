package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.modules.calendar.event.AppointmentEvent;
import com.butent.bee.client.modules.calendar.event.CopyEvent;
import com.butent.bee.client.modules.calendar.event.TimeBlockClickEvent;
import com.butent.bee.client.modules.calendar.event.UpdateEvent;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.State;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;

import java.util.ArrayList;
import java.util.List;

public abstract class CalendarView {

  public enum Type {
    DAY, MONTH, RESOURCE
  }

  private final List<ItemWidget> itemWidgets = new ArrayList<>();

  private CalendarWidget calendarWidget;

  public void attach(CalendarWidget widget) {
    this.calendarWidget = widget;
  }

  public void createAppointment(DateTime start, Long attendeeId) {
    if (getCalendarWidget() != null) {
      TimeBlockClickEvent.fire(getCalendarWidget(), start, attendeeId);
    }
  }

  public abstract void doLayout(long calendarId);

  public abstract void doScroll();

  public abstract void doSizing();

  public List<ItemWidget> getItemWidgets() {
    return itemWidgets;
  }

  public CalendarWidget getCalendarWidget() {
    return calendarWidget;
  }

  public abstract Widget getScrollArea();

  public CalendarSettings getSettings() {
    return getCalendarWidget().getSettings();
  }

  public abstract String getStyleName();

  public abstract Type getType();

  public abstract Range<DateTime> getVisibleRange();

  public abstract boolean onClick(long calendarId, Element element, Event event);

  public abstract void onClock();

  public void openItem(CalendarItem item) {
    if (getCalendarWidget() != null) {
      OpenEvent.fire(getCalendarWidget(), item);
    }
  }

  public abstract Pair<DateTime, Long> resolveCoordinates(int x, int y);

  public void copyAppointment(Appointment appointment, DateTime newStart, DateTime newEnd) {
    CopyEvent.fire(getCalendarWidget(), appointment, newStart, newEnd);
  }

  public void updateAppointment(Appointment appointment, DateTime newStart, DateTime newEnd,
      int oldColumnIndex, int newColumnIndex) {
    boolean updated = UpdateEvent.fire(getCalendarWidget(), appointment, newStart, newEnd,
        oldColumnIndex, newColumnIndex);

    if (updated) {
      AppointmentEvent.fire(appointment, State.CHANGED, getCalendarWidget());
    }
  }

  protected List<CalendarItem> getItems() {
    return getCalendarWidget().getItems();
  }

  protected JustDate getDate() {
    return getCalendarWidget().getDate();
  }

  protected int getDisplayedDays() {
    return getCalendarWidget().getDisplayedDays();
  }
}