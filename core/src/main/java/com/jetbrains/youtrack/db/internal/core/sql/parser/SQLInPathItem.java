/* Generated By:JJTree: Do not edit this line. SQLInPathItem.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.query.Result;
import java.util.Map;

public class SQLInPathItem extends SQLMatchPathItem {

  public SQLInPathItem(int id) {
    super(id);
  }

  public SQLInPathItem(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("<-");
    boolean first = true;
    if (this.method.params != null) {
      for (SQLExpression exp : this.method.params) {
        if (!first) {
          builder.append(", ");
        }
        builder.append(exp.execute((Result) null, null));
        first = false;
      }
    }
    builder.append("-");
    if (filter != null) {
      filter.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("<-");
    boolean first = true;
    if (this.method.params != null) {
      for (SQLExpression exp : this.method.params) {
        if (!first) {
          builder.append(", ");
        }
        exp.toGenericStatement(builder);
        first = false;
      }
    }
    builder.append("-");
    if (filter != null) {
      filter.toGenericStatement(builder);
    }
  }
}
/* JavaCC - OriginalChecksum=a1d80718c0b913e46b7b6a1c38e0dc98 (do not edit this line) */
