/* PLEASE NOTE
 * At present this class is used for encrypting sensitive information that is
 * passed between LSC Online and CCLF. If it is changed you must ensure that it
 * is redeployed on all sides so that it can still function, the class should
 * ideally be changed in Online-JAAS and then propogated to any external systems
 * that use it.
 *
 *  In Online-JAAS realm this class is deployed in package uk.gov.lsc.jaas.util
 *  on CCLF this class is deployed in package uk.gov.lsc.cclf.web.util
 *  on LSC Online this class is deployed in package lsc.ebusiness.comp.system
 */

package uk.gov.laa.ccms.caab.opa.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import lombok.extern.slf4j.Slf4j;

/** Provides cryptographic functions for encrypting and decrypting a given String. */
@Slf4j
public class Encryptor {

  private Cipher encryptCipher;

  private Cipher decryptCipher;

  private String password;

  /**
   * Constructs an Encryptor and initializes it with a master password, salt, and iteration count.
   *
   * @throws SecurityException if an error occurs during initialization
   */
  public Encryptor(final String password) throws SecurityException {

    /* If this master password is changed then ensure this class is redeployed on all systems that
    use its functions.*/
    final char[] pass = password.toCharArray();

    final byte[] salt = {
      (byte) 0xa3,
      (byte) 0x21,
      (byte) 0x24,
      (byte) 0x2c,
      (byte) 0xf2,
      (byte) 0xd2,
      (byte) 0x3e,
      (byte) 0x19
    };
    final int iterations = 3;

    init(pass, salt, iterations);
  }

  private void init(final char[] pass, final byte[] salt, final int iterations)
      throws SecurityException {
    try {
      final PBEParameterSpec ps = new PBEParameterSpec(salt, 20);
      final SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      final SecretKey k = kf.generateSecret(new PBEKeySpec(pass));

      encryptCipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
      encryptCipher.init(Cipher.ENCRYPT_MODE, k, ps);

      decryptCipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
      decryptCipher.init(Cipher.DECRYPT_MODE, k, ps);
    } catch (final Exception e) {
      throw new SecurityException("Could not initialize CryptoLibrary: " + e.getMessage());
    }
  }

  /**
   * encrypt a given string.
   *
   * @param value Description of the Parameter
   * @return String the encrypted string.
   * @exception SecurityException Description of the Exceptio
   */
  public synchronized String encrypt(final String value) throws SecurityException {

    try {
      final byte[] utf8Value = value.getBytes(StandardCharsets.UTF_8);
      final byte[] encryptedValue = encryptCipher.doFinal(utf8Value);

      return Base64.getEncoder().encodeToString(encryptedValue);
    } catch (final Exception e) {
      throw new SecurityException("Could not encrypt: " + e.getMessage());
    }
  }

  /**
   * decrypt an encrypted string.
   *
   * @param str Description of the Parameter
   * @return String the encrypted string.
   * @exception SecurityException Description of the Exception
   */
  public synchronized String decrypt(final String str) throws SecurityException {
    try {
      return decodetoken(str);
    } catch (final Exception e) {
      throw new SecurityException("Could not decrypt: " + e.getMessage());
    }
  }

  private String decodetoken(final String str)
      throws IllegalBlockSizeException, BadPaddingException {
    final byte[] decryptedValue;
    // Directly use java.util.Base64 for decoding
    decryptedValue = decryptCipher.doFinal(Base64.getDecoder().decode(str));
    return new String(decryptedValue, StandardCharsets.UTF_8);
  }
}
