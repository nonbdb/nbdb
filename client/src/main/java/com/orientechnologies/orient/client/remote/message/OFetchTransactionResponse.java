package com.orientechnologies.orient.client.remote.message;

import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.orientechnologies.orient.client.remote.message.tx.IndexChange;
import com.orientechnologies.orient.client.remote.message.tx.ORecordOperationRequest;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ORecordOperation;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.record.ORecordInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.ORecordSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.ORecordSerializerNetworkV37;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.ORecordSerializerNetworkV37Client;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionIndexChanges;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.OChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class OFetchTransactionResponse implements OBinaryResponse {

  private long txId;
  private List<ORecordOperationRequest> operations;
  private List<IndexChange> indexChanges;

  public OFetchTransactionResponse() {
  }

  public OFetchTransactionResponse(
      YTDatabaseSessionInternal session, long txId,
      Iterable<ORecordOperation> operations,
      Map<String, OTransactionIndexChanges> indexChanges,
      Map<YTRID, YTRID> updatedRids) {
    // In some cases the reference are update twice is not yet possible to guess what is the id in
    // the client
    Map<YTRID, YTRID> reversed =
        updatedRids.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    this.txId = txId;
    this.indexChanges = new ArrayList<>();
    List<ORecordOperationRequest> netOperations = new ArrayList<>();
    for (ORecordOperation txEntry : operations) {
      ORecordOperationRequest request = new ORecordOperationRequest();
      request.setType(txEntry.type);
      request.setVersion(txEntry.record.getVersion());
      request.setId(txEntry.getRID());
      YTRID oldID = reversed.get(txEntry.getRID());
      request.setOldId(oldID != null ? oldID : txEntry.getRID());
      request.setRecordType(ORecordInternal.getRecordType(txEntry.record));
      request.setRecord(
          ORecordSerializerNetworkV37Client.INSTANCE.toStream(session, txEntry.record));
      request.setContentChanged(ORecordInternal.isContentChanged(txEntry.record));
      netOperations.add(request);
    }
    this.operations = netOperations;

    for (Map.Entry<String, OTransactionIndexChanges> change : indexChanges.entrySet()) {
      this.indexChanges.add(new IndexChange(change.getKey(), change.getValue()));
    }
  }

  @Override
  public void write(YTDatabaseSessionInternal session, OChannelDataOutput channel,
      int protocolVersion, ORecordSerializer serializer)
      throws IOException {
    channel.writeLong(txId);

    for (ORecordOperationRequest txEntry : operations) {
      writeTransactionEntry(channel, txEntry, serializer);
    }

    // END OF RECORD ENTRIES
    channel.writeByte((byte) 0);

    // SEND MANUAL INDEX CHANGES
    OMessageHelper.writeTransactionIndexChanges(
        channel, (ORecordSerializerNetworkV37) serializer, indexChanges);
  }

  static void writeTransactionEntry(
      final OChannelDataOutput iNetwork,
      final ORecordOperationRequest txEntry,
      ORecordSerializer serializer)
      throws IOException {
    iNetwork.writeByte((byte) 1);
    iNetwork.writeByte(txEntry.getType());
    iNetwork.writeRID(txEntry.getId());
    iNetwork.writeRID(txEntry.getOldId());
    iNetwork.writeByte(txEntry.getRecordType());

    switch (txEntry.getType()) {
      case ORecordOperation.CREATED:
        iNetwork.writeBytes(txEntry.getRecord());
        break;

      case ORecordOperation.UPDATED:
        iNetwork.writeVersion(txEntry.getVersion());
        iNetwork.writeBytes(txEntry.getRecord());
        iNetwork.writeBoolean(txEntry.isContentChanged());
        break;

      case ORecordOperation.DELETED:
        iNetwork.writeVersion(txEntry.getVersion());
        iNetwork.writeBytes(txEntry.getRecord());
        break;
    }
  }

  @Override
  public void read(YTDatabaseSessionInternal db, OChannelDataInput network,
      OStorageRemoteSession session) throws IOException {
    ORecordSerializerNetworkV37Client serializer = ORecordSerializerNetworkV37Client.INSTANCE;
    txId = network.readLong();
    operations = new ArrayList<>();
    byte hasEntry;
    do {
      hasEntry = network.readByte();
      if (hasEntry == 1) {
        ORecordOperationRequest entry = readTransactionEntry(network, serializer);
        operations.add(entry);
      }
    } while (hasEntry == 1);

    // RECEIVE MANUAL INDEX CHANGES
    this.indexChanges = OMessageHelper.readTransactionIndexChanges(db, network, serializer);
  }

  static ORecordOperationRequest readTransactionEntry(
      OChannelDataInput channel, ORecordSerializer ser) throws IOException {
    ORecordOperationRequest entry = new ORecordOperationRequest();
    entry.setType(channel.readByte());
    entry.setId(channel.readRID());
    entry.setOldId(channel.readRID());
    entry.setRecordType(channel.readByte());
    switch (entry.getType()) {
      case ORecordOperation.CREATED:
        entry.setRecord(channel.readBytes());
        break;
      case ORecordOperation.UPDATED:
        entry.setVersion(channel.readVersion());
        entry.setRecord(channel.readBytes());
        entry.setContentChanged(channel.readBoolean());
        break;
      case ORecordOperation.DELETED:
        entry.setVersion(channel.readVersion());
        entry.setRecord(channel.readBytes());
        break;
      default:
        break;
    }
    return entry;
  }

  public long getTxId() {
    return txId;
  }

  public List<ORecordOperationRequest> getOperations() {
    return operations;
  }

  public List<IndexChange> getIndexChanges() {
    return indexChanges;
  }
}
