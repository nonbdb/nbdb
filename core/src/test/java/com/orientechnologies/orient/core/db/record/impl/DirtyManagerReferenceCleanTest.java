package com.orientechnologies.orient.core.db.record.impl;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DirtyManagerReferenceCleanTest extends DBTestBase {

  public void beforeTest() throws Exception {
    super.beforeTest();

    db.getMetadata().getSchema().createClass("test");
  }

  @Test
  public void testReferDeletedDocument() {
    db.begin();
    YTDocument doc = new YTDocument();
    YTDocument doc1 = new YTDocument();
    doc1.field("aa", "aa");
    doc.field("ref", doc1);
    doc.field("bb");

    doc.save(db.getClusterNameById(db.getDefaultClusterId()));
    YTIdentifiable id = doc.getIdentity();
    db.commit();

    db.begin();
    doc = db.load(id.getIdentity());
    doc1 = doc.field("ref");
    doc1.delete();
    doc.field("ab", "ab");
    Assert.assertFalse(ORecordInternal.getDirtyManager(doc).getUpdateRecords().contains(doc1));
    db.commit();
  }
}
