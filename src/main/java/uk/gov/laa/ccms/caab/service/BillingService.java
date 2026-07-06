package uk.gov.laa.ccms.caab.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseStatementOfAccount;
import uk.gov.laa.ccms.soa.gateway.model.SoaBillSummary;
import uk.gov.laa.ccms.soa.gateway.model.SoaCostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.SoaInvoice;
import uk.gov.laa.ccms.soa.gateway.model.SoaPoaSummary;
import uk.gov.laa.ccms.soa.gateway.model.SoaStatement;

/**
 * Service responsible for building the Case Statement of Account display from the statement of
 * account returned by the SOA gateway.
 *
 * <p>This ports the legacy PUI {@code PrepareBillSummary} aggregation: the per-firm statements
 * returned by EBS are bucketed into the current provider, prior solicitor and counsel columns, and
 * the invoices are flattened into a single bills/POA list.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

  private static final String ENTITY_TYPE_PROVIDER = "PROVIDER";
  private static final String ENTITY_TYPE_COUNSEL = "COUNSEL";
  private static final String INVOICE_STATUS_DRAFT = "Draft";

  private final SoaApiClient soaApiClient;

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

    final CaseStatementOfAccount soa =
        soaApiClient
            .getCaseStatementOfAccount(caseReferenceNumber, user.getLoginId(), user.getUserType())
            .block();

    final StatementOfAccountDisplay display = new StatementOfAccountDisplay();
    if (soa == null) {
      return display;
    }
    display.setTotal(soa.getTotal());

    final String currentProviderId =
        Optional.ofNullable(user.getProvider())
            .map(provider -> String.valueOf(provider.getId()))
            .orElse(null);
    final String caseProviderId = caseProviderId(ebsCase);
    final boolean userBelongsToCurrentProvider =
        currentProviderId != null && currentProviderId.equals(caseProviderId);
    display.setUserBelongsToCurrentProvider(userBelongsToCurrentProvider);

    final List<SoaStatement> statements =
        soa.getStatements() == null ? List.of() : soa.getStatements();

    final SoaStatement providerStatement =
        currentProviderStatement(statements, currentProviderId, userBelongsToCurrentProvider);
    display.setProviderFirmName(providerFirmName(providerStatement, ebsCase));
    display.setProvider(toColumn(providerStatement));

    if (userBelongsToCurrentProvider) {
      display.setCounsel(sumColumns(byEntityTypeCounsel(statements)));
      display.setPriorSolicitor(sumColumns(priorSolicitors(statements, currentProviderId)));
    } else {
      // When the user does not belong to the case's provider only their own single statement is
      // returned; the counsel and prior solicitor columns are not shown (legacy PUI behaviour).
      display.setCounsel(new SoaFigureColumn());
      display.setPriorSolicitor(new SoaFigureColumn());
    }

    display.setInvoices(flattenNonDraftInvoices(statements));
    return display;
  }

  private String providerFirmName(
      final SoaStatement providerStatement, final ApplicationDetail ebsCase) {
    if (providerStatement != null && providerStatement.getFirmName() != null) {
      return providerStatement.getFirmName();
    }
    return Optional.ofNullable(ebsCase.getProviderDetails())
        .map(details -> details.getProvider())
        .map(provider -> provider.getDisplayValue())
        .orElse(null);
  }

  private String caseProviderId(final ApplicationDetail ebsCase) {
    return Optional.ofNullable(ebsCase.getProviderDetails())
        .map(details -> details.getProvider())
        .map(provider -> provider.getId())
        .map(String::valueOf)
        .orElse(null);
  }

  private SoaStatement currentProviderStatement(
      final List<SoaStatement> statements,
      final String currentProviderId,
      final boolean userBelongsToCurrentProvider) {
    if (statements.isEmpty()) {
      return null;
    }
    if (!userBelongsToCurrentProvider) {
      // Only one statement is returned when viewing another provider's case.
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

  private List<SoaStatement> byEntityTypeCounsel(final List<SoaStatement> statements) {
    return statements.stream()
        .filter(statement -> ENTITY_TYPE_COUNSEL.equalsIgnoreCase(statement.getEntityType()))
        .toList();
  }

  private List<SoaStatement> priorSolicitors(
      final List<SoaStatement> statements, final String currentProviderId) {
    return statements.stream()
        .filter(statement -> ENTITY_TYPE_PROVIDER.equalsIgnoreCase(statement.getEntityType()))
        .filter(
            statement ->
                currentProviderId == null || !currentProviderId.equals(statement.getFirmId()))
        .toList();
  }

  private List<SoaInvoice> flattenNonDraftInvoices(final List<SoaStatement> statements) {
    final List<SoaInvoice> invoices = new ArrayList<>();
    for (final SoaStatement statement : statements) {
      if (statement.getInvoices() == null) {
        continue;
      }
      for (final SoaInvoice invoice : statement.getInvoices()) {
        // EBS draft invoices are backed by the (as yet unbuilt) TDS draft store; do not show them
        // here yet. Submitted / authorised / rejected invoices are the real statement figures.
        if (!INVOICE_STATUS_DRAFT.equalsIgnoreCase(invoice.getInvoiceStatus())) {
          invoices.add(invoice);
        }
      }
    }
    return invoices;
  }

  private SoaFigureColumn toColumn(final SoaStatement statement) {
    final SoaFigureColumn column = new SoaFigureColumn();
    if (statement == null) {
      return column;
    }
    final SoaCostLimitation costLimitation = statement.getCostLimitation();
    final SoaBillSummary bills = statement.getBills();
    final SoaPoaSummary poa = statement.getPoa();

    column.setCertificateCostLimitation(
        costLimitation != null ? costLimitation.getCertificateAmount() : null);
    column.setCostLimitationRemaining(
        costLimitation != null ? costLimitation.getRemainingAmount() : null);
    column.setUndertaking(statement.getUndertakingAmount());
    column.setBillsAuthorised(bills != null ? bills.getAmountAuthorized() : null);
    column.setBillsSubmittedButNotAuthorised(bills != null ? bills.getAmountSubmitted() : null);
    column.setPoaRecouped(poa != null ? poa.getAmountRecouped() : null);
    column.setPoaAuthorised(poa != null ? poa.getAmountAuthorized() : null);
    column.setPoaSubmittedButNotAuthorised(poa != null ? poa.getAmountSubmitted() : null);
    return column;
  }

  private SoaFigureColumn sumColumns(final List<SoaStatement> statements) {
    final SoaFigureColumn total = new SoaFigureColumn();
    for (final SoaStatement statement : statements) {
      final SoaFigureColumn column = toColumn(statement);
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
