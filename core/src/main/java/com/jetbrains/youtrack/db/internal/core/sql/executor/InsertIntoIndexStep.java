package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.common.collection.OMultiValue;
import com.jetbrains.youtrack.db.internal.common.concur.YTTimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.index.OIndex;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ProduceExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OIndexIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OInsertBody;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OInsertSetExpression;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class InsertIntoIndexStep extends AbstractExecutionStep {

  private final OIndexIdentifier targetIndex;
  private final OInsertBody body;

  public InsertIntoIndexStep(
      OIndexIdentifier targetIndex,
      OInsertBody insertBody,
      CommandContext ctx,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.targetIndex = targetIndex;
    this.body = insertBody;
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    return new ProduceExecutionStream(this::produce).limit(1);
  }

  private YTResultInternal produce(CommandContext ctx) {
    final YTDatabaseSessionInternal database = ctx.getDatabase();
    OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, targetIndex.getIndexName());
    if (index == null) {
      throw new YTCommandExecutionException("Index not found: " + targetIndex);
    }
    List<OInsertSetExpression> setExps = body.getSetExpressions();
    if (body.getContent() != null) {
      throw new YTCommandExecutionException(
          "Invalid expression: INSERT INTO INDEX:... CONTENT ...");
    }
    long count;
    if (setExps != null) {
      count = handleSet(setExps, index, ctx);
    } else {
      count = handleKeyValues(body.getIdentifierList(), body.getValueExpressions(), index, ctx);
    }

    YTResultInternal result = new YTResultInternal(database);
    result.setProperty("count", count);
    return result;
  }

  private long handleKeyValues(
      List<OIdentifier> identifierList,
      List<List<OExpression>> setExpressions,
      OIndex index,
      CommandContext ctx) {
    OExpression keyExp = null;
    OExpression valueExp = null;
    if (identifierList == null || setExpressions == null) {
      throw new YTCommandExecutionException("Invalid insert expression");
    }
    long count = 0;
    for (List<OExpression> valList : setExpressions) {
      if (identifierList.size() != valList.size()) {
        throw new YTCommandExecutionException("Invalid insert expression");
      }
      for (int i = 0; i < identifierList.size(); i++) {
        OIdentifier key = identifierList.get(i);
        if (key.getStringValue().equalsIgnoreCase("key")) {
          keyExp = valList.get(i);
        }
        if (key.getStringValue().equalsIgnoreCase("rid")) {
          valueExp = valList.get(i);
        }
      }
      assert valueExp != null;
      assert keyExp != null;
      count += doExecute(index, ctx, keyExp, valueExp);
    }
    if (keyExp == null) {
      throw new YTCommandExecutionException("Invalid insert expression");
    }
    return count;
  }

  private long handleSet(List<OInsertSetExpression> setExps, OIndex index, CommandContext ctx) {
    OExpression keyExp = null;
    OExpression valueExp = null;
    for (OInsertSetExpression exp : setExps) {
      if (exp.getLeft().getStringValue().equalsIgnoreCase("key")) {
        keyExp = exp.getRight();
      } else if (exp.getLeft().getStringValue().equalsIgnoreCase("rid")) {
        valueExp = exp.getRight();
      } else {
        throw new YTCommandExecutionException("Cannot set " + exp + " on index");
      }
    }
    if (keyExp == null || valueExp == null) {
      throw new YTCommandExecutionException("Invalid insert expression");
    }
    return doExecute(index, ctx, keyExp, valueExp);
  }

  private long doExecute(
      OIndex index, CommandContext ctx, OExpression keyExp, OExpression valueExp) {
    long count = 0;
    Object key = keyExp.execute((YTResult) null, ctx);
    Object value = valueExp.execute((YTResult) null, ctx);
    if (value instanceof YTIdentifiable) {
      insertIntoIndex(ctx.getDatabase(), index, key, (YTIdentifiable) value);
      count++;
    } else if (value instanceof YTResult && ((YTResult) value).isEntity()) {
      insertIntoIndex(ctx.getDatabase(), index, key, ((YTResult) value).toEntity());
      count++;
    } else if (value instanceof YTResultSet) {
      ((YTResultSet) value).entityStream().forEach(x -> index.put(ctx.getDatabase(), key, x));
    } else if (OMultiValue.isMultiValue(value)) {
      Iterator<?> iterator = OMultiValue.getMultiValueIterator(value);
      while (iterator.hasNext()) {
        Object item = iterator.next();
        if (item instanceof YTIdentifiable) {
          insertIntoIndex(ctx.getDatabase(), index, key, (YTIdentifiable) item);
          count++;
        } else if (item instanceof YTResult && ((YTResult) item).isEntity()) {
          insertIntoIndex(ctx.getDatabase(), index, key, ((YTResult) item).toEntity());
          count++;
        } else {
          throw new YTCommandExecutionException("Cannot insert into index " + item);
        }
      }
    }
    return count;
  }

  private void insertIntoIndex(YTDatabaseSessionInternal session, final OIndex index,
      final Object key, final YTIdentifiable value) {
    index.put(session, key, value);
  }
}
