package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Transformable;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.logging.Logger;

public class BeeColumn extends TableColumn implements BeeSerializable, Transformable  {

  private enum SerializationMembers {
    ID, NAME, LABEL, SQL_TYPE, VALUE_TYPE, PRECISION, SCALE, ISNULL
  }

  public static final int NO_NULLS = 0;
  public static final int NULLABLE = 1;
  public static final int NULLABLE_UNKNOWN = 2;

  private static Logger logger = Logger.getLogger(BeeColumn.class.getName());

  public static BeeColumn restore(String s) {
    BeeColumn c = new BeeColumn();
    c.deserialize(s);
    return c;
  }
  
  private int index = BeeConst.INDEX_UNKNOWN;

  private String name = null;

  private String schema = null;
  private String catalog = null;
  private String table = null;

  private String clazz = null;
  private int sqlType = 0;
  private String typeName = null;

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
    super(null);
  }

  public BeeColumn(String name) {
    super(null, name, name);
    setName(name);
  }
 
  public BeeColumn(ValueType type, String label, String id) {
    super(type, label, id);
    setName(BeeUtils.ifString(label, id));
  }

  public BeeColumn clone() {
    BeeColumn result = new BeeColumn();
    
    result.setId(getId());
    result.setType(getType());
    result.setLabel(getLabel());
    result.setPattern(getPattern());
    if (getProperties() != null) {
      result.setProperties(getProperties());
    }
    
    result.setIndex(getIndex());
    result.setName(getName());
    result.setSchema(getSchema());
    result.setCatalog(getCatalog());
    result.setTable(getTable());
    result.setClazz(getClazz());
    result.setSqlType(getSqlType());
    result.setTypeName(getTypeName());
    result.setDisplaySize(getDisplaySize());
    result.setPrecision(getPrecision());
    result.setScale(getScale());
    result.setNullable(getNullable());
    result.setSigned(isSigned());
    result.setAutoIncrement(isAutoIncrement());
    result.setCaseSensitive(isCaseSensitive());
    result.setCurrency(isCurrency());
    result.setSearchable(isSearchable());
    result.setReadOnly(isReadOnly());
    result.setWritable(isWritable());
    result.setDefinitelyWritable(isDefinitelyWritable());
    
    return result;
  }

  public void deserialize(String s) {
    SerializationMembers[] members = SerializationMembers.values();
    String[] arr = Codec.beeDeserialize(s);

    Assert.arrayLength(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      SerializationMembers member = members[i];
      String value = arr[i];

      switch (member) {
        case ID:
          setId(value);
          break;
        case NAME:
          setName(value);
          break;
        case LABEL:
          setLabel(value);
          break;
        case SQL_TYPE:
          setSqlType(BeeUtils.toInt(value));
          break;
        case VALUE_TYPE:
          setType(ValueType.getByTypeCode(value));
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
  
  public int getDisplaySize() {
    return displaySize;
  }

  public int getIndex() {
    return index;
  }

  public List<Property> getInfo() {
    List<Property> lst = super.getInfo();

    PropertyUtils.addProperties(lst,
        "index", valueAsString(getIndex()),
        "name", getName(),
        "schema", getSchema(),
        "catalog", getCatalog(),
        "table", getTable(),
        "class", getClazz(),
        "sql type", getSqlType(),
        "type name", getTypeName(),
        "display size", valueAsString(getDisplaySize()),
        "precision", valueAsString(getPrecision()),
        "scale", valueAsString(getScale()),
        "nullable", BeeUtils.concat(1, getNullable(), nullableAsString()),
        "signed", valueAsString(isSigned()),
        "auto increment", valueAsString(isAutoIncrement()),
        "case sensitive", isCaseSensitive(),
        "currency", valueAsString(isCurrency()),
        "searchable", isSearchable(),
        "read only", isReadOnly(),
        "writable", isWritable(),
        "definitely writable", isDefinitelyWritable());

    return lst;
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

  public int getSqlType() {
    return sqlType;
  }

  public String getTable() {
    return table;
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
      case NULLABLE:
        return "nullable";
      case NO_NULLS:
        return "no nulls";
      case NULLABLE_UNKNOWN:
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
        case ID:
          sb.append(Codec.beeSerialize(getId()));
          break;
        case NAME:
          sb.append(Codec.beeSerialize(getName()));
          break;
        case LABEL:
          sb.append(Codec.beeSerialize(getLabel()));
          break;
        case SQL_TYPE:
          sb.append(Codec.beeSerialize(getSqlType()));
          break;
        case VALUE_TYPE:
          sb.append(Codec.beeSerialize((getType() == null) ? null : getType().getTypeCode()));
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
        default:
          Assert.untouchable("Unhandled serialization member: " + member);
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

  public void setIndex(int index) {
    this.index = index;
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

  public void setSqlType(int sqlType) {
    this.sqlType = sqlType;
  }

  public void setTable(String table) {
    this.table = table;
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
      return BeeUtils.transformCollection(getInfo(), BeeConst.DEFAULT_LIST_SEPARATOR);
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
    if (v == BeeConst.INDEX_UNKNOWN || v == BeeConst.SIZE_UNKNOWN || v == BeeConst.TIME_UNKNOWN) {
      return BeeUtils.concat(1, v, BeeConst.UNKNOWN);
    } else {
      return Integer.toString(v);
    }
  }
}
