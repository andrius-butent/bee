package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportResultItem extends ReportNumericItem {

  private static final String LEVEL = "LEVEL";

  private ResultHolder.ResultLevel level = ResultHolder.ResultLevel.CELL;
  private ListBox levelWidget;

  public ReportResultItem(String expression, String caption) {
    super(expression, caption);
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeMap(data);

    if (!BeeUtils.isEmpty(map)) {
      setLevel(EnumUtils.getEnumByName(ResultHolder.ResultLevel.class, map.get(LEVEL)));
    }
  }

  @Override
  public ReportValue evaluate(SimpleRowSet.SimpleRow row) {
    Assert.unsupported();
    return null;
  }

  @Override
  public ReportValue evaluate(ReportValue rowGroup, ReportValue[] rowValues, ReportValue colGroup,
      ResultHolder resultHolder) {

    Object value = null;

    switch (getLevel()) {
      case CELL:
        value = resultHolder.getCellValue(rowGroup, rowValues, colGroup, getExpression());
        break;
      case COL:
        value = resultHolder.getColTotal(colGroup, getExpression());
        break;
      case GROUP:
        value = resultHolder.getGroupTotal(rowGroup, getExpression());
        break;
      case GROUP_COL:
        value = resultHolder.getGroupValue(rowGroup, colGroup, getExpression());
        break;
      case ROW:
        value = resultHolder.getRowTotal(rowGroup, rowValues, getExpression());
        break;
      case TOTAL:
        value = resultHolder.getTotal(getExpression());
        break;
    }
    return value != null ? ReportValue.of(value.toString()) : ReportValue.empty();
  }

  public ResultHolder.ResultLevel getLevel() {
    return level;
  }

  @Override
  public Widget getExpressionWidget(List<ReportItem> reportItems) {
    Label xpr = new Label();
    xpr.addStyleName("bee-output");
    String expr = null;

    for (ReportItem item : reportItems) {
      if (Objects.equals(item, this)) {
        expr = item.getCaption();
        break;
      }
    }
    if (BeeUtils.isEmpty(expr)) {
      xpr.addStyleName("bee-error");
      expr = "???";
    }
    xpr.setHtml(expr);
    return xpr;
  }

  @Override
  public String getOptionsCaption() {
    return Localized.dictionary().result();
  }

  @Override
  public ListBox getOptionsWidget() {
    if (levelWidget == null) {
      levelWidget = new ListBox();

      for (ResultHolder.ResultLevel lvl : ResultHolder.ResultLevel.values()) {
        levelWidget.addItem(lvl.name());
      }
    }
    levelWidget.setValue(getLevel().name());
    return levelWidget;
  }

  @Override
  public boolean isResultItem() {
    return true;
  }

  @Override
  public String saveOptions() {
    if (levelWidget != null) {
      setLevel(EnumUtils.getEnumByName(ResultHolder.ResultLevel.class, levelWidget.getValue()));
    }
    return super.saveOptions();
  }

  @Override
  public String serialize() {
    return serialize(Codec.beeSerialize(Collections.singletonMap(LEVEL, getLevel())));
  }

  public ReportResultItem setLevel(ResultHolder.ResultLevel level) {
    this.level = Assert.notNull(level);
    return this;
  }
}
