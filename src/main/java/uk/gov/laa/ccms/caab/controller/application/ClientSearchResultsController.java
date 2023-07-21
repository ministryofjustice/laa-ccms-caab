package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {"applicationDetails", "clientSearchDetails"})
public class ClientSearchResultsController {

    private final SoaGatewayService soaGatewayService;

    @GetMapping("/application/client-search/results")
    public String clientSearchResults(@ModelAttribute("clientSearchResults") ClientDetails clientSearchResults,
                                      @ModelAttribute("clientSearchDetails") ClientSearchDetails clientSearchDetail) {
        log.info("GET /application/client-search/results: " + clientSearchResults.toString());

        return "/application/application-client-search-results";
    }

    @GetMapping("/application/client-search/no-results")
    public String clientSearch(@ModelAttribute("clientSearchDetails") ClientSearchDetails clientSearchDetails) {
        log.info("GET /application/client-search/no-results: " + clientSearchDetails.toString());

        return "/application/client-search-no-results";
    }
}

