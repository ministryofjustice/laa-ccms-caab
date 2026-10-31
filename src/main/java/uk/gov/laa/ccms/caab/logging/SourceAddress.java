package uk.gov.laa.ccms.caab.logging;

import java.util.regex.Pattern;

/** Reads the forwarded client address from an {@code X-Forwarded-For} header. */
final class SourceAddress {

  private static final Pattern IPV4 =
      Pattern.compile("(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)");
  private static final Pattern HEX_GROUP = Pattern.compile("[0-9a-fA-F]{1,4}");

  private SourceAddress() {}

  /**
   * Returns the right-most address in the header, which is the one added by the proxy nearest to
   * this application. Earlier entries are supplied by the client and can be forged. The value is
   * returned only if it is an IP address literal, so no free text from the header is ever logged.
   *
   * @param forwardedFor the header value, which may be null
   * @return the address, or null if the header is absent or its last entry is not an IP address
   */
  static String forwardedIp(String forwardedFor) {
    if (forwardedFor == null) {
      return null;
    }
    String last = forwardedFor.substring(forwardedFor.lastIndexOf(',') + 1).trim();
    return isIpLiteral(last) ? last : null;
  }

  private static boolean isIpLiteral(String value) {
    return IPV4.matcher(value).matches() || isIpv6(value);
  }

  private static boolean isIpv6(String value) {
    long colons = value.chars().filter(c -> c == ':').count();
    if (value.length() > 45
        || colons < 2
        || colons > 7
        || value.contains(":::")
        || value.indexOf("::") != value.lastIndexOf("::")) {
      return false;
    }
    String[] groups = value.split(":", -1);
    for (int i = 0; i < groups.length; i++) {
      boolean lastGroup = i == groups.length - 1;
      if (!groups[i].isEmpty()
          && !HEX_GROUP.matcher(groups[i]).matches()
          && !(lastGroup && IPV4.matcher(groups[i]).matches())) {
        return false;
      }
    }
    return true;
  }
}
