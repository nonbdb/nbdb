package com.jetbrains.youtrack.db.internal.core.record.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.internal.DBTestBase;
import com.jetbrains.youtrack.db.internal.core.OCreateDatabaseUtil;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.document.YTDatabaseSessionAbstract;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTProperty;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTSchema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.record.ORecordInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.ORecordSerializer;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class ODocumentTest extends DBTestBase {

  private static final String dbName = ODocumentTest.class.getSimpleName();
  private static final String defaultDbAdminCredentials = "admin";

  @Test
  public void testCopyToCopiesEmptyFieldsTypesAndOwners() throws Exception {
    EntityImpl doc1 = new EntityImpl();

    EntityImpl doc2 =
        new EntityImpl()
            .field("integer2", 123)
            .field("string", "YouTrackDB")
            .field("a", 123.3)
            .setFieldType("integer", YTType.INTEGER)
            .setFieldType("string", YTType.STRING)
            .setFieldType("binary", YTType.BINARY);
    var owner = new EntityImpl();
    ODocumentInternal.addOwner(doc2, owner);

    assertEquals(doc2.<Object>field("integer2"), 123);
    assertEquals(doc2.field("string"), "YouTrackDB");

    Assertions.assertThat(doc2.<Double>field("a")).isEqualTo(123.3d);
    assertEquals(doc2.fieldType("integer"), YTType.INTEGER);
    assertEquals(doc2.fieldType("string"), YTType.STRING);
    assertEquals(doc2.fieldType("binary"), YTType.BINARY);

    assertNotNull(doc2.getOwner());

    ORecordInternal.unsetDirty(doc2);
    doc1.copyTo(doc2);

    assertNull(doc2.field("integer2"));
    assertNull(doc2.field("string"));
    assertNull(doc2.field("a"));

    assertNull(doc2.fieldType("integer"));
    assertNull(doc2.fieldType("string"));
    assertNull(doc2.fieldType("binary"));

    assertNull(doc2.getOwner());
  }

  @Test
  public void testNullConstructor() {
    new EntityImpl((String) null);
    new EntityImpl((YTClass) null);
  }

  @Test
  public void testClearResetsFieldTypes() throws Exception {
    EntityImpl doc = new EntityImpl();
    doc.setFieldType("integer", YTType.INTEGER);
    doc.setFieldType("string", YTType.STRING);
    doc.setFieldType("binary", YTType.BINARY);

    assertEquals(doc.fieldType("integer"), YTType.INTEGER);
    assertEquals(doc.fieldType("string"), YTType.STRING);
    assertEquals(doc.fieldType("binary"), YTType.BINARY);

    doc.clear();

    assertNull(doc.fieldType("integer"));
    assertNull(doc.fieldType("string"));
    assertNull(doc.fieldType("binary"));
  }

  @Test
  public void testResetResetsFieldTypes() throws Exception {
    EntityImpl doc = new EntityImpl();
    doc.setFieldType("integer", YTType.INTEGER);
    doc.setFieldType("string", YTType.STRING);
    doc.setFieldType("binary", YTType.BINARY);

    assertEquals(doc.fieldType("integer"), YTType.INTEGER);
    assertEquals(doc.fieldType("string"), YTType.STRING);
    assertEquals(doc.fieldType("binary"), YTType.BINARY);

    doc = new EntityImpl();

    assertNull(doc.fieldType("integer"));
    assertNull(doc.fieldType("string"));
    assertNull(doc.fieldType("binary"));
  }

  @Test
  public void testKeepFieldType() throws Exception {
    EntityImpl doc = new EntityImpl();
    doc.field("integer", 10, YTType.INTEGER);
    doc.field("string", 20, YTType.STRING);
    doc.field("binary", new byte[]{30}, YTType.BINARY);

    assertEquals(doc.fieldType("integer"), YTType.INTEGER);
    assertEquals(doc.fieldType("string"), YTType.STRING);
    assertEquals(doc.fieldType("binary"), YTType.BINARY);
  }

  @Test
  public void testKeepFieldTypeSerialization() throws Exception {
    EntityImpl doc = new EntityImpl();
    doc.field("integer", 10, YTType.INTEGER);
    doc.field("link", new YTRecordId(1, 2), YTType.LINK);
    doc.field("string", 20, YTType.STRING);
    doc.field("binary", new byte[]{30}, YTType.BINARY);

    assertEquals(doc.fieldType("integer"), YTType.INTEGER);
    assertEquals(doc.fieldType("link"), YTType.LINK);
    assertEquals(doc.fieldType("string"), YTType.STRING);
    assertEquals(doc.fieldType("binary"), YTType.BINARY);
    ORecordSerializer ser = YTDatabaseSessionAbstract.getDefaultSerializer();
    byte[] bytes = ser.toStream(db, doc);
    doc = new EntityImpl();
    ser.fromStream(db, bytes, doc, null);
    assertEquals(doc.fieldType("integer"), YTType.INTEGER);
    assertEquals(doc.fieldType("string"), YTType.STRING);
    assertEquals(doc.fieldType("binary"), YTType.BINARY);
    assertEquals(doc.fieldType("link"), YTType.LINK);
  }

  @Test
  public void testKeepAutoFieldTypeSerialization() throws Exception {
    EntityImpl doc = new EntityImpl();
    doc.field("integer", 10);
    doc.field("link", new YTRecordId(1, 2));
    doc.field("string", "string");
    doc.field("binary", new byte[]{30});

    // this is null because is not set on value set.
    assertNull(doc.fieldType("integer"));
    assertNull(doc.fieldType("link"));
    assertNull(doc.fieldType("string"));
    assertNull(doc.fieldType("binary"));
    ORecordSerializer ser = YTDatabaseSessionAbstract.getDefaultSerializer();
    byte[] bytes = ser.toStream(db, doc);
    doc = new EntityImpl();
    ser.fromStream(db, bytes, doc, null);
    assertEquals(doc.fieldType("integer"), YTType.INTEGER);
    assertEquals(doc.fieldType("string"), YTType.STRING);
    assertEquals(doc.fieldType("binary"), YTType.BINARY);
    assertEquals(doc.fieldType("link"), YTType.LINK);
  }

  @Test
  public void testKeepSchemafullFieldTypeSerialization() throws Exception {
    YTDatabaseSessionInternal db = null;
    YouTrackDB odb = null;
    try {
      odb = OCreateDatabaseUtil.createDatabase(dbName, "memory:", OCreateDatabaseUtil.TYPE_MEMORY);
      db = (YTDatabaseSessionInternal) odb.open(dbName, defaultDbAdminCredentials,
          OCreateDatabaseUtil.NEW_ADMIN_PASSWORD);

      YTClass clazz = db.getMetadata().getSchema().createClass("Test");
      clazz.createProperty(db, "integer", YTType.INTEGER);
      clazz.createProperty(db, "link", YTType.LINK);
      clazz.createProperty(db, "string", YTType.STRING);
      clazz.createProperty(db, "binary", YTType.BINARY);
      EntityImpl doc = new EntityImpl(clazz);
      doc.field("integer", 10);
      doc.field("link", new YTRecordId(1, 2));
      doc.field("string", "string");
      doc.field("binary", new byte[]{30});

      // the types are from the schema.
      assertEquals(doc.fieldType("integer"), YTType.INTEGER);
      assertEquals(doc.fieldType("link"), YTType.LINK);
      assertEquals(doc.fieldType("string"), YTType.STRING);
      assertEquals(doc.fieldType("binary"), YTType.BINARY);
      ORecordSerializer ser = YTDatabaseSessionAbstract.getDefaultSerializer();
      byte[] bytes = ser.toStream(db, doc);
      doc = new EntityImpl();
      ser.fromStream(db, bytes, doc, null);
      assertEquals(doc.fieldType("integer"), YTType.INTEGER);
      assertEquals(doc.fieldType("string"), YTType.STRING);
      assertEquals(doc.fieldType("binary"), YTType.BINARY);
      assertEquals(doc.fieldType("link"), YTType.LINK);
    } finally {
      if (db != null) {
        db.close();
      }
      if (odb != null) {
        odb.drop(dbName);
        odb.close();
      }
    }
  }

  @Test
  public void testChangeTypeOnValueSet() throws Exception {
    EntityImpl doc = new EntityImpl();
    doc.field("link", new YTRecordId(1, 2));
    ORecordSerializer ser = YTDatabaseSessionAbstract.getDefaultSerializer();
    byte[] bytes = ser.toStream(db, doc);
    doc = new EntityImpl();
    ser.fromStream(db, bytes, doc, null);
    assertEquals(doc.fieldType("link"), YTType.LINK);
    doc.field("link", new RidBag(db));
    assertNotEquals(doc.fieldType("link"), YTType.LINK);
  }

  @Test
  public void testRemovingReadonlyField() {
    YTDatabaseSessionInternal db = null;
    YouTrackDB odb = null;
    try {
      odb = OCreateDatabaseUtil.createDatabase(dbName, "memory:", OCreateDatabaseUtil.TYPE_MEMORY);
      db = (YTDatabaseSessionInternal) odb.open(dbName, defaultDbAdminCredentials,
          OCreateDatabaseUtil.NEW_ADMIN_PASSWORD);

      YTSchema schema = db.getMetadata().getSchema();
      YTClass classA = schema.createClass("TestRemovingField2");
      classA.createProperty(db, "name", YTType.STRING);
      YTProperty property = classA.createProperty(db, "property", YTType.STRING);
      property.setReadonly(db, true);
      db.begin();
      EntityImpl doc = new EntityImpl(classA);
      doc.field("name", "My Name");
      doc.field("property", "value1");
      doc.save();

      doc.field("name", "My Name 2");
      doc.field("property", "value2");
      doc.undo(); // we decided undo everything
      doc.field("name", "My Name 3"); // change something
      doc.save();
      doc.field("name", "My Name 4");
      doc.field("property", "value4");
      doc.undo("property"); // we decided undo readonly field
      doc.save();
      db.commit();
    } finally {
      if (db != null) {
        db.close();
      }
      if (odb != null) {
        odb.drop(dbName);
        odb.close();
      }
    }
  }

  @Test
  public void testUndo() {
    YTDatabaseSessionInternal db = null;
    YouTrackDB odb = null;
    try {
      odb = OCreateDatabaseUtil.createDatabase(dbName, "memory:", OCreateDatabaseUtil.TYPE_MEMORY);
      db = (YTDatabaseSessionInternal) odb.open(dbName, defaultDbAdminCredentials,
          OCreateDatabaseUtil.NEW_ADMIN_PASSWORD);

      YTSchema schema = db.getMetadata().getSchema();
      YTClass classA = schema.createClass("TestUndo");
      classA.createProperty(db, "name", YTType.STRING);
      classA.createProperty(db, "property", YTType.STRING);

      db.begin();
      EntityImpl doc = new EntityImpl(classA);
      doc.field("name", "My Name");
      doc.field("property", "value1");
      doc.save();
      db.commit();

      db.begin();
      doc = db.bindToSession(doc);
      assertEquals(doc.field("name"), "My Name");
      assertEquals(doc.field("property"), "value1");
      doc.undo();
      assertEquals(doc.field("name"), "My Name");
      assertEquals(doc.field("property"), "value1");
      doc.field("name", "My Name 2");
      doc.field("property", "value2");
      doc.undo();
      doc.field("name", "My Name 3");
      assertEquals(doc.field("name"), "My Name 3");
      assertEquals(doc.field("property"), "value1");
      doc.save();
      db.commit();

      db.begin();
      doc = db.bindToSession(doc);
      doc.field("name", "My Name 4");
      doc.field("property", "value4");
      doc.undo("property");
      assertEquals(doc.field("name"), "My Name 4");
      assertEquals(doc.field("property"), "value1");
      doc.save();
      db.commit();

      doc = db.bindToSession(doc);
      doc.undo("property");
      assertEquals(doc.field("name"), "My Name 4");
      assertEquals(doc.field("property"), "value1");
      doc.undo();
      assertEquals(doc.field("name"), "My Name 4");
      assertEquals(doc.field("property"), "value1");
    } finally {
      if (db != null) {
        db.close();
      }
      if (odb != null) {
        odb.drop(dbName);
        odb.close();
      }
    }
  }

  @Test
  public void testMergeNull() {
    EntityImpl dest = new EntityImpl();

    EntityImpl source = new EntityImpl();
    source.field("key", "value");
    source.field("somenull", (Object) null);

    dest.merge(source, true, false);

    assertEquals(dest.field("key"), "value");

    assertTrue(dest.containsField("somenull"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailNestedSetNull() {
    EntityImpl doc = new EntityImpl();
    doc.field("test.nested", "value");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailNullMapKey() {
    EntityImpl doc = new EntityImpl();
    Map<String, String> map = new HashMap<String, String>();
    map.put(null, "dd");
    doc.field("testMap", map);
    doc.convertAllMultiValuesToTrackedVersions();
  }

  @Test
  public void testGetSetProperty() {
    EntityImpl doc = new EntityImpl();
    Map<String, String> map = new HashMap<String, String>();
    map.put("foo", "valueInTheMap");
    doc.field("theMap", map);
    doc.setProperty("theMap.foo", "bar");

    assertEquals(doc.getProperty("theMap"), map);
    assertEquals(doc.getProperty("theMap.foo"), "bar");
    assertEquals(doc.eval("theMap.foo"), "valueInTheMap");

    //    doc.setProperty("", "foo");
    //    assertEquals(doc.getProperty(""), "foo");

    doc.setProperty(",", "comma");
    assertEquals(doc.getProperty(","), "comma");

    doc.setProperty(",.,/;:'\"", "strange");
    assertEquals(doc.getProperty(",.,/;:'\""), "strange");

    doc.setProperty("   ", "spaces");
    assertEquals(doc.getProperty("   "), "spaces");
  }

  @Test
  public void testNoDirtySameBytes() {
    EntityImpl doc = new EntityImpl();
    byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5};
    doc.field("bytes", bytes);
    ODocumentInternal.clearTrackData(doc);
    ORecordInternal.unsetDirty(doc);
    assertFalse(doc.isDirty());
    assertNull(doc.getOriginalValue("bytes"));
    doc.field("bytes", bytes.clone());
    assertFalse(doc.isDirty());
    assertNull(doc.getOriginalValue("bytes"));
  }
}