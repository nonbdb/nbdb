package com.orientechnologies.orient.client.remote.message;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class OListGlobalConfigurationsResponse implements OBinaryResponse {

  private Map<String, String> configs;

  public OListGlobalConfigurationsResponse() {
  }

  public OListGlobalConfigurationsResponse(Map<String, String> configs) {
    super();
    this.configs = configs;
  }

  @Override
  public void write(DatabaseSessionInternal session, ChannelDataOutput channel,
      int protocolVersion, RecordSerializer serializer)
      throws IOException {
    channel.writeShort((short) configs.size());
    for (Entry<String, String> entry : configs.entrySet()) {
      channel.writeString(entry.getKey());
      channel.writeString(entry.getValue());
    }
  }

  @Override
  public void read(DatabaseSessionInternal db, ChannelDataInput network,
      OStorageRemoteSession session) throws IOException {
    configs = new HashMap<String, String>();
    final int num = network.readShort();
    for (int i = 0; i < num; ++i) {
      configs.put(network.readString(), network.readString());
    }
  }

  public Map<String, String> getConfigs() {
    return configs;
  }
}
