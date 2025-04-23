package uk.gov.laa.ccms.caab.opa.security;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides utility methods for encoding and decoding URLs, with custom
 * handling for specific characters.
 */
@Slf4j
@NoArgsConstructor
public class ContextUrlEncoder {

  /**
   * Encodes the given value using the specified encoding.
   *
   * @param value the value to be encoded
   * @param enc the encoding to be used
   * @return the encoded value
   * @throws UnsupportedEncodingException if the encoding is not supported
   */
  public static String encode(final String value, final String enc)
      throws UnsupportedEncodingException {
    return deflate(URLEncoder.encode(value, enc));
  }

  /**
   * Decodes the given value using the specified encoding.
   *
   * @param value the value to be decoded
   * @param enc the encoding to be used
   * @return the decoded value
   * @throws UnsupportedEncodingException if the encoding is not supported
   */
  public static String decode(final String value, final String enc)
      throws UnsupportedEncodingException {
    return URLDecoder.decode(inflate(value), enc);
  }

  /**
   * Replaces '%' characters in the input value with '*' characters.
   *
   * @param inputValue the value to be deflated
   * @return the deflated value
   */
  private static String deflate(final String inputValue) {
    final String outputValue = StringUtils.replaceEach(
        inputValue, new String[]{"%"}, new String[]{"*"});
    log.debug("deflate() input ...[" + inputValue + "]");
    log.debug("deflate() output ..[" + outputValue + "]");
    return outputValue;
  }

  /**
   * Replaces '*' characters in the input value with '%' characters.
   *
   * @param inputValue the value to be inflated
   * @return the inflated value
   */
  private static String inflate(final String inputValue) {
    final String outputValue = StringUtils.replaceEach(
        inputValue, new String[]{"*"}, new String[]{"%"});
    log.debug("inflate() input ...[" + inputValue + "]");
    log.debug("inflate() output ..[" + outputValue + "]");
    return outputValue;
  }
}
