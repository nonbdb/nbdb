package com.orientechnologies.core.storage.impl.local.paginated.wal.common;

import com.orientechnologies.core.storage.impl.local.paginated.wal.OWALRecord;
import java.nio.ByteBuffer;

public interface WriteableWALRecord extends OWALRecord {

  void setBinaryContent(ByteBuffer buffer);

  ByteBuffer getBinaryContent();

  void freeBinaryContent();

  int getBinaryContentLen();

  int toStream(byte[] content, int offset);

  void toStream(ByteBuffer buffer);

  int fromStream(byte[] content, int offset);

  int serializedSize();

  void written();

  boolean isWritten();

  int getId();
}
