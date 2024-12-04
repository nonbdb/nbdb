package com.orientechnologies.orient.client.remote;

import static org.assertj.core.api.Assertions.assertThat;

import com.orientechnologies.orient.client.remote.message.MockChannel;
import com.orientechnologies.orient.client.remote.message.OMessageHelper;
import com.orientechnologies.orient.client.remote.message.tx.ORecordOperationRequest;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.YouTrackDB;
import com.orientechnologies.orient.core.db.YouTrackDBConfig;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODirtyManager;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerNetworkFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OMessageHelperTest {

  @Test
  public void testOIdentifiable() throws IOException {

    YouTrackDB youTrackDB = new YouTrackDB("embedded", YouTrackDBConfig.defaultConfig());

    youTrackDB.execute(
        "create database testOIdentifiable memory users (admin identified by 'admin' role admin)");

    ODatabaseSessionInternal db =
        (ODatabaseSessionInternal) youTrackDB.open("testOIdentifiable", "admin", "admin");
    int id = db.getClusterIdByName("V");
    try {
      MockChannel channel = new MockChannel();
      ODocument doc = new ODocument();
      ORidBag bags = new ORidBag(db);
      bags.add(new ORecordId(id, 0));
      doc.field("bag", bags);

      ODocumentInternal.fillClassNameIfNeeded(doc, "Test");
      ORecordInternal.setIdentity(doc, new ORecordId(id, 1));
      ORecordInternal.setVersion(doc, 1);

      OMessageHelper.writeIdentifiable(null,
          channel, doc, ORecordSerializerNetworkFactory.INSTANCE.current());
      channel.close();

      ODocument newDoc =
          (ODocument)
              OMessageHelper.readIdentifiable(db,
                  channel, ORecordSerializerNetworkFactory.INSTANCE.current());

      assertThat(newDoc.getClassName()).isEqualTo("Test");
      assertThat((ORidBag) newDoc.field("bag")).hasSize(1);

      ODirtyManager dirtyManager = ORecordInternal.getDirtyManager(newDoc);
      assertThat(dirtyManager.getNewRecords()).isNull();

    } finally {
      db.close();
      youTrackDB.close();
    }
  }

  @Test
  public void testReadWriteTransactionEntry() {
    ORecordOperationRequest request = new ORecordOperationRequest();

    request.setType(ORecordOperation.UPDATED);
    request.setRecordType(ORecordOperation.UPDATED);
    request.setId(new ORecordId(25, 50));
    request.setRecord(new byte[]{10, 20, 30});
    request.setVersion(100);
    request.setContentChanged(true);

    ByteArrayOutputStream outArray = new ByteArrayOutputStream();
    DataOutput out = new DataOutputStream(outArray);

    try {
      OMessageHelper.writeTransactionEntry(out, request);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }

    DataInputStream in = new DataInputStream(new ByteArrayInputStream(outArray.toByteArray()));

    try {
      ORecordOperationRequest result = OMessageHelper.readTransactionEntry(in);
      Assert.assertEquals(request.getType(), result.getType());
      Assert.assertEquals(request.getRecordType(), result.getRecordType());
      Assert.assertEquals(request.getType(), result.getType());
      Assert.assertEquals(request.getId(), result.getId());
      Assert.assertArrayEquals(request.getRecord(), result.getRecord());
      Assert.assertEquals(request.getVersion(), result.getVersion());
      Assert.assertEquals(request.isContentChanged(), result.isContentChanged());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
}
