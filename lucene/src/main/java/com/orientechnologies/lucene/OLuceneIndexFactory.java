/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.lucene;

import static com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass.INDEX_TYPE.FULLTEXT;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBManager;
import com.jetbrains.youtrack.db.internal.core.config.IndexEngineData;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseLifecycleListener;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.YTConfigurationException;
import com.jetbrains.youtrack.db.internal.core.index.OIndexFactory;
import com.jetbrains.youtrack.db.internal.core.index.OIndexInternal;
import com.jetbrains.youtrack.db.internal.core.index.OIndexMetadata;
import com.jetbrains.youtrack.db.internal.core.index.engine.OBaseIndexEngine;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.orientechnologies.lucene.engine.OLuceneFullTextIndexEngine;
import com.orientechnologies.lucene.index.OLuceneFullTextIndex;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class OLuceneIndexFactory implements OIndexFactory, ODatabaseLifecycleListener {

  public static final String LUCENE_ALGORITHM = "LUCENE";

  private static final Set<String> TYPES;
  private static final Set<String> ALGORITHMS;

  static {
    final Set<String> types = new HashSet<String>();
    types.add(FULLTEXT.toString());
    TYPES = Collections.unmodifiableSet(types);
  }

  static {
    final Set<String> algorithms = new HashSet<String>();
    algorithms.add(LUCENE_ALGORITHM);
    ALGORITHMS = Collections.unmodifiableSet(algorithms);
  }

  public OLuceneIndexFactory() {
    this(false);
  }

  public OLuceneIndexFactory(boolean manual) {
    if (!manual) {
      YouTrackDBManager.instance().addDbLifecycleListener(this);
    }
  }

  @Override
  public int getLastVersion(final String algorithm) {
    return 0;
  }

  @Override
  public Set<String> getTypes() {
    return TYPES;
  }

  @Override
  public Set<String> getAlgorithms() {
    return ALGORITHMS;
  }

  @Override
  public OIndexInternal createIndex(Storage storage, OIndexMetadata im)
      throws YTConfigurationException {
    Map<String, ?> metadata = im.getMetadata();
    final String indexType = im.getType();
    final String algorithm = im.getAlgorithm();

    if (metadata == null) {
      var metadataDoc = new EntityImpl();
      metadataDoc.field("analyzer", StandardAnalyzer.class.getName());
      im.setMetadata(metadataDoc);
    }

    if (FULLTEXT.toString().equalsIgnoreCase(indexType)) {
      return new OLuceneFullTextIndex(im, storage);
    }
    throw new YTConfigurationException("Unsupported type : " + algorithm);
  }

  @Override
  public OBaseIndexEngine createIndexEngine(Storage storage, IndexEngineData data) {
    return new OLuceneFullTextIndexEngine(storage, data.getName(), data.getIndexId());
  }

  @Override
  public PRIORITY getPriority() {
    return PRIORITY.REGULAR;
  }

  @Override
  public void onCreate(YTDatabaseSessionInternal db) {
    LogManager.instance().debug(this, "onCreate");
  }

  @Override
  public void onOpen(YTDatabaseSessionInternal db) {
    LogManager.instance().debug(this, "onOpen");
  }

  @Override
  public void onClose(YTDatabaseSessionInternal db) {
    LogManager.instance().debug(this, "onClose");
  }

  @Override
  public void onDrop(final YTDatabaseSessionInternal db) {
    try {
      if (db.isClosed()) {
        return;
      }

      LogManager.instance().debug(this, "Dropping Lucene indexes...");

      final YTDatabaseSessionInternal internal = db;
      internal.getMetadata().getIndexManagerInternal().getIndexes(internal).stream()
          .filter(idx -> idx.getInternal() instanceof OLuceneFullTextIndex)
          .peek(idx -> LogManager.instance().debug(this, "deleting index " + idx.getName()))
          .forEach(idx -> idx.delete(db));

    } catch (Exception e) {
      LogManager.instance().warn(this, "Error on dropping Lucene indexes", e);
    }
  }

  @Override
  public void onLocalNodeConfigurationRequest(EntityImpl iConfiguration) {
  }
}
