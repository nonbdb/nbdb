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

import com.jetbrains.youtrack.db.internal.common.exception.BaseException;
import com.jetbrains.youtrack.db.internal.common.io.IOUtils;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBManager;
import com.jetbrains.youtrack.db.internal.core.cache.LocalRecordCache;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.ScriptExecutor;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.conflict.RecordConflictStrategy;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseLifecycleListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseStats;
import com.jetbrains.youtrack.db.internal.core.db.HookReplacedRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.SharedContext;
import com.jetbrains.youtrack.db.internal.core.db.SharedContextEmbedded;
import com.jetbrains.youtrack.db.internal.core.db.LiveQueryMonitor;
import com.jetbrains.youtrack.db.internal.core.db.LiveQueryResultListener;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.db.record.ClassTrigger;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.viewmanager.ViewManager;
import com.jetbrains.youtrack.db.internal.core.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.exception.DatabaseException;
import com.jetbrains.youtrack.db.internal.core.exception.SecurityAccessException;
import com.jetbrains.youtrack.db.internal.core.exception.SecurityException;
import com.jetbrains.youtrack.db.internal.core.exception.ConcurrentModificationException;
import com.jetbrains.youtrack.db.internal.core.exception.SchemaException;
import com.jetbrains.youtrack.db.internal.core.hook.RecordHook;
import com.jetbrains.youtrack.db.internal.core.id.RID;
import com.jetbrains.youtrack.db.internal.core.index.ClassIndexManager;
import com.jetbrains.youtrack.db.internal.core.iterator.RecordIteratorCluster;
import com.jetbrains.youtrack.db.internal.core.metadata.MetadataDefault;
import com.jetbrains.youtrack.db.internal.core.metadata.function.FunctionLibraryImpl;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaImmutableClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaProxy;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.ImmutableSchema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaView;
import com.jetbrains.youtrack.db.internal.core.metadata.security.RestrictedAccessHook;
import com.jetbrains.youtrack.db.internal.core.metadata.security.RestrictedOperation;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityShared;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Token;
import com.jetbrains.youtrack.db.internal.core.metadata.security.PropertyAccess;
import com.jetbrains.youtrack.db.internal.core.metadata.security.PropertyEncryptionNone;
import com.jetbrains.youtrack.db.internal.core.metadata.security.ImmutableUser;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityUser;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityUserIml;
import com.jetbrains.youtrack.db.internal.core.metadata.security.auth.AuthenticationInfo;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.SequenceAction;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.SequenceLibraryProxy;
import com.jetbrains.youtrack.db.internal.core.query.live.LiveQueryHook;
import com.jetbrains.youtrack.db.internal.core.query.live.LiveQueryHookV2;
import com.jetbrains.youtrack.db.internal.core.query.live.LiveQueryListenerV2;
import com.jetbrains.youtrack.db.internal.core.query.live.YTLiveQueryMonitorEmbedded;
import com.jetbrains.youtrack.db.internal.core.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.DocumentInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EdgeEntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.VertexInternal;
import com.jetbrains.youtrack.db.internal.core.schedule.ScheduledEvent;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializerFactory;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.executor.LiveQueryListenerImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.parser.LocalResultSetLifecycleDecorator;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.LocalResultSet;
import com.jetbrains.youtrack.db.internal.core.storage.RecordMetadata;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.StorageInfo;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.FreezableStorageComponent;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.SBTreeCollectionManager;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionAbstract;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionNoTx;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionNoTx.NonTxReadMode;
import com.jetbrains.youtrack.db.internal.core.tx.TransactionOptimistic;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class DatabaseSessionEmbedded extends DatabaseSessionAbstract
    implements QueryLifecycleListener {

  private YouTrackDBConfig config;
  private Storage storage;

  private FrontendTransactionNoTx.NonTxReadMode nonTxReadMode;

  public DatabaseSessionEmbedded(final Storage storage) {
    activateOnCurrentThread();

    try {
      status = STATUS.CLOSED;

      try {
        var cfg = storage.getConfiguration();
        if (cfg != null) {
          var ctx = cfg.getContextConfiguration();
          if (ctx != null) {
            nonTxReadMode =
                FrontendTransactionNoTx.NonTxReadMode.valueOf(
                    ctx.getValueAsString(GlobalConfiguration.NON_TX_READS_WARNING_MODE));
          } else {
            nonTxReadMode = NonTxReadMode.WARN;
          }
        } else {
          nonTxReadMode = NonTxReadMode.WARN;
        }
      } catch (Exception e) {
        LogManager.instance()
            .warn(
                this,
                "Invalid value for %s, using %s",
                e,
                GlobalConfiguration.NON_TX_READS_WARNING_MODE.getKey(),
                NonTxReadMode.WARN);
        nonTxReadMode = NonTxReadMode.WARN;
      }

      // OVERWRITE THE URL
      url = storage.getURL();
      this.storage = storage;
      this.componentsFactory = storage.getComponentsFactory();

      unmodifiableHooks = Collections.unmodifiableMap(hooks);

      localCache = new LocalRecordCache();

      init();

      databaseOwner = this;

    } catch (Exception t) {
      DatabaseRecordThreadLocal.instance().remove();

      throw BaseException.wrapException(new DatabaseException("Error on opening database "), t);
    }
  }

  public DatabaseSession open(final String iUserName, final String iUserPassword) {
    throw new UnsupportedOperationException("Use YouTrackDB");
  }

  public void init(YouTrackDBConfig config, SharedContext sharedContext) {
    this.sharedContext = sharedContext;
    activateOnCurrentThread();
    this.config = config;
    applyAttributes(config);
    applyListeners(config);
    try {

      status = STATUS.OPEN;
      if (initialized) {
        return;
      }

      RecordSerializerFactory serializerFactory = RecordSerializerFactory.instance();
      String serializeName = getStorageInfo().getConfiguration().getRecordSerializer();
      if (serializeName == null) {
        throw new DatabaseException(
            "Impossible to open database from version before 2.x use export import instead");
      }
      serializer = serializerFactory.getFormat(serializeName);
      if (serializer == null) {
        throw new DatabaseException(
            "RecordSerializer with name '" + serializeName + "' not found ");
      }
      if (getStorageInfo().getConfiguration().getRecordSerializerVersion()
          > serializer.getMinSupportedVersion()) {
        throw new DatabaseException(
            "Persistent record serializer version is not support by the current implementation");
      }

      localCache.startup();

      loadMetadata();

      installHooksEmbedded();

      user = null;

      initialized = true;
    } catch (BaseException e) {
      DatabaseRecordThreadLocal.instance().remove();
      throw e;
    } catch (Exception e) {
      DatabaseRecordThreadLocal.instance().remove();
      throw BaseException.wrapException(
          new DatabaseException("Cannot open database url=" + getURL()), e);
    }
  }

  public void internalOpen(final AuthenticationInfo authenticationInfo) {
    try {
      SecurityInternal security = sharedContext.getSecurity();

      if (user == null || user.getVersion() != security.getVersion(this)) {
        final SecurityUser usr;

        usr = security.securityAuthenticate(this, authenticationInfo);
        if (usr != null) {
          user = new ImmutableUser(this, security.getVersion(this), usr);
        } else {
          user = null;
        }

        checkSecurity(Rule.ResourceGeneric.DATABASE, Role.PERMISSION_READ);
      }

    } catch (BaseException e) {
      DatabaseRecordThreadLocal.instance().remove();
      throw e;
    } catch (Exception e) {
      DatabaseRecordThreadLocal.instance().remove();
      throw BaseException.wrapException(
          new DatabaseException("Cannot open database url=" + getURL()), e);
    }
  }

  public void internalOpen(final String iUserName, final String iUserPassword) {
    internalOpen(iUserName, iUserPassword, true);
  }

  public void internalOpen(
      final String iUserName, final String iUserPassword, boolean checkPassword) {
    executeInTx(
        () -> {
          try {
            SecurityInternal security = sharedContext.getSecurity();

            if (user == null
                || user.getVersion() != security.getVersion(this)
                || !user.getName(this).equalsIgnoreCase(iUserName)) {
              final SecurityUser usr;

              if (checkPassword) {
                usr = security.securityAuthenticate(this, iUserName, iUserPassword);
              } else {
                usr = security.getUser(this, iUserName);
              }
              if (usr != null) {
                user = new ImmutableUser(this, security.getVersion(this), usr);
              } else {
                user = null;
              }

              checkSecurity(Rule.ResourceGeneric.DATABASE, Role.PERMISSION_READ);
            }
          } catch (BaseException e) {
            DatabaseRecordThreadLocal.instance().remove();
            throw e;
          } catch (Exception e) {
            DatabaseRecordThreadLocal.instance().remove();
            throw BaseException.wrapException(
                new DatabaseException("Cannot open database url=" + getURL()), e);
          }
        });
  }

  private void applyListeners(YouTrackDBConfig config) {
    if (config != null) {
      for (DatabaseListener listener : config.getListeners()) {
        registerListener(listener);
      }
    }
  }

  /**
   * Opens a database using an authentication token received as an argument.
   *
   * @param iToken Authentication token
   * @return The Database instance itself giving a "fluent interface". Useful to call multiple
   * methods in chain.
   */
  @Deprecated
  public DatabaseSession open(final Token iToken) {
    throw new UnsupportedOperationException("Deprecated Method");
  }

  @Override
  public DatabaseSession create() {
    throw new UnsupportedOperationException("Deprecated Method");
  }

  /**
   * {@inheritDoc}
   */
  public void internalCreate(YouTrackDBConfig config, SharedContext ctx) {
    RecordSerializer serializer = RecordSerializerFactory.instance().getDefaultRecordSerializer();
    if (serializer.toString().equals("ORecordDocument2csv")) {
      throw new DatabaseException(
          "Impossible to create the database with ORecordDocument2csv serializer");
    }
    storage.setRecordSerializer(serializer.toString(), serializer.getCurrentVersion());
    storage.setProperty(SQLStatement.CUSTOM_STRICT_SQL, "true");

    this.setSerializer(serializer);

    this.sharedContext = ctx;
    this.status = STATUS.OPEN;
    // THIS IF SHOULDN'T BE NEEDED, CREATE HAPPEN ONLY IN EMBEDDED
    applyAttributes(config);
    applyListeners(config);
    metadata = new MetadataDefault(this);
    installHooksEmbedded();
    createMetadata(ctx);
  }

  public void callOnCreateListeners() {
    // WAKE UP DB LIFECYCLE LISTENER
    for (Iterator<DatabaseLifecycleListener> it = YouTrackDBManager.instance()
        .getDbLifecycleListeners();
        it.hasNext(); ) {
      it.next().onCreate(getDatabaseOwner());
    }

    // WAKE UP LISTENERS
    for (DatabaseListener listener : browseListeners()) {
      try {
        listener.onCreate(this);
      } catch (Exception ignore) {
      }
    }
  }

  protected void createMetadata(SharedContext shared) {
    metadata.init(shared);
    ((SharedContextEmbedded) shared).create(this);
  }

  @Override
  protected void loadMetadata() {
    executeInTx(
        () -> {
          metadata = new MetadataDefault(this);
          metadata.init(sharedContext);
          sharedContext.load(this);
        });
  }

  private void applyAttributes(YouTrackDBConfig config) {
    if (config != null) {
      for (Entry<ATTRIBUTES, Object> attrs : config.getAttributes().entrySet()) {
        this.set(attrs.getKey(), attrs.getValue());
      }
    }
  }

  @Override
  public void set(final ATTRIBUTES iAttribute, final Object iValue) {
    checkIfActive();

    if (iAttribute == null) {
      throw new IllegalArgumentException("attribute is null");
    }

    final String stringValue = IOUtils.getStringContent(iValue != null ? iValue.toString() : null);
    final Storage storage = this.storage;
    switch (iAttribute) {
      case STATUS:
        if (stringValue == null) {
          throw new IllegalArgumentException("DB status can't be null");
        }
        setStatus(STATUS.valueOf(stringValue.toUpperCase(Locale.ENGLISH)));
        break;

      case DEFAULTCLUSTERID:
        if (iValue != null) {
          if (iValue instanceof Number) {
            storage.setDefaultClusterId(((Number) iValue).intValue());
          } else {
            storage.setDefaultClusterId(storage.getClusterIdByName(iValue.toString()));
          }
        }
        break;

      case TYPE:
        throw new IllegalArgumentException("Database type cannot be changed at run-time");

      case DATEFORMAT:
        if (stringValue == null) {
          throw new IllegalArgumentException("date format is null");
        }

        // CHECK FORMAT
        new SimpleDateFormat(stringValue).format(new Date());

        storage.setDateFormat(stringValue);
        break;

      case DATETIMEFORMAT:
        if (stringValue == null) {
          throw new IllegalArgumentException("date format is null");
        }

        // CHECK FORMAT
        new SimpleDateFormat(stringValue).format(new Date());

        storage.setDateTimeFormat(stringValue);
        break;

      case TIMEZONE:
        if (stringValue == null) {
          throw new IllegalArgumentException("Timezone can't be null");
        }

        // for backward compatibility, until 2.1.13 YouTrackDB accepted timezones in lowercase as well
        TimeZone timeZoneValue = TimeZone.getTimeZone(stringValue.toUpperCase(Locale.ENGLISH));
        if (timeZoneValue.equals(TimeZone.getTimeZone("GMT"))) {
          timeZoneValue = TimeZone.getTimeZone(stringValue);
        }

        storage.setTimeZone(timeZoneValue);
        break;

      case LOCALECOUNTRY:
        storage.setLocaleCountry(stringValue);
        break;

      case LOCALELANGUAGE:
        storage.setLocaleLanguage(stringValue);
        break;

      case CHARSET:
        storage.setCharset(stringValue);
        break;

      case CUSTOM:
        int indx = stringValue != null ? stringValue.indexOf('=') : -1;
        if (indx < 0) {
          if ("clear".equalsIgnoreCase(stringValue)) {
            clearCustomInternal();
          } else {
            throw new IllegalArgumentException(
                "Syntax error: expected <name> = <value> or clear, instead found: " + iValue);
          }
        } else {
          String customName = stringValue.substring(0, indx).trim();
          String customValue = stringValue.substring(indx + 1).trim();
          if (customValue.isEmpty()) {
            removeCustomInternal(customName);
          } else {
            setCustomInternal(customName, customValue);
          }
        }
        break;

      case CLUSTERSELECTION:
        storage.setClusterSelection(stringValue);
        break;

      case MINIMUMCLUSTERS:
        if (iValue != null) {
          if (iValue instanceof Number) {
            storage.setMinimumClusters(((Number) iValue).intValue());
          } else {
            storage.setMinimumClusters(Integer.parseInt(stringValue));
          }
        } else
        // DEFAULT = 1
        {
          storage.setMinimumClusters(1);
        }

        break;

      case CONFLICTSTRATEGY:
        storage.setConflictStrategy(
            YouTrackDBManager.instance().getRecordConflictStrategy().getStrategy(stringValue));
        break;

      case VALIDATION:
        storage.setValidation(Boolean.parseBoolean(stringValue));
        break;

      default:
        throw new IllegalArgumentException(
            "Option '" + iAttribute + "' not supported on alter database");
    }
  }

  private void clearCustomInternal() {
    storage.clearProperties();
  }

  private void removeCustomInternal(final String iName) {
    setCustomInternal(iName, null);
  }

  private void setCustomInternal(final String iName, final String iValue) {
    final Storage storage = this.storage;
    if (iValue == null || "null".equalsIgnoreCase(iValue))
    // REMOVE
    {
      storage.removeProperty(iName);
    } else
    // SET
    {
      storage.setProperty(iName, iValue);
    }
  }

  public DatabaseSession setCustom(final String name, final Object iValue) {
    checkIfActive();

    if ("clear".equalsIgnoreCase(name) && iValue == null) {
      clearCustomInternal();
    } else {
      String customName = name;
      String customValue = iValue == null ? null : "" + iValue;
      if (customName == null || customValue.isEmpty()) {
        removeCustomInternal(customName);
      } else {
        setCustomInternal(customName, customValue);
      }
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseSession create(String incrementalBackupPath) {
    throw new UnsupportedOperationException("use YouTrackDB");
  }

  @Override
  public DatabaseSession create(final Map<GlobalConfiguration, Object> iInitialSettings) {
    throw new UnsupportedOperationException("use YouTrackDB");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void drop() {
    throw new UnsupportedOperationException("use YouTrackDB");
  }

  /**
   * Returns a copy of current database if it's open. The returned instance can be used by another
   * thread without affecting current instance. The database copy is not set in thread local.
   */
  public DatabaseSessionInternal copy() {
    var storage = (Storage) getSharedContext().getStorage();
    storage.open(this, null, null, config.getConfigurations());
    DatabaseSessionEmbedded database = new DatabaseSessionEmbedded(storage);
    database.init(config, this.sharedContext);
    String user;
    if (getUser() != null) {
      user = getUser().getName(this);
    } else {
      user = null;
    }

    database.internalOpen(user, null, false);
    database.callOnOpenListeners();
    this.activateOnCurrentThread();
    return database;
  }

  @Override
  public boolean exists() {
    throw new UnsupportedOperationException("use YouTrackDB");
  }

  @Override
  public boolean isClosed() {
    return status == STATUS.CLOSED || storage.isClosed(this);
  }

  public void rebuildIndexes() {
    if (metadata.getIndexManagerInternal().autoRecreateIndexesAfterCrash(this)) {
      metadata.getIndexManagerInternal().recreateIndexes(this);
    }
  }

  protected void installHooksEmbedded() {
    hooks.clear();
  }

  @Override
  public Storage getStorage() {
    return storage;
  }

  @Override
  public StorageInfo getStorageInfo() {
    return storage;
  }

  @Override
  public void replaceStorage(Storage iNewStorage) {
    this.getSharedContext().setStorage(iNewStorage);
    storage = iNewStorage;
  }

  @Override
  public ResultSet query(String query, Object[] args) {
    checkOpenness();
    checkIfActive();
    getSharedContext().getYouTrackDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      SQLStatement statement = SQLEngine.parse(query, this);
      if (!statement.isIdempotent()) {
        throw new CommandExecutionException(
            "Cannot execute query on non idempotent statement: " + query);
      }
      ResultSet original = statement.execute(this, args, true);
      LocalResultSetLifecycleDecorator result = new LocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getYouTrackDB().endCommand();
    }
  }

  @Override
  public ResultSet query(String query, Map args) {
    checkOpenness();
    checkIfActive();
    getSharedContext().getYouTrackDB().startCommand(Optional.empty());
    preQueryStart();
    try {
      SQLStatement statement = SQLEngine.parse(query, this);
      if (!statement.isIdempotent()) {
        throw new CommandExecutionException(
            "Cannot execute query on non idempotent statement: " + query);
      }
      ResultSet original = statement.execute(this, args, true);
      LocalResultSetLifecycleDecorator result = new LocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getYouTrackDB().endCommand();
    }
  }

  @Override
  public ResultSet command(String query, Object[] args) {
    checkOpenness();
    checkIfActive();

    getSharedContext().getYouTrackDB().startCommand(Optional.empty());
    preQueryStart();
    try {
      SQLStatement statement = SQLEngine.parse(query, this);
      ResultSet original = statement.execute(this, args, true);
      LocalResultSetLifecycleDecorator result;
      if (!statement.isIdempotent()) {
        // fetch all, close and detach
        InternalResultSet prefetched = new InternalResultSet();
        original.forEachRemaining(x -> prefetched.add(x));
        original.close();
        queryCompleted();
        result = new LocalResultSetLifecycleDecorator(prefetched);
      } else {
        // stream, keep open and attach to the current DB
        result = new LocalResultSetLifecycleDecorator(original);
        queryStarted(result);
      }
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getYouTrackDB().endCommand();
    }
  }

  @Override
  public ResultSet command(String query, Map args) {
    checkOpenness();
    checkIfActive();

    getSharedContext().getYouTrackDB().startCommand(Optional.empty());
    try {
      preQueryStart();

      SQLStatement statement = SQLEngine.parse(query, this);
      ResultSet original = statement.execute(this, args, true);
      LocalResultSetLifecycleDecorator result;
      if (!statement.isIdempotent()) {
        // fetch all, close and detach
        InternalResultSet prefetched = new InternalResultSet();
        original.forEachRemaining(x -> prefetched.add(x));
        original.close();
        queryCompleted();
        result = new LocalResultSetLifecycleDecorator(prefetched);
      } else {
        // stream, keep open and attach to the current DB
        result = new LocalResultSetLifecycleDecorator(original);

        queryStarted(result);
      }

      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getYouTrackDB().endCommand();
    }
  }

  @Override
  public ResultSet execute(String language, String script, Object... args) {
    checkOpenness();
    checkIfActive();
    if (!"sql".equalsIgnoreCase(language)) {
      checkSecurity(Rule.ResourceGeneric.COMMAND, Role.PERMISSION_EXECUTE, language);
    }
    getSharedContext().getYouTrackDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      ScriptExecutor executor =
          getSharedContext()
              .getYouTrackDB()
              .getScriptManager()
              .getCommandManager()
              .getScriptExecutor(language);

      ((AbstractPaginatedStorage) this.storage).pauseConfigurationUpdateNotifications();
      ResultSet original;
      try {
        original = executor.execute(this, script, args);
      } finally {
        ((AbstractPaginatedStorage) this.storage).fireConfigurationUpdateNotifications();
      }
      LocalResultSetLifecycleDecorator result = new LocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getYouTrackDB().endCommand();
    }
  }

  private void cleanQueryState() {
    this.queryState.pop();
  }

  private void queryCompleted() {
    QueryDatabaseState state = this.queryState.peekLast();
    state.closeInternal(this);
  }

  private void queryStarted(LocalResultSetLifecycleDecorator result) {
    QueryDatabaseState state = this.queryState.peekLast();
    state.setResultSet(result);
    this.queryStarted(result.getQueryId(), state);
    result.addLifecycleListener(this);
  }

  private void preQueryStart() {
    this.queryState.push(new QueryDatabaseState());
  }

  @Override
  public ResultSet execute(String language, String script, Map<String, ?> args) {
    checkOpenness();
    checkIfActive();
    if (!"sql".equalsIgnoreCase(language)) {
      checkSecurity(Rule.ResourceGeneric.COMMAND, Role.PERMISSION_EXECUTE, language);
    }
    getSharedContext().getYouTrackDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      ScriptExecutor executor =
          sharedContext
              .getYouTrackDB()
              .getScriptManager()
              .getCommandManager()
              .getScriptExecutor(language);
      ResultSet original;

      ((AbstractPaginatedStorage) this.storage).pauseConfigurationUpdateNotifications();
      try {
        original = executor.execute(this, script, args);
      } finally {
        ((AbstractPaginatedStorage) this.storage).fireConfigurationUpdateNotifications();
      }

      LocalResultSetLifecycleDecorator result = new LocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getYouTrackDB().endCommand();
    }
  }

  public LocalResultSetLifecycleDecorator query(ExecutionPlan plan, Map<Object, Object> params) {
    checkOpenness();
    checkIfActive();
    getSharedContext().getYouTrackDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      BasicCommandContext ctx = new BasicCommandContext();
      ctx.setDatabase(this);
      ctx.setInputParameters(params);

      LocalResultSet result = new LocalResultSet((InternalExecutionPlan) plan);
      LocalResultSetLifecycleDecorator decorator = new LocalResultSetLifecycleDecorator(result);
      queryStarted(decorator);

      return decorator;
    } finally {
      cleanQueryState();
      getSharedContext().getYouTrackDB().endCommand();
    }
  }

  public void queryStartUsingViewCluster(int clusterId) {
    SharedContext sharedContext = getSharedContext();
    ViewManager viewManager = sharedContext.getViewManager();
    viewManager.startUsingViewCluster(clusterId);
    this.queryState.peekLast().addViewUseCluster(clusterId);
  }

  public void queryStartUsingViewIndex(String index) {
    SharedContext sharedContext = getSharedContext();
    ViewManager viewManager = sharedContext.getViewManager();
    viewManager.startUsingViewIndex(index);
    this.queryState.peekLast().addViewUseIndex(index);
  }

  @Override
  public void queryStarted(String id, ResultSet resultSet) {
    // to nothing just compatibility
  }

  public YouTrackDBConfig getConfig() {
    return config;
  }

  @Override
  public LiveQueryMonitor live(String query, LiveQueryResultListener listener, Object... args) {
    checkOpenness();
    checkIfActive();

    LiveQueryListenerV2 queryListener = new LiveQueryListenerImpl(listener, query, this, args);
    DatabaseSessionInternal dbCopy = this.copy();
    this.activateOnCurrentThread();
    LiveQueryMonitor monitor = new YTLiveQueryMonitorEmbedded(queryListener.getToken(), dbCopy);
    return monitor;
  }

  @Override
  public LiveQueryMonitor live(
      String query, LiveQueryResultListener listener, Map<String, ?> args) {
    checkOpenness();
    checkIfActive();

    LiveQueryListenerV2 queryListener =
        new LiveQueryListenerImpl(listener, query, this, (Map) args);
    DatabaseSessionInternal dbCopy = this.copy();
    this.activateOnCurrentThread();
    LiveQueryMonitor monitor = new YTLiveQueryMonitorEmbedded(queryListener.getToken(), dbCopy);
    return monitor;
  }

  @Override
  public void recycle(final Record record) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addBlobCluster(final String iClusterName, final Object... iParameters) {
    int id;
    if (!existsCluster(iClusterName)) {
      id = addCluster(iClusterName, iParameters);
    } else {
      id = getClusterIdByName(iClusterName);
    }
    getMetadata().getSchema().addBlobCluster(id);
    return id;
  }

  @Override
  public Identifiable beforeCreateOperations(Identifiable id, String iClusterName) {
    checkSecurity(Role.PERMISSION_CREATE, id, iClusterName);

    RecordHook.RESULT triggerChanged = null;
    boolean changed = false;
    if (id instanceof EntityImpl doc) {

      if (!getSharedContext().getSecurity().canCreate(this, doc)) {
        throw new SecurityException(
            "Cannot update record "
                + doc
                + ": the resource has restricted access due to security policies");
      }

      SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_CREATE, clazz.getName());
        if (clazz.isScheduler()) {
          getSharedContext().getScheduler().initScheduleRecord(this, doc);
          changed = true;
        }
        if (clazz.isOuser()) {
          doc.validate();
          changed = SecurityUserIml.encodePassword(this, doc);
        }
        if (clazz.isTriggered()) {
          triggerChanged = ClassTrigger.onRecordBeforeCreate(doc, this);
        }
        if (clazz.isRestricted()) {
          changed = RestrictedAccessHook.onRecordBeforeCreate(doc, this);
        }
        if (clazz.isFunction()) {
          FunctionLibraryImpl.validateFunctionRecord(doc);
        }
        DocumentInternal.setPropertyEncryption(doc, PropertyEncryptionNone.instance());
      }
    }

    RecordHook.RESULT res = callbackHooks(RecordHook.TYPE.BEFORE_CREATE, id);
    if (changed
        || res == RecordHook.RESULT.RECORD_CHANGED
        || triggerChanged == RecordHook.RESULT.RECORD_CHANGED) {
      if (id instanceof EntityImpl) {
        ((EntityImpl) id).validate();
      }
      return id;
    } else {
      if (res == RecordHook.RESULT.RECORD_REPLACED
          || triggerChanged == RecordHook.RESULT.RECORD_REPLACED) {
        Record replaced = HookReplacedRecordThreadLocal.INSTANCE.get();
        if (replaced instanceof EntityImpl) {
          ((EntityImpl) replaced).validate();
        }
        return replaced;
      }
    }
    return null;
  }

  @Override
  public Identifiable beforeUpdateOperations(Identifiable id, String iClusterName) {
    checkSecurity(Role.PERMISSION_UPDATE, id, iClusterName);

    RecordHook.RESULT triggerChanged = null;
    boolean changed = false;
    if (id instanceof EntityImpl doc) {
      SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isScheduler()) {
          getSharedContext().getScheduler().preHandleUpdateScheduleInTx(this, doc);
          changed = true;
        }
        if (clazz.isOuser()) {
          changed = SecurityUserIml.encodePassword(this, doc);
        }
        if (clazz.isTriggered()) {
          triggerChanged = ClassTrigger.onRecordBeforeUpdate(doc, this);
        }
        if (clazz.isRestricted()) {
          if (!RestrictedAccessHook.isAllowed(
              this, doc, RestrictedOperation.ALLOW_UPDATE, true)) {
            throw new SecurityException(
                "Cannot update record "
                    + doc.getIdentity()
                    + ": the resource has restricted access");
          }
        }
        if (clazz.isFunction()) {
          FunctionLibraryImpl.validateFunctionRecord(doc);
        }
        if (!getSharedContext().getSecurity().canUpdate(this, doc)) {
          throw new SecurityException(
              "Cannot update record "
                  + doc.getIdentity()
                  + ": the resource has restricted access due to security policies");
        }
        DocumentInternal.setPropertyEncryption(doc, PropertyEncryptionNone.instance());
      }
    }
    RecordHook.RESULT res = callbackHooks(RecordHook.TYPE.BEFORE_UPDATE, id);
    if (res == RecordHook.RESULT.RECORD_CHANGED
        || triggerChanged == RecordHook.RESULT.RECORD_CHANGED) {
      if (id instanceof EntityImpl) {
        ((EntityImpl) id).validate();
      }
      return id;
    } else {
      if (res == RecordHook.RESULT.RECORD_REPLACED
          || triggerChanged == RecordHook.RESULT.RECORD_REPLACED) {
        Record replaced = HookReplacedRecordThreadLocal.INSTANCE.get();
        if (replaced instanceof EntityImpl) {
          ((EntityImpl) replaced).validate();
        }
        return replaced;
      }
    }

    if (changed) {
      return id;
    }
    return null;
  }

  /**
   * Deletes a document. Behavior depends by the current running transaction if any. If no
   * transaction is running then the record is deleted immediately. If an Optimistic transaction is
   * running then the record will be deleted at commit time. The current transaction will continue
   * to see the record as deleted, while others not. If a Pessimistic transaction is running, then
   * an exclusive lock is acquired against the record. Current transaction will continue to see the
   * record as deleted, while others cannot access to it since it's locked.
   *
   * <p>If MVCC is enabled and the version of the document is different by the version stored in
   * the database, then a {@link ConcurrentModificationException} exception is thrown.
   *
   * @param record record to delete
   */
  public void delete(Record record) {
    checkOpenness();

    if (record == null) {
      throw new DatabaseException("Cannot delete null document");
    }

    if (record instanceof Entity) {
      if (((Entity) record).isVertex()) {
        VertexInternal.deleteLinks(((Entity) record).toVertex());
      } else {
        if (((Entity) record).isEdge()) {
          EdgeEntityImpl.deleteLinks(((Entity) record).toEdge());
        }
      }
    }

    // CHECK ACCESS ON SCHEMA CLASS NAME (IF ANY)
    if (record instanceof EntityImpl && ((EntityImpl) record).getClassName() != null) {
      checkSecurity(
          Rule.ResourceGeneric.CLASS,
          Role.PERMISSION_DELETE,
          ((EntityImpl) record).getClassName());
    }

    try {
      currentTx.deleteRecord((RecordAbstract) record);
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      if (record instanceof EntityImpl) {
        throw BaseException.wrapException(
            new DatabaseException(
                "Error on deleting record "
                    + record.getIdentity()
                    + " of class '"
                    + ((EntityImpl) record).getClassName()
                    + "'"),
            e);
      } else {
        throw BaseException.wrapException(
            new DatabaseException("Error on deleting record " + record.getIdentity()), e);
      }
    }
  }

  @Override
  public void beforeDeleteOperations(Identifiable id, String iClusterName) {
    checkSecurity(Role.PERMISSION_DELETE, id, iClusterName);
    if (id instanceof EntityImpl doc) {
      SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isTriggered()) {
          ClassTrigger.onRecordBeforeDelete(doc, this);
        }
        if (clazz.isRestricted()) {
          if (!RestrictedAccessHook.isAllowed(
              this, doc, RestrictedOperation.ALLOW_DELETE, true)) {
            throw new SecurityException(
                "Cannot delete record "
                    + doc.getIdentity()
                    + ": the resource has restricted access");
          }
        }
        if (!getSharedContext().getSecurity().canDelete(this, doc)) {
          throw new SecurityException(
              "Cannot delete record "
                  + doc.getIdentity()
                  + ": the resource has restricted access due to security policies");
        }
      }
    }
    callbackHooks(RecordHook.TYPE.BEFORE_DELETE, id);
  }

  public void afterCreateOperations(final Identifiable id) {
    if (id instanceof EntityImpl doc) {
      final SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);

      if (clazz != null) {
        ClassIndexManager.checkIndexesAfterCreate(doc, this);
        if (clazz.isFunction()) {
          this.getSharedContext().getFunctionLibrary().createdFunction(doc);
        }
        if (clazz.isOuser() || clazz.isOrole() || clazz.isSecurityPolicy()) {
          sharedContext.getSecurity().incrementVersion(this);
        }
        if (clazz.isTriggered()) {
          ClassTrigger.onRecordAfterCreate(doc, this);
        }
      }

      LiveQueryHook.addOp(doc, RecordOperation.CREATED, this);
      LiveQueryHookV2.addOp(this, doc, RecordOperation.CREATED);
    }

    callbackHooks(RecordHook.TYPE.AFTER_CREATE, id);
  }

  public void afterUpdateOperations(final Identifiable id) {
    if (id instanceof EntityImpl doc) {
      SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        ClassIndexManager.checkIndexesAfterUpdate((EntityImpl) id, this);

        if (clazz.isOuser() || clazz.isOrole() || clazz.isSecurityPolicy()) {
          sharedContext.getSecurity().incrementVersion(this);
        }

        if (clazz.isTriggered()) {
          ClassTrigger.onRecordAfterUpdate(doc, this);
        }

      }

    }
    callbackHooks(RecordHook.TYPE.AFTER_UPDATE, id);
  }

  public void afterDeleteOperations(final Identifiable id) {
    if (id instanceof EntityImpl doc) {
      SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        ClassIndexManager.checkIndexesAfterDelete(doc, this);
        if (clazz.isFunction()) {
          this.getSharedContext().getFunctionLibrary().droppedFunction(doc);
        }
        if (clazz.isSequence()) {
          ((SequenceLibraryProxy) getMetadata().getSequenceLibrary())
              .getDelegate()
              .onSequenceDropped(this, doc);
        }
        if (clazz.isScheduler()) {
          final String eventName = doc.field(ScheduledEvent.PROP_NAME);
          getSharedContext().getScheduler().removeEventInternal(eventName);
        }
        if (clazz.isTriggered()) {
          ClassTrigger.onRecordAfterDelete(doc, this);
        }
        getSharedContext().getViewManager().recordDeleted(clazz, doc, this);
      }
      LiveQueryHook.addOp(doc, RecordOperation.DELETED, this);
      LiveQueryHookV2.addOp(this, doc, RecordOperation.DELETED);
    }
    callbackHooks(RecordHook.TYPE.AFTER_DELETE, id);
  }

  @Override
  public void afterReadOperations(Identifiable identifiable) {
    if (identifiable instanceof EntityImpl doc) {
      SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isTriggered()) {
          ClassTrigger.onRecordAfterRead(doc, this);
        }
      }
    }
    callbackHooks(RecordHook.TYPE.AFTER_READ, identifiable);
  }

  @Override
  public boolean beforeReadOperations(Identifiable identifiable) {
    if (identifiable instanceof EntityImpl doc) {
      SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isTriggered()) {
          RecordHook.RESULT val = ClassTrigger.onRecordBeforeRead(doc, this);
          if (val == RecordHook.RESULT.SKIP) {
            return true;
          }
        }
        if (clazz.isRestricted()) {
          if (!RestrictedAccessHook.isAllowed(this, doc, RestrictedOperation.ALLOW_READ, false)) {
            return true;
          }
        }
        try {
          checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_READ, clazz.getName());
        } catch (SecurityException e) {
          return true;
        }

        if (!getSharedContext().getSecurity().canRead(this, doc)) {
          return true;
        }

        DocumentInternal.setPropertyAccess(
            doc, new PropertyAccess(this, doc, getSharedContext().getSecurity()));
        DocumentInternal.setPropertyEncryption(doc, PropertyEncryptionNone.instance());
      }
    }
    return callbackHooks(RecordHook.TYPE.BEFORE_READ, identifiable) == RecordHook.RESULT.SKIP;
  }

  @Override
  public void afterCommitOperations() {
    for (var operation : currentTx.getRecordOperations()) {
      if (operation.type == RecordOperation.CREATED) {
        var record = operation.record;

        if (record instanceof EntityImpl doc) {
          SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);

          if (clazz != null) {
            if (clazz.isSequence()) {
              ((SequenceLibraryProxy) getMetadata().getSequenceLibrary())
                  .getDelegate()
                  .onSequenceCreated(this, doc);
            }

            if (clazz.isScheduler()) {
              getMetadata().getScheduler().scheduleEvent(this, new ScheduledEvent(doc, this));
            }
          }
        }
      } else if (operation.type == RecordOperation.UPDATED) {
        var record = operation.record;

        if (record instanceof EntityImpl doc) {
          SchemaImmutableClass clazz = DocumentInternal.getImmutableSchemaClass(this, doc);
          if (clazz != null) {
            if (clazz.isFunction()) {
              this.getSharedContext().getFunctionLibrary().updatedFunction(doc);
            }
            if (clazz.isScheduler()) {
              getSharedContext().getScheduler().postHandleUpdateScheduleAfterTxCommit(this, doc);
            }
          }

          LiveQueryHook.addOp(doc, RecordOperation.UPDATED, this);
          LiveQueryHookV2.addOp(this, doc, RecordOperation.UPDATED);
        }
      }
    }

    super.afterCommitOperations();

    LiveQueryHook.notifyForTxChanges(this);
    LiveQueryHookV2.notifyForTxChanges(this);
  }

  @Override
  protected void afterRollbackOperations() {
    super.afterRollbackOperations();
    LiveQueryHook.removePendingDatabaseOps(this);
    LiveQueryHookV2.removePendingDatabaseOps(this);
  }

  public String getClusterName(final Record record) {
    int clusterId = record.getIdentity().getClusterId();
    if (clusterId == RID.CLUSTER_ID_INVALID) {
      // COMPUTE THE CLUSTER ID
      SchemaClass schemaClass = null;
      if (record instanceof EntityImpl) {
        schemaClass = DocumentInternal.getImmutableSchemaClass(this, (EntityImpl) record);
      }
      if (schemaClass != null) {
        // FIND THE RIGHT CLUSTER AS CONFIGURED IN CLASS
        if (schemaClass.isAbstract()) {
          throw new SchemaException(
              "Document belongs to abstract class '"
                  + schemaClass.getName()
                  + "' and cannot be saved");
        }
        clusterId = schemaClass.getClusterForNewInstance((EntityImpl) record);
        return getClusterNameById(clusterId);
      } else {
        return getClusterNameById(storage.getDefaultClusterId());
      }

    } else {
      return getClusterNameById(clusterId);
    }
  }

  @Override
  public SchemaView getViewFromCluster(int cluster) {
    ImmutableSchema schema = getMetadata().getImmutableSchemaSnapshot();
    SchemaView view = schema.getViewByClusterId(cluster);
    if (view == null) {
      String viewName = getSharedContext().getViewManager().getViewFromOldCluster(cluster);
      if (viewName != null) {
        view = schema.getView(viewName);
      }
    }
    return view;
  }

  @Override
  public boolean executeExists(RID rid) {
    checkOpenness();
    checkIfActive();
    try {
      checkSecurity(
          Rule.ResourceGeneric.CLUSTER,
          Role.PERMISSION_READ,
          getClusterNameById(rid.getClusterId()));

      Record record = getTransaction().getRecord(rid);
      if (record == FrontendTransactionAbstract.DELETED_RECORD) {
        // DELETED IN TX
        return false;
      }
      if (record != null) {
        return true;
      }

      if (!rid.isPersistent()) {
        return false;
      }

      if (localCache.findRecord(rid) != null) {
        return true;
      }

      return storage.recordExists(this, rid);
    } catch (Exception t) {
      throw BaseException.wrapException(
          new DatabaseException(
              "Error on retrieving record "
                  + rid
                  + " (cluster: "
                  + storage.getPhysicalClusterNameById(rid.getClusterId())
                  + ")"),
          t);
    }
  }

  @Override
  public <T> T sendSequenceAction(SequenceAction action)
      throws ExecutionException, InterruptedException {
    throw new UnsupportedOperationException(
        "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
  }

  /**
   * {@inheritDoc}
   */
  public void checkSecurity(
      final Rule.ResourceGeneric resourceGeneric,
      final String resourceSpecific,
      final int iOperation) {
    if (user != null) {
      try {
        user.allow(this, resourceGeneric, resourceSpecific, iOperation);
      } catch (SecurityAccessException e) {

        if (LogManager.instance().isDebugEnabled()) {
          LogManager.instance()
              .debug(
                  this,
                  "User '%s' tried to access the reserved resource '%s.%s', operation '%s'",
                  getUser(),
                  resourceGeneric,
                  resourceSpecific,
                  iOperation);
        }

        throw e;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void checkSecurity(
      final Rule.ResourceGeneric iResourceGeneric,
      final int iOperation,
      final Object... iResourcesSpecific) {
    if (iResourcesSpecific == null || iResourcesSpecific.length == 0) {
      checkSecurity(iResourceGeneric, null, iOperation);
    } else {
      for (Object target : iResourcesSpecific) {
        checkSecurity(iResourceGeneric, target == null ? null : target.toString(), iOperation);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void checkSecurity(
      final Rule.ResourceGeneric iResourceGeneric,
      final int iOperation,
      final Object iResourceSpecific) {
    checkOpenness();
    checkSecurity(
        iResourceGeneric,
        iResourceSpecific == null ? null : iResourceSpecific.toString(),
        iOperation);
  }

  @Override
  @Deprecated
  public void checkSecurity(final String iResource, final int iOperation) {
    final String resourceSpecific = Rule.mapLegacyResourceToSpecificResource(iResource);
    final Rule.ResourceGeneric resourceGeneric =
        Rule.mapLegacyResourceToGenericResource(iResource);

    if (resourceSpecific == null || resourceSpecific.equals("*")) {
      checkSecurity(resourceGeneric, null, iOperation);
    }

    checkSecurity(resourceGeneric, resourceSpecific, iOperation);
  }

  @Override
  @Deprecated
  public void checkSecurity(
      final String iResourceGeneric, final int iOperation, final Object iResourceSpecific) {
    final Rule.ResourceGeneric resourceGeneric =
        Rule.mapLegacyResourceToGenericResource(iResourceGeneric);
    if (iResourceSpecific == null || iResourceSpecific.equals("*")) {
      checkSecurity(resourceGeneric, iOperation, (Object) null);
    }

    checkSecurity(resourceGeneric, iOperation, iResourceSpecific);
  }

  @Override
  @Deprecated
  public void checkSecurity(
      final String iResourceGeneric, final int iOperation, final Object... iResourcesSpecific) {
    final Rule.ResourceGeneric resourceGeneric =
        Rule.mapLegacyResourceToGenericResource(iResourceGeneric);
    checkSecurity(resourceGeneric, iOperation, iResourcesSpecific);
  }

  @Override
  public int addCluster(final String iClusterName, final Object... iParameters) {
    checkIfActive();
    return storage.addCluster(this, iClusterName, iParameters);
  }

  @Override
  public int addCluster(final String iClusterName, final int iRequestedId) {
    checkIfActive();
    return storage.addCluster(this, iClusterName, iRequestedId);
  }

  public RecordConflictStrategy getConflictStrategy() {
    checkIfActive();
    return getStorageInfo().getRecordConflictStrategy();
  }

  public DatabaseSessionEmbedded setConflictStrategy(final String iStrategyName) {
    checkIfActive();
    storage.setConflictStrategy(
        YouTrackDBManager.instance().getRecordConflictStrategy().getStrategy(iStrategyName));
    return this;
  }

  public DatabaseSessionEmbedded setConflictStrategy(final RecordConflictStrategy iResolver) {
    checkIfActive();
    storage.setConflictStrategy(iResolver);
    return this;
  }

  @Override
  public long getClusterRecordSizeByName(final String clusterName) {
    checkIfActive();
    try {
      return storage.getClusterRecordsSizeByName(clusterName);
    } catch (Exception e) {
      throw BaseException.wrapException(
          new DatabaseException(
              "Error on reading records size for cluster '" + clusterName + "'"),
          e);
    }
  }

  @Override
  public long getClusterRecordSizeById(final int clusterId) {
    checkIfActive();
    try {
      return storage.getClusterRecordsSizeById(clusterId);
    } catch (Exception e) {
      throw BaseException.wrapException(
          new DatabaseException(
              "Error on reading records size for cluster with id '" + clusterId + "'"),
          e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long countClusterElements(int iClusterId, boolean countTombstones) {
    final String name = getClusterNameById(iClusterId);
    if (name == null) {
      return 0;
    }
    checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_READ, name);
    checkIfActive();
    return storage.count(this, iClusterId, countTombstones);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long countClusterElements(int[] iClusterIds, boolean countTombstones) {
    checkIfActive();
    String name;
    for (int iClusterId : iClusterIds) {
      name = getClusterNameById(iClusterId);
      checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_READ, name);
    }
    return storage.count(this, iClusterIds, countTombstones);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long countClusterElements(final String iClusterName) {
    checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_READ, iClusterName);
    checkIfActive();

    final int clusterId = getClusterIdByName(iClusterName);
    if (clusterId < 0) {
      throw new IllegalArgumentException("Cluster '" + iClusterName + "' was not found");
    }
    return storage.count(this, clusterId);
  }

  @Override
  public boolean dropCluster(final String iClusterName) {
    checkIfActive();
    final int clusterId = getClusterIdByName(iClusterName);
    SchemaProxy schema = metadata.getSchema();
    SchemaClass clazz = schema.getClassByClusterId(clusterId);
    if (clazz != null) {
      clazz.removeClusterId(this, clusterId);
    }
    if (schema.getBlobClusters().contains(clusterId)) {
      schema.removeBlobCluster(iClusterName);
    }
    getLocalCache().freeCluster(clusterId);
    checkForClusterPermissions(iClusterName);
    return dropClusterInternal(iClusterName);
  }

  protected boolean dropClusterInternal(final String iClusterName) {
    return storage.dropCluster(this, iClusterName);
  }

  @Override
  public boolean dropCluster(final int clusterId) {
    checkIfActive();

    checkSecurity(
        Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_DELETE, getClusterNameById(clusterId));

    SchemaProxy schema = metadata.getSchema();
    final SchemaClass clazz = schema.getClassByClusterId(clusterId);
    if (clazz != null) {
      clazz.removeClusterId(this, clusterId);
    }
    getLocalCache().freeCluster(clusterId);
    if (schema.getBlobClusters().contains(clusterId)) {
      schema.removeBlobCluster(getClusterNameById(clusterId));
    }

    checkForClusterPermissions(getClusterNameById(clusterId));

    final String clusterName = getClusterNameById(clusterId);
    if (clusterName == null) {
      return false;
    }

    final RecordIteratorCluster<Record> iteratorCluster = browseCluster(clusterName);
    if (iteratorCluster == null) {
      return false;
    }

    executeInTxBatches((Iterator<Record>) iteratorCluster, (session, record) -> delete(record));

    return dropClusterInternal(clusterId);
  }

  public boolean dropClusterInternal(int clusterId) {
    return storage.dropCluster(this, clusterId);
  }

  @Override
  public long getSize() {
    checkIfActive();
    return storage.getSize(this);
  }

  public DatabaseStats getStats() {
    DatabaseStats stats = new DatabaseStats();
    stats.loadedRecords = loadedRecordsCount;
    stats.minLoadRecordTimeMs = minRecordLoadMs;
    stats.maxLoadRecordTimeMs = minRecordLoadMs;
    stats.averageLoadRecordTimeMs =
        loadedRecordsCount == 0 ? 0 : (this.totalRecordLoadMs / loadedRecordsCount);

    stats.prefetchedRidbagsCount = ridbagPrefetchCount;
    stats.minRidbagPrefetchTimeMs = minRidbagPrefetchMs;
    stats.maxRidbagPrefetchTimeMs = maxRidbagPrefetchMs;
    stats.ridbagPrefetchTimeMs = totalRidbagPrefetchMs;
    return stats;
  }

  public void addRidbagPrefetchStats(long execTimeMs) {
    this.ridbagPrefetchCount++;
    totalRidbagPrefetchMs += execTimeMs;
    if (this.ridbagPrefetchCount == 1) {
      this.minRidbagPrefetchMs = execTimeMs;
      this.maxRidbagPrefetchMs = execTimeMs;
    } else {
      this.minRidbagPrefetchMs = Math.min(this.minRidbagPrefetchMs, execTimeMs);
      this.maxRidbagPrefetchMs = Math.max(this.maxRidbagPrefetchMs, execTimeMs);
    }
  }

  public void resetRecordLoadStats() {
    this.loadedRecordsCount = 0L;
    this.totalRecordLoadMs = 0L;
    this.minRecordLoadMs = 0L;
    this.maxRecordLoadMs = 0L;
    this.ridbagPrefetchCount = 0L;
    this.totalRidbagPrefetchMs = 0L;
    this.minRidbagPrefetchMs = 0L;
    this.maxRidbagPrefetchMs = 0L;
  }

  @Override
  public String incrementalBackup(final String path) throws UnsupportedOperationException {
    checkOpenness();
    checkIfActive();
    checkSecurity(Rule.ResourceGeneric.DATABASE, "backup", Role.PERMISSION_EXECUTE);

    return storage.incrementalBackup(this, path, null);
  }

  @Override
  public RecordMetadata getRecordMetadata(final RID rid) {
    checkIfActive();
    return storage.getRecordMetadata(this, rid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void freeze(final boolean throwException) {
    checkOpenness();
    if (!(storage instanceof FreezableStorageComponent)) {
      LogManager.instance()
          .error(
              this,
              "Only local paginated storage supports freeze. If you are using remote client please"
                  + " use OServerAdmin instead",
              null);

      return;
    }

    final long startTime = YouTrackDBManager.instance().getProfiler().startChrono();

    final FreezableStorageComponent storage = getFreezableStorage();
    if (storage != null) {
      storage.freeze(throwException);
    }

    YouTrackDBManager.instance()
        .getProfiler()
        .stopChrono(
            "db." + getName() + ".freeze", "Time to freeze the database", startTime, "db.*.freeze");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void freeze() {
    freeze(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    checkOpenness();
    if (!(storage instanceof FreezableStorageComponent)) {
      LogManager.instance()
          .error(
              this,
              "Only local paginated storage supports release. If you are using remote client please"
                  + " use OServerAdmin instead",
              null);
      return;
    }

    final long startTime = YouTrackDBManager.instance().getProfiler().startChrono();

    final FreezableStorageComponent storage = getFreezableStorage();
    if (storage != null) {
      storage.release();
    }

    YouTrackDBManager.instance()
        .getProfiler()
        .stopChrono(
            "db." + getName() + ".release",
            "Time to release the database",
            startTime,
            "db.*.release");
  }

  private FreezableStorageComponent getFreezableStorage() {
    Storage s = storage;
    if (s instanceof FreezableStorageComponent) {
      return (FreezableStorageComponent) s;
    } else {
      LogManager.instance()
          .error(
              this, "Storage of type " + s.getType() + " does not support freeze operation", null);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public SBTreeCollectionManager getSbTreeCollectionManager() {
    return storage.getSBtreeCollectionManager();
  }

  @Override
  public void reload() {
    checkIfActive();

    if (this.isClosed()) {
      throw new DatabaseException("Cannot reload a closed db");
    }
    metadata.reload();
    storage.reload(this);
  }

  @Override
  public void internalCommit(TransactionOptimistic transaction) {
    this.storage.commit(transaction);
  }

  public void internalClose(boolean recycle) {
    if (status != STATUS.OPEN) {
      return;
    }

    checkIfActive();

    try {
      closeActiveQueries();
      localCache.shutdown();

      if (isClosed()) {
        status = STATUS.CLOSED;
        return;
      }

      try {
        rollback(true);
      } catch (Exception e) {
        LogManager.instance().error(this, "Exception during rollback of active transaction", e);
      }

      callOnCloseListeners();

      status = STATUS.CLOSED;
      if (!recycle) {
        sharedContext = null;

        if (storage != null) {
          storage.close(this);
        }
      }

    } finally {
      // ALWAYS RESET TL
      DatabaseRecordThreadLocal.instance().remove();
    }
  }

  @Override
  public long[] getClusterDataRange(int currentClusterId) {
    return storage.getClusterDataRange(this, currentClusterId);
  }

  @Override
  public void setDefaultClusterId(int addCluster) {
    storage.setDefaultClusterId(addCluster);
  }

  @Override
  public long getLastClusterPosition(int clusterId) {
    return storage.getLastClusterPosition(clusterId);
  }

  @Override
  public String getClusterRecordConflictStrategy(int clusterId) {
    return storage.getClusterRecordConflictStrategy(clusterId);
  }

  @Override
  public int[] getClustersIds(Set<String> filterClusters) {
    checkIfActive();
    return storage.getClustersIds(filterClusters);
  }

  public void startExclusiveMetadataChange() {
    ((AbstractPaginatedStorage) storage).startDDL();
  }

  public void endExclusiveMetadataChange() {
    ((AbstractPaginatedStorage) storage).endDDL();
  }

  @Override
  public long truncateClass(String name, boolean polimorfic) {
    this.checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_UPDATE);
    SchemaClass clazz = getClass(name);
    if (clazz.isSubClassOf(SecurityShared.RESTRICTED_CLASSNAME)) {
      throw new SecurityException(
          "Class '"
              + getName()
              + "' cannot be truncated because has record level security enabled (extends '"
              + SecurityShared.RESTRICTED_CLASSNAME
              + "')");
    }

    int[] clusterIds;
    if (polimorfic) {
      clusterIds = clazz.getPolymorphicClusterIds();
    } else {
      clusterIds = clazz.getClusterIds();
    }
    long count = 0;
    for (int id : clusterIds) {
      if (id < 0) {
        continue;
      }
      final String clusterName = getClusterNameById(id);
      if (clusterName == null) {
        continue;
      }
      count += truncateClusterInternal(clusterName);
    }
    return count;
  }

  @Override
  public void truncateClass(String name) {
    truncateClass(name, true);
  }

  @Override
  public long truncateClusterInternal(String clusterName) {
    checkSecurity(Rule.ResourceGeneric.CLUSTER, Role.PERMISSION_DELETE, clusterName);
    checkForClusterPermissions(clusterName);

    int id = getClusterIdByName(clusterName);
    if (id == -1) {
      throw new DatabaseException("Cluster with name " + clusterName + " does not exist");
    }
    final SchemaClass clazz = getMetadata().getSchema().getClassByClusterId(id);
    if (clazz != null) {
      checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_DELETE, clazz.getName());
    }

    long count = 0;
    final RecordIteratorCluster<Record> iteratorCluster =
        new RecordIteratorCluster<Record>(this, id);

    while (iteratorCluster.hasNext()) {
      executeInTx(
          () -> {
            final Record record = bindToSession(iteratorCluster.next());
            record.delete();
          });
      count++;
    }
    return count;
  }

  @Override
  public NonTxReadMode getNonTxReadMode() {
    return nonTxReadMode;
  }

  @Override
  public void truncateCluster(String clusterName) {
    truncateClusterInternal(clusterName);
  }
}
