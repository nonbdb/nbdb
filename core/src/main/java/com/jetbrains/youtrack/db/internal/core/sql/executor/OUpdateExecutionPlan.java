package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OUpdateExecutionPlan extends OSelectExecutionPlan {

  private final List<YTResult> result = new ArrayList<>();
  private int next = 0;

  public OUpdateExecutionPlan(CommandContext ctx) {
    super(ctx);
  }

  @Override
  public ExecutionStream start() {
    return ExecutionStream.resultIterator(result.iterator());
  }

  @Override
  public void reset(CommandContext ctx) {
    result.clear();
    next = 0;
    super.reset(ctx);
    executeInternal();
  }

  public void executeInternal() throws YTCommandExecutionException {
    ExecutionStream nextBlock = super.start();
    while (nextBlock.hasNext(ctx)) {
      result.add(nextBlock.next(ctx));
    }
    nextBlock.close(ctx);
  }

  @Override
  public YTResult toResult(YTDatabaseSessionInternal db) {
    YTResultInternal res = (YTResultInternal) super.toResult(db);
    res.setProperty("type", "UpdateExecutionPlan");
    return res;
  }

  @Override
  public boolean canBeCached() {
    for (ExecutionStepInternal step : steps) {
      if (!step.canBeCached()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public OInternalExecutionPlan copy(CommandContext ctx) {
    OUpdateExecutionPlan copy = new OUpdateExecutionPlan(ctx);
    super.copyOn(copy, ctx);
    return copy;
  }
}
