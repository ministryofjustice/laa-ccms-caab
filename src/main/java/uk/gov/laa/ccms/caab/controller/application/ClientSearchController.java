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
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes("applicationDetails")
public class ClientSearchController {

    private final DataService dataService;

    private final ClientSearchDetailsValidator clientSearchDetailsValidator;

    @ModelAttribute("clientSearchDetails")
    public ClientSearchDetails getClientSearchDetails() {
        return new ClientSearchDetails();
    }

    @GetMapping("/application/client-search")
    public String clientSearch(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                               Model model) {
        log.info("GET /application/client-search: " + applicationDetails.toString());

        model.addAttribute("clientSearchDetails", getClientSearchDetails());

        List<CommonLookupValueDetail> genders = dataService.getGenders();
        model.addAttribute("genders", genders);

        List<CommonLookupValueDetail> uniqueIdentifierTypes = dataService.getUniqueIdentifierTypes();
        model.addAttribute("uniqueIdentifierTypes", uniqueIdentifierTypes);

        return "/application/client-search";
    }

    @PostMapping("/application/client-search")
    public String clientSearch(@ModelAttribute("clientSearchDetails") ClientSearchDetails clientSearchDetails,
                                  BindingResult bindingResult,
                                  Model model) {
        log.info("POST /application/client-search: " + clientSearchDetails.toString());

        clientSearchDetailsValidator.validate(clientSearchDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            List<CommonLookupValueDetail> genders = dataService.getGenders();
            model.addAttribute("genders", genders);

            List<CommonLookupValueDetail> uniqueIdentifierTypes = dataService.getUniqueIdentifierTypes();
            model.addAttribute("uniqueIdentifierTypes", uniqueIdentifierTypes);
            return "/application/client-search";
        }

        return "redirect:/application/client-search/results";
    }
}
