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
package com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.delete;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpResponse;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpUtils;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.OHttpRequest;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.ServerCommandAuthenticatedDbAbstract;

public class ServerCommandDeleteClass extends ServerCommandAuthenticatedDbAbstract {

  private static final String[] NAMES = {"DELETE|class/*"};

  @Override
  public boolean execute(final OHttpRequest iRequest, HttpResponse iResponse) throws Exception {
    String[] urlParts =
        checkSyntax(iRequest.getUrl(), 3, "Syntax error: class/<database>/<class-name>");

    iRequest.getData().commandInfo = "Delete class";
    iRequest.getData().commandDetail = urlParts[2];

    try (DatabaseSessionInternal db = getProfiledDatabaseInstance(iRequest)) {

      if (db.getMetadata().getSchema().getClass(urlParts[2]) == null) {
        throw new IllegalArgumentException("Invalid class '" + urlParts[2] + "'");
      }

      db.getMetadata().getSchema().dropClass(urlParts[2]);

      iResponse.send(
          HttpUtils.STATUS_OK_CODE,
          HttpUtils.STATUS_OK_DESCRIPTION,
          HttpUtils.CONTENT_TEXT_PLAIN,
          null,
          null);

    }
    return false;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}
