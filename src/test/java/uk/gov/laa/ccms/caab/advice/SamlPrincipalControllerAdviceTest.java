package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.UserDetail;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SamlPrincipalControllerAdviceTest {

    @Mock
    private DataService dataService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    private SamlPrincipalControllerAdvice advice;
    private Saml2AuthenticatedPrincipal principal;
    private UserDetail userDetails;

    @BeforeEach
    public void setUp() {
        principal = mock(Saml2AuthenticatedPrincipal.class);
        advice = new SamlPrincipalControllerAdvice(dataService);
        userDetails = new UserDetail();
        userDetails.setLoginId("test");
        when(dataService.getUser(any())).thenReturn(Mono.just(userDetails));
        when(principal.getName()).thenReturn("test");
    }

    @Test
    public void addSamlPrincipalToModelTest_WhenPrincipalNotNullAndSessionContainsUser() {
        when(session.getAttribute("user")).thenReturn(userDetails);

        advice.addSamlPrincipalToModel(principal, model, session);

        verify(model).addAttribute("user", userDetails);
        verify(model).addAttribute("userAttributes", principal.getAttributes());
        verify(session).setAttribute("user", userDetails);
        verifyNoMoreInteractions(model);
    }

    @Test
    public void addSamlPrincipalToModelTest_WhenPrincipalNotNullAndSessionContainsUserWithDifferentLoginId() {
        UserDetail sessionUser = new UserDetail();
        sessionUser.setLoginId("different");
        when(session.getAttribute("user")).thenReturn(sessionUser);

        advice.addSamlPrincipalToModel(principal, model, session);

        verify(dataService).getUser(any());
        verify(model).addAttribute("user", userDetails);
        verify(model).addAttribute("userAttributes", principal.getAttributes());
        verify(session).setAttribute("user", userDetails);
        verifyNoMoreInteractions(model);
    }

    @Test
    public void addSamlPrincipalToModelTest_WhenPrincipalNotNullAndSessionDoesNotContainUser() {
        when(session.getAttribute("user")).thenReturn(null);

        advice.addSamlPrincipalToModel(principal, model, session);

        verify(dataService).getUser(any());
        verify(model).addAttribute("user", userDetails);
        verify(model).addAttribute("userAttributes", principal.getAttributes());
        verify(session).setAttribute("user", userDetails);
        verifyNoMoreInteractions(model);
    }

    @Test
    public void addSamlPrincipalToModelTest_WhenPrincipalIsNull() {
        advice.addSamlPrincipalToModel(null, model, session);

        verifyNoInteractions(dataService);
        verifyNoInteractions(model);
        verifyNoInteractions(session);
    }
}






