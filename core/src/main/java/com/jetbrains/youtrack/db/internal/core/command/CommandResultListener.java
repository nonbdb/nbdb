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
package com.jetbrains.youtrack.db.internal.core.command;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;

/**
 * Callback interface called when the command returns results.
 */
public interface CommandResultListener {

  /**
   * This method is called for each result.
   *
   * @param querySession
   * @param iRecord      Current record
   * @return True to continue the query, otherwise false
   */
  boolean result(DatabaseSessionInternal querySession, Object iRecord);

  /**
   * Called at the end of processing. This is useful to clean-up local attributes.
   */
  void end();

  Object getResult();
}
