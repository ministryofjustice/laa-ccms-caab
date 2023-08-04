package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class DelegatedFunctionsController {

    private final ApplicationDetailsValidator applicationValidator;

    @GetMapping("/application/delegated-functions")
    public String delegatedFunction(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails){
        log.info("GET /application/delegated-functions: {}", applicationDetails);

        return "/application/select-delegated-functions";
    }

    @PostMapping("/application/delegated-functions")
    public String delegatedFunction(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                                    BindingResult bindingResult) {
        log.info("POST /application/delegated-functions: {}", applicationDetails);
        applicationValidator.validateDelegatedFunction(applicationDetails, bindingResult);

        if (!applicationDetails.isDelegatedFunctions()){
            applicationDetails.setDelegatedFunctionUsedDay(null);
            applicationDetails.setDelegatedFunctionUsedMonth(null);
            applicationDetails.setDelegatedFunctionUsedYear(null);
        }

        if (bindingResult.hasErrors()) {
            return "/application/select-delegated-functions";
        }

        applicationDetails.setApplicationTypeAndDisplayValues();

        return "redirect:/application/client-search";
    }

}
