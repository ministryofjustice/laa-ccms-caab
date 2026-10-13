package uk.gov.laa.ccms.caab.opa.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EncryptorTest {

  @Test
  void decryptThrowsSafeMessageWhenDecodingFails() {
    Encryptor encryptor = new Encryptor("password");

    SecurityException exception =
        assertThrows(SecurityException.class, () -> encryptor.decrypt("not-valid-base64"));

    assertEquals(Encryptor.DECRYPTION_FAILURE_MESSAGE, exception.getMessage());
    assertNotNull(exception.getCause());
  }
}
