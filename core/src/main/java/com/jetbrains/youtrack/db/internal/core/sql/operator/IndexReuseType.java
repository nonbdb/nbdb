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
package com.jetbrains.youtrack.db.internal.core.sql.operator;

import com.jetbrains.youtrack.db.internal.core.index.Index;

/**
 * Represents hint how index can be used to calculate result of operator execution.
 *
 * @see QueryOperator#getIndexReuseType(Object, Object)
 */
public enum IndexReuseType {
  /**
   * Results of this operator can be calculated as intersection of results for left and right
   * operators.
   */
  INDEX_INTERSECTION,

  /**
   * Results of this operator can be calculated as union of results for left and right operators.
   */
  INDEX_UNION,

  /**
   * Index cna be used to calculate result of given operator.
   */
  NO_INDEX,

  /**
   * Result of execution of this operator can be replaced by call to one of {@link Index} methods.
   */
  INDEX_METHOD,
  /**
   * Result of execution of this operator can be replaced by call to one of {@link Index} methods
   * depending on user implementation.
   */
  INDEX_OPERATOR
}
