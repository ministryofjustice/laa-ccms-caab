package uk.gov.laa.ccms.caab.controller.application;


import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes("applicationDetails")
public class CategoryOfLawController {

    private final ApplicationDetailsValidator applicationValidator;

    private final SoaGatewayService soaGatewayService;

    private final DataService dataService;

    @GetMapping("/application/category-of-law")
    public String categoryOfLaw(@RequestParam(value = "exceptional_funding", defaultValue = "false") boolean exceptionalFunding,
                                @ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                @SessionAttribute("user") UserDetail userDetails,
                                Model model) {
        log.info("GET /application/category-of-law: " + applicationDetails.toString());

        applicationDetails.setExceptionalFunding(exceptionalFunding);

        initialiseCategoriesOfLaw(applicationDetails, userDetails, model);

        return "/application/select-category-of-law";
    }

    @PostMapping("/application/category-of-law")
    public String categoryOfLaw(@ModelAttribute("applicationDetails") ApplicationDetails applicationDetails,
                                @SessionAttribute("user") UserDetail userDetails,
                                BindingResult bindingResult,
                                Model model) {
        log.info("POST /application/category-of-law: " + applicationDetails.toString());
        applicationValidator.validateCategoryOfLaw(applicationDetails, bindingResult);

        String viewName = "redirect:/application/application-type";
        if (bindingResult.hasErrors()) {
            initialiseCategoriesOfLaw(applicationDetails, userDetails, model);
            viewName = "/application/select-category-of-law";
        } else if (applicationDetails.isExceptionalFunding()) {
            // Exception Funding has been selected, so initialise the ApplicationType to ECF
            // and bypass the ApplicationType screen.
            applicationDetails.setApplicationTypeId(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);
            viewName = "redirect:/application/client-search";
        }

        return viewName;
    }

    private void initialiseCategoriesOfLaw(ApplicationDetails applicationDetails,
        UserDetail user, Model model) {

        List<CommonLookupValueDetail> categoriesOfLaw;
        if (applicationDetails.isExceptionalFunding()){
            categoriesOfLaw = dataService.getAllCategoriesOfLaw();
        } else {
            List<String> categoryOfLawCodes = soaGatewayService.getCategoryOfLawCodes(
                user.getProvider().getId(),
                applicationDetails.getOfficeId(),
                user.getLoginId(),
                user.getUserType(),
                Boolean.TRUE);
            categoriesOfLaw = dataService.getCategoriesOfLaw(categoryOfLawCodes);
        }

        model.addAttribute("categoriesOfLaw", categoriesOfLaw);
    }
}
