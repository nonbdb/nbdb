/* Generated By:JJTree: Do not edit this line. OCreateDatabaseStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.orient.core.command.OServerCommandContext;
import com.orientechnologies.orient.core.config.YTGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.YouTrackDBConfig;
import com.orientechnologies.orient.core.db.YouTrackDBConfigBuilder;
import com.orientechnologies.orient.core.db.YouTrackDBInternal;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OCreateDatabaseStatement extends OSimpleExecServerStatement {

  protected OIdentifier name;
  protected OInputParameter nameParam;
  protected OIdentifier type;
  protected boolean ifNotExists = false;
  protected OJson config;

  private List<ODatabaseUserData> users = new ArrayList<>();

  public OCreateDatabaseStatement(int id) {
    super(id);
  }

  public OCreateDatabaseStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void addUser(ODatabaseUserData user) {
    if (this.users == null) {
      this.users = new ArrayList<>();
    }
    this.users.add(user);
  }

  @Override
  public OExecutionStream executeSimple(OServerCommandContext ctx) {
    YouTrackDBInternal server = ctx.getServer();
    YTResultInternal result = new YTResultInternal(ctx.getDatabase());
    result.setProperty("operation", "create database");
    String dbName =
        name != null
            ? name.getStringValue()
            : String.valueOf(nameParam.getValue(ctx.getInputParameters()));
    result.setProperty("name", dbName);

    ODatabaseType dbType;
    try {

      dbType = ODatabaseType.valueOf(type.getStringValue().toUpperCase(Locale.ENGLISH));
    } catch (IllegalArgumentException ex) {
      throw new YTCommandExecutionException("Invalid db type: " + type.getStringValue());
    }
    if (ifNotExists && server.exists(dbName, null, null)) {
      result.setProperty("created", false);
      result.setProperty("existing", true);
    } else {
      try {
        YouTrackDBConfigBuilder configBuilder = YouTrackDBConfig.builder();

        if (config != null) {
          configBuilder = mapOrientDBConfig(this.config, ctx, configBuilder);
        }

        if (!users.isEmpty()) {
          configBuilder = configBuilder.addConfig(YTGlobalConfiguration.CREATE_DEFAULT_USERS,
              false);
        }

        server.create(
            dbName,
            null,
            null,
            dbType,
            configBuilder.build(),
            (session) -> {
              if (!users.isEmpty()) {
                session.executeInTx(
                    () -> {
                      for (ODatabaseUserData user : users) {
                        user.executeCreate(session, ctx);
                      }
                    });
              }
              return null;
            });
        result.setProperty("created", true);
      } catch (Exception e) {
        throw YTException.wrapException(
            new YTCommandExecutionException(
                "Could not create database " + type.getStringValue() + ":" + e.getMessage()),
            e);
      }
    }

    return OExecutionStream.singleton(result);
  }

  private YouTrackDBConfigBuilder mapOrientDBConfig(
      OJson config, OServerCommandContext ctx, YouTrackDBConfigBuilder builder) {
    Map<String, Object> configMap = config.toMap(new YTResultInternal(ctx.getDatabase()), ctx);

    Object globalConfig = configMap.get("config");
    if (globalConfig != null && globalConfig instanceof Map) {
      ((Map<String, Object>) globalConfig)
          .entrySet().stream()
          .filter(x -> YTGlobalConfiguration.findByKey(x.getKey()) != null)
          .forEach(
              x -> builder.addConfig(YTGlobalConfiguration.findByKey(x.getKey()), x.getValue()));
    }

    return builder;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CREATE DATABASE ");
    if (name != null) {
      name.toString(params, builder);
    } else {
      nameParam.toString(params, builder);
    }
    builder.append(" ");
    type.toString(params, builder);
    if (ifNotExists) {
      builder.append(" IF NOT EXISTS");
    }

    if (!users.isEmpty()) {
      builder.append(" USERS (");
      boolean first = true;
      for (ODatabaseUserData user : users) {
        if (!first) {
          builder.append(", ");
        }
        user.toString(params, builder);
        first = false;
      }
      builder.append(")");
    }
    if (config != null) {
      builder.append(" ");
      config.toString(params, builder);
    }
  }
}
/* JavaCC - OriginalChecksum=99888a0f8bb929dce0904816cd51fefe (do not edit this line) */
