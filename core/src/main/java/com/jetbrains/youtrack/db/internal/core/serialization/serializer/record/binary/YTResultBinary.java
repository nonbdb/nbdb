/*
 * Copyright 2018 YouTrackDB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary;

import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTImmutableSchema;
import com.jetbrains.youtrack.db.internal.core.record.Edge;
import com.jetbrains.youtrack.db.internal.core.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.record.impl.Blob;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 *
 */
public class YTResultBinary implements YTResult {

  private final ODocumentSerializer serializer;
  @Nullable
  private final YTRecordId id;
  private final byte[] bytes;
  private final int offset;
  private final int fieldLength;
  private final YTImmutableSchema schema;
  private final YTDatabaseSessionInternal db;

  public YTResultBinary(
      YTDatabaseSessionInternal db,
      YTImmutableSchema schema,
      byte[] bytes,
      int offset,
      int fieldLength,
      ODocumentSerializer serializer) {
    this.schema = schema;
    this.id = null;
    this.bytes = bytes;
    this.serializer = serializer;
    this.offset = offset;
    this.fieldLength = fieldLength;
    this.db = db;

  }

  public YTResultBinary(
      YTDatabaseSessionInternal db,
      byte[] bytes,
      int offset,
      int fieldLength,
      ODocumentSerializer serializer,
      @Nullable YTRecordId id) {
    schema = db.getMetadata().getImmutableSchemaSnapshot();
    this.id = id;
    this.bytes = bytes;
    this.serializer = serializer;
    this.offset = offset;
    this.fieldLength = fieldLength;
    this.db = db;
  }

  public int getFieldLength() {
    return fieldLength;
  }

  public int getOffset() {
    return offset;
  }

  public byte[] getBytes() {
    return bytes;
  }

  @Override
  public <T> T getProperty(String name) {
    BytesContainer bytes = new BytesContainer(this.bytes);
    bytes.skip(offset);
    return serializer.deserializeFieldTyped(db, bytes, name, id == null, schema, null);
  }

  @Override
  public Entity getEntityProperty(String name) {
    throw new UnsupportedOperationException(
        "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Vertex getVertexProperty(String name) {
    throw new UnsupportedOperationException(
        "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Edge getEdgeProperty(String name) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Blob getBlobProperty(String name) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Set<String> getPropertyNames() {
    final BytesContainer container = new BytesContainer(bytes);
    container.skip(offset);
    // TODO: use something more correct that new EntityImpl
    String[] fields = serializer.getFieldNames(new EntityImpl(), container, id == null);
    return new HashSet<>(Arrays.asList(fields));
  }

  @Override
  public Optional<YTRID> getIdentity() {
    return Optional.ofNullable(id);
  }

  @Nullable
  @Override
  public YTRID getRecordId() {
    return id;
  }

  @Override
  public boolean isEntity() {
    return true;
  }

  @Override
  public boolean isRecord() {
    return true;
  }

  @Override
  public Optional<Entity> getEntity() {
    return Optional.of(toDocument());
  }

  @Override
  public Entity asEntity() {
    return toDocument();
  }

  @Override
  public Entity toEntity() {
    return toDocument();
  }

  @Override
  public boolean isBlob() {
    return false;
  }

  @Override
  public Optional<Blob> getBlob() {
    return Optional.empty();
  }

  @Override
  public Optional<Record> getRecord() {
    return Optional.of(toDocument());
  }

  @Override
  public boolean isProjection() {
    return false;
  }

  @Override
  public Object getMetadata(String key) {
    return null;
  }

  @Override
  public Set<String> getMetadataKeys() {
    return null;
  }

  @Override
  public boolean hasProperty(String varName) {
    throw new UnsupportedOperationException(
        "Not supported yet."); // To change body of generated methods, choose Tools | Templates.
  }

  private EntityImpl toDocument() {
    EntityImpl doc = new EntityImpl();
    BytesContainer bytes = new BytesContainer(this.bytes);
    bytes.skip(offset);

    serializer.deserialize(db, doc, bytes);
    return doc;
  }
}