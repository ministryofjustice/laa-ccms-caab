package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/** Returns multipart size failures raised during CSRF processing to the upload form. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadSizeErrorFilter extends OncePerRequestFilter {

  static final String ERROR_ATTRIBUTE = UploadSizeErrorFilter.class.getName() + ".error";
  private static final String TOKEN_PARAMETER = "uploadSizeErrorToken";
  private static final FlashMapManager FLASH_MAP_MANAGER = new SessionFlashMapManager();

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

      redirectToUploadForm(request, response);
    }
  }

  /** Uses the same form recovery for size failures raised before or inside MVC. */
  public static void redirectToUploadForm(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (UploadSizeErrorRoutes.formName(path) == null) {
      throw new IllegalArgumentException("Not an upload path: " + path);
    }
    String query = UploadSizeErrorRoutes.redirectQuery(path, request.getQueryString());
    String token = UUID.randomUUID().toString();
    String targetPath = request.getContextPath() + path;
    FlashMap flashMap = new FlashMap();
    flashMap.put(ERROR_ATTRIBUTE, Boolean.TRUE);
    flashMap.setTargetRequestPath(targetPath);
    flashMap.addTargetRequestParam(TOKEN_PARAMETER, token);
    FlashMapManager manager = RequestContextUtils.getFlashMapManager(request);
    (manager != null ? manager : FLASH_MAP_MANAGER).saveOutputFlashMap(flashMap, request, response);
    String redirect =
        targetPath
            + (query == null || query.isEmpty() ? "?" : "?" + query + "&")
            + TOKEN_PARAMETER
            + "="
            + token;
    response.sendRedirect(response.encodeRedirectURL(redirect));
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
