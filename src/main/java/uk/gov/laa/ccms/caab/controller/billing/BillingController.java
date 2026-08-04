package uk.gov.laa.ccms.caab.controller.billing;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
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
}
