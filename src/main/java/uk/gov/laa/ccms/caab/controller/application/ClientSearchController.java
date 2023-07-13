package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetails;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes("applicationDetails")
public class ClientSearchController {

    private final DataService dataService;

    @ModelAttribute("clientSearchDetails")
    public ClientSearchDetails getClientSearchDetails() {
        return new ClientSearchDetails();
    }

    @GetMapping("/application/client-search")
    public String clientSearch(Model model) {
        log.info("GET /application/client-search");

        model.addAttribute("clientSearchDetails", getClientSearchDetails());

        List<CommonLookupValueDetails> genders = dataService.getGenders();
        model.addAttribute("genders", genders);

        List<CommonLookupValueDetails> uniqueIdentifierTypes = dataService.getUniqueIdentifierTypes();
        model.addAttribute("uniqueIdentifierTypes", uniqueIdentifierTypes);

        return "/application/client-search";
    }

    @PostMapping("/application/client-search")
    public String clientSearch(@ModelAttribute("clientSearchDetails") ClientSearchDetails clientSearchDetails,
                                  BindingResult bindingResult,
                                  Model model) {
        log.info("POST /application/client-search: " + clientSearchDetails.toString());

//        applicationValidator.validateApplicationType(applicationDetails, bindingResult);
//
//        if (bindingResult.hasErrors()) {
//            List<CommonLookupValueDetails> applicationTypes = dataService.getApplicationTypes();
//            model.addAttribute("applicationTypes", applicationTypes);
//            return "/application/select-application-type";
//        }

        return "redirect:/application/client-search/results";
    }
}
