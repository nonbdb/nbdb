/* Generated By:JJTree: Do not edit this line. OExpansionItem.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultInternal;
import java.util.Map;
import java.util.Objects;

public class ONestedProjectionItem extends SimpleNode {

  protected boolean exclude = false;

  protected boolean star = false;

  protected OExpression expression;
  protected boolean rightWildcard = false;

  protected ONestedProjection expansion;
  protected OIdentifier alias;

  public ONestedProjectionItem(int id) {
    super(id);
  }

  public ONestedProjectionItem(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public ONestedProjectionItem copy() {
    ONestedProjectionItem result = new ONestedProjectionItem(-1);
    result.exclude = exclude;
    result.star = star;
    result.expression = expression == null ? null : expression.copy();
    result.rightWildcard = rightWildcard;
    result.expansion = expansion == null ? null : expansion.copy();
    result.alias = alias == null ? null : alias.copy();
    return result;
  }

  /**
   * given a property name, calculates if this property name matches this nested projection item,
   * eg.
   *
   * <ul>
   *   <li>this is a *, so it matches any property name
   *   <li>the field name for this projection item is the same as the input property name
   *   <li>this item has a wildcard and the partial field is a prefix of the input property name
   * </ul>
   *
   * @param propertyName
   * @return
   */
  public boolean matches(String propertyName) {
    if (star) {
      return true;
    }
    if (expression != null) {
      String fieldString = expression.getDefaultAlias().getStringValue();
      if (fieldString.equals(propertyName)) {
        return true;
      }
      return rightWildcard && propertyName.startsWith(fieldString);
    }
    return false;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (exclude) {
      builder.append("!");
    }
    if (star) {
      builder.append("*");
    }
    if (expression != null) {
      expression.toString(params, builder);
      if (rightWildcard) {
        builder.append("*");
      }
    }
    if (expansion != null) {
      expansion.toString(params, builder);
    }
    if (alias != null) {
      builder.append(" AS ");
      alias.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    if (exclude) {
      builder.append("!");
    }
    if (star) {
      builder.append("*");
    }
    if (expression != null) {
      expression.toGenericStatement(builder);
      if (rightWildcard) {
        builder.append("*");
      }
    }
    if (expansion != null) {
      expansion.toGenericStatement(builder);
    }
    if (alias != null) {
      builder.append(" AS ");
      alias.toGenericStatement(builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ONestedProjectionItem that = (ONestedProjectionItem) o;

    if (exclude != that.exclude) {
      return false;
    }
    if (star != that.star) {
      return false;
    }
    if (rightWildcard != that.rightWildcard) {
      return false;
    }
    if (!Objects.equals(expression, that.expression)) {
      return false;
    }
    if (!Objects.equals(expansion, that.expansion)) {
      return false;
    }
    return Objects.equals(alias, that.alias);
  }

  @Override
  public int hashCode() {
    int result = (exclude ? 1 : 0);
    result = 31 * result + (star ? 1 : 0);
    result = 31 * result + (expression != null ? expression.hashCode() : 0);
    result = 31 * result + (rightWildcard ? 1 : 0);
    result = 31 * result + (expansion != null ? expansion.hashCode() : 0);
    result = 31 * result + (alias != null ? alias.hashCode() : 0);
    return result;
  }

  public Object expand(
      OExpression expression, String name, Object value, CommandContext ctx, int recursion) {
    return expansion.apply(expression, value, ctx);
  }

  public YTResult serialize(YTDatabaseSessionInternal db) {
    YTResultInternal result = new YTResultInternal(db);
    result.setProperty("exclude", exclude);
    result.setProperty("star", star);
    if (expression != null) {
      result.setProperty("expression", expression.serialize(db));
    }
    result.setProperty("rightWildcard", rightWildcard);
    if (expansion != null) {
      result.setProperty("expansion", expansion.serialize(db));
    }
    if (alias != null) {
      result.setProperty("alias", alias.serialize(db));
    }
    return result;
  }

  public void deserialize(YTResult fromResult) {
    exclude = fromResult.getProperty("exclude");
    star = fromResult.getProperty("star");
    if (fromResult.getProperty("field") != null) {
      expression = new OExpression(-1);
      expression.deserialize(fromResult.getProperty("expression"));
    }
    rightWildcard = fromResult.getProperty("rightWildcard");
    if (fromResult.getProperty("expansion") != null) {
      expansion = new ONestedProjection(-1);
      expansion.deserialize(fromResult.getProperty("expansion"));
    }
    if (fromResult.getProperty("alias") != null) {
      alias = OIdentifier.deserialize(fromResult.getProperty("alias"));
    }
  }
}
/* JavaCC - OriginalChecksum=606b3fe37ff952934e3e2e3daa9915f2 (do not edit this line) */
