package com.jetbrains.youtrack.db.internal.core.security;

import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.OKeyProvider;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.OTokenHeader;
import java.security.Key;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 */
public class DefaultKeyProvider implements OKeyProvider {

  private final SecretKeySpec secretKey;

  public DefaultKeyProvider(byte[] secret) {
    secretKey = new SecretKeySpec(secret, "HmacSHA256");
  }

  @Override
  public Key getKey(OTokenHeader header) {
    return secretKey;
  }

  @Override
  public String getDefaultKey() {
    return "default";
  }

  @Override
  public String[] getKeys() {
    return new String[]{"default"};
  }
}