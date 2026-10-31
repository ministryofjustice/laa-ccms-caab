package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Logs requests rejected as malformed or not allowed, then leaves the exception to the existing
 * handlers by resolving nothing.
 */
@Component
@RequiredArgsConstructor
public class InputRejectionLogger implements HandlerExceptionResolver, Ordered {

  private final SecurityEventLogger securityEventLogger;

  @Override
  public ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if (isInputRejection(ex)) {
      securityEventLogger.inputRejected(request.getMethod(), request.getRequestURI(), ex);
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
        || ex instanceof HttpMediaTypeNotSupportedException;
  }
}
