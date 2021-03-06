package com.butent.bee.shared.css.values;

import com.butent.bee.shared.css.HasCssName;

public enum UnicodeBidi implements HasCssName {
  NORMAL {
    @Override
    public String getCssName() {
      return "normal";
    }
  },
  EMBED {
    @Override
    public String getCssName() {
      return "embed";
    }
  },
  BIDI_OVERRIDE {
    @Override
    public String getCssName() {
      return "bidi-override";
    }
  },
  INHERIT {
    @Override
    public String getCssName() {
      return "inherit";
    }
  }
}
