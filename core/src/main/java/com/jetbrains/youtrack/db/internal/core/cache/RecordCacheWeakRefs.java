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

package com.jetbrains.youtrack.db.internal.core.cache;

import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.ORecordInternal;
import java.util.function.BiConsumer;

/**
 * Cache implementation that uses Soft References.
 */
public class RecordCacheWeakRefs extends
    OAbstractMapCache<ORIDsWeakValuesHashMap<RecordAbstract>>
    implements ORecordCache {

  private static final BiConsumer<YTRID, RecordAbstract> UNLOAD_RECORDS_CONSUMER =
      (rid, record) -> {
        ORecordInternal.unsetDirty(record);
        record.unload();
      };

  private static final BiConsumer<YTRID, RecordAbstract> UNLOAD_NOT_MODIFIED_RECORDS_CONSUMER =
      (rid, record) -> {
        if (!record.isDirtyNoLoading()) {
          record.unload();
        }
      };

  public RecordCacheWeakRefs() {
    super(new ORIDsWeakValuesHashMap<>());
  }

  @Override
  public RecordAbstract get(final YTRID rid) {
    if (!isEnabled()) {
      return null;
    }

    return cache.get(rid);
  }

  @Override
  public RecordAbstract put(final RecordAbstract record) {
    if (!isEnabled()) {
      return null;
    }
    return cache.put(record.getIdentity(), record);
  }

  @Override
  public RecordAbstract remove(final YTRID rid) {
    if (!isEnabled()) {
      return null;
    }
    return cache.remove(rid);
  }

  @Override
  public void unloadRecords() {
    cache.forEach(UNLOAD_RECORDS_CONSUMER);
  }

  @Override
  public void unloadNotModifiedRecords() {
    cache.forEach(UNLOAD_NOT_MODIFIED_RECORDS_CONSUMER);
  }

  @Override
  public void shutdown() {
    clear();
  }

  @Override
  public void clear() {
    cache.clear();
    cache = new ORIDsWeakValuesHashMap<>();
  }

  public void clearRecords() {
    for (Record rec : cache.values()) {
      rec.clear();
    }
  }
}