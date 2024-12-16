package com.jetbrains.youtrack.db.internal.core.sql.executor.resultset;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.query.Result;

public class MapperExecutionStream implements ExecutionStream {

  private final ExecutionStream upstream;
  private final ResultMapper mapper;

  public MapperExecutionStream(ExecutionStream upstream, ResultMapper mapper) {
    if (upstream == null || mapper == null) {
      throw new NullPointerException();
    }
    this.upstream = upstream;
    this.mapper = mapper;
  }

  @Override
  public boolean hasNext(CommandContext ctx) {
    return upstream.hasNext(ctx);
  }

  @Override
  public Result next(CommandContext ctx) {
    return this.mapper.map(upstream.next(ctx), ctx);
  }

  @Override
  public void close(CommandContext ctx) {
    this.upstream.close(ctx);
  }
}
