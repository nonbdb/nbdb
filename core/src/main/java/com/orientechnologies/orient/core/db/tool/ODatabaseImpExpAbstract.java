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
package com.orientechnologies.orient.core.db.tool;

import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import java.util.List;

/**
 * Abstract class for import/export of database and data in general.
 */
public abstract class ODatabaseImpExpAbstract extends ODatabaseTool {

  protected static final String DEFAULT_EXT = ".json";
  protected String fileName;
  protected boolean useLineFeedForRecords = false;

  protected OCommandOutputListener listener;

  public ODatabaseImpExpAbstract(
      final YTDatabaseSessionInternal iDatabase,
      final String iFileName,
      final OCommandOutputListener iListener) {
    database = iDatabase;
    fileName = iFileName;

    // Fix bug where you can't backup files with spaces. Now you can wrap with quotes and the
    // filesystem won't create
    // directories with quotes in their name.
    if (fileName != null) {
      if ((fileName.startsWith("\"") && fileName.endsWith("\""))
          || (fileName.startsWith("'") && fileName.endsWith("'"))) {
        fileName = fileName.substring(1, fileName.length() - 1);
        iListener.onMessage("Detected quotes surrounding filename; new backup file: " + fileName);
      }
    }

    if (fileName != null && fileName.indexOf('.') == -1) {
      fileName += DEFAULT_EXT;
    }

    listener = iListener;
  }

  public OCommandOutputListener getListener() {
    return listener;
  }

  public void setListener(final OCommandOutputListener listener) {
    this.listener = listener;
  }

  public YTDatabaseSessionInternal getDatabase() {
    return database;
  }

  public String getFileName() {
    return fileName;
  }

  public boolean isUseLineFeedForRecords() {
    return useLineFeedForRecords;
  }

  public void setUseLineFeedForRecords(final boolean useLineFeedForRecords) {
    this.useLineFeedForRecords = useLineFeedForRecords;
  }

  protected void parseSetting(final String option, final List<String> items) {
    if (option.equalsIgnoreCase("-useLineFeedForRecords")) {
      useLineFeedForRecords = Boolean.parseBoolean(items.get(0));
    }
  }
}
