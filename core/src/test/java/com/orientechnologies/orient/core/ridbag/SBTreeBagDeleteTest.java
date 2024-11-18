package com.orientechnologies.orient.core.ridbag;

import static com.orientechnologies.orient.core.config.OGlobalConfiguration.RID_BAG_SBTREEBONSAI_DELETE_DELAY;
import static org.junit.Assert.assertEquals;

import com.orientechnologies.BaseMemoryInternalDatabase;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.index.sbtreebonsai.local.OSBTreeBonsai;
import com.orientechnologies.orient.core.storage.ridbag.sbtree.OBonsaiCollectionPointer;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by tglman on 01/07/16.
 */
public class SBTreeBagDeleteTest extends BaseMemoryInternalDatabase {

  public void beforeTest() {
    super.beforeTest();
    db.getConfiguration().setValue(RID_BAG_SBTREEBONSAI_DELETE_DELAY, 50);
  }

  @Test
  public void testDeleteRidbagTx() throws InterruptedException {

    ODocument doc = new ODocument();
    ORidBag bag = new ORidBag();
    int size =
        OGlobalConfiguration.INDEX_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.getValueAsInteger() * 2;
    for (int i = 0; i < size; i++) {
      bag.add(new ORecordId(10, i));
    }
    doc.field("bag", bag);

    db.begin();
    ORID id = db.save(doc, db.getClusterNameById(db.getDefaultClusterId())).getIdentity();
    db.commit();

    doc = db.bindToSession(doc);
    bag = doc.field("bag");
    OBonsaiCollectionPointer pointer = bag.getPointer();

    db.begin();
    doc = db.bindToSession(doc);
    db.delete(doc);
    db.commit();

    try {
      db.load(id);
      Assert.fail();
    } catch (ORecordNotFoundException e) {
      // ignore
    }

    Thread.sleep(100);
    OSBTreeBonsai<OIdentifiable, Integer> tree =
        db.getSbTreeCollectionManager().loadSBTree(pointer);
    assertEquals(0, tree.getRealBagSize(Collections.emptyMap()));
  }
}
