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
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.ODocumentSerializer;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterCondition;
import com.jetbrains.youtrack.db.internal.core.sql.operator.OIndexReuseType;
import com.jetbrains.youtrack.db.internal.core.sql.operator.OQueryOperator;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MULTIPLY "*" operator.
 */
public class OQueryOperatorMultiply extends OQueryOperator {

  public OQueryOperatorMultiply() {
    super("*", 10, false);
  }

  @Override
  public Object evaluateRecord(
      final YTIdentifiable iRecord,
      EntityImpl iCurrentResult,
      final OSQLFilterCondition iCondition,
      Object iLeft,
      Object iRight,
      CommandContext iContext,
      final ODocumentSerializer serializer) {
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
      Class maxPrecisionClass = getMaxPrecisionClass(l, r);
      if (Integer.class.equals(maxPrecisionClass)) {
        return tryDownscaleToInt(l.longValue() * r.longValue());
      } else if (Long.class.equals(maxPrecisionClass)) {
        return l.longValue() * r.longValue();
      } else if (Short.class.equals(maxPrecisionClass)) {
        return l.shortValue() * r.shortValue();
      } else if (Float.class.equals(maxPrecisionClass)) {
        return l.floatValue() * r.floatValue();
      } else if (Double.class.equals(maxPrecisionClass)) {
        return l.doubleValue() * r.doubleValue();
      } else if (BigDecimal.class.equals(maxPrecisionClass)) {
        return (toBigDecimal(l)).multiply(toBigDecimal(r));
      }
    }

    return null;
  }

  public static BigDecimal toBigDecimal(Number number) {
    if (number instanceof BigDecimal) {
      return (BigDecimal) number;
    }
    if (number instanceof Double) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    if (number instanceof Float) {
      return BigDecimal.valueOf(number.floatValue());
    }
    if (number instanceof Long) {
      return new BigDecimal(number.longValue());
    }
    if (number instanceof Integer) {
      return new BigDecimal(number.intValue());
    }
    if (number instanceof Short) {
      return new BigDecimal(number.intValue());
    }

    return null;
  }

  public static Class getMaxPrecisionClass(Number l, Number r) {
    Class<? extends Number> lClass = l.getClass();
    Class<? extends Number> rClass = r.getClass();
    if (lClass.equals(BigDecimal.class) || rClass.equals(BigDecimal.class)) {
      return BigDecimal.class;
    }
    if (lClass.equals(Double.class) || rClass.equals(Double.class)) {
      return Double.class;
    }
    if (lClass.equals(Float.class) || rClass.equals(Float.class)) {
      return Float.class;
    }
    if (lClass.equals(Long.class) || rClass.equals(Long.class)) {
      return Long.class;
    }
    if (lClass.equals(Integer.class) || rClass.equals(Integer.class)) {
      return Integer.class;
    }
    if (lClass.equals(Short.class) || rClass.equals(Short.class)) {
      return Short.class;
    }

    return null;
  }

  public static Object tryDownscaleToInt(long value) {
    if (value < Integer.MAX_VALUE && value > Integer.MIN_VALUE) {
      return (int) value;
    }
    return value;
  }

  @Override
  public OIndexReuseType getIndexReuseType(Object iLeft, Object iRight) {
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