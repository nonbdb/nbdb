package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.YTTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.parser.OProjection;

/**
 *
 */
public class ProjectionCalculationStep extends AbstractExecutionStep {

  protected final OProjection projection;

  public ProjectionCalculationStep(
      OProjection projection, OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.projection = projection;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws YTTimeoutException {
    if (prev == null) {
      throw new IllegalStateException("Cannot calculate projections without a previous source");
    }

    OExecutionStream parentRs = prev.start(ctx);
    return parentRs.map(this::mapResult);
  }

  private YTResult mapResult(YTResult result, OCommandContext ctx) {
    Object oldCurrent = ctx.getVariable("$current");
    ctx.setVariable("$current", result);
    YTResult newResult = calculateProjections(ctx, result);
    ctx.setVariable("$current", oldCurrent);
    return newResult;
  }

  private YTResult calculateProjections(OCommandContext ctx, YTResult next) {
    return this.projection.calculateSingle(ctx, next);
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);

    String result = spaces + "+ CALCULATE PROJECTIONS";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    result += ("\n" + spaces + "  " + projection.toString());
    return result;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }

  @Override
  public OExecutionStep copy(OCommandContext ctx) {
    return new ProjectionCalculationStep(projection.copy(), ctx, profilingEnabled);
  }
}
