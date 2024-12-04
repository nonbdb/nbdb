/*
 *
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <p>
 * *
 */
package com.orientechnologies.spatial;

import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.document.YTDatabaseDocumentTx;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.spatial.collections.OSpatialCompositeKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class LuceneSpatialMemoryTest {

  @Test
  public void boundingBoxTest() {
    //noinspection deprecation
    try (YTDatabaseSessionInternal db = new YTDatabaseDocumentTx("memory:test")) {
      db.create();
      try {

        YTClass point = db.getMetadata().getSchema().createClass("Point");
        point.createProperty(db, "latitude", YTType.DOUBLE);
        point.createProperty(db, "longitude", YTType.DOUBLE);

        db.command("CREATE INDEX Point.ll ON Point(latitude,longitude) SPATIAL ENGINE LUCENE")
            .close();

        YTDocument document = new YTDocument("Point");

        document.field("latitude", 42.2814837);
        document.field("longitude", -83.7605452);

        db.begin();
        db.save(document);
        db.commit();

        List<?> query =
            db.query(
                new OSQLSynchQuery<YTDocument>(
                    "SELECT FROM Point WHERE [latitude, longitude] WITHIN"
                        + " [[42.26531323615103,-83.71986351411135],[42.29239784478525,-83.7662120858887]]"));

        Assert.assertEquals(query.size(), 1);
      } finally {
        db.drop();
      }
    }
  }

  @Test
  public void boundingBoxTestTxRollBack() {

    @SuppressWarnings("deprecation")
    YTDatabaseSessionInternal db = new YTDatabaseDocumentTx("memory:test");
    db.create();
    try {

      YTClass point = db.getMetadata().getSchema().createClass("Point");
      point.createProperty(db, "latitude", YTType.DOUBLE);
      point.createProperty(db, "longitude", YTType.DOUBLE);

      db.command("CREATE INDEX Point.ll ON Point(latitude,longitude) SPATIAL ENGINE LUCENE")
          .close();

      db.begin();

      YTDocument document = new YTDocument("Point");

      document.field("latitude", 42.2814837);
      document.field("longitude", -83.7605452);

      db.begin();
      db.save(document);
      db.commit();

      List<?> query =
          db.query(
              new OSQLSynchQuery<YTDocument>(
                  "SELECT FROM Point WHERE [latitude, longitude] WITHIN"
                      + " [[42.26531323615103,-83.71986351411135],[42.29239784478525,-83.7662120858887]]"));

      Assert.assertEquals(1, query.size());

      OSpatialCompositeKey oSpatialCompositeKey =
          new OSpatialCompositeKey(
              new ArrayList<List<Number>>() {
                {
                  add(
                      new ArrayList<Number>() {
                        {
                          add(42.26531323615103);
                          add(-83.71986351411135);
                        }
                      });
                  add(
                      new ArrayList<Number>() {
                        {
                          add(42.29239784478525);
                          add(-83.7662120858887);
                        }
                      });
                }
              })
              .setOperation(SpatialOperation.IsWithin);
      OIndex index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Point.ll");

      var baseContext = new OBasicCommandContext();
      baseContext.setDatabase(db);
      oSpatialCompositeKey.setContext(baseContext);

      Collection coll;
      try (Stream<YTRID> stream = index.getInternal().getRids(db, oSpatialCompositeKey)) {
        coll = stream.collect(Collectors.toList());
      }
      Assert.assertEquals(1, coll.size());
      db.rollback();

      query =
          db.query(
              new OSQLSynchQuery<YTDocument>(
                  "SELECT FROM Point WHERE [latitude, longitude] WITHIN"
                      + " [[42.26531323615103,-83.71986351411135],[42.29239784478525,-83.7662120858887]]"));

      Assert.assertEquals(0, query.size());

    } finally {
      db.drop();
    }
  }

  @Test
  public void boundingBoxTestTxCommit() {

    YTDatabaseSessionInternal db = new YTDatabaseDocumentTx("memory:test");

    db.create();

    try {

      YTClass point = db.getMetadata().getSchema().createClass("Point");
      point.createProperty(db, "latitude", YTType.DOUBLE);
      point.createProperty(db, "longitude", YTType.DOUBLE);

      db.command("CREATE INDEX Point.ll ON Point(latitude,longitude) SPATIAL ENGINE LUCENE")
          .close();

      db.begin();

      YTDocument document = new YTDocument("Point");

      document.field("latitude", 42.2814837);
      document.field("longitude", -83.7605452);

      db.save(document);

      db.commit();

      List<?> query =
          db.query(
              new OSQLSynchQuery<YTDocument>(
                  "SELECT FROM Point WHERE [latitude, longitude] WITHIN"
                      + " [[42.26531323615103,-83.71986351411135],[42.29239784478525,-83.7662120858887]]"));

      Assert.assertEquals(1, query.size());

      OSpatialCompositeKey oSpatialCompositeKey =
          new OSpatialCompositeKey(
              new ArrayList<List<Number>>() {
                {
                  add(
                      new ArrayList<Number>() {
                        {
                          add(42.26531323615103);
                          add(-83.71986351411135);
                        }
                      });
                  add(
                      new ArrayList<Number>() {
                        {
                          add(42.29239784478525);
                          add(-83.7662120858887);
                        }
                      });
                }
              })
              .setOperation(SpatialOperation.IsWithin);
      var context = new OBasicCommandContext();
      context.setDatabase(db);
      oSpatialCompositeKey.setContext(context);

      OIndex index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Point.ll");

      Collection coll;
      try (Stream<YTRID> stream = index.getInternal().getRids(db, oSpatialCompositeKey)) {
        coll = stream.collect(Collectors.toList());
      }
      Assert.assertEquals(1, coll.size());

      db.begin();

      document = db.bindToSession(document);
      db.delete(document);

      query =
          db.query(
              new OSQLSynchQuery<YTDocument>(
                  "SELECT FROM Point WHERE [latitude, longitude] WITHIN"
                      + " [[42.26531323615103,-83.71986351411135],[42.29239784478525,-83.7662120858887]]"));

      Assert.assertEquals(0, query.size());

      try (Stream<YTRID> stream = index.getInternal().getRids(db, oSpatialCompositeKey)) {
        coll = stream.collect(Collectors.toList());
      }

      Assert.assertEquals(0, coll.size());

      db.rollback();

      query =
          db.query(
              new OSQLSynchQuery<YTDocument>(
                  "SELECT FROM Point WHERE [latitude, longitude] WITHIN"
                      + " [[42.26531323615103,-83.71986351411135],[42.29239784478525,-83.7662120858887]]"));

      Assert.assertEquals(1, query.size());

    } finally {
      db.drop();
    }
  }
}
