package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.FertileElement;
import com.butent.bee.shared.html.builder.Node;

import java.util.List;

public class Ins extends FertileElement {

  public Ins() {
    super();
  }

  public Ins addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Ins append(List<? extends Node> nodes) {
    super.appendChildren(nodes);
    return this;
  }

  public Ins append(Node... nodes) {
    super.appendChildren(nodes);
    return this;
  }

  public Ins cite(String value) {
    setAttribute(Attributes.CITE, value);
    return this;
  }

  public Ins dateTime(String value) {
    setAttribute(Attributes.DATE_TIME, value);
    return this;
  }

  public Ins id(String value) {
    setId(value);
    return this;
  }

  public Ins insert(int index, Node child) {
    super.insertChild(index, child);
    return this;
  }

  public Ins lang(String value) {
    setLang(value);
    return this;
  }

  public Ins remove(Node child) {
    super.removeChild(child);
    return this;
  }

  public Ins text(String text) {
    super.appendText(text);
    return this;
  }

  public Ins title(String value) {
    setTitle(value);
    return this;
  }
}
