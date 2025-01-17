/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.jetbrains.youtrack.db.internal.jdbc;

import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import java.sql.RowId;

public class YouTrackDbRowId implements RowId {

  protected final RecordId rid;

  public YouTrackDbRowId(final RecordId rid) {
    this.rid = rid;
  }

  @Override
  public byte[] getBytes() {
    return rid.toStream();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof YouTrackDbRowId) {
      return rid.equals(((YouTrackDbRowId) obj).rid);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return rid.hashCode();
  }

  @Override
  public String toString() {
    return rid.toString();
  }
}
