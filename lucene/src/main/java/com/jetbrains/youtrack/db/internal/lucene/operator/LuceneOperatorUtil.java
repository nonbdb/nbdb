/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.youtrack.db.internal.lucene.operator;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.sql.IndexSearchResult;
import com.jetbrains.youtrack.db.internal.core.sql.SQLHelper;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterCondition;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterItemField;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterItemVariable;
import com.jetbrains.youtrack.db.internal.core.sql.operator.QueryOperatorBetween;
import com.jetbrains.youtrack.db.internal.core.sql.operator.QueryOperatorIn;
import java.util.Collection;
import java.util.List;

public class LuceneOperatorUtil {

  public static IndexSearchResult buildOIndexSearchResult(
      SchemaClass iSchemaClass,
      SQLFilterCondition iCondition,
      List<IndexSearchResult> iIndexSearchResults,
      CommandContext context) {

    if (iCondition.getLeft() instanceof Collection left) {
      IndexSearchResult lastResult = null;

      int i = 0;
      Object lastValue = null;
      for (Object obj : left) {
        if (obj instanceof SQLFilterItemField item) {

          Object value = null;
          if (iCondition.getRight() instanceof Collection) {
            List<Object> right = (List<Object>) iCondition.getRight();
            value = right.get(i);
          } else {
            value = iCondition.getRight();
          }
          if (lastResult == null) {
            lastResult =
                new IndexSearchResult(iCondition.getOperator(), item.getFieldChain(), value);
          } else {
            lastResult =
                lastResult.merge(
                    new IndexSearchResult(iCondition.getOperator(), item.getFieldChain(), value));
          }

        } else if (obj instanceof SQLFilterItemVariable item) {
          Object value = null;
          if (iCondition.getRight() instanceof Collection) {
            List<Object> right = (List<Object>) iCondition.getRight();
            value = right.get(i);
          } else {
            value = iCondition.getRight();
          }
          context.setVariable(item.toString(), value);
        }
        i++;
      }
      if (lastResult != null && LuceneOperatorUtil.checkIndexExistence(context.getDatabase(),
          iSchemaClass,
          lastResult)) {
        iIndexSearchResults.add(lastResult);
      }
      return lastResult;
    } else {
      IndexSearchResult result =
          LuceneOperatorUtil.createIndexedProperty(iCondition, iCondition.getLeft());
      if (result == null) {
        result = LuceneOperatorUtil.createIndexedProperty(iCondition, iCondition.getRight());
      }

      if (result == null) {
        return null;
      }

      if (LuceneOperatorUtil.checkIndexExistence(context.getDatabase(), iSchemaClass, result)) {
        iIndexSearchResults.add(result);
      }

      return result;
    }
  }

  public static boolean checkIndexExistence(
      DatabaseSessionInternal session, final SchemaClass iSchemaClass,
      final IndexSearchResult result) {
    if (!iSchemaClass.areIndexed(session, result.fields())) {
      return false;
    }

    if (result.lastField.isLong()) {
      final int fieldCount = result.lastField.getItemCount();
      SchemaClass cls = iSchemaClass.getProperty(result.lastField.getItemName(0)).getLinkedClass();

      for (int i = 1; i < fieldCount; i++) {
        if (cls == null || !cls.areIndexed(session, result.lastField.getItemName(i))) {
          return false;
        }

        cls = cls.getProperty(result.lastField.getItemName(i)).getLinkedClass();
      }
    }
    return true;
  }

  public static IndexSearchResult createIndexedProperty(
      final SQLFilterCondition iCondition, final Object iItem) {
    if (iItem == null || !(iItem instanceof SQLFilterItemField item)) {
      return null;
    }

    if (iCondition.getLeft() instanceof SQLFilterItemField
        && iCondition.getRight() instanceof SQLFilterItemField) {
      return null;
    }

    if (item.hasChainOperators() && !item.isFieldChain()) {
      return null;
    }

    final Object origValue =
        iCondition.getLeft() == iItem ? iCondition.getRight() : iCondition.getLeft();

    if (iCondition.getOperator() instanceof QueryOperatorBetween
        || iCondition.getOperator() instanceof QueryOperatorIn) {
      return new IndexSearchResult(iCondition.getOperator(), item.getFieldChain(), origValue);
    }

    final Object value = SQLHelper.getValue(origValue);

    if (value == null) {
      return null;
    }

    return new IndexSearchResult(iCondition.getOperator(), item.getFieldChain(), value);
  }
}
