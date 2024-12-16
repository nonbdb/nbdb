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
package com.jetbrains.youtrack.db.internal.common.listener;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;

/**
 * Listener interface called on task execution.
 */
public interface ProgressListener {

  void onBegin(Object iTask, long iTotal, Object iMetadata);

  boolean onProgress(Object iTask, long iCounter, float iPercent);

  void onCompletition(DatabaseSessionInternal session, Object iTask, boolean iSucceed);
}
