package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary;

import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readByte;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readString;

import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTImmutableSchema;

public class ORecordSerializerBinaryDebug extends ORecordSerializerBinaryV0 {

  public ORecordSerializationDebug deserializeDebug(
      final byte[] iSource, YTDatabaseSessionInternal db) {
    ORecordSerializationDebug debugInfo = new ORecordSerializationDebug();
    YTImmutableSchema schema = db.getMetadata().getImmutableSchemaSnapshot();
    BytesContainer bytes = new BytesContainer(iSource);
    int version = readByte(bytes);

    if (ORecordSerializerBinary.INSTANCE.getSerializer(version).isSerializingClassNameByDefault()) {
      try {
        final String className = readString(bytes);
        debugInfo.className = className;
      } catch (RuntimeException ex) {
        debugInfo.readingFailure = true;
        debugInfo.readingException = ex;
        debugInfo.failPosition = bytes.offset;
        return debugInfo;
      }
    }

    ORecordSerializerBinary.INSTANCE
        .getSerializer(version)
        .deserializeDebug(db, bytes, debugInfo, schema);
    return debugInfo;
  }
}