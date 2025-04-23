package uk.gov.laa.ccms.caab.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.laa.ccms.caab.constants.SessionConstants;
import uk.gov.laa.ccms.data.model.UserDetails;

@ExtendWith(SpringExtension.class)
class FeatureDisabledExceptionHandlerTest {

  FeatureDisabledExceptionHandler exceptionHandler;

  private static final String PUI_HOME_URL_ATTRIBUTE = "puiHomeUrl";
  private static final String FEATURE_ATTRIBUTE = "feature";

  @BeforeEach
  void setUp() {
    exceptionHandler = new FeatureDisabledExceptionHandler("test home url");
  }

  @Test
  @DisplayName(
      "resolveException correctly handles FeatureDisabledException and returns the correct view")
  void resolveExceptionHandlesFeatureDisabledException() {

    String featureName = "a feature";

    Feature feature = mock(Feature.class);

    when(feature.getName()).thenReturn(featureName);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    UserDetails userDetails = new UserDetails();

    when(request.getSession()).thenReturn(session);
    when(session.getAttribute(SessionConstants.USER_DETAILS)).thenReturn(userDetails);

    ModelAndView modelAndView =
        exceptionHandler.resolveException(
            request,
            mock(HttpServletResponse.class),
            new Object(),
            new FeatureDisabledException(feature));

    assertEquals("feature-unavailable", modelAndView.getViewName());
    assertEquals("test home url", modelAndView.getModel().get(PUI_HOME_URL_ATTRIBUTE));
    assertEquals(featureName, modelAndView.getModel().get(FEATURE_ATTRIBUTE));
    assertEquals(userDetails, modelAndView.getModel().get(SessionConstants.USER_DETAILS));
  }

  @Test
  @DisplayName(
      "resolveException ignores exceptions that are not instances of FeatureDisabledException")
  void resolveExceptionIgnoresOtherExceptions() {

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    UserDetails userDetails = new UserDetails();

    when(request.getSession()).thenReturn(session);
    when(session.getAttribute(SessionConstants.USER_DETAILS)).thenReturn(userDetails);

    ModelAndView modelAndView =
        exceptionHandler.resolveException(
            request, mock(HttpServletResponse.class), new Object(), new Exception());

    assertNull(modelAndView);
  }
}
