package com.orientechnologies.orient.core.sql;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OCommandExecutorSQLCreateFunctionTest extends BaseMemoryDatabase {

  @Test
  public void testCreateFunction() {
    db.begin();
    db.command(
            "CREATE FUNCTION testCreateFunction \"return 'hello '+name;\" PARAMETERS [name]"
                + " IDEMPOTENT true LANGUAGE Javascript")
        .close();
    db.commit();

    OResultSet result = db.command("select testCreateFunction('world') as name");
    Assert.assertEquals(result.next().getProperty("name"), "hello world");
    Assert.assertFalse(result.hasNext());
  }
}
