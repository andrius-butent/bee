package com.butent.bee.server.data;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

import com.butent.bee.server.Config;
import com.butent.bee.server.DataSourceBean;
import com.butent.bee.server.data.BeeTable.BeeCheck;
import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeForeignKey;
import com.butent.bee.server.data.BeeTable.BeeIndex;
import com.butent.bee.server.data.BeeTable.BeeRelation;
import com.butent.bee.server.data.BeeTable.BeeTrigger;
import com.butent.bee.server.data.BeeTable.BeeUniqueKey;
import com.butent.bee.server.io.FileNameUtils;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.sql.HasFrom;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.Defaults.DefaultExpression;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerEvent;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerScope;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerTiming;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerType;
import com.butent.bee.shared.data.XmlTable;
import com.butent.bee.shared.data.XmlTable.XmlCheck;
import com.butent.bee.shared.data.XmlTable.XmlConstraint;
import com.butent.bee.shared.data.XmlTable.XmlField;
import com.butent.bee.shared.data.XmlTable.XmlIndex;
import com.butent.bee.shared.data.XmlTable.XmlReference;
import com.butent.bee.shared.data.XmlTable.XmlTrigger;
import com.butent.bee.shared.data.XmlTable.XmlUnique;
import com.butent.bee.shared.data.XmlView;
import com.butent.bee.shared.data.XmlView.XmlColumn;
import com.butent.bee.shared.data.XmlView.XmlSimpleColumn;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.modules.commons.CommonsConstants.RightsState;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Ensures core data management functionality containing: data structures for tables and views,
 * current SQL server configuration, creating data tables only when they are in demand, handles data
 * with exceptions etc.
 */

@Singleton
@Lock(LockType.READ)
public class SystemBean {

  /**
   * Contains a list of system objects, like state or table.
   */

  public enum SysObject {
    TABLE("tables"), VIEW("views");

    private final String path;

    private SysObject(String path) {
      this.path = path;
    }

    public String getFileName(String objName) {
      Assert.notEmpty(objName);
      return BeeUtils.join(".", objName, name().toLowerCase(), XmlUtils.DEFAULT_XML_EXTENSION);
    }

    public String getPath() {
      return path;
    }

    public String getSchemaPath() {
      return Config.getSchemaPath(name().toLowerCase() + ".xsd");
    }
  }

  public static final String AUDIT_PREFIX = "AUDIT";
  public static final String AUDIT_USER = "bee.user";
  public static final String AUDIT_FLD_TIME = "Time";
  public static final String AUDIT_FLD_USER = "UserId";
  public static final String AUDIT_FLD_TX = "TransactionId";
  public static final String AUDIT_FLD_MODE = "Mode";
  public static final String AUDIT_FLD_ID = "RecordId";
  public static final String AUDIT_FLD_FIELD = "Field";
  public static final String AUDIT_FLD_VALUE = "Value";

  @EJB
  DataSourceBean dsb;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  ModuleHolderBean moduleBean;
  @EJB
  ParamHolderBean prm;

  private final BeeLogger logger = LogUtils.getLogger(getClass());

  private String dbName;
  private String dbSchema;
  private String dbAuditSchema;
  private final Map<String, BeeTable> tableCache = Maps.newHashMap();
  private final Map<String, BeeView> viewCache = Maps.newHashMap();
  private final EventBus viewEventBus = new EventBus();

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  @Lock(LockType.WRITE)
  public void activateTable(String tblName) {
    BeeTable table = getTable(tblName);

    if (!table.isActive()) {
      rebuildTable(table);
    }
  }

  public List<Property> checkTables(String... tbls) {
    List<Property> diff = Lists.newArrayList();
    Collection<String> tables;

    if (ArrayUtils.isEmpty(tbls)) {
      initTables();
      tables = getTableNames();
    } else {
      tables = Lists.newArrayList(tbls);
    }
    for (String tbl : tables) {
      createTable(getTable(tbl), diff);
    }
    return diff;
  }

  public void filterVisibleState(SqlSelect query, String tblName) {
    filterVisibleState(query, tblName, null);
  }

  public void filterVisibleState(SqlSelect query, String tblName, String tblAlias) {
    BeeTable table = getTable(tblName);

    table.verifyState(query, tblAlias, RightsState.VISIBLE, table.areRecordsVisible(),
        usr.getUserRoles(usr.getCurrentUserId()));
  }

  public List<DataInfo> getDataInfo() {
    SimpleRowSet dbTables = qs.dbTables(dbName, dbSchema, null);
    String[] tables = dbTables.getColumn(SqlConstants.TBL_NAME);

    List<DataInfo> lst = Lists.newArrayList();
    Set<String> viewNames = Sets.newHashSet(getViewNames());
    viewNames.addAll(getTableNames());

    for (String viewName : viewNames) {
      String sourceName = getView(viewName).getSourceName();
      DataInfo dataInfo = getDataInfo(viewName);

      for (int i = 0; i < tables.length; i++) {
        if (BeeUtils.same(tables[i], sourceName)) {
          dataInfo.setRowCount(BeeUtils.unbox(dbTables.getInt(i, SqlConstants.ROW_COUNT)));
          break;
        }
      }
      lst.add(dataInfo);
    }
    return lst;
  }

  public DataInfo getDataInfo(String viewName) {
    BeeView view = getView(viewName);
    BeeTable source = getTable(view.getSourceName());

    List<BeeColumn> columns = null;
    List<ViewColumn> viewColumns = null;

    columns = Lists.newArrayList();

    for (String col : view.getColumnNames()) {
      BeeColumn column = new BeeColumn();
      view.initColumn(col, column);
      columns.add(column);
    }
    viewColumns = view.getViewColumns();

    return new DataInfo(viewName, source.getName(), source.getIdName(), source.getVersionName(),
        view.getCaption(), view.getEditForm(), view.getRowCaption(),
        view.getNewRowForm(), view.getNewRowColumns(), view.getNewRowCaption(),
        view.getCacheMaximumSize(), view.getCacheEviction(), columns, viewColumns);
  }

  public String getDbName() {
    return dbName;
  }

  public String getDbSchema() {
    return dbSchema;
  }

  public String getIdName(String tblName) {
    return getTable(tblName).getIdName();
  }

  public BeeTable getTable(String tblName) {
    Assert.state(isTable(tblName), "Not a base table: " + tblName);
    return tableCache.get(BeeUtils.normalize(tblName));
  }

  public Map<String, Pair<DefaultExpression, Object>> getTableDefaults(String tblName) {
    return getTable(tblName).getDefaults();
  }

  public Collection<BeeField> getTableFields(String tblName) {
    return getTable(tblName).getFields();
  }

  public List<ExtendedProperty> getTableInfo(String tblName) {
    return getTable(tblName).getExtendedInfo();
  }

  public Collection<String> getTableNames() {
    Collection<String> tables = Lists.newArrayList();

    for (BeeTable table : getTables()) {
      tables.add(table.getName());
    }
    return tables;
  }

  public String getVersionName(String tblName) {
    return getTable(tblName).getVersionName();
  }

  public BeeView getView(String viewName) {
    Assert.state(isView(viewName), "Not a view: " + viewName);
    BeeView view = viewCache.get(BeeUtils.normalize(viewName));

    if (view == null) {
      view = getDefaultView(viewName);
      register(view, viewCache);
    }
    return view;
  }

  public BeeView.ViewFinder getViewFinder() {
    return new BeeView.ViewFinder() {
      @Override
      public BeeView apply(String input) {
        return viewCache.get(BeeUtils.normalize(input));
      }
    };
  }

  public Collection<String> getViewNames() {
    Collection<String> views = Lists.newArrayList();

    for (BeeView view : viewCache.values()) {
      views.add(view.getName());
    }
    return views;
  }

  public String getViewSource(String viewName) {
    return getView(viewName).getSourceName();
  }

  public XmlTable getXmlTable(String moduleName, String tableName) {
    Assert.notEmpty(tableName);

    XmlTable xmlTable = getXmlTable(moduleName, tableName, false);
    XmlTable userTable = getXmlTable(moduleName, tableName, true);

    if (xmlTable == null) {
      xmlTable = userTable;
    } else {
      xmlTable.protect().merge(userTable);
    }
    return xmlTable;
  }

  public XmlTable getXmlTable(String moduleName, String tableName, boolean userMode) {
    Assert.notEmpty(tableName);
    String resource = moduleBean.getResourcePath(moduleName,
        SysObject.TABLE.getPath(), SysObject.TABLE.getFileName(tableName));

    if (userMode) {
      resource = Config.getLocalPath(resource);
    } else {
      resource = Config.getConfigPath(resource);
    }
    return loadXmlTable(resource);
  }

  public XmlView getXmlView(String moduleName, String viewName) {
    Assert.notEmpty(viewName);

    XmlView xmlView = getXmlView(moduleName, viewName, false);
    XmlView userView = getXmlView(moduleName, viewName, true);

    if (userView != null) {
      xmlView = userView;
    }
    return xmlView;
  }

  public XmlView getXmlView(String moduleName, String viewName, boolean userMode) {
    Assert.notEmpty(viewName);
    String resource = moduleBean.getResourcePath(moduleName,
        SysObject.VIEW.getPath(), SysObject.VIEW.getFileName(viewName));

    if (userMode) {
      resource = Config.getLocalPath(resource);
    } else {
      resource = Config.getConfigPath(resource);
    }
    return loadXmlView(resource);
  }

  public boolean hasField(String tblName, String fldName) {
    return getTable(tblName).hasField(fldName);
  }

  public IsCondition idEquals(String tblName, long id) {
    return SqlUtils.equals(tblName, getIdName(tblName), id);
  }

  @Lock(LockType.WRITE)
  public void initTables() {
    initTables(BeeUtils.notEmpty(SqlBuilderFactory.getDsn(), dsb.getDefaultDsn()));
  }

  @Lock(LockType.WRITE)
  public void initTables(String dsn) {
    Assert.state(SqlBuilderFactory.setDefaultBuilder(qs.dbEngine(dsn), dsn));
    initObjects(SysObject.TABLE);

    for (BeeTable table : getTables()) {
      for (BeeForeignKey fKey : table.getForeignKeys()) {
        Assert.state(isTable(fKey.getRefTable()),
            BeeUtils.joinWords(
                "Unknown field", BeeUtils.bracket(table.getName() + "." + fKey.getFields()),
                "relation:", BeeUtils.bracket(fKey.getRefTable())));

        if (!BeeUtils.isEmpty(fKey.getRefFields())) {
          BeeTable refTable = getTable(fKey.getRefTable());

          for (String fld : fKey.getRefFields()) {
            Assert.state(refTable.hasField(fld),
                BeeUtils.joinWords("Unrecognized foreign key field:", refTable.getName(), fld));
          }
        }
      }
    }
    initDatabase();
    initDbTriggers();
    initViews();
  }

  @Lock(LockType.WRITE)
  public void initViews() {
    initObjects(SysObject.VIEW);
  }

  public boolean isExtField(String tblName, String fldName) {
    return getTable(tblName).getField(fldName).isExtended();
  }

  public boolean isTable(String tblName) {
    return !BeeUtils.isEmpty(tblName) && tableCache.containsKey(BeeUtils.normalize(tblName));
  }

  public boolean isView(String viewName) {
    return !BeeUtils.isEmpty(viewName)
        && (viewCache.containsKey(BeeUtils.normalize(viewName)) || isTable(viewName));
  }

  public String joinExtField(HasFrom<?> query, String tblName, String tblAlias, String fldName) {
    Assert.notNull(query);
    BeeTable table = getTable(tblName);
    BeeField field = table.getField(fldName);

    if (!field.isExtended()) {
      logger.warning("Field is not extended:", tblName, fldName);
      return null;
    }
    return table.joinExtField(query, tblAlias, field);
  }

  public IsCondition joinTables(String tblName, String dstTable, String dstField) {
    return joinTables(tblName, null, dstTable, dstField);
  }

  public IsCondition joinTables(String tblName, String tblAlias, String dstTable, String dstField) {
    return SqlUtils.join(BeeUtils.notEmpty(tblAlias, tblName), getIdName(tblName),
        dstTable, dstField);
  }

  public String joinTranslationField(HasFrom<?> query, String tblName, String tblAlias,
      String fldName, String locale) {
    Assert.notNull(query);
    BeeTable table = getTable(tblName);
    BeeField field = table.getField(fldName);

    return table.joinTranslationField(query, tblAlias, field, locale);
  }

  public XmlTable loadXmlTable(String resource) {
    return XmlUtils.unmarshal(XmlTable.class, resource, SysObject.TABLE.getSchemaPath());
  }

  public XmlView loadXmlView(String resource) {
    return XmlUtils.unmarshal(XmlView.class, resource, SysObject.VIEW.getSchemaPath());
  }

  public void postViewEvent(ViewEvent viewEvent) {
    viewEventBus.post(viewEvent);
  }

  @Lock(LockType.WRITE)
  public void rebuildActiveTables() {
    initTables();

    for (BeeTable table : getTables()) {
      if (table.isActive()) {
        rebuildTable(table);
      }
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  @Lock(LockType.WRITE)
  public void rebuildTable(String tblName) {
    rebuildTable(getTable(tblName));
  }

  @Lock(LockType.WRITE)
  public void registerViewEventHandler(ViewEventHandler eventHandler) {
    viewEventBus.register(eventHandler);
  }

  private void createAuditTables(BeeTable table) {
    if (!table.isAuditable()) {
      return;
    }
    if (!qs.dbSchemaExists(dbName, dbAuditSchema)) {
      makeStructureChanges(SqlUtils.createSchema(dbAuditSchema));
    }
    String auditName = BeeUtils.join("_", table.getName(), AUDIT_PREFIX);
    String auditPath = BeeUtils.join(".", dbAuditSchema, auditName);

    if (!qs.dbTableExists(dbName, dbAuditSchema, auditName)) {
      makeStructureChanges(
          new SqlCreate(auditPath, false)
              .addDateTime(AUDIT_FLD_TIME, true)
              .addLong(AUDIT_FLD_USER, false)
              .addLong(AUDIT_FLD_TX, false)
              .addString(AUDIT_FLD_MODE, 1, true)
              .addLong(AUDIT_FLD_ID, true)
              .addString(AUDIT_FLD_FIELD, 30, false)
              .addText(AUDIT_FLD_VALUE, false),
          SqlUtils.createIndex(auditPath, "IK_" + Codec.crc32(auditName + AUDIT_FLD_ID),
              Lists.newArrayList(AUDIT_FLD_ID), false));
    }
  }

  private void createChecks(Collection<BeeCheck> checks) {
    for (BeeCheck check : checks) {
      makeStructureChanges(SqlUtils.createCheck(check.getTable(), check.getName(),
          check.getExpression()));
    }
  }

  private void createForeignKeys(Collection<BeeForeignKey> fKeys) {
    HashMultimap<String, String> flds = HashMultimap.create();

    for (SimpleRow row : qs.dbFields(getDbName(), getDbSchema(), null)) {
      flds.put(row.getValue(SqlConstants.TBL_NAME), row.getValue(SqlConstants.FLD_NAME));
    }
    for (BeeForeignKey fKey : fKeys) {
      String tblName = fKey.getTable();
      List<String> fields = fKey.getFields();
      String refTblName = fKey.getRefTable();
      List<String> refFields = fKey.getRefFields();

      boolean ok = true;

      for (String fldName : fields) {
        if (!flds.containsEntry(tblName, fldName)) {
          ok = false;
          break;
        }
      }
      if (ok) {
        if (!BeeUtils.isEmpty(refFields)) {
          for (String fldName : refFields) {
            if (!flds.containsEntry(refTblName, fldName)) {
              ok = false;
              break;
            }
          }
        } else {
          refFields = Lists.newArrayList(getIdName(refTblName));
        }
      }
      if (ok) {
        makeStructureChanges(SqlUtils.createForeignKey(tblName, fKey.getName(),
            fields, refTblName, refFields, fKey.getCascade()));
      }
    }
  }

  private void createIndexes(Collection<BeeIndex> indexes) {
    for (BeeIndex index : indexes) {
      makeStructureChanges(SqlUtils.createIndex(index.getTable(), index.getName(),
          index.getFields(), index.isUnique()));
    }
  }

  private Map<String, String> createTable(BeeTable table, List<Property> diff) {
    String tblName = table.getName();
    Map<String, SqlCreate> newTables = Maps.newHashMap();

    newTables.put(tblName, new SqlCreate(tblName, false)
        .addLong(table.getIdName(), true)
        .addLong(table.getVersionName(), true));

    for (BeeField field : table.getFields()) {
      tblName = field.getStorageTable();

      if (field.isExtended()) {
        SqlCreate sc = table.createExtTable(newTables.get(tblName), field);

        if (sc != null) {
          newTables.put(tblName, sc);
        }
      } else {
        newTables.get(tblName)
            .addField(field.getName(), field.getType(), field.getPrecision(), field.getScale(),
                field.isNotNull());
      }
      if (field.isTranslatable()) {
        tblName = table.getTranslationTable(field);
        SqlCreate sc = table.createTranslationTable(newTables.get(tblName), field);

        if (sc != null) {
          newTables.put(tblName, sc);
        }
      }
    }
    for (RightsState state : table.getStates()) {
      tblName = table.getStateTable(state);
      SqlCreate sc = table.createStateTable(newTables.get(tblName), state);

      if (sc != null) {
        newTables.put(tblName, sc);
      }
    }
    Map<String, String> rebuilds = Maps.newHashMap();

    for (SqlCreate sc : newTables.values()) {
      tblName = sc.getTarget();
      String tblBackup = null;
      boolean update = !qs.dbTableExists(getDbName(), getDbSchema(), tblName);

      if (update) {
        if (diff != null) {
          PropertyUtils.addProperty(diff, tblName, "DOES NOT EXIST");
          return null;
        } else {
          makeStructureChanges(sc);
        }
      } else {
        tblBackup = tblName + "_BAK";
        logger.debug("Checking indexes...");
        int c = 0;
        Set<String> indexes = Sets.newHashSet();

        for (String index : qs.dbIndexes(getDbName(), getDbSchema(), tblName)
            .getColumn(SqlConstants.KEY_NAME)) {

          if (index.startsWith(BeeTable.UNIQUE_INDEX_PREFIX)
              || index.startsWith(BeeTable.INDEX_KEY_PREFIX)) {
            indexes.add(index);
          }
        }
        for (BeeIndex index : table.getIndexes()) {
          if (BeeUtils.same(index.getTable(), tblName)) {
            if (indexes.contains(index.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("INDEX", index.getName(), index.getFields(),
                  "NOT IN", indexes);
              logger.warning(msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && indexes.size() > c) {
          String msg = "TOO MANY INDEXES";
          logger.warning(msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking unique keys...");
        int c = 0;
        Set<String> keys = Sets.newHashSet();

        for (String key : qs.dbConstraints(getDbName(), getDbSchema(), tblName,
            SqlKeyword.UNIQUE, SqlKeyword.PRIMARY_KEY).getColumn(SqlConstants.KEY_NAME)) {

          if (key.startsWith(BeeTable.UNIQUE_KEY_PREFIX)
              || key.startsWith(BeeTable.PRIMARY_KEY_PREFIX)) {
            keys.add(key);
          }
        }
        for (BeeUniqueKey key : table.getUniqueKeys()) {
          if (BeeUtils.same(key.getTable(), tblName)) {
            if (keys.contains(key.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("KEY", key.getName(), key.getFields(), "NOT IN",
                  keys);
              logger.warning(msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && keys.size() > c) {
          String msg = "TOO MANY UNIQUE KEYS";
          logger.warning(msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking foreign keys...");
        int c = 0;
        Set<String> fKeys = Sets.newHashSet();

        for (String fKey : qs.dbConstraints(getDbName(), getDbSchema(), tblName,
            SqlKeyword.FOREIGN_KEY).getColumn(SqlConstants.KEY_NAME)) {

          if (fKey.startsWith(BeeTable.FOREIGN_KEY_PREFIX)) {
            fKeys.add(fKey);
          }
        }
        for (BeeForeignKey fKey : table.getForeignKeys()) {
          if (BeeUtils.same(fKey.getTable(), tblName)
              && (BeeUtils.same(fKey.getRefTable(), table.getName())
              || getTable(fKey.getRefTable()).isActive())) {

            if (fKeys.contains(fKey.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("FOREIGN KEY", fKey.getName(),
                  BeeUtils.parenthesize(BeeUtils.join(" ON DELETE ",
                      fKey.getFields() + "->" + fKey.getRefTable(), fKey.getCascade())),
                  "NOT IN", fKeys);
              logger.warning(msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && fKeys.size() > c) {
          String msg = "TOO MANY FOREIGN KEYS";
          logger.warning(msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking check constraints...");
        int c = 0;
        Set<String> checks = Sets.newHashSet();

        for (String check : qs.dbConstraints(getDbName(), getDbSchema(), tblName, SqlKeyword.CHECK)
            .getColumn(SqlConstants.KEY_NAME)) {

          if (check.startsWith(BeeTable.CHECK_PREFIX)) {
            checks.add(check);
          }
        }
        for (BeeCheck check : table.getChecks()) {
          if (BeeUtils.same(check.getTable(), tblName)) {
            if (checks.contains(check.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("CHECK", check.getName(), "NOT IN", checks);
              logger.warning(msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && checks.size() > c) {
          String msg = "TOO MANY CHECK CONSTRAINTS";
          logger.warning(msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update) {
        logger.debug("Checking triggers...");
        int c = 0;
        Set<String> triggers = Sets.newHashSet();

        for (String trigger : qs.dbTriggers(getDbName(), getDbSchema(), tblName)
            .getColumn(SqlConstants.TRIGGER_NAME)) {

          if (trigger.startsWith(BeeTable.TRIGGER_PREFIX)) {
            triggers.add(trigger);
          }
        }
        for (BeeTrigger trigger : table.getTriggers()) {
          if (BeeUtils.same(trigger.getTable(), tblName)) {
            if (triggers.contains(trigger.getName())) {
              c++;
            } else {
              String msg = BeeUtils.joinWords("TRIGGER", trigger.getName(), "NOT IN",
                  BeeUtils.parenthesize(triggers));
              logger.warning(msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
        }
        if (!update && triggers.size() > c) {
          String msg = "TOO MANY TRIGGERS";
          logger.warning(msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!update && table.isAuditable() && isTable(tblName)) {
        logger.debug("Checking audit tables...");
        String auditName = BeeUtils.join("_", tblName, AUDIT_PREFIX);

        if (!qs.dbTableExists(dbName, dbAuditSchema, auditName)) {
          String msg = BeeUtils.joinWords("AUDIT TABLE",
              BeeUtils.join(".", dbAuditSchema, auditName), "DOES NOT EXIST");
          logger.warning(msg);

          if (diff != null) {
            PropertyUtils.addProperty(diff, tblName, msg);
          } else {
            update = true;
          }
        }
      }
      if (!BeeUtils.isEmpty(tblBackup)) {
        if (qs.dbTableExists(getDbName(), getDbSchema(), tblBackup)) {
          makeStructureChanges(SqlUtils.dropTable(tblBackup));
        }
        makeStructureChanges(sc.setTarget(tblBackup));

        SimpleRowSet oldFields = qs.dbFields(getDbName(), getDbSchema(), tblName);
        SimpleRowSet newFields = qs.dbFields(getDbName(), getDbSchema(), tblBackup);

        if (!update) {
          logger.debug("Checking fields...");
          int c = 0;

          for (SimpleRow newFieldInfo : newFields) {
            SimpleRow oldFieldInfo = null;
            String fldName = newFieldInfo.getValue(SqlConstants.FLD_NAME);

            for (SimpleRow oldInfo : oldFields) {
              if (BeeUtils.same(oldInfo.getValue(SqlConstants.FLD_NAME), fldName)) {
                c++;
                oldFieldInfo = oldInfo;
                break;
              }
            }
            if (oldFieldInfo != null) {
              for (String info : oldFieldInfo.getColumnNames()) {
                if (!BeeUtils.same(info, SqlConstants.TBL_NAME)
                    && !Objects.equal(oldFieldInfo.getValue(info), newFieldInfo.getValue(info))) {

                  String msg = BeeUtils.joinWords("FIELD", fldName + ":",
                      info, oldFieldInfo.getValue(info), "!=", newFieldInfo.getValue(info));
                  logger.warning(msg);

                  if (diff != null) {
                    PropertyUtils.addProperty(diff, tblName, msg);
                  } else {
                    update = true;
                    break;
                  }
                }
              }
              if (update) {
                break;
              }
            } else {
              String msg = BeeUtils.joinWords("FIELD", fldName, "DOES NOT EXIST");
              logger.warning(msg);

              if (diff != null) {
                PropertyUtils.addProperty(diff, tblName, msg);
              } else {
                update = true;
                break;
              }
            }
          }
          if (!update && oldFields.getNumberOfRows() > c) {
            String msg = "TOO MANY FIELDS";
            logger.warning(msg);

            if (diff != null) {
              PropertyUtils.addProperty(diff, tblName, msg);
            } else {
              update = true;
            }
          }
        }
        if (update) {
          Map<String, String> updFlds = Maps.newLinkedHashMap();
          String[] oldList = oldFields.getColumn(SqlConstants.FLD_NAME);

          for (String newFld : newFields.getColumn(SqlConstants.FLD_NAME)) {
            for (String oldFld : oldList) {
              if (BeeUtils.same(newFld, oldFld)) {
                updFlds.put(newFld, oldFld);
              }
            }
          }
          if (!BeeUtils.isEmpty(updFlds)) {
            Object res = qs.doSql(new SqlInsert(tblBackup)
                .addFields(updFlds.keySet().toArray(new String[0]))
                .setDataSource(new SqlSelect()
                    .addFields(tblName, updFlds.values().toArray(new String[0]))
                    .addFrom(tblName))
                .getQuery());

            Assert.state(res instanceof Number, BeeUtils.join(": ", "Error inserting data", res));
          }
        } else {
          makeStructureChanges(SqlUtils.dropTable(tblBackup));
        }
      }
      if (update) {
        rebuilds.put(tblName, tblBackup);
      }
    }
    return rebuilds;
  }

  private void createTriggers(Collection<BeeTrigger> triggers) {
    for (BeeTrigger trigger : triggers) {
      makeStructureChanges(SqlUtils.createTrigger(trigger.getName(), trigger.getTable(),
          trigger.getType(), trigger.getParameters(), trigger.getTiming(), trigger.getEvents(),
          trigger.getScope()));
    }
  }

  private void createUniqueKeys(Collection<BeeUniqueKey> uniqueKeys) {
    for (BeeUniqueKey uniqueKey : uniqueKeys) {
      IsQuery query;

      if (uniqueKey.isPrimary()) {
        query = SqlUtils.createPrimaryKey(uniqueKey.getTable(), uniqueKey.getName(),
            uniqueKey.getFields());
      } else {
        query = SqlUtils.createUniqueKey(uniqueKey.getTable(), uniqueKey.getName(),
            uniqueKey.getFields());
      }
      makeStructureChanges(query);
    }
  }

  private BeeView getDefaultView(String tblName) {
    List<XmlColumn> columns = Lists.newArrayList();

    for (BeeField field : getTableFields(tblName)) {
      XmlColumn column = new XmlSimpleColumn();
      column.name = field.getName();
      columns.add(column);
    }
    XmlView xmlView = new XmlView();
    xmlView.name = tblName;
    xmlView.source = tblName;
    xmlView.columns = columns;

    return new BeeView(getTable(tblName).getModuleName(), xmlView, tableCache);
  }

  private Collection<BeeTable> getTables() {
    return ImmutableList.copyOf(tableCache.values());
  }

  @PostConstruct
  private void init() {
    initTables();
  }

  private void initDatabase() {
    dbName = qs.dbName();
    dbSchema = qs.dbSchema();
    dbAuditSchema = BeeUtils.join("_", dbSchema, AUDIT_PREFIX);

    String[] dbTables = qs.dbTables(dbName, dbSchema, null).getColumn(SqlConstants.TBL_NAME);
    Set<String> names = Sets.newHashSet();
    for (String name : dbTables) {
      names.add(BeeUtils.normalize(name));
    }

    for (BeeTable table : getTables()) {
      String tblName = table.getName();
      table.setActive(names.contains(BeeUtils.normalize(tblName)));

      Map<String, String[]> tableFields = Maps.newHashMap();

      for (RightsState state : table.getStates()) {
        tblName = table.getStateTable(state);

        if (names.contains(BeeUtils.normalize(tblName))) {
          if (!tableFields.containsKey(tblName)) {
            tableFields.put(tblName,
                qs.dbFields(getDbName(), getDbSchema(), tblName).getColumn(SqlConstants.FLD_NAME));
          }
          table.initState(state, Sets.newHashSet(tableFields.get(tblName)));
        }
      }
    }
  }

  private void initDbTriggers() {
    for (BeeTable table : getTables()) {
      Map<String, List<Map<String, String>>> tr = Maps.newHashMap();

      for (BeeField field : table.getFields()) {
        if (field instanceof BeeRelation && ((BeeRelation) field).isEditable()) {
          String tblName = field.getStorageTable();
          String relTable = ((BeeRelation) field).getRelation();

          List<Map<String, String>> entry = tr.get(tblName);

          if (BeeUtils.isEmpty(entry)) {
            entry = Lists.newArrayList();
            tr.put(tblName, entry);
          }
          entry.add(ImmutableMap.of("field", field.getName(),
              "relTable", relTable, "relField", getIdName(relTable)));
        }
      }
      for (String tblName : tr.keySet()) {
        table.addTrigger(tblName, SqlTriggerType.RELATION,
            ImmutableMap.of("fields", tr.get(tblName)),
            SqlTriggerTiming.AFTER, EnumSet.of(SqlTriggerEvent.DELETE), SqlTriggerScope.ROW);
      }

      if (table.isAuditable()) {
        HashMultimap<String, String> fields = HashMultimap.create();

        for (BeeField field : table.getFields()) {
          if (field.isAuditable()) {
            fields.put(field.getStorageTable(), field.getName());
          }
        }
        for (String tblName : fields.keySet()) {
          table.addTrigger(tblName, SqlTriggerType.AUDIT,
              ImmutableMap.of("auditSchema", dbAuditSchema,
                  "auditTable", BeeUtils.join("_", table.getName(), AUDIT_PREFIX),
                  "idName", table.getIdName(),
                  "fields", fields.get(tblName)),
              SqlTriggerTiming.AFTER,
              EnumSet.of(SqlTriggerEvent.INSERT, SqlTriggerEvent.UPDATE, SqlTriggerEvent.DELETE),
              SqlTriggerScope.ROW);
        }
      }
    }
  }

  private void initObjects(SysObject obj) {
    Assert.notNull(obj);

    switch (obj) {
      case TABLE:
        tableCache.clear();
        break;
      case VIEW:
        viewCache.clear();
        break;
    }
    int cnt = 0;
    Collection<File> roots = Lists.newArrayList();

    for (String moduleName : moduleBean.getModules()) {
      roots.clear();
      String modulePath = moduleBean.getResourcePath(moduleName, obj.getPath());

      File root = new File(Config.CONFIG_DIR, modulePath);
      if (FileUtils.isDirectory(root)) {
        roots.add(root);
      }
      root = new File(Config.LOCAL_DIR, modulePath);
      if (FileUtils.isDirectory(root)) {
        roots.add(root);
      }
      List<File> resources =
          FileUtils.findFiles(obj.getFileName("*"), roots, null, null, false, true);

      if (!BeeUtils.isEmpty(resources)) {
        Set<String> objects = Sets.newHashSet();

        for (File resource : resources) {
          String resourcePath = resource.getPath();
          String objectName = FileNameUtils.getBaseName(resourcePath);
          objectName = objectName.substring(0, objectName.length() - obj.name().length() - 1);
          objects.add(objectName);
        }
        for (String objectName : objects) {
          boolean isOk = false;

          switch (obj) {
            case TABLE:
              isOk = initTable(moduleName, objectName);
              break;
            case VIEW:
              isOk = initView(moduleName, objectName);
              break;
          }
          if (isOk) {
            cnt++;
          }
        }
      }
    }
    if (cnt <= 0) {
      logger.severe("No", obj.name(), "descriptions found");
    } else {
      logger.info("Loaded", cnt, obj.name(), "descriptions");
    }
  }

  private boolean initTable(String moduleName, String tableName) {
    Assert.notEmpty(tableName);
    BeeTable table = null;
    XmlTable xmlTable = getXmlTable(moduleName, tableName);

    if (xmlTable != null) {
      if (!BeeUtils.same(xmlTable.name, tableName)) {
        logger.warning("Table name doesn't match resource name:", xmlTable.name);
      } else {
        table = new BeeTable(moduleName, xmlTable, BeeUtils.isTrue(
            prm.getBoolean(CommonsConstants.COMMONS_MODULE, CommonsConstants.PRM_AUDIT_OFF)));
        String tbl = table.getName();

        for (int i = 0; i < 2; i++) {
          boolean extMode = (i > 0);
          Collection<XmlField> fields = extMode ? xmlTable.extFields : xmlTable.fields;

          if (!BeeUtils.isEmpty(fields)) {
            for (XmlField field : fields) {
              table.addField(field, extMode);
            }
          }
        }
        if (!BeeUtils.isEmpty(xmlTable.indexes)) {
          for (XmlIndex index : xmlTable.indexes) {
            if (!BeeUtils.isEmpty(index.fields)) {
              for (String fld : index.fields) {
                Assert.state(table.hasField(fld),
                    BeeUtils.joinWords("Unrecognized index field:", tbl, fld));
              }
              table.addIndex(tableName, index.fields, index.unique);
            }
          }
        }
        if (!BeeUtils.isEmpty(xmlTable.constraints)) {
          for (XmlConstraint constraint : xmlTable.constraints) {
            if (constraint instanceof XmlCheck) {
              String expression = null;

              switch (SqlBuilderFactory.getBuilder().getEngine()) {
                case POSTGRESQL:
                  expression = ((XmlCheck) constraint).postgreSql;
                  break;
                case MSSQL:
                  expression = ((XmlCheck) constraint).msSql;
                  break;
                case ORACLE:
                  expression = ((XmlCheck) constraint).oracle;
                  break;
                case GENERIC:
                  expression = ((XmlCheck) constraint).generic;
                  break;
              }
              if (BeeUtils.isEmpty(expression)) {
                expression = ((XmlCheck) constraint).generic;
              }
              if (!BeeUtils.isEmpty(expression)) {
                table.addCheck(tableName, expression);
              }
            } else if (constraint instanceof XmlUnique) {
              List<String> fields = ((XmlUnique) constraint).fields;

              if (!BeeUtils.isEmpty(fields)) {
                for (String fld : fields) {
                  Assert.state(table.hasField(fld),
                      BeeUtils.joinWords("Unrecognized unique key field:", tbl, fld));
                }
                table.addUniqueKey(tableName, fields);
              }
            } else if (constraint instanceof XmlReference) {
              List<String> fields = ((XmlReference) constraint).fields;
              List<String> refFields = ((XmlReference) constraint).refFields;

              if (!BeeUtils.isEmpty(fields)) {
                for (String fld : fields) {
                  Assert.state(table.hasField(fld),
                      BeeUtils.joinWords("Unrecognized foreign key field:", tbl, fld));
                }
                Assert.state(BeeUtils.isEmpty(refFields)
                    ? fields.size() == 1 : fields.size() == refFields.size(),
                    "Field count doesn't match");

                table.addForeignKey(tableName, fields, ((XmlReference) constraint).refTable,
                    refFields, NameUtils.getEnumByName(SqlKeyword.class,
                        ((XmlReference) constraint).cascade));
              }
            }
          }
        }
        if (!BeeUtils.isEmpty(xmlTable.triggers)) {
          for (XmlTrigger trigger : xmlTable.triggers) {
            String body = null;
            List<SqlTriggerEvent> events = Lists.newArrayList();

            for (String event : trigger.events) {
              events.add(NameUtils.getEnumByName(SqlTriggerEvent.class, event));
            }
            switch (SqlBuilderFactory.getBuilder().getEngine()) {
              case POSTGRESQL:
                body = trigger.postgreSql;
                break;
              case MSSQL:
                body = trigger.msSql;
                break;
              case ORACLE:
                body = trigger.oracle;
                break;
              case GENERIC:
                body = null;
                break;
            }
            if (!BeeUtils.isEmpty(body)) {
              table.addTrigger(tableName, SqlTriggerType.CUSTOM,
                  ImmutableMap.of("body", body),
                  NameUtils.getEnumByName(SqlTriggerTiming.class, trigger.timing),
                  EnumSet.copyOf(events),
                  NameUtils.getEnumByName(SqlTriggerScope.class, trigger.scope));
            }
          }
        }
        if (table.isEmpty()) {
          logger.warning("Table has no fields defined:", tbl);
          table = null;
        }
      }
    }
    if (table != null) {
      register(table, tableCache);
    } else {
      unregister(tableName, tableCache);
    }
    return table != null;
  }

  private boolean initView(String moduleName, String viewName) {
    Assert.notEmpty(viewName);
    BeeView view = null;
    XmlView xmlView = getXmlView(moduleName, viewName);

    if (xmlView != null) {
      if (!BeeUtils.same(xmlView.name, viewName)) {
        logger.warning("View name doesn't match resource name:", xmlView.name);
      } else {
        String src = xmlView.source;

        if (!isTable(src)) {
          logger.warning("Unrecognized view source:", xmlView.name, src);
        } else {
          view = new BeeView(moduleName, xmlView, tableCache);

          if (view.isEmpty()) {
            logger.warning("View has no columns defined:", view.getName());
            view = null;
          }
        }
      }
    }
    if (view != null) {
      register(view, viewCache);
    } else {
      unregister(viewName, viewCache);
    }
    return view != null;
  }

  private void makeStructureChanges(IsQuery... queries) {
    Assert.notNull(queries);

    for (IsQuery query : queries) {
      if (qs.updateData(query) < 0) {
        Assert.untouchable();
      }
    }
  }

  private void rebuildTable(BeeTable table) {
    table.setActive(false);
    String tblMain = table.getName();
    Map<String, String> rebuilds = createTable(table, null);

    if (rebuilds.containsKey(tblMain)) {
      Collection<BeeIndex> indexes = Lists.newArrayList();

      for (BeeIndex index : table.getIndexes()) {
        if (BeeUtils.same(index.getTable(), tblMain)) {
          indexes.add(index);
        }
      }
      Collection<BeeUniqueKey> uniqueKeys = Lists.newArrayList();

      for (BeeUniqueKey key : table.getUniqueKeys()) {
        if (BeeUtils.same(key.getTable(), tblMain)) {
          uniqueKeys.add(key);
        }
      }
      Collection<BeeForeignKey> foreignKeys = Lists.newArrayList();

      for (BeeForeignKey fKey : table.getForeignKeys()) {
        String refTable = fKey.getRefTable();

        if ((BeeUtils.same(refTable, tblMain) && !rebuilds.containsKey(fKey.getTable()))
            || (BeeUtils.same(fKey.getTable(), tblMain)
            && (BeeUtils.same(refTable, tblMain) || getTable(refTable).isActive()))) {

          foreignKeys.add(fKey);
        }
      }
      for (BeeTable other : getTables()) {
        if (!BeeUtils.same(other.getName(), tblMain) && other.isActive()) {
          for (BeeForeignKey fKey : other.getForeignKeys()) {
            if (BeeUtils.same(fKey.getRefTable(), tblMain)) {
              foreignKeys.add(fKey);
            }
          }
        }
      }
      Collection<BeeCheck> checks = Lists.newArrayList();

      for (BeeCheck check : table.getChecks()) {
        if (BeeUtils.same(check.getTable(), tblMain)) {
          checks.add(check);
        }
      }
      Collection<BeeTrigger> triggers = Lists.newArrayList();

      for (BeeTrigger trigger : table.getTriggers()) {
        if (BeeUtils.same(trigger.getTable(), tblMain)) {
          triggers.add(trigger);
        }
      }
      String tblBackup = rebuilds.get(tblMain);

      if (!BeeUtils.isEmpty(tblBackup)) {
        for (SimpleRow fKeys : qs
            .dbForeignKeys(getDbName(), getDbSchema(), null, tblMain)) {
          String fk = fKeys.getValue(SqlConstants.KEY_NAME);
          String tbl = fKeys.getValue(SqlConstants.TBL_NAME);
          makeStructureChanges(SqlUtils.dropForeignKey(tbl, fk));
        }
        makeStructureChanges(SqlUtils.dropTable(tblMain));
        makeStructureChanges(SqlUtils.renameTable(tblBackup, tblMain));
      }
      createIndexes(indexes);
      createUniqueKeys(uniqueKeys);
      createChecks(checks);
      createTriggers(triggers);
      createForeignKeys(foreignKeys);
    }

    for (String tbl : rebuilds.keySet()) {
      if (!BeeUtils.same(tbl, tblMain)) {
        Collection<BeeIndex> indexes = Lists.newArrayList();

        for (BeeIndex index : table.getIndexes()) {
          if (BeeUtils.same(index.getTable(), tbl)) {
            indexes.add(index);
          }
        }
        Collection<BeeUniqueKey> uniqueKeys = Lists.newArrayList();

        for (BeeUniqueKey key : table.getUniqueKeys()) {
          if (BeeUtils.same(key.getTable(), tbl)) {
            uniqueKeys.add(key);
          }
        }
        Collection<BeeForeignKey> foreignKeys = Lists.newArrayList();

        for (BeeForeignKey fKey : table.getForeignKeys()) {
          String refTable = fKey.getRefTable();

          if (BeeUtils.same(fKey.getTable(), tbl)
              && (BeeUtils.same(refTable, tblMain) || getTable(refTable).isActive())) {

            foreignKeys.add(fKey);
          }
        }
        Collection<BeeCheck> checks = Lists.newArrayList();

        for (BeeCheck check : table.getChecks()) {
          if (BeeUtils.same(check.getTable(), tbl)) {
            checks.add(check);
          }
        }
        Collection<BeeTrigger> triggers = Lists.newArrayList();

        for (BeeTrigger trigger : table.getTriggers()) {
          if (BeeUtils.same(trigger.getTable(), tbl)) {
            triggers.add(trigger);
          }
        }
        String tblBackup = rebuilds.get(tbl);

        if (!BeeUtils.isEmpty(tblBackup)) {
          makeStructureChanges(SqlUtils.dropTable(tbl), SqlUtils.renameTable(tblBackup, tbl));
        }
        createIndexes(indexes);
        createUniqueKeys(uniqueKeys);
        createChecks(checks);
        createTriggers(triggers);
        createForeignKeys(foreignKeys);
      }
    }
    createAuditTables(table);

    table.setActive(true);
  }

  private <T extends BeeObject> void register(T object, Map<String, T> cache) {
    if (object != null) {
      String name = NameUtils.getClassName(object.getClass());
      String objectName = object.getName();
      String moduleName = BeeUtils.parenthesize(object.getModuleName());
      T existingObject = cache.get(BeeUtils.normalize(objectName));

      if (existingObject != null) {
        logger.warning(moduleName, "Dublicate", name, "name:",
            BeeUtils.bracket(objectName), BeeUtils.parenthesize(existingObject.getModuleName()));
      } else {
        cache.put(BeeUtils.normalize(objectName), object);
      }
    }
  }

  private void unregister(String objectName, Map<String, ? extends BeeObject> cache) {
    if (!BeeUtils.isEmpty(objectName)) {
      cache.remove(BeeUtils.normalize(objectName));
    }
  }
}
