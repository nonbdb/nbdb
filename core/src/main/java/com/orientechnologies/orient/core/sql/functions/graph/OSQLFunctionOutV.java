package com.orientechnologies.orient.core.sql.functions.graph;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.ODirection;

/**
 *
 */
public class OSQLFunctionOutV extends OSQLFunctionMove {

  public static final String NAME = "outV";

  public OSQLFunctionOutV() {
    super(NAME, 0, -1);
  }

  @Override
  protected Object move(
      final ODatabaseSession graph, final OIdentifiable iRecord, final String[] iLabels) {
    return e2v(graph, iRecord, ODirection.OUT, iLabels);
  }
}
