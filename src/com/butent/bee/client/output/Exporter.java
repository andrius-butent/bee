package com.butent.bee.client.output;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.Response;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.Settings;
import com.butent.bee.client.communication.RpcUtils;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.utils.Duration;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.ColumnInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.export.XWorkbook;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import elemental.xml.XMLHttpRequest;

public final class Exporter {

  public abstract static class FileNameCallback extends StringCallback {
    @Override
    public boolean validate(String value) {
      return validateFileName(value);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Exporter.class);

  private static final int INPUT_STEP_SIZE = 100;
  private static final int OUTPUT_STEP_SIZE = 1_000;
  private static final int SPLIT_ROWS_THRESHOLD = 1_000;

  private static final double PARENT_LABEL_HEIGHT_FACTOR = 1.1;
  private static final double CAPTION_HEIGHT_FACTOR = 1.5;
  private static final double FILTER_LABEL_HEIGHT_FACTOR = 1.0;
  private static final double HEADER_HEIGHT_FACTOR = 1.2;
  private static final double FOOTER_HEIGHT_FACTOR = 1.1;

  private static final int MAX_NUMBER_OF_ROWS_FOR_AUTOSIZE = 1_000;

  public static void addCaption(XSheet sheet, String caption, int rowIndex, int columnCount) {
    Assert.notNull(sheet);
    Assert.notEmpty(caption);

    XFont font = XFont.bold();
    font.setFactor(CAPTION_HEIGHT_FACTOR);

    XStyle style = XStyle.center();
    style.setFontRef(sheet.registerFont(font));

    XCell cell = new XCell(0, caption, sheet.registerStyle(style));
    if (columnCount > 1) {
      cell.setColSpan(columnCount);
    }

    XRow row = new XRow(rowIndex);
    row.setHeightFactor(CAPTION_HEIGHT_FACTOR);

    row.add(cell);
    sheet.add(row);
  }

  public static void addFilterLabel(XSheet sheet, String label, int rowIndex, int columnCount) {
    XStyle style = XStyle.center();

    XCell cell = new XCell(0, label, sheet.registerStyle(style));
    if (columnCount > 1) {
      cell.setColSpan(columnCount);
    }

    XRow row = new XRow(rowIndex);
    row.setHeightFactor(FILTER_LABEL_HEIGHT_FACTOR);

    row.add(cell);
    sheet.add(row);
  }

  public static void clearServerCache(final String id) {
    doRequest(Service.EXPORT_CLEAR, id, null, null, result -> logger.debug(result));
  }

  public static void confirm(String fileName, FileNameCallback callback) {
    Assert.notNull(callback);

    int width = BeeUtils.resize(BeeUtils.trim(fileName).length(), 20, 100, 300, 600);

    Global.inputString(Localized.dictionary().exportToMsExcel(),
        Localized.dictionary().fileName(), callback, null, fileName, 200, null,
        width, CssUnit.PX, BeeConst.UNDEF,
        Action.EXPORT.getCaption(), Action.CANCEL.getCaption(), null);
  }

  public static XCell createCell(String text, ValueType type, int cellIndex, Integer styleRef) {
    if (BeeUtils.isEmpty(text)) {
      return null;

    } else {
      Value value = null;

      if (ValueType.isNumeric(type)) {
        Double d = BeeUtils.toDoubleOrNull(BeeUtils.removeWhiteSpace(text));
        if (BeeUtils.isDouble(d)) {
          value = new NumberValue(d);
        }
      }

      if (value == null) {
        value = new TextValue(text);
      }

      XCell xc = new XCell(cellIndex, value);
      if (styleRef != null) {
        xc.setStyleRef(styleRef);
      }

      return xc;
    }
  }

  public static XRow createFooterRow(int rowIndex) {
    XRow row = new XRow(rowIndex);
    row.setHeightFactor(FOOTER_HEIGHT_FACTOR);
    return row;
  }

  public static XRow createHeaderRow(int rowIndex) {
    XRow row = new XRow(rowIndex);
    row.setHeightFactor(HEADER_HEIGHT_FACTOR);
    return row;
  }

  public static void export(GridPresenter presenter, String caption, final String fileName) {
    Assert.notNull(presenter);
    Assert.isTrue(validateFileName(fileName));

    CellGrid grid = presenter.getGridView().getGrid();

    final int rowCount = grid.getRowCount();
    if (rowCount <= 0) {
      String message = "grid is empty";
      logger.warning(message);
      BeeKeeper.getScreen().notifyWarning(message);
      return;
    }

    final List<ColumnInfo> columns = new ArrayList<>();

    List<ColumnInfo> gridColumns = grid.getColumns();
    for (ColumnInfo column : gridColumns) {
      if (column.isExportable()) {
        columns.add(column);
      }
    }

    int columnCount = columns.size();
    if (columnCount <= 0) {
      String message = "No exportable columns found";
      logger.warning(message);
      BeeKeeper.getScreen().notifyWarning(message);
      return;
    }

    final XSheet sheet = new XSheet();

    Double rowHeightFactor = getRowHeightFactor(grid);
    if (rowHeightFactor != null) {
      sheet.setRowHeightFactor(rowHeightFactor);
    }

    for (int i = 0; i < columnCount; i++) {
      Double widthFactor = columns.get(i).getExportWidthFactor();
      if (BeeUtils.isPositive(widthFactor)) {
        sheet.setColumnWidthFactor(i, widthFactor);
      }
    }

    int rowIndex = 0;

    XRow row;
    XCell cell;
    XFont font;
    XStyle style;

    Integer styleRef;

    List<String> parentLabels = presenter.getParentLabels();
    if (!BeeUtils.isEmpty(parentLabels)) {
      font = new XFont();
      font.setFactor(PARENT_LABEL_HEIGHT_FACTOR);

      style = new XStyle();
      style.setFontRef(sheet.registerFont(font));
      styleRef = sheet.registerStyle(style);

      for (String label : parentLabels) {
        cell = new XCell(0, label, styleRef);
        if (columnCount > 1) {
          cell.setColSpan(columnCount);
        }

        row = new XRow(rowIndex++);
        row.setHeightFactor(PARENT_LABEL_HEIGHT_FACTOR);

        row.add(cell);
        sheet.add(row);
      }
    }

    if (!BeeUtils.isEmpty(caption)) {
      if (rowHeightFactor != null) {
        row = new XRow(rowIndex);
        row.setHeightFactor(BeeConst.DOUBLE_ONE);
        sheet.add(row);
      }
      rowIndex++;

      addCaption(sheet, caption, rowIndex++, columnCount);
    }

    Filter filter = presenter.getDataProvider().getUserFilter();
    if (filter != null) {
      String label = presenter.getFilterLabel();
      if (BeeUtils.isEmpty(label)) {
        label = filter.toString();
      }

      addFilterLabel(sheet, label, rowIndex++, columnCount);
    }

    if (rowIndex > 0) {
      if (rowHeightFactor != null) {
        row = new XRow(rowIndex);
        row.setHeightFactor(BeeConst.DOUBLE_ONE);
        sheet.add(row);
      }
      rowIndex++;
    }

    if (grid.hasHeaders()) {
      row = createHeaderRow(rowIndex++);

      style = XStyle.center();
      style.setVerticalAlign(VerticalAlign.MIDDLE);
      style.setColor(Colors.LIGHTGRAY);
      style.setFontRef(sheet.registerFont(XFont.bold()));

      styleRef = sheet.registerStyle(style);

      for (int i = 0; i < columnCount; i++) {
        ColumnHeader header = columns.get(i).getHeader();
        if (header != null) {
          row.add(new XCell(i, header.getExportLabel(), styleRef));
        }
      }
      sheet.add(row);
    }

    final Map<Integer, Integer> bodyStyles = new HashMap<>();
    for (int i = 0; i < columnCount; i++) {
      styleRef = columns.get(i).getColumn().initExport(sheet);

      if (styleRef != null) {
        bodyStyles.put(i, styleRef);
      }
    }

    final CellContext context = new CellContext(grid);

    int dataSize = grid.getDataSize();
    String viewName = presenter.getViewName();

    if (dataSize >= rowCount || BeeUtils.isEmpty(viewName)) {
      for (int i = 0; i < dataSize; i++) {
        IsRow dataRow = BeeUtils.getQuietly(grid.getRowData(), i);
        if (dataRow != null) {
          exportRow(context, dataRow, columns, rowIndex++, sheet, bodyStyles);
        }
      }

      if (grid.hasFooters()) {
        row = createFooterRow(rowIndex++);

        style = XStyle.background(Colors.LIGHTGRAY);
        style.setFontRef(sheet.registerFont(XFont.bold()));

        styleRef = sheet.registerStyle(style);

        String footerValue;
        TextAlign footerTextAlign;
        Integer footerStyleRef;

        for (int i = 0; i < columnCount; i++) {
          ColumnFooter footer = columns.get(i).getFooter();

          if (footer == null) {
            footerValue = null;
            footerTextAlign = null;
          } else {
            footerValue = footer.reduce(grid.getRowData());
            footerTextAlign = BeeUtils.isEmpty(footerValue) ? null : footer.getTextAlign();
          }

          if (footerTextAlign == null) {
            footerStyleRef = styleRef;
          } else {
            XStyle footerStyle = style.copy();
            footerStyle.setTextAlign(footerTextAlign);

            footerStyleRef = sheet.registerStyle(footerStyle);
          }

          row.add(new XCell(i, Strings.nullToEmpty(footerValue), footerStyleRef));
        }

        sheet.add(row);
      }

      autosizeNoPictures(sheet, columns.size());
      export(sheet, fileName);

    } else {
      int stepSize = getInputStep(rowCount);
      int numberOfSteps = rowCount / stepSize;
      if (rowCount % stepSize > 0) {
        numberOfSteps++;
      }

      final Latch latch = new Latch(numberOfSteps);

      final int firstRowIndex = rowIndex;

      filter = presenter.getDataProvider().getFilter();
      Order order = presenter.getDataProvider().getOrder();

      for (int offset = 0; offset < rowCount; offset += stepSize) {
        Queries.getRowSet(viewName, null, filter, order, offset, stepSize, CachingPolicy.NONE,
            new Queries.RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                String respOffset = result.getTableProperty(Service.VAR_VIEW_OFFSET);
                Integer remaining = latch.get();
                logger.debug(remaining, respOffset);

                if (BeeUtils.isDigit(respOffset)) {
                  int index = BeeUtils.toInt(respOffset) + firstRowIndex;
                  for (IsRow dataRow : result) {
                    exportRow(context, dataRow, columns, index++, sheet, bodyStyles);
                  }
                }

                latch.decrement();
                if (remaining <= 1) {
                  BeeKeeper.getRpc().getRpcList().tryCompress();

                  if (rowCount <= MAX_NUMBER_OF_ROWS_FOR_AUTOSIZE) {
                    autosizeNoPictures(sheet, columns.size());
                  }
                  export(sheet, fileName);
                }
              }
            });
      }
    }
  }

  public static void export(XSheet sheet, String fileName) {
    if (sheet == null || sheet.isEmpty()) {
      logger.severe(NameUtils.getClassName(Exporter.class), "sheet is empty");

    } else {
      XWorkbook wb = new XWorkbook();
      wb.add(sheet);
      export(wb, fileName);
    }
  }

  public static void export(final XWorkbook wb, final String fileName) {
    if (wb == null || wb.isEmpty()) {
      logger.severe(NameUtils.getClassName(Exporter.class), "workbook is empty");

    } else if (BeeUtils.isEmpty(fileName)) {
      logger.severe(NameUtils.getClassName(Exporter.class), "file name not specified");

    } else if (!validateFileName(fileName)) {
      logger.severe(NameUtils.getClassName(Exporter.class), "invalid file name", fileName);

    } else {
      final String exportId = createId();

      if (splitRows(wb)) {
        int rowCount = wb.getRowCount();
        int stepSize = getOutputStep(rowCount);

        int numberOfSteps = rowCount / stepSize;
        if (rowCount % stepSize > 0) {
          numberOfSteps++;
        }

        final Latch latch = new Latch(numberOfSteps);
        final Holder<Boolean> success = Holder.of(true);

        for (int offset = 0; offset < rowCount; offset += stepSize) {
          if (!BeeUtils.isTrue(success.get())) {
            break;
          }

          int toIndex = Math.min(offset + stepSize, rowCount);
          List<XRow> rows = wb.getSheets().get(0).getRows().subList(offset, toIndex);

          sendRows(exportId, offset, rows, ok -> {
            if (ok) {
              latch.decrement();

              if (latch.isOpen()) {
                wb.clearRows();
                submit(exportId, wb, fileName);
              }

            } else {
              success.set(false);
            }
          });
        }

      } else {
        submit(exportId, wb, fileName);
      }
    }
  }

  public static void maybeExport(final XSheet sheet, String fileName) {
    FileNameCallback callback = new FileNameCallback() {
      @Override
      public void onSuccess(String value) {
        export(sheet, BeeUtils.trim(value));
      }
    };

    confirm(fileName, callback);
  }

  public static boolean validateFileName(String input) {
    return !BeeUtils.isEmpty(sanitizeFileName(input));
  }

  private static void autosizeNoPictures(XSheet sheet, int columnCount) {
    for (int i = 0; i < columnCount; i++) {
      if (!sheet.getColumnWidthFactors().containsKey(i) && !sheet.hasPictures(i)) {
        sheet.autoSizeColumn(i);
      }
    }
  }

  private static String createId() {
    return BeeUtils.join(BeeConst.STRING_UNDER, System.currentTimeMillis(),
        BeeUtils.randomString(10));
  }

  private static void doRequest(String svc, String id, Map<String, String> parameters, String data,
      final Callback<String> callback) {

    final Map<String, String> qp = new HashMap<>();
    qp.put(Service.RPC_VAR_SVC, svc);
    if (!BeeUtils.isEmpty(id)) {
      qp.put(Service.VAR_ID, id);
    }
    if (!BeeUtils.isEmpty(parameters)) {
      qp.putAll(parameters);
    }

    boolean hasData = !BeeUtils.isEmpty(data);

    String url = CommUtils.addQueryString(getUrl(), CommUtils.buildQueryString(qp, false));
    RequestBuilder.Method method = hasData ? RequestBuilder.POST : RequestBuilder.GET;

    final XMLHttpRequest xhr = RpcUtils.createXhr();
    xhr.open(method.toString(), url);
    RpcUtils.addSessionId(xhr);

    final Duration duration = new Duration();

    xhr.setOnload(event -> {
      if (xhr.getStatus() == Response.SC_OK) {
        duration.finish();
        logger.info("<", qp, BeeUtils.bracket(duration.getCompletedTime()));
        logger.addSeparator();

        callback.onSuccess(xhr.getResponseText());

      } else {
        String msg = BeeUtils.joinWords("response status:",
            BeeUtils.bracket(xhr.getStatus()), xhr.getStatusText());

        logger.severe(qp, msg);
        callback.onFailure(qp.toString(), msg);
      }
    });

    logger.info(">", qp, hasData ? BeeUtils.toString(data.length()) : BeeConst.STRING_EMPTY);

    duration.restart(null);
    if (hasData) {
      xhr.send(data);
    } else {
      xhr.send();
    }
  }

  private static void exportRow(CellContext context, IsRow dataRow, List<ColumnInfo> columns,
      int rowIndex, XSheet sheet, Map<Integer, Integer> styles) {

    context.setRow(dataRow);

    XRow row = new XRow(rowIndex);

    int columnCount = columns.size();
    for (int j = 0; j < columnCount; j++) {
      context.setColumnIndex(j);

      ColumnInfo columnInfo = columns.get(j);

      Integer styleRef = null;
      if (columnInfo.getDynStyles() != null) {
        styleRef = columnInfo.getDynStyles().getExportStyleRef(dataRow, sheet);
      }
      if (styleRef == null) {
        styleRef = styles.get(j);
      }

      XCell cell = columnInfo.getColumn().export(context, styleRef, sheet);
      if (cell != null) {
        row.add(cell);
      }
    }

    sheet.add(row);
  }

  private static int getInputStep(int rowCount) {
    int step = BeeUtils.positive(Settings.getExporterInputStepRows(), INPUT_STEP_SIZE);
    return Math.min(rowCount, step);
  }

  private static int getOutputStep(int rowCount) {
    int step = BeeUtils.positive(Settings.getExporterOutputStepRows(), OUTPUT_STEP_SIZE);
    return Math.min(rowCount, step);
  }

  private static Double getRowHeightFactor(CellGrid grid) {
    int height = grid.getBodyCellHeight();
    if (height >= 30) {
      return height / 20d;
    } else {
      return null;
    }
  }

  private static String getUrl() {
    return GWT.getHostPageBaseURL() + "export";
  }

  private static String sanitizeFileName(String input) {
    return FileNameUtils.sanitize(input, BeeConst.STRING_POINT);
  }

  private static void sendRows(String id, int offset, List<XRow> rows,
      final Consumer<Boolean> consumer) {

    Map<String, String> parameters = new HashMap<>();
    parameters.put(Service.VAR_FROM, BeeUtils.toString(offset));

    doRequest(Service.EXPORT_ROWS, id, parameters, Codec.beeSerialize(rows),
        new Callback<String>() {
          @Override
          public void onFailure(String... reason) {
            Callback.super.onFailure(reason);
            consumer.accept(false);
          }

          @Override
          public void onSuccess(String result) {
            consumer.accept(true);
          }
        });
  }

  private static boolean splitRows(XWorkbook wb) {
    int x = BeeUtils.positive(Settings.getExporterSplitRowsThreshold(), SPLIT_ROWS_THRESHOLD);
    return wb.getSheetCount() == 1 && wb.getRowCount() >= x;
  }

  private static void submit(final String id, XWorkbook wb, final String fileName) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(Service.VAR_FILE_NAME, sanitizeFileName(fileName));

    doRequest(Service.EXPORT_WORKBOOK, id, parameters, wb.serialize(),
        new Callback<String>() {
          @Override
          public void onFailure(String... reason) {
            Callback.super.onFailure(reason);
            clearServerCache(id);
          }

          @Override
          public void onSuccess(String result) {
            if (DataUtils.isId(result)) {
              BrowsingContext.open(FileUtils.getUrl(BeeUtils.toLong(result)));
            } else {
              onFailure(Service.EXPORT_WORKBOOK, id, fileName, "response", result, "not an id");
            }
          }
        });
  }

  private Exporter() {
  }
}
