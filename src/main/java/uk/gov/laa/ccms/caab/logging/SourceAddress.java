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
    if (value.length() > 45 || value.indexOf(':') < 0 || value.contains(":::")) {
      return false;
    }
    int gap = value.indexOf("::");
    if (gap != value.lastIndexOf("::")) {
      return false;
    }
    if (gap < 0) {
      return groupCount(value, true) == 8;
    }
    int head = groupCount(value.substring(0, gap), false);
    int tail = groupCount(value.substring(gap + 2), true);
    return head >= 0 && tail >= 0 && head + tail <= 7;
  }

  private static int groupCount(String part, boolean mayEndInIpv4) {
    if (part.isEmpty()) {
      return 0;
    }
    String[] groups = part.split(":", -1);
    int count = 0;
    for (int i = 0; i < groups.length; i++) {
      if (HEX_GROUP.matcher(groups[i]).matches()) {
        count++;
      } else if (mayEndInIpv4 && i == groups.length - 1 && IPV4.matcher(groups[i]).matches()) {
        count += 2;
      } else {
        return -1;
      }
    }
    return count;
  }
}
