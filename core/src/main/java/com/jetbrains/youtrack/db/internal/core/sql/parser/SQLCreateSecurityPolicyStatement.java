/* Generated By:JJTree: Do not edit this line. SQLCreateSecurityPolicyStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityPolicyImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Map;
import java.util.Objects;

public class SQLCreateSecurityPolicyStatement extends SQLSimpleExecStatement {

  protected SQLIdentifier name;

  protected SQLBooleanExpression create;
  protected SQLBooleanExpression read;
  protected SQLBooleanExpression beforeUpdate;
  protected SQLBooleanExpression afterUpdate;
  protected SQLBooleanExpression delete;
  protected SQLBooleanExpression execute;

  public SQLCreateSecurityPolicyStatement(int id) {
    super(id);
  }

  public SQLCreateSecurityPolicyStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    var db = ctx.getDatabase();
    SecurityInternal security = db.getSharedContext().getSecurity();
    SecurityPolicyImpl policy = security.createSecurityPolicy(db, name.getStringValue());
    policy.setActive(ctx.getDatabase(), true);
    if (create != null) {
      policy.setCreateRule(ctx.getDatabase(), create.toString());
    }
    if (read != null) {
      policy.setReadRule(db, read.toString());
    }
    if (beforeUpdate != null) {
      policy.setBeforeUpdateRule(db, beforeUpdate.toString());
    }
    if (afterUpdate != null) {
      policy.setAfterUpdateRule(db, afterUpdate.toString());
    }
    if (delete != null) {
      policy.setDeleteRule(db, delete.toString());
    }
    if (execute != null) {
      policy.setExecuteRule(db, execute.toString());
    }

    security.saveSecurityPolicy(db, policy);

    ResultInternal result = new ResultInternal(db);
    result.setProperty("operation", "create security policy");
    result.setProperty("name", name.getStringValue());
    return ExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CREATE SECURITY POLICY ");
    name.toString(params, builder);

    boolean first = true;
    if (create != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("CREATE = (");
      create.toString(params, builder);
      builder.append(")");
      first = false;
    }

    if (read != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("READ = (");
      read.toString(params, builder);
      builder.append(")");
      first = false;
    }
    if (beforeUpdate != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("BEFORE UPDATE = (");
      beforeUpdate.toString(params, builder);
      builder.append(")");
      first = false;
    }

    if (afterUpdate != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("AFTER UPDATE = (");
      afterUpdate.toString(params, builder);
      builder.append(")");
      first = false;
    }
    if (delete != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("DELETE = (");
      delete.toString(params, builder);
      builder.append(")");
      first = false;
    }
    if (execute != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("EXECUTE = (");
      execute.toString(params, builder);
      builder.append(")");
      first = false;
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("CREATE SECURITY POLICY ");
    name.toGenericStatement(builder);

    boolean first = true;
    if (create != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("CREATE = (");
      create.toGenericStatement(builder);
      builder.append(")");
      first = false;
    }

    if (read != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("READ = (");
      read.toGenericStatement(builder);
      builder.append(")");
      first = false;
    }
    if (beforeUpdate != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("BEFORE UPDATE = (");
      beforeUpdate.toGenericStatement(builder);
      builder.append(")");
      first = false;
    }

    if (afterUpdate != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("AFTER UPDATE = (");
      afterUpdate.toGenericStatement(builder);
      builder.append(")");
      first = false;
    }
    if (delete != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("DELETE = (");
      delete.toGenericStatement(builder);
      builder.append(")");
      first = false;
    }
    if (execute != null) {
      if (first) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("EXECUTE = (");
      execute.toGenericStatement(builder);
      builder.append(")");
      first = false;
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
    SQLCreateSecurityPolicyStatement that = (SQLCreateSecurityPolicyStatement) o;
    return Objects.equals(name, that.name)
        && Objects.equals(create, that.create)
        && Objects.equals(read, that.read)
        && Objects.equals(beforeUpdate, that.beforeUpdate)
        && Objects.equals(afterUpdate, that.afterUpdate)
        && Objects.equals(delete, that.delete)
        && Objects.equals(execute, that.execute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, create, read, beforeUpdate, afterUpdate, delete, execute);
  }

  @Override
  public SQLStatement copy() {
    SQLCreateSecurityPolicyStatement result = new SQLCreateSecurityPolicyStatement(-1);
    result.name = name.copy();
    result.create = this.create == null ? null : this.create.copy();
    result.read = this.read == null ? null : this.read.copy();
    result.beforeUpdate = this.beforeUpdate == null ? null : this.beforeUpdate.copy();
    result.afterUpdate = this.afterUpdate == null ? null : this.afterUpdate.copy();
    result.delete = this.delete == null ? null : this.delete.copy();
    result.execute = this.execute == null ? null : this.execute.copy();
    return result;
  }

  @Override
  public boolean executinPlanCanBeCached(DatabaseSessionInternal session) {
    if (create != null && !create.isCacheable(session)) {
      return false;
    }
    if (read != null && !read.isCacheable(session)) {
      return false;
    }
    if (beforeUpdate != null && !beforeUpdate.isCacheable(session)) {
      return false;
    }
    if (afterUpdate != null && !afterUpdate.isCacheable(session)) {
      return false;
    }
    if (delete != null && !delete.isCacheable(session)) {
      return false;
    }
    return execute == null || execute.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=f41480f6734998f6eac27242db146d09 (do not edit this line) */
