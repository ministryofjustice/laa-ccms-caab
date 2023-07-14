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
import uk.gov.laa.ccms.data.model.UserDetail;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes("applicationDetails")
public class OfficeController {

    private final ApplicationDetailsValidator applicationValidator;

    @ModelAttribute("applicationDetails")
    public ApplicationDetails getApplicationDetails() {
        return new ApplicationDetails();
    }

    @GetMapping("/application/office")
    public String selectOffice(@ModelAttribute("user") UserDetail user, Model model){
        model.addAttribute("applicationDetails", getApplicationDetails());
        model.addAttribute("offices", user.getProvider().getOffices());
        return "/application/select-office";
    }

    @PostMapping("/application/office")
    public String selectOffice(@ModelAttribute("user") UserDetail user,
                               @ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                               BindingResult bindingResult,
                               Model model) {

        log.info("POST /application/office: " + applicationDetails.toString());
        applicationValidator.validateSelectOffice(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("offices", user.getProvider().getOffices());
            return "/application/select-office";
        }

        return "redirect:/application/category-of-law";
    }
}


