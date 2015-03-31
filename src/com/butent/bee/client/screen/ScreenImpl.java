package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.Bee;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Screen;
import com.butent.bee.client.Settings;
import com.butent.bee.client.Users;
import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.Animation;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.CustomComplex;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.menu.MenuCommand;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.screen.TilePanel.Tile;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasProgress;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles default (desktop) screen implementation.
 */

public class ScreenImpl implements Screen {

  private static final BeeLogger logger = LogUtils.getLogger(ScreenImpl.class);

  private static final Set<Direction> hidableDirections = EnumSet.of(Direction.WEST,
      Direction.NORTH, Direction.EAST);

  private Split screenPanel;
  private static final String CONTAINER = "Container";
  private static final String TASK = "Task";
  private static final String COMPANY = "Company";
  private static final String DOCUMENT = "Document";
  private static final String NOTE = "Note";
  private static final String ANNOUNCEMENT = "Announcement";
  private static final String PHOTO_URL = "images/photo/";
  private static FaLabel userLabel = new FaLabel(FontAwesome.USER);
  private static FaLabel emailLabel = new FaLabel(FontAwesome.ENVELOPE_O);
  private static Flow flowUserContainer = new Flow();
  private static Flow flowEmailContainer = new Flow();
  private static Flow flowOnlineUserSize = new Flow();
  private static Flow flowOnlineEmailSize = new Flow();
  private static Flow flowNewsSize = new Flow();

  private static final String DEFAULT_PHOTO_IMAGE = "images/silver/person_profile.png";
  private CentralScrutinizer centralScrutinizer;
  public static final HtmlTable NOTIFICATION_CONTENT = new HtmlTable(BeeConst.CSS_CLASS_PREFIX
      + "NotificationBar-Content");

  private Workspace workspace;
  private HasWidgets commandPanel;

  private HasWidgets menuPanel;

  private HasWidgets userPhotoContainer;
  private HasHtml userSignature;

  private Notification notification;

  private Panel progressPanel;

  private final Map<Direction, Integer> hidden = new EnumMap<>(Direction.class);

  private Toggle northToggle;
  private Toggle westToggle;

  private Toggle maximizer;

  public ScreenImpl() {
  }

  @Override
  public boolean activateDomainEntry(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return false;
    } else {
      return getCentralScrutinizer().activate(domain, key);
    }
  }

  @Override
  public void activateWidget(IdentifiableWidget widget) {
    Assert.notNull(widget, "activateWidget: widget is null");
    getWorkspace().activateWidget(widget);
  }

  @Override
  public void addCommandItem(IdentifiableWidget widget) {
    Assert.notNull(widget);
    if (getCommandPanel() == null) {
      logger.severe(NameUtils.getName(this), "command panel not available");
    } else {
      widget.asWidget().addStyleName(BeeConst.CSS_CLASS_PREFIX + "MainCommandPanelItem");
      getCommandPanel().add(widget.asWidget());
    }
  }

  @Override
  public void addDomainEntry(Domain domain, IdentifiableWidget widget, Long key, String caption) {
    if (getCentralScrutinizer() == null) {
      logger.severe("cannot add domain", domain);
    } else {
      getCentralScrutinizer().add(domain, widget, key, caption);
    }
  }

  @Override
  public String addProgress(HasProgress widget) {
    if (getProgressPanel() != null && widget != null) {
      if (!getProgressPanel().iterator().hasNext()) {
        showProgressPanel();
      }

      getProgressPanel().add(widget);
      return widget.getId();
    } else {
      return null;
    }
  }

  @Override
  public void clearNotifications() {
    if (getNotification() != null) {
      getNotification().clear();
    }
  }

  @Override
  public void closeAll() {
    if (getWorkspace() != null) {
      getWorkspace().clear();
    }
  }

  @Override
  public void closeWidget(IdentifiableWidget widget) {
    Assert.notNull(widget, "closeWidget: widget is null");

    if (UiHelper.isModal(widget.asWidget())) {
      UiHelper.closeDialog(widget.asWidget());
    } else {
      getWorkspace().closeWidget(widget);
    }
  }

  @Override
  public boolean containsDomainEntry(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return false;
    } else {
      return getCentralScrutinizer().contains(domain, key);
    }
  }

  @Override
  public int getActivePanelHeight() {
    Tile activeTile = getWorkspace().getActiveTile();
    return (activeTile == null) ? 0 : activeTile.getOffsetHeight();
  }

  @Override
  public int getActivePanelWidth() {
    Tile activeTile = getWorkspace().getActiveTile();
    return (activeTile == null) ? 0 : activeTile.getOffsetWidth();
  }

  @Override
  public IdentifiableWidget getActiveWidget() {
    return getWorkspace().getActiveContent();
  }

  @Override
  public HasWidgets getCommandPanel() {
    return commandPanel;
  }

  @Override
  public Flow getDomainHeader(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return null;
    } else {
      return getCentralScrutinizer().getDomainHeader(domain, key);
    }
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = getWorkspace().getExtendedInfo();

    if (!hidden.isEmpty()) {
      for (Map.Entry<Direction, Integer> entry : hidden.entrySet()) {
        info.add(new ExtendedProperty("Hidden", entry.getKey().name(),
            BeeUtils.toString(entry.getValue())));
      }
    }

    return info;
  }

  @Override
  public int getHeight() {
    return getScreenPanel().getOffsetHeight();
  }

  @Override
  public Set<Direction> getHiddenDirections() {
    return hidden.keySet();
  }

  @Override
  public List<IdentifiableWidget> getOpenWidgets() {
    return getWorkspace().getOpenWidgets();
  }

  @Override
  public Split getScreenPanel() {
    return screenPanel;
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.DESKTOP;
  }

  @Override
  public int getWidth() {
    return getScreenPanel().getOffsetWidth();
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public boolean hasNotifications() {
    return getNotification() != null && getNotification().isActive();
  }

  @Override
  public void hideDirections(Set<Direction> directions) {
    boolean changed = false;

    if (!BeeUtils.isEmpty(directions)) {
      for (Direction direction : directions) {
        changed |= expand(direction, false);
      }
    }

    if (changed) {
      getScreenPanel().doLayout();
      refreshExpanders();
    }
  }

  @Override
  public void init() {
    createUi();
  }

  @Override
  public void notifyInfo(String... messages) {
    if (getNotification() != null) {
      getNotification().info(messages);
    }
  }

  @Override
  public void notifySevere(String... messages) {
    if (getNotification() != null) {
      getNotification().severe(messages);
    }
  }

  @Override
  public void notifyWarning(String... messages) {
    if (getNotification() != null) {
      getNotification().warning(messages);
    }
  }

  @Override
  public void onLoad() {
    Global.getSearch().focus();

    if (!Global.getSpaces().isEmpty() && !containsDomainEntry(Domain.WORKSPACES, null)) {
      addDomainEntry(Domain.WORKSPACES, Global.getSpaces().getPanel(), null, null);
    }
    if (!Global.getReportSettings().isEmpty() && !containsDomainEntry(Domain.REPORTS, null)) {
      addDomainEntry(Domain.REPORTS, Global.getReportSettings().getPanel(), null, null);
    }

    if (Global.getNewsAggregator().hasNews()) {
      activateDomainEntry(Domain.NEWS, null);
    }
  }

  @Override
  public void onWidgetChange(IdentifiableWidget widget) {
    Assert.notNull(widget, "onWidgetChange: widget is null");
    getWorkspace().onWidgetChange(widget);
  }

  @Override
  public boolean removeDomainEntry(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return false;
    } else {
      return getCentralScrutinizer().remove(domain, key);
    }
  }

  @Override
  public void removeProgress(String id) {
    if (getProgressPanel() != null && !BeeUtils.isEmpty(id)) {
      Widget item = DomUtils.getChildById(getProgressPanel(), id);

      if (item != null) {
        getProgressPanel().remove(item);
        if (!getProgressPanel().iterator().hasNext()) {
          hideProgressPanel();
        }
      }
    }
  }

  @Override
  public void restore(List<String> spaces, boolean append) {
    if (getWorkspace() != null) {
      getWorkspace().restore(spaces, append);
    }
  }

  @Override
  public String serialize() {
    if (getWorkspace() == null) {
      return null;
    } else {
      return getWorkspace().serialize();
    }
  }

  @Override
  public void showInNewPlace(IdentifiableWidget widget) {
    getWorkspace().openInNewPlace(widget);
  }

  @Override
  public void show(IdentifiableWidget widget) {
    if (BeeKeeper.getUser().openInNewTab()) {
      showInNewPlace(widget);
    } else {
      updateActivePanel(widget);
    }
  }

  @Override
  public void start(UserData userData) {
    updateUserData(userData);

    if (getCentralScrutinizer() != null) {
      getCentralScrutinizer().start();
    }

    if (getWorkspace() != null) {
      if (getCentralScrutinizer() != null && getWorkspace() != null) {
        getWorkspace().addActiveWidgetChangeHandler(getCentralScrutinizer());
      }

      Previewer.registerMouseDownPriorHandler(getWorkspace());
    }
  }

  @Override
  public void updateActivePanel(IdentifiableWidget widget) {
    getWorkspace().updateActivePanel(widget);
  }

  @Override
  public void updateCommandPanel(IdentifiableWidget widget) {
    updatePanel(getCommandPanel(), widget);
  }

  @Override
  public void updateMenu(IdentifiableWidget widget) {
    updatePanel(getMenuPanel(), widget);
  }

  @Override
  public boolean updateProgress(String id, String label, double value) {
    if (getProgressPanel() != null && !BeeUtils.isEmpty(id)) {
      Widget item = DomUtils.getChildById(getProgressPanel(), id);

      if (item instanceof HasProgress) {
        ((HasProgress) item).update(label, value);
        return true;
      }
    }
    return false;
  }

  @Override
  public void updateUserData(UserData userData) {
    if (userData != null) {
      if (getUserPhotoContainer() != null) {
        getUserPhotoContainer().clear();
        final Image image;

        String photoFileName = userData.getPhotoFileName();
        if (!BeeUtils.isEmpty(photoFileName)) {
          image = new Image(PhotoRenderer.getUrl(photoFileName));
        } else {
          image = new Image(DEFAULT_PHOTO_IMAGE);
        }

        image.setAlt(userData.getLogin());
        image.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserPhoto");

        image.addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent event) {

            NOTIFICATION_CONTENT.setWidget(0, 0, createUserPanel(NOTIFICATION_CONTENT));
            NOTIFICATION_CONTENT.setWidget(1, 0, createCalendarPanel());

            if (BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.NEWS)) {
              NOTIFICATION_CONTENT.setWidget(2, 0, Global.getNewsAggregator().getNewsPanel()
                  .asWidget());
            }

            final Popup popup = new Popup(OutsideClick.CLOSE);
            popup.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NotificationBar");
            popup.addStyleName("animated fadeInRight");
            popup.setWidget(NOTIFICATION_CONTENT);
            popup.setHideOnEscape(true);

            popup.setAnimationEnabled(true);
            popup.setAnimation(new Animation(1000) {

              @Override
              protected boolean run(double elapsed) {
                if (isCanceled()) {
                  return false;
                } else {
                  popup.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NotificationBar");
                  popup.addStyleName("animated fadeOutRight");
                  return true;
                }
              }

              @Override
              public void start() {
                setPopup(popup);
                super.start();
              }

              @Override
              protected void onComplete() {
                super.onComplete();
              }
            });

            popup.getAnimation().start();
            popup.showRelativeTo(image.getElement());
          }
        });

        flowNewsSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NewsSize-None");
        getUserPhotoContainer().add(flowNewsSize);
        getUserPhotoContainer().add(image);
      }
    }

    if (getUserSignature() != null) {
      getUserSignature().setText(BeeUtils.trim(userData.getUserSign()));
    }
  }

  protected void activateShell() {
    if (getCentralScrutinizer() != null) {
      getCentralScrutinizer().activateShell();

    } else if (getScreenPanel() != null && getScreenPanel().getDirectionSize(Direction.WEST) <= 0) {
      List<Widget> children = getScreenPanel().getDirectionChildren(Direction.WEST);
      for (Widget widget : children) {
        if (UiHelper.isOrHasChild(widget, Shell.class)) {
          getScreenPanel().setDirectionSize(Direction.WEST, getWidth() / 5, true);
          break;
        }
      }
    }
  }

  protected void bindShellActivation(IdentifiableWidget widget) {
    final String id = widget.getId();

    Binder.addClickHandler(widget.asWidget(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (EventUtils.hasModifierKey(event.getNativeEvent())
            && EventUtils.isTargetId(event, id)) {
          activateShell();
        }
      }
    });
  }

  protected Panel createCommandPanel() {
    return new Flow(BeeConst.CSS_CLASS_PREFIX + "MainCommandPanel");
  }

  protected Widget createCopyright(String stylePrefix) {
    Flow copyright = new Flow();
    copyright.addStyleName(stylePrefix + "Copyright");

    Image logo = new Image(UiConstants.wtfplLogo());
    logo.addStyleName(stylePrefix + "Copyright-logo");
    copyright.add(logo);

    Label label = new Label(UiConstants.wtfplLabel());
    label.addStyleName(stylePrefix + "Copyright-label");
    copyright.add(label);

    final String url = UiConstants.wtfplUrl();
    copyright.setTitle(url);

    copyright.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        BrowsingContext.open(url);
      }
    });

    return copyright;
  }

  protected void createExpanders() {
    CustomComplex container = new CustomComplex(DomUtils.createElement(Tags.NAV),
        BeeConst.CSS_CLASS_PREFIX + "Workspace-expander");

    Toggle toggle = new Toggle(FontAwesome.LONG_ARROW_LEFT, FontAwesome.LONG_ARROW_RIGHT,
        BeeConst.CSS_CLASS_PREFIX + "west-toggle", false);
    setWestToggle(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getWestToggle().isChecked()) {
          expand(Direction.WEST, true);
        } else {
          compress(Direction.WEST, true);
        }

        refreshExpanders();
      }
    });

    container.add(toggle);

    toggle = new Toggle(FontAwesome.LONG_ARROW_UP, FontAwesome.LONG_ARROW_DOWN,
        BeeConst.CSS_CLASS_PREFIX + "north-toggle", false);
    setNorthToggle(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getNorthToggle().isChecked()) {
          expand(Direction.NORTH, true);
        } else {
          compress(Direction.NORTH, true);
        }

        refreshExpanders();
      }
    });

    container.add(toggle);

    toggle = new Toggle(FontAwesome.EXPAND, FontAwesome.COMPRESS,
        BeeConst.CSS_CLASS_PREFIX + "workspace-maximizer", false);
    setMaximizer(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getMaximizer().isChecked()) {
          for (Direction direction : hidableDirections) {
            expand(direction, false);
          }

        } else {
          for (Map.Entry<Direction, Integer> entry : hidden.entrySet()) {
            getScreenPanel().setDirectionSize(entry.getKey(), entry.getValue(), false);
          }
          hidden.clear();
        }

        getScreenPanel().doLayout();
        refreshExpanders();
      }
    });

    container.add(toggle);

    getWorkspace().addToTabBar(container);

    refreshExpanders();
  }

  protected Widget createLogo(ScheduledCommand command) {
    String imageUrl = Settings.getLogoImage();
    if (BeeUtils.isEmpty(imageUrl)) {
      return null;
    }

    Image widget = new Image(imageUrl);
    widget.setAlt("logo");

    final String title = Settings.getLogoTitle();
    if (!BeeUtils.isEmpty(title)) {
      widget.setTitle(title);
    }

    final String openUrl = Settings.getLogoOpen();
    if (BeeUtils.isEmpty(openUrl)) {
      if (command == null) {
        widget.getElement().getStyle().setCursor(Cursor.DEFAULT);
      } else {
        widget.setCommand(command);
      }

    } else {
      if (BeeUtils.isEmpty(title)) {
        widget.setTitle(openUrl);
      }

      widget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          BrowsingContext.open(openUrl);
        }
      });
    }

    return widget;
  }

  protected Panel createMenuPanel() {
    return new Flow(BeeConst.CSS_CLASS_PREFIX + "MainMenu");
  }

  protected Widget createSearch() {
    return Global.getSearchWidget();
  }

  protected void createUi() {
    Split p = new Split(0);
    StyleUtils.occupy(p);
    p.addStyleName(getScreenStyle());

    Pair<? extends IdentifiableWidget, Integer> north = initNorth();
    if (north != null) {
      p.addNorth(north.getA(), north.getB());
      bindShellActivation(north.getA());
    }

    Pair<? extends IdentifiableWidget, Integer> south = initSouth();
    if (south != null) {
      p.addSouth(south.getA(), south.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> west = initWest();
    if (west != null) {
      p.addWest(west.getA(), west.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> east = initEast();
    if (east != null) {
      p.addEast(east.getA(), east.getB());
    }

    IdentifiableWidget center = initCenter();
    if (center != null) {
      p.add(center);
    }

    BodyPanel.get().add(p);
    setScreenPanel(p);

    if (getWorkspace() != null) {
      createExpanders();
    }
  }

  protected Widget createUserContainer() {
    Horizontal userContainer = new Horizontal();

    if (Settings.showUserPhoto()) {
      Flow photoContainer = new Flow(BeeConst.CSS_CLASS_PREFIX + "UserPhotoContainer");
      userContainer.add(photoContainer);
      setUserPhotoContainer(photoContainer);
    }
    return userContainer;
  }

  protected int getNorthHeight(int defHeight) {
    return BeeUtils.positive(Settings.getInt("northHeight"), defHeight);
  }

  protected Notification getNotification() {
    return notification;
  }

  protected String getScreenStyle() {
    return BeeConst.CSS_CLASS_PREFIX + "Screen";
  }

  protected void hideProgressPanel() {
    getScreenPanel().setWidgetSize(getProgressPanel(), 0);
  }

  protected IdentifiableWidget initCenter() {
    Workspace area = new Workspace();
    setWorkspace(area);
    return area;
  }

  protected Pair<? extends IdentifiableWidget, Integer> initEast() {
    return Pair.of(ClientLogManager.getLogPanel(), ClientLogManager.getInitialPanelSize());
  }

  protected Pair<? extends IdentifiableWidget, Integer> initNorth() {
    Complex panel = new Complex();
    panel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "NorthContainer");

    Widget logo = createLogo(null);
    if (logo != null) {
      logo.addStyleName(BeeConst.CSS_CLASS_PREFIX + "Logo");
      panel.add(logo);
    }

    Widget search = createSearch();
    if (search != null) {
      panel.add(search);
    }

    Panel commandContainer = createCommandPanel();
    if (commandContainer != null) {
      panel.add(commandContainer);

      commandContainer.add(onlineEmail());
      commandContainer.add(onlineUsers());

      setCommandPanel(commandContainer);
    }

    Panel menuContainer = createMenuPanel();
    if (menuContainer != null) {
      panel.add(menuContainer);
      setMenuPanel(menuContainer);
    }

    Widget userContainer = createUserContainer();
    userContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserContainer");
    panel.add(userContainer);

    Notification nw = new Notification();
    nw.addStyleName(BeeConst.CSS_CLASS_PREFIX + "MainNotificationContainer");
    panel.add(nw);
    setNotification(nw);

    return Pair.of(panel, getNorthHeight(112));
  }

  protected Pair<? extends IdentifiableWidget, Integer> initSouth() {
    Flow panel = new Flow();
    panel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "ProgressPanel");
    setProgressPanel(panel);

    return Pair.of(panel, 0);
  }

  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    setCentralScrutinizer(new CentralScrutinizer());

    Flow top = new Flow();
    Flow containerEnv = new Flow();
    FaLabel bookmark = new FaLabel(FontAwesome.BOOKMARK);
    Label title = new Label();
    final Label createButton = new Label("+ " + Localized.getConstants().create());

    createButton.addStyleName(BeeConst.CSS_CLASS_PREFIX + "CreateButton");
    top.addStyleName(BeeConst.CSS_CLASS_PREFIX + "TopWest");
    title.setText(Localized.getConstants().myEnvironment());
    containerEnv.addStyleName(BeeConst.CSS_CLASS_PREFIX + "MyEnvironment");

    createButton.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {

        final HtmlTable table = new HtmlTable(BeeConst.CSS_CLASS_PREFIX + "create-NewForm");
        int r = 0;

        if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.TASKS))
            && BeeKeeper.getUser().canCreateData(TaskConstants.VIEW_TASKS)) {
          table.setText(r, 0, Localized.getConstants().crmNewTask());
          DomUtils.setDataProperty(table.getRow(r++), CONTAINER, TASK);
        }

        if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.CLASSIFIERS))
          && BeeKeeper.getUser().canCreateData(ClassifierConstants.VIEW_COMPANIES)) {
          table.setText(r, 0, Localized.getConstants().newClient());
          DomUtils.setDataProperty(table.getRow(r++), CONTAINER, COMPANY);
        }

        if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.DOCUMENTS))
          && BeeKeeper.getUser().canCreateData(DocumentConstants.VIEW_DOCUMENTS)) {
          table.setText(r, 0, Localized.getConstants().documentNew());
          DomUtils.setDataProperty(table.getRow(r++), CONTAINER, DOCUMENT);
        }

        if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.TASKS))
          && BeeKeeper.getUser().canCreateData(TaskConstants.VIEW_TODO_LIST)) {
          table.setText(r, 0, Localized.getConstants().crmNewTodoItem());
          DomUtils.setDataProperty(table.getRow(r++), CONTAINER, NOTE);
        }

        if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.DISCUSSIONS))
            && BeeKeeper.getUser().canCreateData(DiscussionsConstants.VIEW_DISCUSSIONS)) {
          table.setText(r, 0, Localized.getConstants().announcementNew());
          DomUtils.setDataProperty(table.getRow(r++), CONTAINER, ANNOUNCEMENT);
        }

        if (r > 0) {
          table.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent ev) {
              Element targetElement = EventUtils.getEventTargetElement(ev);
              TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);
              String index = DomUtils.getDataProperty(rowElement, CONTAINER);
              UiHelper.closeDialog(table);

              switch (index) {
                case TASK:
                  RowFactory.createRow(TaskConstants.VIEW_TASKS);
                  break;

                case COMPANY:
                  RowFactory.createRow(ClassifierConstants.VIEW_COMPANIES);
                  break;

                case DOCUMENT:
                  RowFactory.createRow(DocumentConstants.VIEW_DOCUMENTS);
                  break;

                case NOTE:
                  RowFactory.createRow(TaskConstants.VIEW_TODO_LIST);
                  break;

                case ANNOUNCEMENT:
                  DataInfo data = Data.getDataInfo(VIEW_DISCUSSIONS);
                  final BeeColumn beeCol = data.getColumn(COL_TOPIC);
                  BeeRow emptyRow = RowFactory.createEmptyRow(data, true);
                  if (beeCol != null) {
                    beeCol.setNullable(false);
                  }
                  RowFactory.createRow(FORM_NEW_DISCUSSION, Localized.getConstants()
                      .announcementNew(), data,
                      emptyRow, null);
                  break;
              }
            }
          });
          Popup popup = new Popup(OutsideClick.CLOSE);
          popup.addStyleName(BeeConst.CSS_CLASS_PREFIX + "PopupCreate");
          popup.setWidget(table);
          popup.setHideOnEscape(true);
          popup.showRelativeTo(createButton.getElement());
        }

      }
    });

    top.add(createButton);
    containerEnv.add(bookmark);
    containerEnv.add(title);
    top.add(containerEnv);

    Flow panel = new Flow();
    panel.add(top);
    panel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "WestContainer");
    panel.add(getCentralScrutinizer());
    panel.add(createCopyright(BeeConst.CSS_CLASS_PREFIX));

    return Pair.of(panel, 200);
  }

  protected void onUserSignatureClick() {
    BeeRow row = BeeKeeper.getUser().getSettingsRow();
    if (row != null) {
      RowEditor.open(AdministrationConstants.VIEW_USER_SETTINGS, row, Opener.MODAL,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              BeeKeeper.getUser().updateSettings(result);

              if (getCentralScrutinizer() != null) {
                getCentralScrutinizer().maybeUpdateHeaders();
              }
            }
          });
    }
  }

  protected void setMenuPanel(HasWidgets menuPanel) {
    this.menuPanel = menuPanel;
  }

  protected void setNotification(Notification notification) {
    this.notification = notification;
  }

  protected void setProgressPanel(Panel progressPanel) {
    this.progressPanel = progressPanel;
  }

  protected void setScreenPanel(Split screenPanel) {
    this.screenPanel = screenPanel;
  }

  protected void setUserPhotoContainer(HasWidgets userPhotoContainer) {
    this.userPhotoContainer = userPhotoContainer;
  }

  protected void setUserSignature(HasHtml userSignature) {
    this.userSignature = userSignature;
  }

  protected void showProgressPanel() {
    getScreenPanel().setWidgetSize(getProgressPanel(), 32);
  }

  private boolean compress(Direction direction, boolean doLayout) {
    Integer size = hidden.remove(direction);

    if (BeeUtils.isPositive(size)) {
      getScreenPanel().setDirectionSize(direction, size, doLayout);
      return true;

    } else {
      return false;
    }
  }

  private boolean expand(Direction direction, boolean doLayout) {
    int size = getScreenPanel().getDirectionSize(direction);

    if (size > 0) {
      hidden.put(direction, size);
      getScreenPanel().setDirectionSize(direction, 0, doLayout);
      return true;

    } else {
      return false;
    }
  }

  private CentralScrutinizer getCentralScrutinizer() {
    return centralScrutinizer;
  }

  private Toggle getMaximizer() {
    return maximizer;
  }

  private HasWidgets getMenuPanel() {
    return menuPanel;
  }

  private Toggle getNorthToggle() {
    return northToggle;
  }

  private Panel getProgressPanel() {
    return progressPanel;
  }

  private HasWidgets getUserPhotoContainer() {
    return userPhotoContainer;
  }

  private HasHtml getUserSignature() {
    return userSignature;
  }

  private Toggle getWestToggle() {
    return westToggle;
  }

  private void refreshExpanders() {
    boolean checked;
    String title;

    if (getWestToggle() != null) {
      checked = hidden.containsKey(Direction.WEST);

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceEnlargeToLeft();
      getWestToggle().setTitle(title);

      if (getWestToggle().isChecked() != checked) {
        getWestToggle().setChecked(checked);
      }
    }

    if (getNorthToggle() != null) {
      checked = hidden.containsKey(Direction.NORTH);

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceEnlargeUp();
      getNorthToggle().setTitle(title);

      if (getNorthToggle().isChecked() != checked) {
        getNorthToggle().setChecked(checked);
      }
    }

    if (getMaximizer() != null) {
      checked = true;
      for (Direction direction : hidableDirections) {
        if (getScreenPanel().getDirectionSize(direction) > 0) {
          checked = false;
          break;
        }
      }

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceMaxSize();
      getMaximizer().setTitle(title);

      if (getMaximizer().isChecked() != checked) {
        getMaximizer().setChecked(checked);
      }
    }
  }

  private void setCentralScrutinizer(CentralScrutinizer centralScrutinizer) {
    this.centralScrutinizer = centralScrutinizer;
  }

  private void setCommandPanel(HasWidgets commandPanel) {
    this.commandPanel = commandPanel;
  }

  private void setMaximizer(Toggle maximizer) {
    this.maximizer = maximizer;
  }

  private void setNorthToggle(Toggle northToggle) {
    this.northToggle = northToggle;
  }

  private void setWestToggle(Toggle westToggle) {
    this.westToggle = westToggle;
  }

  private void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  private void updatePanel(HasWidgets panel, IdentifiableWidget widget) {
    if (panel == null) {
      notifyWarning("updatePanel: panel is null");
      return;
    }
    if (widget == null) {
      notifyWarning("updatePanel: widget is null");
      return;
    }

    panel.clear();
    panel.add(widget.asWidget());
  }

  private static Flow onlineUsers() {

    final Set<String> sessions = Global.getUsers().getAllSessions();

    flowUserContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "HeaderIcon-Container");
    flowUserContainer.add(flowOnlineUserSize);
    userLabel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsers");
    flowOnlineUserSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUserSize-None");

    userLabel.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent ev) {
        final HtmlTable table = new HtmlTable(BeeConst.CSS_CLASS_PREFIX + "PopupUsersContent");
        int r = 0;

        Users users = Global.getUsers();

        if (sessions.size() > 0) {
          for (String session : sessions) {
            UserData user = users.getUserData(users.getUserIdBySession(session));
            Image img = new Image(PHOTO_URL + user.getPhotoFileName());
            img.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsersPhoto");
            table.setWidget(r, 0, img);
            table.setText(r, 1, user.getFirstName() + " " + user.getLastName());
            DomUtils.setDataProperty(table.getRow(r++), CONTAINER, user.getPerson());
          }

          if (r > 0) {
            table.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent arg) {
                Element targetElement = EventUtils.getEventTargetElement(arg);
                TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);
                String index = DomUtils.getDataProperty(rowElement, CONTAINER);
                UiHelper.closeDialog(table);

                RowEditor.open(ClassifierConstants.VIEW_PERSONS, Long.valueOf(index),
                    Opener.NEW_TAB);
              }
            });
            Popup popup = new Popup(OutsideClick.CLOSE);
            popup.setStyleName(BeeConst.CSS_CLASS_PREFIX + "PopupUsers");
            popup.setWidget(table);
            popup.setHideOnEscape(true);
            popup.showRelativeTo(userLabel.getElement());
          }
        }
      }
    });

    flowUserContainer.add(userLabel);
    return flowUserContainer;
  }

  public static void updateOnlineUsers() {

    FaLabel label = BeeKeeper.getScreen().getOnlineUserLabel();
    Flow userSize = BeeKeeper.getScreen().getOnlineUserSize();

    if (label == null || userSize == null) {
      return;
    }

    int size = Global.getUsers().getAllSessions().size();

    if (size > 0) {
      userSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUserSize");
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsers" + "-Selected");
      userSize.getElement().setInnerText(String.valueOf(size));

    } else {
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsers");
      userSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUserSize-None");
    }
  }

  public static Widget onlineEmail() {

    final MenuCommand command = new MenuCommand(MenuService.OPEN_MAIL, null);

    flowEmailContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "EmailIcon-Container");
    flowEmailContainer.add(flowOnlineEmailSize);

    flowOnlineEmailSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmailSize-None");
    emailLabel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmail");

    emailLabel.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {

        if (command.getService().equals(MenuService.OPEN_MAIL)) {
          command.execute();
        }
      }
    });

    flowEmailContainer.add(emailLabel);
    return flowEmailContainer;
  }

  public static void updateOnlineEmails(int size) {

    FaLabel label = BeeKeeper.getScreen().getOnlineEmailLabel();
    Flow emailSize = BeeKeeper.getScreen().getOnlineEmailSize();

    if (label == null || emailSize == null) {
      return;
    }

    if (size > 0) {
      emailSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmailSize");
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmail" + "-Selected");
      emailSize.getElement().setInnerText(String.valueOf(size));

    } else {
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmail");
      emailSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmailSize-None");
    }
  }

  public static void updateNewsSize(int size) {

    Flow newsSize = BeeKeeper.getScreen().getNewsSize();

    if (newsSize == null) {
      return;
    }

    if (size > 0) {
      newsSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NewsSize");
      newsSize.getElement().setInnerText(String.valueOf(size));

    } else {
      newsSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NewsSize-None");
    }
  }

  @Override
  public FaLabel getOnlineUserLabel() {
    return userLabel;
  }

  @Override
  public Flow getOnlineUserSize() {
    return flowOnlineUserSize;
  }

  @Override
  public FaLabel getOnlineEmailLabel() {
    return emailLabel;
  }

  @Override
  public Flow getOnlineEmailSize() {
    return flowOnlineEmailSize;
  }

  @Override
  public Flow getNewsSize() {
    return flowNewsSize;
  }

  public Widget createUserPanel(final HtmlTable table) {

    Horizontal userContainer = new Horizontal();
    Flow userPanel = new Flow();
    Image image;
    Label signature = new Label();
    Flow settingsCnt = new Flow();
    FaLabel sett = new FaLabel(FontAwesome.GEAR);
    FaLabel help = new FaLabel(FontAwesome.QUESTION);

    userContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserContainer");
    userPanel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserPanel");
    settingsCnt.addStyleName(BeeConst.CSS_CLASS_PREFIX + "SettingsContainer");
    signature.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserSignature");
    sett.addStyleName(BeeConst.CSS_CLASS_PREFIX + "SettingsIcon");
    help.addStyleName(BeeConst.CSS_CLASS_PREFIX + "HelpIcon");

    sett.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        onUserSignatureClick();
        UiHelper.closeDialog(table);
      }
    });

    help.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg1) {
        BrowsingContext.open(UiConstants.helpURL());
      }
    });

    settingsCnt.add(sett);
    settingsCnt.add(help);

    signature.setText(BeeUtils.trim(BeeKeeper.getUser().getUserSign()));

    Flow exitContainer = new Flow();
    exitContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserExitContainer");

    Label exit = new Label(Localized.getConstants().signOut());
    exit.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserExit");
    exit.setTitle(Localized.getConstants().signOut());

    exit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.getMsgBoxen().confirm(Localized.getMessages().endSession(Settings.getAppName()),
            Icon.QUESTION, Lists.newArrayList(Localized.getConstants().questionLogout()),
            Localized.getConstants().yes(), Localized.getConstants().no(),
            new com.butent.bee.client.dialog.ConfirmationCallback() {

              @Override
              public void onConfirm() {
                Bee.exit();

              }
            }, null, StyleUtils.className(FontSize.MEDIUM), null, null);
      }
    });

    exitContainer.add(signature);
    exitContainer.add(exit);

    String photoFileName = BeeKeeper.getUser().getUserData().getPhotoFileName();
    if (!BeeUtils.isEmpty(photoFileName)) {
      image = new Image(PhotoRenderer.getUrl(photoFileName));
    } else {
      image = new Image(DEFAULT_PHOTO_IMAGE);
    }

    image.setAlt(BeeKeeper.getUser().getLogin());
    image.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserPhoto");

    userPanel.add(settingsCnt);
    userContainer.add(exitContainer);
    userContainer.add(image);
    userPanel.add(userContainer);

    return userPanel;
  }

  public Widget createCalendarPanel() {

    Flow userCal = new Flow();
    userCal.setStyleName(BeeConst.CSS_CLASS_PREFIX + "CalendarContainer");

    JustDate firstDay = TimeUtils.today();
    JustDate day;
    int n = 5;

    for (int i = 0; i < n; i++) {

      if (i == 0) {
        Label lblD = new Label();
        Label lblWd = new Label();
        Flow cal = new Flow();

        lblD.setStyleName(BeeConst.CSS_CLASS_PREFIX + "MonthDay");
        lblD.addStyleName(BeeConst.CSS_CLASS_PREFIX + "MonthDayToday");
        lblD.setText(String.valueOf(firstDay.getDom()));

        lblWd.setStyleName(BeeConst.CSS_CLASS_PREFIX + "WeekDay");
        lblWd.setText(String.valueOf(Format.renderDayOfWeekShort(firstDay.getDow())));

        cal.add(lblWd);
        cal.add(lblD);
        userCal.add(cal);

      } else {
        day = TimeUtils.toDateOrNull(firstDay.getDays() + i);

        if (!TimeUtils.isWeekend(day)) {
          Label lblD = new Label();
          Label lblWd = new Label();
          Flow cal = new Flow();

          lblD.addStyleName(BeeConst.CSS_CLASS_PREFIX + "MonthDay");
          lblD.setText(String.valueOf(day.getDom()));

          lblWd.setStyleName(BeeConst.CSS_CLASS_PREFIX + "WeekDay");
          lblWd.setText(String.valueOf(Format.renderDayOfWeekShort(day.getDow())));

          cal.add(lblWd);
          cal.add(lblD);
          userCal.add(cal);
        } else {
          n++;
        }
      }
    }
    return userCal;
  }
}
