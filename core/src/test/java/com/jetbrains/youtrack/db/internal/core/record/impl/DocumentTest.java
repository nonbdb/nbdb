package com.jetbrains.youtrack.db.internal.core.record.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.schema.Property;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.CreateDatabaseUtil;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionAbstract;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class DocumentTest extends DbTestBase {

  private static final String dbName = DocumentTest.class.getSimpleName();
  private static final String defaultDbAdminCredentials = "admin";

  @Test
  public void testCopyToCopiesEmptyFieldsTypesAndOwners() throws Exception {
    EntityImpl doc1 = (EntityImpl) db.newEntity();

    EntityImpl doc2 =
        ((EntityImpl) db.newEntity())
            .field("integer2", 123)
            .field("string", "YouTrackDB")
            .field("a", 123.3)
            .setFieldType("integer", PropertyType.INTEGER)
            .setFieldType("string", PropertyType.STRING)
            .setFieldType("binary", PropertyType.BINARY);
    var owner = (EntityImpl) db.newEntity();
    EntityInternalUtils.addOwner(doc2, owner);

    assertEquals(123, doc2.<Object>field("integer2"));
    assertEquals("YouTrackDB", doc2.field("string"));

    Assertions.assertThat(doc2.<Double>field("a")).isEqualTo(123.3d);
    assertEquals(PropertyType.INTEGER, doc2.fieldType("integer"));
    assertEquals(PropertyType.STRING, doc2.fieldType("string"));
    assertEquals(PropertyType.BINARY, doc2.fieldType("binary"));

    assertNotNull(doc2.getOwner());

    RecordInternal.unsetDirty(doc2);
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
  public void testClearResetsFieldTypes() throws Exception {
    EntityImpl doc = (EntityImpl) db.newEntity();
    doc.setFieldType("integer", PropertyType.INTEGER);
    doc.setFieldType("string", PropertyType.STRING);
    doc.setFieldType("binary", PropertyType.BINARY);

    assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
    assertEquals(PropertyType.STRING, doc.fieldType("string"));
    assertEquals(PropertyType.BINARY, doc.fieldType("binary"));

    doc.clear();

    assertNull(doc.fieldType("integer"));
    assertNull(doc.fieldType("string"));
    assertNull(doc.fieldType("binary"));
  }

  @Test
  public void testResetResetsFieldTypes() throws Exception {
    EntityImpl doc = (EntityImpl) db.newEntity();
    doc.setFieldType("integer", PropertyType.INTEGER);
    doc.setFieldType("string", PropertyType.STRING);
    doc.setFieldType("binary", PropertyType.BINARY);

    assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
    assertEquals(PropertyType.STRING, doc.fieldType("string"));
    assertEquals(PropertyType.BINARY, doc.fieldType("binary"));

    doc = (EntityImpl) db.newEntity();

    assertNull(doc.fieldType("integer"));
    assertNull(doc.fieldType("string"));
    assertNull(doc.fieldType("binary"));
  }

  @Test
  public void testKeepFieldType() throws Exception {
    EntityImpl doc = (EntityImpl) db.newEntity();
    doc.field("integer", 10, PropertyType.INTEGER);
    doc.field("string", 20, PropertyType.STRING);
    doc.field("binary", new byte[]{30}, PropertyType.BINARY);

    assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
    assertEquals(PropertyType.STRING, doc.fieldType("string"));
    assertEquals(PropertyType.BINARY, doc.fieldType("binary"));
  }

  @Test
  public void testKeepFieldTypeSerialization() throws Exception {
    EntityImpl doc = (EntityImpl) db.newEntity();
    doc.field("integer", 10, PropertyType.INTEGER);
    doc.field("link", new RecordId(1, 2), PropertyType.LINK);
    doc.field("string", 20, PropertyType.STRING);
    doc.field("binary", new byte[]{30}, PropertyType.BINARY);

    assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
    assertEquals(PropertyType.LINK, doc.fieldType("link"));
    assertEquals(PropertyType.STRING, doc.fieldType("string"));
    assertEquals(PropertyType.BINARY, doc.fieldType("binary"));
    RecordSerializer ser = DatabaseSessionAbstract.getDefaultSerializer();
    byte[] bytes = ser.toStream(db, doc);
    doc = (EntityImpl) db.newEntity();
    ser.fromStream(db, bytes, doc, null);
    assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
    assertEquals(PropertyType.STRING, doc.fieldType("string"));
    assertEquals(PropertyType.BINARY, doc.fieldType("binary"));
    assertEquals(PropertyType.LINK, doc.fieldType("link"));
  }

  @Test
  public void testKeepAutoFieldTypeSerialization() throws Exception {
    EntityImpl doc = (EntityImpl) db.newEntity();
    doc.field("integer", 10);
    doc.field("link", new RecordId(1, 2));
    doc.field("string", "string");
    doc.field("binary", new byte[]{30});

    // this is null because is not set on value set.
    assertNull(doc.fieldType("integer"));
    assertNull(doc.fieldType("link"));
    assertNull(doc.fieldType("string"));
    assertNull(doc.fieldType("binary"));
    RecordSerializer ser = DatabaseSessionAbstract.getDefaultSerializer();
    byte[] bytes = ser.toStream(db, doc);
    doc = (EntityImpl) db.newEntity();
    ser.fromStream(db, bytes, doc, null);
    assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
    assertEquals(PropertyType.STRING, doc.fieldType("string"));
    assertEquals(PropertyType.BINARY, doc.fieldType("binary"));
    assertEquals(PropertyType.LINK, doc.fieldType("link"));
  }

  @Test
  public void testKeepSchemafullFieldTypeSerialization() throws Exception {
    DatabaseSessionInternal db = null;
    YouTrackDB odb = null;
    try {
      odb = CreateDatabaseUtil.createDatabase(dbName, "memory:", CreateDatabaseUtil.TYPE_MEMORY);
      db = (DatabaseSessionInternal) odb.open(dbName, defaultDbAdminCredentials,
          CreateDatabaseUtil.NEW_ADMIN_PASSWORD);

      SchemaClass clazz = db.getMetadata().getSchema().createClass("Test");
      clazz.createProperty(db, "integer", PropertyType.INTEGER);
      clazz.createProperty(db, "link", PropertyType.LINK);
      clazz.createProperty(db, "string", PropertyType.STRING);
      clazz.createProperty(db, "binary", PropertyType.BINARY);
      EntityImpl doc = (EntityImpl) db.newEntity(clazz);
      doc.field("integer", 10);
      doc.field("link", new RecordId(1, 2));
      doc.field("string", "string");
      doc.field("binary", new byte[]{30});

      // the types are from the schema.
      assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
      assertEquals(PropertyType.LINK, doc.fieldType("link"));
      assertEquals(PropertyType.STRING, doc.fieldType("string"));
      assertEquals(PropertyType.BINARY, doc.fieldType("binary"));
      RecordSerializer ser = DatabaseSessionAbstract.getDefaultSerializer();
      byte[] bytes = ser.toStream(db, doc);
      doc = (EntityImpl) db.newEntity();
      ser.fromStream(db, bytes, doc, null);
      assertEquals(PropertyType.INTEGER, doc.fieldType("integer"));
      assertEquals(PropertyType.STRING, doc.fieldType("string"));
      assertEquals(PropertyType.BINARY, doc.fieldType("binary"));
      assertEquals(PropertyType.LINK, doc.fieldType("link"));
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
    EntityImpl doc = (EntityImpl) db.newEntity();
    doc.field("link", new RecordId(1, 2));
    RecordSerializer ser = DatabaseSessionAbstract.getDefaultSerializer();
    byte[] bytes = ser.toStream(db, doc);
    doc = (EntityImpl) db.newEntity();
    ser.fromStream(db, bytes, doc, null);
    assertEquals(PropertyType.LINK, doc.fieldType("link"));
    doc.field("link", new RidBag(db));
    assertNotEquals(PropertyType.LINK, doc.fieldType("link"));
  }

  @Test
  public void testRemovingReadonlyField() {
    DatabaseSessionInternal db = null;
    YouTrackDB odb = null;
    try {
      odb = CreateDatabaseUtil.createDatabase(dbName, "memory:", CreateDatabaseUtil.TYPE_MEMORY);
      db = (DatabaseSessionInternal) odb.open(dbName, defaultDbAdminCredentials,
          CreateDatabaseUtil.NEW_ADMIN_PASSWORD);

      Schema schema = db.getMetadata().getSchema();
      SchemaClass classA = schema.createClass("TestRemovingField2");
      classA.createProperty(db, "name", PropertyType.STRING);
      Property property = classA.createProperty(db, "property", PropertyType.STRING);
      property.setReadonly(db, true);
      db.begin();
      EntityImpl doc = (EntityImpl) db.newEntity(classA);
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
    DatabaseSessionInternal db = null;
    YouTrackDB odb = null;
    try {
      odb = CreateDatabaseUtil.createDatabase(dbName, "memory:", CreateDatabaseUtil.TYPE_MEMORY);
      db = (DatabaseSessionInternal) odb.open(dbName, defaultDbAdminCredentials,
          CreateDatabaseUtil.NEW_ADMIN_PASSWORD);

      Schema schema = db.getMetadata().getSchema();
      SchemaClass classA = schema.createClass("TestUndo");
      classA.createProperty(db, "name", PropertyType.STRING);
      classA.createProperty(db, "property", PropertyType.STRING);

      db.begin();
      EntityImpl doc = (EntityImpl) db.newEntity(classA);
      doc.field("name", "My Name");
      doc.field("property", "value1");
      doc.save();
      db.commit();

      db.begin();
      doc = db.bindToSession(doc);
      assertEquals("My Name", doc.field("name"));
      assertEquals("value1", doc.field("property"));
      doc.undo();
      assertEquals("My Name", doc.field("name"));
      assertEquals("value1", doc.field("property"));
      doc.field("name", "My Name 2");
      doc.field("property", "value2");
      doc.undo();
      doc.field("name", "My Name 3");
      assertEquals("My Name 3", doc.field("name"));
      assertEquals("value1", doc.field("property"));
      doc.save();
      db.commit();

      db.begin();
      doc = db.bindToSession(doc);
      doc.field("name", "My Name 4");
      doc.field("property", "value4");
      doc.undo("property");
      assertEquals("My Name 4", doc.field("name"));
      assertEquals("value1", doc.field("property"));
      doc.save();
      db.commit();

      doc = db.bindToSession(doc);
      doc.undo("property");
      assertEquals("My Name 4", doc.field("name"));
      assertEquals("value1", doc.field("property"));
      doc.undo();
      assertEquals("My Name 4", doc.field("name"));
      assertEquals("value1", doc.field("property"));
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
    EntityImpl dest = (EntityImpl) db.newEntity();

    EntityImpl source = (EntityImpl) db.newEntity();
    source.field("key", "value");
    source.field("somenull", (Object) null);

    dest.merge(source, true, false);

    assertEquals("value", dest.field("key"));

    assertTrue(dest.containsField("somenull"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailNestedSetNull() {
    EntityImpl doc = (EntityImpl) db.newEntity();
    doc.field("test.nested", "value");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailNullMapKey() {
    EntityImpl doc = (EntityImpl) db.newEntity();
    Map<String, String> map = new HashMap<String, String>();
    map.put(null, "dd");
    doc.field("testMap", map);
    doc.convertAllMultiValuesToTrackedVersions();
  }

  @Test
  public void testGetSetProperty() {
    EntityImpl doc = (EntityImpl) db.newEntity();
    Map<String, String> map = new HashMap<String, String>();
    map.put("foo", "valueInTheMap");
    doc.field("theMap", map);
    doc.setProperty("theMap.foo", "bar");

    assertEquals(doc.getProperty("theMap"), map);
    assertEquals("bar", doc.getProperty("theMap.foo"));
    assertEquals("valueInTheMap", doc.eval("theMap.foo"));

    //    doc.setProperty("", "foo");
    //    assertEquals(doc.getProperty(""), "foo");

    doc.setProperty(",", "comma");
    assertEquals("comma", doc.getProperty(","));

    doc.setProperty(",.,/;:'\"", "strange");
    assertEquals("strange", doc.getProperty(",.,/;:'\""));

    doc.setProperty("   ", "spaces");
    assertEquals("spaces", doc.getProperty("   "));
  }

  @Test
  public void testNoDirtySameBytes() {
    EntityImpl doc = (EntityImpl) db.newEntity();
    byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5};
    doc.field("bytes", bytes);
    EntityInternalUtils.clearTrackData(doc);
    RecordInternal.unsetDirty(doc);
    assertFalse(doc.isDirty());
    assertNull(doc.getOriginalValue("bytes"));
    doc.field("bytes", bytes.clone());
    assertFalse(doc.isDirty());
    assertNull(doc.getOriginalValue("bytes"));
  }
}