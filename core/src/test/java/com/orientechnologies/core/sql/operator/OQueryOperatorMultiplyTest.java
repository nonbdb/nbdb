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
package com.orientechnologies.core.sql.operator;

import com.orientechnologies.core.serialization.serializer.record.binary.ORecordSerializerBinary;
import com.orientechnologies.core.sql.operator.math.OQueryOperatorMultiply;
import java.math.BigDecimal;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OQueryOperatorMultiplyTest {

  @Test
  public void test() {
    OQueryOperatorMultiply operator = new OQueryOperatorMultiply();
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        100);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10L,
            10L,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        100L);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10000000,
            10000,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        100000000000L); // upscale to long
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10.1,
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10.1 * 10);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            10.1,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10 * 10.1);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10.1d,
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10.1d * 10);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            10.1d,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        10 * 10.1d);
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            new BigDecimal(10),
            10,
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        new BigDecimal(10).multiply(new BigDecimal(10)));
    Assert.assertEquals(
        operator.evaluateRecord(
            null,
            null,
            null,
            10,
            new BigDecimal(10),
            null,
            ORecordSerializerBinary.INSTANCE.getCurrentSerializer()),
        new BigDecimal(10).multiply(new BigDecimal(10)));
  }
}
