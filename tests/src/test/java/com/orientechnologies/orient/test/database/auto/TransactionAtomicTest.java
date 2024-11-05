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

import com.orientechnologies.orient.core.command.OCommandExecutor;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "dictionary")
public class TransactionAtomicTest extends DocumentDBBaseTest {

  @Parameters(value = "remote")
  public TransactionAtomicTest(boolean remote) {
    super(remote);
  }

  @Test
  public void testTransactionAtomic() {
    ODatabaseSessionInternal db1 = acquireSession();
    ODatabaseSessionInternal db2 = acquireSession();

    ODocument record1 = new ODocument();

    db2.begin();
    record1
        .field("value", "This is the first version")
        .save(db2.getClusterNameById(db2.getDefaultClusterId()));
    db2.commit();

    // RE-READ THE RECORD
    db2.activateOnCurrentThread();
    db2.begin();
    ODocument record2 = db2.load(record1.getIdentity());

    record2.field("value", "This is the second version").save();
    db2.commit();

    db2.begin();
    record2.field("value", "This is the third version").save();
    db2.commit();

    db1.activateOnCurrentThread();
    Assert.assertEquals(record1.field("value"), "This is the third version");
    db1.close();

    db2.activateOnCurrentThread();
    db2.close();

    database.activateOnCurrentThread();
  }

  @Test
  public void testMVCC() throws IOException {

    ODocument doc = new ODocument("Account");
    database.begin();
    doc.field("version", 0);
    doc.save();
    database.commit();

    doc.setDirty();
    doc.field("testmvcc", true);
    ORecordInternal.setVersion(doc, doc.getVersion() + 1);
    try {
      database.begin();
      doc.save();
      database.commit();
      Assert.fail();
    } catch (OConcurrentModificationException e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testTransactionPreListenerRollback() throws IOException {
    ODocument record1 = new ODocument();

    database.begin();
    record1
        .field("value", "This is the first version")
        .save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    final ODatabaseListener listener =
        new ODatabaseListener() {

          @Override
          public void onAfterTxCommit(ODatabase iDatabase) {}

          @Override
          public void onAfterTxRollback(ODatabase iDatabase) {}

          @Override
          public void onBeforeTxBegin(ODatabase iDatabase) {}

          @Override
          public void onBeforeTxCommit(ODatabase iDatabase) {
            throw new RuntimeException("Rollback test");
          }

          @Override
          public void onBeforeTxRollback(ODatabase iDatabase) {}

          @Override
          public void onClose(ODatabase iDatabase) {}

          @Override
          public void onBeforeCommand(OCommandRequestText iCommand, OCommandExecutor executor) {}

          @Override
          public void onAfterCommand(
              OCommandRequestText iCommand, OCommandExecutor executor, Object result) {}

          @Override
          public void onCreate(ODatabase iDatabase) {}

          @Override
          public void onDelete(ODatabase iDatabase) {}

          @Override
          public void onOpen(ODatabase iDatabase) {}

          @Override
          public boolean onCorruptionRepairDatabase(
              ODatabase iDatabase, final String iReason, String iWhatWillbeFixed) {
            return true;
          }
        };

    database.registerListener(listener);
    database.begin();

    try {
      database.commit();
      Assert.assertTrue(false);
    } catch (OTransactionException e) {
      Assert.assertTrue(true);
    } finally {
      database.unregisterListener(listener);
    }
  }

  @Test
  public void testTransactionWithDuplicateUniqueIndexValues() {
    OClass fruitClass = database.getMetadata().getSchema().getClass("Fruit");

    if (fruitClass == null) {
      fruitClass = database.getMetadata().getSchema().createClass("Fruit");

      fruitClass.createProperty("name", OType.STRING);
      fruitClass.createProperty("color", OType.STRING);

      database
          .getMetadata()
          .getSchema()
          .getClass("Fruit")
          .getProperty("color")
          .createIndex(OClass.INDEX_TYPE.UNIQUE);
    }

    Assert.assertEquals(database.countClusterElements("Fruit"), 0);

    try {
      database.begin();

      ODocument apple = new ODocument("Fruit").field("name", "Apple").field("color", "Red");
      ODocument orange = new ODocument("Fruit").field("name", "Orange").field("color", "Orange");
      ODocument banana = new ODocument("Fruit").field("name", "Banana").field("color", "Yellow");
      ODocument kumquat = new ODocument("Fruit").field("name", "Kumquat").field("color", "Orange");

      apple.save();
      orange.save();
      banana.save();
      kumquat.save();

      database.commit();

      Assert.assertEquals(apple.getIdentity().getClusterId(), fruitClass.getDefaultClusterId());
      Assert.assertEquals(orange.getIdentity().getClusterId(), fruitClass.getDefaultClusterId());
      Assert.assertEquals(banana.getIdentity().getClusterId(), fruitClass.getDefaultClusterId());
      Assert.assertEquals(kumquat.getIdentity().getClusterId(), fruitClass.getDefaultClusterId());

      Assert.assertTrue(false);

    } catch (ORecordDuplicatedException e) {
      Assert.assertTrue(true);
      database.rollback();
    }

    Assert.assertEquals(database.countClusterElements("Fruit"), 0);
  }

  @Test
  public void testTransactionalSQL() {
    long prev = database.countClass("Account");

    database
        .command(new OCommandSQL("transactional insert into Account set name = 'txTest1'"))
        .execute();

    Assert.assertEquals(database.countClass("Account"), prev + 1);
  }

  @Test
  public void testTransactionalSQLJoinTx() {
    long prev = database.countClass("Account");

    database.begin();
    database
        .command(new OCommandSQL("transactional insert into Account set name = 'txTest2'"))
        .execute();

    Assert.assertTrue(database.getTransaction().isActive());

    if (!remoteDB) {
      Assert.assertEquals(database.countClass("Account"), prev + 1);
    }

    database.commit();

    Assert.assertFalse(database.getTransaction().isActive());
    Assert.assertEquals(database.countClass("Account"), prev + 1);
  }
}
