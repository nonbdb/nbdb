package com.orientechnologies.orient.server.security;

import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.exception.YTSecurityException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import com.orientechnologies.orient.server.OServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ORemoteScriptSecurityTest {

  private OServer server;

  @Before
  public void before()
      throws IOException,
      InstantiationException,
      InvocationTargetException,
      NoSuchMethodException,
      MBeanRegistrationException,
      IllegalAccessException,
      InstanceAlreadyExistsException,
      NotCompliantMBeanException,
      ClassNotFoundException,
      MalformedObjectNameException {
    GlobalConfiguration.SERVER_BACKWARD_COMPATIBILITY.setValue(false);
    server = OServer.startFromClasspathConfig("abstract-orientdb-server-config.xml");

    YouTrackDB youTrackDB =
        new YouTrackDB("remote:localhost", "root", "root", YouTrackDBConfig.defaultConfig());
    youTrackDB.execute(
        "create database ORemoteScriptSecurityTest memory users (admin identified by 'admin' role"
            + " admin)");

    youTrackDB.close();
  }

  @Test(expected = YTSecurityException.class)
  public void testRunJavascript() {
    // CREATE A SEPARATE CONTEXT TO MAKE SURE IT LOAD STAFF FROM SCRATCH
    try (YouTrackDB writerOrient = new YouTrackDB("remote:localhost",
        YouTrackDBConfig.defaultConfig())) {
      try (YTDatabaseSession writer =
          writerOrient.open("ORemoteScriptSecurityTest", "reader", "reader")) {
        try (YTResultSet rs = writer.execute("javascript", "1+1;")) {
        }
      }
    }
  }

  @Test(expected = YTSecurityException.class)
  public void testRunEcmascript() {
    // CREATE A SEPARATE CONTEXT TO MAKE SURE IT LOAD STAFF FROM SCRATCH
    try (YouTrackDB writerOrient = new YouTrackDB("remote:localhost",
        YouTrackDBConfig.defaultConfig())) {
      try (YTDatabaseSession writer =
          writerOrient.open("ORemoteScriptSecurityTest", "reader", "reader")) {

        try (YTResultSet rs = writer.execute("ecmascript", "1+1;")) {
        }
      }
    }
  }

  @After
  public void after() {
    server.shutdown();
  }
}
