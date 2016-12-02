package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.*;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCheckedness;

import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.ALS_COMPANY_TYPE_NAME;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ServiceMaintenanceForm extends MaintenanceStateChangeInterceptor
    implements SelectorEvent.Handler {

  private static final BeeLogger logger = LogUtils.getLogger(ServiceMaintenanceForm.class);

  private static final String FA_LABEL_SUFFIX = "Add";
  private static final String WIDGET_ADDRESS_NAME = "AddressLabel";
  private static final String WIDGET_MAINTENANCE_COMMENTS = "MaintenanceComments";
  private static final String WIDGET_WARRANTY_TYPE_NAME = "WarrantyTypeName";
  private static final int NEW_MAINTENANCE_DATA_LOAD_PROCESS_COUNT = 8;

  private static final String STYLE_PROGRESS_CONTAINER =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressContainer";
  private static final String STYLE_PROGRESS_BAR =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressBar";


  private final MaintenanceEventsHandler eventsHandler = new MaintenanceEventsHandler();
  private Flow maintenanceComments;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof DataSelector && BeeUtils.inList(name, COL_TYPE, COL_SERVICE_OBJECT,
        COL_WARRANTY_MAINTENANCE, COL_COMPANY, COL_PHONE, COL_CONTACT, COL_EMAIL, COL_ADDRESS)) {
      ((DataSelector) widget).addSelectorHandler(this);

    } else if (widget instanceof Flow && BeeUtils.same(name, WIDGET_MAINTENANCE_COMMENTS)) {
      maintenanceComments = (Flow) widget;
      maintenanceComments.clear();

    } else if (widget instanceof ChildGrid && BeeUtils.same(name, VIEW_SERVICE_DATES)) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        @Override
        public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
          FormView parentForm = ViewHelper.getForm(gridView.getViewPresenter().getMainView());

          if (parentForm != null
                  && BeeUtils.same(parentForm.getViewName(), TBL_SERVICE_MAINTENANCE)) {
            event.getColumns().add(Data.getColumn(getViewName(), COL_SERVICE_OBJECT));
            event.getValues().add(parentForm.getActiveRow()
                .getString(Data.getColumnIndex(parentForm.getViewName(), COL_SERVICE_OBJECT)));
          }

          super.onReadyForInsert(gridView, event);
        }
      });

    } else if (widget instanceof FaLabel && BeeUtils.inList(name,
        COL_SERVICE_OBJECT + FA_LABEL_SUFFIX, COL_CONTACT + FA_LABEL_SUFFIX)) {
      ((FaLabel) widget).addClickHandler(event -> {
        String viewName = BeeUtils.same(name, COL_SERVICE_OBJECT + FA_LABEL_SUFFIX)
            ? TBL_SERVICE_OBJECTS : TBL_COMPANY_PERSONS;
        Widget selectorWidget = null;

        if (BeeUtils.same(viewName, TBL_COMPANY_PERSONS)) {
          selectorWidget = getFormView().getWidgetBySource(COL_CONTACT);

        } else if (BeeUtils.same(viewName, TBL_SERVICE_OBJECTS)) {
          selectorWidget = getFormView().getWidgetBySource(COL_SERVICE_OBJECT);
        }

        if (selectorWidget instanceof DataSelector) {
          RowFactory.createRelatedRow((DataSelector) selectorWidget, null);
        }
      });
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    super.afterInsertRow(result, forced);

    updateServiceObject(result.getId(), null);

    fillDataByStateProcessSettings(result, null);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    drawComments(row);

    updateStateDataSelector(false);

    if (DataUtils.isId(row.getLong(Data.getColumnIndex(getViewName(), COL_WARRANTY_MAINTENANCE)))) {
      updateWarrantyTypeWidget(true);
    }

    if (BeeUtils.isTrue(row.getBoolean(Data.getColumnIndex(getViewName(), COL_ADDRESS_REQUIRED)))) {
      updateContactAddressLabel(true);
    }

    if (!DataUtils.isNewRow(row)) {
      Widget typeWidget = form.getWidgetBySource(COL_TYPE);
      if (typeWidget instanceof DataSelector) {
        ((DataSelector) typeWidget).setEnabled(false);
      }
    }
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    super.afterUpdateRow(result);

    updateServiceObject(result.getId(),
            result.getLong(Data.getColumnIndex(getViewName(), COL_SERVICE_OBJECT)));
  }

  @Override
  public FormInterceptor getInstance() {
    return new ServiceMaintenanceForm();
  }

  public Flow getMaintenanceComments() {
    return maintenanceComments;
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isNewRow()
        && BeeUtils.inList(event.getRelatedViewName(), VIEW_COMPANY_PERSONS, TBL_SERVICE_OBJECTS)) {

      event.setDefValue(null);

      if (BeeUtils.same(event.getRelatedViewName(), VIEW_COMPANY_PERSONS)
          && BeeUtils.isTrue(getActiveRow().getBoolean(Data.getColumnIndex(getViewName(),
            COL_ADDRESS_REQUIRED)))) {
        event.setOnOpenNewRow(formView -> {
          Widget widget = formView.getWidgetBySource(COL_ADDRESS);

          if (widget instanceof InputText) {
            formView.getWidgetByName(COL_ADDRESS).addStyleName(StyleUtils.NAME_REQUIRED);

            formView.addCellValidationHandler(COL_ADDRESS, validationEvent -> {

              if (BeeUtils.isEmpty(validationEvent.getNewValue())) {
                formView.notifySevere(Localized.dictionary()
                    .fieldRequired(Localized.dictionary().address()));
                return false;
              }
              return true;
            });
          }
        });
      }
    } else if (event.isChanged()) {
      switch (event.getRelatedViewName()) {
        case VIEW_MAINTENANCE_TYPES:
          updateStateDataSelector(true);

          Boolean addressRequired = event.getRelatedRow()
              .getBoolean(Data.getColumnIndex(event.getRelatedViewName(), COL_ADDRESS_REQUIRED));
          updateContactAddressLabel(BeeUtils.isTrue(addressRequired));
          break;

        case VIEW_COMPANIES :
          ServiceUtils.clearContactValue(getActiveRow());
          getFormView().refreshBySource(COL_CONTACT);
          break;

        case VIEW_COMPANY_PERSONS:
          ServiceUtils.fillCompanyValues(getActiveRow(), event.getRelatedRow(),
              event.getRelatedViewName(), COL_COMPANY, ALS_COMPANY_NAME, ALS_COMPANY_TYPE_NAME);
          getFormView().refreshBySource(COL_COMPANY);
          break;

        case COL_SERVICE_MAINTENANCE:
          updateWarrantyTypeWidget(DataUtils.isId(event.getValue()));
          break;

        case VIEW_SERVICE_OBJECTS:
          ServiceUtils.fillContactValues(getActiveRow(), event.getRelatedRow());
          getFormView().refreshBySource(COL_CONTACT);

          ServiceUtils.fillContractorAndManufacturerValues(getActiveRow(), event.getRelatedRow());
          getFormView().refreshBySource(ALS_MANUFACTURER_NAME);
          getFormView().refreshBySource(ALS_SERVICE_CONTRACTOR_NAME);

          ServiceUtils.fillCompanyValues(getActiveRow(), event.getRelatedRow(),
              event.getRelatedViewName(), COL_SERVICE_CUSTOMER, ALS_SERVICE_CUSTOMER_NAME,
              ALS_CUSTOMER_TYPE_NAME);
          getFormView().refreshBySource(COL_COMPANY);
          break;
      }
    }
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    event.setConsumed(!isValidData(getFormView(), getActiveRow()));
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    if (isValidData(getFormView(), getActiveRow())) {

      if (event.getColumns().contains(Data.getColumn(getViewName(),
          AdministrationConstants.COL_STATE))) {
        fillDataByStateProcessSettings(event.getNewRow(), event.getOldRow());
      }

    } else {
      event.consume();
    }
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    super.onStartNewRow(form, oldRow, newRow);

    form.addStyleName(STYLE_PROGRESS_CONTAINER);
    form.addStyleName(STYLE_PROGRESS_BAR);

    final Consumer<Pair<String, String>> consumer = new Consumer<Pair<String, String>>() {
      Set<Pair<String, String>> values = new HashSet<>();
      int processCount;

      @Override
      public void accept(Pair<String, String> columnAndValue) {
        processCount++;

        if (columnAndValue != null) {
          values.add(columnAndValue);
        }

        if (processCount == NEW_MAINTENANCE_DATA_LOAD_PROCESS_COUNT) {
          values.forEach(value ->
              newRow.setValue(Data.getColumnIndex(getViewName(), value.getA()), value.getB()));

          form.removeStyleName(STYLE_PROGRESS_CONTAINER);
          form.removeStyleName(STYLE_PROGRESS_BAR);

          getFormView().refresh(false, false);
        }
      }
    };

    Global.getRelationParameter(PRM_DEFAULT_MAINTENANCE_TYPE, (prefixId, prefix) -> {
     if (DataUtils.isId(prefixId)) {
        consumer.accept(Pair.of(COL_TYPE, BeeUtils.toString(prefixId)));
        consumer.accept(Pair.of(ALS_MAINTENANCE_TYPE_NAME, prefix));

        Queries.getValue(TBL_MAINTENANCE_TYPES, prefixId, COL_ADDRESS_REQUIRED,
            new RpcCallback<String>() {
          @Override
          public void onSuccess(String result) {
            consumer.accept(Pair.of(COL_ADDRESS_REQUIRED, result));
          }

          @Override
          public void onFailure(String... reason) {
            consumeProcess(consumer, 1);
          }
        });

      } else {
        consumeProcess(consumer, 2);
      }

      Queries.getRowSet(TBL_STATE_PROCESS, null,
          ServiceUtils.getStateFilter(null, prefixId, Filter.isPositive(COL_INITIAL)),
          new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          if (!DataUtils.isEmpty(result)) {
            IsRow processRow = result.getRow(0);
            consumer.accept(Pair.of(AdministrationConstants.COL_STATE, processRow.
                    getString(Data.getColumnIndex(TBL_STATE_PROCESS, COL_MAINTENANCE_STATE))));

            consumer.accept(Pair.of(ALS_STATE_NAME, processRow.
                getString(Data.getColumnIndex(TBL_STATE_PROCESS, ALS_MAINTENANCE_STATE_NAME))));

          } else {
            consumeProcess(consumer, 2);
          }
        }

        @Override
        public void onFailure(String... reason) {
          consumeProcess(consumer, 2);
        }
      });
    });

    Global.getRelationParameter(PRM_DEFAULT_WARRANTY_TYPE, (prefixId, prefix) -> {
      if (DataUtils.isId(prefixId)) {
        consumer.accept(Pair.of(COL_WARRANTY_TYPE, BeeUtils.toString(prefixId)));
        consumer.accept(Pair.of(ALS_WARRANTY_TYPE_NAME, prefix));
      } else {
        consumeProcess(consumer, 2);
      }
    });

    Filter personFilter = Filter.in(Data.getIdColumn(VIEW_COMPANY_PERSONS),
        AdministrationConstants.TBL_USERS, COL_COMPANY_PERSON,
        Filter.compareId(BeeKeeper.getUser().getUserId()));

    Queries.getRowSet(VIEW_COMPANY_PERSONS, null, personFilter, new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (!DataUtils.isEmpty(result)) {
              IsRow personRow = result.getRow(0);
              consumer.accept(Pair.of(ALS_CREATOR_DEPARTMENT_NAME, personRow.
                  getString(Data.getColumnIndex(VIEW_COMPANY_PERSONS, ALS_DEPARTMENT_NAME))));

            } else {
              consumeProcess(consumer, 1);
            }
          }

          @Override
          public void onFailure(String... reason) {
            consumeProcess(consumer, 1);
          }
        });
  }

  private void consumeProcess(Consumer consumer, int processCount) {
    for (int i = 0; i < processCount; i++) {
      consumer.accept(null);
    }
  }

  private void createStateChangeComment(IsRow row, IsRow stateProcessRow) {
    List<BeeColumn> columns = Data.getColumns(TBL_MAINTENANCE_COMMENTS,
        Lists.newArrayList(COL_SERVICE_MAINTENANCE, COL_EVENT_NOTE, COL_MAINTENANCE_STATE));
    List<String> values = Lists.newArrayList(Long.toString(row.getId()),
        row.getString(Data.getColumnIndex(getViewName(), ALS_STATE_NAME)),
        row.getString(Data.getColumnIndex(getViewName(), AdministrationConstants.COL_STATE)));

    if (stateProcessRow != null) {
      String notifyValue = stateProcessRow.
          getString(Data.getColumnIndex(TBL_STATE_PROCESS, COL_NOTIFY_CUSTOMER));

      if (BeeUtils.toBoolean(notifyValue)) {
        columns.add(Data.getColumn(TBL_MAINTENANCE_COMMENTS, COL_CUSTOMER_SENT));
        values.add(notifyValue);
      }

      String showCustomerValue = stateProcessRow.
          getString(Data.getColumnIndex(TBL_STATE_PROCESS, COL_SHOW_CUSTOMER));

      if (BeeUtils.toBoolean(showCustomerValue)) {
        columns.add(Data.getColumn(TBL_MAINTENANCE_COMMENTS, COL_SHOW_CUSTOMER));
        values.add(showCustomerValue);
      }

      String commentValue = stateProcessRow.getString(Data.getColumnIndex(TBL_STATE_PROCESS,
          COL_MESSAGE));

      if (!BeeUtils.isEmpty(commentValue)) {
        columns.add(Data.getColumn(TBL_MAINTENANCE_COMMENTS, COL_COMMENT));
        values.add(commentValue);
      }
    }

    Queries.insert(TBL_MAINTENANCE_COMMENTS, columns, values);
  }

  private void drawComments(IsRow row) {
    final Flow comments = getMaintenanceComments();

    if (comments == null) {
      logger.warning("Widget of project comments not found");
      return;
    }

    if (eventsHandler == null) {
      logger.warning("Events handler not initialized");
      return;
    }

    comments.clear();

    if (!DataUtils.isId(row.getId())) {
      return;
    }

    eventsHandler.create(comments, row.getId());
    eventsHandler.setMaintenanceRow(row);
  }

  private void fillDataByStateProcessSettings(IsRow row, IsRow oldRow) {
    Long stateId = row
        .getLong(Data.getColumnIndex(getViewName(), AdministrationConstants.COL_STATE));
    Long maintenanceTypeId = row.getLong(Data.getColumnIndex(getViewName(), COL_TYPE));

    Queries.getRowSet(TBL_STATE_PROCESS, null,
        ServiceUtils.getStateFilter(stateId, maintenanceTypeId), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (!DataUtils.isEmpty(result)) {
          IsRow stateProcessRow = result.getRow(0);
          String oldValue;

          if (oldRow != null) {
            oldValue = oldRow.getString(Data.getColumnIndex(getViewName(), COL_ENDING_DATE));
          } else {
            oldValue = null;
          }

          if (BeeUtils.toBoolean(stateProcessRow.getString(result.getColumnIndex(COL_FINITE)))) {
            Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(), COL_ENDING_DATE,
                oldValue, BeeUtils.toString(System.currentTimeMillis()),
                ModificationEvent.Kind.UPDATE_ROW);

          } else if (!BeeUtils.isEmpty(oldValue)) {
            Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(), COL_ENDING_DATE,
                oldValue, null, ModificationEvent.Kind.UPDATE_ROW);
          }

          createStateChangeComment(row, stateProcessRow);

        } else {
          createStateChangeComment(row, null);
        }
      }
    });
  }

  private boolean isValidData(FormView form, IsRow row) {
    String phone = row.getString(Data.getColumnIndex(getViewName(), ALS_CONTACT_PHONE));

    if (BeeUtils.isEmpty(phone)) {
      form.notifySevere(Localized.dictionary().fieldRequired(Localized.dictionary().phone()));
      return false;
    }

    Boolean addressRequired = row.getBoolean(Data.getColumnIndex(getViewName(),
        COL_ADDRESS_REQUIRED));

    if (BeeUtils.isTrue(addressRequired)) {
      String address = row.getString(Data.getColumnIndex(getViewName(), ALS_CONTACT_ADDRESS));

      if (BeeUtils.isEmpty(address)) {
        form.notifySevere(Localized.dictionary().fieldRequired(Localized.dictionary().address()));
        return false;
      }
    }

    String department = row.getString(Data.getColumnIndex(getViewName(),
        ALS_CREATOR_DEPARTMENT_NAME));

    if (BeeUtils.isEmpty(department)) {
      form.notifySevere(Localized.dictionary()
          .fieldRequired(Localized.dictionary().companyDepartment()));
      return false;
    }

    Long warrantyTypeId = row.getLong(Data.getColumnIndex(getViewName(), COL_WARRANTY_TYPE));
    Long serviceMaintenanceId = row.getLong(Data.getColumnIndex(getViewName(),
            COL_WARRANTY_MAINTENANCE));

    if (DataUtils.isId(serviceMaintenanceId) && !DataUtils.isId(warrantyTypeId)) {
      form.notifySevere(Localized.dictionary()
          .fieldRequired(Localized.dictionary().svcWarrantyType()));
      return false;
    }

    return true;
  }

  private void updateContactAddressLabel(boolean addressRequired) {
    Widget addressLabel = getFormView().getWidgetByName(WIDGET_ADDRESS_NAME);

    if (addressLabel != null) {
      addressLabel.setStyleName(StyleUtils.NAME_REQUIRED, addressRequired);
    }
  }

  private static void updateServiceObject(long maintenanceId, Long objectId) {
    ParameterList params = ServiceKeeper.createArgs(SVC_UPDATE_SERVICE_MAINTENANCE_OBJECT);
    params.addDataItem(COL_SERVICE_MAINTENANCE, maintenanceId);

    if (DataUtils.isId(objectId)) {
      params.addDataItem(COL_SERVICE_OBJECT, objectId);
    }

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!response.isEmpty() && !response.hasErrors()) {
          Queries.getRow(VIEW_SERVICE_OBJECTS, response.getResponseAsLong(), new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              if (result != null) {
                RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_SERVICE_OBJECTS, result);
              }
            }
          });
        }
      }
    });
  }

  private void updateStateDataSelector(boolean clearValue) {
    Widget stateWidget = getFormView().getWidgetByName(AdministrationConstants.COL_STATE);

    if (stateWidget instanceof DataSelector) {
      Filter stateFilter = DataUtils.isNewRow(getActiveRow())
          ? Filter.isPositive(COL_INITIAL) : Filter.isNull(COL_INITIAL);
      Long maintenanceTypeId = getActiveRow().getLong(Data.getColumnIndex(getViewName(), COL_TYPE));

      ((DataSelector) stateWidget).setAdditionalFilter(
          Filter.in(Data.getIdColumn(VIEW_MAINTENANCE_STATES), TBL_STATE_PROCESS,
              COL_MAINTENANCE_STATE,
              ServiceUtils.getStateFilter(null, maintenanceTypeId, stateFilter)));

      if (clearValue) {
        ((DataSelector) stateWidget).clearValue();
        getFormView().getActiveRow().clearCell(Data.getColumnIndex(getViewName(),
            AdministrationConstants.COL_STATE));
        getFormView().getActiveRow().clearCell(Data.getColumnIndex(getViewName(), ALS_STATE_NAME));
        getFormView().refreshBySource(AdministrationConstants.COL_STATE);
      }
    }
  }

  private void updateWarrantyTypeWidget(boolean mandatory) {
    Widget warrantyWidget = getFormView().getWidgetByName(COL_WARRANTY, false);

    if (warrantyWidget instanceof HasCheckedness) {
      ((HasCheckedness) warrantyWidget).setChecked(mandatory);
    }

    Widget warrantyTypeName = getFormView().getWidgetByName(WIDGET_WARRANTY_TYPE_NAME, false);

    if (warrantyTypeName != null) {
      if (mandatory) {
        warrantyTypeName.addStyleName(StyleUtils.NAME_REQUIRED);
      } else {
        warrantyTypeName.removeStyleName(StyleUtils.NAME_REQUIRED);
      }
    }
  }
}