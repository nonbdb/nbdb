package com.jetbrains.youtrack.db.internal.core.sql.functions.stat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.jetbrains.youtrack.db.internal.DBTestBase;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.OSQLFunctionDecimal;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;

public class OSQLFunctionDecimalTest {

  private OSQLFunctionDecimal function;

  @Before
  public void setup() {
    function = new OSQLFunctionDecimal();
  }

  @Test
  public void testEmpty() {
    Object result = function.getResult();
    assertNull(result);
  }

  @Test
  public void testFromInteger() {
    function.execute(null, null, null, new Object[]{12}, null);
    Object result = function.getResult();
    assertEquals(result, BigDecimal.valueOf(12));
  }

  @Test
  public void testFromLong() {
    function.execute(null, null, null, new Object[]{1287623847384L}, null);
    Object result = function.getResult();
    assertEquals(result, new BigDecimal(1287623847384L));
  }

  @Test
  public void testFromString() {
    String initial = "12324124321234543256758654.76543212345676543254356765434567654";
    function.execute(null, null, null, new Object[]{initial}, null);
    Object result = function.getResult();
    assertEquals(result, new BigDecimal(initial));
  }

  public void testFromQuery() {
    try (YouTrackDB ctx = new YouTrackDB(DBTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig())) {
      ctx.execute("create database test memory users(admin identified by 'adminpwd' role admin)");
      try (var db = ctx.open("test", "admin", "adminpwd")) {
        String initial = "12324124321234543256758654.76543212345676543254356765434567654";
        try (YTResultSet result = db.query("select decimal('" + initial + "')")) {
          assertEquals(result.next().getProperty("decimal"), new BigDecimal(initial));
        }
      }
      ctx.drop("test");
    }
  }
}