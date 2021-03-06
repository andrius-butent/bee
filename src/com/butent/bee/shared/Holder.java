package com.butent.bee.shared;

import java.util.Objects;

public class Holder<T> {

  public static <T> Holder<T> absent() {
    return new Holder<>(null);
  }

  public static <T> Holder<T> of(T value) {
    return new Holder<>(value);
  }

  private T value;

  public Holder(T value) {
    this.value = value;
  }

  public void clear() {
    this.value = null;
  }

  public boolean contains(T object) {
    return Objects.equals(get(), object);
  }

  public T get() {
    return value;
  }

  public boolean isNotNull() {
    return value != null;
  }

  public boolean isNull() {
    return value == null;
  }

  public void set(T v) {
    this.value = v;
  }
}
