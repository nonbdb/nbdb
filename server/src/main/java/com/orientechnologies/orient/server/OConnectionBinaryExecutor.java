package com.orientechnologies.orient.server;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.serialization.types.OBinarySerializer;
import com.orientechnologies.common.serialization.types.OByteSerializer;
import com.orientechnologies.common.serialization.types.ONullSerializer;
import com.orientechnologies.common.util.OCommonConst;
import com.orientechnologies.orient.client.binary.OBinaryRequestExecutor;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.message.OAddClusterRequest;
import com.orientechnologies.orient.client.remote.message.OAddClusterResponse;
import com.orientechnologies.orient.client.remote.message.OBeginTransaction38Request;
import com.orientechnologies.orient.client.remote.message.OBeginTransactionRequest;
import com.orientechnologies.orient.client.remote.message.OBeginTransactionResponse;
import com.orientechnologies.orient.client.remote.message.OBinaryProtocolHelper;
import com.orientechnologies.orient.client.remote.message.OCeilingPhysicalPositionsRequest;
import com.orientechnologies.orient.client.remote.message.OCeilingPhysicalPositionsResponse;
import com.orientechnologies.orient.client.remote.message.OCleanOutRecordRequest;
import com.orientechnologies.orient.client.remote.message.OCleanOutRecordResponse;
import com.orientechnologies.orient.client.remote.message.OCloseQueryRequest;
import com.orientechnologies.orient.client.remote.message.OCloseQueryResponse;
import com.orientechnologies.orient.client.remote.message.OCloseRequest;
import com.orientechnologies.orient.client.remote.message.OCommandRequest;
import com.orientechnologies.orient.client.remote.message.OCommandResponse;
import com.orientechnologies.orient.client.remote.message.OCommit37Request;
import com.orientechnologies.orient.client.remote.message.OCommit37Response;
import com.orientechnologies.orient.client.remote.message.OCommit38Request;
import com.orientechnologies.orient.client.remote.message.OCommitRequest;
import com.orientechnologies.orient.client.remote.message.OCommitResponse;
import com.orientechnologies.orient.client.remote.message.OConnect37Request;
import com.orientechnologies.orient.client.remote.message.OConnectRequest;
import com.orientechnologies.orient.client.remote.message.OConnectResponse;
import com.orientechnologies.orient.client.remote.message.OCountRecordsRequest;
import com.orientechnologies.orient.client.remote.message.OCountRecordsResponse;
import com.orientechnologies.orient.client.remote.message.OCountRequest;
import com.orientechnologies.orient.client.remote.message.OCountResponse;
import com.orientechnologies.orient.client.remote.message.OCreateDatabaseRequest;
import com.orientechnologies.orient.client.remote.message.OCreateDatabaseResponse;
import com.orientechnologies.orient.client.remote.message.OCreateRecordRequest;
import com.orientechnologies.orient.client.remote.message.OCreateRecordResponse;
import com.orientechnologies.orient.client.remote.message.ODistributedConnectRequest;
import com.orientechnologies.orient.client.remote.message.ODistributedConnectResponse;
import com.orientechnologies.orient.client.remote.message.ODistributedStatusRequest;
import com.orientechnologies.orient.client.remote.message.ODistributedStatusResponse;
import com.orientechnologies.orient.client.remote.message.ODropClusterRequest;
import com.orientechnologies.orient.client.remote.message.ODropClusterResponse;
import com.orientechnologies.orient.client.remote.message.ODropDatabaseRequest;
import com.orientechnologies.orient.client.remote.message.ODropDatabaseResponse;
import com.orientechnologies.orient.client.remote.message.OExistsDatabaseRequest;
import com.orientechnologies.orient.client.remote.message.OExistsDatabaseResponse;
import com.orientechnologies.orient.client.remote.message.OFetchTransaction38Request;
import com.orientechnologies.orient.client.remote.message.OFetchTransaction38Response;
import com.orientechnologies.orient.client.remote.message.OFetchTransactionRequest;
import com.orientechnologies.orient.client.remote.message.OFetchTransactionResponse;
import com.orientechnologies.orient.client.remote.message.OFloorPhysicalPositionsRequest;
import com.orientechnologies.orient.client.remote.message.OFloorPhysicalPositionsResponse;
import com.orientechnologies.orient.client.remote.message.OFreezeDatabaseRequest;
import com.orientechnologies.orient.client.remote.message.OFreezeDatabaseResponse;
import com.orientechnologies.orient.client.remote.message.OGetClusterDataRangeRequest;
import com.orientechnologies.orient.client.remote.message.OGetClusterDataRangeResponse;
import com.orientechnologies.orient.client.remote.message.OGetGlobalConfigurationRequest;
import com.orientechnologies.orient.client.remote.message.OGetGlobalConfigurationResponse;
import com.orientechnologies.orient.client.remote.message.OGetRecordMetadataRequest;
import com.orientechnologies.orient.client.remote.message.OGetRecordMetadataResponse;
import com.orientechnologies.orient.client.remote.message.OGetSizeRequest;
import com.orientechnologies.orient.client.remote.message.OGetSizeResponse;
import com.orientechnologies.orient.client.remote.message.OHigherPhysicalPositionsRequest;
import com.orientechnologies.orient.client.remote.message.OHigherPhysicalPositionsResponse;
import com.orientechnologies.orient.client.remote.message.OImportRequest;
import com.orientechnologies.orient.client.remote.message.OImportResponse;
import com.orientechnologies.orient.client.remote.message.OIncrementalBackupRequest;
import com.orientechnologies.orient.client.remote.message.OIncrementalBackupResponse;
import com.orientechnologies.orient.client.remote.message.OListDatabasesRequest;
import com.orientechnologies.orient.client.remote.message.OListDatabasesResponse;
import com.orientechnologies.orient.client.remote.message.OListGlobalConfigurationsRequest;
import com.orientechnologies.orient.client.remote.message.OListGlobalConfigurationsResponse;
import com.orientechnologies.orient.client.remote.message.OLowerPhysicalPositionsRequest;
import com.orientechnologies.orient.client.remote.message.OLowerPhysicalPositionsResponse;
import com.orientechnologies.orient.client.remote.message.OOpen37Request;
import com.orientechnologies.orient.client.remote.message.OOpen37Response;
import com.orientechnologies.orient.client.remote.message.OOpenRequest;
import com.orientechnologies.orient.client.remote.message.OOpenResponse;
import com.orientechnologies.orient.client.remote.message.OQueryNextPageRequest;
import com.orientechnologies.orient.client.remote.message.OQueryRequest;
import com.orientechnologies.orient.client.remote.message.OQueryResponse;
import com.orientechnologies.orient.client.remote.message.OReadRecordRequest;
import com.orientechnologies.orient.client.remote.message.OReadRecordResponse;
import com.orientechnologies.orient.client.remote.message.ORecordExistsRequest;
import com.orientechnologies.orient.client.remote.message.ORecordExistsResponse;
import com.orientechnologies.orient.client.remote.message.OReleaseDatabaseRequest;
import com.orientechnologies.orient.client.remote.message.OReleaseDatabaseResponse;
import com.orientechnologies.orient.client.remote.message.OReloadRequest;
import com.orientechnologies.orient.client.remote.message.OReloadRequest37;
import com.orientechnologies.orient.client.remote.message.OReloadResponse;
import com.orientechnologies.orient.client.remote.message.OReloadResponse37;
import com.orientechnologies.orient.client.remote.message.OReopenRequest;
import com.orientechnologies.orient.client.remote.message.OReopenResponse;
import com.orientechnologies.orient.client.remote.message.ORollbackTransactionRequest;
import com.orientechnologies.orient.client.remote.message.ORollbackTransactionResponse;
import com.orientechnologies.orient.client.remote.message.OSBTCreateTreeRequest;
import com.orientechnologies.orient.client.remote.message.OSBTCreateTreeResponse;
import com.orientechnologies.orient.client.remote.message.OSBTFetchEntriesMajorRequest;
import com.orientechnologies.orient.client.remote.message.OSBTFetchEntriesMajorResponse;
import com.orientechnologies.orient.client.remote.message.OSBTFirstKeyRequest;
import com.orientechnologies.orient.client.remote.message.OSBTFirstKeyResponse;
import com.orientechnologies.orient.client.remote.message.OSBTGetRealBagSizeRequest;
import com.orientechnologies.orient.client.remote.message.OSBTGetRealBagSizeResponse;
import com.orientechnologies.orient.client.remote.message.OSBTGetRequest;
import com.orientechnologies.orient.client.remote.message.OSBTGetResponse;
import com.orientechnologies.orient.client.remote.message.OServerInfoRequest;
import com.orientechnologies.orient.client.remote.message.OServerInfoResponse;
import com.orientechnologies.orient.client.remote.message.OServerQueryRequest;
import com.orientechnologies.orient.client.remote.message.OServerQueryResponse;
import com.orientechnologies.orient.client.remote.message.OSetGlobalConfigurationRequest;
import com.orientechnologies.orient.client.remote.message.OSetGlobalConfigurationResponse;
import com.orientechnologies.orient.client.remote.message.OShutdownRequest;
import com.orientechnologies.orient.client.remote.message.OShutdownResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeDistributedConfigurationRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeDistributedConfigurationResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeFunctionsRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeFunctionsResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeIndexManagerRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeIndexManagerResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeLiveQueryRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeLiveQueryResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeSchemaRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeSchemaResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeSequencesRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeSequencesResponse;
import com.orientechnologies.orient.client.remote.message.OSubscribeStorageConfigurationRequest;
import com.orientechnologies.orient.client.remote.message.OSubscribeStorageConfigurationResponse;
import com.orientechnologies.orient.client.remote.message.OUnsubscribLiveQueryResponse;
import com.orientechnologies.orient.client.remote.message.OUnsubscribeLiveQueryRequest;
import com.orientechnologies.orient.client.remote.message.OUnsubscribeRequest;
import com.orientechnologies.orient.client.remote.message.OUnsubscribeResponse;
import com.orientechnologies.orient.client.remote.message.OUpdateRecordRequest;
import com.orientechnologies.orient.client.remote.message.OUpdateRecordResponse;
import com.orientechnologies.orient.client.remote.message.tx.ORecordOperationRequest;
import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.config.OContextConfiguration;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OLiveQueryMonitor;
import com.orientechnologies.orient.core.db.OxygenDB;
import com.orientechnologies.orient.core.db.OxygenDBInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.exception.OConfigurationException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.fetch.OFetchContext;
import com.orientechnologies.orient.core.fetch.OFetchHelper;
import com.orientechnologies.orient.core.fetch.OFetchListener;
import com.orientechnologies.orient.core.fetch.OFetchPlan;
import com.orientechnologies.orient.core.fetch.remote.ORemoteFetchContext;
import com.orientechnologies.orient.core.fetch.remote.ORemoteFetchListener;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.OSecurityUser;
import com.orientechnologies.orient.core.query.live.OLiveQueryHookV2;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.ORecordAbstract;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.OBlob;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializerFactory;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerNetworkV37;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.parser.OLocalResultSetLifecycleDecorator;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.storage.OPhysicalPosition;
import com.orientechnologies.orient.core.storage.ORecordMetadata;
import com.orientechnologies.orient.core.storage.cluster.OOfflineClusterException;
import com.orientechnologies.orient.core.storage.config.OClusterBasedStorageConfiguration;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperationsManager;
import com.orientechnologies.orient.core.storage.index.sbtree.OTreeInternal;
import com.orientechnologies.orient.core.storage.index.sbtreebonsai.local.OSBTreeBonsai;
import com.orientechnologies.orient.core.storage.ridbag.sbtree.OBonsaiCollectionPointer;
import com.orientechnologies.orient.core.storage.ridbag.sbtree.OSBTreeCollectionManager;
import com.orientechnologies.orient.core.tx.OTransactionOptimistic;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelBinaryProtocol;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ORemoteServerController;
import com.orientechnologies.orient.server.network.protocol.binary.HandshakeInfo;
import com.orientechnologies.orient.server.network.protocol.binary.OAbstractCommandResultListener;
import com.orientechnologies.orient.server.network.protocol.binary.OAsyncCommandResultListener;
import com.orientechnologies.orient.server.network.protocol.binary.OLiveCommandResultListener;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import com.orientechnologies.orient.server.network.protocol.binary.OSyncCommandResultListener;
import com.orientechnologies.orient.server.plugin.OServerPlugin;
import com.orientechnologies.orient.server.tx.OTransactionOptimisticServer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OConnectionBinaryExecutor implements OBinaryRequestExecutor {

  private final OClientConnection connection;
  private final OServer server;
  private final HandshakeInfo handshakeInfo;

  public OConnectionBinaryExecutor(OClientConnection connection, OServer server) {
    this(connection, server, null);
  }

  public OConnectionBinaryExecutor(
      OClientConnection connection, OServer server, HandshakeInfo handshakeInfo) {
    this.connection = connection;
    this.server = server;
    this.handshakeInfo = handshakeInfo;
  }

  @Override
  public OListDatabasesResponse executeListDatabases(OListDatabasesRequest request) {

    Set<String> dbs = server.listDatabases();
    String listener =
        server.getListenerByProtocol(ONetworkProtocolBinary.class).getInboundAddr().toString();
    Map<String, String> toSend = new HashMap<>();
    for (String dbName : dbs) {
      toSend.put(dbName, "remote:" + listener + "/" + dbName);
    }
    return new OListDatabasesResponse(toSend);
  }

  @Override
  public OBinaryResponse executeServerInfo(OServerInfoRequest request) {
    try {
      return new OServerInfoResponse(OServerInfo.getServerInfo(server));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public OBinaryResponse executeDBReload(OReloadRequest request) {
    final ODatabaseSessionInternal db = connection.getDatabase();
    final Collection<String> clusters = db.getClusterNames();

    String[] clusterNames = new String[clusters.size()];
    int[] clusterIds = new int[clusterNames.length];

    int counter = 0;
    for (final String name : clusters) {
      final int clusterId = db.getClusterIdByName(name);
      if (clusterId >= 0) {
        clusterNames[counter] = name;
        clusterIds[counter] = clusterId;
        counter++;
      }
    }

    if (counter < clusters.size()) {
      clusterNames = Arrays.copyOf(clusterNames, counter);
      clusterIds = Arrays.copyOf(clusterIds, counter);
    }

    return new OReloadResponse(clusterNames, clusterIds);
  }

  @Override
  public OBinaryResponse executeDBReload(OReloadRequest37 request) {
    return new OReloadResponse37(connection.getDatabase().getStorage().getConfiguration());
  }

  @Override
  public OBinaryResponse executeCreateDatabase(OCreateDatabaseRequest request) {

    if (server.existsDatabase(request.getDatabaseName())) {
      throw new ODatabaseException(
          "Database named '" + request.getDatabaseName() + "' already exists");
    }
    if (request.getBackupPath() != null && !"".equals(request.getBackupPath().trim())) {
      server.restore(request.getDatabaseName(), request.getBackupPath());
    } else {
      server.createDatabase(
          request.getDatabaseName(),
          ODatabaseType.valueOf(request.getStorageMode().toUpperCase(Locale.ENGLISH)),
          null);
    }
    OLogManager.instance()
        .info(
            this,
            "Created database '%s' of type '%s'",
            request.getDatabaseName(),
            request.getStorageMode());

    // TODO: it should be here an additional check for open with the right user
    connection.setDatabase(
        server
            .getDatabases()
            .openNoAuthenticate(request.getDatabaseName(),
                connection.getServerUser().getName(null)));

    return new OCreateDatabaseResponse();
  }

  @Override
  public OBinaryResponse executeClose(OCloseRequest request) {
    server.getClientConnectionManager().disconnect(connection);
    return null;
  }

  @Override
  public OBinaryResponse executeExistDatabase(OExistsDatabaseRequest request) {
    boolean result = server.existsDatabase(request.getDatabaseName());
    return new OExistsDatabaseResponse(result);
  }

  @Override
  public OBinaryResponse executeDropDatabase(ODropDatabaseRequest request) {

    server.dropDatabase(request.getDatabaseName());
    OLogManager.instance().info(this, "Dropped database '%s'", request.getDatabaseName());
    connection.close();
    return new ODropDatabaseResponse();
  }

  @Override
  public OBinaryResponse executeGetSize(OGetSizeRequest request) {
    var db = connection.getDatabase();
    return new OGetSizeResponse(db.getStorage().getSize(db));
  }

  @Override
  public OBinaryResponse executeCountRecords(OCountRecordsRequest request) {
    var db = connection.getDatabase();
    return new OCountRecordsResponse(db.getStorage().countRecords(db));
  }

  @Override
  public OBinaryResponse executeDistributedStatus(ODistributedStatusRequest request) {
    final ODocument req = request.getStatus();
    ODocument clusterConfig = new ODocument();

    final String operation = req.field("operation");
    if (operation == null) {
      throw new IllegalArgumentException("Cluster operation is null");
    }

    if (operation.equals("status")) {
      final OServerPlugin plugin = server.getPlugin("cluster");
      if (plugin != null && plugin instanceof ODistributedServerManager) {
        clusterConfig = ((ODistributedServerManager) plugin).getClusterConfiguration();
      }
    } else {
      throw new IllegalArgumentException("Cluster operation '" + operation + "' is not supported");
    }

    return new ODistributedStatusResponse(clusterConfig);
  }

  @Override
  public OBinaryResponse executeCountCluster(OCountRequest request) {
    final long count =
        connection
            .getDatabase()
            .countClusterElements(request.getClusterIds(), request.isCountTombstones());
    return new OCountResponse(count);
  }

  @Override
  public OBinaryResponse executeClusterDataRange(OGetClusterDataRangeRequest request) {
    final long[] pos = connection.getDatabase().getClusterDataRange(request.getClusterId());
    return new OGetClusterDataRangeResponse(pos);
  }

  @Override
  public OBinaryResponse executeAddCluster(OAddClusterRequest request) {
    final int num;
    if (request.getRequestedId() < 0) {
      num = connection.getDatabase().addCluster(request.getClusterName());
    } else {
      num = connection.getDatabase().addCluster(request.getClusterName(), request.getRequestedId());
    }

    return new OAddClusterResponse(num);
  }

  @Override
  public OBinaryResponse executeDropCluster(ODropClusterRequest request) {
    final String clusterName = connection.getDatabase().getClusterNameById(request.getClusterId());
    if (clusterName == null) {
      throw new IllegalArgumentException(
          "Cluster "
              + request.getClusterId()
              + " does not exist anymore. Refresh the db structure or just reconnect to the"
              + " database");
    }

    boolean result = connection.getDatabase().dropCluster(clusterName);
    return new ODropClusterResponse(result);
  }

  @Override
  public OBinaryResponse executeGetRecordMetadata(OGetRecordMetadataRequest request) {
    final ORecordMetadata metadata = connection.getDatabase().getRecordMetadata(request.getRid());
    if (metadata != null) {
      return new OGetRecordMetadataResponse(metadata);
    } else {
      throw new ODatabaseException(
          String.format("Record metadata for RID: %s, Not found", request.getRid()));
    }
  }

  @Override
  public OBinaryResponse executeReadRecord(OReadRecordRequest request) {
    final ORecordId rid = request.getRid();
    final String fetchPlanString = request.getFetchPlan();
    boolean ignoreCache = false;
    ignoreCache = request.isIgnoreCache();

    boolean loadTombstones = false;
    loadTombstones = request.isLoadTumbstone();
    OReadRecordResponse response;
    if (rid.getClusterId() == 0 && rid.getClusterPosition() == 0) {
      // @COMPATIBILITY 0.9.25
      // SEND THE DB CONFIGURATION INSTEAD SINCE IT WAS ON RECORD 0:0
      OFetchHelper.checkFetchPlanValid(fetchPlanString);

      final byte[] record =
          ((OClusterBasedStorageConfiguration)
              connection.getDatabase().getStorageInfo().getConfiguration())
              .toStream(connection.getData().protocolVersion, StandardCharsets.UTF_8);

      response = new OReadRecordResponse(OBlob.RECORD_TYPE, 0, record, new HashSet<>());

    } else {
      final ORecordAbstract record = connection.getDatabase()
          .load(rid, fetchPlanString, ignoreCache);
      if (record != null) {
        byte[] bytes = getRecordBytes(connection, record);
        final Set<ORecordAbstract> recordsToSend = new HashSet<>();
        if (record != null) {
          if (fetchPlanString.length() > 0) {
            // BUILD THE SERVER SIDE RECORD TO ACCES TO THE FETCH
            // PLAN
            if (record instanceof ODocument doc) {
              final OFetchPlan fetchPlan = OFetchHelper.buildFetchPlan(fetchPlanString);

              final OFetchListener listener =
                  new ORemoteFetchListener() {
                    @Override
                    protected void sendRecord(ORecordAbstract iLinked) {
                      recordsToSend.add(iLinked);
                    }
                  };
              final OFetchContext context = new ORemoteFetchContext();
              OFetchHelper.fetch(doc, doc, fetchPlan, listener, context, "");
            }
          }
        }
        response =
            new OReadRecordResponse(
                ORecordInternal.getRecordType(record), record.getVersion(), bytes, recordsToSend);
      } else {
        // No Record to send
        response = new OReadRecordResponse((byte) 0, 0, null, null);
      }
    }
    return response;
  }

  @Override
  public OBinaryResponse executeRecordExists(ORecordExistsRequest request) {
    final ORID rid = request.getRecordId();
    final boolean recordExists = connection.getDatabase().exists(rid);
    return new ORecordExistsResponse(recordExists);
  }

  @Override
  public OBinaryResponse executeCreateRecord(OCreateRecordRequest request) {

    final ORecord record = request.getContent();
    ORecordInternal.setIdentity(record, request.getRid());
    ORecordInternal.setVersion(record, 0);
    if (record instanceof ODocument) {
      // Force conversion of value to class for trigger default values.
      ODocumentInternal.autoConvertValueToClass(connection.getDatabase(), (ODocument) record);
    }
    connection.getDatabase().save(record);

    if (request.getMode() < 2) {
      Map<UUID, OBonsaiCollectionPointer> changedIds;
      OSBTreeCollectionManager collectionManager =
          connection.getDatabase().getSbTreeCollectionManager();
      if (collectionManager != null) {
        changedIds = new HashMap<>(collectionManager.changedIds());
        collectionManager.clearChangedIds();
      } else {
        changedIds = new HashMap<>();
      }

      return new OCreateRecordResponse(
          (ORecordId) record.getIdentity(), record.getVersion(), changedIds);
    }
    return null;
  }

  @Override
  public OBinaryResponse executeUpdateRecord(OUpdateRecordRequest request) {

    ODatabaseSessionInternal database = connection.getDatabase();
    final ORecord newRecord = request.getContent();
    ORecordInternal.setIdentity(newRecord, request.getRid());
    ORecordInternal.setVersion(newRecord, request.getVersion());

    ORecordInternal.setContentChanged(newRecord, request.isUpdateContent());
    ORecordInternal.getDirtyManager(newRecord).clearForSave();
    ORecord currentRecord = null;
    if (newRecord instanceof ODocument) {
      try {
        currentRecord = database.load(request.getRid());
      } catch (ORecordNotFoundException e) {
        // MAINTAIN COHERENT THE BEHAVIOR FOR ALL THE STORAGE TYPES
        if (e.getCause() instanceof OOfflineClusterException)
        //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
        {
          throw (OOfflineClusterException) e.getCause();
        }
      }

      if (currentRecord == null) {
        throw new ORecordNotFoundException(request.getRid());
      }

      ((ODocument) currentRecord).merge((ODocument) newRecord, false, false);
      if (request.isUpdateContent()) {
        ((ODocument) currentRecord).setDirty();
      }
    } else {
      currentRecord = newRecord;
    }

    ORecordInternal.setVersion(currentRecord, request.getVersion());

    database.save(currentRecord);

    if (currentRecord
        .getIdentity()
        .toString()
        .equals(database.getStorageInfo().getConfiguration().getIndexMgrRecordId())) {
      // FORCE INDEX MANAGER UPDATE. THIS HAPPENS FOR DIRECT CHANGES FROM REMOTE LIKE IN GRAPH
      database.getMetadata().getIndexManagerInternal().reload(connection.getDatabase());
    }
    final int newVersion = currentRecord.getVersion();

    if (request.getMode() < 2) {
      Map<UUID, OBonsaiCollectionPointer> changedIds;
      OSBTreeCollectionManager collectionManager =
          connection.getDatabase().getSbTreeCollectionManager();
      if (collectionManager != null) {
        changedIds = new HashMap<>(collectionManager.changedIds());
        collectionManager.clearChangedIds();
      } else {
        changedIds = new HashMap<>();
      }

      return new OUpdateRecordResponse(newVersion, changedIds);
    }
    return null;
  }

  @Override
  public OBinaryResponse executeHigherPosition(OHigherPhysicalPositionsRequest request) {
    var db = connection.getDatabase();
    OPhysicalPosition[] nextPositions =
        db
            .getStorage()
            .higherPhysicalPositions(db, request.getClusterId(), request.getClusterPosition());
    return new OHigherPhysicalPositionsResponse(nextPositions);
  }

  @Override
  public OBinaryResponse executeCeilingPosition(OCeilingPhysicalPositionsRequest request) {
    var db = connection.getDatabase();
    final OPhysicalPosition[] previousPositions =
        db.getStorage()
            .ceilingPhysicalPositions(db, request.getClusterId(), request.getPhysicalPosition());
    return new OCeilingPhysicalPositionsResponse(previousPositions);
  }

  @Override
  public OBinaryResponse executeLowerPosition(OLowerPhysicalPositionsRequest request) {
    var db = connection.getDatabase();
    final OPhysicalPosition[] previousPositions =
        db
            .getStorage()
            .lowerPhysicalPositions(db, request.getiClusterId(), request.getPhysicalPosition());
    return new OLowerPhysicalPositionsResponse(previousPositions);
  }

  @Override
  public OBinaryResponse executeFloorPosition(OFloorPhysicalPositionsRequest request) {
    var db = connection.getDatabase();
    final OPhysicalPosition[] previousPositions =
        db
            .getStorage()
            .floorPhysicalPositions(db, request.getClusterId(), request.getPhysicalPosition());
    return new OFloorPhysicalPositionsResponse(previousPositions);
  }

  @Override
  public OBinaryResponse executeCommand(OCommandRequest request) {
    final boolean live = request.isLive();
    final boolean asynch = request.isAsynch();

    OCommandRequestText command = request.getQuery();

    final Map<Object, Object> params = command.getParameters();

    if (asynch && command instanceof OSQLSynchQuery) {
      // CONVERT IT IN ASYNCHRONOUS QUERY
      final OSQLAsynchQuery asynchQuery = new OSQLAsynchQuery(command.getText());
      asynchQuery.setFetchPlan(command.getFetchPlan());
      asynchQuery.setLimit(command.getLimit());
      asynchQuery.setTimeout(command.getTimeoutTime(), command.getTimeoutStrategy());
      asynchQuery.setUseCache(((OSQLSynchQuery) command).isUseCache());
      command = asynchQuery;
    }

    connection.getData().commandDetail = command.getText();

    connection.getData().command = command;
    OAbstractCommandResultListener listener = null;
    OLiveCommandResultListener liveListener = null;

    OCommandResultListener cmdResultListener = command.getResultListener();

    if (live) {
      liveListener = new OLiveCommandResultListener(server, connection, cmdResultListener);
      listener = new OSyncCommandResultListener(null);
      command.setResultListener(liveListener);
    } else {
      if (asynch) {
        listener = new OAsyncCommandResultListener(connection, cmdResultListener);
        command.setResultListener(listener);
      } else {
        listener = new OSyncCommandResultListener(null);
      }
    }

    final long serverTimeout =
        connection
            .getDatabase()
            .getConfiguration()
            .getValueAsLong(OGlobalConfiguration.COMMAND_TIMEOUT);

    if (serverTimeout > 0 && command.getTimeoutTime() > serverTimeout)
    // FORCE THE SERVER'S TIMEOUT
    {
      command.setTimeout(serverTimeout, command.getTimeoutStrategy());
    }

    // REQUEST CAN'T MODIFY THE RESULT, SO IT'S CACHEABLE
    command.setCacheableResult(true);

    // ASSIGNED THE PARSED FETCHPLAN
    var db = connection.getDatabase();
    final OCommandRequestText commandRequest = db.command(command);
    listener.setFetchPlan(commandRequest.getFetchPlan());
    OCommandResponse response;
    if (asynch) {
      // In case of async it execute the request during the write of the response
      response =
          new OCommandResponse(
              null, listener, false, asynch, connection.getDatabase(), command, params);
    } else {
      // SYNCHRONOUS
      final Object result;
      if (params == null) {
        result = commandRequest.execute(db);
      } else {
        result = commandRequest.execute(db, params);
      }

      // FETCHPLAN HAS TO BE ASSIGNED AGAIN, because it can be changed by SQL statement
      listener.setFetchPlan(commandRequest.getFetchPlan());
      boolean isRecordResultSet = true;
      isRecordResultSet = command.isRecordResultSet();
      response =
          new OCommandResponse(
              result,
              listener,
              isRecordResultSet,
              asynch,
              connection.getDatabase(),
              command,
              params);
    }
    return response;
  }

  @Override
  public OBinaryResponse executeCommit(final OCommitRequest request) {
    var recordOperations = request.getOperations();
    var indexChanges = request.getIndexChanges();

    if (!indexChanges.isEmpty()) {
      throw new ODatabaseException("Manual indexes are not supported");
    }

    var database = connection.getDatabase();
    var tx = database.getTransaction();

    if (!tx.isActive()) {
      throw new ODatabaseException("There is no active transaction on server.");
    }
    if (tx.getId() != request.getTxId()) {
      throw new ODatabaseException(
          "Invalid transaction id, expected " + tx.getId() + " but received " + request.getTxId());
    }

    if (!(tx instanceof OTransactionOptimisticServer serverTransaction)) {
      throw new ODatabaseException(
          "Invalid transaction type,"
              + " expected OTransactionOptimisticServer but found "
              + tx.getClass().getName());
    }

    try {
      try {
        serverTransaction.mergeReceivedTransaction(recordOperations);
      } catch (final ORecordNotFoundException e) {
        throw e.getCause() instanceof OOfflineClusterException
            ? (OOfflineClusterException) e.getCause()
            : e;
      }
      try {
        try {
          serverTransaction.commit();
        } catch (final ORecordNotFoundException e) {
          throw e.getCause() instanceof OOfflineClusterException
              ? (OOfflineClusterException) e.getCause()
              : e;
        }
        final OSBTreeCollectionManager collectionManager =
            connection.getDatabase().getSbTreeCollectionManager();
        Map<UUID, OBonsaiCollectionPointer> changedIds = null;
        if (collectionManager != null) {
          changedIds = collectionManager.changedIds();
        }

        return new OCommitResponse(serverTransaction.getTxGeneratedRealRecordIdMap(), changedIds);
      } catch (final RuntimeException e) {
        if (serverTransaction.isActive()) {
          database.rollback(true);
        }

        final OSBTreeCollectionManager collectionManager =
            connection.getDatabase().getSbTreeCollectionManager();
        if (collectionManager != null) {
          collectionManager.clearChangedIds();
        }

        throw e;
      }
    } catch (final RuntimeException e) {
      // Error during TX initialization, possibly index constraints violation.
      if (serverTransaction.isActive()) {
        database.rollback(true);
      }
      throw e;
    }
  }

  @Override
  public OBinaryResponse executeGetGlobalConfiguration(OGetGlobalConfigurationRequest request) {
    final OGlobalConfiguration cfg = OGlobalConfiguration.findByKey(request.getKey());
    String cfgValue = cfg != null ? cfg.isHidden() ? "<hidden>" : cfg.getValueAsString() : "";
    return new OGetGlobalConfigurationResponse(cfgValue);
  }

  @Override
  public OBinaryResponse executeListGlobalConfigurations(OListGlobalConfigurationsRequest request) {
    Map<String, String> configs = new HashMap<>();
    for (OGlobalConfiguration cfg : OGlobalConfiguration.values()) {
      String key;
      try {
        key = cfg.getKey();
      } catch (Exception e) {
        key = "?";
      }

      String value;
      if (cfg.isHidden()) {
        value = "<hidden>";
      } else {
        try {
          OContextConfiguration config =
              connection.getProtocol().getServer().getContextConfiguration();
          value = config.getValueAsString(cfg) != null ? config.getValueAsString(cfg) : "";
        } catch (Exception e) {
          value = "";
        }
      }
      configs.put(key, value);
    }
    return new OListGlobalConfigurationsResponse(configs);
  }

  @Override
  public OBinaryResponse executeFreezeDatabase(OFreezeDatabaseRequest request) {
    ODatabaseSessionInternal database =
        server
            .getDatabases()
            .openNoAuthenticate(request.getName(), connection.getServerUser().getName(null));
    connection.setDatabase(database);

    OLogManager.instance().info(this, "Freezing database '%s'", connection.getDatabase().getURL());

    connection.getDatabase().freeze(true);
    return new OFreezeDatabaseResponse();
  }

  @Override
  public OBinaryResponse executeReleaseDatabase(OReleaseDatabaseRequest request) {
    ODatabaseSessionInternal database =
        server
            .getDatabases()
            .openNoAuthenticate(request.getName(), connection.getServerUser().getName(null));

    connection.setDatabase(database);

    OLogManager.instance().info(this, "Realising database '%s'", connection.getDatabase().getURL());

    connection.getDatabase().release();
    return new OReleaseDatabaseResponse();
  }

  @Override
  public OBinaryResponse executeCleanOutRecord(OCleanOutRecordRequest request) {
    connection.getDatabase().cleanOutRecord(request.getRecordId(), request.getRecordVersion());

    if (request.getMode() < 2) {
      return new OCleanOutRecordResponse(true);
    }
    return null;
  }

  @Override
  public OBinaryResponse executeSBTreeCreate(OSBTCreateTreeRequest request) {
    OBonsaiCollectionPointer collectionPointer = null;
    try {
      final ODatabaseSessionInternal database = connection.getDatabase();
      final OAbstractPaginatedStorage storage = (OAbstractPaginatedStorage) database.getStorage();
      final OAtomicOperationsManager atomicOperationsManager = storage.getAtomicOperationsManager();
      collectionPointer =
          atomicOperationsManager.calculateInsideAtomicOperation(
              null,
              atomicOperation ->
                  connection
                      .getDatabase()
                      .getSbTreeCollectionManager()
                      .createSBTree(request.getClusterId(), atomicOperation, null));
    } catch (IOException e) {
      throw OException.wrapException(new ODatabaseException("Error during ridbag creation"), e);
    }

    return new OSBTCreateTreeResponse(collectionPointer);
  }

  @Override
  public OBinaryResponse executeSBTGet(OSBTGetRequest request) {
    final OSBTreeCollectionManager sbTreeCollectionManager =
        connection.getDatabase().getSbTreeCollectionManager();
    final OSBTreeBonsai<OIdentifiable, Integer> tree =
        sbTreeCollectionManager.loadSBTree(request.getCollectionPointer());
    try {
      final OIdentifiable key = tree.getKeySerializer().deserialize(request.getKeyStream(), 0);

      Integer result = tree.get(key);
      final OBinarySerializer<? super Integer> valueSerializer;
      if (result == null) {
        valueSerializer = ONullSerializer.INSTANCE;
      } else {
        valueSerializer = tree.getValueSerializer();
      }

      byte[] stream = new byte[OByteSerializer.BYTE_SIZE + valueSerializer.getObjectSize(result)];
      OByteSerializer.INSTANCE.serialize(valueSerializer.getId(), stream, 0);
      valueSerializer.serialize(result, stream, OByteSerializer.BYTE_SIZE);
      return new OSBTGetResponse(stream);
    } finally {
      sbTreeCollectionManager.releaseSBTree(request.getCollectionPointer());
    }
  }

  @Override
  public OBinaryResponse executeSBTFirstKey(OSBTFirstKeyRequest request) {

    final OSBTreeCollectionManager sbTreeCollectionManager =
        connection.getDatabase().getSbTreeCollectionManager();
    final OSBTreeBonsai<OIdentifiable, Integer> tree =
        sbTreeCollectionManager.loadSBTree(request.getCollectionPointer());
    byte[] stream;
    try {

      OIdentifiable result = tree.firstKey();
      final OBinarySerializer<? super OIdentifiable> keySerializer;
      if (result == null) {
        keySerializer = ONullSerializer.INSTANCE;
      } else {
        keySerializer = tree.getKeySerializer();
      }

      stream = new byte[OByteSerializer.BYTE_SIZE + keySerializer.getObjectSize(result)];
      OByteSerializer.INSTANCE.serialize(keySerializer.getId(), stream, 0);
      keySerializer.serialize(result, stream, OByteSerializer.BYTE_SIZE);
      return new OSBTFirstKeyResponse(stream);
    } finally {
      sbTreeCollectionManager.releaseSBTree(request.getCollectionPointer());
    }
  }

  @Override
  public OBinaryResponse executeSBTFetchEntriesMajor(
      @SuppressWarnings("rawtypes") OSBTFetchEntriesMajorRequest request) {

    final OSBTreeCollectionManager sbTreeCollectionManager =
        connection.getDatabase().getSbTreeCollectionManager();
    final OSBTreeBonsai<OIdentifiable, Integer> tree =
        sbTreeCollectionManager.loadSBTree(request.getPointer());
    try {
      final OBinarySerializer<OIdentifiable> keySerializer = tree.getKeySerializer();
      OIdentifiable key = keySerializer.deserialize(request.getKeyStream(), 0);

      final OBinarySerializer<Integer> valueSerializer = tree.getValueSerializer();

      OTreeInternal.AccumulativeListener<OIdentifiable, Integer> listener =
          new OTreeInternal.AccumulativeListener<>(request.getPageSize());
      tree.loadEntriesMajor(key, request.isInclusive(), true, listener);
      List<Entry<OIdentifiable, Integer>> result = listener.getResult();
      return new OSBTFetchEntriesMajorResponse<>(keySerializer, valueSerializer, result);
    } finally {
      sbTreeCollectionManager.releaseSBTree(request.getPointer());
    }
  }

  @Override
  public OBinaryResponse executeSBTGetRealSize(OSBTGetRealBagSizeRequest request) {
    final OSBTreeCollectionManager sbTreeCollectionManager =
        connection.getDatabase().getSbTreeCollectionManager();
    final OSBTreeBonsai<OIdentifiable, Integer> tree =
        sbTreeCollectionManager.loadSBTree(request.getCollectionPointer());
    try {
      int realSize = tree.getRealBagSize(request.getChanges());
      return new OSBTGetRealBagSizeResponse(realSize);
    } finally {
      sbTreeCollectionManager.releaseSBTree(request.getCollectionPointer());
    }
  }

  @Override
  public OBinaryResponse executeIncrementalBackup(OIncrementalBackupRequest request) {
    String fileName = connection.getDatabase().incrementalBackup(request.getBackupDirectory());
    return new OIncrementalBackupResponse(fileName);
  }

  @Override
  public OBinaryResponse executeImport(OImportRequest request) {
    List<String> result = new ArrayList<>();
    OLogManager.instance().info(this, "Starting database import");
    ODatabaseImport imp;
    try {
      imp =
          new ODatabaseImport(
              connection.getDatabase(),
              request.getImporPath(),
              iText -> {
                OLogManager.instance().debug(OConnectionBinaryExecutor.this, iText);
                if (iText != null) {
                  result.add(iText);
                }
              });
      imp.setOptions(request.getOptions());
      imp.importDatabase();
      imp.close();
      new File(request.getImporPath()).delete();

    } catch (IOException e) {
      throw OException.wrapException(new ODatabaseException("error on import"), e);
    }
    return new OImportResponse(result);
  }

  @Override
  public OBinaryResponse executeConnect(OConnectRequest request) {
    OBinaryProtocolHelper.checkProtocolVersion(this, request.getProtocolVersion());
    if (request.getProtocolVersion() > 36) {
      throw new OConfigurationException(
          "You can use connect as first operation only for protocol  < 37 please use handshake for"
              + " protocol >= 37");
    }
    connection.getData().driverName = request.getDriverName();
    connection.getData().driverVersion = request.getDriverVersion();
    connection.getData().protocolVersion = request.getProtocolVersion();
    connection.getData().clientId = request.getClientId();
    connection.getData().setSerializationImpl(request.getRecordFormat());

    connection.setTokenBased(request.isTokenBased());
    connection.getData().supportsLegacyPushMessages = request.isSupportPush();
    connection.getData().collectStats = request.isCollectStats();

    if (!request.isTokenBased()
        && !OGlobalConfiguration.NETWORK_BINARY_ALLOW_NO_TOKEN.getValueAsBoolean()) {
      OLogManager.instance()
          .warn(
              this,
              "Session open with token flag false is not supported anymore please use token based"
                  + " sessions");
      throw new OConfigurationException(
          "Session open with token flag false is not supported anymore please use token based"
              + " sessions");
    }

    connection.setServerUser(
        server.authenticateUser(request.getUsername(), request.getPassword(), "server.connect"));

    if (connection.getServerUser() == null) {
      throw new OSecurityAccessException(
          "Wrong user/password to [connect] to the remote OxygenDB Server instance");
    }
    byte[] token = null;
    if (connection.getData().protocolVersion > OChannelBinaryProtocol.PROTOCOL_VERSION_26) {
      connection.getData().serverUsername = connection.getServerUser().getName(null);
      connection.getData().serverUser = true;

      if (Boolean.TRUE.equals(connection.getTokenBased())) {
        token = server.getTokenHandler().getSignedBinaryToken(null, null, connection.getData());
      } else {
        token = OCommonConst.EMPTY_BYTE_ARRAY;
      }
    }

    return new OConnectResponse(connection.getId(), token);
  }

  @Override
  public OBinaryResponse executeConnect37(OConnect37Request request) {
    connection.getData().driverName = handshakeInfo.getDriverName();
    connection.getData().driverVersion = handshakeInfo.getDriverVersion();
    connection.getData().protocolVersion = handshakeInfo.getProtocolVersion();
    connection.getData().setSerializer(handshakeInfo.getSerializer());

    connection.setTokenBased(true);
    connection.getData().supportsLegacyPushMessages = false;
    connection.getData().collectStats = true;

    connection.setServerUser(
        server.authenticateUser(request.getUsername(), request.getPassword(), "server.connect"));

    if (connection.getServerUser() == null) {
      throw new OSecurityAccessException(
          "Wrong user/password to [connect] to the remote OxygenDB Server instance");
    }

    byte[] token = null;
    if (connection.getData().protocolVersion > OChannelBinaryProtocol.PROTOCOL_VERSION_26) {
      connection.getData().serverUsername = connection.getServerUser().getName(null);
      connection.getData().serverUser = true;

      if (Boolean.TRUE.equals(connection.getTokenBased())) {
        token = server.getTokenHandler().getSignedBinaryToken(null, null, connection.getData());
      } else {
        token = OCommonConst.EMPTY_BYTE_ARRAY;
      }
    }

    return new OConnectResponse(connection.getId(), token);
  }

  @Override
  public OBinaryResponse executeDatabaseOpen(OOpenRequest request) {
    OBinaryProtocolHelper.checkProtocolVersion(this, request.getProtocolVersion());
    if (request.getProtocolVersion() > 36) {
      throw new OConfigurationException(
          "You can use open as first operation only for protocol  < 37 please use handshake for"
              + " protocol >= 37");
    }
    connection.getData().driverName = request.getDriverName();
    connection.getData().driverVersion = request.getDriverVersion();
    connection.getData().protocolVersion = request.getProtocolVersion();
    connection.getData().clientId = request.getClientId();
    connection.getData().setSerializationImpl(request.getRecordFormat());
    if (!request.isUseToken()
        && !OGlobalConfiguration.NETWORK_BINARY_ALLOW_NO_TOKEN.getValueAsBoolean()) {
      OLogManager.instance()
          .warn(
              this,
              "Session open with token flag false is not supported anymore please use token based"
                  + " sessions");
      throw new OConfigurationException(
          "Session open with token flag false is not supported anymore please use token based"
              + " sessions");
    }
    connection.setTokenBased(request.isUseToken());
    connection.getData().supportsLegacyPushMessages = request.isSupportsPush();
    connection.getData().collectStats = request.isCollectStats();

    try {
      connection.setDatabase(
          server.openDatabase(
              request.getDatabaseName(),
              request.getUserName(),
              request.getUserPassword(),
              connection.getData()));
    } catch (OException e) {
      server.getClientConnectionManager().disconnect(connection);
      throw e;
    }

    byte[] token = null;

    if (Boolean.TRUE.equals(connection.getTokenBased())) {
      token =
          server
              .getTokenHandler()
              .getSignedBinaryToken(
                  connection.getDatabase(),
                  connection.getDatabase().getUser(),
                  connection.getData());
      // TODO: do not use the parse split getSignedBinaryToken in two methods.
      server.getClientConnectionManager().connect(connection.getProtocol(), connection, token);
    }

    ODatabaseSessionInternal db = connection.getDatabase();
    final Collection<String> clusters = db.getClusterNames();
    final byte[] tokenToSend;
    if (Boolean.TRUE.equals(connection.getTokenBased())) {
      tokenToSend = token;
    } else {
      tokenToSend = OCommonConst.EMPTY_BYTE_ARRAY;
    }

    final OServerPlugin plugin = server.getPlugin("cluster");
    byte[] distriConf = null;
    ODocument distributedCfg;
    if (plugin instanceof ODistributedServerManager) {
      distributedCfg = ((ODistributedServerManager) plugin).getClusterConfiguration();

      final ODistributedConfiguration dbCfg =
          ((ODistributedServerManager) plugin)
              .getDatabaseConfiguration(connection.getDatabase().getName());
      if (dbCfg != null) {
        // ENHANCE SERVER CFG WITH DATABASE CFG
        distributedCfg.field("database", dbCfg.getDocument(), OType.EMBEDDED);
      }
      distriConf = getRecordBytes(connection, distributedCfg);
    }

    String[] clusterNames = new String[clusters.size()];
    int[] clusterIds = new int[clusters.size()];

    int counter = 0;
    for (String name : clusters) {
      final int clusterId = db.getClusterIdByName(name);
      if (clusterId >= 0) {
        clusterNames[counter] = name;
        clusterIds[counter] = clusterId;
        counter++;
      }
    }

    if (counter < clusters.size()) {
      clusterNames = Arrays.copyOf(clusterNames, counter);
      clusterIds = Arrays.copyOf(clusterIds, counter);
    }

    return new OOpenResponse(
        connection.getId(),
        tokenToSend,
        clusterIds,
        clusterNames,
        distriConf,
        OConstants.getVersion());
  }

  @Override
  public OBinaryResponse executeDatabaseOpen37(OOpen37Request request) {
    connection.setTokenBased(true);
    connection.getData().supportsLegacyPushMessages = false;
    connection.getData().collectStats = true;
    connection.getData().driverName = handshakeInfo.getDriverName();
    connection.getData().driverVersion = handshakeInfo.getDriverVersion();
    connection.getData().protocolVersion = handshakeInfo.getProtocolVersion();
    connection.getData().setSerializer(handshakeInfo.getSerializer());
    try {
      connection.setDatabase(
          server.openDatabase(
              request.getDatabaseName(),
              request.getUserName(),
              request.getUserPassword(),
              connection.getData()));
    } catch (OException e) {
      server.getClientConnectionManager().disconnect(connection);
      throw e;
    }

    byte[] token = null;

    token =
        server
            .getTokenHandler()
            .getSignedBinaryToken(
                connection.getDatabase(), connection.getDatabase().getUser(), connection.getData());
    // TODO: do not use the parse split getSignedBinaryToken in two methods.
    server.getClientConnectionManager().connect(connection.getProtocol(), connection, token);

    return new OOpen37Response(connection.getId(), token);
  }

  @Override
  public OBinaryResponse executeShutdown(OShutdownRequest request) {

    OLogManager.instance().info(this, "Received shutdown command from the remote client ");

    final String user = request.getRootUser();
    final String passwd = request.getRootPassword();

    if (server.authenticate(user, passwd, "server.shutdown")) {
      OLogManager.instance()
          .info(this, "Remote client authenticated. Starting shutdown of server...");

      runShutdownInNonDaemonThread();

      return new OShutdownResponse();
    }

    OLogManager.instance()
        .error(this, "Authentication error of remote client: shutdown is aborted.", null);

    throw new OSecurityAccessException("Invalid user/password to shutdown the server");
  }

  private void runShutdownInNonDaemonThread() {
    Thread shutdownThread =
        new Thread("OxygenDB server shutdown thread") {
          public void run() {
            server.shutdown();
          }
        };
    shutdownThread.setDaemon(false);
    shutdownThread.start();
  }

  @Override
  public OBinaryResponse executeReopen(OReopenRequest request) {
    return new OReopenResponse(connection.getId());
  }

  @Override
  public OBinaryResponse executeSetGlobalConfig(OSetGlobalConfigurationRequest request) {

    final OGlobalConfiguration cfg = OGlobalConfiguration.findByKey(request.getKey());

    if (cfg != null) {
      cfg.setValue(request.getValue());
      if (!cfg.isChangeableAtRuntime()) {
        throw new OConfigurationException(
            "Property '"
                + request.getKey()
                + "' cannot be changed at runtime. Change the setting at startup");
      }
    } else {
      throw new OConfigurationException(
          "Property '" + request.getKey() + "' was not found in global configuration");
    }

    return new OSetGlobalConfigurationResponse();
  }

  public static byte[] getRecordBytes(OClientConnection connection, final ORecordAbstract iRecord) {
    var db = connection.getDatabase();
    final byte[] stream;
    String name = connection.getData().getSerializationImpl();
    if (ORecordInternal.getRecordType(iRecord) == ODocument.RECORD_TYPE) {
      ((ODocument) iRecord).deserializeFields();
      ORecordSerializer ser = ORecordSerializerFactory.instance().getFormat(name);
      stream = ser.toStream(db, iRecord);
    } else {
      stream = iRecord.toStream();
    }

    return stream;
  }

  @Override
  public OBinaryResponse executeServerQuery(OServerQueryRequest request) {
    OxygenDB orientdb = server.getContext();

    OResultSet rs;

    if (request.isNamedParams()) {
      rs = orientdb.execute(request.getStatement(), request.getNamedParameters());
    } else {
      rs = orientdb.execute(request.getStatement(), request.getPositionalParameters());
    }

    // copy the result-set to make sure that the execution is successful
    List<OResult> rsCopy = rs.stream().collect(Collectors.toList());

    return new OServerQueryResponse(
        ((OLocalResultSetLifecycleDecorator) rs).getQueryId(),
        false,
        rsCopy,
        rs.getExecutionPlan(),
        false,
        rs.getQueryStats(),
        false);
  }

  @Override
  public OBinaryResponse executeQuery(OQueryRequest request) {
    ODatabaseSessionInternal database = connection.getDatabase();
    OQueryMetadataUpdateListener metadataListener = new OQueryMetadataUpdateListener();
    database.getSharedContext().registerListener(metadataListener);
    if (database.getTransaction().isActive()) {
      ((OTransactionOptimistic) database.getTransaction()).resetChangesTracking();
    }
    OResultSet rs;
    if (OQueryRequest.QUERY == request.getOperationType()) {
      // TODO Assert is sql.
      if (request.isNamedParams()) {
        rs = database.query(request.getStatement(), request.getNamedParameters());
      } else {
        rs = database.query(request.getStatement(), request.getPositionalParameters());
      }
    } else {
      if (OQueryRequest.COMMAND == request.getOperationType()) {
        if (request.isNamedParams()) {
          rs = database.command(request.getStatement(), request.getNamedParameters());
        } else {
          rs = database.command(request.getStatement(), request.getPositionalParameters());
        }
      } else {
        if (request.isNamedParams()) {
          rs =
              database.execute(
                  request.getLanguage(), request.getStatement(), request.getNamedParameters());
        } else {
          rs =
              database.execute(
                  request.getLanguage(), request.getStatement(), request.getPositionalParameters());
        }
      }
    }

    // copy the result-set to make sure that the execution is successful
    Stream<OResult> stream = rs.stream();
    if (database
        .getActiveQueries()
        .containsKey(((OLocalResultSetLifecycleDecorator) rs).getQueryId())) {
      stream = stream.limit(request.getRecordsPerPage());
    }
    List<OResult> rsCopy = stream.collect(Collectors.toList());

    boolean hasNext = rs.hasNext();
    boolean txChanges = false;
    if (database.getTransaction().isActive()) {
      txChanges = ((OTransactionOptimistic) database.getTransaction()).isChanged();
    }
    database.getSharedContext().unregisterListener(metadataListener);

    return new OQueryResponse(
        ((OLocalResultSetLifecycleDecorator) rs).getQueryId(),
        txChanges,
        rsCopy,
        rs.getExecutionPlan(),
        hasNext,
        rs.getQueryStats(),
        metadataListener.isUpdated());
  }

  @Override
  public OBinaryResponse closeQuery(OCloseQueryRequest oQueryRequest) {
    String queryId = oQueryRequest.getQueryId();
    ODatabaseSessionInternal db = connection.getDatabase();
    OResultSet query = db.getActiveQuery(queryId);
    if (query != null) {
      query.close();
    }
    return new OCloseQueryResponse();
  }

  @Override
  public OBinaryResponse executeQueryNextPage(OQueryNextPageRequest request) {
    ODatabaseSessionInternal database = connection.getDatabase();
    OxygenDBInternal orientDB = database.getSharedContext().getOrientDB();
    OLocalResultSetLifecycleDecorator rs =
        (OLocalResultSetLifecycleDecorator) database.getActiveQuery(request.getQueryId());

    if (rs == null) {
      throw new ODatabaseException(
          String.format(
              "No query with id '%s' found probably expired session", request.getQueryId()));
    }

    try {
      orientDB.startCommand(Optional.empty());
      // copy the result-set to make sure that the execution is successful
      List<OResult> rsCopy = new ArrayList<>(request.getRecordsPerPage());
      int i = 0;
      // if it's OInternalResultSet it means that it's a Command, not a Query, so the result has to
      // be
      // sent as it is, not streamed
      while (rs.hasNext() && (rs.isDetached() || i < request.getRecordsPerPage())) {
        rsCopy.add(rs.next());
        i++;
      }
      boolean hasNext = rs.hasNext();
      return new OQueryResponse(
          rs.getQueryId(),
          false,
          rsCopy,
          rs.getExecutionPlan(),
          hasNext,
          rs.getQueryStats(),
          false);
    } finally {
      orientDB.endCommand();
    }
  }

  @Override
  public OBinaryResponse executeBeginTransaction(OBeginTransactionRequest request) {
    var database = connection.getDatabase();
    var tx = database.getTransaction();

    var recordOperations = request.getOperations();
    var indexChanges = request.getIndexChanges();

    if (!indexChanges.isEmpty()) {
      throw new ODatabaseException("Manual indexes are not supported.");
    }

    if (tx.isActive()) {
      if (!(tx instanceof OTransactionOptimisticServer serverTransaction)) {
        throw new ODatabaseException("Non-server based transaction is active");
      }
      if (tx.getId() != request.getTxId()) {
        throw new ODatabaseException(
            "Transaction id mismatch, expected " + tx.getId() + " but got " + request.getTxId());
      }

      try {
        serverTransaction.mergeReceivedTransaction(recordOperations);
      } catch (final ORecordNotFoundException e) {
        throw e.getCause() instanceof OOfflineClusterException
            ? (OOfflineClusterException) e.getCause()
            : e;
      }

      return new OBeginTransactionResponse(
          tx.getId(), serverTransaction.getTxGeneratedRealRecordIdMap());
    }

    database.begin(new OTransactionOptimisticServer(database, request.getTxId()));
    var serverTransaction = (OTransactionOptimisticServer) database.getTransaction();

    try {
      serverTransaction.mergeReceivedTransaction(recordOperations);
    } catch (final ORecordNotFoundException e) {
      throw e.getCause() instanceof OOfflineClusterException
          ? (OOfflineClusterException) e.getCause()
          : e;
    }

    return new OBeginTransactionResponse(
        tx.getId(), serverTransaction.getTxGeneratedRealRecordIdMap());
  }

  @Override
  public OBinaryResponse executeBeginTransaction38(OBeginTransaction38Request request) {
    var database = connection.getDatabase();
    var recordOperations = request.getOperations();

    var indexChanges = request.getIndexChanges();
    if (!indexChanges.isEmpty()) {
      throw new ODatabaseException("Manual indexes are not supported");
    }
    var tx = database.getTransaction();

    if (tx.isActive()) {
      if (tx.getId() != request.getTxId()) {
        throw new ODatabaseException(
            "Transaction id mismatch, expected " + tx.getId() + " but got " + request.getTxId());
      }
      if (!(tx instanceof OTransactionOptimisticServer serverTransaction)) {
        throw new ODatabaseException("Non-server based transaction is active");
      }
      try {
        serverTransaction.mergeReceivedTransaction(recordOperations);
      } catch (final ORecordNotFoundException e) {
        throw e.getCause() instanceof OOfflineClusterException
            ? (OOfflineClusterException) e.getCause()
            : e;
      }

      return new OBeginTransactionResponse(
          tx.getId(), serverTransaction.getTxGeneratedRealRecordIdMap());
    }

    var serverTransaction =
        doExecuteBeginTransaction(request.getTxId(), database, recordOperations);
    return new OBeginTransactionResponse(
        tx.getId(), serverTransaction.getTxGeneratedRealRecordIdMap());
  }

  private static OTransactionOptimisticServer doExecuteBeginTransaction(
      int txId, ODatabaseSessionInternal database, List<ORecordOperationRequest> recordOperations) {
    database.begin(new OTransactionOptimisticServer(database, txId));
    var serverTransaction = (OTransactionOptimisticServer) database.getTransaction();

    try {
      serverTransaction.mergeReceivedTransaction(recordOperations);
    } catch (final ORecordNotFoundException e) {
      throw e.getCause() instanceof OOfflineClusterException
          ? (OOfflineClusterException) e.getCause()
          : e;
    }
    return serverTransaction;
  }

  @Override
  public OBinaryResponse executeCommit38(OCommit38Request request) {
    var recordOperations = request.getOperations();
    var indexChanges = request.getIndexChanges();

    if (!indexChanges.isEmpty()) {
      throw new ODatabaseException("Manual indexes are not supported");
    }

    var database = connection.getDatabase();
    var tx = database.getTransaction();

    if (!tx.isActive()) {
      tx = doExecuteBeginTransaction(request.getTxId(), database, recordOperations);
    }

    if (tx.getId() != request.getTxId()) {
      throw new ODatabaseException(
          "Invalid transaction id, expected " + tx.getId() + " but received " + request.getTxId());
    }

    if (!(tx instanceof OTransactionOptimisticServer serverTransaction)) {
      throw new ODatabaseException(
          "Invalid transaction type,"
              + " expected OTransactionOptimisticServer but found "
              + tx.getClass().getName());
    }

    try {
      try {
        serverTransaction.mergeReceivedTransaction(recordOperations);
      } catch (final ORecordNotFoundException e) {
        throw e.getCause() instanceof OOfflineClusterException
            ? (OOfflineClusterException) e.getCause()
            : e;
      }
      try {
        try {
          database.commit();
        } catch (final ORecordNotFoundException e) {
          throw e.getCause() instanceof OOfflineClusterException
              ? (OOfflineClusterException) e.getCause()
              : e;
        }
        final OSBTreeCollectionManager collectionManager =
            connection.getDatabase().getSbTreeCollectionManager();
        Map<UUID, OBonsaiCollectionPointer> changedIds = null;

        if (collectionManager != null) {
          changedIds = collectionManager.changedIds();
        }

        return new OCommit37Response(serverTransaction.getTxGeneratedRealRecordIdMap(), changedIds);
      } catch (final RuntimeException e) {
        if (serverTransaction.isActive()) {
          database.rollback(true);
        }

        final OSBTreeCollectionManager collectionManager =
            connection.getDatabase().getSbTreeCollectionManager();
        if (collectionManager != null) {
          collectionManager.clearChangedIds();
        }

        throw e;
      }
    } catch (final RuntimeException e) {
      // Error during TX initialization, possibly index constraints violation.
      if (serverTransaction.isActive()) {
        database.rollback(true);
      }
      throw e;
    }
  }

  @Override
  public OBinaryResponse executeCommit37(OCommit37Request request) {
    var recordOperations = request.getOperations();
    var indexChanges = request.getIndexChanges();

    if (indexChanges != null && !indexChanges.isEmpty()) {
      throw new ODatabaseException("Manual indexes are not supported");
    }

    var database = connection.getDatabase();
    var tx = database.getTransaction();

    if (!tx.isActive()) {
      tx = doExecuteBeginTransaction(request.getTxId(), database, recordOperations);
    }

    if (tx.getId() != request.getTxId()) {
      throw new ODatabaseException(
          "Invalid transaction id, expected " + tx.getId() + " but received " + request.getTxId());
    }

    if (!(tx instanceof OTransactionOptimisticServer serverTransaction)) {
      throw new ODatabaseException(
          "Invalid transaction type,"
              + " expected OTransactionOptimisticServer but found "
              + tx.getClass().getName());
    }

    try {
      try {
        serverTransaction.mergeReceivedTransaction(recordOperations);
      } catch (final ORecordNotFoundException e) {
        throw e.getCause() instanceof OOfflineClusterException
            ? (OOfflineClusterException) e.getCause()
            : e;
      }
      try {
        try {
          database.commit();
        } catch (final ORecordNotFoundException e) {
          throw e.getCause() instanceof OOfflineClusterException
              ? (OOfflineClusterException) e.getCause()
              : e;
        }
        final OSBTreeCollectionManager collectionManager =
            connection.getDatabase().getSbTreeCollectionManager();
        Map<UUID, OBonsaiCollectionPointer> changedIds = null;

        if (collectionManager != null) {
          changedIds = collectionManager.changedIds();
        }

        return new OCommit37Response(serverTransaction.getTxGeneratedRealRecordIdMap(), changedIds);
      } catch (final RuntimeException e) {
        if (serverTransaction.isActive()) {
          database.rollback(true);
        }

        final OSBTreeCollectionManager collectionManager =
            connection.getDatabase().getSbTreeCollectionManager();
        if (collectionManager != null) {
          collectionManager.clearChangedIds();
        }

        throw e;
      }
    } catch (final RuntimeException e) {
      // Error during TX initialization, possibly index constraints violation.
      if (serverTransaction.isActive()) {
        database.rollback(true);
      }
      throw e;
    }
  }

  @Override
  public OBinaryResponse executeFetchTransaction(OFetchTransactionRequest request) {
    ODatabaseSessionInternal database = connection.getDatabase();
    if (!database.getTransaction().isActive()) {
      throw new ODatabaseException("No Transaction Active");
    }

    OTransactionOptimistic tx = (OTransactionOptimistic) database.getTransaction();
    if (tx.getId() != request.getTxId()) {
      throw new ODatabaseException(
          "Invalid transaction id, expected " + tx.getId() + " but received " + request.getTxId());
    }

    return new OFetchTransactionResponse(database,
        tx.getId(),
        tx.getRecordOperations(),
        tx.getIndexOperations(),
        tx.getTxGeneratedRealRecordIdMap());
  }

  @Override
  public OBinaryResponse executeFetchTransaction38(OFetchTransaction38Request request) {
    ODatabaseSessionInternal database = connection.getDatabase();
    if (!database.getTransaction().isActive()) {
      throw new ODatabaseException("No Transaction Active");
    }
    OTransactionOptimistic tx = (OTransactionOptimistic) database.getTransaction();
    if (tx.getId() != request.getTxId()) {
      throw new ODatabaseException(
          "Invalid transaction id, expected " + tx.getId() + " but received " + request.getTxId());
    }

    return new OFetchTransaction38Response(database,
        tx.getId(),
        tx.getRecordOperations(),
        Collections.emptyMap(),
        tx.getTxGeneratedRealRecordIdMap(),
        database);
  }

  @Override
  public OBinaryResponse executeRollback(ORollbackTransactionRequest request) {
    ODatabaseSessionInternal database = connection.getDatabase();
    if (database.getTransaction().isActive()) {
      database.rollback(true);
    }
    return new ORollbackTransactionResponse();
  }

  @Override
  public OBinaryResponse executeSubscribe(OSubscribeRequest request) {
    return new OSubscribeResponse(request.getPushRequest().execute(this));
  }

  @Override
  public OBinaryResponse executeUnsubscribe(OUnsubscribeRequest request) {
    return new OUnsubscribeResponse(request.getUnsubscribeRequest().execute(this));
  }

  @Override
  public OBinaryResponse executeSubscribeDistributedConfiguration(
      OSubscribeDistributedConfigurationRequest request) {
    OPushManager manager = server.getPushManager();
    manager.subscribeDistributeConfig((ONetworkProtocolBinary) connection.getProtocol());

    OxygenDBInternal databases = server.getDatabases();
    Set<String> dbs = databases.listLodadedDatabases();
    ODistributedServerManager plugin = server.getPlugin("cluster");
    if (plugin != null) {
      databases.execute(
          () -> {
            for (String db : dbs) {
              plugin.notifyClients(db);
            }
          });
    }
    return new OSubscribeDistributedConfigurationResponse();
  }

  @Override
  public OBinaryResponse executeSubscribeStorageConfiguration(
      OSubscribeStorageConfigurationRequest request) {
    OPushManager manager = server.getPushManager();
    manager.subscribeStorageConfiguration(
        connection.getDatabase(), (ONetworkProtocolBinary) connection.getProtocol());
    return new OSubscribeStorageConfigurationResponse();
  }

  @Override
  public OBinaryResponse executeSubscribeSchema(OSubscribeSchemaRequest request) {
    OPushManager manager = server.getPushManager();
    manager.subscribeSchema(
        connection.getDatabase(), (ONetworkProtocolBinary) connection.getProtocol());
    return new OSubscribeSchemaResponse();
  }

  @Override
  public OBinaryResponse executeSubscribeIndexManager(OSubscribeIndexManagerRequest request) {
    OPushManager manager = server.getPushManager();
    manager.subscribeIndexManager(
        connection.getDatabase(), (ONetworkProtocolBinary) connection.getProtocol());
    return new OSubscribeIndexManagerResponse();
  }

  @Override
  public OBinaryResponse executeSubscribeFunctions(OSubscribeFunctionsRequest request) {
    OPushManager manager = server.getPushManager();
    manager.subscribeFunctions(
        connection.getDatabase(), (ONetworkProtocolBinary) connection.getProtocol());
    return new OSubscribeFunctionsResponse();
  }

  @Override
  public OBinaryResponse executeSubscribeSequences(OSubscribeSequencesRequest request) {
    OPushManager manager = server.getPushManager();
    manager.subscribeSequences(
        connection.getDatabase(), (ONetworkProtocolBinary) connection.getProtocol());
    return new OSubscribeSequencesResponse();
  }

  @Override
  public OBinaryResponse executeUnsubscribeLiveQuery(OUnsubscribeLiveQueryRequest request) {
    ODatabaseSessionInternal database = connection.getDatabase();
    OLiveQueryHookV2.unsubscribe(request.getMonitorId(), database);
    return new OUnsubscribLiveQueryResponse();
  }

  @Override
  public OBinaryResponse executeSubscribeLiveQuery(OSubscribeLiveQueryRequest request) {
    ONetworkProtocolBinary protocol = (ONetworkProtocolBinary) connection.getProtocol();
    OServerLiveQueryResultListener listener =
        new OServerLiveQueryResultListener(protocol, connection.getDatabase().getSharedContext());
    OLiveQueryMonitor monitor =
        connection.getDatabase().live(request.getQuery(), listener, request.getParams());
    listener.setMonitorId(monitor.getMonitorId());
    return new OSubscribeLiveQueryResponse(monitor.getMonitorId());
  }

  @Override
  public OBinaryResponse executeDistributedConnect(ODistributedConnectRequest request) {
    HandshakeInfo handshakeInfo =
        new HandshakeInfo(
            (short) OChannelBinaryProtocol.PROTOCOL_VERSION_38,
            "OxygenDB Distributed",
            "",
            (byte) 0,
            OChannelBinaryProtocol.ERROR_MESSAGE_JAVA);
    ((ONetworkProtocolBinary) connection.getProtocol()).setHandshakeInfo(handshakeInfo);

    // TODO:check auth type
    OSecurityUser serverUser =
        server.authenticateUser(request.getUsername(), request.getPassword(), "server.connect");

    if (serverUser == null) {
      throw new OSecurityAccessException(
          "Wrong user/password to [connect] to the remote OxygenDB Server instance");
    }

    connection.getData().driverName = "OxygenDB Distributed";
    connection.getData().clientId = "OxygenDB Distributed";
    connection.getData().setSerializer(ORecordSerializerNetworkV37.INSTANCE);
    connection.setTokenBased(true);
    connection.getData().supportsLegacyPushMessages = false;
    connection.getData().collectStats = false;
    int chosenProtocolVersion =
        Math.min(
            request.getDistributedProtocolVersion(),
            ORemoteServerController.CURRENT_PROTOCOL_VERSION);
    if (chosenProtocolVersion < ORemoteServerController.MIN_SUPPORTED_PROTOCOL_VERSION) {
      OLogManager.instance()
          .error(
              this,
              "Rejected distributed connection from '%s' too old not supported",
              null,
              connection.getRemoteAddress());
      throw new ODatabaseException("protocol version too old rejected connection");
    } else {
      connection.setServerUser(serverUser);
      connection.getData().serverUsername = serverUser.getName(null);
      connection.getData().serverUser = true;
      byte[] token =
          server.getTokenHandler().getSignedBinaryToken(null, null, connection.getData());

      return new ODistributedConnectResponse(connection.getId(), token, chosenProtocolVersion);
    }
  }
}
