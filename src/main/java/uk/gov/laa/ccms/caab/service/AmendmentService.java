package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;

import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.builders.ApplicationTypeBuilder;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Service class responsible for handling amendments to existing legal aid cases.
 *
 * <p>This class provides methods for creating and submitting amendments as well as retrieving
 * application section details specific to amendments. It interacts with the application
 * service and
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

  /**
   * Creates and submits an amendment for an existing case using the provided application details
   * and user information.
   *
   * @param applicationFormData the data for the application form, including the application type
   *     and delegated function details
   * @param caseReferenceNumber the reference number of the case for which the amendment will be
   *     created
   * @param userDetail the details of the user submitting the amendment, including provider and
   *     login information
   * @return the detailed information of the created amendment application
   */
  public ApplicationDetail createAndSubmitAmendmentForCase(
      final ApplicationFormData applicationFormData,
      final String caseReferenceNumber,
      final UserDetail userDetail) {

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

    ApplicationDetail amendment =
        applicationService.getCase(
            caseReferenceNumber, userDetail.getProvider().getId(), userDetail.getLoginId());

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

  private ApplicationDetail createAmendmentObject(String caseReferenceNumber,
      UserDetail userDetail) {

    ApplicationDetail amendment = applicationService.getCase(
        caseReferenceNumber,
        userDetail.getProvider().getId(), userDetail.getLoginId());

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

  public String submitQuickAmendmentCorrespondenceAddress(
      AddressFormData editCorrespondenceAddress,
      final String caseReferenceNumber,
      final UserDetail userDetail) {
    // Check CorrespondenceAddressController:110
    // Check QuickAmendPollingController::pollQuickAmend
    // Check QuickAmendPollingController::processAmendmentType
    // Check CcmsHelper::prepareAmendment
    // Check SubmitQuickAmendmentInterceptor
    ApplicationDetail amendment = createAmendmentObject(caseReferenceNumber, userDetail);
    amendment.setQuickEditType(FunctionConstants.CASE_CORRESPONDENCE_PREFERENCE);

    AddressDetail address = amendment.getCorrespondenceAddress();

    address.setAddressLine1(editCorrespondenceAddress.getAddressLine1());
    address.setAddressLine2(editCorrespondenceAddress.getAddressLine2());
    address.setCareOf(editCorrespondenceAddress.getCareOf());
    address.setCity(editCorrespondenceAddress.getCityTown());
    address.setCountry(editCorrespondenceAddress.getCountry());
    address.setCounty(editCorrespondenceAddress.getCounty());
    address.setHouseNameOrNumber(editCorrespondenceAddress.getHouseNameNumber());
    address.setPostcode(editCorrespondenceAddress.getPostcode());
    address.setPreferredAddress(editCorrespondenceAddress.getPreferredAddress());

    cleanAppForQuickAmendSubmit(amendment);

    // Submit application and return transaction ID

    // TODO: Return the transaction ID
    return "";
  }

  private void cleanAppForQuickAmendSubmit(ApplicationDetail app) {
    ArrayList<ProceedingDetail> noProceedings = new ArrayList<>();
    ArrayList<OpponentDetail> noOpponents = new ArrayList<>();
    app.setProceedings(noProceedings);
    app.setOpponents(noOpponents);
    // app.setApplicationType(null);
    app.setLarScopeFlag(null);
    if (app.getQuickEditType().equals(QuickEditTypeConstants.MESSAGE_TYPE_EDIT_PROVIDER)) {
      app.setCorrespondenceAddress(null);
      app.setCategoryOfLaw(null);
      app.setCosts(null);
    } else if (app.getQuickEditType()
        .equals(QuickEditTypeConstants.MESSAGE_TYPE_CASE_CORRESPONDENCE_PREFERENCE)) {
      // Commented out as it causes the provider reference to disappear when returning to the
      // view case screens
      // app.setProviderCaseReference(null);
      // TODO: Check if you can just set provider details to null
      app.getProviderDetails().setSupervisor(null);
      app.getProviderDetails().setFeeEarner(null);
      app.getProviderDetails().setProviderContact(null);
      app.setCategoryOfLaw(null);
      app.setCosts(null);
    } else if (app.getQuickEditType()
        .equals(QuickEditTypeConstants.MESSAGE_TYPE_ALLOCATE_COST_LIMIT)) {
      // Commented out as it causes the provider reference to disappear when returning to the
      // view case screens
      // app.setProviderCaseReference(null);
      // TODO: Check if you can just set provider details to null
      app.getProviderDetails().setSupervisor(null);
      app.getProviderDetails().setFeeEarner(null);
      app.getProviderDetails().setProviderContact(null);
      app.setCorrespondenceAddress(null);
    } else if (app.getQuickEditType()
        .equals(QuickEditTypeConstants.MESSAGE_TYPE_MEANS_REASSESSMENT)) {
      // Commented out as it causes the provider reference to disappear when returning to the
      // view case screens
      // app.setProviderCaseReference(null);
      // TODO: Check if you can just set provider details to null
      app.setMeansAssessmentAmended(true);
      app.setMeritsAssessmentAmended(false);
      app.getProviderDetails().setSupervisor(null);
      app.getProviderDetails().setFeeEarner(null);
      app.getProviderDetails().setProviderContact(null);
      app.setCorrespondenceAddress(null);
      app.setCosts(null);
      app.setCategoryOfLaw(null);
    }


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
   * @param application The application details for which the amendment sections need to be
   *                    fetched.
   * @param user The user details, providing context for retrieving and tailoring the sections.
   * @return An ApplicationSectionDisplay object containing the relevant sections for the amendment,
   * with document upload enabled based on specific conditions.
   */
  public ApplicationSectionDisplay getAmendmentSections(
      final ApplicationDetail application, final UserDetail user) {
    final ApplicationSectionDisplay sectionDisplay =
        applicationService.getApplicationSections(application, user);

    // Enable document link if either prior authority added or assessment completed
    boolean isPriorAuthorityAdded =
        sectionDisplay.getPriorAuthorities().stream()
            .anyMatch(x -> "Draft".equalsIgnoreCase(x.getStatus()));
    boolean assessmentComplete =
        application.getMeansAssessmentAmended() || application.getMeritsAssessmentAmended();
    sectionDisplay.getDocumentUpload().setEnabled(isPriorAuthorityAdded || assessmentComplete);

    return sectionDisplay;
  }
}
