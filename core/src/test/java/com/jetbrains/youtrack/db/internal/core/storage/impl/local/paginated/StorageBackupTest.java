package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated;

import com.jetbrains.youtrack.db.internal.DBTestBase;
import com.jetbrains.youtrack.db.internal.common.io.FileUtils;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBEmbedded;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import com.jetbrains.youtrack.db.internal.core.db.tool.ODatabaseCompare;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTSchema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.io.File;
import java.util.Random;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StorageBackupTest {

  private String testDirectory;

  @Before
  public void before() {
    testDirectory = DBTestBase.getDirectoryPath(getClass());
  }

  @Test
  public void testSingeThreadFullBackup() {
    final String dbName = StorageBackupTest.class.getSimpleName();
    final String dbDirectory = testDirectory + File.separator + dbName;

    FileUtils.deleteRecursively(new File(dbDirectory));

    YouTrackDB youTrackDB = new YouTrackDB("embedded:" + testDirectory,
        YouTrackDBConfig.defaultConfig());
    youTrackDB.execute(
        "create database `" + dbName + "` plocal users(admin identified by 'admin' role admin)");

    var db = (YTDatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin");

    final YTSchema schema = db.getMetadata().getSchema();
    final YTClass backupClass = schema.createClass("BackupClass");
    backupClass.createProperty(db, "num", YTType.INTEGER);
    backupClass.createProperty(db, "data", YTType.BINARY);

    backupClass.createIndex(db, "backupIndex", YTClass.INDEX_TYPE.NOTUNIQUE, "num");

    final Random random = new Random();
    for (int i = 0; i < 1000; i++) {
      db.begin();
      final byte[] data = new byte[16];
      random.nextBytes(data);

      final int num = random.nextInt();

      final EntityImpl document = new EntityImpl("BackupClass");
      document.field("num", num);
      document.field("data", data);

      document.save();
      db.commit();
    }

    final File backupDir = new File(testDirectory, "backupDir");
    FileUtils.deleteRecursively(backupDir);

    if (!backupDir.exists()) {
      Assert.assertTrue(backupDir.mkdirs());
    }

    db.incrementalBackup(backupDir.getAbsolutePath());
    youTrackDB.close();

    final String backupDbName = StorageBackupTest.class.getSimpleName() + "BackUp";
    final String backedUpDbDirectory = testDirectory + File.separator + backupDbName;

    FileUtils.deleteRecursively(new File(backedUpDbDirectory));

    YouTrackDBEmbedded embedded =
        (YouTrackDBEmbedded)
            YouTrackDBInternal.embedded(testDirectory, YouTrackDBConfig.defaultConfig());
    embedded.restore(
        backupDbName,
        null,
        null,
        null,
        backupDir.getAbsolutePath(),
        YouTrackDBConfig.defaultConfig());
    embedded.close();

    youTrackDB = new YouTrackDB("embedded:" + testDirectory, YouTrackDBConfig.defaultConfig());
    final ODatabaseCompare compare =
        new ODatabaseCompare(
            (YTDatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin"),
            (YTDatabaseSessionInternal) youTrackDB.open(backupDbName, "admin", "admin"),
            System.out::println);

    Assert.assertTrue(compare.compare());

    if (youTrackDB.isOpen()) {
      youTrackDB.close();
    }

    youTrackDB = new YouTrackDB("embedded:" + testDirectory, YouTrackDBConfig.defaultConfig());
    youTrackDB.drop(dbName);
    youTrackDB.drop(backupDbName);

    youTrackDB.close();

    FileUtils.deleteRecursively(backupDir);
  }

  @Test
  public void testSingeThreadIncrementalBackup() {
    final String dbDirectory =
        testDirectory + File.separator + StorageBackupTest.class.getSimpleName();
    FileUtils.deleteRecursively(new File(dbDirectory));

    YouTrackDB youTrackDB = new YouTrackDB("embedded:" + testDirectory,
        YouTrackDBConfig.defaultConfig());

    final String dbName = StorageBackupTest.class.getSimpleName();
    youTrackDB.execute(
        "create database `" + dbName + "` plocal users(admin identified by 'admin' role admin)");

    var db = (YTDatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin");

    final YTSchema schema = db.getMetadata().getSchema();
    final YTClass backupClass = schema.createClass("BackupClass");
    backupClass.createProperty(db, "num", YTType.INTEGER);
    backupClass.createProperty(db, "data", YTType.BINARY);

    backupClass.createIndex(db, "backupIndex", YTClass.INDEX_TYPE.NOTUNIQUE, "num");

    final Random random = new Random();
    for (int i = 0; i < 1000; i++) {
      db.begin();
      final byte[] data = new byte[16];
      random.nextBytes(data);

      final int num = random.nextInt();

      final EntityImpl document = new EntityImpl("BackupClass");
      document.field("num", num);
      document.field("data", data);

      document.save();
      db.commit();
    }

    final File backupDir = new File(testDirectory, "backupDir");
    FileUtils.deleteRecursively(backupDir);

    if (!backupDir.exists()) {
      Assert.assertTrue(backupDir.mkdirs());
    }

    db.incrementalBackup(backupDir.getAbsolutePath());

    for (int n = 0; n < 3; n++) {
      for (int i = 0; i < 1000; i++) {
        db.begin();
        final byte[] data = new byte[16];
        random.nextBytes(data);

        final int num = random.nextInt();

        final EntityImpl document = new EntityImpl("BackupClass");
        document.field("num", num);
        document.field("data", data);

        document.save();
        db.commit();
      }

      db.incrementalBackup(backupDir.getAbsolutePath());
    }

    db.incrementalBackup(backupDir.getAbsolutePath());
    db.close();

    youTrackDB.close();

    final String backupDbName = StorageBackupTest.class.getSimpleName() + "BackUp";

    final String backedUpDbDirectory = testDirectory + File.separator + backupDbName;
    FileUtils.deleteRecursively(new File(backedUpDbDirectory));

    YouTrackDBEmbedded embedded =
        (YouTrackDBEmbedded)
            YouTrackDBInternal.embedded(testDirectory, YouTrackDBConfig.defaultConfig());
    embedded.restore(
        backupDbName,
        null,
        null,
        null,
        backupDir.getAbsolutePath(),
        YouTrackDBConfig.defaultConfig());
    embedded.close();

    youTrackDB = new YouTrackDB("embedded:" + testDirectory, YouTrackDBConfig.defaultConfig());
    final ODatabaseCompare compare =
        new ODatabaseCompare(
            (YTDatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin"),
            (YTDatabaseSessionInternal) youTrackDB.open(backupDbName, "admin", "admin"),
            System.out::println);

    Assert.assertTrue(compare.compare());

    if (youTrackDB.isOpen()) {
      youTrackDB.close();
    }

    youTrackDB = new YouTrackDB("embedded:" + testDirectory, YouTrackDBConfig.defaultConfig());
    youTrackDB.drop(dbName);
    youTrackDB.drop(backupDbName);

    youTrackDB.close();

    FileUtils.deleteRecursively(backupDir);
  }

  @Test
  public void testSingeThreadIncrementalBackupEncryption() {
    final String dbDirectory =
        testDirectory + File.separator + StorageBackupTest.class.getSimpleName();
    FileUtils.deleteRecursively(new File(dbDirectory));

    final YouTrackDBConfig config =
        YouTrackDBConfig.builder()
            .addConfig(GlobalConfiguration.STORAGE_ENCRYPTION_KEY, "T1JJRU5UREJfSVNfQ09PTA==")
            .build();
    YouTrackDB youTrackDB = new YouTrackDB("embedded:" + testDirectory, config);

    final String dbName = StorageBackupTest.class.getSimpleName();
    youTrackDB.execute(
        "create database `" + dbName + "` plocal users(admin identified by 'admin' role admin)");

    var db = (YTDatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin");

    final YTSchema schema = db.getMetadata().getSchema();
    final YTClass backupClass = schema.createClass("BackupClass");
    backupClass.createProperty(db, "num", YTType.INTEGER);
    backupClass.createProperty(db, "data", YTType.BINARY);

    backupClass.createIndex(db, "backupIndex", YTClass.INDEX_TYPE.NOTUNIQUE, "num");

    final Random random = new Random();
    for (int i = 0; i < 1000; i++) {
      db.begin();
      final byte[] data = new byte[16];
      random.nextBytes(data);

      final int num = random.nextInt();

      final EntityImpl document = new EntityImpl("BackupClass");
      document.field("num", num);
      document.field("data", data);

      document.save();
      db.commit();
    }

    final File backupDir = new File(testDirectory, "backupDir");
    FileUtils.deleteRecursively(backupDir);

    if (!backupDir.exists()) {
      Assert.assertTrue(backupDir.mkdirs());
    }

    db.incrementalBackup(backupDir.getAbsolutePath());

    for (int n = 0; n < 3; n++) {
      for (int i = 0; i < 1000; i++) {
        db.begin();
        final byte[] data = new byte[16];
        random.nextBytes(data);

        final int num = random.nextInt();

        final EntityImpl document = new EntityImpl("BackupClass");
        document.field("num", num);
        document.field("data", data);

        document.save();
        db.commit();
      }

      db.incrementalBackup(backupDir.getAbsolutePath());
    }

    db.incrementalBackup(backupDir.getAbsolutePath());
    db.close();

    youTrackDB.close();

    final String backupDbName = StorageBackupTest.class.getSimpleName() + "BackUp";

    final String backedUpDbDirectory = testDirectory + File.separator + backupDbName;
    FileUtils.deleteRecursively(new File(backedUpDbDirectory));

    YouTrackDBEmbedded embedded =
        (YouTrackDBEmbedded) YouTrackDBInternal.embedded(testDirectory, config);
    embedded.restore(backupDbName, null, null, null, backupDir.getAbsolutePath(), config);
    embedded.close();

    GlobalConfiguration.STORAGE_ENCRYPTION_KEY.setValue("T1JJRU5UREJfSVNfQ09PTA==");
    youTrackDB = new YouTrackDB("embedded:" + testDirectory, YouTrackDBConfig.defaultConfig());

    final ODatabaseCompare compare =
        new ODatabaseCompare(
            (YTDatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin"),
            (YTDatabaseSessionInternal) youTrackDB.open(backupDbName, "admin", "admin"),
            System.out::println);

    Assert.assertTrue(compare.compare());

    if (youTrackDB.isOpen()) {
      youTrackDB.close();
    }

    GlobalConfiguration.STORAGE_ENCRYPTION_KEY.setValue(null);

    youTrackDB = new YouTrackDB("embedded:" + testDirectory, config);
    youTrackDB.drop(dbName);
    youTrackDB.drop(backupDbName);

    youTrackDB.close();

    FileUtils.deleteRecursively(backupDir);

    GlobalConfiguration.STORAGE_ENCRYPTION_KEY.setValue(null);
  }
}