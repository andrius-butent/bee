package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumMap;
import java.util.Map;

public final class AnalysisValue implements BeeSerializable {

  public static AnalysisValue actual(long columnId, long rowId, double value) {
    AnalysisValue av = new AnalysisValue(columnId, rowId);
    av.setActualValue(value);
    return av;
  }

  public static AnalysisValue budget(long columnId, long rowId, double value) {
    AnalysisValue av = new AnalysisValue(columnId, rowId);
    av.setBudgetValue(value);
    return av;
  }

  public static AnalysisValue of(long columnId, long rowId, Double actual, Double budget) {
    AnalysisValue av = new AnalysisValue(columnId, rowId);

    if (BeeUtils.isDouble(actual)) {
      av.setActualValue(actual);
    }
    if (BeeUtils.isDouble(budget)) {
      av.setBudgetValue(budget);
    }

    return av;
  }

  public static AnalysisValue restore(String s) {
    AnalysisValue av = new AnalysisValue();
    av.deserialize(s);
    return av;
  }

  private static String format(double value) {
    return BeeUtils.toString(value);
  }

  private enum Serial {
    COLUMN_ID, ROW_ID, COLUMN_SPLIT, ROW_SPLIT, ACTUAL_VALUE, BUDGET_VALUE
  }

  private long columnId;
  private long rowId;

  private final Map<AnalysisSplitType, AnalysisSplitValue> columnSplit =
      new EnumMap<>(AnalysisSplitType.class);
  private final Map<AnalysisSplitType, AnalysisSplitValue> rowSplit =
      new EnumMap<>(AnalysisSplitType.class);

  private String actualValue;
  private String budgetValue;

  private AnalysisValue() {
  }

  private AnalysisValue(long columnId, long rowId) {
    this.columnId = columnId;
    this.rowId = rowId;
  }

  public void add(AnalysisValue other) {
    if (other != null) {
      if (BeeUtils.isEmpty(actualValue)) {
        setActualValue(other.actualValue);
      } else if (BeeUtils.isDouble(other.actualValue)) {
        setActualValue(getActualNumber() + other.getActualNumber());
      }

      if (BeeUtils.isEmpty(budgetValue)) {
        setBudgetValue(other.budgetValue);
      } else if (BeeUtils.isDouble(other.budgetValue)) {
        setBudgetValue(getBudgetNumber() + other.getBudgetNumber());
      }
    }
  }

  public long getColumnId() {
    return columnId;
  }

  public long getRowId() {
    return rowId;
  }

  public Map<AnalysisSplitType, AnalysisSplitValue> getColumnSplit() {
    return columnSplit;
  }

  public Map<AnalysisSplitType, AnalysisSplitValue> getRowSplit() {
    return rowSplit;
  }

  public String getActualValue() {
    return actualValue;
  }

  public double getActualNumber() {
    return BeeUtils.toDouble(getActualValue());
  }

  public String getBudgetValue() {
    return budgetValue;
  }

  public double getBudgetNumber() {
    return BeeUtils.toDouble(getBudgetValue());
  }

  public void putColumnSplit(Map<AnalysisSplitType, AnalysisSplitValue> parentSplit,
      AnalysisSplitType splitType, AnalysisSplitValue splitValue) {

    if (!BeeUtils.isEmpty(parentSplit)) {
      columnSplit.putAll(parentSplit);
    }

    if (splitType != null && splitValue != null) {
      columnSplit.put(splitType, splitValue);
    }
  }

  public void putRowSplit(Map<AnalysisSplitType, AnalysisSplitValue> parentSplit,
      AnalysisSplitType splitType, AnalysisSplitValue splitValue) {

    if (!BeeUtils.isEmpty(parentSplit)) {
      rowSplit.putAll(parentSplit);
    }

    if (splitType != null && splitValue != null) {
      rowSplit.put(splitType, splitValue);
    }
  }

  private void setColumnId(long columnId) {
    this.columnId = columnId;
  }

  private void setRowId(long rowId) {
    this.rowId = rowId;
  }

  private void setActualValue(String actualValue) {
    this.actualValue = actualValue;
  }

  private void setActualValue(double value) {
    setActualValue(format(value));
  }

  private void setBudgetValue(String budgetValue) {
    this.budgetValue = budgetValue;
  }

  private void setBudgetValue(double value) {
    setBudgetValue(format(value));
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, Serial.values().length);

    for (int i = 0; i < arr.length; i++) {
      String v = arr[i];

      if (!BeeUtils.isEmpty(v)) {
        switch (Serial.values()[i]) {
          case COLUMN_ID:
            setColumnId(BeeUtils.toLong(v));
            break;
          case ROW_ID:
            setRowId(BeeUtils.toLong(v));
            break;

          case COLUMN_SPLIT:
            if (!columnSplit.isEmpty()) {
              columnSplit.clear();
            }
            columnSplit.putAll(deserializeSplit(v));
            break;
          case ROW_SPLIT:
            if (!rowSplit.isEmpty()) {
              rowSplit.clear();
            }
            rowSplit.putAll(deserializeSplit(v));
            break;

          case ACTUAL_VALUE:
            setActualValue(v);
            break;
          case BUDGET_VALUE:
            setBudgetValue(v);
            break;
        }
      }
    }
  }

  private static Map<AnalysisSplitType, AnalysisSplitValue> deserializeSplit(String s) {
    Map<AnalysisSplitType, AnalysisSplitValue> split = new EnumMap<>(AnalysisSplitType.class);

    Codec.deserializeHashMap(s).forEach((st, sv) -> {
      AnalysisSplitType type = EnumUtils.getEnumByName(AnalysisSplitType.class, st);
      if (type != null) {
        split.put(type, AnalysisSplitValue.restore(sv));
      }
    });

    return split;
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[Serial.values().length];
    int i = 0;

    for (Serial member : Serial.values()) {
      switch (member) {
        case COLUMN_ID:
          arr[i++] = getColumnId();
          break;
        case ROW_ID:
          arr[i++] = getRowId();
          break;

        case COLUMN_SPLIT:
          arr[i++] = getColumnSplit();
          break;
        case ROW_SPLIT:
          arr[i++] = getRowSplit();
          break;

        case ACTUAL_VALUE:
          arr[i++] = getActualValue();
          break;
        case BUDGET_VALUE:
          arr[i++] = getBudgetValue();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public boolean matches(AnalysisValue other) {
    return other != null
        && columnId == other.columnId
        && rowId == other.rowId
        && columnSplit.equals(other.columnSplit)
        && rowSplit.equals(other.rowSplit);
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("c", columnId, "r", rowId,
        "cs", columnSplit.isEmpty() ? BeeConst.STRING_EMPTY : columnSplit.toString(),
        "rs", rowSplit.isEmpty() ? BeeConst.STRING_EMPTY : rowSplit.toString(),
        "a", actualValue,
        "b", budgetValue);
  }
}
