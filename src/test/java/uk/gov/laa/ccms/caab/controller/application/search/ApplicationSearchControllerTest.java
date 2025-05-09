package uk.gov.laa.ccms.caab.controller.application.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.feature.FeatureService;
import uk.gov.laa.ccms.caab.mapper.EbsApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringJUnitConfig
@WebAppConfiguration
public class ApplicationSearchControllerTest {
  @Mock
  private CaseSearchCriteriaValidator validator;

  @Mock
  private FeatureService featureService;

  @Mock
  private ProviderService providerService;

  @Mock
  private LookupService lookupService;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private EbsApplicationMapper applicationMapper;

  @Mock
  private SearchConstants searchConstants;

  @InjectMocks
  private ApplicationSearchController applicationSearchController;

  private MockMvc mockMvc;

  private UserDetail user;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(applicationSearchController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    this.user = buildUser();

    when(searchConstants.getMaxSearchResultsCases()).thenReturn(200);
    when(featureService.isEnabled(any())).thenReturn(true);
    when(validator.supports(any())).thenReturn(true);
  }

  @Test
  @DisplayName("Application search form populates dropdowns")
  public void getApplicationSearchPopulatesDropdowns() throws Exception {
    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();
    CaseStatusLookupDetail caseStatuses = new CaseStatusLookupDetail()
        .addContentItem(new CaseStatusLookupValueDetail());

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(caseStatuses));

    this.mockMvc.perform(get("/application/search")
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-search"))
        .andExpect(model().attribute("feeEarners", feeEarners))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()))
        .andExpect(model().attribute("statuses", caseStatuses.getContent()));
  }

  @Test
  @DisplayName("Application search handles missing lookup data")
  public void getApplicationSearchHandlesMissingLookupData() throws Exception {
    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.empty());
    when(lookupService.getCaseStatusValues())
        .thenReturn(Mono.empty());

    this.mockMvc.perform(get("/application/search")
            .sessionAttr(USER_DETAILS, user))
        .andExpect(result -> assertEquals("Failed to retrieve lookup data",
            assertInstanceOf(CaabApplicationException.class,
                result.getResolvedException()).getMessage()));
  }

  @Test
  @DisplayName("Application search is rejected when no search criteria is provided")
  public void postApplicationSearchHandlesValidationFailure() throws Exception {
    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();
    CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail()
        .addContentItem(new CaseStatusLookupValueDetail());

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(caseStatusLookupDetail));

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue(null, "required.atLeastOneSearchCriteria",
          "You must provide at least one search criteria below. Please amend your entry.");
      return null;
    }).when(validator).validate(any(), any());

    this.mockMvc.perform(post("/application/search")
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-search"))
        .andExpect(model().attribute("feeEarners", feeEarners))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()))
        .andExpect(model().attribute("statuses", caseStatusLookupDetail.getContent()));
  }

  @Test
  @DisplayName("Application search redirects to 'no results' screen when there are no results")
  public void postApplicationSearchNoResults() throws Exception {
    List<BaseApplicationDetail> baseApplications = new ArrayList<>();

    when(applicationService.getCases(any(), any())).thenReturn(baseApplications);

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    this.mockMvc.perform(post("/application/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-search-no-results"));

    verify(applicationService).getCases(eq(caseSearchCriteria), any());
  }

  @Test
  @DisplayName("Application search redirects to 'too many results' screen when result limit exceeded")
  public void postApplicationSearchWithTooManyResults() throws Exception {
    when(applicationService.getCases(any(), any())).thenThrow(
        new TooManyResultsException(""));

    this.mockMvc.perform(post("/application/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name(
            "application/application-search-too-many-results"));
  }

  @Test
  @DisplayName("Application search returns results")
  public void postApplicationSearchWithResults() throws Exception {
    List<BaseApplicationDetail> caseSearchResults = List.of(new BaseApplicationDetail());

    when(applicationService.getCases(any(), any())).thenReturn(caseSearchResults);

    this.mockMvc.perform(post("/application/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/search/results"));
  }

  @Test
  @DisplayName("Application search succeeds when text fields have valid values")
  void postApplicationSearchNoValidationErrorsMaxLengthsNotExceededDisplaysNoResults()
      throws Exception {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setClientSurname(RandomStringUtils.insecure().nextAlphabetic(35));
    caseSearchCriteria.setCaseReference(RandomStringUtils.insecure().nextAlphabetic(35));
    caseSearchCriteria.setProviderCaseReference(RandomStringUtils.insecure().nextAlphabetic(35));

    mockMvc.perform(post("/application/search")
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria)
            .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(view().name(
            "application/application-search-no-results"));
  }

  @Test
  @DisplayName("Application search is rejected when field max length is exceeded")
  void postApplicationSearchValidationErrorsMaxLengthsExceededReturnsToSearch() throws Exception {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();

    caseSearchCriteria.setClientSurname(RandomStringUtils.insecure().nextAlphabetic(36));
    caseSearchCriteria.setCaseReference(RandomStringUtils.insecure().nextAlphabetic(36));
    caseSearchCriteria.setProviderCaseReference(RandomStringUtils.insecure().nextAlphabetic(36));

    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();
    CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail()
        .addContentItem(new CaseStatusLookupValueDetail());

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(caseStatusLookupDetail));

    mockMvc.perform(post("/application/search")
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria)
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors(CASE_SEARCH_CRITERIA, "clientSurname"))
        .andExpect(model().attributeHasFieldErrors(CASE_SEARCH_CRITERIA, "providerCaseReference"))
        .andExpect(model().attributeHasFieldErrors(CASE_SEARCH_CRITERIA, "caseReference"))
        .andExpect(model().attribute("feeEarners", feeEarners))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()))
        .andExpect(model().attribute("statuses", caseStatusLookupDetail.getContent()))
        .andExpect(view().name("application/application-search"));
  }

  @Test
  @DisplayName("Application search results are paginated")
  public void applicationSearchResultsPaginatesResults() throws Exception {
    List<BaseApplicationDetail> caseSearchResults = List.of(
        new BaseApplicationDetail(),
        new BaseApplicationDetail());

    when(applicationMapper.toApplicationDetails(any()))
        .thenReturn(new ApplicationDetails());

    this.mockMvc.perform(get("/application/search/results")
            .param("page", "0")
            .param("size", "1")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_RESULTS, caseSearchResults))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-search-results"))
        .andExpect(model().attributeExists(CopyCaseSearchController.CASE_RESULTS_PAGE));
  }

  @Test
  @DisplayName("Attempting to view an application with an invalid case reference is rejected")
  public void selectApplicationRejectsInvalidCaseReference() throws Exception {

    // No TDS applications
    when(applicationService.getTdsApplications(any(), any(), any(), any()))
        .thenReturn(new ApplicationDetails().content(Collections.emptyList()));

    mockMvc.perform(get("/application/{case-reference-number}/view", "1")
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(result -> assertInstanceOf(CaabApplicationException.class,
            result.getResolvedException()));
  }

  @Test
  @DisplayName("Selecting an application with unsubmitted status which is not under amendment"
      + "redirects the user to the application sections screen")
  public void selectApplicationWithUnsubmittedStatusRedirectsToCaseOverview()
      throws Exception {
    final String selectedCaseRef = "1";
    final String appRef = "2";

    // TDS application
    ApplicationDetails applicationDetails = new ApplicationDetails()
        .addContentItem(new BaseApplicationDetail()
            .id(Integer.parseInt(appRef))
            .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
            .caseReferenceNumber(selectedCaseRef));

    when(applicationService.getTdsApplications(any(), any(), any(), any()))
        .thenReturn(applicationDetails);

    mockMvc.perform(get("/application/{case-reference-number}/view", selectedCaseRef)
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(request().sessionAttribute(APPLICATION_ID, Integer.parseInt(appRef)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections"));
  }

  @Test
  @DisplayName("Selecting an application with a status other than unsubmitted"
      + "redirects the user to the case overview screen")
  public void selectApplicationWithOtherStatusRedirectsToCaseOverview() throws Exception {
    final String selectedCaseRef = "2";
    final String appRef = "3";

    // EBS Case
    ApplicationDetail ebsCase = new ApplicationDetail()
        .caseReferenceNumber(selectedCaseRef);

    when(applicationService.getCase(any(), any(Long.class), any())).thenReturn(ebsCase);

    // TDS application
    BaseApplicationDetail tdsApplication = new BaseApplicationDetail()
        .id(Integer.parseInt(appRef))
        .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
        .caseReferenceNumber(selectedCaseRef);

    ApplicationDetails appDetails = new ApplicationDetails()
        .addContentItem(tdsApplication);

    when(applicationService.getTdsApplications(any(), any(), any(), any()))
        .thenReturn(appDetails);

    mockMvc.perform(get("/application/{case-reference-number}/view", selectedCaseRef)
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(request().sessionAttribute(CASE, ebsCase))
        .andExpect(request().sessionAttribute(APPLICATION, tdsApplication))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/case/overview"));
  }

  @Test
  @DisplayName("Selecting an application under amendment redirects"
      + "the user to the case overview screen")
  public void selectApplicationAmendmentRedirectsToCaseOverview() throws Exception {
    final String selectedCaseRef = "2";

    // EBS Case
    ApplicationDetail applicationDetail = new ApplicationDetail()
        .caseReferenceNumber(selectedCaseRef);

    // No TDS applications
    when(applicationService.getTdsApplications(any(), any(), any(), any()))
        .thenReturn(new ApplicationDetails().content(Collections.emptyList()));

    when(applicationService.getCase(any(), any(Long.class), any())).thenReturn(applicationDetail);

    mockMvc.perform(get("/application/{case-reference-number}/view", selectedCaseRef)
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(request().sessionAttribute(CASE, applicationDetail))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/case/overview"));
  }

  @Test
  @DisplayName("Case overview screen loads case details")
  public void caseOverviewLoadsCaseDetails() throws Exception {
    final String selectedCaseRef = "2";
    final Integer providerId = 1;
    final String providerReference = "providerReference";
    final String clientFirstname = "firstname";
    final String clientSurname = "surname";
    final String clientReference = "clientReference";

    // EBS Case
    ApplicationDetail applicationDetail =
        getEbsCase(selectedCaseRef, providerId, providerReference, clientFirstname, clientSurname,
            clientReference, false, null, null, List.of(FunctionConstants.AMEND_CASE));

    final ActiveCase activeCase =
        getActiveCase(selectedCaseRef, providerId, clientFirstname, clientSurname, clientReference,
            providerReference);

    mockMvc
        .perform(
            get("/case/overview", selectedCaseRef)
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(CASE, applicationDetail))
        .andDo(print())
        .andExpect(request().sessionAttribute(CASE, applicationDetail))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase))
        .andExpect(model().attribute("hasEbsAmendments", false))
        .andExpect(model().attribute("draftProceedings", Matchers.empty()))
        .andExpect(model().attribute("draftCosts", Matchers.nullValue()))
        .andExpect(model().attribute("availableActions", Matchers.hasSize(1)))
        .andExpect(model().attribute("returnTo", "caseSearchResults"))
        .andExpect(model().attribute(NOTIFICATION_ID, Matchers.nullValue()))
        .andExpect(view().name("application/case-overview"));
  }

  @Test
  @DisplayName("Case overview screen correctly sets return link")
  public void caseOverviewSetsReturnTo() throws Exception {
    final String selectedCaseRef = "2";
    final Integer providerId = 1;
    final String providerReference = "providerReference";
    final String clientFirstname = "firstname";
    final String clientSurname = "surname";
    final String clientReference = "clientReference";
    final String notificationId = "5";

    // EBS Case
    ApplicationDetail applicationDetail =
        getEbsCase(selectedCaseRef, providerId, providerReference, clientFirstname, clientSurname,
            clientReference, false, null, null);

    final ActiveCase activeCase =
        getActiveCase(selectedCaseRef, providerId, clientFirstname, clientSurname, clientReference,
            providerReference);

    mockMvc
        .perform(
            get("/case/overview", selectedCaseRef)
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(CASE, applicationDetail)
                .sessionAttr(NOTIFICATION_ID, notificationId)
                .header("referer", "/notifications/%s".formatted(notificationId)))
        .andDo(print())
        .andExpect(request().sessionAttribute(CASE, applicationDetail))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase))
        .andExpect(model().attribute("hasEbsAmendments", false))
        .andExpect(model().attribute("draftProceedings", Matchers.empty()))
        .andExpect(model().attribute("draftCosts", Matchers.nullValue()))
        .andExpect(model().attribute("returnTo", "notification"))
        .andExpect(model().attribute(NOTIFICATION_ID, notificationId))
        .andExpect(view().name("application/case-overview"));
  }

  @Test
  @DisplayName("Case overview screen sets amendments details from EBS case")
  public void caseOverviewSetsEbsCaseAmendments() throws Exception {
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
        getEbsCase(selectedCaseRef, providerId, providerReference, clientFirstname, clientSurname,
            clientReference, hasEbsAmendments, proceedingId, costId);

    final ActiveCase activeCase =
        getActiveCase(selectedCaseRef, providerId, clientFirstname, clientSurname, clientReference,
            providerReference);

    // TDS application
    BaseApplicationDetail tdsApplication = new BaseApplicationDetail()
        .id(Integer.parseInt(appRef))
        .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
        .caseReferenceNumber(selectedCaseRef);

    when(applicationService.getApplication(any())).thenReturn(Mono.empty());

    ProceedingDetail expectedProceeding = new ProceedingDetail().id(proceedingId);
    CostStructureDetail expectedCost =
        new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId));

    mockMvc
        .perform(
            get("/case/overview", selectedCaseRef)
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(CASE, applicationDetail)
                .sessionAttr(APPLICATION, tdsApplication))
        .andDo(print())
        .andExpect(request().sessionAttribute(CASE, applicationDetail))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase))
        .andExpect(model().attribute("hasEbsAmendments", hasEbsAmendments))
        .andExpect(model().attribute("draftProceedings", List.of(expectedProceeding)))
        .andExpect(model().attribute("draftCosts", expectedCost))
        .andExpect(model().attribute("returnTo", "caseSearchResults"))
        .andExpect(model().attribute(NOTIFICATION_ID, Matchers.nullValue()))
        .andExpect(view().name("application/case-overview"));
  }

  @Test
  @DisplayName("Case overview screen sets amendments details from TDS")
  public void caseOverviewSetsTdsAmendments() throws Exception {
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
        getEbsCase(selectedCaseRef, providerId, providerReference, clientFirstname, clientSurname,
            clientReference, hasEbsAmendments, null, null);

    final ActiveCase activeCase =
        getActiveCase(selectedCaseRef, providerId, clientFirstname, clientSurname, clientReference,
            providerReference);

    // TDS application
    BaseApplicationDetail tdsApplication = new BaseApplicationDetail()
        .id(Integer.parseInt(appRef))
        .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
        .caseReferenceNumber(selectedCaseRef);

    ProceedingDetail expectedProceeding = new ProceedingDetail().id(proceedingId);
    CostStructureDetail expectedCost =
        new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId));

    ApplicationDetail amendments =
        new ApplicationDetail()
            .proceedings(List.of(expectedProceeding))
            .costs(expectedCost);

    when(applicationService.getApplication(any())).thenReturn(Mono.just(amendments));

    mockMvc
        .perform(
            get("/case/overview", selectedCaseRef)
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(CASE, applicationDetail)
                .sessionAttr(APPLICATION, tdsApplication))
        .andDo(print())
        .andExpect(request().sessionAttribute(CASE, applicationDetail))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase))
        .andExpect(model().attribute("hasEbsAmendments", hasEbsAmendments))
        .andExpect(model().attribute("draftProceedings", List.of(expectedProceeding)))
        .andExpect(model().attribute("draftCosts", expectedCost))
        .andExpect(model().attribute("returnTo", "caseSearchResults"))
        .andExpect(model().attribute(NOTIFICATION_ID, Matchers.nullValue()))
        .andExpect(view().name("application/case-overview"));
  }

  @Test
  @DisplayName("Case overview screen shows no available actions when ebsCase has no functions")
  public void caseOverviewNoAvailableFunctionsShowsNoActions() throws Exception {
    final String selectedCaseRef = "3";
    ApplicationDetail ebsCase = getEbsCase(
        selectedCaseRef, 1, "ref", "client", "smith", "clientRef", false, null, null,
        Collections.emptyList());

    mockMvc.perform(
            get("/case/overview").sessionAttr(USER_DETAILS, user).sessionAttr(CASE, ebsCase))
        .andExpect(status().isOk())
        .andExpect(model().attribute("availableActions", Matchers.empty()));
  }

  @Test
  @DisplayName(
      "Case overview screen shows 'Continue Amendment' when AMEND_CASE is available and it's a TDS amendment")
  public void caseOverviewAmendCaseIsTdsAmendmentShowsContinueAmendment() throws Exception {
    final String selectedCaseRef = "4";
    ApplicationDetail ebsCase = getEbsCase(
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

    mockMvc.perform(
            get("/case/overview")
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(APPLICATION, tdsApplication) // tdsApplication is present
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute(
            "availableActions",
            Matchers.hasSize(1)));
  }

  @Test
  @DisplayName(
      "Case overview screen shows 'Continue Amendment' when AMEND_CASE is available and there are EBS amendments")
  public void caseOverviewAmendCaseHasEbsAmendmentsShowsContinueAmendment() throws Exception {
    final String selectedCaseRef = "5";
    ApplicationDetail ebsCase = getEbsCase(
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

    mockMvc.perform(
            get("/case/overview").sessionAttr(USER_DETAILS, user).sessionAttr(CASE, ebsCase))
        .andExpect(status().isOk())
        .andExpect(model().attribute(
            "availableActions",
            Matchers.hasSize(1)));
  }

  @Test
  @DisplayName("Case overview screen filters available actions based on predefined list and ebsCase functions")
  public void caseOverviewFiltersAvailableActions() throws Exception {
    final String selectedCaseRef = "6";
    ApplicationDetail ebsCase = getEbsCase(
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

    mockMvc.perform(
            get("/case/overview").sessionAttr(USER_DETAILS, user).sessionAttr(CASE, ebsCase))
        .andExpect(status().isOk())
        .andExpect(model().attribute(
            "availableActions",
            Matchers.hasSize(2)))
        .andExpect(model().attribute("availableActions", Matchers.iterableWithSize(2)));
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
    ApplicationDetail ebsCase = new ApplicationDetail()
        .caseReferenceNumber(selectedCaseRef)
        .providerDetails(new ApplicationProviderDetails()
            .provider(new IntDisplayValue().id(providerId))
            .providerCaseReference(providerReference))
        .client(new ClientDetail()
            .firstName(clientFirstname)
            .surname(clientSurname)
            .reference(clientReference))
        .costs(new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId)))
        .availableFunctions(availableFunctions)// Use provided functions
        .amendment(false);

    if (hasEbsAmendments
        && proceedingId !=
        null) { // ensure proceedingId is not null if hasEbsAmendments is true for this setup
      ebsCase.setAmendmentProceedingsInEbs(List.of(new ProceedingDetail().id(proceedingId)));
    }

    return ebsCase;
  }

  private ActiveCase getActiveCase(String selectedCaseRef, Integer providerId,
                                   String clientFirstname,
                                   String clientSurname, String clientReference,
                                   String providerReference) {
    return ActiveCase.builder()
        .caseReferenceNumber(selectedCaseRef)
        .providerId(providerId)
        .client("%s %s".formatted(clientFirstname, clientSurname))
        .clientReferenceNumber(clientReference)
        .providerCaseReferenceNumber(providerReference)
        .build();
  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildBaseProvider());
  }

  private BaseProvider buildBaseProvider() {
    return new BaseProvider()
        .id(123)
        .name("provider1")
        .addOfficesItem(new BaseOffice()
            .id(10)
            .name("Office 1"))
        .addOfficesItem(new BaseOffice()
            .id(11)
            .name("Office 2"));
  }

  private List<ContactDetail> buildFeeEarners() {
    List<ContactDetail> feeEarners = new ArrayList<>();
    feeEarners.add(new ContactDetail()
        .id(1)
        .name("FeeEarner1"));
    feeEarners.add(new ContactDetail()
        .id(2)
        .name("FeeEarner2"));
    return feeEarners;
  }
}
