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
package com.jetbrains.youtrack.db.internal.core.id;

import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.serialization.SerializableStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * RecordID interface that represents a recordid in database. RecordID are made of 2 numbers:
 * cluster id (cluster number) and cluster position (absolute position inside the cluster). Loading
 * a record by its RecordID allows O(1) performance, no matter the database size.
 */
public interface RID extends Identifiable, SerializableStream {

  char PREFIX = '#';
  char SEPARATOR = ':';
  int CLUSTER_MAX = 32767;
  int CLUSTER_ID_INVALID = -1;
  long CLUSTER_POS_INVALID = -1;
  long VERSION_POS_INVALID = -1;

  int getClusterId();

  long getClusterPosition();

  void reset();

  boolean isPersistent();

  boolean isValid();

  boolean isNew();

  boolean isTemporary();

  RID copy();

  String next();

  /**
   * Deprecated since v2.2
   */
  @Deprecated
  RID nextRid();

  int toStream(OutputStream iStream) throws IOException;

  StringBuilder toString(StringBuilder iBuffer);
}
