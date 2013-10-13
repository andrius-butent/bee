package com.butent.bee.shared.html.builder;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;

import java.util.List;

public class FertileElement extends Element {

  private final List<Node> children = Lists.newArrayList();

  protected FertileElement() {
    super();
  }

  public void appendChild(Node child) {
    Assert.notNull(child);
    Assert.isTrue(this != child, "cannot append a node to itself");

    child.setParent(this);
    children.add(child);
  }

  public void appendChildren(List<? extends Node> nodes) {
    if (nodes != null) {
      for (Node node : nodes) {
        appendChild(node);
      }
    }
  }

  public void appendChildren(Node... nodes) {
    if (nodes != null) {
      for (Node node : nodes) {
        appendChild(node);
      }
    }
  }

  public void appendText(String text) {
    appendChild(new Text(text));
  }

  @Override
  public String build(int indentStart, int indentStep) {
    if (children.isEmpty()) {
      return super.build(indentStart, indentStep);

    } else {
      StringBuilder sb = new StringBuilder(Node.indent(indentStart, buildStart()));
      
      int textCount = 0;
      for (Node child : children) {
        if (child instanceof Text) {
          textCount++;
        }
      }
      
      boolean indentChildren = textCount != 1;

      int nextIndent;
      if (!indentChildren) {
        nextIndent = BeeConst.UNDEF;
      } else if (indentStart >= 0 && indentStep > 0) {
        nextIndent = indentStart + indentStep;
      } else {
        nextIndent = indentStart;
      }

      for (Node child : children) {
        sb.append(child.build(nextIndent, indentStep));
      }

      sb.append(Node.indent(indentChildren ? indentStart : BeeConst.UNDEF, buildEnd()));
      return sb.toString();
    }
  }

  @Override
  protected String buildEnd() {
    StringBuilder sb = new StringBuilder("</");
    sb.append(getTag());
    sb.append(">");
    return sb.toString();
  }

  @Override
  protected String buildStart() {
    return super.buildStart() + ">";
  }

  public void clearChildren() {
    children.clear();
  }

  public Node getChild(int index) {
    return children.get(index);
  }

  public List<Node> getChildren() {
    return children;
  }

  public boolean hasComment() {
    for (Node child : children) {
      if (child instanceof Comment) {
        return true;
      }
    }
    return false;
  }

  public boolean hasText() {
    for (Node child : children) {
      if (child instanceof Text) {
        return true;
      }
    }
    return false;
  }

  public void insertChild(int index, Node child) {
    Assert.betweenInclusive(index, 0, children.size());
    Assert.notNull(child);
    Assert.isTrue(this != child, "cannot append a node to itself");

    child.setParent(this);
    children.add(index, child);
  }

  public void removeChild(Node child) {
    children.remove(child);
  }
}
