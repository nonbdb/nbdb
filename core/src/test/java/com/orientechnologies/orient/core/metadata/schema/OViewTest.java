package com.orientechnologies.orient.core.metadata.schema;

import static org.junit.Assert.assertTrue;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OxygenDB;
import com.orientechnologies.orient.core.db.OxygenDBConfig;
import com.orientechnologies.orient.core.db.OxygenDBInternal;
import com.orientechnologies.orient.core.db.viewmanager.ViewCreationListener;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OViewTest {

  private OxygenDB orientdb;

  @Before
  public void setUp() {
    orientdb = new OxygenDB("embedded:./target/views", OxygenDBConfig.defaultConfig());
  }

  @After
  public void after() {
    if (orientdb.exists("view_close")) {
      orientdb.drop("view_close");
    }
    orientdb.close();
  }

  @Test
  public void testSimple() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    orientdb.execute(
        "create database "
            + OViewTest.class.getSimpleName()
            + " memory users (admin identified by 'adminpwd' role admin)");
    try (ODatabaseSession db =
        orientdb.open(OViewTest.class.getSimpleName(), "admin", "adminpwd")) {

      db.getMetadata()
          .getSchema()
          .createView(
              new OViewConfig("testSimple", "SELECT FROM V"),
              new ViewCreationListener() {
                @Override
                public void afterCreate(ODatabaseSession database, String viewName) {
                  latch.countDown();
                }

                @Override
                public void onError(String viewName, Exception exception) {
                }
              });
      latch.await();

      Assert.assertNotNull(db.getMetadata().getSchema().getView("testSimple"));
      Assert.assertNull(db.getMetadata().getSchema().getClass("testSimple"));
      Assert.assertNull(db.getMetadata().getSchema().getView("V"));
    }
  }

  @Test
  public void testCloseDatabase() throws Exception {
    orientdb.execute(
        "create database view_close plocal users (admin identified by 'adminpwd' role admin)");
    try (ODatabaseSession session = orientdb.open("view_close", "admin", "adminpwd")) {
      session.createClass("test");
      session
          .command(
              "create view test_view from( select name from test ) metadata"
                  + " {updateIntervalSeconds:1, indexes: [{type:'NOTUNIQUE',"
                  + " properties:{name:'STRING'}}]}")
          .close();

      session.begin();
      session.command("insert into test set name='abc'").close();
      session.commit();

      BaseMemoryDatabase.assertWithTimeout(
          session,
          () -> {
            try (OResultSet result = session.query("select from test_view where name='abc'")) {
              String execution = result.getExecutionPlan().get().prettyPrint(0, 0);
              assertTrue(execution.contains("FETCH FROM INDEX"));
            }
          });
    }

    OxygenDBInternal.extract(orientdb).forceDatabaseClose("view_close");
    try (ODatabaseSession session = orientdb.open("view_close", "admin", "adminpwd")) {
      BaseMemoryDatabase.assertWithTimeout(
          session,
          () -> {
            try (OResultSet result = session.query("select from test_view where name='abc'")) {
              String execution = result.getExecutionPlan().get().prettyPrint(0, 0);
              assertTrue(execution.contains("FETCH FROM INDEX"));
            }
          });
    }
  }
}
