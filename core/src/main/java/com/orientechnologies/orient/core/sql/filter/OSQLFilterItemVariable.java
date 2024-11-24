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
package com.orientechnologies.orient.core.sql.filter;

import com.orientechnologies.common.parser.OBaseParser;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;

/**
 * Represents a context variable as value in the query condition.
 */
public class OSQLFilterItemVariable extends OSQLFilterItemAbstract {

  protected String name;

  public OSQLFilterItemVariable(ODatabaseSessionInternal session, final OBaseParser iQueryToParse,
      final String iName) {
    super(session, iQueryToParse, iName.substring(1));
  }

  public Object getValue(
      final OIdentifiable iRecord, Object iCurrentResult, final OCommandContext iContext) {
    if (iContext == null) {
      return null;
    }

    return transformValue(iRecord, iContext, iContext.getVariable(name));
  }

  public String getRoot(ODatabaseSession session) {
    return name;
  }

  public void setRoot(ODatabaseSessionInternal session, final OBaseParser iQueryToParse,
      final String iRoot) {
    this.name = iRoot;
  }

  @Override
  public String toString() {
    return "$" + super.toString();
  }
}
