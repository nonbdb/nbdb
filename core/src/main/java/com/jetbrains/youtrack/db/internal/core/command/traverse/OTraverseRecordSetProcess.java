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
package com.jetbrains.youtrack.db.internal.core.command.traverse;

import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Collection;
import java.util.Iterator;

public class OTraverseRecordSetProcess extends OTraverseAbstractProcess<Iterator<YTIdentifiable>> {

  private final OTraversePath path;
  protected YTIdentifiable record;
  protected int index = -1;

  public OTraverseRecordSetProcess(
      final OTraverse iCommand, final Iterator<YTIdentifiable> iTarget, OTraversePath parentPath) {
    super(iCommand, iTarget);
    this.path = parentPath.appendRecordSet();
    command.getContext().push(this);
  }

  @SuppressWarnings("unchecked")
  public YTIdentifiable process() {
    while (target.hasNext()) {
      record = target.next();
      index++;

      final Record rec = record.getRecord();
      if (rec instanceof EntityImpl doc) {
        if (!doc.getIdentity().isPersistent() && doc.fields() == 1) {
          // EXTRACT THE FIELD CONTEXT
          Object fieldvalue = doc.field(doc.fieldNames()[0]);
          if (fieldvalue instanceof Collection<?>) {
            command
                .getContext()
                .push(
                    new OTraverseRecordSetProcess(
                        command, ((Collection<YTIdentifiable>) fieldvalue).iterator(), path));

          } else if (fieldvalue instanceof EntityImpl) {
            command.getContext().push(new OTraverseRecordProcess(command, rec, path));
          }
        } else {
          command.getContext().push(new OTraverseRecordProcess(command, rec, path));
        }

        return null;
      }
    }

    return pop();
  }

  @Override
  public OTraversePath getPath() {
    return path;
  }

  @Override
  public String toString() {
    return target != null ? target.toString() : "-";
  }
}