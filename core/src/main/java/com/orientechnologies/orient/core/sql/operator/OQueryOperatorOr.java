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
package com.orientechnologies.orient.core.sql.operator;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ODocumentSerializer;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterCondition;

/**
 * OR operator.
 */
public class OQueryOperatorOr extends OQueryOperator {

  public OQueryOperatorOr() {
    super("OR", 3, false);
  }

  @Override
  public Object evaluateRecord(
      final YTIdentifiable iRecord,
      YTDocument iCurrentResult,
      final OSQLFilterCondition iCondition,
      final Object iLeft,
      final Object iRight,
      OCommandContext iContext,
      final ODocumentSerializer serializer) {
    if (iLeft == null) {
      return false;
    }
    return (Boolean) iLeft || (Boolean) iRight;
  }

  @Override
  public OIndexReuseType getIndexReuseType(final Object iLeft, final Object iRight) {
    if (iLeft == null || iRight == null) {
      return OIndexReuseType.NO_INDEX;
    }
    return OIndexReuseType.INDEX_UNION;
  }

  @Override
  public YTRID getBeginRidRange(YTDatabaseSession session, final Object iLeft,
      final Object iRight) {
    final YTRID leftRange;
    final YTRID rightRange;

    if (iLeft instanceof OSQLFilterCondition) {
      leftRange = ((OSQLFilterCondition) iLeft).getBeginRidRange(session);
    } else {
      leftRange = null;
    }

    if (iRight instanceof OSQLFilterCondition) {
      rightRange = ((OSQLFilterCondition) iRight).getBeginRidRange(session);
    } else {
      rightRange = null;
    }

    if (leftRange == null || rightRange == null) {
      return null;
    } else {
      return leftRange.compareTo(rightRange) <= 0 ? leftRange : rightRange;
    }
  }

  @Override
  public YTRID getEndRidRange(YTDatabaseSession session, final Object iLeft, final Object iRight) {
    final YTRID leftRange;
    final YTRID rightRange;

    if (iLeft instanceof OSQLFilterCondition) {
      leftRange = ((OSQLFilterCondition) iLeft).getEndRidRange(session);
    } else {
      leftRange = null;
    }

    if (iRight instanceof OSQLFilterCondition) {
      rightRange = ((OSQLFilterCondition) iRight).getEndRidRange(session);
    } else {
      rightRange = null;
    }

    if (leftRange == null || rightRange == null) {
      return null;
    } else {
      return leftRange.compareTo(rightRange) >= 0 ? leftRange : rightRange;
    }
  }

  @Override
  public boolean canShortCircuit(Object l) {
    return Boolean.TRUE.equals(l);
  }
}
