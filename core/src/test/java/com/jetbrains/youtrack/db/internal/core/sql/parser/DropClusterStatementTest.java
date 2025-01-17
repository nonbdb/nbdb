package com.jetbrains.youtrack.db.internal.core.sql.parser;

import org.junit.Test;

public class DropClusterStatementTest extends ParserTestAbstract {

  @Test
  public void testPlain() {
    checkRightSyntax("DROP CLUSTER Foo");
    checkRightSyntax("drop cluster Foo");
    checkRightSyntax("DROP CLUSTER 14");

    checkRightSyntax("DROP CLUSTER 14 IF EXISTS");

    checkWrongSyntax("DROP CLUSTER foo 14");
    checkWrongSyntax("DROP CLUSTER foo bar");
    checkWrongSyntax("DROP CLUSTER 14.1");
    checkWrongSyntax("DROP CLUSTER 14 1");

    checkWrongSyntax("DROP CLUSTER 14 IF NOT EXISTS");
  }
}
