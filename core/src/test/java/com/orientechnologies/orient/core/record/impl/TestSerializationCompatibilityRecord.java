package com.orientechnologies.orient.core.record.impl;

import static org.junit.Assert.assertEquals;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TestSerializationCompatibilityRecord extends DBTestBase {

  @Test
  public void testDataNotMatchSchema() {
    YTClass klass =
        db.getMetadata()
            .getSchema()
            .createClass("Test", db.getMetadata().getSchema().getClass("V"));
    db.begin();
    YTDocument doc = new YTDocument("Test");
    Map<String, YTRID> map = new HashMap<String, YTRID>();
    map.put("some", new YTRecordId(10, 20));
    doc.field("map", map, YTType.LINKMAP);
    YTRID id = db.save(doc).getIdentity();
    db.commit();
    klass.createProperty(db, "map", YTType.EMBEDDEDMAP, (YTType) null, true);

    db.begin();
    YTDocument record = db.load(id);
    // Force deserialize + serialize;
    record.setProperty("some", "aa");
    db.save(record);
    db.commit();

    YTDocument record1 = db.load(id);
    assertEquals(record1.fieldType("map"), YTType.LINKMAP);
  }
}
