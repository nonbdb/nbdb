package com.orientechnologies.orient.core.metadata.security;

import com.orientechnologies.orient.core.OCreateDatabaseUtil;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.OxygenDB;
import com.orientechnologies.orient.core.db.OxygenDBConfig;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.parser.OBooleanExpression;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OSecurityEngineTest {

  static OxygenDB orient;
  private ODatabaseSessionInternal db;
  private static final String DB_NAME = "test";

  @BeforeClass
  public static void beforeClass() {
    orient =
        new OxygenDB(
            "plocal:./target/securityEngineTest",
            OxygenDBConfig.builder()
                .addConfig(OGlobalConfiguration.CREATE_DEFAULT_USERS, false)
                .build());
  }

  @AfterClass
  public static void afterClass() {
    orient.close();
  }

  @Before
  public void before() {
    orient.execute(
        "create database "
            + DB_NAME
            + " "
            + "memory"
            + " users ( admin identified by '"
            + OCreateDatabaseUtil.NEW_ADMIN_PASSWORD
            + "' role admin)");
    this.db =
        (ODatabaseSessionInternal)
            orient.open(DB_NAME, "admin", OCreateDatabaseUtil.NEW_ADMIN_PASSWORD);
  }

  @After
  public void after() {
    this.db.close();
    orient.drop(DB_NAME);
    this.db = null;
  }

  @Test
  public void testAllClasses() {
    OSecurityInternal security = db.getSharedContext().getSecurity();
    db.createClass("Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'admin'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.*", policy);
    db.commit();

    OBooleanExpression pred =
        OSecurityEngine.getPredicateForSecurityResource(
            db, (OSecurityShared) security, "database.class.Person", OSecurityPolicy.Scope.READ);

    Assert.assertEquals("name = 'admin'", pred.toString());
  }

  @Test
  public void testSingleClass() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Person", policy);
    db.commit();

    OBooleanExpression pred =
        OSecurityEngine.getPredicateForSecurityResource(
            db, (OSecurityShared) security, "database.class.Person", OSecurityPolicy.Scope.READ);

    Assert.assertEquals("name = 'foo'", pred.toString());
  }

  @Test
  public void testSuperclass() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");
    db.createClass("Employee", "Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Person", policy);
    db.commit();

    OBooleanExpression pred =
        OSecurityEngine.getPredicateForSecurityResource(
            db, (OSecurityShared) security, "database.class.Employee", OSecurityPolicy.Scope.READ);

    Assert.assertEquals("name = 'foo'", pred.toString());
  }

  @Test
  public void testSuperclass2() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");
    db.createClass("Employee", "Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Person", policy);

    policy = security.createSecurityPolicy(db, "policy2");
    policy.setActive(true);
    policy.setReadRule("name = 'bar'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(
        db, security.getRole(db, "admin"), "database.class.Employee", policy);
    db.commit();

    OBooleanExpression pred =
        OSecurityEngine.getPredicateForSecurityResource(
            db, (OSecurityShared) security, "database.class.Employee", OSecurityPolicy.Scope.READ);

    Assert.assertEquals("name = 'bar'", pred.toString());
  }

  @Test
  public void testSuperclass3() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");
    db.createClass("Employee", "Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'admin'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Person", policy);

    policy = security.createSecurityPolicy(db, "policy2");
    policy.setActive(true);
    policy.setReadRule("name = 'bar' OR name = 'admin'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.*", policy);
    db.commit();

    OBooleanExpression pred =
        OSecurityEngine.getPredicateForSecurityResource(
            db, (OSecurityShared) security, "database.class.Employee", OSecurityPolicy.Scope.READ);

    Assert.assertEquals("name = 'admin'", pred.toString());
  }

  @Test
  public void testTwoSuperclasses() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");
    db.createClass("Foo");
    db.createClass("Employee", "Person", "Foo");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Person", policy);

    policy = security.createSecurityPolicy(db, "policy2");
    policy.setActive(true);
    policy.setReadRule("surname = 'bar'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Foo", policy);
    db.commit();

    OBooleanExpression pred =
        OSecurityEngine.getPredicateForSecurityResource(
            db, (OSecurityShared) security, "database.class.Employee", OSecurityPolicy.Scope.READ);

    Assert.assertTrue(
        "name = 'foo' AND surname = 'bar'".equals(pred.toString())
            || "surname = 'bar' AND name = 'foo'".equals(pred.toString()));
  }

  @Test
  public void testTwoRoles() {

    db.begin();
    db.command(
        "Update OUser set roles = roles || (select from orole where name = 'reader') where name ="
            + " 'admin'");
    db.commit();
    db.close();
    db =
        (ODatabaseSessionInternal)
            orient.open(DB_NAME, "admin", OCreateDatabaseUtil.NEW_ADMIN_PASSWORD);

    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Person", policy);

    policy = security.createSecurityPolicy(db, "policy2");
    policy.setActive(true);
    policy.setReadRule("surname = 'bar'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "reader"), "database.class.Person", policy);
    db.commit();

    OBooleanExpression pred =
        OSecurityEngine.getPredicateForSecurityResource(
            db, (OSecurityShared) security, "database.class.Person", OSecurityPolicy.Scope.READ);

    Assert.assertTrue(
        "name = 'foo' OR surname = 'bar'".equals(pred.toString())
            || "surname = 'bar' OR name = 'foo'".equals(pred.toString()));
  }

  @Test
  public void testRecordFiltering() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");
    var rec1 =
        db.computeInTx(
            () -> {
              OElement record1 = db.newElement("Person");
              record1.setProperty("name", "foo");
              record1.save();
              return record1;
            });

    var rec2 =
        db.computeInTx(
            () -> {
              OElement record2 = db.newElement("Person");
              record2.setProperty("name", "bar");
              record2.save();
              return record2;
            });

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "policy1");
    policy.setActive(true);
    policy.setReadRule("name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "admin"), "database.class.Person", policy);
    db.commit();

    db.bindToSession(rec1);
    Assert.assertTrue(rec1.getIdentity().isPersistent());

    try {
      db.bindToSession(rec2);
      Assert.fail();
    } catch (ORecordNotFoundException e) {
      // ignore
    }
  }
}
