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
import com.orientechnologies.orient.core.sql.filter.OSQLFilterCondition;
import java.util.Collection;

/**
 * CONTAINS ALL operator.
 */
public class OQueryOperatorContainsAll extends OQueryOperatorEqualityNotNulls {

  public OQueryOperatorContainsAll() {
    super("CONTAINSALL", 5, false);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected boolean evaluateExpression(
      final YTIdentifiable iRecord,
      final OSQLFilterCondition iCondition,
      final Object iLeft,
      final Object iRight,
      OCommandContext iContext) {
    final OSQLFilterCondition condition;

    var database = iContext.getDatabase();
    if (iCondition.getLeft() instanceof OSQLFilterCondition) {
      condition = (OSQLFilterCondition) iCondition.getLeft();
    } else if (iCondition.getRight() instanceof OSQLFilterCondition) {
      condition = (OSQLFilterCondition) iCondition.getRight();
    } else {
      condition = null;
    }

    if (iLeft.getClass().isArray()) {
      if (iRight.getClass().isArray()) {
        // ARRAY VS ARRAY
        int matches = 0;
        for (final Object l : (Object[]) iLeft) {
          for (final Object r : (Object[]) iRight) {
            if (OQueryOperatorEquals.equals(database, l, r)) {
              ++matches;
              break;
            }
          }
        }
        return matches == ((Object[]) iRight).length;
      } else if (iRight instanceof Collection<?>) {
        // ARRAY VS ARRAY
        int matches = 0;
        for (final Object l : (Object[]) iLeft) {
          for (final Object r : (Collection<?>) iRight) {
            if (OQueryOperatorEquals.equals(database, l, r)) {
              ++matches;
              break;
            }
          }
        }
        return matches == ((Collection<?>) iRight).size();
      }

    } else if (iLeft instanceof Collection<?>) {

      final Collection<YTDocument> collection = (Collection<YTDocument>) iLeft;

      if (condition != null) {
        // CHECK AGAINST A CONDITION
        for (final YTDocument o : collection) {
          if (condition.evaluate(o, null, iContext) == Boolean.FALSE) {
            return false;
          }
        }
      } else {
        // CHECK AGAINST A SINGLE VALUE
        for (final Object o : collection) {
          if (!OQueryOperatorEquals.equals(database, iRight, o)) {
            return false;
          }
        }
      }
    } else if (iRight instanceof Collection<?>) {

      // CHECK AGAINST A CONDITION
      final Collection<YTDocument> collection = (Collection<YTDocument>) iRight;

      if (condition != null) {
        for (final YTDocument o : collection) {
          if (condition.evaluate(o, null, iContext) == Boolean.FALSE) {
            return false;
          }
        }
      } else {
        // CHECK AGAINST A SINGLE VALUE
        for (final Object o : collection) {
          if (!OQueryOperatorEquals.equals(database, iLeft, o)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public OIndexReuseType getIndexReuseType(final Object iLeft, final Object iRight) {
    return OIndexReuseType.NO_INDEX;
  }

  @Override
  public YTRID getBeginRidRange(YTDatabaseSession session, Object iLeft, Object iRight) {
    return null;
  }

  @Override
  public YTRID getEndRidRange(YTDatabaseSession session, Object iLeft, Object iRight) {
    return null;
  }
}
