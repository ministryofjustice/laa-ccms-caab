package uk.gov.laa.ccms.caab.controller.application;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetails;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CategoryOfLawController {

    private final ApplicationDetailsValidator applicationValidator;

    private final SoaGatewayService soaGatewayService;

    private final DataService dataService;

    @GetMapping("/application/category-of-law")
    public String categoryOfLaw(@RequestParam(value = "exceptional_funding", defaultValue = "false") boolean exceptionalFunding,
                                @ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                Model model) {
        log.info("GET /application/category-of-law: " + applicationDetails.toString());
        return getCategoryOfLaw(exceptionalFunding, applicationDetails, model);
    }

    @PostMapping("/application/category-of-law")
    public String categoryOfLaw(@RequestParam(value = "exceptional_funding", defaultValue = "false") boolean exceptionalFunding,
                                @ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                BindingResult bindingResult,
                                RedirectAttributes model) {
        log.info("POST /application/category-of-law: " + applicationDetails.toString());
        applicationValidator.validateCategoryOfLaw(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            return getCategoryOfLaw(exceptionalFunding, applicationDetails, model);
        }

        model.addFlashAttribute("applicationDetails", applicationDetails);
        return "redirect:/application/application-type";
    }

    private String getCategoryOfLaw(boolean exceptionalFunding, ApplicationDetails applicationDetails, Model model) {

        List<CommonLookupValueDetails> categoriesOfLaw;
        if (exceptionalFunding){
            categoriesOfLaw = dataService.getAllCategoriesOfLaw();
        } else {
            //TODO Amend this as its a dummy list, need the soa call here
            List<String> categoryOfLawCodes = Arrays.asList("MAT", "MED", "MSC", "PCW");
            categoriesOfLaw = dataService.getCategoriesOfLaw(categoryOfLawCodes);
        }

        model.addAttribute("applicationDetails", applicationDetails);
        model.addAttribute("categoriesOfLaw", categoriesOfLaw);
        model.addAttribute("exceptionalFunding", exceptionalFunding);

        return "/application/select-category-of-law";
    }
}
