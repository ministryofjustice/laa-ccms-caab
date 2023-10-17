package uk.gov.laa.ccms.caab.builders;

import java.util.List;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryStatusDisplay;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
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
  public static final String TYPE_ORGANISATION = "Organisation";

  /**
   * Default builder method for application builder summary.
   *
   * @param auditDetail used to populate multiple summary status displays
   */
  public ApplicationSummaryBuilder(AuditDetail auditDetail) {
    ApplicationSummaryStatusDisplay commonStatusDisplay = ApplicationSummaryStatusDisplay.builder()
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
  public ApplicationSummaryBuilder clientFullName(String firstName, String lastName) {
    applicationSummary.setClientFullName(firstName + " " + lastName);
    return this;
  }

  /**
   * Builder method for case reference number.
   *
   * @param caseReferenceNumber the applications case reference number.
   * @return the builder with amended case reference number details.
   */
  public ApplicationSummaryBuilder caseReferenceNumber(String caseReferenceNumber) {
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
      String providerCaseReferenceNumber) {
    applicationSummary.setProviderCaseReferenceNumber(providerCaseReferenceNumber);
    return this;
  }

  /**
   * Builder method for application type.
   *
   * @param applicationTypeDisplayValue the application's type display value.
   * @return the builder with amended application type details.
   */
  public ApplicationSummaryBuilder applicationType(String applicationTypeDisplayValue) {
    applicationSummary.getApplicationType().setStatus(applicationTypeDisplayValue);
    return this;
  }

  /**
   * Builder method for provider details.
   *
   * @param providerContact the provider's contact information.
   * @return the builder with amended provider details.
   */
  public ApplicationSummaryBuilder providerDetails(StringDisplayValue providerContact) {
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
  public ApplicationSummaryBuilder generalDetails(Address address) {
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
      List<Proceeding> proceedings,
      List<PriorAuthority> priorAuthorities,
      CostStructure costs) {
    String status = NOT_STARTED;
    if (!proceedings.isEmpty()) {
      if (!priorAuthorities.isEmpty()) {
        status = STARTED;
      }
      boolean isComplete = proceedings.stream()
            .anyMatch(proc -> proc.getStage() != null);
      if (isComplete) {
        status = COMPLETE;
      }
    } else if (!priorAuthorities.isEmpty()) {
      status = STARTED;
    }
    applicationSummary.getProceedingsAndCosts().setStatus(status);

    for (Proceeding proceeding : proceedings) {
      checkAndSetLastSaved(
          applicationSummary.getProceedingsAndCosts(),
          proceeding.getAuditTrail());
    }

    checkAndSetLastSaved(
        applicationSummary.getProceedingsAndCosts(),
        costs.getAuditTrail());

    for (PriorAuthority priorAuthority : priorAuthorities) {
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
      List<Opponent> opponents,
      List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      List<RelationshipToCaseLookupValueDetail> personRelationships) {

    if (opponents.isEmpty()) {
      applicationSummary.getOpponentsAndOtherParties().setStatus(NOT_STARTED);
    } else {
      boolean opponentCreated = opponents.stream()
          .anyMatch(opponent -> isOpponentCreated(
              opponent,
              organisationRelationships,
              personRelationships));
      applicationSummary.getOpponentsAndOtherParties().setStatus(
          opponentCreated ? COMPLETE : STARTED);
    }

    for (Opponent opponent : opponents) {
      checkAndSetLastSaved(
          applicationSummary.getOpponentsAndOtherParties(),
          opponent.getAuditTrail());
    }

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
      ApplicationSummaryStatusDisplay statusDisplay,
      AuditDetail newInfo) {
    if ((newInfo.getLastSaved() != null
        && statusDisplay.getLastSaved() != null
        && statusDisplay.getLastSaved().compareTo(newInfo.getLastSaved()) < 0)
        || statusDisplay.getLastSaved() == null) {
      statusDisplay.setLastSaved(newInfo.getLastSaved());
      statusDisplay.setLastSavedBy(newInfo.getLastSavedBy());
    }
  }

  private boolean isOpponentCreated(
      Opponent opponent,
      List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      List<RelationshipToCaseLookupValueDetail> personRelationships) {

    List<RelationshipToCaseLookupValueDetail> relationships =
        opponent.getType().equalsIgnoreCase(TYPE_ORGANISATION)
        ? organisationRelationships
        : personRelationships;

    return relationships.stream()
        .anyMatch(item -> item.getCode().equals(
            opponent.getRelationshipToCase()) && item.getOpponentInd());
  }
}