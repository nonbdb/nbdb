/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.query.SQLSynchQuery;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class GEOTest extends BaseDBTest {

  @Parameters(value = "remote")
  public GEOTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @Test
  public void geoSchema() {
    final SchemaClass mapPointClass = db.getMetadata().getSchema().createClass("MapPoint");
    mapPointClass.createProperty(db, "x", PropertyType.DOUBLE)
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);
    mapPointClass.createProperty(db, "y", PropertyType.DOUBLE)
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    final Set<Index> xIndexes =
        db.getMetadata().getSchema().getClassInternal("MapPoint")
            .getInvolvedIndexesInternal(db, "x");
    Assert.assertEquals(xIndexes.size(), 1);

    final Set<Index> yIndexes =
        db.getMetadata().getSchema().getClassInternal("MapPoint")
            .getInvolvedIndexesInternal(db, "y");
    Assert.assertEquals(yIndexes.size(), 1);
  }

  @Test(dependsOnMethods = "geoSchema")
  public void checkGeoIndexes() {
    final Set<Index> xIndexes =
        db.getMetadata().getSchema().getClassInternal("MapPoint").
            getInvolvedIndexesInternal(db, "x");
    Assert.assertEquals(xIndexes.size(), 1);

    final Set<Index> yIndexDefinitions =
        db.getMetadata().getSchema().getClassInternal("MapPoint")
            .getInvolvedIndexesInternal(db, "y");
    Assert.assertEquals(yIndexDefinitions.size(), 1);
  }

  @Test(dependsOnMethods = "checkGeoIndexes")
  public void queryCreatePoints() {
    EntityImpl point;

    for (int i = 0; i < 10000; ++i) {
      point = ((EntityImpl) db.newEntity());
      point.setClassName("MapPoint");

      point.field("x", (52.20472d + i / 100d));
      point.field("y", (0.14056d + i / 100d));

      db.begin();
      point.save();
      db.commit();
    }
  }

  @Test(dependsOnMethods = "queryCreatePoints")
  public void queryDistance() {
    Assert.assertEquals(db.countClass("MapPoint"), 10000);

    List<EntityImpl> result =
        db
            .command(
                new SQLSynchQuery<EntityImpl>(
                    "select from MapPoint where distance(x, y,52.20472, 0.14056 ) <= 30"))
            .execute(db);

    Assert.assertTrue(result.size() != 0);

    for (EntityImpl d : result) {
      Assert.assertEquals(d.getClassName(), "MapPoint");
      Assert.assertEquals(RecordInternal.getRecordType(db, d), EntityImpl.RECORD_TYPE);
    }
  }

  @Test(dependsOnMethods = "queryCreatePoints")
  public void queryDistanceOrdered() {
    Assert.assertEquals(db.countClass("MapPoint"), 10000);

    // MAKE THE FIRST RECORD DIRTY TO TEST IF DISTANCE JUMP IT
    List<EntityImpl> result =
        db.command(new SQLSynchQuery<EntityImpl>("select from MapPoint limit 1"))
            .execute(db);
    try {
      result.get(0).field("x", "--wrong--");
      Assert.fail();
    } catch (DatabaseException e) {
      Assert.assertTrue(true);
    }

    result =
        executeQuery(
            "select distance(x, y,52.20472, 0.14056 ) as distance from MapPoint order by"
                + " distance desc");

    Assert.assertTrue(result.size() != 0);

    Double lastDistance = null;
    for (EntityImpl d : result) {
      if (lastDistance != null && d.field("distance") != null) {
        Assert.assertTrue(((Double) d.field("distance")).compareTo(lastDistance) <= 0);
      }
      lastDistance = d.field("distance");
    }
  }
}