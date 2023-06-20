package uk.gov.laa.ccms.caab.advice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SamlPrincipalControllerAdviceTest {

    @Test
    public void addSamlPrincipalToModelTest() {
        // Arrange
        Saml2AuthenticatedPrincipal principal = mock(Saml2AuthenticatedPrincipal.class);
        Model model = mock(Model.class);
        SamlPrincipalControllerAdvice advice = new SamlPrincipalControllerAdvice();

        // Act
        advice.addSamlPrincipalToModel(principal, model);

        // Assert
        verify(model).addAttribute(eq("user"), any());
        verify(model).addAttribute(eq("userAttributes"), any());
        verifyNoMoreInteractions(model);

        if (principal != null) {
            verify(principal).getName();
        }
    }

}