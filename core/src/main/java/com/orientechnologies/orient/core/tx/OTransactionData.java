package com.orientechnologies.orient.core.tx;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.YouTrackDBManager;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.record.YTRecordAbstract;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ODocumentSerializerDelta;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerNetworkDistributed;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OTransactionData {

  private final OTransactionId transactionId;
  private final List<OTransactionDataChange> changes = new ArrayList<>();

  public OTransactionData(OTransactionId transactionId) {
    this.transactionId = transactionId;
  }

  public static OTransactionData read(DataInput dataInput) throws IOException {
    OTransactionId transactionId = OTransactionId.read(dataInput);
    int entries = dataInput.readInt();
    OTransactionData data = new OTransactionData(transactionId);
    while (entries-- > 0) {
      data.changes.add(OTransactionDataChange.deserialize(dataInput));
    }
    return data;
  }

  public void addRecord(byte[] record) {
    try {
      changes.add(
          OTransactionDataChange.deserialize(
              new DataInputStream(new ByteArrayInputStream(record))));
    } catch (IOException e) {
      throw OException.wrapException(
          new ODatabaseException("error reading transaction data change record"), e);
    }
  }

  public void addChange(OTransactionDataChange change) {
    this.changes.add(change);
  }

  public OTransactionId getTransactionId() {
    return transactionId;
  }

  public List<OTransactionDataChange> getChanges() {
    return changes;
  }

  public void write(DataOutput output) throws IOException {
    transactionId.write(output);
    output.writeInt(changes.size());
    for (OTransactionDataChange change : changes) {
      change.serialize(output);
    }
  }

  public void fill(OTransactionInternal transaction, YTDatabaseSessionInternal database) {
    transaction.fill(
        changes.stream()
            .map(
                (x) -> {
                  ORecordOperation operation = new ORecordOperation(null, x.getType());
                  // TODO: Handle dirty no changed
                  YTRecordAbstract record = null;
                  switch (x.getType()) {
                    case ORecordOperation.CREATED: {
                      record =
                          ORecordSerializerNetworkDistributed.INSTANCE.fromStream(database,
                              x.getRecord().get(), null);
                      ORecordInternal.setRecordSerializer(record, database.getSerializer());
                      break;
                    }
                    case ORecordOperation.UPDATED: {
                      if (x.getRecordType() == YTDocument.RECORD_TYPE) {
                        try {
                          record = database.load(x.getId());
                        } catch (ORecordNotFoundException rnf) {
                          record = new YTDocument();
                        }

                        ((YTDocument) record).deserializeFields();
                        ODocumentInternal.clearTransactionTrackData((YTDocument) record);
                        ODocumentSerializerDelta.instance()
                            .deserializeDelta(database, x.getRecord().get(), (YTDocument) record);
                        /// Got record with empty deltas, at this level we mark the record dirty
                        // anyway.
                        record.setDirty();
                      } else {
                        record =
                            ORecordSerializerNetworkDistributed.INSTANCE.fromStream(database,
                                x.getRecord().get(), null);
                        ORecordInternal.setRecordSerializer(record, database.getSerializer());
                      }
                      break;
                    }
                    case ORecordOperation.DELETED: {
                      try {
                        record = database.load(x.getId());
                      } catch (ORecordNotFoundException rnf) {
                        record =
                            YouTrackDBManager.instance()
                                .getRecordFactoryManager()
                                .newInstance(x.getRecordType(), x.getId(), database);
                      }
                      break;
                    }
                  }
                  ORecordInternal.setIdentity(record, (YTRecordId) x.getId());
                  ORecordInternal.setVersion(record, x.getVersion());
                  operation.record = record;

                  return operation;
                })
            .iterator());
  }

  @Override
  public String toString() {
    return "id:" + transactionId + " nchanges:" + changes.size() + "]";
  }
}
