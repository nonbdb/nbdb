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

import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.core.storage.OPhysicalPosition;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataInput;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;

public class OHigherPhysicalPositionsResponse implements OBinaryResponse {

  private OPhysicalPosition[] nextPositions;

  public OHigherPhysicalPositionsResponse() {
  }

  public OHigherPhysicalPositionsResponse(OPhysicalPosition[] nextPositions) {
    this.nextPositions = nextPositions;
  }

  @Override
  public void read(OChannelDataInput network, OStorageRemoteSession session) throws IOException {
    this.nextPositions = OMessageHelper.readPhysicalPositions(network);
  }

  public void write(ODatabaseSessionInternal session, OChannelDataOutput channel,
      int protocolVersion, ORecordSerializer serializer)
      throws IOException {
    OMessageHelper.writePhysicalPositions(channel, nextPositions);
  }

  public OPhysicalPosition[] getNextPositions() {
    return nextPositions;
  }
}
