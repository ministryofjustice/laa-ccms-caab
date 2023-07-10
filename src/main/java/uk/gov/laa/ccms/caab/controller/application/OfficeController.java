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
import uk.gov.laa.ccms.data.model.UserDetails;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OfficeController {

    private final ApplicationDetailsValidator applicationValidator;

    @GetMapping("/application/office")
    public String selectOffice(@ModelAttribute("user") UserDetails user, Model model){
        model.addAttribute("applicationDetails", new ApplicationDetails());
        model.addAttribute("offices", user.getProvider().getOffices());

        return "/application/select-office";
    }

    @PostMapping("/application/office")
    public String selectOffice(@ModelAttribute("user") UserDetails user,
                               @ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                               BindingResult bindingResult,
                               RedirectAttributes model) {

        log.info("POST /application/office: " + applicationDetails.toString());
        applicationValidator.validateSelectOffice(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("offices", user.getProvider().getOffices());
            return "/application/select-office";
        }

        model.addFlashAttribute("applicationDetails", applicationDetails);

        return "redirect:/application/category-of-law";
    }
}


