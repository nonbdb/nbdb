/* Generated By:JJTree: Do not edit this line. SQLNotBlock.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.sql.executor.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexCandidate;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexFinder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SQLNotBlock extends SQLBooleanExpression {

  protected SQLBooleanExpression sub;

  protected boolean negate = false;

  public SQLNotBlock(int id) {
    super(id);
  }

  public SQLNotBlock(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    if (sub == null) {
      return true;
    }
    boolean result = sub.evaluate(currentRecord, ctx);
    if (negate) {
      return !result;
    }
    return result;
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    if (sub == null) {
      return true;
    }
    boolean result = sub.evaluate(currentRecord, ctx);
    if (negate) {
      return !result;
    }
    return result;
  }

  public SQLBooleanExpression getSub() {
    return sub;
  }

  public void setSub(SQLBooleanExpression sub) {
    this.sub = sub;
  }

  public boolean isNegate() {
    return negate;
  }

  public void setNegate(boolean negate) {
    this.negate = negate;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (negate) {
      builder.append("NOT ");
    }
    sub.toString(params, builder);
  }

  public void toGenericStatement(StringBuilder builder) {
    if (negate) {
      builder.append("NOT ");
    }
    sub.toGenericStatement(builder);
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    return sub.getNumberOfExternalCalculations();
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    return sub.getExternalCalculationConditions();
  }

  public List<SQLBinaryCondition> getIndexedFunctionConditions(
      SchemaClass iSchemaClass, DatabaseSessionInternal database) {
    if (sub == null) {
      return null;
    }
    if (negate) {
      return null;
    }
    return sub.getIndexedFunctionConditions(iSchemaClass, database);
  }

  @Override
  public List<SQLAndBlock> flatten() {
    if (!negate) {
      return sub.flatten();
    }
    return super.flatten();
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    return sub.needsAliases(aliases);
  }

  @Override
  public SQLNotBlock copy() {
    SQLNotBlock result = new SQLNotBlock(-1);
    result.sub = sub.copy();
    result.negate = negate;
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    sub.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return sub.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SQLNotBlock oNotBlock = (SQLNotBlock) o;

    if (negate != oNotBlock.negate) {
      return false;
    }
    return Objects.equals(sub, oNotBlock.sub);
  }

  @Override
  public int hashCode() {
    int result = sub != null ? sub.hashCode() : 0;
    result = 31 * result + (negate ? 1 : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    return sub.getMatchPatternInvolvedAliases();
  }

  @Override
  public void translateLuceneOperator() {
    sub.translateLuceneOperator();
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    return sub.isCacheable(session);
  }

  @Override
  public SQLBooleanExpression rewriteIndexChainsAsSubqueries(CommandContext ctx,
      SchemaClass clazz) {
    if (!negate) {
      sub = sub.rewriteIndexChainsAsSubqueries(ctx, clazz);
    }
    return this;
  }

  public Optional<IndexCandidate> findIndex(IndexFinder info, CommandContext ctx) {
    Optional<IndexCandidate> found = sub.findIndex(info, ctx);
    if (negate && found.isPresent()) {
      found = found.get().invert();
    }
    return found;
  }

  @Override
  public boolean isAlwaysTrue() {
    if (negate) {
      return false;
    }
    return sub.isAlwaysTrue();
  }
}
/* JavaCC - OriginalChecksum=1926313b3f854235aaa20811c22d583b (do not edit this line) */
