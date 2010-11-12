package com.butent.bee.egg.server.data;

import com.butent.bee.egg.server.communication.ResponseBuffer;
import com.butent.bee.egg.server.data.BeeTable.BeeKey;
import com.butent.bee.egg.server.data.BeeTable.BeeStructure;
import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.sql.BeeConstants.DataTypes;
import com.butent.bee.egg.shared.sql.BeeConstants.Keywords;
import com.butent.bee.egg.shared.sql.SqlCreate;
import com.butent.bee.egg.shared.sql.SqlDrop;
import com.butent.bee.egg.shared.sql.SqlIndex;
import com.butent.bee.egg.shared.sql.SqlInsert;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
@Lock(LockType.READ)
public class SystemBean {

  @EJB
  QueryServiceBean qs;

  private Map<String, BeeTable> dataCache = new HashMap<String, BeeTable>();

  public boolean beeTable(String source) {
    Assert.notEmpty(source);
    return dataCache.containsKey(source);
  }

  public String getIdName(String table) {
    return dataCache.get(table).getIdName();
  }

  public String getLockName(String table) {
    return dataCache.get(table).getLockName();
  }

  @Lock(LockType.WRITE)
  public void recreate(ResponseBuffer buff) {
    for (BeeTable tbl : dataCache.values()) {
      String table = tbl.getName();

      if (qs.tableExists(table)) {
        SqlDrop sd = new SqlDrop(table);
        qs.updateData(sd);
      }
      SqlCreate sc = new SqlCreate(table);

      for (BeeStructure field : tbl.getFields()) {
        sc.addField(field.getName(), field.getType(),
            field.getPrecision(), field.getScale(),
            field.isNotNull() ? Keywords.NOTNULL : null,
            field.isUnique() ? Keywords.UNIQUE : null);
      }
      sc.addLong(tbl.getLockName(), Keywords.NOTNULL)
        .addLong(tbl.getIdName(), Keywords.NOTNULL, Keywords.PRIMARY);

      buff.add("Create table " + table + ": " + qs.updateData(sc));

      for (BeeKey key : tbl.getKeys()) {
        SqlIndex si = new SqlIndex(table, key.getName(), key.isUnique());
        si.setColumns(key.getColumns());
        qs.updateData(si);
      }
    }
    createData();
  }

  private void createData() {
    BeeTable tables = dataCache.get("bee_Tables");

    for (BeeTable tbl : dataCache.values()) {
      SqlInsert si = new SqlInsert(tables.getName());
      Iterator<BeeStructure> it = tables.getFields().iterator();

      si.addConstant(it.next().getName(), tbl.getName())
        .addConstant(it.next().getName(), tbl.getLockName())
        .addConstant(it.next().getName(), tbl.getIdName());

      qs.insertData(si);

      BeeTable propert = dataCache.get("bee_Fields");

      for (BeeStructure fld : tbl.getFields()) {
        si = new SqlInsert(propert.getName());
        it = propert.getFields().iterator();

        si.addConstant(it.next().getName(), tbl.getName())
          .addConstant(it.next().getName(), fld.getName())
          .addConstant(it.next().getName(), fld.getType().name())
          .addConstant(it.next().getName(), fld.getPrecision())
          .addConstant(it.next().getName(), fld.getScale())
          .addConstant(it.next().getName(), BeeUtils.toInt(fld.isNotNull()))
          .addConstant(it.next().getName(), BeeUtils.toInt(fld.isUnique()));

        qs.insertData(si);
      }
    }
  }

  @SuppressWarnings("unused")
  @PostConstruct
  private void init() {
    initTables();
    initStructure();
  }

  private void initStructure() {
    dataCache.get("bee_Tables")
      .addField("TableName", DataTypes.STRING, 30, 0, true, true)
      .addField("IdColumn", DataTypes.STRING, 30, 0, false, false)
      .addField("LockColumn", DataTypes.STRING, 30, 0, false, false);
    dataCache.get("bee_Fields")
      .addField("TableName", DataTypes.STRING, 30, 0, true, false)
      .addField("FieldName", DataTypes.STRING, 30, 0, true, false)
      .addField("FieldType", DataTypes.STRING, 30, 0, false, false)
      .addField("Precision", DataTypes.INTEGER, 0, 0, false, false)
      .addField("Scale", DataTypes.INTEGER, 0, 0, false, false)
      .addField("NotNull", DataTypes.BOOLEAN, 0, 0, false, false)
      .addField("Unique", DataTypes.BOOLEAN, 0, 0, false, false);
    dataCache.get("Countries")
      .addField("Country", DataTypes.STRING, 100, 0, true, true);
    dataCache.get("Cities")
      .addField("City", DataTypes.STRING, 100, 0, true, true);
  }

  private void initTables() {
    dataCache.put("bee_Tables", new BeeTable("bee_Tables", "TableID", "Locked"));
    dataCache.put("bee_Fields", new BeeTable("bee_Fields", "FieldID", null)
      .addKey("FieldName")
      .addUniqueKey("TableField", "TableName", "FieldName"));
    dataCache.put("Countries", new BeeTable("Countries", "CountryID", null));
    dataCache.put("Cities", new BeeTable("Cities", "CityID", null));
  }
}
