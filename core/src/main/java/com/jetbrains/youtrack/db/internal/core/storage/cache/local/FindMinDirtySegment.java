package com.jetbrains.youtrack.db.internal.core.storage.cache.local;

import java.util.concurrent.Callable;

final class FindMinDirtySegment implements Callable<Long> {

  /**
   *
   */
  private final WOWCache cache;

  /**
   * @param WOWCache
   */
  FindMinDirtySegment(WOWCache WOWCache) {
    cache = WOWCache;
  }

  @Override
  public Long call() {
    return cache.executeFindDirtySegment();
  }
}
