package com.orientechnologies.orient.core.sql.executor;

/**
 *
 */

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class OIfExecutionPlan implements OInternalExecutionPlan {

  private String location;

  private final OCommandContext ctx;

  @Override
  public OCommandContext getContext() {
    return ctx;
  }

  protected IfStep step;

  public OIfExecutionPlan(OCommandContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void reset(OCommandContext ctx) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    step.close();
  }

  @Override
  public OExecutionStream start() {
    return step.start(ctx);
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    return step.prettyPrint(depth, indent);
  }

  public void chain(IfStep step) {
    this.step = step;
  }

  @Override
  public List<OExecutionStep> getSteps() {
    // TODO do a copy of the steps
    return Collections.singletonList(step);
  }

  public void setSteps(List<OExecutionStepInternal> steps) {
    this.step = (IfStep) steps.get(0);
  }

  @Override
  public OResult toResult(YTDatabaseSessionInternal db) {
    OResultInternal result = new OResultInternal(db);
    result.setProperty("type", "IfExecutionPlan");
    result.setProperty("javaType", getClass().getName());
    result.setProperty("cost", getCost());
    result.setProperty("prettyPrint", prettyPrint(0, 2));
    result.setProperty("steps", Collections.singletonList(step.toResult(db)));
    return result;
  }

  @Override
  public long getCost() {
    return 0L;
  }

  @Override
  public boolean canBeCached() {
    return false;
  }

  public OExecutionStepInternal executeUntilReturn() {
    OScriptExecutionPlan plan = step.producePlan(ctx);
    if (plan != null) {
      return plan.executeUntilReturn();
    } else {
      return null;
    }
  }

  public boolean containsReturn() {
    return step.containsReturn();
  }
}
