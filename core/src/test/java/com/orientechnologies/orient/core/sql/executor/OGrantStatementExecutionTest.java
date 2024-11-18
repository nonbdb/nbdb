package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.ORule;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityPolicyImpl;
import com.orientechnologies.orient.core.metadata.security.OSecurityRole;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Luigi Dell'Aquila (l.dellaquila-(at)-orientdb.com)
 */
public class OGrantStatementExecutionTest extends BaseMemoryDatabase {

  @Test
  public void testSimple() {
    db.begin();
    ORole testRole =
        db.getMetadata()
            .getSecurity()
            .createRole("testRole", OSecurityRole.ALLOW_MODES.DENY_ALL_BUT);
    Assert.assertFalse(
        testRole.allow(ORule.ResourceGeneric.SERVER, "server", ORole.PERMISSION_EXECUTE));
    db.commit();
    db.begin();
    db.command("GRANT execute on server.remove to testRole");
    db.commit();
    testRole = db.getMetadata().getSecurity().getRole("testRole");
    Assert.assertTrue(
        testRole.allow(ORule.ResourceGeneric.SERVER, "remove", ORole.PERMISSION_EXECUTE));
  }

  @Test
  public void testGrantPolicy() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "testPolicy");
    policy.setActive(true);
    policy.setReadRule("name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    db.command("GRANT POLICY testPolicy ON database.class.Person TO reader").close();
    db.commit();

    Assert.assertEquals(
        "testPolicy",
        security
            .getSecurityPolicies(db, security.getRole(db, "reader"))
            .get("database.class.Person")
            .getName());
  }
}
