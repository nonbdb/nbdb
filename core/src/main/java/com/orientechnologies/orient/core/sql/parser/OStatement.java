/* Generated By:JJTree: Do not edit this line. OStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.executor.OInternalExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
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

  public void validate() throws OCommandSQLParsingException {}

  @Override
  public String toString(String prefix) {
    StringBuilder builder = new StringBuilder();
    toString(null, builder);
    return builder.toString();
  }

  public Object execute(
      OSQLAsynchQuery<ODocument> request,
      OCommandContext context,
      OProgressListener progressListener) {
    throw new UnsupportedOperationException("Unsupported command: " + getClass().getSimpleName());
  }

  public OResultSet execute(ODatabaseSessionInternal db, Object[] args) {
    return execute(db, args, true);
  }

  public OResultSet execute(
      ODatabaseSessionInternal db, Object[] args, OCommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public OResultSet execute(ODatabaseSessionInternal db, Map args) {
    return execute(db, args, true);
  }

  public OResultSet execute(ODatabaseSessionInternal db, Map args, OCommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public OResultSet execute(ODatabaseSessionInternal db, Object[] args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public OResultSet execute(
      ODatabaseSessionInternal db,
      Object[] args,
      OCommandContext parentContext,
      boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  public OResultSet execute(ODatabaseSessionInternal db, Map args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public OResultSet execute(
      ODatabaseSessionInternal db, Map args, OCommandContext parentContext, boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  /**
   * creates an execution plan for current statement, with profiling disabled
   *
   * @param ctx the context that will be used to execute the statement
   * @return an execution plan
   */
  public OInternalExecutionPlan createExecutionPlan(OCommandContext ctx) {
    return createExecutionPlan(ctx, false);
  }

  /**
   * creates an execution plan for current statement
   *
   * @param ctx the context that will be used to execute the statement
   * @param profile true to enable profiling, false to disable it
   * @return an execution plan
   */
  public OInternalExecutionPlan createExecutionPlan(OCommandContext ctx, boolean profile) {
    throw new UnsupportedOperationException();
  }

  public OInternalExecutionPlan createExecutionPlanNoCache(OCommandContext ctx, boolean profile) {
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

  public static OStatement deserializeFromOResult(OResult doc) {
    try {
      OStatement result =
          (OStatement)
              Class.forName(doc.getProperty("__class"))
                  .getConstructor(Integer.class)
                  .newInstance(-1);
      result.deserialize(doc);
    } catch (Exception e) {
      throw OException.wrapException(new OCommandExecutionException(""), e);
    }
    return null;
  }

  public OResult serialize() {
    OResultInternal result = new OResultInternal();
    result.setProperty("__class", getClass().getName());
    return result;
  }

  public void deserialize(OResult fromResult) {
    throw new UnsupportedOperationException();
  }

  public boolean executinPlanCanBeCached() {
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
