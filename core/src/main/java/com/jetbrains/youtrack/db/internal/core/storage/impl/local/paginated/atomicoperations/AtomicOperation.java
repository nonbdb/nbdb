package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations;

import com.jetbrains.youtrack.db.internal.core.storage.cache.CacheEntry;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.LogSequenceNumber;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WriteAheadLog;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtreebonsai.local.BonsaiBucketPointer;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.util.Set;

public interface AtomicOperation {

  long getOperationUnitId();

  CacheEntry loadPageForWrite(long fileId, long pageIndex, int pageCount, boolean verifyChecksum)
      throws IOException;

  CacheEntry loadPageForRead(long fileId, long pageIndex) throws IOException;

  void addMetadata(AtomicOperationMetadata<?> metadata);

  AtomicOperationMetadata<?> getMetadata(String key);

  void addDeletedRidBag(BonsaiBucketPointer rootPointer);

  Set<BonsaiBucketPointer> getDeletedBonsaiPointers();

  CacheEntry addPage(long fileId) throws IOException;

  void releasePageFromRead(CacheEntry cacheEntry);

  void releasePageFromWrite(CacheEntry cacheEntry) throws IOException;

  long filledUpTo(long fileId);

  long addFile(String fileName) throws IOException;

  long loadFile(String fileName) throws IOException;

  void deleteFile(long fileId) throws IOException;

  boolean isFileExists(String fileName);

  String fileNameById(long fileId);

  long fileIdByName(String name);

  void truncateFile(long fileId) throws IOException;

  boolean containsInLockedObjects(String lockName);

  void addLockedObject(String lockName);

  void rollbackInProgress();

  boolean isRollbackInProgress();

  LogSequenceNumber commitChanges(WriteAheadLog writeAheadLog) throws IOException;

  Iterable<String> lockedObjects();

  void addDeletedRecordPosition(final int clusterId, final int pageIndex, final int recordPosition);

  IntSet getBookedRecordPositions(final int clusterId, final int pageIndex);

  void incrementComponentOperations();

  void decrementComponentOperations();

  int getComponentOperations();
}
