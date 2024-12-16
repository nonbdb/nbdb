package com.jetbrains.youtrack.db.internal.core.sql.executor.metadata;

import java.util.ArrayList;
import java.util.List;

public class MetadataPath {

  private final List<String> path = new ArrayList<>();

  public MetadataPath(String value) {
    this.path.add(value);
  }

  public void addPre(String value) {
    this.path.add(0, value);
  }

  public List<String> getPath() {
    return path;
  }
}
