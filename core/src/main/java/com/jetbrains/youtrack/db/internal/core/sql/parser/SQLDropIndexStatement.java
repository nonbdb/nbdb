/* Generated By:JJTree: Do not edit this line. SQLDropIndexStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.IndexManagerAbstract;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SQLDropIndexStatement extends DDLStatement {

  protected boolean all = false;
  protected SQLIndexName name;
  protected boolean ifExists = false;

  public SQLDropIndexStatement(int id) {
    super(id);
  }

  public SQLDropIndexStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeDDL(CommandContext ctx) {
    List<Result> rs = new ArrayList<>();
    DatabaseSessionInternal db = ctx.getDatabase();
    IndexManagerAbstract idxMgr = db.getMetadata().getIndexManagerInternal();
    if (all) {
      for (Index idx : idxMgr.getIndexes(db)) {
        db.getMetadata().getIndexManagerInternal().dropIndex(db, idx.getName());
        ResultInternal result = new ResultInternal(db);
        result.setProperty("operation", "drop index");
        result.setProperty("clusterName", idx.getName());
        rs.add(result);
      }

    } else {
      if (!idxMgr.existsIndex(name.getValue()) && !ifExists) {
        throw new CommandExecutionException("Index not found: " + name.getValue());
      }
      idxMgr.dropIndex(db, name.getValue());
      ResultInternal result = new ResultInternal(db);
      result.setProperty("operation", "drop index");
      result.setProperty("indexName", name.getValue());
      rs.add(result);
    }

    return ExecutionStream.resultIterator(rs.iterator());
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DROP INDEX ");
    if (all) {
      builder.append("*");
    } else {
      name.toString(params, builder);
    }
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("DROP INDEX ");
    if (all) {
      builder.append("*");
    } else {
      name.toGenericStatement(builder);
    }
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public SQLDropIndexStatement copy() {
    SQLDropIndexStatement result = new SQLDropIndexStatement(-1);
    result.all = all;
    result.name = name == null ? null : name.copy();
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

    SQLDropIndexStatement that = (SQLDropIndexStatement) o;

    if (all != that.all) {
      return false;
    }
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    int result = (all ? 1 : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=51c8221d049e4f114378e4be03797050 (do not edit this line) */
