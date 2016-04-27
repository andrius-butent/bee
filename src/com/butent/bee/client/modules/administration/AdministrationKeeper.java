package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.i18n.DictionaryGrid;
import com.butent.bee.client.imports.ImportsForm;
import com.butent.bee.client.rights.RightsForm;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.grid.interceptor.GridSettingsInterceptor;
import com.butent.bee.client.view.grid.interceptor.UniqueChildInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Preloader;

public final class AdministrationKeeper {

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_USERS)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_USERS), event.getRow(),
            Lists.newArrayList(COL_LOGIN, COL_FIRST_NAME, COL_LAST_NAME, ALS_COMPANY_NAME),
            BeeConst.STRING_SPACE));
      }
    }
  }

  private static Long company;

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.ADMINISTRATION, method);
  }

  public static Long getCompany() {
    return company;
  }

  public static void register() {
    MenuService.UPDATE_EXCHANGE_RATES.setHandler(p -> AdministrationUtils.updateExchangeRates());

    FormFactory.registerFormInterceptor(FORM_USER, new UserForm());
    FormFactory.registerFormInterceptor(FORM_USER_SETTINGS, new UserSettingsForm());
    FormFactory.registerFormInterceptor(FORM_DEPARTMENT, new DepartmentForm());
    FormFactory.registerFormInterceptor(FORM_COMPANY_STRUCTURE, new CompanyStructureForm());
    FormFactory.registerFormInterceptor(FORM_NEW_ROLE, new NewRoleForm());
    FormFactory.registerFormInterceptor(FORM_IMPORTS, new ImportsForm());
    FormFactory.registerFormInterceptor(TBL_CUSTOM_CONFIG, new CustomConfigForm());

    GridFactory.registerGridInterceptor(TBL_CUSTOM_CONFIG, new CustomConfigGrid());

    GridFactory.registerGridInterceptor(NewsConstants.GRID_USER_FEEDS, new UserFeedsInterceptor());

    GridFactory.registerGridSupplier(
        GridFactory.getSupplierKey(NewsConstants.GRID_USER_FEEDS, null),
        NewsConstants.GRID_USER_FEEDS,
        new UserFeedsInterceptor(BeeKeeper.getUser().getUserId()),
        GridOptions.forCurrentUserFilter(NewsConstants.COL_UF_USER));

    GridFactory.registerGridInterceptor(GRID_USER_GROUP_MEMBERS,
        UniqueChildInterceptor.forUsers(Localized.dictionary().userGroupAddMembers(),
            COL_UG_GROUP, COL_UG_USER));
    GridFactory.registerGridInterceptor(GRID_ROLE_USERS,
        UniqueChildInterceptor.forUsers(Localized.dictionary().roleAddUsers(),
            COL_ROLE, COL_USER));

    GridFactory.registerGridInterceptor(GRID_THEME_COLORS,
        new UniqueChildInterceptor(Localized.dictionary().newThemeColors(),
            COL_THEME, COL_COLOR, VIEW_COLORS, Lists.newArrayList(COL_COLOR_NAME),
            Lists.newArrayList(COL_COLOR_NAME, COL_BACKGROUND, COL_FOREGROUND)));

    GridFactory.registerGridInterceptor(GridSettingsInterceptor.GRID_NAME,
        new GridSettingsInterceptor());

    GridFactory.registerGridInterceptor(GRID_DICTIONARY, new DictionaryGrid());

    GridFactory.registerPreloader(GRID_DICTIONARY, new Preloader() {
      @Override
      public void accept(final Runnable command) {
        BeeKeeper.getRpc().makeRequest(Service.PREPARE_DICTIONARY, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            command.run();
          }
        });
      }

      @Override
      public boolean disposable() {
        return false;
      }
    });

    ColorStyleProvider styleProvider = ColorStyleProvider.createDefault(VIEW_COLORS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_COLORS, COL_BACKGROUND, styleProvider);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_COLORS, COL_FOREGROUND, styleProvider);

    ConditionalStyle.registerGridColumnStyleProvider(GRID_THEMES, ALS_DEFAULT_COLOR_NAME,
        ColorStyleProvider.create(VIEW_THEMES, ALS_DEFAULT_BACKGROUND, ALS_DEFAULT_FOREGROUND));

    styleProvider = ColorStyleProvider.createDefault(VIEW_THEME_COLORS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_THEME_COLORS, COL_BACKGROUND,
        styleProvider);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_THEME_COLORS, COL_FOREGROUND,
        styleProvider);

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler());

    RightsForm.register();

    SelectorEvent.register(event -> onDataSelector(event));
  }

  public static void setCompany(Long company) {
    AdministrationKeeper.company = company;
  }

  private static void onDataSelector(SelectorEvent event) {
    if (event.isOpened() && event.hasRelatedView(VIEW_USERS)) {
      DateTimeValue now = new DateTimeValue(TimeUtils.nowMinutes());

      event.getSelector().getOracle().setResponseFilter(Filter.or(
          Filter.and(Filter.isNull(COL_USER_BLOCK_FROM), Filter.isNull(COL_USER_BLOCK_UNTIL)),
          Filter.and(Filter.notNull(COL_USER_BLOCK_FROM),
              Filter.isMore(COL_USER_BLOCK_FROM, now)),
          Filter.and(Filter.notNull(COL_USER_BLOCK_UNTIL),
              Filter.isLess(COL_USER_BLOCK_UNTIL, now))
          ));
    }
  }

  private AdministrationKeeper() {
  }
}
