package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;

class AllProjectsGrid extends AbstractGridInterceptor {

  private final Long userId = BeeKeeper.getUser().getUserId();

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
}
