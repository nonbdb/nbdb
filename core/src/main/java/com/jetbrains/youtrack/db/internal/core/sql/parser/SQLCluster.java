/* Generated By:JJTree: Do not edit this line. SQLCluster.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.Map;
import java.util.Objects;

public class SQLCluster extends SimpleNode {

  protected String clusterName;
  protected Integer clusterNumber;

  public SQLCluster(String clusterName) {
    super(-1);
    this.clusterName = clusterName;
  }

  public SQLCluster(int id) {
    super(id);
  }

  public SQLCluster(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public String toString(String prefix) {
    return super.toString(prefix);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (clusterName != null) {
      builder.append("cluster:" + clusterName);
    } else {
      builder.append("cluster:" + clusterNumber);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (clusterName != null) {
      builder.append("cluster:" + clusterName);
    } else {
      builder.append("cluster:" + clusterNumber);
    }
  }

  public String getClusterName() {
    return clusterName;
  }

  public Integer getClusterNumber() {
    return clusterNumber;
  }

  public SQLCluster copy() {
    SQLCluster result = new SQLCluster(-1);
    result.clusterName = clusterName;
    result.clusterNumber = clusterNumber;
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

    SQLCluster oCluster = (SQLCluster) o;

    if (!Objects.equals(clusterName, oCluster.clusterName)) {
      return false;
    }
    return Objects.equals(clusterNumber, oCluster.clusterNumber);
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (clusterNumber != null ? clusterNumber.hashCode() : 0);
    return result;
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    result.setProperty("clusterName", clusterName);
    result.setProperty("clusterNumber", clusterNumber);
    return result;
  }

  public void deserialize(Result fromResult) {
    clusterName = fromResult.getProperty("clusterName");
    clusterNumber = fromResult.getProperty("clusterNumber");
  }
}
/* JavaCC - OriginalChecksum=d27abf009fe7db482fbcaac9d52ba192 (do not edit this line) */
