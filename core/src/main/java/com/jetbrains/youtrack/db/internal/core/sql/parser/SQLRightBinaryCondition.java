/* Generated By:JJTree: Do not edit this line. SQLRightBinaryCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.exception.BaseException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLRightBinaryCondition extends SimpleNode {

  SQLBinaryCompareOperator operator;

  boolean not = false;
  SQLInOperator inOperator;

  SQLExpression right;

  public SQLRightBinaryCondition(int id) {
    super(id);
  }

  public SQLRightBinaryCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public SQLRightBinaryCondition copy() {
    SQLRightBinaryCondition result = new SQLRightBinaryCondition(-1);
    result.operator = operator == null ? null : operator.copy();
    result.not = not;
    result.inOperator = inOperator == null ? null : inOperator.copy();
    result.right = right == null ? null : right.copy();
    return result;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (operator != null) {
      builder.append(operator);
      builder.append(" ");
      right.toString(params, builder);
    } else if (inOperator != null) {
      if (not) {
        builder.append("NOT ");
      }
      builder.append("IN ");
      right.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    if (operator != null) {
      if (operator instanceof SimpleNode) {
        ((SimpleNode) operator).toGenericStatement(builder);
      } else {
        builder.append(operator);
      }
      builder.append(" ");
      right.toGenericStatement(builder);
    } else if (inOperator != null) {
      if (not) {
        builder.append("NOT ");
      }
      builder.append("IN ");
      right.toGenericStatement(builder);
    }
  }

  public Object execute(Result iCurrentRecord, Object elementToFilter, CommandContext ctx) {
    if (elementToFilter == null) {
      return null;
    }
    Iterator iterator;
    if (elementToFilter instanceof Identifiable) {
      iterator = Collections.singleton(elementToFilter).iterator();
    } else if (elementToFilter instanceof Iterable) {
      iterator = ((Iterable) elementToFilter).iterator();
    } else if (elementToFilter instanceof Iterator) {
      iterator = (Iterator) elementToFilter;
    } else {
      iterator = Collections.singleton(elementToFilter).iterator();
    }

    List result = new ArrayList();
    while (iterator.hasNext()) {
      Object element = iterator.next();
      if (matchesFilters(iCurrentRecord, element, ctx)) {
        result.add(element);
      }
    }
    return result;
  }

  public Object execute(Identifiable iCurrentRecord, Object elementToFilter,
      CommandContext ctx) {
    if (elementToFilter == null) {
      return null;
    }
    Iterator iterator;
    if (elementToFilter instanceof Identifiable) {
      iterator = Collections.singleton(elementToFilter).iterator();
    } else if (elementToFilter instanceof Iterable) {
      iterator = ((Iterable) elementToFilter).iterator();
    } else if (elementToFilter instanceof Iterator) {
      iterator = (Iterator) elementToFilter;
    } else {
      iterator = Collections.singleton(elementToFilter).iterator();
    }

    List result = new ArrayList();
    while (iterator.hasNext()) {
      Object element = iterator.next();
      if (matchesFilters(iCurrentRecord, element, ctx)) {
        result.add(element);
      }
    }
    return result;
  }

  private boolean matchesFilters(
      Identifiable iCurrentRecord, Object element, CommandContext ctx) {
    if (operator != null) {
      operator.execute(element, right.execute(iCurrentRecord, ctx));
    } else if (inOperator != null) {

      Object rightVal = evaluateRight(iCurrentRecord, ctx);
      if (rightVal == null) {
        return false;
      }
      boolean result = SQLInCondition.evaluateExpression(ctx.getDatabase(), element, rightVal);
      if (not) {
        result = !result;
      }
      return result;
    }
    return false;
  }

  private boolean matchesFilters(Result iCurrentRecord, Object element, CommandContext ctx) {
    if (operator != null) {
      return operator.execute(element, right.execute(iCurrentRecord, ctx));
    } else if (inOperator != null) {

      Object rightVal = evaluateRight(iCurrentRecord, ctx);
      if (rightVal == null) {
        return false;
      }
      boolean result = SQLInCondition.evaluateExpression(ctx.getDatabase(), element, rightVal);
      if (not) {
        result = !result;
      }
      return result;
    }
    return false;
  }

  public Object evaluateRight(Identifiable currentRecord, CommandContext ctx) {
    return right.execute(currentRecord, ctx);
  }

  public Object evaluateRight(Result currentRecord, CommandContext ctx) {
    return right.execute(currentRecord, ctx);
  }

  public boolean needsAliases(Set<String> aliases) {
    return right != null && right.needsAliases(aliases);
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (right != null) {
      right.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    return right != null && right.refersToParent();
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    result.setProperty("operator", operator.getClass().getName());
    result.setProperty("not", not);
    result.setProperty("in", inOperator != null);
    result.setProperty("right", right.serialize(db));
    return result;
  }

  public void deserialize(Result fromResult) {
    try {
      operator =
          (SQLBinaryCompareOperator)
              Class.forName(String.valueOf(fromResult.getProperty("operator"))).newInstance();
    } catch (Exception e) {
      throw BaseException.wrapException(new CommandExecutionException(""), e);
    }
    not = fromResult.getProperty("not");
    if (Boolean.TRUE.equals(fromResult.getProperty("in"))) {
      inOperator = new SQLInOperator(-1);
    }
    right = new SQLExpression(-1);
    right.deserialize(fromResult.getProperty("right"));
  }
}
/* JavaCC - OriginalChecksum=29d59ae04778eb611547292a27863da4 (do not edit this line) */
