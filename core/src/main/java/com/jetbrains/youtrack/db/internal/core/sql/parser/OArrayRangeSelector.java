/* Generated By:JJTree: Do not edit this line. OArrayRangeSelector.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.collection.OMultiValue;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultInternal;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OArrayRangeSelector extends SimpleNode {

  protected Integer from;
  protected Integer to;
  protected boolean newRange = false;
  protected boolean included = false;

  protected OArrayNumberSelector fromSelector;
  protected OArrayNumberSelector toSelector;

  public OArrayRangeSelector(int id) {
    super(id);
  }

  public OArrayRangeSelector(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (from != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else {
      fromSelector.toString(params, builder);
    }
    if (newRange) {
      builder.append("..");
      if (included) {
        builder.append('.');
      }
    } else {
      builder.append("-");
    }
    if (to != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else {
      toSelector.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    if (from != null) {
      builder.append(from);
    } else {
      fromSelector.toGenericStatement(builder);
    }
    if (newRange) {
      builder.append("..");
      if (included) {
        builder.append('.');
      }
    } else {
      builder.append("-");
    }
    if (to != null) {
      builder.append(to);
    } else {
      toSelector.toGenericStatement(builder);
    }
  }

  public Object execute(YTIdentifiable iCurrentRecord, Object result, CommandContext ctx) {
    if (result == null) {
      return null;
    }
    if (!OMultiValue.isMultiValue(result)) {
      return null;
    }
    Integer lFrom = from;
    if (fromSelector != null) {
      lFrom = fromSelector.getValue(iCurrentRecord, result, ctx);
    }
    if (lFrom == null) {
      lFrom = 0;
    }
    Integer lTo = to;
    if (toSelector != null) {
      lTo = toSelector.getValue(iCurrentRecord, result, ctx);
    }
    if (included) {
      lTo++;
    }
    if (lFrom > lTo) {
      return null;
    }
    Object[] arrayResult = OMultiValue.array(result);

    if (arrayResult == null || arrayResult.length == 0) {
      return arrayResult;
    }
    lFrom = Math.max(lFrom, 0);
    if (arrayResult.length < lFrom) {
      return null;
    }
    lFrom = Math.min(lFrom, arrayResult.length - 1);

    lTo = Math.min(lTo, arrayResult.length);

    return Arrays.asList(Arrays.copyOfRange(arrayResult, lFrom, lTo));
  }

  public Object execute(YTResult iCurrentRecord, Object result, CommandContext ctx) {
    if (result == null) {
      return null;
    }
    if (!OMultiValue.isMultiValue(result)) {
      return null;
    }
    Integer lFrom = from;
    if (fromSelector != null) {
      lFrom = fromSelector.getValue(iCurrentRecord, result, ctx);
    }
    if (lFrom == null) {
      lFrom = 0;
    }
    Integer lTo = to;
    if (toSelector != null) {
      lTo = toSelector.getValue(iCurrentRecord, result, ctx);
    }
    if (included) {
      lTo++;
    }
    if (lFrom > lTo) {
      return null;
    }
    Object[] arrayResult = OMultiValue.array(result);

    if (arrayResult == null || arrayResult.length == 0) {
      return arrayResult;
    }
    lFrom = Math.max(lFrom, 0);
    if (arrayResult.length < lFrom) {
      return null;
    }
    lFrom = Math.min(lFrom, arrayResult.length - 1);

    lTo = Math.min(lTo, arrayResult.length);

    return Arrays.asList(Arrays.copyOfRange(arrayResult, lFrom, lTo));
  }

  public boolean needsAliases(Set<String> aliases) {
    if (fromSelector != null && fromSelector.needsAliases(aliases)) {
      return true;
    }
    return toSelector != null && toSelector.needsAliases(aliases);
  }

  public OArrayRangeSelector copy() {
    OArrayRangeSelector result = new OArrayRangeSelector(-1);
    result.from = from;
    result.to = to;
    result.newRange = newRange;
    result.included = included;

    result.fromSelector = fromSelector == null ? null : fromSelector.copy();
    result.toSelector = toSelector == null ? null : toSelector.copy();

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

    OArrayRangeSelector that = (OArrayRangeSelector) o;

    if (newRange != that.newRange) {
      return false;
    }
    if (included != that.included) {
      return false;
    }
    if (!Objects.equals(from, that.from)) {
      return false;
    }
    if (!Objects.equals(to, that.to)) {
      return false;
    }
    if (!Objects.equals(fromSelector, that.fromSelector)) {
      return false;
    }
    return Objects.equals(toSelector, that.toSelector);
  }

  @Override
  public int hashCode() {
    int result = from != null ? from.hashCode() : 0;
    result = 31 * result + (to != null ? to.hashCode() : 0);
    result = 31 * result + (newRange ? 1 : 0);
    result = 31 * result + (included ? 1 : 0);
    result = 31 * result + (fromSelector != null ? fromSelector.hashCode() : 0);
    result = 31 * result + (toSelector != null ? toSelector.hashCode() : 0);
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (fromSelector != null) {
      fromSelector.extractSubQueries(collector);
    }
    if (toSelector != null) {
      toSelector.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    if (fromSelector != null && fromSelector.refersToParent()) {
      return true;
    }
    return toSelector != null && toSelector.refersToParent();
  }

  /**
   * @param target
   * @param value
   * @param ctx
   * @return
   */
  public void setValue(Object target, Object value, CommandContext ctx) {
    if (target == null) {
      return;
    }
    if (target.getClass().isArray()) {
      setArrayValue(target, value, ctx);
    } else if (target instanceof List) {
      setValue((List) target, value, ctx);
    } else if (OMultiValue.isMultiValue(value)) {
      // TODO
    }
    // TODO

  }

  public void setValue(List target, Object value, CommandContext ctx) {
    int from = this.from == null ? 0 : this.from;
    int to = target.size() - 1;
    if (this.to != null) {
      to = this.to;
      if (!included) {
        to--;
      }
    }
    if (from > to) {
      target.clear();
      return;
    }
    for (int i = 0; i <= to; i++) {
      if (i < from && target.size() - 1 < i) {
        target.set(i, null);
      } else if (i >= from) {
        target.set(i, value);
      }
      // else leave untouched the existing element
    }
  }

  public void setValue(Set target, Object value, CommandContext ctx) {
    Set result = new LinkedHashSet<>();
    int from = this.from == null ? 0 : this.from;
    int to = target.size() - 1;
    if (this.to != null) {
      to = this.to;
      if (!included) {
        to--;
      }
    }
    if (from > to) {
      target.clear();
      return;
    }
    Iterator targetIterator = target.iterator();
    for (int i = 0; i <= to; i++) {
      Object next = null;
      if (targetIterator.hasNext()) {
        next = targetIterator.next();
      }
      if (i < from && target.size() - 1 < i) {
        result.add(null);
      } else if (i >= from) {
        result.add(value);
      } else {
        result.add(next);
      }
      target.clear();
      target.addAll(result);
    }
  }

  public void setValue(Map target, Object value, CommandContext ctx) {
    int from = this.from == null ? 0 : this.from;
    int to = this.to;
    if (!included) {
      to--;
    }
    if (from > to) {
      target.clear();
      return;
    }
    for (int i = from; i <= to; i++) {
      target.put(i, value);
    }
  }

  private void setArrayValue(Object target, Object value, CommandContext ctx) {

    int from = this.from == null ? 0 : this.from;
    int to = Array.getLength(target) - 1;
    if (this.to != null) {
      to = this.to;
      if (!included) {
        to--;
      }
    }
    if (from > to || from >= Array.getLength(target)) {
      return;
    }
    to = Math.min(to, Array.getLength(target) - 1);
    for (int i = from; i <= to; i++) {
      Array.set(target, i, value); // TODO type conversion?
    }
  }

  public void applyRemove(
      Object currentValue, YTResultInternal originalRecord, CommandContext ctx) {
    if (currentValue == null) {
      return;
    }
    Integer from = this.from;
    if (fromSelector != null) {
      from = fromSelector.getValue(originalRecord, null, ctx);
    }
    Integer to = this.to;
    if (toSelector != null) {
      to = toSelector.getValue(originalRecord, null, ctx);
    }
    if (from == null || to == null) {
      throw new YTCommandExecutionException(
          "Invalid range expression: " + this + " one of the elements is null");
    }
    if (included) {
      to++;
    }
    if (from < 0) {
      from = 0;
    }
    if (from >= to) {
      return;
    }
    int range = to - from;
    if (currentValue instanceof List list) {
      for (int i = 0; i < range; i++) {
        if (list.size() > from) {
          list.remove(from);
        } else {
          break;
        }
      }
    } else if (currentValue instanceof Set) {
      Iterator iter = ((Set) currentValue).iterator();
      int count = 0;
      while (iter.hasNext()) {
        iter.next();
        if (count >= from) {
          if (count < to) {
            iter.remove();
          } else {
            break;
          }
        }
        count++;
      }
    } else {
      throw new YTCommandExecutionException(
          "Trying to remove elements from "
              + currentValue
              + " ("
              + currentValue.getClass().getSimpleName()
              + ")");
    }
  }

  public YTResult serialize(YTDatabaseSessionInternal db) {
    YTResultInternal result = new YTResultInternal(db);
    result.setProperty("from", from);
    result.setProperty("to", to);
    result.setProperty("newRange", newRange);
    result.setProperty("included", included);

    if (fromSelector != null) {
      result.setProperty("fromSelector", fromSelector.serialize(db));
    }

    if (toSelector != null) {
      result.setProperty("toSelector", toSelector.serialize(db));
    }

    return result;
  }

  public void deserialize(YTResult fromResult) {
    from = fromResult.getProperty("from");
    to = fromResult.getProperty("to");
    newRange = fromResult.getProperty("newRange");
    included = fromResult.getProperty("included");

    if (fromResult.getProperty("fromSelector") != null) {
      fromSelector = new OArrayNumberSelector(-1);
      fromSelector.deserialize(fromResult.getProperty("fromSelector"));
    }
    if (fromResult.getProperty("toSelector") != null) {
      toSelector = new OArrayNumberSelector(-1);
      toSelector.deserialize(fromResult.getProperty("toSelector"));
    }
  }
}
/* JavaCC - OriginalChecksum=594a372e31fcbcd3ed962c2260e76468 (do not edit this line) */