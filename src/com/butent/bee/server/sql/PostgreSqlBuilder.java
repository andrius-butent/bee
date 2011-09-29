package com.butent.bee.server.sql;

import com.butent.bee.server.sql.SqlConstants.SqlDataType;
import com.butent.bee.server.sql.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;

/**
 * Contains specific requirements for SQL statement building for PostgreSQL server.
 */

class PostgreSqlBuilder extends SqlBuilder {

  @Override
  protected String getSelect(SqlSelect ss) {
    int limit = ss.getLimit();
    int offset = ss.getOffset();
    String sql = super.getSelect(ss);

    if (BeeUtils.isPositive(limit)) {
      sql += " LIMIT " + limit;
    }
    if (BeeUtils.isPositive(offset)) {
      sql += " OFFSET " + offset;
    }
    return sql;
  }

  @Override
  protected String sqlKeyword(SqlKeyword option, Map<String, Object> params) {
    switch (option) {
      case CREATE_TRIGGER_FUNCTION:
        List<String[]> content = (List<String[]>) params.get("content");
        String text = null;

        for (String[] entry : content) {
          String fldName = entry[0];
          String relTable = entry[1];
          String relField = entry[2];
          String var = "OLD." + sqlQuote(fldName);

          text = BeeUtils.concat(1, text, "IF", var, "IS NOT NULL THEN",
              new SqlDelete(relTable).setWhere(SqlUtils.equal(relTable, relField, 69))
                  .getQuery().replace("69", var),
              "; END IF;");
        }
        String name = "trigger_" + Codec.crc32((String) params.get("name"));

        return BeeUtils.concat(1,
            "CREATE OR REPLACE FUNCTION", name + "()", "RETURNS TRIGGER AS",
            "$" + name + "$", "BEGIN", text, "RETURN NULL; END;", "$" + name + "$",
            "LANGUAGE plpgsql;");

      case CREATE_TRIGGER:
        return BeeUtils.concat(1,
            "CREATE TRIGGER", params.get("name"), params.get("timing"), params.get("event"),
            "ON", params.get("table"), params.get("scope"),
            "EXECUTE PROCEDURE", "trigger_" + Codec.crc32((String) params.get("name")) + "();");

      case DB_NAME:
        return "SELECT current_database() as " + sqlQuote("dbName");

      case DB_SCHEMA:
        return "SELECT current_schema() as " + sqlQuote("dbSchema");

      default:
        return super.sqlKeyword(option, params);
    }
  }

  @Override
  protected String sqlQuote(String value) {
    return "\"" + value + "\"";
  }

  @Override
  protected String sqlTransform(Object x) {
    Object val;

    if (x instanceof Value) {
      val = ((Value) x).getObjectValue();
    } else {
      val = x;
    }
    String s = super.sqlTransform(val);

    if (val instanceof CharSequence) {
      s = s.replace("\\", "\\\\");
    }
    return s;
  }

  @Override
  protected String sqlType(SqlDataType type, int precision, int scale) {
    switch (type) {
      case BOOLEAN:
        return "NUMERIC(1)";
      case DOUBLE:
        return "DOUBLE PRECISION";
      default:
        return super.sqlType(type, precision, scale);
    }
  }
}
