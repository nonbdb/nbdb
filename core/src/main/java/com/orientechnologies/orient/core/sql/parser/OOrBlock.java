/* Generated By:JJTree: Do not edit this line. OOrBlock.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import com.orientechnologies.orient.core.sql.executor.metadata.OIndexCandidate;
import com.orientechnologies.orient.core.sql.executor.metadata.OIndexFinder;
import com.orientechnologies.orient.core.sql.executor.metadata.ORequiredIndexCanditate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OOrBlock extends OBooleanExpression {

  List<OBooleanExpression> subBlocks = new ArrayList<OBooleanExpression>();

  public OOrBlock(int id) {
    super(id);
  }

  public OOrBlock(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(YTIdentifiable currentRecord, OCommandContext ctx) {
    if (subBlocks == null) {
      return true;
    }

    for (OBooleanExpression block : subBlocks) {
      if (block.evaluate(currentRecord, ctx)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean evaluate(YTResult currentRecord, OCommandContext ctx) {
    if (subBlocks == null) {
      return true;
    }

    for (OBooleanExpression block : subBlocks) {
      if (block.evaluate(currentRecord, ctx)) {
        return true;
      }
    }
    return false;
  }

  public boolean evaluate(Object currentRecord, OCommandContext ctx) {
    if (currentRecord instanceof YTResult) {
      return evaluate((YTResult) currentRecord, ctx);
    } else if (currentRecord instanceof YTIdentifiable) {
      return evaluate((YTIdentifiable) currentRecord, ctx);
    } else if (currentRecord instanceof Map) {
      YTDocument doc = new YTDocument();
      doc.fromMap((Map<String, Object>) currentRecord);
      return evaluate(doc, ctx);
    }
    return false;
  }

  public List<OBooleanExpression> getSubBlocks() {
    return subBlocks;
  }

  public void setSubBlocks(List<OBooleanExpression> subBlocks) {
    this.subBlocks = subBlocks;
  }

  public void addSubBlock(OBooleanExpression block) {
    this.subBlocks.add(block);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (subBlocks == null || subBlocks.size() == 0) {
      return;
    }

    boolean first = true;
    for (OBooleanExpression expr : subBlocks) {
      if (!first) {
        builder.append(" OR ");
      }
      expr.toString(params, builder);
      first = false;
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (subBlocks == null || subBlocks.size() == 0) {
      return;
    }

    boolean first = true;
    for (OBooleanExpression expr : subBlocks) {
      if (!first) {
        builder.append(" OR ");
      }
      expr.toGenericStatement(builder);
      first = false;
    }
  }

  @Override
  protected boolean supportsBasicCalculation() {
    for (OBooleanExpression expr : subBlocks) {
      if (!expr.supportsBasicCalculation()) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    int result = 0;
    for (OBooleanExpression expr : subBlocks) {
      result += expr.getNumberOfExternalCalculations();
    }
    return result;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    List<Object> result = new ArrayList<Object>();
    for (OBooleanExpression expr : subBlocks) {
      result.addAll(expr.getExternalCalculationConditions());
    }
    return result;
  }

  public List<OBinaryCondition> getIndexedFunctionConditions(
      YTClass iSchemaClass, YTDatabaseSessionInternal database) {
    if (subBlocks == null || subBlocks.size() > 1) {
      return null;
    }
    List<OBinaryCondition> result = new ArrayList<OBinaryCondition>();
    for (OBooleanExpression exp : subBlocks) {
      List<OBinaryCondition> sub = exp.getIndexedFunctionConditions(iSchemaClass, database);
      if (sub != null && sub.size() > 0) {
        result.addAll(sub);
      }
    }
    return result.size() == 0 ? null : result;
  }

  public List<OAndBlock> flatten() {
    List<OAndBlock> result = new ArrayList<OAndBlock>();
    for (OBooleanExpression sub : subBlocks) {
      List<OAndBlock> childFlattened = sub.flatten();
      for (OAndBlock child : childFlattened) {
        result.add(child);
      }
    }
    return result;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    for (OBooleanExpression expr : subBlocks) {
      if (expr.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public OOrBlock copy() {
    OOrBlock result = new OOrBlock(-1);
    result.subBlocks = subBlocks.stream().map(x -> x.copy()).collect(Collectors.toList());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OOrBlock oOrBlock = (OOrBlock) o;

    return Objects.equals(subBlocks, oOrBlock.subBlocks);
  }

  @Override
  public int hashCode() {
    return subBlocks != null ? subBlocks.hashCode() : 0;
  }

  @Override
  public boolean isEmpty() {
    if (subBlocks.isEmpty()) {
      return true;
    }
    for (OBooleanExpression block : subBlocks) {
      if (!block.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    for (OBooleanExpression block : subBlocks) {
      block.extractSubQueries(collector);
    }
  }

  @Override
  public boolean refersToParent() {
    for (OBooleanExpression exp : subBlocks) {
      if (exp != null && exp.refersToParent()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> result = new ArrayList<String>();
    for (OBooleanExpression exp : subBlocks) {
      List<String> x = exp.getMatchPatternInvolvedAliases();
      if (x != null) {
        result.addAll(x);
      }
    }
    return result.size() == 0 ? null : result;
  }

  @Override
  public void translateLuceneOperator() {
    subBlocks.forEach(x -> x.translateLuceneOperator());
  }

  @Override
  public boolean isCacheable(YTDatabaseSessionInternal session) {
    for (OBooleanExpression block : this.subBlocks) {
      if (!block.isCacheable(session)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public OBooleanExpression rewriteIndexChainsAsSubqueries(OCommandContext ctx, YTClass clazz) {
    for (OBooleanExpression exp : subBlocks) {
      exp.rewriteIndexChainsAsSubqueries(ctx, clazz);
    }
    return this;
  }

  public Optional<OIndexCandidate> findIndex(OIndexFinder info, OCommandContext ctx) {
    Optional<OIndexCandidate> result = Optional.empty();
    boolean first = true;
    for (OBooleanExpression exp : subBlocks) {
      Optional<OIndexCandidate> singleResult = exp.findIndex(info, ctx);
      if (singleResult.isPresent()) {
        if (first) {
          result = singleResult;

        } else if (result.isPresent()) {
          if (result.get() instanceof ORequiredIndexCanditate) {
            ((ORequiredIndexCanditate) result.get()).addCanditate(singleResult.get());
          } else {
            ORequiredIndexCanditate req = new ORequiredIndexCanditate();
            req.addCanditate(result.get());
            req.addCanditate(singleResult.get());
            result = Optional.of(req);
          }
        } else {
          return Optional.empty();
        }
      } else {
        return Optional.empty();
      }
      first = false;
    }
    return result;
  }

  @Override
  public boolean isAlwaysTrue() {
    if (subBlocks.isEmpty()) {
      return true;
    }
    for (OBooleanExpression exp : subBlocks) {
      if (exp.isAlwaysTrue()) {
        return true;
      }
    }
    return false;
  }
}
/* JavaCC - OriginalChecksum=98d3077303a598705894dbb7bd4e1573 (do not edit this line) */
