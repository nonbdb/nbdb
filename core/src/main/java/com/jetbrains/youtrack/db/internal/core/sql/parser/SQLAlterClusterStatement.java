/* Generated By:JJTree: Do not edit this line. SQLAlterClusterStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.storage.StorageCluster;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLAlterClusterStatement extends DDLStatement {

  protected SQLIdentifier name;
  protected boolean starred = false;
  protected SQLIdentifier attributeName;
  protected SQLExpression attributeValue;

  public SQLAlterClusterStatement(int id) {
    super(id);
  }

  public SQLAlterClusterStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER CLUSTER ");
    name.toString(params, builder);
    if (starred) {
      builder.append("*");
    }
    builder.append(" ");
    attributeName.toString(params, builder);
    builder.append(" ");
    attributeValue.toString(params, builder);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("ALTER CLUSTER ");
    name.toGenericStatement(builder);
    if (starred) {
      builder.append("*");
    }
    builder.append(" ");
    attributeName.toGenericStatement(builder);
    builder.append(" ");
    attributeValue.toGenericStatement(builder);
  }

  @Override
  public SQLAlterClusterStatement copy() {
    SQLAlterClusterStatement result = new SQLAlterClusterStatement(-1);
    result.name = name == null ? null : name.copy();
    result.attributeName = attributeName == null ? null : attributeName.copy();
    result.starred = starred;
    result.attributeValue = attributeValue == null ? null : attributeValue.copy();
    return result;
  }

  @Override
  public ExecutionStream executeDDL(CommandContext ctx) {
    List<Result> result = new ArrayList<>();
    IntArrayList clustersToUpdate = getClusters(ctx);

    Object finalValue = attributeValue.execute((Identifiable) null, ctx);

    final StorageCluster.ATTRIBUTES attribute =
        Arrays.stream(StorageCluster.ATTRIBUTES.values())
            .filter(e -> e.name().equalsIgnoreCase(notNull(attributeName.getStringValue())))
            .findAny()
            .orElseThrow(
                () ->
                    new UnsupportedOperationException(
                        "Unknown class attribute '"
                            + attributeName
                            + "'. Supported attributes are: "
                            + noDeprecatedValues(StorageCluster.ATTRIBUTES.values())));

    final Storage storage = ctx.getDatabase().getStorage();
    for (final int clusterId : clustersToUpdate) {
      storage.setClusterAttribute(clusterId, attribute, finalValue);

      ResultInternal resultItem = new ResultInternal(ctx.getDatabase());
      resultItem.setProperty("cluster", storage.getClusterName(ctx.getDatabase(), clusterId));
      result.add(resultItem);
    }

    return ExecutionStream.resultIterator(result.iterator());
  }

  private List<StorageCluster.ATTRIBUTES> noDeprecatedValues(
      final StorageCluster.ATTRIBUTES[] values) {
    return Arrays.stream(values)
        .filter(
            value -> {
              try {
                final Field field = StorageCluster.ATTRIBUTES.class.getField(value.name());
                return !field.isAnnotationPresent(Deprecated.class);
              } catch (final NoSuchFieldException | SecurityException e) {
                return false;
              }
            })
        .collect(Collectors.toList());
  }

  private String notNull(final String value) {
    return value != null ? value : "";
  }

  private IntArrayList getClusters(CommandContext ctx) {
    DatabaseSessionInternal database = ctx.getDatabase();
    if (starred) {
      IntArrayList result = new IntArrayList();
      for (String clusterName : database.getClusterNames()) {
        if (clusterName.startsWith(name.getStringValue())) {
          result.add(database.getClusterIdByName(clusterName));
        }
      }
      return result;
    } else {
      final int clusterId = database.getClusterIdByName(name.getStringValue());
      if (clusterId <= 0) {
        throw new CommandExecutionException("Cannot find cluster " + name);
      }

      return IntArrayList.of(clusterId);
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

    SQLAlterClusterStatement that = (SQLAlterClusterStatement) o;

    if (starred != that.starred) {
      return false;
    }
    if (!Objects.equals(name, that.name)) {
      return false;
    }
    if (!Objects.equals(attributeName, that.attributeName)) {
      return false;
    }
    return Objects.equals(attributeValue, that.attributeValue);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (attributeName != null ? attributeName.hashCode() : 0);
    result = 31 * result + (starred ? 1 : 0);
    result = 31 * result + (attributeValue != null ? attributeValue.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=ed78ea0f1a05b0963db625ed1f338bd6 (do not edit this line) */
