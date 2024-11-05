/*
 * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentAbstract;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.iterator.ORecordIteratorCluster;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecordAbstract;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.core.serialization.serializer.record.string.ORecordSerializerSchemaAware2CSV;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@SuppressWarnings("groupsTestNG")
@Test(
    groups = {"crud", "record-vobject"},
    singleThreaded = true)
public class CRUDDocumentPhysicalTest extends DocumentDBBaseTest {

  private static final int TOT_RECORDS = 100;
  private static final int TOT_RECORDS_COMPANY = 10;

  private long startRecordNumber;
  private ODocument record;

  @Parameters(value = "remote")
  public CRUDDocumentPhysicalTest(boolean remote) {
    super(remote);
  }

  @Test
  public void cleanAll() {
    record = database.newInstance();
    if (!database.existsCluster("Account")) {
      database.getMetadata().getSchema().createClass("Account", 1, (OClass[]) null);
    }

    startRecordNumber = database.countClusterElements("Account");

    // DELETE ALL THE RECORDS IN THE CLUSTER
    while (database.countClusterElements("Account") > 0) {
      database.begin();
      for (ODocument rec : database.<ODocument>browseCluster("Account")) {
        if (rec != null) {
          rec.delete();
        }
      }
      database.commit();
    }

    Assert.assertEquals(database.countClusterElements("Account"), 0);

    if (!database.existsCluster("Company")) {
      database.getMetadata().getSchema().createClass("Company", 1, (OClass[]) null);
    }
  }

  @Test(dependsOnMethods = "cleanAll")
  public void create() {
    startRecordNumber = database.countClusterElements("Account");

    byte[] binary = new byte[100];
    for (int b = 0; b < binary.length; ++b) {
      binary[b] = (byte) b;
    }

    String base64 = Base64.getEncoder().encodeToString(binary);

    final int accountClusterId = database.getClusterIdByName("Account");

    for (long i = startRecordNumber; i < startRecordNumber + TOT_RECORDS; ++i) {
      database.begin();
      record = database.newInstance();

      record.setClassName("Account");
      record.field("id", i);
      record.field("name", "Gipsy");
      record.field("location", "Italy");
      record.field("salary", (i + 300f));
      record.field("binary", binary);
      record.field("nonSchemaBinary", binary);
      record.field("testLong", 10000000000L); // TEST LONG
      record.field("extra", "This is an extra field not included in the schema");
      record.field("value", (byte) 10);

      record.save();
      database.commit();
      Assert.assertEquals(record.getIdentity().getClusterId(), accountClusterId);
    }

    long startRecordNumberL = database.countClusterElements("Company");
    ODocument doc = new ODocument();

    for (long i = startRecordNumberL; i < startRecordNumberL + TOT_RECORDS_COMPANY; ++i) {
      database.begin();
      doc.setClassName("Company");
      doc.field("id", i);
      doc.field("name", "Microsoft" + i);
      doc.field("employees", (int) (100000 + i));
      database.save(doc);
      database.commit();

      doc = new ODocument();
    }
  }

  @Test(dependsOnMethods = "create")
  public void testCreate() {
    Assert.assertEquals(database.countClusterElements("Account") - startRecordNumber, TOT_RECORDS);
  }

  @Test(dependsOnMethods = "testCreate")
  public void readAndBrowseDescendingAndCheckHoleUtilization() {
    // BROWSE IN THE OPPOSITE ORDER
    byte[] binary;

    Set<Integer> ids = new HashSet<>();

    for (int i = 0; i < TOT_RECORDS; i++) {
      ids.add(i);
    }

    ORecordIteratorCluster<ODocument> it = database.browseCluster("Account");
    for (it.last(); it.hasPrevious(); ) {
      ODocument rec = it.previous();

      if (rec != null) {
        int id = ((Number) rec.field("id")).intValue();
        Assert.assertTrue(ids.remove(id));
        Assert.assertEquals(rec.field("name"), "Gipsy");
        Assert.assertEquals(rec.field("location"), "Italy");
        Assert.assertEquals(((Number) rec.field("testLong")).longValue(), 10000000000L);
        Assert.assertEquals(((Number) rec.field("salary")).intValue(), id + 300);
        Assert.assertNotNull(rec.field("extra"));
        Assert.assertEquals(((Byte) rec.field("value", Byte.class)).byteValue(), (byte) 10);

        binary = rec.field("binary", OType.BINARY);

        for (int b = 0; b < binary.length; ++b) {
          Assert.assertEquals(binary[b], (byte) b);
        }
      }
    }

    Assert.assertTrue(ids.isEmpty());
  }

  @Test(dependsOnMethods = "readAndBrowseDescendingAndCheckHoleUtilization")
  public void update() {
    int i = 0;
    for (ODocument rec : database.<ODocument>browseCluster("Account")) {

      if (i % 2 == 0) {
        rec.field("location", "Spain");
      }

      rec.field("price", i + 100);

      database.begin();
      rec.save();
      database.commit();

      i++;
    }
  }

  @Test(dependsOnMethods = "update")
  public void testUpdate() {
    for (ODocument rec : database.<ODocument>browseCluster("Account")) {
      int price = ((Number) rec.field("price")).intValue();
      Assert.assertTrue(price - 100 >= 0);

      if ((price - 100) % 2 == 0) {
        Assert.assertEquals(rec.field("location"), "Spain");
      } else {
        Assert.assertEquals(rec.field("location"), "Italy");
      }
    }
  }

  @Test(dependsOnMethods = "testUpdate")
  public void testDoubleChanges() {
    checkEmbeddedDB();

    database.begin();
    ODocument vDoc = database.newInstance();
    vDoc.setClassName("Profile");
    vDoc.field("nick", "JayM1").field("name", "Jay").field("surname", "Miner");
    vDoc.save();

    Assert.assertEquals(
        vDoc.getIdentity().getClusterId(), vDoc.getSchemaClass().getDefaultClusterId());

    vDoc = database.load(vDoc.getIdentity());
    vDoc.field("nick", "JayM2");
    vDoc.field("nick", "JayM3");
    vDoc.save();
    database.commit();

    @SuppressWarnings("deprecation")
    Set<OIndex> indexes =
        database.getMetadata().getSchema().getClass("Profile").getProperty("nick").getIndexes();

    Assert.assertEquals(indexes.size(), 1);

    OIndex indexDefinition = indexes.iterator().next();
    try (final Stream<ORID> stream = indexDefinition.getInternal().getRids("JayM1")) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    try (final Stream<ORID> stream = indexDefinition.getInternal().getRids("JayM2")) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    try (Stream<ORID> stream = indexDefinition.getInternal().getRids("JayM3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  @Test(dependsOnMethods = "testDoubleChanges")
  public void testMultiValues() {
    database.begin();
    ODocument vDoc = database.newInstance();
    vDoc.setClassName("Profile");
    vDoc.field("nick", "Jacky").field("name", "Jack").field("surname", "Tramiel");
    vDoc.save();

    // add a new record with the same name "nameA".
    vDoc = database.newInstance();
    vDoc.setClassName("Profile");
    vDoc.field("nick", "Jack").field("name", "Jack").field("surname", "Bauer");
    vDoc.save();
    database.commit();

    @SuppressWarnings("deprecation")
    Collection<OIndex> indexes =
        database.getMetadata().getSchema().getClass("Profile").getProperty("name").getIndexes();
    Assert.assertEquals(indexes.size(), 1);

    OIndex indexName = indexes.iterator().next();
    // We must get 2 records for "nameA".
    try (Stream<ORID> stream = indexName.getInternal().getRids("Jack")) {
      Assert.assertEquals(stream.count(), 2);
    }

    database.begin();
    // Remove this last record.
    database.delete(vDoc);
    database.commit();

    // We must get 1 record for "nameA".
    try (Stream<ORID> stream = indexName.getInternal().getRids("Jack")) {
      Assert.assertEquals(stream.count(), 1);
    }
  }

  @Test(dependsOnMethods = "testMultiValues")
  public void testUnderscoreField() {
    database.begin();
    ODocument vDoc = database.newInstance();
    vDoc.setClassName("Profile");
    vDoc.field("nick", "MostFamousJack")
        .field("name", "Kiefer")
        .field("surname", "Sutherland")
        .field("tag_list", new String[] {"actor", "myth"});
    vDoc.save();
    database.commit();

    @SuppressWarnings("deprecation")
    List<ODocument> result =
        database
            .command(
                new OSQLSynchQuery<ODocument>(
                    "select from Profile where name = 'Kiefer' and tag_list.size() > 0 "))
            .execute();

    Assert.assertEquals(result.size(), 1);
  }

  public void testLazyLoadingByLink() {
    database.begin();
    ODocument coreDoc = new ODocument();
    ODocument linkDoc = new ODocument();

    linkDoc.save(database.getClusterNameById(database.getDefaultClusterId()));
    coreDoc.field("link", linkDoc);
    coreDoc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ODocument coreDocCopy = database.load(coreDoc.getIdentity(), "*:-1", true);
    Assert.assertNotSame(coreDocCopy, coreDoc);

    coreDocCopy.setLazyLoad(false);
    Assert.assertTrue(coreDocCopy.field("link") instanceof ORecordId);
    coreDocCopy.setLazyLoad(true);
    Assert.assertTrue(coreDocCopy.field("link") instanceof ODocument);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDbCacheUpdated() {
    database.begin();
    ODocument vDoc = database.newInstance();
    vDoc.setClassName("Profile");

    Set<String> tags = new HashSet<>();
    tags.add("test");
    tags.add("yeah");

    vDoc.field("nick", "Dexter")
        .field("name", "Michael")
        .field("surname", "Hall")
        .field("tag_list", tags);
    vDoc.save();
    database.commit();

    @SuppressWarnings("deprecation")
    List<ODocument> result =
        database
            .command(new OSQLSynchQuery<ODocument>("select from Profile where name = 'Michael'"))
            .execute();

    Assert.assertEquals(result.size(), 1);
    ODocument dexter = result.get(0);

    database.begin();
    ((Collection<String>) dexter.field("tag_list")).add("actor");

    dexter.setDirty();
    dexter.save();
    database.commit();

    //noinspection deprecation
    result =
        database
            .command(
                new OSQLSynchQuery<ODocument>(
                    "select from Profile where tag_list contains 'actor' and tag_list contains"
                        + " 'test'"))
            .execute();
    Assert.assertEquals(result.size(), 1);
  }

  @Test(dependsOnMethods = "testUnderscoreField")
  public void testUpdateLazyDirtyPropagation() {
    for (ODocument rec : database.<ODocument>browseCluster("Profile")) {
      Assert.assertFalse(rec.isDirty());

      Collection<?> followers = rec.field("followers");
      if (followers != null && followers.size() > 0) {
        followers.remove(followers.iterator().next());
        Assert.assertTrue(rec.isDirty());
        break;
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNestedEmbeddedMap() {
    database.begin();
    ODocument newDoc = new ODocument();

    final Map<String, HashMap<?, ?>> map1 = new HashMap<>();
    newDoc.field("map1", map1, OType.EMBEDDEDMAP);

    final Map<String, HashMap<?, ?>> map2 = new HashMap<>();
    map1.put("map2", (HashMap<?, ?>) map2);

    final Map<String, HashMap<?, ?>> map3 = new HashMap<>();
    map2.put("map3", (HashMap<?, ?>) map3);

    final ORecordId rid =
        (ORecordId)
            newDoc.save(database.getClusterNameById(database.getDefaultClusterId())).getIdentity();
    database.commit();

    final ODocument loadedDoc = database.load(rid);

    Assert.assertTrue(newDoc.hasSameContentOf(loadedDoc));

    Assert.assertTrue(loadedDoc.containsField("map1"));
    Assert.assertTrue(loadedDoc.field("map1") instanceof Map<?, ?>);
    final Map<String, ODocument> loadedMap1 = loadedDoc.field("map1");
    Assert.assertEquals(loadedMap1.size(), 1);

    Assert.assertTrue(loadedMap1.containsKey("map2"));
    Assert.assertTrue(loadedMap1.get("map2") instanceof Map<?, ?>);
    final Map<String, ODocument> loadedMap2 = (Map<String, ODocument>) loadedMap1.get("map2");
    Assert.assertEquals(loadedMap2.size(), 1);

    Assert.assertTrue(loadedMap2.containsKey("map3"));
    Assert.assertTrue(loadedMap2.get("map3") instanceof Map<?, ?>);
    final Map<String, ODocument> loadedMap3 = (Map<String, ODocument>) loadedMap2.get("map3");
    Assert.assertEquals(loadedMap3.size(), 0);
  }

  @Test
  public void commandWithPositionalParameters() {
    final OSQLSynchQuery<ODocument> query =
        new OSQLSynchQuery<>("select from Profile where name = ? and surname = ?");
    @SuppressWarnings("deprecation")
    List<ODocument> result = database.command(query).execute("Barack", "Obama");

    Assert.assertTrue(result.size() != 0);
  }

  @Test
  public void queryWithPositionalParameters() {
    final OSQLSynchQuery<ODocument> query =
        new OSQLSynchQuery<>("select from Profile where name = ? and surname = ?");
    @SuppressWarnings("deprecation")
    List<ODocument> result = database.query(query, "Barack", "Obama");

    Assert.assertTrue(result.size() != 0);
  }

  @Test
  public void commandWithNamedParameters() {
    final OSQLSynchQuery<ODocument> query =
        new OSQLSynchQuery<>("select from Profile where name = :name and surname = :surname");

    HashMap<String, String> params = new HashMap<>();
    params.put("name", "Barack");
    params.put("surname", "Obama");

    @SuppressWarnings("deprecation")
    List<ODocument> result = database.command(query).execute(params);
    Assert.assertTrue(result.size() != 0);
  }

  @Test
  public void commandWrongParameterNames() {
    ODocument doc = database.newInstance();
    database.executeInTx(
        () -> {
          try {
            doc.field("a:b", 10);
            Assert.fail();
          } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
          }
        });

    database.executeInTx(
        () -> {
          try {
            doc.field("a,b", 10);
            Assert.fail();
          } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
          }
        });
  }

  @Test
  public void queryWithNamedParameters() {
    final OSQLSynchQuery<ODocument> query =
        new OSQLSynchQuery<>("select from Profile where name = :name and surname = :surname");

    HashMap<String, String> params = new HashMap<>();
    params.put("name", "Barack");
    params.put("surname", "Obama");

    @SuppressWarnings("deprecation")
    List<ODocument> result = database.query(query, params);

    Assert.assertTrue(result.size() != 0);
  }

  public void testJSONLinkd() {
    database.begin();
    ODocument jaimeDoc = new ODocument("PersonTest");
    jaimeDoc.field("name", "jaime");
    jaimeDoc.save();

    ODocument cerseiDoc = new ODocument("PersonTest");
    cerseiDoc.fromJSON(
        "{\"@type\":\"d\",\"name\":\"cersei\",\"valonqar\":" + jaimeDoc.toJSON() + "}");
    cerseiDoc.save();
    database.commit();

    database.begin();
    // The link between jamie and tyrion is not saved properly
    ODocument tyrionDoc = new ODocument("PersonTest");
    tyrionDoc.fromJSON(
        "{\"@type\":\"d\",\"name\":\"tyrion\",\"emergency_contact\":{\"relationship\":\"brother\",\"contact\":"
            + jaimeDoc.toJSON()
            + "}}");
    tyrionDoc.save();
    database.commit();

    for (ODocument o : database.browseClass("PersonTest")) {
      for (OIdentifiable id :
          new OSQLSynchQuery<ODocument>("traverse * from " + o.getIdentity().toString())) {
        database.load(id.getIdentity()).toJSON();
      }
    }
  }

  @Test
  public void testDirtyChild() {
    ODocument parent = new ODocument();

    ODocument child1 = new ODocument();
    ODocumentInternal.addOwner(child1, parent);
    parent.field("child1", child1);

    Assert.assertTrue(child1.hasOwners());

    ODocument child2 = new ODocument();
    ODocumentInternal.addOwner(child2, child1);
    child1.field("child2", child2);

    Assert.assertTrue(child2.hasOwners());

    // BEFORE FIRST TOSTREAM
    Assert.assertTrue(parent.isDirty());
    parent.toStream();
    // AFTER TOSTREAM
    Assert.assertTrue(parent.isDirty());
    // CHANGE FIELDS VALUE (Automaticaly set dirty this child)
    child1.field("child2", new ODocument());
    Assert.assertTrue(parent.isDirty());
  }

  @Test
  public void testInvalidFetchplanLoad() {
    database.begin();
    ODocument doc = database.newInstance();
    doc.field("test", "test");
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID docRid = doc.getIdentity().copy();

    try {
      // RELOAD THE DOCUMENT, THIS WILL PUT IT IN L1 CACHE
      doc = database.load(docRid, "*:-1");
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, docRid);
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(1, 0));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(1, 1));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(1, 2));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(2, 0));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(2, 1));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(2, 2));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(3, 0));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(3, 1));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(3, 2));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(4, 0));
      doc = testInvalidFetchPlanInvalidateL1Cache(doc, new ORecordId(4, 1));
      // CLOSE DB AND RE-TEST THE LOAD TO MAKE SURE
    } finally {
      database.close();
    }

    database = acquireSession();

    doc = testInvalidFetchPlanClearL1Cache(doc, docRid);
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(1, 0));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(1, 1));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(1, 2));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(2, 0));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(2, 1));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(2, 2));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(3, 0));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(3, 1));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(3, 2));
    doc = testInvalidFetchPlanClearL1Cache(doc, new ORecordId(4, 0));
    testInvalidFetchPlanClearL1Cache(doc, new ORecordId(4, 1));
    doc = database.load(docRid);

    database.begin();
    doc.delete();
    database.commit();
  }

  public void testEncoding() {
    String s = " \r\n\t:;,.|+*/\\=!?[]()'\"";

    database.begin();
    ODocument doc = new ODocument();
    doc.field("test", s);
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    Assert.assertEquals(doc.field("test"), s);
  }

  @Test
  public void polymorphicQuery() {
    database.begin();
    final ORecordAbstract newAccount =
        new ODocument("Account").field("name", "testInheritanceName").save();
    database.commit();

    @SuppressWarnings("deprecation")
    List<ODocument> superClassResult =
        database.query(new OSQLSynchQuery<ODocument>("select from Account"));
    @SuppressWarnings("deprecation")
    List<ODocument> subClassResult =
        database.query(new OSQLSynchQuery<ODocument>("select from Company"));

    Assert.assertTrue(superClassResult.size() != 0);
    Assert.assertTrue(subClassResult.size() != 0);
    Assert.assertTrue(superClassResult.size() >= subClassResult.size());

    // VERIFY ALL THE SUBCLASS RESULT ARE ALSO CONTAINED IN SUPERCLASS
    // RESULT
    for (ODocument d : subClassResult) {
      Assert.assertTrue(superClassResult.contains(d));
    }

    HashSet<ODocument> browsed = new HashSet<>();
    for (ODocument d : database.browseClass("Account")) {
      Assert.assertFalse(browsed.contains(d));
      browsed.add(d);
    }

    database.begin();
    newAccount.delete();
    database.commit();
  }

  @Test(dependsOnMethods = "testCreate")
  public void testBrowseClassHasNextTwice() {
    ODocument doc1 = null;
    //noinspection LoopStatementThatDoesntLoop
    for (Iterator<ODocument> itDoc = database.browseClass("Account"); itDoc.hasNext(); ) {
      doc1 = itDoc.next();
      break;
    }

    ODocument doc2 = null;
    //noinspection LoopStatementThatDoesntLoop
    for (Iterator<ODocument> itDoc = database.browseClass("Account"); itDoc.hasNext(); ) {
      //noinspection ResultOfMethodCallIgnored
      itDoc.hasNext();
      doc2 = itDoc.next();
      break;
    }

    Assert.assertEquals(doc1, doc2);
  }

  @Test(dependsOnMethods = "testCreate")
  public void nonPolymorphicQuery() {
    database.begin();
    final ORecordAbstract newAccount =
        new ODocument("Account").field("name", "testInheritanceName").save();
    database.commit();

    @SuppressWarnings("deprecation")
    List<ODocument> allResult =
        database.query(new OSQLSynchQuery<ODocument>("select from Account"));
    @SuppressWarnings("deprecation")
    List<ODocument> superClassResult =
        database.query(
            new OSQLSynchQuery<ODocument>("select from Account where @class = 'Account'"));
    @SuppressWarnings("deprecation")
    List<ODocument> subClassResult =
        database.query(
            new OSQLSynchQuery<ODocument>("select from Company where @class = 'Company'"));

    Assert.assertTrue(allResult.size() != 0);
    Assert.assertTrue(superClassResult.size() != 0);
    Assert.assertTrue(subClassResult.size() != 0);

    // VERIFY ALL THE SUBCLASS RESULT ARE NOT CONTAINED IN SUPERCLASS RESULT
    for (ODocument d : subClassResult) {
      Assert.assertFalse(superClassResult.contains(d));
    }

    HashSet<ODocument> browsed = new HashSet<>();
    for (ODocument d : database.browseClass("Account")) {
      Assert.assertFalse(browsed.contains(d));
      browsed.add(d);
    }

    database.begin();
    newAccount.delete();
    database.commit();
  }

  @Test(dependsOnMethods = "cleanAll")
  public void testEmbeddeDocumentInTx() {
    ODocument bank = database.newInstance("Account");
    database.begin();

    bank.field("Name", "MyBank");

    ODocument bank2 = database.newInstance("Account");
    bank.field("embedded", bank2, OType.EMBEDDED);
    bank.save();

    database.commit();

    database.close();

    database = acquireSession();

    database.begin();
    Assert.assertTrue(((ODocument) bank.field("embedded")).isEmbedded());
    Assert.assertFalse(((ODocument) bank.field("embedded")).getIdentity().isPersistent());
    database.rollback();

    database.begin();
    bank.delete();
    database.commit();
  }

  @Test(dependsOnMethods = "cleanAll")
  public void testUpdateInChain() {
    database.begin();
    ODocument bank = database.newInstance("Account");
    bank.field("name", "MyBankChained");

    // EMBEDDED
    ODocument embedded = database.<ODocument>newInstance("Account").field("name", "embedded1");
    bank.field("embedded", embedded, OType.EMBEDDED);

    ODocument[] embeddeds =
        new ODocument[] {
          database.<ODocument>newInstance("Account").field("name", "embedded2"),
          database.<ODocument>newInstance("Account").field("name", "embedded3")
        };
    bank.field("embeddeds", embeddeds, OType.EMBEDDEDLIST);

    // LINKED
    ODocument linked = database.<ODocument>newInstance("Account").field("name", "linked1");
    bank.field("linked", linked);

    ODocument[] linkeds =
        new ODocument[] {
          database.<ODocument>newInstance("Account").field("name", "linked2"),
          database.<ODocument>newInstance("Account").field("name", "linked3")
        };
    bank.field("linkeds", linkeds, OType.LINKLIST);

    bank.save();
    database.commit();

    database.close();
    database = acquireSession();

    database.executeInTx(
        () -> {
          ODocument changedDoc1 = bank.field("embedded.total", 100);
          // MUST CHANGE THE PARENT DOC BECAUSE IT'S EMBEDDED
          Assert.assertEquals(changedDoc1.field("name"), "MyBankChained");
          Assert.assertEquals(changedDoc1.<Object>field("embedded.total"), 100);

          ODocument changedDoc2 = bank.field("embeddeds.total", 200);
          // MUST CHANGE THE PARENT DOC BECAUSE IT'S EMBEDDED
          Assert.assertEquals(changedDoc2.field("name"), "MyBankChained");

          Collection<Integer> intEmbeddeds = changedDoc2.field("embeddeds.total");
          for (Integer e : intEmbeddeds) {
            Assert.assertEquals(e.intValue(), 200);
          }

          ODocument changedDoc3 = bank.field("linked.total", 300);
          // MUST CHANGE THE LINKED DOCUMENT
          Assert.assertEquals(changedDoc3.field("name"), "linked1");
          Assert.assertEquals(changedDoc3.<Object>field("total"), 300);

          try {
            bank.field("linkeds.total", 400);
            Assert.fail();
          } catch (IllegalArgumentException ignored) {
          }
        });

    database.begin();
    ((ODocument) bank.field("linked")).delete();
    //noinspection unchecked
    for (ODocument l : (Collection<ODocument>) bank.field("linkeds")) {
      l.delete();
    }
    bank.delete();
    database.commit();
  }

  public void testSerialization() {
    @SuppressWarnings("deprecation")
    ORecordSerializer current = ODatabaseDocumentAbstract.getDefaultSerializer();
    //noinspection deprecation
    ODatabaseDocumentAbstract.setDefaultSerializer(ORecordSerializerSchemaAware2CSV.INSTANCE);
    ODatabaseSessionInternal oldDb = ODatabaseRecordThreadLocal.instance().get();
    ORecordSerializer dbser = oldDb.getSerializer();
    oldDb.setSerializer(ORecordSerializerSchemaAware2CSV.INSTANCE);
    final byte[] streamOrigin =
        "Account@html:{\"path\":\"html/layout\"},config:{\"title\":\"Github Admin\",\"modules\":(githubDisplay:\"github_display\")},complex:(simple1:\"string1\",one_level1:(simple2:\"string2\"),two_levels:(simple3:\"string3\",one_level2:(simple4:\"string4\")))"
            .getBytes();
    ODocument doc =
        (ODocument)
            ORecordSerializerSchemaAware2CSV.INSTANCE.fromStream(
                streamOrigin, new ODocument(), null);
    doc.field("out");
    final byte[] streamDest = ORecordSerializerSchemaAware2CSV.INSTANCE.toStream(doc);
    Assert.assertEquals(streamOrigin, streamDest);
    //noinspection deprecation
    ODatabaseDocumentAbstract.setDefaultSerializer(current);
    oldDb.setSerializer(dbser);
  }

  public void testUpdateNoVersionCheck() {
    @SuppressWarnings("deprecation")
    List<ODocument> result = database.query(new OSQLSynchQuery<ODocument>("select from Account"));

    database.begin();
    ODocument doc = result.get(0);
    doc.field("name", "modified");
    int oldVersion = doc.getVersion();

    ORecordInternal.setVersion(doc, -2);

    doc.save();
    database.commit();

    database.executeInTx(
        () -> {
          Assert.assertEquals(doc.getVersion(), oldVersion);
          Assert.assertEquals(doc.field("name"), "modified");
        });
  }

  public void testCreateEmbddedClassDocument() {
    final OSchema schema = database.getMetadata().getSchema();
    final String SUFFIX = "TESTCLUSTER1";

    OClass testClass1 = schema.createClass("testCreateEmbddedClass1");
    OClass testClass2 = schema.createClass("testCreateEmbddedClass2");
    testClass2.createProperty("testClass1Property", OType.EMBEDDED, testClass1);

    int clusterId = database.addCluster("testCreateEmbddedClass2" + SUFFIX);
    schema.getClass("testCreateEmbddedClass2").addClusterId(clusterId);

    testClass1 = schema.getClass("testCreateEmbddedClass1");
    testClass2 = schema.getClass("testCreateEmbddedClass2");

    database.begin();
    ODocument testClass2Document = new ODocument(testClass2);
    testClass2Document.field("testClass1Property", new ODocument(testClass1));
    testClass2Document.save("testCreateEmbddedClass2" + SUFFIX);
    database.commit();

    testClass2Document = database.load(testClass2Document.getIdentity(), "*:-1", true);
    Assert.assertNotNull(testClass2Document);

    Assert.assertEquals(testClass2Document.getSchemaClass(), testClass2);

    ODocument embeddedDoc = testClass2Document.field("testClass1Property");
    Assert.assertEquals(embeddedDoc.getSchemaClass(), testClass1);
  }

  public void testRemoveAllLinkList() {
    final ODocument doc = new ODocument();

    final List<ODocument> allDocs = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      database.begin();
      final ODocument linkDoc = new ODocument();
      linkDoc.save(database.getClusterNameById(database.getDefaultClusterId()));
      database.commit();

      allDocs.add(linkDoc);
    }

    database.begin();
    doc.field("linkList", allDocs);
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    final List<ODocument> docsToRemove = new ArrayList<>(allDocs.size() / 2);
    for (int i = 0; i < 5; i++) {
      docsToRemove.add(allDocs.get(i));
    }

    List<OIdentifiable> linkList = doc.field("linkList");
    linkList.removeAll(docsToRemove);

    Assert.assertEquals(linkList.size(), 5);

    for (int i = 5; i < 10; i++) {
      Assert.assertEquals(linkList.get(i - 5), allDocs.get(i));
    }
    doc.save();
    database.commit();

    database.begin();
    doc.save();
    database.commit();

    database.begin();
    linkList = doc.field("linkList");
    Assert.assertEquals(linkList.size(), 5);

    for (int i = 5; i < 10; i++) {
      Assert.assertEquals(linkList.get(i - 5), allDocs.get(i));
    }
    database.commit();
  }

  public void testRemoveAndReload() {
    ODocument doc1;

    database.begin();
    {
      doc1 = new ODocument();
      doc1.save(database.getClusterNameById(database.getDefaultClusterId()));
    }
    database.commit();

    database.begin();
    {
      database.delete(doc1);
    }
    database.commit();

    database.begin();
    {
      ODocument deletedDoc = database.load(doc1.getIdentity());
      Assert.assertNull(deletedDoc); // OK!
    }
    database.commit();
  }

  private ODocument testInvalidFetchPlanInvalidateL1Cache(ODocument doc, ORID docRid) {
    try {
      // LOAD DOCUMENT, CHECK BEFORE GETTING IT FROM L1 CACHE
      doc = database.load(docRid, "invalid");
      Assert.fail("Should throw IllegalArgumentException");
    } catch (Exception ignored) {
    }
    // INVALIDATE L1 CACHE TO CHECK THE L2 CACHE
    database.getLocalCache().invalidate();
    try {
      // LOAD DOCUMENT, CHECK BEFORE GETTING IT FROM L2 CACHE
      doc = database.load(docRid, "invalid");
      Assert.fail("Should throw IllegalArgumentException");
    } catch (Exception ignored) {
    }
    // CLEAR THE L2 CACHE TO CHECK THE RAW READ
    try {
      // LOAD DOCUMENT NOT IN ANY CACHE
      doc = database.load(docRid, "invalid");
      Assert.fail("Should throw IllegalArgumentException");
    } catch (Exception ignored) {
    }
    return doc;
  }

  private ODocument testInvalidFetchPlanClearL1Cache(ODocument doc, ORID docRid) {
    try {
      // LOAD DOCUMENT NOT IN ANY CACHE
      doc = database.load(docRid, "invalid");
      Assert.fail("Should throw IllegalArgumentException");
    } catch (Exception ignored) {
    }
    // LOAD DOCUMENT, THIS WILL PUT IT IN L1 CACHE
    try {
      database.load(docRid);
    } catch (Exception ignored) {
    }
    try {
      // LOAD DOCUMENT, CHECK BEFORE GETTING IT FROM L1 CACHE
      doc = database.load(docRid, "invalid");
      Assert.fail("Should throw IllegalArgumentException");
    } catch (Exception ignored) {
    }
    // CLEAR L1 CACHE, THIS WILL PUT IT IN L2 CACHE
    database.getLocalCache().clear();
    try {
      // LOAD DOCUMENT, CHECK BEFORE GETTING IT FROM L2 CACHE
      doc = database.load(docRid, "invalid");
      Assert.fail("Should throw IllegalArgumentException");
    } catch (Exception ignored) {
    }

    try {
      // LOAD DOCUMENT NOT IN ANY CACHE
      doc = database.load(docRid, "invalid");
      Assert.fail("Should throw IllegalArgumentException");
    } catch (Exception ignored) {
    }
    return doc;
  }

  @Test
  public void testAny() {
    database.command("create class TestExport").close();
    database.command("create property TestExport.anything ANY").close();

    database.begin();
    database.command("insert into TestExport set anything = 3").close();
    database.command("insert into TestExport set anything = 'Jay'").close();
    database.command("insert into TestExport set anything = 2.3").close();
    database.commit();

    OResultSet result = database.command("select count(*) from TestExport where anything = 3");
    Assert.assertNotNull(result);
    Assert.assertEquals(result.stream().count(), 1);

    result = database.command("select count(*) from TestExport where anything = 'Jay'");
    Assert.assertNotNull(result);
    Assert.assertEquals(result.stream().count(), 1);

    result = database.command("select count(*) from TestExport where anything = 2.3");
    Assert.assertNotNull(result);
    Assert.assertEquals(result.stream().count(), 1);
  }
}
