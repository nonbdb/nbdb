package com.orientechnologies.orient.test.database.auto;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Matan Shukry (matanshukry@gmail.com)
 * @since 3/3/2015
 */
public class DefaultValuesTrivialTest {
  private static final int DOCUMENT_COUNT = 50;

  private ODatabaseSessionInternal database;

  @BeforeMethod
  public void before() {
    //noinspection deprecation
    database = new ODatabaseDocumentTx("memory:" + DefaultValuesTrivialTest.class.getSimpleName());
    //noinspection deprecation
    database.create();
  }

  @AfterMethod
  public void after() {
    //noinspection deprecation
    database.drop();
  }

  @Test
  public void test() {

    // create example schema
    OSchema schema = database.getMetadata().getSchema();
    OClass classPerson = schema.createClass("Person");

    classPerson.createProperty("name", OType.STRING);
    classPerson.createProperty("join_date", OType.DATETIME).setDefaultValue("sysdate()");
    classPerson.createProperty("active", OType.BOOLEAN).setDefaultValue("true");

    Date dtStart = getDatabaseSysdate(database);

    ODocument[] docs = new ODocument[DOCUMENT_COUNT];
    for (int i = 0; i < DOCUMENT_COUNT; ++i) {
      database.begin();
      ODocument doc = new ODocument("Person");
      doc.field("name", "autoGeneratedName #" + i);
      doc.save();
      database.commit();

      docs[i] = doc;
    }

    Date dtAfter = getDatabaseSysdate(database);
    for (int i = 0; i < DOCUMENT_COUNT; ++i) {
      final ODocument doc = docs[i];

      try {
        //
        Date dt = doc.field("join_date", OType.DATETIME);

        boolean isInRange = (!dt.before(dtStart)) && (!dt.after(dtAfter));
        Assert.assertTrue(isInRange);

        //
        boolean active = doc.field("active", OType.BOOLEAN);
        Assert.assertTrue(active);
      } catch (Exception ex) {
        ex.printStackTrace();
        Assert.fail();
      }
    }
  }

  private static Date getDatabaseSysdate(ODatabaseSession database) {
    try (OResultSet dates = database.query("SELECT sysdate() as sysdate")) {
      return dates.next().getProperty("sysdate");
    }
  }

  @Test
  public void testDefaultValueConversion() {
    OSchema schema = database.getMetadata().getSchema();
    OClass classPerson = schema.createClass("Person");
    classPerson.createProperty("users", OType.LINKSET).setDefaultValue("[#5:1]");

    ODocument doc = new ODocument("Person");

    database.begin();
    ORecord record = database.save(doc);
    database.commit();

    ODocument doc1 = database.load(record.getIdentity());
    Set<OIdentifiable> rids = doc1.field("users");
    assertEquals(rids.size(), 1);
    assertEquals(rids.iterator().next(), new ORecordId(5, 1));
  }

  @Test
  public void testPrepopulation() {
    // create example schema
    OSchema schema = database.getMetadata().getSchema();
    OClass classA = schema.createClass("ClassA");

    classA.createProperty("name", OType.STRING).setDefaultValue("default name");
    classA.createProperty("date", OType.DATETIME).setDefaultValue("sysdate()");
    classA.createProperty("active", OType.BOOLEAN).setDefaultValue("true");

    {
      ODocument doc = new ODocument(classA);
      assertEquals("default name", doc.field("name"));
      assertNotNull(doc.field("date"));
      assertEquals((Boolean) true, doc.field("active"));
    }

    {
      ODocument doc = new ODocument();
      assertNull(doc.field("name"));
      assertNull(doc.field("date"));
      assertNull(doc.field("active"));
      doc.setClassName(classA.getName());
      assertEquals("default name", doc.field("name"));
      assertNotNull(doc.field("date"));
      assertEquals((Boolean) true, doc.field("active"));
    }

    {
      ODocument doc = new ODocument();
      assertNull(doc.field("name"));
      assertNull(doc.field("date"));
      assertNull(doc.field("active"));
      doc.setClassNameIfExists(classA.getName());
      assertEquals("default name", doc.field("name"));
      assertNotNull(doc.field("date"));
      assertEquals((Boolean) true, doc.field("active"));
    }
  }

  @Test
  public void testPrepopulationIndex() {
    // create example schema
    OSchema schema = database.getMetadata().getSchema();
    OClass classA = schema.createClass("ClassA");

    OProperty prop = classA.createProperty("name", OType.STRING);
    prop.setDefaultValue("default name");
    OIndex index = prop.createIndex(OClass.INDEX_TYPE.NOTUNIQUE);

    {
      ODocument doc = new ODocument(classA);
      assertEquals("default name", doc.field("name"));
      database.begin();
      database.save(doc);
      database.commit();
      try (Stream<ORID> stream = index.getInternal().getRids("default name")) {
        assertEquals(1, stream.count());
      }
    }
  }

  @Test
  public void testPrepopulationIndexTx() {

    // create example schema
    OSchema schema = database.getMetadata().getSchema();
    OClass classA = schema.createClass("ClassA");

    OProperty prop = classA.createProperty("name", OType.STRING);
    prop.setDefaultValue("default name");
    OIndex index = prop.createIndex(OClass.INDEX_TYPE.NOTUNIQUE);

    {
      database.begin();
      ODocument doc = new ODocument(classA);
      assertEquals("default name", doc.field("name"));

      database.begin();
      database.save(doc);
      database.commit();

      try (Stream<ORID> stream = index.getInternal().getRids("default name")) {
        assertEquals(1, stream.count());
      }
      database.commit();
      try (Stream<ORID> stream = index.getInternal().getRids("default name")) {
        assertEquals(1, stream.count());
      }
    }
  }

  @Test
  public void testPrepopulationMultivalueIndex() {

    // create example schema
    OSchema schema = database.getMetadata().getSchema();
    OClass classA = schema.createClass("ClassA");

    OProperty prop = classA.createProperty("name", OType.STRING);
    prop.setDefaultValue("default name");
    OProperty prop2 = classA.createProperty("value", OType.STRING);
    OIndex index = classA.createIndex("multi", OClass.INDEX_TYPE.NOTUNIQUE, "value", "name");

    {
      ODocument doc = new ODocument(classA);
      assertEquals("default name", doc.field("name"));
      doc.field("value", "1");

      database.begin();
      database.save(doc);
      database.commit();

      try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("1"))) {
        assertEquals(1, stream.count());
      }
    }
    {
      ODocument doc = new ODocument(classA);
      assertEquals("default name", doc.field("name"));
      doc.field("value", "2");

      database.begin();
      database.save(doc);
      database.commit();

      try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("2"))) {
        assertEquals(1, stream.count());
      }
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("3"))) {
      assertEquals(0, stream.count());
    }
  }
}
