/*
 *
 *
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.orientechnologies.lucene.tests;

import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.OIndex;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTSchema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class OLuceneMassiveInsertDeleteTest extends OLuceneBaseTest {

  @Before
  public void init() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass song = db.createVertexClass("City");
    song.createProperty(db, "name", YTType.STRING);

    db.command("create index City.name on City (name) FULLTEXT ENGINE LUCENE");
  }

  @Test
  public void loadCloseDelete() {

    int size = 1000;
    for (int i = 0; i < size; i++) {
      Vertex city = db.newVertex("City");
      city.setProperty("name", "Rome " + i);

      db.begin();
      db.save(city);
      db.commit();
    }
    String query = "select * from City where search_class('name:Rome')=true";
    YTResultSet docs = db.query(query);
    Assertions.assertThat(docs).hasSize(size);
    docs.close();
    db.close();

    db = (YTDatabaseSessionInternal) pool.acquire();
    docs = db.query(query);
    Assertions.assertThat(docs).hasSize(size);
    docs.close();

    db.begin();
    db.command("delete vertex City");
    db.commit();

    docs = db.query(query);
    Assertions.assertThat(docs).hasSize(0);
    docs.close();
    db.close();
    db = (YTDatabaseSessionInternal) pool.acquire();
    docs = db.query(query);
    Assertions.assertThat(docs).hasSize(0);
    docs.close();
    db.getMetadata().reload();

    db.begin();
    OIndex idx = db.getMetadata().getSchema().getClass("City").getClassIndex(db, "City.name");
    Assert.assertEquals(0, idx.getInternal().size(db));
    db.commit();
  }
}
