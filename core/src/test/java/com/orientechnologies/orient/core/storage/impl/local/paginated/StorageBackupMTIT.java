package com.orientechnologies.orient.core.storage.impl.local.paginated;

import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import com.orientechnologies.common.io.OFileUtils;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.OxygenDB;
import com.orientechnologies.orient.core.db.OxygenDBConfig;
import com.orientechnologies.orient.core.db.OxygenDBEmbedded;
import com.orientechnologies.orient.core.db.OxygenDBInternal;
import com.orientechnologies.orient.core.db.tool.ODatabaseCompare;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class StorageBackupMTIT {

  private final CountDownLatch latch = new CountDownLatch(1);
  private volatile boolean stop = false;
  private OxygenDB oxygenDB;
  private String dbName;

  @Test
  public void testParallelBackup() throws Exception {
    final String buildDirectory = System.getProperty("buildDirectory", ".");
    dbName = StorageBackupMTIT.class.getSimpleName();
    final String dbDirectory = buildDirectory + File.separator + dbName;
    final File backupDir = new File(buildDirectory, "backupDir");
    final String backupDbName = StorageBackupMTIT.class.getSimpleName() + "BackUp";

    OFileUtils.deleteRecursively(new File(dbDirectory));

    try {

      oxygenDB = new OxygenDB("embedded:" + buildDirectory, OxygenDBConfig.defaultConfig());
      oxygenDB.execute(
          "create database " + dbName + " plocal users ( admin identified by 'admin' role admin)");

      var db = (ODatabaseSessionInternal) oxygenDB.open(dbName, "admin", "admin");

      final OSchema schema = db.getMetadata().getSchema();
      final OClass backupClass = schema.createClass("BackupClass");
      backupClass.createProperty(db, "num", OType.INTEGER);
      backupClass.createProperty(db, "data", OType.BINARY);

      backupClass.createIndex(db, "backupIndex", OClass.INDEX_TYPE.NOTUNIQUE, "num");

      OFileUtils.deleteRecursively(backupDir);

      if (!backupDir.exists()) {
        Assert.assertTrue(backupDir.mkdirs());
      }

      final ExecutorService executor = Executors.newCachedThreadPool();
      final List<Future<Void>> futures = new ArrayList<>();

      for (int i = 0; i < 4; i++) {
        futures.add(executor.submit(new DataWriterCallable()));
      }

      futures.add(executor.submit(new DBBackupCallable(backupDir.getAbsolutePath())));

      latch.countDown();

      TimeUnit.MINUTES.sleep(15);

      stop = true;

      for (Future<Void> future : futures) {
        future.get();
      }

      System.out.println("do inc backup last time");
      db.incrementalBackup(backupDir.getAbsolutePath());

      oxygenDB.close();

      final String backedUpDbDirectory = buildDirectory + File.separator + backupDbName;
      OFileUtils.deleteRecursively(new File(backedUpDbDirectory));

      System.out.println("create and restore");

      OxygenDBEmbedded embedded =
          (OxygenDBEmbedded)
              OxygenDBInternal.embedded(buildDirectory, OxygenDBConfig.defaultConfig());
      embedded.restore(
          backupDbName,
          null,
          null,
          null,
          backupDir.getAbsolutePath(),
          OxygenDBConfig.defaultConfig());
      embedded.close();

      oxygenDB = new OxygenDB("embedded:" + buildDirectory, OxygenDBConfig.defaultConfig());
      final ODatabaseCompare compare =
          new ODatabaseCompare(
              (ODatabaseSessionInternal) oxygenDB.open(dbName, "admin", "admin"),
              (ODatabaseSessionInternal) oxygenDB.open(backupDbName, "admin", "admin"),
              System.out::println);

      System.out.println("compare");
      boolean areSame = compare.compare();
      Assert.assertTrue(areSame);

    } finally {
      if (oxygenDB != null && oxygenDB.isOpen()) {
        try {
          oxygenDB.close();
        } catch (Exception ex) {
          OLogManager.instance().error(this, "", ex);
        }
      }
      try {
        oxygenDB = new OxygenDB("embedded:" + buildDirectory, OxygenDBConfig.defaultConfig());
        oxygenDB.drop(dbName);
        oxygenDB.drop(backupDbName);

        oxygenDB.close();

        OFileUtils.deleteRecursively(backupDir);
      } catch (Exception ex) {
        OLogManager.instance().error(this, "", ex);
      }
    }
  }

  @Test
  public void testParallelBackupEncryption() throws Exception {
    final String buildDirectory = System.getProperty("buildDirectory", ".");
    final String backupDbName = StorageBackupMTIT.class.getSimpleName() + "BackUp";
    final String backedUpDbDirectory = buildDirectory + File.separator + backupDbName;
    final File backupDir = new File(buildDirectory, "backupDir");

    dbName = StorageBackupMTIT.class.getSimpleName();
    String dbDirectory = buildDirectory + File.separator + dbName;

    final OxygenDBConfig config =
        OxygenDBConfig.builder()
            .addConfig(OGlobalConfiguration.STORAGE_ENCRYPTION_KEY, "T1JJRU5UREJfSVNfQ09PTA==")
            .build();

    try {

      OFileUtils.deleteRecursively(new File(dbDirectory));

      oxygenDB = new OxygenDB("embedded:" + buildDirectory, config);
      oxygenDB.execute(
          "create database " + dbName + " plocal users ( admin identified by 'admin' role admin)");

      var db = (ODatabaseSessionInternal) oxygenDB.open(dbName, "admin", "admin");

      final OSchema schema = db.getMetadata().getSchema();
      final OClass backupClass = schema.createClass("BackupClass");
      backupClass.createProperty(db, "num", OType.INTEGER);
      backupClass.createProperty(db, "data", OType.BINARY);

      backupClass.createIndex(db, "backupIndex", OClass.INDEX_TYPE.NOTUNIQUE, "num");

      OFileUtils.deleteRecursively(backupDir);

      if (!backupDir.exists()) {
        Assert.assertTrue(backupDir.mkdirs());
      }

      final ExecutorService executor = Executors.newCachedThreadPool();
      final List<Future<Void>> futures = new ArrayList<>();

      for (int i = 0; i < 4; i++) {
        futures.add(executor.submit(new DataWriterCallable()));
      }

      futures.add(executor.submit(new DBBackupCallable(backupDir.getAbsolutePath())));

      latch.countDown();

      TimeUnit.MINUTES.sleep(5);

      stop = true;

      for (Future<Void> future : futures) {
        future.get();
      }

      System.out.println("do inc backup last time");
      db.incrementalBackup(backupDir.getAbsolutePath());

      oxygenDB.close();

      OFileUtils.deleteRecursively(new File(backedUpDbDirectory));

      System.out.println("create and restore");

      OxygenDBEmbedded embedded =
          (OxygenDBEmbedded) OxygenDBInternal.embedded(buildDirectory, config);
      embedded.restore(backupDbName, null, null, null, backupDir.getAbsolutePath(), config);
      embedded.close();

      OGlobalConfiguration.STORAGE_ENCRYPTION_KEY.setValue("T1JJRU5UREJfSVNfQ09PTA==");
      oxygenDB = new OxygenDB("embedded:" + buildDirectory, OxygenDBConfig.defaultConfig());
      final ODatabaseCompare compare =
          new ODatabaseCompare(
              (ODatabaseSessionInternal) oxygenDB.open(dbName, "admin", "admin"),
              (ODatabaseSessionInternal) oxygenDB.open(backupDbName, "admin", "admin"),
              System.out::println);
      System.out.println("compare");

      boolean areSame = compare.compare();
      Assert.assertTrue(areSame);

    } finally {
      if (oxygenDB != null && oxygenDB.isOpen()) {
        try {
          oxygenDB.close();
        } catch (Exception ex) {
          OLogManager.instance().error(this, "", ex);
        }
      }
      try {
        oxygenDB = new OxygenDB("embedded:" + buildDirectory, config);
        oxygenDB.drop(dbName);
        oxygenDB.drop(backupDbName);

        oxygenDB.close();

        OFileUtils.deleteRecursively(backupDir);
      } catch (Exception ex) {
        OLogManager.instance().error(this, "", ex);
      }
    }
  }

  private final class DataWriterCallable implements Callable<Void> {

    @Override
    public Void call() throws Exception {
      latch.await();

      System.out.println(Thread.currentThread() + " - start writing");

      try (var ignored = oxygenDB.open(dbName, "admin", "admin")) {
        final Random random = new Random();
        while (!stop) {
          try {
            final byte[] data = new byte[16];
            random.nextBytes(data);

            final int num = random.nextInt();

            final ODocument document = new ODocument("BackupClass");
            document.field("num", num);
            document.field("data", data);

            document.save();
          } catch (OModificationOperationProhibitedException e) {
            System.out.println("Modification prohibited ... wait ...");
            //noinspection BusyWait
            Thread.sleep(1000);
          } catch (Exception | Error e) {
            OLogManager.instance().error(this, "", e);
            throw e;
          }
        }
      }

      System.out.println(Thread.currentThread() + " - done writing");

      return null;
    }
  }

  public final class DBBackupCallable implements Callable<Void> {

    private final String backupPath;

    public DBBackupCallable(String backupPath) {
      this.backupPath = backupPath;
    }

    @Override
    public Void call() throws Exception {
      latch.await();

      try (var db = oxygenDB.open(dbName, "admin", "admin")) {
        System.out.println(Thread.currentThread() + " - start backup");
        while (!stop) {
          TimeUnit.MINUTES.sleep(1);

          System.out.println(Thread.currentThread() + " do inc backup");
          db.incrementalBackup(backupPath);
          System.out.println(Thread.currentThread() + " done inc backup");
        }
      } catch (Exception | Error e) {
        OLogManager.instance().error(this, "", e);
        throw e;
      }

      System.out.println(Thread.currentThread() + " - stop backup");

      return null;
    }
  }
}
