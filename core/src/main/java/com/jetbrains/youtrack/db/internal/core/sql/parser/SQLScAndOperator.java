/* Generated By:JJTree: Do not edit this line. SQLScAndOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexFinder.Operation;
import com.jetbrains.youtrack.db.internal.core.sql.operator.QueryOperator;
import java.util.Map;

public class SQLScAndOperator extends SimpleNode implements SQLBinaryCompareOperator {

  protected QueryOperator lowLevelOperator = null;

  public SQLScAndOperator(int id) {
    super(id);
  }

  public SQLScAndOperator(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean execute(Object iLeft, Object iRight) {
    if (lowLevelOperator == null) {
      throw new UnsupportedOperationException();
    }
    return false;
  }

  @Override
  public String toString() {
    return "&&";
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("&&");
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("&&");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  public SQLScAndOperator copy() {
    return this;
  }

  @Override
  public Operation getOperation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(this.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
/* JavaCC - OriginalChecksum=12592a24f576571470ce760aff503b30 (do not edit this line) */