/*
 *
 * Copyright 2013 Geomatys.
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
package com.jetbrains.youtrack.db.internal.core.sql.method.sequence;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.exception.YTDatabaseException;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.YTSequence;
import com.jetbrains.youtrack.db.internal.core.sql.YTCommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.method.misc.OAbstractSQLMethod;

/**
 * Reset a sequence. It returns the first sequence number after reset.
 */
public class OSQLMethodReset extends OAbstractSQLMethod {

  public static final String NAME = "reset";

  public OSQLMethodReset() {
    super(NAME, 0, 0);
  }

  @Override
  public String getSyntax() {
    return "reset()";
  }

  @Override
  public Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      CommandContext iContext,
      Object ioResult,
      Object[] iParams) {
    if (iThis == null) {
      throw new YTCommandSQLParsingException(
          "Method 'reset()' can be invoked only on OSequence instances, while NULL was found");
    }

    if (!(iThis instanceof YTSequence)) {
      throw new YTCommandSQLParsingException(
          "Method 'reset()' can be invoked only on OSequence instances, while '"
              + iThis.getClass()
              + "' was found");
    }

    try {
      return ((YTSequence) iThis).reset();
    } catch (YTDatabaseException exc) {
      String message = "Unable to execute command: " + exc.getMessage();
      LogManager.instance().error(this, message, exc, (Object) null);
      throw new YTCommandExecutionException(message);
    }
  }
}
