/* Generated By:JJTree: Do not edit this line. SQLDeleteStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.DeleteExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.DeleteExecutionPlanner;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SQLDeleteStatement extends SQLStatement {

  public SQLFromClause fromClause;
  protected SQLWhereClause whereClause;
  protected boolean returnBefore = false;
  protected SQLLimit limit = null;
  protected boolean unsafe = false;

  public SQLDeleteStatement(int id) {
    super(id);
  }

  public SQLDeleteStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DELETE FROM ");
    fromClause.toString(params, builder);
    if (returnBefore) {
      builder.append(" RETURN BEFORE");
    }
    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toString(params, builder);
    }
    if (limit != null) {
      limit.toString(params, builder);
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append("DELETE FROM ");
    fromClause.toGenericStatement(builder);
    if (returnBefore) {
      builder.append(" RETURN BEFORE");
    }
    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toGenericStatement(builder);
    }
    if (limit != null) {
      limit.toGenericStatement(builder);
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  @Override
  public SQLDeleteStatement copy() {
    SQLDeleteStatement result = new SQLDeleteStatement(-1);
    result.fromClause = fromClause == null ? null : fromClause.copy();
    result.whereClause = whereClause == null ? null : whereClause.copy();
    result.returnBefore = returnBefore;
    result.limit = limit == null ? null : limit.copy();
    result.unsafe = unsafe;
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

    SQLDeleteStatement that = (SQLDeleteStatement) o;

    if (returnBefore != that.returnBefore) {
      return false;
    }
    if (unsafe != that.unsafe) {
      return false;
    }
    if (!Objects.equals(fromClause, that.fromClause)) {
      return false;
    }
    if (!Objects.equals(whereClause, that.whereClause)) {
      return false;
    }
    return Objects.equals(limit, that.limit);
  }

  @Override
  public int hashCode() {
    int result = fromClause != null ? fromClause.hashCode() : 0;
    result = 31 * result + (whereClause != null ? whereClause.hashCode() : 0);
    result = 31 * result + (returnBefore ? 1 : 0);
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    result = 31 * result + (unsafe ? 1 : 0);
    return result;
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Map params, CommandContext parentCtx, boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    DeleteExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (DeleteExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Object[] args, CommandContext parentCtx,
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
    DeleteExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (DeleteExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  public DeleteExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    DeleteExecutionPlanner planner = new DeleteExecutionPlanner(this);
    DeleteExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  public SQLFromClause getFromClause() {
    return fromClause;
  }

  public SQLWhereClause getWhereClause() {
    return whereClause;
  }

  public boolean isReturnBefore() {
    return returnBefore;
  }

  public SQLLimit getLimit() {
    return limit;
  }

  public boolean isUnsafe() {
    return unsafe;
  }
}
/* JavaCC - OriginalChecksum=5fb4ca5ba648e6c9110f41d806206a6f (do not edit this line) */
