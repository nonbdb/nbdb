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

package com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OBinarySerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OBinaryTypeSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OBooleanSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OByteSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OCharSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.ODateSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.ODateTimeSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.ODecimalSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.ODoubleSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OFloatSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OIntegerSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OLongSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.ONullSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OShortSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OStringSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OUTF8Serializer;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.OCompactedLinkSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.OLinkSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.index.OCompositeKeySerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.index.OSimpleKeySerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.stream.OMixedIndexRIDContainerSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.stream.OStreamSerializerRID;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.stream.OStreamSerializerSBTreeIndexRIDContainer;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.multivalue.v2.MultiValueEntrySerializer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is responsible for obtaining OBinarySerializer realization, by it's id of type of
 * object that should be serialized.
 */
public class OBinarySerializerFactory {

  /**
   * Size of the type identifier block size
   */
  public static final int TYPE_IDENTIFIER_SIZE = 1;

  private final ConcurrentMap<Byte, OBinarySerializer<?>> serializerIdMap =
      new ConcurrentHashMap<Byte, OBinarySerializer<?>>();
  private final ConcurrentMap<Byte, Class<? extends OBinarySerializer>> serializerClassesIdMap =
      new ConcurrentHashMap<Byte, Class<? extends OBinarySerializer>>();
  private final ConcurrentMap<YTType, OBinarySerializer<?>> serializerTypeMap =
      new ConcurrentHashMap<YTType, OBinarySerializer<?>>();

  private OBinarySerializerFactory() {
  }

  public static OBinarySerializerFactory create(int binaryFormatVersion) {
    final OBinarySerializerFactory factory = new OBinarySerializerFactory();

    // STATELESS SERIALIER
    factory.registerSerializer(new ONullSerializer(), null);

    factory.registerSerializer(OBooleanSerializer.INSTANCE, YTType.BOOLEAN);
    factory.registerSerializer(OIntegerSerializer.INSTANCE, YTType.INTEGER);
    factory.registerSerializer(OShortSerializer.INSTANCE, YTType.SHORT);
    factory.registerSerializer(OLongSerializer.INSTANCE, YTType.LONG);
    factory.registerSerializer(OFloatSerializer.INSTANCE, YTType.FLOAT);
    factory.registerSerializer(ODoubleSerializer.INSTANCE, YTType.DOUBLE);
    factory.registerSerializer(ODateTimeSerializer.INSTANCE, YTType.DATETIME);
    factory.registerSerializer(OCharSerializer.INSTANCE, null);
    factory.registerSerializer(OStringSerializer.INSTANCE, YTType.STRING);

    factory.registerSerializer(OByteSerializer.INSTANCE, YTType.BYTE);
    factory.registerSerializer(ODateSerializer.INSTANCE, YTType.DATE);
    factory.registerSerializer(OLinkSerializer.INSTANCE, YTType.LINK);
    factory.registerSerializer(OCompositeKeySerializer.INSTANCE, null);
    factory.registerSerializer(OStreamSerializerRID.INSTANCE, null);
    factory.registerSerializer(OBinaryTypeSerializer.INSTANCE, YTType.BINARY);
    factory.registerSerializer(ODecimalSerializer.INSTANCE, YTType.DECIMAL);

    factory.registerSerializer(OStreamSerializerSBTreeIndexRIDContainer.INSTANCE, null);

    // STATEFUL SERIALIER
    factory.registerSerializer(OSimpleKeySerializer.ID, OSimpleKeySerializer.class);

    factory.registerSerializer(OCompactedLinkSerializer.INSTANCE, null);
    factory.registerSerializer(OMixedIndexRIDContainerSerializer.INSTANCE, null);

    factory.registerSerializer(OUTF8Serializer.INSTANCE, null);
    factory.registerSerializer(MultiValueEntrySerializer.INSTANCE, null);

    return factory;
  }

  public static OBinarySerializerFactory getInstance() {
    final YTDatabaseSessionInternal database = ODatabaseRecordThreadLocal.instance().getIfDefined();
    if (database != null) {
      return database.getSerializerFactory();
    } else {
      return OBinarySerializerFactory.create(Integer.MAX_VALUE);
    }
  }

  public void registerSerializer(final OBinarySerializer<?> iInstance, final YTType iType) {
    if (serializerIdMap.containsKey(iInstance.getId())) {
      throw new IllegalArgumentException(
          "Binary serializer with id " + iInstance.getId() + " has been already registered.");
    }

    serializerIdMap.put(iInstance.getId(), iInstance);
    if (iType != null) {
      serializerTypeMap.put(iType, iInstance);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void registerSerializer(final byte iId, final Class<? extends OBinarySerializer> iClass) {
    if (serializerClassesIdMap.containsKey(iId)) {
      throw new IllegalStateException(
          "Serializer with id " + iId + " has been already registered.");
    }

    serializerClassesIdMap.put(iId, iClass);
  }

  /**
   * Obtain OBinarySerializer instance by it's id.
   *
   * @param identifier is serializes identifier.
   * @return OBinarySerializer instance.
   */
  public OBinarySerializer<?> getObjectSerializer(final byte identifier) {
    OBinarySerializer<?> impl = serializerIdMap.get(identifier);
    if (impl == null) {
      final Class<? extends OBinarySerializer> cls = serializerClassesIdMap.get(identifier);
      if (cls != null) {
        try {
          impl = cls.newInstance();
        } catch (Exception e) {
          LogManager.instance()
              .error(
                  this,
                  "Cannot create an instance of class %s invoking the empty constructor",
                  e,
                  cls);
        }
      }
    }
    return impl;
  }

  /**
   * Obtain OBinarySerializer realization for the YTType
   *
   * @param type is the YTType to obtain serializer algorithm for
   * @return OBinarySerializer instance
   */
  @SuppressWarnings("unchecked")
  public <T> OBinarySerializer<T> getObjectSerializer(final YTType type) {
    return (OBinarySerializer<T>) serializerTypeMap.get(type);
  }
}
