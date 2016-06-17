package com.butent.bee.client.modules.orders.ec;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.orders.OrdersConstants.*;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.i18n.Localized;

public abstract class OrdEcView extends Flow {

  public static OrdEcView create(String service) {
    OrdEcView ecView = null;

    if (SVC_EC_SEARCH_BY_ITEM_ARTICLE.equals(service)) {
      ecView = new SearchByArticle(service, Localized.dictionary().ordSearchByItemArticle());
    }
    if (SVC_EC_SEARCH_BY_ITEM_CATEGORY.equals(service)) {
      ecView = new SearchByCategory(service);
    }

    if (ecView != null) {
      ecView.createUi();
    }
    return ecView;
  }

  protected OrdEcView() {
    super(EcStyles.name("View"));
    EcStyles.add(this, getPrimaryStyle());
  }

  protected abstract void createUi();

  protected abstract String getPrimaryStyle();

  @Override
  protected void onLoad() {
    super.onLoad();
    UiHelper.focus(this);
  }

  protected Widget renderNoData(String key) {
    Label label = new Label(Localized.dictionary().dataNotAvailable(key));
    EcStyles.add(label, "noData");
    return label;
  }
}