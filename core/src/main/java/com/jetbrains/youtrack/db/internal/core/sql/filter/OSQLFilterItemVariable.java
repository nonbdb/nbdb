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
package com.jetbrains.youtrack.db.internal.core.sql.filter;

import com.jetbrains.youtrack.db.internal.common.parser.BaseParser;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;

/**
 * Represents a context variable as value in the query condition.
 */
public class OSQLFilterItemVariable extends OSQLFilterItemAbstract {

  protected String name;

  public OSQLFilterItemVariable(YTDatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iName) {
    super(session, iQueryToParse, iName.substring(1));
  }

  public Object getValue(
      final YTIdentifiable iRecord, Object iCurrentResult, final CommandContext iContext) {
    if (iContext == null) {
      return null;
    }

    return transformValue(iRecord, iContext, iContext.getVariable(name));
  }

  public String getRoot(YTDatabaseSession session) {
    return name;
  }

  public void setRoot(YTDatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iRoot) {
    this.name = iRoot;
  }

  @Override
  public String toString() {
    return "$" + super.toString();
  }
}
