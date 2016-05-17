package com.butent.bee.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.output.Printable;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.shared.ui.Action;

import java.util.Collection;
import java.util.Set;

/**
 * Contains requirements for data header implementing classes.
 */

public interface HeaderView extends View, IndexedPanel, Printable, HasClickHandlers {

  void create(String caption, boolean hasData, boolean readOnly, String viewName,
      Collection<UiOption> options, Set<Action> enabledActions, Set<Action> disabledActions,
      Set<Action> hiddenActions);

  void addCaptionStyle(String style);

  void addCommandItem(IdentifiableWidget widget);

  void clearCommandPanel();

  int getHeight();

  boolean hasAction(Action action);

  boolean hasCommands();

  boolean insertControl(Widget w, int beforeIndex);

  boolean isActionEnabled(Action action);

  boolean isActionOrCommand(Element target);

  void removeCaptionStyle(String style);

  void setCaption(String caption);

  void setCaptionTitle(String title);

  void setHeight(int height);

  void setMessage(String message);

  void setMessageTitle(String title);

  void showAction(Action action, boolean visible);
}
