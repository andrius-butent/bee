package com.butent.bee.server.modules.transport;

import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.ViewCallback;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class TransportModuleBean implements BeeModule {

  private static Logger logger = Logger.getLogger(TransportModuleBean.class.getName());

  @EJB
  DataEditorBean deb;
  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;

  @Override
  public String dependsOn() {
    return CommonsConstants.COMMONS_MODULE;
  }

  @Override
  public ResponseObject doService(RequestInfo reqInfo) {
    ResponseObject response = null;
    String svc = reqInfo.getParameter(TransportConstants.TRANSPORT_METHOD);

    if (BeeUtils.same(svc, "UPDATE_KILOMETERS")) {
      BeeRowSet rs = BeeRowSet.restore(reqInfo.getParameter("Rowset"));
      String colName = rs.getColumnId(0);

      Double kmNew = null;
      Double km = BeeUtils.toDouble(rs.getRow(0).getString(0));
      boolean requiresScale = false;

      if (BeeUtils.equals(colName, "Kilometers")) {
        kmNew = km + BeeUtils
            .toDouble(Codec.beeDeserialize(reqInfo.getParameter("SpeedometerFrom")));
        requiresScale = true;

      } else {
        if (BeeUtils.equals(colName, "SpeedometerFrom")) {
          Double kmTo = BeeUtils
              .toDoubleOrNull(Codec.beeDeserialize(reqInfo.getParameter("SpeedometerTo")));

          if (kmTo != null) {
            kmNew = kmTo - km;
          }
        } else {
          kmNew = km - BeeUtils
              .toDouble(Codec.beeDeserialize(reqInfo.getParameter("SpeedometerFrom")));
        }
        requiresScale = (kmNew != null && kmNew < 0);
      }
      if (requiresScale) {
        Integer scale = qs.getInt(new SqlSelect().addFields("Vehicles", "Speedometer")
            .addFrom("TripRoutes").addFromInner("Trips",
                SqlUtils.join("TripRoutes", "Trip", "Trips", sys.getIdName("Trips")))
            .addFromInner("Vehicles",
                SqlUtils.join("Trips", "Vehicle", "Vehicles", sys.getIdName("Vehicles")))
            .setWhere(SqlUtils.equal("TripRoutes", sys.getIdName("TripRoutes"),
                rs.getRow(0).getId())));

        if (BeeUtils.isPositive(scale)) {
          if (kmNew < 0) {
            kmNew += scale;
          } else if (kmNew >= scale) {
            kmNew -= scale;
          }
        } else if (kmNew < 0) {
          kmNew = null;
        }
      }
      rs.getRow(0).preliminaryUpdate(1, BeeUtils.transform(kmNew));
      response = deb.commitRow(rs, true);

    } else {
      String msg = BeeUtils.concat(1, "Transport service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    return null;
  }

  @Override
  public String getName() {
    return TransportConstants.TRANSPORT_MODULE;
  }

  @Override
  public String getResourcePath() {
    return getName();
  }

  @Override
  public void init() {
    sys.registerViewCallback("TripRoutes", new ViewCallback(getName()) {
      @Override
      public void afterViewData(SqlSelect query, BeeRowSet rowset) {
        if (rowset.isEmpty()) {
          return;
        }
        int colIndex = rowset.getColumnIndex("Consumption");
        SimpleRowSet rs = getFuelConsumptions(query);

        for (int i = 0; i < rs.getNumberOfRows(); i++) {
          BeeRow row = rowset.getRowById(rs.getLong(i, 0));

          if (row != null) {
            row.setValue(colIndex, rs.getValue(i, 1));
          }
        }
      }
    });
  }

  private SimpleRowSet getFuelConsumptions(SqlSelect query) {
    String trips = "Trips";
    String routes = "TripRoutes";
    String fuel = "FuelConsumptions";
    String temps = "FuelTemperatures";
    String routeId = sys.getIdName(routes);

    IsExpression xpr =
        SqlUtils.divide(
            SqlUtils.plus(
                SqlUtils.nvl(
                    SqlUtils.multiply(SqlUtils.field(routes, "Kilometers"),
                        SqlUtils.sqlCase(SqlUtils.field(routes, "Season"), 0,
                            SqlUtils.field(fuel, "Summer"), SqlUtils.field(fuel, "Winter")),
                        SqlUtils.plus(1,
                            SqlUtils.divide(SqlUtils.nvl(SqlUtils.field(temps, "Rate"), 0), 100))),
                    0),
                SqlUtils.nvl(
                    SqlUtils.multiply(SqlUtils.field(routes, "Kilometers"),
                        SqlUtils.field(routes, "CargoWeight"),
                        SqlUtils.field(fuel, "TonneKilometer")),
                    0)),
            100);

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addField(routes, routeId, "Id")
        .addSum(xpr, "Consumption")
        .addFrom(routes)
        .addFromInner(query.resetFields().resetOrder().addFields(routes, routeId), "sub",
            SqlUtils.joinUsing(routes, "sub", routeId))
        .addFromInner(trips, SqlUtils.join(routes, "Trip", trips, sys.getIdName(trips)))
        .addFromInner(fuel, SqlUtils.and(SqlUtils.joinUsing(trips, fuel, "Vehicle"),
            SqlUtils.joinUsing(fuel, routes, "Fuel")))
        .addFromLeft(temps,
            SqlUtils.and(SqlUtils.join(fuel, sys.getIdName(fuel), temps, "Consumption"),
                SqlUtils.joinUsing(temps, routes, "Season"),
                SqlUtils.or(SqlUtils.isNull(temps, "TempFrom"),
                    SqlUtils.compare(SqlUtils.field(temps, "TempFrom"), Operator.LE,
                        SqlUtils.nvl(SqlUtils.field(routes, "Temperature"), 0))),
                SqlUtils.or(SqlUtils.isNull(temps, "TempTo"),
                    SqlUtils.compare(SqlUtils.field(temps, "TempTo"), Operator.GT,
                        SqlUtils.nvl(SqlUtils.field(routes, "Temperature"), 0)))))
        .setWhere(SqlUtils.and(
            SqlUtils.or(SqlUtils.isNull(fuel, "DateFrom"),
                SqlUtils.joinLessEqual(fuel, "DateFrom", routes, "Date")),
            SqlUtils.or(SqlUtils.isNull(fuel, "DateTo"),
                SqlUtils.joinMore(fuel, "DateTo", routes, "Date")),
            SqlUtils.notNull(routes, "Kilometers")))
        .addGroup(routes, routeId));

    return rs;
  }
}
