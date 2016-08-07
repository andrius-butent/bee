package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TradeDocumentSums {

  private static final class Item {

    private double quantity;
    private double price;

    private double discount;
    private boolean discountIsPercent;

    private double vat;
    private boolean vatIsPercent;

    private Item() {
    }

    private double getAmount(int scale) {
      if (isValid()) {
        return round(price * quantity, scale);
      } else {
        return BeeConst.DOUBLE_ZERO;
      }
    }

    private double getDiscount(TradeDiscountMode discountMode, int amountScale, int priceScale,
        int discountScale) {

      if (discountMode == null || !isValid() || isZero(discount)) {
        return BeeConst.DOUBLE_ZERO;

      } else {
        switch (discountMode) {
          case FROM_AMOUNT:
            if (discountIsPercent) {
              return round(percent(getAmount(amountScale), discount), discountScale);
            } else {
              return round(discount, discountScale);
            }

          case FROM_PRICE:
            if (discountIsPercent) {
              return round(round(percent(price, discount), priceScale) * quantity, discountScale);
            } else {
              return round(discount * quantity, discountScale);
            }

          default:
            logger.severe("discount mode", discountMode, "not implemented");
            return BeeConst.DOUBLE_ZERO;
        }
      }
    }

    private double getVat(TradeVatMode vatMode, double base, int vatScale) {
      if (vatMode == null || !isValid() || isZero(vat)) {
        return BeeConst.DOUBLE_ZERO;

      } else if (vatIsPercent) {
        return round(vatMode.computePercent(base, vat), vatScale);

      } else {
        return round(vat, vatScale);
      }
    }

    private double getVatBase(TradeDiscountMode discountMode, int amountScale, int priceScale,
        int discountScale) {

      return getAmount(amountScale)
          - getDiscount(discountMode, amountScale, priceScale, discountScale);
    }

    private boolean isValid() {
      return !isZero(quantity) && !isZero(price);
    }

    private boolean setQuantity(Double value) {
      double x = normalize(value);

      if (eq(quantity, x)) {
        return false;
      } else {
        this.quantity = x;
        return true;
      }
    }

    private boolean setPrice(Double value) {
      double x = normalize(value);

      if (eq(price, x)) {
        return false;
      } else {
        this.price = x;
        return true;
      }
    }

    private boolean setDiscount(Double value) {
      double x = normalize(value);

      if (eq(discount, x)) {
        return false;
      } else {
        this.discount = x;
        return true;
      }
    }

    private boolean setDiscountIsPercent(Boolean value) {
      boolean b = normalize(value);

      if (discountIsPercent == b) {
        return false;
      } else {
        this.discountIsPercent = b;
        return true;
      }
    }

    private boolean setVat(Double value) {
      double x = normalize(value);

      if (eq(vat, x)) {
        return false;
      } else {
        this.vat = x;
        return true;
      }
    }

    private boolean setVatIsPercent(Boolean value) {
      boolean b = normalize(value);

      if (vatIsPercent == b) {
        return false;
      } else {
        this.vatIsPercent = b;
        return true;
      }
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(TradeDocumentSums.class);

  private static final int DEFAULT_AMOUNT_SCALE = 2;
  private static final int DEFAULT_PRICE_SCALE = 2;

  private static final int DEFAULT_DISCOUNT_SCALE = 2;
  private static final int DEFAULT_VAT_SCALE = -1;

  private static boolean eq(double x1, double x2) {
    return x1 == x2;
  }

  private static boolean isPositive(double x) {
    return x > BeeConst.DOUBLE_ZERO;
  }

  private static boolean isZero(double x) {
    return x == BeeConst.DOUBLE_ZERO;
  }

  private static boolean normalize(Boolean value) {
    return BeeUtils.isTrue(value);
  }

  private static double normalize(Double value) {
    return BeeUtils.isDouble(value) ? value : BeeConst.DOUBLE_ZERO;
  }

  private static double percent(double x, double p) {
    return x * p / 100d;
  }

  private static double round(double x, int scale) {
    if (scale >= 0 && scale <= BeeConst.MAX_SCALE && !isZero(x)) {
      return BeeUtils.round(x, scale);
    } else {
      return x;
    }
  }

  private int amountScale = DEFAULT_AMOUNT_SCALE;
  private int priceScale = DEFAULT_PRICE_SCALE;

  private int discountScale = DEFAULT_DISCOUNT_SCALE;
  private int vatScale = DEFAULT_VAT_SCALE;

  private double documentDiscount;

  private Long documentId;

  private TradeDiscountMode discountMode;
  private TradeVatMode vatMode;

  private final Map<Long, Item> items = new HashMap<>();

  public TradeDocumentSums() {
  }

  public void add(long id, Double quantity, Double price,
      Double discount, Boolean discountIsPercent, Double vat, Boolean vatIsPercent) {

    Item item = new Item();

    item.setQuantity(quantity);
    item.setPrice(price);

    item.setDiscount(discount);
    item.setDiscountIsPercent(discountIsPercent);

    item.setVat(vat);
    item.setVatIsPercent(vatIsPercent);

    items.put(id, item);
  }

  public void clear() {
    this.documentId = null;

    this.documentDiscount = BeeConst.DOUBLE_ZERO;

    this.discountMode = null;
    this.vatMode = null;

    clearItems();
  }

  public void clearItems() {
    items.clear();
  }

  public boolean delete(Long id) {
    return items.remove(id) != null;
  }

  public boolean delete(Collection<Long> ids) {
    boolean changed = false;

    if (ids != null) {
      for (Long id : ids) {
        changed |= delete(id);
      }
    }

    return changed;
  }

  public int getAmountScale() {
    return amountScale;
  }

  public int getPriceScale() {
    return priceScale;
  }

  public int getDiscountScale() {
    return discountScale;
  }

  public int getVatScale() {
    return vatScale;
  }

  public double getDocumentDiscount() {
    return documentDiscount;
  }

  public TradeDiscountMode getDiscountMode() {
    return discountMode;
  }

  public TradeVatMode getVatMode() {
    return vatMode;
  }

  public double getAmount() {
    if (items.isEmpty()) {
      return BeeConst.DOUBLE_ZERO;
    } else {
      return items.values().stream().mapToDouble(item -> item.getAmount(amountScale)).sum();
    }
  }

  public double getDiscount() {
    if (items.isEmpty() || discountMode == null) {
      return BeeConst.DOUBLE_ZERO;
    } else {
      return items.values().stream().mapToDouble(item -> item.getDiscount(discountMode,
          amountScale, priceScale, discountScale)).sum() + round(documentDiscount, discountScale);
    }
  }

  public double getVat() {
    if (items.isEmpty() || vatMode == null) {
      return BeeConst.DOUBLE_ZERO;
    }

    double dd = round(documentDiscount, discountScale);

    if (discountMode == null || isZero(dd)) {
      return items.values().stream().mapToDouble(item -> item.getVat(vatMode,
          item.getVatBase(discountMode, amountScale, priceScale, discountScale), vatScale)).sum();
    }

    double result = BeeConst.DOUBLE_ZERO;
    double baseSum = BeeConst.DOUBLE_ZERO;

    List<Item> baseItems = new ArrayList<>();
    List<Double> vatBases = new ArrayList<>();
    Set<Double> percentages = new HashSet<>();

    for (Item item : items.values()) {
      if (item.isValid()) {
        double base = item.getVatBase(discountMode, amountScale, priceScale, discountScale);

        baseItems.add(item);
        vatBases.add(base);

        baseSum += base;

        if (item.vatIsPercent && !isZero(item.vat)) {
          percentages.add(item.vat);
        } else {
          percentages.add(BeeConst.DOUBLE_ZERO);
        }
      }
    }

    if (!percentages.isEmpty()) {
      double percentage = (percentages.size() == 1)
          ? BeeUtils.peek(percentages) : BeeConst.DOUBLE_ZERO;

      if (!isZero(percentage)) {
        result = round(vatMode.computePercent(baseSum - dd, percentage), vatScale);

      } else {
        if (isPositive(baseSum) && baseSum > dd
            && percentages.stream().anyMatch(p -> !isZero(p))) {

          double factor = (baseSum - dd) / baseSum;
          double rebased = BeeConst.DOUBLE_ZERO;

          for (int i = 0; i < vatBases.size(); i++) {
            double base = vatBases.get(i);
            if (isPositive(base)) {
              double z = round(base * factor, discountScale);
              rebased += base - z;

              vatBases.set(i, z);
            }
          }

          double diff = dd - rebased;

          if (!isZero(diff)) {
            for (int i = vatBases.size() - 1; i >= 0; i--) {
              double base = vatBases.get(i);

              if (isPositive(base) && base > diff) {
                vatBases.set(i, base - diff);
                break;
              }
            }
          }
        }

        for (int i = 0; i < baseItems.size(); i++) {
          result += baseItems.get(i).getVat(vatMode, vatBases.get(i), vatScale);
        }
      }
    }

    return result;
  }

  public double getTotal() {
    double total = getAmount() - getDiscount();
    if (vatMode == TradeVatMode.PLUS) {
      total += getVat();
    }

    return total;
  }

  public double getItemAmount(Long id) {
    Item item = getItem(id);
    return (item == null) ? BeeConst.DOUBLE_ZERO : item.getAmount(amountScale);
  }

  public double getItemDiscount(Long id) {
    if (discountMode == null) {
      return BeeConst.DOUBLE_ZERO;

    } else {
      Item item = getItem(id);
      return (item == null) ? BeeConst.DOUBLE_ZERO
          : item.getDiscount(discountMode, amountScale, priceScale, discountScale);
    }
  }

  public double getItemVat(Long id) {
    if (vatMode == null) {
      return BeeConst.DOUBLE_ZERO;
    }

    Item item = getItem(id);
    if (item == null || !item.isValid() || isZero(item.vat)) {
      return BeeConst.DOUBLE_ZERO;
    }

    return item.getVat(vatMode,
        item.getVatBase(discountMode, amountScale, priceScale, discountScale), vatScale);
  }

  public double getItemTotal(Long id) {
    Item item = getItem(id);
    if (item == null || !item.isValid()) {
      return BeeConst.DOUBLE_ZERO;
    }

    double total = item.getVatBase(discountMode, amountScale, priceScale, discountScale);
    if (vatMode == TradeVatMode.PLUS) {
      total += item.getVat(vatMode, total, vatScale);
    }

    return total;
  }

  public void setAmountScale(int amountScale) {
    this.amountScale = amountScale;
  }

  public void setPriceScale(int priceScale) {
    this.priceScale = priceScale;
  }

  public void setDiscountScale(int discountScale) {
    this.discountScale = discountScale;
  }

  public void setVatScale(int vatScale) {
    this.vatScale = vatScale;
  }

  public boolean updateDocumentId(Long id) {
    if (Objects.equals(documentId, id)) {
      return false;
    } else {
      this.documentId = id;
      return true;
    }
  }

  public boolean updateDocumentDiscount(Double value) {
    double x = normalize(value);

    if (eq(documentDiscount, x)) {
      return false;
    } else {
      this.documentDiscount = x;
      return true;
    }
  }

  public boolean updateDiscountMode(TradeDiscountMode value) {
    if (discountMode == value) {
      return false;
    } else {
      this.discountMode = value;
      return true;
    }
  }

  public boolean updateVatMode(TradeVatMode value) {
    if (vatMode == value) {
      return false;
    } else {
      this.vatMode = value;
      return true;
    }
  }

  public boolean updateQuantity(long id, Double value) {
    Item item = getItem(id);
    return item != null && item.setQuantity(value);
  }

  public boolean updatePrice(long id, Double value) {
    Item item = getItem(id);
    return item != null && item.setPrice(value);
  }

  public boolean updateDiscount(long id, Double value) {
    Item item = getItem(id);
    return item != null && item.setDiscount(value);
  }

  public boolean updateDiscountIsPercent(long id, Boolean value) {
    Item item = getItem(id);
    return item != null && item.setDiscountIsPercent(value);
  }

  public boolean updateVat(long id, Double value) {
    Item item = getItem(id);
    return item != null && item.setVat(value);
  }

  public boolean updateVatIsPercent(long id, Boolean value) {
    Item item = getItem(id);
    return item != null && item.setVatIsPercent(value);
  }

  private Item getItem(long id) {
    Item item = items.get(id);
    if (item == null) {
      logger.warning(NameUtils.getName(this), "item", id, "not found");
    }

    return item;
  }
}
