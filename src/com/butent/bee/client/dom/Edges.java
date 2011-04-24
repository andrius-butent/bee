package com.butent.bee.client.dom;

import com.google.gwt.dom.client.Style.Unit;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

public class Edges {
  public enum Edge {
    BOTTOM, LEFT, RIGHT, TOP
  }

  public static final String EMPTY_CSS_VALUE = BeeConst.STRING_ZERO;

  private static final String CSS_VALUE_SEPARATOR = BeeConst.STRING_SPACE;
  private static final Unit DEFAULT_UNIT = Unit.PX;

  private Unit bottomUnit = null;
  private Double bottomValue = null;

  private Unit leftUnit = null;
  private Double leftValue = null;

  private Unit rightUnit = null;
  private Double rightValue = null;

  private Unit topUnit = null;
  private Double topValue = null;

  public Edges() {
  }

  public Edges(Double value) {
    this(value, DEFAULT_UNIT);
  }
  
  public Edges(Double verticalValue, Double horizontalValue) {
    this(verticalValue, DEFAULT_UNIT, horizontalValue, DEFAULT_UNIT);
  }

  public Edges(Double topValue, Double horizontalValue, Double bottomValue) {
    this(topValue, DEFAULT_UNIT, horizontalValue, DEFAULT_UNIT, bottomValue, DEFAULT_UNIT);
  }
  
  public Edges(Double topValue, Double rightValue, Double bottomValue, Double leftValue) {
    this(topValue, DEFAULT_UNIT, rightValue, DEFAULT_UNIT, bottomValue, DEFAULT_UNIT,
        leftValue, DEFAULT_UNIT);
  }

  public Edges(Double value, Unit unit) {
    this(value, unit, value, unit);
  }
  
  public Edges(Double verticalValue, Unit verticalUnit,
      Double horizontalValue, Unit horizontalUnit) {
    this(verticalValue, verticalUnit, horizontalValue, horizontalUnit,
        verticalValue, verticalUnit, horizontalValue, horizontalUnit);
  }

  public Edges(Double topValue, Unit topUnit, Double horizontalValue, Unit horizontalUnit,
      Double bottomValue, Unit bottomUnit) {
    this(topValue, topUnit, horizontalValue, horizontalUnit, bottomValue, bottomUnit,
        horizontalValue, horizontalUnit);
  }
  
  public Edges(Double topValue, Unit topUnit, Double rightValue, Unit rightUnit,
      Double bottomValue, Unit bottomUnit, Double leftValue, Unit leftUnit) {
    this.topValue = topValue;
    this.topUnit = topUnit;
    this.rightValue = rightValue;
    this.rightUnit = rightUnit;
    this.bottomValue = bottomValue;
    this.bottomUnit = bottomUnit;
    this.leftValue = leftValue;
    this.leftUnit = leftUnit;
  }

  public Unit getBottomUnit() {
    return bottomUnit;
  }

  public Double getBottomValue() {
    return bottomValue;
  }

  public String getCssEdge(Edge edge) {
    Assert.notNull(edge);

    Double value;
    Unit unit;

    switch (edge) {
      case LEFT:
        value = getLeftValue();
        unit = getLeftUnit();
        break;
      case RIGHT:
        value = getRightValue();
        unit = getRightUnit();
        break;
      case TOP:
        value = getTopValue();
        unit = getTopUnit();
        break;
      case BOTTOM:
        value = getBottomValue();
        unit = getBottomUnit();
        break;
      default:
        Assert.untouchable();
        value = null;
        unit = null;
    }

    if (value == null) {
      return EMPTY_CSS_VALUE;
    } else {
      if (unit == null) {
        unit = DEFAULT_UNIT;
      }
      return BeeUtils.toString(value) + unit.getType();
    }
  }

  public String getCssValue() {
    if (isEmpty()) {
      return EMPTY_CSS_VALUE;
    }
    return getCssEdge(Edge.TOP) + CSS_VALUE_SEPARATOR
        + getCssEdge(Edge.RIGHT) + CSS_VALUE_SEPARATOR
        + getCssEdge(Edge.BOTTOM) + CSS_VALUE_SEPARATOR
        + getCssEdge(Edge.LEFT);
  }

  public Unit getLeftUnit() {
    return leftUnit;
  }

  public Double getLeftValue() {
    return leftValue;
  }

  public Unit getRightUnit() {
    return rightUnit;
  }

  public Double getRightValue() {
    return rightValue;
  }

  public Unit getTopUnit() {
    return topUnit;
  }

  public Double getTopValue() {
    return topValue;
  }

  public boolean isEmpty() {
    return getLeftValue() == null && getRightValue() == null && getTopValue() == null
        && getBottomValue() == null;
  }

  public void setBottom(Double value) {
    setBottom(value, DEFAULT_UNIT);
  }

  public void setBottom(Double value, Unit unit) {
    setBottomValue(value);
    setBottomUnit(unit);
  }

  public void setBottomUnit(Unit bottomUnit) {
    this.bottomUnit = bottomUnit;
  }

  public void setBottomValue(Double bottomValue) {
    this.bottomValue = bottomValue;
  }

  public void setEdge(Edge edge, Double value) {
    setEdge(edge, value, DEFAULT_UNIT);
  }

  public void setEdge(Edge edge, Double value, Unit unit) {
    Assert.notNull(edge);

    switch (edge) {
      case LEFT:
        setLeft(value, unit);
        break;
      case RIGHT:
        setRight(value, unit);
        break;
      case TOP:
        setTop(value, unit);
        break;
      case BOTTOM:
        setBottom(value, unit);
        break;
    }
  }

  public void setLeft(Double value) {
    setLeft(value, DEFAULT_UNIT);
  }

  public void setLeft(Double value, Unit unit) {
    setLeftValue(value);
    setLeftUnit(unit);
  }

  public void setLeftUnit(Unit leftUnit) {
    this.leftUnit = leftUnit;
  }

  public void setLeftValue(Double leftValue) {
    this.leftValue = leftValue;
  }

  public void setRight(Double value) {
    setRight(value, DEFAULT_UNIT);
  }

  public void setRight(Double value, Unit unit) {
    setRightValue(value);
    setRightUnit(unit);
  }

  public void setRightUnit(Unit rightUnit) {
    this.rightUnit = rightUnit;
  }

  public void setRightValue(Double rightValue) {
    this.rightValue = rightValue;
  }

  public void setTop(Double value) {
    setTop(value, DEFAULT_UNIT);
  }

  public void setTop(Double value, Unit unit) {
    setTopValue(value);
    setTopUnit(unit);
  }

  public void setTopUnit(Unit topUnit) {
    this.topUnit = topUnit;
  }

  public void setTopValue(Double topValue) {
    this.topValue = topValue;
  }
}
