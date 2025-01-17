package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ProduceExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIndexIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLInsertBody;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLInsertSetExpression;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class InsertIntoIndexStep extends AbstractExecutionStep {

  private final SQLIndexIdentifier targetIndex;
  private final SQLInsertBody body;

  public InsertIntoIndexStep(
      SQLIndexIdentifier targetIndex,
      SQLInsertBody insertBody,
      CommandContext ctx,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.targetIndex = targetIndex;
    this.body = insertBody;
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws TimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    return new ProduceExecutionStream(this::produce).limit(1);
  }

  private ResultInternal produce(CommandContext ctx) {
    final DatabaseSessionInternal database = ctx.getDatabase();
    Index index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, targetIndex.getIndexName());
    if (index == null) {
      throw new CommandExecutionException("Index not found: " + targetIndex);
    }
    List<SQLInsertSetExpression> setExps = body.getSetExpressions();
    if (body.getContent() != null) {
      throw new CommandExecutionException(
          "Invalid expression: INSERT INTO INDEX:... CONTENT ...");
    }
    long count;
    if (setExps != null) {
      count = handleSet(setExps, index, ctx);
    } else {
      count = handleKeyValues(body.getIdentifierList(), body.getValueExpressions(), index, ctx);
    }

    ResultInternal result = new ResultInternal(database);
    result.setProperty("count", count);
    return result;
  }

  private long handleKeyValues(
      List<SQLIdentifier> identifierList,
      List<List<SQLExpression>> setExpressions,
      Index index,
      CommandContext ctx) {
    SQLExpression keyExp = null;
    SQLExpression valueExp = null;
    if (identifierList == null || setExpressions == null) {
      throw new CommandExecutionException("Invalid insert expression");
    }
    long count = 0;
    for (List<SQLExpression> valList : setExpressions) {
      if (identifierList.size() != valList.size()) {
        throw new CommandExecutionException("Invalid insert expression");
      }
      for (int i = 0; i < identifierList.size(); i++) {
        SQLIdentifier key = identifierList.get(i);
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
      throw new CommandExecutionException("Invalid insert expression");
    }
    return count;
  }

  private long handleSet(List<SQLInsertSetExpression> setExps, Index index, CommandContext ctx) {
    SQLExpression keyExp = null;
    SQLExpression valueExp = null;
    for (SQLInsertSetExpression exp : setExps) {
      if (exp.getLeft().getStringValue().equalsIgnoreCase("key")) {
        keyExp = exp.getRight();
      } else if (exp.getLeft().getStringValue().equalsIgnoreCase("rid")) {
        valueExp = exp.getRight();
      } else {
        throw new CommandExecutionException("Cannot set " + exp + " on index");
      }
    }
    if (keyExp == null || valueExp == null) {
      throw new CommandExecutionException("Invalid insert expression");
    }
    return doExecute(index, ctx, keyExp, valueExp);
  }

  private long doExecute(
      Index index, CommandContext ctx, SQLExpression keyExp, SQLExpression valueExp) {
    long count = 0;
    Object key = keyExp.execute((Result) null, ctx);
    Object value = valueExp.execute((Result) null, ctx);
    if (value instanceof Identifiable) {
      insertIntoIndex(ctx.getDatabase(), index, key, (Identifiable) value);
      count++;
    } else if (value instanceof Result && ((Result) value).isEntity()) {
      insertIntoIndex(ctx.getDatabase(), index, key, ((Result) value).toEntity());
      count++;
    } else if (value instanceof ResultSet) {
      ((ResultSet) value).entityStream().forEach(x -> index.put(ctx.getDatabase(), key, x));
    } else if (MultiValue.isMultiValue(value)) {
      Iterator<?> iterator = MultiValue.getMultiValueIterator(value);
      while (iterator.hasNext()) {
        Object item = iterator.next();
        if (item instanceof Identifiable) {
          insertIntoIndex(ctx.getDatabase(), index, key, (Identifiable) item);
          count++;
        } else if (item instanceof Result && ((Result) item).isEntity()) {
          insertIntoIndex(ctx.getDatabase(), index, key, ((Result) item).toEntity());
          count++;
        } else {
          throw new CommandExecutionException("Cannot insert into index " + item);
        }
      }
    }
    return count;
  }

  private void insertIntoIndex(DatabaseSessionInternal session, final Index index,
      final Object key, final Identifiable value) {
    index.put(session, key, value);
  }
}
