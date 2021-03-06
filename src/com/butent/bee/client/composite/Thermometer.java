package com.butent.bee.client.composite;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.HasProgress;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.DoubleLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.Progress;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

public class Thermometer extends Flow implements HasProgress {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Thermometer-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_PROGRESS = STYLE_PREFIX + "progress";
  private static final String STYLE_CANCEL = STYLE_PREFIX + "cancel";
  private static final String STYLE_PERCENT = STYLE_PREFIX + "percent";

  private final InlineLabel captionLabel;
  private final Progress progress;
  private final DoubleLabel percentLabel;

  public Thermometer(String label) {
    this(label, null);
  }

  public Thermometer(String label, Double max) {
    this(label, max, null);
  }

  public Thermometer(String label, Double max, IdentifiableWidget cancelWidget) {
    super(STYLE_CONTAINER);

    this.captionLabel = new InlineLabel();
    captionLabel.addStyleName(STYLE_LABEL);
    add(captionLabel);

    if (!BeeUtils.isEmpty(label)) {
      captionLabel.setText(label);
    }
    this.progress = (max == null) ? new Progress() : new Progress(max);
    progress.addStyleName(STYLE_PROGRESS);
    add(progress);

    if (cancelWidget != null) {
      cancelWidget.addStyleName(STYLE_CANCEL);
      add(cancelWidget);
    }

    this.percentLabel = new DoubleLabel(Format.getDefaultPercentFormat(), true);
    percentLabel.addStyleName(STYLE_PERCENT);
    add(percentLabel);
  }

  public double getValue() {
    return progress.getValue();
  }

  public void setMax(double max) {
    progress.setMax(max);
  }

  @Override
  public void update(double value) {
    this.update(null, value);
  }

  @Override
  public void update(String label, double value) {
    if (label != null) {
      captionLabel.setText(label);
    }
    progress.setValue(value);

    if (progress.getMax() > 0) {
      percentLabel.setValue(value / progress.getMax());
    }
  }
}
