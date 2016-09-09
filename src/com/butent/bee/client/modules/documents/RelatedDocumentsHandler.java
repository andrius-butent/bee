package com.butent.bee.client.modules.documents;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

public class RelatedDocumentsHandler extends AbstractGridInterceptor {

  private int documentIndex = BeeConst.UNDEF;

  public RelatedDocumentsHandler() {
  }

  @Override
  public void afterCreate(GridView gridView) {
    documentIndex = gridView.getDataIndex(COL_DOCUMENT);
    super.afterCreate(gridView);
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    DataInfo info = Data.getDataInfo(VIEW_DOCUMENTS);
    BeeRow docRow = RowFactory.createEmptyRow(info, true);

    GridView gridView = presenter.getGridView();

    FormView parentForm = null;
    if (gridView != null) {
      parentForm = ViewHelper.getForm(gridView.asWidget());
    }

    IsRow parentRow = null;
    if (parentForm != null) {
      parentRow = parentForm.getActiveRow();
    }

    if (parentRow != null
        && BeeUtils.inList(parentForm.getFormName(), ProjectConstants.FORM_PROJECT,
                              TransportConstants.FORM_ORDER, TransportConstants.FORM_CARGO)) {

      int idxCmp = info.getColumnIndex(COL_DOCUMENT_COMPANY);
      int idxCmpName = info.getColumnIndex(ALS_DOCUMENT_COMPANY_NAME);
      int idxParCmp = BeeConst.UNDEF;
      int idxParCmpName = BeeConst.UNDEF;
      int idxParCmpTypeName = BeeConst.UNDEF;

      if (BeeUtils.same(parentForm.getFormName(), ProjectConstants.FORM_PROJECT)) {
        idxParCmp = parentForm.getDataIndex(ProjectConstants.COL_COMAPNY);
        idxParCmpName = parentForm.getDataIndex(ProjectConstants.ALS_PROJECT_COMPANY_NAME);
        idxParCmpTypeName = parentForm.getDataIndex(ProjectConstants.ALS_COMPANY_TYPE_NAME);

      } else if (BeeUtils.in(parentForm.getFormName(),
                            TransportConstants.FORM_ORDER, TransportConstants.FORM_CARGO)) {
        idxParCmp = parentForm.getDataIndex(TransportConstants.COL_CUSTOMER);
        idxParCmpName = parentForm.getDataIndex(TransportConstants.COL_CUSTOMER_NAME);
        idxParCmpTypeName = parentForm.getDataIndex(TransportConstants.ALS_CUSTOMER_TYPE_NAME);

      }

      if (!BeeConst.isUndef(idxCmp) && !BeeConst.isUndef(idxCmpName)
          && !BeeConst.isUndef(idxParCmp) && !BeeConst.isUndef(idxParCmpName)) {

        docRow.setValue(idxCmp, parentRow.getLong(idxParCmp));
        docRow.setValue(idxCmpName, BeeConst.isUndef(idxParCmpTypeName)
                                      ? parentRow.getString(idxParCmpName)
                                      : BeeUtils.joinWords(parentRow.getString(idxParCmpName),
                                          parentRow.getString(idxParCmpTypeName)));
      }
    }

    RowFactory.createRow(info, docRow, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(final BeeRow result) {
        final long docId = result.getId();

        presenter.getGridView().ensureRelId(new IdCallback() {
          @Override
          public void onSuccess(Long relId) {
            Queries.insert(AdministrationConstants.VIEW_RELATIONS,
                Data.getColumns(AdministrationConstants.VIEW_RELATIONS,
                    Lists.newArrayList(COL_DOCUMENT, presenter.getGridView().getRelColumn())),
                Queries.asList(docId, relId), null, new RowCallback() {
                  @Override
                  public void onSuccess(BeeRow row) {
                    presenter.handleAction(Action.REFRESH);
                    ViewHelper.getForm(presenter.getGridView().asWidget()).refresh();
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
    return new RelatedDocumentsHandler();
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    event.consume();

    if (!BeeConst.isUndef(documentIndex) && event.getRowValue() != null) {
      Long docId = event.getRowValue().getLong(documentIndex);

      if (DataUtils.isId(docId)) {
        RowEditor.open(VIEW_DOCUMENTS, docId, Opener.MODAL, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            getGridPresenter().handleAction(Action.REFRESH);
          }
        });
      }
    }
  }
}
