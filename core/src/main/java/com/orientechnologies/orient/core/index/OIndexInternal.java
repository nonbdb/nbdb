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
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OInvalidIndexEngineIdException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.security.OPropertyAccess;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityResourceProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.tx.OTransactionIndexChangesPerKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Interface to handle index.
 */
public interface OIndexInternal extends OIndex {

  String CONFIG_KEYTYPE = "keyType";
  String CONFIG_AUTOMATIC = "automatic";
  String CONFIG_TYPE = "type";
  String ALGORITHM = "algorithm";
  String VALUE_CONTAINER_ALGORITHM = "valueContainerAlgorithm";
  String CONFIG_NAME = "name";
  String INDEX_DEFINITION = "indexDefinition";
  String INDEX_DEFINITION_CLASS = "indexDefinitionClass";
  String INDEX_VERSION = "indexVersion";
  String METADATA = "metadata";
  String MERGE_KEYS = "mergeKeys";

  Object getCollatingValue(final Object key);

  /**
   * Loads the index giving the configuration.
   *
   * @param session
   * @param iConfig ODocument instance containing the configuration
   */
  boolean loadFromConfiguration(ODatabaseSessionInternal session, ODocument iConfig);

  /**
   * Saves the index configuration to disk.
   *
   * @return The configuration as ODocument instance
   * @see OIndex#getConfiguration(ODatabaseSessionInternal)
   */
  ODocument updateConfiguration(ODatabaseSessionInternal session);

  /**
   * Add given cluster to the list of clusters that should be automatically indexed.
   *
   * @param session
   * @param iClusterName Cluster to add.
   * @return Current index instance.
   */
  OIndex addCluster(ODatabaseSessionInternal session, final String iClusterName);

  /**
   * Remove given cluster from the list of clusters that should be automatically indexed.
   *
   * @param session
   * @param iClusterName Cluster to remove.
   */
  void removeCluster(ODatabaseSessionInternal session, final String iClusterName);

  /**
   * Indicates whether given index can be used to calculate result of
   * {@link com.orientechnologies.orient.core.sql.operator.OQueryOperatorEquality} operators.
   *
   * @return {@code true} if given index can be used to calculate result of
   * {@link com.orientechnologies.orient.core.sql.operator.OQueryOperatorEquality} operators.
   */
  boolean canBeUsedInEqualityOperators();

  boolean hasRangeQuerySupport();

  OIndexMetadata loadMetadata(ODocument iConfig);

  void close();

  /**
   * Returns the index name for a key. The name is always the current index name, but in cases where
   * the index supports key-based sharding.
   *
   * @param key the index key.
   * @return The index name involved
   */
  String getIndexNameByKey(Object key);

  /**
   * Acquires exclusive lock in the active atomic operation running on the current thread for this
   * index.
   *
   * <p>If this index supports a more narrow locking, for example key-based sharding, it may use
   * the provided {@code key} to infer a more narrow lock scope, but that is not a requirement.
   *
   * @param key the index key to lock.
   * @return {@code true} if this index was locked entirely, {@code false} if this index locking is
   * sensitive to the provided {@code key} and only some subset of this index was locked.
   */
  boolean acquireAtomicExclusiveLock(Object key);

  /**
   * @return number of entries in the index.
   */
  long size(ODatabaseSessionInternal session);

  Stream<ORID> getRids(ODatabaseSessionInternal session, final Object key);

  Stream<ORawPair<Object, ORID>> stream(ODatabaseSessionInternal session);

  Stream<ORawPair<Object, ORID>> descStream(ODatabaseSessionInternal session);

  Stream<Object> keyStream();

  /**
   * Returns stream which presents subset of index data between passed in keys.
   *
   * @param session
   * @param fromKey       Lower border of index data.
   * @param fromInclusive Indicates whether lower border should be inclusive or exclusive.
   * @param toKey         Upper border of index data.
   * @param toInclusive   Indicates whether upper border should be inclusive or exclusive.
   * @param ascOrder      Flag which determines whether data iterated by stream should be in
   *                      ascending or descending order.
   * @return Cursor which presents subset of index data between passed in keys.
   */
  Stream<ORawPair<Object, ORID>> streamEntriesBetween(
      ODatabaseSessionInternal session, Object fromKey, boolean fromInclusive, Object toKey,
      boolean toInclusive, boolean ascOrder);

  /**
   * Returns stream which presents data associated with passed in keys.
   *
   * @param session
   * @param keys         Keys data of which should be returned.
   * @param ascSortOrder Flag which determines whether data iterated by stream should be in
   *                     ascending or descending order.
   * @return stream which presents data associated with passed in keys.
   */
  Stream<ORawPair<Object, ORID>> streamEntries(ODatabaseSessionInternal session, Collection<?> keys,
      boolean ascSortOrder);

  /**
   * Returns stream which presents subset of data which associated with key which is greater than
   * passed in key.
   *
   * @param session
   * @param fromKey       Lower border of index data.
   * @param fromInclusive Indicates whether lower border should be inclusive or exclusive.
   * @param ascOrder      Flag which determines whether data iterated by stream should be in
   *                      ascending or descending order.
   * @return stream which presents subset of data which associated with key which is greater than
   * passed in key.
   */
  Stream<ORawPair<Object, ORID>> streamEntriesMajor(
      ODatabaseSessionInternal session, Object fromKey, boolean fromInclusive, boolean ascOrder);

  /**
   * Returns stream which presents subset of data which associated with key which is less than
   * passed in key.
   *
   * @param session
   * @param toKey       Upper border of index data.
   * @param toInclusive Indicates Indicates whether upper border should be inclusive or exclusive.
   * @param ascOrder    Flag which determines whether data iterated by stream should be in ascending
   *                    or descending order.
   * @return stream which presents subset of data which associated with key which is less than
   * passed in key.
   */
  Stream<ORawPair<Object, ORID>> streamEntriesMinor(
      ODatabaseSessionInternal session, Object toKey, boolean toInclusive, boolean ascOrder);

  static OIdentifiable securityFilterOnRead(OIndex idx, OIdentifiable item) {
    if (idx.getDefinition() == null) {
      return item;
    }
    String indexClass = idx.getDefinition().getClassName();
    if (indexClass == null) {
      return item;
    }
    ODatabaseSessionInternal db = ODatabaseRecordThreadLocal.instance().getIfDefined();
    if (db == null) {
      return item;
    }
    OSecurityInternal security = db.getSharedContext().getSecurity();
    if (isReadRestrictedBySecurityPolicy(indexClass, db, security)) {
      try {
        item = item.getRecord();
      } catch (ORecordNotFoundException e) {
        item = null;
      }
    }
    if (item == null) {
      return null;
    }
    if (idx.getDefinition().getFields().size() == 1) {
      String indexProp = idx.getDefinition().getFields().get(0);
      if (isLabelSecurityDefined(db, security, indexClass, indexProp)) {
        try {
          item = item.getRecord();
        } catch (ORecordNotFoundException e) {
          item = null;
        }
        if (item == null) {
          return null;
        }
        if (!(item instanceof ODocument)) {
          return item;
        }
        OPropertyAccess access = ODocumentInternal.getPropertyAccess((ODocument) item);
        if (access != null && !access.isReadable(indexProp)) {
          return null;
        }
      }
    }
    return item;
  }

  static boolean isLabelSecurityDefined(
      ODatabaseSessionInternal database,
      OSecurityInternal security,
      String indexClass,
      String propertyName) {
    Set<String> classesToCheck = new HashSet<>();
    classesToCheck.add(indexClass);
    OClass clazz = database.getClass(indexClass);
    if (clazz == null) {
      return false;
    }
    clazz.getAllSubclasses().forEach(x -> classesToCheck.add(x.getName()));
    clazz.getAllSuperClasses().forEach(x -> classesToCheck.add(x.getName()));
    Set<OSecurityResourceProperty> allFilteredProperties =
        security.getAllFilteredProperties(database);

    for (String className : classesToCheck) {
      Optional<OSecurityResourceProperty> item =
          allFilteredProperties.stream()
              .filter(x -> x.getClassName().equalsIgnoreCase(className))
              .filter(x -> x.getPropertyName().equals(propertyName))
              .findFirst();

      if (item.isPresent()) {
        return true;
      }
    }
    return false;
  }

  static boolean isReadRestrictedBySecurityPolicy(
      String indexClass, ODatabaseSessionInternal db, OSecurityInternal security) {
    if (security.isReadRestrictedBySecurityPolicy(db, "database.class." + indexClass)) {
      return true;
    }

    OClass clazz = db.getClass(indexClass);
    if (clazz != null) {
      Collection<OClass> sub = clazz.getSubclasses();
      for (OClass subClass : sub) {
        if (isReadRestrictedBySecurityPolicy(subClass.getName(), db, security)) {
          return true;
        }
      }
    }

    return false;
  }

  boolean isNativeTxSupported();

  Iterable<OTransactionIndexChangesPerKey.OTransactionIndexEntry> interpretTxKeyChanges(
      OTransactionIndexChangesPerKey changes);

  void doPut(ODatabaseSessionInternal session, OAbstractPaginatedStorage storage, Object key,
      ORID rid)
      throws OInvalidIndexEngineIdException;

  boolean doRemove(ODatabaseSessionInternal session, OAbstractPaginatedStorage storage, Object key,
      ORID rid)
      throws OInvalidIndexEngineIdException;

  boolean doRemove(OAbstractPaginatedStorage storage, Object key)
      throws OInvalidIndexEngineIdException;

  Stream<ORID> getRidsIgnoreTx(ODatabaseSessionInternal session, Object key);

  OIndex create(ODatabaseSessionInternal session, OIndexMetadata metadata, boolean rebuild,
      OProgressListener progressListener);

  int getIndexId();
}
