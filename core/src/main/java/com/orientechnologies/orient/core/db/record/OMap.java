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
package com.orientechnologies.orient.core.db.record;

import com.orientechnologies.common.util.OSizeable;
import com.orientechnologies.orient.core.db.record.ORecordMultiValueHelper.MULTIVALUE_CONTENT_TYPE;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.record.YTRecord;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Lazy implementation of LinkedHashMap. It's bound to a source YTRecord object to keep track of
 * changes. This avoid to call the makeDirty() by hand when the map is changed.
 */
@SuppressWarnings({"serial"})
public class OMap extends OTrackedMap<YTIdentifiable> implements OSizeable {

  private final byte recordType;
  private ORecordMultiValueHelper.MULTIVALUE_CONTENT_TYPE multiValueStatus =
      MULTIVALUE_CONTENT_TYPE.EMPTY;
  private boolean autoConvertToRecord = true;

  public OMap(final ORecordElement iSourceRecord) {
    super(iSourceRecord);
    this.recordType = YTDocument.RECORD_TYPE;
  }

  public OMap(final YTDocument iSourceRecord, final byte iRecordType) {
    super(iSourceRecord);
    this.recordType = iRecordType;

    if (iSourceRecord != null) {
      if (!iSourceRecord.isLazyLoad())
      // SET AS NON-LAZY LOAD THE COLLECTION TOO
      {
        autoConvertToRecord = false;
      }
    }
  }

  public OMap(final YTDocument iSourceRecord, final Map<Object, YTIdentifiable> iOrigin) {
    this(iSourceRecord);
    if (iOrigin != null && !iOrigin.isEmpty()) {
      putAll(iOrigin);
    }
  }

  @Override
  public boolean containsValue(final Object o) {
    return super.containsValue(o);
  }

  @Override
  public YTIdentifiable get(final Object iKey) {
    if (iKey == null) {
      return null;
    }

    final String key = iKey.toString();

    if (autoConvertToRecord) {
      convertLink2Record(key);
    }

    return super.get(key);
  }

  @Override
  public YTIdentifiable put(final Object key, YTIdentifiable value) {
    if (multiValueStatus == MULTIVALUE_CONTENT_TYPE.ALL_RIDS
        && value instanceof YTRecord
        && !value.getIdentity().isNew())
    // IT'S BETTER TO LEAVE ALL RIDS AND EXTRACT ONLY THIS ONE
    {
      value = value.getIdentity();
    } else {
      multiValueStatus = ORecordMultiValueHelper.updateContentType(multiValueStatus, value);
    }

    return super.put(key, value);
  }

  @Override
  public Collection<YTIdentifiable> values() {
    return super.values();
  }

  @Override
  public YTIdentifiable remove(Object o) {
    final YTIdentifiable result = super.remove(o);
    if (size() == 0) {
      multiValueStatus = MULTIVALUE_CONTENT_TYPE.EMPTY;
    }
    return result;
  }

  @Override
  public void clear() {
    super.clear();
    multiValueStatus = MULTIVALUE_CONTENT_TYPE.EMPTY;
  }

  @Override
  public String toString() {
    return ORecordMultiValueHelper.toString(this);
  }

  public boolean isAutoConvertToRecord() {
    return autoConvertToRecord;
  }

  public void setAutoConvertToRecord(boolean convertToRecord) {
    this.autoConvertToRecord = convertToRecord;
  }

  /**
   * Convert the item with the received key to a record.
   *
   * @param iKey Key of the item to convert
   */
  private void convertLink2Record(final Object iKey) {
    if (multiValueStatus == MULTIVALUE_CONTENT_TYPE.ALL_RECORDS) {
      return;
    }

    final Object value;

    if (iKey instanceof YTRID) {
      value = iKey;
    } else {
      value = super.get(iKey);
    }

    if (value instanceof YTRID rid) {
      try {
        // OVERWRITE IT
        YTRecord record = rid.getRecord();
        ORecordInternal.unTrack(sourceRecord, rid);
        ORecordInternal.track(sourceRecord, record);
        super.putInternal(iKey, record);
      } catch (ORecordNotFoundException ignore) {
        // IGNORE THIS
      }
    }
  }

  public byte getRecordType() {
    return recordType;
  }

  public Iterator<YTIdentifiable> rawIterator() {
    return super.values().iterator();
  }

  @Override
  public int size() {
    return super.size();
  }
}
