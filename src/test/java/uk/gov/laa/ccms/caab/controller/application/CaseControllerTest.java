package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.Collections;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
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
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class CaseControllerTest {

  @Mock private ApplicationService applicationService;

  @InjectMocks private CaseController caseController;

  private MockMvcTester mockMvc;

  private UserDetail user;

  private String returnUrl;

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
  }

  @Nested
  @DisplayName("/case/overview tests")
  class CaseOverview {

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
                      .sessionAttr("SEARCH URL", returnUrl)))
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
                      .sessionAttr("SEARCH URL", returnUrl)
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
                      .sessionAttr("SEARCH URL", returnUrl)))
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
                      .sessionAttr("SEARCH URL", returnUrl)))
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
                      .sessionAttr("SEARCH URL", returnUrl)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "availableActions",
              value ->
                  assertThat(value).asInstanceOf(InstanceOfAssertFactories.COLLECTION).isEmpty());
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
                      .sessionAttr("SEARCH URL", returnUrl)))
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
                      .sessionAttr("SEARCH URL", returnUrl)))
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
                      .sessionAttr("SEARCH URL", returnUrl)))
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
            .costs(
                new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId)))
            .availableFunctions(availableFunctions) // Use provided functions
            .amendment(false);

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
}
