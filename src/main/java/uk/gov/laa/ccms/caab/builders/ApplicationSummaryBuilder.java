package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.summary.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ApplicationSummaryStatusDisplay;
import uk.gov.laa.ccms.caab.model.summary.ApplicationTypeSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ClientSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.GeneralDetailsSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentsSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.PriorAuthoritySummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingsAndCostsSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ScopeLimitationSummaryDisplay;
import uk.gov.laa.ccms.caab.util.DisplayUtil;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/**
 * Helper class for constructing an {@link ApplicationSummaryDisplay}
 * instance using a builder pattern.
 */
public class ApplicationSummaryBuilder {

  private final ApplicationSummaryDisplay applicationSummary;

  protected static final String STATUS_COMPLETE = "Complete";
  protected static final String STATUS_NOT_STARTED = "Not started";
  protected static final String STATUS_STARTED = "Started";
  protected static final String STATUS_NOT_AVAILABLE = "Not available";
  protected static final String TYPE_ORGANISATION = "Organisation";

  /**
   * Default builder method for application builder summary.
   *
   * @param auditDetail used to populate multiple summary status displays
   */
  public ApplicationSummaryBuilder(final AuditDetail auditDetail) {
    this.applicationSummary = ApplicationSummaryDisplay.builder()
        .applicationType(ApplicationTypeSummaryDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .provider(ProviderSummaryDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .generalDetails(GeneralDetailsSummaryDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .client(ClientSummaryDisplay.builder()
            .status(STATUS_COMPLETE)
            .build())
        .proceedingsAndCosts(ProceedingsAndCostsSummaryDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .build();
  }

  /**
   * Builder method for case reference number.
   *
   * @param caseReferenceNumber the applications case reference number.
   * @return the builder with amended case reference number details.
   */
  public ApplicationSummaryBuilder caseReferenceNumber(final String caseReferenceNumber) {
    applicationSummary.setCaseReferenceNumber(caseReferenceNumber);
    return this;
  }

  /**
   * Builder method for general details.
   *
   * @param correspondenceMethod the chosen correspondence method.
   * @return the builder with amended general details.
   */
  public ApplicationSummaryBuilder generalDetails(
      final StringDisplayValue applicationStatus,
      final StringDisplayValue categoryOfLaw,
      final String correspondenceMethod) {

    GeneralDetailsSummaryDisplay generalDetails = applicationSummary.getGeneralDetails();
    generalDetails.setApplicationStatus(DisplayUtil.getDisplayValue(applicationStatus));
    generalDetails.setCategoryOfLaw(DisplayUtil.getDisplayValue(categoryOfLaw));
    generalDetails.setCorrespondenceMethod(correspondenceMethod);

    if (StringUtils.hasText(correspondenceMethod)) {
      generalDetails.setStatus(STATUS_COMPLETE);
    } else {
      generalDetails.setStatus(STATUS_STARTED);
    }
    return this;
  }


  /**
   * Builder method for client summary.
   *
   * @param client the application's client
   * @return the builder with amended client summary.
   */
  public ApplicationSummaryBuilder client(
      final ClientDetail client) {
    applicationSummary.getClient().setClientFullName(
        DisplayUtil.getFullName(client.getFirstName(), client.getSurname()));
    applicationSummary.getClient().setClientReferenceNumber(client.getReference());
    return this;
  }

  /**
   * Builder method for application type.
   *
   * @param applicationType the application's type.
   * @return the builder with amended application type details.
   */
  public ApplicationSummaryBuilder applicationType(final ApplicationType applicationType) {
    ApplicationTypeSummaryDisplay applicationTypeSummary = applicationSummary.getApplicationType();

    if (applicationType != null) {
      applicationTypeSummary.setDescription(applicationType.getDisplayValue());

      DevolvedPowersDetail devolvedPowers = Optional.ofNullable(applicationType.getDevolvedPowers())
          .orElse(new DevolvedPowersDetail());

      applicationTypeSummary.setDevolvedPowersDate(devolvedPowers.getDateUsed());
      applicationTypeSummary.setDevolvedPowersUsed(devolvedPowers.getUsed());

      //Not equal to ECF set enabled true
      if (!APP_TYPE_EXCEPTIONAL_CASE_FUNDING.equals(applicationType.getId())) {
        applicationTypeSummary.setEnabled(true);
      }
    }

    return this;
  }

  /**
   * Builder method for provider summary.
   *
   * @param provider the provider's details.
   * @return the builder with amended provider summary.
   */
  public ApplicationSummaryBuilder provider(
      final ApplicationProviderDetails provider) {
    ProviderSummaryDisplay providerSummary = applicationSummary.getProvider();

    if (provider != null) {
      providerSummary.setProviderName(DisplayUtil.getDisplayValue(provider.getProvider()));

      providerSummary.setProviderCaseReferenceNumber(provider.getProviderCaseReference());
      providerSummary.setProviderContactName(
          DisplayUtil.getDisplayValue(provider.getProviderContact()));
      providerSummary.setOfficeName(
          DisplayUtil.getDisplayValue(provider.getOffice()));
      providerSummary.setFeeEarner(
          DisplayUtil.getDisplayValue(provider.getFeeEarner()));
      providerSummary.setSupervisorName(
          DisplayUtil.getDisplayValue(provider.getSupervisor()));

      if (provider.getProviderContact() != null
          && StringUtils.hasText(DisplayUtil.getDisplayValue(provider.getProviderContact()))) {
        providerSummary.setStatus(STATUS_COMPLETE);
      } else {
        providerSummary.setStatus(STATUS_STARTED);
      }
    } else {
      providerSummary.setStatus(STATUS_NOT_STARTED);
    }

    return this;
  }

  /**
   * Builder method for proceedings, prior authorities, and costs.
   *
   * @param proceedings the list of proceedings.
   * @param priorAuthorities the list of prior authorities.
   * @param costs the cost structure information.
   * @return the builder with amended proceedings, prior authorities, and costs details.
   */
  public ApplicationSummaryBuilder proceedingsPriorAuthsAndCosts(
      final List<ProceedingDetail> proceedings,
      final List<PriorAuthorityDetail> priorAuthorities,
      final CostStructureDetail costs) {
    ProceedingsAndCostsSummaryDisplay proceedingsSummary =
        applicationSummary.getProceedingsAndCosts();
    proceedingsSummary.setProceedings(new ArrayList<>());

    String status = STATUS_NOT_STARTED;
    if (proceedings != null && !proceedings.isEmpty()) {
      for (final ProceedingDetail proceeding : proceedings) {
        proceedingsSummary.getProceedings().add(
            buildProceedingSummary(proceeding));

        checkAndSetLastSaved(
            proceedingsSummary,
            proceeding.getAuditTrail());
      }

      if (priorAuthorities != null && !priorAuthorities.isEmpty()) {
        status = STATUS_STARTED;
      }
      final boolean isComplete = proceedings.stream()
            .anyMatch(proc -> proc.getStage() != null);
      if (isComplete) {
        status = STATUS_COMPLETE;
      }
    } else if (priorAuthorities != null && !priorAuthorities.isEmpty()) {
      status = STATUS_STARTED;
    }
    proceedingsSummary.setStatus(status);

    if (costs != null) {
      proceedingsSummary.setGrantedCostLimitation(costs.getGrantedCostLimitation());
      proceedingsSummary.setRequestedCostLimitation(costs.getRequestedCostLimitation());

      checkAndSetLastSaved(
          proceedingsSummary,
          costs.getAuditTrail());
    }

    for (final PriorAuthorityDetail priorAuthority : Optional.ofNullable(priorAuthorities)
        .orElse(Collections.emptyList())) {
      applicationSummary.getPriorAuthorities().add(
          buildPriorAuthoritySummary(priorAuthority));

      checkAndSetLastSaved(
          proceedingsSummary,
          priorAuthority.getAuditTrail());
    }

    return this;
  }



  /**
   * Builder method for opponents and other parties.
   *
   * @param opponents the list of opponents.
   * @param organisationRelationships the list of organisation relationships.
   * @param personRelationships the list of person relationships.
   * @return the builder with amended opponents and other parties details.
   */
  public ApplicationSummaryBuilder opponentsAndOtherParties(
      final List<OpponentDetail> opponents,
      final List<CommonLookupValueDetail> contactTitles,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships,
      final List<CommonLookupValueDetail> relationshipsToClient) {

    OpponentsSummaryDisplay opponentsSummary = applicationSummary.getOpponentsAndOtherParties();

    if (opponents.isEmpty()) {
      opponentsSummary.setStatus(STATUS_NOT_STARTED);
    } else {
      final boolean opponentCreated = opponents.stream()
          .anyMatch(opponent -> isOpponentCreated(
              opponent,
              organisationRelationships,
              personRelationships));
      opponentsSummary.setStatus(
          opponentCreated ? STATUS_COMPLETE : STATUS_STARTED);
    }

    opponentsSummary.setOpponents(new ArrayList<>());
    for (final OpponentDetail opponent : opponents) {
      opponentsSummary.getOpponents().add(buildOpponentSummary(
          opponent,
          contactTitles,
          organisationRelationships,
          personRelationships,
          relationshipsToClient));

      checkAndSetLastSaved(
          opponentsSummary,
          opponent.getAuditTrail());
    }

    return this;
  }

  /**
   * Builder method for both means and merits assessments.
   *
   * @param application               the application details.
   * @param meansAssessment           the means assessment details.
   * @param meritsAssessment          the merits assessment details.
   * @param organisationRelationships the list of organisation relationships.
   * @param personRelationships       the list of person relationships.
   * @return the builder with amended assessment details.
   */
  public ApplicationSummaryBuilder assessments(
      final ApplicationDetail application,
      final AssessmentDetail meansAssessment,
      final AssessmentDetail meritsAssessment,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships
  ) {

    // Check if any opponent has been created
    final boolean opponentCreated = application.getOpponents().stream()
        .anyMatch(opponent -> isOpponentCreated(
            opponent,
            organisationRelationships,
            personRelationships));

    //means
    updateAssessmentStatus(
        application,
        meansAssessment,
        application.getMeansAssessmentStatus(),
        applicationSummary.getMeansAssessment(),
        opponentCreated);

    //merits
    updateAssessmentStatus(
        application,
        meritsAssessment,
        application.getMeritsAssessmentStatus(),
        applicationSummary.getMeritsAssessment(),
        opponentCreated);

    return this;
  }

  /**
   * Builder method for document upload.
   *
   * @param evidenceRequired          flag to indicate that evidence is required.
   * @param allEvidenceProvided       flag to indicate that all evidence has been provided.
   * @return the builder with amended document upload details.
   */
  public ApplicationSummaryBuilder documentUpload(
      final boolean evidenceRequired,
      final boolean allEvidenceProvided) {

    applicationSummary.getDocumentUpload().setStatus(evidenceRequired
        ? (allEvidenceProvided ? STATUS_COMPLETE : STATUS_STARTED) : STATUS_NOT_AVAILABLE);

    applicationSummary.getDocumentUpload().setEnabled(evidenceRequired);

    return this;
  }

  /**
   * Finalizes and returns the constructed ApplicationSummaryDisplay instance.
   *
   * @return The constructed ApplicationSummaryDisplay.
   */
  public ApplicationSummaryDisplay build() {
    return applicationSummary;
  }

  private void checkAndSetLastSaved(
      final ApplicationSummaryStatusDisplay statusDisplay,
      final AuditDetail newInfo) {
    if ((newInfo.getLastSaved() != null
        && statusDisplay.getLastSaved() != null
        && statusDisplay.getLastSaved().compareTo(newInfo.getLastSaved()) < 0)
        || statusDisplay.getLastSaved() == null) {
      statusDisplay.setLastSaved(newInfo.getLastSaved());
      statusDisplay.setLastSavedBy(newInfo.getLastSavedBy());
    }
  }

  private boolean isOpponentCreated(
      final OpponentDetail opponent,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships) {

    final List<RelationshipToCaseLookupValueDetail> relationships =
        opponent.getType().equalsIgnoreCase(TYPE_ORGANISATION)
        ? organisationRelationships
        : personRelationships;

    return relationships.stream()
        .anyMatch(item -> item.getCode().equals(
            opponent.getRelationshipToCase()) && item.getOpponentInd());
  }

  private void updateAssessmentStatus(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final String assessmentStatus,
      final ApplicationSummaryStatusDisplay assessmentStatusDisplay,
      final boolean opponentCreated) {
    boolean assessmentsEnabled = true;

    if (application.getProceedings().isEmpty() || !opponentCreated) {
      assessmentStatusDisplay.setStatus(STATUS_NOT_AVAILABLE);
      assessmentsEnabled = false;

    } else {

      if (assessment != null) {
        // Update the assessment details
        assessmentStatusDisplay.setStatus(assessmentStatus);
        assessmentStatusDisplay.setLastSaved(assessment.getAuditDetail().getLastSaved());
        assessmentStatusDisplay.setLastSavedBy(assessment.getAuditDetail().getLastSavedBy());
      } else {
        assessmentStatusDisplay.setStatus(STATUS_NOT_STARTED);
      }
    }

    // Enable the assessment
    assessmentStatusDisplay.setEnabled(assessmentsEnabled);
  }

  private ProceedingSummaryDisplay buildProceedingSummary(
      final ProceedingDetail proceeding) {
    return ProceedingSummaryDisplay.builder()
        .clientInvolvement(DisplayUtil.getDisplayValue(proceeding.getClientInvolvement()))
        .levelOfService(DisplayUtil.getDisplayValue(proceeding.getLevelOfService()))
        .matterType(DisplayUtil.getDisplayValue(proceeding.getMatterType()))
        .proceedingType(DisplayUtil.getDisplayValue(proceeding.getProceedingType()))
        .scopeLimitations(Optional.ofNullable(proceeding.getScopeLimitations())
            .map(scopeLimitationDetails -> scopeLimitationDetails.stream()
                .map(this::buildScopeLimitationSummary).toList())
            .orElse(null))
        .status(DisplayUtil.getDisplayValue(proceeding.getStatus()))
        .build();
  }

  private ScopeLimitationSummaryDisplay buildScopeLimitationSummary(
      final ScopeLimitationDetail scopeLimitationDetail) {
    return ScopeLimitationSummaryDisplay.builder()
        .scopeLimitation(DisplayUtil.getDisplayValue(scopeLimitationDetail.getScopeLimitation()))
        .wording(scopeLimitationDetail.getScopeLimitationWording())
        .build();
  }

  private OpponentSummaryDisplay buildOpponentSummary(final OpponentDetail opponentDetail,
      final List<CommonLookupValueDetail> contactTitles,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships,
      final List<CommonLookupValueDetail> relationshipsToClient) {

    return OpponentSummaryDisplay.builder()
        .partyName(OpponentUtil.getPartyName(opponentDetail, contactTitles))
        .partyType(opponentDetail.getType())
        .relationshipToCase(
            OpponentUtil.getRelationshipToCase(
                opponentDetail,
                organisationRelationships,
                personRelationships).getDescription())
        .relationshipToClient(
            OpponentUtil.getRelationshipToClient(
                opponentDetail,
                relationshipsToClient).getDescription())
        .build();
  }

  private PriorAuthoritySummaryDisplay buildPriorAuthoritySummary(
      final PriorAuthorityDetail priorAuthorityDetail) {

    return PriorAuthoritySummaryDisplay.builder()
        .description(priorAuthorityDetail.getSummary())
        .type(DisplayUtil.getDisplayValue(priorAuthorityDetail.getType()))
        .amountRequested(priorAuthorityDetail.getAmountRequested())
        .status(priorAuthorityDetail.getStatus())
        .build();
  }

}
