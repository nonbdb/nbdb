/* Generated By:JJTree: Do not edit this line. SQLServerStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.common.listener.ProgressListener;
import com.jetbrains.youtrack.db.internal.core.command.ServerCommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalExecutionPlan;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.query.SQLAsynchQuery;
import java.util.Map;

public class SQLServerStatement extends SimpleNode {

  public SQLServerStatement(int id) {
    super(id);
  }

  public SQLServerStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    throw new UnsupportedOperationException(
        "missing implementation in " + getClass().getSimpleName());
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    throw new UnsupportedOperationException();
  }

  public void validate() throws CommandSQLParsingException {
  }

  @Override
  public String toString(String prefix) {
    StringBuilder builder = new StringBuilder();
    toString(null, builder);
    return builder.toString();
  }

  public Object execute(
      SQLAsynchQuery<EntityImpl> request,
      ServerCommandContext context,
      ProgressListener progressListener) {
    throw new UnsupportedOperationException("Unsupported command: " + getClass().getSimpleName());
  }

  public ResultSet execute(YouTrackDBInternal db, Object[] args) {
    return execute(db, args, true);
  }

  public ResultSet execute(
      YouTrackDBInternal db, Object[] args, ServerCommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public ResultSet execute(YouTrackDBInternal db, Map args) {
    return execute(db, args, true);
  }

  public ResultSet execute(YouTrackDBInternal db, Map args, ServerCommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public ResultSet execute(YouTrackDBInternal db, Object[] args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public ResultSet execute(
      YouTrackDBInternal db,
      Object[] args,
      ServerCommandContext parentContext,
      boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  public ResultSet execute(YouTrackDBInternal db, Map args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public ResultSet execute(
      YouTrackDBInternal db, Map args, ServerCommandContext parentContext, boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  /**
   * creates an execution plan for current statement, with profiling disabled
   *
   * @param ctx the context that will be used to execute the statement
   * @return an execution plan
   */
  public InternalExecutionPlan createExecutionPlan(ServerCommandContext ctx) {
    return createExecutionPlan(ctx, false);
  }

  /**
   * creates an execution plan for current statement
   *
   * @param ctx     the context that will be used to execute the statement
   * @param profile true to enable profiling, false to disable it
   * @return an execution plan
   */
  public InternalExecutionPlan createExecutionPlan(ServerCommandContext ctx, boolean profile) {
    throw new UnsupportedOperationException();
  }

  public InternalExecutionPlan createExecutionPlanNoCache(
      ServerCommandContext ctx, boolean profile) {
    return createExecutionPlan(ctx, profile);
  }

  public SQLServerStatement copy() {
    throw new UnsupportedOperationException("IMPLEMENT copy() ON " + getClass().getSimpleName());
  }

  public boolean refersToParent() {
    throw new UnsupportedOperationException(
        "Implement " + getClass().getSimpleName() + ".refersToParent()");
  }

  public boolean isIdempotent() {
    return false;
  }

  public static SQLStatement deserializeFromOResult(Result res) {
    try {
      SQLStatement result =
          (SQLStatement)
              Class.forName(res.getProperty("__class"))
                  .getConstructor(Integer.class)
                  .newInstance(-1);
      result.deserialize(res);
    } catch (Exception e) {
      throw BaseException.wrapException(new CommandExecutionException(""), e);
    }
    return null;
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    result.setProperty("__class", getClass().getName());
    return result;
  }

  public void deserialize(Result fromResult) {
    throw new UnsupportedOperationException();
  }

  public boolean executinPlanCanBeCached() {
    return false;
  }
}
/* JavaCC - OriginalChecksum=86cab5eeff02ee2a2f8c5e0c0a017e6b (do not edit this line) */
