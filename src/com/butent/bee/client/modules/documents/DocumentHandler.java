package com.butent.bee.client.modules.documents;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.VAR_TOTAL;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.presenter.GridFormPresenter;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.FileLinkRenderer;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
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
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class DocumentHandler {

  private static final class DocumentBuilder extends AbstractFormInterceptor {

    private static final BeeLogger logger = LogUtils.getLogger(DocumentBuilder.class);

    private static void copyValues(FormView form, IsRow oldRow, IsRow newRow,
        List<String> colNames) {
      for (String colName : colNames) {
        int index = form.getDataIndex(colName);
        if (index >= 0) {
          newRow.setValue(index, oldRow.getString(index));
        } else {
          logger.warning("copyValues: column", colName, "not found");
        }
      }
    }

    private FileCollector collector;

    private DocumentBuilder() {
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

    @Override
    public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
      Assert.notNull(event);
      event.consume();

      if (getCollector() == null) {
        event.getCallback().onFailure("File collector not found");
        return;
      }

      if (getCollector().isEmpty()) {
        event.getCallback().onFailure(Localized.getConstants().chooseFiles());
        return;
      }

      Queries.insert(VIEW_DOCUMENTS, event.getColumns(), event.getValues(),
          event.getChildren(), new RowCallback() {
            @Override
            public void onFailure(String... reason) {
              event.getCallback().onFailure(reason);
            }

            @Override
            public void onSuccess(BeeRow result) {
              event.getCallback().onSuccess(result);
              sendFiles(result.getId(), getCollector().getFiles(), null);
            }
          });
    }

    @Override
    public void onStartNewRow(final FormView form, IsRow oldRow, final IsRow newRow) {
      if (getCollector() != null) {
        getCollector().clear();
      }

      if (oldRow != null) {
        copyValues(form, oldRow, newRow,
            Lists.newArrayList(COL_DOCUMENT_CATEGORY, ALS_CATEGORY_NAME,
                COL_DOCUMENT_TYPE, ALS_TYPE_NAME,
                COL_DOCUMENT_PLACE, ALS_PLACE_NAME));

      } else if (form.getViewPresenter() instanceof GridFormPresenter) {
        GridInterceptor gcb = ((GridFormPresenter) form.getViewPresenter()).getGridInterceptor();

        if (gcb instanceof DocumentGridHandler) {
          IsRow category = ((DocumentGridHandler) gcb).getSelectedCategory();

          if (category != null) {
            newRow.setValue(form.getDataIndex(COL_DOCUMENT_CATEGORY), category.getId());
            newRow.setValue(form.getDataIndex(ALS_CATEGORY_NAME),
                ((DocumentGridHandler) gcb).getCategoryValue(category, COL_DOCUMENT_NAME));
          }
        }
      }
    }

    private FileCollector getCollector() {
      return collector;
    }
  }

  private static final class DocumentGridHandler extends AbstractGridInterceptor implements
      SelectionHandler<IsRow> {

    private static final String FILTER_KEY = "f1";

    private static Filter getFilter(Long category) {
      if (category == null) {
        return null;
      } else {
        return Filter.equals(COL_DOCUMENT_CATEGORY, category);
      }
    }

    private TreeView treeView;
    private IsRow selectedCategory;

    private DocumentGridHandler() {
    }

    @Override
    public void afterCreateWidget(String name, IdentifiableWidget widget,
        WidgetDescriptionCallback callback) {

      if (widget instanceof TreeView) {
        setTreeView((TreeView) widget);
        getTreeView().addSelectionHandler(this);
      }
    }

    @Override
    public DocumentGridHandler getInstance() {
      return new DocumentGridHandler();
    }

    @Override
    public List<String> getParentLabels() {
      if (getSelectedCategory() == null || getTreeView() == null) {
        return super.getParentLabels();
      } else {
        return getTreeView().getPathLabels(getSelectedCategory().getId(), COL_CATEGORY_NAME);
      }
    }

    @Override
    public void onSelection(SelectionEvent<IsRow> event) {
      if (event != null && getGridPresenter() != null) {
        setSelectedCategory(event.getSelectedItem());
        Long category = (getSelectedCategory() == null) ? null : getSelectedCategory().getId();

        getGridPresenter().getDataProvider().setParentFilter(FILTER_KEY, getFilter(category));
        getGridPresenter().refresh(true);
      }
    }

    private String getCategoryValue(IsRow category, String colName) {
      if (BeeUtils.allNotNull(category, getTreeView())) {
        return category.getString(Data.getColumnIndex(getTreeView().getViewName(), colName));
      }
      return null;
    }

    private IsRow getSelectedCategory() {
      return selectedCategory;
    }

    private TreeView getTreeView() {
      return treeView;
    }

    private void setSelectedCategory(IsRow selectedCategory) {
      this.selectedCategory = selectedCategory;
    }

    private void setTreeView(TreeView treeView) {
      this.treeView = treeView;
    }
  }

  private static final class FileGridHandler extends AbstractGridInterceptor {

    private static List<NewFileInfo> sanitize(GridView gridView, Collection<NewFileInfo> input) {
      List<NewFileInfo> result = Lists.newArrayList();
      if (BeeUtils.isEmpty(input)) {
        return result;
      }

      List<? extends IsRow> data = gridView.getRowData();
      if (BeeUtils.isEmpty(data)) {
        result.addAll(input);
        return result;
      }

      int nameIndex = gridView.getDataIndex(AdministrationConstants.ALS_FILE_NAME);
      int sizeIndex = gridView.getDataIndex(AdministrationConstants.ALS_FILE_SIZE);
      int dateIndex = gridView.getDataIndex(COL_FILE_DATE);

      Set<NewFileInfo> oldFiles = Sets.newHashSet();
      for (IsRow row : data) {
        oldFiles.add(new NewFileInfo(row.getString(nameIndex),
            BeeUtils.unbox(row.getLong(sizeIndex)), row.getDateTime(dateIndex)));
      }

      List<String> messages = Lists.newArrayList();

      for (NewFileInfo nfi : input) {
        if (oldFiles.contains(nfi)) {
          messages.add(BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, nfi.getName(),
              FileUtils.sizeToText(nfi.getSize()),
              TimeUtils.renderCompact(nfi.getLastModified())));
        } else {
          result.add(nfi);
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
        collector = FileCollector.headless(new Consumer<Collection<NewFileInfo>>() {
          @Override
          public void accept(Collection<NewFileInfo> input) {
            final Collection<NewFileInfo> files = sanitize(gridView, input);

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

        FormView form = UiHelper.getForm(gridView.asWidget());
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
      RowFactory.createRow(VIEW_DOCUMENTS, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          final long docId = result.getId();

          presenter.getGridView().ensureRelId(new IdCallback() {
            @Override
            public void onSuccess(Long relId) {
              Queries.insert(AdministrationConstants.TBL_RELATIONS,
                  Data.getColumns(AdministrationConstants.TBL_RELATIONS,
                      Lists.newArrayList(COL_DOCUMENT, presenter.getGridView().getRelColumn())),
                  Queries.asList(docId, relId), null, new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow row) {
                      presenter.handleAction(Action.REFRESH);
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
          RowEditor.openRow(VIEW_DOCUMENTS, docId, true, new RowCallback() {
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

    GridFactory.registerGridInterceptor(VIEW_DOCUMENTS, new DocumentGridHandler());
    GridFactory.registerGridInterceptor(VIEW_DOCUMENT_FILES, new FileGridHandler());

    GridFactory.registerGridInterceptor("RelatedDocuments", new RelatedDocumentsHandler());

    FormFactory.registerFormInterceptor("DocumentTemplate", new DocumentTemplateForm());
    FormFactory.registerFormInterceptor("Document", new DocumentForm());
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
    ParameterList args = BeeKeeper.getRpc().createParameters(Module.DOCUMENTS.getName());
    args.addQueryItem(AdministrationConstants.METHOD, method);
    return args;
  }

  private static void sendFiles(final Long docId, Collection<NewFileInfo> files,
      final ScheduledCommand onComplete) {

    final String viewName = VIEW_DOCUMENT_FILES;
    final List<BeeColumn> columns = Data.getColumns(viewName);

    final Holder<Integer> latch = Holder.of(files.size());

    for (final NewFileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, new Callback<Long>() {
        @Override
        public void onSuccess(Long result) {
          BeeRow row = DataUtils.createEmptyRow(columns.size());

          Data.setValue(viewName, row, COL_DOCUMENT, docId);
          Data.setValue(viewName, row, AdministrationConstants.COL_FILE, result);

          Data.setValue(viewName, row, COL_FILE_DATE,
              BeeUtils.nvl(fileInfo.getFileDate(), fileInfo.getLastModified()));
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

  private DocumentHandler() {
  }
}
