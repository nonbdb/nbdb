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
import com.jetbrains.youtrack.db.internal.core.storage.PhysicalPosition;
import com.orientechnologies.orient.client.binary.OBinaryRequestExecutor;
import com.orientechnologies.orient.client.remote.OBinaryRequest;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;

public class OHigherPhysicalPositionsRequest
    implements OBinaryRequest<OHigherPhysicalPositionsResponse> {

  private int clusterId;
  private PhysicalPosition clusterPosition;

  public OHigherPhysicalPositionsRequest(int iClusterId, PhysicalPosition iClusterPosition) {
    this.clusterId = iClusterId;
    this.clusterPosition = iClusterPosition;
  }

  public OHigherPhysicalPositionsRequest() {
  }

  @Override
  public void write(DatabaseSessionInternal database, ChannelDataOutput network,
      OStorageRemoteSession session) throws IOException {
    network.writeInt(clusterId);
    network.writeLong(clusterPosition.clusterPosition);
  }

  public void read(DatabaseSessionInternal db, ChannelDataInput channel, int protocolVersion,
      RecordSerializer serializer)
      throws IOException {
    clusterId = channel.readInt();
    clusterPosition = new PhysicalPosition(channel.readLong());
  }

  @Override
  public byte getCommand() {
    return ChannelBinaryProtocol.REQUEST_POSITIONS_HIGHER;
  }

  @Override
  public String getDescription() {
    return "Retrieve higher positions";
  }

  public int getClusterId() {
    return clusterId;
  }

  public PhysicalPosition getClusterPosition() {
    return clusterPosition;
  }

  @Override
  public OHigherPhysicalPositionsResponse createResponse() {
    return new OHigherPhysicalPositionsResponse();
  }

  @Override
  public OBinaryResponse execute(OBinaryRequestExecutor executor) {
    return executor.executeHigherPosition(this);
  }
}
