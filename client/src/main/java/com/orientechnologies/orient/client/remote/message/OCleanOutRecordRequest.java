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

import com.orientechnologies.orient.client.binary.OBinaryRequestExecutor;
import com.orientechnologies.orient.client.remote.OBinaryAsyncRequest;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelBinaryProtocol;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataInput;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;

public class OCleanOutRecordRequest implements OBinaryAsyncRequest<OCleanOutRecordResponse> {

  private int recordVersion;
  private YTRecordId recordId;
  private byte mode;

  public OCleanOutRecordRequest() {
  }

  public OCleanOutRecordRequest(int recordVersion, YTRecordId recordId) {
    this.recordVersion = recordVersion;
    this.recordId = recordId;
  }

  @Override
  public byte getCommand() {
    return OChannelBinaryProtocol.REQUEST_RECORD_CLEAN_OUT;
  }

  @Override
  public String getDescription() {
    return "Clean out record";
  }

  public void read(YTDatabaseSessionInternal db, OChannelDataInput channel, int protocolVersion,
      ORecordSerializer serializer)
      throws IOException {
    recordId = channel.readRID();
    recordVersion = channel.readVersion();
    mode = channel.readByte();
  }

  @Override
  public void write(YTDatabaseSessionInternal database, OChannelDataOutput network,
      OStorageRemoteSession session) throws IOException {
    network.writeRID(recordId);
    network.writeVersion(recordVersion);
    network.writeByte(mode);
  }

  public byte getMode() {
    return mode;
  }

  public YTRecordId getRecordId() {
    return recordId;
  }

  public int getRecordVersion() {
    return recordVersion;
  }

  public void setMode(byte mode) {
    this.mode = mode;
  }

  @Override
  public OCleanOutRecordResponse createResponse() {
    return new OCleanOutRecordResponse();
  }

  @Override
  public OBinaryResponse execute(OBinaryRequestExecutor executor) {
    return executor.executeCleanOutRecord(this);
  }
}
