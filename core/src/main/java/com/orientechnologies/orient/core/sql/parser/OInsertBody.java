/* Generated By:JJTree: Do not edit this line. OInsertBody.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OInsertBody extends SimpleNode {

  private List<OIdentifier> identifierList;
  private List<List<OExpression>> valueExpressions;
  private List<OInsertSetExpression> setExpressions;

  private List<OJson> content;
  private List<OInputParameter> contentInputParam;

  public OInsertBody(int id) {
    super(id);
  }

  public OInsertBody(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {

    if (identifierList != null) {
      builder.append("(");
      boolean first = true;
      for (OIdentifier item : identifierList) {
        if (!first) {
          builder.append(", ");
        }
        item.toString(params, builder);
        first = false;
      }
      builder.append(") VALUES ");
      if (valueExpressions != null) {
        boolean firstList = true;
        for (List<OExpression> itemList : valueExpressions) {
          if (firstList) {
            builder.append("(");
          } else {
            builder.append("),(");
          }
          first = true;
          for (OExpression item : itemList) {
            if (!first) {
              builder.append(", ");
            }
            item.toString(params, builder);
            first = false;
          }
          firstList = false;
        }
      }
      builder.append(")");
    }

    if (setExpressions != null) {
      builder.append("SET ");
      boolean first = true;
      for (OInsertSetExpression item : setExpressions) {
        if (!first) {
          builder.append(", ");
        }
        item.toString(params, builder);
        first = false;
      }
    }

    if (content != null || contentInputParam != null) {
      builder.append("CONTENT ");
      boolean first = true;
      if (content != null) {
        for (OJson item : content) {
          if (!first) {
            builder.append(", ");
          }
          item.toString(params, builder);
          first = false;
        }
      } else if (contentInputParam != null) {
        for (OInputParameter item : contentInputParam) {
          if (!first) {
            builder.append(", ");
          }
          item.toString(params, builder);
          first = false;
        }
      }
    }
  }

  public void toGenericStatement(StringBuilder builder) {

    if (identifierList != null) {
      builder.append("(");
      boolean first = true;
      for (OIdentifier item : identifierList) {
        if (!first) {
          builder.append(", ");
        }
        item.toGenericStatement(builder);
        first = false;
      }
      builder.append(") VALUES ");
      if (valueExpressions != null) {
        boolean firstList = true;
        for (List<OExpression> itemList : valueExpressions) {
          if (firstList) {
            builder.append("(");
          } else {
            builder.append("),(");
          }
          first = true;
          for (OExpression item : itemList) {
            if (!first) {
              builder.append(", ");
            }
            item.toGenericStatement(builder);
            first = false;
          }
          firstList = false;
        }
      }
      builder.append(")");
    }

    if (setExpressions != null) {
      builder.append("SET ");
      boolean first = true;
      for (OInsertSetExpression item : setExpressions) {
        if (!first) {
          builder.append(", ");
        }
        item.toGenericStatement(builder);
        first = false;
      }
    }

    if (content != null || contentInputParam != null) {
      builder.append("CONTENT ");
      boolean first = true;
      if (content != null) {
        for (OJson item : content) {
          if (!first) {
            builder.append(", ");
          }
          item.toGenericStatement(builder);
          first = false;
        }
      } else if (contentInputParam != null) {
        for (OInputParameter item : contentInputParam) {
          if (!first) {
            builder.append(", ");
          }
          item.toGenericStatement(builder);
          first = false;
        }
      }
    }
  }

  public OInsertBody copy() {
    OInsertBody result = new OInsertBody(-1);
    result.identifierList =
        identifierList == null
            ? null
            : identifierList.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.valueExpressions =
        valueExpressions == null
            ? null
            : valueExpressions.stream()
                .map(sub -> sub.stream().map(x -> x.copy()).collect(Collectors.toList()))
                .collect(Collectors.toList());
    result.setExpressions =
        setExpressions == null
            ? null
            : setExpressions.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.content =
        content == null ? null : content.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.contentInputParam =
        contentInputParam == null
            ? null
            : contentInputParam.stream().map(x -> x.copy()).collect(Collectors.toList());
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

    OInsertBody that = (OInsertBody) o;

    if (!Objects.equals(identifierList, that.identifierList)) {
      return false;
    }
    if (!Objects.equals(valueExpressions, that.valueExpressions)) {
      return false;
    }
    if (!Objects.equals(setExpressions, that.setExpressions)) {
      return false;
    }
    if (!Objects.equals(content, that.content)) {
      return false;
    }
    return Objects.equals(contentInputParam, that.contentInputParam);
  }

  @Override
  public int hashCode() {
    int result = identifierList != null ? identifierList.hashCode() : 0;
    result = 31 * result + (valueExpressions != null ? valueExpressions.hashCode() : 0);
    result = 31 * result + (setExpressions != null ? setExpressions.hashCode() : 0);
    result = 31 * result + (content != null ? content.hashCode() : 0);
    result = 31 * result + (contentInputParam != null ? contentInputParam.hashCode() : 0);
    return result;
  }

  public List<OIdentifier> getIdentifierList() {
    return identifierList;
  }

  public void addIdentifier(OIdentifier identifier) {
    if (this.identifierList == null) {
      this.identifierList = new ArrayList<>();
    }
    this.identifierList.add(identifier);
  }

  public List<List<OExpression>> getValueExpressions() {
    return valueExpressions;
  }

  public void addValueExpression(List<OExpression> exp) {
    if (this.valueExpressions == null) {
      this.valueExpressions = new ArrayList<>();
    }
    this.valueExpressions.add(exp);
  }

  public List<OInsertSetExpression> getSetExpressions() {
    return setExpressions;
  }

  public void addInsertSetExpression(OInsertSetExpression exp) {
    if (this.setExpressions == null) {
      this.setExpressions = new ArrayList<>();
    }
    this.setExpressions.add(exp);
  }

  public List<OJson> getContent() {
    return content;
  }

  public List<OInputParameter> getContentInputParam() {
    return contentInputParam;
  }

  public void addContentInputParam(OInputParameter par) {
    if (contentInputParam == null) {
      contentInputParam = new ArrayList<>();
    }
    contentInputParam.add(par);
  }

  public void addContent(OJson json) {
    if (content == null) {
      content = new ArrayList<>();
    }
    content.add(json);
  }

  public boolean isCacheable(YTDatabaseSessionInternal session) {

    if (this.valueExpressions != null) {
      for (List<OExpression> valueExpression : valueExpressions) {
        for (OExpression oExpression : valueExpression) {
          if (!oExpression.isCacheable(session)) {
            return false;
          }
        }
      }
    }
    if (setExpressions != null) {
      for (OInsertSetExpression setExpression : setExpressions) {
        if (!setExpression.isCacheable(session)) {
          return false;
        }
      }
    }

    if (content != null) {
      for (OJson item : content) {
        if (!item.isCacheable()) {
          return false;
        }
      }
    }

    return true;
  }
}
/* JavaCC - OriginalChecksum=7d2079a41a1fc63a812cb679e729b23a (do not edit this line) */
