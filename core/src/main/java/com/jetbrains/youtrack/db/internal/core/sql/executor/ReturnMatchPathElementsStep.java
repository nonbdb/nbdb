package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class ReturnMatchPathElementsStep extends AbstractUnrollStep {

  public ReturnMatchPathElementsStep(CommandContext context, boolean profilingEnabled) {
    super(context, profilingEnabled);
  }

  @Override
  protected Collection<Result> unroll(Result doc, CommandContext iContext) {
    List<Result> result = new ArrayList<>();
    for (String s : doc.getPropertyNames()) {
      Object elem = doc.getProperty(s);
      if (elem instanceof Identifiable) {
        ResultInternal newelem = new ResultInternal(iContext.getDatabase(),
            (Identifiable) elem);
        elem = newelem;
      }
      if (elem instanceof Result) {
        result.add((Result) elem);
      }
      // else...? TODO
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = ExecutionStepInternal.getIndent(depth, indent);
    return spaces + "+ UNROLL $pathElements";
  }
}
