package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.sql.parser.OBatch;
import com.orientechnologies.orient.core.sql.parser.OCreateEdgeStatement;
import com.orientechnologies.orient.core.sql.parser.OExecutionPlanCache;
import com.orientechnologies.orient.core.sql.parser.OExpression;
import com.orientechnologies.orient.core.sql.parser.OIdentifier;
import com.orientechnologies.orient.core.sql.parser.OInputParameter;
import com.orientechnologies.orient.core.sql.parser.OInsertBody;
import com.orientechnologies.orient.core.sql.parser.OInsertSetExpression;
import com.orientechnologies.orient.core.sql.parser.OJson;
import com.orientechnologies.orient.core.sql.parser.OUpdateItem;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OCreateEdgeExecutionPlanner {

  private final OCreateEdgeStatement statement;
  protected OIdentifier targetClass;
  protected OIdentifier targetClusterName;
  protected OExpression leftExpression;
  protected OExpression rightExpression;

  protected boolean upsert = false;

  protected OInsertBody body;
  protected Number retry;
  protected Number wait;
  protected OBatch batch;

  public OCreateEdgeExecutionPlanner(OCreateEdgeStatement statement) {
    this.statement = statement;
    this.targetClass =
        statement.getTargetClass() == null ? null : statement.getTargetClass().copy();
    this.targetClusterName =
        statement.getTargetClusterName() == null ? null : statement.getTargetClusterName().copy();
    this.leftExpression =
        statement.getLeftExpression() == null ? null : statement.getLeftExpression().copy();
    this.rightExpression =
        statement.getRightExpression() == null ? null : statement.getRightExpression().copy();
    this.upsert = statement.isUpsert();
    this.body = statement.getBody() == null ? null : statement.getBody().copy();
    this.retry = statement.getRetry();
    this.wait = statement.getWait();
    this.batch = statement.getBatch() == null ? null : statement.getBatch().copy();
  }

  public OInsertExecutionPlan createExecutionPlan(
      OCommandContext ctx, boolean enableProfiling, boolean useCache) {
    ODatabaseSessionInternal db = ctx.getDatabase();
    if (useCache && !enableProfiling && statement.executinPlanCanBeCached(db)) {
      OExecutionPlan plan = OExecutionPlanCache.get(statement.getOriginalStatement(), ctx, db);
      if (plan != null) {
        return (OInsertExecutionPlan) plan;
      }
    }

    long planningStart = System.currentTimeMillis();

    if (targetClass == null) {
      if (targetClusterName == null) {
        targetClass = new OIdentifier("E");
      } else {
        OClass clazz =
            db.getMetadata()
                .getImmutableSchemaSnapshot()
                .getClassByClusterId(db.getClusterIdByName(targetClusterName.getStringValue()));
        if (clazz != null) {
          targetClass = new OIdentifier(clazz.getName());
        } else {
          targetClass = new OIdentifier("E");
        }
      }
    }

    OInsertExecutionPlan result = new OInsertExecutionPlan(ctx);

    handleCheckType(result, ctx, enableProfiling);

    handleGlobalLet(
        result,
        new OIdentifier("$__ORIENT_CREATE_EDGE_fromV"),
        leftExpression,
        ctx,
        enableProfiling);
    handleGlobalLet(
        result,
        new OIdentifier("$__ORIENT_CREATE_EDGE_toV"),
        rightExpression,
        ctx,
        enableProfiling);

    String uniqueIndexName = null;
    if (upsert) {
      OClass clazz =
          ctx.getDatabase()
              .getMetadata()
              .getImmutableSchemaSnapshot()
              .getClass(targetClass.getStringValue());
      if (clazz == null) {
        throw new OCommandExecutionException(
            "Class " + targetClass + " not found in the db schema");
      }
      uniqueIndexName =
          clazz.getIndexes(db).stream()
              .filter(OIndex::isUnique)
              .filter(
                  x ->
                      x.getDefinition().getFields().size() == 2
                          && x.getDefinition().getFields().contains("out")
                          && x.getDefinition().getFields().contains("in"))
              .map(OIndex::getName)
              .findFirst()
              .orElse(null);

      if (uniqueIndexName == null) {
        throw new OCommandExecutionException(
            "Cannot perform an UPSERT on "
                + targetClass
                + " edge class: no unique index present on out/in");
      }
    }

    result.chain(
        new CreateEdgesStep(
            targetClass,
            targetClusterName,
            uniqueIndexName,
            new OIdentifier("$__ORIENT_CREATE_EDGE_fromV"),
            new OIdentifier("$__ORIENT_CREATE_EDGE_toV"),
            wait,
            retry,
            batch,
            ctx,
            enableProfiling));

    handleSetFields(result, body, ctx, enableProfiling);
    handleSave(result, targetClusterName, ctx, enableProfiling);
    // TODO implement batch, wait and retry

    if (useCache
        && !enableProfiling
        && statement.executinPlanCanBeCached(db)
        && result.canBeCached()
        && OExecutionPlanCache.getLastInvalidation(db) < planningStart) {
      OExecutionPlanCache.put(statement.getOriginalStatement(), result, ctx.getDatabase());
    }

    return result;
  }

  private void handleGlobalLet(
      OInsertExecutionPlan result,
      OIdentifier name,
      OExpression expression,
      OCommandContext ctx,
      boolean profilingEnabled) {
    result.chain(new GlobalLetExpressionStep(name, expression, ctx, profilingEnabled));
  }

  private void handleCheckType(
      OInsertExecutionPlan result, OCommandContext ctx, boolean profilingEnabled) {
    if (targetClass != null) {
      result.chain(
          new CheckClassTypeStep(targetClass.getStringValue(), "E", ctx, profilingEnabled));
    }
  }

  private void handleSave(
      OInsertExecutionPlan result,
      OIdentifier targetClusterName,
      OCommandContext ctx,
      boolean profilingEnabled) {
    result.chain(new SaveElementStep(ctx, targetClusterName, profilingEnabled));
  }

  private void handleSetFields(
      OInsertExecutionPlan result,
      OInsertBody insertBody,
      OCommandContext ctx,
      boolean profilingEnabled) {
    if (insertBody == null) {
      return;
    }
    if (insertBody.getIdentifierList() != null) {
      result.chain(
          new InsertValuesStep(
              insertBody.getIdentifierList(),
              insertBody.getValueExpressions(),
              ctx,
              profilingEnabled));
    } else if (insertBody.getContent() != null) {
      for (OJson json : insertBody.getContent()) {
        result.chain(new UpdateContentStep(json, ctx, profilingEnabled));
      }
    } else if (insertBody.getContentInputParam() != null) {
      for (OInputParameter inputParam : insertBody.getContentInputParam()) {
        result.chain(new UpdateContentStep(inputParam, ctx, profilingEnabled));
      }
    } else if (insertBody.getSetExpressions() != null) {
      List<OUpdateItem> items = new ArrayList<>();
      for (OInsertSetExpression exp : insertBody.getSetExpressions()) {
        OUpdateItem item = new OUpdateItem(-1);
        item.setOperator(OUpdateItem.OPERATOR_EQ);
        item.setLeft(exp.getLeft().copy());
        item.setRight(exp.getRight().copy());
        items.add(item);
      }
      result.chain(new UpdateSetStep(items, ctx, profilingEnabled));
    }
  }
}
