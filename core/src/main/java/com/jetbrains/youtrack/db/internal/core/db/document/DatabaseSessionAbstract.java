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

package com.jetbrains.youtrack.db.internal.core.db.document;

import com.jetbrains.youtrack.db.internal.common.concur.NeedRetryException;
import com.jetbrains.youtrack.db.internal.common.exception.BaseException;
import com.jetbrains.youtrack.db.internal.common.exception.HighLevelException;
import com.jetbrains.youtrack.db.internal.common.listener.ListenerManger;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBManager;
import com.jetbrains.youtrack.db.internal.core.cache.LocalRecordCache;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequestInternal;
import com.jetbrains.youtrack.db.internal.core.config.ContextConfiguration;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseLifecycleListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.ScenarioThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.SharedContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseListener;
import com.jetbrains.youtrack.db.internal.core.db.record.CurrentStorageComponentsFactory;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.dictionary.Dictionary;
import com.jetbrains.youtrack.db.internal.core.exception.ConcurrentModificationException;
import com.jetbrains.youtrack.db.internal.core.exception.DatabaseException;
import com.jetbrains.youtrack.db.internal.core.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.internal.core.exception.SecurityException;
import com.jetbrains.youtrack.db.internal.core.exception.SessionNotActivatedException;
import com.jetbrains.youtrack.db.internal.core.exception.TransactionBlockedException;
import com.jetbrains.youtrack.db.internal.core.exception.ValidationException;
import com.jetbrains.youtrack.db.internal.core.exception.SchemaException;
import com.jetbrains.youtrack.db.internal.core.exception.TransactionException;
import com.jetbrains.youtrack.db.internal.core.hook.RecordHook;
import com.jetbrains.youtrack.db.internal.core.id.RID;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.iterator.RecordIteratorClass;
import com.jetbrains.youtrack.db.internal.core.iterator.RecordIteratorCluster;
import com.jetbrains.youtrack.db.internal.core.metadata.Metadata;
import com.jetbrains.youtrack.db.internal.core.metadata.MetadataDefault;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.Schema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaImmutableClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaImmutableView;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaView;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityShared;
import com.jetbrains.youtrack.db.internal.core.metadata.security.ImmutableUser;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityUser;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityUserIml;
import com.jetbrains.youtrack.db.internal.core.query.Query;
import com.jetbrains.youtrack.db.internal.core.record.Direction;
import com.jetbrains.youtrack.db.internal.core.record.Edge;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.record.impl.EdgeInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.DocumentInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.Blob;
import com.jetbrains.youtrack.db.internal.core.record.impl.RecordBytes;
import com.jetbrains.youtrack.db.internal.core.record.impl.VertexInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EdgeDelegate;
import com.jetbrains.youtrack.db.internal.core.record.impl.EdgeEntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.VertexEntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.BinarySerializerFactory;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializerFactory;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultSet;
import com.jetbrains.youtrack.db.internal.core.storage.RawBuffer;
import com.jetbrains.youtrack.db.internal.core.storage.StorageInfo;
import com.jetbrains.youtrack.db.internal.core.storage.StorageOperationResult;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.OfflineClusterException;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.BonsaiCollectionPointer;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransaction;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransaction.TXSTATUS;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionAbstract;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionNoTx;
import com.jetbrains.youtrack.db.internal.core.tx.TransactionOptimistic;
import com.jetbrains.youtrack.db.internal.core.tx.RollbackException;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * Document API entrypoint.
 */
@SuppressWarnings("unchecked")
public abstract class DatabaseSessionAbstract extends ListenerManger<DatabaseListener>
    implements DatabaseSessionInternal {

  protected final Map<String, Object> properties = new HashMap<String, Object>();
  protected Map<RecordHook, RecordHook.HOOK_POSITION> unmodifiableHooks;
  protected final Set<Identifiable> inHook = new HashSet<Identifiable>();
  protected RecordSerializer serializer;
  protected String url;
  protected STATUS status;
  protected DatabaseSessionInternal databaseOwner;
  protected MetadataDefault metadata;
  protected ImmutableUser user;
  protected static final byte recordType = EntityImpl.RECORD_TYPE;
  protected final Map<RecordHook, RecordHook.HOOK_POSITION> hooks = new LinkedHashMap<>();
  protected boolean retainRecords = true;
  protected LocalRecordCache localCache;
  protected CurrentStorageComponentsFactory componentsFactory;
  protected boolean initialized = false;
  protected FrontendTransactionAbstract currentTx;

  protected final RecordHook[][] hooksByScope =
      new RecordHook[RecordHook.SCOPE.values().length][];
  protected SharedContext sharedContext;

  private boolean prefetchRecords;

  protected Map<String, QueryDatabaseState> activeQueries = new ConcurrentHashMap<>();
  protected LinkedList<QueryDatabaseState> queryState = new LinkedList<>();
  private Map<UUID, BonsaiCollectionPointer> collectionsChanges;

  // database stats!
  protected long loadedRecordsCount;
  protected long totalRecordLoadMs;
  protected long minRecordLoadMs;
  protected long maxRecordLoadMs;
  protected long ridbagPrefetchCount;
  protected long totalRidbagPrefetchMs;
  protected long minRidbagPrefetchMs;
  protected long maxRidbagPrefetchMs;

  protected DatabaseSessionAbstract() {
    // DO NOTHING IS FOR EXTENDED OBJECTS
    super(false);
  }

  /**
   * @return default serializer which is used to serialize documents. Default serializer is common
   * for all database instances.
   */
  public static RecordSerializer getDefaultSerializer() {
    return RecordSerializerFactory.instance().getDefaultRecordSerializer();
  }

  /**
   * Sets default serializer. The default serializer is common for all database instances.
   *
   * @param iDefaultSerializer new default serializer value
   */
  public static void setDefaultSerializer(RecordSerializer iDefaultSerializer) {
    RecordSerializerFactory.instance().setDefaultRecordSerializer(iDefaultSerializer);
  }

  public void callOnOpenListeners() {
    wakeupOnOpenDbLifecycleListeners();
    wakeupOnOpenListeners();
  }

  protected abstract void loadMetadata();

  public void callOnCloseListeners() {
    wakeupOnCloseDbLifecycleListeners();
    wakeupOnCloseListeners();
  }

  private void wakeupOnOpenDbLifecycleListeners() {
    for (Iterator<DatabaseLifecycleListener> it = YouTrackDBManager.instance()
        .getDbLifecycleListeners();
        it.hasNext(); ) {
      it.next().onOpen(getDatabaseOwner());
    }
  }

  private void wakeupOnOpenListeners() {
    for (DatabaseListener listener : getListenersCopy()) {
      try {
        //noinspection deprecation
        listener.onOpen(getDatabaseOwner());
      } catch (Exception e) {
        LogManager.instance().error(this, "Error during call of database listener", e);
      }
    }
  }

  private void wakeupOnCloseDbLifecycleListeners() {
    for (Iterator<DatabaseLifecycleListener> it = YouTrackDBManager.instance()
        .getDbLifecycleListeners();
        it.hasNext(); ) {
      it.next().onClose(getDatabaseOwner());
    }
  }

  private void wakeupOnCloseListeners() {
    for (DatabaseListener listener : getListenersCopy()) {
      try {
        listener.onClose(getDatabaseOwner());
      } catch (Exception e) {
        LogManager.instance().error(this, "Error during call of database listener", e);
      }
    }
  }

  public void callOnDropListeners() {
    wakeupOnDropListeners();
  }

  private void wakeupOnDropListeners() {
    for (DatabaseListener listener : getListenersCopy()) {
      try {
        activateOnCurrentThread();
        //noinspection deprecation
        listener.onDelete(getDatabaseOwner());
      } catch (Exception e) {
        LogManager.instance().error(this, "Error during call of database listener", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public <RET extends Record> RET getRecord(final Identifiable iIdentifiable) {
    if (iIdentifiable instanceof Record) {
      return (RET) iIdentifiable;
    }
    return load(iIdentifiable.getIdentity());
  }

  /**
   * Deletes the record checking the version.
   */
  private void delete(final RID iRecord, final int iVersion) {
    final Record record = load(iRecord);
    RecordInternal.setVersion(record, iVersion);
    delete(record);
  }

  public DatabaseSessionInternal cleanOutRecord(final RID iRecord, final int iVersion) {
    delete(iRecord, iVersion);
    return this;
  }

  public String getType() {
    return TYPE;
  }

  public <REC extends Record> RecordIteratorCluster<REC> browseCluster(
      final String iClusterName, final Class<REC> iClass) {
    return (RecordIteratorCluster<REC>) browseCluster(iClusterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public <REC extends Record> RecordIteratorCluster<REC> browseCluster(
      final String iClusterName,
      final Class<REC> iRecordClass,
      final long startClusterPosition,
      final long endClusterPosition,
      final boolean loadTombstones) {
    checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_READ, iClusterName);
    checkIfActive();
    final int clusterId = getClusterIdByName(iClusterName);
    return new RecordIteratorCluster<REC>(
        this, clusterId, startClusterPosition, endClusterPosition);
  }

  @Override
  public <REC extends Record> RecordIteratorCluster<REC> browseCluster(
      String iClusterName,
      Class<REC> iRecordClass,
      long startClusterPosition,
      long endClusterPosition) {
    checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_READ, iClusterName);
    checkIfActive();
    final int clusterId = getClusterIdByName(iClusterName);
    //noinspection deprecation
    return new RecordIteratorCluster<>(this, clusterId, startClusterPosition, endClusterPosition);
  }

  /**
   * {@inheritDoc}
   */
  public CommandRequest command(final CommandRequest iCommand) {
    checkSecurity(Rule.ResourceGeneric.COMMAND, Role.PERMISSION_READ);
    checkIfActive();
    final CommandRequestInternal command = (CommandRequestInternal) iCommand;
    try {
      command.reset();
      return command;
    } catch (Exception e) {
      throw BaseException.wrapException(new DatabaseException("Error on command execution"), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public <RET extends List<?>> RET query(final Query<?> iCommand, final Object... iArgs) {
    checkIfActive();
    iCommand.reset();
    return iCommand.execute(this, iArgs);
  }

  /**
   * {@inheritDoc}
   */
  public byte getRecordType() {
    return recordType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long countClusterElements(final int[] iClusterIds) {
    return countClusterElements(iClusterIds, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long countClusterElements(final int iClusterId) {
    return countClusterElements(iClusterId, false);
  }

  /**
   * {@inheritDoc}
   */
  public MetadataDefault getMetadata() {
    checkOpenness();
    return metadata;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseSessionInternal getDatabaseOwner() {
    DatabaseSessionInternal current = databaseOwner;
    while (current != null && current != this && current.getDatabaseOwner() != current) {
      current = current.getDatabaseOwner();
    }
    return current;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseSessionInternal setDatabaseOwner(DatabaseSessionInternal iOwner) {
    databaseOwner = iOwner;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isRetainRecords() {
    return retainRecords;
  }

  /**
   * {@inheritDoc}
   */
  public DatabaseSession setRetainRecords(boolean retainRecords) {
    this.retainRecords = retainRecords;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public DatabaseSession setStatus(final STATUS status) {
    checkIfActive();
    this.status = status;
    return this;
  }

  public void setStatusInternal(final STATUS status) {
    this.status = status;
  }

  /**
   * {@inheritDoc}
   */
  public void setInternal(final ATTRIBUTES iAttribute, final Object iValue) {
    set(iAttribute, iValue);
  }

  /**
   * {@inheritDoc}
   */
  public SecurityUser getUser() {
    return user;
  }

  /**
   * {@inheritDoc}
   */
  public void setUser(final SecurityUser user) {
    checkIfActive();
    if (user instanceof SecurityUserIml) {
      final Metadata metadata = getMetadata();
      if (metadata != null) {
        final SecurityInternal security = sharedContext.getSecurity();
        this.user = new ImmutableUser(this, security.getVersion(this), user);
      } else {
        this.user = new ImmutableUser(this, -1, user);
      }
    } else {
      this.user = (ImmutableUser) user;
    }
  }

  public void reloadUser() {
    if (user != null) {
      activateOnCurrentThread();
      if (user.checkIfAllowed(this, Rule.ResourceGeneric.CLASS, SecurityUserIml.CLASS_NAME,
          Role.PERMISSION_READ)
          != null) {
        Metadata metadata = getMetadata();
        if (metadata != null) {
          final SecurityInternal security = sharedContext.getSecurity();
          final SecurityUserIml secGetUser = security.getUser(this, user.getName(this));

          if (secGetUser != null) {
            user = new ImmutableUser(this, security.getVersion(this), secGetUser);
          } else {
            user = new ImmutableUser(this, -1, new SecurityUserIml());
          }
        } else {
          user = new ImmutableUser(this, -1, new SecurityUserIml());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isMVCC() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public DatabaseSession setMVCC(boolean mvcc) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Dictionary<Record> getDictionary() {
    checkOpenness();
    return metadata.getIndexManagerInternal().getDictionary(this);
  }

  /**
   * {@inheritDoc}
   */
  public void registerHook(final RecordHook iHookImpl,
      final RecordHook.HOOK_POSITION iPosition) {
    checkOpenness();
    checkIfActive();

    final Map<RecordHook, RecordHook.HOOK_POSITION> tmp =
        new LinkedHashMap<RecordHook, RecordHook.HOOK_POSITION>(hooks);
    tmp.put(iHookImpl, iPosition);
    hooks.clear();
    for (RecordHook.HOOK_POSITION p : RecordHook.HOOK_POSITION.values()) {
      for (Map.Entry<RecordHook, RecordHook.HOOK_POSITION> e : tmp.entrySet()) {
        if (e.getValue() == p) {
          hooks.put(e.getKey(), e.getValue());
        }
      }
    }
    compileHooks();
  }

  /**
   * {@inheritDoc}
   */
  public void registerHook(final RecordHook iHookImpl) {
    registerHook(iHookImpl, RecordHook.HOOK_POSITION.REGULAR);
  }

  /**
   * {@inheritDoc}
   */
  public void unregisterHook(final RecordHook iHookImpl) {
    checkIfActive();
    if (iHookImpl != null) {
      iHookImpl.onUnregister();
      hooks.remove(iHookImpl);
      compileHooks();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalRecordCache getLocalCache() {
    return localCache;
  }

  /**
   * {@inheritDoc}
   */
  public Map<RecordHook, RecordHook.HOOK_POSITION> getHooks() {
    return unmodifiableHooks;
  }

  /**
   * Callback the registered hooks if any.
   *
   * @param type Hook type. Define when hook is called.
   * @param id   Record received in the callback
   * @return True if the input record is changed, otherwise false
   */
  public RecordHook.RESULT callbackHooks(final RecordHook.TYPE type, final Identifiable id) {
    if (id == null || hooks.isEmpty() || id.getIdentity().getClusterId() == 0) {
      return RecordHook.RESULT.RECORD_NOT_CHANGED;
    }

    final RecordHook.SCOPE scope = RecordHook.SCOPE.typeToScope(type);
    final int scopeOrdinal = scope.ordinal();

    final RID identity = id.getIdentity().copy();
    if (!pushInHook(identity)) {
      return RecordHook.RESULT.RECORD_NOT_CHANGED;
    }

    try {
      final Record rec;
      try {
        rec = id.getRecord();
      } catch (RecordNotFoundException e) {
        return RecordHook.RESULT.RECORD_NOT_CHANGED;
      }

      final ScenarioThreadLocal.RUN_MODE runMode = ScenarioThreadLocal.INSTANCE.getRunMode();

      boolean recordChanged = false;
      for (RecordHook hook : hooksByScope[scopeOrdinal]) {
        switch (runMode) {
          case DEFAULT: // NON_DISTRIBUTED OR PROXIED DB
            if (isDistributed()
                && hook.getDistributedExecutionMode()
                == RecordHook.DISTRIBUTED_EXECUTION_MODE.TARGET_NODE)
            // SKIP
            {
              continue;
            }
            break; // TARGET NODE
          case RUNNING_DISTRIBUTED:
            if (hook.getDistributedExecutionMode()
                == RecordHook.DISTRIBUTED_EXECUTION_MODE.SOURCE_NODE) {
              continue;
            }
        }

        final RecordHook.RESULT res = hook.onTrigger(type, rec);

        if (res == RecordHook.RESULT.RECORD_CHANGED) {
          recordChanged = true;
        } else {
          if (res == RecordHook.RESULT.SKIP_IO)
          // SKIP IO OPERATION
          {
            return res;
          } else {
            if (res == RecordHook.RESULT.SKIP)
            // SKIP NEXT HOOKS AND RETURN IT
            {
              return res;
            } else {
              if (res == RecordHook.RESULT.RECORD_REPLACED) {
                return res;
              }
            }
          }
        }
      }
      return recordChanged
          ? RecordHook.RESULT.RECORD_CHANGED
          : RecordHook.RESULT.RECORD_NOT_CHANGED;
    } finally {
      popInHook(identity);
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isValidationEnabled() {
    return (Boolean) get(ATTRIBUTES.VALIDATION);
  }

  /**
   * {@inheritDoc}
   */
  public DatabaseSession setValidationEnabled(final boolean iEnabled) {
    set(ATTRIBUTES.VALIDATION, iEnabled);
    return this;
  }

  @Override
  public ContextConfiguration getConfiguration() {
    checkIfActive();
    if (getStorageInfo() != null) {
      return getStorageInfo().getConfiguration().getContextConfiguration();
    }
    return null;
  }

  @Override
  public void close() {
    internalClose(false);
  }

  @Override
  public STATUS getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return getStorageInfo() != null ? getStorageInfo().getName() : url;
  }

  @Override
  public String getURL() {
    return url != null ? url : getStorageInfo().getURL();
  }

  @Override
  public int getDefaultClusterId() {
    checkIfActive();
    return getStorageInfo().getDefaultClusterId();
  }

  @Override
  public int getClusters() {
    checkIfActive();
    return getStorageInfo().getClusters();
  }

  @Override
  public boolean existsCluster(final String iClusterName) {
    checkIfActive();
    return getStorageInfo().getClusterNames().contains(iClusterName.toLowerCase(Locale.ENGLISH));
  }

  @Override
  public Collection<String> getClusterNames() {
    checkIfActive();
    return getStorageInfo().getClusterNames();
  }

  @Override
  public int getClusterIdByName(final String iClusterName) {
    if (iClusterName == null) {
      return -1;
    }

    checkIfActive();
    return getStorageInfo().getClusterIdByName(iClusterName.toLowerCase(Locale.ENGLISH));
  }

  @Override
  public String getClusterNameById(final int iClusterId) {
    if (iClusterId < 0) {
      return null;
    }

    checkIfActive();
    return getStorageInfo().getPhysicalClusterNameById(iClusterId);
  }

  public void checkForClusterPermissions(final String iClusterName) {
    // CHECK FOR ORESTRICTED
    final Set<SchemaClass> classes =
        getMetadata().getImmutableSchemaSnapshot().getClassesRelyOnCluster(iClusterName);
    for (SchemaClass c : classes) {
      if (c.isSubClassOf(SecurityShared.RESTRICTED_CLASSNAME)) {
        throw new SecurityException(
            "Class '"
                + c.getName()
                + "' cannot be truncated because has record level security enabled (extends '"
                + SecurityShared.RESTRICTED_CLASSNAME
                + "')");
      }
    }
  }

  @Override
  public Object setProperty(final String iName, final Object iValue) {
    if (iValue == null) {
      return properties.remove(iName.toLowerCase(Locale.ENGLISH));
    } else {
      return properties.put(iName.toLowerCase(Locale.ENGLISH), iValue);
    }
  }

  @Override
  public Object getProperty(final String iName) {
    return properties.get(iName.toLowerCase(Locale.ENGLISH));
  }

  @Override
  public Iterator<Map.Entry<String, Object>> getProperties() {
    return properties.entrySet().iterator();
  }

  @Override
  public Object get(final ATTRIBUTES iAttribute) {
    checkIfActive();

    if (iAttribute == null) {
      throw new IllegalArgumentException("attribute is null");
    }
    final StorageInfo storage = getStorageInfo();
    return switch (iAttribute) {
      case STATUS -> getStatus();
      case DEFAULTCLUSTERID -> getDefaultClusterId();
      case TYPE ->
          getMetadata().getImmutableSchemaSnapshot().existsClass("V") ? "graph" : "document";
      case DATEFORMAT -> storage.getConfiguration().getDateFormat();
      case DATETIMEFORMAT -> storage.getConfiguration().getDateTimeFormat();
      case TIMEZONE -> storage.getConfiguration().getTimeZone().getID();
      case LOCALECOUNTRY -> storage.getConfiguration().getLocaleCountry();
      case LOCALELANGUAGE -> storage.getConfiguration().getLocaleLanguage();
      case CHARSET -> storage.getConfiguration().getCharset();
      case CUSTOM -> storage.getConfiguration().getProperties();
      case CLUSTERSELECTION -> storage.getConfiguration().getClusterSelection();
      case MINIMUMCLUSTERS -> storage.getConfiguration().getMinimumClusters();
      case CONFLICTSTRATEGY -> storage.getConfiguration().getConflictStrategy();
      case VALIDATION -> storage.getConfiguration().isValidationEnabled();
    };
  }

  public FrontendTransaction getTransaction() {
    checkIfActive();
    return currentTx;
  }

  /**
   * Returns the schema of the database.
   *
   * @return the schema of the database
   */
  @Override
  public Schema getSchema() {
    return getMetadata().getSchema();
  }


  @Nonnull
  @SuppressWarnings("unchecked")
  @Override
  public <RET extends Record> RET load(final RID recordId) {
    checkIfActive();
    return (RET) currentTx.loadRecord(recordId);
  }

  @Override
  public boolean exists(RID rid) {
    checkIfActive();
    return currentTx.exists(rid);
  }

  /**
   * Deletes the record without checking the version.
   */
  public void delete(final RID iRecord) {
    checkOpenness();
    checkIfActive();

    final Record rec = load(iRecord);
    delete(rec);
  }

  @Override
  public BinarySerializerFactory getSerializerFactory() {
    return componentsFactory.binarySerializerFactory;
  }

  @Override
  public void setPrefetchRecords(boolean prefetchRecords) {
    this.prefetchRecords = prefetchRecords;
  }

  @Override
  public boolean isPrefetchRecords() {
    return prefetchRecords;
  }

  @Override
  public <T extends Identifiable> T bindToSession(T identifiable) {
    if (!(identifiable instanceof Record record)) {
      return identifiable;
    }

    if (identifiable instanceof Edge edge && edge.isLightweight()) {
      return (T) edge;
    }

    var rid = record.getIdentity();
    if (rid == null) {
      throw new DatabaseException(
          "Cannot bind record to session with not persisted rid: " + rid);
    }

    checkOpenness();
    checkIfActive();

    // unwrap the record if wrapper is passed
    record = record.getRecord();

    var txRecord = currentTx.getRecord(rid);
    if (txRecord == record) {
      assert !txRecord.isUnloaded();
      assert txRecord.getSession() == this;
      return (T) record;
    }

    var cachedRecord = localCache.findRecord(rid);
    if (cachedRecord == record) {
      assert !cachedRecord.isUnloaded();
      assert cachedRecord.getSession() == this;
      return (T) record;
    }

    if (!rid.isPersistent()) {
      throw new DatabaseException(
          "Cannot bind record to session with not persisted rid: " + rid);
    }

    var result = executeReadRecord((RecordId) rid);

    assert !result.isUnloaded();
    assert result.getSession() == this;

    return (T) result;
  }

  @Nonnull
  public final <RET extends RecordAbstract> RET executeReadRecord(final RecordId rid) {
    checkOpenness();
    checkIfActive();

    getMetadata().makeThreadLocalSchemaSnapshot();
    try {
      checkSecurity(
          Rule.ResourceGeneric.CLUSTER,
          Role.PERMISSION_READ,
          getClusterNameById(rid.getClusterId()));

      // SEARCH IN LOCAL TX
      var record = getTransaction().getRecord(rid);
      if (record == FrontendTransactionAbstract.DELETED_RECORD) {
        // DELETED IN TX
        throw new RecordNotFoundException(rid);
      }

      var cachedRecord = localCache.findRecord(rid);
      if (record == null) {
        record = cachedRecord;
      }

      if (record != null && !record.isUnloaded()) {
        if (beforeReadOperations(record)) {
          throw new RecordNotFoundException(rid);
        }

        afterReadOperations(record);
        if (record instanceof EntityImpl) {
          DocumentInternal.checkClass((EntityImpl) record, this);
        }

        localCache.updateRecord(record);

        assert !record.isUnloaded();
        assert record.getSession() == this;

        return (RET) record;
      }

      if (cachedRecord != null) {
        if (cachedRecord.isDirty()) {
          throw new IllegalStateException("Cached record is dirty");
        }

        record = cachedRecord;
      }

      loadedRecordsCount++;
      final RawBuffer recordBuffer;
      if (!rid.isValid()) {
        recordBuffer = null;
      } else {
        recordBuffer = getStorage().readRecord(this, rid, false, prefetchRecords, null);
      }

      if (recordBuffer == null) {
        throw new RecordNotFoundException(rid);
      }

      if (record == null) {
        record =
            YouTrackDBManager.instance()
                .getRecordFactoryManager()
                .newInstance(recordBuffer.recordType, rid, this);
        RecordInternal.unsetDirty(record);
      }

      if (RecordInternal.getRecordType(record) != recordBuffer.recordType) {
        throw new DatabaseException("Record type is different from the one in the database");
      }

      RecordInternal.setRecordSerializer(record, serializer);
      RecordInternal.fill(record, rid, recordBuffer.version, recordBuffer.buffer, false, this);

      if (record instanceof EntityImpl) {
        DocumentInternal.checkClass((EntityImpl) record, this);
      }

      if (beforeReadOperations(record)) {
        throw new RecordNotFoundException(rid);
      }

      RecordInternal.fromStream(record, recordBuffer.buffer, this);
      afterReadOperations(record);

      localCache.updateRecord(record);

      assert !record.isUnloaded();
      assert record.getSession() == this;

      return (RET) record;
    } catch (OfflineClusterException | RecordNotFoundException t) {
      throw t;
    } catch (Exception t) {
      if (rid.isTemporary()) {
        throw BaseException.wrapException(
            new DatabaseException("Error on retrieving record using temporary RID: " + rid), t);
      } else {
        throw BaseException.wrapException(
            new DatabaseException(
                "Error on retrieving record "
                    + rid
                    + " (cluster: "
                    + getStorage().getPhysicalClusterNameById(rid.getClusterId())
                    + ")"),
            t);
      }
    } finally {
      getMetadata().clearThreadLocalSchemaSnapshot();
    }
  }

  public int assignAndCheckCluster(Record record, String iClusterName) {
    RecordId rid = (RecordId) record.getIdentity();
    // if provided a cluster name use it.
    if (rid.getClusterId() <= RID.CLUSTER_POS_INVALID && iClusterName != null) {
      rid.setClusterId(getClusterIdByName(iClusterName));
      if (rid.getClusterId() == -1) {
        throw new IllegalArgumentException("Cluster name '" + iClusterName + "' is not configured");
      }
    }
    SchemaClass schemaClass = null;
    // if cluster id is not set yet try to find it out
    if (rid.getClusterId() <= RID.CLUSTER_ID_INVALID
        && getStorageInfo().isAssigningClusterIds()) {
      if (record instanceof EntityImpl) {
        schemaClass = DocumentInternal.getImmutableSchemaClass(this, ((EntityImpl) record));
        if (schemaClass != null) {
          if (schemaClass.isAbstract()) {
            throw new SchemaException(
                "Document belongs to abstract class "
                    + schemaClass.getName()
                    + " and cannot be saved");
          }
          rid.setClusterId(schemaClass.getClusterForNewInstance((EntityImpl) record));
        } else {
          var defaultCluster = getStorageInfo().getDefaultClusterId();
          if (defaultCluster < 0) {
            throw new DatabaseException(
                "Cannot save (1) document " + record + ": no class or cluster defined");
          }
          rid.setClusterId(defaultCluster);
        }
      } else {
        if (record instanceof RecordBytes) {
          IntSet blobs = getBlobClusterIds();
          if (blobs.isEmpty()) {
            rid.setClusterId(getDefaultClusterId());
          } else {
            rid.setClusterId(blobs.iterator().nextInt());
          }
        } else {
          throw new DatabaseException(
              "Cannot save (3) document " + record + ": no class or cluster defined");
        }
      }
    } else {
      if (record instanceof EntityImpl) {
        schemaClass = DocumentInternal.getImmutableSchemaClass(this, ((EntityImpl) record));
      }
    }
    // If the cluster id was set check is validity
    if (rid.getClusterId() > RID.CLUSTER_ID_INVALID) {
      if (schemaClass != null) {
        String messageClusterName = getClusterNameById(rid.getClusterId());
        checkRecordClass(schemaClass, messageClusterName, rid);
        if (!schemaClass.hasClusterId(rid.getClusterId())) {
          throw new IllegalArgumentException(
              "Cluster name '"
                  + messageClusterName
                  + "' (id="
                  + rid.getClusterId()
                  + ") is not configured to store the class '"
                  + schemaClass.getName()
                  + "', valid are "
                  + Arrays.toString(schemaClass.getClusterIds()));
        }
      }
    }
    return rid.getClusterId();
  }

  public int begin() {
    assert assertIfNotActive();

    if (currentTx.isActive()) {
      return currentTx.begin();
    }

    return begin(newTxInstance());
  }

  public int begin(TransactionOptimistic transaction) {
    checkOpenness();
    checkIfActive();

    // CHECK IT'S NOT INSIDE A HOOK
    if (!inHook.isEmpty()) {
      throw new IllegalStateException("Cannot begin a transaction while a hook is executing");
    }

    if (currentTx.isActive()) {
      if (currentTx instanceof TransactionOptimistic) {
        return currentTx.begin();
      }
    }

    // WAKE UP LISTENERS
    for (DatabaseListener listener : browseListeners()) {
      try {
        listener.onBeforeTxBegin(this);
      } catch (Exception e) {
        LogManager.instance().error(this, "Error before tx begin", e);
      }
    }

    currentTx = transaction;

    return currentTx.begin();
  }

  protected TransactionOptimistic newTxInstance() {
    return new TransactionOptimistic(this);
  }

  public void setDefaultTransactionMode() {
    if (!(currentTx instanceof FrontendTransactionNoTx)) {
      currentTx = new FrontendTransactionNoTx(this);
    }
  }

  /**
   * Creates a new EntityImpl.
   */
  public EntityImpl newInstance() {
    return new EntityImpl(Entity.DEFAULT_CLASS_NAME, this);
  }

  @Override
  public Blob newBlob(byte[] bytes) {
    return new RecordBytes(this, bytes);
  }

  @Override
  public Blob newBlob() {
    return new RecordBytes(this);
  }

  /**
   * Creates a document with specific class.
   *
   * @param iClassName the name of class that should be used as a class of created document.
   * @return new instance of document.
   */
  @Override
  public EntityImpl newInstance(final String iClassName) {
    return new EntityImpl(this, iClassName);
  }

  @Override
  public Entity newEntity() {
    return newInstance();
  }

  @Override
  public Entity newEntity(String className) {
    return newInstance(className);
  }

  public Entity newEntity(SchemaClass clazz) {
    return newInstance(clazz.getName());
  }

  public Vertex newVertex(final String iClassName) {
    return new VertexEntityImpl(this, iClassName);
  }

  private EdgeInternal newEdgeInternal(final String iClassName) {
    return new EdgeEntityImpl(this, iClassName);
  }

  @Override
  public Vertex newVertex(SchemaClass type) {
    if (type == null) {
      return newVertex("V");
    }
    return newVertex(type.getName());
  }

  @Override
  public EdgeInternal newEdge(Vertex from, Vertex to, String type) {
    SchemaClass cl = getMetadata().getImmutableSchemaSnapshot().getClass(type);
    if (cl == null || !cl.isEdgeType()) {
      throw new IllegalArgumentException(type + " is not an edge class");
    }

    return addEdgeInternal(from, to, type, true);
  }

  @Override
  public EdgeInternal addLightweightEdge(Vertex from, Vertex to, String className) {
    SchemaClass cl = getMetadata().getImmutableSchemaSnapshot().getClass(className);
    if (cl == null || !cl.isEdgeType()) {
      throw new IllegalArgumentException(className + " is not an edge class");
    }

    return addEdgeInternal(from, to, className, false);
  }

  @Override
  public Edge newEdge(Vertex from, Vertex to, SchemaClass type) {
    if (type == null) {
      return newEdge(from, to, "E");
    }
    return newEdge(from, to, type.getName());
  }

  private EdgeInternal addEdgeInternal(
      final Vertex toVertex,
      final Vertex inVertex,
      String className,
      boolean isRegular) {
    Objects.requireNonNull(toVertex, "From vertex is null");
    Objects.requireNonNull(inVertex, "To vertex is null");

    EdgeInternal edge;
    EntityImpl outDocument;
    EntityImpl inDocument;

    boolean outDocumentModified = false;
    if (checkDeletedInTx(toVertex)) {
      throw new RecordNotFoundException(
          toVertex.getIdentity(),
          "The vertex " + toVertex.getIdentity() + " has been deleted");
    }

    if (checkDeletedInTx(inVertex)) {
      throw new RecordNotFoundException(
          inVertex.getIdentity(), "The vertex " + inVertex.getIdentity() + " has been deleted");
    }

    try {
      outDocument = toVertex.getRecord();
    } catch (RecordNotFoundException e) {
      throw new IllegalArgumentException(
          "source vertex is invalid (rid=" + toVertex.getIdentity() + ")");
    }

    try {
      inDocument = inVertex.getRecord();
    } catch (RecordNotFoundException e) {
      throw new IllegalArgumentException(
          "source vertex is invalid (rid=" + inVertex.getIdentity() + ")");
    }

    Schema schema = getMetadata().getImmutableSchemaSnapshot();
    final SchemaClass edgeType = schema.getClass(className);

    if (edgeType == null) {
      throw new IllegalArgumentException("Class " + className + " does not exist");
    }

    className = edgeType.getName();

    var createLightweightEdge =
        !isRegular
            && (edgeType.isAbstract() || className.equals(EdgeInternal.CLASS_NAME));
    if (!isRegular && !createLightweightEdge) {
      throw new IllegalArgumentException(
          "Cannot create lightweight edge for class " + className + " because it is not abstract");
    }

    final String outFieldName = Vertex.getEdgeLinkFieldName(Direction.OUT, className);
    final String inFieldName = Vertex.getEdgeLinkFieldName(Direction.IN, className);

    if (createLightweightEdge) {
      edge = newLightweightEdge(className, toVertex, inVertex);
      VertexInternal.createLink(toVertex.getRecord(), inVertex.getRecord(), outFieldName);
      VertexInternal.createLink(inVertex.getRecord(), toVertex.getRecord(), inFieldName);
    } else {
      edge = newEdgeInternal(className);
      edge.setPropertyInternal(EdgeInternal.DIRECTION_OUT, toVertex.getRecord());
      edge.setPropertyInternal(Edge.DIRECTION_IN, inDocument.getRecord());

      if (!outDocumentModified) {
        // OUT-VERTEX ---> IN-VERTEX/EDGE
        VertexInternal.createLink(outDocument, edge.getRecord(), outFieldName);
      }

      // IN-VERTEX ---> OUT-VERTEX/EDGE
      VertexInternal.createLink(inDocument, edge.getRecord(), inFieldName);
    }
    // OK

    return edge;
  }

  private boolean checkDeletedInTx(Vertex currentVertex) {
    RID id;
    if (!currentVertex.getRecord().exists()) {
      id = currentVertex.getRecord().getIdentity();
    } else {
      return false;
    }

    final RecordOperation oper = getTransaction().getRecordEntry(id);
    if (oper == null) {
      return id.isTemporary();
    } else {
      return oper.type == RecordOperation.DELETED;
    }
  }

  /**
   * {@inheritDoc}
   */
  public RecordIteratorClass<EntityImpl> browseClass(final String iClassName) {
    return browseClass(iClassName, true);
  }

  /**
   * {@inheritDoc}
   */
  public RecordIteratorClass<EntityImpl> browseClass(
      final String iClassName, final boolean iPolymorphic) {
    if (getMetadata().getImmutableSchemaSnapshot().getClass(iClassName) == null) {
      throw new IllegalArgumentException(
          "Class '" + iClassName + "' not found in current database");
    }

    checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_READ, iClassName);
    return new RecordIteratorClass<EntityImpl>(this, iClassName, iPolymorphic, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RecordIteratorCluster<Record> browseCluster(final String iClusterName) {
    checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_READ, iClusterName);

    return new RecordIteratorCluster<>(this, getClusterIdByName(iClusterName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<DatabaseListener> getListeners() {
    return getListenersCopy();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public RecordIteratorCluster<EntityImpl> browseCluster(
      String iClusterName,
      long startClusterPosition,
      long endClusterPosition,
      boolean loadTombstones) {
    checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_READ, iClusterName);

    return new RecordIteratorCluster<EntityImpl>(
        this, getClusterIdByName(iClusterName), startClusterPosition, endClusterPosition);
  }

  /**
   * Saves a document to the database. Behavior depends on the current running transaction if any.
   * If no transaction is running then changes apply immediately. If an Optimistic transaction is
   * running then the record will be changed at commit time. The current transaction will continue
   * to see the record as modified, while others not. If a Pessimistic transaction is running, then
   * an exclusive lock is acquired against the record. Current transaction will continue to see the
   * record as modified, while others cannot access to it since it's locked.
   *
   * <p>If MVCC is enabled and the version of the document is different by the version stored in
   * the database, then a {@link ConcurrentModificationException} exception is thrown.Before to save
   * the document it must be valid following the constraints declared in the schema if any (can work
   * also in schema-less mode). To validate the document the {@link EntityImpl#validate()} is
   * called.
   *
   * @param record Record to save.
   * @return The Database instance itself giving a "fluent interface". Useful to call multiple
   * methods in chain.
   * @throws ConcurrentModificationException if the version of the document is different by the
   *                                         version contained in the database.
   * @throws ValidationException             if the document breaks some validation constraints
   *                                         defined in the schema
   * @see #setMVCC(boolean), {@link #isMVCC()}
   */
  @Override
  public <RET extends Record> RET save(final Record record) {
    return save(record, null);
  }

  /**
   * Saves a document specifying a cluster where to store the record. Behavior depends by the
   * current running transaction if any. If no transaction is running then changes apply
   * immediately. If an Optimistic transaction is running then the record will be changed at commit
   * time. The current transaction will continue to see the record as modified, while others not. If
   * a Pessimistic transaction is running, then an exclusive lock is acquired against the record.
   * Current transaction will continue to see the record as modified, while others cannot access to
   * it since it's locked.
   *
   * <p>If MVCC is enabled and the version of the document is different by the version stored in
   * the database, then a {@link ConcurrentModificationException} exception is thrown. Before to
   * save the document it must be valid following the constraints declared in the schema if any (can
   * work also in schema-less mode). To validate the document the {@link EntityImpl#validate()} is
   * called.
   *
   * @param record      Record to save
   * @param clusterName Cluster name where to save the record
   * @return The Database instance itself giving a "fluent interface". Useful to call multiple
   * methods in chain.
   * @throws ConcurrentModificationException if the version of the document is different by the
   *                                         version contained in the database.
   * @throws ValidationException             if the document breaks some validation constraints
   *                                         defined in the schema
   * @see #setMVCC(boolean), {@link #isMVCC()}, EntityImpl#validate()
   */
  @Override
  public <RET extends Record> RET save(Record record, String clusterName) {
    checkOpenness();

    if (record instanceof Edge edge) {
      if (edge.isLightweight()) {
        record = edge.getFrom();
      }
    }

    // unwrap the record if wrapper is passed
    record = record.getRecord();

    if (record.isUnloaded()) {
      throw new DatabaseException(
          "Record "
              + record
              + " is not bound to session, please call "
              + DatabaseSession.class.getSimpleName()
              + ".bindToSession(record) before save it");
    }

    return saveInternal((RecordAbstract) record, clusterName);
  }

  private <RET extends Record> RET saveInternal(RecordAbstract record, String clusterName) {

    if (!(record instanceof EntityImpl document)) {
      assignAndCheckCluster(record, clusterName);
      return (RET) currentTx.saveRecord(record, clusterName);
    }

    EntityImpl doc = document;
    DocumentInternal.checkClass(doc, this);
    try {
      doc.autoConvertValues();
    } catch (ValidationException e) {
      doc.undo();
      throw e;
    }
    DocumentInternal.convertAllMultiValuesToTrackedVersions(doc);

    if (!doc.getIdentity().isValid()) {
      if (doc.getClassName() != null) {
        checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_CREATE, doc.getClassName());
      }

      assignAndCheckCluster(doc, clusterName);
    } else {
      // UPDATE: CHECK ACCESS ON SCHEMA CLASS NAME (IF ANY)
      if (doc.getClassName() != null) {
        checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_UPDATE, doc.getClassName());
      }
    }

    if (!serializer.equals(RecordInternal.getRecordSerializer(doc))) {
      RecordInternal.setRecordSerializer(doc, serializer);
    }

    doc = (EntityImpl) currentTx.saveRecord(record, clusterName);
    return (RET) doc;
  }

  /**
   * Returns the number of the records of the class iClassName.
   */
  public long countView(final String viewName) {
    final SchemaImmutableView cls =
        (SchemaImmutableView) getMetadata().getImmutableSchemaSnapshot().getView(viewName);
    if (cls == null) {
      throw new IllegalArgumentException("View '" + cls + "' not found in database");
    }

    return countClass(cls, false);
  }

  /**
   * Returns the number of the records of the class iClassName.
   */
  public long countClass(final String iClassName) {
    return countClass(iClassName, true);
  }

  /**
   * Returns the number of the records of the class iClassName considering also sub classes if
   * polymorphic is true.
   */
  public long countClass(final String iClassName, final boolean iPolymorphic) {
    final SchemaImmutableClass cls =
        (SchemaImmutableClass) getMetadata().getImmutableSchemaSnapshot().getClass(iClassName);
    if (cls == null) {
      throw new IllegalArgumentException("Class '" + cls + "' not found in database");
    }

    return countClass(cls, iPolymorphic);
  }

  protected long countClass(final SchemaImmutableClass cls, final boolean iPolymorphic) {
    checkOpenness();

    long totalOnDb = cls.countImpl(iPolymorphic);

    long deletedInTx = 0;
    long addedInTx = 0;
    String className = cls.getName();
    if (getTransaction().isActive()) {
      for (RecordOperation op : getTransaction().getRecordOperations()) {
        if (op.type == RecordOperation.DELETED) {
          final Record rec = op.record;
          if (rec instanceof EntityImpl) {
            SchemaClass schemaClass = DocumentInternal.getImmutableSchemaClass(((EntityImpl) rec));
            if (iPolymorphic) {
              if (schemaClass.isSubClassOf(className)) {
                deletedInTx++;
              }
            } else {
              if (className.equals(schemaClass.getName())
                  || className.equals(schemaClass.getShortName())) {
                deletedInTx++;
              }
            }
          }
        }
        if (op.type == RecordOperation.CREATED) {
          final Record rec = op.record;
          if (rec instanceof EntityImpl) {
            SchemaClass schemaClass = DocumentInternal.getImmutableSchemaClass(((EntityImpl) rec));
            if (schemaClass != null) {
              if (iPolymorphic) {
                if (schemaClass.isSubClassOf(className)) {
                  addedInTx++;
                }
              } else {
                if (className.equals(schemaClass.getName())
                    || className.equals(schemaClass.getShortName())) {
                  addedInTx++;
                }
              }
            }
          }
        }
      }
    }

    return (totalOnDb + addedInTx) - deletedInTx;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean commit() {
    checkOpenness();
    checkIfActive();

    if (currentTx.getStatus() == TXSTATUS.ROLLBACKING) {
      throw new RollbackException("Transaction is rolling back");
    }

    if (!currentTx.isActive()) {
      throw new DatabaseException("No active transaction to commit. Call begin() first");
    }

    if (currentTx.amountOfNestedTxs() > 1) {
      // This just do count down no real commit here
      currentTx.commit();
      return false;
    }

    // WAKE UP LISTENERS

    try {
      beforeCommitOperations();
    } catch (BaseException e) {
      try {
        rollback();
      } catch (Exception re) {
        LogManager.instance()
            .error(this, "Exception during rollback `%08X`", re, System.identityHashCode(re));
      }
      throw e;
    }
    try {
      currentTx.commit();
    } catch (RuntimeException e) {

      if ((e instanceof HighLevelException) || (e instanceof NeedRetryException)) {
        LogManager.instance()
            .debug(this, "Error on transaction commit `%08X`", e, System.identityHashCode(e));
      } else {
        LogManager.instance()
            .error(this, "Error on transaction commit `%08X`", e, System.identityHashCode(e));
      }

      // WAKE UP ROLLBACK LISTENERS
      beforeRollbackOperations();

      try {
        // ROLLBACK TX AT DB LEVEL
        currentTx.internalRollback();
      } catch (Exception re) {
        LogManager.instance()
            .error(
                this, "Error during transaction rollback `%08X`", re, System.identityHashCode(re));
      }

      // WAKE UP ROLLBACK LISTENERS
      afterRollbackOperations();
      throw e;
    }

    return true;
  }

  protected void beforeCommitOperations() {
    for (DatabaseListener listener : browseListeners()) {
      try {
        listener.onBeforeTxCommit(this);
      } catch (Exception e) {
        LogManager.instance()
            .error(
                this,
                "Cannot commit the transaction: caught exception on execution of"
                    + " %s.onBeforeTxCommit() `%08X`",
                e,
                listener.getClass().getName(),
                System.identityHashCode(e));
        throw BaseException.wrapException(
            new TransactionException(
                "Cannot commit the transaction: caught exception on execution of "
                    + listener.getClass().getName()
                    + "#onBeforeTxCommit()"),
            e);
      }
    }
  }

  public void afterCommitOperations() {
    for (DatabaseListener listener : browseListeners()) {
      try {
        listener.onAfterTxCommit(this);
      } catch (Exception e) {
        final String message =
            "Error after the transaction has been committed. The transaction remains valid. The"
                + " exception caught was on execution of "
                + listener.getClass()
                + ".onAfterTxCommit() `%08X`";

        LogManager.instance().error(this, message, e, System.identityHashCode(e));

        throw BaseException.wrapException(new TransactionBlockedException(message), e);
      }
    }
  }

  protected void beforeRollbackOperations() {
    for (DatabaseListener listener : browseListeners()) {
      try {
        listener.onBeforeTxRollback(this);
      } catch (Exception t) {
        LogManager.instance()
            .error(this, "Error before transaction rollback `%08X`", t, System.identityHashCode(t));
      }
    }
  }

  protected void afterRollbackOperations() {
    for (DatabaseListener listener : browseListeners()) {
      try {
        listener.onAfterTxRollback(this);
      } catch (Exception t) {
        LogManager.instance()
            .error(this, "Error after transaction rollback `%08X`", t, System.identityHashCode(t));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() {
    rollback(false);
  }

  @Override
  public void rollback(boolean force) throws TransactionException {
    checkOpenness();
    if (currentTx.isActive()) {

      if (!force && currentTx.amountOfNestedTxs() > 1) {
        // This just decrement the counter no real rollback here
        currentTx.rollback();
        return;
      }

      // WAKE UP LISTENERS
      beforeRollbackOperations();
      currentTx.rollback(force, -1);
      // WAKE UP LISTENERS
      afterRollbackOperations();
    }
  }

  /**
   * This method is internal, it can be subject to signature change or be removed, do not use.
   */
  @Override
  public DatabaseSession getUnderlying() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CurrentStorageComponentsFactory getStorageVersions() {
    return componentsFactory;
  }

  public RecordSerializer getSerializer() {
    return serializer;
  }

  /**
   * Sets serializer for the database which will be used for document serialization.
   *
   * @param serializer the serializer to set.
   */
  public void setSerializer(RecordSerializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public void resetInitialization() {
    for (RecordHook h : hooks.keySet()) {
      h.onUnregister();
    }

    hooks.clear();
    compileHooks();

    close();

    initialized = false;
  }

  public void checkSecurity(final int operation, final Identifiable record, String cluster) {
    if (cluster == null) {
      cluster = getClusterNameById(record.getIdentity().getClusterId());
    }
    checkSecurity(Rule.ResourceGeneric.CLUSTER, operation, cluster);

    if (record instanceof EntityImpl) {
      String clazzName = ((EntityImpl) record).getClassName();
      if (clazzName != null) {
        checkSecurity(Rule.ResourceGeneric.CLASS, operation, clazzName);
      }
    }
  }

  /**
   * @return <code>true</code> if database is obtained from the pool and <code>false</code>
   * otherwise.
   */
  @Override
  public boolean isPooled() {
    return false;
  }

  /**
   * Use #activateOnCurrentThread instead.
   */
  @Deprecated
  public void setCurrentDatabaseInThreadLocal() {
    activateOnCurrentThread();
  }

  /**
   * Activates current database instance on current thread.
   */
  @Override
  public void activateOnCurrentThread() {
    final DatabaseRecordThreadLocal tl = DatabaseRecordThreadLocal.instance();
    tl.set(this);
  }

  @Override
  public boolean isActiveOnCurrentThread() {
    final DatabaseRecordThreadLocal tl = DatabaseRecordThreadLocal.instance();
    final DatabaseSessionInternal db = tl.getIfDefined();
    return db == this;
  }

  protected void checkOpenness() {
    if (status == STATUS.CLOSED) {
      throw new DatabaseException("Database '" + getURL() + "' is closed");
    }
  }

  private void popInHook(Identifiable id) {
    inHook.remove(id);
  }

  private boolean pushInHook(Identifiable id) {
    return inHook.add(id);
  }

  protected void callbackHookFailure(Record record, boolean wasNew, byte[] stream) {
    if (stream != null && stream.length > 0) {
      callbackHooks(
          wasNew ? RecordHook.TYPE.CREATE_FAILED : RecordHook.TYPE.UPDATE_FAILED, record);
    }
  }

  protected void callbackHookSuccess(
      final Record record,
      final boolean wasNew,
      final byte[] stream,
      final StorageOperationResult<Integer> operationResult) {
    if (stream != null && stream.length > 0) {
      final RecordHook.TYPE hookType;
      if (!operationResult.isMoved()) {
        hookType = wasNew ? RecordHook.TYPE.AFTER_CREATE : RecordHook.TYPE.AFTER_UPDATE;
      } else {
        hookType =
            wasNew ? RecordHook.TYPE.CREATE_REPLICATED : RecordHook.TYPE.UPDATE_REPLICATED;
      }
      callbackHooks(hookType, record);
    }
  }

  protected void callbackHookFinalize(
      final Record record, final boolean wasNew, final byte[] stream) {
    if (stream != null && stream.length > 0) {
      final RecordHook.TYPE hookType;
      hookType = wasNew ? RecordHook.TYPE.FINALIZE_CREATION : RecordHook.TYPE.FINALIZE_UPDATE;
      callbackHooks(hookType, record);

      clearDocumentTracking(record);
    }
  }

  protected static void clearDocumentTracking(final Record record) {
    if (record instanceof EntityImpl && ((EntityImpl) record).isTrackingChanges()) {
      DocumentInternal.clearTrackData((EntityImpl) record);
    }
  }

  protected void checkRecordClass(
      final SchemaClass recordClass, final String iClusterName, final RecordId rid) {
    final SchemaClass clusterIdClass =
        metadata.getImmutableSchemaSnapshot().getClassByClusterId(rid.getClusterId());
    if (recordClass == null && clusterIdClass != null
        || clusterIdClass == null && recordClass != null
        || (recordClass != null && !recordClass.equals(clusterIdClass))) {
      throw new IllegalArgumentException(
          "Record saved into cluster '"
              + iClusterName
              + "' should be saved with class '"
              + clusterIdClass
              + "' but has been created with class '"
              + recordClass
              + "'");
    }
  }

  protected void init() {
    currentTx = new FrontendTransactionNoTx(this);
  }

  public void checkIfActive() {
    final DatabaseRecordThreadLocal tl = DatabaseRecordThreadLocal.instance();
    DatabaseSessionInternal currentDatabase = tl.get();
    //noinspection deprecation
    if (currentDatabase instanceof DatabaseDocumentTx databaseDocumentTx) {
      currentDatabase = databaseDocumentTx.internal;
    }
    if (currentDatabase != this) {
      throw new IllegalStateException(
          "The current database instance ("
              + this
              + ") is not active on the current thread ("
              + Thread.currentThread()
              + "). Current active database is: "
              + currentDatabase);
    }
  }

  @Override
  public boolean assertIfNotActive() {
    final DatabaseRecordThreadLocal tl = DatabaseRecordThreadLocal.instance();
    DatabaseSessionInternal currentDatabase = tl.get();

    //noinspection deprecation
    if (currentDatabase instanceof DatabaseDocumentTx databaseDocumentTx) {
      currentDatabase = databaseDocumentTx.internal;
    }

    if (currentDatabase != this) {
      throw new SessionNotActivatedException(getName());
    }

    return true;
  }

  public IntSet getBlobClusterIds() {
    return getMetadata().getSchema().getBlobClusters();
  }

  private void compileHooks() {
    final List<RecordHook>[] intermediateHooksByScope =
        new List[RecordHook.SCOPE.values().length];
    for (RecordHook.SCOPE scope : RecordHook.SCOPE.values()) {
      intermediateHooksByScope[scope.ordinal()] = new ArrayList<>();
    }

    for (RecordHook hook : hooks.keySet()) {
      for (RecordHook.SCOPE scope : hook.getScopes()) {
        intermediateHooksByScope[scope.ordinal()].add(hook);
      }
    }

    for (RecordHook.SCOPE scope : RecordHook.SCOPE.values()) {
      final int ordinal = scope.ordinal();
      final List<RecordHook> scopeHooks = intermediateHooksByScope[ordinal];
      hooksByScope[ordinal] = scopeHooks.toArray(new RecordHook[0]);
    }
  }

  @Override
  public SharedContext getSharedContext() {
    return sharedContext;
  }


  public void setUseLightweightEdges(boolean b) {
    this.setCustom("useLightweightEdges", b);
  }

  public EdgeInternal newLightweightEdge(String iClassName, Vertex from, Vertex to) {
    SchemaImmutableClass clazz =
        (SchemaImmutableClass) getMetadata().getImmutableSchemaSnapshot().getClass(iClassName);

    return new EdgeDelegate(from, to, clazz, iClassName);
  }

  public Edge newRegularEdge(String iClassName, Vertex from, Vertex to) {
    SchemaClass cl = getMetadata().getImmutableSchemaSnapshot().getClass(iClassName);

    if (cl == null || !cl.isEdgeType()) {
      throw new IllegalArgumentException(iClassName + " is not an edge class");
    }

    return addEdgeInternal(from, to, iClassName, true);
  }

  public synchronized void queryStarted(String id, QueryDatabaseState state) {
    if (this.activeQueries.size() > 1 && this.activeQueries.size() % 10 == 0) {
      String msg =
          "This database instance has "
              + activeQueries.size()
              + " open command/query result sets, please make sure you close them with"
              + " ResultSet.close()";
      LogManager.instance().warn(this, msg);
      if (LogManager.instance().isDebugEnabled()) {
        activeQueries.values().stream()
            .map(pendingQuery -> pendingQuery.getResultSet().getExecutionPlan())
            .filter(Objects::nonNull)
            .forEach(plan -> LogManager.instance().debug(this, plan.toString()));
      }
    }
    this.activeQueries.put(id, state);

    getListeners().forEach((it) -> it.onCommandStart(this, state.getResultSet()));
  }

  public void queryClosed(String id) {
    QueryDatabaseState removed = this.activeQueries.remove(id);
    getListeners().forEach((it) -> it.onCommandEnd(this, removed.getResultSet()));
    removed.closeInternal(this);
  }

  protected synchronized void closeActiveQueries() {
    while (!activeQueries.isEmpty()) {
      this.activeQueries
          .values()
          .iterator()
          .next()
          .close(this); // the query automatically unregisters itself
    }
  }

  public Map<String, QueryDatabaseState> getActiveQueries() {
    return activeQueries;
  }

  public ResultSet getActiveQuery(String id) {
    QueryDatabaseState state = activeQueries.get(id);
    if (state != null) {
      return state.getResultSet();
    } else {
      return null;
    }
  }

  @Override
  public boolean isClusterEdge(int cluster) {
    SchemaClass clazz = getMetadata().getImmutableSchemaSnapshot().getClassByClusterId(cluster);
    return clazz != null && clazz.isEdgeType();
  }

  @Override
  public boolean isClusterVertex(int cluster) {
    SchemaClass clazz = getMetadata().getImmutableSchemaSnapshot().getClassByClusterId(cluster);
    return clazz != null && clazz.isVertexType();
  }

  @Override
  public boolean isClusterView(int cluster) {
    SchemaView view = getViewFromCluster(cluster);
    return view != null;
  }

  public SchemaView getViewFromCluster(int cluster) {
    return getMetadata().getImmutableSchemaSnapshot().getViewByClusterId(cluster);
  }

  public Map<UUID, BonsaiCollectionPointer> getCollectionsChanges() {
    if (collectionsChanges == null) {
      collectionsChanges = new HashMap<>();
    }
    return collectionsChanges;
  }

  @Override
  public void executeInTx(Runnable runnable) {
    var ok = false;
    checkIfActive();
    begin();
    try {
      runnable.run();
      ok = true;
    } finally {
      finishTx(ok);
    }
  }

  @Override
  public <T> void executeInTxBatches(
      Iterable<T> iterable, int batchSize, BiConsumer<DatabaseSession, T> consumer) {
    var ok = false;
    checkIfActive();
    int counter = 0;

    begin();
    try {
      for (T t : iterable) {
        consumer.accept(this, t);
        counter++;

        if (counter % batchSize == 0) {
          commit();
          begin();
        }
      }

      ok = true;
    } finally {
      finishTx(ok);
    }
  }

  @Override
  public <T> void forEachInTx(Iterator<T> iterator, BiConsumer<DatabaseSession, T> consumer) {
    forEachInTx(iterator, (db, t) -> {
      consumer.accept(db, t);
      return true;
    });
  }

  @Override
  public <T> void forEachInTx(Iterable<T> iterable, BiConsumer<DatabaseSession, T> consumer) {
    forEachInTx(iterable.iterator(), consumer);
  }

  @Override
  public <T> void forEachInTx(Stream<T> stream, BiConsumer<DatabaseSession, T> consumer) {
    try (Stream<T> s = stream) {
      forEachInTx(s.iterator(), consumer);
    }
  }

  @Override
  public <T> void forEachInTx(Iterator<T> iterator,
      BiFunction<DatabaseSession, T, Boolean> consumer) {
    var ok = false;
    checkIfActive();

    begin();
    try {
      while (iterator.hasNext()) {
        var cont = consumer.apply(this, iterator.next());
        commit();
        if (!cont) {
          break;
        }
        begin();
      }

      ok = true;
    } finally {
      finishTx(ok);
    }
  }

  @Override
  public <T> void forEachInTx(Iterable<T> iterable,
      BiFunction<DatabaseSession, T, Boolean> consumer) {
    forEachInTx(iterable.iterator(), consumer);
  }

  @Override
  public <T> void forEachInTx(Stream<T> stream,
      BiFunction<DatabaseSession, T, Boolean> consumer) {
    try (stream) {
      forEachInTx(stream.iterator(), consumer);
    }
  }

  private void finishTx(boolean ok) {
    if (currentTx.isActive()) {
      if (ok && currentTx.getStatus() != TXSTATUS.ROLLBACKING) {
        commit();
      } else {
        if (isActiveOnCurrentThread()) {
          rollback();
        } else {
          currentTx.rollback();
        }
      }
    }
  }

  @Override
  public <T> void executeInTxBatches(
      Iterator<T> iterator, int batchSize, BiConsumer<DatabaseSession, T> consumer) {
    var ok = false;
    checkIfActive();
    int counter = 0;

    begin();
    try {
      while (iterator.hasNext()) {
        consumer.accept(this, iterator.next());
        counter++;

        if (counter % batchSize == 0) {
          commit();
          begin();
        }
      }

      ok = true;
    } finally {
      finishTx(ok);
    }
  }

  @Override
  public <T> void executeInTxBatches(
      Iterator<T> iterator, BiConsumer<DatabaseSession, T> consumer) {
    executeInTxBatches(
        iterator,
        getConfiguration().getValueAsInteger(GlobalConfiguration.TX_BATCH_SIZE),
        consumer);
  }

  @Override
  public <T> void executeInTxBatches(
      Iterable<T> iterable, BiConsumer<DatabaseSession, T> consumer) {
    executeInTxBatches(
        iterable,
        getConfiguration().getValueAsInteger(GlobalConfiguration.TX_BATCH_SIZE),
        consumer);
  }

  @Override
  public <T> void executeInTxBatches(
      Stream<T> stream, int batchSize, BiConsumer<DatabaseSession, T> consumer) {
    try (stream) {
      executeInTxBatches(stream.iterator(), batchSize, consumer);
    }
  }

  @Override
  public <T> void executeInTxBatches(Stream<T> stream, BiConsumer<DatabaseSession, T> consumer) {
    try (stream) {
      executeInTxBatches(stream.iterator(), consumer);
    }
  }

  @Override
  public <T> T computeInTx(Supplier<T> supplier) {
    checkIfActive();
    var ok = false;
    begin();
    try {
      var result = supplier.get();
      ok = true;
      return result;
    } finally {
      finishTx(ok);
    }
  }

  @Override
  public int activeTxCount() {
    var transaction = getTransaction();

    if (transaction.isActive()) {
      return transaction.amountOfNestedTxs();
    }

    return 0;
  }
}
