package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OFromClause;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OLimit;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OProjection;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OSelectStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OTimeout;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OUpdateEdgeStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OUpdateOperations;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OUpdateStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OWhereClause;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class OUpdateExecutionPlanner {

  private final OFromClause target;
  public OWhereClause whereClause;

  protected boolean upsert = false;

  protected List<OUpdateOperations> operations = new ArrayList<OUpdateOperations>();
  protected boolean returnBefore = false;
  protected boolean returnAfter = false;
  protected boolean returnCount = false;

  protected boolean updateEdge = false;

  protected OProjection returnProjection;

  public OLimit limit;
  public OTimeout timeout;

  public OUpdateExecutionPlanner(OUpdateStatement oUpdateStatement) {
    if (oUpdateStatement instanceof OUpdateEdgeStatement) {
      updateEdge = true;
    }
    this.target = oUpdateStatement.getTarget().copy();
    this.whereClause =
        oUpdateStatement.getWhereClause() == null ? null : oUpdateStatement.getWhereClause().copy();
    if (oUpdateStatement.getOperations() == null) {
      this.operations = null;
    } else {
      this.operations =
          oUpdateStatement.getOperations().stream().map(x -> x.copy()).collect(Collectors.toList());
    }
    this.upsert = oUpdateStatement.isUpsert();

    this.returnBefore = oUpdateStatement.isReturnBefore();
    this.returnAfter = oUpdateStatement.isReturnAfter();
    this.returnCount = !(returnAfter || returnBefore);
    this.returnProjection =
        oUpdateStatement.getReturnProjection() == null
            ? null
            : oUpdateStatement.getReturnProjection().copy();
    this.limit = oUpdateStatement.getLimit() == null ? null : oUpdateStatement.getLimit().copy();
    this.timeout =
        oUpdateStatement.getTimeout() == null ? null : oUpdateStatement.getTimeout().copy();
  }

  public OUpdateExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    OUpdateExecutionPlan result = new OUpdateExecutionPlan(ctx);

    handleTarget(result, ctx, this.target, this.whereClause, this.timeout, enableProfiling);
    if (updateEdge) {
      result.chain(new CheckRecordTypeStep(ctx, "E", enableProfiling));
    }
    handleUpsert(result, ctx, this.target, this.whereClause, this.upsert, enableProfiling);
    handleTimeout(result, ctx, this.timeout, enableProfiling);
    convertToModifiableResult(result, ctx, enableProfiling);
    handleLimit(result, ctx, this.limit, enableProfiling);
    handleReturnBefore(result, ctx, this.returnBefore, enableProfiling);
    handleOperations(result, ctx, this.operations, enableProfiling);
    handleSave(result, ctx, enableProfiling);
    handleResultForReturnBefore(result, ctx, this.returnBefore, returnProjection, enableProfiling);
    handleResultForReturnAfter(result, ctx, this.returnAfter, returnProjection, enableProfiling);
    handleResultForReturnCount(result, ctx, this.returnCount, enableProfiling);
    return result;
  }

  /**
   * add a step that transforms a normal YTResult in a specific object that under setProperty()
   * updates the actual YTIdentifiable
   *
   * @param plan the execution plan
   * @param ctx  the executino context
   */
  private void convertToModifiableResult(
      OUpdateExecutionPlan plan, CommandContext ctx, boolean profilingEnabled) {
    plan.chain(new ConvertToUpdatableResultStep(ctx, profilingEnabled));
  }

  private void handleResultForReturnCount(
      OUpdateExecutionPlan result,
      CommandContext ctx,
      boolean returnCount,
      boolean profilingEnabled) {
    if (returnCount) {
      result.chain(new CountStep(ctx, profilingEnabled));
    }
  }

  private void handleResultForReturnAfter(
      OUpdateExecutionPlan result,
      CommandContext ctx,
      boolean returnAfter,
      OProjection returnProjection,
      boolean profilingEnabled) {
    if (returnAfter) {
      // re-convert to normal step
      result.chain(new ConvertToResultInternalStep(ctx, profilingEnabled));
      if (returnProjection != null) {
        result.chain(new ProjectionCalculationStep(returnProjection, ctx, profilingEnabled));
      }
    }
  }

  private void handleResultForReturnBefore(
      OUpdateExecutionPlan result,
      CommandContext ctx,
      boolean returnBefore,
      OProjection returnProjection,
      boolean profilingEnabled) {
    if (returnBefore) {
      result.chain(new UnwrapPreviousValueStep(ctx, profilingEnabled));
      if (returnProjection != null) {
        result.chain(new ProjectionCalculationStep(returnProjection, ctx, profilingEnabled));
      }
    }
  }

  private void handleSave(
      OUpdateExecutionPlan result, CommandContext ctx, boolean profilingEnabled) {
    result.chain(new SaveElementStep(ctx, profilingEnabled));
  }

  private void handleTimeout(
      OUpdateExecutionPlan result,
      CommandContext ctx,
      OTimeout timeout,
      boolean profilingEnabled) {
    if (timeout != null && timeout.getVal().longValue() > 0) {
      result.chain(new TimeoutStep(timeout, ctx, profilingEnabled));
    }
  }

  private void handleReturnBefore(
      OUpdateExecutionPlan result,
      CommandContext ctx,
      boolean returnBefore,
      boolean profilingEnabled) {
    if (returnBefore) {
      result.chain(new CopyRecordContentBeforeUpdateStep(ctx, profilingEnabled));
    }
  }

  private void handleLimit(
      OUpdateExecutionPlan plan, CommandContext ctx, OLimit limit, boolean profilingEnabled) {
    if (limit != null) {
      plan.chain(new LimitExecutionStep(limit, ctx, profilingEnabled));
    }
  }

  private void handleUpsert(
      OUpdateExecutionPlan plan,
      CommandContext ctx,
      OFromClause target,
      OWhereClause where,
      boolean upsert,
      boolean profilingEnabled) {
    if (upsert) {
      plan.chain(new UpsertStep(target, where, ctx, profilingEnabled));
    }
  }

  private void handleOperations(
      OUpdateExecutionPlan plan,
      CommandContext ctx,
      List<OUpdateOperations> ops,
      boolean profilingEnabled) {
    if (ops != null) {
      for (OUpdateOperations op : ops) {
        switch (op.getType()) {
          case OUpdateOperations.TYPE_SET:
            plan.chain(new UpdateSetStep(op.getUpdateItems(), ctx, profilingEnabled));
            if (updateEdge) {
              plan.chain(new UpdateEdgePointersStep(ctx, profilingEnabled));
            }
            break;
          case OUpdateOperations.TYPE_REMOVE:
            plan.chain(new UpdateRemoveStep(op.getUpdateRemoveItems(), ctx, profilingEnabled));
            break;
          case OUpdateOperations.TYPE_MERGE:
            plan.chain(new UpdateMergeStep(op.getJson(), ctx, profilingEnabled));
            break;
          case OUpdateOperations.TYPE_CONTENT:
            plan.chain(new UpdateContentStep(op.getJson(), ctx, profilingEnabled));
            break;
          case OUpdateOperations.TYPE_PUT:
          case OUpdateOperations.TYPE_INCREMENT:
          case OUpdateOperations.TYPE_ADD:
            throw new YTCommandExecutionException(
                "Cannot execute with UPDATE PUT/ADD/INCREMENT new executor: " + op);
        }
      }
    }
  }

  private void handleTarget(
      OUpdateExecutionPlan result,
      CommandContext ctx,
      OFromClause target,
      OWhereClause whereClause,
      OTimeout timeout,
      boolean profilingEnabled) {
    OSelectStatement sourceStatement = new OSelectStatement(-1);
    sourceStatement.setTarget(target);
    sourceStatement.setWhereClause(whereClause);
    if (timeout != null) {
      sourceStatement.setTimeout(this.timeout.copy());
    }
    OSelectExecutionPlanner planner = new OSelectExecutionPlanner(sourceStatement);
    result.chain(
        new SubQueryStep(
            planner.createExecutionPlan(ctx, profilingEnabled, false), ctx, ctx, profilingEnabled));
  }
}