package com.jetbrains.youtrack.db.internal.core.db.tool;

import com.jetbrains.youtrack.db.internal.BaseMemoryInternalDatabase;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.index.OIndex;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultSet;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OCheckIndexToolTest extends BaseMemoryInternalDatabase {

  @Test
  public void test() {
    db.command("create class Foo").close();
    db.command("create property Foo.name STRING").close();
    db.command("create index Foo.name on Foo (name) NOTUNIQUE").close();

    db.begin();
    EntityImpl doc = db.newInstance("Foo");
    doc.field("name", "a");
    doc.save();
    db.commit();

    YTRID rid = doc.getIdentity();

    int N_RECORDS = 100000;
    for (int i = 0; i < N_RECORDS; i++) {
      db.begin();
      doc = db.newInstance("Foo");
      doc.field("name", "x" + i);
      doc.save();
      db.commit();
    }

    db.begin();
    OIndex idx = db.getMetadata().getIndexManagerInternal().getIndex(db, "Foo.name");
    Object key = idx.getDefinition().createValue(db, "a");
    idx.remove(db, key, rid);
    db.commit();

    db.begin();
    YTResultSet result = db.query("SELECT FROM Foo");
    Assert.assertEquals(N_RECORDS + 1, result.stream().count());

    OCheckIndexTool tool = new OCheckIndexTool();
    tool.setDatabase(db);
    tool.setVerbose(true);
    tool.setOutputListener(System.out::println);

    tool.run();
    db.commit();

    Assert.assertEquals(1, tool.getTotalErrors());
  }

  @Test
  public void testBugOnCollectionIndex() {
    db.command("create class testclass");
    db.command("create property testclass.name string");
    db.command("create property testclass.tags linklist");
    db.command("alter property testclass.tags default '[]'");
    db.command("create index testclass_tags_idx on testclass (tags) NOTUNIQUE_HASH_INDEX");

    db.begin();
    db.command("insert into testclass set name = 'a',tags = [#5:0] ");
    db.command("insert into testclass set name = 'b'");
    db.command("insert into testclass set name = 'c' ");
    db.commit();

    final OCheckIndexTool tool = new OCheckIndexTool();

    tool.setDatabase(db);
    tool.setVerbose(true);
    tool.setOutputListener(System.out::println);
    tool.run();
    Assert.assertEquals(0, tool.getTotalErrors());
  }
}