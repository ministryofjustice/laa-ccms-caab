package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROGRESS_STATUS_TYPES;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.OPPONENT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.COMPLETE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.ERROR;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.NOT_STARTED;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.REQUIRED;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.UNCHANGED;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.client.AssessmentApiClient;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.constants.assessment.InstanceMappingPrefix;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Service class for handling operations related to assessments. This class provides
 * methods to retrieve, manipulate, and evaluate assessment data using the assessment
 * API client and a lookup service. It facilitates operations such as fetching assessments
 * based on specific criteria, determining the status of assessments, and retrieving
 * detailed information about assessment entities and attributes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

  private final AssessmentApiClient assessmentApiClient;
  private final LookupService lookupService;

  /**
   * Retrieves assessments matching the given criteria from the assessment API client.
   *
   * @param assessmentName the name of the assessment to retrieve
   * @param providerId the ID of the provider associated with the assessment
   * @param caseReferenceNumber the case reference number associated with the assessment
   * @param status the status of the assessment to filter by
   * @return a Mono that emits the requested assessments
   */
  public Mono<AssessmentDetails> getAssessments(
      final String assessmentName,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {
    return assessmentApiClient.getAssessments(
        assessmentName, providerId, caseReferenceNumber, status);
  }


  /**
   * Retrieves the most recent assessment detail from a list of assessments based on the
   * last saved date within their audit details.
   *
   * @param assessments the list of assessment details
   * @return the most recent assessment detail, or null if the list is empty or null
   */
  public AssessmentDetail getMostRecentAssessmentDetail(final List<AssessmentDetail> assessments) {
    return assessments != null ? assessments.stream()
        .max(Comparator.comparing(assessment -> assessment.getAuditDetail().getLastSaved(),
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null) : null;
  }

  /**
   * Calculates and updates the statuses for both means and merits assessments of an application.
   * It determines which assessment was last saved to set the isMeansLast flag and applies status
   * updates based on whether assessments have started or changed.
   *
   * @param application the application whose assessments are being evaluated
   * @param meansAssessment the means assessment details
   * @param meritsAssessment the merits assessment details
   * @param user the user performing the update operation
   */
  public void calculateAssessmentStatuses(
      final ApplicationDetail application,
      final AssessmentDetail meansAssessment,
      final AssessmentDetail meritsAssessment,
      final UserDetail user) {
    log.info("Calculating assessment statuses");

    if (meansAssessment != null) {
      if (application.getMeansAssessmentStatus() == null) {
        if (!NOT_STARTED.getStatus().equals(meansAssessment.getStatus())) {
          application.setMeansAssessmentAmended(true);
        }
      } else if (!application.getMeansAssessmentStatus().equals(meansAssessment.getStatus())) {
        application.setMeansAssessmentAmended(true);
      }
    }

    log.info("Calculating means assessment status");
    calculateAssessmentStatus(meansAssessment, application, user);

    log.info("Calculating merits assessment status");
    calculateAssessmentStatus(meritsAssessment, application, user);
  }

  /**
   * Calculates and updates the status of the provided assessment based on specific criteria,
   * including the amendment state of the application and whether a reassessment is required.
   * The status is updated through an API client and also set on the application object.
   *
   * @param currentAssessment the assessment whose status needs to be evaluated
   * @param application the application related to the assessment
   * @param user the user performing the update operation
   */
  private void calculateAssessmentStatus(
      final AssessmentDetail currentAssessment,
      final ApplicationDetail application,
      final UserDetail user) {

    boolean statusChanged = false;
    String statusKey = getStatus(currentAssessment);

    if (!application.getAmendment()
        || (isApplicationsAssessmentAmended(application, currentAssessment))) {

      if (COMPLETE.getStatus().equalsIgnoreCase(statusKey)
          || ERROR.getStatus().equalsIgnoreCase(statusKey)) {

        if (isReassessmentRequired(application, currentAssessment)) {
          statusKey = REQUIRED.getStatus();
          statusChanged = true;
        }
      }
    } else {
      if (isReassessmentRequired(application, currentAssessment)) {
        statusKey = REQUIRED.getStatus();
      } else {
        statusKey = UNCHANGED.getStatus();
      }
      statusChanged = true;
    }

    // update the assessment status if it has changed
    if (statusChanged && currentAssessment != null) {
      assessmentApiClient.updateAssessment(
              currentAssessment.getId(),
              user.getLoginId(),
              new PatchAssessmentDetail().status(statusKey))
          .block();
    }

    setAssessmentStatusOnApplication(application, currentAssessment, statusKey);

  }

  /**
   * Sets the status of an assessment on the application object based on the status key provided.
   * The status is determined by querying a lookup service and updating the corresponding
   * application field depending on the type of the assessment.
   *
   * @param application the application to update with the new assessment status
   * @param assessment the assessment whose status is being updated
   * @param statusKey the key used to retrieve the status description from a lookup service
   */
  private void setAssessmentStatusOnApplication(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final String statusKey) {

    if (assessment != null) {
      final String statusValue =
          lookupService.getCommonValue(COMMON_VALUE_PROGRESS_STATUS_TYPES, statusKey)
              .flatMap(commonLookupValueDetailOptional ->
                  commonLookupValueDetailOptional
                      .map(commonLookupValueDetail ->
                          Mono.just(commonLookupValueDetail.getDescription()))
                      .orElseGet(Mono::empty)
              )
              .blockOptional()
              .orElse(statusKey);

      if (MEANS.getName().equalsIgnoreCase(assessment.getName())) {
        application.setMeansAssessmentStatus(statusValue);
      } else if (MERITS.getName().equalsIgnoreCase(assessment.getName())) {
        application.setMeritsAssessmentStatus(statusValue);
      }
    }
  }

  /**
   * Determines if the assessment of an application has been amended based on the type of assessment
   * provided, whether means or merits.
   *
   * @param application the application to check for amendments
   * @param assessment the assessment details to determine the type
   * @return true if the specified assessment type has been amended; false otherwise
   */
  private boolean isApplicationsAssessmentAmended(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    if (assessment != null) {
      if (MEANS.getName().equalsIgnoreCase(assessment.getName())) {
        return application.getMeansAssessmentAmended();
      } else if (MERITS.getName().equalsIgnoreCase(assessment.getName())) {
        return application.getMeritsAssessmentAmended();
      }
    }
    return false;
  }


  /**
   * Determines if a reassessment is required based on changes between an
   * application and its latest assessment.
   *
   * @param application the application details to compare against the assessment
   * @param assessment the assessment details to compare against the application
   * @return true if a reassessment is necessary; false otherwise
   */
  protected boolean isReassessmentRequired(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    if (!application.getAmendment()) {
      if (assessment != null) {

        final AssessmentEntityTypeDetail assessmentEntityType =
            getAssessmentEntityType(assessment, PROCEEDING.getType());

        //check for any proceeding attribute changes between assessment and application
        if (checkAssessmentForProceedingKeyChange(application, assessmentEntityType)) {
          return true;
        }

        //check if proceeding is deleted
        if (application.getProceedings() != null) {
          log.debug("app.getProceedings().size(): {} - opaListEntity.getOpaEntities().size(): {}",
              application.getProceedings().size(), assessmentEntityType.getEntities().size());

          if (application.getProceedings().size() < assessmentEntityType.getEntities().size()) {
            log.debug("When proceeding is deleted condition - returns TRUE");
            return true;
          }
        }

        //used to check if the proceeding is deleted - this should be included for both assessments,
        //although not specifically needed for means, both assessments and application data should
        //be kept in sync.
        if (Boolean.TRUE.equals(application.getMeritsReassessmentRequired())) {
          log.info(
              "Reassessment Required for {} as application.getMeritsReassessmentRequired() "
                  + "IS TRUE", application.getCaseReferenceNumber());
          return true;
        }

        //used specifically for merits - this should be included for both assessments,
        //although not specifically needed for means, both assessments and application data should
        //be kept in sync.
        for (final Opponent opponent : application.getOpponents()) {
          if ("Individual".equalsIgnoreCase(opponent.getType()) && differenceGreaterThanTenSecs(
              opponent.getAuditTrail().getLastSaved(),
              assessment.getAuditDetail().getCreated())) {

            log.info("Reassessment Required for {} as When individual is updated - IS TRUE",
                application.getCaseReferenceNumber());
            return true;
          }
        }

        //When Individual/Organisation deleted
        //used specifically for merits - this should be included for both assessments,
        //although not specifically needed for means, both assessments and application data should
        //be kept in sync.
        final AssessmentEntityTypeDetail opponentEntityType =
            getAssessmentEntityType(assessment, OPPONENT.getType());
        if ((application.getOpponents() != null) && (opponentEntityType != null)) {
          if (application.getOpponents().size() < opponentEntityType.getEntities().size()) {
            log.info(
                "Reassessment Required for {} as When organisation/individual is deleted "
                    + "condition - IS TRUE", application.getCaseReferenceNumber());
            return true;
          }
        }

        if (application.getCostLimit() == null
            || application.getCostLimit().getLimitAtTimeOfMerits() == null
            || application.getCostLimit().getLimitAtTimeOfMerits()
            .compareTo(application.getCosts().getRequestedCostLimitation()) < 0) {

          log.info("Merit Reassessment Required for {} as app.getCostLimitAtTimeOfMerits() == null "
                  + "|| app.getCostLimitAtTimeOfMerits().compareTo(app.getCosts()"
                  + ".getRequestedOrDefaultCostLimitation()) < 0 IS TRUE",
              application.getCaseReferenceNumber());
          return true;
        }

      }
    } else {
      //todo amendment - need an EBS Case to workout if its required
      //if (application.getAmendment()){
      //  if (meansAssessment == null && !ebsCase.hasEbsAmendments()
      //    && application.getApplicationType().getId().equals("SUBSTANTIVE")
      //    && ebsCase.getCertificateType().getId().equals("EMERGENCY")
      //  ){
      //    return true;
      //  }
      //}
    }

    return false;
  }

  /**
   * Checks if there are any discrepancies between the key data of the proceedings in the
   * application and their corresponding assessment records based on the specified assessment
   * entity type.
   *
   * @param application the application containing proceedings to check
   * @param assessmentEntityType the type of assessment entity to match against proceedings
   * @return true if any discrepancies are found; false otherwise
   */
  protected boolean checkAssessmentForProceedingKeyChange(
      final ApplicationDetail application,
      final AssessmentEntityTypeDetail assessmentEntityType) {

    if (assessmentEntityType != null && application.getProceedings() != null) {
      for (final Proceeding proceeding : application.getProceedings()) {
        final String matterType = proceeding.getMatterType().getId();
        final String proceedingType = proceeding.getProceedingType().getId();
        final String clientInvolvementType = proceeding.getClientInvolvement().getId();
        final String entityId = getOpaInstanceMappingId(proceeding);

        //find entity in entity type where matched entity id
        final AssessmentEntityDetail assessmentEntity =
            getAssessmentEntity(assessmentEntityType, entityId);

        if (assessmentEntity != null) {
          final AssessmentAttributeDetail matterTypeAttribute =
              getAssessmentAttribute(assessmentEntity,
                  AssessmentAttribute.MATTER_TYPE.getAttribute());
          final AssessmentAttributeDetail proceedingTypeAttribute =
              getAssessmentAttribute(assessmentEntity,
                  AssessmentAttribute.PROCEEDING_NAME.getAttribute());
          final AssessmentAttributeDetail clientInvolvementTypeAttribute =
              getAssessmentAttribute(assessmentEntity,
                  AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE.getAttribute());

          if (!matterType.equals(matterTypeAttribute.getValue())
              || !proceedingType.equals(proceedingTypeAttribute.getValue())
              || !clientInvolvementType.equals(clientInvolvementTypeAttribute.getValue())) {
            return true;
          }

          //Check scope limitations for both, although not specifically needed for means, both
          //assessments and application data should be kept in sync.
          final AssessmentAttributeDetail scopeLimitationAttribute =
              getAssessmentAttribute(assessmentEntity,
                  AssessmentAttribute.REQUESTED_SCOPE.getAttribute());

          if (scopeLimitationAttribute != null) {
            final String assessmentScopeLimitation = scopeLimitationAttribute.getValue();
            final String applicationScopeLimitation =
                getRequestedScopeForAssessmentInput(proceeding);
            if (!assessmentScopeLimitation.equals(applicationScopeLimitation)) {
              return true;
            }
          }

        } else {
          return true;
        }
      }
    } else {
      return true;
    }

    return false;
  }

  /**
   * Retrieves the status of an assessment, defaulting to "Not Started" if the assessment is null.
   *
   * @param assessment the assessment to check the status of
   * @return the status of the assessment or "Not Started" if assessment is null
   */
  private String getStatus(final AssessmentDetail assessment) {
    String assessmentStatus = NOT_STARTED.getStatus();
    if (assessment != null) {
      assessmentStatus = assessment.getStatus();
    }
    return assessmentStatus;
  }

  /**
   * Checks if the difference between two dates is greater than ten seconds.
   *
   * @param keyChangeDate the first date to compare
   * @param comparedDate the second date to compare
   * @return true if the difference is greater than ten seconds, otherwise false
   */
  private boolean differenceGreaterThanTenSecs(final Date keyChangeDate, final Date comparedDate) {
    final long keyChangeMillis = keyChangeDate.getTime();
    final long comparedMillis = comparedDate.getTime();

    final long differenceInSeconds = (keyChangeMillis - comparedMillis) / 1000;
    return differenceInSeconds > 10;
  }

  /**
   * Retrieves the most recent date on which any key data was modified within the given
   * application. This includes checks across proceedings and opponents involved in the
   * application.
   *
   * @param application the application to check for recent key data changes
   * @return the most recent date of modification, or null if no modifications have occurred
   */
  private Date getDateOfLatestKeyChange(final ApplicationDetail application) {
    Date latestKeyChange = null;

    for (final Proceeding proceeding : application.getProceedings()) {
      if (proceeding.getAuditTrail() != null
          && (latestKeyChange == null || latestKeyChange
              .before(proceeding.getAuditTrail().getLastSaved()))) {
        latestKeyChange = proceeding.getAuditTrail().getLastSaved();
      }

      if (proceeding.getScopeLimitations() != null && !proceeding.getScopeLimitations().isEmpty()) {
        for (final ScopeLimitation scopeLimitation : proceeding.getScopeLimitations()) {
          if (scopeLimitation.getAuditTrail() != null
              && (latestKeyChange == null || latestKeyChange
                  .before(scopeLimitation.getAuditTrail().getLastSaved()))) {
            latestKeyChange = scopeLimitation.getAuditTrail().getLastSaved();
          }
        }
      }

    }

    for (final Opponent opponent : application.getOpponents()) {
      if (opponent.getAuditTrail() != null
          && (latestKeyChange == null || latestKeyChange
              .before(opponent.getAuditTrail().getLastSaved()))) {
        latestKeyChange = opponent.getAuditTrail().getLastSaved();
      }
    }
    return latestKeyChange;

  }

  /**
   * Determines the unique mapping ID for an object, which could be either a Proceeding or an
   * Opponent. If the object's EBS ID is null, constructs a new ID using the appropriate prefix and
   * object's ID. If EBS ID is not null, it returns the EBS ID directly.
   *
   * @param opponentOrProceeding the object to determine the mapping ID for, which could be an
   *                             instance of Proceeding or Opponent.
   * @return the mapping ID or null if the object type is neither Proceeding nor Opponent
   */
  private String getOpaInstanceMappingId(final Object opponentOrProceeding) {
    if (opponentOrProceeding instanceof final Proceeding proceeding) {
      if (proceeding.getEbsId() == null) {
        return InstanceMappingPrefix.PROCEEDING.getPrefix() + proceeding.getId();
      }
      return proceeding.getEbsId();
    } else if (opponentOrProceeding instanceof final Opponent opponent) {
      if (opponent.getEbsId() == null) {
        return InstanceMappingPrefix.OPPONENT.getPrefix() + opponent.getId();
      }
      return opponent.getEbsId();
    }
    return null;
  }

  /**
   * Retrieves the scope limitation identifier for a given proceeding. If multiple scope limitations
   * exist, returns "MULTIPLE". Otherwise, returns the identifier of the single scope limitation.
   *
   * @param proceeding the proceeding to evaluate for scope limitations
   * @return the scope limitation identifier or "MULTIPLE"
   */
  private String getRequestedScopeForAssessmentInput(final Proceeding proceeding) {
    String scopeLimitations = null;
    if (proceeding.getScopeLimitations().size() > 1) {
      scopeLimitations = "MULTIPLE";
    } else {
      for (final ScopeLimitation scopeLimitation : proceeding.getScopeLimitations()) {
        scopeLimitations = scopeLimitation.getScopeLimitation().getId();
        break;
      }
    }
    return scopeLimitations;
  }

  /**
   * Retrieves the specific type of assessment entity from the assessment details.
   *
   * @param assessment the assessment containing entity types
   * @param entityTypeName the name of the entity type to retrieve
   * @return the matching assessment entity type detail, or null if not found
   */
  private AssessmentEntityTypeDetail getAssessmentEntityType(
      final AssessmentDetail assessment,
      final String entityTypeName) {

    return assessment.getEntityTypes().stream()
        .filter(entityType -> entityType.getName().equalsIgnoreCase(entityTypeName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves a specific assessment entity from the given assessment entity type.
   *
   * @param assessmentEntityType the assessment entity type containing entities
   * @param entityName the name of the entity to retrieve
   * @return the matching assessment entity detail, or null if not found
   */
  private AssessmentEntityDetail getAssessmentEntity(
      final AssessmentEntityTypeDetail assessmentEntityType,
      final String entityName) {

    return assessmentEntityType.getEntities().stream()
        .filter(entity -> entity.getName().equals(entityName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves a specific assessment attribute from the given assessment entity.
   *
   * @param assessmentEntity the assessment entity containing attributes
   * @param attributeName the name of the attribute to retrieve
   * @return the matching assessment attribute detail, or null if not found
   */
  private AssessmentAttributeDetail getAssessmentAttribute(
      final AssessmentEntityDetail assessmentEntity,
      final String attributeName) {

    return assessmentEntity.getAttributes().stream()
        .filter(attribute -> attribute.getName().equalsIgnoreCase(attributeName))
        .findFirst()
        .orElse(null);
  }


}
