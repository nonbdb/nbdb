package com.orientechnologies.orient.client.remote;

import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultSet;

/**
 *
 */
public class ORemoteQueryResult {

  private final ResultSet result;
  private final boolean transactionUpdated;
  private final boolean reloadMetadata;

  public ORemoteQueryResult(ResultSet result, boolean transactionUpdated,
      boolean reloadMetadata) {
    this.result = result;
    this.transactionUpdated = transactionUpdated;
    this.reloadMetadata = reloadMetadata;
  }

  public ResultSet getResult() {
    return result;
  }

  public boolean isTransactionUpdated() {
    return transactionUpdated;
  }

  public boolean isReloadMetadata() {
    return reloadMetadata;
  }
}
