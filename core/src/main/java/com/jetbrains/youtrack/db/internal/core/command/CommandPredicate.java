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

import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;

/**
 * Command predicate to be evaluated against a record and a context.
 */
public interface CommandPredicate {

  /**
   * Evaluates the predicate.
   *
   * @param iRecord        Target record
   * @param iCurrentResult TODO
   * @param iContext       Context of execution
   * @return The result of predicate
   */
  Object evaluate(
      final Identifiable iRecord, EntityImpl iCurrentResult, final CommandContext iContext);
}