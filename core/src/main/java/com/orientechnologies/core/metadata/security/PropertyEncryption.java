package com.orientechnologies.core.metadata.security;

public interface PropertyEncryption {

  boolean isEncrypted(String name);

  byte[] encrypt(String name, byte[] values);

  byte[] decrypt(String name, byte[] values);
}
