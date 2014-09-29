package com.butent.bee.client.modules.transport;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.layout.TabbedPages.SelectionOrigin;
import com.butent.bee.client.modules.mail.NewMailMessage;
import com.butent.bee.client.modules.trade.TotalRenderer;
import com.butent.bee.client.output.PrintFormInterceptor;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants.AssessmentStatus;
import com.butent.bee.shared.modules.transport.TransportConstants.OrderStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.Relation.Caching;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class AssessmentForm extends PrintFormInterceptor implements SelectorEvent.Handler,
    ValueChangeHandler<String> {

  private class ChildAssessmentsGrid extends AbstractGridInterceptor implements ClickHandler {
    @Override
    public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
        AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
        EditableColumn editable) {

      if (BeeUtils.inListSame(columnName, COL_ASSESSMENT, COL_STATUS)) {
        column.getCell().addClickHandler(this);
      }
      return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editable);
    }

    @Override
    public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
      HtmlTable container = new HtmlTable();
      final Holder<Long> department = Holder.absent();

      Relation relation = Relation.create(VIEW_ASSESSMENT_EXECUTORS,
          Lists.newArrayList(COL_LAST_NAME, COL_FIRST_NAME, COL_DEPARTMENT_NAME));
      relation.disableNewRow();
      relation.disableEdit();
      relation.setCaching(Caching.LOCAL);
      relation.setFilter(Filter.or(Filter.any(COL_DEPARTMENT, employees.get(userPerson)),
          Filter.notNull(COL_DEPARTMENT_HEAD)));

      final UnboundSelector user = UnboundSelector.create(relation,
          Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME));
      user.addSelectorHandler(new SelectorEvent.Handler() {
        @Override
        public void onDataSelector(SelectorEvent event) {
          if (event.isChanged()) {
            department.set(Data.getLong(event.getRelatedViewName(), event.getRelatedRow(),
                COL_DEPARTMENT));
          }
        }
      });
      container.setText(0, 0, loc.manager(), StyleUtils.NAME_REQUIRED);
      container.setWidget(0, 1, user);

      final InputArea notes = new InputArea();
      notes.setValue(form.getStringValue(COL_ASSESSMENT_NOTES));
      container.setText(1, 0, loc.comment());
      container.setWidget(1, 1, notes);

      Global.inputWidget(loc.trNewAssessment(), container, new InputCallback() {
        @Override
        public String getErrorMessage() {
          if (BeeUtils.isEmpty(user.getNormalizedValue())) {
            user.setFocus(true);
            return loc.valueRequired();
          }
          return super.getErrorMessage();
        }

        @Override
        public void onSuccess() {
          presenter.getGridView().ensureRelId(new IdCallback() {
            @Override
            public void onSuccess(Long assessment) {
              BeeRow newRow = DataUtils.cloneRow(form.getActiveRow());

              for (String col : new String[] {COL_DATE, COL_CARGO, COL_ASSESSMENT_STATUS,
                  COL_ASSESSMENT_EXPENSES, COL_ASSESSMENT_LOG,
                  "LogCount", "Finished", "FinishedCount"}) {
                newRow.clearCell(form.getDataIndex(col));
              }
              if (isRequest()) {
                newRow.setValue(form.getDataIndex(COL_ASSESSMENT_STATUS),
                    AssessmentStatus.NEW.ordinal());
                newRow.setValue(form.getDataIndex(ALS_ORDER_STATUS), OrderStatus.REQUEST.ordinal());
              } else {
                newRow.setValue(form.getDataIndex(ALS_ORDER_STATUS), OrderStatus.ACTIVE.ordinal());
              }
              newRow.setValue(form.getDataIndex(COL_ASSESSMENT), assessment);
              newRow.setValue(form.getDataIndex(COL_ORDER_MANAGER), user.getNormalizedValue());
              newRow.setValue(form.getDataIndex(COL_DEPARTMENT), department.get());
              newRow.setValue(form.getDataIndex(COL_ASSESSMENT_NOTES), notes.getValue());

              Queries.insertRow(DataUtils.createRowSetForInsert(form.getViewName(),
                  form.getDataColumns(), newRow), new Callback<RowInfo>() {
                @Override
                public void onSuccess(RowInfo result) {
                  Queries.getRow(presenter.getViewName(), result.getId(), new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow res) {
                      presenter.getGridView().getGrid().insertRow(res, true);
                    }
                  });
                }
              });
            }
          });
        }
      });
      return false;
    }

    @Override
    public GridInterceptor getInstance() {
      return new ChildAssessmentsGrid();
    }

    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {

      if (BeeUtils.same(columnName, COL_STATUS)) {
        return new AbstractCellRenderer(cellSource) {
          @Override
          public String render(IsRow row) {
            if (isRequest()) {
              AssessmentStatus status = EnumUtils.getEnumByIndex(AssessmentStatus.class,
                  Data.getInteger(VIEW_CHILD_ASSESSMENTS, row, COL_ASSESSMENT_STATUS));

              if (isRevocable(status)) {
                FaLabel lbl = new FaLabel(FontAwesome.UNDO, true);
                lbl.getElement().getStyle().setMarginRight(2, Unit.PX);

                return lbl.toString() + status.getCaption();
              }
              return status.getCaption();
            } else {
              return EnumUtils.getCaption(OrderStatus.class,
                  Data.getInteger(VIEW_CHILD_ASSESSMENTS, row, ALS_ORDER_STATUS));
            }
          }
        };
      }
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }

    @Override
    public void onClick(ClickEvent event) {
      if (event.getSource() instanceof AbstractCell<?>) {
        CellContext context = ((AbstractCell<?>) event.getSource()).getEventContext();
        final IsRow row = context.getRow();

        switch (context.getGrid().getColumnId(context.getColumnIndex())) {
          case COL_ASSESSMENT:
            RowEditor.open(TBL_ASSESSMENTS, row.getId(), Opener.NEW_TAB);
            break;

          case COL_STATUS:
            final String view = getViewName();
            final AssessmentStatus status = EnumUtils.getEnumByIndex(AssessmentStatus.class,
                Data.getInteger(view, row, COL_ASSESSMENT_STATUS));

            if (isRevocable(status)) {
              Global.inputString(loc.trAssessmentRejection(), loc.trAssessmentReason(),
                  new StringCallback() {
                    @Override
                    public void onSuccess(String value) {
                      String oldLog = Data.getString(view, row, COL_ASSESSMENT_LOG);

                      Queries.update(view, row.getId(), row.getVersion(), Data.getColumns(view,
                          Lists.newArrayList(COL_ASSESSMENT_STATUS, COL_ASSESSMENT_LOG)),
                          Lists.newArrayList(BeeUtils.toString(status.ordinal()), oldLog),
                          Lists.newArrayList(BeeUtils.toString(AssessmentStatus.NEW.ordinal()),
                              buildLog(loc.trAssessmentRejection(), value, oldLog)), null,
                          new RowUpdateCallback(view));
                    }
                  });
            }
            break;
        }
      }
    }

    private boolean isRevocable(Enum<?> status) {
      return Objects.equals(status, AssessmentStatus.ANSWERED);
    }
  }

  private class ForwardersGrid extends AbstractGridInterceptor {
    private static final String SERVICE_CARGO = "ServiceCargo";
    private static final String SUPPLIER = "Supplier";
    private static final String FORWARDER = "Forwarder";

    @Override
    public void afterInsertRow(IsRow result) {
      refresh();
    }

    @Override
    public void afterDeleteRow(long rowId) {
      refresh();
    }

    @Override
    public void afterUpdateRow(IsRow result) {
      refresh();
    }

    @Override
    public GridInterceptor getInstance() {
      return new ForwardersGrid();
    }

    @Override
    public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
      super.onReadyForInsert(gridView, event);

      List<BeeColumn> columns = event.getColumns();
      List<String> values = event.getValues();

      String cargo = form.getStringValue(COL_CARGO);

      columns.add(DataUtils.getColumn(COL_CARGO, gridView.getDataColumns()));
      values.add(cargo);
      columns.add(DataUtils.getColumn(SERVICE_CARGO, gridView.getDataColumns()));
      values.add(cargo);

      if (DataUtils.getColumn(SUPPLIER, columns) == null) {
        columns.add(DataUtils.getColumn(SUPPLIER, gridView.getDataColumns()));
        values.add(values.get(DataUtils.getColumnIndex(FORWARDER, columns)));
      }
    }

    private void refresh() {
      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), TBL_CARGO_EXPENSES);
      refreshTotals();
    }
  }

  private class ServicesGrid extends AbstractGridInterceptor {

    @Override
    public void afterDeleteRow(long rowId) {
      if (BeeUtils.same(getViewName(), TBL_CARGO_EXPENSES)) {
        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), TBL_ASSESSMENT_FORWARDERS);
      }
      refresh();
    }

    @Override
    public void afterInsertRow(IsRow result) {
      refresh();
    }

    @Override
    public void afterUpdateCell(IsColumn column, String oldValue, String newValue, IsRow result,
        boolean rowMode) {
      if (BeeUtils.inListSame(column.getId(), COL_DATE, COL_AMOUNT, COL_CURRENCY,
          COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)) {
        refresh();
      }
      if (BeeUtils.same(getViewName(), TBL_CARGO_EXPENSES)) {
        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), TBL_ASSESSMENT_FORWARDERS);
      }
    }

    @Override
    public GridInterceptor getInstance() {
      return new ServicesGrid();
    }

    private void refresh() {
      refreshTotals();

      if (BeeUtils.same(getViewName(), TBL_CARGO_INCOMES)
          && BeeUtils.unbox(form.getBooleanValue("Finished"))) {

        int idx = form.getDataIndex("FinishedCount");
        int cnt = BeeUtils.unbox(form.getActiveRow().getInteger(idx)) + 1;

        form.getOldRow().setValue(idx, cnt);
        form.getActiveRow().setValue(idx, cnt);

        Queries.update(form.getViewName(), form.getActiveRowId(), "FinishedCount",
            new IntegerValue(cnt));
      }
    }
  }

  private class StatusUpdater implements ClickHandler {

    private final AssessmentStatus status;
    private final OrderStatus orderStatus;
    private final boolean check;
    private final boolean request;
    private final String confirmationQuestion;

    public StatusUpdater(AssessmentStatus status, String confirm) {
      this(status, null, false, confirm);
    }

    public StatusUpdater(OrderStatus orderStatus, String confirm) {
      this(orderStatus, null, false, confirm);
    }

    public StatusUpdater(AssessmentStatus status, OrderStatus orderStatus, boolean check,
        String confirm) {
      this.status = status;
      this.orderStatus = orderStatus;
      this.check = check;
      this.confirmationQuestion = confirm;
      this.request = true;
    }

    public StatusUpdater(OrderStatus orderStatus, AssessmentStatus status, boolean check,
        String confirm) {
      this.status = status;
      this.orderStatus = orderStatus;
      this.check = check;
      this.confirmationQuestion = confirm;
      this.request = false;
    }

    @Override
    public void onClick(ClickEvent event) {
      if (isPrimary() && check) {
        Queries.getRowCount(VIEW_CHILD_ASSESSMENTS,
            Filter.and(Filter.equals(COL_ASSESSMENT, form.getActiveRowId()),
                request ? Filter.isNotEqual(COL_ASSESSMENT_STATUS, IntegerValue.of(status))
                    : Filter.isNotEqual(ALS_ORDER_STATUS, IntegerValue.of(orderStatus))),
            new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                if (BeeUtils.isPositive(result)) {
                  Global.showError(Localized.getMessages().trAssessmentInvalidStatusError(result,
                      request ? status.getCaption() : orderStatus.getCaption()));
                } else {
                  checkInvoices();
                }
              }
            });
      } else {
        checkInvoices();
      }
    }

    private void checkInvoices() {
      if (Objects.equals(orderStatus, OrderStatus.COMPLETED)) {
        Queries.getRowCount(TBL_CARGO_INCOMES,
            Filter.and(Filter.equals(COL_CARGO, form.getLongValue(COL_CARGO)),
                Filter.isNull(COL_SALE)),
            new IntCallback() {
              @Override
              public void onSuccess(Integer res) {
                if (BeeUtils.isPositive(res)) {
                  form.notifySevere("Yra neišrašytų sąskaitų", BeeUtils.toString(res));
                } else {
                  confirm();
                }
              }
            });
      } else {
        confirm();
      }
    }

    private void confirm() {
      if (Objects.equals(orderStatus, OrderStatus.CANCELED)
          || Objects.equals(status, AssessmentStatus.LOST)) {
        Global.inputString(confirmationQuestion, loc.trAssessmentReason(), new StringCallback() {
          @Override
          public void onSuccess(String value) {
            save(value);
          }
        });
      } else {
        Global.confirm(confirmationQuestion, new ConfirmationCallback() {
          @Override
          public void onConfirm() {
            save(null);
          }
        });
      }
    }

    public void save(final String notes) {
      ScheduledCommand command = new ScheduledCommand() {
        @Override
        public void execute() {
          int updateSize = Queries.update(getViewName(), form.getDataColumns(),
              form.getOldRow(), form.getActiveRow(), form.getChildrenForUpdate(),
              new RowCallback() {
                @Override
                public void onSuccess(BeeRow result) {
                  RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), result);
                  update(notes);
                }
              });
          if (updateSize == 0) {
            update(notes);
          }
        }
      };
      if (!handleSaveAction(command)) {
        command.execute();
      }
    }

    public void update(String notes) {
      final List<BeeColumn> columns = new ArrayList<>();
      List<String> oldValues = new ArrayList<>();
      final List<String> newValues = new ArrayList<>();
      final String viewName = form.getViewName();

      if (status != null) {
        columns.add(Data.getColumn(viewName, COL_ASSESSMENT_STATUS));
        oldValues.add(form.getStringValue(COL_ASSESSMENT_STATUS));
        newValues.add(BeeUtils.toString(status.ordinal()));
      }
      if (orderStatus != null) {
        columns.add(Data.getColumn(viewName, ALS_ORDER_STATUS));
        oldValues.add(form.getStringValue(ALS_ORDER_STATUS));
        newValues.add(BeeUtils.toString(orderStatus.ordinal()));

        if (Objects.equals(orderStatus, OrderStatus.COMPLETED)) {
          columns.add(Data.getColumn(viewName, "Finished"));
          oldValues.add(form.getStringValue("Finished"));
          newValues.add("1");
        }
      }
      if (!BeeUtils.isEmpty(notes)) {
        String oldLog = form.getStringValue(COL_ASSESSMENT_LOG);
        columns.add(Data.getColumn(viewName, COL_ASSESSMENT_LOG));
        oldValues.add(oldLog);
        newValues.add(buildLog(status != null ? status.getCaption() : orderStatus.getCaption(),
            notes, oldLog));
      }
      Queries.update(viewName, form.getActiveRowId(), form.getActiveRow().getVersion(),
          columns, oldValues, newValues, form.getChildrenForUpdate(), new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              new RowUpdateCallback(form.getViewName()).onSuccess(result);

              if (isPrimary() && !check) {
                List<String> cols = new ArrayList<>();
                List<String> vals = new ArrayList<>();

                for (int i = 0; i < columns.size(); i++) {
                  if (BeeUtils.inListSame(columns.get(i).getId(),
                      COL_ASSESSMENT_STATUS, ALS_ORDER_STATUS)) {
                    cols.add(columns.get(i).getId());
                    vals.add(newValues.get(i));
                  }
                }
                Queries.update(VIEW_CHILD_ASSESSMENTS,
                    Filter.equals(COL_ASSESSMENT, result.getId()), cols, vals, new IntCallback() {
                      @Override
                      public void onSuccess(Integer res) {
                        if (BeeUtils.allNotNull(status, orderStatus)) {
                          DataChangeEvent.fire(BeeKeeper.getBus(), viewName,
                              DataChangeEvent.CANCEL_RESET_REFRESH);
                        } else {
                          DataChangeEvent.fireRefresh(BeeKeeper.getBus(),
                              VIEW_CHILD_ASSESSMENTS);
                        }
                      }
                    });
              }
            }
          });
    }
  }

  private static String buildLog(String caption, String value, String oldLog) {
    return BeeUtils.join("\n\n",
        TimeUtils.nowMinutes().toCompactString() + " " + caption + "\n" + value, oldLog);
  }

  private static void updateTotals(final FormView formView, IsRow row,
      final Widget incomeTotalWidget, final Widget expenseTotalWidget, final Widget profitWidget,
      final Widget incomeWidget, final Widget expenseWidget) {

    boolean ok = false;

    for (Widget widget : new Widget[] {incomeTotalWidget, expenseTotalWidget, profitWidget,
        incomeWidget, expenseWidget}) {
      if (widget != null) {
        ok = true;
        widget.getElement().setInnerText(null);
      }
    }
    if (!ok || !DataUtils.isId(row.getId())) {
      return;
    }
    ParameterList args = TransportHandler.createArgs(SVC_GET_ASSESSMENT_TOTALS);
    args.addDataItem(COL_ASSESSMENT, row.getId());

    Long curr = DataUtils.getLong(formView.getDataColumns(), row, COL_CURRENCY);

    if (DataUtils.isId(curr)) {
      args.addDataItem(COL_CURRENCY, curr);
    }
    if (!DataUtils.isId(row.getLong(formView.getDataIndex(COL_ASSESSMENT)))) {
      args.addDataItem("isPrimary", 1);
    }
    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(formView);

        if (response.hasErrors()) {
          return;
        }
        SimpleRowSet rs = SimpleRowSet.restore((String) response.getResponse());

        double income = BeeUtils.round(BeeUtils
            .toDouble(rs.getValueByKey(COL_SERVICE, TBL_CARGO_INCOMES, COL_AMOUNT)), 2);
        double expense = BeeUtils.round(BeeUtils
            .toDouble(rs.getValueByKey(COL_SERVICE, TBL_CARGO_EXPENSES, COL_AMOUNT)), 2);
        double incomeTotal = BeeUtils.round(BeeUtils.round(BeeUtils
            .toDouble(rs.getValueByKey(COL_SERVICE, TBL_CARGO_INCOMES + VAR_TOTAL, COL_AMOUNT)), 2)
            + income, 2);
        double expenseTotal = BeeUtils.round(BeeUtils.round(BeeUtils
            .toDouble(rs.getValueByKey(COL_SERVICE, TBL_CARGO_EXPENSES + VAR_TOTAL,
                COL_AMOUNT)), 2) + expense, 2);

        if (incomeTotalWidget != null) {
          incomeTotalWidget.getElement().setInnerText(BeeUtils.toString(incomeTotal));
        }
        if (expenseTotalWidget != null) {
          expenseTotalWidget.getElement().setInnerText(BeeUtils.toString(expenseTotal));
        }
        if (profitWidget != null) {
          profitWidget.getElement()
              .setInnerText(BeeUtils.toString(BeeUtils.round(incomeTotal - expenseTotal, 2)));
        }
        if (incomeWidget != null) {
          incomeWidget.getElement()
              .setInnerText(income != 0 ? BeeUtils.parenthesize(income) : null);
        }
        if (expenseWidget != null) {
          expenseWidget.getElement()
              .setInnerText(expense != 0 ? BeeUtils.parenthesize(expense) : null);
        }
      }
    });
  }

  private FormView form;
  private final LocalizableConstants loc = Localized.getConstants();

  private final Button reqNew = new Button(loc.trAssessmentToRequests(),
      new StatusUpdater(AssessmentStatus.NEW, loc.trAssessmentAskRequest()));

  private final Button reqLost = new Button(AssessmentStatus.LOST.getCaption(),
      new StatusUpdater(AssessmentStatus.LOST, loc.trAssessmentAskLost()));

  private final Button reqAnswered = new Button(AssessmentStatus.ANSWERED.getCaption(),
      new StatusUpdater(AssessmentStatus.ANSWERED, null, true, loc.trAssessmentAskAnswered()));

  private final Button reqApproved = new Button(AssessmentStatus.APPROVED.getCaption(),
      new StatusUpdater(AssessmentStatus.APPROVED, OrderStatus.ACTIVE, false,
          loc.trAssessmentAskOrder()));

  private final Button ordRequest = new Button(loc.trAssessmentToRequests(),
      new StatusUpdater(AssessmentStatus.NEW, OrderStatus.REQUEST, false,
          loc.trAssessmentAskRequest()));

  private final Button ordActive = new Button(OrderStatus.ACTIVE.getCaption(),
      new StatusUpdater(OrderStatus.ACTIVE, loc.trAssessmentAskOrder()));

  private final Button ordCanceled = new Button(OrderStatus.CANCELED.getCaption(),
      new StatusUpdater(OrderStatus.CANCELED, loc.trAssessmentAskCanceled()));

  private final Button ordCompleted = new Button(OrderStatus.COMPLETED.getCaption(),
      new StatusUpdater(OrderStatus.COMPLETED, null, true, loc.trAssessmentAskCompleted()));

  private ChildGrid childAssessments;
  private HasWidgets statusLabel;
  private InputBoolean expensesRegistered;
  private ChildGrid childExpenses;
  private DataSelector manager;
  private final Long userPerson = BeeKeeper.getUser().getUserData().getCompanyPerson();
  private final Multimap<Long, Long> departmentHeads = HashMultimap.create();
  private final Map<Long, String> departments = new HashMap<>();
  private final Multimap<Long, Long> employees = HashMultimap.create();

  @Override
  public void afterCreateWidget(final String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      ChildGrid grid = (ChildGrid) widget;
      AbstractGridInterceptor interceptor = null;

      if (BeeUtils.same(name, VIEW_CHILD_ASSESSMENTS)) {
        childAssessments = grid;
        interceptor = new ChildAssessmentsGrid();

      } else if (BeeUtils.same(name, TBL_ASSESSMENT_FORWARDERS)) {
        interceptor = new ForwardersGrid();

      } else if (BeeUtils.inListSame(name, TBL_CARGO_INCOMES, TBL_CARGO_EXPENSES)) {
        interceptor = new ServicesGrid();

        if (BeeUtils.same(name, TBL_CARGO_EXPENSES)) {
          childExpenses = grid;
        }
      }
      if (interceptor != null) {
        grid.setGridInterceptor(interceptor);
      }
    } else if (widget instanceof DataSelector) {
      if (BeeUtils.same(name, COL_CURRENCY)) {
        ((DataSelector) widget).addSelectorHandler(this);

      } else if (BeeUtils.same(name, COL_ORDER_MANAGER)) {
        manager = (DataSelector) widget;
        manager.addSelectorHandler(this);
      }

    } else if (BeeUtils.same(name, COL_STATUS) && widget instanceof HasWidgets) {
      statusLabel = (HasWidgets) widget;

    } else if (BeeUtils.same(name, COL_ASSESSMENT_EXPENSES) && widget instanceof InputBoolean) {
      expensesRegistered = (InputBoolean) widget;
      expensesRegistered.addValueChangeHandler(this);

    } else if (widget instanceof TabbedPages) {
      ((TabbedPages) widget)
          .addSelectionHandler(new SelectionHandler<Pair<Integer, SelectionOrigin>>() {
            @Override
            public void onSelection(SelectionEvent<Pair<Integer, SelectionOrigin>> event) {
              onValueChange(null);
            }
          });
    }
  }

  @Override
  public void afterRefresh(FormView formView, IsRow row) {
    if (form == null) {
      return;
    }
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    boolean newRecord = isNewRow();
    boolean primary = isPrimary();
    boolean request = isRequest();
    boolean executor = isExecutor();

    Integer status = form.getIntegerValue(COL_ASSESSMENT_STATUS);
    int orderStatus = form.getIntegerValue(ALS_ORDER_STATUS);

    String caption = request ? (primary ? loc.trAssessmentRequest() : loc.trAssessment())
        : (primary ? loc.trOrder() : loc.trChildOrder());

    if (!BeeUtils.isEmpty(caption)) {
      header.setCaption(caption);
    }
    if (statusLabel != null) {
      statusLabel.clear();
      statusLabel.add(new InlineLabel(request
          ? EnumUtils.getCaption(AssessmentStatus.class, status)
          : EnumUtils.getCaption(OrderStatus.class, orderStatus)));

      final String log = form.getStringValue(COL_ASSESSMENT_LOG);

      if (!BeeUtils.isEmpty(log)) {
        FaLabel lbl = new FaLabel(FontAwesome.COMMENT_O, true);
        lbl.getElement().getStyle().setMarginLeft(5, Unit.PX);
        lbl.getElement().getStyle().setCursor(Cursor.POINTER);
        lbl.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Global.showInfo(log);
          }
        });
        statusLabel.add(lbl);
      }
    }
    if (executor && !newRecord) {
      header.addCommandItem(new Button(loc.trWriteEmail(), new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          sendMail();
        }
      }));
      if (request) {
        if (AssessmentStatus.NEW.is(status)) {
          header.addCommandItem(reqAnswered);
        }
        if (primary) {
          if (AssessmentStatus.NEW.is(status) || AssessmentStatus.ANSWERED.is(status)) {
            header.addCommandItem(reqLost);
          }
          if (!AssessmentStatus.NEW.is(status)) {
            header.addCommandItem(reqNew);
          }
          if (AssessmentStatus.ANSWERED.is(status)) {
            header.addCommandItem(reqApproved);
          }
        }
      } else {
        if (OrderStatus.ACTIVE.is(orderStatus)) {
          header.addCommandItem(ordCompleted);
        }
        if (primary) {
          if (OrderStatus.ACTIVE.is(orderStatus)) {
            header.addCommandItem(ordCanceled);
          } else {
            header.addCommandItem(ordActive);
          }
          if (!OrderStatus.COMPLETED.is(orderStatus)) {
            header.addCommandItem(ordRequest);
          }
        }
      }
    }
    form.setEnabled(newRecord || executor
        && (AssessmentStatus.NEW.is(status) || OrderStatus.ACTIVE.is(orderStatus)));

    if (expensesRegistered != null) {
      expensesRegistered.setEnabled(executor);
    }
    if (childAssessments != null && !primary) {
      childAssessments.setEnabled(false);
    }
    if (manager != null && manager.isEnabled() && !departmentHeads.containsKey(userPerson)) {
      manager.setEnabled(false);
    }
    onValueChange(null);
    refreshTotals();
  }

  @Override
  public boolean beforeAction(final Action action, final Presenter presenter) {
    if (action == Action.SAVE && !isNewRow()
        && handleSaveAction(new ScheduledCommand() {
          @Override
          public void execute() {
            presenter.handleAction(action);
          }
        })) {
      return false;
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new AssessmentPrintForm() {
      @Override
      public void beforeRefresh(FormView formView, IsRow row) {
        super.beforeRefresh(formView, row);
        updateTotals(formView, row, formView.getWidgetByName(VAR_TOTAL), null, null, null, null);
        updateVehicles(formView.getWidgetByName(COL_FORWARDER_VEHICLE), form.getActiveRowId());
      }

      private void updateVehicles(final Widget vehicleWidget, long assessmentId) {
        if (vehicleWidget != null) {
          Queries.getRowSet(TBL_ASSESSMENT_FORWARDERS, Lists.newArrayList(COL_FORWARDER_VEHICLE),
              Filter.and(Filter.isEqual(COL_ASSESSMENT, new LongValue(assessmentId)),
                  Filter.notNull(COL_FORWARDER_VEHICLE)), new RowSetCallback() {
                @Override
                public void onSuccess(BeeRowSet result) {
                  StringBuilder vehicles = new StringBuilder();

                  for (int i = 0; i < result.getNumberOfRows(); i++) {
                    if (i > 0) {
                      vehicles.append(", ");
                    }
                    vehicles.append(result.getString(i, COL_FORWARDER_VEHICLE));
                  }
                  vehicleWidget.getElement().setInnerText(vehicles.toString());
                }
              });
        }
      }
    };
  }

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentForm();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (BeeUtils.same(event.getRelatedViewName(), TBL_CURRENCIES)) {
      if (event.isChanged() && !isNewRow()) {
        refreshTotals();
      }
    } else if (Objects.equals(event.getSelector(), manager)) {
      if (event.isOpened()) {
        manager.setAdditionalFilter(Filter.any(COL_DEPARTMENT, employees.get(userPerson)));

      } else if (event.isChanged()) {
        updateDepartment(form, form.getActiveRow(),
            Data.getLong(event.getRelatedViewName(), event.getRelatedRow(), COL_DEPARTMENT));
      }
    }
  }

  @Override
  public void onLoad(final FormView formView) {
    Queries.getRowSet(TBL_DEPARTMENT_EMPLOYEES, Lists.newArrayList(COL_DEPARTMENT,
        COL_COMPANY_PERSON, COL_DEPARTMENT_HEAD, COL_DEPARTMENT_NAME), null, new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        for (BeeRow row : result) {
          Long department = row.getLong(0);
          Long employer = row.getLong(1);
          Long headDepartment = row.getLong(2);
          String departmentName = row.getString(3);

          employees.put(employer, department);
          departments.put(department, departmentName);

          if (DataUtils.isId(headDepartment)) {
            departmentHeads.put(employer, headDepartment);
          }
        }
        form = formView;
        updateDepartment(form, form.getActiveRow(), null);
        afterRefresh(form, form.getActiveRow());
      }
    });
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    for (int i = 0; i < event.getColumns().size(); i++) {
      if (BeeUtils.same(event.getColumns().get(i).getId(), COL_DEPARTMENT)) {
        super.onReadyForInsert(listener, event);
        return;
      }
    }
    form.notifySevere(loc.department(), loc.valueRequired());
    event.consume();
  }

  @Override
  public void onStartNewRow(FormView formView, IsRow oldRow, IsRow newRow) {
    newRow.setValue(formView.getDataIndex(COL_ASSESSMENT_STATUS), AssessmentStatus.NEW.ordinal());
    newRow.setValue(formView.getDataIndex(ALS_ORDER_STATUS), OrderStatus.REQUEST.ordinal());
    updateDepartment(formView, newRow, null);
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    if (childExpenses != null && expensesRegistered != null) {
      childExpenses.setEnabled(isExecutor() && !expensesRegistered.isChecked()
          && !AssessmentStatus.LOST.is(form.getIntegerValue(COL_ASSESSMENT_STATUS))
          && !OrderStatus.CANCELED.is(form.getIntegerValue(ALS_ORDER_STATUS)));
    }
  }

  private boolean handleSaveAction(final ScheduledCommand action) {
    final int logIdx = form.getDataIndex(COL_ASSESSMENT_LOG);
    final String oldLog = form.getOldRow().getString(logIdx);
    String newLog = form.getActiveRow().getString(logIdx);

    if (Objects.equals(oldLog, newLog)) {
      final Map<String, DateTime> dates = new LinkedHashMap<>();

      for (String col : new String[] {COL_DATE, "LoadingDate", "UnloadingDate"}) {
        int idx = form.getDataIndex(col);
        DateTime oldValue = form.getOldRow().getDateTime(idx);
        DateTime value = form.getActiveRow().getDateTime(idx);

        if (!Objects.equals(oldValue, value)) {
          dates.put(Localized.maybeTranslate(form.getDataColumns().get(idx).getLabel()), value);
        }
      }
      if (!BeeUtils.isEmpty(dates)) {
        Global.inputString(BeeUtils.join("/", dates.keySet()), loc.trAssessmentReason(),
            new StringCallback() {
              @Override
              public void onSuccess(String value) {
                String log = oldLog;

                for (Entry<String, DateTime> entry : dates.entrySet()) {
                  log = buildLog(BeeUtils.joinWords(entry.getKey(),
                      entry.getValue() != null ? entry.getValue().toCompactString()
                          : loc.filterNullLabel()), value, log);
                }
                form.getActiveRow().setValue(logIdx, log);

                int cntIdx = form.getDataIndex(COL_ASSESSMENT_LOG + "Count");
                form.getActiveRow().setValue(cntIdx,
                    BeeUtils.unbox(form.getActiveRow().getInteger(cntIdx)) + dates.size());

                action.execute();
              }
            });
        return true;
      }
    }
    return false;
  }

  private boolean isExecutor() {
    return Objects.equals(form.getLongValue(COL_COMPANY_PERSON), userPerson)
        || departmentHeads.get(userPerson).contains(form.getLongValue(COL_DEPARTMENT));
  }

  private boolean isPrimary() {
    return !DataUtils.isId(form.getLongValue(COL_ASSESSMENT));
  }

  private boolean isRequest() {
    return OrderStatus.REQUEST.is(form.getIntegerValue(ALS_ORDER_STATUS));
  }

  private boolean isNewRow() {
    return DataUtils.isNewRow(form.getActiveRow());
  }

  private void refreshTotals() {
    updateTotals(form, form.getActiveRow(), form.getWidgetByName(VAR_INCOME + VAR_TOTAL),
        form.getWidgetByName(VAR_EXPENSE + VAR_TOTAL), form.getWidgetByName("Profit"),
        form.getWidgetByName(VAR_INCOME), form.getWidgetByName(VAR_EXPENSE));
  }

  private void sendMail() {
    final String cellStyle = "border:1px solid black;padding:3px;";

    final HtmlTable table = new HtmlTable();
    table.getElement().getStyle().setProperty("border-collapse", "collapse");
    table.setDefaultCellStyles(cellStyle);
    table.setColumnCellStyles(0, cellStyle + "text-align:right; font-weight:bold;");
    int c = 0;

    Multimap<String, String> flds = LinkedListMultimap.create();
    flds.put("CustomerName", null);
    flds.put("Date", null);
    flds.put("Number", null);
    flds.put("OrderNotes", null);
    flds.put("LoadingDate", null);
    flds.put("UnloadingDate", null);
    flds.put("LoadingAddress", "LoadingPostIndex");
    flds.put("LoadingAddress", "LoadingCountryName");
    flds.put("UnloadingAddress", "UnloadingPostIndex");
    flds.put("UnloadingAddress", "UnloadingCountryName");
    flds.put("CargoNotes", null);
    flds.put("Description", null);
    flds.put("Quantity", "QuantityUnitName");
    flds.put("Weight", "WeightUnitName");

    for (String fld : flds.keySet()) {
      String value = form.getStringValue(fld);

      if (!BeeUtils.isEmpty(value)) {
        table.setText(c, 0, Data.getColumnLabel(form.getViewName(), fld));
        String value2 = null;

        for (String val : flds.get(fld)) {
          if (!BeeUtils.isEmpty(val)) {
            value2 = BeeUtils.joinItems(value2, form.getStringValue(val));
          }
        }
        table.setText(c, 1, BeeUtils.joinWords(fld.contains("Date")
            ? TimeUtils.renderCompact(DateTime.restore(value)) : value, value2));
        c++;
      }
    }
    String total = form.getWidgetByName(VAR_INCOME + VAR_TOTAL).getElement().getInnerText();
    table.setText(c, 0, Localized.maybeTranslate("=customerPrice"));
    table.setText(c, 1, BeeUtils.joinWords(total, form.getStringValue("CurrencyName")));
    c++;

    Queries.getRowSet(VIEW_ASSESSMENTS, Lists.newArrayList(COL_CARGO),
        Filter.equals(COL_ASSESSMENT, getActiveRowId()), new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet cargos) {
            Set<Long> cargoIds = new HashSet<>();
            cargoIds.add(form.getLongValue(COL_CARGO));

            for (IsRow row : cargos) {
              cargoIds.add(row.getLong(0));
            }
            Queries.getRowSet(VIEW_CARGO_INCOMES, null,
                Filter.any(COL_CARGO, cargoIds), new RowSetCallback() {
                  @Override
                  public void onSuccess(BeeRowSet incomes) {
                    if (!DataUtils.isEmpty(incomes)) {
                      int r = table.getRowCount();

                      table.getCellFormatter().setColSpan(r, 0, 2);
                      table.getCellFormatter().setHorizontalAlignment(r, 0, TextAlign.CENTER);
                      table.setText(r, 0, Localized.maybeTranslate("=trOrderCargoServices"));
                      r++;

                      TotalRenderer totalRenderer = new TotalRenderer(incomes.getColumns());
                      table.setColumnCellStyles(0, cellStyle);

                      for (IsRow row : incomes) {
                        table.setText(r, 0,
                            Data.getString(VIEW_CARGO_INCOMES, row, COL_SERVICE_NAME));

                        String currency = Data.getString(VIEW_CARGO_INCOMES, row, "CurrencyName");
                        String amount = BeeUtils.joinWords(totalRenderer.render(row), currency);

                        String vat = Data.getString(VIEW_CARGO_INCOMES, row, "Vat");

                        if (!BeeUtils.isEmpty(vat)) {
                          boolean percent = BeeUtils.unbox(Data.getBoolean(VIEW_CARGO_INCOMES, row,
                              "VatPercent"));
                          amount += " (" + Localized.getConstants().vat() + " " + vat
                              + (percent ? "%" : " " + currency) + ")";
                        }
                        table.setText(r, 1, amount);
                        r++;
                      }
                    }
                    Set<String> to = null;
                    String addr = form.getStringValue("PersonEmail");

                    if (addr == null) {
                      addr = form.getStringValue("CustomerEmail");
                    }
                    if (addr != null) {
                      to = Sets.newHashSet(addr);
                    }
                    NewMailMessage.create(to, null, null, null,
                        Document.get().createBRElement().getString() + table.toString(),
                        null, null);
                  }
                });
          }
        });
  }

  private void updateDepartment(FormView formView, IsRow row, Long department) {
    Long dept = department != null
        ? department : row.getLong(formView.getDataIndex(COL_DEPARTMENT));

    if (!DataUtils.isId(dept)) {
      Long person = row.getLong(formView.getDataIndex(COL_COMPANY_PERSON));

      if (employees.containsKey(person)) {
        dept = employees.get(person).iterator().next();
      }
    }
    if (DataUtils.isId(dept)) {
      row.setValue(formView.getDataIndex(COL_DEPARTMENT), dept);
      row.setValue(formView.getDataIndex(COL_DEPARTMENT_NAME), departments.get(dept));
      formView.refreshBySource(COL_DEPARTMENT_NAME);
    }
  }
}
