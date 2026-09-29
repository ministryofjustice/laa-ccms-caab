package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Returns multipart size failures raised during CSRF processing to the upload form. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadSizeErrorFilter extends OncePerRequestFilter {

  public static final String SESSION_ATTRIBUTE = UploadSizeErrorFilter.class.getName() + ".path";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      chain.doFilter(request, response);
    } catch (InvalidParameterException exception) {
      String path = request.getRequestURI().substring(request.getContextPath().length());
      if (!"POST".equals(request.getMethod())
          || UploadSizeErrorRoutes.formName(path) == null
          || !isSizeError(exception)) {
        throw exception;
      }

      request.getSession().setAttribute(SESSION_ATTRIBUTE, path);
      String query = UploadSizeErrorRoutes.redirectQuery(path, request.getQueryString());
      String redirect = request.getContextPath() + path + (query == null ? "" : "?" + query);
      response.sendRedirect(response.encodeRedirectURL(redirect));
    }
  }

  private static boolean isSizeError(Throwable exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof SizeLimitExceededException
          || cause instanceof FileSizeLimitExceededException) {
        return true;
      }
    }
    return false;
  }
}
