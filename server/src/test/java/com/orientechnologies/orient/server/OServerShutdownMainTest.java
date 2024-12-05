package com.orientechnologies.orient.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.orientechnologies.common.io.OFileUtils;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.core.YouTrackDBManager;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class OServerShutdownMainTest {

  private OServer server;
  private String prevPassword;
  private String prevOrientHome;

  @Before
  public void startupOServer() throws Exception {

    OLogManager.instance().setConsoleLevel(Level.OFF.getName());
    prevPassword = System.setProperty("YOU_TRACK_DB_ROOT_PASSWORD", "rootPassword");
    prevOrientHome = System.setProperty("YOU_TRACK_DB_HOME", "./target/testhome");

    OServerConfiguration conf = new OServerConfiguration();
    conf.network = new OServerNetworkConfiguration();

    conf.network.protocols = new ArrayList<OServerNetworkProtocolConfiguration>();
    conf.network.protocols.add(
        new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName()));
    conf.network.protocols.add(
        new OServerNetworkProtocolConfiguration("http", ONetworkProtocolHttpDb.class.getName()));

    conf.network.listeners = new ArrayList<OServerNetworkListenerConfiguration>();
    conf.network.listeners.add(new OServerNetworkListenerConfiguration());

    server = new OServer(false);
    server.startup(conf);
    server.activate();

    assertThat(server.isActive()).isTrue();
  }

  @After
  public void tearDown() throws Exception {
    if (server.isActive()) {
      server.shutdown();
    }

    YouTrackDBManager.instance().shutdown();
    OFileUtils.deleteRecursively(new File("./target/testhome"));
    YouTrackDBManager.instance().startup();

    if (prevOrientHome != null) {
      System.setProperty("YOU_TRACK_DB_HOME", prevOrientHome);
    }
    if (prevPassword != null) {
      System.setProperty("YOU_TRACK_DB_ROOT_PASSWORD", prevPassword);
    }
  }

  @Test
  public void shouldShutdownServerWithDirectCall() throws Exception {

    OServerShutdownMain shutdownMain =
        new OServerShutdownMain("localhost", "2424", "root", "rootPassword");
    shutdownMain.connect(5000);

    TimeUnit.SECONDS.sleep(2);
    assertThat(server.isActive()).isFalse();
  }

  @Test
  public void shouldShutdownServerParsingShortArguments() throws Exception {

    OServerShutdownMain.main(
        new String[]{"-h", "localhost", "-P", "2424", "-p", "rootPassword", "-u", "root"});

    TimeUnit.SECONDS.sleep(2);
    assertThat(server.isActive()).isFalse();
  }

  @Test
  public void shouldShutdownServerParsingLongArguments() throws Exception {

    OServerShutdownMain.main(
        new String[]{
            "--host", "localhost", "--ports", "2424", "--password", "rootPassword", "--user", "root"
        });

    TimeUnit.SECONDS.sleep(2);
    assertThat(server.isActive()).isFalse();
  }
}
