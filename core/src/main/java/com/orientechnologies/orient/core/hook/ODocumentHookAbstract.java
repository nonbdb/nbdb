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
package com.orientechnologies.orient.core.hook;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.YTDatabaseSession.STATUS;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.record.YTRecord;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;

/**
 * Hook abstract class that calls separate methods for YTDocument records.
 *
 * @see ORecordHook
 */
public abstract class ODocumentHookAbstract implements ORecordHook {

  private String[] includeClasses;
  private String[] excludeClasses;

  protected YTDatabaseSession database;

  @Deprecated
  public ODocumentHookAbstract() {
    this.database = ODatabaseRecordThreadLocal.instance().get();
  }

  public ODocumentHookAbstract(YTDatabaseSession database) {
    this.database = database;
  }

  @Override
  public void onUnregister() {
  }

  /**
   * It's called just before to create the new document.
   *
   * @param iDocument The document to create
   * @return True if the document has been modified and a new marshalling is required, otherwise
   * false
   */
  public RESULT onRecordBeforeCreate(final YTDocument iDocument) {
    return RESULT.RECORD_NOT_CHANGED;
  }

  /**
   * It's called just after the document is created.
   *
   * @param iDocument The document is going to be created
   */
  public void onRecordAfterCreate(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document creation was failed.
   *
   * @param iDocument The document just created
   */
  public void onRecordCreateFailed(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document creation was replicated on another node.
   *
   * @param iDocument The document just created
   */
  public void onRecordCreateReplicated(final YTDocument iDocument) {
  }

  /**
   * It's called just before to read the document.
   *
   * @param iDocument The document to read
   * @return True if the document has been modified and a new marshalling is required, otherwise
   * false
   */
  public RESULT onRecordBeforeRead(final YTDocument iDocument) {
    return RESULT.RECORD_NOT_CHANGED;
  }

  /**
   * It's called just after the document is read.
   *
   * @param iDocument The document just read
   */
  public void onRecordAfterRead(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document read was failed.
   *
   * @param iDocument The document just created
   */
  public void onRecordReadFailed(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document read was replicated on another node.
   *
   * @param iDocument The document just created
   */
  public void onRecordReadReplicated(final YTDocument iDocument) {
  }

  /**
   * It's called just before to update the document.
   *
   * @param iDocument The document to update
   * @return True if the document has been modified and a new marshalling is required, otherwise
   * false
   */
  public RESULT onRecordBeforeUpdate(final YTDocument iDocument) {
    return RESULT.RECORD_NOT_CHANGED;
  }

  /**
   * It's called just after the document is updated.
   *
   * @param iDocument The document just updated
   */
  public void onRecordAfterUpdate(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document updated was failed.
   *
   * @param iDocument The document is going to be updated
   */
  public void onRecordUpdateFailed(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document updated was replicated.
   *
   * @param iDocument The document is going to be updated
   */
  public void onRecordUpdateReplicated(final YTDocument iDocument) {
  }

  /**
   * It's called just before to delete the document.
   *
   * @param iDocument The document to delete
   * @return True if the document has been modified and a new marshalling is required, otherwise
   * false
   */
  public RESULT onRecordBeforeDelete(final YTDocument iDocument) {
    return RESULT.RECORD_NOT_CHANGED;
  }

  /**
   * It's called just after the document is deleted.
   *
   * @param iDocument The document just deleted
   */
  public void onRecordAfterDelete(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document deletion was failed.
   *
   * @param iDocument The document is going to be deleted
   */
  public void onRecordDeleteFailed(final YTDocument iDocument) {
  }

  /**
   * It's called just after the document deletion was replicated.
   *
   * @param iDocument The document is going to be deleted
   */
  public void onRecordDeleteReplicated(final YTDocument iDocument) {
  }

  public void onRecordFinalizeUpdate(final YTDocument document) {
  }

  public void onRecordFinalizeCreation(final YTDocument document) {
  }

  public void onRecordFinalizeDeletion(final YTDocument document) {
  }

  public RESULT onTrigger(final TYPE iType, final YTRecord iRecord) {
    if (database.getStatus() != STATUS.OPEN) {
      return RESULT.RECORD_NOT_CHANGED;
    }

    if (!(iRecord instanceof YTDocument document)) {
      return RESULT.RECORD_NOT_CHANGED;
    }

    if (!filterBySchemaClass(document)) {
      return RESULT.RECORD_NOT_CHANGED;
    }

    switch (iType) {
      case BEFORE_CREATE:
        return onRecordBeforeCreate(document);

      case AFTER_CREATE:
        onRecordAfterCreate(document);
        break;

      case CREATE_FAILED:
        onRecordCreateFailed(document);
        break;

      case CREATE_REPLICATED:
        onRecordCreateReplicated(document);
        break;

      case BEFORE_READ:
        return onRecordBeforeRead(document);

      case AFTER_READ:
        onRecordAfterRead(document);
        break;

      case READ_FAILED:
        onRecordReadFailed(document);
        break;

      case READ_REPLICATED:
        onRecordReadReplicated(document);
        break;

      case BEFORE_UPDATE:
        return onRecordBeforeUpdate(document);

      case AFTER_UPDATE:
        onRecordAfterUpdate(document);
        break;

      case UPDATE_FAILED:
        onRecordUpdateFailed(document);
        break;

      case UPDATE_REPLICATED:
        onRecordUpdateReplicated(document);
        break;

      case BEFORE_DELETE:
        return onRecordBeforeDelete(document);

      case AFTER_DELETE:
        onRecordAfterDelete(document);
        break;

      case DELETE_FAILED:
        onRecordDeleteFailed(document);
        break;

      case DELETE_REPLICATED:
        onRecordDeleteReplicated(document);
        break;

      case FINALIZE_CREATION:
        onRecordFinalizeCreation(document);
        break;

      case FINALIZE_UPDATE:
        onRecordFinalizeUpdate(document);
        break;

      case FINALIZE_DELETION:
        onRecordFinalizeDeletion(document);
        break;

      default:
        throw new IllegalStateException("Hook method " + iType + " is not managed");
    }

    return RESULT.RECORD_NOT_CHANGED;
  }

  public String[] getIncludeClasses() {
    return includeClasses;
  }

  public ODocumentHookAbstract setIncludeClasses(final String... includeClasses) {
    if (excludeClasses != null) {
      throw new IllegalStateException("Cannot include classes if exclude classes has been set");
    }
    this.includeClasses = includeClasses;
    return this;
  }

  public String[] getExcludeClasses() {
    return excludeClasses;
  }

  public ODocumentHookAbstract setExcludeClasses(final String... excludeClasses) {
    if (includeClasses != null) {
      throw new IllegalStateException("Cannot exclude classes if include classes has been set");
    }
    this.excludeClasses = excludeClasses;
    return this;
  }

  protected boolean filterBySchemaClass(final YTDocument iDocument) {
    if (includeClasses == null && excludeClasses == null) {
      return true;
    }

    final YTClass clazz =
        ODocumentInternal.getImmutableSchemaClass((YTDatabaseSessionInternal) database, iDocument);
    if (clazz == null) {
      return false;
    }

    if (includeClasses != null) {
      // FILTER BY CLASSES
      for (String cls : includeClasses) {
        if (clazz.isSubClassOf(cls)) {
          return true;
        }
      }
      return false;
    }

    if (excludeClasses != null) {
      // FILTER BY CLASSES
      for (String cls : excludeClasses) {
        if (clazz.isSubClassOf(cls)) {
          return false;
        }
      }
    }

    return true;
  }
}
