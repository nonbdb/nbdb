/* Generated By:JJTree: Do not edit this line. OFetchPlan.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResultInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OFetchPlan extends SimpleNode {

  protected List<OFetchPlanItem> items = new ArrayList<OFetchPlanItem>();

  public OFetchPlan(int id) {
    super(id);
  }

  public OFetchPlan(OrientSql p, int id) {
    super(p, id);
  }

  public void addItem(OFetchPlanItem item) {
    this.items.add(item);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("FETCHPLAN ");
    boolean first = true;
    for (OFetchPlanItem item : items) {
      if (!first) {
        builder.append(" ");
      }

      item.toString(params, builder);
      first = false;
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("FETCHPLAN ");
    boolean first = true;
    for (OFetchPlanItem item : items) {
      if (!first) {
        builder.append(" ");
      }

      item.toGenericStatement(builder);
      first = false;
    }
  }

  public OFetchPlan copy() {
    OFetchPlan result = new OFetchPlan(-1);
    result.items = items.stream().map(x -> x.copy()).collect(Collectors.toList());
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

    OFetchPlan that = (OFetchPlan) o;

    return Objects.equals(items, that.items);
  }

  @Override
  public int hashCode() {
    return items != null ? items.hashCode() : 0;
  }

  public YTResult serialize(YTDatabaseSessionInternal db) {
    YTResultInternal result = new YTResultInternal(db);
    if (items != null) {
      result.setProperty(
          "items", items.stream().map(oFetchPlanItem -> oFetchPlanItem.serialize(db))
              .collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(YTResult fromResult) {

    if (fromResult.getProperty("items") != null) {
      List<YTResult> ser = fromResult.getProperty("items");
      items = new ArrayList<>();
      for (YTResult r : ser) {
        OFetchPlanItem exp = new OFetchPlanItem(-1);
        exp.deserialize(r);
        items.add(exp);
      }
    }
  }
}
/* JavaCC - OriginalChecksum=b4cd86f2c6e8fc5e9dce8912389a1167 (do not edit this line) */