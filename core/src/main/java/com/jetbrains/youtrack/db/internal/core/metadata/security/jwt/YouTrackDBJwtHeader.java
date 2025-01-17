package com.jetbrains.youtrack.db.internal.core.metadata.security.jwt;

/**
 *
 */
public class YouTrackDBJwtHeader implements TokenHeader {

  private String typ;
  private String alg;
  private String kid;

  @Override
  public String getAlgorithm() {
    return alg;
  }

  @Override
  public void setAlgorithm(String alg) {
    this.alg = alg;
  }

  @Override
  public String getType() {
    return typ;
  }

  @Override
  public void setType(String typ) {
    this.typ = typ;
  }

  @Override
  public String getKeyId() {
    return kid;
  }

  @Override
  public void setKeyId(String kid) {
    this.kid = kid;
  }
}
