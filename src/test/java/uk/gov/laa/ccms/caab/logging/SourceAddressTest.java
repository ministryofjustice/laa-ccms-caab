package uk.gov.laa.ccms.caab.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SourceAddressTest {

  @Test
  void takesTheRightMostEntry() {
    assertEquals("203.0.113.9", SourceAddress.forwardedIp("6.6.6.6, 198.51.100.4,203.0.113.9"));
    assertEquals("203.0.113.9", SourceAddress.forwardedIp("203.0.113.9"));
  }

  @Test
  void acceptsIpv6() {
    assertEquals("2001:db8::1", SourceAddress.forwardedIp("1.2.3.4, 2001:db8::1"));
  }

  @Test
  void absentHeaderGivesNull() {
    assertNull(SourceAddress.forwardedIp(null));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        " ",
        "unknown",
        "1.2.3.4:8080",
        "999.1.1.1",
        "1.2.3",
        "evil.example.com",
        "1.2.3.4, <script>",
        "::::::::::::::::::::::::::::::::::::::::::::::::::"
      })
  void anythingThatIsNotAnIpAddressGivesNull(String header) {
    assertNull(SourceAddress.forwardedIp(header));
  }
}
