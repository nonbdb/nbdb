/* Generated By:JJTree: Do not edit this line. SQLTruncateClassStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SQLTruncateClassStatement extends DDLStatement {

  protected SQLIdentifier className;
  protected boolean polymorphic = false;
  protected boolean unsafe = false;

  public SQLTruncateClassStatement(int id) {
    super(id);
  }

  public SQLTruncateClassStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeDDL(CommandContext ctx) {
    DatabaseSessionInternal db = ctx.getDatabase();
    var schema = db.getMetadata().getSchemaInternal();
    SchemaClassInternal clazz = schema.getClassInternal(className.getStringValue());
    if (clazz == null) {
      throw new CommandExecutionException("Schema Class not found: " + className);
    }

    final long recs = clazz.count(ctx.getDatabase(), polymorphic);
    if (recs > 0 && !unsafe) {
      if (clazz.isSubClassOf("V")) {
        throw new CommandExecutionException(
            "'TRUNCATE CLASS' command cannot be used on not empty vertex classes. Apply the"
                + " 'UNSAFE' keyword to force it (at your own risk)");
      } else if (clazz.isSubClassOf("E")) {
        throw new CommandExecutionException(
            "'TRUNCATE CLASS' command cannot be used on not empty edge classes. Apply the 'UNSAFE'"
                + " keyword to force it (at your own risk)");
      }
    }

    List<Result> rs = new ArrayList<>();
    Collection<SchemaClass> subclasses = clazz.getAllSubclasses();
    if (polymorphic && !unsafe) { // for multiple inheritance
      for (SchemaClass subclass : subclasses) {
        long subclassRecs = clazz.count(db);
        if (subclassRecs > 0) {
          if (subclass.isSubClassOf("V")) {
            throw new CommandExecutionException(
                "'TRUNCATE CLASS' command cannot be used on not empty vertex classes ("
                    + subclass.getName()
                    + "). Apply the 'UNSAFE' keyword to force it (at your own risk)");
          } else if (subclass.isSubClassOf("E")) {
            throw new CommandExecutionException(
                "'TRUNCATE CLASS' command cannot be used on not empty edge classes ("
                    + subclass.getName()
                    + "). Apply the 'UNSAFE' keyword to force it (at your own risk)");
          }
        }
      }
    }

    long count = db.truncateClass(clazz.getName(), false);
    ResultInternal result = new ResultInternal(db);
    result.setProperty("operation", "truncate class");
    result.setProperty("className", className.getStringValue());
    result.setProperty("count", count);
    rs.add(result);
    if (polymorphic) {
      for (SchemaClass subclass : subclasses) {
        count = db.truncateClass(subclass.getName(), false);
        result = new ResultInternal(db);
        result.setProperty("operation", "truncate class");
        result.setProperty("className", className.getStringValue());
        result.setProperty("count", count);
        rs.add(result);
      }
    }

    return ExecutionStream.resultIterator(rs.iterator());
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("TRUNCATE CLASS ");
    className.toString(params, builder);
    if (polymorphic) {
      builder.append(" POLYMORPHIC");
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("TRUNCATE CLASS ");
    className.toGenericStatement(builder);
    if (polymorphic) {
      builder.append(" POLYMORPHIC");
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  @Override
  public SQLTruncateClassStatement copy() {
    SQLTruncateClassStatement result = new SQLTruncateClassStatement(-1);
    result.className = className == null ? null : className.copy();
    result.polymorphic = polymorphic;
    result.unsafe = unsafe;
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

    SQLTruncateClassStatement that = (SQLTruncateClassStatement) o;

    if (polymorphic != that.polymorphic) {
      return false;
    }
    if (unsafe != that.unsafe) {
      return false;
    }
    return Objects.equals(className, that.className);
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (polymorphic ? 1 : 0);
    result = 31 * result + (unsafe ? 1 : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=301f993f6ba2893cb30c8f189674b974 (do not edit this line) */
