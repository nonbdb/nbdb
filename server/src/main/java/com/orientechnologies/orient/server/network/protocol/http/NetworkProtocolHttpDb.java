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
package com.orientechnologies.orient.server.network.protocol.http;

import com.jetbrains.youtrack.db.internal.client.binary.BinaryRequestExecutor;
import com.jetbrains.youtrack.db.internal.core.config.ContextConfiguration;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.orientechnologies.orient.server.OClientConnection;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.network.OServerNetworkListener;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostUploadSingleFile;
import com.orientechnologies.orient.server.network.protocol.http.command.post.ServerCommandPostImportDatabase;
import java.io.IOException;
import java.net.Socket;

public class NetworkProtocolHttpDb extends NetworkProtocolHttpAbstract {

  private static final int CURRENT_PROTOCOL_VERSION = 10;

  public NetworkProtocolHttpDb(OServer server) {
    super(server);
  }

  @Override
  public void config(
      final OServerNetworkListener iListener,
      final OServer iServer,
      final Socket iSocket,
      final ContextConfiguration iConfiguration)
      throws IOException {
    server = iServer;
    setName(
        "YouTrackDB HTTP Connection "
            + iSocket.getLocalAddress()
            + ":"
            + iSocket.getLocalPort()
            + "<-"
            + iSocket.getRemoteSocketAddress());

    super.config(iListener, server, iSocket, iConfiguration);
    cmdManager.registerCommand(new ServerCommandPostImportDatabase());
    cmdManager.registerCommand(new OServerCommandPostUploadSingleFile());

    connection.getData().serverInfo =
        iConfiguration.getValueAsString(GlobalConfiguration.NETWORK_HTTP_SERVER_INFO);
  }

  @Override
  public int getVersion() {
    return CURRENT_PROTOCOL_VERSION;
  }

  public String getType() {
    return "http";
  }

  @Override
  protected void afterExecution() throws InterruptedException {
    DatabaseRecordThreadLocal.instance().remove();
  }

  @Override
  public BinaryRequestExecutor executor(OClientConnection connection) {
    return null;
  }
}
