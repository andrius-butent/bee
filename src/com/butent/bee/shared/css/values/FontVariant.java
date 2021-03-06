package com.butent.bee.shared.css.values;

import com.butent.bee.shared.css.HasCssName;

public enum FontVariant implements HasCssName {
  NORMAL {
    @Override
    public String getCssName() {
      return "normal";
    }
  },
  NONE {
    @Override
    public String getCssName() {
      return "none";
    }
  },
  HISTORICAL_FORMS {
    @Override
    public String getCssName() {
      return "historical-forms";
    }
  },
  SMALL_CAPS {
    @Override
    public String getCssName() {
      return "small-caps";
    }
  },
  ALL_SMALL_CAPS {
    @Override
    public String getCssName() {
      return "all-small-caps";
    }
  },
  PETITE_CAPS {
    @Override
    public String getCssName() {
      return "petite-caps";
    }
  },
  ALL_PETITE_CAPS {
    @Override
    public String getCssName() {
      return "all-petite-caps";
    }
  },
  UNICASE {
    @Override
    public String getCssName() {
      return "unicase";
    }
  },
  TITLING_CAPS {
    @Override
    public String getCssName() {
      return "titling-caps";
    }
  },
  ORDINAL {
    @Override
    public String getCssName() {
      return "ordinal";
    }
  },
  SLASHED_ZERO {
    @Override
    public String getCssName() {
      return "slashed-zero";
    }
  },
  RUBY {
    @Override
    public String getCssName() {
      return "ruby";
    }
  }
}
