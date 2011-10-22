package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Transformable;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowFilter;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

/**
 * Is an abstract class for all specific filter applying classes, determines which type of filter to
 * apply.
 */

public abstract class Filter implements BeeSerializable, Transformable, RowFilter {

  public static Filter restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);
    String clazz = arr[0];
    String data = arr[1];
    Filter flt = null;

    if (data != null) {
      flt = Filter.getFilter(clazz);
      flt.deserialize(data);
    }
    return flt;
  }

  private static Filter getFilter(String clazz) {
    Filter flt = null;

    if (BeeUtils.getClassName(ColumnValueFilter.class).equals(clazz)) {
      flt = new ColumnValueFilter();

    } else if (BeeUtils.getClassName(ColumnColumnFilter.class).equals(clazz)) {
      flt = new ColumnColumnFilter();

    } else if (BeeUtils.getClassName(ColumnIsEmptyFilter.class).equals(clazz)) {
      flt = new ColumnIsEmptyFilter();

    } else if (BeeUtils.getClassName(NegationFilter.class).equals(clazz)) {
      flt = new NegationFilter();

    } else if (BeeUtils.getClassName(CompoundFilter.class).equals(clazz)) {
      flt = new CompoundFilter();

    } else if (BeeUtils.getClassName(IdFilter.class).equals(clazz)) {
      flt = new IdFilter();

    } else if (BeeUtils.getClassName(VersionFilter.class).equals(clazz)) {
      flt = new VersionFilter();

    } else {
      Assert.unsupported("Unsupported class name: " + clazz);
    }
    return flt;
  }

  private boolean safe = true;

  protected Filter() {
    this.safe = false;
  }

  public abstract boolean involvesColumn(String colName);

  public abstract boolean isMatch(List<? extends IsColumn> columns, IsRow row);

  public String transform() {
    return toString();
  }

  protected int getColumnIndex(String colName, List<? extends IsColumn> columns) {
    Assert.notEmpty(columns);

    for (int i = 0; i < columns.size(); i++) {
      if (BeeUtils.same(colName, columns.get(i).getId())) {
        return i;
      }
    }
    return -1;
  }

  protected String serialize(Object obj) {
    return Codec.beeSerialize(new Object[] {BeeUtils.getClassName(this.getClass()), obj});
  }

  protected void setSafe() {
    Assert.isFalse(safe);
    this.safe = true;
  }
}
