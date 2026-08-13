package uk.gov.laa.ccms.caab.controller.billing;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData;
import uk.gov.laa.ccms.caab.bean.validators.billing.BillingUndertakingValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling requests related to case billing. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class BillingController {

  private final BillingService billingService;
  private final BillingUndertakingValidator billingUndertakingValidator;
  private final AmendmentService amendmentService;

  /**
   * Displays the case statement of account (billing) screen. The available billing actions are
   * shown based on the functions the case carries, and the statement of account figures and
   * bills/POA invoices are retrieved from EBS, mirroring the legacy PUI behaviour. The bills/POA
   * table can be long, so it is paginated in line with the other PUI tables.
   *
   * @param ebsCase The case details from EBS.
   * @param user The logged-in user.
   * @param page The requested bills/POA page (zero-based).
   * @param size The number of bills/POA rows per page.
   * @param request The current request, used to build the pagination links.
   * @param model The model used to pass data to the view.
   * @return The case statement of account view.
   */
  @GetMapping("/case/billing")
  public String caseStatementOfAccount(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      final HttpServletRequest request,
      final Model model) {

    final Set<String> availableFunctions =
        ebsCase.getAvailableFunctions() == null
            ? Collections.emptySet()
            : Set.copyOf(ebsCase.getAvailableFunctions());

    final StatementOfAccountDisplay statementOfAccount =
        billingService.getStatementOfAccountDisplay(
            ebsCase.getCaseReferenceNumber(), ebsCase, user);

    final List<BillPoaRow> billsAndPoa =
        statementOfAccount.getBillsAndPoa() == null
            ? List.of()
            : statementOfAccount.getBillsAndPoa();

    model.addAttribute("caseReferenceNumber", ebsCase.getCaseReferenceNumber());
    model.addAttribute("statementOfAccount", statementOfAccount);
    model.addAttribute(
        "billsAndPoaPage",
        PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), billsAndPoa));
    model.addAttribute("currentUrl", request.getRequestURL().toString());
    // Return the user to the bills/POA table (below the statement) after paging, rather than the
    // top of the page. The shared pagination fragment appends this as the link's anchor.
    model.addAttribute("paginationAnchor", "bills-and-poa");
    model.addAttribute(
        "showEnterUndertaking", availableFunctions.contains(FunctionConstants.ENTER_UNDERTAKING));
    // A create action is offered only when the case allows it and no draft of that type already
    // exists, since the create screen edits the existing draft rather than adding a second one.
    model.addAttribute(
        "showCreateBill",
        availableFunctions.contains(FunctionConstants.ADD_UPDATE_BILL)
            && !statementOfAccount.isDraftBillExists());
    model.addAttribute(
        "showCreatePoa",
        availableFunctions.contains(FunctionConstants.ADD_UPDATE_POA)
            && !statementOfAccount.isDraftPoaExists());

    return "application/billing/case-statement-of-account";
  }

  /**
   * Displays the enter undertaking screen and clears any previously cached undertaking range
   * values from the session.
   *
   * @param model The model used to pass form data to the view.
   * @param session The current HTTP session.
   * @return The enter undertaking view.
   */
  @GetMapping("/case/billing/undertaking")
  public String enterUndertaking(final Model model,
      final HttpSession session) {
    session.removeAttribute("undertakingMinimum");
    session.removeAttribute("undertakingMaximum");
    model.addAttribute("undertakingFormData", new UndertakingFormData());
    return "application/billing/enter-undertaking";
  }

  /**
   * Validates and submits an undertaking amount as a quick amendment.
   *
   * <p>The valid undertaking range is derived from the current provider statement and cached in
   * the session for redisplay when validation fails.
   *
   * @param ebsCase The case details from EBS.
   * @param user The logged-in user.
   * @param undertakingFormData The undertaking form payload.
   * @param bindingResult Validation errors for the form.
   * @param model The model used to pass form data back to the view.
   * @param session The current HTTP session.
   * @return The enter undertaking view when validation fails; otherwise a redirect to submit the
   *     amendment.
   */
  @PostMapping("/case/billing/undertaking")
  public String submitUndertaking(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("undertakingFormData") final UndertakingFormData undertakingFormData,
      final BindingResult bindingResult,
      final Model model,
      HttpSession session) {

    BigDecimal undertakingMinimum =
        (BigDecimal) session.getAttribute("undertakingMinimum");
    BigDecimal undertakingMaximum =
        (BigDecimal) session.getAttribute("undertakingMaximum");

    if (undertakingMinimum == null || undertakingMaximum == null) {
      final StatementOfAccountDetail statementOfAccount =
          billingService.getCurrentProviderStatement(ebsCase.getCaseReferenceNumber(), ebsCase, user);
      if (statementOfAccount != null) {
        if (statementOfAccount.getBills() != null) {
          undertakingMinimum = statementOfAccount.getBills().getTotalAmount();
        }
        if (statementOfAccount.getCostLimitation() != null) {
          undertakingMaximum = statementOfAccount.getCostLimitation().getRemainingAmount();
        }
      }

      undertakingMinimum = undertakingMinimum == null ? BigDecimal.ZERO : undertakingMinimum;
      undertakingMaximum = undertakingMaximum == null ? BigDecimal.ZERO : undertakingMaximum;
    }

    undertakingFormData.setUndertakingMinimumAmount(undertakingMinimum);
    undertakingFormData.setUndertakingMaximumAmount(undertakingMaximum);

    // validate
    billingUndertakingValidator.validate(undertakingFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      session.setAttribute("undertakingMinimum", undertakingMinimum);
      session.setAttribute("undertakingMaximum", undertakingMaximum);
      model.addAttribute("undertakingFormData", undertakingFormData);
      return "application/billing/enter-undertaking";
    }

    final String transactionId = amendmentService.submitQuickAmendmentUndertaking(
        undertakingFormData,
        ebsCase.getCaseReferenceNumber(),
        user);

    session.setAttribute(SUBMISSION_TRANSACTION_ID, transactionId);
    session.removeAttribute(SUBMISSION_RESULT);
    session.removeAttribute("undertakingMinimum");
    session.removeAttribute("undertakingMaximum");
    model.addAttribute("undertakingFormData", new UndertakingFormData());
    return "redirect:/amendments/submit-case";
  }
}
