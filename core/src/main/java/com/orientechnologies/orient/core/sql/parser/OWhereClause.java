/* Generated By:JJTree: Do not edit this line. OWhereClause.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OCompositeIndexDefinition;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexDefinition;
import com.orientechnologies.orient.core.index.OPropertyIndexDefinition;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.metadata.OIndexCandidate;
import com.orientechnologies.orient.core.sql.executor.metadata.OIndexFinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OWhereClause extends SimpleNode {

  protected OBooleanExpression baseExpression;

  private List<OAndBlock> flattened;

  public OWhereClause(int id) {
    super(id);
  }

  public OWhereClause(OrientSql p, int id) {
    super(p, id);
  }

  public boolean matchesFilters(OIdentifiable currentRecord, OCommandContext ctx) {
    if (baseExpression == null) {
      return true;
    }
    return baseExpression.evaluate(currentRecord, ctx);
  }

  public boolean matchesFilters(OResult currentRecord, OCommandContext ctx) {
    if (baseExpression == null) {
      return true;
    }
    return baseExpression.evaluate(currentRecord, ctx);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (baseExpression == null) {
      return;
    }
    baseExpression.toString(params, builder);
  }

  public void toGenericStatement(StringBuilder builder) {
    if (baseExpression == null) {
      return;
    }
    baseExpression.toGenericStatement(builder);
  }

  /**
   * estimates how many items of this class will be returned applying this filter
   *
   * @return an estimation of the number of records of this class returned applying this filter, 0
   * if and only if sure that no records are returned
   */
  public long estimate(OClass oClass, long threshold, OCommandContext ctx) {
    var database = ctx.getDatabase();
    long count = oClass.count(database);
    if (count > 1) {
      count = count / 2;
    }
    if (count < threshold) {
      return count;
    }

    long indexesCount = 0L;
    List<OAndBlock> flattenedConditions = flatten();
    Set<OIndex> indexes = oClass.getIndexes(database);
    for (OAndBlock condition : flattenedConditions) {

      List<OBinaryCondition> indexedFunctConditions =
          condition.getIndexedFunctionConditions(oClass, ctx.getDatabase());

      long conditionEstimation = Long.MAX_VALUE;

      if (indexedFunctConditions != null) {
        for (OBinaryCondition cond : indexedFunctConditions) {
          OFromClause from = new OFromClause(-1);
          from.item = new OFromItem(-1);
          from.item.setIdentifier(new OIdentifier(oClass.getName()));
          long newCount = cond.estimateIndexed(from, ctx);
          if (newCount < conditionEstimation) {
            conditionEstimation = newCount;
          }
        }
      } else {
        Map<String, Object> conditions = getEqualityOperations(condition, ctx);

        for (OIndex index : indexes) {
          if (index.getType().equals(OClass.INDEX_TYPE.FULLTEXT.name())) {
            continue;
          }
          List<String> indexedFields = index.getDefinition().getFields();
          int nMatchingKeys = 0;
          for (String indexedField : indexedFields) {
            if (conditions.containsKey(indexedField)) {
              nMatchingKeys++;
            } else {
              break;
            }
          }
          if (nMatchingKeys > 0) {
            long newCount = estimateFromIndex(database, index, conditions, nMatchingKeys);
            if (newCount < conditionEstimation) {
              conditionEstimation = newCount;
            }
          }
        }
      }
      if (conditionEstimation > count) {
        return count;
      }
      indexesCount += conditionEstimation;
    }
    return Math.min(indexesCount, count);
  }

  private static long estimateFromIndex(
      ODatabaseSessionInternal session, OIndex index, Map<String, Object> conditions,
      int nMatchingKeys) {
    if (nMatchingKeys < 1) {
      throw new IllegalArgumentException("Cannot estimate from an index with zero keys");
    }
    OIndexDefinition definition = index.getDefinition();
    List<String> definitionFields = definition.getFields();
    Object key = null;
    if (definition instanceof OPropertyIndexDefinition) {
      key = convert(session, conditions.get(definitionFields.get(0)), definition.getTypes()[0]);
    } else if (definition instanceof OCompositeIndexDefinition) {
      key = new OCompositeKey();
      for (int i = 0; i < nMatchingKeys; i++) {
        Object keyValue =
            convert(session, conditions.get(definitionFields.get(i)), definition.getTypes()[i]);
        ((OCompositeKey) key).addKey(keyValue);
      }
    }
    if (key != null) {
      if (conditions.size() == definitionFields.size()) {
        try (Stream<ORID> rids = index.getInternal().getRids(session, key)) {
          return rids.count();
        }
      } else if (index.supportsOrderedIterations()) {
        final Spliterator<ORawPair<Object, ORID>> spliterator;

        try (Stream<ORawPair<Object, ORID>> stream =
            index.getInternal().streamEntriesBetween(session, key, true, key, true, true)) {
          spliterator = stream.spliterator();
          return spliterator.estimateSize();
        }
      }
    }
    return Long.MAX_VALUE;
  }

  private static Object convert(ODatabaseSessionInternal session, Object o, OType oType) {
    return OType.convert(session, o, oType.getDefaultJavaType());
  }

  private static Map<String, Object> getEqualityOperations(
      OAndBlock condition, OCommandContext ctx) {
    Map<String, Object> result = new HashMap<>();
    for (OBooleanExpression expression : condition.subBlocks) {
      if (expression instanceof OBinaryCondition b) {
        if (b.operator instanceof OEqualsCompareOperator) {
          if (b.left.isBaseIdentifier() && b.right.isEarlyCalculated(ctx)) {
            result.put(b.left.toString(), b.right.execute((OResult) null, ctx));
          }
        }
      }
    }
    return result;
  }

  public List<OAndBlock> flatten() {
    if (this.baseExpression == null) {
      return Collections.emptyList();
    }
    if (flattened == null) {
      flattened = this.baseExpression.flatten();
    }
    // TODO remove false conditions (contraddictions)
    return flattened;
  }

  public List<OBinaryCondition> getIndexedFunctionConditions(
      OClass iSchemaClass, ODatabaseSessionInternal database) {
    if (baseExpression == null) {
      return null;
    }
    return this.baseExpression.getIndexedFunctionConditions(iSchemaClass, database);
  }

  public boolean needsAliases(Set<String> aliases) {
    return this.baseExpression.needsAliases(aliases);
  }

  public void setBaseExpression(OBooleanExpression baseExpression) {
    this.baseExpression = baseExpression;
  }

  public OWhereClause copy() {
    OWhereClause result = new OWhereClause(-1);
    result.baseExpression = baseExpression.copy();
    result.flattened =
        Optional.ofNullable(flattened)
            .map(
                oAndBlocks -> {
                  try (Stream<OAndBlock> stream = oAndBlocks.stream()) {
                    return stream.map(OAndBlock::copy).collect(Collectors.toList());
                  }
                })
            .orElse(null);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OWhereClause that = (OWhereClause) o;

    if (!Objects.equals(baseExpression, that.baseExpression)) {
      return false;
    }
    return Objects.equals(flattened, that.flattened);
  }

  @Override
  public int hashCode() {
    int result = Optional.ofNullable(baseExpression).map(Object::hashCode).orElse(0);
    result = 31 * result + (Optional.ofNullable(flattened).map(List::hashCode).orElse(0));
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (baseExpression != null) {
      baseExpression.extractSubQueries(collector);
    }
    flattened = null;
  }

  public boolean refersToParent() {
    return baseExpression != null && baseExpression.refersToParent();
  }

  public OBooleanExpression getBaseExpression() {
    return baseExpression;
  }

  public List<OAndBlock> getFlattened() {
    return flattened;
  }

  public void setFlattened(List<OAndBlock> flattened) {
    this.flattened = flattened;
  }

  public OResult serialize(ODatabaseSessionInternal db) {
    OResultInternal result = new OResultInternal(db);
    if (baseExpression != null) {
      result.setProperty("baseExpression", baseExpression.serialize(db));
    }
    if (flattened != null) {
      try (Stream<OAndBlock> stream = flattened.stream()) {
        result.setProperty(
            "flattened",
            stream.map(oAndBlock -> oAndBlock.serialize(db)).collect(Collectors.toList()));
      }
    }
    return result;
  }

  public void deserialize(OResult fromResult) {
    if (fromResult.getProperty("baseExpression") != null) {
      baseExpression =
          OBooleanExpression.deserializeFromOResult(fromResult.getProperty("baseExpression"));
    }
    if (fromResult.getProperty("flattened") != null) {
      List<OResult> ser = fromResult.getProperty("flattened");
      flattened = new ArrayList<>();
      for (OResult r : ser) {
        OAndBlock block = new OAndBlock(-1);
        block.deserialize(r);
        flattened.add(block);
      }
    }
  }

  public boolean isCacheable(ODatabaseSessionInternal session) {
    return baseExpression.isCacheable(session);
  }

  public Optional<OIndexCandidate> findIndex(OIndexFinder info, OCommandContext ctx) {
    return this.baseExpression.findIndex(info, ctx);
  }
}
/* JavaCC - OriginalChecksum=e8015d01ce1ab2bc337062e9e3f2603e (do not edit this line) */
