/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.core.db.record;

import java.util.Objects;

/**
 * Event that contains information about operation that is performed on tracked collection.
 *
 * @param <K> Value that indicates position of item inside collection.
 * @param <V> Item value.
 */
public class MultiValueChangeEvent<K, V> {

  /**
   * Operation that is performed on collection.
   */
  public enum ChangeType {
    ADD,
    UPDATE,
    REMOVE,
    NESTED
  }

  /**
   * Operation that is performed on collection.
   */
  private final ChangeType changeType;

  /**
   * Value that indicates position of item inside collection.
   */
  private final K key;

  /**
   * New item value.
   */
  private final V value;

  /**
   * Previous item value.
   */
  private V oldValue;

  public MultiValueChangeEvent(ChangeType changeType, K key, V value) {
    this.changeType = changeType;
    this.key = key;
    this.value = value;
  }

  public MultiValueChangeEvent(ChangeType changeType, K key, V value, V oldValue) {
    this.changeType = changeType;
    this.key = key;
    this.value = value;
    this.oldValue = oldValue;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public V getOldValue() {
    return oldValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MultiValueChangeEvent<?, ?> that = (MultiValueChangeEvent<?, ?>) o;
    return changeType == that.changeType
        && Objects.equals(key, that.key)
        && Objects.equals(value, that.value)
        && Objects.equals(oldValue, that.oldValue);
  }

  @Override
  public int hashCode() {
    int result = changeType != null ? changeType.hashCode() : 0;
    result = 31 * result + (key != null ? key.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    result = 31 * result + (oldValue != null ? oldValue.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "MultiValueChangeEvent{"
        + "changeType="
        + changeType
        + ", key="
        + key
        + ", value="
        + value
        + ", oldValue="
        + oldValue
        + '}';
  }
}
