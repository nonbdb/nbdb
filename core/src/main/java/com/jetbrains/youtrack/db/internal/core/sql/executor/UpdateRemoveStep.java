package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.common.concur.YTTimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OUpdateRemoveItem;
import java.util.List;

/**
 *
 */
public class UpdateRemoveStep extends AbstractExecutionStep {

  private final List<OUpdateRemoveItem> items;

  public UpdateRemoveStep(
      List<OUpdateRemoveItem> items, CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.items = items;
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
    assert prev != null;
    ExecutionStream upstream = prev.start(ctx);
    return upstream.map(this::mapResult);
  }

  private YTResult mapResult(YTResult result, CommandContext ctx) {
    if (result instanceof YTResultInternal) {
      for (OUpdateRemoveItem item : items) {
        item.applyUpdate((YTResultInternal) result, ctx);
      }
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = ExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ UPDATE REMOVE");
    for (OUpdateRemoveItem item : items) {
      result.append("\n");
      result.append(spaces);
      result.append("  ");
      result.append(item.toString());
    }
    return result.toString();
  }
}