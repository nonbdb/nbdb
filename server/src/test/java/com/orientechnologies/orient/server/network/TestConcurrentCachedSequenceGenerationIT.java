package com.orientechnologies.orient.server.network;

import static org.junit.Assert.assertNotNull;

import com.jetbrains.youtrack.db.internal.common.io.OFileUtils;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBManager;
import com.jetbrains.youtrack.db.internal.core.db.ODatabasePool;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.record.Vertex;
import com.orientechnologies.orient.server.OServer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestConcurrentCachedSequenceGenerationIT {

  static final int THREADS = 20;
  static final int RECORDS = 100;
  private OServer server;
  private YouTrackDB youTrackDB;

  @Before
  public void before() throws Exception {
    server = new OServer(false);
    server.startup(getClass().getResourceAsStream("orientdb-server-config.xml"));
    server.activate();
    youTrackDB = new YouTrackDB("remote:localhost", "root", "root",
        YouTrackDBConfig.defaultConfig());
    youTrackDB.execute(
        "create database ? memory users (admin identified by 'admin' role admin)",
        TestConcurrentCachedSequenceGenerationIT.class.getSimpleName());
    YTDatabaseSession databaseSession =
        youTrackDB.open(
            TestConcurrentCachedSequenceGenerationIT.class.getSimpleName(), "admin", "admin");
    databaseSession.execute(
        "sql",
        """
            CREATE CLASS TestSequence EXTENDS V;
            begin;
            CREATE SEQUENCE TestSequenceIdSequence TYPE CACHED CACHE 100;
            commit;
            CREATE PROPERTY TestSequence.id LONG (MANDATORY TRUE, default\
             "sequence('TestSequenceIdSequence').next()");
            CREATE INDEX TestSequence_id_index ON TestSequence (id BY VALUE) UNIQUE;""");
    databaseSession.close();
  }

  @Test
  public void test() throws InterruptedException {
    AtomicLong failures = new AtomicLong(0);
    ODatabasePool pool =
        new ODatabasePool(
            youTrackDB,
            TestConcurrentCachedSequenceGenerationIT.class.getSimpleName(),
            "admin",
            "admin");
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < THREADS; i++) {
      Thread thread =
          new Thread() {
            @Override
            public void run() {
              try (YTDatabaseSession db = pool.acquire()) {
                for (int j = 0; j < RECORDS; j++) {
                  db.begin();
                  Vertex vert = db.newVertex("TestSequence");
                  assertNotNull(vert.getProperty("id"));
                  db.save(vert);
                  db.commit();
                }
              } catch (Exception e) {
                failures.incrementAndGet();
                e.printStackTrace();
              }
            }
          };
      threads.add(thread);
      thread.start();
    }
    for (Thread t : threads) {
      t.join();
    }
    Assert.assertEquals(0, failures.get());
    pool.close();
  }

  @After
  public void after() {
    youTrackDB.drop(TestConcurrentCachedSequenceGenerationIT.class.getSimpleName());
    youTrackDB.close();
    server.shutdown();

    YouTrackDBManager.instance().shutdown();
    OFileUtils.deleteRecursively(new File(server.getDatabaseDirectory()));
    YouTrackDBManager.instance().startup();
  }
}
