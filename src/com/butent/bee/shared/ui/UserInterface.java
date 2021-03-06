package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.html.builder.elements.Meta;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public enum UserInterface implements HasCaption {
  DESKTOP { /* ordinal 0 */

    @Override
    public String getCaption() {
      return "Desktop";
    }

    @Override
    public Collection<Component> getComponents() {
      return EnumSet.allOf(Component.class);
    }

    @Override
    public List<Meta> getMeta() {
      return new ArrayList<>();
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("settings", "js/tinymce/js/tinymce/tinymce.min.js");
    }

    @Override
    public String getShortName() {
      return "desktop";
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  TABLET { /* ordinal 1 */

    @Override
    public String getCaption() {
      return "Tablet";
    }

    @Override
    public Collection<Component> getComponents() {
      return EnumSet.of(Component.AUTOCOMPLETE, Component.DATA_INFO, Component.DECORATORS,
          Component.DIMENSIONS, Component.FILTERS, Component.GRIDS, Component.MENU,
          Component.MONEY, Component.SETTINGS, Component.USERS);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(
          new Meta().name("gwt:property").content("screen=tablet"),
          new Meta().name("apple-mobile-web-app-capable").content("yes"),
          new Meta().name("viewport").content("width=device-width, user-scalable=yes"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("tabletsettings");
    }

    @Override
    public String getShortName() {
      return "tablet";
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  MOBILE { /* ordinal 2 */

    @Override
    public String getCaption() {
      return "Mobile";
    }

    @Override
    public Collection<Component> getComponents() {
      return EnumSet.of(Component.AUTOCOMPLETE, Component.DATA_INFO, Component.DECORATORS,
          Component.DIMENSIONS, Component.FILTERS, Component.GRIDS, Component.MENU,
          Component.MONEY, Component.SETTINGS, Component.USERS);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(
          new Meta().name("gwt:property").content("screen=mobile"),
          new Meta().name("apple-mobile-web-app-capable").content("yes"),
          new Meta().name("viewport").content("width=device-width, user-scalable=yes"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("mobilesettings");
    }

    @Override
    public String getShortName() {
      return "mobile";
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  E_COMMERCE { /* ordinal 3 */

    @Override
    public String getCaption() {
      return "E-Commerce";
    }

    @Override
    public Collection<Component> getComponents() {
      return EnumSet.of(Component.DATA_INFO, Component.AUTOCOMPLETE, Component.USERS);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(new Meta().name("gwt:property").content("screen=ec"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("ecsettings");
    }

    @Override
    public String getShortName() {
      return "ec";
    }

    @Override
    public List<String> getStyleSheets() {
      List<String> sheets = getMainStyleSheets();
      sheets.add(EcConstants.CLIENT_STYLE_SHEET);
      return sheets;
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  SELF_SERVICE { /* ordinal 4 */

    @Override
    public String getCaption() {
      return "Self-service";
    }

    @Override
    public Collection<Component> getComponents() {
      return EnumSet.of(Component.AUTOCOMPLETE, Component.DATA_INFO, Component.DECORATORS,
          Component.FILTERS, Component.GRIDS, Component.SETTINGS, Component.USERS);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(new Meta().name("gwt:property").content("screen=trss"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("trsettings");
    }

    @Override
    public String getShortName() {
      return "trss";
    }

    @Override
    public List<String> getStyleSheets() {
      List<String> sheets = getMainStyleSheets();
      sheets.add(Module.TRANSPORT.getStyleSheet(null));
      return sheets;
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  TRADE_ACTS { /* ordinal 5 */

    @Override
    public String getCaption() {
      return "Trade Acts";
    }

    @Override
    public Collection<Component> getComponents() {
      return EnumSet.of(Component.AUTOCOMPLETE, Component.DATA_INFO, Component.DECORATORS,
          Component.FILTERS, Component.GRIDS, Component.SETTINGS, Component.USERS);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(new Meta().name("gwt:property").content("screen=acts"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("actsettings");
    }

    @Override
    public String getShortName() {
      return "acts";
    }

    @Override
    public List<String> getStyleSheets() {
      List<String> sheets = getMainStyleSheets();
      sheets.add(Module.TRADE.getStyleSheet(SubModule.ACTS));
      return sheets;
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  };

  public enum Component {
    AUTOCOMPLETE(false),
    CHATS(false),
    DATA_INFO(false),
    DECORATORS(false),
    DIMENSIONS(false),
    FAVORITES(false),
    FILTERS(false),
    GRIDS(false),
    MAIL(false),
    MENU(false),
    MONEY(false),
    NEWS(false),
    REPORTS(false),
    SETTINGS(false),
    USERS(true),
    WORKSPACES(false);

    private final boolean required;

    Component(boolean required) {
      this.required = required;
    }

    public boolean isRequired() {
      return required;
    }

    public String key() {
      return name().toLowerCase();
    }
  }

  public static final UserInterface DEFAULT = DESKTOP;

  public static final String MAIN_STYLE_SHEET = "bee";

  public static final String TITLE = "B-NOVO";

  public static UserInterface getByShortName(String input) {
    for (UserInterface ui : values()) {
      if (BeeUtils.same(ui.getShortName(), input)) {
        return ui;
      }
    }

    for (UserInterface ui : values()) {
      if (BeeUtils.containsSame(input, ui.getShortName())) {
        return ui;
      }
    }

    return EnumUtils.getEnumByName(UserInterface.class, input);
  }

  public static Collection<Component> getRequiredComponents() {
    EnumSet<Component> components = EnumSet.noneOf(Component.class);

    for (Component component : Component.values()) {
      if (component.isRequired()) {
        components.add(component);
      }
    }

    return components;
  }

  public static UserInterface normalize(UserInterface ui) {
    return (ui == null) ? DEFAULT : ui;
  }

  public abstract Collection<Component> getComponents();

  public abstract List<Meta> getMeta();

  public abstract List<String> getScripts();

  public abstract String getShortName();

  public List<String> getExternalScripts() {
    return Lists.newArrayList("rtcadapter", "micromarkdown");
  }

  public List<String> getStyleSheets() {
    List<String> sheets = getMainStyleSheets();

    sheets.add("misc");
    sheets.addAll(Module.getEnabledStyleSheets());

    return sheets;
  }

  public abstract String getTitle();

  public boolean hasComponent(Component component) {
    return getComponents().contains(component);
  }

  public boolean hasMenu() {
    return getComponents().contains(Component.MENU);
  }

  protected List<String> getMainStyleSheets() {
    List<String> sheets = new ArrayList<>();

    sheets.add(MAIN_STYLE_SHEET);
    sheets.addAll(getComponentStyleSheets());

    return sheets;
  }

  private List<String> getComponentStyleSheets() {
    List<String> sheets = new ArrayList<>();
    if (getComponents().contains(Component.CHATS)) {
      sheets.add("chat");
    }
    return sheets;
  }
}
