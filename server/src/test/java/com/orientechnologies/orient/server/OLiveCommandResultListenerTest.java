package com.orientechnologies.orient.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;

import com.jetbrains.youtrack.db.internal.core.command.OCommandResultListener;
import com.jetbrains.youtrack.db.internal.core.config.YTContextConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ORecordOperation;
import com.jetbrains.youtrack.db.internal.core.query.live.OLiveQueryHook;
import com.jetbrains.youtrack.db.internal.core.query.live.OLiveQueryListener;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.ORecordSerializerNetwork;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.OChannelBinaryServer;
import com.orientechnologies.orient.server.network.protocol.binary.OLiveCommandResultListener;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import com.orientechnologies.orient.server.token.OTokenHandlerImpl;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 *
 */
public class OLiveCommandResultListenerTest extends BaseMemoryInternalDatabase {

  @Mock
  private OServer server;
  @Mock
  private OChannelBinaryServer channelBinary;

  @Mock
  private OLiveQueryListener rawListener;

  private ONetworkProtocolBinary protocol;
  private OClientConnection connection;

  private static class TestResultListener implements OCommandResultListener {

    @Override
    public boolean result(YTDatabaseSessionInternal querySession, Object iRecord) {
      return false;
    }

    @Override
    public void end() {
    }

    @Override
    public Object getResult() {
      return null;
    }
  }

  @Before
  public void beforeTests() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(server.getContextConfiguration()).thenReturn(new YTContextConfiguration());

    OClientConnectionManager manager = new OClientConnectionManager(server);
    protocol = new ONetworkProtocolBinary(server);
    protocol.initVariables(server, channelBinary);
    connection = manager.connect(protocol);
    OTokenHandlerImpl tokenHandler = new OTokenHandlerImpl(new YTContextConfiguration());
    Mockito.when(server.getTokenHandler()).thenReturn(tokenHandler);
    byte[] token = tokenHandler.getSignedBinaryToken(db, db.getUser(), connection.getData());
    connection = manager.connect(protocol, connection, token);
    connection.setDatabase(db);
    connection.getData().setSerializationImpl(ORecordSerializerNetwork.NAME);
    Mockito.when(server.getClientConnectionManager()).thenReturn(manager);
  }

  @Test
  public void testSimpleMessageSend() throws IOException {
    OLiveCommandResultListener listener =
        new OLiveCommandResultListener(server, connection, new TestResultListener());
    ORecordOperation op = new ORecordOperation(new EntityImpl(), ORecordOperation.CREATED);
    listener.onLiveResult(10, op);
    Mockito.verify(channelBinary, atLeastOnce()).writeBytes(Mockito.any(byte[].class));
  }

  @Test
  public void testNetworkError() throws IOException {
    Mockito.when(channelBinary.writeInt(Mockito.anyInt()))
        .thenThrow(new IOException("Mock Exception"));
    OLiveCommandResultListener listener =
        new OLiveCommandResultListener(server, connection, new TestResultListener());
    OLiveQueryHook.subscribe(10, rawListener, db);
    assertTrue(OLiveQueryHook.getOpsReference(db).getQueueThread().hasToken(10));
    ORecordOperation op = new ORecordOperation(new EntityImpl(), ORecordOperation.CREATED);
    listener.onLiveResult(10, op);
    assertFalse(OLiveQueryHook.getOpsReference(db).getQueueThread().hasToken(10));
  }
}
