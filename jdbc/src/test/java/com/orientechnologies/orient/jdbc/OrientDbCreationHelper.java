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
package com.orientechnologies.orient.jdbc;

import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.YTVertex;
import com.orientechnologies.orient.core.record.impl.YTBlob;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.record.impl.YTRecordBytes;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

public class OrientDbCreationHelper {

  public static void loadDB(YTDatabaseSession db, int documents) throws IOException {

    db.begin();
    for (int i = 1; i <= documents; i++) {
      YTDocument doc = new YTDocument();
      doc.setClassName("Item");
      doc = createItem(i, doc);
      ((YTDatabaseSessionInternal) db).save(doc, "Item");
    }

    createAuthorAndArticles(db, 50, 50);
    createArticleWithAttachmentSplitted(db);

    createWriterAndPosts(db, 10, 10);
    db.commit();
  }

  public static YTDocument createItem(int id, YTDocument doc) {
    String itemKey = Integer.valueOf(id).toString();

    doc.setClassName("Item");
    doc.field("stringKey", itemKey);
    doc.field("intKey", id);
    String contents =
        "YouTrackDB is a deeply scalable Document-Graph DBMS with the flexibility of the Document"
            + " databases and the power to manage links of the Graph databases. It can work in"
            + " schema-less mode, schema-full or a mix of both. Supports advanced features such as"
            + " ACID Transactions, Fast Indexes, Native and SQL queries. It imports and exports"
            + " documents in JSON. Graphs of hundreads of linked documents can be retrieved all in"
            + " memory in few milliseconds without executing costly JOIN such as the Relational"
            + " DBMSs do. YouTrackDB uses a new indexing algorithm called MVRB-Tree, derived from the"
            + " Red-Black Tree and from the B+Tree with benefits of both: fast insertion and ultra"
            + " fast lookup. The transactional engine can run in distributed systems supporting up"
            + " to 9.223.372.036 Billions of records for the maximum capacity of"
            + " 19.807.040.628.566.084 Terabytes of data distributed on multiple disks in multiple"
            + " nodes. YouTrackDB is FREE for any use. Open Source License Apache 2.0. ";
    doc.field("text", contents);
    doc.field("title", "youTrackDB");
    doc.field("score", BigDecimal.valueOf(contents.length() / id));
    doc.field("length", contents.length(), YTType.LONG);
    doc.field("published", (id % 2 > 0));
    doc.field("author", "anAuthor" + id);
    // doc.field("tags", asList("java", "orient", "nosql"),
    // YTType.EMBEDDEDLIST);
    Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    instance.add(Calendar.HOUR_OF_DAY, -id);
    Date time = instance.getTime();
    doc.field("date", time, YTType.DATE);
    doc.field("time", time, YTType.DATETIME);

    return doc;
  }

  public static void createAuthorAndArticles(YTDatabaseSession db, int totAuthors, int totArticles)
      throws IOException {
    int articleSerial = 0;
    for (int a = 1; a <= totAuthors; ++a) {
      YTDocument author = new YTDocument("Author");
      List<YTDocument> articles = new ArrayList<>(totArticles);
      author.field("articles", articles);

      author.field("uuid", a, YTType.DOUBLE);
      author.field("name", "Jay");
      author.field("rating", new Random().nextDouble());

      for (int i = 1; i <= totArticles; ++i) {
        YTDocument article = new YTDocument("Article");

        Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date time = instance.getTime();
        article.field("date", time, YTType.DATE);

        article.field("uuid", articleSerial++);
        article.field("title", "the title for article " + articleSerial);
        article.field("content", "the content for article " + articleSerial);
        article.field("attachment", loadFile(db, "./src/test/resources/file.pdf"));

        articles.add(article);
      }

      author.save();
    }
  }

  public static YTDocument createArticleWithAttachmentSplitted(YTDatabaseSession db)
      throws IOException {

    YTDocument article = new YTDocument("Article");
    Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    Date time = instance.getTime();
    article.field("date", time, YTType.DATE);

    article.field("uuid", 1000000);
    article.field("title", "the title 2");
    article.field("content", "the content 2");
    if (new File("./src/test/resources/file.pdf").exists()) {
      article.field("attachment", loadFile(db, "./src/test/resources/file.pdf", 256));
    }
    db.begin();
    db.save(article);
    db.commit();
    return article;
  }

  public static void createWriterAndPosts(YTDatabaseSession db, int totAuthors, int totArticles)
      throws IOException {
    int articleSerial = 0;
    for (int a = 1; a <= totAuthors; ++a) {
      YTVertex writer = db.newVertex("Writer");
      writer.setProperty("uuid", a);
      writer.setProperty("name", "happy writer");
      writer.setProperty("is_active", Boolean.TRUE);
      writer.setProperty("isActive", Boolean.TRUE);

      for (int i = 1; i <= totArticles; ++i) {

        YTVertex post = db.newVertex("Post");

        Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date time = instance.getTime();
        post.setProperty("date", time, YTType.DATE);
        post.setProperty("uuid", articleSerial++);
        post.setProperty("title", "the title");
        post.setProperty("content", "the content");

        db.newEdge(writer, post, "Writes");
      }
    }

    // additional wrong data
    YTVertex writer = db.newVertex("Writer");
    writer.setProperty("uuid", totAuthors * 2);
    writer.setProperty("name", "happy writer");
    writer.setProperty("is_active", Boolean.TRUE);
    writer.setProperty("isActive", Boolean.TRUE);

    YTVertex post = db.newVertex("Post");

    // no date!!

    post.setProperty("uuid", articleSerial * 2);
    post.setProperty("title", "the title");
    post.setProperty("content", "the content");

    db.newEdge(writer, post, "Writes");
  }

  private static YTBlob loadFile(YTDatabaseSession database, String filePath) throws IOException {
    final File f = new File(filePath);
    if (f.exists()) {
      BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(f));
      YTBlob record = new YTRecordBytes();
      record.fromInputStream(inputStream);
      return record;
    }

    return null;
  }

  private static List<YTRID> loadFile(YTDatabaseSession database, String filePath, int bufferSize)
      throws IOException {
    File binaryFile = new File(filePath);
    long binaryFileLength = binaryFile.length();
    int numberOfRecords = (int) (binaryFileLength / bufferSize);
    int remainder = (int) (binaryFileLength % bufferSize);
    if (remainder > 0) {
      numberOfRecords++;
    }
    List<YTRID> binaryChuncks = new ArrayList<>(numberOfRecords);
    BufferedInputStream binaryStream = new BufferedInputStream(new FileInputStream(binaryFile));

    for (int i = 0; i < numberOfRecords; i++) {
      var index = i;
      var recnum = numberOfRecords;

      database.executeInTx(
          () -> {
            byte[] chunk;
            if (index == recnum - 1) {
              chunk = new byte[remainder];
            } else {
              chunk = new byte[bufferSize];
            }
            try {
              binaryStream.read(chunk);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }

            YTBlob recordChunk = new YTRecordBytes(chunk);

            database.save(recordChunk);
            binaryChuncks.add(recordChunk.getIdentity());
          });
    }

    return binaryChuncks;
  }

  public static void createSchemaDB(YTDatabaseSessionInternal db) {

    YTSchema schema = db.getMetadata().getSchema();

    // item
    YTClass item = schema.createClass("Item");

    item.createProperty(db, "stringKey", YTType.STRING).createIndex(db, INDEX_TYPE.UNIQUE);
    item.createProperty(db, "intKey", YTType.INTEGER).createIndex(db, INDEX_TYPE.UNIQUE);
    item.createProperty(db, "date", YTType.DATE).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    item.createProperty(db, "time", YTType.DATETIME).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    item.createProperty(db, "text", YTType.STRING);
    item.createProperty(db, "score", YTType.DECIMAL);
    item.createProperty(db, "length", YTType.INTEGER).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    item.createProperty(db, "published", YTType.BOOLEAN).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    item.createProperty(db, "title", YTType.STRING).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    item.createProperty(db, "author", YTType.STRING).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    item.createProperty(db, "tags", YTType.EMBEDDEDLIST);

    // class Article
    YTClass article = schema.createClass("Article");

    article.createProperty(db, "uuid", YTType.LONG).createIndex(db, INDEX_TYPE.UNIQUE);
    article.createProperty(db, "date", YTType.DATE).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    article.createProperty(db, "title", YTType.STRING);
    article.createProperty(db, "content", YTType.STRING);
    // article.createProperty("attachment", YTType.LINK);

    // author
    YTClass author = schema.createClass("Author");

    author.createProperty(db, "uuid", YTType.LONG).createIndex(db, INDEX_TYPE.UNIQUE);
    author.createProperty(db, "name", YTType.STRING).setMin(db, "3");
    author.createProperty(db, "rating", YTType.DOUBLE);
    author.createProperty(db, "articles", YTType.LINKLIST, article);

    // link article-->author
    article.createProperty(db, "author", YTType.LINK, author);

    // Graph

    YTClass v = schema.getClass("V");
    if (v == null) {
      schema.createClass("V");
    }

    YTClass post = schema.createClass("Post", v);
    post.createProperty(db, "uuid", YTType.LONG);
    post.createProperty(db, "title", YTType.STRING);
    post.createProperty(db, "date", YTType.DATE).createIndex(db, INDEX_TYPE.NOTUNIQUE);
    post.createProperty(db, "content", YTType.STRING);

    YTClass writer = schema.createClass("Writer", v);
    writer.createProperty(db, "uuid", YTType.LONG).createIndex(db, INDEX_TYPE.UNIQUE);
    writer.createProperty(db, "name", YTType.STRING);
    writer.createProperty(db, "is_active", YTType.BOOLEAN);
    writer.createProperty(db, "isActive", YTType.BOOLEAN);

    YTClass e = schema.getClass("E");
    if (e == null) {
      schema.createClass("E");
    }

    schema.createClass("Writes", e);
  }
}
