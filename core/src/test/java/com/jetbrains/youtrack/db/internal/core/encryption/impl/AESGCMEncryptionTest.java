package com.jetbrains.youtrack.db.internal.core.encryption.impl;

import com.jetbrains.youtrack.db.internal.core.exception.SecurityException;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;

public class AESGCMEncryptionTest {

  @Test(expected = SecurityException.class)
  public void testNotInited() {
    AESGCMEncryption encryption = new AESGCMEncryption();
    byte[] original = "this is a test string to encrypt".getBytes();
    encryption.encrypt(original);
  }

  @Test
  public void test() {
    AESGCMEncryption encryption = new AESGCMEncryption();
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
