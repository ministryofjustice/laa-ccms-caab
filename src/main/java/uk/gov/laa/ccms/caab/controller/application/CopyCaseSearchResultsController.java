package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COPY_CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COPY_CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {APPLICATION_DETAILS, COPY_CASE_SEARCH_CRITERIA, COPY_CASE_SEARCH_RESULTS})
public class CopyCaseSearchResultsController {

    private final SoaGatewayService soaGatewayService;

    private final DataService dataService;

    private final SearchConstants searchConstants;

    @GetMapping("/application/copy-case-search/results")
    public String copyCaseSearchResults(@RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "10") int size,
                                      @ModelAttribute(COPY_CASE_SEARCH_CRITERIA) CopyCaseSearchCriteria copyCaseSearchCriteria,
                                      @SessionAttribute(USER_DETAILS) UserDetail user,
                                      HttpServletRequest request,
                                      Model model) {
        log.info("GET /application/copy-case-search/results");

        // Get the Copy Case Status
        CaseStatusLookupValueDetail copyCaseStatus = dataService.getCopyCaseStatus();
        if (copyCaseStatus != null) {
            copyCaseSearchCriteria.setActualStatus(copyCaseStatus.getCode());
        }

        CaseDetails caseSearchResults = soaGatewayService.getCases(copyCaseSearchCriteria, user.getLoginId(),
                user.getUserType(), page, size).block();

        if (caseSearchResults != null && caseSearchResults.getContent() != null && caseSearchResults.getTotalElements() > 0){
            if (caseSearchResults.getTotalElements() > searchConstants.getMaxSearchResultsCases()){
                return "/application/application-copy-case-search-too-many-results";
            }
            String currentUrl = request.getRequestURL().toString();
            model.addAttribute("currentUrl", currentUrl);
            model.addAttribute(COPY_CASE_SEARCH_RESULTS, caseSearchResults);
            return "/application/application-copy-case-search-results";
        } else {
            return "/application/application-copy-case-search-no-results";
        }
    }

    @GetMapping("/application/copy-case-search/{case-reference-number}/select")
    public String copyCaseSearchResults(@PathVariable("case-reference-number") String caseReferenceNumber) {
        log.info("GET /application/copy-case-search/{}/select", caseReferenceNumber);

        return "redirect:/application/TODO";
    }
}

