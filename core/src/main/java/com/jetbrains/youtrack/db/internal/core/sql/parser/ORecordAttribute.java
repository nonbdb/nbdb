/* Generated By:JJTree: Do not edit this line. ORecordAttribute.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.YTRecordNotFoundException;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.ORecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.RecordBytes;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultInternal;
import java.util.Map;
import java.util.Objects;

public class ORecordAttribute extends SimpleNode {

  protected String name;

  public ORecordAttribute(int id) {
    super(id);
  }

  public ORecordAttribute(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(name);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append(name);
  }

  public ORecordAttribute copy() {
    ORecordAttribute result = new ORecordAttribute(-1);
    result.name = name;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ORecordAttribute that = (ORecordAttribute) o;

    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public YTResult serialize(YTDatabaseSessionInternal db) {
    YTResultInternal result = new YTResultInternal(db);
    result.setProperty("name", name);
    return result;
  }

  public void deserialize(YTResult fromResult) {
    name = fromResult.getProperty("name");
  }

  public Object evaluate(YTResult iCurrentRecord, CommandContext ctx) {
    if (name.equalsIgnoreCase("@rid")) {
      YTRID identity = iCurrentRecord.getIdentity().orElse(null);
      if (identity == null) {
        identity = iCurrentRecord.getProperty(name);
      }
      return identity;
    } else if (name.equalsIgnoreCase("@class")) {
      var element = iCurrentRecord.toEntity();
      if (element != null) {
        return element.getSchemaType().map(YTClass::getName).orElse(null);
      }
      return null;
    } else if (name.equalsIgnoreCase("@version")) {
      return iCurrentRecord.getRecord().map(Record::getVersion).orElse(null);
    } else if (name.equals("@type")) {
      return iCurrentRecord
          .getRecord()
          .map(
              r -> {
                var recordType = ORecordInternal.getRecordType(r);
                if (recordType == EntityImpl.RECORD_TYPE) {
                  return "document";
                } else if (recordType == RecordBytes.RECORD_TYPE) {
                  return "bytes";
                } else {
                  return "unknown";
                }
              })
          .orElse(null);
    } else if (name.equals("@size")) {
      return iCurrentRecord
          .getRecord()
          .map(r -> ((RecordAbstract) r).toStream().length)
          .orElse(null);
    } else if (name.equals("@raw")) {
      return iCurrentRecord.getRecord().map(r -> ((RecordAbstract) r).toStream()).orElse(null);
    } else if (name.equals("@rid")) {
      return iCurrentRecord.getIdentity().orElse(null);
    }

    return null;
  }

  public Object evaluate(Entity iCurrentRecord, CommandContext ctx) {
    if (iCurrentRecord == null) {
      return null;
    }
    if (name.equalsIgnoreCase("@rid")) {
      return iCurrentRecord.getIdentity();
    } else if (name.equalsIgnoreCase("@class")) {
      return iCurrentRecord.getSchemaType().map(YTClass::getName).orElse(null);
    } else if (name.equalsIgnoreCase("@version")) {
      try {
        Record record = iCurrentRecord.getRecord();
        return record.getVersion();
      } catch (YTRecordNotFoundException e) {
        return null;
      }
    }
    return null;
  }
}
/* JavaCC - OriginalChecksum=45ce3cd16399dec7d7ef89f8920d02ae (do not edit this line) */
