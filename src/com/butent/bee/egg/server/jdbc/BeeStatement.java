package com.butent.bee.egg.server.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.BeeConst;
import com.butent.bee.egg.shared.utils.BeeUtils;
import com.butent.bee.egg.shared.utils.LogUtils;
import com.butent.bee.egg.shared.utils.PropUtils;
import com.butent.bee.egg.shared.utils.StringProp;

public class BeeStatement {
  private static final Logger logger = Logger.getLogger(BeeStatement.class
      .getName());

  private int state = BeeConst.STATE_UNKNOWN;
  private List<SQLException> errors = new ArrayList<SQLException>();

  private int fetchDirection;
  private int fetchSize;
  private int maxFieldSize;
  private int maxRows;
  private int queryTimeout;
  private int concurrency;
  private int holdability;
  private int resultSetType;
  private boolean poolable;

  public static List<StringProp> getInfo(Statement stmt) {
    Assert.notNull(stmt);
    List<StringProp> lst = new ArrayList<StringProp>();

    int z;

    try {
      z = stmt.getFetchDirection();
      PropUtils.addString(lst, "Fetch Direction",
          BeeUtils.concat(1, z, JdbcUtils.fetchDirectionAsString(z)));

      PropUtils.addString(lst, "Fetch Size", stmt.getFetchSize());

      z = stmt.getResultSetType();
      PropUtils.addString(lst, "Result Set Type",
          BeeUtils.concat(1, z, JdbcUtils.rsTypeAsString(z)));

      z = stmt.getResultSetConcurrency();
      PropUtils.addString(lst, "Concurrency",
          BeeUtils.concat(1, z, JdbcUtils.concurrencyAsString(z)));

      z = stmt.getResultSetHoldability();
      PropUtils.addString(lst, "Holdability",
          BeeUtils.concat(1, z, JdbcUtils.holdabilityAsString(z)));

      PropUtils.addString(lst, "Max Field Size", stmt.getMaxFieldSize(),
          "Max Rows", stmt.getMaxRows(), "Query Timeout",
          stmt.getQueryTimeout(), "Closed", stmt.isClosed(), "Poolable",
          stmt.isPoolable());

      SQLWarning warn = stmt.getWarnings();
      if (warn != null) {
        List<String> wLst = JdbcUtils.unchain(warn);
        for (String w : wLst)
          PropUtils.addString(lst, "Warning", w);
      }
    } catch (SQLException ex) {
      LogUtils.warning(logger, ex);
    }

    return lst;
  }

  protected BeeStatement() {
    super();
  }

  public BeeStatement(Statement stmt) {
    setStatementInfo(stmt);
  }

  public void setStatementInfo(Statement stmt) {
    Assert.notNull(stmt);

    try {
      fetchDirection = stmt.getFetchDirection();
      fetchSize = stmt.getFetchSize();
      maxFieldSize = stmt.getMaxFieldSize();
      maxRows = stmt.getMaxRows();
      queryTimeout = stmt.getQueryTimeout();
      concurrency = stmt.getResultSetConcurrency();
      holdability = stmt.getResultSetHoldability();
      resultSetType = stmt.getResultSetType();
      poolable = stmt.isPoolable();

      state = BeeConst.STATE_INITIALIZED;
    } catch (SQLException ex) {
      handleError(ex);
    }
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public List<SQLException> getErrors() {
    return errors;
  }

  public void setErrors(List<SQLException> errors) {
    this.errors = errors;
  }

  public int getFetchDirection() {
    return fetchDirection;
  }

  public void setFetchDirection(int fetchDirection) {
    this.fetchDirection = fetchDirection;
  }

  public int getFetchSize() {
    return fetchSize;
  }

  public void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  public int getMaxFieldSize() {
    return maxFieldSize;
  }

  public void setMaxFieldSize(int maxFieldSize) {
    this.maxFieldSize = maxFieldSize;
  }

  public int getMaxRows() {
    return maxRows;
  }

  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }

  public int getQueryTimeout() {
    return queryTimeout;
  }

  public void setQueryTimeout(int queryTimeout) {
    this.queryTimeout = queryTimeout;
  }

  public int getConcurrency() {
    return concurrency;
  }

  public void setConcurrency(int concurrency) {
    this.concurrency = concurrency;
  }

  public int getHoldability() {
    return holdability;
  }

  public void setHoldability(int holdability) {
    this.holdability = holdability;
  }

  public int getResultSetType() {
    return resultSetType;
  }

  public void setResultSetType(int resultSetType) {
    this.resultSetType = resultSetType;
  }

  public boolean isPoolable() {
    return poolable;
  }

  public void setPoolable(boolean poolable) {
    this.poolable = poolable;
  }

  public boolean isModified(Statement stmt) {
    Assert.notNull(stmt);
    Assert.state(validState());

    if (!checkConcurrency(stmt))
      return true;
    if (!checkFetchDirection(stmt))
      return true;
    if (!checkFetchSize(stmt))
      return true;
    if (!checkHoldability(stmt))
      return true;
    if (!checkMaxFieldSize(stmt))
      return true;
    if (!checkMaxRows(stmt))
      return true;
    if (!checkPoolable(stmt))
      return true;
    if (!checkQueryTimeout(stmt))
      return true;
    if (!checkResultSetType(stmt))
      return true;

    return false;
  }

  public void revert(Statement stmt) {
    Assert.notNull(stmt);
    Assert.state(validState());

    if (!hasState(BeeConst.STATE_CHANGED))
      return;

    try {
      if (stmt.getFetchDirection() != getFetchDirection())
        stmt.setFetchDirection(getFetchDirection());
      if (stmt.getFetchSize() != getFetchSize())
        stmt.setFetchSize(getFetchSize());
      if (stmt.getMaxFieldSize() != getMaxFieldSize())
        stmt.setMaxFieldSize(getMaxFieldSize());
      if (stmt.getMaxRows() != getMaxRows())
        stmt.setMaxRows(getMaxRows());
      if (stmt.getQueryTimeout() != getQueryTimeout())
        stmt.setQueryTimeout(getQueryTimeout());
      if (stmt.isPoolable() != isPoolable())
        stmt.setPoolable(isPoolable());
    } catch (SQLException ex) {
      handleError(ex);
    }
  }

  public boolean updateFetchDirection(Statement stmt, String s) {
    Assert.notNull(stmt);
    Assert.notEmpty(s);
    Assert.state(validState());

    if (BeeConst.isDefault(s))
      return true;
    int direction = JdbcUtils.fetchDirectionFromString(s);
    if (!JdbcUtils.validFetchDirection(direction)) {
      LogUtils.warning(logger, "unrecognized fetch direction", s);
      return false;
    }

    return updateFetchDirection(stmt, direction);
  }

  public boolean updateFetchSize(Statement stmt, int size) {
    Assert.notNull(stmt);
    Assert.state(validState());

    if (size == getFetchSize())
      return true;
    boolean ok;

    try {
      stmt.setFetchSize(size);
      int z = stmt.getFetchSize();
      if (z == size) {
        addState(BeeConst.STATE_CHANGED);
        ok = true;
      } else {
        LogUtils.warning(logger, "fetch size not updated:", "expected", size,
            "getFetchSize", z);
        ok = false;
      }
    } catch (SQLException ex) {
      if (size < 0) {
        LogUtils.warning(logger, ex, "Fetch Size:", size);
      } else {
        handleError(ex);
      }
      ok = false;
    }

    return ok;
  }

  public boolean updateMaxFieldSize(Statement stmt, int size) {
    Assert.notNull(stmt);
    Assert.state(validState());

    if (size == getMaxFieldSize())
      return true;
    boolean ok;

    try {
      stmt.setMaxFieldSize(size);
      int z = stmt.getMaxFieldSize();
      if (z == size) {
        addState(BeeConst.STATE_CHANGED);
        ok = true;
      } else {
        LogUtils.warning(logger, "max field size not updated:", "expected",
            size, "getMaxFieldSize", z);
        ok = false;
      }
    } catch (SQLException ex) {
      if (size < 0) {
        LogUtils.warning(logger, ex, "Max Field Size:", size);
      } else {
        handleError(ex);
      }
      ok = false;
    }

    return ok;
  }

  public boolean updateMaxRows(Statement stmt, int rows) {
    Assert.notNull(stmt);
    Assert.state(validState());

    if (rows == getMaxRows())
      return true;
    boolean ok;

    try {
      stmt.setMaxRows(rows);
      int z = stmt.getMaxRows();
      if (z == rows) {
        addState(BeeConst.STATE_CHANGED);
        ok = true;
      } else {
        LogUtils.warning(logger, "max rows not updated:", "expected", rows,
            "getMaxRows", z);
        ok = false;
      }
    } catch (SQLException ex) {
      if (rows < 0) {
        LogUtils.warning(logger, ex, "Max Rows:", rows);
      } else {
        handleError(ex);
      }
      ok = false;
    }

    return ok;
  }

  public boolean updateQueryTimeout(Statement stmt, int timeout) {
    Assert.notNull(stmt);
    Assert.state(validState());

    if (timeout == getQueryTimeout())
      return true;
    boolean ok;

    try {
      stmt.setQueryTimeout(timeout);
      int z = stmt.getQueryTimeout();
      if (z == timeout) {
        addState(BeeConst.STATE_CHANGED);
        ok = true;
      } else {
        LogUtils.warning(logger, "query timeout not updated:", "expected",
            timeout, "getQueryTimeout", z);
        ok = false;
      }
    } catch (SQLException ex) {
      if (timeout < 0) {
        LogUtils.warning(logger, ex, "Query Timeout:", timeout);
      } else {
        handleError(ex);
      }
      ok = false;
    }

    return ok;
  }

  public boolean updatePoolable(Statement stmt, String s) {
    Assert.notNull(stmt);
    Assert.notEmpty(s);
    Assert.state(validState());

    if (BeeConst.isDefault(s))
      return true;
    if (!BeeUtils.isBoolean(s)) {
      LogUtils.warning(logger, "unrecognized poolable value", s);
      return false;
    }

    return updatePoolable(stmt, BeeUtils.toBoolean(s));
  }

  public boolean setCursorName(Statement stmt, String cn) {
    Assert.notNull(stmt);
    Assert.notEmpty(cn);
    boolean ok;

    try {
      stmt.setCursorName(cn);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  public boolean setEscapeProcessing(Statement stmt, boolean enable) {
    Assert.notNull(stmt);
    boolean ok;

    try {
      stmt.setEscapeProcessing(enable);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  public int getFetchDirectionQuietly(Statement stmt) {
    if (stmt == null) {
      noStatement();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = stmt.getFetchDirection();
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getFetchSizeQuietly(Statement stmt) {
    if (stmt == null) {
      noStatement();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = stmt.getFetchSize();
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getMaxFieldSizeQuietly(Statement stmt) {
    if (stmt == null) {
      noStatement();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = stmt.getMaxFieldSize();
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getMaxRowsQuietly(Statement stmt) {
    if (stmt == null) {
      noStatement();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = stmt.getMaxRows();
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getQueryTimeoutQuietly(Statement stmt) {
    if (stmt == null) {
      noStatement();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = stmt.getQueryTimeout();
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getPoolableQuietly(Statement stmt) {
    if (stmt == null) {
      noStatement();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = BeeUtils.toInt(stmt.isPoolable());
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public ResultSet executeQuery(Statement stmt, String sql) {
    Assert.notNull(stmt);
    Assert.notEmpty(sql);
    ResultSet rs;

    try {
      rs = stmt.executeQuery(sql);
    } catch (SQLException ex) {
      handleError(ex);
      rs = null;
    }

    return rs;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  private boolean updateFetchDirection(Statement stmt, int direction) {
    if (direction == getFetchDirection())
      return true;
    boolean ok;

    try {
      stmt.setFetchDirection(direction);
      addState(BeeConst.STATE_CHANGED);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  private boolean updatePoolable(Statement stmt, boolean pool) {
    if (pool == isPoolable())
      return true;
    boolean ok;

    try {
      stmt.setPoolable(pool);
      addState(BeeConst.STATE_CHANGED);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  private boolean checkFetchDirection(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getFetchDirection() == getFetchDirection());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkFetchSize(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getFetchSize() == getFetchSize());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkMaxFieldSize(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getMaxFieldSize() == getMaxFieldSize());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkMaxRows(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getMaxRows() == getMaxRows());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkQueryTimeout(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getQueryTimeout() == getQueryTimeout());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkHoldability(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getResultSetHoldability() == getHoldability());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkConcurrency(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getResultSetConcurrency() == getConcurrency());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkResultSetType(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.getResultSetType() == getResultSetType());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkPoolable(Statement stmt) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (stmt.isPoolable() == isPoolable());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private void handleError(SQLException ex) {
    errors.add(ex);
    LogUtils.error(logger, ex);
    addState(BeeConst.STATE_ERROR);
  }

  private void noStatement() {
    handleError(new SQLException("statement not available"));
  }

  private void addState(int st) {
    this.state |= st;
  }

  private boolean hasState(int st) {
    return (state & st) != 0;
  }

  private boolean validState() {
    return hasState(BeeConst.STATE_INITIALIZED)
        && !hasState(BeeConst.STATE_ERROR);
  }

}
