/*
 *
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.fetch;

import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTFetchException;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;

/**
 *
 */
public interface OFetchContext {

  void onBeforeFetch(final EntityImpl iRootRecord) throws YTFetchException;

  void onAfterFetch(final EntityImpl iRootRecord) throws YTFetchException;

  void onBeforeArray(
      final EntityImpl iRootRecord,
      final String iFieldName,
      final Object iUserObject,
      final YTIdentifiable[] iArray)
      throws YTFetchException;

  void onAfterArray(final EntityImpl iRootRecord, final String iFieldName,
      final Object iUserObject)
      throws YTFetchException;

  void onBeforeCollection(
      final EntityImpl iRootRecord,
      final String iFieldName,
      final Object iUserObject,
      final Iterable<?> iterable)
      throws YTFetchException;

  void onAfterCollection(
      final EntityImpl iRootRecord, final String iFieldName, final Object iUserObject)
      throws YTFetchException;

  void onBeforeMap(final EntityImpl iRootRecord, final String iFieldName,
      final Object iUserObject)
      throws YTFetchException;

  void onAfterMap(final EntityImpl iRootRecord, final String iFieldName, final Object iUserObject)
      throws YTFetchException;

  void onBeforeDocument(
      final EntityImpl iRecord,
      final EntityImpl iDocument,
      final String iFieldName,
      final Object iUserObject)
      throws YTFetchException;

  void onAfterDocument(
      final EntityImpl iRootRecord,
      final EntityImpl iDocument,
      final String iFieldName,
      final Object iUserObject)
      throws YTFetchException;

  void onBeforeStandardField(
      final Object iFieldValue, final String iFieldName, final Object iUserObject,
      YTType fieldType);

  void onAfterStandardField(
      final Object iFieldValue, final String iFieldName, final Object iUserObject,
      YTType fieldType);

  boolean fetchEmbeddedDocuments();
}