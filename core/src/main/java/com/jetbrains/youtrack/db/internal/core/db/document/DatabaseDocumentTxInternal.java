package com.jetbrains.youtrack.db.internal.core.db.document;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;

/**
 *
 */
public class DatabaseDocumentTxInternal {

  private DatabaseDocumentTxInternal() {
  }

  public static DatabaseSessionInternal getInternal(DatabaseSessionInternal db) {
    if (db instanceof DatabaseDocumentTx) {
      db = ((DatabaseDocumentTx) db).internal;
    }
    return db;
  }

  public static DatabaseDocumentTx wrap(DatabaseSessionInternal database) {
    return new DatabaseDocumentTx(database, null);
  }

  public static YouTrackDBInternal getOrCreateEmbeddedFactory(
      String databaseDirectory, YouTrackDBConfig config) {
    return DatabaseDocumentTx.getOrCreateEmbeddedFactory(databaseDirectory, config);
  }

  public static YouTrackDBInternal getOrCreateRemoteFactory(String url) {
    return DatabaseDocumentTx.getOrCreateRemoteFactory(url);
  }

  public static void closeAllOnShutdown() {
    DatabaseDocumentTx.embeddedLock.lock();
    try {
      for (YouTrackDBInternal factory : DatabaseDocumentTx.embedded.values()) {
        factory.internalClose();
      }
      DatabaseDocumentTx.embedded.clear();
    } finally {
      DatabaseDocumentTx.embeddedLock.unlock();
    }

    DatabaseDocumentTx.remoteLock.lock();
    try {
      DatabaseDocumentTx.remote.clear();
    } finally {
      DatabaseDocumentTx.remoteLock.unlock();
    }
  }
}
