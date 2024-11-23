package com.orientechnologies.orient.core.index;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.text.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class UniqueHashIndexForDate extends BaseMemoryDatabase {

  @Test
  public void testSimpleUniqueDateIndex() throws ParseException {
    OClass clazz = db.getMetadata().getSchema().createClass("test_edge");
    OProperty prop = clazz.createProperty(db, "date", OType.DATETIME);
    prop.createIndex(db, INDEX_TYPE.UNIQUE);
    ODocument doc = new ODocument("test_edge");
    doc.field("date", "2015-03-24 08:54:49");

    ODocument doc1 = new ODocument("test_edge");
    doc1.field("date", "2015-03-24 08:54:49");

    db.save(doc);
    try {
      db.begin();
      db.save(doc1);
      doc1.field("date", "2015-03-24 08:54:49");
      db.save(doc1);
      db.commit();
      Assert.fail("expected exception for duplicate ");
    } catch (OException e) {

    }
  }
}
