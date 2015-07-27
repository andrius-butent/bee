package com.butent.bee.client.modules.documents;

import static com.butent.bee.shared.modules.documents.DocumentConstants.ALS_DOCUMENT_COMPANY_NAME;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_DESCRIPTION;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_DOCUMENT;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_DOCUMENT_COMPANY;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_DOCUMENT_DATA;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_FILE_CAPTION;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_FILE_DATE;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_FILE_VERSION;
import static com.butent.bee.shared.modules.documents.DocumentConstants.FORM_DOCUMENT;
import static com.butent.bee.shared.modules.documents.DocumentConstants.SVC_COPY_DOCUMENT_DATA;
import static com.butent.bee.shared.modules.documents.DocumentConstants.TBL_DOCUMENT_TREE;
import static com.butent.bee.shared.modules.documents.DocumentConstants.VIEW_DOCUMENTS;
import static com.butent.bee.shared.modules.documents.DocumentConstants.VIEW_DOCUMENT_FILES;
import static com.butent.bee.shared.modules.documents.DocumentConstants.VIEW_DOCUMENT_ITEMS;
import static com.butent.bee.shared.modules.documents.DocumentConstants.VIEW_DOCUMENT_TEMPLATES;
import static com.butent.bee.shared.modules.trade.TradeConstants.VAR_TOTAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.*;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.FileLinkRenderer;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public final class DocumentsHandler {

  private static final class DocumentBuilder extends AbstractFormInterceptor {

    private FileCollector collector;

    @Override
    public void afterInsertRow(IsRow result, boolean forced) {
      if (collector != null && !collector.isEmpty()) {
        sendFiles(result.getId(), collector.getFiles(), null);
        collector.clear();
      }

      if (result.getString(Data.getColumnIndex(VIEW_DOCUMENTS, COL_DOCUMENT_COMPANY)) != null) {
        insertCompanyInfo(result, null);
      }
    }

    @Override
    public void afterCreateWidget(String name, IdentifiableWidget widget,
        WidgetDescriptionCallback callback) {

      if (widget instanceof FileCollector) {
        this.collector = (FileCollector) widget;
        this.collector.bindDnd(getFormView());
      }
    }

    @Override
    public FormInterceptor getInstance() {
      return new DocumentBuilder();
    }
  }

  private static final class FileGridHandler extends AbstractGridInterceptor {

    private static Collection<FileInfo> sanitize(GridView gridView,
        Collection<? extends FileInfo> input) {

      Collection<FileInfo> result = new ArrayList<>();
      if (BeeUtils.isEmpty(input)) {
        return result;
      }

      List<? extends IsRow> data = gridView.getRowData();
      if (BeeUtils.isEmpty(data)) {
        result.addAll(input);
        return result;
      }

      int fileIndex = gridView.getDataIndex(AdministrationConstants.COL_FILE);
      int nameIndex = gridView.getDataIndex(AdministrationConstants.ALS_FILE_NAME);
      int sizeIndex = gridView.getDataIndex(AdministrationConstants.ALS_FILE_SIZE);
      int typeIndex = gridView.getDataIndex(AdministrationConstants.ALS_FILE_TYPE);

      Set<FileInfo> oldFiles = new HashSet<>();
      for (IsRow row : data) {
        oldFiles.add(new FileInfo(row.getLong(fileIndex), row.getString(nameIndex),
            row.getLong(sizeIndex), row.getString(typeIndex)));
      }

      List<String> messages = new ArrayList<>();

      for (FileInfo fi : input) {
        if (oldFiles.contains(fi)) {
          messages.add(BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, fi.getName(),
              FileUtils.sizeToText(fi.getSize()),
              TimeUtils.renderCompact(fi.getFileDate())));
        } else {
          result.add(fi);
        }
      }

      if (!messages.isEmpty()) {
        result.clear();

        messages.add(0, Localized.getConstants().documentFileExists());
        gridView.notifyWarning(ArrayUtils.toArray(messages));
      }

      return result;
    }

    private FileCollector collector;

    private FileGridHandler() {
    }

    @Override
    public boolean beforeAction(Action action, final GridPresenter presenter) {
      if (Action.ADD.equals(action)) {
        if (collector != null) {
          collector.clickInput();
        }
        return false;

      } else {
        return super.beforeAction(action, presenter);
      }
    }

    @Override
    public GridInterceptor getInstance() {
      return new FileGridHandler();
    }

    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {

      if (BeeUtils.same(columnName, AdministrationConstants.COL_FILE)) {
        return new FileLinkRenderer(DataUtils.getColumnIndex(columnName, dataColumns),
            DataUtils.getColumnIndex(COL_FILE_CAPTION, dataColumns),
            DataUtils.getColumnIndex(AdministrationConstants.ALS_FILE_NAME, dataColumns));

      } else {
        return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
      }
    }

    @Override
    public void onLoad(final GridView gridView) {
      if (collector == null) {
        collector = FileCollector.headless(new Consumer<Collection<? extends FileInfo>>() {
          @Override
          public void accept(Collection<? extends FileInfo> input) {
            final Collection<FileInfo> files = sanitize(gridView, input);

            if (!files.isEmpty()) {
              gridView.ensureRelId(new IdCallback() {
                @Override
                public void onSuccess(Long result) {
                  sendFiles(result, files, new ScheduledCommand() {
                    @Override
                    public void execute() {
                      gridView.getViewPresenter().handleAction(Action.REFRESH);
                    }
                  });
                }
              });
            }
          }
        });

        gridView.add(collector);

        FormView form = ViewHelper.getForm(gridView.asWidget());
        if (form != null) {
          collector.bindDnd(form);
        }
      }
    }
  }

  private static final class RelatedDocumentsHandler extends AbstractGridInterceptor {

    private int documentIndex = BeeConst.UNDEF;

    private RelatedDocumentsHandler() {
    }

    @Override
    public void afterCreate(GridView gridView) {
      documentIndex = gridView.getDataIndex(COL_DOCUMENT);
      super.afterCreate(gridView);
    }

    @Override
    public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
      DataInfo info = Data.getDataInfo(VIEW_DOCUMENTS);
      final GridView gridView = presenter.getGridView();
      FormView parentForm = null;
      IsRow parentRow = null;
      BeeRow docRow = RowFactory.createEmptyRow(info, true);

      if (gridView != null) {
        parentForm = ViewHelper.getForm(gridView.asWidget());
      }

      if (parentForm != null) {
        parentRow = parentForm.getActiveRow();
      }

      if (parentRow != null
          && BeeUtils.same(parentForm.getFormName(), ProjectConstants.FORM_PROJECT)) {
        int idxCmp = info.getColumnIndex(COL_DOCUMENT_COMPANY);
        int idxCmpName = info.getColumnIndex(ALS_DOCUMENT_COMPANY_NAME);
        int idxParCmp = parentForm.getDataIndex(ProjectConstants.COL_COMAPNY);
        int idxParCmpName = parentForm.getDataIndex(ProjectConstants.ALS_PROJECT_COMPANY_NAME);

        if (!BeeConst.isUndef(idxCmp) && !BeeConst.isUndef(idxCmpName)
            && !BeeConst.isUndef(idxCmp) && !BeeConst.isUndef(idxCmpName)) {

          docRow.setValue(idxCmp, parentRow.getLong(idxParCmp));
          docRow.setValue(idxCmpName, parentRow.getString(idxParCmpName));
        }

      }
      RowFactory.createRow(info, docRow, new RowCallback() {
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
                      ViewHelper.getForm(gridView.asWidget()).refresh();
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

  public static void register() {
    GridFactory.registerGridInterceptor(VIEW_DOCUMENT_TEMPLATES, new DocumentTemplatesGrid());

    GridFactory.registerGridInterceptor(VIEW_DOCUMENTS, new DocumentTemplatesGrid());
    GridFactory.registerGridInterceptor(VIEW_DOCUMENT_FILES, new FileGridHandler());

    GridFactory.registerGridInterceptor("RelatedDocuments", new RelatedDocumentsHandler());

    FormFactory.registerFormInterceptor(TBL_DOCUMENT_TREE, new DocumentTreeForm());

    FormFactory.registerFormInterceptor("DocumentTemplate", new DocumentTemplateForm());
    FormFactory.registerFormInterceptor(FORM_DOCUMENT, new DocumentForm());
    FormFactory.registerFormInterceptor("DocumentItem", new DocumentDataForm());

    FormFactory.registerFormInterceptor("NewDocument", new DocumentBuilder());

    TradeUtils.registerTotalRenderer(VIEW_DOCUMENT_ITEMS, VAR_TOTAL);
  }

  static void copyDocumentData(Long dataId, final IdCallback callback) {
    Assert.notNull(callback);

    if (!DataUtils.isId(dataId)) {
      callback.onSuccess(dataId);
    } else {
      ParameterList args = createArgs(SVC_COPY_DOCUMENT_DATA);
      args.addDataItem(COL_DOCUMENT_DATA, dataId);

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(BeeKeeper.getScreen());

          if (!response.hasErrors()) {
            callback.onSuccess(response.getResponseAsLong());
          }
        }
      });
    }
  }

  static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.DOCUMENTS, method);
  }

  private static void sendFiles(final Long docId, Collection<FileInfo> files,
      final ScheduledCommand onComplete) {

    final String viewName = VIEW_DOCUMENT_FILES;
    final List<BeeColumn> columns = Data.getColumns(viewName);

    final Holder<Integer> latch = Holder.of(files.size());

    for (final FileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, new Callback<Long>() {
        @Override
        public void onSuccess(Long result) {
          BeeRow row = DataUtils.createEmptyRow(columns.size());

          Data.setValue(viewName, row, COL_DOCUMENT, docId);
          Data.setValue(viewName, row, AdministrationConstants.COL_FILE, result);

          Data.setValue(viewName, row, COL_FILE_DATE,
              fileInfo.getFileDate() == null ? new DateTime() : fileInfo.getFileDate());
          Data.setValue(viewName, row, COL_FILE_VERSION, fileInfo.getFileVersion());

          Data.setValue(viewName, row, COL_FILE_CAPTION,
              BeeUtils.notEmpty(fileInfo.getCaption(), fileInfo.getName()));
          Data.setValue(viewName, row, COL_DESCRIPTION, fileInfo.getDescription());

          Queries.insert(viewName, columns, row, new RowCallback() {
            @Override
            public void onSuccess(BeeRow br) {
              latch.set(latch.get() - 1);
              if (!BeeUtils.isPositive(latch.get()) && onComplete != null) {
                onComplete.execute();
              }
            }
          });
        }
      });
    }
  }

  private DocumentsHandler() {
  }

  public static void insertCompanyInfo(IsRow row, String oldValue) {

    if (row == null) {
      return;
    }

    final Long rowId = row.getId();
    final String company = row.getString(Data.getColumnIndex(VIEW_DOCUMENTS,
        COL_DOCUMENT_COMPANY));

    if (!BeeUtils.isEmpty(company)) {

      Filter filter =
          Filter.and(Filter.equals(COL_DOCUMENT, rowId), Filter
              .equals(COL_DOCUMENT_COMPANY, oldValue));

      Queries.update(AdministrationConstants.VIEW_RELATIONS, filter, COL_DOCUMENT_COMPANY, company,
          new IntCallback() {

            @Override
            public void onSuccess(Integer result) {
              if (result == 0) {
                Queries.insert(AdministrationConstants.VIEW_RELATIONS, Data.getColumns(
                    AdministrationConstants.VIEW_RELATIONS,
                    Lists.newArrayList(COL_DOCUMENT_COMPANY,
                        COL_DOCUMENT)), Lists.newArrayList(company, BeeUtils
                    .toString(rowId)));
              }
            }
          });
    }
  }
}
