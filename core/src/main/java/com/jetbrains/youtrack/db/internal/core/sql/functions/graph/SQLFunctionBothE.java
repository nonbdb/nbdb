package com.jetbrains.youtrack.db.internal.core.sql.functions.graph;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.record.Direction;

/**
 *
 */
public class SQLFunctionBothE extends SQLFunctionMove {

  public static final String NAME = "bothE";

  public SQLFunctionBothE() {
    super(NAME, 0, -1);
  }

  @Override
  protected Object move(
      final DatabaseSession graph, final Identifiable iRecord, final String[] iLabels) {
    return v2e(graph, iRecord, Direction.BOTH, iLabels);
  }
}
