package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.record.OVertex;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ODeleteEdgeStatementExecutionTest extends DBTestBase {

  @Test
  public void testDeleteSingleEdge() {
    String vertexClassName = "testDeleteSingleEdgeV";
    db.createVertexClass(vertexClassName);

    String edgeClassName = "testDeleteSingleEdgeE";
    db.createEdgeClass(edgeClassName);

    OVertex prev = null;
    for (int i = 0; i < 10; i++) {
      db.begin();
      OVertex v1 = db.newVertex(vertexClassName);
      v1.setProperty("name", "a" + i);
      v1.save();
      if (prev != null) {
        prev = db.bindToSession(prev);
        prev.addEdge(v1, edgeClassName).save();
      }
      prev = v1;
      db.commit();
    }

    OResultSet rs = db.query("SELECT expand(out()) FROM " + vertexClassName);
    Assert.assertEquals(9, rs.stream().count());
    rs.close();

    rs = db.query("SELECT expand(in()) FROM " + vertexClassName);
    Assert.assertEquals(9, rs.stream().count());
    rs.close();

    db.begin();
    db.command(
            "DELETE EDGE "
                + edgeClassName
                + " from (SELECT FROM "
                + vertexClassName
                + " where name = 'a1') to (SELECT FROM "
                + vertexClassName
                + " where name = 'a2')")
        .close();
    db.commit();

    rs = db.query("SELECT FROM " + edgeClassName);
    Assert.assertEquals(8, rs.stream().count());
    rs.close();

    rs = db.query("SELECT expand(out()) FROM " + vertexClassName + " where name = 'a1'");
    Assert.assertEquals(0, rs.stream().count());
    rs.close();

    rs = db.query("SELECT expand(in()) FROM " + vertexClassName + " where name = 'a2'");
    Assert.assertEquals(0, rs.stream().count());
    rs.close();
  }

  @Test
  public void testDeleteAll() {
    String vertexClassName = "testDeleteAllV";
    db.createVertexClass(vertexClassName);

    String edgeClassName = "testDeleteAllE";
    db.createEdgeClass(edgeClassName);

    OVertex prev = null;
    for (int i = 0; i < 10; i++) {
      db.begin();
      OVertex v1 = db.newVertex(vertexClassName);
      v1.setProperty("name", "a" + i);
      v1.save();
      if (prev != null) {
        prev = db.bindToSession(prev);
        prev.addEdge(v1, edgeClassName).save();
      }
      prev = v1;
      db.commit();
    }

    OResultSet rs = db.query("SELECT expand(out()) FROM " + vertexClassName);
    Assert.assertEquals(9, rs.stream().count());
    rs.close();

    rs = db.query("SELECT expand(in()) FROM " + vertexClassName);
    Assert.assertEquals(9, rs.stream().count());
    rs.close();

    db.begin();
    db.command("DELETE EDGE " + edgeClassName).close();
    db.commit();

    rs = db.query("SELECT FROM " + edgeClassName);
    Assert.assertEquals(0, rs.stream().count());
    rs.close();

    rs = db.query("SELECT expand(out()) FROM " + vertexClassName);
    Assert.assertEquals(0, rs.stream().count());
    rs.close();

    rs = db.query("SELECT expand(in()) FROM " + vertexClassName);
    Assert.assertEquals(0, rs.stream().count());
    rs.close();
  }
}
