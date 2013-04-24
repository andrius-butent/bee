package com.butent.bee.client.visualization.showcase;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.visualization.visualizations.ImageBarChart;
import com.butent.bee.client.visualization.visualizations.ImageBarChart.Options;

/**
 * Implements demonstration of image bar chart visualization.
 */

public class ImageBarDemo implements LeftTabPanel.WidgetProvider {

  @Override
  public Widget getWidget() {
    Options options = Options.create();
    options.setValueLabelsInterval(300);
    return new ImageBarChart(Showcase.getCompanyPerformance(), options);
  }
}
