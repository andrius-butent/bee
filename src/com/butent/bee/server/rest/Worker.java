package com.butent.bee.server.rest;

import static com.butent.bee.server.rest.CrudWorker.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
@Produces(RestResponse.JSON_TYPE)
@Stateless
public class Worker {

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  FileStorageBean fs;

  @GET
  @Path("durationtypes")
  public RestResponse getDurationTypes(@HeaderParam(RestResponse.LAST_SYNC_TIME) Long lastSynced) {
    long time = System.currentTimeMillis();

    BeeRowSet rowSet = (BeeRowSet) qs.doSql(new SqlSelect()
        .addField(TBL_DURATION_TYPES, sys.getIdName(TBL_DURATION_TYPES), ID)
        .addField(TBL_DURATION_TYPES, sys.getVersionName(TBL_DURATION_TYPES), VERSION)
        .addField(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME, COL_DURATION_TYPE)
        .addFrom(TBL_DURATION_TYPES)
        .setWhere(Objects.nonNull(lastSynced)
            ? SqlUtils.more(TBL_DURATION_TYPES, sys.getVersionName(TBL_DURATION_TYPES), lastSynced)
            : null).getQuery());

    return RestResponse.ok(rsToMap(rowSet)).setLastSync(time);
  }

  @GET
  @Path("users")
  public RestResponse getUsers(@HeaderParam(RestResponse.LAST_SYNC_TIME) Long lastSynced) {
    long time = System.currentTimeMillis();

    BeeRowSet users = (BeeRowSet) qs.doSql(new SqlSelect()
        .addField(TBL_USERS, sys.getIdName(TBL_USERS), ID)
        .addField(TBL_USERS, sys.getVersionName(TBL_USERS), VERSION)
        .addField(TBL_USERS, COL_COMPANY_PERSON, COL_COMPANY_PERSON + ID)
        .addFields(TBL_USERS, COL_USER_BLOCK_FROM, COL_USER_BLOCK_UNTIL)
        .addFrom(TBL_USERS)
        .setWhere(Objects.nonNull(lastSynced)
            ? SqlUtils.more(TBL_USERS, sys.getVersionName(TBL_USERS), lastSynced) : null)
        .getQuery());

    BeeRowSet persons = qs.getViewData(new CompanyPersonsWorker().getViewName(),
        Filter.idIn(users.getDistinctLongs(users.getColumnIndex(COL_COMPANY_PERSON + ID))));

    Collection<Map<String, Object>> data = rsToMap(users);

    for (Map<String, Object> user : data) {
      int r = persons.getRowIndex((Long) user.get(COL_COMPANY_PERSON + ID));
      BeeRow row = persons.getRow(r);
      Map<String, Object> person = new LinkedHashMap<>();
      person.put(ID, row.getId());
      person.put(VERSION, row.getVersion());

      for (int c = 0; c < persons.getNumberOfColumns(); c++) {
        person.put(persons.getColumnId(c), CrudWorker.getValue(persons, r, c));
      }
      user.put(COL_COMPANY_PERSON, person);
    }
    return RestResponse.ok(data).setLastSync(time);
  }

  @GET
  @Path("login")
  public RestResponse login() {
    Map<String, Object> resp = new HashMap<>();

    for (Class<? extends Enum<?>> aClass : Arrays.asList(TaskStatus.class,
        TaskPriority.class, TaskEvent.class)) {

      List<Object> list = new ArrayList<>();

      for (Enum<?> constant : aClass.getEnumConstants()) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Id", constant.ordinal());
        map.put("Caption", ((HasCaption) constant).getCaption());
        list.add(map);
      }
      resp.put(aClass.getSimpleName(), list);
    }
    Long userId = usr.getCurrentUserId();

    SimpleRowSet.SimpleRow info = qs.getRow(new SqlSelect()
        .addConstant(userId, COL_USER + ID)
        .addField(TBL_COMPANY_PERSONS, COL_COMPANY, COL_COMPANY + ID)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addField(TBL_USERS, COL_COMPANY_PERSON, COL_COMPANY_PERSON + ID)
        .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addFrom(TBL_USERS)
        .addFromInner(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
        .addFromInner(TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .addFromInner(TBL_COMPANIES,
            sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS, COL_COMPANY))
        .setWhere(sys.idEquals(TBL_USERS, userId)));

    Map<String, Object> map = new HashMap<>();

    for (String col : info.getColumnNames()) {
      map.put(col, BeeUtils.isSuffix(col, ID) ? info.getLong(col) : info.getValue(col));
    }
    map.put(COL_EMAIL, usr.getUserEmail(userId, true));
    resp.put(COL_USER, map);

    return RestResponse.ok(resp);
  }

  private static Collection<Map<String, Object>> rsToMap(BeeRowSet rowSet) {
    List<Map<String, Object>> data = new ArrayList<>();

    for (int i = 0; i < rowSet.getNumberOfRows(); i++) {
      Map<String, Object> row = new LinkedHashMap<>(rowSet.getNumberOfColumns());

      for (int j = 0; j < rowSet.getNumberOfColumns(); j++) {
        row.put(rowSet.getColumnId(j), CrudWorker.getValue(rowSet, i, j));
      }
      data.add(row);
    }
    return data;
  }
}
