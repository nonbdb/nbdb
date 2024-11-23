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
package com.orientechnologies.orient.core.index;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.util.OMultiKey;
import com.orientechnologies.common.util.OUncaughtExceptionHandler;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.OMetadataUpdateListener;
import com.orientechnologies.orient.core.db.record.OTrackedSet;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.OMetadataDefault;
import com.orientechnologies.orient.core.metadata.OMetadataInternal;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClassImpl;
import com.orientechnologies.orient.core.metadata.schema.OSchemaShared;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityResourceProperty;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sharding.auto.OAutoShardingIndexFactory;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages indexes at database level. A single instance is shared among multiple databases.
 * Contentions are managed by r/w locks.
 */
public class OIndexManagerShared implements OIndexManagerAbstract {

  private transient volatile Thread recreateIndexesThread = null;
  volatile boolean rebuildCompleted = false;
  final OStorage storage;
  // values of this Map should be IMMUTABLE !! for thread safety reasons.
  protected final Map<String, Map<OMultiKey, Set<OIndex>>> classPropertyIndex =
      new ConcurrentHashMap<>();
  protected Map<String, OIndex> indexes = new ConcurrentHashMap<>();
  protected String defaultClusterName = OMetadataDefault.CLUSTER_INDEX_NAME;
  protected String manualClusterName = OMetadataDefault.CLUSTER_MANUAL_INDEX_NAME;
  protected final AtomicInteger writeLockNesting = new AtomicInteger();

  protected final ReadWriteLock lock = new ReentrantReadWriteLock();
  protected ORID identity;

  public OIndexManagerShared(OStorage storage) {
    super();
    this.storage = storage;
  }

  public void load(ODatabaseSessionInternal database) {
    if (!autoRecreateIndexesAfterCrash(database)) {
      acquireExclusiveLock();
      try {
        if (database.getStorageInfo().getConfiguration().getIndexMgrRecordId() == null)
        // @COMPATIBILITY: CREATE THE INDEX MGR
        {
          create(database);
        }
        identity =
            new ORecordId(database.getStorageInfo().getConfiguration().getIndexMgrRecordId());
        // RELOAD IT
        ODocument document = database.load(identity, null, false);
        fromStream(database, document);
        ORecordInternal.unsetDirty(document);
        document.unload();
      } finally {
        releaseExclusiveLock(database);
      }
    }
  }

  public void reload(ODatabaseSessionInternal session) {
    acquireExclusiveLock();
    try {
      session.executeInTx(
          () -> {
            ODocument document = session.load(identity);
            fromStream(session, document);
          });
    } finally {
      releaseExclusiveLock(session);
    }
  }

  public void save(ODatabaseSessionInternal session) {
    internalSave(session);
  }

  public void addClusterToIndex(ODatabaseSessionInternal session, final String clusterName,
      final String indexName) {
    acquireSharedLock();
    try {
      final OIndex index = indexes.get(indexName);
      if (index.getInternal().getClusters().contains(clusterName)) {
        return;
      }
    } finally {
      releaseSharedLock();
    }
    acquireExclusiveLock();
    try {
      final OIndex index = indexes.get(indexName);
      if (index == null) {
        throw new OIndexException("Index with name " + indexName + " does not exist.");
      }

      if (index.getInternal() == null) {
        throw new OIndexException(
            "Index with name " + indexName + " has no internal presentation.");
      }
      if (!index.getInternal().getClusters().contains(clusterName)) {
        index.getInternal().addCluster(session, clusterName);
      }
    } finally {
      releaseExclusiveLock(session, true);
    }
  }

  public void removeClusterFromIndex(ODatabaseSessionInternal session, final String clusterName,
      final String indexName) {
    acquireSharedLock();
    try {
      final OIndex index = indexes.get(indexName);
      if (!index.getInternal().getClusters().contains(clusterName)) {
        return;
      }
    } finally {
      releaseSharedLock();
    }
    acquireExclusiveLock();
    try {
      final OIndex index = indexes.get(indexName);
      if (index == null) {
        throw new OIndexException("Index with name " + indexName + " does not exist.");
      }
      index.getInternal().removeCluster(session, clusterName);
    } finally {
      releaseExclusiveLock(session, true);
    }
  }

  public void create(ODatabaseSessionInternal database) {
    acquireExclusiveLock();
    try {
      ODocument document =
          database.computeInTx(
              () -> database.save(new ODocument(), OMetadataDefault.CLUSTER_INTERNAL_NAME));
      identity = document.getIdentity();
      database.getStorage().setIndexMgrRecordId(document.getIdentity().toString());
    } finally {
      releaseExclusiveLock(database);
    }
  }

  public Collection<? extends OIndex> getIndexes(ODatabaseSessionInternal database) {
    return indexes.values();
  }

  public OIndex getRawIndex(final String iName) {
    final OIndex index = indexes.get(iName);
    return index;
  }

  public OIndex getIndex(ODatabaseSessionInternal database, final String iName) {
    final OIndex index = indexes.get(iName);
    return index;
  }

  public boolean existsIndex(final String iName) {
    return indexes.containsKey(iName);
  }

  public String getDefaultClusterName() {
    acquireSharedLock();
    try {
      return defaultClusterName;
    } finally {
      releaseSharedLock();
    }
  }

  public void setDefaultClusterName(
      ODatabaseSessionInternal database, final String defaultClusterName) {
    acquireExclusiveLock();
    try {
      this.defaultClusterName = defaultClusterName;
    } finally {
      releaseExclusiveLock(database);
    }
  }

  public ODictionary<ORecord> getDictionary(ODatabaseSessionInternal database) {
    OIndex idx;
    acquireSharedLock();
    try {
      idx = getIndex(database, DICTIONARY_NAME);
    } finally {
      releaseSharedLock();
    }
    // we lock exclusively only when ODictionary not found
    if (idx == null) {
      idx = createDictionaryIfNeeded(database);
    }
    return new ODictionary<>(idx);
  }

  public ODocument getConfiguration(ODatabaseSessionInternal session) {
    acquireSharedLock();

    try {
      return getDocument(session);
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public void close() {
    indexes.clear();
    classPropertyIndex.clear();
  }

  public Set<OIndex> getClassInvolvedIndexes(
      ODatabaseSessionInternal database, final String className, Collection<String> fields) {
    final OMultiKey multiKey = new OMultiKey(fields);

    final Map<OMultiKey, Set<OIndex>> propertyIndex = getIndexOnProperty(className);

    if (propertyIndex == null || !propertyIndex.containsKey(multiKey)) {
      return Collections.emptySet();
    }

    final Set<OIndex> rawResult = propertyIndex.get(multiKey);
    final Set<OIndex> transactionalResult = new HashSet<>(rawResult.size());
    for (final OIndex index : rawResult) {
      // ignore indexes that ignore null values on partial match
      if (fields.size() == index.getDefinition().getFields().size()
          || !index.getDefinition().isNullValuesIgnored()) {
        transactionalResult.add(index);
      }
    }

    return transactionalResult;
  }

  public Set<OIndex> getClassInvolvedIndexes(
      ODatabaseSessionInternal database, final String className, final String... fields) {
    return getClassInvolvedIndexes(database, className, Arrays.asList(fields));
  }

  public boolean areIndexed(final String className, Collection<String> fields) {
    final OMultiKey multiKey = new OMultiKey(fields);

    final Map<OMultiKey, Set<OIndex>> propertyIndex = getIndexOnProperty(className);

    if (propertyIndex == null) {
      return false;
    }

    return propertyIndex.containsKey(multiKey) && !propertyIndex.get(multiKey).isEmpty();
  }

  public boolean areIndexed(final String className, final String... fields) {
    return areIndexed(className, Arrays.asList(fields));
  }

  public Set<OIndex> getClassIndexes(ODatabaseSessionInternal database, final String className) {
    final HashSet<OIndex> coll = new HashSet<OIndex>(4);
    getClassIndexes(database, className, coll);
    return coll;
  }

  public void getClassIndexes(
      ODatabaseSessionInternal database, final String className, final Collection<OIndex> indexes) {
    final Map<OMultiKey, Set<OIndex>> propertyIndex = getIndexOnProperty(className);

    if (propertyIndex == null) {
      return;
    }

    for (final Set<OIndex> propertyIndexes : propertyIndex.values()) {
      for (final OIndex index : propertyIndexes) {
        indexes.add(index);
      }
    }
  }

  public void getClassRawIndexes(final String className, final Collection<OIndex> indexes) {
    final Map<OMultiKey, Set<OIndex>> propertyIndex = getIndexOnProperty(className);

    if (propertyIndex == null) {
      return;
    }

    for (final Set<OIndex> propertyIndexes : propertyIndex.values()) {
      indexes.addAll(propertyIndexes);
    }
  }

  public OIndexUnique getClassUniqueIndex(final String className) {
    final Map<OMultiKey, Set<OIndex>> propertyIndex = getIndexOnProperty(className);

    if (propertyIndex != null) {
      for (final Set<OIndex> propertyIndexes : propertyIndex.values()) {
        for (final OIndex index : propertyIndexes) {
          if (index instanceof OIndexUnique) {
            return (OIndexUnique) index;
          }
        }
      }
    }

    return null;
  }

  public OIndex getClassIndex(
      ODatabaseSessionInternal database, String className, String indexName) {
    className = className.toLowerCase();

    final OIndex index = indexes.get(indexName);
    if (index != null
        && index.getDefinition() != null
        && index.getDefinition().getClassName() != null
        && className.equals(index.getDefinition().getClassName().toLowerCase())) {
      return index;
    }
    return null;
  }

  public OIndex getClassAutoShardingIndex(ODatabaseSessionInternal database, String className) {
    className = className.toLowerCase();

    // LOOK FOR INDEX
    for (OIndex index : indexes.values()) {
      if (index != null
          && OAutoShardingIndexFactory.AUTOSHARDING_ALGORITHM.equals(index.getAlgorithm())
          && index.getDefinition() != null
          && index.getDefinition().getClassName() != null
          && className.equals(index.getDefinition().getClassName().toLowerCase())) {
        return index;
      }
    }
    return null;
  }

  private void acquireSharedLock() {
    lock.readLock().lock();
  }

  private void releaseSharedLock() {
    lock.readLock().unlock();
  }

  protected void acquireExclusiveLock() {
    internalAcquireExclusiveLock();
    writeLockNesting.incrementAndGet();
  }

  void internalAcquireExclusiveLock() {
    final ODatabaseSessionInternal databaseRecord = getDatabaseIfDefined();
    if (databaseRecord != null && !databaseRecord.isClosed()) {
      final OMetadataInternal metadata = databaseRecord.getMetadata();
      if (metadata != null) {
        metadata.makeThreadLocalSchemaSnapshot();
      }
      databaseRecord.startExclusiveMetadataChange();
    }

    lock.writeLock().lock();
  }

  protected void releaseExclusiveLock(ODatabaseSessionInternal session) {
    releaseExclusiveLock(session, false);
  }

  protected void releaseExclusiveLock(ODatabaseSessionInternal session, boolean save) {
    int val = writeLockNesting.decrementAndGet();
    try {
      if (val == 0) {
        if (save) {
          internalSave(session);
        }
      }
    } finally {
      internalReleaseExclusiveLock();
    }
    if (val == 0) {
      session
          .getSharedContext()
          .getSchema()
          .forceSnapshot(ODatabaseRecordThreadLocal.instance().get());

      for (OMetadataUpdateListener listener : session.getSharedContext().browseListeners()) {
        listener.onIndexManagerUpdate(session, session.getName(), this);
      }
    }
  }

  void internalReleaseExclusiveLock() {
    lock.writeLock().unlock();

    final ODatabaseSessionInternal databaseRecord = getDatabaseIfDefined();
    if (databaseRecord != null && !databaseRecord.isClosed()) {
      databaseRecord.endExclusiveMetadataChange();
      final OMetadata metadata = databaseRecord.getMetadata();
      if (metadata != null) {
        ((OMetadataInternal) metadata).clearThreadLocalSchemaSnapshot();
      }
    }
  }

  void clearMetadata(ODatabaseSessionInternal session) {
    acquireExclusiveLock();
    try {
      indexes.clear();
      classPropertyIndex.clear();
    } finally {
      releaseExclusiveLock(session);
    }
  }

  private static ODatabaseSessionInternal getDatabaseIfDefined() {
    return ODatabaseRecordThreadLocal.instance().getIfDefined();
  }

  void addIndexInternal(ODatabaseSessionInternal session, final OIndex index) {
    acquireExclusiveLock();
    try {
      addIndexInternalNoLock(index);
    } finally {
      releaseExclusiveLock(session);
    }
  }

  private void addIndexInternalNoLock(final OIndex index) {
    indexes.put(index.getName(), index);

    final OIndexDefinition indexDefinition = index.getDefinition();
    if (indexDefinition == null || indexDefinition.getClassName() == null) {
      return;
    }

    Map<OMultiKey, Set<OIndex>> propertyIndex = getIndexOnProperty(indexDefinition.getClassName());

    if (propertyIndex == null) {
      propertyIndex = new HashMap<>();
    } else {
      propertyIndex = new HashMap<>(propertyIndex);
    }

    final int paramCount = indexDefinition.getParamCount();

    for (int i = 1; i <= paramCount; i++) {
      final List<String> fields = indexDefinition.getFields().subList(0, i);
      final OMultiKey multiKey = new OMultiKey(fields);
      Set<OIndex> indexSet = propertyIndex.get(multiKey);

      if (indexSet == null) {
        indexSet = new HashSet<>();
      } else {
        indexSet = new HashSet<>(indexSet);
      }

      indexSet.add(index);
      propertyIndex.put(multiKey, indexSet);
    }

    classPropertyIndex.put(
        indexDefinition.getClassName().toLowerCase(), copyPropertyMap(propertyIndex));
  }

  static Map<OMultiKey, Set<OIndex>> copyPropertyMap(Map<OMultiKey, Set<OIndex>> original) {
    final Map<OMultiKey, Set<OIndex>> result = new HashMap<>();

    for (Map.Entry<OMultiKey, Set<OIndex>> entry : original.entrySet()) {
      Set<OIndex> indexes = new HashSet<>(entry.getValue());
      assert indexes.equals(entry.getValue());

      result.put(entry.getKey(), Collections.unmodifiableSet(indexes));
    }

    assert result.equals(original);

    return Collections.unmodifiableMap(result);
  }

  private OIndex createDictionaryIfNeeded(ODatabaseSessionInternal database) {
    acquireExclusiveLock();
    try {
      OIndex idx = getIndex(database, DICTIONARY_NAME);
      return idx != null ? idx : createDictionary(database);
    } finally {
      releaseExclusiveLock(database, true);
    }
  }

  private OIndex createDictionary(ODatabaseSessionInternal database) {
    return createIndex(
        database,
        DICTIONARY_NAME,
        OClass.INDEX_TYPE.DICTIONARY.toString(),
        new OSimpleKeyIndexDefinition(OType.STRING),
        null,
        null,
        null);
  }

  private Map<OMultiKey, Set<OIndex>> getIndexOnProperty(final String className) {
    acquireSharedLock();
    try {
      return classPropertyIndex.get(className.toLowerCase());

    } finally {
      releaseSharedLock();
    }
  }

  /**
   * Create a new index with default algorithm.
   *
   * @param iName             - name of index
   * @param iType             - index type. Specified by plugged index factories.
   * @param indexDefinition   metadata that describes index structure
   * @param clusterIdsToIndex ids of clusters that index should track for changes.
   * @param progressListener  listener to track task progress.
   * @param metadata          document with additional properties that can be used by index engine.
   * @return a newly created index instance
   */
  public OIndex createIndex(
      ODatabaseSessionInternal database,
      final String iName,
      final String iType,
      final OIndexDefinition indexDefinition,
      final int[] clusterIdsToIndex,
      OProgressListener progressListener,
      ODocument metadata) {
    return createIndex(
        database,
        iName,
        iType,
        indexDefinition,
        clusterIdsToIndex,
        progressListener,
        metadata,
        null);
  }

  /**
   * Create a new index.
   *
   * <p>May require quite a long time if big amount of data should be indexed.
   *
   * @param iName             name of index
   * @param type              index type. Specified by plugged index factories.
   * @param indexDefinition   metadata that describes index structure
   * @param clusterIdsToIndex ids of clusters that index should track for changes.
   * @param progressListener  listener to track task progress.
   * @param metadata          document with additional properties that can be used by index engine.
   * @param algorithm         tip to an index factory what algorithm to use
   * @return a newly created index instance
   */
  public OIndex createIndex(
      ODatabaseSessionInternal database,
      final String iName,
      String type,
      final OIndexDefinition indexDefinition,
      final int[] clusterIdsToIndex,
      OProgressListener progressListener,
      ODocument metadata,
      String algorithm) {

    final boolean manualIndexesAreUsed =
        indexDefinition == null
            || indexDefinition.getClassName() == null
            || indexDefinition.getFields() == null
            || indexDefinition.getFields().isEmpty();
    if (manualIndexesAreUsed) {
      OIndexAbstract.manualIndexesWarning();
    } else {
      checkSecurityConstraintsForIndexCreate(database, indexDefinition);
    }
    if (database.getTransaction().isActive()) {
      throw new IllegalStateException("Cannot create a new index inside a transaction");
    }

    final Character c = OSchemaShared.checkIndexNameIfValid(iName);
    if (c != null) {
      throw new IllegalArgumentException(
          "Invalid index name '" + iName + "'. Character '" + c + "' is invalid");
    }

    if (indexDefinition == null) {
      throw new IllegalArgumentException("Index definition cannot be null");
    }

    type = type.toUpperCase();
    if (algorithm == null) {
      algorithm = OIndexes.chooseDefaultIndexAlgorithm(type);
    }

    final String valueContainerAlgorithm = chooseContainerAlgorithm(type);

    final OIndexInternal index;
    acquireExclusiveLock();
    try {

      if (indexes.containsKey(iName)) {
        throw new OIndexException("Index with name " + iName + " already exists.");
      }

      // manual indexes are always durable
      if (clusterIdsToIndex == null || clusterIdsToIndex.length == 0) {
        if (metadata == null) {
          metadata = new ODocument().setTrackingChanges(false);
        }
        if (metadata.field("trackMode") == null) {
          metadata.field("trackMode", "FULL");
        }
      }

      final Set<String> clustersToIndex = findClustersByIds(clusterIdsToIndex, database);
      Object ignoreNullValues =
          Optional.ofNullable(metadata)
              .map(entries -> entries.field("ignoreNullValues"))
              .orElse(null);
      if (Boolean.TRUE.equals(ignoreNullValues)) {
        indexDefinition.setNullValuesIgnored(true);
      } else if (Boolean.FALSE.equals(ignoreNullValues)) {
        indexDefinition.setNullValuesIgnored(false);
      } else {
        indexDefinition.setNullValuesIgnored(
            storage
                .getConfiguration()
                .getContextConfiguration()
                .getValueAsBoolean(OGlobalConfiguration.INDEX_IGNORE_NULL_VALUES_DEFAULT));
      }

      OIndexMetadata im =
          new OIndexMetadata(
              iName,
              indexDefinition,
              clustersToIndex,
              type,
              algorithm,
              valueContainerAlgorithm,
              -1,
              metadata);

      index = createIndexFromMetadata(database, storage, im, progressListener);

      addIndexInternal(database, index);

    } finally {
      releaseExclusiveLock(database, true);
      notifyInvolvedClasses(database, clusterIdsToIndex);
    }

    return index;
  }

  private OIndexInternal createIndexFromMetadata(
      ODatabaseSessionInternal session, OStorage storage, OIndexMetadata indexMetadata,
      OProgressListener progressListener) {

    OIndexInternal index = OIndexes.createIndex(storage, indexMetadata);
    if (progressListener == null)
    // ASSIGN DEFAULT PROGRESS LISTENER
    {
      progressListener = new OIndexRebuildOutputListener(index);
    }
    indexes.put(index.getName(), index);
    try {
      index.create(session, indexMetadata, true, progressListener);
    } catch (Throwable e) {
      indexes.remove(index.getName());
      throw e;
    }

    return index;
  }

  private static void checkSecurityConstraintsForIndexCreate(
      ODatabaseSessionInternal database, OIndexDefinition indexDefinition) {

    OSecurityInternal security = database.getSharedContext().getSecurity();

    String indexClass = indexDefinition.getClassName();
    List<String> indexedFields = indexDefinition.getFields();
    if (indexedFields.size() == 1) {
      return;
    }

    Set<String> classesToCheck = new HashSet<>();
    classesToCheck.add(indexClass);
    OClass clazz = database.getMetadata().getImmutableSchemaSnapshot().getClass(indexClass);
    if (clazz == null) {
      return;
    }
    clazz.getAllSubclasses().forEach(x -> classesToCheck.add(x.getName()));
    clazz.getAllSuperClasses().forEach(x -> classesToCheck.add(x.getName()));
    Set<OSecurityResourceProperty> allFilteredProperties =
        security.getAllFilteredProperties(database);

    for (String className : classesToCheck) {
      Set<OSecurityResourceProperty> indexedAndFilteredProperties;
      try (Stream<OSecurityResourceProperty> stream = allFilteredProperties.stream()) {
        indexedAndFilteredProperties =
            stream
                .filter(x -> x.getClassName().equalsIgnoreCase(className))
                .filter(x -> indexedFields.contains(x.getPropertyName()))
                .collect(Collectors.toSet());
      }

      if (indexedAndFilteredProperties.size() > 0) {
        try (Stream<OSecurityResourceProperty> stream = indexedAndFilteredProperties.stream()) {
          throw new OIndexException(
              "Cannot create index on "
                  + indexClass
                  + "["
                  + (stream
                  .map(OSecurityResourceProperty::getPropertyName)
                  .collect(Collectors.joining(", ")))
                  + " because of existing column security rules");
        }
      }
    }
  }

  private static void notifyInvolvedClasses(
      ODatabaseSessionInternal database, int[] clusterIdsToIndex) {
    if (clusterIdsToIndex == null || clusterIdsToIndex.length == 0) {
      return;
    }

    // UPDATE INVOLVED CLASSES
    final Set<String> classes = new HashSet<>();
    for (int clusterId : clusterIdsToIndex) {
      final OClass cls = database.getMetadata().getSchema().getClassByClusterId(clusterId);
      if (cls instanceof OClassImpl && !classes.contains(cls.getName())) {
        ((OClassImpl) cls).onPostIndexManagement(database);
        classes.add(cls.getName());
      }
    }
  }

  private static Set<String> findClustersByIds(
      int[] clusterIdsToIndex, ODatabaseSessionInternal database) {
    Set<String> clustersToIndex = new HashSet<>();
    if (clusterIdsToIndex != null) {
      for (int clusterId : clusterIdsToIndex) {
        final String clusterNameToIndex = database.getClusterNameById(clusterId);
        if (clusterNameToIndex == null) {
          throw new OIndexException("Cluster with id " + clusterId + " does not exist.");
        }

        clustersToIndex.add(clusterNameToIndex);
      }
    }
    return clustersToIndex;
  }

  private static String chooseContainerAlgorithm(String type) {
    final String valueContainerAlgorithm;
    if (OClass.INDEX_TYPE.NOTUNIQUE.toString().equals(type)
        || OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString().equals(type)
        || OClass.INDEX_TYPE.FULLTEXT.toString().equals(type)) {
      valueContainerAlgorithm = ODefaultIndexFactory.SBTREE_BONSAI_VALUE_CONTAINER;
    } else {
      valueContainerAlgorithm = ODefaultIndexFactory.NONE_VALUE_CONTAINER;
    }
    return valueContainerAlgorithm;
  }

  public void dropIndex(ODatabaseSessionInternal database, final String iIndexName) {
    if (database.getTransaction().isActive()) {
      throw new IllegalStateException("Cannot drop an index inside a transaction");
    }

    int[] clusterIdsToIndex = null;

    acquireExclusiveLock();

    OIndex idx;
    try {
      idx = indexes.get(iIndexName);
      if (idx != null) {
        final Set<String> clusters = idx.getClusters();
        if (clusters != null && !clusters.isEmpty()) {
          clusterIdsToIndex = new int[clusters.size()];
          int i = 0;
          for (String cl : clusters) {
            clusterIdsToIndex[i++] = database.getClusterIdByName(cl);
          }
        }

        removeClassPropertyIndex(database, idx);

        idx.delete(database);
        indexes.remove(iIndexName);
      }
    } finally {
      releaseExclusiveLock(database, true);
      notifyInvolvedClasses(database, clusterIdsToIndex);
    }
  }

  /**
   * Binds POJO to ODocument.
   */
  public ODocument toStream(ODatabaseSessionInternal session) {
    internalAcquireExclusiveLock();
    try {
      ODocument document = session.load(identity, null, false);
      final OTrackedSet<ODocument> indexes = new OTrackedSet<>(document);

      for (final OIndex i : this.indexes.values()) {
        var indexInternal = (OIndexInternal) i;
        indexes.add(indexInternal.updateConfiguration());
      }
      document.field(CONFIG_INDEXES, indexes, OType.EMBEDDEDSET);
      document.setDirty();

      return document;
    } finally {
      internalReleaseExclusiveLock();
    }
  }

  @Override
  public void recreateIndexes(ODatabaseSessionInternal database) {
    acquireExclusiveLock();
    try {
      if (recreateIndexesThread != null && recreateIndexesThread.isAlive())
      // BUILDING ALREADY IN PROGRESS
      {
        return;
      }

      Runnable recreateIndexesTask = new ORecreateIndexesTask(this, database.getSharedContext());
      recreateIndexesThread = new Thread(recreateIndexesTask, "OxygenDB rebuild indexes");
      recreateIndexesThread.setUncaughtExceptionHandler(new OUncaughtExceptionHandler());
      recreateIndexesThread.start();
    } finally {
      releaseExclusiveLock(database);
    }

    if (database
        .getConfiguration()
        .getValueAsBoolean(OGlobalConfiguration.INDEX_SYNCHRONOUS_AUTO_REBUILD)) {
      waitTillIndexRestore();

      database.getMetadata().reload();
    }
  }

  public void waitTillIndexRestore() {
    if (recreateIndexesThread != null && recreateIndexesThread.isAlive()) {
      if (Thread.currentThread().equals(recreateIndexesThread)) {
        return;
      }

      OLogManager.instance().info(this, "Wait till indexes restore after crash was finished.");
      while (recreateIndexesThread.isAlive()) {
        try {
          recreateIndexesThread.join();
          OLogManager.instance().info(this, "Indexes restore after crash was finished.");
        } catch (InterruptedException e) {
          OLogManager.instance().info(this, "Index rebuild task was interrupted.", e);
        }
      }
    }
  }

  public boolean autoRecreateIndexesAfterCrash(ODatabaseSessionInternal database) {
    if (rebuildCompleted) {
      return false;
    }

    final OStorage storage = database.getStorage();
    if (storage instanceof OAbstractPaginatedStorage paginatedStorage) {
      return paginatedStorage.wereDataRestoredAfterOpen()
          && paginatedStorage.wereNonTxOperationsPerformedInPreviousOpen();
    }

    return false;
  }

  protected void fromStream(ODatabaseSessionInternal session, ODocument document) {
    internalAcquireExclusiveLock();
    try {
      final Map<String, OIndex> oldIndexes = new HashMap<>(indexes);
      clearMetadata(session);

      final Collection<ODocument> indexDocuments = document.field(CONFIG_INDEXES);

      if (indexDocuments != null) {
        OIndexInternal index;
        boolean configUpdated = false;
        Iterator<ODocument> indexConfigurationIterator = indexDocuments.iterator();
        while (indexConfigurationIterator.hasNext()) {
          final ODocument d = indexConfigurationIterator.next();
          try {

            final OIndexMetadata newIndexMetadata = OIndexAbstract.loadMetadataFromDoc(d);

            index = OIndexes.createIndex(storage, newIndexMetadata);

            final String normalizedName = newIndexMetadata.getName();

            OIndex oldIndex = oldIndexes.remove(normalizedName);
            if (oldIndex != null) {
              OIndexMetadata oldIndexMetadata =
                  oldIndex.getInternal().loadMetadata(oldIndex.getConfiguration());

              if (!(oldIndexMetadata.equals(newIndexMetadata)
                  || newIndexMetadata.getIndexDefinition() == null)) {
                oldIndex.delete(session);
              }

              if (index.loadFromConfiguration(session, d)) {
                addIndexInternalNoLock(index);
              } else {
                indexConfigurationIterator.remove();
                configUpdated = true;
              }
            } else {
              if (index.loadFromConfiguration(session, d)) {
                addIndexInternalNoLock(index);
              } else {
                indexConfigurationIterator.remove();
                configUpdated = true;
              }
            }
          } catch (RuntimeException e) {
            indexConfigurationIterator.remove();
            configUpdated = true;
            OLogManager.instance().error(this, "Error on loading index by configuration: %s", e, d);
          }
        }

        for (OIndex oldIndex : oldIndexes.values()) {
          try {
            OLogManager.instance()
                .warn(
                    this,
                    "Index '%s' was not found after reload and will be removed",
                    oldIndex.getName());

            oldIndex.delete(session);
          } catch (Exception e) {
            OLogManager.instance()
                .error(this, "Error on deletion of index '%s'", e, oldIndex.getName());
          }
        }

        if (configUpdated) {
          document.field(CONFIG_INDEXES, indexDocuments);
          save(session);
        }
      }
    } finally {
      internalReleaseExclusiveLock();
    }
  }

  public void removeClassPropertyIndex(ODatabaseSessionInternal session, final OIndex idx) {
    acquireExclusiveLock();
    try {
      final OIndexDefinition indexDefinition = idx.getDefinition();
      if (indexDefinition == null || indexDefinition.getClassName() == null) {
        return;
      }

      Map<OMultiKey, Set<OIndex>> map =
          classPropertyIndex.get(indexDefinition.getClassName().toLowerCase());

      if (map == null) {
        return;
      }

      map = new HashMap<>(map);

      final int paramCount = indexDefinition.getParamCount();

      for (int i = 1; i <= paramCount; i++) {
        final List<String> fields = indexDefinition.getFields().subList(0, i);
        final OMultiKey multiKey = new OMultiKey(fields);

        Set<OIndex> indexSet = map.get(multiKey);
        if (indexSet == null) {
          continue;
        }

        indexSet = new HashSet<>(indexSet);
        indexSet.remove(idx);

        if (indexSet.isEmpty()) {
          map.remove(multiKey);
        } else {
          map.put(multiKey, indexSet);
        }
      }

      if (map.isEmpty()) {
        classPropertyIndex.remove(indexDefinition.getClassName().toLowerCase());
      } else {
        classPropertyIndex.put(indexDefinition.getClassName().toLowerCase(), copyPropertyMap(map));
      }

    } finally {
      releaseExclusiveLock(session);
    }
  }

  public ODocument toNetworkStream() {
    ODocument document = new ODocument();
    internalAcquireExclusiveLock();
    try {
      document.setTrackingChanges(false);
      final OTrackedSet<ODocument> indexes = new OTrackedSet<>(document);

      for (final OIndex i : this.indexes.values()) {
        var indexInternal = (OIndexInternal) i;
        indexes.add(indexInternal.updateConfiguration());
      }
      document.field(CONFIG_INDEXES, indexes, OType.EMBEDDEDSET);

      return document;
    } finally {
      internalReleaseExclusiveLock();
    }
  }

  public OIndex preProcessBeforeReturn(ODatabaseSessionInternal database, final OIndex index) {
    return index;
  }

  protected OStorage getStorage() {
    return storage;
  }

  public ODocument getDocument(ODatabaseSessionInternal session) {
    return toStream(session);
  }

  private void internalSave(ODatabaseSessionInternal session) {
    session.begin();
    ODocument document = toStream(session);
    document.save();
    session.commit();
  }
}
