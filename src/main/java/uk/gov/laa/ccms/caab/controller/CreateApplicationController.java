package uk.gov.laa.ccms.caab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.UserDetails;

@Controller
@RequiredArgsConstructor
public class CreateApplicationController {

    private final DataService dataService;

    @GetMapping("/application/office")
    public String selectOffice(Model model){
        UserDetails user = initialiseUser(model);
        model.addAttribute("applicationDetails", new ApplicationDetails());
        model.addAttribute("offices", user.getProvider().getOffices());

        return "/application/select-office";
    }

    @PostMapping("/application/office")
    public String selectOffice(@Valid ApplicationDetails applicationDetails, BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            UserDetails user = initialiseUser(model);
            model.addAttribute("offices", user.getProvider().getOffices());
            return "/application/select-office";
        }

        return "redirect:/application/select-category-of-law";
    }

    private UserDetails initialiseUser(Model model) {
        /* TODO: Sort out cacheing of User in session */
        UserDetails user = (UserDetails) model.getAttribute("user");
        user = dataService.getUser(user.getLoginId()).block();

        model.addAttribute("user", user);
        return user;
    }

}
