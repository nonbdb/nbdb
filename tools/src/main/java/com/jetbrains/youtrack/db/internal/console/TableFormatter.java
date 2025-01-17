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
package com.jetbrains.youtrack.db.internal.console;

import com.jetbrains.youtrack.db.api.record.Blob;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.common.collection.MultiCollectionIterator;
import com.jetbrains.youtrack.db.internal.common.util.CallableFunction;
import com.jetbrains.youtrack.db.internal.common.util.Pair;
import com.jetbrains.youtrack.db.internal.common.util.Sizeable;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.id.ImmutableRecordId;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.util.DateHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TableFormatter {

  public enum ALIGNMENT {
    LEFT,
    CENTER,
    RIGHT
  }

  protected static final String MORE = "...";
  protected static final SimpleDateFormat DEF_DATEFORMAT =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  protected Pair<String, Boolean> columnSorting = null;
  protected final Map<String, ALIGNMENT> columnAlignment = new HashMap<String, ALIGNMENT>();
  protected final Map<String, Map<String, String>> columnMetadata =
      new HashMap<String, Map<String, String>>();
  protected final Set<String> columnHidden = new HashSet<String>();
  protected final Set<String> prefixedColumns =
      new LinkedHashSet<String>(Arrays.asList("#", "@RID", "@CLASS"));
  protected final OTableOutput out;
  protected int maxMultiValueEntries = 10;
  protected int minColumnSize = 4;
  protected int maxWidthSize = 150;
  protected String nullValue = "";
  private boolean leftBorder = true;
  private boolean rightBorder = true;
  private EntityImpl footer;

  public interface OTableOutput {

    void onMessage(String text);
  }

  public TableFormatter(final OTableOutput iConsole) {
    this.out = iConsole;
  }

  public void setColumnSorting(final String column, final boolean ascending) {
    columnSorting = new Pair<String, Boolean>(column, ascending);
  }

  public void setColumnHidden(final String column) {
    columnHidden.add(column);
  }

  public void writeRecords(final List<? extends Identifiable> resultSet, final int limit) {
    writeRecords(resultSet, limit, null);
  }

  public void writeRecords(
      final List<? extends Identifiable> resultSet,
      final int limit,
      final CallableFunction<Object, Identifiable> iAfterDump) {
    final Map<String, Integer> columns = parseColumns(resultSet, limit);

    if (columnSorting != null) {
      resultSet.sort(
          (Comparator<Object>)
              (o1, o2) -> {
                final EntityImpl doc1 = ((Identifiable) o1).getRecord();
                final EntityImpl doc2 = ((Identifiable) o2).getRecord();
                final Object value1 = doc1.field(columnSorting.getKey());
                final Object value2 = doc2.field(columnSorting.getKey());
                final boolean ascending = columnSorting.getValue();

                final int result;
                if (value2 == null) {
                  result = 1;
                } else if (value1 == null) {
                  result = 0;
                } else if (value1 instanceof Comparable) {
                  result = ((Comparable) value1).compareTo(value2);
                } else {
                  result = value1.toString().compareTo(value2.toString());
                }

                return ascending ? result : result * -1;
              });
    }

    int fetched = 0;
    for (Identifiable record : resultSet) {
      dumpRecordInTable(fetched++, record, columns);
      if (iAfterDump != null) {
        iAfterDump.call(record);
      }

      if (limit > -1 && fetched >= limit) {
        printHeaderLine(columns);
        out.onMessage(
            "\nLIMIT EXCEEDED: resultset contains more items not displayed (limit=" + limit + ")");
        return;
      }
    }

    if (fetched > 0) {
      printHeaderLine(columns);
    }

    if (footer != null) {
      dumpRecordInTable(-1, footer, columns);
      printHeaderLine(columns);
    }
  }

  public void setColumnAlignment(final String column, final ALIGNMENT alignment) {
    columnAlignment.put(column, alignment);
  }

  public void setColumnMetadata(
      final String columnName, final String metadataName, final String metadataValue) {
    Map<String, String> metadata = columnMetadata.get(columnName);
    if (metadata == null) {
      metadata = new LinkedHashMap<String, String>();
      columnMetadata.put(columnName, metadata);
    }
    metadata.put(metadataName, metadataValue);
  }

  public int getMaxWidthSize() {
    return maxWidthSize;
  }

  public TableFormatter setMaxWidthSize(final int maxWidthSize) {
    this.maxWidthSize = maxWidthSize;
    return this;
  }

  public int getMaxMultiValueEntries() {
    return maxMultiValueEntries;
  }

  public TableFormatter setMaxMultiValueEntries(final int maxMultiValueEntries) {
    this.maxMultiValueEntries = maxMultiValueEntries;
    return this;
  }

  public void dumpRecordInTable(
      final int iIndex, final Identifiable iRecord, final Map<String, Integer> iColumns) {
    if (iIndex == 0) {
      printHeader(iColumns);
    }

    // FORMAT THE LINE DYNAMICALLY
    List<String> vargs = new ArrayList<String>();
    try {
      if (iRecord instanceof EntityImpl) {
        ((EntityImpl) iRecord).setLazyLoad(false);
      }

      final StringBuilder format = new StringBuilder(maxWidthSize);

      if (leftBorder) {
        format.append('|');
      }

      int i = 0;
      for (Entry<String, Integer> col : iColumns.entrySet()) {
        final String columnName = col.getKey();
        final int columnWidth = col.getValue();

        if (i++ > 0) {
          format.append('|');
        }

        format.append("%-" + columnWidth + "s");

        Object value = getFieldValue(iIndex, iRecord, columnName);
        String valueAsString = null;

        if (value != null) {
          valueAsString = value.toString();
          if (valueAsString.length() > columnWidth) {
            // APPEND ...
            valueAsString = valueAsString.substring(0, columnWidth - 3) + MORE;
          }
        }

        valueAsString = formatCell(columnName, columnWidth, valueAsString);

        vargs.add(valueAsString);
      }

      if (rightBorder) {
        format.append('|');
      }

      out.onMessage(String.format("\n" + format, vargs.toArray()));

    } catch (Exception t) {
      out.onMessage(
          String.format(
              "%3d|%9s|%s\n",
              iIndex, iRecord.getIdentity(), "Error on loading record due to: " + t));
    }
  }

  protected String formatCell(
      final String columnName, final int columnWidth, String valueAsString) {
    if (valueAsString == null) {
      valueAsString = nullValue;
    }

    final ALIGNMENT alignment = columnAlignment.get(columnName);
    if (alignment != null) {
      switch (alignment) {
        case LEFT:
          break;
        case CENTER: {
          final int room = columnWidth - valueAsString.length();
          if (room > 1) {
            for (int k = 0; k < room / 2; ++k) {
              valueAsString = " " + valueAsString;
            }
          }
          break;
        }
        case RIGHT: {
          final int room = columnWidth - valueAsString.length();
          if (room > 0) {
            for (int k = 0; k < room; ++k) {
              valueAsString = " " + valueAsString;
            }
          }
          break;
        }
      }
    }
    return valueAsString;
  }

  private Object getFieldValue(
      final int iIndex, final Identifiable iRecord, final String iColumnName) {
    Object value = null;

    if (iColumnName.equals("#"))
    // RECORD NUMBER
    {
      value = iIndex > -1 ? iIndex : "";
    } else if (iColumnName.equals("@RID"))
    // RID
    {
      value = iRecord.getIdentity().toString();
    } else if (iRecord instanceof EntityImpl) {
      value = ((EntityImpl) iRecord).getProperty(iColumnName);
    } else if (iRecord instanceof Blob) {
      value = "<binary> (size=" + ((RecordAbstract) iRecord).toStream().length + " bytes)";
    } else if (iRecord instanceof Identifiable) {
      final DBRecord rec = iRecord.getRecord();
      if (rec instanceof EntityImpl) {
        value = ((EntityImpl) rec).getProperty(iColumnName);
      } else if (rec instanceof Blob) {
        value = "<binary> (size=" + ((RecordAbstract) rec).toStream().length + " bytes)";
      }
    }

    return getPrettyFieldValue(value, maxMultiValueEntries);
  }

  public void setNullValue(final String s) {
    nullValue = s;
  }

  public void setLeftBorder(final boolean value) {
    leftBorder = value;
  }

  public void setRightBorder(final boolean value) {
    rightBorder = value;
  }

  public static String getPrettyFieldMultiValue(
      final Iterator<?> iterator, final int maxMultiValueEntries) {
    final StringBuilder value = new StringBuilder("[");
    for (int i = 0; iterator.hasNext(); i++) {
      if (i >= maxMultiValueEntries) {
        if (iterator instanceof Sizeable) {
          value.append("(size=");
          value.append(((Sizeable) iterator).size());
          value.append(")");
        } else {
          value.append("(more)");
        }

        break;
      }

      if (i > 0) {
        value.append(',');
      }

      value.append(getPrettyFieldValue(iterator.next(), maxMultiValueEntries));
    }

    value.append("]");

    return value.toString();
  }

  public void setFooter(final EntityImpl footer) {
    this.footer = footer;
  }

  public static Object getPrettyFieldValue(Object value, final int multiValueMaxEntries) {
    if (value instanceof MultiCollectionIterator<?>) {
      value =
          getPrettyFieldMultiValue(
              ((MultiCollectionIterator<?>) value).iterator(), multiValueMaxEntries);
    } else if (value instanceof RidBag) {
      value = getPrettyFieldMultiValue(((RidBag) value).iterator(), multiValueMaxEntries);
    } else if (value instanceof Iterator) {
      value = getPrettyFieldMultiValue((Iterator<?>) value, multiValueMaxEntries);
    } else if (value instanceof Collection<?>) {
      value = getPrettyFieldMultiValue(((Collection<?>) value).iterator(), multiValueMaxEntries);
    } else if (value instanceof DBRecord) {
      if (((DBRecord) value).getIdentity().equals(ImmutableRecordId.EMPTY_RECORD_ID)) {
        value = value.toString();
      } else {
        value = ((DBRecord) value).getIdentity().toString();
      }
    } else if (value instanceof Date) {
      final DatabaseSessionInternal db = DatabaseRecordThreadLocal.instance().getIfDefined();
      if (db != null) {
        value = DateHelper.getDateTimeFormatInstance(db).format((Date) value);
      } else {
        value = DEF_DATEFORMAT.format((Date) value);
      }
    } else if (value instanceof byte[]) {
      value = "byte[" + ((byte[]) value).length + "]";
    }

    return value;
  }

  private void printHeader(final Map<String, Integer> iColumns) {
    final StringBuilder columnRow = new StringBuilder("\n");
    final Map<String, StringBuilder> metadataRows = new HashMap<String, StringBuilder>();

    // INIT METADATA
    final LinkedHashSet<String> allMetadataNames = new LinkedHashSet<String>();
    final Set<String> metadataColumns = new HashSet<String>();

    for (Entry<String, Map<String, String>> entry : columnMetadata.entrySet()) {
      metadataColumns.add(entry.getKey());

      for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
        allMetadataNames.add(entry2.getKey());

        StringBuilder metadataRow = metadataRows.get(entry2.getKey());
        if (metadataRow == null) {
          metadataRow = new StringBuilder("\n");
          metadataRows.put(entry2.getKey(), metadataRow);
        }
      }
    }

    printHeaderLine(iColumns);
    int i = 0;

    if (leftBorder) {
      columnRow.append('|');
      if (!metadataRows.isEmpty()) {
        for (StringBuilder buffer : metadataRows.values()) {
          buffer.append('|');
        }
      }
    }

    for (Entry<String, Integer> column : iColumns.entrySet()) {
      String colName = column.getKey();

      if (columnHidden.contains(colName)) {
        continue;
      }

      if (i > 0) {
        columnRow.append('|');
        if (!metadataRows.isEmpty()) {
          for (StringBuilder buffer : metadataRows.values()) {
            buffer.append('|');
          }
        }
      }

      if (colName.length() > column.getValue()) {
        colName = colName.substring(0, column.getValue());
      }
      columnRow.append(
          String.format(
              "%-" + column.getValue() + "s", formatCell(colName, column.getValue(), colName)));

      if (!metadataRows.isEmpty()) {
        // METADATA VALUE
        for (String metadataName : allMetadataNames) {
          final StringBuilder buffer = metadataRows.get(metadataName);
          final Map<String, String> metadataColumn = columnMetadata.get(column.getKey());
          String metadataValue = metadataColumn != null ? metadataColumn.get(metadataName) : null;
          if (metadataValue == null) {
            metadataValue = "";
          }

          if (metadataValue.length() > column.getValue()) {
            metadataValue = metadataValue.substring(0, column.getValue());
          }
          buffer.append(
              String.format(
                  "%-" + column.getValue() + "s",
                  formatCell(colName, column.getValue(), metadataValue)));
        }
      }

      ++i;
    }

    if (rightBorder) {
      columnRow.append('|');
      if (!metadataRows.isEmpty()) {
        for (StringBuilder buffer : metadataRows.values()) {
          buffer.append('|');
        }
      }
    }

    if (!metadataRows.isEmpty()) {
      // PRINT METADATA IF ANY
      for (StringBuilder buffer : metadataRows.values()) {
        out.onMessage(buffer.toString());
      }
      printHeaderLine(iColumns);
    }

    out.onMessage(columnRow.toString());

    printHeaderLine(iColumns);
  }

  private void printHeaderLine(final Map<String, Integer> iColumns) {
    final StringBuilder buffer = new StringBuilder("\n");

    if (iColumns.size() > 0) {
      if (leftBorder) {
        buffer.append('+');
      }

      int i = 0;
      for (Entry<String, Integer> column : iColumns.entrySet()) {
        final String colName = column.getKey();
        if (columnHidden.contains(colName)) {
          continue;
        }

        if (i++ > 0) {
          buffer.append("+");
        }

        buffer.append("-".repeat(Math.max(0, column.getValue())));
      }

      if (rightBorder) {
        buffer.append('+');
      }
    }

    out.onMessage(buffer.toString());
  }

  /**
   * Fill the column map computing the maximum size for a field.
   *
   * @param resultSet
   * @param limit
   * @return
   */
  private Map<String, Integer> parseColumns(
      final Collection<? extends Identifiable> resultSet, final int limit) {
    final Map<String, Integer> columns = new LinkedHashMap<String, Integer>();

    for (String c : prefixedColumns) {
      columns.put(c, minColumnSize);
    }

    boolean tempRids = false;
    boolean hasClass = false;

    int fetched = 0;
    for (Identifiable id : resultSet) {
      DBRecord rec = id.getRecord();

      for (String c : prefixedColumns) {
        columns.put(c, getColumnSize(fetched, rec, c, columns.get(c)));
      }

      if (rec instanceof EntityImpl entity) {
        entity.setLazyLoad(false);
        // PARSE ALL THE DOCUMENT'S FIELDS
        for (String fieldName : entity.getPropertyNames()) {
          columns.put(fieldName, getColumnSize(fetched, entity, fieldName, columns.get(fieldName)));
        }

        if (!hasClass && entity.getClassName() != null) {
          hasClass = true;
        }

      } else if (rec instanceof Blob) {
        // UNIQUE BINARY FIELD
        columns.put("value", maxWidthSize - 15);
      }

      if (!tempRids && !rec.getIdentity().isPersistent()) {
        tempRids = true;
      }

      if (limit > -1 && fetched++ >= limit) {
        break;
      }
    }

    if (tempRids) {
      columns.remove("@RID");
    }

    if (!hasClass) {
      columns.remove("@CLASS");
    }

    if (footer != null) {
      footer.setLazyLoad(false);
      // PARSE ALL THE DOCUMENT'S FIELDS
      for (String fieldName : footer.fieldNames()) {
        columns.put(fieldName, getColumnSize(fetched, footer, fieldName, columns.get(fieldName)));
      }
    }

    // COMPUTE MAXIMUM WIDTH
    int width = 0;
    for (Entry<String, Integer> col : columns.entrySet()) {
      width += col.getValue();
    }

    if (width > maxWidthSize) {
      // SCALE COLUMNS AUTOMATICALLY
      final List<Map.Entry<String, Integer>> orderedColumns =
          new ArrayList<Map.Entry<String, Integer>>();
      orderedColumns.addAll(columns.entrySet());
      Collections.sort(
          orderedColumns,
          new Comparator<Map.Entry<String, Integer>>() {

            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
              return o1.getValue().compareTo(o2.getValue());
            }
          });

      // START CUTTING THE BIGGEST ONES
      Collections.reverse(orderedColumns);
      while (width > maxWidthSize) {
        int oldWidth = width;

        for (Map.Entry<String, Integer> entry : orderedColumns) {
          final int redux = entry.getValue() * 10 / 100;

          if (entry.getValue() - redux < minColumnSize)
          // RESTART FROM THE LARGEST COLUMN
          {
            break;
          }

          entry.setValue(entry.getValue() - redux);

          width -= redux;
          if (width <= maxWidthSize) {
            break;
          }
        }

        if (width == oldWidth)
        // REACHED THE MINIMUM
        {
          break;
        }
      }

      // POPULATE THE COLUMNS WITH THE REDUXED VALUES
      columns.clear();
      for (String c : prefixedColumns) {
        columns.put(c, minColumnSize);
      }
      Collections.reverse(orderedColumns);
      for (Entry<String, Integer> col : orderedColumns)
      // if (!col.getKey().equals("#") && !col.getKey().equals("@RID"))
      {
        columns.put(col.getKey(), col.getValue());
      }
    }

    if (tempRids) {
      columns.remove("@RID");
    }
    if (!hasClass) {
      columns.remove("@CLASS");
    }

    for (String c : columnHidden) {
      columns.remove(c);
    }

    return columns;
  }

  private Integer getColumnSize(
      final Integer iIndex, final DBRecord iRecord, final String fieldName,
      final Integer origSize) {
    Integer newColumnSize;
    if (origSize == null)
    // START FROM THE FIELD NAME SIZE
    {
      newColumnSize = fieldName.length();
    } else {
      newColumnSize = Math.max(origSize, fieldName.length());
    }

    final Map<String, String> metadata = columnMetadata.get(fieldName);
    if (metadata != null) {
      // UPDATE WIDTH WITH METADATA VALUES
      for (String v : metadata.values()) {
        if (v != null) {
          if (v.length() > newColumnSize) {
            newColumnSize = v.length();
          }
        }
      }
    }

    final Object fieldValue = getFieldValue(iIndex, iRecord, fieldName);

    if (fieldValue != null) {
      final String fieldValueAsString = fieldValue.toString();
      if (fieldValueAsString.length() > newColumnSize) {
        newColumnSize = fieldValueAsString.length();
      }
    }

    if (newColumnSize < minColumnSize)
    // SET THE MINIMUM SIZE
    {
      newColumnSize = minColumnSize;
    }

    return newColumnSize;
  }
}
