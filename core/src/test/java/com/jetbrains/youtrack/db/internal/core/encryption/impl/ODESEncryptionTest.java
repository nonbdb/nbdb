package com.jetbrains.youtrack.db.internal.core.encryption.impl;

import com.jetbrains.youtrack.db.internal.core.exception.YTSecurityException;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;

public class ODESEncryptionTest {

  @Test(expected = YTSecurityException.class)
  public void testNotInited() {
    ODESEncryption encryption = new ODESEncryption();
    byte[] original = "this is a test string to encrypt".getBytes();
    encryption.encrypt(original);
  }

  @Test
  public void test() {
    ODESEncryption encryption = new ODESEncryption();
    String key =
        new String(
            Base64.getEncoder()
                .encode(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8}));
    encryption.configure(key);

    byte[] original = "this is a test string to encrypt".getBytes();
    byte[] encrypted = encryption.encrypt(original);
    Assert.assertArrayEquals(original, encryption.decrypt(encrypted));
  }
}