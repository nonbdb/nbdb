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

package com.orientechnologies.orient.core.conflict;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.storage.OStorage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Record conflict strategy that check the records content: if content is the same, se the higher
 * version number.
 */
public class OContentRecordConflictStrategy extends OVersionRecordConflictStrategy {

  public static final String NAME = "content";

  @Override
  public byte[] onUpdate(
      OStorage storage,
      final byte iRecordType,
      final ORecordId rid,
      final int iRecordVersion,
      final byte[] iRecordContent,
      final AtomicInteger iDatabaseVersion) {

    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return NAME;
  }
}
