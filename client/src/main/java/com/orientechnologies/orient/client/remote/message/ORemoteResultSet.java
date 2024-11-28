package com.orientechnologies.orient.client.remote.message;

import com.orientechnologies.orient.client.remote.db.document.ODatabaseSessionRemote;
import com.orientechnologies.orient.core.db.document.OQueryDatabaseState;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.sql.executor.OExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class ORemoteResultSet implements OResultSet {

  private final ODatabaseSessionRemote db;
  private final String queryId;
  private List<OResult> currentPage;
  private Optional<OExecutionPlan> executionPlan;
  private Map<String, Long> queryStats;
  private boolean hasNextPage;

  public ORemoteResultSet(
      ODatabaseSessionRemote db,
      String queryId,
      List<OResult> currentPage,
      Optional<OExecutionPlan> executionPlan,
      Map<String, Long> queryStats,
      boolean hasNextPage) {
    this.db = db;
    this.queryId = queryId;
    this.currentPage = currentPage;
    this.executionPlan = executionPlan;
    this.queryStats = queryStats;
    this.hasNextPage = hasNextPage;
    if (db != null) {
      db.queryStarted(queryId, new OQueryDatabaseState(this));
      for (OResult result : currentPage) {
        if (result instanceof OResultInternal) {
          ((OResultInternal) result).bindToCache(db);
        }
      }
    }
  }

  @Override
  public boolean hasNext() {
    if (!currentPage.isEmpty()) {
      return true;
    }
    if (!hasNextPage()) {
      return false;
    }
    fetchNextPage();
    return !currentPage.isEmpty();
  }

  private void fetchNextPage() {
    if (db != null) {
      db.fetchNextPage(this);
    }
  }

  @Override
  public OResult next() {
    if (currentPage.isEmpty()) {
      if (!hasNextPage()) {
        throw new IllegalStateException();
      }
      fetchNextPage();
    }
    if (currentPage.isEmpty()) {
      throw new IllegalStateException();
    }
    OResult internal = currentPage.remove(0);

    if (internal.isRecord() && db != null && db.getTransaction().isActive()) {
      ORecord record = db.getTransaction().getRecord(internal.getRecord().get().getIdentity());
      if (record != null) {
        internal = new OResultInternal(db, record);
      }
    }
    return internal;
  }

  @Override
  public void close() {
    if (hasNextPage && db != null) {
      // CLOSES THE QUERY SERVER SIDE ONLY IF THERE IS ANOTHER PAGE. THE SERVER ALREADY
      // AUTOMATICALLY CLOSES THE QUERY AFTER SENDING THE LAST PAGE
      db.closeQuery(queryId);
    }
  }

  @Override
  public Optional<OExecutionPlan> getExecutionPlan() {
    return executionPlan;
  }

  @Override
  public Map<String, Long> getQueryStats() {
    return queryStats;
  }

  public void add(OResultInternal item) {
    currentPage.add(item);
  }

  public boolean hasNextPage() {
    return hasNextPage;
  }

  public String getQueryId() {
    return queryId;
  }

  public void fetched(
      List<OResult> result,
      boolean hasNextPage,
      Optional<OExecutionPlan> executionPlan,
      Map<String, Long> queryStats) {
    this.currentPage = result;
    this.hasNextPage = hasNextPage;

    if (queryStats != null) {
      this.queryStats = queryStats;
    }
    executionPlan.ifPresent(x -> this.executionPlan = executionPlan);
  }
}
