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

package com.jetbrains.youtrack.db.internal.core.storage.ridbag.ridbagbtree;

import com.jetbrains.youtrack.db.internal.common.serialization.types.BinarySerializer;
import com.jetbrains.youtrack.db.internal.core.storage.cache.ReadCache;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.TreeInternal;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.BonsaiCollectionPointer;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.Change;
import java.io.IOException;
import java.util.Map;

public interface EdgeBTree<K, V> extends TreeInternal<K, V> {

  /**
   * Gets id of file where this bonsai tree is stored.
   *
   * @return id of file in {@link ReadCache}
   */
  long getFileId();

  /**
   * @return the pointer to the root bucket in tree.
   */
  RidBagBucketPointer getRootBucketPointer();

  /**
   * @return pointer to a collection.
   */
  BonsaiCollectionPointer getCollectionPointer();

  /**
   * Search for entry with specific key and return its value.
   *
   * @param key
   * @return value associated with given key, NULL if no value is associated.
   */
  V get(K key);

  boolean put(AtomicOperation atomicOperation, K key, V value) throws IOException;

  /**
   * Deletes all entries from tree.
   *
   * @param atomicOperation
   */
  void clear(AtomicOperation atomicOperation) throws IOException;

  /**
   * Deletes whole tree. After this operation tree is no longer usable.
   *
   * @param atomicOperation
   */
  void delete(AtomicOperation atomicOperation);

  boolean isEmpty();

  V remove(AtomicOperation atomicOperation, K key) throws IOException;

  void loadEntriesMajor(
      K key, boolean inclusive, boolean ascSortOrder, RangeResultListener<K, V> listener);

  K firstKey();

  K lastKey();

  /**
   * Hardcoded method for Bag to avoid creation of extra layer.
   *
   * <p>Don't make any changes to tree.
   *
   * @param changes Bag changes
   * @return real bag size
   */
  int getRealBagSize(Map<K, Change> changes);

  BinarySerializer<K> getKeySerializer();

  BinarySerializer<V> getValueSerializer();
}
