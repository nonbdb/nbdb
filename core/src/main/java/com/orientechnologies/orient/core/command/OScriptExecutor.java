package com.orientechnologies.orient.core.command;

import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.Map;

/**
 *
 */
public interface OScriptExecutor {

  OResultSet execute(ODatabaseSessionInternal database, String script, Object... params);

  OResultSet execute(ODatabaseSessionInternal database, String script, Map params);

  Object executeFunction(
      OCommandContext context, final String functionName, final Map<Object, Object> iArgs);

  void registerInterceptor(OScriptInterceptor interceptor);

  void unregisterInterceptor(OScriptInterceptor interceptor);

  default void close(String iDatabaseName) {
  }

  default void closeAll() {
  }
}
