package com.orientechnologies.orient.client.remote.message;

import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataInput;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;

/**
 *
 */
public interface OBinaryPushResponse {

  void write(final OChannelDataOutput network) throws IOException;

  void read(OChannelDataInput channel) throws IOException;
}
