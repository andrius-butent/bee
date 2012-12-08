package com.butent.bee.client.data;

import com.google.common.collect.Lists;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcParameter;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.List;

/**
 * Contains methods for getting {@code RowSets} and making POST requests.
 */

public class Queries {

  /**
   * Requires implementing classes to have {@code onResponse) method. 
   */
  public abstract static class IntCallback extends Callback<Integer> {
  }

  public abstract static class RowSetCallback extends Callback<BeeRowSet> {
  }

  private static final BeeLogger logger = LogUtils.getLogger(Queries.class);

  private static final int RESPONSE_FROM_CACHE = 0;

  public static boolean checkResponse(String service, String viewName, ResponseObject response,
      Class<?> clazz, Callback<?> callback) {

    if (response == null) {
      error(callback, Lists.newArrayList(service, viewName, "response is null"));
      return false;

    } else if (response.hasErrors()) {
      if (callback != null) {
        callback.onFailure(response.getErrors());
      }
      return false;

    } else if (clazz != null && !response.hasResponse(clazz)) {
      error(callback, Lists.newArrayList(service, viewName, "response type:", response.getType(),
          "expected:", NameUtils.getClassName(clazz)));
      return false;

    } else {
      return true;
    }
  }

  public static BeeRowSet createRowSetForInsert(String viewName, List<BeeColumn> columns,
      IsRow row) {
    return createRowSetForInsert(viewName, columns, row, null, false);
  }

  public static BeeRowSet createRowSetForInsert(String viewName, List<BeeColumn> columns,
      IsRow row, Collection<String> alwaysInclude, boolean addProperties) {
    List<BeeColumn> newColumns = Lists.newArrayList();
    List<String> values = Lists.newArrayList();

    for (int i = 0; i < columns.size(); i++) {
      BeeColumn column = columns.get(i);
      if (!column.isWritable()) {
        continue;
      }

      String value = row.getString(i);
      if (!BeeUtils.isEmpty(value)
          || alwaysInclude != null && alwaysInclude.contains(column.getId())) {
        newColumns.add(column);
        values.add(value);
      }
    }
    if (newColumns.isEmpty()) {
      return null;
    }

    BeeRow newRow = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);
    if (addProperties && row.getProperties() != null) {
      newRow.setProperties(row.getProperties().copy());
    }

    BeeRowSet rs = new BeeRowSet(viewName, newColumns);
    rs.addRow(newRow);
    return rs;
  }

  public static void delete(final String viewName, Filter filter, final IntCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(filter, "Delete: filter required");

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName,
        Service.VAR_VIEW_WHERE, filter.serialize());
    ParameterList parameters = new ParameterList(Service.DELETE, RpcParameter.Section.DATA, lst);

    BeeKeeper.getRpc().makePostRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(Service.DELETE, viewName, response, Integer.class, callback)) {
          int responseCount = BeeUtils.toInt((String) response.getResponse());
          logger.info(viewName, "deleted", responseCount, "rows");
          if (callback != null) {
            callback.onSuccess(responseCount);
          }
        }
      }
    });
  }

  public static void deleteRow(String viewName, long rowId, long version) {
    deleteRow(viewName, rowId, version, null);
  }

  public static void deleteRow(String viewName, long rowId, long version, IntCallback callback) {
    deleteRows(viewName, Lists.newArrayList(new RowInfo(rowId, version)), callback);
  }

  public static void deleteRows(String viewName, Collection<RowInfo> rows) {
    deleteRows(viewName, rows, null);
  }

  public static void deleteRows(final String viewName, Collection<RowInfo> rows,
      final IntCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(rows);

    final int requestCount = rows.size();
    Assert.isPositive(requestCount);

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName,
        Service.VAR_VIEW_ROWS, Codec.beeSerialize(rows));

    BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.DELETE_ROWS,
        RpcParameter.Section.DATA, lst),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (checkResponse(Service.DELETE_ROWS, viewName, response, Integer.class, callback)) {
              int responseCount = BeeUtils.toInt((String) response.getResponse());
              String message;

              if (responseCount == requestCount) {
                message = BeeUtils.joinWords(viewName, "deleted", responseCount, "rows");
                logger.info(message);
              } else {
                message = BeeUtils.joinWords(viewName, "deleted", responseCount, "rows of",
                    requestCount, "requested");
                logger.warning(message);
              }

              if (callback != null) {
                if (responseCount > 0) {
                  callback.onSuccess(responseCount);
                } else {
                  callback.onFailure(message);
                }
              }
            }
          }
        });
  }

  public static void getRow(final String viewName, final long rowId, List<String> columns,
      final RowCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(callback);

    final String columnNames;
    if (BeeUtils.isEmpty(columns)) {
      columnNames = null;
    } else {
      columnNames = BeeUtils.join(Service.VIEW_COLUMN_SEPARATOR, columns);
    }

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName,
        Service.VAR_VIEW_ROW_ID, BeeUtils.toString(rowId));
    if (!BeeUtils.isEmpty(columnNames)) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_COLUMNS, columnNames);
    }

    BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.QUERY,
        RpcParameter.Section.DATA, lst),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (checkResponse(Service.QUERY, viewName, response, BeeRowSet.class, callback)) {
              BeeRowSet rs = BeeRowSet.restore((String) response.getResponse());
              if (rs.getNumberOfRows() == 1) {
                callback.onSuccess(rs.getRow(0));
              } else {
                error(callback, Lists.newArrayList("Get Row:", viewName, "id " + rowId,
                    "response number of rows: " + rs.getNumberOfRows()));
              }
            }
          }
        });
  }

  public static void getRow(String viewName, long rowId, RowCallback callback) {
    getRow(viewName, rowId, null, callback);
  }

  public static void getRowCount(final String viewName, final Filter filter,
      final IntCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(callback);

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName);
    if (filter != null) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_WHERE, filter.serialize());
    }

    BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.COUNT_ROWS,
        RpcParameter.Section.DATA, lst),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (checkResponse(Service.COUNT_ROWS, viewName, response, Integer.class, callback)) {
              int rowCount = BeeUtils.toInt((String) response.getResponse());
              logger.info(viewName, filter, "row count:", rowCount);
              callback.onSuccess(rowCount);
            }
          }
        });
  }

  public static void getRowCount(String viewName, final IntCallback callback) {
    getRowCount(viewName, null, callback);
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      CachingPolicy cachingPolicy, RowSetCallback callback) {
    return getRowSet(viewName, columns, filter, order, BeeConst.UNDEF, BeeConst.UNDEF,
        cachingPolicy, callback);
  }

  public static int getRowSet(final String viewName, List<String> columns, final Filter filter,
      final Order order, final int offset, final int limit, final CachingPolicy cachingPolicy,
      Collection<Property> options, final RowSetCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(callback);

    final String columnNames;
    if (BeeUtils.isEmpty(columns)) {
      columnNames = null;
    } else {
      columnNames = BeeUtils.join(Service.VIEW_COLUMN_SEPARATOR, columns);
    }

    if (cachingPolicy != null && cachingPolicy.doRead() && BeeUtils.isEmpty(columnNames)
        && BeeUtils.isEmpty(options)) {
      BeeRowSet rowSet = Global.getCache().getRowSet(viewName, filter, order, offset, limit);
      if (rowSet != null) {
        callback.onSuccess(rowSet);
        return RESPONSE_FROM_CACHE;
      }
    }

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName);
    if (!BeeUtils.isEmpty(columnNames)) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_COLUMNS, columnNames);
    }

    if (filter != null) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_WHERE, filter.serialize());
    }
    if (order != null) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_ORDER, order.serialize());
    }

    if (offset >= 0 && limit > 0) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_OFFSET, offset,
          Service.VAR_VIEW_LIMIT, limit);
    }

    if (!BeeUtils.isEmpty(options)) {
      lst.addAll(options);
    }

    return BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.QUERY,
        RpcParameter.Section.DATA, lst),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (checkResponse(Service.QUERY, viewName, response, BeeRowSet.class, callback)) {
              BeeRowSet rs = BeeRowSet.restore((String) response.getResponse());
              callback.onSuccess(rs);

              if (cachingPolicy != null && cachingPolicy.doWrite()
                  && BeeUtils.isEmpty(columnNames)) {
                Global.getCache().add(Data.getDataInfo(viewName), rs, filter, order, offset, limit);
              }
            }
          }
        });
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      int offset, int limit, CachingPolicy cachingPolicy, RowSetCallback callback) {
    return getRowSet(viewName, columns, filter, order, offset, limit, cachingPolicy, null,
        callback);
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      int offset, int limit, RowSetCallback callback) {
    return getRowSet(viewName, columns, filter, order, offset, limit, CachingPolicy.NONE, callback);
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      RowSetCallback callback) {
    return getRowSet(viewName, columns, filter, order, CachingPolicy.NONE, callback);
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter,
      RowSetCallback callback) {
    return getRowSet(viewName, columns, filter, null, callback);
  }

  public static int getRowSet(String viewName, List<String> columns, RowSetCallback callback) {
    return getRowSet(viewName, columns, null, null, callback);
  }

  public static int insert(String viewName, List<BeeColumn> columns, IsRow row,
      RowCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notEmpty(columns);
    Assert.notNull(row);

    BeeRowSet rs = createRowSetForInsert(viewName, columns, row);
    if (rs == null) {
      if (callback != null) {
        callback.onFailure(viewName, "nothing to insert");
      }
      return 0;
    }

    insertRow(rs, callback);
    return rs.getNumberOfColumns();
  }

  public static void insert(String viewName, List<BeeColumn> columns, List<String> values,
      final RowCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notEmpty(columns);
    Assert.notEmpty(values);
    Assert.isTrue(columns.size() == values.size());

    BeeRowSet rs = new BeeRowSet(columns);
    rs.setViewName(viewName);
    rs.addRow(0, ArrayUtils.toArray(values));

    insertRow(rs, callback);
  }

  public static void insertRow(BeeRowSet rowSet, RowCallback callback) {
    doRow(Service.INSERT_ROW, rowSet, callback);
  }

  public static boolean isResponseFromCache(int id) {
    return id == RESPONSE_FROM_CACHE;
  }

  public static void update(final String viewName, Filter filter, String column, Value value,
      final IntCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(filter);
    Assert.notEmpty(column);
    Assert.notNull(value);

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName,
        Service.VAR_VIEW_WHERE, filter.serialize(), Service.VAR_COLUMN, column,
        Service.VAR_VALUE, value.serialize());
    ParameterList parameters = new ParameterList(Service.UPDATE, RpcParameter.Section.DATA, lst);

    BeeKeeper.getRpc().makePostRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        int responseCount = BeeUtils.toInt((String) response.getResponse());
        logger.info(viewName, "updated", responseCount, "rows");
        if (callback != null) {
          callback.onSuccess(responseCount);
        }
      }
    });
  }
  
  public static int update(String viewName, List<BeeColumn> columns, IsRow oldRow, IsRow newRow,
      RowCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notEmpty(columns);

    Assert.notNull(oldRow);
    Assert.notNull(newRow);

    BeeRowSet rs = DataUtils.getUpdated(viewName, columns, oldRow, newRow);
    if (rs == null) {
      return 0;
    }

    updateRow(rs, callback);
    return rs.getNumberOfColumns();
  }

  public static void update(String viewName, long rowId, long version, List<BeeColumn> columns,
      List<String> oldValues, List<String> newValues, RowCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(columns);
    Assert.notNull(oldValues);
    Assert.notNull(newValues);

    int cc = columns.size();
    Assert.isPositive(cc);
    Assert.isTrue(cc == oldValues.size());
    Assert.isTrue(cc == newValues.size());

    BeeRowSet rs = new BeeRowSet(columns);
    rs.setViewName(viewName);
    rs.addRow(rowId, version, ArrayUtils.toArray(oldValues));
    for (int i = 0; i < cc; i++) {
      rs.getRow(0).preliminaryUpdate(i, newValues.get(i));
    }

    updateRow(rs, callback);
  }

  public static void updateCell(BeeRowSet rowSet, RowCallback callback) {
    doRow(Service.UPDATE_CELL, rowSet, callback);
  }

  public static void updateRow(BeeRowSet rowSet, RowCallback callback) {
    doRow(Service.UPDATE_ROW, rowSet, callback);
  }

  private static boolean checkRowSet(String service, BeeRowSet rowSet, Callback<?> callback) {
    if (rowSet == null) {
      error(callback, Lists.newArrayList(service, "rowSet is null"));
      return false;

    } else if (BeeUtils.isEmpty(rowSet.getViewName())) {
      error(callback, Lists.newArrayList(service, "rowSet view name not specified"));
      return false;

    } else if (rowSet.getNumberOfColumns() <= 0 || rowSet.getNumberOfRows() <= 0) {
      error(callback, Lists.newArrayList(service, rowSet.getViewName(), "rowSet is empty"));
      return false;

    } else {
      return true;
    }
  }

  private static void doRow(final String service, BeeRowSet rowSet, final RowCallback callback) {
    if (!checkRowSet(service, rowSet, callback)) {
      return;
    }
    final String viewName = rowSet.getViewName();

    BeeKeeper.getRpc().sendText(service, Codec.beeSerialize(rowSet), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(service, viewName, response, BeeRow.class, callback)) {
          BeeRow row = BeeRow.restore((String) response.getResponse());
          if (row == null) {
            error(callback, Lists.newArrayList(service, viewName, "cannot restore row"));
          } else if (callback != null) {
            callback.onSuccess(row);
          }
        }
      }
    });
  }

  private static void error(Callback<?> callback, List<String> messages) {
    logger.severe(messages.toArray());
    if (callback != null) {
      callback.onFailure(ArrayUtils.toArray(messages));
    }
  }

  private Queries() {
  }
}
