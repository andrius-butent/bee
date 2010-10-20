package com.butent.bee.egg.server.ui;

import com.butent.bee.egg.server.DataSourceBean;
import com.butent.bee.egg.shared.BeeConst;
import com.butent.bee.egg.shared.sql.QueryBuilder;
import com.butent.bee.egg.shared.utils.BeeUtils;
import com.butent.bee.egg.shared.utils.LogUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class QueryServiceBean {

  private static Logger logger = Logger.getLogger(QueryServiceBean.class.getName());

  // @PersistenceContext
  // EntityManager em;
  DataSource ds = null;
  @EJB
  DataSourceBean dsb;

  public List<Object[]> getQueryData(QueryBuilder qb) {
    if (!BeeUtils.isEmpty(qb)) {
      return processSQL(qb.getQuery());
    }
    return null;
  }

  public List<Object[]> processSQL(String sql) {
    LogUtils.info(logger, sql);

    if (ds == null) {
      ds = dsb.locateDs(BeeConst.MYSQL).getDs();
    }
    Connection con = null;
    Statement stmt = null;
    List<Object[]> result = new ArrayList<Object[]>();
    try {
      con = ds.getConnection();
      stmt = con.createStatement();

      ResultSet rs = stmt.executeQuery(sql);
      int cols = rs.getMetaData().getColumnCount();

      while (rs.next()) {
        Object[] o = new Object[cols];

        for (int i = 0; i < cols; i++) {
          o[i] = rs.getObject(i + 1);
        }
        result.add(o);
      }
      LogUtils.info(logger, result.size(), sql);
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException("Cannot perform query: " + ex, ex);
    } finally {
      try {
        stmt.close();
        con.close();
        con = null;
        stmt = null;
      } catch (SQLException ex) {
        throw new RuntimeException("Cannot close connection: " + ex, ex);
      }
    }
  }
}