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
package com.orientechnologies.orient.core.sql.functions.coll;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterItem;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionConfigurableAbstract;

/**
 * Extract the first item of multi values (arrays, collections and maps) or return the same value
 * for non multi-value types.
 */
public class OSQLFunctionFirst extends OSQLFunctionConfigurableAbstract {

  public static final String NAME = "first";

  public OSQLFunctionFirst() {
    super(NAME, 1, 1);
  }

  public Object execute(
      Object iThis,
      final YTIdentifiable iCurrentRecord,
      Object iCurrentResult,
      final Object[] iParams,
      final OCommandContext iContext) {
    Object value = iParams[0];

    if (value instanceof OSQLFilterItem) {
      value = ((OSQLFilterItem) value).getValue(iCurrentRecord, iCurrentResult, iContext);
    }

    if (OMultiValue.isMultiValue(value)) {
      value = OMultiValue.getFirstValue(value);
    }

    return value;
  }

  public String getSyntax(YTDatabaseSession session) {
    return "first(<field>)";
  }
}
