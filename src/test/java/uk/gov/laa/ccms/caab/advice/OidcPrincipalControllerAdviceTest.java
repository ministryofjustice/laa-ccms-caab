package uk.gov.laa.ccms.caab.advice;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.controller.HomeController;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {OidcPrincipalControllerAdvice.class})
class OidcPrincipalControllerAdviceTest {

  @MockitoBean private UserService userService;

  @MockitoBean private HttpSession session;

  @MockitoBean private Model model;

  @MockitoBean private HttpServletRequest request;

  @Autowired private OidcPrincipalControllerAdvice advice;

  @MockitoBean private Authentication authentication;

  @MockitoBean private OidcUser oidcUser;

  private UserDetail userDetails;

  private final Map<String, Object> claims = Map.of("groups", List.of("attribute1", "attribute2"));

  @BeforeEach
  public void setUp() {
    userDetails = new UserDetail();
    userDetails.setLoginId("test");
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(oidcUser.getClaims()).thenReturn(claims);
    when(userService.getUser(any())).thenReturn(Mono.just(userDetails));
    when(userService.getUserByLoginId(any())).thenReturn(Mono.just(userDetails));
    when(authentication.getName()).thenReturn("test");
  }

  @Nested
  class AddOidcPrincipalToModelTests {

    @Test
    @DisplayName("User details are added to model from session when matching user in session")
    public void whenPrincipalNotNullAndSessionContainsUser() {
      when(session.getAttribute("user")).thenReturn(userDetails);

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verifyNoInteractions(userService);
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", claims);
      verify(session).setAttribute("user", userDetails);
      verifyNoMoreInteractions(model);
    }

    @Test
    @DisplayName(
        "User details are retrieved from database and added to model when session contains "
            + "mismatched user")
    public void whenPrincipalNotNullAndSessionContainsUserWithDifferentLoginId() {
      UserDetail sessionUser = new UserDetail();
      sessionUser.setLoginId("different");
      when(session.getAttribute("user")).thenReturn(sessionUser);

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verify(userService).getUser(any());
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", claims);
      verify(session).setAttribute("user", userDetails);
      verifyNoMoreInteractions(model);
    }

    @Test
    @DisplayName(
        "User details are retrieved from database and added to model when session missing user")
    public void userDetailsRetrievedAndAddedWhenPrincipalNotNullAndSessionDoesNotContainUser() {
      when(session.getAttribute("user")).thenReturn(null);

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verify(userService).getUserByLoginId(any());
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", claims);
      verify(session).setAttribute("user", userDetails);
      verifyNoMoreInteractions(model);
    }

    @Test
    @DisplayName("User details are not retrieved or added to model when authentication is missing")
    public void userDetailsNotRetrievedOrAddedWhenPrincipalIsNull() {
      advice.addOidcPrincipalToModel(null, model, session, request);

      verifyNoInteractions(userService);
      verifyNoInteractions(model);
      verifyNoInteractions(session);
    }

    @Test
    @DisplayName(
        "User details are not retrieved or added to model when principal is not an OidcUser")
    public void userDetailsNotRetrievedOrAddedWhenPrincipalIsNotOidcUser() {
      when(authentication.getPrincipal()).thenReturn("not-an-oidc-user");

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verifyNoInteractions(userService);
      verifyNoInteractions(model);
      verifyNoInteractions(session);
    }

    @Test
    @DisplayName(
        "User details are always retrieved from database when home page is requested, "
            + "even with matching user in session")
    public void userDetailsRetrievedWhenHomePageRequested() {
      when(session.getAttribute("user")).thenReturn(userDetails);

      HandlerMethod handler = mock(HandlerMethod.class);
      when(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
          .thenReturn(handler);
      doReturn(HomeController.class).when(handler).getBeanType();

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verify(userService).getUserByLoginId(any());
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", claims);
      verify(session).setAttribute("user", userDetails);
      verifyNoMoreInteractions(model);
    }
  }
}
