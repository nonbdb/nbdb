package com.orientechnologies.orient.core.sql.executor;

/**
 *
 */

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.parser.WhileStep;

/**
 *
 */
public class OForEachExecutionPlan extends OUpdateExecutionPlan {

  public OForEachExecutionPlan(OCommandContext ctx) {
    super(ctx);
  }

  public boolean containsReturn() {
    for (OExecutionStep step : getSteps()) {
      if (step instanceof ForEachStep) {
        return ((ForEachStep) step).containsReturn();
      }
      if (step instanceof WhileStep) {
        return ((WhileStep) step).containsReturn();
      }
    }

    return false;
  }
}
