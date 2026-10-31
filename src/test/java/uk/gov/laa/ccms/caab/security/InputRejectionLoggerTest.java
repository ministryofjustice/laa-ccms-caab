package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ExtendWith(MockitoExtension.class)
class InputRejectionLoggerTest {

  @Mock private SecurityEventLogger securityEventLogger;

  @InjectMocks private InputRejectionLogger resolver;

  static Stream<Exception> rejections() {
    return Stream.of(
        new MaxUploadSizeExceededException(1L),
        new TypeMismatchException("abc", Integer.class),
        new MissingServletRequestParameterException("id", "String"),
        new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0])),
        new HttpRequestMethodNotSupportedException("TRACE"));
  }

  @ParameterizedTest
  @MethodSource("rejections")
  void logsRejectionWithPathOnlyAndLeavesHandlingToOthers(Exception exception) {
    var request = new MockHttpServletRequest("POST", "/application/sections");
    request.setQueryString("ref=SENSITIVE");

    var result = resolver.resolveException(request, new MockHttpServletResponse(), null, exception);

    assertNull(result);
    verify(securityEventLogger)
        .inputRejected(eq("POST"), eq("/application/sections"), eq(exception));
  }

  @Test
  void ignoresOtherExceptions() {
    var result =
        resolver.resolveException(
            new MockHttpServletRequest("GET", "/home"),
            new MockHttpServletResponse(),
            null,
            new IllegalStateException("boom"));

    assertNull(result);
    verifyNoInteractions(securityEventLogger);
  }

  @Test
  void runsBeforeTheOtherResolvers() {
    org.junit.jupiter.api.Assertions.assertEquals(
        org.springframework.core.Ordered.HIGHEST_PRECEDENCE, resolver.getOrder());
  }
}
