/*
 * Copyright 2018 YouTrackDB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.lucene.engine;

import org.apache.lucene.store.Directory;

/**
 *
 */
public class LuceneDirectory {

  private final Directory dir;
  private final String path;

  public LuceneDirectory(final Directory dir, final String path) {
    this.dir = dir;
    this.path = path;
  }

  public Directory getDirectory() {
    return dir;
  }

  public String getPath() {
    return path;
  }
}
