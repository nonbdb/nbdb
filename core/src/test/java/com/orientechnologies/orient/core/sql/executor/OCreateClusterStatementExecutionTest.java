package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OCreateClusterStatementExecutionTest extends DBTestBase {

  @Test
  public void testPlain() {
    String clusterName = "testPlain";
    YTResultSet result = db.command("create cluster " + clusterName);
    Assert.assertTrue(db.getClusterIdByName(clusterName) > 0);
    result.close();
  }

  @Test
  public void testExisting() {
    YTClass clazz = db.getMetadata().getSchema().createClass("testExisting");
    String clusterName = db.getClusterNameById(clazz.getClusterIds()[0]);
    try {
      db.command("create cluster " + clusterName);
      Assert.fail();
    } catch (YTCommandExecutionException ex) {

    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testWithNumber() {
    String clusterName = "testWithNumber";
    YTResultSet result = db.command("create cluster " + clusterName + " id 1000");
    Assert.assertTrue(db.getClusterIdByName(clusterName) > 0);
    Assert.assertNotNull(db.getClusterNameById(1000));

    Assert.assertTrue(result.hasNext());
    YTResult next = result.next();
    Assert.assertFalse(result.hasNext());
    Assert.assertNotNull(next);
    Assert.assertEquals((Object) 1000, next.getProperty("requestedId"));
    result.close();
  }

  @Test
  public void testBlob() {
    String clusterName = "testBlob";
    YTResultSet result = db.command("create blob cluster " + clusterName);
    Assert.assertTrue(db.getClusterIdByName(clusterName) > 0);
    Assert.assertTrue(db.getStorage().getClusterIdByName(clusterName) >= 0);
    // TODO test that it's a blob cluster
    result.close();
  }

  @Test
  public void testIfNotExists() {
    String clusterName = "testIfNotExists";
    YTResultSet result = db.command("create cluster " + clusterName + " IF NOT EXISTS id 2000");
    Assert.assertTrue(db.getClusterIdByName(clusterName) > 0);
    Assert.assertNotNull(db.getClusterNameById(2000));

    Assert.assertTrue(result.hasNext());
    YTResult next = result.next();
    Assert.assertFalse(result.hasNext());
    Assert.assertNotNull(next);
    Assert.assertEquals((Object) 2000, next.getProperty("requestedId"));
    result.close();

    result = db.command("create cluster " + clusterName + " IF NOT EXISTS id 1000");
    Assert.assertFalse(result.hasNext());
    result.close();
  }
}
