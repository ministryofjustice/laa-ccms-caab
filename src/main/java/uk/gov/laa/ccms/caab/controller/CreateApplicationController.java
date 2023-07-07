package uk.gov.laa.ccms.caab.controller;

import lombok.RequiredArgsConstructor;
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
import uk.gov.laa.ccms.data.model.UserDetails;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CreateApplicationController {

    private final ApplicationDetailsValidator applicationValidator;

    private final SoaGatewayService soaGatewayService;

    private final DataService dataService;

    @GetMapping("/application/office")
    public String selectOffice(Model model){
        UserDetails user = (UserDetails) model.getAttribute("user");

        model.addAttribute("applicationDetails", new ApplicationDetails());
        model.addAttribute("offices", user.getProvider().getOffices());

        return "/application/select-office";
    }

    @PostMapping("/application/office")
    public String selectOffice(ApplicationDetails applicationDetails, BindingResult bindingResult, Model model) {
        applicationValidator.validateSelectOffice(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            UserDetails user = (UserDetails) model.getAttribute("user");
            model.addAttribute("offices", user.getProvider().getOffices());
            return "/application/select-office";
        }

        return "redirect:/application/category-of-law";
    }

    @GetMapping("/application/category-of-law")
    public String categoryOfLaw(Model model){
        UserDetails user = (UserDetails) model.getAttribute("user");

        model.addAttribute("applicationDetails", new ApplicationDetails());
        model.addAttribute("categoriesOfLaw", user.getProvider().getOffices());

        return "/application/select-category-of-law";
    }

    @PostMapping("/application/category-of-law")
    public String categoryOfLaw(ApplicationDetails applicationDetails, BindingResult bindingResult, Model model) {
        applicationValidator.validateCategoryOfLaw(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            UserDetails user = (UserDetails) model.getAttribute("user");
            model.addAttribute("categoriesOfLaw", user.getProvider().getOffices());
            return "/application/select-category-of-law";
        }

        return "redirect:/application/application-type";
    }

    @GetMapping("/application/application-type")
    public String applicationType(Model model){
        List<CommonLookupValueDetails> applicationTypes = dataService.getApplicationTypes();

        model.addAttribute("applicationDetails", new ApplicationDetails());
        model.addAttribute("applicationTypes", applicationTypes);

        return "select-application-type";
    }

    @PostMapping("/application/application-type")
    public String applicationType(ApplicationDetails applicationDetails, BindingResult bindingResult, Model model) {
        applicationValidator.validateApplicationType(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            List<CommonLookupValueDetails> applicationTypes = dataService.getApplicationTypes();
            model.addAttribute("applicationTypes", applicationTypes);
            return "select-application-type";
        }

        return "redirect:/application/delegate-functions";
    }

}
