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
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.*;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes("applicationDetails")
public class DelegatedFunctionsController {

    private final ApplicationDetailsValidator applicationValidator;

    @GetMapping("/application/delegated-functions")
    public String delegatedFunction(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                    Model model){
        log.info("GET /application/delegated-functions: " + applicationDetails.toString());
        return "/application/select-delegated-functions";
    }

    @PostMapping("/application/delegated-functions")
    public String delegatedFunction(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                    BindingResult bindingResult,
                                    Model model) {
        log.info("POST /application/delegated-functions: " + applicationDetails.toString());
        applicationValidator.validateDelegatedFunction(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            return "/application/select-delegated-functions";
        }

        String applicationTypeId;
        if (APP_TYPE_SUBSTANTIVE.equals(applicationDetails.getApplicationTypeCategory())) {
            applicationTypeId = applicationDetails.getDelegatedFunctionsOption().equals("Y")
                    ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS
                    : APP_TYPE_SUBSTANTIVE;
        } else {
            applicationTypeId = applicationDetails.getDelegatedFunctionsOption().equals("Y")
                    ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS
                    : APP_TYPE_EMERGENCY;
        }
        applicationDetails.setApplicationTypeId(applicationTypeId);

        return "redirect:/application/client-search";
    }

}
