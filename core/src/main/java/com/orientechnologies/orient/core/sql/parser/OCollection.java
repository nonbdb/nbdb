/* Generated By:JJTree: Do not edit this line. OCollection.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import com.orientechnologies.orient.core.sql.executor.YTResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OCollection extends SimpleNode {

  protected List<OExpression> expressions = new ArrayList<OExpression>();

  public OCollection(int id) {
    super(id);
  }

  public OCollection(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("[");
    boolean first = true;
    for (OExpression expr : expressions) {
      if (!first) {
        builder.append(", ");
      }
      expr.toString(params, builder);
      first = false;
    }
    builder.append("]");
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append("[");
    boolean first = true;
    for (OExpression expr : expressions) {
      if (!first) {
        builder.append(", ");
      }
      expr.toGenericStatement(builder);
      first = false;
    }
    builder.append("]");
  }

  public void add(OExpression exp) {
    this.expressions.add(exp);
  }

  public Object execute(YTIdentifiable iCurrentRecord, OCommandContext ctx) {
    List<Object> result = new ArrayList<Object>();
    for (OExpression exp : expressions) {
      result.add(exp.execute(iCurrentRecord, ctx));
    }
    return result;
  }

  public Object execute(YTResult iCurrentRecord, OCommandContext ctx) {
    List<Object> result = new ArrayList<Object>();
    for (OExpression exp : expressions) {
      result.add(convert(exp.execute(iCurrentRecord, ctx)));
    }
    return result;
  }

  private Object convert(Object item) {
    if (item instanceof YTResultSet) {
      return ((YTResultSet) item).stream().collect(Collectors.toList());
    }
    return item;
  }

  public boolean needsAliases(Set<String> aliases) {
    for (OExpression expr : this.expressions) {
      if (expr.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAggregate(YTDatabaseSessionInternal session) {
    for (OExpression exp : this.expressions) {
      if (exp.isAggregate(session)) {
        return true;
      }
    }
    return false;
  }

  public OCollection splitForAggregation(
      AggregateProjectionSplit aggregateProj, OCommandContext ctx) {
    var db = ctx.getDatabase();
    if (isAggregate(db)) {
      OCollection result = new OCollection(-1);
      for (OExpression exp : this.expressions) {
        if (exp.isAggregate(db) || exp.isEarlyCalculated(ctx)) {
          result.expressions.add(exp.splitForAggregation(aggregateProj, ctx));
        } else {
          throw new YTCommandExecutionException(
              "Cannot mix aggregate and non-aggregate operations in a collection: " + this);
        }
      }
      return result;
    } else {
      return this;
    }
  }

  public boolean isEarlyCalculated(OCommandContext ctx) {
    for (OExpression exp : expressions) {
      if (!exp.isEarlyCalculated(ctx)) {
        return false;
      }
    }
    return true;
  }

  public OCollection copy() {
    OCollection result = new OCollection(-1);
    result.expressions =
        expressions == null
            ? null
            : expressions.stream().map(x -> x.copy()).collect(Collectors.toList());
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

    OCollection that = (OCollection) o;

    return Objects.equals(expressions, that.expressions);
  }

  @Override
  public int hashCode() {
    return expressions != null ? expressions.hashCode() : 0;
  }

  public boolean refersToParent() {
    if (expressions != null) {
      for (OExpression exp : expressions) {
        if (exp != null && exp.refersToParent()) {
          return true;
        }
      }
    }
    return false;
  }

  public YTResult serialize(YTDatabaseSessionInternal db) {
    YTResultInternal result = new YTResultInternal(db);
    if (expressions != null) {
      result.setProperty(
          "expressions",
          expressions.stream().map(x -> x.serialize(db)).collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(YTResult fromResult) {
    if (fromResult.getProperty("expressions") != null) {
      expressions = new ArrayList<>();
      List<YTResult> ser = fromResult.getProperty("expressions");
      for (YTResult item : ser) {
        OExpression exp = new OExpression(-1);
        exp.deserialize(item);
        expressions.add(exp);
      }
    }
  }

  public boolean isCacheable(YTDatabaseSessionInternal session) {
    for (OExpression exp : expressions) {
      if (!exp.isCacheable(session)) {
        return false;
      }
    }
    return true;
  }

  public List<OExpression> getExpressions() {
    return expressions;
  }
}
/* JavaCC - OriginalChecksum=c93b20138b2ae58c5f76e458c34b5946 (do not edit this line) */
