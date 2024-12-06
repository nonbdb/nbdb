/*
 * Copyright 2010-2012 henryzhao81-at-gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.youtrack.db.internal.core.db.record;

import com.jetbrains.youtrack.db.internal.core.config.StorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.BinarySerializerFactory;

/**
 * The factory that defines a set of components that current database should use to be compatible to
 * current version of storage. So if you open a database create with old version of YouTrackDB it
 * defines a components that should be used to provide backward compatibility with that version of
 * database.
 *
 * @since 2/14/14
 */
public class CurrentStorageComponentsFactory {

  public final int binaryFormatVersion;
  public final BinarySerializerFactory binarySerializerFactory;

  public CurrentStorageComponentsFactory(StorageConfiguration configuration) {
    this.binaryFormatVersion = configuration.getBinaryFormatVersion();

    binarySerializerFactory = BinarySerializerFactory.create(binaryFormatVersion);
  }

  /**
   * @return Whether class of is detected by cluster id or it is taken from documents serialized
   * content.
   * @since 1.7
   */
  public boolean classesAreDetectedByClusterId() {
    return binaryFormatVersion >= 10;
  }
}
