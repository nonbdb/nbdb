package com.orientechnologies.orient.core.sql.functions.sql;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.metadata.function.OFunction;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OLegacyResultSet;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OFunctionSqlTest extends BaseMemoryDatabase {

  @Test
  public void functionSqlWithParameters() {
    db.getMetadata().getSchema().createClass("Test");

    db.begin();
    ODocument doc1 = new ODocument("Test");
    doc1.field("name", "Enrico");
    db.save(doc1);
    doc1 = new ODocument("Test");
    doc1.setClassName("Test");
    doc1.field("name", "Luca");
    db.save(doc1);
    db.commit();

    db.begin();
    OFunction function = new OFunction(db);
    function.setName(db, "test");
    function.setCode(db, "select from Test where name = :name");
    function.setParameters(db,
        new ArrayList<>() {
          {
            add("name");
          }
        });
    function.save(db);
    db.commit();

    Object result = function.executeInContext(new OBasicCommandContext(), "Enrico");

    Assert.assertEquals(((OLegacyResultSet) result).size(), 1);
  }

  @Test
  public void functionSqlWithInnerFunctionJs() {

    db.getMetadata().getSchema().createClass("Test");
    db.begin();
    ODocument doc1 = new ODocument("Test");
    doc1.field("name", "Enrico");
    db.save(doc1);
    doc1 = new ODocument("Test");
    doc1.setClassName("Test");
    doc1.field("name", "Luca");
    db.save(doc1);
    db.commit();

    db.begin();
    OFunction function = new OFunction(db);
    function.setName(db, "test");
    function.setCode(db,
        "select name from Test where name = :name and hello(:name) = 'Hello Enrico'");
    function.setParameters(db,
        new ArrayList<>() {
          {
            add("name");
          }
        });
    function.save(db);
    db.commit();

    db.begin();
    OFunction function1 = new OFunction(db);
    function1.setName(db, "hello");
    function1.setLanguage(db, "javascript");
    function1.setCode(db, "return 'Hello ' + name");
    function1.setParameters(db,
        new ArrayList<>() {
          {
            add("name");
          }
        });
    function1.save(db);
    db.commit();

    Object result = function.executeInContext(new OBasicCommandContext(), "Enrico");
    Assert.assertEquals(((OLegacyResultSet) result).size(), 1);
  }
}
