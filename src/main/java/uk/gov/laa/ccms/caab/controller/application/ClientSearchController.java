package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {"applicationDetails", "clientSearchDetails"})
public class ClientSearchController {

    private final DataService dataService;

    private final SoaGatewayService soaGatewayService;

    private final ClientSearchDetailsValidator clientSearchDetailsValidator;

    @ModelAttribute("clientSearchDetails")
    public ClientSearchDetails getClientSearchDetails() {
        return new ClientSearchDetails();
    }

    @GetMapping("/application/client-search")
    public String clientSearch(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                               @ModelAttribute("clientSearchDetails") ClientSearchDetails clientSearchDetails,
                               Model model) {
        log.info("GET /application/client-search: " + clientSearchDetails.toString());

        populateDropdowns(model);
        return "/application/application-client-search";
    }

    @PostMapping("/application/client-search")
    public String clientSearch(@ModelAttribute("clientSearchDetails") ClientSearchDetails clientSearchDetails,
                               @SessionAttribute("user") UserDetail user,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        log.info("POST /application/client-search: " + clientSearchDetails.toString());

        clientSearchDetailsValidator.validate(clientSearchDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            populateDropdowns(model);
            return "/application/application-client-search";
        }

        ClientDetails clientSearchResults = soaGatewayService.getClients(clientSearchDetails, user.getLoginId(),
                user.getUserType()).block();

        if (clientSearchResults != null && clientSearchResults.getClients() != null){
            redirectAttributes.addFlashAttribute("clientSearchResults", clientSearchResults);
            return "redirect:/application/client-search/results";
        } else {
            return "redirect:/application/client-search/no-results";
        }

    }

    private void populateDropdowns(Model model){
        List<CommonLookupValueDetail> genders = dataService.getGenders();
        model.addAttribute("genders", genders);

        List<CommonLookupValueDetail> uniqueIdentifierTypes = dataService.getUniqueIdentifierTypes();
        model.addAttribute("uniqueIdentifierTypes", uniqueIdentifierTypes);
    }
}
