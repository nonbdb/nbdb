/*
 * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.lucene.index;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.lucene.OLuceneIndex;
import com.orientechnologies.lucene.engine.OLuceneIndexEngine;
import com.orientechnologies.lucene.tx.OLuceneTxChanges;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OInvalidIndexEngineIdException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.IndexStreamSecurityDecorator;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndexAbstract;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.index.OIndexMetadata;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.orientechnologies.orient.core.tx.OTransaction;
import com.orientechnologies.orient.core.tx.OTransactionIndexChanges;
import com.orientechnologies.orient.core.tx.OTransactionIndexChangesPerKey;
import com.orientechnologies.orient.core.tx.OTransactionIndexChangesPerKey.OTransactionIndexEntry;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

public class OLuceneIndexNotUnique extends OIndexAbstract implements OLuceneIndex {

  public OLuceneIndexNotUnique(OIndexMetadata im, final OStorage storage) {
    super(im, storage);
  }

  @Override
  public long rebuild(OProgressListener iProgressListener) {
    return super.rebuild(iProgressListener);
  }

  @Override
  public boolean remove(final Object key, final OIdentifiable rid) {

    ODatabaseSessionInternal database = getDatabase();
    if (key != null) {
      OTransaction transaction = database.getTransaction();
      if (transaction.isActive()) {

        transaction.addIndexEntry(
            this, super.getName(), OTransactionIndexChanges.OPERATION.REMOVE, encodeKey(key), rid);
        OLuceneTxChanges transactionChanges = getTransactionChanges(transaction);
        transactionChanges.remove(key, rid);
        return true;
      } else {
        database.begin();
        transaction.addIndexEntry(
            this, super.getName(), OTransactionIndexChanges.OPERATION.REMOVE, encodeKey(key), rid);
        OLuceneTxChanges transactionChanges = getTransactionChanges(transaction);
        transactionChanges.remove(key, rid);
        database.commit();
      }
    }
    return true;
  }

  @Override
  public boolean remove(final Object key) {
    if (key != null) {
      ODatabaseSessionInternal database = getDatabase();
      OTransaction transaction = database.getTransaction();
      if (transaction.isActive()) {

        transaction.addIndexEntry(
            this, super.getName(), OTransactionIndexChanges.OPERATION.REMOVE, encodeKey(key), null);
        OLuceneTxChanges transactionChanges = getTransactionChanges(transaction);
        transactionChanges.remove(key, null);
        return true;
      } else {
        database.begin();
        try {
          transaction = database.getTransaction();
          transaction.addIndexEntry(
              this,
              super.getName(),
              OTransactionIndexChanges.OPERATION.REMOVE,
              encodeKey(key),
              null);
          OLuceneTxChanges transactionChanges = getTransactionChanges(transaction);
          transactionChanges.remove(key, null);
          database.commit();
        } catch (RuntimeException e) {
          database.rollback();
          throw e;
        }
      }
    }
    return true;
  }

  @Override
  public OIndexAbstract removeCluster(String iClusterName) {
    acquireExclusiveLock();
    try {
      if (clustersToIndex.remove(iClusterName)) {
        updateConfiguration();
        remove("_CLUSTER:" + storage.getClusterIdByName(iClusterName));
      }

      return this;
    } finally {
      releaseExclusiveLock();
    }
  }

  @Override
  public Iterable<OTransactionIndexEntry> interpretTxKeyChanges(
      OTransactionIndexChangesPerKey changes) {
    return changes.interpret(OTransactionIndexChangesPerKey.Interpretation.NonUnique);
  }

  @Override
  public void doPut(OAbstractPaginatedStorage storage, Object key, ORID rid) {
    while (true) {
      try {
        storage.callIndexEngine(
            false,
            indexId,
            engine -> {
              try {
                OLuceneIndexEngine indexEngine = (OLuceneIndexEngine) engine;

                OAtomicOperation atomicOperation =
                    storage.getAtomicOperationsManager().getCurrentOperation();
                indexEngine.put(atomicOperation, decodeKey(key), rid);
                return null;
              } catch (IOException e) {
                throw OException.wrapException(
                    new OIndexException("Error during commit of index changes"), e);
              }
            });
        break;
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public boolean doRemove(OAbstractPaginatedStorage storage, Object key)
      throws OInvalidIndexEngineIdException {
    while (true) {
      try {
        storage.callIndexEngine(
            false,
            indexId,
            engine -> {
              OLuceneIndexEngine indexEngine = (OLuceneIndexEngine) engine;
              indexEngine.remove(decodeKey(key));
              return true;
            });
        break;
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
    return false;
  }

  @Override
  public boolean doRemove(OAbstractPaginatedStorage storage, Object key, ORID rid)
      throws OInvalidIndexEngineIdException {
    while (true) {
      try {
        storage.callIndexEngine(
            false,
            indexId,
            engine -> {
              OLuceneIndexEngine indexEngine = (OLuceneIndexEngine) engine;
              indexEngine.remove(decodeKey(key), rid);
              return true;
            });
        break;
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
    return false;
  }

  @Override
  public Object getCollatingValue(Object key) {
    return key;
  }

  public void doDelete() {
    while (true) {
      try {
        storage.deleteIndexEngine(indexId);
        break;
      } catch (OInvalidIndexEngineIdException ignore) {
        doReloadIndexEngine();
      }
    }
  }

  protected Object decodeKey(Object key) {
    return key;
  }

  protected void populateIndex(ODocument doc, Object fieldValue) {
    if (fieldValue instanceof Collection) {
      for (final Object fieldValueItem : (Collection<?>) fieldValue) {
        put(fieldValueItem, doc);
      }
    } else {
      put(fieldValue, doc);
    }
  }

  @Override
  protected void onIndexEngineChange(int indexId) {
    while (true) {
      try {
        storage.callIndexEngine(
            false,
            indexId,
            engine -> {
              OLuceneIndexEngine oIndexEngine = (OLuceneIndexEngine) engine;
              oIndexEngine.init(im);
              return null;
            });
        break;
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  protected Object encodeKey(Object key) {
    return key;
  }

  private OLuceneTxChanges getTransactionChanges(OTransaction transaction) {

    OLuceneTxChanges changes = (OLuceneTxChanges) transaction.getCustomData(getName());
    if (changes == null) {
      while (true) {
        try {
          changes =
              storage.callIndexEngine(
                  false,
                  indexId,
                  engine -> {
                    OLuceneIndexEngine indexEngine = (OLuceneIndexEngine) engine;
                    try {
                      return indexEngine.buildTxChanges();
                    } catch (IOException e) {
                      throw OException.wrapException(
                          new OIndexException("Cannot get searcher from index " + getName()), e);
                    }
                  });
          break;
        } catch (OInvalidIndexEngineIdException e) {
          doReloadIndexEngine();
        }
      }

      transaction.setCustomData(getName(), changes);
    }
    return changes;
  }

  @Deprecated
  @Override
  public Collection<OIdentifiable> get(final Object key) {
    try (Stream<ORID> stream = getRids(key)) {
      return stream.collect(Collectors.toList());
    }
  }

  @Override
  public Stream<ORID> getRidsIgnoreTx(Object key) {
    while (true) {
      try {
        @SuppressWarnings("unchecked")
        Set<OIdentifiable> result = (Set<OIdentifiable>) storage.getIndexValue(indexId, key);
        //noinspection resource
        return result.stream().map(OIdentifiable::getIdentity);
        // TODO filter these results based on security
        //          return new HashSet(OIndexInternal.securityFilterOnRead(this, result));
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public Stream<ORID> getRids(Object key) {
    final OTransaction transaction = getDatabase().getTransaction();
    if (transaction.isActive()) {
      while (true) {
        try {
          //noinspection resource
          return storage
              .callIndexEngine(
                  false,
                  indexId,
                  engine -> {
                    OLuceneIndexEngine indexEngine = (OLuceneIndexEngine) engine;
                    return indexEngine.getInTx(key, getTransactionChanges(transaction));
                  })
              .stream()
              .map(OIdentifiable::getIdentity);
        } catch (OInvalidIndexEngineIdException e) {
          doReloadIndexEngine();
        }
      }

    } else {
      while (true) {
        try {
          @SuppressWarnings("unchecked")
          Set<OIdentifiable> result = (Set<OIdentifiable>) storage.getIndexValue(indexId, key);
          //noinspection resource
          return result.stream().map(OIdentifiable::getIdentity);
          // TODO filter these results based on security
          //          return new HashSet(OIndexInternal.securityFilterOnRead(this, result));
        } catch (OInvalidIndexEngineIdException e) {
          doReloadIndexEngine();
        }
      }
    }
  }

  @Override
  public OLuceneIndexNotUnique put(final Object key, final OIdentifiable value) {
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
    if (key != null) {
      ODatabaseSessionInternal db = getDatabase();
      OTransaction transaction = db.getTransaction();

      if (transaction.isActive()) {
        OLuceneTxChanges transactionChanges = getTransactionChanges(transaction);
        transaction.addIndexEntry(
            this, super.getName(), OTransactionIndexChanges.OPERATION.PUT, encodeKey(key), value);

        Document luceneDoc;
        while (true) {
          try {
            luceneDoc =
                storage.callIndexEngine(
                    false,
                    indexId,
                    engine -> {
                      OLuceneIndexEngine oIndexEngine = (OLuceneIndexEngine) engine;
                      return oIndexEngine.buildDocument(key, value);
                    });
            break;
          } catch (OInvalidIndexEngineIdException e) {
            doReloadIndexEngine();
          }
        }

        transactionChanges.put(key, value, luceneDoc);

      } else {
        db.begin();
        OTransaction singleTx = db.getTransaction();
        singleTx.addIndexEntry(
            this, super.getName(), OTransactionIndexChanges.OPERATION.PUT, encodeKey(key), value);
        db.commit();
      }
    }
    return this;
  }

  @Override
  public long size() {
    var database = getDatabase();
    return database.computeInTx(
        () -> {
          while (true) {
            try {
              return storage.callIndexEngine(
                  false,
                  indexId,
                  engine -> {
                    OTransaction transaction = getDatabase().getTransaction();
                    OLuceneIndexEngine indexEngine = (OLuceneIndexEngine) engine;
                    return indexEngine.sizeInTx(getTransactionChanges(transaction));
                  });
            } catch (OInvalidIndexEngineIdException e) {
              doReloadIndexEngine();
            }
          }
        });
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntries(Collection<?> keys, boolean ascSortOrder) {

    @SuppressWarnings("resource")
    String query =
        (String)
            keys.stream()
                .findFirst()
                .map(k -> (OCompositeKey) k)
                .map(OCompositeKey::getKeys)
                .orElse(Collections.singletonList("q=*:*"))
                .get(0);
    return IndexStreamSecurityDecorator.decorateStream(
        this, getRids(query).map((rid) -> new ORawPair<>(query, rid)));
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntriesBetween(
      Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive, boolean ascOrder) {
    while (true) {
      try {
        return IndexStreamSecurityDecorator.decorateStream(
            this,
            storage.iterateIndexEntriesBetween(
                indexId, fromKey, fromInclusive, toKey, toInclusive, ascOrder, null));
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntriesMajor(
      Object fromKey, boolean fromInclusive, boolean ascOrder) {
    while (true) {
      try {
        return IndexStreamSecurityDecorator.decorateStream(
            this,
            storage.iterateIndexEntriesMajor(indexId, fromKey, fromInclusive, ascOrder, null));
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public Stream<ORawPair<Object, ORID>> streamEntriesMinor(
      Object toKey, boolean toInclusive, boolean ascOrder) {
    while (true) {
      try {
        return IndexStreamSecurityDecorator.decorateStream(
            this, storage.iterateIndexEntriesMinor(indexId, toKey, toInclusive, ascOrder, null));
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public boolean isNativeTxSupported() {
    return false;
  }

  @Override
  public Stream<ORawPair<Object, ORID>> stream() {
    while (true) {
      try {
        return IndexStreamSecurityDecorator.decorateStream(
            this, storage.getIndexStream(indexId, null));
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public Stream<ORawPair<Object, ORID>> descStream() {
    while (true) {
      try {
        return IndexStreamSecurityDecorator.decorateStream(
            this, storage.getIndexStream(indexId, null));
      } catch (OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public boolean supportsOrderedIterations() {
    return false;
  }

  @Override
  public IndexSearcher searcher() {
    while (true) {
      try {
        return storage.callIndexEngine(
            false,
            indexId,
            engine -> {
              final OLuceneIndexEngine indexEngine = (OLuceneIndexEngine) engine;
              return indexEngine.searcher();
            });
      } catch (final OInvalidIndexEngineIdException e) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public boolean canBeUsedInEqualityOperators() {
    return false;
  }
}
