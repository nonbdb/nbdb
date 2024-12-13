package com.orientechnologies.orient.server.network;

import com.jetbrains.youtrack.db.api.config.ContextConfiguration;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.SocketChannelBinary;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 */
public class MockPipeChannel extends SocketChannelBinary {

  public MockPipeChannel(InputStream in, OutputStream out) throws IOException {
    super(new Socket(), new ContextConfiguration());
    this.in = new DataInputStream(in);
    this.out = new DataOutputStream(out);
  }
}
