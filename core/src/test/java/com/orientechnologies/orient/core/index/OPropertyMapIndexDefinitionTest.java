package com.orientechnologies.orient.core.index;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OMultiValueChangeEvent;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author LomakiA <a href="mailto:a.lomakin@orientechnologies.com">Andrey Lomakin</a>
 * @since 20.12.11
 */
public class OPropertyMapIndexDefinitionTest extends BaseMemoryDatabase {

  private final Map<String, Integer> mapToTest = new HashMap<String, Integer>();
  private OPropertyMapIndexDefinition propertyIndexByKey;
  private OPropertyMapIndexDefinition propertyIndexByValue;
  private OPropertyMapIndexDefinition propertyIndexByIntegerKey;

  @Before
  public void beforeClass() {
    mapToTest.put("st1", 1);
    mapToTest.put("st2", 2);
  }

  @Before
  public void beforeMethod() {
    propertyIndexByKey =
        new OPropertyMapIndexDefinition(
            "testClass", "fOne", OType.STRING, OPropertyMapIndexDefinition.INDEX_BY.KEY);
    propertyIndexByIntegerKey =
        new OPropertyMapIndexDefinition(
            "testClass", "fTwo", OType.INTEGER, OPropertyMapIndexDefinition.INDEX_BY.KEY);
    propertyIndexByValue =
        new OPropertyMapIndexDefinition(
            "testClass", "fOne", OType.INTEGER, OPropertyMapIndexDefinition.INDEX_BY.VALUE);
  }

  @Test
  public void testCreateValueByKeySingleParameter() {
    final Object result = propertyIndexByKey.createValue(db, Collections.singletonList(mapToTest));
    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains("st1"));
    Assert.assertTrue(collectionResult.contains("st2"));
  }

  @Test
  public void testCreateValueByValueSingleParameter() {
    final Object result =
        propertyIndexByValue.createValue(db, Collections.singletonList(mapToTest));
    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains(1));
    Assert.assertTrue(collectionResult.contains(2));
  }

  @Test
  public void testCreateValueByKeyTwoParameters() {
    final Object result = propertyIndexByKey.createValue(db, Arrays.asList(mapToTest, "25"));

    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains("st1"));
    Assert.assertTrue(collectionResult.contains("st2"));
  }

  @Test
  public void testCreateValueByValueTwoParameters() {
    final Object result = propertyIndexByValue.createValue(db, Arrays.asList(mapToTest, "25"));

    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains(1));
    Assert.assertTrue(collectionResult.contains(2));
  }

  @Test
  public void testCreateValueWrongParameter() {
    final Object result = propertyIndexByKey.createValue(db, Collections.singletonList("tt"));
    Assert.assertNull(result);
  }

  @Test
  public void testCreateValueByKeySingleParameterArrayParams() {
    final Object result = propertyIndexByKey.createValue(db, mapToTest);
    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains("st1"));
    Assert.assertTrue(collectionResult.contains("st2"));
  }

  @Test
  public void testCreateValueByValueSingleParameterArrayParams() {
    final Object result = propertyIndexByValue.createValue(db, mapToTest);
    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains(1));
    Assert.assertTrue(collectionResult.contains(2));
  }

  @Test
  public void testCreateValueByKeyTwoParametersArrayParams() {
    final Object result = propertyIndexByKey.createValue(db, mapToTest, "25");

    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains("st1"));
    Assert.assertTrue(collectionResult.contains("st2"));
  }

  @Test
  public void testCreateValueByValueTwoParametersArrayParams() {
    final Object result = propertyIndexByValue.createValue(db, mapToTest, "25");

    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains(1));
    Assert.assertTrue(collectionResult.contains(2));
  }

  @Test
  public void testCreateValueWrongParameterArrayParams() {
    final Object result = propertyIndexByKey.createValue(db, "tt");
    Assert.assertNull(result);
  }

  @Test
  public void testGetDocumentValueByKeyToIndex() {
    final ODocument document = new ODocument();

    document.field("fOne", mapToTest);
    document.field("fTwo", 10);

    final Object result = propertyIndexByKey.getDocumentValueToIndex(db, document);
    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains("st1"));
    Assert.assertTrue(collectionResult.contains("st2"));
  }

  @Test
  public void testGetDocumentValueByValueToIndex() {
    final ODocument document = new ODocument();

    document.field("fOne", mapToTest);
    document.field("fTwo", 10);

    final Object result = propertyIndexByValue.getDocumentValueToIndex(db, document);
    Assert.assertTrue(result instanceof Collection);

    final Collection<?> collectionResult = (Collection<?>) result;

    Assert.assertEquals(collectionResult.size(), 2);
    Assert.assertTrue(collectionResult.contains(1));
    Assert.assertTrue(collectionResult.contains(2));
  }

  @Test
  public void testGetFields() {
    final List<String> result = propertyIndexByKey.getFields();
    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0), "fOne");
  }

  @Test
  public void testGetTypes() {
    final OType[] result = propertyIndexByKey.getTypes();
    Assert.assertEquals(result.length, 1);
    Assert.assertEquals(result[0], OType.STRING);
  }

  @Test
  public void testEmptyIndexByKeyReload() {
    final ODatabaseDocumentTx database = new ODatabaseDocumentTx("memory:propertytest");
    database.create();

    propertyIndexByKey =
        new OPropertyMapIndexDefinition(
            "tesClass", "fOne", OType.STRING, OPropertyMapIndexDefinition.INDEX_BY.KEY);

    database.begin();
    final ODocument docToStore = propertyIndexByKey.toStream(new ODocument());
    database.save(docToStore, database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    final ODocument docToLoad = database.load(docToStore.getIdentity());

    final OPropertyIndexDefinition result = new OPropertyMapIndexDefinition();
    result.fromStream(docToLoad);

    database.drop();
    Assert.assertEquals(result, propertyIndexByKey);
  }

  @Test
  public void testEmptyIndexByValueReload() {
    final ODatabaseDocumentTx database = new ODatabaseDocumentTx("memory:propertytest");
    database.create();

    propertyIndexByValue =
        new OPropertyMapIndexDefinition(
            "tesClass", "fOne", OType.INTEGER, OPropertyMapIndexDefinition.INDEX_BY.VALUE);

    database.begin();
    final ODocument docToStore = propertyIndexByValue.toStream(new ODocument());
    database.save(docToStore, database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    final ODocument docToLoad = database.load(docToStore.getIdentity());

    final OPropertyIndexDefinition result = new OPropertyMapIndexDefinition();
    result.fromStream(docToLoad);

    database.drop();
    Assert.assertEquals(result, propertyIndexByValue);
  }

  @Test
  public void testCreateSingleValueByKey() {
    final Object result = propertyIndexByKey.createSingleValue(db, "tt");
    Assert.assertEquals(result, "tt");
  }

  @Test
  public void testCreateSingleValueByValue() {
    final Object result = propertyIndexByValue.createSingleValue(db, "12");
    Assert.assertEquals(result, 12);
  }

  @Test(expected = ODatabaseException.class)
  public void testCreateWrongSingleValueByValue() {
    propertyIndexByValue.createSingleValue(db, "tt");
  }

  @Test(expected = NullPointerException.class)
  public void testIndexByIsRequired() {
    new OPropertyMapIndexDefinition("testClass", "testField", OType.STRING, null);
  }

  @Test
  public void testCreateDDLByKey() {
    final String ddl =
        propertyIndexByKey
            .toCreateIndexDDL("testIndex", "unique", null)
            .toLowerCase(Locale.ENGLISH);
    Assert.assertEquals(ddl, "create index `testindex` on `testclass` ( `fone` by key ) unique");
  }

  @Test
  public void testCreateDDLByValue() {
    final String ddl =
        propertyIndexByValue
            .toCreateIndexDDL("testIndex", "unique", null)
            .toLowerCase(Locale.ENGLISH);
    Assert.assertEquals(ddl, "create index `testindex` on `testclass` ( `fone` by value ) unique");
  }

  @Test
  public void testProcessChangeEventAddKey() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, String> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, String>(
            OMultiValueChangeEvent.OChangeType.ADD, "key1", "value1");

    propertyIndexByKey.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    addedKeys.put("key1", 1);

    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventAddKeyWithConversion() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, String> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, String>(
            OMultiValueChangeEvent.OChangeType.ADD, "12", "value1");

    propertyIndexByIntegerKey.processChangeEvent(
        db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    addedKeys.put(12, 1);

    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventAddValue() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, Integer> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, Integer>(
            OMultiValueChangeEvent.OChangeType.ADD, "key1", 42);

    propertyIndexByValue.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    addedKeys.put(42, 1);

    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventAddValueWithConversion() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, String> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, String>(
            OMultiValueChangeEvent.OChangeType.ADD, "12", "42");

    propertyIndexByValue.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    addedKeys.put(42, 1);

    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventRemoveKey() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, String> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, String>(
            OMultiValueChangeEvent.OChangeType.REMOVE, "key1", "value1");

    propertyIndexByKey.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();
    removedKeys.put("key1", 1);

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventRemoveKeyWithConversion() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, String> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, String>(
            OMultiValueChangeEvent.OChangeType.REMOVE, "12", "value1");

    propertyIndexByIntegerKey.processChangeEvent(
        db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();

    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();
    removedKeys.put(12, 1);

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventRemoveValue() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, Integer> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, Integer>(
            OMultiValueChangeEvent.OChangeType.REMOVE, "key1", null, 42);

    propertyIndexByValue.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();
    removedKeys.put(42, 1);

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventRemoveValueWithConversion() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, String> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, String>(
            OMultiValueChangeEvent.OChangeType.REMOVE, "12", null, "42");

    propertyIndexByValue.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();
    removedKeys.put(42, 1);

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventUpdateKey() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, Integer> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, Integer>(
            OMultiValueChangeEvent.OChangeType.UPDATE, "key1", 42);

    propertyIndexByKey.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);
    Assert.assertTrue(keysToAdd.isEmpty());
    Assert.assertTrue(keysToRemove.isEmpty());
  }

  @Test
  public void testProcessChangeEventUpdateValue() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, Integer> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, Integer>(
            OMultiValueChangeEvent.OChangeType.UPDATE, "key1", 41, 42);

    propertyIndexByValue.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    addedKeys.put(41, 1);

    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();
    removedKeys.put(42, 1);

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }

  @Test
  public void testProcessChangeEventUpdateValueWithConversion() {
    final Object2IntOpenHashMap<Object> keysToAdd = new Object2IntOpenHashMap<>();
    keysToAdd.defaultReturnValue(-1);

    final Object2IntOpenHashMap<Object> keysToRemove = new Object2IntOpenHashMap<>();
    keysToRemove.defaultReturnValue(-1);

    final OMultiValueChangeEvent<String, String> multiValueChangeEvent =
        new OMultiValueChangeEvent<String, String>(
            OMultiValueChangeEvent.OChangeType.UPDATE, "12", "42", "41");

    propertyIndexByValue.processChangeEvent(db, multiValueChangeEvent, keysToAdd, keysToRemove);

    final Map<Object, Integer> addedKeys = new HashMap<Object, Integer>();
    addedKeys.put(42, 1);

    final Map<Object, Integer> removedKeys = new HashMap<Object, Integer>();
    removedKeys.put(41, 1);

    Assert.assertEquals(keysToAdd, addedKeys);
    Assert.assertEquals(keysToRemove, removedKeys);
  }
}
