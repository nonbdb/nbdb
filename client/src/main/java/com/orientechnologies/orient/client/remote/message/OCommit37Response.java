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
package com.orientechnologies.orient.client.remote.message;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RID;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.BonsaiCollectionPointer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OCommit37Response implements OBinaryResponse {

  private Map<UUID, BonsaiCollectionPointer> collectionChanges;
  private List<ObjectObjectImmutablePair<RID, RID>> updatedRids;

  public OCommit37Response(
      Map<RID, RID> updatedRids, Map<UUID, BonsaiCollectionPointer> collectionChanges) {
    super();
    this.updatedRids = new ArrayList<>(updatedRids.size());

    for (Map.Entry<RID, RID> entry : updatedRids.entrySet()) {
      this.updatedRids.add(new ObjectObjectImmutablePair<>(entry.getValue(), entry.getKey()));
    }

    this.collectionChanges = collectionChanges;
  }

  public OCommit37Response() {
  }

  @Override
  public void write(DatabaseSessionInternal session, ChannelDataOutput channel,
      int protocolVersion, RecordSerializer serializer)
      throws IOException {

    channel.writeInt(updatedRids.size());
    for (var pair : updatedRids) {
      channel.writeRID(pair.first());
      channel.writeRID(pair.second());
    }

    OMessageHelper.writeCollectionChanges(channel, collectionChanges);
  }

  @Override
  public void read(DatabaseSessionInternal db, ChannelDataInput network,
      OStorageRemoteSession session) throws IOException {

    int updatedRidsSize = network.readInt();
    updatedRids = new ArrayList<>(updatedRidsSize);
    for (int i = 0; i < updatedRidsSize; i++) {
      RID first = network.readRID();
      RID second = network.readRID();

      updatedRids.add(new ObjectObjectImmutablePair<>(first, second));
    }

    collectionChanges = OMessageHelper.readCollectionChanges(network);
  }

  public List<ObjectObjectImmutablePair<RID, RID>> getUpdatedRids() {
    return updatedRids;
  }

  public Map<UUID, BonsaiCollectionPointer> getCollectionChanges() {
    return collectionChanges;
  }
}
