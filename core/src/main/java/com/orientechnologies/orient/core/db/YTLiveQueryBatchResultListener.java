package com.orientechnologies.orient.core.db;

/**
 * Designed to allow live query result listeners to be optimised for batch elaboration. The normal
 * mechanics of the {@link YTLiveQueryResultListener} is preserved; In addition, at the end of a
 * logical batch of invocations to on*() methods, onBatchEnd() is invoked.
 */
public interface YTLiveQueryBatchResultListener extends YTLiveQueryResultListener {

  /**
   * invoked at the end of a logical batch of live query events
   *
   * @param database the instance of the active datatabase connection where the live query operation
   *                 is being performed
   */
  void onBatchEnd(YTDatabaseSession database);
}
