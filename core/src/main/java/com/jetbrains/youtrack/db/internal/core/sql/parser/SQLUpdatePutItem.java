/* Generated By:JJTree: Do not edit this line. SQLUpdatePutItem.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import java.util.Map;
import java.util.Objects;

public class SQLUpdatePutItem extends SimpleNode {

  protected SQLIdentifier left;
  protected SQLExpression key;
  protected SQLExpression value;

  public SQLUpdatePutItem(int id) {
    super(id);
  }

  public SQLUpdatePutItem(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    builder.append(" = ");
    key.toString(params, builder);
    builder.append(", ");
    value.toString(params, builder);
  }

  public void toGenericStatement(StringBuilder builder) {
    left.toGenericStatement(builder);
    builder.append(" = ");
    key.toGenericStatement(builder);
    builder.append(", ");
    value.toGenericStatement(builder);
  }

  public SQLUpdatePutItem copy() {
    SQLUpdatePutItem result = new SQLUpdatePutItem(-1);
    result.left = left == null ? null : left.copy();
    result.key = key == null ? null : key.copy();
    result.value = value == null ? null : value.copy();
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

    SQLUpdatePutItem that = (SQLUpdatePutItem) o;

    if (!Objects.equals(left, that.left)) {
      return false;
    }
    if (!Objects.equals(key, that.key)) {
      return false;
    }
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (key != null ? key.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=a38339c33ebf0a8b21e76ddb278f4958 (do not edit this line) */