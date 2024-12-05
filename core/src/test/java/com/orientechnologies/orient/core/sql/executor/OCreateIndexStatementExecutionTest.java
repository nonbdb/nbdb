package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.BaseMemoryInternalDatabase;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OCreateIndexStatementExecutionTest extends BaseMemoryInternalDatabase {

  @Test
  public void testPlain() {
    String className = "testPlain";
    YTClass clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", YTType.STRING);

    Assert.assertNull(db.getMetadata().getIndexManagerInternal().getIndex(db, className + ".name"));
    YTResultSet result =
        db.command("create index " + className + ".name on " + className + " (name) notunique");
    Assert.assertTrue(result.hasNext());
    YTResult next = result.next();
    Assert.assertFalse(result.hasNext());
    Assert.assertNotNull(next);
    result.close();
    OIndex idx = db.getMetadata().getIndexManagerInternal().getIndex(db, className + ".name");
    Assert.assertNotNull(idx);
    Assert.assertFalse(idx.isUnique());
  }

  @Test
  public void testIfNotExists() {
    String className = "testIfNotExists";
    YTClass clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", YTType.STRING);

    Assert.assertNull(db.getMetadata().getIndexManagerInternal().getIndex(db, className + ".name"));
    YTResultSet result =
        db.command(
            "create index "
                + className
                + ".name IF NOT EXISTS on "
                + className
                + " (name) notunique");
    Assert.assertTrue(result.hasNext());
    YTResult next = result.next();
    Assert.assertFalse(result.hasNext());
    Assert.assertNotNull(next);
    result.close();
    OIndex idx = db.getMetadata().getIndexManagerInternal().getIndex(db, className + ".name");
    Assert.assertNotNull(idx);
    Assert.assertFalse(idx.isUnique());

    result =
        db.command(
            "create index "
                + className
                + ".name IF NOT EXISTS on "
                + className
                + " (name) notunique");
    Assert.assertFalse(result.hasNext());
    result.close();
  }
}
