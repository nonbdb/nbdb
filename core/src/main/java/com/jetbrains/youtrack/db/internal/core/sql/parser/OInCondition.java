/* Generated By:JJTree: Do not edit this line. OInCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.collection.OMultiValue;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.sql.executor.OIndexSearchInfo;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.OIndexCandidate;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.OIndexFinder;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.OPath;
import com.jetbrains.youtrack.db.internal.core.sql.operator.OQueryOperatorEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OInCondition extends OBooleanExpression {

  protected OExpression left;
  protected OBinaryCompareOperator operator;
  protected OSelectStatement rightStatement;
  protected OInputParameter rightParam;
  protected OMathExpression rightMathExpression;
  protected Object right;

  private static final Object UNSET = new Object();
  private final Object inputFinalValue = UNSET;

  public OInCondition(int id) {
    super(id);
  }

  public OInCondition(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(YTIdentifiable currentRecord, CommandContext ctx) {
    Object leftVal = evaluateLeft(currentRecord, ctx);
    Object rightVal = evaluateRight(currentRecord, ctx);
    if (rightVal == null) {
      return false;
    }
    return evaluateExpression(ctx.getDatabase(), leftVal, rightVal);
  }

  public Object evaluateRight(YTIdentifiable currentRecord, CommandContext ctx) {
    Object rightVal = null;
    if (rightStatement != null) {
      rightVal = executeQuery(rightStatement, ctx);
    } else if (rightParam != null) {
      rightVal = rightParam.getValue(ctx.getInputParameters());
    } else if (rightMathExpression != null) {
      rightVal = rightMathExpression.execute(currentRecord, ctx);
    }
    return rightVal;
  }

  public Object evaluateLeft(YTIdentifiable currentRecord, CommandContext ctx) {
    return left.execute(currentRecord, ctx);
  }

  @Override
  public boolean evaluate(YTResult currentRecord, CommandContext ctx) {
    Object rightVal = evaluateRight(currentRecord, ctx);
    if (rightVal == null) {
      return false;
    }

    if (left.isFunctionAny()) {
      return evaluateAny(currentRecord, rightVal, ctx);
    }

    if (left.isFunctionAll()) {
      return evaluateAllFunction(currentRecord, rightVal, ctx);
    }

    Object leftVal = evaluateLeft(currentRecord, ctx);
    return evaluateExpression(ctx.getDatabase(), leftVal, rightVal);
  }

  private boolean evaluateAny(YTResult currentRecord, Object rightVal, CommandContext ctx) {
    for (String s : currentRecord.getPropertyNames()) {
      Object leftVal = currentRecord.getProperty(s);
      if (evaluateExpression(ctx.getDatabase(), leftVal, rightVal)) {
        return true;
      }
    }
    return false;
  }

  private boolean evaluateAllFunction(YTResult currentRecord, Object rightVal,
      CommandContext ctx) {
    for (String s : currentRecord.getPropertyNames()) {
      Object leftVal = currentRecord.getProperty(s);
      if (!evaluateExpression(ctx.getDatabase(), leftVal, rightVal)) {
        return false;
      }
    }
    return true;
  }

  public Object evaluateRight(YTResult currentRecord, CommandContext ctx) {
    Object rightVal = null;
    if (rightStatement != null) {
      rightVal = executeQuery(rightStatement, ctx);
    } else if (rightParam != null) {
      rightVal = rightParam.getValue(ctx.getInputParameters());
    } else if (rightMathExpression != null) {
      rightVal = rightMathExpression.execute(currentRecord, ctx);
    }
    return rightVal;
  }

  public Object evaluateLeft(YTResult currentRecord, CommandContext ctx) {
    return left.execute(currentRecord, ctx);
  }

  protected static Object executeQuery(OSelectStatement rightStatement, CommandContext ctx) {
    BasicCommandContext subCtx = new BasicCommandContext();
    subCtx.setParentWithoutOverridingChild(ctx);
    YTResultSet result = rightStatement.execute(ctx.getDatabase(), ctx.getInputParameters(), false);
    return result.stream().collect(Collectors.toSet());
  }

  protected static boolean evaluateExpression(YTDatabaseSessionInternal session, final Object iLeft,
      final Object iRight) {
    if (OMultiValue.isMultiValue(iRight)) {
      if (iRight instanceof Set<?> set) {
        if (set.contains(iLeft)) {
          return true;
        }
        if (OMultiValue.isMultiValue(iLeft)) {
          for (final Object o : OMultiValue.getMultiValueIterable(iLeft)) {
            if (!set.contains(o)) {
              return false;
            }
          }
        }
      }

      for (final Object rightItem : OMultiValue.getMultiValueIterable(iRight)) {
        if (OQueryOperatorEquals.equals(session, iLeft, rightItem)) {
          return true;
        }
        if (OMultiValue.isMultiValue(iLeft)) {
          if (OMultiValue.getSize(iLeft) == 1) {
            Object leftItem = OMultiValue.getFirstValue(iLeft);
            if (compareItems(session, rightItem, leftItem)) {
              return true;
            }
          } else {
            for (final Object leftItem : OMultiValue.getMultiValueIterable(iLeft)) {
              if (compareItems(session, rightItem, leftItem)) {
                return true;
              }
            }
          }
        }
      }
    } else if (iRight.getClass().isArray()) {
      for (final Object rightItem : (Object[]) iRight) {
        if (OQueryOperatorEquals.equals(session, iLeft, rightItem)) {
          return true;
        }
      }
    } else if (iRight instanceof YTResultSet rsRight) {
      rsRight.reset();

      while (rsRight.hasNext()) {
        if (OQueryOperatorEquals.equals(session, iLeft, rsRight.next())) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean compareItems(YTDatabaseSessionInternal session, Object rightItem,
      Object leftItem) {
    if (OQueryOperatorEquals.equals(session, leftItem, rightItem)) {
      return true;
    }

    if (leftItem instanceof YTResult && ((YTResult) leftItem).getPropertyNames().size() == 1) {
      Object propValue =
          ((YTResult) leftItem)
              .getProperty(((YTResult) leftItem).getPropertyNames().iterator().next());
      return OQueryOperatorEquals.equals(session, propValue, rightItem);
    }

    return false;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    builder.append(" IN ");
    if (rightStatement != null) {
      builder.append("(");
      rightStatement.toString(params, builder);
      builder.append(")");
    } else if (right != null) {
      builder.append(convertToString(right));
    } else if (rightParam != null) {
      rightParam.toString(params, builder);
    } else if (rightMathExpression != null) {
      rightMathExpression.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    left.toGenericStatement(builder);
    builder.append(" IN ");
    if (rightStatement != null) {
      builder.append("(");
      rightStatement.toGenericStatement(builder);
      builder.append(")");
    } else if (right != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else if (rightParam != null) {
      rightParam.toGenericStatement(builder);
    } else if (rightMathExpression != null) {
      rightMathExpression.toGenericStatement(builder);
    }
  }

  private String convertToString(Object o) {
    if (o instanceof String) {
      return "\"" + ((String) o).replaceAll("\"", "\\\"") + "\"";
    }
    return o.toString();
  }

  @Override
  public boolean supportsBasicCalculation() {
    if (!left.supportsBasicCalculation()) {
      return false;
    }
    if (!rightMathExpression.supportsBasicCalculation()) {
      return false;
    }
    return operator.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    int total = 0;
    if (operator != null && !operator.supportsBasicCalculation()) {
      total++;
    }
    if (!left.supportsBasicCalculation()) {
      total++;
    }
    if (rightMathExpression != null && !rightMathExpression.supportsBasicCalculation()) {
      total++;
    }
    return total;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    List<Object> result = new ArrayList<Object>();

    if (operator != null) {
      result.add(this);
    }
    if (!left.supportsBasicCalculation()) {
      result.add(left);
    }
    if (rightMathExpression != null && !rightMathExpression.supportsBasicCalculation()) {
      result.add(rightMathExpression);
    }
    return result;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    if (left.needsAliases(aliases)) {
      return true;
    }

    return rightMathExpression != null && rightMathExpression.needsAliases(aliases);
  }

  @Override
  public OInCondition copy() {
    OInCondition result = new OInCondition(-1);
    result.operator = operator == null ? null : operator.copy();
    result.left = left == null ? null : left.copy();
    result.rightMathExpression = rightMathExpression == null ? null : rightMathExpression.copy();
    result.rightStatement = rightStatement == null ? null : rightStatement.copy();
    result.rightParam = rightParam == null ? null : rightParam.copy();
    result.right = right == null ? null : right;
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    if (left != null) {
      left.extractSubQueries(collector);
    }
    if (rightMathExpression != null) {
      rightMathExpression.extractSubQueries(collector);
    }
    if (rightStatement != null) {
      OIdentifier alias = collector.addStatement(rightStatement);
      rightMathExpression = new OBaseExpression(alias);
      rightStatement = null;
    }
  }

  @Override
  public boolean refersToParent() {
    if (left != null && left.refersToParent()) {
      return true;
    }
    if (rightStatement != null && rightStatement.refersToParent()) {
      return true;
    }
    return rightMathExpression != null && rightMathExpression.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OInCondition that = (OInCondition) o;

    if (!Objects.equals(left, that.left)) {
      return false;
    }
    if (!Objects.equals(operator, that.operator)) {
      return false;
    }
    if (!Objects.equals(rightStatement, that.rightStatement)) {
      return false;
    }
    if (!Objects.equals(rightParam, that.rightParam)) {
      return false;
    }
    if (!Objects.equals(rightMathExpression, that.rightMathExpression)) {
      return false;
    }
    if (!Objects.equals(right, that.right)) {
      return false;
    }
    return Objects.equals(inputFinalValue, that.inputFinalValue);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (operator != null ? operator.hashCode() : 0);
    result = 31 * result + (rightStatement != null ? rightStatement.hashCode() : 0);
    result = 31 * result + (rightParam != null ? rightParam.hashCode() : 0);
    result = 31 * result + (rightMathExpression != null ? rightMathExpression.hashCode() : 0);
    result = 31 * result + (right != null ? right.hashCode() : 0);
    result = 31 * result + (inputFinalValue != null ? inputFinalValue.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> leftX = left == null ? null : left.getMatchPatternInvolvedAliases();

    List<String> conditionX =
        rightMathExpression == null ? null : rightMathExpression.getMatchPatternInvolvedAliases();

    List<String> result = new ArrayList<String>();
    if (leftX != null) {
      result.addAll(leftX);
    }
    if (conditionX != null) {
      result.addAll(conditionX);
    }

    return result.size() == 0 ? null : result;
  }

  @Override
  public boolean isCacheable(YTDatabaseSessionInternal session) {
    if (left != null && !left.isCacheable(session)) {
      return false;
    }
    if (rightStatement != null && !rightStatement.executinPlanCanBeCached(session)) {
      return false;
    }
    return rightMathExpression == null || rightMathExpression.isCacheable(session);
  }

  public OExpression getLeft() {
    return left;
  }

  public void setLeft(OExpression left) {
    this.left = left;
  }

  public OSelectStatement getRightStatement() {
    return rightStatement;
  }

  public OInputParameter getRightParam() {
    return rightParam;
  }

  public OMathExpression getRightMathExpression() {
    return rightMathExpression;
  }

  public void setRightParam(OInputParameter rightParam) {
    this.rightParam = rightParam;
  }

  public void setRightMathExpression(OMathExpression rightMathExpression) {
    this.rightMathExpression = rightMathExpression;
  }

  public boolean isIndexAware(OIndexSearchInfo info) {
    if (left.isBaseIdentifier()) {
      if (info.getField().equals(left.getDefaultAlias().getStringValue())) {
        if (rightMathExpression != null) {
          return rightMathExpression.isEarlyCalculated(info.getCtx());
        } else {
          return rightParam != null;
        }
      }
    }
    return false;
  }

  public Optional<OIndexCandidate> findIndex(OIndexFinder info, CommandContext ctx) {
    Optional<OPath> path = left.getPath();
    if (path.isPresent()) {
      if (rightMathExpression != null && rightMathExpression.isEarlyCalculated(ctx)) {
        Object value = rightMathExpression.execute((YTResult) null, ctx);
        return info.findExactIndex(path.get(), value, ctx);
      }
    }

    return Optional.empty();
  }

  @Override
  public OExpression resolveKeyFrom(OBinaryCondition additional) {
    OExpression item = new OExpression(-1);
    if (rightMathExpression != null) {
      item.setMathExpression(rightMathExpression);
      return item;
    } else if (rightParam != null) {
      OBaseExpression e = new OBaseExpression(-1);
      e.setInputParam(rightParam.copy());
      item.setMathExpression(e);
      return item;
    } else {
      throw new UnsupportedOperationException("Cannot execute index query with " + this);
    }
  }

  @Override
  public OExpression resolveKeyTo(OBinaryCondition additional) {
    OExpression item = new OExpression(-1);
    if (rightMathExpression != null) {
      item.setMathExpression(rightMathExpression);
      return item;
    } else if (rightParam != null) {
      OBaseExpression e = new OBaseExpression(-1);
      e.setInputParam(rightParam.copy());
      item.setMathExpression(e);
      return item;
    } else {
      throw new UnsupportedOperationException("Cannot execute index query with " + this);
    }
  }
}
/* JavaCC - OriginalChecksum=00df7cb1877c0a12d24205c1700653c7 (do not edit this line) */
