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
package com.orientechnologies.orient.core.index;

import com.orientechnologies.orient.core.collate.OCollate;
import com.orientechnologies.orient.core.collate.ODefaultCollate;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.OSQLEngine;

/**
 * Abstract index definition implementation.
 */
public abstract class OAbstractIndexDefinition implements OIndexDefinition {

  protected OCollate collate = new ODefaultCollate();
  private boolean nullValuesIgnored = true;

  protected OAbstractIndexDefinition() {
  }

  public OCollate getCollate() {
    return collate;
  }

  public void setCollate(final OCollate collate) {
    if (collate == null) {
      throw new IllegalArgumentException("COLLATE cannot be null");
    }
    this.collate = collate;
  }

  public void setCollate(String iCollate) {
    if (iCollate == null) {
      iCollate = ODefaultCollate.NAME;
    }

    setCollate(OSQLEngine.getCollate(iCollate));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OAbstractIndexDefinition that = (OAbstractIndexDefinition) o;

    if (!collate.equals(that.collate)) {
      return false;
    }

    return nullValuesIgnored == that.nullValuesIgnored;
  }

  @Override
  public int hashCode() {
    int result = collate.hashCode();
    result = 31 * result + (nullValuesIgnored ? 1 : 0);
    return result;
  }

  @Override
  public boolean isNullValuesIgnored() {
    return nullValuesIgnored;
  }

  @Override
  public void setNullValuesIgnored(boolean value) {
    nullValuesIgnored = value;
  }

  protected void serializeToStream(YTDocument document) {
  }

  protected void serializeFromStream(YTDocument document) {
  }

  protected static <T> T refreshRid(YTDatabaseSessionInternal session, T value) {
    if (value instanceof YTRID rid) {
      if (rid.isNew()) {
        try {
          var record = session.load(rid);
          //noinspection unchecked
          value = (T) record.getIdentity();
        } catch (ORecordNotFoundException rnf) {
          return value;
        }
      }
    }
    return value;
  }
}
