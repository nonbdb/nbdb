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
package com.orientechnologies.orient.server.network.protocol.http.command.all;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.api.exception.CommandScriptException;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.function.Function;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequestWrapper;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponse;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponseWrapper;
import com.orientechnologies.orient.server.network.protocol.http.OHttpUtils;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommandAuthenticatedDbAbstract;
import java.io.IOException;

public abstract class OServerCommandAbstractLogic extends OServerCommandAuthenticatedDbAbstract {

  @Override
  public boolean execute(final OHttpRequest iRequest, final OHttpResponse iResponse)
      throws Exception {
    final String[] parts = init(iRequest, iResponse);
    DatabaseSessionInternal db = null;

    try {
      db = getProfiledDatabaseInstance(iRequest);

      final Function f = db.getMetadata().getFunctionLibrary().getFunction(parts[2]);
      if (f == null) {
        throw new IllegalArgumentException("Function '" + parts[2] + "' is not configured");
      }

      if (iRequest.getHttpMethod().equalsIgnoreCase("GET") && !f.isIdempotent(db)) {
        iResponse.send(
            OHttpUtils.STATUS_BADREQ_CODE,
            OHttpUtils.STATUS_BADREQ_DESCRIPTION,
            OHttpUtils.CONTENT_TEXT_PLAIN,
            "GET method is not allowed to execute function '"
                + parts[2]
                + "' because has been declared as non idempotent. Use POST instead.",
            null);
        return false;
      }

      Object[] args = new String[parts.length - 3];
      System.arraycopy(parts, 3, args, 0, parts.length - 3);

      // BIND CONTEXT VARIABLES
      final BasicCommandContext context = new BasicCommandContext();
      context.setDatabase(db);
      context.setVariable(
          "session", server.getHttpSessionManager().getSession(iRequest.getSessionId()));
      context.setVariable("request", new OHttpRequestWrapper(iRequest, (String[]) args));
      context.setVariable("response", new OHttpResponseWrapper(iResponse));

      final Object functionResult;
      if (args.length == 0 && iRequest.getContent() != null && !iRequest.getContent().isEmpty()) {
        // PARSE PARAMETERS FROM CONTENT PAYLOAD
        try {
          final EntityImpl params = new EntityImpl();
          params.fromJSON(iRequest.getContent());
          functionResult = f.executeInContext(context, params.toMap());
        } catch (Exception e) {
          throw BaseException.wrapException(
              new CommandScriptException("Error on parsing parameters from request body"), e);
        }
      } else {
        functionResult = f.executeInContext(context, args);
      }

      handleResult(iRequest, iResponse, functionResult, db);

    } catch (CommandScriptException e) {
      // EXCEPTION
      final StringBuilder msg = new StringBuilder(256);
      for (Exception currentException = e;
          currentException != null;
          currentException = (Exception) currentException.getCause()) {
        if (msg.length() > 0) {
          msg.append("\n");
        }
        msg.append(currentException.getMessage());
      }

      if (isJsonResponse(iResponse)) {
        sendJsonError(
            iResponse,
            OHttpUtils.STATUS_BADREQ_CODE,
            OHttpUtils.STATUS_BADREQ_DESCRIPTION,
            OHttpUtils.CONTENT_TEXT_PLAIN,
            msg.toString(),
            null);
      } else {
        iResponse.send(
            OHttpUtils.STATUS_BADREQ_CODE,
            OHttpUtils.STATUS_BADREQ_DESCRIPTION,
            OHttpUtils.CONTENT_TEXT_PLAIN,
            msg.toString(),
            null);
      }

    } finally {
      if (db != null) {
        db.close();
      }
    }

    return false;
  }

  protected abstract String[] init(OHttpRequest iRequest, OHttpResponse iResponse);

  protected abstract void handleResult(
      OHttpRequest iRequest,
      OHttpResponse iResponse,
      Object iResult,
      DatabaseSessionInternal databaseDocumentInternal)
      throws InterruptedException, IOException;
}
