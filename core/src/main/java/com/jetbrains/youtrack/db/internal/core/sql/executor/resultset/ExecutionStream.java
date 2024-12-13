package com.jetbrains.youtrack.db.internal.core.sql.executor.resultset;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.query.ExecutionStep;
import com.jetbrains.youtrack.db.api.query.Result;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ExecutionStream {

  boolean hasNext(CommandContext ctx);

  Result next(CommandContext ctx);

  void close(CommandContext ctx);

  default ExecutionStream map(ResultMapper mapper) {
    return new MapperExecutionStream(this, mapper);
  }

  default ExecutionStream filter(FilterResult filter) {
    return new FilterExecutionStream(this, filter);
  }

  default ExecutionStream flatMap(MapExecutionStream map) {
    return new FlatMapExecutionStream(this, map);
  }

  default ExecutionStream interruptable() {
    return new InterruptResultSet(this);
  }

  default ExecutionStream limit(long limit) {
    return new LimitedExecutionStream(this, limit);
  }

  static ExecutionStream iterator(Iterator<?> iterator) {
    return new IteratorExecutionStream(iterator);
  }

  static ExecutionStream resultIterator(Iterator<Result> iterator) {
    return new ResultIteratorExecutionStream(iterator);
  }

  default CostMeasureExecutionStream profile(ExecutionStep step) {
    return new CostMeasureExecutionStream(this, step);
  }

  static ExecutionStream loadIterator(Iterator<? extends Identifiable> iterator) {
    return new LoaderExecutionStream(iterator);
  }

  static ExecutionStream empty() {
    return EmptyExecutionStream.EMPTY;
  }

  static ExecutionStream singleton(Result result) {
    return new SingletonExecutionStream(result);
  }

  interface OnClose {

    void close(CommandContext ctx);
  }

  default ExecutionStream onClose(OnClose onClose) {
    return new OnCloseExecutionStream(this, onClose);
  }

  default Stream<Result> stream(CommandContext ctx) {
    return StreamSupport.stream(
            new Spliterator<Result>() {

              @Override
              public boolean tryAdvance(Consumer<? super Result> action) {
                if (hasNext(ctx)) {
                  action.accept(next(ctx));
                  return true;
                }
                return false;
              }

              @Override
              public Spliterator<Result> trySplit() {
                return null;
              }

              @Override
              public long estimateSize() {
                return Long.MAX_VALUE;
              }

              @Override
              public int characteristics() {
                return 0;
              }
            },
            false)
        .onClose(() -> this.close(ctx));
  }
}
