package com.butent.bee.shared.modules.payroll;

import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;

import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PayrollUtils {

  private static final class WSEntry {

    private final Long timeRange;
    private final Long timeCard;

    private final Long from;
    private final Long until;
    private final Long duration;

    private WSEntry(Long timeRange, Long timeCard, Long from, Long until, Long duration) {
      this.timeRange = timeRange;
      this.timeCard = timeCard;
      this.from = from;
      this.until = until;
      this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof WSEntry)) {
        return false;
      }

      WSEntry wsEntry = (WSEntry) o;

      if (timeRange != null ? !timeRange.equals(wsEntry.timeRange) : wsEntry.timeRange != null) {
        return false;
      }
      if (timeCard != null ? !timeCard.equals(wsEntry.timeCard) : wsEntry.timeCard != null) {
        return false;
      }
      if (from != null ? !from.equals(wsEntry.from) : wsEntry.from != null) {
        return false;
      }
      if (until != null ? !until.equals(wsEntry.until) : wsEntry.until != null) {
        return false;
      }
      return duration != null ? duration.equals(wsEntry.duration) : wsEntry.duration == null;
    }

    @Override
    public int hashCode() {
      int result = timeRange != null ? timeRange.hashCode() : 0;
      result = 31 * result + (timeCard != null ? timeCard.hashCode() : 0);
      result = 31 * result + (from != null ? from.hashCode() : 0);
      result = 31 * result + (until != null ? until.hashCode() : 0);
      result = 31 * result + (duration != null ? duration.hashCode() : 0);
      return result;
    }
  }

  public static Filter getIntersectionFilter(YearMonth ym, String col1, String col2) {
    if (ym == null || BeeUtils.anyEmpty(col1, col2) || BeeUtils.same(col1, col2)) {
      return Filter.isFalse();

    } else {
      StringList columns = StringList.of(col1, col2);
      Range<Value> range = Range.closed(new DateValue(ym.getDate()), new DateValue(ym.getLast()));

      return Filter.anyIntersects(columns, range);
    }
  }

  public static long getMillis(String from, String until, String duration) {
    Long time = TimeUtils.parseTime(duration);
    if (BeeUtils.isNonNegative(time)) {
      return time;
    }

    Long lower = TimeUtils.parseTime(from);
    if (BeeUtils.isNonNegative(lower)) {
      Long upper = TimeUtils.parseTime(until);
      if (BeeUtils.isLess(lower, upper)) {
        return upper - lower;
      }
    }

    return 0L;
  }

  public static List<List<BeeRow>> getSequel(Map<Integer, List<BeeRow>> input,
      List<BeeColumn> columns, int lower, int upper) {

    List<List<BeeRow>> sequel = new ArrayList<>();

    int trIndex = DataUtils.getColumnIndex(COL_TIME_RANGE_CODE, columns);
    int tcIndex = DataUtils.getColumnIndex(COL_TIME_CARD_CODE, columns);

    int fromIndex = DataUtils.getColumnIndex(COL_WORK_SCHEDULE_FROM, columns);
    int untilIndex = DataUtils.getColumnIndex(COL_WORK_SCHEDULE_UNTIL, columns);
    int durIndex = DataUtils.getColumnIndex(COL_WORK_SCHEDULE_DURATION, columns);

    List<Set<WSEntry>> entrySets = new ArrayList<>();
    Map<Set<WSEntry>, List<BeeRow>> entryValues = new HashMap<>();

    List<Integer> sequence = new ArrayList<>();

    int emptyValue = -1;

    for (int day = lower; day <= upper; day++) {
      List<BeeRow> value = input.get(day);
      int index = emptyValue;

      if (!BeeUtils.isEmpty(value)) {
        Set<WSEntry> entries = new HashSet<>();

        for (BeeRow row : value) {
          entries.add(new WSEntry(row.getLong(trIndex), row.getLong(tcIndex),
              TimeUtils.parseTime(row.getString(fromIndex)),
              TimeUtils.parseTime(row.getString(untilIndex)),
              TimeUtils.parseTime(row.getString(durIndex))));
        }

        if (!entries.isEmpty()) {
          index = entrySets.indexOf(entries);
          if (index < 0) {
            entrySets.add(entries);
            index = entrySets.size() - 1;
          }

          entryValues.put(entries, value);
        }
      }

      sequence.add(index);
    }

    if (entrySets.isEmpty()) {
      return sequel;
    }

    if (entrySets.size() == 1 && !sequence.contains(emptyValue)) {
      sequel.add(entryValues.get(entrySets.get(0)));
      return sequel;
    }

    LogUtils.getRootLogger().debug(sequence);

    return sequel;
  }

  public static boolean validTimeOfDay(String input) {
    Long millis = TimeUtils.parseTime(input);
    return BeeUtils.isNonNegative(millis) && BeeUtils.isLeq(millis, TimeUtils.MILLIS_PER_DAY);
  }

  private PayrollUtils() {
  }
}
