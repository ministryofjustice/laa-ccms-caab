package uk.gov.laa.ccms.caab.controller.application;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {"applicationDetails", "clientSearchCriteria", "clientSearchResults"})
public class ClientSearchResultsController {

    private final SoaGatewayService soaGatewayService;

    @GetMapping("/application/client-search/results")
    public String clientSearchResults(@RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "10") int size,
                                      @ModelAttribute("clientSearchCriteria") ClientSearchCriteria clientSearchCriteria,
                                      @SessionAttribute("user") UserDetail user,
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
            model.addAttribute("clientSearchResults", clientSearchResults);
            return "/application/application-client-search-results";
        } else {
            return "/application/application-client-search-no-results";
        }
    }

    @PostMapping("/application/client-search/results")
    public String clientSearch(@ModelAttribute("clientSearchResults") ClientDetails clientSearchResults) {
        log.info("POST /application/client-search/results");

        return "redirect:/application/client-search/results";
    }
}

