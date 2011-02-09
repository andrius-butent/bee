package com.butent.bee.shared.data.value;

public class TextValue extends Value {

  private static final TextValue NULL_VALUE = new TextValue("");

  public static TextValue getNullValue() {
    return NULL_VALUE;
  }

  private String value;

  public TextValue(String value) {
    this.value = (value == null) ? NULL_VALUE.getValue() : value;
  }

  @Override
  public int compareTo(Value other) {
    if (this == other) {
      return 0;
    }
    return value.compareTo(((TextValue) other).value);
  }

  @Override
  public String getObjectValue() {
    return value;
  }

  @Override
  public ValueType getType() {
    return ValueType.TEXT;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean isNull() {
    return (this == NULL_VALUE);
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  protected String innerToQueryString() {
    if (value.contains("\"")) {
      if (value.contains("'")) {
        throw new RuntimeException("Cannot run toQueryString() on string"
            + " values that contain both \" and '.");
      } else {
        return "'" + value + "'";
      }
    } else {
      return "\"" + value + "\"";
    }
  }
}
