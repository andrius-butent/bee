package com.butent.bee.egg.client.visualization.showcase;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.egg.client.layout.Vertical;
import com.butent.bee.egg.client.visualization.DataTable;
import com.butent.bee.egg.client.visualization.LegendPosition;
import com.butent.bee.egg.client.visualization.visualizations.corechart.AxisOptions;
import com.butent.bee.egg.client.visualization.visualizations.corechart.BarChart;
import com.butent.bee.egg.client.visualization.visualizations.corechart.Options;
import com.butent.bee.egg.client.widget.BeeLabel;

public class BarDemo implements LeftTabPanel.WidgetProvider {
  public Widget getWidget() {
    Options options = Options.create();
    options.setHeight(300);
    options.setWidth(400);
    options.setLegend(LegendPosition.TOP);

    AxisOptions hAxisOptions = AxisOptions.create();
    hAxisOptions.setTitle("Tūkstančiai");
    options.setHAxisOptions(hAxisOptions);

    AxisOptions vAxisOptions = AxisOptions.create();
    vAxisOptions.setTitle("Metai");
    options.setVAxisOptions(vAxisOptions);

    DataTable data = Showcase.getCompanyPerformance();
    BarChart viz = new BarChart(data, options);

    BeeLabel status = new BeeLabel();
    BeeLabel onMouseOverAndOutStatus = new BeeLabel();
    viz.addSelectHandler(new SelectionDemo(viz, status));
    viz.addReadyHandler(new ReadyDemo(status));
    viz.addOnMouseOverHandler(new OnMouseOverDemo(onMouseOverAndOutStatus));
    viz.addOnMouseOutHandler(new OnMouseOutDemo(onMouseOverAndOutStatus));

    Vertical result = new Vertical();
    result.add(status);
    result.add(viz);
    result.add(onMouseOverAndOutStatus);
    return result;
  }
}
