package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.Map;

/**
 * Manages grid component xml configurations.
 */

public class GridComponentDescription implements BeeSerializable, HasInfo {

  /**
   * Contains serializable members of a grid user interface component.
   */

  private enum Serial {
    STYLE, HEIGHT, MIN_HEIGHT, MAX_HEIGHT, PADDING, BORDER_WIDTH, MARGIN
  }

  public static final String TAG_STYLE = "style";
  private static final String ATTR_HEIGHT = "height";
  private static final String ATTR_MIN_HEIGHT = "minHeight";
  private static final String ATTR_MAX_HEIGHT = "maxHeight";
  private static final String ATTR_PADDING = "padding";
  private static final String ATTR_BORDER_WIDTH = "borderWidth";

  private static final String ATTR_MARGIN = "margin";

  public static GridComponentDescription restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    GridComponentDescription component = new GridComponentDescription();
    component.deserialize(s);
    return component;
  }

  private StyleDeclaration style = null;

  private Integer height = null;
  private Integer minHeight = null;
  private Integer maxHeight = null;

  private String padding = null;
  private String borderWidth = null;
  private String margin = null;

  public GridComponentDescription(Integer height) {
    setHeight(height);
  }

  public GridComponentDescription(StyleDeclaration style, Map<String, String> attributes) {
    setStyle(style);
    setAttributes(attributes);
  }

  private GridComponentDescription() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      switch (members[i]) {
        case STYLE:
          setStyle(StyleDeclaration.restore(arr[i]));
          break;
        case HEIGHT:
          setHeight(BeeUtils.toIntOrNull(arr[i]));
          break;
        case MIN_HEIGHT:
          setMinHeight(BeeUtils.toIntOrNull(arr[i]));
          break;
        case MAX_HEIGHT:
          setMaxHeight(BeeUtils.toIntOrNull(arr[i]));
          break;
        case PADDING:
          setPadding(arr[i]);
          break;
        case BORDER_WIDTH:
          setBorderWidth(arr[i]);
          break;
        case MARGIN:
          setMargin(arr[i]);
          break;
      }
    }
  }

  public String getBorderWidth() {
    return borderWidth;
  }

  public Integer getHeight() {
    return height;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = Lists.newArrayList();
    if (getStyle() != null) {
      info.addAll(getStyle().getInfo());
    }

    PropertyUtils.addProperties(info,
        "Height", getHeight(), "Min Height", getMinHeight(), "Max Height", getMaxHeight(),
        "Padding", getPadding(), "Border Width", getBorderWidth(), "Margin", getMargin());

    PropertyUtils.addWhenEmpty(info, getClass());
    return info;
  }

  public String getMargin() {
    return margin;
  }

  public Integer getMaxHeight() {
    return maxHeight;
  }

  public Integer getMinHeight() {
    return minHeight;
  }

  public String getPadding() {
    return padding;
  }

  public StyleDeclaration getStyle() {
    return style;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];

    for (int i = 0; i < members.length; i++) {
      switch (members[i]) {
        case STYLE:
          arr[i] = getStyle();
          break;
        case HEIGHT:
          arr[i] = getHeight();
          break;
        case MIN_HEIGHT:
          arr[i] = getMinHeight();
          break;
        case MAX_HEIGHT:
          arr[i] = getMaxHeight();
          break;
        case PADDING:
          arr[i] = getPadding();
          break;
        case BORDER_WIDTH:
          arr[i] = getBorderWidth();
          break;
        case MARGIN:
          arr[i] = getMargin();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return;
    }

    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String key = attribute.getKey();
      String value = attribute.getValue();
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (BeeUtils.same(key, ATTR_HEIGHT)) {
        setHeight(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_MIN_HEIGHT)) {
        setMinHeight(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_MAX_HEIGHT)) {
        setMaxHeight(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_PADDING)) {
        setPadding(value.trim());
      } else if (BeeUtils.same(key, ATTR_BORDER_WIDTH)) {
        setBorderWidth(value.trim());
      } else if (BeeUtils.same(key, ATTR_MARGIN)) {
        setMargin(value.trim());
      }
    }
  }

  public void setStyle(StyleDeclaration style) {
    this.style = style;
  }

  private void setBorderWidth(String borderWidth) {
    this.borderWidth = borderWidth;
  }

  private void setHeight(Integer height) {
    this.height = height;
  }

  private void setMargin(String margin) {
    this.margin = margin;
  }

  private void setMaxHeight(Integer maxHeight) {
    this.maxHeight = maxHeight;
  }

  private void setMinHeight(Integer minHeight) {
    this.minHeight = minHeight;
  }

  private void setPadding(String padding) {
    this.padding = padding;
  }
}
