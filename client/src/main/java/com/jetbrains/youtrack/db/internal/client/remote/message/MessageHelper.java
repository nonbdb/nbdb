package com.jetbrains.youtrack.db.internal.client.remote.message;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Record;
import com.jetbrains.youtrack.db.internal.client.remote.CollectionNetworkSerializer;
import com.jetbrains.youtrack.db.internal.client.remote.message.tx.RecordOperationRequest;
import com.jetbrains.youtrack.db.internal.common.util.CommonConst;
import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBEnginesManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.exception.SerializationException;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkV37;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkV37Client;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.result.binary.ResultSerializerNetwork;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.storage.PhysicalPosition;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.BonsaiCollectionPointer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nullable;

public class MessageHelper {

  public static void writeIdentifiable(
      DatabaseSessionInternal session, ChannelDataOutput channel, final Identifiable o,
      RecordSerializer serializer)
      throws IOException {
    if (o == null) {
      channel.writeShort(ChannelBinaryProtocol.RECORD_NULL);
    } else if (o instanceof RecordId) {
      channel.writeShort(ChannelBinaryProtocol.RECORD_RID);
      channel.writeRID((RID) o);
    } else {
      writeRecord(session, channel, o.getRecord(session), serializer);
    }
  }

  public static void writeRecord(
      DatabaseSessionInternal db, ChannelDataOutput channel, RecordAbstract iRecord,
      RecordSerializer serializer)
      throws IOException {
    channel.writeShort((short) 0);
    channel.writeByte(RecordInternal.getRecordType(db, iRecord));
    channel.writeRID(iRecord.getIdentity());
    channel.writeVersion(iRecord.getVersion());
    try {
      final byte[] stream = getRecordBytes(db, iRecord, serializer);
      channel.writeBytes(stream);
    } catch (Exception e) {
      channel.writeBytes(null);
      final String message =
          "Error on marshalling record " + iRecord.getIdentity().toString() + " (" + e + ")";

      throw BaseException.wrapException(new SerializationException(message), e);
    }
  }

  public static byte[] getRecordBytes(@Nullable DatabaseSessionInternal db,
      final RecordAbstract iRecord, RecordSerializer serializer) {
    final byte[] stream;
    String dbSerializerName = null;
    if (db != null) {
      dbSerializerName = db.getSerializer().toString();
    }
    if (RecordInternal.getRecordType(db, iRecord) == EntityImpl.RECORD_TYPE
        && (dbSerializerName == null || !dbSerializerName.equals(serializer.toString()))) {
      ((EntityImpl) iRecord).deserializeFields();
      stream = serializer.toStream(db, iRecord);
    } else {
      stream = iRecord.toStream();
    }

    return stream;
  }

  public static Map<UUID, BonsaiCollectionPointer> readCollectionChanges(ChannelDataInput network)
      throws IOException {
    Map<UUID, BonsaiCollectionPointer> collectionsUpdates = new HashMap<>();
    int count = network.readInt();
    for (int i = 0; i < count; i++) {
      final long mBitsOfId = network.readLong();
      final long lBitsOfId = network.readLong();

      final BonsaiCollectionPointer pointer =
          CollectionNetworkSerializer.INSTANCE.readCollectionPointer(network);

      collectionsUpdates.put(new UUID(mBitsOfId, lBitsOfId), pointer);
    }
    return collectionsUpdates;
  }

  public static void writeCollectionChanges(
      ChannelDataOutput channel, Map<UUID, BonsaiCollectionPointer> changedIds)
      throws IOException {
    channel.writeInt(changedIds.size());
    for (Entry<UUID, BonsaiCollectionPointer> entry : changedIds.entrySet()) {
      channel.writeLong(entry.getKey().getMostSignificantBits());
      channel.writeLong(entry.getKey().getLeastSignificantBits());
      CollectionNetworkSerializer.INSTANCE.writeCollectionPointer(channel, entry.getValue());
    }
  }

  public static void writePhysicalPositions(
      ChannelDataOutput channel, PhysicalPosition[] previousPositions) throws IOException {
    if (previousPositions == null) {
      channel.writeInt(0); // NO ENTRIEs
    } else {
      channel.writeInt(previousPositions.length);

      for (final PhysicalPosition physicalPosition : previousPositions) {
        channel.writeLong(physicalPosition.clusterPosition);
        channel.writeInt(physicalPosition.recordSize);
        channel.writeVersion(physicalPosition.recordVersion);
      }
    }
  }

  public static PhysicalPosition[] readPhysicalPositions(ChannelDataInput network)
      throws IOException {
    final int positionsCount = network.readInt();
    final PhysicalPosition[] physicalPositions;
    if (positionsCount == 0) {
      physicalPositions = CommonConst.EMPTY_PHYSICAL_POSITIONS_ARRAY;
    } else {
      physicalPositions = new PhysicalPosition[positionsCount];

      for (int i = 0; i < physicalPositions.length; i++) {
        final PhysicalPosition position = new PhysicalPosition();

        position.clusterPosition = network.readLong();
        position.recordSize = network.readInt();
        position.recordVersion = network.readVersion();

        physicalPositions[i] = position;
      }
    }
    return physicalPositions;
  }

  public static RawPair<String[], int[]> readClustersArray(final ChannelDataInput network)
      throws IOException {
    final int tot = network.readShort();
    final String[] clusterNames = new String[tot];
    final int[] clusterIds = new int[tot];

    for (int i = 0; i < tot; ++i) {
      String clusterName = network.readString().toLowerCase(Locale.ENGLISH);
      final int clusterId = network.readShort();
      clusterNames[i] = clusterName;
      clusterIds[i] = clusterId;
    }

    return new RawPair<>(clusterNames, clusterIds);
  }

  public static void writeClustersArray(
      ChannelDataOutput channel, RawPair<String[], int[]> clusters, int protocolVersion)
      throws IOException {
    final String[] clusterNames = clusters.first;
    final int[] clusterIds = clusters.second;

    channel.writeShort((short) clusterNames.length);

    for (int i = 0; i < clusterNames.length; i++) {
      channel.writeString(clusterNames[i]);
      channel.writeShort((short) clusterIds[i]);
    }
  }

  public static void writeTransactionEntry(
      final DataOutput iNetwork, final RecordOperationRequest txEntry) throws IOException {
    iNetwork.writeByte(txEntry.getType());
    iNetwork.writeInt(txEntry.getId().getClusterId());
    iNetwork.writeLong(txEntry.getId().getClusterPosition());
    iNetwork.writeByte(txEntry.getRecordType());

    switch (txEntry.getType()) {
      case RecordOperation.CREATED:
        byte[] record = txEntry.getRecord();
        iNetwork.writeInt(record.length);
        iNetwork.write(record);
        break;

      case RecordOperation.UPDATED:
        iNetwork.writeInt(txEntry.getVersion());
        byte[] record2 = txEntry.getRecord();
        iNetwork.writeInt(record2.length);
        iNetwork.write(record2);
        iNetwork.writeBoolean(txEntry.isContentChanged());
        break;

      case RecordOperation.DELETED:
        iNetwork.writeInt(txEntry.getVersion());
        break;
    }
  }

  static void writeTransactionEntry(
      final ChannelDataOutput iNetwork,
      final RecordOperationRequest txEntry,
      RecordSerializer serializer)
      throws IOException {
    iNetwork.writeByte(txEntry.getType());
    iNetwork.writeRID(txEntry.getId());
    iNetwork.writeByte(txEntry.getRecordType());

    switch (txEntry.getType()) {
      case RecordOperation.CREATED:
        iNetwork.writeBytes(txEntry.getRecord());
        break;

      case RecordOperation.UPDATED:
        iNetwork.writeVersion(txEntry.getVersion());
        iNetwork.writeBytes(txEntry.getRecord());
        iNetwork.writeBoolean(txEntry.isContentChanged());
        break;

      case RecordOperation.DELETED:
        iNetwork.writeVersion(txEntry.getVersion());
        break;
    }
  }

  public static RecordOperationRequest readTransactionEntry(final DataInput iNetwork)
      throws IOException {
    RecordOperationRequest result = new RecordOperationRequest();
    result.setType(iNetwork.readByte());
    int clusterId = iNetwork.readInt();
    long clusterPosition = iNetwork.readLong();
    result.setId(new RecordId(clusterId, clusterPosition));
    result.setRecordType(iNetwork.readByte());

    switch (result.getType()) {
      case RecordOperation.CREATED:
        int length = iNetwork.readInt();
        byte[] record = new byte[length];
        iNetwork.readFully(record);
        result.setRecord(record);
        break;

      case RecordOperation.UPDATED:
        result.setVersion(iNetwork.readInt());
        int length2 = iNetwork.readInt();
        byte[] record2 = new byte[length2];
        iNetwork.readFully(record2);
        result.setRecord(record2);
        result.setContentChanged(iNetwork.readBoolean());
        break;

      case RecordOperation.DELETED:
        result.setVersion(iNetwork.readInt());
        break;
    }
    return result;
  }

  static RecordOperationRequest readTransactionEntry(
      ChannelDataInput channel, RecordSerializer ser) throws IOException {
    RecordOperationRequest entry = new RecordOperationRequest();
    entry.setType(channel.readByte());
    entry.setId(channel.readRID());
    entry.setRecordType(channel.readByte());
    switch (entry.getType()) {
      case RecordOperation.CREATED:
        entry.setRecord(channel.readBytes());
        break;
      case RecordOperation.UPDATED:
        entry.setVersion(channel.readVersion());
        entry.setRecord(channel.readBytes());
        entry.setContentChanged(channel.readBoolean());
        break;
      case RecordOperation.DELETED:
        entry.setVersion(channel.readVersion());
        break;
      default:
        break;
    }
    return entry;
  }

  public static Identifiable readIdentifiable(
      DatabaseSessionInternal db, final ChannelDataInput network, RecordSerializer serializer)
      throws IOException {
    final int classId = network.readShort();
    if (classId == ChannelBinaryProtocol.RECORD_NULL) {
      return null;
    }

    if (classId == ChannelBinaryProtocol.RECORD_RID) {
      return network.readRID();
    } else {
      final Record record = readRecordFromBytes(db, network, serializer);
      return record;
    }
  }

  private static Record readRecordFromBytes(
      DatabaseSessionInternal db, ChannelDataInput network, RecordSerializer serializer)
      throws IOException {
    byte rec = network.readByte();
    final RecordId rid = network.readRID();
    final int version = network.readVersion();
    final byte[] content = network.readBytes();

    RecordAbstract record =
        YouTrackDBEnginesManager.instance()
            .getRecordFactoryManager()
            .newInstance(rec, rid, db);
    RecordInternal.setVersion(record, version);
    serializer.fromStream(db, content, record, null);
    RecordInternal.unsetDirty(record);

    return record;
  }

  private static void writeProjection(DatabaseSessionInternal db, Result item,
      ChannelDataOutput channel)
      throws IOException {
    channel.writeByte(QueryResponse.RECORD_TYPE_PROJECTION);
    ResultSerializerNetwork ser = new ResultSerializerNetwork();
    ser.toStream(db, item, channel);
  }

  private static void writeBlob(
      DatabaseSessionInternal session, Result row, ChannelDataOutput channel,
      RecordSerializer recordSerializer)
      throws IOException {
    channel.writeByte(QueryResponse.RECORD_TYPE_BLOB);
    writeIdentifiable(session, channel, row.getBlob().get(), recordSerializer);
  }

  private static void writeVertex(
      DatabaseSessionInternal db, Result row, ChannelDataOutput channel,
      RecordSerializer recordSerializer)
      throws IOException {
    channel.writeByte(QueryResponse.RECORD_TYPE_VERTEX);
    writeDocument(db, channel, row.getEntity().get().getRecord(db), recordSerializer);
  }

  private static void writeElement(
      DatabaseSessionInternal db, Result row, ChannelDataOutput channel,
      RecordSerializer recordSerializer)
      throws IOException {
    channel.writeByte(QueryResponse.RECORD_TYPE_ELEMENT);
    writeDocument(db, channel, row.getEntity().get().getRecord(db), recordSerializer);
  }

  private static void writeEdge(
      DatabaseSessionInternal db, Result row, ChannelDataOutput channel,
      RecordSerializer recordSerializer)
      throws IOException {
    channel.writeByte(QueryResponse.RECORD_TYPE_EDGE);
    writeDocument(db, channel, row.getEntity().get().getRecord(db), recordSerializer);
  }

  private static void writeDocument(
      DatabaseSessionInternal session, ChannelDataOutput channel, EntityImpl entity,
      RecordSerializer serializer) throws IOException {
    writeIdentifiable(session, channel, entity, serializer);
  }

  public static void writeResult(
      DatabaseSessionInternal db, Result row, ChannelDataOutput channel,
      RecordSerializer recordSerializer)
      throws IOException {
    if (row.isBlob()) {
      writeBlob(db, row, channel, recordSerializer);
    } else if (row.isVertex()) {
      writeVertex(db, row, channel, recordSerializer);
    } else if (row.isEdge()) {
      writeEdge(db, row, channel, recordSerializer);
    } else if (row.isEntity()) {
      writeElement(db, row, channel, recordSerializer);
    } else {
      writeProjection(db, row, channel);
    }
  }

  private static ResultInternal readBlob(DatabaseSessionInternal db, ChannelDataInput channel)
      throws IOException {
    RecordSerializer serializer = RecordSerializerNetworkV37.INSTANCE;
    return new ResultInternal(db, readIdentifiable(db, channel, serializer));
  }

  public static ResultInternal readResult(DatabaseSessionInternal db, ChannelDataInput channel)
      throws IOException {
    byte type = channel.readByte();
    return switch (type) {
      case QueryResponse.RECORD_TYPE_BLOB -> readBlob(db, channel);
      case QueryResponse.RECORD_TYPE_VERTEX -> readVertex(db, channel);
      case QueryResponse.RECORD_TYPE_EDGE -> readEdge(db, channel);
      case QueryResponse.RECORD_TYPE_ELEMENT -> readElement(db, channel);
      case QueryResponse.RECORD_TYPE_PROJECTION -> readProjection(db, channel);
      default -> new ResultInternal(db);
    };
  }

  private static ResultInternal readElement(DatabaseSessionInternal db,
      ChannelDataInput channel)
      throws IOException {
    return new ResultInternal(db, readDocument(db, channel));
  }

  private static ResultInternal readVertex(DatabaseSessionInternal db,
      ChannelDataInput channel)
      throws IOException {
    return new ResultInternal(db, readDocument(db, channel));
  }

  private static ResultInternal readEdge(DatabaseSessionInternal db, ChannelDataInput channel)
      throws IOException {
    return new ResultInternal(db, readDocument(db, channel));
  }

  private static Record readDocument(DatabaseSessionInternal db, ChannelDataInput channel)
      throws IOException {
    RecordSerializer serializer = RecordSerializerNetworkV37Client.INSTANCE;
    return (Record) readIdentifiable(db, channel, serializer);
  }

  private static ResultInternal readProjection(DatabaseSessionInternal db,
      ChannelDataInput channel) throws IOException {
    ResultSerializerNetwork ser = new ResultSerializerNetwork();
    return ser.fromStream(db, channel);
  }
}