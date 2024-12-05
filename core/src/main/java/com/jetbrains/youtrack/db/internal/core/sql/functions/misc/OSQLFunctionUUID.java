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
package com.jetbrains.youtrack.db.internal.core.sql.functions.misc;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.sql.functions.OSQLFunctionAbstract;
import java.util.UUID;

/**
 * Generates a UUID as a 128-bits value using the Leach-Salz variant. For more information look at:
 * http://docs.oracle.com/javase/6/docs/api/java/util/UUID.html.
 */
public class OSQLFunctionUUID extends OSQLFunctionAbstract {

  public static final String NAME = "uuid";

  /**
   * Get the date at construction to have the same date for all the iteration.
   */
  public OSQLFunctionUUID() {
    super(NAME, 0, 0);
  }

  public Object execute(
      Object iThis,
      final YTIdentifiable iCurrentRecord,
      final Object iCurrentResult,
      final Object[] iParams,
      CommandContext iContext) {
    return UUID.randomUUID().toString();
  }

  public boolean aggregateResults(final Object[] configuredParameters) {
    return false;
  }

  public String getSyntax(YTDatabaseSession session) {
    return "uuid()";
  }

  @Override
  public Object getResult() {
    return null;
  }
}
