package com.jetbrains.youtrack.db.internal.core.sql.select;

import com.jetbrains.youtrack.db.internal.DBTestBase;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import org.junit.Assert;
import org.junit.Test;

public class TestSqlForeach extends DBTestBase {

  @Test
  public void testForeach() {
    db.getMetadata().getSchema().createClass("Test");

    db.begin();
    EntityImpl doc = new EntityImpl("Test");
    db.save(doc);
    db.commit();

    YTResultSet result =
        db.execute(
            "sql",
            "let $res = select from Test; foreach ($r in $res) { begin; update $r set timestamp ="
                + " sysdate(); commit;}; return $res; ");

    Assert.assertTrue(result.hasNext());

    while (result.hasNext()) {
      result.next();
    }
  }
}