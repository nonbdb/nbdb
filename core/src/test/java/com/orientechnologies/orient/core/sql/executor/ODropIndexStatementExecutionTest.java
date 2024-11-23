package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.BaseMemoryInternalDatabase;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ODropIndexStatementExecutionTest extends BaseMemoryInternalDatabase {

  @Test
  public void testPlain() {
    OIndex index =
        db.getMetadata()
            .getSchema()
            .createClass("testPlain")
            .createProperty(db, "bar", OType.STRING)
            .createIndex(db, OClass.INDEX_TYPE.NOTUNIQUE);
    String indexName = index.getName();

    db.getMetadata().getIndexManagerInternal().reload(db);
    Assert.assertNotNull((db.getMetadata().getIndexManagerInternal()).getIndex(db, indexName));

    OResultSet result = db.command("drop index " + indexName);
    Assert.assertTrue(result.hasNext());
    OResult next = result.next();
    Assert.assertEquals("drop index", next.getProperty("operation"));
    Assert.assertFalse(result.hasNext());
    result.close();

    db.getMetadata().getIndexManagerInternal().reload(db);
    Assert.assertNull(db.getMetadata().getIndexManagerInternal().getIndex(db, indexName));
  }

  @Test
  public void testAll() {
    OIndex index =
        db.getMetadata()
            .getSchema()
            .createClass("testAll")
            .createProperty(db, "baz", OType.STRING)
            .createIndex(db, OClass.INDEX_TYPE.NOTUNIQUE);
    String indexName = index.getName();

    db.getMetadata().getIndexManagerInternal().reload(db);
    Assert.assertNotNull(db.getMetadata().getIndexManagerInternal().getIndex(db, indexName));

    OResultSet result = db.command("drop index *");
    Assert.assertTrue(result.hasNext());
    OResult next = result.next();
    Assert.assertEquals("drop index", next.getProperty("operation"));
    result.close();
    db.getMetadata().getIndexManagerInternal().reload(db);
    Assert.assertNull(db.getMetadata().getIndexManagerInternal().getIndex(db, indexName));
    Assert.assertTrue(db.getMetadata().getIndexManagerInternal().getIndexes(db).isEmpty());
  }

  @Test
  public void testWrongName() {

    String indexName = "nonexistingindex";
    db.getMetadata().getIndexManagerInternal().reload(db);
    Assert.assertNull(db.getMetadata().getIndexManagerInternal().getIndex(db, indexName));

    try {
      OResultSet result = db.command("drop index " + indexName);
      Assert.fail();
    } catch (OCommandExecutionException ex) {
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testIfExists() {

    String indexName = "nonexistingindex";
    db.getMetadata().getIndexManagerInternal().reload(db);
    Assert.assertNull(db.getMetadata().getIndexManagerInternal().getIndex(db, indexName));

    try {
      OResultSet result = db.command("drop index " + indexName + " if exists");
    } catch (Exception e) {
      Assert.fail();
    }
  }
}
