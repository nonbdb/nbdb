/* Generated By:JJTree: Do not edit this line. SQLDeleteVertexStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.DeleteExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.DeleteVertexExecutionPlanner;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SQLDeleteVertexStatement extends SQLStatement {

  protected boolean from = false;
  protected SQLFromClause fromClause;
  protected SQLWhereClause whereClause;
  protected boolean returnBefore = false;
  protected SQLLimit limit = null;
  protected SQLBatch batch = null;

  public SQLDeleteVertexStatement(int id) {
    super(id);
  }

  public SQLDeleteVertexStatement(YouTrackDBSql p, int id) {
    super(p, id);
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
    DeleteVertexExecutionPlanner planner = new DeleteVertexExecutionPlanner(this);
    DeleteExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DELETE VERTEX ");
    if (from) {
      builder.append("FROM ");
    }
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
    if (batch != null) {
      batch.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append("DELETE VERTEX ");
    if (from) {
      builder.append("FROM ");
    }
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
    if (batch != null) {
      batch.toGenericStatement(builder);
    }
  }

  @Override
  public SQLDeleteVertexStatement copy() {
    SQLDeleteVertexStatement result = new SQLDeleteVertexStatement(-1);
    result.from = from;
    result.fromClause = fromClause == null ? null : fromClause.copy();
    result.whereClause = whereClause == null ? null : whereClause.copy();
    result.returnBefore = returnBefore;
    result.limit = limit == null ? null : limit.copy();
    result.batch = batch == null ? null : batch.copy();
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

    SQLDeleteVertexStatement that = (SQLDeleteVertexStatement) o;

    if (from != that.from) {
      return false;
    }
    if (returnBefore != that.returnBefore) {
      return false;
    }
    if (!Objects.equals(fromClause, that.fromClause)) {
      return false;
    }
    if (!Objects.equals(whereClause, that.whereClause)) {
      return false;
    }
    if (!Objects.equals(limit, that.limit)) {
      return false;
    }
    return Objects.equals(batch, that.batch);
  }

  @Override
  public int hashCode() {
    int result = (from ? 1 : 0);
    result = 31 * result + (fromClause != null ? fromClause.hashCode() : 0);
    result = 31 * result + (whereClause != null ? whereClause.hashCode() : 0);
    result = 31 * result + (returnBefore ? 1 : 0);
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    result = 31 * result + (batch != null ? batch.hashCode() : 0);
    return result;
  }

  public boolean isFrom() {
    return from;
  }

  public void setFrom(boolean from) {
    this.from = from;
  }

  public SQLFromClause getFromClause() {
    return fromClause;
  }

  public void setFromClause(SQLFromClause fromClause) {
    this.fromClause = fromClause;
  }

  public SQLWhereClause getWhereClause() {
    return whereClause;
  }

  public void setWhereClause(SQLWhereClause whereClause) {
    this.whereClause = whereClause;
  }

  public boolean isReturnBefore() {
    return returnBefore;
  }

  public void setReturnBefore(boolean returnBefore) {
    this.returnBefore = returnBefore;
  }

  public SQLLimit getLimit() {
    return limit;
  }

  public void setLimit(SQLLimit limit) {
    this.limit = limit;
  }

  public SQLBatch getBatch() {
    return batch;
  }

  public void setBatch(SQLBatch batch) {
    this.batch = batch;
  }
}
/* JavaCC - OriginalChecksum=b62d3046f4bd1b9c1f78ed4f125b06d3 (do not edit this line) */
