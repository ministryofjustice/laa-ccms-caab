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
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

import java.util.List;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;


@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class ApplicationTypeController {

    private final ApplicationDetailsValidator applicationValidator;

    private final DataService dataService;

    @GetMapping("/application/application-type")
    public String applicationType(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                                  Model model){
        log.info("GET /application/application-type: {}", applicationDetails);

        if (applicationDetails.isExceptionalFunding()) {
            log.warn("ApplicationTypeController hit despite exceptionalFunding being true. Redirecting to client-search");
            return "redirect:/application/client-search";
        }

        List<CommonLookupValueDetail> applicationTypes = dataService.getApplicationTypes();
        model.addAttribute("applicationTypes", applicationTypes);

        return "/application/select-application-type";
    }

    @PostMapping("/application/application-type")
    public String applicationType(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                                  BindingResult bindingResult,
                                  Model model) {
        log.info("POST /application/application-type: {}", applicationDetails);
        applicationValidator.validateApplicationTypeCategory(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            List<CommonLookupValueDetail> applicationTypes = dataService.getApplicationTypes();
            model.addAttribute("applicationTypes", applicationTypes);
            return "/application/select-application-type";
        }

        return "redirect:/application/delegated-functions";
    }
}
