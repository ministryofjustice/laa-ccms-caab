package uk.gov.laa.ccms.caab.controller.application.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplication;
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
  private ProviderService providerService;

  @Mock
  private LookupService lookupService;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private ApplicationMapper applicationMapper;

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
    mockMvc = standaloneSetup(applicationSearchController).build();
    this.user = buildUser();

    when(searchConstants.getMaxSearchResultsCases()).thenReturn(200);
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
  public void testGetApplicationSearch_HandlesMissingLookupData() {
    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.empty());
    when(lookupService.getCaseStatusValues())
        .thenReturn(Mono.empty());

    Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(get("/application/search")
            .sessionAttr(USER_DETAILS, user)));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to retrieve lookup data",
        exception.getCause().getMessage());
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
    List<BaseApplication> baseApplications = new ArrayList<>();

    when(applicationService.getCases(any(), any(), any())).thenReturn(baseApplications);

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    this.mockMvc.perform(post("/application/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-search-no-results"));

    verify(applicationService).getCases(eq(caseSearchCriteria), any(), any());
  }

  @Test
  public void testPostApplicationSearch_WithTooManyResults() throws Exception {
    when(applicationService.getCases(any(), any(), any())).thenThrow(
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
    List<BaseApplication> caseSearchResults = List.of(new BaseApplication());

    when(applicationService.getCases(any(), any(), any())).thenReturn(caseSearchResults);

    this.mockMvc.perform(post("/application/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/search/results"));
  }

  @Test
  public void testGetApplicationSearchResults_PaginatesResults() throws Exception {
    List<BaseApplication> caseSearchResults = List.of(
        new BaseApplication(),
        new BaseApplication());

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
