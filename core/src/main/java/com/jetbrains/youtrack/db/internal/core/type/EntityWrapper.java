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
package com.jetbrains.youtrack.db.internal.core.type;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RID;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.io.Serializable;

/**
 * Base abstract class to wrap a document.
 */
@SuppressWarnings("unchecked")
public class EntityWrapper implements Serializable {

  private EntityImpl document;

  public EntityWrapper() {
  }

  public EntityWrapper(final String iClassName) {
    this(new EntityImpl(iClassName));
  }

  public EntityWrapper(final EntityImpl iDocument) {
    document = iDocument;
  }

  public void fromStream(DatabaseSessionInternal session, final EntityImpl iDocument) {
    document = iDocument;
  }

  public EntityImpl toStream(DatabaseSession session) {
    return getDocument(session);
  }

  public <RET extends EntityWrapper> RET save(DatabaseSessionInternal session) {
    document.save();
    return (RET) this;
  }

  public <RET extends EntityWrapper> RET save(final String iClusterName) {
    document.save(iClusterName);
    return (RET) this;
  }

  public EntityImpl getDocument(DatabaseSession session) {
    if (document != null && document.isNotBound(session)) {
      document = session.bindToSession(document);
    }

    return document;
  }

  public void setDocument(DatabaseSessionInternal session, EntityImpl document) {
    if (document != null && document.isNotBound(session)) {
      document = session.bindToSession(document);
    }

    this.document = document;
  }

  public RID getIdentity() {
    return document.getIdentity();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((document == null) ? 0 : document.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final EntityWrapper other = (EntityWrapper) obj;
    if (document == null) {
      return other.document == null;
    } else {
      return document.equals(other.document);
    }
  }

  @Override
  public String toString() {
    return document != null ? document.toString() : "?";
  }
}
