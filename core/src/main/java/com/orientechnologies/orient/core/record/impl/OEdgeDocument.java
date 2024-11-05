package com.orientechnologies.orient.core.record.impl;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import java.util.Set;
import javax.annotation.Nullable;

public class OEdgeDocument extends ODocument implements OEdgeInternal {
  public OEdgeDocument(ODatabaseSession session, String cl) {
    super(session, cl);
  }

  public OEdgeDocument() {
    super();
  }

  public OEdgeDocument(ODatabaseSession session) {
    super(session);
  }

  public OEdgeDocument(ODatabaseSession database, ORID rid) {
    super(database, rid);
  }

  @Override
  public OVertex getFrom() {
    Object result = getPropertyWithoutValidation(DIRECTION_OUT);
    if (!(result instanceof OElement v)) {
      return null;
    }

    if (!v.isVertex()) {
      return null;
    }

    return v.toVertex();
  }

  @Override
  @Nullable
  public OIdentifiable getFromIdentifiable() {
    var db = getDatabase();
    var schema = db.getMetadata().getImmutableSchemaSnapshot();

    var result = getLinkPropertyWithoutValidation(DIRECTION_OUT);
    if (result == null) {
      return null;
    }

    var rid = result.getIdentity();
    if (schema.getClassByClusterId(rid.getClusterId()).isVertexType()) {
      return rid;
    }

    return null;
  }

  @Override
  public OVertex getTo() {
    Object result = getPropertyWithoutValidation(DIRECTION_IN);
    if (!(result instanceof OElement v)) {
      return null;
    }

    if (!v.isVertex()) {
      return null;
    }

    return v.toVertex();
  }

  @Override
  public OIdentifiable getToIdentifiable() {
    var db = getDatabase();
    var schema = db.getMetadata().getImmutableSchemaSnapshot();

    var result = getLinkPropertyWithoutValidation(DIRECTION_IN);
    if (result == null) {
      return null;
    }

    var rid = result.getIdentity();
    if (schema.getClassByClusterId(rid.getClusterId()).isVertexType()) {
      return rid;
    }

    return null;
  }

  @Override
  public boolean isLightweight() {
    // LIGHTWEIGHT EDGES MANAGED BY OEdgeDelegate, IN FUTURE MAY BE WE NEED TO HANDLE THEM WITH THIS
    return false;
  }

  public OEdgeDocument delete() {
    checkForBinding();

    super.delete();
    return this;
  }

  @Override
  @Nullable
  public ODocument getBaseDocument() {
    return this;
  }

  @Override
  public OEdgeDocument copy() {
    checkForBinding();

    return (OEdgeDocument) super.copyTo(new OEdgeDocument());
  }

  @Override
  public Set<String> getPropertyNames() {
    checkForBinding();

    return OEdgeInternal.filterPropertyNames(super.getPropertyNames());
  }

  @Override
  public <RET> RET getProperty(String fieldName) {
    checkForBinding();

    OEdgeInternal.checkPropertyName(fieldName);

    return getPropertyWithoutValidation(fieldName);
  }

  @Nullable
  @Override
  public OIdentifiable getLinkProperty(String fieldName) {
    checkForBinding();

    OEdgeInternal.checkPropertyName(fieldName);

    return super.getLinkProperty(fieldName);
  }

  @Override
  public void setProperty(String fieldName, Object propertyValue) {
    checkForBinding();

    OEdgeInternal.checkPropertyName(fieldName);

    setPropertyWithoutValidation(fieldName, propertyValue);
  }

  @Override
  public void setProperty(String name, Object value, OType... types) {
    checkForBinding();
    OEdgeInternal.checkPropertyName(name);

    super.setProperty(name, value, types);
  }

  @Override
  public <RET> RET removeProperty(String fieldName) {
    checkForBinding();
    OEdgeInternal.checkPropertyName(fieldName);

    return removePropertyWithoutValidation(fieldName);
  }

  @Override
  public void promoteToRegularEdge() {
    checkForBinding();
  }

  public static void deleteLinks(OEdge delegate) {
    OVertex from = delegate.getFrom();
    if (from != null) {
      OVertexInternal.removeOutgoingEdge(from, delegate);
    }
    OVertex to = delegate.getTo();
    if (to != null) {
      OVertexInternal.removeIncomingEdge(to, delegate);
    }
  }
}
