package com.butent.bee.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.Global;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.SelectionCountChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.navigation.PagerView;
import com.butent.bee.client.view.navigation.SimplePager;
import com.butent.bee.client.view.search.SearchBox;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

/**
 * Implements styling and user command capture for data footers.
 */

public class FooterImpl extends Flow implements FooterView, HasNavigation, HasSearch {

  private static final String STYLE_CONTAINER = "bee-FooterContainer";
  
  private static final String STYLE_PAGER = "bee-SimplePager";
  private static final String STYLE_SEARCH = "bee-FooterSearch";
  private static final String STYLE_REMOVE_FILTER = "bee-RemoveFilter";
  private static final String STYLE_SELECTION_COUNTER = "bee-SelectionCounter";

  private static final int HEIGHT = 32;

  private Presenter viewPresenter = null;

  private String pagerId = null;
  private String searchId = null;
  private String removeFilterId = null;
  private String selectionCounterId = null;

  private boolean enabled = true;

  public FooterImpl() {
    super();
    addStyleName(STYLE_CONTAINER);
  }

  @Override
  public void create(int rowCount, boolean addPaging, boolean showPageSize, boolean addSearch) {
    if (addPaging) {
      SimplePager pager = new SimplePager(rowCount, showPageSize);
      pager.addStyleName(STYLE_PAGER);
      add(pager);
      setPagerId(pager.getWidgetId());
    }

    if (addSearch) {
      SearchBox search = new SearchBox();
      search.addStyleName(STYLE_SEARCH);
      add(search);
      setSearchId(search.getWidgetId());
      
      BeeImage removeFilter = new BeeImage(Global.getImages().silverFilterRemove());
      removeFilter.addStyleName(STYLE_REMOVE_FILTER);
      removeFilter.setTitle(Action.REMOVE_FILTER.getCaption());
      
      removeFilter.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (getViewPresenter() != null) {
            getViewPresenter().handleAction(Action.REMOVE_FILTER);
          }
        }
      });
      
      add(removeFilter);
      setRemoveFilterId(removeFilter.getId());
      
      StyleUtils.hideDisplay(removeFilter);
    }

    BeeLabel selectionCounter = new BeeLabel();
    selectionCounter.addStyleName(STYLE_SELECTION_COUNTER);
    add(selectionCounter);
    setSelectionCounterId(selectionCounter.getId());
  }

  @Override
  public int getHeight() {
    return HEIGHT;
  }

  @Override
  public Collection<PagerView> getPagers() {
    return ViewHelper.getPagers(this);
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public Collection<SearchView> getSearchers() {
    return ViewHelper.getSearchers(this);
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (!BeeUtils.isEmpty(getPagerId())) {
      for (PagerView pager : getPagers()) {
        if (pager.asWidget().getElement().isOrHasChild(source)) {
          return pager.onPrint(source, target);
        }
      }
    }
    
    if (!BeeUtils.isEmpty(getSearchId()) && getSearchId().equals(source.getId())) {
      return !BeeUtils.isEmpty(DomUtils.getValue(source));
    }
    return true;
  }

  @Override
  public void onSelectionCountChange(SelectionCountChangeEvent event) {
    Assert.notNull(event);
    if (!BeeUtils.isEmpty(getSelectionCounterId())) {
      int cnt = event.getCount();
      String text = (cnt > 0) ? BeeUtils.toString(cnt) : BeeConst.STRING_EMPTY;
      DomUtils.setText(getSelectionCounterId(), text);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled == isEnabled()) {
      return;
    }
    this.enabled = enabled;
    DomUtils.enableChildren(this, enabled);
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  @Override
  public void showFilterDelete(boolean show) {
    if (show) {
      StyleUtils.unhideDisplay(getRemoveFilterId());
    } else {
      StyleUtils.hideDisplay(getRemoveFilterId());
    }
  }

  private String getPagerId() {
    return pagerId;
  }

  private String getRemoveFilterId() {
    return removeFilterId;
  }

  private String getSearchId() {
    return searchId;
  }

  private String getSelectionCounterId() {
    return selectionCounterId;
  }

  private void setPagerId(String pagerId) {
    this.pagerId = pagerId;
  }

  private void setRemoveFilterId(String removeFilterId) {
    this.removeFilterId = removeFilterId;
  }

  private void setSearchId(String searchId) {
    this.searchId = searchId;
  }

  private void setSelectionCounterId(String selectionCounterId) {
    this.selectionCounterId = selectionCounterId;
  }
}
