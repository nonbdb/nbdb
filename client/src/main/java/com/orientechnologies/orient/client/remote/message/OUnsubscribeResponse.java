package com.orientechnologies.orient.client.remote.message;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;

/**
 *
 */
public class OUnsubscribeResponse implements OBinaryResponse {

  private final OBinaryResponse response;

  public OUnsubscribeResponse(OBinaryResponse response) {
    this.response = response;
  }

  @Override
  public void write(DatabaseSessionInternal session, ChannelDataOutput channel,
      int protocolVersion, RecordSerializer serializer)
      throws IOException {
    response.write(session, channel, protocolVersion, serializer);
  }

  @Override
  public void read(DatabaseSessionInternal db, ChannelDataInput network,
      OStorageRemoteSession session) throws IOException {
    response.read(db, network, session);
  }
}
