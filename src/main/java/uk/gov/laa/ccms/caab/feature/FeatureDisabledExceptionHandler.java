package uk.gov.laa.ccms.caab.feature;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.laa.ccms.caab.constants.SessionConstants;

/**
 * Handler for {@link FeatureDisabledException}.
 */
@Component
public class FeatureDisabledExceptionHandler implements HandlerExceptionResolver {

  @Override
  public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    if (ex instanceof FeatureDisabledException) {
      ModelAndView modelAndView = new ModelAndView("unsupported");
      modelAndView.addObject(SessionConstants.USER_DETAILS,
          request.getSession().getAttribute(SessionConstants.USER_DETAILS));
      modelAndView.addObject("message", ex.getLocalizedMessage());
      modelAndView.addObject("referer", request.getHeader("referer"));
      return modelAndView;
    }

    return null;
  }
}
