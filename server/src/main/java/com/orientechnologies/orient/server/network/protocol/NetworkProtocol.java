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
package com.orientechnologies.orient.server.network.protocol;

import com.jetbrains.youtrack.db.internal.client.binary.BinaryRequestExecutor;
import com.jetbrains.youtrack.db.internal.common.thread.SoftThread;
import com.jetbrains.youtrack.db.api.config.ContextConfiguration;
import com.jetbrains.youtrack.db.internal.enterprise.channel.SocketChannel;
import com.orientechnologies.orient.server.OClientConnection;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.network.OServerNetworkListener;
import java.io.IOException;
import java.net.Socket;

public abstract class NetworkProtocol extends SoftThread {

  protected OServer server;

  public NetworkProtocol(final ThreadGroup group, final String name) {
    super(group, name);
    setDumpExceptions(false);
  }

  public abstract void config(
      final OServerNetworkListener iListener,
      final OServer iServer,
      final Socket iSocket,
      ContextConfiguration iConfiguration)
      throws IOException;

  public abstract String getType();

  public abstract int getVersion();

  public abstract SocketChannel getChannel();

  public String getListeningAddress() {
    final SocketChannel c = getChannel();
    if (c != null) {
      return c.socket.getLocalAddress().getHostAddress();
    }
    return null;
  }

  public OServer getServer() {
    return server;
  }

  public abstract BinaryRequestExecutor executor(OClientConnection connection);
}
