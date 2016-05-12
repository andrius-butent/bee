package com.butent.bee.client.modules.trade;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.modules.trade.acts.TradeActKeeper;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.grid.interceptor.UniqueChildInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.utils.Codec;

public final class TradeKeeper implements HandlesAllDataEvents {
  public interface FilterCallback {
    Filter getFilter();
  }

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "trade-";

  private static final TradeKeeper INSTANCE = new TradeKeeper();

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.TRADE, method);
  }

  public static IdentifiableWidget createAmountAction(final String viewName,
      final FilterCallback filterCallback,
      final String salesRelColumn, final NotificationListener listener) {

    Assert.notEmpty(viewName);

    FaLabel summary = new FaLabel(FontAwesome.LINE_CHART);
    summary.setTitle(Localized.getConstants().totalOf());

    summary.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        ParameterList args = createArgs(SVC_GET_SALE_AMOUNTS);
        args.addDataItem(VAR_VIEW_NAME, viewName);
        args.addDataItem(Service.VAR_COLUMN, salesRelColumn);
        Filter filter = null;

        if (filterCallback != null) {
          filter = filterCallback.getFilter();
        }

        if (filter != null) {
          args.addDataItem(EcConstants.VAR_FILTER, Codec.beeSerialize(filter));
        }

        BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            response.notify(listener);
          }
        });
      }
    });

    return summary;
  }

  public static IdentifiableWidget createAmountAction(final String viewName,
      final Filter filter, final String salesRelColumn,
      final NotificationListener listener) {

    return createAmountAction(viewName, new FilterCallback() {

      @Override
      public Filter getFilter() {
        return filter;
      }
    }, salesRelColumn, listener);

  }

  public static void register() {
    GridFactory.registerGridInterceptor(VIEW_PURCHASE_ITEMS, new TradeItemsGrid());
    GridFactory.registerGridInterceptor(VIEW_SALE_ITEMS, new TradeItemsGrid());

    GridFactory.registerGridInterceptor(GRID_SERIES_MANAGERS,
        UniqueChildInterceptor.forUsers(Localized.dictionary().managers(),
            COL_SERIES, COL_TRADE_MANAGER));
    GridFactory.registerGridInterceptor(GRID_DEBTS, new DebtsGrid());
    GridFactory.registerGridInterceptor(GRID_DEBT_REPORTS, new DebtReportsGrid());
    GridFactory.registerGridInterceptor(GRID_SALES, new SalesGrid());
    GridFactory.registerGridInterceptor(GRID_ERP_SALES, new ERPSalesGrid());

    GridFactory.registerGridInterceptor(GRID_TRADE_DOCUMENT_FILES,
        new FileGridInterceptor(COL_TRADE_DOCUMENT, COL_FILE, COL_FILE_CAPTION, ALS_FILE_NAME));

    GridFactory.registerGridInterceptor(VIEW_SALE_FILES,
        new FileGridInterceptor(COL_SALE, COL_FILE, COL_FILE_CAPTION, ALS_FILE_NAME));

    GridFactory.registerGridInterceptor(GRID_TRADE_DOCUMENT_ITEMS, new TradeDocumentItemsGrid());

    FormFactory.registerFormInterceptor(FORM_SALES_INVOICE, new SalesInvoiceForm());
    FormFactory.registerFormInterceptor(FORM_TRADE_DOCUMENT, new TradeDocumentForm());

    ColorStyleProvider csp = ColorStyleProvider.createDefault(VIEW_TRADE_OPERATIONS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_OPERATIONS, COL_BACKGROUND, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_OPERATIONS, COL_FOREGROUND, csp);

    csp = ColorStyleProvider.createDefault(VIEW_TRADE_STATUSES);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_STATUSES, COL_BACKGROUND, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_STATUSES, COL_FOREGROUND, csp);

    csp = ColorStyleProvider.createDefault(VIEW_TRADE_TAGS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_TAGS, COL_BACKGROUND, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_TAGS, COL_FOREGROUND, csp);

    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_DOCUMENTS, COL_TRADE_OPERATION,
        ColorStyleProvider.create(VIEW_TRADE_DOCUMENTS,
            ALS_OPERATION_BACKGROUND, ALS_OPERATION_FOREGROUND));
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_DOCUMENTS,
        COL_TRADE_DOCUMENT_STATUS,
        ColorStyleProvider.create(VIEW_TRADE_DOCUMENTS,
            ALS_STATUS_BACKGROUND, ALS_STATUS_FOREGROUND));

    registerDocumentViews();
    BeeKeeper.getBus().registerDataHandler(INSTANCE, false);

    if (ModuleAndSub.of(Module.TRADE, SubModule.ACTS).isEnabled()) {
      TradeActKeeper.register();
    }
  }

  private static String getDocumentGridSupplierKey(long typeId) {
    return BeeUtils.join(BeeConst.STRING_UNDER, GRID_TRADE_DOCUMENTS, typeId);
  }

  private static void onDataEvent(DataEvent event) {
    if (event.hasView(VIEW_TRADE_DOCUMENT_TYPES)
        && BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.TRADE))) {

      BeeKeeper.getMenu().loadMenu(() -> registerDocumentViews());
    }
  }

  private static void openDocumentGrid(final long typeId, final PresenterCallback callback) {
    ParameterList params = createArgs(SVC_GET_DOCUMENT_TYPE_CAPTION_AND_FILTER);
    params.addDataItem(COL_DOCUMENT_TYPE, typeId);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse()) {
          Pair<String, String> pair = Pair.restore(response.getResponseAsString());

          String caption = pair.getA();
          Filter filter = BeeUtils.isEmpty(pair.getB()) ? null : Filter.restore(pair.getB());

          String supplierKey = getDocumentGridSupplierKey(typeId);

          GridFactory.createGrid(GRID_TRADE_DOCUMENTS, supplierKey, new TradeDocumentsGrid(),
              EnumSet.of(UiOption.GRID), GridOptions.forCaptionAndFilter(caption, filter),
              callback);
        }
      }
    });
  }

  private static void registerDocumentViews() {
    Set<Long> typeIds = new HashSet<>();

    List<MenuItem> menuItems = BeeKeeper.getMenu().filter(MenuService.TRADE_DOCUMENTS);
    if (!BeeUtils.isEmpty(menuItems)) {
      for (MenuItem menuItem : menuItems) {
        String id = menuItem.getParameters();
        if (DataUtils.isId(id)) {
          typeIds.add(BeeUtils.toLong(id));
        }
      }
    }

    if (!typeIds.isEmpty()) {
      for (long typeId : typeIds) {
        String key = getDocumentGridSupplierKey(typeId);

        if (!ViewFactory.hasSupplier(key)) {
          ViewFactory.registerSupplier(key,
              callback -> openDocumentGrid(typeId, ViewFactory.getPresenterCallback(callback)));
        }
      }

      if (MenuService.TRADE_DOCUMENTS.getHandler() == null) {
        MenuService.TRADE_DOCUMENTS.setHandler(parameters -> {
          if (DataUtils.isId(parameters)) {
            long typeId = BeeUtils.toLong(parameters);
            ViewFactory.createAndShow(getDocumentGridSupplierKey(typeId));
          }
        });
      }
    }
  }

  private TradeKeeper() {
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    onDataEvent(event);
  }
}
