/* Generated By:JJTree: Do not edit this line. ODropViewStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTView;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;
import java.util.Objects;

public class ODropViewStatement extends ODDLStatement {

  public OIdentifier name;
  public boolean ifExists = false;

  public ODropViewStatement(int id) {
    super(id);
  }

  public ODropViewStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {
    var db = ctx.getDatabase();
    YTSchema schema = db.getMetadata().getSchema();
    YTView view = schema.getView(name.getStringValue());
    if (view == null) {
      if (ifExists) {
        return OExecutionStream.empty();
      }
      throw new YTCommandExecutionException("View " + name.getStringValue() + " does not exist");
    }

    if (view.count(db) > 0) {
      // no need for this probably, but perhaps in the future...
      if (view.isVertexType()) {
        throw new YTCommandExecutionException(
            "'DROP VIEW' command cannot drop view '"
                + name.getStringValue()
                + "' because it contains Vertices. Use 'DELETE VERTEX' command first to avoid"
                + " broken edges in a database, or apply the 'UNSAFE' keyword to force it");
      } else if (view.isEdgeType()) {
        throw new YTCommandExecutionException(
            "'DROP VIEW' command cannot drop view '"
                + name.getStringValue()
                + "' because it contains Edges. Use 'DELETE EDGE' command first to avoid broken"
                + " vertices in a database, or apply the 'UNSAFE' keyword to force it");
      }
    }

    schema.dropView(name.getStringValue());

    YTResultInternal result = new YTResultInternal(db);
    result.setProperty("operation", "drop view");
    result.setProperty("viewName", name.getStringValue());
    return OExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DROP VIEW ");
    name.toString(params, builder);
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("DROP VIEW ");
    name.toGenericStatement(builder);
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public ODropViewStatement copy() {
    ODropViewStatement result = new ODropViewStatement(-1);
    result.name = name == null ? null : name.copy();
    result.ifExists = ifExists;
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

    ODropViewStatement that = (ODropViewStatement) o;

    if (ifExists != that.ifExists) {
      return false;
    }
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    return result;
  }
}
/* JavaCC - OriginalChecksum=743b24c40e2ea4f55408463b9bc0b5b9 (do not edit this line) */
