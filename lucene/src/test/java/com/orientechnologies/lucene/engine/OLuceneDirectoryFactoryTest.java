package com.orientechnologies.lucene.engine;

import static com.orientechnologies.lucene.engine.OLuceneDirectoryFactory.DIRECTORY_MMAP;
import static com.orientechnologies.lucene.engine.OLuceneDirectoryFactory.DIRECTORY_NIO;
import static com.orientechnologies.lucene.engine.OLuceneDirectoryFactory.DIRECTORY_RAM;
import static com.orientechnologies.lucene.engine.OLuceneDirectoryFactory.DIRECTORY_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.orientechnologies.lucene.test.BaseLuceneTest;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.index.OIndexDefinition;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 */
public class OLuceneDirectoryFactoryTest extends BaseLuceneTest {

  private OLuceneDirectoryFactory fc;
  private Map<String, Object> meta;
  private OIndexDefinition indexDef;

  @Before
  public void setUp() throws Exception {
    meta = new HashMap<>();
    indexDef = Mockito.mock(OIndexDefinition.class);
    when(indexDef.getFields()).thenReturn(Collections.emptyList());
    when(indexDef.getClassName()).thenReturn("Song");
    fc = new OLuceneDirectoryFactory();
  }

  @Test
  public void shouldCreateNioFsDirectory() throws Exception {
    meta.put(DIRECTORY_TYPE, DIRECTORY_NIO);
    try (YouTrackDB ctx =
        new YouTrackDB(embeddedDBUrl(getClass()),
            YouTrackDBConfig.defaultConfig())) {
      ctx.execute(
          "create database "
              + databaseName
              + " plocal users (admin identified by 'adminpwd' role admin)");
      YTDatabaseSessionInternal db =
          (YTDatabaseSessionInternal) ctx.open(databaseName, "admin", "adminpwd");
      Directory directory = fc.createDirectory(db.getStorage(), "index.name", meta).getDirectory();
      assertThat(directory).isInstanceOf(NIOFSDirectory.class);
      assertThat(new File(
          getDirectoryPath(getClass()) + File.separator + databaseName
              + "/luceneIndexes/index.name"))
          .exists();
      ctx.drop(databaseName);
    }
  }

  @Test
  public void shouldCreateMMapFsDirectory() throws Exception {
    meta.put(DIRECTORY_TYPE, DIRECTORY_MMAP);
    try (YouTrackDB ctx =
        new YouTrackDB(embeddedDBUrl(getClass()),
            YouTrackDBConfig.defaultConfig())) {
      ctx.execute(
          "create database "
              + databaseName
              + " plocal users (admin identified by 'adminpwd' role admin)");
      YTDatabaseSessionInternal db =
          (YTDatabaseSessionInternal) ctx.open(databaseName, "admin", "adminpwd");
      Directory directory = fc.createDirectory(db.getStorage(), "index.name", meta).getDirectory();
      assertThat(directory).isInstanceOf(MMapDirectory.class);
      assertThat(new File(
          getDirectoryPath(getClass()) + File.separator + databaseName
              + "/luceneIndexes/index.name"))
          .exists();
      ctx.drop(databaseName);
    }
  }

  @Test
  public void shouldCreateRamDirectory() throws Exception {
    meta.put(DIRECTORY_TYPE, DIRECTORY_RAM);
    try (YouTrackDB ctx =
        new YouTrackDB(embeddedDBUrl(getClass()), YouTrackDBConfig.defaultConfig())) {
      ctx.execute(
          "create database "
              + databaseName
              + " plocal users (admin identified by 'adminpwd' role admin)");
      YTDatabaseSessionInternal db =
          (YTDatabaseSessionInternal) ctx.open(databaseName, "admin", "adminpwd");
      Directory directory = fc.createDirectory(db.getStorage(), "index.name", meta).getDirectory();
      assertThat(directory).isInstanceOf(RAMDirectory.class);
      ctx.drop(databaseName);
    }
  }

  @Test
  public void shouldCreateRamDirectoryOnMemoryDatabase() {
    meta.put(DIRECTORY_TYPE, DIRECTORY_RAM);
    try (YouTrackDB ctx =
        new YouTrackDB(embeddedDBUrl(getClass()), YouTrackDBConfig.defaultConfig())) {
      ctx.execute(
          "create database "
              + databaseName
              + " memory users (admin identified by 'adminpwd' role admin)");
      YTDatabaseSessionInternal db =
          (YTDatabaseSessionInternal) ctx.open(databaseName, "admin", "adminpwd");
      final Directory directory =
          fc.createDirectory(db.getStorage(), "index.name", meta).getDirectory();
      // 'ODatabaseType.MEMORY' and 'DIRECTORY_RAM' determines the RAMDirectory.
      assertThat(directory).isInstanceOf(RAMDirectory.class);
      ctx.drop(databaseName);
    }
  }

  @Test
  public void shouldCreateRamDirectoryOnMemoryFromMmapDatabase() {
    meta.put(DIRECTORY_TYPE, DIRECTORY_MMAP);
    try (YouTrackDB ctx =
        new YouTrackDB(embeddedDBUrl(getClass()), YouTrackDBConfig.defaultConfig())) {
      ctx.execute(
          "create database "
              + databaseName
              + " memory users (admin identified by 'adminpwd' role admin)");
      YTDatabaseSessionInternal db =
          (YTDatabaseSessionInternal) ctx.open(databaseName, "admin", "adminpwd");
      final Directory directory =
          fc.createDirectory(db.getStorage(), "index.name", meta).getDirectory();
      // 'ODatabaseType.MEMORY' plus 'DIRECTORY_MMAP' leads to the same result as just
      // 'DIRECTORY_RAM'.
      assertThat(directory).isInstanceOf(RAMDirectory.class);
      ctx.drop(databaseName);
    }
  }
}
