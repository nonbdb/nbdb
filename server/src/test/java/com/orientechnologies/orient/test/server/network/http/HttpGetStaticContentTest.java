package com.orientechnologies.orient.test.server.network.http;

import com.jetbrains.youtrack.db.internal.common.io.IOUtils;
import com.jetbrains.youtrack.db.internal.common.util.CallableFunction;
import com.orientechnologies.orient.server.network.OServerNetworkListener;
import com.orientechnologies.orient.server.network.protocol.http.NetworkProtocolHttpAbstract;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStaticContent;
import java.io.BufferedInputStream;
import java.net.URL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests HTTP "static content" command.
 */
public class HttpGetStaticContentTest extends BaseHttpTest {

  @Before
  public void setupFolder() {
    registerFakeVirtualFolder();
  }

  public void registerFakeVirtualFolder() {
    CallableFunction callableFunction =
        new CallableFunction<Object, String>() {
          @Override
          public Object call(final String iArgument) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final URL url = classLoader.getResource(iArgument);

            if (url != null) {
              final OServerCommandGetStaticContent.OStaticContent content =
                  new OServerCommandGetStaticContent.OStaticContent();
              content.is = new BufferedInputStream(classLoader.getResourceAsStream(iArgument));
              content.contentSize = -1;
              content.type = OServerCommandGetStaticContent.getContentType(url.getFile());
              return content;
            }
            return null;
          }
        };
    final OServerNetworkListener httpListener =
        getServer().getListenerByProtocol(NetworkProtocolHttpAbstract.class);
    final OServerCommandGetStaticContent command =
        (OServerCommandGetStaticContent)
            httpListener.getCommand(OServerCommandGetStaticContent.class);
    command.registerVirtualFolder("fake", callableFunction);
  }

  @Test
  public void testIndexHTML() throws Exception {
    var response = get("fake/index.htm").getResponse();
    Assert.assertEquals(200, response.getCode());

    String expected =
        IOUtils.readStreamAsString(
            this.getClass().getClassLoader().getResourceAsStream("index.htm"));
    String actual = IOUtils.readStreamAsString(response.getEntity().getContent());
    Assert.assertEquals(expected, actual);
  }

  @Override
  public String getDatabaseName() {
    return "httpdb";
  }

  @Before
  public void startServer() throws Exception {
    super.startServer();
  }

  @After
  public void stopServer() throws Exception {
    super.stopServer();
  }
}
