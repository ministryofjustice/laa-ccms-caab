package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_COMPLETE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_NOT_AVAILABLE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_NOT_STARTED;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_STARTED;

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
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionStatusDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationTypeSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ClientSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.GeneralDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.LinkedCaseDisplay;
import uk.gov.laa.ccms.caab.model.sections.LinkedCasesDisplaySection;
import uk.gov.laa.ccms.caab.model.sections.OpponentSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.PriorAuthoritySectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProceedingSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProceedingsAndCostsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProviderSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ScopeLimitationSectionDisplay;
import uk.gov.laa.ccms.caab.util.DisplayUtil;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/**
 * Helper class for constructing an
 * {@link uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay}
 * instance using a builder pattern.
 */
public class ApplicationSectionsBuilder {

  private final ApplicationSectionDisplay applicationSections;
  protected static final String TYPE_ORGANISATION = "Organisation";

  /**
   * Default builder method for application builder section.
   *
   * @param auditDetail used to populate multiple section status displays
   */
  public ApplicationSectionsBuilder(final AuditDetail auditDetail) {
    this.applicationSections = ApplicationSectionDisplay.builder()
        .applicationType(ApplicationTypeSectionDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .provider(ProviderSectionDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .generalDetails(GeneralDetailsSectionDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .client(ClientSectionDisplay.builder()
            .status(SECTION_STATUS_COMPLETE)
            .build())
        .proceedingsAndCosts(ProceedingsAndCostsSectionDisplay.builder()
            .lastSavedBy(auditDetail.getLastSavedBy())
            .lastSaved(auditDetail.getLastSaved())
            .build())
        .build();
  }

  /**
   *
   */
  public ApplicationSectionsBuilder() {
    this.applicationSections = ApplicationSectionDisplay.builder().build();
  }

  /**
   * Builder method for case reference number.
   *
   * @param caseReferenceNumber the applications case reference number.
   * @return the builder with amended case reference number details.
   */
  public ApplicationSectionsBuilder caseReferenceNumber(final String caseReferenceNumber) {
    applicationSections.setCaseReferenceNumber(caseReferenceNumber);
    return this;
  }

  /**
   * @param linkedCaseDetails
   * @return
   */
  public ApplicationSectionsBuilder linkedCases(final List<LinkedCaseDetail> linkedCaseDetails) {
    List<LinkedCaseDisplay> list = linkedCaseDetails.stream()
        .map(linkedCaseDetail ->
            new LinkedCaseDisplay(linkedCaseDetail.getLscCaseReference(),
                linkedCaseDetail.getRelationToCase()))
        .toList();
    applicationSections.setLinkedCasesDisplaySection(new LinkedCasesDisplaySection(list));
    return this;
  }
  /**
   * Builder method for general details.
   *
   * @param correspondenceMethod the chosen correspondence method.
   * @return the builder with amended general details.
   */
  public ApplicationSectionsBuilder generalDetails(
      final StringDisplayValue applicationStatus,
      final StringDisplayValue categoryOfLaw,
      final String correspondenceMethod) {

    final GeneralDetailsSectionDisplay generalDetails = applicationSections.getGeneralDetails();
    generalDetails.setApplicationStatus(DisplayUtil.getDisplayValue(applicationStatus));
    generalDetails.setCategoryOfLaw(DisplayUtil.getDisplayValue(categoryOfLaw));
    generalDetails.setCorrespondenceMethod(correspondenceMethod);

    if (StringUtils.hasText(correspondenceMethod)) {
      generalDetails.setStatus(SECTION_STATUS_COMPLETE);
    } else {
      generalDetails.setStatus(SECTION_STATUS_STARTED);
    }
    return this;
  }


  /**
   * Builder method for client summary.
   *
   * @param client the application's client
   * @return the builder with amended client section.
   */
  public ApplicationSectionsBuilder client(
      final ClientDetail client) {
    applicationSections.getClient().setClientFullName(
        DisplayUtil.getFullName(client.getFirstName(), client.getSurname()));
    applicationSections.getClient().setClientReferenceNumber(client.getReference());
    return this;
  }

  /**
   * Builder method for application type.
   *
   * @param applicationType the application's type.
   * @return the builder with amended application type details.
   */
  public ApplicationSectionsBuilder applicationType(final ApplicationType applicationType) {
    final ApplicationTypeSectionDisplay applicationTypeSection =
        applicationSections.getApplicationType();

    if (applicationType != null) {
      applicationTypeSection.setDescription(applicationType.getDisplayValue());

      final DevolvedPowersDetail devolvedPowers =
          Optional.ofNullable(applicationType.getDevolvedPowers())
              .orElse(new DevolvedPowersDetail());

      applicationTypeSection.setDevolvedPowersDate(devolvedPowers.getDateUsed());
      applicationTypeSection.setDevolvedPowersUsed(devolvedPowers.getUsed());

      //Not equal to ECF set enabled true
      if (!APP_TYPE_EXCEPTIONAL_CASE_FUNDING.equals(applicationType.getId())) {
        applicationTypeSection.setEnabled(true);
      }
    }

    return this;
  }

  /**
   * Builder method for provider summary.
   *
   * @param provider the provider's details.
   * @return the builder with amended provider section.
   */
  public ApplicationSectionsBuilder provider(
      final ApplicationProviderDetails provider) {
    final ProviderSectionDisplay providerSection = applicationSections.getProvider();

    if (provider != null) {
      providerSection.setProviderName(DisplayUtil.getDisplayValue(provider.getProvider()));

      providerSection.setProviderCaseReferenceNumber(provider.getProviderCaseReference());
      providerSection.setProviderContactName(
          DisplayUtil.getDisplayValue(provider.getProviderContact()));
      providerSection.setOfficeName(
          DisplayUtil.getDisplayValue(provider.getOffice()));
      providerSection.setFeeEarner(
          DisplayUtil.getDisplayValue(provider.getFeeEarner()));
      providerSection.setSupervisorName(
          DisplayUtil.getDisplayValue(provider.getSupervisor()));

      if (provider.getProviderContact() != null
          && StringUtils.hasText(DisplayUtil.getDisplayValue(provider.getProviderContact()))) {
        providerSection.setStatus(SECTION_STATUS_COMPLETE);
      } else {
        providerSection.setStatus(SECTION_STATUS_STARTED);
      }
    } else {
      providerSection.setStatus(SECTION_STATUS_NOT_STARTED);
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
  public ApplicationSectionsBuilder proceedingsPriorAuthsAndCosts(
      final List<ProceedingDetail> proceedings,
      final List<PriorAuthorityDetail> priorAuthorities,
      final CostStructureDetail costs) {
    final ProceedingsAndCostsSectionDisplay proceedingsSection =
        applicationSections.getProceedingsAndCosts();
    proceedingsSection.setProceedings(new ArrayList<>());

    String status = SECTION_STATUS_NOT_STARTED;
    if (proceedings != null && !proceedings.isEmpty()) {
      for (final ProceedingDetail proceeding : proceedings) {
        proceedingsSection.getProceedings().add(
            buildProceedingSummary(proceeding));

        checkAndSetLastSaved(
            proceedingsSection,
            proceeding.getAuditTrail());
      }

      if (priorAuthorities != null && !priorAuthorities.isEmpty()) {
        status = SECTION_STATUS_STARTED;
      }
      final boolean isComplete = proceedings.stream()
            .anyMatch(proc -> proc.getStage() != null);
      if (isComplete) {
        status = SECTION_STATUS_COMPLETE;
      }
    } else if (priorAuthorities != null && !priorAuthorities.isEmpty()) {
      status = SECTION_STATUS_STARTED;
    }
    proceedingsSection.setStatus(status);

    if (costs != null) {
      proceedingsSection.setGrantedCostLimitation(costs.getGrantedCostLimitation());
      proceedingsSection.setRequestedCostLimitation(costs.getRequestedCostLimitation());

      checkAndSetLastSaved(
          proceedingsSection,
          costs.getAuditTrail());
    }

    for (final PriorAuthorityDetail priorAuthority : Optional.ofNullable(priorAuthorities)
        .orElse(Collections.emptyList())) {
      applicationSections.getPriorAuthorities().add(
          buildPriorAuthoritySection(priorAuthority));

      checkAndSetLastSaved(
          proceedingsSection,
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
  public ApplicationSectionsBuilder opponentsAndOtherParties(
      final List<OpponentDetail> opponents,
      final List<CommonLookupValueDetail> contactTitles,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships,
      final List<CommonLookupValueDetail> relationshipsToClient) {

    OpponentsSectionDisplay opponentsSection = applicationSections.getOpponentsAndOtherParties();

    if (opponents.isEmpty()) {
      opponentsSection.setStatus(SECTION_STATUS_NOT_STARTED);
    } else {
      final boolean opponentCreated = opponents.stream()
          .anyMatch(opponent -> isOpponentCreated(
              opponent,
              organisationRelationships,
              personRelationships));
      opponentsSection.setStatus(
          opponentCreated ? SECTION_STATUS_COMPLETE : SECTION_STATUS_STARTED);
    }

    opponentsSection.setOpponents(new ArrayList<>());
    for (final OpponentDetail opponent : opponents) {
      opponentsSection.getOpponents().add(buildOpponentSection(
          opponent,
          contactTitles,
          organisationRelationships,
          personRelationships,
          relationshipsToClient));

      checkAndSetLastSaved(
          opponentsSection,
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
  public ApplicationSectionsBuilder assessments(
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
        applicationSections.getMeansAssessment(),
        opponentCreated);

    //merits
    updateAssessmentStatus(
        application,
        meritsAssessment,
        application.getMeritsAssessmentStatus(),
        applicationSections.getMeritsAssessment(),
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
  public ApplicationSectionsBuilder documentUpload(
      final boolean evidenceRequired,
      final boolean allEvidenceProvided) {

    applicationSections.getDocumentUpload().setStatus(evidenceRequired
        ? (allEvidenceProvided ? SECTION_STATUS_COMPLETE : SECTION_STATUS_STARTED) :
        SECTION_STATUS_NOT_AVAILABLE);

    applicationSections.getDocumentUpload().setEnabled(evidenceRequired);

    return this;
  }

  /**
   * Finalizes and returns the constructed ApplicationSectionDisplay instance.
   *
   * @return The constructed ApplicationSectionDisplay.
   */
  public ApplicationSectionDisplay build() {
    return applicationSections;
  }

  /**
   * Checks and sets the last saved details in the status display.
   *
   * @param statusDisplay the status display to update
   * @param newInfo the new audit details to compare and set
   */
  private void checkAndSetLastSaved(
      final ApplicationSectionStatusDisplay statusDisplay,
      final AuditDetail newInfo) {

    if (newInfo != null && ((newInfo.getLastSaved() != null
        && statusDisplay.getLastSaved() != null
        && statusDisplay.getLastSaved().compareTo(newInfo.getLastSaved()) < 0)
        || statusDisplay.getLastSaved() == null)) {
      statusDisplay.setLastSaved(newInfo.getLastSaved());
      statusDisplay.setLastSavedBy(newInfo.getLastSavedBy());
    }
  }

  /**
   * Checks if the opponent is created based on the relationship details.
   *
   * @param opponent the opponent detail
   * @param organisationRelationships the list of organisation relationships
   * @param personRelationships the list of person relationships
   * @return true if the opponent is created, false otherwise
   */
  private boolean isOpponentCreated(
      final OpponentDetail opponent,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships) {

    final List<RelationshipToCaseLookupValueDetail> relationships =
        TYPE_ORGANISATION.equalsIgnoreCase(opponent.getType())
        ? organisationRelationships
        : personRelationships;

    return relationships.stream()
        .anyMatch(item -> item.getCode().equals(
            opponent.getRelationshipToCase()) && item.getOpponentInd());
  }

  /**
   * Updates the assessment status in the status display.
   *
   * @param application the application detail
   * @param assessment the assessment detail
   * @param assessmentStatus the assessment status
   * @param assessmentStatusDisplay the assessment status display
   * @param opponentCreated flag indicating if the opponent is created
   */
  private void updateAssessmentStatus(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final String assessmentStatus,
      final ApplicationSectionStatusDisplay assessmentStatusDisplay,
      final boolean opponentCreated) {
    boolean assessmentsEnabled = true;

    if (application.getProceedings().isEmpty() || !opponentCreated) {
      assessmentStatusDisplay.setStatus(SECTION_STATUS_NOT_AVAILABLE);
      assessmentsEnabled = false;

    } else {

      if (assessment != null) {
        // Update the assessment details
        assessmentStatusDisplay.setStatus(assessmentStatus);
        assessmentStatusDisplay.setLastSaved(assessment.getAuditDetail().getLastSaved());
        assessmentStatusDisplay.setLastSavedBy(assessment.getAuditDetail().getLastSavedBy());
      } else {
        assessmentStatusDisplay.setStatus(SECTION_STATUS_NOT_STARTED);
      }
    }

    // Enable the assessment
    assessmentStatusDisplay.setEnabled(assessmentsEnabled);
  }

  /**
   * Builds a proceeding summary display from the proceeding detail.
   *
   * @param proceeding the proceeding detail
   * @return the proceeding summary display
   */
  private ProceedingSectionDisplay buildProceedingSummary(
      final ProceedingDetail proceeding) {
    return ProceedingSectionDisplay.builder()
        .clientInvolvement(DisplayUtil.getDisplayValue(proceeding.getClientInvolvement()))
        .levelOfService(DisplayUtil.getDisplayValue(proceeding.getLevelOfService()))
        .matterType(DisplayUtil.getDisplayValue(proceeding.getMatterType()))
        .proceedingType(DisplayUtil.getDisplayValue(proceeding.getProceedingType()))
        .scopeLimitations(Optional.ofNullable(proceeding.getScopeLimitations())
            .map(scopeLimitationDetails -> scopeLimitationDetails.stream()
                .map(this::buildScopeLimitationSection).toList())
            .orElse(null))
        .status(DisplayUtil.getDisplayValue(proceeding.getStatus()))
        .build();
  }


  /**
   * Builds a scope limitation section display from the scope limitation detail.
   *
   * @param scopeLimitationDetail the scope limitation detail
   * @return the scope limitation section display
   */
  private ScopeLimitationSectionDisplay buildScopeLimitationSection(
      final ScopeLimitationDetail scopeLimitationDetail) {
    return ScopeLimitationSectionDisplay.builder()
        .scopeLimitation(DisplayUtil.getDisplayValue(scopeLimitationDetail.getScopeLimitation()))
        .wording(scopeLimitationDetail.getScopeLimitationWording())
        .build();
  }

  /**
   * Builds an opponent section display from the opponent detail.
   *
   * @param opponentDetail the opponent detail
   * @param contactTitles the list of contact titles
   * @param organisationRelationships the list of organisation relationships
   * @param personRelationships the list of person relationships
   * @param relationshipsToClient the list of relationships to client
   * @return the opponent section display
   */
  private OpponentSectionDisplay buildOpponentSection(
      final OpponentDetail opponentDetail,
      final List<CommonLookupValueDetail> contactTitles,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships,
      final List<CommonLookupValueDetail> relationshipsToClient) {

    return OpponentSectionDisplay.builder()
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

  /**
   * Builds a prior authority section display from the prior authority detail.
   *
   * @param priorAuthorityDetail the prior authority detail
   * @return the prior authority section display
   */
  private PriorAuthoritySectionDisplay buildPriorAuthoritySection(
      final PriorAuthorityDetail priorAuthorityDetail) {

    return PriorAuthoritySectionDisplay.builder()
        .description(priorAuthorityDetail.getSummary())
        .type(DisplayUtil.getDisplayValue(priorAuthorityDetail.getType()))
        .amountRequested(priorAuthorityDetail.getAmountRequested())
        .status(priorAuthorityDetail.getStatus())
        .build();
  }

}
