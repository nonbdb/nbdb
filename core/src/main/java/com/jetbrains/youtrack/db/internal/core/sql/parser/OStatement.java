/* Generated By:JJTree: Do not edit this line. OStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.exception.YTException;
import com.jetbrains.youtrack.db.internal.common.listener.OProgressListener;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.YTCommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.OInternalExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.query.OSQLAsynchQuery;
import java.util.Map;

public class OStatement extends SimpleNode {

  // only for internal use!!! (caching and profiling)
  protected String originalStatement;

  public static final String CUSTOM_STRICT_SQL = "strictSql";

  public OStatement(int id) {
    super(id);
  }

  public OStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(originalStatement);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append(originalStatement);
  }

  public void validate() throws YTCommandSQLParsingException {
  }

  @Override
  public String toString(String prefix) {
    StringBuilder builder = new StringBuilder();
    toString(null, builder);
    return builder.toString();
  }

  public Object execute(
      OSQLAsynchQuery<EntityImpl> request,
      CommandContext context,
      OProgressListener progressListener) {
    throw new UnsupportedOperationException("Unsupported command: " + getClass().getSimpleName());
  }

  public YTResultSet execute(YTDatabaseSessionInternal db, Object[] args) {
    return execute(db, args, true);
  }

  public YTResultSet execute(
      YTDatabaseSessionInternal db, Object[] args, CommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public YTResultSet execute(YTDatabaseSessionInternal db, Map args) {
    return execute(db, args, true);
  }

  public YTResultSet execute(YTDatabaseSessionInternal db, Map args,
      CommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public YTResultSet execute(YTDatabaseSessionInternal db, Object[] args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public YTResultSet execute(
      YTDatabaseSessionInternal db,
      Object[] args,
      CommandContext parentContext,
      boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  public YTResultSet execute(YTDatabaseSessionInternal db, Map args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public YTResultSet execute(
      YTDatabaseSessionInternal db, Map args, CommandContext parentContext, boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  /**
   * creates an execution plan for current statement, with profiling disabled
   *
   * @param ctx the context that will be used to execute the statement
   * @return an execution plan
   */
  public OInternalExecutionPlan createExecutionPlan(CommandContext ctx) {
    return createExecutionPlan(ctx, false);
  }

  /**
   * creates an execution plan for current statement
   *
   * @param ctx     the context that will be used to execute the statement
   * @param profile true to enable profiling, false to disable it
   * @return an execution plan
   */
  public OInternalExecutionPlan createExecutionPlan(CommandContext ctx, boolean profile) {
    throw new UnsupportedOperationException();
  }

  public OInternalExecutionPlan createExecutionPlanNoCache(CommandContext ctx, boolean profile) {
    return createExecutionPlan(ctx, profile);
  }

  public OStatement copy() {
    throw new UnsupportedOperationException("IMPLEMENT copy() ON " + getClass().getSimpleName());
  }

  public boolean refersToParent() {
    throw new UnsupportedOperationException(
        "Implement " + getClass().getSimpleName() + ".refersToParent()");
  }

  public boolean isIdempotent() {
    return false;
  }

  public static OStatement deserializeFromOResult(YTResult doc) {
    try {
      OStatement result =
          (OStatement)
              Class.forName(doc.getProperty("__class"))
                  .getConstructor(Integer.class)
                  .newInstance(-1);
      result.deserialize(doc);
    } catch (Exception e) {
      throw YTException.wrapException(new YTCommandExecutionException(""), e);
    }
    return null;
  }

  public YTResult serialize(YTDatabaseSessionInternal db) {
    YTResultInternal result = new YTResultInternal(db);
    result.setProperty("__class", getClass().getName());
    return result;
  }

  public void deserialize(YTResult fromResult) {
    throw new UnsupportedOperationException();
  }

  public boolean executinPlanCanBeCached(YTDatabaseSessionInternal session) {
    return false;
  }

  public String getOriginalStatement() {
    return originalStatement;
  }

  public void setOriginalStatement(String originalStatement) {
    this.originalStatement = originalStatement;
  }
}
/* JavaCC - OriginalChecksum=589c4dcc8287f430e46d8eb12b0412c5 (do not edit this line) */