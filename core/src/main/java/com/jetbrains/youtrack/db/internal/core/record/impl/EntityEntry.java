/*
 *
 *  *  Copyright YouTrackDB
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
package com.jetbrains.youtrack.db.internal.core.record.impl;

import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.OMultiValueChangeTimeLine;
import com.jetbrains.youtrack.db.internal.core.db.record.OTrackedMultiValue;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTProperty;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;

/**
 * Document entry. Used by EntityImpl.
 */
public class EntityEntry {

  public Object value;
  public Object original;
  public YTType type;
  public YTProperty property;
  private boolean changed = false;
  private boolean exists = true;
  private boolean created = false;
  private boolean txChanged = false;
  private boolean txExists = true;
  private boolean txCreated = false;
  private Object onLoadValue;
  private boolean hasOnLoadValue = false;

  public EntityEntry() {
  }

  public boolean isChanged() {
    return changed;
  }

  public void setChanged(final boolean changed) {
    this.changed = changed;
    checkAndStoreOnLoadValue();
  }

  public boolean exists() {
    return exists;
  }

  public void setExists(final boolean exists) {
    this.exists = exists;
    this.txExists = exists;
  }

  public boolean isCreated() {
    return created;
  }

  @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "MethodDoesntCallSuperMethod"})
  protected EntityEntry clone() {
    final EntityEntry entry = new EntityEntry();
    entry.type = type;
    entry.property = property;
    entry.value = value;
    entry.changed = changed;
    entry.created = created;
    entry.exists = exists;
    entry.txChanged = changed;
    entry.txCreated = created;
    entry.txExists = exists;
    return entry;
  }

  public OMultiValueChangeTimeLine<Object, Object> getTimeLine() {
    //noinspection rawtypes
    if (!changed && value instanceof OTrackedMultiValue trackedMultiValue) {
      //noinspection unchecked
      return trackedMultiValue.getTimeLine();
    } else {
      return null;
    }
  }

  public void clear() {
    this.created = false;
    this.changed = false;
    original = null;
    removeTimeline();
  }

  public void clearNotExists() {
    original = null;
    removeTimeline();
  }

  public void removeTimeline() {
    //noinspection rawtypes
    if (value instanceof OTrackedMultiValue trackedMultiValue) {
      trackedMultiValue.disableTracking(null);
    }
  }

  public void replaceListener(EntityImpl document) {
    enableTracking(document);
  }

  public boolean enableTracking(EntityImpl document) {
    //noinspection rawtypes
    if (value instanceof OTrackedMultiValue trackedMultiValue) {
      trackedMultiValue.enableTracking(document);
      return true;
    } else {
      return false;
    }
  }

  public void disableTracking(EntityImpl document, Object fieldValue) {
    //noinspection rawtypes
    if (fieldValue instanceof OTrackedMultiValue trackedMultiValue) {
      trackedMultiValue.disableTracking(document);
    }
  }

  public boolean isTrackedModified() {
    //noinspection rawtypes
    if (value instanceof OTrackedMultiValue trackedMultiValue) {
      return trackedMultiValue.isModified();
    }
    if (value instanceof EntityImpl document && document.isEmbedded()) {
      return document.isDirty();
    }
    return false;
  }

  public boolean isTxTrackedModified() {
    //noinspection rawtypes
    if (value instanceof OTrackedMultiValue trackedMultiValue) {
      return trackedMultiValue.isTransactionModified();
    }
    if (value instanceof EntityImpl document && document.isEmbedded()) {
      return document.isDirty();
    }
    return false;
  }

  public void markChanged() {
    this.changed = true;
    this.txChanged = true;
    checkAndStoreOnLoadValue();
  }

  public void unmarkChanged() {
    this.changed = false;
  }

  public void markCreated() {
    this.created = true;
    this.txCreated = true;
  }

  public void unmarkCreated() {
    this.created = false;
  }

  public void undo() {
    if (changed) {
      this.value = original;
      unmarkChanged();
      original = null;
      exists = true;
    }
  }

  public void transactionClear() {
    //noinspection rawtypes
    if (value instanceof OTrackedMultiValue trackedMultiValue) {
      trackedMultiValue.transactionClear();
    }
    this.txCreated = false;
    this.txChanged = false;
    this.hasOnLoadValue = false;
    this.onLoadValue = null;
  }

  public Object getOnLoadValue(YTDatabaseSessionInternal session) {
    if (!hasOnLoadValue && !(value instanceof OTrackedMultiValue<?, ?>)) {
      return value;
    }

    if (hasOnLoadValue) {
      //noinspection rawtypes
      if (onLoadValue instanceof OTrackedMultiValue trackedOnLoadValue) {
        //noinspection rawtypes
        OMultiValueChangeTimeLine transactionTimeLine = trackedOnLoadValue.getTransactionTimeLine();
        //noinspection unchecked
        return transactionTimeLine != null
            ? trackedOnLoadValue.returnOriginalState(session,
            transactionTimeLine.getMultiValueChangeEvents())
            : onLoadValue;
      } else {
        return onLoadValue;
      }
    } else {
      //noinspection rawtypes
      OTrackedMultiValue trackedOnLoadValue = (OTrackedMultiValue) value;
      //noinspection rawtypes
      OMultiValueChangeTimeLine transactionTimeLine = trackedOnLoadValue.getTransactionTimeLine();
      //noinspection unchecked
      return transactionTimeLine != null
          ? trackedOnLoadValue.returnOriginalState(session,
          transactionTimeLine.getMultiValueChangeEvents())
          : trackedOnLoadValue;
    }
  }

  public boolean isTxChanged() {
    return txChanged;
  }

  public boolean isTxCreated() {
    return txCreated;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isTxExists() {
    return txExists;
  }

  private void checkAndStoreOnLoadValue() {
    if (changed && !hasOnLoadValue) {
      onLoadValue = original;
      hasOnLoadValue = true;
    }
  }
}