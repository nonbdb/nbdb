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
package com.jetbrains.youtrack.db.internal.core.db.record;

import com.jetbrains.youtrack.db.internal.common.collection.OMultiValue;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.record.Record;

/**
 * Lazy implementation of ArrayList. It's bound to a source Record object to keep track of changes.
 * This avoid to call the makeDirty() by hand when the list is changed.
 */
public class ORecordMultiValueHelper {

  public enum MULTIVALUE_CONTENT_TYPE {
    EMPTY,
    ALL_RECORDS,
    ALL_RIDS,
    HYBRID
  }

  public static MULTIVALUE_CONTENT_TYPE updateContentType(
      final MULTIVALUE_CONTENT_TYPE iPreviousStatus, final Object iValue) {
    if (iPreviousStatus == MULTIVALUE_CONTENT_TYPE.HYBRID) {
      // DO NOTHING

    } else if (iPreviousStatus == MULTIVALUE_CONTENT_TYPE.EMPTY) {
      if (iValue instanceof YTRID) {
        return MULTIVALUE_CONTENT_TYPE.ALL_RIDS;
      } else if (iValue instanceof Record) {
        return MULTIVALUE_CONTENT_TYPE.ALL_RECORDS;
      } else {
        return MULTIVALUE_CONTENT_TYPE.HYBRID;
      }

    } else if (iPreviousStatus == MULTIVALUE_CONTENT_TYPE.ALL_RECORDS) {
      if (iValue instanceof YTRID) {
        return MULTIVALUE_CONTENT_TYPE.HYBRID;
      }

    } else if (iPreviousStatus == MULTIVALUE_CONTENT_TYPE.ALL_RIDS) {
      if (!(iValue instanceof YTRID)) {
        return MULTIVALUE_CONTENT_TYPE.HYBRID;
      }
    }
    return iPreviousStatus;
  }

  public static String toString(final Object iMultivalue) {
    return OMultiValue.toString(iMultivalue);
  }
}