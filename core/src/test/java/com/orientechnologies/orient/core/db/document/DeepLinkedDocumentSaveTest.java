package com.orientechnologies.orient.core.db.document;

import static org.junit.Assert.assertEquals;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class DeepLinkedDocumentSaveTest extends DBTestBase {

  @Test
  public void testLinkedTx() {
    final Set<ODocument> docs = new HashSet<>();

    db.getMetadata().getSchema().createClass("Test");

    db.begin();
    ODocument doc = new ODocument("Test");
    docs.add(doc);
    for (int i = 0; i < 3000; i++) {
      docs.add(doc = new ODocument("Test").field("linked", doc));
    }
    db.save(doc);
    db.commit();

    assertEquals(3001, db.countClass("Test"));

    for (ODocument d : docs) {
      assertEquals(1, d.getVersion());
    }
  }
}
