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
package com.jetbrains.youtrack.db.internal.core.sql.method;

import com.jetbrains.youtrack.db.internal.common.collection.OMultiValue;
import com.jetbrains.youtrack.db.internal.common.io.OIOUtils;
import com.jetbrains.youtrack.db.internal.common.parser.BaseParser;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.command.YTCommandExecutorNotFoundException;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.exception.YTRecordNotFoundException;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.OStringSerializerHelper;
import com.jetbrains.youtrack.db.internal.core.sql.CommandSQL;
import com.jetbrains.youtrack.db.internal.core.sql.OSQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.OSQLHelper;
import com.jetbrains.youtrack.db.internal.core.sql.YTCommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemAbstract;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemField;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemVariable;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLPredicate;
import com.jetbrains.youtrack.db.internal.core.sql.functions.OSQLFunctionRuntime;
import java.util.List;

/**
 * Wraps function managing the binding of parameters.
 */
public class OSQLMethodRuntime extends OSQLFilterItemAbstract
    implements Comparable<OSQLMethodRuntime> {

  public OSQLMethod method;
  public Object[] configuredParameters;
  public Object[] runtimeParameters;

  public OSQLMethodRuntime(YTDatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iText) {
    super(session, iQueryToParse, iText);
  }

  public OSQLMethodRuntime(final OSQLMethod iFunction) {
    method = iFunction;
  }

  /**
   * Execute a method.
   *
   * @param iCurrentRecord Current record
   * @param iCurrentResult TODO
   * @param iContext
   * @return
   */
  public Object execute(
      final Object iThis,
      final YTIdentifiable iCurrentRecord,
      final Object iCurrentResult,
      final CommandContext iContext) {
    if (iThis == null) {
      return null;
    }

    if (configuredParameters != null) {
      // RESOLVE VALUES USING THE CURRENT RECORD
      for (int i = 0; i < configuredParameters.length; ++i) {
        runtimeParameters[i] = configuredParameters[i];

        if (method.evaluateParameters()) {
          if (configuredParameters[i] instanceof OSQLFilterItemField) {
            runtimeParameters[i] =
                ((OSQLFilterItemField) configuredParameters[i])
                    .getValue(iCurrentRecord, iCurrentResult, iContext);
            if (runtimeParameters[i] == null && iCurrentResult instanceof YTIdentifiable)
            // LOOK INTO THE CURRENT RESULT
            {
              runtimeParameters[i] =
                  ((OSQLFilterItemField) configuredParameters[i])
                      .getValue((YTIdentifiable) iCurrentResult, iCurrentResult, iContext);
            }
          } else if (configuredParameters[i] instanceof OSQLMethodRuntime) {
            runtimeParameters[i] =
                ((OSQLMethodRuntime) configuredParameters[i])
                    .execute(iThis, iCurrentRecord, iCurrentResult, iContext);
          } else if (configuredParameters[i] instanceof OSQLFunctionRuntime) {
            runtimeParameters[i] =
                ((OSQLFunctionRuntime) configuredParameters[i])
                    .execute(iCurrentRecord, iCurrentRecord, iCurrentResult, iContext);
          } else if (configuredParameters[i] instanceof OSQLFilterItemVariable) {
            runtimeParameters[i] =
                ((OSQLFilterItemVariable) configuredParameters[i])
                    .getValue(iCurrentRecord, iCurrentResult, iContext);
            if (runtimeParameters[i] == null && iCurrentResult instanceof YTIdentifiable)
            // LOOK INTO THE CURRENT RESULT
            {
              runtimeParameters[i] =
                  ((OSQLFilterItemVariable) configuredParameters[i])
                      .getValue((YTIdentifiable) iCurrentResult, iCurrentResult, iContext);
            }
          } else if (configuredParameters[i] instanceof CommandSQL) {
            try {
              runtimeParameters[i] =
                  ((CommandSQL) configuredParameters[i]).setContext(iContext)
                      .execute(iContext.getDatabase());
            } catch (YTCommandExecutorNotFoundException ignore) {
              // TRY WITH SIMPLE CONDITION
              final String text = ((CommandSQL) configuredParameters[i]).getText();
              final SQLPredicate pred = new SQLPredicate(iContext, text);
              runtimeParameters[i] =
                  pred.evaluate(
                      iCurrentRecord instanceof Record ? iCurrentRecord : null,
                      (EntityImpl) iCurrentResult,
                      iContext);
              // REPLACE ORIGINAL PARAM
              configuredParameters[i] = pred;
            }
          } else if (configuredParameters[i] instanceof SQLPredicate) {
            runtimeParameters[i] =
                ((SQLPredicate) configuredParameters[i])
                    .evaluate(
                        iCurrentRecord.getRecord(),
                        (iCurrentRecord instanceof EntityImpl ? (EntityImpl) iCurrentResult
                            : null),
                        iContext);
          } else if (configuredParameters[i] instanceof String) {
            if (configuredParameters[i].toString().startsWith("\"")
                || configuredParameters[i].toString().startsWith("'")) {
              runtimeParameters[i] = OIOUtils.getStringContent(configuredParameters[i]);
            }
          }
        }
      }

      var db = iContext.getDatabase();
      if (method.getMaxParams(db) == -1 || method.getMaxParams(db) > 0) {
        if (runtimeParameters.length < method.getMinParams()
            || (method.getMaxParams(db) > -1 && runtimeParameters.length > method.getMaxParams(
            db))) {
          String params;
          if (method.getMinParams() == method.getMaxParams(db)) {
            params = "" + method.getMinParams();
          } else {
            params = method.getMinParams() + "-" + method.getMaxParams(db);
          }
          throw new YTCommandExecutionException(
              "Syntax error: function '"
                  + method.getName()
                  + "' needs "
                  + (params)
                  + " argument(s) while has been received "
                  + runtimeParameters.length);
        }
      }
    }

    final Object functionResult =
        method.execute(iThis, iCurrentRecord, iContext, iCurrentResult, runtimeParameters);

    return transformValue(iCurrentRecord, iContext, functionResult);
  }

  @Override
  public Object getValue(
      final YTIdentifiable iRecord, Object iCurrentResult, CommandContext iContext) {
    try {
      final EntityImpl current = iRecord != null ? (EntityImpl) iRecord.getRecord() : null;
      return execute(current, current, null, iContext);
    } catch (YTRecordNotFoundException rnf) {
      return null;
    }
  }

  @Override
  public String getRoot(YTDatabaseSession session) {
    return method.getName();
  }

  @Override
  protected void setRoot(YTDatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iText) {
    final int beginParenthesis = iText.indexOf('(');

    // SEARCH FOR THE FUNCTION
    final String funcName = iText.substring(0, beginParenthesis);

    final List<String> funcParamsText = OStringSerializerHelper.getParameters(iText);

    method = OSQLEngine.getMethod(funcName);
    if (method == null) {
      throw new YTCommandSQLParsingException("Unknown method " + funcName + "()");
    }

    // PARSE PARAMETERS
    this.configuredParameters = new Object[funcParamsText.size()];
    for (int i = 0; i < funcParamsText.size(); ++i) {
      this.configuredParameters[i] = funcParamsText.get(i);
    }

    setParameters(session, configuredParameters, true);
  }

  public OSQLMethodRuntime setParameters(YTDatabaseSessionInternal session,
      final Object[] iParameters, final boolean iEvaluate) {
    if (iParameters != null) {
      var context = new BasicCommandContext();
      context.setDatabase(session);

      this.configuredParameters = new Object[iParameters.length];
      for (int i = 0; i < iParameters.length; ++i) {
        this.configuredParameters[i] = iParameters[i];

        if (iParameters[i] != null) {
          if (iParameters[i] instanceof String && !iParameters[i].toString().startsWith("[")) {
            final Object v = OSQLHelper.parseValue(null, null, iParameters[i].toString(), context);
            if (v == OSQLHelper.VALUE_NOT_PARSED
                || (OMultiValue.isMultiValue(v)
                && OMultiValue.getFirstValue(v) == OSQLHelper.VALUE_NOT_PARSED)) {
              continue;
            }

            configuredParameters[i] = v;
          }
        } else {
          this.configuredParameters[i] = null;
        }
      }

      // COPY STATIC VALUES
      this.runtimeParameters = new Object[configuredParameters.length];
      for (int i = 0; i < configuredParameters.length; ++i) {
        if (!(configuredParameters[i] instanceof OSQLFilterItemField)
            && !(configuredParameters[i] instanceof OSQLMethodRuntime)) {
          runtimeParameters[i] = configuredParameters[i];
        }
      }
    }

    return this;
  }

  public OSQLMethod getMethod() {
    return method;
  }

  public Object[] getConfiguredParameters() {
    return configuredParameters;
  }

  public Object[] getRuntimeParameters() {
    return runtimeParameters;
  }

  @Override
  public int compareTo(final OSQLMethodRuntime o) {
    return method.compareTo(o.method);
  }
}