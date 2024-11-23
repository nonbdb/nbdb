package com.orientechnologies.orient.core.db.document;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ODatabaseDocumentSaveClusterTest extends BaseMemoryDatabase {

  @Test(expected = IllegalArgumentException.class)
  public void testSaveWrongCluster() {
    db.getMetadata().getSchema().createClass("test");
    db.addCluster("test_one");

    db.save(new ODocument("test"), "test_one");
  }

  @Test(expected = OSchemaException.class)
  public void testUsedClusterTest() {
    OClass clazz = db.getMetadata().getSchema().createClass("test");
    db.addCluster("test_one");
    clazz.addCluster(db, "test_one");
    OClass clazz2 = db.getMetadata().getSchema().createClass("test2");
    clazz2.addCluster(db, "test_one");
  }

  @Test
  public void testSaveCluster() {
    OClass clazz = db.getMetadata().getSchema().createClass("test");
    int res = db.addCluster("test_one");
    clazz.addCluster(db, "test_one");

    ORecord saved = db.computeInTx(() -> db.save(new ODocument("test"), "test_one"));
    Assert.assertEquals(saved.getIdentity().getClusterId(), res);
  }

  @Test
  public void testDeleteClassAndClusters() {
    OClass clazz = db.getMetadata().getSchema().createClass("test");
    int res = db.addCluster("test_one");
    clazz.addCluster(db, "test_one");

    ORecord saved = db.computeInTx(() -> db.save(new ODocument("test"), "test_one"));
    Assert.assertEquals(saved.getIdentity().getClusterId(), res);
    db.getMetadata().getSchema().dropClass(clazz.getName());
    Assert.assertFalse(db.existsCluster("test_one"));
  }
}
