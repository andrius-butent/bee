package com.butent.bee.client.dialog;

import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.safehtml.client.HasSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.utils.BeeCommand;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.client.widget.Html;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

public class DialogBox extends Popup implements HasHTML, HasSafeHtml {

  public interface Caption extends HasHTML, HasSafeHtml, IsWidget {
  }

  public static class CaptionImpl extends Html implements Caption {
    public CaptionImpl() {
      super();
    }

    public CaptionImpl(String html) {
      super(BeeUtils.trim(html));
    }
  }

  private static final String STYLE_DIALOG = "bee-DialogBox";

  private static final String STYLE_CAPTION = "Caption";
  private static final String STYLE_CONTENT = "Content";
  private static final String STYLE_CLOSE = "Close";

  private final Complex container = new Complex(Position.RELATIVE);
  private final Vertical layout = new Vertical();
  private final Caption caption;

  public DialogBox() {
    this(false);
  }

  public DialogBox(boolean autoHide) {
    this(autoHide, true, STYLE_DIALOG);
  }

  public DialogBox(boolean autoHide, boolean modal, String styleName) {
    this(autoHide, modal, new CaptionImpl(), styleName);
  }

  public DialogBox(boolean autoHide, boolean modal, Caption captionWidget, String styleName) {
    super(autoHide, modal, BeeUtils.ifString(styleName, STYLE_DIALOG));

    Assert.notNull(captionWidget);
    captionWidget.asWidget().addStyleName(STYLE_CAPTION);
    this.caption = captionWidget;

    this.layout.add(captionWidget);
    this.container.add(layout);

    BeeImage close = new BeeImage(Global.getImages().close(), new BeeCommand() {
      @Override
      public void execute() {
        hide();
      }
    });
    close.addStyleName(STYLE_CLOSE);
    this.container.add(close);
    
    enableDragging();
  }

  public DialogBox(Caption captionWidget, String styleName) {
    this(false, true, captionWidget, styleName);
  }

  public DialogBox(String html, String styleName) {
    this(new CaptionImpl(html), styleName);
  }

  public DialogBox(String html) {
    this(html, STYLE_DIALOG);
  }

  public Caption getCaption() {
    return caption;
  }

  public String getHTML() {
    return caption.getHTML();
  }

  @Override
  public String getIdPrefix() {
    return "dialog";
  }

  public String getText() {
    return caption.getText();
  }

  public void setHTML(SafeHtml html) {
    caption.setHTML(html);
  }

  public void setHTML(String html) {
    caption.setHTML(SafeHtmlUtils.fromTrustedString(html));
  }

  public void setText(String text) {
    caption.setText(text);
  }

  @Override
  public void setWidget(Widget w) {
    Assert.notNull(w);
    w.addStyleName(STYLE_CONTENT);
    layout.add(w);

    super.setWidget(container);
  }
  
  @Override
  protected boolean isCaptionEvent(NativeEvent event) {
    EventTarget target = event.getEventTarget();
    if (Element.is(target)) {
      return caption.asWidget().getElement().getParentElement().isOrHasChild(Element.as(target));
    }
    return false;
  }
}
