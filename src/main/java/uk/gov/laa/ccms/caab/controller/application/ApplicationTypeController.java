package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetails;

import java.util.List;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationTypeController {

    private final ApplicationDetailsValidator applicationValidator;

    private final DataService dataService;

    @GetMapping("/application/application-type")
    public String applicationType(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                  Model model){
        log.info("GET /application/application-type: " + applicationDetails.toString());

        List<CommonLookupValueDetails> applicationTypes = dataService.getApplicationTypes();

        model.addAttribute("applicationDetails", new ApplicationDetails());
        model.addAttribute("applicationTypes", applicationTypes);

        return "/application/select-application-type";
    }

    @PostMapping("/application/application-type")
    public String applicationType(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                  BindingResult bindingResult,
                                  RedirectAttributes model) {
        applicationValidator.validateApplicationType(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            List<CommonLookupValueDetails> applicationTypes = dataService.getApplicationTypes();
            model.addAttribute("applicationTypes", applicationTypes);
            return "/application/select-application-type";
        }

        return "redirect:/application/delegated-functions";
    }
}
