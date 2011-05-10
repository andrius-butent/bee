package com.butent.bee.shared;

import java.util.Date;

/**
 * Requires any implementing classes to be able to get date formats in {@code JustDate},
 * {@code DateTime} and standard Java date type.
 */

public interface HasDateValue {
  HasDateValue fromDate(JustDate justDate);

  HasDateValue fromDateTime(DateTime dateTime);

  HasDateValue fromJava(Date date);

  JustDate getDate();

  DateTime getDateTime();

  Date getJava();
}
