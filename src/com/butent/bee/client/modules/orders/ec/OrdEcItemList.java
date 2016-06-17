package com.butent.bee.client.modules.orders.ec;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.orders.ec.OrdEcItem;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class OrdEcItemList extends Flow {

  private static final String STYLE_PRIMARY = "ItemList";
  private static final String STYLE_WAREHOUSE = "warehouse";

  private static final String STYLE_HEADER_ROW = EcStyles.name(STYLE_PRIMARY, "header");
  private static final String STYLE_ITEM_ROW = EcStyles.name(STYLE_PRIMARY, "item");

  private static final String STYLE_PICTURE = EcStyles.name(STYLE_PRIMARY, "picture");
  private static final String STYLE_INFO = EcStyles.name(STYLE_PRIMARY, "info");
  private static final String STYLE_QUANTITY = EcStyles.name(STYLE_PRIMARY, "quantity");
  private static final String STYLE_PRICE = EcStyles.name(STYLE_PRIMARY, "price");
  private static final String STYLE_STOCK = EcStyles.name(STYLE_PRIMARY, "stock");

  private static final String STYLE_ITEM_NAME = EcStyles.name(STYLE_PRIMARY, "name");

  private static final int PAGE_SIZE = 50;

  Dictionary lc = Localized.dictionary();

  private final HtmlTable table;
  private final Button moreWidget;
  private String stockLabel;

  private final List<OrdEcItem> data = new ArrayList<>();

  public OrdEcItemList(List<OrdEcItem> items) {
    this();
    render(items);
  }

  private OrdEcItemList() {
    super();
    addStyleName(EcStyles.name(STYLE_PRIMARY));

    this.table = new HtmlTable();
    EcStyles.add(table, STYLE_PRIMARY, "table");
    add(table);

    this.moreWidget = new Button(Localized.dictionary().ecMoreItems());
    EcStyles.add(moreWidget, STYLE_PRIMARY, "more");
    moreWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showMoreItems();
      }
    });
    add(moreWidget);

    this.stockLabel = OrdEcKeeper.getStockLabel();
  }

  public void render(List<OrdEcItem> items) {
    if (!table.isEmpty()) {
      table.clear();
    }
    StyleUtils.hideDisplay(moreWidget);

    if (!data.isEmpty()) {
      data.clear();
    }

    if (!BeeUtils.isEmpty(items)) {
      data.addAll(items);
      int row = 0;
      int col = 0;

      if (items.size() > 1) {
        Label caption = new Label(BeeUtils.joinWords(Localized.dictionary().ecFoundItems(),
            BeeUtils.bracket(items.size())));
        EcStyles.add(caption, STYLE_PRIMARY, "caption");
        table.setWidget(row, col, caption);
      }

      Label photo = new Label(lc.photo());
      EcStyles.add(photo, STYLE_PRIMARY, "photoLabel");
      table.setWidget(row, col++, photo);

      Label article = new Label(lc.article());
      EcStyles.add(article, STYLE_PRIMARY, "articleLabel");
      table.setWidget(row, col++, article);

      Label name = new Label(lc.name());
      EcStyles.add(name, STYLE_PRIMARY, "nameLabel");
      table.setWidget(row, col++, name);

      Label unit = new Label(lc.unitShort());
      EcStyles.add(unit, STYLE_PRIMARY, "unitLabel");
      table.setWidget(row, col++, unit);

      Label price = new Label(lc.price());
      EcStyles.add(price, STYLE_PRIMARY, "priceLabel");
      table.setWidget(row, col++, price);

      if (hasWarehouse()) {
        Label wrh = new Label(stockLabel);
        EcStyles.add(wrh, STYLE_PRIMARY, STYLE_WAREHOUSE);
        table.setWidget(row, col++, wrh);

      } else {
        Label wrh = new Label("S1");
        EcStyles.add(wrh, STYLE_PRIMARY, STYLE_WAREHOUSE);
        table.setWidget(row, col++, wrh);
      }

      Label quantity = new Label(lc.quantity());
      EcStyles.add(quantity, STYLE_PRIMARY, "quantityLabel");
      table.setWidget(row, col++, quantity);

      table.getRowFormatter().addStyleName(row, STYLE_HEADER_ROW);

      Multimap<Long, OrdEcItemPicture> pictureWidgets = ArrayListMultimap.create();

      int pageSize = (items.size() > PAGE_SIZE * 3 / 2) ? PAGE_SIZE : items.size();

      row++;
      for (OrdEcItem item : items) {
        if (row > pageSize) {
          break;
        }

        OrdEcItemPicture pictureWidget = new OrdEcItemPicture(item.getCaption());

        renderItem(row++, item, pictureWidget);

        pictureWidgets.put(item.getId(), pictureWidget);
      }

      if (pageSize < items.size()) {
        StyleUtils.unhideDisplay(moreWidget);
      }

      if (!pictureWidgets.isEmpty()) {
        OrdEcKeeper.setBackgroundPictures(pictureWidgets);
      }
    }
  }

  private void renderItem(int row, OrdEcItem item, Widget pictureWidget) {
    int col = 0;

    if (pictureWidget != null) {
      table.setWidgetAndStyle(row, col, pictureWidget, STYLE_PICTURE);
    }
    col++;

    String article = item.getArticle();
    if (!BeeUtils.isEmpty(article)) {
      Label itemArticle = new Label(article);
      table.setWidgetAndStyle(row, col, itemArticle, STYLE_INFO);
    }
    col++;

    String name = item.getName();
    if (!BeeUtils.isEmpty(name)) {
      Label itemName = new Label(name);
      itemName.addStyleName(STYLE_ITEM_NAME);
      table.setWidgetAndStyle(row, col, itemName, STYLE_INFO);
    }
    col++;

    String unit = item.getUnit();
    if (!BeeUtils.isEmpty(name)) {
      Label itemUnit = new Label(unit);
      table.setWidgetAndStyle(row, col, itemUnit, STYLE_INFO);
    }
    col++;

    double price = item.getPrice();

    if (price > 0) {
      Label itemPrice = new Label(String.valueOf(price));
      itemPrice.addStyleName(STYLE_PRICE);
      table.setWidgetAndStyle(row, col, itemPrice, STYLE_PRICE);
    }
    col++;

    Label itemStock = new Label(item.getRemainder());
    itemStock.addStyleName(STYLE_STOCK);
    table.setWidgetAndStyle(row, col, itemStock, STYLE_STOCK);
    col++;

    OrdEcCartAccumulator accumulator = new OrdEcCartAccumulator(item, 1);
    table.setWidgetAndStyle(row, col++, accumulator, STYLE_QUANTITY);

    table.getRowFormatter().addStyleName(row, STYLE_ITEM_ROW);
  }

  private void showMoreItems() {
    int pageStart = table.getRowCount() - 1;
    int more = data.size() - pageStart;

    if (more > 0) {

      int pageSize = (more > PAGE_SIZE * 3 / 2) ? PAGE_SIZE : more;

      for (int i = pageStart; i < pageStart + pageSize; i++) {
        OrdEcItem item = data.get(i);

        OrdEcItemPicture pictureWidget = new OrdEcItemPicture(item.getCaption());
        renderItem(i + 1, item, pictureWidget);
      }

      if (pageSize >= more) {
        StyleUtils.hideDisplay(moreWidget);
      }
    }
  }

  private boolean hasWarehouse() {
    return !BeeUtils.isEmpty(stockLabel);
  }
}