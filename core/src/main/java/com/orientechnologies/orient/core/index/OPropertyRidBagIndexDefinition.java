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

import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.OMultiValueChangeEvent;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Index definition for index which is bound to field with type {@link OType#LINKBAG} .
 *
 * @since 1/30/14
 */
public class OPropertyRidBagIndexDefinition extends OPropertyIndexDefinition
    implements OIndexDefinitionMultiValue {

  public OPropertyRidBagIndexDefinition() {
  }

  public OPropertyRidBagIndexDefinition(String className, String field) {
    super(className, field, OType.LINK);
  }

  @Override
  public Object createSingleValue(ODatabaseSessionInternal session, Object... param) {
    return OType.convert(session, refreshRid(session, param[0]), keyType.getDefaultJavaType());
  }

  public void processChangeEvent(
      ODatabaseSessionInternal session,
      final OMultiValueChangeEvent<?, ?> changeEvent,
      final Object2IntMap<Object> keysToAdd,
      final Object2IntMap<Object> keysToRemove) {
    switch (changeEvent.getChangeType()) {
      case ADD: {
        processAdd(createSingleValue(session, changeEvent.getValue()), keysToAdd, keysToRemove);
        break;
      }
      case REMOVE: {
        processRemoval(
            createSingleValue(session, changeEvent.getOldValue()), keysToAdd, keysToRemove);
        break;
      }
      default:
        throw new IllegalArgumentException("Invalid change type : " + changeEvent.getChangeType());
    }
  }

  @Override
  public Object getDocumentValueToIndex(ODatabaseSessionInternal session, ODocument iDocument) {
    return createValue(session, iDocument.<Object>field(field));
  }

  @Override
  public Object createValue(ODatabaseSessionInternal session, final List<?> params) {
    if (!(params.get(0) instanceof ORidBag ridBag)) {
      return null;
    }
    final List<Object> values = new ArrayList<>();
    for (final OIdentifiable item : ridBag) {
      values.add(createSingleValue(session, item.getIdentity()));
    }

    return values;
  }

  @Override
  public Object createValue(ODatabaseSessionInternal session, final Object... params) {
    if (!(params[0] instanceof ORidBag ridBag)) {
      return null;
    }
    final List<Object> values = new ArrayList<>();
    for (final OIdentifiable item : ridBag) {
      values.add(createSingleValue(session, item.getIdentity()));
    }

    return values;
  }

  @Override
  public String toCreateIndexDDL(String indexName, String indexType, String engine) {
    return createIndexDDLWithoutFieldType(indexName, indexType, engine).toString();
  }
}
