package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.Element;

public class Param extends Element {

  public Param() {
    super();
  }

  public Param addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Param id(String value) {
    setId(value);
    return this;
  }

  public Param lang(String value) {
    setLang(value);
    return this;
  }

  public Param name(String value) {
    setAttribute(Attributes.NAME, value);
    return this;
  }

  public Param title(String value) {
    setTitle(value);
    return this;
  }

  public Param value(String value) {
    setAttribute(Attributes.VALUE, value);
    return this;
  }
}
