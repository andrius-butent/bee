package com.butent.bee.shared.testutils;

import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlBuilder;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst.SqlEngine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.SqlUpdate}.
 */
public class TestSqlUpdate {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testGetSources() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlUpdate update2 = new SqlUpdate("Source1", "alias1");
    IsCondition where = SqlUtils.equal("Source2", "name", "John");
    update2.addConstant("name", "Petras");
    update2.setWhere(where);
    update2.addFrom("Source5");
    update2.addFrom("Source4");
    update2.addFrom("Source3");

    Object[] arr = update2.getSources().toArray();
    Object[] expected = {"Source5", "Source4", "Source3", "Source1"};

    assertArrayEquals(expected, arr);
  }

  @Test
  public final void testGetSqlString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlUpdate update = new SqlUpdate("Source1");
    update.addConstant("field1", "value1");
    update.addConstant("field2", "value2");
    assertEquals("UPDATE Source1 SET field1='value1', field2='value2'", update.getSqlString(
        builder));

    update.reset();
    IsExpression expr = SqlUtils.field("Sourceexpr", "fieldexpr");
    update.addExpression("field1", expr);

    assertEquals("UPDATE Source1 SET field1=Sourceexpr.fieldexpr", update.getSqlString(builder));

    SqlUpdate update2 = new SqlUpdate("Source1", "alias1");
    IsCondition where = SqlUtils.equal("Source1", "name", "John");
    update2.addConstant("name", "Petras");
    update2.setWhere(where);

    assertEquals("UPDATE Source1 alias1 SET name='Petras' WHERE Source1.name = 'John'", update2
        .getSqlString(builder));
  }

  @Test
  public final void testIsEmpty() {
    SqlUpdate update = new SqlUpdate("target", "trg");
    assertTrue(update.isEmpty()); // nes neturi BeeUtils nepalaiko isFrom, bet tikrina ar jis nera
                                  // NULL ;

    update.addExpression("Field1", SqlUtils.constant("hello"));

    assertFalse(update.isEmpty());

    update = new SqlUpdate("Table1");
    assertTrue(update.isEmpty());
  }
}
