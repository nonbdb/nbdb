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
package com.jetbrains.youtrack.db.internal.core.sql.operator.math;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.id.RID;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.DocumentSerializer;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterCondition;
import com.jetbrains.youtrack.db.internal.core.sql.operator.IndexReuseType;
import com.jetbrains.youtrack.db.internal.core.sql.operator.QueryOperator;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MOD "%" operator.
 */
public class QueryOperatorMod extends QueryOperator {

  public QueryOperatorMod() {
    super("%", 10, false);
  }

  @Override
  public Object evaluateRecord(
      final Identifiable iRecord,
      EntityImpl iCurrentResult,
      final SQLFilterCondition iCondition,
      Object iLeft,
      Object iRight,
      CommandContext iContext,
      final DocumentSerializer serializer) {
    if (iRight == null || iLeft == null) {
      return null;
    }

    if (iLeft instanceof Date) {
      iLeft = ((Date) iLeft).getTime();
    }
    if (iRight instanceof Date) {
      iRight = ((Date) iRight).getTime();
    }

    if (iLeft instanceof Number l && iRight instanceof Number r) {
      if (l instanceof Integer) {
        return l.intValue() % r.intValue();
      } else if (l instanceof Long) {
        return l.longValue() % r.longValue();
      } else if (l instanceof Short) {
        return l.shortValue() % r.shortValue();
      } else if (l instanceof Float) {
        return l.floatValue() % r.floatValue();
      } else if (l instanceof Double) {
        return l.doubleValue() % r.doubleValue();
      } else if (l instanceof BigDecimal) {
        if (r instanceof BigDecimal) {
          return ((BigDecimal) l).remainder((BigDecimal) r);
        } else if (r instanceof Float) {
          return ((BigDecimal) l).remainder(BigDecimal.valueOf(r.floatValue()));
        } else if (r instanceof Double) {
          return ((BigDecimal) l).remainder(BigDecimal.valueOf(r.doubleValue()));
        } else if (r instanceof Long) {
          return ((BigDecimal) l).remainder(new BigDecimal(r.longValue()));
        } else if (r instanceof Integer) {
          return ((BigDecimal) l).remainder(new BigDecimal(r.intValue()));
        } else if (r instanceof Short) {
          return ((BigDecimal) l).remainder(new BigDecimal(r.shortValue()));
        }
      }
    }

    return null;
  }

  @Override
  public IndexReuseType getIndexReuseType(Object iLeft, Object iRight) {
    return IndexReuseType.NO_INDEX;
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
