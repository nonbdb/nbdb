package com.orientechnologies.orient.core.record.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.db.document.YTDatabaseSessionAbstract;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.core.serialization.serializer.record.string.ORecordSerializerSchemaAware2CSV;
import com.orientechnologies.orient.core.serialization.serializer.record.string.ORecordSerializerStringAbstract;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ODocumentSchemalessSerializationTest extends DBTestBase {

  protected ORecordSerializer serializer;
  private ORecordSerializer defaultSerializer;

  @Before
  public void before() {
    serializer = new ORecordSerializerSchemaAware2CSV();
    defaultSerializer = YTDatabaseSessionAbstract.getDefaultSerializer();
    YTDatabaseSessionAbstract.setDefaultSerializer(serializer);
  }

  @After
  public void after() {
    YTDatabaseSessionAbstract.setDefaultSerializer(defaultSerializer);
  }

  @Test
  public void testSimpleSerialization() {
    YTDocument document = new YTDocument();

    document.field("name", "name");
    document.field("age", 20);
    document.field("youngAge", (short) 20);
    document.field("oldAge", (long) 20);
    document.field("heigth", 12.5f);
    document.field("bitHeigth", 12.5d);
    document.field("class", (byte) 'C');
    document.field("character", 'C');
    document.field("alive", true);
    document.field("date", new Date());
    document.field("recordId", new YTRecordId(10, 10));

    byte[] res = serializer.toStream(db, document);
    YTDocument extr = (YTDocument) serializer.fromStream(db, res, new YTDocument(), new String[]{});

    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<String>field("name"), document.field("name"));
    assertEquals(extr.<String>field("age"), document.field("age"));
    assertEquals(extr.<String>field("youngAge"), document.field("youngAge"));
    assertEquals(extr.<String>field("oldAge"), document.field("oldAge"));
    assertEquals(extr.<String>field("heigth"), document.field("heigth"));
    assertEquals(extr.<String>field("bitHeigth"), document.field("bitHeigth"));
    assertEquals(extr.<String>field("class"), document.field("class"));
    // TODO fix char management issue:#2427
    // assertEquals(document.field("character"), extr.field("character"));
    assertEquals(extr.<String>field("alive"), document.field("alive"));
    assertEquals(extr.<String>field("date"), document.field("date"));
    // assertEquals(extr.field("recordId"), document.field("recordId"));

  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void testSimpleLiteralList() {

    YTDocument document = new YTDocument();
    List<String> strings = new ArrayList<String>();
    strings.add("a");
    strings.add("b");
    strings.add("c");
    document.field("listStrings", strings);

    List<Short> shorts = new ArrayList<Short>();
    shorts.add((short) 1);
    shorts.add((short) 2);
    shorts.add((short) 3);
    document.field("shorts", shorts);

    List<Long> longs = new ArrayList<Long>();
    longs.add((long) 1);
    longs.add((long) 2);
    longs.add((long) 3);
    document.field("longs", longs);

    List<Integer> ints = new ArrayList<Integer>();
    ints.add(1);
    ints.add(2);
    ints.add(3);
    document.field("integers", ints);

    List<Float> floats = new ArrayList<Float>();
    floats.add(1.1f);
    floats.add(2.2f);
    floats.add(3.3f);
    document.field("floats", floats);

    List<Double> doubles = new ArrayList<Double>();
    doubles.add(1.1);
    doubles.add(2.2);
    doubles.add(3.3);
    document.field("doubles", doubles);

    List<Date> dates = new ArrayList<Date>();
    dates.add(new Date());
    dates.add(new Date());
    dates.add(new Date());
    document.field("dates", dates);

    List<Byte> bytes = new ArrayList<Byte>();
    bytes.add((byte) 0);
    bytes.add((byte) 1);
    bytes.add((byte) 3);
    document.field("bytes", bytes);

    // TODO: char not currently supported in orient.
    List<Character> chars = new ArrayList<Character>();
    chars.add('A');
    chars.add('B');
    chars.add('C');
    // document.field("chars", chars);

    List<Boolean> booleans = new ArrayList<Boolean>();
    booleans.add(true);
    booleans.add(false);
    booleans.add(false);
    document.field("booleans", booleans);

    List listMixed = new ArrayList();
    listMixed.add(true);
    listMixed.add(1);
    listMixed.add((long) 5);
    listMixed.add((short) 2);
    listMixed.add(4.0f);
    listMixed.add(7.0D);
    listMixed.add("hello");
    listMixed.add(new Date());
    listMixed.add((byte) 10);
    document.field("listMixed", listMixed);

    byte[] res = serializer.toStream(db, document);
    YTDocument extr = (YTDocument) serializer.fromStream(db, res, new YTDocument(), new String[]{});

    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<String>field("listStrings"), document.field("listStrings"));
    assertEquals(extr.<String>field("integers"), document.field("integers"));
    assertEquals(extr.<String>field("doubles"), document.field("doubles"));
    assertEquals(extr.<String>field("dates"), document.field("dates"));
    assertEquals(extr.<String>field("bytes"), document.field("bytes"));
    assertEquals(extr.<String>field("booleans"), document.field("booleans"));
    assertEquals(extr.<String>field("listMixed"), document.field("listMixed"));
  }

  @Test
  public void testSimpleMapStringLiteral() {
    YTDocument document = new YTDocument();

    Map<String, String> mapString = new HashMap<String, String>();
    mapString.put("key", "value");
    mapString.put("key1", "value1");
    document.field("mapString", mapString);

    Map<String, Integer> mapInt = new HashMap<String, Integer>();
    mapInt.put("key", 2);
    mapInt.put("key1", 3);
    document.field("mapInt", mapInt);

    Map<String, Long> mapLong = new HashMap<String, Long>();
    mapLong.put("key", 2L);
    mapLong.put("key1", 3L);
    document.field("mapLong", mapLong);

    Map<String, Short> shortMap = new HashMap<String, Short>();
    shortMap.put("key", (short) 2);
    shortMap.put("key1", (short) 3);
    document.field("shortMap", shortMap);

    Map<String, Date> dateMap = new HashMap<String, Date>();
    dateMap.put("key", new Date());
    dateMap.put("key1", new Date());
    document.field("dateMap", dateMap);

    Map<String, Float> floatMap = new HashMap<String, Float>();
    floatMap.put("key", 10f);
    floatMap.put("key1", 11f);
    document.field("floatMap", floatMap);

    Map<String, Double> doubleMap = new HashMap<String, Double>();
    doubleMap.put("key", 10d);
    doubleMap.put("key1", 11d);
    document.field("doubleMap", doubleMap);

    Map<String, Byte> bytesMap = new HashMap<String, Byte>();
    bytesMap.put("key", (byte) 10);
    bytesMap.put("key1", (byte) 11);
    document.field("bytesMap", bytesMap);

    byte[] res = serializer.toStream(db, document);
    YTDocument extr = (YTDocument) serializer.fromStream(db, res, new YTDocument(), new String[]{});
    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<String>field("mapString"), document.field("mapString"));
    assertEquals(extr.<String>field("mapLong"), document.field("mapLong"));
    assertEquals(extr.<String>field("shortMap"), document.field("shortMap"));
    assertEquals(extr.<String>field("dateMap"), document.field("dateMap"));
    assertEquals(extr.<String>field("doubleMap"), document.field("doubleMap"));
    assertEquals(extr.<String>field("bytesMap"), document.field("bytesMap"));
  }

  @Test
  public void testSimpleEmbeddedDoc() {
    YTDocument document = new YTDocument();
    YTDocument embedded = new YTDocument();
    embedded.field("name", "test");
    embedded.field("surname", "something");
    document.field("embed", embedded, YTType.EMBEDDED);

    byte[] res = serializer.toStream(db, document);
    YTDocument extr = (YTDocument) serializer.fromStream(db, res, new YTDocument(), new String[]{});
    assertEquals(document.fields(), extr.fields());
    YTDocument emb = extr.field("embed");
    assertNotNull(emb);
    assertEquals(emb.<String>field("name"), embedded.field("name"));
    assertEquals(emb.<String>field("surname"), embedded.field("surname"));
  }

  @Test
  public void testMapOfEmbeddedDocument() {

    YTDocument document = new YTDocument();

    YTDocument embeddedInMap = new YTDocument();
    embeddedInMap.field("name", "test");
    embeddedInMap.field("surname", "something");
    Map<String, YTDocument> map = new HashMap<String, YTDocument>();
    map.put("embedded", embeddedInMap);
    document.field("map", map, YTType.EMBEDDEDMAP);

    byte[] res = serializer.toStream(db, document);
    YTDocument extr = (YTDocument) serializer.fromStream(db, res, new YTDocument(), new String[]{});
    Map<String, YTDocument> mapS = extr.field("map");
    assertEquals(1, mapS.size());
    YTDocument emb = mapS.get("embedded");
    assertNotNull(emb);
    assertEquals(emb.<String>field("name"), embeddedInMap.field("name"));
    assertEquals(emb.<String>field("surname"), embeddedInMap.field("surname"));
  }

  @Test
  public void testCollectionOfEmbeddedDocument() {

    YTDocument document = new YTDocument();

    YTDocument embeddedInList = new YTDocument();
    embeddedInList.field("name", "test");
    embeddedInList.field("surname", "something");

    List<YTDocument> embeddedList = new ArrayList<YTDocument>();
    embeddedList.add(embeddedInList);
    document.field("embeddedList", embeddedList, YTType.EMBEDDEDLIST);

    YTDocument embeddedInSet = new YTDocument();
    embeddedInSet.field("name", "test1");
    embeddedInSet.field("surname", "something2");

    Set<YTDocument> embeddedSet = new HashSet<YTDocument>();
    embeddedSet.add(embeddedInSet);
    document.field("embeddedSet", embeddedSet, YTType.EMBEDDEDSET);

    byte[] res = serializer.toStream(db, document);
    YTDocument extr = (YTDocument) serializer.fromStream(db, res, new YTDocument(), new String[]{});

    List<YTDocument> ser = extr.field("embeddedList");
    assertEquals(1, ser.size());
    YTDocument inList = ser.get(0);
    assertNotNull(inList);
    assertEquals(inList.<String>field("name"), embeddedInList.field("name"));
    assertEquals(inList.<String>field("surname"), embeddedInList.field("surname"));

    Set<YTDocument> setEmb = extr.field("embeddedSet");
    assertEquals(1, setEmb.size());
    YTDocument inSet = setEmb.iterator().next();
    assertNotNull(inSet);
    assertEquals(inSet.<String>field("name"), embeddedInSet.field("name"));
    assertEquals(inSet.<String>field("surname"), embeddedInSet.field("surname"));
  }

  @Test
  @Ignore
  public void testCsvGetTypeByValue() {
    Object res = ORecordSerializerStringAbstract.getTypeValue(db, "-");
    assertTrue(res instanceof String);
    res = ORecordSerializerStringAbstract.getTypeValue(db, "-email-@gmail.com");
    assertTrue(res instanceof String);
  }
}
