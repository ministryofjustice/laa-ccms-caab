package uk.gov.laa.ccms.caab.feature;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.laa.ccms.caab.constants.SessionConstants;

/** Handler for {@link FeatureDisabledException}. */
@Component
public class FeatureDisabledExceptionHandler implements HandlerExceptionResolver {

  private final String puiHomeUrl;

  FeatureDisabledExceptionHandler(@Value("${laa.ccms.pui-home-url}") String puiHomeUrl) {
    this.puiHomeUrl = puiHomeUrl;
  }

  @Override
  public ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if (ex instanceof FeatureDisabledException exception) {
      ModelAndView modelAndView = new ModelAndView("feature-unavailable");
      modelAndView.addObject(
          SessionConstants.USER_DETAILS,
          request.getSession().getAttribute(SessionConstants.USER_DETAILS));
      modelAndView.addObject("puiHomeUrl", puiHomeUrl);
      modelAndView.addObject("feature", exception.getFeature().getName());
      return modelAndView;
    }

    // Let Spring handle the exception
    return null;
  }
}
