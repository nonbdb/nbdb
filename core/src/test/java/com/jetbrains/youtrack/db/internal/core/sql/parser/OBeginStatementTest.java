package com.jetbrains.youtrack.db.internal.core.sql.parser;

import org.junit.Test;

public class OBeginStatementTest extends OParserTestAbstract {

  @Test
  public void testPlain() {
    checkRightSyntax("BEGIN");
    checkRightSyntax("begin");

    checkWrongSyntax("BEGIN foo ");
  }
}