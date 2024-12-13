package com.orientechnologies.orient.server.token;

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.JwtPayload;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.TokenMetaInfo;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
public class OrientJwtPayload implements JwtPayload {

  public String iss;
  public String sub;
  public String aud;
  public String jti;
  public long exp;
  public long iat;
  public long nbf;
  public RID userRid;
  public String database;
  public String databaseType;

  @Override
  public String getIssuer() {
    return iss;
  }

  @Override
  public void setIssuer(String iss) {
    this.iss = iss;
  }

  @Override
  public long getExpiry() {
    return exp;
  }

  @Override
  public void setExpiry(long exp) {
    this.exp = exp;
  }

  @Override
  public long getIssuedAt() {
    return iat;
  }

  @Override
  public void setIssuedAt(long iat) {
    this.iat = iat;
  }

  @Override
  public long getNotBefore() {
    return nbf;
  }

  @Override
  public void setNotBefore(long nbf) {
    this.nbf = nbf;
  }

  @Override
  public String getUserName() {
    return sub;
  }

  @Override
  public void setUserName(String sub) {
    this.sub = sub;
  }

  @Override
  public String getAudience() {
    return aud;
  }

  @Override
  public void setAudience(String aud) {
    this.aud = aud;
  }

  @Override
  public String getTokenId() {
    return jti;
  }

  @Override
  public void setTokenId(String jti) {
    this.jti = jti;
  }

  public RID getUserRid() {
    return userRid;
  }

  public void setUserRid(RID userRid) {
    this.userRid = userRid;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String dbName) {
    this.database = dbName;
  }

  @Override
  public String getDatabaseType() {
    return databaseType;
  }

  @Override
  public void setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
  }

  @Override
  public String getPayloadType() {
    return "";
  }

  @Override
  public void serialize(DataOutputStream output, TokenMetaInfo serializer) throws IOException {
    throw new UnsupportedOperationException();
  }
}
