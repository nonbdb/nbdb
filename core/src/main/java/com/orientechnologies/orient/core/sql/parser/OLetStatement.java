/* Generated By:JJTree: Do not edit this line. OLetStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.YTInternalResultSet;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import com.orientechnologies.orient.core.sql.executor.YTResultSet;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;
import java.util.Objects;

public class OLetStatement extends OSimpleExecStatement {

  protected OIdentifier name;

  protected OStatement statement;
  protected OExpression expression;

  public OLetStatement(int id) {
    super(id);
  }

  public OLetStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    Object result;
    if (expression != null) {
      result = expression.execute((YTResult) null, ctx);
    } else {
      Map<Object, Object> params = ctx.getInputParameters();
      if (statement.originalStatement == null) {
        statement.setOriginalStatement(statement.toString());
      }
      result = statement.execute(ctx.getDatabase(), params, ctx, false);
    }
    if (result instanceof YTResultSet) {
      YTInternalResultSet rs = new YTInternalResultSet();
      ((YTResultSet) result).stream().forEach(x -> rs.add(x));
      rs.setPlan(((YTResultSet) result).getExecutionPlan().orElse(null));
      ((YTResultSet) result).close();
      result = rs;
    }

    if (ctx != null) {
      if (ctx.getParent() != null) {

        ctx.getParent().setVariable(name.getStringValue(), result);
      } else {
        ctx.setVariable(name.getStringValue(), result);
      }
    }
    return OExecutionStream.empty();
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("LET ");
    name.toString(params, builder);
    builder.append(" = ");
    if (statement != null) {
      statement.toString(params, builder);
    } else {
      expression.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("LET ");
    name.toGenericStatement(builder);
    builder.append(" = ");
    if (statement != null) {
      statement.toGenericStatement(builder);
    } else {
      expression.toGenericStatement(builder);
    }
  }

  @Override
  public OLetStatement copy() {
    OLetStatement result = new OLetStatement(-1);
    result.name = name == null ? null : name.copy();
    result.statement = statement == null ? null : statement.copy();
    result.expression = expression == null ? null : expression.copy();
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

    OLetStatement that = (OLetStatement) o;

    if (!Objects.equals(name, that.name)) {
      return false;
    }
    if (!Objects.equals(statement, that.statement)) {
      return false;
    }
    return Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (statement != null ? statement.hashCode() : 0);
    result = 31 * result + (expression != null ? expression.hashCode() : 0);
    return result;
  }

  public OIdentifier getName() {
    return name;
  }
}
/* JavaCC - OriginalChecksum=cc646e5449351ad9ced844f61b687928 (do not edit this line) */
