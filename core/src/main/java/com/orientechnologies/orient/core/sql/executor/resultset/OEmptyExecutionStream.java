package com.orientechnologies.orient.core.sql.executor.resultset;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.YTResult;

public class OEmptyExecutionStream implements OExecutionStream {

  protected static final OExecutionStream EMPTY = new OEmptyExecutionStream();

  @Override
  public boolean hasNext(OCommandContext ctx) {
    return false;
  }

  @Override
  public YTResult next(OCommandContext ctx) {
    throw new IllegalStateException();
  }

  @Override
  public void close(OCommandContext ctx) {
  }
}
