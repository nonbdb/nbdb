package com.jetbrains.youtrack.db.internal.core.sql.executor.resultset;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;

public class FilterExecutionStream implements ExecutionStream {

  private final ExecutionStream prevResult;
  private final FilterResult filter;
  private YTResult nextItem = null;

  public FilterExecutionStream(ExecutionStream resultSet, FilterResult filter) {
    super();
    this.prevResult = resultSet;
    this.filter = filter;
  }

  @Override
  public boolean hasNext(CommandContext ctx) {
    if (nextItem == null) {
      fetchNextItem(ctx);
    }

    return nextItem != null;
  }

  @Override
  public YTResult next(CommandContext ctx) {
    if (nextItem == null) {
      fetchNextItem(ctx);
    }
    if (nextItem == null) {
      throw new IllegalStateException();
    }
    YTResult result = nextItem;
    nextItem = null;
    return result;
  }

  @Override
  public void close(CommandContext ctx) {
    this.prevResult.close(ctx);
  }

  private void fetchNextItem(CommandContext ctx) {
    while (prevResult.hasNext(ctx)) {
      nextItem = prevResult.next(ctx);
      nextItem = filter.filterMap(nextItem, ctx);
      if (nextItem != null) {
        break;
      }
    }
  }
}