package com.jetbrains.youtrack.db.internal.core.db.tool;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseType;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDB;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DatabaseImportTest {

  @Test
  public void exportImportOnlySchemaTest() throws IOException {
    String databaseName = "export";
    final String exportDbPath = "target/export_" + DatabaseImportTest.class.getSimpleName();
    YouTrackDB youTrackDB = YouTrackDB.embedded(exportDbPath, YouTrackDBConfig.defaultConfig());
    youTrackDB.createIfNotExists(databaseName, DatabaseType.PLOCAL, "admin", "admin", "admin");

    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (final DatabaseSession db = youTrackDB.open(databaseName, "admin", "admin")) {
      db.createClass("SimpleClass");

      final DatabaseExport export =
          new DatabaseExport((DatabaseSessionInternal) db, output, iText -> {
          });
      export.setOptions(" -excludeAll -includeSchema=true");
      export.exportDatabase();
    }
    youTrackDB.drop(databaseName);
    youTrackDB.close();

    final String importDbPath = "target/import_" + DatabaseImportTest.class.getSimpleName();
    youTrackDB = YouTrackDB.embedded(importDbPath, YouTrackDBConfig.defaultConfig());
    databaseName = "import";

    youTrackDB.createIfNotExists(databaseName, DatabaseType.PLOCAL, "admin", "admin", "admin");
    try (var db = (DatabaseSessionInternal) youTrackDB.open(databaseName, "admin",
        "admin")) {
      final DatabaseImport importer =
          new DatabaseImport(
              db,
              new ByteArrayInputStream(output.toByteArray()),
              iText -> {
              });
      importer.importDatabase();
      Assert.assertTrue(db.getMetadata().getSchema().existsClass("SimpleClass"));
    }
    youTrackDB.drop(databaseName);
    youTrackDB.close();
  }
}
