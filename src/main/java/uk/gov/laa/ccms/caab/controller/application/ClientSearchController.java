package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

import java.util.List;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {APPLICATION_DETAILS, CLIENT_SEARCH_CRITERIA})
public class ClientSearchController {

    private final DataService dataService;

    private final ClientSearchCriteriaValidator clientSearchCriteriaValidator;

    @ModelAttribute(CLIENT_SEARCH_CRITERIA)
    public ClientSearchCriteria getClientSearchDetails() {
        return new ClientSearchCriteria();
    }

    @GetMapping("/application/client/search")
    public String clientSearch(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                               @ModelAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
                               Model model) {
        log.info("GET /application/client/search: {}", clientSearchCriteria);

        populateDropdowns(model);
        return "/application/application-client-search";
    }

    @PostMapping("/application/client/search")
    public String clientSearch(@ModelAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
                               BindingResult bindingResult,
                               Model model) {
        log.info("POST /application/client/search: {}", clientSearchCriteria);

        clientSearchCriteriaValidator.validate(clientSearchCriteria, bindingResult);

        if (bindingResult.hasErrors()) {
            populateDropdowns(model);
            return "/application/application-client-search";
        }

        return "redirect:/application/client/results";
    }

    private void populateDropdowns(Model model){
        List<CommonLookupValueDetail> genders = dataService.getGenders();
        model.addAttribute("genders", genders);

        List<CommonLookupValueDetail> uniqueIdentifierTypes = dataService.getUniqueIdentifierTypes();
        model.addAttribute("uniqueIdentifierTypes", uniqueIdentifierTypes);
    }
}
