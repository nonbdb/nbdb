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
package com.jetbrains.youtrack.db.internal.core.iterator;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ORecordOperation;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTDatabaseException;
import com.jetbrains.youtrack.db.internal.core.exception.YTNoTxRecordReadException;
import com.jetbrains.youtrack.db.internal.core.exception.YTRecordNotFoundException;
import com.jetbrains.youtrack.db.internal.core.exception.YTSecurityException;
import com.jetbrains.youtrack.db.internal.core.id.ChangeableRecordId;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.security.ORole;
import com.jetbrains.youtrack.db.internal.core.metadata.security.ORule;
import com.jetbrains.youtrack.db.internal.core.metadata.security.YTSecurityUser;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.storage.OPhysicalPosition;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Iterator class to browse forward and backward the records of a cluster. Once browsed in a
 * direction, the iterator cannot change it.
 */
public abstract class OIdentifiableIterator<REC extends YTIdentifiable>
    implements Iterator<REC>, Iterable<REC> {

  protected final YTDatabaseSessionInternal database;
  protected final YTRecordId current = new ChangeableRecordId();
  private final Storage dbStorage;
  protected boolean liveUpdated = false;
  protected long limit = -1;
  protected long browsedRecords = 0;
  protected long totalAvailableRecords;
  protected List<ORecordOperation> txEntries;
  protected int currentTxEntryPosition = -1;
  protected long firstClusterEntry = 0;
  protected long lastClusterEntry = Long.MAX_VALUE;
  // REUSE IT
  private Boolean directionForward;
  private long currentEntry = YTRID.CLUSTER_POS_INVALID;
  private int currentEntryPosition = -1;
  private OPhysicalPosition[] positionsToProcess = null;

  /**
   * Set of RIDs of records which were indicated as broken during cluster iteration. Mainly used
   * during JSON export/import procedure to fix links on broken records.
   */
  protected final Set<YTRID> brokenRIDs = new HashSet<>();

  /**
   * @deprecated usage of this constructor may lead to deadlocks.
   */
  @Deprecated
  public OIdentifiableIterator(final YTDatabaseSessionInternal iDatabase) {
    database = iDatabase;

    dbStorage = database.getStorage();
    current.setClusterPosition(YTRID.CLUSTER_POS_INVALID); // DEFAULT = START FROM THE BEGIN
  }

  public abstract boolean hasPrevious();

  public abstract YTIdentifiable previous();

  public abstract OIdentifiableIterator<REC> begin();

  public abstract OIdentifiableIterator<REC> last();

  public Record current() {
    return readCurrentRecord(0);
  }

  public Set<YTRID> getBrokenRIDs() {
    return brokenRIDs;
  }

  public void remove() {
    throw new UnsupportedOperationException("remove");
  }

  public long getCurrentEntry() {
    return currentEntry;
  }

  /**
   * Return the iterator to be used in Java5+ constructs<br>
   * <br>
   * <code>
   * for( ORecordDocument rec : database.browseCluster( "Animal" ) ){<br> ...<br> }<br>
   * </code>
   */
  public Iterator<REC> iterator() {
    return this;
  }

  /**
   * Return the current limit on browsing record. -1 means no limits (default).
   *
   * @return The limit if setted, otherwise -1
   * @see #setLimit(long)
   */
  public long getLimit() {
    return limit;
  }

  /**
   * Set the limit on browsing record. -1 means no limits. You can set the limit even while you're
   * browsing.
   *
   * @param limit The current limit on browsing record. -1 means no limits (default).
   * @see #getLimit()
   */
  public OIdentifiableIterator<REC> setLimit(final long limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Return current configuration of live updates.
   *
   * @return True to activate it, otherwise false (default)
   * @see #setLiveUpdated(boolean)
   */
  public boolean isLiveUpdated() {
    return liveUpdated;
  }

  /**
   * Tell to the iterator that the upper limit must be checked at every cycle. Useful when
   * concurrent deletes or additions change the size of the cluster while you're browsing it.
   * Default is false.
   *
   * @param liveUpdated True to activate it, otherwise false (default)
   * @see #isLiveUpdated()
   */
  public OIdentifiableIterator<REC> setLiveUpdated(final boolean liveUpdated) {
    this.liveUpdated = liveUpdated;
    return this;
  }

  protected Record getTransactionEntry() {
    boolean noPhysicalRecordToBrowse;

    if (current.getClusterPosition() < YTRID.CLUSTER_POS_INVALID) {
      noPhysicalRecordToBrowse = true;
    } else if (directionForward) {
      noPhysicalRecordToBrowse = lastClusterEntry <= currentEntry;
    } else {
      noPhysicalRecordToBrowse = currentEntry <= firstClusterEntry;
    }

    if (!noPhysicalRecordToBrowse && positionsToProcess.length == 0) {
      noPhysicalRecordToBrowse = true;
    }

    if (noPhysicalRecordToBrowse && txEntries != null) {
      // IN TX
      currentTxEntryPosition++;
      if (currentTxEntryPosition >= txEntries.size()) {
        throw new NoSuchElementException();
      } else {
        return txEntries.get(currentTxEntryPosition).record;
      }
    }
    return null;
  }

  protected void checkDirection(final boolean iForward) {
    if (directionForward == null)
    // SET THE DIRECTION
    {
      directionForward = iForward;
    } else if (directionForward != iForward) {
      throw new YTIterationException("Iterator cannot change direction while browsing");
    }
  }

  /**
   * Read the current record and increment the counter if the record was found.
   *
   * @return record which was read from db.
   */
  protected Record readCurrentRecord(final int movement) {
    // LIMIT REACHED
    if (limit > -1 && browsedRecords >= limit) {
      return null;
    }

    final boolean moveResult =
        switch (movement) {
          case 1 -> nextPosition();
          case -1 -> prevPosition();
          case 0 -> checkCurrentPosition();
          default -> throw new IllegalStateException("Invalid movement value : " + movement);
        };

    if (!moveResult) {
      return null;
    }

    Record record;
    try {
      record = database.load(current);
    } catch (YTRecordNotFoundException rne) {
      record = null;
    } catch (YTDatabaseException e) {
      if (Thread.interrupted() || database.isClosed())
      // THREAD INTERRUPTED: RETURN
      {
        throw e;
      }

      if (e.getCause() instanceof YTSecurityException) {
        throw e;
      }

      if (e instanceof YTNoTxRecordReadException) {
        throw e;
      }

      if (GlobalConfiguration.DB_SKIP_BROKEN_RECORDS.getValueAsBoolean()) {
        record = null;
        brokenRIDs.add(current.copy());

        LogManager.instance()
            .error(
                this, "Error on fetching record during browsing. The record has been skipped", e);
      } else {
        throw e;
      }
    }

    if (record != null) {
      browsedRecords++;
      return record;
    }

    return null;
  }

  protected boolean nextPosition() {
    if (positionsToProcess == null) {
      positionsToProcess =
          dbStorage.ceilingPhysicalPositions(database,
              current.getClusterId(), new OPhysicalPosition(firstClusterEntry));
      if (positionsToProcess == null) {
        return false;
      }
    } else {
      if (currentEntry >= lastClusterEntry) {
        return false;
      }
    }

    incrementEntreePosition();
    while (positionsToProcess.length > 0 && currentEntryPosition >= positionsToProcess.length) {
      positionsToProcess =
          dbStorage.higherPhysicalPositions(database,
              current.getClusterId(), positionsToProcess[positionsToProcess.length - 1]);

      currentEntryPosition = -1;
      incrementEntreePosition();
    }

    if (positionsToProcess.length == 0) {
      return false;
    }

    currentEntry = positionsToProcess[currentEntryPosition].clusterPosition;

    if (currentEntry > lastClusterEntry || currentEntry == YTRID.CLUSTER_POS_INVALID) {
      return false;
    }

    current.setClusterPosition(currentEntry);
    return true;
  }

  protected boolean checkCurrentPosition() {
    if (currentEntry == YTRID.CLUSTER_POS_INVALID
        || firstClusterEntry > currentEntry
        || lastClusterEntry < currentEntry) {
      return false;
    }

    current.setClusterPosition(currentEntry);
    return true;
  }

  protected boolean prevPosition() {
    if (positionsToProcess == null) {
      positionsToProcess =
          dbStorage.floorPhysicalPositions(database,
              current.getClusterId(), new OPhysicalPosition(lastClusterEntry));
      if (positionsToProcess == null) {
        return false;
      }

      if (positionsToProcess.length == 0) {
        return false;
      }

      currentEntryPosition = positionsToProcess.length;
    } else {
      if (currentEntry < firstClusterEntry) {
        return false;
      }
    }

    decrementEntreePosition();

    while (positionsToProcess.length > 0 && currentEntryPosition < 0) {
      positionsToProcess =
          dbStorage.lowerPhysicalPositions(database, current.getClusterId(), positionsToProcess[0]);
      currentEntryPosition = positionsToProcess.length;

      decrementEntreePosition();
    }

    if (positionsToProcess.length == 0) {
      return false;
    }

    currentEntry = positionsToProcess[currentEntryPosition].clusterPosition;

    if (currentEntry < firstClusterEntry) {
      return false;
    }

    current.setClusterPosition(currentEntry);
    return true;
  }

  protected void resetCurrentPosition() {
    currentEntry = YTRID.CLUSTER_POS_INVALID;
    positionsToProcess = null;
    currentEntryPosition = -1;
  }

  protected long currentPosition() {
    return currentEntry;
  }

  protected static void checkForSystemClusters(
      final YTDatabaseSessionInternal iDatabase, final int[] iClusterIds) {
    if (iDatabase.isRemote()) {
      return;
    }

    for (int clId : iClusterIds) {
      if (iDatabase.getStorage().isSystemCluster(clId)) {
        final YTSecurityUser dbUser = iDatabase.getUser();
        if (dbUser == null
            || dbUser.allow(iDatabase, ORule.ResourceGeneric.SYSTEM_CLUSTERS, null,
            ORole.PERMISSION_READ)
            != null)
        // AUTHORIZED
        {
          break;
        }
      }
    }
  }

  private void decrementEntreePosition() {
    if (positionsToProcess.length > 0) {
      currentEntryPosition--;
    }
  }

  private void incrementEntreePosition() {
    if (positionsToProcess.length > 0) {
      currentEntryPosition++;
    }
  }
}