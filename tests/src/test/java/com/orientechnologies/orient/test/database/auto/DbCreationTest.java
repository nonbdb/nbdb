/*
 * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.exception.OCoreException;
import com.orientechnologies.orient.core.exception.OStorageException;
import java.io.File;
import java.util.Locale;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class DbCreationTest {

  private static final String DB_NAME = "DbCreationTest";

  private OrientDB orientDB;
  private final boolean remoteDB;

  @Parameters(value = "remote")
  public DbCreationTest(@Optional Boolean remote) {
    remoteDB = remote != null ? remote : false;
  }

  @BeforeClass
  public void beforeClass() {
    initODB();
  }

  @AfterClass
  public void afterClass() {
    orientDB.close();
  }

  private void initODB() {
    var configBuilder = OrientDBConfig.builder();
    configBuilder.addConfig(OGlobalConfiguration.NON_TX_READS_WARNING_MODE, "EXCEPTION");

    if (remoteDB) {
      orientDB =
          OrientDB.remote(
              "localhost",
              "root",
              "D2AFD02F20640EC8B7A5140F34FCA49D2289DB1F0D0598BB9DE8AAA75A0792F3",
              configBuilder.build());
    } else {
      final String buildDirectory = System.getProperty("buildDirectory", ".");
      orientDB = OrientDB.embedded(buildDirectory + "/test-db", configBuilder.build());
    }
  }

  @Test
  public void testDbCreationDefault() {
    if (orientDB.exists(DB_NAME)) {
      orientDB.drop(DB_NAME);
    }

    orientDB.create(DB_NAME, ODatabaseType.PLOCAL, "admin", "admin", "admin");
  }

  @Test(dependsOnMethods = {"testDbCreationDefault"})
  public void testDbExists() {
    Assert.assertTrue(orientDB.exists(DB_NAME), "Database " + DB_NAME + " not found");
  }

  @Test(dependsOnMethods = {"testDbExists"})
  public void testDbOpen() {
    var database = orientDB.open(DB_NAME, "admin", "admin");
    Assert.assertNotNull(database.getName());
    database.close();
  }

  @Test(dependsOnMethods = {"testDbOpen"})
  public void testDbOpenWithLastAsSlash() {
    orientDB.close();

    String url = calculateURL() + "/";

    try (var odb = new OrientDB(url, "root", "root", OrientDBConfig.defaultConfig())) {
      var database = odb.open(DB_NAME, "admin", "admin");
      database.close();
    }

    initODB();
  }

  @Test(dependsOnMethods = {"testDbOpenWithLastAsSlash"})
  public void testDbOpenWithBackSlash() {
    orientDB.close();

    String url;
    url = calculateURL();
    url = url.replace('/', '\\');

    try (var odb = new OrientDB(url, "root", "root", OrientDBConfig.defaultConfig())) {
      var database = odb.open(DB_NAME, "admin", "admin");
      database.close();
    }

    initODB();
  }

  private String calculateURL() {
    String url;
    if (remoteDB) {
      url = "remote:localhost";
    } else {
      final String buildDirectory = System.getProperty("buildDirectory", ".");
      url = "plocal:" + buildDirectory + "/test-db";
    }
    return url;
  }

  @Test(dependsOnMethods = {"testDbOpenWithBackSlash"})
  public void testChangeLocale() {
    try (var database = orientDB.open(DB_NAME, "admin", "admin")) {
      database.command(" ALTER DATABASE LOCALELANGUAGE  ?", Locale.GERMANY.getLanguage()).close();
      database.command(" ALTER DATABASE LOCALECOUNTRY  ?", Locale.GERMANY.getCountry()).close();

      Assert.assertEquals(
          database.get(ODatabase.ATTRIBUTES.LOCALELANGUAGE), Locale.GERMANY.getLanguage());
      Assert.assertEquals(
          database.get(ODatabase.ATTRIBUTES.LOCALECOUNTRY), Locale.GERMANY.getCountry());
      database.set(ODatabase.ATTRIBUTES.LOCALECOUNTRY, Locale.ENGLISH.getCountry());
      database.set(ODatabase.ATTRIBUTES.LOCALELANGUAGE, Locale.ENGLISH.getLanguage());
      Assert.assertEquals(
          database.get(ODatabase.ATTRIBUTES.LOCALECOUNTRY), Locale.ENGLISH.getCountry());
      Assert.assertEquals(
          database.get(ODatabase.ATTRIBUTES.LOCALELANGUAGE), Locale.ENGLISH.getLanguage());
    }
  }

  @Test(dependsOnMethods = {"testChangeLocale"})
  public void testRoles() {
    try (var database = orientDB.open(DB_NAME, "admin", "admin")) {
      database.begin();
      database.query("select from ORole where name = 'admin'").close();
      database.commit();
    }
  }

  @Test(dependsOnMethods = {"testChangeLocale"})
  public void testSubFolderDbCreate() {
    if (remoteDB) {
      return;
    }

    var url = calculateURL();

    var odb = new OrientDB(url, "root", "root", OrientDBConfig.defaultConfig());
    if (odb.exists("sub")) {
      odb.drop("sub");
    }

    odb.create("sub", ODatabaseType.PLOCAL, "admin", "admin", "admin");
    var db = odb.open("sub", "admin", "admin");
    db.close();

    odb.drop("sub");
  }

  @Test(dependsOnMethods = {"testChangeLocale"})
  public void testSubFolderDbCreateConnPool() {
    if (remoteDB) {
      return;
    }

    var url = calculateURL();

    var odb = new OrientDB(url, "root", "root", OrientDBConfig.defaultConfig());
    if (odb.exists("sub")) {
      odb.drop("sub");
    }

    odb.create("sub", ODatabaseType.PLOCAL, "admin", "admin", "admin");
    var db = odb.cachedPool("sub", "admin", "admin");
    db.close();

    odb.drop("sub");
  }

  @Test(dependsOnMethods = {"testSubFolderDbCreateConnPool"})
  public void testOpenCloseConnectionPool() {
    for (int i = 0; i < 500; i++) {
      orientDB.cachedPool(DB_NAME, "admin", "admin").acquire().close();
    }
  }

  @Test(dependsOnMethods = {"testChangeLocale"})
  public void testSubFolderMultipleDbCreateSameName() {
    if (remoteDB) {
      return;
    }

    for (int i = 0; i < 3; ++i) {
      String dbName = "a" + i + "$db";
      try {
        orientDB.drop(dbName);
        Assert.fail();
      } catch (OStorageException e) {
        // ignore
      }

      orientDB.create(dbName, ODatabaseType.PLOCAL, "admin", "admin", "admin");
      Assert.assertTrue(orientDB.exists(dbName));

      orientDB.open(dbName, "admin", "admin").close();
    }

    for (int i = 0; i < 3; ++i) {
      String dbName = "a" + i + "$db";
      Assert.assertTrue(orientDB.exists(dbName));
      orientDB.drop(dbName);
      Assert.assertFalse(orientDB.exists(dbName));
    }
  }

  public void testDbIsNotRemovedOnSecondTry() {
    orientDB.create(DB_NAME + "Remove", ODatabaseType.PLOCAL, "admin", "admin", "admin");

    try {
      orientDB.create(DB_NAME + "Remove", ODatabaseType.PLOCAL, "admin", "admin", "admin");
      Assert.fail();
    } catch (OCoreException e) {
      // ignore all is correct
    }

    if (!remoteDB) {
      final String buildDirectory = System.getProperty("buildDirectory", ".");
      var path = buildDirectory + "/test-db/" + DB_NAME + "Remove";
      Assert.assertTrue(new File(path).exists());
    }

    orientDB.drop(DB_NAME + "Remove");
    try {
      orientDB.drop(DB_NAME + "Remove");
      Assert.fail();
    } catch (OCoreException e) {
      // ignore all is correct
    }
  }
}
