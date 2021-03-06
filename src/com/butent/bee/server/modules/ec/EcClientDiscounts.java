package com.butent.bee.server.modules.ec;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.modules.ec.EcConstants.EcSupplier;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EcClientDiscounts {

  private static final class Discount {

    private final int depth;

    private final Range<Long> timeRange;
    private final EcSupplier supplier;

    private final Double percent;
    private final Double price;

    private Discount(int depth, Range<Long> timeRange, EcSupplier supplier,
        Double percent, Double price) {

      this.depth = depth;

      this.timeRange = timeRange;
      this.supplier = supplier;

      this.percent = percent;
      this.price = price;
    }

    private int getDepth() {
      return depth;
    }

    private Double getPercent() {
      return percent;
    }

    private Double getPrice() {
      return price;
    }

    private boolean isValid(long time, EcSupplier ecs) {
      return (timeRange == null || timeRange.contains(time))
          && (supplier == null || supplier == ecs);
    }
  }

  private static List<Discount> filterDiscounts(List<Discount> input, EcSupplier supplier) {
    List<Discount> result = new ArrayList<>();

    long time = System.currentTimeMillis();
    for (Discount discount : input) {
      if (discount.isValid(time, supplier)) {
        result.add(discount);
      }
    }

    return result;
  }

  private static List<Discount> getCategoryDiscounts(ListMultimap<Long, Discount> input,
      Collection<Long> categories, Map<Long, Long> categoryParents, EcSupplier supplier) {

    List<Discount> result = new ArrayList<>();
    if (input == null || input.isEmpty() || BeeUtils.isEmpty(categories)) {
      return result;
    }

    Map<Long, List<Discount>> discountsByCategory = new HashMap<>();

    for (Long category : categories) {
      if (discountsByCategory.containsKey(category)) {
        continue;
      }

      if (input.containsKey(category)) {
        List<Discount> filtered = filterDiscounts(input.get(category), supplier);
        if (!filtered.isEmpty()) {
          discountsByCategory.put(category, filtered);
        }
      }

      if (!BeeUtils.isEmpty(categoryParents)) {
        for (Long parent = categoryParents.get(category); parent != null; parent =
            categoryParents.get(parent)) {
          if (discountsByCategory.containsKey(parent)) {
            break;
          }

          if (input.containsKey(parent)) {
            List<Discount> filtered = filterDiscounts(input.get(parent), supplier);
            if (!filtered.isEmpty()) {
              discountsByCategory.put(parent, filtered);
            }
          }
        }
      }
    }

    if (discountsByCategory.isEmpty()) {
      return result;
    }

    ImmutableSet<Long> keys = ImmutableSet.copyOf(discountsByCategory.keySet());
    if (keys.size() > 1 && !BeeUtils.isEmpty(categoryParents)) {
      for (Long category : keys) {
        for (Long parent = categoryParents.get(category); parent != null; parent =
            categoryParents.get(parent)) {
          if (discountsByCategory.containsKey(parent)) {
            discountsByCategory.remove(parent);
          }
        }
      }
    }

    for (List<Discount> discounts : discountsByCategory.values()) {
      result.addAll(discounts);
    }
    return result;
  }

  private static double getClientPrice(double base, List<Discount> discounts) {
    Double bestPrice = null;
    Integer minDepth = null;

    for (Discount discount : discounts) {
      if (minDepth == null || discount.getDepth() <= minDepth) {
        if (minDepth != null && discount.getDepth() < minDepth) {
          bestPrice = null;
        }
        minDepth = discount.getDepth();

        Double price;
        if (discount.getPrice() != null) {
          price = discount.getPrice();
        } else if (discount.getPercent() != null && base > BeeConst.DOUBLE_ZERO) {
          price = BeeUtils.minusPercent(base, discount.getPercent());
        } else {
          price = null;
        }

        if (price != null) {
          if (bestPrice == null) {
            bestPrice = price;
          } else {
            bestPrice = Math.min(bestPrice, price);
          }
        }
      }
    }

    return (bestPrice == null) ? base : bestPrice;
  }

  private static Range<Long> getTimeRange(Long lower, Long upper) {
    if (lower == null && upper == null) {
      return null;
    } else if (lower == null) {
      return Range.lessThan(upper);
    } else if (upper == null) {
      return Range.greaterThan(lower);
    } else {
      return Range.closedOpen(lower, upper);
    }
  }

  private static Double normalizePercent(Double percent) {
    return (percent == null) ? null : Math.min(percent, 100d);
  }

  private static Double normalizePrice(Double price) {
    return (price == null) ? null : Math.max(price, BeeConst.DOUBLE_ZERO);
  }

  private final Double defPercent;

  private final ListMultimap<Long, Discount> itemDiscounts = ArrayListMultimap.create();

  private final Map<Long, ListMultimap<Long, Discount>> brandAndCategoryDiscounts = new HashMap<>();

  private final ListMultimap<Long, Discount> brandDiscounts = ArrayListMultimap.create();

  private final ListMultimap<Long, Discount> categoryDiscounts = ArrayListMultimap.create();

  private final List<Discount> globalDiscounts = new ArrayList<>();

  public EcClientDiscounts(Double defPercent, List<SimpleRowSet> discounts) {
    super();

    this.defPercent = normalizePercent(defPercent);

    if (!BeeUtils.isEmpty(discounts)) {
      for (int i = 0; i < discounts.size(); i++) {
        SimpleRowSet rowSet = discounts.get(i);
        if (!DataUtils.isEmpty(rowSet)) {
          add(i, rowSet);
        }
      }
    }
  }

  public void applyTo(EcItem ecItem, Map<Long, Long> categoryParents) {
    List<Discount> discounts = new ArrayList<>();

    long id = ecItem.getArticleId();
    EcSupplier supplier = ecItem.getPriceSupplier();

    if (itemDiscounts.containsKey(id)) {
      List<Discount> filtered = filterDiscounts(itemDiscounts.get(id), supplier);
      if (!filtered.isEmpty()) {
        discounts.addAll(filtered);
      }
    }

    if (discounts.isEmpty()) {
      Long brand = ecItem.getBrand();
      Set<Long> categories = ecItem.getCategorySet();

      if (brand != null && brandAndCategoryDiscounts.containsKey(brand) && !categories.isEmpty()) {
        List<Discount> filtered = getCategoryDiscounts(brandAndCategoryDiscounts.get(brand),
            categories, categoryParents, supplier);
        if (!filtered.isEmpty()) {
          discounts.addAll(filtered);
        }
      }

      if (discounts.isEmpty()) {
        if (brand != null && brandDiscounts.containsKey(brand)) {
          List<Discount> filtered = filterDiscounts(brandDiscounts.get(brand), supplier);
          if (!filtered.isEmpty()) {
            discounts.addAll(filtered);
          }
        }

        if (!categories.isEmpty() && !categoryDiscounts.isEmpty()) {
          List<Discount> filtered = getCategoryDiscounts(categoryDiscounts, categories,
              categoryParents, supplier);
          if (!filtered.isEmpty()) {
            discounts.addAll(filtered);
          }
        }
      }
    }

    if (discounts.isEmpty() && !globalDiscounts.isEmpty()) {
      List<Discount> filtered = filterDiscounts(globalDiscounts, supplier);
      if (!filtered.isEmpty()) {
        discounts.addAll(filtered);
      }
    }

    if (!discounts.isEmpty()) {
      ecItem.setClientPrice(getClientPrice(ecItem.getRealClientPrice(), discounts));

    } else if (defPercent != null) {
      ecItem.setClientPrice(BeeUtils.minusPercent(ecItem.getRealClientPrice(), defPercent));
    }
  }

  public boolean hasCategories() {
    return !categoryDiscounts.isEmpty() || !brandAndCategoryDiscounts.isEmpty();
  }

  public boolean isEmpty() {
    return itemDiscounts.isEmpty() && brandAndCategoryDiscounts.isEmpty()
        && brandDiscounts.isEmpty() && categoryDiscounts.isEmpty()
        && globalDiscounts.isEmpty() && defPercent == null;
  }

  private void add(int depth, SimpleRowSet rowSet) {
    for (SimpleRow row : rowSet) {
      Long timeFrom = row.getLong(COL_DISCOUNT_DATE_FROM);
      Long timeTo = row.getLong(COL_DISCOUNT_DATE_TO);

      boolean ok;

      if (timeTo != null) {
        ok = timeTo > System.currentTimeMillis();
        if (ok && timeFrom != null) {
          ok = timeTo > timeFrom;
        }
      } else {
        ok = true;
      }

      if (ok) {
        Long brand = row.getLong(COL_DISCOUNT_BRAND);
        Long category = row.getLong(COL_DISCOUNT_CATEGORY);

        EcSupplier supplier =
            EnumUtils.getEnumByIndex(EcSupplier.class, row.getInt(COL_DISCOUNT_SUPPLIER));

        Long item = row.getLong(COL_DISCOUNT_ARTICLE);

        Double percent = row.getDouble(COL_DISCOUNT_PERCENT);
        Double price = (item == null) ? null : row.getDouble(COL_DISCOUNT_PRICE);

        ok = percent != null || price != null;
        if (ok) {
          Discount discount = new Discount(depth, getTimeRange(timeFrom, timeTo), supplier,
              normalizePercent(percent), normalizePrice(price));

          if (item != null) {
            itemDiscounts.put(item, discount);

          } else if (brand != null && category != null) {
            if (brandAndCategoryDiscounts.containsKey(brand)) {
              brandAndCategoryDiscounts.get(brand).put(category, discount);
            } else {
              ListMultimap<Long, Discount> cd = ArrayListMultimap.create();
              cd.put(category, discount);
              brandAndCategoryDiscounts.put(brand, cd);
            }

          } else if (brand != null) {
            brandDiscounts.put(brand, discount);

          } else if (category != null) {
            categoryDiscounts.put(category, discount);

          } else {
            globalDiscounts.add(discount);
          }
        }
      }
    }
  }
}
