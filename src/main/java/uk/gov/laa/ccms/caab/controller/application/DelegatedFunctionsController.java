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

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes("applicationDetails")
public class DelegatedFunctionsController {

    private final ApplicationDetailsValidator applicationValidator;

    @GetMapping("/application/delegated-functions")
    public String delegatedFunction(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails){
        log.info("GET /application/delegated-functions: " + applicationDetails.toString());

        if (applicationDetails.getDelegatedFunctionsOption() == null){
            applicationDetails.setDelegatedFunctionsOption("N");
        }

        return "/application/select-delegated-functions";
    }

    @PostMapping("/application/delegated-functions")
    public String delegatedFunction(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                    BindingResult bindingResult) {
        log.info("POST /application/delegated-functions: " + applicationDetails.toString());
        applicationValidator.validateDelegatedFunction(applicationDetails, bindingResult);

        if (applicationDetails.getDelegatedFunctionsOption().equals("N")){
            applicationDetails.setDelegatedFunctionUsedDay(null);
            applicationDetails.setDelegatedFunctionUsedMonth(null);
            applicationDetails.setDelegatedFunctionUsedYear(null);
        }

        if (bindingResult.hasErrors()) {
            return "/application/select-delegated-functions";
        }

        return "redirect:/application/client-search";
    }

}
