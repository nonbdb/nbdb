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

package com.orientechnologies.orient.core.storage.impl.local.paginated.wal;

import com.orientechnologies.orient.core.storage.impl.local.OCheckpointRequestListener;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperationMetadata;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.common.WriteableWALRecord;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @since 6/25/14
 */
public interface OWriteAheadLog extends AutoCloseable {

  String MASTER_RECORD_EXTENSION = ".wmr";
  String WAL_SEGMENT_EXTENSION = ".wal";

  OLogSequenceNumber begin();

  OLogSequenceNumber begin(long segmentId) throws IOException;

  OLogSequenceNumber end();

  void flush();

  OLogSequenceNumber logAtomicOperationStartRecord(
      final boolean isRollbackSupported, final long unitId, byte[] metadata);

  OLogSequenceNumber logAtomicOperationStartRecord(boolean isRollbackSupported, long unitId)
      throws IOException;

  OLogSequenceNumber logAtomicOperationEndRecord(
      long operationUnitId,
      boolean rollback,
      OLogSequenceNumber startLsn,
      Map<String, OAtomicOperationMetadata<?>> atomicOperationMetadata)
      throws IOException;

  OLogSequenceNumber log(WriteableWALRecord record) throws IOException;

  void close() throws IOException;

  void close(boolean flush) throws IOException;

  void delete() throws IOException;

  List<WriteableWALRecord> read(OLogSequenceNumber lsn, int limit) throws IOException;

  List<WriteableWALRecord> next(OLogSequenceNumber lsn, int limit) throws IOException;

  OLogSequenceNumber getFlushedLsn();

  /**
   * Cut WAL content till passed in value of LSN at maximum in many cases smaller portion of WAL may
   * be cut. If value of LSN is bigger than values provided in
   * {@link #addCutTillLimit(OLogSequenceNumber)} then "protected" part of WAL will be preserved for
   * sure.
   *
   * @param lsn Maximum value of LSN till WAL will be cut.
   * @return <code>true</code> if some portion of WAL will be cut and <code>false</code> if WAL left
   * untouched.
   */
  boolean cutTill(OLogSequenceNumber lsn) throws IOException;

  boolean cutAllSegmentsSmallerThan(long segmentId) throws IOException;

  void addCheckpointListener(OCheckpointRequestListener listener);

  void removeCheckpointListener(OCheckpointRequestListener listener);

  /**
   * Next LSN generated by WAL will be bigger than passed in value.
   */
  @SuppressWarnings("unused")
  void moveLsnAfter(OLogSequenceNumber lsn) throws IOException;

  /**
   * Adds LSN after which WAL log should be preserved. It is possible to add many such LSNs smallest
   * value among them will be used to limit value of LSN after which WAL may be cut.
   *
   * @param lsn LSN after which cut of the WAL is not allowed.
   * @see #removeCutTillLimit(OLogSequenceNumber)
   * @see #cutTill(OLogSequenceNumber)
   */
  void addCutTillLimit(OLogSequenceNumber lsn);

  /**
   * Removes LSN after which WAL log should be preserved. It is possible to add many such LSNs
   * smallest value among them will be used to limit value of LSN after which WAL may be cut.
   *
   * @param lsn LSN after which cut of the WAL is not allowed.
   * @see #removeCutTillLimit(OLogSequenceNumber)
   * @see #cutTill(OLogSequenceNumber)
   */
  void removeCutTillLimit(OLogSequenceNumber lsn);

  File[] nonActiveSegments(long fromSegment);

  long[] nonActiveSegments();

  long activeSegment();

  /**
   * Adds the event to fire when this write ahead log instances reaches the given LSN. The thread on
   * which the event will be fired is unspecified, the event may be even fired synchronously before
   * this method returns. Avoid running long tasks in the event handler since this may degrade the
   * performance of this write ahead log and/or its event managing component. The exact LSN, up to
   * which this write ahead log is actually grown, may differ from the event's LSN at the moment of
   * invocation. But it's guarantied that the write ahead log's LSN will be larger than or equal to
   * the event's LSN. In other words, the event invocation may be postponed, exact timings depend on
   * implementation details of this write ahead log.
   *
   * @param lsn   the LSN to fire at.
   * @param event the event to fire.
   */
  void addEventAt(OLogSequenceNumber lsn, Runnable event);

  /**
   * Adds new segment so all subsequent log entries will be added to this new segment. New segment
   * can not be appended if:
   *
   * <ol>
   *   <li>WAL is empty
   *   <li>There last segment in WAL is empty.
   * </ol>
   * <p>
   * Despite of the fact that WAL segment will not be appended, method call still will reach its
   * main target, all subsequent log records will have segment number higher than previously logged
   * records. But to inform user that segment is not added result of success of failure of this
   * method will be returned.
   *
   * @return <code>true</code> if new segment is added, and <code>false</code> otherwise.
   */
  boolean appendNewSegment();
}
