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
package com.orientechnologies.orient.server.network.protocol.http.command;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponse;

/**
 * Generic interface for server-side commands.
 */
public interface OServerCommand {

  /**
   * Called before to execute. Useful to make checks.
   *
   * @param iResponse TODO
   */
  boolean beforeExecute(OHttpRequest iRequest, OHttpResponse iResponse) throws Exception;

  /**
   * Called after to execute. Useful to free resources.
   *
   * @param iResponse TODO
   */
  boolean afterExecute(OHttpRequest iRequest, OHttpResponse iResponse) throws Exception;

  /**
   * Executes the command requested.
   *
   * @return boolean value that indicates if this command is part of a chain
   */
  boolean execute(OHttpRequest iRequest, OHttpResponse iResponse) throws Exception;

  String[] getNames();

  void configure(OServer server);
}
