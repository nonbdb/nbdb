package com.orientechnologies.orient.core.sql.executor;

import static org.junit.Assert.assertEquals;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.YTEntity;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ORebuildIndexStatementExecutionTest extends DBTestBase {

  @Test
  public void indexAfterRebuildShouldIncludeAllClusters() {
    // given
    YTSchema schema = db.getMetadata().getSchema();
    String className = "IndexClusterTest";

    YTClass oclass = schema.createClass(className);
    oclass.createProperty(db, "key", YTType.STRING);
    oclass.createProperty(db, "value", YTType.INTEGER);
    oclass.createIndex(db, className + "index1", YTClass.INDEX_TYPE.NOTUNIQUE, "key");

    db.begin();
    YTEntity ele = db.newInstance(className);
    ele.setProperty("key", "a");
    ele.setProperty("value", 1);
    db.save(ele);
    db.commit();

    int clId = db.addCluster(className + "secondCluster");
    oclass.addClusterId(db, clId);

    db.begin();
    YTEntity ele1 = db.newInstance(className);
    ele1.setProperty("key", "a");
    ele1.setProperty("value", 2);
    db.save(ele1, className + "secondCluster");
    db.commit();

    // when
    YTResultSet result = db.command("rebuild index " + className + "index1");
    Assert.assertTrue(result.hasNext());
    YTResult resultRecord = result.next();
    Assert.assertEquals(2L, resultRecord.<Object>getProperty("totalIndexed"));
    Assert.assertFalse(result.hasNext());
    assertEquals(
        db.query("select from " + className + " where key = 'a'").stream().toList().size(), 2);
  }
}
