package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStreamProducer;
import com.orientechnologies.orient.core.sql.executor.resultset.OMultipleExecutionStream;
import com.orientechnologies.orient.core.sql.parser.OIdentifier;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by luigidellaquila on 21/02/17.
 */
public class FetchEdgesToVerticesStep extends AbstractExecutionStep {

  private final String toAlias;
  private final OIdentifier targetCluster;
  private final OIdentifier targetClass;

  public FetchEdgesToVerticesStep(
      String toAlias,
      OIdentifier targetClass,
      OIdentifier targetCluster,
      OCommandContext ctx,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.toAlias = toAlias;
    this.targetClass = targetClass;
    this.targetCluster = targetCluster;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    Stream<Object> source = init();

    OExecutionStreamProducer res =
        new OExecutionStreamProducer() {
          private final Iterator iter = source.iterator();

          @Override
          public OExecutionStream next(OCommandContext ctx) {
            return edges(iter.next());
          }

          @Override
          public boolean hasNext(OCommandContext ctx) {
            return iter.hasNext();
          }

          @Override
          public void close(OCommandContext ctx) {}
        };

    return new OMultipleExecutionStream(res);
  }

  private Stream<Object> init() {
    Object toValues;

    toValues = ctx.getVariable(toAlias);
    if (toValues instanceof Iterable && !(toValues instanceof OIdentifiable)) {
      toValues = ((Iterable<?>) toValues).iterator();
    } else if (!(toValues instanceof Iterator)) {
      toValues = Collections.singleton(toValues).iterator();
    }

    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize((Iterator<?>) toValues, 0), false);
  }

  private OExecutionStream edges(Object from) {
    if (from instanceof OResult) {
      from = ((OResult) from).toElement();
    }
    if (from instanceof OIdentifiable && !(from instanceof OElement)) {
      from = ((OIdentifiable) from).getRecord();
    }
    if (from instanceof OElement && ((OElement) from).isVertex()) {
      var vertex = ((OElement) from).toVertex();
      assert vertex != null;
      Iterable<OEdge> edges = vertex.getEdges(ODirection.IN);
      Stream<OResult> stream =
          StreamSupport.stream(edges.spliterator(), false)
              .filter((edge) -> matchesClass(edge) && matchesCluster(edge))
              .map(OResultInternal::new);
      return OExecutionStream.resultIterator(stream.iterator());
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
    String result = spaces + "+ FOR EACH x in " + toAlias + "\n";
    result += spaces + "       FETCH EDGES TO x";
    if (targetClass != null) {
      result += "\n" + spaces + "       (target class " + targetClass + ")";
    }
    if (targetCluster != null) {
      result += "\n" + spaces + "       (target cluster " + targetCluster + ")";
    }
    return result;
  }
}
