/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.orientechnologies.spatial.functions;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunctionAbstract;
import com.orientechnologies.spatial.shape.OShapeFactory;
import org.locationtech.spatial4j.shape.Shape;

/**
 *
 */
public class OSTEnvelopFunction extends SQLFunctionAbstract {

  public static final String NAME = "ST_Envelope";

  private final OShapeFactory factory = OShapeFactory.INSTANCE;

  public OSTEnvelopFunction() {
    super(NAME, 1, 1);
  }

  @Override
  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] iParams,
      CommandContext iContext) {
    Shape shape = factory.fromObject(iParams[0]);
    return factory.toDoc(shape.getBoundingBox());
  }

  @Override
  public String getSyntax(DatabaseSession session) {
    return "ST_AsBinary(<doc>)";
  }
}
