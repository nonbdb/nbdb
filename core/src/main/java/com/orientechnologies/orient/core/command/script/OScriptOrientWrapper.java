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
package com.orientechnologies.orient.core.command.script;

import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OConfigurationException;

/**
 * Orient wrapper class to use from scripts.
 */
@Deprecated
public class OScriptOrientWrapper {

  protected final YTDatabaseSession db;

  public OScriptOrientWrapper() {
    this.db = null;
  }

  public OScriptOrientWrapper(final YTDatabaseSession db) {
    this.db = db;
  }

  public OScriptDocumentDatabaseWrapper getDatabase() {
    if (db == null) {
      throw new OConfigurationException("No database instance found in context");
    }

    if (db instanceof YTDatabaseSessionInternal) {
      return new OScriptDocumentDatabaseWrapper((YTDatabaseSessionInternal) db);
    }

    throw new OConfigurationException(
        "No valid database instance found in context: " + db + ", class: " + db.getClass());
  }
}
