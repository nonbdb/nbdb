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
package com.jetbrains.youtrack.db.internal.core.sql.functions;

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
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Wraps function managing the binding of parameters.
 */
public class OSQLFunctionRuntime extends OSQLFilterItemAbstract {

  public OSQLFunction function;
  public Object[] configuredParameters;
  public Object[] runtimeParameters;

  public OSQLFunctionRuntime(YTDatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iText) {
    super(session, iQueryToParse, iText);
  }

  public OSQLFunctionRuntime(final OSQLFunction iFunction) {
    function = iFunction;
  }

  public boolean aggregateResults() {
    return function.aggregateResults();
  }

  public boolean filterResult() {
    return function.filterResult();
  }

  /**
   * Execute a function.
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
      @Nonnull final CommandContext iContext) {
    // RESOLVE VALUES USING THE CURRENT RECORD
    for (int i = 0; i < configuredParameters.length; ++i) {
      runtimeParameters[i] = configuredParameters[i];

      if (configuredParameters[i] instanceof OSQLFilterItemField) {
        runtimeParameters[i] =
            ((OSQLFilterItemField) configuredParameters[i])
                .getValue(iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof OSQLFunctionRuntime) {
        runtimeParameters[i] =
            ((OSQLFunctionRuntime) configuredParameters[i])
                .execute(iThis, iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof OSQLFilterItemVariable) {
        runtimeParameters[i] =
            ((OSQLFilterItemVariable) configuredParameters[i])
                .getValue(iCurrentRecord, iCurrentResult, iContext);
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
                    (iCurrentRecord instanceof EntityImpl ? (EntityImpl) iCurrentResult : null),
                    iContext);
      } else if (configuredParameters[i] instanceof String) {
        if (configuredParameters[i].toString().startsWith("\"")
            || configuredParameters[i].toString().startsWith("'")) {
          runtimeParameters[i] = OIOUtils.getStringContent(configuredParameters[i]);
        }
      }
    }

    var db = iContext.getDatabase();
    if (function.getMaxParams(db) == -1 || function.getMaxParams(db) > 0) {
      if (runtimeParameters.length < function.getMinParams()
          || (function.getMaxParams(db) > -1 && runtimeParameters.length > function.getMaxParams(
          db))) {
        String params;
        if (function.getMinParams() == function.getMaxParams(db)) {
          params = "" + function.getMinParams();
        } else {
          params = function.getMinParams() + "-" + function.getMaxParams(db);
        }
        throw new YTCommandExecutionException(
            "Syntax error: function '"
                + function.getName(db)
                + "' needs "
                + params
                + " argument(s) while has been received "
                + runtimeParameters.length);
      }
    }

    final Object functionResult =
        function.execute(iThis, iCurrentRecord, iCurrentResult, runtimeParameters, iContext);

    return transformValue(iCurrentRecord, iContext, functionResult);
  }

  public Object getResult(YTDatabaseSessionInternal session) {
    var context = new BasicCommandContext();
    context.setDatabase(session);

    return transformValue(null, context, function.getResult());
  }

  public void setResult(final Object iValue) {
    function.setResult(iValue);
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
    return function.getName(session);
  }

  public OSQLFunctionRuntime setParameters(@Nonnull CommandContext context,
      final Object[] iParameters, final boolean iEvaluate) {
    this.configuredParameters = new Object[iParameters.length];

    for (int i = 0; i < iParameters.length; ++i) {
      this.configuredParameters[i] = iParameters[i];

      if (iEvaluate) {
        if (iParameters[i] != null) {
          if (iParameters[i] instanceof String) {
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
    }

    function.config(configuredParameters);

    // COPY STATIC VALUES
    this.runtimeParameters = new Object[configuredParameters.length];
    for (int i = 0; i < configuredParameters.length; ++i) {
      if (!(configuredParameters[i] instanceof OSQLFilterItemField)
          && !(configuredParameters[i] instanceof OSQLFunctionRuntime)) {
        runtimeParameters[i] = configuredParameters[i];
      }
    }

    return this;
  }

  public OSQLFunction getFunction() {
    return function;
  }

  public Object[] getConfiguredParameters() {
    return configuredParameters;
  }

  public Object[] getRuntimeParameters() {
    return runtimeParameters;
  }

  @Override
  protected void setRoot(YTDatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iText) {
    final int beginParenthesis = iText.indexOf('(');

    // SEARCH FOR THE FUNCTION
    final String funcName = iText.substring(0, beginParenthesis);

    final List<String> funcParamsText = OStringSerializerHelper.getParameters(iText);

    function = OSQLEngine.getInstance().getFunction(session, funcName);
    if (function == null) {
      throw new YTCommandSQLParsingException("Unknown function " + funcName + "()");
    }

    // PARSE PARAMETERS
    this.configuredParameters = new Object[funcParamsText.size()];
    for (int i = 0; i < funcParamsText.size(); ++i) {
      this.configuredParameters[i] = funcParamsText.get(i);
    }

    var context = new BasicCommandContext();
    context.setDatabase(session);

    setParameters(context, configuredParameters, true);
  }
}