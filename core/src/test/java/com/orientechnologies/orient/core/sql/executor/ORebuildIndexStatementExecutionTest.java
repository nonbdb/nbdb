package com.orientechnologies.orient.core.sql.executor;

import static org.junit.Assert.assertEquals;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OElement;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ORebuildIndexStatementExecutionTest extends DBTestBase {

  @Test
  public void indexAfterRebuildShouldIncludeAllClusters() {
    // given
    OSchema schema = db.getMetadata().getSchema();
    String className = "IndexClusterTest";

    OClass oclass = schema.createClass(className);
    oclass.createProperty(db, "key", OType.STRING);
    oclass.createProperty(db, "value", OType.INTEGER);
    oclass.createIndex(db, className + "index1", OClass.INDEX_TYPE.NOTUNIQUE, "key");

    db.begin();
    OElement ele = db.newInstance(className);
    ele.setProperty("key", "a");
    ele.setProperty("value", 1);
    db.save(ele);
    db.commit();

    int clId = db.addCluster(className + "secondCluster");
    oclass.addClusterId(db, clId);

    db.begin();
    OElement ele1 = db.newInstance(className);
    ele1.setProperty("key", "a");
    ele1.setProperty("value", 2);
    db.save(ele1, className + "secondCluster");
    db.commit();

    // when
    OResultSet result = db.command("rebuild index " + className + "index1");
    Assert.assertTrue(result.hasNext());
    OResult resultRecord = result.next();
    Assert.assertEquals(2L, resultRecord.<Object>getProperty("totalIndexed"));
    Assert.assertFalse(result.hasNext());
    assertEquals(
        db.query("select from " + className + " where key = 'a'").stream().toList().size(), 2);
  }
}
