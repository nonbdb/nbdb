/* Generated By:JJTree: Do not edit this line. SQLSelectStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
/*


 */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.CommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.SelectExecutionPlanner;
import com.jetbrains.youtrack.db.internal.core.sql.executor.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SQLSelectStatement extends SQLStatement {

  protected SQLFromClause target;

  protected SQLProjection projection;

  protected SQLWhereClause whereClause;

  protected SQLGroupBy groupBy;

  protected SQLOrderBy orderBy;

  protected SQLUnwind unwind;

  protected SQLSkip skip;

  protected SQLLimit limit;

  protected SQLFetchPlan fetchPlan;

  protected SQLLetClause letClause;

  protected SQLTimeout timeout;

  protected Boolean parallel;

  protected Boolean noCache;

  public SQLSelectStatement(int id) {
    super(id);
  }

  public SQLSelectStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public SQLProjection getProjection() {
    return projection;
  }

  public void setProjection(SQLProjection projection) {
    this.projection = projection;
  }

  public SQLFromClause getTarget() {
    return target;
  }

  public void setTarget(SQLFromClause target) {
    this.target = target;
  }

  public SQLWhereClause getWhereClause() {
    return whereClause;
  }

  public void setWhereClause(SQLWhereClause whereClause) {
    this.whereClause = whereClause;
  }

  public SQLGroupBy getGroupBy() {
    return groupBy;
  }

  public void setGroupBy(SQLGroupBy groupBy) {
    this.groupBy = groupBy;
  }

  public SQLOrderBy getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(SQLOrderBy orderBy) {
    this.orderBy = orderBy;
  }

  public SQLSkip getSkip() {
    return skip;
  }

  public void setSkip(SQLSkip skip) {
    this.skip = skip;
  }

  public SQLLimit getLimit() {
    return limit;
  }

  public void setLimit(SQLLimit limit) {
    this.limit = limit;
  }

  public SQLFetchPlan getFetchPlan() {
    return fetchPlan;
  }

  public void setFetchPlan(SQLFetchPlan fetchPlan) {
    this.fetchPlan = fetchPlan;
  }

  public SQLLetClause getLetClause() {
    return letClause;
  }

  public void setLetClause(SQLLetClause letClause) {
    this.letClause = letClause;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {

    builder.append("SELECT");
    if (projection != null) {
      builder.append(" ");
      projection.toString(params, builder);
    }
    if (target != null) {
      builder.append(" FROM ");
      target.toString(params, builder);
    }

    if (letClause != null) {
      builder.append(" ");
      letClause.toString(params, builder);
    }

    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toString(params, builder);
    }

    if (groupBy != null) {
      builder.append(" ");
      groupBy.toString(params, builder);
    }

    if (orderBy != null) {
      builder.append(" ");
      orderBy.toString(params, builder);
    }

    if (unwind != null) {
      builder.append(" ");
      unwind.toString(params, builder);
    }

    if (skip != null) {
      skip.toString(params, builder);
    }

    if (limit != null) {
      limit.toString(params, builder);
    }

    if (fetchPlan != null) {
      builder.append(" ");
      fetchPlan.toString(params, builder);
    }

    if (timeout != null) {
      timeout.toString(params, builder);
    }

    if (Boolean.TRUE.equals(parallel)) {
      builder.append(" PARALLEL");
    }

    if (Boolean.TRUE.equals(noCache)) {
      builder.append(" NOCACHE");
    }
  }

  public void toGenericStatement(StringBuilder builder) {

    builder.append("SELECT");
    if (projection != null) {
      builder.append(" ");
      projection.toGenericStatement(builder);
    }
    if (target != null) {
      builder.append(" FROM ");
      target.toGenericStatement(builder);
    }

    if (letClause != null) {
      builder.append(" ");
      letClause.toGenericStatement(builder);
    }

    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toGenericStatement(builder);
    }

    if (groupBy != null) {
      builder.append(" ");
      groupBy.toGenericStatement(builder);
    }

    if (orderBy != null) {
      builder.append(" ");
      orderBy.toGenericStatement(builder);
    }

    if (unwind != null) {
      builder.append(" ");
      unwind.toGenericStatement(builder);
    }

    if (skip != null) {
      skip.toGenericStatement(builder);
    }

    if (limit != null) {
      limit.toGenericStatement(builder);
    }

    if (fetchPlan != null) {
      builder.append(" ");
      fetchPlan.toGenericStatement(builder);
    }

    if (timeout != null) {
      timeout.toGenericStatement(builder);
    }

    if (Boolean.TRUE.equals(parallel)) {
      builder.append(" PARALLEL");
    }

    if (Boolean.TRUE.equals(noCache)) {
      builder.append(" NOCACHE");
    }
  }

  public void validate() throws CommandSQLParsingException {
    if (projection != null) {
      projection.validate();
      if (projection.isExpand() && groupBy != null) {
        throw new CommandSQLParsingException("expand() cannot be used together with GROUP BY");
      }
    }
  }

  @Override
  public boolean executinPlanCanBeCached(DatabaseSessionInternal session) {
    if (originalStatement == null) {
      setOriginalStatement(this.toString());
    }
    if (this.target != null && !this.target.isCacheable(session)) {
      return false;
    }

    if (this.letClause != null && !this.letClause.isCacheable(session)) {
      return false;
    }

    if (projection != null && !this.projection.isCacheable(session)) {
      return false;
    }

    return whereClause == null || whereClause.isCacheable(session);
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
    InternalExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = createExecutionPlanNoCache(ctx, false);
    }

    LocalResultSet result = new LocalResultSet(executionPlan);
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
    InternalExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = createExecutionPlanNoCache(ctx, false);
    }

    LocalResultSet result = new LocalResultSet(executionPlan);
    return result;
  }

  public InternalExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    SelectExecutionPlanner planner = new SelectExecutionPlanner(this);
    InternalExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling, true);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  public InternalExecutionPlan createExecutionPlanNoCache(
      CommandContext ctx, boolean enableProfiling) {
    SelectExecutionPlanner planner = new SelectExecutionPlanner(this);
    InternalExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling, false);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  @Override
  public SQLSelectStatement copy() {
    SQLSelectStatement result = null;
    try {
      result = getClass().getConstructor(Integer.TYPE).newInstance(-1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    result.originalStatement = originalStatement;
    result.target = target == null ? null : target.copy();
    result.projection = projection == null ? null : projection.copy();
    result.whereClause = whereClause == null ? null : whereClause.copy();
    result.groupBy = groupBy == null ? null : groupBy.copy();
    result.orderBy = orderBy == null ? null : orderBy.copy();
    result.unwind = unwind == null ? null : unwind.copy();
    result.skip = skip == null ? null : skip.copy();
    result.limit = limit == null ? null : limit.copy();
    result.fetchPlan = fetchPlan == null ? null : fetchPlan.copy();
    result.letClause = letClause == null ? null : letClause.copy();
    result.timeout = timeout == null ? null : timeout.copy();
    result.parallel = parallel;
    result.noCache = noCache;

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

    SQLSelectStatement that = (SQLSelectStatement) o;

    if (!Objects.equals(target, that.target)) {
      return false;
    }
    if (!Objects.equals(projection, that.projection)) {
      return false;
    }
    if (!Objects.equals(whereClause, that.whereClause)) {
      return false;
    }
    if (!Objects.equals(groupBy, that.groupBy)) {
      return false;
    }
    if (!Objects.equals(orderBy, that.orderBy)) {
      return false;
    }
    if (!Objects.equals(unwind, that.unwind)) {
      return false;
    }
    if (!Objects.equals(skip, that.skip)) {
      return false;
    }
    if (!Objects.equals(limit, that.limit)) {
      return false;
    }
    if (!Objects.equals(fetchPlan, that.fetchPlan)) {
      return false;
    }
    if (!Objects.equals(letClause, that.letClause)) {
      return false;
    }
    if (!Objects.equals(timeout, that.timeout)) {
      return false;
    }
    if (!Objects.equals(parallel, that.parallel)) {
      return false;
    }
    return Objects.equals(noCache, that.noCache);
  }

  @Override
  public int hashCode() {
    int result = target != null ? target.hashCode() : 0;
    result = 31 * result + (projection != null ? projection.hashCode() : 0);
    result = 31 * result + (whereClause != null ? whereClause.hashCode() : 0);
    result = 31 * result + (groupBy != null ? groupBy.hashCode() : 0);
    result = 31 * result + (orderBy != null ? orderBy.hashCode() : 0);
    result = 31 * result + (unwind != null ? unwind.hashCode() : 0);
    result = 31 * result + (skip != null ? skip.hashCode() : 0);
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    result = 31 * result + (fetchPlan != null ? fetchPlan.hashCode() : 0);
    result = 31 * result + (letClause != null ? letClause.hashCode() : 0);
    result = 31 * result + (timeout != null ? timeout.hashCode() : 0);
    result = 31 * result + (parallel != null ? parallel.hashCode() : 0);
    result = 31 * result + (noCache != null ? noCache.hashCode() : 0);
    return result;
  }

  @Override
  public boolean refersToParent() {
    // no FROM, if a subquery refers to parent it does not make sense, so that reference will be
    // just ignored

    if (projection != null && projection.refersToParent()) {
      return true;
    }
    if (target != null && target.refersToParent()) {
      return true;
    }
    if (whereClause != null && whereClause.refersToParent()) {
      return true;
    }
    if (groupBy != null && groupBy.refersToParent()) {
      return true;
    }
    if (orderBy != null && orderBy.refersToParent()) {
      return true;
    }
    return letClause != null && letClause.refersToParent();
  }

  public SQLUnwind getUnwind() {
    return unwind;
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  public void setUnwind(SQLUnwind unwind) {
    this.unwind = unwind;
  }

  public SQLTimeout getTimeout() {
    return timeout;
  }

  public void setTimeout(SQLTimeout timeout) {
    this.timeout = timeout;
  }

  public void setParallel(Boolean parallel) {
    this.parallel = parallel;
  }

  public void setNoCache(Boolean noCache) {
    this.noCache = noCache;
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = (ResultInternal) super.serialize(db);
    if (target != null) {
      result.setProperty("target", target.serialize(db));
    }
    if (projection != null) {
      result.setProperty("projection", projection.serialize(db));
    }
    if (whereClause != null) {
      result.setProperty("whereClause", whereClause.serialize(db));
    }
    if (groupBy != null) {
      result.setProperty("groupBy", groupBy.serialize(db));
    }
    if (orderBy != null) {
      result.setProperty("orderBy", orderBy.serialize(db));
    }
    if (unwind != null) {
      result.setProperty("unwind", unwind.serialize(db));
    }
    if (skip != null) {
      result.setProperty("skip", skip.serialize(db));
    }
    if (limit != null) {
      result.setProperty("limit", limit.serialize(db));
    }
    if (fetchPlan != null) {
      result.setProperty("fetchPlan", fetchPlan.serialize(db));
    }
    if (letClause != null) {
      result.setProperty("letClause", letClause.serialize(db));
    }
    if (timeout != null) {
      result.setProperty("timeout", timeout.serialize(db));
    }
    result.setProperty("parallel", parallel);
    result.setProperty("noCache", noCache);
    return result;
  }

  public void deserialize(Result fromResult) {
    if (fromResult.getProperty("target") != null) {
      target = new SQLFromClause(-1);
      target.deserialize(fromResult.getProperty("target"));
    }
    if (fromResult.getProperty("projection") != null) {
      projection = new SQLProjection(-1);
      projection.deserialize(fromResult.getProperty("projection"));
    }
    if (fromResult.getProperty("whereClause") != null) {
      whereClause = new SQLWhereClause(-1);
      whereClause.deserialize(fromResult.getProperty("whereClause"));
    }
    if (fromResult.getProperty("groupBy") != null) {
      groupBy = new SQLGroupBy(-1);
      groupBy.deserialize(fromResult.getProperty("groupBy"));
    }
    if (fromResult.getProperty("orderBy") != null) {
      orderBy = new SQLOrderBy(-1);
      orderBy.deserialize(fromResult.getProperty("orderBy"));
    }
    if (fromResult.getProperty("unwind") != null) {
      unwind = new SQLUnwind(-1);
      unwind.deserialize(fromResult.getProperty("unwind"));
    }
    if (fromResult.getProperty("skip") != null) {
      skip = new SQLSkip(-1);
      skip.deserialize(fromResult.getProperty("skip"));
    }
    if (fromResult.getProperty("limit") != null) {
      limit = new SQLLimit(-1);
      limit.deserialize(fromResult.getProperty("limit"));
    }
    if (fromResult.getProperty("fetchPlan") != null) {
      fetchPlan = new SQLFetchPlan(-1);
      fetchPlan.deserialize(fromResult.getProperty("fetchPlan"));
    }
    if (fromResult.getProperty("letClause") != null) {
      letClause = new SQLLetClause(-1);
      letClause.deserialize(fromResult.getProperty("letClause"));
    }
    if (fromResult.getProperty("timeout") != null) {
      timeout = new SQLTimeout(-1);
      timeout.deserialize(fromResult.getProperty("timeout"));
    }

    parallel = fromResult.getProperty("parallel");
    noCache = fromResult.getProperty("noCache");
  }
}
/* JavaCC - OriginalChecksum=b26959b9726a8cf35d6283eca931da6b (do not edit this line) */
