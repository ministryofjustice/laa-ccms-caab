package uk.gov.laa.ccms.caab.controller.application;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.*;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {APPLICATION_DETAILS, CLIENT_SEARCH_CRITERIA, CLIENT_SEARCH_RESULTS})
public class ClientSearchResultsController {

    private final SoaGatewayService soaGatewayService;

    @GetMapping("/application/client-search/results")
    public String clientSearchResults(@RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "10") int size,
                                      @ModelAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
                                      @SessionAttribute(USER_DETAILS) UserDetail user,
                                      HttpServletRequest request,
                                      Model model) {
        log.info("GET /application/client-search/results");

        ClientDetails clientSearchResults = soaGatewayService.getClients(clientSearchCriteria, user.getLoginId(),
                user.getUserType(), page, size).block();

        if (clientSearchResults != null && clientSearchResults.getContent() != null && clientSearchResults.getTotalElements() > 0){
            if (clientSearchResults.getTotalElements() > 200){
                return "/application/application-client-search-many-results";
            }
            String currentUrl = request.getRequestURL().toString();
            model.addAttribute("currentUrl", currentUrl);
            model.addAttribute(CLIENT_SEARCH_RESULTS, clientSearchResults);
            return "/application/application-client-search-results";
        } else {
            return "/application/application-client-search-no-results";
        }
    }

    @PostMapping("/application/client-search/results")
    public String clientSearch(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                               @ModelAttribute(CLIENT_SEARCH_RESULTS) ClientDetails clientSearchResults) {
        log.info("POST /application/client-search/results");
        applicationDetails.setClient(null);
        applicationDetails.setAgreementAccepted(false);

        return "redirect:/application/agreement";
    }
}

