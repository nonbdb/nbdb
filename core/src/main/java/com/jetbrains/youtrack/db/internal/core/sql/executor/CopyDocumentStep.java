package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.common.concur.YTTimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;

/**
 * Reads an upstream result set and returns a new result set that contains copies of the original
 * YTResult instances
 *
 * <p>This is mainly used from statements that need to copy of the original data to save it
 * somewhere else, eg. INSERT ... FROM SELECT
 */
public class CopyDocumentStep extends AbstractExecutionStep {

  public CopyDocumentStep(CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
    assert prev != null;

    ExecutionStream upstream = prev.start(ctx);
    return upstream.map(CopyDocumentStep::mapResult);
  }

  private static YTResult mapResult(YTResult result, CommandContext ctx) {
    EntityImpl resultDoc;
    if (result.isEntity()) {
      var docToCopy = (EntityImpl) result.toEntity();
      resultDoc = docToCopy.copy();
      resultDoc.getIdentity().reset();
      resultDoc.setClassName(null);
      resultDoc.setDirty();
    } else {
      resultDoc = (EntityImpl) result.toEntity();
    }
    return new YTUpdatableResult(ctx.getDatabase(), resultDoc);
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = ExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ COPY DOCUMENT");
    if (profilingEnabled) {
      result.append(" (").append(getCostFormatted()).append(")");
    }
    return result.toString();
  }
}
