/* Generated By:JJTree: Do not edit this line. SQLNumber.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.query.Result;
import java.util.Map;

public class SQLNumber extends SimpleNode {

  public SQLNumber(int id) {
    super(id);
  }

  public SQLNumber(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public Number getValue() {
    return null;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(value);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append(PARAMETER_PLACEHOLDER);
  }

  public SQLNumber copy() {
    SQLNumber result = new SQLNumber(-1);
    result.value = value;
    return result;
  }

  public Result serialize(DatabaseSessionInternal db) {
    throw new UnsupportedOperationException();
  }

  public void deserialize(Result fromResult) {
    throw new UnsupportedOperationException();
  }
}
/* JavaCC - OriginalChecksum=ebedbca280f59eb8ba8f21dc6132ba10 (do not edit this line) */
