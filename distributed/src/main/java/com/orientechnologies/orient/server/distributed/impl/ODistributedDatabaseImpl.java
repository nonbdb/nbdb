/*
 *
 *  *  Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
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
 *  * For more information: http://orientdb.com
 *
 */
package com.orientechnologies.orient.server.distributed.impl;

import static com.orientechnologies.orient.core.config.OGlobalConfiguration.DISTRIBUTED_TRANSACTION_SEQUENCE_SET_SIZE;

import com.orientechnologies.common.concur.OOfflineNodeException;
import com.orientechnologies.common.concur.lock.OInterruptedException;
import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.common.thread.OSourceTraceExecutorService;
import com.orientechnologies.common.thread.OThreadPoolExecutors;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OSystemDatabase;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.OSyncSource;
import com.orientechnologies.orient.core.tx.OTransactionId;
import com.orientechnologies.orient.core.tx.OTransactionSequenceStatus;
import com.orientechnologies.orient.core.tx.OTxMetadataHolder;
import com.orientechnologies.orient.core.tx.ValidationResult;
import com.orientechnologies.orient.distributed.db.OrientDBDistributed;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedDatabase;
import com.orientechnologies.orient.server.distributed.ODistributedException;
import com.orientechnologies.orient.server.distributed.ODistributedRequest;
import com.orientechnologies.orient.server.distributed.ODistributedRequestId;
import com.orientechnologies.orient.server.distributed.ODistributedResponse;
import com.orientechnologies.orient.server.distributed.ODistributedServerLog;
import com.orientechnologies.orient.server.distributed.ODistributedServerLog.DIRECTION;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.distributed.ODistributedTxContext;
import com.orientechnologies.orient.server.distributed.ORemoteServerController;
import com.orientechnologies.orient.server.distributed.impl.lock.OFreezeGuard;
import com.orientechnologies.orient.server.distributed.impl.lock.OLockGuard;
import com.orientechnologies.orient.server.distributed.impl.lock.OLockManager;
import com.orientechnologies.orient.server.distributed.impl.lock.OLockManagerImpl;
import com.orientechnologies.orient.server.distributed.impl.lock.OTxPromiseManager;
import com.orientechnologies.orient.server.distributed.impl.lock.OnLocksAcquired;
import com.orientechnologies.orient.server.distributed.impl.task.OLockKeySource;
import com.orientechnologies.orient.server.distributed.impl.task.OUnreachableServerLocalTask;
import com.orientechnologies.orient.server.distributed.impl.task.transaction.OTransactionUniqueKey;
import com.orientechnologies.orient.server.distributed.task.ORemoteTask;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Distributed database implementation. There is one instance per database. Each node creates own
 * instance to talk with each others.
 *
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 */
public class ODistributedDatabaseImpl implements ODistributedDatabase {

  public static final String DISTRIBUTED_SYNC_JSON_FILENAME = "distributed-sync.json";
  protected final ODistributedPlugin manager;
  protected final String databaseName;
  private final String localNodeName;
  private final OTxPromiseManager<ORID> recordPromiseManager;
  private final OTxPromiseManager<Object> indexKeyPromiseManager;
  protected Map<ODistributedRequestId, ODistributedTxContext> activeTxContexts =
      new ConcurrentHashMap<>(64);
  private AtomicLong totalSentRequests = new AtomicLong();
  private AtomicLong totalReceivedRequests = new AtomicLong();
  private TimerTask txTimeoutTask = null;
  private volatile boolean running = true;
  private volatile boolean parsing = true;
  private AtomicLong operationsRunnig = new AtomicLong(0);
  private ODistributedSynchronizedSequence sequenceManager;
  private ExecutorService requestExecutor;
  private OLockManager lockManager = new OLockManagerImpl();
  private Set<OTransactionId> inQueue = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private OSyncSource lastValidBackup;
  private volatile DB_STATUS freezePrevStatus;
  private OFreezeGuard freezeGuard;
  private final OrientDBDistributed context;

  public ODistributedDatabaseImpl(
      OrientDBDistributed context, final ODistributedPlugin manager, final String iDatabaseName) {
    this.context = context;
    this.manager = manager;
    this.databaseName = iDatabaseName;
    this.localNodeName = manager.getLocalNodeName();

    initExecutor();

    if (iDatabaseName.equals(OSystemDatabase.SYSTEM_DB_NAME)) {
      recordPromiseManager = null;
      indexKeyPromiseManager = null;
      return;
    }

    startTxTimeoutTimerTask();

    initProfilerHooks();

    int sequenceSize =
        manager
            .getServerInstance()
            .getContextConfiguration()
            .getValueAsInteger(DISTRIBUTED_TRANSACTION_SEQUENCE_SET_SIZE);
    recordPromiseManager = new OTxPromiseManager<>();
    indexKeyPromiseManager = new OTxPromiseManager<>();
    sequenceManager = new ODistributedSynchronizedSequence(localNodeName, sequenceSize);
  }

  public void initProfilerHooks() {
    OProfiler profiler = Orient.instance().getProfiler();
    profiler.registerHookValue(
        "distributed.db." + databaseName + ".msgSent",
        "Number of replication messages sent from current node",
        OProfiler.METRIC_TYPE.COUNTER,
        () -> totalSentRequests.get(),
        "distributed.db.*.msgSent");

    profiler.registerHookValue(
        "distributed.db." + databaseName + ".msgReceived",
        "Number of replication messages received from external nodes",
        OProfiler.METRIC_TYPE.COUNTER,
        () -> totalReceivedRequests.get(),
        "distributed.db.*.msgReceived");

    profiler.registerHookValue(
        "distributed.db." + databaseName + ".activeContexts",
        "Number of active distributed transactions",
        OProfiler.METRIC_TYPE.COUNTER,
        () -> (long) activeTxContexts.size(),
        "distributed.db.*.activeContexts");

    profiler.registerHookValue(
        "distributed.db." + databaseName + ".workerThreads",
        "Number of worker threads",
        OProfiler.METRIC_TYPE.COUNTER,
        () -> (long) getPoolSize(requestExecutor),
        "distributed.db.*.workerThreads");

    profiler.registerHookValue(
        "distributed.db." + databaseName + ".recordLocks",
        "Number of records locked",
        OProfiler.METRIC_TYPE.COUNTER,
        () -> recordPromiseManager.size() + indexKeyPromiseManager.size(),
        "distributed.db.*.recordLocks");
  }

  public static boolean sendResponseBack(
      final Object current,
      final ODistributedServerManager manager,
      final ODistributedRequestId iRequestId,
      Object responsePayload) {

    if (iRequestId.getMessageId() < 0)
    // INTERNAL MSG
    {
      return true;
    }

    final String local = manager.getLocalNodeName();

    final String sender = manager.getNodeNameById(iRequestId.getNodeId());

    final ODistributedResponse response =
        new ODistributedResponse(null, iRequestId, local, sender, responsePayload);

    // TODO: check if using remote channel for local node still makes sense
    //    if (!senderNodeName.equalsIgnoreCase(manager.getLocalNodeName()))
    try {
      // GET THE SENDER'S RESPONSE QUEUE
      final ORemoteServerController remoteSenderServer = manager.getRemoteServer(sender);

      remoteSenderServer.sendResponse(response);

    } catch (Exception e) {
      ODistributedServerLog.error(
          current,
          local,
          sender,
          DIRECTION.OUT,
          "Error on sending response '%s' back (reqId=%s err=%s)",
          e,
          response,
          iRequestId,
          e.toString());
      return false;
    }

    return true;
  }

  public int getPoolSize(ExecutorService service) {
    if (service instanceof ThreadPoolExecutor) {
      return ((ThreadPoolExecutor) service).getPoolSize();
    } else {
      return 0;
    }
  }

  public OTxPromiseManager<ORID> getRecordPromiseManager() {
    return recordPromiseManager;
  }

  public OTxPromiseManager<Object> getIndexKeyPromiseManager() {
    return indexKeyPromiseManager;
  }

  public void startOperation() {
    waitDistributedIsReady();
    operationsRunnig.incrementAndGet();
  }

  public void endOperation() {
    operationsRunnig.decrementAndGet();
  }

  @Override
  public void waitForOnline() {
    try {
      synchronized (this) {
        if (!this.parsing) {
          this.wait(OGlobalConfiguration.DISTRIBUTED_MAX_STARTUP_DELAY.getValueAsLong());
          if (!this.parsing) {
            throw new OOfflineNodeException("Node is offline");
          }
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // IGNORE IT
    }
  }

  public void reEnqueue(
      final int senderNodeId,
      final long msgSequence,
      final String databaseName,
      final ORemoteTask payload,
      int retryCount,
      int autoRetryDelay) {
    context.scheduleOnce(
        new TimerTask() {

          @Override
          public void run() {
            processRequest(
                new ODistributedRequest(
                    getManager(), senderNodeId, msgSequence, databaseName, payload),
                false);
          }
        },
        autoRetryDelay * retryCount);
  }

  /**
   * Distributed requests against the available workers by using one queue per worker. This
   * guarantee the sequence of the operations against the same record cluster.
   */
  public void processRequest(
      final ODistributedRequest request, final boolean waitForAcceptingRequests) {
    if (!running) {
      throw new ODistributedException(
          "Server is going down or is removing the database:'"
              + getDatabaseName()
              + "' discarding");
    }

    final ORemoteTask task = request.getTask();
    if (waitForAcceptingRequests) {
      waitIsReady(task);

      if (!running) {
        throw new ODistributedException(
            "Server is going down or is removing the database:'"
                + getDatabaseName()
                + "' discarding");
      }
    }
    synchronized (this) {
      task.received(request, this);
      manager.messageReceived(request);

      totalReceivedRequests.incrementAndGet();
      if (task instanceof OLockKeySource) {
        SortedSet<ORID> rids = ((OLockKeySource) task).getRids();
        SortedSet<OTransactionUniqueKey> uniqueKeys = ((OLockKeySource) task).getUniqueKeys();
        OTransactionId txId = ((OLockKeySource) task).getTransactionId();

        OnLocksAcquired acquired =
            (guards) -> {
              Runnable executeTask =
                  () -> {
                    try {
                      execute(request);
                    } finally {
                      this.lockManager.unlock(guards);
                    }
                  };
              try {
                this.requestExecutor.submit(executeTask);
              } catch (RejectedExecutionException e) {
                task.finished(this);
                this.lockManager.unlock(guards);
                throw e;
              }
            };
        try {
          this.lockManager.lock(rids, uniqueKeys, txId, acquired);
        } catch (OOfflineNodeException e) {
          task.finished(this);
          throw e;
        }
      } else {
        try {
          this.requestExecutor.submit(
              () -> {
                execute(request);
              });
        } catch (RejectedExecutionException e) {
          task.finished(this);
          throw e;
        }
      }
    }
  }

  public void trackTransactions(OTransactionId id) {
    inQueue.add(id);
  }

  public void untrackTransactions(OTransactionId id) {
    inQueue.remove(id);
  }

  private void execute(ODistributedRequest request) {
    ORemoteTask task = request.getTask();
    try {
      manager.messageProcessStart(request);
      Object response;
      if (task.isUsingDatabase()) {
        try (ODatabaseDocumentInternal db = context.openNoAuthorization(databaseName)) {
          response = this.manager.executeOnLocalNode(request.getId(), task, db);
        }
      } else {
        response = this.manager.executeOnLocalNode(request.getId(), task, null);
      }
      if (task.hasResponse()) {
        sendResponseBack(this, this.manager, request.getId(), response);
      }
      manager.messageProcessEnd(request, response);
    } finally {
      task.finished(this);
    }
  }

  public void waitIsReady(ORemoteTask task) {
    if (task.isNodeOnlineRequired()) {
      waitDistributedIsReady();
    }
  }

  public void waitDistributedIsReady() {
    synchronized (this) {
      if (!parsing) {
        // WAIT FOR PARSING REQUESTS
        while (!parsing && running) {
          try {
            this.wait(1000);
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    }
  }

  @Override
  public void setOnline() {
    fillStatus();
    ODistributedServerLog.info(
        this,
        localNodeName,
        null,
        DIRECTION.NONE,
        "Publishing ONLINE status for database %s.%s...",
        localNodeName,
        databaseName);

    // SET THE NODE.DB AS ONLINE
    manager.setDatabaseStatus(localNodeName, databaseName, DB_STATUS.ONLINE);
    resume();
  }

  public void fillStatus() {
    OAbstractPaginatedStorage storage = context.getStorage(databaseName);

    if (storage != null) {
      sequenceManager.fill(storage.getLastMetadata());
    }
  }

  @Override
  public void unlockResourcesOfServer(
      final ODatabaseDocumentInternal database, final String serverName) {
    final int nodeLeftId = manager.getNodeIdByName(serverName);

    final Iterator<ODistributedTxContext> pendingReqIterator = activeTxContexts.values().iterator();
    while (pendingReqIterator.hasNext()) {
      final ODistributedTxContext pReq = pendingReqIterator.next();
      if (pReq != null && pReq.getReqId().getNodeId() == nodeLeftId) {

        try {
          pReq.rollback(database);
          pReq.destroy();
        } catch (Exception | Error t) {
          // IGNORE IT
          ODistributedServerLog.error(
              this,
              manager.getLocalNodeName(),
              null,
              DIRECTION.NONE,
              "Distributed transaction: error on rolling back transaction (req=%s)",
              pReq.getReqId());
        }
        pendingReqIterator.remove();
      }
    }
  }

  public ValidationResult validate(OTransactionId id) {
    // this check should happen only of destination nodes
    return sequenceManager.validateTransactionId(id);
  }

  @Override
  public OTxMetadataHolder commit(OTransactionId id) {
    return sequenceManager.notifySuccess(id);
  }

  @Override
  public void rollback(OTransactionId id) {
    sequenceManager.notifyFailure(id);
  }

  @Override
  public ODistributedTxContext registerTxContext(
      final ODistributedRequestId reqId, ODistributedTxContext ctx) {
    final ODistributedTxContext prevCtx = activeTxContexts.put(reqId, ctx);
    if (prevCtx != ctx && prevCtx != null) {
      prevCtx.destroy();
    }
    return ctx;
  }

  @Override
  public Optional<OTransactionId> nextId() {
    return sequenceManager.next();
  }

  @Override
  public List<OTransactionId> missingTransactions(OTransactionSequenceStatus lastState) {
    return sequenceManager.missingTransactions(lastState);
  }

  @Override
  public ODistributedTxContext popTxContext(final ODistributedRequestId requestId) {
    final ODistributedTxContext ctx = activeTxContexts.remove(requestId);
    return ctx;
  }

  @Override
  public ODistributedTxContext getTxContext(final ODistributedRequestId requestId) {
    final ODistributedTxContext ctx = activeTxContexts.get(requestId);
    return ctx;
  }

  @Override
  public ODistributedServerManager getManager() {
    return manager;
  }

  public boolean exists() {
    return context.exists(databaseName, null, null);
  }

  @Override
  public void handleUnreachableNode(final String nodeName) {
    if (!running) {
      return;
    }

    final OUnreachableServerLocalTask task = new OUnreachableServerLocalTask(nodeName);
    final ODistributedRequest rollbackRequest =
        new ODistributedRequest(
            null, manager.getLocalNodeId(), manager.getNextMessageIdCounter(), null, task);
    processRequest(rollbackRequest, false);
  }

  @Override
  public String getDatabaseName() {
    return databaseName;
  }

  @Override
  public ODatabaseDocumentInternal getDatabaseInstance() {
    return context.openNoAuthorization(databaseName);
  }

  @Override
  public long getReceivedRequests() {
    return totalReceivedRequests.get();
  }

  @Override
  public long getProcessedRequests() {
    if (requestExecutor instanceof ThreadPoolExecutor) {
      return ((ThreadPoolExecutor) requestExecutor).getCompletedTaskCount();
    } else {
      return 0;
    }
  }

  public void onDropShutdown() {
    // Drop is often called directly from the exeutor so it cannot wait itself to finish
    shutdown(false);
  }

  public void shutdown() {
    shutdown(true);
  }

  public synchronized void shutdown(boolean wait) {
    if (!running) {
      return;
    }
    running = false;

    try {
      if (txTimeoutTask != null) {
        txTimeoutTask.cancel();
      }
      requestExecutor.shutdown();
      if (wait) {
        try {
          requestExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
      }

      activeTxContexts.clear();

      removeProfilerHook();

    } finally {

      final DB_STATUS serverStatus =
          manager.getDatabaseStatus(manager.getLocalNodeName(), databaseName);

      if (serverStatus == DB_STATUS.ONLINE || serverStatus == DB_STATUS.SYNCHRONIZING) {
        try {
          manager.setDatabaseStatus(
              manager.getLocalNodeName(), databaseName, DB_STATUS.NOT_AVAILABLE);
        } catch (Exception e) {
          // IGNORE IT
        }
      }
    }
  }

  public void removeProfilerHook() {
    OProfiler profiler = Orient.instance().getProfiler();
    profiler.unregisterHookValue("distributed.db." + databaseName + ".msgSent");
    profiler.unregisterHookValue("distributed.db." + databaseName + ".msgReceived");
    profiler.unregisterHookValue("distributed.db." + databaseName + ".activeContexts");
    profiler.unregisterHookValue("distributed.db." + databaseName + ".workerThreads");
    profiler.unregisterHookValue("distributed.db." + databaseName + ".recordLocks");
  }

  public void initFirstOpen(ODatabaseDocumentInternal session) {
    ODistributedConfiguration cfg = this.context.getOrInitDistributedConfiguration(session);
    manager.checkNodeInConfiguration(databaseName, cfg);
    setOnline();
  }

  protected String getLocalNodeName() {
    return localNodeName;
  }

  private void initExecutor() {
    // START ALL THE WORKER THREADS (CONFIGURABLE)
    int totalWorkers = OGlobalConfiguration.DISTRIBUTED_DB_WORKERTHREADS.getValueAsInteger();
    if (totalWorkers < 0) {
      throw new ODistributedException(
          "Cannot create configured distributed workers (" + totalWorkers + ")");
    } else if (totalWorkers == 0) {
      // AUTOMATIC
      final int totalDatabases = context.getActiveDatabases().size() + 1;

      final int cpus = Runtime.getRuntime().availableProcessors();

      if (cpus > 1) {
        totalWorkers = cpus / totalDatabases;
      }

      if (totalWorkers == 0) {
        totalWorkers = 1;
      }
    }

    synchronized (this) {
      String name =
          String.format(
              "OrientDB DistributedWorker node=%s db=%s", getLocalNodeName(), databaseName);
      ExecutorService exec =
          OThreadPoolExecutors.newScalingThreadPool(name, 0, totalWorkers, 0, 1, TimeUnit.HOURS);
      if (manager
          .getServerInstance()
          .getContextConfiguration()
          .getValueAsBoolean(OGlobalConfiguration.EXECUTOR_DEBUG_TRACE_SOURCE)) {
        exec = new OSourceTraceExecutorService(exec);
      }

      this.requestExecutor = exec;
    }
  }

  private void startTxTimeoutTimerTask() {
    txTimeoutTask =
        new TimerTask() {
          @Override
          public void run() {
            checkTxTimeout();
          }
        };
  }

  public void checkTxTimeout() {
    ODatabaseDocumentInternal database = null;
    try {
      final long now = System.currentTimeMillis();
      final long timeout = OGlobalConfiguration.DISTRIBUTED_TX_EXPIRE_TIMEOUT.getValueAsLong();

      for (final Iterator<ODistributedTxContext> it = activeTxContexts.values().iterator();
          it.hasNext(); ) {
        if (!isRunning()) {
          break;
        }

        final ODistributedTxContext ctx = it.next();
        if (ctx != null) {
          final long started = ctx.getStartedOn();
          final long elapsed = now - started;
          if (elapsed > timeout) {
            // TRANSACTION EXPIRED, ROLLBACK IT

            if (database == null)
            // GET THE DATABASE THE FIRST TIME
            {
              database = getDatabaseInstance();
            }

            if (database != null) {
              database.activateOnCurrentThread();
            }

            try {
              ctx.cancel(manager, database);

              if (ctx.getReqId().getNodeId() == manager.getLocalNodeId())
              // REQUEST WAS ORIGINATED FROM CURRENT SERVER
              {
                manager.getMessageService().timeoutRequest(ctx.getReqId().getMessageId());
              }

            } catch (Exception t) {
              ODistributedServerLog.info(
                  this,
                  localNodeName,
                  null,
                  DIRECTION.NONE,
                  "Error on rolling back distributed transaction %s on database '%s' (err=%s)",
                  ctx.getReqId(),
                  databaseName,
                  t);
            } finally {
              it.remove();
            }
          }
        }
      }

    } catch (Exception t) {
      // CATCH EVERYTHING TO AVOID THE TIMER IS CANCELED
      ODistributedServerLog.info(
          this,
          localNodeName,
          null,
          DIRECTION.NONE,
          "Error on checking for expired distributed transaction on database '%s'",
          databaseName);
    } finally {
      if (database != null) {
        database.activateOnCurrentThread();
        database.close();
      }
    }
  }

  private boolean isRunning() {
    return running;
  }

  public void suspend() {
    boolean parsing;
    synchronized (this) {
      parsing = this.parsing;
      this.parsing = false;
    }
    if (parsing) {
      while (operationsRunnig.get() != 0) {
        try {

          Thread.sleep(300);
        } catch (InterruptedException e) {
          break;
        }
      }

      recordPromiseManager.reset();
      indexKeyPromiseManager.reset();
    }
    LinkedBlockingQueue<OFreezeGuard> latch = new LinkedBlockingQueue<OFreezeGuard>(1);
    this.lockManager.freeze(
        (guards) -> {
          try {
            latch.put(guards);
          } catch (InterruptedException e) {
            throw new OInterruptedException(e.getMessage());
          }
        });
    try {
      this.freezeGuard = latch.take();
    } catch (InterruptedException e) {
      throw new OInterruptedException(e.getMessage());
    }
  }

  public void resume() {
    synchronized (this) {
      this.parsing = true;
      this.notifyAll();
    }
    if (this.freezeGuard != null) {
      this.freezeGuard.release();
    }
  }

  @Override
  public String dump() {
    final StringBuilder buffer = new StringBuilder(1024);

    buffer.append(
        "\n\nDATABASE '" + databaseName + "' ON SERVER '" + manager.getLocalNodeName() + "'");

    buffer.append("\n- MESSAGES IN QUEUES");
    buffer.append(" (" + getPoolSize(requestExecutor) + " WORKERS):");

    return buffer.toString();
  }

  public Map<ODistributedRequestId, ODistributedTxContext> getActiveTxContexts() {
    return activeTxContexts;
  }

  @Override
  public void validateStatus(OTransactionSequenceStatus status) {
    List<OTransactionId> res = sequenceManager.checkSelfStatus(status);
    res.removeAll(this.inQueue);
    if (!res.isEmpty()) {
      context.execute(
          () -> {
            manager.installDatabase(false, databaseName, true, true);
          });
    }
  }

  @Override
  public Optional<OTransactionSequenceStatus> status() {
    if (sequenceManager == null) {
      return Optional.empty();
    } else {
      return Optional.of(sequenceManager.currentStatus());
    }
  }

  private void runReverseSync() {
    manager.installDatabase(false, databaseName, true, true);
  }

  @Override
  public void checkReverseSync(OTransactionSequenceStatus lastState) {
    List<OTransactionId> res = sequenceManager.checkSelfStatus(lastState);
    if (!res.isEmpty()) {
      context.execute(this::runReverseSync);
    }
  }

  public List<OLockGuard> localLock(OLockKeySource keySource) {
    SortedSet<ORID> rids = keySource.getRids();
    SortedSet<OTransactionUniqueKey> uniqueKeys = keySource.getUniqueKeys();
    OTransactionId txId = keySource.getTransactionId();
    LinkedBlockingQueue<List<OLockGuard>> latch = new LinkedBlockingQueue<List<OLockGuard>>(1);
    this.lockManager.lock(
        rids,
        uniqueKeys,
        txId,
        (guards) -> {
          try {
            latch.put(guards);
          } catch (InterruptedException e) {
            throw new OInterruptedException(e.getMessage());
          }
        });
    try {
      return latch.take();
    } catch (InterruptedException e) {
      throw new OInterruptedException(e.getMessage());
    }
  }

  public void localUnlock(List<OLockGuard> guards) {
    this.lockManager.unlock(guards);
  }

  public OSyncSource getLastValidBackup() {
    return lastValidBackup;
  }

  public void setLastValidBackup(final OSyncSource lastValidBackup) {
    this.lastValidBackup = lastValidBackup;
  }

  public void resetLastValidBackup() {
    if (lastValidBackup != null) {
      lastValidBackup.invalidate();
    }
  }

  public void clearLastValidBackup() {
    if (lastValidBackup != null) {
      lastValidBackup = null;
    }
  }

  public synchronized void freezeStatus() {
    final String localNode = manager.getLocalNodeName();
    freezePrevStatus = manager.getDatabaseStatus(localNode, databaseName);
    if (freezePrevStatus == DB_STATUS.ONLINE)
    // SET STATUS = BACKUP
    {
      manager.setDatabaseStatus(localNode, databaseName, DB_STATUS.BACKUP);
    }
  }

  public synchronized void releaseStatus() {
    if (freezePrevStatus != null) {
      final String localNode = manager.getLocalNodeName();
      manager.setDatabaseStatus(localNode, databaseName, freezePrevStatus);
    }
  }

  public void incSentRequest() {
    this.totalSentRequests.incrementAndGet();
  }

  public Set<String> getAvailableNodesButLocal(
      ODatabaseSession database, Set<String> involvedClusters) {
    final Set<String> nodes =
        context.getDistributedConfiguration(database).getServers(involvedClusters);

    // REMOVE CURRENT NODE BECAUSE IT HAS BEEN ALREADY EXECUTED LOCALLY
    nodes.remove(localNodeName);
    return nodes;
  }
}
