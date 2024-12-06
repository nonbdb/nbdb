/*
 *
 *  *  Copyright YouTrackDB
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
package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.internal.common.util.CallableFunction;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequestText;
import com.jetbrains.youtrack.db.internal.core.command.CommandResultListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.exception.SecurityException;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.metadata.security.RestrictedAccessHook;
import com.jetbrains.youtrack.db.internal.core.metadata.security.RestrictedOperation;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import com.jetbrains.youtrack.db.internal.core.query.live.LiveQueryHook;
import com.jetbrains.youtrack.db.internal.core.query.live.LiveQueryListener;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.query.LegacyResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.query.LiveResultListener;
import java.util.Map;
import java.util.Random;

/**
 *
 */
public class CommandExecutorSQLLiveSelect extends CommandExecutorSQLSelect
    implements LiveQueryListener {

  public static final String KEYWORD_LIVE_SELECT = "LIVE SELECT";
  private DatabaseSessionInternal execDb;
  private int token;
  private static final Random random = new Random();

  public CommandExecutorSQLLiveSelect() {
  }

  public Object execute(final Map<Object, Object> iArgs, DatabaseSessionInternal querySession) {
    try {
      final DatabaseSessionInternal db = getDatabase();
      execInSeparateDatabase(
          new CallableFunction() {
            @Override
            public Object call(Object iArgument) {
              return execDb = db.copy();
            }
          });

      synchronized (random) {
        token = random.nextInt(); // TODO do something better ;-)!
      }
      subscribeToLiveQuery(token, db);
      bindDefaultContextVariables();

      if (iArgs != null)
      // BIND ARGUMENTS INTO CONTEXT TO ACCESS FROM ANY POINT (EVEN FUNCTIONS)
      {
        for (Map.Entry<Object, Object> arg : iArgs.entrySet()) {
          context.setVariable(arg.getKey().toString(), arg.getValue());
        }
      }

      if (timeoutMs > 0) {
        getContext().beginExecution(timeoutMs, timeoutStrategy);
      }

      EntityImpl result = new EntityImpl();
      result.field("token", token); // TODO change this name...?

      ((LegacyResultSet) getResult(querySession)).add(result);
      return getResult(querySession);
    } finally {
      if (request != null && request.getResultListener() != null) {
        request.getResultListener().end();
      }
    }
  }

  private void subscribeToLiveQuery(Integer token, DatabaseSessionInternal db) {
    LiveQueryHook.subscribe(token, this, db);
  }

  public void onLiveResult(final RecordOperation iOp) {

    DatabaseSessionInternal oldThreadLocal = DatabaseRecordThreadLocal.instance().getIfDefined();
    execDb.activateOnCurrentThread();

    try {
      final Identifiable value = iOp.record;

      if (!matchesTarget(value)) {
        return;
      }
      if (!matchesFilters(value)) {
        return;
      }
      if (!checkSecurity(value)) {
        return;
      }
    } finally {
      if (oldThreadLocal == null) {
        DatabaseRecordThreadLocal.instance().remove();
      } else {
        DatabaseRecordThreadLocal.instance().set(oldThreadLocal);
      }
    }
    final CommandResultListener listener = request.getResultListener();
    if (listener instanceof LiveResultListener) {
      execInSeparateDatabase(
          new CallableFunction() {
            @Override
            public Object call(Object iArgument) {
              execDb.activateOnCurrentThread();
              ((LiveResultListener) listener).onLiveResult(token, iOp);
              return null;
            }
          });
    }
  }

  protected void execInSeparateDatabase(final CallableFunction iCallback) {
    final DatabaseSessionInternal prevDb = DatabaseRecordThreadLocal.instance().getIfDefined();
    try {
      iCallback.call(null);
    } finally {
      if (prevDb != null) {
        DatabaseRecordThreadLocal.instance().set(prevDb);
      } else {
        DatabaseRecordThreadLocal.instance().remove();
      }
    }
  }

  private boolean checkSecurity(Identifiable value) {
    try {
      // TODO check this!
      execDb.checkSecurity(
          Rule.ResourceGeneric.CLASS,
          Role.PERMISSION_READ,
          ((EntityImpl) value.getRecord()).getClassName());
    } catch (SecurityException ignore) {
      return false;
    }
    SecurityInternal security = execDb.getSharedContext().getSecurity();
    boolean allowedByPolicy = security.canRead(execDb, value.getRecord());
    return allowedByPolicy
        && RestrictedAccessHook.isAllowed(
        execDb, value.getRecord(), RestrictedOperation.ALLOW_READ, false);
  }

  private boolean matchesFilters(Identifiable value) {
    if (this.compiledFilter == null || this.compiledFilter.getRootCondition() == null) {
      return true;
    }
    if (!(value instanceof EntityImpl)) {
      value = value.getRecord();
    }
    return !(Boolean.FALSE.equals(
        compiledFilter.evaluate(value, (EntityImpl) value, getContext())));
  }

  private boolean matchesTarget(Identifiable value) {
    if (!(value instanceof EntityImpl)) {
      return false;
    }
    final String className = ((EntityImpl) value).getClassName();
    if (className == null) {
      return false;
    }
    final SchemaClass docClass = execDb.getMetadata().getSchema().getClass(className);
    if (docClass == null) {
      return false;
    }

    if (this.parsedTarget.getTargetClasses() != null) {
      for (String clazz : parsedTarget.getTargetClasses().keySet()) {
        if (docClass.isSubClassOf(clazz)) {
          return true;
        }
      }
    }
    if (this.parsedTarget.getTargetRecords() != null) {
      for (Identifiable r : parsedTarget.getTargetRecords()) {
        if (r.getIdentity().equals(value.getIdentity())) {
          return true;
        }
      }
    }
    if (this.parsedTarget.getTargetClusters() != null) {
      final String clusterName = execDb.getClusterNameById(value.getIdentity().getClusterId());
      if (clusterName != null) {
        for (String cluster : parsedTarget.getTargetClusters().keySet()) {
          if (clusterName.equalsIgnoreCase(cluster)) { // make it case insensitive in 3.0?
            return true;
          }
        }
      }
    }
    return false;
  }

  public void onLiveResultEnd() {
    if (request.getResultListener() instanceof LiveResultListener) {
      ((LiveResultListener) request.getResultListener()).onUnsubscribe(token);
    }

    if (execDb != null) {
      DatabaseSessionInternal oldThreadDB = DatabaseRecordThreadLocal.instance().getIfDefined();
      execDb.activateOnCurrentThread();
      execDb.close();
      if (oldThreadDB == null) {
        DatabaseRecordThreadLocal.instance().remove();
      } else {
        DatabaseRecordThreadLocal.instance().set(oldThreadDB);
      }
    }
  }

  @Override
  public CommandExecutorSQLSelect parse(final CommandRequest iRequest) {
    final CommandRequestText requestText = (CommandRequestText) iRequest;
    final String originalText = requestText.getText();
    final String remainingText = requestText.getText().trim().substring(5).trim();
    requestText.setText(remainingText);
    try {
      return super.parse(iRequest);
    } finally {
      requestText.setText(originalText);
    }
  }

  @Override
  public QUORUM_TYPE getQuorumType() {
    return QUORUM_TYPE.NONE;
  }
}
