package com.orientechnologies.orient.core.storage.index.sbtree.multivalue;

import com.orientechnologies.common.serialization.types.OBinarySerializer;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.encryption.OEncryption;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import java.io.IOException;
import java.util.stream.Stream;

public interface OCellBTreeMultiValue<K> {

  void create(
      OBinarySerializer<K> keySerializer,
      YTType[] keyTypes,
      int keySize,
      OEncryption encryption,
      OAtomicOperation atomicOperation)
      throws IOException;

  Stream<YTRID> get(K key);

  void put(OAtomicOperation atomicOperation, K key, YTRID value) throws IOException;

  void close();

  void delete(OAtomicOperation atomicOperation) throws IOException;

  void load(
      String name,
      int keySize,
      YTType[] keyTypes,
      OBinarySerializer<K> keySerializer,
      OEncryption encryption);

  long size();

  boolean remove(OAtomicOperation atomicOperation, K key, YTRID value) throws IOException;

  Stream<ORawPair<K, YTRID>> iterateEntriesMinor(K key, boolean inclusive, boolean ascSortOrder);

  Stream<ORawPair<K, YTRID>> iterateEntriesMajor(K key, boolean inclusive, boolean ascSortOrder);

  K firstKey();

  K lastKey();

  Stream<K> keyStream();

  Stream<ORawPair<K, YTRID>> iterateEntriesBetween(
      K keyFrom, boolean fromInclusive, K keyTo, boolean toInclusive, boolean ascSortOrder);

  void acquireAtomicExclusiveLock();
}
