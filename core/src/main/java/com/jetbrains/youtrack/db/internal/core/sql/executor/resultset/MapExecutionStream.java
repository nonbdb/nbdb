package com.jetbrains.youtrack.db.internal.core.sql.executor.resultset;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;

public interface MapExecutionStream {

  ExecutionStream flatMap(YTResult next, CommandContext ctx);
}