package com.jetbrains.youtrack.db.internal.core.metadata.schema.validation;

public class ValidationStringComparable implements Comparable<Object> {

  private final int size;

  public ValidationStringComparable(int size) {
    this.size = size;
  }

  @Override
  public int compareTo(Object o) {
    return size - o.toString().length();
  }
}
