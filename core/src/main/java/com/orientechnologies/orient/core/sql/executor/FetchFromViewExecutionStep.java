package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.YTView;
import java.util.Set;

/**
 *
 */
public class FetchFromViewExecutionStep extends FetchFromClassExecutionStep {

  public FetchFromViewExecutionStep(
      String className,
      Set<String> clusters,
      QueryPlanningInfo planningInfo,
      OCommandContext ctx,
      Boolean ridOrder,
      boolean profilingEnabled) {
    super(className, clusters, planningInfo, ctx, ridOrder, profilingEnabled);

    YTDatabaseSessionInternal database = ctx.getDatabase();
    YTView view = loadClassFromSchema(className, ctx);
    int[] classClusters = view.getPolymorphicClusterIds();
    for (int clusterId : classClusters) {
      String clusterName = ctx.getDatabase().getClusterNameById(clusterId);
      if (clusters == null || clusters.contains(clusterName)) {
        database.queryStartUsingViewCluster(clusterId);
      }
    }
  }

  @Override
  public void close() {
    super.close();
  }

  protected YTView loadClassFromSchema(String className, OCommandContext ctx) {
    YTView clazz = ctx.getDatabase().getMetadata().getImmutableSchemaSnapshot().getView(className);
    if (clazz == null) {
      throw new OCommandExecutionException("View " + className + " not found");
    }
    return clazz;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    StringBuilder builder = new StringBuilder();
    String ind = OExecutionStepInternal.getIndent(depth, indent);
    builder.append(ind);
    builder.append("+ FETCH FROM VIEW ").append(className);
    if (profilingEnabled) {
      builder.append(" (").append(getCostFormatted()).append(")");
    }
    builder.append("\n");
    for (int i = 0; i < getSubSteps().size(); i++) {
      OExecutionStepInternal step = (OExecutionStepInternal) getSubSteps().get(i);
      builder.append(step.prettyPrint(depth + 1, indent));
      if (i < getSubSteps().size() - 1) {
        builder.append("\n");
      }
    }
    return builder.toString();
  }

  @Override
  public boolean canBeCached() {
    return false;
  }
}
