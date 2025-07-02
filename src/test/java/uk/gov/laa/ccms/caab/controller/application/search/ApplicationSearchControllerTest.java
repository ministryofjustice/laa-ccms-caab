package uk.gov.laa.ccms.caab.controller.application.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.controller.application.ApplicationTestUtils;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.feature.FeatureService;
import uk.gov.laa.ccms.caab.mapper.EbsApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@SpringJUnitConfig
@WebAppConfiguration
public class ApplicationSearchControllerTest {
  @Mock private CaseSearchCriteriaValidator validator;

  @Mock private FeatureService featureService;

  @Mock private ProviderService providerService;

  @Mock private LookupService lookupService;

  @Mock private ApplicationService applicationService;

  @Mock private EbsApplicationMapper applicationMapper;

  @Mock private SearchConstants searchConstants;

  @InjectMocks private ApplicationSearchController applicationSearchController;

  private MockMvcTester mockMvc;

  private UserDetail user;

  @BeforeEach
  public void setup() {
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(applicationSearchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build());
    this.user = ApplicationTestUtils.buildUser();

    when(searchConstants.getMaxSearchResultsCases()).thenReturn(200);
    when(featureService.isEnabled(any())).thenReturn(true);
    when(validator.supports(any())).thenReturn(true);
  }

  @Test
  @DisplayName("Application search form populates dropdowns")
  public void getApplicationSearchPopulatesDropdowns() throws Exception {
    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = ApplicationTestUtils.buildFeeEarners();
    CaseStatusLookupDetail caseStatuses =
        new CaseStatusLookupDetail().addContentItem(new CaseStatusLookupValueDetail());

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(caseStatuses));

    assertThat(this.mockMvc.perform(get("/application/search").sessionAttr("user", user)))
        .hasStatusOk()
        .hasViewName("application/application-search")
        .model()
        .containsEntry("feeEarners", feeEarners)
        .containsEntry("offices", user.getProvider().getOffices())
        .containsEntry("statuses", caseStatuses.getContent());
  }

  @Test
  @DisplayName("Application search handles missing lookup data")
  public void getApplicationSearchHandlesMissingLookupData() throws Exception {
    when(providerService.getProvider(user.getProvider().getId())).thenReturn(Mono.empty());
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.empty());

    assertThat(this.mockMvc.perform(get("/application/search").sessionAttr(USER_DETAILS, user)))
        .failure()
        .isInstanceOf(CaabApplicationException.class)
        .hasMessage("Failed to retrieve lookup data");
  }

  @Test
  @DisplayName("Application search is rejected when no search criteria is provided")
  public void postApplicationSearchHandlesValidationFailure() throws Exception {
    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = ApplicationTestUtils.buildFeeEarners();
    CaseStatusLookupDetail caseStatusLookupDetail =
        new CaseStatusLookupDetail().addContentItem(new CaseStatusLookupValueDetail());

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(caseStatusLookupDetail));

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue(
                  null,
                  "required.atLeastOneSearchCriteria",
                  "You must provide at least one search criteria below. Please amend your entry.");
              return null;
            })
        .when(validator)
        .validate(any(), any());

    assertThat(this.mockMvc.perform(post("/application/search").sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("application/application-search")
        .model()
        .containsEntry("feeEarners", feeEarners)
        .containsEntry("offices", user.getProvider().getOffices())
        .containsEntry("statuses", caseStatusLookupDetail.getContent());
  }

  @Test
  @DisplayName("Application search redirects to 'no results' screen when there are no results")
  public void postApplicationSearchNoResults() throws Exception {
    List<BaseApplicationDetail> baseApplications = new ArrayList<>();

    when(applicationService.getCases(any(), any())).thenReturn(baseApplications);

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    assertThat(
            this.mockMvc.perform(
                post("/application/search")
                    .sessionAttr(USER_DETAILS, user)
                    .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria)))
        .hasStatusOk()
        .hasViewName("application/application-search-no-results");

    verify(applicationService).getCases(eq(caseSearchCriteria), any());
  }

  @Test
  @DisplayName(
      "Application search redirects to 'too many results' screen when result limit exceeded")
  public void postApplicationSearchWithTooManyResults() throws Exception {
    when(applicationService.getCases(any(), any())).thenThrow(new TooManyResultsException(""));

    assertThat(
            this.mockMvc.perform(
                post("/application/search")
                    .sessionAttr(USER_DETAILS, user)
                    .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria())))
        .hasStatusOk()
        .hasViewName("application/application-search-too-many-results");
  }

  @Test
  @DisplayName("Application search returns results")
  public void postApplicationSearchWithResults() throws Exception {
    List<BaseApplicationDetail> caseSearchResults = List.of(new BaseApplicationDetail());

    when(applicationService.getCases(any(), any())).thenReturn(caseSearchResults);

    assertThat(
            this.mockMvc.perform(
                post("/application/search")
                    .sessionAttr(USER_DETAILS, user)
                    .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria())))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/application/search/results");
  }

  @Test
  @DisplayName("Application search succeeds when text fields have valid values")
  void postApplicationSearchNoValidationErrorsMaxLengthsNotExceededDisplaysNoResults()
      throws Exception {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setClientSurname(RandomStringUtils.insecure().nextAlphabetic(35));
    caseSearchCriteria.setCaseReference(RandomStringUtils.insecure().nextAlphabetic(35));
    caseSearchCriteria.setProviderCaseReference(RandomStringUtils.insecure().nextAlphabetic(35));

    assertThat(
            mockMvc.perform(
                post("/application/search")
                    .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria)
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("application/application-search-no-results");
  }

  @Test
  @DisplayName("Application search is rejected when field max length is exceeded")
  void postApplicationSearchValidationErrorsMaxLengthsExceededReturnsToSearch() throws Exception {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();

    caseSearchCriteria.setClientSurname(RandomStringUtils.insecure().nextAlphabetic(36));
    caseSearchCriteria.setCaseReference(RandomStringUtils.insecure().nextAlphabetic(36));
    caseSearchCriteria.setProviderCaseReference(RandomStringUtils.insecure().nextAlphabetic(36));

    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = ApplicationTestUtils.buildFeeEarners();
    CaseStatusLookupDetail caseStatusLookupDetail =
        new CaseStatusLookupDetail().addContentItem(new CaseStatusLookupValueDetail());

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(caseStatusLookupDetail));

    assertThat(
            mockMvc.perform(
                post("/application/search")
                    .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria)
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("application/application-search")
        .model()
        .containsEntry("feeEarners", feeEarners)
        .containsEntry("offices", user.getProvider().getOffices())
        .containsEntry("statuses", caseStatusLookupDetail.getContent())
        .extractingBindingResult(CASE_SEARCH_CRITERIA)
        .hasFieldErrors("clientSurname", "providerCaseReference", "caseReference");
  }

  @Test
  @DisplayName("Application search results are paginated")
  public void applicationSearchResultsPaginatesResults() throws Exception {
    List<BaseApplicationDetail> caseSearchResults =
        List.of(new BaseApplicationDetail(), new BaseApplicationDetail());

    when(applicationMapper.toApplicationDetails(any())).thenReturn(new ApplicationDetails());

    assertThat(
            this.mockMvc.perform(
                get("/application/search/results")
                    .param("page", "0")
                    .param("size", "1")
                    .sessionAttr(USER_DETAILS, user)
                    .sessionAttr(CASE_SEARCH_RESULTS, caseSearchResults)))
        .hasStatusOk()
        .hasViewName("application/application-search-results")
        .model()
        .containsKey(CopyCaseSearchController.CASE_RESULTS_PAGE);
  }

  @Test
  @DisplayName("Attempting to view an application with an invalid case reference is rejected")
  public void selectApplicationRejectsInvalidCaseReference() throws Exception {
    String caseReference = "1";

    // No TDS applications
    when(applicationService.getTdsApplications(any(), any(), any(), any()))
        .thenReturn(new ApplicationDetails().content(Collections.emptyList()));

    assertThat(
            mockMvc.perform(
                get("/application/{case-reference-number}/view", caseReference)
                    .sessionAttr(USER_DETAILS, user)))
        .failure()
        .isInstanceOf(CaabApplicationException.class)
        .hasMessage(
            "Unable to find case in EBS or application in TDS with case reference "
                + caseReference);
  }

  @Test
  @DisplayName(
      "Selecting an application with unsubmitted status which is not under amendment"
          + "redirects the user to the application sections screen")
  public void selectApplicationWithUnsubmittedStatusRedirectsToCaseOverview() throws Exception {
    final String selectedCaseRef = "1";
    final String appRef = "2";

    // TDS application
    ApplicationDetails applicationDetails =
        new ApplicationDetails()
            .addContentItem(
                new BaseApplicationDetail()
                    .id(Integer.parseInt(appRef))
                    .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
                    .caseReferenceNumber(selectedCaseRef));

    when(applicationService.getTdsApplications(any(), any(), any(), any()))
        .thenReturn(applicationDetails);

    assertThat(
            mockMvc.perform(
                get("/application/{case-reference-number}/view", selectedCaseRef)
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/application/sections")
        .request()
        .sessionAttributes()
        .hasEntrySatisfying(
            APPLICATION_ID, value -> assertThat(value).isEqualTo(Integer.parseInt(appRef)));
  }

  @Test
  @DisplayName(
      "Selecting an application with a status other than unsubmittedredirects the user to the case overview screen")
  public void selectApplicationWithOtherStatusRedirectsToCaseOverview() throws Exception {
    final String selectedCaseRef = "2";
    final String appRef = "3";

    // EBS Case
    ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber(selectedCaseRef);

    when(applicationService.getCase(any(), any(Long.class), any())).thenReturn(ebsCase);

    // TDS application
    BaseApplicationDetail tdsApplication =
        new BaseApplicationDetail()
            .id(Integer.parseInt(appRef))
            .status(new StringDisplayValue().id(STATUS_UNSUBMITTED_ACTUAL_VALUE))
            .caseReferenceNumber(selectedCaseRef);

    ApplicationDetails appDetails = new ApplicationDetails().addContentItem(tdsApplication);

    when(applicationService.getTdsApplications(any(), any(), any(), any())).thenReturn(appDetails);

    assertThat(
            mockMvc.perform(
                get("/application/{case-reference-number}/view", selectedCaseRef)
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/case/overview")
        .request()
        .sessionAttributes()
        .hasEntrySatisfying(CASE, value -> assertThat(value).isEqualTo(ebsCase))
        .hasEntrySatisfying(
            APPLICATION_SUMMARY, value -> assertThat(value).isEqualTo(tdsApplication));
  }

  @Test
  @DisplayName(
      "Selecting an application under amendment redirects the user to the case overview screen")
  public void selectApplicationAmendmentRedirectsToCaseOverview() throws Exception {
    final String selectedCaseRef = "2";

    // EBS Case
    ApplicationDetail applicationDetail =
        new ApplicationDetail().caseReferenceNumber(selectedCaseRef);

    // No TDS applications
    when(applicationService.getTdsApplications(any(), any(), any(), any()))
        .thenReturn(new ApplicationDetails().content(Collections.emptyList()));

    when(applicationService.getCase(any(), any(Long.class), any())).thenReturn(applicationDetail);

    assertThat(
            mockMvc.perform(
                get("/application/{case-reference-number}/view", selectedCaseRef)
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/case/overview")
        .request()
        .sessionAttributes()
        .hasEntrySatisfying(CASE, value -> assertThat(value).isEqualTo(applicationDetail));
  }
}
