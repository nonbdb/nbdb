package com.orientechnologies.orient.client.remote.message;

import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.ORecordSerializer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.OChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;

public class OExistsDatabaseResponse implements OBinaryResponse {

  private boolean exists;

  public OExistsDatabaseResponse() {
  }

  public OExistsDatabaseResponse(boolean exists) {
    this.exists = exists;
  }

  @Override
  public void write(YTDatabaseSessionInternal session, OChannelDataOutput channel,
      int protocolVersion, ORecordSerializer serializer)
      throws IOException {
    channel.writeBoolean(exists);
  }

  @Override
  public void read(YTDatabaseSessionInternal db, OChannelDataInput network,
      OStorageRemoteSession session) throws IOException {
    exists = network.readBoolean();
  }

  public boolean isExists() {
    return exists;
  }
}
