package uk.gov.laa.ccms.caab.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.Bills;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetail;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetails;
import uk.gov.laa.ccms.data.model.StatementOfAccountBills;
import uk.gov.laa.ccms.data.model.StatementOfAccountCostLimitation;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetails;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoice;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoiceList;
import uk.gov.laa.ccms.data.model.StatementOfAccountPoa;
import uk.gov.laa.ccms.data.model.TaxRateLookupDetail;
import uk.gov.laa.ccms.data.model.TaxRateLookupValueDetail;
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
  private static final String COST_CATEGORY_COUNSEL = "COUNSEL";
  private static final String INVOICE_STATUS_DRAFT = "Draft";
  private static final String INVOICE_TYPE_BILL = "Bill";
  private static final String INVOICE_TYPE_POA = "POA";
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final ZoneId EBS_ZONE = ZoneId.of("Europe/London");

  private final EbsApiClient ebsApiClient;
  private final CaabApiClient caabApiClient;
  private final LookupService lookupService;

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

    final StatementOfAccountDisplay display = new StatementOfAccountDisplay();
    display.setUserBelongsToCurrentProvider(userBelongsToCurrentProvider);

    // If the user does not belong to the case's provider and we cannot identify their own
    // provider, there is nothing to scope the query to. An unrestricted query returns every firm's
    // statement and invoices, so return an empty display rather than expose another firm's billing
    // data.
    if (!userBelongsToCurrentProvider && currentProviderId == null) {
      return display;
    }

    // Users outside the case's provider only see their own firm's figures (legacy PUI behaviour).
    final StatementOfAccountDetails response =
        ebsApiClient
            .getStatementOfAccount(
                caseReferenceNumber, userBelongsToCurrentProvider ? null : currentProviderId)
            .block();

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

    setCounselCostCeiling(display, ebsCase);

    // Rows are grouped by firm, the current provider first: its submitted invoices, then its own
    // drafts, then the other firms' invoices. The legacy PUI added the drafts to the provider's own
    // statement, so they sit within the provider's block rather than at the foot of the table.
    final List<StatementOfAccountDetail> otherStatements =
        statements.stream().filter(statement -> statement != providerStatement).toList();
    final List<BillPoaRow> rows =
        new ArrayList<>(
            toRows(
                flattenNonDraftInvoices(
                    providerStatement == null ? List.of() : List.of(providerStatement))));
    addDraftRows(rows, display, caseReferenceNumber, String.valueOf(currentProviderId));
    rows.addAll(toRows(flattenNonDraftInvoices(otherStatements)));
    display.setBillsAndPoa(rows);
    return display;
  }

  /**
   * Adds the provider's draft payments on account and draft bill to the bills/POA rows, and records
   * whether each exists so the view can suppress the matching create action (a case carries at most
   * one draft bill and the create screens edit the existing draft rather than adding another).
   */
  private void addDraftRows(
      final List<BillPoaRow> rows,
      final StatementOfAccountDisplay display,
      final String caseReferenceNumber,
      final String providerId) {

    // The drafts are supplementary to the EBS statement, and come from a separate service. A
    // failure to load them must not take down the whole statement of account, so each fetch is
    // tolerated: on error the drafts are simply not shown.
    final PaymentOnAccountDetails poaResponse =
        loadDraftQuietly(
            caabApiClient.getPaymentsOnAccount(caseReferenceNumber, providerId),
            "payments on account",
            caseReferenceNumber);
    final List<PaymentOnAccountDetail> draftPoas =
        poaResponse == null || poaResponse.getContent() == null
            ? List.of()
            : poaResponse.getContent();
    if (!draftPoas.isEmpty()) {
      final Map<String, BigDecimal> taxRates = taxRatesByCode();
      for (final PaymentOnAccountDetail poa : draftPoas) {
        rows.add(
            new BillPoaRow(
                INVOICE_TYPE_POA,
                INVOICE_STATUS_DRAFT,
                null,
                null,
                poaTotalCost(poa, taxRates),
                true));
      }
    }
    display.setDraftPoaExists(!draftPoas.isEmpty());

    // The CAAB API returns 404 (mapped to an empty result) when there is no draft bill, so a
    // present bill always means a real draft — shown regardless of whether an amount has been
    // entered yet, as the legacy PUI did (loadBill != null). Its presence also hides Create Bill.
    final Bills draftBill =
        loadDraftQuietly(
            caabApiClient.getBill(caseReferenceNumber, providerId), "bill", caseReferenceNumber);
    if (draftBill != null) {
      rows.add(
          new BillPoaRow(
              INVOICE_TYPE_BILL, INVOICE_STATUS_DRAFT, null, null, draftBill.getAmount(), true));
    }
    display.setDraftBillExists(draftBill != null);
  }

  /**
   * Blocks on a draft lookup, returning {@code null} rather than propagating the error so a failure
   * to reach the draft store does not break the statement of account screen.
   */
  private <T> T loadDraftQuietly(
      final Mono<T> draft, final String description, final String caseReferenceNumber) {
    try {
      return draft.block();
    } catch (final Exception e) {
      log.warn(
          "Could not load draft {} for case {}; showing the statement of account without it",
          description,
          caseReferenceNumber,
          e);
      return null;
    }
  }

  private List<BillPoaRow> toRows(final List<StatementOfAccountInvoice> invoices) {
    return invoices.stream()
        .map(
            invoice ->
                new BillPoaRow(
                    invoice.getInvoiceType(),
                    invoice.getInvoiceStatus(),
                    legacyDisplayDate(invoice.getDateSubmitted()),
                    legacyDisplayDate(invoice.getDateAuthorised()),
                    invoice.getInvoiceAmount(),
                    false))
        .toList();
  }

  /**
   * Reproduces the legacy PUI's rendering of EBS statement dates for exact parity. EBS supplies the
   * timestamp as London wall-clock time, but the legacy PUI displayed it in UTC, so a British
   * Summer Time value recorded at midnight shows as the previous day. Converting Europe/London to
   * UTC replicates that shift (British Summer Time only; GMT and non-midnight times are
   * unaffected).
   */
  private LocalDateTime legacyDisplayDate(final LocalDateTime ebsDate) {
    if (ebsDate == null) {
      return null;
    }
    return ebsDate.atZone(EBS_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
  }

  /**
   * The gross draft POA amount, mirroring the legacy PUI: the net cost grossed up by VAT and
   * rounded down to the penny. The stored VAT rate is a reference-data code resolved to a
   * percentage via the tax-rate lookup; an unknown code contributes no VAT.
   */
  private BigDecimal poaTotalCost(
      final PaymentOnAccountDetail poa, final Map<String, BigDecimal> taxRates) {
    final BigDecimal net = orZero(poa.getActualNetCost());
    final BigDecimal percent = taxRates.getOrDefault(poa.getVatRate(), BigDecimal.ZERO);
    return net.multiply(percent.divide(HUNDRED).add(BigDecimal.ONE)).setScale(2, RoundingMode.DOWN);
  }

  /**
   * Loads the VAT rate code to percentage map from the tax-rate reference data. A failure to load
   * it is tolerated: draft POA amounts then simply exclude VAT rather than breaking the screen.
   */
  private Map<String, BigDecimal> taxRatesByCode() {
    final TaxRateLookupDetail response;
    try {
      response = lookupService.getTaxRates().block();
    } catch (final Exception e) {
      log.warn("Could not load tax rates; draft POA amounts will exclude VAT", e);
      return Map.of();
    }
    if (response == null || response.getContent() == null) {
      return Map.of();
    }
    final Map<String, BigDecimal> rates = new HashMap<>();
    for (final TaxRateLookupValueDetail rate : response.getContent()) {
      final BigDecimal percent = parsePercent(rate.getTaxRate());
      if (rate.getCode() != null && percent != null) {
        rates.put(rate.getCode(), percent);
      }
    }
    return rates;
  }

  private BigDecimal parsePercent(final String taxRate) {
    if (taxRate == null || taxRate.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(taxRate.trim());
    } catch (final NumberFormatException e) {
      return null;
    }
  }

  /**
   * Sets the counsel cost ceiling shown against the case.
   *
   * <p>The {@code /statementofaccount} view carries no ceiling, so it is derived from the case's
   * counsel cost limitations, as the legacy PUI's EBS summary block did. The ceiling is the sum of
   * the requested counsel cost limits, and the remaining is that total less what has been billed
   * against them.
   */
  private void setCounselCostCeiling(
      final StatementOfAccountDisplay display, final ApplicationDetail ebsCase) {
    final List<CostEntryDetail> counselEntries =
        Optional.ofNullable(ebsCase.getCosts())
            .map(CostStructureDetail::getCostEntries)
            .orElseGet(List::of)
            .stream()
            .filter(entry -> COST_CATEGORY_COUNSEL.equalsIgnoreCase(entry.getCostCategory()))
            .toList();

    BigDecimal ceiling = BigDecimal.ZERO;
    BigDecimal remaining = BigDecimal.ZERO;
    for (final CostEntryDetail entry : counselEntries) {
      final BigDecimal requested = orZero(entry.getRequestedCosts());
      ceiling = ceiling.add(requested);
      remaining = remaining.add(requested.subtract(orZero(entry.getAmountBilled())));
    }
    display.setCounselCostCeiling(ceiling);
    display.setCounselCostCeilingRemaining(remaining);
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
      // The request was restricted to the user's own firm, so its statement is the only one
      // returned. With no provider to restrict by, the response holds every firm and none of them
      // can be attributed to the user, so no provider statement is shown.
      return currentProviderId == null ? null : statements.get(0);
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
      final StatementOfAccountInvoiceList invoiceList = statement.getInvoiceList();
      if (invoiceList == null || invoiceList.getInvoice() == null) {
        continue;
      }
      for (final StatementOfAccountInvoice invoice : invoiceList.getInvoice()) {
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
    column.setPoaAuthorised(orZero(poa != null ? poa.getAmountUnRecouped() : null));
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
