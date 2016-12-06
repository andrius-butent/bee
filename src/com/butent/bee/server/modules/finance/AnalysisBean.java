package com.butent.bee.server.modules.finance;

import static com.butent.bee.shared.modules.finance.Dimensions.*;
import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class AnalysisBean {

  private static BeeLogger logger = LogUtils.getLogger(AnalysisBean.class);

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  public ResponseObject calculateForm(long formId) {
    String msg = BeeUtils.joinWords(SVC_CALCULATE_ANALYSIS_FORM, formId);
    return ResponseObject.response(msg);
  }

  public ResponseObject verifyForm(long formId) {
    AnalysisFormData formData = getFormData(formId);
    if (formData == null) {
      return ResponseObject.error("form", formId, "not found");
    }

    List<String> messages = formData.validate();
    if (BeeUtils.isEmpty(messages)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.responseWithSize(messages);
    }
  }

  private AnalysisFormData getFormData(long formId) {
    BeeRowSet headerData = qs.getViewDataById(VIEW_ANALYSIS_HEADERS, formId);
    if (DataUtils.isEmpty(headerData)) {
      logger.warning(VIEW_ANALYSIS_HEADERS, "form", formId, "not found");
      return null;
    }

    BeeRowSet columnData =
        qs.getViewData(VIEW_ANALYSIS_COLUMNS, Filter.equals(COL_ANALYSIS_HEADER, formId));
    BeeRowSet rowData =
        qs.getViewData(VIEW_ANALYSIS_ROWS, Filter.equals(COL_ANALYSIS_HEADER, formId));

    CompoundFilter filter = Filter.or();
    filter.add(Filter.equals(COL_ANALYSIS_HEADER, formId));

    if (!DataUtils.isEmpty(columnData)) {
      filter.add(Filter.any(COL_ANALYSIS_COLUMN, columnData.getRowIds()));
    }
    if (!DataUtils.isEmpty(rowData)) {
      filter.add(Filter.any(COL_ANALYSIS_ROW, rowData.getRowIds()));
    }

    BeeRowSet filterData = qs.getViewData(VIEW_ANALYSIS_FILTERS, filter);

    return new AnalysisFormData(headerData, columnData, rowData, filterData);
  }

  private SqlSelect addDimensions(SqlSelect query, String tblName) {
    if (getObserved() > 0) {
      query.addFromLeft(TBL_EXTRA_DIMENSIONS,
          sys.joinTables(TBL_EXTRA_DIMENSIONS, tblName, COL_EXTRA_DIMENSIONS));

      for (int ordinal = 1; ordinal <= getObserved(); ordinal++) {
        query.addFields(TBL_EXTRA_DIMENSIONS, getRelationColumn(ordinal));
      }
    }

    return query;
  }
}
