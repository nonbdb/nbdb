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
package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.internal.common.listener.OProgressListener;
import com.jetbrains.youtrack.db.internal.common.profiler.OProfiler;
import com.jetbrains.youtrack.db.internal.common.profiler.ProfilerStub;
import com.jetbrains.youtrack.db.internal.common.util.ORawPair;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBManager;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.index.OIndex;
import com.jetbrains.youtrack.db.internal.core.index.OIndexCursor;
import com.jetbrains.youtrack.db.internal.core.index.OIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.OIndexInternal;
import com.jetbrains.youtrack.db.internal.core.index.OIndexKeyCursor;
import com.jetbrains.youtrack.db.internal.core.index.OIndexMetadata;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemField;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemField.FieldChain;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionIndexChangesPerKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * There are some cases when we need to create index for some class by traversed property.
 * Unfortunately, such functionality is not supported yet. But we can do that by creating index for
 * each element of {@link OSQLFilterItemField.FieldChain} (which define "way" to our property), and
 * then process operations consequently using previously created indexes.
 *
 * <p>This class provides possibility to find optimal chain of indexes and then use it just like it
 * was index for traversed property.
 *
 * <p>IMPORTANT: this class is only for internal usage!
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class OChainedIndexProxy<T> implements OIndexInternal {

  private final OIndex firstIndex;

  private final List<OIndex> indexChain;
  private final OIndex lastIndex;
  private final YTDatabaseSessionInternal session;

  private OChainedIndexProxy(YTDatabaseSessionInternal session, List<OIndex> indexChain) {
    this.session = session;
    this.firstIndex = indexChain.get(0);
    this.indexChain = Collections.unmodifiableList(indexChain);
    lastIndex = indexChain.get(indexChain.size() - 1);
  }

  /**
   * Create proxies that support maximum number of different operations. In case when several
   * different indexes which support different operations (e.g. indexes of {@code UNIQUE} and
   * {@code FULLTEXT} types) are possible, the creates the only one index of each type.
   *
   * @param session
   * @param longChain - property chain from the query, which should be evaluated
   * @return proxies needed to process query.
   */
  public static <T> Collection<OChainedIndexProxy<T>> createProxies(
      YTDatabaseSessionInternal session, YTClass iSchemaClass, FieldChain longChain) {
    List<OChainedIndexProxy<T>> proxies = new ArrayList<>();

    for (List<OIndex> indexChain : getIndexesForChain(session, iSchemaClass, longChain)) {
      //noinspection ObjectAllocationInLoop
      proxies.add(new OChainedIndexProxy<>(session, indexChain));
    }

    return proxies;
  }

  private static boolean isComposite(OIndex currentIndex) {
    return currentIndex.getDefinition().getParamCount() > 1;
  }

  private static Iterable<List<OIndex>> getIndexesForChain(
      YTDatabaseSessionInternal session, YTClass iSchemaClass, FieldChain fieldChain) {
    List<OIndex> baseIndexes = prepareBaseIndexes(session, iSchemaClass, fieldChain);

    if (baseIndexes == null) {
      return Collections.emptyList();
    }

    Collection<OIndex> lastIndexes = prepareLastIndexVariants(session, iSchemaClass, fieldChain);

    Collection<List<OIndex>> result = new ArrayList<>();
    for (OIndex lastIndex : lastIndexes) {
      @SuppressWarnings("ObjectAllocationInLoop") final List<OIndex> indexes = new ArrayList<>(
          fieldChain.getItemCount());
      indexes.addAll(baseIndexes);
      indexes.add(lastIndex);

      result.add(indexes);
    }

    return result;
  }

  private static Collection<OIndex> prepareLastIndexVariants(
      YTDatabaseSessionInternal session, YTClass iSchemaClass, FieldChain fieldChain) {
    YTClass oClass = iSchemaClass;
    final Collection<OIndex> result = new ArrayList<>();

    for (int i = 0; i < fieldChain.getItemCount() - 1; i++) {
      oClass = oClass.getProperty(fieldChain.getItemName(i)).getLinkedClass();
      if (oClass == null) {
        return result;
      }
    }

    final Set<OIndex> involvedIndexes =
        new TreeSet<>(Comparator.comparingInt(o -> o.getDefinition().getParamCount()));

    involvedIndexes.addAll(
        oClass.getInvolvedIndexes(session, fieldChain.getItemName(fieldChain.getItemCount() - 1)));
    final Collection<Class<? extends OIndex>> indexTypes = new HashSet<>(3);

    for (OIndex involvedIndex : involvedIndexes) {
      if (!indexTypes.contains(involvedIndex.getInternal().getClass())) {
        result.add(involvedIndex);
        indexTypes.add(involvedIndex.getInternal().getClass());
      }
    }

    return result;
  }

  private static List<OIndex> prepareBaseIndexes(
      YTDatabaseSessionInternal session, YTClass iSchemaClass, FieldChain fieldChain) {
    List<OIndex> result = new ArrayList<>(fieldChain.getItemCount() - 1);

    YTClass oClass = iSchemaClass;
    for (int i = 0; i < fieldChain.getItemCount() - 1; i++) {
      final Set<OIndex> involvedIndexes = oClass.getInvolvedIndexes(session,
          fieldChain.getItemName(i));
      final OIndex bestIndex = findBestIndex(involvedIndexes);

      if (bestIndex == null) {
        return null;
      }

      result.add(bestIndex);
      oClass = oClass.getProperty(fieldChain.getItemName(i)).getLinkedClass();
    }
    return result;
  }

  /**
   * Finds the index that fits better as a base index in chain. Requirements to the base index:
   *
   * <ul>
   *   <li>Should be unique or not unique. Other types cannot be used to get all documents with
   *       required links.
   *   <li>Should not be composite hash index. As soon as hash index does not support partial match
   *       search.
   *   <li>Composite index that ignores null values should not be used.
   *   <li>Hash index is better than tree based indexes.
   *   <li>Non composite indexes is better that composite.
   * </ul>
   *
   * @param indexes where search
   * @return the index that fits better as a base index in chain
   */
  protected static OIndex findBestIndex(Iterable<OIndex> indexes) {
    OIndex bestIndex = null;
    for (OIndex index : indexes) {
      if (priorityOfUsage(index) > priorityOfUsage(bestIndex)) {
        bestIndex = index;
      }
    }
    return bestIndex;
  }

  private static int priorityOfUsage(OIndex index) {
    if (index == null) {
      return -1;
    }

    final YTClass.INDEX_TYPE indexType = YTClass.INDEX_TYPE.valueOf(index.getType());
    final boolean isComposite = isComposite(index);
    final boolean supportNullValues = supportNullValues(index);

    int priority = 1;

    if (isComposite) {
      if (!supportNullValues) {
        return -1;
      }
    } else {
      priority += 10;
    }

    switch (indexType) {
      case UNIQUE_HASH_INDEX:
      case NOTUNIQUE_HASH_INDEX:
        if (isComposite) {
          return -1;
        } else {
          priority += 10;
        }
        break;
      case UNIQUE:
      case NOTUNIQUE:
        priority += 5;
        break;
      case PROXY:
      case FULLTEXT:
        //noinspection deprecation
      case DICTIONARY:
      case DICTIONARY_HASH_INDEX:
      case SPATIAL:
        return -1;
    }

    return priority;
  }

  /**
   * Checks if index can be used as base index. Requirements to the base index:
   *
   * <ul>
   *   <li>Should be unique or not unique. Other types cannot be used to get all documents with
   *       required links.
   *   <li>Should not be composite hash index. As soon as hash index does not support partial match
   *       search.
   *   <li>Composite index that ignores null values should not be used.
   * </ul>
   *
   * @param index to check
   * @return true if index usage is allowed as base index.
   */
  public static boolean isAppropriateAsBase(OIndex index) {
    return priorityOfUsage(index) > 0;
  }

  private static boolean supportNullValues(OIndex index) {
    var metadata = index.getMetadata();
    if (metadata == null) {
      return false;
    }

    final Boolean ignoreNullValues = (Boolean) metadata.get("ignoreNullValues");
    return Boolean.FALSE.equals(ignoreNullValues);
  }

  public String getDatabaseName() {
    return firstIndex.getDatabaseName();
  }

  public List<String> getIndexNames() {
    final ArrayList<String> names = new ArrayList<>(indexChain.size());
    for (OIndex oIndex : indexChain) {
      names.add(oIndex.getName());
    }

    return names;
  }

  @Override
  public String getName() {
    final StringBuilder res = new StringBuilder("IndexChain{");
    final List<String> indexNames = getIndexNames();

    for (int i = 0; i < indexNames.size(); i++) {
      String indexName = indexNames.get(i);
      if (i > 0) {
        res.append(", ");
      }
      res.append(indexName);
    }

    res.append("}");

    return res.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public T get(YTDatabaseSessionInternal session, Object key) {
    final List<YTRID> lastIndexResult;
    try (Stream<YTRID> stream = lastIndex.getInternal().getRids(session, key)) {
      lastIndexResult = stream.collect(Collectors.toList());
    }

    final Set<YTIdentifiable> result = new HashSet<>(
        applyTailIndexes(this.session, lastIndexResult));
    return (T) result;
  }

  @Override
  public Stream<YTRID> getRidsIgnoreTx(YTDatabaseSessionInternal session, Object key) {
    final List<YTRID> lastIndexResult;
    try (Stream<YTRID> stream = lastIndex.getInternal().getRids(session, key)) {
      lastIndexResult = stream.collect(Collectors.toList());
    }

    final Set<YTIdentifiable> result = new HashSet<>(
        applyTailIndexes(this.session, lastIndexResult));
    return result.stream().map(YTIdentifiable::getIdentity);
  }

  @Override
  public Stream<YTRID> getRids(YTDatabaseSessionInternal session, Object key) {
    final List<YTRID> lastIndexResult;
    try (Stream<YTRID> stream = lastIndex.getInternal().getRids(session, key)) {
      lastIndexResult = stream.collect(Collectors.toList());
    }

    final Set<YTIdentifiable> result = new HashSet<>(
        applyTailIndexes(this.session, lastIndexResult));
    return result.stream().map(YTIdentifiable::getIdentity);
  }

  /**
   * Returns internal index of last chain index, because proxy applicable to all operations that
   * last index applicable.
   */
  public OIndexInternal getInternal() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public OIndexDefinition getDefinition() {
    return lastIndex.getDefinition();
  }

  private List<YTRID> applyTailIndexes(
      YTDatabaseSessionInternal session, final Object lastIndexResult) {
    final OIndex beforeTheLastIndex = indexChain.get(indexChain.size() - 2);
    Set<Comparable> currentKeys = prepareKeys(session, beforeTheLastIndex, lastIndexResult);

    for (int j = indexChain.size() - 2; j > 0; j--) {
      final OIndex currentIndex = indexChain.get(j);
      final OIndex nextIndex = indexChain.get(j - 1);

      final Set<Comparable> newKeys;
      if (isComposite(currentIndex)) {
        //noinspection ObjectAllocationInLoop
        newKeys = new TreeSet<>();
        for (Comparable currentKey : currentKeys) {
          final List<YTRID> currentResult = getFromCompositeIndex(session, currentKey,
              currentIndex);
          newKeys.addAll(prepareKeys(session, nextIndex, currentResult));
        }
      } else {
        final List<YTIdentifiable> keys;
        try (Stream<ORawPair<Object, YTRID>> stream =
            currentIndex.getInternal().streamEntries(session, currentKeys, true)) {
          keys = stream.map((pair) -> pair.second).collect(Collectors.toList());
        }
        newKeys = prepareKeys(session, nextIndex, keys);
      }

      updateStatistic(currentIndex);

      currentKeys = newKeys;
    }

    return applyFirstIndex(session, currentKeys);
  }

  private List<YTRID> applyFirstIndex(YTDatabaseSessionInternal session,
      Collection<Comparable> currentKeys) {
    final List<YTRID> result;
    if (isComposite(firstIndex)) {
      result = new ArrayList<>();
      for (Comparable key : currentKeys) {
        result.addAll(getFromCompositeIndex(session, key, firstIndex));
      }
    } else {
      try (Stream<ORawPair<Object, YTRID>> stream =
          firstIndex.getInternal().streamEntries(session, currentKeys, true)) {
        result = stream.map((pair) -> pair.second).collect(Collectors.toList());
      }
    }

    updateStatistic(firstIndex);

    return result;
  }

  private static List<YTRID> getFromCompositeIndex(YTDatabaseSessionInternal session,
      Comparable currentKey, OIndex currentIndex) {
    try (Stream<ORawPair<Object, YTRID>> stream =
        currentIndex.getInternal()
            .streamEntriesBetween(session, currentKey, true, currentKey, true, true)) {
      return stream.map((pair) -> pair.second).collect(Collectors.toList());
    }
  }

  /**
   * Make type conversion of keys for specific index.
   *
   * @param session
   * @param index   - index for which keys prepared for.
   * @param keys    - which should be prepared.
   * @return keys converted to necessary type.
   */
  private static Set<Comparable> prepareKeys(
      YTDatabaseSessionInternal session, OIndex index, Object keys) {
    final OIndexDefinition indexDefinition = index.getDefinition();
    if (keys instanceof Collection) {
      final Set<Comparable> newKeys = new TreeSet<>();
      for (Object o : ((Collection) keys)) {
        newKeys.add((Comparable) indexDefinition.createValue(session, o));
      }
      return newKeys;
    } else {
      return Collections.singleton((Comparable) indexDefinition.createValue(session, keys));
    }
  }

  /**
   * Register statistic information about usage of index in {@link ProfilerStub}.
   *
   * @param index which usage is registering.
   */
  private static void updateStatistic(OIndex index) {

    final OProfiler profiler = YouTrackDBManager.instance().getProfiler();
    if (profiler.isRecording()) {
      YouTrackDBManager.instance()
          .getProfiler()
          .updateCounter(
              profiler.getDatabaseMetric(index.getDatabaseName(), "query.indexUsed"),
              "Used index in query",
              +1);

      final int paramCount = index.getDefinition().getParamCount();
      if (paramCount > 1) {
        final String profiler_prefix =
            profiler.getDatabaseMetric(index.getDatabaseName(), "query.compositeIndexUsed");
        profiler.updateCounter(profiler_prefix, "Used composite index in query", +1);
        profiler.updateCounter(
            profiler_prefix + "." + paramCount,
            "Used composite index in query with " + paramCount + " params",
            +1);
      }
    }
  }

  //
  // Following methods are not allowed for proxy.
  //

  @Override
  public OIndex create(
      YTDatabaseSessionInternal session, OIndexMetadata indexMetadat, boolean rebuild,
      OProgressListener progressListener) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public YTType[] getKeyTypes() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public Iterator<Map.Entry<Object, T>> iterator() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public OIndex put(YTDatabaseSessionInternal session, Object key, YTIdentifiable value) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public boolean remove(YTDatabaseSessionInternal session, Object key) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public boolean remove(YTDatabaseSessionInternal session, Object key, YTIdentifiable rid) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated Manual indexes are deprecated and will be removed
   */
  @Deprecated
  @Override
  public OIndex clear(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public long getSize(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long count(YTDatabaseSessionInternal session, Object iKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getKeySize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void flush() {
  }

  @Override
  public long getRebuildVersion() {
    return 0;
  }

  @Override
  public boolean isRebuilding() {
    return false;
  }

  @Override
  public Object getFirstKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getLastKey(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OIndexCursor cursor(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OIndexCursor descCursor(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OIndexKeyCursor keyCursor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getCollatingValue(Object key) {
    return this.lastIndex.getInternal().getCollatingValue(key);
  }

  @Override
  public boolean loadFromConfiguration(YTDatabaseSessionInternal session, EntityImpl iConfig) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityImpl updateConfiguration(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OIndex addCluster(YTDatabaseSessionInternal session, String iClusterName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeCluster(YTDatabaseSessionInternal session, String iClusterName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean canBeUsedInEqualityOperators() {
    return this.lastIndex.getInternal().canBeUsedInEqualityOperators();
  }

  @Override
  public boolean hasRangeQuerySupport() {
    return this.lastIndex.getInternal().hasRangeQuerySupport();
  }

  @Override
  public OIndexMetadata loadMetadata(EntityImpl iConfig) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getIndexNameByKey(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean acquireAtomicExclusiveLock(Object key) {
    throw new UnsupportedOperationException();
  }

  public long size(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public OIndex delete(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public String getType() {
    return lastIndex.getType();
  }

  @Override
  public String getAlgorithm() {
    return lastIndex.getAlgorithm();
  }

  public boolean isAutomatic() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public long rebuild(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public long rebuild(YTDatabaseSessionInternal session, OProgressListener iProgressListener) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public EntityImpl getConfiguration(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public Map<String, ?> getMetadata() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public Set<String> getClusters() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public OIndexCursor iterateEntries(YTDatabaseSessionInternal session, Collection<?> keys,
      boolean ascSortOrder) {
    return null;
  }

  @Override
  public OIndexCursor iterateEntriesBetween(
      YTDatabaseSessionInternal session, Object fromKey, boolean fromInclusive, Object toKey,
      boolean toInclusive, boolean ascOrder) {
    return null;
  }

  @Override
  public OIndexCursor iterateEntriesMajor(YTDatabaseSessionInternal session, Object fromKey,
      boolean fromInclusive, boolean ascOrder) {
    return null;
  }

  @Override
  public OIndexCursor iterateEntriesMinor(YTDatabaseSessionInternal session, Object toKey,
      boolean toInclusive, boolean ascOrder) {
    return null;
  }

  @Override
  public int getIndexId() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public boolean isUnique() {
    return firstIndex.isUnique();
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> stream(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> descStream(YTDatabaseSessionInternal session) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public Stream<Object> keyStream() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public int getVersion() {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  public boolean supportsOrderedIterations() {
    return false;
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> streamEntries(YTDatabaseSessionInternal session,
      Collection<?> keys, boolean ascSortOrder) {
    return applyTailIndexes(lastIndex.getInternal().streamEntries(session, keys, ascSortOrder));
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> streamEntriesBetween(
      YTDatabaseSessionInternal session, Object fromKey, boolean fromInclusive, Object toKey,
      boolean toInclusive, boolean ascOrder) {
    return applyTailIndexes(
        lastIndex
            .getInternal()
            .streamEntriesBetween(session, fromKey, fromInclusive, toKey, toInclusive, ascOrder));
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> streamEntriesMajor(
      YTDatabaseSessionInternal session, Object fromKey, boolean fromInclusive, boolean ascOrder) {
    return applyTailIndexes(
        lastIndex.getInternal().streamEntriesMajor(session, fromKey, fromInclusive, ascOrder));
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> streamEntriesMinor(
      YTDatabaseSessionInternal session, Object toKey, boolean toInclusive, boolean ascOrder) {
    return applyTailIndexes(
        lastIndex.getInternal().streamEntriesMinor(session, toKey, toInclusive, ascOrder));
  }

  @Override
  public boolean isNativeTxSupported() {
    return false;
  }

  @Override
  public Iterable<OTransactionIndexChangesPerKey.OTransactionIndexEntry> interpretTxKeyChanges(
      OTransactionIndexChangesPerKey changes) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public void doPut(YTDatabaseSessionInternal session, AbstractPaginatedStorage storage,
      Object key,
      YTRID rid) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public boolean doRemove(YTDatabaseSessionInternal session, AbstractPaginatedStorage storage,
      Object key, YTRID rid) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  @Override
  public boolean doRemove(AbstractPaginatedStorage storage, Object key) {
    throw new UnsupportedOperationException("Not allowed operation");
  }

  private Stream<ORawPair<Object, YTRID>> applyTailIndexes(
      Stream<ORawPair<Object, YTRID>> indexStream) {
    //noinspection resource
    return indexStream.flatMap(
        (entry) ->
            applyTailIndexes(session, entry.second).stream()
                .map((rid) -> new ORawPair<>(null, rid)));
  }

  @Override
  public int compareTo(OIndex o) {
    throw new UnsupportedOperationException();
  }
}
