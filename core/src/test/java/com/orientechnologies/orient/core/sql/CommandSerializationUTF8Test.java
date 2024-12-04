package com.orientechnologies.orient.core.sql;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializerFactory;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.junit.Assert;
import org.junit.Test;

public class CommandSerializationUTF8Test extends DBTestBase {

  @Test
  public void testRightSerializationEncoding() {

    OSQLQuery<?> query = new OSQLSynchQuery<Object>("select from Profile where name='😢😂 '");

    Assert.assertEquals(query.toStream().length, 66);

    OSQLQuery<?> query1 = new OSQLSynchQuery<Object>();
    query1.fromStream(db,
        query.toStream(), ORecordSerializerFactory.instance().getDefaultRecordSerializer());

    Assert.assertEquals(query1.getText(), "select from Profile where name='😢😂 '");
  }
}
