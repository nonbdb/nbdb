/* Generated By:JJTree: Do not edit this line. SQLOptimizeDatabaseStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLOptimizeDatabaseStatement extends SQLSimpleExecStatement {

  protected List<SQLCommandLineOption> options = new ArrayList<SQLCommandLineOption>();
  private final int batch = 1000;

  public SQLOptimizeDatabaseStatement(int id) {
    super(id);
  }

  public SQLOptimizeDatabaseStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void addOption(SQLCommandLineOption option) {
    this.options.add(option);
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    var db = ctx.getDatabase();
    ResultInternal result = new ResultInternal(db);
    result.setProperty("operation", "optimize databae");

    if (isOptimizeEdges()) {
      String edges = optimizeEdges(db);
      result.setProperty("optimizeEdges", edges);
    }

    return ExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("OPTIMIZE DATABASE");
    for (SQLCommandLineOption option : options) {
      builder.append(" ");
      option.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("OPTIMIZE DATABASE");
    for (SQLCommandLineOption option : options) {
      builder.append(" ");
      option.toGenericStatement(builder);
    }
  }

  @Override
  public SQLOptimizeDatabaseStatement copy() {
    SQLOptimizeDatabaseStatement result = new SQLOptimizeDatabaseStatement(-1);
    result.options =
        options == null
            ? null
            : options.stream().map(SQLCommandLineOption::copy).collect(Collectors.toList());
    return result;
  }

  private String optimizeEdges(DatabaseSessionInternal db) {
    long transformed = 0;
    final long totalEdges = db.countClass("E");
    long browsedEdges = 0;
    long lastLapBrowsed = 0;
    long lastLapTime = System.currentTimeMillis();

    for (EntityImpl entity : db.browseClass("E")) {
      if (Thread.currentThread().isInterrupted()) {
        break;
      }

      browsedEdges++;

      if (entity != null) {
        if (entity.fields() == 2) {
          final RID edgeIdentity = entity.getIdentity();

          final EntityImpl outV = entity.getPropertyInternal("out");
          final EntityImpl inV = entity.getPropertyInternal("in");

          // OUTGOING
          final Object outField = outV.getPropertyInternal("out_" + entity.getClassName());
          if (outField instanceof RidBag) {
            final Iterator<Identifiable> it = ((RidBag) outField).iterator();
            while (it.hasNext()) {
              Identifiable v = it.next();
              if (edgeIdentity.equals(v)) {
                // REPLACE EDGE RID WITH IN-VERTEX RID
                it.remove();
                ((RidBag) outField).add(inV.getIdentity());
                break;
              }
            }
          }

          outV.save();

          // INCOMING
          final Object inField = inV.getPropertyInternal("in_" + entity.getClassName());
          if (outField instanceof RidBag) {
            final Iterator<Identifiable> it = ((RidBag) inField).iterator();
            while (it.hasNext()) {
              Identifiable v = it.next();
              if (edgeIdentity.equals(v)) {
                // REPLACE EDGE RID WITH IN-VERTEX RID
                it.remove();
                ((RidBag) inField).add(outV.getIdentity());
                break;
              }
            }
          }

          inV.save();

          entity.delete();

          final long now = System.currentTimeMillis();

          if (verbose() && (now - lastLapTime > 2000)) {
            final long elapsed = now - lastLapTime;

            LogManager.instance()
                .info(
                    this,
                    "Browsed %,d of %,d edges, transformed %,d so far (%,d edges/sec)",
                    browsedEdges,
                    totalEdges,
                    transformed,
                    (((browsedEdges - lastLapBrowsed) * 1000 / elapsed)));

            lastLapTime = System.currentTimeMillis();
            lastLapBrowsed = browsedEdges;
          }
        }
      }
    }

    return "Transformed " + transformed + " regular edges in lightweight edges";
  }

  private boolean isOptimizeEdges() {
    for (SQLCommandLineOption option : options) {
      if (option.name.getStringValue().equalsIgnoreCase("LWEDGES")) {
        return true;
      }
    }
    return false;
  }

  private boolean verbose() {
    for (SQLCommandLineOption option : options) {
      if (option.name.getStringValue().equalsIgnoreCase("NOVERBOSE")) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SQLOptimizeDatabaseStatement that = (SQLOptimizeDatabaseStatement) o;

    return Objects.equals(options, that.options);
  }

  @Override
  public int hashCode() {
    return options != null ? options.hashCode() : 0;
  }
}
/* JavaCC - OriginalChecksum=b85d66f84bbae92224565361df9d0c91 (do not edit this line) */
