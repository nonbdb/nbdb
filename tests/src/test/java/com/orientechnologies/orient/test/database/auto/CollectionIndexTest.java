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

import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class CollectionIndexTest extends DocumentDBBaseTest {

  @Parameters(value = "remote")
  public CollectionIndexTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  public void setupSchema() {
    if (database.getMetadata().getSchema().existsClass("Collector")) {
      database.getMetadata().getSchema().dropClass("Collector");
    }
    final OClass collector = database.createClass("Collector");
    collector.createProperty("id", OType.STRING);
    collector
        .createProperty("stringCollection", OType.EMBEDDEDLIST, OType.STRING)
        .createIndex(OClass.INDEX_TYPE.NOTUNIQUE);
  }

  @AfterMethod
  public void afterMethod() throws Exception {
    database.begin();
    database.command("delete from Collector").close();
    database.commit();

    super.afterMethod();
  }

  public void testIndexCollection() {
    checkEmbeddedDB();

    database.begin();
    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));

    database.save(collector);
    database.commit();

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionInTx() {
    checkEmbeddedDB();

    try {
      database.begin();
      OElement collector = database.newElement("Collector");
      collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
      database.save(collector);
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();
      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdate() {
    checkEmbeddedDB();

    database.begin();
    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
    collector = database.save(collector);
    collector.setProperty("stringCollection", Arrays.asList("spam", "bacon"));
    database.save(collector);
    database.commit();

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("bacon")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateInTx() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
    database.begin();
    collector = database.save(collector);
    database.commit();
    try {
      database.begin();
      collector = database.bindToSession(collector);
      collector.setProperty("stringCollection", Arrays.asList("spam", "bacon"));
      database.save(collector);
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final OIndex index = getIndex("Collector.stringCollection");

    Assert.assertEquals(index.getInternal().size(), 2);
    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("bacon")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
    collector = database.save(collector);
    database.commit();

    database.begin();
    collector = database.bindToSession(collector);
    collector.setProperty("stringCollection", Arrays.asList("spam", "bacon"));
    database.save(collector);
    database.rollback();

    final OIndex index = getIndex("Collector.stringCollection");

    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateAddItem() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));

    database.begin();
    collector = database.save(collector);
    database.commit();

    database.begin();
    database
        .command(
            "UPDATE "
                + collector.getIdentity()
                + " set stringCollection = stringCollection || 'cookies'")
        .close();
    database.commit();

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 3);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs") && !key.equals("cookies")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateAddItemInTx() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", new ArrayList<>(Arrays.asList("spam", "eggs")));
    database.begin();
    collector = database.save(collector);
    database.commit();

    try {
      database.begin();
      OElement loadedCollector = database.load(collector.getIdentity());
      loadedCollector.<List<String>>getProperty("stringCollection").add("cookies");
      database.save(loadedCollector);
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final OIndex index = getIndex("Collector.stringCollection");

    Assert.assertEquals(index.getInternal().size(), 3);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs") && !key.equals("cookies")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateAddItemInTxRollback() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", new ArrayList<>(Arrays.asList("spam", "eggs")));
    database.begin();
    collector = database.save(collector);
    database.commit();

    database.begin();
    OElement loadedCollector = database.load(collector.getIdentity());
    loadedCollector.<List<String>>getProperty("stringCollection").add("cookies");
    database.save(loadedCollector);
    database.rollback();

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateRemoveItemInTx() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", new ArrayList<>(Arrays.asList("spam", "eggs")));
    database.begin();
    collector = database.save(collector);
    database.commit();

    try {
      database.begin();
      OElement loadedCollector = database.load(collector.getIdentity());
      loadedCollector.<List<String>>getProperty("stringCollection").remove("spam");
      database.save(loadedCollector);
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 1);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateRemoveItemInTxRollback() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", new ArrayList<>(Arrays.asList("spam", "eggs")));
    database.begin();
    collector = database.save(collector);
    database.commit();

    database.begin();
    OElement loadedCollector = database.load(collector.getIdentity());
    loadedCollector.<List<String>>getProperty("stringCollection").remove("spam");
    database.save(loadedCollector);
    database.rollback();

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionUpdateRemoveItem() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
    database.begin();
    collector = database.save(collector);
    database.commit();

    database.begin();
    database
        .command("UPDATE " + collector.getIdentity() + " remove stringCollection = 'spam'")
        .close();
    database.commit();

    final OIndex index = getIndex("Collector.stringCollection");

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionRemove() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
    database.begin();
    collector = database.save(collector);
    database.delete(collector);
    database.commit();

    final OIndex index = getIndex("Collector.stringCollection");

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testIndexCollectionRemoveInTx() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
    database.begin();
    collector = database.save(collector);
    database.commit();
    try {
      database.begin();
      database.delete(database.bindToSession(collector));
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final OIndex index = getIndex("Collector.stringCollection");

    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testIndexCollectionRemoveInTxRollback() {
    checkEmbeddedDB();

    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));
    database.begin();
    collector = database.save(collector);
    database.commit();

    database.begin();
    database.delete(database.bindToSession(collector));
    database.rollback();

    final OIndex index = getIndex("Collector.stringCollection");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        String key = (String) keysIterator.next();
        if (!key.equals("spam") && !key.equals("eggs")) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexCollectionSQL() {
    OElement collector = database.newElement("Collector");
    collector.setProperty("stringCollection", Arrays.asList("spam", "eggs"));

    database.begin();
    database.save(collector);
    database.commit();

    List<ODocument> result =
        executeQuery("select * from Collector where stringCollection contains ?", "eggs");
    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(
        Arrays.asList("spam", "eggs"), result.get(0).getProperty("stringCollection"));
  }
}
