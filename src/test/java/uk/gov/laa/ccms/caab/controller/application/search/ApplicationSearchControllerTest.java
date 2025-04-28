package uk.gov.laa.ccms.caab.controller.application.search;

import org.hamcrest.Matchers;
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
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration
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

  @Autowired
  private WebApplicationContext webApplicationContext;

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
  public void testGetApplicationSearch_PopulatesDropdowns() throws Exception {
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
  public void testGetApplicationSearch_HandlesMissingLookupData() throws Exception {
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
  public void testPostApplicationSearchHandlesValidationFailure() throws Exception {
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
  public void testPostApplicationSearch_NoResults() throws Exception {
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
  public void testPostApplicationSearch_WithTooManyResults() throws Exception {
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
  public void testPostApplicationSearch_WithResults() throws Exception {
    List<BaseApplicationDetail> caseSearchResults = List.of(new BaseApplicationDetail());

    when(applicationService.getCases(any(), any())).thenReturn(caseSearchResults);

    this.mockMvc.perform(post("/application/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/search/results"));
  }

  @Test
  void testPostApplicationSearch_noValidationErrors_maxLengthsNotExceeded_displaysNoResults() throws Exception {
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
  void testPostApplicationSearch_validationErrors_maxLengthsExceeded_returnsToSearch() throws Exception {
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
  public void testGetApplicationSearchResults_PaginatesResults() throws Exception {
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
  public void testSelectApplication_rejectsInvalidCaseReference() throws Exception {

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
  public void testSelectApplication_unsubmittedApplication_redirectsToApplicationSummary()
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
  public void testSelectApplication_otherStatus_redirectsToCaseOverview() throws Exception {
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
  public void testSelectApplication_amendment_redirectsToCaseOverview() throws Exception {
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
  public void testCaseOverview_loadsCaseDetails() throws Exception {
    final String selectedCaseRef = "2";
    final Integer providerId = 1;
    final String providerReference = "providerReference";
    final String clientFirstname = "firstname";
    final String clientSurname = "surname";
    final String clientReference = "clientReference";

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
                .sessionAttr(CASE, applicationDetail))
        .andDo(print())
        .andExpect(request().sessionAttribute(CASE, applicationDetail))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase))
        .andExpect(model().attribute("hasEbsAmendments", false))
        .andExpect(model().attribute("draftProceedings", Matchers.empty()))
        .andExpect(model().attribute("draftCosts", Matchers.nullValue()))
        .andExpect(model().attribute("returnTo", "caseSearchResults"))
        .andExpect(model().attribute(NOTIFICATION_ID, Matchers.nullValue()))
        .andExpect(view().name("application/case-overview"));
  }

  @Test
  public void testCaseOverview_setsReturnTo() throws Exception {
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
  public void testCaseOverview_setsAmendmentDetails_fromEbsCase() throws Exception {
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
  public void testCaseOverview_setsAmendmentDetails_fromTdsAmendments() throws Exception {
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

  private ApplicationDetail getEbsCase(String selectedCaseRef, Integer providerId,
      String providerReference, String clientFirstname, String clientSurname,
      String clientReference, boolean hasEbsAmendments, Integer proceedingId, String costId) {
    ApplicationDetail ebsCase =  new ApplicationDetail()
        .caseReferenceNumber(selectedCaseRef)
        .providerDetails(new ApplicationProviderDetails()
            .provider(new IntDisplayValue()
                .id(providerId))
            .providerCaseReference(providerReference))
        .client(new ClientDetail()
            .firstName(clientFirstname)
            .surname(clientSurname)
            .reference(clientReference))
        .costs(new CostStructureDetail().addCostEntriesItem(new CostEntryDetail().ebsId(costId)))
        .amendment(false);

    if (hasEbsAmendments) {
      ebsCase.setAmendmentProceedingsInEbs(List.of(new ProceedingDetail().id(proceedingId)));
    }

    return ebsCase;
  }

  private ActiveCase getActiveCase(String selectedCaseRef, Integer providerId, String clientFirstname,
      String clientSurname, String clientReference, String providerReference) {
    return ActiveCase.builder()
        .caseReferenceNumber(selectedCaseRef)
        .providerId(providerId)
        .client("%s %s" .formatted(clientFirstname, clientSurname))
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
