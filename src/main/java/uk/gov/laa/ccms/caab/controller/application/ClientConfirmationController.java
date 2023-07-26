package uk.gov.laa.ccms.caab.controller.application;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_RESULTS;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ClientConfirmationController {

    @GetMapping("/application/client/{id}/confirm")
    public String clientConfirm(@SessionAttribute(CLIENT_SEARCH_RESULTS) ClientDetails clientSearchResults,
                                @PathVariable("id") int id,
                                Model model) {
        log.info("GET /application/client/{}/confirm", id);

        ClientDetail clientInformation = clientSearchResults.getContent().get(id);
        model.addAttribute("clientInformation", clientInformation);
        model.addAttribute("confirmedClientId", id);

        return "/application/application-client-confirmation";
    }

    @PostMapping("/application/client/confirmed")
    public String clientConfirmed(int confirmedClientId,
                                  @SessionAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                                  @SessionAttribute(CLIENT_SEARCH_RESULTS) ClientDetails clientSearchResults) {
        log.info("POST /application/client/confirmed: {}", applicationDetails);

        ClientDetail confirmedClient = clientSearchResults.getContent().get(confirmedClientId);

        applicationDetails.setClient(confirmedClient);
        log.info("Application details: {}", applicationDetails);

        return "redirect:/application/agreement";
    }
}
