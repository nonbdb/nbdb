package com.orientechnologies.orient.client.remote.message;

import static com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration.NETWORK_BINARY_MIN_PROTOCOL_VERSION;
import static com.jetbrains.youtrack.db.internal.enterprise.channel.binary.OChannelBinaryProtocol.OLDEST_SUPPORTED_PROTOCOL_VERSION;

import com.jetbrains.youtrack.db.internal.common.log.OLogManager;
import com.jetbrains.youtrack.db.internal.core.exception.YTDatabaseException;

public class OBinaryProtocolHelper {

  public static void checkProtocolVersion(Object caller, int protocolVersion) {

    if (OLDEST_SUPPORTED_PROTOCOL_VERSION > protocolVersion) {
      String message =
          String.format(
              "Backward compatibility support available from to version %d your version is %d",
              OLDEST_SUPPORTED_PROTOCOL_VERSION, protocolVersion);
      OLogManager.instance().error(caller, message, null);
      throw new YTDatabaseException(message);
    }

    if (NETWORK_BINARY_MIN_PROTOCOL_VERSION.getValueAsInteger() > protocolVersion) {
      String message =
          String.format(
              "Backward compatibility support enabled from version %d your version is %d, check"
                  + " `%s` settings",
              NETWORK_BINARY_MIN_PROTOCOL_VERSION.getValueAsInteger(),
              protocolVersion,
              NETWORK_BINARY_MIN_PROTOCOL_VERSION.getKey());
      OLogManager.instance().error(caller, message, null);
      throw new YTDatabaseException(message);
    }
  }
}
