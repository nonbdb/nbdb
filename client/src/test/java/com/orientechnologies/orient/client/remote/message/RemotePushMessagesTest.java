package com.orientechnologies.orient.client.remote.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.config.StorageClusterConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkV37;
import com.orientechnologies.orient.client.remote.message.push.OStorageConfigurationPayload;
import com.jetbrains.youtrack.db.internal.core.config.StorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.config.StorageEntryConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Test;

/**
 *
 */
public class RemotePushMessagesTest extends DbTestBase {

  @Test
  public void testDistributedConfig() throws IOException {
    MockChannel channel = new MockChannel();
    List<String> hosts = new ArrayList<>();
    hosts.add("one");
    hosts.add("two");
    OPushDistributedConfigurationRequest request = new OPushDistributedConfigurationRequest(hosts);
    request.write(null, channel);
    channel.close();

    OPushDistributedConfigurationRequest readRequest = new OPushDistributedConfigurationRequest();
    readRequest.read(db, channel);
    assertEquals(2, readRequest.getHosts().size());
    assertEquals("one", readRequest.getHosts().get(0));
    assertEquals("two", readRequest.getHosts().get(1));
  }

  @Test
  public void testSchema() throws IOException {
    YouTrackDB youTrackDB = new YouTrackDB(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig());
    youTrackDB.execute(
        "create database test memory users (admin identified by 'admin' role admin)");
    var session = (DatabaseSessionInternal) youTrackDB.open("test", "admin", "admin");

    session.begin();
    EntityImpl schema =
        session.getSharedContext().getSchema().toStream(session).copy();
    session.commit();

    MockChannel channel = new MockChannel();
    OPushSchemaRequest request = new OPushSchemaRequest(schema);
    request.write(session, channel);
    channel.close();

    OPushSchemaRequest readRequest = new OPushSchemaRequest();
    readRequest.read(session, channel);
    assertNotNull(readRequest.getSchema());
  }

  @Test
  public void testIndexManager() throws IOException {
    try (YouTrackDB youTrackDB = new YouTrackDB(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig())) {
      youTrackDB.execute(
          "create database test memory users (admin identified by 'admin' role admin)");
      try (DatabaseSession session = youTrackDB.open("test", "admin", "admin")) {
        session.begin();
        EntityImpl schema =
            ((DatabaseSessionInternal) session).getSharedContext().getIndexManager()
                .toStream((DatabaseSessionInternal) session);

        MockChannel channel = new MockChannel();

        OPushIndexManagerRequest request = new OPushIndexManagerRequest(schema);
        request.write(null, channel);
        channel.close();
        session.commit();

        OPushIndexManagerRequest readRequest = new OPushIndexManagerRequest();
        readRequest.read(db, channel);
        assertNotNull(readRequest.getIndexManager());
      }
    }
  }

  @Test
  public void testStorageConfiguration() throws IOException {
    YouTrackDB youTrackDB = new YouTrackDB(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig());
    youTrackDB.execute(
        "create database test memory users (admin identified by 'admin' role admin)");
    DatabaseSession session = youTrackDB.open("test", "admin", "admin");
    StorageConfiguration configuration =
        ((DatabaseSessionInternal) session).getStorage().getConfiguration();
    session.close();
    youTrackDB.close();
    MockChannel channel = new MockChannel();

    OPushStorageConfigurationRequest request = new OPushStorageConfigurationRequest(configuration);
    request.write(null, channel);
    channel.close();

    OPushStorageConfigurationRequest readRequest = new OPushStorageConfigurationRequest();
    readRequest.read(db, channel);
    OStorageConfigurationPayload readPayload = readRequest.getPayload();
    OStorageConfigurationPayload payload = request.getPayload();
    assertEquals(readPayload.getName(), payload.getName());
    assertEquals(readPayload.getDateFormat(), payload.getDateFormat());
    assertEquals(readPayload.getDateTimeFormat(), payload.getDateTimeFormat());
    assertEquals(readPayload.getVersion(), payload.getVersion());
    assertEquals(readPayload.getDirectory(), payload.getDirectory());
    for (StorageEntryConfiguration readProperty : readPayload.getProperties()) {
      boolean found = false;
      for (StorageEntryConfiguration property : payload.getProperties()) {
        if (readProperty.name.equals(property.name) && readProperty.value.equals(property.value)) {
          found = true;
          break;
        }
      }
      assertTrue(found);
    }
    assertEquals(readPayload.getSchemaRecordId(), payload.getSchemaRecordId());
    assertEquals(readPayload.getIndexMgrRecordId(), payload.getIndexMgrRecordId());
    assertEquals(readPayload.getClusterSelection(), payload.getClusterSelection());
    assertEquals(readPayload.getConflictStrategy(), payload.getConflictStrategy());
    assertEquals(readPayload.isValidationEnabled(), payload.isValidationEnabled());
    assertEquals(readPayload.getLocaleLanguage(), payload.getLocaleLanguage());
    assertEquals(readPayload.getMinimumClusters(), payload.getMinimumClusters());
    assertEquals(readPayload.isStrictSql(), payload.isStrictSql());
    assertEquals(readPayload.getCharset(), payload.getCharset());
    assertEquals(readPayload.getTimeZone(), payload.getTimeZone());
    assertEquals(readPayload.getLocaleCountry(), payload.getLocaleCountry());
    assertEquals(readPayload.getRecordSerializer(), payload.getRecordSerializer());
    assertEquals(readPayload.getRecordSerializerVersion(), payload.getRecordSerializerVersion());
    assertEquals(readPayload.getBinaryFormatVersion(), payload.getBinaryFormatVersion());
    for (StorageClusterConfiguration readCluster : readPayload.getClusters()) {
      boolean found = false;
      for (StorageClusterConfiguration cluster : payload.getClusters()) {
        if (readCluster.getName().equals(cluster.getName())
            && readCluster.getId() == cluster.getId()) {
          found = true;
          break;
        }
      }
      assertTrue(found);
    }
  }

  @Test
  public void testSubscribeRequest() throws IOException {
    MockChannel channel = new MockChannel();

    OSubscribeRequest request =
        new OSubscribeRequest(new OSubscribeLiveQueryRequest("10", new HashMap<>()));
    request.write(null, channel, null);
    channel.close();

    OSubscribeRequest requestRead = new OSubscribeRequest();
    requestRead.read(db, channel, 1, RecordSerializerNetworkV37.INSTANCE);

    assertEquals(request.getPushMessage(), requestRead.getPushMessage());
    assertTrue(requestRead.getPushRequest() instanceof OSubscribeLiveQueryRequest);
  }

  @Test
  public void testSubscribeResponse() throws IOException {
    MockChannel channel = new MockChannel();

    OSubscribeResponse response = new OSubscribeResponse(new OSubscribeLiveQueryResponse(10));
    response.write(null, channel, 1, RecordSerializerNetworkV37.INSTANCE);
    channel.close();

    OSubscribeResponse responseRead = new OSubscribeResponse(new OSubscribeLiveQueryResponse());
    responseRead.read(db, channel, null);

    assertTrue(responseRead.getResponse() instanceof OSubscribeLiveQueryResponse);
    assertEquals(10, ((OSubscribeLiveQueryResponse) responseRead.getResponse()).getMonitorId());
  }

  @Test
  public void testUnsubscribeRequest() throws IOException {
    MockChannel channel = new MockChannel();
    OUnsubscribeRequest request = new OUnsubscribeRequest(new OUnsubscribeLiveQueryRequest(10));
    request.write(null, channel, null);
    channel.close();
    OUnsubscribeRequest readRequest = new OUnsubscribeRequest();
    readRequest.read(db, channel, 0, null);
    assertEquals(
        10, ((OUnsubscribeLiveQueryRequest) readRequest.getUnsubscribeRequest()).getMonitorId());
  }
}
