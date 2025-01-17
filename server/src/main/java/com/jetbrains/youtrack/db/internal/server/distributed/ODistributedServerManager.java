/*
 *
 *  *  Copyright YouTrackDB
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.server.distributed;

import com.jetbrains.youtrack.db.internal.common.util.CallableFunction;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.server.YouTrackDBServer;
import com.jetbrains.youtrack.db.internal.server.distributed.DistributedRequest.EXECUTION_MODE;
import com.jetbrains.youtrack.db.internal.server.distributed.task.RemoteTask;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Server cluster interface to abstract cluster behavior.
 */
public interface ODistributedServerManager {

  String FILE_DISTRIBUTED_DB_CONFIG = "distributed-config.json";

  /**
   * Server status.
   */
  enum NODE_STATUS {
    /**
     * The server was never started or the shutdown is complete.
     */
    OFFLINE,

    /**
     * The server is STARTING.
     */
    STARTING,

    /**
     * The server is ONLINE.
     */
    ONLINE,

    /**
     * The server starts to merge to another cluster.
     */
    MERGING,

    /**
     * The server is shutting down.
     */
    SHUTTINGDOWN
  }

  /**
   * Database status.
   */
  enum DB_STATUS {
    /**
     * The database is not installed. In this status the server does not receive any request.
     */
    NOT_AVAILABLE,

    /**
     * The database has been put in OFFLINE status. In this status the server does not receive any
     * request.
     */
    OFFLINE,

    /**
     * The database is in synchronization status. This status is set when a synchronization (full or
     * delta) is requested. The node tha accepts the synchronization, is in SYNCHRONIZING mode too.
     * During this status the server receive requests that will be enqueue until the database is
     * ready. Server in SYNCHRONIZING status do not concur in the quorum.
     */
    SYNCHRONIZING,

    /**
     * The database is ONLINE as fully operative. During this status the server is considered in the
     * quorum (if the server's role is MASTER)
     */
    ONLINE,

    /**
     * The database is ONLINE, but is not involved in the quorum.
     */
    BACKUP
  }

  /**
   * Checks the node status if it's one of the statuses received as argument.
   *
   * @param iNodeName     Node name
   * @param iDatabaseName Database name
   * @param statuses      vararg of statuses
   * @return true if the node's status is equals to one of the passed statuses, otherwise false
   */
  boolean isNodeStatusEqualsTo(String iNodeName, String iDatabaseName, DB_STATUS... statuses);

  boolean isNodeAvailable(String iNodeName);

  Set<String> getAvailableNodeNames(String databaseName);

  @Deprecated
  String getCoordinatorServer();

  void waitUntilNodeOnline() throws InterruptedException;

  void waitUntilNodeOnline(String nodeName, String databaseName) throws InterruptedException;

  YouTrackDBServer getServerInstance();

  boolean isEnabled();

  ODistributedServerManager registerLifecycleListener(ODistributedLifecycleListener iListener);

  ODistributedServerManager unregisterLifecycleListener(ODistributedLifecycleListener iListener);

  Object executeOnLocalNode(
      DistributedRequestId reqId, RemoteTask task, DatabaseSessionInternal database);

  void executeOnLocalNodeFromRemote(DistributedRequest request);

  RemoteServerController getRemoteServer(String nodeName) throws IOException;

  long getLastClusterChangeOn();

  NODE_STATUS getNodeStatus();

  void setNodeStatus(NODE_STATUS iStatus);

  boolean checkNodeStatus(NODE_STATUS status);

  void removeServer(String nodeLeftName, boolean removeOnlyDynamicServers);

  DB_STATUS getDatabaseStatus(String iNode, String iDatabaseName);

  void setDatabaseStatus(String iNode, String iDatabaseName, DB_STATUS iStatus);

  int getNodesWithStatus(Collection<String> iNodes, String databaseName, DB_STATUS... statuses);

  ODistributedMessageService getMessageService();

  ODistributedDatabase getDatabase(String name);

  ODistributedStrategy getDistributedStrategy();

  void setDistributedStrategy(ODistributedStrategy streatgy);

  // This is always used with deployToCluster=true!
  boolean updateCachedDatabaseConfiguration(
      String iDatabaseName, ModifiableDistributedConfiguration cfg);

  void publishDistributedConfiguration(String iDatabaseName, DistributedConfiguration cfg);

  long getNextMessageIdCounter();

  String getNodeUuidByName(String name);

  void updateLastClusterChange();

  void reassignClustersOwnership(
      String iNode,
      String databaseName,
      ModifiableDistributedConfiguration cfg,
      boolean canCreateNewClusters);

  /**
   * Available means not OFFLINE, so ONLINE or SYNCHRONIZING.
   */
  boolean isNodeAvailable(String iNodeName, String databaseName);

  /**
   * Returns true if the node status is ONLINE.
   */
  boolean isNodeOnline(String iNodeName, String databaseName);

  int getTotalNodes(String iDatabaseName);

  int getAvailableNodes(String iDatabaseName);

  int getAvailableNodes(Collection<String> iNodes, String databaseName);

  boolean isOffline();

  int getLocalNodeId();

  String getLocalNodeName();

  EntityImpl getClusterConfiguration();

  String getNodeNameById(int id);

  int getNodeIdByName(String node);

  EntityImpl getNodeConfigurationByUuid(String iNode, boolean useCache);

  EntityImpl getLocalNodeConfiguration();

  DistributedConfiguration getDatabaseConfiguration(String iDatabaseName);

  /**
   * Sends a distributed request against multiple servers.
   *
   * @param iDatabaseName
   * @param iClusterNames
   * @param iTargetNodeNames
   * @param iTask
   * @param messageId        Message Id as long
   * @param iExecutionMode
   * @param localResult      It's the result of the request executed locally
   * @return
   */
  DistributedResponse sendRequest(
      String iDatabaseName,
      Collection<String> iClusterNames,
      Collection<String> iTargetNodeNames,
      RemoteTask iTask,
      long messageId,
      EXECUTION_MODE iExecutionMode,
      Object localResult);

  DistributedResponse sendRequest(
      String iDatabaseName,
      Collection<String> iClusterNames,
      Collection<String> iTargetNodeNames,
      RemoteTask iTask,
      long messageId,
      EXECUTION_MODE iExecutionMode,
      Object localResult,
      ODistributedResponseManagerFactory responseManagerFactory);

  EntityImpl getStats();

  Throwable convertException(Throwable original);

  List<String> getOnlineNodes(String iDatabaseName);

  boolean installDatabase(
      boolean iStartup, String databaseName, boolean forceDeployment, boolean tryWithDeltaFirst);

  /**
   * Returns the task factory manager. During first connect the minor version of the protocol is
   * used.
   */
  ORemoteTaskFactoryManager getTaskFactoryManager();

  Set<String> getActiveServers();

  /**
   * Returns the cluster-wide time in milliseconds.
   *
   * <p>Cluster tries to keep a cluster-wide time which might be different than the member's own
   * system time. Cluster-wide time is -almost- the same on all members of the cluster.
   */
  long getClusterTime();

  File getDefaultDatabaseConfigFile();

  ODistributedLockManager getLockManagerRequester();

  ODistributedLockManager getLockManagerExecutor();

  /**
   * Executes an operation protected by a distributed lock (one per database).
   *
   * @param <T>            Return type
   * @param databaseName   Database name
   * @param timeoutLocking
   * @param iCallback      Operation @return The operation's result of type T
   */
  <T> T executeInDistributedDatabaseLock(
      String databaseName,
      long timeoutLocking,
      ModifiableDistributedConfiguration lastCfg,
      CallableFunction<T, ModifiableDistributedConfiguration> iCallback);

  /**
   * Returns true if the quorum is present in terms of number of available nodes for full
   * replication only. With sharding, instead, the quorum may depend on the involved clusters.
   *
   * @return
   */
  boolean isWriteQuorumPresent(String databaseName);

  void notifyClients(String databaseName);

  default void messageReceived(DistributedRequest request) {
  }

  default void messagePartitionCalculate(
      DistributedRequest request, Set<Integer> involvedWorkerQueues) {
  }

  default void messageBeforeOp(String op, DistributedRequestId requestId) {
  }

  default void messageAfterOp(String op, DistributedRequestId requestId) {
  }

  default void messageCurrentPayload(DistributedRequestId requestId, Object responsePayload) {
  }

  default void messageProcessStart(DistributedRequest message) {
  }

  default void messageProcessEnd(DistributedRequest iRequest, Object responsePayload) {
  }

  EntityImpl getOnlineDatabaseConfiguration(String databaseName);
}
