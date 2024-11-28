package com.orientechnologies.orient.client.remote.message;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerNetworkFactory;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelBinaryProtocol;
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
public class OQueryResponseTest extends BaseMemoryDatabase {

  @Test
  public void test() throws IOException {

    List<OResult> resuls = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      OResultInternal item = new OResultInternal(db);
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
    Iterator<OResult> responseRs = newResponse.getResult().iterator();

    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(responseRs.hasNext());
      OResult item = responseRs.next();
      Assert.assertEquals("foo", item.getProperty("name"));
      Assert.assertEquals((Integer) i, item.getProperty("counter"));
    }
    Assert.assertFalse(responseRs.hasNext());
    Assert.assertTrue(newResponse.isReloadMetadata());
    Assert.assertTrue(newResponse.isTxChanges());
  }
}
