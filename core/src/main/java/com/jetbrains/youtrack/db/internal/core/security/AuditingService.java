/*
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
package com.jetbrains.youtrack.db.internal.core.security;

import com.jetbrains.youtrack.db.api.security.SecurityUser;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.io.IOException;

/**
 * Provides an interface the auditing service.
 */
public interface AuditingService extends SecurityComponent {

  void changeConfig(DatabaseSessionInternal session, SecurityUser user,
      final String databaseName,
      final EntityImpl cfg)
      throws IOException;

  EntityImpl getConfig(final String databaseName);

  void log(DatabaseSessionInternal session, final AuditingOperation operation,
      final String message);

  void log(DatabaseSessionInternal session, final AuditingOperation operation,
      SecurityUser user,
      final String message);

  void log(
      DatabaseSessionInternal session, final AuditingOperation operation,
      final String dbName,
      SecurityUser user,
      final String message);
}
