package com.butent.bee.shared.data.view;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

/**
 * Enables to get main information about data objects, like row count, ID column etc.
 */

public class DataInfo implements BeeSerializable, Comparable<DataInfo> {
  public static DataInfo restore(String s) {
    Assert.notEmpty(s);
    DataInfo ti = new DataInfo();
    ti.deserialize(s);
    return ti;
  }

  private String name;
  private String idColumn;

  private int rowCount;

  public DataInfo(String name, String idColumn, int rowCount) {
    this.name = name;
    this.idColumn = idColumn;
    this.rowCount = rowCount;
  }

  private DataInfo() {
  }

  public int compareTo(DataInfo o) {
    if (o == null) {
      return BeeConst.COMPARE_MORE;
    }
    return BeeUtils.compareNormalized(getName(), o.getName());
  }

  public void deserialize(String s) {
    String[] arr = Codec.beeDeserialize(s);
    Assert.lengthEquals(arr, 3);

    name = arr[0];
    idColumn = arr[1];
    setRowCount(BeeUtils.toInt(arr[2]));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DataInfo)) {
      return false;
    }
    return BeeUtils.same(getName(), ((DataInfo) obj).getName());
  }

  public String getIdColumn() {
    return idColumn;
  }

  public String getName() {
    return name;
  }

  public int getRowCount() {
    return rowCount;
  }

  @Override
  public int hashCode() {
    return BeeUtils.normalize(getName()).hashCode();
  }

  public String serialize() {
    return Codec.beeSerializeAll(getName(), getIdColumn(), getRowCount());
  }

  public void setRowCount(int rowCount) {
    this.rowCount = rowCount;
  }
}
