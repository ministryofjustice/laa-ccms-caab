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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "::",
        "::1",
        "2001:db8::1",
        "1:2:3:4:5:6:7:8",
        "1:2:3:4:5:6:7::",
        "::1:2:3:4:5:6:7",
        "::ffff:192.0.2.1",
        "1:2:3:4:5:6:192.0.2.1"
      })
  void acceptsEveryValidIpv6Form(String address) {
    assertEquals(address, SourceAddress.forwardedIp(address));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1:2:3",
        ":1:2",
        "bad:bad:bad",
        "1:2:3:4:5:6:7",
        "1:2:3:4:5:6:7:8:9",
        "1:2:3:4:5:6:7:",
        "1:2:3:4:5:6:7:192.0.2.1",
        "1:2:3:4:5:6:7::192.0.2.1",
        "1::2::3",
        "12345::1"
      })
  void rejectsMalformedIpv6(String address) {
    assertNull(SourceAddress.forwardedIp(address));
  }
}
