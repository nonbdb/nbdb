package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.record.YTEntity;
import com.orientechnologies.orient.core.record.impl.YTEntityInternal;

/**
 *
 */
public class OUpdatableResult extends OResultInternal {

  protected OResultInternal previousValue = null;

  public OUpdatableResult(YTDatabaseSessionInternal session, YTEntity element) {
    super(session, element);
  }

  @Override
  public boolean isElement() {
    return true;
  }

  public <T> T getProperty(String name) {
    loadIdentifiable();
    T result = null;
    if (content != null && content.containsKey(name)) {
      result = (T) content.get(name);
    } else if (isElement()) {
      result = ((YTEntity) identifiable).getProperty(name);
    }
    if (result instanceof YTIdentifiable && ((YTIdentifiable) result).getIdentity()
        .isPersistent()) {
      result = (T) ((YTIdentifiable) result).getIdentity();
    }
    return result;
  }

  @Override
  public YTEntity toElement() {
    return (YTEntity) identifiable;
  }

  @Override
  public void setProperty(String name, Object value) {
    ((YTEntityInternal) identifiable).setPropertyInternal(name, value);
  }

  public void removeProperty(String name) {
    ((YTEntity) identifiable).removeProperty(name);
  }
}
