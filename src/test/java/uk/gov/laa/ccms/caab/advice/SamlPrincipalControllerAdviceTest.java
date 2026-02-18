package uk.gov.laa.ccms.caab.advice;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SamlPrincipalControllerAdvice.class})
class SamlPrincipalControllerAdviceTest {

  @MockitoBean private UserService userService;

  @MockitoBean private HttpSession session;

  @MockitoBean private Model model;

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
    when(authentication.getName()).thenReturn("test");
  }

  @Test
  public void addSamlPrincipalToModelTest_WhenPrincipalNotNullAndSessionContainsUser() {
    when(session.getAttribute("user")).thenReturn(userDetails);

    advice.addSamlPrincipalToModel(authentication, model, session);

    verify(model).addAttribute("user", userDetails);
    verify(model).addAttribute("userAttributes", attributes);
    verify(session).setAttribute("user", userDetails);
    verifyNoMoreInteractions(model);
  }

  @Test
  public void
      addSamlPrincipalToModelTest_WhenPrincipalNotNullAndSessionContainsUserWithDifferentLoginId() {
    UserDetail sessionUser = new UserDetail();
    sessionUser.setLoginId("different");
    when(session.getAttribute("user")).thenReturn(sessionUser);

    advice.addSamlPrincipalToModel(authentication, model, session);

    verify(userService).getUser(any());
    verify(model).addAttribute("user", userDetails);
    verify(model).addAttribute("userAttributes", attributes);
    verify(session).setAttribute("user", userDetails);
    verifyNoMoreInteractions(model);
  }

  @Test
  public void addSamlPrincipalToModelTest_WhenPrincipalNotNullAndSessionDoesNotContainUser() {
    when(session.getAttribute("user")).thenReturn(null);

    advice.addSamlPrincipalToModel(authentication, model, session);

    verify(userService).getUser(any());
    verify(model).addAttribute("user", userDetails);
    verify(model).addAttribute("userAttributes", attributes);
    verify(session).setAttribute("user", userDetails);
    verifyNoMoreInteractions(model);
  }

  @Test
  public void addSamlPrincipalToModelTest_WhenPrincipalIsNull() {
    advice.addSamlPrincipalToModel(null, model, session);

    verifyNoInteractions(userService);
    verifyNoInteractions(model);
    verifyNoInteractions(session);
  }
}
