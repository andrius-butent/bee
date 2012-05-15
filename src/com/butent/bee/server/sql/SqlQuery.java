package com.butent.bee.server.sql;

import com.butent.bee.shared.Assert;

/**
 * Is an abstract class for SQL queries forming classes and indicates to use SQL builder classes.
 * 
 * @param <T> used for reference getting.
 */

abstract class SqlQuery<T> implements IsQuery {

  /**
   * @return a query using a currently set builder.
   */
  @Override
  public String getQuery() {
    return getSqlString(SqlBuilderFactory.getBuilder());
  }

  /**
   * @return a query using the specified builder {@code builder}
   */
  @Override
  public String getSqlString(SqlBuilder builder) {
    Assert.notEmpty(builder);
    return builder.getQuery(this);
  }

  public abstract T reset();

  @SuppressWarnings("unchecked")
  protected T getReference() {
    return (T) this;
  }
}
