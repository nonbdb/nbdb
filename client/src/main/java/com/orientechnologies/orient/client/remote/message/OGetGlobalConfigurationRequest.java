package com.orientechnologies.orient.client.remote.message;

import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.orientechnologies.orient.client.binary.OBinaryRequestExecutor;
import com.orientechnologies.orient.client.remote.OBinaryRequest;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;

public class OGetGlobalConfigurationRequest
    implements OBinaryRequest<OGetGlobalConfigurationResponse> {

  private String key;

  public OGetGlobalConfigurationRequest(String key) {
    this.key = key;
  }

  public OGetGlobalConfigurationRequest() {
  }

  @Override
  public void write(DatabaseSessionInternal database, ChannelDataOutput network,
      OStorageRemoteSession session) throws IOException {
    network.writeString(key);
  }

  @Override
  public void read(DatabaseSessionInternal db, ChannelDataInput channel, int protocolVersion,
      RecordSerializer serializer)
      throws IOException {
    key = channel.readString();
  }

  @Override
  public byte getCommand() {
    return ChannelBinaryProtocol.REQUEST_CONFIG_GET;
  }

  @Override
  public String getDescription() {
    return "Get config";
  }

  public String getKey() {
    return key;
  }

  @Override
  public boolean requireDatabaseSession() {
    return false;
  }

  @Override
  public OGetGlobalConfigurationResponse createResponse() {
    return new OGetGlobalConfigurationResponse();
  }

  @Override
  public OBinaryResponse execute(OBinaryRequestExecutor executor) {
    return executor.executeGetGlobalConfiguration(this);
  }
}
