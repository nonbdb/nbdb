package com.orientechnologies.lucene.functions;

import static com.orientechnologies.lucene.functions.OLuceneFunctionsUtils.getOrCreateMemoryIndex;

import com.orientechnologies.lucene.builder.OLuceneQueryBuilder;
import com.orientechnologies.lucene.collections.OLuceneCompositeKey;
import com.orientechnologies.lucene.index.OLuceneFullTextIndex;
import com.orientechnologies.lucene.query.OLuceneKeyAndMetadata;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.metadata.OMetadataInternal;
import com.orientechnologies.orient.core.record.YTEntity;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import com.orientechnologies.orient.core.sql.parser.OBinaryCompareOperator;
import com.orientechnologies.orient.core.sql.parser.OExpression;
import com.orientechnologies.orient.core.sql.parser.OFromClause;
import com.orientechnologies.orient.core.sql.parser.OFromItem;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.memory.MemoryIndex;

/**
 *
 */
public class OLuceneSearchOnClassFunction extends OLuceneSearchFunctionTemplate {

  public static final String NAME = "search_class";

  public OLuceneSearchOnClassFunction() {
    super(NAME, 1, 2);
  }

  @Override
  public String getName(YTDatabaseSession session) {
    return NAME;
  }

  @Override
  public boolean canExecuteInline(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      OCommandContext ctx,
      OExpression... args) {
    return true;
  }

  @Override
  public Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] params,
      OCommandContext ctx) {

    YTResult result;
    if (iThis instanceof YTResult) {
      result = (YTResult) iThis;
    } else {
      result = new YTResultInternal(ctx.getDatabase(), (YTIdentifiable) iThis);
    }

    YTEntity element = result.toEntity();

    String className = element.getSchemaType().get().getName();

    OLuceneFullTextIndex index = searchForIndex(ctx, className);

    if (index == null) {
      return false;
    }

    String query = (String) params[0];

    MemoryIndex memoryIndex = getOrCreateMemoryIndex(ctx);

    List<Object> key =
        index.getDefinition().getFields().stream()
            .map(s -> element.getProperty(s))
            .collect(Collectors.toList());

    for (IndexableField field : index.buildDocument(ctx.getDatabase(), key).getFields()) {
      memoryIndex.addField(field, index.indexAnalyzer());
    }

    var metadata = getMetadata(params);
    OLuceneKeyAndMetadata keyAndMetadata =
        new OLuceneKeyAndMetadata(
            new OLuceneCompositeKey(Collections.singletonList(query)).setContext(ctx), metadata);

    return memoryIndex.search(index.buildQuery(keyAndMetadata)) > 0.0f;
  }

  private Map<String, ?> getMetadata(Object[] params) {

    if (params.length == 2) {
      return (Map<String, ?>) params[1];
    }

    return Collections.emptyMap();
  }

  @Override
  public String getSyntax(YTDatabaseSession session) {
    return "SEARCH_INDEX( indexName, [ metdatada {} ] )";
  }

  @Override
  public boolean filterResult() {
    return true;
  }

  @Override
  public Iterable<YTIdentifiable> searchFromTarget(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      OCommandContext ctx,
      OExpression... args) {

    OLuceneFullTextIndex index = searchForIndex(target, ctx);

    OExpression expression = args[0];
    String query = (String) expression.execute((YTIdentifiable) null, ctx);

    if (index != null) {

      var metadata = getMetadata(args, ctx);

      List<YTIdentifiable> luceneResultSet;
      try (Stream<YTRID> rids =
          index
              .getInternal()
              .getRids(ctx.getDatabase(),
                  new OLuceneKeyAndMetadata(
                      new OLuceneCompositeKey(Collections.singletonList(query)).setContext(ctx),
                      metadata))) {
        luceneResultSet = rids.collect(Collectors.toList());
      }

      return luceneResultSet;
    }
    return Collections.emptySet();
  }

  private Map<String, ?> getMetadata(OExpression[] args, OCommandContext ctx) {
    if (args.length == 2) {
      return getMetadata(args[1], ctx);
    }
    return OLuceneQueryBuilder.EMPTY_METADATA;
  }

  @Override
  protected OLuceneFullTextIndex searchForIndex(
      OFromClause target, OCommandContext ctx, OExpression... args) {
    OFromItem item = target.getItem();

    String className = item.getIdentifier().getStringValue();

    return searchForIndex(ctx, className);
  }

  private static OLuceneFullTextIndex searchForIndex(OCommandContext ctx, String className) {
    var db = ctx.getDatabase();
    db.activateOnCurrentThread();
    OMetadataInternal dbMetadata = db.getMetadata();

    List<OLuceneFullTextIndex> indices =
        dbMetadata.getImmutableSchemaSnapshot().getClass(className).getIndexes(db).stream()
            .filter(idx -> idx instanceof OLuceneFullTextIndex)
            .map(idx -> (OLuceneFullTextIndex) idx)
            .toList();

    if (indices.size() > 1) {
      throw new IllegalArgumentException("too many full-text indices on given class: " + className);
    }

    return indices.isEmpty() ? null : indices.get(0);
  }
}
