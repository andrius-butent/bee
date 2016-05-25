package com.butent.bee.client.modules.mail;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.utils.BeeUtils;

public class RecipientsGroupForm extends AbstractFormInterceptor {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, VIEW_NEWS_COMPANIES) && widget instanceof ChildGrid) {
      ((ChildGrid) widget).setGridInterceptor(new RecipientsGroupsGrid(VIEW_SELECT_COMPANIES));
    } else if (BeeUtils.same(name, VIEW_NEWS_PERSONS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget)
          .setGridInterceptor(new RecipientsGroupsGrid(VIEW_SELECT_PERSONS));
    } else if (BeeUtils.same(name, VIEW_NEWS_COMPANY_PERSONS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget)
          .setGridInterceptor(new RecipientsGroupsGrid(VIEW_SELECT_COMPANY_PERSONS));
    } else if (BeeUtils.same(name, VIEW_NEWS_COMPANY_CONTACTS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget)
          .setGridInterceptor(new RecipientsGroupsGrid(VIEW_SELECT_COMPANY_CONTACTS));
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new RecipientsGroupForm();
  }
}
