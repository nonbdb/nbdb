package com.jetbrains.youtrack.db.internal.core.metadata.security.jwt;

import java.io.DataInputStream;
import java.io.IOException;

public interface OTokenPayloadDeserializer {

  OBinaryTokenPayload deserialize(DataInputStream input, OTokenMetaInfo base) throws IOException;
}