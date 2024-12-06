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

import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.id.RID;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.DocumentSerializer;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterCondition;
import java.util.List;
import java.util.stream.Stream;

/**
 * CONTAINSTEXT operator. Look if a text is contained in a property. This is usually used with the
 * FULLTEXT-INDEX for fast lookup at piece of text.
 */
public class QueryOperatorContainsText extends QueryTargetOperator {

  private boolean ignoreCase = true;

  public QueryOperatorContainsText(final boolean iIgnoreCase) {
    super("CONTAINSTEXT", 5, false);
    ignoreCase = iIgnoreCase;
  }

  public QueryOperatorContainsText() {
    super("CONTAINSTEXT", 5, false);
  }

  @Override
  public String getSyntax() {
    return "<left> CONTAINSTEXT[( noignorecase ] )] <right>";
  }

  /**
   * This is executed on non-indexed fields.
   */
  @Override
  public Object evaluateRecord(
      final Identifiable iRecord,
      EntityImpl iCurrentResult,
      final SQLFilterCondition iCondition,
      final Object iLeft,
      final Object iRight,
      CommandContext iContext,
      final DocumentSerializer serializer) {
    if (iLeft == null || iRight == null) {
      return false;
    }

    return iLeft.toString().indexOf(iRight.toString()) > -1;
  }

  public boolean isIgnoreCase() {
    return ignoreCase;
  }

  @Override
  public IndexReuseType getIndexReuseType(final Object iLeft, final Object iRight) {
    return IndexReuseType.INDEX_METHOD;
  }

  @Override
  public Stream<RawPair<Object, RID>> executeIndexQuery(
      CommandContext iContext, Index index, List<Object> keyParams, boolean ascSortOrder) {
    return null;
  }

  @Override
  public RID getBeginRidRange(DatabaseSession session, Object iLeft, Object iRight) {
    return null;
  }

  @Override
  public RID getEndRidRange(DatabaseSession session, Object iLeft, Object iRight) {
    return null;
  }
}
