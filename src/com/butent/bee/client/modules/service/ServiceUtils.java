package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.ALS_COMPANY_TYPE_NAME;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.RpcInfo;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ServiceUtils {

  private static DataInfo maintenanceDataInfo = Data.getDataInfo(TBL_SERVICE_MAINTENANCE);
  private static DataInfo objectDataInfo = Data.getDataInfo(VIEW_SERVICE_OBJECTS);

  public static void fillContactValues(IsRow maintenanceRow, IsRow objectRow) {
    if (objectRow != null) {
      Long contactPerson = objectRow.getLong(objectDataInfo.getColumnIndex(ALS_CONTACT_PERSON));

      if (DataUtils.isId(contactPerson)) {
        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(COL_CONTACT), contactPerson);

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_PHONE),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_PHONE)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_FIRST_NAME),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_FIRST_NAME)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_LAST_NAME),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_LAST_NAME)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_EMAIL),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_EMAIL)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_ADDRESS),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_ADDRESS)));
      }
    }
  }

  public static double calculateServicePrice(double price, IsRow serviceMaintenanceRow) {
    return calculateServicePrice(price, null, serviceMaintenanceRow);
  }

  public static double calculateServicePrice(BeeRow commentRow, IsRow serviceMaintenanceRow) {
    return calculateServicePrice(0, commentRow, serviceMaintenanceRow);
  }

  public static DateTime calculateWarrantyDate(SelectorEvent event) {
    DateTime warrantyDate = null;
    BeeRow warrantyRow = event.getRelatedRow();

    if (warrantyRow != null) {
      int duration = BeeUtils.unbox(warrantyRow.getInteger(
          Data.getColumnIndex(event.getRelatedViewName(), COL_WARRANTY_DURATION)));
      if (BeeUtils.isPositive(duration)) {
        DateTime warrantyValidToValue = new DateTime();
        TimeUtils.add(warrantyValidToValue, TimeUtils.FIELD_DATE, duration);
        warrantyDate = warrantyValidToValue;
      }
    }
    return warrantyDate;
  }

  public static void checkCanChangeState(Boolean isFinalState, Boolean isItemsRequired,
      Consumer<String> changeStateConsumer, FormView formView) {
    if (!BeeUtils.unbox(isFinalState)) {
      changeStateConsumer.accept(null);

    } else {
      Filter filter = Filter.equals(COL_SERVICE_MAINTENANCE, formView.getActiveRowId());
      Queries.getRowSet(TBL_SERVICE_ITEMS, null, filter, result -> {
        int qtyIdx = Data.getColumnIndex(TBL_SERVICE_ITEMS, TradeConstants.COL_TRADE_ITEM_QUANTITY);
        int doneIdx = Data.getColumnIndex(TBL_SERVICE_ITEMS, RpcInfo.COL_COMPLETED);
        String changeStateErrorMsg = null;

        if (isItemsRequired && (result == null || result.isEmpty())) {
          changeStateConsumer.accept(Localized.dictionary().shoppingCartIsEmpty());
          return;
        }

        if (result != null) {
          for (IsRow row : result) {
            Double completed = row.getDouble(doneIdx);
            Double qty = row.getDouble(qtyIdx);

            if (BeeUtils.unbox(completed) <= 0 || !Objects.equals(completed, qty)) {
              changeStateErrorMsg = Localized.dictionary().ordEmptyInvoice();
              break;
            }
          }
        }
        if (!BeeUtils.isEmpty(changeStateErrorMsg)) {
          Global.confirm(changeStateErrorMsg, new ConfirmationCallback() {
            @Override
            public void onCancel() {
              changeStateConsumer.accept("Klaida");
            }

            @Override
            public void onConfirm() {
              changeStateConsumer.accept(null);
            }
          });
        } else {
          changeStateConsumer.accept(null);
        }
      });
    }
  }

  public static void clearContactValue(IsRow maintenanceRow) {
    setClientValuesForRevert(maintenanceRow);
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(COL_CONTACT));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_PHONE));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_FIRST_NAME));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_LAST_NAME));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_EMAIL));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_ADDRESS));
  }

  public static void fillCompanyColumns(IsRow maintenanceRow, IsRow dataRow, String dataViewName,
      String companyColumnAlias, String companyTypeColumnAlias) {
    maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_COMPANY_NAME),
        dataRow.getString(Data.getColumnIndex(dataViewName, companyColumnAlias)));
    maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_COMPANY_TYPE_NAME),
        dataRow.getString(Data.getColumnIndex(dataViewName, companyTypeColumnAlias)));
  }

  public static void fillCompanyValues(IsRow maintenanceRow, IsRow dataRow, String dataViewName,
      String companyColumnName, String companyColumnAlias, String companyTypeColumnAlias) {
    if (dataRow != null) {
      Long company = dataRow.getLong(Data.getColumnIndex(dataViewName, companyColumnName));
      if (DataUtils.isId(company)) {
        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(COL_COMPANY), company);
        fillCompanyColumns(maintenanceRow, dataRow, dataViewName, companyColumnAlias,
            companyTypeColumnAlias);
      }
      setClientValuesForRevert(maintenanceRow);
    }
  }

  public static void fillContractorAndManufacturerValues(IsRow maintenanceRow, IsRow objectRow) {
    maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_MANUFACTURER_NAME),
        objectRow.getString(objectDataInfo.getColumnIndex(ALS_MANUFACTURER_NAME)));

    maintenanceRow.setValue(
        maintenanceDataInfo.getColumnIndex(ALS_SERVICE_CONTRACTOR_NAME),
        objectRow.getString(objectDataInfo.getColumnIndex(ALS_SERVICE_CONTRACTOR_NAME)));
  }

  public static Filter getStateFilter(Long stateId, Long maintenanceTypeId) {
    return getStateFilter(stateId, maintenanceTypeId, null);
  }

  public static Filter getStateFilter(Long stateId, Long typeId, Filter stateFilter) {
    Filter roleFilter = Filter.in(AdministrationConstants.COL_ROLE,
        AdministrationConstants.VIEW_USER_ROLES, AdministrationConstants.COL_ROLE,
        Filter.equals(AdministrationConstants.COL_USER, BeeKeeper.getUser().getUserId()));

    return Filter.and(
        DataUtils.isId(stateId) ? Filter.equals(COL_MAINTENANCE_STATE, stateId) : null,
        DataUtils.isId(typeId) ? Filter.equals(COL_MAINTENANCE_TYPE, typeId) : null,
        roleFilter, stateFilter);
  }

  public static void getStateProcessRowSet(IsRow maintenanceRow,
      Consumer<BeeRowSet> processConsumer) {
    Long stateId = maintenanceRow.getLong(maintenanceDataInfo
        .getColumnIndex(AdministrationConstants.COL_STATE));
    Long maintenanceTypeId = maintenanceRow.getLong(maintenanceDataInfo.getColumnIndex(COL_TYPE));

    Queries.getRowSet(TBL_STATE_PROCESS, null,
        ServiceUtils.getStateFilter(stateId, maintenanceTypeId), processConsumer::accept);
  }

  private ServiceUtils() {
  }

  public static void informClient(BeeRow commentRow, Long contactPerson, Long repairerPerson) {
    ParameterList params = ServiceKeeper.createArgs(SVC_INFORM_CUSTOMER);

    if (Data.isTrue(TBL_MAINTENANCE_COMMENTS, commentRow, COL_CUSTOMER_SENT)) {
      if (DataUtils.isId(contactPerson)) {
        params.addDataItem(COL_CONTACT, contactPerson);
      } else {
        Global.showError("Nenurodytas kontaktinis asmuo");
      }
    }
    if (Data.isTrue(TBL_MAINTENANCE_COMMENTS, commentRow, COL_REPAIRER_SENT)) {
      if (DataUtils.isId(repairerPerson)) {
        params.addDataItem(COL_REPAIRER, repairerPerson);
      } else {
        Global.showError("Nenurodytas meistras");
      }
    }
    if (!params.isEmpty()) {
      params.addDataItem(COL_COMMENT, commentRow.getId());

      BeeKeeper.getRpc().makePostRequest(params, response -> {
        if (response.hasMessages()) {
          Global.showInfo(response.getMessages().stream()
              .map(ResponseMessage::getMessage).collect(Collectors.toList()));
        }
        if (!response.isEmpty()) {
          Codec.deserializeHashMap(response.getResponseAsString())
              .forEach((k, v) -> Data.setValue(TBL_MAINTENANCE_COMMENTS, commentRow, k, v));

          RowUpdateEvent.fire(BeeKeeper.getBus(), TBL_MAINTENANCE_COMMENTS, commentRow);
        }
      });
    }
  }

  public static void setClientValuesForRevert(IsRow maintenanceRow) {
    Stream.of(COL_COMPANY, ALS_COMPANY_NAME, ALS_COMPANY_TYPE_NAME).forEach(column ->
        maintenanceRow.setProperty(column,
            maintenanceRow.getString(maintenanceDataInfo.getColumnIndex(column)))
    );
  }

  public static void setObjectValuesForRevert(IsRow maintenanceRow) {
    Stream.of(COL_SERVICE_OBJECT, COL_MANUFACTURER, ALS_MANUFACTURER_NAME, COL_SERVICE_CONTRACTOR,
        ALS_SERVICE_CONTRACTOR_NAME, COL_CATEGORY, ALS_SERVICE_CATEGORY_NAME, COL_MODEL,
        COL_SERIAL_NO, COL_ARTICLE_NO, COL_SERVICE_CUSTOMER).forEach(column ->
        maintenanceRow.setProperty(column,
            maintenanceRow.getString(maintenanceDataInfo.getColumnIndex(column)))
    );
  }

  public static void onClientOrObjectUpdate(SelectorEvent event, FormView formView,
      Consumer<String> reasonValue) {
    Dictionary loc = Localized.dictionary();
    String viewName = event.getRelatedViewName();
    String repairCompanyId = BeeUtils.same(viewName, VIEW_COMPANIES)
        ? BeeUtils.toString(event.getValue())
        : formView.getActiveRow().getString(maintenanceDataInfo.getColumnIndex(COL_COMPANY));
    String objectCompanyId = BeeUtils.same(viewName, VIEW_SERVICE_OBJECTS)
        ? event.getRelatedRow().getString(objectDataInfo.getColumnIndex(COL_SERVICE_CUSTOMER))
        : formView.getActiveRow().getString(maintenanceDataInfo
        .getColumnIndex(COL_SERVICE_CUSTOMER));
    Number clientChangingPrm = Global.getParameterNumber(PRM_CLIENT_CHANGING_SETTING);

    if (repairCompanyId != null && objectCompanyId != null && clientChangingPrm != null
        && !BeeUtils.same(repairCompanyId, objectCompanyId)) {
      boolean requiredReason = Objects.equals(clientChangingPrm.intValue(), 2);

      switch (clientChangingPrm.intValue()) {
        case 1:
        case 2:
          Global.inputString(loc.svcChangingClient(), loc.trAssessmentReason(),
              new StringCallback() {
                @Override
                public void onSuccess(String value) {
                  updateClientAndContact(event, formView, false);
                  reasonValue.accept(value);
                }

                @Override
                public void onCancel() {
                  revertClientOrObject(event, formView);
                  super.onCancel();
                }

                @Override
                public boolean isRequired() {
                  return requiredReason;
                }
              }, null);
          break;

        case 3:
          Global.confirm(loc.svcChangingClient(), new ConfirmationCallback() {
            @Override
            public void onConfirm() {
              updateClientAndContact(event, formView, false);
            }

            @Override
            public void onCancel() {
              revertClientOrObject(event, formView);
            }
          });
          break;

        case 4:
          updateClientAndContact(event, formView, true);
          formView.notifyInfo(Localized.dictionary().svcChangedClient());
          break;

        case 6:
          updateClientAndContact(event, formView, false);
          break;

        case 5:
        default:
          updateClientAndContact(event, formView, !BeeUtils.same(repairCompanyId, objectCompanyId));
      }
    } else {
      updateClientAndContact(event, formView, !BeeUtils.same(repairCompanyId, objectCompanyId));
    }
  }

  private static double calculateServicePrice(double price, BeeRow commentRow,
      IsRow serviceMaintenanceRow) {
    Number urgentRate = Global.getParameterNumber(PRM_URGENT_RATE);
    int urgentIndex = Data.getColumnIndex(TBL_SERVICE_MAINTENANCE, COL_MAINTENANCE_URGENT);
    boolean isUrgentMaintenance = BeeUtils.unbox(serviceMaintenanceRow.getBoolean(urgentIndex));
    double calculatedPrice = price;

    if (commentRow != null) {
      calculatedPrice = BeeUtils.unbox(commentRow.getDouble(
          Data.getColumnIndex(TBL_MAINTENANCE_COMMENTS, COL_ITEM_PRICE)));
      isUrgentMaintenance = isUrgentMaintenance
          || BeeUtils.toBoolean(commentRow.getProperty(COL_MAINTENANCE_URGENT));
    }

    if (urgentRate != null && isUrgentMaintenance) {
      return calculatedPrice * urgentRate.doubleValue();
    }

    return calculatedPrice;
  }

  private static void revertClientOrObject(SelectorEvent event, FormView formView) {
    event.consume();
    if (event.hasRelatedView(VIEW_SERVICE_OBJECTS)) {
      Stream.of(COL_SERVICE_OBJECT, COL_MANUFACTURER, ALS_MANUFACTURER_NAME, COL_SERVICE_CONTRACTOR,
          ALS_SERVICE_CONTRACTOR_NAME, COL_CATEGORY, ALS_SERVICE_CATEGORY_NAME, COL_MODEL,
          COL_SERIAL_NO, COL_ARTICLE_NO, COL_SERVICE_CUSTOMER).forEach(column -> {
        if (formView.getActiveRow().hasPropertyValue(column)) {
          formView.getActiveRow().setValue(maintenanceDataInfo.getColumnIndex(column),
              formView.getActiveRow().getProperty(column));
        }
      });

      formView.refreshBySource(COL_SERVICE_OBJECT);
      formView.refreshBySource(ALS_MANUFACTURER_NAME);
      formView.refreshBySource(ALS_SERVICE_CONTRACTOR_NAME);
      formView.refreshBySource(ALS_SERVICE_CATEGORY_NAME);
      formView.refreshBySource(COL_MODEL);
      formView.refreshBySource(COL_SERIAL_NO);
      formView.refreshBySource(COL_ARTICLE_NO);

    } else if (event.hasRelatedView(VIEW_COMPANIES)) {
      Stream.of(COL_COMPANY, ALS_COMPANY_NAME, ALS_COMPANY_TYPE_NAME).forEach(column -> {
        if (formView.getActiveRow().hasPropertyValue(column)) {
          formView.getActiveRow().setValue(maintenanceDataInfo.getColumnIndex(column),
              formView.getActiveRow().getProperty(column));
        }
      });
      formView.refreshBySource(COL_COMPANY);
    }
  }

  private static void updateClientAndContact(SelectorEvent event, FormView formView,
      boolean fillClientFormObject) {
    if (event.hasRelatedView(VIEW_SERVICE_OBJECTS)) {

      if (fillClientFormObject) {
        ServiceUtils.fillCompanyValues(formView.getActiveRow(), event.getRelatedRow(),
            event.getRelatedViewName(), COL_SERVICE_CUSTOMER, ALS_SERVICE_CUSTOMER_NAME,
            ALS_CUSTOMER_TYPE_NAME);
        formView.refreshBySource(COL_COMPANY);
      }

      Number clientChangingPrm = Global.getParameterNumber(PRM_CLIENT_CHANGING_SETTING);
      Long maintenanceContactId = formView.getActiveRow()
          .getLong(maintenanceDataInfo.getColumnIndex(COL_CONTACT));

      if (clientChangingPrm == null
          || (BeeUtils.inList(clientChangingPrm.intValue(), 1, 2, 3, 6)
          && !DataUtils.isId(maintenanceContactId))
          || BeeUtils.inList(clientChangingPrm.intValue(), 4, 5)) {
        fillContactValues(formView.getActiveRow(), event.getRelatedRow());
        formView.refreshBySource(COL_CONTACT);
        formView.refreshBySource(ALS_CONTACT_PHONE);
        formView.refreshBySource(ALS_CONTACT_EMAIL);
        formView.refreshBySource(ALS_CONTACT_ADDRESS);
      }

      fillContractorAndManufacturerValues(formView.getActiveRow(),
          event.getRelatedRow());
      formView.refreshBySource(ALS_MANUFACTURER_NAME);
      formView.refreshBySource(ALS_SERVICE_CONTRACTOR_NAME);

      setObjectValuesForRevert(formView.getActiveRow());

    } else if (event.hasRelatedView(VIEW_COMPANIES)) {
      clearContactValue(formView.getActiveRow());
      formView.refreshBySource(COL_CONTACT);
      formView.refreshBySource(ALS_CONTACT_PHONE);
      formView.refreshBySource(ALS_CONTACT_EMAIL);
      formView.refreshBySource(ALS_CONTACT_ADDRESS);
    }
  }
}