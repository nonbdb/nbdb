/* Generated By:JJTree: Do not edit this line. SQLArrayNumberSelector.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SQLArrayNumberSelector extends SimpleNode {

  SQLInputParameter inputValue;
  SQLMathExpression expressionValue;

  Integer integer;

  public SQLArrayNumberSelector(int id) {
    super(id);
  }

  public SQLArrayNumberSelector(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (inputValue != null) {
      inputValue.toString(params, builder);
    } else if (expressionValue != null) {
      expressionValue.toString(params, builder);
    } else if (integer != null) {
      builder.append(integer);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    if (inputValue != null) {
      inputValue.toGenericStatement(builder);
    } else if (expressionValue != null) {
      expressionValue.toGenericStatement(builder);
    } else if (integer != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    }
  }

  public Integer getValue(Identifiable iCurrentRecord, Object iResult, CommandContext ctx) {
    Object result = null;
    if (inputValue != null) {
      result = inputValue.getValue(ctx.getInputParameters());
    } else if (expressionValue != null) {
      result = expressionValue.execute(iCurrentRecord, ctx);
    } else if (integer != null) {
      result = integer;
    }

    if (result == null) {
      return null;
    }
    if (result instanceof Number) {
      return ((Number) result).intValue();
    }
    return null;
  }

  public Integer getValue(Result iCurrentRecord, Object iResult, CommandContext ctx) {
    Object result = null;
    if (inputValue != null) {
      result = inputValue.getValue(ctx.getInputParameters());
    } else if (expressionValue != null) {
      result = expressionValue.execute(iCurrentRecord, ctx);
    } else if (integer != null) {
      result = integer;
    }

    if (result == null) {
      return null;
    }
    if (result instanceof Number) {
      return ((Number) result).intValue();
    }
    return null;
  }

  public boolean needsAliases(Set<String> aliases) {
    if (expressionValue != null) {
      return expressionValue.needsAliases(aliases);
    }
    return false;
  }

  public SQLArrayNumberSelector copy() {
    SQLArrayNumberSelector result = new SQLArrayNumberSelector(-1);
    result.inputValue = inputValue == null ? null : inputValue.copy();
    result.expressionValue = expressionValue == null ? null : expressionValue.copy();
    result.integer = integer;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SQLArrayNumberSelector that = (SQLArrayNumberSelector) o;

    if (!Objects.equals(inputValue, that.inputValue)) {
      return false;
    }
    if (!Objects.equals(expressionValue, that.expressionValue)) {
      return false;
    }
    return Objects.equals(integer, that.integer);
  }

  @Override
  public int hashCode() {
    int result = inputValue != null ? inputValue.hashCode() : 0;
    result = 31 * result + (expressionValue != null ? expressionValue.hashCode() : 0);
    result = 31 * result + (integer != null ? integer.hashCode() : 0);
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (expressionValue != null) {
      expressionValue.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    return expressionValue != null && expressionValue.refersToParent();
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    if (inputValue != null) {
      result.setProperty("inputValue", inputValue.serialize(db));
    }
    if (expressionValue != null) {
      result.setProperty("expressionValue", expressionValue.serialize(db));
    }
    result.setProperty("integer", integer);
    return result;
  }

  public void deserialize(Result fromResult) {
    if (fromResult.getProperty("inputValue") != null) {
      inputValue = SQLInputParameter.deserializeFromOResult(fromResult.getProperty("inputValue"));
    }
    if (fromResult.getProperty("toSelector") != null) {
      expressionValue = new SQLMathExpression(-1);
      expressionValue.deserialize(fromResult.getProperty("expressionValue"));
    }
    integer = fromResult.getProperty("integer");
  }
}
/* JavaCC - OriginalChecksum=5b2e495391ede3ccdc6c25aa63c8e591 (do not edit this line) */
