package com.butent.bee.client.output;

import com.google.common.base.Function;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.CustomSpan;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportFormulaItem extends ReportNumericItem {

  private static final String EXPRESSION = "EXPRESSION";

  private List<Pair<String, ReportItem>> expression = new ArrayList<>();
  private List<Pair<String, ReportItem>> temporaryExpression = new ArrayList<>();

  public ReportFormulaItem(String caption) {
    super(BeeUtils.randomString(10), caption);
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeLinkedHashMap(data);

    if (!BeeUtils.isEmpty(map)) {
      for (String itemData : Codec.beeDeserializeCollection(map.get(EXPRESSION))) {
        Pair<String, String> pair = Pair.restore(itemData);
        addItem(expression, pair.getA(), ReportItem.restore(pair.getB()));
      }
      super.deserialize(map.get(Service.VAR_DATA));
    }
  }

  public ReportFormulaItem divide(ReportItem item) {
    return addItem(expression, BeeConst.STRING_SLASH, item);
  }

  @Override
  public ReportValue evaluate(SimpleRowSet.SimpleRow row) {
    return evaluate(item -> item.evaluate(row));
  }

  @Override
  public ReportValue evaluate(ReportValue rowGroup, ReportValue[] rowValues, ReportValue colGroup,
      ResultHolder resultHolder) {
    return evaluate(item -> item.evaluate(rowGroup, rowValues, colGroup, resultHolder));
  }

  @Override
  public String getCaption() {
    String cap = super.getCaption();

    if (Objects.equals(cap, getExpression())) {
      cap = BeeConst.STRING_EMPTY;
    }
    return cap;
  }

  @Override
  public String getExpressionCaption() {
    return Localized.dictionary().formula();
  }

  @Override
  public Widget getExpressionWidget(List<ReportItem> reportItems) {
    Flow container = new Flow(getStyle() + "-expression");
    temporaryExpression.clear();
    temporaryExpression.addAll(expression);
    render(container, reportItems);
    return container;
  }

  @Override
  public String getFormatedCaption() {
    String cap = getCaption();

    if (BeeUtils.isEmpty(cap)) {
      StringBuilder display = new StringBuilder();

      for (Pair<String, ReportItem> pair : expression) {
        if (display.length() > 0) {
          display.append(pair.getA());
        }
        display.append(BeeUtils.embrace(pair.getB().getFormatedCaption()));
      }
      cap = display.toString();
    }
    return cap;
  }

  @Override
  public List<ReportItem> getMembers() {
    List<ReportItem> members = new ArrayList<>();

    for (Pair<String, ReportItem> pair : expression) {
      members.addAll(pair.getB().getMembers());
    }
    return members;
  }

  @Override
  public boolean isResultItem() {
    for (Pair<String, ReportItem> pair : expression) {
      if (pair.getB().isResultItem()) {
        return true;
      }
    }
    return super.isResultItem();
  }

  public ReportFormulaItem minus(ReportItem item) {
    return addItem(expression, BeeConst.STRING_MINUS, item);
  }

  public ReportFormulaItem multiply(ReportItem item) {
    return addItem(expression, BeeConst.STRING_ASTERISK, item);
  }

  public ReportFormulaItem plus(ReportItem item) {
    return addItem(expression, BeeConst.STRING_PLUS, item);
  }

  @Override
  public String saveOptions() {
    if (BeeUtils.isEmpty(temporaryExpression)) {
      return Localized.dictionary().dataNotAvailable(Localized.dictionary().expression());
    }
    expression.clear();
    expression.addAll(temporaryExpression);
    return super.saveOptions();
  }

  @Override
  protected String serialize(String data) {
    Map<String, Object> map = new HashMap<>();
    map.put(Service.VAR_DATA, data);
    map.put(EXPRESSION, expression);
    return super.serialize(Codec.beeSerialize(map));
  }

  private ReportFormulaItem addItem(List<Pair<String, ReportItem>> list, String op,
      ReportItem item) {
    if (item != null) {
      list.add(Pair.of(BeeUtils.notEmpty(op, BeeConst.STRING_PLUS),
          ReportItem.restore(item.serialize())));
    }
    return this;
  }

  private ReportValue evaluate(Function<ReportItem, ReportValue> evaluator) {
    List<BigDecimal> values = new ArrayList<>();
    BigDecimal previous = null;

    for (Pair<String, ReportItem> pair : expression) {
      BigDecimal value = BeeUtils.nvl(BeeUtils.toDecimalOrNull(evaluator.apply(pair.getB())
          .getValue()), BigDecimal.ZERO);

      if (previous == null) {
        previous = value;
      } else {
        switch (pair.getA()) {
          case BeeConst.STRING_PLUS:
            values.add(previous);
            previous = value;
            break;
          case BeeConst.STRING_MINUS:
            values.add(previous);
            previous = value.negate();
            break;
          case BeeConst.STRING_ASTERISK:
            previous = previous.multiply(value, new MathContext(5));
            break;
          case BeeConst.STRING_SLASH:
            if (BeeUtils.isZero(value.doubleValue())) {
              return ReportValue.empty();
            }
            previous = previous.divide(value, new MathContext(5));
            break;
        }
      }
    }
    if (previous != null) {
      values.add(previous);
    }
    BigDecimal result = BigDecimal.ZERO;

    for (BigDecimal value : values) {
      result = result.add(value);
    }
    if (BeeUtils.isZero(result.doubleValue())) {
      return ReportValue.empty();
    }
    return ReportValue.of(result.setScale(getPrecision(), RoundingMode.HALF_UP).toPlainString());
  }

  private void render(Flow container, List<ReportItem> reportItems) {
    Runnable refresh = () -> render(container, reportItems);
    container.clear();
    boolean hasResultItems = false;

    for (Pair<String, ReportItem> pair : temporaryExpression) {
      if (pair.getB().isResultItem()) {
        hasResultItems = true;
        break;
      }
    }
    List<ReportItem> choiceItems = new ArrayList<>();

    for (ReportItem item : reportItems) {
      if (item instanceof ReportDateItem && !hasResultItems) {
        choiceItems.add(item);
      }
      if (item instanceof ReportNumericItem) {
        if (temporaryExpression.isEmpty() || Objects.equals(hasResultItems, item.isResultItem())) {
          choiceItems.add(item);
        }
      }
    }
    for (int i = 0; i < temporaryExpression.size(); i++) {
      Pair<String, ReportItem> pair = temporaryExpression.get(i);

      if (i > 0) {
        Label sep = new Label(pair.getA());
        sep.addStyleName(getStyle() + "-operator");
        sep.addClickHandler(event -> {
          final List<String> options = Arrays.asList(BeeConst.STRING_PLUS, BeeConst.STRING_MINUS,
              BeeConst.STRING_ASTERISK, BeeConst.STRING_SLASH);

          Global.choice(Localized.dictionary().operator(), null, options, value -> {
            pair.setA(options.get(value));
            refresh.run();
          });
        });
        container.add(sep);
      }
      container.add(ReportItem.renderDnd(pair.getB(), temporaryExpression, i, choiceItems,
          refresh));
    }
    CustomSpan add = new CustomSpan(STYLE_ADD);
    add.addClickHandler(event -> chooseItem(choiceItems, true, item -> {
      addItem(temporaryExpression, null, item);
      refresh.run();
    }));
    container.add(add);
  }
}
