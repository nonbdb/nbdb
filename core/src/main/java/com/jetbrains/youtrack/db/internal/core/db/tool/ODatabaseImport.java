/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.core.db.tool;

import com.jetbrains.youtrack.db.internal.common.exception.YTException;
import com.jetbrains.youtrack.db.internal.common.io.OIOUtils;
import com.jetbrains.youtrack.db.internal.common.listener.OProgressListener;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OBinarySerializer;
import com.jetbrains.youtrack.db.internal.common.util.OPair;
import com.jetbrains.youtrack.db.internal.core.command.OCommandOutputListener;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession.STATUS;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.document.ODocumentFieldWalker;
import com.jetbrains.youtrack.db.internal.core.db.record.OClassTrigger;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.db.tool.importer.OConverterData;
import com.jetbrains.youtrack.db.internal.core.db.tool.importer.OLinksRewriter;
import com.jetbrains.youtrack.db.internal.core.exception.YTConfigurationException;
import com.jetbrains.youtrack.db.internal.core.exception.YTDatabaseException;
import com.jetbrains.youtrack.db.internal.core.exception.YTSchemaException;
import com.jetbrains.youtrack.db.internal.core.exception.YTSerializationException;
import com.jetbrains.youtrack.db.internal.core.id.ChangeableRecordId;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.index.OIndex;
import com.jetbrains.youtrack.db.internal.core.index.OIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.OIndexManagerAbstract;
import com.jetbrains.youtrack.db.internal.core.index.ORuntimeKeyIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.OSimpleKeyIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.metadata.OMetadataDefault;
import com.jetbrains.youtrack.db.internal.core.metadata.function.OFunction;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClassEmbedded;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClassImpl;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTPropertyImpl;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTSchema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.metadata.security.OIdentity;
import com.jetbrains.youtrack.db.internal.core.metadata.security.ORole;
import com.jetbrains.youtrack.db.internal.core.metadata.security.ORule;
import com.jetbrains.youtrack.db.internal.core.metadata.security.OSecurityPolicy;
import com.jetbrains.youtrack.db.internal.core.metadata.security.OSecurityShared;
import com.jetbrains.youtrack.db.internal.core.metadata.security.YTUser;
import com.jetbrains.youtrack.db.internal.core.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.ORecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.OJSONReader;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.OStringSerializerHelper;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string.ORecordSerializerJSON;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ORidSet;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import com.jetbrains.youtrack.db.internal.core.storage.OPhysicalPosition;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Import data from a file into a database.
 */
public class ODatabaseImport extends ODatabaseImpExpAbstract {

  public static final String EXPORT_IMPORT_CLASS_NAME = "___exportImportRIDMap";
  public static final String EXPORT_IMPORT_INDEX_NAME = EXPORT_IMPORT_CLASS_NAME + "Index";

  public static final int IMPORT_RECORD_DUMP_LAP_EVERY_MS = 5000;

  private final Map<YTPropertyImpl, String> linkedClasses = new HashMap<>();
  private final Map<YTClass, List<String>> superClasses = new HashMap<>();
  private OJSONReader jsonReader;
  private boolean schemaImported = false;
  private int exporterVersion = -1;
  private YTRID schemaRecordId;
  private YTRID indexMgrRecordId;

  private boolean deleteRIDMapping = true;

  private boolean preserveClusterIDs = true;
  private boolean migrateLinks = true;
  private boolean rebuildIndexes = true;

  private final Set<String> indexesToRebuild = new HashSet<>();
  private final Map<String, String> convertedClassNames = new HashMap<>();

  private final Int2IntOpenHashMap clusterToClusterMapping = new Int2IntOpenHashMap();

  private int maxRidbagStringSizeBeforeLazyImport = 100_000_000;

  public ODatabaseImport(
      final YTDatabaseSessionInternal database,
      final String fileName,
      final OCommandOutputListener outputListener)
      throws IOException {
    super(database, fileName, outputListener);

    clusterToClusterMapping.defaultReturnValue(-2);
    // TODO: check unclosed stream?
    final BufferedInputStream bufferedInputStream =
        new BufferedInputStream(new FileInputStream(this.fileName));
    bufferedInputStream.mark(1024);
    InputStream inputStream;
    try {
      inputStream = new GZIPInputStream(bufferedInputStream, 16384); // 16KB
    } catch (final Exception ignore) {
      bufferedInputStream.reset();
      inputStream = bufferedInputStream;
    }
    createJsonReaderDefaultListenerAndDeclareIntent(database, outputListener, inputStream);
  }

  public ODatabaseImport(
      final YTDatabaseSessionInternal database,
      final InputStream inputStream,
      final OCommandOutputListener outputListener)
      throws IOException {
    super(database, "streaming", outputListener);
    clusterToClusterMapping.defaultReturnValue(-2);
    createJsonReaderDefaultListenerAndDeclareIntent(database, outputListener, inputStream);
  }

  private void createJsonReaderDefaultListenerAndDeclareIntent(
      final YTDatabaseSessionInternal database,
      final OCommandOutputListener outputListener,
      final InputStream inputStream) {
    if (outputListener == null) {
      listener = text -> {
      };
    }
    jsonReader = new OJSONReader(new InputStreamReader(inputStream));
  }

  @Override
  public ODatabaseImport setOptions(final String options) {
    super.setOptions(options);
    return this;
  }

  @Override
  public void run() {
    importDatabase();
  }

  @Override
  protected void parseSetting(final String option, final List<String> items) {
    if (option.equalsIgnoreCase("-deleteRIDMapping")) {
      deleteRIDMapping = Boolean.parseBoolean(items.get(0));
    } else {
      if (option.equalsIgnoreCase("-preserveClusterIDs")) {
        preserveClusterIDs = Boolean.parseBoolean(items.get(0));
      } else {

        if (option.equalsIgnoreCase("-migrateLinks")) {
          migrateLinks = Boolean.parseBoolean(items.get(0));
        } else {
          if (option.equalsIgnoreCase("-rebuildIndexes")) {
            rebuildIndexes = Boolean.parseBoolean(items.get(0));
          } else {
            super.parseSetting(option, items);
          }
        }
      }
    }
  }

  public ODatabaseImport importDatabase() {
    database.checkSecurity(ORule.ResourceGeneric.DATABASE, ORole.PERMISSION_ALL);
    final boolean preValidation = database.isValidationEnabled();
    try {
      listener.onMessage(
          "\nStarted import of database '" + database.getURL() + "' from " + fileName + "...");
      final long time = System.nanoTime();

      jsonReader.readNext(OJSONReader.BEGIN_OBJECT);
      database.setValidationEnabled(false);
      database.setUser(null);

      removeDefaultNonSecurityClasses();
      database.getMetadata().getIndexManagerInternal().reload(database);

      for (final OIndex index :
          database.getMetadata().getIndexManagerInternal().getIndexes(database)) {
        if (index.isAutomatic()) {
          indexesToRebuild.add(index.getName());
        }
      }

      var beforeImportSchemaSnapshot = database.getMetadata().getImmutableSchemaSnapshot();

      boolean clustersImported = false;
      while (jsonReader.hasNext() && jsonReader.lastChar() != '}') {
        final String tag = jsonReader.readString(OJSONReader.FIELD_ASSIGNMENT);

        if (tag.equals("info")) {
          importInfo();
        } else {
          if (tag.equals("clusters")) {
            importClusters();
            clustersImported = true;
          } else {
            if (tag.equals("schema")) {
              importSchema(clustersImported);
            } else {
              if (tag.equals("records")) {
                importRecords(beforeImportSchemaSnapshot);
              } else {
                if (tag.equals("indexes")) {
                  importIndexes();
                } else {
                  if (tag.equals("manualIndexes")) {
                    importManualIndexes();
                  } else {
                    if (tag.equals("brokenRids")) {
                      processBrokenRids();
                    } else {
                      throw new YTDatabaseImportException(
                          "Invalid format. Found unsupported tag '" + tag + "'");
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (rebuildIndexes) {
        rebuildIndexes();
      }

      // This is needed to insure functions loaded into an open
      // in memory database are available after the import.
      // see issue #5245
      database.getMetadata().reload();

      database.getStorage().synch();
      // status concept seems deprecated, but status `OPEN` is checked elsewhere
      database.setStatus(STATUS.OPEN);

      if (deleteRIDMapping) {
        removeExportImportRIDsMap();
      }
      listener.onMessage(
          "\n\nDatabase import completed in " + ((System.nanoTime() - time) / 1000000) + " ms");
    } catch (final Exception e) {
      final StringWriter writer = new StringWriter();
      writer.append(
          "Error on database import happened just before line "
              + jsonReader.getLineNumber()
              + ", column "
              + jsonReader.getColumnNumber()
              + "\n");
      final PrintWriter printWriter = new PrintWriter(writer);
      e.printStackTrace(printWriter);
      printWriter.flush();

      listener.onMessage(writer.toString());

      try {
        writer.close();
      } catch (final IOException e1) {
        throw new ODatabaseExportException(
            "Error on importing database '" + database.getName() + "' from file: " + fileName, e1);
      }
      throw new ODatabaseExportException(
          "Error on importing database '" + database.getName() + "' from file: " + fileName, e);
    } finally {
      database.setValidationEnabled(preValidation);
      close();
    }
    return this;
  }

  private void processBrokenRids() throws IOException, ParseException {
    final Set<YTRID> brokenRids = new HashSet<>();
    processBrokenRids(brokenRids);
    jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);
  }

  // just read collection so import process can continue
  private void processBrokenRids(final Set<YTRID> brokenRids) throws IOException, ParseException {
    if (exporterVersion >= 12) {
      listener.onMessage(
          "Reading of set of RIDs of records which were detected as broken during database"
              + " export\n");
      jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

      while (true) {
        jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);

        final YTRecordId recordId = new YTRecordId(jsonReader.getValue());
        brokenRids.add(recordId);

        if (jsonReader.lastChar() == ']') {
          break;
        }
      }
    }
    if (migrateLinks) {
      if (exporterVersion >= 12) {
        listener.onMessage(
            brokenRids.size()
                + " were detected as broken during database export, links on those records will be"
                + " removed from result database");
      }
      migrateLinksInImportedDocuments(brokenRids);
    }
  }

  public void rebuildIndexes() {
    database.getMetadata().getIndexManagerInternal().reload(database);

    OIndexManagerAbstract indexManager = database.getMetadata().getIndexManagerInternal();

    listener.onMessage("\nRebuild of stale indexes...");
    for (String indexName : indexesToRebuild) {

      if (indexManager.getIndex(database, indexName) == null) {
        listener.onMessage(
            "\nIndex " + indexName + " is skipped because it is absent in imported DB.");
        continue;
      }

      listener.onMessage("\nStart rebuild index " + indexName);
      database.command("rebuild index " + indexName).close();
      listener.onMessage("\nRebuild  of index " + indexName + " is completed.");
    }
    listener.onMessage("\nStale indexes were rebuilt...");
  }

  public ODatabaseImport removeExportImportRIDsMap() {
    listener.onMessage("\nDeleting RID Mapping table...");

    YTSchema schema = database.getMetadata().getSchema();
    if (schema.getClass(EXPORT_IMPORT_CLASS_NAME) != null) {
      schema.dropClass(EXPORT_IMPORT_CLASS_NAME);
    }

    listener.onMessage("OK\n");
    return this;
  }

  public void close() {
  }

  public boolean isMigrateLinks() {
    return migrateLinks;
  }

  public void setMigrateLinks(boolean migrateLinks) {
    this.migrateLinks = migrateLinks;
  }

  public boolean isRebuildIndexes() {
    return rebuildIndexes;
  }

  public void setRebuildIndexes(boolean rebuildIndexes) {
    this.rebuildIndexes = rebuildIndexes;
  }

  public boolean isPreserveClusterIDs() {
    return preserveClusterIDs;
  }

  public void setPreserveClusterIDs(boolean preserveClusterIDs) {
    this.preserveClusterIDs = preserveClusterIDs;
  }

  public boolean isDeleteRIDMapping() {
    return deleteRIDMapping;
  }

  public void setDeleteRIDMapping(boolean deleteRIDMapping) {
    this.deleteRIDMapping = deleteRIDMapping;
  }

  public void setOption(final String option, String value) {
    parseSetting("-" + option, Collections.singletonList(value));
  }

  protected void removeDefaultClusters() {
    listener.onMessage(
        "\nWARN: Exported database does not support manual index separation."
            + " Manual index cluster will be dropped.");

    // In v4 new cluster for manual indexes has been implemented. To keep database consistent we
    // should shift back all clusters and recreate cluster for manual indexes in the end.
    database.dropCluster(OMetadataDefault.CLUSTER_MANUAL_INDEX_NAME);

    final YTSchema schema = database.getMetadata().getSchema();
    if (schema.existsClass(YTUser.CLASS_NAME)) {
      schema.dropClass(YTUser.CLASS_NAME);
    }
    if (schema.existsClass(ORole.CLASS_NAME)) {
      schema.dropClass(ORole.CLASS_NAME);
    }
    if (schema.existsClass(OSecurityShared.RESTRICTED_CLASSNAME)) {
      schema.dropClass(OSecurityShared.RESTRICTED_CLASSNAME);
    }
    if (schema.existsClass(OFunction.CLASS_NAME)) {
      schema.dropClass(OFunction.CLASS_NAME);
    }
    if (schema.existsClass("ORIDs")) {
      schema.dropClass("ORIDs");
    }
    if (schema.existsClass(OClassTrigger.CLASSNAME)) {
      schema.dropClass(OClassTrigger.CLASSNAME);
    }

    database.dropCluster(Storage.CLUSTER_DEFAULT_NAME);

    database.setDefaultClusterId(database.addCluster(Storage.CLUSTER_DEFAULT_NAME));

    // Starting from v4 schema has been moved to internal cluster.
    // Create a stub at #2:0 to prevent cluster position shifting.
    database.begin();
    new EntityImpl().save(Storage.CLUSTER_DEFAULT_NAME);
    database.commit();

    database.getSharedContext().getSecurity().create(database);
  }

  private void importInfo() throws IOException, ParseException {
    listener.onMessage("\nImporting database info...");

    jsonReader.readNext(OJSONReader.BEGIN_OBJECT);
    while (jsonReader.lastChar() != '}') {
      final String fieldName = jsonReader.readString(OJSONReader.FIELD_ASSIGNMENT);
      if (fieldName.equals("exporter-version")) {
        exporterVersion = jsonReader.readInteger(OJSONReader.NEXT_IN_OBJECT);
      } else {
        if (fieldName.equals("schemaRecordId")) {
          schemaRecordId = new YTRecordId(jsonReader.readString(OJSONReader.NEXT_IN_OBJECT));
        } else {
          if (fieldName.equals("indexMgrRecordId")) {
            indexMgrRecordId = new YTRecordId(jsonReader.readString(OJSONReader.NEXT_IN_OBJECT));
          } else {
            jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);
          }
        }
      }
    }
    jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);

    if (schemaRecordId == null) {
      schemaRecordId =
          new YTRecordId(database.getStorageInfo().getConfiguration().getSchemaRecordId());
    }

    if (indexMgrRecordId == null) {
      indexMgrRecordId =
          new YTRecordId(database.getStorageInfo().getConfiguration().getIndexMgrRecordId());
    }

    listener.onMessage("OK");
  }

  private void removeDefaultNonSecurityClasses() {
    listener.onMessage(
        "\nNon merge mode (-merge=false): removing all default non security classes");

    final YTSchema schema = database.getMetadata().getSchema();
    final Collection<YTClass> classes = schema.getClasses();
    final YTClass role = schema.getClass(ORole.CLASS_NAME);
    final YTClass user = schema.getClass(YTUser.CLASS_NAME);
    final YTClass identity = schema.getClass(OIdentity.CLASS_NAME);
    // final YTClass oSecurityPolicy = schema.getClass(OSecurityPolicy.class.getSimpleName());
    final Map<String, YTClass> classesToDrop = new HashMap<>();
    final Set<String> indexNames = new HashSet<>();
    for (final YTClass dbClass : classes) {
      final String className = dbClass.getName();
      if (!dbClass.isSuperClassOf(role)
          && !dbClass.isSuperClassOf(user)
          && !dbClass.isSuperClassOf(identity) /*&& !dbClass.isSuperClassOf(oSecurityPolicy)*/) {
        classesToDrop.put(className, dbClass);
        for (final OIndex index : dbClass.getIndexes(database)) {
          indexNames.add(index.getName());
        }
      }
    }

    final OIndexManagerAbstract indexManager = database.getMetadata().getIndexManagerInternal();
    for (final String indexName : indexNames) {
      indexManager.dropIndex(database, indexName);
    }

    int removedClasses = 0;
    while (!classesToDrop.isEmpty()) {
      final AbstractList<String> classesReadyToDrop = new ArrayList<>();
      for (final String className : classesToDrop.keySet()) {
        boolean isSuperClass = false;
        for (YTClass dbClass : classesToDrop.values()) {
          final List<YTClass> parentClasses = dbClass.getSuperClasses();
          if (parentClasses != null) {
            for (YTClass parentClass : parentClasses) {
              if (className.equalsIgnoreCase(parentClass.getName())) {
                isSuperClass = true;
                break;
              }
            }
          }
        }
        if (!isSuperClass) {
          classesReadyToDrop.add(className);
        }
      }
      for (final String className : classesReadyToDrop) {
        schema.dropClass(className);
        classesToDrop.remove(className);
        removedClasses++;
        listener.onMessage("\n- Class " + className + " was removed.");
      }
    }
    schema.reload();
    listener.onMessage("\nRemoved " + removedClasses + " classes.");
  }

  private void importManualIndexes() throws IOException, ParseException {
    listener.onMessage("\nImporting manual index entries...");

    EntityImpl document = new EntityImpl();

    OIndexManagerAbstract indexManager = database.getMetadata().getIndexManagerInternal();
    // FORCE RELOADING
    indexManager.reload(database);

    int n = 0;
    do {
      jsonReader.readNext(OJSONReader.BEGIN_OBJECT);

      jsonReader.readString(OJSONReader.FIELD_ASSIGNMENT);
      final String indexName = jsonReader.readString(OJSONReader.NEXT_IN_ARRAY);

      if (indexName == null || indexName.length() == 0) {
        return;
      }

      listener.onMessage("\n- Index '" + indexName + "'...");

      final OIndex index =
          database.getMetadata().getIndexManagerInternal().getIndex(database, indexName);

      long tot = 0;

      jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

      do {
        final String value = jsonReader.readString(OJSONReader.NEXT_IN_ARRAY).trim();
        if ("[]".equals(value)) {
          return;
        }

        if (!value.isEmpty()) {
          document = (EntityImpl) ORecordSerializerJSON.INSTANCE.fromString(database, value,
              document, null);
          document.setLazyLoad(false);

          final YTIdentifiable oldRid = document.field("rid");
          assert oldRid != null;

          final YTIdentifiable newRid;
          if (!document.<Boolean>field("binary")) {
            try (final YTResultSet result =
                database.query(
                    "select value from " + EXPORT_IMPORT_CLASS_NAME + " where key = ?",
                    String.valueOf(oldRid))) {
              if (!result.hasNext()) {
                newRid = oldRid;
              } else {
                newRid = new YTRecordId(result.next().<String>getProperty("value"));
              }
            }

            index.put(database, document.field("key"), newRid.getIdentity());
          } else {
            ORuntimeKeyIndexDefinition<?> runtimeKeyIndexDefinition =
                (ORuntimeKeyIndexDefinition<?>) index.getDefinition();
            OBinarySerializer<?> binarySerializer = runtimeKeyIndexDefinition.getSerializer();

            try (final YTResultSet result =
                database.query(
                    "select value from " + EXPORT_IMPORT_CLASS_NAME + " where key = ?",
                    String.valueOf(document.<YTIdentifiable>field("rid")))) {
              if (!result.hasNext()) {
                newRid = document.field("rid");
              } else {
                newRid = new YTRecordId(result.next().<String>getProperty("value"));
              }
            }

            index.put(database, binarySerializer.deserialize(document.field("key"), 0), newRid);
          }
          tot++;
        }
      } while (jsonReader.lastChar() == ',');

      if (index != null) {
        listener.onMessage("OK (" + tot + " entries)");
        n++;
      } else {
        listener.onMessage("ERR, the index wasn't found in configuration");
      }

      jsonReader.readNext(OJSONReader.END_OBJECT);
      jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);

    } while (jsonReader.lastChar() == ',');

    listener.onMessage("\nDone. Imported " + String.format("%,d", n) + " indexes.");

    jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);
  }

  private void setLinkedClasses() {
    for (final Entry<YTPropertyImpl, String> linkedClass : linkedClasses.entrySet()) {
      linkedClass
          .getKey()
          .setLinkedClass(database,
              database.getMetadata().getSchema().getClass(linkedClass.getValue()));
    }
  }

  private void importSchema(boolean clustersImported) throws IOException, ParseException {
    if (!clustersImported) {
      removeDefaultClusters();
    }

    listener.onMessage("\nImporting database schema...");

    jsonReader.readNext(OJSONReader.BEGIN_OBJECT);
    @SuppressWarnings("unused")
    int schemaVersion =
        jsonReader
            .readNext(OJSONReader.FIELD_ASSIGNMENT)
            .checkContent("\"version\"")
            .readNumber(OJSONReader.ANY_NUMBER, true);
    jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);
    jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT);
    // This can be removed after the M1 expires
    if (jsonReader.getValue().equals("\"globalProperties\"")) {
      jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);
      do {
        jsonReader.readNext(OJSONReader.BEGIN_OBJECT);
        jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT).checkContent("\"name\"");
        String name = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
        jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT).checkContent("\"global-id\"");
        String id = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
        jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT).checkContent("\"type\"");
        String type = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
        // getDatabase().getMetadata().getSchema().createGlobalProperty(name, YTType.valueOf(type),
        // Integer.valueOf(id));
        jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);
      } while (jsonReader.lastChar() == ',');
      jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);
      jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT);
    }

    if (jsonReader.getValue().equals("\"blob-clusters\"")) {
      String blobClusterIds = jsonReader.readString(OJSONReader.END_COLLECTION, true).trim();
      blobClusterIds = blobClusterIds.substring(1, blobClusterIds.length() - 1);

      if (!"".equals(blobClusterIds)) {
        // READ BLOB CLUSTER IDS
        for (String i :
            OStringSerializerHelper.split(
                blobClusterIds, OStringSerializerHelper.RECORD_SEPARATOR)) {
          Integer cluster = Integer.parseInt(i);
          if (!database.getBlobClusterIds().contains(cluster)) {
            String name = database.getClusterNameById(cluster);
            database.addBlobCluster(name);
          }
        }
      }

      jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);
      jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT);
    }

    jsonReader.checkContent("\"classes\"").readNext(OJSONReader.BEGIN_COLLECTION);

    long classImported = 0;

    try {
      String prevClassName = "";
      do {
        jsonReader.readNext(OJSONReader.BEGIN_OBJECT);

        String className =
            jsonReader
                .readNext(OJSONReader.FIELD_ASSIGNMENT)
                .checkContent("\"name\"")
                .readString(OJSONReader.COMMA_SEPARATOR);
        prevClassName = className;

        String next = jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT).getValue();

        if (next.equals("\"id\"")) {
          // @COMPATIBILITY 1.0rc4 IGNORE THE ID
          next = jsonReader.readString(OJSONReader.COMMA_SEPARATOR);
          next = jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT).getValue();
        }

        final int classDefClusterId;
        if (jsonReader.isContent("\"default-cluster-id\"")) {
          next = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
          classDefClusterId = Integer.parseInt(next);
        } else {
          classDefClusterId = database.getDefaultClusterId();
        }

        int realClassDefClusterId = classDefClusterId;
        if (!clusterToClusterMapping.isEmpty()
            && clusterToClusterMapping.get(classDefClusterId) > -2) {
          realClassDefClusterId = clusterToClusterMapping.get(classDefClusterId);
        }
        String classClusterIds =
            jsonReader
                .readNext(OJSONReader.FIELD_ASSIGNMENT)
                .checkContent("\"cluster-ids\"")
                .readString(OJSONReader.END_COLLECTION, true)
                .trim();

        jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);

        if (className.contains(".")) {
          // MIGRATE OLD NAME WITH . TO _
          final String newClassName = className.replace('.', '_');
          convertedClassNames.put(className, newClassName);

          listener.onMessage(
              "\nWARNING: class '" + className + "' has been renamed in '" + newClassName + "'\n");

          className = newClassName;
        }

        YTClassImpl cls = (YTClassImpl) database.getMetadata().getSchema().getClass(className);

        if (cls != null) {
          if (cls.getDefaultClusterId() != realClassDefClusterId) {
            cls.setDefaultClusterId(database, realClassDefClusterId);
          }
        } else {
          if (clustersImported) {
            cls =
                (YTClassImpl)
                    database
                        .getMetadata()
                        .getSchema()
                        .createClass(className, new int[]{realClassDefClusterId});
          } else {
            if (className.equalsIgnoreCase("ORestricted")) {
              cls = (YTClassImpl) database.getMetadata().getSchema().createAbstractClass(className);
            } else {
              cls = (YTClassImpl) database.getMetadata().getSchema().createClass(className);
            }
          }
        }

        if (clustersImported) {
          // REMOVE BRACES
          classClusterIds = classClusterIds.substring(1, classClusterIds.length() - 1);

          // ASSIGN OTHER CLUSTER IDS
          for (int i : OStringSerializerHelper.splitIntArray(classClusterIds)) {
            if (i != -1) {
              if (!clusterToClusterMapping.isEmpty()
                  && clusterToClusterMapping.get(classDefClusterId) > -2) {
                i = clusterToClusterMapping.get(i);
              }
              cls.addClusterId(database, i);
            }
          }
        }

        String value;
        while (jsonReader.lastChar() == ',') {
          jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT);
          value = jsonReader.getValue();

          switch (value) {
            case "\"strictMode\"" ->
                cls.setStrictMode(database, jsonReader.readBoolean(OJSONReader.NEXT_IN_OBJECT));
            case "\"abstract\"" ->
                cls.setAbstract(database, jsonReader.readBoolean(OJSONReader.NEXT_IN_OBJECT));
            case "\"oversize\"" -> {
              final String oversize = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
              cls.setOverSize(database, Float.parseFloat(oversize));
            }
            case "\"short-name\"" -> {
              final String shortName = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
              if (!cls.getName().equalsIgnoreCase(shortName)) {
                cls.setShortName(database, shortName);
              }
            }
            case "\"super-class\"" -> {
              // @compatibility <2.1 SINGLE CLASS ONLY
              final String classSuper = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
              final List<String> superClassNames = new ArrayList<String>();
              superClassNames.add(classSuper);
              superClasses.put(cls, superClassNames);
            }
            case "\"super-classes\"" -> {
              // MULTIPLE CLASSES
              jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

              final List<String> superClassNames = new ArrayList<String>();
              while (jsonReader.lastChar() != ']') {
                jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);

                final String clsName = jsonReader.getValue();

                superClassNames.add(OIOUtils.getStringContent(clsName));
              }
              jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);

              superClasses.put(cls, superClassNames);
            }
            case "\"properties\"" -> {
              // GET PROPERTIES
              jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

              while (jsonReader.lastChar() != ']') {
                importProperty(cls);

                if (jsonReader.lastChar() == '}') {
                  jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);
                }
              }
              jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);
            }
            case "\"customFields\"" -> {
              Map<String, String> customFields = importCustomFields();
              for (Entry<String, String> entry : customFields.entrySet()) {
                cls.setCustom(database, entry.getKey(), entry.getValue());
              }
            }
            case "\"cluster-selection\"" ->
              // @SINCE 1.7
                cls.setClusterSelection(database,
                    jsonReader.readString(OJSONReader.NEXT_IN_OBJECT));
          }
        }

        classImported++;

        jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);
      } while (jsonReader.lastChar() == ',');

      this.rebuildCompleteClassInheritence();
      this.setLinkedClasses();

      if (exporterVersion < 11) {
        YTClass role = database.getMetadata().getSchema().getClass("ORole");
        role.dropProperty(database, "rules");
      }

      listener.onMessage("OK (" + classImported + " classes)");
      schemaImported = true;
      jsonReader.readNext(OJSONReader.END_OBJECT);
      jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);
    } catch (final Exception e) {
      LogManager.instance().error(this, "Error on importing schema", e);
      listener.onMessage("ERROR (" + classImported + " entries): " + e);
    }
  }

  private void rebuildCompleteClassInheritence() {
    for (final Entry<YTClass, List<String>> entry : superClasses.entrySet()) {
      for (final String superClassName : entry.getValue()) {
        final YTClass superClass = database.getMetadata().getSchema().getClass(superClassName);

        if (!entry.getKey().getSuperClasses().contains(superClass)) {
          entry.getKey().addSuperClass(database, superClass);
        }
      }
    }
  }

  private void importProperty(final YTClass iClass) throws IOException, ParseException {
    jsonReader.readNext(OJSONReader.NEXT_OBJ_IN_ARRAY);

    if (jsonReader.lastChar() == ']') {
      return;
    }

    final String propName =
        jsonReader
            .readNext(OJSONReader.FIELD_ASSIGNMENT)
            .checkContent("\"name\"")
            .readString(OJSONReader.COMMA_SEPARATOR);

    String next = jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT).getValue();

    if (next.equals("\"id\"")) {
      // @COMPATIBILITY 1.0rc4 IGNORE THE ID
      next = jsonReader.readString(OJSONReader.COMMA_SEPARATOR);
      next = jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT).getValue();
    }
    next = jsonReader.checkContent("\"type\"").readString(OJSONReader.NEXT_IN_OBJECT);

    final YTType type = YTType.valueOf(next);

    String attrib;
    String value = null;

    String min = null;
    String max = null;
    String linkedClass = null;
    YTType linkedType = null;
    boolean mandatory = false;
    boolean readonly = false;
    boolean notNull = false;
    String collate = null;
    String regexp = null;
    String defaultValue = null;

    Map<String, String> customFields = null;

    while (jsonReader.lastChar() == ',') {
      jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT);

      attrib = jsonReader.getValue();
      if (!attrib.equals("\"customFields\"")) {
        value =
            jsonReader.readString(
                OJSONReader.NEXT_IN_OBJECT, false, OJSONReader.DEFAULT_JUMP, null, false);
      }

      if (attrib.equals("\"min\"")) {
        min = value;
      } else {
        if (attrib.equals("\"max\"")) {
          max = value;
        } else {
          if (attrib.equals("\"linked-class\"")) {
            linkedClass = value;
          } else {
            if (attrib.equals("\"mandatory\"")) {
              mandatory = Boolean.parseBoolean(value);
            } else {
              if (attrib.equals("\"readonly\"")) {
                readonly = Boolean.parseBoolean(value);
              } else {
                if (attrib.equals("\"not-null\"")) {
                  notNull = Boolean.parseBoolean(value);
                } else {
                  if (attrib.equals("\"linked-type\"")) {
                    linkedType = YTType.valueOf(value);
                  } else {
                    if (attrib.equals("\"collate\"")) {
                      collate = value;
                    } else {
                      if (attrib.equals("\"default-value\"")) {
                        defaultValue = value;
                      } else {
                        if (attrib.equals("\"customFields\"")) {
                          customFields = importCustomFields();
                        } else {
                          if (attrib.equals("\"regexp\"")) {
                            regexp = value;
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    YTPropertyImpl prop = (YTPropertyImpl) iClass.getProperty(propName);
    if (prop == null) {
      // CREATE IT
      prop = (YTPropertyImpl) iClass.createProperty(database, propName, type, (YTType) null, true);
    }
    prop.setMandatory(database, mandatory);
    prop.setReadonly(database, readonly);
    prop.setNotNull(database, notNull);

    if (min != null) {
      prop.setMin(database, min);
    }
    if (max != null) {
      prop.setMax(database, max);
    }
    if (linkedClass != null) {
      linkedClasses.put(prop, linkedClass);
    }
    if (linkedType != null) {
      prop.setLinkedType(database, linkedType);
    }
    if (collate != null) {
      prop.setCollate(database, collate);
    }
    if (regexp != null) {
      prop.setRegexp(database, regexp);
    }
    if (defaultValue != null) {
      prop.setDefaultValue(database, value);
    }
    if (customFields != null) {
      for (Entry<String, String> entry : customFields.entrySet()) {
        prop.setCustom(database, entry.getKey(), entry.getValue());
      }
    }
  }

  private Map<String, String> importCustomFields() throws ParseException, IOException {
    Map<String, String> result = new HashMap<>();

    jsonReader.readNext(OJSONReader.BEGIN_OBJECT);

    while (jsonReader.lastChar() != '}') {
      final String key = jsonReader.readString(OJSONReader.FIELD_ASSIGNMENT);
      final String value = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);

      result.put(key, value);
    }

    jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);

    return result;
  }

  private long importClusters() throws ParseException, IOException {
    listener.onMessage("\nImporting clusters...");

    long total = 0;

    jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

    boolean recreateManualIndex = false;
    if (exporterVersion <= 4) {
      removeDefaultClusters();
      recreateManualIndex = true;
    }

    final Set<String> indexesToRebuild = new HashSet<>();

    @SuppressWarnings("unused")
    YTRecordId rid = null;
    while (jsonReader.lastChar() != ']') {
      jsonReader.readNext(OJSONReader.BEGIN_OBJECT);

      String name =
          jsonReader
              .readNext(OJSONReader.FIELD_ASSIGNMENT)
              .checkContent("\"name\"")
              .readString(OJSONReader.COMMA_SEPARATOR);

      if (name.length() == 0) {
        name = null;
      }

      name = YTClassImpl.decodeClassName(name);

      int clusterIdFromJson;
      if (exporterVersion < 9) {
        clusterIdFromJson =
            jsonReader
                .readNext(OJSONReader.FIELD_ASSIGNMENT)
                .checkContent("\"id\"")
                .readInteger(OJSONReader.COMMA_SEPARATOR);
        String type =
            jsonReader
                .readNext(OJSONReader.FIELD_ASSIGNMENT)
                .checkContent("\"type\"")
                .readString(OJSONReader.NEXT_IN_OBJECT);
      } else {
        clusterIdFromJson =
            jsonReader
                .readNext(OJSONReader.FIELD_ASSIGNMENT)
                .checkContent("\"id\"")
                .readInteger(OJSONReader.NEXT_IN_OBJECT);
      }

      String type;
      if (jsonReader.lastChar() == ',') {
        type =
            jsonReader
                .readNext(OJSONReader.FIELD_ASSIGNMENT)
                .checkContent("\"type\"")
                .readString(OJSONReader.NEXT_IN_OBJECT);
      } else {
        type = "PHYSICAL";
      }

      if (jsonReader.lastChar() == ',') {
        rid =
            new YTRecordId(
                jsonReader
                    .readNext(OJSONReader.FIELD_ASSIGNMENT)
                    .checkContent("\"rid\"")
                    .readString(OJSONReader.NEXT_IN_OBJECT));
      } else {
        rid = null;
      }

      listener.onMessage(
          "\n- Creating cluster " + (name != null ? "'" + name + "'" : "NULL") + "...");

      int createdClusterId = name != null ? database.getClusterIdByName(name) : -1;
      if (createdClusterId == -1) {
        // CREATE IT
        if (!preserveClusterIDs) {
          createdClusterId = database.addCluster(name);
        } else {
          if (getDatabase().getClusterNameById(clusterIdFromJson) == null) {
            createdClusterId = database.addCluster(name, clusterIdFromJson, null);
            assert createdClusterId == clusterIdFromJson;
          } else {
            createdClusterId = database.addCluster(name);
            listener.onMessage(
                "\n- WARNING cluster with id " + clusterIdFromJson + " already exists");
          }
        }
      }

      if (createdClusterId != clusterIdFromJson) {
        if (!preserveClusterIDs) {
          if (database.countClusterElements(createdClusterId - 1) == 0) {
            listener.onMessage("Found previous version: migrating old clusters...");
            database.dropCluster(name);
            database.addCluster("temp_" + createdClusterId, null);
            createdClusterId = database.addCluster(name);
          } else {
            throw new YTConfigurationException(
                "Imported cluster '"
                    + name
                    + "' has id="
                    + createdClusterId
                    + " different from the original: "
                    + clusterIdFromJson
                    + ". To continue the import drop the cluster '"
                    + database.getClusterNameById(createdClusterId - 1)
                    + "' that has "
                    + database.countClusterElements(createdClusterId - 1)
                    + " records");
          }
        } else {

          final YTClass clazz =
              database.getMetadata().getSchema().getClassByClusterId(createdClusterId);
          if (clazz instanceof YTClassEmbedded) {
            ((YTClassEmbedded) clazz).removeClusterId(database, createdClusterId, true);
          }

          database.dropCluster(createdClusterId);
          createdClusterId = database.addCluster(name, clusterIdFromJson, null);
        }
      }
      clusterToClusterMapping.put(clusterIdFromJson, createdClusterId);

      listener.onMessage("OK, assigned id=" + createdClusterId + ", was " + clusterIdFromJson);

      total++;

      jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);
    }
    jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);

    listener.onMessage("\nRebuilding indexes of truncated clusters ...");

    for (final String indexName : indexesToRebuild) {
      database
          .getMetadata()
          .getIndexManagerInternal()
          .getIndex(database, indexName)
          .rebuild(database,
              new OProgressListener() {
                private long last = 0;

                @Override
                public void onBegin(Object iTask, long iTotal, Object metadata) {
                  listener.onMessage(
                      "\n- Cluster content was updated: rebuilding index '" + indexName + "'...");
                }

                @Override
                public boolean onProgress(Object iTask, long iCounter, float iPercent) {
                  final long now = System.currentTimeMillis();
                  if (last == 0) {
                    last = now;
                  } else {
                    if (now - last > 1000) {
                      listener.onMessage(
                          String.format(
                              "\nIndex '%s' is rebuilding (%.2f/100)", indexName, iPercent));
                      last = now;
                    }
                  }
                  return true;
                }

                @Override
                public void onCompletition(YTDatabaseSessionInternal session, Object iTask,
                    boolean iSucceed) {
                  listener.onMessage(" Index " + indexName + " was successfully rebuilt.");
                }
              });
    }
    listener.onMessage("\nDone " + indexesToRebuild.size() + " indexes were rebuilt.");

    if (recreateManualIndex) {
      database.addCluster(OMetadataDefault.CLUSTER_MANUAL_INDEX_NAME);
      database.getMetadata().getIndexManagerInternal().create();

      listener.onMessage("\nManual index cluster was recreated.");
    }
    listener.onMessage("\nDone. Imported " + total + " clusters");

    database.begin();
    if (!database.exists(
        new YTRecordId(database.getStorageInfo().getConfiguration().getIndexMgrRecordId()))) {
      EntityImpl indexDocument = new EntityImpl();
      indexDocument.save(OMetadataDefault.CLUSTER_INTERNAL_NAME);
      database.getStorage().setIndexMgrRecordId(indexDocument.getIdentity().toString());
    }
    database.commit();

    return total;
  }

  /**
   * From `exporterVersion` >= `13`, `fromStream()` will be used. However, the import is still of
   * type String, and thus has to be converted to InputStream, which can only be avoided by
   * introducing a new interface method.
   */
  private YTRID importRecord(HashSet<YTRID> recordsBeforeImport,
      YTSchema beforeImportSchemaSnapshot)
      throws Exception {
    OPair<String, Map<String, ORidSet>> recordParse =
        jsonReader.readRecordString(this.maxRidbagStringSizeBeforeLazyImport);
    String value = recordParse.getKey().trim();

    if (value.isEmpty()) {
      return null;
    }

    // JUMP EMPTY RECORDS
    while (!value.isEmpty() && value.charAt(0) != '{') {
      value = value.substring(1);
    }

    RecordAbstract record = null;

    // big ridbags (ie. supernodes) sometimes send the system OOM, so they have to be discarded at
    // this stage
    // and processed later. The following collects the positions ("value" inside the string) of
    // skipped fields.
    IntOpenHashSet skippedPartsIndexes = new IntOpenHashSet();

    try {
      try {
        record =
            ORecordSerializerJSON.INSTANCE.fromString(database,
                value,
                null,
                null,
                null,
                false,
                maxRidbagStringSizeBeforeLazyImport, skippedPartsIndexes);
      } catch (final YTSerializationException e) {
        if (e.getCause() instanceof YTSchemaException) {
          // EXTRACT CLASS NAME If ANY
          final int pos = value.indexOf("\"@class\":\"");
          if (pos > -1) {
            final int end = value.indexOf('"', pos + "\"@class\":\"".length() + 1);
            final String value1 = value.substring(0, pos + "\"@class\":\"".length());
            final String clsName = value.substring(pos + "\"@class\":\"".length(), end);
            final String value2 = value.substring(end);

            final String newClassName = convertedClassNames.get(clsName);

            value = value1 + newClassName + value2;
            // OVERWRITE CLASS NAME WITH NEW NAME
            record =
                ORecordSerializerJSON.INSTANCE.fromString(database,
                    value,
                    record,
                    null,
                    null,
                    false,
                    maxRidbagStringSizeBeforeLazyImport, skippedPartsIndexes);
          }
        } else {
          throw YTException.wrapException(
              new YTDatabaseImportException("Error on importing record"), e);
        }
      }

      // Incorrect record format, skip this record
      if (record == null || record.getIdentity() == null) {
        LogManager.instance().warn(this, "Broken record was detected and will be skipped");
        return null;
      }

      if (schemaImported && record.getIdentity().equals(schemaRecordId)) {
        recordsBeforeImport.remove(record.getIdentity());
        // JUMP THE SCHEMA
        return null;
      }

      // CHECK IF THE CLUSTER IS INCLUDED

      if (record.getIdentity().getClusterId() == 0
          && record.getIdentity().getClusterPosition() == 1) {
        recordsBeforeImport.remove(record.getIdentity());
        // JUMP INTERNAL RECORDS
        return null;
      }

      if (exporterVersion >= 3) {
        int oridsId = database.getClusterIdByName("ORIDs");
        int indexId = database.getClusterIdByName(OMetadataDefault.CLUSTER_INDEX_NAME);

        if (record.getIdentity().getClusterId() == indexId
            || record.getIdentity().getClusterId() == oridsId) {
          recordsBeforeImport.remove(record.getIdentity());
          // JUMP INDEX RECORDS
          return null;
        }
      }

      final int manualIndexCluster =
          database.getClusterIdByName(OMetadataDefault.CLUSTER_MANUAL_INDEX_NAME);
      final int internalCluster =
          database.getClusterIdByName(OMetadataDefault.CLUSTER_INTERNAL_NAME);
      final int indexCluster = database.getClusterIdByName(OMetadataDefault.CLUSTER_INDEX_NAME);

      if (exporterVersion >= 4) {
        if (record.getIdentity().getClusterId() == manualIndexCluster) {
          // JUMP INDEX RECORDS
          recordsBeforeImport.remove(record.getIdentity());
          return null;
        }
      }

      if (record.getIdentity().equals(indexMgrRecordId)) {
        recordsBeforeImport.remove(record.getIdentity());
        return null;
      }

      final YTRID rid = record.getIdentity().copy();
      final int clusterId = rid.getClusterId();

      Entity systemRecord = null;
      var cls = beforeImportSchemaSnapshot.getClassByClusterId(clusterId);
      if (cls != null) {
        assert record instanceof EntityImpl;

        if (cls.getName().equals(YTUser.CLASS_NAME)) {
          try (var resultSet =
              database.query(
                  "select from " + YTUser.CLASS_NAME + " where name = ?",
                  ((EntityImpl) record).<String>getProperty("name"))) {
            if (resultSet.hasNext()) {
              systemRecord = resultSet.next().toEntity();
            }
          }
        } else if (cls.getName().equals(ORole.CLASS_NAME)) {
          try (var resultSet =
              database.query(
                  "select from " + ORole.CLASS_NAME + " where name = ?",
                  ((EntityImpl) record).<String>getProperty("name"))) {
            if (resultSet.hasNext()) {
              systemRecord = resultSet.next().toEntity();
            }
          }
        } else if (cls.getName().equals(OSecurityPolicy.class.getSimpleName())) {
          try (var resultSet =
              database.query(
                  "select from " + OSecurityPolicy.class.getSimpleName() + " where name = ?",
                  ((EntityImpl) record).<String>getProperty("name"))) {
            if (resultSet.hasNext()) {
              systemRecord = resultSet.next().toEntity();
            }
          }
        } else if (cls.getName().equals("V") || cls.getName().equals("E")) {
          // skip it
        } else {
          throw new IllegalStateException("Class " + cls.getName() + " is not supported.");
        }
      }

      if ((clusterId != manualIndexCluster
          && clusterId != internalCluster
          && clusterId != indexCluster)) {
        if (systemRecord != null) {
          if (!record.getClass().isAssignableFrom(systemRecord.getClass())) {
            throw new IllegalStateException(
                "Imported record and record stored in database under id "
                    + rid
                    + " have different types. "
                    + "Stored record class is : "
                    + record.getClass()
                    + " and imported "
                    + systemRecord.getClass()
                    + " .");
          }

          ORecordInternal.setVersion(record, systemRecord.getVersion());
          ORecordInternal.setIdentity(record, (YTRecordId) systemRecord.getIdentity());
          recordsBeforeImport.remove(systemRecord.getIdentity());
        } else {
          ORecordInternal.setVersion(record, 0);
          ORecordInternal.setIdentity(record, new ChangeableRecordId());
        }
        record.setDirty();

        var recordToSave = record;
        database.executeInTx(
            () -> recordToSave.save(database.getClusterNameById(clusterId)));
        if (!rid.equals(record.getIdentity())) {
          // SAVE IT ONLY IF DIFFERENT
          var recordRid = record.getIdentity();
          database.executeInTx(
              () ->
                  new EntityImpl(EXPORT_IMPORT_CLASS_NAME)
                      .field("key", rid.toString())
                      .field("value", recordRid.toString())
                      .save());
        }
      }

      // import skipped records (too big to be imported before)
      if (!skippedPartsIndexes.isEmpty()) {
        for (Integer skippedPartsIndex : skippedPartsIndexes) {
          importSkippedRidbag(record, value, skippedPartsIndex);
        }
      }

      if (!recordParse.value.isEmpty()) {
        importSkippedRidbag(record, recordParse.getValue());
      }

    } catch (Exception t) {
      if (record != null) {
        LogManager.instance()
            .error(
                this,
                "Error importing record "
                    + record.getIdentity()
                    + ". Source line "
                    + jsonReader.getLineNumber()
                    + ", column "
                    + jsonReader.getColumnNumber(),
                t);
      } else {
        LogManager.instance()
            .error(
                this,
                "Error importing record. Source line "
                    + jsonReader.getLineNumber()
                    + ", column "
                    + jsonReader.getColumnNumber(),
                t);
      }

      if (!(t instanceof YTDatabaseException)) {
        throw t;
      }
    }

    return record.getIdentity();
  }

  private long importRecords(YTSchema beforeImportSchemaSnapshot) throws Exception {
    long total = 0;

    final YTSchema schema = database.getMetadata().getSchema();
    if (schema.getClass(EXPORT_IMPORT_CLASS_NAME) != null) {
      schema.dropClass(EXPORT_IMPORT_CLASS_NAME);
    }
    final YTClass cls = schema.createClass(EXPORT_IMPORT_CLASS_NAME);
    cls.createProperty(database, "key", YTType.STRING);
    cls.createProperty(database, "value", YTType.STRING);
    cls.createIndex(database, EXPORT_IMPORT_INDEX_NAME, YTClass.INDEX_TYPE.DICTIONARY, "key");

    jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

    long totalRecords = 0;

    listener.onMessage("\n\nImporting records...");

    // the only security records are left at this moment so we need to overwrite them
    // and then remove left overs
    final HashSet<YTRID> recordsBeforeImport = new HashSet<>();

    for (final String clusterName : database.getClusterNames()) {
      final Iterator<Record> recordIterator = database.browseCluster(clusterName);
      while (recordIterator.hasNext()) {
        recordsBeforeImport.add(recordIterator.next().getIdentity());
      }
    }

    // excluding placeholder record that exist for binary compatibility
    recordsBeforeImport.remove(new YTRecordId(0, 0));

    YTRID rid;
    YTRID lastRid = new ChangeableRecordId();
    final long begin = System.currentTimeMillis();
    long lastLapRecords = 0;
    long last = begin;
    Set<String> involvedClusters = new HashSet<>();

    LogManager.instance().debug(this, "Detected exporter version " + exporterVersion + ".");
    while (jsonReader.lastChar() != ']') {
      // TODO: add special handling for `exporterVersion` / `ODatabaseExport.EXPORTER_VERSION` >= 13
      rid = importRecord(recordsBeforeImport, beforeImportSchemaSnapshot);

      total++;
      if (rid != null) {
        ++lastLapRecords;
        ++totalRecords;

        if (rid.getClusterId() != lastRid.getClusterId() || involvedClusters.isEmpty()) {
          involvedClusters.add(database.getClusterNameById(rid.getClusterId()));
        }
        lastRid = rid;
      }

      final long now = System.currentTimeMillis();
      if (now - last > IMPORT_RECORD_DUMP_LAP_EVERY_MS) {
        final List<String> sortedClusters = new ArrayList<>(involvedClusters);
        Collections.sort(sortedClusters);

        listener.onMessage(
            String.format(
                "\n"
                    + "- Imported %,d records into clusters: %s. Total JSON records imported so for"
                    + " %,d .Total records imported so far: %,d (%,.2f/sec)",
                lastLapRecords,
                total,
                sortedClusters.size(),
                totalRecords,
                (float) lastLapRecords * 1000 / (float) IMPORT_RECORD_DUMP_LAP_EVERY_MS));

        // RESET LAP COUNTERS
        last = now;
        lastLapRecords = 0;
        involvedClusters.clear();
      }
    }

    // remove all records which were absent in new database but
    // exist in old database
    for (final YTRID leftOverRid : recordsBeforeImport) {
      database.executeInTx(() -> database.delete(leftOverRid));
    }

    database.getMetadata().reload();

    final Set<YTRID> brokenRids = new HashSet<>();
    processBrokenRids(brokenRids);

    listener.onMessage(
        String.format(
            "\n\nDone. Imported %,d records in %,.2f secs\n",
            totalRecords, ((float) (System.currentTimeMillis() - begin)) / 1000));

    jsonReader.readNext(OJSONReader.COMMA_SEPARATOR);

    return total;
  }

  private void importSkippedRidbag(final Record record, final Map<String, ORidSet> bags) {
    if (bags == null) {
      return;
    }
    Entity doc = (Entity) record;
    bags.forEach(
        (field, ridset) -> {
          RidBag ridbag = ((EntityInternal) record).getPropertyInternal(field);
          ridset.forEach(
              rid -> {
                ridbag.add(rid);
                doc.save();
              });
        });
  }

  private void importSkippedRidbag(Record record, String value, Integer skippedPartsIndex) {
    var doc = (EntityInternal) record;

    StringBuilder builder = new StringBuilder();

    int nextIndex =
        OStringSerializerHelper.parse(
            value,
            builder,
            skippedPartsIndex,
            -1,
            ORecordSerializerJSON.PARAMETER_SEPARATOR,
            true,
            true,
            false,
            -1,
            false,
            ' ',
            '\n',
            '\r',
            '\t');

    String fieldName = OIOUtils.getStringContent(builder.toString());
    RidBag bag = doc.getPropertyInternal(fieldName);

    if (!(value.charAt(nextIndex) == '[')) {
      throw new YTDatabaseImportException("Cannot import field: " + fieldName + " (too big)");
    }

    StringBuilder ridBuffer = new StringBuilder();

    for (int i = nextIndex + 1; i < value.length() + 2; i++) {
      if (value.charAt(i) == ',' || value.charAt(i) == ']') {
        String ridString = OIOUtils.getStringContent(ridBuffer.toString().trim());
        if (ridString.length() > 0) {
          YTRecordId rid = new YTRecordId(ridString);
          bag.add(rid);
          record.save();
        }
        ridBuffer = new StringBuilder();
        if (value.charAt(i) == ']') {
          break;
        }
      } else {
        ridBuffer.append(value.charAt(i));
      }
    }
  }

  private void importIndexes() throws IOException, ParseException {
    listener.onMessage("\n\nImporting indexes ...");

    OIndexManagerAbstract indexManager = database.getMetadata().getIndexManagerInternal();
    indexManager.reload(database);

    jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

    int numberOfCreatedIndexes = 0;
    while (jsonReader.lastChar() != ']') {
      jsonReader.readNext(OJSONReader.NEXT_OBJ_IN_ARRAY);
      if (jsonReader.lastChar() == ']') {
        break;
      }

      String blueprintsIndexClass = null;
      String indexName = null;
      String indexType = null;
      String indexAlgorithm = null;
      Set<String> clustersToIndex = new HashSet<>();
      OIndexDefinition indexDefinition = null;
      EntityImpl metadata = null;
      Map<String, String> engineProperties = null;

      while (jsonReader.lastChar() != '}') {
        final String fieldName = jsonReader.readString(OJSONReader.FIELD_ASSIGNMENT);
        if (fieldName.equals("name")) {
          indexName = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
        } else {
          if (fieldName.equals("type")) {
            indexType = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
          } else {
            if (fieldName.equals("algorithm")) {
              indexAlgorithm = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
            } else {
              if (fieldName.equals("clustersToIndex")) {
                clustersToIndex = importClustersToIndex();
              } else {
                if (fieldName.equals("definition")) {
                  indexDefinition = importIndexDefinition();
                  jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);
                } else {
                  if (fieldName.equals("metadata")) {
                    final String jsonMetadata = jsonReader.readString(OJSONReader.END_OBJECT, true);
                    metadata = new EntityImpl();
                    metadata.fromJSON(jsonMetadata);
                    jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);
                  } else {
                    if (fieldName.equals("engineProperties")) {
                      final String jsonEngineProperties =
                          jsonReader.readString(OJSONReader.END_OBJECT, true);
                      var doc = new EntityImpl();
                      doc.fromJSON(jsonEngineProperties);
                      Map<String, ?> map = doc.toMap();
                      if (map != null) {
                        map.replaceAll((k, v) -> v);
                      }
                      jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);
                    } else {
                      if (fieldName.equals("blueprintsIndexClass")) {
                        blueprintsIndexClass = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      jsonReader.readNext(OJSONReader.NEXT_IN_ARRAY);

      numberOfCreatedIndexes =
          dropAutoCreatedIndexesAndCountCreatedIndexes(
              indexManager,
              numberOfCreatedIndexes,
              blueprintsIndexClass,
              indexName,
              indexType,
              indexAlgorithm,
              clustersToIndex,
              indexDefinition,
              metadata);
    }
    listener.onMessage("\nDone. Created " + numberOfCreatedIndexes + " indexes.");
    jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);
  }

  private int dropAutoCreatedIndexesAndCountCreatedIndexes(
      final OIndexManagerAbstract indexManager,
      int numberOfCreatedIndexes,
      final String blueprintsIndexClass,
      final String indexName,
      final String indexType,
      final String indexAlgorithm,
      final Set<String> clustersToIndex,
      OIndexDefinition indexDefinition,
      final EntityImpl metadata) {
    if (indexName == null) {
      throw new IllegalArgumentException("Index name is missing");
    }

    // drop automatically created indexes
    if (!indexName.equalsIgnoreCase(EXPORT_IMPORT_INDEX_NAME)) {
      listener.onMessage("\n- Index '" + indexName + "'...");

      indexManager.dropIndex(database, indexName);
      indexesToRebuild.remove(indexName);
      IntArrayList clusterIds = new IntArrayList();

      for (final String clusterName : clustersToIndex) {
        int id = database.getClusterIdByName(clusterName);
        if (id != -1) {
          clusterIds.add(id);
        } else {
          listener.onMessage(
              String.format(
                  "found not existent cluster '%s' in index '%s' configuration, skipping",
                  clusterName, indexName));
        }
      }
      int[] clusterIdsToIndex = new int[clusterIds.size()];

      int i = 0;
      for (var n = 0; n < clusterIds.size(); n++) {
        int clusterId = clusterIds.getInt(n);
        clusterIdsToIndex[i] = clusterId;
        i++;
      }

      if (indexDefinition == null) {
        indexDefinition = new OSimpleKeyIndexDefinition(YTType.STRING);
      }

      boolean oldValue = GlobalConfiguration.INDEX_IGNORE_NULL_VALUES_DEFAULT.getValueAsBoolean();
      GlobalConfiguration.INDEX_IGNORE_NULL_VALUES_DEFAULT.setValue(
          indexDefinition.isNullValuesIgnored());
      final OIndex index =
          indexManager.createIndex(
              database,
              indexName,
              indexType,
              indexDefinition,
              clusterIdsToIndex,
              null,
              metadata,
              indexAlgorithm);
      GlobalConfiguration.INDEX_IGNORE_NULL_VALUES_DEFAULT.setValue(oldValue);
      if (blueprintsIndexClass != null) {
        EntityImpl configuration = index.getConfiguration(database);
        configuration.field("blueprintsIndexClass", blueprintsIndexClass);
        indexManager.save(database);
      }
      numberOfCreatedIndexes++;
      listener.onMessage("OK");
    }
    return numberOfCreatedIndexes;
  }

  private Set<String> importClustersToIndex() throws IOException, ParseException {
    final Set<String> clustersToIndex = new HashSet<>();

    jsonReader.readNext(OJSONReader.BEGIN_COLLECTION);

    while (jsonReader.lastChar() != ']') {
      final String clusterToIndex = jsonReader.readString(OJSONReader.NEXT_IN_ARRAY);
      clustersToIndex.add(clusterToIndex);
    }

    jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);
    return clustersToIndex;
  }

  private OIndexDefinition importIndexDefinition() throws IOException, ParseException {
    jsonReader.readString(OJSONReader.BEGIN_OBJECT);
    jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT);

    final String className = jsonReader.readString(OJSONReader.NEXT_IN_OBJECT);

    jsonReader.readNext(OJSONReader.FIELD_ASSIGNMENT);

    final String value = jsonReader.readString(OJSONReader.END_OBJECT, true);

    final OIndexDefinition indexDefinition;
    final EntityImpl indexDefinitionDoc =
        (EntityImpl) ORecordSerializerJSON.INSTANCE.fromString(database, value, null, null);
    try {
      final Class<?> indexDefClass = Class.forName(className);
      indexDefinition = (OIndexDefinition) indexDefClass.getDeclaredConstructor().newInstance();
      indexDefinition.fromStream(indexDefinitionDoc);
    } catch (final ClassNotFoundException e) {
      throw new IOException("Error during deserialization of index definition", e);
    } catch (final NoSuchMethodException e) {
      throw new IOException("Error during deserialization of index definition", e);
    } catch (final InvocationTargetException e) {
      throw new IOException("Error during deserialization of index definition", e);
    } catch (final InstantiationException e) {
      throw new IOException("Error during deserialization of index definition", e);
    } catch (final IllegalAccessException e) {
      throw new IOException("Error during deserialization of index definition", e);
    }

    jsonReader.readNext(OJSONReader.NEXT_IN_OBJECT);

    return indexDefinition;
  }

  private void migrateLinksInImportedDocuments(Set<YTRID> brokenRids) throws IOException {
    listener.onMessage(
        "\n\n"
            + "Started migration of links (-migrateLinks=true). Links are going to be updated"
            + " according to new RIDs:");

    final long begin = System.currentTimeMillis();
    final long[] last = new long[]{begin};
    final long[] documentsLastLap = new long[1];

    long[] totalDocuments = new long[1];
    Collection<String> clusterNames = database.getClusterNames();
    for (String clusterName : clusterNames) {
      if (OMetadataDefault.CLUSTER_INDEX_NAME.equals(clusterName)
          || OMetadataDefault.CLUSTER_INTERNAL_NAME.equals(clusterName)
          || OMetadataDefault.CLUSTER_MANUAL_INDEX_NAME.equals(clusterName)) {
        continue;
      }

      final long[] documents = new long[1];
      final String[] prefix = new String[]{""};

      listener.onMessage("\n- Cluster " + clusterName + "...");

      final int clusterId = database.getClusterIdByName(clusterName);
      final long clusterRecords = database.countClusterElements(clusterId);
      Storage storage = database.getStorage();

      OPhysicalPosition[] positions =
          storage.ceilingPhysicalPositions(database, clusterId, new OPhysicalPosition(0));
      while (positions.length > 0) {
        for (OPhysicalPosition position : positions) {
          database.executeInTx(() -> {
            Record record = database.load(new YTRecordId(clusterId, position.clusterPosition));
            if (record instanceof EntityImpl document) {
              rewriteLinksInDocument(database, document, brokenRids);

              documents[0]++;
              documentsLastLap[0]++;
              totalDocuments[0]++;

              final long now = System.currentTimeMillis();
              if (now - last[0] > IMPORT_RECORD_DUMP_LAP_EVERY_MS) {
                listener.onMessage(
                    String.format(
                        "\n--- Migrated %,d of %,d records (%,.2f/sec)",
                        documents[0],
                        clusterRecords,
                        (float) documentsLastLap[0] * 1000
                            / (float) IMPORT_RECORD_DUMP_LAP_EVERY_MS));

                // RESET LAP COUNTERS
                last[0] = now;
                documentsLastLap[0] = 0;
                prefix[0] = "\n---";
              }
            }
          });
        }

        positions = storage.higherPhysicalPositions(database, clusterId,
            positions[positions.length - 1]);
      }

      listener.onMessage(
          String.format(
              "%s Completed migration of %,d records in current cluster", prefix[0], documents[0]));
    }

    listener.onMessage(String.format("\nTotal links updated: %,d", totalDocuments[0]));
  }

  protected static void rewriteLinksInDocument(
      YTDatabaseSessionInternal session, EntityImpl document, Set<YTRID> brokenRids) {
    var doc = doRewriteLinksInDocument(session, document, brokenRids);

    if (!doc.isDirty()) {
      // nothing changed
      return;
    }

    session.executeInTx(doc::save);
  }

  protected static EntityImpl doRewriteLinksInDocument(
      YTDatabaseSessionInternal session, EntityImpl document, Set<YTRID> brokenRids) {
    final OLinksRewriter rewriter = new OLinksRewriter(new OConverterData(session, brokenRids));
    final ODocumentFieldWalker documentFieldWalker = new ODocumentFieldWalker();
    return documentFieldWalker.walkDocument(session, document, rewriter);
  }

  public int getMaxRidbagStringSizeBeforeLazyImport() {
    return maxRidbagStringSizeBeforeLazyImport;
  }

  public void setMaxRidbagStringSizeBeforeLazyImport(int maxRidbagStringSizeBeforeLazyImport) {
    this.maxRidbagStringSizeBeforeLazyImport = maxRidbagStringSizeBeforeLazyImport;
  }
}