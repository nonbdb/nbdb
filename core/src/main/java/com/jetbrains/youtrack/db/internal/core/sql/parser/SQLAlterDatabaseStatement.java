/* Generated By:JJTree: Do not edit this line. SQLAlterDatabaseStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.config.StorageEntryConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal.ATTRIBUTES;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.sql.executor.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SQLAlterDatabaseStatement extends DDLStatement {

  SQLIdentifier customPropertyName;
  SQLExpression customPropertyValue;

  SQLIdentifier settingName;
  SQLExpression settingValue;

  public SQLAlterDatabaseStatement(int id) {
    super(id);
  }

  public SQLAlterDatabaseStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeDDL(CommandContext ctx) {
    if (customPropertyName == null) {
      return ExecutionStream.singleton(executeSimpleAlter(settingName, settingValue, ctx));
    } else {
      return ExecutionStream.singleton(
          executeCustomAlter(customPropertyName, customPropertyValue, ctx));
    }
  }

  private Result executeCustomAlter(
      SQLIdentifier customPropertyName, SQLExpression customPropertyValue, CommandContext ctx) {
    DatabaseSessionInternal db = ctx.getDatabase();
    db.checkSecurity(Rule.ResourceGeneric.DATABASE, Role.PERMISSION_UPDATE);
    List<StorageEntryConfiguration> oldValues =
        (List<StorageEntryConfiguration>) db.get(ATTRIBUTES.CUSTOM);
    String oldValue = null;
    if (oldValues != null) {
      for (StorageEntryConfiguration entry : oldValues) {
        if (entry.name.equals(customPropertyName.getStringValue())) {
          oldValue = entry.value;
          break;
        }
      }
    }
    Object finalValue = customPropertyValue.execute((Identifiable) null, ctx);
    db.setCustom(customPropertyName.getStringValue(), finalValue);

    ResultInternal result = new ResultInternal(db);
    result.setProperty("operation", "alter database");
    result.setProperty("customAttribute", customPropertyName.getStringValue());
    result.setProperty("oldValue", oldValue);
    result.setProperty("newValue", finalValue);
    return result;
  }

  private Result executeSimpleAlter(
      SQLIdentifier settingName, SQLExpression settingValue, CommandContext ctx) {
    ATTRIBUTES attribute =
        ATTRIBUTES.valueOf(
            settingName.getStringValue().toUpperCase(Locale.ENGLISH));
    DatabaseSessionInternal db = ctx.getDatabase();
    db.checkSecurity(Rule.ResourceGeneric.DATABASE, Role.PERMISSION_UPDATE);
    Object oldValue = db.get(attribute);
    Object finalValue = settingValue.execute((Identifiable) null, ctx);
    db.setInternal(attribute, finalValue);

    ResultInternal result = new ResultInternal(db);
    result.setProperty("operation", "alter database");
    result.setProperty("attribute", settingName.getStringValue());
    result.setProperty("oldValue", oldValue);
    result.setProperty("newValue", finalValue);
    return result;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER DATABASE ");

    if (customPropertyName != null) {
      builder.append("CUSTOM ");
      customPropertyName.toString(params, builder);
      builder.append(" = ");
      customPropertyValue.toString(params, builder);
    } else {
      settingName.toString(params, builder);
      builder.append(" ");
      settingValue.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("ALTER DATABASE ");

    if (customPropertyName != null) {
      builder.append("CUSTOM ");
      customPropertyName.toGenericStatement(builder);
      builder.append(" = ");
      customPropertyValue.toGenericStatement(builder);
    } else {
      settingName.toGenericStatement(builder);
      builder.append(" ");
      settingValue.toGenericStatement(builder);
    }
  }

  @Override
  public SQLAlterDatabaseStatement copy() {
    SQLAlterDatabaseStatement result = new SQLAlterDatabaseStatement(-1);
    result.customPropertyName = customPropertyName == null ? null : customPropertyName.copy();
    result.customPropertyValue = customPropertyValue == null ? null : customPropertyValue.copy();
    result.settingName = settingName == null ? null : settingName.copy();
    result.settingValue = settingValue == null ? null : settingValue.copy();
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

    SQLAlterDatabaseStatement that = (SQLAlterDatabaseStatement) o;

    if (!Objects.equals(customPropertyName, that.customPropertyName)) {
      return false;
    }
    if (!Objects.equals(customPropertyValue, that.customPropertyValue)) {
      return false;
    }
    if (!Objects.equals(settingName, that.settingName)) {
      return false;
    }
    return Objects.equals(settingValue, that.settingValue);
  }

  @Override
  public int hashCode() {
    int result = customPropertyName != null ? customPropertyName.hashCode() : 0;
    result = 31 * result + (customPropertyValue != null ? customPropertyValue.hashCode() : 0);
    result = 31 * result + (settingName != null ? settingName.hashCode() : 0);
    result = 31 * result + (settingValue != null ? settingValue.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=8fec57db8dd2a3b52aaa52dec7367cd4 (do not edit this line) */
