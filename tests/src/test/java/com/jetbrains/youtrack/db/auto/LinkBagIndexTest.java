package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @since 1/30/14
 */
@Test
public class LinkBagIndexTest extends BaseDBTest {

  @Parameters(value = "remote")
  public LinkBagIndexTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  public void setupSchema() {
    final SchemaClass ridBagIndexTestClass =
        database.getMetadata().getSchema().createClass("RidBagIndexTestClass");

    ridBagIndexTestClass.createProperty(database, "ridBag", PropertyType.LINKBAG);

    ridBagIndexTestClass.createIndex(database, "ridBagIndex", SchemaClass.INDEX_TYPE.NOTUNIQUE,
        "ridBag");

    database.close();
  }

  @AfterClass
  public void destroySchema() {
    if (database.isClosed()) {
      database = acquireSession();
    }

    database.getMetadata().getSchema().dropClass("RidBagIndexTestClass");
    database.close();
  }

  @AfterMethod
  public void afterMethod() {
    database.begin();
    database.command("DELETE FROM RidBagIndexTestClass").close();
    database.commit();

    ResultSet result = database.query("select from RidBagIndexTestClass");
    Assert.assertEquals(result.stream().count(), 0);

    if (!database.getStorage().isRemote()) {
      final Index index = getIndex("ridBagIndex");
      Assert.assertEquals(index.getInternal().size(database), 0);
    }
  }

  public void testIndexRidBag() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);

    document.save();
    database.commit();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 2);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagInTx() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));
    try {
      final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
      final RidBag ridBag = new RidBag(database);
      ridBag.add(docOne);
      ridBag.add(docTwo);

      document.field("ridBag", ridBag);
      document.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 2);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdate() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docThree = new EntityImpl();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBagOne = new RidBag(database);
    ridBagOne.add(docOne);
    ridBagOne.add(docTwo);

    document.field("ridBag", ridBagOne);

    document.save();
    database.commit();

    database.begin();
    final RidBag ridBagTwo = new RidBag(database);
    ridBagTwo.add(docOne);
    ridBagTwo.add(docThree);

    document = database.bindToSession(document);
    document.field("ridBag", ridBagTwo);

    database.bindToSession(document).save();
    database.commit();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 2);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateInTx() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docThree = new EntityImpl();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBagOne = new RidBag(database);
    ridBagOne.add(docOne);
    ridBagOne.add(docTwo);

    document.field("ridBag", ridBagOne);

    document.save();
    database.commit();

    try {
      database.begin();

      document = database.bindToSession(document);
      final RidBag ridBagTwo = new RidBag(database);
      ridBagTwo.add(docOne);
      ridBagTwo.add(docThree);

      document.field("ridBag", ridBagTwo);
      document.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 2);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docThree = new EntityImpl();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final RidBag ridBagOne = new RidBag(database);
    ridBagOne.add(docOne);
    ridBagOne.add(docTwo);

    EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    document.field("ridBag", ridBagOne);
    document.save();
    Assert.assertTrue(database.commit());

    database.begin();
    document = database.bindToSession(document);
    final RidBag ridBagTwo = new RidBag(database);
    ridBagTwo.add(docOne);
    ridBagTwo.add(docThree);

    document.field("ridBag", ridBagTwo);
    document.save();
    database.rollback();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 2);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateAddItem() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docThree = new EntityImpl();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);
    document.field("ridBag", ridBag);

    document.save();
    database.commit();

    database.begin();
    database
        .command(
            "UPDATE "
                + document.getIdentity()
                + " set ridBag = ridBag || "
                + docThree.getIdentity())
        .close();
    database.commit();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 3);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateAddItemInTx() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    EntityImpl docThree = new EntityImpl();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);

    document.save();
    database.commit();

    try {
      database.begin();
      docThree = database.bindToSession(docThree);
      EntityImpl loadedDocument = database.load(document.getIdentity());
      loadedDocument.<RidBag>field("ridBag").add(docThree);
      loadedDocument.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 3);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())
            && !key.getIdentity().equals(docThree.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateAddItemInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    EntityImpl docThree = new EntityImpl();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);

    document.save();
    database.commit();

    database.begin();
    docThree = database.bindToSession(docThree);
    EntityImpl loadedDocument = database.load(document.getIdentity());
    loadedDocument.<RidBag>field("ridBag").add(docThree);
    loadedDocument.save();
    database.rollback();

    final Index index = getIndex("ridBagIndex");

    Assert.assertEquals(index.getInternal().size(database), 2);
    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateRemoveItemInTx() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);
    document.field("ridBag", ridBag);

    document.save();
    database.commit();

    try {
      database.begin();
      EntityImpl loadedDocument = database.load(document.getIdentity());
      loadedDocument.<RidBag>field("ridBag").remove(docTwo);
      loadedDocument.save();
      database.commit();
    } catch (Exception e) {
      database.rollback();
      throw e;
    }

    final Index index = getIndex("ridBagIndex");

    Assert.assertEquals(index.getInternal().size(database), 1);
    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateRemoveItemInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);
    document.field("ridBag", ridBag);
    document.save();
    database.commit();

    database.begin();
    EntityImpl loadedDocument = database.load(document.getIdentity());
    loadedDocument.<RidBag>field("ridBag").remove(docTwo);
    loadedDocument.save();
    database.rollback();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 2);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagUpdateRemoveItem() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);
    document.save();
    database.commit();

    //noinspection deprecation
    database.begin();
    database
        .command("UPDATE " + document.getIdentity() + " remove ridBag = " + docTwo.getIdentity())
        .close();
    database.commit();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 1);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();
        if (!key.getIdentity().equals(docOne.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagRemove() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");

    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);
    document.save();
    document.delete();
    database.commit();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 0);
  }

  public void testIndexRidBagRemoveInTx() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");

    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);
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

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 0);
  }

  public void testIndexRidBagRemoveInTxRollback() {
    checkEmbeddedDB();

    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBag = new RidBag(database);
    ridBag.add(docOne);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);
    document.save();
    database.commit();

    database.begin();
    database.bindToSession(document).delete();
    database.rollback();

    final Index index = getIndex("ridBagIndex");
    Assert.assertEquals(index.getInternal().size(database), 2);

    final Iterator<Object> keyIterator;
    try (Stream<Object> keyStream = index.getInternal().keyStream()) {
      keyIterator = keyStream.iterator();

      while (keyIterator.hasNext()) {
        Identifiable key = (Identifiable) keyIterator.next();

        if (!key.getIdentity().equals(docOne.getIdentity())
            && !key.getIdentity().equals(docTwo.getIdentity())) {
          Assert.fail("Unknown key found: " + key);
        }
      }
    }
  }

  public void testIndexRidBagSQL() {
    database.begin();
    final EntityImpl docOne = new EntityImpl();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docTwo = new EntityImpl();
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));

    final EntityImpl docThree = new EntityImpl();
    docThree.save(database.getClusterNameById(database.getDefaultClusterId()));

    EntityImpl document = new EntityImpl("RidBagIndexTestClass");
    final RidBag ridBagOne = new RidBag(database);
    ridBagOne.add(docOne);
    ridBagOne.add(docTwo);

    document.field("ridBag", ridBagOne);
    document.save();

    document = new EntityImpl("RidBagIndexTestClass");
    RidBag ridBag = new RidBag(database);
    ridBag.add(docThree);
    ridBag.add(docTwo);

    document.field("ridBag", ridBag);
    document.save();
    database.commit();

    ResultSet result =
        database.query(
            "select * from RidBagIndexTestClass where ridBag contains ?", docOne.getIdentity());
    Result res = result.next();

    List<Identifiable> listResult = new ArrayList<>();
    for (Identifiable identifiable : res.<RidBag>getProperty("ridBag")) {
      listResult.add(identifiable);
    }
    result.close();

    Assert.assertEquals(Arrays.asList(docOne.getIdentity(), docTwo.getIdentity()), listResult);
  }
}
