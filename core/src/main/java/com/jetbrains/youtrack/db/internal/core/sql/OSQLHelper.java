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
package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.internal.common.collection.OMultiValue;
import com.jetbrains.youtrack.db.internal.common.io.OIOUtils;
import com.jetbrains.youtrack.db.internal.common.parser.BaseParser;
import com.jetbrains.youtrack.db.internal.common.util.OPair;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTImmutableClass;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTProperty;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.ODocumentHelper;
import com.jetbrains.youtrack.db.internal.core.record.impl.ODocumentInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.OStringSerializerHelper;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string.ORecordSerializerCSVAbstract;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItem;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemAbstract;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemField;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemParameter;
import com.jetbrains.youtrack.db.internal.core.sql.filter.OSQLFilterItemVariable;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLPredicate;
import com.jetbrains.youtrack.db.internal.core.sql.functions.OSQLFunctionRuntime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * SQL Helper class
 */
public class OSQLHelper {

  public static final String NAME = "sql";

  public static final String VALUE_NOT_PARSED = "_NOT_PARSED_";
  public static final String NOT_NULL = "_NOT_NULL_";
  public static final String DEFINED = "_DEFINED_";

  private static final ClassLoader orientClassLoader =
      OSQLFilterItemAbstract.class.getClassLoader();

  public static Object parseDefaultValue(YTDatabaseSessionInternal session, EntityImpl iRecord,
      final String iWord) {
    final Object v = OSQLHelper.parseValue(iWord, null);

    if (v != VALUE_NOT_PARSED) {
      return v;
    }

    // TRY TO PARSE AS FUNCTION
    final OSQLFunctionRuntime func = OSQLHelper.getFunction(session, null, iWord);
    if (func != null) {
      var context = new BasicCommandContext();
      context.setDatabase(session);

      return func.execute(iRecord, iRecord, null, context);
    }

    // PARSE AS FIELD
    return iWord;
  }

  /**
   * Convert fields from text to real value. Supports: String, RID, Boolean, Float, Integer and
   * NULL.
   *
   * @param iValue Value to convert.
   * @return The value converted if recognized, otherwise VALUE_NOT_PARSED
   */
  public static Object parseValue(String iValue, final CommandContext iContext) {
    return parseValue(iValue, iContext, false);
  }

  public static Object parseValue(
      String iValue, final CommandContext iContext, boolean resolveContextVariables) {

    if (iValue == null) {
      return null;
    }

    iValue = iValue.trim();

    Object fieldValue = VALUE_NOT_PARSED;

    if (iValue.length() == 0) {
      return iValue;
    }
    if (iValue.startsWith("'") && iValue.endsWith("'")
        || iValue.startsWith("\"") && iValue.endsWith("\""))
    // STRING
    {
      fieldValue = OIOUtils.getStringContent(iValue);
    } else if (iValue.charAt(0) == OStringSerializerHelper.LIST_BEGIN
        && iValue.charAt(iValue.length() - 1) == OStringSerializerHelper.LIST_END) {
      // COLLECTION/ARRAY
      final List<String> items =
          OStringSerializerHelper.smartSplit(
              iValue.substring(1, iValue.length() - 1), OStringSerializerHelper.RECORD_SEPARATOR);

      final List<Object> coll = new ArrayList<Object>();
      for (String item : items) {
        coll.add(parseValue(item, iContext, resolveContextVariables));
      }
      fieldValue = coll;

    } else if (iValue.charAt(0) == OStringSerializerHelper.MAP_BEGIN
        && iValue.charAt(iValue.length() - 1) == OStringSerializerHelper.MAP_END) {
      // MAP
      final List<String> items =
          OStringSerializerHelper.smartSplit(
              iValue.substring(1, iValue.length() - 1), OStringSerializerHelper.RECORD_SEPARATOR);

      final Map<Object, Object> map = new HashMap<Object, Object>();
      for (String item : items) {
        final List<String> parts =
            OStringSerializerHelper.smartSplit(item, OStringSerializerHelper.ENTRY_SEPARATOR);

        if (parts == null || parts.size() != 2) {
          throw new YTCommandSQLParsingException(
              "Map found but entries are not defined as <key>:<value>");
        }

        Object key = OStringSerializerHelper.decode(parseValue(parts.get(0), iContext).toString());
        Object value = parseValue(parts.get(1), iContext);
        if (VALUE_NOT_PARSED == value) {
          value = new SQLPredicate(iContext, parts.get(1)).evaluate(iContext);
        }
        if (value instanceof String) {
          value = OStringSerializerHelper.decode(value.toString());
        }
        map.put(key, value);
      }

      if (map.containsKey(ODocumentHelper.ATTRIBUTE_TYPE))
      // IT'S A DOCUMENT
      // TODO: IMPROVE THIS CASE AVOIDING DOUBLE PARSING
      {
        var document = new EntityImpl();
        document.fromJSON(iValue);
        fieldValue = document;
      } else {
        fieldValue = map;
      }

    } else if (iValue.charAt(0) == OStringSerializerHelper.EMBEDDED_BEGIN
        && iValue.charAt(iValue.length() - 1) == OStringSerializerHelper.EMBEDDED_END) {
      // SUB-COMMAND
      fieldValue = new CommandSQL(iValue.substring(1, iValue.length() - 1));
      ((CommandSQL) fieldValue).getContext().setParent(iContext);

    } else if (YTRecordId.isA(iValue))
    // RID
    {
      fieldValue = new YTRecordId(iValue.trim());
    } else {

      if (iValue.equalsIgnoreCase("null"))
      // NULL
      {
        fieldValue = null;
      } else if (iValue.equalsIgnoreCase("not null"))
      // NULL
      {
        fieldValue = NOT_NULL;
      } else if (iValue.equalsIgnoreCase("defined"))
      // NULL
      {
        fieldValue = DEFINED;
      } else if (iValue.equalsIgnoreCase("true"))
      // BOOLEAN, TRUE
      {
        fieldValue = Boolean.TRUE;
      } else if (iValue.equalsIgnoreCase("false"))
      // BOOLEAN, FALSE
      {
        fieldValue = Boolean.FALSE;
      } else if (iValue.startsWith("date(")) {
        final OSQLFunctionRuntime func = OSQLHelper.getFunction(iContext.getDatabase(), null,
            iValue);
        if (func != null) {
          fieldValue = func.execute(null, null, null, iContext);
        }
      } else if (resolveContextVariables && iValue.startsWith("$") && iContext != null) {
        fieldValue = iContext.getVariable(iValue);
      } else {
        final Object v = parseStringNumber(iValue);
        if (v != null) {
          fieldValue = v;
        }
      }
    }

    return fieldValue;
  }

  public static Object parseStringNumber(final String iValue) {
    final YTType t = ORecordSerializerCSVAbstract.getType(iValue);

    if (t == YTType.INTEGER) {
      return Integer.parseInt(iValue);
    } else if (t == YTType.LONG) {
      return Long.parseLong(iValue);
    } else if (t == YTType.FLOAT) {
      return Float.parseFloat(iValue);
    } else if (t == YTType.SHORT) {
      return Short.parseShort(iValue);
    } else if (t == YTType.BYTE) {
      return Byte.parseByte(iValue);
    } else if (t == YTType.DOUBLE) {
      return Double.parseDouble(iValue);
    } else if (t == YTType.DECIMAL) {
      return new BigDecimal(iValue);
    } else if (t == YTType.DATE || t == YTType.DATETIME) {
      return new Date(Long.parseLong(iValue));
    }

    return null;
  }

  public static Object parseValue(
      final SQLPredicate iSQLFilter,
      final BaseParser iCommand,
      final String iWord,
      @Nonnull final CommandContext iContext) {
    if (iWord.charAt(0) == OStringSerializerHelper.PARAMETER_POSITIONAL
        || iWord.charAt(0) == OStringSerializerHelper.PARAMETER_NAMED) {
      if (iSQLFilter != null) {
        return iSQLFilter.addParameter(iWord);
      } else {
        return new OSQLFilterItemParameter(iWord);
      }
    } else {
      return parseValue(iCommand, iWord, iContext);
    }
  }

  public static Object parseValue(
      final BaseParser iCommand, final String iWord, final CommandContext iContext) {
    return parseValue(iCommand, iWord, iContext, false);
  }

  public static Object parseValue(
      final BaseParser iCommand,
      final String iWord,
      final CommandContext iContext,
      boolean resolveContextVariables) {
    if (iWord.equals("*")) {
      return "*";
    }

    // TRY TO PARSE AS RAW VALUE
    final Object v = parseValue(iWord, iContext, resolveContextVariables);
    if (v != VALUE_NOT_PARSED) {
      return v;
    }

    if (!iWord.equalsIgnoreCase("any()") && !iWord.equalsIgnoreCase("all()")) {
      // TRY TO PARSE AS FUNCTION
      final Object func = OSQLHelper.getFunction(iContext.getDatabase(), iCommand, iWord);
      if (func != null) {
        return func;
      }
    }

    if (iWord.startsWith("$"))
    // CONTEXT VARIABLE
    {
      return new OSQLFilterItemVariable(iContext.getDatabase(), iCommand, iWord);
    }

    // PARSE AS FIELD
    return new OSQLFilterItemField(iContext.getDatabase(), iCommand, iWord, null);
  }

  public static OSQLFunctionRuntime getFunction(YTDatabaseSessionInternal session,
      final BaseParser iCommand, final String iWord) {
    final int separator = iWord.indexOf('.');
    final int beginParenthesis = iWord.indexOf(OStringSerializerHelper.EMBEDDED_BEGIN);
    if (beginParenthesis > -1 && (separator == -1 || separator > beginParenthesis)) {
      final int endParenthesis =
          iWord.indexOf(OStringSerializerHelper.EMBEDDED_END, beginParenthesis);

      final char firstChar = iWord.charAt(0);
      if (endParenthesis > -1 && (firstChar == '_' || Character.isLetter(firstChar)))
      // FUNCTION: CREATE A RUN-TIME CONTAINER FOR IT TO SAVE THE PARAMETERS
      {
        return new OSQLFunctionRuntime(session, iCommand, iWord);
      }
    }

    return null;
  }

  public static Object getValue(final Object iObject) {
    if (iObject == null) {
      return null;
    }

    if (iObject instanceof OSQLFilterItem) {
      return ((OSQLFilterItem) iObject).getValue(null, null, null);
    }

    return iObject;
  }

  public static Object getValue(
      final Object iObject, final Record iRecord, final CommandContext iContext) {
    if (iObject == null) {
      return null;
    }

    if (iObject instanceof OSQLFilterItem) {
      return ((OSQLFilterItem) iObject).getValue(iRecord, null, iContext);
    } else if (iObject instanceof String) {
      final String s = ((String) iObject).trim();
      if (iRecord != null & !s.isEmpty()
          && !OIOUtils.isStringContent(iObject)
          && !Character.isDigit(s.charAt(0)))
      // INTERPRETS IT
      {
        return ODocumentHelper.getFieldValue(iContext.getDatabase(), iRecord, s, iContext);
      }
    }

    return iObject;
  }

  public static Object resolveFieldValue(
      YTDatabaseSession session, final EntityImpl iDocument,
      final String iFieldName,
      final Object iFieldValue,
      final OCommandParameters iArguments,
      final CommandContext iContext) {
    if (iFieldValue instanceof OSQLFilterItemField f) {
      if (f.getRoot(session).equals("?"))
      // POSITIONAL PARAMETER
      {
        return iArguments.getNext();
      } else if (f.getRoot(session).startsWith(":"))
      // NAMED PARAMETER
      {
        return iArguments.getByName(f.getRoot(session).substring(1));
      }
    }

    if (iFieldValue instanceof EntityImpl && !((EntityImpl) iFieldValue).getIdentity()
        .isValid())
    // EMBEDDED DOCUMENT
    {
      ODocumentInternal.addOwner((EntityImpl) iFieldValue, iDocument);
    }

    // can't use existing getValue with iContext
    if (iFieldValue == null) {
      return null;
    }
    if (iFieldValue instanceof OSQLFilterItem) {
      return ((OSQLFilterItem) iFieldValue).getValue(iDocument, null, iContext);
    }

    return iFieldValue;
  }

  public static EntityImpl bindParameters(
      final EntityImpl iDocument,
      final Map<String, Object> iFields,
      final OCommandParameters iArguments,
      final CommandContext iContext) {
    if (iFields == null) {
      return null;
    }

    final List<OPair<String, Object>> fields = new ArrayList<OPair<String, Object>>(iFields.size());

    for (Map.Entry<String, Object> entry : iFields.entrySet()) {
      fields.add(new OPair<String, Object>(entry.getKey(), entry.getValue()));
    }

    return bindParameters(iDocument, fields, iArguments, iContext);
  }

  public static EntityImpl bindParameters(
      final EntityImpl iDocument,
      final List<OPair<String, Object>> iFields,
      final OCommandParameters iArguments,
      final CommandContext iContext) {
    if (iFields == null) {
      return null;
    }

    // BIND VALUES
    for (OPair<String, Object> field : iFields) {
      final String fieldName = field.getKey();
      Object fieldValue = field.getValue();

      if (fieldValue != null) {
        if (fieldValue instanceof CommandSQL cmd) {
          cmd.getContext().setParent(iContext);
          fieldValue = ODatabaseRecordThreadLocal.instance().get().command(cmd)
              .execute(iContext.getDatabase());

          // CHECK FOR CONVERSIONS
          YTImmutableClass immutableClass = ODocumentInternal.getImmutableSchemaClass(iDocument);
          if (immutableClass != null) {
            final YTProperty prop = immutableClass.getProperty(fieldName);
            if (prop != null) {
              if (prop.getType() == YTType.LINK) {
                if (OMultiValue.isMultiValue(fieldValue)) {
                  final int size = OMultiValue.getSize(fieldValue);
                  if (size == 1)
                  // GET THE FIRST ITEM AS UNIQUE LINK
                  {
                    fieldValue = OMultiValue.getFirstValue(fieldValue);
                  } else if (size == 0)
                  // NO ITEMS, SET IT AS NULL
                  {
                    fieldValue = null;
                  }
                }
              }
            } else if (immutableClass.isEdgeType()
                && ("out".equals(fieldName) || "in".equals(fieldName))
                && (fieldValue instanceof List lst)) {
              if (lst.size() == 1) {
                fieldValue = lst.get(0);
              }
            }
          }

          if (OMultiValue.isMultiValue(fieldValue)) {
            final List<Object> tempColl = new ArrayList<Object>(OMultiValue.getSize(fieldValue));

            String singleFieldName = null;
            for (Object o : OMultiValue.getMultiValueIterable(fieldValue)) {
              if (o instanceof YTIdentifiable && !((YTIdentifiable) o).getIdentity()
                  .isPersistent()) {
                // TEMPORARY / EMBEDDED
                final Record rec = ((YTIdentifiable) o).getRecord();
                if (rec != null && rec instanceof EntityImpl doc) {
                  // CHECK FOR ONE FIELD ONLY
                  if (doc.fields() == 1) {
                    singleFieldName = doc.fieldNames()[0];
                    tempColl.add(doc.field(singleFieldName));
                  } else {
                    // TRANSFORM IT IN EMBEDDED
                    doc.getIdentity().reset();
                    ODocumentInternal.addOwner(doc, iDocument);
                    ODocumentInternal.addOwner(doc, iDocument);
                    tempColl.add(doc);
                  }
                }
              } else {
                tempColl.add(o);
              }
            }

            fieldValue = tempColl;
          }
        }
      }

      iDocument.field(
          fieldName,
          resolveFieldValue(iContext.getDatabase(), iDocument, fieldName, fieldValue, iArguments,
              iContext));
    }
    return iDocument;
  }
}
