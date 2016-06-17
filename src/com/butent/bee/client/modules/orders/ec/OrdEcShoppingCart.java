package com.butent.bee.client.modules.orders.ec;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputInteger;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.modules.orders.ec.OrdEcCart;
import com.butent.bee.shared.modules.orders.ec.OrdEcCartItem;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class OrdEcShoppingCart extends Split {

  private static final String STYLE_PRIMARY = EcStyles.name("shoppingCart");
  private static final String STYLE_ITEMS = STYLE_PRIMARY + "-items";

  private static final String STYLE_HEADER_ROW = STYLE_ITEMS + "-header";
  private static final String STYLE_ITEM_ROW = STYLE_PRIMARY + "-item";

  private static final String STYLE_COMMENT = STYLE_PRIMARY + "-comment";
  private static final String STYLE_COPY_BY_MAIL = STYLE_PRIMARY + "-copyByMail";

  private static final String STYLE_ITEM_PREFIX = STYLE_PRIMARY + "-item-";
  private static final String STYLE_PICTURE = STYLE_ITEM_PREFIX + "picture";
  private static final String STYLE_NAME = STYLE_ITEM_PREFIX + "name";
  private static final String STYLE_ARTICLE = STYLE_ITEM_PREFIX + "code";
  private static final String STYLE_QUANTITY = STYLE_ITEM_PREFIX + "quantity";
  private static final String STYLE_PRICE = STYLE_ITEM_PREFIX + "price";
  private static final String STYLE_REMOVE = STYLE_ITEM_PREFIX + "remove";

  private static final String STYLE_PANEL = "-panel";
  private static final String STYLE_LABEL = "-label";
  private static final String STYLE_INPUT = "-input";

  private static final int COL_PICTURE = 0;
  private static final int COL_NAME = 1;
  private static final int COL_ARTICLE = 2;
  private static final int COL_QUANTITY = 3;
  private static final int COL_PRICE = 4;
  private static final int COL_REMOVE = 5;

  private static final int SIZE_NORTH = 32;
  private static final int SIZE_SOUTH = 180;

  private final HtmlTable itemTable = new HtmlTable(STYLE_ITEMS + "-table");
  private final CustomDiv totalWidget = new CustomDiv(STYLE_PRIMARY + "-total");

  public OrdEcShoppingCart(OrdEcCart cart) {
    super(0);
    addStyleName(STYLE_PRIMARY);

    initNorth(cart);
    initSouth(cart);
    initCenter();

    renderItems(cart.getItems());
  }

  private void initCenter() {
    Simple wrapper = new Simple(itemTable);
    wrapper.addStyleName(STYLE_ITEMS + "-wrapper");

    add(wrapper);
  }

  private void initNorth(OrdEcCart cart) {
    Label caption = new Label(cart.getCaption());
    caption.addStyleName(STYLE_PRIMARY + "-caption");

    addNorth(caption, SIZE_NORTH);
  }

  private void initSouth(OrdEcCart cart) {
    Flow panel = new Flow(STYLE_PRIMARY + "-south");

    totalWidget.setHtml(renderTotal(cart));
    panel.add(totalWidget);

    Widget commentWidget = renderComment(cart);
    if (commentWidget != null) {
      panel.add(commentWidget);
    }

    final CheckBox copyByMail = new CheckBox(Localized.dictionary().ecOrderCopyByMail());
    copyByMail.addStyleName(STYLE_COPY_BY_MAIL);
    panel.add(copyByMail);

    Button submitWidget = new Button(Localized.dictionary().ecShoppingCartSubmit());
    submitWidget.addStyleName(STYLE_PRIMARY + "-submit");
    submitWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
      }
    });
    panel.add(submitWidget);

    addSouth(panel, SIZE_SOUTH);
  }

  private static Widget renderArticle(OrdEcCartItem item) {
    return new Label(item.getEcItem().getArticle());
  }

  private static Widget renderComment(OrdEcCart cart) {
    Flow panel = new Flow(STYLE_COMMENT + STYLE_PANEL);

    Label label = new Label(Localized.dictionary().comment());
    label.addStyleName(STYLE_COMMENT + STYLE_LABEL);
    panel.add(label);

    final InputArea input = new InputArea();
    input.addStyleName(STYLE_COMMENT + STYLE_INPUT);

    if (!BeeUtils.isEmpty(cart.getComment())) {
      input.setValue(BeeUtils.trim(cart.getComment()));
    }
    input.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        cart.setComment(Strings.emptyToNull(BeeUtils.trim(input.getValue())));
      }
    });

    panel.add(input);

    return panel;
  }

  private void renderItem(int row, OrdEcCartItem item, Widget pictureWidget) {
    if (pictureWidget != null) {
      itemTable.setWidgetAndStyle(row, COL_PICTURE, pictureWidget, STYLE_PICTURE);
    }

    Widget nameWidget = renderName(item);
    if (nameWidget != null) {
      itemTable.setWidgetAndStyle(row, COL_NAME, nameWidget, STYLE_NAME);
    }

    Widget codeWidget = renderArticle(item);
    if (codeWidget != null) {
      itemTable.setWidgetAndStyle(row, COL_ARTICLE, codeWidget, STYLE_ARTICLE);
    }

    Widget qtyWidget = renderQuantity(item);
    if (qtyWidget != null) {
      itemTable.setWidgetAndStyle(row, COL_QUANTITY, qtyWidget, STYLE_QUANTITY);
    }

    Widget priceWidget = renderPrice(item);
    if (priceWidget != null) {
      itemTable.setWidgetAndStyle(row, COL_PRICE, priceWidget, STYLE_PRICE);
    }

    Widget removeWidget = renderRemove(item);
    if (removeWidget != null) {
      itemTable.setWidgetAndStyle(row, COL_REMOVE, removeWidget, STYLE_REMOVE);
    }

    itemTable.getRowFormatter().addStyleName(row, STYLE_ITEM_ROW);
  }

  private void renderItems(List<OrdEcCartItem> items) {
    if (!itemTable.isEmpty()) {
      itemTable.clear();
    }

    if (!BeeUtils.isEmpty(items)) {
      int row = 0;

      Label nameLabel = new Label(Localized.dictionary().ecItemName());
      nameLabel.addStyleName(STYLE_NAME + STYLE_LABEL);
      itemTable.setWidget(row, COL_NAME, nameLabel);

      Label articleLabel = new Label(Localized.dictionary().article());
      articleLabel.addStyleName(STYLE_ARTICLE + STYLE_LABEL);
      itemTable.setWidget(row, COL_ARTICLE, articleLabel);

      Label qtyLabel = new Label(Localized.dictionary().ecItemQuantity());
      qtyLabel.addStyleName(STYLE_QUANTITY + STYLE_LABEL);
      itemTable.setWidget(row, COL_QUANTITY, qtyLabel);

      Label priceLabel = new Label(Localized.dictionary().ecItemPrice());
      priceLabel.addStyleName(STYLE_PRICE + STYLE_LABEL);
      itemTable.setWidget(row, COL_PRICE, priceLabel);

      Label removeLabel = new Label(Localized.dictionary().ecShoppingCartRemove());
      removeLabel.addStyleName(STYLE_REMOVE + STYLE_LABEL);
      itemTable.setWidget(row, COL_REMOVE, removeLabel);

      itemTable.getRowFormatter().addStyleName(row, STYLE_HEADER_ROW);

      Multimap<Long, OrdEcItemPicture> pictureWidgets = ArrayListMultimap.create();

      row++;
      for (OrdEcCartItem item : items) {
        OrdEcItemPicture pictureWidget = new OrdEcItemPicture(item.getEcItem().getCaption());

        renderItem(row++, item, pictureWidget);

        pictureWidgets.put(item.getEcItem().getId(), pictureWidget);
      }

      if (!pictureWidgets.isEmpty()) {
        OrdEcKeeper.setBackgroundPictures(pictureWidgets);
      }
    }
  }

  private static Widget renderName(final OrdEcCartItem item) {
    Label nameWidget = new Label(item.getEcItem().getName());

    nameWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        OrdEcKeeper.openItem(item.getEcItem(), false);
      }
    });

    return nameWidget;
  }

  private static Widget renderPrice(OrdEcCartItem item) {
    return new Label(EcUtils.formatCents(item.getEcItem().getPrice()));
  }

  private Widget renderQuantity(final OrdEcCartItem item) {
    String stylePrefix = STYLE_QUANTITY + "-";

    Horizontal panel = new Horizontal();

    final InputInteger input = new InputInteger();
    input.setValue(item.getQuantity());
    input.addStyleName(stylePrefix + "input");

    input.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        switch (event.getNativeKeyCode()) {
          case KeyCodes.KEY_ENTER:
            int value = input.getIntValue();
            if (value > 0 && DomUtils.isInView(input) && item.getQuantity() != value) {
              updateQuantity(item, value);
            }
            break;

          case KeyCodes.KEY_ESCAPE:
            if (DomUtils.isInView(input) && item.getQuantity() != input.getIntValue()) {
              input.setValue(item.getQuantity());
            }
            break;
        }
      }
    });

    panel.add(input);

    Flow spin = new Flow(stylePrefix + "spin");

    FaLabel plus = new FaLabel(FontAwesome.PLUS_SQUARE_O);
    plus.addStyleName(stylePrefix + "plus");

    plus.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        int value = item.getQuantity() + 1;

        input.setValue(value);
        updateQuantity(item, value);
      }
    });
    spin.add(plus);

    FaLabel minus = new FaLabel(FontAwesome.MINUS_SQUARE_O);
    minus.addStyleName(stylePrefix + "minus");

    minus.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        int value = item.getQuantity() - 1;

        if (value > 0) {
          input.setValue(value);
          updateQuantity(item, value);
        }
      }
    });
    spin.add(minus);

    panel.add(spin);

    return panel;
  }

  private Widget renderRemove(final OrdEcCartItem item) {
    Image remove = new Image(EcUtils.imageUrl("shoppingcart_remove.png"));
    remove.setAlt("remove");

    remove.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        OrdEcCart cart = OrdEcKeeper.removeFromCart(item.getEcItem());
        if (cart != null) {
          if (cart.isEmpty()) {
            OrdEcKeeper.closeView(OrdEcShoppingCart.this);
          } else {
            renderItems(cart.getItems());
            updateTotal(cart);
          }
        }
      }
    });

    return remove;
  }

  private static String renderTotal(OrdEcCart cart) {
    return BeeUtils.joinWords(Localized.dictionary().ecShoppingCartTotal(),
        EcUtils.formatCents(cart.totalCents()), EcConstants.CURRENCY);
  }

  private void updateQuantity(OrdEcCartItem item, int value) {
    item.setQuantity(value);

    BeeKeeper.getScreen().clearNotifications();
    BeeKeeper.getScreen().notifyInfo(
        Localized.dictionary()
            .ecUpdateCartItem(Localized.dictionary().ecShoppingCart(),
                item.getEcItem().getName(), value));

    OrdEcCart cart = OrdEcKeeper.refreshCart();
    updateTotal(cart);

    OrdEcKeeper.persistCartItem(item);
  }

  private void updateTotal(OrdEcCart cart) {
    if (cart != null) {
      totalWidget.setHtml(renderTotal(cart));
    }
  }
}