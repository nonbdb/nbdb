package com.orientechnologies.orient.server.query;

import static com.orientechnologies.orient.core.config.OGlobalConfiguration.QUERY_REMOTE_RESULTSET_PAGE_SIZE;

import com.orientechnologies.common.io.OFileUtils;
import com.orientechnologies.orient.client.remote.OStorageRemote;
import com.orientechnologies.orient.core.Oxygen;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OxygenDB;
import com.orientechnologies.orient.core.db.OxygenDBConfig;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.enterprise.channel.binary.OTokenSecurityException;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.token.OTokenHandlerImpl;
import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class RemoteTokenExpireTest {

  private static final String SERVER_DIRECTORY = "./target/token";
  private OServer server;
  private OxygenDB oxygenDB;
  private ODatabaseSession session;
  private int oldPageSize;

  private final long expireTimeout = 500;

  @Before
  public void before() throws Exception {

    OFileUtils.deleteRecursively(new File(SERVER_DIRECTORY));
    server = new OServer(false);
    server.setServerRootDirectory(SERVER_DIRECTORY);
    server.startup(getClass().getResourceAsStream("orientdb-server-config.xml"));

    server.activate();

    OTokenHandlerImpl token = (OTokenHandlerImpl) server.getTokenHandler();
    token.setSessionInMills(expireTimeout);

    oxygenDB = new OxygenDB("remote:localhost", "root", "root", OxygenDBConfig.defaultConfig());
    oxygenDB.execute(
        "create database ? memory users (admin identified by 'admin' role admin)",
        RemoteTokenExpireTest.class.getSimpleName());
    session = oxygenDB.open(RemoteTokenExpireTest.class.getSimpleName(), "admin", "admin");
    session.createClass("Some");
    oldPageSize = QUERY_REMOTE_RESULTSET_PAGE_SIZE.getValueAsInteger();
    QUERY_REMOTE_RESULTSET_PAGE_SIZE.setValue(10);

    session.close();
    oxygenDB.close();

    var config =
        OxygenDBConfig.builder().addConfig(OGlobalConfiguration.NETWORK_SOCKET_RETRY, 0).build();
    oxygenDB = new OxygenDB("remote:localhost", "root", "root", config);
    session = oxygenDB.open(RemoteTokenExpireTest.class.getSimpleName(), "admin", "admin");
  }

  private void clean() {
    server.getClientConnectionManager().cleanExpiredConnections();
  }

  private void waitAndClean(long ms) {
    try {
      Thread.sleep(ms);
      clean();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void waitAndClean() {
    waitAndClean(expireTimeout);
  }

  @Test
  public void itShouldNotFailWithQuery() {

    waitAndClean();

    session.activateOnCurrentThread();

    try (OResultSet res = session.query("select from Some")) {

      Assert.assertEquals(0, res.stream().count());

    } catch (OTokenSecurityException e) {

      Assert.fail("It should not get the exception");
    }
  }

  @Test
  public void itShouldNotFailWithCommand() {

    waitAndClean();

    session.activateOnCurrentThread();

    session.begin();
    try (OResultSet res = session.command("insert into V set name = 'foo'")) {
      session.commit();

      Assert.assertEquals(1, res.stream().count());

    } catch (OTokenSecurityException e) {

      Assert.fail("It should not get the exception");
    }
  }

  @Test
  public void itShouldNotFailWithScript() {

    waitAndClean();

    session.activateOnCurrentThread();

    try (OResultSet res = session.execute("sql", "begin;insert into V set name = 'foo';commit;")) {

      Assert.assertEquals(1, res.stream().count());

    } catch (OTokenSecurityException e) {

      Assert.fail("It should not get the exception");
    }
  }

  @Test
  public void itShouldFailWithQueryNext() throws InterruptedException {

    QUERY_REMOTE_RESULTSET_PAGE_SIZE.setValue(1);

    try (OResultSet res = session.query("select from ORole")) {

      waitAndClean();
      session.activateOnCurrentThread();
      Assert.assertEquals(3, res.stream().count());

    } catch (OTokenSecurityException e) {
      return;
    } finally {
      QUERY_REMOTE_RESULTSET_PAGE_SIZE.setValue(10);
    }
    Assert.fail("It should get an exception");
  }

  @Test
  public void itShouldNotFailWithNewTXAndQuery() {

    waitAndClean();

    session.activateOnCurrentThread();

    session.begin();

    session.save(session.newElement("Some"));

    try (OResultSet res = session.query("select from Some")) {
      Assert.assertEquals(1, res.stream().count());
    } catch (OTokenSecurityException e) {
      Assert.fail("It should not get the expire exception");
    } finally {
      session.rollback();
    }
  }

  @Test
  public void itShouldFailAtBeingAndQuery() {

    session.begin();

    session.save(session.newElement("Some"));

    try (OResultSet resultSet = session.query("select from Some")) {
      Assert.assertEquals(1, resultSet.stream().count());
    }
    waitAndClean();

    session.activateOnCurrentThread();

    try {
      session.query("select from Some");
    } catch (OTokenSecurityException e) {
      session.rollback();
      return;
    }
    Assert.fail("It should not get the expire exception");
  }

  @Test
  public void itShouldNotFailWithRoundRobin() {

    ODatabasePool pool =
        new ODatabasePool(
            oxygenDB,
            RemoteTokenExpireTest.class.getSimpleName(),
            "admin",
            "admin",
            OxygenDBConfig.builder()
                .addConfig(
                    OGlobalConfiguration.CLIENT_CONNECTION_STRATEGY,
                    OStorageRemote.CONNECTION_STRATEGY.ROUND_ROBIN_CONNECT)
                .build());

    ODatabaseSession session = pool.acquire();

    try (OResultSet resultSet = session.query("select from Some")) {
      Assert.assertEquals(0, resultSet.stream().count());
    }

    waitAndClean();

    session.activateOnCurrentThread();

    try {
      try (OResultSet resultSet = session.query("select from Some")) {
        Assert.assertEquals(0, resultSet.stream().count());
      }
    } catch (OTokenSecurityException e) {
      Assert.fail("It should  get the expire exception");
    }
    pool.close();
  }

  @After
  public void after() {
    QUERY_REMOTE_RESULTSET_PAGE_SIZE.setValue(oldPageSize);
    session.activateOnCurrentThread();
    session.close();
    oxygenDB.close();
    server.shutdown();

    Oxygen.instance().shutdown();
    OFileUtils.deleteRecursively(new File(SERVER_DIRECTORY));
    Oxygen.instance().startup();
  }
}
