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

package com.orientechnologies.lucene.engine;

import static com.orientechnologies.lucene.builder.OLuceneQueryBuilder.EMPTY_METADATA;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.lucene.builder.OLuceneDocumentBuilder;
import com.orientechnologies.lucene.builder.OLuceneIndexType;
import com.orientechnologies.lucene.builder.OLuceneQueryBuilder;
import com.orientechnologies.lucene.collections.LuceneIndexTransformer;
import com.orientechnologies.lucene.collections.OLuceneCompositeKey;
import com.orientechnologies.lucene.collections.OLuceneResultSet;
import com.orientechnologies.lucene.query.OLuceneKeyAndMetadata;
import com.orientechnologies.lucene.query.OLuceneQueryContext;
import com.orientechnologies.lucene.tx.OLuceneTxChanges;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.id.YTContextualRecordId;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndexEngineException;
import com.orientechnologies.orient.core.index.OIndexKeyUpdater;
import com.orientechnologies.orient.core.index.OIndexMetadata;
import com.orientechnologies.orient.core.index.engine.IndexEngineValidator;
import com.orientechnologies.orient.core.index.engine.IndexEngineValuesTransformer;
import com.orientechnologies.orient.core.sql.parser.ParseException;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.store.Directory;

public class OLuceneFullTextIndexEngine extends OLuceneIndexEngineAbstract {

  private final OLuceneDocumentBuilder builder;
  private OLuceneQueryBuilder queryBuilder;
  private final AtomicLong bonsayFileId = new AtomicLong(0);

  public OLuceneFullTextIndexEngine(OStorage storage, String idxName, int id) {
    super(id, storage, idxName);
    builder = new OLuceneDocumentBuilder();
  }

  @Override
  public void init(OIndexMetadata im) {
    super.init(im);
    queryBuilder = new OLuceneQueryBuilder(im.getMetadata());
  }

  @Override
  public IndexWriter createIndexWriter(Directory directory) throws IOException {

    OLuceneIndexWriterFactory fc = new OLuceneIndexWriterFactory();

    OLogManager.instance().debug(this, "Creating Lucene index in '%s'...", directory);

    return fc.createIndexWriter(directory, metadata, indexAnalyzer());
  }

  @Override
  public void onRecordAddedToResultSet(
      final OLuceneQueryContext queryContext,
      final YTContextualRecordId recordId,
      final Document ret,
      final ScoreDoc score) {
    HashMap<String, Object> data = new HashMap<String, Object>();

    final Map<String, TextFragment[]> frag = queryContext.getFragments();
    frag.forEach(
        (key, fragments) -> {
          final StringBuilder hlField = new StringBuilder();
          for (final TextFragment fragment : fragments) {
            if ((fragment != null) && (fragment.getScore() > 0)) {
              hlField.append(fragment);
            }
          }
          data.put("$" + key + "_hl", hlField.toString());
        });
    data.put("$score", score.score);

    recordId.setContext(data);
  }

  @Override
  public boolean remove(final OAtomicOperation atomicOperation, final Object key) {
    return remove(key);
  }

  @Override
  public Object get(YTDatabaseSessionInternal session, final Object key) {
    return getInTx(session, key, null);
  }

  @Override
  public void update(
      YTDatabaseSessionInternal session, final OAtomicOperation atomicOperation,
      final Object key,
      final OIndexKeyUpdater<Object> updater) {
    put(session, atomicOperation, key, updater.update(null, bonsayFileId).getValue());
  }

  @Override
  public void put(YTDatabaseSessionInternal session, final OAtomicOperation atomicOperation,
      final Object key, final Object value) {
    updateLastAccess();
    openIfClosed();
    final Document doc = buildDocument(session, key, (YTIdentifiable) value);
    addDocument(doc);
  }

  @Override
  public boolean validatedPut(
      OAtomicOperation atomicOperation,
      Object key,
      YTRID value,
      IndexEngineValidator<Object, YTRID> validator) {
    throw new UnsupportedOperationException(
        "Validated put is not supported by OLuceneFullTextIndexEngine");
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesBetween(
      YTDatabaseSessionInternal session, Object rangeFrom,
      boolean fromInclusive,
      Object rangeTo,
      boolean toInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return LuceneIndexTransformer.transformToStream((OLuceneResultSet) get(session, rangeFrom),
        rangeFrom);
  }

  private Set<YTIdentifiable> getResults(
      final Query query,
      final OCommandContext context,
      final OLuceneTxChanges changes,
      final Map<String, ?> metadata) {
    // sort
    final List<SortField> fields = OLuceneIndexEngineUtils.buildSortFields(metadata);
    final IndexSearcher luceneSearcher = searcher();
    final OLuceneQueryContext queryContext =
        new OLuceneQueryContext(context, luceneSearcher, query, fields).withChanges(changes);
    return new OLuceneResultSet(this, queryContext, metadata);
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMajor(
      Object fromKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return null;
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMinor(
      Object toKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return null;
  }

  @Override
  public boolean hasRangeQuerySupport() {
    return false;
  }

  @Override
  public void updateUniqueIndexVersion(Object key) {
    // not implemented
  }

  @Override
  public int getUniqueIndexVersion(Object key) {
    return 0; // not implemented
  }

  @Override
  public Document buildDocument(YTDatabaseSessionInternal session, Object key,
      YTIdentifiable value) {
    if (indexDefinition.isAutomatic()) {
      //      builder.newBuild(index, key, value);

      return builder.build(indexDefinition, key, value, collectionFields, metadata);
    } else {
      return putInManualindex(key, value);
    }
  }

  private static Document putInManualindex(Object key, YTIdentifiable oIdentifiable) {
    Document doc = new Document();
    doc.add(OLuceneIndexType.createOldIdField(oIdentifiable));
    doc.add(OLuceneIndexType.createIdField(oIdentifiable, key));

    if (key instanceof OCompositeKey) {

      List<Object> keys = ((OCompositeKey) key).getKeys();

      int k = 0;
      for (Object o : keys) {
        doc.add(OLuceneIndexType.createField("k" + k, o, Field.Store.YES));
        k++;
      }
    } else if (key instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<Object> keys = (Collection<Object>) key;

      int k = 0;
      for (Object o : keys) {
        doc.add(OLuceneIndexType.createField("k" + k, o, Field.Store.YES));
        k++;
      }
    } else {
      doc.add(OLuceneIndexType.createField("k0", key, Field.Store.NO));
    }
    return doc;
  }

  @Override
  public Query buildQuery(final Object maybeQuery) {
    try {
      if (maybeQuery instanceof String) {
        return queryBuilder.query(indexDefinition, maybeQuery, EMPTY_METADATA,
            queryAnalyzer());
      } else {
        OLuceneKeyAndMetadata q = (OLuceneKeyAndMetadata) maybeQuery;
        return queryBuilder.query(indexDefinition, q.key, q.metadata, queryAnalyzer());
      }
    } catch (final ParseException e) {
      throw OException.wrapException(new OIndexEngineException("Error parsing query"), e);
    }
  }

  @Override
  public Set<YTIdentifiable> getInTx(YTDatabaseSessionInternal session, Object key,
      OLuceneTxChanges changes) {
    updateLastAccess();
    openIfClosed();
    try {
      if (key instanceof OLuceneKeyAndMetadata q) {
        Query query = queryBuilder.query(indexDefinition, q.key, q.metadata, queryAnalyzer());

        OCommandContext commandContext = q.key.getContext();
        return getResults(query, commandContext, changes, q.metadata);

      } else {
        Query query = queryBuilder.query(indexDefinition, key, EMPTY_METADATA,
            queryAnalyzer());

        OCommandContext commandContext = null;
        if (key instanceof OLuceneCompositeKey) {
          commandContext = ((OLuceneCompositeKey) key).getContext();
        }
        return getResults(query, commandContext, changes, EMPTY_METADATA);
      }
    } catch (ParseException e) {
      throw OException.wrapException(new OIndexEngineException("Error parsing lucene query"), e);
    }
  }
}
