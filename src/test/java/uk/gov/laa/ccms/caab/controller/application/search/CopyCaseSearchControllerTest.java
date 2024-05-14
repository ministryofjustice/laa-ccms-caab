package uk.gov.laa.ccms.caab.controller.application.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.ServletException;
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
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class CopyCaseSearchControllerTest {
  @Mock
  private CaseSearchCriteriaValidator validator;

  @Mock
  private ProviderService providerService;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private ApplicationMapper applicationMapper;

  @Mock
  private SearchConstants searchConstants;

  @InjectMocks
  private CopyCaseSearchController copyCaseSearchController;

  private MockMvc mockMvc;

  private UserDetail user;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(copyCaseSearchController).build();
    this.user = buildUser();

    when(searchConstants.getMaxSearchResultsCases()).thenReturn(200);
  }

  @Test
  public void testGetCopyCaseSearchAddsFeeEarnersToModel() throws Exception {
    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);

    this.mockMvc.perform(get("/application/copy-case/search")
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search"))
        .andExpect(model().attribute("feeEarners", feeEarners))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()));
  }

  @Test
  public void testGetCopyCaseSearchNoFeeEarners() {
    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.empty());

    Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(get("/application/copy-case/search")
            .sessionAttr(USER_DETAILS, user)));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals(String.format("Failed to retrieve Provider with id: %s", user.getProvider().getId()), exception.getCause().getMessage());
  }

  @Test
  public void testPostCopyCaseSearchHandlesValidationFailure() throws Exception {
    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue(null, "required.atLeastOneSearchCriteria",
          "You must provide at least one search criteria below. Please amend your entry.");
      return null;
    }).when(validator).validate(any(), any());

    this.mockMvc.perform(post("/application/copy-case/search")
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search"))
        .andExpect(model().attribute("feeEarners", feeEarners))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()));
  }

  @Test
  public void testPostCopyCaseSearch_NoCopyCaseStatus() throws Exception {
    List<BaseApplicationDetail> baseApplications = new ArrayList<>();

    when(applicationService.getCases(any(), any())).thenReturn(baseApplications);

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    this.mockMvc.perform(post("/application/copy-case/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search-no-results"));

    verify(applicationService).getCopyCaseStatus();
    verify(applicationService).getCases(eq(caseSearchCriteria), any());
    // No copy case status, so search criteria shouldn't have been updated
    assertNull(caseSearchCriteria.getStatus());
  }

  @Test
  public void testPostCopyCaseSearch_NoResults() throws Exception {
    List<BaseApplicationDetail> baseApplications = new ArrayList<>();

    when(applicationService.getCases(any(), any())).thenReturn(baseApplications);

    String COPY_STATUS_CODE = "APP";
    when(applicationService.getCopyCaseStatus()).thenReturn(
        new CaseStatusLookupValueDetail().code(COPY_STATUS_CODE));

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    this.mockMvc.perform(post("/application/copy-case/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search-no-results"));

    verify(applicationService).getCopyCaseStatus();
    verify(applicationService).getCases(eq(caseSearchCriteria), any());
    assertEquals(COPY_STATUS_CODE, caseSearchCriteria.getStatus());
  }

  @Test
  public void testPostCopyCaseSearch_WithTooManyResults() throws Exception {
    when(applicationService.getCases(any(), any())).thenThrow(
        new TooManyResultsException(""));

    this.mockMvc.perform(post("/application/copy-case/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name(
            "application/application-copy-case-search-too-many-results"));
  }

  @Test
  public void testPostCopyCaseSearch_WithResults() throws Exception {
    List<BaseApplicationDetail> caseSearchResults = List.of(new BaseApplicationDetail());

    when(applicationService.getCases(any(), any())).thenReturn(caseSearchResults);

    this.mockMvc.perform(post("/application/copy-case/search")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/copy-case/results"));
  }

  @Test
  public void testGetCopyCaseSearchResults_PaginatesResults() throws Exception {
    List<BaseApplicationDetail> caseSearchResults = List.of(
        new BaseApplicationDetail(),
        new BaseApplicationDetail());

    when(applicationMapper.toApplicationDetails(any()))
        .thenReturn(new ApplicationDetails());

    this.mockMvc.perform(get("/application/copy-case/results")
            .param("page", "0")
            .param("size", "1")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_RESULTS, caseSearchResults))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search-results"))
        .andExpect(model().attributeExists(CopyCaseSearchController.CASE_RESULTS_PAGE));
  }


  @Test
  public void testSelectCopyCaseReferenceNumber_InvalidCaseRef() {
    Exception exception = assertThrows(ServletException.class, () ->
        this.mockMvc.perform(get("/application/copy-case/{caseRef}/confirm", "123")
            .sessionAttr(CASE_SEARCH_RESULTS, new ArrayList<BaseApplicationDetail>())
            .sessionAttr(APPLICATION_FORM_DATA, new ApplicationFormData())));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());

    String expectedMessage = "Invalid copyCaseReferenceNumber supplied";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  public void testSelectCopyCaseReferenceNumber_ValidCaseRef() throws Exception {
    List<BaseApplicationDetail> caseSearchResults =
        List.of(new BaseApplicationDetail().caseReferenceNumber("123"));

    ApplicationFormData applicationFormData = new ApplicationFormData();
    this.mockMvc.perform(get("/application/copy-case/{caseRef}/confirm", "123")
            .sessionAttr(CASE_SEARCH_RESULTS, caseSearchResults)
            .sessionAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/search"));

    assertEquals("123", applicationFormData.getCopyCaseReferenceNumber());
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
