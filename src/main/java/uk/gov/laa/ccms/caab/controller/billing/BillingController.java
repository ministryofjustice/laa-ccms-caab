package uk.gov.laa.ccms.caab.controller.billing;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling requests related to case billing. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class BillingController {

  private final BillingService billingService;

  /**
   * Displays the case statement of account (billing) screen. The available billing actions are
   * shown based on the functions the case carries, and the statement of account figures and
   * bills/POA invoices are retrieved from EBS, mirroring the legacy PUI behaviour.
   *
   * @param ebsCase The case details from EBS.
   * @param user The logged-in user.
   * @param model The model used to pass data to the view.
   * @return The case statement of account view.
   */
  @GetMapping("/case/billing")
  public String caseStatementOfAccount(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    final Set<String> availableFunctions =
        ebsCase.getAvailableFunctions() == null
            ? Collections.emptySet()
            : Set.copyOf(ebsCase.getAvailableFunctions());

    final StatementOfAccountDisplay statementOfAccount =
        billingService.getStatementOfAccountDisplay(
            ebsCase.getCaseReferenceNumber(), ebsCase, user);

    model.addAttribute("caseReferenceNumber", ebsCase.getCaseReferenceNumber());
    model.addAttribute("statementOfAccount", statementOfAccount);
    model.addAttribute(
        "showEnterUndertaking", availableFunctions.contains(FunctionConstants.ENTER_UNDERTAKING));
    model.addAttribute(
        "showCreateBill", availableFunctions.contains(FunctionConstants.ADD_UPDATE_BILL));
    model.addAttribute(
        "showCreatePoa", availableFunctions.contains(FunctionConstants.ADD_UPDATE_POA));

    return "application/billing/case-statement-of-account";
  }
}
