/* Generated By:JJTree: Do not edit this line. ORid.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import java.util.Map;

public class ORid extends SimpleNode {
  protected OInteger cluster;
  protected OInteger position;

  protected OExpression expression;
  protected boolean legacy;

  public ORid(int id) {
    super(id);
  }

  public ORid(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public String toString(String prefix) {
    return "#" + cluster.getValue() + ":" + position.getValue();
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (legacy || (expression == null && cluster != null && position != null)) {
      builder.append("#" + cluster.getValue() + ":" + position.getValue());
    } else {
      builder.append("{\"@rid\":");
      expression.toString(params, builder);
      builder.append("}");
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (legacy || (expression == null && cluster != null && position != null)) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else {
      builder.append("{\"@rid\":");
      expression.toGenericStatement(builder);
      builder.append("}");
    }
  }

  public ORecordId toRecordId(OResult target, OCommandContext ctx) {
    if (legacy || (expression == null && cluster != null && position != null)) {
      return new ORecordId(cluster.value.intValue(), position.value.longValue());
    } else {
      Object result = expression.execute(target, ctx);
      if (result == null) {
        return null;
      }
      if (result instanceof OIdentifiable) {
        return (ORecordId) ((OIdentifiable) result).getIdentity();
      }
      if (result instanceof String) {
        return new ORecordId((String) result);
      }
      return null;
    }
  }

  public ORecordId toRecordId(OIdentifiable target, OCommandContext ctx) {
    if (legacy || (expression == null && cluster != null && position != null)) {
      return new ORecordId(cluster.value.intValue(), position.value.longValue());
    } else {
      Object result = expression.execute(target, ctx);
      if (result == null) {
        return null;
      }
      if (result instanceof OIdentifiable) {
        return (ORecordId) ((OIdentifiable) result).getIdentity();
      }
      if (result instanceof String) {
        return new ORecordId((String) result);
      }
      return null;
    }
  }

  public ORid copy() {
    ORid result = new ORid(-1);
    result.cluster = cluster == null ? null : cluster.copy();
    result.position = position == null ? null : position.copy();
    result.expression = expression == null ? null : expression.copy();
    result.legacy = legacy;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ORid oRid = (ORid) o;

    if (cluster != null ? !cluster.equals(oRid.cluster) : oRid.cluster != null) return false;
    if (position != null ? !position.equals(oRid.position) : oRid.position != null) return false;
    if (expression != null ? !expression.equals(oRid.expression) : oRid.expression != null)
      return false;
    if (legacy != oRid.legacy) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = cluster != null ? cluster.hashCode() : 0;
    result = 31 * result + (position != null ? position.hashCode() : 0);
    result = 31 * result + (expression != null ? expression.hashCode() : 0);
    return result;
  }

  public void setCluster(OInteger cluster) {
    this.cluster = cluster;
  }

  public void setPosition(OInteger position) {
    this.position = position;
  }

  public void setLegacy(boolean b) {
    this.legacy = b;
  }

  public OInteger getCluster() {
    if (expression != null) {
      ORecordId rid = toRecordId((OResult) null, new OBasicCommandContext());
      if (rid != null) {
        OInteger result = new OInteger(-1);
        result.setValue(rid.getClusterId());
        return result;
      }
    }
    return cluster;
  }

  public OInteger getPosition() {
    if (expression != null) {
      ORecordId rid = toRecordId((OResult) null, new OBasicCommandContext());
      if (rid != null) {
        OInteger result = new OInteger(-1);
        result.setValue(rid.getClusterPosition());
        return result;
      }
    }
    return position;
  }

  public OResult serialize() {
    OResultInternal result = new OResultInternal();
    if (cluster != null) {
      result.setProperty("cluster", cluster.serialize());
    }
    if (position != null) {
      result.setProperty("position", position.serialize());
    }
    if (expression != null) {
      result.setProperty("expression", expression.serialize());
    }
    result.setProperty("legacy", legacy);
    return result;
  }

  public void deserialize(OResult fromResult) {
    if (fromResult.getProperty("cluster") != null) {
      cluster = new OInteger(-1);
      cluster.deserialize(fromResult.getProperty("cluster"));
    }
    if (fromResult.getProperty("position") != null) {
      position = new OInteger(-1);
      position.deserialize(fromResult.getProperty("position"));
    }
    if (fromResult.getProperty("expression") != null) {
      expression = new OExpression(-1);
      expression.deserialize(fromResult.getProperty("expression"));
    }
    legacy = fromResult.getProperty("legacy");
  }
}
/* JavaCC - OriginalChecksum=c2c6d67d7722e29212e438574698d7cd (do not edit this line) */
