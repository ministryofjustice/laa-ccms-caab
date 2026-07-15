package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
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
import static uk.gov.laa.ccms.caab.util.ProceedingUtil.getRequestedScopeForAssessmentInput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import uk.gov.laa.ccms.caab.assessment.model.AuditDetail;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;
import uk.gov.laa.ccms.caab.client.AssessmentApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentName;
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
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.OpaAttribute;
import uk.gov.laa.ccms.caab.model.OpaEntity;
import uk.gov.laa.ccms.caab.model.OpaInstance;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryAttributeDisplay;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.caab.util.AssessmentReuseUtil;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.caab.util.ProceedingUtil;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Service class for handling operations related to assessments. This class provides methods to
 * retrieve, manipulate, and evaluate assessment data using the assessment API client and a lookup
 * service. It facilitates operations such as fetching assessments based on specific criteria,
 * determining the status of assessments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

  private static final DateTimeFormatter ASSESSMENT_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MM-yyyy");

  private final AssessmentApiClient assessmentApiClient;
  private final CaabApiClient caabApiClient;
  private final SoaApiClient soaApiClient;
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
    return assessmentApiClient.getAssessments(assessmentNames, providerId, caseReferenceNumber);
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
   * Saves an assessment by creating a new one or updating an existing one based on the assessment
   * ID.
   *
   * @param user the user details
   * @param assessment the assessment details
   * @return a Mono indicating when the save operation has completed
   */
  public Mono<Void> saveAssessment(final UserDetail user, final AssessmentDetail assessment) {
    if (assessment.getId() == null) {
      return assessmentApiClient.createAssessment(assessment, user.getLoginId());
    } else {
      return assessmentApiClient.updateAssessment(
          assessment.getId(), assessment, user.getLoginId());
    }
  }

  /**
   * Calculates and updates the statuses for both means and merits assessments of an application. It
   * determines which assessment was last saved to set the isMeansLast flag and applies status
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

    // Progress-status descriptions are resolved via a (blocking) lookup; memoise per invocation so
    // the same status code is not looked up again by both the amended-flag comparison and the
    // status-setting path.
    final Map<String, String> statusDisplayCache = new HashMap<>();

    // The stored *AssessmentStatus holds the progress-status display description (e.g. "In
    // progress"), so the assessment's raw status (e.g. "INCOMPLETE") is resolved to its description
    // before comparing - otherwise the values would never match and the assessment would be flagged
    // as amended on every load. Recomputed each load, so it does not need persisting.
    if (meansAssessment != null) {
      if (application.getMeansAssessmentStatus() == null) {
        if (!NOT_STARTED.getStatus().equals(meansAssessment.getStatus())) {
          application.setMeansAssessmentAmended(true);
        }
      } else if (!application
          .getMeansAssessmentStatus()
          .equals(getProgressStatusDisplay(meansAssessment.getStatus(), statusDisplayCache))) {
        application.setMeansAssessmentAmended(true);
      }
    }

    // Symmetric merits tracking (mirrors the means block above and old PUI's
    // StartOpaAssessment.setAssessmentAmended). Without this meritsAssessmentAmended is never set,
    // so an amended merits assessment is routed through the "unchanged" branch and never reaches
    // COMPLETE.
    if (meritsAssessment != null) {
      if (application.getMeritsAssessmentStatus() == null) {
        if (!NOT_STARTED.getStatus().equals(meritsAssessment.getStatus())) {
          application.setMeritsAssessmentAmended(true);
        }
      } else if (!application
          .getMeritsAssessmentStatus()
          .equals(getProgressStatusDisplay(meritsAssessment.getStatus(), statusDisplayCache))) {
        application.setMeritsAssessmentAmended(true);
      }
    }

    log.info("Calculating means assessment status");
    calculateAssessmentStatus(
        MEANS, meansAssessment, meritsAssessment, application, user, statusDisplayCache);

    log.info("Calculating merits assessment status");
    calculateAssessmentStatus(
        MERITS, meritsAssessment, meansAssessment, application, user, statusDisplayCache);

    // If a proceeding was removed, we should set reassessment required for both.
    if (Boolean.TRUE.equals(application.getMeritsReassessmentRequired())) {
      setReassessmentRequired(application, meansAssessment, user, statusDisplayCache);
      setReassessmentRequired(application, meritsAssessment, user, statusDisplayCache);
    }
  }

  /**
   * Sets the status of the provided assessment to REQUIRED and updates it in the database and on
   * the application.
   *
   * @param application the application related to the assessment
   * @param assessment the assessment to update
   * @param user the user performing the update operation
   */
  private void setReassessmentRequired(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final UserDetail user,
      final Map<String, String> statusDisplayCache) {
    if (assessment != null) {
      assessmentApiClient
          .patchAssessment(
              assessment.getId(),
              user.getLoginId(),
              new PatchAssessmentDetail().status(REQUIRED.getStatus()))
          .block();
      setAssessmentStatusOnApplication(application, assessment, REQUIRED, statusDisplayCache);
    }
  }

  /**
   * Calculates and updates the status of the provided assessment based on specific criteria,
   * including the amendment state of the application and whether a reassessment is required. The
   * status is updated through an API client and also set on the application object.
   *
   * @param currentAssessment the assessment whose status needs to be evaluated
   * @param application the application related to the assessment
   * @param user the user performing the update operation
   */
  private void calculateAssessmentStatus(
      final AssessmentName assessmentName,
      final AssessmentDetail currentAssessment,
      final AssessmentDetail otherAssessment,
      final ApplicationDetail application,
      final UserDetail user,
      final Map<String, String> statusDisplayCache) {

    boolean statusChanged = false;
    AssessmentStatus assessmentStatus = getStatus(currentAssessment);

    if (!application.getAmendment()
        || (isApplicationsAssessmentAmended(application, assessmentName))) {

      if (COMPLETE == assessmentStatus || ERROR == assessmentStatus) {

        if (isReassessmentRequired(
            application, assessmentName, currentAssessment, otherAssessment, user)) {
          assessmentStatus = REQUIRED;
          statusChanged = true;
        }
      }
    } else {
      if (application.getAmendment()) {
        if (isReassessmentRequired(
            application, assessmentName, currentAssessment, otherAssessment, user)) {
          assessmentStatus = REQUIRED;
        } else {
          assessmentStatus = UNCHANGED;
        }
        statusChanged = true;
      }
    }

    // update the assessment status if it has changed
    if (statusChanged && currentAssessment != null) {
      assessmentApiClient
          .patchAssessment(
              currentAssessment.getId(),
              user.getLoginId(),
              new PatchAssessmentDetail().status(assessmentStatus.getStatus()))
          .block();
    }

    if (currentAssessment != null
        || (Boolean.TRUE.equals(application.getAmendment())
            && StringUtils.hasText(application.getCaseReferenceNumber()))) {
      setAssessmentStatusOnApplication(
          application, assessmentName, currentAssessment, assessmentStatus, statusDisplayCache);
    }
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
      final AssessmentStatus assessmentStatus,
      final Map<String, String> statusDisplayCache) {
    if (assessment != null) {
      final AssessmentName assessmentName =
          MEANS.getName().equalsIgnoreCase(assessment.getName()) ? MEANS : MERITS;
      setAssessmentStatusOnApplication(
          application, assessmentName, assessment, assessmentStatus, statusDisplayCache);
    }
  }

  /**
   * Sets the status of an assessment on the application object using the assessment name when an
   * assessment does not yet exist.
   *
   * @param application the application to update with the new assessment status
   * @param assessmentName the assessment name being updated
   * @param assessment the assessment whose status is being updated, if it exists
   * @param assessmentStatus the AssessmentStatus to set on the application
   */
  private void setAssessmentStatusOnApplication(
      final ApplicationDetail application,
      final AssessmentName assessmentName,
      final AssessmentDetail assessment,
      final AssessmentStatus assessmentStatus,
      final Map<String, String> statusDisplayCache) {

    if (assessmentName != null && assessmentStatus != null) {
      final String statusValue =
          getProgressStatusDisplay(assessmentStatus.getStatus(), statusDisplayCache);

      if (MEANS == assessmentName) {
        application.setMeansAssessmentStatus(statusValue);
      } else if (MERITS == assessmentName) {
        application.setMeritsAssessmentStatus(statusValue);
      }
    }
  }

  /**
   * Resolves an assessment status code to its display description from the OPA progress-status
   * lookup (e.g. {@code "INCOMPLETE"} to {@code "In progress"}), falling back to the raw status
   * code when no mapping exists.
   *
   * @param statusCode the raw assessment status code
   * @param statusDisplayCache per-invocation cache of already-resolved status codes, so the same
   *     code is not looked up again by both the amended-flag comparison and the status-setting path
   * @return the display description, or the raw status code if not found
   */
  private String getProgressStatusDisplay(
      final String statusCode, final Map<String, String> statusDisplayCache) {
    if (statusCode == null) {
      return null;
    }
    return statusDisplayCache.computeIfAbsent(statusCode, this::lookupProgressStatusDisplay);
  }

  private String lookupProgressStatusDisplay(final String statusCode) {
    return lookupService
        .getCommonValue(COMMON_VALUE_PROGRESS_STATUS_TYPES, statusCode)
        .flatMap(
            commonLookupValueDetailOptional ->
                commonLookupValueDetailOptional
                    .map(
                        commonLookupValueDetail ->
                            Mono.just(commonLookupValueDetail.getDescription()))
                    .orElseGet(Mono::empty))
        .blockOptional()
        .orElse(statusCode);
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
      final ApplicationDetail application, final AssessmentDetail assessment) {

    if (assessment != null) {
      if (MEANS.getName().equalsIgnoreCase(assessment.getName())) {
        return Boolean.TRUE.equals(application.getMeansAssessmentAmended());
      } else if (MERITS.getName().equalsIgnoreCase(assessment.getName())) {
        return Boolean.TRUE.equals(application.getMeritsAssessmentAmended());
      }
    }
    return false;
  }

  /**
   * Determines if the named assessment of an application has been amended.
   *
   * @param application the application to check for amendments
   * @param assessmentName the assessment name
   * @return true if the specified assessment type has been amended; false otherwise
   */
  private boolean isApplicationsAssessmentAmended(
      final ApplicationDetail application, final AssessmentName assessmentName) {
    if (MEANS == assessmentName) {
      return Boolean.TRUE.equals(application.getMeansAssessmentAmended());
    } else if (MERITS == assessmentName) {
      return Boolean.TRUE.equals(application.getMeritsAssessmentAmended());
    }
    return false;
  }

  /**
   * Determines if a reassessment is required based on changes between an application and its latest
   * assessment.
   *
   * @param application the application details to compare against the assessment
   * @param assessment the assessment details to compare against the application
   * @param user the user performing the update operation
   * @return true if a reassessment is necessary; false otherwise
   */
  protected boolean isReassessmentRequired(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final UserDetail user) {
    final AssessmentName assessmentName =
        assessment != null && MEANS.getName().equalsIgnoreCase(assessment.getName())
            ? MEANS
            : MERITS;
    return isReassessmentRequired(application, assessmentName, assessment, null, user);
  }

  /**
   * Determines if a reassessment is required based on changes between an application and its latest
   * assessment.
   *
   * @param application the application details to compare against the assessment
   * @param assessmentName the assessment name to evaluate
   * @param assessment the assessment details to compare against the application, if one exists
   * @param otherAssessment the other of the means/merits pair, if one exists
   * @param user the user performing the update operation
   * @return true if a reassessment is necessary; false otherwise
   */
  protected boolean isReassessmentRequired(
      final ApplicationDetail application,
      final AssessmentName assessmentName,
      final AssessmentDetail assessment,
      final AssessmentDetail otherAssessment,
      final UserDetail user) {

    if (!application.getAmendment()) {
      if (assessment != null) {

        final AssessmentEntityTypeDetail proceedingEntityType =
            getAssessmentEntityType(assessment, PROCEEDING);

        // check for any proceeding attribute changes between assessment and application
        if (checkAssessmentForProceedingKeyChange(application, proceedingEntityType)) {
          return true;
        }

        // check if proceeding is deleted
        if (application.getProceedings() != null
            && proceedingEntityType != null
            && proceedingEntityType.getEntities() != null) {
          log.debug(
              "app.getProceedings().size(): {} - opaListEntity.getOpaEntities().size(): {}",
              application.getProceedings().size(),
              proceedingEntityType.getEntities().size());

          if (application.getProceedings().size() < proceedingEntityType.getEntities().size()) {
            log.debug("When proceeding is deleted condition - returns TRUE");
            return true;
          }
        }

        // used to check if the proceeding is deleted - this should be included for both
        // assessments,
        // although not specifically needed for means, both assessments and application data should
        // be kept in sync.
        if (Boolean.TRUE.equals(application.getMeritsReassessmentRequired())) {
          log.info(
              "Reassessment Required for {} as application.getMeritsReassessmentRequired() IS TRUE",
              application.getCaseReferenceNumber());
          return true;
        }

        // used specifically for merits - this should be included for both assessments,
        // although not specifically needed for means, both assessments and application data should
        // be kept in sync.
        for (final OpponentDetail opponent : application.getOpponents()) {
          if (OPPONENT_TYPE_INDIVIDUAL.equalsIgnoreCase(opponent.getType())
              && differenceGreaterThanTenSecs(
                  opponent.getAuditTrail().getLastSaved(),
                  assessment.getAuditDetail().getCreated())) {

            log.info(
                "Reassessment Required for {} as When individual is updated - IS TRUE",
                application.getCaseReferenceNumber());
            return true;
          }
        }

        // When Individual/Organisation deleted
        // used specifically for merits - this should be included for both assessments,
        // although not specifically needed for means, both assessments and application data should
        // be kept in sync.
        final AssessmentEntityTypeDetail opponentEntityType =
            getAssessmentEntityType(assessment, OPPONENT);
        if ((application.getOpponents() != null) && (opponentEntityType != null)) {
          if (application.getOpponents().size() < opponentEntityType.getEntities().size()) {
            log.info(
                "Reassessment Required for {} as When organisation/individual is deleted "
                    + "condition - IS TRUE",
                application.getCaseReferenceNumber());
            return true;
          }
        }

        // only check when it's a merits assessment - any change to the cost limit since the merits
        // assessment was run requires a reassessment (the provider must justify the new costs), not
        // only an increase above the previous figure.
        if (MERITS == assessmentName && isCostLimitReassessmentRequired(application)) {
          log.info(
              "Merit Reassessment Required for {} as the requested (or default) cost limit differs "
                  + "from the limit at the time of the merits assessment (or either is unknown)",
              application.getCaseReferenceNumber());
          return true;
        }
      }
    } else {
      // Amend case logic
      if (MEANS == assessmentName) {
        return isMeansReassessmentRequiredForAmendment(application, assessment, user);
      } else if (MERITS == assessmentName) {
        return isMeritsReassessmentRequiredForAmendment(
            application, otherAssessment, assessment, user);
      }
    }

    return false;
  }

  /**
   * Determines whether a means reassessment is required during amendment processing.
   *
   * @param application the application details
   * @param assessment the latest means assessment, if one exists
   * @param user the user performing the operation
   * @return true if a means reassessment is required
   */
  public boolean isMeansReassessmentRequiredForAmendment(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final UserDetail user) {
    final uk.gov.laa.ccms.soa.gateway.model.CaseDetail ebsCase = getEbsCase(application, user);

    // Old PUI's only means rule on an amendment (AssessmentHelper.isMeansReassessmentRequired,
    // LSC-1783): a substantive amendment of an emergency certificate, and only while no means
    // assessment exists. Once one exists it must not re-trigger.
    return isSubstantiveAmendmentOfEmergencyCertificate(application, assessment, ebsCase);
  }

  private boolean isSubstantiveAmendmentOfEmergencyCertificate(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final uk.gov.laa.ccms.soa.gateway.model.CaseDetail ebsCase) {
    return assessment == null
        && !hasEbsAmendments(ebsCase)
        && APP_TYPE_SUBSTANTIVE.equals(getApplicationTypeId(application))
        && ebsCase != null
        && APP_TYPE_EMERGENCY.equals(ebsCase.getCertificateType());
  }

  /**
   * Determines whether a merits reassessment is required during amendment processing.
   *
   * @param application the application details
   * @param assessment the latest merits assessment, if one exists
   * @param user the user performing the operation
   * @return true if a merits reassessment is required
   */
  public boolean isMeritsReassessmentRequiredForAmendment(
      final ApplicationDetail application,
      final AssessmentDetail assessment,
      final UserDetail user) {
    return isMeritsReassessmentRequiredForAmendment(application, null, assessment, user);
  }

  /**
   * Determines whether a merits reassessment is required during amendment processing.
   *
   * @param application the application details
   * @param meansAssessment the latest means assessment, if one exists. Used to establish whether
   *     the means assessment was the last one run: a change to the means must not, on its own,
   *     demand a merits reassessment (old PUI guards its date check with {@code !meansLast}).
   * @param assessment the latest merits assessment, if one exists
   * @param user the user performing the operation
   * @return true if a merits reassessment is required
   */
  public boolean isMeritsReassessmentRequiredForAmendment(
      final ApplicationDetail application,
      final AssessmentDetail meansAssessment,
      final AssessmentDetail assessment,
      final UserDetail user) {
    final uk.gov.laa.ccms.soa.gateway.model.CaseDetail ebsCase = getEbsCase(application, user);

    // Old PUI only forces reassessment here when there is NO merits assessment at all (a
    // substantive amendment of an emergency certificate); once one exists it must not re-trigger.
    if (isSubstantiveAmendmentOfEmergencyCertificate(application, assessment, ebsCase)) {
      return true;
    }

    final Date latestKeyChange = getDateOfLatestKeyChange(application);
    if (latestKeyChange == null) {
      return false;
    }

    final Date meritsCreated = getMeritsComparisonDate(application, assessment);
    if (meritsCreated == null) {
      return false;
    }

    // Skipped when the means assessment was the last one run: the key change that the merits
    // assessment predates is then the means assessment's own, and a means change must not flip
    // merits to reassessment-required (old PUI AssessmentHelper.isMeritsReassessmentRequired).
    if (!isMeansLast(meansAssessment, assessment)
        && differenceGreaterThanTenSecs(latestKeyChange, meritsCreated)) {
      return true;
    }

    if (Boolean.TRUE.equals(application.getMeritsReassessmentRequired())) {
      return true;
    }

    // Cost limit at the time of merits is below the current requested/default limit. This mirrors
    // old PUI (AssessmentHelper.isMeritsReassessmentRequired), where the cost-limit check sits
    // below
    // the dateOfLatestKeyChange == null and merits-assessment == null early returns above - it is
    // deliberately NOT evaluated when those timestamps are unavailable.
    if (isCostLimitReassessmentRequired(application)) {
      return true;
    }

    // The remaining checks compare the application against an existing assessment's recorded data.
    return assessment != null && isMeritsReassessmentRequiredForAssessment(application, assessment);
  }

  /**
   * Determines whether the means assessment was the last of the two to be saved.
   *
   * @param meansAssessment the latest means assessment, may be {@code null}
   * @param meritsAssessment the latest merits assessment, may be {@code null}
   * @return true if the means assessment was saved no earlier than the merits assessment
   */
  private boolean isMeansLast(
      final AssessmentDetail meansAssessment, final AssessmentDetail meritsAssessment) {
    final Date meansSaved = getLastSaved(meansAssessment);
    final Date meritsSaved = getLastSaved(meritsAssessment);

    if (meansSaved == null || meritsSaved == null) {
      return false;
    }

    return !meansSaved.before(meritsSaved);
  }

  private Date getLastSaved(final AssessmentDetail assessment) {
    return Optional.ofNullable(assessment)
        .map(AssessmentDetail::getAuditDetail)
        .map(AuditDetail::getLastSaved)
        .orElse(null);
  }

  private uk.gov.laa.ccms.soa.gateway.model.CaseDetail getEbsCase(
      final ApplicationDetail application, final UserDetail user) {
    final Mono<uk.gov.laa.ccms.soa.gateway.model.CaseDetail> ebsCaseMono =
        soaApiClient.getCase(
            application.getCaseReferenceNumber(), user.getLoginId(), user.getUserType());
    return ebsCaseMono == null ? null : ebsCaseMono.block();
  }

  private boolean hasEbsAmendments(final uk.gov.laa.ccms.soa.gateway.model.CaseDetail ebsCase) {
    return ebsCase != null
        && ebsCase.getApplicationDetails() != null
        && ebsCase.getApplicationDetails().getProceedings() != null
        && ebsCase.getApplicationDetails().getProceedings().stream()
            .anyMatch(p -> "DRAFT".equalsIgnoreCase(p.getStatus()));
  }

  private boolean isMeansReassessmentApplicationType(final ApplicationDetail application) {
    return APP_TYPE_EXCEPTIONAL_CASE_FUNDING.equals(getApplicationTypeId(application));
  }

  private String getApplicationTypeId(final ApplicationDetail application) {
    return Optional.ofNullable(application)
        .map(ApplicationDetail::getApplicationType)
        .map(ApplicationType::getId)
        .orElse(null);
  }

  private Date getMeritsComparisonDate(
      final ApplicationDetail application, final AssessmentDetail assessment) {
    if (Boolean.TRUE.equals(application.getAmendment())
        && !Boolean.TRUE.equals(application.getMeritsAssessmentAmended())) {
      return Optional.ofNullable(application.getAuditTrail())
          .map(uk.gov.laa.ccms.caab.model.AuditDetail::getCreated)
          .map(created -> Date.from(created.toInstant().plusSeconds(2)))
          .orElse(null);
    }

    return Optional.ofNullable(assessment)
        .map(AssessmentDetail::getAuditDetail)
        .map(AuditDetail::getCreated)
        .orElse(null);
  }

  private boolean isMeritsReassessmentRequiredForAssessment(
      final ApplicationDetail application, final AssessmentDetail assessment) {

    final AssessmentEntityTypeDetail proceedingEntityType =
        getAssessmentEntityType(assessment, PROCEEDING);
    if (checkAssessmentForProceedingKeyChange(application, proceedingEntityType)) {
      return true;
    }

    // An individual opponent was updated after the merits assessment was created (old PUI).
    if (application.getOpponents() != null) {
      for (final OpponentDetail opponent : application.getOpponents()) {
        if (OPPONENT_TYPE_INDIVIDUAL.equalsIgnoreCase(opponent.getType())
            && opponent.getAuditTrail() != null
            && differenceGreaterThanTenSecs(
                opponent.getAuditTrail().getLastSaved(),
                assessment.getAuditDetail().getCreated())) {
          return true;
        }
      }
    }

    // An individual or organisation opponent was deleted since the assessment was created.
    final AssessmentEntityTypeDetail opponentEntityType =
        getAssessmentEntityType(assessment, OPPONENT);
    return (application.getOpponents() != null)
        && (opponentEntityType != null)
        && application.getOpponents().size() < opponentEntityType.getEntities().size();
  }

  /**
   * Determines whether the application's requested (or default) cost limit has RISEN above the cost
   * limit captured at the time of the merits assessment, which requires a merits reassessment - the
   * provider must justify the higher costs. A reduced limit does not, matching old PUI ({@code
   * AssessmentHelper.isMeritsReassessmentRequired}, which tests {@code compareTo(...) < 0}). An
   * unknown limit-at-time-of-merits or an unknown current limit is treated as
   * reassessment-required.
   *
   * @param application the application to check
   * @return true if a cost-limit driven merits reassessment is required
   */
  private boolean isCostLimitReassessmentRequired(final ApplicationDetail application) {
    final BigDecimal limitAtTimeOfMerits =
        Optional.ofNullable(application.getCostLimit())
            .map(CostLimitDetail::getLimitAtTimeOfMerits)
            .orElse(null);
    final BigDecimal requestedOrDefault = getRequestedOrDefaultCostLimitation(application);

    if (limitAtTimeOfMerits == null || requestedOrDefault == null) {
      return true;
    }

    return limitAtTimeOfMerits.compareTo(requestedOrDefault) < 0;
  }

  private BigDecimal getRequestedOrDefaultCostLimitation(final ApplicationDetail application) {
    final CostStructureDetail costs = application.getCosts();
    if (costs == null) {
      return null;
    }
    return costs.getRequestedCostLimitation() != null
        ? costs.getRequestedCostLimitation()
        : costs.getDefaultCostLimitation();
  }

  /**
   * Checks if there are any discrepancies between the key data of the applications type details and
   * their corresponding assessment records based on the specified assessment.
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

    final Optional<LocalDate> applicationDateUsed =
        Optional.of(application)
            .map(ApplicationDetail::getApplicationType)
            .map(ApplicationType::getDevolvedPowers)
            .map(DevolvedPowersDetail::getDateUsed)
            .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

    for (final AssessmentEntityDetail globalEntity : globalEntities) {

      final String applicationTypeFromAssessment =
          Optional.ofNullable(getAssessmentAttribute(globalEntity, APP_AMEND_TYPE))
              .map(AssessmentAttributeDetail::getValue)
              .orElse(null);

      final boolean applicationTypeMismatch =
          applicationTypeFromAssessment != null
              && (applicationType == null
                  || !applicationType.equalsIgnoreCase(applicationTypeFromAssessment));

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

      final Optional<LocalDate> attributeDate =
          delegatedFunctionsAttribute != null
              ? parseAssessmentDate(delegatedFunctionsAttribute.getValue())
              : Optional.empty();

      if (attributeDate.isPresent()) {
        if (applicationDateUsed.isEmpty()
            || !applicationDateUsed.get().isEqual(attributeDate.get())) {
          return true;
        }
      } else if (applicationDateUsed.isPresent()) {
        return true;
      }
    }

    return false;
  }

  private Optional<LocalDate> parseAssessmentDate(final String date) {
    if (!StringUtils.hasText(date)) {
      return Optional.empty();
    }

    try {
      return Optional.of(LocalDate.parse(date, ASSESSMENT_DATE_FORMATTER));
    } catch (DateTimeParseException exception) {
      log.debug("Ignoring unparsable assessment date value [{}]", date);
      return Optional.empty();
    }
  }

  /**
   * Checks if there are any discrepancies between the key data of the proceedings in the
   * application and their corresponding assessment records based on the specified assessment entity
   * type.
   *
   * @param application the application containing proceedings to check
   * @param proceedingEntityType the type of assessment entity to match against proceedings
   * @return true if any discrepancies are found; false otherwise
   */
  protected boolean checkAssessmentForProceedingKeyChange(
      final ApplicationDetail application, final AssessmentEntityTypeDetail proceedingEntityType) {

    if (proceedingEntityType == null
        && (application.getProceedings() == null || application.getProceedings().isEmpty())) {
      return true;
    }

    for (final ProceedingDetail proceeding : application.getProceedings()) {
      final String matterType = proceeding.getMatterType().getId();
      final String proceedingType = proceeding.getProceedingType().getId();
      final String clientInvolvementType = proceeding.getClientInvolvement().getId();
      final String entityId = ProceedingUtil.getAssessmentMappingId(proceeding);

      // find entity in entity type where matched entity id
      final AssessmentEntityDetail proceedingEntity =
          getAssessmentEntity(proceedingEntityType, entityId);

      if (proceedingEntity == null) {
        // The proceeding is not in the assessment, so it was added after the assessment was run and
        // the assessment must be redone (old PUI AssessmentHelper.isMeritsReassessmentRequired).
        return true;
      }

      final AssessmentAttributeDetail matterTypeAttribute =
          getAssessmentAttribute(proceedingEntity, AssessmentAttribute.MATTER_TYPE);
      final AssessmentAttributeDetail proceedingTypeAttribute =
          getAssessmentAttribute(proceedingEntity, AssessmentAttribute.PROCEEDING_NAME);
      final AssessmentAttributeDetail clientInvolvementTypeAttribute =
          getAssessmentAttribute(proceedingEntity, AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE);

      if (!matterType.equals(matterTypeAttribute.getValue())
          || !proceedingType.equals(proceedingTypeAttribute.getValue())
          || !clientInvolvementType.equals(clientInvolvementTypeAttribute.getValue())) {
        return true;
      }

      // Check scope limitations for both, although not specifically needed for means, both
      // assessments and application data should be kept in sync.
      final AssessmentAttributeDetail scopeLimitationAttribute =
          getAssessmentAttribute(proceedingEntity, AssessmentAttribute.REQUESTED_SCOPE);

      if (scopeLimitationAttribute != null) {
        final String assessmentScopeLimitation = scopeLimitationAttribute.getValue();
        final String applicationScopeLimitation = getRequestedScopeForAssessmentInput(proceeding);
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
   * Cleans up data by deleting redundant opponents and proceedings from the assessment, i.e. those
   * no longer present in the application. For amendments the retained proceedings include any draft
   * amendment proceedings from EBS, matching the set the assessment is built from.
   *
   * @param assessment the assessment detail to clean up
   * @param application the application detail for reference
   */
  public void cleanupData(final AssessmentDetail assessment, final ApplicationDetail application) {

    if (assessment != null) {
      deleteRedundantOpponents(assessment, application);
      deleteRedundantProceedings(assessment, application);
    }
  }

  /**
   * Deletes redundant opponents from the assessment that are not present in the application.
   *
   * @param assessment the assessment details
   * @param application the application details
   */
  private void deleteRedundantOpponents(
      final AssessmentDetail assessment, final ApplicationDetail application) {
    final List<String> opponentsToDelete =
        getEntitiesToDelete(
            assessment,
            OPPONENT,
            AssessmentRelationship.OPPONENT,
            (list, id) -> addRedundantOpponent(list, id, application));

    deleteEntityAndRelationship(
        assessment, OPPONENT, AssessmentRelationship.OPPONENT, opponentsToDelete);
  }

  /**
   * Deletes redundant proceedings from the assessment that are not present in the application.
   *
   * @param assessment the assessment details
   * @param application the application details
   */
  private void deleteRedundantProceedings(
      final AssessmentDetail assessment, final ApplicationDetail application) {
    final List<String> proceedingsToDelete =
        getEntitiesToDelete(
            assessment,
            PROCEEDING,
            AssessmentRelationship.PROCEEDING,
            (list, id) -> addRedundantProceeding(list, id, application));

    deleteEntityAndRelationship(
        assessment, PROCEEDING, AssessmentRelationship.PROCEEDING, proceedingsToDelete);
  }

  /**
   * Deletes entities and their relationships from the assessment based on the given entity type and
   * relationship type.
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
          .ifPresent(
              entity -> {
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
        .filter(
            globalEntity ->
                assessment.getCaseReferenceNumber().equalsIgnoreCase(globalEntity.getName()))
        .map(globalEntity -> getEntityRelationship(globalEntity, relationshipType))
        .filter(Objects::nonNull)
        .flatMap(relationship -> relationship.getRelationshipTargets().stream())
        .forEach(
            target -> {
              log.debug(
                  "%s relationship target entity ID : %s"
                      .formatted(relationshipType, target.getTargetEntityId()));
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

    addRedundantEntity(
        opponentsToDelete,
        entityName,
        application.getOpponents(),
        opponent ->
            (opponent.getEbsId() != null && entityName.equalsIgnoreCase(opponent.getEbsId()))
                || (opponent.getId() != null
                    && entityName.equalsIgnoreCase(
                        InstanceMappingPrefix.OPPONENT
                            .getPrefix()
                            .concat(opponent.getId().toString()))));
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

    // Use the same amendment-inclusive proceeding set the mapper builds the assessment from
    // (live proceedings plus any draft amendment proceedings from EBS). Otherwise draft
    // amendment proceedings - which are present in the assessment but not in
    // application.getProceedings() - would be wrongly treated as redundant and removed.
    addRedundantEntity(
        proceedingsToDelete,
        entityName,
        ProceedingUtil.getAssessmentProceedings(application),
        proceeding ->
            (proceeding.getEbsId() != null && entityName.equalsIgnoreCase(proceeding.getEbsId()))
                || (proceeding.getId() != null
                    && entityName.equalsIgnoreCase(
                        InstanceMappingPrefix.PROCEEDING
                            .getPrefix()
                            .concat(proceeding.getId().toString()))));
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

    final Optional<AssessmentEntityDetail> globalEntity =
        globalEntities.stream()
            .filter(
                entity -> assessment.getCaseReferenceNumber().equalsIgnoreCase(entity.getName()))
            .findFirst();

    globalEntity.ifPresent(
        entity -> {
          final AssessmentRelationshipDetail relationship =
              getEntityRelationship(globalEntity.get(), assessmentRelationship);

          if (relationship != null) {
            relationship
                .getRelationshipTargets()
                .removeIf(
                    target -> {
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
   * @param isReassessment whether this is the standalone means reassessment journey, which reuses
   *     the prior assessment's data rather than stripping the "do not reuse" attributes (old PUI's
   *     StartOpaReassessment, unlike the amend-case StartOpaAssessment, applies no such strip)
   */
  public void startAssessment(
      final ApplicationDetail application,
      final AssessmentRulebase assessmentRulebase,
      final ClientDetail client,
      final UserDetail user,
      final boolean isReassessment) {

    final String providerId = user.getProvider().getId().toString();
    final String referenceId = application.getCaseReferenceNumber();

    // Preserve a finished assessment on mere re-entry. Re-opening the assessment page would
    // otherwise delete the COMPLETE assessment and rebuild an empty INCOMPLETE shell, discarding
    // the
    // completed result - including the evidence outputs the document-upload checklist reads back.
    // Only wipe and rebuild when the assessment is not yet complete, or when the application data
    // has
    // changed since it was built (a genuine reassessment, detected by the same staleness check used
    // for the prepop checkpoint). When preserved, the existing prepop's checkpoint still drives the
    // OPA RESUME so the interview can be viewed or re-submitted.
    final AssessmentDetail existingAssessment =
        findOrCreate(providerId, referenceId, assessmentRulebase.getName());
    // Fetch the prepop only when the assessment is COMPLETE - it is only needed to decide
    // preservation, so skipping it on the common path avoids an extra assessment-api call.
    if (existingAssessment.getId() != null
        && COMPLETE.getStatus().equalsIgnoreCase(existingAssessment.getStatus())) {
      final AssessmentDetail existingPrepop =
          findOrCreate(providerId, referenceId, assessmentRulebase.getPrePopAssessmentName());
      if (existingPrepop.getId() != null
          && !isAssessmentCheckpointToBeDeleted(application, existingPrepop)) {
        log.info(
            "Preserving COMPLETE assessment [{}] for case [{}] on re-entry - application data "
                + "unchanged, so the completed result is kept rather than wiped and rebuilt.",
            assessmentRulebase.getName(),
            referenceId);
        return;
      }
    }

    // remove previous assessment
    deleteAssessments(
            user, List.of(assessmentRulebase.getName()), application.getCaseReferenceNumber(), null)
        .block();

    // start new assessment
    startNewAssessment(assessmentRulebase, application, client, user, isReassessment);
  }

  /**
   * Starts a new assessment based on the given rulebase, application, client, and user details.
   *
   * @param assessmentRulebase the rulebase for the assessment
   * @param application the application details
   * @param client the client details
   * @param user the user details
   * @param isReassessment whether this is the standalone means reassessment journey
   */
  protected void startNewAssessment(
      final AssessmentRulebase assessmentRulebase,
      final ApplicationDetail application,
      final ClientDetail client,
      final UserDetail user,
      final boolean isReassessment) {
    log.debug("Name - {}, AssessmentType - {}", user.getUsername(), assessmentRulebase.getType());
    final String referenceId = application.getCaseReferenceNumber();
    final String providerId = user.getProvider().getId().toString();

    final boolean prepopulateFromEbs = Boolean.TRUE.equals(application.getAmendment());

    final List<AssessmentEntityType> opaEntitiesRetrievedFromEbs = null;

    // find or Create
    final AssessmentDetail assessment =
        findOrCreate(providerId, referenceId, assessmentRulebase.getName());
    AssessmentDetail prepopAssessment =
        findOrCreate(providerId, referenceId, assessmentRulebase.getPrePopAssessmentName());

    // If a persisted prepop has gone stale (its source application data has changed since it was
    // built) regenerate it from scratch so newly added opponents/proceedings - e.g. a child party
    // required by a new proceeding - reach the OPA interview. The mapper rebuilds the whole entity
    // graph, which the assessment-api cannot apply as an update to an existing prepop, so the
    // stale one is deleted and rebuilt as a fresh insert. An unchanged prepop is left untouched so
    // its checkpoint continues to drive OPA RESUME.
    if (prepopAssessment.getId() != null
        && isAssessmentCheckpointToBeDeleted(application, prepopAssessment)) {
      deleteAssessments(
              user, List.of(assessmentRulebase.getPrePopAssessmentName()), referenceId, null)
          .block();
      prepopAssessment =
          findOrCreate(providerId, referenceId, assessmentRulebase.getPrePopAssessmentName());
    }

    final boolean createdNewPrepopAssessment = prepopAssessment.getId() == null;

    // used to populate the lookups for title for the opponent
    final List<AssessmentOpponentMappingContext> opponentContext =
        getAssessmentOpponentMappingContexts(application);

    final AssessmentMappingContext assessmentContext =
        AssessmentMappingContext.builder()
            .application(application)
            .opponentContext(opponentContext)
            .assessment(assessment)
            .client(client)
            .user(user)
            .build();

    // Only map the prepop when it is (re)created here - an existing prepop keeps its persisted
    // entity graph (and ids) so it can be saved as an update. The main assessment is always new
    // (deleted in startAssessment) so it is always mapped fresh.
    if (createdNewPrepopAssessment) {
      assessmentMapper.toAssessmentDetail(prepopAssessment, assessmentContext);
    }
    assessmentMapper.toAssessmentDetail(assessment, assessmentContext);

    updateCostLimitIfMeritsAssessment(assessmentRulebase, application, user);

    if (prepopulateFromEbs) {
      if (createdNewPrepopAssessment) {
        prepopulateAssessmentFromEbs(application, assessmentRulebase, prepopAssessment);
      }
      prepopulateAssessmentFromEbs(application, assessmentRulebase, assessment);
    }

    // An amend-case assessment must not reuse the answers the rulebase marks "Do Not Reuse"
    // (merits)
    // / "Short Term Reuse" (means) - the evidence families among them. Old PUI's amend-case path
    // (StartOpaAssessment) clears them from both sessions so the provider is asked again. The
    // standalone means reassessment (StartOpaReassessment) applies no such strip - it reuses the
    // prior EBS data - so the strip is skipped here for the reassessment journey, otherwise the
    // EBS-seeded means inputs would be removed and the means goal (CLIENT_PROV_LA) could not
    // resolve.
    if (prepopulateFromEbs && !assessmentRulebase.isFinancialAssessment() && !isReassessment) {
      removeNonReusableAttributes(prepopAssessment, assessmentRulebase);
      removeNonReusableAttributes(assessment, assessmentRulebase);
    }

    // if means and merits:
    if (!assessmentRulebase.isFinancialAssessment()) {
      if (isAssessmentReferenceConsistent(prepopAssessment)
          && isAssessmentReferenceConsistent(assessment)) {

        // call opa - save to database
        saveAssessment(user, assessment).block();
        saveAssessment(user, prepopAssessment).block();
      } else {
        log.info("pre-pop assessment or assessment data is corrupted!");
        throw new CaabApplicationException("pre-pop assessment or assessment data is corrupted!");
      }

    } else {
      // todo - later implementation in future story
    }
  }

  /**
   * Clears the attributes that must not be reused on an amendment from an assessment's entities.
   *
   * @param assessment the assessment being started, may be {@code null}
   * @param assessmentRulebase the rulebase being run
   */
  void removeNonReusableAttributes(
      final AssessmentDetail assessment, final AssessmentRulebase assessmentRulebase) {

    final Set<String> nonReusableAttributes =
        AssessmentReuseUtil.getNonReusableAttributes(assessmentRulebase);

    if (assessment == null
        || assessment.getEntityTypes() == null
        || nonReusableAttributes.isEmpty()) {
      return;
    }

    // The attribute lists are not necessarily mutable, so replace rather than remove in place.
    assessment.getEntityTypes().stream()
        .filter(entityType -> entityType.getEntities() != null)
        .flatMap(entityType -> entityType.getEntities().stream())
        .filter(entity -> entity.getAttributes() != null)
        .forEach(
            entity ->
                entity.setAttributes(
                    entity.getAttributes().stream()
                        .filter(attribute -> !nonReusableAttributes.contains(attribute.getName()))
                        .collect(Collectors.toCollection(ArrayList::new))));
  }

  void prepopulateAssessmentFromEbs(
      final ApplicationDetail application,
      final AssessmentRulebase assessmentRulebase,
      final AssessmentDetail assessment) {
    final AssessmentResult assessmentResult =
        getEbsAssessmentResult(application, assessmentRulebase);

    if (assessmentResult == null || assessmentResult.getAssessmentDetails() == null) {
      return;
    }

    assessmentResult.getAssessmentDetails().stream()
        .filter(Objects::nonNull)
        .filter(screen -> screen.getEntity() != null)
        .flatMap(screen -> screen.getEntity().stream())
        .filter(Objects::nonNull)
        .forEach(opaEntity -> mergeOpaEntityIntoAssessment(assessment, opaEntity));
  }

  private AssessmentResult getEbsAssessmentResult(
      final ApplicationDetail application, final AssessmentRulebase assessmentRulebase) {
    if (AssessmentRulebase.MEANS.equals(assessmentRulebase)) {
      return application.getMeansAssessment();
    }

    if (AssessmentRulebase.MERITS.equals(assessmentRulebase)) {
      return application.getMeritsAssessment();
    }

    return null;
  }

  private void mergeOpaEntityIntoAssessment(
      final AssessmentDetail assessment, final OpaEntity opaEntity) {
    if (opaEntity.getEntityName() == null || opaEntity.getInstances() == null) {
      return;
    }

    final AssessmentEntityTypeDetail entityType =
        findEntityType(assessment, opaEntity.getEntityName());

    if (entityType == null) {
      return;
    }

    opaEntity.getInstances().stream()
        .filter(Objects::nonNull)
        .forEach(opaInstance -> mergeOpaInstanceIntoEntityType(entityType, opaInstance));
  }

  private AssessmentEntityTypeDetail findEntityType(
      final AssessmentDetail assessment, final String entityTypeName) {
    return Optional.ofNullable(assessment.getEntityTypes()).orElseGet(List::of).stream()
        .filter(entityType -> entityTypeName.equalsIgnoreCase(entityType.getName()))
        .findFirst()
        .orElse(null);
  }

  private void mergeOpaInstanceIntoEntityType(
      final AssessmentEntityTypeDetail entityType, final OpaInstance opaInstance) {
    if (opaInstance.getInstanceLabel() == null || opaInstance.getAttributes() == null) {
      return;
    }

    final AssessmentEntityDetail entity = findEntity(entityType, opaInstance);

    if (entity == null) {
      return;
    }

    opaInstance.getAttributes().stream()
        .filter(Objects::nonNull)
        .forEach(opaAttribute -> mergeOpaAttributeIntoEntity(entity, opaAttribute));
  }

  private AssessmentEntityDetail findEntity(
      final AssessmentEntityTypeDetail entityType, final OpaInstance opaInstance) {
    return Optional.ofNullable(entityType.getEntities()).orElseGet(List::of).stream()
        .filter(entity -> opaInstance.getInstanceLabel().equalsIgnoreCase(entity.getName()))
        .findFirst()
        .orElse(null);
  }

  private void mergeOpaAttributeIntoEntity(
      final AssessmentEntityDetail entity, final OpaAttribute opaAttribute) {
    if (opaAttribute.getAttribute() == null || getOpaAttributeValue(opaAttribute) == null) {
      return;
    }

    if (entity.getAttributes() == null) {
      entity.setAttributes(new ArrayList<>());
    }

    entity.getAttributes().stream()
        .filter(attribute -> opaAttribute.getAttribute().equalsIgnoreCase(attribute.getName()))
        .findFirst()
        .ifPresentOrElse(
            existingAttribute -> {
              if (existingAttribute.getValue() == null) {
                existingAttribute.setValue(getOpaAttributeValue(opaAttribute));
                existingAttribute.setPrepopulated(true);
              }
            },
            () ->
                entity.addAttributesItem(
                    new AssessmentAttributeDetail()
                        .name(opaAttribute.getAttribute())
                        .type(opaAttribute.getResponseType())
                        .value(getOpaAttributeValue(opaAttribute))
                        .prepopulated(true)));
  }

  private String getOpaAttributeValue(final OpaAttribute opaAttribute) {
    return opaAttribute.getResponseValue() != null
        ? opaAttribute.getResponseValue()
        : opaAttribute.getResponseText();
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

      final CommonLookupValueDetail titleCommonLookup =
          lookupService
              .getCommonValue(COMMON_VALUE_CONTACT_TITLE, opponent.getTitle())
              .map(
                  commonLookupValueDetail ->
                      commonLookupValueDetail.orElse(
                          new CommonLookupValueDetail()
                              .code(opponent.getTitle())
                              .description(opponent.getTitle())))
              .blockOptional()
              .orElseThrow();

      opponentContext.add(
          AssessmentOpponentMappingContext.builder()
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
      final String providerId, final String referenceId, final String assessmentName) {

    final AssessmentDetails assessments =
        getAssessments(List.of(assessmentName), providerId, referenceId).block();

    if (assessments != null && !assessments.getContent().isEmpty()) {
      return getMostRecentAssessmentDetail(assessments.getContent());
    } else {
      final AssessmentDetail assessment =
          new AssessmentDetail()
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
      final ApplicationDetail application, final AssessmentDetail assessment) {
    boolean mismatch = false;
    final Date dateOfLastChange = getDateOfLatestKeyChange(application);

    if (dateOfLastChange != null) {
      // The application changed after the assessment was last saved -> mismatch.
      mismatch = dateOfLastChange.after(assessment.getAuditDetail().getLastSaved());
    }

    return mismatch || isProceedingsCountMismatch(application, assessment);
  }

  /**
   * Checks if any proceeding entity in the assessment does not exist in the application's
   * proceedings.
   *
   * @param application the application details containing proceedings
   * @param assessment the assessment details containing proceeding entities
   * @return true if a proceeding entity in the assessment does not exist in the application, or if
   *     there is a mismatch with application proceedings, otherwise false
   */
  protected boolean isProceedingsCountMismatch(
      final ApplicationDetail application, final AssessmentDetail assessment) {

    final List<AssessmentEntityDetail> proceedingEntities =
        getAssessmentEntitiesForEntityType(assessment, PROCEEDING);

    // For amendments the assessment is built from the live proceedings plus any draft amendment
    // proceedings from EBS, so compare against that same set rather than the live proceedings
    // alone. Otherwise an unchanged amendment always looks like a mismatch and its OPA checkpoint
    // is needlessly deleted on every relaunch.
    return ProceedingUtil.getAssessmentProceedings(application).size() != proceedingEntities.size()
        || isAssessmentProceedingsMatchingApplication(application, assessment);
  }

  /**
   * Checks if any proceeding entity in the assessment does not exist in the application's
   * proceedings.
   *
   * @param application the application details containing proceedings
   * @param assessment the assessment details containing proceeding entities
   * @return true if a proceeding entity in the assessment does not exist in the application, or if
   *     there is a mismatch with application proceedings, otherwise false
   */
  protected boolean isAssessmentProceedingsMatchingApplication(
      final ApplicationDetail application, final AssessmentDetail assessment) {

    boolean mismatch = false;

    final List<AssessmentEntityDetail> proceedingEntities =
        getAssessmentEntitiesForEntityType(assessment, PROCEEDING);

    // Resolve assessment proceeding entities against the amendment-inclusive set (live plus draft
    // amendment proceedings), matching on the OPA instance mapping id used to build each entity, so
    // a draft amendment proceeding is not mistaken for one that has been removed.
    final List<ProceedingDetail> expectedProceedings =
        ProceedingUtil.getAssessmentProceedings(application);

    for (final AssessmentEntityDetail proceedingEntity : proceedingEntities) {
      final boolean proceedingExists =
          expectedProceedings.stream()
              .anyMatch(
                  proceeding ->
                      proceedingEntity.getName().equals(getAssessmentMappingId(proceeding)));

      if (!proceedingExists) {
        // A proceeding entity in the assessment no longer exists in the application -> mismatch.
        mismatch = true;
      }
    }
    return mismatch || isApplicationProceedingsMatchingAssessment(application, assessment);
  }

  /**
   * Checks if the proceedings in the application exist in the assessment's session and if their
   * scopes match.
   *
   * @param application the application details containing proceedings
   * @param assessment the assessment details to check against
   * @return true if a proceeding in the application does not exist in the assessment or if the
   *     scope of any proceeding has changed, otherwise false
   */
  protected boolean isApplicationProceedingsMatchingAssessment(
      final ApplicationDetail application, final AssessmentDetail assessment) {

    boolean mismatch = false;
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
          // The proceeding's requested scope has changed -> mismatch.
          log.debug("Looks like scope is change, hence return true");
          mismatch = true;
        }
      } else {
        // An application proceeding has no matching assessment entity -> mismatch.
        log.debug("APP PROCEEDINGS DOESN'T EXIST IN OPASESSION OBJECT");
        mismatch = true;
      }
    }
    return mismatch || isOpponentCountMatchingAssessments(application, assessment);
  }

  /**
   * Checks if the number of opponents in the application matches the number of opponent entities in
   * the assessment and verifies the opponent details.
   *
   * @param application the application details containing opponents
   * @param assessment the assessment details containing opponent entities
   * @return true if the opponent counts do not match or if an opponent entity in the assessment
   *     does not exist in the application, otherwise false
   */
  protected boolean isOpponentCountMatchingAssessments(
      final ApplicationDetail application, final AssessmentDetail assessment) {

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
   *     otherwise false
   */
  protected boolean isAssessmentOpponentsMatchingApplication(
      final ApplicationDetail application, final AssessmentDetail assessment) {

    boolean mismatch = false;

    final List<AssessmentEntityDetail> opponentEntities =
        getAssessmentEntitiesForEntityType(assessment, OPPONENT);

    for (final AssessmentEntityDetail opponentEntity : opponentEntities) {
      log.debug("Assessment Opponent EntityID : " + opponentEntity.getName());

      final String opponentId =
          opponentEntity.getName().replaceFirst(InstanceMappingPrefix.OPPONENT.getPrefix(), "");

      final boolean opponentExists =
          getOpponentByEbsId(application, opponentEntity.getName()) != null
              || getOpponentById(application, Integer.parseInt(opponentId)) != null;

      if (!opponentExists) {
        // An opponent entity in the assessment no longer exists in the application -> mismatch.
        log.debug("OPA SESSION OPPONENT DOESN'T EXIST IN APPLICATION OBJECT");
        mismatch = true;
      }
    }
    return mismatch || isApplicationOpponentsMatchingAssessments(application, assessment);
  }

  /**
   * Checks if any opponent in the application does not exist in the assessment's OPA session.
   *
   * @param application the application details containing opponents
   * @param assessment the assessment details to check against
   * @return true if an opponent in the application does not exist in the assessment, otherwise
   *     false
   */
  protected boolean isApplicationOpponentsMatchingAssessments(
      final ApplicationDetail application, final AssessmentDetail assessment) {

    boolean mismatch = false;

    for (final OpponentDetail opponent : application.getOpponents()) {
      log.debug("App opponent ID - " + opponent.getId() + ", EBS-ID - " + opponent.getEbsId());

      final String opponentId = OpponentUtil.getAssessmentMappingId(opponent);

      log.debug("Opponent ID is " + opponentId);

      final AssessmentEntityTypeDetail opponentEntityType =
          getAssessmentEntityType(assessment, OPPONENT);

      final AssessmentEntityDetail opponentEntity =
          getAssessmentEntity(opponentEntityType, opponentId);

      if (opponentEntity == null) {
        // An application opponent has no matching assessment entity -> mismatch.
        log.debug("APP OPPONENTS DOESN'T EXIST IN OPASESSION OBJECT");
        mismatch = true;
      }
    }
    return mismatch || applicationTypeMatches(application, assessment);
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

      application
          .getCostLimit()
          .setLimitAtTimeOfMerits(application.getCosts().getRequestedCostLimitation());

      final CostLimitDetail costLimit = application.getCostLimit();
      costLimit.setLimitAtTimeOfMerits(application.getCosts().getRequestedCostLimitation());

      final ApplicationDetail patch = new ApplicationDetail().costLimit(costLimit);
      caabApiClient
          .patchApplication(String.valueOf(application.getId()), patch, user.getLoginId())
          .block();
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

    // loop through all parent summary lookups
    for (final AssessmentSummaryEntityLookupValueDetail parentSummaryLookup :
        parentSummaryLookups) {
      log.debug("Parent Summary: {}", parentSummaryLookup.getDisplayName());

      // get all entities for the parent summary lookup
      final List<AssessmentEntityDetail> entities =
          getAssessmentEntitiesForEntityType(assessment, parentSummaryLookup.getName());

      // loop through all entities in the assessment where the entity type matches
      // the parent summary lookup
      for (final AssessmentEntityDetail entity : entities) {
        log.debug("Entity: {}", entity.getName());
        createSummaryEntity(
            assessment, summaryToDisplay, childSummaryLookups, parentSummaryLookup, entity);
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

    // we have the entity from the assessment where it matched the parent summary lookup

    // need to stream through each attribute in the summaryEntityLookup
    // check it matches the assessment entity attributes, then add it to a list if matches
    final List<AssessmentSummaryAttributeDisplay> attributesToDisplay =
        summaryEntityLookup.getAttributes().stream()
            .map(attributeLookup -> getAssessmentAttribute(entity, attributeLookup.getName()))
            .filter(Objects::nonNull)
            .map(attribute -> createSummaryAttributeDisplay(attribute, summaryEntityLookup))
            .filter(Objects::nonNull)
            .toList();

    if (!attributesToDisplay.isEmpty()) {
      // if we have data in the list then we can create a summary display entity
      final AssessmentSummaryEntityDisplay summaryEntityToDisplay =
          new AssessmentSummaryEntityDisplay(
              summaryEntityLookup.getName(),
              summaryEntityLookup.getDisplayName(),
              summaryEntityLookup.getEntityLevel());

      // then we add all the attributes to the summary entity
      summaryEntityToDisplay.getAttributes().addAll(attributesToDisplay);
      // then add the summary entity to the list
      summaryEntitiesToDisplay.add(summaryEntityToDisplay);

      // Child entities to add
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
        .map(
            attr ->
                new AssessmentSummaryAttributeDisplay(
                    attr.getName(),
                    getDisplayNameForAttribute(summaryEntityLookup, attr.getName()),
                    formattedAttributeValue))
        // If the condition is not met, return null
        .orElse(null);
  }
}
