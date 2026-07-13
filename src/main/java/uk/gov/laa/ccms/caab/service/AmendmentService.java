package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.CcmsModule.AMENDMENT;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.builders.ApplicationTypeBuilder;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.ApplicationConstants;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.SoaApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.context.CaseMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionStatusDisplay;
import uk.gov.laa.ccms.caab.util.AmendmentUtil;
import uk.gov.laa.ccms.caab.util.AssessmentUtil;
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
  private final AssessmentService assessmentService;
  private final EvidenceService evidenceService;

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
   * Submits a quick amendment to the case cost for a given case. This method creates a quick
   * amendment application, applies the new case costs, and submits the amendment. Finally, a case
   * is updated which returns the transaction ID associated with the submission.
   *
   * @param allocateCostsFormData the data representing the updated cost allocations
   * @param caseReferenceNumber the unique reference number of the case to which the amendment
   *     applies
   * @param userDetail the details of the user initiating the amendment
   * @return the transaction ID of the submitted amendment
   */
  public String submitQuickAmendmentCostAllocation(
      final AllocateCostsFormData allocateCostsFormData,
      final String caseReferenceNumber,
      final UserDetail userDetail) {

    if (allocateCostsFormData == null) {
      throw new CaabApplicationException(
          "AllocateCostsFormData is required for cost allocation amendment");
    }

    ApplicationDetail amendment = createAmendmentObject(caseReferenceNumber, userDetail);
    amendment.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_ALLOCATE_COST_LIMIT);
    amendment.setMeansAssessmentAmended(Boolean.FALSE);
    amendment.setMeritsAssessmentAmended(Boolean.FALSE);

    CostStructureDetail costs = amendment.getCosts();
    if (costs == null) {
      costs = new CostStructureDetail();
      amendment.setCosts(costs);
    }

    // Allocating the cost limit redistributes the granted limit between the provider and counsel;
    // it does not change the requested limit. Keep the case's existing value unless the form
    // carries one, otherwise the case would be submitted with its default limit instead.
    if (allocateCostsFormData.getRequestedCostLimitation() != null) {
      costs.setRequestedCostLimitation(allocateCostsFormData.getRequestedCostLimitation());
    }

    costs.setCostEntries(toSubmittableCostEntries(allocateCostsFormData.getCostEntries()));

    amendment.getCostLimit().setChanged(true);

    return updateCaseWithQuickAmendment(userDetail, amendment);
  }

  /**
   * Normalises the cost entries bound from the review screen so they can be submitted to EBS.
   *
   * <p>A newly added counsel has no EBS id. Its cost limit id must be absent rather than blank, as
   * EBS decides whether to insert or update a cost limitation on the presence of that id.
   *
   * @param costEntries the cost entries bound from the review form
   * @return the cost entries to submit, empty when none were supplied
   */
  private List<CostEntryDetail> toSubmittableCostEntries(final List<CostEntryDetail> costEntries) {
    if (costEntries == null) {
      return Collections.emptyList();
    }

    return costEntries.stream()
        .filter(Objects::nonNull)
        .map(
            entry -> {
              entry.setRequestedCosts(
                  entry.getRequestedCosts() == null
                      ? BigDecimal.ZERO
                      : entry.getRequestedCosts().setScale(2, RoundingMode.HALF_UP));

              if (entry.getAmountBilled() == null) {
                entry.setAmountBilled(BigDecimal.ZERO);
              }

              if (entry.getCostCategory() != null) {
                entry.setCostCategory(entry.getCostCategory().toUpperCase());
              }

              if (entry.getEbsId() != null && entry.getEbsId().isBlank()) {
                entry.setEbsId(null);
              }

              return entry;
            })
        .collect(Collectors.toList());
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

    // Register and transfer any documents uploaded against this amendment so they are attached to
    // the case update, matching the legacy provider UI amendment submission behaviour.
    final List<BaseEvidenceDocumentDetail> caseDocs =
        registerAndUploadAmendmentDocuments(amendment.getCaseReferenceNumber(), userDetail);

    CaseMappingContext caseMappingContext =
        CaseMappingContext.builder()
            .tdsApplication(amendment)
            .meansAssessment(meansAssessment)
            .meritsAssessment(meritsAssessment)
            .caseDocs(caseDocs)
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
   * Register and upload any documents that were uploaded against this amendment (CCMS module "M")
   * so they are transferred to EBS and can be attached to the case update. This mirrors the legacy
   * provider UI, which on amendment submission registers previously uploaded documents, uploads
   * their content, and includes them as case documents on the case update request.
   *
   * @param caseReferenceNumber the case reference the documents belong to.
   * @param userDetail the user submitting the amendment.
   * @return the amendment evidence documents to attach to the case update, carrying their EBS
   *     registered document ids; an empty list when no documents were uploaded.
   */
  private List<BaseEvidenceDocumentDetail> registerAndUploadAmendmentDocuments(
      final String caseReferenceNumber, final UserDetail userDetail) {

    final EvidenceDocumentDetails evidenceDocuments =
        evidenceService.getEvidenceDocumentsForCase(caseReferenceNumber, AMENDMENT).block();

    if (evidenceDocuments == null
        || evidenceDocuments.getContent() == null
        || evidenceDocuments.getContent().isEmpty()) {
      return Collections.emptyList();
    }

    // Register any not-yet-registered documents in EBS and patch the TDS with the returned ids.
    evidenceService.registerPreviouslyUploadedDocuments(evidenceDocuments, userDetail);

    // Upload the document content to EBS and update each document's transfer status.
    evidenceService
        .uploadAndUpdateDocuments(evidenceDocuments, caseReferenceNumber, null, userDetail)
        .block();

    // Re-fetch so the attached case documents carry the registered document ids.
    final EvidenceDocumentDetails updatedDocuments =
        evidenceService.getEvidenceDocumentsForCase(caseReferenceNumber, AMENDMENT).block();

    return updatedDocuments != null && updatedDocuments.getContent() != null
        ? updatedDocuments.getContent()
        : Collections.emptyList();
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

    final String caseUpdateType = "LegalAmendment";

    final AssessmentDetail meansAssessment =
        getMostRecentAmendmentAssessment(AssessmentRulebase.MEANS, amendment, userDetail);
    final AssessmentDetail meritsAssessment =
        getMostRecentAmendmentAssessment(AssessmentRulebase.MERITS, amendment, userDetail);

    assessmentService.calculateAssessmentStatuses(
        amendment, meansAssessment, meritsAssessment, userDetail);

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
            userDetail.getLoginId(), userDetail.getUserType(), caseToSubmit, caseUpdateType);

    CaseTransactionResponse response = Objects.requireNonNull(caseTransactionResponseMono.block());

    updateTdsAmendmentStatusToSubmitted(amendment, userDetail);

    return response.getTransactionId();
  }

  private void updateTdsAmendmentStatusToSubmitted(ApplicationDetail amendment, UserDetail user) {
    try {
      final ApplicationDetail status_patch = new ApplicationDetail();
      status_patch.setStatus(
          new StringDisplayValue().id(ApplicationConstants.PROCEEDING_STATUS_SUBMITTED_DISPLAY));

      applicationService.patchApplication(String.valueOf(amendment.getId()), status_patch, user);
    } catch (Exception e) {
      log.warn("Failed to update the TDS amendment status to Submitted");
    }
  }

  private AssessmentDetail getMostRecentAmendmentAssessment(
      final AssessmentRulebase rulebase,
      final ApplicationDetail amendment,
      final UserDetail userDetail) {
    return assessmentService
        .getAssessments(
            List.of(rulebase.getName()),
            userDetail.getProvider().getId().toString(),
            amendment.getCaseReferenceNumber())
        .mapNotNull(details -> AssessmentUtil.getMostRecentAssessmentDetail(details.getContent()))
        .blockOptional()
        .orElse(null);
  }
}
