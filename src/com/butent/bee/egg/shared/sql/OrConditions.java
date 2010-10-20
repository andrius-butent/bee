package com.butent.bee.egg.shared.sql;

import com.butent.bee.egg.shared.utils.BeeUtils;

class OrConditions extends Conditions {

  @Override
  public String getCondition(SqlBuilder builder, boolean queryMode) {
    String cond = super.getCondition(builder, queryMode);

    if (!BeeUtils.isEmpty(cond)) {
      cond = "(" + cond + ")";
    }

    return cond;
  }

  @Override
  protected String joinMode() {
    return " OR ";
  }
}
