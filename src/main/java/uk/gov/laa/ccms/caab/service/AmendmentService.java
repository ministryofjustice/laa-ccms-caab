package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.builders.ApplicationTypeBuilder;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.ApplicationConstants;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.SoaApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.context.CaseMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionStatusDisplay;
import uk.gov.laa.ccms.caab.util.AmendmentUtil;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseTransactionResponse;

/**
 * Service class responsible for handling amendments to existing legal aid cases.
 *
 * <p>This class provides methods for creating and submitting amendments as well as retrieving
 * application section details specific to amendments. It interacts with the application service and
 * CAAB API client to perform the necessary operations.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmendmentService {

  private final ApplicationService applicationService;
  private final CaabApiClient caabApiClient;
  private final SoaApiClient soaApiClient;
  private final SoaApplicationMapper soaApplicationMapper;

  /**
   * Creates and submits an amendment for an existing case using the provided application details
   * and user information.
   *
   * @param applicationFormData the data for the application form, including the application type
   *     and delegated function details
   * @param caseDetail the case for which the amendment will be created
   * @param userDetail the details of the user submitting the amendment, including provider and
   *     login information
   * @return the detailed information of the created amendment application
   */
  public ApplicationDetail createAndSubmitAmendmentForCase(
      final ApplicationFormData applicationFormData,
      final ApplicationDetail caseDetail,
      final UserDetail userDetail) {
    final String caseReferenceNumber = caseDetail.getCaseReferenceNumber();

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference(caseReferenceNumber);
    boolean applicationExists =
        applicationService
            .getTdsApplications(caseSearchCriteria, userDetail, 0, 1)
            .getContent()
            .stream()
            .findFirst()
            .isPresent();
    if (applicationExists) {
      throw new CaabApplicationException(
          "Application already exists for case reference: " + caseReferenceNumber);
    }

    ApplicationDetail amendment = createAmendmentObject(caseReferenceNumber, userDetail);
    amendment.setCategoryOfLaw(caseDetail.getCategoryOfLaw());
    amendment.setAvailableFunctions(caseDetail.getAvailableFunctions());

    // Set application type based on previously entered answers prior to creating an amendment.
    ApplicationType amendmentType =
        new ApplicationTypeBuilder()
            .applicationType(
                applicationFormData.getApplicationTypeCategory(),
                applicationFormData.isDelegatedFunctions())
            .devolvedPowers(
                applicationFormData.isDelegatedFunctions(),
                applicationFormData.getDelegatedFunctionUsedDate())
            .build();

    amendment.setApplicationType(amendmentType);

    // Create application/amendment in TDS.
    Mono<String> application = caabApiClient.createApplication(userDetail.getLoginId(), amendment);
    log.info("Application created: {}", application.block());
    return amendment;
  }

  /**
   * Creates a draft TDS amendment for the standalone means reassessment quick amendment journey.
   *
   * @param caseDetail the existing case to reassess
   * @param userDetail the user creating the reassessment
   * @return the created amendment application id
   */
  public String createMeansReassessmentForCase(
      final ApplicationDetail caseDetail, final UserDetail userDetail) {
    final String caseReferenceNumber = caseDetail.getCaseReferenceNumber();

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference(caseReferenceNumber);
    boolean applicationExists =
        applicationService
            .getTdsApplications(caseSearchCriteria, userDetail, 0, 1)
            .getContent()
            .stream()
            .findFirst()
            .isPresent();
    if (applicationExists) {
      throw new CaabApplicationException(
          "Application already exists for case reference: " + caseReferenceNumber);
    }

    ApplicationDetail amendment = createAmendmentObject(caseReferenceNumber, userDetail);
    amendment.setCategoryOfLaw(caseDetail.getCategoryOfLaw());
    amendment.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_MEANS_REASSESSMENT);
    amendment.setMeansAssessmentAmended(Boolean.FALSE);
    amendment.setMeritsAssessmentAmended(Boolean.FALSE);

    return caabApiClient.createApplication(userDetail.getLoginId(), amendment).block();
  }

  private ApplicationDetail createAmendmentObject(
      String caseReferenceNumber, UserDetail userDetail) {

    ApplicationDetail amendment =
        applicationService.getCase(
            caseReferenceNumber, userDetail.getProvider().getId(), userDetail.getLoginId());

    // Set the amendment type
    amendment.setAmendment(true);

    // Set cost limit changed flag to false if it exists
    if (!Objects.isNull(amendment.getCostLimit())) {
      amendment.getCostLimit().setChanged(false);
    } else {
      amendment.setCostLimit(new CostLimitDetail().changed(false));
    }

    // Merits status is unchange, if the requested cost limit is increased then the merits
    // assessment needs to be redone.
    amendment
        .getCostLimit()
        .setLimitAtTimeOfMerits(amendment.getCosts().getRequestedCostLimitation());
    amendment.setStatus(
        new StringDisplayValue()
            .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
            .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY));

    // Update all linked cases
    amendment.getLinkedCases().forEach(x -> x.setId(null));

    // Update all proceedings
    amendment
        .getProceedings()
        .forEach(
            x -> {
              x.setId(null);
              x.setStatus(
                  new StringDisplayValue()
                      .id(STATUS_DRAFT)
                      .displayValue(PROCEEDING_STATUS_UNCHANGED_DISPLAY));
            });

    // Update prior authorities
    amendment.getPriorAuthorities().forEach(x -> x.setId(null));

    // Update opponents
    amendment
        .getOpponents()
        .forEach(
            x -> {
              x.setConfirmed(true);
              x.setId(null);
              x.setAmendment(true);
              x.setAppMode(false);
              x.setAward(false);
            });

    // assessmentService.calculateAssessmentStatuses(amendment, );
    // TODO: Add merits & means assessments ~ Awaiting on CCMSPUI-380
    return amendment;
  }

  /**
   * Submits a quick amendment to the correspondence address for a given case. This method creates a
   * quick amendment application, applies the new correspondence address details, and submits the
   * amendment. Finally, a case is updated which returns the transaction ID associated with the
   * submission.
   *
   * @param editCorrespondenceAddress the data representing the updated correspondence address
   * @param caseReferenceNumber the unique reference number of the case to which the amendment
   *     applies
   * @param userDetail the details of the user initiating the amendment
   * @return the transaction ID of the submitted amendment
   */
  public String submitQuickAmendmentCorrespondenceAddress(
      final AddressFormData editCorrespondenceAddress,
      final String caseReferenceNumber,
      final UserDetail userDetail) {
    ApplicationDetail amendment = createAmendmentObject(caseReferenceNumber, userDetail);
    amendment.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_CASE_CORRESPONDENCE_PREFERENCE);
    amendment.setMeansAssessmentAmended(Boolean.FALSE);
    amendment.setMeritsAssessmentAmended(Boolean.FALSE);

    AddressDetail address = amendment.getCorrespondenceAddress();
    if (Objects.isNull(address)) {
      address = new AddressDetail();
    }

    address.setAddressLine1(editCorrespondenceAddress.getAddressLine1());
    address.setAddressLine2(editCorrespondenceAddress.getAddressLine2());
    address.setCareOf(editCorrespondenceAddress.getCareOf());
    address.setCity(editCorrespondenceAddress.getCityTown());
    address.setCountry(editCorrespondenceAddress.getCountry());
    address.setCounty(editCorrespondenceAddress.getCounty());
    address.setHouseNameOrNumber(editCorrespondenceAddress.getHouseNameNumber());
    address.setPostcode(editCorrespondenceAddress.getPostcode());
    address.setPreferredAddress(editCorrespondenceAddress.getPreferredAddress());

    return updateCaseWithQuickAmendment(userDetail, amendment);
  }

  /**
   * Submits a quick amendment to the provider details for a given case. This method creates a quick
   * amendment application, applies the new provider details, and submits the amendment. Finally, a
   * case is updated which returns the transaction ID associated with the submission.
   *
   * @param providerDetails the data representing the updated provider details
   * @param caseReferenceNumber the unique reference number of the case to which the amendment
   *     applies
   * @param userDetail the details of the user initiating the amendment
   * @return the transaction ID of the submitted amendment
   */
  public String submitQuickAmendmentProviderDetails(
      final ApplicationFormData providerDetails,
      final String caseReferenceNumber,
      final UserDetail userDetail) {
    ApplicationDetail amendment = createAmendmentObject(caseReferenceNumber, userDetail);
    amendment.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_EDIT_PROVIDER);
    amendment.setMeansAssessmentAmended(Boolean.FALSE);
    amendment.setMeritsAssessmentAmended(Boolean.FALSE);

    amendment
        .getProviderDetails()
        .setOffice(new IntDisplayValue().id(providerDetails.getOfficeId()));
    amendment
        .getProviderDetails()
        .setFeeEarner(
            providerDetails.getFeeEarnerId() != null
                ? new StringDisplayValue().id(String.valueOf(providerDetails.getFeeEarnerId()))
                : null);
    amendment
        .getProviderDetails()
        .setSupervisor(
            providerDetails.getSupervisorId() != null
                ? new StringDisplayValue().id(String.valueOf(providerDetails.getSupervisorId()))
                : null);
    amendment
        .getProviderDetails()
        .setProviderCaseReference(providerDetails.getProviderCaseReference());
    amendment
        .getProviderDetails()
        .setProviderContact(new StringDisplayValue().id(providerDetails.getContactNameId()));

    return updateCaseWithQuickAmendment(userDetail, amendment);
  }

  /**
   * Submits a completed means reassessment as a legacy quick amendment.
   *
   * @param userDetail user submitting the reassessment
   * @param amendment draft reassessment amendment
   * @param meansAssessment completed means assessment to submit
   * @return the transaction id for the case update
   */
  public String submitMeansReassessment(
      final UserDetail userDetail,
      final ApplicationDetail amendment,
      final AssessmentDetail meansAssessment) {
    amendment.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_MEANS_REASSESSMENT);
    amendment.setMeansAssessmentAmended(Boolean.TRUE);
    amendment.setMeritsAssessmentAmended(Boolean.FALSE);

    return updateCaseWithQuickAmendment(userDetail, amendment, meansAssessment, null);
  }

  /**
   * Common update case logic for quick amendments.
   *
   * @param userDetail User updating the case.
   * @param amendment The amendment to be applied to the case.
   * @return Transaction ID of the updated case.
   */
  private String updateCaseWithQuickAmendment(UserDetail userDetail, ApplicationDetail amendment) {
    return updateCaseWithQuickAmendment(userDetail, amendment, null, null);
  }

  private String updateCaseWithQuickAmendment(
      UserDetail userDetail,
      ApplicationDetail amendment,
      AssessmentDetail meansAssessment,
      AssessmentDetail meritsAssessment) {
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendment);

    BaseApplicationDetail existingApplication =
        applicationService.getTdsApplicationSummary(amendment.getCaseReferenceNumber(), userDetail);

    if (existingApplication == null) {
      // Create an application in TDS
      caabApiClient.createApplication(userDetail.getLoginId(), amendment).block();
    }

    CaseMappingContext caseMappingContext =
        CaseMappingContext.builder()
            .tdsApplication(amendment)
            .meansAssessment(meansAssessment)
            .meritsAssessment(meritsAssessment)
            .caseDocs(Collections.emptyList())
            .user(userDetail)
            .build();
    CaseDetail caseToSubmit = soaApplicationMapper.toCaseDetail(caseMappingContext);

    Mono<CaseTransactionResponse> caseTransactionResponseMono =
        soaApiClient.updateCase(
            userDetail.getLoginId(),
            userDetail.getUserType(),
            caseToSubmit,
            amendment.getQuickEditType());

    return Objects.requireNonNull(caseTransactionResponseMono.block()).getTransactionId();
  }

  /**
   * Retrieves the opponents associated with an amendment.
   *
   * @param applicationId The ID of the application.
   * @param user The user details.
   * @return A list of opponent form data objects.
   */
  public List<AbstractOpponentFormData> getAmendmentOpponents(
      final String applicationId, final UserDetail user) {
    final ApplicationDetail application = applicationService.getApplication(applicationId).block();
    final ApplicationSectionDisplay amendmentSections = getAmendmentSections(application, user);

    return amendmentSections.getOpponentsAndOtherParties().getOpponents().stream()
        .map(
            opponent -> {
              OpponentDetail opponentDetail =
                  OpponentUtil.getOpponentById(application, opponent.getId());
              AbstractOpponentFormData formData =
                  applicationService.buildOpponentFormData(opponentDetail);
              formData.setEditable(true);
              formData.setDeletable(Boolean.TRUE.equals(opponentDetail.getDeleteInd()));
              return formData;
            })
        .toList();
  }

  /**
   * Retrieves the amendment-specific sections of an application. Additionally, enables the document
   * upload feature if certain conditions related to prior authorities or assessment completions are
   * met:
   *
   * <ul>
   *   <li>There is a draft prior authority.
   *   <li>Means assessment has been amended.
   *   <li>Merits assessment has been amended.
   * </ul>
   *
   * @param application The application details for which the amendment sections need to be fetched.
   * @param user The user details, providing context for retrieving and tailoring the sections.
   * @return An ApplicationSectionDisplay object containing the relevant sections for the amendment,
   *     with document upload enabled based on specific conditions.
   */
  public ApplicationSectionDisplay getAmendmentSections(
      final ApplicationDetail application, final UserDetail user) {
    final ApplicationSectionDisplay sectionDisplay =
        applicationService.getApplicationSections(application, user);

    // Old PUI shows the Means amendment section when the case has the MNLA function.
    final boolean meansLegalAmendmentAvailable =
        application.getAvailableFunctions() != null
            && application
                .getAvailableFunctions()
                .contains(FunctionConstants.MEANS_ASSESSMENT_LEGAL_AMENDMENT);

    if (sectionDisplay.getMeansAssessment() != null) {
      sectionDisplay.getMeansAssessment().setEnabled(meansLegalAmendmentAvailable);
      if (meansLegalAmendmentAvailable
          && !StringUtils.hasText(sectionDisplay.getMeansAssessment().getStatus())) {
        sectionDisplay
            .getMeansAssessment()
            .setStatus(ApplicationConstants.SECTION_STATUS_NOT_STARTED);
      }
    }

    // Old PUI (PrepareAmendment.validateAndEnableDocUploadLink) enables the document upload link
    // when a draft prior authority exists, or when the means OR merits assessment is complete.
    boolean isPriorAuthorityAdded =
        sectionDisplay.getPriorAuthorities().stream()
            .anyMatch(x -> "Draft".equalsIgnoreCase(x.getStatus()));
    boolean assessmentComplete =
        isAssessmentSectionComplete(sectionDisplay.getMeansAssessment())
            || isAssessmentSectionComplete(sectionDisplay.getMeritsAssessment());
    sectionDisplay.getDocumentUpload().setEnabled(isPriorAuthorityAdded || assessmentComplete);

    if (sectionDisplay.getDocumentUpload().isEnabled()) {
      sectionDisplay.getDocumentUpload().setStatus("Available");
    }

    return sectionDisplay;
  }

  private boolean isAssessmentSectionComplete(final ApplicationSectionStatusDisplay section) {
    return section != null
        && ApplicationConstants.SECTION_STATUS_COMPLETE.equals(section.getStatus());
  }

  public String submitAmendment(final ApplicationDetail amendment, final UserDetail userDetail) {
    CaseMappingContext caseMappingContext =
        CaseMappingContext.builder()
            .tdsApplication(amendment)
            .meansAssessment(null)
            .meritsAssessment(null)
            .caseDocs(Collections.emptyList())
            .user(userDetail)
            .build();
    CaseDetail caseToSubmit = soaApplicationMapper.toCaseDetail(caseMappingContext);

    Mono<CaseTransactionResponse> caseTransactionResponseMono =
        soaApiClient.updateCase(
            userDetail.getLoginId(),
            userDetail.getUserType(),
            caseToSubmit,
            amendment.getApplicationType().getId().toString());

    return Objects.requireNonNull(caseTransactionResponseMono.block()).getTransactionId();
  }
}
