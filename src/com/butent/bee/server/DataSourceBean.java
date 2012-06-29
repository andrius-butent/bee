package com.butent.bee.server;

import com.google.common.collect.Lists;

import com.butent.bee.server.utils.BeeDataSource;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Manages the data source (DSN) that the system is using.
 */

@Singleton
@Startup
@Lock(LockType.READ)
public class DataSourceBean {

  private static final String PROPERTY_DSN = "DataSourceName";
  private static Logger logger = Logger.getLogger(DataSourceBean.class.getName());

  private List<BeeDataSource> bds = new ArrayList<BeeDataSource>();
  private int defaultDataSourceIndex = -1;

  @PreDestroy
  public void destroy() {
    if (!bds.isEmpty()) {
      for (BeeDataSource z : bds) {
        if (z.isOpen()) {
          try {
            z.close();
            LogUtils.info(logger, "closed", z.getDsn());
          } catch (Exception ex) {
            LogUtils.warning(logger, ex);
          }
        }
      }
    }
  }

  public BeeDataSource getDefaultDs() {
    if (defaultDataSourceIndex >= 0) {
      return bds.get(defaultDataSourceIndex);
    } else {
      return null;
    }
  }

  public String getDefaultDsn() {
    BeeDataSource ds = getDefaultDs();

    if (ds == null) {
      return null;
    }
    return ds.getDsn();
  }

  public List<String> getDsns() {
    List<String> dsns = Lists.newArrayList();

    if (!bds.isEmpty()) {
      for (BeeDataSource z : bds) {
        dsns.add(z.getDsn());
      }
    }
    return dsns;
  }

  public BeeDataSource locateDs(String dsn) {
    Assert.notEmpty(dsn);
    BeeDataSource z = null;

    for (BeeDataSource k : bds) {
      if (dsn.equals(k.getDsn())) {
        z = k;
        break;
      }
    }

    if (z == null) {
      LogUtils.warning(logger, "dsn", dsn, "not found");
    }
    return z;
  }

  @PostConstruct
  private void init() {
    String dsn = Config.getProperty(PROPERTY_DSN);
    if (BeeUtils.isEmpty(dsn)) {
      LogUtils.severe(logger, "property", PROPERTY_DSN, "not found");
      return;
    }

    String[] arr = dsn.split(",");
    char defChar = '*';

    String nm;
    DataSource ds;
    boolean ok;
    boolean isDef;

    for (String z : arr) {
      nm = z.trim();
      isDef = BeeUtils.isPrefixOrSuffix(nm, defChar);
      if (isDef) {
        nm = BeeUtils.removePrefixAndSuffix(nm, defChar);
      }
      try {
        ds = (DataSource) InitialContext.doLookup("jdbc/" + nm);
        ok = true;
      } catch (NamingException ex) {
        try {
          ds = (DataSource) InitialContext.doLookup("java:jdbc/" + nm);
          ok = true;
        } catch (NamingException ex2) {
          LogUtils.error(logger, ex);
          ds = null;
          ok = false;
        }
      }

      if (ok) {
        bds.add(new BeeDataSource(nm, ds));
        if (isDef) {
          defaultDataSourceIndex = bds.size() - 1;

        } else if (defaultDataSourceIndex < 0) {
          defaultDataSourceIndex = 0;
        }
      }
    }
  }
}
