package com.butent.bee.client.view.grid;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CustomProperties;
import com.butent.bee.shared.data.HasCustomProperties;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class GridUtils {

  private static final BeeLogger logger = LogUtils.getLogger(GridUtils.class);

  static boolean containsColumn(Collection<ColumnDescription> columnDescriptions, String id) {
    return getColumnDescription(columnDescriptions, id) != null;
  }

  static ColumnDescription getColumnDescription(Collection<ColumnDescription> columnDescriptions,
      String id) {
    if (!BeeUtils.isEmpty(columnDescriptions)) {
      for (ColumnDescription columnDescription : columnDescriptions) {
        if (columnDescription.is(id)) {
          return columnDescription;
        }
      }
    }
    return null;
  }

  static ColumnInfo getColumnInfo(Collection<ColumnInfo> columns, String id) {
    if (!BeeUtils.isEmpty(columns)) {
      for (ColumnInfo columnInfo : columns) {
        if (columnInfo.is(id)) {
          return columnInfo;
        }
      }
    }
    return null;
  }

  static int getColumnIndex(List<ColumnInfo> columns, String id) {
    if (!BeeUtils.isEmpty(columns)) {
      for (int i = 0; i < columns.size(); i++) {
        if (columns.get(i).is(id)) {
          return i;
        }
      }
    }
    return BeeConst.UNDEF;
  }

  static int getIndex(List<String> names, String name) {
    int index = names.indexOf(name);
    if (index < 0) {
      logger.severe("name not found:", name);
    }
    return index;
  }

  static String normalizeValue(String value) {
    return BeeUtils.isEmpty(value) ? null : value.trim();
  }

  static void updateProperties(IsRow target, IsRow source) {
    if (!BeeUtils.isEmpty(target.getProperties())) {
      Long userId = BeeKeeper.getUser().getUserId();
      CustomProperties retain = new CustomProperties();

      for (Map.Entry<String, String> entry : target.getProperties().entrySet()) {
        if (HasCustomProperties.isUserPropertyName(entry.getKey(), userId)) {
          retain.put(entry.getKey(), entry.getValue());
        }
      }

      if (retain.isEmpty() && BeeUtils.isEmpty(source.getProperties())) {
        target.setProperties(null);

      } else {
        target.getProperties().clear();
        if (!retain.isEmpty()) {
          target.getProperties().putAll(retain);
        }
      }
    }

    if (!BeeUtils.isEmpty(source.getProperties())) {
      for (Map.Entry<String, String> entry : source.getProperties().entrySet()) {
        if (isPropertyRelevant(entry.getKey())) {
          target.setProperty(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private static boolean isPropertyRelevant(String key) {
    if (HasCustomProperties.isUserPropertyName(key)) {
      return BeeKeeper.getUser().is(HasCustomProperties.extractUserIdFromUserPropertyName(key));
    } else {
      return !BeeUtils.isEmpty(key);
    }
  }

  private GridUtils() {
  }
}
