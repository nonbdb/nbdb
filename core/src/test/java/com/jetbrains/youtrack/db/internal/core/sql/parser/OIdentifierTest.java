package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.sql.parser.OIdentifier;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OIdentifierTest {

  @Test
  public void testBackTickQuoted() {
    OIdentifier identifier = new OIdentifier("foo`bar");

    Assert.assertEquals(identifier.getStringValue(), "foo`bar");
    Assert.assertEquals(identifier.getValue(), "foo\\`bar");
  }
}