package com.jetbrains.youtrack.db.internal.core.sql.parser;

import org.junit.Test;

public class BeginStatementTest extends ParserTestAbstract {

  @Test
  public void testPlain() {
    checkRightSyntax("BEGIN");
    checkRightSyntax("begin");

    checkWrongSyntax("BEGIN foo ");
  }
}
