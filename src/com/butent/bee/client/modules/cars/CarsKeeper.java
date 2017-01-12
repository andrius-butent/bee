package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.administration.StageUtils;
import com.butent.bee.client.modules.classifiers.VehiclesGrid;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.Module;

public final class CarsKeeper {

  public static ParameterList createSvcArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.CARS, method);
  }

  public static void register() {
    FormFactory.registerFormInterceptor(TBL_CONF_PRICELIST, new ConfPricelistForm());
    FormFactory.registerFormInterceptor(FORM_CONF_OPTION, new PhotoHandler());
    FormFactory.registerFormInterceptor(FORM_CAR_ORDER, new CarOrderForm());
    FormFactory.registerFormInterceptor(FORM_CAR, new SpecificationForm());
    FormFactory.registerFormInterceptor(FORM_TEMPLATE, new SpecificationForm());

    FormFactory.registerFormInterceptor(FORM_CAR_SERVICE_ORDER, new CarServiceOrderForm());

    GridFactory.registerGridInterceptor(VIEW_CARS, new VehiclesGrid());

    Dictionary loc = Localized.dictionary();
    StageUtils.registerStageAction(TBL_CAR_ORDERS, STAGE_ACTION_READONLY, loc.rowIsReadOnly());
    StageUtils.registerStageAction(TBL_CAR_ORDERS, STAGE_ACTION_LOST, loc.reason());

    StageUtils.registerStageTrigger(TBL_CAR_ORDERS, STAGE_TRIGGER_NEW, loc.newOrder());
    StageUtils.registerStageTrigger(TBL_CAR_ORDERS, STAGE_TRIGGER_SENT, loc.messageSent());
  }

  private CarsKeeper() {
  }
}
