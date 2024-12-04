package com.orientechnologies.orient.core.tx;

import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ODocumentSerializerDelta;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerNetworkDistributed;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public class OTransactionDataChange {

  private byte type;
  private byte recordType;
  private YTRID id;
  private Optional<byte[]> record;
  private int version;
  private boolean contentChanged;

  public OTransactionDataChange(YTDatabaseSessionInternal session, ORecordOperation operation) {
    this.type = operation.type;
    var rec = operation.record;
    this.recordType = ORecordInternal.getRecordType(rec);
    this.id = rec.getIdentity();
    this.version = rec.getVersion();
    switch (operation.type) {
      case ORecordOperation.CREATED:
        this.record = Optional.of(
            ORecordSerializerNetworkDistributed.INSTANCE.toStream(session, rec));
        this.contentChanged = ORecordInternal.isContentChanged(rec);
        break;
      case ORecordOperation.UPDATED:
        if (recordType == YTDocument.RECORD_TYPE) {
          record = Optional.of(
              ODocumentSerializerDelta.instance().serializeDelta((YTDocument) rec));
        } else {
          record = Optional.of(ORecordSerializerNetworkDistributed.INSTANCE.toStream(session, rec));
        }
        this.contentChanged = ORecordInternal.isContentChanged(rec);
        break;
      case ORecordOperation.DELETED:
        record = Optional.empty();
        break;
    }
  }

  private OTransactionDataChange() {
  }

  public OTransactionDataChange(
      byte type,
      byte recordType,
      YTRID id,
      Optional<byte[]> record,
      int version,
      boolean contentChanged) {
    this.type = type;
    this.recordType = recordType;
    this.id = id;
    this.record = record;
    this.version = version;
    this.contentChanged = contentChanged;
  }

  public void serialize(DataOutput output) throws IOException {
    output.writeByte(type);
    output.writeByte(recordType);
    output.writeInt(id.getClusterId());
    output.writeLong(id.getClusterPosition());
    if (record.isPresent()) {
      output.writeBoolean(true);
      output.writeInt(record.get().length);
      output.write(record.get(), 0, record.get().length);
    } else {
      output.writeBoolean(false);
    }
    output.writeInt(this.version);
    output.writeBoolean(this.contentChanged);
  }

  public static OTransactionDataChange deserialize(DataInput input) throws IOException {
    OTransactionDataChange change = new OTransactionDataChange();
    change.type = input.readByte();
    change.recordType = input.readByte();
    int cluster = input.readInt();
    long position = input.readLong();
    change.id = new YTRecordId(cluster, position);
    boolean isThereRecord = input.readBoolean();
    if (isThereRecord) {
      int size = input.readInt();
      byte[] record = new byte[size];
      input.readFully(record);
      change.record = Optional.of(record);
    } else {
      change.record = Optional.empty();
    }
    change.version = input.readInt();
    change.contentChanged = input.readBoolean();
    return change;
  }

  public byte getRecordType() {
    return recordType;
  }

  public byte getType() {
    return type;
  }

  public int getVersion() {
    return version;
  }

  public Optional<byte[]> getRecord() {
    return record;
  }

  public boolean isContentChanged() {
    return contentChanged;
  }

  public YTRID getId() {
    return id;
  }
}
