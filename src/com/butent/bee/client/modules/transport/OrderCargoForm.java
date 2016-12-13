package com.butent.bee.client.modules.transport;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.modules.transport.TransportHandler.Profit;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;

class OrderCargoForm extends AbstractFormInterceptor implements SelectorEvent.Handler {

  private FaLabel copyAction;

  static void preload(final ScheduledCommand command) {
    Global.getParameter(PRM_CARGO_TYPE, new Consumer<String>() {
      @Override
      public void accept(String input) {
        if (DataUtils.isId(input)) {
          Queries.getRow(VIEW_CARGO_TYPES, BeeUtils.toLong(input), new RowCallback() {
            @Override
            public void onFailure(String... reason) {
              super.onFailure(reason);
              defaultCargoType = null;
              command.execute();
            }

            @Override
            public void onSuccess(BeeRow result) {
              defaultCargoType = result;
              command.execute();
            }
          });

        } else {
          defaultCargoType = null;
          command.execute();
        }
      }
    });
  }

  private static IsRow defaultCargoType;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, COL_CURRENCY) && widget instanceof DataSelector) {
      final DataSelector selector = (DataSelector) widget;

      selector.addEditStopHandler(event -> {
        if (event.isChanged()) {
          refresh(BeeUtils.toLongOrNull(selector.getNormalizedValue()));
        }
      });
    } else if (widget instanceof ChildGrid) {
      switch (name) {
        case TBL_CARGO_INCOMES:
          ((ChildGrid) widget).addReadyHandler(re -> {
            GridView gridView = ViewHelper.getChildGrid(getFormView(), name);

            if (gridView != null) {
              gridView.getGrid().addMutationHandler(mu -> {
                refresh(getLongValue(COL_CURRENCY));
                Data.onTableChange(TBL_CARGO_TRIPS, EnumSet.of(DataChangeEvent.Effect.REFRESH));
              });
            }
          });
          break;

        case TBL_CARGO_EXPENSES:
          ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
            @Override
            public void afterCreateEditor(String source, Editor editor, boolean embedded) {
              if (BeeUtils.same(source, COL_CARGO_INCOME) && editor instanceof DataSelector) {
                ((DataSelector) editor).addSelectorHandler(OrderCargoForm.this);
              }
              super.afterCreateEditor(source, editor, embedded);
            }

            @Override
            public ColumnDescription beforeCreateColumn(GridView gridView,
                ColumnDescription descr) {
              if (!TransportHandler.bindExpensesToIncomes()
                  && Objects.equals(descr.getId(), COL_CARGO_INCOME)) {
                return null;
              }
              return super.beforeCreateColumn(gridView, descr);
            }

            @Override
            public GridInterceptor getInstance() {
              return null;
            }
          });
          break;

        case VIEW_CARGO_TRIPS:
          ((ChildGrid) widget).setGridInterceptor(new CargoTripsGrid());
          break;
      }
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    Widget cmrWidget = form.getWidgetBySource(COL_CARGO_CMR);
    if (cmrWidget instanceof DataSelector) {
      Filter filter;

      if (DataUtils.hasId(row)) {
        filter = Filter.in(Data.getIdColumn(DocumentConstants.VIEW_DOCUMENTS),
            DocumentConstants.VIEW_RELATED_DOCUMENTS, DocumentConstants.COL_DOCUMENT,
            Filter.equals(COL_CARGO, row.getId()));
      } else {
        filter = Filter.isFalse();
      }

      ((DataSelector) cmrWidget).setAdditionalFilter(filter);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new OrderCargoForm();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (BeeUtils.same(event.getRelatedViewName(), TBL_CARGO_INCOMES) && event.isOpened()) {
      event.getSelector().setAdditionalFilter(Filter.equals(COL_CARGO, getActiveRowId()));
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (Data.isViewEditable(VIEW_CARGO_INVOICES)) {
      header.addCommandItem(new InvoiceCreator(VIEW_CARGO_SALES,
          Filter.equals(COL_CARGO, row.getId())));
    }
    header.addCommandItem(new Profit(COL_CARGO, BeeUtils.toString(row.getId())));

    if (!DataUtils.isNewRow(row)) {
      header.addCommandItem(getCopyAction());
    }

    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    form.getViewPresenter().getHeader().clearCommandPanel();

    if (defaultCargoType != null) {
      RelationUtils.updateRow(Data.getDataInfo(form.getViewName()), COL_CARGO_TYPE, newRow,
          Data.getDataInfo(VIEW_CARGO_TYPES), defaultCargoType, true);
    }
  }

  private IdentifiableWidget getCopyAction() {
    if (copyAction == null) {
      copyAction = new FaLabel(FontAwesome.COPY);
      copyAction.setTitle(Localized.dictionary().actionCopy());

      copyAction.addClickHandler(clickEvent -> {
        DataInfo info = Data.getDataInfo(getViewName());
        BeeRow cargo = DataUtils.cloneRow(getActiveRow());
        cargo.setId(DataUtils.NEW_ROW_ID);
        cargo.setProperties(null);

        RowFactory.createRow(FORM_CARGO, null, info, cargo, Modality.ENABLED, new RowCallback() {
          @Override
          public void onSuccess(BeeRow newCargo) {
            GridView gridLoading = ViewHelper.getChildGrid(getFormView(), TBL_CARGO_LOADING);
            GridView gridUnLoading = ViewHelper.getChildGrid(getFormView(), TBL_CARGO_UNLOADING);
            GridView[] gridList = new GridView[] {gridLoading, gridUnLoading};

            if (!gridLoading.isEmpty() || !gridUnLoading.isEmpty()) {
              Runnable onCloneChildren = new Runnable() {
                int copiedGrids;

                @Override
                public void run() {
                  copiedGrids++;

                  if (Objects.equals(gridList.length, copiedGrids)) {
                    RowEditor.open(getViewName(), newCargo.getId(), Opener.MODAL);
                  }
                }
              };
              for (GridView gridView : gridList) {
                BeeRowSet newPlaces = Data.createRowSet(gridView.getViewName());
                int cargoIdx = newPlaces.getColumnIndex(COL_CARGO);

                for (IsRow row : gridView.getRowData()) {
                  BeeRow cloned = DataUtils.cloneRow(row);
                  cloned.setValue(cargoIdx, newCargo.getId());
                  newPlaces.addRow(cloned);
                }
                if (!newPlaces.isEmpty()) {
                  newPlaces = DataUtils.createRowSetForInsert(newPlaces);
                  newPlaces.removeColumn(newPlaces.getColumnIndex(COL_PLACE_DATE));
                  Queries.insertRows(newPlaces, new RpcCallback<RowInfoList>() {
                    @Override
                    public void onSuccess(RowInfoList result) {
                      onCloneChildren.run();
                    }
                  });
                } else {
                  onCloneChildren.run();
                }
              }
            }
          }
        });
      });
    }
    return copyAction;
  }

  private void refresh(Long currency) {
    Widget widget = getWidgetByName(COL_AMOUNT);

    if (widget != null) {
      widget.getElement().setInnerText(null);
      Long id = getActiveRowId();

      if (!DataUtils.isId(id)) {
        return;
      }
      ParameterList args = TransportHandler.createArgs(SVC_GET_CARGO_TOTAL);
      args.addDataItem(COL_CARGO, id);

      if (DataUtils.isId(currency)) {
        args.addDataItem(COL_CURRENCY, currency);
      }
      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(getFormView());

          if (!response.hasErrors()) {
            widget.getElement().setInnerText(response.getResponseAsString());
          }
        }
      });
    }
  }
}