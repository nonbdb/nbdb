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
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.StringSerializerHelper;

/**
 * Represent one or more object fields as value in the query condition.
 */
public class SQLFilterItemFieldAll extends SQLFilterItemFieldMultiAbstract {

  public static final String NAME = "ALL";
  public static final String FULL_NAME = "ALL()";

  public SQLFilterItemFieldAll(
      DatabaseSessionInternal session, final SQLPredicate iQueryCompiled, final String iName,
      final SchemaClass iClass) {
    super(session, iQueryCompiled, iName, iClass, StringSerializerHelper.getParameters(iName));
  }

  @Override
  public String getRoot(DatabaseSession session) {
    return FULL_NAME;
  }

  @Override
  protected void setRoot(DatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iRoot) {
  }
}
