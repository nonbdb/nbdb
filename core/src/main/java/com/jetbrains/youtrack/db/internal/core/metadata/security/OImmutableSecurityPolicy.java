package com.jetbrains.youtrack.db.internal.core.metadata.security;

import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import javax.annotation.Nonnull;

public class OImmutableSecurityPolicy implements OSecurityPolicy {

  private final YTRID identity;
  private final String name;
  private final boolean active;
  private final String create;
  private final String read;
  private final String beforeUpdate;
  private final String afterUpdate;
  private final String delete;
  private final String execute;

  public OImmutableSecurityPolicy(@Nonnull YTDatabaseSessionInternal db, OSecurityPolicy element) {
    this.identity = element.getIdentity();
    this.name = element.getName(db);
    this.active = element.isActive(db);
    this.create = element.getCreateRule(db);
    this.read = element.getReadRule(db);
    this.beforeUpdate = element.getBeforeUpdateRule(db);
    this.afterUpdate = element.getAfterUpdateRule(db);
    this.delete = element.getDeleteRule(db);
    this.execute = element.getExecuteRule(db);
  }

  public OImmutableSecurityPolicy(
      String name,
      String create,
      String read,
      String beforeUpdate,
      String afterUpdate,
      String delete,
      String execute) {
    super();
    this.identity = new YTRecordId(-1, -1);
    this.active = true;
    this.name = name;
    this.create = create;
    this.read = read;
    this.beforeUpdate = beforeUpdate;
    this.afterUpdate = afterUpdate;
    this.delete = delete;
    this.execute = execute;
  }

  @Override
  public String getName(@Nonnull YTDatabaseSessionInternal db) {
    return name;
  }

  @Override
  public boolean isActive(@Nonnull YTDatabaseSessionInternal db) {
    return active;
  }

  @Override
  public String getCreateRule(@Nonnull YTDatabaseSessionInternal db) {
    return create;
  }

  @Override
  public String getReadRule(@Nonnull YTDatabaseSessionInternal db) {
    return read;
  }

  @Override
  public String getBeforeUpdateRule(@Nonnull YTDatabaseSessionInternal db) {
    return beforeUpdate;
  }

  @Override
  public String getAfterUpdateRule(@Nonnull YTDatabaseSessionInternal db) {
    return afterUpdate;
  }

  @Override
  public String getDeleteRule(@Nonnull YTDatabaseSessionInternal db) {
    return delete;
  }

  @Override
  public String getExecuteRule(@Nonnull YTDatabaseSessionInternal db) {
    return execute;
  }

  @Override
  public YTRID getIdentity() {
    return identity;
  }
}