package uk.gov.laa.ccms.caab.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountBills;
import uk.gov.laa.ccms.data.model.StatementOfAccountCostLimitation;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetails;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoice;
import uk.gov.laa.ccms.data.model.StatementOfAccountPoa;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Service responsible for building the Case Statement of Account display from the per-firm
 * statements returned by EBS.
 *
 * <p>This ports the legacy PUI {@code PrepareBillSummary} aggregation: the statements are bucketed
 * into the current provider, prior solicitor and counsel columns, and the invoices are flattened
 * into a single bills/POA list.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

  private static final String ENTITY_TYPE_PROVIDER = "PROVIDER";
  private static final String ENTITY_TYPE_COUNSEL = "COUNSEL";
  private static final String INVOICE_STATUS_DRAFT = "Draft";

  private final EbsApiClient ebsApiClient;

  /**
   * Retrieve and build the statement of account display for the supplied case.
   *
   * @param caseReferenceNumber the case reference number.
   * @param ebsCase the case the statement of account belongs to.
   * @param user the logged-in user.
   * @return the assembled {@link StatementOfAccountDisplay}.
   */
  public StatementOfAccountDisplay getStatementOfAccountDisplay(
      final String caseReferenceNumber, final ApplicationDetail ebsCase, final UserDetail user) {

    final Long currentProviderId =
        Optional.ofNullable(user.getProvider())
            .map(provider -> provider.getId())
            .map(Integer::longValue)
            .orElse(null);
    final Long caseProviderId = caseProviderId(ebsCase);
    final boolean userBelongsToCurrentProvider =
        currentProviderId != null && currentProviderId.equals(caseProviderId);

    // Users outside the case's provider only see their own firm's figures (legacy PUI behaviour).
    final StatementOfAccountDetails response =
        ebsApiClient
            .getStatementOfAccount(
                caseReferenceNumber, userBelongsToCurrentProvider ? null : currentProviderId)
            .block();

    final StatementOfAccountDisplay display = new StatementOfAccountDisplay();
    display.setUserBelongsToCurrentProvider(userBelongsToCurrentProvider);
    if (response == null) {
      return display;
    }

    final List<StatementOfAccountDetail> statements =
        response.getContent() == null ? List.of() : response.getContent();

    final StatementOfAccountDetail providerStatement =
        currentProviderStatement(statements, currentProviderId, userBelongsToCurrentProvider);
    display.setProviderFirmName(providerFirmName(ebsCase, user, userBelongsToCurrentProvider));
    display.setProvider(providerStatement == null ? zeroColumn() : toColumn(providerStatement));

    if (userBelongsToCurrentProvider) {
      display.setCounsel(sumColumns(byEntityTypeCounsel(statements)));
      display.setPriorSolicitor(sumColumns(priorSolicitors(statements, currentProviderId)));
    } else {
      // The counsel and prior solicitor columns are not shown (legacy PUI behaviour).
      display.setCounsel(new SoaFigureColumn());
      display.setPriorSolicitor(new SoaFigureColumn());
    }

    // EBS returns one row per firm with no case-wide totals block, so the total column is derived.
    display.setTotal(
        addColumns(display.getProvider(), display.getPriorSolicitor(), display.getCounsel()));

    setCounselCostCeiling(display);

    display.setInvoices(flattenNonDraftInvoices(providerFirst(statements, providerStatement)));
    return display;
  }

  /** Lists the current provider's statement first, so its invoices head the bills/POA table. */
  private List<StatementOfAccountDetail> providerFirst(
      final List<StatementOfAccountDetail> statements,
      final StatementOfAccountDetail providerStatement) {
    return Stream.concat(
            Stream.ofNullable(providerStatement),
            statements.stream().filter(statement -> statement != providerStatement))
        .toList();
  }

  /**
   * Sets the counsel cost ceiling shown against the case.
   *
   * <p>TODO: this is a placeholder of zero. EBS holds the ceiling on the case-level total of its
   * statement of account service, and the view behind {@code /statementofaccount} does not carry
   * it. Zero is what the legacy PUI displays wherever EBS reports no ceiling, but a case that
   * carries a real one will be understated until the figure is exposed on the API.
   */
  private void setCounselCostCeiling(final StatementOfAccountDisplay display) {
    display.setCounselCostCeiling(BigDecimal.ZERO);
    display.setCounselCostCeilingRemaining(BigDecimal.ZERO);
  }

  /** EBS does not return a firm name against a statement, so it is taken from the case or user. */
  private String providerFirmName(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final boolean userBelongsToCurrentProvider) {
    if (!userBelongsToCurrentProvider) {
      return Optional.ofNullable(user.getProvider())
          .map(provider -> provider.getName())
          .orElse(null);
    }
    return Optional.ofNullable(ebsCase.getProviderDetails())
        .map(details -> details.getProvider())
        .map(provider -> provider.getDisplayValue())
        .orElse(null);
  }

  private Long caseProviderId(final ApplicationDetail ebsCase) {
    return Optional.ofNullable(ebsCase.getProviderDetails())
        .map(details -> details.getProvider())
        .map(provider -> provider.getId())
        .map(Integer::longValue)
        .orElse(null);
  }

  private StatementOfAccountDetail currentProviderStatement(
      final List<StatementOfAccountDetail> statements,
      final Long currentProviderId,
      final boolean userBelongsToCurrentProvider) {
    if (statements.isEmpty()) {
      return null;
    }
    if (!userBelongsToCurrentProvider) {
      // Only the user's own statement was requested when viewing another provider's case.
      return statements.get(0);
    }
    return statements.stream()
        .filter(statement -> ENTITY_TYPE_PROVIDER.equalsIgnoreCase(statement.getEntityType()))
        .filter(
            statement ->
                currentProviderId != null && currentProviderId.equals(statement.getFirmId()))
        .findFirst()
        .orElse(null);
  }

  private List<StatementOfAccountDetail> byEntityTypeCounsel(
      final List<StatementOfAccountDetail> statements) {
    return statements.stream()
        .filter(statement -> ENTITY_TYPE_COUNSEL.equalsIgnoreCase(statement.getEntityType()))
        .toList();
  }

  private List<StatementOfAccountDetail> priorSolicitors(
      final List<StatementOfAccountDetail> statements, final Long currentProviderId) {
    return statements.stream()
        .filter(statement -> ENTITY_TYPE_PROVIDER.equalsIgnoreCase(statement.getEntityType()))
        .filter(
            statement ->
                currentProviderId == null || !currentProviderId.equals(statement.getFirmId()))
        .toList();
  }

  private List<StatementOfAccountInvoice> flattenNonDraftInvoices(
      final List<StatementOfAccountDetail> statements) {
    final List<StatementOfAccountInvoice> invoices = new ArrayList<>();
    for (final StatementOfAccountDetail statement : statements) {
      if (statement.getInvoices() == null) {
        continue;
      }
      for (final StatementOfAccountInvoice invoice : statement.getInvoices()) {
        // EBS draft invoices are backed by the (as yet unbuilt) TDS draft store; do not show them
        // here yet. Submitted / authorised / rejected invoices are the real statement figures.
        if (!INVOICE_STATUS_DRAFT.equalsIgnoreCase(invoice.getInvoiceStatus())) {
          invoices.add(invoice);
        }
      }
    }
    return invoices;
  }

  private SoaFigureColumn toColumn(final StatementOfAccountDetail statement) {
    final SoaFigureColumn column = new SoaFigureColumn();
    if (statement == null) {
      return column;
    }
    final StatementOfAccountCostLimitation costLimitation = statement.getCostLimitation();
    final StatementOfAccountBills bills = statement.getBills();
    final StatementOfAccountPoa poa = statement.getPoa();

    // The cost limitation is the only optional block on a statement, so it alone can show blank.
    // The undertaking, bills and payment on account amounts always carry a figure, zero included.
    column.setCertificateCostLimitation(
        costLimitation != null ? costLimitation.getCertificateAmount() : null);
    column.setCostLimitationRemaining(
        costLimitation != null ? costLimitation.getRemainingAmount() : null);
    column.setUndertaking(orZero(statement.getUndertakingAmount()));
    column.setBillsAuthorised(orZero(bills != null ? bills.getAmountAuthorised() : null));
    column.setBillsSubmittedButNotAuthorised(
        orZero(bills != null ? bills.getAmountSubmitted() : null));
    column.setPoaRecouped(orZero(poa != null ? poa.getAmountRecouped() : null));
    // The "POA authorised" row shows the un-recouped balance, as the legacy PUI did.
    column.setPoaAuthorised(orZero(poa != null ? poa.getAmountUnrecouped() : null));
    column.setPoaSubmittedButNotAuthorised(orZero(poa != null ? poa.getAmountSubmitted() : null));
    return column;
  }

  private BigDecimal orZero(final BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  /** The legacy PUI shows zeros, not blanks, when the case carries no statement for the firm. */
  private SoaFigureColumn zeroColumn() {
    final SoaFigureColumn column = new SoaFigureColumn();
    column.setCertificateCostLimitation(BigDecimal.ZERO);
    column.setCostLimitationRemaining(BigDecimal.ZERO);
    column.setUndertaking(BigDecimal.ZERO);
    column.setBillsAuthorised(BigDecimal.ZERO);
    column.setBillsSubmittedButNotAuthorised(BigDecimal.ZERO);
    column.setPoaRecouped(BigDecimal.ZERO);
    column.setPoaAuthorised(BigDecimal.ZERO);
    column.setPoaSubmittedButNotAuthorised(BigDecimal.ZERO);
    return column;
  }

  private SoaFigureColumn sumColumns(final List<StatementOfAccountDetail> statements) {
    SoaFigureColumn total = new SoaFigureColumn();
    for (final StatementOfAccountDetail statement : statements) {
      total = addColumns(total, toColumn(statement));
    }
    return total;
  }

  private SoaFigureColumn addColumns(final SoaFigureColumn... columns) {
    final SoaFigureColumn total = new SoaFigureColumn();
    for (final SoaFigureColumn column : columns) {
      if (column == null) {
        continue;
      }
      total.setCertificateCostLimitation(
          add(total.getCertificateCostLimitation(), column.getCertificateCostLimitation()));
      total.setCostLimitationRemaining(
          add(total.getCostLimitationRemaining(), column.getCostLimitationRemaining()));
      total.setUndertaking(add(total.getUndertaking(), column.getUndertaking()));
      total.setBillsAuthorised(add(total.getBillsAuthorised(), column.getBillsAuthorised()));
      total.setBillsSubmittedButNotAuthorised(
          add(
              total.getBillsSubmittedButNotAuthorised(),
              column.getBillsSubmittedButNotAuthorised()));
      total.setPoaRecouped(add(total.getPoaRecouped(), column.getPoaRecouped()));
      total.setPoaAuthorised(add(total.getPoaAuthorised(), column.getPoaAuthorised()));
      total.setPoaSubmittedButNotAuthorised(
          add(total.getPoaSubmittedButNotAuthorised(), column.getPoaSubmittedButNotAuthorised()));
    }
    return total;
  }

  private BigDecimal add(final BigDecimal runningTotal, final BigDecimal amount) {
    if (amount == null) {
      return runningTotal;
    }
    return runningTotal == null ? amount : runningTotal.add(amount);
  }
}
