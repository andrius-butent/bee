package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Invokes SQL statement builders for MySQL, Microsoft SQL Server, Oracle, PostgreSQL servers, sets
 * default SQL builder for a particular instance of application.
 */

public class SqlBuilderFactory {

  private static String defaultEngine;
  private static SqlBuilder defaultBuilder;

  public static SqlBuilder getBuilder() {
    return defaultBuilder;
  }

  public static SqlBuilder getBuilder(String engine) {
    SqlBuilder builder;

    if (BeeUtils.same(engine, BeeConst.MYSQL)) {
      builder = new MySqlBuilder();
    } else if (BeeUtils.same(engine, BeeConst.MSSQL)) {
      builder = new MsSqlBuilder();
    } else if (BeeUtils.same(engine, BeeConst.ORACLE)) {
      builder = new OracleSqlBuilder();
    } else if (BeeUtils.same(engine, BeeConst.PGSQL)) {
      builder = new PostgreSqlBuilder();
    } else {
      builder = new GenericSqlBuilder();
    }
    return builder;
  }

  public static String getEngine() {
    return defaultEngine;
  }

  public static synchronized void setDefaultEngine(String engine) {
    defaultEngine = engine;
    defaultBuilder = getBuilder(defaultEngine);
  }
}
