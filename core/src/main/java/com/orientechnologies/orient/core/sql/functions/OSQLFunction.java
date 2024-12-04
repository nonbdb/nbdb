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
package com.orientechnologies.orient.core.sql.functions;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import java.util.List;

/**
 * Interface that defines a SQL Function. Functions can be state-less if registered as instance, or
 * state-full when registered as class. State-less function are reused across queries, so don't keep
 * any run-time information inside of it. State-full function, instead, stores Implement it and
 * register it with: <code>OSQLParser.getInstance().registerFunction()</code> to being used by the
 * SQL engine.
 *
 * <p>??? could it be possible to have a small piece of code here showing where to register a
 * function using services ???
 */
public interface OSQLFunction {

  /**
   * Process a record.
   *
   * @param iThis
   * @param iCurrentRecord : current record
   * @param iCurrentResult TODO
   * @param iParams        : function parameters, number is ensured to be within minParams and
   *                       maxParams.
   * @param iContext       : object calling this function
   * @return function result, can be null. Special cases : can be null if function aggregate
   * results, can be null if function filter results : this mean result is excluded
   */
  Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] iParams,
      OCommandContext iContext);

  /**
   * Configure the function.
   *
   * @param configuredParameters
   */
  void config(Object[] configuredParameters);

  /**
   * A function can make calculation on several records before returning a result.
   *
   * <p>Example of such function : sum, count, max, min ...
   *
   * <p>The final result of the aggregation is obtain by calling {@link #getResult() }
   *
   * @return true if function aggregate results
   */
  boolean aggregateResults();

  /**
   * A function can act both as transformation or filtering records. If the function may reduce the
   * number final records than it must return true.
   *
   * <p>Function should return null for the {@linkplain #execute(Object, YTIdentifiable, Object,
   * Object[], OCommandContext) execute} method if the record must be excluded.
   *
   * @return true if the function acts as a record filter.
   */
  boolean filterResult();

  /**
   * Function name, the name is used by the sql parser to identify a call this function.
   *
   * @return String , function name, never null or empty.
   */
  String getName(YTDatabaseSession session);

  /**
   * Minimum number of parameter this function must have.
   *
   * @return minimum number of parameters
   */
  int getMinParams();

  /**
   * Maximum number of parameter this function can handle.
   *
   * @return maximum number of parameters ??? -1 , negative or Integer.MAX_VALUE for unlimited ???
   */
  int getMaxParams(YTDatabaseSession session);

  /**
   * Returns a convenient SQL String representation of the function.
   *
   * <p>Example :
   *
   * <pre>
   *  myFunction( param1, param2, [optionalParam3])
   * </pre>
   *
   * <p>This text will be used in exception messages.
   *
   * @return String , never null.
   */
  String getSyntax(YTDatabaseSession session);

  /**
   * Only called when function aggregates results after all records have been passed to the
   * function.
   *
   * @return Aggregation result
   */
  Object getResult();

  /**
   * Called by OCommandExecutor, given parameter is the number of results. ??? strange ???
   *
   * @param iResult
   */
  void setResult(Object iResult);

  /**
   * This method correspond to distributed query execution
   *
   * @return {@code true} if results that comes from different nodes need to be merged to obtain
   * valid one, {@code false} otherwise
   */
  boolean shouldMergeDistributedResult();

  /**
   * This method correspond to distributed query execution
   *
   * @param resultsToMerge is the results that comes from different nodes
   * @return is the valid merged result
   */
  Object mergeDistributedResult(List<Object> resultsToMerge);
}
