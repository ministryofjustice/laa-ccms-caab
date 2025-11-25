package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;

import java.math.BigDecimal;
import java.util.Optional;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;

@RequiredArgsConstructor
@Controller
@Slf4j
public class AllocateCostLimitController {
    private final ApplicationService applicationService;

    @GetMapping("/allocate-cost-limit")
    public String caseDetails(
            @SessionAttribute(CASE) final ApplicationDetail ebsCase,
            @SessionAttribute(APPLICATION_SUMMARY) @Nullable final BaseApplicationDetail tdsApplication,
            @SessionAttribute(NOTIFICATION_ID) @Nullable final String notificationId,
            Model model) {

        final ApplicationSectionDisplay applicationSectionDisplay =
                Optional.ofNullable(applicationService.getCaseDetailsDisplay(ebsCase))
                        .orElseThrow(()
                                -> new CaabApplicationException("Failed to retrieve case details"));

        BigDecimal sum = ebsCase.getCosts().getCostEntries().stream().distinct()
                .map(CostEntryDetail::getRequestedCosts)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grantedCostLimitation = ebsCase.getCosts().getGrantedCostLimitation();

        model.addAttribute("totalRemaining", grantedCostLimitation.subtract(sum));
        model.addAttribute("summary", applicationSectionDisplay);
        model.addAttribute("costEntries", ebsCase.getCosts().getCostEntries().stream().distinct().toList());

        return "application/costAllocation";
    }
}
