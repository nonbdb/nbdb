package com.jetbrains.youtrack.db.internal.core.metadata.schema.validation;

import java.util.Collection;

public class ValidationCollectionComparable implements Comparable<Object> {

  private final int size;

  public ValidationCollectionComparable(int size) {
    this.size = size;
  }

  @Override
  public int compareTo(Object o) {
    return size - ((Collection<Object>) o).size();
  }
}
