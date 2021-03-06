package com.butent.bee.shared.css.values;

import com.butent.bee.shared.css.HasCssName;

public enum All implements HasCssName {
  INITIAL {
    @Override
    public String getCssName() {
      return "initial";
    }
  },
  INHERIT {
    @Override
    public String getCssName() {
      return "inherit";
    }
  },
  UNSET {
    @Override
    public String getCssName() {
      return "unset";
    }
  }
}
