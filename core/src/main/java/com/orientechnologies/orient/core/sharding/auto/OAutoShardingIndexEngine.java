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
package com.orientechnologies.orient.core.sharding.auto;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.serialization.types.OBinarySerializer;
import com.orientechnologies.common.util.OCommonConst;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.config.IndexEngineData;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.encryption.OEncryption;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.index.OIndexKeyUpdater;
import com.orientechnologies.orient.core.index.OIndexMetadata;
import com.orientechnologies.orient.core.index.OIndexUpdateAction;
import com.orientechnologies.orient.core.index.YTIndexException;
import com.orientechnologies.orient.core.index.engine.IndexEngineValidator;
import com.orientechnologies.orient.core.index.engine.IndexEngineValuesTransformer;
import com.orientechnologies.orient.core.index.engine.OIndexEngine;
import com.orientechnologies.orient.core.iterator.OEmptyIterator;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OHashFunction;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OHashTable;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OMurmurHash3HashFunction;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OSHA256HashFunction;
import com.orientechnologies.orient.core.storage.index.hashindex.local.v2.LocalHashTableV2;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Index engine implementation that relies on multiple hash indexes partitioned by key.
 */
public final class OAutoShardingIndexEngine implements OIndexEngine {

  public static final int VERSION = 1;
  private static final String SUBINDEX_METADATA_FILE_EXTENSION = ".asm";
  private static final String SUBINDEX_TREE_FILE_EXTENSION = ".ast";
  private static final String SUBINDEX_BUCKET_FILE_EXTENSION = ".asb";
  private static final String SUBINDEX_NULL_BUCKET_FILE_EXTENSION = ".asn";

  private final OAbstractPaginatedStorage storage;
  private List<OHashTable<Object, Object>> partitions;
  private OAutoShardingStrategy strategy;
  private final String name;
  private int partitionSize;
  private final AtomicLong bonsayFileId = new AtomicLong(0);
  private final int id;

  OAutoShardingIndexEngine(
      final String iName, int id, final OAbstractPaginatedStorage iStorage, final int iVersion) {
    this.name = iName;
    this.id = id;
    this.storage = iStorage;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  public OAutoShardingStrategy getStrategy() {
    return strategy;
  }

  public void create(OAtomicOperation atomicOperation, IndexEngineData data) throws IOException {

    OBinarySerializer valueSerializer =
        storage.resolveObjectSerializer(data.getValueSerializerId());
    OBinarySerializer keySerializer = storage.resolveObjectSerializer(data.getKeySerializedId());

    final OEncryption encryption =
        OAbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());

    this.strategy = new OAutoShardingMurmurStrategy(keySerializer);

    final OHashFunction<Object> hashFunction;

    if (encryption != null) {
      hashFunction = new OSHA256HashFunction<>(keySerializer);
    } else {
      hashFunction = new OMurmurHash3HashFunction<>(keySerializer);
    }
    Map<String, String> engineProperties = data.getEngineProperties();
    final String partitionsProperty = engineProperties.get("partitions");
    if (partitionsProperty != null) {
      try {
        this.partitionSize = Integer.parseInt(partitionsProperty);
      } catch (NumberFormatException e) {
        OLogManager.instance()
            .error(
                this, "Invalid value of 'partitions' property : `" + partitionsProperty + "`", e);
      }
      engineProperties = new HashMap<>();
      engineProperties.put("partitions", String.valueOf(partitionSize));
    }

    init();

    try {
      for (OHashTable<Object, Object> p : partitions) {
        p.create(
            atomicOperation,
            keySerializer,
            valueSerializer,
            data.getKeyTypes(),
            encryption,
            hashFunction,
            data.isNullValuesSupport());
      }
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during creation of index with name " + name), e);
    }
  }

  @Override
  public void load(IndexEngineData data) {
    OBinarySerializer keySerializer = storage.resolveObjectSerializer(data.getKeySerializedId());
    OBinarySerializer valueSerializer =
        storage.resolveObjectSerializer(data.getValueSerializerId());
    Map<String, String> engineProperties = data.getEngineProperties();

    final OEncryption encryption =
        OAbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());

    this.strategy = new OAutoShardingMurmurStrategy(keySerializer);

    if (storage != null) {
      final String partitionsAsString = engineProperties.get("partitions");
      if (partitionsAsString == null || partitionsAsString.isEmpty()) {
        throw new YTIndexException(
            "Cannot load autosharding index '"
                + data.getName()
                + "' because there is no metadata about the number of partitions");
      }

      partitionSize = Integer.parseInt(partitionsAsString);
      init();

      int i = 0;

      final OHashFunction<Object> hashFunction;

      if (encryption != null) {
        //noinspection unchecked
        hashFunction = new OSHA256HashFunction<>(keySerializer);
      } else {
        //noinspection unchecked
        hashFunction = new OMurmurHash3HashFunction<>(keySerializer);
      }

      for (OHashTable<Object, Object> p : partitions)
      //noinspection unchecked
      {
        p.load(
            data.getName() + "_" + (i++),
            data.getKeyTypes(),
            data.isNullValuesSupport(),
            encryption,
            hashFunction,
            keySerializer,
            valueSerializer);
      }
    }
  }

  @Override
  public void flush() {
    if (partitions != null) {
      for (OHashTable<Object, Object> p : partitions) {
        p.flush();
      }
    }
  }

  @Override
  public void delete(OAtomicOperation atomicOperation) {
    try {
      if (partitions != null) {
        doClearPartitions(atomicOperation);

        for (OHashTable<Object, Object> p : partitions) {
          p.delete(atomicOperation);
        }
      }

    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during deletion of index with name " + name), e);
    }
  }

  private void doClearPartitions(final OAtomicOperation atomicOperation) throws IOException {
    for (OHashTable<Object, Object> p : partitions) {
      final OHashTable.Entry<Object, Object> firstEntry = p.firstEntry();

      if (firstEntry != null) {
        OHashTable.Entry<Object, Object>[] entries = p.ceilingEntries(firstEntry.key);
        while (entries.length > 0) {
          for (final OHashTable.Entry<Object, Object> entry : entries) {
            p.remove(atomicOperation, entry.key);
          }

          entries = p.higherEntries(entries[entries.length - 1].key);
        }
      }

      if (p.isNullKeyIsSupported()) {
        p.remove(atomicOperation, null);
      }
    }
  }

  @Override
  public void init(OIndexMetadata metadata) {
  }

  private void init() {
    if (partitions != null) {
      return;
    }

    partitions = new ArrayList<>(partitionSize);
    for (int i = 0; i < partitionSize; ++i) {
      partitions.add(
          new LocalHashTableV2<>(
              name + "_" + i,
              SUBINDEX_METADATA_FILE_EXTENSION,
              SUBINDEX_TREE_FILE_EXTENSION,
              SUBINDEX_BUCKET_FILE_EXTENSION,
              SUBINDEX_NULL_BUCKET_FILE_EXTENSION,
              storage));
    }
  }

  @Override
  public boolean remove(OAtomicOperation atomicOperation, final Object key) {
    try {
      return getPartition(key).remove(atomicOperation, key) != null;
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException(
              "Error during deletion of key " + key + " of index with name " + name),
          e);
    }
  }

  @Override
  public void clear(OAtomicOperation atomicOperation) {
    try {
      if (partitions != null) {
        doClearPartitions(atomicOperation);
      }
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during clear of index with name " + name), e);
    }
  }

  @Override
  public void close() {
    if (partitions != null) {
      for (OHashTable<Object, Object> p : partitions) {
        p.close();
      }
    }
  }

  @Override
  public Object get(YTDatabaseSessionInternal session, final Object key) {
    return getPartition(key).get(key);
  }

  @Override
  public void put(YTDatabaseSessionInternal session, OAtomicOperation atomicOperation,
      final Object key, final Object value) {
    try {
      getPartition(key).put(atomicOperation, key, value);
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException(
              "Error during insertion of key " + key + " of index with name " + name),
          e);
    }
  }

  @Override
  public void update(
      YTDatabaseSessionInternal session, OAtomicOperation atomicOperation, Object key,
      OIndexKeyUpdater<Object> updater) {
    Object value = get(session, key);
    OIndexUpdateAction<Object> updated = updater.update(value, bonsayFileId);
    if (updated.isChange()) {
      put(session, atomicOperation, key, updated.getValue());
    } else if (updated.isRemove()) {
      remove(atomicOperation, key);
    } else //noinspection StatementWithEmptyBody
      if (updated.isNothing()) {
        // Do Nothing
      }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean validatedPut(
      OAtomicOperation atomicOperation,
      Object key,
      YTRID value,
      IndexEngineValidator<Object, YTRID> validator) {
    try {
      return getPartition(key)
          .validatedPut(atomicOperation, key, value, (IndexEngineValidator) validator);
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException(
              "Error during insertion of key " + key + " of index with name " + name),
          e);
    }
  }

  @Override
  public long size(final IndexEngineValuesTransformer transformer) {
    long counter = 0;

    if (partitions != null) {
      for (OHashTable<Object, Object> p : partitions) {
        if (transformer == null) {
          counter += p.size();
        } else {
          final OHashTable.Entry<Object, Object> firstEntry = p.firstEntry();
          if (firstEntry == null) {
            continue;
          }

          OHashTable.Entry<Object, Object>[] entries = p.ceilingEntries(firstEntry.key);

          while (entries.length > 0) {
            for (OHashTable.Entry<Object, Object> entry : entries) {
              counter += transformer.transformFromValue(entry.value).size();
            }

            entries = p.higherEntries(entries[entries.length - 1].key);
          }
        }
      }
    }
    return counter;
  }

  @Override
  public boolean hasRangeQuerySupport() {
    return false;
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> stream(
      final IndexEngineValuesTransformer valuesTransformer) {
    //noinspection resource
    return partitions.stream()
        .flatMap(
            (partition) ->
                StreamSupport.stream(
                    new HashTableSpliterator(valuesTransformer, partition), false));
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> descStream(
      final IndexEngineValuesTransformer valuesTransformer) {
    throw new UnsupportedOperationException("descCursor");
  }

  @Override
  public Stream<Object> keyStream() {
    return StreamSupport.stream(
        new Spliterator<Object>() {
          private int nextPartition = 1;
          private OHashTable<Object, Object> hashTable;
          private int nextEntriesIndex;
          private OHashTable.Entry<Object, Object>[] entries;

          {
            if (partitions == null || partitions.isEmpty())
            //noinspection unchecked
            {
              entries = OCommonConst.EMPTY_BUCKET_ENTRY_ARRAY;
            } else {
              hashTable = partitions.get(0);
              OHashTable.Entry<Object, Object> firstEntry = hashTable.firstEntry();
              if (firstEntry == null)
              //noinspection unchecked
              {
                entries = OCommonConst.EMPTY_BUCKET_ENTRY_ARRAY;
              } else {
                entries = hashTable.ceilingEntries(firstEntry.key);
              }
            }
          }

          @Override
          public boolean tryAdvance(Consumer<? super Object> action) {
            if (entries.length == 0) {
              return false;
            }

            final OHashTable.Entry<Object, Object> bucketEntry = entries[nextEntriesIndex];
            nextEntriesIndex++;
            if (nextEntriesIndex >= entries.length) {
              entries = hashTable.higherEntries(entries[entries.length - 1].key);
              nextEntriesIndex = 0;

              if (entries.length == 0 && nextPartition < partitions.size()) {
                // GET NEXT PARTITION
                hashTable = partitions.get(nextPartition++);
                OHashTable.Entry<Object, Object> firstEntry = hashTable.firstEntry();
                if (firstEntry == null)
                //noinspection unchecked
                {
                  entries = OCommonConst.EMPTY_BUCKET_ENTRY_ARRAY;
                } else {
                  entries = hashTable.ceilingEntries(firstEntry.key);
                }
              }
            }

            action.accept(bucketEntry.key);
            return true;
          }

          @Override
          public Spliterator<Object> trySplit() {
            return null;
          }

          @Override
          public long estimateSize() {
            return Long.MAX_VALUE;
          }

          @Override
          public int characteristics() {
            return NONNULL;
          }
        },
        false);
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesBetween(
      YTDatabaseSessionInternal session, final Object rangeFrom,
      final boolean fromInclusive,
      final Object rangeTo,
      final boolean toInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    throw new UnsupportedOperationException("iterateEntriesBetween");
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMajor(
      final Object fromKey,
      final boolean isInclusive,
      final boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    throw new UnsupportedOperationException("iterateEntriesMajor");
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMinor(
      Object toKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    throw new UnsupportedOperationException("iterateEntriesMinor");
  }

  @Override
  public boolean acquireAtomicExclusiveLock(final Object key) {
    getPartition(key).acquireAtomicExclusiveLock();
    return false;
  }

  @Override
  public String getIndexNameByKey(final Object key) {
    return getPartition(key).getName();
  }

  @Override
  public void updateUniqueIndexVersion(final Object key) {
    // not implemented
  }

  @Override
  public int getUniqueIndexVersion(final Object key) {
    return 0; // not implemented
  }

  private OHashTable<Object, Object> getPartition(final Object iKey) {
    final int partitionId =
        Optional.ofNullable(iKey)
            .map(key -> strategy.getPartitionsId(key, partitionSize))
            .orElse(0);
    return partitions.get(partitionId);
  }

  private static final class HashTableSpliterator implements Spliterator<ORawPair<Object, YTRID>> {

    private int nextEntriesIndex;
    private OHashTable.Entry<Object, Object>[] entries;
    private final IndexEngineValuesTransformer valuesTransformer;

    private Iterator<YTRID> currentIterator = new OEmptyIterator<>();
    private Object currentKey;
    private final OHashTable hashTable;

    private HashTableSpliterator(
        IndexEngineValuesTransformer valuesTransformer, OHashTable hashTable) {
      this.valuesTransformer = valuesTransformer;
      this.hashTable = hashTable;

      @SuppressWarnings("unchecked")
      OHashTable.Entry<Object, Object> firstEntry = hashTable.firstEntry();
      if (firstEntry == null) {
        //noinspection unchecked
        entries = OCommonConst.EMPTY_BUCKET_ENTRY_ARRAY;
      } else {
        //noinspection unchecked
        entries = hashTable.ceilingEntries(firstEntry.key);
      }

      if (entries.length == 0) {
        currentIterator = null;
      }
    }

    @Override
    public boolean tryAdvance(Consumer<? super ORawPair<Object, YTRID>> action) {
      if (currentIterator == null) {
        return false;
      }

      if (currentIterator.hasNext()) {
        final YTIdentifiable identifiable = currentIterator.next();
        action.accept(new ORawPair<>(currentKey, identifiable.getIdentity()));
        return true;
      }

      while (currentIterator != null && !currentIterator.hasNext()) {
        if (entries.length == 0) {
          currentIterator = null;
          return false;
        }

        final OHashTable.Entry<Object, Object> bucketEntry = entries[nextEntriesIndex];

        currentKey = bucketEntry.key;

        Object value = bucketEntry.value;
        if (valuesTransformer != null) {
          currentIterator = valuesTransformer.transformFromValue(value).iterator();
        } else {
          currentIterator = Collections.singletonList((YTRID) value).iterator();
        }

        nextEntriesIndex++;

        if (nextEntriesIndex >= entries.length) {
          //noinspection unchecked
          entries = hashTable.higherEntries(entries[entries.length - 1].key);

          nextEntriesIndex = 0;
        }
      }

      if (currentIterator != null) {
        final YTIdentifiable identifiable = currentIterator.next();
        action.accept(new ORawPair<>(currentKey, identifiable.getIdentity()));
        return true;
      }

      currentIterator = null;
      return false;
    }

    @Override
    public Spliterator<ORawPair<Object, YTRID>> trySplit() {
      return null;
    }

    @Override
    public long estimateSize() {
      return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
      return NONNULL;
    }
  }

  public boolean hasRidBagTreesSupport() {
    return true;
  }
}
