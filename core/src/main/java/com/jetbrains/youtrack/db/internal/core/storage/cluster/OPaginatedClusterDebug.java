package com.jetbrains.youtrack.db.internal.core.storage.cluster;

import java.util.List;

public class OPaginatedClusterDebug {

  public long clusterPosition;
  public List<OClusterPageDebug> pages;
  public boolean empty;
  public int contentSize;
  public long fileId;
}