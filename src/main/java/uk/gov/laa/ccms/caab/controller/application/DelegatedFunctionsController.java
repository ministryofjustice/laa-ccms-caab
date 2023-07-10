package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetails;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DelegatedFunctionsController {

    private final ApplicationDetailsValidator applicationValidator;

    private final DataService dataService;

    @GetMapping("/application/delegated-functions")
    public String delegatedFunction(Model model){
        List<CommonLookupValueDetails> applicationTypes = dataService.getApplicationTypes();
        model.addAttribute("applicationDetails", new ApplicationDetails());

        return "/application/select-delegated-functions";
    }

    @PostMapping("/application/delegated-functions")
    public String delegatedFunction(ApplicationDetails applicationDetails, BindingResult bindingResult, Model model) {
        applicationValidator.validateDelegatedFunction(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            return "/application/select-delegated-functions";
        }

        return "redirect:/application/client-search";
    }

}
