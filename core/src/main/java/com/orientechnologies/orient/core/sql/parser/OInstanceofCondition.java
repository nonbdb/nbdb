/* Generated By:JJTree: Do not edit this line. OInstanceofCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.exception.YTRecordNotFoundException;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.record.YTRecord;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OInstanceofCondition extends OBooleanExpression {

  protected OExpression left;
  protected OIdentifier right;
  protected String rightString;

  public OInstanceofCondition(int id) {
    super(id);
  }

  public OInstanceofCondition(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(YTIdentifiable currentRecord, OCommandContext ctx) {
    if (currentRecord == null) {
      return false;
    }
    YTRecord record;
    try {
      record = currentRecord.getRecord();
    } catch (YTRecordNotFoundException rnf) {
      return false;
    }

    if (!(record instanceof YTDocument doc)) {
      return false;
    }
    YTClass clazz = ODocumentInternal.getImmutableSchemaClass(doc);
    if (clazz == null) {
      return false;
    }
    if (right != null) {
      return clazz.isSubClassOf(right.getStringValue());
    } else if (rightString != null) {
      return clazz.isSubClassOf(decode(rightString));
    }
    return false;
  }

  @Override
  public boolean evaluate(YTResult currentRecord, OCommandContext ctx) {
    if (currentRecord == null) {
      return false;
    }
    if (!currentRecord.isElement()) {
      return false;
    }

    YTRecord record = currentRecord.getElement().get().getRecord();
    if (!(record instanceof YTDocument doc)) {
      return false;
    }
    YTClass clazz = ODocumentInternal.getImmutableSchemaClass(doc);
    if (clazz == null) {
      return false;
    }
    if (right != null) {
      return clazz.isSubClassOf(right.getStringValue());
    } else if (rightString != null) {
      return clazz.isSubClassOf(decode(rightString));
    }
    return false;
  }

  private String decode(String rightString) {
    if (rightString == null) {
      return null;
    }
    return OStringSerializerHelper.decode(rightString.substring(1, rightString.length() - 1));
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    builder.append(" instanceof ");
    if (right != null) {
      right.toString(params, builder);
    } else if (rightString != null) {
      builder.append(rightString);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    left.toGenericStatement(builder);
    builder.append(" instanceof ");
    if (right != null) {
      right.toGenericStatement(builder);
    } else if (rightString != null) {
      builder.append(rightString);
    }
  }

  @Override
  public boolean supportsBasicCalculation() {
    return left.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    if (!left.supportsBasicCalculation()) {
      return 1;
    }
    return 0;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    if (!left.supportsBasicCalculation()) {
      return Collections.singletonList(left);
    }
    return Collections.EMPTY_LIST;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    return left.needsAliases(aliases);
  }

  @Override
  public OInstanceofCondition copy() {
    OInstanceofCondition result = new OInstanceofCondition(-1);
    result.left = left.copy();
    result.right = right == null ? null : right.copy();
    result.rightString = rightString;
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    left.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return left != null && left.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OInstanceofCondition that = (OInstanceofCondition) o;

    if (!Objects.equals(left, that.left)) {
      return false;
    }
    if (!Objects.equals(right, that.right)) {
      return false;
    }
    return Objects.equals(rightString, that.rightString);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (right != null ? right.hashCode() : 0);
    result = 31 * result + (rightString != null ? rightString.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    return left == null ? null : left.getMatchPatternInvolvedAliases();
  }

  @Override
  public boolean isCacheable(YTDatabaseSessionInternal session) {
    return left.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=0b5eb529744f307228faa6b26f0592dc (do not edit this line) */
