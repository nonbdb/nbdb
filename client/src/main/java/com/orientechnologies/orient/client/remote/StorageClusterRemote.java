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
package com.orientechnologies.orient.client.remote;

import com.jetbrains.youtrack.db.internal.core.config.StorageClusterConfiguration;
import com.jetbrains.youtrack.db.internal.core.conflict.RecordConflictStrategy;
import com.jetbrains.youtrack.db.internal.core.storage.StorageCluster;
import com.jetbrains.youtrack.db.internal.core.storage.PhysicalPosition;
import com.jetbrains.youtrack.db.internal.core.storage.RawBuffer;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.ClusterBrowsePage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * Remote cluster implementation
 */
public class StorageClusterRemote implements StorageCluster {

  private String name;
  private int id;

  /*
   * (non-Javadoc)
   *
   * @see com.orientechnologies.core.storage.SQLCluster#configure(com.orientechnologies.core.storage.Storage, int,
   * java.lang.String, java.lang.String, int, java.lang.Object[])
   */
  public void configure(int iId, String iClusterName) {
    id = iId;
    name = iClusterName;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.orientechnologies.core.storage.SQLCluster#configure(com.orientechnologies.core.storage.Storage,
   * com.orientechnologies.core.config.StorageClusterConfiguration)
   */
  public void configure(Storage iStorage, StorageClusterConfiguration iConfig) {
    id = iConfig.getId();
    name = iConfig.getName();
  }

  /*
   * (non-Javadoc)
   *
   * @see com.orientechnologies.core.storage.SQLCluster#create(int)
   */
  public void create(AtomicOperation atomicOperation) {
  }

  /*
   * (non-Javadoc)
   *
   * @see com.orientechnologies.core.storage.SQLCluster#open()
   */
  public void open(AtomicOperation atomicOperation) {
  }

  public void close() {
  }

  @Override
  public void close(boolean flush) {
  }

  @Override
  public PhysicalPosition allocatePosition(byte recordType, AtomicOperation atomicOperation) {
    throw new UnsupportedOperationException("allocatePosition");
  }

  @Override
  public PhysicalPosition createRecord(
      byte[] content,
      int recordVersion,
      byte recordType,
      PhysicalPosition allocatedPosition,
      AtomicOperation atomicOperation) {
    throw new UnsupportedOperationException("createRecord");
  }

  @Override
  public boolean deleteRecord(AtomicOperation atomicOperation, long clusterPosition) {
    throw new UnsupportedOperationException("deleteRecord");
  }

  @Override
  public void updateRecord(
      long clusterPosition,
      byte[] content,
      int recordVersion,
      byte recordType,
      AtomicOperation atomicOperation) {
    throw new UnsupportedOperationException("updateRecord");
  }

  @Override
  public @Nonnull RawBuffer readRecord(long clusterPosition, boolean prefetchRecords) {
    throw new UnsupportedOperationException("readRecord");
  }

  @Override
  public void setClusterName(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRecordConflictStrategy(final String conflictStrategy) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEncryption(final String encryptionName, final String encryptionKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean exists() {
    throw new UnsupportedOperationException("exists");
  }

  public void delete(AtomicOperation atomicOperation) {
  }

  public Object set(ATTRIBUTES iAttribute, Object iValue) {
    return null;
  }

  @Override
  public String encryption() {
    throw new UnsupportedOperationException("encryption");
  }

  public PhysicalPosition getPhysicalPosition(PhysicalPosition iPPosition) {
    return null;
  }

  public long getEntries() {
    return 0;
  }

  @Override
  public long getTombstonesCount() {
    throw new UnsupportedOperationException("getTombstonesCount()");
  }

  @Override
  public long getFirstPosition() {
    return 0;
  }

  @Override
  public long getLastPosition() {
    return 0;
  }

  @Override
  public long getNextPosition() {
    return 0;
  }

  @Override
  public String getFileName() {
    throw new UnsupportedOperationException("getFileName()");
  }

  public int getId() {
    return id;
  }

  public void synch() {
  }

  public String getName() {
    return name;
  }

  public long getRecordsSize() {
    throw new UnsupportedOperationException("getRecordsSize()");
  }

  @Override
  public boolean isSystemCluster() {
    return false;
  }

  @Override
  public PhysicalPosition[] higherPositions(PhysicalPosition position) {
    throw new UnsupportedOperationException("higherPositions()");
  }

  @Override
  public PhysicalPosition[] lowerPositions(PhysicalPosition position) {
    throw new UnsupportedOperationException("lowerPositions()");
  }

  @Override
  public PhysicalPosition[] ceilingPositions(PhysicalPosition position) {
    throw new UnsupportedOperationException("ceilingPositions()");
  }

  @Override
  public PhysicalPosition[] floorPositions(PhysicalPosition position) {
    throw new UnsupportedOperationException("floorPositions()");
  }

  @Override
  public boolean exists(long clusterPosition) throws IOException {
    throw new UnsupportedOperationException("exists()");
  }

  @Override
  public String compression() {
    throw new UnsupportedOperationException("compression()");
  }

  @Override
  public RecordConflictStrategy getRecordConflictStrategy() {
    return null;
  }

  @Override
  public void acquireAtomicExclusiveLock() {
    throw new UnsupportedOperationException("remote cluster doesn't support atomic locking");
  }

  @Override
  public ClusterBrowsePage nextPage(long lastPosition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getBinaryVersion() {
    throw new UnsupportedOperationException(
        "Operation is not supported for given cluster implementation");
  }
}
