package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.modules.ec.EcConstants.EcSupplier;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArticleSupplier implements BeeSerializable {

  private enum Serial {
    SUPPLIER, SUPPLIER_ID, COST, LIST_PRICE, PRICE, REMAINDERS
  }

  public static ArticleSupplier restore(String s) {
    ArticleSupplier as = new ArticleSupplier();
    as.deserialize(s);
    return as;
  }

  private EcSupplier supplier;
  private String supplierId;

  private int cost;
  private int listPrice;
  private int price;

  private final Map<String, String> remainders = new HashMap<>();

  public ArticleSupplier(EcSupplier supplier, String supplierId, Double cost,
      Double listPrice, Double price) {

    this.supplier = supplier;
    this.supplierId = supplierId;

    setCost(cost);
    setListPrice(listPrice);
    setPrice(price);
  }

  private ArticleSupplier() {
  }

  public void addRemainder(String warehouse, Double remainder) {
    if (BeeUtils.isPositive(remainder)) {
      remainders.put(warehouse, BeeUtils.toString(remainder));
    }
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];
      if (value == null) {
        continue;
      }

      switch (member) {
        case SUPPLIER:
          setSupplier(EnumUtils.getEnumByIndex(EcSupplier.class, BeeUtils.toIntOrNull(value)));
          break;

        case SUPPLIER_ID:
          setSupplierId(value);
          break;

        case COST:
          setCost(BeeUtils.toInt(value));
          break;

        case LIST_PRICE:
          setListPrice(BeeUtils.toInt(value));
          break;

        case PRICE:
          setPrice(BeeUtils.toInt(value));
          break;

        case REMAINDERS:
          remainders.clear();
          remainders.putAll(Codec.deserializeLinkedHashMap(value));
          break;
      }
    }
  }

  public int getCost() {
    return cost;
  }

  public int getListPrice() {
    return listPrice;
  }

  public int getListPrice(Double marginPercent) {
    if (getListPrice() > 0) {
      return getListPrice();
    } else if (getPrice() > 0) {
      return getPrice();
    } else if (marginPercent == null || getCost() <= 0) {
      return getCost();
    } else {
      return BeeUtils.plusPercent(getCost(), marginPercent);
    }
  }

  public int getPrice() {
    return price;
  }

  public int getPrice(Double marginPercent) {
    if (getPrice() > 0) {
      return getPrice();
    } else {
      return getListPrice(marginPercent);
    }
  }

  public double getRealCost() {
    return cost / 100d;
  }

  public double getRealListPrice() {
    return listPrice / 100d;
  }

  public double getRealPrice() {
    return price / 100d;
  }

  public Map<String, String> getRemainders() {
    return remainders;
  }

  public double getStock(Collection<String> warehouses) {
    double stock = 0;

    if (!remainders.isEmpty()) {
      for (Map.Entry<String, String> entry : remainders.entrySet()) {
        if (warehouses.contains(entry.getKey())) {
          stock += BeeUtils.toDouble(entry.getValue());
        }
      }
    }

    return stock;
  }

  public EcSupplier getSupplier() {
    return supplier;
  }

  public String getSupplierId() {
    return supplierId;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case SUPPLIER:
          arr[i++] = getSupplier().ordinal();
          break;

        case SUPPLIER_ID:
          arr[i++] = getSupplierId();
          break;

        case COST:
          arr[i++] = getCost();
          break;

        case LIST_PRICE:
          arr[i++] = getListPrice();
          break;

        case PRICE:
          arr[i++] = getPrice();
          break;

        case REMAINDERS:
          arr[i++] = getRemainders();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setCost(Double cost) {
    setCost(BeeUtils.isDouble(cost) ? BeeUtils.round(cost * 100) : 0);
  }

  public void setCost(int cost) {
    this.cost = cost;
  }

  public void setListPrice(Double listPrice) {
    setListPrice(BeeUtils.isDouble(listPrice) ? BeeUtils.round(listPrice * 100) : 0);
  }

  public void setListPrice(int listPrice) {
    this.listPrice = listPrice;
  }

  public void setPrice(Double price) {
    setPrice(BeeUtils.isDouble(price) ? BeeUtils.round(price * 100) : 0);
  }

  public void setPrice(int price) {
    this.price = price;
  }

  public double totalStock() {
    double stock = 0;

    for (String remainder : remainders.values()) {
      stock += BeeUtils.toDouble(remainder);
    }

    return stock;
  }

  private void setSupplier(EcSupplier supplier) {
    this.supplier = supplier;
  }

  private void setSupplierId(String supplierId) {
    this.supplierId = supplierId;
  }
}
