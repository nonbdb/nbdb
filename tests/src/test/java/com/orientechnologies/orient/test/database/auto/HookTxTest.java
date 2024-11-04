/*
 * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.hook.ORecordHookAbstract;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.test.domain.whiz.Profile;
import java.io.IOException;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "hook")
public class HookTxTest extends ObjectDBBaseTest {

  public static final int RECORD_BEFORE_CREATE = 3;
  public static final int RECORD_AFTER_CREATE = 5;
  public static final int RECORD_BEFORE_READ = 7;
  public static final int RECORD_AFTER_READ = 11;
  public static final int RECORD_BEFORE_UPDATE = 13;
  public static final int RECORD_AFTER_UPDATE = 17;
  public static final int RECORD_BEFORE_DELETE = 19;
  public static final int RECORD_AFTER_DELETE = 23;

  private int callbackCount = 0;
  private Profile p;
  private int expectedHookState;

  private final class RecordHook extends ORecordHookAbstract {

    @Override
    @Test(enabled = false)
    public RESULT onRecordBeforeCreate(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_BEFORE_CREATE;
      }
      return RESULT.RECORD_NOT_CHANGED;
    }

    @Override
    @Test(enabled = false)
    public void onRecordAfterCreate(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_AFTER_CREATE;
      }
    }

    @Override
    @Test(enabled = false)
    public RESULT onRecordBeforeRead(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_BEFORE_READ;
      }
      return RESULT.RECORD_NOT_CHANGED;
    }

    @Override
    @Test(enabled = false)
    public void onRecordAfterRead(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_AFTER_READ;
      }
    }

    @Override
    @Test(enabled = false)
    public RESULT onRecordBeforeUpdate(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_BEFORE_UPDATE;
      }
      return RESULT.RECORD_NOT_CHANGED;
    }

    @Override
    @Test(enabled = false)
    public void onRecordAfterUpdate(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_AFTER_UPDATE;
      }
    }

    @Override
    @Test(enabled = false)
    public RESULT onRecordBeforeDelete(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_BEFORE_DELETE;
      }
      return RESULT.RECORD_NOT_CHANGED;
    }

    @Override
    @Test(enabled = false)
    public void onRecordAfterDelete(ORecord iRecord) {
      if (iRecord instanceof ODocument
          && ((ODocument) iRecord).getClassName() != null
          && ((ODocument) iRecord).getClassName().equals("Profile")) {
        callbackCount += RECORD_AFTER_DELETE;
      }
    }

    @Override
    public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
      return DISTRIBUTED_EXECUTION_MODE.TARGET_NODE;
    }
  }

  @Parameters(value = "remote")
  public HookTxTest(boolean remote) {
    super(remote);
  }

  @Test
  public void testRegisterHook() throws IOException {
    database.registerHook(new RecordHook());
    database
        .getEntityManager()
        .registerEntityClasses("com.orientechnologies.orient.test.domain.business");
    database
        .getEntityManager()
        .registerEntityClasses("com.orientechnologies.orient.test.domain.whiz");
    database
        .getEntityManager()
        .registerEntityClasses("com.orientechnologies.orient.test.domain.base");
  }

  @Test(dependsOnMethods = "testRegisterHook")
  public void testHookCallsCreate() {
    database.registerHook(new RecordHook());
    p = new Profile("HookTxTest");

    expectedHookState = 0;

    // TEST HOOKS ON CREATE
    Assert.assertEquals(callbackCount, 0);
    database.begin();
    database.save(p);
    database.commit();

    expectedHookState += RECORD_BEFORE_CREATE + RECORD_AFTER_CREATE;
    Assert.assertEquals(callbackCount, expectedHookState);
  }

  @Test(dependsOnMethods = "testHookCallsCreate")
  public void testHookCallsRead() {
    database.registerHook(new RecordHook());
    // TEST HOOKS ON READ
    database.begin();
    List<Profile> result =
        database.query(
            new OSQLSynchQuery<Profile>("select * from Profile where nick = 'HookTxTest'"));
    expectedHookState += RECORD_BEFORE_READ + RECORD_AFTER_READ;

    Assert.assertFalse(result.isEmpty());

    for (Profile profile : result) {
      Assert.assertEquals(callbackCount, expectedHookState);

      p = profile;
    }

    Assert.assertEquals(callbackCount, expectedHookState);
    database.commit();
  }

  @Test(dependsOnMethods = "testHookCallsRead")
  public void testHookCallsUpdate() {
    database.registerHook(new RecordHook());
    database.begin();
    // TEST HOOKS ON UPDATE
    p.setValue(p.getValue() + 1000);
    database.save(p);

    database.commit();

    expectedHookState +=
        RECORD_BEFORE_UPDATE + RECORD_AFTER_UPDATE + RECORD_BEFORE_READ + RECORD_AFTER_READ;
    Assert.assertEquals(callbackCount, expectedHookState);
  }

  @Test(dependsOnMethods = "testHookCallsUpdate")
  public void testHookCallsDelete() throws IOException {
    database.registerHook(new RecordHook());
    // TEST HOOKS ON DELETE
    database.begin();
    database.delete(p);
    database.commit();

    expectedHookState +=
        RECORD_BEFORE_DELETE + RECORD_AFTER_DELETE + RECORD_BEFORE_READ + RECORD_AFTER_READ;
    Assert.assertEquals(callbackCount, expectedHookState);

    database.unregisterHook(new RecordHook());
  }
}
