package com.orientechnologies.orient.core.serialization.serializer.record.binary;

import com.orientechnologies.orient.core.metadata.schema.YTType;

public class ORecordSerializationDebugProperty {

  public String name;
  public int globalId;
  public YTType type;
  public RuntimeException readingException;
  public boolean faildToRead;
  public int failPosition;
  public Object value;
  public int valuePos;
}
