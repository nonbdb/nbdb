/* Generated By:JJTree: Do not edit this line. OIsNullCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OIsNullCondition extends OBooleanExpression {

  protected OExpression expression;

  public OIsNullCondition(int id) {
    super(id);
  }

  public OIsNullCondition(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(YTIdentifiable currentRecord, OCommandContext ctx) {
    return expression.execute(currentRecord, ctx) == null;
  }

  @Override
  public boolean evaluate(YTResult currentRecord, OCommandContext ctx) {
    if (expression.isFunctionAny()) {
      return evaluateAny(currentRecord, ctx);
    }

    if (expression.isFunctionAll()) {
      return evaluateAllFunction(currentRecord, ctx);
    }

    return expression.execute(currentRecord, ctx) == null;
  }

  private boolean evaluateAny(YTResult currentRecord, OCommandContext ctx) {
    for (String s : currentRecord.getPropertyNames()) {
      Object leftVal = currentRecord.getProperty(s);
      if (leftVal == null) {
        return true;
      }
    }
    return false;
  }

  private boolean evaluateAllFunction(YTResult currentRecord, OCommandContext ctx) {
    for (String s : currentRecord.getPropertyNames()) {
      Object leftVal = currentRecord.getProperty(s);
      if (!(leftVal == null)) {
        return false;
      }
    }
    return true;
  }

  public OExpression getExpression() {
    return expression;
  }

  public void setExpression(OExpression expression) {
    this.expression = expression;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    expression.toString(params, builder);
    builder.append(" is null");
  }

  public void toGenericStatement(StringBuilder builder) {
    expression.toGenericStatement(builder);
    builder.append(" is null");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return expression.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    if (expression.supportsBasicCalculation()) {
      return 0;
    }
    return 1;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    if (expression.supportsBasicCalculation()) {
      return Collections.EMPTY_LIST;
    }
    return Collections.singletonList(expression);
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    return expression.needsAliases(aliases);
  }

  @Override
  public OIsNullCondition copy() {
    OIsNullCondition result = new OIsNullCondition(-1);
    result.expression = expression.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    this.expression.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return expression != null && expression.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OIsNullCondition that = (OIsNullCondition) o;

    return Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    return expression != null ? expression.hashCode() : 0;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    return expression.getMatchPatternInvolvedAliases();
  }

  @Override
  public boolean isCacheable(YTDatabaseSessionInternal session) {
    return expression.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=29ebbc506a98f90953af91a66a03aa1e (do not edit this line) */
