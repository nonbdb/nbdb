/* Generated By:JJTree: Do not edit this line. SQLAlterSystemRoleStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.ServerCommandContext;
import com.jetbrains.youtrack.db.internal.core.db.SystemDatabase;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityPolicyImpl;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLAlterSystemRoleStatement extends SQLSimpleExecServerStatement {

  static class Op {

    protected static int TYPE_ADD = 0;
    protected static int TYPE_REMOVE = 1;

    Op(int type, SQLSecurityResourceSegment resource, SQLIdentifier policyName) {
      this.type = type;
      this.resource = resource;
      this.policyName = policyName;
    }

    protected final int type;
    protected final SQLSecurityResourceSegment resource;
    protected final SQLIdentifier policyName;
  }

  protected SQLIdentifier name;
  protected List<Op> operations = new ArrayList<>();

  public SQLAlterSystemRoleStatement(int id) {
    super(id);
  }

  public SQLAlterSystemRoleStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void addOperation(Op operation) {
    this.operations.add(operation);
  }

  @Override
  public ExecutionStream executeSimple(ServerCommandContext ctx) {

    SystemDatabase systemDb = ctx.getServer().getSystemDatabase();

    return systemDb.executeWithDB(
        (db) -> {
          List<Result> rs = new ArrayList<>();

          SecurityInternal security = db.getSharedContext().getSecurity();

          Role role = db.getMetadata().getSecurity().getRole(name.getStringValue());
          if (role == null) {
            throw new CommandExecutionException("role not found: " + name.getStringValue());
          }
          for (Op op : operations) {
            ResultInternal result = new ResultInternal(db);
            result.setProperty("operation", "alter system role");
            result.setProperty("name", name.getStringValue());
            result.setProperty("resource", op.resource.toString());
            if (op.type == Op.TYPE_ADD) {
              SecurityPolicyImpl policy =
                  security.getSecurityPolicy(db, op.policyName.getStringValue());
              result.setProperty("operation", "ADD POLICY");
              result.setProperty("policyName", op.policyName.getStringValue());
              try {
                security.setSecurityPolicy(db, role, op.resource.toString(), policy);
                result.setProperty("result", "OK");
              } catch (Exception e) {
                result.setProperty("result", "failure");
              }
            } else {
              result.setProperty("operation", "REMOVE POLICY");
              try {
                security.removeSecurityPolicy(db, role, op.resource.toString());
                result.setProperty("result", "OK");
              } catch (Exception e) {
                result.setProperty("result", "failure");
              }
            }
            rs.add(result);
          }
          return ExecutionStream.resultIterator(rs.iterator());
        });
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER SYSTEM ROLE ");
    name.toString(params, builder);

    for (Op operation : operations) {
      if (operation.type == SQLAlterRoleStatement.Op.TYPE_ADD) {
        builder.append(" SET POLICY ");
        operation.policyName.toString(params, builder);
        builder.append(" ON ");
        operation.resource.toString(params, builder);
      } else {
        builder.append(" REMOVE POLICY ON ");
        operation.resource.toString(params, builder);
      }
    }
  }
}
/* JavaCC - OriginalChecksum=50b6859b3a4d19767a526b979554bbdb (do not edit this line) */
