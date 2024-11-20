package com.orientechnologies.orient.client.remote.db.document;

import com.orientechnologies.orient.core.db.OLiveQueryMonitor;

/**
 * Created by tglman on 16/05/17.
 */
public class OLiveQueryMonitorRemote implements OLiveQueryMonitor {

  private final ODatabaseDocumentRemote database;
  private final int monitorId;

  public OLiveQueryMonitorRemote(ODatabaseDocumentRemote database, int monitorId) {
    this.database = database;
    this.monitorId = monitorId;
  }

  @Override
  public void unSubscribe() {
    database.getStorageRemote().unsubscribeLive(database, this.monitorId);
  }

  @Override
  public int getMonitorId() {
    return monitorId;
  }
}
