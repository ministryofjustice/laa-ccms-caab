package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROGRESS_STATUS_TYPES;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.APP_AMEND_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.DELEGATED_FUNCTIONS_DATE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.REQUESTED_SCOPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.OPPONENT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.COMPLETE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.ERROR;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.INCOMPLETE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.NOT_STARTED;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.REQUIRED;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.UNCHANGED;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getDateOfLatestKeyChange;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentAttribute;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntitiesForEntityType;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntity;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntityType;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getDisplayNameForAttribute;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getEntityRelationship;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getFormattedAttributeValue;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getMostRecentAssessmentDetail;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getRelatedEntities;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.isAssessmentReferenceConsistent;
import static uk.gov.laa.ccms.caab.util.OpponentUtil.getOpponentByEbsId;
import static uk.gov.laa.ccms.caab.util.OpponentUtil.getOpponentById;
import static uk.gov.laa.ccms.caab.util.ProceedingUtil.getAssessmentMappingId;
import static uk.gov.laa.ccms.caab.util.ProceedingUtil.getProceedingByEbsId;
import static uk.gov.laa.ccms.caab.util.ProceedingUtil.getProceedingById;
import static uk.gov.laa.ccms.caab.util.ProceedingUtil.getRequestedScopeForAssessmentInput;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.client.AssessmentApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRelationship;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.constants.assessment.InstanceMappingPrefix;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.AssessmentMapper;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentOpponentMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryAttributeDisplay;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.caab.util.ProceedingUtil;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Service class for handling operations related to assessments. This class provides
 * methods to retrieve, manipulate, and evaluate assessment data using the assessment
 * API client and a lookup service. It facilitates operations such as fetching assessments
 * based on specific criteria, determining the status of assessments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

  private final AssessmentApiClient assessmentApiClient;
  private final CaabApiClient caabApiClient;
  private final AssessmentMapper assessmentMapper;
  private final LookupService lookupService;


  /**
   * Retrieves assessments matching the given criteria from the assessment API client.
   *
   * @param assessmentNames the list of assessment names to retrieve
   * @param providerId the ID of the provider associated with the assessment
   * @param caseReferenceNumber the case reference number associated with the assessment
   * @return a Mono that emits the requested assessments
   */
  public Mono<AssessmentDetails> getAssessments(
      final List<String> assessmentNames,
      final String providerId,
      final String caseReferenceNumber) {
    return assessmentApiClient.getAssessments(
        assessmentNames, providerId, caseReferenceNumber);
  }

  /**
   * Retrieves assessments matching the given criteria from the assessment API client.
   *
   * @param assessmentNames the list of assessment names to retrieve
   * @param providerId the ID of the provider associated with the assessment
   * @param caseReferenceNumber the case reference number associated with the assessment
   * @return a Mono that emits the requested assessments
   */
  public Mono<AssessmentDetails> getAssessments(
      final List<String> assessmentNames,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {
    return assessmentApiClient.getAssessments(
        assessmentNames, providerId, caseReferenceNumber, status);
  }

  /**
   * Deletes assessments matching the given criteria from the assessment API client.
   *
   * @param user the user performing the delete operation
   * @param assessmentNames the list of assessment names to delete
   * @param caseReferenceNumber the case reference number associated with the assessment
   * @param status the status of the assessment to filter by
   * @return a Mono that emits the result of the delete operation
   */
  public Mono<Void> deleteAssessments(
      final UserDetail user,
      final List<String> assessmentNames,
      final String caseReferenceNumber,
      final String status) {
    return assessmentApiClient.deleteAssessments(
        assessmentNames,
        user.getProvider().getId().toString(),
        caseReferenceNumber,
        status,
        user.getLoginId());
  }


  /**
   * Saves an assessment by creating a new one or updating an existing one based on the
   * assessment ID.
   *
   * @param user the user details
   * @param assessment the assessment details
   * @return a Mono indicating when the save operation has completed
   */
  public Mono<Void> saveAssessment(
      final UserDetail user,
      final AssessmentDetail assessment) {
    if (assessment.getId() == null) {
      return assessmentApiClient.createAssessment(
          assessment, user.getLoginId());
    } else {
      return assessmentApiClient.updateAssessment(
          assessment.getId(), assessment, user.getLoginId());
    }
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
    AssessmentStatus assessmentStatus = getStatus(currentAssessment);

    if (!application.getAmendment()
        || (isApplicationsAssessmentAmended(application, currentAssessment))) {

      if (COMPLETE == assessmentStatus || ERROR == assessmentStatus) {

        if (isReassessmentRequired(application, currentAssessment)) {
          assessmentStatus = REQUIRED;
          statusChanged = true;
        }
      }
    } else {
      if (isReassessmentRequired(application, currentAssessment)) {
        assessmentStatus = REQUIRED;
      } else {
        assessmentStatus = UNCHANGED;
      }
      statusChanged = true;
    }

    // update the assessment status if it has changed
    if (statusChanged && currentAssessment != null) {
      assessmentApiClient.patchAssessment(
              currentAssessment.getId(),
              user.getLoginId(),
              new PatchAssessmentDetail().status(assessmentStatus.getStatus()))
          .block();
    }

    setAssessmentStatusOnApplication(application, currentAssessment, assessmentStatus);

  }

  /**
   * Sets the status of an assessment on the application object based on the status key provided.
   * The status is determined by querying a lookup service and updating the corresponding
   * application field depending on the type of the assessment.
   *
   * @param application the application to update with the new assessment status
   * @param assessment the assessment whose status is being updated
   * @param assessmentStatus the AsseessmentStatus to set on the application
   */
  private void setAssessmentStatusOnApplication(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final AssessmentStatus assessmentStatus) {

    if (assessment != null && assessmentStatus != null) {
      final String statusValue =
          lookupService.getCommonValue(COMMON_VALUE_PROGRESS_STATUS_TYPES,
                  assessmentStatus.getStatus())
              .flatMap(commonLookupValueDetailOptional ->
                  commonLookupValueDetailOptional
                      .map(commonLookupValueDetail ->
                          Mono.just(commonLookupValueDetail.getDescription()))
                      .orElseGet(Mono::empty)
              )
              .blockOptional()
              .orElse(assessmentStatus.getStatus());

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

        final AssessmentEntityTypeDetail proceedingEntityType =
            getAssessmentEntityType(assessment, PROCEEDING);

        //check for any proceeding attribute changes between assessment and application
        if (checkAssessmentForProceedingKeyChange(application, proceedingEntityType)) {
          return true;
        }

        //check if proceeding is deleted
        if (application.getProceedings() != null) {
          log.debug("app.getProceedings().size(): {} - opaListEntity.getOpaEntities().size(): {}",
              application.getProceedings().size(), proceedingEntityType.getEntities().size());

          if (application.getProceedings().size() < proceedingEntityType.getEntities().size()) {
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
        for (final OpponentDetail opponent : application.getOpponents()) {
          if (OPPONENT_TYPE_INDIVIDUAL.equalsIgnoreCase(opponent.getType())
              && differenceGreaterThanTenSecs(
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
            getAssessmentEntityType(assessment, OPPONENT);
        if ((application.getOpponents() != null) && (opponentEntityType != null)) {
          if (application.getOpponents().size() < opponentEntityType.getEntities().size()) {
            log.info(
                "Reassessment Required for {} as When organisation/individual is deleted "
                    + "condition - IS TRUE", application.getCaseReferenceNumber());
            return true;
          }
        }

        //only check when it's a merits assessment
        if (assessment.getName().equalsIgnoreCase(MERITS.getName())) {
          final boolean meritReassessmentRequired = Optional.ofNullable(application.getCostLimit())
              .map(CostLimitDetail::getLimitAtTimeOfMerits)
              .map(limitAtTimeOfMerits -> limitAtTimeOfMerits.compareTo(
                  application.getCosts().getRequestedCostLimitation()) < 0)
              .orElse(true);

          if (meritReassessmentRequired) {
            log.info(
                "Merit Reassessment Required for {} as app.getCostLimitAtTimeOfMerits() == null "
                    + "|| app.getCostLimitAtTimeOfMerits().compareTo(app.getCosts()"
                    + ".getRequestedOrDefaultCostLimitation()) < 0 IS TRUE",
                application.getCaseReferenceNumber());
            return true;
          }
        }

      }
    } else {
      // TODO CCMSPUI-738 - Amend case ~ need an EBS Case to workout if its required
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
   * Checks if there are any discrepancies between the key data of the applications type details
   * and their corresponding assessment records based on the specified assessment.
   *
   * @param application the application containing application type details to check
   * @param assessment the assessment data
   * @return true if any discrepancies are found; false otherwise
   */
  protected boolean applicationTypeMatches(
      final ApplicationDetail application, final AssessmentDetail assessment) {

    final List<AssessmentEntityDetail> globalEntities =
        getAssessmentEntitiesForEntityType(assessment, GLOBAL);

    if (globalEntities.isEmpty()) {
      return false; // No entities to compare against
    }

    final String applicationType = application.getApplicationType().getId();

    final Optional<LocalDate> applicationDateUsed = Optional.of(application)
        .map(ApplicationDetail::getApplicationType)
        .map(ApplicationType::getDevolvedPowers)
        .map(DevolvedPowersDetail::getDateUsed)
        .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

    for (final AssessmentEntityDetail globalEntity : globalEntities) {

      final String applicationTypeFromAssessment = Optional.ofNullable(
              getAssessmentAttribute(globalEntity, APP_AMEND_TYPE))
          .map(AssessmentAttributeDetail::getValue)
          .orElse(null);

      final boolean applicationTypeMismatch = (applicationTypeFromAssessment != null
          && (applicationType == null
          || !applicationType.equalsIgnoreCase(applicationTypeFromAssessment)));

      if (applicationTypeMismatch) {
        if (APP_TYPE_EXCEPTIONAL_CASE_FUNDING.equalsIgnoreCase(applicationType)) {
          if (!APP_TYPE_SUBSTANTIVE.equalsIgnoreCase(applicationTypeFromAssessment)) {
            return true;
          }
        } else {
          return true;
        }
      }

      final AssessmentAttributeDetail delegatedFunctionsAttribute =
          getAssessmentAttribute(globalEntity, DELEGATED_FUNCTIONS_DATE);

      if (delegatedFunctionsAttribute != null) {
        final Optional<LocalDate> attributeDate = Optional.ofNullable(
            delegatedFunctionsAttribute.getValue())
            .map(dateStr -> 
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        if (attributeDate.isPresent()) {
          if (!applicationDateUsed.isPresent()
              || !applicationDateUsed.get().isEqual(attributeDate.get())) {
            return true;
          }
        }
      } else if (applicationDateUsed.isPresent()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if there are any discrepancies between the key data of the proceedings in the
   * application and their corresponding assessment records based on the specified assessment
   * entity type.
   *
   * @param application the application containing proceedings to check
   * @param proceedingEntityType the type of assessment entity to match against proceedings
   * @return true if any discrepancies are found; false otherwise
   */
  protected boolean checkAssessmentForProceedingKeyChange(
      final ApplicationDetail application,
      final AssessmentEntityTypeDetail proceedingEntityType) {

    if (proceedingEntityType == null
        && (application.getProceedings() == null || application.getProceedings().isEmpty())) {
      return true;
    }

    for (final ProceedingDetail proceeding : application.getProceedings()) {
      final String matterType = proceeding.getMatterType().getId();
      final String proceedingType = proceeding.getProceedingType().getId();
      final String clientInvolvementType = proceeding.getClientInvolvement().getId();
      final String entityId = ProceedingUtil.getAssessmentMappingId(proceeding);

      //find entity in entity type where matched entity id
      final AssessmentEntityDetail proceedingEntity =
          getAssessmentEntity(proceedingEntityType, entityId);

      if (proceedingEntity == null) {
        return true;
      }

      final AssessmentAttributeDetail matterTypeAttribute =
          getAssessmentAttribute(proceedingEntity,
              AssessmentAttribute.MATTER_TYPE);
      final AssessmentAttributeDetail proceedingTypeAttribute =
          getAssessmentAttribute(proceedingEntity,
              AssessmentAttribute.PROCEEDING_NAME);
      final AssessmentAttributeDetail clientInvolvementTypeAttribute =
          getAssessmentAttribute(proceedingEntity,
              AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE);

      if (!matterType.equals(matterTypeAttribute.getValue())
          || !proceedingType.equals(proceedingTypeAttribute.getValue())
          || !clientInvolvementType.equals(clientInvolvementTypeAttribute.getValue())) {
        return true;
      }

      //Check scope limitations for both, although not specifically needed for means, both
      //assessments and application data should be kept in sync.
      final AssessmentAttributeDetail scopeLimitationAttribute =
          getAssessmentAttribute(proceedingEntity,
              AssessmentAttribute.REQUESTED_SCOPE);

      if (scopeLimitationAttribute != null) {
        final String assessmentScopeLimitation = scopeLimitationAttribute.getValue();
        final String applicationScopeLimitation =
            getRequestedScopeForAssessmentInput(proceeding);
        if (!assessmentScopeLimitation.equals(applicationScopeLimitation)) {
          return true;
        }
      }

    }


    return false;
  }

  /**
   * Retrieves the status of an assessment, defaulting to "Not Started" if the assessment is null.
   *
   * @param assessment the assessment to check the status of
   * @return the status of the assessment or "Not Started" if assessment is null
   */
  private AssessmentStatus getStatus(final AssessmentDetail assessment) {
    AssessmentStatus assessmentStatus = AssessmentStatus.NOT_STARTED;
    if (assessment != null) {
      assessmentStatus = AssessmentStatus.findByStatus(assessment.getStatus());
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
   * Cleans up data by deleting redundant opponents and proceedings from the assessment.
   *
   * @param assessment the assessment detail to clean up
   * @param application the application detail for reference
   */
  public void cleanupData(
      final AssessmentDetail assessment,
      final ApplicationDetail application) {

    if (assessment != null) {
      deleteRedundantOpponents(assessment, application);
      deleteRedundantProceedings(assessment, application);

      if (application != null && application.getAmendment()) {
        //todo - future implementation for amendments
      }
    }
  }

  /**
   * Deletes redundant opponents from the assessment that are not present in the application.
   *
   * @param assessment the assessment details
   * @param application the application details
   */
  private void deleteRedundantOpponents(
      final AssessmentDetail assessment,
      final ApplicationDetail application) {
    final List<String> opponentsToDelete =  getEntitiesToDelete(
        assessment,
        OPPONENT,
        AssessmentRelationship.OPPONENT,
        (list, id) -> addRedundantOpponent(list, id, application));

    deleteEntityAndRelationship(
        assessment,
        OPPONENT,
        AssessmentRelationship.OPPONENT,
        opponentsToDelete);
  }

  /**
   * Deletes redundant proceedings from the assessment that are not present in the application.
   *
   * @param assessment the assessment details
   * @param application the application details
   */
  private void deleteRedundantProceedings(
      final AssessmentDetail assessment,
      final ApplicationDetail application) {
    final List<String> proceedingsToDelete = getEntitiesToDelete(
        assessment,
        PROCEEDING,
        AssessmentRelationship.PROCEEDING,
        (list, id) -> addRedundantProceeding(list, id, application));

    deleteEntityAndRelationship(
        assessment,
        PROCEEDING,
        AssessmentRelationship.PROCEEDING,
        proceedingsToDelete);
  }

  /**
   * Deletes entities and their relationships from the assessment based on the given entity type
   * and relationship type.
   *
   * @param assessment the assessment details
   * @param entityType the type of entities to delete
   * @param relationshipType the type of relationships to delete
   * @param entitiesToDelete the list of entity names to delete
   */
  private void deleteEntityAndRelationship(
      final AssessmentDetail assessment,
      final AssessmentEntityType entityType,
      final AssessmentRelationship relationshipType,
      final List<String> entitiesToDelete) {

    final List<AssessmentEntityDetail> entities =
        getAssessmentEntitiesForEntityType(assessment, entityType);

    for (final String entityName : entitiesToDelete) {
      entities.stream()
          .filter(entity -> entity.getName().equals(entityName))
          .findFirst()
          .ifPresent(entity -> {
            log.debug("Removing entity: {}", entityName);
            entities.remove(entity);
          });
      removeOpaRelationshipTarget(assessment, relationshipType, entityName);
    }
  }

  /**
   * Retrieves entities to delete based on the given entity type, relationship type, and redundant
   * entity method.
   *
   * @param assessment the assessment details
   * @param entityType the type of entities to retrieve
   * @param relationshipType the type of relationships to retrieve
   * @param addRedundantEntityMethod the method to identify redundant entities
   * @return a list of entity names to delete
   */
  private List<String> getEntitiesToDelete(
      final AssessmentDetail assessment,
      final AssessmentEntityType entityType,
      final AssessmentRelationship relationshipType,
      final BiConsumer<List<String>, String> addRedundantEntityMethod) {
    final List<String> entitiesToDelete = new ArrayList<>();

    // Retrieve entities for the specified type
    final List<AssessmentEntityDetail> entities =
        getAssessmentEntitiesForEntityType(assessment, entityType);

    for (final AssessmentEntityDetail entity : entities) {
      log.debug("%s entity ID : %s".formatted(entityType, entity.getName()));
      addRedundantEntityMethod.accept(entitiesToDelete, entity.getName());
    }

    // Process global entities specific to the relationship type
    final List<AssessmentEntityDetail> globalEntities =
        getAssessmentEntitiesForEntityType(assessment, GLOBAL);

    globalEntities.stream()
        .filter(globalEntity -> assessment.getCaseReferenceNumber()
            .equalsIgnoreCase(globalEntity.getName()))
        .map(globalEntity -> getEntityRelationship(globalEntity, relationshipType))
        .filter(Objects::nonNull)
        .flatMap(relationship -> relationship.getRelationshipTargets().stream())
        .forEach(target -> {
          log.debug("%s relationship target entity ID : %s".formatted(
              relationshipType,
              target.getTargetEntityId()));
          addRedundantEntityMethod.accept(entitiesToDelete, target.getTargetEntityId());
        });

    return entitiesToDelete;
  }

  /**
   * Adds a redundant opponent to the list of opponents to delete if it does not exist in the
   * application.
   *
   * @param opponentsToDelete the list of opponents to delete
   * @param entityName the name of the opponent entity
   * @param application the application details
   */
  private void addRedundantOpponent(
      final List<String> opponentsToDelete,
      final String entityName,
      final ApplicationDetail application) {

    addRedundantEntity(opponentsToDelete, entityName, application.getOpponents(), opponent ->
        (opponent.getEbsId() != null && entityName.equalsIgnoreCase(opponent.getEbsId()))
            || (opponent.getId() != null && entityName.equalsIgnoreCase(
                InstanceMappingPrefix.OPPONENT.getPrefix().concat(opponent.getId().toString())))
    );
  }

  /**
   * Adds a redundant proceeding to the list of proceedings to delete if it does not exist in the
   * application.
   *
   * @param proceedingsToDelete the list of proceedings to delete
   * @param entityName the name of the proceeding entity
   * @param application the application details
   */
  private void addRedundantProceeding(
      final List<String> proceedingsToDelete,
      final String entityName,
      final ApplicationDetail application) {

    addRedundantEntity(proceedingsToDelete, entityName, application.getProceedings(), proceeding ->
        (proceeding.getEbsId() != null && entityName.equalsIgnoreCase(proceeding.getEbsId()))
            || (proceeding.getId() != null && entityName.equalsIgnoreCase(
                InstanceMappingPrefix.PROCEEDING.getPrefix().concat(proceeding.getId().toString())))
    );
  }

  /**
   * Adds a redundant entity to the list of entities to delete if it does not exist in the provided
   * list of entities.
   *
   * @param <T> the type of entities in the list
   * @param entitiesToDelete the list of entities to delete
   * @param entityName the name of the entity
   * @param entities the list of entities to check against
   * @param matchCondition the condition to match entities
   */
  private <T> void addRedundantEntity(
      final List<String> entitiesToDelete,
      final String entityName,
      final List<T> entities,
      final Predicate<T> matchCondition) {

    final boolean exists = entities.stream().anyMatch(matchCondition);
    if (!exists) {
      log.debug("Entity ID is redundant:::" + entityName);
      entitiesToDelete.add(entityName);
    } else {
      log.debug("Entity ID isn't redundant:::" + entityName);
    }
  }

  /**
   * Removes a relationship target from the assessment based on the specified entity ID.
   *
   * @param assessment the assessment details
   * @param assessmentRelationship the relationship details
   * @param removeEntityId the ID of the entity to be removed
   */
  protected void removeOpaRelationshipTarget(
      final AssessmentDetail assessment,
      final AssessmentRelationship assessmentRelationship,
      final String removeEntityId) {
    log.debug("removeOpaRelationshipTarget()");

    final List<AssessmentEntityDetail> globalEntities =
        getAssessmentEntitiesForEntityType(assessment, GLOBAL);
    log.debug("globalEntities size : " + globalEntities.size());

    final Optional<AssessmentEntityDetail> globalEntity = globalEntities.stream()
        .filter(entity -> assessment.getCaseReferenceNumber().equalsIgnoreCase(entity.getName()))
        .findFirst();

    globalEntity.ifPresent(entity -> {
      final AssessmentRelationshipDetail relationship =
          getEntityRelationship(globalEntity.get(), assessmentRelationship);

      if (relationship != null) {
        relationship.getRelationshipTargets().removeIf(target -> {
          log.debug("Target Entity ID to be removed : " + target.getTargetEntityId());
          return removeEntityId != null
              && removeEntityId.equalsIgnoreCase(target.getTargetEntityId());
        });
      }
    });
  }

  /**
   * Starts a new assessment by removing any previous assessments and initiating a new one.
   *
   * @param application the application detail for the assessment
   * @param assessmentRulebase the rulebase for the assessment
   * @param client the client detail for the assessment
   * @param user the user detail initiating the assessment
   */
  public void startAssessment(
      final ApplicationDetail application,
      final AssessmentRulebase assessmentRulebase,
      final ClientDetail client,
      final UserDetail user) {

    //remove previous assessment
    deleteAssessments(
        user,
        List.of(assessmentRulebase.getName()),
        application.getCaseReferenceNumber(),
        null).block();

    //start new assessment
    startNewAssessment(assessmentRulebase, application, client, user);
  }

  /**
   * Starts a new assessment based on the given rulebase, application, client, and user details.
   *
   * @param assessmentRulebase the rulebase for the assessment
   * @param application the application details
   * @param client the client details
   * @param user the user details
   */
  protected void startNewAssessment(
      final AssessmentRulebase assessmentRulebase,
      final ApplicationDetail application,
      final ClientDetail client,
      final UserDetail user) {
    log.debug("Name - {}, AssessmentType - {}",
        user.getUsername(), assessmentRulebase.getType());
    final String referenceId = application.getCaseReferenceNumber();
    final String providerId = user.getProvider().getId().toString();

    final boolean prepopulateFromEbs = application.getAmendment();

    final List<AssessmentEntityType> opaEntitiesRetrievedFromEbs = null;

    //find or Create
    final AssessmentDetail assessment = findOrCreate(
        providerId, referenceId, assessmentRulebase.getName());
    final AssessmentDetail prepopAssessment = findOrCreate(
        providerId, referenceId, assessmentRulebase.getPrePopAssessmentName());

    // find or create an opa session
    final boolean createdNewPrepopAssessment = prepopAssessment.getId() == null;

    //used to populate the lookups for title for the opponent
    final List<AssessmentOpponentMappingContext>
        opponentContext = getAssessmentOpponentMappingContexts(application);

    final AssessmentMappingContext assessmentContext = AssessmentMappingContext.builder()
        .application(application)
        .opponentContext(opponentContext)
        .assessment(assessment)
        .client(client)
        .user(user)
        .build();

    //if we create a new prepop assessment, we need to map the context to it,
    // otherwise we will just use the existing one
    if (createdNewPrepopAssessment) {
      assessmentMapper.toAssessmentDetail(prepopAssessment, assessmentContext);
    }

    //always map to the assessment as it should always be new here.
    assessmentMapper.toAssessmentDetail(assessment, assessmentContext);

    updateCostLimitIfMeritsAssessment(assessmentRulebase, application, user);

    if (prepopulateFromEbs) {
      //todo - later implementation in future story
    }

    //if means and merits:
    if (!assessmentRulebase.isFinancialAssessment()) {
      if (isAssessmentReferenceConsistent(prepopAssessment)
          && isAssessmentReferenceConsistent(assessment)) {

        //call opa - save to database
        saveAssessment(user, assessment).block();
        saveAssessment(user, prepopAssessment).block();
      } else {
        log.info("pre-pop assessment or assessment data is corrupted!");
        throw new CaabApplicationException("pre-pop assessment or assessment data is corrupted!");
      }

    } else {
      //todo - later implementation in future story
    }

  }

  /**
   * Generates a list of assessment opponent mapping contexts for each opponent in the application.
   *
   * @param application the application details containing opponents
   * @return a list of assessment opponent mapping contexts
   */
  protected List<AssessmentOpponentMappingContext> getAssessmentOpponentMappingContexts(
      final ApplicationDetail application) {
    final List<AssessmentOpponentMappingContext> opponentContext = new ArrayList<>();

    for (final OpponentDetail opponent : application.getOpponents()) {

      final CommonLookupValueDetail titleCommonLookup = lookupService.getCommonValue(
              COMMON_VALUE_CONTACT_TITLE, opponent.getTitle())
          .map(commonLookupValueDetail -> commonLookupValueDetail
              .orElse(new CommonLookupValueDetail()
                  .code(opponent.getTitle())
                  .description(opponent.getTitle())))
          .blockOptional()
          .orElseThrow();

      opponentContext.add(AssessmentOpponentMappingContext.builder()
          .opponent(opponent)
          .titleCommonLookupValue(titleCommonLookup)
          .build());
    }
    return opponentContext;
  }

  /**
   * Finds the most recent assessment with the given provider ID, reference ID, and assessment name,
   * or creates a new assessment if none exists.
   *
   * @param providerId the ID of the provider
   * @param referenceId the reference ID
   * @param assessmentName the name of the assessment
   * @return the found or newly created assessment detail
   */
  protected AssessmentDetail findOrCreate(
      final String providerId,
      final String referenceId,
      final String assessmentName) {

    final AssessmentDetails assessments = getAssessments(
        List.of(assessmentName),
        providerId,
        referenceId).block();

    if (assessments != null && !assessments.getContent().isEmpty()) {
      return getMostRecentAssessmentDetail(assessments.getContent());
    } else {
      final AssessmentDetail assessment = new AssessmentDetail()
          .caseReferenceNumber(referenceId)
          .providerId(providerId)
          .name(assessmentName)
          .status(INCOMPLETE.getStatus());
      log.debug("Created Assessment {} ", assessment.toString());
      return assessment;
    }
  }

  /**
   * Determines if the assessment checkpoint should be deleted based on the application detail and
   * assessment detail.
   *
   * @param application the application detail to compare
   * @param assessment the assessment detail to compare
   * @return {@code true} if the checkpoint should be deleted, {@code false} otherwise
   */
  public boolean isAssessmentCheckpointToBeDeleted(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {
    boolean isMatching = false;
    final Date dateOfLastChange = getDateOfLatestKeyChange(application);

    if (dateOfLastChange != null) {
      isMatching = dateOfLastChange.after(assessment.getAuditDetail().getLastSaved());
    }

    return isMatching || isProceedingsCountMismatch(application, assessment);
  }

  /**
   * Checks if any proceeding entity in the assessment does not exist in the application's
   * proceedings.
   *
   * @param application the application details containing proceedings
   * @param assessment the assessment details containing proceeding entities
   * @return true if a proceeding entity in the assessment does not exist in the application, or
   *         if there is a mismatch with application proceedings, otherwise false
   */
  protected boolean isProceedingsCountMismatch(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    final List<AssessmentEntityDetail> proceedingEntities =
        getAssessmentEntitiesForEntityType(assessment, PROCEEDING);

    return application.getProceedings().size() != proceedingEntities.size()
        || isAssessmentProceedingsMatchingApplication(application, assessment);
  }

  /**
   * Checks if any proceeding entity in the assessment does not exist in the application's
   * proceedings.
   *
   * @param application the application details containing proceedings
   * @param assessment the assessment details containing proceeding entities
   * @return true if a proceeding entity in the assessment does not exist in the application, or
   *         if there is a mismatch with application proceedings, otherwise false
   */
  protected boolean isAssessmentProceedingsMatchingApplication(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    boolean isMatching = false;

    final List<AssessmentEntityDetail> proceedingEntities =
        getAssessmentEntitiesForEntityType(assessment, PROCEEDING);

    for (final AssessmentEntityDetail proceedingEntity : proceedingEntities) {
      final String proceedingId = proceedingEntity
          .getName()
          .replaceFirst(InstanceMappingPrefix.PROCEEDING.getPrefix(), "");

      // Check if proceedingEntity with the given name or proceedingId exists in the application
      final boolean proceedingExists =
          getProceedingByEbsId(application, proceedingEntity.getName()) != null
              || getProceedingById(application, Integer.parseInt(proceedingId)) != null;

      if (!proceedingExists) {
        // If no proceeding exists, set isMatching to true
        isMatching = true;
      }

    }
    return isMatching || isApplicationProceedingsMatchingAssessment(application, assessment);
  }

  /**
   * Checks if the proceedings in the application exist in the assessment's session and if
   * their scopes match.
   *
   * @param application the application details containing proceedings
   * @param assessment the assessment details to check against
   * @return true if a proceeding in the application does not exist in the assessment or if the
   *         scope of any proceeding has changed, otherwise false
   */
  protected boolean isApplicationProceedingsMatchingAssessment(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    boolean isMatching = false;
    for (final ProceedingDetail proceeding : application.getProceedings()) {
      log.debug(
          "App proceedings ID - " + proceeding.getId() + ", EBS-ID - " + proceeding.getEbsId());

      final String proceedingId = getAssessmentMappingId(proceeding);
      log.debug("Proceeding ID is " + proceedingId);

      final AssessmentEntityTypeDetail proceedingEntityType =
          getAssessmentEntityType(assessment, PROCEEDING);

      final AssessmentEntityDetail proceedingEntity =
          getAssessmentEntity(proceedingEntityType, proceedingId);

      if (proceedingEntity != null) {
        final String attributeValue =
            getAssessmentAttribute(proceedingEntity, REQUESTED_SCOPE).getValue();
        log.debug("Assessment Scope Request value : " + attributeValue);

        final String scopeInAssessment = getRequestedScopeForAssessmentInput(proceeding);
        if (scopeInAssessment != null && !scopeInAssessment.equalsIgnoreCase(attributeValue)) {
          log.debug("Looks like scope is change, hence return true");
          isMatching = true;
        }
      } else {
        log.debug("APP PROCEEDINGS DOESN'T EXIST IN OPASESSION OBJECT");
        isMatching = true;
      }
    }
    return isMatching || isOpponentCountMatchingAssessments(application, assessment);
  }

  /**
   * Checks if the number of opponents in the application matches the number of opponent entities
   * in the assessment and verifies the opponent details.
   *
   * @param application the application details containing opponents
   * @param assessment the assessment details containing opponent entities
   * @return true if the opponent counts do not match or if an opponent entity in the assessment
   *         does not exist in the application, otherwise false
   */
  protected boolean isOpponentCountMatchingAssessments(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    final List<AssessmentEntityDetail> opponentEntities =
        getAssessmentEntitiesForEntityType(assessment, OPPONENT);

    return application.getOpponents().size() != opponentEntities.size()
        || isAssessmentOpponentsMatchingApplication(application, assessment);
  }

  /**
   * Checks if any opponent entity in the assessment does not exist in the application's opponents.
   *
   * @param application the application details containing opponents
   * @param assessment the assessment details containing opponent entities
   * @return true if an opponent entity in the assessment does not exist in the application,
   *         otherwise false
   */
  protected boolean isAssessmentOpponentsMatchingApplication(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    boolean isMatching = false;

    final List<AssessmentEntityDetail> opponentEntities =
        getAssessmentEntitiesForEntityType(assessment, OPPONENT);

    for (final AssessmentEntityDetail opponentEntity : opponentEntities) {
      log.debug("Assessment Opponent EntityID : " + opponentEntity.getName());

      final String opponentId = opponentEntity
          .getName()
          .replaceFirst(InstanceMappingPrefix.OPPONENT.getPrefix(), "");

      final boolean opponentExists =
          getOpponentByEbsId(application, opponentEntity.getName()) != null
              || getOpponentById(application, Integer.parseInt(opponentId)) != null;

      if (!opponentExists) {
        log.debug("OPA SESSION OPPONENT DOESN'T EXIST IN APPLICATION OBJECT");
        isMatching = true;
      }
    }
    return isMatching || isApplicationOpponentsMatchingAssessments(application, assessment);
  }

  /**
   * Checks if any opponent in the application does not exist in the assessment's OPA session.
   *
   * @param application the application details containing opponents
   * @param assessment the assessment details to check against
   * @return true if an opponent in the application does not exist in the assessment, otherwise
   *         false
   */
  protected boolean isApplicationOpponentsMatchingAssessments(
      final ApplicationDetail application,
      final AssessmentDetail assessment) {

    boolean isMatching = false;

    for (final OpponentDetail opponent : application.getOpponents()) {
      log.debug("App opponent ID - " + opponent.getId() + ", EBS-ID - " + opponent.getEbsId());

      final String opponentId = OpponentUtil.getAssessmentMappingId(opponent);

      log.debug("Opponent ID is " + opponentId);

      final AssessmentEntityTypeDetail opponentEntityType =
          getAssessmentEntityType(assessment, OPPONENT);

      final AssessmentEntityDetail opponentEntity =
          getAssessmentEntity(opponentEntityType, opponentId);

      if (opponentEntity == null) {
        log.debug("APP OPPONENTS DOESN'T EXIST IN OPASESSION OBJECT");
        isMatching = true;
      }

    }
    return isMatching || applicationTypeMatches(application, assessment);
  }


  /**
   * Updates the cost limit if the assessment rulebase is for merits assessment.
   *
   * @param assessmentRulebase the rulebase for the assessment
   * @param application the application detail to update
   */
  protected void updateCostLimitIfMeritsAssessment(
      final AssessmentRulebase assessmentRulebase,
      final ApplicationDetail application,
      final UserDetail user) {
    if (assessmentRulebase != null && assessmentRulebase.equals(AssessmentRulebase.MERITS)) {

      application.getCostLimit().setLimitAtTimeOfMerits(
          application.getCosts().getRequestedCostLimitation());

      final CostLimitDetail costLimit = application.getCostLimit();
      costLimit.setLimitAtTimeOfMerits(
          application.getCosts().getRequestedCostLimitation());

      final ApplicationDetail patch = new ApplicationDetail().costLimit(costLimit);
      caabApiClient.patchApplication(
          String.valueOf(application.getId()),
          patch,
          user.getLoginId()).block();

    }
  }

  /**
   * Retrieves the assessment summary to display based on the given assessment details.
   *
   * @param assessment the assessment containing details to be summarized
   * @return a list of assessment summary entity displays
   */
  public List<AssessmentSummaryEntityDisplay> getAssessmentSummaryToDisplay(
      final AssessmentDetail assessment,
      final List<AssessmentSummaryEntityLookupValueDetail> parentSummaryLookups,
      final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups) {

    final List<AssessmentSummaryEntityDisplay> summaryToDisplay = new ArrayList<>();

    //loop through all parent summary lookups
    for (final AssessmentSummaryEntityLookupValueDetail parentSummaryLookup
        : parentSummaryLookups) {
      log.debug("Parent Summary: {}", parentSummaryLookup.getDisplayName());

      // get all entities for the parent summary lookup
      final List<AssessmentEntityDetail> entities =
          getAssessmentEntitiesForEntityType(assessment, parentSummaryLookup.getName());

      //loop through all entities in the assessment where the entity type matches
      // the parent summary lookup
      for (final AssessmentEntityDetail entity : entities) {
        log.debug("Entity: {}", entity.getName());
        createSummaryEntity(
            assessment,
            summaryToDisplay,
            childSummaryLookups,
            parentSummaryLookup,
            entity);
      }
    }

    return summaryToDisplay;
  }

  /**
   * Creates a summary display entity from the given entity and adds it to the list if applicable.
   *
   * @param assessment the current assessment detail
   * @param summaryEntitiesToDisplay the list to add created summary display entities to
   * @param childSummaryLookups list of lookup values for potential child summary entities
   * @param summaryEntityLookup the lookup data for the current summary entity
   * @param entity the current assessment entity to process
   */
  protected void createSummaryEntity(
      final AssessmentDetail assessment,
      final List<AssessmentSummaryEntityDisplay> summaryEntitiesToDisplay,
      final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups,
      final AssessmentSummaryEntityLookupValueDetail summaryEntityLookup,
      final AssessmentEntityDetail entity) {

    //we have the entity from the assessment where it matched the parent summary lookup

    //need to stream through each attribute in the summaryEntityLookup
    //check it matches the assessment entity attributes, then add it to a list if matches
    final List<AssessmentSummaryAttributeDisplay> attributesToDisplay =
        summaryEntityLookup.getAttributes().stream()
            .map(attributeLookup -> getAssessmentAttribute(entity, attributeLookup.getName()))
            .filter(Objects::nonNull)
            .map(attribute -> createSummaryAttributeDisplay(attribute, summaryEntityLookup))
            .filter(Objects::nonNull)
            .toList();

    if (!attributesToDisplay.isEmpty()) {
      //if we have data in the list then we can create a summary display entity
      final AssessmentSummaryEntityDisplay summaryEntityToDisplay =
          new AssessmentSummaryEntityDisplay(
              summaryEntityLookup.getName(),
              summaryEntityLookup.getDisplayName(),
              summaryEntityLookup.getEntityLevel());

      //then we add all the attributes to the summary entity
      summaryEntityToDisplay.getAttributes().addAll(attributesToDisplay);
      //then add the summary entity to the list
      summaryEntitiesToDisplay.add(summaryEntityToDisplay);

      //Child entities to add
      if (!GLOBAL.getType().equalsIgnoreCase(summaryEntityToDisplay.getName())) {
        for (final AssessmentRelationshipDetail relationship : entity.getRelations()) {
          final String relationType = relationship.getName();
          if (StringUtils.hasLength(relationType)) {

            // ignore reverse relationships
            if (!relationType.startsWith("rev") && !relationType.endsWith("_rev")) {
              log.debug("Relation Type: " + relationType);
              for (final AssessmentEntityDetail childEntity :
                  getRelatedEntities(relationship, assessment)) {
                for (final AssessmentSummaryEntityLookupValueDetail childSummaryEntityLookup :
                    childSummaryLookups) {
                  createSummaryEntity(
                      assessment,
                      summaryEntitiesToDisplay,
                      childSummaryLookups,
                      childSummaryEntityLookup,
                      childEntity);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Creates a summary attribute display.
   *
   * @param attribute the assessment attribute detail
   * @param summaryEntityLookup the summary entity lookup value detail
   * @return the summary attribute display or null if the condition is not met
   */
  protected AssessmentSummaryAttributeDisplay createSummaryAttributeDisplay(
      final AssessmentAttributeDetail attribute,
      final AssessmentSummaryEntityLookupValueDetail summaryEntityLookup) {

    // Format the attribute value
    final String formattedAttributeValue = getFormattedAttributeValue(attribute);

    // Use Optional to handle the conditionally creating the display object
    return Optional.of(attribute)
        // Check if the formatted attribute value has text or if the attribute was asked
        .filter(attr -> StringUtils.hasText(formattedAttributeValue) || attr.getAsked())
        // If the condition is met, create a new AssessmentSummaryAttributeDisplay
        .map(attr -> new AssessmentSummaryAttributeDisplay(
            attr.getName(),
            getDisplayNameForAttribute(summaryEntityLookup, attr.getName()),
            formattedAttributeValue))
        // If the condition is not met, return null
        .orElse(null);
  }


}
