/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.orientechnologies.orient.jdbc;

import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.ODatabasePool;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseType;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.util.OURLConnection;
import com.jetbrains.youtrack.db.internal.core.util.OURLHelper;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class OrientDataSource implements DataSource {

  static {
    try {
      Class.forName(OrientJdbcDriver.class.getCanonicalName());
    } catch (ClassNotFoundException e) {
      System.err.println("YouTrackDB DataSource unable to load YouTrackDB JDBC Driver");
    }
  }

  private PrintWriter logger;
  private int loginTimeout;
  private String dbUrl;
  private String username;
  private String password;
  private Properties info;

  private YouTrackDB youTrackDB;
  private String dbName;

  private ODatabasePool pool;

  public OrientDataSource() {
    info = new Properties();
    info.setProperty("db.usePool", "TRUE");
    info.setProperty("db.pool.min", "1");
    info.setProperty("db.pool.max", "10");
  }

  /**
   * Creates a {@link DataSource}
   *
   * @param dbUrl
   * @param username
   * @param password
   * @param info
   */
  public OrientDataSource(String dbUrl, String username, String password, Properties info) {
    this.dbUrl = dbUrl;
    this.username = username;
    this.password = password;
    this.info = info;
  }

  @Deprecated
  public OrientDataSource(YouTrackDB youTrackDB) {
    this.youTrackDB = youTrackDB;
  }

  public OrientDataSource(YouTrackDB youTrackDB, String dbName) {
    this.youTrackDB = youTrackDB;
    this.dbName = dbName;
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return logger;
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    this.logger = out;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return getConnection(username, password);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {

    if (youTrackDB == null) {

      Properties info = new Properties(this.info);
      info.put("user", username);
      info.put("password", password);

      final String serverUsername = info.getProperty("serverUser", "");
      final String serverPassword = info.getProperty("serverPassword", "");

      String orientDbUrl = dbUrl.replace("jdbc:orient:", "");

      OURLConnection connUrl = OURLHelper.parseNew(orientDbUrl);
      YouTrackDBConfig settings =
          YouTrackDBConfig.builder()
              .addConfig(
                  GlobalConfiguration.DB_POOL_MIN,
                  Integer.valueOf(info.getProperty("db.pool.min", "1")))
              .addConfig(
                  GlobalConfiguration.DB_POOL_MAX,
                  Integer.valueOf(info.getProperty("db.pool.max", "10")))
              .build();

      youTrackDB =
          new YouTrackDB(
              connUrl.getType() + ":" + connUrl.getPath(),
              serverUsername,
              serverPassword,
              settings);

      if (!serverUsername.isEmpty() && !serverPassword.isEmpty()) {
        youTrackDB.createIfNotExists(
            connUrl.getDbName(),
            connUrl.getDbType().orElse(ODatabaseType.MEMORY),
            username,
            password,
            "admin");
      }

      pool = new ODatabasePool(youTrackDB, connUrl.getDbName(), username, password);
    } else if (pool == null) {
      pool = new ODatabasePool(youTrackDB, this.dbName, username, password);
    }

    return new OrientJdbcConnection(
        pool.acquire(), youTrackDB, info == null ? new Properties() : info);
  }

  public void setDbUrl(String dbUrl) {
    this.dbUrl = dbUrl;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setInfo(Properties info) {
    this.info = info;
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return loginTimeout;
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    this.loginTimeout = seconds;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  public void close() {
    youTrackDB.close();
  }
}
