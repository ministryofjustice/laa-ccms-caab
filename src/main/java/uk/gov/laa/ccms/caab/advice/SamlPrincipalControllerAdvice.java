package uk.gov.laa.ccms.caab.advice;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.data.model.UserDetails;

@ControllerAdvice
public class SamlPrincipalControllerAdvice {

    @ModelAttribute
    public void addSamlPrincipalToModel(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal, Model model) {

        if (principal != null) {

            UserDetails user = new UserDetails();
            user.setLoginId(principal.getName());

            model.addAttribute("user", user);
            model.addAttribute("userAttributes", principal.getAttributes());
        }
    }
}
