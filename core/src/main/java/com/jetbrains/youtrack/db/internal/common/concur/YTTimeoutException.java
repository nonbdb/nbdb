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
package com.jetbrains.youtrack.db.internal.common.concur;

import com.jetbrains.youtrack.db.internal.common.exception.YTSystemException;

/**
 * Timeout exception. The acquiring of a shared resource caused a timeout.
 */
public class YTTimeoutException extends YTSystemException {

  private static final long serialVersionUID = 1L;

  public YTTimeoutException(YTTimeoutException exception) {
    super(exception);
  }

  public YTTimeoutException(final String message) {
    super(message);
  }
}