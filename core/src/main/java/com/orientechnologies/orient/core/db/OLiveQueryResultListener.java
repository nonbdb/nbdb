package com.orientechnologies.orient.core.db;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.sql.executor.OResult;

/**
 *
 */
public interface OLiveQueryResultListener {

  void onCreate(ODatabaseSession database, OResult data);

  void onUpdate(ODatabaseSession database, OResult before, OResult after);

  void onDelete(ODatabaseSession database, OResult data);

  void onError(ODatabaseSession database, OException exception);

  void onEnd(ODatabaseSession database);
}
