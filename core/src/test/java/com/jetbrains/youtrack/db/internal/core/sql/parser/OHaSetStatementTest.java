package com.jetbrains.youtrack.db.internal.core.sql.parser;

import org.junit.Test;

public class OHaSetStatementTest extends OParserTestAbstract {

  @Test
  public void testPlain() {
    checkRightSyntax("HA SET DBSTATUS china = 'OFFLINE'");
  }
}