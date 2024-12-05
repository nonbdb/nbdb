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
package com.jetbrains.youtrack.db.internal.core.sql.functions.misc;

import com.jetbrains.youtrack.db.internal.core.command.OCommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.sql.functions.OSQLFunctionAbstract;

/**
 *
 */
public class OSQLFunctionStrcmpci extends OSQLFunctionAbstract {

  public static final String NAME = "strcmpci";

  public OSQLFunctionStrcmpci() {
    super(NAME, 2, 2);
  }

  @Override
  public Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] iParams,
      OCommandContext iContext) {
    String s1 = null;
    String s2 = null;

    if (iParams[0] != null && iParams[0] instanceof String) {
      s1 = (String) iParams[0];
    }

    if (iParams[1] != null && iParams[1] instanceof String) {
      s2 = (String) iParams[1];
    }

    if (s1 == null && s2 == null) {
      return 0;
    }

    if (s1 == null) {
      return -1;
    }

    if (s2 == null) {
      return 1;
    }

    int res = s1.compareToIgnoreCase(s2);
    if (res != 0) {
      // normalize res to -1, 0, 1
      res = res / Math.abs(res);
    }
    return res;
  }

  @Override
  public String getSyntax(YTDatabaseSession session) {
    return "strcmpci(<arg1>, <arg2>)";
  }
}
