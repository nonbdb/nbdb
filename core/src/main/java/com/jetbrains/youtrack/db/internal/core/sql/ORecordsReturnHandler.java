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

package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class ORecordsReturnHandler implements OReturnHandler {

  private final Object returnExpression;
  private final CommandContext context;
  private List<Object> results;

  protected ORecordsReturnHandler(final Object returnExpression, final CommandContext context) {
    this.returnExpression = returnExpression;
    this.context = context;
  }

  @Override
  public void reset() {
    results = new ArrayList<Object>();
  }

  @Override
  public Object ret() {
    return results;
  }

  protected void storeResult(final EntityImpl result) {
    final EntityImpl processedResult = preprocess(result);

    results.add(evaluateExpression(processedResult));
  }

  protected abstract EntityImpl preprocess(final EntityImpl result);

  private Object evaluateExpression(final EntityImpl record) {
    if (returnExpression == null) {
      return record;
    } else {
      final Object itemResult;
      final EntityImpl wrappingDoc;
      context.setVariable("current", record);

      itemResult =
          OSQLHelper.getValue(returnExpression, ((YTIdentifiable) record).getRecord(), context);
      if (itemResult instanceof YTIdentifiable) {
        return itemResult;
      }

      // WRAP WITH ODOCUMENT TO BE TRANSFERRED THROUGH BINARY DRIVER
      return new EntityImpl("value", itemResult);
    }
  }
}