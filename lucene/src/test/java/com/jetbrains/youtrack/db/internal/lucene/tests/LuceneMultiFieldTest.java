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

package com.jetbrains.youtrack.db.internal.lucene.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import java.io.InputStream;
import java.util.stream.Stream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneMultiFieldTest extends LuceneBaseTest {

  @Before
  public void init() throws Exception {
    try (InputStream stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql")) {
      //noinspection deprecation
      db.execute("sql", getScriptFromStream(stream)).close();
    }

    //noinspection resource
    db.command(
        "create index Song.title_author on Song (title,author) FULLTEXT ENGINE LUCENE METADATA {"
            + "\"title_index\":\""
            + EnglishAnalyzer.class.getName()
            + "\" , "
            + "\"title_query\":\""
            + EnglishAnalyzer.class.getName()
            + "\" , "
            + "\"author_index\":\""
            + StandardAnalyzer.class.getName()
            + "\"}");

    var index =
        db.getMetadata().getIndexManagerInternal().getIndex(db, "Song.title_author").getMetadata();

    assertThat(index.get("author_index")).isEqualTo(StandardAnalyzer.class.getName());
    assertThat(index.get("title_index")).isEqualTo(EnglishAnalyzer.class.getName());
  }

  @Test
  public void testSelectSingleDocumentWithAndOperator() {
    try (ResultSet docs =
        db.query(
            "select * from Song where  search_fields(['title','author'] ,'title:mountain AND"
                + " author:Fabbio')=true")) {

      assertThat(docs).hasSize(1);
    }
  }

  @Test
  public void testSelectMultipleDocumentsWithOrOperator() {
    try (ResultSet docs =
        db.query(
            "select * from Song where  search_fields(['title','author'] ,'title:mountain OR"
                + " author:Fabbio')=true")) {

      assertThat(docs).hasSize(91);
    }
  }

  @Test
  public void testSelectOnTitleAndAuthorWithMatchOnTitle() {
    try (ResultSet docs =
        db.query(
            "select * from  Song where search_fields(['title','author'] ,'title:mountain')=true")) {
      assertThat(docs).hasSize(5);
    }
  }

  @Test
  public void testSelectOnTitleAndAuthorWithMatchOnAuthor() {
    try (ResultSet docs =
        db.query("select * from Song where search_class('author:fabbio')=true")) {
      assertThat(docs).hasSize(87);
    }
    try (ResultSet docs = db.query("select * from Song where search_class('fabbio')=true")) {
      assertThat(docs).hasSize(87);
    }
  }

  @Test
  public void testSelectOnIndexWithIgnoreNullValuesToFalse() {
    // #5579
    String script =
        """
            create class Item;
            create property Item.title string;
            create property Item.summary string;
            create property Item.content string;
            create index Item.fulltext on Item(title, summary, content) FULLTEXT ENGINE LUCENE\
             METADATA {'ignoreNullValues':false};
             begin;
            insert into Item set title = 'wrong', content = 'not me please';
            insert into Item set title = 'test', content = 'this is a test';
            commit;
            """;

    db.execute("sql", script).close();

    try (ResultSet resultSet = db.query("select * from Item where search_class('te*')=true")) {
      assertThat(resultSet).hasSize(1);
    }

    try (ResultSet docs = db.query("select * from Item where search_class('test')=true")) {
      assertThat(docs).hasSize(1);
    }

    try (ResultSet docs = db.query("select * from Item where search_class('title:test')=true")) {
      assertThat(docs).hasSize(1);
    }

    // index
    Index index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Item.fulltext");
    try (Stream<RID> stream = index.getInternal().getRids(db, "title:test")) {
      assertThat(stream.count()).isEqualTo(1);
    }
  }
}