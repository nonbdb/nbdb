/*
 *
 *
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

package com.jetbrains.youtrack.db.internal.core.storage.impl.local;

import com.google.common.util.concurrent.Striped;
import com.jetbrains.youtrack.db.internal.common.concur.YTNeedRetryException;
import com.jetbrains.youtrack.db.internal.common.concur.lock.ScalableRWLock;
import com.jetbrains.youtrack.db.internal.common.concur.lock.YTInterruptedException;
import com.jetbrains.youtrack.db.internal.common.concur.lock.YTModificationOperationProhibitedException;
import com.jetbrains.youtrack.db.internal.common.exception.YTException;
import com.jetbrains.youtrack.db.internal.common.exception.YTHighLevelException;
import com.jetbrains.youtrack.db.internal.common.io.OIOException;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.profiler.ModifiableLongProfileHookValue;
import com.jetbrains.youtrack.db.internal.common.profiler.OProfiler.METRIC_TYPE;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OBinarySerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OIntegerSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OUTF8Serializer;
import com.jetbrains.youtrack.db.internal.common.thread.OThreadPoolExecutors;
import com.jetbrains.youtrack.db.internal.common.types.OModifiableBoolean;
import com.jetbrains.youtrack.db.internal.common.types.OModifiableLong;
import com.jetbrains.youtrack.db.internal.common.util.OCallable;
import com.jetbrains.youtrack.db.internal.common.util.OCommonConst;
import com.jetbrains.youtrack.db.internal.common.util.ORawPair;
import com.jetbrains.youtrack.db.internal.core.OConstants;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBManager;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandExecutor;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequestText;
import com.jetbrains.youtrack.db.internal.core.command.OCommandOutputListener;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.config.IndexEngineData;
import com.jetbrains.youtrack.db.internal.core.config.OStorageClusterConfiguration;
import com.jetbrains.youtrack.db.internal.core.config.OStorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.config.OStorageConfigurationUpdateListener;
import com.jetbrains.youtrack.db.internal.core.config.YTContextConfiguration;
import com.jetbrains.youtrack.db.internal.core.conflict.ORecordConflictStrategy;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseListener;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.OCurrentStorageComponentsFactory;
import com.jetbrains.youtrack.db.internal.core.db.record.ORecordOperation;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.ORidBagDeleter;
import com.jetbrains.youtrack.db.internal.core.encryption.OEncryption;
import com.jetbrains.youtrack.db.internal.core.encryption.OEncryptionFactory;
import com.jetbrains.youtrack.db.internal.core.encryption.impl.ONothingEncryption;
import com.jetbrains.youtrack.db.internal.core.exception.OInvalidIndexEngineIdException;
import com.jetbrains.youtrack.db.internal.core.exception.YTClusterDoesNotExistException;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommitSerializationException;
import com.jetbrains.youtrack.db.internal.core.exception.YTConcurrentCreateException;
import com.jetbrains.youtrack.db.internal.core.exception.YTConcurrentModificationException;
import com.jetbrains.youtrack.db.internal.core.exception.YTConfigurationException;
import com.jetbrains.youtrack.db.internal.core.exception.YTDatabaseException;
import com.jetbrains.youtrack.db.internal.core.exception.YTFastConcurrentModificationException;
import com.jetbrains.youtrack.db.internal.core.exception.YTInternalErrorException;
import com.jetbrains.youtrack.db.internal.core.exception.YTInvalidDatabaseNameException;
import com.jetbrains.youtrack.db.internal.core.exception.YTInvalidInstanceIdException;
import com.jetbrains.youtrack.db.internal.core.exception.YTRecordNotFoundException;
import com.jetbrains.youtrack.db.internal.core.exception.YTRetryQueryException;
import com.jetbrains.youtrack.db.internal.core.exception.YTStorageDoesNotExistException;
import com.jetbrains.youtrack.db.internal.core.exception.YTStorageException;
import com.jetbrains.youtrack.db.internal.core.exception.YTStorageExistsException;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.index.OIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.OIndexInternal;
import com.jetbrains.youtrack.db.internal.core.index.OIndexKeyUpdater;
import com.jetbrains.youtrack.db.internal.core.index.OIndexManagerAbstract;
import com.jetbrains.youtrack.db.internal.core.index.OIndexMetadata;
import com.jetbrains.youtrack.db.internal.core.index.OIndexes;
import com.jetbrains.youtrack.db.internal.core.index.ORuntimeKeyIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.YTIndexException;
import com.jetbrains.youtrack.db.internal.core.index.engine.IndexEngineValidator;
import com.jetbrains.youtrack.db.internal.core.index.engine.IndexEngineValuesTransformer;
import com.jetbrains.youtrack.db.internal.core.index.engine.OBaseIndexEngine;
import com.jetbrains.youtrack.db.internal.core.index.engine.OIndexEngine;
import com.jetbrains.youtrack.db.internal.core.index.engine.OMultiValueIndexEngine;
import com.jetbrains.youtrack.db.internal.core.index.engine.OSingleValueIndexEngine;
import com.jetbrains.youtrack.db.internal.core.index.engine.OV1IndexEngine;
import com.jetbrains.youtrack.db.internal.core.index.engine.v1.OCellBTreeMultiValueIndexEngine;
import com.jetbrains.youtrack.db.internal.core.index.engine.v1.OCellBTreeSingleValueIndexEngine;
import com.jetbrains.youtrack.db.internal.core.metadata.OMetadataDefault;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass.INDEX_TYPE;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTImmutableClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.metadata.security.YTSecurityUser;
import com.jetbrains.youtrack.db.internal.core.query.QueryAbstract;
import com.jetbrains.youtrack.db.internal.core.record.ORecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.ORecordVersionHelper;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.Blob;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.ODocumentInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.index.OCompositeKeySerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.ORecordSerializer;
import com.jetbrains.youtrack.db.internal.core.sharding.auto.OAutoShardingIndexEngine;
import com.jetbrains.youtrack.db.internal.core.storage.IdentifiableStorage;
import com.jetbrains.youtrack.db.internal.core.storage.OCluster;
import com.jetbrains.youtrack.db.internal.core.storage.OCluster.ATTRIBUTES;
import com.jetbrains.youtrack.db.internal.core.storage.OPhysicalPosition;
import com.jetbrains.youtrack.db.internal.core.storage.ORawBuffer;
import com.jetbrains.youtrack.db.internal.core.storage.ORecordCallback;
import com.jetbrains.youtrack.db.internal.core.storage.ORecordMetadata;
import com.jetbrains.youtrack.db.internal.core.storage.OStorageOperationResult;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.cache.OCacheEntry;
import com.jetbrains.youtrack.db.internal.core.storage.cache.OPageDataVerificationError;
import com.jetbrains.youtrack.db.internal.core.storage.cache.OReadCache;
import com.jetbrains.youtrack.db.internal.core.storage.cache.OWriteCache;
import com.jetbrains.youtrack.db.internal.core.storage.cache.local.OBackgroundExceptionListener;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.OOfflineCluster;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.PaginatedCluster;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.PaginatedCluster.RECORD_STATUS;
import com.jetbrains.youtrack.db.internal.core.storage.config.OClusterBasedStorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.ORecordSerializationContext;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.OStorageTransaction;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperationsTable;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.OAtomicOperationsManager;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.base.ODurablePage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.MetaDataRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OAtomicUnitEndRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OAtomicUnitStartMetadataRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OAtomicUnitStartRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OFileCreatedWALRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OFileDeletedWALRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OHighLevelTransactionChangeRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.ONonTxOperationPerformedWALRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OOperationUnitRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OPaginatedClusterFactory;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OUpdatePageRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OWALPageBrokenException;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OWALRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.OWriteAheadLog;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.common.EmptyWALRecord;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.common.WriteableWALRecord;
import com.jetbrains.youtrack.db.internal.core.storage.index.engine.OHashTableIndexEngine;
import com.jetbrains.youtrack.db.internal.core.storage.index.engine.OSBTreeIndexEngine;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtreebonsai.local.OSBTreeBonsaiLocal;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.OBonsaiCollectionPointer;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.OIndexRIDContainerSBTree;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.OSBTreeCollectionManager;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.OSBTreeCollectionManagerShared;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.OSBTreeRidBag;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionData;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionId;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionIndexChanges;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionIndexChangesPerKey;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionIndexChangesPerKey.OTransactionIndexEntry;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionInternal;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionOptimistic;
import com.jetbrains.youtrack.db.internal.core.tx.OTxMetadataHolder;
import com.jetbrains.youtrack.db.internal.core.tx.OTxMetadataHolderImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @since 28.03.13
 */
public abstract class AbstractPaginatedStorage
    implements OCheckpointRequestListener,
    IdentifiableStorage,
    OBackgroundExceptionListener,
    OFreezableStorageComponent,
    OPageIsBrokenListener,
    Storage {

  private static final int WAL_RESTORE_REPORT_INTERVAL = 30 * 1000; // milliseconds

  private static final Comparator<ORecordOperation> COMMIT_RECORD_OPERATION_COMPARATOR =
      Comparator.comparing(
          o -> {
            return o.record.getIdentity();
          });
  public static final ThreadGroup storageThreadGroup;

  protected static final ScheduledExecutorService fuzzyCheckpointExecutor;

  static {
    ThreadGroup parentThreadGroup = Thread.currentThread().getThreadGroup();

    final ThreadGroup parentThreadGroupBackup = parentThreadGroup;

    boolean found = false;

    while (parentThreadGroup.getParent() != null) {
      if (parentThreadGroup.equals(YouTrackDBManager.instance().getThreadGroup())) {
        parentThreadGroup = parentThreadGroup.getParent();
        found = true;
        break;
      } else {
        parentThreadGroup = parentThreadGroup.getParent();
      }
    }

    if (!found) {
      parentThreadGroup = parentThreadGroupBackup;
    }

    storageThreadGroup = new ThreadGroup(parentThreadGroup, "YouTrackDB Storage");

    fuzzyCheckpointExecutor =
        OThreadPoolExecutors.newSingleThreadScheduledPool("Fuzzy Checkpoint", storageThreadGroup);
  }

  protected volatile OSBTreeCollectionManagerShared sbTreeCollectionManager;

  /**
   * Lock is used to atomically update record versions.
   */
  private final Striped<Lock> recordVersionManager = Striped.lazyWeakLock(1024);

  private final Map<String, OCluster> clusterMap = new HashMap<>();
  private final List<OCluster> clusters = new CopyOnWriteArrayList<>();

  private volatile ThreadLocal<OStorageTransaction> transaction;
  private final AtomicBoolean walVacuumInProgress = new AtomicBoolean();

  protected volatile OWriteAheadLog writeAheadLog;
  @Nullable
  private OStorageRecoverListener recoverListener;

  protected volatile OReadCache readCache;
  protected volatile OWriteCache writeCache;

  private volatile ORecordConflictStrategy recordConflictStrategy =
      YouTrackDBManager.instance().getRecordConflictStrategy().getDefaultImplementation();

  private volatile int defaultClusterId = -1;
  protected volatile OAtomicOperationsManager atomicOperationsManager;
  private volatile boolean wereNonTxOperationsPerformedInPreviousOpen;
  private final int id;

  private final Map<String, OBaseIndexEngine> indexEngineNameMap = new HashMap<>();
  private final List<OBaseIndexEngine> indexEngines = new ArrayList<>();
  private final AtomicOperationIdGen idGen = new AtomicOperationIdGen();

  private boolean wereDataRestoredAfterOpen;
  private UUID uuid;
  private volatile byte[] lastMetadata = null;

  private final OModifiableLong recordCreated = new OModifiableLong();
  private final OModifiableLong recordUpdated = new OModifiableLong();
  private final OModifiableLong recordRead = new OModifiableLong();
  private final OModifiableLong recordDeleted = new OModifiableLong();

  private final OModifiableLong recordScanned = new OModifiableLong();
  private final OModifiableLong recordRecycled = new OModifiableLong();
  private final OModifiableLong recordConflict = new OModifiableLong();
  private final OModifiableLong txBegun = new OModifiableLong();
  private final OModifiableLong txCommit = new OModifiableLong();
  private final OModifiableLong txRollback = new OModifiableLong();

  private final AtomicInteger sessionCount = new AtomicInteger(0);
  private volatile long lastCloseTime = System.currentTimeMillis();

  protected static final String DATABASE_INSTANCE_ID = "databaseInstenceId";

  protected AtomicOperationsTable atomicOperationsTable;
  protected final String url;
  protected final ScalableRWLock stateLock;

  protected volatile OStorageConfiguration configuration;
  protected volatile OCurrentStorageComponentsFactory componentsFactory;
  protected String name;
  private final AtomicLong version = new AtomicLong();

  protected volatile STATUS status = STATUS.CLOSED;

  protected AtomicReference<Throwable> error = new AtomicReference<>(null);
  protected YouTrackDBInternal context;
  private volatile CountDownLatch migration = new CountDownLatch(1);

  private volatile int backupRunning = 0;
  private volatile int ddlRunning = 0;

  protected final Lock backupLock = new ReentrantLock();
  protected final Condition backupIsDone = backupLock.newCondition();

  public AbstractPaginatedStorage(
      final String name, final String filePath, final int id, YouTrackDBInternal context) {
    this.context = context;
    this.name = checkName(name);

    url = filePath;

    stateLock = new ScalableRWLock();

    this.id = id;
    sbTreeCollectionManager = new OSBTreeCollectionManagerShared(this);

    registerProfilerHooks();
  }

  protected static String normalizeName(String name) {
    final int firstIndexOf = name.lastIndexOf('/');
    final int secondIndexOf = name.lastIndexOf(File.separator);

    if (firstIndexOf >= 0 || secondIndexOf >= 0) {
      return name.substring(Math.max(firstIndexOf, secondIndexOf) + 1);
    } else {
      return name;
    }
  }

  public static String checkName(String name) {
    name = normalizeName(name);

    Pattern pattern = Pattern.compile("^\\p{L}[\\p{L}\\d_$-]*$");
    Matcher matcher = pattern.matcher(name);
    boolean isValid = matcher.matches();
    if (!isValid) {
      throw new YTInvalidDatabaseNameException(
          "Invalid name for database. ("
              + name
              + ") Name can contain only letters, numbers, underscores and dashes. "
              + "Name should start with letter.");
    }

    return name;
  }

  @Override
  @Deprecated
  public Storage getUnderlying() {
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getURL() {
    return url;
  }

  @Override
  public void close(YTDatabaseSessionInternal session) {
    var sessions = sessionCount.decrementAndGet();

    if (sessions < 0) {
      throw new YTStorageException(
          "Amount of closed sessions in storage "
              + name
              + " is bigger than amount of open sessions");
    }
    lastCloseTime = System.currentTimeMillis();
  }

  public long getSessionsCount() {
    return sessionCount.get();
  }

  public long getLastCloseTime() {
    return lastCloseTime;
  }

  @Override
  public boolean dropCluster(YTDatabaseSessionInternal session, final String iClusterName) {
    return dropCluster(session, getClusterIdByName(iClusterName));
  }

  @Override
  public long countRecords(YTDatabaseSessionInternal session) {
    long tot = 0;

    for (OCluster c : getClusterInstances()) {
      if (c != null) {
        tot += c.getEntries() - c.getTombstonesCount();
      }
    }

    return tot;
  }

  @Override
  public String toString() {
    return url != null ? url : "?";
  }

  @Override
  public STATUS getStatus() {
    return status;
  }

  @Deprecated
  @Override
  public boolean isDistributed() {
    return false;
  }

  @Override
  public boolean isAssigningClusterIds() {
    return true;
  }

  @Override
  public OCurrentStorageComponentsFactory getComponentsFactory() {
    return componentsFactory;
  }

  @Override
  public long getVersion() {
    return version.get();
  }

  @Override
  public void shutdown() {
    stateLock.writeLock().lock();
    try {
      doShutdown();
    } catch (final IOException e) {
      final String message = "Error on closing of storage '" + name;
      LogManager.instance().error(this, message, e);

      throw YTException.wrapException(new YTStorageException(message), e);
    } finally {
      stateLock.writeLock().unlock();
    }
  }

  private static void checkPageSizeAndRelatedParametersInGlobalConfiguration() {
    final int pageSize = GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getValueAsInteger() * 1024;
    int maxKeySize = GlobalConfiguration.SBTREE_MAX_KEY_SIZE.getValueAsInteger();
    var bTreeMaxKeySize = (int) (pageSize * 0.3);

    if (maxKeySize <= 0) {
      maxKeySize = bTreeMaxKeySize;
      GlobalConfiguration.SBTREE_MAX_KEY_SIZE.setValue(maxKeySize);
    }

    if (maxKeySize > bTreeMaxKeySize) {
      throw new YTStorageException(
          "Value of parameter "
              + GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getKey()
              + " should be at least 4 times bigger than value of parameter "
              + GlobalConfiguration.SBTREE_MAX_KEY_SIZE.getKey()
              + " but real values are :"
              + GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getKey()
              + " = "
              + pageSize
              + " , "
              + GlobalConfiguration.SBTREE_MAX_KEY_SIZE.getKey()
              + " = "
              + maxKeySize);
    }
  }

  private static TreeMap<String, OTransactionIndexChanges> getSortedIndexOperations(
      final OTransactionInternal clientTx) {
    return new TreeMap<>(clientTx.getIndexOperations());
  }

  @Override
  public final void open(
      YTDatabaseSessionInternal remote, final String iUserName,
      final String iUserPassword,
      final YTContextConfiguration contextConfiguration) {
    open(contextConfiguration);
  }

  public final void open(final YTContextConfiguration contextConfiguration) {
    checkPageSizeAndRelatedParametersInGlobalConfiguration();
    try {
      stateLock.readLock().lock();
      try {
        if (status == STATUS.OPEN || isInError()) {
          // ALREADY OPENED: THIS IS THE CASE WHEN A STORAGE INSTANCE IS
          // REUSED

          sessionCount.incrementAndGet();
          return;
        }

      } finally {
        stateLock.readLock().unlock();
      }

      try {
        stateLock.writeLock().lock();
        try {
          if (status == STATUS.MIGRATION) {
            try {
              // Yes this look inverted but is correct.
              stateLock.writeLock().unlock();
              migration.await();
            } finally {
              stateLock.writeLock().lock();
            }
          }

          if (status == STATUS.OPEN || isInError())
          // ALREADY OPENED: THIS IS THE CASE WHEN A STORAGE INSTANCE IS
          // REUSED
          {
            return;
          }

          if (status != STATUS.CLOSED) {
            throw new YTStorageException(
                "Storage " + name + " is in wrong state " + status + " and can not be opened.");
          }

          if (!exists()) {
            throw new YTStorageDoesNotExistException(
                "Cannot open the storage '" + name + "' because it does not exist in path: " + url);
          }

          readIv();

          initWalAndDiskCache(contextConfiguration);
          transaction = new ThreadLocal<>();

          final OStartupMetadata startupMetadata = checkIfStorageDirty();
          final long lastTxId = startupMetadata.lastTxId;
          if (lastTxId > 0) {
            idGen.setStartId(lastTxId + 1);
          } else {
            idGen.setStartId(0);
          }

          atomicOperationsTable =
              new AtomicOperationsTable(
                  contextConfiguration.getValueAsInteger(
                      GlobalConfiguration.STORAGE_ATOMIC_OPERATIONS_TABLE_COMPACTION_LIMIT),
                  idGen.getLastId() + 1);
          atomicOperationsManager = new OAtomicOperationsManager(this, atomicOperationsTable);

          recoverIfNeeded();

          atomicOperationsManager.executeInsideAtomicOperation(
              null,
              atomicOperation -> {
                if (OClusterBasedStorageConfiguration.exists(writeCache)) {
                  configuration = new OClusterBasedStorageConfiguration(this);
                  ((OClusterBasedStorageConfiguration) configuration)
                      .load(contextConfiguration, atomicOperation);

                  // otherwise delayed to disk based storage to convert old format to new format.
                }

                initConfiguration(contextConfiguration, atomicOperation);
              });

          atomicOperationsManager.executeInsideAtomicOperation(
              null,
              (atomicOperation) -> {
                String uuid = configuration.getUuid();
                if (uuid == null) {
                  uuid = UUID.randomUUID().toString();
                  configuration.setUuid(atomicOperation, uuid);
                }
                this.uuid = UUID.fromString(uuid);
              });

          checkPageSizeAndRelatedParameters();

          componentsFactory = new OCurrentStorageComponentsFactory(configuration);

          sbTreeCollectionManager.load();

          atomicOperationsManager.executeInsideAtomicOperation(null, this::openClusters);
          openIndexes();

          atomicOperationsManager.executeInsideAtomicOperation(
              null,
              (atomicOperation) -> {
                final String cs = configuration.getConflictStrategy();
                if (cs != null) {
                  // SET THE CONFLICT STORAGE STRATEGY FROM THE LOADED CONFIGURATION
                  doSetConflictStrategy(
                      YouTrackDBManager.instance().getRecordConflictStrategy().getStrategy(cs),
                      atomicOperation);
                }
                if (lastMetadata == null) {
                  lastMetadata = startupMetadata.txMetadata;
                }
              });

          status = STATUS.MIGRATION;
        } finally {
          stateLock.writeLock().unlock();
        }

        // we need to use read lock to allow for example correctly truncate WAL during data
        // processing
        // all operations are prohibited on storage because of usage of special status.
        stateLock.readLock().lock();
        try {
          if (status != STATUS.MIGRATION) {
            LogManager.instance()
                .error(
                    this,
                    "Unexpected storage status %s, process of creation of storage is aborted",
                    null,
                    status.name());
            return;
          }

          sbTreeCollectionManager.migrate();
        } finally {
          stateLock.readLock().unlock();
        }

        stateLock.writeLock().lock();
        try {
          if (status != STATUS.MIGRATION) {
            LogManager.instance()
                .error(
                    this,
                    "Unexpected storage status %s, process of creation of storage is aborted",
                    null,
                    status.name());
            return;
          }

          // we need to check presence of ridbags for backward compatibility with previous
          // versions
          atomicOperationsManager.executeInsideAtomicOperation(null, this::checkRidBagsPresence);
          status = STATUS.OPEN;
          migration.countDown();
        } finally {
          stateLock.writeLock().unlock();
        }

      } catch (final RuntimeException e) {
        try {
          if (writeCache != null) {
            readCache.closeStorage(writeCache);
          }
        } catch (final Exception ee) {
          // ignore
        }

        try {
          if (writeAheadLog != null) {
            writeAheadLog.close();
          }
        } catch (final Exception ee) {
          // ignore
        }

        try {
          postCloseSteps(false, true, idGen.getLastId());
        } catch (final Exception ee) {
          // ignore
        }

        status = STATUS.CLOSED;
        throw e;
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      if (status == STATUS.OPEN) {
        sessionCount.incrementAndGet();
      }
    }

    final Object[] additionalArgs = new Object[]{getURL(), OConstants.getVersion()};
    LogManager.instance()
        .info(this, "Storage '%s' is opened under YouTrackDB distribution : %s", additionalArgs);
  }

  protected abstract void readIv() throws IOException;

  @SuppressWarnings("unused")
  protected abstract byte[] getIv();

  /**
   * @inheritDoc
   */
  @Override
  public final String getCreatedAtVersion() {
    return configuration.getCreatedAtVersion();
  }

  protected final void openIndexes() {
    final OCurrentStorageComponentsFactory cf = componentsFactory;
    if (cf == null) {
      throw new YTStorageException("Storage '" + name + "' is not properly initialized");
    }
    final Set<String> indexNames = configuration.indexEngines();
    int counter = 0;

    // avoid duplication of index engine ids
    for (final String indexName : indexNames) {
      final IndexEngineData engineData = configuration.getIndexEngine(indexName, -1);
      if (counter <= engineData.getIndexId()) {
        counter = engineData.getIndexId() + 1;
      }
    }

    for (final String indexName : indexNames) {
      final IndexEngineData engineData = configuration.getIndexEngine(indexName, counter);

      final OBaseIndexEngine engine = OIndexes.createIndexEngine(this, engineData);

      engine.load(engineData);

      indexEngineNameMap.put(engineData.getName(), engine);
      while (engineData.getIndexId() >= indexEngines.size()) {
        indexEngines.add(null);
      }
      indexEngines.set(engineData.getIndexId(), engine);
      counter++;
    }
  }

  protected final void openClusters(final OAtomicOperation atomicOperation) throws IOException {
    // OPEN BASIC SEGMENTS
    int pos;

    // REGISTER CLUSTER
    final List<OStorageClusterConfiguration> configurationClusters = configuration.getClusters();
    for (int i = 0; i < configurationClusters.size(); ++i) {
      final OStorageClusterConfiguration clusterConfig = configurationClusters.get(i);

      if (clusterConfig != null) {
        pos = createClusterFromConfig(clusterConfig);

        try {
          if (pos == -1) {
            clusters.get(i).open(atomicOperation);
          } else {
            if (clusterConfig.getName().equals(CLUSTER_DEFAULT_NAME)) {
              defaultClusterId = pos;
            }

            clusters.get(pos).open(atomicOperation);
          }
        } catch (final FileNotFoundException e) {
          LogManager.instance()
              .warn(
                  this,
                  "Error on loading cluster '"
                      + configurationClusters.get(i).getName()
                      + "' ("
                      + i
                      + "): file not found. It will be excluded from current database '"
                      + name
                      + "'.",
                  e);

          clusterMap.remove(configurationClusters.get(i).getName().toLowerCase());

          setCluster(i, null);
        }
      } else {
        setCluster(i, null);
      }
    }
  }

  private void checkRidBagsPresence(final OAtomicOperation operation) {
    for (final OCluster cluster : clusters) {
      if (cluster != null) {
        final int clusterId = cluster.getId();

        if (!sbTreeCollectionManager.isComponentPresent(operation, clusterId)) {
          LogManager.instance()
              .info(
                  this,
                  "Cluster with id %d does not have associated rid bag, fixing ...",
                  clusterId);
          sbTreeCollectionManager.createComponent(operation, clusterId);
        }
      }
    }
  }

  @Override
  public void create(final YTContextConfiguration contextConfiguration) {

    try {
      stateLock.writeLock().lock();
      try {
        doCreate(contextConfiguration);
      } catch (final InterruptedException e) {
        throw YTException.wrapException(
            new YTStorageException("Storage creation was interrupted"), e);
      } catch (final YTStorageException e) {
        close(null);
        throw e;
      } catch (final IOException e) {
        close(null);
        throw YTException.wrapException(
            new YTStorageException("Error on creation of storage '" + name + "'"), e);
      } finally {
        stateLock.writeLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }

    boolean fsyncAfterCreate =
        contextConfiguration.getValueAsBoolean(
            GlobalConfiguration.STORAGE_MAKE_FULL_CHECKPOINT_AFTER_CREATE);
    if (fsyncAfterCreate) {
      synch();
    }

    final Object[] additionalArgs = new Object[]{getURL(), OConstants.getVersion()};
    LogManager.instance()
        .info(this, "Storage '%s' is created under YouTrackDB distribution : %s", additionalArgs);
  }

  protected void doCreate(YTContextConfiguration contextConfiguration)
      throws IOException, InterruptedException {
    checkPageSizeAndRelatedParametersInGlobalConfiguration();

    if (name == null) {
      throw new YTInvalidDatabaseNameException("Database name can not be null");
    }

    if (name.isEmpty()) {
      throw new YTInvalidDatabaseNameException("Database name can not be empty");
    }

    final Pattern namePattern = Pattern.compile("[^\\w\\d$_-]+");
    final Matcher matcher = namePattern.matcher(name);
    if (matcher.find()) {
      throw new YTInvalidDatabaseNameException(
          "Only letters, numbers, `$`, `_` and `-` are allowed in database name. Provided name :`"
              + name
              + "`");
    }

    if (status != STATUS.CLOSED) {
      throw new YTStorageExistsException(
          "Cannot create new storage '" + getURL() + "' because it is not closed");
    }

    if (exists()) {
      throw new YTStorageExistsException(
          "Cannot create new storage '" + getURL() + "' because it already exists");
    }

    uuid = UUID.randomUUID();
    initIv();

    initWalAndDiskCache(contextConfiguration);

    atomicOperationsTable =
        new AtomicOperationsTable(
            contextConfiguration.getValueAsInteger(
                GlobalConfiguration.STORAGE_ATOMIC_OPERATIONS_TABLE_COMPACTION_LIMIT),
            idGen.getLastId() + 1);
    atomicOperationsManager = new OAtomicOperationsManager(this, atomicOperationsTable);
    transaction = new ThreadLocal<>();

    preCreateSteps();
    makeStorageDirty();

    atomicOperationsManager.executeInsideAtomicOperation(
        null,
        (atomicOperation) -> {
          configuration = new OClusterBasedStorageConfiguration(this);
          ((OClusterBasedStorageConfiguration) configuration)
              .create(atomicOperation, contextConfiguration);
          configuration.setUuid(atomicOperation, uuid.toString());

          componentsFactory = new OCurrentStorageComponentsFactory(configuration);

          sbTreeCollectionManager.load();

          status = STATUS.OPEN;

          sbTreeCollectionManager = new OSBTreeCollectionManagerShared(this);

          // ADD THE METADATA CLUSTER TO STORE INTERNAL STUFF
          doAddCluster(atomicOperation, OMetadataDefault.CLUSTER_INTERNAL_NAME);

          ((OClusterBasedStorageConfiguration) configuration)
              .setCreationVersion(atomicOperation, OConstants.getVersion());
          ((OClusterBasedStorageConfiguration) configuration)
              .setPageSize(
                  atomicOperation,
                  GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getValueAsInteger() * 1024);
          ((OClusterBasedStorageConfiguration) configuration)
              .setMaxKeySize(
                  atomicOperation, GlobalConfiguration.SBTREE_MAX_KEY_SIZE.getValueAsInteger());

          generateDatabaseInstanceId(atomicOperation);

          // ADD THE INDEX CLUSTER TO STORE, BY DEFAULT, ALL THE RECORDS OF
          // INDEXING
          doAddCluster(atomicOperation, OMetadataDefault.CLUSTER_INDEX_NAME);

          // ADD THE INDEX CLUSTER TO STORE, BY DEFAULT, ALL THE RECORDS OF
          // INDEXING
          doAddCluster(atomicOperation, OMetadataDefault.CLUSTER_MANUAL_INDEX_NAME);

          // ADD THE DEFAULT CLUSTER
          defaultClusterId = doAddCluster(atomicOperation, CLUSTER_DEFAULT_NAME);

          clearStorageDirty();

          postCreateSteps();

          // binary compatibility with previous version, this record contained configuration of
          // storage
          doCreateRecord(
              atomicOperation,
              new YTRecordId(0, -1),
              new byte[]{0, 0, 0, 0},
              0,
              Blob.RECORD_TYPE,
              null,
              doGetAndCheckCluster(0),
              null);
        });
  }

  protected void generateDatabaseInstanceId(OAtomicOperation atomicOperation) {
    ((OClusterBasedStorageConfiguration) configuration)
        .setProperty(atomicOperation, DATABASE_INSTANCE_ID, UUID.randomUUID().toString());
  }

  protected UUID readDatabaseInstanceId() {
    String id = configuration.getProperty(DATABASE_INSTANCE_ID);
    if (id != null) {
      return UUID.fromString(id);
    } else {
      return null;
    }
  }

  @SuppressWarnings("unused")
  protected void checkDatabaseInstanceId(UUID backupUUID) {
    UUID dbUUID = readDatabaseInstanceId();
    if (backupUUID == null) {
      throw new YTInvalidInstanceIdException(
          "The Database Instance Id do not mach, backup UUID is null");
    }
    if (dbUUID != null) {
      if (!dbUUID.equals(backupUUID)) {
        throw new YTInvalidInstanceIdException(
            String.format(
                "The Database Instance Id do not mach, database: '%s' backup: '%s'",
                dbUUID, backupUUID));
      }
    }
  }

  protected abstract void initIv() throws IOException;

  private void checkPageSizeAndRelatedParameters() {
    final int pageSize = GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getValueAsInteger() * 1024;
    final int maxKeySize = GlobalConfiguration.SBTREE_MAX_KEY_SIZE.getValueAsInteger();

    if (configuration.getPageSize() != -1 && configuration.getPageSize() != pageSize) {
      throw new YTStorageException(
          "Storage is created with value of "
              + configuration.getPageSize()
              + " parameter equal to "
              + GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getKey()
              + " but current value is "
              + pageSize);
    }

    if (configuration.getMaxKeySize() != -1 && configuration.getMaxKeySize() != maxKeySize) {
      throw new YTStorageException(
          "Storage is created with value of "
              + configuration.getMaxKeySize()
              + " parameter equal to "
              + GlobalConfiguration.SBTREE_MAX_KEY_SIZE.getKey()
              + " but current value is "
              + maxKeySize);
    }
  }

  @Override
  public final boolean isClosed(YTDatabaseSessionInternal database) {
    try {
      stateLock.readLock().lock();
      try {
        return isClosedInternal();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  protected final boolean isClosedInternal() {
    return status == STATUS.CLOSED;
  }

  @Override
  public final void close(YTDatabaseSessionInternal database, final boolean force) {
    try {
      if (!force) {
        close(database);
        return;
      }

      doShutdown();
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final void delete() {
    try {
      final long timer = YouTrackDBManager.instance().getProfiler().startChrono();
      stateLock.writeLock().lock();
      try {
        doDelete();
      } finally {
        stateLock.writeLock().unlock();
        YouTrackDBManager.instance()
            .getProfiler()
            .stopChrono("db." + name + ".drop", "Drop a database", timer, "db.*.drop");
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private void doDelete() throws IOException {
    makeStorageDirty();

    // CLOSE THE DATABASE BY REMOVING THE CURRENT USER
    doShutdownOnDelete();
    postDeleteSteps();
  }

  public boolean check(final boolean verbose, final OCommandOutputListener listener) {
    try {
      listener.onMessage("Check of storage is started...");

      stateLock.readLock().lock();
      try {
        final long lockId = atomicOperationsManager.freezeAtomicOperations(null, null);
        try {

          checkOpennessAndMigration();

          final long start = System.currentTimeMillis();

          final OPageDataVerificationError[] pageErrors =
              writeCache.checkStoredPages(verbose ? listener : null);

          String errors =
              pageErrors.length > 0 ? pageErrors.length + " with errors." : " without errors.";
          listener.onMessage(
              "Check of storage completed in "
                  + (System.currentTimeMillis() - start)
                  + "ms. "
                  + errors);

          return pageErrors.length == 0;
        } finally {
          atomicOperationsManager.releaseAtomicOperations(lockId);
        }
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final int addCluster(YTDatabaseSessionInternal database, final String clusterName,
      final Object... parameters) {
    try {
      checkBackupRunning();
      stateLock.writeLock().lock();
      try {
        if (clusterMap.containsKey(clusterName)) {
          throw new YTConfigurationException(
              String.format("Cluster with name:'%s' already exists", clusterName));
        }
        checkOpennessAndMigration();

        makeStorageDirty();
        return atomicOperationsManager.calculateInsideAtomicOperation(
            null, (atomicOperation) -> doAddCluster(atomicOperation, clusterName));

      } catch (final IOException e) {
        throw YTException.wrapException(
            new YTStorageException("Error in creation of new cluster '" + clusterName), e);
      } finally {
        stateLock.writeLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final int addCluster(YTDatabaseSessionInternal database, final String clusterName,
      final int requestedId) {
    try {
      checkBackupRunning();
      stateLock.writeLock().lock();
      try {

        checkOpennessAndMigration();

        if (requestedId < 0) {
          throw new YTConfigurationException("Cluster id must be positive!");
        }
        if (requestedId < clusters.size() && clusters.get(requestedId) != null) {
          throw new YTConfigurationException(
              "Requested cluster ID ["
                  + requestedId
                  + "] is occupied by cluster with name ["
                  + clusters.get(requestedId).getName()
                  + "]");
        }
        if (clusterMap.containsKey(clusterName)) {
          throw new YTConfigurationException(
              String.format("Cluster with name:'%s' already exists", clusterName));
        }

        makeStorageDirty();
        return atomicOperationsManager.calculateInsideAtomicOperation(
            null, atomicOperation -> doAddCluster(atomicOperation, clusterName, requestedId));

      } catch (final IOException e) {
        throw YTException.wrapException(
            new YTStorageException("Error in creation of new cluster '" + clusterName + "'"), e);
      } finally {
        stateLock.writeLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final boolean dropCluster(YTDatabaseSessionInternal database, final int clusterId) {
    try {
      checkBackupRunning();
      stateLock.writeLock().lock();
      try {

        checkOpennessAndMigration();

        if (clusterId < 0 || clusterId >= clusters.size()) {
          throw new IllegalArgumentException(
              "Cluster id '"
                  + clusterId
                  + "' is outside the of range of configured clusters (0-"
                  + (clusters.size() - 1)
                  + ") in database '"
                  + name
                  + "'");
        }

        makeStorageDirty();

        return atomicOperationsManager.calculateInsideAtomicOperation(
            null,
            atomicOperation -> {
              if (dropClusterInternal(atomicOperation, clusterId)) {
                return false;
              }

              ((OClusterBasedStorageConfiguration) configuration)
                  .dropCluster(atomicOperation, clusterId);
              sbTreeCollectionManager.deleteComponentByClusterId(atomicOperation, clusterId);

              return true;
            });
      } catch (final Exception e) {
        throw YTException.wrapException(
            new YTStorageException("Error while removing cluster '" + clusterId + "'"), e);

      } finally {
        stateLock.writeLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private void checkClusterId(int clusterId) {
    if (clusterId < 0 || clusterId >= clusters.size()) {
      throw new YTClusterDoesNotExistException(
          "Cluster id '"
              + clusterId
              + "' is outside the of range of configured clusters (0-"
              + (clusters.size() - 1)
              + ") in database '"
              + name
              + "'");
    }
  }

  @Override
  public String getClusterNameById(int clusterId) {
    try {
      stateLock.readLock().lock();
      try {
        checkOpennessAndMigration();

        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return cluster.getName();
      } finally {
        stateLock.readLock().unlock();
      }

    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public long getClusterRecordsSizeById(int clusterId) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return cluster.getRecordsSize();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public long getClusterRecordsSizeByName(String clusterName) {
    Objects.requireNonNull(clusterName);

    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OCluster cluster = clusterMap.get(clusterName.toLowerCase());
        if (cluster == null) {
          throwClusterDoesNotExist(clusterName);
        }

        return cluster.getRecordsSize();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public String getClusterRecordConflictStrategy(int clusterId) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return Optional.ofNullable(cluster.getRecordConflictStrategy())
            .map(ORecordConflictStrategy::getName)
            .orElse(null);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public String getClusterEncryption(int clusterId) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return cluster.encryption();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public boolean isSystemCluster(int clusterId) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return cluster.isSystemCluster();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public long getLastClusterPosition(int clusterId) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return cluster.getLastPosition();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public long getClusterNextPosition(int clusterId) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return cluster.getNextPosition();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public RECORD_STATUS getRecordStatus(YTRID rid) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final int clusterId = rid.getClusterId();
        checkClusterId(clusterId);
        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          throwClusterDoesNotExist(clusterId);
        }

        return ((PaginatedCluster) cluster).getRecordStatus(rid.getClusterPosition());
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private void throwClusterDoesNotExist(int clusterId) {
    throw new YTClusterDoesNotExistException(
        "Cluster with id " + clusterId + " does not exist inside of storage " + name);
  }

  private void throwClusterDoesNotExist(String clusterName) {
    throw new YTClusterDoesNotExistException(
        "Cluster with name `" + clusterName + "` does not exist inside of storage " + name);
  }

  @Override
  public final int getId() {
    return id;
  }

  public UUID getUuid() {
    return uuid;
  }

  private boolean setClusterStatus(
      final OAtomicOperation atomicOperation,
      final OCluster cluster,
      final OStorageClusterConfiguration.STATUS iStatus)
      throws IOException {
    if (iStatus == OStorageClusterConfiguration.STATUS.OFFLINE && cluster instanceof OOfflineCluster
        || iStatus == OStorageClusterConfiguration.STATUS.ONLINE
        && !(cluster instanceof OOfflineCluster)) {
      return false;
    }

    final OCluster newCluster;
    final int clusterId = cluster.getId();
    if (iStatus == OStorageClusterConfiguration.STATUS.OFFLINE) {
      cluster.close(true);
      newCluster = new OOfflineCluster(this, clusterId, cluster.getName());

      boolean configured = false;
      for (final OStorageClusterConfiguration clusterConfiguration : configuration.getClusters()) {
        if (clusterConfiguration.getId() == cluster.getId()) {
          newCluster.configure(this, clusterConfiguration);
          configured = true;
          break;
        }
      }

      if (!configured) {
        throw new YTStorageException("Can not configure offline cluster with id " + clusterId);
      }
    } else {
      newCluster =
          OPaginatedClusterFactory.createCluster(
              cluster.getName(), configuration.getVersion(), cluster.getBinaryVersion(), this);
      newCluster.configure(clusterId, cluster.getName());
      newCluster.open(atomicOperation);
    }

    clusterMap.put(cluster.getName().toLowerCase(), newCluster);
    clusters.set(clusterId, newCluster);

    ((OClusterBasedStorageConfiguration) configuration)
        .setClusterStatus(atomicOperation, clusterId, iStatus);

    return true;
  }

  @Override
  public final OSBTreeCollectionManager getSBtreeCollectionManager() {
    return sbTreeCollectionManager;
  }

  public OReadCache getReadCache() {
    return readCache;
  }

  public OWriteCache getWriteCache() {
    return writeCache;
  }

  @Override
  public final long count(YTDatabaseSessionInternal session, final int iClusterId) {
    return count(session, iClusterId, false);
  }

  @Override
  public final long count(YTDatabaseSessionInternal session, final int clusterId,
      final boolean countTombstones) {
    try {
      if (clusterId == -1) {
        throw new YTStorageException(
            "Cluster Id " + clusterId + " is invalid in database '" + name + "'");
      }

      // COUNT PHYSICAL CLUSTER IF ANY
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
          return 0;
        }

        if (countTombstones) {
          return cluster.getEntries();
        }

        return cluster.getEntries() - cluster.getTombstonesCount();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final long[] getClusterDataRange(YTDatabaseSessionInternal session, final int iClusterId) {
    try {
      if (iClusterId == -1) {
        return new long[]{YTRID.CLUSTER_POS_INVALID, YTRID.CLUSTER_POS_INVALID};
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        if (clusters.get(iClusterId) != null) {
          return new long[]{
              clusters.get(iClusterId).getFirstPosition(),
              clusters.get(iClusterId).getLastPosition()
          };
        } else {
          return OCommonConst.EMPTY_LONG_ARRAY;
        }

      } catch (final IOException ioe) {
        throw YTException.wrapException(
            new YTStorageException("Cannot retrieve information about data range"), ioe);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final long count(YTDatabaseSessionInternal session, final int[] iClusterIds) {
    return count(session, iClusterIds, false);
  }

  @Override
  public final void onException(final Throwable e) {

    LogManager.instance()
        .error(
            this,
            "Error in data flush background thread, for storage %s ,"
                + "please restart database and send full stack trace inside of bug report",
            e,
            name);

    if (status == STATUS.CLOSED) {
      return;
    }

    if (!(e instanceof YTInternalErrorException)) {
      setInError(e);
    }

    try {
      makeStorageDirty();
    } catch (IOException ioException) {
      // ignore
    }
  }

  private void setInError(final Throwable e) {
    error.set(e);
  }

  @Override
  public final long count(YTDatabaseSessionInternal session, final int[] iClusterIds,
      final boolean countTombstones) {
    try {
      long tot = 0;

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        for (final int iClusterId : iClusterIds) {
          if (iClusterId >= clusters.size()) {
            throw new YTConfigurationException(
                "Cluster id " + iClusterId + " was not found in database '" + name + "'");
          }

          if (iClusterId > -1) {
            final OCluster c = clusters.get(iClusterId);
            if (c != null) {
              tot += c.getEntries() - (countTombstones ? 0L : c.getTombstonesCount());
            }
          }
        }

        return tot;
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public final OStorageOperationResult<OPhysicalPosition> createRecord(
      final YTRecordId rid,
      final byte[] content,
      final int recordVersion,
      final byte recordType,
      final ORecordCallback<Long> callback) {
    try {

      final OCluster cluster = doGetAndCheckCluster(rid.getClusterId());
      if (transaction.get() != null) {
        final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
        return doCreateRecord(
            atomicOperation, rid, content, recordVersion, recordType, callback, cluster, null);
      }

      stateLock.readLock().lock();
      try {
        checkOpennessAndMigration();

        makeStorageDirty();

        return atomicOperationsManager.calculateInsideAtomicOperation(
            null,
            atomicOperation ->
                doCreateRecord(
                    atomicOperation,
                    rid,
                    content,
                    recordVersion,
                    recordType,
                    callback,
                    cluster,
                    null));
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final ORecordMetadata getRecordMetadata(YTDatabaseSessionInternal session,
      final YTRID rid) {
    try {
      if (rid.isNew()) {
        throw new YTStorageException(
            "Passed record with id " + rid + " is new and cannot be stored.");
      }

      stateLock.readLock().lock();
      try {

        final OCluster cluster = doGetAndCheckCluster(rid.getClusterId());
        checkOpennessAndMigration();

        final OPhysicalPosition ppos =
            cluster.getPhysicalPosition(new OPhysicalPosition(rid.getClusterPosition()));
        if (ppos == null) {
          return null;
        }

        return new ORecordMetadata(rid, ppos.recordVersion);
      } catch (final IOException ioe) {
        LogManager.instance()
            .error(this, "Retrieval of record  '" + rid + "' cause: " + ioe.getMessage(), ioe);
      } finally {
        stateLock.readLock().unlock();
      }

      return null;
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public Iterator<OClusterBrowsePage> browseCluster(final int clusterId) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final int finalClusterId;

        if (clusterId == YTRID.CLUSTER_ID_INVALID) {
          // GET THE DEFAULT CLUSTER
          finalClusterId = defaultClusterId;
        } else {
          finalClusterId = clusterId;
        }
        return new Iterator<>() {
          @Nullable
          private OClusterBrowsePage page;
          private long lastPos = -1;

          @Override
          public boolean hasNext() {
            if (page == null) {
              page = nextPage(finalClusterId, lastPos);
              if (page != null) {
                lastPos = page.getLastPosition();
              }
            }
            return page != null;
          }

          @Override
          public OClusterBrowsePage next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            final OClusterBrowsePage curPage = page;
            page = null;
            return curPage;
          }
        };
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private OClusterBrowsePage nextPage(final int clusterId, final long lastPosition) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OCluster cluster = doGetAndCheckCluster(clusterId);
        return cluster.nextPage(lastPosition);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private OCluster doGetAndCheckCluster(final int clusterId) {
    checkClusterSegmentIndexRange(clusterId);

    final OCluster cluster = clusters.get(clusterId);
    if (cluster == null) {
      throw new IllegalArgumentException("Cluster " + clusterId + " is null");
    }
    return cluster;
  }

  @Override
  public @Nonnull ORawBuffer readRecord(
      YTDatabaseSessionInternal session, final YTRecordId rid,
      final boolean iIgnoreCache,
      final boolean prefetchRecords,
      final ORecordCallback<ORawBuffer> iCallback) {
    try {
      return readRecord(rid, prefetchRecords);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public final void updateRecord(
      final YTRecordId rid,
      final boolean updateContent,
      final byte[] content,
      final int version,
      final byte recordType,
      @SuppressWarnings("unused") final int mode,
      final ORecordCallback<Integer> callback) {
    try {
      assert transaction.get() == null;

      stateLock.readLock().lock();
      try {
        checkOpennessAndMigration();

        // GET THE SHARED LOCK AND GET AN EXCLUSIVE LOCK AGAINST THE RECORD
        final Lock lock = recordVersionManager.get(rid);
        lock.lock();
        try {
          checkOpennessAndMigration();

          makeStorageDirty();

          final OCluster cluster = doGetAndCheckCluster(rid.getClusterId());
          atomicOperationsManager.calculateInsideAtomicOperation(
              null,
              atomicOperation ->
                  doUpdateRecord(
                      atomicOperation,
                      rid,
                      updateContent,
                      content,
                      version,
                      recordType,
                      callback,
                      cluster));
        } finally {
          lock.unlock();
        }
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  public final OAtomicOperationsManager getAtomicOperationsManager() {
    return atomicOperationsManager;
  }

  @Nonnull
  public OWriteAheadLog getWALInstance() {
    return writeAheadLog;
  }

  public AtomicOperationIdGen getIdGen() {
    return idGen;
  }

  private OStorageOperationResult<Boolean> deleteRecord(
      final YTRecordId rid,
      final int version,
      final int mode,
      final ORecordCallback<Boolean> callback) {
    try {
      assert transaction.get() == null;

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OCluster cluster = doGetAndCheckCluster(rid.getClusterId());

        makeStorageDirty();

        return atomicOperationsManager.calculateInsideAtomicOperation(
            null, atomicOperation -> doDeleteRecord(atomicOperation, rid, version, cluster));
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final Set<String> getClusterNames() {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return Collections.unmodifiableSet(clusterMap.keySet());
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final int getClusterIdByName(final String clusterName) {
    try {
      if (clusterName == null) {
        throw new IllegalArgumentException("Cluster name is null");
      }

      if (clusterName.isEmpty()) {
        throw new IllegalArgumentException("Cluster name is empty");
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        // SEARCH IT BETWEEN PHYSICAL CLUSTERS

        final OCluster segment = clusterMap.get(clusterName.toLowerCase());
        if (segment != null) {
          return segment.getId();
        }

        return -1;
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  /**
   * Scan the given transaction for new record and allocate a record id for them, the relative
   * record id is inserted inside the transaction for future use.
   *
   * @param clientTx the transaction of witch allocate rids
   */
  public void preallocateRids(final OTransactionInternal clientTx) {
    try {
      final Iterable<ORecordOperation> entries = clientTx.getRecordOperations();
      final TreeMap<Integer, OCluster> clustersToLock = new TreeMap<>();

      final Set<ORecordOperation> newRecords = new TreeSet<>(COMMIT_RECORD_OPERATION_COMPARATOR);

      for (final ORecordOperation txEntry : entries) {

        if (txEntry.type == ORecordOperation.CREATED) {
          newRecords.add(txEntry);
          final int clusterId = txEntry.getRID().getClusterId();
          clustersToLock.put(clusterId, doGetAndCheckCluster(clusterId));
        }
      }
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        makeStorageDirty();
        atomicOperationsManager.executeInsideAtomicOperation(
            null,
            atomicOperation -> {
              lockClusters(clustersToLock);

              for (final ORecordOperation txEntry : newRecords) {
                final Record rec = txEntry.record;
                if (!rec.getIdentity().isPersistent()) {
                  if (rec.isDirty()) {
                    // This allocate a position for a new record
                    final YTRecordId rid = (YTRecordId) rec.getIdentity().copy();
                    final YTRecordId oldRID = rid.copy();
                    final OCluster cluster = doGetAndCheckCluster(rid.getClusterId());
                    final OPhysicalPosition ppos =
                        cluster.allocatePosition(
                            ORecordInternal.getRecordType(rec), atomicOperation);
                    rid.setClusterPosition(ppos.clusterPosition);
                    clientTx.updateIdentityAfterCommit(oldRID, rid);
                  }
                } else {
                  // This allocate position starting from a valid rid, used in distributed for
                  // allocate the same position on other nodes
                  final YTRecordId rid = (YTRecordId) rec.getIdentity();
                  final PaginatedCluster cluster =
                      (PaginatedCluster) doGetAndCheckCluster(rid.getClusterId());
                  RECORD_STATUS recordStatus = cluster.getRecordStatus(rid.getClusterPosition());
                  if (recordStatus == RECORD_STATUS.NOT_EXISTENT) {
                    OPhysicalPosition ppos =
                        cluster.allocatePosition(
                            ORecordInternal.getRecordType(rec), atomicOperation);
                    while (ppos.clusterPosition < rid.getClusterPosition()) {
                      ppos =
                          cluster.allocatePosition(
                              ORecordInternal.getRecordType(rec), atomicOperation);
                    }
                    if (ppos.clusterPosition != rid.getClusterPosition()) {
                      throw new YTConcurrentCreateException(
                          rid, new YTRecordId(rid.getClusterId(), ppos.clusterPosition));
                    }
                  } else if (recordStatus == RECORD_STATUS.PRESENT
                      || recordStatus == RECORD_STATUS.REMOVED) {
                    final OPhysicalPosition ppos =
                        cluster.allocatePosition(
                            ORecordInternal.getRecordType(rec), atomicOperation);
                    throw new YTConcurrentCreateException(
                        rid, new YTRecordId(rid.getClusterId(), ppos.clusterPosition));
                  }
                }
              }
            });
      } catch (final IOException | RuntimeException ioe) {
        throw YTException.wrapException(new YTStorageException("Could not preallocate RIDs"), ioe);
      } finally {
        stateLock.readLock().unlock();
      }

    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  /**
   * Traditional commit that support already temporary rid and already assigned rids
   *
   * @param clientTx the transaction to commit
   * @return The list of operations applied by the transaction
   */
  @Override
  public List<ORecordOperation> commit(final OTransactionOptimistic clientTx) {
    return commit(clientTx, false);
  }

  /**
   * Commit a transaction where the rid where pre-allocated in a previous phase
   *
   * @param clientTx the pre-allocated transaction to commit
   * @return The list of operations applied by the transaction
   */
  @SuppressWarnings("UnusedReturnValue")
  public List<ORecordOperation> commitPreAllocated(final OTransactionOptimistic clientTx) {
    return commit(clientTx, true);
  }

  /**
   * The commit operation can be run in 3 different conditions, embedded commit, pre-allocated
   * commit, other node commit. <bold>Embedded commit</bold> is the basic commit where the operation
   * is run in embedded or server side, the transaction arrive with invalid rids that get allocated
   * and committed. <bold>pre-allocated commit</bold> is the commit that happen after an
   * preAllocateRids call is done, this is usually run by the coordinator of a tx in distributed.
   * <bold>other node commit</bold> is the commit that happen when a node execute a transaction of
   * another node where all the rids are already allocated in the other node.
   *
   * @param transaction the transaction to commit
   * @param allocated   true if the operation is pre-allocated commit
   * @return The list of operations applied by the transaction
   */
  protected List<ORecordOperation> commit(
      final OTransactionOptimistic transaction, final boolean allocated) {
    // XXX: At this moment, there are two implementations of the commit method. One for regular
    // client transactions and one for
    // implicit micro-transactions. The implementations are quite identical, but operate on slightly
    // different data. If you change
    // this method don't forget to change its counterpart:
    //
    //
    // AbstractPaginatedStorage.commit(com.orientechnologies.orient.core.storage.impl.local.OMicroTransaction)

    try {
      txBegun.increment();

      final YTDatabaseSessionInternal database = transaction.getDatabase();
      final OIndexManagerAbstract indexManager = database.getMetadata().getIndexManagerInternal();
      final TreeMap<String, OTransactionIndexChanges> indexOperations =
          getSortedIndexOperations(transaction);

      database.getMetadata().makeThreadLocalSchemaSnapshot();

      final Collection<ORecordOperation> recordOperations = transaction.getRecordOperations();
      final TreeMap<Integer, OCluster> clustersToLock = new TreeMap<>();
      final Map<ORecordOperation, Integer> clusterOverrides = new IdentityHashMap<>(8);

      final Set<ORecordOperation> newRecords = new TreeSet<>(COMMIT_RECORD_OPERATION_COMPARATOR);
      for (final ORecordOperation recordOperation : recordOperations) {
        var record = recordOperation.record;

        if (record.isUnloaded()) {
          throw new IllegalStateException(
              "Unloaded record " + record.getIdentity() + " cannot be committed");
        }

        if (recordOperation.type == ORecordOperation.CREATED
            || recordOperation.type == ORecordOperation.UPDATED) {
          if (record instanceof EntityImpl) {
            ((EntityImpl) record).validate();
          }
        }

        if (recordOperation.type == ORecordOperation.UPDATED
            || recordOperation.type == ORecordOperation.DELETED) {
          final int clusterId = recordOperation.record.getIdentity().getClusterId();
          clustersToLock.put(clusterId, doGetAndCheckCluster(clusterId));
        } else if (recordOperation.type == ORecordOperation.CREATED) {
          newRecords.add(recordOperation);

          final YTRID rid = record.getIdentity();

          int clusterId = rid.getClusterId();

          if (record.isDirty()
              && clusterId == YTRID.CLUSTER_ID_INVALID
              && record instanceof EntityImpl) {
            // TRY TO FIX CLUSTER ID TO THE DEFAULT CLUSTER ID DEFINED IN SCHEMA CLASS

            final YTImmutableClass class_ =
                ODocumentInternal.getImmutableSchemaClass(((EntityImpl) record));
            if (class_ != null) {
              clusterId = class_.getClusterForNewInstance((EntityImpl) record);
              clusterOverrides.put(recordOperation, clusterId);
            }
          }
          clustersToLock.put(clusterId, doGetAndCheckCluster(clusterId));
        }
      }

      final List<ORecordOperation> result = new ArrayList<>(8);
      stateLock.readLock().lock();
      try {
        try {
          checkOpennessAndMigration();

          makeStorageDirty();

          Throwable error = null;
          startStorageTx(transaction);
          try {
            final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
            lockClusters(clustersToLock);

            final Map<ORecordOperation, OPhysicalPosition> positions = new IdentityHashMap<>(8);
            for (final ORecordOperation recordOperation : newRecords) {
              final Record rec = recordOperation.record;

              if (allocated) {
                if (rec.getIdentity().isPersistent()) {
                  positions.put(
                      recordOperation,
                      new OPhysicalPosition(rec.getIdentity().getClusterPosition()));
                } else {
                  throw new YTStorageException(
                      "Impossible to commit a transaction with not valid rid in pre-allocated"
                          + " commit");
                }
              } else if (rec.isDirty() && !rec.getIdentity().isPersistent()) {
                final YTRecordId rid = (YTRecordId) rec.getIdentity().copy();
                final YTRecordId oldRID = rid.copy();

                final Integer clusterOverride = clusterOverrides.get(recordOperation);
                final int clusterId =
                    Optional.ofNullable(clusterOverride).orElseGet(rid::getClusterId);

                final OCluster cluster = doGetAndCheckCluster(clusterId);

                OPhysicalPosition physicalPosition =
                    cluster.allocatePosition(ORecordInternal.getRecordType(rec), atomicOperation);
                rid.setClusterId(cluster.getId());

                if (rid.getClusterPosition() > -1) {
                  // CREATE EMPTY RECORDS UNTIL THE POSITION IS REACHED. THIS IS THE CASE WHEN A
                  // SERVER IS OUT OF SYNC
                  // BECAUSE A TRANSACTION HAS BEEN ROLLED BACK BEFORE TO SEND THE REMOTE CREATES.
                  // SO THE OWNER NODE DELETED
                  // RECORD HAVING A HIGHER CLUSTER POSITION
                  while (rid.getClusterPosition() > physicalPosition.clusterPosition) {
                    physicalPosition =
                        cluster.allocatePosition(
                            ORecordInternal.getRecordType(rec), atomicOperation);
                  }

                  if (rid.getClusterPosition() != physicalPosition.clusterPosition) {
                    throw new YTConcurrentCreateException(
                        rid, new YTRecordId(rid.getClusterId(), physicalPosition.clusterPosition));
                  }
                }
                positions.put(recordOperation, physicalPosition);
                rid.setClusterPosition(physicalPosition.clusterPosition);
                transaction.updateIdentityAfterCommit(oldRID, rid);
              }
            }
            lockRidBags(clustersToLock, indexOperations, indexManager, database);

            for (final ORecordOperation recordOperation : recordOperations) {
              commitEntry(
                  transaction,
                  atomicOperation,
                  recordOperation,
                  positions.get(recordOperation),
                  database.getSerializer());
              result.add(recordOperation);
            }
            lockIndexes(indexOperations);

            commitIndexes(transaction.getDatabase(), indexOperations);
          } catch (final IOException | RuntimeException e) {
            error = e;
            if (e instanceof RuntimeException) {
              throw ((RuntimeException) e);
            } else {
              throw YTException.wrapException(
                  new YTStorageException("Error during transaction commit"), e);
            }
          } finally {
            if (error != null) {
              rollback(error);
            } else {
              endStorageTx();
            }
            this.transaction.set(null);
          }
        } finally {
          atomicOperationsManager.ensureThatComponentsUnlocked();
          database.getMetadata().clearThreadLocalSchemaSnapshot();
        }
      } finally {
        stateLock.readLock().unlock();
      }

      if (LogManager.instance().isDebugEnabled()) {
        LogManager.instance()
            .debug(
                this,
                "%d Committed transaction %d on database '%s' (result=%s)",
                Thread.currentThread().getId(),
                transaction.getId(),
                database.getName(),
                result);
      }
      return result;
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      atomicOperationsManager.alarmClearOfAtomicOperation();
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private void commitIndexes(YTDatabaseSessionInternal session,
      final Map<String, OTransactionIndexChanges> indexesToCommit) {
    for (final OTransactionIndexChanges changes : indexesToCommit.values()) {
      final OIndexInternal index = changes.getAssociatedIndex();

      try {
        final int indexId = index.getIndexId();
        if (changes.cleared) {
          clearIndex(indexId);
        }
        for (final OTransactionIndexChangesPerKey changesPerKey : changes.changesPerKey.values()) {
          applyTxChanges(session, changesPerKey, index);
        }
        applyTxChanges(session, changes.nullKeyChanges, index);
      } catch (final OInvalidIndexEngineIdException e) {
        throw YTException.wrapException(new YTStorageException("Error during index commit"), e);
      }
    }
  }

  private void applyTxChanges(YTDatabaseSessionInternal session,
      OTransactionIndexChangesPerKey changes, OIndexInternal index)
      throws OInvalidIndexEngineIdException {
    assert !(changes.key instanceof YTRID orid) || orid.isPersistent();
    for (OTransactionIndexEntry op : index.interpretTxKeyChanges(changes)) {
      switch (op.getOperation()) {
        case PUT:
          index.doPut(session, this, changes.key, op.getValue().getIdentity());
          break;
        case REMOVE:
          if (op.getValue() != null) {
            index.doRemove(session, this, changes.key, op.getValue().getIdentity());
          } else {
            index.doRemove(this, changes.key);
          }
          break;
        case CLEAR:
          // SHOULD NEVER BE THE CASE HANDLE BY cleared FLAG
          break;
      }
      applyUniqueIndexChange(index.getName(), changes.key);
    }
  }

  public int loadIndexEngine(final String name) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OBaseIndexEngine engine = indexEngineNameMap.get(name);
        if (engine == null) {
          return -1;
        }
        final int indexId = indexEngines.indexOf(engine);
        assert indexId == engine.getId();
        return generateIndexId(indexId, engine);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public int loadExternalIndexEngine(
      final OIndexMetadata indexMetadata, final Map<String, String> engineProperties) {
    final OIndexDefinition indexDefinition = indexMetadata.getIndexDefinition();
    try {
      stateLock.writeLock().lock();
      try {

        checkOpennessAndMigration();

        // this method introduced for binary compatibility only
        if (configuration.getBinaryFormatVersion() > 15) {
          return -1;
        }
        if (indexEngineNameMap.containsKey(indexMetadata.getName())) {
          throw new YTIndexException(
              "Index with name " + indexMetadata.getName() + " already exists");
        }
        makeStorageDirty();

        final int binaryFormatVersion = configuration.getBinaryFormatVersion();
        final byte valueSerializerId = indexMetadata.getValueSerializerId(binaryFormatVersion);

        final OBinarySerializer<?> keySerializer = determineKeySerializer(indexDefinition);
        if (keySerializer == null) {
          throw new YTIndexException("Can not determine key serializer");
        }
        final int keySize = determineKeySize(indexDefinition);
        final YTType[] keyTypes =
            Optional.of(indexDefinition).map(OIndexDefinition::getTypes).orElse(null);
        int generatedId = indexEngines.size();
        final IndexEngineData engineData =
            new IndexEngineData(
                generatedId,
                indexMetadata,
                true,
                valueSerializerId,
                keySerializer.getId(),
                keyTypes,
                keySize,
                null,
                null,
                engineProperties);

        final OBaseIndexEngine engine = OIndexes.createIndexEngine(this, engineData);

        engine.load(engineData);

        atomicOperationsManager.executeInsideAtomicOperation(
            null,
            atomicOperation -> {
              indexEngineNameMap.put(indexMetadata.getName(), engine);
              indexEngines.add(engine);
              ((OClusterBasedStorageConfiguration) configuration)
                  .addIndexEngine(atomicOperation, indexMetadata.getName(), engineData);
            });
        return generateIndexId(engineData.getIndexId(), engine);
      } catch (final IOException e) {
        throw YTException.wrapException(
            new YTStorageException(
                "Cannot add index engine " + indexMetadata.getName() + " in storage."),
            e);
      } finally {
        stateLock.writeLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public int addIndexEngine(
      final OIndexMetadata indexMetadata, final Map<String, String> engineProperties) {
    final OIndexDefinition indexDefinition = indexMetadata.getIndexDefinition();

    try {
      if (indexDefinition == null) {
        throw new YTIndexException("Index definition has to be provided");
      }
      final YTType[] keyTypes = indexDefinition.getTypes();
      if (keyTypes == null) {
        throw new YTIndexException("Types of indexed keys have to be provided");
      }

      final OBinarySerializer<?> keySerializer = determineKeySerializer(indexDefinition);
      if (keySerializer == null) {
        throw new YTIndexException("Can not determine key serializer");
      }

      final int keySize = determineKeySize(indexDefinition);

      checkBackupRunning();
      stateLock.writeLock().lock();
      try {

        checkOpennessAndMigration();

        makeStorageDirty();
        return atomicOperationsManager.calculateInsideAtomicOperation(
            null,
            atomicOperation -> {
              if (indexEngineNameMap.containsKey(indexMetadata.getName())) {
                // OLD INDEX FILE ARE PRESENT: THIS IS THE CASE OF PARTIAL/BROKEN INDEX
                LogManager.instance()
                    .warn(
                        this,
                        "Index with name '%s' already exists, removing it and re-create the index",
                        indexMetadata.getName());
                final OBaseIndexEngine engine = indexEngineNameMap.remove(indexMetadata.getName());
                if (engine != null) {
                  indexEngines.set(engine.getId(), null);

                  engine.delete(atomicOperation);
                  ((OClusterBasedStorageConfiguration) configuration)
                      .deleteIndexEngine(atomicOperation, indexMetadata.getName());
                }
              }
              final int binaryFormatVersion = configuration.getBinaryFormatVersion();
              final byte valueSerializerId =
                  indexMetadata.getValueSerializerId(binaryFormatVersion);
              final YTContextConfiguration ctxCfg = configuration.getContextConfiguration();
              final String cfgEncryptionKey =
                  ctxCfg.getValueAsString(GlobalConfiguration.STORAGE_ENCRYPTION_KEY);
              int genenrateId = indexEngines.size();
              final IndexEngineData engineData =
                  new IndexEngineData(
                      genenrateId,
                      indexMetadata,
                      true,
                      valueSerializerId,
                      keySerializer.getId(),
                      keyTypes,
                      keySize,
                      null,
                      cfgEncryptionKey,
                      engineProperties);

              final OBaseIndexEngine engine = OIndexes.createIndexEngine(this, engineData);

              engine.create(atomicOperation, engineData);
              indexEngineNameMap.put(indexMetadata.getName(), engine);
              indexEngines.add(engine);

              ((OClusterBasedStorageConfiguration) configuration)
                  .addIndexEngine(atomicOperation, indexMetadata.getName(), engineData);

              if (indexMetadata.isMultivalue() && engine.hasRidBagTreesSupport()) {
                final OSBTreeBonsaiLocal<YTIdentifiable, Boolean> tree =
                    new OSBTreeBonsaiLocal<>(
                        indexMetadata.getName(),
                        OIndexRIDContainerSBTree.INDEX_FILE_EXTENSION,
                        this);
                tree.createComponent(atomicOperation);
              }
              return generateIndexId(engineData.getIndexId(), engine);
            });
      } catch (final IOException e) {
        throw YTException.wrapException(
            new YTStorageException(
                "Cannot add index engine " + indexMetadata.getName() + " in storage."),
            e);
      } finally {
        stateLock.writeLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  public OBinarySerializer<?> resolveObjectSerializer(final byte serializerId) {
    return componentsFactory.binarySerializerFactory.getObjectSerializer(serializerId);
  }

  public static OEncryption loadEncryption(
      final String cfgEncryption, final String cfgEncryptionKey) {
    final OEncryption encryption;
    if (cfgEncryption == null || cfgEncryption.equalsIgnoreCase(ONothingEncryption.NAME)) {
      encryption = null;
    } else {
      encryption = OEncryptionFactory.INSTANCE.getEncryption(cfgEncryption, cfgEncryptionKey);
    }
    return encryption;
  }

  private static int generateIndexId(final int internalId, final OBaseIndexEngine indexEngine) {
    return indexEngine.getEngineAPIVersion() << (OIntegerSerializer.INT_SIZE * 8 - 5) | internalId;
  }

  private static int extractInternalId(final int externalId) {
    if (externalId < 0) {
      throw new IllegalStateException("Index id has to be positive");
    }

    return externalId & 0x7_FF_FF_FF;
  }

  public static int extractEngineAPIVersion(final int externalId) {
    return externalId >>> (OIntegerSerializer.INT_SIZE * 8 - 5);
  }

  private static int determineKeySize(final OIndexDefinition indexDefinition) {
    if (indexDefinition == null || indexDefinition instanceof ORuntimeKeyIndexDefinition) {
      return 1;
    } else {
      return indexDefinition.getTypes().length;
    }
  }

  private OBinarySerializer<?> determineKeySerializer(final OIndexDefinition indexDefinition) {
    if (indexDefinition == null) {
      throw new YTStorageException("Index definition has to be provided");
    }

    final YTType[] keyTypes = indexDefinition.getTypes();
    if (keyTypes == null || keyTypes.length == 0) {
      throw new YTStorageException("Types of index keys has to be defined");
    }
    if (keyTypes.length < indexDefinition.getFields().size()) {
      throw new YTStorageException(
          "Types are provided only for "
              + keyTypes.length
              + " fields. But index definition has "
              + indexDefinition.getFields().size()
              + " fields.");
    }

    final OBinarySerializer<?> keySerializer;
    if (indexDefinition.getTypes().length > 1) {
      keySerializer = OCompositeKeySerializer.INSTANCE;
    } else {
      final YTType keyType = indexDefinition.getTypes()[0];

      if (keyType == YTType.STRING && configuration.getBinaryFormatVersion() >= 13) {
        return OUTF8Serializer.INSTANCE;
      }

      final OCurrentStorageComponentsFactory currentStorageComponentsFactory = componentsFactory;
      if (currentStorageComponentsFactory != null) {
        keySerializer =
            currentStorageComponentsFactory.binarySerializerFactory.getObjectSerializer(keyType);
      } else {
        throw new IllegalStateException(
            "Cannot load binary serializer, storage is not properly initialized");
      }
    }

    return keySerializer;
  }

  public void deleteIndexEngine(int indexId) throws OInvalidIndexEngineIdException {
    final int internalIndexId = extractInternalId(indexId);

    try {
      checkBackupRunning();
      stateLock.writeLock().lock();
      try {

        checkOpennessAndMigration();

        checkIndexId(internalIndexId);

        makeStorageDirty();

        atomicOperationsManager.executeInsideAtomicOperation(
            null,
            atomicOperation -> {
              final OBaseIndexEngine engine =
                  deleteIndexEngineInternal(atomicOperation, internalIndexId);
              final String engineName = engine.getName();

              final IndexEngineData engineData =
                  configuration.getIndexEngine(engineName, internalIndexId);
              ((OClusterBasedStorageConfiguration) configuration)
                  .deleteIndexEngine(atomicOperation, engineName);

              if (engineData.isMultivalue() && engine.hasRidBagTreesSupport()) {
                final OSBTreeBonsaiLocal<YTIdentifiable, Boolean> tree =
                    new OSBTreeBonsaiLocal<>(
                        engineName, OIndexRIDContainerSBTree.INDEX_FILE_EXTENSION, this);
                tree.deleteComponent(atomicOperation);
              }
            });

      } catch (final IOException e) {
        throw YTException.wrapException(new YTStorageException("Error on index deletion"), e);
      } finally {
        stateLock.writeLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private OBaseIndexEngine deleteIndexEngineInternal(
      final OAtomicOperation atomicOperation, final int indexId) throws IOException {
    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();
    indexEngines.set(indexId, null);
    engine.delete(atomicOperation);

    final String engineName = engine.getName();
    indexEngineNameMap.remove(engineName);
    return engine;
  }

  private void checkIndexId(final int indexId) throws OInvalidIndexEngineIdException {
    if (indexId < 0 || indexId >= indexEngines.size() || indexEngines.get(indexId) == null) {
      throw new OInvalidIndexEngineIdException(
          "Engine with id " + indexId + " is not registered inside of storage");
    }
  }

  public boolean removeKeyFromIndex(final int indexId, final Object key)
      throws OInvalidIndexEngineIdException {
    final int internalIndexId = extractInternalId(indexId);

    try {
      assert transaction.get() != null;
      final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
      return removeKeyFromIndexInternal(atomicOperation, internalIndexId, key);
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private boolean removeKeyFromIndexInternal(
      final OAtomicOperation atomicOperation, final int indexId, final Object key)
      throws OInvalidIndexEngineIdException {
    try {
      checkIndexId(indexId);

      final OBaseIndexEngine engine = indexEngines.get(indexId);
      if (engine.getEngineAPIVersion() == OIndexEngine.VERSION) {
        return ((OIndexEngine) engine).remove(atomicOperation, key);
      } else {
        final OV1IndexEngine v1IndexEngine = (OV1IndexEngine) engine;
        if (!v1IndexEngine.isMultiValue()) {
          return ((OSingleValueIndexEngine) engine).remove(atomicOperation, key);
        } else {
          throw new YTStorageException(
              "To remove entry from multi-value index not only key but value also should be"
                  + " provided");
        }
      }
    } catch (final IOException e) {
      throw YTException.wrapException(
          new YTStorageException("Error during removal of entry with key " + key + " from index "),
          e);
    }
  }

  public void clearIndex(final int indexId) throws OInvalidIndexEngineIdException {
    try {
      if (transaction.get() != null) {
        final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
        doClearIndex(atomicOperation, indexId);
        return;
      }

      final int internalIndexId = extractInternalId(indexId);
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        makeStorageDirty();

        atomicOperationsManager.executeInsideAtomicOperation(
            null, atomicOperation -> doClearIndex(atomicOperation, internalIndexId));
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private void doClearIndex(final OAtomicOperation atomicOperation, final int indexId)
      throws OInvalidIndexEngineIdException {
    try {
      checkIndexId(indexId);

      final OBaseIndexEngine engine = indexEngines.get(indexId);
      assert indexId == engine.getId();

      engine.clear(atomicOperation);
    } catch (final IOException e) {
      throw YTException.wrapException(new YTStorageException("Error during clearing of index"), e);
    }
  }

  public Object getIndexValue(YTDatabaseSessionInternal session, int indexId, final Object key)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doGetIndexValue(session, indexId, key);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doGetIndexValue(session, indexId, key);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Object doGetIndexValue(YTDatabaseSessionInternal session, final int indexId,
      final Object key)
      throws OInvalidIndexEngineIdException {
    final int engineAPIVersion = extractEngineAPIVersion(indexId);
    if (engineAPIVersion != 0) {
      throw new IllegalStateException(
          "Unsupported version of index engine API. Required 0 but found " + engineAPIVersion);
    }
    checkIndexId(indexId);
    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();
    return ((OIndexEngine) engine).get(session, key);
  }

  public Stream<YTRID> getIndexValues(int indexId, final Object key)
      throws OInvalidIndexEngineIdException {
    final int engineAPIVersion = extractEngineAPIVersion(indexId);
    if (engineAPIVersion != 1) {
      throw new IllegalStateException(
          "Unsupported version of index engine API. Required 1 but found " + engineAPIVersion);
    }

    indexId = extractInternalId(indexId);

    try {

      if (transaction.get() != null) {
        return doGetIndexValues(indexId, key);
      }

      stateLock.readLock().lock();
      try {
        checkOpennessAndMigration();

        return doGetIndexValues(indexId, key);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Stream<YTRID> doGetIndexValues(final int indexId, final Object key)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return ((OV1IndexEngine) engine).get(key);
  }

  public OBaseIndexEngine getIndexEngine(int indexId) throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      checkIndexId(indexId);

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OBaseIndexEngine engine = indexEngines.get(indexId);
        assert indexId == engine.getId();
        return engine;
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public void updateIndexEntry(
      YTDatabaseSessionInternal session, int indexId, final Object key,
      final OIndexKeyUpdater<Object> valueCreator)
      throws OInvalidIndexEngineIdException {
    final int engineAPIVersion = extractEngineAPIVersion(indexId);

    if (engineAPIVersion != 0) {
      throw new IllegalStateException(
          "Unsupported version of index engine API. Required 0 but found " + engineAPIVersion);
    }

    try {
      assert transaction.get() != null;
      final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
      assert atomicOperation != null;
      doUpdateIndexEntry(session, atomicOperation, indexId, key, valueCreator);
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  public <T> T callIndexEngine(
      final boolean readOperation, int indexId, final OIndexEngineCallback<T> callback)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        if (readOperation) {
          makeStorageDirty();
        }

        return doCallIndexEngine(indexId, callback);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private <T> T doCallIndexEngine(final int indexId, final OIndexEngineCallback<T> callback)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);

    return callback.callEngine(engine);
  }

  private void doUpdateIndexEntry(
      YTDatabaseSessionInternal session, final OAtomicOperation atomicOperation,
      final int indexId,
      final Object key,
      final OIndexKeyUpdater<Object> valueCreator)
      throws OInvalidIndexEngineIdException, IOException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    ((OIndexEngine) engine).update(session, atomicOperation, key, valueCreator);
  }

  public void putRidIndexEntry(int indexId, final Object key, final YTRID value)
      throws OInvalidIndexEngineIdException {
    final int engineAPIVersion = extractEngineAPIVersion(indexId);
    final int internalIndexId = extractInternalId(indexId);

    if (engineAPIVersion != 1) {
      throw new IllegalStateException(
          "Unsupported version of index engine API. Required 1 but found " + engineAPIVersion);
    }

    try {
      assert transaction.get() != null;
      final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
      assert atomicOperation != null;
      putRidIndexEntryInternal(atomicOperation, internalIndexId, key, value);
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private void putRidIndexEntryInternal(
      final OAtomicOperation atomicOperation, final int indexId, final Object key,
      final YTRID value)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert engine.getId() == indexId;

    ((OV1IndexEngine) engine).put(atomicOperation, key, value);
  }

  public boolean removeRidIndexEntry(int indexId, final Object key, final YTRID value)
      throws OInvalidIndexEngineIdException {
    final int engineAPIVersion = extractEngineAPIVersion(indexId);
    final int internalIndexId = extractInternalId(indexId);

    if (engineAPIVersion != 1) {
      throw new IllegalStateException(
          "Unsupported version of index engine API. Required 1 but found " + engineAPIVersion);
    }

    try {
      assert transaction.get() != null;
      final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
      assert atomicOperation != null;
      return removeRidIndexEntryInternal(atomicOperation, internalIndexId, key, value);

    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private boolean removeRidIndexEntryInternal(
      final OAtomicOperation atomicOperation, final int indexId, final Object key,
      final YTRID value)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert engine.getId() == indexId;

    return ((OMultiValueIndexEngine) engine).remove(atomicOperation, key, value);
  }

  public void putIndexValue(YTDatabaseSessionInternal session, int indexId, final Object key,
      final Object value)
      throws OInvalidIndexEngineIdException {
    final int engineAPIVersion = extractEngineAPIVersion(indexId);

    if (engineAPIVersion != 0) {
      throw new IllegalStateException(
          "Unsupported version of index engine API. Required 0 but found " + engineAPIVersion);
    }

    try {
      assert transaction.get() != null;
      final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
      assert atomicOperation != null;
      putIndexValueInternal(session, atomicOperation, indexId, key, value);

    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private void putIndexValueInternal(
      YTDatabaseSessionInternal session, OAtomicOperation atomicOperation, final int indexId,
      final Object key, final Object value)
      throws OInvalidIndexEngineIdException {
    try {
      checkIndexId(indexId);

      final OBaseIndexEngine engine = indexEngines.get(indexId);
      assert engine.getId() == indexId;

      ((OIndexEngine) engine).put(session, atomicOperation, key, value);
    } catch (final IOException e) {
      throw YTException.wrapException(
          new YTStorageException(
              "Cannot put key " + key + " value " + value + " entry to the index"),
          e);
    }
  }

  /**
   * Puts the given value under the given key into this storage for the index with the given index
   * id. Validates the operation using the provided validator.
   *
   * @param indexId   the index id of the index to put the value into.
   * @param key       the key to put the value under.
   * @param value     the value to put.
   * @param validator the operation validator.
   * @return {@code true} if the validator allowed the put, {@code false} otherwise.
   * @see IndexEngineValidator#validate(Object, Object, Object)
   */
  @SuppressWarnings("UnusedReturnValue")
  public boolean validatedPutIndexValue(
      final int indexId,
      final Object key,
      final YTRID value,
      final IndexEngineValidator<Object, YTRID> validator)
      throws OInvalidIndexEngineIdException {
    final int internalIndexId = extractInternalId(indexId);

    try {
      assert transaction.get() != null;
      final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
      assert atomicOperation != null;
      return doValidatedPutIndexValue(atomicOperation, internalIndexId, key, value, validator);
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private boolean doValidatedPutIndexValue(
      OAtomicOperation atomicOperation,
      final int indexId,
      final Object key,
      final YTRID value,
      final IndexEngineValidator<Object, YTRID> validator)
      throws OInvalidIndexEngineIdException {
    try {
      checkIndexId(indexId);

      final OBaseIndexEngine engine = indexEngines.get(indexId);
      assert indexId == engine.getId();

      if (engine instanceof OIndexEngine) {
        return ((OIndexEngine) engine).validatedPut(atomicOperation, key, value, validator);
      }

      if (engine instanceof OSingleValueIndexEngine) {
        return ((OSingleValueIndexEngine) engine)
            .validatedPut(atomicOperation, key, value.getIdentity(), validator);
      }

      throw new IllegalStateException(
          "Invalid type of index engine " + engine.getClass().getName());
    } catch (final IOException e) {
      throw YTException.wrapException(
          new YTStorageException(
              "Cannot put key " + key + " value " + value + " entry to the index"),
          e);
    }
  }

  public Stream<ORawPair<Object, YTRID>> iterateIndexEntriesBetween(
      YTDatabaseSessionInternal session, int indexId,
      final Object rangeFrom,
      final boolean fromInclusive,
      final Object rangeTo,
      final boolean toInclusive,
      final boolean ascSortOrder,
      final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doIterateIndexEntriesBetween(session,
            indexId, rangeFrom, fromInclusive, rangeTo, toInclusive, ascSortOrder, transformer);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doIterateIndexEntriesBetween(session,
            indexId, rangeFrom, fromInclusive, rangeTo, toInclusive, ascSortOrder, transformer);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Stream<ORawPair<Object, YTRID>> doIterateIndexEntriesBetween(
      YTDatabaseSessionInternal session, final int indexId,
      final Object rangeFrom,
      final boolean fromInclusive,
      final Object rangeTo,
      final boolean toInclusive,
      final boolean ascSortOrder,
      final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.iterateEntriesBetween(session
        , rangeFrom, fromInclusive, rangeTo, toInclusive, ascSortOrder, transformer);
  }

  public Stream<ORawPair<Object, YTRID>> iterateIndexEntriesMajor(
      int indexId,
      final Object fromKey,
      final boolean isInclusive,
      final boolean ascSortOrder,
      final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doIterateIndexEntriesMajor(indexId, fromKey, isInclusive, ascSortOrder, transformer);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doIterateIndexEntriesMajor(indexId, fromKey, isInclusive, ascSortOrder, transformer);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Stream<ORawPair<Object, YTRID>> doIterateIndexEntriesMajor(
      final int indexId,
      final Object fromKey,
      final boolean isInclusive,
      final boolean ascSortOrder,
      final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.iterateEntriesMajor(fromKey, isInclusive, ascSortOrder, transformer);
  }

  public Stream<ORawPair<Object, YTRID>> iterateIndexEntriesMinor(
      int indexId,
      final Object toKey,
      final boolean isInclusive,
      final boolean ascSortOrder,
      final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doIterateIndexEntriesMinor(indexId, toKey, isInclusive, ascSortOrder, transformer);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doIterateIndexEntriesMinor(indexId, toKey, isInclusive, ascSortOrder, transformer);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Stream<ORawPair<Object, YTRID>> doIterateIndexEntriesMinor(
      final int indexId,
      final Object toKey,
      final boolean isInclusive,
      final boolean ascSortOrder,
      final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.iterateEntriesMinor(toKey, isInclusive, ascSortOrder, transformer);
  }

  public Stream<ORawPair<Object, YTRID>> getIndexStream(
      int indexId, final IndexEngineValuesTransformer valuesTransformer)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doGetIndexStream(indexId, valuesTransformer);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doGetIndexStream(indexId, valuesTransformer);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Stream<ORawPair<Object, YTRID>> doGetIndexStream(
      final int indexId, final IndexEngineValuesTransformer valuesTransformer)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.stream(valuesTransformer);
  }

  public Stream<ORawPair<Object, YTRID>> getIndexDescStream(
      int indexId, final IndexEngineValuesTransformer valuesTransformer)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doGetIndexDescStream(indexId, valuesTransformer);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doGetIndexDescStream(indexId, valuesTransformer);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Stream<ORawPair<Object, YTRID>> doGetIndexDescStream(
      final int indexId, final IndexEngineValuesTransformer valuesTransformer)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.descStream(valuesTransformer);
  }

  public Stream<Object> getIndexKeyStream(int indexId) throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doGetIndexKeyStream(indexId);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doGetIndexKeyStream(indexId);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private Stream<Object> doGetIndexKeyStream(final int indexId)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.keyStream();
  }

  public long getIndexSize(int indexId, final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doGetIndexSize(indexId, transformer);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doGetIndexSize(indexId, transformer);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private long doGetIndexSize(final int indexId, final IndexEngineValuesTransformer transformer)
      throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.size(transformer);
  }

  public boolean hasIndexRangeQuerySupport(int indexId) throws OInvalidIndexEngineIdException {
    indexId = extractInternalId(indexId);

    try {
      if (transaction.get() != null) {
        return doHasRangeQuerySupport(indexId);
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return doHasRangeQuerySupport(indexId);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final OInvalidIndexEngineIdException ie) {
      throw logAndPrepareForRethrow(ie);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  private boolean doHasRangeQuerySupport(final int indexId) throws OInvalidIndexEngineIdException {
    checkIndexId(indexId);

    final OBaseIndexEngine engine = indexEngines.get(indexId);
    assert indexId == engine.getId();

    return engine.hasRangeQuerySupport();
  }

  private void rollback(final Throwable error) throws IOException {
    assert transaction.get() != null;
    atomicOperationsManager.endAtomicOperation(error);

    assert atomicOperationsManager.getCurrentOperation() == null;

    txRollback.increment();
  }

  public void moveToErrorStateIfNeeded(final Throwable error) {
    if (error != null
        && !((error instanceof YTHighLevelException)
        || (error instanceof YTNeedRetryException)
        || (error instanceof YTInternalErrorException))) {
      setInError(error);
    }
  }

  @Override
  public final boolean checkForRecordValidity(final OPhysicalPosition ppos) {
    try {
      return ppos != null && !ORecordVersionHelper.isTombstone(ppos.recordVersion);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final void synch() {
    try {
      stateLock.readLock().lock();
      try {

        final long timer = YouTrackDBManager.instance().getProfiler().startChrono();
        final long lockId = atomicOperationsManager.freezeAtomicOperations(null, null);
        try {
          checkOpennessAndMigration();

          if (!isInError()) {
            for (final OBaseIndexEngine indexEngine : indexEngines) {
              try {
                if (indexEngine != null) {
                  indexEngine.flush();
                }
              } catch (final Throwable t) {
                LogManager.instance()
                    .error(
                        this,
                        "Error while flushing index via index engine of class %s.",
                        t,
                        indexEngine.getClass().getSimpleName());
              }
            }

            flushAllData();

          } else {
            LogManager.instance()
                .error(
                    this,
                    "Sync can not be performed because of internal error in storage %s",
                    null,
                    this.name);
          }

        } finally {
          atomicOperationsManager.releaseAtomicOperations(lockId);
          YouTrackDBManager.instance()
              .getProfiler()
              .stopChrono("db." + name + ".synch", "Synch a database", timer, "db.*.synch");
        }
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final String getPhysicalClusterNameById(final int iClusterId) {
    try {
      stateLock.readLock().lock();
      try {
        checkOpennessAndMigration();

        if (iClusterId < 0 || iClusterId >= clusters.size()) {
          return null;
        }

        return clusters.get(iClusterId) != null ? clusters.get(iClusterId).getName() : null;
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final int getDefaultClusterId() {
    return defaultClusterId;
  }

  @Override
  public final void setDefaultClusterId(final int defaultClusterId) {
    this.defaultClusterId = defaultClusterId;
  }

  @Override
  public String getClusterName(YTDatabaseSessionInternal database, int clusterId) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      if (clusterId == YTRID.CLUSTER_ID_INVALID) {
        clusterId = defaultClusterId;
      }

      return doGetAndCheckCluster(clusterId).getName();

    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final long getSize(YTDatabaseSessionInternal session) {
    try {
      try {
        long size = 0;

        stateLock.readLock().lock();
        try {

          checkOpennessAndMigration();

          for (final OCluster c : clusters) {
            if (c != null) {
              size += c.getRecordsSize();
            }
          }
        } finally {
          stateLock.readLock().unlock();
        }

        return size;
      } catch (final IOException ioe) {
        throw YTException.wrapException(new YTStorageException("Cannot calculate records size"),
            ioe);
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final int getClusters() {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        return clusterMap.size();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final Set<OCluster> getClusterInstances() {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final Set<OCluster> result = new HashSet<>(1024);

        // ADD ALL THE CLUSTERS
        for (final OCluster c : clusters) {
          if (c != null) {
            result.add(c);
          }
        }

        return result;

      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final boolean cleanOutRecord(
      YTDatabaseSessionInternal session, final YTRecordId recordId,
      final int recordVersion,
      final int iMode,
      final ORecordCallback<Boolean> callback) {
    return deleteRecord(recordId, recordVersion, iMode, callback).getResult();
  }

  @Override
  public final void freeze(final boolean throwException) {
    try {
      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        if (throwException) {
          atomicOperationsManager.freezeAtomicOperations(
              YTModificationOperationProhibitedException.class,
              "Modification requests are prohibited");
        } else {
          atomicOperationsManager.freezeAtomicOperations(null, null);
        }

        final List<OFreezableStorageComponent> frozenIndexes = new ArrayList<>(indexEngines.size());
        try {
          for (final OBaseIndexEngine indexEngine : indexEngines) {
            if (indexEngine instanceof OFreezableStorageComponent) {
              ((OFreezableStorageComponent) indexEngine).freeze(false);
              frozenIndexes.add((OFreezableStorageComponent) indexEngine);
            }
          }
        } catch (final Exception e) {
          // RELEASE ALL THE FROZEN INDEXES
          for (final OFreezableStorageComponent indexEngine : frozenIndexes) {
            indexEngine.release();
          }

          throw YTException.wrapException(
              new YTStorageException("Error on freeze of storage '" + name + "'"), e);
        }

        synch();
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final void release() {
    try {
      for (final OBaseIndexEngine indexEngine : indexEngines) {
        if (indexEngine instanceof OFreezableStorageComponent) {
          ((OFreezableStorageComponent) indexEngine).release();
        }
      }

      atomicOperationsManager.releaseAtomicOperations(-1);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public final boolean isRemote() {
    return false;
  }

  public boolean wereDataRestoredAfterOpen() {
    return wereDataRestoredAfterOpen;
  }

  public boolean wereNonTxOperationsPerformedInPreviousOpen() {
    return wereNonTxOperationsPerformedInPreviousOpen;
  }

  @Override
  public final void reload(YTDatabaseSessionInternal database) {
    try {
      close(database);
      open(new YTContextConfiguration());
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @SuppressWarnings("unused")
  public static String getMode() {
    return "rw";
  }

  /**
   * @inheritDoc
   */
  @Override
  public final void pageIsBroken(final String fileName, final long pageIndex) {
    LogManager.instance()
        .error(
            this,
            "In storage %s file with name '%s' has broken page under the index %d",
            null,
            name,
            fileName,
            pageIndex);

    if (status == STATUS.CLOSED) {
      return;
    }

    setInError(new YTStorageException("Page " + pageIndex + " is broken in file " + fileName));

    try {
      makeStorageDirty();
    } catch (final IOException e) {
      // ignore
    }
  }

  @Override
  public final void requestCheckpoint() {
    try {
      if (!walVacuumInProgress.get() && walVacuumInProgress.compareAndSet(false, true)) {
        fuzzyCheckpointExecutor.submit(new OWALVacuum(this));
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  /**
   * Executes the command request and return the result back.
   */
  @Override
  public final Object command(YTDatabaseSessionInternal database,
      final CommandRequestText command) {
    try {
      var db = database;
      assert db.assertIfNotActive();

      while (true) {
        try {
          final CommandExecutor executor =
              db.getSharedContext()
                  .getYouTrackDB()
                  .getScriptManager()
                  .getCommandManager()
                  .getExecutor(command);
          // COPY THE CONTEXT FROM THE REQUEST
          var context = (BasicCommandContext) command.getContext();
          context.setDatabase(db);
          executor.setContext(command.getContext());
          executor.setProgressListener(command.getProgressListener());
          executor.parse(command);
          return executeCommand(database, command, executor);
        } catch (final YTRetryQueryException ignore) {
          if (command instanceof QueryAbstract<?> query) {
            query.reset();
          }
        }
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public final Object executeCommand(
      YTDatabaseSessionInternal session, final CommandRequestText iCommand,
      final CommandExecutor executor) {
    try {
      if (iCommand.isIdempotent() && !executor.isIdempotent()) {
        throw new YTCommandExecutionException("Cannot execute non idempotent command");
      }
      final long beginTime = YouTrackDBManager.instance().getProfiler().startChrono();
      try {
        final YTDatabaseSessionInternal db = ODatabaseRecordThreadLocal.instance().get();
        // CALL BEFORE COMMAND
        final Iterable<YTDatabaseListener> listeners = db.getListeners();
        for (final YTDatabaseListener oDatabaseListener : listeners) {
          //noinspection deprecation
          oDatabaseListener.onBeforeCommand(iCommand, executor);
        }

        // EXECUTE THE COMMAND
        final Map<Object, Object> params = iCommand.getParameters();
        Object result = executor.execute(params, session);

        // CALL AFTER COMMAND
        for (final YTDatabaseListener oDatabaseListener : listeners) {
          //noinspection deprecation
          oDatabaseListener.onAfterCommand(iCommand, executor, result);
        }

        return result;

      } catch (final YTException e) {
        // PASS THROUGH
        throw e;
      } catch (final Exception e) {
        throw YTException.wrapException(
            new YTCommandExecutionException("Error on execution of command: " + iCommand), e);

      } finally {
        if (YouTrackDBManager.instance().getProfiler().isRecording()) {
          final YTDatabaseSessionInternal db = ODatabaseRecordThreadLocal.instance().getIfDefined();
          if (db != null) {
            final YTSecurityUser user = db.getUser();
            final String userString = Optional.ofNullable(user).map(Object::toString).orElse(null);
            YouTrackDBManager.instance()
                .getProfiler()
                .stopChrono(
                    "db."
                        + ODatabaseRecordThreadLocal.instance().get().getName()
                        + ".command."
                        + iCommand,
                    "Command executed against the database",
                    beginTime,
                    "db.*.command.*",
                    null,
                    userString);
          }
        }
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final OPhysicalPosition[] higherPhysicalPositions(
      YTDatabaseSessionInternal session, final int currentClusterId,
      final OPhysicalPosition physicalPosition) {
    try {
      if (currentClusterId == -1) {
        return new OPhysicalPosition[0];
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OCluster cluster = doGetAndCheckCluster(currentClusterId);
        return cluster.higherPositions(physicalPosition);
      } catch (final IOException ioe) {
        throw YTException.wrapException(
            new YTStorageException(
                "Cluster Id " + currentClusterId + " is invalid in storage '" + name + '\''),
            ioe);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final OPhysicalPosition[] ceilingPhysicalPositions(
      YTDatabaseSessionInternal session, final int clusterId,
      final OPhysicalPosition physicalPosition) {
    try {
      if (clusterId == -1) {
        return new OPhysicalPosition[0];
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OCluster cluster = doGetAndCheckCluster(clusterId);
        return cluster.ceilingPositions(physicalPosition);
      } catch (final IOException ioe) {
        throw YTException.wrapException(
            new YTStorageException(
                "Cluster Id " + clusterId + " is invalid in storage '" + name + '\''),
            ioe);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final OPhysicalPosition[] lowerPhysicalPositions(
      YTDatabaseSessionInternal session, final int currentClusterId,
      final OPhysicalPosition physicalPosition) {
    try {
      if (currentClusterId == -1) {
        return new OPhysicalPosition[0];
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();

        final OCluster cluster = doGetAndCheckCluster(currentClusterId);

        return cluster.lowerPositions(physicalPosition);
      } catch (final IOException ioe) {
        throw YTException.wrapException(
            new YTStorageException(
                "Cluster Id " + currentClusterId + " is invalid in storage '" + name + '\''),
            ioe);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final OPhysicalPosition[] floorPhysicalPositions(
      YTDatabaseSessionInternal session, final int clusterId,
      final OPhysicalPosition physicalPosition) {
    try {
      if (clusterId == -1) {
        return new OPhysicalPosition[0];
      }

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();
        final OCluster cluster = doGetAndCheckCluster(clusterId);

        return cluster.floorPositions(physicalPosition);
      } catch (final IOException ioe) {
        throw YTException.wrapException(
            new YTStorageException(
                "Cluster Id " + clusterId + " is invalid in storage '" + name + '\''),
            ioe);
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public final ORecordConflictStrategy getRecordConflictStrategy() {
    return recordConflictStrategy;
  }

  @Override
  public final void setConflictStrategy(final ORecordConflictStrategy conflictResolver) {
    Objects.requireNonNull(conflictResolver);
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null, atomicOperation -> doSetConflictStrategy(conflictResolver, atomicOperation));
    } catch (final Exception e) {
      throw YTException.wrapException(
          new YTStorageException(
              "Exception during setting of conflict strategy "
                  + conflictResolver.getName()
                  + " for storage "
                  + name),
          e);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  private void doSetConflictStrategy(
      ORecordConflictStrategy conflictResolver, OAtomicOperation atomicOperation) {

    if (recordConflictStrategy == null
        || !recordConflictStrategy.getName().equals(conflictResolver.getName())) {

      this.recordConflictStrategy = conflictResolver;
      ((OClusterBasedStorageConfiguration) configuration)
          .setConflictStrategy(atomicOperation, conflictResolver.getName());
    }
  }

  @SuppressWarnings("unused")
  public long getRecordScanned() {
    return recordScanned.value;
  }

  @SuppressWarnings("unused")
  protected abstract OLogSequenceNumber copyWALToIncrementalBackup(
      ZipOutputStream zipOutputStream, long startSegment) throws IOException;

  @SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
  protected abstract boolean isWriteAllowedDuringIncrementalBackup();

  @SuppressWarnings("unused")
  public OStorageRecoverListener getRecoverListener() {
    return recoverListener;
  }

  public void registerRecoverListener(final OStorageRecoverListener recoverListener) {
    this.recoverListener = recoverListener;
  }

  @SuppressWarnings("unused")
  public void unregisterRecoverListener(final OStorageRecoverListener recoverListener) {
    if (this.recoverListener == recoverListener) {
      this.recoverListener = null;
    }
  }

  @SuppressWarnings("unused")
  protected abstract File createWalTempDirectory();

  @SuppressWarnings("unused")
  protected abstract OWriteAheadLog createWalFromIBUFiles(
      File directory,
      final YTContextConfiguration contextConfiguration,
      final Locale locale,
      byte[] iv)
      throws IOException;

  /**
   * Checks if the storage is open. If it's closed an exception is raised.
   */
  protected final void checkOpennessAndMigration() {
    checkErrorState();

    final STATUS status = this.status;

    if (status == STATUS.MIGRATION) {
      throw new YTStorageException(
          "Storage data are under migration procedure, please wait till data will be migrated.");
    }

    if (status != STATUS.OPEN) {
      throw new YTStorageException("Storage " + name + " is not opened.");
    }
  }

  protected boolean isInError() {
    return this.error.get() != null;
  }

  public void checkErrorState() {
    if (this.error.get() != null) {
      throw YTException.wrapException(
          new YTStorageException(
              "Internal error happened in storage "
                  + name
                  + " please restart the server or re-open the storage to undergo the restore"
                  + " process and fix the error."),
          this.error.get());
    }
  }

  public final void makeFuzzyCheckpoint() {
    // check every 1 ms.
    while (true) {
      try {
        if (stateLock.readLock().tryLock(1, TimeUnit.MILLISECONDS)) {
          break;
        }
      } catch (InterruptedException e) {
        throw YTException.wrapException(
            new YTInterruptedException("Fuzzy check point was interrupted"), e);
      }

      if (status != STATUS.OPEN || status != STATUS.MIGRATION) {
        return;
      }
    }

    try {

      if (status != STATUS.OPEN || status != STATUS.MIGRATION) {
        return;
      }

      OLogSequenceNumber beginLSN = writeAheadLog.begin();
      OLogSequenceNumber endLSN = writeAheadLog.end();

      final Long minLSNSegment = writeCache.getMinimalNotFlushedSegment();

      long fuzzySegment;

      if (minLSNSegment != null) {
        fuzzySegment = minLSNSegment;
      } else {
        if (endLSN == null) {
          return;
        }

        fuzzySegment = endLSN.getSegment();
      }

      atomicOperationsTable.compactTable();
      final long minAtomicOperationSegment =
          atomicOperationsTable.getSegmentEarliestNotPersistedOperation();
      if (minAtomicOperationSegment >= 0 && fuzzySegment > minAtomicOperationSegment) {
        fuzzySegment = minAtomicOperationSegment;
      }
      LogManager.instance()
          .debug(
              this,
              "Before fuzzy checkpoint: min LSN segment is %s, "
                  + "WAL begin is %s, WAL end is %s fuzzy segment is %d",
              minLSNSegment,
              beginLSN,
              endLSN,
              fuzzySegment);

      if (fuzzySegment > beginLSN.getSegment() && beginLSN.getSegment() < endLSN.getSegment()) {
        LogManager.instance().debug(this, "Making fuzzy checkpoint");
        writeCache.syncDataFiles(fuzzySegment, lastMetadata);

        beginLSN = writeAheadLog.begin();
        endLSN = writeAheadLog.end();

        LogManager.instance()
            .debug(this, "After fuzzy checkpoint: WAL begin is %s WAL end is %s", beginLSN, endLSN);
      } else {
        LogManager.instance().debug(this, "No reason to make fuzzy checkpoint");
      }
    } catch (final IOException ioe) {
      throw YTException.wrapException(new OIOException("Error during fuzzy checkpoint"), ioe);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  public void deleteTreeRidBag(final OSBTreeRidBag ridBag) {
    try {
      checkOpennessAndMigration();

      assert transaction.get() != null;
      deleteTreeRidBag(ridBag, atomicOperationsManager.getCurrentOperation());
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  private void deleteTreeRidBag(OSBTreeRidBag ridBag, OAtomicOperation atomicOperation) {
    final OBonsaiCollectionPointer collectionPointer = ridBag.getCollectionPointer();
    checkOpennessAndMigration();

    try {
      makeStorageDirty();
      sbTreeCollectionManager.delete(atomicOperation, collectionPointer);
    } catch (final Exception e) {
      LogManager.instance().error(this, "Error during deletion of rid bag", e);
      throw YTException.wrapException(new YTStorageException("Error during deletion of ridbag"), e);
    }

    ridBag.confirmDelete();
  }

  protected void flushAllData() {
    try {
      writeAheadLog.flush();

      // so we will be able to cut almost all the log
      writeAheadLog.appendNewSegment();

      final OLogSequenceNumber lastLSN;
      if (lastMetadata != null) {
        lastLSN = writeAheadLog.log(new MetaDataRecord(lastMetadata));
      } else {
        lastLSN = writeAheadLog.log(new EmptyWALRecord());
      }

      writeCache.flush();

      atomicOperationsTable.compactTable();
      final long operationSegment = atomicOperationsTable.getSegmentEarliestOperationInProgress();
      if (operationSegment >= 0) {
        throw new IllegalStateException(
            "Can not perform full checkpoint if some of atomic operations in progress");
      }

      writeAheadLog.flush();

      writeAheadLog.cutTill(lastLSN);

      clearStorageDirty();

    } catch (final IOException ioe) {
      throw YTException.wrapException(
          new YTStorageException("Error during checkpoint creation for storage " + name), ioe);
    }
  }

  protected OStartupMetadata checkIfStorageDirty() throws IOException {
    return new OStartupMetadata(-1, null);
  }

  protected void initConfiguration(
      final YTContextConfiguration contextConfiguration,
      OAtomicOperation atomicOperation)
      throws IOException {
  }

  @SuppressWarnings({"EmptyMethod"})
  protected final void postCreateSteps() {
  }

  protected void preCreateSteps() throws IOException {
  }

  protected abstract void initWalAndDiskCache(YTContextConfiguration contextConfiguration)
      throws IOException, InterruptedException;

  protected abstract void postCloseSteps(
      @SuppressWarnings("unused") boolean onDelete, boolean internalError, long lastTxId)
      throws IOException;

  @SuppressWarnings({"EmptyMethod"})
  protected Map<String, Object> preCloseSteps() {
    return new HashMap<>(2);
  }

  protected void postDeleteSteps() {
  }

  protected void makeStorageDirty() throws IOException {
  }

  protected void clearStorageDirty() throws IOException {
  }

  protected boolean isDirty() {
    return false;
  }

  protected String getOpenedAtVersion() {
    return null;
  }

  @Nonnull
  private ORawBuffer readRecord(final YTRecordId rid, final boolean prefetchRecords) {

    if (!rid.isPersistent()) {
      throw new YTRecordNotFoundException(
          rid,
          "Cannot read record "
              + rid
              + " since the position is invalid in database '"
              + name
              + '\'');
    }

    if (transaction.get() != null) {
      checkOpennessAndMigration();
      final OCluster cluster;
      try {
        cluster = doGetAndCheckCluster(rid.getClusterId());
      } catch (IllegalArgumentException e) {
        throw YTException.wrapException(new YTRecordNotFoundException(rid), e);
      }
      // Disabled this assert have no meaning anymore
      // assert iLockingStrategy.equals(LOCKING_STRATEGY.DEFAULT);
      return doReadRecord(cluster, rid, prefetchRecords);
    }

    stateLock.readLock().lock();
    try {
      checkOpennessAndMigration();
      final OCluster cluster;
      try {
        cluster = doGetAndCheckCluster(rid.getClusterId());
      } catch (IllegalArgumentException e) {
        throw YTException.wrapException(new YTRecordNotFoundException(rid), e);
      }
      return doReadRecord(cluster, rid, prefetchRecords);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public boolean recordExists(YTDatabaseSessionInternal session, YTRID rid) {
    if (!rid.isPersistent()) {
      throw new YTRecordNotFoundException(
          rid,
          "Cannot read record "
              + rid
              + " since the position is invalid in database '"
              + name
              + '\'');
    }

    if (transaction.get() != null) {
      checkOpennessAndMigration();
      final OCluster cluster;
      try {
        cluster = doGetAndCheckCluster(rid.getClusterId());
      } catch (IllegalArgumentException e) {
        return false;
      }

      return doRecordExists(cluster, rid);
    }

    stateLock.readLock().lock();
    try {
      checkOpennessAndMigration();
      final OCluster cluster;
      try {
        cluster = doGetAndCheckCluster(rid.getClusterId());
      } catch (IllegalArgumentException e) {
        return false;
      }
      return doRecordExists(cluster, rid);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  private void endStorageTx() throws IOException {
    atomicOperationsManager.endAtomicOperation(null);
    assert atomicOperationsManager.getCurrentOperation() == null;

    txCommit.increment();
  }

  private void startStorageTx(final OTransactionInternal clientTx) throws IOException {
    final OStorageTransaction storageTx = transaction.get();
    assert storageTx == null || storageTx.getClientTx().getId() == clientTx.getId();
    assert atomicOperationsManager.getCurrentOperation() == null;
    transaction.set(new OStorageTransaction(clientTx));
    try {
      final OAtomicOperation atomicOperation =
          atomicOperationsManager.startAtomicOperation(clientTx.getMetadata());
      if (clientTx.getMetadata() != null) {
        this.lastMetadata = clientTx.getMetadata();
      }
      clientTx.storageBegun();
      Iterator<byte[]> ops = clientTx.getSerializedOperations();
      while (ops.hasNext()) {
        byte[] next = ops.next();
        writeAheadLog.log(
            new OHighLevelTransactionChangeRecord(atomicOperation.getOperationUnitId(), next));
      }
    } catch (final RuntimeException e) {
      transaction.set(null);
      throw e;
    }
  }

  public void metadataOnly(byte[] metadata) {
    try {
      atomicOperationsManager.executeInsideAtomicOperation(metadata, (op) -> {
      });
      this.lastMetadata = metadata;
    } catch (IOException e) {
      throw logAndPrepareForRethrow(e);
    }
  }

  private void recoverIfNeeded() throws Exception {
    if (isDirty()) {
      LogManager.instance()
          .warn(
              this,
              "Storage '"
                  + name
                  + "' was not closed properly. Will try to recover from write ahead log");
      try {
        final String openedAtVersion = getOpenedAtVersion();

        if (openedAtVersion != null && !openedAtVersion.equals(OConstants.getRawVersion())) {
          throw new YTStorageException(
              "Database has been opened at version "
                  + openedAtVersion
                  + " but is attempted to be restored at version "
                  + OConstants.getRawVersion()
                  + ". Please use correct version to restore database.");
        }

        wereDataRestoredAfterOpen = true;
        restoreFromWAL();

        if (recoverListener != null) {
          recoverListener.onStorageRecover();
        }

        flushAllData();
      } catch (final Exception e) {
        LogManager.instance().error(this, "Exception during storage data restore", e);
        throw e;
      }

      LogManager.instance().info(this, "Storage data recover was completed");
    }
  }

  private OStorageOperationResult<OPhysicalPosition> doCreateRecord(
      final OAtomicOperation atomicOperation,
      final YTRecordId rid,
      @Nonnull final byte[] content,
      int recordVersion,
      final byte recordType,
      final ORecordCallback<Long> callback,
      final OCluster cluster,
      final OPhysicalPosition allocated) {
    //noinspection ConstantValue
    if (content == null) {
      throw new IllegalArgumentException("Record is null");
    }

    if (recordVersion > -1) {
      recordVersion++;
    } else {
      recordVersion = 0;
    }

    OPhysicalPosition ppos;
    try {
      ppos = cluster.createRecord(content, recordVersion, recordType, allocated, atomicOperation);
      rid.setClusterPosition(ppos.clusterPosition);

      final ORecordSerializationContext context = ORecordSerializationContext.getContext();
      if (context != null) {
        context.executeOperations(atomicOperation, this);
      }
    } catch (final Exception e) {
      LogManager.instance().error(this, "Error on creating record in cluster: " + cluster, e);
      throw YTDatabaseException.wrapException(
          new YTStorageException("Error during creation of record"), e);
    }

    if (callback != null) {
      callback.call(rid, ppos.clusterPosition);
    }

    if (LogManager.instance().isDebugEnabled()) {
      LogManager.instance()
          .debug(this, "Created record %s v.%s size=%d bytes", rid, recordVersion, content.length);
    }

    recordCreated.increment();

    return new OStorageOperationResult<>(ppos);
  }

  private OStorageOperationResult<Integer> doUpdateRecord(
      final OAtomicOperation atomicOperation,
      final YTRecordId rid,
      final boolean updateContent,
      byte[] content,
      final int version,
      final byte recordType,
      final ORecordCallback<Integer> callback,
      final OCluster cluster) {

    YouTrackDBManager.instance().getProfiler().startChrono();
    try {

      final OPhysicalPosition ppos =
          cluster.getPhysicalPosition(new OPhysicalPosition(rid.getClusterPosition()));
      if (!checkForRecordValidity(ppos)) {
        final int recordVersion = -1;
        if (callback != null) {
          callback.call(rid, recordVersion);
        }

        return new OStorageOperationResult<>(recordVersion);
      }

      boolean contentModified = false;
      if (updateContent) {
        final AtomicInteger recVersion = new AtomicInteger(version);
        final AtomicInteger dbVersion = new AtomicInteger(ppos.recordVersion);

        final byte[] newContent = checkAndIncrementVersion(rid, recVersion, dbVersion);

        ppos.recordVersion = dbVersion.get();

        // REMOVED BECAUSE DISTRIBUTED COULD UNDO AN OPERATION RESTORING A LOWER VERSION
        // assert ppos.recordVersion >= oldVersion;

        if (newContent != null) {
          contentModified = true;
          content = newContent;
        }
      }

      if (updateContent) {
        cluster.updateRecord(
            rid.getClusterPosition(), content, ppos.recordVersion, recordType, atomicOperation);
      }

      final ORecordSerializationContext context = ORecordSerializationContext.getContext();
      if (context != null) {
        context.executeOperations(atomicOperation, this);
      }

      // if we do not update content of the record we should keep version of the record the same
      // otherwise we would have issues when two records may have the same version but different
      // content
      final int newRecordVersion;
      if (updateContent) {
        newRecordVersion = ppos.recordVersion;
      } else {
        newRecordVersion = version;
      }

      if (callback != null) {
        callback.call(rid, newRecordVersion);
      }

      if (LogManager.instance().isDebugEnabled()) {
        LogManager.instance()
            .debug(this, "Updated record %s v.%s size=%d", rid, newRecordVersion, content.length);
      }

      recordUpdated.increment();

      if (contentModified) {
        return new OStorageOperationResult<>(newRecordVersion, content, false);
      } else {
        return new OStorageOperationResult<>(newRecordVersion);
      }
    } catch (final YTConcurrentModificationException e) {
      recordConflict.increment();
      throw e;
    } catch (final IOException ioe) {
      throw YTException.wrapException(
          new YTStorageException(
              "Error on updating record " + rid + " (cluster: " + cluster.getName() + ")"),
          ioe);
    }
  }

  private OStorageOperationResult<Boolean> doDeleteRecord(
      final OAtomicOperation atomicOperation,
      final YTRecordId rid,
      final int version,
      final OCluster cluster) {
    YouTrackDBManager.instance().getProfiler().startChrono();
    try {

      final OPhysicalPosition ppos =
          cluster.getPhysicalPosition(new OPhysicalPosition(rid.getClusterPosition()));

      if (ppos == null) {
        // ALREADY DELETED
        return new OStorageOperationResult<>(false);
      }

      // MVCC TRANSACTION: CHECK IF VERSION IS THE SAME
      if (version > -1 && ppos.recordVersion != version) {
        recordConflict.increment();

        if (YTFastConcurrentModificationException.enabled()) {
          throw YTFastConcurrentModificationException.instance();
        } else {
          throw new YTConcurrentModificationException(
              rid, ppos.recordVersion, version, ORecordOperation.DELETED);
        }
      }

      cluster.deleteRecord(atomicOperation, ppos.clusterPosition);

      final ORecordSerializationContext context = ORecordSerializationContext.getContext();
      if (context != null) {
        context.executeOperations(atomicOperation, this);
      }

      if (LogManager.instance().isDebugEnabled()) {
        LogManager.instance().debug(this, "Deleted record %s v.%s", rid, version);
      }

      recordDeleted.increment();

      return new OStorageOperationResult<>(true);
    } catch (final IOException ioe) {
      throw YTException.wrapException(
          new YTStorageException(
              "Error on deleting record " + rid + "( cluster: " + cluster.getName() + ")"),
          ioe);
    }
  }

  @Nonnull
  private ORawBuffer doReadRecord(
      final OCluster clusterSegment, final YTRecordId rid, final boolean prefetchRecords) {
    try {

      final ORawBuffer buff = clusterSegment.readRecord(rid.getClusterPosition(), prefetchRecords);

      if (LogManager.instance().isDebugEnabled()) {
        LogManager.instance()
            .debug(
                this,
                "Read record %s v.%s size=%d bytes",
                rid,
                buff.version,
                buff.buffer != null ? buff.buffer.length : 0);
      }

      recordRead.increment();

      return buff;
    } catch (final IOException e) {
      throw YTException.wrapException(
          new YTStorageException("Error during read of record with rid = " + rid), e);
    }
  }

  private static boolean doRecordExists(final OCluster clusterSegment, final YTRID rid) {
    try {
      return clusterSegment.exists(rid.getClusterPosition());
    } catch (final IOException e) {
      throw YTException.wrapException(
          new YTStorageException("Error during read of record with rid = " + rid), e);
    }
  }

  private int createClusterFromConfig(final OStorageClusterConfiguration config)
      throws IOException {
    OCluster cluster = clusterMap.get(config.getName().toLowerCase());

    if (cluster != null) {
      cluster.configure(this, config);
      return -1;
    }

    if (config.getStatus() == OStorageClusterConfiguration.STATUS.ONLINE) {
      cluster =
          OPaginatedClusterFactory.createCluster(
              config.getName(), configuration.getVersion(), config.getBinaryVersion(), this);
    } else {
      cluster = new OOfflineCluster(this, config.getId(), config.getName());
    }

    cluster.configure(this, config);

    return registerCluster(cluster);
  }

  private void setCluster(final int id, final OCluster cluster) {
    if (clusters.size() <= id) {
      while (clusters.size() < id) {
        clusters.add(null);
      }

      clusters.add(cluster);
    } else {
      clusters.set(id, cluster);
    }
  }

  /**
   * Register the cluster internally.
   *
   * @param cluster OCluster implementation
   * @return The id (physical position into the array) of the new cluster just created. First is 0.
   */
  private int registerCluster(final OCluster cluster) {
    final int id;

    if (cluster != null) {
      // CHECK FOR DUPLICATION OF NAMES
      if (clusterMap.containsKey(cluster.getName().toLowerCase())) {
        throw new YTConfigurationException(
            "Cannot add cluster '"
                + cluster.getName()
                + "' because it is already registered in database '"
                + name
                + "'");
      }
      // CREATE AND ADD THE NEW REF SEGMENT
      clusterMap.put(cluster.getName().toLowerCase(), cluster);
      id = cluster.getId();
    } else {
      id = clusters.size();
    }

    setCluster(id, cluster);

    return id;
  }

  private int doAddCluster(final OAtomicOperation atomicOperation, final String clusterName)
      throws IOException {
    // FIND THE FIRST AVAILABLE CLUSTER ID
    int clusterPos = clusters.size();
    for (int i = 0; i < clusters.size(); ++i) {
      if (clusters.get(i) == null) {
        clusterPos = i;
        break;
      }
    }

    return doAddCluster(atomicOperation, clusterName, clusterPos);
  }

  private int doAddCluster(
      final OAtomicOperation atomicOperation, String clusterName, final int clusterPos)
      throws IOException {
    final PaginatedCluster cluster;
    if (clusterName != null) {
      clusterName = clusterName.toLowerCase();

      cluster =
          OPaginatedClusterFactory.createCluster(
              clusterName,
              configuration.getVersion(),
              configuration
                  .getContextConfiguration()
                  .getValueAsInteger(GlobalConfiguration.STORAGE_CLUSTER_VERSION),
              this);
      cluster.configure(clusterPos, clusterName);
    } else {
      cluster = null;
    }

    int createdClusterId = -1;

    if (cluster != null) {
      cluster.create(atomicOperation);
      createdClusterId = registerCluster(cluster);

      ((OClusterBasedStorageConfiguration) configuration)
          .updateCluster(atomicOperation, cluster.generateClusterConfig());

      sbTreeCollectionManager.createComponent(atomicOperation, createdClusterId);
    }

    return createdClusterId;
  }

  @Override
  public boolean setClusterAttribute(final int id, final ATTRIBUTES attribute, final Object value) {
    checkBackupRunning();
    stateLock.writeLock().lock();
    try {

      checkOpennessAndMigration();

      if (id >= clusters.size()) {
        return false;
      }

      final OCluster cluster = clusters.get(id);

      if (cluster == null) {
        return false;
      }

      makeStorageDirty();

      return atomicOperationsManager.calculateInsideAtomicOperation(
          null,
          atomicOperation -> doSetClusterAttributed(atomicOperation, attribute, value, cluster));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.writeLock().unlock();
    }
  }

  private boolean doSetClusterAttributed(
      final OAtomicOperation atomicOperation,
      final ATTRIBUTES attribute,
      final Object value,
      final OCluster cluster)
      throws IOException {
    final String stringValue = Optional.ofNullable(value).map(Object::toString).orElse(null);
    switch (attribute) {
      case NAME:
        Objects.requireNonNull(stringValue);

        final String oldName = cluster.getName();
        cluster.setClusterName(stringValue);
        clusterMap.remove(oldName.toLowerCase());
        clusterMap.put(stringValue.toLowerCase(), cluster);
        break;
      case CONFLICTSTRATEGY:
        cluster.setRecordConflictStrategy(stringValue);
        break;
      case STATUS: {
        if (stringValue == null) {
          throw new IllegalStateException("Value of attribute is null");
        }

        return setClusterStatus(
            atomicOperation,
            cluster,
            OStorageClusterConfiguration.STATUS.valueOf(stringValue.toUpperCase()));
      }
      //noinspection deprecation
      case ENCRYPTION:
        throw new UnsupportedOperationException(
            "Encryption should be configured on storage level.");
      default:
        throw new IllegalArgumentException(
            "Runtime change of attribute '" + attribute + "' is not supported");
    }

    ((OClusterBasedStorageConfiguration) configuration)
        .updateCluster(atomicOperation, ((PaginatedCluster) cluster).generateClusterConfig());
    return true;
  }

  private boolean dropClusterInternal(final OAtomicOperation atomicOperation, final int clusterId)
      throws IOException {
    final OCluster cluster = clusters.get(clusterId);

    if (cluster == null) {
      return true;
    }

    cluster.delete(atomicOperation);

    clusterMap.remove(cluster.getName().toLowerCase());
    clusters.set(clusterId, null);

    return false;
  }

  protected void doShutdown() throws IOException {
    final long timer = YouTrackDBManager.instance().getProfiler().startChrono();
    try {
      if (status == STATUS.CLOSED) {
        return;
      }

      if (status != STATUS.OPEN && !isInError()) {
        throw YTException.wrapException(
            new YTStorageException("Storage " + name + " was not opened, so can not be closed"),
            this.error.get());
      }

      status = STATUS.CLOSING;

      if (!isInError()) {
        flushAllData();
        preCloseSteps();

        atomicOperationsManager.executeInsideAtomicOperation(
            null,
            atomicOperation -> {
              // we close all files inside cache system so we only clear index metadata and close
              // non core indexes
              for (final OBaseIndexEngine engine : indexEngines) {
                if (engine != null
                    && !(engine instanceof OSBTreeIndexEngine
                    || engine instanceof OHashTableIndexEngine
                    || engine instanceof OCellBTreeSingleValueIndexEngine
                    || engine instanceof OCellBTreeMultiValueIndexEngine
                    || engine instanceof OAutoShardingIndexEngine)) {
                  engine.close();
                }
              }
              ((OClusterBasedStorageConfiguration) configuration).close(atomicOperation);
            });

        sbTreeCollectionManager.close();

        // we close all files inside cache system so we only clear cluster metadata
        clusters.clear();
        clusterMap.clear();
        indexEngines.clear();
        indexEngineNameMap.clear();

        if (writeCache != null) {
          writeCache.removeBackgroundExceptionListener(this);
          writeCache.removePageIsBrokenListener(this);
        }

        writeAheadLog.removeCheckpointListener(this);

        if (readCache != null) {
          readCache.closeStorage(writeCache);
        }

        writeAheadLog.close();
      } else {
        LogManager.instance()
            .error(
                this,
                "Because of JVM error happened inside of storage it can not be properly closed",
                null);
      }

      postCloseSteps(false, isInError(), idGen.getLastId());
      transaction = null;
      lastMetadata = null;
      migration = new CountDownLatch(1);
      status = STATUS.CLOSED;
    } finally {
      YouTrackDBManager.instance()
          .getProfiler()
          .stopChrono("db." + name + ".close", "Close a database", timer, "db.*.close");
    }
  }

  private void doShutdownOnDelete() {
    if (status == STATUS.CLOSED) {
      return;
    }

    if (status != STATUS.OPEN && !isInError()) {
      throw YTException.wrapException(
          new YTStorageException("Storage " + name + " was not opened, so can not be closed"),
          this.error.get());
    }

    status = STATUS.CLOSING;
    try {
      if (!isInError()) {
        preCloseSteps();

        for (final OBaseIndexEngine engine : indexEngines) {
          if (engine != null
              && !(engine instanceof OSBTreeIndexEngine
              || engine instanceof OHashTableIndexEngine
              || engine instanceof OCellBTreeSingleValueIndexEngine
              || engine instanceof OCellBTreeMultiValueIndexEngine
              || engine instanceof OAutoShardingIndexEngine)) {
            // delete method is implemented only in non native indexes, so they do not use ODB
            // atomic operation
            engine.delete(null);
          }
        }

        sbTreeCollectionManager.close();

        // we close all files inside cache system so we only clear cluster metadata
        clusters.clear();
        clusterMap.clear();
        indexEngines.clear();
        indexEngineNameMap.clear();

        if (writeCache != null) {
          writeCache.removeBackgroundExceptionListener(this);
          writeCache.removePageIsBrokenListener(this);
        }

        writeAheadLog.removeCheckpointListener(this);

        if (readCache != null) {
          readCache.deleteStorage(writeCache);
        }

        writeAheadLog.delete();
      } else {
        LogManager.instance()
            .error(
                this,
                "Because of JVM error happened inside of storage it can not be properly closed",
                null);
      }
      postCloseSteps(true, isInError(), idGen.getLastId());
      transaction = null;
      lastMetadata = null;
      migration = new CountDownLatch(1);
      status = STATUS.CLOSED;
    } catch (final IOException e) {
      final String message = "Error on closing of storage '" + name;
      LogManager.instance().error(this, message, e);

      throw YTException.wrapException(new YTStorageException(message), e);
    }
  }

  @SuppressWarnings("unused")
  protected void closeClusters() throws IOException {
    for (final OCluster cluster : clusters) {
      if (cluster != null) {
        cluster.close(true);
      }
    }
    clusters.clear();
    clusterMap.clear();
  }

  @SuppressWarnings("unused")
  protected void closeIndexes(final OAtomicOperation atomicOperation) {
    for (final OBaseIndexEngine engine : indexEngines) {
      if (engine != null) {
        engine.close();
      }
    }

    indexEngines.clear();
    indexEngineNameMap.clear();
  }

  private static byte[] checkAndIncrementVersion(
      final YTRecordId rid, final AtomicInteger version, final AtomicInteger iDatabaseVersion) {
    final int v = version.get();
    switch (v) {
      // DOCUMENT UPDATE, NO VERSION CONTROL
      case -1:
        iDatabaseVersion.incrementAndGet();
        break;

      // DOCUMENT UPDATE, NO VERSION CONTROL, NO VERSION UPDATE
      case -2:
        break;

      default:
        // MVCC CONTROL AND RECORD UPDATE OR WRONG VERSION VALUE
        // MVCC TRANSACTION: CHECK IF VERSION IS THE SAME
        if (v < -2) {
          // OVERWRITE VERSION: THIS IS USED IN CASE OF FIX OF RECORDS IN DISTRIBUTED MODE
          version.set(ORecordVersionHelper.clearRollbackMode(v));
          iDatabaseVersion.set(version.get());
        } else if (v != iDatabaseVersion.get()) {
          throw new YTConcurrentModificationException(
              rid, iDatabaseVersion.get(), v, ORecordOperation.UPDATED);
        } else
        // OK, INCREMENT DB VERSION
        {
          iDatabaseVersion.incrementAndGet();
        }
    }
    return null;
  }

  private void commitEntry(
      OTransactionOptimistic transcation,
      final OAtomicOperation atomicOperation,
      final ORecordOperation txEntry,
      final OPhysicalPosition allocated,
      final ORecordSerializer serializer) {
    final RecordAbstract rec = txEntry.record;
    if (txEntry.type != ORecordOperation.DELETED && !rec.isDirty())
    // NO OPERATION
    {
      return;
    }
    final YTRecordId rid = (YTRecordId) rec.getIdentity();

    if (txEntry.type == ORecordOperation.UPDATED && rid.isNew())
    // OVERWRITE OPERATION AS CREATE
    {
      txEntry.type = ORecordOperation.CREATED;
    }

    ORecordSerializationContext.pushContext();
    try {
      final OCluster cluster = doGetAndCheckCluster(rid.getClusterId());

      if (cluster.getName().equals(OMetadataDefault.CLUSTER_INDEX_NAME)
          || cluster.getName().equals(OMetadataDefault.CLUSTER_MANUAL_INDEX_NAME))
      // AVOID TO COMMIT INDEX STUFF
      {
        return;
      }

      switch (txEntry.type) {
        case ORecordOperation.CREATED: {
          final byte[] stream;
          try {
            stream = serializer.toStream(transcation.getDatabase(), rec);
          } catch (RuntimeException e) {
            throw YTException.wrapException(
                new YTCommitSerializationException("Error During Record Serialization"), e);
          }
          if (allocated != null) {
            final OPhysicalPosition ppos;
            final byte recordType = ORecordInternal.getRecordType(rec);
            ppos =
                doCreateRecord(
                    atomicOperation,
                    rid,
                    stream,
                    rec.getVersion(),
                    recordType,
                    null,
                    cluster,
                    allocated)
                    .getResult();

            ORecordInternal.setVersion(rec, ppos.recordVersion);
          } else {
            final OStorageOperationResult<Integer> updateRes =
                doUpdateRecord(
                    atomicOperation,
                    rid,
                    ORecordInternal.isContentChanged(rec),
                    stream,
                    -2,
                    ORecordInternal.getRecordType(rec),
                    null,
                    cluster);
            ORecordInternal.setVersion(rec, updateRes.getResult());
            if (updateRes.getModifiedRecordContent() != null) {
              ORecordInternal.fill(
                  rec, rid, updateRes.getResult(), updateRes.getModifiedRecordContent(), false);
            }
          }
          break;
        }
        case ORecordOperation.UPDATED: {
          final byte[] stream;
          try {
            stream = serializer.toStream(transcation.getDatabase(), rec);
          } catch (RuntimeException e) {
            throw YTException.wrapException(
                new YTCommitSerializationException("Error During Record Serialization"), e);
          }

          final OStorageOperationResult<Integer> updateRes =
              doUpdateRecord(
                  atomicOperation,
                  rid,
                  ORecordInternal.isContentChanged(rec),
                  stream,
                  rec.getVersion(),
                  ORecordInternal.getRecordType(rec),
                  null,
                  cluster);
          ORecordInternal.setVersion(rec, updateRes.getResult());
          if (updateRes.getModifiedRecordContent() != null) {
            ORecordInternal.fill(
                rec, rid, updateRes.getResult(), updateRes.getModifiedRecordContent(), false);
          }

          break;
        }
        case ORecordOperation.DELETED: {
          if (rec instanceof EntityImpl doc) {
            doc.incrementLoading();
            try {
              ORidBagDeleter.deleteAllRidBags(doc);
            } finally {
              doc.decrementLoading();
            }
          }
          doDeleteRecord(atomicOperation, rid, rec.getVersionNoLoad(), cluster);
          break;
        }
        default:
          throw new YTStorageException("Unknown record operation " + txEntry.type);
      }
    } finally {
      ORecordSerializationContext.pullContext();
    }

    // RESET TRACKING
    if (rec instanceof EntityImpl && ((EntityImpl) rec).isTrackingChanges()) {
      ODocumentInternal.clearTrackData(((EntityImpl) rec));
      ODocumentInternal.clearTransactionTrackData(((EntityImpl) rec));
    }
    ORecordInternal.unsetDirty(rec);
  }

  private void checkClusterSegmentIndexRange(final int iClusterId) {
    if (iClusterId < 0 || iClusterId > clusters.size() - 1) {
      throw new IllegalArgumentException(
          "Cluster segment #" + iClusterId + " does not exist in database '" + name + "'");
    }
  }

  private void restoreFromWAL() throws IOException {
    final OLogSequenceNumber begin = writeAheadLog.begin();
    if (begin == null) {
      LogManager.instance()
          .error(this, "Restore is not possible because write ahead log is empty.", null);
      return;
    }

    LogManager.instance().info(this, "Looking for last checkpoint...");

    writeAheadLog.addCutTillLimit(begin);
    try {
      restoreFromBeginning();
    } finally {
      writeAheadLog.removeCutTillLimit(begin);
    }
  }

  @SuppressWarnings("CanBeFinal")
  @Override
  public String incrementalBackup(YTDatabaseSessionInternal session, final String backupDirectory,
      final OCallable<Void, Void> started)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "Incremental backup is supported only in enterprise version");
  }

  @Override
  public boolean supportIncremental() {
    return false;
  }

  @Override
  public void fullIncrementalBackup(final OutputStream stream)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "Incremental backup is supported only in enterprise version");
  }

  @SuppressWarnings("CanBeFinal")
  @Override
  public void restoreFromIncrementalBackup(YTDatabaseSessionInternal session,
      final String filePath) {
    throw new UnsupportedOperationException(
        "Incremental backup is supported only in enterprise version");
  }

  @Override
  public void restoreFullIncrementalBackup(YTDatabaseSessionInternal session,
      final InputStream stream)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "Incremental backup is supported only in enterprise version");
  }

  private void restoreFromBeginning() throws IOException {
    LogManager.instance().info(this, "Data restore procedure is started.");

    final OLogSequenceNumber lsn = writeAheadLog.begin();

    writeCache.restoreModeOn();
    try {
      restoreFrom(writeAheadLog, lsn);
    } finally {
      writeCache.restoreModeOff();
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  protected OLogSequenceNumber restoreFrom(OWriteAheadLog writeAheadLog, OLogSequenceNumber lsn)
      throws IOException {
    final OModifiableBoolean atLeastOnePageUpdate = new OModifiableBoolean();

    long recordsProcessed = 0;

    final int reportBatchSize =
        GlobalConfiguration.WAL_REPORT_AFTER_OPERATIONS_DURING_RESTORE.getValueAsInteger();
    final Long2ObjectOpenHashMap<List<OWALRecord>> operationUnits =
        new Long2ObjectOpenHashMap<>(1024);
    final Map<Long, byte[]> operationMetadata = new LinkedHashMap<>(1024);

    long lastReportTime = 0;
    OLogSequenceNumber lastUpdatedLSN = null;

    try {
      List<WriteableWALRecord> records = writeAheadLog.read(lsn, 1_000);

      while (!records.isEmpty()) {
        for (final WriteableWALRecord walRecord : records) {
          if (walRecord instanceof OAtomicUnitEndRecord atomicUnitEndRecord) {
            final List<OWALRecord> atomicUnit =
                operationUnits.remove(atomicUnitEndRecord.getOperationUnitId());

            // in case of data restore from fuzzy checkpoint part of operations may be already
            // flushed to the disk
            if (atomicUnit != null) {
              atomicUnit.add(walRecord);
              if (!restoreAtomicUnit(atomicUnit, atLeastOnePageUpdate)) {
                return lastUpdatedLSN;
              } else {
                lastUpdatedLSN = walRecord.getLsn();
              }
            }
            byte[] metadata = operationMetadata.remove(atomicUnitEndRecord.getOperationUnitId());
            if (metadata != null) {
              this.lastMetadata = metadata;
            }
          } else if (walRecord instanceof OAtomicUnitStartRecord oAtomicUnitStartRecord) {
            if (walRecord instanceof OAtomicUnitStartMetadataRecord) {
              byte[] metadata = ((OAtomicUnitStartMetadataRecord) walRecord).getMetadata();
              operationMetadata.put(
                  ((OAtomicUnitStartRecord) walRecord).getOperationUnitId(), metadata);
            }

            final List<OWALRecord> operationList = new ArrayList<>(1024);

            assert !operationUnits.containsKey(oAtomicUnitStartRecord.getOperationUnitId());

            operationUnits.put(oAtomicUnitStartRecord.getOperationUnitId(), operationList);
            operationList.add(walRecord);
          } else if (walRecord instanceof OOperationUnitRecord operationUnitRecord) {
            List<OWALRecord> operationList =
                operationUnits.computeIfAbsent(
                    operationUnitRecord.getOperationUnitId(), k -> new ArrayList<>(1024));
            operationList.add(operationUnitRecord);
          } else if (walRecord instanceof ONonTxOperationPerformedWALRecord ignored) {
            if (!wereNonTxOperationsPerformedInPreviousOpen) {
              LogManager.instance()
                  .warn(
                      this,
                      "Non tx operation was used during data modification we will need index"
                          + " rebuild.");
              wereNonTxOperationsPerformedInPreviousOpen = true;
            }
          } else if (walRecord instanceof MetaDataRecord metaDataRecord) {
            this.lastMetadata = metaDataRecord.getMetadata();
            lastUpdatedLSN = walRecord.getLsn();
          } else {
            LogManager.instance()
                .warn(this, "Record %s will be skipped during data restore", walRecord);
          }

          recordsProcessed++;

          final long currentTime = System.currentTimeMillis();
          if (reportBatchSize > 0 && recordsProcessed % reportBatchSize == 0
              || currentTime - lastReportTime > WAL_RESTORE_REPORT_INTERVAL) {
            final Object[] additionalArgs =
                new Object[]{recordsProcessed, walRecord.getLsn(), writeAheadLog.end()};
            LogManager.instance()
                .info(
                    this,
                    "%d operations were processed, current LSN is %s last LSN is %s",
                    additionalArgs);
            lastReportTime = currentTime;
          }
        }

        records = writeAheadLog.next(records.get(records.size() - 1).getLsn(), 1_000);
      }
    } catch (final OWALPageBrokenException e) {
      LogManager.instance()
          .error(
              this,
              "Data restore was paused because broken WAL page was found. The rest of changes will"
                  + " be rolled back.",
              e);
    } catch (final RuntimeException e) {
      LogManager.instance()
          .error(
              this,
              "Data restore was paused because of exception. The rest of changes will be rolled"
                  + " back.",
              e);
    }

    return lastUpdatedLSN;
  }

  protected final boolean restoreAtomicUnit(
      final List<OWALRecord> atomicUnit, final OModifiableBoolean atLeastOnePageUpdate)
      throws IOException {
    assert atomicUnit.get(atomicUnit.size() - 1) instanceof OAtomicUnitEndRecord;
    for (final OWALRecord walRecord : atomicUnit) {
      if (walRecord instanceof OFileDeletedWALRecord fileDeletedWALRecord) {
        if (writeCache.exists(fileDeletedWALRecord.getFileId())) {
          readCache.deleteFile(fileDeletedWALRecord.getFileId(), writeCache);
        }
      } else if (walRecord instanceof OFileCreatedWALRecord fileCreatedCreatedWALRecord) {
        if (!writeCache.exists(fileCreatedCreatedWALRecord.getFileName())) {
          readCache.addFile(
              fileCreatedCreatedWALRecord.getFileName(),
              fileCreatedCreatedWALRecord.getFileId(),
              writeCache);
        }
      } else if (walRecord instanceof OUpdatePageRecord updatePageRecord) {
        long fileId = updatePageRecord.getFileId();
        if (!writeCache.exists(fileId)) {
          final String fileName = writeCache.restoreFileById(fileId);

          if (fileName == null) {
            throw new YTStorageException(
                "File with id "
                    + fileId
                    + " was deleted from storage, the rest of operations can not be restored");
          } else {
            LogManager.instance()
                .warn(
                    this,
                    "Previously deleted file with name "
                        + fileName
                        + " was deleted but new empty file was added to continue restore process");
          }
        }

        final long pageIndex = updatePageRecord.getPageIndex();
        fileId = writeCache.externalFileId(writeCache.internalFileId(fileId));

        OCacheEntry cacheEntry = readCache.loadForWrite(fileId, pageIndex, writeCache, true, null);
        if (cacheEntry == null) {
          do {
            if (cacheEntry != null) {
              readCache.releaseFromWrite(cacheEntry, writeCache, true);
            }

            cacheEntry = readCache.allocateNewPage(fileId, writeCache, null);
          } while (cacheEntry.getPageIndex() != pageIndex);
        }

        try {
          final ODurablePage durablePage = new ODurablePage(cacheEntry);
          var pageLsn = durablePage.getLsn();
          if (durablePage.getLsn().compareTo(walRecord.getLsn()) < 0) {
            if (!pageLsn.equals(updatePageRecord.getInitialLsn())) {
              LogManager.instance()
                  .error(
                      this,
                      "Page with index "
                          + pageIndex
                          + " and file "
                          + writeCache.fileNameById(fileId)
                          + " was changed before page restore was started. Page will be restored"
                          + " from WAL, but it may contain changes that were not present before"
                          + " storage crash and data may be lost. Initial LSN is "
                          + updatePageRecord.getInitialLsn()
                          + ", but page contains changes with LSN "
                          + pageLsn,
                      null);
            }
            durablePage.restoreChanges(updatePageRecord.getChanges());
            durablePage.setLsn(updatePageRecord.getLsn());
          }
        } finally {
          readCache.releaseFromWrite(cacheEntry, writeCache, true);
        }

        atLeastOnePageUpdate.setValue(true);
      } else if (walRecord instanceof OAtomicUnitStartRecord) {
        //noinspection UnnecessaryContinue
        continue;
      } else if (walRecord instanceof OAtomicUnitEndRecord) {
        //noinspection UnnecessaryContinue
        continue;
      } else if (walRecord instanceof OHighLevelTransactionChangeRecord) {
        //noinspection UnnecessaryContinue
        continue;
      } else {
        assert walRecord != null;
        LogManager.instance()
            .error(
                this,
                "Invalid WAL record type was passed %s. Given record will be skipped.",
                null,
                walRecord.getClass());

        assert false : "Invalid WAL record type was passed " + walRecord.getClass().getName();
      }
    }
    return true;
  }

  @SuppressWarnings("unused")
  public void setStorageConfigurationUpdateListener(
      final OStorageConfigurationUpdateListener storageConfigurationUpdateListener) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      ((OClusterBasedStorageConfiguration) configuration)
          .setConfigurationUpdateListener(storageConfigurationUpdateListener);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  public void pauseConfigurationUpdateNotifications() {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      ((OClusterBasedStorageConfiguration) configuration).pauseUpdateNotifications();
    } finally {
      stateLock.readLock().unlock();
    }
  }

  public void fireConfigurationUpdateNotifications() {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();
      ((OClusterBasedStorageConfiguration) configuration).fireUpdateNotifications();
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @SuppressWarnings("unused")
  protected static Int2ObjectMap<List<YTRecordId>> getRidsGroupedByCluster(
      final Collection<YTRecordId> rids) {
    final Int2ObjectOpenHashMap<List<YTRecordId>> ridsPerCluster = new Int2ObjectOpenHashMap<>(8);
    for (final YTRecordId rid : rids) {
      final List<YTRecordId> group =
          ridsPerCluster.computeIfAbsent(rid.getClusterId(), k -> new ArrayList<>(rids.size()));
      group.add(rid);
    }
    return ridsPerCluster;
  }

  private static void lockIndexes(final TreeMap<String, OTransactionIndexChanges> indexes) {
    for (final OTransactionIndexChanges changes : indexes.values()) {
      assert changes.changesPerKey instanceof TreeMap;

      final OIndexInternal index = changes.getAssociatedIndex();

      final List<Object> orderedIndexNames = new ArrayList<>(changes.changesPerKey.keySet());
      if (orderedIndexNames.size() > 1) {
        orderedIndexNames.sort(
            (o1, o2) -> {
              final String i1 = index.getIndexNameByKey(o1);
              final String i2 = index.getIndexNameByKey(o2);
              return i1.compareTo(i2);
            });
      }

      boolean fullyLocked = false;
      for (final Object key : orderedIndexNames) {
        if (index.acquireAtomicExclusiveLock(key)) {
          fullyLocked = true;
          break;
        }
      }
      if (!fullyLocked && !changes.nullKeyChanges.isEmpty()) {
        index.acquireAtomicExclusiveLock(null);
      }
    }
  }

  private static void lockClusters(final TreeMap<Integer, OCluster> clustersToLock) {
    for (final OCluster cluster : clustersToLock.values()) {
      cluster.acquireAtomicExclusiveLock();
    }
  }

  private void lockRidBags(
      final TreeMap<Integer, OCluster> clusters,
      final TreeMap<String, OTransactionIndexChanges> indexes,
      final OIndexManagerAbstract manager,
      YTDatabaseSessionInternal db) {
    final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();

    for (final Integer clusterId : clusters.keySet()) {
      atomicOperationsManager.acquireExclusiveLockTillOperationComplete(
          atomicOperation, OSBTreeCollectionManagerShared.generateLockName(clusterId));
    }

    for (final Entry<String, OTransactionIndexChanges> entry : indexes.entrySet()) {
      final String indexName = entry.getKey();
      final OIndexInternal index = entry.getValue().resolveAssociatedIndex(indexName, manager, db);
      if (index != null) {
        try {
          OBaseIndexEngine engine = getIndexEngine(index.getIndexId());

          if (!index.isUnique() && engine.hasRidBagTreesSupport()) {
            atomicOperationsManager.acquireExclusiveLockTillOperationComplete(
                atomicOperation, OIndexRIDContainerSBTree.generateLockName(indexName));
          }
        } catch (OInvalidIndexEngineIdException e) {
          throw logAndPrepareForRethrow(e, false);
        }
      }
    }
  }

  private void registerProfilerHooks() {
    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".createRecord",
            "Number of created records",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(recordCreated),
            "db.*.createRecord");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".readRecord",
            "Number of read records",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(recordRead),
            "db.*.readRecord");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".updateRecord",
            "Number of updated records",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(recordUpdated),
            "db.*.updateRecord");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".deleteRecord",
            "Number of deleted records",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(recordDeleted),
            "db.*.deleteRecord");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".scanRecord",
            "Number of read scanned",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(recordScanned),
            "db.*.scanRecord");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".recyclePosition",
            "Number of recycled records",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(recordRecycled),
            "db.*.recyclePosition");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".conflictRecord",
            "Number of conflicts during updating and deleting records",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(recordConflict),
            "db.*.conflictRecord");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".txBegun",
            "Number of transactions begun",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(txBegun),
            "db.*.txBegun");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".txCommit",
            "Number of committed transactions",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(txCommit),
            "db.*.txCommit");

    YouTrackDBManager.instance()
        .getProfiler()
        .registerHookValue(
            "db." + this.name + ".txRollback",
            "Number of rolled back transactions",
            METRIC_TYPE.COUNTER,
            new ModifiableLongProfileHookValue(txRollback),
            "db.*.txRollback");
  }

  protected RuntimeException logAndPrepareForRethrow(final RuntimeException runtimeException) {
    if (!(runtimeException instanceof YTHighLevelException
        || runtimeException instanceof YTNeedRetryException
        || runtimeException instanceof YTInternalErrorException
        || runtimeException instanceof IllegalArgumentException)) {
      final Object[] iAdditionalArgs =
          new Object[]{
              System.identityHashCode(runtimeException), getURL(), OConstants.getVersion()
          };
      LogManager.instance()
          .error(this, "Exception `%08X` in storage `%s`: %s", runtimeException, iAdditionalArgs);
    }

    return runtimeException;
  }

  protected final Error logAndPrepareForRethrow(final Error error) {
    return logAndPrepareForRethrow(error, true);
  }

  protected Error logAndPrepareForRethrow(final Error error, final boolean putInReadOnlyMode) {
    if (!(error instanceof YTHighLevelException)) {
      if (putInReadOnlyMode) {
        setInError(error);
      }

      final Object[] iAdditionalArgs =
          new Object[]{System.identityHashCode(error), getURL(), OConstants.getVersion()};
      LogManager.instance()
          .error(this, "Exception `%08X` in storage `%s`: %s", error, iAdditionalArgs);
    }

    return error;
  }

  protected final RuntimeException logAndPrepareForRethrow(final Throwable throwable) {
    return logAndPrepareForRethrow(throwable, true);
  }

  protected RuntimeException logAndPrepareForRethrow(
      final Throwable throwable, final boolean putInReadOnlyMode) {
    if (!(throwable instanceof YTHighLevelException
        || throwable instanceof YTNeedRetryException
        || throwable instanceof YTInternalErrorException)) {
      if (putInReadOnlyMode) {
        setInError(throwable);
      }
      final Object[] iAdditionalArgs =
          new Object[]{System.identityHashCode(throwable), getURL(), OConstants.getVersion()};
      LogManager.instance()
          .error(this, "Exception `%08X` in storage `%s`: %s", throwable, iAdditionalArgs);
    }
    return new RuntimeException(throwable);
  }

  private OInvalidIndexEngineIdException logAndPrepareForRethrow(
      final OInvalidIndexEngineIdException exception) {
    final Object[] iAdditionalArgs =
        new Object[]{System.identityHashCode(exception), getURL(), OConstants.getVersion()};
    LogManager.instance()
        .error(this, "Exception `%08X` in storage `%s` : %s", exception, iAdditionalArgs);
    return exception;
  }

  @Override
  public final OStorageConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public final void setSchemaRecordId(final String schemaRecordId) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation ->
              storageConfiguration.setSchemaRecordId(atomicOperation, schemaRecordId));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setDateFormat(final String dateFormat) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null, atomicOperation -> storageConfiguration.setDateFormat(atomicOperation, dateFormat));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setTimeZone(final TimeZone timeZoneValue) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation -> storageConfiguration.setTimeZone(atomicOperation, timeZoneValue));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setLocaleLanguage(final String locale) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null, atomicOperation -> storageConfiguration.setLocaleLanguage(atomicOperation, locale));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setCharset(final String charset) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null, atomicOperation -> storageConfiguration.setCharset(atomicOperation, charset));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setIndexMgrRecordId(final String indexMgrRecordId) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation ->
              storageConfiguration.setIndexMgrRecordId(atomicOperation, indexMgrRecordId));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setDateTimeFormat(final String dateTimeFormat) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation ->
              storageConfiguration.setDateTimeFormat(atomicOperation, dateTimeFormat));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setLocaleCountry(final String localeCountry) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation -> storageConfiguration.setLocaleCountry(atomicOperation, localeCountry));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setClusterSelection(final String clusterSelection) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation ->
              storageConfiguration.setClusterSelection(atomicOperation, clusterSelection));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setMinimumClusters(final int minimumClusters) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      storageConfiguration.setMinimumClusters(minimumClusters);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setValidation(final boolean validation) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null, atomicOperation -> storageConfiguration.setValidation(atomicOperation, validation));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void removeProperty(final String property) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null, atomicOperation -> storageConfiguration.removeProperty(atomicOperation, property));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setProperty(final String property, final String value) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation -> storageConfiguration.setProperty(atomicOperation, property, value));
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void setRecordSerializer(final String recordSerializer, final int version) {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null,
          atomicOperation -> {
            storageConfiguration.setRecordSerializer(atomicOperation, recordSerializer);
            storageConfiguration.setRecordSerializerVersion(atomicOperation, version);
          });
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public final void clearProperties() {
    stateLock.readLock().lock();
    try {

      checkOpennessAndMigration();

      final OClusterBasedStorageConfiguration storageConfiguration =
          (OClusterBasedStorageConfiguration) configuration;

      makeStorageDirty();

      atomicOperationsManager.executeInsideAtomicOperation(
          null, storageConfiguration::clearProperties);
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  public Optional<byte[]> getLastMetadata() {
    return Optional.ofNullable(lastMetadata);
  }

  void runWALVacuum() {
    stateLock.readLock().lock();
    try {

      if (status == STATUS.CLOSED) {
        return;
      }

      final long[] nonActiveSegments = writeAheadLog.nonActiveSegments();
      if (nonActiveSegments.length == 0) {
        return;
      }

      long flushTillSegmentId;
      if (nonActiveSegments.length == 1) {
        flushTillSegmentId = writeAheadLog.activeSegment();
      } else {
        flushTillSegmentId =
            (nonActiveSegments[0] + nonActiveSegments[nonActiveSegments.length - 1]) / 2;
      }

      long minDirtySegment;
      do {
        writeCache.flushTillSegment(flushTillSegmentId);

        // we should take active segment BEFORE min write cache LSN call
        // to avoid case when new data are changed before call
        final long activeSegment = writeAheadLog.activeSegment();
        final Long minLSNSegment = writeCache.getMinimalNotFlushedSegment();

        minDirtySegment = Objects.requireNonNullElse(minLSNSegment, activeSegment);
      } while (minDirtySegment < flushTillSegmentId);

      atomicOperationsTable.compactTable();
      final long operationSegment = atomicOperationsTable.getSegmentEarliestNotPersistedOperation();
      if (operationSegment >= 0 && minDirtySegment > operationSegment) {
        minDirtySegment = operationSegment;
      }

      if (minDirtySegment <= nonActiveSegments[0]) {
        return;
      }

      writeCache.syncDataFiles(minDirtySegment, lastMetadata);
    } catch (final Exception e) {
      LogManager.instance()
          .error(
              this, "Error during flushing of data for fuzzy checkpoint, in storage %s", e, name);
    } finally {
      stateLock.readLock().unlock();
      walVacuumInProgress.set(false);
    }
  }

  @SuppressWarnings("unused")
  public int getVersionForKey(final String indexName, final Object key) {
    assert isIndexUniqueByName(indexName);
    if (!isDistributedMode(lastMetadata)) {
      return 0;
    }
    final OBaseIndexEngine indexEngine = indexEngineNameMap.get(indexName);
    return indexEngine.getUniqueIndexVersion(key);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isDistributedMode(final byte[] metadata) {
    return metadata == null;
  }

  private boolean isIndexUniqueByName(final String indexName) {
    final IndexEngineData engineData = configuration.getIndexEngine(indexName, 0);
    return isIndexUniqueByType(engineData.getIndexType());
  }

  private static boolean isIndexUniqueByType(final String indexType) {
    //noinspection deprecation
    return indexType.equals(INDEX_TYPE.UNIQUE.name())
        || indexType.equals(INDEX_TYPE.UNIQUE_HASH_INDEX.name())
        || indexType.equals(INDEX_TYPE.DICTIONARY.name())
        || indexType.equals(INDEX_TYPE.DICTIONARY_HASH_INDEX.name());
  }

  private void applyUniqueIndexChange(final String indexName, final Object key) {
    if (!isDistributedMode(lastMetadata)) {
      final OBaseIndexEngine indexEngine = indexEngineNameMap.get(indexName);
      indexEngine.updateUniqueIndexVersion(key);
    }
  }

  @Override
  public int[] getClustersIds(Set<String> filterClusters) {
    try {

      stateLock.readLock().lock();
      try {

        checkOpennessAndMigration();
        int[] result = new int[filterClusters.size()];
        int i = 0;
        for (String clusterName : filterClusters) {
          if (clusterName == null) {
            throw new IllegalArgumentException("Cluster name is null");
          }

          if (clusterName.isEmpty()) {
            throw new IllegalArgumentException("Cluster name is empty");
          }

          // SEARCH IT BETWEEN PHYSICAL CLUSTERS
          final OCluster segment = clusterMap.get(clusterName.toLowerCase());
          if (segment != null) {
            result[i] = segment.getId();
          } else {
            result[i] = -1;
          }
          i++;
        }
        return result;
      } finally {
        stateLock.readLock().unlock();
      }
    } catch (final RuntimeException ee) {
      throw logAndPrepareForRethrow(ee);
    } catch (final Error ee) {
      throw logAndPrepareForRethrow(ee, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  public Optional<OBackgroundNewDelta> extractTransactionsFromWal(
      List<OTransactionId> transactionsMetadata) {
    Map<OTransactionId, OTransactionData> finished = new HashMap<>();
    List<OTransactionId> started = new ArrayList<>();
    stateLock.readLock().lock();
    try {
      Set<OTransactionId> transactionsToRead = new HashSet<>(transactionsMetadata);
      // we iterate till the last record is contained in wal at the moment when we call this method
      OLogSequenceNumber beginLsn = writeAheadLog.begin();
      Long2ObjectOpenHashMap<OTransactionData> units = new Long2ObjectOpenHashMap<>();

      writeAheadLog.addCutTillLimit(beginLsn);
      try {
        List<WriteableWALRecord> records = writeAheadLog.next(beginLsn, 1_000);
        // all information about changed records is contained in atomic operation metadata
        while (!records.isEmpty()) {
          for (final OWALRecord record : records) {

            if (record instanceof OFileCreatedWALRecord) {
              return Optional.empty();
            }

            if (record instanceof OFileDeletedWALRecord) {
              return Optional.empty();
            }

            if (record instanceof OAtomicUnitStartMetadataRecord) {
              byte[] meta = ((OAtomicUnitStartMetadataRecord) record).getMetadata();
              OTxMetadataHolder data = OTxMetadataHolderImpl.read(meta);
              // This will not be a byte to byte compare, but should compare only the tx id not all
              // status
              //noinspection ConstantConditions
              OTransactionId txId =
                  new OTransactionId(
                      Optional.empty(), data.getId().getPosition(), data.getId().getSequence());
              if (transactionsToRead.contains(txId)) {
                long unitId = ((OAtomicUnitStartMetadataRecord) record).getOperationUnitId();
                units.put(unitId, new OTransactionData(txId));
                started.add(txId);
              }
            }
            if (record instanceof OAtomicUnitEndRecord) {
              long opId = ((OAtomicUnitEndRecord) record).getOperationUnitId();
              OTransactionData opes = units.remove(opId);
              if (opes != null) {
                transactionsToRead.remove(opes.getTransactionId());
                finished.put(opes.getTransactionId(), opes);
              }
            }
            if (record instanceof OHighLevelTransactionChangeRecord) {
              byte[] data = ((OHighLevelTransactionChangeRecord) record).getData();
              long unitId = ((OHighLevelTransactionChangeRecord) record).getOperationUnitId();
              OTransactionData tx = units.get(unitId);
              if (tx != null) {
                tx.addRecord(data);
              }
            }
            if (transactionsToRead.isEmpty() && units.isEmpty()) {
              // all read stop scanning and return the transactions
              List<OTransactionData> transactions = new ArrayList<>();
              for (OTransactionId id : started) {
                OTransactionData data = finished.get(id);
                if (data != null) {
                  transactions.add(data);
                }
              }
              return Optional.of(new OBackgroundNewDelta(transactions));
            }
          }
          records = writeAheadLog.next(records.get(records.size() - 1).getLsn(), 1_000);
        }
      } finally {
        writeAheadLog.removeCutTillLimit(beginLsn);
      }
      if (transactionsToRead.isEmpty()) {
        List<OTransactionData> transactions = new ArrayList<>();
        for (OTransactionId id : started) {
          OTransactionData data = finished.get(id);
          if (data != null) {
            transactions.add(data);
          }
        }
        return Optional.of(new OBackgroundNewDelta(transactions));
      } else {
        return Optional.empty();
      }
    } catch (final IOException e) {
      throw YTException.wrapException(
          new YTStorageException("Error of reading of records from  WAL"), e);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  public void startDDL() {
    backupLock.lock();
    try {
      waitBackup();
      //noinspection NonAtomicOperationOnVolatileField
      this.ddlRunning += 1;
    } finally {
      backupLock.unlock();
    }
  }

  public void endDDL() {
    backupLock.lock();
    try {
      assert this.ddlRunning > 0;
      //noinspection NonAtomicOperationOnVolatileField
      this.ddlRunning -= 1;

      if (this.ddlRunning == 0) {
        backupIsDone.signalAll();
      }
    } finally {
      backupLock.unlock();
    }
  }

  private void waitBackup() {
    while (isIcrementalBackupRunning()) {
      try {
        backupIsDone.await();
      } catch (InterruptedException e) {
        throw YTException.wrapException(
            new YTInterruptedException("Interrupted wait for backup to finish"), e);
      }
    }
  }

  protected void checkBackupRunning() {
    waitBackup();
  }

  @Override
  public YouTrackDBInternal getContext() {
    return this.context;
  }

  public boolean isMemory() {
    return false;
  }

  @SuppressWarnings("unused")
  protected void endBackup() {
    backupLock.lock();
    try {
      assert this.backupRunning > 0;
      //noinspection NonAtomicOperationOnVolatileField
      this.backupRunning -= 1;

      if (this.backupRunning == 0) {
        backupIsDone.signalAll();
      }
    } finally {
      backupLock.unlock();
    }
  }

  public boolean isIcrementalBackupRunning() {
    return this.backupRunning > 0;
  }

  protected boolean isDDLRunning() {
    return this.ddlRunning > 0;
  }

  @SuppressWarnings("unused")
  protected void startBackup() {
    backupLock.lock();
    try {
      while (isDDLRunning()) {
        try {
          backupIsDone.await();
        } catch (InterruptedException e) {
          throw YTException.wrapException(
              new YTInterruptedException("Interrupted wait for backup to finish"), e);
        }
      }
      //noinspection NonAtomicOperationOnVolatileField
      this.backupRunning += 1;
    } finally {
      backupLock.unlock();
    }
  }
}