package uk.gov.laa.ccms.caab.opa.security;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility class for encoding context URLs.
 */
public class ContextUrlEncoder {

  private static final Logger logger = LoggerFactory.getLogger(ContextUrlEncoder.class.getName());
  private static final String[] deflatedForm = new String[]{";", "?", "/", ":", "#", "&", "=", "+",
      "$", ",", "+", " ", "<", ">", "~"};  // ,"%"
  private static final String[] expandedForm = new String[]{"%3B", "%3F", "%2F", "%3A", "%23",
      "%26", "%3D", "%2B", "%24", "%2C", "%20", "%20", "%3C",
      "%3E", "%7E"};  // ,"%25"

  public static String encode(final String value, final String enc)
      throws UnsupportedEncodingException {
    return deflate(URLEncoder.encode(value, enc));
  }

  public static String decode(final String value, final String enc)
      throws UnsupportedEncodingException {
    return URLDecoder.decode(inflate(value), enc);
  }

  private static String deflate(final String inputValue) {
    final String outputValue = StringUtils.replaceEach(
        inputValue, new String[]{"%"}, new String[]{"*"});
    logger.debug("deflate() input ...[" + inputValue + "]");
    logger.debug("deflate() output ..[" + outputValue + "]");
    return outputValue;
  }

  private static String inflate(final String inputValue) {
    final String outputValue = StringUtils.replaceEach(
        inputValue, new String[]{"*"}, new String[]{"%"});
    logger.debug("inflate() input ...[" + inputValue + "]");
    logger.debug("inflate() output ..[" + outputValue + "]");
    return outputValue;
  }
}
