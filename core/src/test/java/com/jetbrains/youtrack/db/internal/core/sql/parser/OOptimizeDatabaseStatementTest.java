package com.jetbrains.youtrack.db.internal.core.sql.parser;

import org.junit.Test;

public class OOptimizeDatabaseStatementTest extends OParserTestAbstract {

  @Test
  public void testPlain() {

    checkRightSyntax("OPTIMIZE DATABASE");
    checkRightSyntax("optimize database");
    checkRightSyntax("OPTIMIZE DATABASE -lwedges -noverbose");
    checkRightSyntax("OPTIMIZE DATABASE -lwedges");
  }
}