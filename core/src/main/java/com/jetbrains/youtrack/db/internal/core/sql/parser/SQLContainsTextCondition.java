/* Generated By:JJTree: Do not edit this line. SQLContainsTextCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.sql.executor.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexCandidate;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexFinder;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.MetadataPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SQLContainsTextCondition extends SQLBooleanExpression {

  protected SQLExpression left;
  protected SQLExpression right;

  public SQLContainsTextCondition(int id) {
    super(id);
  }

  public SQLContainsTextCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    Object leftValue = left.execute(currentRecord, ctx);
    if (leftValue == null || !(leftValue instanceof String)) {
      return false;
    }
    Object rightValue = right.execute(currentRecord, ctx);
    if (rightValue == null || !(rightValue instanceof String)) {
      return false;
    }

    return ((String) leftValue).indexOf((String) rightValue) > -1;
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    if (left.isFunctionAny()) {
      return evaluateAny(currentRecord, ctx);
    }

    if (left.isFunctionAll()) {
      return evaluateAllFunction(currentRecord, ctx);
    }
    Object leftValue = left.execute(currentRecord, ctx);
    if (leftValue == null || !(leftValue instanceof String)) {
      return false;
    }
    Object rightValue = right.execute(currentRecord, ctx);
    if (rightValue == null || !(rightValue instanceof String)) {
      return false;
    }

    return ((String) leftValue).indexOf((String) rightValue) > -1;
  }

  private boolean evaluateAny(Result currentRecord, CommandContext ctx) {
    Object rightValue = right.execute(currentRecord, ctx);
    if (rightValue == null || !(rightValue instanceof String)) {
      return false;
    }

    for (String s : currentRecord.getPropertyNames()) {
      Object leftValue = currentRecord.getProperty(s);
      if (leftValue == null || !(leftValue instanceof String)) {
        continue;
      }

      if (((String) leftValue).indexOf((String) rightValue) > -1) {
        return true;
      }
    }
    return false;
  }

  private boolean evaluateAllFunction(Result currentRecord, CommandContext ctx) {
    Object rightValue = right.execute(currentRecord, ctx);
    if (rightValue == null || !(rightValue instanceof String)) {
      return false;
    }

    for (String s : currentRecord.getPropertyNames()) {
      Object leftValue = currentRecord.getProperty(s);
      if (leftValue == null || !(leftValue instanceof String)) {
        return false;
      }

      if (!(((String) leftValue).indexOf((String) rightValue) > -1)) {
        return false;
      }
    }
    return true;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    builder.append(" CONTAINSTEXT ");
    right.toString(params, builder);
  }

  public void toGenericStatement(StringBuilder builder) {
    left.toGenericStatement(builder);
    builder.append(" CONTAINSTEXT ");
    right.toGenericStatement(builder);
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    int total = 0;
    if (!left.supportsBasicCalculation()) {
      total++;
    }
    if (!right.supportsBasicCalculation()) {
      total++;
    }
    return total;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    List<Object> result = new ArrayList<Object>();
    if (!left.supportsBasicCalculation()) {
      result.add(left);
    }
    if (!right.supportsBasicCalculation()) {
      result.add(right);
    }
    return result;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    if (!left.needsAliases(aliases)) {
      return true;
    }
    return !right.needsAliases(aliases);
  }

  @Override
  public SQLContainsTextCondition copy() {
    SQLContainsTextCondition result = new SQLContainsTextCondition(-1);
    result.left = left.copy();
    result.right = right.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    left.extractSubQueries(collector);
    right.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return left.refersToParent() || right.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SQLContainsTextCondition that = (SQLContainsTextCondition) o;

    if (!Objects.equals(left, that.left)) {
      return false;
    }
    return Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (right != null ? right.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> leftX = left == null ? null : left.getMatchPatternInvolvedAliases();
    List<String> rightX = right == null ? null : right.getMatchPatternInvolvedAliases();

    List<String> result = new ArrayList<String>();
    if (leftX != null) {
      result.addAll(leftX);
    }
    if (rightX != null) {
      result.addAll(rightX);
    }

    return result.size() == 0 ? null : result;
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    if (left != null && !left.isCacheable(session)) {
      return false;
    }
    return right == null || right.isCacheable(session);
  }

  public void setLeft(SQLExpression left) {
    this.left = left;
  }

  public void setRight(SQLExpression right) {
    this.right = right;
  }

  public SQLExpression getLeft() {
    return left;
  }

  public SQLExpression getRight() {
    return right;
  }

  public Optional<IndexCandidate> findIndex(IndexFinder info, CommandContext ctx) {
    Optional<MetadataPath> path = left.getPath();
    if (path.isPresent()) {
      if (right != null && right.isEarlyCalculated(ctx)) {
        Object value = right.execute((Result) null, ctx);
        return info.findFullTextIndex(path.get(), value, ctx);
      }
    }

    return Optional.empty();
  }

  @Override
  public boolean isFullTextIndexAware(String indexField) {
    if (left.isBaseIdentifier()) {
      String fieldName = left.getDefaultAlias().getStringValue();
      return indexField.equals(fieldName);
    }
    return false;
  }

  @Override
  public SQLExpression resolveKeyFrom(SQLBinaryCondition additional) {
    if (right != null) {
      return right;
    } else {
      throw new UnsupportedOperationException("Cannot execute index query with " + this);
    }
  }

  @Override
  public SQLExpression resolveKeyTo(SQLBinaryCondition additional) {
    return right;
  }
}
/* JavaCC - OriginalChecksum=b588492ba2cbd0f932055f1f64bbbecd (do not edit this line) */
