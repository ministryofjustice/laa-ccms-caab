package uk.gov.laa.ccms.caab.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.security.CspNonceFilter;

@ExtendWith(MockitoExtension.class)
class CspAdviceTest {

  @Mock private HttpServletRequest request;

  @InjectMocks private CspAdvice cspAdvice;

  @Test
  void getCspNonce_returnsNonceFromRequestAttribute() {
    String nonce = "test-nonce";
    when(request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE)).thenReturn(nonce);

    String result = cspAdvice.getCspNonce(request);

    assertEquals(nonce, result);
  }

  @Test
  void getCspNonce_returnsNullWhenAttributeMissing() {
    when(request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE)).thenReturn(null);

    String result = cspAdvice.getCspNonce(request);

    assertNull(result);
  }
}
