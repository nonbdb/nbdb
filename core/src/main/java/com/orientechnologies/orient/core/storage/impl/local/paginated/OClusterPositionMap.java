/*
 *
 *  *  Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
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
 *  * For more information: http://orientdb.com
 *
 */

package com.orientechnologies.orient.core.storage.impl.local.paginated;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.util.OCommonConst;
import com.orientechnologies.orient.core.exception.OClusterPositionMapException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.storage.cache.OCacheEntry;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.orientechnologies.orient.core.storage.impl.local.paginated.base.ODurableComponent;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Andrey Lomakin (a.lomakin-at-orientdb.com)
 * @since 10/7/13
 */
public class OClusterPositionMap extends ODurableComponent {
  public static final String DEF_EXTENSION = ".cpm";
  private long fileId;

  public OClusterPositionMap(OAbstractPaginatedStorage storage, String name, String lockName) {
    super(storage, name, DEF_EXTENSION, lockName);
  }

  public void open() throws IOException {
    try {
      acquireExclusiveLock();
      try {
        OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
        fileId = openFile(atomicOperation, getFullName());
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public void create() throws IOException {
    try {
      final OAtomicOperation atomicOperation = startAtomicOperation(false);

      acquireExclusiveLock();
      try {
        fileId = addFile(atomicOperation, getFullName());
        endAtomicOperation(false, null);
      } catch (IOException ioe) {
        endAtomicOperation(true, ioe);
        throw ioe;
      } catch (Exception e) {
        endAtomicOperation(true, e);
        throw OException
            .wrapException(new OClusterPositionMapException("Error during cluster position - physical position map", this), e);
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public void flush() {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          writeCache.flush(fileId);
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public void close(boolean flush) throws IOException {
    try {
      acquireExclusiveLock();
      try {
        readCache.closeFile(fileId, flush, writeCache);
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public void truncate() throws IOException {
    try {
      final OAtomicOperation atomicOperation = startAtomicOperation(true);
      acquireExclusiveLock();
      try {
        truncateFile(atomicOperation, fileId);
        endAtomicOperation(false, null);
      } catch (IOException ioe) {
        endAtomicOperation(true, ioe);
        throw ioe;
      } catch (Exception e) {
        endAtomicOperation(true, e);
        throw OException.wrapException(
            new OClusterPositionMapException("Error during truncation of cluster position - physical position map", this), e);
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public void delete() throws IOException {
    try {
      final OAtomicOperation atomicOperation = startAtomicOperation(false);

      acquireExclusiveLock();
      try {
        deleteFile(atomicOperation, fileId);
        endAtomicOperation(false, null);
      } catch (IOException ioe) {
        endAtomicOperation(true, ioe);
        throw ioe;
      } catch (Exception e) {
        endAtomicOperation(true, e);
        throw OException.wrapException(
            new OClusterPositionMapException("Error during deletion of cluster position - physical position map", this), e);
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public void rename(String newName) throws IOException {
    try {
      startAtomicOperation(true);
      acquireExclusiveLock();
      try {
        writeCache.renameFile(fileId, newName + getExtension());
        setName(newName);
        endAtomicOperation(false, null);
      } catch (IOException ioe) {
        endAtomicOperation(true, ioe);
        throw ioe;
      } catch (Exception e) {
        endAtomicOperation(true, e);
        throw OException.wrapException(
            new OClusterPositionMapException("Error during rename of cluster position - physical position map", this), e);
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public long add(long pageIndex, int recordPosition) throws IOException {
    try {
      OAtomicOperation atomicOperation = startAtomicOperation(true);

      acquireExclusiveLock();
      try {
        long lastPage = getFilledUpTo(atomicOperation, fileId) - 1;
        OCacheEntry cacheEntry;
        if (lastPage < 0)
          cacheEntry = addPage(atomicOperation, fileId);
        else
          cacheEntry = loadPageForWrite(atomicOperation, fileId, lastPage, false, 1);

        Exception exception = null;
        try {
          OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
          if (bucket.isFull()) {
            releasePageFromWrite(atomicOperation, cacheEntry);

            cacheEntry = addPage(atomicOperation, fileId);

            bucket = new OClusterPositionMapBucket(cacheEntry);
          }

          final long index = bucket.add(pageIndex, recordPosition);
          return index + cacheEntry.getPageIndex() * OClusterPositionMapBucket.MAX_ENTRIES;
        } catch (Exception e) {
          exception = e;
          throw OException.wrapException(
              new OClusterPositionMapException("Error during creation of mapping between logical and physical record position",
                  this), e);
        } finally {
          try {
            releasePageFromWrite(atomicOperation, cacheEntry);
          } finally {
            endAtomicOperation(exception != null, exception);
          }
        }
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public long allocate() throws IOException {
    try {
      OAtomicOperation atomicOperation = startAtomicOperation(true);

      acquireExclusiveLock();
      try {
        long lastPage = getFilledUpTo(atomicOperation, fileId) - 1;
        OCacheEntry cacheEntry;
        if (lastPage < 0)
          cacheEntry = addPage(atomicOperation, fileId);
        else
          cacheEntry = loadPageForWrite(atomicOperation, fileId, lastPage, false, 1);

        Exception exception = null;
        try {

          OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
          if (bucket.isFull()) {
            releasePageFromWrite(atomicOperation, cacheEntry);

            cacheEntry = addPage(atomicOperation, fileId);

            bucket = new OClusterPositionMapBucket(cacheEntry);
          }

          final long index = bucket.allocate();
          return index + cacheEntry.getPageIndex() * OClusterPositionMapBucket.MAX_ENTRIES;
        } catch (Exception e) {
          exception = e;
          throw OException.wrapException(
              new OClusterPositionMapException("Error during creation of mapping between logical and physical record position",
                  this), e);
        } finally {
          try {
            releasePageFromWrite(atomicOperation, cacheEntry);
          } finally {
            endAtomicOperation(exception != null, exception);
          }
        }
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public void update(final long clusterPosition, final OClusterPositionMapBucket.PositionEntry entry) throws IOException {
    try {
      OAtomicOperation atomicOperation = startAtomicOperation(true);

      acquireExclusiveLock();
      try {
        final long pageIndex = clusterPosition / OClusterPositionMapBucket.MAX_ENTRIES;
        final int index = (int) (clusterPosition % OClusterPositionMapBucket.MAX_ENTRIES);

        if (pageIndex >= getFilledUpTo(atomicOperation, fileId))
          throw new OClusterPositionMapException(
              "Passed in cluster position " + clusterPosition + " is outside of range of cluster-position map", this);

        final OCacheEntry cacheEntry = loadPageForWrite(atomicOperation, fileId, pageIndex, false, 1);
        try {
          final OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
          bucket.set(index, entry);
        } finally {
          releasePageFromWrite(atomicOperation, cacheEntry);
        }

        endAtomicOperation(false, null);
      } catch (IOException | RuntimeException e) {
        endAtomicOperation(true, e);
        throw OException.wrapException(
            new OClusterPositionMapException("Error of update of mapping between logical adn physical record position", this), e);
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public void resurrect(final long clusterPosition, final OClusterPositionMapBucket.PositionEntry entry) throws IOException {
    try {
      OAtomicOperation atomicOperation = startAtomicOperation(true);

      acquireExclusiveLock();
      try {
        final long pageIndex = clusterPosition / OClusterPositionMapBucket.MAX_ENTRIES;
        final int index = (int) (clusterPosition % OClusterPositionMapBucket.MAX_ENTRIES);

        if (pageIndex >= getFilledUpTo(atomicOperation, fileId))
          throw new OClusterPositionMapException(
              "Passed in cluster position " + clusterPosition + " is outside of range of cluster-position map", this);

        final OCacheEntry cacheEntry = loadPageForWrite(atomicOperation, fileId, pageIndex, false, 1);
        try {
          final OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
          bucket.resurrect(index, entry);
        } finally {
          releasePageFromWrite(atomicOperation, cacheEntry);
        }

        endAtomicOperation(false, null);
      } catch (IOException | RuntimeException e) {
        endAtomicOperation(true, e);
        throw OException.wrapException(
            new OClusterPositionMapException("Error of resurrecting mapping between logical adn physical record position", this),
            e);
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public OClusterPositionMapBucket.PositionEntry get(final long clusterPosition, final int pageCount) throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          long pageIndex = clusterPosition / OClusterPositionMapBucket.MAX_ENTRIES;
          int index = (int) (clusterPosition % OClusterPositionMapBucket.MAX_ENTRIES);

          final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();

          if (pageIndex >= getFilledUpTo(atomicOperation, fileId))
            return null;

          final OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, pageCount);
          try {
            final OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
            return bucket.get(index);
          } finally {
            releasePageFromRead(atomicOperation, cacheEntry);
          }
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public void remove(final long clusterPosition) throws IOException {
    try {
      OAtomicOperation atomicOperation = startAtomicOperation(true);

      acquireExclusiveLock();
      try {
        long pageIndex = clusterPosition / OClusterPositionMapBucket.MAX_ENTRIES;
        int index = (int) (clusterPosition % OClusterPositionMapBucket.MAX_ENTRIES);

        Exception exception = null;
        final OCacheEntry cacheEntry = loadPageForWrite(atomicOperation, fileId, pageIndex, false, 1);
        try {
          final OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);

          bucket.remove(index);
        } catch (Exception e) {
          exception = e;
          throw OException.wrapException(
              new OClusterPositionMapException("Error during removal of mapping between logical and physical record position",
                  this), e);
        } finally {
          try {
            releasePageFromWrite(atomicOperation, cacheEntry);
          } finally {
            endAtomicOperation(exception != null, exception);
          }
        }
      } finally {
        releaseExclusiveLock();
      }
    } finally {
    }
  }

  public long[] higherPositions(final long clusterPosition) throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          if (clusterPosition == Long.MAX_VALUE)
            return OCommonConst.EMPTY_LONG_ARRAY;

          return ceilingPositions(clusterPosition + 1);
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public OClusterPositionEntry[] higherPositionsEntries(final long clusterPosition) throws IOException {
    long realPosition = clusterPosition + 1;
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          if (clusterPosition == Long.MAX_VALUE)
            return new OClusterPositionEntry[] {};

          if (realPosition < 0)
            realPosition = 0;

          long pageIndex = realPosition / OClusterPositionMapBucket.MAX_ENTRIES;
          int index = (int) (realPosition % OClusterPositionMapBucket.MAX_ENTRIES);

          OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
          final long filledUpTo = getFilledUpTo(atomicOperation, fileId);

          if (pageIndex >= filledUpTo)
            return new OClusterPositionEntry[] {};

          OClusterPositionEntry[] result = null;
          do {
            OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, 1);

            OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
            int resultSize = bucket.getSize() - index;

            if (resultSize <= 0) {
              releasePageFromRead(atomicOperation, cacheEntry);
              pageIndex++;
              index = 0;
            } else {
              int entriesCount = 0;
              long startIndex = cacheEntry.getPageIndex() * OClusterPositionMapBucket.MAX_ENTRIES + index;

              result = new OClusterPositionEntry[resultSize];
              for (int i = 0; i < resultSize; i++) {
                if (bucket.exists(i + index)) {
                  OClusterPositionMapBucket.PositionEntry val = bucket.get(i + index);
                  result[entriesCount] = new OClusterPositionEntry(startIndex + i, val.getPageIndex(), val.getRecordPosition());
                  entriesCount++;
                }
              }

              if (entriesCount == 0) {
                result = null;
                pageIndex++;
                index = 0;
              } else
                result = Arrays.copyOf(result, entriesCount);

              releasePageFromRead(atomicOperation, cacheEntry);
            }
          } while (result == null && pageIndex < filledUpTo);

          if (result == null)
            result = new OClusterPositionEntry[] {};

          return result;
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public long[] ceilingPositions(long clusterPosition) throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          if (clusterPosition < 0)
            clusterPosition = 0;

          long pageIndex = clusterPosition / OClusterPositionMapBucket.MAX_ENTRIES;
          int index = (int) (clusterPosition % OClusterPositionMapBucket.MAX_ENTRIES);

          OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
          final long filledUpTo = getFilledUpTo(atomicOperation, fileId);

          if (pageIndex >= filledUpTo)
            return OCommonConst.EMPTY_LONG_ARRAY;

          long[] result = null;
          do {
            OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, 1);

            OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
            int resultSize = bucket.getSize() - index;

            if (resultSize <= 0) {
              releasePageFromRead(atomicOperation, cacheEntry);
              pageIndex++;
              index = 0;
            } else {
              int entriesCount = 0;
              long startIndex = cacheEntry.getPageIndex() * OClusterPositionMapBucket.MAX_ENTRIES + index;

              result = new long[resultSize];
              for (int i = 0; i < resultSize; i++) {
                if (bucket.exists(i + index)) {
                  result[entriesCount] = startIndex + i;
                  entriesCount++;
                }
              }

              if (entriesCount == 0) {
                result = null;
                pageIndex++;
                index = 0;
              } else
                result = Arrays.copyOf(result, entriesCount);

              releasePageFromRead(atomicOperation, cacheEntry);
            }
          } while (result == null && pageIndex < filledUpTo);

          if (result == null)
            result = OCommonConst.EMPTY_LONG_ARRAY;

          return result;
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public long[] lowerPositions(final long clusterPosition) throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          if (clusterPosition == 0)
            return OCommonConst.EMPTY_LONG_ARRAY;

          return floorPositions(clusterPosition - 1);
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public long[] floorPositions(final long clusterPosition) throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          if (clusterPosition < 0)
            return OCommonConst.EMPTY_LONG_ARRAY;

          long pageIndex = clusterPosition / OClusterPositionMapBucket.MAX_ENTRIES;
          int index = (int) (clusterPosition % OClusterPositionMapBucket.MAX_ENTRIES);

          OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();

          final long filledUpTo = getFilledUpTo(atomicOperation, fileId);
          long[] result;

          if (pageIndex >= filledUpTo) {
            pageIndex = filledUpTo - 1;
            index = Integer.MIN_VALUE;
          }

          if (pageIndex < 0) {
            return OCommonConst.EMPTY_LONG_ARRAY;
          }

          do {
            OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, 1);

            OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
            if (index == Integer.MIN_VALUE)
              index = bucket.getSize() - 1;

            int resultSize = index + 1;
            int entriesCount = 0;

            long startPosition = cacheEntry.getPageIndex() * OClusterPositionMapBucket.MAX_ENTRIES;
            result = new long[resultSize];

            for (int i = 0; i < resultSize; i++) {
              if (bucket.exists(i)) {
                result[entriesCount] = startPosition + i;
                entriesCount++;
              }
            }

            if (entriesCount == 0) {
              result = null;
              pageIndex--;
              index = Integer.MIN_VALUE;
            } else
              result = Arrays.copyOf(result, entriesCount);

            releasePageFromRead(atomicOperation, cacheEntry);
          } while (result == null && pageIndex >= 0);

          if (result == null)
            result = OCommonConst.EMPTY_LONG_ARRAY;

          return result;
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public long getFirstPosition() throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();

          final long filledUpTo = getFilledUpTo(atomicOperation, fileId);
          for (long pageIndex = 0; pageIndex < filledUpTo; pageIndex++) {
            OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, 1);
            try {
              OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
              int bucketSize = bucket.getSize();

              for (int index = 0; index < bucketSize; index++) {
                if (bucket.exists(index))
                  return pageIndex * OClusterPositionMapBucket.MAX_ENTRIES + index;
              }
            } finally {
              releasePageFromRead(atomicOperation, cacheEntry);
            }
          }

          return ORID.CLUSTER_POS_INVALID;
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public byte getStatus(final long clusterPosition) throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          final long pageIndex = clusterPosition / OClusterPositionMapBucket.MAX_ENTRIES;
          final int index = (int) (clusterPosition % OClusterPositionMapBucket.MAX_ENTRIES);

          final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();

          if (pageIndex >= getFilledUpTo(atomicOperation, fileId))
            return OClusterPositionMapBucket.NOT_EXISTENT;

          final OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, 1);
          try {
            final OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);

            return bucket.getStatus(index);

          } finally {
            releasePageFromRead(atomicOperation, cacheEntry);
          }
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public long getLastPosition() throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
          final long filledUpTo = getFilledUpTo(atomicOperation, fileId);

          for (long pageIndex = filledUpTo - 1; pageIndex >= 0; pageIndex--) {
            OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, 1);
            try {
              OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
              final int bucketSize = bucket.getSize();

              for (int index = bucketSize - 1; index >= 0; index--) {
                if (bucket.exists(index))
                  return pageIndex * OClusterPositionMapBucket.MAX_ENTRIES + index;
              }
            } finally {
              releasePageFromRead(atomicOperation, cacheEntry);
            }
          }

          return ORID.CLUSTER_POS_INVALID;
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  /**
   * Returns the next position available.
   */
  public long getNextPosition() throws IOException {
    try {
      atomicOperationsManager.acquireReadLock(this);
      try {
        acquireSharedLock();
        try {
          OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
          final long filledUpTo = getFilledUpTo(atomicOperation, fileId);

          final long pageIndex = filledUpTo - 1;
          OCacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex, false, 1);
          try {
            OClusterPositionMapBucket bucket = new OClusterPositionMapBucket(cacheEntry);
            final int bucketSize = bucket.getSize();
            return pageIndex * OClusterPositionMapBucket.MAX_ENTRIES + bucketSize;
          } finally {
            releasePageFromRead(atomicOperation, cacheEntry);
          }
        } finally {
          releaseSharedLock();
        }
      } finally {
        atomicOperationsManager.releaseReadLock(this);
      }
    } finally {
    }
  }

  public long getFileId() {
    return fileId;
  }

  public void replaceFileId(long newFileId) {
    this.fileId = newFileId;
  }

  public static class OClusterPositionEntry {
    private long position;
    private long page;
    private int  offset;

    public OClusterPositionEntry(long position, long page, int offset) {
      this.position = position;
      this.page = page;
      this.offset = offset;
    }

    public long getPosition() {
      return position;
    }

    public long getPage() {
      return page;
    }

    public int getOffset() {
      return offset;
    }
  }
}
