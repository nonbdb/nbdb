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
package com.jetbrains.youtrack.db.internal.common.util;

import com.jetbrains.youtrack.db.internal.core.config.StorageFileConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.storage.StorageCluster;
import com.jetbrains.youtrack.db.internal.core.storage.PhysicalPosition;
import com.jetbrains.youtrack.db.internal.core.storage.cache.PageDataVerificationError;
import com.jetbrains.youtrack.db.internal.core.storage.index.hashindex.local.HashTable;

public final class CommonConst {

  public static final long[] EMPTY_LONG_ARRAY = new long[0];
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final char[] EMPTY_CHAR_ARRAY = new char[0];
  public static final int[] EMPTY_INT_ARRAY = new int[0];
  public static final StorageCluster[] EMPTY_CLUSTER_ARRAY = new StorageCluster[0];
  public static final Identifiable[] EMPTY_IDENTIFIABLE_ARRAY = new Identifiable[0];
  public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  public static final PropertyType[] EMPTY_TYPES_ARRAY = new PropertyType[0];
  public static final PageDataVerificationError[] EMPTY_PAGE_DATA_VERIFICATION_ARRAY =
      new PageDataVerificationError[0];
  public static final HashTable.Entry[] EMPTY_BUCKET_ENTRY_ARRAY = new HashTable.Entry[0];
  public static final PhysicalPosition[] EMPTY_PHYSICAL_POSITIONS_ARRAY = new PhysicalPosition[0];
  public static final StorageFileConfiguration[] EMPTY_FILE_CONFIGURATIONS_ARRAY =
      new StorageFileConfiguration[0];

  private CommonConst() {
  }
}
