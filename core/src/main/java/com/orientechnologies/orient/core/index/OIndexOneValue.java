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

import com.orientechnologies.common.comparator.ODefaultComparator;
import com.orientechnologies.common.stream.Streams;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OInvalidIndexEngineIdException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.comparator.AscComparator;
import com.orientechnologies.orient.core.index.comparator.DescComparator;
import com.orientechnologies.orient.core.index.iterator.PureTxBetweenIndexBackwardSpliterator;
import com.orientechnologies.orient.core.index.iterator.PureTxBetweenIndexForwardSpliterator;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.tx.OTransaction;
import com.orientechnologies.orient.core.tx.OTransactionIndexChanges;
import com.orientechnologies.orient.core.tx.OTransactionIndexChanges.OPERATION;
import com.orientechnologies.orient.core.tx.OTransactionIndexChangesPerKey;
import com.orientechnologies.orient.core.tx.OTransactionIndexChangesPerKey.OTransactionIndexEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Abstract Index implementation that allows only one value for a key.
 */
public abstract class OIndexOneValue extends OIndexAbstract {

  public OIndexOneValue(OIndexMetadata im, final OStorage storage) {
    super(im, storage);
  }

  @Deprecated
  @Override
  public Object get(ODatabaseSessionInternal session, Object key) {
    final Iterator<ORID> iterator;
    try (Stream<ORID> stream = getRids(session, key)) {
      iterator = stream.iterator();
      if (iterator.hasNext()) {
        return iterator.next();
      }
    }

    return null;
  }

  @Override
  public Stream<ORID> getRidsIgnoreTx(ODatabaseSessionInternal session, Object key) {
    key = getCollatingValue(key);

    acquireSharedLock();
    Stream<ORID> stream;
    try {
      while (true) {
        try {
          if (apiVersion == 0) {
            final ORID rid = (ORID) storage.getIndexValue(session, indexId, key);
            stream = Stream.ofNullable(rid);
          } else if (apiVersion == 1) {
            stream = storage.getIndexValues(indexId, key);
          } else {
            throw new IllegalStateException("Unknown version of index API " + apiVersion);
          }
          stream = IndexStreamSecurityDecorator.decorateRidStream(this, stream);
          break;
        } catch (OInvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }
    return stream;
  }

  @Override
  public Stream<ORID> getRids(ODatabaseSessionInternal session, Object key) {
    key = getCollatingValue(key);

    Stream<ORID> stream = getRidsIgnoreTx(session, key);

    final OTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    ORID rid;
    if (!indexChanges.cleared) {
      // BEGIN FROM THE UNDERLYING RESULT SET
      //noinspection resource
      rid = stream.findFirst().orElse(null);
    } else {
      rid = null;
    }

    final ORawPair<Object, ORID> txIndexEntry = calculateTxIndexEntry(key, rid, indexChanges);
    if (txIndexEntry == null) {
      return Stream.empty();
    }

    return IndexStreamSecurityDecorator.decorateRidStream(this, Stream.of(txIndexEntry.second));
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntries(ODatabaseSessionInternal session,
      Collection<?> keys, boolean ascSortOrder) {
    final List<Object> sortedKeys = new ArrayList<>(keys);
    final Comparator<Object> comparator;

    if (ascSortOrder) {
      comparator = ODefaultComparator.INSTANCE;
    } else {
      comparator = Collections.reverseOrder(ODefaultComparator.INSTANCE);
    }

    sortedKeys.sort(comparator);

    //noinspection resource
    Stream<ORawPair<Object, ORID>> stream =
        IndexStreamSecurityDecorator.decorateStream(
            this,
            sortedKeys.stream()
                .flatMap(
                    (key) -> {
                      final Object collatedKey = getCollatingValue(key);

                      acquireSharedLock();
                      try {
                        while (true) {
                          try {
                            if (apiVersion == 0) {
                              final ORID rid = (ORID) storage.getIndexValue(session, indexId,
                                  collatedKey);
                              if (rid == null) {
                                return Stream.empty();
                              }
                              return Stream.of(new ORawPair<>(collatedKey, rid));
                            } else if (apiVersion == 1) {
                              return storage
                                  .getIndexValues(indexId, collatedKey)
                                  .map((rid) -> new ORawPair<>(collatedKey, rid));
                            } else {
                              throw new IllegalStateException(
                                  "Invalid version of index API - " + apiVersion);
                            }
                          } catch (OInvalidIndexEngineIdException ignore) {
                            doReloadIndexEngine();
                          }
                        }
                      } finally {
                        releaseSharedLock();
                      }
                    })
                .filter(Objects::nonNull));
    final OTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }
    Comparator<ORawPair<Object, ORID>> keyComparator;
    if (ascSortOrder) {
      keyComparator = AscComparator.INSTANCE;
    } else {
      keyComparator = DescComparator.INSTANCE;
    }

    @SuppressWarnings("resource") final Stream<ORawPair<Object, ORID>> txStream =
        keys.stream()
            .map((key) -> calculateTxIndexEntry(getCollatingValue(key), null, indexChanges))
            .filter(Objects::nonNull)
            .sorted(keyComparator);

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascSortOrder));
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntriesBetween(
      ODatabaseSessionInternal session, Object fromKey, boolean fromInclusive, Object toKey,
      boolean toInclusive, boolean ascOrder) {
    fromKey = getCollatingValue(fromKey);
    toKey = getCollatingValue(toKey);
    Stream<ORawPair<Object, ORID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this,
                  storage.iterateIndexEntriesBetween(session,
                      indexId, fromKey, fromInclusive, toKey, toInclusive, ascOrder, null));
          break;
        } catch (OInvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final OTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<ORawPair<Object, ORID>> txStream;
    if (ascOrder) {
      //noinspection resource
      txStream =
          StreamSupport.stream(
              new PureTxBetweenIndexForwardSpliterator(
                  this, fromKey, fromInclusive, toKey, toInclusive, indexChanges),
              false);
    } else {
      //noinspection resource
      txStream =
          StreamSupport.stream(
              new PureTxBetweenIndexBackwardSpliterator(
                  this, fromKey, fromInclusive, toKey, toInclusive, indexChanges),
              false);
    }

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascOrder));
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntriesMajor(
      ODatabaseSessionInternal session, Object fromKey, boolean fromInclusive, boolean ascOrder) {
    fromKey = getCollatingValue(fromKey);
    Stream<ORawPair<Object, ORID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this,
                  storage.iterateIndexEntriesMajor(
                      indexId, fromKey, fromInclusive, ascOrder, null));
          break;
        } catch (OInvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final OTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    fromKey = getCollatingValue(fromKey);

    final Stream<ORawPair<Object, ORID>> txStream;

    final Object lastKey = indexChanges.getLastKey();
    if (ascOrder) {
      txStream =
          StreamSupport.stream(
              new PureTxBetweenIndexForwardSpliterator(
                  this, fromKey, fromInclusive, lastKey, true, indexChanges),
              false);
    } else {
      txStream =
          StreamSupport.stream(
              new PureTxBetweenIndexBackwardSpliterator(
                  this, fromKey, fromInclusive, lastKey, true, indexChanges),
              false);
    }

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascOrder));
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntriesMinor(
      ODatabaseSessionInternal session, Object toKey, boolean toInclusive, boolean ascOrder) {
    toKey = getCollatingValue(toKey);
    Stream<ORawPair<Object, ORID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this,
                  storage.iterateIndexEntriesMinor(indexId, toKey, toInclusive, ascOrder, null));
          break;
        } catch (OInvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }

    } finally {
      releaseSharedLock();
    }
    final OTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    toKey = getCollatingValue(toKey);

    final Stream<ORawPair<Object, ORID>> txStream;

    final Object firstKey = indexChanges.getFirstKey();
    if (ascOrder) {
      txStream =
          StreamSupport.stream(
              new PureTxBetweenIndexForwardSpliterator(
                  this, firstKey, true, toKey, toInclusive, indexChanges),
              false);
    } else {
      txStream =
          StreamSupport.stream(
              new PureTxBetweenIndexBackwardSpliterator(
                  this, firstKey, true, toKey, toInclusive, indexChanges),
              false);
    }

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascOrder));
  }

  public long size(ODatabaseSessionInternal session) {
    acquireSharedLock();
    try {
      while (true) {
        try {
          return storage.getIndexSize(indexId, null);
        } catch (OInvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public Stream<ORawPair<Object, ORID>> stream(ODatabaseSessionInternal session) {
    Stream<ORawPair<Object, ORID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this, storage.getIndexStream(indexId, null));
          break;
        } catch (OInvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final OTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<ORawPair<Object, ORID>> txStream =
        StreamSupport.stream(
            new PureTxBetweenIndexForwardSpliterator(this, null, true, null, true, indexChanges),
            false);
    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, true));
  }

  @Override
  public Stream<ORawPair<Object, ORID>> descStream(ODatabaseSessionInternal session) {
    Stream<ORawPair<Object, ORID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this, storage.getIndexDescStream(indexId, null));
          break;
        } catch (OInvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final OTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<ORawPair<Object, ORID>> txStream =
        StreamSupport.stream(
            new PureTxBetweenIndexBackwardSpliterator(this, null, true, null, true, indexChanges),
            false);
    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, false));
  }

  @Override
  public boolean isUnique() {
    return true;
  }

  public ORawPair<Object, ORID> calculateTxIndexEntry(
      Object key, final ORID backendValue, final OTransactionIndexChanges indexChanges) {
    key = getCollatingValue(key);
    ORID result = backendValue;
    final OTransactionIndexChangesPerKey changesPerKey = indexChanges.getChangesPerKey(key);
    if (changesPerKey.isEmpty()) {
      if (backendValue == null) {
        return null;
      } else {
        return new ORawPair<>(key, backendValue);
      }
    }

    for (OTransactionIndexEntry entry : changesPerKey.getEntriesAsList()) {
      if (entry.getOperation() == OPERATION.REMOVE) {
        result = null;
      } else if (entry.getOperation() == OPERATION.PUT) {
        result = entry.getValue().getIdentity();
      }
    }

    if (result == null) {
      return null;
    }

    return new ORawPair<>(key, result);
  }

  private Stream<ORawPair<Object, ORID>> mergeTxAndBackedStreams(
      OTransactionIndexChanges indexChanges,
      Stream<ORawPair<Object, ORID>> txStream,
      Stream<ORawPair<Object, ORID>> backedStream,
      boolean ascSortOrder) {
    Comparator<ORawPair<Object, ORID>> comparator;
    if (ascSortOrder) {
      comparator = AscComparator.INSTANCE;
    } else {
      comparator = DescComparator.INSTANCE;
    }

    return Streams.mergeSortedSpliterators(
        txStream,
        backedStream
            .map(
                (entry) ->
                    calculateTxIndexEntry(
                        getCollatingValue(entry.first), entry.second, indexChanges))
            .filter(Objects::nonNull),
        comparator);
  }

  @Override
  public OIndexOneValue put(ODatabaseSessionInternal session, Object key,
      final OIdentifiable value) {
    final ORID rid = value.getIdentity();

    if (!rid.isValid()) {
      if (value instanceof ORecord) {
        // EARLY SAVE IT
        ((ORecord) value).save();
      } else {
        throw new IllegalArgumentException(
            "Cannot store non persistent RID as index value for key '" + key + "'");
      }
    }
    key = getCollatingValue(key);

    OTransaction singleTx = session.getTransaction();
    singleTx.addIndexEntry(
        this, super.getName(), OTransactionIndexChanges.OPERATION.PUT, key, value.getIdentity());
    return this;
  }
}
