/* Generated By:JJTree: Do not edit this line. ODatabaseUserData.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ODatabaseUserData extends SimpleNode {

  protected OIdentifier name;
  protected OInputParameter nameParam;

  protected OIdentifier passwordIdentifier;
  protected String passwordString;
  protected OInputParameter passwordParam;

  protected List<OIdentifier> roles = new ArrayList<>();

  public ODatabaseUserData(int id) {
    super(id);
  }

  public ODatabaseUserData(OrientSql p, int id) {
    super(p, id);
  }

  public void addRole(OIdentifier role) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(role);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (name != null) {
      name.toString(params, builder);
    } else {
      nameParam.toString(params, builder);
    }

    builder.append(" IDENTIFIED BY ");

    if (passwordIdentifier != null) {
      passwordIdentifier.toString(params, builder);
    } else if (passwordString != null) {
      builder.append(passwordString);
    } else {
      passwordParam.toString(params, builder);
    }

    if (!roles.isEmpty()) {
      builder.append("ROLE [");
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
  public void toGenericStatement(StringBuilder builder) {
    if (name != null) {
      name.toGenericStatement(builder);
    } else {
      nameParam.toGenericStatement(builder);
    }

    builder.append(" IDENTIFIED BY ");

    if (passwordIdentifier != null) {
      passwordIdentifier.toGenericStatement(builder);
    } else if (passwordString != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else {
      passwordParam.toGenericStatement(builder);
    }

    if (!roles.isEmpty()) {
      builder.append("ROLE [");
      boolean first = true;
      for (OIdentifier role : roles) {
        if (!first) {
          builder.append(", ");
        }
        role.toGenericStatement(builder);
        first = false;
      }
      builder.append("]");
    }
  }

  @Override
  public ODatabaseUserData copy() {
    ODatabaseUserData result = new ODatabaseUserData(-1);
    if (name != null) {
      result.name = name.copy();
    }
    if (nameParam != null) {
      result.nameParam = nameParam.copy();
    }

    if (passwordIdentifier != null) {
      result.passwordIdentifier = passwordIdentifier.copy();
    }
    result.passwordString = this.passwordString;
    if (passwordParam != null) {
      result.passwordParam = passwordParam.copy();
    }

    for (OIdentifier role : roles) {
      result.roles.add(role.copy());
    }

    return result;
  }

  public void executeCreate(YTDatabaseSessionInternal db, CommandContext parentCtx) {
    BasicCommandContext ctx = new BasicCommandContext();
    ctx.setInputParameters(parentCtx.getInputParameters());
    ctx.setDatabase(db);
    OCreateUserStatement stm = new OCreateUserStatement(-1);
    if (name != null) {
      stm.name = name.copy();
    } else {
      stm.name = new OIdentifier("" + nameParam.getValue(ctx.getInputParameters()));
    }

    if (passwordIdentifier != null) {
      stm.passwordIdentifier = passwordIdentifier.copy();
    } else if (passwordString != null) {
      stm.passwordString = passwordString;
    } else {
      stm.passwordParam = passwordParam.copy();
    }

    for (OIdentifier role : roles) {
      stm.roles.add(role.copy());
    }

    stm.executeSimple(ctx).close(ctx);
  }
}
/* JavaCC - OriginalChecksum=15bf3e16120859f9ab9f311935570d5d (do not edit this line) */