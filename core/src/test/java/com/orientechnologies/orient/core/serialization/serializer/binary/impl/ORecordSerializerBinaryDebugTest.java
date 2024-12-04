package com.orientechnologies.orient.core.serialization.serializer.binary.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.db.document.ODatabaseSessionAbstract;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializationDebug;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerBinary;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerBinaryDebug;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ORecordSerializerBinaryDebugTest extends DBTestBase {

  private ORecordSerializer previous;

  @Before
  public void before() {
    previous = ODatabaseSessionAbstract.getDefaultSerializer();
    ODatabaseSessionAbstract.setDefaultSerializer(new ORecordSerializerBinary());
  }

  @After
  public void after() {
    ODatabaseSessionAbstract.setDefaultSerializer(previous);
  }

  @Test
  public void testSimpleDocumentDebug() {

    ODocument doc = new ODocument();
    doc.field("test", "test");
    doc.field("anInt", 2);
    doc.field("anDouble", 2D);

    byte[] bytes = doc.toStream();

    ORecordSerializerBinaryDebug debugger = new ORecordSerializerBinaryDebug();
    ORecordSerializationDebug debug = debugger.deserializeDebug(bytes, db);

    assertEquals(debug.properties.size(), 3);
    assertEquals(debug.properties.get(0).name, "test");
    assertEquals(debug.properties.get(0).type, OType.STRING);
    assertEquals(debug.properties.get(0).value, "test");

    assertEquals(debug.properties.get(1).name, "anInt");
    assertEquals(debug.properties.get(1).type, OType.INTEGER);
    assertEquals(debug.properties.get(1).value, 2);

    assertEquals(debug.properties.get(2).name, "anDouble");
    assertEquals(debug.properties.get(2).type, OType.DOUBLE);
    assertEquals(debug.properties.get(2).value, 2D);
  }

  @Test
  public void testSchemaFullDocumentDebug() {
    OClass clazz = db.getMetadata().getSchema().createClass("some");
    clazz.createProperty(db, "testP", OType.STRING);
    clazz.createProperty(db, "theInt", OType.INTEGER);
    ODocument doc = new ODocument("some");
    doc.field("testP", "test");
    doc.field("theInt", 2);
    doc.field("anDouble", 2D);

    byte[] bytes = doc.toStream();

    ORecordSerializerBinaryDebug debugger = new ORecordSerializerBinaryDebug();
    ORecordSerializationDebug debug = debugger.deserializeDebug(bytes, db);

    assertEquals(debug.properties.size(), 3);
    assertEquals(debug.properties.get(0).name, "testP");
    assertEquals(debug.properties.get(0).type, OType.STRING);
    assertEquals(debug.properties.get(0).value, "test");

    assertEquals(debug.properties.get(1).name, "theInt");
    assertEquals(debug.properties.get(1).type, OType.INTEGER);
    assertEquals(debug.properties.get(1).value, 2);

    assertEquals(debug.properties.get(2).name, "anDouble");
    assertEquals(debug.properties.get(2).type, OType.DOUBLE);
    assertEquals(debug.properties.get(2).value, 2D);
  }

  @Test
  public void testSimpleBrokenDocumentDebug() {
    ODocument doc = new ODocument();
    doc.field("test", "test");
    doc.field("anInt", 2);
    doc.field("anDouble", 2D);

    byte[] bytes = doc.toStream();
    byte[] brokenBytes = new byte[bytes.length - 10];
    System.arraycopy(bytes, 0, brokenBytes, 0, bytes.length - 10);

    ORecordSerializerBinaryDebug debugger = new ORecordSerializerBinaryDebug();
    ORecordSerializationDebug debug = debugger.deserializeDebug(brokenBytes, db);

    assertEquals(debug.properties.size(), 3);
    assertEquals(debug.properties.get(0).name, "test");
    assertEquals(debug.properties.get(0).type, OType.STRING);
    assertTrue(debug.properties.get(0).faildToRead);
    assertNotNull(debug.properties.get(0).readingException);

    assertEquals(debug.properties.get(1).name, "anInt");
    assertEquals(debug.properties.get(1).type, OType.INTEGER);
    assertTrue(debug.properties.get(1).faildToRead);
    assertNotNull(debug.properties.get(1).readingException);

    assertEquals(debug.properties.get(2).name, "anDouble");
    assertEquals(debug.properties.get(2).type, OType.DOUBLE);
    assertTrue(debug.properties.get(2).faildToRead);
    assertNotNull(debug.properties.get(2).readingException);
  }

  @Test
  public void testBrokenSchemaFullDocumentDebug() {
    OClass clazz = db.getMetadata().getSchema().createClass("some");
    clazz.createProperty(db, "testP", OType.STRING);
    clazz.createProperty(db, "theInt", OType.INTEGER);
    ODocument doc = new ODocument("some");
    doc.field("testP", "test");
    doc.field("theInt", 2);
    doc.field("anDouble", 2D);

    byte[] bytes = doc.toStream();
    byte[] brokenBytes = new byte[bytes.length - 10];
    System.arraycopy(bytes, 0, brokenBytes, 0, bytes.length - 10);

    ORecordSerializerBinaryDebug debugger = new ORecordSerializerBinaryDebug();
    ORecordSerializationDebug debug = debugger.deserializeDebug(brokenBytes, db);

    assertEquals(debug.properties.size(), 3);
    assertEquals(debug.properties.get(0).name, "testP");
    assertEquals(debug.properties.get(0).type, OType.STRING);
    assertTrue(debug.properties.get(0).faildToRead);
    assertNotNull(debug.properties.get(0).readingException);

    assertEquals(debug.properties.get(1).name, "theInt");
    assertEquals(debug.properties.get(1).type, OType.INTEGER);
    assertTrue(debug.properties.get(1).faildToRead);
    assertNotNull(debug.properties.get(1).readingException);

    assertEquals(debug.properties.get(2).name, "anDouble");
    assertEquals(debug.properties.get(2).type, OType.DOUBLE);
    assertTrue(debug.properties.get(2).faildToRead);
    assertNotNull(debug.properties.get(2).readingException);
  }
}
