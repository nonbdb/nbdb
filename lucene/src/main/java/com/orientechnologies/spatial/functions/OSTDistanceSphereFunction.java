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
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OBinaryCompareOperator;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OFromClause;
import com.orientechnologies.spatial.shape.OShapeFactory;
import com.orientechnologies.spatial.strategy.SpatialQueryBuilderDistanceSphere;
import java.util.Map;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Shape;

/**
 *
 */
public class OSTDistanceSphereFunction extends OSpatialFunctionAbstractIndexable {

  public static final String NAME = "st_distance_sphere";
  private final OShapeFactory factory = OShapeFactory.INSTANCE;

  public OSTDistanceSphereFunction() {
    super(NAME, 2, 2);
  }

  @Override
  public Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] iParams,
      CommandContext iContext) {

    if (containsNull(iParams)) {
      return null;
    }

    Shape shape = toShape(iParams[0]);
    Shape shape1 = toShape(iParams[1]);

    if (shape == null || shape1 == null) {
      return null;
    }

    double distance =
        factory.context().getDistCalc().distance(shape.getCenter(), shape1.getCenter());
    final double docDistInKM =
        DistanceUtils.degrees2Dist(distance, DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM);
    return docDistInKM * 1000;
  }

  @Override
  public String getSyntax(YTDatabaseSession session) {
    return null;
  }

  @Override
  protected String operator() {
    return SpatialQueryBuilderDistanceSphere.NAME;
  }

  @Override
  public Iterable<YTIdentifiable> searchFromTarget(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      OExpression... args) {
    return results(target, args, ctx, rightValue);
  }

  @Override
  public long estimate(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      OExpression... args) {

    if (rightValue == null || !isValidBinaryOperator(operator)) {
      return -1;
    }

    return super.estimate(target, operator, rightValue, ctx, args);
  }

  @Override
  protected void onAfterParsing(
      Map<String, Object> params, OExpression[] args, CommandContext ctx, Object rightValue) {

    Number parsedNumber = (Number) rightValue;

    params.put("distance", parsedNumber.doubleValue());
  }
}
