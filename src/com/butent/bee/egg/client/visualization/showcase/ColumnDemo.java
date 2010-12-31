package com.butent.bee.egg.client.visualization.showcase;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.egg.client.layout.Vertical;
import com.butent.bee.egg.client.visualization.DataTable;
import com.butent.bee.egg.client.visualization.LegendPosition;
import com.butent.bee.egg.client.visualization.visualizations.corechart.ColumnChart;
import com.butent.bee.egg.client.visualization.visualizations.corechart.CoreChart;
import com.butent.bee.egg.client.visualization.visualizations.corechart.HorizontalAxisOptions;
import com.butent.bee.egg.client.visualization.visualizations.corechart.Options;
import com.butent.bee.egg.client.widget.BeeLabel;

public class ColumnDemo implements LeftTabPanel.WidgetProvider {
  public Widget getWidget() {
    Options options = CoreChart.createOptions();
    options.setHeight(240);
    options.setWidth(400);
    options.setFontSize(18);
    options.setTitle("Pajamos / Sąnaudos");
    options.setLegend(LegendPosition.NONE);

    HorizontalAxisOptions hAxisOptions = HorizontalAxisOptions.create();
    hAxisOptions.setSlantedText(true);
    options.setHAxisOptions(hAxisOptions);

    DataTable data = Showcase.getCompanyPerformance();
    ColumnChart viz = new ColumnChart(data, options);

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
