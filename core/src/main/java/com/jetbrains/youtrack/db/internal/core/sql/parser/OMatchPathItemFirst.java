package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class OMatchPathItemFirst extends OMatchPathItem {

  protected OFunctionCall function;

  protected OMethodCall methodWrapper;

  public OMatchPathItemFirst(int id) {
    super(id);
  }

  public OMatchPathItemFirst(OrientSql p, int id) {
    super(p, id);
  }

  public boolean isBidirectional() {
    return false;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    function.toString(params, builder);
    if (filter != null) {
      filter.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    function.toGenericStatement(builder);
    if (filter != null) {
      filter.toGenericStatement(builder);
    }
  }

  protected Iterable<YTIdentifiable> traversePatternEdge(
      OMatchStatement.MatchContext matchContext,
      YTIdentifiable startingPoint,
      CommandContext iCommandContext) {
    Object qR = this.function.execute(startingPoint, iCommandContext);
    return (qR instanceof Iterable) ? (Iterable) qR : Collections.singleton((YTIdentifiable) qR);
  }

  @Override
  public OMatchPathItem copy() {
    OMatchPathItemFirst result = (OMatchPathItemFirst) super.copy();
    result.function = function == null ? null : function.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    OMatchPathItemFirst that = (OMatchPathItemFirst) o;

    return Objects.equals(function, that.function);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (function != null ? function.hashCode() : 0);
    return result;
  }

  public OFunctionCall getFunction() {
    return function;
  }

  public void setFunction(OFunctionCall function) {
    this.function = function;
  }

  @Override
  public OMethodCall getMethod() {
    if (methodWrapper == null) {
      synchronized (this) {
        if (methodWrapper == null) {
          methodWrapper = new OMethodCall(-1);
          methodWrapper.params = function.params;
          methodWrapper.methodName = function.name;
        }
      }
    }
    return methodWrapper;
  }
}
