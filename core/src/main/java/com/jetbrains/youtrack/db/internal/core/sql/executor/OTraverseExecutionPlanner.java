package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.index.OIndex;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.sql.CommandExecutorSQLAbstract;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OCluster;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OFromClause;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OFromItem;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OIndexIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OInputParameter;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OInteger;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OLimit;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OMetadataIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.ORid;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OSkip;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OTraverseProjectionItem;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OTraverseStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OWhereClause;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class OTraverseExecutionPlanner {

  private List<OTraverseProjectionItem> projections = null;
  private final OFromClause target;

  private final OWhereClause whileClause;

  private final OTraverseStatement.Strategy strategy;
  private final OInteger maxDepth;

  private final OSkip skip;
  private final OLimit limit;

  public OTraverseExecutionPlanner(OTraverseStatement statement) {
    // copying the content, so that it can be manipulated and optimized
    if (statement.getProjections() == null) {
      this.projections = null;
    } else {
      this.projections =
          statement.getProjections().stream().map(x -> x.copy()).collect(Collectors.toList());
    }

    this.target = statement.getTarget();
    this.whileClause =
        statement.getWhileClause() == null ? null : statement.getWhileClause().copy();

    this.strategy =
        statement.getStrategy() == null
            ? OTraverseStatement.Strategy.DEPTH_FIRST
            : statement.getStrategy();
    this.maxDepth = statement.getMaxDepth() == null ? null : statement.getMaxDepth().copy();

    this.skip = statement.getSkip();
    this.limit = statement.getLimit();
  }

  public OInternalExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    OSelectExecutionPlan result = new OSelectExecutionPlan(ctx);

    handleFetchFromTarger(result, ctx, enableProfiling);

    handleTraversal(result, ctx, enableProfiling);

    if (skip != null) {
      result.chain(new SkipExecutionStep(skip, ctx, enableProfiling));
    }
    if (limit != null) {
      result.chain(new LimitExecutionStep(limit, ctx, enableProfiling));
    }

    return result;
  }

  private void handleTraversal(
      OSelectExecutionPlan result, CommandContext ctx, boolean profilingEnabled) {
    switch (strategy) {
      case BREADTH_FIRST:
        result.chain(
            new BreadthFirstTraverseStep(
                this.projections, this.whileClause, maxDepth, ctx, profilingEnabled));
        break;
      case DEPTH_FIRST:
        result.chain(
            new DepthFirstTraverseStep(
                this.projections, this.whileClause, maxDepth, ctx, profilingEnabled));
        break;
    }
    // TODO
  }

  private void handleFetchFromTarger(
      OSelectExecutionPlan result, CommandContext ctx, boolean profilingEnabled) {

    OFromItem target = this.target == null ? null : this.target.getItem();
    if (target == null) {
      handleNoTarget(result, ctx, profilingEnabled);
    } else if (target.getIdentifier() != null) {
      handleClassAsTarget(result, this.target, ctx, profilingEnabled);
    } else if (target.getCluster() != null) {
      handleClustersAsTarget(
          result, Collections.singletonList(target.getCluster()), ctx, profilingEnabled);
    } else if (target.getClusterList() != null) {
      handleClustersAsTarget(
          result, target.getClusterList().toListOfClusters(), ctx, profilingEnabled);
    } else if (target.getStatement() != null) {
      handleSubqueryAsTarget(result, target.getStatement(), ctx, profilingEnabled);
    } else if (target.getFunctionCall() != null) {
      //        handleFunctionCallAsTarget(result, target.getFunctionCall(), ctx);//TODO
      throw new YTCommandExecutionException("function call as target is not supported yet");
    } else if (target.getInputParam() != null) {
      handleInputParamAsTarget(result, target.getInputParam(), ctx, profilingEnabled);
    } else if (target.getIndex() != null) {
      handleIndexAsTarget(result, target.getIndex(), ctx, profilingEnabled);
    } else if (target.getMetadata() != null) {
      handleMetadataAsTarget(result, target.getMetadata(), ctx, profilingEnabled);
    } else if (target.getRids() != null && target.getRids().size() > 0) {
      handleRidsAsTarget(result, target.getRids(), ctx, profilingEnabled);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private void handleInputParamAsTarget(
      OSelectExecutionPlan result,
      OInputParameter inputParam,
      CommandContext ctx,
      boolean profilingEnabled) {
    Object paramValue = inputParam.getValue(ctx.getInputParameters());
    if (paramValue == null) {
      result.chain(new EmptyStep(ctx, profilingEnabled)); // nothing to return
    } else if (paramValue instanceof YTClass) {
      OFromClause from = new OFromClause(-1);
      OFromItem item = new OFromItem(-1);
      from.setItem(item);
      item.setIdentifier(new OIdentifier(((YTClass) paramValue).getName()));
      handleClassAsTarget(result, from, ctx, profilingEnabled);
    } else if (paramValue instanceof String) {
      // strings are treated as classes
      OFromClause from = new OFromClause(-1);
      OFromItem item = new OFromItem(-1);
      from.setItem(item);
      item.setIdentifier(new OIdentifier((String) paramValue));
      handleClassAsTarget(result, from, ctx, profilingEnabled);
    } else if (paramValue instanceof YTIdentifiable) {
      YTRID orid = ((YTIdentifiable) paramValue).getIdentity();

      ORid rid = new ORid(-1);
      OInteger cluster = new OInteger(-1);
      cluster.setValue(orid.getClusterId());
      OInteger position = new OInteger(-1);
      position.setValue(orid.getClusterPosition());
      rid.setLegacy(true);
      rid.setCluster(cluster);
      rid.setPosition(position);

      handleRidsAsTarget(result, Collections.singletonList(rid), ctx, profilingEnabled);
    } else if (paramValue instanceof Iterable) {
      // try list of RIDs
      List<ORid> rids = new ArrayList<>();
      for (Object x : (Iterable) paramValue) {
        if (!(x instanceof YTIdentifiable)) {
          throw new YTCommandExecutionException("Cannot use colleciton as target: " + paramValue);
        }
        YTRID orid = ((YTIdentifiable) x).getIdentity();

        ORid rid = new ORid(-1);
        OInteger cluster = new OInteger(-1);
        cluster.setValue(orid.getClusterId());
        OInteger position = new OInteger(-1);
        position.setValue(orid.getClusterPosition());
        rid.setCluster(cluster);
        rid.setPosition(position);

        rids.add(rid);
      }
      handleRidsAsTarget(result, rids, ctx, profilingEnabled);
    } else {
      throw new YTCommandExecutionException("Invalid target: " + paramValue);
    }
  }

  private void handleNoTarget(
      OSelectExecutionPlan result, CommandContext ctx, boolean profilingEnabled) {
    result.chain(new EmptyDataGeneratorStep(1, ctx, profilingEnabled));
  }

  private void handleIndexAsTarget(
      OSelectExecutionPlan result,
      OIndexIdentifier indexIdentifier,
      CommandContext ctx,
      boolean profilingEnabled) {
    String indexName = indexIdentifier.getIndexName();
    final YTDatabaseSessionInternal database = ctx.getDatabase();
    OIndex index = database.getMetadata().getIndexManagerInternal().getIndex(database, indexName);
    if (index == null) {
      throw new YTCommandExecutionException("Index not found: " + indexName);
    }

    switch (indexIdentifier.getType()) {
      case INDEX:
        if (!index.supportsOrderedIterations()) {
          throw new YTCommandExecutionException(
              "Index " + indexName + " does not allow iteration without a condition");
        }

        result.chain(
            new FetchFromIndexStep(new IndexSearchDescriptor(index), true, ctx, profilingEnabled));
        result.chain(new GetValueFromIndexEntryStep(ctx, null, profilingEnabled));
        break;
      case VALUES:
      case VALUESASC:
        if (!index.supportsOrderedIterations()) {
          throw new YTCommandExecutionException(
              "Index " + indexName + " does not allow iteration on values");
        }
        result.chain(
            new FetchFromIndexValuesStep(
                new IndexSearchDescriptor(index), true, ctx, profilingEnabled));
        result.chain(new GetValueFromIndexEntryStep(ctx, null, profilingEnabled));
        break;
      case VALUESDESC:
        if (!index.supportsOrderedIterations()) {
          throw new YTCommandExecutionException(
              "Index " + indexName + " does not allow iteration on values");
        }
        result.chain(
            new FetchFromIndexValuesStep(
                new IndexSearchDescriptor(index), false, ctx, profilingEnabled));
        result.chain(new GetValueFromIndexEntryStep(ctx, null, profilingEnabled));
        break;
    }
  }

  private void handleMetadataAsTarget(
      OSelectExecutionPlan plan,
      OMetadataIdentifier metadata,
      CommandContext ctx,
      boolean profilingEnabled) {
    var db = ctx.getDatabase();
    String schemaRecordIdAsString;
    if (metadata.getName().equalsIgnoreCase(CommandExecutorSQLAbstract.METADATA_SCHEMA)) {
      schemaRecordIdAsString = db.getStorageInfo().getConfiguration().getSchemaRecordId();
    } else if (metadata.getName().equalsIgnoreCase(CommandExecutorSQLAbstract.METADATA_INDEXMGR)) {
      schemaRecordIdAsString = db.getStorageInfo().getConfiguration().getIndexMgrRecordId();
    } else {
      throw new UnsupportedOperationException("Invalid metadata: " + metadata.getName());
    }
    YTRecordId schemaRid = new YTRecordId(schemaRecordIdAsString);
    plan.chain(new FetchFromRidsStep(Collections.singleton(schemaRid), ctx, profilingEnabled));
  }

  private void handleRidsAsTarget(
      OSelectExecutionPlan plan, List<ORid> rids, CommandContext ctx, boolean profilingEnabled) {
    List<YTRecordId> actualRids = new ArrayList<>();
    for (ORid rid : rids) {
      actualRids.add(rid.toRecordId((YTResult) null, ctx));
    }
    plan.chain(new FetchFromRidsStep(actualRids, ctx, profilingEnabled));
  }

  private void handleClassAsTarget(
      OSelectExecutionPlan plan,
      OFromClause queryTarget,
      CommandContext ctx,
      boolean profilingEnabled) {
    OIdentifier identifier = queryTarget.getItem().getIdentifier();

    Boolean orderByRidAsc = null; // null: no order. true: asc, false:desc
    FetchFromClassExecutionStep fetcher =
        new FetchFromClassExecutionStep(
            identifier.getStringValue(), null, ctx, orderByRidAsc, profilingEnabled);
    plan.chain(fetcher);
  }

  private void handleClustersAsTarget(
      OSelectExecutionPlan plan,
      List<OCluster> clusters,
      CommandContext ctx,
      boolean profilingEnabled) {
    var db = ctx.getDatabase();
    Boolean orderByRidAsc = null; // null: no order. true: asc, false:desc
    if (clusters.size() == 1) {
      OCluster cluster = clusters.get(0);
      Integer clusterId = cluster.getClusterNumber();
      if (clusterId == null) {
        clusterId = db.getClusterIdByName(cluster.getClusterName());
      }
      if (clusterId == null) {
        throw new YTCommandExecutionException("Cluster " + cluster + " does not exist");
      }
      FetchFromClusterExecutionStep step =
          new FetchFromClusterExecutionStep(clusterId, ctx, profilingEnabled);
      if (Boolean.TRUE.equals(orderByRidAsc)) {
        step.setOrder(FetchFromClusterExecutionStep.ORDER_ASC);
      } else if (Boolean.FALSE.equals(orderByRidAsc)) {
        step.setOrder(FetchFromClusterExecutionStep.ORDER_DESC);
      }
      plan.chain(step);
    } else {
      int[] clusterIds = new int[clusters.size()];
      for (int i = 0; i < clusters.size(); i++) {
        OCluster cluster = clusters.get(i);
        Integer clusterId = cluster.getClusterNumber();
        if (clusterId == null) {
          clusterId = db.getClusterIdByName(cluster.getClusterName());
        }
        if (clusterId == null) {
          throw new YTCommandExecutionException("Cluster " + cluster + " does not exist");
        }
        clusterIds[i] = clusterId;
      }
      FetchFromClustersExecutionStep step =
          new FetchFromClustersExecutionStep(clusterIds, ctx, orderByRidAsc, profilingEnabled);
      plan.chain(step);
    }
  }

  private void handleSubqueryAsTarget(
      OSelectExecutionPlan plan,
      OStatement subQuery,
      CommandContext ctx,
      boolean profilingEnabled) {
    BasicCommandContext subCtx = new BasicCommandContext();
    subCtx.setDatabase(ctx.getDatabase());
    subCtx.setParent(ctx);
    OInternalExecutionPlan subExecutionPlan =
        subQuery.createExecutionPlan(subCtx, profilingEnabled);
    plan.chain(new SubQueryStep(subExecutionPlan, ctx, subCtx, profilingEnabled));
  }
}