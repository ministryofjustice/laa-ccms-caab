package uk.gov.laa.ccms.caab.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.controller.HomeController;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.security.CcmsOidcUser;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {OidcPrincipalControllerAdvice.class})
class OidcPrincipalControllerAdviceTest {

  @MockitoBean private UserService userService;
  @MockitoBean private SecurityEventLogger securityEventLogger;

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

    @Test
    @DisplayName(
        "Reloading the user on the home page replaces their functions in the logged-in "
            + "authentication and keeps their other authorities")
    public void homePageReloadRefreshesUserFunctions() {
      UserDetail previousUser = new UserDetail().loginId("test").functions(List.of("CA", "NOT"));
      UserDetail reloadedUser = new UserDetail().loginId("test").functions(List.of("NOT", "VC"));
      when(session.getAttribute("user")).thenReturn(previousUser);
      when(userService.getUserByLoginId(any())).thenReturn(Mono.just(reloadedUser));
      CcmsOidcUser principal =
          new CcmsOidcUser(
              oidcUser,
              "test",
              List.of(
                  new SimpleGrantedAuthority("group1"),
                  new SimpleGrantedAuthority("CA"),
                  new SimpleGrantedAuthority("NOT")));
      OAuth2AuthenticationToken token =
          new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "silas-identity");

      HandlerMethod handler = mock(HandlerMethod.class);
      when(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
          .thenReturn(handler);
      doReturn(HomeController.class).when(handler).getBeanType();

      try {
        advice.addOidcPrincipalToModel(token, model, session, request);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication().getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("group1", "NOT", "VC");
        verify(session)
            .setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
      } finally {
        SecurityContextHolder.clearContext();
      }
    }

    @Test
    @DisplayName("Functions are compared with the session copy when the home page reloads the user")
    public void privilegesComparedWhenHomePageReloadsTheUser() {
      UserDetail sessionUser = new UserDetail();
      sessionUser.setLoginId("test");
      sessionUser.setFunctions(List.of("VIEW"));
      userDetails.setFunctions(List.of("VIEW", "DELETE"));
      when(session.getAttribute("user")).thenReturn(sessionUser);
      HandlerMethod handler = mock(HandlerMethod.class);
      when(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
          .thenReturn(handler);
      doReturn(HomeController.class).when(handler).getBeanType();

      try {
        advice.addOidcPrincipalToModel(authentication, model, session, request);

        verify(securityEventLogger).privilegesReloaded(List.of("VIEW"), List.of("VIEW", "DELETE"));
      } finally {
        SecurityContextHolder.clearContext();
      }
    }

    @ParameterizedTest
    @CsvSource({"1, 2, true", "1, 1, false"})
    @DisplayName("A provider changed elsewhere drops the open case when Home reloads the user")
    public void providerChangedElsewhereDropsTheOpenCase(
        int sessionProvider, int reloadedProvider, boolean cleared) {
      UserDetail sessionUser = new UserDetail();
      sessionUser.setLoginId("test");
      sessionUser.setProvider(new BaseProvider().id(sessionProvider));
      userDetails.setProvider(new BaseProvider().id(reloadedProvider));
      when(session.getAttribute("user")).thenReturn(sessionUser);
      when(session.getAttributeNames())
          .thenReturn(Collections.enumeration(List.of(ACTIVE_CASE, APPLICATION_ID, "user")));
      HandlerMethod handler = mock(HandlerMethod.class);
      when(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
          .thenReturn(handler);
      doReturn(HomeController.class).when(handler).getBeanType();

      try {
        advice.addOidcPrincipalToModel(authentication, model, session, request);

        verify(session, times(cleared ? 1 : 0)).removeAttribute(ACTIVE_CASE);
        verify(session, times(cleared ? 1 : 0)).removeAttribute(APPLICATION_ID);
        verify(session, never()).removeAttribute("user");
      } finally {
        SecurityContextHolder.clearContext();
      }
    }

    @Test
    @DisplayName("Functions are not compared when the user is taken from the session")
    public void privilegesNotComparedWhenUserComesFromTheSession() {
      when(session.getAttribute("user")).thenReturn(userDetails);

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verifyNoInteractions(securityEventLogger);
    }

    @Test
    @DisplayName("A first load of the user leaves the login authentication alone")
    public void firstLoadDoesNotReplaceAuthentication() {
      when(session.getAttribute("user")).thenReturn(null);

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verify(session, never())
          .setAttribute(
              eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
    }

    @Test
    @DisplayName("Functions are not compared on the first load, when the session has no user")
    public void privilegesNotComparedOnTheFirstLoad() {
      when(session.getAttribute("user")).thenReturn(null);

      advice.addOidcPrincipalToModel(authentication, model, session, request);

      verifyNoInteractions(securityEventLogger);
    }

    @Test
    @DisplayName("An empty user reload drops the logged-in authentication and fails")
    public void emptyReloadEndsTheLogin() {
      when(session.getAttribute("user")).thenReturn(userDetails);
      when(userService.getUserByLoginId(any())).thenReturn(Mono.empty());
      HandlerMethod handler = mock(HandlerMethod.class);
      when(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
          .thenReturn(handler);
      doReturn(HomeController.class).when(handler).getBeanType();
      SecurityContextHolder.getContext().setAuthentication(authentication);

      try {
        assertThrows(
            CaabApplicationException.class,
            () -> advice.addOidcPrincipalToModel(authentication, model, session, request));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(session)
            .removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        verify(session).removeAttribute("user");
      } finally {
        SecurityContextHolder.clearContext();
      }
    }

    @Test
    @DisplayName("A failed user reload drops the logged-in authentication and fails")
    public void failedReloadEndsTheLogin() {
      when(session.getAttribute("user")).thenReturn(userDetails);
      when(userService.getUserByLoginId(any()))
          .thenReturn(Mono.error(new RuntimeException("User service unavailable")));
      HandlerMethod handler = mock(HandlerMethod.class);
      when(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
          .thenReturn(handler);
      doReturn(HomeController.class).when(handler).getBeanType();
      SecurityContextHolder.getContext().setAuthentication(authentication);

      try {
        assertThrows(
            CaabApplicationException.class,
            () -> advice.addOidcPrincipalToModel(authentication, model, session, request));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(session)
            .removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        verify(session).removeAttribute("user");
      } finally {
        SecurityContextHolder.clearContext();
      }
    }
  }
}
