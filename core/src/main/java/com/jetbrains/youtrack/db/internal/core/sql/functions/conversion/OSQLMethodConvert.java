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
package com.jetbrains.youtrack.db.internal.core.sql.functions.conversion;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.sql.method.misc.OAbstractSQLMethod;
import java.util.Locale;

/**
 * Converts a value to another type in Java or YouTrackDB's supported types.
 */
public class OSQLMethodConvert extends OAbstractSQLMethod {

  public static final String NAME = "convert";

  public OSQLMethodConvert() {
    super(NAME, 1, 1);
  }

  @Override
  public String getSyntax() {
    return "convert(<type>)";
  }

  @Override
  public Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      CommandContext iContext,
      Object ioResult,
      Object[] iParams) {
    if (iThis == null || iParams[0] == null) {
      return null;
    }

    final String destType = iParams[0].toString();

    var db = iContext.getDatabase();
    if (destType.contains(".")) {
      try {
        return YTType.convert(db, iThis, Class.forName(destType));
      } catch (ClassNotFoundException e) {
        LogManager.instance().error(this, "Class for destination type was not found", e);
      }
    } else {
      final YTType orientType = YTType.valueOf(destType.toUpperCase(Locale.ENGLISH));
      if (orientType != null) {
        return YTType.convert(db, iThis, orientType.getDefaultJavaType());
      }
    }

    return null;
  }
}
