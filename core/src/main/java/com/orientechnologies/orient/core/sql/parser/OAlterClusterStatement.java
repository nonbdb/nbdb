/* Generated By:JJTree: Do not edit this line. OAlterClusterStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.storage.OCluster;
import com.orientechnologies.orient.core.storage.OStorage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OAlterClusterStatement extends ODDLStatement {

  protected OIdentifier name;
  protected boolean starred = false;
  protected OIdentifier attributeName;
  protected OExpression attributeValue;

  public OAlterClusterStatement(int id) {
    super(id);
  }

  public OAlterClusterStatement(OrientSql p, int id) {
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
  public OAlterClusterStatement copy() {
    OAlterClusterStatement result = new OAlterClusterStatement(-1);
    result.name = name == null ? null : name.copy();
    result.attributeName = attributeName == null ? null : attributeName.copy();
    result.starred = starred;
    result.attributeValue = attributeValue == null ? null : attributeValue.copy();
    return result;
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {
    List<OResult> result = new ArrayList<>();
    IntArrayList clustersToUpdate = getClusters(ctx);

    Object finalValue = attributeValue.execute((YTIdentifiable) null, ctx);

    final com.orientechnologies.orient.core.storage.OCluster.ATTRIBUTES attribute =
        Arrays.stream(OCluster.ATTRIBUTES.values())
            .filter(e -> e.name().equalsIgnoreCase(notNull(attributeName.getStringValue())))
            .findAny()
            .orElseThrow(
                () ->
                    new UnsupportedOperationException(
                        "Unknown class attribute '"
                            + attributeName
                            + "'. Supported attributes are: "
                            + noDeprecatedValues(OCluster.ATTRIBUTES.values())));

    final OStorage storage = ctx.getDatabase().getStorage();
    for (final int clusterId : clustersToUpdate) {
      storage.setClusterAttribute(clusterId, attribute, finalValue);

      OResultInternal resultItem = new OResultInternal(ctx.getDatabase());
      resultItem.setProperty("cluster", storage.getClusterName(ctx.getDatabase(), clusterId));
      result.add(resultItem);
    }

    return OExecutionStream.resultIterator(result.iterator());
  }

  private List<OCluster.ATTRIBUTES> noDeprecatedValues(final OCluster.ATTRIBUTES[] values) {
    return Arrays.stream(values)
        .filter(
            value -> {
              try {
                final Field field = OCluster.ATTRIBUTES.class.getField(value.name());
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

  private IntArrayList getClusters(OCommandContext ctx) {
    YTDatabaseSessionInternal database = ctx.getDatabase();
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
        throw new OCommandExecutionException("Cannot find cluster " + name);
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

    OAlterClusterStatement that = (OAlterClusterStatement) o;

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
