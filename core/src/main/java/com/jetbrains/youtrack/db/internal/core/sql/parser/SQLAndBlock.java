/* Generated By:JJTree: Do not edit this line. SQLAndBlock.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexCandidate;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexFinder;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.MultipleIndexCanditate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SQLAndBlock extends SQLBooleanExpression {

  List<SQLBooleanExpression> subBlocks = new ArrayList<SQLBooleanExpression>();

  public SQLAndBlock(int id) {
    super(id);
  }

  public SQLAndBlock(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    if (subBlocks == null) {
      return true;
    }

    for (SQLBooleanExpression block : subBlocks) {
      if (!block.evaluate(currentRecord, ctx)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    if (subBlocks == null) {
      return true;
    }

    for (SQLBooleanExpression block : subBlocks) {
      if (!block.evaluate(currentRecord, ctx)) {
        return false;
      }
    }
    return true;
  }

  public List<SQLBooleanExpression> getSubBlocks() {
    return subBlocks;
  }

  public void setSubBlocks(List<SQLBooleanExpression> subBlocks) {
    this.subBlocks = subBlocks;
  }

  public void addSubBlock(SQLBooleanExpression block) {
    this.subBlocks.add(block);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (subBlocks == null || subBlocks.size() == 0) {
      return;
    }
    boolean first = true;
    for (SQLBooleanExpression expr : subBlocks) {
      if (!first) {
        builder.append(" AND ");
      }
      expr.toString(params, builder);
      first = false;
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    if (subBlocks == null || subBlocks.size() == 0) {
      return;
    }
    boolean first = true;
    for (SQLBooleanExpression expr : subBlocks) {
      if (!first) {
        builder.append(" AND ");
      }
      expr.toGenericStatement(builder);
      first = false;
    }
  }

  @Override
  protected boolean supportsBasicCalculation() {
    for (SQLBooleanExpression expr : subBlocks) {
      if (!expr.supportsBasicCalculation()) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    int result = 0;
    for (SQLBooleanExpression expr : subBlocks) {
      result += expr.getNumberOfExternalCalculations();
    }
    return result;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    List<Object> result = new ArrayList<Object>();
    for (SQLBooleanExpression expr : subBlocks) {
      result.addAll(expr.getExternalCalculationConditions());
    }
    return result;
  }

  public List<SQLBinaryCondition> getIndexedFunctionConditions(
      SchemaClass iSchemaClass, DatabaseSessionInternal database) {
    if (subBlocks == null) {
      return null;
    }
    List<SQLBinaryCondition> result = new ArrayList<SQLBinaryCondition>();
    for (SQLBooleanExpression exp : subBlocks) {
      List<SQLBinaryCondition> sub = exp.getIndexedFunctionConditions(iSchemaClass, database);
      if (sub != null && sub.size() > 0) {
        result.addAll(sub);
      }
    }
    return result.size() == 0 ? null : result;
  }

  public List<SQLAndBlock> flatten() {
    List<SQLAndBlock> result = new ArrayList<SQLAndBlock>();
    boolean first = true;
    for (SQLBooleanExpression sub : subBlocks) {
      List<SQLAndBlock> subFlattened = sub.flatten();
      List<SQLAndBlock> oldResult = result;
      result = new ArrayList<SQLAndBlock>();
      for (SQLAndBlock subAndItem : subFlattened) {
        if (first) {
          result.add(subAndItem);
        } else {
          for (SQLAndBlock oldResultItem : oldResult) {
            SQLAndBlock block = new SQLAndBlock(-1);
            block.subBlocks.addAll(oldResultItem.subBlocks);
            for (SQLBooleanExpression resultItem : subAndItem.subBlocks) {
              block.subBlocks.add(resultItem);
            }
            result.add(block);
          }
        }
      }
      first = false;
    }
    return result;
  }

  protected SQLAndBlock encapsulateInAndBlock(SQLBooleanExpression item) {
    if (item instanceof SQLAndBlock) {
      return (SQLAndBlock) item;
    }
    SQLAndBlock result = new SQLAndBlock(-1);
    result.subBlocks.add(item);
    return result;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    for (SQLBooleanExpression block : subBlocks) {
      if (block.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public SQLAndBlock copy() {
    SQLAndBlock result = new SQLAndBlock(-1);
    for (SQLBooleanExpression exp : subBlocks) {
      result.subBlocks.add(exp.copy());
    }
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

    SQLAndBlock andBlock = (SQLAndBlock) o;

    return Objects.equals(subBlocks, andBlock.subBlocks);
  }

  @Override
  public int hashCode() {
    return subBlocks != null ? subBlocks.hashCode() : 0;
  }

  @Override
  public boolean isEmpty() {
    if (subBlocks.isEmpty()) {
      return true;
    }
    for (SQLBooleanExpression block : subBlocks) {
      if (!block.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    for (SQLBooleanExpression exp : subBlocks) {
      exp.extractSubQueries(collector);
    }
  }

  @Override
  public boolean refersToParent() {
    for (SQLBooleanExpression exp : subBlocks) {
      if (exp.refersToParent()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> result = new ArrayList<String>();
    for (SQLBooleanExpression exp : subBlocks) {
      List<String> x = exp.getMatchPatternInvolvedAliases();
      if (x != null) {
        result.addAll(x);
      }
    }
    return result.size() == 0 ? null : result;
  }

  @Override
  public void translateLuceneOperator() {
    subBlocks.forEach(x -> x.translateLuceneOperator());
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    for (SQLBooleanExpression exp : subBlocks) {
      if (!exp.isCacheable(session)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public SQLBooleanExpression rewriteIndexChainsAsSubqueries(CommandContext ctx,
      SchemaClassInternal clazz) {
    for (SQLBooleanExpression exp : subBlocks) {
      exp.rewriteIndexChainsAsSubqueries(ctx, clazz);
      // this is on purpose. Multiple optimizations in this case in an AND block can
      // lead to wrong results (no intersection)
      return this;
    }
    return this;
  }

  public Optional<IndexCandidate> findIndex(IndexFinder info, CommandContext ctx) {
    Optional<IndexCandidate> result = Optional.empty();
    for (SQLBooleanExpression exp : subBlocks) {
      Optional<IndexCandidate> singleResult = exp.findIndex(info, ctx);
      if (singleResult.isPresent()) {
        if (result.isPresent()) {
          if (result.get() instanceof MultipleIndexCanditate) {
            ((MultipleIndexCanditate) result.get()).addCanditate(singleResult.get());
          } else {
            MultipleIndexCanditate mult = new MultipleIndexCanditate();
            mult.addCanditate(result.get());
            mult.addCanditate(singleResult.get());
            result = Optional.of(mult);
          }
        } else {
          result = singleResult;
        }
      }
    }
    return result;
  }

  @Override
  public boolean isAlwaysTrue() {
    if (subBlocks.isEmpty()) {
      return true;
    }
    for (SQLBooleanExpression exp : subBlocks) {
      if (!exp.isAlwaysTrue()) {
        return false;
      }
    }
    return true;
  }
}
/* JavaCC - OriginalChecksum=cf1f66cc86cfc93d357f9fcdfa4a4604 (do not edit this line) */
