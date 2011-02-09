package com.butent.bee.server.data;

import com.butent.bee.server.communication.ResponseBuffer;
import com.butent.bee.server.jdbc.JdbcException;
import com.butent.bee.server.jdbc.JdbcUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeDate;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.utils.LogUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.ejb.Stateless;

@Stateless
public class ResultSetBean {
  private static final Logger logger = Logger.getLogger(ResultSetBean.class.getName());
  private static final BeeColumn[] metaCols;

  static {
    String[] arr = new String[]{
        "index", "id", "name", "label", "schema", "catalog", "table", "class", "sql type",
        "type name", "type", "precision", "scale", "nullable", "pattern", "display size", "signed",
        "auto increment", "case sensitive", "currency", "searchable", "read only", "writable",
        "definitely writable", "properties"};

    metaCols = new BeeColumn[arr.length];
    for (int i = 0; i < arr.length; i++) {
      metaCols[i] = new BeeColumn(arr[i]);
    }
  }

  public void rsMdToResponse(ResultSet rs, ResponseBuffer buff, boolean debug) {
    Assert.noNulls(rs, buff);
    BeeDate start = new BeeDate();

    BeeColumn[] cols = null;
    int c;

    try {
      cols = JdbcUtils.getColumns(rs);
      c = cols.length;
    } catch (JdbcException ex) {
      LogUtils.error(logger, ex);
      buff.addError(ex);
      c = 0;
    }

    if (c <= 0) {
      buff.addSevere("Cannot get result set meta data");
      return;
    }

    for (int i = 0; i < metaCols.length; i++) {
      buff.addColumn(metaCols[i]);
    }
    if (debug) {
      buff.addColumn(new BeeColumn(start.toLog()));
    }

    BeeColumn z;
    for (int i = 0; i < c; i++) {
      z = cols[i];

      buff.add(z.getIndex(), z.getId(), z.getName(), z.getLabel(), z.getSchema(), z.getCatalog(),
          z.getTable(), z.getClazz(), z.getSqlType(), z.getTypeName(), z.getType(),
          z.getPrecision(), z.getScale(), z.getNullable(), z.getPattern(), z.getDisplaySize(),
          z.isSigned(), z.isAutoIncrement(), z.isCaseSensitive(), z.isCurrency(), z.isSearchable(),
          z.isReadOnly(), z.isWritable(), z.isDefinitelyWritable(), z.getProperties());

      if (debug) {
        buff.add(new BeeDate().toLog());
      }
    }
  }

  public void rsToResponse(ResultSet rs, ResponseBuffer buff, boolean debug) {
    Assert.noNulls(rs, buff);
    BeeDate start = new BeeDate();

    BeeColumn[] cols = null;
    int c;

    try {
      cols = JdbcUtils.getColumns(rs);
      c = cols.length;
    } catch (JdbcException ex) {
      LogUtils.error(logger, ex);
      buff.addError(ex);
      c = 0;
    }

    if (c <= 0) {
      buff.addSevere("Cannot get result set meta data");
      return;
    }

    for (int i = 0; i < c; i++) {
      buff.addColumn(cols[i]);
    }

    if (debug) {
      buff.addColumn(new BeeColumn(start.toLog()));
    }

    try {
      while (rs.next()) {
        for (int i = 0; i < c; i++) {
          buff.add(rs.getString(cols[i].getIndex()));
        }
        if (debug) {
          buff.add(new BeeDate().toLog());
        }
      }
    } catch (SQLException ex) {
      LogUtils.error(logger, ex);
      buff.addError(ex);
      buff.clearData();
    }
  }
}
