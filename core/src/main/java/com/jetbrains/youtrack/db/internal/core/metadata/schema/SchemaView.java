package com.jetbrains.youtrack.db.internal.core.metadata.schema;

import java.util.List;
import java.util.Set;

public interface SchemaView extends SchemaClass {

  String getQuery();

  int getUpdateIntervalSeconds();

  List<String> getWatchClasses();

  String getOriginRidField();

  boolean isUpdatable();

  List<String> getNodes();

  List<ViewIndexConfig> getRequiredIndexesInfo();

  String getUpdateStrategy();

  Set<String> getActiveIndexNames();

  long getLastRefreshTime();
}
