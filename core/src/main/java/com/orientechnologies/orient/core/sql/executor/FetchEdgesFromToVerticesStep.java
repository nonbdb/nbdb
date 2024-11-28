package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStreamProducer;
import com.orientechnologies.orient.core.sql.executor.resultset.OMultipleExecutionStream;
import com.orientechnologies.orient.core.sql.parser.OIdentifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 *
 */
public class FetchEdgesFromToVerticesStep extends AbstractExecutionStep {

  private final OIdentifier targetClass;
  private final OIdentifier targetCluster;
  private final String fromAlias;
  private final String toAlias;

  public FetchEdgesFromToVerticesStep(
      String fromAlias,
      String toAlias,
      OIdentifier targetClass,
      OIdentifier targetCluster,
      OCommandContext ctx,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.targetClass = targetClass;
    this.targetCluster = targetCluster;
    this.fromAlias = fromAlias;
    this.toAlias = toAlias;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    final Iterator fromIter = loadFrom();

    final Set<ORID> toList = loadTo();

    var db = ctx.getDatabase();
    OExecutionStreamProducer res =
        new OExecutionStreamProducer() {
          private final Iterator iter = fromIter;
          private final Set<ORID> to = toList;

          @Override
          public OExecutionStream next(OCommandContext ctx) {
            return createResultSet(db, to, iter.next());
          }

          @Override
          public boolean hasNext(OCommandContext ctx) {
            return iter.hasNext();
          }

          @Override
          public void close(OCommandContext ctx) {
          }
        };

    return new OMultipleExecutionStream(res);
  }

  private OExecutionStream createResultSet(ODatabaseSessionInternal db, Set<ORID> toList,
      Object val) {
    return OExecutionStream.resultIterator(
        StreamSupport.stream(this.loadNextResults(val).spliterator(), false)
            .filter((e) -> filterResult(e, toList))
            .map(
                (edge) -> {
                  return (OResult) new OResultInternal(db, edge);
                })
            .iterator());
  }

  private Set<ORID> loadTo() {
    Object toValues = null;

    toValues = ctx.getVariable(toAlias);
    if (toValues instanceof Iterable && !(toValues instanceof OIdentifiable)) {
      toValues = ((Iterable<?>) toValues).iterator();
    } else if (!(toValues instanceof Iterator) && toValues != null) {
      toValues = Collections.singleton(toValues).iterator();
    }

    Iterator<?> toIter = (Iterator<?>) toValues;
    if (toIter != null) {
      final Set<ORID> toList = new HashSet<ORID>();
      while (toIter.hasNext()) {
        Object elem = toIter.next();
        if (elem instanceof OResult) {
          elem = ((OResult) elem).toElement();
        }
        if (elem instanceof OIdentifiable && !(elem instanceof OElement)) {
          elem = ((OIdentifiable) elem).getRecord();
        }
        if (!(elem instanceof OElement)) {
          throw new OCommandExecutionException("Invalid vertex: " + elem);
        }
        ((OElement) elem).asVertex().ifPresent(x -> toList.add(x.getIdentity()));
      }

      return toList;
    }
    return null;
  }

  private Iterator<?> loadFrom() {
    Object fromValues = null;

    fromValues = ctx.getVariable(fromAlias);
    if (fromValues instanceof Iterable && !(fromValues instanceof OIdentifiable)) {
      fromValues = ((Iterable<?>) fromValues).iterator();
    } else if (!(fromValues instanceof Iterator)) {
      fromValues = Collections.singleton(fromValues).iterator();
    }
    return (Iterator<?>) fromValues;
  }

  private boolean filterResult(OEdge edge, Set<ORID> toList) {
    if (toList == null || toList.contains(edge.getTo().getIdentity())) {
      return matchesClass(edge) && matchesCluster(edge);
    }
    return true;
  }

  private Iterable<OEdge> loadNextResults(Object from) {
    if (from instanceof OResult) {
      from = ((OResult) from).toElement();
    }
    if (from instanceof OIdentifiable && !(from instanceof OElement)) {
      from = ((OIdentifiable) from).getRecord();
    }
    if (from instanceof OElement && ((OElement) from).isVertex()) {
      var vertex = ((OElement) from).toVertex();
      assert vertex != null;
      return vertex.getEdges(ODirection.OUT);
    } else {
      throw new OCommandExecutionException("Invalid vertex: " + from);
    }
  }

  private boolean matchesCluster(OEdge edge) {
    if (targetCluster == null) {
      return true;
    }
    int clusterId = edge.getIdentity().getClusterId();
    String clusterName = ctx.getDatabase().getClusterNameById(clusterId);
    return clusterName.equals(targetCluster.getStringValue());
  }

  private boolean matchesClass(OEdge edge) {
    if (targetClass == null) {
      return true;
    }
    var schemaClass = edge.getSchemaClass();
    assert schemaClass != null;
    return schemaClass.isSubClassOf(targetClass.getStringValue());
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    String result = spaces + "+ FOR EACH x in " + fromAlias + "\n";
    result += spaces + "    FOR EACH y in " + toAlias + "\n";
    result += spaces + "       FETCH EDGES FROM x TO y";
    if (targetClass != null) {
      result += "\n" + spaces + "       (target class " + targetClass + ")";
    }
    if (targetCluster != null) {
      result += "\n" + spaces + "       (target cluster " + targetCluster + ")";
    }
    return result;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }

  @Override
  public OExecutionStep copy(OCommandContext ctx) {
    return new FetchEdgesFromToVerticesStep(
        fromAlias, toAlias, targetClass, targetCluster, ctx, profilingEnabled);
  }
}
