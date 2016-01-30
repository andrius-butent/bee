package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.TreeGridInterceptor;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClassifierKeeper {

  private static BeeLogger logger = LogUtils.getLogger(ClassifierKeeper.class);

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_COMPANIES)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_COMPANIES), event.getRow(),
            Lists.newArrayList(COL_COMPANY_NAME, COL_COMPANY_CODE, COL_PHONE, COL_EMAIL_ADDRESS,
                COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), BeeConst.STRING_SPACE));

      } else if (event.hasView(VIEW_PERSONS)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_PERSONS), event.getRow(),
            Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME, COL_PHONE, COL_EMAIL_ADDRESS,
                COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), BeeConst.STRING_SPACE));
      }
    }
  }

  static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.CLASSIFIERS, method);
  }

  private static class VehiclesGridHandler extends TreeGridInterceptor {

    @Override
    public VehiclesGridHandler getInstance() {
      return new VehiclesGridHandler();
    }

    @Override
    public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
      IsRow model = getSelectedTreeItem();

      if (model != null) {
        List<BeeColumn> cols = getGridPresenter().getDataColumns();
        newRow.setValue(DataUtils.getColumnIndex("Model", cols), model.getId());
        newRow.setValue(DataUtils.getColumnIndex("ParentModelName", cols),
            getModelValue(model, "ParentName"));
        newRow.setValue(DataUtils.getColumnIndex("ModelName", cols),
            getModelValue(model, "Name"));
      }
      return true;
    }

    @Override
    protected Filter getFilter(Long model) {
      if (model == null) {
        return null;
      } else {
        Value value = new LongValue(model);

        return Filter.or(Filter.isEqual("ParentModel", value),
            Filter.isEqual("Model", value));
      }
    }

    private String getModelValue(IsRow model, String colName) {
      if (BeeUtils.allNotNull(model, getTreeDataColumns())) {
        return model.getString(getTreeColumnIndex(colName));
      }
      return null;
    }
  }

  public static void getHolidays(final Consumer<Set<Integer>> consumer) {
    Global.getParameter(AdministrationConstants.PRM_COUNTRY, new Consumer<String>() {
      @Override
      public void accept(String input) {
        if (DataUtils.isId(input)) {
          Queries.getRowSet(VIEW_HOLIDAYS, Collections.singletonList(COL_HOLY_DAY),
              Filter.equals(COL_HOLY_COUNTRY, BeeUtils.toLong(input)),
              new Queries.RowSetCallback() {
                @Override
                public void onSuccess(BeeRowSet result) {
                  Set<Integer> holidays = new HashSet<>();

                  if (!DataUtils.isEmpty(result)) {
                    int index = result.getColumnIndex(COL_HOLY_DAY);
                    for (BeeRow row : result) {
                      holidays.add(row.getInteger(index));
                    }
                  }

                  consumer.accept(holidays);
                }
              });

        } else {
          consumer.accept(BeeConst.EMPTY_IMMUTABLE_INT_SET);
        }
      }
    });
  }

  public static void getPricesAndDiscounts(Map<String, Long> options,
      Set<Long> items, Map<Long, Double> quantities, Map<Long, ItemPrice> priceNames,
      final Consumer<Map<Long, Pair<Double, Double>>> consumer) {

    Assert.notEmpty(options);
    Assert.notEmpty(items);
    Assert.notNull(consumer);

    final Map<Long, Pair<Double, Double>> result = new HashMap<>();
    final Latch latch = new Latch(items.size());

    for (final Long item : items) {
      ParameterList params = createArgs(SVC_GET_PRICE_AND_DISCOUNT);
      for (Map.Entry<String, Long> entry : options.entrySet()) {
        if (!BeeUtils.isEmpty(entry.getKey()) && entry.getValue() != null) {
          params.addQueryItem(entry.getKey(), entry.getValue());
        }
      }

      params.addQueryItem(COL_DISCOUNT_ITEM, item);
      if (quantities.containsKey(item)) {
        params.addQueryItem(Service.VAR_QTY, BeeUtils.toString(quantities.get(item), 6));
      }
      if (priceNames.containsKey(item)) {
        ItemPrice ip = priceNames.get(item);
        if (ip != null) {
          params.addQueryItem(COL_DISCOUNT_PRICE_NAME, ip.ordinal());
        }
      }

      if (Global.getExplain() > 0) {
        params.addQueryItem(Service.VAR_EXPLAIN, Global.getExplain());
      }

      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasResponse()) {
            Pair<String, String> pair = Pair.restore(response.getResponseAsString());

            Double price = BeeUtils.toDoubleOrNull(pair.getA());
            Double percent = BeeUtils.toDoubleOrNull(pair.getB());

            result.put(item, Pair.of(price, percent));
          }

          latch.decrement();
          if (latch.isOpen()) {
            consumer.accept(result);
          }
        }
      });
    }
  }

  public static void generateQrCode(FormView form, IsRow row) {
    final Image qrCodeImage = new Image();
    ParameterList prm = ClassifierKeeper.createArgs(SVC_GENERATE_QR_CODE);

    int idxMobile = form.getDataIndex(COL_MOBILE);
    int idxEmail = form.getDataIndex(COL_EMAIL);
    int idxAddress = form.getDataIndex(COL_ADDRESS);

    if (form.getViewName().equals(VIEW_COMPANIES)) {
      int idxName = form.getDataIndex(COL_COMPANY_NAME);
      String name = row.getString(idxName);
      prm.addDataItem(QR_TYPE, QR_COMPANY);
      if (!BeeUtils.isEmpty(name)) {
        prm.addDataItem(COL_COMPANY_NAME, name);
      }
    } else if (form.getViewName().equals(VIEW_PERSONS)
        || form.getViewName().equals(VIEW_COMPANY_PERSONS)) {
      int idxFirstName = form.getDataIndex(COL_FIRST_NAME);
      int idxLastName = form.getDataIndex(COL_LAST_NAME);
      String userName = row.getString(idxFirstName);
      prm.addDataItem(QR_TYPE, QR_PERSON);
      if (!BeeUtils.isEmpty(userName)) {
        prm.addDataItem(COL_FIRST_NAME, userName);
      }
      String userLastName = row.getString(idxLastName);
      if (!BeeUtils.isEmpty(userLastName)) {
        prm.addDataItem(COL_LAST_NAME, userLastName);
      }
    } else {
      logger.info("Qr Code cannot be generated");
    }
    String mobile = row.getString(idxMobile);
    if (!BeeUtils.isEmpty(mobile)) {
      prm.addDataItem(COL_MOBILE, mobile);
    }
    String email = row.getString(idxEmail);
    if (!BeeUtils.isEmpty(email)) {
      prm.addDataItem(COL_EMAIL, email);
    }
    String address = row.getString(idxAddress);
    if (!BeeUtils.isEmpty(address)) {
      prm.addDataItem(COL_ADDRESS, address);
    }

    BeeKeeper.getRpc().makePostRequest(prm, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        String qrBase64 = response.getResponseAsString();
        qrCodeImage.setUrl("data:image/png;base64," + qrBase64);
        Global.showModalWidget(Localized.getConstants().qrCode(), qrCodeImage);
      }
    });

  }

  public static void register() {
    GridFactory.registerGridSupplier(ItemsGrid.getSupplierKey(false), GRID_ITEMS,
        new ItemsGrid(false));
    GridFactory.registerGridSupplier(ItemsGrid.getSupplierKey(true), GRID_ITEMS,
        new ItemsGrid(true));

    MenuService.ITEMS.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        String key = ItemsGrid.getSupplierKey(BeeUtils.startsSame(parameters, "s"));
        ViewFactory.createAndShow(key);
      }
    });

    GridFactory.registerGridInterceptor(VIEW_VEHICLES, new VehiclesGridHandler());
    GridFactory.registerGridInterceptor(VIEW_COMPANIES, new CompaniesGrid());

    FormFactory.registerFormInterceptor("Item", new ItemForm());
    FormFactory.registerFormInterceptor(FORM_PERSON, new PersonForm());
    FormFactory.registerFormInterceptor(FORM_COMPANY, new CompanyForm());
    FormFactory.registerFormInterceptor("Holidays", new HolidaysForm());
    FormFactory.registerFormInterceptor(FORM_COMPANY_ACTION, new CompanyActionForm());
    FormFactory.registerFormInterceptor(FORM_NEW_COMPANY, new CompanyForm());
    FormFactory.registerFormInterceptor(FORM_COMPANY_PERSON, new CompanyPersonForm());

    SelectorEvent.register(new ClassifierSelector());

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler());
  }

  private ClassifierKeeper() {
  }
}
