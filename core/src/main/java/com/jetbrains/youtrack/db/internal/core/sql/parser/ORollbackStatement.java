/* Generated By:JJTree: Do not edit this line. ORollbackStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Map;

public class ORollbackStatement extends OSimpleExecStatement {

  public ORollbackStatement(int id) {
    super(id);
  }

  public ORollbackStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    ctx.getDatabase().rollback();
    var db = ctx.getDatabase();
    YTResultInternal item = new YTResultInternal(db);
    item.setProperty("operation", "rollback");
    return ExecutionStream.singleton(item);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ROLLBACK");
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("ROLLBACK");
  }

  @Override
  public ORollbackStatement copy() {
    ORollbackStatement result = new ORollbackStatement(-1);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
/* JavaCC - OriginalChecksum=7efe0306e0cec51e035d64cad02ebc30 (do not edit this line) */
