package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OAlterClassStatementExecutionTest extends DBTestBase {

  @Test
  public void testName1() {
    String className = "testName1";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result = db.command("alter class " + className + " name " + className + "_new");
    Assert.assertNull(schema.getClass(className));
    Assert.assertNotNull(schema.getClass(className + "_new"));
    result.close();
  }

  @Test
  public void testName2() {
    String className = "testName2";
    OSchema schema = db.getMetadata().getSchema();
    OClass e = schema.getClass("E");
    if (e == null) {
      schema.createClass("E");
    }
    schema.createClass(className, e);
    try {
      db.command("alter class " + className + " name " + className + "_new");
      Assert.fail();
    } catch (OCommandExecutionException ex) {

    } catch (Exception ex) {
      Assert.fail();
    }
    Assert.assertNotNull(schema.getClass(className));
    Assert.assertNull(schema.getClass(className + "_new"));
  }

  @Test
  public void testShortName() {
    String className = "testShortName";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result = db.command("alter class " + className + " shortname " + className + "_new");
    OClass clazz = schema.getClass(className);
    Assert.assertEquals(className + "_new", clazz.getShortName());
    result.close();
  }

  @Test
  public void testAddCluster() {
    String className = "testAddCluster";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result =
        db.command("alter class " + className + " addcluster " + className + "_new");
    OClass clazz = schema.getClass(className);
    boolean found = false;
    for (int i : clazz.getClusterIds()) {
      String clusterName = db.getClusterNameById(i);
      if (clusterName.equalsIgnoreCase(className + "_new")) {
        found = true;
      }
    }
    result.close();
    Assert.assertTrue(found);
  }

  @Test
  public void testRemoveCluster() {
    String className = "testRemoveCluster";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result =
        db.command("alter class " + className + " addcluster " + className + "_new");
    OClass clazz = schema.getClass(className);
    boolean found = false;
    for (int i : clazz.getClusterIds()) {
      String clusterName = db.getClusterNameById(i);
      if (clusterName.equalsIgnoreCase(className + "_new")) {
        found = true;
      }
    }
    result.close();
    Assert.assertTrue(found);

    result = db.command("alter class " + className + " removecluster " + className + "_new");
    clazz = schema.getClass(className);
    found = false;
    for (int i : clazz.getClusterIds()) {
      String clusterName = db.getClusterNameById(i);
      if (clusterName.equalsIgnoreCase(className + "_new")) {
        found = true;
      }
    }
    result.close();
    Assert.assertFalse(found);
  }

  @Test
  public void testSuperclass() {
    String className = "testSuperclass_sub";
    String superclassName = "testSuperclass_super";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OClass superclass = schema.createClass(superclassName);
    OResultSet result = db.command("alter class " + className + " superclass " + superclassName);
    Assert.assertTrue(schema.getClass(className).getSuperClasses().contains(superclass));
    result.close();
  }

  @Test
  public void testSuperclasses() {
    String className = "testSuperclasses_sub";
    String superclassName = "testSuperclasses_super1";
    String superclassName2 = "testSuperclasses_super2";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OClass superclass = schema.createClass(superclassName);
    OClass superclass2 = schema.createClass(superclassName2);
    OResultSet result =
        db.command(
            "alter class "
                + className
                + " superclasses "
                + superclassName
                + ", "
                + superclassName2);
    Assert.assertTrue(schema.getClass(className).getSuperClasses().contains(superclass));
    Assert.assertTrue(schema.getClass(className).getSuperClasses().contains(superclass2));
    result.close();
  }

  @Test
  public void testOversize() {
    String className = "testOversize";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result = db.command("alter class " + className + " oversize 10");
    OClass clazz = schema.getClass(className);
    Assert.assertEquals((Object) 10.0f, clazz.getOverSize());
    result.close();
  }

  @Test
  public void testStrictmode() {
    String className = "testStrictmode";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result = db.command("alter class " + className + " strictmode true");
    OClass clazz = schema.getClass(className);
    Assert.assertTrue(clazz.isStrictMode());
    result.close();
  }

  @Test
  public void testCustom() {
    String className = "testCustom";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result = db.command("alter class " + className + " custom foo = 'bar'");
    OClass clazz = schema.getClass(className);
    Assert.assertEquals("bar", clazz.getCustom("foo"));
    result.close();
  }

  @Test
  public void testCustom2() {
    String className = "testCustom2";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result = db.command("alter class " + className + " custom foo = ?", "bar");
    OClass clazz = schema.getClass(className);
    Assert.assertEquals("bar", clazz.getCustom("foo"));
    result.close();
  }

  @Test
  public void testAbstract() {
    String className = "testAbstract";
    OSchema schema = db.getMetadata().getSchema();
    schema.createClass(className);
    OResultSet result = db.command("alter class " + className + " abstract true");
    OClass clazz = schema.getClass(className);
    Assert.assertTrue(clazz.isAbstract());
    result.close();
  }

  @Test
  public void testUnsafe1() {
    String className = "testUnsafe1";
    OSchema schema = db.getMetadata().getSchema();
    OClass e = schema.getClass("E");
    if (e == null) {
      e = schema.createClass("E");
    }
    schema.createClass(className, e);
    try {
      db.command("alter class " + className + " name " + className + "_new");
      Assert.fail();
    } catch (OCommandExecutionException ex) {

    }
    OResultSet result =
        db.command("alter class " + className + " name " + className + "_new unsafe");
    Assert.assertNull(schema.getClass(className));
    Assert.assertNotNull(schema.getClass(className + "_new"));
    result.close();
  }

  @Test
  public void testDefaultCluster() {
    String className = "testDefaultCluster";
    OSchema schema = db.getMetadata().getSchema();
    OClass clazz = schema.createClass(className);
    int[] clusterIds = clazz.getClusterIds();
    if (clusterIds.length < 2) {
      clazz.addCluster(db, className + "_1");
      clusterIds = clazz.getClusterIds();
    }
    int currentDefault = clazz.getDefaultClusterId();
    int firstNonDefault = -1;
    for (int clusterId : clusterIds) {
      if (clusterId != currentDefault) {
        firstNonDefault = clusterId;
      }
    }

    try {
      db.command("alter class " + className + " defaultcluster " + firstNonDefault).close();
    } catch (OCommandExecutionException ex) {
    }

    Assert.assertEquals(firstNonDefault, schema.getClass(className).getDefaultClusterId());
  }
}
