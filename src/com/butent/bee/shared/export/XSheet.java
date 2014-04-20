package com.butent.bee.shared.export;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class XSheet implements BeeSerializable {

  private static final double H1_FONT_FACTOR = 1.4;
  private static final double HX_FONT_FACTOR = 1.2;

  public static XSheet restore(String s) {
    Assert.notEmpty(s);
    XSheet sheet = new XSheet();
    sheet.deserialize(s);
    return sheet;
  }

  private String name;

  private final List<XRow> rows = new ArrayList<>();

  private final List<XFont> fonts = new ArrayList<>();
  private final List<XStyle> styles = new ArrayList<>();

  private final List<Integer> autoSize = new ArrayList<>();

  public XSheet() {
    super();
  }

  public XSheet(String name) {
    this();
    this.name = name;
  }

  public void add(XRow row) {
    Assert.notNull(row);
    rows.add(row);
  }

  public void addHeaders(List<String> headers) {
    if (BeeUtils.isEmpty(headers)) {
      return;
    }

    XStyle s1 = XStyle.center();

    XFont f1 = XFont.bold();
    f1.setFactor(H1_FONT_FACTOR);
    s1.setFontRef(registeFont(f1));

    XStyle sx;
    if (headers.size() > 1) {
      sx = XStyle.center();

      XFont fx = XFont.bold();
      fx.setFactor(HX_FONT_FACTOR);
      sx.setFontRef(registeFont(fx));

    } else {
      sx = null;
    }

    addHeaders(headers, s1, sx, 0, getMaxColumn() + 1);
  }

  public void addHeaders(List<String> headers, XStyle s1, XStyle sx, int column, int colSpan) {
    if (BeeUtils.isEmpty(headers)) {
      return;
    }

    for (XRow row : rows) {
      row.shift(headers.size() + 2);
    }

    Integer cs1 = (s1 == null) ? null : registerStyle(s1);
    Integer csx = (sx == null) ? cs1 : registerStyle(sx);

    int rowIndex = 1;

    for (String header : headers) {
      if (!BeeUtils.isEmpty(header)) {
        XRow row = new XRow(rowIndex);
        XCell cell = new XCell(column, header);

        Integer styleRef = (rowIndex == 1) ? cs1 : csx;
        if (styleRef != null) {
          cell.setStyleRef(styleRef);
        }

        if (colSpan > 1) {
          cell.setColSpan(colSpan);
        }

        row.add(cell);
        add(row);
      }
      rowIndex++;
    }
  }

  public void autoSizeAll() {
    if (!autoSize.isEmpty()) {
      autoSize.clear();
    }

    int maxColumn = getMaxColumn();
    for (int i = 0; i <= maxColumn; i++) {
      autoSize.add(i);
    }
  }

  public void autoSizeColumn(int index) {
    if (!autoSize.contains(index)) {
      autoSize.add(index);
    }
  }

  public void clear() {
    rows.clear();

    fonts.clear();
    styles.clear();

    autoSize.clear();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 5);

    int i = 0;
    setName(arr[i++]);

    if (!rows.isEmpty()) {
      rows.clear();
    }

    String[] rarr = Codec.beeDeserializeCollection(arr[i++]);
    if (rarr != null) {
      for (String rv : rarr) {
        add(XRow.restore(rv));
      }
    }

    if (!fonts.isEmpty()) {
      fonts.clear();
    }

    String[] farr = Codec.beeDeserializeCollection(arr[i++]);
    if (farr != null) {
      for (String fv : farr) {
        fonts.add(XFont.restore(fv));
      }
    }

    if (!styles.isEmpty()) {
      styles.clear();
    }

    String[] sarr = Codec.beeDeserializeCollection(arr[i++]);
    if (sarr != null) {
      for (String sv : sarr) {
        styles.add(XStyle.restore(sv));
      }
    }

    if (!autoSize.isEmpty()) {
      autoSize.clear();
    }

    String[] carr = Codec.beeDeserializeCollection(arr[i++]);
    if (carr != null) {
      for (String cv : carr) {
        if (BeeUtils.isDigit(cv)) {
          autoSize.add(BeeUtils.toInt(cv));
        }
      }
    }
  }

  public List<Integer> getAutoSize() {
    return autoSize;
  }

  public XFont getFont(int index) {
    return fonts.get(index);
  }

  public List<XFont> getFonts() {
    return fonts;
  }

  public int getMaxColumn() {
    int result = BeeConst.UNDEF;
    for (XRow row : rows) {
      result = Math.max(result, row.getMaxColumn());
    }
    return result;
  }

  public String getName() {
    return name;
  }

  public List<XRow> getRows() {
    return rows;
  }

  public XStyle getStyle(int index) {
    return styles.get(index);
  }

  public List<XStyle> getStyles() {
    return styles;
  }

  public boolean isEmpty() {
    return rows.isEmpty() && fonts.isEmpty() && styles.isEmpty() && autoSize.isEmpty();
  }

  public int registeFont(XFont font) {
    Assert.notNull(font);

    int index = fonts.indexOf(font);
    if (index >= 0) {
      return index;
    } else {
      fonts.add(font);
      return fonts.size() - 1;
    }
  }

  public int registerStyle(XStyle style) {
    Assert.notNull(style);

    int index = styles.indexOf(style);
    if (index >= 0) {
      return index;
    } else {
      styles.add(style);
      return styles.size() - 1;
    }
  }

  @Override
  public String serialize() {
    List<String> values = new ArrayList<>();

    values.add(getName());
    values.add(rows.isEmpty() ? null : Codec.beeSerialize(rows));

    values.add(fonts.isEmpty() ? null : Codec.beeSerialize(fonts));
    values.add(styles.isEmpty() ? null : Codec.beeSerialize(styles));

    values.add(autoSize.isEmpty() ? null : Codec.beeSerialize(autoSize));

    return Codec.beeSerialize(values);
  }

  public void setName(String name) {
    this.name = name;
  }
}
