package com.jetbrains.youtrack.db.internal.core.db;

import com.jetbrains.youtrack.db.internal.core.exception.AcquireTimeoutException;
import com.jetbrains.youtrack.db.internal.core.util.DatabaseURLConnection;
import com.jetbrains.youtrack.db.internal.core.util.URLHelper;

/**
 * A Pool of databases.
 *
 * <p>Example of usage with an YouTrackDB context:
 *
 * <p>
 *
 * <pre>
 * <code>
 * YouTrackDB youTrackDb= new YouTrackDB("remote:localhost","root","password");
 * //...
 * DatabasePool pool = new DatabasePool(youTrackDb,"myDb","admin","adminpwd");
 * ODatabaseDocument session = pool.acquire();
 * //....
 * session.close();
 * pool.close();
 * youTrackDb.close();
 *
 * </code>
 * </pre>
 *
 * <p>
 *
 * <p>
 *
 * <p>Example of usage as simple access to a specific database without a context:
 *
 * <p>
 *
 * <pre><code>
 * DatabasePool pool = new DatabasePool("remote:localhost/myDb","admin","adminpwd");
 * ODatabaseDocument session = pool.acquire();
 * //....
 * session.close();
 * pool.close();
 *
 * </code></pre>
 *
 * <p>
 *
 * <p>
 */
public class DatabasePool implements AutoCloseable {

  private final YouTrackDB youTrackDb;
  private final DatabasePoolInternal internal;
  private final boolean autoclose;

  /**
   * Open a new database pool on a specific environment.
   *
   * @param environment the starting environment.
   * @param database    the database name
   * @param user        the database user for the current pool of databases.
   * @param password    the password relative to the user name
   */
  public DatabasePool(YouTrackDB environment, String database, String user, String password) {
    this(environment, database, user, password, YouTrackDBConfig.defaultConfig());
  }

  /**
   * Open a new database pool on a specific environment, with a specific configuration for this
   * pool.
   *
   * @param environment   the starting environment.
   * @param database      the database name
   * @param user          the database user for the current pool of databases.
   * @param password      the password relative to the user name
   * @param configuration the configuration relative for the current pool.
   */
  public DatabasePool(
      YouTrackDB environment,
      String database,
      String user,
      String password,
      YouTrackDBConfig configuration) {
    youTrackDb = environment;
    autoclose = false;
    internal = youTrackDb.openPool(database, user, password, configuration);
  }

  /**
   * Open a new database pool from a url, useful in case the application access to only a database
   * or do not manipulate databases.
   *
   * @param url      the full url for a database, like "embedded:/full/path/to/database" or
   *                 "remote:localhost/database"
   * @param user     the database user for the current pool of databases.
   * @param password the password relative to the user
   */
  public DatabasePool(String url, String user, String password) {
    this(url, user, password, YouTrackDBConfig.defaultConfig());
  }

  /**
   * Open a new database pool from a url and additional configuration, useful in case the
   * application access to only a database or do not manipulate databases.
   *
   * @param url           the full url for a database, like "embedded:/full/path/to/database" or
   *                      "remote:localhost/database"
   * @param user          the database user for the current pool of databases.
   * @param password      the password relative to the user
   * @param configuration the configuration relative to the current pool.
   */
  public DatabasePool(String url, String user, String password, YouTrackDBConfig configuration) {
    DatabaseURLConnection val = URLHelper.parseNew(url);
    youTrackDb = new YouTrackDB(val.getType() + ":" + val.getPath(), configuration);
    autoclose = true;
    internal = youTrackDb.openPool(val.getDbName(), user, password, configuration);
  }

  /**
   * Open a new database pool from a environment and a database name, useful in case the application
   * access to only a database or do not manipulate databases.
   *
   * @param environment the url for an environemnt, like "embedded:/the/environment/path/" or
   *                    "remote:localhost"
   * @param database    the database for the current url.
   * @param user        the database user for the current pool of databases.
   * @param password    the password relative to the user
   */
  public DatabasePool(String environment, String database, String user, String password) {
    this(environment, database, user, password, YouTrackDBConfig.defaultConfig());
  }

  /**
   * Open a new database pool from a environment and a database name with a custom configuration,
   * useful in case the application access to only a database or do not manipulate databases.
   *
   * @param environment   the url for an environemnt, like "embedded:/the/environment/path/" or
   *                      "remote:localhost"
   * @param database      the database for the current url.
   * @param user          the database user for the current pool of databases.
   * @param password      the password relative to the user
   * @param configuration the configuration relative to the current pool.
   */
  public DatabasePool(
      String environment,
      String database,
      String user,
      String password,
      YouTrackDBConfig configuration) {
    youTrackDb = new YouTrackDB(environment, configuration);
    autoclose = true;
    internal = youTrackDb.openPool(database, user, password, configuration);
  }

  DatabasePool(YouTrackDB environment, DatabasePoolInternal internal) {
    this.youTrackDb = environment;
    this.internal = internal;
    autoclose = false;
  }

  /**
   * Acquire a session from the pool, if no session are available will wait until a session is
   * available or a timeout is reached
   *
   * @return a session from the pool.
   * @throws AcquireTimeoutException in case the timeout for waiting for a session is reached.
   */
  public DatabaseSession acquire() throws AcquireTimeoutException {
    return internal.acquire();
  }

  @Override
  public void close() {
    internal.close();
    if (autoclose) {
      youTrackDb.close();
    }
  }

  /**
   * Check if database pool is closed
   *
   * @return true if database pool is closed
   */
  public boolean isClosed() {
    return internal.isClosed();
  }
}
