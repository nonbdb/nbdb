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

package com.orientechnologies.orient.core.db.document;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.io.OIOUtils;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.Oxygen;
import com.orientechnologies.orient.core.cache.OLocalRecordCache;
import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OScriptExecutor;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.conflict.ORecordConflictStrategy;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.ODatabaseStats;
import com.orientechnologies.orient.core.db.OHookReplacedRecordThreadLocal;
import com.orientechnologies.orient.core.db.OLiveQueryMonitor;
import com.orientechnologies.orient.core.db.OLiveQueryResultListener;
import com.orientechnologies.orient.core.db.OSharedContext;
import com.orientechnologies.orient.core.db.OSharedContextEmbedded;
import com.orientechnologies.orient.core.db.OxygenDBConfig;
import com.orientechnologies.orient.core.db.record.OClassTrigger;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.db.viewmanager.ViewManager;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OClassIndexManager;
import com.orientechnologies.orient.core.iterator.ORecordIteratorCluster;
import com.orientechnologies.orient.core.metadata.OMetadataDefault;
import com.orientechnologies.orient.core.metadata.function.OFunctionLibraryImpl;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OImmutableClass;
import com.orientechnologies.orient.core.metadata.schema.OImmutableSchema;
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy;
import com.orientechnologies.orient.core.metadata.schema.OView;
import com.orientechnologies.orient.core.metadata.security.OImmutableUser;
import com.orientechnologies.orient.core.metadata.security.OPropertyAccess;
import com.orientechnologies.orient.core.metadata.security.OPropertyEncryptionNone;
import com.orientechnologies.orient.core.metadata.security.ORestrictedAccessHook;
import com.orientechnologies.orient.core.metadata.security.ORestrictedOperation;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.ORule;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityShared;
import com.orientechnologies.orient.core.metadata.security.OSecurityUser;
import com.orientechnologies.orient.core.metadata.security.OToken;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.metadata.security.auth.OAuthenticationInfo;
import com.orientechnologies.orient.core.metadata.sequence.OSequenceAction;
import com.orientechnologies.orient.core.metadata.sequence.OSequenceLibraryProxy;
import com.orientechnologies.orient.core.query.live.OLiveQueryHook;
import com.orientechnologies.orient.core.query.live.OLiveQueryHookV2;
import com.orientechnologies.orient.core.query.live.OLiveQueryListenerV2;
import com.orientechnologies.orient.core.query.live.OLiveQueryMonitorEmbedded;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.ORecordAbstract;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.record.impl.OEdgeDocument;
import com.orientechnologies.orient.core.record.impl.OVertexInternal;
import com.orientechnologies.orient.core.schedule.OScheduledEvent;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializerFactory;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.executor.LiveQueryListenerImpl;
import com.orientechnologies.orient.core.sql.executor.OExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OInternalExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OInternalResultSet;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.parser.OLocalResultSet;
import com.orientechnologies.orient.core.sql.parser.OLocalResultSetLifecycleDecorator;
import com.orientechnologies.orient.core.sql.parser.OStatement;
import com.orientechnologies.orient.core.storage.ORecordMetadata;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.OStorageInfo;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.OFreezableStorageComponent;
import com.orientechnologies.orient.core.storage.ridbag.sbtree.OSBTreeCollectionManager;
import com.orientechnologies.orient.core.tx.OTransactionAbstract;
import com.orientechnologies.orient.core.tx.OTransactionNoTx;
import com.orientechnologies.orient.core.tx.OTransactionNoTx.NonTxReadMode;
import com.orientechnologies.orient.core.tx.OTransactionOptimistic;
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
public class ODatabaseSessionEmbedded extends ODatabaseSessionAbstract
    implements OQueryLifecycleListener {

  private OxygenDBConfig config;
  private OStorage storage;

  private OTransactionNoTx.NonTxReadMode nonTxReadMode;

  public ODatabaseSessionEmbedded(final OStorage storage) {
    activateOnCurrentThread();

    try {
      status = STATUS.CLOSED;

      try {
        var cfg = storage.getConfiguration();
        if (cfg != null) {
          var ctx = cfg.getContextConfiguration();
          if (ctx != null) {
            nonTxReadMode =
                OTransactionNoTx.NonTxReadMode.valueOf(
                    ctx.getValueAsString(OGlobalConfiguration.NON_TX_READS_WARNING_MODE));
          } else {
            nonTxReadMode = NonTxReadMode.WARN;
          }
        } else {
          nonTxReadMode = NonTxReadMode.WARN;
        }
      } catch (Exception e) {
        OLogManager.instance()
            .warn(
                this,
                "Invalid value for %s, using %s",
                e,
                OGlobalConfiguration.NON_TX_READS_WARNING_MODE.getKey(),
                NonTxReadMode.WARN);
        nonTxReadMode = NonTxReadMode.WARN;
      }

      // OVERWRITE THE URL
      url = storage.getURL();
      this.storage = storage;
      this.componentsFactory = storage.getComponentsFactory();

      unmodifiableHooks = Collections.unmodifiableMap(hooks);

      localCache = new OLocalRecordCache();

      init();

      databaseOwner = this;

    } catch (Exception t) {
      ODatabaseRecordThreadLocal.instance().remove();

      throw OException.wrapException(new ODatabaseException("Error on opening database "), t);
    }
  }

  public ODatabaseSession open(final String iUserName, final String iUserPassword) {
    throw new UnsupportedOperationException("Use OxygenDB");
  }

  public void init(OxygenDBConfig config, OSharedContext sharedContext) {
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

      ORecordSerializerFactory serializerFactory = ORecordSerializerFactory.instance();
      String serializeName = getStorageInfo().getConfiguration().getRecordSerializer();
      if (serializeName == null) {
        throw new ODatabaseException(
            "Impossible to open database from version before 2.x use export import instead");
      }
      serializer = serializerFactory.getFormat(serializeName);
      if (serializer == null) {
        throw new ODatabaseException(
            "RecordSerializer with name '" + serializeName + "' not found ");
      }
      if (getStorageInfo().getConfiguration().getRecordSerializerVersion()
          > serializer.getMinSupportedVersion()) {
        throw new ODatabaseException(
            "Persistent record serializer version is not support by the current implementation");
      }

      localCache.startup();

      loadMetadata();

      installHooksEmbedded();

      user = null;

      initialized = true;
    } catch (OException e) {
      ODatabaseRecordThreadLocal.instance().remove();
      throw e;
    } catch (Exception e) {
      ODatabaseRecordThreadLocal.instance().remove();
      throw OException.wrapException(
          new ODatabaseException("Cannot open database url=" + getURL()), e);
    }
  }

  public void internalOpen(final OAuthenticationInfo authenticationInfo) {
    try {
      OSecurityInternal security = sharedContext.getSecurity();

      if (user == null || user.getVersion() != security.getVersion(this)) {
        final OSecurityUser usr;

        usr = security.securityAuthenticate(this, authenticationInfo);
        if (usr != null) {
          user = new OImmutableUser(this, security.getVersion(this), usr);
        } else {
          user = null;
        }

        checkSecurity(ORule.ResourceGeneric.DATABASE, ORole.PERMISSION_READ);
      }

    } catch (OException e) {
      ODatabaseRecordThreadLocal.instance().remove();
      throw e;
    } catch (Exception e) {
      ODatabaseRecordThreadLocal.instance().remove();
      throw OException.wrapException(
          new ODatabaseException("Cannot open database url=" + getURL()), e);
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
            OSecurityInternal security = sharedContext.getSecurity();

            if (user == null
                || user.getVersion() != security.getVersion(this)
                || !user.getName(this).equalsIgnoreCase(iUserName)) {
              final OSecurityUser usr;

              if (checkPassword) {
                usr = security.securityAuthenticate(this, iUserName, iUserPassword);
              } else {
                usr = security.getUser(this, iUserName);
              }
              if (usr != null) {
                user = new OImmutableUser(this, security.getVersion(this), usr);
              } else {
                user = null;
              }

              checkSecurity(ORule.ResourceGeneric.DATABASE, ORole.PERMISSION_READ);
            }
          } catch (OException e) {
            ODatabaseRecordThreadLocal.instance().remove();
            throw e;
          } catch (Exception e) {
            ODatabaseRecordThreadLocal.instance().remove();
            throw OException.wrapException(
                new ODatabaseException("Cannot open database url=" + getURL()), e);
          }
        });
  }

  private void applyListeners(OxygenDBConfig config) {
    if (config != null) {
      for (ODatabaseListener listener : config.getListeners()) {
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
  public ODatabaseSession open(final OToken iToken) {
    throw new UnsupportedOperationException("Deprecated Method");
  }

  @Override
  public ODatabaseSession create() {
    throw new UnsupportedOperationException("Deprecated Method");
  }

  /**
   * {@inheritDoc}
   */
  public void internalCreate(OxygenDBConfig config, OSharedContext ctx) {
    ORecordSerializer serializer = ORecordSerializerFactory.instance().getDefaultRecordSerializer();
    if (serializer.toString().equals("ORecordDocument2csv")) {
      throw new ODatabaseException(
          "Impossible to create the database with ORecordDocument2csv serializer");
    }
    storage.setRecordSerializer(serializer.toString(), serializer.getCurrentVersion());
    storage.setProperty(OStatement.CUSTOM_STRICT_SQL, "true");

    this.setSerializer(serializer);

    this.sharedContext = ctx;
    this.status = STATUS.OPEN;
    // THIS IF SHOULDN'T BE NEEDED, CREATE HAPPEN ONLY IN EMBEDDED
    applyAttributes(config);
    applyListeners(config);
    metadata = new OMetadataDefault(this);
    installHooksEmbedded();
    createMetadata(ctx);
  }

  public void callOnCreateListeners() {
    // WAKE UP DB LIFECYCLE LISTENER
    for (Iterator<ODatabaseLifecycleListener> it = Oxygen.instance().getDbLifecycleListeners();
        it.hasNext(); ) {
      it.next().onCreate(getDatabaseOwner());
    }

    // WAKE UP LISTENERS
    for (ODatabaseListener listener : browseListeners()) {
      try {
        listener.onCreate(this);
      } catch (Exception ignore) {
      }
    }
  }

  protected void createMetadata(OSharedContext shared) {
    metadata.init(shared);
    ((OSharedContextEmbedded) shared).create(this);
  }

  @Override
  protected void loadMetadata() {
    executeInTx(
        () -> {
          metadata = new OMetadataDefault(this);
          metadata.init(sharedContext);
          sharedContext.load(this);
        });
  }

  private void applyAttributes(OxygenDBConfig config) {
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

    final String stringValue = OIOUtils.getStringContent(iValue != null ? iValue.toString() : null);
    final OStorage storage = this.storage;
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

        // for backward compatibility, until 2.1.13 OxygenDB accepted timezones in lowercase as well
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
            Oxygen.instance().getRecordConflictStrategy().getStrategy(stringValue));
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
    final OStorage storage = this.storage;
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

  public ODatabaseSession setCustom(final String name, final Object iValue) {
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
  public ODatabaseSession create(String incrementalBackupPath) {
    throw new UnsupportedOperationException("use OxygenDB");
  }

  @Override
  public ODatabaseSession create(final Map<OGlobalConfiguration, Object> iInitialSettings) {
    throw new UnsupportedOperationException("use OxygenDB");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void drop() {
    throw new UnsupportedOperationException("use OxygenDB");
  }

  /**
   * Returns a copy of current database if it's open. The returned instance can be used by another
   * thread without affecting current instance. The database copy is not set in thread local.
   */
  public ODatabaseSessionInternal copy() {
    var storage = (OStorage) getSharedContext().getStorage();
    storage.open(this, null, null, config.getConfigurations());
    ODatabaseSessionEmbedded database = new ODatabaseSessionEmbedded(storage);
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
    throw new UnsupportedOperationException("use OxygenDB");
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
  public OStorage getStorage() {
    return storage;
  }

  @Override
  public OStorageInfo getStorageInfo() {
    return storage;
  }

  @Override
  public void replaceStorage(OStorage iNewStorage) {
    this.getSharedContext().setStorage(iNewStorage);
    storage = iNewStorage;
  }

  @Override
  public OResultSet query(String query, Object[] args) {
    checkOpenness();
    checkIfActive();
    getSharedContext().getOxygenDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      OStatement statement = OSQLEngine.parse(query, this);
      if (!statement.isIdempotent()) {
        throw new OCommandExecutionException(
            "Cannot execute query on non idempotent statement: " + query);
      }
      OResultSet original = statement.execute(this, args, true);
      OLocalResultSetLifecycleDecorator result = new OLocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getOxygenDB().endCommand();
    }
  }

  @Override
  public OResultSet query(String query, Map args) {
    checkOpenness();
    checkIfActive();
    getSharedContext().getOxygenDB().startCommand(Optional.empty());
    preQueryStart();
    try {
      OStatement statement = OSQLEngine.parse(query, this);
      if (!statement.isIdempotent()) {
        throw new OCommandExecutionException(
            "Cannot execute query on non idempotent statement: " + query);
      }
      OResultSet original = statement.execute(this, args, true);
      OLocalResultSetLifecycleDecorator result = new OLocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getOxygenDB().endCommand();
    }
  }

  @Override
  public OResultSet command(String query, Object[] args) {
    checkOpenness();
    checkIfActive();

    getSharedContext().getOxygenDB().startCommand(Optional.empty());
    preQueryStart();
    try {
      OStatement statement = OSQLEngine.parse(query, this);
      OResultSet original = statement.execute(this, args, true);
      OLocalResultSetLifecycleDecorator result;
      if (!statement.isIdempotent()) {
        // fetch all, close and detach
        OInternalResultSet prefetched = new OInternalResultSet();
        original.forEachRemaining(x -> prefetched.add(x));
        original.close();
        queryCompleted();
        result = new OLocalResultSetLifecycleDecorator(prefetched);
      } else {
        // stream, keep open and attach to the current DB
        result = new OLocalResultSetLifecycleDecorator(original);
        queryStarted(result);
      }
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getOxygenDB().endCommand();
    }
  }

  @Override
  public OResultSet command(String query, Map args) {
    checkOpenness();
    checkIfActive();

    getSharedContext().getOxygenDB().startCommand(Optional.empty());
    try {
      preQueryStart();

      OStatement statement = OSQLEngine.parse(query, this);
      OResultSet original = statement.execute(this, args, true);
      OLocalResultSetLifecycleDecorator result;
      if (!statement.isIdempotent()) {
        // fetch all, close and detach
        OInternalResultSet prefetched = new OInternalResultSet();
        original.forEachRemaining(x -> prefetched.add(x));
        original.close();
        queryCompleted();
        result = new OLocalResultSetLifecycleDecorator(prefetched);
      } else {
        // stream, keep open and attach to the current DB
        result = new OLocalResultSetLifecycleDecorator(original);

        queryStarted(result);
      }

      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getOxygenDB().endCommand();
    }
  }

  @Override
  public OResultSet execute(String language, String script, Object... args) {
    checkOpenness();
    checkIfActive();
    if (!"sql".equalsIgnoreCase(language)) {
      checkSecurity(ORule.ResourceGeneric.COMMAND, ORole.PERMISSION_EXECUTE, language);
    }
    getSharedContext().getOxygenDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      OScriptExecutor executor =
          getSharedContext()
              .getOxygenDB()
              .getScriptManager()
              .getCommandManager()
              .getScriptExecutor(language);

      ((OAbstractPaginatedStorage) this.storage).pauseConfigurationUpdateNotifications();
      OResultSet original;
      try {
        original = executor.execute(this, script, args);
      } finally {
        ((OAbstractPaginatedStorage) this.storage).fireConfigurationUpdateNotifications();
      }
      OLocalResultSetLifecycleDecorator result = new OLocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getOxygenDB().endCommand();
    }
  }

  private void cleanQueryState() {
    this.queryState.pop();
  }

  private void queryCompleted() {
    OQueryDatabaseState state = this.queryState.peekLast();
    state.closeInternal(this);
  }

  private void queryStarted(OLocalResultSetLifecycleDecorator result) {
    OQueryDatabaseState state = this.queryState.peekLast();
    state.setResultSet(result);
    this.queryStarted(result.getQueryId(), state);
    result.addLifecycleListener(this);
  }

  private void preQueryStart() {
    this.queryState.push(new OQueryDatabaseState());
  }

  @Override
  public OResultSet execute(String language, String script, Map<String, ?> args) {
    checkOpenness();
    checkIfActive();
    if (!"sql".equalsIgnoreCase(language)) {
      checkSecurity(ORule.ResourceGeneric.COMMAND, ORole.PERMISSION_EXECUTE, language);
    }
    getSharedContext().getOxygenDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      OScriptExecutor executor =
          sharedContext
              .getOxygenDB()
              .getScriptManager()
              .getCommandManager()
              .getScriptExecutor(language);
      OResultSet original;

      ((OAbstractPaginatedStorage) this.storage).pauseConfigurationUpdateNotifications();
      try {
        original = executor.execute(this, script, args);
      } finally {
        ((OAbstractPaginatedStorage) this.storage).fireConfigurationUpdateNotifications();
      }

      OLocalResultSetLifecycleDecorator result = new OLocalResultSetLifecycleDecorator(original);
      queryStarted(result);
      return result;
    } finally {
      cleanQueryState();
      getSharedContext().getOxygenDB().endCommand();
    }
  }

  public OLocalResultSetLifecycleDecorator query(OExecutionPlan plan, Map<Object, Object> params) {
    checkOpenness();
    checkIfActive();
    getSharedContext().getOxygenDB().startCommand(Optional.empty());
    try {
      preQueryStart();
      OBasicCommandContext ctx = new OBasicCommandContext();
      ctx.setDatabase(this);
      ctx.setInputParameters(params);

      OLocalResultSet result = new OLocalResultSet((OInternalExecutionPlan) plan);
      OLocalResultSetLifecycleDecorator decorator = new OLocalResultSetLifecycleDecorator(result);
      queryStarted(decorator);

      return decorator;
    } finally {
      cleanQueryState();
      getSharedContext().getOxygenDB().endCommand();
    }
  }

  public void queryStartUsingViewCluster(int clusterId) {
    OSharedContext sharedContext = getSharedContext();
    ViewManager viewManager = sharedContext.getViewManager();
    viewManager.startUsingViewCluster(clusterId);
    this.queryState.peekLast().addViewUseCluster(clusterId);
  }

  public void queryStartUsingViewIndex(String index) {
    OSharedContext sharedContext = getSharedContext();
    ViewManager viewManager = sharedContext.getViewManager();
    viewManager.startUsingViewIndex(index);
    this.queryState.peekLast().addViewUseIndex(index);
  }

  @Override
  public void queryStarted(String id, OResultSet resultSet) {
    // to nothing just compatibility
  }

  public OxygenDBConfig getConfig() {
    return config;
  }

  @Override
  public OLiveQueryMonitor live(String query, OLiveQueryResultListener listener, Object... args) {
    checkOpenness();
    checkIfActive();

    OLiveQueryListenerV2 queryListener = new LiveQueryListenerImpl(listener, query, this, args);
    ODatabaseSessionInternal dbCopy = this.copy();
    this.activateOnCurrentThread();
    OLiveQueryMonitor monitor = new OLiveQueryMonitorEmbedded(queryListener.getToken(), dbCopy);
    return monitor;
  }

  @Override
  public OLiveQueryMonitor live(
      String query, OLiveQueryResultListener listener, Map<String, ?> args) {
    checkOpenness();
    checkIfActive();

    OLiveQueryListenerV2 queryListener =
        new LiveQueryListenerImpl(listener, query, this, (Map) args);
    ODatabaseSessionInternal dbCopy = this.copy();
    this.activateOnCurrentThread();
    OLiveQueryMonitor monitor = new OLiveQueryMonitorEmbedded(queryListener.getToken(), dbCopy);
    return monitor;
  }

  @Override
  public void recycle(final ORecord record) {
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
  public OIdentifiable beforeCreateOperations(OIdentifiable id, String iClusterName) {
    checkSecurity(ORole.PERMISSION_CREATE, id, iClusterName);

    ORecordHook.RESULT triggerChanged = null;
    boolean changed = false;
    if (id instanceof ODocument doc) {

      if (!getSharedContext().getSecurity().canCreate(this, doc)) {
        throw new OSecurityException(
            "Cannot update record "
                + doc
                + ": the resource has restricted access due to security policies");
      }

      OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        checkSecurity(ORule.ResourceGeneric.CLASS, ORole.PERMISSION_CREATE, clazz.getName());
        if (clazz.isScheduler()) {
          getSharedContext().getScheduler().initScheduleRecord(this, doc);
          changed = true;
        }
        if (clazz.isOuser()) {
          doc.validate();
          changed = OUser.encodePassword(this, doc);
        }
        if (clazz.isTriggered()) {
          triggerChanged = OClassTrigger.onRecordBeforeCreate(doc, this);
        }
        if (clazz.isRestricted()) {
          changed = ORestrictedAccessHook.onRecordBeforeCreate(doc, this);
        }
        if (clazz.isFunction()) {
          OFunctionLibraryImpl.validateFunctionRecord(doc);
        }
        ODocumentInternal.setPropertyEncryption(doc, OPropertyEncryptionNone.instance());
      }
    }

    ORecordHook.RESULT res = callbackHooks(ORecordHook.TYPE.BEFORE_CREATE, id);
    if (changed
        || res == ORecordHook.RESULT.RECORD_CHANGED
        || triggerChanged == ORecordHook.RESULT.RECORD_CHANGED) {
      if (id instanceof ODocument) {
        ((ODocument) id).validate();
      }
      return id;
    } else {
      if (res == ORecordHook.RESULT.RECORD_REPLACED
          || triggerChanged == ORecordHook.RESULT.RECORD_REPLACED) {
        ORecord replaced = OHookReplacedRecordThreadLocal.INSTANCE.get();
        if (replaced instanceof ODocument) {
          ((ODocument) replaced).validate();
        }
        return replaced;
      }
    }
    return null;
  }

  @Override
  public OIdentifiable beforeUpdateOperations(OIdentifiable id, String iClusterName) {
    checkSecurity(ORole.PERMISSION_UPDATE, id, iClusterName);

    ORecordHook.RESULT triggerChanged = null;
    boolean changed = false;
    if (id instanceof ODocument doc) {
      OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isScheduler()) {
          getSharedContext().getScheduler().preHandleUpdateScheduleInTx(this, doc);
          changed = true;
        }
        if (clazz.isOuser()) {
          changed = OUser.encodePassword(this, doc);
        }
        if (clazz.isTriggered()) {
          triggerChanged = OClassTrigger.onRecordBeforeUpdate(doc, this);
        }
        if (clazz.isRestricted()) {
          if (!ORestrictedAccessHook.isAllowed(
              this, doc, ORestrictedOperation.ALLOW_UPDATE, true)) {
            throw new OSecurityException(
                "Cannot update record "
                    + doc.getIdentity()
                    + ": the resource has restricted access");
          }
        }
        if (clazz.isFunction()) {
          OFunctionLibraryImpl.validateFunctionRecord(doc);
        }
        if (!getSharedContext().getSecurity().canUpdate(this, doc)) {
          throw new OSecurityException(
              "Cannot update record "
                  + doc.getIdentity()
                  + ": the resource has restricted access due to security policies");
        }
        ODocumentInternal.setPropertyEncryption(doc, OPropertyEncryptionNone.instance());
      }
    }
    ORecordHook.RESULT res = callbackHooks(ORecordHook.TYPE.BEFORE_UPDATE, id);
    if (res == ORecordHook.RESULT.RECORD_CHANGED
        || triggerChanged == ORecordHook.RESULT.RECORD_CHANGED) {
      if (id instanceof ODocument) {
        ((ODocument) id).validate();
      }
      return id;
    } else {
      if (res == ORecordHook.RESULT.RECORD_REPLACED
          || triggerChanged == ORecordHook.RESULT.RECORD_REPLACED) {
        ORecord replaced = OHookReplacedRecordThreadLocal.INSTANCE.get();
        if (replaced instanceof ODocument) {
          ((ODocument) replaced).validate();
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
   * the database, then a {@link OConcurrentModificationException} exception is thrown.
   *
   * @param record record to delete
   */
  public void delete(ORecord record) {
    checkOpenness();

    if (record == null) {
      throw new ODatabaseException("Cannot delete null document");
    }

    if (record instanceof OElement) {
      if (((OElement) record).isVertex()) {
        OVertexInternal.deleteLinks(((OElement) record).toVertex());
      } else {
        if (((OElement) record).isEdge()) {
          OEdgeDocument.deleteLinks(((OElement) record).toEdge());
        }
      }
    }

    // CHECK ACCESS ON SCHEMA CLASS NAME (IF ANY)
    if (record instanceof ODocument && ((ODocument) record).getClassName() != null) {
      checkSecurity(
          ORule.ResourceGeneric.CLASS,
          ORole.PERMISSION_DELETE,
          ((ODocument) record).getClassName());
    }

    try {
      currentTx.deleteRecord((ORecordAbstract) record);
    } catch (OException e) {
      throw e;
    } catch (Exception e) {
      if (record instanceof ODocument) {
        throw OException.wrapException(
            new ODatabaseException(
                "Error on deleting record "
                    + record.getIdentity()
                    + " of class '"
                    + ((ODocument) record).getClassName()
                    + "'"),
            e);
      } else {
        throw OException.wrapException(
            new ODatabaseException("Error on deleting record " + record.getIdentity()), e);
      }
    }
  }

  @Override
  public void beforeDeleteOperations(OIdentifiable id, String iClusterName) {
    checkSecurity(ORole.PERMISSION_DELETE, id, iClusterName);
    if (id instanceof ODocument doc) {
      OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isTriggered()) {
          OClassTrigger.onRecordBeforeDelete(doc, this);
        }
        if (clazz.isRestricted()) {
          if (!ORestrictedAccessHook.isAllowed(
              this, doc, ORestrictedOperation.ALLOW_DELETE, true)) {
            throw new OSecurityException(
                "Cannot delete record "
                    + doc.getIdentity()
                    + ": the resource has restricted access");
          }
        }
        if (!getSharedContext().getSecurity().canDelete(this, doc)) {
          throw new OSecurityException(
              "Cannot delete record "
                  + doc.getIdentity()
                  + ": the resource has restricted access due to security policies");
        }
      }
    }
    callbackHooks(ORecordHook.TYPE.BEFORE_DELETE, id);
  }

  public void afterCreateOperations(final OIdentifiable id) {
    if (id instanceof ODocument doc) {
      final OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);

      if (clazz != null) {
        OClassIndexManager.checkIndexesAfterCreate(doc, this);
        if (clazz.isFunction()) {
          this.getSharedContext().getFunctionLibrary().createdFunction(doc);
        }
        if (clazz.isOuser() || clazz.isOrole() || clazz.isSecurityPolicy()) {
          sharedContext.getSecurity().incrementVersion(this);
        }
        if (clazz.isTriggered()) {
          OClassTrigger.onRecordAfterCreate(doc, this);
        }
      }

      OLiveQueryHook.addOp(doc, ORecordOperation.CREATED, this);
      OLiveQueryHookV2.addOp(this, doc, ORecordOperation.CREATED);
    }

    callbackHooks(ORecordHook.TYPE.AFTER_CREATE, id);
  }

  public void afterUpdateOperations(final OIdentifiable id) {
    if (id instanceof ODocument doc) {
      OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        OClassIndexManager.checkIndexesAfterUpdate((ODocument) id, this);

        if (clazz.isOuser() || clazz.isOrole() || clazz.isSecurityPolicy()) {
          sharedContext.getSecurity().incrementVersion(this);
        }

        if (clazz.isTriggered()) {
          OClassTrigger.onRecordAfterUpdate(doc, this);
        }

      }

    }
    callbackHooks(ORecordHook.TYPE.AFTER_UPDATE, id);
  }

  public void afterDeleteOperations(final OIdentifiable id) {
    if (id instanceof ODocument doc) {
      OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        OClassIndexManager.checkIndexesAfterDelete(doc, this);
        if (clazz.isFunction()) {
          this.getSharedContext().getFunctionLibrary().droppedFunction(doc);
        }
        if (clazz.isSequence()) {
          ((OSequenceLibraryProxy) getMetadata().getSequenceLibrary())
              .getDelegate()
              .onSequenceDropped(this, doc);
        }
        if (clazz.isScheduler()) {
          final String eventName = doc.field(OScheduledEvent.PROP_NAME);
          getSharedContext().getScheduler().removeEventInternal(eventName);
        }
        if (clazz.isTriggered()) {
          OClassTrigger.onRecordAfterDelete(doc, this);
        }
        getSharedContext().getViewManager().recordDeleted(clazz, doc, this);
      }
      OLiveQueryHook.addOp(doc, ORecordOperation.DELETED, this);
      OLiveQueryHookV2.addOp(this, doc, ORecordOperation.DELETED);
    }
    callbackHooks(ORecordHook.TYPE.AFTER_DELETE, id);
  }

  @Override
  public void afterReadOperations(OIdentifiable identifiable) {
    if (identifiable instanceof ODocument doc) {
      OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isTriggered()) {
          OClassTrigger.onRecordAfterRead(doc, this);
        }
      }
    }
    callbackHooks(ORecordHook.TYPE.AFTER_READ, identifiable);
  }

  @Override
  public boolean beforeReadOperations(OIdentifiable identifiable) {
    if (identifiable instanceof ODocument doc) {
      OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
      if (clazz != null) {
        if (clazz.isTriggered()) {
          ORecordHook.RESULT val = OClassTrigger.onRecordBeforeRead(doc, this);
          if (val == ORecordHook.RESULT.SKIP) {
            return true;
          }
        }
        if (clazz.isRestricted()) {
          if (!ORestrictedAccessHook.isAllowed(this, doc, ORestrictedOperation.ALLOW_READ, false)) {
            return true;
          }
        }
        try {
          checkSecurity(ORule.ResourceGeneric.CLASS, ORole.PERMISSION_READ, clazz.getName());
        } catch (OSecurityException e) {
          return true;
        }

        if (!getSharedContext().getSecurity().canRead(this, doc)) {
          return true;
        }

        ODocumentInternal.setPropertyAccess(
            doc, new OPropertyAccess(this, doc, getSharedContext().getSecurity()));
        ODocumentInternal.setPropertyEncryption(doc, OPropertyEncryptionNone.instance());
      }
    }
    return callbackHooks(ORecordHook.TYPE.BEFORE_READ, identifiable) == ORecordHook.RESULT.SKIP;
  }

  @Override
  public void afterCommitOperations() {
    for (var operation : currentTx.getRecordOperations()) {
      if (operation.type == ORecordOperation.CREATED) {
        var record = operation.record;

        if (record instanceof ODocument doc) {
          OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);

          if (clazz != null) {
            if (clazz.isSequence()) {
              ((OSequenceLibraryProxy) getMetadata().getSequenceLibrary())
                  .getDelegate()
                  .onSequenceCreated(this, doc);
            }

            if (clazz.isScheduler()) {
              getMetadata().getScheduler().scheduleEvent(this, new OScheduledEvent(doc, this));
            }
          }
        }
      } else if (operation.type == ORecordOperation.UPDATED) {
        var record = operation.record;

        if (record instanceof ODocument doc) {
          OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(this, doc);
          if (clazz != null) {
            if (clazz.isFunction()) {
              this.getSharedContext().getFunctionLibrary().updatedFunction(doc);
            }
            if (clazz.isScheduler()) {
              getSharedContext().getScheduler().postHandleUpdateScheduleAfterTxCommit(this, doc);
            }
          }

          OLiveQueryHook.addOp(doc, ORecordOperation.UPDATED, this);
          OLiveQueryHookV2.addOp(this, doc, ORecordOperation.UPDATED);
        }
      }
    }

    super.afterCommitOperations();

    OLiveQueryHook.notifyForTxChanges(this);
    OLiveQueryHookV2.notifyForTxChanges(this);
  }

  @Override
  protected void afterRollbackOperations() {
    super.afterRollbackOperations();
    OLiveQueryHook.removePendingDatabaseOps(this);
    OLiveQueryHookV2.removePendingDatabaseOps(this);
  }

  public String getClusterName(final ORecord record) {
    int clusterId = record.getIdentity().getClusterId();
    if (clusterId == ORID.CLUSTER_ID_INVALID) {
      // COMPUTE THE CLUSTER ID
      OClass schemaClass = null;
      if (record instanceof ODocument) {
        schemaClass = ODocumentInternal.getImmutableSchemaClass(this, (ODocument) record);
      }
      if (schemaClass != null) {
        // FIND THE RIGHT CLUSTER AS CONFIGURED IN CLASS
        if (schemaClass.isAbstract()) {
          throw new OSchemaException(
              "Document belongs to abstract class '"
                  + schemaClass.getName()
                  + "' and cannot be saved");
        }
        clusterId = schemaClass.getClusterForNewInstance((ODocument) record);
        return getClusterNameById(clusterId);
      } else {
        return getClusterNameById(storage.getDefaultClusterId());
      }

    } else {
      return getClusterNameById(clusterId);
    }
  }

  @Override
  public OView getViewFromCluster(int cluster) {
    OImmutableSchema schema = getMetadata().getImmutableSchemaSnapshot();
    OView view = schema.getViewByClusterId(cluster);
    if (view == null) {
      String viewName = getSharedContext().getViewManager().getViewFromOldCluster(cluster);
      if (viewName != null) {
        view = schema.getView(viewName);
      }
    }
    return view;
  }

  @Override
  public boolean executeExists(ORID rid) {
    checkOpenness();
    checkIfActive();
    try {
      checkSecurity(
          ORule.ResourceGeneric.CLUSTER,
          ORole.PERMISSION_READ,
          getClusterNameById(rid.getClusterId()));

      ORecord record = getTransaction().getRecord(rid);
      if (record == OTransactionAbstract.DELETED_RECORD) {
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
      throw OException.wrapException(
          new ODatabaseException(
              "Error on retrieving record "
                  + rid
                  + " (cluster: "
                  + storage.getPhysicalClusterNameById(rid.getClusterId())
                  + ")"),
          t);
    }
  }

  @Override
  public <T> T sendSequenceAction(OSequenceAction action)
      throws ExecutionException, InterruptedException {
    throw new UnsupportedOperationException(
        "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
  }

  /**
   * {@inheritDoc}
   */
  public void checkSecurity(
      final ORule.ResourceGeneric resourceGeneric,
      final String resourceSpecific,
      final int iOperation) {
    if (user != null) {
      try {
        user.allow(this, resourceGeneric, resourceSpecific, iOperation);
      } catch (OSecurityAccessException e) {

        if (OLogManager.instance().isDebugEnabled()) {
          OLogManager.instance()
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
      final ORule.ResourceGeneric iResourceGeneric,
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
      final ORule.ResourceGeneric iResourceGeneric,
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
    final String resourceSpecific = ORule.mapLegacyResourceToSpecificResource(iResource);
    final ORule.ResourceGeneric resourceGeneric =
        ORule.mapLegacyResourceToGenericResource(iResource);

    if (resourceSpecific == null || resourceSpecific.equals("*")) {
      checkSecurity(resourceGeneric, null, iOperation);
    }

    checkSecurity(resourceGeneric, resourceSpecific, iOperation);
  }

  @Override
  @Deprecated
  public void checkSecurity(
      final String iResourceGeneric, final int iOperation, final Object iResourceSpecific) {
    final ORule.ResourceGeneric resourceGeneric =
        ORule.mapLegacyResourceToGenericResource(iResourceGeneric);
    if (iResourceSpecific == null || iResourceSpecific.equals("*")) {
      checkSecurity(resourceGeneric, iOperation, (Object) null);
    }

    checkSecurity(resourceGeneric, iOperation, iResourceSpecific);
  }

  @Override
  @Deprecated
  public void checkSecurity(
      final String iResourceGeneric, final int iOperation, final Object... iResourcesSpecific) {
    final ORule.ResourceGeneric resourceGeneric =
        ORule.mapLegacyResourceToGenericResource(iResourceGeneric);
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

  public ORecordConflictStrategy getConflictStrategy() {
    checkIfActive();
    return getStorageInfo().getRecordConflictStrategy();
  }

  public ODatabaseSessionEmbedded setConflictStrategy(final String iStrategyName) {
    checkIfActive();
    storage.setConflictStrategy(
        Oxygen.instance().getRecordConflictStrategy().getStrategy(iStrategyName));
    return this;
  }

  public ODatabaseSessionEmbedded setConflictStrategy(final ORecordConflictStrategy iResolver) {
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
      throw OException.wrapException(
          new ODatabaseException("Error on reading records size for cluster '" + clusterName + "'"),
          e);
    }
  }

  @Override
  public long getClusterRecordSizeById(final int clusterId) {
    checkIfActive();
    try {
      return storage.getClusterRecordsSizeById(clusterId);
    } catch (Exception e) {
      throw OException.wrapException(
          new ODatabaseException(
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
    checkSecurity(ORule.ResourceGeneric.CLUSTER, ORole.PERMISSION_READ, name);
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
      checkSecurity(ORule.ResourceGeneric.CLUSTER, ORole.PERMISSION_READ, name);
    }
    return storage.count(this, iClusterIds, countTombstones);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long countClusterElements(final String iClusterName) {
    checkSecurity(ORule.ResourceGeneric.CLUSTER, ORole.PERMISSION_READ, iClusterName);
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
    OSchemaProxy schema = metadata.getSchema();
    OClass clazz = schema.getClassByClusterId(clusterId);
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
        ORule.ResourceGeneric.CLUSTER, ORole.PERMISSION_DELETE, getClusterNameById(clusterId));

    OSchemaProxy schema = metadata.getSchema();
    final OClass clazz = schema.getClassByClusterId(clusterId);
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

    final ORecordIteratorCluster<ORecord> iteratorCluster = browseCluster(clusterName);
    if (iteratorCluster == null) {
      return false;
    }

    executeInTxBatches((Iterator<ORecord>) iteratorCluster, (session, record) -> delete(record));

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

  public ODatabaseStats getStats() {
    ODatabaseStats stats = new ODatabaseStats();
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
    checkSecurity(ORule.ResourceGeneric.DATABASE, "backup", ORole.PERMISSION_EXECUTE);

    return storage.incrementalBackup(this, path, null);
  }

  @Override
  public ORecordMetadata getRecordMetadata(final ORID rid) {
    checkIfActive();
    return storage.getRecordMetadata(this, rid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void freeze(final boolean throwException) {
    checkOpenness();
    if (!(storage instanceof OFreezableStorageComponent)) {
      OLogManager.instance()
          .error(
              this,
              "Only local paginated storage supports freeze. If you are using remote client please"
                  + " use OServerAdmin instead",
              null);

      return;
    }

    final long startTime = Oxygen.instance().getProfiler().startChrono();

    final OFreezableStorageComponent storage = getFreezableStorage();
    if (storage != null) {
      storage.freeze(throwException);
    }

    Oxygen.instance()
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
    if (!(storage instanceof OFreezableStorageComponent)) {
      OLogManager.instance()
          .error(
              this,
              "Only local paginated storage supports release. If you are using remote client please"
                  + " use OServerAdmin instead",
              null);
      return;
    }

    final long startTime = Oxygen.instance().getProfiler().startChrono();

    final OFreezableStorageComponent storage = getFreezableStorage();
    if (storage != null) {
      storage.release();
    }

    Oxygen.instance()
        .getProfiler()
        .stopChrono(
            "db." + getName() + ".release",
            "Time to release the database",
            startTime,
            "db.*.release");
  }

  private OFreezableStorageComponent getFreezableStorage() {
    OStorage s = storage;
    if (s instanceof OFreezableStorageComponent) {
      return (OFreezableStorageComponent) s;
    } else {
      OLogManager.instance()
          .error(
              this, "Storage of type " + s.getType() + " does not support freeze operation", null);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public OSBTreeCollectionManager getSbTreeCollectionManager() {
    return storage.getSBtreeCollectionManager();
  }

  @Override
  public void reload() {
    checkIfActive();

    if (this.isClosed()) {
      throw new ODatabaseException("Cannot reload a closed db");
    }
    metadata.reload();
    storage.reload(this);
  }

  @Override
  public void internalCommit(OTransactionOptimistic transaction) {
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
        OLogManager.instance().error(this, "Exception during rollback of active transaction", e);
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
      ODatabaseRecordThreadLocal.instance().remove();
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
    ((OAbstractPaginatedStorage) storage).startDDL();
  }

  public void endExclusiveMetadataChange() {
    ((OAbstractPaginatedStorage) storage).endDDL();
  }

  @Override
  public long truncateClass(String name, boolean polimorfic) {
    this.checkSecurity(ORule.ResourceGeneric.CLASS, ORole.PERMISSION_UPDATE);
    OClass clazz = getClass(name);
    if (clazz.isSubClassOf(OSecurityShared.RESTRICTED_CLASSNAME)) {
      throw new OSecurityException(
          "Class '"
              + getName()
              + "' cannot be truncated because has record level security enabled (extends '"
              + OSecurityShared.RESTRICTED_CLASSNAME
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
    checkSecurity(ORule.ResourceGeneric.CLUSTER, ORole.PERMISSION_DELETE, clusterName);
    checkForClusterPermissions(clusterName);

    int id = getClusterIdByName(clusterName);
    if (id == -1) {
      throw new ODatabaseException("Cluster with name " + clusterName + " does not exist");
    }
    final OClass clazz = getMetadata().getSchema().getClassByClusterId(id);
    if (clazz != null) {
      checkSecurity(ORule.ResourceGeneric.CLASS, ORole.PERMISSION_DELETE, clazz.getName());
    }

    long count = 0;
    final ORecordIteratorCluster<ORecord> iteratorCluster =
        new ORecordIteratorCluster<ORecord>(this, id);

    while (iteratorCluster.hasNext()) {
      executeInTx(
          () -> {
            final ORecord record = bindToSession(iteratorCluster.next());
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
