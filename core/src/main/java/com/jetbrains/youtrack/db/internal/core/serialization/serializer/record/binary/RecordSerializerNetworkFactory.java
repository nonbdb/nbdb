package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary;

import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;

/**
 *
 */
public class RecordSerializerNetworkFactory {

  public static RecordSerializerNetworkFactory INSTANCE = new RecordSerializerNetworkFactory();

  public RecordSerializer current() {
    return forProtocol(ChannelBinaryProtocol.CURRENT_PROTOCOL_VERSION);
  }

  public RecordSerializer forProtocol(int protocolNumber) {

    if (protocolNumber >= ChannelBinaryProtocol.PROTOCOL_VERSION_37) {
      return RecordSerializerNetworkV37.INSTANCE;
    } else {
      return RecordSerializerNetwork.INSTANCE;
    }
  }
}
