/* Generated By:JJTree: Do not edit this line. OTimeout.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import java.util.Map;
import java.util.Objects;

public class OTimeout extends SimpleNode {

  public static final String RETURN = "RETURN";
  public static final String EXCEPTION = "EXCEPTION";

  protected Number val;
  protected String failureStrategy;

  public OTimeout(int id) {
    super(id);
  }

  public OTimeout(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(" TIMEOUT " + val);
    if (failureStrategy != null) {
      builder.append(" ");
      builder.append(failureStrategy);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append(" TIMEOUT " + PARAMETER_PLACEHOLDER);
    if (failureStrategy != null) {
      builder.append(" ");
      builder.append(failureStrategy);
    }
  }

  public OTimeout copy() {
    OTimeout result = new OTimeout(-1);
    result.val = val;
    result.failureStrategy = failureStrategy;
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

    OTimeout timeout = (OTimeout) o;

    if (!Objects.equals(val, timeout.val)) {
      return false;
    }
    return Objects.equals(failureStrategy, timeout.failureStrategy);
  }

  @Override
  public int hashCode() {
    int result = val != null ? val.hashCode() : 0;
    result = 31 * result + (failureStrategy != null ? failureStrategy.hashCode() : 0);
    return result;
  }

  public Number getVal() {
    return val;
  }

  public String getFailureStrategy() {
    return failureStrategy;
  }

  public YTResult serialize(YTDatabaseSessionInternal db) {
    YTResultInternal result = new YTResultInternal(db);
    result.setProperty("val", val);
    result.setProperty("failureStrategy", failureStrategy);
    return result;
  }

  public void deserialize(YTResult fromResult) {
    val = fromResult.getProperty("val");
    failureStrategy = fromResult.getProperty("failureStrategy");
  }

  public void setVal(Number val) {
    this.val = val;
  }
}
/* JavaCC - OriginalChecksum=fef7f5d488f7fca1b6ad0b70c6841931 (do not edit this line) */
