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

package com.orientechnologies.lucene.test;

import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.executor.YTResultSet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneBooleanIndexTest extends BaseLuceneTest {

  @Before
  public void init() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass v = schema.getClass("V");
    YTClass song = schema.createClass("Person");
    song.setSuperClass(db, v);
    song.createProperty(db, "isDeleted", YTType.BOOLEAN);

    db.command("create index Person.isDeleted on Person (isDeleted) FULLTEXT ENGINE LUCENE")
        .close();
  }

  @Test
  public void insertPerson() {

    for (int i = 0; i < 1000; i++) {
      YTDocument doc = new YTDocument("Person");
      doc.field("isDeleted", i % 2 == 0);
      db.begin();
      db.save(doc);
      db.commit();
    }

    YTResultSet docs = db.query("select from Person where isDeleted lucene false");

    Assert.assertEquals(
        500, docs.stream().filter((doc) -> !((Boolean) doc.getProperty("isDeleted"))).count());
    docs = db.query("select from Person where isDeleted lucene true");
    Assert.assertEquals(500, docs.stream().filter((doc) -> doc.getProperty("isDeleted")).count());
  }

  @Test
  public void testMemoryIndex() throws ParseException {
    // TODO To be used in evaluate Record
    MemoryIndex index = new MemoryIndex();

    Document doc = new Document();
    doc.add(new StringField("text", "my text", Field.Store.YES));
    StandardAnalyzer analyzer = new StandardAnalyzer();

    for (IndexableField field : doc.getFields()) {
      index.addField(field.name(), field.stringValue(), analyzer);
    }

    QueryParser parser = new QueryParser("text", analyzer);
    float score = index.search(parser.parse("+text:my"));
  }
}
