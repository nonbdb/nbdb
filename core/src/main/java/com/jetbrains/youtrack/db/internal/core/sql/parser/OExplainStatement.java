/* Generated By:JJTree: Do not edit this line. OExplainStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseStats;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.OExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.OInternalExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OExplainStatement extends OStatement {

  protected OStatement statement;

  public OExplainStatement(int id) {
    super(id);
  }

  public OExplainStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("EXPLAIN ");
    statement.toString(params, builder);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("EXPLAIN ");
    statement.toGenericStatement(builder);
  }

  @Override
  public YTResultSet execute(
      YTDatabaseSessionInternal db, Object[] args, CommandContext parentCtx,
      boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    ctx.setInputParameters(params);

    OExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = statement.createExecutionPlan(ctx, false);
    } else {
      executionPlan = statement.createExecutionPlanNoCache(ctx, false);
    }

    YTExplainResultSet result = new YTExplainResultSet(db, executionPlan, new ODatabaseStats());
    return result;
  }

  @Override
  public YTResultSet execute(
      YTDatabaseSessionInternal db, Map args, CommandContext parentCtx, boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(args);

    OExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = statement.createExecutionPlan(ctx, false);
    } else {
      executionPlan = statement.createExecutionPlanNoCache(ctx, false);
    }

    YTExplainResultSet result = new YTExplainResultSet(db, executionPlan, new ODatabaseStats());
    return result;
  }

  @Override
  public OInternalExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    return statement.createExecutionPlan(ctx, enableProfiling);
  }

  @Override
  public OExplainStatement copy() {
    OExplainStatement result = new OExplainStatement(-1);
    result.statement = statement == null ? null : statement.copy();
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

    OExplainStatement that = (OExplainStatement) o;

    return Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return statement != null ? statement.hashCode() : 0;
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }
}
/* JavaCC - OriginalChecksum=9fdd24510993cbee32e38a51c838bdb4 (do not edit this line) */