package com.jetbrains.youtrack.db.internal.core.db.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jetbrains.youtrack.db.internal.DBTestBase;
import com.jetbrains.youtrack.db.internal.core.OCreateDatabaseUtil;
import com.jetbrains.youtrack.db.internal.core.config.OStorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.OMetadataUpdateListener;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal.ATTRIBUTES;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.exception.YTDatabaseException;
import com.jetbrains.youtrack.db.internal.core.index.OIndexManagerAbstract;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.OSchemaShared;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.YTSequence;
import java.util.Locale;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ODatabaseMetadataUpdateListener {

  private YouTrackDB youTrackDB;
  private YTDatabaseSessionInternal session;
  private int count;

  @Before
  public void before() {
    youTrackDB =
        OCreateDatabaseUtil.createDatabase("test", DBTestBase.embeddedDBUrl(getClass()),
            OCreateDatabaseUtil.TYPE_MEMORY);
    session = (YTDatabaseSessionInternal) youTrackDB.open("test", "admin",
        OCreateDatabaseUtil.NEW_ADMIN_PASSWORD);
    count = 0;
    OMetadataUpdateListener listener =
        new OMetadataUpdateListener() {

          @Override
          public void onSchemaUpdate(YTDatabaseSessionInternal session, String database,
              OSchemaShared schema) {
            count++;
            assertNotNull(schema);
          }

          @Override
          public void onIndexManagerUpdate(YTDatabaseSessionInternal session, String database,
              OIndexManagerAbstract indexManager) {
            count++;
            assertNotNull(indexManager);
          }

          @Override
          public void onFunctionLibraryUpdate(YTDatabaseSessionInternal session, String database) {
            count++;
          }

          @Override
          public void onSequenceLibraryUpdate(YTDatabaseSessionInternal session, String database) {
            count++;
          }

          @Override
          public void onStorageConfigurationUpdate(String database, OStorageConfiguration update) {
            count++;
            assertNotNull(update);
          }
        };

    session.getSharedContext().registerListener(listener);
  }

  @Test
  public void testSchemaUpdateListener() {
    session.createClass("test1");
    assertEquals(count, 1);
  }

  @Test
  public void testFunctionUpdateListener() {
    session.getMetadata().getFunctionLibrary().createFunction("some");
    assertEquals(count, 1);
  }

  @Test
  public void testSequenceUpdate() {
    try {
      session
          .getMetadata()
          .getSequenceLibrary()
          .createSequence("sequence1", YTSequence.SEQUENCE_TYPE.ORDERED, null);
    } catch (YTDatabaseException exc) {
      Assert.fail("Failed to create sequence");
    }
    assertEquals(count, 1);
  }

  @Test
  public void testIndexUpdate() {
    session
        .createClass("Some")
        .createProperty(session, "test", YTType.STRING)
        .createIndex(session, YTClass.INDEX_TYPE.NOTUNIQUE);
    assertEquals(count, 3);
  }

  @Test
  public void testIndexConfigurationUpdate() {
    session.set(ATTRIBUTES.LOCALECOUNTRY, Locale.GERMAN);
    assertEquals(count, 1);
  }

  @After
  public void after() {
    session.close();
    youTrackDB.close();
  }
}