package com.jetbrains.youtrack.db.internal.core.sql.functions.graph;

import com.jetbrains.youtrack.db.internal.common.collection.OMultiCollectionIterator;
import com.jetbrains.youtrack.db.internal.common.util.ORawPair;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandExecutorAbstract;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.record.Edge;
import com.jetbrains.youtrack.db.internal.core.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.ODirection;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.OEdgeToVertexIterable;
import com.jetbrains.youtrack.db.internal.core.sql.OSQLHelper;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.OSQLFunctionMathAbstract;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shortest path algorithm to find the shortest path from one node to another node in a directed
 * graph.
 */
public class OSQLFunctionShortestPath extends OSQLFunctionMathAbstract {

  public static final String NAME = "shortestPath";
  public static final String PARAM_MAX_DEPTH = "maxDepth";

  protected static final float DISTANCE = 1f;

  public OSQLFunctionShortestPath() {
    super(NAME, 2, 5);
  }

  private class OShortestPathContext {

    private Vertex sourceVertex;
    private Vertex destinationVertex;
    private ODirection directionLeft = ODirection.BOTH;
    private ODirection directionRight = ODirection.BOTH;

    private String edgeType;
    private String[] edgeTypeParam;

    private ArrayDeque<Vertex> queueLeft = new ArrayDeque<>();
    private ArrayDeque<Vertex> queueRight = new ArrayDeque<>();

    private final Set<YTRID> leftVisited = new HashSet<YTRID>();
    private final Set<YTRID> rightVisited = new HashSet<YTRID>();

    private final Map<YTRID, YTRID> previouses = new HashMap<YTRID, YTRID>();
    private final Map<YTRID, YTRID> nexts = new HashMap<YTRID, YTRID>();

    private Vertex current;
    private Vertex currentRight;
    public Integer maxDepth;

    /**
     * option that decides whether or not to return the edge information
     */
    public Boolean edge;
  }

  public List<YTRID> execute(
      Object iThis,
      final YTIdentifiable iCurrentRecord,
      final Object iCurrentResult,
      final Object[] iParams,
      final CommandContext iContext) {

    final Record record = iCurrentRecord != null ? iCurrentRecord.getRecord() : null;

    final OShortestPathContext ctx = new OShortestPathContext();

    Object source = iParams[0];
    source = getSingleItem(source);
    if (source == null) {
      throw new IllegalArgumentException("Only one sourceVertex is allowed");
    }
    source = OSQLHelper.getValue(source, record, iContext);
    if (source instanceof YTIdentifiable) {
      Entity elem = ((YTIdentifiable) source).getRecord();
      if (elem == null || !elem.isVertex()) {
        throw new IllegalArgumentException("The sourceVertex must be a vertex record");
      }
      ctx.sourceVertex = elem.asVertex().get();
    } else {
      throw new IllegalArgumentException("The sourceVertex must be a vertex record");
    }

    Object dest = iParams[1];
    dest = getSingleItem(dest);
    if (dest == null) {
      throw new IllegalArgumentException("Only one destinationVertex is allowed");
    }
    dest = OSQLHelper.getValue(dest, record, iContext);
    if (dest instanceof YTIdentifiable) {
      Entity elem = ((YTIdentifiable) dest).getRecord();
      if (elem == null || !elem.isVertex()) {
        throw new IllegalArgumentException("The destinationVertex must be a vertex record");
      }
      ctx.destinationVertex = elem.asVertex().get();
    } else {
      throw new IllegalArgumentException("The destinationVertex must be a vertex record");
    }

    if (ctx.sourceVertex.equals(ctx.destinationVertex)) {
      final List<YTRID> result = new ArrayList<YTRID>(1);
      result.add(ctx.destinationVertex.getIdentity());
      return result;
    }

    if (iParams.length > 2 && iParams[2] != null) {
      ctx.directionLeft = ODirection.valueOf(iParams[2].toString().toUpperCase(Locale.ENGLISH));
    }
    if (ctx.directionLeft == ODirection.OUT) {
      ctx.directionRight = ODirection.IN;
    } else if (ctx.directionLeft == ODirection.IN) {
      ctx.directionRight = ODirection.OUT;
    }

    ctx.edgeType = null;
    if (iParams.length > 3) {

      Object param = iParams[3];
      if (param instanceof Collection
          && ((Collection) param).stream().allMatch(x -> x instanceof String)) {
        ctx.edgeType = ((Collection<String>) param).stream().collect(Collectors.joining(","));
        ctx.edgeTypeParam = (String[]) ((Collection) param).toArray(new String[0]);
      } else {
        ctx.edgeType = param == null ? null : "" + param;
        ctx.edgeTypeParam = new String[]{ctx.edgeType};
      }
    } else {
      ctx.edgeTypeParam = new String[]{null};
    }

    if (iParams.length > 4) {
      bindAdditionalParams(iParams[4], ctx);
    }

    ctx.queueLeft.add(ctx.sourceVertex);
    ctx.leftVisited.add(ctx.sourceVertex.getIdentity());

    ctx.queueRight.add(ctx.destinationVertex);
    ctx.rightVisited.add(ctx.destinationVertex.getIdentity());

    int depth = 1;
    while (true) {
      if (ctx.maxDepth != null && ctx.maxDepth <= depth) {
        break;
      }
      if (ctx.queueLeft.isEmpty() || ctx.queueRight.isEmpty()) {
        break;
      }

      if (Thread.interrupted()) {
        throw new YTCommandExecutionException("The shortestPath() function has been interrupted");
      }

      if (!CommandExecutorAbstract.checkInterruption(iContext)) {
        break;
      }

      List<YTRID> neighborIdentity;

      if (ctx.queueLeft.size() <= ctx.queueRight.size()) {
        // START EVALUATING FROM LEFT
        neighborIdentity = walkLeft(ctx);
        if (neighborIdentity != null) {
          return neighborIdentity;
        }
        depth++;
        if (ctx.maxDepth != null && ctx.maxDepth <= depth) {
          break;
        }

        if (ctx.queueLeft.isEmpty()) {
          break;
        }

        neighborIdentity = walkRight(ctx);
        if (neighborIdentity != null) {
          return neighborIdentity;
        }

      } else {

        // START EVALUATING FROM RIGHT
        neighborIdentity = walkRight(ctx);
        if (neighborIdentity != null) {
          return neighborIdentity;
        }

        depth++;
        if (ctx.maxDepth != null && ctx.maxDepth <= depth) {
          break;
        }

        if (ctx.queueRight.isEmpty()) {
          break;
        }

        neighborIdentity = walkLeft(ctx);
        if (neighborIdentity != null) {
          return neighborIdentity;
        }
      }

      depth++;
    }
    return new ArrayList<YTRID>();
  }

  private void bindAdditionalParams(Object additionalParams, OShortestPathContext ctx) {
    if (additionalParams == null) {
      return;
    }
    Map<String, ?> mapParams = null;
    if (additionalParams instanceof Map) {
      mapParams = (Map) additionalParams;
    } else if (additionalParams instanceof YTIdentifiable) {
      mapParams = ((EntityImpl) ((YTIdentifiable) additionalParams).getRecord()).toMap();
    }
    if (mapParams != null) {
      ctx.maxDepth = integer(mapParams.get("maxDepth"));
      Boolean withEdge = toBoolean(mapParams.get("edge"));
      ctx.edge = Boolean.TRUE.equals(withEdge) ? Boolean.TRUE : Boolean.FALSE;
    }
  }

  private Integer integer(Object fromObject) {
    if (fromObject == null) {
      return null;
    }
    if (fromObject instanceof Number) {
      return ((Number) fromObject).intValue();
    }
    if (fromObject instanceof String) {
      try {
        return Integer.parseInt(fromObject.toString());
      } catch (NumberFormatException ignore) {
      }
    }
    return null;
  }

  /**
   * @return
   */
  private Boolean toBoolean(Object fromObject) {
    if (fromObject == null) {
      return null;
    }
    if (fromObject instanceof Boolean) {
      return (Boolean) fromObject;
    }
    if (fromObject instanceof String) {
      try {
        return Boolean.parseBoolean(fromObject.toString());
      } catch (NumberFormatException ignore) {
      }
    }
    return null;
  }

  /**
   * get adjacent vertices and edges
   *
   * @param srcVertex
   * @param direction
   * @param types
   * @return
   */
  private ORawPair<Iterable<Vertex>, Iterable<Edge>> getVerticesAndEdges(
      Vertex srcVertex, ODirection direction, String... types) {
    if (direction == ODirection.BOTH) {
      OMultiCollectionIterator<Vertex> vertexIterator = new OMultiCollectionIterator<>();
      OMultiCollectionIterator<Edge> edgeIterator = new OMultiCollectionIterator<>();
      ORawPair<Iterable<Vertex>, Iterable<Edge>> pair1 =
          getVerticesAndEdges(srcVertex, ODirection.OUT, types);
      ORawPair<Iterable<Vertex>, Iterable<Edge>> pair2 =
          getVerticesAndEdges(srcVertex, ODirection.IN, types);
      vertexIterator.add(pair1.first);
      vertexIterator.add(pair2.first);
      edgeIterator.add(pair1.second);
      edgeIterator.add(pair2.second);
      return new ORawPair<>(vertexIterator, edgeIterator);
    } else {
      Iterable<Edge> edges1 = srcVertex.getEdges(direction, types);
      Iterable<Edge> edges2 = srcVertex.getEdges(direction, types);
      return new ORawPair<>(new OEdgeToVertexIterable(edges1, direction), edges2);
    }
  }

  /**
   * get adjacent vertices and edges
   *
   * @param srcVertex
   * @param direction
   * @return
   */
  private ORawPair<Iterable<Vertex>, Iterable<Edge>> getVerticesAndEdges(
      Vertex srcVertex, ODirection direction) {
    return getVerticesAndEdges(srcVertex, direction, (String[]) null);
  }

  public String getSyntax(YTDatabaseSession session) {
    return "shortestPath(<sourceVertex>, <destinationVertex>, [<direction>, [ <edgeTypeAsString>"
        + " ]])";
  }

  protected List<YTRID> walkLeft(final OSQLFunctionShortestPath.OShortestPathContext ctx) {
    ArrayDeque<Vertex> nextLevelQueue = new ArrayDeque<>();
    if (!Boolean.TRUE.equals(ctx.edge)) {
      while (!ctx.queueLeft.isEmpty()) {
        ctx.current = ctx.queueLeft.poll();

        Iterable<Vertex> neighbors;
        if (ctx.edgeType == null) {
          neighbors = ctx.current.getVertices(ctx.directionLeft);
        } else {
          neighbors = ctx.current.getVertices(ctx.directionLeft, ctx.edgeTypeParam);
        }
        for (Vertex neighbor : neighbors) {
          final Vertex v = neighbor;
          final YTRID neighborIdentity = v.getIdentity();

          if (ctx.rightVisited.contains(neighborIdentity)) {
            ctx.previouses.put(neighborIdentity, ctx.current.getIdentity());
            return computePath(ctx.previouses, ctx.nexts, neighborIdentity);
          }
          if (!ctx.leftVisited.contains(neighborIdentity)) {
            ctx.previouses.put(neighborIdentity, ctx.current.getIdentity());

            nextLevelQueue.offer(v);
            ctx.leftVisited.add(neighborIdentity);
          }
        }
      }
    } else {
      while (!ctx.queueLeft.isEmpty()) {
        ctx.current = ctx.queueLeft.poll();

        ORawPair<Iterable<Vertex>, Iterable<Edge>> neighbors;
        if (ctx.edgeType == null) {
          neighbors = getVerticesAndEdges(ctx.current, ctx.directionLeft);
        } else {
          neighbors = getVerticesAndEdges(ctx.current, ctx.directionLeft, ctx.edgeTypeParam);
        }
        Iterator<Vertex> vertexIterator = neighbors.first.iterator();
        Iterator<Edge> edgeIterator = neighbors.second.iterator();
        while (vertexIterator.hasNext() && edgeIterator.hasNext()) {
          Vertex v = vertexIterator.next();
          final YTRID neighborVertexIdentity = v.getIdentity();
          final YTRID neighborEdgeIdentity = edgeIterator.next().getIdentity();

          if (ctx.rightVisited.contains(neighborVertexIdentity)) {
            ctx.previouses.put(neighborVertexIdentity, neighborEdgeIdentity);
            ctx.previouses.put(neighborEdgeIdentity, ctx.current.getIdentity());
            return computePath(ctx.previouses, ctx.nexts, neighborVertexIdentity);
          }
          if (!ctx.leftVisited.contains(neighborVertexIdentity)) {
            ctx.previouses.put(neighborVertexIdentity, neighborEdgeIdentity);
            ctx.previouses.put(neighborEdgeIdentity, ctx.current.getIdentity());

            nextLevelQueue.offer(v);
            ctx.leftVisited.add(neighborVertexIdentity);
          }
        }
      }
    }
    ctx.queueLeft = nextLevelQueue;
    return null;
  }

  protected List<YTRID> walkRight(final OSQLFunctionShortestPath.OShortestPathContext ctx) {
    final ArrayDeque<Vertex> nextLevelQueue = new ArrayDeque<>();
    if (!Boolean.TRUE.equals(ctx.edge)) {
      while (!ctx.queueRight.isEmpty()) {
        ctx.currentRight = ctx.queueRight.poll();

        Iterable<Vertex> neighbors;
        if (ctx.edgeType == null) {
          neighbors = ctx.currentRight.getVertices(ctx.directionRight);
        } else {
          neighbors = ctx.currentRight.getVertices(ctx.directionRight, ctx.edgeTypeParam);
        }
        for (Vertex neighbor : neighbors) {
          final Vertex v = neighbor;
          final YTRID neighborIdentity = v.getIdentity();

          if (ctx.leftVisited.contains(neighborIdentity)) {
            ctx.nexts.put(neighborIdentity, ctx.currentRight.getIdentity());
            return computePath(ctx.previouses, ctx.nexts, neighborIdentity);
          }
          if (!ctx.rightVisited.contains(neighborIdentity)) {

            ctx.nexts.put(neighborIdentity, ctx.currentRight.getIdentity());

            nextLevelQueue.offer(v);
            ctx.rightVisited.add(neighborIdentity);
          }
        }
      }
    } else {
      while (!ctx.queueRight.isEmpty()) {
        ctx.currentRight = ctx.queueRight.poll();

        ORawPair<Iterable<Vertex>, Iterable<Edge>> neighbors;
        if (ctx.edgeType == null) {
          neighbors = getVerticesAndEdges(ctx.currentRight, ctx.directionRight);
        } else {
          neighbors = getVerticesAndEdges(ctx.currentRight, ctx.directionRight, ctx.edgeTypeParam);
        }

        Iterator<Vertex> vertexIterator = neighbors.first.iterator();
        Iterator<Edge> edgeIterator = neighbors.second.iterator();
        while (vertexIterator.hasNext() && edgeIterator.hasNext()) {
          final Vertex v = vertexIterator.next();
          final YTRID neighborVertexIdentity = v.getIdentity();
          final YTRID neighborEdgeIdentity = edgeIterator.next().getIdentity();

          if (ctx.leftVisited.contains(neighborVertexIdentity)) {
            ctx.nexts.put(neighborVertexIdentity, neighborEdgeIdentity);
            ctx.nexts.put(neighborEdgeIdentity, ctx.currentRight.getIdentity());
            return computePath(ctx.previouses, ctx.nexts, neighborVertexIdentity);
          }
          if (!ctx.rightVisited.contains(neighborVertexIdentity)) {
            ctx.nexts.put(neighborVertexIdentity, neighborEdgeIdentity);
            ctx.nexts.put(neighborEdgeIdentity, ctx.currentRight.getIdentity());

            nextLevelQueue.offer(v);
            ctx.rightVisited.add(neighborVertexIdentity);
          }
        }
      }
    }
    ctx.queueRight = nextLevelQueue;
    return null;
  }

  private List<YTRID> computePath(
      final Map<YTRID, YTRID> leftDistances,
      final Map<YTRID, YTRID> rightDistances,
      final YTRID neighbor) {
    final List<YTRID> result = new ArrayList<YTRID>();

    YTRID current = neighbor;
    while (current != null) {
      result.add(0, current);
      current = leftDistances.get(current);
    }

    current = neighbor;
    while (current != null) {
      current = rightDistances.get(current);
      if (current != null) {
        result.add(current);
      }
    }

    return result;
  }
}