package uk.gov.laa.ccms.caab.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
@SpringBootTest(classes = {SamlPrincipalControllerAdvice.class})
class SamlPrincipalControllerAdviceTest {

  @MockitoBean private UserService userService;

  @MockitoBean private HttpSession session;

  @MockitoBean private Model model;

  @MockitoBean private HttpServletRequest request;

  @Autowired private SamlPrincipalControllerAdvice advice;

  @MockitoBean private Saml2AssertionAuthentication authentication;

  @MockitoBean private Saml2ResponseAssertionAccessor accessor;

  private UserDetail userDetails;

  private final Map<String, List<Object>> attributes =
      Map.of("groups", List.of("attribute1", "attribute2"));

  @BeforeEach
  public void setUp() {
    userDetails = new UserDetail();
    userDetails.setLoginId("test");
    when(authentication.getCredentials()).thenReturn(accessor);
    when(accessor.getAttributes()).thenReturn(attributes);
    when(userService.getUser(any())).thenReturn(Mono.just(userDetails));
    when(userService.getUserByLoginId(any())).thenReturn(Mono.just(userDetails));
    when(authentication.getName()).thenReturn("test");
  }

  @Nested
  class AddSamlPrincipalToModelTests {

    @Test
    @DisplayName("User details are added to model from session when matching user in session")
    public void whenPrincipalNotNullAndSessionContainsUser() {
      when(session.getAttribute("user")).thenReturn(userDetails);

      advice.addSamlPrincipalToModel(authentication, model, session, request);

      verifyNoInteractions(userService);
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", attributes);
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

      advice.addSamlPrincipalToModel(authentication, model, session, request);

      verify(userService).getUser(any());
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", attributes);
      verify(session).setAttribute("user", userDetails);
      verifyNoMoreInteractions(model);
    }

    @Test
    @DisplayName(
        "User details are retrieved from database and added to model when session missing user")
    public void userDetailsRetrievedAndAddedWhenPrincipalNotNullAndSessionDoesNotContainUser() {
      when(session.getAttribute("user")).thenReturn(null);

      advice.addSamlPrincipalToModel(authentication, model, session, request);

      verify(userService).getUserByLoginId(any());
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", attributes);
      verify(session).setAttribute("user", userDetails);
      verifyNoMoreInteractions(model);
    }

    @Test
    @DisplayName("User details are not retrieved or added to model when authentication is missing")
    public void userDetailsNotRetrievedOrAddedWhenPrincipalIsNull() {
      advice.addSamlPrincipalToModel(null, model, session, request);

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

      advice.addSamlPrincipalToModel(authentication, model, session, request);

      verify(userService).getUserByLoginId(any());
      verify(model).addAttribute("user", userDetails);
      verify(model).addAttribute("userAttributes", attributes);
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
      doReturn(
              List.of(
                  new SimpleGrantedAuthority("group1"),
                  new SimpleGrantedAuthority("CA"),
                  new SimpleGrantedAuthority("NOT")))
          .when(authentication)
          .getAuthorities();
      when(authentication.getPrincipal()).thenReturn("test");
      when(authentication.getRelyingPartyRegistrationId()).thenReturn("idp");

      HandlerMethod handler = mock(HandlerMethod.class);
      when(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
          .thenReturn(handler);
      doReturn(HomeController.class).when(handler).getBeanType();

      try {
        advice.addSamlPrincipalToModel(authentication, model, session, request);

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
    @DisplayName("A first load of the user leaves the login authentication alone")
    public void firstLoadDoesNotReplaceAuthentication() {
      when(session.getAttribute("user")).thenReturn(null);

      advice.addSamlPrincipalToModel(authentication, model, session, request);

      verify(session, never())
          .setAttribute(
              eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
    }
  }
}
