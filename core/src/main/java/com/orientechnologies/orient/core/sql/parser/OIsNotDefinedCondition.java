/* Generated By:JJTree: Do not edit this line. OIsNotDefinedCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.OResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OIsNotDefinedCondition extends OBooleanExpression {

  protected OExpression expression;

  public OIsNotDefinedCondition(int id) {
    super(id);
  }

  public OIsNotDefinedCondition(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(OIdentifiable currentRecord, OCommandContext ctx) {
    try {
      Object elem = currentRecord.getRecord();
      if (elem instanceof OElement) {
        return !expression.isDefinedFor((OElement) elem);
      }
    } catch (ORecordNotFoundException rnf) {
      return true;
    }
    return true;
  }

  @Override
  public boolean evaluate(OResult currentRecord, OCommandContext ctx) {
    if (expression.isFunctionAny() || expression.isFunctionAll()) {
      return false;
    }
    return !expression.isDefinedFor(currentRecord);
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    return 0;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    return expression.needsAliases(aliases);
  }

  @Override
  public OIsNotDefinedCondition copy() {
    OIsNotDefinedCondition result = new OIsNotDefinedCondition(-1);
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

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    expression.toString(params, builder);
    builder.append(" is not defined");
  }

  public void toGenericStatement(StringBuilder builder) {
    expression.toGenericStatement(builder);
    builder.append(" is not defined");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OIsNotDefinedCondition that = (OIsNotDefinedCondition) o;

    return Objects.equals(expression, that.expression);
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    return expression.getMatchPatternInvolvedAliases();
  }

  @Override
  public boolean isCacheable(ODatabaseSessionInternal session) {
    return expression.isCacheable(session);
  }

  @Override
  public int hashCode() {
    return expression != null ? expression.hashCode() : 0;
  }
}
/* JavaCC - OriginalChecksum=1c766d6caf5ccae19c1c291396bb56f2 (do not edit this line) */
