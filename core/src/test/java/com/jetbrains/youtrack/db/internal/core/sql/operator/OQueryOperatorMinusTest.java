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

import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.ORecordSerializerBinary;
import com.jetbrains.youtrack.db.internal.core.sql.operator.OQueryOperator;
import com.jetbrains.youtrack.db.internal.core.sql.operator.math.OQueryOperatorMinus;
import java.math.BigDecimal;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OQueryOperatorMinusTest {

  @Test
  public void test() {
    OQueryOperator operator = new OQueryOperatorMinus();
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        0);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10L,
            10L,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        0L);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        (((long) Integer.MIN_VALUE)) - Integer.MAX_VALUE); // upscale to long
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10.1,
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10.1 - 10);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            10.1,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10 - 10.1);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10.1d,
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10.1d - 10);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            10.1d,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10 - 10.1d);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            new BigDecimal(10),
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        new BigDecimal(10).subtract(new BigDecimal(10)));
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            new BigDecimal(10),
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        new BigDecimal(10).subtract(new BigDecimal(10)));
  }
}