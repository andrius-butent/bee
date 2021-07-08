package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.TreeGridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.List;

class AllProjectsGrid extends TreeGridInterceptor {

  private final Long userId = BeeKeeper.getUser().getUserId();

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {
    if (BeeUtils.same(columnName, NAME_SLACK) && column instanceof HasCellRenderer) {
      ((HasCellRenderer) column).setRenderer(new ProjectSlackRenderer(dataColumns));
    }
    return true;
  }

  @Override
  public AbstractFilterSupplier getFilterSupplier(String columnName,
      ColumnDescription columnDescription) {
    if (BeeUtils.same(columnName, COL_OVERDUE)) {
      return new OverdueFilterSupplier(columnDescription.getFilterOptions());
    } else if (BeeUtils.same(columnName, NAME_SLACK)) {
      return new ProjectSlackFilterSupplier(columnDescription.getFilterOptions());
    } else {
      return super.getFilterSupplier(columnName, columnDescription);
    }
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {

    Provider provider = presenter.getDataProvider();

    int idxStatus = provider.getColumnIndex(COL_PROJECT_STATUS);
    int idxOwner = provider.getColumnIndex(COL_PROJECT_OWNER);

    int statusValue = BeeUtils.unbox(activeRow.getInteger(idxStatus));
    long ownerValue = BeeUtils.unbox(activeRow.getLong(idxOwner));

    boolean active = EnumUtils.getEnumByIndex(ProjectStatus.class, statusValue)
        == ProjectStatus.ACTIVE;

    boolean owner = ownerValue == BeeUtils.unbox(userId);

    if (active) {
      presenter.getGridView().notifyWarning(
          BeeUtils.joinWords(Localized.dictionary().project(), activeRow.getId(),
              Localized.dictionary().prjStatusActive())
          );
      return GridInterceptor.DeleteMode.CANCEL;
    } else if (owner) {
      return GridInterceptor.DeleteMode.SINGLE;
    } else {
      presenter.getGridView().notifyWarning(Localized.dictionary().prjDeleteCanManager());
      return GridInterceptor.DeleteMode.CANCEL;
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new AllProjectsGrid();
  }

  @Override
  protected Filter getFilter(Long category) {
    if (category == null) {
      return null;
    } else {
      return Filter.equals(COL_PROJECT_CATEGORY, category);
    }
  }

  IsRow getSelectedCategory() {
    return getSelectedTreeItem();
  }
}
