package com.butent.bee.server.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeRelation;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.news.Feed;
import com.butent.bee.shared.data.news.Headline;
import com.butent.bee.shared.data.news.NewsUtils;
import com.butent.bee.shared.data.news.Subscription;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class NewsBean {

  private static BeeLogger logger = LogUtils.getLogger(NewsBean.class);

  private static final String TBL_USER_FEEDS = "UserFeeds";

  private static final String COL_UF_USER = "User";
  private static final String COL_UF_FEED = CommonsConstants.COL_FEED;

  private static final String COL_UF_CAPTION = "Caption";
  private static final String COL_UF_SUBSCRIPTION_DATE = "SubscriptionDate";
  private static final String COL_UF_ORDINAL = "Ordinal";

  private static final String COL_USAGE_USER = "User";

  private static final String COL_USAGE_ACCESS = "Access";
  private static final String COL_USAGE_UPDATE = "Update";

  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  public ResponseObject getNews() {
    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error("user id not available");
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_USER_FEEDS, COL_UF_FEED, COL_UF_CAPTION, COL_UF_SUBSCRIPTION_DATE)
        .addFrom(TBL_USER_FEEDS)
        .setWhere(SqlUtils.equals(TBL_USER_FEEDS, COL_UF_USER, userId))
        .addOrder(TBL_USER_FEEDS, COL_UF_ORDINAL, sys.getIdName(TBL_USER_FEEDS));

    SimpleRowSet userFeeds = qs.getData(query);
    if (DataUtils.isEmpty(userFeeds)) {
      return ResponseObject.emptyResponse();
    }

    List<Subscription> subscriptions = Lists.newArrayList();
    int countHeadlines = 0;

    for (SimpleRow row : userFeeds) {
      Feed feed = EnumUtils.getEnumByName(Feed.class, row.getValue(COL_UF_FEED));
      if (feed == null) {
        logger.warning("invalid user feed name", row.getValue(COL_UF_FEED));
        break;
      }

      String caption = row.getValue(COL_UF_CAPTION);
      DateTime date = row.getDateTime(COL_UF_SUBSCRIPTION_DATE);

      Subscription subscription = new Subscription(feed, caption, date);

      List<Headline> headlines = getHeadlines(userId, feed, date);
      if (!headlines.isEmpty()) {
        subscription.getHeadlines().addAll(headlines);
        countHeadlines += headlines.size();
      }

      subscriptions.add(subscription);
    }

    logger.info("user", userId, "subscriptions", subscriptions.size(), "headlines", countHeadlines);

    return ResponseObject.response(subscriptions);
  }

  public ResponseObject onAccess(RequestInfo reqInfo) {
    Assert.notNull(reqInfo);

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(reqInfo.getService(), "user id not available");
    }

    String table = reqInfo.getParameter(Service.VAR_TABLE);
    if (BeeUtils.isEmpty(table)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_TABLE);
    }

    Long dataId = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_ID));
    if (!DataUtils.isId(dataId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_ID);
    }

    String usageTable = NewsUtils.getUsageTable(table);
    if (BeeUtils.isEmpty(usageTable)) {
      return ResponseObject.error(reqInfo.getService(), "table", table, "has no usage table");
    }

    String relationColumn = getUsageRelationColumn(table, usageTable);
    if (BeeUtils.isEmpty(relationColumn)) {
      return ResponseObject.error(reqInfo.getService(), "relation column not found for");
    }

    IsCondition where = SqlUtils.equals(usageTable, relationColumn, dataId, COL_USAGE_USER, userId);

    if (qs.sqlExists(usageTable, where)) {
      return qs.updateDataWithResponse(new SqlUpdate(usageTable)
          .addConstant(COL_USAGE_ACCESS, System.currentTimeMillis())
          .setWhere(where));

    } else {
      return qs.insertDataWithResponse(new SqlInsert(usageTable)
          .addFields(relationColumn, COL_USAGE_USER, COL_USAGE_ACCESS)
          .addValues(dataId, userId, System.currentTimeMillis()));
    }
  }

  public void onUpdate(String table, Long rowId) {
    String usageTable = NewsUtils.getUsageTable(table);
    if (BeeUtils.isEmpty(usageTable)) {
      return;
    }

    if (!DataUtils.isId(rowId)) {
      return;
    }

    String relationColumn = getUsageRelationColumn(table, usageTable);
    if (BeeUtils.isEmpty(relationColumn)) {
      return;
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return;
    }

    long now = System.currentTimeMillis();

    IsCondition where = SqlUtils.equals(usageTable, relationColumn, rowId, COL_USAGE_USER, userId);

    if (qs.sqlExists(usageTable, where)) {
      qs.updateData(new SqlUpdate(usageTable)
          .addConstant(COL_USAGE_ACCESS, now)
          .addConstant(COL_USAGE_UPDATE, now)
          .setWhere(where));

    } else {
      qs.insertData(new SqlInsert(usageTable)
          .addFields(relationColumn, COL_USAGE_USER, COL_USAGE_ACCESS, COL_USAGE_UPDATE)
          .addValues(rowId, userId, now, now));
    }
  }

  public ResponseObject subscribe(RequestInfo reqInfo) {
    Assert.notNull(reqInfo);

    Long userId = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_USER));
    if (!DataUtils.isId(userId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_USER);
    }

    String feedList = reqInfo.getParameter(Service.VAR_FEED);
    if (BeeUtils.isEmpty(feedList)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_FEED);
    }

    Set<Feed> feeds = NewsUtils.splitFeeds(feedList);
    if (feeds.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), "cannot parse feeds", feedList);
    }

    long time = TimeUtils.nowMinutes().getTime();

    for (Feed feed : feeds) {
      SqlInsert insert = new SqlInsert(TBL_USER_FEEDS)
          .addFields(COL_UF_USER, COL_UF_FEED, COL_UF_SUBSCRIPTION_DATE)
          .addValues(userId, feed.name(), time);

      ResponseObject response = qs.insertDataWithResponse(insert);
      if (response.hasErrors()) {
        return response;
      }
    }

    logger.info(reqInfo.getService(), "user", userId, "subscribed to", feeds);

    return ResponseObject.response(feeds.size());
  }

  private Map<Long, Long> getAccess(long userId, Feed feed, String usageTable,
      String relationColumn) {

    Map<Long, Long> access = Maps.newHashMap();

    if (!BeeUtils.anyEmpty(usageTable, relationColumn)) {
      SqlSelect query = new SqlSelect()
          .addFields(usageTable, relationColumn)
          .addMax(usageTable, COL_USAGE_ACCESS)
          .addFrom(usageTable)
          .setWhere(SqlUtils.equals(usageTable, COL_USAGE_USER, userId))
          .addGroup(usageTable, relationColumn);

      SimpleRowSet data = qs.getData(query);
      if (!DataUtils.isEmpty(data)) {
        for (SimpleRow row : data) {
          access.put(row.getLong(0), row.getLong(1));
        }
      }
    }

    return access;
  }

  private List<Headline> getHeadlines(long userId, Feed feed, DateTime startDate) {
    List<Headline> result = Lists.newArrayList();

    String table = feed.getTable();
    String usageTable = NewsUtils.getUsageTable(table);

    if (!BeeUtils.isEmpty(table) && !sys.isTable(table)) {
      logger.severe("feed", feed.name(), "table not recognized", table);
      return result;
    }

    if (!BeeUtils.isEmpty(usageTable) && !sys.isTable(usageTable)) {
      logger.severe("feed", feed.name(), "usage table not recognized", usageTable);
      return result;
    }

    String relationColumn = BeeUtils.isEmpty(usageTable) ? null
        : getUsageRelationColumn(table, usageTable);

    long startTime = (startDate == null) ? 0L : startDate.getTime();

    Map<Long, Long> updates = getUpdates(userId, feed, usageTable, relationColumn, startTime);
    if (updates.isEmpty()) {
      return result;
    }

    Map<Long, Long> access = getAccess(userId, feed, usageTable, relationColumn);

    Set<Long> newIds = Sets.newHashSet();
    Set<Long> updIds = Sets.newHashSet();

    if (access.isEmpty()) {
      newIds.addAll(updates.keySet());

    } else {
      for (Map.Entry<Long, Long> updateEntry : updates.entrySet()) {
        Long id = updateEntry.getKey();

        Long accessTime = access.get(id);
        if (accessTime == null) {
          newIds.add(id);
        } else if (accessTime < updateEntry.getValue()) {
          updIds.add(id);
        }
      }
    }

    List<Headline> headlines = queryHeadlines(feed, newIds, updIds);
    if (!headlines.isEmpty()) {
      result.addAll(headlines);
    }

    return result;
  }
  
  private Map<Long, Long> getUpdates(long userId, Feed feed, String usageTable,
      String relationColumn, long startTime) {

    Map<Long, Long> updates = Maps.newHashMap();

    if (!BeeUtils.anyEmpty(usageTable, relationColumn)) {
      SqlSelect query = new SqlSelect()
          .addFields(usageTable, relationColumn)
          .addMax(usageTable, COL_USAGE_UPDATE)
          .addFrom(usageTable)
          .setWhere(SqlUtils.and(SqlUtils.notEqual(usageTable, COL_USAGE_USER, userId),
              SqlUtils.more(usageTable, COL_USAGE_UPDATE, startTime)))
          .addGroup(usageTable, relationColumn);

      SimpleRowSet data = qs.getData(query);
      if (!DataUtils.isEmpty(data)) {
        for (SimpleRow row : data) {
          updates.put(row.getLong(0), row.getLong(1));
        }
      }
    }

    return updates;
  }

  private String getUsageRelationColumn(String table, String usageTable) {
    Collection<BeeField> fields = sys.getTableFields(usageTable);

    if (fields != null) {
      for (BeeField field : fields) {
        if (field instanceof BeeRelation && table.equals(((BeeRelation) field).getRelation())) {
          return field.getName();
        }
      }
    }

    logger.severe("table", table, "usage table", usageTable, "relation column not found");
    return null;
  }

  private List<Headline> queryHeadlines(Feed feed, Collection<Long> newIds,
      Collection<Long> updIds) {
    List<Headline> headlines = Lists.newArrayList();

    boolean hasNew = !BeeUtils.isEmpty(newIds);
    boolean hasUpd = !BeeUtils.isEmpty(updIds);

    if (!hasNew && !hasUpd) {
      return headlines;
    }

    Set<Long> ids = Sets.newHashSet();
    if (hasNew) {
      ids.addAll(newIds);
    }
    if (hasUpd) {
      ids.addAll(updIds);
    }

    if (!BeeUtils.isEmpty(feed.getHeadlineView())) {
      String viewName = feed.getHeadlineView();
      if (!sys.isView(viewName)) {
        logger.severe("feed", feed.name(), "view", viewName, "not available");
        return headlines;
      }

      List<String> headlineColumns = feed.getHeadlineColumns();
      if (BeeUtils.isEmpty(headlineColumns)) {
        logger.severe("feed", feed.name(), "headline columns not specified");
        return headlines;
      }

      BeeRowSet rowSet = qs.getViewData(viewName, Filter.idIn(ids), null, headlineColumns);
      if (DataUtils.isEmpty(rowSet)) {
        return headlines;
      }

      List<Integer> indexes = Lists.newArrayList();
      for (String column : headlineColumns) {
        indexes.add(rowSet.getColumnIndex(column));
      }

      for (BeeRow row : rowSet.getRows()) {
        long id = row.getId();
        String caption = DataUtils.join(row, indexes, Headline.CAPTION_SEPARATOR);

        if (hasNew && newIds.contains(id)) {
          headlines.add(Headline.forInsert(id, caption));
        } else {
          headlines.add(Headline.forUpdate(id, caption));
        }
      }
    }

    return headlines;
  }
}
