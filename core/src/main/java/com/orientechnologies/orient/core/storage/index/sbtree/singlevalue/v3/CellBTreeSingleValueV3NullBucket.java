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
package com.orientechnologies.orient.core.storage.index.sbtree.singlevalue.v3;

import com.orientechnologies.common.serialization.types.OShortSerializer;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.storage.cache.OCacheEntry;
import com.orientechnologies.orient.core.storage.impl.local.paginated.base.ODurablePage;

/**
 * Bucket which is intended to save values stored in sbtree under <code>null</code> key. Bucket has
 * following layout:
 *
 * <ol>
 *   <li>First byte is flag which indicates presence of value in bucket
 *   <li>Second byte indicates whether value is presented by link to the "bucket list" where actual
 *       value is stored or real value passed be user.
 *   <li>The rest is serialized value whether link or passed in value.
 * </ol>
 *
 * @since 4/15/14
 */
public final class CellBTreeSingleValueV3NullBucket extends ODurablePage {

  public CellBTreeSingleValueV3NullBucket(final OCacheEntry cacheEntry) {
    super(cacheEntry);
  }

  public void init() {
    setByteValue(NEXT_FREE_POSITION, (byte) 0);
  }

  public void setValue(final YTRID value) {

    setByteValue(NEXT_FREE_POSITION, (byte) 1);

    setShortValue(NEXT_FREE_POSITION + 1, (short) value.getClusterId());
    setLongValue(NEXT_FREE_POSITION + 1 + OShortSerializer.SHORT_SIZE, value.getClusterPosition());
  }

  public YTRID getValue() {
    if (getByteValue(NEXT_FREE_POSITION) == 0) {
      return null;
    }

    final int clusterId = getShortValue(NEXT_FREE_POSITION + 1);
    final long clusterPosition = getLongValue(NEXT_FREE_POSITION + 1 + OShortSerializer.SHORT_SIZE);
    return new YTRecordId(clusterId, clusterPosition);
  }

  public void removeValue() {
    setByteValue(NEXT_FREE_POSITION, (byte) 0);
  }
}
