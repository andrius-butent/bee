package com.butent.bee.server.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlConstants.SqlDataType;
import com.butent.bee.server.sql.SqlConstants.SqlFunction;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.XmlView;
import com.butent.bee.shared.data.XmlView.XmlAggregateColumn;
import com.butent.bee.shared.data.XmlView.XmlColumn;
import com.butent.bee.shared.data.XmlView.XmlExternalJoin;
import com.butent.bee.shared.data.XmlView.XmlHiddenColumn;
import com.butent.bee.shared.data.XmlView.XmlOrder;
import com.butent.bee.shared.data.XmlView.XmlSimpleColumn;
import com.butent.bee.shared.data.XmlView.XmlSimpleJoin;
import com.butent.bee.shared.data.filter.ColumnColumnFilter;
import com.butent.bee.shared.data.filter.ColumnIsEmptyFilter;
import com.butent.bee.shared.data.filter.ColumnValueFilter;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.IdFilter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.filter.VersionFilter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.LogUtils;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements database view management - contains parameters for views and their fields and methods
 * for doing operations with them.
 */

public class BeeView implements HasExtendedInfo {

  private class ColumnInfo {
    private final String colName;
    private final String alias;
    private final BeeField field;
    private final String locale;
    private final SqlFunction aggregate;
    private final boolean hidden;
    private final String parentName;
    private final String ownerAlias;

    public ColumnInfo(String alias, BeeField field, String colName, String locale,
        SqlFunction aggregate, boolean hidden, String parent, String owner) {
      this.colName = colName;
      this.alias = alias;
      this.field = field;
      this.locale = locale;
      this.aggregate = aggregate;
      this.hidden = hidden;
      this.parentName = parent;
      this.ownerAlias = owner;
    }

    public SqlFunction getAggregate() {
      return aggregate;
    }

    public String getAlias() {
      return alias;
    }

    public BeeField getField() {
      return field;
    }

    public String getLocale() {
      return locale;
    }

    public String getName() {
      return colName;
    }

    public String getOwner() {
      return ownerAlias;
    }

    public String getParent() {
      return parentName;
    }

    public boolean isHidden() {
      return hidden;
    }

    public boolean isReadOnly() {
      return isHidden() || getAggregate() != null;
    }
  }

  private enum JoinType {
    INNER, RIGHT, LEFT, FULL;
  }

  private final String name;
  private final BeeTable source;
  private final String sourceAlias;
  private final boolean readOnly;
  private boolean hasAggregate;
  private final SqlSelect query;
  private final Map<String, ColumnInfo> columns = Maps.newLinkedHashMap();
  private Filter filter = null;
  private Order order = null;

  BeeView(XmlView xmlView, Map<String, BeeTable> tables) {
    Assert.notNull(xmlView);
    this.name = xmlView.name;
    Assert.notEmpty(name);

    this.source = tables.get(BeeUtils.normalize(xmlView.source));
    Assert.notNull(source);

    this.sourceAlias = getSourceName();
    this.readOnly = xmlView.readOnly;

    this.query = new SqlSelect().addFrom(getSourceName(), getSourceAlias());

    addColumns(source, getSourceAlias(), xmlView.columns, null, tables);
    setGrouping();

    if (!BeeUtils.isEmpty(xmlView.filter)) {
      this.filter = parseFilter(xmlView.filter);
    }
    if (!BeeUtils.isEmpty(xmlView.orders)) {
      this.order = new Order();

      for (XmlOrder ord : xmlView.orders) {
        order.add(ord.column, !ord.descending);
      }
    }
  }

  public SqlFunction getColumnAggregate(String colName) {
    return getColumnInfo(colName).getAggregate();
  }

  public int getColumnCount() {
    return getColumnNames().size();
  }

  public String getColumnField(String colName) {
    return getColumnInfo(colName).getField().getName();
  }

  public String getColumnLocale(String colName) {
    return getColumnInfo(colName).getLocale();
  }

  public String getColumnName(String colName) {
    return getColumnInfo(colName).getName();
  }

  public Collection<String> getColumnNames() {
    Collection<String> cols = Lists.newArrayList();

    for (ColumnInfo col : columns.values()) {
      if (!col.isHidden()) {
        cols.add(col.getName());
      }
    }
    return cols;
  }

  public String getColumnOwner(String colName) {
    return getColumnInfo(colName).getOwner();
  }

  public String getColumnParent(String colName) {
    return getColumnInfo(colName).getParent();
  }

  public String getColumnSource(String colName) {
    return getColumnInfo(colName).getAlias();
  }

  public String getColumnTable(String colName) {
    return getColumnInfo(colName).getField().getTable();
  }

  public SqlDataType getColumnType(String colName) {
    return getColumnInfo(colName).getField().getType();
  }

  public IsCondition getCondition(Filter flt) {
    if (flt != null) {
      String clazz = BeeUtils.getClassName(flt.getClass());

      if (BeeUtils.getClassName(ColumnValueFilter.class).equals(clazz)) {
        return getCondition((ColumnValueFilter) flt);

      } else if (BeeUtils.getClassName(ColumnColumnFilter.class).equals(clazz)) {
        return getCondition((ColumnColumnFilter) flt);

      } else if (BeeUtils.getClassName(ColumnIsEmptyFilter.class).equals(clazz)) {
        return getCondition((ColumnIsEmptyFilter) flt);

      } else if (BeeUtils.getClassName(CompoundFilter.class).equals(clazz)) {
        return getCondition((CompoundFilter) flt);

      } else if (BeeUtils.getClassName(IdFilter.class).equals(clazz)) {
        return getCondition((IdFilter) flt);

      } else if (BeeUtils.getClassName(VersionFilter.class).equals(clazz)) {
        return getCondition((VersionFilter) flt);

      } else {
        Assert.unsupported("Unsupported class name: " + clazz);
      }
    }
    return null;
  }

  public Filter getFilter() {
    return filter;
  }

  public List<ExtendedProperty> getInfo() {
    List<ExtendedProperty> info = Lists.newArrayList();

    PropertyUtils.addProperties(info, false, "Name", getName(), "Source", getSourceName(),
        "Source Alias", getSourceAlias(), "Source Id Name", getSourceIdName(),
        "Source Version Name", getSourceVersionName(), "Filter", BeeUtils.transform(getFilter()),
        "Read Only", isReadOnly(), "Query", query.getQuery(), "Columns", columns.size());

    int i = 0;
    for (String col : columns.keySet()) {
      String key = BeeUtils.concat(1, "Column", ++i, col);

      PropertyUtils.addChildren(info, key,
          "Table", getColumnTable(col), "Alias", getColumnSource(col),
          "Field", getColumnField(col), "Type", getColumnType(col), "Locale", getColumnLocale(col),
          "Aggregate Function", getColumnAggregate(col), "Hidden", isColHidden(col),
          "ReadOnly", isColReadOnly(col),
          "Parent Column", getColumnParent(col), "Owner Alias", getColumnOwner(col));
    }
    if (order != null) {
      info.add(new ExtendedProperty("Orders", BeeUtils.toString(order.getSize())));
      i = 0;
      for (Order.Column ordCol : order.getColumns()) {
        String key = BeeUtils.concat(1, "Order", ++i, ordCol.isAscending() ? "" : "DESC");
        PropertyUtils.addChildren(info, key,
            "Sources", BeeUtils.transformCollection(ordCol.getSources()));
      }
    }
    return info;
  }

  public String getName() {
    return name;
  }

  public SqlSelect getQuery(String... cols) {
    return getQuery(null, null, cols);
  }

  public SqlSelect getQuery(Filter flt, Order ord, String... cols) {
    SqlSelect ss = query.copyOf();
    Collection<String> activeCols = null;

    if (!BeeUtils.isEmpty(cols)) {
      ss.resetFields();
      activeCols = Sets.newHashSet();

      for (String col : cols) {
        setColumn(ss, col);
        activeCols.add(getColumnName(col));
      }
    }
    setFilter(ss, flt);

    String src = getSourceAlias();
    String idCol = getSourceIdName();
    String verCol = getSourceVersionName();
    boolean hasId = false;
    Order o = BeeUtils.nvl(ord, order);
    String alias;
    String colName;

    if (o != null) {
      for (Order.Column ordCol : o.getColumns()) {
        for (String col : ordCol.getSources()) {
          if (hasColumn(col) && (activeCols == null || activeCols.contains(getColumnName(col)))) {
            if (getColumnAggregate(col) != null) {
              alias = null;
              colName = getColumnName(col);
            } else {
              alias = getColumnSource(col);
              colName = getColumnField(col);
            }
          } else if (BeeUtils.inListSame(col, idCol, verCol)) {
            hasId = hasId || BeeUtils.same(col, idCol);
            alias = src;
            colName = col;

          } else {
            LogUtils.warning(LogUtils.getDefaultLogger(), "view: ", getName(), "order by:", col,
                "column not reccognized");
            continue;
          }
          if (!ordCol.isAscending()) {
            ss.addOrderDesc(alias, colName);
          } else {
            ss.addOrder(alias, colName);
          }
        }
      }
    }
    if (!hasId) {
      ss.addOrder(src, idCol);
    }
    return ss.addFields(src, idCol, verCol);
  }

  public Filter getRowFilter(long rowId) {
    return ComparisonFilter.compareId(getSourceIdName(), Operator.EQ, BeeUtils.toString(rowId));
  }

  public String getSourceAlias() {
    return sourceAlias;
  }

  public String getSourceIdName() {
    return source.getIdName();
  }

  public String getSourceName() {
    return source.getName();
  }

  public String getSourceVersionName() {
    return source.getVersionName();
  }

  public boolean hasColumn(String colName) {
    return !BeeUtils.isEmpty(colName) && columns.containsKey(BeeUtils.normalize(colName));
  }

  public boolean isColHidden(String colName) {
    return getColumnInfo(colName).isHidden();
  }

  public boolean isColReadOnly(String colName) {
    return getColumnInfo(colName).isReadOnly();
  }

  public boolean isEmpty() {
    return BeeUtils.isEmpty(getColumnCount());
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public Filter parseFilter(String flt) {
    Assert.notEmpty(flt);
    List<IsColumn> cols = Lists.newArrayListWithCapacity(columns.size());

    for (String col : columns.keySet()) {
      cols.add(new BeeColumn(getColumnType(col).toValueType(), col));
    }
    Filter f = DataUtils.parseCondition(flt, cols, getSourceIdName(), getSourceVersionName());

    if (f == null) {
      LogUtils.warning(LogUtils.getDefaultLogger(), "Error in filter expression:", flt);
    }
    return f;
  }

  public Order parseOrder(String input) {
    Assert.notEmpty(input);

    Set<String> colNames = Sets.newHashSet(getSourceIdName(), getSourceVersionName());
    colNames.addAll(getColumnNames());

    return Order.parse(input, colNames);
  }

  private void addColumn(String alias, BeeField field, String colName, String locale,
      String aggregateType, boolean hidden, String parent) {
    String fldName = field.getName();

    Assert.state(!BeeUtils.inListSame(colName, getSourceIdName(), getSourceVersionName()),
        BeeUtils.concat(1, "Reserved column name:", getName(), colName));
    Assert.state(!hasColumn(colName),
        BeeUtils.concat(1, "Dublicate column name:", getName(), colName));

    BeeTable table = field.getOwner();
    String ownerAlias = null;

    if (!BeeUtils.isEmpty(locale)) {
      if (field.isTranslatable()) {
        ownerAlias = alias;
        alias = table.joinTranslationField(query, ownerAlias, field, locale);
      } else {
        LogUtils.warning(LogUtils.getDefaultLogger(),
            "Field is not translatable:", table.getName() + "." + fldName,
            "View:", getName());
        return;
      }
    } else if (field.isExtended()) {
      ownerAlias = alias;
      alias = table.joinExtField(query, ownerAlias, field);
    }
    SqlFunction aggregate = null;

    if (!BeeUtils.isEmpty(aggregateType)) {
      aggregate = SqlFunction.valueOf(aggregateType);
    }
    columns.put(BeeUtils.normalize(colName),
        new ColumnInfo(alias, field, colName, locale, aggregate, hidden, parent, ownerAlias));
  }

  private void addColumns(BeeTable table, String alias, Collection<XmlColumn> cols, String parent,
      Map<String, BeeTable> tables) {
    Assert.notNull(table);
    Assert.notEmpty(alias);
    Assert.notEmpty(cols);

    for (XmlColumn column : cols) {
      if (column instanceof XmlSimpleJoin) {
        XmlSimpleJoin col = (XmlSimpleJoin) column;
        BeeTable relTable;
        BeeField field;
        IsCondition join;
        String als;
        String relAls = SqlUtils.uniqueName();

        if (col instanceof XmlExternalJoin) {
          relTable = tables.get(BeeUtils.normalize(((XmlExternalJoin) col).source));
          Assert.notEmpty(relTable);
          als = relAls;
          field = relTable.getField(col.expression);

          if (field.isExtended()) {
            LogUtils.warning(LogUtils.getDefaultLogger(),
                "Inverse join is not supported on extended fields:",
                relTable.getName() + "." + field.getName(), "View:", getName());
            continue;
          }
          join = SqlUtils.join(alias, table.getIdName(), relAls, field.getName());
        } else {
          field = table.getField(col.expression);

          if (field.isExtended()) {
            als = table.joinExtField(query, alias, field);
          } else {
            als = alias;
          }
          relTable = tables.get(BeeUtils.normalize(field.getRelation()));
          Assert.notEmpty(relTable);
          join = SqlUtils.join(als, field.getName(), relAls, relTable.getIdName());
        }
        String relTbl = relTable.getName();

        switch (JoinType.valueOf(col.joinType)) {
          case INNER:
            query.addFromInner(relTbl, relAls, join);
            break;
          case LEFT:
            query.addFromLeft(relTbl, relAls, join);
            break;
          case RIGHT:
            query.addFromRight(relTbl, relAls, join);
            break;
          case FULL:
            query.addFromFull(relTbl, relAls, join);
            break;
        }
        String colName = SqlUtils.uniqueName();
        addColumn(als, field, colName, null, null, true, parent);
        addColumns(relTable, relAls, col.columns, colName, tables);

      } else if (column instanceof XmlSimpleColumn) {
        XmlSimpleColumn col = (XmlSimpleColumn) column;
        BeeField field = table.getField(col.expression);
        String colName = BeeUtils.ifString(col.name, field.getName());
        String aggregate = null;
        boolean hidden = (col instanceof XmlHiddenColumn);

        if (col instanceof XmlAggregateColumn) {
          aggregate = ((XmlAggregateColumn) col).aggregate;
        }
        addColumn(alias, field, colName, col.locale, aggregate, hidden, parent);

        if (!hidden) {
          setColumn(query, colName);
        }
      }
    }
  }

  private ColumnInfo getColumnInfo(String colName) {
    Assert.state(hasColumn(colName), "Unknown view column: " + getName() + "." + colName);
    return columns.get(BeeUtils.normalize(colName));
  }

  private IsCondition getCondition(ColumnColumnFilter flt) {
    return SqlUtils.compare(
        getSqlExpression(flt.getColumn()), flt.getOperator(), getSqlExpression(flt.getValue()));
  }

  private IsCondition getCondition(ColumnIsEmptyFilter flt) {
    String colName = flt.getColumn();
    String als = getColumnSource(colName);
    String fld = getColumnField(colName);

    return SqlUtils.or(SqlUtils.isNull(als, fld),
        SqlUtils.equal(als, fld, getColumnType(colName).getEmptyValue()));
  }

  private IsCondition getCondition(ColumnValueFilter flt) {
    return SqlUtils.compare(
        getSqlExpression(flt.getColumn()), flt.getOperator(), SqlUtils.constant(flt.getValue()));
  }

  private IsCondition getCondition(CompoundFilter flt) {
    HasConditions condition = null;

    if (!flt.isEmpty()) {
      switch (flt.getType()) {
        case AND:
          condition = SqlUtils.and();
          break;
        case OR:
          condition = SqlUtils.or();
          break;
        case NOT:
          return SqlUtils.not(getCondition(flt.getSubFilters().get(0)));
      }
      for (Filter subFilter : flt.getSubFilters()) {
        condition.add(getCondition(subFilter));
      }
    }
    return condition;
  }

  private IsCondition getCondition(IdFilter flt) {
    return SqlUtils.compare(SqlUtils.field(getSourceAlias(), getSourceIdName()),
        flt.getOperator(), SqlUtils.constant(flt.getValue()));
  }

  private IsCondition getCondition(VersionFilter flt) {
    return SqlUtils.compare(SqlUtils.field(getSourceAlias(), getSourceVersionName()),
        flt.getOperator(), SqlUtils.constant(flt.getValue()));
  }

  private IsExpression getSqlExpression(String colName) {
    IsExpression expr = SqlUtils.field(getColumnSource(colName), getColumnField(colName));
    SqlFunction aggregate = getColumnAggregate(colName);

    if (aggregate != null) {
      expr = SqlUtils.aggregate(aggregate, expr);
    }
    return expr;
  }

  private void setColumn(SqlSelect ss, String col) {
    String alias = getColumnSource(col);
    String fldName = getColumnField(col);
    String colName = getColumnName(col);
    SqlFunction aggregate = getColumnAggregate(col);

    if (aggregate == null) {
      ss.addField(alias, fldName, colName);
    } else {
      switch (aggregate) {
        case MIN:
          ss.addMin(alias, fldName, colName);
          break;
        case MAX:
          ss.addMax(alias, fldName, colName);
          break;
        case SUM:
          ss.addSum(alias, fldName, colName);
          break;
        case AVG:
          ss.addAvg(alias, fldName, colName);
          break;
        case COUNT:
          ss.addCount(alias, fldName, colName);
          break;
        case SUM_DISTINCT:
          ss.addSumDistinct(alias, fldName, colName);
          break;
        case AVG_DISTINCT:
          ss.addAvgDistinct(alias, fldName, colName);
          break;
        case COUNT_DISTINCT:
          ss.addCountDistinct(alias, fldName, colName);
          break;
        default:
          Assert.unsupported();
      }
    }
  }

  private void setFilter(SqlSelect ss, Filter flt) {
    CompoundFilter f = Filter.and(filter, flt);

    if (!f.isEmpty()) {
      IsCondition condition = getCondition(f);

      if (hasAggregate) {
        for (String col : columns.keySet()) {
          if (getColumnAggregate(col) != null && f.involvesColumn(col)) {
            ss.setHaving(condition);
            break;
          }
        }
        if (ss.getHaving() != null) {
          for (String col : columns.keySet()) {
            if (isColHidden(col) && f.involvesColumn(col)) {
              ss.addGroup(getColumnSource(col), getColumnField(col));
            }
          }
          return;
        }
      }
      ss.setWhere(condition);
    }
  }

  private void setGrouping() {
    Multimap<String, String> group = HashMultimap.create();

    for (String col : getColumnNames()) {
      if (getColumnAggregate(col) != null) {
        hasAggregate = true;
      } else {
        group.put(getColumnSource(col), getColumnField(col));
      }
    }
    if (hasAggregate) {
      query.addGroup(getSourceAlias(), getSourceIdName(), getSourceVersionName());

      for (String alias : group.keySet()) {
        query.addGroup(alias, group.get(alias).toArray(new String[0]));
      }
    }
  }
}
