package com.jetbrains.youtrack.db.internal.core.index.engine.v1;

import com.jetbrains.youtrack.db.internal.common.exception.YTException;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OBinarySerializer;
import com.jetbrains.youtrack.db.internal.common.util.ORawPair;
import com.jetbrains.youtrack.db.internal.core.config.IndexEngineData;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.encryption.OEncryption;
import com.jetbrains.youtrack.db.internal.core.exception.YTStorageException;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.index.OCompositeKey;
import com.jetbrains.youtrack.db.internal.core.index.OIndexMetadata;
import com.jetbrains.youtrack.db.internal.core.index.YTIndexException;
import com.jetbrains.youtrack.db.internal.core.index.engine.IndexEngineValuesTransformer;
import com.jetbrains.youtrack.db.internal.core.index.engine.OMultiValueIndexEngine;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.OCompactedLinkSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.index.CompositeKeySerializer;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.multivalue.OCellBTreeMultiValue;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.multivalue.v2.CellBTreeMultiValueV2;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.singlevalue.OCellBTreeSingleValue;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.singlevalue.v3.CellBTreeSingleValueV3;
import java.io.IOException;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;

public final class OCellBTreeMultiValueIndexEngine
    implements OMultiValueIndexEngine, OCellBTreeIndexEngine {

  public static final String DATA_FILE_EXTENSION = ".cbt";
  private static final String NULL_BUCKET_FILE_EXTENSION = ".nbt";
  public static final String M_CONTAINER_EXTENSION = ".mbt";

  private final OCellBTreeMultiValue<Object> mvTree;

  private final OCellBTreeSingleValue<OCompositeKey> svTree;
  private final OCellBTreeSingleValue<YTIdentifiable> nullTree;

  private final String name;
  private final int id;
  private final String nullTreeName;
  private final AbstractPaginatedStorage storage;

  public OCellBTreeMultiValueIndexEngine(
      int id, @Nonnull String name, AbstractPaginatedStorage storage, final int version) {
    this.id = id;
    this.name = name;
    this.storage = storage;
    nullTreeName = name + "$null";

    if (version == 1) {
      throw new IllegalArgumentException("Unsupported version of index : " + version);
    } else if (version == 2) {
      this.mvTree =
          new CellBTreeMultiValueV2<>(
              name,
              DATA_FILE_EXTENSION,
              NULL_BUCKET_FILE_EXTENSION,
              M_CONTAINER_EXTENSION,
              storage);
      this.svTree = null;
      this.nullTree = null;
    } else if (version == 3) {
      throw new IllegalArgumentException("Unsupported version of index : " + version);
    } else if (version == 4) {
      mvTree = null;
      svTree =
          new CellBTreeSingleValueV3<>(
              name, DATA_FILE_EXTENSION, NULL_BUCKET_FILE_EXTENSION, storage);
      nullTree =
          new CellBTreeSingleValueV3<>(
              nullTreeName, DATA_FILE_EXTENSION, NULL_BUCKET_FILE_EXTENSION, storage);
    } else {
      throw new IllegalStateException("Invalid tree version " + version);
    }
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void init(OIndexMetadata metadata) {
  }

  @Override
  public void flush() {
  }

  @Override
  public String getName() {
    return name;
  }

  public void create(OAtomicOperation atomicOperation, IndexEngineData data) throws IOException {

    OBinarySerializer keySerializer = storage.resolveObjectSerializer(data.getKeySerializedId());

    final OEncryption encryption =
        AbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());
    try {
      if (mvTree != null) {
        mvTree.create(
            keySerializer, data.getKeyTypes(), data.getKeySize(), encryption, atomicOperation);
      } else {
        final YTType[] sbTypes = calculateTypes(data.getKeyTypes());
        assert svTree != null;
        assert nullTree != null;

        svTree.create(
            atomicOperation,
            new CompositeKeySerializer(),
            sbTypes,
            data.getKeySize() + 1,
            encryption);
        nullTree.create(
            atomicOperation, OCompactedLinkSerializer.INSTANCE, new YTType[]{YTType.LINK}, 1, null);
      }
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during creation of index " + name), e);
    }
  }

  @Override
  public void delete(OAtomicOperation atomicOperation) {
    try {
      if (mvTree != null) {
        doClearMVTree(atomicOperation);
        mvTree.delete(atomicOperation);
      } else {
        assert svTree != null;
        assert nullTree != null;

        doClearSVTree(atomicOperation);
        svTree.delete(atomicOperation);
        nullTree.delete(atomicOperation);
      }
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during deletion of index " + name), e);
    }
  }

  private void doClearMVTree(final OAtomicOperation atomicOperation) {
    assert mvTree != null;

    final Object firstKey = mvTree.firstKey();
    final Object lastKey = mvTree.lastKey();

    try (Stream<ORawPair<Object, YTRID>> stream =
        mvTree.iterateEntriesBetween(firstKey, true, lastKey, true, true)) {
      stream.forEach(
          (pair) -> {
            try {
              mvTree.remove(atomicOperation, pair.first, pair.second);
            } catch (IOException e) {
              throw YTException.wrapException(
                  new YTIndexException("Error during cleaning of index " + name), e);
            }
          });
    }

    try (final Stream<YTRID> rids = mvTree.get(null)) {
      rids.forEach(
          (rid) -> {
            try {
              mvTree.remove(atomicOperation, null, rid);
            } catch (final IOException e) {
              throw YTException.wrapException(
                  new YTStorageException("Error during cleaning of index " + name), e);
            }
          });
    }
  }

  private void doClearSVTree(final OAtomicOperation atomicOperation) {
    assert svTree != null;
    assert nullTree != null;

    {
      final OCompositeKey firstKey = svTree.firstKey();
      final OCompositeKey lastKey = svTree.lastKey();

      try (Stream<ORawPair<OCompositeKey, YTRID>> stream =
          svTree.iterateEntriesBetween(firstKey, true, lastKey, true, true)) {
        stream.forEach(
            (pair) -> {
              try {
                svTree.remove(atomicOperation, pair.first);
              } catch (IOException e) {
                throw YTException.wrapException(
                    new YTIndexException("Error during index cleaning"), e);
              }
            });
      }
    }

    {
      final YTIdentifiable firstKey = nullTree.firstKey();
      final YTIdentifiable lastKey = nullTree.lastKey();

      if (firstKey != null && lastKey != null) {
        try (Stream<ORawPair<YTIdentifiable, YTRID>> stream =
            nullTree.iterateEntriesBetween(firstKey, true, lastKey, true, true)) {
          stream.forEach(
              (pair) -> {
                try {
                  nullTree.remove(atomicOperation, pair.first);
                } catch (IOException e) {
                  throw YTException.wrapException(
                      new YTIndexException("Error during index cleaning"), e);
                }
              });
        }
      }
    }
  }

  @Override
  public void load(IndexEngineData data) {
    final OEncryption encryption =
        AbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());

    String name = data.getName();
    int keySize = data.getKeySize();
    YTType[] keyTypes = data.getKeyTypes();
    OBinarySerializer keySerializer = storage.resolveObjectSerializer(data.getKeySerializedId());

    if (mvTree != null) {
      //noinspection unchecked
      mvTree.load(name, keySize, keyTypes, keySerializer, encryption);
    } else {
      assert svTree != null;
      assert nullTree != null;

      final YTType[] sbTypes = calculateTypes(keyTypes);

      svTree.load(name, keySize + 1, sbTypes, new CompositeKeySerializer(), null);
      nullTree.load(
          nullTreeName, 1, new YTType[]{YTType.LINK}, OCompactedLinkSerializer.INSTANCE, null);
    }
  }

  @Override
  public boolean remove(final OAtomicOperation atomicOperation, Object key, YTRID value) {
    try {
      if (mvTree != null) {
        return mvTree.remove(atomicOperation, key, value);
      } else {
        if (key != null) {
          assert svTree != null;

          final OCompositeKey compositeKey = createCompositeKey(key, value);

          final boolean[] removed = new boolean[1];
          try (Stream<ORawPair<OCompositeKey, YTRID>> stream =
              svTree.iterateEntriesBetween(compositeKey, true, compositeKey, true, true)) {
            stream.forEach(
                (pair) -> {
                  try {
                    final boolean result = svTree.remove(atomicOperation, pair.first) != null;
                    removed[0] = result || removed[0];
                  } catch (final IOException e) {
                    throw YTException.wrapException(
                        new YTIndexException(
                            "Error during remove of entry (" + key + ", " + value + ")"),
                        e);
                  }
                });
          }

          return removed[0];
        } else {
          assert nullTree != null;
          return nullTree.remove(atomicOperation, value) != null;
        }
      }
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException(
              "Error during removal of entry with key "
                  + key
                  + "and RID "
                  + value
                  + " from index "
                  + name),
          e);
    }
  }

  @Override
  public void clear(OAtomicOperation atomicOperation) {
    if (mvTree != null) {
      doClearMVTree(atomicOperation);
    } else {
      doClearSVTree(atomicOperation);
    }
  }

  @Override
  public void close() {
    if (mvTree != null) {
      mvTree.close();
    } else {
      assert svTree != null;
      assert nullTree != null;

      svTree.close();
      nullTree.close();
    }
  }

  @Override
  public Stream<YTRID> get(Object key) {
    if (mvTree != null) {
      return mvTree.get(key);
    } else if (key != null) {
      assert svTree != null;

      final OCompositeKey firstKey = convertToCompositeKey(key);
      final OCompositeKey lastKey = convertToCompositeKey(key);

      //noinspection resource
      return svTree
          .iterateEntriesBetween(firstKey, true, lastKey, true, true)
          .map((pair) -> pair.second);
    } else {
      assert nullTree != null;

      //noinspection resource
      return nullTree
          .iterateEntriesBetween(
              new YTRecordId(0, 0), true, new YTRecordId(Short.MAX_VALUE, Long.MAX_VALUE), true,
              true)
          .map((pair) -> pair.second);
    }
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> stream(IndexEngineValuesTransformer valuesTransformer) {
    if (mvTree != null) {
      final Object firstKey = mvTree.firstKey();
      if (firstKey == null) {
        return emptyStream();
      }

      return mvTree.iterateEntriesMajor(firstKey, true, true);
    } else {
      assert svTree != null;

      final OCompositeKey firstKey = svTree.firstKey();
      if (firstKey == null) {
        return emptyStream();
      }

      return mapSVStream(svTree.iterateEntriesMajor(firstKey, true, true));
    }
  }

  private static Stream<ORawPair<Object, YTRID>> mapSVStream(
      Stream<ORawPair<OCompositeKey, YTRID>> stream) {
    return stream.map((entry) -> new ORawPair<>(extractKey(entry.first), entry.second));
  }

  private static Stream<ORawPair<Object, YTRID>> emptyStream() {
    return StreamSupport.stream(Spliterators.emptySpliterator(), false);
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> descStream(
      IndexEngineValuesTransformer valuesTransformer) {
    if (mvTree != null) {
      final Object lastKey = mvTree.lastKey();
      if (lastKey == null) {
        return emptyStream();
      }
      return mvTree.iterateEntriesMinor(lastKey, true, false);
    } else {
      assert svTree != null;

      final OCompositeKey lastKey = svTree.lastKey();
      if (lastKey == null) {
        return emptyStream();
      }
      return mapSVStream(svTree.iterateEntriesMinor(lastKey, true, false));
    }
  }

  @Override
  public Stream<Object> keyStream() {
    if (mvTree != null) {
      return mvTree.keyStream();
    }

    assert svTree != null;
    //noinspection resource
    return svTree.keyStream().map(OCellBTreeMultiValueIndexEngine::extractKey);
  }

  @Override
  public void put(OAtomicOperation atomicOperation, Object key, YTRID value) {
    if (mvTree != null) {
      try {
        mvTree.put(atomicOperation, key, value);
      } catch (IOException e) {
        throw YTException.wrapException(
            new YTIndexException(
                "Error during insertion of key " + key + " and RID " + value + " to index " + name),
            e);
      }
    } else if (key != null) {
      assert svTree != null;
      try {
        svTree.put(atomicOperation, createCompositeKey(key, value), value);
      } catch (IOException e) {
        throw YTException.wrapException(
            new YTIndexException(
                "Error during insertion of key " + key + " and RID " + value + " to index " + name),
            e);
      }
    } else {
      assert nullTree != null;
      try {
        nullTree.put(atomicOperation, value, value);
      } catch (IOException e) {
        throw YTException.wrapException(
            new YTIndexException(
                "Error during insertion of null key and RID " + value + " to index " + name),
            e);
      }
    }
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesBetween(
      YTDatabaseSessionInternal session, Object rangeFrom,
      boolean fromInclusive,
      Object rangeTo,
      boolean toInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    if (mvTree != null) {
      return mvTree.iterateEntriesBetween(
          rangeFrom, fromInclusive, rangeTo, toInclusive, ascSortOrder);
    }
    assert svTree != null;

    // "from", "to" are null, then scan whole tree as for infinite range
    if (rangeFrom == null && rangeTo == null) {
      return mapSVStream(svTree.allEntries());
    }

    // "from" could be null, then "to" is not (minor)
    final OCompositeKey toKey = convertToCompositeKey(rangeTo);
    if (rangeFrom == null) {
      return mapSVStream(svTree.iterateEntriesMinor(toKey, toInclusive, ascSortOrder));
    }
    final OCompositeKey fromKey = convertToCompositeKey(rangeFrom);
    // "to" could be null, then "from" is not (major)
    if (rangeTo == null) {
      return mapSVStream(svTree.iterateEntriesMajor(fromKey, fromInclusive, ascSortOrder));
    }
    return mapSVStream(
        svTree.iterateEntriesBetween(fromKey, fromInclusive, toKey, toInclusive, ascSortOrder));
  }

  private static OCompositeKey convertToCompositeKey(Object rangeFrom) {
    OCompositeKey firstKey;
    if (rangeFrom instanceof OCompositeKey) {
      firstKey = (OCompositeKey) rangeFrom;
    } else {
      firstKey = new OCompositeKey(rangeFrom);
    }
    return firstKey;
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMajor(
      Object fromKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    if (mvTree != null) {
      return mvTree.iterateEntriesMajor(fromKey, isInclusive, ascSortOrder);
    }
    assert svTree != null;

    final OCompositeKey firstKey = convertToCompositeKey(fromKey);
    return mapSVStream(svTree.iterateEntriesMajor(firstKey, isInclusive, ascSortOrder));
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMinor(
      Object toKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    if (mvTree != null) {
      return mvTree.iterateEntriesMinor(toKey, isInclusive, ascSortOrder);
    }
    assert svTree != null;

    final OCompositeKey lastKey = convertToCompositeKey(toKey);
    return mapSVStream(svTree.iterateEntriesMinor(lastKey, isInclusive, ascSortOrder));
  }

  @Override
  public long size(final IndexEngineValuesTransformer transformer) {
    if (mvTree != null) {
      return mvTreeSize(transformer);
    }

    assert svTree != null;
    assert nullTree != null;

    return svTreeEntries();
  }

  private long mvTreeSize(final IndexEngineValuesTransformer transformer) {
    assert mvTree != null;

    // calculate amount of keys
    if (transformer == null) {
      final Object firstKey = mvTree.firstKey();
      final Object lastKey = mvTree.lastKey();

      int counter = 0;

      try (Stream<YTRID> oridStream = mvTree.get(null)) {
        if (oridStream.iterator().hasNext()) {
          counter++;
        }
      }

      if (firstKey != null && lastKey != null) {
        final Object[] prevKey = new Object[]{new Object()};
        try (final Stream<ORawPair<Object, YTRID>> stream =
            mvTree.iterateEntriesBetween(firstKey, true, lastKey, true, true)) {
          counter +=
              stream
                  .filter(
                      (pair) -> {
                        final boolean result = !prevKey[0].equals(pair.first);
                        prevKey[0] = pair.first;
                        return result;
                      })
                  .count();
        }
      }
      return counter;
    }
    // calculate amount of entries
    return mvTree.size();
  }

  private long svTreeEntries() {
    assert svTree != null;
    assert nullTree != null;
    return svTree.size() + nullTree.size();
  }

  @Override
  public boolean hasRangeQuerySupport() {
    return true;
  }

  @Override
  public boolean acquireAtomicExclusiveLock(Object key) {
    if (mvTree != null) {
      mvTree.acquireAtomicExclusiveLock();
    } else {
      assert svTree != null;
      assert nullTree != null;

      svTree.acquireAtomicExclusiveLock();
      nullTree.acquireAtomicExclusiveLock();
    }

    return true;
  }

  @Override
  public String getIndexNameByKey(Object key) {
    return name;
  }

  @Override
  public void updateUniqueIndexVersion(final Object key) {
    // not implemented
  }

  @Override
  public int getUniqueIndexVersion(final Object key) {
    return 0; // not implemented
  }

  private static YTType[] calculateTypes(final YTType[] keyTypes) {
    final YTType[] sbTypes;
    if (keyTypes != null) {
      sbTypes = new YTType[keyTypes.length + 1];
      System.arraycopy(keyTypes, 0, sbTypes, 0, keyTypes.length);
      sbTypes[sbTypes.length - 1] = YTType.LINK;
    } else {
      throw new YTIndexException("Types of fields should be provided upon of creation of index");
    }
    return sbTypes;
  }

  private static OCompositeKey createCompositeKey(final Object key, final YTRID value) {
    final OCompositeKey compositeKey = new OCompositeKey(key);
    compositeKey.addKey(value);
    return compositeKey;
  }

  private static Object extractKey(final OCompositeKey compositeKey) {
    if (compositeKey == null) {
      return null;
    }
    final List<Object> keys = compositeKey.getKeys();

    final Object key;
    if (keys.size() == 2) {
      key = keys.get(0);
    } else {
      key = new OCompositeKey(keys.subList(0, keys.size() - 1));
    }
    return key;
  }
}