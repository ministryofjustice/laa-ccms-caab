package uk.gov.laa.ccms.caab.controller.application;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;


@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {"applicationDetails"})
public class ClientConfirmationController {

    @GetMapping("/application/client/{id}/confirm")
    public String clientConfirm(@SessionAttribute("clientSearchResults") ClientDetails clientSearchResults,
                                @PathVariable("id") int id,
                                Model model) {
        log.info("GET /application/client/{}/confirm", id);

        ClientDetail clientInformation = clientSearchResults.getContent().get(id);
        model.addAttribute("clientInformation", clientInformation);

        log.info("Client info: "+ clientInformation);

        return "/application/application-client-confirmation";
    }

    @PostMapping("/application/client/confirmed")
    public String clientConfirmed(@ModelAttribute("clientInformation") ClientDetail clientInformation,
                                  HttpSession session) {
        log.info("POST /application/client/confirmed");

        session.setAttribute("applicationClient", clientInformation);
        log.info("Application Client: "+ clientInformation);

        return "redirect:TODO";
    }
}
