package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.common.concur.YTTimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.record.Edge;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;

/**
 *
 */
public class CastToEdgeStep extends AbstractExecutionStep {

  public CastToEdgeStep(CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
    assert prev != null;
    ExecutionStream upstream = prev.start(ctx);
    return upstream.map(this::mapResult);
  }

  private YTResult mapResult(YTResult result, CommandContext ctx) {
    if (result.getEntity().orElse(null) instanceof Edge) {
      return result;
    }
    var db = ctx.getDatabase();
    if (result.isEdge()) {
      if (result instanceof YTResultInternal) {
        ((YTResultInternal) result).setIdentifiable(result.toEntity().toEdge());
      } else {
        result = new YTResultInternal(db, result.toEntity().toEdge());
      }
    } else {
      throw new YTCommandExecutionException("Current element is not a vertex: " + result);
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String result = ExecutionStepInternal.getIndent(depth, indent) + "+ CAST TO EDGE";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }

  @Override
  public ExecutionStep copy(CommandContext ctx) {
    return new CastToEdgeStep(ctx, profilingEnabled);
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}