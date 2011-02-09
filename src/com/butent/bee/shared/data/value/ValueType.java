package com.butent.bee.shared.data.value;

import com.google.common.collect.Maps;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeDate;

import java.util.Map;

public enum ValueType { BOOLEAN("boolean"), NUMBER("number"), TEXT("string"),
  DATE("date"), TIMEOFDAY("timeofday"), DATETIME("datetime");

  private static Map<String, ValueType> typeCodeToValueType;

  static {
    typeCodeToValueType = Maps.newHashMap();
    for (ValueType type : ValueType.values()) {
      typeCodeToValueType.put(type.typeCode.trim().toLowerCase(), type);
    }
  }

  public static ValueType getByTypeCode(String code) {
    if (code == null || code.isEmpty()) {
      return null;
    }
    return typeCodeToValueType.get(code.trim().toLowerCase());
  }

  private String typeCode;

  ValueType(String typeCode) {
    this.typeCode = typeCode;
  }

  public Value createValue(Object value) {
    Value ret = null;

    if (value == null) {
      ret = Value.getNullValueFromValueType(this);
    } else if ((this == TEXT) && (value instanceof String)) {
      ret = new TextValue((String) value);
    } else if ((this == NUMBER) && (value instanceof Number)) {
        ret = new NumberValue(((Number) value).doubleValue());
    } else if ((this == BOOLEAN) && (value instanceof Boolean)) {
        ret = ((Boolean) value).booleanValue() ? BooleanValue.TRUE : BooleanValue.FALSE;
    } else if ((this == DATE) && (value instanceof BeeDate)) {
        ret = new DateValue((BeeDate) value);
    } else if ((this == DATETIME) && (value instanceof BeeDate)) {
        ret = new DateTimeValue((BeeDate) value);
    } else if ((this == TIMEOFDAY) && (value instanceof BeeDate)) {
        ret = new TimeOfDayValue((BeeDate) value);
    }
    
    Assert.notNull(ret, "Value type mismatch.");
    return ret;
  }

  public String getTypeCode() {
    return typeCode;
  }
}
