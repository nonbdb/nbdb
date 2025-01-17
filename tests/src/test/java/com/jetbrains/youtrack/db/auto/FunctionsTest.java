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

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.CommandSQL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class FunctionsTest extends BaseDBTest {

  @Parameters(value = "remote")
  public FunctionsTest(boolean remote) {
    super(remote);
  }

  @Test
  public void createFunctionBug2415() {
    Identifiable result =
        database
            .command(
                new CommandSQL(
                    "create function FunctionsTest \"return a + b\" PARAMETERS [a,b] IDEMPOTENT"
                        + " true LANGUAGE Javascript"))
            .execute(database);

    database.begin();
    final EntityImpl record = result.getRecord();
    final List<String> parameters = record.field("parameters");

    Assert.assertNotNull(parameters);
    Assert.assertEquals(parameters.size(), 2);
    database.rollback();
  }

  @Test
  public void testFunctionDefinitionAndCall() {
    database.command("create function testCall \"return 0;\" LANGUAGE Javascript").close();

    ResultSet res1 = database.command("select testCall() as testCall");
    Assert.assertEquals((int) res1.next().getProperty("testCall"), 0);
  }

  @Test
  public void testFunctionCacheAndReload() {
    Identifiable f =
        database
            .command(new CommandSQL("create function testCache \"return 1;\" LANGUAGE Javascript"))
            .execute(database);
    Assert.assertNotNull(f);

    try (ResultSet res1 = database.command("select testCache() as testCache")) {
      Assert.assertEquals(res1.next().<Object>getProperty("testCache"), 1);
    }

    database.begin();
    EntityImpl func = f.getRecord();
    func.field("code", "return 2;");
    func.save();
    database.commit();

    try (ResultSet res2 = database.command("select testCache() as testCache")) {
      Assert.assertEquals(res2.next().<Object>getProperty("testCache"), 2);
    }
  }

  @Test
  public void testMultiThreadsFunctionCallMoreThanPool() {
    database.command("create function testMTCall \"return 3;\" LANGUAGE Javascript").close();

    final int TOT = 1000;
    final int threadNum = GlobalConfiguration.SCRIPT_POOL.getValueAsInteger() * 3;
    final AtomicLong counter = new AtomicLong();

    final Thread[] threads = new Thread[threadNum];
    for (int i = 0; i < threadNum; ++i) {
      threads[i] =
          new Thread() {
            public void run() {
              for (int cycle = 0; cycle < TOT; ++cycle) {
                ResultSet res1 = database.command("select testMTCall() as testMTCall");
                Assert.assertNotNull(res1);
                Assert.assertEquals(res1.next().<Object>getProperty("testMTCall"), 3);

                counter.incrementAndGet();
              }
            }
          };
      threads[i].start();
    }

    for (int i = 0; i < threadNum; ++i) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Assert.assertEquals(counter.get(), (long) threadNum * TOT);
  }

  @Test
  public void testFunctionDefinitionAndCallWithParams() {
    database
        .command(
            "create function testParams \"return 'Hello ' + name + ' ' + surname + ' from ' +"
                + " country;\" PARAMETERS [name,surname,country] LANGUAGE Javascript")
        .close();

    try (ResultSet res1 =
        database.command("select testParams('Jay', 'Miner', 'USA') as testParams")) {
      Assert.assertEquals(res1.next().getProperty("testParams"), "Hello Jay Miner from USA");
    }

    final HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("name", "Jay");
    params.put("surname", "Miner");
    params.put("country", "USA");

    Object result =
        database
            .getMetadata()
            .getFunctionLibrary()
            .getFunction("testParams")
            .executeInContext(null, params);
    Assert.assertEquals(result, "Hello Jay Miner from USA");
  }

  @Test
  public void testMapParamToFunction() {
    database
        .command(
            "create function testMapParamToFunction \"return mapParam.get('foo')[0];\" PARAMETERS"
                + " [mapParam] LANGUAGE Javascript")
        .close();

    Map<String, Object> params = new HashMap<String, Object>();

    List theList = new ArrayList();
    theList.add("bar");
    Map theMap = new HashMap();
    theMap.put("foo", theList);
    params.put("theParam", theMap);

    ResultSet res1 = database.command("select testMapParamToFunction(:theParam) as a", params);
    Assert.assertEquals(res1.next().getProperty("a"), "bar");
  }
}
