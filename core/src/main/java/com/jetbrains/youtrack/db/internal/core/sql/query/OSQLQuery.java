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
package com.jetbrains.youtrack.db.internal.core.sql.query;

import com.jetbrains.youtrack.db.internal.common.util.OCommonConst;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequestText;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTQueryParsingException;
import com.jetbrains.youtrack.db.internal.core.exception.YTSerializationException;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTImmutableSchema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.query.QueryAbstract;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.OMemoryStream;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.ORecordSerializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * SQL query implementation.
 *
 * @param <T> Record type to return.
 */
@SuppressWarnings("serial")
public abstract class OSQLQuery<T> extends QueryAbstract<T> implements CommandRequestText {

  protected String text;

  public OSQLQuery() {
  }

  public OSQLQuery(final String iText) {
    text = iText.trim();
  }

  /**
   * Delegates to the OQueryExecutor the query execution.
   */
  @SuppressWarnings("unchecked")
  public List<T> run(final Object... iArgs) {
    final YTDatabaseSessionInternal database = ODatabaseRecordThreadLocal.instance().get();
    if (database == null) {
      throw new YTQueryParsingException("No database configured");
    }

    database.getMetadata().makeThreadLocalSchemaSnapshot();
    try {
      setParameters(iArgs);
      Object o = database.getStorage().command(database, this);
      if (o instanceof List) {
        return (List<T>) o;
      } else {
        return (List<T>) Collections.singletonList(o);
      }

    } finally {
      database.getMetadata().clearThreadLocalSchemaSnapshot();
    }
  }

  /**
   * Returns only the first record if any.
   */
  public T runFirst(YTDatabaseSessionInternal database, final Object... iArgs) {
    setLimit(1);
    final List<T> result = execute(database, iArgs);
    return result != null && !result.isEmpty() ? result.get(0) : null;
  }

  public String getText() {
    return text;
  }

  public CommandRequestText setText(final String iText) {
    text = iText;
    return this;
  }

  @Override
  public String toString() {
    return "sql." + text;
  }

  public CommandRequestText fromStream(YTDatabaseSessionInternal db, final byte[] iStream,
      ORecordSerializer serializer)
      throws YTSerializationException {
    final OMemoryStream buffer = new OMemoryStream(iStream);

    queryFromStream(db, buffer, serializer);

    return this;
  }

  public byte[] toStream() throws YTSerializationException {
    return queryToStream().toByteArray();
  }

  protected OMemoryStream queryToStream() {
    final OMemoryStream buffer = new OMemoryStream();

    buffer.setUtf8(text); // TEXT AS STRING
    buffer.set(limit); // LIMIT AS INTEGER
    buffer.setUtf8(fetchPlan != null ? fetchPlan : "");

    buffer.set(serializeQueryParameters(parameters));

    return buffer;
  }

  protected void queryFromStream(YTDatabaseSessionInternal db, final OMemoryStream buffer,
      ORecordSerializer serializer) {
    text = buffer.getAsString();
    limit = buffer.getAsInteger();

    setFetchPlan(buffer.getAsString());

    final byte[] paramBuffer = buffer.getAsByteArray();
    parameters = deserializeQueryParameters(db, paramBuffer, serializer);
  }

  protected Map<Object, Object> deserializeQueryParameters(
      YTDatabaseSessionInternal db, final byte[] paramBuffer, ORecordSerializer serializer) {
    if (paramBuffer == null || paramBuffer.length == 0) {
      return Collections.emptyMap();
    }

    final EntityImpl param = new EntityImpl();

    YTImmutableSchema schema =
        ODatabaseRecordThreadLocal.instance().get().getMetadata().getImmutableSchemaSnapshot();
    serializer.fromStream(db, paramBuffer, param, null);
    param.setFieldType("params", YTType.EMBEDDEDMAP);
    final Map<String, Object> params = param.rawField("params");

    final Map<Object, Object> result = new HashMap<Object, Object>();
    for (Entry<String, Object> p : params.entrySet()) {
      if (Character.isDigit(p.getKey().charAt(0))) {
        result.put(Integer.parseInt(p.getKey()), p.getValue());
      } else {
        result.put(p.getKey(), p.getValue());
      }
    }
    return result;
  }

  protected byte[] serializeQueryParameters(final Map<Object, Object> params) {
    if (params == null || params.size() == 0)
    // NO PARAMETER, JUST SEND 0
    {
      return OCommonConst.EMPTY_BYTE_ARRAY;
    }

    final EntityImpl param = new EntityImpl();
    param.field("params", convertToRIDsIfPossible(params));
    return param.toStream();
  }

  @SuppressWarnings("unchecked")
  private Map<Object, Object> convertToRIDsIfPossible(final Map<Object, Object> params) {
    final Map<Object, Object> newParams = new HashMap<Object, Object>(params.size());

    for (Entry<Object, Object> entry : params.entrySet()) {
      final Object value = entry.getValue();

      if (value instanceof Set<?>
          && !((Set<?>) value).isEmpty()
          && ((Set<?>) value).iterator().next() instanceof Record) {
        // CONVERT RECORDS AS RIDS
        final Set<YTRID> newSet = new HashSet<YTRID>();
        for (Record rec : (Set<Record>) value) {
          newSet.add(rec.getIdentity());
        }
        newParams.put(entry.getKey(), newSet);

      } else if (value instanceof List<?>
          && !((List<?>) value).isEmpty()
          && ((List<?>) value).get(0) instanceof Record) {
        // CONVERT RECORDS AS RIDS
        final List<YTRID> newList = new ArrayList<YTRID>();
        for (Record rec : (List<Record>) value) {
          newList.add(rec.getIdentity());
        }
        newParams.put(entry.getKey(), newList);

      } else if (value instanceof Map<?, ?>
          && !((Map<?, ?>) value).isEmpty()
          && ((Map<?, ?>) value).values().iterator().next() instanceof Record) {
        // CONVERT RECORDS AS RIDS
        final Map<Object, YTRID> newMap = new HashMap<Object, YTRID>();
        for (Entry<?, Record> mapEntry : ((Map<?, Record>) value).entrySet()) {
          newMap.put(mapEntry.getKey(), mapEntry.getValue().getIdentity());
        }
        newParams.put(entry.getKey(), newMap);
      } else if (value instanceof YTIdentifiable) {
        newParams.put(entry.getKey(), ((YTIdentifiable) value).getIdentity());
      } else {
        newParams.put(entry.getKey(), value);
      }
    }

    return newParams;
  }
}