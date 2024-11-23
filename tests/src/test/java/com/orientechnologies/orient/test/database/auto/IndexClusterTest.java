package com.orientechnologies.orient.test.database.auto;

import static org.testng.Assert.assertEquals;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = {"index"})
public class IndexClusterTest extends DocumentDBBaseTest {

  @Parameters(value = "remote")
  public IndexClusterTest(boolean remote) {
    super(remote);
  }

  @Test
  public void indexAfterRebuildShouldIncludeAllClusters() {
    // given
    OSchema schema = database.getMetadata().getSchema();
    String className = "IndexClusterTest";

    OClass oclass = schema.createClass(className);
    oclass.createProperty(database, "key", OType.STRING);
    oclass.createProperty(database, "value", OType.INTEGER);
    oclass.createIndex(database, className + "index1", OClass.INDEX_TYPE.NOTUNIQUE, "key");

    database.begin();
    database.<ODocument>newInstance(className).field("key", "a").field("value", 1).save();
    database.commit();

    int clId = database.addCluster(className + "secondCluster");
    oclass.addClusterId(database, clId);

    database.begin();
    database
        .<ODocument>newInstance(className)
        .field("key", "a")
        .field("value", 2)
        .save(className + "secondCluster");
    database.commit();

    // when
    database.command("rebuild index " + className + "index1").close();
    assertEquals(
        database.query("select from " + className + " where key = 'a'").stream().count(), 2);
  }
}
