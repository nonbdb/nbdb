package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @since 10/21/13
 */
@Test
public class DateIndexTest extends BaseDBTest {

  @Parameters(value = "remote")
  public DateIndexTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final Schema schema = db.getMetadata().getSchema();

    SchemaClass dateIndexTest = schema.createClass("DateIndexTest");

    dateIndexTest.createProperty(db, "dateField", PropertyType.DATE);
    dateIndexTest.createProperty(db, "dateTimeField", PropertyType.DATETIME);

    dateIndexTest.createProperty(db, "dateList", PropertyType.EMBEDDEDLIST,
        PropertyType.DATE);
    dateIndexTest.createProperty(db, "dateTimeList", PropertyType.EMBEDDEDLIST,
        PropertyType.DATETIME);

    dateIndexTest.createProperty(db, "value", PropertyType.STRING);

    dateIndexTest.createIndex(db, "DateIndexTestDateIndex", SchemaClass.INDEX_TYPE.UNIQUE,
        "dateField");
    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateIndex", SchemaClass.INDEX_TYPE.UNIQUE, "value", "dateField");

    dateIndexTest.createIndex(db,
        "DateIndexTestDateTimeIndex", SchemaClass.INDEX_TYPE.UNIQUE, "dateTimeField");
    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateTimeIndex", SchemaClass.INDEX_TYPE.UNIQUE, "value", "dateTimeField");

    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateListIndex", SchemaClass.INDEX_TYPE.UNIQUE, "value", "dateList");
    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateTimeListIndex", SchemaClass.INDEX_TYPE.UNIQUE, "value",
        "dateTimeList");

    dateIndexTest.createIndex(db,
        "DateIndexTestDateHashIndex", SchemaClass.INDEX_TYPE.UNIQUE, "dateField");
    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateHashIndex",
        SchemaClass.INDEX_TYPE.UNIQUE,
        "value", "dateField");

    dateIndexTest.createIndex(db,
        "DateIndexTestDateTimeHashIndex", SchemaClass.INDEX_TYPE.UNIQUE,
        "dateTimeField");
    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateTimeHashIndex",
        SchemaClass.INDEX_TYPE.UNIQUE,
        "value", "dateTimeField");

    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateListHashIndex",
        SchemaClass.INDEX_TYPE.UNIQUE,
        "value", "dateList");
    dateIndexTest.createIndex(db,
        "DateIndexTestValueDateTimeListHashIndex",
        SchemaClass.INDEX_TYPE.UNIQUE,
        "value", "dateTimeList");
  }

  public void testDateIndexes() {
    checkEmbeddedDB();

    final Date dateOne = new Date();

    final Date dateTwo = new Date(dateOne.getTime() + 24 * 60 * 60 * 1000 + 100);

    final EntityImpl dateDoc = ((EntityImpl) db.newEntity("DateIndexTest"));

    dateDoc.field("dateField", dateOne);
    dateDoc.field("dateTimeField", dateTwo);

    final List<Date> dateList = new ArrayList<>();

    final Date dateThree = new Date(dateOne.getTime() + 100);
    final Date dateFour = new Date(dateThree.getTime() + 24 * 60 * 60 * 1000 + 100);

    dateList.add(new Date(dateThree.getTime()));
    dateList.add(new Date(dateFour.getTime()));

    final List<Date> dateTimeList = new ArrayList<>();

    dateTimeList.add(new Date(dateThree.getTime()));
    dateTimeList.add(new Date(dateFour.getTime()));

    dateDoc.field("dateList", dateList);
    dateDoc.field("dateTimeList", dateTimeList);

    dateDoc.field("value", "v1");

    db.begin();
    dateDoc.save();
    db.commit();

    final Index dateIndexTestDateIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestDateIndex");
    try (Stream<RID> stream = dateIndexTestDateIndex.getInternal().getRids(db, dateOne)) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream = dateIndexTestDateIndex.getInternal().getRids(db, dateTwo)) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestDateTimeIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestDateTimeIndex");
    try (Stream<RID> stream = dateIndexTestDateTimeIndex.getInternal()
        .getRids(db, dateTwo)) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream = dateIndexTestDateTimeIndex.getInternal()
        .getRids(db, dateOne)) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestValueDateIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateIndex");
    try (Stream<RID> stream =
        dateIndexTestValueDateIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateOne))) {
      Assert.assertEquals((stream.findAny().orElse(null)), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateTwo))) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestValueDateTimeIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateTimeIndex");
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateTwo))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateOne))) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestValueDateListIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateListIndex");

    try (Stream<RID> stream =
        dateIndexTestValueDateListIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateThree))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateListIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateFour))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }

    final Index dateIndexTestValueDateTimeListIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateListIndex");
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeListIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateThree))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeListIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateFour))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }

    final Index dateIndexTestDateHashIndexIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestDateHashIndex");
    try (Stream<RID> stream = dateIndexTestDateHashIndexIndex.getInternal()
        .getRids(db, dateOne)) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream = dateIndexTestDateHashIndexIndex.getInternal()
        .getRids(db, dateTwo)) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestDateTimeHashIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestDateTimeHashIndex");
    try (Stream<RID> stream = dateIndexTestDateTimeHashIndex.getInternal()
        .getRids(db, dateTwo)) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream = dateIndexTestDateTimeHashIndex.getInternal()
        .getRids(db, dateOne)) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestValueDateHashIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateHashIndex");
    try (Stream<RID> stream =
        dateIndexTestValueDateHashIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateOne))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateHashIndex.getInternal()
            .getRids(db, new CompositeKey("v1", dateTwo))) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestValueDateTimeHashIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateTimeHashIndex");
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeHashIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateTwo))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeHashIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateOne))) {
      Assert.assertFalse(stream.findAny().isPresent());
    }

    final Index dateIndexTestValueDateListHashIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateListHashIndex");

    try (Stream<RID> stream =
        dateIndexTestValueDateListHashIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateThree))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateListHashIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateFour))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }

    final Index dateIndexTestValueDateTimeListHashIndex =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "DateIndexTestValueDateListHashIndex");
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeListHashIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateThree))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
    try (Stream<RID> stream =
        dateIndexTestValueDateTimeListHashIndex
            .getInternal()
            .getRids(db, new CompositeKey("v1", dateFour))) {
      Assert.assertEquals(stream.findAny().orElse(null), dateDoc.getIdentity());
    }
  }
}