/* Generated By:JJTree: Do not edit this line. SQLAlterSecurityPolicyStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityPolicyImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Map;

public class SQLAlterSecurityPolicyStatement extends SQLSimpleExecStatement {

  protected SQLIdentifier name;

  protected SQLBooleanExpression create;
  protected SQLBooleanExpression read;
  protected SQLBooleanExpression beforeUpdate;
  protected SQLBooleanExpression afterUpdate;
  protected SQLBooleanExpression delete;
  protected SQLBooleanExpression execute;

  protected boolean removeCreate = false;
  protected boolean removeRead = false;
  protected boolean removeBeforeUpdate = false;
  protected boolean removeAfterUpdate = false;
  protected boolean removeDelete = false;
  protected boolean removeExecute = false;

  public SQLAlterSecurityPolicyStatement(int id) {
    super(id);
  }

  public SQLAlterSecurityPolicyStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    var db = ctx.getDatabase();
    SecurityInternal security = db.getSharedContext().getSecurity();
    SecurityPolicyImpl policy = security.getSecurityPolicy(db, name.getStringValue());
    if (policy == null) {
      throw new CommandExecutionException("Cannot find security policy " + name.toString());
    }

    if (create != null) {
      policy.setCreateRule(db, create.toString());
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

    if (removeCreate) {
      policy.setCreateRule(db, null);
    }
    if (removeRead) {
      policy.setReadRule(db, null);
    }
    if (removeBeforeUpdate) {
      policy.setBeforeUpdateRule(db, null);
    }
    if (removeAfterUpdate) {
      policy.setAfterUpdateRule(db, null);
    }
    if (removeDelete) {
      policy.setDeleteRule(db, null);
    }
    if (removeExecute) {
      policy.setExecuteRule(db, null);
    }
    security.saveSecurityPolicy(db, policy);

    ResultInternal result = new ResultInternal(db);
    result.setProperty("operation", "alter security policy");
    result.setProperty("name", name.getStringValue());
    return ExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER SECURITY POLICY ");
    name.toString(params, builder);

    boolean firstSet = true;
    if (create != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("CREATE = (");
      create.toString(params, builder);
      builder.append(")");
      firstSet = false;
    }

    if (read != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("READ = (");
      read.toString(params, builder);
      builder.append(")");
      firstSet = false;
    }
    if (beforeUpdate != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("BEFORE UPDATE = (");
      beforeUpdate.toString(params, builder);
      builder.append(")");
      firstSet = false;
    }

    if (afterUpdate != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("AFTER UPDATE = (");
      afterUpdate.toString(params, builder);
      builder.append(")");
      firstSet = false;
    }
    if (delete != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("DELETE = (");
      delete.toString(params, builder);
      builder.append(")");
      firstSet = false;
    }
    if (execute != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("EXECUTE = (");
      execute.toString(params, builder);
      builder.append(")");
      firstSet = false;
    }

    boolean firstRemove = true;
    if (removeCreate) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("CREATE");
      firstRemove = false;
    }

    if (removeRead) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("READ");
      firstRemove = false;
    }
    if (removeBeforeUpdate) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("BEFORE UPDATE");
      firstRemove = false;
    }

    if (removeAfterUpdate) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("AFTER UPDATE");
      firstRemove = false;
    }
    if (removeDelete) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("DELETE");
      firstRemove = false;
    }
    if (removeExecute) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("EXECUTE");
      firstRemove = false;
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("ALTER SECURITY POLICY ");
    name.toGenericStatement(builder);

    boolean firstSet = true;
    if (create != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("CREATE = (");
      create.toGenericStatement(builder);
      builder.append(")");
      firstSet = false;
    }

    if (read != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("READ = (");
      read.toGenericStatement(builder);
      builder.append(")");
      firstSet = false;
    }
    if (beforeUpdate != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("BEFORE UPDATE = (");
      beforeUpdate.toGenericStatement(builder);
      builder.append(")");
      firstSet = false;
    }

    if (afterUpdate != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("AFTER UPDATE = (");
      afterUpdate.toGenericStatement(builder);
      builder.append(")");
      firstSet = false;
    }
    if (delete != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("DELETE = (");
      delete.toGenericStatement(builder);
      builder.append(")");
      firstSet = false;
    }
    if (execute != null) {
      if (firstSet) {
        builder.append(" SET ");
      } else {
        builder.append(", ");
      }
      builder.append("EXECUTE = (");
      execute.toGenericStatement(builder);
      builder.append(")");
      firstSet = false;
    }

    boolean firstRemove = true;
    if (removeCreate) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("CREATE");
      firstRemove = false;
    }

    if (removeRead) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("READ");
      firstRemove = false;
    }
    if (removeBeforeUpdate) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("BEFORE UPDATE");
      firstRemove = false;
    }

    if (removeAfterUpdate) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("AFTER UPDATE");
      firstRemove = false;
    }
    if (removeDelete) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("DELETE");
      firstRemove = false;
    }
    if (removeExecute) {
      if (firstRemove) {
        builder.append(" REMOVE ");
      } else {
        builder.append(", ");
      }
      builder.append("EXECUTE");
      firstRemove = false;
    }
  }
}
/* JavaCC - OriginalChecksum=849f284b6e4057d1f554daf024534423 (do not edit this line) */
