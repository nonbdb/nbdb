package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.YTTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.executor.resultset.OProduceExecutionStream;

/**
 *
 */
public class CreateRecordStep extends AbstractExecutionStep {

  private int total = 0;

  public CreateRecordStep(OCommandContext ctx, int total, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.total = total;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws YTTimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    return new OProduceExecutionStream(CreateRecordStep::produce).limit(total);
  }

  private static YTResult produce(OCommandContext ctx) {
    var db = ctx.getDatabase();
    return new YTUpdatableResult(db, db.newInstance());
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ CREATE EMPTY RECORDS");
    if (profilingEnabled) {
      result.append(" (").append(getCostFormatted()).append(")");
    }
    result.append("\n");
    result.append(spaces);
    if (total == 1) {
      result.append("  1 record");
    } else {
      result.append("  ").append(total).append(" record");
    }
    return result.toString();
  }
}
