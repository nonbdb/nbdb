package com.orientechnologies.orient.client.remote.db.document;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.IndexManagerRemote;
import com.orientechnologies.orient.client.remote.YouTrackDBRemote;
import com.orientechnologies.orient.client.remote.metadata.schema.SchemaRemote;
import com.orientechnologies.orient.client.remote.metadata.security.SecurityRemote;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.SharedContext;
import com.jetbrains.youtrack.db.internal.core.db.StringCache;
import com.jetbrains.youtrack.db.internal.core.metadata.function.FunctionLibraryImpl;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.SequenceLibraryImpl;
import com.jetbrains.youtrack.db.internal.core.schedule.SchedulerImpl;
import com.jetbrains.youtrack.db.internal.core.storage.StorageInfo;

/**
 *
 */
public class SharedContextRemote extends SharedContext {

  public SharedContextRemote(StorageInfo storage, YouTrackDBRemote orientDBRemote) {
    stringCache =
        new StringCache(
            orientDBRemote
                .getContextConfiguration()
                .getValueAsInteger(GlobalConfiguration.DB_STRING_CAHCE_SIZE));
    this.youtrackDB = orientDBRemote;
    this.storage = storage;
    schema = new SchemaRemote();
    security = new SecurityRemote();
    indexManager = new IndexManagerRemote(storage);
    functionLibrary = new FunctionLibraryImpl();
    scheduler = new SchedulerImpl(youtrackDB);
    sequenceLibrary = new SequenceLibraryImpl();
  }

  public synchronized void load(DatabaseSessionInternal database) {
    final long timer = PROFILER.startChrono();

    try {
      if (!loaded) {
        schema.load(database);
        indexManager.load(database);
        // The Immutable snapshot should be after index and schema that require and before
        // everything else that use it
        schema.forceSnapshot(database);
        security.load(database);
        sequenceLibrary.load(database);
        schema.onPostIndexManagement(database);
        loaded = true;
      }
    } finally {
      PROFILER.stopChrono(
          PROFILER.getDatabaseMetric(database.getName(), "metadata.load"),
          "Loading of database metadata",
          timer,
          "db.*.metadata.load");
    }
  }

  @Override
  public synchronized void close() {
    stringCache.close();
    schema.close();
    security.close();
    indexManager.close();
    sequenceLibrary.close();
    loaded = false;
  }

  public synchronized void reload(DatabaseSessionInternal database) {
    schema.reload(database);
    indexManager.reload(database);
    // The Immutable snapshot should be after index and schema that require and before everything
    // else that use it
    schema.forceSnapshot(database);
    security.load(database);
    scheduler.load(database);
    sequenceLibrary.load(database);
    functionLibrary.load(database);
  }
}
