package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ControllerAdvice
@RequiredArgsConstructor
public class SamlPrincipalControllerAdvice {

    private final DataService dataService;

    @ModelAttribute
    public void addSamlPrincipalToModel(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal, Model model, HttpSession session) {

        if (principal != null) {

            UserDetail user = new UserDetail();
            user.setLoginId(principal.getName());

            if (session.getAttribute("user") != null){
                user = (UserDetail) session.getAttribute("user");

                if (!user.getLoginId().equals(principal.getName())){
                    user = dataService.getUser(user.getLoginId()).block();
                }

            } else {
                user = dataService.getUser(user.getLoginId()).block();
            }

            model.addAttribute("user", user);
            model.addAttribute("userAttributes", principal.getAttributes());

            session.setAttribute("user", user);
        }
    }

}
