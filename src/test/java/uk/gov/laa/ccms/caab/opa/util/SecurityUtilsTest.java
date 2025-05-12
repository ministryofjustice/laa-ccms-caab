package uk.gov.laa.ccms.caab.opa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.laa.ccms.caab.opa.security.Encryptor;

@ExtendWith(MockitoExtension.class)
public class SecurityUtilsTest {
  private SecurityUtils securityUtils;
  @Mock
  private Encryptor encryptor;

  private static final String RETURN_URL = "http://example.com";
  private static final String PASSWORD = "password";
  private static final String USERNAME = "username";
  private static final String PROVIDER_NAME = "provider";

  @BeforeEach
  public void setUp() {
    securityUtils = new SecurityUtils(RETURN_URL, PASSWORD);
  }

  private static Stream<Arguments> testCreateJsonTokenParameters() {
    return Stream.of(
        Arguments.of(null, PROVIDER_NAME, "username is null cannot create ContextToken"),
        Arguments.of(USERNAME, null, "providerName is null cannot create ContextToken")
    );
  }

  @ParameterizedTest
  @MethodSource("testCreateJsonTokenParameters")
  void testCreateJsonToken_withErrors(
      final String username,
      final String providerName,
      final String expectedMessage) {
    final Exception exception = assertThrows(UnsupportedOperationException.class, () ->
      securityUtils.createJsonToken(username, providerName));
    final String actualMessage = exception.getMessage();

    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testCreateJsonToken_success() {
    String jsonToken = securityUtils.createJsonToken(USERNAME, PROVIDER_NAME);
    assertNotNull(jsonToken);
  }
}
