package com.jetbrains.youtrack.db.internal.core.db;

/**
 * Cached database pool factory which allows store database pools associated with users
 */
public interface CachedDatabasePoolFactory {

  /**
   * Get {@link DatabasePoolInternal} from cache or create and cache new
   * {@link DatabasePoolInternal}
   *
   * @param database name of database
   * @param username user name
   * @param password user password
   * @return {@link DatabasePoolInternal} cached database pool
   */
  DatabasePoolInternal get(
      String database, String username, String password, YouTrackDBConfigImpl config);

  /**
   * Close all cached pools and clear cache
   *
   * @return this instance
   */
  CachedDatabasePoolFactory reset();

  /**
   * Close all cached pools, clear cache. Can't use this factory after close.
   */
  void close();

  /**
   * Check if factory is closed
   *
   * @return true if factory is closed
   */
  boolean isClosed();
}
