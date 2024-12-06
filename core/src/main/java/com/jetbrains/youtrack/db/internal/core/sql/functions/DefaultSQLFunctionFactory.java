/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.sql.functions;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionDifference;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionDistinct;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionDocument;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionFirst;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionIntersect;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionLast;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionList;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionMap;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionSet;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionSymmetricDifference;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionTraversedEdge;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionTraversedElement;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionTraversedVertex;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLFunctionUnionAll;
import com.jetbrains.youtrack.db.internal.core.sql.functions.geo.SQLFunctionDistance;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionAstar;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionBoth;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionBothE;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionBothV;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionDijkstra;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionIn;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionInE;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionInV;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionOut;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionOutE;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionOutV;
import com.jetbrains.youtrack.db.internal.core.sql.functions.graph.SQLFunctionShortestPath;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionAbsoluteValue;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionAverage;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionDecimal;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionEval;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionInterval;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionMax;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionMin;
import com.jetbrains.youtrack.db.internal.core.sql.functions.math.SQLFunctionSum;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionCoalesce;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionCount;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionDate;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionDecode;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionEncode;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionIf;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionIfNull;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionIndexKeySize;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionStrcmpci;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionSysdate;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionThrowCME;
import com.jetbrains.youtrack.db.internal.core.sql.functions.misc.SQLFunctionUUID;
import com.jetbrains.youtrack.db.internal.core.sql.functions.sequence.SQLFunctionSequence;
import com.jetbrains.youtrack.db.internal.core.sql.functions.stat.SQLFunctionMedian;
import com.jetbrains.youtrack.db.internal.core.sql.functions.stat.SQLFunctionMode;
import com.jetbrains.youtrack.db.internal.core.sql.functions.stat.SQLFunctionPercentile;
import com.jetbrains.youtrack.db.internal.core.sql.functions.stat.SQLFunctionStandardDeviation;
import com.jetbrains.youtrack.db.internal.core.sql.functions.stat.SQLFunctionVariance;
import com.jetbrains.youtrack.db.internal.core.sql.functions.text.SQLFunctionConcat;
import com.jetbrains.youtrack.db.internal.core.sql.functions.text.SQLFunctionFormat;

/**
 * Default set of SQL function.
 */
public final class DefaultSQLFunctionFactory extends SQLFunctionFactoryTemplate {

  @Override
  public void registerDefaultFunctions(DatabaseSessionInternal db) {
    // MISC FUNCTIONS
    register(SQLFunctionAverage.NAME, SQLFunctionAverage.class);
    register(SQLFunctionCoalesce.NAME, new SQLFunctionCoalesce());
    register(SQLFunctionCount.NAME, SQLFunctionCount.class);
    register(SQLFunctionDate.NAME, SQLFunctionDate.class);
    register(SQLFunctionDecode.NAME, new SQLFunctionDecode());
    register(SQLFunctionDifference.NAME, SQLFunctionDifference.class);
    register(SQLFunctionSymmetricDifference.NAME, SQLFunctionSymmetricDifference.class);
    register(SQLFunctionDistance.NAME, new SQLFunctionDistance());
    register(SQLFunctionDistinct.NAME, SQLFunctionDistinct.class);
    register(SQLFunctionDocument.NAME, SQLFunctionDocument.class);
    register(SQLFunctionEncode.NAME, new SQLFunctionEncode());
    register(SQLFunctionEval.NAME, SQLFunctionEval.class);
    register(SQLFunctionFirst.NAME, new SQLFunctionFirst());
    register(SQLFunctionFormat.NAME, new SQLFunctionFormat());
    register(SQLFunctionTraversedEdge.NAME, SQLFunctionTraversedEdge.class);
    register(SQLFunctionTraversedElement.NAME, SQLFunctionTraversedElement.class);
    register(SQLFunctionTraversedVertex.NAME, SQLFunctionTraversedVertex.class);
    register(SQLFunctionIf.NAME, new SQLFunctionIf());
    register(SQLFunctionIfNull.NAME, new SQLFunctionIfNull());
    register(SQLFunctionIntersect.NAME, SQLFunctionIntersect.class);
    register(SQLFunctionLast.NAME, new SQLFunctionLast());
    register(SQLFunctionList.NAME, SQLFunctionList.class);
    register(SQLFunctionMap.NAME, SQLFunctionMap.class);
    register(SQLFunctionMax.NAME, SQLFunctionMax.class);
    register(SQLFunctionMin.NAME, SQLFunctionMin.class);
    register(SQLFunctionInterval.NAME, SQLFunctionInterval.class);
    register(SQLFunctionSet.NAME, SQLFunctionSet.class);
    register(SQLFunctionSysdate.NAME, SQLFunctionSysdate.class);
    register(SQLFunctionSum.NAME, SQLFunctionSum.class);
    register(SQLFunctionUnionAll.NAME, SQLFunctionUnionAll.class);
    register(SQLFunctionMode.NAME, SQLFunctionMode.class);
    register(SQLFunctionPercentile.NAME, SQLFunctionPercentile.class);
    register(SQLFunctionMedian.NAME, SQLFunctionMedian.class);
    register(SQLFunctionVariance.NAME, SQLFunctionVariance.class);
    register(SQLFunctionStandardDeviation.NAME, SQLFunctionStandardDeviation.class);
    register(SQLFunctionUUID.NAME, SQLFunctionUUID.class);
    register(SQLFunctionConcat.NAME, SQLFunctionConcat.class);
    register(SQLFunctionDecimal.NAME, SQLFunctionDecimal.class);
    register(SQLFunctionSequence.NAME, SQLFunctionSequence.class);
    register(SQLFunctionAbsoluteValue.NAME, SQLFunctionAbsoluteValue.class);
    register(SQLFunctionIndexKeySize.NAME, SQLFunctionIndexKeySize.class);
    register(SQLFunctionStrcmpci.NAME, SQLFunctionStrcmpci.class);
    register(SQLFunctionThrowCME.NAME, SQLFunctionThrowCME.class);
    // graph
    register(SQLFunctionOut.NAME, SQLFunctionOut.class);
    register(SQLFunctionIn.NAME, SQLFunctionIn.class);
    register(SQLFunctionBoth.NAME, SQLFunctionBoth.class);
    register(SQLFunctionOutE.NAME, SQLFunctionOutE.class);
    register(SQLFunctionOutV.NAME, SQLFunctionOutV.class);
    register(SQLFunctionInE.NAME, SQLFunctionInE.class);
    register(SQLFunctionInV.NAME, SQLFunctionInV.class);
    register(SQLFunctionBothE.NAME, SQLFunctionBothE.class);
    register(SQLFunctionBothV.NAME, SQLFunctionBothV.class);
    register(SQLFunctionShortestPath.NAME, SQLFunctionShortestPath.class);
    register(SQLFunctionDijkstra.NAME, SQLFunctionDijkstra.class);
    register(SQLFunctionAstar.NAME, SQLFunctionAstar.class);
  }
}
