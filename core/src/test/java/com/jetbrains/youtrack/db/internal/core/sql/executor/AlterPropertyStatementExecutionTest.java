package com.jetbrains.youtrack.db.internal.core.sql.executor;

import static com.jetbrains.youtrack.db.internal.core.sql.executor.ExecutionPlanPrintUtils.printExecutionPlan;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.Property;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClass;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class AlterPropertyStatementExecutionTest extends DbTestBase {

  @Test
  public void testSetProperty() {
    String className = "testSetProperty";
    SchemaClass clazz = db.getMetadata().getSchema().createClass(className);
    Property prop = clazz.createProperty(db, "name", PropertyType.STRING);
    prop.setMax(db, "15");

    ResultSet result = db.command("alter property " + className + ".name max 30");
    printExecutionPlan(null, result);
    Object currentValue = prop.getMax();

    Assert.assertNotNull(result);
    Assert.assertTrue(result.hasNext());
    Result next = result.next();
    Assert.assertNotNull(next);
    Assert.assertEquals("15", next.getProperty("oldValue"));
    Assert.assertEquals("30", currentValue);
    Assert.assertEquals(currentValue, next.getProperty("newValue"));
    result.close();
  }

  @Test
  public void testSetCustom() {
    String className = "testSetCustom";
    SchemaClass clazz = db.getMetadata().getSchema().createClass(className);
    Property prop = clazz.createProperty(db, "name", PropertyType.STRING);
    prop.setCustom(db, "foo", "bar");

    ResultSet result = db.command("alter property " + className + ".name custom foo='baz'");
    printExecutionPlan(null, result);
    Object currentValue = prop.getCustom("foo");

    Assert.assertNotNull(result);
    Assert.assertTrue(result.hasNext());
    Result next = result.next();
    Assert.assertNotNull(next);
    Assert.assertEquals("bar", next.getProperty("oldValue"));
    Assert.assertEquals("baz", currentValue);
    Assert.assertEquals(currentValue, next.getProperty("newValue"));
    result.close();
  }
}
