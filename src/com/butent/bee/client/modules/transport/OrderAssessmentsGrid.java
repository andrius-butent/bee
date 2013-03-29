package com.butent.bee.client.modules.transport;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.grid.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.GridInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.modules.transport.TransportConstants.AssessmentStatus;
import com.butent.bee.shared.modules.transport.TransportConstants.OrderStatus;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.Map;

public class OrderAssessmentsGrid extends AbstractGridInterceptor {

  private final Map<AssessmentStatus, InputBoolean> checks = Maps.newHashMap();

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    AssessmentStatus status = NameUtils.getEnumByName(AssessmentStatus.class, name);

    if (widget instanceof InputBoolean && status != null) {
      checks.put(status, (InputBoolean) widget);

      checks.get(status).addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          getGridPresenter().handleAction(Action.REFRESH);
        }
      });
    }
  }

  @Override
  public DeleteMode beforeDeleteRow(final GridPresenter presenter, final IsRow row) {
    final long orderId = row.getLong(presenter.getGridView().getDataIndex(COL_ORDER));

    Queries.deleteRow(TBL_ORDERS, orderId, 0, new IntCallback() {
      @Override
      public void onSuccess(Integer result) {
        BeeKeeper.getBus().fireEvent(new RowDeleteEvent(TBL_ORDERS, orderId));
        BeeKeeper.getBus().fireEvent(new RowDeleteEvent(presenter.getViewName(), row.getId()));
      }
    });
    return DeleteMode.CANCEL;
  }

  @Override
  public void beforeRefresh(GridPresenter presenter) {
    presenter.getDataProvider().setParentFilter(COL_STATUS, getFilter());
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {

    GridView gridView = presenter.getGridView();

    boolean primary = !DataUtils.isId(activeRow.getLong(gridView.getDataIndex(COL_ASSESSOR)));
    boolean owner = Objects.equal(activeRow.getLong(gridView.getDataIndex(COL_ASSESSOR_MANAGER)),
        BeeKeeper.getUser().getUserId());
    boolean validState = AssessmentStatus.NEW
        .is(activeRow.getInteger(gridView.getDataIndex(COL_STATUS)));

    if (!primary || !owner || !validState) {
      Global.showError("No way");
      return DeleteMode.CANCEL;
    } else {
      return DeleteMode.SINGLE;
    }
  }

  @Override
  public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
    return new BeeRowSet(gridDescription.getViewName(),
        Data.getColumns(gridDescription.getViewName()));
  }

  @Override
  public GridInterceptor getInstance() {
    return new OrderAssessmentsGrid();
  }

  @Override
  public boolean onLoad(GridDescription gridDescription) {
    gridDescription.setFilter(ComparisonFilter.isEqual(COL_ASSESSOR_MANAGER,
        new LongValue(BeeKeeper.getUser().getUserId())));
    return true;
  }

  @Override
  public void onShow(GridPresenter presenter) {
    presenter.handleAction(Action.REFRESH);
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
    newRow.setValue(gridView.getDataIndex("OrderStatus"), OrderStatus.REQUESTED.ordinal());
    return true;
  }

  private Filter getFilter() {
    CompoundFilter filter = Filter.or();

    for (AssessmentStatus check : checks.keySet()) {
      if (BooleanValue.unpack(checks.get(check).getValue())) {
        filter.add(ComparisonFilter.isEqual(COL_STATUS, new IntegerValue(check.ordinal())));
      }
    }
    if (filter.isEmpty()) {
      filter.add(Filter.isFalse());
    }
    return filter;
  }
}
