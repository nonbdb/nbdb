package com.orientechnologies.orient.core.sql.functions.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.exception.YTQueryParsingException;
import com.orientechnologies.orient.core.sql.executor.YTResultSet;
import org.junit.Test;

public class OCustomSQLFunctionsTest extends DBTestBase {

  @Test
  public void testRandom() {
    YTResultSet result = db.query("select math_random() as random");
    assertTrue((Double) result.next().getProperty("random") > 0);
  }

  @Test
  public void testLog10() {
    YTResultSet result = db.query("select math_log10(10000) as log10");
    assertEquals(4.0, result.next().getProperty("log10"), 0.0001);
  }

  @Test
  public void testAbsInt() {
    YTResultSet result = db.query("select math_abs(-5) as abs");
    assertEquals(5, (int) (Integer) result.next().getProperty("abs"));
  }

  @Test
  public void testAbsDouble() {
    YTResultSet result = db.query("select math_abs(-5.0d) as abs");
    assertEquals(5.0, result.findFirst().getProperty("abs"), 0.0);
  }

  @Test
  public void testAbsFloat() {
    YTResultSet result = db.query("select math_abs(-5.0f) as abs");
    assertEquals(5.0f, result.findFirst().<Float>getProperty("abs"), 0.0);
  }

  @Test(expected = YTQueryParsingException.class)
  public void testNonExistingFunction() {
    db.query("select math_min('boom', 'boom') as boom").findFirst();
  }
}
