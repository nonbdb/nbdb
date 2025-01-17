/*
 *
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
package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.query.LegacyResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.query.LiveQuery;
import com.jetbrains.youtrack.db.internal.core.sql.query.LiveResultListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * If some of the tests start to fail then check cluster number in queries, e.g #7:1. It can be
 * because the order of clusters could be affected due to adding or removing cluster from storage.
 */
@Test(groups = "sql-select")
@SuppressWarnings("unchecked")
public class SQLLiveSelectTest extends AbstractSelectTest {

  @Parameters(value = "remote")
  public SQLLiveSelectTest(boolean remote) throws Exception {
    super(remote);
  }

  @BeforeClass
  public void init() {

    database.getMetadata().getSchema().getOrCreateClass("LiveClass");
    database.getMetadata().getSchema().getOrCreateClass("LiveClassTx");
  }

  @Test
  public void liveQueryTestTX() throws InterruptedException {

    int TOTAL_OPS = 6;
    final CountDownLatch latch = new CountDownLatch(TOTAL_OPS);
    final List<RecordOperation> ops = Collections.synchronizedList(new ArrayList());
    LegacyResultSet<EntityImpl> tokens =
        database.query(
            new LiveQuery<Object>(
                "live select from LiveClassTx",
                new LiveResultListener() {
                  @Override
                  public void onLiveResult(int iLiveToken, RecordOperation iOp)
                      throws BaseException {
                    ops.add(iOp);
                    latch.countDown();
                  }

                  @Override
                  public void onError(int iLiveToken) {
                  }

                  @Override
                  public void onUnsubscribe(int iLiveToken) {
                  }
                }));
    Assert.assertEquals(tokens.size(), 1);

    EntityImpl tokenDoc = tokens.get(0);
    Integer token = tokenDoc.field("token");
    Assert.assertNotNull(token);

    database.begin();
    database.command("insert into LiveClassTx set name = 'foo', surname = 'bar'").close();
    database.command("insert into LiveClassTx set name = 'foo', surname = 'baz'").close();
    database.command("insert into LiveClassTx set name = 'foo'").close();
    database.commit();

    database.begin();
    database.command("update LiveClassTx set name = 'updated'").close();
    database.commit();

    latch.await();

    Assert.assertEquals(ops.size(), TOTAL_OPS);
    for (RecordOperation doc : ops) {
      if (doc.type == RecordOperation.CREATED) {
        Assert.assertEquals(((EntityImpl) doc.record).field("name"), "foo");
      } else if (doc.type == RecordOperation.UPDATED) {
        Assert.assertEquals(((EntityImpl) doc.record).field("name"), "updated");
      } else {
        Assert.fail();
      }
    }
  }

  @Test
  public void liveQueryTest() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(6);
    final List<RecordOperation> ops = Collections.synchronizedList(new ArrayList());
    LegacyResultSet<EntityImpl> tokens =
        database.query(
            new LiveQuery<Object>(
                "live select from LiveClass",
                new LiveResultListener() {
                  @Override
                  public void onLiveResult(int iLiveToken, RecordOperation iOp)
                      throws BaseException {
                    ops.add(iOp);
                    latch.countDown();
                  }

                  @Override
                  public void onError(int iLiveToken) {
                  }

                  @Override
                  public void onUnsubscribe(int iLiveToken) {
                  }
                }));
    Assert.assertEquals(tokens.size(), 1);

    EntityImpl tokenDoc = tokens.get(0);
    Integer token = tokenDoc.field("token");
    Assert.assertNotNull(token);

    database.command("insert into liveclass set name = 'foo', surname = 'bar'").close();
    database.command("insert into liveclass set name = 'foo', surname = 'baz'").close();
    database.command("insert into liveclass set name = 'foo'").close();

    database.command("update liveclass set name = 'updated'").close();

    latch.await();

    Assert.assertEquals(ops.size(), 6);
    for (RecordOperation doc : ops) {
      if (doc.type == RecordOperation.CREATED) {
        Assert.assertEquals(((EntityImpl) doc.record).field("name"), "foo");
      } else if (doc.type == RecordOperation.UPDATED) {
        Assert.assertEquals(((EntityImpl) doc.record).field("name"), "updated");
      } else {
        Assert.fail();
      }
    }
  }
}
