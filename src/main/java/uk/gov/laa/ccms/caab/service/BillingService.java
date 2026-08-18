package uk.gov.laa.ccms.caab.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.SoaApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BillCreate;
import uk.gov.laa.ccms.caab.model.Bills;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetail;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetails;
import uk.gov.laa.ccms.data.model.BaseProvider;
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
import uk.gov.laa.ccms.soa.gateway.model.BillDetail;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceDataResponse;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceDetail;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceResponse;
import uk.gov.laa.ccms.soa.gateway.model.OpaEntity;
import uk.gov.laa.ccms.soa.gateway.model.OpaInstance;

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
  private static final String INVOICE_STATUS_REJECTED = "Rejected";
  private static final String INVOICE_TYPE_BILL = "Bill";
  // Entities a copied bill must not carry forward: they belong to the case as it stands now, and
  // the legacy CopyBill empties them so they re-populate from it.
  private static final Set<String> COPY_EXCLUDED_ENTITY_TYPES =
      Set.of("PROCEEDING", "OPPONENT_OTHER_PARTIES");
  private static final String INVOICE_TYPE_POA = "POA";
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final ZoneId EBS_ZONE = ZoneId.of("Europe/London");

  /** Submitted invoices are shown most recent first, tie-broken by billing incident id. */
  private static final Comparator<StatementOfAccountInvoice> BY_DATE_SUBMITTED_DESC =
      Comparator.comparing(
              StatementOfAccountInvoice::getDateSubmitted,
              Comparator.nullsLast(Comparator.reverseOrder()))
          .thenComparing(
              StatementOfAccountInvoice::getBillingIncidentId,
              Comparator.nullsLast(Comparator.naturalOrder()));

  private final EbsApiClient ebsApiClient;
  private final CaabApiClient caabApiClient;
  private final LookupService lookupService;
  private final SoaApiClient soaApiClient;
  private final SoaApplicationMapper soaApplicationMapper;
  private final AssessmentService assessmentService;

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

    // The provider's drafts (the actionable, in-progress rows) are pinned to the top, so they stay
    // visible when the table paginates. The submitted invoices follow, most recent first, with a
    // stable tie-break so equal submitted dates keep a deterministic order.
    final List<BillPoaRow> rows = new ArrayList<>();
    addDraftRows(rows, display, caseReferenceNumber, String.valueOf(currentProviderId));
    final List<StatementOfAccountInvoice> submitted =
        new ArrayList<>(flattenNonDraftInvoices(statements));
    submitted.sort(BY_DATE_SUBMITTED_DESC);
    rows.addAll(toRows(submitted));
    display.setBillsAndPoa(markCopyableBills(rows, display.isDraftBillExists()));
    return display;
  }

  /**
   * Returns the provider's draft payments on account for a case, most recently created first.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the drafts belong to.
   * @return the draft payments on account, empty when there are none.
   */
  public List<PaymentOnAccountDetail> getDraftPaymentsOnAccount(
      final String caseReferenceNumber, final String providerId) {
    final PaymentOnAccountDetails response =
        caabApiClient.getPaymentsOnAccount(caseReferenceNumber, providerId).block();

    return response == null || response.getContent() == null ? List.of() : response.getContent();
  }

  /**
   * Ensures the provider has a draft payment on account for the case, creating an empty one when
   * there is none. This ports the legacy PUI {@code AddPaymentOnAccount}: entering the POA details
   * screen creates the draft if it is not already there, so the POA shows in the bills/POA table
   * straight away and the OPA interview has a draft to write its answers back to. It is
   * deliberately idempotent - re-entering the screen edits the existing draft rather than adding a
   * second one.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the draft belongs to.
   * @param user the logged-in user.
   * @return the existing or newly created draft payment on account.
   */
  public PaymentOnAccountDetail createDraftPaymentOnAccountIfAbsent(
      final String caseReferenceNumber, final String providerId, final UserDetail user) {

    final List<PaymentOnAccountDetail> existing =
        getDraftPaymentsOnAccount(caseReferenceNumber, providerId);
    if (!existing.isEmpty()) {
      return existing.get(0);
    }

    // The legacy PUI creates the POA carrying only the case and provider; every other field is
    // filled in by the OPA interview and written back by the connector.
    final PaymentOnAccountDetail paymentOnAccount =
        new PaymentOnAccountDetail().lscCaseReference(caseReferenceNumber).providerId(providerId);

    caabApiClient.createPaymentOnAccount(paymentOnAccount, user.getLoginId()).block();

    return getDraftPaymentsOnAccount(caseReferenceNumber, providerId).stream()
        .findFirst()
        .orElse(paymentOnAccount);
  }

  /**
   * Offers the copy action against rejected bills. A case carries at most one draft bill, and
   * copying creates one, so the action is withheld entirely while a draft bill is already in
   * progress - the same rule the legacy PUI applies to the Create Bill button.
   *
   * <p>A bill is anything EBS does not type as a POA, rather than anything typed exactly "Bill":
   * EBS returns specific bill types such as "Counsel Bill", and the legacy PUI tests {@code not
   * fn:contains(invoiceType, 'POA')} so those remain copyable.
   *
   * <p>The billing incident id is what addresses the invoice in EBS, and it is optional in the
   * statement of account - some invoices come back without one. A row missing it cannot be copied,
   * so the action is withheld rather than offered as a link that could only fail.
   */
  private List<BillPoaRow> markCopyableBills(
      final List<BillPoaRow> rows, final boolean draftBillExists) {
    if (draftBillExists) {
      return rows;
    }

    return rows.stream()
        .map(
            row ->
                isBillInvoice(row.type())
                        && INVOICE_STATUS_REJECTED.equalsIgnoreCase(row.status())
                        && row.billingIncidentId() != null
                    ? row.withCopyable()
                    : row)
        .toList();
  }

  private boolean isBillInvoice(final String invoiceType) {
    return invoiceType != null && !invoiceType.toUpperCase(Locale.UK).contains(INVOICE_TYPE_POA);
  }

  /**
   * Returns the provider's draft bill for a case, if there is one.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the draft belongs to.
   * @return the draft bill, or {@code null} when there is none.
   */
  public Bills getDraftBill(final String caseReferenceNumber, final String providerId) {
    return caabApiClient.getBill(caseReferenceNumber, providerId).block();
  }

  /**
   * Ensures the provider has a draft bill for the case, creating an empty one when there is none.
   * This ports the legacy PUI {@code AddBill}: entering the bill details screen creates the draft
   * if it is not already there, so the bill shows in the bills/POA table straight away and the OPA
   * interview has a draft to write its answers back to. It is deliberately idempotent - re-entering
   * the screen edits the existing draft rather than adding a second one.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the draft belongs to.
   * @param user the logged-in user.
   * @return the existing or newly created draft bill.
   */
  public Bills createDraftBillIfAbsent(
      final String caseReferenceNumber, final String providerId, final UserDetail user) {

    final Bills existing = getDraftBill(caseReferenceNumber, providerId);
    if (existing != null) {
      return existing;
    }

    // The legacy PUI creates the bill carrying only the case and provider; every other field is
    // filled in by the OPA interview and written back by the connector.
    final BillCreate bill =
        new BillCreate().lscCaseReference(caseReferenceNumber).providerId(providerId);

    caabApiClient.createBill(bill, user.getLoginId()).block();

    // The read back should find what was just created; if it does not, still return a draft
    // carrying both identifiers, since callers rely on a draft always knowing its case and
    // provider.
    final Bills created = getDraftBill(caseReferenceNumber, providerId);
    return created == null
        ? new Bills().lscCaseReferenceNumber(caseReferenceNumber).providerId(providerId)
        : created;
  }

  /**
   * Deletes the provider's draft payments on account for a case. This ports the legacy PUI {@code
   * RemovePaymentOfAccount}, which deletes the POA held for the case and provider. Deleting the OPA
   * assessment data that went with it is the caller's responsibility, as it is in the legacy PUI
   * (the same handler removes the POA OPA sessions).
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the drafts belong to.
   * @param user the logged-in user.
   */
  public void deleteDraftPaymentsOnAccount(
      final String caseReferenceNumber, final String providerId, final UserDetail user) {

    for (final PaymentOnAccountDetail poa :
        getDraftPaymentsOnAccount(caseReferenceNumber, providerId)) {
      if (poa.getId() != null) {
        caabApiClient.removePaymentOnAccount(poa.getId().longValue(), user.getLoginId()).block();
      }
    }
  }

  /**
   * Submits the provider's draft payment on account to EBS and, once accepted, deletes the draft.
   *
   * <p>This ports the legacy PUI {@code FinancialSubmissionHelper.addPoa}: the draft POA (whose
   * line details were written back by the OPA interview) and the completed POA assessment are
   * marshalled into an invoice and sent to EBS via the soa-gateway {@code createInvoice} operation.
   * The invoice returns a reference used to track the submission. On success the draft is removed,
   * as the legacy PUI's post-submission cleanup does; removing the POA OPA sessions is the caller's
   * responsibility, mirroring how the delete journey splits the same work.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the draft belongs to.
   * @param poaAssessment the completed POA assessment, marshalled onto the invoice.
   * @param user the logged-in user.
   * @return the invoice reference returned by EBS.
   */
  public String submitPaymentOnAccount(
      final String caseReferenceNumber,
      final String providerId,
      final AssessmentDetail poaAssessment,
      final UserDetail user) {

    final PaymentOnAccountDetail draft =
        getDraftPaymentsOnAccount(caseReferenceNumber, providerId).stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new CaabApplicationException(
                        "No draft payment on account to submit for case " + caseReferenceNumber));

    final InvoiceDetail invoice =
        new InvoiceDetail().poa(toSoaPaymentOnAccount(draft, poaAssessment, providerId));

    final InvoiceResponse response =
        soaApiClient.createInvoice(invoice, user.getLoginId(), user.getUserType()).block();

    // The invoice reached EBS, so the draft has served its purpose; remove it as the legacy PUI's
    // post-submission cleanup does. A failed submission throws before this, leaving the draft
    // intact.
    deleteDraftPaymentsOnAccount(caseReferenceNumber, providerId, user);

    return response == null ? null : response.getInvoiceReferenceId();
  }

  /**
   * Maps the draft payment on account and its completed assessment onto the soa-gateway invoice
   * payload. The line-detail fields are taken straight from the draft (the OPA interview wrote them
   * there); the gross total is derived from the net and VAT exactly as the draft amount is shown,
   * and the assessment is marshalled as the OPA response. Mirrors the legacy PUI's {@code
   * EBSCreateInvoiceClient.createInvoiceAddRQ(PaymentOnAccount)}.
   */
  private uk.gov.laa.ccms.soa.gateway.model.PaymentOnAccountDetail toSoaPaymentOnAccount(
      final PaymentOnAccountDetail draft,
      final AssessmentDetail poaAssessment,
      final String providerId) {

    return new uk.gov.laa.ccms.soa.gateway.model.PaymentOnAccountDetail()
        .providerId(providerId)
        .caseReferenceNumber(draft.getLscCaseReference())
        .reason(draft.getReason())
        .courtType(draft.getCourtType())
        .dateIncurred(draft.getDateIncurred())
        .actualNetCost(draft.getActualNetCost())
        .vatRate(draft.getVatRate())
        .dtldAssessmentOrderDate(draft.getDtldAssessmentOrderDate())
        .notes(draft.getNotes())
        // calculatedNetCost has no source - the legacy PUI's poaClaim is a transient field it never
        // populates - so it is left unset rather than inventing a value.
        .actualTotalCost(poaTotalCost(draft, taxRatesByCode()))
        .opaResponse(toOpaResponse(poaAssessment, AssessmentRulebase.POA));
  }

  private uk.gov.laa.ccms.soa.gateway.model.AssessmentResult toOpaResponse(
      final AssessmentDetail assessment, final AssessmentRulebase rulebase) {
    final List<uk.gov.laa.ccms.soa.gateway.model.AssessmentResult> results =
        soaApplicationMapper.mapAssessment(assessment, rulebase.getGoalAttributeName());
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Submits the provider's draft bill to EBS and returns the reference tracking the submission.
   *
   * <p>This ports the legacy PUI {@code FinancialSubmissionHelper.addBill}: the draft bill (whose
   * line details were written back by the OPA interview) and the completed billing assessment are
   * marshalled into an invoice and sent to EBS via the soa-gateway {@code createInvoice} operation.
   *
   * <p>On success the draft is removed, as the legacy PUI's post-submission cleanup does; removing
   * the billing OPA sessions is the caller's responsibility, mirroring how the delete journey
   * splits the same work.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the draft belongs to.
   * @param billingAssessment the completed billing assessment, marshalled onto the invoice.
   * @param user the logged-in user.
   * @return the invoice reference returned by EBS.
   */
  public String submitBill(
      final String caseReferenceNumber,
      final String providerId,
      final AssessmentDetail billingAssessment,
      final UserDetail user) {

    final Bills draft = getDraftBill(caseReferenceNumber, providerId);
    if (draft == null) {
      throw new CaabApplicationException("No draft bill to submit for case " + caseReferenceNumber);
    }

    final InvoiceDetail invoice =
        new InvoiceDetail().bill(toSoaBill(draft, billingAssessment, providerId));

    final InvoiceResponse response =
        soaApiClient.createInvoice(invoice, user.getLoginId(), user.getUserType()).block();

    // The invoice reached EBS, so the draft has served its purpose; remove it as the legacy PUI's
    // post-submission cleanup does. A failed submission throws before this, leaving the draft
    // intact.
    removeDraftBill(draft, user);

    return response == null ? null : response.getInvoiceReferenceId();
  }

  /**
   * Copies a rejected bill onto a new draft, seeding the billing pre-population with the answers
   * the copied bill carried.
   *
   * <p>This ports the legacy PUI {@code CopyBill}: EBS is asked for the copied bill's assessment
   * data, which is written to a billing pre-population assessment and picked up when the interview
   * starts, and a draft bill is created so the bill details screen has one to work with.
   *
   * <p>The {@code PROCEEDING} and {@code OPPONENT_OTHER_PARTIES} entities are deliberately dropped.
   * The legacy empties them for the same reason: they belong to the case as it stands now, so
   * carrying another bill's copies forward would seed stale proceedings and opponents. Emptied,
   * they are re-populated from the case when the assessment starts.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the new draft belongs to.
   * @param billingId the billing incident id of the bill being copied.
   * @param user the logged-in user.
   */
  public void copyBill(
      final String caseReferenceNumber,
      final String providerId,
      final String billingId,
      final UserDetail user) {

    final InvoiceDataResponse invoiceData =
        soaApiClient.getInvoiceData(billingId, user.getLoginId(), user.getUserType()).block();

    final AssessmentDetail prepop =
        new AssessmentDetail()
            .name(AssessmentRulebase.BILLING.getPrePopAssessmentName())
            .caseReferenceNumber(caseReferenceNumber)
            .providerId(providerId)
            .status(AssessmentStatus.INCOMPLETE.getStatus())
            .entityTypes(toAssessmentEntityTypes(invoiceData));

    assessmentService.saveAssessment(user, prepop).block();

    createDraftBillIfAbsent(caseReferenceNumber, providerId, user);
  }

  /**
   * Maps the OPA entities EBS returns for an invoice onto the assessment entity types the
   * pre-population is held in. Entity types on the exclusion list keep their place but carry no
   * entities, exactly as the legacy replaces their contents with an empty map.
   */
  private List<AssessmentEntityTypeDetail> toAssessmentEntityTypes(
      final InvoiceDataResponse invoiceData) {

    if (invoiceData == null || invoiceData.getOpaResponse() == null) {
      return List.of();
    }

    return invoiceData.getOpaResponse().stream()
        .map(
            entity ->
                new AssessmentEntityTypeDetail()
                    .name(entity.getEntityName())
                    .entities(
                        isCopyExcluded(entity.getEntityName())
                            ? List.of()
                            : toAssessmentEntities(entity)))
        .toList();
  }

  private boolean isCopyExcluded(final String entityName) {
    return entityName != null
        && COPY_EXCLUDED_ENTITY_TYPES.contains(entityName.trim().toUpperCase());
  }

  private List<AssessmentEntityDetail> toAssessmentEntities(final OpaEntity entity) {
    if (entity.getInstances() == null) {
      return List.of();
    }

    return entity.getInstances().stream()
        .map(
            instance ->
                new AssessmentEntityDetail()
                    .name(instance.getInstanceLabel())
                    .prepopulated(true)
                    .attributes(toAssessmentAttributes(instance)))
        .toList();
  }

  private List<AssessmentAttributeDetail> toAssessmentAttributes(final OpaInstance instance) {
    if (instance.getAttributes() == null) {
      return List.of();
    }

    return instance.getAttributes().stream()
        .map(
            attribute ->
                new AssessmentAttributeDetail()
                    .name(attribute.getAttribute())
                    .type(attribute.getResponseType())
                    .value(attribute.getResponseValue())
                    // The copied answers are the user's own, carried forward as pre-populated
                    // input, which is how the legacy seeds them onto the new session.
                    .prepopulated(true))
        .toList();
  }

  /**
   * Deletes the provider's draft bill for a case. This ports the legacy PUI {@code RemoveBill},
   * which deletes the bill held for the case and provider. Deleting the OPA assessment data that
   * went with it is the caller's responsibility, as it is in the legacy PUI.
   *
   * @param caseReferenceNumber the case reference number.
   * @param providerId the provider the draft belongs to.
   * @param user the logged-in user.
   */
  public void deleteDraftBill(
      final String caseReferenceNumber, final String providerId, final UserDetail user) {
    removeDraftBill(getDraftBill(caseReferenceNumber, providerId), user);
  }

  private void removeDraftBill(final Bills draft, final UserDetail user) {
    if (draft != null && draft.getId() != null) {
      caabApiClient.removeBill(draft.getId(), user.getLoginId()).block();
    }
  }

  /**
   * Maps the draft bill and its completed assessment onto the soa-gateway invoice payload. Mirrors
   * the legacy PUI's {@code EBSCreateInvoiceClient.createInvoiceAddRQ(Bill)}, field for field. The
   * provider firm is the logged-in user's own provider there, not the draft's, so it is passed in.
   */
  private BillDetail toSoaBill(
      final Bills draft, final AssessmentDetail billingAssessment, final String providerId) {

    return new BillDetail()
        .caseReferenceNumber(draft.getLscCaseReferenceNumber())
        .providerFirmId(providerId)
        .typeOfBill(draft.getTypeOfBill())
        .supportingInfo(draft.getSupportingInfo())
        .clientApproval(toBoolean(draft.getClientApproval()))
        .dateSentToClient(draft.getDateSendToClient())
        .clientResponse(draft.getClientResponse())
        .clientObjectionReason(draft.getClientObjectionReason())
        .courtCode(draft.getCourtCode())
        .courtAssessment(toBoolean(draft.getCourtAssessment()))
        .courtAssessmentDate(draft.getCourtAssessmentDate())
        .opaResponse(toOpaResponse(billingAssessment, AssessmentRulebase.BILLING));
  }

  /**
   * The legacy PUI holds the bill's yes/no answers as booleans; the CAAB API stores them as the
   * underlying numeric column, so a set flag is any non-zero value. An absent answer stays absent
   * rather than becoming "no".
   */
  private Boolean toBoolean(final Integer value) {
    return value == null ? null : value != 0;
  }

  /**
   * The cost limit allocated to the provider, which the billing and POA rulebases check the claim
   * against. This ports the legacy PUI {@code CaseHelper.getProviderAllocatedCostLimitation}: the
   * certificate cost limitation from the provider's own statement, falling back to the case's
   * granted cost limitation when EBS holds no statement for that provider.
   *
   * @param statementOfAccount the assembled statement of account display.
   * @param ebsCase the case the statement of account belongs to.
   * @return the allocated cost limit, never {@code null}.
   */
  public BigDecimal getAllocatedCostLimit(
      final StatementOfAccountDisplay statementOfAccount, final ApplicationDetail ebsCase) {

    final BigDecimal certificateCostLimitation =
        Optional.ofNullable(statementOfAccount)
            .map(StatementOfAccountDisplay::getProvider)
            .map(SoaFigureColumn::getCertificateCostLimitation)
            .orElse(null);

    if (certificateCostLimitation != null) {
      return certificateCostLimitation;
    }

    return Optional.ofNullable(ebsCase.getCosts())
        .map(CostStructureDetail::getGrantedCostLimitation)
        .orElse(BigDecimal.ZERO);
  }

  /**
   * Returns the statement-of-account entry for the relevant provider for the supplied case.
   *
   * <p>If the user belongs to the case provider, the case-wide statement is requested; otherwise
   * the request is scoped to the user's provider (to avoid exposing other firms' billing data).
   *
   * @param caseReferenceNumber the case reference number.
   * @param ebsCase the case details from EBS (used to determine the case provider).
   * @param user the logged-in user.
   * @return the matching provider statement, or {@code null} when none can be safely determined.
   */
  public StatementOfAccountDetail getCurrentProviderStatement(
      final String caseReferenceNumber, final ApplicationDetail ebsCase, final UserDetail user) {

    final Long currentProviderId =
        Optional.ofNullable(user.getProvider())
            .map(BaseProvider::getId)
            .map(Integer::longValue)
            .orElse(null);
    final Long caseProviderId = caseProviderId(ebsCase);
    final boolean userBelongsToCurrentProvider =
        currentProviderId != null && currentProviderId.equals(caseProviderId);

    // If the user does not belong to the case's provider and we cannot identify their own
    // provider, there is nothing to scope the query to. An unrestricted query returns every firm's
    // statement and invoices, so return null rather than expose another firm's billing
    // data.
    if (!userBelongsToCurrentProvider && currentProviderId == null) {
      return null;
    }

    // Users outside the case's provider only see their own firm's figures (legacy PUI behaviour).
    final StatementOfAccountDetails response =
        ebsApiClient
            .getStatementOfAccount(
                caseReferenceNumber, userBelongsToCurrentProvider ? null : currentProviderId)
            .block();

    if (response == null) {
      return null;
    }

    final List<StatementOfAccountDetail> statements =
        response.getContent() == null ? List.of() : response.getContent();

    return currentProviderStatement(statements, currentProviderId, userBelongsToCurrentProvider);
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
                    false,
                    invoice.getBillingIncidentId()))
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
