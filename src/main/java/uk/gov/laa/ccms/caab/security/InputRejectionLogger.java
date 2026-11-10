package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.laa.ccms.caab.logging.HandledException;
import uk.gov.laa.ccms.caab.logging.RequestRoute;

/**
 * Logs requests rejected as malformed or not allowed, and notes every exception on the request so
 * the audit event can tell the request failed. It then leaves the exception to the existing
 * handlers by resolving nothing.
 */
@Component
@RequiredArgsConstructor
public class InputRejectionLogger implements HandlerExceptionResolver, Ordered {

  private final SecurityEventLogger securityEventLogger;

  @Override
  public ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    HandledException.mark(request, ex);
    if (isInputRejection(ex)) {
      securityEventLogger.inputRejected(request.getMethod(), RequestRoute.of(request), ex);
    }
    return null;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  private static boolean isInputRejection(Exception ex) {
    return ex instanceof MultipartException
        || ex instanceof TypeMismatchException
        || ex instanceof ServletRequestBindingException
        || ex instanceof HttpMessageNotReadableException
        || ex instanceof HttpRequestMethodNotSupportedException
        || ex instanceof HttpMediaTypeNotSupportedException
        || ex instanceof HttpMediaTypeNotAcceptableException;
  }
}
