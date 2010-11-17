package com.butent.bee.egg.shared.data;

import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.BeeConst;
import com.butent.bee.egg.shared.BeeSerializable;
import com.butent.bee.egg.shared.Transformable;
import com.butent.bee.egg.shared.utils.BeeUtils;
import com.butent.bee.egg.shared.utils.Codec;
import com.butent.bee.egg.shared.utils.PropUtils;
import com.butent.bee.egg.shared.utils.StringProp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BeeColumn implements Transformable, BeeSerializable {

  private enum SerializationMembers {
    NAME, TYPE, PRECISION, SCALE, ISNULL, SCHEMA, CATALOG, TABLE, TYPENAME
  }

  public static final int NO_NULLS = 0;
  public static final int NULLABLE = 1;

  public static final int NULLABLE_UNKNOWN = 2;

  private static Logger logger = Logger.getLogger(BeeRowSet.class.getName());

  public static BeeColumn restore(String s) {
    BeeColumn c = new BeeColumn();
    c.deserialize(s);
    return c;
  }

  private int idx = BeeConst.INDEX_UNKNOWN;

  private String name = null;
  private String schema = null;
  private String catalog = null;

  private String table = null;
  private String clazz = null;
  private int type = 0;

  private String typeName = null;
  private String label = null;

  private int displaySize = BeeConst.SIZE_UNKNOWN;
  private int precision = BeeConst.SIZE_UNKNOWN;

  private int scale = BeeConst.SIZE_UNKNOWN;

  private int nullable = NULLABLE_UNKNOWN;
  private boolean signed = false;
  private boolean autoIncrement = false;
  private boolean caseSensitive = false;

  private boolean currency = false;
  private boolean definitelyWritable = false;
  private boolean readOnly = false;
  private boolean searchable = false;

  private boolean writable = false;

  public BeeColumn() {
    super();
  }

  public BeeColumn(String name) {
    this();
    setName(name);
  }

  public void deserialize(String s) {
    SerializationMembers[] members = SerializationMembers.values();
    String[] arr = Codec.beeDeserialize(s);

    Assert.arrayLength(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      SerializationMembers member = members[i];
      String value = arr[i];

      switch (member) {
        case NAME:
          setName(value);
          break;
        case TYPE:
          setType(BeeUtils.toInt(value));
          break;
        case PRECISION:
          setPrecision(BeeUtils.toInt(value));
          break;
        case SCALE:
          setScale(BeeUtils.toInt(value));
          break;
        case ISNULL:
          setNullable(BeeUtils.toInt(value));
          break;
        case SCHEMA:
          setSchema(value);
          break;
        case CATALOG:
          setCatalog(value);
          break;
        case TABLE:
          setTable(value);
          break;
        case TYPENAME:
          setTypeName(value);
          break;
        default:
          logger.severe("Unhandled serialization member: " + member);
          break;
      }
    }
  }

  public String getCatalog() {
    return catalog;
  }

  public String getClazz() {
    return clazz;
  }

  public List<StringProp> getColumnInfo() {
    List<StringProp> lst = new ArrayList<StringProp>();

    PropUtils.addString(lst, "index", valueAsString(getIdx()), "name",
        getName(), "schema", getSchema(), "catalog", getCatalog(), "table",
        getTable(), "class", getClazz(), "type", getType(), "type name",
        getTypeName(), "label", getLabel(), "display size",
        valueAsString(getDisplaySize()), "precision",
        valueAsString(getPrecision()), "scale", valueAsString(getScale()),
        "nullable", BeeUtils.concat(1, getNullable(), nullableAsString()),
        "signed", valueAsString(isSigned()), "auto increment",
        valueAsString(isAutoIncrement()), "case sensitive", isCaseSensitive(),
        "currency", valueAsString(isCurrency()), "searchable", isSearchable(),
        "read only", isReadOnly(), "writable", isWritable(),
        "definitely writable", isDefinitelyWritable());

    return lst;
  }

  public int getDisplaySize() {
    return displaySize;
  }

  public int getIdx() {
    return idx;
  }

  public String getLabel() {
    return label;
  }

  public String getName() {
    return name;
  }

  public int getNullable() {
    return nullable;
  }

  public int getPrecision() {
    return precision;
  }

  public int getScale() {
    return scale;
  }

  public String getSchema() {
    return schema;
  }

  public String getTable() {
    return table;
  }

  public int getType() {
    return type;
  }

  public String getTypeName() {
    return typeName;
  }

  public boolean isAutoIncrement() {
    return autoIncrement;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public boolean isCurrency() {
    return currency;
  }

  public boolean isDefinitelyWritable() {
    return definitelyWritable;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public boolean isSigned() {
    return signed;
  }

  public boolean isWritable() {
    return writable;
  }

  public String nullableAsString() {
    switch (getNullable()) {
      case (NULLABLE):
        return "nullable";
      case (NO_NULLS):
        return "no nulls";
      case (NULLABLE_UNKNOWN):
        return "nullable unkown";
      default:
        return BeeConst.UNKNOWN;
    }
  }

  public String serialize() {
    Assert.state(validState());

    StringBuilder sb = new StringBuilder();

    for (SerializationMembers member : SerializationMembers.values()) {
      switch (member) {
        case NAME:
          sb.append(Codec.beeSerialize(getName()));
          break;
        case TYPE:
          sb.append(Codec.beeSerialize(getType()));
          break;
        case PRECISION:
          sb.append(Codec.beeSerialize(getPrecision()));
          break;
        case SCALE:
          sb.append(Codec.beeSerialize(getScale()));
          break;
        case ISNULL:
          sb.append(Codec.beeSerialize(getNullable()));
          break;
        case SCHEMA:
          sb.append(Codec.beeSerialize(getSchema()));
          break;
        case CATALOG:
          sb.append(Codec.beeSerialize(getCatalog()));
          break;
        case TABLE:
          sb.append(Codec.beeSerialize(getTable()));
          break;
        case TYPENAME:
          sb.append(Codec.beeSerialize(getTypeName()));
          break;
        default:
          logger.severe("Unhandled serialization member: " + member);
          break;
      }
    }
    return sb.toString();
  }

  public void setAutoIncrement(boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
  }

  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }

  public void setClazz(String clazz) {
    this.clazz = clazz;
  }

  public void setCurrency(boolean currency) {
    this.currency = currency;
  }

  public void setDefinitelyWritable(boolean definitelyWritable) {
    this.definitelyWritable = definitelyWritable;
  }

  public void setDisplaySize(int displaySize) {
    this.displaySize = displaySize;
  }

  public void setIdx(int idx) {
    this.idx = idx;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNullable(int nullable) {
    this.nullable = nullable;
  }

  public void setPrecision(int precision) {
    this.precision = precision;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setScale(int scale) {
    this.scale = scale;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public void setSigned(boolean signed) {
    this.signed = signed;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public void setType(int type) {
    this.type = type;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public void setWritable(boolean writable) {
    this.writable = writable;
  }

  @Override
  public String toString() {
    if (validState()) {
      return BeeUtils.transformCollection(getColumnInfo(),
          BeeConst.DEFAULT_LIST_SEPARATOR);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public String transform() {
    return toString();
  }

  private boolean validState() {
    return !BeeUtils.isEmpty(getName());
  }

  private String valueAsString(boolean v) {
    return v ? Boolean.toString(v) : BeeConst.STRING_EMPTY;
  }

  private String valueAsString(int v) {
    if (v == BeeConst.INDEX_UNKNOWN || v == BeeConst.SIZE_UNKNOWN
        || v == BeeConst.TIME_UNKNOWN) {
      return BeeUtils.concat(1, v, BeeConst.UNKNOWN);
    } else {
      return Integer.toString(v);
    }
  }

}
