package com.butent.bee.client.modules.transport;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.AbstractFormInterceptor;
import com.butent.bee.client.ui.FormFactory.FormInterceptor;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.BeeButton;
import com.butent.bee.server.modules.commons.ExchangeUtils;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowActionEvent;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.modules.transport.TransportConstants.AssessmentStatus;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OrderAssessmentForm extends AbstractFormInterceptor {

  private abstract class AssessmentGrid extends AbstractGridInterceptor {
    @Override
    public void beforeRefresh(GridPresenter presenter) {
      presenter.getDataProvider().setParentFilter(COL_ASSESSOR, getFilter());
    }

    @Override
    public Map<String, Filter> getInitialFilters() {
      IsRow row = getFormView().getActiveRow();

      if (row == null) {
        return super.getInitialFilters();
      } else {
        return ImmutableMap.of(COL_ASSESSOR, getFilter());
      }
    }

    @Override
    public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
      newRow.setValue(gridView.getDataIndex(COL_ASSESSOR), getFormView().getActiveRow().getId());
      return true;
    }

    protected Filter getFilter() {
      return ComparisonFilter.isEqual(COL_ASSESSOR,
          new LongValue(getFormView().getActiveRow().getId()));
    }
  }

  private class ServicesGrid extends AssessmentGrid {
    final boolean expense;

    public ServicesGrid(boolean expense) {
      this.expense = expense;
    }

    @Override
    public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
      if (expense) {
        newRow.setValue(gridView.getDataIndex(COL_SERVICE_EXPENSE), true);
      }
      return super.onStartNewRow(gridView, oldRow, newRow);
    }
  }

  private class AssessorGrid extends AssessmentGrid {
    @Override
    public DeleteMode getDeleteMode(GridPresenter presenter, final IsRow activeRow,
        Collection<RowInfo> selectedRows, DeleteMode defMode) {

      final String view = presenter.getViewName();
      final int oldStatus = activeRow.getInteger(Data.getColumnIndex(view, COL_STATUS));

      if (AssessmentStatus.ANSWERED.is(oldStatus)) {
        Global.inputString("Vertinimo atmetimas", "Nurodykite priežastį",
            new StringCallback() {
              @Override
              public void onSuccess(String value) {
                Queries.update(view, activeRow.getId(), activeRow.getVersion(),
                    Lists.newArrayList(Data.getColumn(view, COL_STATUS),
                        Data.getColumn(view, COL_ASSESSOR_NOTES)),
                    Lists.newArrayList(BeeUtils.toString(oldStatus),
                        activeRow.getString(Data.getColumnIndex(view, COL_ASSESSOR_NOTES))),
                    Lists.newArrayList(BeeUtils.toString(AssessmentStatus.NEW.ordinal()),
                        value),
                    new RowUpdateCallback(view));
              }
            });
        return DeleteMode.CANCEL;
      } else {
        return DeleteMode.SINGLE;
      }
    }

    @Override
    public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
      int idx = 0;

      for (Iterator<BeeColumn> iterator = event.getColumns().iterator(); iterator.hasNext();) {
        BeeColumn column = iterator.next();

        if (column.getLevel() > 0) {
          iterator.remove();
          event.getValues().remove(idx);
        } else {
          idx++;
        }
      }
    }

    @Override
    public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
      newRow.setValue(gridView.getDataIndex(COL_ASSESSOR_NOTES),
          getFormView().getActiveRow().getString(getFormView().getDataIndex(COL_ASSESSOR_NOTES)));

      return super.onStartNewRow(gridView, oldRow, newRow);
    }

    @Override
    protected Filter getFilter() {
      IsRow row = getFormView().getActiveRow();

      if (isPrimaryRequest(row)) {
        return ComparisonFilter.compareId(Operator.NE, row.getId());
      } else {
        return super.getFilter();
      }
    }
  }

  private class StatusUpdater implements ClickHandler {

    private final AssessmentStatus status;
    private final String preconditionError;
    private FormView formView = null;
    private int statusIdx;
    private int orderStatusIdx;
    private RowCallback rowCallback;

    public StatusUpdater(AssessmentStatus status, String preconditionError) {
      this.status = status;
      this.preconditionError = preconditionError;
    }

    @Override
    public void onClick(ClickEvent event) {
      if (formView == null) {
        formView = getFormView();
        statusIdx = formView.getDataIndex(COL_STATUS);
        orderStatusIdx = formView.getDataIndex("OrderStatus");
        rowCallback = new RowUpdateCallback(formView.getViewName());
      }
      IsRow row = getFormView().getActiveRow();

      if (isPrimaryRequest(row) && !BeeUtils.isEmpty(preconditionError)) {
        Queries.getRowCount(TBL_CARGO_ASSESSORS, Filter.and(ComparisonFilter.isEqual(COL_CARGO,
            new LongValue(row.getLong(getFormView().getDataIndex(COL_CARGO)))),
            ComparisonFilter.compareId(Operator.NE, row.getId()),
            ComparisonFilter.isNotEqual(COL_STATUS, new IntegerValue(status.ordinal()))),
            new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                if (BeeUtils.isPositive(result)) {
                  Global.showError(preconditionError);
                } else {
                  update();
                }
              }
            });
      } else {
        update();
      }
    }

    public void update() {
      Global.confirm(status.getConfirmation(), new ConfirmationCallback() {
        @Override
        public void onConfirm() {
          final IsRow oldRow = formView.getOldRow();
          final IsRow newRow = formView.getActiveRow();

          final List<Integer> indexes = Lists.newArrayList(statusIdx);
          List<BeeColumn> columns = Lists.newArrayList(DataUtils.getColumn(COL_STATUS,
              formView.getDataColumns()));
          List<String> oldValues = Lists.newArrayList(newRow.getString(statusIdx));
          final List<String> newValues = Lists.newArrayList(BeeUtils.toString(status.ordinal()));

          if (isPrimaryRequest(newRow) && status.getOrderStatus() != null) {
            indexes.add(orderStatusIdx);
            columns.add(DataUtils.getColumn("OrderStatus", formView.getDataColumns()));
            oldValues.add(newRow.getString(orderStatusIdx));
            newValues.add(BeeUtils.toString(status.getOrderStatus().ordinal()));
          }
          Queries.update(formView.getViewName(), newRow.getId(), newRow.getVersion(),
              columns, oldValues, newValues, new RowCallback() {
                @Override
                public void onSuccess(BeeRow result) {
                  rowCallback.onSuccess(result);

                  for (int i = 0; i < indexes.size(); i++) {
                    Value value = new IntegerValue(BeeUtils.toInt(newValues.get(i)));
                    oldRow.setValue(indexes.get(i), value);
                    newRow.setValue(indexes.get(i), value);
                  }
                  newRow.setVersion(result.getVersion());

                  if (isPrimaryRequest(newRow)) {
                    Queries.update(TBL_CARGO_ASSESSORS,
                        Filter.and(ComparisonFilter.isEqual(COL_CARGO,
                            new LongValue(newRow.getLong(formView.getDataIndex(COL_CARGO)))),
                            ComparisonFilter.compareId(Operator.NE, newRow.getId())),
                        COL_STATUS, new IntegerValue(status.ordinal()), new IntCallback() {
                          @Override
                          public void onSuccess(Integer res) {
                            if (status.isClosable()) {
                              formView.getViewPresenter().handleAction(Action.CLOSE);
                              GridView gridView = getGridView();

                              if (gridView != null) {
                                gridView.getViewPresenter().handleAction(Action.REFRESH);
                              }
                            } else {
                              formView.refresh(true);
                            }
                          }
                        });
                  } else {
                    formView.refresh(false);
                  }
                }
              });
        }
      });
    }
  }

  public static void doRowAction(final RowActionEvent event) {
    if (BeeUtils.same(event.getOptions(), "open")) {
      RowEditor.openRow(FORM_ORDER_ASSESSMENT, TBL_CARGO_ASSESSORS, event.getRow(), false, null);
    }
  }

  public static void updateTotal(final FormView form, IsRow row, final Widget tot) {
    if (tot == null) {
      return;
    }
    tot.getElement().setInnerText(null);

    ParameterList args = TransportHandler.createArgs(SVC_GET_ASSESSMENT_TOTAL);
    args.addDataItem(COL_CARGO, row.getLong(form.getDataIndex(COL_CARGO)));

    Long currency = row.getLong(form.getDataIndex(ExchangeUtils.FLD_CURRENCY));

    if (currency != null) {
      args.addDataItem(ExchangeUtils.FLD_CURRENCY, currency);
    }
    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(form);

        if (response.hasErrors()) {
          return;
        }
        double total = BeeUtils.round(BeeUtils.toDouble((String) response.getResponse()), 2);
        tot.getElement().setInnerText(total != 0 ? Double.toString(total) : null);
      }
    });
  }

  private final BeeButton cmdNew = new BeeButton("Užklausimas",
      new StatusUpdater(AssessmentStatus.NEW, null));

  private final BeeButton cmdLost = new BeeButton("Pralaimėtas",
      new StatusUpdater(AssessmentStatus.LOST, null));

  private final BeeButton cmdAnswered = new BeeButton("Atsakytas",
      new StatusUpdater(AssessmentStatus.ANSWERED, "Yra likę nepatvirtintų vertinimų"));

  private final BeeButton cmdActive = new BeeButton("Užsakymas",
      new StatusUpdater(AssessmentStatus.ACTIVE, null));

  private final BeeButton cmdCompleted = new BeeButton("Įvykdytas",
      new StatusUpdater(AssessmentStatus.COMPLETED, "Yra likę vykdomų antrinių užsakymų"));

  private final BeeButton cmdCanceled = new BeeButton("Atšauktas",
      new StatusUpdater(AssessmentStatus.CANCELED, null));

  private final List<ChildGrid> grids = Lists.newArrayList();

  @Override
  public void afterCreateWidget(final String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      AssessmentGrid interceptor = null;

      if (BeeUtils.same(name, "AssessmentExpenses")) {
        interceptor = new ServicesGrid(true);

      } else if (BeeUtils.same(name, "AssessmentIncomes")) {
        interceptor = new ServicesGrid(false);

      } else if (BeeUtils.same(name, TBL_CARGO_ASSESSORS)) {
        interceptor = new AssessorGrid();
      }
      if (interceptor != null) {
        ChildGrid grid = ((ChildGrid) widget);
        grid.setGridInterceptor(interceptor);
        grids.add(grid);
      }
    } else if (widget instanceof HasClickHandlers) {
      if (BeeUtils.inList(name, "LT", "RU", "EN")) {
        ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            String printView = "PrintOrder";
            RowEditor.openRow(printView + name, Data.getDataInfo(printView),
                getFormView().getActiveRow().getId(), true, null, null);
          }
        });
      }
    }
  }

  @Override
  public void afterRefresh(final FormView form, IsRow row) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (row == null) {
      return;
    }
    boolean primary = isPrimaryRequest(row);
    boolean owner = Objects.equal(row.getLong(form.getDataIndex(COL_ASSESSOR_MANAGER)),
        BeeKeeper.getUser().getUserId());
    int status = row.getInteger(form.getDataIndex(COL_STATUS));

    header.setCaption(AssessmentStatus.in(status,
        AssessmentStatus.ACTIVE, AssessmentStatus.COMPLETED, AssessmentStatus.CANCELED)
        ? (primary ? "Užsakymas" : "Antrinis užsakymas")
        : (primary ? form.getCaption() : "Vertinimas"));

    if (owner) {
      header.addCommandItem(new BeeButton("Rašyti laišką", new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
        }
      }));
      if (AssessmentStatus.NEW.is(status)) {
        header.addCommandItem(cmdAnswered);
      }
      if (primary) {
        if (AssessmentStatus.in(status, AssessmentStatus.NEW, AssessmentStatus.ANSWERED)) {
          header.addCommandItem(cmdLost);
        }
        if (!AssessmentStatus.NEW.is(status)) {
          header.addCommandItem(cmdNew);
        }
        if (AssessmentStatus.ANSWERED.is(status)) {
          header.addCommandItem(cmdActive);
        }
        if (AssessmentStatus.ACTIVE.is(status)) {
          header.addCommandItem(cmdCanceled);
        }
      }
      if (AssessmentStatus.ACTIVE.is(status)) {
        header.addCommandItem(cmdCompleted);
      }
    }
    boolean ok = owner
        && AssessmentStatus.in(status, AssessmentStatus.NEW, AssessmentStatus.ACTIVE);

    form.setEnabled(ok && primary);

    for (ChildGrid grid : grids) {
      grid.setEnabled(ok);
    }
    for (String item : new String[] {"Income", "Expenses"}) {
      Widget cap = form.getWidgetByName(item + "Total");

      if (cap != null) {
        double total = BeeUtils.round(BeeUtils.unbox(row.getDouble(form.getDataIndex(item))), 2);
        cap.getElement().setInnerText(total != 0 ? BeeUtils.parenthesize(total) : "");
      }
    }
    Widget tot = form.getWidgetByName("Total");
    Widget curr = form.getWidgetByName("Currency");

    if (tot != null) {
      if (primary) {
        updateTotal(form, row, tot);
      } else {
        tot.getElement().setInnerText(null);
      }
    }
    if (curr != null) {
      curr.setVisible(primary);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new OrderAssessmentForm();
  }

  private boolean isPrimaryRequest(IsRow row) {
    return !DataUtils.isId(row.getLong(getFormView().getDataIndex(COL_ASSESSOR)));
  }
}
