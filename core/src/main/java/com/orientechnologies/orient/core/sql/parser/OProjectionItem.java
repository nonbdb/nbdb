/* Generated By:JJTree: Do not edit this line. OProjectionItem.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.OEdgeToVertexIterable;
import com.orientechnologies.orient.core.record.impl.OEdgeToVertexIterator;
import com.orientechnologies.orient.core.sql.executor.AggregationContext;
import com.orientechnologies.orient.core.sql.executor.OInternalResultSet;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OProjectionItem extends SimpleNode {

  protected boolean exclude = false;

  protected boolean all = false;

  protected OIdentifier alias;

  protected OExpression expression;

  protected Boolean aggregate;

  protected ONestedProjection nestedProjection;

  public OProjectionItem(
      OExpression expression, OIdentifier alias, ONestedProjection nestedProjection) {
    super(-1);
    this.expression = expression;
    this.alias = alias;
    this.nestedProjection = nestedProjection;
  }

  public OProjectionItem(int id) {
    super(id);
  }

  public OProjectionItem(OrientSql p, int id) {
    super(p, id);
  }

  public boolean isAll() {
    if (all) {
      return true;
    }
    return expression != null && "*".equals(expression.toString());
  }

  public void setAll(boolean all) {
    this.all = all;
  }

  public OIdentifier getAlias() {
    return alias;
  }

  public void setAlias(OIdentifier alias) {
    this.alias = alias;
  }

  public OExpression getExpression() {
    return expression;
  }

  public void setExpression(OExpression expression) {
    this.expression = expression;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (all) {
      builder.append("*");
    } else {
      if (exclude) {
        builder.append("!");
      }
      if (expression != null) {
        expression.toString(params, builder);
      }
      if (nestedProjection != null) {
        builder.append(" ");
        nestedProjection.toString(params, builder);
      }
      if (alias != null) {

        builder.append(" AS ");
        alias.toString(params, builder);
      }
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (all) {
      builder.append("*");
    } else {
      if (exclude) {
        builder.append("!");
      }
      if (expression != null) {
        expression.toGenericStatement(builder);
      }
      if (nestedProjection != null) {
        builder.append(" ");
        nestedProjection.toGenericStatement(builder);
      }
      if (alias != null) {

        builder.append(" AS ");
        alias.toGenericStatement(builder);
      }
    }
  }

  public Object execute(OIdentifiable iCurrentRecord, OCommandContext ctx) {
    Object result;
    if (all) {
      result = iCurrentRecord;
    } else {
      result = expression.execute(iCurrentRecord, ctx);
    }
    if (nestedProjection != null) {
      result = nestedProjection.apply(expression, result, ctx);
    }
    return convert(result, ctx);
  }

  public static Object convert(Object value, OCommandContext context) {
    if (value instanceof ORidBag) {
      List result = new ArrayList();
      ((ORidBag) value).iterator().forEachRemaining(result::add);
      return result;
    }
    if (value instanceof OEdgeToVertexIterable) {
      value = ((OEdgeToVertexIterable) value).iterator();
    }
    if (value instanceof OEdgeToVertexIterator) {
      List<ORID> result = new ArrayList<>();
      while (((OEdgeToVertexIterator) value).hasNext()) {
        OVertex v = ((OEdgeToVertexIterator) value).next();
        if (v != null) {
          result.add(v.getIdentity());
        }
      }
      return result;
    }
    if (value instanceof OInternalResultSet) {
      ((OInternalResultSet) value).reset();
      value = ((OInternalResultSet) value).stream().collect(Collectors.toList());
    }
    if (value instanceof OExecutionStream) {
      value = ((OExecutionStream) value).stream(context).collect(Collectors.toList());
    }
    if (!(value instanceof OIdentifiable)) {
      Iterator<?> iter = null;
      if (value instanceof Iterator) {
        iter = (Iterator<?>) value;
      } else if (value instanceof Iterable && !(value instanceof Collection<?>)) {
        iter = ((Iterable<?>) value).iterator();
      }

      if (iter != null) {
        var list = new ArrayList<>();
        while (iter.hasNext()) {
          list.add(iter.next());
        }

        value = list;
      }
    }

    return value;
  }

  public Object execute(OResult iCurrentRecord, OCommandContext ctx) {
    Object result;
    if (all) {
      result = iCurrentRecord;
    } else {
      result = expression.execute(iCurrentRecord, ctx);
    }
    if (nestedProjection != null) {
      if (result instanceof ODocument && ((ODocument) result).isEmpty()) {
        ((ODocument) result).load(null);
      }
      result = nestedProjection.apply(expression, result, ctx);
    }
    return convert(result, ctx);
  }

  /**
   * returns the final alias for this projection item (the explicit alias, if defined, or the
   * default alias)
   *
   * @return the final alias for this projection item
   */
  public String getProjectionAliasAsString() {
    return getProjectionAlias().getStringValue();
  }

  public OIdentifier getProjectionAlias() {
    if (alias != null) {
      return alias;
    }
    OIdentifier result;
    if (all) {
      result = new OIdentifier("*");
    } else {
      result = expression.getDefaultAlias();
    }
    return result;
  }

  public boolean isExpand() {
    return expression.isExpand();
  }

  public OProjectionItem getExpandContent() {
    OProjectionItem result = new OProjectionItem(-1);
    result.expression = expression.getExpandContent();
    return result;
  }

  public boolean isAggregate() {
    if (aggregate != null) {
      return aggregate;
    }
    if (all) {
      aggregate = false;
      return false;
    }
    if (expression.isAggregate()) {
      aggregate = true;
      return true;
    }
    aggregate = false;
    return false;
  }

  /**
   * INTERNAL USE ONLY this has to be invoked ONLY if the item is aggregate!!!
   *
   * @param aggregateSplit
   */
  public OProjectionItem splitForAggregation(
      AggregateProjectionSplit aggregateSplit, OCommandContext ctx) {
    if (isAggregate()) {
      OProjectionItem result = new OProjectionItem(-1);
      result.alias = getProjectionAlias();
      result.expression = expression.splitForAggregation(aggregateSplit, ctx);
      result.nestedProjection = nestedProjection;
      return result;
    } else {
      return this;
    }
  }

  public AggregationContext getAggregationContext(OCommandContext ctx) {
    if (expression == null) {
      throw new OCommandExecutionException("Cannot aggregate on this projection: " + this);
    }
    return expression.getAggregationContext(ctx);
  }

  public OProjectionItem copy() {
    OProjectionItem result = new OProjectionItem(-1);
    result.exclude = exclude;
    result.all = all;
    result.alias = alias == null ? null : alias.copy();
    result.expression = expression == null ? null : expression.copy();
    result.nestedProjection = nestedProjection == null ? null : nestedProjection.copy();
    result.aggregate = aggregate;
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
    OProjectionItem that = (OProjectionItem) o;
    return exclude == that.exclude
        && all == that.all
        && Objects.equals(alias, that.alias)
        && Objects.equals(expression, that.expression)
        && Objects.equals(aggregate, that.aggregate)
        && Objects.equals(nestedProjection, that.nestedProjection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exclude, all, alias, expression, aggregate, nestedProjection);
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (expression != null) {
      expression.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    if (expression != null) {
      return expression.refersToParent();
    }
    return false;
  }

  public OResult serialize() {
    OResultInternal result = new OResultInternal();
    result.setProperty("all", all);
    if (alias != null) {
      result.setProperty("alias", alias.serialize());
    }
    if (expression != null) {
      result.setProperty("expression", expression.serialize());
    }
    result.setProperty("aggregate", aggregate);
    if (nestedProjection != null) {
      result.setProperty("nestedProjection", nestedProjection.serialize());
    }
    result.setProperty("exclude", exclude);
    return result;
  }

  public void deserialize(OResult fromResult) {
    all = fromResult.getProperty("all");
    if (fromResult.getProperty("alias") != null) {
      alias = OIdentifier.deserialize(fromResult.getProperty("alias"));
    }
    if (fromResult.getProperty("expression") != null) {
      expression = new OExpression(-1);
      expression.deserialize(fromResult.getProperty("expression"));
    }
    aggregate = fromResult.getProperty("aggregate");
    if (fromResult.getProperty("nestedProjection") != null) {
      nestedProjection = new ONestedProjection(-1);
      nestedProjection.deserialize(fromResult.getProperty("nestedProjection"));
    }
    if (Boolean.TRUE.equals(fromResult.getProperty("exclude"))) {
      exclude = true;
    }
  }

  public void setNestedProjection(ONestedProjection nestedProjection) {
    this.nestedProjection = nestedProjection;
  }

  public boolean isCacheable() {
    if (expression != null) {
      return expression.isCacheable();
    }
    return true;
  }
}
/* JavaCC - OriginalChecksum=6d6010734c7434a6f516e2eac308e9ce (do not edit this line) */
