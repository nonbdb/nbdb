package com.orientechnologies.orient.core.metadata.security;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class OSecurityPolicyTest extends BaseMemoryDatabase {

  @Test
  public void testSecurityPolicyCreate() {
    OResultSet rs =
        db.query(
            "select from " + OSecurityPolicy.class.getSimpleName() + " WHERE name = ?", "test");
    Assert.assertFalse(rs.hasNext());
    rs.close();
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.begin();
    security.createSecurityPolicy(db, "test");
    db.commit();

    rs =
        db.query(
            "select from " + OSecurityPolicy.class.getSimpleName() + " WHERE name = ?", "test");
    Assert.assertTrue(rs.hasNext());
    OResult item = rs.next();
    Assert.assertEquals("test", item.getProperty("name"));
    Assert.assertFalse(rs.hasNext());
    rs.close();
  }

  @Test
  public void testSecurityPolicyGet() {
    OSecurityInternal security = db.getSharedContext().getSecurity();
    Assert.assertNull(security.getSecurityPolicy(db, "test"));

    db.begin();
    security.createSecurityPolicy(db, "test");
    db.commit();

    Assert.assertNotNull(security.getSecurityPolicy(db, "test"));
  }

  @Test
  public void testValidPredicates() {
    OSecurityInternal security = db.getSharedContext().getSecurity();
    Assert.assertNull(security.getSecurityPolicy(db, "test"));

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "test");
    policy.setCreateRule("name = 'create'");
    policy.setReadRule("name = 'read'");
    policy.setBeforeUpdateRule("name = 'beforeUpdate'");
    policy.setAfterUpdateRule("name = 'afterUpdate'");
    policy.setDeleteRule("name = 'delete'");
    policy.setExecuteRule("name = 'execute'");

    security.saveSecurityPolicy(db, policy);
    db.commit();

    OSecurityPolicy readPolicy = security.getSecurityPolicy(db, "test");
    Assert.assertNotNull(policy);
    Assert.assertEquals("name = 'create'", readPolicy.getCreateRule());
    Assert.assertEquals("name = 'read'", readPolicy.getReadRule());
    Assert.assertEquals("name = 'beforeUpdate'", readPolicy.getBeforeUpdateRule());
    Assert.assertEquals("name = 'afterUpdate'", readPolicy.getAfterUpdateRule());
    Assert.assertEquals("name = 'delete'", readPolicy.getDeleteRule());
    Assert.assertEquals("name = 'execute'", readPolicy.getExecuteRule());
  }

  @Test
  public void testInvalidPredicates() {
    OSecurityInternal security = db.getSharedContext().getSecurity();
    Assert.assertNull(security.getSecurityPolicy(db, "test"));

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "test");
    db.commit();
    try {
      db.begin();
      policy.setCreateRule("foo bar");
      db.commit();
      Assert.fail();
    } catch (IllegalArgumentException ex) {
    }
    try {
      db.begin();
      policy.setReadRule("foo bar");
      db.commit();
      Assert.fail();
    } catch (IllegalArgumentException ex) {
    }
    try {
      db.begin();
      policy.setBeforeUpdateRule("foo bar");
      db.commit();
      Assert.fail();
    } catch (IllegalArgumentException ex) {
    }
    try {
      db.begin();
      policy.setAfterUpdateRule("foo bar");
      db.commit();
      Assert.fail();
    } catch (IllegalArgumentException ex) {
    }
    try {
      db.begin();
      policy.setDeleteRule("foo bar");
      db.commit();
      Assert.fail();
    } catch (IllegalArgumentException ex) {
    }
    try {
      db.begin();
      policy.setExecuteRule("foo bar");
      db.commit();
      Assert.fail();
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testAddPolicyToRole() {
    OSecurityInternal security = db.getSharedContext().getSecurity();
    Assert.assertNull(security.getSecurityPolicy(db, "test"));

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "test");
    policy.setCreateRule("1 = 1");
    policy.setBeforeUpdateRule("1 = 2");
    policy.setActive(true);
    security.saveSecurityPolicy(db, policy);
    db.commit();

    db.begin();
    ORole reader = security.getRole(db, "reader");
    String resource = "database.class.Person";
    security.setSecurityPolicy(db, reader, resource, policy);
    db.commit();

    ORID policyRid = policy.getElement().getIdentity();
    try (OResultSet rs = db.query("select from ORole where name = 'reader'")) {
      Map<String, OIdentifiable> rolePolicies = rs.next().getProperty("policies");
      OIdentifiable id = rolePolicies.get(resource);
      Assert.assertEquals(id.getIdentity(), policyRid);
    }

    OSecurityPolicy policy2 = security.getSecurityPolicy(db, reader, resource);
    Assert.assertNotNull(policy2);
    Assert.assertEquals(policy2.getIdentity(), policyRid);
  }

  @Test
  public void testRemovePolicyToRole() {
    OSecurityInternal security = db.getSharedContext().getSecurity();
    Assert.assertNull(security.getSecurityPolicy(db, "test"));

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "test");
    policy.setCreateRule("1 = 1");
    policy.setBeforeUpdateRule("1 = 2");
    policy.setActive(true);
    security.saveSecurityPolicy(db, policy);
    db.commit();

    db.begin();
    ORole reader = security.getRole(db, "reader");
    String resource = "database.class.Person";
    security.setSecurityPolicy(db, reader, resource, policy);

    security.removeSecurityPolicy(db, reader, resource);
    db.commit();

    Assert.assertNull(security.getSecurityPolicy(db, reader, resource));
  }
}
