/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */

package com.orientechnologies.orient.core.serialization.serializer.result.binary;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.common.serialization.types.ODecimalSerializer;
import com.orientechnologies.common.serialization.types.OIntegerSerializer;
import com.orientechnologies.common.serialization.types.OLongSerializer;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.exception.OValidationException;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.YTEntity;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.serialization.OSerializableStream;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.BytesContainer;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.OSerializableWrapper;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.OVarIntSerializer;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.util.ODateHelper;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataInput;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

public class OResultSerializerNetwork {

  private static final String CHARSET_UTF_8 = "UTF-8";
  private static final YTRecordId NULL_RECORD_ID = new YTRecordId(-2, YTRID.CLUSTER_POS_INVALID);
  private static final long MILLISEC_PER_DAY = 86400000;

  public OResultSerializerNetwork() {
  }

  public OResultInternal deserialize(YTDatabaseSessionInternal db, final BytesContainer bytes) {
    final OResultInternal document = new OResultInternal(db);
    String fieldName;
    YTType type;
    int size = OVarIntSerializer.readAsInteger(bytes);
    // fields
    while (size-- > 0) {
      final int len = OVarIntSerializer.readAsInteger(bytes);
      // PARSE FIELD NAME
      fieldName = stringFromBytes(bytes.bytes, bytes.offset, len).intern();
      bytes.skip(len);
      type = readOType(bytes);

      if (type == null) {
        document.setProperty(fieldName, null);
      } else {
        final Object value = deserializeValue(db, bytes, type);
        document.setProperty(fieldName, value);
      }
    }

    int metadataSize = OVarIntSerializer.readAsInteger(bytes);
    // metadata
    while (metadataSize-- > 0) {
      final int len = OVarIntSerializer.readAsInteger(bytes);
      // PARSE FIELD NAME
      fieldName = stringFromBytes(bytes.bytes, bytes.offset, len).intern();
      bytes.skip(len);
      type = readOType(bytes);

      if (type == null) {
        document.setMetadata(fieldName, null);
      } else {
        final Object value = deserializeValue(db, bytes, type);
        document.setMetadata(fieldName, value);
      }
    }

    return document;
  }

  @SuppressWarnings("unchecked")
  public void serialize(final OResult result, final BytesContainer bytes) {
    var propertyNames = result.getPropertyNames();

    OVarIntSerializer.write(bytes, propertyNames.size());
    for (String property : propertyNames) {
      writeString(bytes, property);
      Object propertyValue = result.getProperty(property);
      if (propertyValue != null) {
        if (propertyValue instanceof OResult) {
          if (((OResult) propertyValue).isElement()) {
            YTEntity elem = ((OResult) propertyValue).getElement().get();
            writeOType(bytes, bytes.alloc(1), YTType.LINK);
            serializeValue(bytes, elem.getIdentity(), YTType.LINK, null);
          } else {
            writeOType(bytes, bytes.alloc(1), YTType.EMBEDDED);
            serializeValue(bytes, propertyValue, YTType.EMBEDDED, null);
          }
        } else {
          final YTType type = YTType.getTypeByValue(propertyValue);
          if (type == null) {
            throw new OSerializationException(
                "Impossible serialize value of type "
                    + propertyValue.getClass()
                    + " with the Result binary serializer");
          }
          writeOType(bytes, bytes.alloc(1), type);
          serializeValue(bytes, propertyValue, type, null);
        }
      } else {
        writeOType(bytes, bytes.alloc(1), null);
      }
    }

    Set<String> metadataKeys = result.getMetadataKeys();
    OVarIntSerializer.write(bytes, metadataKeys.size());

    for (String field : metadataKeys) {
      writeString(bytes, field);
      final Object value = result.getMetadata(field);
      if (value != null) {
        if (value instanceof OResult) {
          writeOType(bytes, bytes.alloc(1), YTType.EMBEDDED);
          serializeValue(bytes, value, YTType.EMBEDDED, null);
        } else {
          final YTType type = YTType.getTypeByValue(value);
          if (type == null) {
            throw new OSerializationException(
                "Impossible serialize value of type "
                    + value.getClass()
                    + " with the Result binary serializer");
          }
          writeOType(bytes, bytes.alloc(1), type);
          serializeValue(bytes, value, type, null);
        }
      } else {
        writeOType(bytes, bytes.alloc(1), null);
      }
    }
  }

  protected YTType readOType(final BytesContainer bytes) {
    byte val = readByte(bytes);
    if (val == -1) {
      return null;
    }
    return YTType.getById(val);
  }

  private void writeOType(BytesContainer bytes, int pos, YTType type) {
    if (type == null) {
      bytes.bytes[pos] = (byte) -1;
    } else {
      bytes.bytes[pos] = (byte) type.getId();
    }
  }

  public Object deserializeValue(YTDatabaseSessionInternal db, BytesContainer bytes, YTType type) {
    Object value = null;
    switch (type) {
      case INTEGER:
        value = OVarIntSerializer.readAsInteger(bytes);
        break;
      case LONG:
        value = OVarIntSerializer.readAsLong(bytes);
        break;
      case SHORT:
        value = OVarIntSerializer.readAsShort(bytes);
        break;
      case STRING:
        value = readString(bytes);
        break;
      case DOUBLE:
        value = Double.longBitsToDouble(readLong(bytes));
        break;
      case FLOAT:
        value = Float.intBitsToFloat(readInteger(bytes));
        break;
      case BYTE:
        value = readByte(bytes);
        break;
      case BOOLEAN:
        value = readByte(bytes) == 1;
        break;
      case DATETIME:
        value = new Date(OVarIntSerializer.readAsLong(bytes));
        break;
      case DATE:
        long savedTime = OVarIntSerializer.readAsLong(bytes) * MILLISEC_PER_DAY;
        savedTime =
            convertDayToTimezone(
                TimeZone.getTimeZone("GMT"), ODateHelper.getDatabaseTimeZone(), savedTime);
        value = new Date(savedTime);
        break;
      case EMBEDDED:
        value = deserialize(db, bytes);
        break;
      case EMBEDDEDSET:
        value = readEmbeddedCollection(db, bytes, new LinkedHashSet<>());
        break;
      case EMBEDDEDLIST:
        value = readEmbeddedCollection(db, bytes, new ArrayList<>());
        break;
      case LINKSET:
        value = readLinkCollection(bytes, new LinkedHashSet<>());
        break;
      case LINKLIST:
        value = readLinkCollection(bytes, new ArrayList<>());
        break;
      case BINARY:
        value = readBinary(bytes);
        break;
      case LINK:
        value = readOptimizedLink(bytes);
        break;
      case LINKMAP:
        value = readLinkMap(db, bytes);
        break;
      case EMBEDDEDMAP:
        value = readEmbeddedMap(db, bytes);
        break;
      case DECIMAL:
        value = ODecimalSerializer.INSTANCE.deserialize(bytes.bytes, bytes.offset);
        bytes.skip(ODecimalSerializer.INSTANCE.getObjectSize(bytes.bytes, bytes.offset));
        break;
      case LINKBAG:
        throw new UnsupportedOperationException("LINKBAG should never appear in a projection");
      case TRANSIENT:
        break;
      case CUSTOM:
        try {
          String className = readString(bytes);
          Class<?> clazz = Class.forName(className);
          OSerializableStream stream = (OSerializableStream) clazz.newInstance();
          stream.fromStream(readBinary(bytes));
          if (stream instanceof OSerializableWrapper) {
            value = ((OSerializableWrapper) stream).getSerializable();
          } else {
            value = stream;
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        break;
      case ANY:
        break;
    }
    return value;
  }

  private byte[] readBinary(BytesContainer bytes) {
    int n = OVarIntSerializer.readAsInteger(bytes);
    byte[] newValue = new byte[n];
    System.arraycopy(bytes.bytes, bytes.offset, newValue, 0, newValue.length);
    bytes.skip(n);
    return newValue;
  }

  private Map<Object, YTIdentifiable> readLinkMap(YTDatabaseSessionInternal db,
      final BytesContainer bytes) {
    int size = OVarIntSerializer.readAsInteger(bytes);
    Map<Object, YTIdentifiable> result = new HashMap<>();
    while ((size--) > 0) {
      YTType keyType = readOType(bytes);
      Object key = deserializeValue(db, bytes, keyType);
      YTRecordId value = readOptimizedLink(bytes);
      if (value.equals(NULL_RECORD_ID)) {
        result.put(key, null);
      } else {
        result.put(key, value);
      }
    }
    return result;
  }

  private Map readEmbeddedMap(YTDatabaseSessionInternal db, final BytesContainer bytes) {
    int size = OVarIntSerializer.readAsInteger(bytes);
    final Map document = new LinkedHashMap();
    String fieldName;
    YTType type;
    while ((size--) > 0) {
      final int len = OVarIntSerializer.readAsInteger(bytes);
      // PARSE FIELD NAME
      fieldName = stringFromBytes(bytes.bytes, bytes.offset, len).intern();
      bytes.skip(len);
      type = readOType(bytes);

      if (type == null) {
        document.put(fieldName, null);
      } else {
        final Object value = deserializeValue(db, bytes, type);
        document.put(fieldName, value);
      }
    }
    return document;
  }

  private Collection<YTIdentifiable> readLinkCollection(
      BytesContainer bytes, Collection<YTIdentifiable> found) {
    final int items = OVarIntSerializer.readAsInteger(bytes);
    for (int i = 0; i < items; i++) {
      YTRecordId id = readOptimizedLink(bytes);
      if (id.equals(NULL_RECORD_ID)) {
        found.add(null);
      } else {
        found.add(id);
      }
    }
    return found;
  }

  private YTRecordId readOptimizedLink(final BytesContainer bytes) {
    return new YTRecordId(
        OVarIntSerializer.readAsInteger(bytes), OVarIntSerializer.readAsLong(bytes));
  }

  private Collection<?> readEmbeddedCollection(
      YTDatabaseSessionInternal db, final BytesContainer bytes, final Collection<Object> found) {
    final int items = OVarIntSerializer.readAsInteger(bytes);
    for (int i = 0; i < items; i++) {
      YTType itemType = readOType(bytes);
      if (itemType == null) {
        found.add(null);
      } else {
        found.add(deserializeValue(db, bytes, itemType));
      }
    }
    return found;
  }

  @SuppressWarnings("unchecked")
  public void serializeValue(
      final BytesContainer bytes, Object value, final YTType type, final YTType linkedType) {

    final int pointer;
    switch (type) {
      case INTEGER:
      case LONG:
      case SHORT:
        OVarIntSerializer.write(bytes, ((Number) value).longValue());
        break;
      case STRING:
        writeString(bytes, value.toString());
        break;
      case DOUBLE:
        long dg = Double.doubleToLongBits((Double) value);
        pointer = bytes.alloc(OLongSerializer.LONG_SIZE);
        OLongSerializer.INSTANCE.serializeLiteral(dg, bytes.bytes, pointer);
        break;
      case FLOAT:
        int fg = Float.floatToIntBits((Float) value);
        pointer = bytes.alloc(OIntegerSerializer.INT_SIZE);
        OIntegerSerializer.INSTANCE.serializeLiteral(fg, bytes.bytes, pointer);
        break;
      case BYTE:
        pointer = bytes.alloc(1);
        bytes.bytes[pointer] = (Byte) value;
        break;
      case BOOLEAN:
        pointer = bytes.alloc(1);
        bytes.bytes[pointer] = ((Boolean) value) ? (byte) 1 : (byte) 0;
        break;
      case DATETIME:
        if (value instanceof Long) {
          OVarIntSerializer.write(bytes, (Long) value);
        } else {
          OVarIntSerializer.write(bytes, ((Date) value).getTime());
        }
        break;
      case DATE:
        long dateValue;
        if (value instanceof Long) {
          dateValue = (Long) value;
        } else {
          dateValue = ((Date) value).getTime();
        }
        dateValue =
            convertDayToTimezone(
                ODateHelper.getDatabaseTimeZone(), TimeZone.getTimeZone("GMT"), dateValue);
        OVarIntSerializer.write(bytes, dateValue / MILLISEC_PER_DAY);
        break;
      case EMBEDDED:
        if (!(value instanceof OResult)) {
          throw new UnsupportedOperationException();
        }
        serialize((OResult) value, bytes);
        break;
      case EMBEDDEDSET:
      case EMBEDDEDLIST:
        if (value.getClass().isArray()) {
          writeEmbeddedCollection(bytes, Arrays.asList(OMultiValue.array(value)));
        } else {
          writeEmbeddedCollection(bytes, (Collection<?>) value);
        }
        break;
      case DECIMAL:
        BigDecimal decimalValue = (BigDecimal) value;
        pointer = bytes.alloc(ODecimalSerializer.INSTANCE.getObjectSize(decimalValue));
        ODecimalSerializer.INSTANCE.serialize(decimalValue, bytes.bytes, pointer);
        break;
      case BINARY:
        writeBinary(bytes, (byte[]) (value));
        break;
      case LINKSET:
      case LINKLIST:
        Collection<YTIdentifiable> ridCollection = (Collection<YTIdentifiable>) value;
        writeLinkCollection(bytes, ridCollection);
        break;
      case LINK:
        if (value instanceof OResult && ((OResult) value).isElement()) {
          value = ((OResult) value).getElement().get();
        }
        if (!(value instanceof YTIdentifiable)) {
          throw new OValidationException("Value '" + value + "' is not a YTIdentifiable");
        }
        writeOptimizedLink(bytes, (YTIdentifiable) value);
        break;
      case LINKMAP:
        writeLinkMap(bytes, (Map<Object, YTIdentifiable>) value);
        break;
      case EMBEDDEDMAP:
        writeEmbeddedMap(bytes, (Map<Object, Object>) value);
        break;
      case LINKBAG:
        throw new UnsupportedOperationException("LINKBAG should never appear in a projection");
      case CUSTOM:
        if (!(value instanceof OSerializableStream)) {
          value = new OSerializableWrapper((Serializable) value);
        }
        writeString(bytes, value.getClass().getName());
        writeBinary(bytes, ((OSerializableStream) value).toStream());
        break;
      case TRANSIENT:
        break;
      case ANY:
        break;
    }
  }

  private int writeBinary(final BytesContainer bytes, final byte[] valueBytes) {
    final int pointer = OVarIntSerializer.write(bytes, valueBytes.length);
    final int start = bytes.alloc(valueBytes.length);
    System.arraycopy(valueBytes, 0, bytes.bytes, start, valueBytes.length);
    return pointer;
  }

  private int writeLinkMap(final BytesContainer bytes, final Map<Object, YTIdentifiable> map) {
    final int fullPos = OVarIntSerializer.write(bytes, map.size());
    for (Entry<Object, YTIdentifiable> entry : map.entrySet()) {
      // TODO:check skip of complex types
      // FIXME: changed to support only string key on map
      final YTType type = YTType.STRING;
      writeOType(bytes, bytes.alloc(1), type);
      writeString(bytes, entry.getKey().toString());
      if (entry.getValue() == null) {
        writeNullLink(bytes);
      } else {
        writeOptimizedLink(bytes, entry.getValue());
      }
    }
    return fullPos;
  }

  @SuppressWarnings("unchecked")
  private void writeEmbeddedMap(BytesContainer bytes, Map<Object, Object> map) {
    Set fieldNames = map.keySet();

    OVarIntSerializer.write(bytes, map.size());
    for (Object f : fieldNames) {
      if (!(f instanceof String field)) {
        throw new OSerializationException(
            "Invalid key type for map: " + f + " (only Strings supported)");
      }
      writeString(bytes, field);
      final Object value = map.get(field);
      if (value != null) {
        if (value instanceof OResult) {
          writeOType(bytes, bytes.alloc(1), YTType.EMBEDDED);
          serializeValue(bytes, value, YTType.EMBEDDED, null);
        } else {
          final YTType type = YTType.getTypeByValue(value);
          if (type == null) {
            throw new OSerializationException(
                "Impossible serialize value of type "
                    + value.getClass()
                    + " with the Result binary serializer");
          }
          writeOType(bytes, bytes.alloc(1), type);
          serializeValue(bytes, value, type, null);
        }
      } else {
        writeOType(bytes, bytes.alloc(1), null);
      }
    }
  }

  private int writeNullLink(final BytesContainer bytes) {
    final int pos = OVarIntSerializer.write(bytes, NULL_RECORD_ID.getIdentity().getClusterId());
    OVarIntSerializer.write(bytes, NULL_RECORD_ID.getIdentity().getClusterPosition());
    return pos;
  }

  private int writeOptimizedLink(final BytesContainer bytes, YTIdentifiable link) {
    if (!link.getIdentity().isPersistent()) {
      try {
        link = link.getRecord();
      } catch (ORecordNotFoundException rnf) {
        // IGNORE THIS
      }
    }
    final int pos = OVarIntSerializer.write(bytes, link.getIdentity().getClusterId());
    OVarIntSerializer.write(bytes, link.getIdentity().getClusterPosition());
    return pos;
  }

  private void writeLinkCollection(
      final BytesContainer bytes, final Collection<YTIdentifiable> value) {
    final int pos = OVarIntSerializer.write(bytes, value.size());
    for (YTIdentifiable itemValue : value) {
      if (itemValue == null) {
        writeNullLink(bytes);
      } else {
        writeOptimizedLink(bytes, itemValue);
      }
    }
  }

  private void writeEmbeddedCollection(final BytesContainer bytes, final Collection<?> value) {
    OVarIntSerializer.write(bytes, value.size());

    for (Object itemValue : value) {
      // TODO:manage in a better way null entry
      if (itemValue == null) {
        writeOType(bytes, bytes.alloc(1), null);
        continue;
      }
      YTType type = getTypeFromValueEmbedded(itemValue);
      if (type != null) {
        writeOType(bytes, bytes.alloc(1), type);
        serializeValue(bytes, itemValue, type, null);
      } else {
        throw new OSerializationException(
            "Impossible serialize value of type "
                + value.getClass()
                + " with the YTDocument binary serializer");
      }
    }
  }

  private YTType getTypeFromValueEmbedded(final Object fieldValue) {
    if (fieldValue instanceof OResult && ((OResult) fieldValue).isElement()) {
      return YTType.LINK;
    }
    YTType type =
        fieldValue instanceof OResult ? YTType.EMBEDDED : YTType.getTypeByValue(fieldValue);
    if (type == YTType.LINK
        && fieldValue instanceof YTDocument
        && !((YTDocument) fieldValue).getIdentity().isValid()) {
      type = YTType.EMBEDDED;
    }
    return type;
  }

  protected String readString(final BytesContainer bytes) {
    final int len = OVarIntSerializer.readAsInteger(bytes);
    final String res = stringFromBytes(bytes.bytes, bytes.offset, len);
    bytes.skip(len);
    return res;
  }

  protected int readInteger(final BytesContainer container) {
    final int value =
        OIntegerSerializer.INSTANCE.deserializeLiteral(container.bytes, container.offset);
    container.offset += OIntegerSerializer.INT_SIZE;
    return value;
  }

  private byte readByte(final BytesContainer container) {
    return container.bytes[container.offset++];
  }

  private long readLong(final BytesContainer container) {
    final long value =
        OLongSerializer.INSTANCE.deserializeLiteral(container.bytes, container.offset);
    container.offset += OLongSerializer.LONG_SIZE;
    return value;
  }

  private int writeEmptyString(final BytesContainer bytes) {
    return OVarIntSerializer.write(bytes, 0);
  }

  private int writeString(final BytesContainer bytes, final String toWrite) {
    final byte[] nameBytes = bytesFromString(toWrite);
    final int pointer = OVarIntSerializer.write(bytes, nameBytes.length);
    final int start = bytes.alloc(nameBytes.length);
    System.arraycopy(nameBytes, 0, bytes.bytes, start, nameBytes.length);
    return pointer;
  }

  private byte[] bytesFromString(final String toWrite) {
    return toWrite.getBytes(StandardCharsets.UTF_8);
  }

  protected String stringFromBytes(final byte[] bytes, final int offset, final int len) {
    return new String(bytes, offset, len, StandardCharsets.UTF_8);
  }

  private long convertDayToTimezone(TimeZone from, TimeZone to, long time) {
    Calendar fromCalendar = Calendar.getInstance(from);
    fromCalendar.setTimeInMillis(time);
    Calendar toCalendar = Calendar.getInstance(to);
    toCalendar.setTimeInMillis(0);
    toCalendar.set(Calendar.ERA, fromCalendar.get(Calendar.ERA));
    toCalendar.set(Calendar.YEAR, fromCalendar.get(Calendar.YEAR));
    toCalendar.set(Calendar.MONTH, fromCalendar.get(Calendar.MONTH));
    toCalendar.set(Calendar.DAY_OF_MONTH, fromCalendar.get(Calendar.DAY_OF_MONTH));
    toCalendar.set(Calendar.HOUR_OF_DAY, 0);
    toCalendar.set(Calendar.MINUTE, 0);
    toCalendar.set(Calendar.SECOND, 0);
    toCalendar.set(Calendar.MILLISECOND, 0);
    return toCalendar.getTimeInMillis();
  }

  public void toStream(OResult item, OChannelDataOutput channel) throws IOException {
    final BytesContainer bytes = new BytesContainer();
    this.serialize(item, bytes);
    channel.writeBytes(bytes.fitBytes());
  }

  public OResultInternal fromStream(YTDatabaseSessionInternal db, OChannelDataInput channel)
      throws IOException {
    BytesContainer bytes = new BytesContainer();
    bytes.bytes = channel.readBytes();
    return this.deserialize(db, bytes);
  }
}
