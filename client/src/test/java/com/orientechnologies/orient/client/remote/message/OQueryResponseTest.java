package com.orientechnologies.orient.client.remote.message;

import com.jetbrains.youtrack.db.internal.DBTestBase;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.ORecordSerializerNetworkFactory;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultInternal;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.OChannelBinaryProtocol;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OQueryResponseTest extends DBTestBase {

  @Test
  public void test() throws IOException {

    List<YTResult> resuls = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      YTResultInternal item = new YTResultInternal(db);
      item.setProperty("name", "foo");
      item.setProperty("counter", i);
      resuls.add(item);
    }
    OQueryResponse response =
        new OQueryResponse("query", true, resuls, Optional.empty(), false, new HashMap<>(), true);

    MockChannel channel = new MockChannel();
    response.write(null,
        channel,
        OChannelBinaryProtocol.CURRENT_PROTOCOL_VERSION,
        ORecordSerializerNetworkFactory.INSTANCE.current());

    channel.close();

    OQueryResponse newResponse = new OQueryResponse();

    newResponse.read(db, channel, null);
    Iterator<YTResult> responseRs = newResponse.getResult().iterator();

    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(responseRs.hasNext());
      YTResult item = responseRs.next();
      Assert.assertEquals("foo", item.getProperty("name"));
      Assert.assertEquals((Integer) i, item.getProperty("counter"));
    }
    Assert.assertFalse(responseRs.hasNext());
    Assert.assertTrue(newResponse.isReloadMetadata());
    Assert.assertTrue(newResponse.isTxChanges());
  }
}
