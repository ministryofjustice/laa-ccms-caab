package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;

import java.util.List;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryStatusDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/**
 * Helper class for constructing an {@link uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay}
 * instance using a builder pattern.
 */
public class ApplicationSummaryBuilder {

  private final ApplicationSummaryDisplay applicationSummary;

  private static final String COMPLETE = "Complete";
  private static final String NOT_STARTED = "Not started";
  private static final String STARTED = "Started";
  public static final String NOT_AVAILABLE = "Not available";
  public static final String TYPE_ORGANISATION = "Organisation";

  /**
   * Default builder method for application builder summary.
   *
   * @param auditDetail used to populate multiple summary status displays
   */
  public ApplicationSummaryBuilder(final AuditDetail auditDetail) {
    final ApplicationSummaryStatusDisplay commonStatusDisplay =
        ApplicationSummaryStatusDisplay.builder()
        .lastSavedBy(auditDetail.getLastSavedBy())
        .lastSaved(auditDetail.getLastSaved())
        .build();

    this.applicationSummary = ApplicationSummaryDisplay.builder()
        .applicationType(new ApplicationSummaryStatusDisplay(commonStatusDisplay))
        .providerDetails(new ApplicationSummaryStatusDisplay(commonStatusDisplay))
        .generalDetails(new ApplicationSummaryStatusDisplay(commonStatusDisplay))
        .clientDetails(ApplicationSummaryStatusDisplay.builder()
            .status(COMPLETE).build())
        .proceedingsAndCosts(new ApplicationSummaryStatusDisplay(commonStatusDisplay))
        .build();
  }

  /**
   * Builder method for clients full name.
   *
   * @param firstName the application's clients first name
   * @param lastName the application's client's last name
   * @return the builder with amended clientFullName details.
   */
  public ApplicationSummaryBuilder clientFullName(final String firstName, final String lastName) {
    String fullName = (firstName != null ? firstName : "")
        + " "
        + (lastName != null ? lastName : "");
    applicationSummary.setClientFullName(fullName.trim());
    return this;
  }

  /**
   * Builder method for clients reference number.
   *
   * @param referenceNumber the application's clients reference number
   * @return the builder with amended clientFullName details.
   */
  public ApplicationSummaryBuilder clientReferenceNumber(final String referenceNumber) {
    applicationSummary.setClientReferenceNumber(referenceNumber);
    return this;
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
   * Builder method for provider case reference number.
   *
   * @param providerCaseReferenceNumber the provider's case reference number.
   * @return the builder with amended provider case reference number details.
   */
  public ApplicationSummaryBuilder providerCaseReferenceNumber(
      final String providerCaseReferenceNumber) {
    applicationSummary.setProviderCaseReferenceNumber(providerCaseReferenceNumber);
    return this;
  }

  /**
   * Builder method for application type.
   *
   * @param applicationType the application's type.
   * @return the builder with amended application type details.
   */
  public ApplicationSummaryBuilder applicationType(final ApplicationType applicationType) {
    applicationSummary.getApplicationType().setStatus(applicationType.getDisplayValue());
    //Not equal to ECF set enabled true
    if (!applicationType.getId().equalsIgnoreCase(APP_TYPE_EXCEPTIONAL_CASE_FUNDING)) {
      applicationSummary.getApplicationType().setEnabled(true);
    }
    return this;
  }

  /**
   * Builder method for provider details.
   *
   * @param providerContact the provider's contact information.
   * @return the builder with amended provider details.
   */
  public ApplicationSummaryBuilder providerDetails(final StringDisplayValue providerContact) {
    if (providerContact != null && StringUtils.hasText(providerContact.getDisplayValue())) {
      applicationSummary.getProviderDetails().setStatus(COMPLETE);
    } else {
      applicationSummary.getProviderDetails().setStatus(STARTED);
    }
    return this;
  }

  /**
   * Builder method for general details.
   *
   * @param address the address information.
   * @return the builder with amended general details.
   */
  public ApplicationSummaryBuilder generalDetails(final AddressDetail address) {
    if (address != null && StringUtils.hasText(address.getPreferredAddress())) {
      applicationSummary.getGeneralDetails().setStatus(COMPLETE);
    } else {
      applicationSummary.getGeneralDetails().setStatus(STARTED);
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
  public ApplicationSummaryBuilder proceedingsAndCosts(
      final List<ProceedingDetail> proceedings,
      final List<PriorAuthorityDetail> priorAuthorities,
      final CostStructureDetail costs) {
    String status = NOT_STARTED;
    if (!proceedings.isEmpty()) {
      if (!priorAuthorities.isEmpty()) {
        status = STARTED;
      }
      final boolean isComplete = proceedings.stream()
            .anyMatch(proc -> proc.getStage() != null);
      if (isComplete) {
        status = COMPLETE;
      }
    } else if (!priorAuthorities.isEmpty()) {
      status = STARTED;
    }
    applicationSummary.getProceedingsAndCosts().setStatus(status);

    for (final ProceedingDetail proceeding : proceedings) {
      checkAndSetLastSaved(
          applicationSummary.getProceedingsAndCosts(),
          proceeding.getAuditTrail());
    }

    checkAndSetLastSaved(
        applicationSummary.getProceedingsAndCosts(),
        costs.getAuditTrail());

    for (final PriorAuthorityDetail priorAuthority : priorAuthorities) {
      checkAndSetLastSaved(
          applicationSummary.getProceedingsAndCosts(),
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
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships) {

    if (opponents.isEmpty()) {
      applicationSummary.getOpponentsAndOtherParties().setStatus(NOT_STARTED);
    } else {
      final boolean opponentCreated = opponents.stream()
          .anyMatch(opponent -> isOpponentCreated(
              opponent,
              organisationRelationships,
              personRelationships));
      applicationSummary.getOpponentsAndOtherParties().setStatus(
          opponentCreated ? COMPLETE : STARTED);
    }

    for (final OpponentDetail opponent : opponents) {
      checkAndSetLastSaved(
          applicationSummary.getOpponentsAndOtherParties(),
          opponent.getAuditTrail());
    }

    return this;
  }

  /**
   * Builder method for both means and merits assessments.
   *
   * @param application the application details.
   * @param meritsAssessment the merits assessment details.
   * @param meansAssessment the means assessment details.
   * @param organisationRelationships the list of organisation relationships.
   * @param personRelationships the list of person relationships.
   * @return the builder with amended assessment details.
   */
  public ApplicationSummaryBuilder assessments(
      final ApplicationDetail application,
      final AssessmentDetail meritsAssessment,
      final AssessmentDetail meansAssessment,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships
  ) {

    // Check if any opponent has been created
    final boolean opponentCreated = application.getOpponents().stream()
        .anyMatch(opponent -> isOpponentCreated(
            opponent,
            organisationRelationships,
            personRelationships));

    //merits
    updateAssessmentStatus(
        application,
        meritsAssessment,
        application.getMeritsAssessmentStatus(),
        applicationSummary.getMeritsAssessment(),
        opponentCreated);

    //means
    updateAssessmentStatus(
        application,
        meansAssessment,
        application.getMeansAssessmentStatus(),
        applicationSummary.getMeansAssessment(),
        opponentCreated);

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
      assessmentStatusDisplay.setStatus(NOT_AVAILABLE);
      assessmentsEnabled = false;

    } else {

      if (assessment != null) {
        // Update the assessment details
        assessmentStatusDisplay.setStatus(assessmentStatus);
        assessmentStatusDisplay.setLastSaved(assessment.getAuditDetail().getLastSaved());
        assessmentStatusDisplay.setLastSavedBy(assessment.getAuditDetail().getLastSavedBy());
      } else {
        assessmentStatusDisplay.setStatus(NOT_STARTED);
      }
    }

    // Enable the assessment
    assessmentStatusDisplay.setEnabled(assessmentsEnabled);
  }
}
