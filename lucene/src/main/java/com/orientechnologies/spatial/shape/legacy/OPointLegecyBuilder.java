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
package com.orientechnologies.spatial.shape.legacy;

import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.OCompositeKey;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

/**
 *
 */
public class OPointLegecyBuilder implements OShapeBuilderLegacy<Point> {

  @Override
  public Point makeShape(YTDatabaseSessionInternal session, OCompositeKey key, SpatialContext ctx) {
    double lat = ((Double) YTType.convert(session, key.getKeys().get(0),
        Double.class)).doubleValue();
    double lng = ((Double) YTType.convert(session, key.getKeys().get(1),
        Double.class)).doubleValue();
    return ctx.makePoint(lng, lat);
  }

  @Override
  public boolean canHandle(OCompositeKey key) {

    boolean canHandle = key.getKeys().size() == 2;
    for (Object o : key.getKeys()) {
      if (!(o instanceof Number)) {
        canHandle = false;
        break;
      }
    }
    return canHandle;
  }
}
