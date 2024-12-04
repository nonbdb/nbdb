package com.orientechnologies.orient.core.db.tool;

import com.orientechnologies.orient.core.OCreateDatabaseUtil;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.YouTrackDB;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ODatabaseImportSimpleCompatibilityTest {

  private YouTrackDB youTrackDB;

  private ODatabaseSessionInternal importDatabase;
  private ODatabaseImport importer;

  private ODatabaseExport export;

  // Known compatibility issue: the deprecation of manual indexes is checked by maven, which makes
  // this test fail: `"manualIndexes":[{"name":"dictionary","content":[]}]`
  @Ignore
  @Test
  public void testImportExportOldEmpty() throws Exception {
    final InputStream emptyDbV2 = load("/databases/databases_2_2/Empty.json");
    Assert.assertNotNull("Input must not be null!", emptyDbV2);
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    Assert.assertEquals(0, output.size());
    final String databaseName = "testImportExportOldEmpty";
    this.setup(databaseName, emptyDbV2, output);

    this.executeImport();
    this.executeExport(" -excludeAll -includeSchema=true -includeManualIndexes=false");

    this.tearDown(databaseName);
    Assert.assertTrue(output.size() > 0);
  }

  // The deprecation of manual indexes is checked by maven, which makes this test fail.
  @Ignore
  @Test
  public void testImportExportOldSimple() throws Exception {
    final InputStream simpleDbV2 = load("/databases/databases_2_2/OrderCustomer-sl-0.json");
    Assert.assertNotNull("Input must not be null!", simpleDbV2);
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    Assert.assertEquals(0, output.size());
    final String databaseName = "testImportExportOldSimple";
    this.setup(databaseName, simpleDbV2, output);

    this.executeImport();
    this.executeExport(" -excludeAll -includeSchema=true -includeManualIndexes=false");

    Assert.assertTrue(importDatabase.getMetadata().getSchema().existsClass("OrderCustomer"));

    this.tearDown(databaseName);
    Assert.assertTrue(output.size() > 0);
  }

  // Fails on OIndexManagerShared with 'manualIndexesAreUsed' == true, due to missing class name and
  // empty fields, thus throwing 'OIndexAbstract.manualIndexesWarning()'.
  // Hence, it is not sufficient to just remove the manualIndexes section in the import JSON
  @Ignore
  @Test
  public void testImportExportNewerSimple() throws Exception {
    // Only required in case of manual indexes:
    System.setProperty("index.allowManualIndexes", String.valueOf(true));

    final InputStream simpleDbV3 = load("/databases/databases_3_1/OrderCustomer-sl-0.json");
    Assert.assertNotNull("Input must not be null!", simpleDbV3);
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    Assert.assertEquals(0, output.size());
    final String databaseName = "testImportExportNewerSimple";
    this.setup(databaseName, simpleDbV3, output);

    this.executeImport();
    this.executeExport(" -excludeAll -includeSchema=true");

    Assert.assertTrue(importDatabase.getMetadata().getSchema().existsClass("OrderCustomer"));

    this.tearDown(databaseName);
    Assert.assertTrue(output.size() > 0);
    System.setProperty(
        OGlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES.getKey(), String.valueOf(false));
  }

  private InputStream load(final String path) throws FileNotFoundException {
    final File file = new File(getClass().getResource(path).getFile());
    return new FileInputStream(file);
  }

  private void setup(
      final String databaseName, final InputStream input, final OutputStream output) {
    final String importDbUrl = "embedded:target/import_" + this.getClass().getSimpleName();
    youTrackDB =
        OCreateDatabaseUtil.createDatabase(
            databaseName, importDbUrl, OCreateDatabaseUtil.TYPE_MEMORY);
    importDatabase = (ODatabaseSessionInternal) youTrackDB.open(databaseName, "admin",
        OCreateDatabaseUtil.NEW_ADMIN_PASSWORD);
    try {
      importer = new ODatabaseImport(importDatabase, input, iText -> {
      });
      export = new ODatabaseExport(importDatabase, output, iText -> {
      });
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void tearDown(final String databaseName) {
    try {
      youTrackDB.drop(databaseName);
      youTrackDB.close();
    } catch (final Exception e) {
      System.out.println("Issues during teardown " + e.getMessage());
    }
  }

  private void executeImport() {
    importer.setOptions(" -includeManualIndexes=true");
    importer.importDatabase();
  }

  public void executeExport(final String options) {
    export.setOptions(options);
    export.exportDatabase();
  }
}
