package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class InfoExecutionStep implements ExecutionStep {

  private String name;
  private String type;
  private String javaType;
  private String targetNode;
  private String description;
  private long cost;
  private final List<ExecutionStep> subSteps = new ArrayList<>();

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getTargetNode() {
    return targetNode;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public List<ExecutionStep> getSubSteps() {
    return subSteps;
  }

  @Override
  public long getCost() {
    return cost;
  }

  @Override
  public Result toResult(DatabaseSessionInternal db) {
    return null;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setTargetNode(String targetNode) {
    this.targetNode = targetNode;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCost(long cost) {
    this.cost = cost;
  }

  public String getJavaType() {
    return javaType;
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }
}
