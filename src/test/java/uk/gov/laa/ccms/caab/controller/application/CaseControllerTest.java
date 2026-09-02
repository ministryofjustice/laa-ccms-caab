package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COST_ALLOCATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_OUTCOME_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.CourtSearchCriteria;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingOutcomeValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualAddressContactDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualEmploymentDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualGeneralDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationAddressDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationOrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class CaseControllerTest {

  @Mock private ApplicationService applicationService;
  @Mock private LookupService lookupService;
  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private ProceedingOutcomeValidator proceedingOutcomeValidator;

  @InjectMocks private CaseController caseController;

  private MockMvcTester mockMvc;

  private UserDetail user;

  private String returnUrl;

  private static final String SEARCH_URL = "SEARCH_URL";

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(caseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setControllerAdvice(new ActiveCaseModelAdvice())
                .build());
    this.user = ApplicationTestUtils.buildUser();
    returnUrl = "returnUrl";
    lenient()
        .when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
        .thenReturn(java.util.Optional.empty());
  }

  @Nested
  @DisplayName("/case/overview tests")
  class CaseOverview {

    @Test
    @DisplayName("Case overview refetches case when missing from session")
    public void caseOverviewRefetchesCaseWhenMissingFromSession() {
      final String selectedCaseRef = "2";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";

      // EBS Case
      ApplicationDetail applicationDetail =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              false,
              null,
              null,
              List.of(FunctionConstants.AMEND_CASE));

      when(applicationService.getCase(
              selectedCaseRef, user.getProvider().getId(), user.getLoginId()))
          .thenReturn(applicationDetail);

      assertThat(
              mockMvc.perform(
                  get("/case/overview")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE_REFERENCE_NUMBER, selectedCaseRef)))
          .hasViewName("application/case-overview")
          .satisfies(
              response -> {
                assertThat(response)
                    .request()
                    .sessionAttributes()
                    .hasEntrySatisfying(
                        CASE, value -> assertThat(value).isEqualTo(applicationDetail));
              });

      verify(applicationService)
          .getCase(selectedCaseRef, user.getProvider().getId(), user.getLoginId());
    }

    @Test
    @DisplayName("Case overview screen loads case details")
    public void caseOverviewLoadsCaseDetails() {
      final String selectedCaseRef = "2";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";

      // EBS Case
      ApplicationDetail applicationDetail =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              false,
              null,
              null,
              List.of(FunctionConstants.AMEND_CASE));

      final ActiveCase activeCase =
          getActiveCase(
              selectedCaseRef,
              providerId,
              clientFirstname,
              clientSurname,
              clientReference,
              providerReference);

      assertThat(
              mockMvc.perform(
                  get("/case/overview", selectedCaseRef)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, applicationDetail)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasViewName("application/case-overview")
          .satisfies(
              response -> {
                assertThat(response)
                    .request()
                    .sessionAttributes()
                    .hasEntrySatisfying(
                        CASE, value -> assertThat(value).isEqualTo(applicationDetail))
                    .hasEntrySatisfying(
                        ACTIVE_CASE, value -> assertThat(value).isEqualTo(activeCase));
                assertThat(response)
                    .model()
                    .hasEntrySatisfying(
                        "hasEbsAmendments",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                                .isFalse())
                    .hasEntrySatisfying(
                        "draftProceedings",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .isEmpty())
                    .hasEntrySatisfying("draftCosts", value -> assertThat(value).isNull())
                    .hasEntrySatisfying(
                        "availableActions",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .hasSize(1))
                    .hasEntrySatisfying(
                        "returnTo", value -> assertThat(value).isEqualTo("caseSearchResults"))
                    .hasEntrySatisfying(NOTIFICATION_ID, value -> assertThat(value).isNull());
              });
    }

    @Test
    @DisplayName("Outcome and awards treats legacy local clear marker as cleared and not clearable")
    public void outcomeAndAwardsIgnoresLegacyClearMarker() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcome =
          new ProceedingOutcomeDetail()
              .proceedingCaseId("pc1")
              .result(new StringDisplayValue().id("R1").displayValue("EBS outcome"));
      final ProceedingOutcomeDetail legacyMarker =
          new ProceedingOutcomeDetail()
              .id(99)
              .proceedingCaseId("pc1")
              .description("Proceeding 1")
              .proceedingType(new StringDisplayValue().id("P1").displayValue("Type 1"))
              .stageEnd(new StringDisplayValue().id("").displayValue(""))
              .result(new StringDisplayValue().id("").displayValue(""))
              .resolutionMethod("")
              .resultInfo("")
              .alternativeResolution("")
              .adrInfo("")
              .courtCode("")
              .courtName("")
              .outcomeCourtCaseNo("")
              .widerBenefits("");
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(new ProceedingDetail().proceedingCaseId("pc1").outcome(ebsOutcome)));

      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(List.of(legacyMarker))));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/outcome-and-awards")
          .model()
          .hasEntrySatisfying(
              "resolvedOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> resolved =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(resolved.get("pc1")).isNull();
              })
          .hasEntrySatisfying(
              "clearableOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> clearable =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(clearable).containsKey("pc1");
                assertThat(clearable.get("pc1")).isNull();
              });
    }

    @Test
    @DisplayName("Case overview clears cost allocation flow data")
    public void caseOverviewClearsCostAllocationFlowData() {
      final String selectedCaseRef = "2";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";

      ApplicationDetail applicationDetail =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              false,
              null,
              null);

      AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();

      assertThat(
              mockMvc.perform(
                  get("/case/overview", selectedCaseRef)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, applicationDetail)
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, allocateCostsFormData)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasViewName("application/case-overview")
          .satisfies(
              response ->
                  assertThat(response)
                      .request()
                      .sessionAttributes()
                      .doesNotContainKey(COST_ALLOCATION_FORM_DATA));
    }

    @Test
    @DisplayName("Case overview screen correctly sets return link")
    public void caseOverviewSetsReturnTo() {
      final String selectedCaseRef = "2";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";
      final String notificationId = "5";

      // EBS Case
      ApplicationDetail applicationDetail =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              false,
              null,
              null);

      final ActiveCase activeCase =
          getActiveCase(
              selectedCaseRef,
              providerId,
              clientFirstname,
              clientSurname,
              clientReference,
              providerReference);

      assertThat(
              mockMvc.perform(
                  get("/case/overview", selectedCaseRef)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, applicationDetail)
                      .sessionAttr(NOTIFICATION_ID, notificationId)
                      .sessionAttr(SEARCH_URL, returnUrl)
                      .header("referer", "/notifications/%s".formatted(notificationId))))
          .hasViewName("application/case-overview")
          .satisfies(
              response -> {
                assertThat(response)
                    .request()
                    .sessionAttributes()
                    .hasEntrySatisfying(
                        CASE, value -> assertThat(value).isEqualTo(applicationDetail))
                    .hasEntrySatisfying(
                        ACTIVE_CASE, value -> assertThat(value).isEqualTo(activeCase));
                assertThat(response)
                    .model()
                    .hasEntrySatisfying(
                        "hasEbsAmendments",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                                .isFalse())
                    .hasEntrySatisfying(
                        "draftProceedings",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .isEmpty())
                    .hasEntrySatisfying("draftCosts", value -> assertThat(value).isNull())
                    .hasEntrySatisfying(
                        "availableActions",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .hasSize(1))
                    .hasEntrySatisfying(
                        "returnTo", value -> assertThat(value).isEqualTo("notification"))
                    .hasEntrySatisfying(
                        NOTIFICATION_ID, value -> assertThat(value).isEqualTo(notificationId));
              });
    }

    @Test
    @DisplayName("Record proceeding outcome treats legacy local clear marker as cleared")
    public void recordProceedingOutcomeUsesEbsWhenLegacyMarkerExists() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcome =
          new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("EBS result info");
      final ProceedingOutcomeDetail legacyMarker =
          new ProceedingOutcomeDetail()
              .id(99)
              .proceedingCaseId("pc1")
              .description("Proceeding 1")
              .proceedingType(new StringDisplayValue().id("P1").displayValue("Type 1"));
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(
              new ProceedingDetail()
                  .proceedingCaseId("pc1")
                  .description("Proceeding 1")
                  .proceedingType(new StringDisplayValue().id("P1").displayValue("Proceeding name"))
                  .outcome(ebsOutcome)));

      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(List.of(legacyMarker))));
      when(lookupService.getStageEnds("P1", null))
          .thenReturn(Mono.just(new StageEndLookupDetail()));
      when(lookupService.getOutcomeResults("P1", null))
          .thenReturn(Mono.just(new OutcomeResultLookupDetail()));
      when(lookupService.getCommonValues(anyString()))
          .thenReturn(Mono.just(new CommonLookupDetail()));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/record-proceeding-outcome")
          .model()
          .hasEntrySatisfying(
              "proceedingOutcome",
              value -> {
                ProceedingOutcomeFormData formData = (ProceedingOutcomeFormData) value;
                assertThat(formData.getResultInfo()).isNull();
              });
    }

    @Test
    @DisplayName("Case overview screen sets amendments details from EBS case")
    public void caseOverviewSetsEbsCaseAmendments() {
      final String selectedCaseRef = "2";
      final String appRef = "3";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";
      final boolean hasEbsAmendments = true;
      final Integer proceedingId = 2;
      final String costId = "4";

      // EBS Case
      ApplicationDetail applicationDetail =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              hasEbsAmendments,
              proceedingId,
              costId);

      final ActiveCase activeCase =
          getActiveCase(
              selectedCaseRef,
              providerId,
              clientFirstname,
              clientSurname,
              clientReference,
              providerReference);

      // TDS application
      BaseApplicationDetail tdsApplication =
          new BaseApplicationDetail()
              .id(Integer.parseInt(appRef))
              .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
              .caseReferenceNumber(selectedCaseRef);

      when(applicationService.getApplication(any())).thenReturn(Mono.empty());
      when(applicationService.isAmendment(any(), any())).thenReturn(Boolean.TRUE);

      ProceedingDetail expectedProceeding = new ProceedingDetail().id(proceedingId);
      CostStructureDetail expectedCost =
          new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId));

      assertThat(
              mockMvc.perform(
                  get("/case/overview", selectedCaseRef)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, applicationDetail)
                      .sessionAttr(APPLICATION_SUMMARY, tdsApplication)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasViewName("application/case-overview")
          .satisfies(
              response -> {
                assertThat(response)
                    .request()
                    .sessionAttributes()
                    .hasEntrySatisfying(
                        CASE, value -> assertThat(value).isEqualTo(applicationDetail))
                    .hasEntrySatisfying(
                        ACTIVE_CASE, value -> assertThat(value).isEqualTo(activeCase));
                assertThat(response)
                    .model()
                    .hasEntrySatisfying(
                        "hasEbsAmendments",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                                .isEqualTo(hasEbsAmendments))
                    .hasEntrySatisfying(
                        "draftProceedings",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .hasSize(1)
                                .contains(expectedProceeding))
                    .hasEntrySatisfying(
                        "draftCosts", value -> assertThat(value).isEqualTo(expectedCost))
                    .hasEntrySatisfying(
                        "availableActions",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .hasSize(1))
                    .hasEntrySatisfying(
                        "returnTo", value -> assertThat(value).isEqualTo("caseSearchResults"))
                    .hasEntrySatisfying(NOTIFICATION_ID, value -> assertThat(value).isNull());
              });
    }

    @Test
    @DisplayName("Case overview screen sets amendments details from TDS")
    public void caseOverviewSetsTdsAmendments() {
      final String selectedCaseRef = "2";
      final String appRef = "3";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";
      final boolean hasEbsAmendments = false;
      final Integer proceedingId = 2;
      final String costId = "4";

      // EBS Case
      ApplicationDetail applicationDetail =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              hasEbsAmendments,
              null,
              null);

      final ActiveCase activeCase =
          getActiveCase(
              selectedCaseRef,
              providerId,
              clientFirstname,
              clientSurname,
              clientReference,
              providerReference);

      // TDS application
      BaseApplicationDetail tdsApplication =
          new BaseApplicationDetail()
              .id(Integer.parseInt(appRef))
              .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
              .caseReferenceNumber(selectedCaseRef);

      ProceedingDetail expectedProceeding = new ProceedingDetail().id(proceedingId);
      CostStructureDetail expectedCost =
          new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId));

      ApplicationDetail amendments =
          new ApplicationDetail().proceedings(List.of(expectedProceeding)).costs(expectedCost);

      when(applicationService.getApplication(any())).thenReturn(Mono.just(amendments));
      when(applicationService.isAmendment(any(), any())).thenReturn(Boolean.TRUE);

      assertThat(
              mockMvc.perform(
                  get("/case/overview", selectedCaseRef)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, applicationDetail)
                      .sessionAttr(APPLICATION_SUMMARY, tdsApplication)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasViewName("application/case-overview")
          .satisfies(
              response -> {
                assertThat(response)
                    .request()
                    .sessionAttributes()
                    .hasEntrySatisfying(
                        CASE, value -> assertThat(value).isEqualTo(applicationDetail))
                    .hasEntrySatisfying(
                        ACTIVE_CASE, value -> assertThat(value).isEqualTo(activeCase));
                assertThat(response)
                    .model()
                    .hasEntrySatisfying(
                        "hasEbsAmendments",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                                .isEqualTo(hasEbsAmendments))
                    .hasEntrySatisfying(
                        "draftProceedings",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .hasSize(1)
                                .contains(expectedProceeding))
                    .hasEntrySatisfying(
                        "draftCosts", value -> assertThat(value).isEqualTo(expectedCost))
                    .hasEntrySatisfying(
                        "availableActions",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                                .hasSize(1))
                    .hasEntrySatisfying(
                        "returnTo", value -> assertThat(value).isEqualTo("caseSearchResults"))
                    .hasEntrySatisfying(NOTIFICATION_ID, value -> assertThat(value).isNull());
              });
    }

    @Test
    @DisplayName("Case overview refreshes TDS amendment summary when missing from session")
    public void caseOverviewRefreshesTdsSummaryWhenMissing() {
      final String selectedCaseRef = "7";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";

      ApplicationDetail ebsCase =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              false,
              null,
              null,
              List.of(FunctionConstants.AMEND_CASE));

      BaseApplicationDetail tdsApplication =
          new BaseApplicationDetail()
              .id(100)
              .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
              .caseReferenceNumber(selectedCaseRef);

      ProceedingDetail expectedProceeding = new ProceedingDetail().id(2);
      CostStructureDetail expectedCost =
          new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId("4"));

      ApplicationDetail amendments =
          new ApplicationDetail().proceedings(List.of(expectedProceeding)).costs(expectedCost);

      when(applicationService.getTdsApplicationSummary(any(), any())).thenReturn(tdsApplication);
      when(applicationService.getApplication(any())).thenReturn(Mono.just(amendments));
      when(applicationService.isAmendment(any(), any())).thenReturn(Boolean.TRUE);

      assertThat(
              mockMvc.perform(
                  get("/case/overview", selectedCaseRef)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasViewName("application/case-overview")
          .satisfies(
              response -> {
                assertThat(response)
                    .request()
                    .sessionAttributes()
                    .hasEntrySatisfying(
                        APPLICATION_SUMMARY, value -> assertThat(value).isEqualTo(tdsApplication));
                assertThat(response)
                    .model()
                    .hasEntrySatisfying(
                        "isAmendment",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                                .isTrue());
              });
    }

    @Test
    @DisplayName("Case overview clears stale TDS amendment when not found")
    public void caseOverviewClearsStaleTdsAmendment() {
      final String selectedCaseRef = "2";
      final Integer providerId = 1;
      final String providerReference = "providerReference";
      final String clientFirstname = "firstname";
      final String clientSurname = "surname";
      final String clientReference = "clientReference";
      final String applicationId = "applicationId";
      final ApplicationDetail amendmentApplication = new ApplicationDetail().id(99);
      final CostStructureDetail amendmentCosts = new CostStructureDetail();
      final String applicationFormData = "applicationFormData";

      ApplicationDetail applicationDetail =
          getEbsCase(
              selectedCaseRef,
              providerId,
              providerReference,
              clientFirstname,
              clientSurname,
              clientReference,
              false,
              null,
              null);

      BaseApplicationDetail tdsApplication =
          new BaseApplicationDetail().id(3).caseReferenceNumber(selectedCaseRef);

      when(applicationService.getApplication(any()))
          .thenReturn(Mono.error(new CaabApiClientException("not found", HttpStatus.NOT_FOUND)));
      when(applicationService.isAmendment(any(), any())).thenReturn(Boolean.TRUE);

      assertThat(
              mockMvc.perform(
                  get("/case/overview", selectedCaseRef)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, applicationDetail)
                      .sessionAttr(APPLICATION_SUMMARY, tdsApplication)
                      .sessionAttr(APPLICATION_ID, applicationId)
                      .sessionAttr(APPLICATION, amendmentApplication)
                      .sessionAttr(APPLICATION_COSTS, amendmentCosts)
                      .sessionAttr(APPLICATION_FORM_DATA, applicationFormData)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasViewName("application/case-overview")
          .satisfies(
              response -> {
                assertThat(response)
                    .request()
                    .sessionAttributes()
                    .doesNotContainKey(APPLICATION_SUMMARY)
                    .doesNotContainKey(APPLICATION_ID)
                    .doesNotContainKey(APPLICATION)
                    .doesNotContainKey(APPLICATION_COSTS)
                    .doesNotContainKey(APPLICATION_FORM_DATA);
                assertThat(response)
                    .model()
                    .hasEntrySatisfying(
                        "isAmendment",
                        value ->
                            assertThat(value)
                                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                                .isFalse());
              });
    }

    @Test
    @DisplayName("Case overview screen shows no available actions when ebsCase has no functions")
    public void caseOverviewNoAvailableFunctionsShowsNoActions() {
      final String selectedCaseRef = "3";
      ApplicationDetail ebsCase =
          getEbsCase(
              selectedCaseRef,
              1,
              "ref",
              "client",
              "smith",
              "clientRef",
              false,
              null,
              null,
              Collections.emptyList());

      assertThat(
              mockMvc.perform(
                  get("/case/overview")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "availableActions",
              value ->
                  assertThat(value).asInstanceOf(InstanceOfAssertFactories.COLLECTION).isEmpty());
    }

    @Test
    @DisplayName("Outcome and awards page loads")
    public void outcomeAndAwardsPageLoads() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/outcome-and-awards");
    }

    @Test
    @DisplayName("Outcome and awards retains EBS proceeding outcome when no local save exists")
    public void outcomeAndAwardsRetainsEbsOutcomeWhenNoLocalSave() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcome =
          new ProceedingOutcomeDetail()
              .proceedingCaseId("pc1")
              .result(new StringDisplayValue().id("R1").displayValue("EBS outcome"));
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(new ProceedingDetail().proceedingCaseId("pc1").outcome(ebsOutcome)));

      // No CAAB record at all means outcomes have never been managed for this case → fall back to
      // EBS.
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(java.util.Optional.empty());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/outcome-and-awards")
          .model()
          .hasEntrySatisfying(
              "resolvedOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> resolved =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(resolved).containsKey("pc1");
                assertThat(resolved.get("pc1").getResult().getId()).isEqualTo("R1");
              })
          .hasEntrySatisfying(
              "proceedings",
              value -> {
                // Session object is NOT mutated — original EBS outcome is preserved
                @SuppressWarnings("unchecked")
                List<ProceedingDetail> proceedings = (List<ProceedingDetail>) value;
                assertThat(proceedings).hasSize(1);
                assertThat(proceedings.get(0).getOutcome()).isSameAs(ebsOutcome);
              });
    }

    @Test
    @DisplayName(
        "Outcome and awards shows EBS outcome but no clearable local outcome when EBS only")
    public void outcomeAndAwardsEbsOnlyIsNotClearable() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcome =
          new ProceedingOutcomeDetail()
              .proceedingCaseId("pc1")
              .result(new StringDisplayValue().id("R1").displayValue("EBS outcome"));
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(new ProceedingDetail().proceedingCaseId("pc1").outcome(ebsOutcome)));

      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(java.util.Optional.empty());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "resolvedOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> resolved =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(resolved.get("pc1")).isSameAs(ebsOutcome);
              })
          .hasEntrySatisfying(
              "clearableOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> clearable =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(clearable.get("pc1")).isNull();
              });
    }

    @Test
    @DisplayName(
        "Outcome and awards falls back to EBS outcome when local case outcome exists without this proceeding")
    public void outcomeAndAwardsClearedOutcomeNotRestoredFromEbs() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcome =
          new ProceedingOutcomeDetail()
              .proceedingCaseId("pc1")
              .result(new StringDisplayValue().id("R1").displayValue("EBS outcome"));
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(new ProceedingDetail().proceedingCaseId("pc1").outcome(ebsOutcome)));

      // A local case outcome exists, but there is no local record for this proceeding, so this
      // proceeding should still fall back to EBS.
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(Collections.emptyList())));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/outcome-and-awards")
          .model()
          .hasEntrySatisfying(
              "resolvedOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> resolved =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(resolved).containsKey("pc1");
                assertThat(resolved.get("pc1").getResult().getId()).isEqualTo("R1");
              });
    }

    @Test
    @DisplayName("Record proceeding outcome page loads for selected proceeding")
    public void recordProceedingOutcomePageLoads() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(
              new ProceedingDetail()
                  .description("Proceeding 1")
                  .proceedingType(
                      new StringDisplayValue().id("P1").displayValue("Proceeding name"))));

      when(lookupService.getStageEnds("P1", null))
          .thenReturn(
              Mono.just(
                  new StageEndLookupDetail()
                      .addContentItem(
                          new StageEndLookupValueDetail()
                              .stageEnd("SE1")
                              .description("Stage End 1"))));
      when(lookupService.getOutcomeResults("P1", null))
          .thenReturn(
              Mono.just(
                  new OutcomeResultLookupDetail()
                      .addContentItem(
                          new OutcomeResultLookupValueDetail()
                              .outcomeResult("R1")
                              .outcomeResultDescription("Result 1"))));
      lenient()
          .when(lookupService.getCourts(anyString()))
          .thenReturn(
              Mono.just(
                  new CommonLookupDetail()
                      .addContentItem(
                          new CommonLookupValueDetail().code("CT1").description("Court 1"))));
      when(lookupService.getCommonValues(anyString()))
          .thenReturn(
              Mono.just(
                  new CommonLookupDetail()
                      .addContentItem(
                          new CommonLookupValueDetail()
                              .code("C1")
                              .description("Common option 1"))));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/record-proceeding-outcome");
    }

    @Test
    @DisplayName("Record proceeding outcome falls back to EBS outcome when no local save exists")
    public void recordProceedingOutcomeUsesEbsOutcomeWhenNoLocalSave() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcome =
          new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("EBS result info");
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(
              new ProceedingDetail()
                  .proceedingCaseId("pc1")
                  .description("Proceeding 1")
                  .proceedingType(new StringDisplayValue().id("P1").displayValue("Proceeding name"))
                  .outcome(ebsOutcome)));

      // No local save — no CAAB case outcome record exists.
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(java.util.Optional.empty());
      when(lookupService.getStageEnds("P1", null))
          .thenReturn(Mono.just(new StageEndLookupDetail()));
      when(lookupService.getOutcomeResults("P1", null))
          .thenReturn(Mono.just(new OutcomeResultLookupDetail()));
      when(lookupService.getCommonValues(anyString()))
          .thenReturn(Mono.just(new CommonLookupDetail()));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/record-proceeding-outcome")
          .model()
          .hasEntrySatisfying(
              "proceedingOutcome",
              value -> {
                ProceedingOutcomeFormData formData = (ProceedingOutcomeFormData) value;
                assertThat(formData.getResultInfo()).isEqualTo("EBS result info");
              });
    }

    @Test
    @DisplayName(
        "Record proceeding outcome falls back to EBS outcome when local case outcome has no matching proceeding")
    public void recordProceedingOutcomeShowsBlankWhenProceedingClearedInCaab() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcome =
          new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("EBS result info");
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(
              new ProceedingDetail()
                  .proceedingCaseId("pc1")
                  .description("Proceeding 1")
                  .proceedingType(new StringDisplayValue().id("P1").displayValue("Proceeding name"))
                  .outcome(ebsOutcome)));

      // A local case outcome exists but there is no matching local proceeding outcome, so this
      // proceeding should still use EBS.
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(Collections.emptyList())));
      when(lookupService.getStageEnds("P1", null))
          .thenReturn(Mono.just(new StageEndLookupDetail()));
      when(lookupService.getOutcomeResults("P1", null))
          .thenReturn(Mono.just(new OutcomeResultLookupDetail()));
      when(lookupService.getCommonValues(anyString()))
          .thenReturn(Mono.just(new CommonLookupDetail()));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/record-proceeding-outcome")
          .model()
          .hasEntrySatisfying(
              "proceedingOutcome",
              value -> {
                ProceedingOutcomeFormData formData = (ProceedingOutcomeFormData) value;
                assertThat(formData.getResultInfo()).isEqualTo("EBS result info");
              });
    }

    @Test
    @DisplayName(
        "Outcome and awards falls back to EBS for untouched proceeding when other local outcome exists")
    public void outcomeAndAwardsFallsBackToEbsForUntouchedProceedingWhenLocalRecordExists() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail ebsOutcomePc1 =
          new ProceedingOutcomeDetail()
              .proceedingCaseId("pc1")
              .result(new StringDisplayValue().id("R1").displayValue("EBS outcome 1"));
      final ProceedingOutcomeDetail localOutcomePc2 =
          new ProceedingOutcomeDetail()
              .id(200)
              .proceedingCaseId("pc2")
              .result(new StringDisplayValue().id("R2").displayValue("Local outcome 2"));
      final ProceedingOutcomeDetail ebsOutcomePc2 =
          new ProceedingOutcomeDetail()
              .proceedingCaseId("pc2")
              .result(new StringDisplayValue().id("R3").displayValue("EBS outcome 2"));

      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(
              new ProceedingDetail().proceedingCaseId("pc1").outcome(ebsOutcomePc1),
              new ProceedingDetail().proceedingCaseId("pc2").outcome(ebsOutcomePc2)));

      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(List.of(localOutcomePc2))));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/outcome-and-awards")
          .model()
          .hasEntrySatisfying(
              "resolvedOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> resolved =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(resolved.get("pc1")).isSameAs(ebsOutcomePc1);
                assertThat(resolved.get("pc2")).isSameAs(localOutcomePc2);
              })
          .hasEntrySatisfying(
              "clearableOutcomes",
              value -> {
                @SuppressWarnings("unchecked")
                Map<String, ProceedingOutcomeDetail> clearable =
                    (Map<String, ProceedingOutcomeDetail>) value;
                assertThat(clearable.get("pc1")).isNull();
                assertThat(clearable.get("pc2")).isSameAs(localOutcomePc2);
              });
    }

    @Test
    @DisplayName("Record proceeding outcome post redirects to outcome and awards")
    public void recordProceedingOutcomePostRedirectsToOutcomeAndAwards() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(
              new ProceedingDetail()
                  .id(77)
                  .proceedingCaseId("pc1")
                  .proceedingType(
                      new StringDisplayValue().id("P1").displayValue("Proceeding name"))));

      doNothing().when(proceedingOutcomeValidator).validate(any(), any());
      when(lookupService.getStageEnds("P1", null))
          .thenReturn(
              Mono.just(
                  new StageEndLookupDetail()
                      .addContentItem(
                          new StageEndLookupValueDetail()
                              .stageEnd("SE1")
                              .description("Stage End 1"))));
      when(lookupService.getOutcomeResults("P1", null))
          .thenReturn(
              Mono.just(
                  new OutcomeResultLookupDetail()
                      .addContentItem(
                          new OutcomeResultLookupValueDetail()
                              .outcomeResult("R1")
                              .outcomeResultDescription("Result 1"))));
      lenient()
          .when(lookupService.getCourts(anyString()))
          .thenReturn(
              Mono.just(
                  new CommonLookupDetail()
                      .addContentItem(
                          new CommonLookupValueDetail().code("CT1").description("Court 1"))));
      doNothing()
          .when(caseOutcomeService)
          .updateProceedingOutcome(anyString(), anyInt(), any(), anyString());

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/proceeding/0/outcome")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)
                      .param("dateOfFinalWork", "01/01/2026")
                      .param("stageEnd", "SE1")
                      .param("resolutionMethod", "RM1")
                      .param("result", "R1")
                      .param("resultInfo", "Result info")
                      .param("alternativeResolution", "ADR1")
                      .param("adrInfo", "ADR info")
                      .param("courtCode", "CT1")
                      .param("outcomeCourtCaseNo", "123")
                      .param("widerBenefits", "WB1")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    @DisplayName(
        "Clear proceeding outcome confirmation redirects when only legacy local clear marker exists")
    public void clearProceedingOutcomePageRedirectsWhenLegacyMarkerExists() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail legacyMarker =
          new ProceedingOutcomeDetail()
              .id(99)
              .proceedingCaseId("pc1")
              .description("Proceeding 1")
              .proceedingType(new StringDisplayValue().id("P1").displayValue("Type 1"));
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of(FunctionConstants.CLEAR_RECORDED_OUTCOME))
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(List.of(legacyMarker))));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    @DisplayName("Clear proceeding outcome confirmation page loads for allowed proceeding")
    public void clearProceedingOutcomePageLoadsForAllowedProceeding() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail localOutcome =
          new ProceedingOutcomeDetail()
              .id(99)
              .proceedingCaseId("pc1")
              .result(new StringDisplayValue().id("R1").displayValue("Local outcome"));
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of(FunctionConstants.CLEAR_RECORDED_OUTCOME))
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(List.of(localOutcome))));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/clear-proceeding-outcome")
          .model()
          .containsEntry("proceeding", proceeding)
          .containsEntry("proceedingIndex", 0)
          .containsEntry("resolvedOutcome", localOutcome);
    }

    @Test
    @DisplayName("Clear proceeding outcome confirmation redirects when action not allowed")
    public void clearProceedingOutcomePageRedirectsWhenActionNotAllowed() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of())
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    @DisplayName("Clear proceeding outcome confirmation rejects negative proceeding index")
    public void clearProceedingOutcomePageRejectsNegativeIndex() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(List.of(new ProceedingDetail().proceedingCaseId("pc1")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/-1/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .failure()
          .hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Could not find proceeding with index: -1");
    }

    @Test
    @DisplayName(
        "Clear proceeding outcome confirmation redirects when only EBS outcome exists and no local persisted outcome")
    public void clearProceedingOutcomePageRedirectsWhenNoLocalPersistedOutcome() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of(FunctionConstants.CLEAR_RECORDED_OUTCOME))
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(Collections.emptyList())));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    @DisplayName("Clear proceeding outcome confirm clears data and redirects")
    public void clearProceedingOutcomeConfirmClearsDataAndRedirects() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail localOutcome =
          new ProceedingOutcomeDetail().id(99).proceedingCaseId("pc1").resultInfo("Local outcome");
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of(FunctionConstants.CLEAR_RECORDED_OUTCOME))
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(List.of(localOutcome))));

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");

      verify(caseOutcomeService)
          .clearProceedingOutcome(eq("8"), eq(123), eq("pc1"), eq(user.getLoginId()));
    }

    @Test
    @DisplayName(
        "Clear proceeding outcome confirm redirects without clearing when action not allowed")
    public void clearProceedingOutcomeConfirmRedirectsWhenActionNotAllowed() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of())
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");

      verify(caseOutcomeService, times(0))
          .clearProceedingOutcome(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName(
        "Clear proceeding outcome confirm redirects without clearing when only EBS outcome exists")
    public void clearProceedingOutcomeConfirmRedirectsWhenNoLocalPersistedOutcome() {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of(FunctionConstants.CLEAR_RECORDED_OUTCOME))
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(Collections.emptyList())));

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");

      verify(caseOutcomeService, times(0))
          .clearProceedingOutcome(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("Clear proceeding outcome confirm wraps API failures in CaabApplicationException")
    public void clearProceedingOutcomeConfirmThrowsCaabApplicationExceptionWhenServiceFails() {
      final String selectedCaseRef = "8";
      final ProceedingOutcomeDetail localOutcome =
          new ProceedingOutcomeDetail().id(99).proceedingCaseId("pc1").resultInfo("Local outcome");
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ProceedingDetail proceeding =
          new ProceedingDetail()
              .description("1854553")
              .proceedingCaseId("pc1")
              .availableFunctions(List.of(FunctionConstants.CLEAR_RECORDED_OUTCOME))
              .outcome(new ProceedingOutcomeDetail().id(99));
      ebsCase.setProceedings(List.of(proceeding));
      when(caseOutcomeService.getCaseOutcome(anyString(), anyInt()))
          .thenReturn(
              java.util.Optional.of(
                  new CaseOutcomeDetail().proceedingOutcomes(List.of(localOutcome))));

      doThrow(new CaabApiClientException("Downstream failure"))
          .when(caseOutcomeService)
          .clearProceedingOutcome(eq("8"), eq(123), eq("pc1"), eq(user.getLoginId()));

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/proceeding/0/outcome/clear")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .failure()
          .hasCauseInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Failed to clear proceeding outcome.");
    }

    @Test
    @DisplayName(
        "Record proceeding outcome court search prepopulates court search criteria from form data")
    public void recordProceedingOutcomeCourtSearchPrepopulatesCourtSearchCriteria()
        throws Exception {
      final String selectedCaseRef = "8";
      ApplicationDetail ebsCase =
          getEbsCase(selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null);
      ebsCase.setProceedings(
          List.of(
              new ProceedingDetail()
                  .id(77)
                  .proceedingCaseId("pc1")
                  .proceedingType(
                      new StringDisplayValue().id("P1").displayValue("Proceeding name"))));

      var result =
          mockMvc.perform(
              post("/case/outcome-and-awards/proceeding/0/outcome/court-search")
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(CASE, ebsCase)
                  .param("dateOfFinalWork", "01/01/2026")
                  .param("courtCode", "CT1")
                  .param("courtName", "Test Court"));

      assertThat(result)
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/court/search?proceedingIndex=0");

      ProceedingOutcomeFormData savedFormData =
          (ProceedingOutcomeFormData)
              result.getRequest().getSession().getAttribute(PROCEEDING_OUTCOME_FORM_DATA);
      assertThat(savedFormData)
          .hasFieldOrPropertyWithValue("courtCode", "CT1")
          .hasFieldOrPropertyWithValue("courtName", "Test Court");

      CourtSearchCriteria courtSearchCriteria =
          (CourtSearchCriteria)
              result.getRequest().getSession().getAttribute(COURT_SEARCH_CRITERIA);
      assertThat(courtSearchCriteria)
          .hasFieldOrPropertyWithValue("courtCode", "CT1")
          .hasFieldOrPropertyWithValue("courtName", "Test Court");
    }

    @Test
    @DisplayName(
        "Case overview screen shows 'Continue Amendment' when AMEND_CASE is available and it's a TDS amendment")
    public void caseOverviewAmendCaseIsTdsAmendmentShowsContinueAmendment() {
      final String selectedCaseRef = "4";
      ApplicationDetail ebsCase =
          getEbsCase(
              selectedCaseRef,
              1,
              "ref",
              "client",
              "smith",
              "clientRef",
              false,
              null,
              null,
              List.of(FunctionConstants.AMEND_CASE));
      BaseApplicationDetail tdsApplication =
          new BaseApplicationDetail().id(100); // Indicates an amendment

      ProceedingDetail expectedProceeding = new ProceedingDetail().id(2);
      CostStructureDetail expectedCost =
          new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId("4"));

      ApplicationDetail amendments =
          new ApplicationDetail().proceedings(List.of(expectedProceeding)).costs(expectedCost);

      when(applicationService.getApplication(any())).thenReturn(Mono.just(amendments));
      when(applicationService.isAmendment(any(), any())).thenReturn(Boolean.TRUE);

      assertThat(
              mockMvc.perform(
                  get("/case/overview")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION_SUMMARY, tdsApplication)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "availableActions",
              value ->
                  assertThat(value).asInstanceOf(InstanceOfAssertFactories.COLLECTION).hasSize(1));
    }

    @Test
    @DisplayName(
        "Case overview screen shows 'Continue Amendment' when AMEND_CASE is available and there are EBS amendments")
    public void caseOverviewAmendCaseHasEbsAmendmentsShowsContinueAmendment() throws Exception {
      final String selectedCaseRef = "5";
      ApplicationDetail ebsCase =
          getEbsCase(
              selectedCaseRef,
              1,
              "ref",
              "client",
              "smith",
              "clientRef",
              true,
              1,
              "cost1",
              List.of(FunctionConstants.AMEND_CASE)); // hasEbsAmendments = true

      assertThat(
              mockMvc.perform(
                  get("/case/overview")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "availableActions",
              value ->
                  assertThat(value).asInstanceOf(InstanceOfAssertFactories.COLLECTION).hasSize(1));
    }

    @Test
    @DisplayName(
        "Case overview screen filters available actions based on predefined list and ebsCase functions")
    public void caseOverviewFiltersAvailableActions() throws Exception {
      final String selectedCaseRef = "6";
      ApplicationDetail ebsCase =
          getEbsCase(
              selectedCaseRef,
              1,
              "ref",
              "client",
              "smith",
              "clientRef",
              false,
              null,
              null,
              List.of(FunctionConstants.AMEND_CASE, FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  get("/case/overview")
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(SEARCH_URL, returnUrl)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "availableActions",
              value ->
                  assertThat(value).asInstanceOf(InstanceOfAssertFactories.COLLECTION).hasSize(2));
    }
  }

  @Nested
  @DisplayName("GET: /case/details")
  class CaseDetails {

    @Test
    @DisplayName("Should return view and model when case details exist")
    void caseDetailsReturnsViewAndModelWhenCaseDetailsExist() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ApplicationSectionDisplay display = ApplicationSectionDisplay.builder().build();
      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(display);

      assertThat(mockMvc.perform(get("/case/details").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-details")
          .model()
          .containsEntry("summary", display);
    }

    @Test
    @DisplayName("Should return view and model for case cost details")
    void caseCostDetailsReturnsViewAndModel() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ApplicationSectionDisplay display = ApplicationSectionDisplay.builder().build();

      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(display);

      assertThat(mockMvc.perform(get("/case/details/costs").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-cost-details")
          .model()
          .containsEntry("summary", display);
    }

    @Test
    @DisplayName("Should throw exception when case details missing for case cost details")
    void caseCostDetailsThrowsExceptionWhenCaseDetailsMissing() {
      ApplicationDetail ebsCase = new ApplicationDetail();

      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(null);

      assertThat(mockMvc.perform(get("/case/details/costs").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Failed to retrieve case details");
    }

    @Test
    @DisplayName("Should throw exception when case details missing")
    void caseDetailsThrowsExceptionWhenCaseDetailsMissing() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(null);

      assertThat(mockMvc.perform(get("/case/details").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Failed to retrieve case details");
    }
  }

  @Nested
  @DisplayName("GET: /case/details/other-party/{index}")
  class CaseDetailsOtherParty {

    @Test
    @DisplayName("Should return view and model when case details exist")
    void caseDetailsOtherPartyReturnsViewAndModelWhenCaseDetailsExist() {
      OpponentDetail opponent = new OpponentDetail();
      opponent.setType("Individual");
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(Collections.singletonList(opponent));
      IndividualDetailsSectionDisplay otherParty =
          new IndividualDetailsSectionDisplay(
              new IndividualGeneralDetailsSectionDisplay(),
              new IndividualAddressContactDetailsSectionDisplay(),
              new IndividualEmploymentDetailsSectionDisplay());
      when(applicationService.getIndividualDetailsSectionDisplay(any())).thenReturn(otherParty);
      assertThat(mockMvc.perform(get("/case/details/other-party/0").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-details-other-party")
          .model()
          .containsEntry("otherParty", otherParty);
    }

    @Test
    @DisplayName("Should throw exception when other party list null")
    void caseDetailsOtherPartyThrowsExceptionWhenOtherPartyListNull() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(null);

      assertThat(mockMvc.perform(get("/case/details/other-party/0").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Could not find opponent with index 0");
    }

    @Test
    @DisplayName("Should throw exception when other party doesn't exist")
    void caseDetailsOtherPartyThrowsExceptionWhenOtherPartyDoesNotExist() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(Collections.emptyList());

      assertThat(mockMvc.perform(get("/case/details/other-party/0").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Could not find opponent with index 0");
    }
  }

  @Nested
  @DisplayName("/case/details/proceeding/{index} tests")
  class CaseDetailsProceeding {

    @Test
    @DisplayName("Should return view and model when case details exist")
    void caseDetailsProceedingReturnsViewAndModelWhenCaseDetailsExist() {
      ProceedingDetail proceeding = new ProceedingDetail();
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setApplicationType(new ApplicationType().id("APP_TYPE"));
      ebsCase.setCategoryOfLaw(new StringDisplayValue());
      ebsCase.setProceedings(Collections.singletonList(proceeding));
      assertThat(mockMvc.perform(get("/case/details/proceeding/0").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/proceeding-details")
          .model()
          .containsEntry("proceeding", proceeding);
    }

    @Test
    @DisplayName("Should throw exception when proceeding list null")
    void caseDetailsProceedingThrowsExceptionWhenOtherPartyListNull() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setProceedings(null);

      assertThat(mockMvc.perform(get("/case/details/proceeding/0").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Could not find proceeding with index: 0");
    }

    @Test
    @DisplayName("Should throw exception when proceeding doesn't exist")
    void caseDetailsProceedingThrowsExceptionWhenOtherPartyDoesNotExist() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setProceedings(List.of());

      assertThat(mockMvc.perform(get("/case/details/proceeding/1").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Could not find proceeding with index: 1");
    }

    @Test
    @DisplayName("Should throw exception when proceeding index is negative")
    void caseDetailsProceedingThrowsExceptionWhenIndexNegative() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setProceedings(List.of(new ProceedingDetail()));

      assertThat(mockMvc.perform(get("/case/details/proceeding/-1").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Could not find proceeding with index: -1");
    }

    @Test
    @DisplayName("Should return view and model when case details exist for an organisation")
    void caseDetailsOtherPartyOrganisationReturnsViewAndModelWhenCaseDetailsExist() {
      OpponentDetail opponent = new OpponentDetail();
      opponent.setType("Organisation");
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(Collections.singletonList(opponent));
      OrganisationDetailsSectionDisplay otherParty =
          new OrganisationDetailsSectionDisplay(
              new OrganisationOrganisationDetailsSectionDisplay(),
              new OrganisationAddressDetailsSectionDisplay());

      when(applicationService.getOrganisationDetailsSectionDisplay(any())).thenReturn(otherParty);
      assertThat(mockMvc.perform(get("/case/details/other-party/0").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-details-other-party-organisation")
          .model()
          .containsEntry("otherPartyOrganisation", otherParty);
    }
  }

  @Test
  void handleAbandonGetReturnsCorrectView() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    assertThat(mockMvc.perform(get("/case/amendment/abandon").sessionAttr(APPLICATION, ebsCase)))
        .hasStatusOk()
        .hasViewName("application/amendment-remove");
  }

  @Test
  void handleAbandonPostCallsServiceAndReturnsCorrectView() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    final UserDetail user = buildUserDetail();
    doNothing().when(applicationService).abandonApplication(ebsCase, user);

    assertThat(
            mockMvc.perform(
                post("/case/amendment/abandon")
                    .sessionAttr(APPLICATION, ebsCase)
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/case/overview");

    verify(applicationService, times(1)).abandonApplication(ebsCase, user);
  }

  @Test
  void handleAbandonPostCallsServiceAndReturnsErrorView() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    final UserDetail user = buildUserDetail();

    doThrow(new CaabApplicationException("Something went wrong"))
        .when(applicationService)
        .abandonApplication(ebsCase, user);

    assertThat(
            mockMvc.perform(
                post("/case/amendment/abandon")
                    .sessionAttr(APPLICATION, ebsCase)
                    .sessionAttr(USER_DETAILS, user)))
        .failure()
        .hasCauseInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("Something went wrong");

    verify(applicationService, times(1)).abandonApplication(ebsCase, user);
  }

  @Test
  void getCaseDetailsViewReturnsViewAndModelForValidIndex() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail();
    priorAuthority.setType(new StringDisplayValue().id("EXPERT").displayValue("Expert"));
    ebsCase.setPriorAuthorities(List.of(priorAuthority));

    assertThat(mockMvc.perform(get("/case/details/prior-authority/0").sessionAttr(CASE, ebsCase)))
        .hasStatusOk()
        .hasViewName("application/prior-authority-review")
        .model()
        .containsEntry("priorAuthority", priorAuthority);
  }

  @Test
  void getCaseDetailsViewThrowsExceptionWhenPriorAuthoritiesAreEmpty() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    ebsCase.setPriorAuthorities(List.of());

    assertThat(mockMvc.perform(get("/case/details/prior-authority/0").sessionAttr(CASE, ebsCase)))
        .failure()
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Could not find prior authority with index: 0");
  }

  @Test
  void getCaseDetailsViewThrowsExceptionForInvalidIndex() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    ebsCase.setPriorAuthorities(List.of(new PriorAuthorityDetail()));

    assertThat(mockMvc.perform(get("/case/details/prior-authority/1").sessionAttr(CASE, ebsCase)))
        .failure()
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Could not find prior authority with index: 1");
  }

  @Test
  @DisplayName("editGeneralDetails redirects to delegated functions for emergency app type")
  void editGeneralDetailsRedirectsToDelegatedFunctionsForEmergency() {
    ApplicationDetail tdsApplication = new ApplicationDetail();
    tdsApplication.setId(123);
    tdsApplication.setApplicationType(
        new uk.gov.laa.ccms.caab.model.ApplicationType().id(APP_TYPE_EMERGENCY));

    assertThat(
            mockMvc.perform(
                get("/case/amendment/edit-general-details")
                    .sessionAttr(APPLICATION, tdsApplication)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/amendments/edit-delegated-functions");
  }

  @Test
  @DisplayName("editGeneralDetails redirects to linked cases for non-emergency app type")
  void editGeneralDetailsRedirectsToLinkedCasesForNonEmergency() {
    ApplicationDetail tdsApplication = new ApplicationDetail();
    tdsApplication.setId(456);
    tdsApplication.setApplicationType(
        new uk.gov.laa.ccms.caab.model.ApplicationType().id("NON_EMERGENCY"));

    assertThat(
            mockMvc.perform(
                get("/case/amendment/edit-general-details")
                    .sessionAttr(APPLICATION, tdsApplication)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/amendments/sections/linked-cases");
  }

  @Test
  @DisplayName("editGeneralDetails throws exception if application type is null")
  void editGeneralDetailsThrowsIfApplicationTypeNull() {
    ApplicationDetail tdsApplication = new ApplicationDetail();
    tdsApplication.setId(789);

    assertThat(
            mockMvc.perform(
                get("/case/amendment/edit-general-details")
                    .sessionAttr(APPLICATION, tdsApplication)))
        .failure()
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TDS Application type must not be null");
  }

  private ApplicationDetail getEbsCase(
      String selectedCaseRef,
      Integer providerId,
      String providerReference,
      String clientFirstname,
      String clientSurname,
      String clientReference,
      boolean hasEbsAmendments,
      Integer proceedingId,
      String costId) { // Keep existing signature for other tests
    return getEbsCase(
        selectedCaseRef,
        providerId,
        providerReference,
        clientFirstname,
        clientSurname,
        clientReference,
        hasEbsAmendments,
        proceedingId,
        costId,
        List.of(FunctionConstants.AMEND_CASE)); // Default with AMEND_CASE
  }

  // Overloaded method to specify available functions
  private ApplicationDetail getEbsCase(
      String selectedCaseRef,
      Integer providerId,
      String providerReference,
      String clientFirstname,
      String clientSurname,
      String clientReference,
      boolean hasEbsAmendments,
      Integer proceedingId,
      String costId,
      List<String> availableFunctions) {
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(selectedCaseRef)
            .providerDetails(
                new ApplicationProviderDetails()
                    .provider(new IntDisplayValue().id(providerId))
                    .providerCaseReference(providerReference))
            .client(
                new ClientDetail()
                    .firstName(clientFirstname)
                    .surname(clientSurname)
                    .reference(clientReference))
            .availableFunctions(availableFunctions) // Use provided functions
            .amendment(false);

    if (costId != null) {
      ebsCase.setCosts(
          new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId)));
    }

    if (hasEbsAmendments
        && proceedingId
            != null) { // ensure proceedingId is not null if hasEbsAmendments is true for this setup
      ebsCase.setAmendmentProceedingsInEbs(List.of(new ProceedingDetail().id(proceedingId)));
    }

    return ebsCase;
  }

  private ActiveCase getActiveCase(
      String selectedCaseRef,
      Integer providerId,
      String clientFirstname,
      String clientSurname,
      String clientReference,
      String providerReference) {
    return ActiveCase.builder()
        .caseReferenceNumber(selectedCaseRef)
        .providerId(providerId)
        .client("%s %s".formatted(clientFirstname, clientSurname))
        .clientReferenceNumber(clientReference)
        .providerCaseReferenceNumber(providerReference)
        .build();
  }

  @Nested
  @DisplayName("caseCostAllocation tests")
  class CaseCostAllocationTests {

    @Test
    @DisplayName("Should display cost allocation page with calculated values")
    void shouldDisplayCostAllocationPageWithCalculatedValues() {
      // Given
      final java.math.BigDecimal grantedCost = new java.math.BigDecimal("1000.00");
      final java.math.BigDecimal counselCost1 = new java.math.BigDecimal("300.00");
      final java.math.BigDecimal counselCost2 = new java.math.BigDecimal("200.00");
      final java.math.BigDecimal mainProviderAllocation = new java.math.BigDecimal("500.00");
      final java.math.BigDecimal currentProviderBilled = new java.math.BigDecimal("150.00");

      final ApplicationDetail ebsCase = new ApplicationDetail();
      final CostStructureDetail costs = new CostStructureDetail();
      costs.setGrantedCostLimitation(grantedCost);

      final CostEntryDetail counsel1 = new CostEntryDetail();
      counsel1.setRequestedCosts(counselCost1);
      counsel1.setResourceName("Counsel One");

      final CostEntryDetail counsel2 = new CostEntryDetail();
      counsel2.setRequestedCosts(counselCost2);
      counsel2.setResourceName("Counsel Two");

      costs.setCostEntries(List.of(counsel1, counsel2));
      costs.setCurrentProviderBilledAmount(currentProviderBilled);
      ebsCase.setCosts(costs);

      final ApplicationSectionDisplay applicationSectionDisplay =
          ApplicationSectionDisplay.builder().build();

      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(applicationSectionDisplay);
      when(applicationService.calculateMainProviderAllocation(ebsCase))
          .thenReturn(mainProviderAllocation);
      when(applicationService.getCurrentProviderBilledAmount(ebsCase))
          .thenReturn(currentProviderBilled);

      // When/Then
      assertThat(mockMvc.perform(get("/case/details/costs/allocation").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/cost-limit-allocation")
          .model()
          .containsEntry("summary", applicationSectionDisplay)
          .containsEntry("case", ebsCase)
          .containsEntry("mainProviderAllocation", mainProviderAllocation)
          .containsEntry("currentProviderBilledAmount", currentProviderBilled);

      verify(applicationService).calculateMainProviderAllocation(ebsCase);
      verify(applicationService).getCurrentProviderBilledAmount(ebsCase);
    }

    @Test
    @DisplayName("Should handle null current provider billed amount")
    void shouldHandleNullCurrentProviderBilledAmount() {
      // Given
      final ApplicationDetail ebsCase = new ApplicationDetail();
      final CostStructureDetail costs = new CostStructureDetail();
      costs.setGrantedCostLimitation(new java.math.BigDecimal("1000.00"));
      costs.setCostEntries(Collections.emptyList());
      costs.setCurrentProviderBilledAmount(null);
      ebsCase.setCosts(costs);

      final ApplicationSectionDisplay applicationSectionDisplay =
          ApplicationSectionDisplay.builder().build();
      final java.math.BigDecimal mainProviderAllocation = new java.math.BigDecimal("1000.00");

      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(applicationSectionDisplay);
      when(applicationService.calculateMainProviderAllocation(ebsCase))
          .thenReturn(mainProviderAllocation);
      when(applicationService.getCurrentProviderBilledAmount(ebsCase)).thenReturn(null);

      // When/Then
      assertThat(mockMvc.perform(get("/case/details/costs/allocation").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/cost-limit-allocation")
          .model()
          .containsEntry("summary", applicationSectionDisplay)
          .containsEntry("case", ebsCase)
          .containsEntry("mainProviderAllocation", mainProviderAllocation)
          .containsEntry("currentProviderBilledAmount", null);

      verify(applicationService).calculateMainProviderAllocation(ebsCase);
      verify(applicationService).getCurrentProviderBilledAmount(ebsCase);
    }

    @Test
    @DisplayName("Should throw exception when case details display fails")
    void shouldThrowExceptionWhenCaseDetailsDisplayFails() {
      // Given
      final ApplicationDetail ebsCase = new ApplicationDetail();
      final CostStructureDetail costs = new CostStructureDetail();
      ebsCase.setCosts(costs);

      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(null);

      // When/Then
      assertThat(mockMvc.perform(get("/case/details/costs/allocation").sessionAttr(CASE, ebsCase)))
          .failure()
          .hasCauseInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Failed to retrieve case details");
    }
  }
}
