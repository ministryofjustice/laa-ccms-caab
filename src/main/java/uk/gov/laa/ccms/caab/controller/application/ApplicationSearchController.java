package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.ApplicationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ApplicationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationSearchController {

    private final ApplicationSearchCriteriaValidator searchCriteriaValidator;

    private final DataService dataService;

    @GetMapping("/application/search")
    public String applicationSearch(
        @ModelAttribute("applicationSearchCriteria") ApplicationSearchCriteria searchCriteria,
        @SessionAttribute("user") UserDetail userDetails,
        Model model){
        log.info("GET /application/search");

        model.addAttribute("feeEarners",
            getFeeEarners(userDetails.getProvider().getId()));
        model.addAttribute("offices",
            userDetails.getProvider().getOffices());

        return "/application/application-search";
    }

    @PostMapping("/application/search")
    public String applicationSearch(
        @ModelAttribute("applicationSearchCriteria") ApplicationSearchCriteria searchCriteria,
        @SessionAttribute("user") UserDetail userDetails,
        BindingResult bindingResult,
        Model model){
        log.info("POST /application/search: criteria={}", searchCriteria.toString());

        searchCriteriaValidator.validate(searchCriteria, bindingResult);
        if(bindingResult.hasErrors()) {
            model.addAttribute("feeEarners",
                getFeeEarners(userDetails.getProvider().getId()));
            model.addAttribute("offices",
                userDetails.getProvider().getOffices());
            return "/application/application-search";
        }
        return "redirect:/application/application-search-results";
    }

    private List<ContactDetail> getFeeEarners(Integer providerId) {
        FeeEarnerDetail feeEarners = dataService.getFeeEarners(providerId).block();
        return Optional.ofNullable(feeEarners)
            .map(FeeEarnerDetail::getContent)
            .orElse(Collections.emptyList());
    }
}
