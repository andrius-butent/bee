package com.butent.bee.server;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import com.butent.bee.server.http.HttpUtils;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.export.XWorkbook;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/export")
@SuppressWarnings("serial")
public class ExportServlet extends LoginServlet {

  private static BeeLogger logger = LogUtils.getLogger(ExportServlet.class);

  private static final String EXT_WORKBOOK = "xlsx";

  private static short convertBorderStyle(BorderStyle input) {
    if (input == null) {
      return CellStyle.BORDER_NONE;
    }

    switch (input) {
      case DASHED:
        return CellStyle.BORDER_DASHED;
      case DOTTED:
        return CellStyle.BORDER_DOTTED;
      case DOUBLE:
        return CellStyle.BORDER_DOUBLE;

      case HIDDEN:
      case NONE:
        return CellStyle.BORDER_NONE;

      case GROOVE:
      case INSET:
      case OUTSET:
      case RIDGE:
        return CellStyle.BORDER_MEDIUM;

      case SOLID:
        return CellStyle.BORDER_THIN;
    }

    Assert.untouchable();
    return CellStyle.BORDER_NONE;
  }

  private static Font convertFont(XSSFWorkbook wb, XFont input) {
    if (input == null) {
      return null;
    }

    XSSFFont font = wb.createFont();

    if (!BeeUtils.isEmpty(input.getName())) {
      font.setFontName(input.getName());
    }
    if (BeeUtils.isPositive(input.getFactor())) {
      int hp = BeeUtils.round(font.getFontHeightInPoints() * input.getFactor());
      if (hp > 0) {
        font.setFontHeightInPoints((short) hp);
      }
    }

    if (input.getWeight() != null) {
      switch (input.getWeight()) {
        case BOLD:
        case BOLDER:
          font.setBoldweight(Font.BOLDWEIGHT_BOLD);
          break;

        case NORMAL:
          font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
          break;

        default:
          logger.warning("font weight", input.getWeight(), "not converted");
      }
    }

    if (input.getStyle() != null) {
      switch (input.getStyle()) {
        case ITALIC:
        case OBLIQUE:
          font.setItalic(true);
          break;
        case NORMAL:
          font.setItalic(false);
          break;
      }
    }

    if (!BeeUtils.isEmpty(input.getColor())) {
      byte[] rgb = Color.getRgb(input.getColor());

      if (rgb == null) {
        logger.warning("cannot parse color", input.getColor());
      } else {
        font.setColor(new XSSFColor(rgb));
      }
    }
    
    return font;
  }

  private static CellStyle convertStyle(XSSFWorkbook wb, XStyle input, Map<Integer, Font> fonts) {
    if (input == null) {
      return null;
    }

    XSSFCellStyle cellStyle = wb.createCellStyle();
    
    if (!BeeUtils.isEmpty(input.getColor())) {
      byte[] rgb = Color.getRgb(input.getColor());
      
      if (rgb == null) {
        logger.warning("cannot parse color", input.getColor());
      } else {
        cellStyle.setFillForegroundColor(new XSSFColor(rgb));
        cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
      }
    }

    if (input.getFontRef() != null) {
      Font font = fonts.get(input.getFontRef());

      if (font == null) {
        logger.warning("invalid font index", input.getFontRef());
      } else {
        cellStyle.setFont(font);
      }
    }

    if (!BeeUtils.isEmpty(input.getFormat())) {
      cellStyle.setDataFormat(wb.createDataFormat().getFormat(input.getFormat()));
    }

    if (input.getTextAlign() != null) {
      switch (input.getTextAlign()) {
        case CENTER:
          cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
          break;
        case END:
        case RIGHT:
          cellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
          break;
        case JUSTIFY:
          cellStyle.setAlignment(CellStyle.ALIGN_JUSTIFY);
          break;
        case LEFT:
        case START:
          cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
          break;
        case MATCH_PARENT:
          cellStyle.setAlignment(CellStyle.ALIGN_GENERAL);
          break;
        case START_END:
          cellStyle.setAlignment(CellStyle.ALIGN_FILL);
          break;
      }
    }

    if (input.getVerticalAlign() != null) {
      switch (input.getVerticalAlign()) {
        case BOTTOM:
          cellStyle.setVerticalAlignment(CellStyle.VERTICAL_BOTTOM);
          break;
        case CENTRAL:
        case MIDDLE:
          cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
          break;
        case TOP:
          cellStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
          break;
        default:
          logger.warning("vertical align", input.getVerticalAlign(), "not converted");
      }
    }

    if (input.getBorderLeft() != null) {
      cellStyle.setBorderLeft(convertBorderStyle(input.getBorderLeft()));
    }

    if (input.getBorderRight() != null) {
      cellStyle.setBorderRight(convertBorderStyle(input.getBorderRight()));
    }

    if (input.getBorderTop() != null) {
      cellStyle.setBorderTop(convertBorderStyle(input.getBorderTop()));
    }

    if (input.getBorderBottom() != null) {
      cellStyle.setBorderBottom(convertBorderStyle(input.getBorderBottom()));
    }

    return cellStyle;
  }

  private static Workbook createWorkbook(XWorkbook inputBook) {
    XSSFWorkbook wb = new XSSFWorkbook();

    for (XSheet inputSheet : inputBook.getSheets()) {
      Sheet sheet;
      if (BeeUtils.isEmpty(inputSheet.getName())) {
        sheet = wb.createSheet();
      } else {
        sheet = wb.createSheet(inputSheet.getName());
      }

      Map<Integer, Font> fonts = new HashMap<>();
      if (!inputSheet.getFonts().isEmpty()) {
        for (int i = 0; i < inputSheet.getFonts().size(); i++) {
          XFont inputFont = inputSheet.getFont(i);
          if (inputFont != null) {
            fonts.put(i, convertFont(wb, inputFont));
          }
        }
      }

      Map<Integer, CellStyle> styles = new HashMap<>();
      if (!inputSheet.getStyles().isEmpty()) {
        for (int i = 0; i < inputSheet.getStyles().size(); i++) {
          XStyle inputStyle = inputSheet.getStyle(i);
          if (inputStyle != null) {
            styles.put(i, convertStyle(wb, inputStyle, fonts));
          }
        }
      }

      for (XRow inputRow : inputSheet.getRows()) {
        Row row = sheet.createRow(inputRow.getIndex());

        if (BeeUtils.isPositive(inputRow.getHeightFactor())) {
          row.setHeightInPoints(sheet.getDefaultRowHeightInPoints()
              * inputRow.getHeightFactor().floatValue());
        }

        if (inputRow.getStyleRef() != null) {
          CellStyle rowStyle = styles.get(inputRow.getStyleRef());

          if (rowStyle == null) {
            logger.warning("row", inputRow.getIndex(), "invalid style index",
                inputRow.getStyleRef());
          } else {
            row.setRowStyle(rowStyle);
          }
        }

        for (XCell inputCell : inputRow.getCells()) {
          Cell cell = row.createCell(inputCell.getIndex());

          Value value = inputCell.getValue();
          if (value != null && !value.isNull()) {
            ValueType type = value.getType();

            switch (type) {
              case BOOLEAN:
                cell.setCellValue(value.getBoolean());
                break;
              case DATE:
                cell.setCellValue(value.getDate().getJava());
                break;
              case DATE_TIME:
                cell.setCellValue(value.getDateTime().getJava());
                break;
              case DECIMAL:
              case INTEGER:
              case LONG:
              case NUMBER:
                cell.setCellValue(value.getDouble());
                break;
              case TEXT:
              case TIME_OF_DAY:
                cell.setCellValue(value.getString());
                break;
              default:
                logger.warning("cell type", type, "not supported");
            }

          } else if (!BeeUtils.isEmpty(inputCell.getFormula())) {
            cell.setCellFormula(inputCell.getFormula());
          }

          if (inputCell.getStyleRef() != null) {
            CellStyle cellStyle = styles.get(inputCell.getStyleRef());

            if (cellStyle == null) {
              logger.warning("row", inputRow.getIndex(), "cell", inputCell.getIndex(),
                  "invalid style index", inputCell.getStyleRef());
            } else {
              cell.setCellStyle(cellStyle);
            }
          }

          if (inputCell.getRowSpan() > 1 || inputCell.getColSpan() > 1) {
            int firstRow = row.getRowNum();
            int lastRow = row.getRowNum() + Math.max(inputCell.getRowSpan() - 1, 0);
            int firstCol = cell.getColumnIndex();
            int lastCol = cell.getColumnIndex() + Math.max(inputCell.getColSpan() - 1, 0);

            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
          }
        }
      }
      
      if (!inputSheet.getAutoSize().isEmpty()) {
        for (int column : inputSheet.getAutoSize()) {
          sheet.autoSizeColumn(column, true);
        }
      }
    }

    return wb;
  }

  @Override
  protected void doService(HttpServletRequest req, HttpServletResponse resp) {
    long start = System.currentTimeMillis();

    RequestInfo reqInfo = new RequestInfo(req);
    String svc = reqInfo.getService();

    if (Service.EXPORT_WORKBOOK.equals(svc)) {
      String fileName = reqInfo.getParameter(Service.VAR_FILE_NAME);
      if (BeeUtils.isEmpty(fileName)) {
        HttpUtils.badRequest(resp, svc, "parameter not found:", Service.VAR_FILE_NAME);
        return;
      }

      String data = reqInfo.getParameter(Service.VAR_DATA);
      if (BeeUtils.isEmpty(data)) {
        HttpUtils.badRequest(resp, svc, "parameter not found:", Service.VAR_DATA);
        return;
      }

      XWorkbook input = XWorkbook.restore(data);
      if (input == null || input.isEmpty()) {
        HttpUtils.badRequest(resp, svc, "workbook is empty");
        return;
      }

      Workbook workbook = createWorkbook(input);

      resp.reset();
      resp.setContentType(MediaType.MICROSOFT_EXCEL.toString());

      String name = Codec.rfc5987(FileNameUtils.defaultExtension(fileName, EXT_WORKBOOK));
      resp.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=" + name);

      logger.info(">", svc, fileName, TimeUtils.elapsedSeconds(start));

      try {
        OutputStream output = resp.getOutputStream();
        workbook.write(output);
        output.flush();

      } catch (IOException ex) {
        logger.error(ex);
      }

    } else {
      HttpUtils.badRequest(resp, NameUtils.getName(this), "service not recognized", svc);
    }
  }
}
