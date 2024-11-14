package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexDefinition;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class ClassIndexManagerTest extends DocumentDBBaseTest {
  @Parameters(value = "remote")
  public ClassIndexManagerTest(@Optional Boolean remote) {
    super(remote != null ? remote : false);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final OSchema schema = database.getMetadata().getSchema();

    if (schema.existsClass("classIndexManagerTestClass")) {
      schema.dropClass("classIndexManagerTestClass");
    }

    if (schema.existsClass("classIndexManagerTestClassTwo")) {
      schema.dropClass("classIndexManagerTestClassTwo");
    }

    if (schema.existsClass("classIndexManagerTestSuperClass")) {
      schema.dropClass("classIndexManagerTestSuperClass");
    }

    if (schema.existsClass("classIndexManagerTestCompositeCollectionClass")) {
      schema.dropClass("classIndexManagerTestCompositeCollectionClass");
    }

    final OClass superClass = schema.createClass("classIndexManagerTestSuperClass");
    final OProperty propertyZero = superClass.createProperty("prop0", OType.STRING);
    superClass.createIndex(
        "classIndexManagerTestSuperClass.prop0",
        OClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        new ODocument().fields("ignoreNullValues", true),
        new String[] {"prop0"});

    final OClass oClass = schema.createClass("classIndexManagerTestClass", superClass);
    final OProperty propOne = oClass.createProperty("prop1", OType.STRING);
    oClass.createIndex(
        "classIndexManagerTestClass.prop1",
        OClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        new ODocument().fields("ignoreNullValues", true),
        new String[] {"prop1"});

    final OProperty propTwo = oClass.createProperty("prop2", OType.INTEGER);
    propTwo.createIndex(OClass.INDEX_TYPE.NOTUNIQUE);

    oClass.createProperty("prop3", OType.BOOLEAN);

    final OProperty propFour = oClass.createProperty("prop4", OType.EMBEDDEDLIST, OType.STRING);
    propFour.createIndex(OClass.INDEX_TYPE.NOTUNIQUE);

    oClass.createProperty("prop5", OType.EMBEDDEDMAP, OType.STRING);
    oClass.createIndex("classIndexManagerTestIndexByKey", OClass.INDEX_TYPE.NOTUNIQUE, "prop5");
    oClass.createIndex(
        "classIndexManagerTestIndexByValue", OClass.INDEX_TYPE.NOTUNIQUE, "prop5 by value");

    final OProperty propSix = oClass.createProperty("prop6", OType.EMBEDDEDSET, OType.STRING);
    propSix.createIndex(OClass.INDEX_TYPE.NOTUNIQUE);

    oClass.createIndex(
        "classIndexManagerComposite",
        OClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        new ODocument().fields("ignoreNullValues", true),
        new String[] {"prop1", "prop2"});

    final OClass oClassTwo = schema.createClass("classIndexManagerTestClassTwo");
    oClassTwo.createProperty("prop1", OType.STRING);
    oClassTwo.createProperty("prop2", OType.INTEGER);

    final OClass compositeCollectionClass =
        schema.createClass("classIndexManagerTestCompositeCollectionClass");
    compositeCollectionClass.createProperty("prop1", OType.STRING);
    compositeCollectionClass.createProperty("prop2", OType.EMBEDDEDLIST, OType.INTEGER);

    compositeCollectionClass.createIndex(
        "classIndexManagerTestIndexValueAndCollection",
        OClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        new ODocument().fields("ignoreNullValues", true),
        new String[] {"prop1", "prop2"});

    oClass.createIndex(
        "classIndexManagerTestIndexOnPropertiesFromClassAndSuperclass",
        OClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        new ODocument().fields("ignoreNullValues", true),
        new String[] {"prop0", "prop1"});

    database.close();
  }

  @AfterMethod
  public void afterMethod() throws Exception {
    database.begin();
    database.command("delete from classIndexManagerTestClass").close();
    database.commit();

    database.begin();
    database.command("delete from classIndexManagerTestClassTwo").close();
    database.commit();

    database.begin();
    database.command("delete from classIndexManagerTestSuperClass").close();
    database.commit();

    if (!database.getStorage().isRemote()) {
      Assert.assertEquals(
          database
              .getMetadata()
              .getIndexManagerInternal()
              .getIndex(database, "classIndexManagerTestClass.prop1")
              .getInternal()
              .size(),
          0);
      Assert.assertEquals(
          database
              .getMetadata()
              .getIndexManagerInternal()
              .getIndex(database, "classIndexManagerTestClass.prop2")
              .getInternal()
              .size(),
          0);
    }

    super.afterMethod();
  }

  public void testPropertiesCheckUniqueIndexDubKeysCreate() {
    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    final ODocument docTwo = new ODocument("classIndexManagerTestClass");

    docOne.field("prop1", "a");
    database.begin();
    docOne.save();
    database.commit();

    boolean exceptionThrown = false;
    try {
      docTwo.field("prop1", "a");
      database.begin();
      docTwo.save();
      database.commit();

    } catch (ORecordDuplicatedException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullCreate() {
    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    final ODocument docTwo = new ODocument("classIndexManagerTestClass");

    docOne.field("prop1", "a");
    database.begin();
    docOne.save();
    database.commit();

    docTwo.field("prop1", (String) null);
    database.begin();
    docTwo.save();
    database.commit();
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullCreateInTx() {
    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    final ODocument docTwo = new ODocument("classIndexManagerTestClass");

    database.begin();
    docOne.field("prop1", "a");
    docOne.save();

    docTwo.field("prop1", (String) null);
    docTwo.save();
    database.commit();
  }

  public void testPropertiesCheckUniqueIndexInParentDubKeysCreate() {
    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    final ODocument docTwo = new ODocument("classIndexManagerTestClass");

    docOne.field("prop0", "a");
    database.begin();
    docOne.save();
    database.commit();

    boolean exceptionThrown = false;
    try {
      docTwo.field("prop0", "a");
      database.begin();
      docTwo.save();
      database.commit();
    } catch (ORecordDuplicatedException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  public void testPropertiesCheckUniqueIndexDubKeysUpdate() {
    database.begin();
    ODocument docOne = new ODocument("classIndexManagerTestClass");
    ODocument docTwo = new ODocument("classIndexManagerTestClass");

    boolean exceptionThrown = false;
    docOne.field("prop1", "a");

    docOne.save();
    database.commit();

    database.begin();
    docTwo.field("prop1", "b");

    docTwo.save();
    database.commit();

    try {
      database.begin();
      docTwo = database.bindToSession(docTwo);
      docTwo.field("prop1", "a");

      docTwo.save();
      database.commit();
    } catch (ORecordDuplicatedException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullUpdate() {
    database.begin();
    ODocument docOne = new ODocument("classIndexManagerTestClass");
    ODocument docTwo = new ODocument("classIndexManagerTestClass");

    docOne.field("prop1", "a");

    docOne.save();
    database.commit();

    database.begin();
    docTwo.field("prop1", "b");

    docTwo.save();
    database.commit();

    database.begin();
    docTwo = database.bindToSession(docTwo);
    docTwo.field("prop1", (String) null);

    docTwo.save();
    database.commit();
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullUpdateInTX() {
    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    final ODocument docTwo = new ODocument("classIndexManagerTestClass");

    database.begin();
    docOne.field("prop1", "a");
    docOne.save();

    docTwo.field("prop1", "b");
    docTwo.save();

    docTwo.field("prop1", (String) null);
    docTwo.save();
    database.commit();
  }

  public void testPropertiesCheckNonUniqueIndexDubKeys() {
    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    docOne.field("prop2", 1);
    database.begin();
    docOne.save();
    database.commit();

    final ODocument docTwo = new ODocument("classIndexManagerTestClass");
    docTwo.field("prop2", 1);
    database.begin();
    docTwo.save();
    database.commit();
  }

  public void testPropertiesCheckUniqueNullKeys() {
    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    database.begin();
    docOne.save();
    database.commit();

    final ODocument docTwo = new ODocument("classIndexManagerTestClass");
    database.begin();
    docTwo.save();
    database.commit();
  }

  public void testCreateDocumentWithoutClass() {
    checkEmbeddedDB();

    final Collection<? extends OIndex> beforeIndexes =
        database.getMetadata().getIndexManagerInternal().getIndexes(database);
    final Map<String, Long> indexSizeMap = new HashMap<>();

    for (final OIndex index : beforeIndexes) {
      indexSizeMap.put(index.getName(), index.getInternal().size());
    }

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.field("prop1", "a");
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.field("prop1", "a");
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    final Collection<? extends OIndex> afterIndexes =
        database.getMetadata().getIndexManagerInternal().getIndexes(database);
    for (final OIndex index : afterIndexes) {
      Assert.assertEquals(
          index.getInternal().size(), indexSizeMap.get(index.getName()).longValue());
    }
  }

  public void testUpdateDocumentWithoutClass() {
    checkEmbeddedDB();

    final Collection<? extends OIndex> beforeIndexes =
        database.getMetadata().getIndexManagerInternal().getIndexes(database);
    final Map<String, Long> indexSizeMap = new HashMap<>();

    for (final OIndex index : beforeIndexes) {
      indexSizeMap.put(index.getName(), index.getInternal().size());
    }

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.field("prop1", "a");
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.field("prop1", "b");
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    docOne.field("prop1", "a");
    docOne.save();
    database.commit();

    final Collection<? extends OIndex> afterIndexes =
        database.getMetadata().getIndexManagerInternal().getIndexes(database);
    for (final OIndex index : afterIndexes) {
      Assert.assertEquals(
          index.getInternal().size(), indexSizeMap.get(index.getName()).longValue());
    }
  }

  public void testDeleteDocumentWithoutClass() {
    final ODocument docOne = new ODocument();
    docOne.field("prop1", "a");

    database.begin();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    database.bindToSession(docOne).delete();
    database.commit();
  }

  public void testDeleteModifiedDocumentWithoutClass() {
    ODocument docOne = new ODocument();
    docOne.field("prop1", "a");

    database.begin();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    docOne = database.bindToSession(docOne);
    docOne.field("prop1", "b");
    docOne.delete();
    database.commit();
  }

  public void testDocumentUpdateWithoutDirtyFields() {
    database.begin();
    ODocument docOne = new ODocument("classIndexManagerTestClass");
    docOne.field("prop1", "a");

    docOne.save();
    database.commit();

    database.begin();
    docOne = database.bindToSession(docOne);
    docOne.setDirty();
    docOne.save();
    database.commit();
  }

  public void testCreateDocumentIndexRecordAdded() {
    checkEmbeddedDB();

    final ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    database.begin();
    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");
    final OClass oSuperClass = schema.getClass("classIndexManagerTestSuperClass");

    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    try (Stream<ORID> stream = propOneIndex.getInternal().getRids("a")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    Assert.assertEquals(propOneIndex.getInternal().size(), 1);

    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");

    final OIndexDefinition compositeIndexDefinition = compositeIndex.getDefinition();
    try (Stream<ORID> rids =
        compositeIndex.getInternal().getRids(compositeIndexDefinition.createValue("a", 1))) {
      Assert.assertTrue(rids.findFirst().isPresent());
    }
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);

    final OIndex propZeroIndex = oSuperClass.getClassIndex("classIndexManagerTestSuperClass.prop0");
    try (Stream<ORID> stream = propZeroIndex.getInternal().getRids("x")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    Assert.assertEquals(propZeroIndex.getInternal().size(), 1);
  }

  public void testUpdateDocumentIndexRecordRemoved() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");
    final OIndex propZeroIndex = oSuperClass.getClassIndex("classIndexManagerTestSuperClass.prop0");

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(), 1);

    database.begin();
    doc = database.bindToSession(doc);
    doc.removeField("prop2");
    doc.removeField("prop0");

    doc.save();
    database.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);
    Assert.assertEquals(propZeroIndex.getInternal().size(), 0);
  }

  public void testUpdateDocumentNullKeyIndexRecordRemoved() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");

    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");
    final OIndex propZeroIndex = oSuperClass.getClassIndex("classIndexManagerTestSuperClass.prop0");

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(), 1);

    database.begin();
    doc = database.bindToSession(doc);
    doc.field("prop2", (Object) null);
    doc.field("prop0", (Object) null);
    doc.save();
    database.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);
    Assert.assertEquals(propZeroIndex.getInternal().size(), 0);
  }

  public void testUpdateDocumentIndexRecordUpdated() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propZeroIndex = oSuperClass.getClassIndex("classIndexManagerTestSuperClass.prop0");
    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");
    final OIndexDefinition compositeIndexDefinition = compositeIndex.getDefinition();

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(), 1);

    database.begin();
    doc = database.bindToSession(doc);
    doc.field("prop2", 2);
    doc.field("prop0", "y");
    doc.save();
    database.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(), 1);

    try (Stream<ORID> stream = propZeroIndex.getInternal().getRids("y")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    try (Stream<ORID> stream = propOneIndex.getInternal().getRids("a")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream =
        compositeIndex.getInternal().getRids(compositeIndexDefinition.createValue("a", 2))) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testUpdateDocumentIndexRecordUpdatedFromNullField() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop1", "a");
    doc.field("prop2", (Object) null);

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");
    final OIndexDefinition compositeIndexDefinition = compositeIndex.getDefinition();

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);

    database.begin();
    doc = database.bindToSession(doc);
    doc.field("prop2", 2);
    doc.save();
    database.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);

    try (Stream<ORID> stream = propOneIndex.getInternal().getRids("a")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream =
        compositeIndex.getInternal().getRids(compositeIndexDefinition.createValue("a", 2))) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
  }

  public void testListUpdate() {
    checkEmbeddedDB();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propFourIndex = oClass.getClassIndex("classIndexManagerTestClass.prop4");

    Assert.assertEquals(propFourIndex.getInternal().size(), 0);

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");

    final List<String> listProperty = new ArrayList<>();
    listProperty.add("value1");
    listProperty.add("value2");

    doc.field("prop4", listProperty);
    doc.save();
    database.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(), 2);
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value1")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    List<String> trackedList = doc.field("prop4");
    trackedList.set(0, "value3");

    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.remove("value4");
    trackedList.remove("value2");
    trackedList.add("value5");

    doc.save();
    database.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(), 3);
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testMapUpdate() {
    checkEmbeddedDB();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propFiveIndexKey = oClass.getClassIndex("classIndexManagerTestIndexByKey");
    final OIndex propFiveIndexValue = oClass.getClassIndex("classIndexManagerTestIndexByValue");

    Assert.assertEquals(propFiveIndexKey.getInternal().size(), 0);

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");

    final Map<String, String> mapProperty = new HashMap<>();
    mapProperty.put("key1", "value1");
    mapProperty.put("key2", "value2");

    doc.field("prop5", mapProperty);
    doc.save();
    database.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(), 2);
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    Map<String, String> trackedMap = doc.field("prop5");
    trackedMap.put("key3", "value3");
    trackedMap.put("key4", "value4");
    trackedMap.remove("key1");
    trackedMap.put("key1", "value5");
    trackedMap.remove("key2");
    trackedMap.put("key6", "value6");
    trackedMap.put("key7", "value6");
    trackedMap.put("key8", "value6");
    trackedMap.put("key4", "value7");

    trackedMap.remove("key8");

    doc.save();
    database.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(), 5);
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    Assert.assertEquals(propFiveIndexValue.getInternal().size(), 4);
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testSetUpdate() {
    checkEmbeddedDB();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propSixIndex = oClass.getClassIndex("classIndexManagerTestClass.prop6");

    Assert.assertEquals(propSixIndex.getInternal().size(), 0);

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");

    final Set<String> setProperty = new HashSet<>();
    setProperty.add("value1");
    setProperty.add("value2");

    doc.field("prop6", setProperty);
    doc.save();
    database.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(), 2);
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    Set<String> trackedSet = doc.field("prop6");

    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    trackedSet.remove("value4");
    trackedSet.remove("value2");
    trackedSet.add("value5");

    doc.save();
    database.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(), 2);
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testListDelete() {
    checkEmbeddedDB();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propFourIndex = oClass.getClassIndex("classIndexManagerTestClass.prop4");

    Assert.assertEquals(propFourIndex.getInternal().size(), 0);

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");

    final List<String> listProperty = new ArrayList<>();
    listProperty.add("value1");
    listProperty.add("value2");

    doc.field("prop4", listProperty);
    doc.save();
    database.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(), 2);
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    List<String> trackedList = doc.field("prop4");
    trackedList.set(0, "value3");

    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.remove("value4");
    trackedList.remove("value2");
    trackedList.add("value5");

    doc.save();
    database.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(), 3);
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFourIndex.getInternal().getRids("value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    trackedList = doc.field("prop4");
    trackedList.remove("value3");
    trackedList.remove("value4");
    trackedList.add("value8");

    doc.delete();
    database.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(), 0);
  }

  public void testMapDelete() {
    checkEmbeddedDB();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propFiveIndexKey = oClass.getClassIndex("classIndexManagerTestIndexByKey");
    final OIndex propFiveIndexValue = oClass.getClassIndex("classIndexManagerTestIndexByValue");

    Assert.assertEquals(propFiveIndexKey.getInternal().size(), 0);

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");

    final Map<String, String> mapProperty = new HashMap<>();
    mapProperty.put("key1", "value1");
    mapProperty.put("key2", "value2");

    doc.field("prop5", mapProperty);
    doc.save();
    database.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(), 2);
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    Map<String, String> trackedMap = doc.field("prop5");
    trackedMap.put("key3", "value3");
    trackedMap.put("key4", "value4");
    trackedMap.remove("key1");
    trackedMap.put("key1", "value5");
    trackedMap.remove("key2");
    trackedMap.put("key6", "value6");
    trackedMap.put("key7", "value6");
    trackedMap.put("key8", "value6");
    trackedMap.put("key4", "value7");

    trackedMap.remove("key8");

    doc.save();
    database.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(), 5);
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexKey.getInternal().getRids("key7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    Assert.assertEquals(propFiveIndexValue.getInternal().size(), 4);
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propFiveIndexValue.getInternal().getRids("value6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    trackedMap = doc.field("prop5");

    trackedMap.remove("key1");
    trackedMap.remove("key3");
    trackedMap.remove("key4");
    trackedMap.put("key6", "value10");
    trackedMap.put("key11", "value11");

    doc.delete();
    database.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(), 0);
    Assert.assertEquals(propFiveIndexValue.getInternal().size(), 0);
  }

  public void testSetDelete() {
    checkEmbeddedDB();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propSixIndex = oClass.getClassIndex("classIndexManagerTestClass.prop6");

    Assert.assertEquals(propSixIndex.getInternal().size(), 0);

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");

    final Set<String> setProperty = new HashSet<>();
    setProperty.add("value1");
    setProperty.add("value2");

    doc.field("prop6", setProperty);
    doc.save();
    database.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(), 2);
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    Set<String> trackedSet = doc.field("prop6");

    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    trackedSet.remove("value4");
    trackedSet.remove("value2");
    trackedSet.add("value5");

    doc.save();
    database.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(), 2);
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (Stream<ORID> stream = propSixIndex.getInternal().getRids("value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    database.begin();
    doc = database.bindToSession(doc);
    trackedSet = doc.field("prop6");
    trackedSet.remove("value1");
    trackedSet.add("value6");

    doc.delete();
    database.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(), 0);
  }

  public void testDeleteDocumentIndexRecordDeleted() {
    checkEmbeddedDB();

    final ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    database.begin();
    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propZeroIndex = oSuperClass.getClassIndex("classIndexManagerTestSuperClass.prop0");
    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");

    Assert.assertEquals(propZeroIndex.getInternal().size(), 1);
    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(propZeroIndex.getInternal().size(), 0);
    Assert.assertEquals(propOneIndex.getInternal().size(), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);
  }

  public void testDeleteUpdatedDocumentIndexRecordDeleted() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");

    final OIndex propZeroIndex = oSuperClass.getClassIndex("classIndexManagerTestSuperClass.prop0");
    Assert.assertEquals(propZeroIndex.getInternal().size(), 1);
    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 1);

    database.begin();
    doc = database.bindToSession(doc);
    doc.field("prop2", 2);
    doc.field("prop0", "y");

    doc.delete();
    database.commit();

    Assert.assertEquals(propZeroIndex.getInternal().size(), 0);
    Assert.assertEquals(propOneIndex.getInternal().size(), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);
  }

  public void testDeleteUpdatedDocumentNullFieldIndexRecordDeleted() {
    checkEmbeddedDB();

    database.begin();
    final ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop1", "a");
    doc.field("prop2", (Object) null);

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);
  }

  public void testDeleteUpdatedDocumentOrigNullFieldIndexRecordDeleted() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClass");
    doc.field("prop1", "a");
    doc.field("prop2", (Object) null);

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final OIndex propOneIndex = oClass.getClassIndex("classIndexManagerTestClass.prop1");
    final OIndex compositeIndex = oClass.getClassIndex("classIndexManagerComposite");

    Assert.assertEquals(propOneIndex.getInternal().size(), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);

    database.begin();
    doc = database.bindToSession(doc);
    doc.field("prop2", 2);

    doc.delete();
    database.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(), 0);
  }

  public void testNoClassIndexesUpdate() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClassTwo");
    doc.field("prop1", "a");

    doc.save();
    database.commit();
    database.begin();
    doc = database.bindToSession(doc);
    doc.field("prop1", "b");

    doc.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");

    final Collection<OIndex> indexes = oClass.getIndexes();
    for (final OIndex index : indexes) {
      Assert.assertEquals(index.getInternal().size(), 0);
    }
  }

  public void testNoClassIndexesDelete() {
    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestClassTwo");
    doc.field("prop1", "a");

    doc.save();
    database.commit();

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();
  }

  public void testCollectionCompositeCreation() {
    checkEmbeddedDB();

    final ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    database.begin();
    doc.save();
    database.commit();

    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 1))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeNullSimpleFieldCreation() {
    checkEmbeddedDB();

    database.begin();
    final ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", (Object) null);
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 0);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();
  }

  public void testCollectionCompositeNullCollectionFieldCreation() {
    checkEmbeddedDB();

    final ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", (Object) null);

    database.begin();
    doc.save();
    database.commit();

    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 0);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();
  }

  public void testCollectionCompositeUpdateSimpleField() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop1", "test2");

    doc.save();
    database.commit();

    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test2", 1))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test2", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    Assert.assertEquals(index.getInternal().size(), 2);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasAssigned() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop2", Arrays.asList(1, 3));

    doc.save();
    database.commit();

    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 1))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 3))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    Assert.assertEquals(index.getInternal().size(), 2);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasChanged() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);
    docList.add(5);

    docList.remove(0);

    doc.save();
    database.commit();

    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 3))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 4))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test1", 5))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    Assert.assertEquals(index.getInternal().size(), 4);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasChangedSimpleFieldWasAssigned() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);
    docList.add(5);

    docList.remove(0);

    doc.field("prop1", "test2");

    doc.save();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 4);

    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test2", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test2", 3))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test2", 4))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (Stream<ORID> stream = index.getInternal().getRids(new OCompositeKey("test2", 5))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeUpdateSimpleFieldNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop1", (Object) null);

    doc.save();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasAssignedNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop2", (Object) null);

    doc.save();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeUpdateBothAssignedNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    doc = database.bindToSession(doc);
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc.field("prop2", (Object) null);
    doc.field("prop1", (Object) null);

    doc.save();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasChangedSimpleFieldWasAssignedNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);
    docList.add(5);

    docList.remove(0);

    doc.field("prop1", (Object) null);

    doc.save();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);

    database.begin();
    database.bindToSession(doc).delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteSimpleFieldAssigend() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop1", "test2");

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldAssigend() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop2", Arrays.asList(1, 3));

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldChanged() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);

    docList.remove(1);

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteBothCollectionSimpleFieldChanged() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    doc = database.bindToSession(doc);
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);

    docList.remove(1);

    doc.field("prop1", "test2");

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteBothCollectionSimpleFieldAssigend() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop2", Arrays.asList(1, 3));
    doc.field("prop1", "test2");

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteSimpleFieldNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    doc = database.bindToSession(doc);
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc.field("prop1", (Object) null);

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc = database.bindToSession(doc);
    doc.field("prop2", (Object) null);

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteBothSimpleCollectionFieldNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    doc = database.bindToSession(doc);
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    doc.field("prop2", (Object) null);
    doc.field("prop1", (Object) null);

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldChangedSimpleFieldNull() {
    checkEmbeddedDB();

    database.begin();
    ODocument doc = new ODocument("classIndexManagerTestCompositeCollectionClass");

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    database.commit();

    database.begin();
    doc = database.bindToSession(doc);
    final OIndex index =
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);

    docList.remove(1);

    doc.field("prop1", (Object) null);

    doc.delete();
    database.commit();

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testIndexOnPropertiesFromClassAndSuperclass() {
    checkEmbeddedDB();

    final ODocument docOne = new ODocument("classIndexManagerTestClass");
    docOne.field("prop0", "doc1-prop0");
    docOne.field("prop1", "doc1-prop1");

    database.begin();
    docOne.save();
    database.commit();

    final ODocument docTwo = new ODocument("classIndexManagerTestClass");
    docTwo.field("prop0", "doc2-prop0");
    docTwo.field("prop1", "doc2-prop1");

    database.begin();
    docTwo.save();
    database.commit();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass oClass = schema.getClass("classIndexManagerTestClass");
    final OIndex oIndex =
        oClass.getClassIndex("classIndexManagerTestIndexOnPropertiesFromClassAndSuperclass");

    Assert.assertEquals(oIndex.getInternal().size(), 2);
  }
}
