/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfigBuilderImpl;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.exception.SecurityException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.metadata.security.RestrictedOperation;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityShared;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class RestrictedTest extends BaseDBTest {

  private RID adminRecordId;
  private RID writerRecordId;

  private Role readerRole = null;

  @Parameters(value = "remote")
  public RestrictedTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @Override
  protected YouTrackDBConfig createConfig(YouTrackDBConfigBuilderImpl builder) {
    builder.addGlobalConfigurationParameter(GlobalConfiguration.NON_TX_READS_WARNING_MODE,
        "EXCEPTION");
    return builder.build();
  }

  @Test
  public void testCreateRestrictedClass() {
    database = createSessionInstance();
    database
        .getMetadata()
        .getSchema()
        .createClass("CMSDocument", database.getMetadata().getSchema().getClass("ORestricted"));

    database.begin();
    var adminRecord = new EntityImpl("CMSDocument").field("user", "admin");
    adminRecord.save();
    this.adminRecordId = adminRecord.getIdentity();
    database.commit();

    database.begin();
    readerRole = database.getMetadata().getSecurity().getRole("reader");
    database.commit();

    Assert.assertTrue(adminRecord.isUnloaded());
  }

  @Test(dependsOnMethods = "testCreateRestrictedClass")
  public void testFilteredQuery() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    ResultSet result = database.query("select from CMSDocument");
    Assert.assertEquals(result.stream().count(), 0);
    database.commit();
  }

  @Test(dependsOnMethods = "testFilteredQuery")
  public void testCreateAsWriter() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    var writerRecord = new EntityImpl("CMSDocument").field("user", "writer");
    writerRecord.save();
    this.writerRecordId = writerRecord.getIdentity();
    database.commit();
  }

  @Test(dependsOnMethods = "testCreateAsWriter")
  public void testFilteredQueryAsReader() throws IOException {
    database = createSessionInstance("reader", "reader");

    database.begin();
    ResultSet result = database.query("select from CMSDocument");
    Assert.assertEquals(result.stream().count(), 0);
    database.commit();
  }

  @Test(dependsOnMethods = "testFilteredQueryAsReader")
  public void testFilteredQueryAsAdmin() throws IOException {
    database = createSessionInstance();

    database.begin();
    ResultSet result = database.query("select from CMSDocument where user = 'writer'");
    Assert.assertEquals(result.stream().count(), 1);
    database.commit();
  }

  @Test(dependsOnMethods = "testFilteredQueryAsAdmin")
  public void testFilteredQueryAsWriter() throws IOException {
    database = createSessionInstance("writer", "writer");

    database.begin();
    ResultSet result = database.query("select from CMSDocument");
    Assert.assertEquals(result.stream().count(), 1);
    database.commit();
  }

  @Test(dependsOnMethods = "testFilteredQueryAsWriter")
  public void testFilteredDirectReadAsWriter() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    try {
      database.load(adminRecordId.getIdentity());
      Assert.fail();
    } catch (RecordNotFoundException e) {
      // ignore
    }

    database.commit();
  }

  @Test(dependsOnMethods = "testFilteredDirectReadAsWriter")
  public void testFilteredDirectUpdateAsWriter() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    try {
      var adminRecord = database.loadEntity(this.adminRecordId);
      adminRecord.setProperty("user", "writer-hacker");
      adminRecord.save();
      database.commit();
    } catch (SecurityException | RecordNotFoundException e) {
      // OK AS EXCEPTION
    }
    database.close();

    database = createSessionInstance();
    database.begin();
    var adminRecord = database.<EntityImpl>load(this.adminRecordId);
    Assert.assertEquals(adminRecord.field("user"), "admin");
    database.commit();
  }

  @Test(dependsOnMethods = "testFilteredDirectUpdateAsWriter")
  public void testFilteredDirectDeleteAsWriter() throws IOException {
    database = createSessionInstance("writer", "writer");
    try {
      database.begin();
      database.delete(adminRecordId);
      database.commit();
    } catch (SecurityException | RecordNotFoundException e) {
      // OK AS EXCEPTION
    }
    database.close();

    database = createSessionInstance();
    database.begin();
    var adminRecord = database.<EntityImpl>load(this.adminRecordId);
    Assert.assertEquals(adminRecord.field("user"), "admin");
    database.commit();
  }

  @Test(dependsOnMethods = "testFilteredDirectDeleteAsWriter")
  public void testFilteredHackingAllowFieldAsWriter() throws IOException {
    database = createSessionInstance("writer", "writer");
    try {
      database.begin();
      // FORCE LOADING
      EntityImpl adminRecord = database.load(this.adminRecordId);
      Set<Identifiable> allows = adminRecord.field(SecurityShared.ALLOW_ALL_FIELD);
      allows.add(
          database.getMetadata().getSecurity().getUser(database.geCurrentUser().getName(database))
              .getIdentity(database));
      adminRecord.save();
      database.commit();
    } catch (SecurityException | RecordNotFoundException e) {
      // OK AS EXCEPTION
    }
    database.close();

    database = createSessionInstance();
  }

  @Test(dependsOnMethods = "testFilteredHackingAllowFieldAsWriter")
  public void testAddReaderAsRole() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    var writerRecord = database.<EntityImpl>load(this.writerRecordId);
    Set<Identifiable> allows = writerRecord.field(SecurityShared.ALLOW_ALL_FIELD);
    allows.add(readerRole.getIdentity(database));

    writerRecord.save();
    database.commit();
  }

  @Test(dependsOnMethods = "testAddReaderAsRole")
  public void testReaderCanSeeWriterDocumentAfterPermission() throws IOException {
    database = createSessionInstance("reader", "reader");
    database.begin();
    Assert.assertNotNull(database.load(writerRecordId.getIdentity()));
    database.commit();
  }

  @Test(dependsOnMethods = "testReaderCanSeeWriterDocumentAfterPermission")
  public void testWriterRoleCanRemoveReader() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    EntityImpl writerRecord = database.load(this.writerRecordId);
    Assert.assertEquals(
        ((Collection<?>) writerRecord.field(RestrictedOperation.ALLOW_ALL.getFieldName())).size(),
        2);
    database
        .getMetadata()
        .getSecurity()
        .denyRole(writerRecord, RestrictedOperation.ALLOW_ALL, "reader");
    Assert.assertEquals(
        ((Collection<?>) writerRecord.field(RestrictedOperation.ALLOW_ALL.getFieldName())).size(),
        1);
    writerRecord.save();
    database.commit();
  }

  @Test(dependsOnMethods = "testWriterRoleCanRemoveReader")
  public void testReaderCannotSeeWriterDocument() throws IOException {
    database = createSessionInstance("reader", "reader");
    database.begin();
    try {
      database.load(writerRecordId.getIdentity());
      Assert.fail();
    } catch (RecordNotFoundException e) {
      // ignore
    }
    database.commit();
  }

  @Test(dependsOnMethods = "testReaderCannotSeeWriterDocument")
  public void testWriterAddReaderUserOnlyForRead() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    EntityImpl writerRecord = database.load(this.writerRecordId);
    database
        .getMetadata()
        .getSecurity()
        .allowUser(writerRecord, RestrictedOperation.ALLOW_READ, "reader");
    writerRecord.save();
    database.commit();
  }

  @Test(dependsOnMethods = "testWriterAddReaderUserOnlyForRead")
  public void testReaderCanSeeWriterDocument() throws IOException {
    database = createSessionInstance("reader", "reader");
    database.begin();
    Assert.assertNotNull(database.load(writerRecordId.getIdentity()));
    database.commit();
  }

  /**
   * *** TESTS FOR #1980: Record Level Security: permissions don't follow role's inheritance ****
   */
  @Test(dependsOnMethods = "testReaderCanSeeWriterDocument")
  public void testWriterRemoveReaderUserOnlyForRead() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    EntityImpl writerRecord = database.load(this.writerRecordId);
    database
        .getMetadata()
        .getSecurity()
        .denyUser(writerRecord, RestrictedOperation.ALLOW_READ, "reader");
    writerRecord.save();
    database.commit();
  }

  @Test(dependsOnMethods = "testWriterRemoveReaderUserOnlyForRead")
  public void testReaderCannotSeeWriterDocumentAgain() throws IOException {
    database = createSessionInstance("reader", "reader");
    database.begin();
    try {
      database.load(writerRecordId.getIdentity());
      Assert.fail();
    } catch (RecordNotFoundException e) {
      // ignore
    }
    database.commit();
  }

  @Test(dependsOnMethods = "testReaderCannotSeeWriterDocumentAgain")
  public void testReaderRoleInheritsFromWriterRole() throws IOException {
    database = createSessionInstance();
    database.begin();
    Role reader = database.getMetadata().getSecurity().getRole("reader");
    reader.setParentRole(database, database.getMetadata().getSecurity().getRole("writer"));

    reader.save(database);
    database.commit();
  }

  @Test(dependsOnMethods = "testReaderRoleInheritsFromWriterRole")
  public void testWriterRoleCanSeeWriterDocument() throws IOException {
    database = createSessionInstance("writer", "writer");
    database.begin();
    EntityImpl writerRecord = database.load(this.writerRecordId);
    database
        .getMetadata()
        .getSecurity()
        .allowRole(writerRecord, RestrictedOperation.ALLOW_READ, "writer");
    writerRecord.save();
    database.commit();
  }

  @Test(dependsOnMethods = "testWriterRoleCanSeeWriterDocument")
  public void testReaderRoleCanSeeInheritedDocument() {
    database = createSessionInstance("reader", "reader");

    database.begin();
    Assert.assertNotNull(database.load(writerRecordId.getIdentity()));
    database.commit();
  }

  @Test(dependsOnMethods = "testReaderRoleCanSeeInheritedDocument")
  public void testReaderRoleDesntInheritsFromWriterRole() throws IOException {
    database = createSessionInstance();
    database.begin();
    Role reader = database.getMetadata().getSecurity().getRole("reader");
    reader.setParentRole(database, null);
    reader.save(database);
    database.commit();
  }

  /**
   * ** END TEST FOR #1980: Record Level Security: permissions don't follow role's inheritance ***
   */
  @Test(dependsOnMethods = "testReaderRoleDesntInheritsFromWriterRole")
  public void testTruncateClass() {
    database = createSessionInstance();
    try {
      database.command("truncate class CMSDocument").close();
      Assert.fail();
    } catch (SecurityException e) {
      Assert.assertTrue(true);
    }
  }

  @Test(dependsOnMethods = "testTruncateClass")
  public void testTruncateUnderlyingCluster() {
    database = createSessionInstance();
    try {
      database.command("truncate cluster CMSDocument").close();
    } catch (SecurityException e) {

    }
  }

  @Test(dependsOnMethods = "testTruncateUnderlyingCluster")
  public void testUpdateRestricted() {
    database = createSessionInstance();
    database
        .getMetadata()
        .getSchema()
        .createClass(
            "TestUpdateRestricted", database.getMetadata().getSchema().getClass("ORestricted"));

    database.begin();
    var adminRecord = new EntityImpl("TestUpdateRestricted").field("user", "admin");
    adminRecord.save();
    this.adminRecordId = adminRecord.getIdentity();
    database.commit();

    database.close();

    database = createSessionInstance("writer", "writer");
    database.begin();
    ResultSet result = database.query("select from TestUpdateRestricted");
    Assert.assertEquals(result.stream().count(), 0);
    database.commit();

    database.close();

    database = createSessionInstance();
    database.begin();
    database
        .command("update TestUpdateRestricted content {\"data\":\"My Test\"}").close();
    database.commit();

    database.begin();
    result = database.query("select from TestUpdateRestricted");
    Result res = result.next();
    Assert.assertFalse(result.hasNext());

    final Entity doc = res.getEntity().get();
    Assert.assertEquals(doc.getProperty("data"), "My Test");
    doc.setProperty("user", "admin");

    database.save(doc);
    database.commit();

    database.close();

    database = createSessionInstance("writer", "writer");
    database.begin();
    result = database.query("select from TestUpdateRestricted");
    Assert.assertEquals(result.stream().count(), 0);
    database.commit();
  }

  @BeforeMethod
  protected void closeDb() {
    database.close();
  }
}
