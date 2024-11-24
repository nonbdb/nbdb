package com.orientechnologies.orient.core.sql.parser;

import static org.junit.Assert.fail;

import com.orientechnologies.BaseMemoryDatabase;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public abstract class OParserTestAbstract extends BaseMemoryDatabase {

  protected SimpleNode checkRightSyntax(String query) {
    SimpleNode result = checkSyntax(query, true);
    StringBuilder builder = new StringBuilder();
    result.toString(null, builder);
    return checkSyntax(builder.toString(), true);
  }

  protected SimpleNode checkRightSyntaxServer(String query) {
    SimpleNode result = checkSyntaxServer(query, true);
    StringBuilder builder = new StringBuilder();
    result.toString(null, builder);
    return checkSyntaxServer(builder.toString(), true);
  }

  protected SimpleNode checkWrongSyntax(String query) {
    return checkSyntax(query, false);
  }

  protected SimpleNode checkWrongSyntaxServer(String query) {
    return checkSyntaxServer(query, false);
  }

  protected SimpleNode checkSyntax(String query, boolean isCorrect) {
    OrientSql osql = getParserFor(query);
    try {
      SimpleNode result = osql.parse();
      if (!isCorrect) {
        fail();
      }

      return result;
    } catch (Exception e) {
      if (isCorrect) {
        fail();
      }
    }
    return null;
  }

  protected SimpleNode checkSyntaxServer(String query, boolean isCorrect) {
    OrientSql osql = getParserFor(query);
    try {
      SimpleNode result = osql.parseServerStatement();
      if (!isCorrect) {
        fail();
      }

      return result;
    } catch (Exception e) {
      if (isCorrect) {
        fail();
      }
    }
    return null;
  }

  protected OrientSql getParserFor(String string) {
    InputStream is = new ByteArrayInputStream(string.getBytes());
    OrientSql osql = new OrientSql(is);
    return osql;
  }
}
