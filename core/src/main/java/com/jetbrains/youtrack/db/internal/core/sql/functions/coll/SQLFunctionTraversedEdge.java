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
package com.jetbrains.youtrack.db.internal.core.sql.functions.coll;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;

/**
 * Returns a traversed element from the stack. Use it with SQL traverse only.
 */
public class SQLFunctionTraversedEdge extends SQLFunctionTraversedElement {

  public static final String NAME = "traversedEdge";

  public SQLFunctionTraversedEdge() {
    super(NAME);
  }

  public Object execute(
      Object iThis,
      final Identifiable iCurrentRecord,
      Object iCurrentResult,
      final Object[] iParams,
      final CommandContext iContext) {
    return evaluate(iThis, iParams, iContext, "E");
  }
}
