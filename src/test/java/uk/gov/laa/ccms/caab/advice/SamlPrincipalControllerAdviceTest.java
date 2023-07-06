package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.UserDetails;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SamlPrincipalControllerAdviceTest {

    @Mock
    private DataService dataService;

    @Mock
    private HttpSession session;

    @Test
    public void addSamlPrincipalToModelTest() {
        // Arrange
        Saml2AuthenticatedPrincipal principal = mock(Saml2AuthenticatedPrincipal.class);
        Model model = mock(Model.class);
        SamlPrincipalControllerAdvice advice = new SamlPrincipalControllerAdvice(dataService);
        UserDetails userDetails = new UserDetails();
        userDetails.setLoginId("test");
        when(dataService.getUser(any())).thenReturn(Mono.just(userDetails));
        when(principal.getName()).thenReturn("test");

        // Act
        advice.addSamlPrincipalToModel(principal, model, session);

        // Assert
        verify(model).addAttribute(eq("user"), any());
        verify(model).addAttribute(eq("userAttributes"), any());
        verifyNoMoreInteractions(model);

        if (principal != null) {
            verify(principal).getName();
        }
    }

}