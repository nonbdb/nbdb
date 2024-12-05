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
package com.orientechnologies.orient.core.sql;

import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import java.util.Collection;
import java.util.Map;

/**
 * Explains the execution of a command returning profiling information.
 */
public class OCommandExecutorSQLExplain extends OCommandExecutorSQLDelegate {

  public static final String KEYWORD_EXPLAIN = "EXPLAIN";

  @Override
  public OCommandExecutorSQLExplain parse(OCommandRequest iCommand) {
    final OCommandRequestText textRequest = (OCommandRequestText) iCommand;

    String queryText = textRequest.getText();
    String originalQuery = queryText;
    try {
      queryText = preParse(queryText, iCommand);
      textRequest.setText(queryText);

      final String cmd = ((OCommandRequestText) iCommand).getText();
      var command = new OCommandSQL(cmd.substring(KEYWORD_EXPLAIN.length()));
      var context = new OBasicCommandContext();
      context.setParent(iCommand.getContext());

      command.setContext(context);

      super.parse(command);
    } finally {
      textRequest.setText(originalQuery);
    }
    return this;
  }

  @Override
  public Object execute(Map<Object, Object> iArgs, YTDatabaseSessionInternal querySession) {
    delegate.getContext().setRecordingMetrics(true);

    final long startTime = System.nanoTime();

    final Object result = super.execute(iArgs, querySession);
    final YTEntityImpl report = new YTEntityImpl(delegate.getContext().getVariables());

    report.field("elapsed", (System.nanoTime() - startTime) / 1000000f);

    if (result instanceof Collection<?>) {
      report.field("resultType", "collection");
      report.field("resultSize", ((Collection<?>) result).size());
    } else if (result instanceof YTEntityImpl) {
      report.field("resultType", "document");
      report.field("resultSize", 1);
    } else if (result instanceof Number) {
      report.field("resultType", "number");
    }

    return report;
  }

  @Override
  public QUORUM_TYPE getQuorumType() {
    return QUORUM_TYPE.READ;
  }

  @Override
  public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
    return DISTRIBUTED_EXECUTION_MODE.REPLICATE;
  }

  @Override
  public DISTRIBUTED_RESULT_MGMT getDistributedResultManagement() {
    return DISTRIBUTED_RESULT_MGMT.MERGE;
  }

  @Override
  public boolean isCacheable() {
    return false;
  }
}
