package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * @author Andrey Lomakin (a.lomakin-at-orientdb.com)
 * @since 3/28/14
 */
@SuppressWarnings("deprecation")
public class LinkSetIndexTest extends DocumentDBBaseTest {

  @Parameters(value = "remote")
  public LinkSetIndexTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  public void setupSchema() {
    final OClass ridBagIndexTestClass =
        database.getMetadata().getSchema().createClass("LinkSetIndexTestClass");

    ridBagIndexTestClass.createProperty("linkSet", OType.LINKSET);

    ridBagIndexTestClass.createIndex("linkSetIndex", OClass.INDEX_TYPE.NOTUNIQUE, "linkSet");
    database.close();
  }

  @BeforeMethod
  public void beforeMethod() {
    database = createSessionInstance();
  }

  @AfterMethod
  public void afterMethod() {
    checkEmbeddedDB();

    database.begin();
    database.command("DELETE FROM LinkSetIndexTestClass").close();
    database.commit();

    OResultSet result = database.command("select from LinkSetIndexTestClass");
    Assert.assertEquals(result.stream().count(), 0);

    if (database.getStorage().isRemote()) {
      OIndex index =
          database.getMetadata().getIndexManagerInternal().getIndex(database, "linkSetIndex");
      Assert.assertEquals(index.getInternal().size(), 0);
    }

    database.close();
  }

  public void testIndexLinkSet() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetInTx() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    try {
      database.begin();
      final ODocument document = new ODocument("LinkSetIndexTestClass");
      final Set<OIdentifiable> linkSet = new HashSet<>();
      linkSet.add(database.bindToSession(docOne));
      linkSet.add(database.bindToSession(docTwo));

      document.field("linkSet", linkSet);
      document.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdate() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docThree = new ODocument();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSetOne = new HashSet<>();
    linkSetOne.add(docOne);
    linkSetOne.add(docTwo);

    document.field("linkSet", linkSetOne);
    document.save();

    final Set<OIdentifiable> linkSetTwo = new HashSet<>();
    linkSetTwo.add(docOne);
    linkSetTwo.add(docThree);

    document.field("linkSet", linkSetTwo);
    document.save();
    database.commit();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateInTx() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docThree = new ODocument();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSetOne = new HashSet<>();
    linkSetOne.add(docOne);
    linkSetOne.add(docTwo);

    document.field("linkSet", linkSetOne);
    document.save();
    database.commit();

    try {
      database.begin();

      document = database.bindToSession(document);
      final Set<OIdentifiable> linkSetTwo = new HashSet<>();
      linkSetTwo.add(database.bindToSession(docOne));
      linkSetTwo.add(database.bindToSession(docThree));

      document.field("linkSet", linkSetTwo);
      document.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docThree = new ODocument();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final Set<OIdentifiable> linkSetOne = new HashSet<>();
    linkSetOne.add(docOne);
    linkSetOne.add(docTwo);

    ODocument document = new ODocument("LinkSetIndexTestClass");
    document.field("linkSet", linkSetOne);
    document.save();
    database.commit();

    database.begin();

    document = database.bindToSession(document);
    final Set<OIdentifiable> linkSetTwo = new HashSet<>();
    linkSetTwo.add(database.bindToSession(docOne));
    linkSetTwo.add(database.bindToSession(docThree));

    document.field("linkSet", linkSetTwo);
    document.save();
    database.rollback();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateAddItem() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docThree = new ODocument();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);
    document.field("linkSet", linkSet);

    document.save();
    database.commit();

    database.begin();
    database
        .command(
            "UPDATE "
                + document.getIdentity()
                + " set linkSet = linkSet || "
                + docThree.getIdentity())
        .close();
    database.commit();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 3);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateAddItemInTx() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save();

    final ODocument docTwo = new ODocument();
    docTwo.save();

    final ODocument docThree = new ODocument();
    docThree.save();

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    try {
      database.begin();
      ODocument loadedDocument = database.load(document.getIdentity());
      loadedDocument.<Set<OIdentifiable>>field("linkSet").add(database.bindToSession(docThree));
      loadedDocument.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 3);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateAddItemInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docThree = new ODocument();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    database.begin();
    ODocument loadedDocument = database.load(document.getIdentity());
    loadedDocument.<Set<OIdentifiable>>field("linkSet").add(database.bindToSession(docThree));
    loadedDocument.save();
    database.rollback();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateRemoveItemInTx() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);
    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    try {
      database.begin();
      ODocument loadedDocument = database.load(document.getIdentity());
      loadedDocument.<Set<OIdentifiable>>field("linkSet").remove(docTwo);
      loadedDocument.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 1);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateRemoveItemInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);
    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    database.begin();
    ODocument loadedDocument = database.load(document.getIdentity());
    loadedDocument.<Set<OIdentifiable>>field("linkSet").remove(docTwo);
    loadedDocument.save();
    database.rollback();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetUpdateRemoveItem() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    database.begin();
    database
        .command("UPDATE " + document.getIdentity() + " remove linkSet = " + docTwo.getIdentity())
        .close();
    database.commit();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 1);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetRemove() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");

    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    database.begin();
    database.bindToSession(document).delete();
    database.commit();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testIndexLinkSetRemoveInTx() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");

    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docOne);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    try {
      database.begin();
      database.bindToSession(document).delete();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 0);
  }

  public void testIndexLinkSetRemoveInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();

    linkSet.add(docOne);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    database.begin();
    database.bindToSession(document).delete();
    database.rollback();

    OIndex index = getIndex("linkSetIndex");
    Assert.assertEquals(index.getInternal().size(), 2);

    Iterator<Object> keysIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keysIterator = keyStream.iterator();

      while (keysIterator.hasNext()) {
        OIdentifiable key = (OIdentifiable) keysIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexLinkSetSQL() {
    checkEmbeddedDB();

    database.begin();
    final ODocument docOne = new ODocument();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docTwo = new ODocument();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final ODocument docThree = new ODocument();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    ODocument document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSetOne = new HashSet<>();
    linkSetOne.add(docOne);
    linkSetOne.add(docTwo);

    document.field("linkSet", linkSetOne);
    document.save();

    document = new ODocument("LinkSetIndexTestClass");
    final Set<OIdentifiable> linkSet = new HashSet<>();
    linkSet.add(docThree);
    linkSet.add(docTwo);

    document.field("linkSet", linkSet);
    document.save();
    database.commit();

    OResultSet result =
        database.query(
            "select * from LinkSetIndexTestClass where linkSet contains ?", docOne.getIdentity());

    List<OIdentifiable> listResult =
        new ArrayList<>(result.next().<Set<OIdentifiable>>getProperty("linkSet"));
    Assert.assertEquals(listResult.size(), 2);
    Assert.assertTrue(
        listResult.containsAll(Arrays.asList(docOne.getIdentity(), docTwo.getIdentity())));
  }
}
