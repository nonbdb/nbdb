/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.orientechnologies.orient.core.sql.functions.graph;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.common.io.OIOUtils;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.YTEdge;
import com.orientechnologies.orient.core.record.YTEntity;
import com.orientechnologies.orient.core.record.YTRecord;
import com.orientechnologies.orient.core.record.YTVertex;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.OSQLHelper;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A*'s algorithm describes how to find the cheapest path from one node to another node in a
 * directed weighted graph with husrestic function.
 *
 * <p>The first parameter is source record. The second parameter is destination record. The third
 * parameter is a name of property that represents 'weight' and fourth represnts the map of
 * options.
 *
 * <p>If property is not defined in edge or is null, distance between vertexes are 0 .
 */
public class OSQLFunctionAstar extends OSQLFunctionHeuristicPathFinderAbstract {

  public static final String NAME = "astar";

  private String paramWeightFieldName = "weight";
  private long currentDepth = 0;
  protected Set<YTVertex> closedSet = new HashSet<YTVertex>();
  protected Map<YTVertex, YTVertex> cameFrom = new HashMap<YTVertex, YTVertex>();

  protected Map<YTVertex, Double> gScore = new HashMap<YTVertex, Double>();
  protected Map<YTVertex, Double> fScore = new HashMap<YTVertex, Double>();
  protected PriorityQueue<YTVertex> open =
      new PriorityQueue<YTVertex>(
          1,
          new Comparator<YTVertex>() {

            public int compare(YTVertex nodeA, YTVertex nodeB) {
              return Double.compare(fScore.get(nodeA), fScore.get(nodeB));
            }
          });

  public OSQLFunctionAstar() {
    super(NAME, 3, 4);
  }

  public LinkedList<YTVertex> execute(
      final Object iThis,
      final YTIdentifiable iCurrentRecord,
      final Object iCurrentResult,
      final Object[] iParams,
      final OCommandContext iContext) {
    context = iContext;
    final OSQLFunctionAstar context = this;

    final YTRecord record = iCurrentRecord != null ? iCurrentRecord.getRecord() : null;

    Object source = iParams[0];
    if (OMultiValue.isMultiValue(source)) {
      if (OMultiValue.getSize(source) > 1) {
        throw new IllegalArgumentException("Only one sourceVertex is allowed");
      }
      source = OMultiValue.getFirstValue(source);
      if (source instanceof YTResult && ((YTResult) source).isElement()) {
        source = ((YTResult) source).getElement().get();
      }
    }
    source = OSQLHelper.getValue(source, record, iContext);
    if (source instanceof YTIdentifiable) {
      YTEntity elem = ((YTIdentifiable) source).getRecord();
      if (!elem.isVertex()) {
        throw new IllegalArgumentException("The sourceVertex must be a vertex record");
      }
      paramSourceVertex = elem.asVertex().get();
    } else {
      throw new IllegalArgumentException("The sourceVertex must be a vertex record");
    }

    Object dest = iParams[1];
    if (OMultiValue.isMultiValue(dest)) {
      if (OMultiValue.getSize(dest) > 1) {
        throw new IllegalArgumentException("Only one destinationVertex is allowed");
      }
      dest = OMultiValue.getFirstValue(dest);
      if (dest instanceof YTResult && ((YTResult) dest).isElement()) {
        dest = ((YTResult) dest).getElement().get();
      }
    }
    dest = OSQLHelper.getValue(dest, record, iContext);
    if (dest instanceof YTIdentifiable) {
      YTEntity elem = ((YTIdentifiable) dest).getRecord();
      if (!elem.isVertex()) {
        throw new IllegalArgumentException("The destinationVertex must be a vertex record");
      }
      paramDestinationVertex = elem.asVertex().get();
    } else {
      throw new IllegalArgumentException("The destinationVertex must be a vertex record");
    }

    paramWeightFieldName = OIOUtils.getStringContent(iParams[2]);

    if (iParams.length > 3) {
      bindAdditionalParams(iParams[3], context);
    }
    iContext.setVariable("getNeighbors", 0);
    if (paramSourceVertex == null || paramDestinationVertex == null) {
      return new LinkedList<>();
    }
    return internalExecute(iContext, iContext.getDatabase());
  }

  private LinkedList<YTVertex> internalExecute(
      final OCommandContext iContext, YTDatabaseSession graph) {

    YTVertex start = paramSourceVertex;
    YTVertex goal = paramDestinationVertex;

    open.add(start);

    // The cost of going from start to start is zero.
    gScore.put(start, 0.0);
    // For the first node, that value is completely heuristic.
    fScore.put(start, getHeuristicCost(start, null, goal, iContext));

    while (!open.isEmpty()) {
      YTVertex current = open.poll();

      // we discussed about this feature in
      // https://github.com/orientechnologies/orientdb/pull/6002#issuecomment-212492687
      if (paramEmptyIfMaxDepth && currentDepth >= paramMaxDepth) {
        route.clear(); // to ensure our result is empty
        return getPath();
      }
      // if start and goal vertex is equal so return current path from  cameFrom hash map
      if (current.getIdentity().equals(goal.getIdentity()) || currentDepth >= paramMaxDepth) {

        while (current != null) {
          route.add(0, current);
          current = cameFrom.get(current);
        }
        return getPath();
      }

      closedSet.add(current);
      for (YTEdge neighborEdge : getNeighborEdges(current)) {

        YTVertex neighbor = getNeighbor(current, neighborEdge, graph);
        // Ignore the neighbor which is already evaluated.
        if (closedSet.contains(neighbor)) {
          continue;
        }
        // The distance from start to a neighbor
        double tentativeGScore = gScore.get(current) + getDistance(neighborEdge);
        boolean contains = open.contains(neighbor);

        if (!contains || tentativeGScore < gScore.get(neighbor)) {
          gScore.put(neighbor, tentativeGScore);
          fScore.put(
              neighbor, tentativeGScore + getHeuristicCost(neighbor, current, goal, iContext));

          if (contains) {
            open.remove(neighbor);
          }
          open.offer(neighbor);
          cameFrom.put(neighbor, current);
        }
      }

      // Increment Depth Level
      currentDepth++;
    }

    return getPath();
  }

  private static YTVertex getNeighbor(YTVertex current, YTEdge neighborEdge,
      YTDatabaseSession graph) {
    if (neighborEdge.getFrom().equals(current)) {
      return toVertex(neighborEdge.getTo());
    }
    return toVertex(neighborEdge.getFrom());
  }

  private static YTVertex toVertex(YTIdentifiable outVertex) {
    if (outVertex == null) {
      return null;
    }
    if (!(outVertex instanceof YTEntity)) {
      outVertex = outVertex.getRecord();
    }
    return ((YTEntity) outVertex).asVertex().orElse(null);
  }

  protected Set<YTEdge> getNeighborEdges(final YTVertex node) {
    context.incrementVariable("getNeighbors");

    final Set<YTEdge> neighbors = new HashSet<YTEdge>();
    if (node != null) {
      for (YTEdge v : node.getEdges(paramDirection, paramEdgeTypeNames)) {
        final YTEdge ov = v;
        if (ov != null) {
          neighbors.add(ov);
        }
      }
    }
    return neighbors;
  }

  private void bindAdditionalParams(Object additionalParams, OSQLFunctionAstar ctx) {
    if (additionalParams == null) {
      return;
    }
    Map<String, ?> mapParams = null;
    if (additionalParams instanceof Map) {
      mapParams = (Map) additionalParams;
    } else if (additionalParams instanceof YTIdentifiable) {
      mapParams = ((YTDocument) ((YTIdentifiable) additionalParams).getRecord()).toMap();
    }
    if (mapParams != null) {
      ctx.paramEdgeTypeNames = stringArray(mapParams.get(OSQLFunctionAstar.PARAM_EDGE_TYPE_NAMES));
      ctx.paramVertexAxisNames =
          stringArray(mapParams.get(OSQLFunctionAstar.PARAM_VERTEX_AXIS_NAMES));
      if (mapParams.get(OSQLFunctionAstar.PARAM_DIRECTION) != null) {
        if (mapParams.get(OSQLFunctionAstar.PARAM_DIRECTION) instanceof String) {
          ctx.paramDirection =
              ODirection.valueOf(
                  stringOrDefault(mapParams.get(OSQLFunctionAstar.PARAM_DIRECTION), "OUT")
                      .toUpperCase(Locale.ENGLISH));
        } else {
          ctx.paramDirection = (ODirection) mapParams.get(OSQLFunctionAstar.PARAM_DIRECTION);
        }
      }

      ctx.paramParallel = booleanOrDefault(mapParams.get(OSQLFunctionAstar.PARAM_PARALLEL), false);
      ctx.paramMaxDepth =
          longOrDefault(mapParams.get(OSQLFunctionAstar.PARAM_MAX_DEPTH), ctx.paramMaxDepth);
      ctx.paramEmptyIfMaxDepth =
          booleanOrDefault(
              mapParams.get(OSQLFunctionAstar.PARAM_EMPTY_IF_MAX_DEPTH), ctx.paramEmptyIfMaxDepth);
      ctx.paramTieBreaker =
          booleanOrDefault(mapParams.get(OSQLFunctionAstar.PARAM_TIE_BREAKER), ctx.paramTieBreaker);
      ctx.paramDFactor =
          doubleOrDefault(mapParams.get(OSQLFunctionAstar.PARAM_D_FACTOR), ctx.paramDFactor);
      if (mapParams.get(OSQLFunctionAstar.PARAM_HEURISTIC_FORMULA) != null) {
        if (mapParams.get(OSQLFunctionAstar.PARAM_HEURISTIC_FORMULA) instanceof String) {
          ctx.paramHeuristicFormula =
              HeuristicFormula.valueOf(
                  stringOrDefault(
                      mapParams.get(OSQLFunctionAstar.PARAM_HEURISTIC_FORMULA), "MANHATAN")
                      .toUpperCase(Locale.ENGLISH));
        } else {
          ctx.paramHeuristicFormula =
              (HeuristicFormula) mapParams.get(OSQLFunctionAstar.PARAM_HEURISTIC_FORMULA);
        }
      }

      ctx.paramCustomHeuristicFormula =
          stringOrDefault(mapParams.get(OSQLFunctionAstar.PARAM_CUSTOM_HEURISTIC_FORMULA), "");
    }
  }

  public String getSyntax(YTDatabaseSession session) {
    return "astar(<sourceVertex>, <destinationVertex>, <weightEdgeFieldName>, [<options>]) \n"
        + " // options  : {direction:\"OUT\",edgeTypeNames:[] , vertexAxisNames:[] ,"
        + " parallel : false ,"
        + " tieBreaker:true,maxDepth:99999,dFactor:1.0,customHeuristicFormula:'custom_Function_Name_here'"
        + "  }";
  }

  @Override
  public Object getResult() {
    return getPath();
  }

  @Override
  protected double getDistance(final YTVertex node, final YTVertex parent, final YTVertex target) {
    final Iterator<YTEdge> edges = node.getEdges(paramDirection).iterator();
    YTEdge e = null;
    while (edges.hasNext()) {
      YTEdge next = edges.next();
      if (next.getFrom().equals(target) || next.getTo().equals(target)) {
        e = next;
        break;
      }
    }
    if (e != null) {
      final Object fieldValue = e.getProperty(paramWeightFieldName);
      if (fieldValue != null) {
        if (fieldValue instanceof Float) {
          return (Float) fieldValue;
        } else if (fieldValue instanceof Number) {
          return ((Number) fieldValue).doubleValue();
        }
      }
    }

    return MIN;
  }

  protected double getDistance(final YTEdge edge) {
    if (edge != null) {
      final Object fieldValue = edge.getProperty(paramWeightFieldName);
      if (fieldValue != null) {
        if (fieldValue instanceof Float) {
          return (Float) fieldValue;
        } else if (fieldValue instanceof Number) {
          return ((Number) fieldValue).doubleValue();
        }
      }
    }

    return MIN;
  }

  @Override
  public boolean aggregateResults() {
    return false;
  }

  @Override
  protected double getHeuristicCost(
      final YTVertex node, YTVertex parent, final YTVertex target, OCommandContext iContext) {
    double hresult = 0.0;

    if (paramVertexAxisNames.length == 0) {
      return hresult;
    } else if (paramVertexAxisNames.length == 1) {
      double n = doubleOrDefault(node.getProperty(paramVertexAxisNames[0]), 0.0);
      double g = doubleOrDefault(target.getProperty(paramVertexAxisNames[0]), 0.0);
      hresult = getSimpleHeuristicCost(n, g, paramDFactor);
    } else if (paramVertexAxisNames.length == 2) {
      if (parent == null) {
        parent = node;
      }
      double sx = doubleOrDefault(paramSourceVertex.getProperty(paramVertexAxisNames[0]), 0);
      double sy = doubleOrDefault(paramSourceVertex.getProperty(paramVertexAxisNames[1]), 0);
      double nx = doubleOrDefault(node.getProperty(paramVertexAxisNames[0]), 0);
      double ny = doubleOrDefault(node.getProperty(paramVertexAxisNames[1]), 0);
      double px = doubleOrDefault(parent.getProperty(paramVertexAxisNames[0]), 0);
      double py = doubleOrDefault(parent.getProperty(paramVertexAxisNames[1]), 0);
      double gx = doubleOrDefault(target.getProperty(paramVertexAxisNames[0]), 0);
      double gy = doubleOrDefault(target.getProperty(paramVertexAxisNames[1]), 0);

      switch (paramHeuristicFormula) {
        case MANHATAN:
          hresult = getManhatanHeuristicCost(nx, ny, gx, gy, paramDFactor);
          break;
        case MAXAXIS:
          hresult = getMaxAxisHeuristicCost(nx, ny, gx, gy, paramDFactor);
          break;
        case DIAGONAL:
          hresult = getDiagonalHeuristicCost(nx, ny, gx, gy, paramDFactor);
          break;
        case EUCLIDEAN:
          hresult = getEuclideanHeuristicCost(nx, ny, gx, gy, paramDFactor);
          break;
        case EUCLIDEANNOSQR:
          hresult = getEuclideanNoSQRHeuristicCost(nx, ny, gx, gy, paramDFactor);
          break;
        case CUSTOM:
          hresult =
              getCustomHeuristicCost(
                  paramCustomHeuristicFormula,
                  paramVertexAxisNames,
                  paramSourceVertex,
                  paramDestinationVertex,
                  node,
                  parent,
                  currentDepth,
                  paramDFactor,
                  iContext);
          break;
      }
      if (paramTieBreaker) {
        hresult = getTieBreakingHeuristicCost(px, py, sx, sy, gx, gy, hresult);
      }

    } else {
      Map<String, Double> sList = new HashMap<String, Double>();
      Map<String, Double> cList = new HashMap<String, Double>();
      Map<String, Double> pList = new HashMap<String, Double>();
      Map<String, Double> gList = new HashMap<String, Double>();
      parent = parent == null ? node : parent;
      for (int i = 0; i < paramVertexAxisNames.length; i++) {
        Double s = doubleOrDefault(paramSourceVertex.getProperty(paramVertexAxisNames[i]), 0);
        Double c = doubleOrDefault(node.getProperty(paramVertexAxisNames[i]), 0);
        Double g = doubleOrDefault(target.getProperty(paramVertexAxisNames[i]), 0);
        Double p = doubleOrDefault(parent.getProperty(paramVertexAxisNames[i]), 0);
        if (s != null) {
          sList.put(paramVertexAxisNames[i], s);
        }
        if (c != null) {
          cList.put(paramVertexAxisNames[i], s);
        }
        if (g != null) {
          gList.put(paramVertexAxisNames[i], g);
        }
        if (p != null) {
          pList.put(paramVertexAxisNames[i], p);
        }
      }
      switch (paramHeuristicFormula) {
        case MANHATAN:
          hresult =
              getManhatanHeuristicCost(
                  paramVertexAxisNames, sList, cList, pList, gList, currentDepth, paramDFactor);
          break;
        case MAXAXIS:
          hresult =
              getMaxAxisHeuristicCost(
                  paramVertexAxisNames, sList, cList, pList, gList, currentDepth, paramDFactor);
          break;
        case DIAGONAL:
          hresult =
              getDiagonalHeuristicCost(
                  paramVertexAxisNames, sList, cList, pList, gList, currentDepth, paramDFactor);
          break;
        case EUCLIDEAN:
          hresult =
              getEuclideanHeuristicCost(
                  paramVertexAxisNames, sList, cList, pList, gList, currentDepth, paramDFactor);
          break;
        case EUCLIDEANNOSQR:
          hresult =
              getEuclideanNoSQRHeuristicCost(
                  paramVertexAxisNames, sList, cList, pList, gList, currentDepth, paramDFactor);
          break;
        case CUSTOM:
          hresult =
              getCustomHeuristicCost(
                  paramCustomHeuristicFormula,
                  paramVertexAxisNames,
                  paramSourceVertex,
                  paramDestinationVertex,
                  node,
                  parent,
                  currentDepth,
                  paramDFactor,
                  iContext);
          break;
      }
      if (paramTieBreaker) {
        hresult =
            getTieBreakingHeuristicCost(
                paramVertexAxisNames, sList, cList, pList, gList, currentDepth, hresult);
      }
    }

    return hresult;
  }

  @Override
  protected boolean isVariableEdgeWeight() {
    return true;
  }
}
