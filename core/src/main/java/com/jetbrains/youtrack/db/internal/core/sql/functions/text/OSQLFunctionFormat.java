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
package com.jetbrains.youtrack.db.internal.core.sql.functions.text;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.sql.functions.OSQLFunctionAbstract;

/**
 * Formats content.
 */
public class OSQLFunctionFormat extends OSQLFunctionAbstract {

  public static final String NAME = "format";

  public OSQLFunctionFormat() {
    super(NAME, 1, -1);
  }

  public Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      Object iCurrentResult,
      final Object[] params,
      CommandContext iContext) {
    final Object[] args = new Object[params.length - 1];

    System.arraycopy(params, 1, args, 0, args.length);

    return String.format((String) params[0], args);
  }

  public String getSyntax(YTDatabaseSession session) {
    return "format(<format>, <arg1> [,<argN>]*)";
  }
}
