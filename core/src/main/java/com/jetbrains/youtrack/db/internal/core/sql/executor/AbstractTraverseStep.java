package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.common.concur.YTTimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OInteger;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OTraverseProjectionItem;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OWhereClause;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public abstract class AbstractTraverseStep extends AbstractExecutionStep {

  protected final OWhereClause whileClause;
  protected final List<OTraverseProjectionItem> projections;
  protected final OInteger maxDepth;

  public AbstractTraverseStep(
      List<OTraverseProjectionItem> projections,
      OWhereClause whileClause,
      OInteger maxDepth,
      CommandContext ctx,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.whileClause = whileClause;
    this.maxDepth = maxDepth;

    try (final Stream<OTraverseProjectionItem> stream = projections.stream()) {
      this.projections = stream.map(OTraverseProjectionItem::copy).collect(Collectors.toList());
    }
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
    assert prev != null;

    ExecutionStream resultSet = prev.start(ctx);
    return new ExecutionStream() {
      private final List<YTResult> entryPoints = new ArrayList<>();
      private final List<YTResult> results = new ArrayList<>();
      private final Set<YTRID> traversed = new ORidSet();

      @Override
      public boolean hasNext(CommandContext ctx) {
        if (results.isEmpty()) {
          fetchNextBlock(ctx, this.entryPoints, this.results, this.traversed, resultSet);
        }
        return !results.isEmpty();
      }

      @Override
      public YTResult next(CommandContext ctx) {
        if (!hasNext(ctx)) {
          throw new IllegalStateException();
        }
        YTResult result = results.remove(0);
        if (result.isEntity()) {
          this.traversed.add(result.toEntity().getIdentity());
        }
        return result;
      }

      @Override
      public void close(CommandContext ctx) {
      }
    };
  }

  private void fetchNextBlock(
      CommandContext ctx,
      List<YTResult> entryPoints,
      List<YTResult> results,
      Set<YTRID> traversed,
      ExecutionStream resultSet) {
    if (!results.isEmpty()) {
      return;
    }
    while (true) {
      if (entryPoints.isEmpty()) {
        fetchNextEntryPoints(resultSet, ctx, entryPoints, traversed);
      }
      if (entryPoints.isEmpty()) {
        return;
      }
      fetchNextResults(ctx, results, entryPoints, traversed);
      if (!results.isEmpty()) {
        return;
      }
    }
  }

  protected abstract void fetchNextEntryPoints(
      ExecutionStream toFetch,
      CommandContext ctx,
      List<YTResult> entryPoints,
      Set<YTRID> traversed);

  protected abstract void fetchNextResults(
      CommandContext ctx, List<YTResult> results, List<YTResult> entryPoints,
      Set<YTRID> traversed);

  @Override
  public String toString() {
    return prettyPrint(0, 2);
  }
}
