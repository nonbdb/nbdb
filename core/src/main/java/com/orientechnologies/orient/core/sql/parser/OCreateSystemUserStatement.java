/* Generated By:JJTree: Do not edit this line. OCreateSystemUserStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OServerCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.OSystemDatabase;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class OCreateSystemUserStatement extends OSimpleExecServerStatement {

  protected static final String USER_FIELD_NAME = "name";
  private static final String USER_FIELD_PASSWORD = "password";
  private static final String USER_FIELD_STATUS = "status";
  private static final String USER_FIELD_ROLES = "roles";

  private static final String DEFAULT_STATUS = "ACTIVE";
  private static final String DEFAULT_ROLE = "writer";
  private static final String ROLE_CLASS = "ORole";
  private static final String ROLE_FIELD_NAME = "name";

  public OCreateSystemUserStatement(int id) {
    super(id);
  }

  public OCreateSystemUserStatement(OrientSql p, int id) {
    super(p, id);
  }

  protected OIdentifier name;
  protected OIdentifier passwordIdentifier;
  protected String passwordString;
  protected OInputParameter passwordParam;

  protected List<OIdentifier> roles = new ArrayList<>();

  @Override
  public OExecutionStream executeSimple(OServerCommandContext ctx) {

    OSystemDatabase systemDb = ctx.getServer().getSystemDatabase();

    return systemDb.executeWithDB(
        (db) -> {
          List<Object> params = new ArrayList<>();
          // INSERT INTO OUser SET
          StringBuilder sb = new StringBuilder();
          sb.append("INSERT INTO OUser SET ");

          sb.append(USER_FIELD_NAME);
          sb.append("=?");
          params.add(this.name.getStringValue());

          // pass=<pass>
          sb.append(',');
          sb.append(USER_FIELD_PASSWORD);
          sb.append("=");
          if (passwordString != null) {
            sb.append(passwordString);
          } else if (passwordIdentifier != null) {
            sb.append("?");
            params.add(passwordIdentifier.getStringValue());
          } else {
            sb.append("?");
            params.add(passwordParam.getValue(ctx.getInputParameters()));
          }

          // status=ACTIVE
          sb.append(',');
          sb.append(USER_FIELD_STATUS);
          sb.append("='");
          sb.append(DEFAULT_STATUS);
          sb.append("'");

          // role=(select from ORole where name in [<input_role || 'writer'>)]
          List<OIdentifier> roles = new ArrayList<>();
          roles.addAll(this.roles);
          if (roles.size() == 0) {
            roles.add(new OIdentifier(DEFAULT_ROLE));
          }

          sb.append(',');
          sb.append(USER_FIELD_ROLES);
          sb.append("=(SELECT FROM ");
          sb.append(ROLE_CLASS);
          sb.append(" WHERE ");
          sb.append(ROLE_FIELD_NAME);
          sb.append(" IN [");
          OSecurity security = ((ODatabaseDocumentInternal) db).getMetadata().getSecurity();
          for (int i = 0; i < this.roles.size(); ++i) {
            String roleName = this.roles.get(i).getStringValue();
            ORole role = security.getRole(roleName);
            if (role == null) {
              throw new OCommandExecutionException(
                  "Cannot create user " + this.name + ": role " + roleName + " does not exist");
            }
            if (i > 0) {
              sb.append(", ");
            }

            if (roleName.startsWith("'") || roleName.startsWith("\"")) {
              sb.append(roleName);
            } else {
              sb.append("'");
              sb.append(roleName);
              sb.append("'");
            }
          }
          sb.append("])");
          Stream<OResult> stream =
              db.computeInTx(() -> db.command(sb.toString(), params.toArray()).stream());
          return OExecutionStream.resultIterator(stream.iterator())
              .onClose((context) -> stream.close());
        });
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CREATE SYSTEM USER ");
    name.toString(params, builder);
    builder.append(" IDENTIFIED BY ");
    if (passwordIdentifier != null) {
      passwordIdentifier.toString(params, builder);
    } else if (passwordString != null) {
      builder.append(passwordString);
    } else {
      passwordParam.toString(params, builder);
    }
    if (!roles.isEmpty()) {
      builder.append(" ROLE [");
      boolean first = true;
      for (OIdentifier role : roles) {
        if (!first) {
          builder.append(", ");
        }
        role.toString(params, builder);
        first = false;
      }
      builder.append("]");
    }
  }

  @Override
  public OCreateSystemUserStatement copy() {
    OCreateSystemUserStatement result = new OCreateSystemUserStatement(-1);
    result.name = name == null ? null : name.copy();
    result.passwordIdentifier = passwordIdentifier == null ? null : passwordIdentifier.copy();
    result.passwordString = passwordString;
    result.passwordParam = passwordParam == null ? null : passwordParam.copy();
    roles.forEach(x -> result.roles.add(x.copy()));
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
    OCreateSystemUserStatement that = (OCreateSystemUserStatement) o;
    return Objects.equals(name, that.name)
        && Objects.equals(passwordIdentifier, that.passwordIdentifier)
        && Objects.equals(passwordString, that.passwordString)
        && Objects.equals(passwordParam, that.passwordParam)
        && Objects.equals(roles, that.roles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, passwordIdentifier, passwordString, passwordParam, roles);
  }

  public void addRole(OIdentifier identifer) {
    this.roles.add(identifer);
  }
}
/* JavaCC - OriginalChecksum=7d1598a31cf500e4b388bb961049e27f (do not edit this line) */
