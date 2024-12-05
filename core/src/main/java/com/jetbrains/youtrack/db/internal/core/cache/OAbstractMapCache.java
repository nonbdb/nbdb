/*
 *
 *
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

package com.jetbrains.youtrack.db.internal.core.cache;

import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public abstract class OAbstractMapCache<T extends Map<YTRID, ?>> implements ORecordCache {

  protected T cache;

  private boolean enabled = true;

  public OAbstractMapCache(T cache) {
    this.cache = cache;
  }

  @Override
  public void startup() {
  }

  @Override
  public void shutdown() {
    cache.clear();
  }

  @Override
  public void clear() {
    cache.clear();
  }

  @Override
  public int size() {
    return cache.size();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean disable() {
    return enabled = false;
  }

  @Override
  public boolean enable() {
    return enabled = true;
  }

  @Override
  public Collection<YTRID> keys() {
    return new ArrayList<>(cache.keySet());
  }
}
