package com.orientechnologies.orient.test.server.network.http;

import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Before;

/**
 * Test HTTP "batch" command.
 */
public class HttpBatchTest extends BaseHttpDatabaseTest {

  @Before
  public void beforeMethod() {
    getServer().getPlugin("script-interpreter").startup();
  }

  public void batchUpdate() throws Exception {
    Assert.assertEquals(
        post("command/" + getDatabaseName() + "/sql/")
            .payload("create class User", CONTENT.TEXT)
            .getResponse()
            .getCode(),
        200);

    Assert.assertEquals(
        post("command/" + getDatabaseName() + "/sql/")
            .payload("insert into User content {\"userID\": \"35862601\"}", CONTENT.TEXT)
            .setUserName("admin")
            .setUserPassword("admin")
            .getResponse()
            .getCode(),
        200);

    String response = EntityUtils.toString(getResponse().getEntity());

    Assert.assertNotNull(response);

    var responseDoc = new ODocument();
    responseDoc.fromJSON(response);
    ODocument insertedDocument =
        ((List<ODocument>) responseDoc.field("result")).get(0);

    // TEST UPDATE
    Assert.assertEquals(
        post("batch/" + getDatabaseName() + "/sql/")
            .payload(
                "{\n"
                    + "    \"transaction\": true,\n"
                    + "    \"operations\": [{\n"
                    + "        \"record\": {\n"
                    + "            \"userID\": \"35862601\",\n"
                    + "            \"externalID\": \"35862601\",\n"
                    + "            \"@rid\": \""
                    + insertedDocument.getIdentity()
                    + "\", \"@class\": \"User\", \"@version\": "
                    + insertedDocument.getVersion()
                    + "\n"
                    + "        },\n"
                    + "        \"type\": \"u\"\n"
                    + "    }]\n"
                    + "}",
                CONTENT.JSON)
            .getResponse()
            .getCode(),
        200);

    // TEST DOUBLE UPDATE
    Assert.assertEquals(
        post("batch/" + getDatabaseName() + "/sql/")
            .payload(
                "{\n"
                    + "    \"transaction\": true,\n"
                    + "    \"operations\": [{\n"
                    + "        \"record\": {\n"
                    + "            \"userID\": \"35862601\",\n"
                    + "            \"externalID\": \"35862601\",\n"
                    + "            \"@rid\": \""
                    + insertedDocument.getIdentity()
                    + "\", \"@class\": \"User\", \"@version\": "
                    + (insertedDocument.getVersion() + 1)
                    + "\n"
                    + "        },\n"
                    + "        \"type\": \"u\"\n"
                    + "    }]\n"
                    + "}",
                CONTENT.JSON)
            .getResponse()
            .getCode(),
        200);

    // TEST WRONG VERSION ON UPDATE
    Assert.assertEquals(
        post("batch/" + getDatabaseName() + "/sql/")
            .payload(
                "{\n"
                    + "    \"transaction\": true,\n"
                    + "    \"operations\": [{\n"
                    + "        \"record\": {\n"
                    + "            \"userID\": \"35862601\",\n"
                    + "            \"externalID\": \"35862601\",\n"
                    + "            \"@rid\": \""
                    + insertedDocument.getIdentity()
                    + "\", \"@class\": \"User\", \"@version\": "
                    + (insertedDocument.getVersion() + 1)
                    + "\n"
                    + "        },\n"
                    + "        \"type\": \"u\"\n"
                    + "    }]\n"
                    + "}",
                CONTENT.JSON)
            .getResponse()
            .getCode(),
        409);

    batchWithEmpty();
  }

  private void batchWithEmpty() throws IOException {
    String json =
        "{\n"
            + "\"operations\": [{\n"
            + "\"type\": \"script\",\n"
            + "\"language\": \"SQL\","
            + "\"script\": \"let $a = select from User limit 2 \\n"
            + "let $b = select sum(foo) from (select from User where name = 'adsfafoo') \\n"
            + "return [$a, $b]\""
            + "}]\n"
            + "}";
    var response = post("batch/" + getDatabaseName()).payload(json, CONTENT.TEXT).getResponse();

    Assert.assertEquals(response.getCode(), 200);
    InputStream stream = response.getEntity().getContent();
    String string = "";
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    String line = reader.readLine();
    while (line != null) {
      string += line;
      line = reader.readLine();
    }
    System.out.println(string);
    ODocument doc = new ODocument();
    doc.fromJSON(string);

    stream.close();
    Iterable iterable = (Iterable) doc.eval("result.value");

    System.out.println(iterable);
    Iterator iterator = iterable.iterator();
    Assert.assertTrue(iterator.hasNext());
    iterator.next();
    Assert.assertTrue(iterator.hasNext());
    Object emptyList = iterator.next();
    Assert.assertNotNull(emptyList);
    Assert.assertTrue(emptyList instanceof Iterable);
    Iterator emptyListIterator = ((Iterable) emptyList).iterator();
    Assert.assertFalse(emptyListIterator.hasNext());
  }

  @Override
  public String getDatabaseName() {
    return "httpscript";
  }
}
