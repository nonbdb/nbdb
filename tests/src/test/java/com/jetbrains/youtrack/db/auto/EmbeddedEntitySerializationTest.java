package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * @since 3/27/14
 */
public class EmbeddedEntitySerializationTest extends BaseDBTest {

  @Parameters(value = "remote")
  public EmbeddedEntitySerializationTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  public void testEmbeddedObjectSerialization() {
    db.begin();
    final EntityImpl originalDoc = ((EntityImpl) db.newEntity());

    final CompositeKey compositeKey =
        new CompositeKey(123, "56", new Date(), new RecordId("#0:12"));
    originalDoc.field("compositeKey", compositeKey);
    originalDoc.field("int", 12);
    originalDoc.field("val", "test");
    originalDoc.save(db.getClusterNameById(db.getDefaultClusterId()));
    db.commit();

    final EntityImpl loadedDoc = db.load(originalDoc.getIdentity());
    Assert.assertNotSame(loadedDoc, originalDoc);

    final CompositeKey loadedCompositeKey = loadedDoc.field("compositeKey");
    Assert.assertEquals(loadedCompositeKey, compositeKey);

    db.begin();
    db.bindToSession(originalDoc).delete();
    db.commit();
  }

  public void testEmbeddedObjectSerializationInsideOfOtherEmbeddedObjects() {
    final EntityImpl originalDoc = ((EntityImpl) db.newEntity());

    final CompositeKey compositeKeyOne =
        new CompositeKey(123, "56", new Date(), new RecordId("#0:12"));
    final CompositeKey compositeKeyTwo =
        new CompositeKey(
            245, "63", new Date(System.currentTimeMillis() + 100), new RecordId("#0:2"));
    final CompositeKey compositeKeyThree =
        new CompositeKey(
            36, "563", new Date(System.currentTimeMillis() + 1000), new RecordId("#0:23"));

    final EntityImpl embeddedDocOne = ((EntityImpl) db.newEntity());
    embeddedDocOne.field("compositeKey", compositeKeyOne);
    embeddedDocOne.field("val", "test");
    embeddedDocOne.field("int", 10);

    final EntityImpl embeddedDocTwo = ((EntityImpl) db.newEntity());
    embeddedDocTwo.field("compositeKey", compositeKeyTwo);
    embeddedDocTwo.field("val", "test");
    embeddedDocTwo.field("int", 10);

    final EntityImpl embeddedDocThree = ((EntityImpl) db.newEntity());
    embeddedDocThree.field("compositeKey", compositeKeyThree);
    embeddedDocThree.field("val", "test");
    embeddedDocThree.field("int", 10);

    List<EntityImpl> embeddedCollection = new ArrayList<EntityImpl>();
    embeddedCollection.add(embeddedDocTwo);
    embeddedCollection.add(embeddedDocThree);

    originalDoc.field("embeddedDoc", embeddedDocOne, PropertyType.EMBEDDED);
    originalDoc.field("embeddedCollection", embeddedCollection, PropertyType.EMBEDDEDLIST);

    db.begin();
    originalDoc.save(db.getClusterNameById(db.getDefaultClusterId()));
    db.commit();

    final EntityImpl loadedDocument = db.load(originalDoc.getIdentity());
    Assert.assertNotSame(loadedDocument, originalDoc);

    final EntityImpl loadedEmbeddedDocOne = loadedDocument.field("embeddedDoc");
    Assert.assertNotSame(loadedEmbeddedDocOne, embeddedDocOne);

    Assert.assertEquals(loadedEmbeddedDocOne.field("compositeKey"), compositeKeyOne);

    List<EntityImpl> loadedEmbeddedCollection = loadedDocument.field("embeddedCollection");
    Assert.assertNotSame(loadedEmbeddedCollection, embeddedCollection);

    final EntityImpl loadedEmbeddedDocTwo = loadedEmbeddedCollection.get(0);
    Assert.assertNotSame(loadedEmbeddedDocTwo, embeddedDocTwo);

    Assert.assertEquals(loadedEmbeddedDocTwo.field("compositeKey"), compositeKeyTwo);

    final EntityImpl loadedEmbeddedDocThree = loadedEmbeddedCollection.get(1);
    Assert.assertNotSame(loadedEmbeddedDocThree, embeddedDocThree);

    Assert.assertEquals(loadedEmbeddedDocThree.field("compositeKey"), compositeKeyThree);

    db.begin();
    db.bindToSession(originalDoc).delete();
    db.commit();
  }
}