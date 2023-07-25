package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {"applicationDetails", "clientSearchCriteria"})
public class ClientSearchController {

    private final DataService dataService;

    private final ClientSearchCriteriaValidator clientSearchCriteriaValidator;

    @ModelAttribute("clientSearchCriteria")
    public ClientSearchCriteria getClientSearchDetails() {
        return new ClientSearchCriteria();
    }

    @GetMapping("/application/client-search")
    public String clientSearch(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                               @ModelAttribute("clientSearchCriteria") ClientSearchCriteria clientSearchCriteria,
                               Model model) {
        log.info("GET /application/client-search: " + clientSearchCriteria.toString());

        populateDropdowns(model);
        return "/application/application-client-search";
    }

    @PostMapping("/application/client-search")
    public String clientSearch(@ModelAttribute("clientSearchCriteria") ClientSearchCriteria clientSearchCriteria,
                               BindingResult bindingResult,
                               Model model) {
        log.info("POST /application/client-search: " + clientSearchCriteria.toString());

        clientSearchCriteriaValidator.validate(clientSearchCriteria, bindingResult);

        if (bindingResult.hasErrors()) {
            populateDropdowns(model);
            return "/application/application-client-search";
        }

        return "redirect:/application/client-search/results";
    }

    private void populateDropdowns(Model model){
        List<CommonLookupValueDetail> genders = dataService.getGenders();
        model.addAttribute("genders", genders);

        List<CommonLookupValueDetail> uniqueIdentifierTypes = dataService.getUniqueIdentifierTypes();
        model.addAttribute("uniqueIdentifierTypes", uniqueIdentifierTypes);
    }
}
