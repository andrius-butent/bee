package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionStatus;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;

enum DiscussionsListType implements HasCaption, HasWidgetSupplier {

  ALL(Localized.dictionary().discussAllShort()) {
    @Override
    Filter getFilter(LongValue userId) {
      return null;
    }
  },
  ACTIVE(Localized.dictionary().discussPublic1()) {
    @Override
    Filter getFilter(LongValue userId) {
      Filter isPublic = Filter.notNull(COL_ACCESSIBILITY);
      Filter isActive = Filter.isEqual(COL_STATUS, IntegerValue.of(DiscussionStatus.ACTIVE));
      Filter hasTopic = Filter.isNull(COL_TOPIC);

      return Filter.and(isPublic, isActive, hasTopic);
    }
  },
  CLOSED(Localized.dictionary().discussClosed()) {
    @Override
    Filter getFilter(LongValue userId) {
      Filter isMemberFilter = Filter.isEqual(COL_MEMBER, BooleanValue.TRUE);
      Filter isOwner = Filter.isEqual(COL_OWNER, userId);
      Filter isPublic = Filter.notNull(COL_ACCESSIBILITY);
      Filter isUserFilter = Filter.isEqual(COL_USER, userId);
      Filter isActive = Filter.isEqual(COL_STATUS, IntegerValue.of(DiscussionStatus.CLOSED));
      Filter hasTopic = Filter.isNull(COL_TOPIC);

      Filter discussUsersFilter = Filter.in(Data.getIdColumn(VIEW_DISCUSSIONS),
          VIEW_DISCUSSIONS_USERS, COL_DISCUSSION,
          Filter.and(isUserFilter, isMemberFilter));
      return Filter.and(Filter.or(
          Filter.or(discussUsersFilter, isOwner), isPublic),
          isActive, hasTopic);
    }
  },
  OBSERVED(Localized.dictionary().discussPrivateShort()) {
    @Override
    Filter getFilter(LongValue userId) {
      Filter activeStatusFilter = Filter.isEqual(COL_STATUS,
          IntegerValue.of(DiscussionStatus.ACTIVE));
      Filter isOwnerFilter = Filter.isEqual(COL_OWNER, userId);
      Filter isMemberFilter = Filter.isEqual(COL_MEMBER, BooleanValue.TRUE);
      Filter isUserFilter = Filter.isEqual(COL_USER, userId);
      Filter hasTopic = Filter.isNull(COL_TOPIC);
      Filter notPublic = Filter.isNull(COL_ACCESSIBILITY);

      Filter discussUsersFilter = Filter.in(Data.getIdColumn(VIEW_DISCUSSIONS),
          VIEW_DISCUSSIONS_USERS, COL_DISCUSSION,
          Filter.and(isUserFilter, isMemberFilter));

      return Filter.and(Filter.or(isOwnerFilter, discussUsersFilter), hasTopic, activeStatusFilter,
          notPublic);
    }
  },
  STARRED(Localized.dictionary().discussStarred()) {
    @Override
    Filter getFilter(LongValue userId) {
      Filter isPublic = Filter.notNull(COL_ACCESSIBILITY);
      Filter isMember =
          Filter.in(Data.getIdColumn(VIEW_DISCUSSIONS), VIEW_DISCUSSIONS_USERS, COL_DISCUSSION,
              Filter.and(Filter.isEqual(COL_USER, userId),
                  Filter.isEqual(COL_MEMBER, BooleanValue.TRUE)));

      Filter isStarred =
          Filter.in(Data.getIdColumn(VIEW_DISCUSSIONS), VIEW_DISCUSSIONS_USERS, COL_DISCUSSION,
              Filter.and(Filter.notNull(COL_STAR),
                  Filter.isEqual(COL_USER, userId)));

      Filter notPublicIsMember = Filter.and(Filter.isNot(isPublic), isMember);
      Filter isPublicNotMember = Filter.and(isPublic, Filter.isNot(isMember));
      Filter isPublicIsMember = Filter.and(isPublic, isMember);
      Filter hasTopic = Filter.isNull(COL_TOPIC);

      return Filter.and(isStarred, Filter.or(Lists.newArrayList(notPublicIsMember,
          isPublicNotMember,
          isPublicIsMember, hasTopic)));
    }
  },

  ANNOUNCEMENTSBOARDLIST(Localized.dictionary().announcements()) {
    @Override
    Filter getFilter(LongValue userId) {
      Filter isMemberFilter = Filter.isEqual(COL_MEMBER, BooleanValue.TRUE);
      Filter isOwner = Filter.isEqual(COL_OWNER, userId);
      Filter isPublic = Filter.notNull(COL_ACCESSIBILITY);
      Filter isUserFilter = Filter.isEqual(COL_USER, userId);
      Filter hasTopic = Filter.notNull(COL_TOPIC);

      Filter discussUsersFilter = Filter.in(Data.getIdColumn(VIEW_DISCUSSIONS),
          VIEW_DISCUSSIONS_USERS, COL_DISCUSSION,
          Filter.and(isUserFilter, isMemberFilter));
      return Filter.and(Filter.or(
          Filter.or(discussUsersFilter, isOwner), isPublic), hasTopic);
    }
  };

  public static DiscussionsListType getByPrefix(String input) {
    for (DiscussionsListType type : values()) {
      if (BeeUtils.startsSame(type.name(), input)) {
        return type;
      }
    }
    return null;
  }

  private final String caption;

  DiscussionsListType(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public String getSupplierKey() {
    return GRID_DISCUSSIONS + BeeConst.STRING_UNDER + name().toLowerCase();
  }

  abstract Filter getFilter(LongValue userId);
}
